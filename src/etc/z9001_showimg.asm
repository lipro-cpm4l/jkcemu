;
; (c) 2017 Jens Mueller
;
; Programm zum Anzeigen eines Blockgrafikbildes auf dem Z9001
;
	ORG     0300H
        JP      M1
;Menueeintrag
        DB      'SHOWIMG '	;8 Zeichen!
        DB      00H		;Ende Kommando
	DB      00H		;Ende OS-Rahmen
;Cursor auschalten
M1:     LD      C,1DH
        CALL    0005H
;Farbspeicher mit schwarz/weiss fuellen
        LD      HL,0E800H
        LD      (HL),70H
        LD      DE,0E801H
        LD      BC,04BFH
        LDIR
;Bilddaten kopieren
        LD      HL,IMGDATA
        LD      DE,0EC00H
        LD      BC,03C0H
        LDIR
;auf Tastenbetaetigung warten
        LD      C,01H
        CALL    0005H
;Cursor einchalten
        LD      C,1EH
        CALL    0005H
;Bildschirm loeschen
        LD      E,0CH
        LD      C,02H
        CALL    0005H
;Programmende
        JP      0000H
;ab hier unkomprimierte Bilddaten
IMGDATA:
