/*
 * (c) 2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * AC1-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Simulation einer 128x64-Pixel Vollgrafik und Farbunterstuetzung
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import jkcemu.base.EmuSys;
import jkcemu.emusys.AC1;
import jkcemu.programming.basic.*;


public class AC1Target extends SCCHTarget
{
  private boolean usesColors;
  private boolean pixUtilAppended;
  private boolean xclsAppended;
  private boolean xpsetAppended;


  public AC1Target()
  {
    reset();
  }


  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    if( this.usesColors ) {
      buf.append( "X_MCOS:\tDS\t1\n"		// Status: 0 = keine Farbe
		+ "X_MCOV:\tDS\t1\n" );		// Farbwert
    }
  }


  @Override
  public void appendXCLS( AsmCodeBuf buf )
  {
    if( !this.xclsAppended ) {
      buf.append( "XCLS:" );
      if( this.usesColors ) {
	buf.append( "\tLD\tA,(X_MCOS)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XCOL1\n"
		+ "\tIN\tA,(0F0H)\n"
		+ "\tOR\t04H\n"
		+ "\tOUT\t(0F0H),A\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tLD\tA,(X_MCOV)\n"
		+ "\tLD\tHL,1000H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tDE,1001H\n"
		+ "\tLD\tBC,07FFH\n"
		+ "\tLDIR\n"
		+ "\tEX\tAF,AF\'\n"
		+ "\tAND\t0FBH\n"
		+ "\tOUT\t(0F0H),A\n"
		+ "XCOL1:\tLD\tA,20H\n"
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
	if( this.xoutchAppended ) {
	  buf.append( "\tJR\tXOUTCH\n" );
	} else {
	  appendXOUTCH( buf );
	}
      }
      this.xclsAppended = true;
    }
  }


  @Override
  public void appendHPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0040H\n" );
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    if( this.usesColors ) {
      buf.append( "\tLD\tB,00H\n"		// erstmal keine Farbe
      // Farbkarte vorhanden?
		+ "\tLD\tC,0F0H\n"
		+ "\tIN\tE,(C)\n"
		+ "\tSET\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tIN\tD,(C)\n"
		+ "\tRES\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tIN\tA,(C)\n"
		+ "\tXOR\tD\n"
		+ "\tAND\t04H\n"
		+ "\tJR\tZ,X_INIT1\n"
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
		+ "\tJR\tZ,X_INIT1\n"
		+ "\tLD\tB,D\n"
      // Farbbyte uebernehmen
		+ "X_INIT1:\n"
		+ "\tLD\tA,B\n"
		+ "\tLD\t(X_MCOS),A\n"
		+ "\tLD\t(X_MCOV),A\n" );
    }
  }


  @Override
  public void appendWPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0080H\n" );
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
                + "\tLD\t(X_MCOV),A\n"
                + "\tRET\n" );
    this.usesColors = true;
  }


  @Override
  public void appendXINK( AsmCodeBuf buf )
  {
    buf.append( "XINK:\tLD\tA,(X_MCOV)\n"
		+ "\tAND\t0F0H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t0FH\n"
		+ "\tOR\tB\n"
		+ "\tLD\t(X_MCOV),A\n"
		+ "\tRET\n" );
    this.usesColors = true;
  }


  /*
   * Setzen der Cursor-Position auf dem Bildschirm
   * Parameter:
   *   DE: Zeile, >= 0
   *   HL: Spalte, >= 0
   */
  @Override
  public void appendXLOCATE( AsmCodeBuf buf )
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
      super.appendXLOCATE( buf );
    }
  }


  @Override
  public void appendXOUTCH( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      if( this.usesColors ) {
	buf.append( "XOUTCH:\tLD\tD,A\n"
		+ "\tLD\tA,(1846H)\n"
		+ "\tLD\tHL,(1800H)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tC,0F0H\n"
		+ "\tIN\tE,(C)\n"
		+ "\tLD\tA,D\n"
		+ "\tCP\t08H\n"
		+ "\tJR\tZ,XOUTC7\n"
		+ "\tCP\t09H\n"
		+ "\tJR\tZ,XOUTC1\n"
		+ "\tCP\t0AH\n"
		+ "\tJR\tZ,XOUTC8\n"
		+ "\tCP\t0BH\n"
		+ "\tJR\tZ,XOUTC9\n"
		+ "\tCP\t0CH\n"
		+ "\tJP\tZ,XCLS\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,XOUTC10\n"
	// Schreiben der Zeichens in D auf die Adresse in HL mit Farbausgabe
		+ "\tSET\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\tA,(X_MCOV)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRES\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\t(HL),D\n"
	// Cursor eins weiter
		+ "XOUTC1:\tDEC\tHL\n"
	// ggf. scrollen
		+ "XOUTC2:\tLD\tA,H\n"
		+ "\tCP\t10H\n"
		+ "\tCALL\tC,XOUTC3\n"
	// Cursor setzen
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(1846H),A\n"
		+ "\tLD\t(1800H),HL\n"
		+ "\tRET\n"
	// Bildschirm scrollen
		+ "XOUTC3:\tPUSH\tHL\n"
		+ "\tLD\tA,(X_MCOS)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XOUTC4\n"
		+ "\tSET\t2,E\n"
		+ "\tLD\tA,(X_MCOV)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tXOUTC5\n"
		+ "\tPOP\tDE\n"
		+ "\tRES\t2,E\n"
		+ "XOUTC4:\tLD\tA,20H\n"
		+ "\tCALL\tXOUTC5\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC,0040H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tRET\n"
		+ "XOUTC5:\tLD\tC,0F0H\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\tHL,17BFH\n"
		+ "\tLD\tDE,17FFH\n"
		+ "\tLD\tBC,07C0H\n"
		+ "\tLDDR\n"
		+ "\tLD\tB,40H\n"
		+ "XOUTC6:\tLD\t(DE),A\n"
		+ "\tDEC\tDE\n"
		+ "\tDJNZ\tXOUTC6\n"
		+ "\tRET\n"
		+ "XOUTC7:\tINC\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\t18H\n"
		+ "\tJR\tC,XOUTC2\n"
		+ "\tLD\tHL,1000H\n"
		+ "\tJR\tXOUTC2\n"
		+ "XOUTC8:\tLD\tBC,0040H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tJR\tXOUTC2\n"
		+ "XOUTC9:\tLD\tBC,0040H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\t18H\n"
		+ "\tJR\tC,XOUTC2\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t3FH\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,10H\n"
		+ "\tJR\tXOUTC2\n"
		+ "XOUTC10:\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t0C0H\n"
		+ "\tLD\tL,A\n"
		+ "\tDEC\tHL\n"
		+ "\tJR\tXOUTC2\n" );
	if( !this.xclsAppended ) {
	  appendXCLS( buf );
	}
      } else {
	super.appendXOUTCH( buf );
      }
      this.xoutchAppended = true;
    }
  }


  @Override
  public void appendXOUTNL( AsmCodeBuf buf )
  {
    if( this.usesColors ) {
      buf.append( "XOUTNL:\tLD\tA,0DH\n"
		+ "\tJP\tXOUTCH\n" );
    } else {
      super.appendXOUTNL( buf );
    }
  }


  @Override
  public void appendXPAPER( AsmCodeBuf buf )
  {
    buf.append( "XPAPER:\tLD\tA,L\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tAND\t0F0H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,(X_MCOV)\n"
		+ "\tAND\t0FH\n"
		+ "\tOR\tB\n"
		+ "\tLD\t(X_MCOV),A\n"
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
		+ "XPSET3:\tAND\t0FH\n" );
      if( this.usesColors ) {
	buf.append( "\tLD\tD,A\n"
		+ "\tLD\tC,0F0H\n"
		+ "\tIN\tE,(C)\n"
		+ "\tSET\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\tA,(X_MCOV)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRES\t2,E\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\t(HL),D\n" );
      } else {
	buf.append( "\tLD\t(HL),A\n" );
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
   *   HL=0:  Pixel nicht gesetzt
   *   HL=1:  Pixel gesetzt
   *   HL=-1: Pixel existiert nicht
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
    buf.append( "XTARID:\tDB\t\'AC1\'\n"
		+ "\tDB\t00H\n" );
  }


  @Override
  public boolean createsCodeFor( EmuSys emuSys )
  {
    boolean rv = false;
    if( emuSys != null ) {
      if( emuSys instanceof AC1 ) {
        rv = (((AC1) emuSys).emulates2010Mode()
	      || ((AC1) emuSys).emulatesSCCHMode());
      }
    }
    return rv;
  }


  @Override
  public int getColorBlack()
  {
    return 0;
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
  public int getGraphicScreenNum()
  {
    return 0;
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
  public boolean supportsXLOCAT()
  {
    return this.usesColors ? true : super.supportsXLOCAT();
  }


  @Override
  public String toString()
  {
    return "AC1-2010, AC1-SCCH mit optionaler Farbgrafikkarte";
  }


	/* --- private Methoden --- */

  private void appendPixUtil( AsmCodeBuf buf )
  {
    if( !this.pixUtilAppended ) {
      buf.append(
	    /*
	     * Pruefen der Parameter
	     *   DE: X-Koordinate (0...127)
	     *   HL: Y-Koordinate (0...63)
	     * Rueckgabe:
	     *   CY=1: Pixel ausserhalb des gueltigen Bereichs
	     */
		"X_PCK:\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tJR\tZ,X_PCK1\n"
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "X_PCK1:\tLD\tA,7FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tL\n"
		+ "\tRET\n"
	    /*
	     * Ermitteln von Informationen zu einem Pixel
	     * Parameter:
	     *   DE: X-Koordinate (0...127)
	     *   HL: Y-Koordinate (0...63)
	     * Rueckgabe:
	     *   B:  Aktuelles Bitmuster (gesetzte Pixel) in der Speicherzelle,
	     *       Bitanordnung innerhalb einer Zeichenposition:
	     *         +---+
	     *         |0 1|
	     *         |2 3|
	     *         +---+
	     *   C:  Bitmuster mit einem gesetzten Bit,
             *       dass das Pixel in der Speicherzelle beschreibt
	     *   HL: Speicherzelle, in der sich das Pixel befindet
	     */
		+ "X_PST:\tLD\tA,01H\n"
		+ "\tSRL\tL\n"
		+ "\tJR\tC,X_PST1\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "X_PST1:\tSRL\tE\n"
		+ "\tJR\tNC,X_PST2\n"
		+ "\tSLA\tA\n"
		+ "X_PST2:\tADD\tHL,HL\n"
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
		+ "\tRET\n" );
      this.pixUtilAppended = true;
    }
  }
}
