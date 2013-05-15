/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Z9001-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Ansteuerung der KRT-Grafik
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import java.util.Set;
import jkcemu.base.EmuSys;
import jkcemu.emusys.Z9001;
import jkcemu.programming.basic.*;


public class Z9001KRTTarget extends Z9001Target
{
  private boolean needsScreenSizeChar;
  private boolean needsScreenSizePixel;
  private boolean xpsetAppended;


  public void Z9001KRTTarget()
  {
    // leer
  }


  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    buf.append( "X_MSCR:\tDS\t1\n" );
  }


  @Override
  public void appendEtc( AsmCodeBuf buf )
  {
    buf.append( "XSCRS:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,XSCRS2\n"
		+ "XSCRS1:\tLD\tA,(X_MSCR)\n"
		+ "\tCP\tL\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XSCRS3\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,XSCRS4\n"
		+ "XSCRS2:\tSCF\n"
		+ "\tRET\n"
		+ "XSCRS3:\tLD\tDE,0800H\n"	// KRT -> STD
		+ "\tJR\tXSCRS5\n"
		+ "XSCRS4:\tLD\tC,29\n"		// Cursor abschalten
		+ "\tCALL\t0005H\n"
		+ "\tLD\tDE,0008H\n"		// STD -> KRT
		+ "XSCRS5:\tLD\t(X_MSCR),A\n"
		+ "\tLD\tBC,40B8H\n"
		+ "\tLD\tHL,0EFC0H\n"
		+ "\tDI\n"
		+ "XSCRS6:\tOUT\t(C),D\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOUT\t(C),E\n"
		+ "\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tXSCRS6\n"
		+ "\tEI\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
    if( this.needsScreenSizeChar ) {
      buf.append( "X_HCHR:\tLD\tHL,0018H\n"
		+ "\tJR\tX_SSZC\n"
		+ "X_WCHR:\tLD\tHL,0028H\n"
		+ "X_SSZC:\tLD\tA,(X_MSCR)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
    }
    if( this.needsScreenSizePixel ) {
      buf.append( "X_HPIX:\tLD\tHL,00C0H\n"
		+ "\tJR\tX_SSZP\n"
		+ "X_WPIX:\tLD\tHL,0140H\n"
		+ "X_SSZP:\tLD\tA,(X_MSCR)\n"
		+ "\tCP\t01H\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
    }
  }


  @Override
  public void appendExit( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tL,00H\n"
		+ "\tCALL\tXSCRS1\n"
		+ "\tJP\t0000H\n" );
  }


  @Override
  public void appendHChar( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_HCHR\n" );
    this.needsScreenSizeChar = true;
  }


  @Override
  public void appendHPixel( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_HPIX\n" );
    this.needsScreenSizePixel = true;
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_MSCR),A\n"
		+ "\tOUT\t(0B8H),A\n" );
  }


  @Override
  public void appendSwitchToTextScreen( AsmCodeBuf buf )
  {
    if( buf != null ) {
      buf.append( "\tLD\tL,00H\n"
		+ "\tCALL\tXSCRS1\n" );
    }
  }


  @Override
  public void appendWChar( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_WCHR\n" );
    this.needsScreenSizeChar = true;
  }


  @Override
  public void appendWPixel( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_WPIX\n" );
    this.needsScreenSizePixel = true;
  }


  @Override
  public void appendXCLS( AsmCodeBuf buf )
  {
    buf.append( "XCLS:\tLD\tA,(X_MSCR)\n"
		+ "\tCP\t01\n"
		+ "\tJR\tZ,XCLS1\n"
		+ "\tLD\tA,0CH\n"
		+ "\tJR\tXOUTCH\n"
		+ "XCLS1:\tDI\n"
		+ "\tLD\tA,08H\n"
		+ "XCLS2:\tOUT\t(0B8H),A\n"
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
		+ "\tJR\tC,XCLS2\n"
		+ "\tLD\tA,08\n"
		+ "\tOUT\t(0B8H),A\n"		// Bank 0 einstellen
		+ "\tEI\n" );
    if( this.usesColors ) {
      buf.append( "\tLD\tHL,0E800H\n"
		+ "\tLD\tA,(0027H)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tDE,0E801H\n"
		+ "\tLD\tBC,03BFH\n"
		+ "\tLDIR\n" );
    }
    buf.append( "\tRET\n" );
    appendXOUTCH( buf );
  }


  @Override
  public void appendXOUTCH( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tLD\tE,A\n"
		+ "\tLD\tA,(X_MSCR)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tC,2\n"
		+ "\tLD\tA,E\n"
		+ "\tJP\t0005H\n" );
      this.xoutchAppended = true;
    }
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
      buf.append( "\tDI\n"
		+ "\tLD\tA,B\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
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
                + "\tJR\tZ,XPSET2\n"            // Stift 1 (Normal)
                + "\tDEC\tA\n"
                + "\tJR\tZ,XPSET1\n"            // Stift 2 (Loeschen)
                + "\tDEC\tA\n"
                + "\tRET\tNZ\n"
		+ "\tDI\n"			// Stift 3 (XOR-Mode)
		+ "\tLD\tA,B\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,C\n"
		+ "\tXOR\t(HL)\n"
		+ "\tJR\tXPSET3\n"
		+ "XPSET1:\tDI\n"		// Pixel loeschen
		+ "\tLD\tA,B\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tJR\tXPSET3\n" );
      }
      buf.append( "XPSET2:\tDI\n"		// Pixel setzen
		+ "\tLD\tA,B\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,C\n"
		+ "\tOR\t(HL)\n"
		+ "XPSET3:\tLD\t(HL),A\n"
		+ "\tLD\tA,08H\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tEI\n" );
      if( this.usesColors ) {
	buf.append( "\tLD\tDE,0FC00H\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tA,(0027H)\n"
		+ "\tLD\t(HL),A\n" );
      }
      buf.append( "\tRET\n" );
      appendPixUtil( buf, compiler );
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
    buf.append( "XPTEST:\tLD\tA,(X_MSCR)\n"
                + "\tCP\t01H\n"
                + "\tJR\tNZ,XPTST1\n"
		+ "\tCALL\tX_PCK1\n"
                + "\tJR\tC,XPTST1\n"
		+ "\tCALL\tX_PST\n"
		+ "\tDI\n"
		+ "\tLD\tA,B\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tLD\tA,C\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\tA,08H\n"
		+ "\tOUT\t(0B8H),A\n"
		+ "\tEI\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
		+ "XPTST1:\tLD\tHL,0FFFFH\n"
                + "\tRET\t" );
    appendPixUtil( buf, compiler );
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
  public void appendXSCRS( AsmCodeBuf buf )
  {
    /*
     * Der Programmcode wird bei appendExit(...) erzeugt.
     * Deshalb ist diese Methode hier leer.
     * Die Methode muss aber ueberschrieben werden,
     * damit der Standardcode nicht generiert wird.
     */
  }


  /*
   * Target-ID-String
   */
  @Override
  public void appendXTARID( AsmCodeBuf buf )
  {
    buf.append( "XTARID:\tDB\t\'Z9001_KRT\'\n"
		+ "\tDB\t00H\n" );
  }


  @Override
  public boolean createsCodeFor( EmuSys emuSys )
  {
    boolean rv = false;
    if( emuSys != null ) {
      if( emuSys instanceof Z9001 ) {
	rv = ((Z9001) emuSys).emulatesGraphicsKRT();
      }
    }
    return rv;
  }


  @Override
  public int getLastScreenNum()
  {
    return 1;
  }


  @Override
  public void reset()
  {
    super.reset();
    this.needsScreenSizeChar  = false;
    this.needsScreenSizePixel = false;
    this.xpsetAppended        = false;
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
  public String toString()
  {
    return "KC85/1, KC87, Z9001 mit KRT-Grafik";
  }


	/* --- private Methoden --- */

  private void appendPixUtil( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.pixUtilAppended ) {
      buf.append(
	    /*
	     * Pruefen der Parameter
	     *   DE: X-Koordinate (0...320)
	     *   HL: Y-Koordinate (0...192)
	     * Rueckgabe:
	     *   CY=1: Pixel ausserhalb des gueltigen Bereichs
	     */
		"X_PCK:\tLD\tA,(X_MSCR)\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tNZ,X_PCK4\n"
		+ "X_PCK1:\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_PCK2\n"
		+ "\tCP\t02H\n"
		+ "\tJR\tNC,X_PCK3\n"
		+ "\tLD\tA,E\n"
		+ "\tCP\t40H\n"
		+ "\tJR\tNC,X_PCK3\n"
		+ "X_PCK2:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,X_PCK3\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\t0C0H\n"
		+ "\tJR\tNC,X_PCK3\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n"
		+ "X_PCK3:\tSCF\n"
		+ "\tRET\n"
		+ "X_PCK4:" );
      appendExitNoGraphicsScreen( buf, compiler );
	    /*
	     * Ermitteln von Informationen zu einem Pixel
	     * Die entsprechende Speicher-Bank der KRT-Grafik
	     * wird eingeblendet.
	     * Parameter:
	     *   DE: X-Koordinate (0...320)
	     *   HL: Y-Koordinate (0...192)
	     * Rueckgabe:
	     *   B:  Nr. der Speicherbank inkl. gesetzten Bit 3
	     *   C:  Bitmuster mit einem gesetzten Bit,
             *       dass das Pixel in der Speicherzelle beschreibt
	     *   HL: Speicherzelle, in der sich das Pixel befindet
	     */
      buf.append( "X_PST:\tLD\tA,E\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tC,80H\n"
		+ "\tJR\tZ,X_PST2\n"
		+ "X_PST1:\tSRL\tC\n"
		+ "\tDJNZ\tX_PST1\n"
		+ "X_PST2:\tSRL\tD\n"
		+ "\tRR\tE\n"
		+ "\tSRL\tE\n"
		+ "\tSRL\tE\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,0FH\n"
		+ "\tSUB\tB\n"
		+ "\tLD\tB,A\n"
		+ "\tPUSH\tBC\n"
		+ "\tSRL\tL\n"			// H=0
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
		+ "\tOR\tA\n"		// CY=0, A unveraendert
		+ "\tSBC\tHL,BC\n"
		+ "\tPOP\tBC\n"
		+ "\tRET\n" );
      this.pixUtilAppended = true;
    }
  }
}
