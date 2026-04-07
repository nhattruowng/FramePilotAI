package com.framepilotai.server.infrastructure.ocr;

import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.port.OcrAdapter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
@Primary
public class StubOcrAdapter implements OcrAdapter {

    @Override
    public String name() {
        return "stub-local-ocr";
    }

    @Override
    public List<OcrBlock> extract(ProjectRecord project, List<PanelData> panels) {
        // TODO: Replace with PaddleOCR CLI/service or ONNX OCR adapter when native runtime is provisioned.
        return panels.stream()
                .map(panel -> new OcrBlock(
                        UUID.randomUUID().toString(),
                        panel.panelId(),
                        "Dialogue from " + Path.of(panel.assetPath()).getFileName() + " in panel " + panel.readingOrder(),
                        0.93,
                        "vi",
                        name()
                ))
                .toList();
    }
}
