#!/bin/sh

SRC_DIR=../src

rm -f $SRC_DIR/jkcemu/*.class
rm -f $SRC_DIR/jkcemu/audio/*.class
rm -f $SRC_DIR/jkcemu/base/*.class
rm -f $SRC_DIR/jkcemu/filebrowser/*.class
rm -f $SRC_DIR/jkcemu/image/*.class
rm -f $SRC_DIR/jkcemu/print/*.class
rm -f $SRC_DIR/jkcemu/programming/*.class
rm -f $SRC_DIR/jkcemu/programming/assembler/*.class
rm -f $SRC_DIR/jkcemu/programming/basic/*.class
rm -f $SRC_DIR/jkcemu/text/*.class
rm -f $SRC_DIR/jkcemu/tools/*.class
rm -f $SRC_DIR/jkcemu/tools/calculator/*.class
rm -f $SRC_DIR/jkcemu/tools/hexdiff/*.class
rm -f $SRC_DIR/jkcemu/tools/hexeditor/*.class
rm -f $SRC_DIR/jkcemu/ac1/*.class
rm -f $SRC_DIR/jkcemu/bcs3/*.class
rm -f $SRC_DIR/jkcemu/kc85/*.class
rm -f $SRC_DIR/jkcemu/z1013/*.class
rm -f $SRC_DIR/jkcemu/z9001/*.class
rm -f $SRC_DIR/z80emu/*.class

javac $* -classpath $SRC_DIR \
  $SRC_DIR/jkcemu/*.java \
  $SRC_DIR/jkcemu/audio/*.java \
  $SRC_DIR/jkcemu/base/*.java \
  $SRC_DIR/jkcemu/filebrowser/*.java \
  $SRC_DIR/jkcemu/image/*.java \
  $SRC_DIR/jkcemu/print/*.java \
  $SRC_DIR/jkcemu/programming/*.java \
  $SRC_DIR/jkcemu/programming/assembler/*.java \
  $SRC_DIR/jkcemu/programming/basic/*.java \
  $SRC_DIR/jkcemu/text/*.java \
  $SRC_DIR/jkcemu/tools/*.java \
  $SRC_DIR/jkcemu/tools/calculator/*.java \
  $SRC_DIR/jkcemu/tools/hexdiff/*.java \
  $SRC_DIR/jkcemu/tools/hexeditor/*.java \
  $SRC_DIR/jkcemu/ac1/*.java \
  $SRC_DIR/jkcemu/bcs3/*.java \
  $SRC_DIR/jkcemu/kc85/*.java \
  $SRC_DIR/jkcemu/z1013/*.java \
  $SRC_DIR/jkcemu/z9001/*.java \
  $SRC_DIR/z80emu/*.java

