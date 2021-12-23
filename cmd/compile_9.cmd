@ECHO OFF

REM
REM Compilieren der Klassen mit speziellem Code fuer Java 9
REM

set SRC_DIR=..\src

del %SRC_DIR%\jkcemu\base\jversion\*_9*.class

javac -classpath %SRC_DIR% %1 %2 %3 ^
  %SRC_DIR%\jkcemu\base\jversion\*_9.java
