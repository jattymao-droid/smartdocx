$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$Java = 'java'
$JavaOpts = @('-Dfile.encoding=UTF-8')

function Test-PortOpen([int]$Port) {
  return (Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue).TcpTestSucceeded
}

function Wait-Port([int]$Port, [int]$Seconds = 120, [string]$Label = '') {
  for ($i = 0; $i -lt $Seconds; $i += 2) {
    if (Test-PortOpen $Port) {
      if ($Label) { Write-Host "OK  $Label (port $Port)" }
      return $true
    }
    Start-Sleep -Seconds 2
  }
  throw "Timeout waiting for port $Port ($Label)"
}

function Stop-PortProcess([int]$Port) {
  $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($conn) {
    $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
    if ($proc -and $proc.ProcessName -eq 'java') {
      Write-Host "Stop java on port $Port (pid $($proc.Id))"
      Stop-Process -Id $proc.Id -Force
      Start-Sleep -Seconds 2
    }
  }
}

Write-Host 'Stopping old RuoYi backend/front ports if occupied...'
foreach ($port in 8080, 9200, 9201, 9300) { Stop-PortProcess $port }

if (-not (Test-PortOpen 8848)) {
  Write-Host 'Starting Nacos...'
  $nacosBin = Join-Path $Root 'tools\nacos\nacos\bin'
  Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'startup.cmd -m standalone' -WorkingDirectory $nacosBin -WindowStyle Minimized
}

Wait-Port 8848 120 'Nacos'

if (-not (Test-PortOpen 8012)) {
  Write-Host 'Starting kkFileView (required for zip/archive preview)...'
  & (Join-Path $Root 'scripts\start-kkfileview.ps1')
  if ($LASTEXITCODE -ne 0) {
    Write-Host 'WARN  kkFileView failed to start; archive preview may show "archive manifest failed"'
  }
} else {
  Write-Host 'OK  kkFileView (port 8012)'
}

Write-Host 'Publishing config from config/ to Nacos...'
& python (Join-Path $Root 'scripts\publish_local_config.py')

$authJar = Join-Path $Root 'ruoyi-auth\target\ruoyi-auth.jar'
$systemJar = Join-Path $Root 'ruoyi-modules\ruoyi-system\target\ruoyi-modules-system.jar'
$fileJar = Join-Path $Root 'ruoyi-modules\ruoyi-file\target\ruoyi-modules-file.jar'
$gatewayJar = Join-Path $Root 'ruoyi-gateway\target\ruoyi-gateway.jar'

Write-Host 'Starting ruoyi-auth...'
Start-Process -FilePath $Java -ArgumentList ($JavaOpts + @('-jar', $authJar)) -WorkingDirectory $Root -WindowStyle Minimized
Wait-Port 9200 120 'ruoyi-auth'

Write-Host 'Starting ruoyi-system...'
Start-Process -FilePath $Java -ArgumentList ($JavaOpts + @('-jar', $systemJar)) -WorkingDirectory $Root -WindowStyle Minimized
Wait-Port 9201 180 'ruoyi-system'

Write-Host 'Starting ruoyi-file...'
Start-Process -FilePath $Java -ArgumentList ($JavaOpts + @('-jar', $fileJar)) -WorkingDirectory $Root -WindowStyle Minimized
Wait-Port 9300 120 'ruoyi-file'

Write-Host 'Starting ruoyi-gateway...'
Start-Process -FilePath $Java -ArgumentList ($JavaOpts + @('-jar', $gatewayJar)) -WorkingDirectory $Root -WindowStyle Minimized
Wait-Port 8080 120 'ruoyi-gateway'

if (-not (Test-PortOpen 8081)) {
  Write-Host 'Starting ruoyi-ui...'
  $uiDir = Join-Path $Root 'ruoyi-ui'
  $env:BROWSER = 'none'
  Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'npm run dev -- --port 8081' -WorkingDirectory $uiDir -WindowStyle Minimized
  Wait-Port 8081 120 'ruoyi-ui'
}

Write-Host ''
Write-Host '========================================'
Write-Host ' RuoYi-Cloud started'
Write-Host ' User     : http://localhost:8081'
Write-Host ' Admin    : http://localhost:8081/admin'
Write-Host ' Gateway  : http://localhost:8080'
Write-Host ' Nacos    : http://localhost:8850'
Write-Host ' kkFileView: http://localhost:8012'
Write-Host ' Login    : admin / admin123'
Write-Host '========================================'
