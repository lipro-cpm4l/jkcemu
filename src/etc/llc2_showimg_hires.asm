;
; (c) 2017 Jens Mueller
;
; Programm zum Anzeigen eines LLC2-HIRES-Bildes
;

MSTACK:	EQU     2000H

	ORG     2000H
        LD      SP,MSTACK
;Bilddaten entpacken
        LD      DE,IMGDATA
        LD      HL,8000H
UNPACK: LD      A,(DE)
        INC     DE
        CP      80H
        JR      Z,M1
        JR      C,UNPACK2
        LD      B,A
        LD      A,01H
        SUB     B
        LD      B,A
        LD      A,(DE)
        INC     DE
UNPACK1:
        LD      (HL),A
        INC     HL
        DJNZ    UNPACK1
        JR      UNPACK
UNPACK2:
        INC     A
        LD      B,A
UNPACK3:
        LD      A,(DE)
        INC     DE
        LD      (HL),A
        INC     HL
        DJNZ    UNPACK3
        JR      UNPACK
;HIRES einschalten
M1:     LD      A,50H
        OUT     (0EEH),A
;auf Tastenbetaetigung warten
WAIT_FOR_KEY:
        CALL    07FAH
        AND     7FH
        JR      Z,WAIT_FOR_KEY
;HIRES ausschalten
        XOR     A
        OUT     (0EEH),A
;Programmende
        JP      07FDH
;ab hier komprimierte Bilddaten
IMGDATA:
