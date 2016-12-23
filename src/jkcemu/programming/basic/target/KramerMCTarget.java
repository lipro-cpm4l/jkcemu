/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * KramerMC-spezifische Code-Erzeugung des BASIC-Compilers
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import jkcemu.base.EmuSys;
import jkcemu.emusys.KramerMC;
import jkcemu.programming.basic.AbstractTarget;
import jkcemu.programming.basic.AsmCodeBuf;


public class KramerMCTarget extends AbstractTarget
{
  public static final String BASIC_TARGET_NAME = "TARGET_KRAMER";

  public KramerMCTarget()
  {
    // leer
  }


  @Override
  public void appendExitTo( AsmCodeBuf buf )
  {
    buf.append( "\tJP\t0000H\n" );
  }


  @Override
  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0010H\n" );
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
    }
    if( xckbrk || xinkey) {
      buf.append( "XINKEY:\tCALL\t00EFH\n"
		+ "\tOR\tA\n"
		+ "\tRET\tZ\n" );
    }
    if( xckbrk || xinkey || xinch ) {
      if( canBreakOnInput ) {
	buf.append( "XINCH:\tCALL\t00E0H\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
      } else {
	buf.append( "XINCH:\tJP\t00E0H\n" );
      }
    }
  }


  @Override
  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0040H\n" );
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
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tLD\tC,A\n"
		+ "\tJP\t00E6H\n" );
      this.xoutchAppended = true;
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
	rv = 3;
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
