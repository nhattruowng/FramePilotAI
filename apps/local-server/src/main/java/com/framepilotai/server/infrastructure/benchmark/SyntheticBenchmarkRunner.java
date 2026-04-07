package com.framepilotai.server.infrastructure.benchmark;

import com.framepilotai.server.domain.model.BenchmarkResult;
import com.framepilotai.server.domain.model.DeviceProfile;
import com.framepilotai.server.domain.port.BenchmarkRunner;
import com.framepilotai.server.domain.port.ExportService;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Component
public class SyntheticBenchmarkRunner implements BenchmarkRunner {

    private final ExportService exportService;

    public SyntheticBenchmarkRunner(ExportService exportService) {
        this.exportService = exportService;
    }

    @Override
    public BenchmarkResult run(DeviceProfile deviceProfile) {
        long startedAt = System.nanoTime();
        List<String> notes = new ArrayList<>();

        long cpuDuration = runCpuBenchmark();
        long imageDuration = runImageBenchmark();
        BenchmarkProbe encodeProbe = runEncodeBenchmark();
        BenchmarkProbe ocrProbe = runOcrInferenceStubBenchmark(deviceProfile);

        if (encodeProbe.note() != null) {
            notes.add(encodeProbe.note());
        }
        if (ocrProbe.note() != null) {
            notes.add(ocrProbe.note());
        }

        double cpuScore = scoreFromDuration(cpuDuration, 120, 2200);
        double imageScore = scoreFromDuration(imageDuration, 140, 1800);
        double encodeScore = scoreFromDuration(encodeProbe.durationMillis(), 220, 3600);
        double ocrScore = scoreFromDuration(ocrProbe.durationMillis(), 120, 2000);
        double aggregate = Math.round((cpuScore * 0.30 + imageScore * 0.20 + encodeScore * 0.30 + ocrScore * 0.20) * 10d) / 10d;
        long totalDuration = Math.round((System.nanoTime() - startedAt) / 1_000_000d);

        return new BenchmarkResult(
                cpuScore,
                imageScore,
                encodeScore,
                ocrScore,
                aggregate,
                totalDuration,
                encodeProbe.usedStub() || ocrProbe.usedStub(),
                List.copyOf(notes)
        );
    }

    private long runCpuBenchmark() {
        long startedAt = System.nanoTime();
        long state = 0x9E3779B97F4A7C15L;
        for (int index = 0; index < 2_400_000; index++) {
            state ^= (state << 13);
            state ^= (state >>> 7);
            state ^= (state << 17);
        }
        if (state == 0) {
            throw new IllegalStateException("CPU benchmark produced invalid state");
        }
        return Math.round((System.nanoTime() - startedAt) / 1_000_000d);
    }

    private long runImageBenchmark() {
        long startedAt = System.nanoTime();
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, new Color((x * 17) % 255, (y * 23) % 255, (x + y) % 255).getRGB());
            }
        }

        long checksum = 0;
        for (int pass = 0; pass < 8; pass++) {
            for (int y = 1; y < image.getHeight() - 1; y++) {
                for (int x = 1; x < image.getWidth() - 1; x++) {
                    int rgb = image.getRGB(x, y);
                    int mixed = ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
                    checksum += mixed * (pass + 1L);
                }
            }
        }
        if (checksum == 0) {
            throw new IllegalStateException("Image benchmark checksum is invalid");
        }
        return Math.round((System.nanoTime() - startedAt) / 1_000_000d);
    }

    private BenchmarkProbe runEncodeBenchmark() {
        try {
            var ffmpegExecutable = exportService.resolveFfmpegExecutable();
            if (ffmpegExecutable.isPresent()) {
                long startedAt = System.nanoTime();
                Process process = new ProcessBuilder(
                        ffmpegExecutable.get().toString(),
                        "-hide_banner",
                        "-loglevel",
                        "error",
                        "-f",
                        "lavfi",
                        "-t",
                        "1.5",
                        "-i",
                        "color=c=0x111827:s=1280x720:r=24",
                        "-f",
                        "null",
                        "-"
                ).start();
                int exitCode = process.waitFor();
                long duration = Math.round((System.nanoTime() - startedAt) / 1_000_000d);
                if (exitCode == 0) {
                    return new BenchmarkProbe(duration, false, "Encode benchmark used FFmpeg dry-run.");
                }
            }
        } catch (Exception ignored) {
            // Falls back to deterministic stub below.
        }

        long simulatedDuration = 900L;
        return new BenchmarkProbe(simulatedDuration, true, "Encode benchmark used deterministic stub because FFmpeg dry-run was unavailable.");
    }

    private BenchmarkProbe runOcrInferenceStubBenchmark(DeviceProfile deviceProfile) {
        long startedAt = System.nanoTime();
        double accumulator = 0;
        int loops = Math.max(120_000, deviceProfile.cpu().logicalCores() * 80_000);
        for (int index = 0; index < loops; index++) {
            accumulator += Math.sin(index * 0.0007d) * Math.cos(index * 0.0011d);
        }
        if (Double.isNaN(accumulator)) {
            throw new IllegalStateException("OCR benchmark accumulator is invalid");
        }
        long duration = Math.round((System.nanoTime() - startedAt) / 1_000_000d);
        return new BenchmarkProbe(duration, true, "OCR/inference benchmark ran with a local stub adapter until OCR runtime is provisioned.");
    }

    private double scoreFromDuration(long durationMillis, long bestMillis, long worstMillis) {
        if (durationMillis <= bestMillis) {
            return 100d;
        }
        if (durationMillis >= worstMillis) {
            return 18d;
        }
        double ratio = (double) (durationMillis - bestMillis) / (worstMillis - bestMillis);
        double score = 100d - ratio * 82d;
        return Math.round(score * 10d) / 10d;
    }

    private record BenchmarkProbe(long durationMillis, boolean usedStub, String note) {
    }
}
