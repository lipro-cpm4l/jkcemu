#!/bin/sh

# Script zum Compilieren des C#-Programms WDnsFile
# unter Linux (mono erforderlich)

SRC_DIR=../src/cs

gmcs -out:$SRC_DIR/wdnsfile.exe $SRC_DIR/WDnsFile.cs

