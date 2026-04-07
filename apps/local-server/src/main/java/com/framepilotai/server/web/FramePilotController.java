package com.framepilotai.server.web;

import com.framepilotai.server.application.AgentCoordinatorService;
import com.framepilotai.server.application.DeviceIntelligenceAgentService;
import com.framepilotai.server.application.ProjectManagerService;
import com.framepilotai.server.application.ProjectAnalysisAgentService;
import com.framepilotai.server.application.ProjectPlanningService;
import com.framepilotai.server.application.RenderWorkflowService;
import com.framepilotai.server.common.config.FramePilotProperties;
import com.framepilotai.server.web.dto.AgentBriefingRequest;
import com.framepilotai.server.web.dto.AgentBriefingResponse;
import com.framepilotai.server.web.dto.AgentContractsResponse;
import com.framepilotai.server.web.dto.BenchmarkRequest;
import com.framepilotai.server.web.dto.BenchmarkRunResponse;
import com.framepilotai.server.web.dto.CopilotQueryRequest;
import com.framepilotai.server.web.dto.CopilotResponse;
import com.framepilotai.server.web.dto.CreateProjectRequest;
import com.framepilotai.server.web.dto.DeviceExplanationResponse;
import com.framepilotai.server.web.dto.DeviceRecommendationResponse;
import com.framepilotai.server.web.dto.DeviceScanResponse;
import com.framepilotai.server.web.dto.ImportProjectRequest;
import com.framepilotai.server.web.dto.ImportAssetsRequest;
import com.framepilotai.server.web.dto.OcrRunResponse;
import com.framepilotai.server.web.dto.PanelParseResponse;
import com.framepilotai.server.web.dto.ProjectAnalysisSummaryResponse;
import com.framepilotai.server.web.dto.ProjectAnalysisResponse;
import com.framepilotai.server.web.dto.ProjectResponse;
import com.framepilotai.server.web.dto.ProjectWorkspaceResponse;
import com.framepilotai.server.web.dto.RenderArtifactsResponse;
import com.framepilotai.server.web.dto.RenderJobStatusResponse;
import com.framepilotai.server.web.dto.RenderRuntimeStatsResponse;
import com.framepilotai.server.web.dto.ScenePlanResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:1420", "tauri://localhost"})
public class FramePilotController {

    private final AgentCoordinatorService agentCoordinatorService;
    private final DeviceIntelligenceAgentService deviceIntelligenceAgentService;
    private final ProjectManagerService projectManagerService;
    private final ProjectAnalysisAgentService projectAnalysisAgentService;
    private final ProjectPlanningService projectPlanningService;
    private final RenderWorkflowService renderWorkflowService;
    private final FramePilotProperties properties;

    public FramePilotController(
            AgentCoordinatorService agentCoordinatorService,
            DeviceIntelligenceAgentService deviceIntelligenceAgentService,
            ProjectManagerService projectManagerService,
            ProjectAnalysisAgentService projectAnalysisAgentService,
            ProjectPlanningService projectPlanningService,
            RenderWorkflowService renderWorkflowService,
            FramePilotProperties properties
    ) {
        this.agentCoordinatorService = agentCoordinatorService;
        this.deviceIntelligenceAgentService = deviceIntelligenceAgentService;
        this.projectManagerService = projectManagerService;
        this.projectAnalysisAgentService = projectAnalysisAgentService;
        this.projectPlanningService = projectPlanningService;
        this.renderWorkflowService = renderWorkflowService;
        this.properties = properties;
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("framepilot-ai-local-server-ok");
    }

