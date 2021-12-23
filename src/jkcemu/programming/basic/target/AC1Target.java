/*
 * (c) 2013-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * AC1-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Simulation einer 128x64-Pixel Vollgrafik und Farbunterstuetzung
 */

package jkcemu.programming.basic.target;

import jkcemu.base.EmuSys;
import jkcemu.emusys.AC1;
import jkcemu.emusys.ac1_llc2.AbstractSCCHSys;
import jkcemu.programming.basic.AsmCodeBuf;
import jkcemu.programming.basic.BasicCompiler;


public class AC1Target extends SCCHTarget
{
  public static final String BASIC_TARGET_NAME = "TARGET_AC1";

  private boolean usesColors;
  private boolean pixUtilAppended;
  private boolean xclsAppended;
  private boolean xpsetAppended;


  public AC1Target()
  {
    setNamedValue( "GRAPHICSCREEN", 0 );
    setNamedValue( "BLACK", 0 );
    setNamedValue( "BLUE", 0x04 );
    setNamedValue( "CYAN", 0x06 );
    setNamedValue( "GREEN", 0x02 );
    setNamedValue( "MAGENTA", 0x05 );
    setNamedValue( "RED", 0x01 );
    setNamedValue( "WHITE", 0x07 );
    setNamedValue( "YELLOW", 0x03 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    if( this.usesColors ) {
      buf.append( "X_M_COLOR_STATUS:\tDS\t1\n"		// 0: keine Farbe
		+ "X_M_COLOR_VALUE:\tDS\t1\n" );	// Farbwert
    }
  }


  @Override
  public void appendHPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0040H\n" );
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    if( this.usesColors ) {
      /*
       * B=0:    erstmal keine Farbe
       * danach: Test, ob Farbkarte vorhanden ist
       */
      buf.append( "\tLD\tBC,00F0H\n"
		+ "\tIN\tE,(C)\n"
		+ "\tSET\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tIN\tD,(C)\n"
		+ "\tRES\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tIN\tA,(C)\n"
		+ "\tXOR\tD\n"
		+ "\tAND\t04H\n"
		+ "\tJR\tZ,X_INIT_1\n"
		+ "\tLD\tB,0FH\n"		// weiss auf schwarz
      // Farbbyte lesen
		+ "\tLD\tA,(181FH)\n"
      // Vorder- und Hintergrundfarbe gleich?
		+ "\tLD\tD,A\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tXOR\tD\n"
		+ "\tAND\t0F0H\n"
		+ "\tJR\tZ,X_INIT_1\n"
		+ "\tLD\tB,D\n"
      // Farbbyte uebernehmen
		+ "X_INIT_1:\n"
		+ "\tLD\tA,B\n"
		+ "\tLD\t(X_M_COLOR_STATUS),A\n"
		+ "\tLD\t(X_M_COLOR_VALUE),A\n" );
    }
  }


