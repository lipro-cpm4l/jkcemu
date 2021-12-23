@ECHO OFF

REM
REM Dieses Skript verwendet das DLLTOOL und den GCC fuer Windows 32 Bit
REM aus dem Projekt www.mingw.org
REM
REM Wenn Sie dieses Skript auf einem Windows x64 System
REM mit GCC fuer Windows x64 ausfuehren und es erscheint
REM die Fehlermeldung "cannot find -lmsvcrt", dann kann
REM der GCC die Datei MSVCRT.DLL in der 32-Bit-Version nicht finden.
REM In dem Fall koennen Sie das Verzeichnis "C:\Windows" nach
REM dieser DLL durchsuchen und falls sie in einem x86-Ordner
REM gefunden wird, koennen Sie das gefundene Verzeichnis
REM mit der Option -B dem GCC bekanntmachen, also z.B. so:
REM
REM gcc -B C:\Windows\winsxs\x86_microsoft-windows-msvcrt_... ..\src\lib\deviceio_win32.o ..\src\lib\exports_win32.o -lwinmm -m32 -mdll -s -o ..\src\lib\jkcemu_win32.dll
REM
REM Es ist aber einfacher, auf einem Windows x64 System
REM die 32 Bit Version von MinGW zu verwenden.
REM

dlltool -e ..\src\lib\exports_win32.o ..\src\lib\deviceio_win32.o
gcc ..\src\lib\deviceio_win32.o ..\src\lib\exports_win32.o -lmpr -lwinmm -m32 -mdll -s -o ..\src\lib\jkcemu_win32.dll
