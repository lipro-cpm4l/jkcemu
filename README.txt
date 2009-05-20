JKCEMU
======


1. Allgemeines
--------------

JKCEMU ist ein in Java entwickelter Software-Emulator,
der einige in der DDR hergestllte bzw. entwickelte
Klein- und Selbstbaucomputer nachbildet.


2. Lizenzbestimmungen und Haftung
---------------------------------

Die Software JKCEMU darf von jedermann, auch fuer kommerzielle Zwecke,
entsprechend den Bedingungen der GNU General Puplic License (GNU-GPL)
Version 3 angewendet und weitergegeben werden.
Weitere Rechte, die in der GNU-GPL nicht aufgefuehrt sind,
werden ausdruecklich nicht gewaehrt.
Wird das Programm weiterentwickelt oder der Quelltext teilweise
oder vollstaendig in anderen Programmen verwendet,
so ist die daraus neu entstandene Software ebenfalls unter der GNU-GPL
zu veroeffentlichen und muss einen Copyright-Vermerk mit allen,
auch den urspruenglichen, Rechteinhabern enthalten.
Den originalen Wortlaut der GNU-GPL finden Sie in der Datei LICENSE.txt.
Zum besseren Verstaendnis gibt es auch in der Dateu src/help/gpl_de.htm
eine deutsche Uebersetzung,
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
Die ROM-Images unterliegen nicht der GNU-GPL,
d.h., die Rechte, die Ihnen von der GNU-GPL bzgl. der Benutzung,
Modifizierung und Weitergabe von JKCEMU eingeraeumt werden,
gelten nicht fuer die ROM-Inhalte!
Jegliche Benutzung dieser ROM-Images ausserhalb von JKCEMU
oder ausserhalb eines rein privaten, nicht kommerziellen Umfeldes
muessen Sie im Zweifelsfall mit den Urhebern bzw. deren Rechtsnachfolgern
klaeren.

Die Urheberschaften der ROM-Inhalte liegen bei:
- VEB Messelektronik Dresden (Z9001, KC85/1, KC87)
- VEB Mikroelektronik Erfurt (LC80, SC2)
- VEB Mikroelektronik Muehlhausen (HC900, KC85/2, KC85/3, KC85/4)
- VEB Polytechnik Karl-Marx-Stadt (Poly-Computer 880)
- VEB Robotron-Elektronik Riesa (Z1013)
- Prof. Dr. Albrecht Mugler (PC/M)
- Dr. Gerd Maudrich (LLC1)
- Dr. Rainer Brosig (erweitertes Z1013-Monitorprogramm)
- Bernd Huebler (Huebler/Evert-MC, Huebler-Grafik-MC)
- Eckart Buschendorf (LC-80.2-Monitorprogramm)
- Eckhard Ludwig (SCCH-Software fuer AC1 und LLC2)
- Eckhard Schiller (BCS3 und VCS80)
- Frank Heyder (Monitorprogramm 3.1 und MiniBASIC fuer AC1)
- Frank Pruefer (S/P-BASIC V3.3 fuer BCS3)
- H. Mathes (PC/M)
- Joachim Czepa (C-80)
- Klaus-Peter Evert (Huebler/Evert-MC)
- Manfred Kramer (Kramer-MC)
- Torsten Musiol (Maschinenkode-Editor fuer BCS3)


4. Installation
---------------

1. Installieren Sie die Java Runtime Environment (JRE)
   der Java Standard Edition (Java SE) Version 6 oder hoeher.
   Ueberpruefen Sie die installierte Java-Version durch den
   Kommandozeilenaufruf: "java -version"
   Fuer Java 6 muss die Versionsnummer 1.6.x erscheinen.
2. Laden Sie die Datei jkcemu.jar von http://www.jens-mueller.org/jkcemu
   herunter. Die Datei darf dabei nicht als ZIP-Datei entpackt werden!
3. Kopieren Sie die Datei jkcemu.jar in ein beliebiges Verzeichnis
   auf die Festplatte ihres Computers,
   z.B. unter Linux/Unix nach "/tmp" oder unter Windows nach "C:\".
4. Starten Sie JKCEMU
   - unter Linux/Unix mit: "java -jar /tmp/jkcemu.jar"
   - unter Windows in der DOS-Box mit: "javaw.exe -jar C:\jkcemu.jar"
