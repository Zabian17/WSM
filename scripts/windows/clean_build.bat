@echo off
setlocal
pushd "%~dp0..\.."
echo ========================================================
echo   GUDANGKU - Maven Clean & Compile Project
echo ========================================================

echo Running Maven Clean and Test-Compile...
call mvnw.cmd clean test-compile
set "EXIT_CODE=%ERRORLEVEL%"

echo ========================================================
echo   Build finished! You can now reload VS Code.
echo ========================================================
popd
pause
exit /b %EXIT_CODE%
