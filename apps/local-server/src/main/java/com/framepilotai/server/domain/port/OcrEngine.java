package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectRecord;

import java.util.List;

public interface OcrEngine {
    List<OcrBlock> extract(ProjectRecord project, List<PanelData> panels);
}
