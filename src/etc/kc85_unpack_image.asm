;
; (c) Ralf Kaestner
;
; Entpackungsroutine eines komprimierten KC85/2..5-Bildes
;
	ORG     4000H
        LD      HL,IMGDATA
        LD      DE,8000H
M1:     LD      A,0A8H
        CP      D
        RET     Z
        LD      A,(HL)
        INC     HL
        CP      1BH
        JR      Z,M2
        LD      (DE),A
        INC     DE
        JR      M1
M2:     LD      A,(HL)
        INC     HL
        LD      B,(HL)
        INC     HL
M3:     LD      (DE),A
        INC     DE
        DJNZ    M3
        JR      M1
;ab hier komprimierte Bilddaten
IMGDATA:
