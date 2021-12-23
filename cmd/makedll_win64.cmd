@ECHO OFF

REM
REM Dieses Skript verwendet das DLLTOOL und den GCC fuer Windows 64 Bit
REM aus dem Projekt www.mingw.org
REM

dlltool -e ..\src\lib\exports_win64.o ..\src\lib\deviceio_win64.o
gcc ..\src\lib\deviceio_win64.o ..\src\lib\exports_win64.o -lmpr -lwinmm -m64 -mdll -s -o ..\src\lib\jkcemu_win64.dll
