$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$env:PYTHONPATH = $root
$url = if ($args.Count -gt 0) { $args[0] } else { 'https://zujuan.xkw.com/gzyw/zj135948/' }
python -m zujuan_collector.catalog_crawler --url $url @args[1..($args.Length-1)]
