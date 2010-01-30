#!/bin/sh

SRC_DIR=../src

find $SRC_DIR -name "*.class" -exec rm -f {} \;

javac $* -classpath $SRC_DIR $SRC_DIR/jkcemu/Main.java

