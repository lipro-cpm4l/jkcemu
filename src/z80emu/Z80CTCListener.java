/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer das Empfangen von Nulldurchgaengen von CTC-Kanaelen
 */

package z80emu;

import java.lang.*;


public interface Z80CTCListener
{
  public void z80CTCUpdate( Z80CTC ctc, int timerNum );
}

