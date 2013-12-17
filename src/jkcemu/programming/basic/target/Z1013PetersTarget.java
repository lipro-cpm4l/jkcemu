/*
 * (c) 2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Z1013-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Unterstuetzung des 64x16-Modus der Peters-Platine
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import jkcemu.programming.basic.*;


public class Z1013PetersTarget extends Z1013Target
{
  private boolean needsScreenSizeHChar;
  private boolean needsScreenSizeWChar;
  private boolean needsScreenSizePixel;


  public Z1013PetersTarget()
  {
    reset();
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
    buf.append( "X_SCR0:\n"
		+ "\tIN\tA,(04H)\n"
		+ "\tAND\t7FH\n"
		+ "\tOUT\t(04H),A\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_MSCR),A\n"
		+ "\tRET\n" );
    if( this.needsScreenSizeHChar ) {
      buf.append( "X_HCHR:\tLD\tHL,0010H\n"
		+ "\tLD\tA,(X_MSCR)\n"
		+ "\tDEC\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tL,20H\n"
		+ "\tRET\n" );
    }
    if( this.needsScreenSizeWChar ) {
      buf.append( "X_WCHR:\tLD\tHL,0040H\n"
		+ "\tLD\tA,(X_MSCR)\n"
		+ "\tDEC\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tL,0020H\n"
		+ "\tRET\n" );
    }
    if( this.needsScreenSizePixel ) {
      buf.append( "X_HPIX:\n"
		+ "X_WPIX:\tLD\tHL,0040H\n"
		+ "\tLD\tA,(X_MSCR)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tL,00H\n"
		+ "\tRET\n" );
    }
  }


  @Override
  public void appendExit( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_SCR0\n" );
    super.appendExit( buf );
  }


  @Override
  public void appendHChar( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_HCHR\n" );
    this.needsScreenSizeHChar = true;
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
		+ "\tCALL\tX_SCR0\n" );
  }


  @Override
  public void appendSwitchToTextScreen( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_SCR0\n" );
  }


  @Override
  public void appendWChar( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_WCHR\n" );
    this.needsScreenSizeWChar = true;
  }


  @Override
  public void appendWPixel( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_WPIX\n" );
    this.needsScreenSizePixel = true;
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
    buf.append( "XLOCATE:\n"
		+ "\tLD\tA,(X_MSCR)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XLOCATE1\n"
		+ "\tLD\tA,1FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tXLOCATE3\n"
		+ "\tJR\tXLOCATE2\n"
		+ "XLOCATE1:\n"
		+ "\tLD\tA,0FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tCALL\tXLOCATE3\n"
		+ "\tADD\tHL,HL\n"
		+ "XLOCATE2:\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tBC,0EC00H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(HL),0FFH\n"
		+ "\tLD\t(002BH),HL\n"
		+ "\tRET\n"
		+ "XLOCATE3:\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\tA,(001FH)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tPOP\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXOUTCH( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tCP\t0AH\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,(X_MSCR)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XOUTC1\n"
		+ "\tLD\tA,D\n"
		+ "\tRST\t20H\n"
		+ "\tDB\t00H\n"
		+ "\tRET\n"
      // Bildschirmroutine fuer 64x16-Modus
		+ "XOUTC1:\tLD\tA,(001FH)\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tA,D\n"
		+ "\tCP\t08H\n"
		+ "\tJR\tZ,XOUTC7\n"
		+ "\tCP\t09H\n"
		+ "\tJR\tZ,XOUTC2\n"
		+ "\tCP\t0CH\n"
		+ "\tJP\tZ,XOUTC8\n"
		+ "\tCP\t0DH\n"
		+ "\tJR\tZ,XOUTC9\n"
		+ "\tLD\t(HL),D\n"
	// Cursor eins weiter
		+ "XOUTC2:\tINC\tHL\n"
	// ggf. scrollen
		+ "XOUTC3:\tLD\tA,H\n"
		+ "\tCP\t0F0H\n"
		+ "\tCALL\tNC,XOUTC5\n"
	// Cursor setzen
		+ "XOUTC4:\tLD\tA,(HL)\n"
		+ "\tLD\t(001FH),A\n"
		+ "\tLD\t(HL),0FFH\n"
		+ "\tLD\t(002BH),HL\n"
		+ "\tRET\n"
	// Bildschirm scrollen
		+ "XOUTC5:\tPUSH\tHL\n"
		+ "\tLD\tHL,0EC40H\n"
		+ "\tLD\tDE,0EC00H\n"
		+ "\tLD\tBC,03C0H\n"
		+ "\tLDIR\n"
		+ "\tLD\tA,20H\n"
		+ "\tLD\tB,40H\n"
		+ "XOUTC6:\tLD\t(DE),A\n"
		+ "\tINC\tDE\n"
		+ "\tDJNZ\tXOUTC6\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tBC,0040H\n"
		+ "\tOR\tA\n"
		+ "\tSBC\tHL,BC\n"
		+ "\tRET\n"
	// Cursor links
		+ "XOUTC7:\tDEC\tHL\n"
		+ "\tLD\tA,H\n"
		+ "\tCP\t0ECH\n"
		+ "\tJR\tNC,XOUTC4\n"
		+ "\tLD\tHL,0EFFFH\n"
		+ "\tJR\tXOUTC4\n"
	// Bildschirm loeschen
		+ "XOUTC8:\tLD\tHL,0EC00H\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tA,20H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tLD\tDE,0EC01H\n"
		+ "\tLD\tBC,03FFH\n"
		+ "\tLDIR\n"
		+ "\tPOP\tHL\n"
		+ "\tJR\tXOUTC4\n"
	// Cursor auf neue Zeile
		+ "XOUTC9:\tLD\tB,20H\n"
		+ "XOUTC10:\n"
		+ "\tLD\t(HL),B\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t03FH\n"
		+ "\tJR\tNZ,XOUTC10\n"
		+ "\tJR\tXOUTC3\n" );
      this.xoutchAppended = true;
    }
  }


  /*
   * Ausgabe eines Zeilenumbruchs
   */
  @Override
  public void appendXOUTNL( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tA,0DH\n" );
    if( this.xoutchAppended ) {
      buf.append( "\tJR\tXOUTCH\n" );
    } else {
      appendXOUTCH( buf );
    }
  }


  /*
   * Bildschirmmode einstellen
   *   HL: Screen-Nummer
   *        0: Text mit 32x32 Zeichen und Grafik mit 64x64 Pixel
   *        1: Text mit 64x16 Zeichen
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
		+ "\tLD\tC,04H\n"
		+ "\tIN\tB,(C)\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XSCRS2\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tZ,XSCRS3\n"
		+ "XSCRS1:\tSCF\n"
		+ "\tRET\n"
		+ "XSCRS2:\tRES\t7,B\n"		// SCREEN 0
		+ "\tJR\tXSCRS4\n"
		+ "XSCRS3:\tSET\t7,B\n"		// SCREEN 1
		+ "XSCRS4:\tOUT\t(C),B\n"
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
    buf.append( "XTARID:\tDB\t\'Z1013_64X16\'\n"
		+ "\tDB\t00H\n" );
  }


  @Override
  protected void appendX_PCK( AsmCodeBuf buf, BasicCompiler compiler )
  {
      buf.append( "X_PCK:"
	// Pruefen, ob der Grafik-Screen eingestellt ist
		+ "\tLD\tA,(X_MSCR)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,X_PCK2\n"
	/*
	 * Pruefen der Parameter
	 *   DE: X-Koordinate (0...63)
	 *   HL: Y-Koordinate (0...63)
	 * Rueckgabe:
	 *   CY=1: Pixel ausserhalb des gueltigen Bereichs
	 */
		+ "\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tJR\tZ,X_PCK1\n"
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "X_PCK1:\tLD\tA,3FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tCP\tL\n"
		+ "\tRET\n"
		+ "X_PCK2:" );
      appendExitNoGraphicsScreen( buf, compiler );
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
    this.needsScreenSizeHChar = false;
    this.needsScreenSizeWChar = false;
    this.needsScreenSizePixel = false;
  }


  @Override
  public String toString()
  {
    return "Z1013 mit Peters-Platine (64x16 Zeichen)";
  }
}
