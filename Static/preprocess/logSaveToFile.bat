@Rem 日志输出到文件中
@echo off
adb logcat -s %1 -v time > %2
