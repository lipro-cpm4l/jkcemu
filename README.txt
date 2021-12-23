JKCEMU
======


1. Allgemeines
--------------

JKCEMU ist ein hauptsaechlich in Java entwickelter Software-Emulator,
der die meisten der in der DDR hergestellten Heim- und Kleincomputer
sowie als Bauanleitung veroeffentlichten Selbstbaucomputer nachbildet.


2. Lizenzbestimmungen und Haftung
---------------------------------

Die Software JKCEMU darf von jedermann entsprechend den Bedingungen
der GNU General Puplic License (GNU-GPL) Version 3
angewendet und weitergegeben werden.
Weitere Rechte, die in der GNU-GPL nicht aufgefuehrt sind,
werden ausdruecklich nicht gewaehrt.
Wird das Programm weiterentwickelt oder der Quelltext teilweise
oder vollstaendig in anderen Programmen verwendet,
so ist die daraus neu entstandene Software ebenfalls unter der GNU-GPL
zu veroeffentlichen und muss einen Copyright-Vermerk mit allen,
auch den urspruenglichen, Rechteinhabern enthalten.
Den originalen Wortlaut der GNU-GPL finden Sie in der Datei LICENSE.txt.
Zum besseren Verstaendnis gibt es auch in der Dateu src/help/gpl_de.htm
eine deutsche Uebersetzung.
Sollten Sie Verstaendnisprobleme mit dem englischsprachigen Wortlaut haben
und Ihnen die inoffizielle Uebersetzung nicht genuegen,
muessen Sie sich selbst um eine juristisch abgesicherte Uebersetzung bemuehen,
bevor Sie JKCEMU anwenden, weitergeben oder modifizieren.
Der Autor stellt die Software entsprechend den Bedingungen der GNU-GPL
zur Verfuegung.
Der Gebrauch der Software ist kostenfrei und erfolgt deshalb ausschliesslich
auf eigenes Risiko!

Jegliche Gewaehrleistung und Haftung ist ausgeschlossen!


3. Von der GNU-GPL ausgenommene Programmteile
---------------------------------------------

Fuer den Betrieb von JKCEMU sind ROM-Images der jeweils emulierten
Computer notwendig.
Diese finden Sie im Verzeichnis src/rom.
Des weiteren enthaelt JKCEMU im Verzeichnis src/disks
auch einige Disketteninhalte.
Die ROM- und Disketten-Images unterliegen nicht der GNU-GPL,
d.h., die Rechte, die Ihnen von der GNU-GPL bzgl. der Benutzung,
Modifizierung und Weitergabe von JKCEMU eingeraeumt werden,
gelten nicht fuer die ROM- und Disketteninhalte!
Jegliche Benutzung dieser ROM- und Disketten-Images ausserhalb
von JKCEMU muessen Sie im Zweifelsfall mit den Urhebern
bzw. deren Rechtsnachfolgern klaeren.


4. Urheberschaften
------------------


4.1. Urheberschaft am Programmcode

Die Urheberschaft am Java- und C-Programmcode sowie den dazu
gehoerenden Skripten, Beschreibungen, Hilfe-Dateien und Bildern
liegt bei Jens Mueller.
Dieser Teil ist unter der GNU General Public License Version 3
(GNU-GPL) freigegeben.

Ein Programmcodeteil basiert auf der Arbeit anderer Autoren:

John Elliott, Per Ola Ingvarsson:
  CRC-Berechnung fuer das CopyQM-Dateiformat
  (aus dem Projekt LIBDSK uebernommen und nach Java portiert)


4.2.  Urheberschaften an den ROM- und Disketteninhalten

- Akademie der Wissenschaften der DDR, Berlin (CP/A)
- Amstrad plc (KC compact, ZX Spectrum)
  Amstrad has kindly given their permission for the redistribution
  of their copyrighted material but retain that copyright.
- Ingenieurhochschule fuer Seefahrt Warnemuende/Wustrow (NANOS 2.2)
- International Research Institute for Management Sciences (IRIMS), Moskau
  (MicroDOS)
