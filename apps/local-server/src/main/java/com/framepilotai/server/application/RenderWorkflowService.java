package com.framepilotai.server.application;

import com.framepilotai.server.common.config.FramePilotProperties;
import com.framepilotai.server.common.error.DomainException;
import com.framepilotai.server.domain.model.ExportArtifact;
import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.ProjectStatus;
import com.framepilotai.server.domain.model.RenderCheckpoint;
import com.framepilotai.server.domain.model.RenderJobRecord;
import com.framepilotai.server.domain.model.RenderJobSnapshot;
import com.framepilotai.server.domain.model.RenderOutcome;
import com.framepilotai.server.domain.model.RenderPreparation;
import com.framepilotai.server.domain.model.RenderProfile;
import com.framepilotai.server.domain.model.RenderStatus;
import com.framepilotai.server.domain.model.RenderedShot;
import com.framepilotai.server.domain.model.RuntimeSampleRecord;
import com.framepilotai.server.domain.model.RuntimeSnapshot;
import com.framepilotai.server.domain.model.SceneData;
import com.framepilotai.server.domain.model.ShotData;
import com.framepilotai.server.domain.port.ExportService;
import com.framepilotai.server.domain.port.FallbackEngine;
import com.framepilotai.server.domain.port.ProjectRepository;
import com.framepilotai.server.domain.port.RenderJobRepository;
import com.framepilotai.server.domain.port.RenderOrchestrator;
import com.framepilotai.server.domain.port.RuntimeMonitor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RenderWorkflowService {

    private final DeviceIntelligenceAgentService deviceIntelligenceAgentService;
    private final ProjectPlanningService projectPlanningService;
    private final ProjectRepository projectRepository;
    private final RenderJobRepository renderJobRepository;
    private final RenderOrchestrator renderOrchestrator;
    private final RuntimeMonitor runtimeMonitor;
    private final FallbackEngine fallbackEngine;
    private final ExportService exportService;
    private final FramePilotProperties properties;
    private final ExecutorService renderExecutor;
    private final Map<UUID, RenderControlState> controls = new ConcurrentHashMap<>();
    private final Map<UUID, Future<?>> activeJobs = new ConcurrentHashMap<>();

    public RenderWorkflowService(
            DeviceIntelligenceAgentService deviceIntelligenceAgentService,
            ProjectPlanningService projectPlanningService,
            ProjectRepository projectRepository,
            RenderJobRepository renderJobRepository,
            RenderOrchestrator renderOrchestrator,
            RuntimeMonitor runtimeMonitor,
            FallbackEngine fallbackEngine,
            ExportService exportService,
            FramePilotProperties properties
    ) {
        this.deviceIntelligenceAgentService = deviceIntelligenceAgentService;
        this.projectPlanningService = projectPlanningService;
        this.projectRepository = projectRepository;
        this.renderJobRepository = renderJobRepository;
        this.renderOrchestrator = renderOrchestrator;
        this.runtimeMonitor = runtimeMonitor;
        this.fallbackEngine = fallbackEngine;
        this.exportService = exportService;
        this.properties = properties;
        this.renderExecutor = Executors.newFixedThreadPool(Math.max(1, properties.getRender().getQueueConcurrency()));
    }

    public RenderJobSnapshot startRender(UUID projectId) {
        ProjectAnalysis analysis = buildRenderAnalysis(projectId);
        UUID jobId = UUID.randomUUID();
        PipelinePlan initialPlan = analysis.pipelinePlan();
        return scheduleRender(jobId, analysis, initialPlan, List.of(), false);
    }

    public RenderJobSnapshot resumeRender(UUID jobId) {
        RenderJobRecord job = renderJobRepository.findById(jobId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Render job not found: " + jobId));
        if (!(job.status() == RenderStatus.PAUSED || job.status() == RenderStatus.FAILED || job.status() == RenderStatus.CANCELLED)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Only paused, failed or cancelled jobs can be resumed.");
        }
        ProjectAnalysis analysis = buildRenderAnalysis(job.projectId());
        RenderCheckpoint checkpoint = readCheckpoint(job.projectId(), jobId)
                .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "Checkpoint is missing for job: " + jobId));
        PipelinePlan resumePlan = new PipelinePlan(
                checkpoint.pipelineType(),
                analysis.pipelinePlan().preset(),
                checkpoint.fallbackLevel(),
                checkpoint.reasons()
        );
        return scheduleRender(jobId, analysis, resumePlan, checkpoint.completedShots(), true);
    }

    public RenderJobSnapshot getJobSnapshot(UUID jobId) {
        RenderJobRecord job = renderJobRepository.findById(jobId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Render job not found: " + jobId));
        RenderCheckpoint checkpoint = readCheckpoint(job.projectId(), jobId).orElse(defaultCheckpoint(job, List.of(), List.of(), "queued", null, 0, 0));
        try {
            return new RenderJobSnapshot(
                    job,
                    checkpoint,
                    renderJobRepository.listEvents(jobId),
                    renderJobRepository.listRuntimeSamples(jobId),
                    exportService.listArtifacts(job.projectId(), jobId)
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to collect render job snapshot", exception);
        }
    }

    public RenderJobSnapshot pauseRender(UUID jobId) {
        if (!controls.containsKey(jobId)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Render job is not active: " + jobId);
        }
        controls.put(jobId, RenderControlState.PAUSE_REQUESTED);
        renderJobRepository.appendEvent(jobId, "pause-requested", null, getJobSnapshot(jobId).checkpoint().progressPercent(), "Pause requested. The renderer will stop at the next safe shot boundary.");
        return getJobSnapshot(jobId);
    }

    public RenderJobSnapshot cancelRender(UUID jobId) {
        if (!controls.containsKey(jobId)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Render job is not active: " + jobId);
        }
        controls.put(jobId, RenderControlState.CANCEL_REQUESTED);
        RenderJobRecord job = renderJobRepository.updateStatus(jobId, RenderStatus.CANCELLING, null);
        renderJobRepository.appendEvent(jobId, "cancel-requested", null, getJobSnapshot(jobId).checkpoint().progressPercent(), "Cancel requested. The renderer will stop at the next safe shot boundary.");
        return getJobSnapshot(job.id());
    }

    public List<RuntimeSampleRecord> getRuntimeSamples(UUID jobId) {
        renderJobRepository.findById(jobId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Render job not found: " + jobId));
        return renderJobRepository.listRuntimeSamples(jobId);
    }

    public List<ExportArtifact> getArtifacts(UUID jobId) {
        RenderJobRecord job = renderJobRepository.findById(jobId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Render job not found: " + jobId));
        try {
            return exportService.listArtifacts(job.projectId(), jobId);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to list export artifacts", exception);
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        renderExecutor.shutdownNow();
    }

    private RenderJobSnapshot scheduleRender(UUID jobId, ProjectAnalysis analysis, PipelinePlan plan, List<String> completedShots, boolean resumeExisting) {
        if (activeJobs.containsKey(jobId) && !activeJobs.get(jobId).isDone()) {
            throw new DomainException(HttpStatus.CONFLICT, "Render job is already running: " + jobId);
        }

        try {
            RenderPreparation preparation = renderOrchestrator.prepare(jobId, analysis.project(), analysis, plan);
            RenderCheckpoint checkpoint = defaultCheckpoint(
                    new RenderJobRecord(jobId, analysis.project().id(), RenderStatus.QUEUED, plan.pipelineType(), plan.fallbackLevel(), null, null, preparation.jobDirectory().resolve("render-checkpoint.json").toString(), Instant.now(), null),
                    completedShots,
                    plan.reasons(),
                    resumeExisting ? "resume-queued" : "queued",
                    null,
                    flattenShots(analysis).size(),
                    calculateProgress(completedShots.size(), flattenShots(analysis).size())
            );
            Path checkpointPath = exportService.writeCheckpoint(analysis.project().id(), jobId, checkpoint);
            if (resumeExisting) {
                renderJobRepository.updateStatus(jobId, RenderStatus.QUEUED, checkpointPath.toString());
            } else {
                renderJobRepository.create(jobId, analysis.project().id(), plan, checkpointPath.toString());
            }
            renderJobRepository.appendEvent(jobId, checkpoint.currentPhase(), checkpoint.currentShotId(), checkpoint.progressPercent(), resumeExisting
                    ? "Render job resumed and queued."
                    : "Render job created and queued.");

            controls.put(jobId, RenderControlState.RUNNING);
            Future<?> future = renderExecutor.submit(() -> executeRender(jobId, analysis, preparation, checkpoint, plan));
            activeJobs.put(jobId, future);
            return getJobSnapshot(jobId);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to schedule render job", exception);
        }
    }

    private void executeRender(UUID jobId, ProjectAnalysis analysis, RenderPreparation preparation, RenderCheckpoint checkpoint, PipelinePlan initialPlan) {
        try {
            renderJobRepository.updateStatus(jobId, RenderStatus.STARTED, preparation.jobDirectory().resolve("render-checkpoint.json").toString());
            renderJobRepository.appendEvent(jobId, "started", checkpoint.currentShotId(), checkpoint.progressPercent(), "Render worker picked up job from the internal queue.");

            if (exportService.resolveFfmpegExecutable().isEmpty()) {
                completeFallback(jobId, analysis, preparation, initialPlan, checkpoint.completedShots(), "FFmpeg is unavailable. Exporting deterministic fallback package.");
                return;
            }

            List<ShotContext> shots = flattenShots(analysis);
            LinkedHashSet<String> completedShotIds = new LinkedHashSet<>(checkpoint.completedShots());
            Map<String, RenderedShot> renderedShots = loadRenderedShots(preparation, shots, completedShotIds);
            PipelinePlan currentPlan = initialPlan;
            RenderProfile lockedProfile = null;

            for (ShotContext shotContext : shots) {
                if (completedShotIds.contains(shotContext.shot().shotId())) {
                    continue;
                }

                if (handleRequestedStop(jobId, analysis.project().id(), preparation, currentPlan, completedShotIds, shots.size(), "pre-shot", shotContext.shot().shotId())) {
                    return;
                }

                RuntimeSnapshot runtimeBefore = runtimeMonitor.capture();
                double progressBefore = calculateProgress(completedShotIds.size(), shots.size());
                renderJobRepository.appendRuntimeSample(jobId, "before-shot", shotContext.shot().shotId(), progressBefore, runtimeBefore);
                PipelinePlan fallbackPlan = fallbackEngine.apply(currentPlan, runtimeBefore);
                if (!fallbackPlan.equals(currentPlan)) {
                    currentPlan = fallbackPlan;
                    renderJobRepository.appendEvent(jobId, "fallback-applied", shotContext.shot().shotId(), progressBefore, "Guardrail applied: " + String.join(" | ", currentPlan.reasons()));
                }

                if (lockedProfile == null) {
                    lockedProfile = buildProfile(currentPlan, null);
                }
                RenderProfile activeProfile = buildProfile(currentPlan, lockedProfile);

                boolean rendered = false;
                for (int attempt = 1; attempt <= Math.max(1, properties.getRender().getMaxRetryPerShot()); attempt++) {
                    renderJobRepository.appendEvent(jobId, "shot-started", shotContext.shot().shotId(), progressBefore, "Rendering shot " + shotContext.shot().shotId() + " attempt " + attempt + ".");
                    try {
                        Thread.sleep(properties.getRender().getPausePollMillis());
                        RenderedShot renderedShot = renderOrchestrator.renderShot(preparation, shotContext.scene(), shotContext.shot(), analysis, currentPlan, activeProfile);
                        renderedShots.put(renderedShot.shotId(), renderedShot);
                        completedShotIds.add(renderedShot.shotId());

                        RuntimeSnapshot runtimeAfter = runtimeMonitor.capture();
                        double progressAfter = calculateProgress(completedShotIds.size(), shots.size());
                        renderJobRepository.appendRuntimeSample(jobId, "after-shot", shotContext.shot().shotId(), progressAfter, runtimeAfter);
                        Path checkpointPath = exportService.writeCheckpoint(
                                analysis.project().id(),
                                jobId,
                                new RenderCheckpoint(
                                        jobId,
                                        analysis.project().id(),
                                        "running",
                                        currentPlan.pipelineType(),
                                        currentPlan.fallbackLevel(),
                                        shotContext.shot().shotId(),
                                        shots.size(),
                                        progressAfter,
                                        List.copyOf(completedShotIds),
                                        currentPlan.reasons(),
                                        Instant.now()
                                )
                        );
                        renderJobRepository.updateStatus(jobId, RenderStatus.STARTED, checkpointPath.toString());
                        renderJobRepository.appendEvent(jobId, "shot-completed", shotContext.shot().shotId(), progressAfter, "Shot rendered successfully.");
                        rendered = true;
                        break;
                    } catch (Exception exception) {
                        renderJobRepository.appendEvent(jobId, "shot-error", shotContext.shot().shotId(), progressBefore, "Shot render failed: " + exception.getMessage());
                        if (attempt == Math.max(1, properties.getRender().getMaxRetryPerShot())) {
                            Path checkpointPath = exportService.writeCheckpoint(
                                    analysis.project().id(),
                                    jobId,
                                    new RenderCheckpoint(
                                            jobId,
                                            analysis.project().id(),
                                            "failed",
                                            currentPlan.pipelineType(),
                                            currentPlan.fallbackLevel(),
                                            shotContext.shot().shotId(),
                                            shots.size(),
                                            calculateProgress(completedShotIds.size(), shots.size()),
                                            List.copyOf(completedShotIds),
                                            currentPlan.reasons(),
                                            Instant.now()
                                    )
                            );
                            renderJobRepository.fail(jobId, checkpointPath.toString());
                            renderJobRepository.appendEvent(jobId, "failed", shotContext.shot().shotId(), calculateProgress(completedShotIds.size(), shots.size()), "Retry budget exhausted. Render job failed.");
                            return;
                        }
                    }
                }

                if (!rendered) {
                    return;
                }

                if (handleRequestedStop(jobId, analysis.project().id(), preparation, currentPlan, completedShotIds, shots.size(), "post-shot", shotContext.shot().shotId())) {
                    return;
                }
            }

            Path outputPath = renderOrchestrator.muxFinal(preparation, new ArrayList<>(renderedShots.values()), analysis, currentPlan, buildProfile(currentPlan, lockedProfile));
            Path checkpointPath = exportService.writeCheckpoint(
                    analysis.project().id(),
                    jobId,
                    new RenderCheckpoint(
                            jobId,
                            analysis.project().id(),
                            "completed",
                            currentPlan.pipelineType(),
                            currentPlan.fallbackLevel(),
                            null,
                            shots.size(),
                            100,
                            List.copyOf(completedShotIds),
                            currentPlan.reasons(),
                            Instant.now()
                    )
            );
            RenderStatus finalStatus = currentPlan.fallbackLevel().name().equals("NONE") ? RenderStatus.COMPLETED : RenderStatus.COMPLETED_WITH_FALLBACK;
            renderJobRepository.complete(jobId, finalStatus, outputPath.toString(), "video/mp4", checkpointPath.toString());
            renderJobRepository.appendEvent(jobId, "completed", null, 100, "Final mux completed at " + outputPath + ".");
            projectRepository.updateStatus(analysis.project().id(), ProjectStatus.RENDERED);
        } catch (Exception exception) {
            try {
                Path checkpointPath = exportService.writeCheckpoint(
                        analysis.project().id(),
                        jobId,
                        new RenderCheckpoint(
                                jobId,
                                analysis.project().id(),
                                "failed",
                                initialPlan.pipelineType(),
                                initialPlan.fallbackLevel(),
                                null,
                                flattenShots(analysis).size(),
                                calculateProgress(checkpoint.completedShots().size(), flattenShots(analysis).size()),
                                checkpoint.completedShots(),
                                initialPlan.reasons(),
                                Instant.now()
                        )
                );
                renderJobRepository.fail(jobId, checkpointPath.toString());
                renderJobRepository.appendEvent(jobId, "failed", null, calculateProgress(checkpoint.completedShots().size(), flattenShots(analysis).size()), "Render job crashed: " + exception.getMessage());
            } catch (Exception nestedException) {
                renderJobRepository.fail(jobId, null);
            }
        } finally {
            activeJobs.remove(jobId);
            controls.remove(jobId);
        }
    }

    private void completeFallback(UUID jobId, ProjectAnalysis analysis, RenderPreparation preparation, PipelinePlan plan, List<String> completedShots, String message) throws Exception {
        RenderOutcome fallbackOutcome = renderOrchestrator.fallbackPackage(preparation, analysis, plan)
                .orElseThrow(() -> new IllegalStateException("Fallback export package is unavailable."));
        Path checkpointPath = exportService.writeCheckpoint(
                analysis.project().id(),
                jobId,
                new RenderCheckpoint(
                        jobId,
                        analysis.project().id(),
                        "fallback-completed",
                        plan.pipelineType(),
                        plan.fallbackLevel(),
                        null,
                        flattenShots(analysis).size(),
                        calculateProgress(completedShots.size(), flattenShots(analysis).size()),
                        completedShots,
                        plan.reasons(),
                        Instant.now()
                )
        );
        renderJobRepository.complete(jobId, fallbackOutcome.status(), fallbackOutcome.artifactPath(), fallbackOutcome.artifactType(), checkpointPath.toString());
        renderJobRepository.appendEvent(jobId, "fallback-completed", null, calculateProgress(completedShots.size(), flattenShots(analysis).size()), message);
    }

    private boolean handleRequestedStop(
            UUID jobId,
            UUID projectId,
            RenderPreparation preparation,
            PipelinePlan currentPlan,
            LinkedHashSet<String> completedShotIds,
            int totalShots,
            String phase,
            String shotId
    ) throws Exception {
        RenderControlState controlState = controls.getOrDefault(jobId, RenderControlState.RUNNING);
        if (controlState == RenderControlState.PAUSE_REQUESTED) {
            Path checkpointPath = exportService.writeCheckpoint(
                    projectId,
                    jobId,
                    new RenderCheckpoint(
                            jobId,
                            projectId,
                            "paused",
                            currentPlan.pipelineType(),
                            currentPlan.fallbackLevel(),
                            shotId,
                            totalShots,
                            calculateProgress(completedShotIds.size(), totalShots),
                            List.copyOf(completedShotIds),
                            currentPlan.reasons(),
                            Instant.now()
                    )
            );
            renderJobRepository.updateStatus(jobId, RenderStatus.PAUSED, checkpointPath.toString());
            renderJobRepository.appendEvent(jobId, "paused", shotId, calculateProgress(completedShotIds.size(), totalShots), "Pause request honored at " + phase + " boundary.");
            return true;
        }
        if (controlState == RenderControlState.CANCEL_REQUESTED) {
            Path checkpointPath = exportService.writeCheckpoint(
                    projectId,
                    jobId,
                    new RenderCheckpoint(
                            jobId,
                            projectId,
                            "cancelled",
                            currentPlan.pipelineType(),
                            currentPlan.fallbackLevel(),
                            shotId,
                            totalShots,
                            calculateProgress(completedShotIds.size(), totalShots),
                            List.copyOf(completedShotIds),
                            currentPlan.reasons(),
                            Instant.now()
                    )
            );
            renderJobRepository.complete(jobId, RenderStatus.CANCELLED, null, null, checkpointPath.toString());
            renderJobRepository.appendEvent(jobId, "cancelled", shotId, calculateProgress(completedShotIds.size(), totalShots), "Cancel request honored at " + phase + " boundary.");
            return true;
        }
        return false;
    }

    private ProjectAnalysis buildRenderAnalysis(UUID projectId) {
        var deviceSnapshot = deviceIntelligenceAgentService.scanAndAssess();
        ProjectAnalysis analysis = projectPlanningService.analyzeProject(projectId, deviceSnapshot);
        projectRepository.updateStatus(projectId, ProjectStatus.READY);
        return analysis;
    }

    private RenderProfile buildProfile(PipelinePlan plan, RenderProfile lockedProfile) {
        RenderProfile baseProfile = switch (plan.pipelineType()) {
            case ENHANCED_HYBRID -> new RenderProfile(1920, 1080, 30, 0.10, 1.00, 4200, true);
            case HYBRID -> new RenderProfile(1600, 900, 24, 0.08, 0.94, 3600, true);
            case HYBRID_LITE -> new RenderProfile(1280, 720, 24, 0.05, 0.84, 3200, true);
            case MOTION_COMIC -> new RenderProfile(1280, 720, 20, 0.03, 0.72, 2600, true);
        };

        RenderProfile fallbackAdjusted = switch (plan.fallbackLevel()) {
            case NONE -> baseProfile;
            case F1 -> new RenderProfile(baseProfile.outputWidth(), baseProfile.outputHeight(), baseProfile.fps(), baseProfile.zoomIntensity() * 0.85, baseProfile.shotDurationScale() * 0.92, Math.min(baseProfile.maxShotDurationMillis(), 3400), true);
            case F2 -> new RenderProfile(Math.min(baseProfile.outputWidth(), 1280), Math.min(baseProfile.outputHeight(), 720), Math.min(baseProfile.fps(), 24), baseProfile.zoomIntensity() * 0.70, baseProfile.shotDurationScale() * 0.78, Math.min(baseProfile.maxShotDurationMillis(), 2800), true);
            case F3 -> new RenderProfile(Math.min(baseProfile.outputWidth(), 1152), Math.min(baseProfile.outputHeight(), 648), Math.min(baseProfile.fps(), 20), baseProfile.zoomIntensity() * 0.50, baseProfile.shotDurationScale() * 0.68, Math.min(baseProfile.maxShotDurationMillis(), 2400), true);
            case F4 -> new RenderProfile(1024, 576, 18, 0.02, 0.60, 2200, true);
        };

        if (lockedProfile == null) {
            return fallbackAdjusted;
        }

        return new RenderProfile(
                lockedProfile.outputWidth(),
                lockedProfile.outputHeight(),
                lockedProfile.fps(),
                fallbackAdjusted.zoomIntensity(),
                fallbackAdjusted.shotDurationScale(),
                fallbackAdjusted.maxShotDurationMillis(),
                fallbackAdjusted.subtitleOverlayEnabled()
        );
    }

    private List<ShotContext> flattenShots(ProjectAnalysis analysis) {
        List<ShotContext> shots = new ArrayList<>();
        for (SceneData scene : analysis.scenes()) {
            for (ShotData shot : scene.shots()) {
                shots.add(new ShotContext(scene, shot));
            }
        }
        return List.copyOf(shots);
    }

    private Map<String, RenderedShot> loadRenderedShots(RenderPreparation preparation, List<ShotContext> shots, LinkedHashSet<String> completedShotIds) {
        Map<String, RenderedShot> renderedShots = new LinkedHashMap<>();
        for (ShotContext shotContext : shots) {
            if (!completedShotIds.contains(shotContext.shot().shotId())) {
                continue;
            }
            Path clipPath = preparation.jobDirectory().resolve(shotContext.shot().shotId() + ".mp4");
            if (Files.exists(clipPath)) {
                renderedShots.put(shotContext.shot().shotId(), new RenderedShot(shotContext.shot().shotId(), clipPath, shotContext.shot().durationMillis()));
            }
        }
        return renderedShots;
    }

    private Optional<RenderCheckpoint> readCheckpoint(UUID projectId, UUID jobId) {
        try {
            return exportService.readCheckpoint(projectId, jobId);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read render checkpoint", exception);
        }
    }

    private RenderCheckpoint defaultCheckpoint(RenderJobRecord job, List<String> completedShots, List<String> reasons, String phase, String currentShotId, int totalShots, double progressPercent) {
        return new RenderCheckpoint(
                job.id(),
                job.projectId(),
                phase,
                job.pipelineType(),
                job.fallbackLevel(),
                currentShotId,
                totalShots,
                progressPercent,
                List.copyOf(completedShots),
                List.copyOf(reasons),
                Instant.now()
        );
    }

    private double calculateProgress(int completedShots, int totalShots) {
        if (totalShots <= 0) {
            return 0;
        }
        return Math.round((completedShots * 10000d) / totalShots) / 100d;
    }

    private enum RenderControlState {
        RUNNING,
        PAUSE_REQUESTED,
        CANCEL_REQUESTED
    }

    private record ShotContext(SceneData scene, ShotData shot) {
    }
}
