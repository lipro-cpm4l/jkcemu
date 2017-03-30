@ECHO OFF

set SRC_DIR=..\src

del %SRC_DIR%\jkcemu\*.class
del %SRC_DIR%\jkcemu\audio\*.class
del %SRC_DIR%\jkcemu\base\*.class
del %SRC_DIR%\jkcemu\disk\*.class
del %SRC_DIR%\jkcemu\emusys\*.class
del %SRC_DIR%\jkcemu\emusys\a5105\*.class
del %SRC_DIR%\jkcemu\emusys\ac1_llc2\*.class
del %SRC_DIR%\jkcemu\emusys\bcs3\*.class
del %SRC_DIR%\jkcemu\emusys\customsys\*.class
del %SRC_DIR%\jkcemu\emusys\etc\*.class
del %SRC_DIR%\jkcemu\emusys\huebler\*.class
del %SRC_DIR%\jkcemu\emusys\kc85\*.class
del %SRC_DIR%\jkcemu\emusys\kccompact\*.class
del %SRC_DIR%\jkcemu\emusys\lc80\*.class
del %SRC_DIR%\jkcemu\emusys\llc1\*.class
del %SRC_DIR%\jkcemu\emusys\poly880\*.class
del %SRC_DIR%\jkcemu\emusys\z1013\*.class
del %SRC_DIR%\jkcemu\emusys\z9001\*.class
del %SRC_DIR%\jkcemu\emusys\zxspectrum\*.class
del %SRC_DIR%\jkcemu\etc\*.class
del %SRC_DIR%\jkcemu\filebrowser\*.class
del %SRC_DIR%\jkcemu\image\*.class
del %SRC_DIR%\jkcemu\joystick\*.class
del %SRC_DIR%\jkcemu\net\*.class
del %SRC_DIR%\jkcemu\print\*.class
del %SRC_DIR%\jkcemu\programming\*.class
del %SRC_DIR%\jkcemu\programming\assembler\*.class
del %SRC_DIR%\jkcemu\programming\basic\*.class
del %SRC_DIR%\jkcemu\programming\basic\target\*.class
del %SRC_DIR%\jkcemu\text\*.class
del %SRC_DIR%\jkcemu\tools\*.class
del %SRC_DIR%\jkcemu\tools\calculator\*.class
del %SRC_DIR%\jkcemu\tools\debugger\*.class
del %SRC_DIR%\jkcemu\tools\fileconverter\*.class
del %SRC_DIR%\jkcemu\tools\hexdiff\*.class
del %SRC_DIR%\jkcemu\tools\hexedit\*.class
del %SRC_DIR%\z80emu\*.class

javac -classpath %SRC_DIR% %1 %2 %3 %SRC_DIR%\jkcemu\Main.java