  @Override
  public void appendWPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0080H\n" );
  }


  @Override
  public void appendXClsTo( AsmCodeBuf buf )
  {
    if( !this.xclsAppended ) {
      buf.append( "XCLS:" );
      if( this.usesColors ) {
	buf.append( "\tLD\tA,(X_M_COLOR_STATUS)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_CLS_1\n"
		+ "\tIN\tA,(0F0H)\n"
		+ "\tOR\t04H\n"
		+ "\tOUT\t(0F0H),A\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tLD\tA,(X_M_COLOR_VALUE)\n"
		+ "\tLD\tHL,1000H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tDE,1001H\n"
		+ "\tLD\tBC,07FFH\n"
		+ "\tLDIR\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tAND\t0FBH\n"
		+ "\tOUT\t(0F0H),A\n"
		+ "X_CLS_1:\n"
		+ "\tLD\tA,20H\n"
		+ "\tLD\tHL,1000H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tDE,1001H\n"
		+ "\tLD\tBC,07FFH\n"
		+ "\tLDIR\n"
		+ "\tLD\t(1800H),HL\n"
		+ "\tLD\t(1846H),A\n"
		+ "\tRET\n" );
      } else {
	buf.append( "\tLD\tA,0CH\n" );
	if( this.xOutchAppended ) {
	  buf.append( "\tJR\tXOUTCH\n" );
	} else {
	  appendXOutchTo( buf );
	}
      }
      this.xclsAppended = true;
    }
  }


  /*
   * Gleichzeitiges Setzen der Vorder- und Hintergrundfarbe
   * Parameter:
   *   HL: Vordergrundfarbe
   *   DE: Hintergrundfarbe
   */
  @Override
  public void appendXColorTo( AsmCodeBuf buf )
  {
    buf.append( "XCOLOR:\tLD\tA,E\n"
		+ "\tSLA\tA\n"
                + "\tSLA\tA\n"
                + "\tSLA\tA\n"
                + "\tSLA\tA\n"
                + "\tAND\t0F0H\n"
                + "\tLD\tB,A\n"
                + "\tLD\tA,L\n"
                + "\tAND\t0FH\n"
                + "\tOR\tB\n"
                + "\tLD\t(X_M_COLOR_VALUE),A\n"
                + "\tRET\n" );
    this.usesColors = true;
  }


  @Override
  public void appendXInkTo( AsmCodeBuf buf )
  {
    buf.append( "XINK:\tLD\tA,(X_M_COLOR_VALUE)\n"
		+ "\tAND\t0F0H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t0FH\n"
		+ "\tOR\tB\n"
		+ "\tLD\t(X_M_COLOR_VALUE),A\n"
		+ "\tRET\n" );
    this.usesColors = true;
  }


  /*
   * Ermittlung der Cursor-Position
   * Rueckgabe:
   *   HL: Bit 0..5:  Spalte
   *       Bit 6..10: Zeile
   *   CY=1: Fehler -> HL=0FFFFH
   */
  @Override
  protected void appendXGetCrsPosTo( AsmCodeBuf buf )
  {
    if( !this.xGetCrsPosAppended ) {
      buf.append( "X_GET_CRS_POS:\n"
		+ "\tLD\tDE,(1800H)\n"
		+ "\tLD\tHL,17FFH\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tJR\tC,X_GET_CRS_POS_1\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t0F8H\n"
		+ "\tRET\tZ\n"			// CY=0
		+ "X_GET_CRS_POS_1:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
      this.xGetCrsPosAppended = true;
    }
  }


  /*
   * Setzen der Cursor-Position auf dem Bildschirm
   * Parameter:
   *   DE: Zeile, >= 0
   *   HL: Spalte, >= 0
   */
  @Override
  public void appendXLocateTo( AsmCodeBuf buf )
  {
    if( this.usesColors ) {
      buf.append( "XLOCATE:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tD\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,1FH\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(1800H)\n"
		+ "\tLD\tA,(1846H)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tPOP\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tDE,17FFH\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\t(1800H),HL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(1846H),A\n"
		+ "\tRET\n" );
    } else {
      super.appendXLocateTo( buf );
    }
  }


  @Override
  public void appendXOutchTo( AsmCodeBuf buf )
  {
    if( !this.xOutchAppended ) {
      if( this.usesColors ) {
	buf.append( "XOUTCH:\tLD\tD,A\n"
		+ "\tLD\tA,(1846H)\n"
		+ "\tLD\tHL,(1800H)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tC,0F0H\n"
		+ "\tIN\tE,(C)\n"
		+ "\tLD\tA,D\n"
		+ "\tCP\t08H\n"
		+ "\tJR\tZ,X_OUTCH_7\n"
		+ "\tCP\t09H\n"
		+ "\tJR\tZ,X_OUTCH_1\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tZ,X_OUTCH_8\n"
		+ "\tCP\t0BH\n"
		+ "\tJR\tZ,X_OUTCH_9\n"
		+ "\tCP\t0CH\n"
		+ "\tJP\tZ,XCLS\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,X_OUTCH_10\n"
	// Schreiben des Zeichens in D auf die Adresse in HL mit Farbausgabe
		+ "\tSET\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\tA,(X_M_COLOR_VALUE)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRES\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\t(HL),D\n"
	// Cursor eins weiter
		+ "X_OUTCH_1:\n"
		+ "\tDEC\tHL\n"
	// ggf. scrollen
		+ "X_OUTCH_2:\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\t10H\n"
		+ "\tCALL\tC,X_OUTCH_3\n"
	// Cursor setzen
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(1846H),A\n"
		+ "\tLD\t(1800H),HL\n"
		+ "\tRET\n"
	// Bildschirm scrollen
		+ "X_OUTCH_3:\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,(X_M_COLOR_STATUS)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_OUTCH_4\n"
		+ "\tSET\t2,E\n"
		+ "\tLD\tA,(X_M_COLOR_VALUE)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_OUTCH_5\n"
		+ "\tPOP\tDE\n"
		+ "\tRES\t2,E\n"
		+ "X_OUTCH_4:\n"
		+ "\tLD\tA,20H\n"
		+ "\tCALL\tX_OUTCH_5\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC,0040H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tRET\n"
		+ "X_OUTCH_5:\n"
		+ "\tLD\tC,0F0H\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\tHL,17BFH\n"
		+ "\tLD\tDE,17FFH\n"
		+ "\tLD\tBC,07C0H\n"
		+ "\tLDDR\n"
		+ "\tLD\tB,40H\n"
		+ "X_OUTCH_6:\n"
		+ "\tLD\t(DE),A\n"
		+ "\tDEC\tDE\n"
		+ "\tDJNZ\tX_OUTCH_6\n"
		+ "\tRET\n"
		+ "X_OUTCH_7:\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\t18H\n"
		+ "\tJR\tC,X_OUTCH_2\n"
		+ "\tLD\tHL,1000H\n"
		+ "\tJR\tX_OUTCH_2\n"
		+ "X_OUTCH_8:\n"
		+ "\tLD\tBC,0040H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tX_OUTCH_2\n"
		+ "X_OUTCH_9:\n"
		+ "\tLD\tBC,0040H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\t18H\n"
		+ "\tJR\tC,X_OUTCH_2\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t3FH\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,10H\n"
		+ "\tJR\tX_OUTCH_2\n"
		+ "X_OUTCH_10:\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t0C0H\n"
		+ "\tLD\tL,A\n"
		+ "\tDEC\tHL\n"
		+ "\tJR\tX_OUTCH_2\n" );
	if( !this.xclsAppended ) {
	  appendXClsTo( buf );
	}
      } else {
	super.appendXOutchTo( buf );
      }
      this.xOutchAppended = true;
    }
  }


  @Override
  public void appendXOutnlTo( AsmCodeBuf buf )
  {
    if( this.usesColors ) {
      buf.append( "XOUTNL:\tLD\tA,0DH\n"
		+ "\tJP\tXOUTCH\n" );
    } else {
      super.appendXOutnlTo( buf );
    }
  }


  @Override
  public void appendXPaperTo( AsmCodeBuf buf )
  {
    buf.append( "XPAPER:\tLD\tA,L\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tAND\t0F0H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,(X_M_COLOR_VALUE)\n"
		+ "\tAND\t0FH\n"
		+ "\tOR\tB\n"
		+ "\tLD\t(X_M_COLOR_VALUE),A\n"
		+ "\tRET\n" );
    this.usesColors = true;
  }


  /*
   * Setzen eines Pixels ohne Beruecksichtigung des eingestellten Stiftes
   * bei gleichzeitigem Test, ob dieser schon gesetzt ist
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   * Rueckgabe:
   *   CY=1: Pixel bereits gesetzt oder ausserhalb des sichtbaren Bereichs
   */
  @Override
  public void appendXPaintTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPAINT:\tCALL\tX_PINFO\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\tC\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tCALL\tX_PSET_2\n"
		+ "\tOR\tA\n"		// CY=0
		+ "\tRET\n" );
    appendXPSetTo( buf, compiler );
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
    buf.append( "XPOINT:\tCALL\tX_PINFO\n"
		+ "\tJR\tC,X_POINT_2\n"
    // Farbbyte lesen
		+ "\tIN\tA,(0F0H)\n"
		+ "\tOR\t04H\n"
		+ "\tOUT\t(0F0H),A\n"
		+ "\tLD\tD,(HL)\n"
		+ "\tAND\t0FBH\n"
		+ "\tOUT\t(0F0H),A\n"
    // Pixel auswerten
		+ "\tLD\tA,B\n"
		+ "\tAND\tC\n"
		+ "\tLD\tA,D\n"
		+ "\tJR\tNZ,X_POINT_1\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "X_POINT_1:\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "X_POINT_2:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    appendPixUtilTo( buf );
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
      buf.append( "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tAND\tB\n"
		+ "\tJR\tX_PSET_3\n" );
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
		+ "\tRET\tC\n" );
      if( this.usesX_M_PEN ) {
	buf.append( "\tLD\tA,(X_M_PEN)\n"
                + "\tDEC\tA\n"
                + "\tJR\tZ,X_PSET_2\n"		// Stift 1 (Normal)
                + "\tDEC\tA\n"
                + "\tJR\tZ,X_PSET_1\n"		// Stift 2 (Loeschen)
                + "\tDEC\tA\n"
                + "\tRET\tNZ\n"
		+ "\tLD\tA,B\n"			// Stift 3 (XOR-Mode)
		+ "\tXOR\tC\n"
		+ "\tJR\tX_PSET_3\n"
		+ "X_PSET_1:\n"			// Pixel loeschen
		+ "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tAND\tB\n"
		+ "\tJR\tX_PSET_3\n" );
      }
      buf.append( "X_PSET_2:\n"			// Pixel setzen
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "X_PSET_3:\n"
		+ "\tAND\t0FH\n" );
      if( this.usesColors ) {
	buf.append( "\tLD\tD,A\n"
		+ "\tLD\tC,0F0H\n"
		+ "\tIN\tE,(C)\n"
		+ "\tSET\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\tA,(X_M_COLOR_VALUE)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRES\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\t(HL),D\n" );
      } else {
	buf.append( "\tLD\t(HL),A\n" );
      }
      buf.append( "\tRET\n" );
      appendPixUtilTo( buf );
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
    buf.append( "XPTEST:\tCALL\tX_PINFO\n"
		+ "\tJR\tC,X_PTEST_1\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\tC\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
		+ "X_PTEST_1:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    appendPixUtilTo( buf );
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
      if( emuSys instanceof AC1 ) {
	rv = 1;
	if( ((AC1) emuSys).emulates2010Mode()
	    || ((AC1) emuSys).emulatesSCCHMode() )
	{
	  rv = 2;
	}
	if( emuSys.getColorCount() > 2 ) {
	  rv = 3;
	}
      } else if( emuSys instanceof AbstractSCCHSys ) {
	rv = 1;
      }
    }
    return rv;
  }


  @Override
  public int[] getVdipBaseIOAddresses()
  {
    return new int[] { 0xDC, 0xFC, 0x08 };
  }


  @Override
  public void reset()
  {
    super.reset();
    this.usesColors      = false;
    this.pixUtilAppended = false;
    this.xclsAppended    = false;
    this.xpsetAppended   = false;
  }


  @Override
  public boolean supportsColors()
  {
    return true;
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
  public boolean supportsXLOCATE()
  {
    return this.usesColors ? true : super.supportsXLOCATE();
  }


  @Override
  public String toString()
  {
    return "AC1-2010, AC1-SCCH mit optionaler Farbgrafikkarte";
  }


	/* --- private Methoden --- */

  private void appendPixUtilTo( AsmCodeBuf buf )
  {
    if( !this.pixUtilAppended ) {
      /*
       * Pruefen der Parameter und
       * ermitteln von Informationen zu einem Pixel
       *
       * Parameter:
       *   DE: X-Koordinate (0...127)
       *   HL: Y-Koordinate (0...63)
       *
       * Rueckgabe:
       *   CY=1: Pixel ausserhalb des gueltigen Bereichs
       *   B:    Aktuelles Bitmuster (gesetzte Pixel) in der Speicherzelle,
       *         Bitanordnung innerhalb einer Zeichenposition:
       *           +---+
       *           |0 1|
       *           |2 3|
       *           +---+
       *   C:    Bitmuster mit einem gesetzten Bit,
       *         dass das Pixel in der Speicherzelle beschreibt
       *   HL:   Speicherzelle, in der sich das Pixel befindet
       */
      buf.append( "X_PINFO:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,7FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,01H\n"
		+ "\tSRL\tL\n"
		+ "\tJR\tC,X_PINFO_1\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "X_PINFO_1:\n"
		+ "\tSRL\tE\n"
		+ "\tJR\tNC,X_PINFO_2\n"
		+ "\tSLA\tA\n"
		+ "X_PINFO_2:\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tLD\tBC,103FH\n"
		+ "\tADD\tHL,BC\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,00H\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t10H\n"
		+ "\tRET\tNC\n"
		+ "\tLD\tB,A\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
      this.pixUtilAppended = true;
    }
  }
}
