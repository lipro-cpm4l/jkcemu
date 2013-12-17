/*
 * (c) 2008-2013 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer eine Interrupt-Quelle
 */

package z80emu;

import java.lang.*;


public interface Z80InterruptSource
{
  public void    appendInterruptStatusHTMLTo( StringBuilder buf );
  public int     interruptAccept();		// RET: Interrupt-Vektor
  public void    interruptFinish();
  public boolean isInterruptAccepted();
  public boolean isInterruptRequested();
  public void    reset( boolean powerOn );
}

