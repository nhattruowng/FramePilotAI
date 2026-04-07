package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.RuntimeSnapshot;

public interface RuntimeMonitor {
    RuntimeSnapshot capture();
}
