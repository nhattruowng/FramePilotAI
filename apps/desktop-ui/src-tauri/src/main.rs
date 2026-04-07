#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::path::PathBuf;
use std::process::{Child, Command, Stdio};
use std::sync::Mutex;
use tauri::path::BaseDirectory;
use tauri::{Manager, WindowEvent};

struct BackendState(Mutex<Option<Child>>);

fn main() {
    tauri::Builder::default()
        .manage(BackendState(Mutex::new(None)))
        .setup(|app| {
            let handle = app.handle();
            maybe_spawn_backend(&handle);
            Ok(())
        })
        .on_window_event(|window, event| {
            if matches!(event, WindowEvent::Destroyed) {
                stop_backend(&window.app_handle());
            }
        })
        .run(tauri::generate_context!())
        .expect("error while running FramePilot AI desktop application");
}

fn maybe_spawn_backend(app: &tauri::AppHandle) {
    if std::env::var("FRAMEPILOT_SKIP_BUNDLED_BACKEND").is_ok() {
        return;
    }

    let Some(jar_path) = resolve_backend_jar(app) else {
        return;
    };
    if !jar_path.exists() {
        return;
    }

    let java_binary = resolve_java_binary();
    let storage_root = resolve_storage_root();
    let log_dir = resolve_log_dir();
    let _ = std::fs::create_dir_all(&storage_root);
    let _ = std::fs::create_dir_all(&log_dir);

    let mut command = Command::new(java_binary);
    command
        .arg(format!("-Dlogging.file.name={}", log_dir.join("local-server.log").display()))
        .arg("-jar")
        .arg(jar_path)
        .arg(format!("--framepilot.storage.root={}", storage_root.display()))
        .stdout(Stdio::null())
        .stderr(Stdio::null());

    if let Ok(child) = command.spawn() {
        if let Ok(mut state) = app.state::<BackendState>().0.lock() {
            *state = Some(child);
        }
    }
}

fn stop_backend(app: &tauri::AppHandle) {
    if let Ok(mut state) = app.state::<BackendState>().0.lock() {
        if let Some(child) = state.as_mut() {
            let _ = child.kill();
        }
        *state = None;
    }
}

fn resolve_backend_jar(app: &tauri::AppHandle) -> Option<PathBuf> {
    app.path()
        .resolve("backend/framepilot-ai-local-server.jar", BaseDirectory::Resource)
        .ok()
}

fn resolve_java_binary() -> String {
    if let Ok(java_home) = std::env::var("JAVA_HOME") {
        let candidate = PathBuf::from(java_home).join("bin").join("java.exe");
        if candidate.exists() {
            return candidate.to_string_lossy().to_string();
        }
    }
    "java".to_string()
}

fn resolve_storage_root() -> PathBuf {
    let base = std::env::current_exe()
        .ok()
        .and_then(|path| path.parent().map(|parent| parent.to_path_buf()))
        .unwrap_or_else(|| std::env::current_dir().unwrap_or_else(|_| PathBuf::from(".")));
    base.join("framepilot-data").join("storage")
}

fn resolve_log_dir() -> PathBuf {
    let base = std::env::current_exe()
        .ok()
        .and_then(|path| path.parent().map(|parent| parent.to_path_buf()))
        .unwrap_or_else(|| std::env::current_dir().unwrap_or_else(|_| PathBuf::from(".")));
    base.join("framepilot-data").join("logs")
}
