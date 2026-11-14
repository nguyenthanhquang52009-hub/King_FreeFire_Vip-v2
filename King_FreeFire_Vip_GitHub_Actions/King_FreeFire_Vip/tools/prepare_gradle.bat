@echo off
setlocal
cd /d "%~dp0\.."
set "VERSION=8.9"
set "DIST=%CD%\.gradle-bootstrap\gradle-%VERSION%"
set "ZIP=%CD%\.gradle-bootstrap\gradle-%VERSION%-bin.zip"
if exist "%DIST%\bin\gradle.bat" goto makewrapper
if not exist ".gradle-bootstrap" mkdir ".gradle-bootstrap"
echo Downloading official Gradle %VERSION% distribution...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip' -OutFile '%ZIP%'"
if errorlevel 1 (
  echo Download failed. Check Internet connection and try again.
  pause
  exit /b 1
)
echo Extracting Gradle...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP%' -DestinationPath '.gradle-bootstrap' -Force"
if errorlevel 1 (
  echo Extraction failed.
  pause
  exit /b 1
)
:makewrapper
echo Creating Gradle Wrapper files for this project...
call "%DIST%\bin\gradle.bat" wrapper --gradle-version %VERSION% --distribution-type bin
if errorlevel 1 (
  echo Could not create Gradle Wrapper.
  pause
  exit /b 1
)
echo.
echo SUCCESS. Close this window and open this folder in Android Studio:
echo %CD%
echo Then choose Gradle Wrapper and press Sync.
pause
