package com.framepilotai.server.application;

import com.framepilotai.server.common.error.DomainException;
import com.framepilotai.server.domain.model.BenchmarkRunRecord;
import com.framepilotai.server.domain.model.DeviceExplanation;
import com.framepilotai.server.domain.model.DeviceIntelligenceSnapshot;
import com.framepilotai.server.domain.model.DeviceRecommendation;
import com.framepilotai.server.domain.model.DeviceScanRecord;
import com.framepilotai.server.domain.model.agent.AgentAdvisory;
import com.framepilotai.server.domain.model.agent.AgentContractSpec;
import com.framepilotai.server.domain.model.agent.AgentType;
import com.framepilotai.server.domain.port.BenchmarkRunner;
import com.framepilotai.server.domain.port.CapabilityEngine;
import com.framepilotai.server.domain.port.DeviceIntelligenceRepository;
import com.framepilotai.server.domain.port.DeviceScanner;
import com.framepilotai.server.domain.port.PresetEngine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DeviceIntelligenceAgentService {

    private final DeviceScanner deviceScanner;
    private final BenchmarkRunner benchmarkRunner;
    private final CapabilityEngine capabilityEngine;
    private final PresetEngine presetEngine;
    private final DeviceIntelligenceRepository deviceIntelligenceRepository;

    public DeviceIntelligenceAgentService(
            DeviceScanner deviceScanner,
            BenchmarkRunner benchmarkRunner,
            CapabilityEngine capabilityEngine,
            PresetEngine presetEngine,
            DeviceIntelligenceRepository deviceIntelligenceRepository
    ) {
        this.deviceScanner = deviceScanner;
        this.benchmarkRunner = benchmarkRunner;
        this.capabilityEngine = capabilityEngine;
        this.presetEngine = presetEngine;
        this.deviceIntelligenceRepository = deviceIntelligenceRepository;
    }

    public DeviceScanRecord scanDevice() {
        return deviceIntelligenceRepository.saveScan(deviceScanner.scan());
    }

    public BenchmarkRunRecord runBenchmark(UUID scanId) {
        DeviceScanRecord scan = resolveScan(scanId);
        return deviceIntelligenceRepository.saveBenchmark(scan.id(), benchmarkRunner.run(scan.profile()));
    }

    public DeviceRecommendation getRecommendation(UUID scanId, UUID benchmarkId) {
        DeviceScanRecord scan = resolveScan(scanId);
        BenchmarkRunRecord benchmark = resolveBenchmark(benchmarkId);
        if (!benchmark.scanId().equals(scan.id())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Benchmark does not belong to the requested device scan.");
        }

        var capability = capabilityEngine.assess(scan.profile(), benchmark.result());
        var preset = presetEngine.recommend(capability);
        String summary = buildSummary(scan, benchmark, capability, preset);
        List<String> explanation = buildExplanation(scan, benchmark, capability, preset);

        return new DeviceRecommendation(
                scan.id(),
                benchmark.id(),
                scan.profile(),
                benchmark.result(),
                capability,
                preset,
                summary,
                explanation
        );
    }

    public DeviceExplanation explain(UUID scanId, UUID benchmarkId) {
        DeviceRecommendation recommendation = getRecommendation(scanId, benchmarkId);
        return new DeviceExplanation(
                recommendation.scanId(),
                recommendation.benchmarkId(),
                recommendation.preset().headline(),
                recommendation.summary(),
                recommendation.explanation()
        );
    }

    public DeviceIntelligenceSnapshot scanAndAssess() {
        DeviceScanRecord scan = scanDevice();
        BenchmarkRunRecord benchmark = runBenchmark(scan.id());
        DeviceRecommendation recommendation = getRecommendation(scan.id(), benchmark.id());
        return snapshotFromRecommendation(recommendation);
    }

    public DeviceIntelligenceSnapshot snapshot(UUID scanId, UUID benchmarkId) {
        return snapshotFromRecommendation(getRecommendation(scanId, benchmarkId));
    }

    private DeviceIntelligenceSnapshot snapshotFromRecommendation(DeviceRecommendation recommendation) {
        return new DeviceIntelligenceSnapshot(
                recommendation.scanId(),
                recommendation.benchmarkId(),
                recommendation.deviceProfile(),
                recommendation.benchmark(),
                recommendation.capability(),
                recommendation.preset(),
                recommendation.summary(),
                recommendation.explanation()
        );
    }

    public AgentAdvisory advisory(UUID scanId, UUID benchmarkId) {
        DeviceRecommendation recommendation = getRecommendation(scanId, benchmarkId);
        List<String> warnings = new ArrayList<>(recommendation.capability().constraints());
        if (recommendation.benchmark().usedStub()) {
            warnings.add("OCR and inference benchmarking currently includes stubbed runtime behavior.");
        }
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Default preset is " + recommendation.preset().preset() + " with pipeline " + recommendation.preset().defaultPipeline() + ".");
        recommendations.add("Keep shot batches within " + recommendation.preset().limits().checkpointIntervalShots() + " shots per checkpoint.");
        if (recommendation.capability().tier().name().equals("LOW")) {
            recommendations.add("Prefer motion-comic heavy sequences and avoid aggressive AI ratio.");
        }

        double confidence = recommendation.benchmark().usedStub() ? 0.72 : 0.9;
        return new AgentAdvisory(
                AgentType.DEVICE_INTELLIGENCE,
                recommendation.summary(),
                confidence,
                recommendation.explanation(),
                List.copyOf(warnings),
                List.copyOf(recommendations)
        );
    }

    public AgentContractSpec contract() {
        return new AgentContractSpec(
                AgentType.DEVICE_INTELLIGENCE,
                "Turns hardware scan and benchmark results into advisory capability summaries.",
                "{ scanId?: uuid, benchmarkId?: uuid }",
                "{ summary, confidence, explanations[], warnings[], recommendations[] }",
                "summary -> reasons -> actionable limits",
                List.of("hardware constraints", "stub adapter notice", "fallback risk"),
                List.of("preset choice", "checkpoint interval", "runtime-safe operating envelope"),
                List.of(
                        "Must not override capability engine score or preset engine limits.",
                        "Must not invent cloud-only remediation paths.",
                        "Advisory output only. Runtime and rule engines remain the final authority."
                )
        );
    }

    private DeviceScanRecord resolveScan(UUID scanId) {
        if (scanId != null) {
            return deviceIntelligenceRepository.findScan(scanId)
                    .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Device scan not found: " + scanId));
        }
        return deviceIntelligenceRepository.findLatestScan()
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "No device scan found. Run device scan first."));
    }

    private BenchmarkRunRecord resolveBenchmark(UUID benchmarkId) {
        if (benchmarkId != null) {
            return deviceIntelligenceRepository.findBenchmark(benchmarkId)
                    .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Benchmark run not found: " + benchmarkId));
        }
        return deviceIntelligenceRepository.findLatestBenchmark()
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "No benchmark result found. Run benchmark first."));
    }

    private String buildSummary(DeviceScanRecord scan, BenchmarkRunRecord benchmark, com.framepilotai.server.domain.model.CapabilityAssessment capability, com.framepilotai.server.domain.model.PresetRecommendation preset) {
        return preset.headline()
                + " for "
                + scan.profile().cpu().logicalCores()
                + " logical cores, "
                + String.format(Locale.ROOT, "%.1f", benchmark.result().aggregateScore())
                + " aggregate benchmark, tier "
                + capability.tier()
                + ".";
    }

    private List<String> buildExplanation(DeviceScanRecord scan, BenchmarkRunRecord benchmark, com.framepilotai.server.domain.model.CapabilityAssessment capability, com.framepilotai.server.domain.model.PresetRecommendation preset) {
        List<String> explanation = new ArrayList<>();
        explanation.add("Capability score is " + capability.score() + ", which maps to tier " + capability.tier() + ".");
        explanation.add("Benchmark breakdown: CPU " + benchmark.result().cpuTaskScore() + ", image " + benchmark.result().imageProcessingScore() + ", encode " + benchmark.result().encodeScore() + ", OCR/inference " + benchmark.result().ocrInferenceScore() + ".");
        explanation.add("Preset limits: " + preset.limits().outputWidth() + "x" + preset.limits().outputHeight() + " at " + preset.limits().fps() + " fps, max shot " + preset.limits().maxShotLengthMillis() + " ms, AI ratio " + preset.limits().aiShotRatio() + ".");
        explanation.addAll(preset.rationale());
        if (!scan.profile().gpus().isEmpty()) {
            explanation.add("GPU detection strategy returned " + scan.profile().gpus().size() + " profile(s).");
        }
        return List.copyOf(explanation);
    }
}
