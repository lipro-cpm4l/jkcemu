@ECHO OFF

set SRC_DIR=..\src

del %SRC_DIR%\jkcemu\*.class
del %SRC_DIR%\jkcemu\audio\*.class
del %SRC_DIR%\jkcemu\base\*.class
del %SRC_DIR%\jkcemu\disk\*.class
del %SRC_DIR%\jkcemu\filebrowser\*.class
del %SRC_DIR%\jkcemu\image\*.class
del %SRC_DIR%\jkcemu\print\*.class
del %SRC_DIR%\jkcemu\programming\*.class
del %SRC_DIR%\jkcemu\programming\assembler\*.class
del %SRC_DIR%\jkcemu\programming\basic\*.class
del %SRC_DIR%\jkcemu\system\*.class
del %SRC_DIR%\jkcemu\system\kc85\*.class
del %SRC_DIR%\jkcemu\system\z1013\*.class
del %SRC_DIR%\jkcemu\text\*.class
del %SRC_DIR%\jkcemu\tools\*.class
del %SRC_DIR%\jkcemu\tools\calculator\*.class
del %SRC_DIR%\jkcemu\tools\hexdiff\*.class
del %SRC_DIR%\jkcemu\tools\hexedit\*.class
del %SRC_DIR%\z80emu\*.class

javac -classpath %SRC_DIR% %1 %2 %3 %SRC_DIR%\jkcemu\Main.java