5. Legen Sie auf dem Desktop eine "Verknuepfung zu einem Programm" an,
   und tragen dort die in der Console/DOS-Box erfolgreich verwendete
   Kommandozeile ein.


5. Compilieren
--------------

Moechten Sie den Quelltext compilieren, sind folgende Schritte notwendig:
1. Stellen Sie sicher, dass das Java Development Kit (JDK)
   der Java Standard Edition (Java SE) Version 6 oder hoeher installiert ist.
2. Wechseln Sie in das cmd-Verzeichnis des JKCEMU-Quelltextes
3. Compilieren Sie
   - unter Linux/Unix mit: "./compile"
   - unter Windows in der DOS-Box mit: "compile.bat"

Alternativ koennen Sie auch mit "ant" compilieren.
Die dazu notwendige Datei build.xml finden Sie im Wurzelverzeichnis
des Quelltextes.


6. Dank
-------
Die Entwicklung des Emulators war nur moeglich durch die Zuarbeit anderer,
vorallem durch Bereitstellung von Dokumenten und Software.
Dabei hat so mancher ganz unbewusst seinen Anteil an JKCEMU beigetragen,
indem er auf seiner Homepage wichtige Informationen bereithaelt.
Besonders bedanken moechte ich mich bei:

- Prof. Dr. Albrecht Mugler fuer die freundliche Genehmigung zur Integration
  der PC/M-Systemsoftware (BIOS, V-Tape, Debugger, CCP und BDOS) in JKCEMU
- Dr. Gerd Maudrich fuer die freundliche Genehmigung zur Integration
  des LLC1-ROM-Images (Monitorprogramm und Tiny-BASIC-Interpreter) in JKCEMU
- Andre Schenk fuer das ANT-Skript
- Andreas Suske fuer Informationen und Software zum AC1
- Claus-Peter Fischer fuer die Bereitstellung eines ROM-Images zum PC/M
- Frank Pruefer fuer die freundliche Genehmigung zur Integration
  von S/P-BASIC 3.3 in JKCEMU sowie fuer das Testen der BCS3-Emulation
- H. Mathes fuer die freundliche Genehmigung zur Integration
  der PC/M-Systemsoftware (BIOS, V-Tape, Debugger, CCP und BDOS) in JKCEMU
- Frank Pruefer fuer die freundliche Genehmigung zur Integration
- Johann Spannenkrebs fuer seine Homepage http://www.ac1-info.de,
  der Bereitstellung von Informationen und Software fuer den AC1
  und den Poly-Computer 880 sowie das Testen der AC1- und Poly880-Emulation
- Manfred Kramer fuer die freundliche Genehmigung zur Integration
  der Systemsoftware des Kramer-MC in JKCEMU
- Peter Salomon fuer seine Homepage http://www.robotron-net.de
- Ralf Kaestner fuer die Bereitstellung von Informationen und Software
  zum KC85/4
- Rolf Weidlich fuer die Bereitstellung von Informationen und Software
  zum AC1 und LLC1
- Siegfried Schenk fuer die Bereitstellung von Informationen
  und Software zum LLC2 und zu den SCCH-Modulen
- Stephan Linz fuer seine Homepage zum PC/M http://www.li-pro.net
- Thomas Scherrer fuer seine Z80-Seite http://www.z80.info"
- Torsten Paul fuer seinen Emulator KCemu (http://kcemu.sourceforge.net)
  und der Bereitstellung von Informationen und ROM-Images diverser Computer
- Ulrich Zander fuer seine Homepage http://www.sax.de/~zander
  und der Beantwortung technischer Fragen zum Z9001
- Volker Pohlers fuer seine Homepage http://www.homecomputer-ddr.de.vu"
  sowie seiner aktiven Unterstuetzung durch Bereitstellung von
  Dokumenten und Software sowie durch Testen des Emulators
- den aktiven Mitgliedern im Robotrontechnikforum


7. Kontakt
----------

Autor:  Jens Mueller
E-Mail: info@jens-mueller.org

Ihre Mail muss im Betreff das Wort "JKCEMU" enthalten.
Anderenfalls wird sie moeglicherweise vom Spam-Filter zurueckgehalten.

