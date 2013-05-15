/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * CP/M-spezifische Code-Erzeugung des BASIC-Compilers
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import jkcemu.base.EmuSys;
import jkcemu.emusys.PCM;
import jkcemu.programming.basic.*;


public class CPMTarget extends AbstractTarget
{
  public CPMTarget()
  {
    // leer
  }


  @Override
  public void appendExit( AsmCodeBuf buf )
  {
    buf.append( "\tJP\t0000H\n" );
  }


  @Override
  public void appendInput(
			AsmCodeBuf buf,
			boolean    xckbrk,
			boolean    xinkey,
			boolean    xinch,
			boolean    canBreakOnInput )
  {
    if( xinch ) {
      buf.append( "XINCH:\tCALL\tXINKEY\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,XINCH\n"
		+ "\tRET\n" );
      xinkey = true;
    }
    if( xckbrk ) {
      buf.append( "XCKBRK:\n" );
    }
    if( xckbrk || xinkey) {
      buf.append( "XINKEY:\tLD\tC,06H\n"
		+ "\tLD\tE,0FFH\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCALL\t0005H\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
      } else {
	buf.append( "\tJP\t0005H\n" );
      }
    }
  }


  @Override
  public void appendXLOCATE( AsmCodeBuf buf )
  {
    buf.append( "XLOCATE:\n"
		+ "\tPUSH\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tA,1BH\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tA,E\n"
		+ "\tOR\t80H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\t80H\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tRET\n" );
    appendXOUTCH( buf );
  }


  @Override
  public void appendXLPTCH( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tC,5\n"
		+ "\tLD\tE,A\n"
		+ "\tJP\t0005H\n" );
  }


  @Override
  public void appendXOUTCH( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tLD\tC,02H\n"
		+ "\tLD\tE,A\n"
		+ "\tJP\t0005H\n" );
      this.xoutchAppended = true;
    }
  }


  @Override
  public void appendXOUTNL( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tC,02H\n"
		+ "\tLD\tE,0DH\n"
		+ "\tCALL\t0005H\n"
		+ "\tLD\tC,02H\n"
		+ "\tLD\tE,0AH\n"
		+ "\tJP\t0005H\n" );
  }


  /*
   * Target-ID-String
   */
  @Override
  public void appendXTARID( AsmCodeBuf buf )
  {
    buf.append( "XTARID:\tDB\t\'CP/M\'\n"
		+ "\tDB\t00H\n" );
  }


  @Override
  public boolean createsCodeFor( EmuSys emuSys )
  {
    return emuSys != null ? (emuSys instanceof PCM) : false;
  }


  @Override
  public int get100msLoopCount()
  {
    return 69;
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
    return "CP/M-kompatibel";
  }
}