- Universitaet Rostock (MicroDOS)
- VEB Messelektronik Dresden (A5105, KC85/1, KC87, Z9001)
- VEB Mikroelektronik Erfurt (LC80, SC2)
- VEB Mikroelektronik Muehlhausen (HC900, KC85/2...5, KC compact, MicroDOS)
- VEB Polytechnik Karl-Marx-Stadt (Poly-Computer 880)
- VEB Robotron-Elektronik Riesa (Z1013)
- Prof. Dr. Albrecht Mugler (PC/M)
- Dr. Dieter Scheuschner (SLC1)
- Dr. Frank Schwarzenberg (CP/A fuer KC85/1, KC87 und Z9001)
- Dr. Gerd Maudrich (LLC1)
- Dr. Hans-Juergen Gatsche (RBASIC-Programme fuer A5105)
- Dr. Rainer Brosig (erweitertes Z1013-Monitorprogramm sowie CP/M fuer Z1013)
- Andreas Suske (Monitorprogramm, FDC-Programm und ROM-Bank-Verwaltung
  fuer AC1-2010)
- Bernd Huebler (Huebler/Evert-MC, Huebler-Grafik-MC)
- Christian Schiewe (80-Zeichen-Treiber fuer KC85/1, KC87 und Z9001)
- Eckart Buschendorf (LC-80.2-Monitorprogramm)
- Eckhard Ludwig (SCCH-Software fuer AC1 und LLC2)
- Eckhard Schiller (BCS3 und VCS80)
- Frank Heyder (Monitorprogramm 3.1 und MiniBASIC fuer AC1)
- Frank Ludwig (Joystick-Editor und CAOS-Taschenrechner fuer KC85/5)
- Frank Pruefer (S/P-BASIC V3.3 fuer BCS3)
- Harald Saegert (RBASIC-Programme fuer A5105)
- Herbert Mathes (PC/M)
- Jens Mueller (Diskette mit Bildern fuer A5105)
- Joachim Czepa (C-80)
- Klaus-Peter Evert (Huebler/Evert-MC)
- Manfred Kramer (Kramer-MC)
- Mario Leubner (CAOS 4.8 und 80-Zeichen-Editor fuer KC85/5,
  D004-ROM-Version 3.31, D008-ROM-Version 3.41, USB-Software sowie
  Anpassungen an KC-BASIC, Assembler, Reassembler
  und Testmonitor/Debugger fuer KC85/5)
- Ralf Kaestner (KCNet-Software)
- Rolf Weidlich (LCTools und BASIC-Adaption fuer LC-80ex)
- Torsten Musiol (Maschinenkode-Editor fuer BCS3)
- Ulrich Zander (Treiberanpassungen fuer A5105, KC81/1, KC87 und Z9001)
- Volker Pohlers (Demos, Treiber und Softwareanpassungen fuer
  KC85/1, KC87 und Z9001)


5. Installation
---------------

1. Installieren Sie die Java Runtime Environment (JRE)
   der Java Standard Edition (Java SE) Version 7 oder hoeher.
   Alternativ koennen Sie auch das Java Development Kit (JDK) installieren.
   Ueberpruefen Sie die installierte Java-Version durch den
   Kommandozeilenaufruf: "java -version"
   Fuer Java 7 muss die Versionsnummer 1.7.x erscheinen.
2. Laden Sie die Datei jkcemu-<Version>.jar
   von http://www.jens-mueller.org/jkcemu herunter.
   "<Version>" ist dabei durch die konkrete Versionsnummer zu ersetzen.
   Die Datei darf nicht als ZIP-Datei entpackt werden!
3. Kopieren Sie die Datei jkcemu-<Version>.jar in ein beliebiges Verzeichnis
   auf die Festplatte ihres Computers,
   z.B. unter Linux/Unix nach "/tmp" oder unter Windows nach "C:\Temp\".
4. Starten Sie JKCEMU
   - unter Linux/Unix mit: "java -jar /tmp/jkcemu-<Version>.jar"
   - unter Windows in der Eingabeaufforderung mit:
     "javaw.exe -jar C:\Temp\jkcemu-<Version>.jar"
