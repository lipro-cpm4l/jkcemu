/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * LLC2-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Ansteuerung der HIRES-Vollgrafik
 *
 * Fuer den LLC2 wird mit Ausnahme der Grafikbefehle
 * der gleiche Code erzeugt wie fuer SCCH.
 * Aus diesem Grund ist die Klasse von SCCHTaget abgeleitet.
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import java.util.Set;
import jkcemu.base.EmuSys;
import jkcemu.emusys.LLC2;
import jkcemu.programming.basic.*;


public class LLC2HIRESTarget extends SCCHTarget
{
  private boolean needsScreenSizeChar;
  private boolean needsScreenSizePixel;
  private boolean pixUtilAppended;


  public void LLC2HIRESTarget()
  {
    reset();
  }


  @Override
  public void appendBSS( AsmCodeBuf buf )
  {
    super.appendBSS( buf );
    buf.append( "X_MSCR:\tDS\t1\n" );
  }


  @Override
  public void appendEtc( AsmCodeBuf buf )
  {
    if( this.needsScreenSizeChar ) {
      buf.append( "X_HCHR:\tLD\tHL,0020H\n"
		+ "\tJR\tX_SSZC\n"
		+ "X_WCHR:\tLD\tHL,0040H\n"
		+ "X_SSZC:\tLD\tA,(X_MSCR)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
    }
    if( this.needsScreenSizePixel ) {
      buf.append( "X_HPIX:\tLD\tHL,0100H\n"
		+ "\tJR\tX_SSZP\n"
		+ "X_WPIX:\tLD\tHL,0200H\n"
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
    buf.append( "\tXOR\tA\n"
		+ "\tOUT\t(0EEH),A\n"
		+ "\tJP\t07FDH\n" );
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
  public void appendInit( AsmCodeBuf buf )
  {
    super.appendInit( buf );
    buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_MSCR),A\n"
		+ "\tOUT\t(0EEH),A\n" );
  }


  @Override
  public void appendLastScreenNum( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0001H\n" );
  }


  @Override
  public void appendSwitchToTextScreen( AsmCodeBuf buf )
  {
    buf.append( "\tXOR\tA\n"
		+ "\tOUT\t(0EEH),A\n" );
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
		+ "XCLS1:\tLD\tHL,8000H\n"
		+ "\tXOR\tA\n"
		+ "\tLD\tC,40H\n"
		+ "XCLS2:\tLD\tB,00H\n"
		+ "XCLS3:\tLD\t(HL),A\n"
		+ "\tINC\tHL\n"
		+ "\tDJNZ\tXCLS3\n"
		+ "\tDEC\tC\n"
		+ "\tJR\tNZ,XCLS2\n"
		+ "\tRET\n" );
    appendXOUTCH( buf );
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
		+ "\tCALL\tX_PST\n"
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
    appendPixUtil( buf, compiler );
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
    buf.append( "XPSET:\tCALL\tX_PCK\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tX_PST\n" );
    if( this.usesX_MPEN ) {
      buf.append( "\tLD\tB,A\n"
		+ "\tLD\tA,(X_MPEN)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XPSET2\n"		// Stift 1 (Normal)
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XPSET1\n"		// Stift 2 (Loeschen)
		+ "\tDEC\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,(HL)\n"		// Stift 3 (XOR-Mode)
		+ "\tXOR\tB\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n"
		+ "XPSET1:\tLD\tA,B\n"		// Pixel loeschen
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n"
		+ "XPSET2\tLD\tA,B\n" );	// Pixel setzen
    }
    buf.append( "\tOR\t(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
    appendPixUtil( buf, compiler );
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
		+ "\tAND\t(HL)\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
		+ "XPTST1:\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    appendPixUtil( buf, compiler );
  }


  /*
   * Bildschirmmode einstellen
   *   HL: Screen-Nummer
   *        0: Textmode
   *        1: HIRES-Grafik auf 8000h
   * Rueckgabe:
   *   CY=1: Screen-Nummer nicht unterstuetzt
   */
  @Override
  public void appendXSCRS( AsmCodeBuf buf )
  {
    buf.append( "XSCRS:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,XSCRS1\n"
		+ "\tLD\tA,(X_MSCR)\n"
		+ "\tCP\tL\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XSCRS3\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,XSCRS2\n"
		+ "XSCRS1:\tSCF\n"
		+ "\tRET\n"
		+ "XSCRS2:\tLD\tA,50H\n"	// HIRES, 8000h
		+ "XSCRS3:\tOUT\t(0EEH),A\n"
		+ "\tLD\tA,L\n"
		+ "\tLD\t(X_MSCR),A\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
  }


  /*
   * Target-ID-String
   */
  @Override
  public void appendXTARID( AsmCodeBuf buf )
  {
    buf.append( "XTARID:\tDB\t\'LLC2_HIRES\'\n"
		+ "\tDB\t00H\n" );
  }


  @Override
  public boolean createsCodeFor( EmuSys emuSys )
  {
    return emuSys != null ? (emuSys instanceof LLC2) : false;
  }


  @Override
  public int get100msLoopCount()
  {
    return 85;
  }


  @Override
  public void reset()
  {
    super.reset();
    this.needsScreenSizeChar  = false;
    this.needsScreenSizePixel = false;
    this.pixUtilAppended      = false;
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
    return "LLC2 mit HIRES-Grafik";
  }


	/* --- private Methoden --- */

  private void appendPixUtil( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.pixUtilAppended ) {
      buf.append(
	    /*
	     * Pruefen der Parameter
	     *   DE: X-Koordinate (0...512)
	     *   HL: Y-Koordinate (0...255)
	     * Rueckgabe:
	     *   CY=1: Pixel ausserhalb des gueltigen Bereichs
	     */
		"X_PCK:\tLD\tA,(X_MSCR)\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tNZ,X_PCK3\n"
		+ "X_PCK1:\tLD\tA,D\n"
		+ "\tCP\t02H\n"
		+ "\tJR\tNC,X_PCK2\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tA\n"		// CY=0
		+ "\tRET\tZ\n"
		+ "X_PCK2:\tSCF\n"
		+ "\tRET\n"
		+ "X_PCK3:" );
      appendExitNoGraphicsScreen( buf, compiler );
	    /*
	     * Ermitteln von Informationen zu einem Pixel
	     * Parameter:
	     *   DE: X-Koordinate (0...512)
	     *   HL: Y-Koordinate (0...255)
	     * Rueckgabe:
	     *   A:  Bitmuster mit einem gesetzten Bit,
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
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,L\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tAND\t07H\n"
		+ "\tJR\tZ,X_PST4\n"
		+ "\tLD\tDE,0800H\n"
		+ "X_PST3:\tADD\tHL,DE\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tNZ,X_PST3\n"
		+ "X_PST4:\tEX\tDE,HL\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t0F8H\n"
		+ "\tLD\tL,A\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tHL,0BFC0H\n"
		+ "\tOR\tA\n"		// CY=0, A unveraendert
		+ "\tSBC\tHL,DE\n"
		+ "\tPOP\tDE\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tA,C\n"
		+ "\tRET\n" );
      this.pixUtilAppended = true;
    }
  }
}
