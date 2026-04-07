package com.framepilotai.server.infrastructure.repository;

import com.framepilotai.server.common.error.DomainException;
import com.framepilotai.server.domain.model.FallbackLevel;
import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.PipelineType;
import com.framepilotai.server.domain.model.RenderEventRecord;
import com.framepilotai.server.domain.model.RenderJobRecord;
import com.framepilotai.server.domain.model.RenderStatus;
import com.framepilotai.server.domain.model.RuntimeSampleRecord;
import com.framepilotai.server.domain.model.RuntimeSnapshot;
import com.framepilotai.server.domain.port.RenderJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcRenderJobRepository implements RenderJobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRenderJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RenderJobRecord create(UUID jobId, UUID projectId, PipelinePlan plan, String checkpointPath) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                insert into render_jobs(id, project_id, status, pipeline_type, fallback_level, artifact_path, artifact_type, checkpoint_path, started_at, finished_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                jobId.toString(),
                projectId.toString(),
                RenderStatus.QUEUED.name(),
                plan.pipelineType().name(),
                plan.fallbackLevel().name(),
                null,
                null,
                checkpointPath,
                Timestamp.from(now),
                null
        );
        return new RenderJobRecord(jobId, projectId, RenderStatus.QUEUED, plan.pipelineType(), plan.fallbackLevel(), null, null, checkpointPath, now, null);
    }

    @Override
    public Optional<RenderJobRecord> findById(UUID jobId) {
        return jdbcTemplate.query(
                "select * from render_jobs where id = ?",
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(mapJob(rs));
                },
                jobId.toString()
        );
    }

    @Override
    public RenderJobRecord updateStatus(UUID jobId, RenderStatus status, String checkpointPath) {
        int updated = jdbcTemplate.update(
                """
                update render_jobs
                set status = ?, checkpoint_path = coalesce(?, checkpoint_path), finished_at = case when ? in ('COMPLETED', 'COMPLETED_WITH_FALLBACK', 'FAILED', 'CANCELLED') then ? else finished_at end
                where id = ?
                """,
                status.name(),
                checkpointPath,
                status.name(),
                Timestamp.from(Instant.now()),
                jobId.toString()
        );
        if (updated == 0) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Render job not found: " + jobId);
        }
        return findById(jobId).orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Render job not found: " + jobId));
    }

    @Override
    public RenderJobRecord complete(UUID jobId, RenderStatus status, String artifactPath, String artifactType, String checkpointPath) {
        Instant finishedAt = Instant.now();
        int updated = jdbcTemplate.update(
                """
                update render_jobs
                set status = ?, artifact_path = ?, artifact_type = ?, checkpoint_path = ?, finished_at = ?
                where id = ?
                """,
                status.name(),
                artifactPath,
                artifactType,
                checkpointPath,
                Timestamp.from(finishedAt),
                jobId.toString()
        );
        if (updated == 0) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Render job not found: " + jobId);
        }
        return findById(jobId).orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Render job not found: " + jobId));
    }

    @Override
    public RenderJobRecord fail(UUID jobId, String checkpointPath) {
        return complete(jobId, RenderStatus.FAILED, null, null, checkpointPath);
    }

    @Override
    public void appendEvent(UUID jobId, String phase, String shotId, double progressPercent, String message) {
        jdbcTemplate.update(
                """
                insert into render_job_events(job_id, phase, shot_id, progress_percent, message, created_at)
                values (?, ?, ?, ?, ?, ?)
                """,
                jobId.toString(),
                phase,
                shotId,
                progressPercent,
                message,
                Timestamp.from(Instant.now())
        );
    }

    @Override
    public List<RenderEventRecord> listEvents(UUID jobId) {
        return jdbcTemplate.query(
                """
                select job_id, phase, shot_id, progress_percent, message, created_at
                from render_job_events
                where job_id = ?
                order by id asc
                """,
                (rs, rowNum) -> new RenderEventRecord(
                        UUID.fromString(rs.getString("job_id")),
                        rs.getString("phase"),
                        rs.getString("shot_id"),
                        rs.getDouble("progress_percent"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                jobId.toString()
        );
    }

    @Override
    public void appendRuntimeSample(UUID jobId, String phase, String shotId, double progressPercent, RuntimeSnapshot snapshot) {
        jdbcTemplate.update(
                """
                insert into render_runtime_samples(job_id, phase, shot_id, progress_percent, cpu_load_percent, used_memory_bytes, total_memory_bytes, available_memory_bytes, estimated_vram_bytes, captured_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                jobId.toString(),
                phase,
                shotId,
                progressPercent,
                snapshot.cpuLoadPercent(),
                snapshot.usedMemoryBytes(),
                snapshot.totalMemoryBytes(),
                snapshot.availableMemoryBytes(),
                snapshot.estimatedVramBytes(),
                Timestamp.from(snapshot.capturedAt())
        );
    }

    @Override
    public List<RuntimeSampleRecord> listRuntimeSamples(UUID jobId) {
        return jdbcTemplate.query(
                """
                select job_id, phase, shot_id, progress_percent, cpu_load_percent, used_memory_bytes, total_memory_bytes, available_memory_bytes, estimated_vram_bytes, captured_at
                from render_runtime_samples
                where job_id = ?
                order by id asc
                """,
                (rs, rowNum) -> new RuntimeSampleRecord(
                        UUID.fromString(rs.getString("job_id")),
                        rs.getString("phase"),
                        rs.getString("shot_id"),
                        rs.getDouble("progress_percent"),
                        rs.getDouble("cpu_load_percent"),
                        rs.getLong("used_memory_bytes"),
                        rs.getLong("total_memory_bytes"),
                        rs.getLong("available_memory_bytes"),
                        rs.getLong("estimated_vram_bytes"),
                        rs.getTimestamp("captured_at").toInstant()
                ),
                jobId.toString()
        );
    }

    private RenderJobRecord mapJob(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RenderJobRecord(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("project_id")),
                RenderStatus.valueOf(rs.getString("status")),
                PipelineType.valueOf(rs.getString("pipeline_type")),
                FallbackLevel.valueOf(rs.getString("fallback_level")),
                rs.getString("artifact_path"),
                rs.getString("artifact_type"),
                rs.getString("checkpoint_path"),
                rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toInstant()
        );
    }
}
