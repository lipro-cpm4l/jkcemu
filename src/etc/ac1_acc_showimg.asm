;
; (c) 2017 Jens Mueller
;
; Programm zum Anzeigen eines Blockgrafikbildes auf dem AC1-ACC
;

MSTACK:	EQU     2000H

	ORG     2000H
        LD      SP,MSTACK
        LD      HL,IMGDATA
        LD      DE,1000H
        LD      BC,0800H
        LDIR
;auf Tastenbetaetigung warten
WAIT_FOR_KEY:
        CALL    07FAH
        AND     7FH
        JR      Z,WAIT_FOR_KEY
        CALL    0008H             ;INCH
;Bildschirm loeschen
        LD      A,0CH
        CALL    0010H             ;OUTCH
;Programmende
        JP      07FDH
;ab hier unkomprimierte Bilddaten
IMGDATA:
