@ECHO OFF

REM
REM Dieses Skript verwendet den GCC,
REM unter Windows verfuegbar durch das Projekt www.mingw.org
REM
REM Die Umgebungsvariable JAVA_HOME muss gesetzt sein!
REM

javah -classpath ..\src -o ..\src\lib\deviceio.h jkcemu.base.deviceio.WinDeviceIO
gcc -I "%JAVA_HOME%\include" -I "%JAVA_HOME%\include\win32" -m32 -o ..\src\lib\deviceio_win32.s -S ..\src\lib\deviceio_win.c

REM
REM An die Java-Methodennamen hat der Compiler @... angehaengt.
REM Diese muessen nun entfernt werden.
REM Das erledigt ein kleines Java-Programm,
REM welches hier compiliert und ausgefuehrt wird.
REM

javac -cp ..\etc ..\etc\ConvertAsmWin32Lib.java
java -cp ..\etc ConvertAsmWin32Lib ..\src\lib\deviceio_win32.s

gcc -m32 -o ..\src\lib\deviceio_win32.o -c ..\src\lib\deviceio_win32.s
