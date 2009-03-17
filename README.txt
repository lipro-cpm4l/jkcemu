JKCEMU
======


1. Allgemeines
--------------

JKCEMU ist ein in Java entwickelter Software-Emulator,
der einige in der DDR hergestllten bzw. entwickelten
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
Sollten Sie Verstaendnisprobleme mit dem englischsprachigen Wortlaut haben,
muessen Sie sich selbst um eine juristisch abgesicherte Uebersetzung bemuehen,
bevor Sie JKCEMU anwenden, weitergeben oder modifizieren.
Der Autor stellt die Software entsprechend den Bedingungen der GNU-GPL
zur Verfuegung.
Der Gebrauch der Software ist kostenfrei und erfolgt deshalb ausschliesslich
auf eigenes Risiko!

Jegliche Gewaehrleistung und Haftung ist ausgeschlossen!


3. Installation
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


4. Compilieren
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


5. Kontakt
----------

Autor:  Jens Mueller
E-Mail: info@jens-mueller.org

Ihre Mail muss im Betreff das Wort "JKCEMU" enthalten.
Anderenfalls wird sie moeglicherweise vom Spam-Filter zurueckgehalten.

