@echo off
setlocal
cd /d "%~dp0"
title Projector rescue

set ADB=adb
if exist "%~dp0adb.exe" set ADB="%~dp0adb.exe"

echo.
echo  Projector rescue: puts back the old home screen and earlier changes, and saves what went wrong.
echo  (PC and projector on the same hotspot as before.)
echo.
set /p IP= Type the projector's IP address and press Enter:
set IP=%IP: =%
set T=%IP%:5555
if not "%IP::=%"=="%IP%" set T=[%IP%]:5555
%ADB% connect %T%
%ADB% -s %T% get-state >nul 2>&1
if errorlevel 1 (
  echo.
  echo  Could not connect. Unplug the projector, plug it back in, wait 1 minute
  echo  for it to rejoin the hotspot, then run this again.
  pause
  exit /b 1
)
set D=%ADB% -s %T%
set OUT=%USERPROFILE%\projector-rescue.txt

echo  Saving details...
> "%OUT%" echo === CRASHES ===
%D% logcat -d -b crash >> "%OUT%" 2>&1
>> "%OUT%" echo === ON SCREEN NOW ===
%D% shell "dumpsys activity activities | grep -E 'mResumedActivity|mFocusedApp|topResumedActivity'" >> "%OUT%" 2>&1
>> "%OUT%" echo === TIME ===
%D% shell date >> "%OUT%" 2>&1
%D% shell getprop persist.sys.timezone >> "%OUT%" 2>&1
>> "%OUT%" echo === DISPLAY ===
%D% shell "dumpsys power | grep -iE 'mWakefulness|Display Power|mScreenOn'" >> "%OUT%" 2>&1
>> "%OUT%" echo === RECENT LOG ===
%D% shell "logcat -d -t 400 | grep -iE 'htc|home.projector|keystone|display|AndroidRuntime|FATAL|ActivityTaskManager'" >> "%OUT%" 2>&1

echo  Taking a screenshot of what the projector is trying to show...
%D% exec-out screencap -p > "%USERPROFILE%\projector-screen.png"

echo  Putting the earlier changes back...
%D% shell pm enable com.htc.htclauncherhighenglishd08
%D% shell pm enable com.hotack.writesn
%D% shell settings put global window_animation_scale 1
%D% shell settings put global transition_animation_scale 1
%D% shell settings put global animator_duration_scale 1
%D% shell cmd package set-home-activity com.htc.htclauncherhighenglishd08/com.htc.activity.HomeActivity
%D% shell input keyevent KEYCODE_WAKEUP
%D% shell input keyevent KEYCODE_HOME

echo.
echo  Done. Is there a picture on the wall now?
echo  A folder window will open: send projector-rescue.txt and projector-screen.png to Claude.
explorer /select,"%OUT%"
pause
