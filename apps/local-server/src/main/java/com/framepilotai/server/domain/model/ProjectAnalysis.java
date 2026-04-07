package com.framepilotai.server.domain.model;

import java.util.List;

public record ProjectAnalysis(
        ProjectRecord project,
        List<PanelData> panels,
        List<OcrBlock> ocrBlocks,
        List<SceneData> scenes,
        String projectSummary,
        String complexityLevel,
        PipelinePlan pipelinePlan,
        List<String> copilotNotes
) {
}
