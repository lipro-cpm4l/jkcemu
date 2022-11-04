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
KC85/4, KC85/5, KC87, KC compact, Kramer-MC, LC-80 and LC-80ex,
LLC1, LLC2, NANOS, PC/M (Mugler/Mathes-PC), Poly-Computer 880,
Schachcomputer SC2, Schach- und Lerncomputer SLC1, VCS80, Z1013,
Z9001, ZX-Spectrum, *user customizable system*

**Integrated Tools**:

Assembler, Disassembler (Reassembler), BASIC Compiler, Debugger,
Audio Recorder, Floppy Disk Image Inspector, Calculator, File
Browser, File Converter, File Search Engine, Image Viewer, Memory
Editor, Text-File Editor, Hex-File Editor, Hex-File Comparison,
Checksum and Hash Value Calculation

**Special Features**:

Network Emulation, USB Emulation, Floppy Disk Emulation, Hard Disk
Emulation (GIDE), CP/M Floppy Disk Image creation, CP/M Floppy
Disk (Image) packaging, Files and Programs stored on Audio
Compact Casset, Joysticks, ZEXALL and ZEXDOC

## Compilation

### Requirements

You will need the Java Development Kit (JDK) Version 7 or later
and [ANT](https://ant.apache.org/). You can also easily use the
[OpenJDK](https://openjdk.java.net/).

### Get the Code

```bash
git clone https://github.com/lipro-cpm4l/jkcemu.git
cd jkcemu
```

### Build the JAR and execute

```bash
ant jar
java -jar jkcemu.jar
```

---

This is an unofficial fork!
===========================

Original written by Jens Müller <jens@mueller-franke.de> and
distributed under the GNU General Public License version 3.

*Primary-site*: http://www.jens-mueller.org/jkcemu/

**At least, you have to read the origin author's hints at
http://www.jens-mueller.org/jkcemu/forks.html (German). Here
you will see a short and may be incomplete translation.**

I have independently extended the section "ROM and floppy disk
content" to clarify the authorship and licensing of CP/M.

## License terms and liability

*Primary-site*: http://www.jens-mueller.org/jkcemu/license.html (German)

The software JKCEMU may be used and passed on by everyone, also
for commercial purposes, in accordance with the provisions of the
GNU General Public License (GNU-GPL) Version 3. Other rights that
are not listed in the GNU-GPL are expressly not granted. If the
program is developed further or the source text is used in part
or in full in other programs, the software that has been created
from this must also be published under the GNU-GPL and must
contain a copyright notice with all, including the original,
rights holders. You can find the original wording of the GNU-GPL
in the file [**LICENSE.txt**](LICENSE.txt) or on the original
[GNU-Homepage](http://www.gnu.org/licenses/gpl-3.0.html). There
is also a
[German translation](http://www.gnu.de/documents/gpl-3.0.de.html)
for better understanding. However, this is not an official or
legally binding replacement for the original English language
wording! If you have problems understanding the English language
wording and the unofficial translation is not enough for you,
you have to make a legally secure translation yourself before
you use, pass on or modify JKCEMU.

The author provides the software in accordance with the terms of
the GNU-GPL. The use of the software is free of charge and is
therefore only at your own risk! **No warranty or liability!**

**Any guarantee and liability is excluded!**

If you intend to modify JKCEMU and publish modified versions (e.g.
by creating a fork on a public server), then please read the
["Instructions for modifying JKCEMU (German)"](http://www.jens-mueller.org/jkcemu/forks.html)!

### Program parts excluded and exempted from the GNU-GPL

ROM images of the emulated computers are required to operate
JKCEMU. You can find this in the directory [src/rom](src/rom).
Furthermore JKCEMU also contains some floppy disk content images
in the [src/disks](src/disks) directory. The ROM and floppy
disk images are not subject to the GNU-GPL, this means that:
**The rights granted by the GNU-GPL regarding the use,
modification and distribution of JKCEMU do not apply to the
contents of the ROM and floppy disk images!** Any use of these
ROM and floppy disk images outside of JKCEMU or outside of a
purely private, non-commercial environment must be clarified
with the respective original authors or their legal successors.

## Authorship

*Primary-site*: http://www.jens-mueller.org/jkcemu/license.html (German)

### Source code

**Jens Müller is the originator of the Java and C source code**
as well as the associated scripts, descriptions, help files and
pictures. This part is released and distributed under the GNU
General Public License (GNU-GPL) Version 3.

**A few parts of the source code are based on the work of other
authors:**

> - **John Elliott**, **Per Ola Ingvarsson**:
>   CRC calculation for the CopyQM file format
>   (taken over from the [LIBDSK](http://www.seasip.info/Unix/LibDsk/)
>    project and ported to Java)
> - **Spencer W. Thomas**, **Jim McKie**, **Steve Davies**,
>   **Ken Turkowski**, **James A. Woods**, **Joe Orost**,
>   **David Rowley**:
>   Authors of the source files
>   [compress.c](http://www.cs.columbia.edu/~fotis/OS/htree_pr/compress.c)
>   and [gifcompr.c](http://web.mit.edu/graphics/src/xfe-v1.0/gifcompr.c)
>   on which the implementation of the LZW encoder contained in the
>   JKCEMU is based (is required for creating animated GIF files,
>   video screen recording, and also ported to Java)

### ROM and floppy disk content

**The authorship of the ROM and floppy disk content are:**

> - **Akademie der Wissenschaften der DDR (Academy of Science of GDR),
>   Berlin** (CP/A)
> - **Amstrad plc** (KC compact, ZX Spectrum)
> > Amstrad have kindly given their permission for the
> > redistribution of their copyrighted material but retain
> > that copyright.
> - **Ingenieurhochschule für Seefahrt (College of engineering for marine
>   traffic) Warnemünde/Wustrow** (NANOS 2.2)
> - **International Research Institute for Management Sciences (IRIMS),
>   Moskau** (MicroDOS)
> - **University Rostock** (MicroDOS)
> - **VEB Datenverarbeitungszentrum Rostock** (EPOS)
> - **VEB Messelektronik Dresden** (A5105, KC85/1, KC87, Z9001)
> - **VEB Mikroelektronik Erfurt** (LC80, SC2)
> - **VEB Mikroelektronik Muehlhausen** (HC900, KC85/2...5, KC-compact,
>     MicroDOS)
> - **VEB Polytechnik Karl-Marx-Stadt** (Poly-Computer 880)
> - **VEB Robotron-Elektronik Riesa** (Z1013)
> - **Prof. Dr. Albrecht Mugler** (PC/M)
> - **Dr. Dieter Scheuschner** (SLC1)
> - **Dr. Frank Schwarzenberg** (CP/A for KC85/1, KC87 and Z9001)
> - **Dr. Gerd Maudrich** (LLC1)
> - **Dr. Hans-Jürgen Gatsche** (RBASIC programs for A5105)
> - **Dr. Rainer Brosig** (extended Z1013 monitor program and CP/M for Z1013)
> - **Andreas Suske** (FDC program and ROM bank management for AC1-2010)
> - **Bernd Hübler** (Hübler/Evert-MC, Hübler-Grafik-MC)
> - **Christian Schiewe** (80 character driver for KC85/1, KC87 and Z9001)
> - **Eckart Buschendorf** (LC-80.2 monitor program)
> - **Eckhard Ludwig** (SCCH software for AC1 and LLC2)
> - **Eckhard Schiller** (BCS3 and VCS80)
> - **Frank Heyder** (monitor program 3.1 und MiniBASIC for AC1)
> - **Frank Prüfer** (S/P-BASIC V3.3 for BCS3)
> - **Harald Saegert** (RBASIC programs for A5105)
> - **Herbert Mathes** (PC/M)
> - **Joachim Czepa** (C-80)
> - **Klaus Wilfling** (EPOS adaption)
> - **Klaus-Peter Evert** † (Hübler/Evert-MC)
> - **Manfred Kramer** (Kramer-MC)
> - **Mario Leubner** (CAOS 4.5 and EDAS for KC85/5, D004 ROM versions 3.2
>     and 3.3 and USB software)
> - **Ralf Kästner** (KCNet software)
> - **Rolf Weidlich** (LCTools and BASIC adaption for LC-80ex)
> - **Torsten Musiol** (maschine code editor for BCS3)
> - **Ulrich Zander** (driver adaption for A5105, KC81/1, KC87 and Z9001)
> - **Volker Pohlers** (demos, drivers and software adaptions for KC85/1,
>     KC87 and Z9001)

**Additionals about CP/M source and program code legalization:**

*From Wikipedia
["Source code releases"](https://en.wikipedia.org/wiki/CP/M#Source_code_releases)
(Jan 2020)*:
In 1997 and 1998 [Caldera released some CP/M 2.2 binaries and source
code](ftp://ftp.uni-bayreuth.de/pc/caldera/cpm2.2/) under an
[open source license](ftp://ftp.uni-bayreuth.de/pc/caldera/cpm2.2/README.license.txt)
**[FOR NON-COMMERCIAL USE ONLY]**, also [allowing the redistribution
and modification of further collected Digital Research files related
to the CP/M and MP/M families](http://www.retroarchive.org/cpm/archive/unofficial/lisence.html)
through Tim Olmstead's ["The Unofficial CP/M Web site"](http://www.cpm.z80.de/)
since 1997. After Olmstead's death on 12 September 2001, the [distribution
license was refreshed and expanded by Lineo](http://www.cpm.z80.de/license.html),
who had meanwhile become the owner of those Digital Research assets,
on 19 October 2001. In October 2014, to mark the 40th anniversary of
the first presentation of CP/M, the Computer History Museum
[released early source code versions of CP/M](https://computerhistory.org/blog/early-digital-research-cpm-source-code#code)
**[PROVIDED AS IS FOR NON-COMMERCIAL USE ONLY]**.

> - **Lineo, Inc.** as legal successor of
>   **Caldera, Inc.** and
>   **Digital Research, Inc.** (CP/M)
> > […] Let [this email](http://www.cpm.z80.de/license.html)
> > represent a right to use, distribute, modify, enhance and
> > otherwise make available in a nonexclusive manner the CP/M
> > technology as part of the "Unofficial CP/M Web Site" with
> > its maintainers, developers and community. I further state
> > that as Chairman and CEO of Lineo, Inc. that I have the
> > right to do offer such a license. […] Bryan Sparks […]

## Acknowledgments

*Primary-site*: http://www.jens-mueller.org/jkcemu/thanks.html (German)

The development of the emulator was only possible by support
of so many other people, especially by supply of documents and
software. Thus, some have contributed their share of JKCEMU
quite unconsciously by providing important information on
their homepage.

**Special thanks goes to:**

> - **Prof. Dr. Albrecht Mugler**:
>     for the kindly permission to integrate the PC/M system
>     software (BIOS, V-tape, Debugger, CCP and BDOS) in JKCEMU
> - **Dr. Dieter Scheuschner**:
>     for the provision of all ROM content of the SLC1 and the
>     kindly permission to may integrate this in JKCEMU
> - **Dr. Gerd Maudrich**:
>     for the kindly permission to integrate the LLC1 ROM images
>     (monitor program and BASIC interpreter) in JKCEMU
> - **Dr. Hans-Jürgen Gatsche**:
>     for the kindly permission to may integrate the RBASIC
>     programs for the A5105 in JKCEMU he developed
> - **André Schenk**:
>     for ANT script
> - **Andreas Suske**:
>     for the kindly permission to may integrate his AC1-2010
>     software (monitor program, FDC program and ROM bank
>     management) in JKCEMU and for his help with AC1 emulation
> - **Claus-Peter Fischer**:
>     for the provision of his ROM images of the PC/M
> - **Cliff Lawson (Amstrad plc)**:
>     for general permission to integrate Amstrad's copyrighted ROMs
>     into emulators
> - **Eckhard Schiller**:
>     for the kindly permission to integrate the VCS80 and
>     the BCS3 ROM images in JKCEMU
> - **Enrico Grämer**:
>     for the provision of material about the KC compact
> - **Frank Prüfer**:
>     for the kindly permission to integrate S/P-BASIC 3.3
>     in JKCEMU and the support with the BCS3 emulation
> - **Gunar Hänke**:
>     for his help with AC1 and floppy disk emulation
> - **Heiko Poppe**
>     for the kindly permission to integrate the CP/M File-Commander
>     in JKCEMU and his help with AC1, K1520 graphic and USB emulation
> - **Herbert Mathes**:
>     for the kindly permission to integrate the PC/M system
>     software (BIOS, V-tape, Debugger, CCP and BDOS) in JKCEMU
> - **Holger Bretfeld**:
>     for the loan of a KC85/5 with D004
> - **Jan Kuhnert**:
>     for intensive testing of the emulator
> - **Johann Spannenkrebs**:
>     for his homepage [www.ac1-info.de](http://www.ac1-info.de/)
>     and his help with AC1 and Poly-Computer 880 emulation
> - **John Elliott**:
>     for the kindly permission to take over parts of source code
>     from the LIBDSK project (CRC calculation for the CopyQM file
>     format)
> - **Jörg Felgentreu**:
>     for his support with the A5105 emulation
> - **Jürgen Helas**:
>     for intensive testing of the assembler and reassembler
> - **Klaus Wilfling**:
>     for the kindly permission to integrate EPOS in JKCEMU
>     he extend with color functionality and the support
>     with the NANOS emulation
> - **Klaus Junge**:
>     for the support with the NANOS emulation
> - **Manfred Kramer**:
>     for the kindly permission to integrate the Kramer-MC
>     system software in JKCEMU
> - **Mario Leubner**:
>     for the kindly permission to integrate the CAOS, EDAS,
>     D004, and USB software in JKCEMU he developed and his
>     very active and comprehensive help in the KC85/2..5,
>     USB, floppy and hard disks environment
> - **Norbert Richter**:
>     for the provision of information and software for the AC1
> - **Peter Salomon**:
>     for his homepage [www.robotron-net.de](http://www.robotron-net.de/)
> - **Ralf Däubner**:
>     for his help with the A5105 emulation
> - **Ralf Kästner**:
>     for the kindly permission to integrate the KCNet software
>     in JKCEMU he developed, for his homepage
>     [susowa.homeftp.net](http://susowa.homeftp.net/) and for
>     the help with the KC85/2..5 and network emulation
> - **Ralph Hänsel**:
>     for his comprehensive help with the AC1, floppy and hard
>     disk, and USB emulation
> - **René Nitzsche**:
>     for the loan of a KC85/5
> - **Rolf Weidlich**:
>     for the support with the AC1, LLC1, and LLC2 emulation
> - **Siegfried Schenk**:
>     for the provision of information and software for the LLC2
>     and the SCCH modules
> - **Steffen Gruhn**:
>     for his help with the A5105 and KC compact emulation
> - **Stephan Linz**:
>     for his help with the PC/M emulation, the provision of
>     floppy disk images and the PC/M system software and his
>     homepage [www.li-pro.net](http://www.li-pro.net/)
> - **Thomas Scherrer**:
>     for his Z80 web side [www.z80.info](http://www.z80.info/)
> - **Torsten Paul**:
>     for his emulator **KCemu** (http://kcemu.sourceforge.net/)
>     and for the provision of informations and ROM images
>     of various computers
> - **Ulrich Zander**:
>     for his homepage [www.sax.de](http://www.sax.de/~zander/)
>     and his support with the A5105, KC85/1, KC87,
>     and Z9001 emulation
> - **Volker Pohlers**:
>     for his homepage [hc-ddr.hucki.net](https://hc-ddr.hucki.net/)
>     and his versatile support and inspirations
> - to everyone who found and reported bugs
> - to the many active members in the
>   [Robotrontechnik-Forum](http://www.robotrontechnik.de/html/forum/index.htm)
