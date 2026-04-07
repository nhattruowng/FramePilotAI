package com.framepilotai.server.application;

import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScenePlanningAgentServiceTest {

    private final ScenePlanningAgentService service = new ScenePlanningAgentService(new ShotPlannerService());

    @Test
    void shouldGroupPanelsIntoOrderedScenesWithShots() {
        var panels = List.of(
                new PanelData("panel-1", "asset-1", "page-1.png", 1, 0, 0, 1200, 800, "Intro", "editable-baseline"),
                new PanelData("panel-2", "asset-1", "page-1.png", 2, 0, 810, 1200, 800, "Response", "editable-baseline"),
                new PanelData("panel-3", "asset-2", "page-2.png", 3, 0, 0, 1200, 800, "Cliffhanger", "editable-baseline")
        );
        var blocks = List.of(
                new OcrBlock("ocr-1", "panel-1", "A short line.", 0.91, "en", "stub-local-ocr"),
                new OcrBlock("ocr-2", "panel-2", "Another short line.", 0.90, "en", "stub-local-ocr"),
                new OcrBlock("ocr-3", "panel-3", "A longer dramatic line for the final beat.", 0.88, "en", "stub-local-ocr")
        );

        var scenes = service.planScenes(panels, blocks);

        assertThat(scenes).hasSize(2);
        assertThat(scenes.get(0).sceneOrder()).isEqualTo(1);
        assertThat(scenes.get(0).shots()).hasSize(2);
        assertThat(scenes.get(1).sceneOrder()).isEqualTo(2);
        assertThat(scenes.get(1).shots()).hasSize(1);
        assertThat(scenes.get(1).shots().get(0).timelineStartMillis())
                .isEqualTo(scenes.get(0).shots().get(1).timelineEndMillis());
        assertThat(scenes.get(0).complexityScore()).isBetween(0.0, 1.0);
        assertThat(scenes.get(1).cameraEffectLevel()).isNotBlank();
    }
}
