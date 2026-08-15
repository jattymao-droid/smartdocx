$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    Write-Error 'Python not found. Install Python 3.10+ and retry.'
}

$env:PYTHONPATH = $root
python -m zujuan_collector.desktop_app
