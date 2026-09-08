@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Media Organizer Launcher

set "LAUNCHER_VERSION=1"

rem Everything installs beside this BAT.
set "APPDIR=%~dp0"
if "%APPDIR:~-1%"=="\" set "APPDIR=%APPDIR:~0,-1%"

set "REPO=https://github.com/Quest79/MediaOrgonizer.git"
set "LATEST_URL=https://raw.githubusercontent.com/Quest79/MediaOrgonizer/main/.launcher/latest.txt"
set "LAUNCHER_BASE=https://raw.githubusercontent.com/Quest79/MediaOrgonizer/main/.launcher"

set "TOOLROOT=%APPDIR%\.mediaorganizer-tools"
set "LOGDIR=%APPDIR%\.mediaorganizer-logs"

set "MAVEN_VERSION=3.9.16"
set "MAVEN_DIR=%TOOLROOT%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"

echo.
echo ============================================
echo   Media Organizer Launcher v%LAUNCHER_VERSION%
echo ============================================
echo.

call :self_update
if defined LAUNCHER_UPDATED exit /b 0

call :ensure_git
if errorlevel 1 goto :failed

call :ensure_jdk21
if errorlevel 1 goto :failed

call :ensure_maven
if errorlevel 1 goto :failed

if not exist "%LOGDIR%" mkdir "%LOGDIR%" >nul 2>&1
if not exist "%TOOLROOT%" mkdir "%TOOLROOT%" >nul 2>&1

echo [1/3] Updating Media Organizer...

pushd "%APPDIR%"

if exist ".git" (
    "%GIT_EXE%" fetch origin main
    if errorlevel 1 (
        popd
        goto :git_failed
    )

    "%GIT_EXE%" checkout main >nul 2>&1
    "%GIT_EXE%" pull --ff-only origin main
    if errorlevel 1 (
        popd
        goto :git_failed
    )
) else (
    "%GIT_EXE%" init
    if errorlevel 1 (
        popd
        goto :git_failed
    )

    "%GIT_EXE%" remote add origin "%REPO%"
    if errorlevel 1 (
        popd
        goto :git_failed
    )

    "%GIT_EXE%" fetch origin main
    if errorlevel 1 (
        popd
        goto :git_failed
    )

    "%GIT_EXE%" checkout -B main origin/main
    if errorlevel 1 (
        popd
        goto :git_failed
    )
)

echo [2/3] Verifying JDK 21...
"%JAVA_HOME%\bin\javac.exe" -version
if errorlevel 1 (
    popd
    echo ERROR: JDK compiler check failed.
    goto :failed
)

echo [3/3] Launching Media Organizer...

> "%LOGDIR%\launcher.log" (
    echo Media Organizer launcher v%LAUNCHER_VERSION% started %DATE% %TIME%
    echo JAVA_HOME=%JAVA_HOME%
    "%JAVA_HOME%\bin\java.exe" -version 2^>^&1
    "%JAVA_HOME%\bin\javac.exe" -version 2^>^&1
    echo Maven=%MVN_EXE%
    echo AppDir=%APPDIR%
)

set "RUNNER=%TOOLROOT%\run-media-organizer.cmd"

> "%RUNNER%" (
    echo @echo off
    echo setlocal
    echo set "JAVA_HOME=%JAVA_HOME%"
    echo set "PATH=%%JAVA_HOME%%\bin;%%PATH%%"
    echo cd /d "%APPDIR%"
    echo call "%MVN_EXE%" -DskipTests javafx:run ^>^> "%LOGDIR%\launcher.log" 2^>^&1
)

set "MEDIAORG_RUNNER=%RUNNER%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$r=$env:MEDIAORG_RUNNER; Start-Process -FilePath $env:ComSpec -ArgumentList @('/d','/c',('call ""'+$r+'""')) -WindowStyle Hidden"

if errorlevel 1 (
    popd
    echo ERROR: Windows could not start Media Organizer.
    goto :failed
)

popd

echo Launch command sent.
echo.
echo If the app does not appear, upload:
echo   %LOGDIR%\launcher.log
echo.
timeout /t 5 /nobreak >nul
exit /b 0


:self_update
set "LATEST_VERSION="

