[![Build status](https://ci.appveyor.com/api/projects/status/mkgj9ab635dp7hfo/branch/master?svg=true)](https://ci.appveyor.com/project/rexut/jkcemu/branch/master)

JKCEMU - Java KC Emulator
=========================

JKCEMU is a software emulator primarily developed in Java that
mimics the most of the "Heim- und Kleincomputer" produced in
the GDR as well as most of already published self-made computer.

The emulation supports a number of additional hardware, e.g.
floppy disk drives, extended graphic modules, Joysticks and
specific network and USB adapters.

**Emulated computers**:

A5105 (BIC, ALBA PC 1505), AC1 and AC1-2010, BCS3, C-80, HC900,
Huebler/Evert-MC, Huebler-Grafik-MC, KC85/1, KC85/2, KC85/3,
KC85/4, KC85/5, KC87, KC compact, Kramer-MC, LC-80, LLC1, LLC2,
PC/M (Mugler/Mathes-PC), Poly-Computer 880, Schachcomputer SC2,
Schach- und Lerncomputer SLC1, VCS80, Z1013, Z9001, ZX-Spectrum

**Integrated Tools**:

Assembler, Disassembler (Reassembler), BASIC Compiler, Debugger,
Image Viewer, Calculator, File Browser, File Converter, Memory
Editor, Text-File Editor, Hex-File Editor, Hex-File Comparison,
Checksum and Hash Value Calculation

## Compilation

### Requirements

You will need the Java Development Kit (JDK) Version 6 or later
and [ANT](https://ant.apache.org/). You can also easily use the
[OpenJDK](https://openjdk.java.net/).

### Get the Code

```
git clone https://github.com/lipro/jkcemu.git
cd jkcemu
```

### Build the JAR and execute

```
ant jar
java -jar jkcemu.jar
```

---

This is an unofficial fork!
===========================

Original written by Jens Müller <jens@mueller-franke.de> and
distributed under the GNU General Public License version 3.

*Primary-site*: http://www.jens-mueller.org/jkcemu/
