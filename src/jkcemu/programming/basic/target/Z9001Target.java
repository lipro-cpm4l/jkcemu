/*
 * (c) 2012-2013 Jens Mueller
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
  protected boolean usesColors;
  protected boolean pixUtilAppended;
  protected boolean xpsetAppended;


  public Z9001Target()
  {
    reset();
  }


  @Override
  public void appendExit( AsmCodeBuf buf )
  {
    buf.append( "\tJP\t0000H\n" );
  }


  @Override
  public void appendHChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0018H\n" );
  }


  @Override
  public void appendHPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0030H\n" );
  }


  @Override
  public void appendInput(
			AsmCodeBuf buf,
			boolean    xckbrk,
			boolean    xinkey,
			boolean    xinch,
			boolean    canBreakOnInput )
  {
    if( xckbrk ) {
      buf.append( "XCKBRK:\n" );
    }
    if( xckbrk || xinkey) {
      buf.append( "XINKEY:\tLD\tC,0BH\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tNC,XINKE2\n"
		+ "XINKE1:\tXOR\tA\n"
		+ "\tRET\n"
		+ "XINKE2:\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tC,01H\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tC,XINKE1\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n" );
      }
      buf.append( "\tRET\n" );
    }
    if( xinch ) {
      buf.append( "XINCH:\tLD\tC,1\n"
		+ "\tCALL\t0005H\n"
		+ "\tJR\tC,XINCH\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n" );
      }
      buf.append( "\tRET\n" );
    }
  }


  @Override
  public void appendMenuItem(
			BasicCompiler compiler,
			AsmCodeBuf    buf,
			String        appName )
  {
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
  public void appendWChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0028H\n" );
  }


  @Override
  public void appendWPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0050H\n" );
  }


  @Override
  public void appendXBORDER( AsmCodeBuf buf )
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
      appendXOUTCH( buf );
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
  public void appendXCOLOR( AsmCodeBuf buf )
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
  public void appendXCURS( AsmCodeBuf buf )
  {
    buf.append( "XCURS:\tLD\tC,1DH\n"		// DCU (Cursor loeschen)
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tZ,XCURS1\n"
		+ "\tINC\tC\n"			// SCU (Cursor setzen)
		+ "XCURS1:\tJP\t0005H\n" );
  }


  @Override
  public void appendXINK( AsmCodeBuf buf )
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
   *         Bit 0: links
   *         Bit 1: rechts
   *         Bit 2: runter
   *         Bit 3: hoch
   *         Bit 4: Aktionsknopf
   */
  public void appendXJOY( AsmCodeBuf buf )
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
  public void appendXLOCATE( AsmCodeBuf buf )
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
  public void appendXLPTCH( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tC,5\n"
		+ "\tLD\tE,A\n"
		+ "\tJP\t0005H\n" );
  }


  @Override
  public void appendXOUTCH( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tLD\tC,2\n"
		+ "\tLD\tE,A\n"
		+ "\tJP\t0005H\n" );
      this.xoutchAppended = true;
    }
  }


  @Override
  public void appendXOUTNL( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tC,02H\n"
		+ "\tLD\tE,0DH\n"
		+ "\tCALL\t0005H\n"
		+ "\tLD\tC,02H\n"
		+ "\tLD\tE,0AH\n"
		+ "\tJP\t0005H\n" );
  }


  @Override
  public void appendXPAPER( AsmCodeBuf buf )
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
   * Zuruecksetzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXPRES( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPRES:\tCALL\tX_PCK\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tX_PST\n" );
    if( this.usesX_MPEN ) {
      buf.append( "\tJR\tXPSET1\n" );
    } else {
      buf.append( "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tAND\tB\n"
		+ "\tJR\tXPSET3\n" );
    }
    appendXPSET( buf, compiler );
  }


  /*
   * Setzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXPSET( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.xpsetAppended ) {
      buf.append( "XPSET:\tCALL\tX_PCK\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tX_PST\n" );
      if( this.usesX_MPEN ) {
        buf.append( "\tLD\tA,(X_MPEN)\n"
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
		+ "\tLD\tHL,X_PST4\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(DE),A\n" );
      if( this.usesColors ) {
	buf.append( "\tLD\tHL,0FC00H\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tA,(0027H)\n"
		+ "\tLD\t(HL),A\n" );
      }
      buf.append( "\tRET\n" );
      appendPixUtil( buf );
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
  public void appendXPTEST( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPTEST:\tCALL\tX_PCK\n"
		+ "\tJR\tNC,X_PTST1\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n"
		+ "X_PTST1:\n"
		+ "\tCALL\tX_PST\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\tC\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n" );
    appendPixUtil( buf );
  }


  /*
   * Target-ID-String
   */
  @Override
  public void appendXTARID( AsmCodeBuf buf )
  {
    buf.append( "XTARID:\tDB\t\'Z9001\'\n"
		+ "\tDB\t00H\n" );
  }


  @Override
  public boolean createsCodeFor( EmuSys emuSys )
  {
    return emuSys != null ? (emuSys instanceof Z9001) : false;
  }


  @Override
  public int get100msLoopCount()
  {
    return 69;
  }


  @Override
  public int getColorBlack()
  {
    return 0;
  }


  @Override
  public int getColorBlinking()
  {
    return 0x08;
  }


  @Override
  public int getColorBlue()
  {
    return 0x04;
  }


  @Override
  public int getColorCyan()
  {
    return 0x06;
  }


  @Override
  public int getColorGreen()
  {
    return 0x02;
  }


  @Override
  public int getColorMagenta()
  {
    return 0x05;
  }


  @Override
  public int getColorRed()
  {
    return 0x01;
  }


  @Override
  public int getColorWhite()
  {
    return 0x07;
  }


  @Override
  public int getColorYellow()
  {
    return 0x03;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x0300;
  }


  @Override
  public int getGraphicScreenNum()
  {
    return 0;
  }


  @Override
  public int getKCNetBaseIOAddr()
  {
    return 0xC0;
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
    this.usesColors      = false;
    this.pixUtilAppended = false;
    this.xpsetAppended   = false;
  }


  @Override
  public boolean supportsAppName()
  {
    return true;
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

  private void appendPixUtil( AsmCodeBuf buf )
  {
    if( !this.pixUtilAppended ) {
      buf.append(
	    /*
	     * Pruefen der Parameter
	     *   DE: X-Koordinate (0...79)
	     *   HL: Y-Koordinate (0...47)
	     * Rueckgabe:
	     *   CY=1: Pixel ausserhalb des gueltigen Bereichs
	     */
		"X_PCK:\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tJR\tZ,X_PCK1\n"
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "X_PCK1:\tLD\tA,4FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,2FH\n"
		+ "\tCP\tL\n"
		+ "\tRET\n"
	    /*
	     * Ermitteln von Informationen zu einem Pixel
	     * Parameter:
	     *   DE: X-Koordinate (0...79)
	     *   HL: Y-Koordinate (0...47)
	     * Rueckgabe:
	     *   B:  Aktuelles Bitmuster (gesetzte Pixel) in der Speicherzelle,
	     *       Bitanordnung innerhalb einer Zeichenposition:
	     *         +---+
	     *         |2 3|
	     *         |0 1|
	     *         +---+
	     *   C:  Bitmuster mit einem gesetzten Bit,
             *       dass das Pixel in der Speicherzelle beschreibt
	     *   HL: Speicherzelle, in der sich das Pixel befindet
	     */
		+ "X_PST:\tLD\tA,01H\n"
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
		+ "\tRET\tC\n"
		+ "\tCP\t0FFH\n"	// 0FFh: 4 gesetzte Pixel
		+ "\tRET\tZ\n"
		+ "\tLD\tB,0\n"
		+ "\tSUB\t0B0H\n"	// < 0B0h: 4 nicht gesetzte Pixel
		+ "\tRET\tC\n"
		+ "\tCP\t0EH\n"		// >= 0BEh: 4 nicht gesetzte Pixel
		+ "\tRET\tNC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,X_PST3\n"
		+ "\tLD\tD,0\n"
		+ "\tLD\tE,A\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tB,A\n"
		+ "\tRET\n"
	    // Umcodierungstabelle von (Zeichen - 0B0H) zu Bitmuster
		+ "X_PST3:\tDB\t04H,08H,02H,01H,05H,0AH,0CH,03H\n"
		+ "\tDB\t06H,09H,0BH,07H,0DH,0EH\n"
	    // Umcodierungstabelle von Bitmuster zu Zeichen
		+ "X_PST4:\tDB\t20H,0B3H,0B2H,0B7H,0B0H,0B4H,0B8H,0BBH\n"
		+ "\tDB\t0B1H,0B9H,0B5H,0BAH,0B6H,0BCH,0BDH,0FFH\n" );
      this.pixUtilAppended = true;
    }
  }
}
