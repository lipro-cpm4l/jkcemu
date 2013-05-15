/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * KramerMC-spezifische Code-Erzeugung des BASIC-Compilers
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import jkcemu.base.EmuSys;
import jkcemu.emusys.KramerMC;
import jkcemu.programming.basic.*;


public class KramerMCTarget extends AbstractTarget
{
  public KramerMCTarget()
  {
    // leer
  }


  @Override
  public void appendExit( AsmCodeBuf buf )
  {
    buf.append( "\tJP\t0000H\n" );
  }


  @Override
  public void appendHChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0010H\n" );
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
  public void appendWChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0040H\n" );
  }


  @Override
  public void appendXLPTCH( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tC,A\n"
		+ "\tJP\t00ECH\n" );
  }


  @Override
  public void appendXOUTCH( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tLD\tC,A\n"
		+ "\tJP\t00E6H\n" );
      this.xoutchAppended = true;
    }
  }


  @Override
  public void appendXOUTNL( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tC,0DH\n"
		+ "\tCALL\t00E6H\n"
		+ "\tLD\tC,0AH\n"
		+ "\tJP\t00E6H\n" );
  }


  /*
   * Target-ID-String
   */
  @Override
  public void appendXTARID( AsmCodeBuf buf )
  {
    buf.append( "XTARID:\tDB\t\'KRAMER\'\n"
		+ "\tDB\t00H\n" );
  }


  @Override
  public boolean createsCodeFor( EmuSys emuSys )
  {
    return emuSys != null ? (emuSys instanceof KramerMC) : false;
  }


  @Override
  public int get100msLoopCount()
  {
    return 42;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x1000;
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
