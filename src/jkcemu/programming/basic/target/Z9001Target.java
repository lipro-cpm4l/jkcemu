/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Z9001-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Simulation einer 80x48-Pixel Vollgrafik
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import java.util.Set;
import jkcemu.base.EmuSys;
import jkcemu.emusys.Z9001;
import jkcemu.programming.basic.*;


public class Z9001Target extends AbstractTarget
{
  public static final String BASIC_TARGET_NAME = "TARGET_Z9001";

  protected boolean usesColors;
  protected boolean pixUtilAppended;
  protected boolean xpsetAppended;

  private boolean usesX_M_INKEY;


  public Z9001Target()
  {
    setNamedValue( "GRAPHICSCREEN", 0 );
    setNamedValue( "BLACK", 0 );
    setNamedValue( "BLINKING", 0x08 );
    setNamedValue( "BLUE", 0x04 );
    setNamedValue( "CYAN", 0x06 );
    setNamedValue( "GREEN", 0x02 );
    setNamedValue( "MAGENTA", 0x05 );
    setNamedValue( "RED", 0x01 );
    setNamedValue( "WHITE", 0x07 );
    setNamedValue( "YELLOW", 0x03 );
    setNamedValue( "JOYST_LEFT", 0x01 );
    setNamedValue( "JOYST_RIGHT", 0x02 );
    setNamedValue( "JOYST_DOWN", 0x04 );
    setNamedValue( "JOYST_UP", 0x08 );
    setNamedValue( "JOYST_BUTTON1", 0x10 );
  }


  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    if( this.usesX_M_INKEY ) {
      buf.append( "X_M_INKEY:\n"
		+ "\tDS\t1\n" );
    }
  }


  @Override
  public void appendExitTo( AsmCodeBuf buf )
  {
    buf.append( "\tJP\t0000H\n" );
  }


  @Override
  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0018H\n" );
  }


  @Override
  public void appendHPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0030H\n" );
  }


  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    if( this.usesX_M_INKEY ) {
      buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_INKEY),A\n" );
    }
  }


  @Override
  public void appendInputTo(
			AsmCodeBuf buf,
			boolean    xckbrk,
			boolean    xinkey,
			boolean    xinch,
			boolean    canBreakOnInput )
  {
    if( xckbrk ) {
      buf.append( "XCKBRK:\n" );
      if( xinkey ) {
	buf.append( "\tCALL\tXINKE1\n"
		+ "\tRET\tZ\n"
		+ "\tLD\t(X_M_INKEY),A\n"
		+ "\tRET\n"
		+ "XINKEY:\tLD\tA,(X_M_INKEY)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XINKE1\n"
		+ "\tPUSH\tAF\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_M_INKEY),A\n"
		+ "\tPOP\tAF\n"
		+ "\tRET\n" );
	this.usesX_M_INKEY = true;
      }
      buf.append( "XINKE1:\tLD\tC,0BH\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tC,XINKE2\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tC,01H\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tC,XINKE2\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tOR\tA\n"
		+ "\tRET\n"
		+ "XINKE2:\tXOR\tA\n"
		+ "\tRET\n" );
    } else {
      if( xinkey ) {
	buf.append( "XINKEY:\tLD\tC,0BH\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tC,XINKE1\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tC,01H\n"
		+ "\tCALL\t0005H\n" );
	if( canBreakOnInput ) {
	  buf.append( "\tJR\tC,XINKE1\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
	} else {
	  buf.append( "\tRET\tNC\n" );
	}
	buf.append( "XINKE1:\tXOR\tA\n"
		+ "\tRET\n" );
      }
    }
    if( xinch ) {
      buf.append( "XINCH:\n" );
      if( this.usesX_M_INKEY ) {
	buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_INKEY),A\n" );
      }
      buf.append( "XINCH1:\tLD\tC,01H\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tC,XINCH1\n" );
      if( xckbrk || canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tRET\tNZ\n"
		+ "\tJR\tXBREAK\n" );
      } else {
	buf.append( "\tRET\n" );
      }
    }
  }


  @Override
  public void appendPrologTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler,
			String        appName )
  {
    buf.append( "\tJP\t" );
    buf.append( BasicCompiler.START_LABEL );
    buf.newLine();
    if( appName == null ) {
      appName = "";
    }
    int len = appName.length();
    if( len < 8 ) {
      StringBuilder tmpBuf = new StringBuilder( 8 );
      tmpBuf.append( appName );
      for( int i = len; i < 8; i++ ) {
	tmpBuf.append( (char) '\u0020' );
      }
      appName = tmpBuf.toString();
    } else if( len > 8 ) {
      appName = appName.substring( 0, 8 );
    }
    buf.appendStringLiteral( appName );
    buf.append( "\tDB\t00H\n" );
    if( appName.trim().isEmpty() ) {
      compiler.putWarning(
		"Programm kann auf dem Zielsystem nicht aufgerufen werden,"
				+ " da der Programmname leer ist." );
    }
  }


  @Override
  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0028H\n" );
  }


  @Override
  public void appendWPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0050H\n" );
  }


  @Override
  public void appendXBorderTo( AsmCodeBuf buf )
  {
    buf.append( "XBORDER:\n"
		+ "\tLD\tA,05H\n"		// Code fuer Randfarbe
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,L\n" );
    if( this.xoutchAppended ) {
      buf.append( "\tJR\tXOUTCH\n" );
    } else {
      appendXOutchTo( buf );
    }
    this.usesColors = true;
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
    buf.append( "XCOLOR:\tLD\tA,L\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tAND\t0F0H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,E\n"
		+ "\tAND\t07H\n"
		+ "\tOR\tB\n"
		+ "\tLD\t(0027H),A\n"
		+ "\tRET\n" );
    this.usesColors = true;
  }


  @Override
  public void appendXCursTo( AsmCodeBuf buf )
  {
    buf.append( "XCURS:\tLD\tC,1DH\n"		// DCU (Cursor loeschen)
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,XCURS1\n"
		+ "\tINC\tC\n"			// SCU (Cursor setzen)
		+ "XCURS1:\tJP\t0005H\n" );
  }


  @Override
  public void appendXInkTo( AsmCodeBuf buf )
  {
    buf.append( "XINK:\tLD\tA,L\n"
                + "\tSLA\tA\n"
                + "\tSLA\tA\n"
                + "\tSLA\tA\n"
                + "\tSLA\tA\n"
                + "\tAND\t0F0H\n"
                + "\tLD\tD,A\n"
                + "\tLD\tA,(0027H)\n"
                + "\tAND\t07H\n"
                + "\tOR\tD\n"
                + "\tLD\t(0027H),A\n"
		+ "\tRET\n" );
    this.usesColors = true;
  }


  /*
   * Abfrage des Joystick-Status
   * Parameter:
   *   HL: Nummer des Joysticks (0: erster Joystick)
   * Rueckgabewert:
   *   HL: Joystickstatus
   */
  public void appendXJoyTo( AsmCodeBuf buf )
  {
    buf.append( "XJOY:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,XJOY3\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,XJOY1\n"
		+ "\tLD\tA,(0013H)\n"
		+ "\tJR\tXJOY2\n"
		+ "XJOY1:\tCP\t01H\n"
		+ "\tJR\tNZ,XJOY3\n"
		+ "\tLD\tA,(0014H)\n"
		+ "XJOY2:\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "XJOY3:\tLD\tHL,0000H\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXLocateTo( AsmCodeBuf buf )
  {
    buf.append( "XLOCATE:\n"
		+ "\tLD\tC,18\n"
		+ "\tLD\tD,E\n"
		+ "\tINC\tD\n"
		+ "\tLD\tE,L\n"
		+ "\tINC\tE\n"
		+ "\tJP\t0005H\n" );
  }


  @Override
  public void appendXLPtchTo( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tC,5\n"
		+ "\tLD\tE,A\n"
		+ "\tJP\t0005H\n" );
  }


  @Override
  public void appendXOutchTo( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tLD\tC,2\n"
		+ "\tLD\tE,A\n"
		+ "\tJP\t0005H\n" );
      this.xoutchAppended = true;
    }
  }


  @Override
  public void appendXOutnlTo( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tC,02H\n"
		+ "\tLD\tE,0DH\n"
		+ "\tCALL\t0005H\n"
		+ "\tLD\tC,02H\n"
		+ "\tLD\tE,0AH\n"
		+ "\tJP\t0005H\n" );
  }


  @Override
  public void appendXPaperTo( AsmCodeBuf buf )
  {
    buf.append( "XPAPER:\tLD\tA,L\n"
                + "\tAND\t07H\n"
                + "\tLD\tD,A\n"
                + "\tLD\tA,(0027H)\n"
                + "\tAND\t0F0H\n"
                + "\tOR\tD\n"
                + "\tLD\t(0027H),A\n"
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
    buf.append( "XPAINT:\tCALL\tX_PST\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\tC\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tCALL\tXPSET2\n"
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
  public void appendXPointTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPOINT:\tCALL\tX_PST\n"
		+ "\tJR\tC,XPOINT2\n"
    // Farbbyte lesen
		+ "\tRES\t2,H\n"
		+ "\tLD\tD,(HL)\n"
    // Pixel auswerten
		+ "\tLD\tA,B\n"
		+ "\tAND\tC\n"
		+ "\tLD\tA,D\n"
		+ "\tLD\tE,07H\n"
		+ "\tJR\tZ,XPOINT1\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tLD\tE,0FH\n"
		+ "XPOINT1:\n"
		+ "\tAND\tE\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "XPOINT2:\n"
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
    buf.append( "XPRES:\tCALL\tX_PST\n"
		+ "\tRET\tC\n" );
    if( this.usesX_M_PEN ) {
      buf.append( "\tJR\tXPSET1\n" );
    } else {
      buf.append( "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tAND\tB\n"
		+ "\tJR\tXPSET3\n" );
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
      buf.append( "XPSET:\tCALL\tX_PST\n"
		+ "\tRET\tC\n" );
      if( this.usesX_M_PEN ) {
        buf.append( "\tLD\tA,(X_M_PEN)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XPSET2\n"		// Stift 1 (Normal)
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XPSET1\n"		// Stift 2 (Loeschen)
		+ "\tDEC\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,B\n"			// Stift 3 (XOR-Mode)
		+ "\tXOR\tC\n"
		+ "\tJR\tXPSET3\n"
		+ "XPSET1:\tLD\tA,C\n"		// Pixel loeschen
		+ "\tCPL\n"
		+ "\tAND\tB\n"
		+ "\tJR\tXPSET3\n" );
      }
      buf.append( "XPSET2:\tLD\tA,B\n"		// Pixel setzen
		+ "\tOR\tC\n"
		+ "XPSET3:\tEX\tDE,HL\n"
		+ "\tLD\tB,0\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tHL,XPSET_TAB\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(DE),A\n" );
      if( this.usesColors ) {
	buf.append( "\tRES\t2,D\n"
		+ "\tLD\tA,(0027H)\n"
		+ "\tLD\t(DE),A\n" );
      }
      buf.append( "\tRET\n"
      // Umcodierungstabelle von Bitmuster zu Zeichen
		+ "XPSET_TAB:\n"
		+ "\tDB\t20H,0B3H,0B2H,0B7H,0B0H,0B4H,0B8H,0BBH\n"
		+ "\tDB\t0B1H,0B9H,0B5H,0BAH,0B6H,0BCH,0BDH,0FFH\n" );
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
   *   HL=0: Pixel nicht gesetzt
   *   HL=1: Pixel gesetzt
   */
  @Override
  public void appendXPTestTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPTEST:\tCALL\tX_PST\n"
		+ "\tJR\tC,X_PTEST1\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\tC\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
		+ "X_PTEST1:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    appendPixUtilTo( buf );
  }


  @Override
  public int get100msLoopCount()
  {
    return 69;
  }


  @Override
  public String[] getBasicTargetNames()
  {
    return new String[] { BASIC_TARGET_NAME };
  }


  @Override
  public int getCompatibilityLevel( EmuSys emuSys )
  {
    int rv = 0;
    if( emuSys != null ) {
      if( emuSys instanceof Z9001 ) {
        rv = 3;
      }
    }
    return rv;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x0300;
  }


  @Override
  public String getHostName()
  {
    return "Z9001";
  }


  @Override
  public int getMaxAppNameLen()
  {
    return 8;
  }


  @Override
  public String getStartCmd( EmuSys emuSys, String appName, int begAdAdr )
  {
    String rv = null;
    if( (emuSys != null) && (appName != null) ) {
      if( emuSys instanceof Z9001 ) {
	if( appName.length() > 8 ) {
	  appName = appName.substring( 0, 8 );
	}
	rv = appName;
      }
    }
    return rv;
  }


  @Override
  public int[] getVdipBaseIOAddresses()
  {
    return new int[] { 0xDC };
  }


  @Override
  public void reset()
  {
    super.reset();
    this.usesX_M_INKEY   = false;
    this.usesColors      = false;
    this.pixUtilAppended = false;
    this.xpsetAppended   = false;
  }


  @Override
  public boolean supportsBorderColor()
  {
    return true;
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
  public boolean supportsXCURS()
  {
    return true;
  }


  @Override
  public boolean supportsXJOY()
  {
    return true;
  }


  @Override
  public boolean supportsXLOCAT()
  {
    return true;
  }


  @Override
  public boolean supportsXLPTCH()
  {
    return true;
  }


  @Override
  public String toString()
  {
    return "KC85/1, KC87, Z9001";
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
       *   DE: X-Koordinate (0...79)
       *   HL: Y-Koordinate (0...47)
       *
       * Rueckgabe:
       *   CY=1: Pixel ausserhalb des gueltigen Bereichs
       *   B:    Aktuelles Bitmuster (gesetzte Pixel) in der Speicherzelle,
       *         Bitanordnung innerhalb einer Zeichenposition:
       *           +---+
       *           |2 3|
       *           |0 1|
       *           +---+
       *   C:    Bitmuster mit einem gesetzten Bit,
       *         dass das Pixel in der Speicherzelle beschreibt
       *   HL:   Speicherzelle, in der sich das Pixel befindet
       */
      buf.append( "X_PST:\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,X_PST3\n"
		+ "\tLD\tA,4FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,2FH\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,01H\n"
		+ "\tSRL\tL\n"
		+ "\tJR\tNC,X_PST1\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "X_PST1:\tSRL\tE\n"
		+ "\tJR\tNC,X_PST2\n"
		+ "\tSLA\tA\n"
		+ "X_PST2:\tEX\tDE,HL\n"
		+ "\tLD\tBC,0EF98H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tLD\tB,H\n"
		+ "\tLD\tC,L\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tC,A\n"
		+ "\tXOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\tB,0FH\n"
		+ "\tCP\t20H\n"
		+ "\tCP\t0EH\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n"
		+ "\tCP\t0FFH\n"	// 0FFh: 4 gesetzte Pixel
		+ "\tRET\tZ\n"
		+ "\tLD\tB,0\n"
		+ "\tSUB\t0B0H\n"	// < 0B0h: 4 nicht gesetzte Pixel
		+ "\tCCF\n"
		+ "\tRET\tNC\n"
		+ "\tCP\t0EH\n"		// >= 0BEh: 4 nicht gesetzte Pixel
		+ "\tRET\tNC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,X_PST_TAB\n"
		+ "\tLD\tD,0\n"
		+ "\tLD\tE,A\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tB,A\n"
		+ "\tOR\tA\n"		// CY=0
		+ "\tRET\n"
		+ "X_PST3:\tSCF\n"
		+ "\tRET\n"
      // Umcodierungstabelle von (Zeichen - 0B0H) zu Bitmuster
		+ "X_PST_TAB:\n"
		+ "\tDB\t04H,08H,02H,01H,05H,0AH,0CH,03H\n"
		+ "\tDB\t06H,09H,0BH,07H,0DH,0EH\n" );
      this.pixUtilAppended = true;
    }
  }
}
