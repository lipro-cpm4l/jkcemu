/*
 * (c) 2008-2009 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer eine Interrupt-Quelle
 */

package z80emu;

import java.lang.*;


public interface Z80InterruptSource
{
  public int     interruptAccepted();		// RET: Interrupt-Vektor
  public void    interruptFinished();
  public boolean isInterruptPending();
  public boolean isInterruptRequested();
  public void    reset();
}

