$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$SrcDir = Join-Path $Root 'tools\kkfileview\src'
$DistDir = Join-Path $Root 'tools\kkfileview\dist'
$Maven = 'D:\Tools\apache-maven-3.9.9\bin\mvn.cmd'
if (-not (Test-Path $Maven)) { $Maven = 'mvn' }

if (-not (Test-Path (Join-Path $SrcDir 'pom.xml'))) {
  Write-Host 'Cloning kkFileView v4.2.1 from Gitee...'
  New-Item -ItemType Directory -Force -Path (Split-Path $SrcDir) | Out-Null
  git clone --depth 1 --branch v4.2.1 https://gitee.com/kekingcn/file-online-preview.git $SrcDir
}

Write-Host 'Building kkFileView (may take a few minutes)...'
Push-Location $SrcDir
& $Maven clean package -DskipTests -pl server -am
Pop-Location

$zip = Join-Path $SrcDir 'server\target\kkFileView-4.2.1.zip'
if (-not (Test-Path $zip)) {
  Write-Host 'Build failed: zip not found.'
  exit 1
}

if (Test-Path $DistDir) { Remove-Item $DistDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null
Expand-Archive -Path $zip -DestinationPath $DistDir -Force
Write-Host 'OK  kkFileView built to tools\kkfileview\dist\kkFileView-4.2.1'
