/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer eine Interrupt-Quelle
 */

package z80emu;

import java.lang.*;


public interface Z80InterruptSource
{
  public int     z80GetInterruptVector();
  public void    z80InterruptFinished();
  public boolean z80IsInterruptPending();
}

