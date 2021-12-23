/*
 * (c) 2018-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * BASIC-Bibliothek fuer Grafikfunktionen
 */

package jkcemu.programming.basic;

import java.util.Set;


public class GraphicsLibrary
{
  public static void appendCodeTo( BasicCompiler compiler )
  {
    AsmCodeBuf     buf    = compiler.getCodeBuf();
    AbstractTarget target = compiler.getTarget();

    if( compiler.usesLibItem( BasicLibrary.LibItem.CIRCLE ) ) {
      /*
       * Zeichnen eines Kreises
       *
       * Parameter:
       *   CIRCLE_M_X:   X-Koordinate Kreismittelpunkt (Xm)
       *   CIRCLE_M_Y:   Y-Koordinate Kreismittelpunkt (Ym)
       *   CIRCLE_M_R:   Radius
       * Hilfszellen:
       *   CIRCLE_M_ER:  Fehlerglied
       *   CIRCLE_M_RX:  relative X-Koordinate (Xr)
       *   CIRCLE_M_RY:  relative Y-Koordinate (Yr)
       *   CIRCLE_M_XMX: Koordinate Xm - Xr
       *   CIRCLE_M_XPX: Koordinate Xm + Xr
       *   CIRCLE_M_XMY: Koordinate Xm - Yr
       *   CIRCLE_M_XPY: Koordinate Xm + Yr
       *   CIRCLE_M_YMX: Koordinate Ym - Xr
       *   CIRCLE_M_YPX: Koordinate Ym + Xr
       *   CIRCLE_M_YMY: Koordinate Ym - Yr
       *   CIRCLE_M_YPY: Koordinate Ym + Yr
       */
      buf.append( "CIRCLE:\tLD\tDE,(CIRCLE_M_R)\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJP\tM,E_INVALID_PARAM\n"	// bei R<0
		+ "\tOR\tE\n"
		+ "\tRET\tZ\n"			// bei R=0
		+ "\tLD\tHL,0001\n"		// Fehlerglied: 1-R
		+ "\tSBC\tHL,DE\n"		// CY ist 0
		+ "\tLD\t(CIRCLE_M_ER),HL\n"
		+ "\tLD\tHL,0000H\n"		// (Xr,Yr) = (0,R)
		+ "\tLD\t(CIRCLE_M_RX),HL\n"
		+ "\tLD\t(CIRCLE_M_RY),DE\n"
		+ "\tLD\tHL,(CIRCLE_M_X)\n"
		+ "\tLD\t(CIRCLE_M_XMX),HL\n"
		+ "\tLD\t(CIRCLE_M_XPX),HL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\t(CIRCLE_M_XMY),HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(CIRCLE_M_XPY),HL\n"
		+ "\tLD\tHL,(CIRCLE_M_Y)\n"
		+ "\tLD\t(CIRCLE_M_YMX),HL\n"
		+ "\tLD\t(CIRCLE_M_YPX),HL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\t(CIRCLE_M_YMY),HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(CIRCLE_M_YPY),HL\n"
		+ "CIRCLE1:\n"
		+ "\tLD\tHL,(CIRCLE_M_RY)\n"	// Ende erreicht?
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tDE,(CIRCLE_M_RX)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tRET\tC\n"
		+ "\tPUSH\tAF\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tCIRCLE3\n"		// Punkte setzen
		+ "\tPOP\tDE\n"
		+ "\tPOP\tAF\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tDE\n"			// Xr = Xr + 1
		+ "\tLD\t(CIRCLE_M_RX),DE\n"
		+ "\tLD\tHL,(CIRCLE_M_XMX)\n"	// Xm-Xr anpassen
		+ "\tDEC\tHL\n"
		+ "\tLD\t(CIRCLE_M_XMX),HL\n"
		+ "\tLD\tHL,(CIRCLE_M_XPX)\n"	// Xm+Xr anpassen
		+ "\tINC\tHL\n"
		+ "\tLD\t(CIRCLE_M_XPX),HL\n"
		+ "\tLD\tHL,(CIRCLE_M_YMX)\n"	// Xm-Xr anpassen
		+ "\tDEC\tHL\n"
		+ "\tLD\t(CIRCLE_M_YMX),HL\n"
		+ "\tLD\tHL,(CIRCLE_M_YPX)\n"	// Xm+Xr anpassen
		+ "\tINC\tHL\n"
		+ "\tLD\t(CIRCLE_M_YPX),HL\n"
		+ "\tLD\tHL,(CIRCLE_M_ER)\n"	// F >= 0 -> CIRCLE2
		+ "\tBIT\t7,H\n"
		+ "\tJR\tZ,CIRCLE2\n"
		+ "\tSLA\tE\n"			// F = F + 2*X - 1
		+ "\tRL\tD\n"
		+ "\tADD\tHL,DE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\t(CIRCLE_M_ER),HL\n"
		+ "\tJR\tCIRCLE1\n"
		+ "CIRCLE2:\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tHL,(CIRCLE_M_RY)\n"	// F = F + 2*(X-Y)
		+ "\tEX\tDE,HL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\t(CIRCLE_M_ER),HL\n"
		+ "\tLD\tHL,(CIRCLE_M_RY)\n"	// Yr = Yr - 1
		+ "\tDEC\tHL\n"
		+ "\tLD\t(CIRCLE_M_RY),HL\n"
		+ "\tLD\tHL,(CIRCLE_M_XMY)\n"	// Xm-Xr anpassen
		+ "\tINC\tHL\n"
		+ "\tLD\t(CIRCLE_M_XMY),HL\n"
		+ "\tLD\tHL,(CIRCLE_M_XPY)\n"	// Xm+Xr anpassen
		+ "\tDEC\tHL\n"
		+ "\tLD\t(CIRCLE_M_XPY),HL\n"
		+ "\tLD\tHL,(CIRCLE_M_YMY)\n"	// Xm-Xr anpassen
		+ "\tINC\tHL\n"
		+ "\tLD\t(CIRCLE_M_YMY),HL\n"
		+ "\tLD\tHL,(CIRCLE_M_YPY)\n"	// Xm+Xr anpassen
		+ "\tDEC\tHL\n"
		+ "\tLD\t(CIRCLE_M_YPY),HL\n"
		+ "\tJP\tCIRCLE1\n"
      /*
       * Setzen eines Punktes in allen 8 Oktanden
       *
       * DE:  relative X-Koordinate
       * BC:  relative Y-Koordinate
       * Z=1: Xr == Yr
       */
		+ "CIRCLE3:\n"
		+ "\tJR\tZ,CIRCLE5\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tEXX\n"
		+ "\tJR\tZ,CIRCLE4\n"
		+ "\tLD\tDE,(CIRCLE_M_XMX)\n"
		+ "\tCALL\tCIRCLE9\n"
		+ "CIRCLE4:\n"
		+ "\tLD\tDE,(CIRCLE_M_XPX)\n"
		+ "\tCALL\tCIRCLE9\n"
		+ "\tEXX\n"
		+ "CIRCLE5:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tEXX\n"
		+ "\tJR\tZ,CIRCLE6\n"
		+ "\tLD\tDE,(CIRCLE_M_XMY)\n"
		+ "\tCALL\tCIRCLE7\n"
		+ "CIRCLE6:\n"
		+ "\tLD\tDE,(CIRCLE_M_XPY)\n"
		+ "CIRCLE7:\n"
		+ "\tEXX\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tEXX\n"
		+ "\tJR\tZ,CIRCLE8\n"
		+ "\tLD\tHL,(CIRCLE_M_YMX)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tDE\n"
		+ "CIRCLE8:\n"
		+ "\tLD\tHL,(CIRCLE_M_YPX)\n"
		+ "\tJP\tXPSET\n"
		+ "CIRCLE9:\n"
		+ "\tEXX\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tEXX\n"
		+ "\tJR\tZ,CIRCLE10\n"
		+ "\tLD\tHL,(CIRCLE_M_YMY)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tDE\n"
		+ "CIRCLE10:\n"
		+ "\tLD\tHL,(CIRCLE_M_YPY)\n"
		+ "\tJP\tXPSET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
      compiler.addLibItem( BasicLibrary.LibItem.XPSET );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRLBLT ) ) {
      buf.append( "DRLBLT:\tEX\t(SP),HL\n"
		+ "\tCALL\tDRLBL\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.DRLBL );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRLBL ) ) {
      buf.append( "DRLBL:\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tZ,DRLBL5\n"
		+ "\tCP\t7FH\n"	
		+ "\tJR\tNC,DRLBL\n"
		+ "\tSUB\t21H\n"
		+ "\tJR\tC,DRLBL\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tB,00H\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tH,B\n"
		+ "\tLD\tL,C\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tADD\tHL,HL\n"
		+ "\tLD\tBC,FONT_5X7\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tDE,(M_XPOS)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tC,A\n"
		+ "DRLBL1:\tLD\tA,(HL)\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_YPOS)\n"
		+ "\tLD\tB,08H\n"
		+ "DRLBL2:\tSRL\tA\n"
		+ "\tJR\tNC,DRLBL3\n"
		+ "\tPUSH\tAF\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tPOP\tAF\n"
		+ "DRLBL3:\tINC\tHL\n"
		+ "\tDJNZ\tDRLBL2\n"
		+ "\tINC\tDE\n"
		+ "\tPOP\tHL\n"
		+ "\tINC\tHL\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,DRLBL1\n"
		+ "\tPOP\tHL\n"
		+ "DRLBL4:\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(M_XPOS),DE\n"
		+ "\tJR\tDRLBL\n"
		+ "DRLBL5:\tLD\tDE,(M_XPOS)\n"
		+ "\tINC\tDE\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tDRLBL4\n" );
      compiler.addLibItem( BasicLibrary.LibItem.FONT_5X7 );
      compiler.addLibItem( BasicLibrary.LibItem.M_XYPOS );
      compiler.addLibItem( BasicLibrary.LibItem.XPSET );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRAWST ) ) {
      /*
       * DRAW-Macro ausfuehren,
       * Der Macro-String folgt direkt hinter dem Aufruf.
       *
       * Parameter:
       *   DE: relative X-Koordinate
       *   HL: relative Y-Koordinate
       */
      buf.append( "DRAWST:\tEX\t(SP),HL\n"
		+ "\tCALL\tDRAWS\n"
		+ "\tEX\tDE,HL\n"
		+ "\tEX\t(SP),HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.DRAWS );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRAWS ) ) {
      /*
       * DRAW-Macro ausfuehren
       *
       * Parameter:
       *   HL: Zeiger auf Macro-String
       * Rueckgabe:
       *   DE: 1. Zeichen hinter der Zeichenkette
       */
      buf.append( "DRAWS:\tEX\tDE,HL\n"
		+ "DRAWS1:\tXOR\tA\n"		// Modus zuruecksetzen
		+ "\tLD\t(M_DRSM),A\n"
		+ "DRAWS2:\tLD\tA,(DE)\n"
		+ "\tINC\tDE\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCALL\tC_UPPER_C\n"
		+ "\tCP\t42H\n"				// B
		+ "\tJP\tZ,DRAWS_MOVE\n"
		+ "\tCP\t44H\n"				// D
		+ "\tJR\tZ,DRAWS_DOWN\n"
		+ "\tCP\t45H\n"				// E
		+ "\tJP\tZ,DRAWS_RIGHT_UP\n"
		+ "\tCP\t46H\n"				// F
		+ "\tJR\tZ,DRAWS_RIGHT_DOWN\n"
		+ "\tCP\t47H\n"				// G
		+ "\tJR\tZ,DRAWS_LEFT_DOWN\n"
		+ "\tCP\t48H\n"				// H
		+ "\tJR\tZ,DRAWS_LEFT_UP\n"
		+ "\tCP\t4CH\n"				// L
		+ "\tJR\tZ,DRAWS_LEFT\n"
		+ "\tCP\t4DH\n"				// M
		+ "\tJP\tZ,DRAWS_TO\n"
		+ "\tCP\t52H\n"				// R
		+ "\tJR\tZ,DRAWS_RIGHT\n"
		+ "\tCP\t55H\n"				// U
		+ "\tJR\tZ,DRAWS_UP\n"
		+ "\tJP\tE_INVALID_PARAM\n"
		+ "DRAWS_DOWN:\n"
		+ "\tCALL\tDRAWS_NUM\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tI2_NEG_HL\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tCALL\tDRAWS_REL\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tDRAWS1\n"
		+ "DRAWS_LEFT:\n"
		+ "\tCALL\tDRAWS_NUM\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tI2_NEG_HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tCALL\tDRAWS_REL\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tDRAWS1\n"
		+ "DRAWS_LEFT_DOWN:\n"
		+ "\tCALL\tDRAWS_NUM\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tI2_NEG_HL\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tCALL\tDRAWS_REL\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tDRAWS1\n"
		+ "DRAWS_LEFT_UP:\n"
		+ "\tCALL\tDRAWS_NUM\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tCALL\tI2_NEG_HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tCALL\tDRAWS_REL\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tDRAWS1\n"
		+ "DRAWS_RIGHT:\n"
		+ "\tCALL\tDRAWS_NUM\n"
		+ "\tPUSH\tDE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tCALL\tDRAWS_REL\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tDRAWS1\n"
		+ "DRAWS_RIGHT_DOWN:\n"
		+ "\tCALL\tDRAWS_NUM\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tCALL\tI2_NEG_HL\n"
		+ "\tCALL\tDRAWS_REL\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tDRAWS1\n"
		+ "DRAWS_RIGHT_UP:\n"
		+ "\tCALL\tDRAWS_NUM\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tCALL\tDRAWS_REL\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tDRAWS1\n"
		+ "DRAWS_UP:\n"
		+ "\tCALL\tDRAWS_NUM\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tCALL\tDRAWS_REL\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tDRAWS1\n"
		+ "DRAWS_TO:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCP\t2BH\n"				// +
		+ "\tJR\tZ,DRAWS_TO_REL\n"
		+ "\tCP\t2DH\n"				// -
		+ "\tJR\tZ,DRAWS_TO_REL\n"
		+ "\tCALL\tDRAWS_PARSE_POINT\n"
		+ "\tLD\tA,(M_DRSM)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,DRAWS_TO1\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\t(LINE_M_EX),BC\n"
		+ "\tLD\t(LINE_M_EY),HL\n"
		+ "\tCALL\tDRAW\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tDRAWS1\n"
		+ "DRAWS_TO1:\n"
		+ "\tLD\t(M_XPOS),BC\n"
		+ "\tLD\t(M_YPOS),HL\n"
		+ "\tJP\tDRAWS1\n"
		+ "DRAWS_TO_REL:\n"
		+ "\tCALL\tDRAWS_PARSE_POINT\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tD,B\n"
		+ "\tLD\tE,C\n"
		+ "\tCALL\tDRAWS_REL\n"
		+ "\tPOP\tDE\n"
		+ "\tJP\tDRAWS1\n"
		+ "DRAWS_MOVE:\n"
		+ "\tLD\tA,01H\n"
		+ "\tLD\t(M_DRSM),A\n"
		+ "\tJP\tDRAWS2\n"
      /*
       * Parsen einer X/Y-Koordinate
       *
       * Parameter:
       *   DE: Zeiger auf die aktuelle Position im Macro-String
       * Rueckgabe:
       *   BC: geparste X-Koordinate
       *   DE: Zeiger auf das erste Zeichen hinter der Zahl
       *   HL: geparste Y-Koordinate
       */
		+ "DRAWS_PARSE_POINT:\n"
		+ "\tCALL\tDRAWS_SIGNED_NUM\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCP\t2CH\n"				// Komma
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tINC\tDE\n"
		+ "\tCALL\tDRAWS_SIGNED_NUM\n"
		+ "\tPOP\tBC\n"
		+ "\tRET\n"
		+ "DRAWS_SIGNED_NUM:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tCP\t2BH\n"				// +
		+ "\tJR\tNZ,DRAWS_SIGNED_NUM1\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tDRAWS_SIGNED_NUM2\n"
		+ "DRAWS_SIGNED_NUM1:\n"
		+ "\tCP\t2DH\n"				// -
		+ "\tJR\tNZ,DRAWS_SIGNED_NUM2\n"
		+ "\tINC\tDE\n"
		+ "\tCALL\tDRAWS_SIGNED_NUM2\n"
		+ "\tCALL\tI2_NEG_HL\n"
		+ "\tRET\n"
		+ "DRAWS_SIGNED_NUM2:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tSUB\t30H\n"
		+ "\tJP\tC,E_INVALID_PARAM\n"
		+ "\tCP\t0AH\n"
		+ "\tJP\tNC,E_INVALID_PARAM\n"
		+ "\tJR\tDRAWS_NUM1\n"
      /*
       * Parsen einer evtl. vorhandenen Zahl,
       * Ist keine Zahl vorhanden, wird 1 zurueckgeliefert.
       *
       * Parameter:
       *   DE: Zeiger auf die aktuelle Position im Macro-String
       * Rueckgabe:
       *   DE: Zeiger auf das erste Zeichen hinter der Zahl
       *   HL: geparste Zahl oder 1, wenn keine vorhanden ist
       */
		+ "DRAWS_NUM:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tSUB\t30H\n"
		+ "\tJR\tC,DRAWS_NUM3\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tNC,DRAWS_NUM3\n"
		+ "DRAWS_NUM1:\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "DRAWS_NUM2:\n"
		+ "\tINC\tDE\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tSUB\t30H\n"
		+ "\tRET\tC\n"
		+ "\tCP\t0AH\n"
		+ "\tRET\tNC\n"
		+ "\tCALL\tI2_APPEND_DIGIT_TO_HL\n"
		+ "\tJR\tNC,DRAWS_NUM2\n"
		+ "\tJP\tE_NUMERIC_OVERFLOW\n"
		+ "DRAWS_NUM3:\n"
		+ "\tLD\tHL,0001H\n"
		+ "\tRET\n"
		+ "DRAWS_REL:\n"
		+ "\tLD\tA,(M_DRSM)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,DRAWR\n"
		+ "\tJP\tMOVER\n" );
      compiler.addLibItem( BasicLibrary.LibItem.C_UPPER_C );
      compiler.addLibItem( BasicLibrary.LibItem.I2_NEG_HL );
      compiler.addLibItem( BasicLibrary.LibItem.I2_APPEND_DIGIT_TO_HL );
      compiler.addLibItem( BasicLibrary.LibItem.DRAW );
      compiler.addLibItem( BasicLibrary.LibItem.DRAWR );
      compiler.addLibItem( BasicLibrary.LibItem.MOVER );
      compiler.addLibItem( BasicLibrary.LibItem.E_NUMERIC_OVERFLOW );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRAWR ) ) {
      /*
       * Linie zum relativ angegebenen Punkt zeichnen,
       * Das Pixel auf der aktuellen Position wird nicht gesetzt.
       *
       * Parameter:
       *   M_XPOS: X-Koordinate Startpunkt
       *   M_YPOS: Y-Koordinate Startpunkt
       *   DE:     relative X-Koordinate Endpunkt
       *   HL:     relative Y-Koordinate Endpunt
       */
      buf.append( "DRAWR:\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_XPOS)\n"
		+ "\tLD\t(LINE_M_BX),HL\n"
		+ "\tCALL\tI2_ADD_I2_I2\n"
		+ "\tLD\t(LINE_M_EX),HL\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tHL,(M_YPOS)\n"
		+ "\tLD\t(LINE_M_BY),HL\n"
		+ "\tCALL\tI2_ADD_I2_I2\n"
		+ "\tLD\t(LINE_M_EY),HL\n"
		+ "\tJR\tDRAW1\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_XYPOS );
      compiler.addLibItem( BasicLibrary.LibItem.I2_ADD_I2_I2 );
      compiler.addLibItem( BasicLibrary.LibItem.DRAW );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRAW ) ) {
      /*
       * Linie zum Punkt (LINE_M_EX,LINE_M_EY) zeichnen,
       * Das Pixel auf der aktuellen Position wird nicht gesetzt.
       *
       * Parameter:
       *   M_XPOS:    X-Koordinate Startpunkt
       *   M_YPOS:    Y-Koordinate Startpunkt
       *   LINE_M_EX: X-Koordinate Endpunkt
       *   LINE_M_EY: Y-Koordinate Endpunkt
       */
      buf.append( "DRAW:\tLD\tHL,(M_XPOS)\n"
		+ "\tLD\t(LINE_M_BX),HL\n"
		+ "\tLD\tHL,(M_YPOS)\n"
		+ "\tLD\t(LINE_M_BY),HL\n"
		+ "DRAW1:\tCALL\tDRAW_LINE2\n"
		+ "\tLD\tHL,(LINE_M_EX)\n"
		+ "\tLD\t(M_XPOS),HL\n"
		+ "\tLD\tHL,(LINE_M_EY)\n"
		+ "\tLD\t(M_YPOS),HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_XYPOS );
      compiler.addLibItem( BasicLibrary.LibItem.DRAW_LINE );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRAW_LINE ) ) {
      /*
       * Linie zeichnen
       *
       * Parameter:
       *   (LINE_M_BX): X-Koordinate Anfang
       *   (LINE_M_BY): Y-Koordinate Anfang
       *   (LINE_M_EX): X-Koordinate Ende
       *   (LINE_M_EY): Y-Koordinate Ende
       * Hilfszellen:
       *   (LINE_M_SX): X-Schrittweite
       *   (LINE_M_SY): Y-Schrittweite
       */
      buf.append( "DRAW_LINE:\n" );
      if( target.supportsXHLINE() ) {
	buf.append( "\tLD\tHL,(LINE_M_EY)\n"
		+ "\tLD\tDE,(LINE_M_BY)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNZ,DRAW_LINE1\n"
		+ "\tCALL\tH_BOX\n"
		+ "\tRET\tC\n"
		+ "\tJP\tDRAW_HLINE\n"
		+ "DRAW_LINE1:\n" );
	compiler.addLibItem( BasicLibrary.LibItem.H_BOX );
	compiler.addLibItem( BasicLibrary.LibItem.DRAW_HLINE );
      }
      buf.append( "\tLD\tDE,(LINE_M_BX)\n"	// Anfangspixel setzen
		+ "\tLD\tHL,(LINE_M_BY)\n"
		+ "\tCALL\tXPSET\n"
		+ "DRAW_LINE2:\n"
		+ "\tLD\tBC,0001H\n"		// sx=1
		+ "\tLD\tHL,(LINE_M_EX)\n"	// dx=xe-xa
		+ "\tLD\tDE,(LINE_M_BX)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tBIT\t7,H\n"
		+ "\tJR\tZ,DRAW_LINE3\n"	// if dx<0 then dx=-dx:sx=-1
		+ "\tCALL\tI2_NEG_HL\n"
		+ "\tLD\tBC,0FFFFH\n"
		+ "DRAW_LINE3:\n"
		+ "\tLD\t(LINE_M_SX),BC\n"
		+ "\tPUSH\tHL\n"		// dx
		+ "\tPUSH\tHL\n"		// dx
		+ "\tLD\tBC,0001H\n"		// sy=1
		+ "\tLD\tHL,(LINE_M_EY)\n"	// dy=ye-ya
		+ "\tLD\tDE,(LINE_M_BY)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tBIT\t7,H\n"
		+ "\tJR\tZ,DRAW_LINE4\n"	// if dx<0 then dx=-dx:sx=-1
		+ "\tCALL\tI2_NEG_HL\n"
		+ "\tLD\tBC,0FFFFH\n"
		+ "DRAW_LINE4:\n"
		+ "\tLD\t(LINE_M_SY),BC\n"
		+ "\tPUSH\tHL\n"		// dy
		+ "\tEXX\n"
		+ "\tPOP\tBC\n"			// BC': dy
		+ "\tPOP\tDE\n"			// DE': dx
		+ "\tEXX\n"
		+ "\tEX\tDE,HL\n"		// dy -> DE
		+ "\tPOP\tHL\n"			// dx
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"		// if dx<dy then DRAW_LINE6
		+ "\tJR\tC,DRAW_LINE7\n"
      /*
       * ABS(dx) >= ABS(dy)
       * 2. Registersatz:
       *     BC: dy
       *     DE: dx
       *     HL: Fehlerglied
       */
		+ "\tEXX\n"
		+ "\tLD\tH,D\n"			// Fehlerglied mit
		+ "\tLD\tL,E\n"			// dx/2 initialisieren
		+ "\tSRA\tH\n"
		+ "\tRR\tL\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,(LINE_M_BX)\n"
		+ "DRAW_LINE5:\n"		// Ende erreicht?
		+ "\tLD\tHL,(LINE_M_EX)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tRET\tZ\n"
		+ "\tEXX\n"			// Test auf Y-Schritt
		+ "\tOR\tA\n"			// Fehler = Fehler - dy
		+ "\tSBC\tHL,BC\n"
		+ "\tEXX\n"
		+ "\tJR\tNC,DRAW_LINE6\n"	// wenn Fehler < 0
		+ "\tEXX\n"
		+ "\tADD\tHL,DE\n"		// Fehler = Fehler + dx
		+ "\tEXX\n"
		+ "\tLD\tHL,(LINE_M_BY)\n"	// Y-Schritt
		+ "\tLD\tDE,(LINE_M_SY)\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(LINE_M_BY),HL\n"
		+ "DRAW_LINE6:\n"		// X-Schritt
		+ "\tLD\tHL,(LINE_M_BX)\n"
		+ "\tLD\tDE,(LINE_M_SX)\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(LINE_M_BX),HL\n"
		+ "\tPUSH\tHL\n"
		+ "\tEX\tDE,HL\n"		// Pixel setzen
		+ "\tLD\tHL,(LINE_M_BY)\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tDRAW_LINE5\n"
      /*
       * ABS(dx) < ABS(dy)
       * 2. Registersatz:
       *     BC: dy
       *     DE: dx
       *     HL: Fehlerglied
       */
		+ "DRAW_LINE7:\n"
		+ "\tEXX\n"
		+ "\tLD\tH,B\n"			// Fehlerglied mit
		+ "\tLD\tL,C\n"			// dy/2 initialisieren
		+ "\tSRA\tH\n"
		+ "\tRR\tL\n"
		+ "\tEXX\n"
		+ "\tLD\tDE,(LINE_M_BY)\n"
		+ "DRAW_LINE8:\n"		// Ende erreicht?
		+ "\tLD\tHL,(LINE_M_EY)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tRET\tZ\n"
		+ "\tEXX\n"			// Test auf X-Schritt
		+ "\tOR\tA\n"			// Fehler = Fehler - dx
		+ "\tSBC\tHL,DE\n"
		+ "\tEXX\n"
		+ "\tJR\tNC,DRAW_LINE9\n"	// wenn Fehler < 0
		+ "\tEXX\n"
		+ "\tADD\tHL,BC\n"		// Fehler = Fehler + dy
		+ "\tEXX\n"
		+ "\tLD\tHL,(LINE_M_BX)\n"	// X-Schritt
		+ "\tLD\tDE,(LINE_M_SX)\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(LINE_M_BX),HL\n"
		+ "DRAW_LINE9:\n"		// Y-Schritt
		+ "\tLD\tHL,(LINE_M_BY)\n"
		+ "\tLD\tDE,(LINE_M_SY)\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(LINE_M_BY),HL\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tDE,(LINE_M_BX)\n"
		+ "\tCALL\tXPSET\n"		// Pixel setzen
		+ "\tPOP\tDE\n"
		+ "\tJR\tDRAW_LINE8\n" );
      compiler.addLibItem( BasicLibrary.LibItem.I2_NEG_HL );
      compiler.addLibItem( BasicLibrary.LibItem.M_XYPOS );
      compiler.addLibItem( BasicLibrary.LibItem.XPSET );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRBOX ) ) {
      /*
       * Rechteck zeichnen
       *
       * Parameter:
       *   (LINE_M_BX): X-Koordinate Anfang
       *   (LINE_M_BY): Y-Koordinate Anfang
       *   (LINE_M_EX): X-Koordinate Ende
       *   (LINE_M_EY): Y-Koordinate Ende
       */
      buf.append( "DRBOX:\tCALL\tH_BOX\n"
		+ "\tRET\tC\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tDRAW_HLINE\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,(LINE_M_EY)\n"
		+ "\tCP\tL\n"
		+ "\tJR\tNZ,DRBOX1\n"
		+ "\tLD\tA,(LINE_M_EY+1)\n"
		+ "\tCP\tH\n"
		+ "\tRET\tZ\n"
		+ "DRBOX1:\tINC\tHL\n"
		+ "\tLD\tA,(LINE_M_EY)\n"
		+ "\tCP\tL\n"
		+ "\tJR\tNZ,DRBOX2\n"
		+ "\tLD\tA,(LINE_M_EY+1)\n"
		+ "\tCP\tH\n"
		+ "\tJP\tZ,DRAW_HLINE\n"
		+ "DRBOX2:\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tDE,(LINE_M_EX)\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tJR\tDRBOX1\n" );
      compiler.addLibItem( BasicLibrary.LibItem.H_BOX );
      compiler.addLibItem( BasicLibrary.LibItem.DRAW_HLINE );
      compiler.addLibItem( BasicLibrary.LibItem.XPSET );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRBOXF ) ) {
      /*
       * ausgefuelltes Rechteck zeichnen
       *
       * Parameter:
       *   (LINE_M_BX): X-Koordinate Anfang
       *   (LINE_M_BY): Y-Koordinate Anfang
       *   (LINE_M_EX): X-Koordinate Ende
       *   (LINE_M_EY): Y-Koordinate Ende
       */
      buf.append( "DRBOXF:\tCALL\tH_BOX\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tDRAW_HLINE_CHECK_X\n"
		+ "DRBXF1:\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n" );
      if( target.supportsXHLINE() ) {
	buf.append( "\tCALL\tXHLINE\n" );
	compiler.addLibItem( BasicLibrary.LibItem.XHLINE );
      } else {
	buf.append( "\tCALL\tDRAW_HLINE1\n" );
      }
      buf.append( "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,(LINE_M_EY)\n"
		+ "\tCP\tL\n"
		+ "\tJR\tNZ,DRFBX2\n"
		+ "\tLD\tA,(LINE_M_EY+1)\n"
		+ "\tCP\tH\n"
		+ "\tRET\tZ\n"
		+ "DRFBX2:\tINC\tHL\n"
		+ "\tJR\tDRBXF1\n" );
      compiler.addLibItem( BasicLibrary.LibItem.H_BOX );
      compiler.addLibItem( BasicLibrary.LibItem.DRAW_HLINE );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.H_BOX ) ) {
      /*
       * Anpassung der Koordinaten fuer das Zeichen eines Rechtecks
       *
       * Parameter:
       *   (LINE_M_BX): X-Koordinate Anfang
       *   (LINE_M_BY): Y-Koordinate Anfang
       *   (LINE_M_EX): X-Koordinate Ende
       *   (LINE_M_EY): Y-Koordinate Ende
       * Rueckgabewerte:
       *   (LINE_M_BX): linke X-Koordinate
       *   (LINE_M_BY): untere Y-Koordinate
       *   (LINE_M_EX): rechte X-Koordinate
       *   (LINE_M_EY): ober Y-Koordinate
       *   DE:          linke X-Koordinate
       *   HL:          untere Y-Koordinate
       *   BC:          Breite des Rechtecks - 1
       *   CY=1:        rechte X- oder obere Y-Koordinate kleiner 0,
       *                d.h., das Rechteck liegt vollstaendig
       *                ausserhalb des sichtbaren Bereichs
       */
      buf.append( "H_BOX:\tLD\tHL,(LINE_M_EY)\n"
		+ "\tLD\tDE,(LINE_M_BY)\n"
		+ "\tCALL\tCMP_HL_DE\n"
		+ "\tJR\tNC,H_BOX1\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\t(LINE_M_BY),DE\n"
		+ "\tLD\t(LINE_M_EY),HL\n"
		+ "H_BOX1:\tBIT\t7,H\n"
		+ "\tJR\tNZ,H_BOX3\n"
		+ "\tLD\tHL,(LINE_M_EX)\n"
		+ "\tLD\tDE,(LINE_M_BX)\n"
		+ "\tCALL\tCMP_HL_DE\n"
		+ "\tJR\tNC,H_BOX2\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\t(LINE_M_BX),DE\n"
		+ "\tLD\t(LINE_M_EX),HL\n"
		+ "H_BOX2:\tBIT\t7,H\n"
		+ "\tJR\tNZ,H_BOX3\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tB,H\n"			// BC = X2 - X1
		+ "\tLD\tC,L\n"
		+ "\tLD\tHL,(LINE_M_BY)\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n"
		+ "H_BOX3:\tSCF\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.CMP_HL_DE );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRAW_HLINE ) ) {
      /*
       * Horizontale Linie zeichnen
       *
       * Parameter:
       *   BC: Laenge - 1
       *   DE: linke X-Koordinate
       *   HL: Y-Koordinate
       */
      buf.append( "DRAW_HLINE:\n"
		+ "\tCALL\tDRAW_HLINE_CHECK_X\n"
		+ "DRAW_HLINE1:\n"
		+ "\tBIT\t7,H\n"		// Y pruefen
		+ "\tRET\tNZ\n"
		+ "\tBIT\t7,B\n"		// Laenge pruefen
		+ "\tRET\tNZ\n" );
      if( target.supportsXHLINE() ) {
	buf.append( "\tJP\tXHLINE\n" );
	compiler.addLibItem( BasicLibrary.LibItem.XHLINE );
      } else {
	buf.append( "DRAW_HLINE2:\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXPSET\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tBC\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tDRAW_HLINE2\n" );
      }
      /*
       * Sicherstellen, dass die X-Koordinate nicht kleiner 0 ist
       *
       * Parameter:
       *   BC: Laenge - 1
       *   DE: linke X-Koordinate
       */
      buf.append( "DRAW_HLINE_CHECK_X:\n"
		+ "\tBIT\t7,D\n"
		+ "\tRET\tZ\n"
		+ "\tEX\tDE,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.XPSET );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.MOVER ) ) {
      /*
       * Grafikkursor relativ positionieren
       *
       * Parameter:
       *   DE: X-Koordinate relativ
       *   HL: Y-Koordinate relativ
       */
      buf.append( "MOVER:\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_XPOS)\n"
		+ "\tCALL\tI2_ADD_I2_I2\n"
		+ "\tLD\t(M_XPOS),HL\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tHL,(M_YPOS)\n"
		+ "\tCALL\tI2_ADD_I2_I2\n"
		+ "\tLD\t(M_YPOS),HL\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.I2_ADD_I2_I2 );
      compiler.addLibItem( BasicLibrary.LibItem.M_XYPOS );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.PAINT ) ) {
      /*
       * Fuellen einer Flaeche
       *
       * Der Algorithmus arbeitet mit einer Tabelle,
       * in der die zu pruefenden Linien einschliesslich
       * der weiteren Suchrichtung eingetragen werden.
       * Ausgehend vom Startpunkt wird die erste Linie gefuellt
       * und im Erfolgsfall der gefundene X-Bereich fuer Y-1 und Y+1
       * in die Tabelle eingetragen.
       * Anschliessend wird die Tabelle durchgegangen und pro Eintrag
       * der eingetragene X-Bereich fuer die Y-Position gefuellt.
       * Konnte wieder eine Linie gefuellt werden, wird der entsprechende
       * X-Bereich mit der naechsten Y-Koordinate in Suchrichtung
       * wieder in die Tabelle eingetragen usw.
       * Wenn eine gefundene zu fuellende Linie links oder rechts
       * mehr als einen Pixel ueber die vorherige gefuellte Linie
       * hinausragt, wird der ueber das eine Pixel hinausragende Bereich
       * mit umgekehrter Suchrichtung in die Tabelle eingetragen,
       * um auch um Ecken herum zu fuellen.
       *
       * Fuer die Tabelle wird ein Block aus dem Heap genommen.
       * Dieser Block wird jedoch nicht allokiert,
       * da er nur waehrend der PAINT-Routine benoetigt wird und
       * in dieser Zeit keine weiteren speicherallokierenden
       * Funktionen aufgerufen werden.
       *
       * Die Tabelle ist als Stack realisiert.
       *
       * Ein Eintrag in der Tabelle hat folgenden Aufbau:
       *   1. Byte: L-Teil der X-Koordinate des Suchanfangs
       *   2. Byte: Bit 0-6: H-Teil der X-Koordinate des Suchanfangs
       *            Bit 7:   Suchrichtung:
       *                      0: nach oben (Y aufsteigend)
       *                      1: nach unten (Y absteigend)
       *   3. Byte: L-Teil der X-Koordinate des Suchendes
       *   4. Byte: H-Teil der X-Koordinate des Suchendes
       *   5. Byte: Y-Koordinate (nur 8 Bit)
       *
       * Parameter:
       *   PAINT_M_X:   X-Koordinate des Ausgangspunktes
       *   PAINT_M_Y:   Y-Koordinate des Ausgangspunktes
       * Hilfszellen:
       *   PAINT_M_TAD: Anfangsadresse der Tabelle
       *   PAINT_M_TSZ: Max. Groesse der Tabelle
       *   PAINT_M_TIX: Index des ersten freien Eintrags in der Tabelle
       */
      buf.append( "PAINT:" );
      // schneller Zugriff auf Bildschirmbreite
      int pos = buf.length();
      target.appendWPixelTo( buf );
      String inst_LD_HL_wPix = buf.cut( pos );
      if( !BasicUtil.isSingleInst_LD_HL_xx( inst_LD_HL_wPix ) ) {
	buf.append( inst_LD_HL_wPix );
	buf.append( "\tLD\t(PAINT_M_WPIX),HL\n" );
	inst_LD_HL_wPix = "\tLD\tHL,(PAINT_M_WPIX)\n";
	compiler.addLibItem( BasicLibrary.LibItem.PAINT_M_WPIX );
      }
      // schneller Zugriff auf Bildschirmhoehe
      pos = buf.length();
      target.appendHPixelTo( buf );
      String inst_LD_HL_hPix = buf.cut( pos );
      if( !BasicUtil.isSingleInst_LD_HL_xx( inst_LD_HL_hPix ) ) {
	buf.append( inst_LD_HL_hPix );
	buf.append( "\tLD\t(PAINT_M_HPIX),HL\n" );
	inst_LD_HL_hPix = "\tLD\tHL,(PAINT_M_HPIX)\n";
	compiler.addLibItem( BasicLibrary.LibItem.PAINT_M_HPIX );
      }
      if( target.supportsXPAINT_LEFT_RIGHT() ) {
	buf.append( "\tCALL\tXPAINT_RIGHT\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tXPAINT_LEFT\n" );
      } else {
	buf.append( "\tCALL\tPAINT_RIGHT\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tPAINT_LEFT\n" );
      }
      // Startpunkt konnte gefuellt werden -> Speicher fuer Tabelle holen
      buf.append( "\tCALL\tMFIND\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\t(PAINT_M_TAD),HL\n"
      // Groesse des Blocks ermitteln
		+ "\tDEC\tHL\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tEX\tDE,HL\n"
      // Anzahl der Eintraege ermitteln
		+ "\tLD\tDE,0005H\n"
		+ "\tCALL\tI2_DIV_I2_I2\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,PAINT1\n"
		+ "\tLD\tL,00H\n"
		+ "\tJP\tM,PAINT1\n"
		+ "\tDEC\tL\n"
		+ "PAINT1:\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJP\tZ,E_OUT_OF_MEM\n"
		+ "\tLD\t(PAINT_M_TSZ),A\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(PAINT_M_TIX),A\n"
      // Bereich ueber und unter der gefuellten Linie anhaengen
		+ "\tCALL\tPAINT_ADD\n"		// A=0 -> nach oben suchen
		+ "\tLD\tA,80H\n"		// nach unten suchen
		+ "\tCALL\tPAINT_ADD\n"
      // Tabelle durchgehen
		+ "PAINT2:\tLD\tA,(PAINT_M_TIX)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"			// kein weiterer Eintrag
      // Eintrag holen
		+ "\tDEC\tA\n"
		+ "\tLD\t(PAINT_M_TIX),A\n"
		+ "\tINC\tA\n"
		+ "\tCALL\tPAINT_GET_ENTRY_ADDR\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tLD\tD,00H\n"
		+ "\tLD\t(PAINT_M_Y),DE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tE,(HL)\n"
		+ "\tLD\t(PAINT_M_SX2),DE\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(PAINT_M_SDIR),A\n"	// weitere Suchrichtung
		+ "\tDEC\tHL\n"
		+ "\tLD\tL,(HL)\n"
		+ "\tAND\t7FH\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\t(PAINT_M_X),HL\n"
      /*
       * Eckpunkte berechnen, ab denen auch in die andere Richtung
       * gesucht werden muss
       */
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,PAINT3\n"
		+ "\tDEC\tHL\n"
		+ "PAINT3:\tLD\t(PAINT_M_CX1),HL\n"
		+ "\tINC\tDE\n" );
      buf.append( inst_LD_HL_wPix );
      buf.append( "\tEX\tDE,HL\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tADD\tHL,DE\n"
		+ "\tJR\tC,PAINT4\n"
		+ "\tDEC\tHL\n"
		+ "PAINT4:\tLD\t(PAINT_M_CX2),HL\n" );
      // am Startpunkt nach links und rechts suchen
      if( target.supportsXPAINT_LEFT_RIGHT() ) {
	buf.append( "\tCALL\tXPAINT_RIGHT\n"
		+ "\tJR\tC,PAINT8\n"		// naechste X-Koordinate
		+ "\tCALL\tXPAINT_LEFT\n" );
      } else {
	buf.append( "\tCALL\tPAINT_RIGHT\n"
		+ "\tJR\tC,PAINT8\n"		// naechste X-Koordinate
		+ "\tCALL\tPAINT_LEFT\n" );
      }
      buf.append( "\tLD\tA,(PAINT_M_SDIR)\n"
		+ "\tCALL\tPAINT_ADD\n"
      // X1 < CX1 ?
		+ "\tLD\tHL,(PAINT_M_X2)\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(PAINT_M_X1)\n"
		+ "\tLD\tDE,(PAINT_M_CX1)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,PAINT5\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\t(PAINT_M_X2),DE\n"
		+ "\tLD\tA,(PAINT_M_SDIR)\n"
		+ "\tXOR\t80H\n"
		+ "\tCALL\tPAINT_ADD\n"
      // X2 > CX2 ?
		+ "PAINT5:\tPOP\tDE\n"		// X2
		+ "PAINT6:\tLD\tHL,(PAINT_M_CX2)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tNC,PAINT7\n"
		+ "\tADD\tHL,DE\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(PAINT_M_X1),HL\n"
		+ "\tLD\t(PAINT_M_X1),DE\n"
		+ "\tLD\tA,(PAINT_M_SDIR)\n"
		+ "\tXOR\t80H\n"
		+ "\tCALL\tPAINT_ADD\n"
      // bei X2+2 weitersuchen
		+ "PAINT7:\tLD\tDE,(PAINT_M_X2)\n"
		+ "\tINC\tDE\n"
		+ "\tJR\tPAINT9\n"
		+ "PAINT8:\tLD\tDE,(PAINT_M_X)\n"
		+ "PAINT9:\tINC\tDE\n"
		+ "\tLD\tHL,(PAINT_M_SX2)\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJP\tC,PAINT2\n"
		+ "\tLD\t(PAINT_M_X),DE\n" );
      // von der neuen X-Koordinate aus nur noch nach rechts suchen
      if( target.supportsXPAINT_LEFT_RIGHT() ) {
	buf.append(  "\tCALL\tXPAINT_RIGHT\n" );
      } else {
	buf.append( "\tCALL\tPAINT_RIGHT\n" );
      }
      buf.append( "\tJR\tC,PAINT8\n"
		+ "\tLD\tHL,(PAINT_M_X)\n"
		+ "\tLD\t(PAINT_M_X1),HL\n"
		+ "\tLD\tA,(PAINT_M_SDIR)\n"
		+ "\tCALL\tPAINT_ADD\n"
		+ "\tLD\tDE,(PAINT_M_X2)\n"
		+ "\tJR\tPAINT6\n" );
      if( !target.supportsXPAINT_LEFT_RIGHT() ) {
	/*
	 * Fuellen einer Linie vom Startpunkt aus nach links,
	 * Der Startpunkt selbst wird nicht geprueft,
	 * da die Routine hinter einem erfolgreichen PAINT_RIGHT
	 * aufgerufen wird und somit der Startpunkt schon gefuellt wurde.
	 * Ebenso erfolgt keine Bereichspruefung der Startkoordinaten.
	 *
	 * Parameter:
	 *   PAINT_M_X:   X-Koordinate Startpunkt
	 *   PAINT_M_Y:   Y-Koordinate Startpunkt
	 * Rueckgabewerte:
	 *   PAINT_M_X1:  linke X-Koordinate der gefuellten Linie
	 */
	buf.append( "PAINT_LEFT:\n"
		+ "\tLD\tDE,(PAINT_M_X)\n"
		+ "PAINT_LEFT1:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,PAINT_LEFT2\n"
		+ "\tDEC\tDE\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tCALL\tXPAINT\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tNC,PAINT_LEFT1\n"
		+ "PAINT_LEFT2:\n"
		+ "\tINC\tDE\n"
		+ "\tLD\t(PAINT_M_X1),DE\n"
		+ "\tRET\n"
	/*
	 * Fuellen einer Linie vom Startpunkt aus nach rechts,
	 * Der Startpunkt selbst wird als erstes geprueft und gefuellt.
	 *
	 * Parameter:
	 *   PAINT_M_X:   X-Koordinate Startpunkt
	 *   PAINT_M_Y:   Y-Koordinate Startpunkt
	 * Rueckgabewerte:
	 *   CY=1:        Startpunkt schon gefuellt oder ausserhalb
	 *                des sichtbaren Bereichs
	 *   PAINT_M_X1:  linke X-Koordinate der gefuellten Linie
	 */
		+ "PAINT_RIGHT:\n"
		+ "\tLD\tDE,(PAINT_M_X)\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tXPAINT\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\tC\n"
		+ "PAINT_RIGHT1:\n"
		+ "\tINC\tDE\n" );
	buf.append( inst_LD_HL_wPix );
	buf.append( "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tZ,PAINT_RIGHT2\n"
		+ "\tJR\tC,PAINT_RIGHT2\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tCALL\tXPAINT\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tNC,PAINT_RIGHT1\n"
		+ "PAINT_RIGHT2:\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\t(PAINT_M_X2),DE\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
      }
      /*
       * Anhaengen eines horizentalen Suchbereichs an die Tabelle,
       * Dabei erfolgt auch eine Bereichspruefung auf die Y-Koordinate.
       *
       * Parameter:
       *   A:           Bit 7: Suchrichtung: 0=hochwaerts, 1=runterwaerts
       *   PAINT_M_X1:  linke X-Koordinate des Suchbereichs
       *   PAINT_M_X2:  rechte X-Koordinate des Suchbereichs
       *   PAINT_M_Y:   Ausgangs-Y-Koordinate des Suchbereichs
       *                hinzugefuegt wird entweder Y-1 oder Y+1
       *   PAINT_M_TAD: Anfangsadresse der Tabelle
       *   PAINT_M_TSZ: maximale Groesse (Anzahl Eintraege) der Tabelle
       *   PAINT_M_TIX: Index in der Tabelle
       * Rueckgabewerte:
       *   PAINT_M_TIX: neuer Index in der Tabelle
       */
      buf.append( "PAINT_ADD:\n"
		+ "\tAND\t80H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tBIT\t7,B\n"
		+ "\tJR\tNZ,PAINT_ADD1\n"
      // Y=Y+1
		+ "\tINC\tHL\n"
		+ "\tEX\tDE,HL\n" );
      buf.append( inst_LD_HL_hPix );
      buf.append( "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tRET\tZ\n"
		+ "\tRET\tC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tJR\tPAINT_ADD2\n"
      // Y=Y-1
		+ "PAINT_ADD1:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tRET\tZ\n"
		+ "\tDEC\tHL\n"
      // Tabelleneintrag schreiben
		+ "PAINT_ADD2:\n"
		+ "\tLD\tA,(PAINT_M_TSZ)\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,(PAINT_M_TIX)\n"
		+ "\tCP\tC\n"
		+ "\tJP\tNC,E_OUT_OF_MEM\n"
		+ "\tLD\tC,A\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tPAINT_GET_ENTRY_ADDR\n"
      // X-Koordinate Suchanfang und weitere Suchrichtung
		+ "\tLD\tDE,(PAINT_M_X1)\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tB\n"
		+ "\tLD\t(HL),A\n"
      // X-Koordinate Suchende
		+ "\tINC\tHL\n"
		+ "\tLD\tDE,(PAINT_M_X2)\n"
		+ "\tLD\t(HL),E\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),D\n"
      // Y-Koordinate
		+ "\tINC\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\t(HL),E\n"
      // Schreibindex inkrementieren
		+ "\tLD\tA,C\n"
		+ "\tINC\tA\n"
		+ "\tLD\t(PAINT_M_TIX),A\n"
		+ "\tRET\n"
      /*
       * Adresse eines Eintrags in der Tabelle berechnen
       *
       * Parameter:
       *   PAINT_M_TAD: Anfangsadresse der Tabelle
       *   A:           Index des Eintrags in der Tabelle
       */
		+ "PAINT_GET_ENTRY_ADDR:\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
      // Index in HL mit 5 multiplizieren
		+ "\tLD\tD,H\n"
		+ "\tLD\tE,L\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
      // Anfangsadresse addieren
		+ "\tLD\tDE,(PAINT_M_TAD)\n"
		+ "\tADD\tHL,DE\n"
		+ "\tRET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.MFIND );
      compiler.addLibItem( BasicLibrary.LibItem.I2_DIV_I2_I2 );
      compiler.addLibItem( BasicLibrary.LibItem.XPAINT );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.PEN ) ) {
      /*
       * Einstellen des Stiftes
       *
       * Parameter:
       *   HL: Mode: 0=Loeschen, 1=Zeichnen, 2=XOR
       */
      buf.append( "PEN:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJP\tNZ,E_INVALID_PARAM\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\t04H\n"
		+ "\tJP\tNC,E_INVALID_PARAM\n"
		+ "\tLD\tA,L\n"
		+ "\tJP\tXPEN\n" );
      compiler.addLibItem( BasicLibrary.LibItem.E_INVALID_PARAM );
      compiler.addLibItem( BasicLibrary.LibItem.XPEN );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.PLOTR ) ) {
      /*
       * Grafikkursor relativ positionieren und Punkt setzen
       *
       * Parameter:
       *   DE: X-Koordinate relativ
       *   HL: Y-Koordinate relativ
       */
      buf.append( "PLOTR:\tPUSH\tHL\n"
		+ "\tLD\tHL,(M_XPOS)\n"
		+ "\tCALL\tI2_ADD_I2_I2\n"
		+ "\tLD\t(LINE_M_EX),HL\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tHL,(M_YPOS)\n"
		+ "\tCALL\tI2_ADD_I2_I2\n"
		+ "\tLD\t(LINE_M_EY),HL\n"
		+ "\tLD\tDE,(M_XPOS)\n"
		+ "\tJP\tXPSET\n" );
      compiler.addLibItem( BasicLibrary.LibItem.M_XYPOS );
      compiler.addLibItem( BasicLibrary.LibItem.I2_ADD_I2_I2 );
      compiler.addLibItem( BasicLibrary.LibItem.XPSET );
    }
  }


  public static void appendDataTo( BasicCompiler compiler )
  {
    AsmCodeBuf   buf     = compiler.getCodeBuf();
    BasicOptions options = compiler.getBasicOptions();

    if( compiler.usesLibItem( BasicLibrary.LibItem.FONT_5X7 ) ) {
      if( options.getShowAssemblerText() ) {
	buf.append( "\n;Zeichensatz\n" );
      }
      buf.append( "FONT_5X7:\n"
		+ "\tDB\t01H,0FAH,00H,00H,00H,00H\n"		// !
		+ "\tDB\t03H,0E0H,00H,0E0H,00H,00H\n"		// "
		+ "\tDB\t05H,28H,0FEH,28H,0FEH,28H\n"		// #
		+ "\tDB\t05H,24H,54H,0FEH,54H,48H\n"		// $
		+ "\tDB\t05H,0C4H,0C8H,10H,26H,46H\n"		// %
		+ "\tDB\t05H,0CH,72H,9AH,64H,0AH\n"		// &
		+ "\tDB\t02H,20H,0C0H,00H,00H,00H\n"		// '
		+ "\tDB\t03H,38H,44H,82H,00H,00H\n"		// (
		+ "\tDB\t03H,82H,44H,38H,00H,00H\n"		// )
		+ "\tDB\t05H,28H,10H,7CH,10H,28H\n"		// *
		+ "\tDB\t05H,10H,10H,7CH,10H,10H\n"		// +
		+ "\tDB\t02H,02H,0CH,00H,00H,00H\n"		// ,
		+ "\tDB\t05H,10H,10H,10H,10H,10H\n"		// -
		+ "\tDB\t01H,02H,00H,00H,00H,00H\n"		// .
		+ "\tDB\t05H,04H,08H,10H,20H,40H\n"		// /
		+ "\tDB\t05H,7CH,8AH,92H,0A2H,7CH\n"		// 0
		+ "\tDB\t03H,42H,0FEH,02H,00H,00H\n"		// 1
		+ "\tDB\t05H,46H,8AH,92H,92H,62H\n"		// 2
		+ "\tDB\t05H,84H,82H,92H,0B2H,0CCH\n"		// 3
		+ "\tDB\t05H,18H,28H,48H,0FEH,08H\n"		// 4
		+ "\tDB\t05H,0E4H,0A2H,0A2H,0A2H,9CH\n"		// 5
		+ "\tDB\t05H,0CH,32H,52H,92H,0CH\n"		// 6
		+ "\tDB\t05H,80H,8EH,90H,0A0H,0C0H\n"		// 7
		+ "\tDB\t05H,6CH,92H,92H,92H,6CH\n"		// 8
		+ "\tDB\t05H,60H,92H,94H,98H,60H\n"		// 9
		+ "\tDB\t01H,24H,00H,00H,00H,00H\n"		// :
		+ "\tDB\t02H,02H,2CH,00H,00H,00H\n"		// ;
		+ "\tDB\t04H,10H,28H,44H,82H,00H\n"		// <
		+ "\tDB\t05H,28H,28H,28H,28H,28H\n"		// =
		+ "\tDB\t04H,82H,44H,28H,10H,00H\n"		// >
		+ "\tDB\t05H,40H,80H,8AH,90H,60H\n"		// ?
		+ "\tDB\t05H,7CH,82H,9AH,0AAH,78H\n"		// @
		+ "\tDB\t05H,3EH,48H,88H,48H,3EH\n"		// A
		+ "\tDB\t05H,82H,0FEH,92H,92H,6CH\n"		// B
		+ "\tDB\t05H,7CH,82H,82H,82H,44H\n"		// C
		+ "\tDB\t05H,82H,0FEH,82H,82H,7CH\n"		// D
		+ "\tDB\t04H,0FEH,92H,92H,82H,00H\n"		// E
		+ "\tDB\t04H,0FEH,90H,90H,80H,00H\n"		// F
		+ "\tDB\t05H,7CH,82H,82H,92H,5CH\n"		// G
		+ "\tDB\t05H,0FEH,10H,10H,10H,0FEH\n"		// H
		+ "\tDB\t03H,82H,0FEH,82H,00H,00H\n"		// I
		+ "\tDB\t05H,04H,02H,82H,0FCH,80H\n"		// J
		+ "\tDB\t05H,0FEH,10H,28H,44H,82H\n"		// K
		+ "\tDB\t04H,0FEH,02H,02H,02H,00H\n"		// L
		+ "\tDB\t05H,0FEH,40H,30H,40H,0FEH\n"		// M
		+ "\tDB\t05H,0FEH,60H,10H,0CH,0FEH\n"		// N
		+ "\tDB\t05H,7CH,82H,82H,82H,7CH\n"		// O
		+ "\tDB\t05H,0FEH,90H,90H,90H,60H\n"		// P
		+ "\tDB\t05H,7CH,82H,8AH,84H,7AH\n"		// Q
		+ "\tDB\t05H,0FEH,90H,98H,94H,62H\n"		// R
		+ "\tDB\t05H,64H,92H,92H,92H,4CH\n"		// S
		+ "\tDB\t05H,80H,80H,0FEH,80H,80H\n"		// T
		+ "\tDB\t05H,0FCH,02H,02H,02H,0FCH\n"		// U
		+ "\tDB\t05H,0E0H,18H,06H,18H,0E0H\n"		// V
		+ "\tDB\t05H,0FCH,02H,1CH,02H,0FCH\n"		// W
		+ "\tDB\t05H,0C6H,28H,10H,28H,0C6H\n"		// X
		+ "\tDB\t05H,0E0H,10H,0EH,10H,0E0H\n"		// Y
		+ "\tDB\t05H,86H,8AH,92H,0A2H,0C2H\n"		// Z
		+ "\tDB\t03H,0FEH,82H,82H,00H,00H\n"		// [
		+ "\tDB\t05H,40H,20H,10H,08H,04H\n"		// \
		+ "\tDB\t03H,82H,82H,0FEH,00H,00H\n"		// ]
		+ "\tDB\t05H,20H,40H,80H,40H,20H\n"		// ^
		+ "\tDB\t05H,02H,02H,02H,02H,02H\n"		// _
		+ "\tDB\t02H,80H,40H,00H,00H,00H\n"		// `
		+ "\tDB\t05H,04H,2AH,2AH,2AH,1EH\n"		// a
		+ "\tDB\t05H,0FEH,22H,22H,22H,1CH\n"		// b
		+ "\tDB\t04H,1CH,22H,22H,22H,00H\n"		// c
		+ "\tDB\t05H,1CH,22H,22H,22H,0FEH\n"		// d
		+ "\tDB\t05H,1CH,2AH,2AH,2AH,18H\n"		// e
		+ "\tDB\t04H,20H,7EH,0A0H,80H,00H\n"		// f
		+ "\tDB\t05H,18H,25H,25H,25H,3EH\n"		// g
		+ "\tDB\t04H,0FEH,20H,20H,1EH,00H\n"		// h
		+ "\tDB\t03H,22H,0BEH,02H,00H,00H\n"		// i
		+ "\tDB\t04H,02H,01H,21H,0BEH,00H\n"		// j
		+ "\tDB\t05H,0FEH,04H,08H,14H,22H\n"		// k
		+ "\tDB\t03H,0FCH,02H,00H,00H,00H\n"		// l
		+ "\tDB\t05H,3EH,20H,1EH,20H,1EH\n"		// m
		+ "\tDB\t05H,3EH,10H,20H,20H,1EH\n"		// n
		+ "\tDB\t05H,1CH,22H,22H,22H,1CH\n"		// o
		+ "\tDB\t05H,3FH,24H,24H,24H,18H\n"		// p
		+ "\tDB\t05H,18H,24H,24H,24H,3FH\n"		// q
		+ "\tDB\t04H,3EH,10H,20H,20H,00H\n"		// r
		+ "\tDB\t05H,12H,2AH,2AH,2AH,24H\n"		// s
		+ "\tDB\t03H,20H,0FCH,22H,00H,00H\n"		// t
		+ "\tDB\t05H,3CH,02H,02H,04H,3EH\n"		// u
		+ "\tDB\t05H,38H,04H,02H,04H,38H\n"		// v
		+ "\tDB\t05H,3CH,02H,0CH,02H,3CH\n"		// w
		+ "\tDB\t05H,22H,14H,08H,14H,22H\n"		// x
		+ "\tDB\t05H,31H,0AH,04H,08H,30H\n"		// y
		+ "\tDB\t05H,22H,26H,2AH,32H,22H\n"		// z
		+ "\tDB\t03H,10H,6CH,82H,00H,00H\n"		// {
		+ "\tDB\t01H,0FEH,00H,00H,00H,00H\n"		// |
		+ "\tDB\t03H,82H,6CH,10H,00H,00H\n"		// }
		+ "\tDB\t05H,10H,20H,10H,08H,10H\n"		// ~
		+ "\n" );
    }
  }


  public static void appendBssTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler )
  {
    BasicOptions options = compiler.getBasicOptions();

    if( compiler.usesLibItem( BasicLibrary.LibItem.M_XYPOS ) ) {
      buf.append( "M_XPOS:\tDS\t2\n"
		+ "M_YPOS:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRAWS ) ) {
      buf.append( "M_DRSM:\tDS\t1\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.DRBOX )
	|| compiler.usesLibItem( BasicLibrary.LibItem.DRBOXF )
	|| compiler.usesLibItem( BasicLibrary.LibItem.DRAW_LINE ) )
    {
      buf.append( "LINE_M_BX:\tDS\t2\n"
		+ "LINE_M_BY:\tDS\t2\n"
		+ "LINE_M_EX:\tDS\t2\n"
		+ "LINE_M_EY:\tDS\t2\n"
		+ "LINE_M_SX:\tDS\t2\n"
		+ "LINE_M_SY:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.CIRCLE ) ) {
      buf.append( "CIRCLE_M_X:\tDS\t2\n"
		+ "CIRCLE_M_Y:\tDS\t2\n"
		+ "CIRCLE_M_R:\tDS\t2\n"
		+ "CIRCLE_M_ER:\tDS\t2\n"
		+ "CIRCLE_M_RX:\tDS\t2\n"
		+ "CIRCLE_M_RY:\tDS\t2\n"
		+ "CIRCLE_M_XMX:\tDS\t2\n"
		+ "CIRCLE_M_XPX:\tDS\t2\n"
		+ "CIRCLE_M_XMY:\tDS\t2\n"
		+ "CIRCLE_M_XPY:\tDS\t2\n"
		+ "CIRCLE_M_YMX:\tDS\t2\n"
		+ "CIRCLE_M_YPX:\tDS\t2\n"
		+ "CIRCLE_M_YMY:\tDS\t2\n"
		+ "CIRCLE_M_YPY:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.PAINT ) ) {
      buf.append( "PAINT_M_X:\tDS\t2\n"
		+ "PAINT_M_Y:\tDS\t2\n"
		+ "PAINT_M_X1:\tDS\t2\n"
		+ "PAINT_M_X2:\tDS\t2\n"
		+ "PAINT_M_SX2:\tDS\t2\n"
		+ "PAINT_M_SDIR:\tDS\t1\n"
		+ "PAINT_M_CX1:\tDS\t2\n"
		+ "PAINT_M_CX2:\tDS\t2\n"
		+ "PAINT_M_TAD:\tDS\t2\n"
		+ "PAINT_M_TSZ:\tDS\t1\n"
		+ "PAINT_M_TIX:\tDS\t1\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.PAINT_M_HPIX ) ) {
      buf.append( "PAINT_M_HPIX:\tDS\t2\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.PAINT_M_WPIX ) ) {
      buf.append( "PAINT_M_WPIX:\tDS\t2\n" );
    }
  }


  public static void appendInitTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler )
  {
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_XYPOS ) ) {
      buf.append( "\tLD\tHL,0000H\n"
		+ "\tLD\t(M_XPOS),HL\n"
		+ "\tLD\t(M_YPOS),HL\n" );
    }
  }


	/* --- Konstruktor --- */

  private GraphicsLibrary()
  {
    // nicht intstanziierbar
  }
}
