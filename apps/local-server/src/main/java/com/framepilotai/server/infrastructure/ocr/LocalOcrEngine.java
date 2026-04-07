package com.framepilotai.server.infrastructure.ocr;

import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.port.OcrAdapter;
import com.framepilotai.server.domain.port.OcrEngine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocalOcrEngine implements OcrEngine {

    private final OcrAdapter adapter;

    public LocalOcrEngine(OcrAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public List<OcrBlock> extract(ProjectRecord project, List<PanelData> panels) {
        return adapter.extract(project, panels);
    }
}
