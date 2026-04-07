Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-FramePilotRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
}

function Write-Section {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Assert-Command {
    param([string]$CommandName, [string]$Hint)
    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "Required command '$CommandName' is missing. $Hint"
    }
}

function Get-JavaMajorVersion {
    param([string]$JavaBinary)
    try {
        $versionOutput = cmd /c "`"$JavaBinary`" -version 2>&1" | Select-Object -First 1
        if ($versionOutput -match '"(?<version>\d+)(\.\d+)?') {
            return [int]$Matches.version
        }
    } catch {
    }
    return $null
}

function Resolve-FramePilotJavaHome {
    $candidateHomes = New-Object System.Collections.Generic.List[string]
    $fallbackHome = $null

    if ($env:JAVA_HOME) {
        $candidateHomes.Add($env:JAVA_HOME)
    }

    foreach ($pattern in @(
        "C:\Program Files\Java\jdk-*",
        "C:\Program Files\Eclipse Adoptium\jdk-*",
        "C:\Program Files\Microsoft\jdk-*"
    )) {
        Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object { $candidateHomes.Add($_.FullName) }
    }

    foreach ($javaHome in $candidateHomes) {
        $javaBinary = Join-Path $javaHome "bin\java.exe"
        if (-not (Test-Path $javaBinary)) {
            continue
        }
        $majorVersion = Get-JavaMajorVersion -JavaBinary $javaBinary
        if ($majorVersion -eq 21) {
            return $javaHome
        }
        if ($null -ne $majorVersion -and $majorVersion -gt 21 -and $null -eq $fallbackHome) {
            $fallbackHome = $javaHome
        }
    }

    if ($null -ne $fallbackHome) {
        return $fallbackHome
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $majorVersion = Get-JavaMajorVersion -JavaBinary $javaCommand.Source
        if ($null -ne $majorVersion -and $majorVersion -ge 21) {
            $binDir = Split-Path -Parent $javaCommand.Source
            return Split-Path -Parent $binDir
        }
    }

    throw "Unable to find a Java 21+ runtime. Install Java 21+ and set JAVA_HOME if auto-detection fails."
}

function Use-FramePilotJava {
    $env:JAVA_HOME = Resolve-FramePilotJavaHome
    $candidate = Join-Path $env:JAVA_HOME "bin"
    if (Test-Path $candidate) {
        $env:Path = "$candidate;$env:Path"
    }
    Assert-Command -CommandName "java" -Hint "Install Java 21+ and set JAVA_HOME."
    $majorVersion = Get-JavaMajorVersion -JavaBinary (Join-Path $candidate "java.exe")
    if ($null -eq $majorVersion -or $majorVersion -lt 21) {
        throw "FramePilot AI requires Java 21+. Resolved JAVA_HOME was '$($env:JAVA_HOME)'."
    }
}

function Use-FramePilotNode {
    Assert-Command -CommandName "node" -Hint "Install Node.js 22+."
    Assert-Command -CommandName "corepack" -Hint "Corepack ships with modern Node.js builds."
}

function Ensure-Directory {
    param([string]$PathValue)
    if (-not (Test-Path $PathValue)) {
        New-Item -ItemType Directory -Path $PathValue -Force | Out-Null
    }
}

function Stage-TauriBackendResource {
    param([string]$RepoRoot)
    $jarSource = Join-Path $RepoRoot "apps\local-server\build\libs\framepilot-ai-local-server.jar"
    $resourceDir = Join-Path $RepoRoot "apps\desktop-ui\src-tauri\resources\backend"
    Ensure-Directory -PathValue $resourceDir
    if (-not (Test-Path $jarSource)) {
        throw "Backend jar not found at $jarSource. Run bootJar first."
    }
    Copy-Item -Path $jarSource -Destination (Join-Path $resourceDir "framepilot-ai-local-server.jar") -Force
}

function Test-WindowsPackagingPrerequisites {
    $hasCargo = Get-Command cargo -ErrorAction SilentlyContinue
    $hasRustc = Get-Command rustc -ErrorAction SilentlyContinue
    $hasCl = Get-Command cl.exe -ErrorAction SilentlyContinue
    $hasRc = Get-Command rc.exe -ErrorAction SilentlyContinue
    return ($null -ne $hasCargo -and $null -ne $hasRustc -and $null -ne $hasCl -and $null -ne $hasRc)
}
