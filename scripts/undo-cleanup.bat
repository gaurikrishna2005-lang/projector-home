@echo off
setlocal
cd /d "%~dp0"
title Projector cleanup - undo everything

set ADB=adb
if exist "%~dp0adb.exe" set ADB="%~dp0adb.exe"

echo.
echo  This puts the projector back the way it came:
echo  stock home screen, original apps and animations.
echo.
set /p IP= Type the projector's IP address and press Enter:
set IP=%IP: =%
set T=%IP%:5555
if not "%IP::=%"=="%IP%" set T=[%IP%]:5555
%ADB% connect %T%
set D=%ADB% -s %T%

for %%P in (com.hotack.silentsdk com.hotack.writesn com.htc.luminaos com.htc.htclauncherhighenglishd08) do (
  %D% shell pm enable %%P
)
%D% shell settings put global window_animation_scale 1
%D% shell settings put global transition_animation_scale 1
%D% shell settings put global animator_duration_scale 1
%D% shell pm uninstall home.projector

echo.
echo  Done. Restart the projector.
pause
