@echo off
setlocal
cd /d "%~dp0"
title Projector cleanup - step 1

rem Use adb.exe from this folder (put this file inside platform-tools)
set ADB=adb
if exist "%~dp0adb.exe" set ADB="%~dp0adb.exe"

echo.
echo  Projector cleanup - step 1
echo  --------------------------
echo  Make sure "ADB Debugging" is ON in the projector's Developer options.
echo.
set /p IP= Type the projector's IP address and press Enter:

%ADB% disconnect >nul 2>&1
%ADB% connect %IP%
echo.
echo  If the projector shows "Allow debugging?", tick "Always allow" and press OK.
echo  Then come back here.
pause

%ADB% -s %IP%:5555 get-state >nul 2>&1
if errorlevel 1 (
  echo.
  echo  Could not connect. Check the IP address and that ADB Debugging is on, then run this again.
  pause
  exit /b 1
)
set D=%ADB% -s %IP%:5555

echo.
echo  [1/3] Switching off the suspected proxy apps...
%D% shell pm disable-user --user 0 com.hotack.silentsdk
%D% shell pm disable-user --user 0 com.hotack.writesn

echo.
echo  [2/3] Turning off animations (menus become instant)...
%D% shell settings put global window_animation_scale 0
%D% shell settings put global transition_animation_scale 0
%D% shell settings put global animator_duration_scale 0

echo.
echo  [3/3] Collecting info for Claude into projector-info.txt...
(
  echo === HOME APPS ===
  %D% shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME
  echo === LAUNCHABLE APPS ===
  %D% shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
  echo === LEANBACK APPS ===
  %D% shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER
  echo === STOCK LAUNCHER PARTS ===
  %D% shell "dumpsys package com.htc.luminaos | grep -o 'com.htc.luminaos/[A-Za-z0-9_.$]*' | sort -u"
  %D% shell "dumpsys package com.htc.htclauncherhighenglishd08 | grep -o 'com.htc.htclauncherhighenglishd08/[A-Za-z0-9_.$]*' | sort -u"
  %D% shell "dumpsys package com.softwinner.awlivetv | grep -o 'com.softwinner.awlivetv/[A-Za-z0-9_.$]*' | sort -u"
  echo === KEYSTONE SEARCH ===
  %D% shell "pm list packages -f | grep -iE 'keystone|projection|trapez|correct|htc|softwinner|hotack'"
  echo === ALL PACKAGES ===
  %D% shell pm list packages
  echo === DISABLED ===
  %D% shell pm list packages -d
  echo === MEMORY ===
  %D% shell "dumpsys meminfo | head -45"
) > projector-info.txt 2>&1

echo.
echo  Done. Upload projector-info.txt (in this folder) to Claude.
echo  Everything here can be undone with undo-cleanup.bat
echo.
pause
