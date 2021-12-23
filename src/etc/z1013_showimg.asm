;
; (c) 2017 Jens Mueller
;
; Programm zum Anzeigen eines Blockgrafikbildes auf dem Z1013
;
	ORG     0100H
        LD      HL,IMGDATA
        LD      DE,0EC00H
        LD      BC,0400H
        LDIR
;auf Tastenbetaetigung warten
        RST     20H
        DB      01H               ;INCH
        NOP
;Bildschirm loeschen
        LD      A,0CH
        RST     20H
        DB      00H               ;OUTCH
        NOP
;Programmende
        JP      0038H
;ab hier unkomprimierte Bilddaten
IMGDATA:
