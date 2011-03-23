/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer das Empfangen von verarbeiteten Taktzyklen
 */

package z80emu;

import java.lang.*;


public interface Z80TStatesListener
{
  public void z80TStatesProcessed( Z80CPU cpu, int tStates );
}

