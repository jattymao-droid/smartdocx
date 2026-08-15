$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$env:PYTHONPATH = $root

Write-Host 'Step 1: Crawl missing senior subject catalogs (resume)...' -ForegroundColor Cyan
python -m zujuan_collector.catalog_crawler --resume --delay 2.0

Write-Host 'Step 2: Sync catalogs to RuoYi textbook tree...' -ForegroundColor Cyan
$env:PYTHONIOENCODING = 'utf-8'
python -m zujuan_collector.catalog_sync --all
