package com.framepilotai.server.infrastructure.repository;

import com.framepilotai.server.common.error.DomainException;
import com.framepilotai.server.domain.model.AssetRecord;
import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectMetadataRecord;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.ProjectStatus;
import com.framepilotai.server.domain.model.ProjectWorkspace;
import com.framepilotai.server.domain.model.SceneData;
import com.framepilotai.server.domain.model.ShotData;
import com.framepilotai.server.domain.port.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcProjectRepository implements ProjectRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ProjectRecord create(String name, String description, String sourceType) {
        UUID projectId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                "insert into projects(id, name, status, created_at, updated_at) values (?, ?, ?, ?, ?)",
                projectId.toString(),
                name,
                ProjectStatus.DRAFT.name(),
                Timestamp.from(now),
                Timestamp.from(now)
        );
        saveMetadata(projectId, new ProjectMetadataRecord(sourceType, description, "", "LOW", 0, 0, "{}", now));
        return new ProjectRecord(projectId, name, ProjectStatus.DRAFT, List.of(), now, now);
    }

    @Override
    public ProjectRecord create(String name, List<AssetRecord> assets) {
        ProjectRecord draft = create(name, "Imported from local assets", "image-sequence");
        return importAssets(draft.id(), assets);
    }

    @Override
    public ProjectRecord importAssets(UUID projectId, List<AssetRecord> assets) {
        ProjectRecord project = findById(projectId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
        int startOrder = project.assets().size();
        List<AssetRecord> persistedAssets = new ArrayList<>(project.assets());
        for (int index = 0; index < assets.size(); index++) {
            AssetRecord asset = assets.get(index);
            UUID assetId = UUID.randomUUID();
            jdbcTemplate.update(
                    "insert into project_assets(id, project_id, source_path, media_type, size_bytes) values (?, ?, ?, ?, ?)",
                    assetId.toString(),
                    projectId.toString(),
                    asset.sourcePath(),
                    asset.mediaType(),
                    asset.sizeBytes()
            );
            persistedAssets.add(new AssetRecord(assetId, projectId, asset.sourcePath(), asset.mediaType(), asset.sizeBytes(), startOrder + index));
        }
        touchProject(projectId, ProjectStatus.DRAFT);
        return findById(projectId).orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
    }

    @Override
    public Optional<ProjectRecord> findById(UUID projectId) {
        List<ProjectRecord> projects = jdbcTemplate.query(
                "select id, name, status, created_at, updated_at from projects where id = ?",
                (resultSet, rowNum) -> mapProject(resultSet, loadAssets(projectId)),
                projectId.toString()
        );
        return projects.stream().findFirst();
    }

    @Override
    public ProjectWorkspace getWorkspace(UUID projectId) {
        ProjectRecord project = findById(projectId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
        ProjectMetadataRecord metadata = loadMetadata(projectId).orElseGet(() -> new ProjectMetadataRecord("image-sequence", "", "", "LOW", 0, 0, "{}", Instant.now()));
        return new ProjectWorkspace(project, metadata, findPanels(projectId), findOcrBlocks(projectId), findScenePlan(projectId));
    }

    @Override
    public ProjectRecord updateStatus(UUID projectId, ProjectStatus status) {
        int updated = jdbcTemplate.update(
                "update projects set status = ?, updated_at = ? where id = ?",
                status.name(),
                Timestamp.from(Instant.now()),
                projectId.toString()
        );
        if (updated == 0) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Project not found: " + projectId);
        }
        return findById(projectId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
    }

    @Override
    public ProjectMetadataRecord saveMetadata(UUID projectId, ProjectMetadataRecord metadata) {
        jdbcTemplate.update(
                """
                insert into project_metadata(project_id, source_type, description, summary, complexity_level, panel_count, scene_count, metadata_json, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict(project_id) do update set
                    source_type = excluded.source_type,
                    description = excluded.description,
                    summary = excluded.summary,
                    complexity_level = excluded.complexity_level,
                    panel_count = excluded.panel_count,
                    scene_count = excluded.scene_count,
                    metadata_json = excluded.metadata_json,
                    updated_at = excluded.updated_at
                """,
                projectId.toString(),
                metadata.sourceType(),
                metadata.description(),
                metadata.summary(),
                metadata.complexityLevel(),
                metadata.panelCount(),
                metadata.sceneCount(),
                metadata.metadataJson(),
                Timestamp.from(metadata.updatedAt())
        );
        return metadata;
    }

    @Override
    public List<PanelData> savePanels(UUID projectId, List<PanelData> panels) {
        jdbcTemplate.update("delete from project_panels where project_id = ?", projectId.toString());
        Instant now = Instant.now();
        for (PanelData panel : panels) {
            jdbcTemplate.update(
                    """
                    insert into project_panels(panel_id, project_id, asset_id, asset_path, reading_order, crop_x, crop_y, width, height, summary, review_state, metadata_json, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    panel.panelId(),
                    projectId.toString(),
                    panel.assetId(),
                    panel.assetPath(),
                    panel.readingOrder(),
                    panel.cropX(),
                    panel.cropY(),
                    panel.width(),
                    panel.height(),
                    panel.summary(),
                    panel.reviewState(),
                    "{}",
                    Timestamp.from(now)
            );
        }
        touchProject(projectId, ProjectStatus.PLANNING);
        return findPanels(projectId);
    }

    @Override
    public List<PanelData> findPanels(UUID projectId) {
        return jdbcTemplate.query(
                """
                select panel_id, asset_id, asset_path, reading_order, crop_x, crop_y, width, height, summary, review_state
                from project_panels
                where project_id = ?
                order by reading_order asc
                """,
                (rs, rowNum) -> new PanelData(
                        rs.getString("panel_id"),
                        rs.getString("asset_id"),
                        rs.getString("asset_path"),
                        rs.getInt("reading_order"),
                        rs.getInt("crop_x"),
                        rs.getInt("crop_y"),
                        rs.getInt("width"),
                        rs.getInt("height"),
                        rs.getString("summary"),
                        rs.getString("review_state")
                ),
                projectId.toString()
        );
    }

    @Override
    public List<OcrBlock> saveOcrBlocks(UUID projectId, List<OcrBlock> blocks) {
        jdbcTemplate.update("delete from project_ocr where project_id = ?", projectId.toString());
        Instant now = Instant.now();
        for (OcrBlock block : blocks) {
            jdbcTemplate.update(
                    """
                    insert into project_ocr(ocr_id, project_id, panel_id, text, confidence, language, adapter_name, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    block.ocrId(),
                    projectId.toString(),
                    block.panelId(),
                    block.text(),
                    block.confidence(),
                    block.language(),
                    block.adapterName(),
                    Timestamp.from(now)
            );
        }
        touchProject(projectId, ProjectStatus.PLANNING);
        return findOcrBlocks(projectId);
    }

    @Override
    public List<OcrBlock> findOcrBlocks(UUID projectId) {
        return jdbcTemplate.query(
                """
                select ocr_id, panel_id, text, confidence, language, adapter_name
                from project_ocr
                where project_id = ?
                order by panel_id asc
                """,
                (rs, rowNum) -> new OcrBlock(
                        rs.getString("ocr_id"),
                        rs.getString("panel_id"),
                        rs.getString("text"),
                        rs.getDouble("confidence"),
                        rs.getString("language"),
                        rs.getString("adapter_name")
                ),
                projectId.toString()
        );
    }

    @Override
    public List<SceneData> saveScenePlan(UUID projectId, List<SceneData> scenes) {
        jdbcTemplate.update("delete from project_shots where project_id = ?", projectId.toString());
        jdbcTemplate.update("delete from project_scenes where project_id = ?", projectId.toString());
        Instant now = Instant.now();
        for (SceneData scene : scenes) {
            jdbcTemplate.update(
                    """
                    insert into project_scenes(scene_id, project_id, scene_order, title, narrative, complexity_score, camera_effect_level, total_duration_ms, metadata_json, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    scene.sceneId(),
                    projectId.toString(),
                    scene.sceneOrder(),
                    scene.title(),
                    scene.narrative(),
                    scene.complexityScore(),
                    scene.cameraEffectLevel(),
                    scene.totalDurationMillis(),
                    "{}",
                    Timestamp.from(now)
            );
            for (ShotData shot : scene.shots()) {
                jdbcTemplate.update(
                        """
                        insert into project_shots(shot_id, project_id, scene_id, panel_id, shot_order, duration_ms, camera_move, effect_level, timeline_start_ms, timeline_end_ms, notes, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        shot.shotId(),
                        projectId.toString(),
                        scene.sceneId(),
                        shot.panelId(),
                        shot.orderIndex(),
                        shot.durationMillis(),
                        shot.cameraMove(),
                        shot.effectLevel(),
                        shot.timelineStartMillis(),
                        shot.timelineEndMillis(),
                        shot.notes(),
                        Timestamp.from(now)
                );
            }
        }
        touchProject(projectId, ProjectStatus.READY);
        return findScenePlan(projectId);
    }

    @Override
    public List<SceneData> findScenePlan(UUID projectId) {
        List<ShotData> shots = jdbcTemplate.query(
                """
                select shot_id, scene_id, panel_id, shot_order, duration_ms, camera_move, effect_level, timeline_start_ms, timeline_end_ms, notes
                from project_shots
                where project_id = ?
                order by shot_order asc
                """,
                (rs, rowNum) -> new ShotData(
                        rs.getString("shot_id"),
                        rs.getString("scene_id"),
                        rs.getString("panel_id"),
                        rs.getInt("shot_order"),
                        rs.getString("camera_move"),
                        rs.getInt("duration_ms"),
                        rs.getString("effect_level"),
                        rs.getInt("timeline_start_ms"),
                        rs.getInt("timeline_end_ms"),
                        rs.getString("notes")
                ),
                projectId.toString()
        );
        Map<String, List<ShotData>> shotsByScene = shots.stream().collect(Collectors.groupingBy(ShotData::sceneId));
        return jdbcTemplate.query(
                """
                select scene_id, scene_order, title, narrative, complexity_score, camera_effect_level, total_duration_ms
                from project_scenes
                where project_id = ?
                order by scene_order asc
                """,
                (rs, rowNum) -> new SceneData(
                        rs.getString("scene_id"),
                        rs.getInt("scene_order"),
                        rs.getString("title"),
                        rs.getString("narrative"),
                        rs.getDouble("complexity_score"),
                        rs.getString("camera_effect_level"),
                        rs.getInt("total_duration_ms"),
                        shotsByScene.getOrDefault(rs.getString("scene_id"), List.of()).stream()
                                .sorted(Comparator.comparingInt(ShotData::orderIndex))
                                .toList()
                ),
                projectId.toString()
        );
    }

    private Optional<ProjectMetadataRecord> loadMetadata(UUID projectId) {
        return jdbcTemplate.query(
                "select source_type, description, summary, complexity_level, panel_count, scene_count, metadata_json, updated_at from project_metadata where project_id = ?",
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new ProjectMetadataRecord(
                            rs.getString("source_type"),
                            rs.getString("description"),
                            rs.getString("summary"),
                            rs.getString("complexity_level"),
                            rs.getInt("panel_count"),
                            rs.getInt("scene_count"),
                            rs.getString("metadata_json"),
                            rs.getTimestamp("updated_at").toInstant()
                    ));
                },
                projectId.toString()
        );
    }

    private List<AssetRecord> loadAssets(UUID projectId) {
        List<AssetRecord> assets = jdbcTemplate.query(
                "select id, project_id, source_path, media_type, size_bytes from project_assets where project_id = ? order by rowid asc",
                (resultSet, rowNum) -> new AssetRecord(
                        UUID.fromString(resultSet.getString("id")),
                        UUID.fromString(resultSet.getString("project_id")),
                        resultSet.getString("source_path"),
                        resultSet.getString("media_type"),
                        resultSet.getLong("size_bytes"),
                        rowNum
                ),
                projectId.toString()
        );
        return List.copyOf(assets);
    }

    private ProjectRecord mapProject(ResultSet resultSet, List<AssetRecord> assets) throws java.sql.SQLException {
        return new ProjectRecord(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("name"),
                ProjectStatus.valueOf(resultSet.getString("status")),
                assets,
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private void touchProject(UUID projectId, ProjectStatus status) {
        updateStatus(projectId, status);
    }
}
