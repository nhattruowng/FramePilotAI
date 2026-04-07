package com.framepilotai.server.application;

import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ShotData;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShotPlannerService {

    public List<ShotData> planShots(String sceneId, List<PanelData> panels, List<OcrBlock> ocrBlocks, int startingOrder, int startingTimelineMs) {
        int timeline = startingTimelineMs;
        int order = startingOrder;
        java.util.ArrayList<ShotData> shots = new java.util.ArrayList<>();
        for (PanelData panel : panels) {
            String text = ocrBlocks.stream()
                    .filter(block -> block.panelId().equals(panel.panelId()))
                    .map(OcrBlock::text)
                    .findFirst()
                    .orElse("");
            int duration = text.isBlank() ? 2200 : Math.min(4200, 1800 + text.length() * 18);
            String effectLevel = duration > 3000 ? "medium" : "light";
            String cameraMove = switch (panel.readingOrder() % 3) {
                case 0 -> "pan-left";
                case 1 -> "push-in";
                default -> "hold";
            };
            ShotData shot = new ShotData(
                    "shot-" + panel.panelId(),
                    sceneId,
                    panel.panelId(),
                    order,
                    cameraMove,
                    duration,
                    effectLevel,
                    timeline,
                    timeline + duration,
                    text.isBlank() ? "Ambient transition beat." : "Dialogue-led emphasis."
            );
            order += 1;
            timeline += duration;
            shots.add(shot);
        }
        return shots;
    }
}
