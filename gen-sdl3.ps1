<# gen-sdl3.ps1 — Generate Panama bindings for SDL3 using jextract on PATH #>

[CmdletBinding()]
param(
  [string]$PlatformDir = "ExternalLibs/Platforms/Windowsx64",
  [string]$SDLVersion  = "SDL3-3.2.22",
  [string]$OutDir      = "ExternalLibs/SDL3",
  [string]$BuildDir    = "Build",
  [string]$Pkg         = "org.libsdl3",
  [string]$HeaderClass = "SDL3",
  [switch]$Clean
)

$ErrorActionPreference = 'Stop'
$orig = Get-Location
try {
  # repo root (where the script lives)
  Set-Location (Split-Path -Parent $MyInvocation.MyCommand.Path)

  # Compute absolute paths
  $rootPath  = (Get-Location).Path
  $SDLRoot   = Join-Path $rootPath (Join-Path $PlatformDir $SDLVersion)
  $IncDir    = Join-Path $SDLRoot "include"
  $Header    = Join-Path $IncDir "SDL3\SDL.h"
  $BuildPath = Join-Path $rootPath $BuildDir
  $OutPath   = Join-Path $rootPath $OutDir

  if (-not (Test-Path $Header)) {
    throw "SDL header not found: $Header`nCheck -PlatformDir / -SDLVersion."
  }

  if ($Clean -and (Test-Path $OutPath)) {
    Write-Host "==> Cleaning $OutPath"
    Remove-Item -Recurse -Force $OutPath
  }
  if (-not (Test-Path $OutPath))  { New-Item -ItemType Directory -Path $OutPath  | Out-Null }
  if (-not (Test-Path $BuildPath)) { New-Item -ItemType Directory -Path $BuildPath | Out-Null }

  # Normalize to forward slashes for libclang/jextract
  $IncDirN  = $IncDir  -replace '\\','/'
  $HeaderN  = $Header  -replace '\\','/'
  $OutPathN = $OutPath -replace '\\','/'

  Write-Host "==> jextract version"
  jextract --version

  Push-Location $BuildPath
  Write-Host "==> Running jextract"
  Write-Host "    -I $IncDirN"
  Write-Host "    -o $OutPathN"
  Write-Host "    package: $Pkg, header-class: $HeaderClass"

  jextract `
    --output "$OutPathN" `
    --target-package "$Pkg" `
    --header-class-name "$HeaderClass" `
    -I "$IncDirN" `
    -l SDL3 `
    "$HeaderN"

  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

  Write-Host "==> Done. Generated sources in: $OutPath"
  Write-Host "Add '$OutDir' as a sources root in your IDE/build."
}
finally {
  Pop-Location -ErrorAction SilentlyContinue
  Set-Location $orig
}
