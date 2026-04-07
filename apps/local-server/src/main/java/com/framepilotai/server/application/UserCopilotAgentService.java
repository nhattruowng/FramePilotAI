package com.framepilotai.server.application;

import com.framepilotai.server.domain.model.DeviceIntelligenceSnapshot;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.ProjectWorkspace;
import com.framepilotai.server.domain.model.RenderJobSnapshot;
import com.framepilotai.server.domain.model.agent.AgentAdvisory;
import com.framepilotai.server.domain.model.agent.AgentContractSpec;
import com.framepilotai.server.domain.model.agent.AgentType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class UserCopilotAgentService {

    public List<String> buildNotes(DeviceIntelligenceSnapshot snapshot, ProjectAnalysis analysis) {
        List<String> notes = new ArrayList<>();
        notes.add("Recommended preset is " + snapshot.preset().preset() + " based on tier " + snapshot.capability().tier() + ".");
        notes.add("Pipeline selected: " + analysis.pipelinePlan().pipelineType() + " with fallback level " + analysis.pipelinePlan().fallbackLevel() + ".");
        notes.add("Scene plan contains " + analysis.scenes().size() + " scenes and " + analysis.panels().size() + " panels.");
        if (!snapshot.capability().constraints().isEmpty()) {
            notes.add("Watch-outs: " + String.join(" | ", snapshot.capability().constraints()));
        }
        return List.copyOf(notes);
    }

    public String answer(ProjectWorkspace workspace, String question) {
        return answer(workspace, null, null, question);
    }

    public String answer(
            ProjectWorkspace workspace,
            DeviceIntelligenceSnapshot snapshot,
            RenderJobSnapshot renderSnapshot,
            String question
    ) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (normalized.contains("preset")) {
            return snapshot == null
                    ? "Device intelligence snapshot is unavailable. Run device analysis first to explain preset choice."
                    : "Preset "
                    + snapshot.preset().preset()
                    + " is recommended because device tier is "
                    + snapshot.capability().tier()
                    + " with score "
                    + snapshot.capability().score()
                    + ". Limits are "
                    + snapshot.preset().limits().outputWidth()
                    + "x"
                    + snapshot.preset().limits().outputHeight()
                    + " at "
                    + snapshot.preset().limits().fps()
                    + " fps.";
        }
        if (normalized.contains("fallback")) {
            return renderSnapshot == null
                    ? "No render snapshot is loaded, so there is no fallback explanation yet."
                    : "Fallback level "
                    + renderSnapshot.job().fallbackLevel()
                    + " is active. Reasons: "
                    + String.join(" | ", renderSnapshot.checkpoint().reasons());
        }
        if (normalized.contains("complex")) {
            return "Project complexity is "
                    + workspace.metadata().complexityLevel()
                    + ". Summary: "
                    + workspace.metadata().summary();
        }
        if (normalized.contains("scene")) {
            return "Project has "
                    + workspace.scenes().size()
                    + " scenes. First scene: "
                    + workspace.scenes().stream().findFirst().map(scene -> scene.title() + " with " + scene.shots().size() + " shots.").orElse("No scene plan yet.");
        }
        if (normalized.contains("ocr") || normalized.contains("text")) {
            return "OCR currently has "
                    + workspace.ocrBlocks().size()
                    + " text blocks. Adapter status is deterministic stub until native OCR is provisioned.";
        }
        if (normalized.contains("panel")) {
            return "Panel review contains "
                    + workspace.panels().size()
                    + " panels. Review state defaults to editable baseline output.";
        }
        if (normalized.contains("optim") || normalized.contains("improve")) {
            return optimizeProject(workspace, snapshot, renderSnapshot);
        }
        return "Project "
                + workspace.project().name()
                + " is in status "
                + workspace.project().status()
                + ". Summary: "
                + workspace.metadata().summary();
    }

    public AgentAdvisory advisory(
            ProjectWorkspace workspace,
            DeviceIntelligenceSnapshot snapshot,
            RenderJobSnapshot renderSnapshot,
            String question
    ) {
        List<String> explanations = new ArrayList<>();
        explanations.add(answer(workspace, snapshot, renderSnapshot, question));
        explanations.add("Copilot suggestions stay advisory only. Engines and guardrails keep the final decision.");

        List<String> warnings = new ArrayList<>();
        if (workspace.ocrBlocks().stream().anyMatch(block -> block.adapterName().contains("stub"))) {
            warnings.add("OCR still relies on a stub adapter, so dialogue timing may need manual review.");
        }
        if (renderSnapshot != null && renderSnapshot.job().status().name().contains("FALLBACK")) {
            warnings.add("Render used fallback behavior. Review artifact quality before release.");
        }

        List<String> recommendations = new ArrayList<>();
        recommendations.add(optimizeProject(workspace, snapshot, renderSnapshot));
        recommendations.add("Review long dialogue scenes first, because they dominate pacing and subtitle density.");

        return new AgentAdvisory(
                AgentType.USER_COPILOT,
                "Copilot ready with project explanation and optimization guidance.",
                0.76,
                List.copyOf(explanations),
                List.copyOf(warnings),
                List.copyOf(recommendations)
        );
    }

    public AgentContractSpec contract() {
        return new AgentContractSpec(
                AgentType.USER_COPILOT,
                "Turns device, project and render context into plain-language explanations and lightweight suggestions.",
                "{ workspace, deviceSnapshot?, renderSnapshot?, question? }",
                "{ summary, confidence, explanations[], warnings[], recommendations[] }",
                "plain-language answer -> caveats -> optimization suggestions",
                List.of("stub runtime notice", "fallback quality risk", "manual review reminder"),
                List.of("preset clarification", "complexity optimization", "render-safe cleanup"),
                List.of(
                        "Must not claim to override preset, pipeline, fallback or runtime decisions.",
                        "Must not propose cloud-only steps.",
                        "Copilot can explain and suggest, but rule engines remain authoritative."
                )
        );
    }

    private String optimizeProject(ProjectWorkspace workspace, DeviceIntelligenceSnapshot snapshot, RenderJobSnapshot renderSnapshot) {
        List<String> suggestions = new ArrayList<>();
        if (workspace.scenes().size() > 3) {
            suggestions.add("split the chapter into smaller render batches");
        }
        if (workspace.ocrBlocks().size() > workspace.panels().size()) {
            suggestions.add("trim dialogue-heavy panels before final timing");
        }
        if (snapshot != null && !snapshot.capability().constraints().isEmpty()) {
            suggestions.add("respect device constraints by keeping shorter shot batches");
        }
        if (renderSnapshot != null && renderSnapshot.job().fallbackLevel().ordinal() > 0) {
            suggestions.add("review fallback reasons and reduce effect density on the heaviest scenes");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("the current project is already in a stable baseline state for local render");
        }
        return "Optimization suggestions: " + String.join("; ", suggestions) + ".";
    }
}
