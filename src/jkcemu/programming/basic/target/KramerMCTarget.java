/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * KramerMC-spezifische Code-Erzeugung des BASIC-Compilers
 */

package jkcemu.programming.basic.target;

import jkcemu.base.EmuSys;
import jkcemu.emusys.KramerMC;
import jkcemu.programming.basic.AbstractTarget;
import jkcemu.programming.basic.AsmCodeBuf;


public class KramerMCTarget extends AbstractTarget
{
  public static final String BASIC_TARGET_NAME = "TARGET_KRAMER";

  private boolean xGetCrsPosAppended;


  public KramerMCTarget()
  {
    // leer
  }


  /*
   * Ermittlung der Cursor-Position
   * Rueckgabe:
   *   HL: Bit 0..5:  Spalte
   *       Bit 6..10: Zeile
   *   CY=1: Fehler -> HL=0FFFFH
   */
  protected void appendXGetCrsPosTo( AsmCodeBuf buf )
  {
    if( !this.xGetCrsPosAppended ) {
      buf.append( "X_GET_CRS_POS:\n"
		+ "\tLD\tHL,(0FF1H)\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t0FCH\n"
		+ "\tCP\t0FCH\n"
		+ "\tRET\tZ\n"			// CY=0
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
      this.xGetCrsPosAppended = true;
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendExitTo( AsmCodeBuf buf, boolean isAppTypeSub )
  {
    if( isAppTypeSub ) {
      buf.append( "\tRET\n" );
    } else {
      buf.append( "\tJP\t0000H\n" );
    }
  }


  @Override
  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0010H\n" );
  }


  @Override
  public void appendInputTo(
			AsmCodeBuf buf,
			boolean    xCheckBreak,
			boolean    xInkey,
			boolean    xInch,
			boolean    canBreakOnInput )
  {
    if( xCheckBreak ) {
      buf.append( "XCHECK_BREAK:\n" );
    }
    if( xCheckBreak || xInkey) {
      buf.append( "XINKEY:\tCALL\t00EFH\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n" );
    }
    if( xCheckBreak || xInkey || xInch ) {
      if( canBreakOnInput ) {
	buf.append( "XINCH:\n"
		+ "\tCALL\t00E0H\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
      } else {
	buf.append( "XINCH:\n"
		+ "\tJP\t00E0H\n" );
      }
    }
  }


  @Override
  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0040H\n" );
  }


  @Override
  public void appendXCrsLinTo( AsmCodeBuf buf )
  {
    buf.append( "XCRSLIN:\n"
		+ "\tCALL\tX_GET_CRS_POS\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t03H\n"
		+ "\tSLA\tL\n"
		+ "\tRLA\n"
		+ "\tSLA\tL\n"
		+ "\tRLA\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n" );
    appendXGetCrsPosTo( buf );
  }


  @Override
  public void appendXCrsPosTo( AsmCodeBuf buf )
  {
    buf.append( "XCRSPOS:\n"
		+ "\tCALL\tX_GET_CRS_POS\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t3FH\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n" );
    appendXGetCrsPosTo( buf );
  }


  /*
   * Ein- und Ausschalten des Cursors
   * Parameter:
   *   HL: 0:   Cursor ausschalten
   *       <>0: Cursor einschalten
   */
  @Override
  public void appendXCursorTo( AsmCodeBuf buf )
  {
    buf.append( "XCURSOR:\n"
		+ "\tLD\tA,H\n"
                + "\tOR\tL\n"
		+ "\tLD\tHL,0FF1H\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tJR\tZ,XCURSOR_1\n"
		+ "\tOR\t80H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n"
		+ "XCURSOR_1:\n"
		+ "\tAND\t7FH\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
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
    buf.append( "XLOCATE:\n"
		+ "\tLD\tA,0FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tBC,0FC00H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tHL,(0FF1H)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tAND\t7FH\n"
		+ "\tLD\t(HL),A\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\t(0FF1H),HL\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tOR\t80H\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXLPtchTo( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tC,A\n"
		+ "\tJP\t00ECH\n" );
  }


  @Override
  public void appendXOutchTo( AsmCodeBuf buf )
  {
    if( !this.xOutchAppended ) {
      buf.append( "XOUTCH:\tLD\tC,A\n"
		+ "\tJP\t00E6H\n" );
      this.xOutchAppended = true;
    }
  }


  @Override
  public void appendXOutnlTo( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tC,0DH\n"
		+ "\tCALL\t00E6H\n"
		+ "\tLD\tC,0AH\n"
		+ "\tJP\t00E6H\n" );
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
      if( emuSys instanceof KramerMC ) {
	rv = 2;
      }
    }
    return rv;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x1000;
  }


  @Override
  public String getStartCmd( EmuSys emuSys, String appName, int begAddr )
  {
    String rv = null;
    if( (emuSys != null) && (begAddr >= 0) ) {
      if( emuSys instanceof KramerMC ) {
	rv = String.format( "G%04X", begAddr );
      }
    }
    return rv;
  }


  @Override
  public void reset()
  {
    super.reset();
    this.xGetCrsPosAppended = false;
  }


  @Override
  public boolean supportsXCURSOR()
  {
    return true;
  }


  @Override
  public boolean supportsXLOCATE()
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
    return "Kramer-MC";
  }
}
