/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Haltepunkte auf eine Programmadresse bzw. Adressbereich
 */

package jkcemu.tools.debugger;

import java.lang.*;
import z80emu.*;


public class PCBreakpoint extends AbstractBreakpoint
{
  protected int addr;


  public PCBreakpoint( int addr )
  {
    this.addr = addr & 0xFFFF;
    createAndSetText( this.addr, -1, false, -1, -1 );
  }


  public int getAddress()
  {
    return this.addr;
  }


	/* --- Z80Breakpoint --- */

  @Override
  public boolean matches( Z80CPU cpu, Z80InterruptSource iSource )
  {
    return cpu.getRegPC() == this.addr;
  }
}
