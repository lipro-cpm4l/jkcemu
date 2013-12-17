/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Z1013-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Simulation einer 64x64-Pixel Vollgrafik
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import jkcemu.base.EmuSys;
import jkcemu.emusys.Z1013;
import jkcemu.programming.basic.*;


public class Z1013Target extends AbstractTarget
{
  private boolean pixUtilAppended;
  private boolean xpsetAppended;


  public Z1013Target()
  {
    reset();
  }


  @Override
  public void appendExit( AsmCodeBuf buf )
  {
    buf.append( "\tJP\t0038H\n" );
  }


  @Override
  public void appendHChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0020H\n" );
  }


  @Override
  public void appendHPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0040H\n" );
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
      buf.append( "XINKEY:\tXOR\tA\n"
		+ "\tLD\t(0004H),A\n"
		+ "\tRST\t20H\n"
		+ "\tDB\t04H\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n" );
      }
      buf.append( "\tRET\n" );
    }
    if( xinch) {
      buf.append( "XINCH:\tRST\t20H\n"
		+ "\tDB\t01H\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n" );
      }
      buf.append( "\tRET\n" );
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
    buf.append( "\tLD\tHL,0040H\n" );
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
    buf.append( "XCURS:\tLD\tA,H\n"
                + "\tOR\tL\n"
		+ "\tJR\tZ,XCURS1\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t0FFH\n"
		+ "\tRET\tZ\n"
		+ "\tLD\t(001FH),A\n"
		+ "\tLD\t(HL),0FFH\n"
		+ "\tRET\n"
		+ "XCURS1:\tLD\tA,(001FH)\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
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
  @Override
  public void appendXJOY( AsmCodeBuf buf )
  {
    buf.append( "XJOY:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,XJOY2\n"
		+ "\tLD\tA,(0FFBBH)\n"
		+ "\tCP\t0C3H\n"
		+ "\tJR\tNZ,XJOY2\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,XJOY1\n"
		+ "\tCALL\t0FFBBH\n"
		+ "\tLD\tL,C\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "XJOY1:\tCP\t01H\n"
		+ "\tJR\tNZ,XJOY2\n"
		+ "\tCALL\t0FFBBH\n"
		+ "\tLD\tL,B\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "XJOY2:\tLD\tHL,0000H\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXLOCATE( AsmCodeBuf buf )
  {
    buf.append( "XLOCATE:\n"
		+ "\tLD\tA,1FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\tA,(001FH)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tPOP\tHL\n"
		+ "\tEX\tDE,HL\n"
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
		+ "\tRET\n" );
  }


  @Override
  public void appendXLPTCH( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tB,A\n"
		+ "\tLD\tA,(0FFE8H)\n"
		+ "\tCP\t0C3H\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,B\n"
		+ "\tJP\t0FFE8H\n" );
  }


  @Override
  public void appendXOUTCH( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tCP\t0AH\n"
		+ "\tRET\tZ\n"
		+ "\tRST\t20H\n"
		+ "\tDB\t00H\n"
		+ "\tRET\n" );
      this.xoutchAppended = true;
    }
  }


  /*
   * Ausgabe eines Zeilenumbruchs
   */
  @Override
  public void appendXOUTNL( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tA,0DH\n"
		+ "\tRST\t20H\n"
		+ "\tDB\t00H\n"
		+ "\tRET\n" );
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
		+ "\tLD\t(DE),A\n"
		+ "\tRET\n" );
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
    appendPixUtil( buf, compiler );
  }


  /*
   * Target-ID-String
   */
  @Override
  public void appendXTARID( AsmCodeBuf buf )
  {
    buf.append( "XTARID:\tDB\t\'Z1013\'\n"
		+ "\tDB\t00H\n" );
  }


  /*
   * Der Programmcode der Routine X_PCK liegt in einer separaten Methode,
   * damit er in abgeleiteten Klassen ueberschrieben werden kann.
   */
  protected void appendX_PCK( AsmCodeBuf buf, BasicCompiler compiler )
  {
    /*
     * Pruefen der Parameter
     *   DE: X-Koordinate (0...63)
     *   HL: Y-Koordinate (0...63)
     * Rueckgabe:
     *   CY=1: Pixel ausserhalb des gueltigen Bereichs
     */
      buf.append( "X_PCK:\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tJR\tZ,X_PCK1\n"
		+ "\tSCF\n"
		+ "\tRET\n"
		+ "X_PCK1:\tLD\tA,3FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tCP\tL\n"
		+ "\tRET\n" );
  }


  @Override
  public boolean createsCodeFor( EmuSys emuSys )
  {
    return emuSys != null ? (emuSys instanceof Z1013) : false;
  }


  @Override
  public int get100msLoopCount()
  {
    return 55;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x0100;
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
    return new int[] { 0xDC, 0xFC };
  }


  @Override
  public void reset()
  {
    super.reset();
    this.pixUtilAppended = false;
    this.xpsetAppended   = false;
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
    return "Z1013";
  }


	/* --- private Methoden --- */

  private void appendPixUtil( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.pixUtilAppended ) {
      appendX_PCK( buf, compiler );

      /*
       * Ermitteln von Informationen zu einem Pixel
       * Parameter:
       *   DE: X-Koordinate (0...63)
       *   HL: Y-Koordinate (0...63)
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
      buf.append( "X_PST:\tLD\tA,01H\n"
		+ "\tSRL\tL\n"
		+ "\tJR\tNC,X_PST1\n"
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
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tBC,0EFE0H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tC,A\n"
		+ "\tXOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\tB,0FH\n"
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
