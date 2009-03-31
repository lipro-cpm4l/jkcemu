#!/bin/sh

cd ../src
jar cvmf Manifest.txt ../jkcemu.jar \
  jkcemu/*.class \
  jkcemu/audio/*.class \
  jkcemu/base/*.class \
  jkcemu/filebrowser/*.class \
  jkcemu/image/*.class \
  jkcemu/print/*.class \
  jkcemu/programming/*.class \
  jkcemu/programming/assembler/*.class \
  jkcemu/programming/basic/*.class \
  jkcemu/system/*.class \
  jkcemu/system/z1013/*.class \
  jkcemu/text/*.class \
  jkcemu/tools/*.class \
  jkcemu/tools/calculator/*.class \
  jkcemu/tools/hexdiff/*.class \
  jkcemu/tools/hexeditor/*.class \
  z80emu/*.class \
  rom/ac1/*.bin \
  rom/bcs3/*.bin \
  rom/c80/*.bin \
  rom/huebler/*.bin \
  rom/kc85/*.bin \
  rom/kramermc/*.bin \
  rom/lc80/*.bin \
  rom/llc2/*.bin \
  rom/poly880/*.bin \
  rom/vcs80/*.bin \
  rom/z1013/*.bin \
  rom/z9001/*.bin \
  images/debug/*.png \
  images/edit/*.png \
  images/file/*.png \
  images/icon/*.png \
  images/nav/*.png \
  help/*.htm \
  help/bcs3/*.htm \
  help/kramermc/*.htm \
  help/tips/*.htm \
  help/tools/*.htm \
  help/tools/basicc/*.htm \
  help/z1013/*.htm

