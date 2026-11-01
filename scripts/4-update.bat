@echo off
setlocal
cd /d "%~dp0"
title Projector Home - update and tidy up

set ADB=adb
if exist "%~dp0adb.exe" set ADB="%~dp0adb.exe"

if not exist "%~dp0projector-home.apk" (
  echo  projector-home.apk is missing. Put it in this folder and run this again.
  pause
  exit /b 1
)

echo.
echo  Projector Home - update and tidy up
echo  -----------------------------------
echo  (PC and projector on the same network, e.g. your phone's hotspot.)
echo.
set /p IP= Type the projector's IP address and press Enter:
set IP=%IP: =%
set T=%IP%:5555
if not "%IP::=%"=="%IP%" set T=[%IP%]:5555
%ADB% connect %T%
%ADB% -s %T% get-state >nul 2>&1
if errorlevel 1 (
  echo.
  echo  Could not connect. Check both devices are on the same hotspot and the IP is right.
  pause
  exit /b 1
)
set D=%ADB% -s %T%

echo.
echo  [1/5] Installing the new version of the home screen...
%D% install -r "%~dp0projector-home.apk"
%D% shell cmd package set-home-activity home.projector/home.projector.HomeActivity

echo.
echo  [2/5] Setting the clock to London time...
%D% shell service call alarm 3 s16 Europe/London >nul
%D% shell settings put global auto_time 1

echo.
echo  [3/5] Switching off the extra app store, firmware updaters and factory test app...
for %%P in (com.htc.storeos com.htc.htcotaupdate android.rockchip.update.service com.htc.hyk_test) do (
  %D% shell pm disable-user --user 0 %%P
)

echo.
echo  [4/5] Switching off screen casting (AirPlay, Miracast, mirroring)...
for %%P in (com.android.toofifi com.toofifi.lineserver com.mphotool.usbcastserver com.mphotool.dlna com.mphotool.airplay com.htc.samescreen com.softwinner.miracastReceiver) do (
  %D% shell pm disable-user --user 0 %%P
)

echo.
echo  [5/5] Switching off the old home screen (with its ad services)...
%D% shell pm disable-user --user 0 com.htc.htclauncherhighenglishd08
%D% shell input keyevent KEYCODE_HOME

set OUT=%USERPROFILE%\projector-check.txt
> "%OUT%" echo === TIME ZONE ===
%D% shell getprop persist.sys.timezone >> "%OUT%" 2>&1
>> "%OUT%" echo === HOME APPS ===
%D% shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME >> "%OUT%" 2>&1
>> "%OUT%" echo === PROJECTOR SETTINGS SCREENS ===
%D% shell "dumpsys package com.htc.htcsettings | grep -o 'com.htc.htcsettings/[A-Za-z0-9_.$]*' | sort -u" >> "%OUT%" 2>&1
>> "%OUT%" echo === DISABLED ===
%D% shell pm list packages -d >> "%OUT%" 2>&1
>> "%OUT%" echo === MEMORY ===
%D% shell "cat /proc/meminfo | head -3" >> "%OUT%" 2>&1

echo.
echo  Done. Restart the projector, then test Picture and Settings.
echo  A folder window will open with projector-check.txt selected: send it to Claude.
echo  If anything is wrong, undo-cleanup.bat puts everything back.
explorer /select,"%OUT%"
pause
