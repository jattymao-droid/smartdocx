$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Resolve-OcrServiceDir {
    $candidates = @(
        (Join-Path $root 'packages\question-bank\_extracted\题库\ocr-service\paddleocr-service'),
        (Join-Path $root 'packages\question-bank\ocr-service\paddleocr-service')
    )
    foreach ($candidate in $candidates) {
        if (Test-Path (Join-Path $candidate 'start.ps1')) {
            return (Resolve-Path $candidate).Path
        }
    }
    $found = Get-ChildItem -Path (Join-Path $root 'packages\question-bank\_extracted') -Recurse -Filter 'start.ps1' -ErrorAction SilentlyContinue |
        Where-Object { $_.DirectoryName -match 'paddleocr-service$' } |
        Select-Object -First 1
    if ($found) {
        return $found.Directory.FullName
    }
    return ''
}

$ocrDir = Resolve-OcrServiceDir
if (-not $ocrDir) {
    Write-Error "OCR service not found under packages\question-bank"
}

$listening = $false
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $tcp.Connect('127.0.0.1', 8867)
    $listening = $tcp.Connected
    $tcp.Close()
} catch {
    $listening = $false
}

if ($listening) {
    Write-Host 'OCR service already running on http://127.0.0.1:8867' -ForegroundColor Green
    exit 0
}

Write-Host 'Starting PaddleOCR service (first run may download models, wait a few minutes)...' -ForegroundColor Cyan
& "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File (Join-Path $ocrDir 'start.ps1')
