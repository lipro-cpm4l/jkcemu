@ECHO OFF

set SRC_DIR=..\src

del %SRC_DIR%\jkcemu\*.class
del %SRC_DIR%\jkcemu\audio\*.class
del %SRC_DIR%\jkcemu\base\*.class
del %SRC_DIR%\jkcemu\filebrowser\*.class
del %SRC_DIR%\jkcemu\image\*.class
del %SRC_DIR%\jkcemu\print\*.class
del %SRC_DIR%\jkcemu\programming\*.class
del %SRC_DIR%\jkcemu\programming\assembler\*.class
del %SRC_DIR%\jkcemu\programming\basic\*.class
del %SRC_DIR%\jkcemu\text\*.class
del %SRC_DIR%\jkcemu\tools\*.class
del %SRC_DIR%\jkcemu\tools\calculator\*.class
del %SRC_DIR%\jkcemu\tools\hexdiff\*.class
del %SRC_DIR%\jkcemu\tools\hexeditor\*.class
del %SRC_DIR%\jkcemu\ac1\*.class
del %SRC_DIR%\jkcemu\bcs3\*.class
del %SRC_DIR%\jkcemu\kc85\*.class
del %SRC_DIR%\jkcemu\z1013\*.class
del %SRC_DIR%\jkcemu\z9001\*.class
del %SRC_DIR%\z80emu\*.class

javac -classpath %SRC_DIR% %1 %2 %3 %SRC_DIR%\jkcemu\Main.java

