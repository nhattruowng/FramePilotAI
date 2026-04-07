#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR"

POWERSHELL_BIN=""
if command -v pwsh >/dev/null 2>&1; then
  POWERSHELL_BIN="pwsh"
elif command -v powershell.exe >/dev/null 2>&1; then
  POWERSHELL_BIN="powershell.exe"
elif command -v powershell >/dev/null 2>&1; then
  POWERSHELL_BIN="powershell"
else
  echo "FramePilot AI requires PowerShell to run the Windows-first automation scripts." >&2
  exit 1
fi

INCLUDE_BUNDLE=0
INCLUDE_CARGO_CHECK=0
SKIP_FRONTEND_INSTALL=0

while [ "$#" -gt 0 ]; do
  case "$1" in
    --bundle)
      INCLUDE_BUNDLE=1
      ;;
    --cargo-check)
      INCLUDE_CARGO_CHECK=1
      ;;
    --skip-frontend-install)
      SKIP_FRONTEND_INSTALL=1
      ;;
    -h|--help)
      cat <<'EOF'
Usage: sh ./run-all.sh [options]

Runs the main FramePilot AI validation flow in one pass:
  1. bootstrap
  2. build-all
  3. test-all with smoke test

Options:
  --bundle                 Also build the Tauri bundle
  --cargo-check            Include cargo check during test-all
  --skip-frontend-install  Skip pnpm install during bootstrap
  -h, --help               Show this help
EOF
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
  shift
done

run_ps() {
  script_path=$1
  shift
  "$POWERSHELL_BIN" -NoLogo -NoProfile -ExecutionPolicy Bypass -File "$script_path" "$@"
}

echo "== FramePilot AI one-shot run =="

if [ "$SKIP_FRONTEND_INSTALL" -eq 1 ]; then
  run_ps "./scripts/bootstrap.ps1" -SkipFrontendInstall
else
  run_ps "./scripts/bootstrap.ps1"
fi

if [ "$INCLUDE_BUNDLE" -eq 1 ]; then
  run_ps "./scripts/build-all.ps1" -IncludeTauriBundle
else
  run_ps "./scripts/build-all.ps1"
fi

if [ "$INCLUDE_CARGO_CHECK" -eq 1 ]; then
  run_ps "./scripts/test-all.ps1" -IncludeCargoCheck -IncludeSmoke
else
  run_ps "./scripts/test-all.ps1" -IncludeSmoke
fi

echo "== FramePilot AI run complete =="
