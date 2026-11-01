@echo off
setlocal
cd /d "%~dp0"
title Projector Home - step 2 install

set ADB=adb
if exist "%~dp0adb.exe" set ADB="%~dp0adb.exe"

if not exist "%~dp0projector-home.apk" (
  echo  projector-home.apk is missing. Put it in this folder and run this again.
  pause
  exit /b 1
)

echo.
echo  Projector Home - step 2
echo  -----------------------
set /p IP= Type the projector's IP address and press Enter:
%ADB% connect %IP%
set D=%ADB% -s %IP%:5555

echo.
echo  [1/3] Installing the new home screen...
%D% install -r "%~dp0projector-home.apk"

echo.
echo  [2/3] Making it the default home screen...
%D% shell cmd package set-home-activity home.projector/home.projector.HomeActivity

echo.
echo  [3/3] Opening it...
%D% shell am start -a android.intent.action.MAIN -c android.intent.category.HOME

echo.
echo  Now press HOME on the remote. If a "choose app" box appears,
echo  pick "Home" and "Always". Then tell Claude what you see.
echo  The old home screen is NOT removed yet.
echo.
pause
