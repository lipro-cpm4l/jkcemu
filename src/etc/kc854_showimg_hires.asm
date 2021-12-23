;
; (c) 2017 Jens Mueller
;
; Programm zum Anzeigen eines KC85/4-HIRES-Bildes
;
	ORG     0300H
;Menueeintrag
        DB      7FH,7FH
        DB      'SHOWIMG'
        DB      01H
;Blinken ausschalten
        IN      A,(89H)
        PUSH    AF
        AND     7FH
        OUT     (89H),A
;IRM einschalten
        IN      A,(88H)
        PUSH    AF
        OR      04H
        OUT     (88H),A
;die gerade nicht angezeigte Bildebene aktivieren
        LD      A,(IX+01H)
        PUSH    AF
        AND     0F1H
        XOR     01H
        JR      Z,M1
        OR      04H
M1:     OUT     (84H),A
        LD      (IX+01H),A
;Bilddaten fuer IRM-Pixelebene entpacken
        PUSH    AF
        LD      DE,IMGDATA
        LD      HL,8000H
        CALL    UNPACK
        POP     AF
;Bilddaten fuer IRM-Farbebene entpacken
        OR      02H
        OUT     (84H),A
        LD      (IX+01H),A
        LD      HL,8000H
        CALL    UNPACK
;auf Tastenbetaetigung warten
WAIT_FOR_KEY:
        CALL    0F003H
        DB      0EH               ;KBDZ
        JR      NC,WAIT_FOR_KEY
;urspruenglichen IRM-Zustand wieder herstellen
        POP     AF
        OUT     (84H),A
        LD      (IX+01H),A
        POP     AF
        OUT     (88H),A
;Blinken wieder herstellen
	POP     AF
	AND     80H
	LD      B,A
        IN      A,(89H)
        AND     7FH
	OR      B
        OUT     (89H),A
;Programmende
        RET
;PackBits Entpackungsroutine
UNPACK: LD      A,(DE)
        INC     DE
        CP      80H
        RET     Z
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
;ab hier komprimierte Bilddaten
IMGDATA:
