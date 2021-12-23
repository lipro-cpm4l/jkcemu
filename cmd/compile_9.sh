#!/bin/sh

#
# Compilieren der Klassen mit speziellem Code fuer Java 9
#

SRC_DIR=../src

rm -f $SRC_DIR/jkcemu/base/jversion/*_9*.class

javac -classpath $SRC_DIR $* $SRC_DIR/jkcemu/base/jversion/*_9.java
