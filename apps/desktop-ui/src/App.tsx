import { useEffect, useState } from "react";
import { ActionCard } from "./components/ActionCard";
import {
  API_BASE,
  analyzeProject,
  cancelRenderJob,
  createProject,
  generateScenePlan,
  getRenderArtifacts,
  getRenderJob,
  getRenderRuntime,
  getWorkspace,
  importAssets,
  importDemoProject,
  parsePanels,
  pauseRenderJob,
  queryCopilot,
  resumeRenderJob,
  runProjectOcr,
  startRender
} from "./lib/api";
import type {
  CopilotResponse,
  OcrRunResponse,
  PanelParseResponse,
  ProjectAnalysisSummaryResponse,
  ProjectResponse,
  ProjectWorkspaceResponse,
  RenderArtifactsResponse,
  RenderJobStatusResponse,
  RenderRuntimeStatsResponse,
  ScenePlanResponse
} from "./lib/types";

type Status = "idle" | "loading" | "done" | "error";

function formatBytes(bytes: number) {
  if (bytes === 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  const order = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** order).toFixed(order === 0 ? 0 : 1)} ${units[order]}`;
}

function formatDuration(durationMillis: number) {
  return `${(durationMillis / 1000).toFixed(durationMillis >= 1000 ? 1 : 0)}s`;
}

function parseAssetLines(input: string) {
  return input
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

function isRenderActive(status: string) {
  return status === "QUEUED" || status === "STARTED" || status === "CANCELLING";
}

function isRenderTerminal(status: string) {
  return status === "COMPLETED" || status === "COMPLETED_WITH_FALLBACK" || status === "FAILED" || status === "CANCELLED";
}

export default function App() {
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [workspace, setWorkspace] = useState<ProjectWorkspaceResponse | null>(null);
  const [panelRun, setPanelRun] = useState<PanelParseResponse | null>(null);
  const [ocrRun, setOcrRun] = useState<OcrRunResponse | null>(null);
  const [analysis, setAnalysis] = useState<ProjectAnalysisSummaryResponse | null>(null);
  const [scenePlan, setScenePlan] = useState<ScenePlanResponse | null>(null);
  const [copilot, setCopilot] = useState<CopilotResponse | null>(null);
  const [renderJob, setRenderJob] = useState<RenderJobStatusResponse | null>(null);
  const [runtimeStats, setRuntimeStats] = useState<RenderRuntimeStatsResponse | null>(null);
  const [artifactCenter, setArtifactCenter] = useState<RenderArtifactsResponse | null>(null);
  const [projectName, setProjectName] = useState("Chapter 01 Planning");
  const [projectDescription, setProjectDescription] = useState("Imported comic chapter for motion comic planning.");
  const [assetPaths, setAssetPaths] = useState("");
  const [copilotQuestion, setCopilotQuestion] = useState("Explain the current scene plan.");
  const [status, setStatus] = useState<Record<string, Status>>({
    create: "idle",
    import: "idle",
    refresh: "idle",
    parse: "idle",
    ocr: "idle",
    analyze: "idle",
    plan: "idle",
    copilot: "idle",
    renderStart: "idle",
    renderPause: "idle",
    renderResume: "idle",
    renderCancel: "idle",
    renderRefresh: "idle"
  });
  const [error, setError] = useState("");

  const progressText = renderJob
    ? `Render ${renderJob.status.toLowerCase()} at ${renderJob.checkpoint.progressPercent}%`
    : workspace
      ? workspace.project.status === "READY"
        ? "Scene plan ready for render handoff"
        : workspace.project.status === "PLANNING"
          ? "Project is in planning with editable intermediate data"
          : "Project draft is ready for parsing"
      : "Create or import a project to begin planning";

  async function refreshWorkspace(projectId: string) {
    const nextWorkspace = await getWorkspace(projectId);
    setWorkspace(nextWorkspace);
    setProject(nextWorkspace.project);
  }

  async function runStep(key: keyof typeof status, task: () => Promise<void>) {
    setError("");
    setStatus((current) => ({ ...current, [key]: "loading" }));
    try {
      await task();
      setStatus((current) => ({ ...current, [key]: "done" }));
    } catch (taskError) {
      setStatus((current) => ({ ...current, [key]: "error" }));
      setError(taskError instanceof Error ? taskError.message : "Unexpected error");
    }
  }

  useEffect(() => {
    if (!renderJob?.jobId || !isRenderActive(renderJob.status)) {
      return undefined;
    }

    const source = new EventSource(`${API_BASE}/render/jobs/${renderJob.jobId}/stream`);
    source.onmessage = (event) => {
      try {
        const nextJob = JSON.parse(event.data) as RenderJobStatusResponse;
        setRenderJob(nextJob);
      } catch {
        source.close();
      }
    };
    source.onerror = () => {
      source.close();
    };

    return () => source.close();
  }, [renderJob?.jobId, renderJob?.status]);

  useEffect(() => {
    if (!renderJob?.jobId) {
      return;
    }

    let active = true;
    void (async () => {
      try {
        const [nextRuntime, nextArtifacts] = await Promise.all([
          getRenderRuntime(renderJob.jobId),
          getRenderArtifacts(renderJob.jobId)
        ]);
        if (!active) {
          return;
        }
        setRuntimeStats(nextRuntime);
        setArtifactCenter(nextArtifacts);
      } catch {
        // Keep the current panel state if stats refresh fails mid-render.
      }
    })();

    return () => {
      active = false;
    };
  }, [renderJob?.jobId, renderJob?.status, renderJob?.checkpoint.progressPercent]);

  return (
    <main className="app-shell">
      <section className="hero">
        <div>
          <p className="eyebrow">FramePilot AI</p>
          <h1>Comic-to-video planning and render console</h1>
          <p className="hero-copy">
            Run the local-first pipeline end to end: create a project, import chapter assets, review parsed panels, rerun OCR,
            generate the scene plan, then launch a render job with progress tracking, runtime guardrails, checkpoint-aware pause and resume,
            and an export center for the generated artifacts.
          </p>
        </div>
        <div className="hero-panel">
          <span className="status-dot" />
          <strong>{progressText}</strong>
          <p>Backend API: http://localhost:8080/api</p>
          <p>{project ? `Active project: ${project.name}` : "No active project selected."}</p>
        </div>
      </section>

      {error ? <div className="error-banner">{error}</div> : null}

      <section className="grid project-grid">
        <ActionCard
          eyebrow="01 / Project Import"
          title="Create a project and attach assets"
          action={
            <div className="action-row">
              <button
                className="secondary-button"
                onClick={() => runStep("create", async () => {
                  const created = await createProject(projectName, projectDescription);
                  setProject(created);
                  setWorkspace(null);
                  setPanelRun(null);
                  setOcrRun(null);
                  setAnalysis(null);
                  setScenePlan(null);
                  setCopilot(null);
                  setRenderJob(null);
                  setRuntimeStats(null);
                  setArtifactCenter(null);
                  await refreshWorkspace(created.id);
                })}
              >
                {status.create === "loading" ? "Creating..." : "Create Project"}
              </button>
              <button
                className="primary-button"
                onClick={() => runStep("import", async () => {
                  const imported = await importDemoProject();
                  setProject(imported);
                  setPanelRun(null);
                  setOcrRun(null);
                  setAnalysis(null);
                  setScenePlan(null);
                  setCopilot(null);
                  setRenderJob(null);
                  setRuntimeStats(null);
                  setArtifactCenter(null);
                  await refreshWorkspace(imported.id);
                })}
              >
                {status.import === "loading" ? "Importing..." : "Import Demo"}
              </button>
            </div>
          }
        >
          <div className="form-grid">
            <label className="field">
              <span>Project name</span>
              <input value={projectName} onChange={(event) => setProjectName(event.target.value)} />
            </label>
            <label className="field">
              <span>Description</span>
              <textarea value={projectDescription} onChange={(event) => setProjectDescription(event.target.value)} rows={3} />
            </label>
            <label className="field field-full">
              <span>Asset paths</span>
              <textarea
                value={assetPaths}
                onChange={(event) => setAssetPaths(event.target.value)}
                rows={4}
                placeholder={"D:\\FramePilotAI\\storage\\projects\\demo-assets\\sample-page-01.svg\nD:\\FramePilotAI\\storage\\projects\\demo-assets\\sample-page-02.svg"}
              />
            </label>
            <div className="action-row">
              <button
                className="ghost-button"
                disabled={!project || parseAssetLines(assetPaths).length === 0}
                onClick={() => project && runStep("import", async () => {
                  const updated = await importAssets(project.id, parseAssetLines(assetPaths));
                  setProject(updated);
                  await refreshWorkspace(updated.id);
                })}
              >
                {status.import === "loading" ? "Attaching..." : "Import Asset Paths"}
              </button>
              <button
                className="ghost-button"
                disabled={!project}
                onClick={() => project && runStep("refresh", async () => {
                  await refreshWorkspace(project.id);
                })}
              >
                {status.refresh === "loading" ? "Refreshing..." : "Refresh Workspace"}
              </button>
            </div>
          </div>
        </ActionCard>

        <ActionCard eyebrow="02 / Planning Flow" title="Run parse, OCR, analysis and scene planning">
          <div className="action-stack">
            <button
              className="primary-button"
              disabled={!project}
              onClick={() => project && runStep("parse", async () => {
                const result = await parsePanels(project.id);
                setPanelRun(result);
                await refreshWorkspace(project.id);
              })}
            >
              {status.parse === "loading" ? "Parsing..." : "Parse Panels"}
            </button>
            <button
              className="primary-button"
              disabled={!project}
              onClick={() => project && runStep("ocr", async () => {
                const result = await runProjectOcr(project.id);
                setOcrRun(result);
                await refreshWorkspace(project.id);
              })}
            >
              {status.ocr === "loading" ? "Running OCR..." : "Run OCR"}
            </button>
            <button
              className="primary-button"
              disabled={!project}
              onClick={() => project && runStep("analyze", async () => {
                const result = await analyzeProject(project.id);
                setAnalysis(result);
                await refreshWorkspace(project.id);
              })}
            >
              {status.analyze === "loading" ? "Analyzing..." : "Analyze Project"}
            </button>
            <button
              className="primary-button"
              disabled={!project}
              onClick={() => project && runStep("plan", async () => {
                const result = await generateScenePlan(project.id);
                setScenePlan(result);
                await refreshWorkspace(project.id);
              })}
            >
              {status.plan === "loading" ? "Planning..." : "Generate Scene Plan"}
            </button>
          </div>
          <div className="stats-grid compact-top">
            <div className="metric">
              <span>Assets</span>
              <strong>{workspace?.project.assets.length ?? 0}</strong>
            </div>
            <div className="metric">
              <span>Panels</span>
              <strong>{workspace?.panels.length ?? 0}</strong>
            </div>
            <div className="metric">
              <span>OCR blocks</span>
              <strong>{workspace?.ocrBlocks.length ?? 0}</strong>
            </div>
            <div className="metric">
              <span>Scenes</span>
              <strong>{workspace?.scenes.length ?? 0}</strong>
            </div>
          </div>
        </ActionCard>
      </section>

      <section className="detail-grid overview-grid">
        <ActionCard eyebrow="Project" title="Lifecycle and metadata">
          <div className="list-block">
            {(workspace
              ? [
                  `Status: ${workspace.project.status}`,
                  `Source type: ${workspace.metadata.sourceType}`,
                  `Description: ${workspace.metadata.description || "No description"}`,
                  `Summary: ${workspace.metadata.summary || "No project summary yet."}`,
                  `Complexity: ${workspace.metadata.complexityLevel}`,
                  `Assets in order: ${workspace.project.assets.map((asset) => `${asset.assetOrder + 1}:${asset.sourcePath.split(/[\\\\/]/).pop()}`).join(" | ") || "none"}`
                ]
              : ["Create or import a project to inspect metadata and lifecycle state."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>

        <ActionCard eyebrow="Analysis" title="Project analysis summary">
          <div className="list-block">
            {(analysis
              ? [
                  analysis.projectSummary,
                  `Complexity level: ${analysis.complexityLevel}`,
                  `Preset: ${analysis.preset}`,
                  `Pipeline recommendation: ${analysis.pipelineType}`,
                  `Project status at analysis: ${analysis.status}`,
                  ...analysis.copilotNotes
                ]
              : ["Run project analysis to generate summary, complexity classification and copilot notes."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>

        <ActionCard eyebrow="Runs" title="Most recent execution output">
          <div className="list-block">
            {[
              panelRun ? `Panel parsing completed with ${panelRun.panelCount} panels.` : "Panel parsing has not been run yet.",
              ocrRun ? `OCR completed with ${ocrRun.blockCount} text blocks.` : "OCR has not been run yet.",
              scenePlan ? `Scene plan completed with ${scenePlan.sceneCount} scenes.` : "Scene planning has not been run yet."
            ].map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>
      </section>

      <section className="review-grid">
        <ActionCard eyebrow="03 / Panel Review" title="Editable baseline panel output">
          <div className="review-list">
            {(workspace?.panels.length
              ? workspace.panels.map((panel) => `${panel.readingOrder}. ${panel.summary} | ${panel.width}x${panel.height} | crop ${panel.cropX},${panel.cropY} | ${panel.reviewState}`)
              : ["Run panel parsing to review ordered panels and baseline crops."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>

        <ActionCard eyebrow="04 / OCR Review" title="Panel text and adapter status">
          <div className="review-list">
            {(workspace?.ocrBlocks.length
              ? workspace.ocrBlocks.map((block) => `${block.panelId}: ${block.text} | confidence ${block.confidence.toFixed(2)} | ${block.adapterName}`)
              : ["Run OCR to review extracted text per panel."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>
      </section>

      <section className="detail-grid planning-grid">
        <ActionCard eyebrow="05 / Scene Plan" title="Scene and shot timeline">
          <div className="scene-list">
            {(workspace?.scenes.length
              ? workspace.scenes.map((scene) => `${scene.sceneOrder}. ${scene.title} | ${formatDuration(scene.totalDurationMillis)} | complexity ${scene.complexityScore} | ${scene.cameraEffectLevel}`)
              : ["Generate a scene plan to inspect grouped scenes, durations and camera effect levels."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
          {workspace?.scenes.length ? (
            <div className="shot-grid">
              {workspace.scenes.flatMap((scene) =>
                scene.shots.map((shot) => (
                  <div className="shot-card" key={shot.shotId}>
                    <strong>{shot.shotId}</strong>
                    <span>{scene.title}</span>
                    <p>Panel: {shot.panelId}</p>
                    <p>Move: {shot.cameraMove}</p>
                    <p>Duration: {formatDuration(shot.durationMillis)}</p>
                    <p>Timeline: {formatDuration(shot.timelineStartMillis)} - {formatDuration(shot.timelineEndMillis)}</p>
                    <p>Effect: {shot.effectLevel}</p>
                    <p>{shot.notes}</p>
                  </div>
                ))
              )}
            </div>
          ) : null}
        </ActionCard>

        <ActionCard
          eyebrow="06 / Copilot"
          title="Ask questions about the project"
          action={
            <button
              className="primary-button"
              disabled={!project}
              onClick={() => project && runStep("copilot", async () => {
                const answer = await queryCopilot(project.id, copilotQuestion);
                setCopilot(answer);
              })}
            >
              {status.copilot === "loading" ? "Thinking..." : "Ask Copilot"}
            </button>
          }
        >
          <label className="field">
            <span>Question</span>
            <textarea value={copilotQuestion} onChange={(event) => setCopilotQuestion(event.target.value)} rows={3} />
          </label>
          <div className="list-block compact-top">
            {(copilot
              ? [copilot.answer]
              : ["Ask about scenes, OCR, panel review or the current project status to get a short explanation."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>
      </section>

      <section className="grid render-grid">
        <ActionCard
          eyebrow="07 / Preset Confirmation"
          title="Lock the baseline render recipe"
          action={
            <button
              className="primary-button"
              disabled={!project}
              onClick={() => project && runStep("renderStart", async () => {
                const nextJob = await startRender(project.id);
                setRenderJob(nextJob);
                setRuntimeStats(null);
                setArtifactCenter(null);
              })}
            >
              {status.renderStart === "loading" ? "Queueing..." : "Start Render"}
            </button>
          }
        >
          <div className="list-block">
            {(analysis
              ? [
                  `Preset: ${analysis.preset}`,
                  `Pipeline: ${analysis.pipelineType}`,
                  `Complexity: ${analysis.complexityLevel}`,
                  `Project ready state: ${workspace?.project.status ?? "unknown"}`,
                  "Guardrails will auto-lower effect intensity, shot length and pipeline weight if runtime pressure rises."
                ]
              : ["Run analysis first to confirm preset and pipeline before starting render."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>

        <ActionCard eyebrow="08 / Render Console" title="Pause, resume, cancel and refresh">
          <div className="action-stack">
            <button
              className="secondary-button"
              disabled={!renderJob || renderJob.status !== "STARTED"}
              onClick={() => renderJob && runStep("renderPause", async () => {
                setRenderJob(await pauseRenderJob(renderJob.jobId));
              })}
            >
              {status.renderPause === "loading" ? "Pausing..." : "Pause"}
            </button>
            <button
              className="secondary-button"
              disabled={!renderJob || (renderJob.status !== "PAUSED" && renderJob.status !== "FAILED" && renderJob.status !== "CANCELLED")}
              onClick={() => renderJob && runStep("renderResume", async () => {
                setRenderJob(await resumeRenderJob(renderJob.jobId));
              })}
            >
              {status.renderResume === "loading" ? "Resuming..." : "Resume"}
            </button>
            <button
              className="ghost-button"
              disabled={!renderJob || !isRenderActive(renderJob.status)}
              onClick={() => renderJob && runStep("renderCancel", async () => {
                setRenderJob(await cancelRenderJob(renderJob.jobId));
              })}
            >
              {status.renderCancel === "loading" ? "Cancelling..." : "Cancel"}
            </button>
            <button
              className="ghost-button"
              disabled={!renderJob}
              onClick={() => renderJob && runStep("renderRefresh", async () => {
                setRenderJob(await getRenderJob(renderJob.jobId));
              })}
            >
              {status.renderRefresh === "loading" ? "Refreshing..." : "Refresh Status"}
            </button>
          </div>
          <div className="stats-grid compact-top">
            <div className="metric">
              <span>Status</span>
              <strong>{renderJob?.status ?? "idle"}</strong>
            </div>
            <div className="metric">
              <span>Progress</span>
              <strong>{renderJob ? `${renderJob.checkpoint.progressPercent}%` : "0%"}</strong>
            </div>
            <div className="metric">
              <span>Current phase</span>
              <strong>{renderJob?.checkpoint.currentPhase ?? "not-started"}</strong>
            </div>
            <div className="metric">
              <span>Current shot</span>
              <strong>{renderJob?.checkpoint.currentShotId ?? "n/a"}</strong>
            </div>
          </div>
        </ActionCard>
      </section>

      <section className="detail-grid render-detail-grid">
        <ActionCard eyebrow="09 / Progress Timeline" title="Event log and checkpoint state">
          <div className="list-block">
            {(renderJob
              ? [
                  `Pipeline: ${renderJob.pipelineType}`,
                  `Fallback level: ${renderJob.fallbackLevel}`,
                  `Completed shots: ${renderJob.checkpoint.completedShots.length}/${renderJob.checkpoint.totalShots}`,
                  ...renderJob.checkpoint.reasons,
                  ...renderJob.events.slice(-8).map((event) => `${event.phase}: ${event.message}`)
                ]
              : ["Start a render job to stream progress, fallback reasons and checkpoint updates."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>

        <ActionCard eyebrow="10 / Runtime Stats" title="CPU, RAM and VRAM samples">
          <div className="list-block">
            {(renderJob?.latestRuntime
              ? [
                  `CPU load: ${renderJob.latestRuntime.cpuLoadPercent}%`,
                  `Used RAM: ${formatBytes(renderJob.latestRuntime.usedMemoryBytes)} / ${formatBytes(renderJob.latestRuntime.totalMemoryBytes)}`,
                  `Available RAM: ${formatBytes(renderJob.latestRuntime.availableMemoryBytes)}`,
                  `Estimated VRAM: ${formatBytes(renderJob.latestRuntime.estimatedVramBytes)}`,
                  ...runtimeStats?.samples.slice(-5).map((sample) => `${sample.phase} | ${sample.shotId ?? "job"} | ${sample.progressPercent}%`)
                    ?? []
                ]
              : ["Runtime monitor starts sampling when a render job enters the queue."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>

        <ActionCard eyebrow="11 / Export Center" title="Final artifact and sidecar metadata">
          <div className="list-block">
            {(artifactCenter?.artifacts.length
              ? artifactCenter.artifacts.map((artifact) => `${artifact.label} | ${artifact.mediaType ?? "unknown"} | ${formatBytes(artifact.sizeBytes)} | ${artifact.verified ? "verified" : "unverified"} | ${artifact.path}`)
              : renderJob?.artifactPath
                ? [`Primary artifact: ${renderJob.artifactPath}`]
                : ["Artifacts will appear here after render preparation and final export."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>
      </section>

      <section className="detail-grid assets-grid">
        <ActionCard eyebrow="Assets" title="Imported chapter files">
          <div className="list-block">
            {(workspace?.project.assets.length
              ? workspace.project.assets.map((asset) => `${asset.assetOrder + 1}. ${asset.sourcePath} | ${asset.mediaType || "unknown"} | ${formatBytes(asset.sizeBytes)}`)
              : ["No assets attached yet. Import demo assets or add local file paths."]).map((item) => (
              <p key={item}>{item}</p>
            ))}
          </div>
        </ActionCard>
      </section>
    </main>
  );
}
