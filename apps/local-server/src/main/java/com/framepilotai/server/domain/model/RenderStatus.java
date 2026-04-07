package com.framepilotai.server.domain.model;

public enum RenderStatus {
    QUEUED,
    STARTED,
    PAUSED,
    CANCELLING,
    CANCELLED,
    COMPLETED,
    COMPLETED_WITH_FALLBACK,
    FAILED
}
