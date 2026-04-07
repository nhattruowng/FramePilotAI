create table if not exists projects (
    id text primary key,
    name text not null,
    status text not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists project_assets (
    id text primary key,
    project_id text not null,
    source_path text not null,
    media_type text,
    size_bytes integer not null,
    foreign key(project_id) references projects(id)
);

create table if not exists device_scans (
    id text primary key,
    device_id text not null,
    os_name text not null,
    os_version text not null,
    profile_json text not null,
    created_at timestamp not null
);

create table if not exists benchmark_runs (
    id text primary key,
    scan_id text not null,
    result_json text not null,
    aggregate_score real not null,
    created_at timestamp not null,
    foreign key(scan_id) references device_scans(id)
);

create table if not exists project_metadata (
    project_id text primary key,
    source_type text,
    description text,
    summary text,
    complexity_level text,
    panel_count integer not null default 0,
    scene_count integer not null default 0,
    metadata_json text,
    updated_at timestamp not null,
    foreign key(project_id) references projects(id)
);

create table if not exists project_panels (
    panel_id text primary key,
    project_id text not null,
    asset_id text,
    asset_path text not null,
    reading_order integer not null,
    crop_x integer not null,
    crop_y integer not null,
    width integer not null,
    height integer not null,
    summary text,
    review_state text not null,
    metadata_json text,
    updated_at timestamp not null,
    foreign key(project_id) references projects(id)
);

create table if not exists project_ocr (
    ocr_id text primary key,
    project_id text not null,
    panel_id text not null,
    text text not null,
    confidence real not null,
    language text not null,
    adapter_name text not null,
    updated_at timestamp not null,
    foreign key(project_id) references projects(id)
);

create table if not exists project_scenes (
    scene_id text primary key,
    project_id text not null,
    scene_order integer not null,
    title text not null,
    narrative text not null,
    complexity_score real not null,
    camera_effect_level text not null,
    total_duration_ms integer not null,
    metadata_json text,
    updated_at timestamp not null,
    foreign key(project_id) references projects(id)
);

create table if not exists project_shots (
    shot_id text primary key,
    project_id text not null,
    scene_id text not null,
    panel_id text not null,
    shot_order integer not null,
    duration_ms integer not null,
    camera_move text not null,
    effect_level text not null,
    timeline_start_ms integer not null,
    timeline_end_ms integer not null,
    notes text,
    updated_at timestamp not null,
    foreign key(project_id) references projects(id)
);

create table if not exists render_jobs (
    id text primary key,
    project_id text not null,
    status text not null,
    pipeline_type text not null,
    fallback_level text not null,
    artifact_path text,
    artifact_type text,
    checkpoint_path text,
    started_at timestamp not null,
    finished_at timestamp,
    foreign key(project_id) references projects(id)
);

create table if not exists render_job_events (
    id integer primary key autoincrement,
    job_id text not null,
    phase text not null,
    shot_id text,
    progress_percent real not null,
    message text not null,
    created_at timestamp not null,
    foreign key(job_id) references render_jobs(id)
);

create table if not exists render_runtime_samples (
    id integer primary key autoincrement,
    job_id text not null,
    phase text not null,
    shot_id text,
    progress_percent real not null,
    cpu_load_percent real not null,
    used_memory_bytes integer not null,
    total_memory_bytes integer not null,
    available_memory_bytes integer not null,
    estimated_vram_bytes integer not null,
    captured_at timestamp not null,
    foreign key(job_id) references render_jobs(id)
);
