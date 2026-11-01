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

for %%P in (com.hotack.writesn com.htc.htclauncherhighenglishd08 com.htc.storeos com.htc.htcotaupdate android.rockchip.update.service com.htc.hyk_test com.android.toofifi com.toofifi.lineserver com.mphotool.usbcastserver com.mphotool.dlna com.mphotool.airplay com.htc.samescreen com.softwinner.miracastReceiver) do (
  %D% shell pm enable %%P
)
%D% shell settings put global window_animation_scale 1
%D% shell settings put global transition_animation_scale 1
%D% shell settings put global animator_duration_scale 1
%D% shell pm uninstall home.projector

echo.
echo  Done. Restart the projector.
pause
