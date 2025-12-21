@echo off
REM Docker WSL Manager Launcher
REM Requires Java 22 or higher

echo Starting Docker WSL Manager...
echo.

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found!
    echo Please install Java 22 or higher and add it to your PATH.
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

REM Run the application
java -jar docker-wsl-manager-1.0.0-standalone.jar

if %errorlevel% neq 0 (
    echo.
    echo Application exited with error code: %errorlevel%
    pause
)

