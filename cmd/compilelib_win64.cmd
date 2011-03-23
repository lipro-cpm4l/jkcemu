@ECHO OFF

javah -classpath ..\src -o ..\src\lib\deviceio.h jkcemu.base.DeviceIO
gcc -I %JAVA_HOME%\include -I %JAVA_HOME%\include\win32 -m64 -o ..\src\lib\deviceio_win64.o -c ..\src\lib\deviceio_win.c
