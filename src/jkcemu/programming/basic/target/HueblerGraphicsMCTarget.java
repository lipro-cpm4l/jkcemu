/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * HueblerGraphicsMC-spezifische Code-Erzeugung des BASIC-Compilers
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import jkcemu.base.EmuSys;
import jkcemu.emusys.HueblerGraphicsMC;
import jkcemu.emusys.huebler.AbstractHueblerMC;
import jkcemu.programming.basic.*;


public class HueblerGraphicsMCTarget extends AbstractTarget
{
  public static final String BASIC_TARGET_NAME = "TARGET_HUEBLER";

  private boolean usesX_M_INKEY;
  private boolean pixUtilAppended;
  private boolean xpsetAppended;
  private boolean xptestAppended;


  public HueblerGraphicsMCTarget()
  {
    setNamedValue( "GRAPHICSCREEN", 0 );
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
    buf.append( "\tJP\t0F01EH\n" );
  }


  @Override
  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0019H\n" );
  }


  @Override
  public void appendHPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0100H\n" );
  }


  public void appendInitTo( AsmCodeBuf buf )
  {
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
      buf.append( "XCKBRK:\tCALL\t0F012H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n"
		+ "\tCALL\t0F003H\n"
		+ "\tCP\t03H\n" );
      if( xinkey ) {
	buf.append( "\tJR\tZ,XBREAK\n"
		+ "\tLD\t(X_M_INKEY),A\n"
		+ "\tRET\n"
		+ "XINKEY:\tLD\tA,(X_M_INKEY)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XINKE1\n"
		+ "\tPUSH\tAF\n"
		+ "\tXOR\tA\n"
		+ "\tLD\t(X_M_INKEY),A\n"
		+ "\tPOP\tAF\n"
		+ "\tRET\n"
		+ "XINKE1:\tCALL\t0F012H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n" );
	this.usesX_M_INKEY = true;
      } else {
	buf.append( "\tRET\tNZ\n"
		+ "\tJR\tXBREAK\n" );
      }
      if( xinch ) {
	buf.append( "XINCH:\n" );
	if( this.usesX_M_INKEY ) {
	  buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_INKEY),A\n" );
	}
	buf.append( "XINCH1:\tCALL\t0F003H\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\tNZ\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XINCH1\n"
		+ "\tRET\n" );
      }
    } else {
      if( xinkey ) {
	buf.append( "XINKEY:\tCALL\t0F012H\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n" );
      }
      if( xinkey || xinch ) {
	if( canBreakOnInput ) {
	  buf.append( "XINCH:\tCALL\t0F003H\n"
		+ "\tCP\t03H\n"
		+ "\tRET\tNZ\n"
		+ "\tJR\tXBREAK\n" );
	} else {
	  buf.append( "XINCH:\tJP\t0F003H\n" );
	}
      }
    }
  }


  @Override
  public void appendPrologTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler,
			String        appName )
  {
    boolean done = false;
    if( appName != null ) {
      if( !appName.isEmpty() ) {
	buf.append( "\tJP\t" );
	buf.append( BasicCompiler.START_LABEL );
	buf.append( "\n"
		+ "\tDB\t0EDH,0FFH\n" );
	buf.appendStringLiteral( appName );
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
  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0020H\n" );
  }


  @Override
  public void appendWPixelTo( AsmCodeBuf buf )
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
  public void appendXCursTo( AsmCodeBuf buf )
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


  /*
   * Zeichnen einer horizontalen Linie
   * Parameter:
   *   BC: Laenge - 1
   *   DE: linke X-Koordinate, nicht kleiner 0
   *   HL: Y-Koordinate
   */
  public void appendXHLineTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XHLINE:\tBIT\t7,B\n"
		+ "\tRET\tNZ\n"
		+ "\tPUSH\tBC\n"
		+ "\tCALL\tX_PST\n"
		+ "\tPOP\tBC\n"
		+ "\tRET\tC\n"
		+ "\tLD\tD,00H\n"
		+ "XHLINE1:\n"
		+ "\tOR\tD\n"
		+ "\tLD\tD,A\n"
		+ "\tSLA\tA\n"
		+ "\tJR\tNC,XHLINE2\n"
		+ "\tLD\tA,D\n" );
    if( this.usesX_M_PEN ) {
      buf.append( "\tCALL\tXPSET_A\n" );
    } else {
      buf.append( "\tOR\t(HL)\n"
		+ "\tLD\t(HL),A\n" );
    }
    buf.append( "\tINC\tHL\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t1FH\n"
		+ "\tRET\tZ\n"
		+ "\tLD\tA,01H\n"
		+ "\tLD\tD,00H\n"
		+ "XHLINE2:\n"
		+ "\tDEC\tBC\n"
		+ "\tBIT\t7,B\n"
		+ "\tJR\tZ,XHLINE1\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,XPSET_A\n"
		+ "\tRET\n" );
    appendXPSetTo( buf, compiler );
  }


  @Override
  public void appendXLocateTo( AsmCodeBuf buf )
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
    appendXOutchTo( buf );
  }


  @Override
  public void appendXLPtchTo( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tC,A\n"
		+ "\tJP\t0F00FH\n" );
  }


  @Override
  public void appendXOutchTo( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tLD\tC,A\n"
		+ "\tJP\t0F009H\n" );
      this.xoutchAppended = true;
    }
  }


  @Override
  public void appendXOutnlTo( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tC,0DH\n"
		+ "\tCALL\t0F009H\n"
		+ "\tLD\tC,0AH\n"
		+ "\tJP\t0F009H\n" );
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
		+ "\tJR\tZ,XPAINT_LEFT5\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tDEC\tDE\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_PST\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tC,XPAINT_LEFT4\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,(HL)\n"
		+ "XPAINT_LEFT1:\n"
		+ "\tLD\tA,B\n"
		+ "XPAINT_LEFT2:\n"
		+ "\tAND\tC\n"
		+ "\tJR\tNZ,XPAINT_LEFT3\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tLD\tB,A\n"
		+ "\tDEC\tDE\n"
		+ "\tSRL\tC\n"
		+ "\tJR\tNC,XPAINT_LEFT2\n"
		+ "\tLD\t(HL),B\n"
		+ "\tDEC\tHL\n"
		+ "\tLD\tA,E\n"
		+ "\tINC\tA\n"
		+ "\tJR\tZ,XPAINT_LEFT4\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\tC,80H\n"
		+ "\tJR\tXPAINT_LEFT1\n"
		+ "XPAINT_LEFT3:\n"
		+ "\tLD\t(HL),B\n"
		+ "XPAINT_LEFT4:\n"
		+ "\tINC\tDE\n"
		+ "XPAINT_LEFT5:\n"
		+ "\tLD\t(PAINT_M_X1),DE\n"
		+ "\tRET\n"
		+ "XPAINT_RIGHT:\n"
		+ "\tLD\tDE,(PAINT_M_X)\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_PST\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tAND\tB\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tJR\tXPAINT_RIGHT3\n"
		+ "XPAINT_RIGHT1:\n"
		+ "\tLD\tA,B\n"
		+ "XPAINT_RIGHT2:\n"
		+ "\tAND\tC\n"
		+ "\tJR\tNZ,XPAINT_RIGHT4\n"
		+ "XPAINT_RIGHT3:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tLD\tB,A\n"
		+ "\tINC\tDE\n"
		+ "\tSLA\tC\n"
		+ "\tJR\tNC,XPAINT_RIGHT2\n"
		+ "\tLD\t(HL),B\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tA,E\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XPAINT_RIGHT5\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\tC,01H\n"
		+ "\tJR\tXPAINT_RIGHT1\n"
		+ "XPAINT_RIGHT4:\n"
		+ "\tLD\t(HL),B\n"
		+ "XPAINT_RIGHT5:\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\t(PAINT_M_X2),DE\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
    appendPixUtilTo( buf );
  }


  /*
   * Farbe eines Pixels ermitteln,
   * Die Rueckgabewerte sind identisch zur Funktion XPTEST
   *
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
    appendXPTestTo( buf, compiler );
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
		+ "\tRET\tC\n"
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\t(HL),A\n"
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
      buf.append( "XPSET:\tCALL\tX_PST\n"
		+ "\tRET\tC\n"
		+ "XPSET_A:\n" );
      if( this.usesX_M_PEN ) {
	buf.append( "\tLD\tE,A\n"
		+ "\tLD\tA,(X_M_PEN)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XPSET2\n"		// Stift 1 (Normal)
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XPSET1\n"		// Stift 2 (Loeschen)
		+ "\tDEC\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,(HL)\n"		// Stift 3 (XOR-Mode)
		+ "\tXOR\tE\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n"
		+ "XPSET1:\tLD\tA,E\n"		// Pixel loeschen
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n"
		+ "XPSET2:\tLD\tA,E\n" );	// Pixel setzen
      }
      buf.append( "\tOR\t(HL)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
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
   *   HL=-1: Pixel exisistiert nicht
   */
  @Override
  public void appendXPTestTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.xptestAppended ) {
      buf.append( "XPOINT:\n"
		+ "XPTEST:\tCALL\tX_PST\n"
		+ "\tJR\tC,X_PTEST1\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
		+ "X_PTEST1:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
      appendPixUtilTo( buf );
      this.xptestAppended = true;
    }
  }


  @Override
  public int get100msLoopCount()
  {
    return 42;
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
      if( emuSys instanceof HueblerGraphicsMC ) {
	rv = 3;
      } else if( emuSys instanceof AbstractHueblerMC ) {
	rv = 1;
      }
    }
    return rv;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x0100;
  }


  @Override
  public String getHostName()
  {
    return "Huebler-MC";
  }


  @Override
  public int getMaxAppNameLen()
  {
    return 14;
  }


  @Override
  public String getStartCmd( EmuSys emuSys, String appName, int begAddr )
  {
    String rv = null;
    if( emuSys != null ) {
      if( emuSys instanceof AbstractHueblerMC ) {
	rv = appName;
      }
    }
    return rv;
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
    this.usesX_M_INKEY   = false;
    this.pixUtilAppended = false;
    this.xpsetAppended   = false;
    this.xptestAppended  = false;
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
  public boolean supportsXHLINE()
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
  public boolean supportsXPAINT_LEFT_RIGHT()
  {
    return true;
  }


  @Override
  public String toString()
  {
    return "H\u00FCbler-Grafik-MC";
  }


	/* --- private Methoden --- */

  private void appendPixUtilTo( AsmCodeBuf buf )
  {
    if( !this.pixUtilAppended ) {
      buf.append(
	    /*
	     * Pruefen der Parameter und
	     * ermitteln von Informationen zu einem Pixel
	     * Parameter:
	     *   DE: X-Koordinate (0...255)
	     *   HL: Y-Koordinate (0...255)
	     * Rueckgabe:
	     *   CY=1: Fehler
	     *   A:    Bitmuster mit einem gesetzten Bit,
             *         dass das Pixel in der Speicherzelle beschreibt
	     *   HL:   Speicherzelle, in der sich das Pixel befindet
	     */
		"X_PST:\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,E\n"
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
		+ "\tOR\tA\n"			// CY=0
		+ "\tSBC\tHL,DE\n"
		+ "\tADD\tHL,BC\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
      this.pixUtilAppended = true;
    }
  }
}
