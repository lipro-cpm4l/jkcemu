@ECHO OFF

dlltool -e ..\src\lib\exports_win64.o ..\src\lib\deviceio_win64.o
gcc ..\src\lib\deviceio_win64.o ..\src\lib\exports_win64.o -lwinmm -m64 -mdll -s -o ..\src\lib\jkcemu_win64.dll