    @GetMapping("/agents/contracts")
    public Mono<AgentContractsResponse> getAgentContracts() {
        return Mono.fromCallable(() -> AgentContractsResponse.from(agentCoordinatorService.contracts()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/agents/briefing")
    public Mono<AgentBriefingResponse> coordinateAgents(@RequestBody(required = false) AgentBriefingRequest request) {
        return Mono.fromCallable(() -> AgentBriefingResponse.from(agentCoordinatorService.coordinate(
                        request == null ? null : request.projectId(),
                        request == null ? null : request.scanId(),
                        request == null ? null : request.benchmarkId(),
                        request == null ? null : request.renderJobId(),
                        request == null ? null : request.question()
                )))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/device/scan")
    public Mono<DeviceScanResponse> scanDevice() {
        return Mono.fromCallable(() -> DeviceScanResponse.from(deviceIntelligenceAgentService.scanDevice()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/device/benchmark")
    public Mono<BenchmarkRunResponse> runBenchmark(@Valid @RequestBody(required = false) BenchmarkRequest request) {
        return Mono.fromCallable(() -> BenchmarkRunResponse.from(deviceIntelligenceAgentService.runBenchmark(request == null ? null : request.scanId())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/device/recommendation")
    public Mono<DeviceRecommendationResponse> getRecommendation(
            @RequestParam(required = false) UUID scanId,
            @RequestParam(required = false) UUID benchmarkId
    ) {
        return Mono.fromCallable(() -> DeviceRecommendationResponse.from(deviceIntelligenceAgentService.getRecommendation(scanId, benchmarkId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/device/explanation")
    public Mono<DeviceExplanationResponse> getExplanation(
            @RequestParam(required = false) UUID scanId,
            @RequestParam(required = false) UUID benchmarkId
    ) {
        return Mono.fromCallable(() -> DeviceExplanationResponse.from(deviceIntelligenceAgentService.explain(scanId, benchmarkId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects")
    public Mono<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return Mono.fromCallable(() -> ProjectResponse.from(projectManagerService.createProject(
                        request.name(),
                        request.description() == null ? "" : request.description(),
                        request.sourceType() == null || request.sourceType().isBlank() ? "image-sequence" : request.sourceType()
                )))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/import")
    public Mono<ProjectResponse> importProject(@Valid @RequestBody ImportProjectRequest request) {
        return Mono.fromCallable(() -> ProjectResponse.from(projectAnalysisAgentService.importProject(request.name(), request.assetPaths())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/{projectId}/assets/import")
    public Mono<ProjectResponse> importAssets(@PathVariable UUID projectId, @Valid @RequestBody ImportAssetsRequest request) {
        return Mono.fromCallable(() -> ProjectResponse.from(projectManagerService.importAssets(projectId, request.assetPaths())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/import-demo")
    public Mono<ProjectResponse> importDemoProject() {
        return Mono.fromCallable(() -> {
            Path demoRoot = Path.of(properties.getStorage().getRoot(), properties.getStorage().getDemoAssetsDir()).normalize();
            Files.createDirectories(demoRoot);
            List<String> demoAssets = Files.list(demoRoot)
                    .filter(Files::isRegularFile)
                    .map(path -> path.toAbsolutePath().toString())
                    .sorted()
                    .toList();
            return ProjectResponse.from(projectManagerService.importDemoProject(demoAssets));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/projects/{projectId}")
    public Mono<ProjectResponse> getProject(@PathVariable UUID projectId) {
        return Mono.fromCallable(() -> ProjectResponse.from(agentCoordinatorService.getProject(projectId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/projects/{projectId}/workspace")
    public Mono<ProjectWorkspaceResponse> getWorkspace(@PathVariable UUID projectId) {
        return Mono.fromCallable(() -> ProjectWorkspaceResponse.from(projectPlanningService.getWorkspace(projectId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/{projectId}/panels/parse")
    public Mono<PanelParseResponse> parsePanels(@PathVariable UUID projectId) {
        return Mono.fromCallable(() -> PanelParseResponse.from(projectId, projectPlanningService.parsePanels(projectId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/{projectId}/ocr/run")
    public Mono<OcrRunResponse> runOcr(@PathVariable UUID projectId) {
        return Mono.fromCallable(() -> OcrRunResponse.from(projectId, projectPlanningService.runOcr(projectId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/{projectId}/analysis")
    public Mono<ProjectAnalysisResponse> analyzeProject(@PathVariable UUID projectId) {
        return Mono.fromCallable(() -> new ProjectAnalysisResponse(agentCoordinatorService.analyzeProject(projectId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/{projectId}/analysis/summary")
    public Mono<ProjectAnalysisSummaryResponse> analyzeProjectSummary(@PathVariable UUID projectId) {
        return Mono.fromCallable(() -> ProjectAnalysisSummaryResponse.from(projectPlanningService.analyzeProject(projectId, deviceIntelligenceAgentService.scanAndAssess())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/{projectId}/scene-plan")
    public Mono<ScenePlanResponse> generateScenePlan(@PathVariable UUID projectId) {
        return Mono.fromCallable(() -> ScenePlanResponse.from(projectId, projectPlanningService.generateScenePlan(projectId, deviceIntelligenceAgentService.scanAndAssess())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/{projectId}/copilot/query")
    public Mono<CopilotResponse> queryCopilot(@PathVariable UUID projectId, @Valid @RequestBody CopilotQueryRequest request) {
        return Mono.fromCallable(() -> new CopilotResponse(projectId, projectPlanningService.queryCopilot(projectId, request.question())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/{projectId}/render")
    public Mono<RenderJobStatusResponse> renderProject(@PathVariable UUID projectId) {
        return Mono.fromCallable(() -> RenderJobStatusResponse.from(renderWorkflowService.startRender(projectId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/projects/{projectId}/render/start")
    public Mono<RenderJobStatusResponse> startRender(@PathVariable UUID projectId) {
        return Mono.fromCallable(() -> RenderJobStatusResponse.from(renderWorkflowService.startRender(projectId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/render/jobs/{jobId}")
    public Mono<RenderJobStatusResponse> getRenderStatus(@PathVariable UUID jobId) {
        return Mono.fromCallable(() -> RenderJobStatusResponse.from(renderWorkflowService.getJobSnapshot(jobId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/render/jobs/{jobId}/pause")
    public Mono<RenderJobStatusResponse> pauseRender(@PathVariable UUID jobId) {
        return Mono.fromCallable(() -> RenderJobStatusResponse.from(renderWorkflowService.pauseRender(jobId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/render/jobs/{jobId}/resume")
    public Mono<RenderJobStatusResponse> resumeRender(@PathVariable UUID jobId) {
        return Mono.fromCallable(() -> RenderJobStatusResponse.from(renderWorkflowService.resumeRender(jobId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/render/jobs/{jobId}/cancel")
    public Mono<RenderJobStatusResponse> cancelRender(@PathVariable UUID jobId) {
        return Mono.fromCallable(() -> RenderJobStatusResponse.from(renderWorkflowService.cancelRender(jobId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/render/jobs/{jobId}/runtime")
    public Mono<RenderRuntimeStatsResponse> getRenderRuntime(@PathVariable UUID jobId) {
        return Mono.fromCallable(() -> RenderRuntimeStatsResponse.from(jobId, renderWorkflowService.getRuntimeSamples(jobId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/render/jobs/{jobId}/artifacts")
    public Mono<RenderArtifactsResponse> getRenderArtifacts(@PathVariable UUID jobId) {
        return Mono.fromCallable(() -> RenderArtifactsResponse.from(jobId, renderWorkflowService.getArtifacts(jobId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(value = "/render/jobs/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<RenderJobStatusResponse> streamRenderStatus(@PathVariable UUID jobId) {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                .map(tick -> RenderJobStatusResponse.from(renderWorkflowService.getJobSnapshot(jobId)))
                .distinctUntilChanged(
                        response -> response.status() + "|" + response.checkpoint().progressPercent() + "|" + String.valueOf(response.checkpoint().currentShotId())
                )
                .takeUntil(response -> switch (response.status()) {
                    case "COMPLETED", "COMPLETED_WITH_FALLBACK", "FAILED", "CANCELLED" -> true;
                    default -> false;
                });
    }
}
