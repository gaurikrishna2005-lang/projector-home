@echo off
setlocal
cd /d "%~dp0"
title Projector rescue

echo.
echo  Projector rescue: puts back the old home screen and earlier changes,
echo  and saves what went wrong.
echo  (Phone hotspot on, PC connected to it by Wi-Fi, projector switched on.)
echo.
set IP=
set /p IP= Type the projector's IP address, or just press Enter to search for it:
if not "%IP%"=="" set IP=%IP: =%
if "%IP%"=="" call :find
if "%IP%"=="" (
  echo.
  echo  Couldn't find the projector. Check the PC is on the hotspot by Wi-Fi,
  echo  unplug the projector, plug it back in, wait 1 minute and run this again.
  pause
  exit /b 1
)
echo  Using %IP%

set T=%IP%:5555
if not "%IP::=%"=="%IP%" set T=[%IP%]:5555
set TRY=0
:check
adb.exe disconnect %T% >nul 2>&1
adb.exe connect %T% >nul 2>&1
timeout /t 4 /nobreak >nul
adb.exe devices > "%TEMP%\adb-devices.txt" 2>&1
findstr /c:"%T%" "%TEMP%\adb-devices.txt" | findstr "unauthorized" >nul
if not errorlevel 1 goto unauthorized
adb.exe -s %T% get-state >nul 2>&1
if errorlevel 1 goto notready
echo.
echo  Connected.
set D=adb.exe -s %T%
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
exit /b

rem Finds the projector by scanning for the ADB port and checking its serial number.
:find
echo  Searching for the projector, this takes a few seconds...
set COUNT=0
set FIRST=
for /f "usebackq delims=" %%A in (`powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0find-projector.ps1"`) do (
  set /a COUNT+=1
  if not defined FIRST set FIRST=%%A
  adb.exe connect %%A:5555 >nul 2>&1
  adb.exe -s %%A:5555 shell getprop ro.serialno 2>nul | findstr /i "9c00108a2694c7322d5" >nul
  if not errorlevel 1 set IP=%%A
)
if "%IP%"=="" if %COUNT%==1 set IP=%FIRST%
exit /b

:unauthorized
set /a TRY+=1
if %TRY%==1 set KEYS=OK,   Down,   Right,   OK
if %TRY%==2 set KEYS=Right,   OK
if %TRY%==3 set KEYS=OK
if %TRY%==4 set KEYS=Down,   Right,   OK
if %TRY%==5 set KEYS=Down,   Down,   OK
if %TRY% GTR 5 goto giveup
echo.
echo  ----------------------------------------------------------
echo  Attempt %TRY% of 5: the projector is asking "Allow debugging?"
echo  on the dark screen. Point the remote at it and press slowly,
echo  one second apart:
echo.
echo        %KEYS%
echo.
echo  Then come back and press any key HERE to check.
pause >nul
goto check

:giveup
echo.
echo  None of the key combinations worked. Send Claude a photo of this window.
pause
exit /b 1

:notready
echo.
echo  The projector is on the hotspot but not answering yet (see the line above).
echo  Wait one minute and run rescue.bat again. If it still fails, send Claude a photo.
pause
exit /b 1
