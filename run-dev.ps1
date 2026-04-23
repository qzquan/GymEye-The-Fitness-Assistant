param(
  [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $repoRoot 'backend'
$androidDir = Join-Path $repoRoot 'android_project'
$port = 8080
$packageName = 'com.example.strong_body'
$launcherActivity = "$packageName/.LoginActivity"

function Test-PortOpen {
  param([int]$Port)
  $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
  return $null -ne $connection
}

if (Test-PortOpen -Port $port) {
  Write-Host "GymEye backend is already running on port $port."
} else {
  Write-Host "Starting GymEye backend on port $port..."
  $stdout = Join-Path $backendDir 'backend.autostart.stdout.log'
  $stderr = Join-Path $backendDir 'backend.autostart.stderr.log'
  Start-Process -FilePath 'node.exe' `
    -ArgumentList 'src/index.js' `
    -WorkingDirectory $backendDir `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr `
    -WindowStyle Hidden

  $deadline = (Get-Date).AddSeconds(10)
  while ((Get-Date) -lt $deadline) {
    if (Test-PortOpen -Port $port) {
      break
    }
    Start-Sleep -Milliseconds 500
  }

  if (-not (Test-PortOpen -Port $port)) {
    throw "Backend did not start on port $port. Check $stderr"
  }
}

if (-not $SkipInstall) {
  Write-Host "Building and installing Android debug app..."
  Push-Location $androidDir
  try {
    .\gradlew.bat installDebug
  } finally {
    Pop-Location
  }
}

Write-Host "Launching Android app..."
adb shell am start -n $launcherActivity
