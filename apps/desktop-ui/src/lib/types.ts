export type CapabilityTier = "LOW" | "MEDIUM" | "HIGH";
export type PresetName = "ECO" | "BALANCED" | "QUALITY";
export type PipelineType = "MOTION_COMIC" | "HYBRID_LITE" | "HYBRID" | "ENHANCED_HYBRID";

export interface DeviceScanResponse {
  scanId: string;
  scannedAt: string;
  device: {
    deviceId: string;
    osName: string;
    osVersion: string;
    cpu: { model: string; physicalCores: number; logicalCores: number; systemLoadPercent: number };
    memory: { totalBytes: number; availableBytes: number };
    gpus: Array<{ vendor: string; name: string; vramBytes: number; hardwareAcceleration: boolean }>;
    disks: Array<{ name: string; totalBytes: number; freeBytes: number }>;
    capturedAt: string;
  };
}

export interface BenchmarkRunResponse {
  benchmarkId: string;
  scanId: string;
  benchmark: {
    cpuTaskScore: number;
    imageProcessingScore: number;
    encodeScore: number;
    ocrInferenceScore: number;
    aggregateScore: number;
    benchmarkDurationMillis: number;
    usedStub: boolean;
    notes: string[];
  };
  executedAt: string;
}

export interface DeviceRecommendationResponse {
  scanId: string;
  benchmarkId: string;
  device: DeviceScanResponse["device"];
  benchmark: BenchmarkRunResponse["benchmark"];
  capability: {
    tier: CapabilityTier;
    score: number;
    constraints: string[];
  };
  preset: {
    preset: PresetName;
    defaultPipeline: PipelineType;
    limits: {
      outputWidth: number;
      outputHeight: number;
      fps: number;
      maxShotLengthMillis: number;
      aiShotRatio: number;
      checkpointIntervalShots: number;
    };
    headline: string;
    rationale: string[];
  };
  summary: string;
  explanation: string[];
}

export interface DeviceExplanationResponse {
  scanId: string;
  benchmarkId: string;
  headline: string;
  summary: string;
  reasons: string[];
}

export interface ProjectResponse {
  id: string;
  name: string;
  status: "DRAFT" | "PLANNING" | "READY" | "RENDERED";
  createdAt: string;
  updatedAt: string;
  assets: Array<{ id: string; sourcePath: string; mediaType: string; sizeBytes: number; assetOrder: number }>;
}

export interface ProjectWorkspaceResponse {
  project: ProjectResponse;
  metadata: {
    sourceType: string;
    description: string;
    summary: string;
    complexityLevel: string;
    panelCount: number;
    sceneCount: number;
    updatedAt: string;
  };
  panels: Array<{
    panelId: string;
    assetId: string;
    assetPath: string;
    readingOrder: number;
    cropX: number;
    cropY: number;
    width: number;
    height: number;
    summary: string;
    reviewState: string;
  }>;
  ocrBlocks: Array<{
    ocrId: string;
    panelId: string;
    text: string;
    confidence: number;
    language: string;
    adapterName: string;
  }>;
  scenes: Array<{
    sceneId: string;
    sceneOrder: number;
    title: string;
    narrative: string;
    complexityScore: number;
    cameraEffectLevel: string;
    totalDurationMillis: number;
    shots: Array<{
      shotId: string;
      sceneId: string;
      panelId: string;
      orderIndex: number;
      cameraMove: string;
      durationMillis: number;
      effectLevel: string;
      timelineStartMillis: number;
      timelineEndMillis: number;
      notes: string;
    }>;
  }>;
}

export interface PanelParseResponse {
  projectId: string;
  panelCount: number;
  panels: ProjectWorkspaceResponse["panels"];
}

export interface OcrRunResponse {
  projectId: string;
  blockCount: number;
  ocrBlocks: ProjectWorkspaceResponse["ocrBlocks"];
}

export interface ProjectAnalysisSummaryResponse {
  projectId: string;
  projectSummary: string;
  complexityLevel: string;
  status: string;
  preset: PresetName;
  pipelineType: PipelineType;
  copilotNotes: string[];
}

export interface ScenePlanResponse {
  projectId: string;
  sceneCount: number;
  scenes: ProjectWorkspaceResponse["scenes"];
}

export interface CopilotResponse {
  projectId: string;
  answer: string;
}

export interface RenderJobStatusResponse {
  jobId: string;
  projectId: string;
  status: string;
  artifactPath: string;
  artifactType: string;
  pipelineType: PipelineType;
  fallbackLevel: string;
  startedAt: string;
  finishedAt: string | null;
  checkpoint: {
    currentPhase: string;
    currentShotId: string | null;
    totalShots: number;
    progressPercent: number;
    completedShots: string[];
    reasons: string[];
    updatedAt: string;
  };
  events: Array<{
    phase: string;
    shotId: string | null;
    progressPercent: number;
    message: string;
    createdAt: string;
  }>;
  latestRuntime: {
    phase: string;
    shotId: string | null;
    progressPercent: number;
    cpuLoadPercent: number;
    usedMemoryBytes: number;
    totalMemoryBytes: number;
    availableMemoryBytes: number;
    estimatedVramBytes: number;
    capturedAt: string;
  } | null;
  artifacts: Array<{
    label: string;
    path: string;
    mediaType: string | null;
    sizeBytes: number;
    verified: boolean;
  }>;
}

export interface RenderRuntimeStatsResponse {
  jobId: string;
  samples: Array<{
    phase: string;
    shotId: string | null;
    progressPercent: number;
    cpuLoadPercent: number;
    usedMemoryBytes: number;
    totalMemoryBytes: number;
    availableMemoryBytes: number;
    estimatedVramBytes: number;
    capturedAt: string;
  }>;
}

export interface RenderArtifactsResponse {
  jobId: string;
  artifacts: Array<{
    label: string;
    path: string;
    mediaType: string | null;
    sizeBytes: number;
    verified: boolean;
  }>;
}