5. Legen Sie auf dem Desktop eine "Verknuepfung zu einem Programm" an,
   und tragen dort die in der Console bzw. Eingabeaufforderung
   erfolgreich verwendete Kommandozeile ein.
6. Auf manchen Systemen funktioniert der Start von JKCEMU auch,
   indem Sie in einem Datei Manager (z.B. Windows Explorer) einfach
   auf die JAR-Datei (doppelt) klicken.


6. Compilieren
--------------

Moechten Sie den Quelltext compilieren, sind folgende Schritte notwendig:
1. Stellen Sie sicher, dass das Java Development Kit (JDK)
   der Java Standard Edition (Java SE) Version 7 oder hoeher installiert ist.
2. Wechseln Sie in das cmd-Verzeichnis des JKCEMU-Quelltextes
3. Compilieren Sie
   - unter Linux/Unix mit: "./compile"
   - unter Windows in der Eingabeaufforderung mit: "compile.cmd"

Alternativ koennen Sie auch mit "ant" compilieren.
Die dazu notwendige Datei build.xml finden Sie im Wurzelverzeichnis
des Quelltextes.


7. Dank
-------

Die Entwicklung des Emulators war nur moeglich durch die Zuarbeit anderer,
vorallem durch Bereitstellung von Dokumenten und Software.
Dabei hat so mancher ganz unbewusst seinen Anteil an JKCEMU beigetragen,
indem er auf seiner Homepage wichtige Informationen bereithaelt.
Besonders bedanken moechte ich mich bei:

- Prof. Dr. Albrecht Mugler fuer die freundliche Genehmigung zur Integration
  der PC/M-Systemsoftware (BIOS, V-Tape, Debugger, CCP und BDOS) in JKCEMU
- Dr. Dieter Scheuschner fuer die Bereitstellung des ROM-Inhalts des SLC1
  und fuer die freundliche Genehmigung,
  diesen in JKCEMU integrieren zu duerfen
- Dr. Gerd Maudrich fuer die freundliche Genehmigung zur Integration
  des LLC1-ROM-Images (Monitorprogramm und Tiny-BASIC-Interpreter) in JKCEMU
- Dr. Hans-Juergen Gatsche fuer die freundliche Genehmigung,
  von ihm entwickelte RBASIC-Programme fuer den A5105
  in JKCEMU integrieren zu duerfen
- Andre Schenk fuer das ANT-Skript
- Andreas Suske fuer die freundliche Genehmigung
  zur Integration seiner AC1-2010-Software
  (Monitorprogramm, FDC-Programm und ROM-Bank-Verwaltung)
  in JKCEMU sowie fuer seine Hilfe bei der AC1-Emulation
- Claus-Peter Fischer fuer die Bereitstellung eines ROM-Images zum PC/M
- Cliff Lawson (Amstrad plc) fuer die allgemeine Erlaubnis zur Integration
  der unter dem Urheberrecht von Amstrad stehenden ROMs in Emulatoren.
- Eckhard Schiller fuer die freundliche Genehmigung zur Integration
  des VCS80- und der BCS3-ROM-Images in JKCEMU
- Enrico Graemer fuer die Bereitstellung von Material zum KC compact
- Frank Ludwig fuer die freundliche Genehmigung zur Integration
  der von ihm entwickelten bzw. mitentwickelten KC85/5-Software in JKCEMU
- Frank Pruefer fuer die freundliche Genehmigung zur Integration
  von S/P-BASIC 3.3 in JKCEMU sowie fuer die Unterstuetzung
  bei der BCS3-Emulation
- Gunar Haenke fuer seine Hilfe bei der AC1- und Diskettenemulation
- Heiko Poppe fuer die freundliche Genehmigung zur Integration
  des CP/M File-Commanders in JKCEMU sowie fuer seine Hilfe
  bei der AC1-, K1520-Farbgrafikkarten- und USB-Emulation
- Herbert Mathes fuer die freundliche Genehmigung zur Integration
  der PC/M-Systemsoftware (BIOS, V-Tape, Debugger, CCP und BDOS) in JKCEMU
