# Docker WSL Manager - Release Build Script
# This script creates a complete release package

param(
    [string]$Version = "1.0.0",
    [string]$OutputDir = "release"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Docker WSL Manager - Release Build" -ForegroundColor Cyan
Write-Host "Version: $Version" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Clean previous build
Write-Host "[1/5] Cleaning previous build..." -ForegroundColor Yellow
mvn clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Clean failed!" -ForegroundColor Red
    exit 1
}

# Step 2: Build with release profile
Write-Host ""
Write-Host "[2/5] Building release package..." -ForegroundColor Yellow
mvn package -P release -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed!" -ForegroundColor Red
    exit 1
}

# Step 3: Create release directory
Write-Host ""
Write-Host "[3/5] Creating release directory..." -ForegroundColor Yellow
if (Test-Path $OutputDir) {
    Remove-Item $OutputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $OutputDir | Out-Null

# Step 4: Copy artifacts
Write-Host ""
Write-Host "[4/5] Copying release artifacts..." -ForegroundColor Yellow

$standaloneName = "docker-wsl-manager-$Version-standalone.jar"
$targetJar = "target\$standaloneName"

if (Test-Path $targetJar) {
    Copy-Item $targetJar "$OutputDir\docker-wsl-manager.jar"
    Write-Host "  [OK] Copied standalone JAR" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Standalone JAR not found: $targetJar" -ForegroundColor Red
    exit 1
}

# Copy launcher script
if (Test-Path "run.bat") {
    Copy-Item "run.bat" "$OutputDir\"

    # Update version in run.bat
    $runBatContent = Get-Content "$OutputDir\run.bat" -Raw
    $runBatContent = $runBatContent -replace "docker-wsl-manager-\d+\.\d+\.\d+-standalone\.jar", "docker-wsl-manager.jar"
    Set-Content "$OutputDir\run.bat" $runBatContent

    Write-Host "  [OK] Copied launcher script" -ForegroundColor Green
}

# Copy documentation
if (Test-Path "README.md") {
    Copy-Item "README.md" "$OutputDir\"
    Write-Host "  [OK] Copied README" -ForegroundColor Green
}

if (Test-Path "LICENSE") {
    Copy-Item "LICENSE" "$OutputDir\"
    Write-Host "  [OK] Copied LICENSE" -ForegroundColor Green
}

# Create release notes
$releaseNotes = @"
# Docker WSL Manager - Release $Version

## What's New in 1.1.0

### Features & Enhancements
- **Enhanced Log Viewer**: Refactored to support real-time log streaming with ANSI color support.
- **Smart Scroll Lock**: Implemented a robust scroll-lock mechanism that pauses auto-scrolling while reading history and resumes at the bottom.
- **Container Details Page**: Renamed "View Logs" to "Open Details", providing a dedicated space to view output and control the container.
- **Reactive UI**: Action buttons (Start, Stop, Restart) now automatically react to the container's running state.
- **Dynamic Keyboard Shortcuts**: Implemented configurable shortcuts (e.g., Ctrl+S, Ctrl+R) for all major actions via a new Settings menu.
- **Auto-Update Check**: The application now automatically checks for newer versions on GitHub during startup.
- **Configurable Auto-Refresh**: Added an option to toggle and customize the container list refresh interval.
- **Clickable Ports**: Container ports in the Details view are now clickable hyperlinks.
- **Better Keyboard Control**: The application now focuses the first container on startup for immediate keyboard navigation.

### Fixes & Quality
- **Improved Stability**: Resolved multiple potential memory leaks and null pointer issues (verified via SonarQube & SpotBugs).
- **Stream Reliability**: Fixed an issue where logs would stop updating after a container restart.
- **Code Quality**: Achieved a passing status on the local SonarQube Quality Gate.

## Installation

1. Ensure Java 22 or higher is installed
2. Run the application:
   - Windows: Double-click `run.bat` or run `java -jar docker-wsl-manager.jar`
   - Linux/Mac: Run `java -jar docker-wsl-manager.jar`

## System Requirements

- Java Runtime Environment (JRE) 22 or higher
- Windows 10/11 with WSL 2 (recommended)
- Docker Desktop or Docker daemon

## Files Included

- `docker-wsl-manager.jar` - Main application (standalone, all dependencies included)
- `run.bat` - Windows launcher script
- `README.md` - User documentation
- `LICENSE` - License information

## Build Information

- Version: $Version
- Build Date: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
- Java Version: 22
- JavaFX Version: 25.0.1

## Support

For issues and feature requests, please visit the project repository.
"@

Set-Content "$OutputDir\RELEASE_NOTES.txt" $releaseNotes
Write-Host "  [OK] Created release notes" -ForegroundColor Green

# Step 5: Create ZIP archive
Write-Host ""
Write-Host "[5/5] Creating ZIP archive..." -ForegroundColor Yellow

$zipName = "docker-wsl-manager-$Version.zip"
if (Test-Path $zipName) {
    Remove-Item $zipName -Force
}

Compress-Archive -Path "$OutputDir\*" -DestinationPath $zipName
Write-Host "  [OK] Created $zipName" -ForegroundColor Green

# Get file sizes
$jarSize = (Get-Item "$OutputDir\docker-wsl-manager.jar").Length / 1MB
$zipSize = (Get-Item $zipName).Length / 1MB

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Release Package: $OutputDir\" -ForegroundColor White
Write-Host "  - JAR Size: $([math]::Round($jarSize, 2)) MB" -ForegroundColor White
Write-Host ""
Write-Host "Distribution Archive: $zipName" -ForegroundColor White
Write-Host "  - ZIP Size: $([math]::Round($zipSize, 2)) MB" -ForegroundColor White
Write-Host ""
Write-Host "To test the application:" -ForegroundColor Yellow
Write-Host "  cd $OutputDir" -ForegroundColor Gray
Write-Host "  java -jar docker-wsl-manager.jar" -ForegroundColor Gray
Write-Host ""

