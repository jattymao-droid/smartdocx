$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$Port = 8012
$DistRoot = Join-Path $Root 'tools\kkfileview\dist\kkFileView-4.2.1'
$BinDir = Join-Path $DistRoot 'bin'
$Jar = Join-Path $BinDir 'kkFileView-4.2.1.jar'
$Config = Join-Path $DistRoot 'config\application.properties'
$LogDir = Join-Path $DistRoot 'log'

function Test-PortOpen([int]$p) {
  return (Test-NetConnection -ComputerName localhost -Port $p -WarningAction SilentlyContinue).TcpTestSucceeded
}

function Stop-PortProcess([int]$p) {
  $conn = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($conn) {
    $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
    if ($proc) {
      Write-Host "Stop process on port $p (pid $($proc.Id))"
      Stop-Process -Id $proc.Id -Force
      Start-Sleep -Seconds 2
    }
  }
}

if (-not (Test-Path $Jar)) {
  Write-Host "kkFileView not built yet. Run scripts\build-kkfileview.ps1 first."
  exit 1
}

Stop-PortProcess $Port

if (Test-PortOpen $Port) {
  Write-Host "Port $Port still in use."
  exit 1
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$logFile = Join-Path $LogDir 'kkFileView.log'

Write-Host 'Starting kkFileView (native, no Docker)...'
Start-Process -FilePath 'java' -ArgumentList @(
  '-Dfile.encoding=UTF-8',
  "-Dspring.config.location=$Config",
  '-jar', $Jar
) -WorkingDirectory $BinDir -WindowStyle Minimized

for ($i = 0; $i -lt 90; $i++) {
  Start-Sleep -Seconds 2
  if (Test-PortOpen $Port) {
    Write-Host "OK  kkFileView http://localhost:$Port"
    Write-Host "Log: $logFile"
    exit 0
  }
}

Write-Host "kkFileView did not open port $Port in time. Check log:"
Write-Host $logFile
Get-Content $logFile -Tail 30 -ErrorAction SilentlyContinue
exit 1
