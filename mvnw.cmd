@REM ----------------------------------------------------------------------------
@REM HelloAgent lightweight Maven Wrapper (no maven-wrapper.jar required)
@REM Downloads Apache Maven 3.9.9 on first run, reuses it afterwards.
@REM ----------------------------------------------------------------------------
@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0.mvn\wrapper\mvnw.ps1" %*
exit /b %ERRORLEVEL%
