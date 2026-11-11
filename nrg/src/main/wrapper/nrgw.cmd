@echo off
rem NRG wrapper. Runs the Nanolaba Readme Generator release pinned in .nrg\wrapper.properties,
rem downloading it once into %USERPROFILE%\.nrg\wrapper\<version>\ (or %NRG_WRAPPER_HOME%\<version>\).
rem Created by `nrg --init-wrapper`; commit it together with .nrg\wrapper.properties.
rem
rem Exit status: whatever NRG returns, or 69 when the wrapper cannot provide NRG
rem (no Java, download failed, checksum mismatch).
setlocal

set "NRGW_PROPS=%~dp0.nrg\wrapper.properties"
if not exist "%NRGW_PROPS%" (
  echo nrgw: %NRGW_PROPS% not found; run 'nrg --init-wrapper' to create it 1>&2
  exit /b 69
)

set "NRGW_VERSION="
set "NRGW_SHA256="
set "NRGW_URL="
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%NRGW_PROPS%") do (
  if /i "%%A"=="version" set "NRGW_VERSION=%%B"
  if /i "%%A"=="sha256" set "NRGW_SHA256=%%B"
  if /i "%%A"=="distributionUrl" set "NRGW_URL=%%B"
)
if not defined NRGW_VERSION (
  echo nrgw: no version= in %NRGW_PROPS% 1>&2
  exit /b 69
)
if not defined NRGW_URL set "NRGW_URL=https://github.com/nanolaba/readme-generator/releases/download/v%NRGW_VERSION%/readme-generator-%NRGW_VERSION%.zip"
if not defined NRG_WRAPPER_HOME set "NRG_WRAPPER_HOME=%USERPROFILE%\.nrg\wrapper"
set "NRGW_DIR=%NRG_WRAPPER_HOME%\%NRGW_VERSION%"
set "NRGW_JAR=%NRGW_DIR%\nrg.jar"

set "NRGW_JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "NRGW_JAVA=%JAVA_HOME%\bin\java.exe"
if not defined NRGW_JAVA (
  where java >nul 2>nul && set "NRGW_JAVA=java"
)
if not defined NRGW_JAVA (
  echo nrgw: NRG needs Java 8 or newer: install it or set JAVA_HOME 1>&2
  exit /b 69
)

if not exist "%NRGW_JAR%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $tmp = $null; try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $tmp = $env:NRGW_DIR + '.download.' + $PID; if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }; New-Item -ItemType Directory -Force -Path $tmp, $env:NRGW_DIR | Out-Null; [Console]::Error.WriteLine('nrgw: downloading NRG ' + $env:NRGW_VERSION + ' from ' + $env:NRGW_URL); $zip = Join-Path $tmp 'nrg.zip'; Invoke-WebRequest -UseBasicParsing -Uri $env:NRGW_URL -OutFile $zip; $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $zip).Hash.ToLower(); $expected = ('' + $env:NRGW_SHA256).Trim().ToLower(); if (-not $expected) { try { $sumFile = Join-Path $tmp 'nrg.zip.sha256'; Invoke-WebRequest -UseBasicParsing -Uri ($env:NRGW_URL + '.sha256') -OutFile $sumFile; $expected = ((Get-Content -Raw -LiteralPath $sumFile).Trim() -split '\s+')[0].ToLower() } catch { throw ('no sha256= in ' + $env:NRGW_PROPS + ' and no ' + $env:NRGW_URL + '.sha256 to check the download against') }; [Console]::Error.WriteLine('nrgw: pin this download by adding sha256=' + $actual + ' to ' + $env:NRGW_PROPS) }; if ($actual -ne $expected) { throw ('sha256 mismatch for ' + $env:NRGW_URL + ': expected ' + $expected + ', got ' + $actual) }; Expand-Archive -LiteralPath $zip -DestinationPath $tmp -Force; $found = Get-ChildItem -LiteralPath $tmp -Recurse -Filter nrg.jar | Select-Object -First 1; if (-not $found) { throw ('nrg.jar not found in ' + $env:NRGW_URL) }; Move-Item -Force -LiteralPath $found.FullName -Destination $env:NRGW_JAR; Remove-Item -Recurse -Force $tmp } catch { [Console]::Error.WriteLine('nrgw: ' + $_.Exception.Message); if ($tmp -and (Test-Path $tmp)) { Remove-Item -Recurse -Force $tmp }; exit 69 }"
  if errorlevel 1 exit /b 69
)

"%NRGW_JAVA%" -Dfile.encoding=UTF-8 -cp "%NRGW_JAR%" com.nanolaba.nrg.NRG %*
exit /b %ERRORLEVEL%
