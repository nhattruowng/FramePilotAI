import type {
  BenchmarkRunResponse,
  CopilotResponse,
  DeviceExplanationResponse,
  DeviceRecommendationResponse,
  DeviceScanResponse,
  OcrRunResponse,
  PanelParseResponse,
  ProjectAnalysisSummaryResponse,
  ProjectResponse,
  ProjectWorkspaceResponse,
  RenderArtifactsResponse,
  RenderJobStatusResponse,
  RenderRuntimeStatsResponse,
  ScenePlanResponse
} from "./types";

export const API_BASE = "http://localhost:8080/api";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...init
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `Request failed with status ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export async function scanDevice(): Promise<DeviceScanResponse> {
  return request<DeviceScanResponse>("/device/scan", { method: "POST" });
}

export async function runBenchmark(scanId: string): Promise<BenchmarkRunResponse> {
  return request<BenchmarkRunResponse>("/device/benchmark", {
    method: "POST",
    body: JSON.stringify({ scanId })
  });
}

export async function getRecommendation(scanId: string, benchmarkId: string): Promise<DeviceRecommendationResponse> {
  return request<DeviceRecommendationResponse>(`/device/recommendation?scanId=${scanId}&benchmarkId=${benchmarkId}`);
}

export async function getExplanation(scanId: string, benchmarkId: string): Promise<DeviceExplanationResponse> {
  return request<DeviceExplanationResponse>(`/device/explanation?scanId=${scanId}&benchmarkId=${benchmarkId}`);
}

export async function createProject(name: string, description: string, sourceType = "image-sequence"): Promise<ProjectResponse> {
  return request<ProjectResponse>("/projects", {
    method: "POST",
    body: JSON.stringify({ name, description, sourceType })
  });
}

export async function importAssets(projectId: string, assetPaths: string[]): Promise<ProjectResponse> {
  return request<ProjectResponse>(`/projects/${projectId}/assets/import`, {
    method: "POST",
    body: JSON.stringify({ assetPaths })
  });
}

export async function importDemoProject(): Promise<ProjectResponse> {
  return request<ProjectResponse>("/projects/import-demo", { method: "POST" });
}

export async function getWorkspace(projectId: string): Promise<ProjectWorkspaceResponse> {
  return request<ProjectWorkspaceResponse>(`/projects/${projectId}/workspace`);
}

export async function parsePanels(projectId: string): Promise<PanelParseResponse> {
  return request<PanelParseResponse>(`/projects/${projectId}/panels/parse`, { method: "POST" });
}

export async function runProjectOcr(projectId: string): Promise<OcrRunResponse> {
  return request<OcrRunResponse>(`/projects/${projectId}/ocr/run`, { method: "POST" });
}

export async function analyzeProject(projectId: string): Promise<ProjectAnalysisSummaryResponse> {
  return request<ProjectAnalysisSummaryResponse>(`/projects/${projectId}/analysis/summary`, { method: "POST" });
}

export async function generateScenePlan(projectId: string): Promise<ScenePlanResponse> {
  return request<ScenePlanResponse>(`/projects/${projectId}/scene-plan`, { method: "POST" });
}

export async function queryCopilot(projectId: string, question: string): Promise<CopilotResponse> {
  return request<CopilotResponse>(`/projects/${projectId}/copilot/query`, {
    method: "POST",
    body: JSON.stringify({ question })
  });
}

export async function startRender(projectId: string): Promise<RenderJobStatusResponse> {
  return request<RenderJobStatusResponse>(`/projects/${projectId}/render/start`, { method: "POST" });
}

export async function getRenderJob(jobId: string): Promise<RenderJobStatusResponse> {
  return request<RenderJobStatusResponse>(`/render/jobs/${jobId}`);
}

export async function pauseRenderJob(jobId: string): Promise<RenderJobStatusResponse> {
  return request<RenderJobStatusResponse>(`/render/jobs/${jobId}/pause`, { method: "POST" });
}

export async function resumeRenderJob(jobId: string): Promise<RenderJobStatusResponse> {
  return request<RenderJobStatusResponse>(`/render/jobs/${jobId}/resume`, { method: "POST" });
}

export async function cancelRenderJob(jobId: string): Promise<RenderJobStatusResponse> {
  return request<RenderJobStatusResponse>(`/render/jobs/${jobId}/cancel`, { method: "POST" });
}

export async function getRenderRuntime(jobId: string): Promise<RenderRuntimeStatsResponse> {
  return request<RenderRuntimeStatsResponse>(`/render/jobs/${jobId}/runtime`);
}

export async function getRenderArtifacts(jobId: string): Promise<RenderArtifactsResponse> {
  return request<RenderArtifactsResponse>(`/render/jobs/${jobId}/artifacts`);
}
