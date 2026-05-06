param(
  [switch]$SkipInstall,
  [switch]$RequireBackend
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

function Resolve-NodeExecutable {
  $candidates = New-Object System.Collections.Generic.List[string]

  if (-not [string]::IsNullOrWhiteSpace($env:GYMEYE_NODE)) {
    $candidates.Add($env:GYMEYE_NODE)
  }

  $candidates.Add('node.exe')
  $candidates.Add('node')

  $commonPaths = @(
    (Join-Path ${env:ProgramFiles} 'nodejs\node.exe'),
    (Join-Path ${env:ProgramFiles(x86)} 'nodejs\node.exe'),
    (Join-Path $env:LOCALAPPDATA 'Programs\nodejs\node.exe')
  )

  foreach ($path in $commonPaths) {
    if (-not [string]::IsNullOrWhiteSpace($path) -and (Test-Path -LiteralPath $path)) {
      $candidates.Add($path)
    }
  }

  foreach ($candidate in ($candidates | Select-Object -Unique)) {
    try {
      $command = Get-Command $candidate -ErrorAction Stop
      $executable = if ($command.Source) { $command.Source } else { $candidate }
      & $executable --version *> $null
      if ($LASTEXITCODE -eq 0) {
        return $executable
      }
    } catch {
      continue
    }
  }

  return $null
}

function Skip-BackendAutostart {
  param([string]$Reason)

  $message = "$Reason`nSkipping GymEye backend autostart so the Android debug app can still be built/launched.`nInstall Node.js, or set GYMEYE_NODE to your node.exe path if Node is installed outside PATH."
  if ($RequireBackend) {
    throw $message
  }
  Write-Warning $message
}

if (Test-PortOpen -Port $port) {
  Write-Host "GymEye backend is already running on port $port."
} else {
  $nodeExecutable = Resolve-NodeExecutable
  if ($null -eq $nodeExecutable) {
    Skip-BackendAutostart -Reason 'Node.js executable was not found.'
  } else {
    Write-Host "Starting GymEye backend on port $port with $nodeExecutable..."
    $stdout = Join-Path $backendDir 'backend.autostart.stdout.log'
    $stderr = Join-Path $backendDir 'backend.autostart.stderr.log'
    Start-Process -FilePath $nodeExecutable `
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
      Skip-BackendAutostart -Reason "Backend did not start on port $port. Check $stderr"
    }
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
