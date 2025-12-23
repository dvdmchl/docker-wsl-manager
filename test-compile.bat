@echo off
echo Testing compilation after Issue #5 changes...
echo.
mvn clean compile
if %errorlevel% neq 0 (
    echo.
    echo COMPILATION FAILED!
    pause
    exit /b 1
)
echo.
echo COMPILATION SUCCESSFUL!
pause
