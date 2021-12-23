/*
 * (c) 2008-2019 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer eine Interrupt-Quelle
 */

package z80emu;


public interface Z80InterruptSource
{
  public void    appendInterruptStatusHTMLTo( StringBuilder buf );
  public int     interruptAccept();		// RET: Interrupt-Vektor
  public boolean interruptFinish( int retiAddr );
  public boolean isInterruptAccepted();
  public boolean isInterruptRequested();
}

