@echo off
setlocal
cd /d "%~dp0"
title Projector info for Claude

set ADB=adb
if exist "%~dp0adb.exe" set ADB="%~dp0adb.exe"

echo.
echo  Collects details about the projector and opens them for you.
echo  (The PC and projector must be on the same network, e.g. your phone's hotspot.)
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
set OUT=%USERPROFILE%\projector-info.txt

echo  Collecting, this takes a few seconds...
> "%OUT%" echo === HOME APPS ===
%D% shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME >> "%OUT%" 2>&1
>> "%OUT%" echo === LAUNCHABLE APPS ===
%D% shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER >> "%OUT%" 2>&1
>> "%OUT%" echo === LEANBACK APPS ===
%D% shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER >> "%OUT%" 2>&1
>> "%OUT%" echo === STOCK LAUNCHER PARTS ===
%D% shell "dumpsys package com.htc.luminaos | grep -o 'com.htc.luminaos/[A-Za-z0-9_.$]*' | sort -u" >> "%OUT%" 2>&1
%D% shell "dumpsys package com.htc.htclauncherhighenglishd08 | grep -o 'com.htc.htclauncherhighenglishd08/[A-Za-z0-9_.$]*' | sort -u" >> "%OUT%" 2>&1
%D% shell "dumpsys package com.softwinner.awlivetv | grep -o 'com.softwinner.awlivetv/[A-Za-z0-9_.$]*' | sort -u" >> "%OUT%" 2>&1
>> "%OUT%" echo === KEYSTONE SEARCH ===
%D% shell "pm list packages -f | grep -iE 'keystone|projection|trapez|correct|htc|softwinner|hotack|amazon|prime|netflix|youtube'" >> "%OUT%" 2>&1
>> "%OUT%" echo === ALL PACKAGES ===
%D% shell pm list packages >> "%OUT%" 2>&1
>> "%OUT%" echo === DISABLED ===
%D% shell pm list packages -d >> "%OUT%" 2>&1
>> "%OUT%" echo === TIME ZONE ===
%D% shell getprop persist.sys.timezone >> "%OUT%" 2>&1
>> "%OUT%" echo === MEMORY ===
%D% shell "dumpsys meminfo | head -45" >> "%OUT%" 2>&1

echo.
echo  Done. A folder window will open with projector-info.txt selected.
echo  Drag that file into the chat with Claude.
explorer /select,"%OUT%"
pause
