@ECHO OFF

dlltool -e ..\src\lib\exports_win32.o ..\src\lib\deviceio_win32.o
gcc ..\src\lib\deviceio_win32.o ..\src\lib\exports_win32.o -lwinmm -m32 -mdll -s -o ..\src\lib\jkcemu_win32.dll
