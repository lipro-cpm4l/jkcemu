@ECHO OFF

REM
REM Dieses Skript verwendet den GCC,
REM unter Windows verfuegbar durch das Projekt www.mingw.org
REM
REM Die Umgebungsvariable JAVA_HOME muss gesetzt sein!
REM

javah -classpath ..\src -o ..\src\lib\deviceio.h jkcemu.base.deviceio.WinDeviceIO
gcc -I "%JAVA_HOME%\include" -I "%JAVA_HOME%\include\win32" -m64 -o ..\src\lib\deviceio_win64.o -c ..\src\lib\deviceio_win.c