for /f "usebackq delims=" %%V in (\`powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; try { (Invoke-WebRequest -UseBasicParsing '%LATEST_URL%').Content.Trim() } catch { '' }"\`) do (
    set "LATEST_VERSION=%%V"
)

if not defined LATEST_VERSION exit /b 0

set /a CURRENT_NUM=%LAUNCHER_VERSION% >nul 2>&1
set /a LATEST_NUM=!LATEST_VERSION! >nul 2>&1

if !LATEST_NUM! LEQ !CURRENT_NUM! exit /b 0

set "NEW_NAME=MediaOrganizer-Launcher-!LATEST_NUM!.bat"
set "NEW_PATH=%APPDIR%\!NEW_NAME!"
set "NEW_URL=%LAUNCHER_BASE%/!NEW_NAME!"

echo New launcher found: v!LATEST_NUM!
echo Downloading !NEW_NAME!...

set "MEDIAORG_UPDATE_URL=!NEW_URL!"
set "MEDIAORG_UPDATE_PATH=!NEW_PATH!"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing $env:MEDIAORG_UPDATE_URL -OutFile $env:MEDIAORG_UPDATE_PATH"

if errorlevel 1 (
    echo Launcher update failed. Continuing with v%LAUNCHER_VERSION%.
    exit /b 0
)

if not exist "!NEW_PATH!" (
    echo Launcher update was not saved. Continuing with v%LAUNCHER_VERSION%.
    exit /b 0
)

echo Starting !NEW_NAME!...
start "" "!NEW_PATH!"
set "LAUNCHER_UPDATED=1"
exit /b 0


:ensure_git
where git >nul 2>&1
if not errorlevel 1 (
    set "GIT_EXE=git"
    exit /b 0
)

if exist "%ProgramFiles%\Git\cmd\git.exe" (
    set "GIT_EXE=%ProgramFiles%\Git\cmd\git.exe"
    exit /b 0
)

echo Git was not found. Installing Git...
where winget >nul 2>&1
if errorlevel 1 (
    echo ERROR: Git is missing and winget is unavailable.
    exit /b 1
)

winget install --id Git.Git -e --source winget --silent --accept-source-agreements --accept-package-agreements
if errorlevel 1 exit /b 1

if exist "%ProgramFiles%\Git\cmd\git.exe" (
    set "GIT_EXE=%ProgramFiles%\Git\cmd\git.exe"
    exit /b 0
)

where git >nul 2>&1
if not errorlevel 1 (
    set "GIT_EXE=git"
    exit /b 0
)

exit /b 1


:ensure_jdk21
set "JAVA_HOME="

for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do (
    if exist "%%~fD\bin\javac.exe" (
        set "JAVA_HOME=%%~fD"
        goto :verify_jdk21
    )
)

for /d %%D in ("%ProgramFiles%\Java\jdk-21*") do (
    if exist "%%~fD\bin\javac.exe" (
        set "JAVA_HOME=%%~fD"
        goto :verify_jdk21
    )
)

for /d %%D in ("%ProgramFiles%\Microsoft\jdk-21*") do (
    if exist "%%~fD\bin\javac.exe" (
        set "JAVA_HOME=%%~fD"
        goto :verify_jdk21
    )
)

echo JDK 21 was not found. Installing Eclipse Temurin JDK 21...

where winget >nul 2>&1
if errorlevel 1 (
    echo ERROR: winget is unavailable, so JDK 21 cannot be installed automatically.
    exit /b 1
)

winget install --id EclipseAdoptium.Temurin.21.JDK -e --source winget --silent --accept-source-agreements --accept-package-agreements
if errorlevel 1 (
    echo ERROR: JDK 21 installation failed.
    exit /b 1
)

for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do (
    if exist "%%~fD\bin\javac.exe" (
        set "JAVA_HOME=%%~fD"
        goto :verify_jdk21
    )
)

echo ERROR: JDK 21 installed, but javac.exe could not be found.
exit /b 1


:verify_jdk21
if not defined JAVA_HOME exit /b 1
if not exist "%JAVA_HOME%\bin\javac.exe" exit /b 1

for /f "tokens=2" %%V in ('"%JAVA_HOME%\bin\javac.exe" -version 2^>^&1') do set "JAVAC_VERSION=%%V"
for /f "tokens=1 delims=." %%M in ("!JAVAC_VERSION!") do set "JAVAC_MAJOR=%%M"

set /a JAVAC_MAJOR_NUM=!JAVAC_MAJOR! >nul 2>&1
if !JAVAC_MAJOR_NUM! LSS 21 exit /b 1

set "PATH=%JAVA_HOME%\bin;%PATH%"
exit /b 0


:ensure_maven
where mvn >nul 2>&1
if not errorlevel 1 (
    set "MVN_EXE=mvn.cmd"
    exit /b 0
)

if exist "%MAVEN_DIR%\bin\mvn.cmd" (
    set "MVN_EXE=%MAVEN_DIR%\bin\mvn.cmd"
    exit /b 0
)

echo Maven was not found. Downloading Maven %MAVEN_VERSION%...

if not exist "%TOOLROOT%" mkdir "%TOOLROOT%" >nul 2>&1

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue';" ^
  "$primary='https://dlcdn.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip';" ^
  "$archive='https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip';" ^
  "try { Invoke-WebRequest -UseBasicParsing $primary -OutFile '%MAVEN_ZIP%' } catch { Invoke-WebRequest -UseBasicParsing $archive -OutFile '%MAVEN_ZIP%' };" ^
  "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%TOOLROOT%' -Force"

if errorlevel 1 (
    echo ERROR: Maven download failed.
    exit /b 1
)

del "%MAVEN_ZIP%" >nul 2>&1

if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
    echo ERROR: Maven downloaded, but mvn.cmd was not found.
    exit /b 1
)

set "MVN_EXE=%MAVEN_DIR%\bin\mvn.cmd"
exit /b 0


:git_failed
echo.
echo ERROR: Could not download/update Media Organizer from GitHub.
goto :failed


:failed
echo.
echo Setup or launch failed.
echo.
echo If a launcher log exists, upload:
echo   %LOGDIR%\launcher.log
echo.
pause
exit /b 1
