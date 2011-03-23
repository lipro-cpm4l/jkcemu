/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Haltepunkte auf eine Interruptquelle
 */

package jkcemu.tools.debugger;

import java.lang.*;
import z80emu.*;


public class InterruptBreakpoint extends AbstractBreakpoint
{
  private Z80InterruptSource iSource;


  public InterruptBreakpoint( Z80InterruptSource iSource )
  {
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


	/* --- Z80Breakpoint --- */

  @Override
  public boolean matches( Z80CPU cpu, Z80InterruptSource iSource )
  {
    return iSource == this.iSource;
  }
}
