/*
 * (c) 2011-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Halte-/Log-Punkte auf eine Interruptquelle
 */

package jkcemu.tools.debugger;

import java.lang.*;
import z80emu.*;


public class InterruptBreakpoint extends AbstractBreakpoint
{
  private Z80InterruptSource iSource;


  public InterruptBreakpoint( DebugFrm debugFrm, Z80InterruptSource iSource )
  {
    super( debugFrm );
    this.iSource = iSource;
    setText( iSource.toString() );
  }


  public Z80InterruptSource getInterruptSource()
  {
    return this.iSource;
  }


  public void setInterruptSource( Z80InterruptSource iSource )
  {
    this.iSource = iSource;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean matchesImpl( Z80CPU cpu, Z80InterruptSource iSource )
  {
    return iSource == this.iSource;
  }
}
