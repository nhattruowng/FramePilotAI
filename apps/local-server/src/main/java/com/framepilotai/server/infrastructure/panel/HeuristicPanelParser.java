package com.framepilotai.server.infrastructure.panel;

import com.framepilotai.server.domain.model.AssetRecord;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.port.PanelParser;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class HeuristicPanelParser implements PanelParser {

    @Override
    public List<PanelData> parse(ProjectRecord project) {
        List<PanelData> panels = new ArrayList<>();
        int order = 1;
        for (AssetRecord asset : project.assets()) {
            int width = 1600;
            int height = 900;
            String assetKey = asset.id().toString();
            try {
                BufferedImage image = ImageIO.read(new File(asset.sourcePath()));
                if (image != null) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
            } catch (Exception ignored) {
                // Unsupported formats fall back to default dimensions for deterministic planning.
            }

            panels.add(new PanelData(
                    "panel-" + assetKey + "-1",
                    assetKey,
                    asset.sourcePath(),
                    order,
                    0,
                    0,
                    width,
                    height,
                    "Primary beat extracted from " + new File(asset.sourcePath()).getName(),
                    "editable-baseline"
            ));

            panels.add(new PanelData(
                    "panel-" + assetKey + "-2",
                    assetKey,
                    asset.sourcePath(),
                    order + 1,
                    width / 8,
                    height / 10,
                    width,
                    height,
                    "Secondary reaction beat extracted from " + new File(asset.sourcePath()).getName(),
                    "editable-baseline"
            ));
            order += 2;
        }
        return List.copyOf(panels);
    }
}
