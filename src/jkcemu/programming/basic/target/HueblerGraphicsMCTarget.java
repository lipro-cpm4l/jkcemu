/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * HueblerGraphicsMC-spezifische Code-Erzeugung des BASIC-Compilers
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import jkcemu.base.EmuSys;
import jkcemu.emusys.HueblerGraphicsMC;
import jkcemu.programming.basic.*;


public class HueblerGraphicsMCTarget extends AbstractTarget
{
  private boolean pixUtilAppended;


  public HueblerGraphicsMCTarget()
  {
    reset();
  }


  @Override
  public void appendExit( AsmCodeBuf buf )
  {
    buf.append( "\tJP\t0F01EH\n" );
  }


  @Override
  public void appendHChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0019H\n" );
  }


  @Override
  public void appendHPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0100H\n" );
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
      buf.append( "XINKEY:\tCALL\t0F012H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCALL\t0F003H\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
      } else {
	buf.append( "\tJP\t0F003H\n" );
      }
    }
    if( xinch ) {
      if( canBreakOnInput ) {
	buf.append( "XINCH:\tCALL\t0F003H\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
      } else {
	buf.append( "XINCH:\tJP\t0F003H\n" );
      }
    }
  }


  @Override
  public void appendMenuItem(
			BasicCompiler compiler,
			AsmCodeBuf    buf,
			String        appName )
  {
    boolean done = false;
    if( appName != null ) {
      if( !appName.isEmpty() ) {
	buf.append( "\tDB\t0EDH,0FFH\n" );
	buf.appendStringLiteral( appName );
	buf.append( "\tJP\tMINIT\n" );
	done = true;
      }
    }
    if( !done ) {
      compiler.putWarning(
		"Programm kann auf dem Zielsystem nicht aufgerufen werden,"
				+ " da der Programmname leer ist." );
      buf.append( "\tENT\n" );
    }
  }


  @Override
  public void appendWChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0020H\n" );
  }


  @Override
  public void appendWPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0100H\n" );
  }


  /*
   * Ein- und Ausschalten des Cursors
   * Parameter:
   *   HL: 0:   Cursor ausschalten
   *       <>0: Cursor einschalten
   */
  @Override
  public void appendXCURS( AsmCodeBuf buf )
  {
    buf.append( "XCURS:\tPUSH\tHL\n"
		+ "\tLD\tC,1BH\n"
		+ "\tCALL\t0F009H\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tLD\tC,0BH\n"
		+ "\tJR\tZ,XCURS1\n"
		+ "\tINC\tC\n"
		+ "XCURS1:\tJP\t0F009H\n" );
  }


  @Override
  public void appendXLOCATE( AsmCodeBuf buf )
  {
    buf.append( "XLOCATE:\n"
		+ "\tLD\tA,1BH\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tLD\tA,E\n"
		+ "\tOR\t80H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\t80H\n"
		+ "\tJR\tXOUTCH\n" );
    appendXOUTCH( buf );
  }


  @Override
  public void appendXLPTCH( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tC,A\n"
		+ "\tJP\t0F00FH\n" );
  }


  @Override
  public void appendXOUTCH( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tLD\tC,A\n"
		+ "\tJP\t0F009H\n" );
      this.xoutchAppended = true;
    }
  }


  @Override
  public void appendXOUTNL( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tC,0DH\n"
		+ "\tCALL\t0F009H\n"
		+ "\tLD\tC,0AH\n"
		+ "\tJP\t0F009H\n" );
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
    appendPixUtil( buf );
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
		+ "XPSET2:\tLD\tA,B\n" );	// Pixel setzen
    }
    buf.append( "\tOR\t(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
    appendPixUtil( buf );
  }


  /*
   * Testen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   * Rueckgabe:
   *   HL=0:  Pixel nicht gesetzt
   *   HL=1:  Pixel gesetzt
   *   HL=-1: Pixel exisistiert nicht
   */
  @Override
  public void appendXPTEST( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPTEST:\tCALL\tX_PCK\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tX_PST\n"
		+ "\tAND\t(HL)\n"
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
    buf.append( "XTARID:\tDB\t\'HUEBLER\'\n"
		+ "\tDB\t00H\n" );
  }


  @Override
  public boolean createsCodeFor( EmuSys emuSys )
  {
    return emuSys != null ? (emuSys instanceof HueblerGraphicsMC) : false;
  }


  @Override
  public int get100msLoopCount()
  {
    return 42;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x0100;
  }


  @Override
  public int getKCNetBaseIOAddr()
  {
    return 0xC0;
  }


  @Override
  public int[] getVdipBaseIOAddresses()
  {
    return new int[] { 0xFC };
  }


  @Override
  public void reset()
  {
    super.reset();
    this.pixUtilAppended = false;
  }


  @Override
  public boolean supportsAppName()
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
    return "H\u00FCbler-Grafik-MC";
  }


	/* --- private Methoden --- */

  private void appendPixUtil( AsmCodeBuf buf )
  {
    if( !this.pixUtilAppended ) {
      buf.append(
	    /*
	     * Pruefen der Parameter
	     *   DE: X-Koordinate (0...255)
	     *   HL: Y-Koordinate (0...255)
	     * Rueckgabe:
	     *   CY=1: Fehler
	     */
		"X_PCK:\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tRET\tZ\n"
		+ "\tSCF\n"
		+ "\tRET\n"
	    /*
	     * Ermitteln von Informationen zu einem Pixel
	     * Parameter:
	     *   DE: X-Koordinate (0...255)
	     *   HL: Y-Koordinate (0...255)
	     * Rueckgabe:
	     *   A:  Bitmuster mit einem gesetzten Bit,
             *       dass das Pixel in der Speicherzelle beschreibt
	     *   HL: Speicherzelle, in der sich das Pixel befindet
	     */
		+ "X_PST:\tLD\tA,E\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,01H\n"
		+ "\tJR\tZ,X_PST2\n"
		+ "X_PST1:\tSLA\tA\n"
		+ "\tDJNZ\tX_PST1\n"
		+ "X_PST2:\tSRL\tE\n"
		+ "\tSRL\tE\n"
		+ "\tSRL\tE\n"
		+ "\tLD\tC,E\n"
		+ "\tLD\tB,00H\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tLD\tDE,0DFE0H\n"
		+ "\tEX\tDE,HL\n"
		+ "\tOR\tA\n"		// CY=0, A unveraendert
		+ "\tSBC\tHL,DE\n"
		+ "\tADD\tHL,BC\n"
		+ "\tRET\n" );
      this.pixUtilAppended = true;
    }
  }
}
