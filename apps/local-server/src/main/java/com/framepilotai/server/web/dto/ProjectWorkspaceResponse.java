package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectWorkspace;
import com.framepilotai.server.domain.model.SceneData;

import java.time.Instant;
import java.util.List;

public record ProjectWorkspaceResponse(
        ProjectResponse project,
        MetadataDto metadata,
        List<PanelDto> panels,
        List<OcrDto> ocrBlocks,
        List<SceneDto> scenes
) {
    public static ProjectWorkspaceResponse from(ProjectWorkspace workspace) {
        return new ProjectWorkspaceResponse(
                ProjectResponse.from(workspace.project()),
                new MetadataDto(
                        workspace.metadata().sourceType(),
                        workspace.metadata().description(),
                        workspace.metadata().summary(),
                        workspace.metadata().complexityLevel(),
                        workspace.metadata().panelCount(),
                        workspace.metadata().sceneCount(),
                        workspace.metadata().updatedAt()
                ),
                workspace.panels().stream().map(PanelDto::from).toList(),
                workspace.ocrBlocks().stream().map(OcrDto::from).toList(),
                workspace.scenes().stream().map(SceneDto::from).toList()
        );
    }

    public record MetadataDto(
            String sourceType,
            String description,
            String summary,
            String complexityLevel,
            int panelCount,
            int sceneCount,
            Instant updatedAt
    ) {
    }

    public record PanelDto(
            String panelId,
            String assetId,
            String assetPath,
            int readingOrder,
            int cropX,
            int cropY,
            int width,
            int height,
            String summary,
            String reviewState
    ) {
        static PanelDto from(PanelData panel) {
            return new PanelDto(panel.panelId(), panel.assetId(), panel.assetPath(), panel.readingOrder(), panel.cropX(), panel.cropY(), panel.width(), panel.height(), panel.summary(), panel.reviewState());
        }
    }

    public record OcrDto(
            String ocrId,
            String panelId,
            String text,
            double confidence,
            String language,
            String adapterName
    ) {
        static OcrDto from(OcrBlock block) {
            return new OcrDto(block.ocrId(), block.panelId(), block.text(), block.confidence(), block.language(), block.adapterName());
        }
    }

    public record SceneDto(
            String sceneId,
            int sceneOrder,
            String title,
            String narrative,
            double complexityScore,
            String cameraEffectLevel,
            int totalDurationMillis,
            List<ShotDto> shots
    ) {
        static SceneDto from(SceneData scene) {
            return new SceneDto(
                    scene.sceneId(),
                    scene.sceneOrder(),
                    scene.title(),
                    scene.narrative(),
                    scene.complexityScore(),
                    scene.cameraEffectLevel(),
                    scene.totalDurationMillis(),
                    scene.shots().stream().map(ShotDto::from).toList()
            );
        }
    }

    public record ShotDto(
            String shotId,
            String sceneId,
            String panelId,
            int orderIndex,
            String cameraMove,
            int durationMillis,
            String effectLevel,
            int timelineStartMillis,
            int timelineEndMillis,
            String notes
    ) {
        static ShotDto from(com.framepilotai.server.domain.model.ShotData shot) {
            return new ShotDto(shot.shotId(), shot.sceneId(), shot.panelId(), shot.orderIndex(), shot.cameraMove(), shot.durationMillis(), shot.effectLevel(), shot.timelineStartMillis(), shot.timelineEndMillis(), shot.notes());
        }
    }
}
