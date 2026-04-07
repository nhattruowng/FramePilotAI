package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectRecord;

import java.util.List;

public interface PanelParser {
    List<PanelData> parse(ProjectRecord project);
}
