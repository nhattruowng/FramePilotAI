package com.framepilotai.server.infrastructure.render;

import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.RenderOutcome;
import com.framepilotai.server.domain.model.RenderPreparation;
import com.framepilotai.server.domain.model.RenderProfile;
import com.framepilotai.server.domain.model.RenderStatus;
import com.framepilotai.server.domain.model.RenderedShot;
import com.framepilotai.server.domain.model.SceneData;
import com.framepilotai.server.domain.model.ShotData;
import com.framepilotai.server.domain.port.ExportService;
import com.framepilotai.server.domain.port.RenderOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class FfmpegRenderOrchestrator implements RenderOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(FfmpegRenderOrchestrator.class);

    private final ExportService exportService;

    public FfmpegRenderOrchestrator(ExportService exportService) {
        this.exportService = exportService;
    }

    @Override
    public RenderPreparation prepare(UUID jobId, ProjectRecord project, ProjectAnalysis analysis, PipelinePlan plan) throws Exception {
        Path jobDirectory = exportService.createJobDirectory(project.id(), jobId);
        Path storyPackage = exportService.writeStoryPackage(project.id(), jobId, analysis);
        Path subtitleFile = exportService.writeSubtitleFile(project.id(), jobId, analysis.scenes());
        Path ttsHook = exportService.writeTtsHook(project.id(), jobId, analysis);
        Path audioHook = exportService.writeAudioHook(project.id(), jobId, analysis);
        return new RenderPreparation(jobId, project.id(), jobDirectory, storyPackage, subtitleFile, ttsHook, audioHook);
    }

    @Override
    public RenderedShot renderShot(RenderPreparation preparation, SceneData scene, ShotData shot, ProjectAnalysis analysis, PipelinePlan plan, RenderProfile profile) throws Exception {
        Path ffmpegExecutable = exportService.resolveFfmpegExecutable()
                .orElseThrow(() -> new IllegalStateException("FFmpeg executable is not available for local render."));
        Path shotVideo = preparation.jobDirectory().resolve(shot.shotId() + ".mp4");
        Path ffmpegLog = preparation.jobDirectory().resolve(shot.shotId() + ".ffmpeg.log");
        Path previewImage = preparation.jobDirectory().resolve(shot.shotId() + ".png");

        int durationMillis = Math.min(profile.maxShotDurationMillis(), Math.max(1200, (int) Math.round(shot.durationMillis() * profile.shotDurationScale())));
        int frameCount = Math.max(profile.fps(), (int) Math.ceil(durationMillis / 1000d * profile.fps()));
        BufferedImage card = buildShotCard(scene, shot, analysis, plan, profile);
        ImageIO.write(card, "png", previewImage.toFile());
        Path frameDirectory = preparation.jobDirectory().resolve(shot.shotId() + "-frames");
        Files.createDirectories(frameDirectory);
        generateShotFrames(card, frameDirectory, shot, profile, frameCount);

        Process process = new ProcessBuilder(buildShotCommand(ffmpegExecutable, frameDirectory, shotVideo, ffmpegLog, profile))
                .directory(preparation.jobDirectory().toFile())
                .redirectErrorStream(true)
                .redirectOutput(ffmpegLog.toFile())
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0 || !Files.exists(shotVideo)) {
            throw new IllegalStateException("FFmpeg shot render failed for " + shot.shotId() + ". Log: " + ffmpegLog);
        }

        return new RenderedShot(shot.shotId(), shotVideo, durationMillis);
    }

    @Override
    public Path muxFinal(RenderPreparation preparation, List<RenderedShot> renderedShots, ProjectAnalysis analysis, PipelinePlan plan, RenderProfile profile) throws Exception {
        Path ffmpegExecutable = exportService.resolveFfmpegExecutable()
                .orElseThrow(() -> new IllegalStateException("FFmpeg executable is not available for final mux."));
        Path manifest = exportService.writeConcatManifest(preparation.projectId(), preparation.jobId(), renderedShots);
        Path outputFile = preparation.jobDirectory().resolve("framepilot-demo.mp4");
        Path ffmpegLog = preparation.jobDirectory().resolve("final-mux.ffmpeg.log");
        Process process = new ProcessBuilder(List.of(
                ffmpegExecutable.toString(),
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", manifest.toString(),
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-movflags", "+faststart",
                outputFile.toString()
        ))
                .directory(preparation.jobDirectory().toFile())
                .redirectErrorStream(true)
                .redirectOutput(ffmpegLog.toFile())
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0 || !exportService.verifyArtifact(outputFile)) {
            throw new IllegalStateException("FFmpeg final mux failed. Log: " + ffmpegLog);
        }
        return outputFile;
    }

    @Override
    public Optional<RenderOutcome> fallbackPackage(RenderPreparation preparation, ProjectAnalysis analysis, PipelinePlan plan) throws Exception {
        if (!exportService.verifyArtifact(preparation.storyPackagePath())) {
            return Optional.empty();
        }
        return Optional.of(new RenderOutcome(
                preparation.jobId(),
                preparation.projectId(),
                RenderStatus.COMPLETED_WITH_FALLBACK,
                preparation.storyPackagePath().toString(),
                "application/json",
                plan,
                List.of(
                        "FFmpeg render path is unavailable. Returning deterministic story package fallback.",
                        "Subtitle manifest: " + preparation.subtitleFilePath(),
                        "TTS hook: " + preparation.ttsHookPath(),
                        "Audio hook: " + preparation.audioHookPath()
                ),
                null
        ));
    }

    private List<String> buildShotCommand(Path ffmpegExecutable, Path frameDirectory, Path shotVideo, Path ffmpegLog, RenderProfile profile) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegExecutable.toString());
        command.add("-y");
        command.add("-framerate");
        command.add(Integer.toString(profile.fps()));
        command.add("-i");
        command.add(frameDirectory.resolve("frame-%04d.png").toString());
        command.add("-an");
        command.add("-c:v");
        command.add("libx264");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add(shotVideo.toString());
        return command;
    }

    private BufferedImage buildShotCard(SceneData scene, ShotData shot, ProjectAnalysis analysis, PipelinePlan plan, RenderProfile profile) throws Exception {
        int canvasWidth = Math.max(profile.outputWidth() + 240, (int) Math.round(profile.outputWidth() * 1.2));
        int canvasHeight = Math.max(profile.outputHeight() + 120, (int) Math.round(profile.outputHeight() * 1.15));
        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setPaint(new GradientPaint(0, 0, new Color(12, 23, 42), canvasWidth, canvasHeight, pickColor(shot.orderIndex())));
            graphics.fillRect(0, 0, canvasWidth, canvasHeight);

            drawPanelPreview(graphics, canvasWidth, canvasHeight, analysis.panels().stream()
                    .filter(panel -> panel.panelId().equals(shot.panelId()))
                    .findFirst()
                    .orElse(null));

            graphics.setColor(new Color(248, 250, 252));
            graphics.setFont(new Font("Segoe UI", Font.BOLD, 34));
            graphics.drawString(scene.title(), 64, 72);

            graphics.setFont(new Font("Segoe UI", Font.PLAIN, 22));
            graphics.setColor(new Color(191, 219, 254));
            graphics.drawString("Pipeline " + plan.pipelineType() + " | " + shot.cameraMove() + " | " + shot.effectLevel(), 64, 108);

            graphics.setFont(new Font("Segoe UI", Font.PLAIN, 20));
            graphics.setColor(new Color(226, 232, 240));
            drawWrappedText(graphics, scene.narrative(), 64, canvasHeight - 190, canvasWidth - 128, 28);

            String subtitle = exportService.sanitizeText(shot.notes());
            if (profile.subtitleOverlayEnabled()) {
                graphics.setColor(new Color(15, 23, 42, 200));
                graphics.fill(new RoundRectangle2D.Double(48, canvasHeight - 116, canvasWidth - 96, 64, 18, 18));
                graphics.setColor(Color.WHITE);
                graphics.setFont(new Font("Segoe UI", Font.BOLD, 22));
                drawWrappedText(graphics, subtitle, 72, canvasHeight - 78, canvasWidth - 144, 26);
            }

            graphics.setColor(new Color(148, 163, 184));
            graphics.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            graphics.drawString("Generated " + Instant.now(), 64, canvasHeight - 18);
        } finally {
            graphics.dispose();
        }
        return canvas;
    }

    private void generateShotFrames(BufferedImage card, Path frameDirectory, ShotData shot, RenderProfile profile, int frameCount) throws Exception {
        for (int index = 0; index < frameCount; index++) {
            double progress = frameCount <= 1 ? 1.0 : (double) index / (frameCount - 1);
            BufferedImage frame = new BufferedImage(profile.outputWidth(), profile.outputHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = frame.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                double zoom = switch (shot.cameraMove()) {
                    case "push-in" -> 1.0 + (profile.zoomIntensity() * progress);
                    case "pan-left" -> 1.0 + (profile.zoomIntensity() * 0.35);
                    default -> 1.0 + (profile.zoomIntensity() * 0.12);
                };
                int cropWidth = Math.max(profile.outputWidth(), (int) Math.round(card.getWidth() / zoom));
                int cropHeight = Math.max(profile.outputHeight(), (int) Math.round(card.getHeight() / zoom));
                int maxX = Math.max(0, card.getWidth() - cropWidth);
                int maxY = Math.max(0, card.getHeight() - cropHeight);
                int cropX = switch (shot.cameraMove()) {
                    case "pan-left" -> (int) Math.round(maxX * (1.0 - progress));
                    default -> maxX / 2;
                };
                int cropY = maxY / 2;

                graphics.drawImage(
                        card,
                        0,
                        0,
                        profile.outputWidth(),
                        profile.outputHeight(),
                        cropX,
                        cropY,
                        cropX + cropWidth,
                        cropY + cropHeight,
                        null
                );
            } finally {
                graphics.dispose();
            }
            ImageIO.write(frame, "png", frameDirectory.resolve(String.format(Locale.ROOT, "frame-%04d.png", index + 1)).toFile());
        }
    }

    private void drawPanelPreview(Graphics2D graphics, int canvasWidth, int canvasHeight, PanelData panel) throws Exception {
        int previewX = 64;
        int previewY = 140;
        int previewWidth = canvasWidth - 128;
        int previewHeight = canvasHeight - 300;
        graphics.setColor(new Color(15, 23, 42, 185));
        graphics.fill(new RoundRectangle2D.Double(previewX, previewY, previewWidth, previewHeight, 28, 28));
        graphics.setStroke(new BasicStroke(3f));
        graphics.setColor(new Color(251, 191, 36, 180));
        graphics.draw(new RoundRectangle2D.Double(previewX, previewY, previewWidth, previewHeight, 28, 28));

        if (panel == null) {
            return;
        }

        BufferedImage source = null;
        try {
            source = ImageIO.read(new File(panel.assetPath()));
        } catch (Exception ignored) {
            // Unsupported formats intentionally fall back to placeholder cards.
        }

        if (source != null) {
            int targetWidth = previewWidth - 40;
            int targetHeight = previewHeight - 40;
            graphics.drawImage(source, previewX + 20, previewY + 20, targetWidth, targetHeight, null);
        } else {
            graphics.setColor(new Color(30, 41, 59));
            graphics.fill(new RoundRectangle2D.Double(previewX + 20, previewY + 20, previewWidth - 40, previewHeight - 40, 18, 18));
            graphics.setColor(new Color(226, 232, 240));
            graphics.setFont(new Font("Segoe UI", Font.BOLD, 26));
            graphics.drawString("Asset Preview Placeholder", previewX + 44, previewY + 72);
            graphics.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            drawWrappedText(graphics, panel.assetPath(), previewX + 44, previewY + 112, previewWidth - 88, 24);
            graphics.drawString("Panel " + panel.readingOrder() + " | crop " + panel.cropX() + "," + panel.cropY() + " | " + panel.width() + "x" + panel.height(), previewX + 44, previewY + 168);
            drawWrappedText(graphics, panel.summary(), previewX + 44, previewY + 210, previewWidth - 88, 24);
        }
    }

    private void drawWrappedText(Graphics2D graphics, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text == null || text.isBlank()) {
            return;
        }
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int currentY = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (graphics.getFontMetrics().stringWidth(candidate) > maxWidth) {
                graphics.drawString(line.toString(), x, currentY);
                currentY += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            graphics.drawString(line.toString(), x, currentY);
        }
    }

    private Color pickColor(int index) {
        Color[] palette = new Color[]{
                new Color(30, 41, 59),
                new Color(15, 118, 110),
                new Color(124, 45, 18),
                new Color(55, 48, 163),
                new Color(21, 128, 61),
                new Color(127, 29, 29)
        };
        return palette[Math.floorMod(index, palette.length)];
    }
}
