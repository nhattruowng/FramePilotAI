package com.framepilotai.server.domain.model;

import java.util.List;

public record ProjectWorkspace(
        ProjectRecord project,
        ProjectMetadataRecord metadata,
        List<PanelData> panels,
        List<OcrBlock> ocrBlocks,
        List<SceneData> scenes
) {
}
