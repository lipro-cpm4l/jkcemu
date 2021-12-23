;
; (c) 2017 Jens Mueller
;
; Programm zum Anzeigen eines KC85/2-schwarz/weiss-Bildes
;
	ORG     0300H
;Menueeintrag
        DB      7FH,7FH
        DB      'SHOWIMG'
        DB      01H
;Fenstereinstellungen retten
	LD      HL,(0B79CH)
	PUSH    HL
	LD      HL,(0B79EH)
	PUSH    HL
;Fenster auf maximale Groesse einstellen
	LD      HL,0000H
	LD      (0B79CH),HL
	LD      (0B7A0H),HL
	LD      HL,2028H
	LD      (0B79EH),HL
;IRM einschalten
        IN      A,(88H)
        PUSH    AF
        OR      04H
        OUT     (88H),A
;Bilddaten entpacken (nur IRM-Pixelebene)
        LD      DE,IMGDATA
        LD      HL,8000H
        CALL    UNPACK
;IRM-Farbebene mit schwarz/weiss beschreiben
        LD      HL,0A800H
        LD      A,07H
        LD      C,0AH
M1:     LD      B,00H
M2:     LD      (HL),A
        INC     HL
        DJNZ    M2
        DEC     C
        JR      NZ,M1
;auf Tastenbetaetigung warten
WAIT_FOR_KEY:
        CALL    0F003H
        DB      0EH               ;KBDZ
        JR      NC,WAIT_FOR_KEY
;urspruenglichen IRM-Zustand wieder herstellen
        POP     AF
        OUT     (88H),A
;Bildschirm loeschen
        LD      A,0CH
        CALL    0F003H
        DB      00H               ;CRT
;urspruengliche Fenstereinstellungen wieder herstellen
	POP     HL
	LD      (0B79EH),HL
	POP     HL
	LD      (0B79CH),HL
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
