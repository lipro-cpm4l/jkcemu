/*
 * (c) 2014-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * KC85/4..5-spezifische Code-Erzeugung des BASIC-Compilers
 * mit direktem Hardwarezugriff bei den Grafikfunktionen
 */

package jkcemu.programming.basic.target;

import jkcemu.base.EmuSys;
import jkcemu.emusys.KC85;
import jkcemu.programming.basic.AsmCodeBuf;
import jkcemu.programming.basic.BasicCompiler;
import jkcemu.programming.basic.BasicLibrary;


public class KC854Target extends KC85Target
{
  public static final String BASIC_TARGET_NAME = "TARGET_KC85_4";

  private boolean usesScreens;


  public KC854Target()
  {
    setNamedValue( "LASTSCREEN", 1 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    if( this.usesScreens ) {
      buf.append( "X_M_SCRWIN:\tDS\t2\n" );
    }
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    if( this.usesScreens ) {
      buf.append( "\tLD\tA,(0B79BH)\n"		// Nr. aktuelles Fenster
		+ "\tLD\tHL,X_M_SCRWIN\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tINC\tA\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tC,X_INIT_WIN_3\n"
		+ "\tXOR\tA\n"
		+ "X_INIT_WIN_3:\n"
		+ "\tLD\t(HL),A\n" );
    }
  }


  @Override
  public void appendPreExitTo( AsmCodeBuf buf )
  {
    super.appendPreExitTo( buf );
    appendSwitchToTextScreenTo( buf );
  }


  @Override
  public void appendSwitchToTextScreenTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "\tLD\tA,(X_M_SCRWIN)\n"
		+ "\tLD\tL,A\n"
		+ "\tCALL\tXSCREEN\n" );
    }
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
		+ "\tCALL\tX_PSET_A\n"
		+ "\tINC\tH\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\t0A8H\n"
		+ "\tRET\tNC\n"
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
		+ "\tJR\tX_PSET_A\n" );
    appendXPSetTo( buf, compiler );
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
		+ "\tPOP\tDE\n"
		+ "\tJR\tC,X_PAINT_LEFT_5\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,(HL)\n"
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
		+ "\tLD\t(HL),B\n"
		+ "\tLD\tC,84H\n"
		+ "\tCALL\tX_PSET_WR_COLOR\n"
		+ "\tDEC\tH\n"
		+ "\tBIT\t0,D\n"
		+ "\tJR\tZ,X_PAINT_LEFT_3\n"
		+ "\tLD\tA,E\n"
		+ "\tINC\tA\n"
		+ "\tJR\tZ,X_PAINT_LEFT_5\n"
		+ "X_PAINT_LEFT_3:\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\tC,01H\n"
		+ "\tJR\tX_PAINT_LEFT_1\n"
		+ "X_PAINT_LEFT_4:\n"
		+ "\tLD\t(HL),B\n"
		+ "\tLD\tC,84H\n"
		+ "\tCALL\tX_PSET_WR_COLOR\n"
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
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,(HL)\n"
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
		+ "\tLD\t(HL),B\n"
		+ "\tLD\tC,84H\n"
		+ "\tCALL\tX_PSET_WR_COLOR\n"
		+ "\tINC\tH\n"
		+ "\tLD\tA,E\n"
		+ "\tCP\t40H\n"
		+ "\tJR\tNZ,X_PAINT_RIGHT_4\n"
		+ "\tLD\tA,D\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_PAINT_RIGHT_6\n"
		+ "X_PAINT_RIGHT_4:\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\tC,80H\n"
		+ "\tJR\tX_PAINT_RIGHT_1\n"
		+ "X_PAINT_RIGHT_5:\n"
		+ "\tLD\t(HL),B\n"
		+ "\tLD\tC,84H\n"
		+ "\tCALL\tX_PSET_WR_COLOR\n"
		+ "X_PAINT_RIGHT_6:\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\t(PAINT_M_X2),DE\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
    appendXPSetTo( buf, compiler );
  }


  /*
   * Farbe eines Pixels ermitteln
   * Parameter:
   *   DE: X-Koordinate (0...319)
   *   HL: Y-Koordinate (0...255)
   * Rueckgabe:
   *   HL >= 0: Farbcode des Pixels
   *   HL=-1:   Pixel exisitiert nicht
   */
  @Override
  public void appendXPointTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPOINT:\tCALL\tX_PINFO\n"
		+ "\tJR\tC,X_POINT_3\n"
    // Farbbyte
		+ "\tDB\t0DDH,0CBH,01H,0C8H\t;SET 1,(IX+01H),B\n"
		+ "\tOUT\t(C),B\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tDB\t0DDH,0CBH,01H,88H\t;RES 1,(IX+01H),B\n"
		+ "\tOUT\t(C),B\n"
    // Pixel auswerten
		+ "\tAND\t(HL)\n"
		+ "\tLD\tA,D\n"
		+ "\tJR\tZ,X_POINT_1\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tAND\t1FH\n"
		+ "\tJR\tX_POINT_2\n"
		+ "X_POINT_1:\n"
		+ "\tAND\t07H\n"
		+ "X_POINT_2:\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "X_POINT_3:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    appendPixUtilTo( buf );
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
		+ "X_PSET_A:\n" );
      if( this.usesX_M_PEN ) {
	buf.append( "\tLD\tD,A\n"
		+ "\tLD\tA,(X_M_PEN)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_PSET_2\n"		// Stift 1 (Normal)
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,X_PSET_1\n"		// Stift 2 (Loeschen)
		+ "\tDEC\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,(HL)\n"		// Stift 3 (XOR-Mode)
		+ "\tXOR\tD\n"
		+ "\tJR\tX_PSET_WR_A\n"
		+ "X_PSET_1:\n"			// Pixel loeschen
		+ "\tLD\tA,D\n"
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tJR\tX_PSET_WR_A\n"
		+ "X_PSET_2:\n"			// Pixel setzen
		+ "\tLD\tA,D\n" );
      }
      buf.append( "X_PSET_OR_A:\n"
		+ "\tOR\t(HL)\n"
		+ "X_PSET_WR_A:\n"
		+ "\tLD\t(HL),A\n"
		+ "X_PSET_WR_COLOR:\n"
		+ "\tDB\t0DDH,0CBH,01H,0C8H\t;SET 1,(IX+01H),B\n"
		+ "\tOUT\t(C),B\n"
		+ "\tLD\tA,(0B7A3H)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDB\t0DDH,0CBH,01H,88H\t;RES 1,(IX+01H),B\n"
		+ "\tOUT\t(C),B\n"
		+ "\tRET\n" );
      appendPixUtilTo( buf );
      this.needsFullWindow = true;
      this.xpsetAppended   = true;
    }
  }


  /*
   * Bildschirmmode einstellen
   *   HL: Screen-Nummer
   *        0: IRM Bank 0
   *        1: IRM Bank 1
   * Rueckgabe:
   *   CY=1: Screen-Nummer nicht unterstuetzt
   */
  @Override
  public void appendXScreenTo( AsmCodeBuf buf )
  {
    if( this.usesScreens ) {
      buf.append( "XSCREEN:\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t0FEH\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,X_SCREEN_2\n"
		+ "\tLD\tB,(IX+01H)\n"
		+ "\tLD\tA,B\n"
		+ "\tXOR\tL\n"
		+ "\tAND\t01H\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,B\n"
		+ "\tBIT\t0,L\n"
		+ "\tLD\tHL,X_M_SCRWIN\n"
		+ "\tJR\tZ,X_SCREEN_1\n"
      // Bank 0 -> 1
		+ "\tOR\t05H\n"
		+ "\tLD\t(IX+01),A\n"
		+ "\tOUT\t(84H),A\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tBIT\t7,A\n"
		+ "\tJR\tZ,X_SCREEN_2\n"
      // Fenster intialisieren
		+ "\tAND\t7FH\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tHL,0000H\n"		// Fensteranfang
		+ "\tLD\tDE,2028H\n"		// Fenstergroesse
		+ "\tLD\tBC,0000H\n"		// bei CAOS 4.1 notwendig
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t3CH\n"
		+ "\tRET\n"
      // Bank 1 -> 0
		+ "X_SCREEN_1:\n"
		+ "\tAND\t0FAH\n"
		+ "\tLD\t(IX+01),A\n"
		+ "\tOUT\t(84H),A\n"
		+ "\tLD\tA,(HL)\n"
      // Fenster in A aufrufen
		+ "X_SCREEN_2:\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t3DH\n"
		+ "\tRET\n"
		+ "X_SCREEN_3:\n"
		+ "\tSCF\n"
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
      if( emuSys instanceof KC85 ) {
	rv = 1;
	if( ((KC85) emuSys).getKCTypeNum() >= 4 ) {
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
    this.usesScreens = false;
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
    return "KC85/4..5 mit Unterst\u00FCtzung beider Bildspeicher";
  }


  @Override
  protected void appendPixUtilTo( AsmCodeBuf buf )
  {
    if( !this.pixUtilAppended ) {
      /*
       * Pruefen der Parameter, einschalten der Pixelebene
       * und ermitteln von Informationen zu einem Pixel
       * 
       * Parameter:
       *   DE: X-Koordinate (0...319)
       *   HL: Y-Koordinate (0...255)
       *
       * Rueckgabe:
       *   CY=1: Pixel ausserhalb des gueltigen Bereichs
       *   A:    Bitmuster mit einem gesetzten Bit,
       *         dass das Pixel in der Speicherzelle beschreibt
       *   C:    84h
       *   HL:   Adresse im Pixel-/Farbspeicher
       */
      buf.append( "X_PINFO:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_PINFO_1\n"
		+ "\tCP\t02H\n"
		+ "\tCCF\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "X_PINFO_1:\n"
		+ "\tLD\tA,L\n"
		+ "\tCPL\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,E\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,80H\n"
		+ "\tJR\tZ,X_PINFO_3\n"
		+ "X_PINFO_2:\n"
		+ "\tSRL\tA\n"
		+ "\tDJNZ\tX_PINFO_2\n"
		+ "X_PINFO_3:\n"
		+ "\tSRL\tD\n"
		+ "\tRR\tE\n"
		+ "\tSRL\tE\n"
		+ "\tSRL\tE\n"
		+ "\tLD\tD,E\n"
		+ "\tLD\tE,00H\n"
		+ "\tADD\tHL,DE\n"
		+ "\tSET\t7,H\n"
		+ "\tDB\t0DDH,0CBH,01H,88H\t;RES 1,(IX+01H),B\n"
		+ "\tLD\tC,84H\n"
		+ "\tOUT\t(C),B\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
      this.pixUtilAppended = true;
    }
  }
}
