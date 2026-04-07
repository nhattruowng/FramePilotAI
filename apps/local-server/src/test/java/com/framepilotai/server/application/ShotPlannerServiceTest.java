package com.framepilotai.server.application;

import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShotPlannerServiceTest {

    private final ShotPlannerService service = new ShotPlannerService();

    @Test
    void shouldBuildSequentialTimelineForPanels() {
        var panels = List.of(
                new PanelData("panel-1", "asset-1", "page-1.png", 1, 0, 0, 1200, 800, "Opening beat", "editable-baseline"),
                new PanelData("panel-2", "asset-1", "page-1.png", 2, 0, 810, 1200, 800, "Dialogue beat", "editable-baseline")
        );
        var blocks = List.of(
                new OcrBlock("ocr-1", "panel-1", "Welcome to FramePilot.", 0.94, "en", "stub-local-ocr"),
                new OcrBlock("ocr-2", "panel-2", "We can animate this chapter.", 0.92, "en", "stub-local-ocr")
        );

        var shots = service.planShots("scene-1", panels, blocks, 1, 0);

        assertThat(shots).hasSize(2);
        assertThat(shots.get(0).orderIndex()).isEqualTo(1);
        assertThat(shots.get(1).orderIndex()).isEqualTo(2);
        assertThat(shots.get(0).timelineStartMillis()).isZero();
        assertThat(shots.get(0).timelineEndMillis()).isEqualTo(shots.get(1).timelineStartMillis());
        assertThat(shots.get(1).timelineEndMillis()).isGreaterThan(shots.get(1).timelineStartMillis());
        assertThat(shots.get(0).notes()).contains("Dialogue");
    }
}
