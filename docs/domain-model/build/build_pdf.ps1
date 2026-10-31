# PowerShell build script for Domain Model Specification PDF
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$docDir = Split-Path -Parent $scriptDir

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Compiling Domain Model Specification (XeLaTeX)...        " -ForegroundColor Cyan
Write-Host " Document Directory: $docDir                              " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Regenerate LaTeX source from script (optional/safe sync)
$genScript = Join-Path $scriptDir "generate_latex.py"
if (Test-Path $genScript) {
    Write-Host "[1/3] Generating synchronized LaTeX source..." -ForegroundColor Yellow
    python $genScript
}

# 2. Check for xelatex
$xelatexCmd = Get-Command xelatex -ErrorAction SilentlyContinue
if (-not $xelatexCmd) {
    # Check TinyTeX path fallback
    $tinytexPath = "C:\tools\TinyTeX\bin\windows\xelatex.exe"
    if (Test-Path $tinytexPath) {
        $xelatex = $tinytexPath
    } else {
        Write-Error "xelatex executable not found in PATH or standard location."
        exit 1
    }
} else {
    $xelatex = "xelatex"
}

Push-Location $docDir
try {
    Write-Host "[2/3] First XeLaTeX compilation pass..." -ForegroundColor Yellow
    & $xelatex -interaction=nonstopmode domain-model-specification.tex
    if ($LASTEXITCODE -ne 0) {
        Write-Error "First pass compilation failed with exit code $LASTEXITCODE"
        exit $LASTEXITCODE
    }

    Write-Host "[3/3] Second XeLaTeX compilation pass (resolving cross-references & TOC)..." -ForegroundColor Yellow
    & $xelatex -interaction=nonstopmode domain-model-specification.tex
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Second pass compilation failed with exit code $LASTEXITCODE"
        exit $LASTEXITCODE
    }

    $pdfPath = Join-Path $docDir "domain-model-specification.pdf"
    if (Test-Path $pdfPath) {
        $pdfSize = (Get-Item $pdfPath).Length
        Write-Host "==========================================================" -ForegroundColor Green
        Write-Host " PDF successfully generated: domain-model-specification.pdf" -ForegroundColor Green
        Write-Host " Size: $([math]::Round($pdfSize / 1MB, 2)) MB ($pdfSize bytes)" -ForegroundColor Green
        Write-Host "==========================================================" -ForegroundColor Green
    }
} finally {
    Pop-Location
}
