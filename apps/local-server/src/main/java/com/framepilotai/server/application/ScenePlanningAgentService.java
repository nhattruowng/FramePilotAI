package com.framepilotai.server.application;

import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.SceneData;
import com.framepilotai.server.domain.model.agent.AgentAdvisory;
import com.framepilotai.server.domain.model.agent.AgentContractSpec;
import com.framepilotai.server.domain.model.agent.AgentType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScenePlanningAgentService {

    private final ShotPlannerService shotPlannerService;

    public ScenePlanningAgentService(ShotPlannerService shotPlannerService) {
        this.shotPlannerService = shotPlannerService;
    }

    public List<SceneData> planScenes(List<PanelData> panels, List<OcrBlock> ocrBlocks) {
        List<SceneData> scenes = new ArrayList<>();
        int timelineCursor = 0;
        int shotOrder = 1;
        for (int index = 0; index < panels.size(); index += 2) {
            List<PanelData> scenePanels = panels.subList(index, Math.min(index + 2, panels.size()));
            PanelData anchorPanel = scenePanels.getFirst();
            String sceneId = "scene-" + anchorPanel.panelId();
            String dialogueSummary = ocrBlocks.stream()
                    .filter(block -> scenePanels.stream().anyMatch(panel -> panel.panelId().equals(block.panelId())))
                    .map(OcrBlock::text)
                    .reduce((left, right) -> left + " / " + right)
                    .orElse("No speech extracted. Use ambient pacing.");

            var shots = shotPlannerService.planShots(sceneId, scenePanels, ocrBlocks, shotOrder, timelineCursor);
            shotOrder += shots.size();
            timelineCursor = shots.isEmpty() ? timelineCursor : shots.get(shots.size() - 1).timelineEndMillis();
            int totalDuration = shots.stream().mapToInt(shot -> shot.durationMillis()).sum();
            double complexity = Math.min(1.0, (scenePanels.size() * 0.28) + (dialogueSummary.length() / 180.0));
            String cameraLevel = complexity > 0.65 ? "enhanced" : complexity > 0.38 ? "balanced" : "safe";

            scenes.add(new SceneData(
                    sceneId,
                    scenes.size() + 1,
                    "Scene " + (scenes.size() + 1),
                    dialogueSummary,
                    Math.round(complexity * 100d) / 100d,
                    cameraLevel,
                    totalDuration,
                    List.copyOf(shots)
            ));
        }
        return List.copyOf(scenes);
    }

    public AgentAdvisory advisory(List<SceneData> scenes) {
        int shotCount = scenes.stream().mapToInt(scene -> scene.shots().size()).sum();
        double averageComplexity = scenes.stream().mapToDouble(SceneData::complexityScore).average().orElse(0);
        List<String> warnings = new ArrayList<>();
        if (averageComplexity > 0.78) {
            warnings.add("Scene complexity is high. Watch runtime pressure and fallback events.");
        }
        if (shotCount > 16) {
            warnings.add("Shot count is high for a single render pass. More checkpoints are recommended.");
        }

        List<String> recommendations = new ArrayList<>();
        recommendations.add("Timeline currently has " + scenes.size() + " scenes and " + shotCount + " shots.");
        recommendations.add("Average scene complexity is " + Math.round(averageComplexity * 100d) / 100d + ".");
        recommendations.add("Review camera effect levels before final render confirmation.");

        return new AgentAdvisory(
                AgentType.SCENE_PLANNING,
                "Scene planning grouped the project into " + scenes.size() + " scenes with " + shotCount + " shots.",
                0.86,
                scenes.stream()
                        .limit(3)
                        .map(scene -> scene.title() + " -> " + scene.cameraEffectLevel() + " camera level, " + scene.totalDurationMillis() + " ms.")
                        .toList(),
                List.copyOf(warnings),
                List.copyOf(recommendations)
        );
    }

    public AgentContractSpec contract() {
        return new AgentContractSpec(
                AgentType.SCENE_PLANNING,
                "Explains grouping, pacing and camera intent for planned scenes and shots.",
                "{ scenes: SceneData[] }",
                "{ summary, confidence, explanations[], warnings[], recommendations[] }",
                "timeline summary -> pacing rationale -> review recommendations",
                List.of("high complexity", "large shot count", "aggressive camera effect"),
                List.of("checkpoint density", "camera cleanup", "chapter split"),
                List.of(
                        "Must not change timeline ordering on its own.",
                        "Must not override shot planner durations outside deterministic engine updates.",
                        "Advisory output only. Scene/shot engines decide the actual timeline."
                )
        );
    }
}
