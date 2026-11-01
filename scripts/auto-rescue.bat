@echo off
setlocal
cd /d "%~dp0"
title Projector auto-rescue

echo.
echo  Projector auto-rescue (no typing needed)
echo  ---------------------------------------
echo  Phone hotspot on, PC on it by Wi-Fi, projector switched on.
echo.

set IP=
call :find
if "%IP%"=="" set IP=172.20.10.3
echo  Projector: %IP%
set T=%IP%:5555

set N=0
:window
set /a N+=1
if %N% GTR 10 goto giveup
set /a K=(N-1) %% 5 + 1
if %K%==1 set KEYS=OK,   Down,   Right,   OK
if %K%==2 set KEYS=Right,   OK
if %K%==3 set KEYS=OK
if %K%==4 set KEYS=Down,   Right,   OK
if %K%==5 set KEYS=Down,   Down,   OK
adb.exe disconnect %T% >nul 2>&1
adb.exe connect %T% >nul 2>&1
adb.exe -s %T% get-state >nul 2>&1
if not errorlevel 1 goto connected
echo.
echo  ==========================================================
echo   Attempt %N% of 10.  Point the remote at the projector and
echo   press slowly, one second apart:
echo.
echo        %KEYS%
echo.
echo   (waiting 30 seconds for it to work...)
echo  ==========================================================
set W=0
:poll
timeout /t 2 /nobreak >nul
adb.exe -s %T% get-state >nul 2>&1
if not errorlevel 1 goto connected
set /a W+=2
if %W% LSS 30 goto poll
goto window

:connected
echo.
echo  *** CONNECTED ***  You can stop pressing buttons.
set D=adb.exe -s %T%
set OUT=%~dp0projector-rescue.txt

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
%D% shell "dumpsys display | grep -iE 'mScreenState|mActualState|mDisplayInfos|mode' | head -20" >> "%OUT%" 2>&1
>> "%OUT%" echo === RECENT LOG ===
%D% shell "logcat -d -t 600 | grep -iE 'htc|home.projector|keystone|display|AndroidRuntime|FATAL|ActivityTaskManager|panel|lcd|backlight'" >> "%OUT%" 2>&1

echo  Taking a screenshot of what the projector is trying to show...
%D% exec-out screencap -p > "%~dp0projector-screen.png"

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
echo  A folder window opens with projector-rescue.txt; projector-screen.png is next to it.
explorer /select,"%OUT%"
timeout /t 900 >nul
exit /b

:giveup
echo.
echo  None of the combinations worked in 10 attempts. Tell Claude.
timeout /t 900 >nul
exit /b 1

rem Finds the projector by scanning for the ADB port and checking its serial number.
:find
echo  Searching for the projector, this takes a few seconds...
set COUNT=0
set FIRST=
for /f "usebackq delims=" %%A in (`powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0find-projector.ps1"`) do (
  set /a COUNT+=1
  if not defined FIRST set FIRST=%%A
)
if %COUNT% GEQ 1 set IP=%FIRST%
exit /b
