@Rem 签名，并安装
@echo off
if exist %1\%2_apktool rd /s /q %1\%2_apktool
md %1\%2_apktool
if "%PATH_BASE%" == "" set PATH_BASE=%PATH%
set PATH=%CD%;%PATH_BASE%;
java -jar -Duser.language=en "%~dp0\apktool.jar" d -f %1\%2.apk -o %1\%2_apktool