- Holger Bretfeld fuer die leihweise Bereitstellung eines KC85/5 mit D004
- Jan Kuhnert fuer das intensive Testen des Emulators
- Johann Spannenkrebs fuer seine Homepage http://www.ac1-info.de
  sowie fuer seine Hilfe bei der AC1- und Poly880-Emulation
- John Elliott fuer die freundliche Genehmigung,
  Programmcodeteile aus dem Projekt LIBDSK uebernehmen zu duerfen
  (CRC-Berechnung fuer das CopyQM-Dateiformat)
- Joerg Felgentreu fuer seine Unterstuetzung bei der A5105-Emulation
- Juergen Helas fuer das intensive Testen des Assemblers und Reassemblers
- Klaus Junge fuer die Unterstuetzung bei der NANOS-Emulation
- Klaus Wilfling fuer die freundliche Genehmigung zur Integration
  des von Ihm um Farbfunktionalitaet erweiterten EPOS
  sowie fuer die Unterstuetzung bei der K1520-Sound- und NANOS-Emulation
- Manfred Kramer fuer die freundliche Genehmigung zur Integration
  der Systemsoftware des Kramer-MC in JKCEMU
- Mario Leubner fuer die freundliche Genehmigung zur Integration
  der von ihm entwickelten und weiterentwickelten CAOS-, EDAS-,
  D004-, D008- und USB-Software in JKCEMU
  sowie fuer seine seine sehr aktive und umfangreiche Hilfe
  im KC85/2..5-, USB-, Disketten- und Festplattenumfeld
- Matthias Haustein fuer die Hilfe bei der Emulation
  von Z1013-128-Funktionalitaeren
- Norbert Richter fuer die Bereitstellung von Informationen
  und Software zum AC1
- Peter Salomon fuer seine Homepage http://www.robotron-net.de
- Ralf Daeubner fuer seine Hilfe bei der A5105-Emulation
- Ralf Kaestner fuer die freundliche Genehmigung zur Integration
  der von ihm entwickelten KCNet-Software,
  fuer seine Homepage http://kc85.info
  sowie fuer die Hilfe bei der KC85/2..5- und Netzwerk-Emulation
- Ralph Haensel fuer seine umfangreiche Hilfe
  bei der AC1-, Disketten-, Festplatten- und USB-Emulation
- Rene Nitzsche fuer die leihweise Bereitstellung eines KC85/5
- Rolf Weidlich fuer die freundliche Genehmigung zur Integration
  seiner LC-80ex-Software (LCTools und BASIC-Adaption)
  sowie fuer die intensive Unterstuetzung bei der AC1-, LC-80-,
  LLC1- und LLC2-Emulation und der Emulation des KC85/2..5-Moduls M066
- Siegfried Schenk fuer die Bereitstellung von Informationen
  und Software zum LLC2 und zu den SCCH-Modulen
- Steffen Gruhn fuer seine Hilfe bei der A5105- und KC-compact-Emulation
- Stephan Linz fuer seine Hilfe bei der PC/M-Emulation,
  der Bereitstellung von Disketten-Images und PC/M-Systemprogrammen
  sowie fuer seine Homepage http://www.li-pro.net
- Thomas Scherrer fuer seine Z80-Seite http://www.z80.info
- Torsten Paul fuer seinen Emulator KCemu (http://kcemu.sourceforge.net)
  und fuer die Bereitstellung von Informationen und ROM-Images
  zu diversen Computern
- Ulrich Zander fuer seine Homepage http://www.sax.de/~zander
  und fuer seine Unterstuetzung bei der BIC A5105-, KC85/1-, KC87-
  und Z9001-Emulation
- Volker Pohlers fuer seine Homepage https://hc-ddr.hucki.net
  sowie fuer seine vielseitige Unterstuetzung
- all denen, die Fehler gefunden und gemeldet haben
- den aktiven Mitgliedern im Robotrontechnikforum


8. Kontakt
----------

Autor:  Jens Mueller
E-Mail: info@jens-mueller.org

Ihre Mail muss im Betreff das Wort "JKCEMU" enthalten.
Anderenfalls wird sie moeglicherweise vom Spam-Filter zurueckgehalten.
