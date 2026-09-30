$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $taskProjectRoot 'gradlew.bat') -p $taskProjectRoot -I (Join-Path $PSScriptRoot 'local-build.init.gradle') build
exit $LASTEXITCODE
