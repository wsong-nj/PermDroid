@Rem 签名，并安装
@echo off
jarsigner -keystore "%~dp0\debug.keystore" -storepass android -signedjar %2.apk -digestalg SHA1 -sigalg MD5withRSA %1.apk androiddebugkey
rem adb install -r %1\%2_1.apk
