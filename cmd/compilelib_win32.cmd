@ECHO OFF

javah -classpath ..\src -o ..\src\lib\deviceio.h jkcemu.base.DeviceIO
gcc -I %JAVA_HOME%\include -I %JAVA_HOME%\include\win32 -m32 -o ..\src\lib\deviceio_win32.s -S ..\src\lib\deviceio_win.c

echo Achtung!
echo An die Java-Methodennamen hat der Compiler @... angehaengt.
echo Diese muessen nun entfernt werden.
echo Oeffnen Sie in einem Texteditor die Datei ..\src\lib\deviceio_win32.s
echo und entfernen darin die angehaengten @... aus den Java-Methodennamen.
echo Jeder Java-Methodenname ist viermal vorhanden
echo (Marke, .def, .globl und .ascii),
echo d.h. Sie muessen pro Methodenname viermal das angehaengte @... entfernen!
echo Anschliessend fuehren Sie bitte folgende Kommandozeile aus:
echo gcc -m32 -o ..\src\lib\deviceio_win32.o -c ..\src\lib\deviceio_win32.s
