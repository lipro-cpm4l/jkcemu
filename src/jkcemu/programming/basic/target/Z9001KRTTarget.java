/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Z9001-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Ansteuerung der KRT-Grafik
 */

package jkcemu.programming.basic.target;

import jkcemu.base.EmuSys;
import jkcemu.emusys.Z9001;
import jkcemu.programming.basic.AsmCodeBuf;
import jkcemu.programming.basic.BasicCompiler;
import jkcemu.programming.basic.BasicLibrary;


public class Z9001KRTTarget extends Z9001Target
{
  public static final String BASIC_TARGET_NAME = "TARGET_Z9001_KRT";

  private boolean needsScreenSizeChar;
  private boolean needsScreenSizePixel;
  private boolean usesScreens;
  private boolean usesPaint;


  public Z9001KRTTarget()
  {
    setNamedValue( "GRAPHICSCREEN", 1 );
    setNamedValue( "LASTSCREEN", 1 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    if( this.usesPaint ) {
      buf.append( "X_M_PBANK:\tDS\t1\n" );
    }
    if( this.usesScreens ) {
      buf.append( "X_M_SCREEN:\tDS\t1\n" );
    }
  }


  @Override
  public void appendEtcPastXOutTo( AsmCodeBuf buf )
  {
    if( this.needsScreenSizeChar ) {
      if( this.usesScreens ) {
	buf.append( "X_HCHR:\tLD\tHL,001BH\n"
		+ "\tJR\tX_SSZC\n"
		+ "X_WCHR:\tLD\tHL,0028H\n"
		+ "X_SSZC:\tLD\tA,(X_M_SCREEN)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tL,00H\n"
		+ "\tRET\n" );
      } else {
	buf.append( "X_HCHR:\tLD\tHL,001BH\n"
		+ "\tRET\n"
		+ "X_WCHR:\tLD\tHL,0028H\n"
		+ "\tRET\n" );
      }
    }
    if( this.needsScreenSizePixel ) {
      if( this.usesScreens ) {
	buf.append( "X_HPIX:\tLD\tHL,00C0H\n"
		+ "\tJR\tX_HWPIX_1\n"
		+ "X_WPIX:\tLD\tHL,0140H\n"
		+ "X_HWPIX_1:\n"
		+ "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tDEC\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
      } else {
	buf.append( "X_HPIX:\n"
		+ "X_WPIX:\tLD\tHL,0000H\n"
		+ "\tRET\n" );
      }
    }
  }


  @Override
  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_HCHR\n" );
    this.needsScreenSizeChar = true;
  }


  @Override
  public void appendHPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_HPIX\n" );
    this.needsScreenSizePixel = true;
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    if( this.usesScreens ) {
      buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_SCREEN),A\n"
		+ "\tOUT\t(0B8H),A\n" );
    }
  }


  @Override
  public void appendPreExitTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "\tLD\tL,00H\n"
		+ "\tCALL\tX_SCREEN_1\n" );
    }
  }


  @Override
  public void appendSwitchToTextScreenTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "\tLD\tL,00H\n"
		+ "\tCALL\tX_SCREEN_1\n" );
    }
  }


  @Override
  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_WCHR\n" );
    this.needsScreenSizeChar = true;
  }


  @Override
  public void appendWPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_WPIX\n" );
    this.needsScreenSizePixel = true;
  }


  @Override
  public void appendXClsTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "XCLS:\tLD\tA,(X_M_SCREEN)\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,X_CLS_1\n"
		+ "\tLD\tA,0CH\n"
		+ "\tJR\tXOUTCH\n"
		+ "X_CLS_1:\n"
		+ "\tDI\n"
		+ "\tLD\tA,08H\n"
		+ "X_CLS_2:\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tPUSH\tAF\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tHL,0EC00H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tDE,0EC01H\n"
		+ "\tLD\tBC,03BFH\n"
		+ "\tLDIR\n"
		+ "\tPOP\tAF\n"
		+ "\tINC\tA\n"
		+ "\tCP\t10H\n"
		+ "\tJR\tC,X_CLS_2\n"
		+ "\tLD\tA,08\n"
		+ "\tOUT\t(0B8H),A\n"		// Bank 0 einstellen
		+ "\tEI\n"
		+ "\tLD\tHL,0E800H\n"
		+ "\tLD\tA,(0027H)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tDE,0E801H\n"
		+ "\tLD\tBC,03BFH\n"
		+ "\tLDIR\n"
		+ "\tRET\n" );
    } else {
      buf.append( "XCLS:\tLD\tA,0CH\n"
		+ "\tJR\tXOUTCH\n" );
    }
    appendXOutchTo( buf );
  }


  /*
   * Zeichnen einer horizontalen Linie
   * Parameter:
   *   BC: Laenge - 1
   *   DE: linke X-Koordinate, nicht kleiner 0
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXHLineTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XHLINE:\tBIT\t7,B\n"
		+ "\tRET\tNZ\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tX_PINFO\n"
		+ "\tLD\tA,B\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tRET\tC\n"
		+ "\tLD\tD,00H\n"
		+ "X_HLINE_1:\n"
		+ "\tOR\tD\n"
		+ "\tLD\tD,A\n"
		+ "\tSRL\tA\n"
		+ "\tJR\tNC,X_HLINE_2\n"
		+ "\tLD\tA,D\n"
		+ "\tEXX\n"
		+ "\tLD\tB,A\n"
		+ "\tCALL\tX_PSET_B\n" );
      if( this.usesColors ) {
	/*
	 * Bei Verwendung von Farben zeigt HL nach Aufruf von X_PSET_B
	 * nicht auf den Pixel- sondern den Farbspeicher
	 * und muss deshalb korrigiert werden.
	 */
	buf.append( "\tSET\t2,H\n" );
      }
      buf.append( "\tINC\tHL\n"
		+ "\tEXX\n"
		+ "\tLD\tA,80H\n"
		+ "\tLD\tD,00H\n"
		+ "X_HLINE_2:\n"
		+ "\tDEC\tBC\n"
		+ "\tBIT\t7,B\n"
		+ "\tJR\tZ,X_HLINE_1\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tEXX\n"
		+ "\tLD\tB,A\n"
		+ "\tJR\tX_PSET_B\n" );
    appendXPSetTo( buf, compiler );
  }


  @Override
  public void appendXOutchTo( AsmCodeBuf buf )
  {
    if( !this.xOutchAppended ) {
      buf.append( "XOUTCH:\tLD\tE,A\n" );
      if( this.usesScreens ) {
	buf.append( "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n" );
      }
      buf.append( "\tLD\tC,2\n"
		+ "\tJP\t0005H\n" );
      this.xOutchAppended = true;
    }
  }


  /*
   * XPAINT_LEFT:
   *   Fuellen einer Linie ab dem uebergebenen Punkt nach links,
   *   Der Startpunkt selbst wird nicht geprueft
   *   Parameter:
   *     PAINT_M_X:    X-Koordinate Startpunkt
   *     PAINT_M_Y:    Y-Koordinate Startpunkt
   *   Rueckgabe:
   *     (PAINT_M_X1): X-Endkoordinate der gefuellten Linie,
   *                   kleiner oder gleich X-Startpunkt
   *
   * XPAINT_RIGHT:
   *   Fuellen einer Linie ab dem uebergebenen Punkt nach rechts
   *   Parameter:
   *     PAINT_M_X:    X-Koordinate Startpunkt
   *     PAINT_M_Y:    Y-Koordinate Startpunkt
   *   Rueckgabe:
   *     CY=1:         Pixel im Startpunkt bereits gesetzt (gefuellt)
   *                   oder ausserhalb des sichtbaren Bereichs
   *     (PAINT_M_X2): X-Endkoordinate der gefuellten Linie, nur bei CY=0
   */
  @Override
  public void appendXPaintTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPAINT_LEFT:\n"
		+ "\tLD\tDE,(PAINT_M_X)\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,X_PAINT_LEFT_6\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tDEC\tDE\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_PINFO\n"
		+ "\tLD\tA,C\n"
		+ "\tLD\t(X_M_PBANK),A\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tC,X_PAINT_LEFT_5\n"
		+ "\tLD\tC,B\n"
		+ "\tCALL\tX_PAINT_RD_B\n"
		+ "X_PAINT_LEFT_1:\n"
		+ "\tLD\tA,B\n"
		+ "X_PAINT_LEFT_2:\n"
		+ "\tAND\tC\n"
		+ "\tJR\tNZ,X_PAINT_LEFT_4\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tLD\tB,A\n"
		+ "\tDEC\tDE\n"
		+ "\tSLA\tC\n"
		+ "\tJR\tNC,X_PAINT_LEFT_2\n"
		+ "\tCALL\tX_PAINT_WR_B\n"
		+ "\tDEC\tHL\n"
		+ "\tBIT\t0,D\n"
		+ "\tJR\tZ,X_PAINT_LEFT_3\n"
		+ "\tLD\tA,E\n"
		+ "\tINC\tA\n"
		+ "\tJR\tZ,X_PAINT_LEFT_5\n"
		+ "X_PAINT_LEFT_3:\n"
		+ "\tCALL\tX_PAINT_RD_B\n"
		+ "\tLD\tC,01H\n"
		+ "\tJR\tX_PAINT_LEFT_1\n"
		+ "X_PAINT_LEFT_4:\n"
		+ "\tCALL\tX_PAINT_WR_B\n"
		+ "X_PAINT_LEFT_5:\n"
		+ "\tINC\tDE\n"
		+ "X_PAINT_LEFT_6:\n"
		+ "\tLD\t(PAINT_M_X1),DE\n"
		+ "\tRET\n"
		+ "XPAINT_RIGHT:\n"
		+ "\tLD\tDE,(PAINT_M_X)\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_PINFO\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,C\n"
		+ "\tLD\t(X_M_PBANK),A\n"
		+ "\tLD\tC,B\n"
		+ "\tCALL\tX_PAINT_RD_B\n"
		+ "\tLD\tA,C\n"
		+ "\tAND\tB\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tJR\tX_PAINT_RIGHT_3\n"
		+ "X_PAINT_RIGHT_1:\n"
		+ "\tLD\tA,B\n"
		+ "X_PAINT_RIGHT_2:\n"
		+ "\tAND\tC\n"
		+ "\tJR\tNZ,X_PAINT_RIGHT_5\n"
		+ "X_PAINT_RIGHT_3:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tLD\tB,A\n"
		+ "\tINC\tDE\n"
		+ "\tSRL\tC\n"
		+ "\tJR\tNC,X_PAINT_RIGHT_2\n"
		+ "\tCALL\tX_PAINT_WR_B\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,E\n"
		+ "\tCP\t40H\n"
		+ "\tJR\tNZ,X_PAINT_RIGHT_4\n"
		+ "\tLD\tA,D\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_PAINT_RIGHT_6\n"
		+ "X_PAINT_RIGHT_4:\n"
		+ "\tCALL\tX_PAINT_RD_B\n"
		+ "\tLD\tC,80H\n"
		+ "\tJR\tX_PAINT_RIGHT_1\n"
		+ "X_PAINT_RIGHT_5:\n"
		+ "\tCALL\tX_PAINT_WR_B\n"
		+ "X_PAINT_RIGHT_6:\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\t(PAINT_M_X2),DE\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n"
		+ "X_PAINT_RD_B:\n"
		+ "\tDI\n"
		+ "\tLD\tA,(X_M_PBANK)\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\tA,08H\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tEI\n"
		+ "\tRET\n"
		+ "X_PAINT_WR_B:\n"
		+ "\tDI\n"
		+ "\tLD\tA,(X_M_PBANK)\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\t(HL),B\n"
		+ "\tLD\tA,08H\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tEI\n" );
    if( this.usesColors ) {
      buf.append( "\tRES\t2,H\n"
		+ "\tLD\tA,(0027H)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tSET\t2,H\n" );
    }
    buf.append( "\tRET\n" );
    appendPixUtilTo( buf, compiler );
    this.usesPaint = true;
  }


  /*
   * Farbe eines Pixels ermitteln
   * Parameter:
   *   DE: X-Koordinate (0...255)
   *   HL: Y-Koordinate (0...255)
   * Rueckgabe:
   *   HL >= 0: Farbcode des Pixels
   *   HL=-1:   Pixel exisitiert nicht
   */
  @Override
  public void appendXPointTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( this.usesScreens ) {
      buf.append( "XPOINT:\tLD\tA,(X_M_SCREEN)\n"
                + "\tCP\t01H\n"
                + "\tJR\tNZ,X_POINT_2\n"
		+ "\tCALL\tX_PINFO_1\n"
                + "\tJR\tC,X_POINT_2\n"
      // Pixel lesen 
		+ "\tDI\n"
		+ "\tLD\tA,C\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,08H\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tEI\n"
      // Farbbyte lesen und Pixel auswerten
		+ "\tRES\t2,H\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\tE,07H\n"
		+ "\tJR\tZ,X_POINT_1\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tLD\tE,0FH\n"
		+ "X_POINT_1:\n"
		+ "\tAND\tE\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "X_POINT_2:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
      appendPixUtilTo( buf, compiler );
    } else {
      buf.append( "XPOINT:\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    }
  }


  /*
   * Zuruecksetzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXPResTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPRES:\tCALL\tX_PINFO\n"
		+ "\tRET\tC\n" );
    if( this.usesX_M_PEN ) {
      buf.append( "\tJR\tX_PSET_1\n" );
    } else {
      buf.append( "\tDI\n"
		+ "\tLD\tA,C\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,B\n"
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tJR\tX_PSET_WR_A\n" );
    }
    appendXPSetTo( buf, compiler );
  }


  /*
   * Setzen eines Pixels unter Beruecksichtigung des eingestellten Stiftes
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXPSetTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.xpsetAppended ) {
      buf.append( "XPSET:\tCALL\tX_PINFO\n"
		+ "\tRET\tC\n"
		+ "X_PSET_B:\n" );
      if( this.usesX_M_PEN ) {
	buf.append( "\tLD\tA,(X_M_PEN)\n"
                + "\tDEC\tA\n"
                + "\tJR\tZ,X_PSET_2\n"		// Stift 1 (Normal)
                + "\tDEC\tA\n"
                + "\tJR\tZ,X_PSET_1\n"		// Stift 2 (Loeschen)
                + "\tDEC\tA\n"
                + "\tRET\tNZ\n"
		+ "\tDI\n"			// Stift 3 (XOR-Mode)
		+ "\tLD\tA,C\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,B\n"
		+ "\tXOR\t(HL)\n"
		+ "\tJR\tX_PSET_WR_A\n"
		+ "X_PSET_1:\n"			// Pixel loeschen
		+ "\tDI\n"
		+ "\tLD\tA,C\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,B\n"
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tJR\tX_PSET_WR_A\n" );
      }
      buf.append( "X_PSET_2:\n"
		+ "\tDI\n"			// Pixel setzen
		+ "\tLD\tA,C\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\t(HL)\n"
		+ "X_PSET_WR_A:\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tA,08H\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tEI\n" );
      if( this.usesColors ) {
	buf.append( "\tRES\t2,H\n"
		+ "\tLD\tA,(0027H)\n"
		+ "\tLD\t(HL),A\n" );
      }
      buf.append( "\tRET\n" );
      appendPixUtilTo( buf, compiler );
      this.xpsetAppended = true;
    }
  }


  /*
   * Testen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   * Rueckgabe:
   *   HL=0:  Pixel nicht gesetzt
   *   HL=1:  Pixel gesetzt
   *   HL=-1: Pixel existiert nicht
   */
  @Override
  public void appendXPTestTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( this.usesScreens ) {
      buf.append( "XPTEST:\n"
		+ "\tLD\tA,(X_M_SCREEN)\n"
                + "\tCP\t01H\n"
                + "\tJR\tNZ,XPTST1\n"
		+ "\tCALL\tX_PINFO_1\n"
                + "\tJR\tC,XPTST1\n"
		+ "\tDI\n"
		+ "\tLD\tA,C\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\tA,08H\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tEI\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
		+ "XPTST1:\tLD\tHL,0FFFFH\n"
                + "\tRET\n" );
      appendPixUtilTo( buf, compiler );
    } else {
      buf.append( "XPTEST:\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    }
  }


  /*
   * Bildschirmmode einstellen
   *   HL: Screen-Nummer
   *        0: Textmode
   *        1: KRT-Grafik
   * Rueckgabe:
   *   CY=1: Screen-Nummer nicht unterstuetzt
   */
  @Override
  public void appendXScreenTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "XSCREEN:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,X_SCREEN_2\n"
		+ "X_SCREEN_1:\n"
		+ "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tCP\tL\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_SCREEN_3\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,X_SCREEN_4\n"
		+ "X_SCREEN_2:\n"
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "X_SCREEN_3:\n"
		+ "\tLD\tDE,0800H\n"		// KRT -> STD
		+ "\tJR\tX_SCREEN_5\n"
		+ "X_SCREEN_4:\n"
		+ "\tLD\tC,29\n"		// Cursor abschalten
		+ "\tCALL\t0005H\n"
		+ "\tLD\tDE,0008H\n"		// STD -> KRT
    // SCREEN-Nr. setzen und Systemzellen umkopieren
		+ "X_SCREEN_5:\n"
		+ "\tLD\t(X_M_SCREEN),A\n"
		+ "\tLD\tBC,40B8H\n"
		+ "\tLD\tHL,0EFC0H\n"
		+ "\tDI\n"
		+ "X_SCREEN_6:\n"
		+ "\tOUT\t(C),D\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tX_SCREEN_6\n"
		+ "\tEI\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
    }
  }


  @Override
  public String[] getBasicTargetNames()
  {
    return add( super.getBasicTargetNames(), BASIC_TARGET_NAME );
  }


  @Override
  public int getCompatibilityLevel( EmuSys emuSys )
  {
    int rv = 0;
    if( emuSys != null ) {
      if( emuSys instanceof Z9001 ) {
	rv = 2;
	if( ((Z9001) emuSys).emulatesGraphicKRT() ) {
	  rv = 3;
	}
      }
    }
    return rv;
  }


  @Override
  public void preAppendLibraryCode( BasicCompiler compiler )
  {
    super.preAppendLibraryCode( compiler );
    if( compiler.usesLibItem( BasicLibrary.LibItem.SCREEN )
	|| compiler.usesLibItem( BasicLibrary.LibItem.XSCREEN ) )
    {
      this.usesScreens = true;
    }
  }


  @Override
  public void reset()
  {
    super.reset();
    this.needsScreenSizeChar  = false;
    this.needsScreenSizePixel = false;
    this.usesScreens          = false;
    this.usesPaint            = false;
  }


  @Override
  public boolean supportsGraphics()
  {
    return true;
  }


  @Override
  public boolean supportsXCLS()
  {
    return true;
  }


  @Override
  public boolean supportsXHLINE()
  {
    return true;
  }


  @Override
  public boolean supportsXPAINT_LEFT_RIGHT()
  {
    return true;
  }


  @Override
  public String toString()
  {
    return "KC85/1, KC87, Z9001 mit KRT-Grafik";
  }


	/* --- private Methoden --- */

  private void appendPixUtilTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.pixUtilAppended ) {
      /*
       * Pruefen der Parameter
       * Ermitteln von Informationen zu einem Pixel
       *
       * Parameter:
       *   DE: X-Koordinate (0...320)
       *   HL: Y-Koordinate (0...192)
       *
       * Rueckgabe:
       *   CY=1: Pixel ausserhalb des gueltigen Bereichs
       *   B:    Bitmuster mit einem gesetzten Bit,
       *         dass das Pixel in der Speicherzelle beschreibt
       *   C:    Nr. der Speicherbank inkl. gesetzten Bit 3
       *   HL:   Speicherzelle, in der sich das Pixel befindet
       */
      if( this.usesScreens ) {
	buf.append( "X_PINFO:\n"
		+ "\tLD\tA,(X_M_SCREEN)\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tNZ,X_PINFO_5\n"
		+ "X_PINFO_1:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_PINFO_2\n"
		+ "\tCP\t02H\n"
		+ "\tCCF\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "X_PINFO_2:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,0BFH\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,E\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tC,80H\n"
		+ "\tJR\tZ,X_PINFO_4\n"
		+ "X_PINFO_3:\n"
		+ "\tSRL\tC\n"
		+ "\tDJNZ\tX_PINFO_3\n"
		+ "X_PINFO_4:\n"
		+ "\tSRL\tD\n"
		+ "\tRR\tE\n"
		+ "\tSRL\tE\n"
		+ "\tSRL\tE\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tB,C\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,0FH\n"
		+ "\tSUB\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tPUSH\tBC\n"
		+ "\tSRL\tL\n"		// H=0
		+ "\tSRL\tL\n"
		+ "\tSRL\tL\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tLD\tHL,0EF98H\n"
		+ "\tADD\tHL,DE\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tPOP\tBC\n"
		+ "\tOR\tA\n"		// CY=0
		+ "\tRET\n"
		+ "X_PINFO_5:\n" );
      } else {
	buf.append( "X_PINFO:\n" );
      }
      appendExitNoGraphicsScreenTo( buf, compiler );
      this.pixUtilAppended = true;
    }
  }
}
