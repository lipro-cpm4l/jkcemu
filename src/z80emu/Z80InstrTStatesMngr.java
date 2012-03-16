/*
 * (c) 2011 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer einen Manager,
 * der die Anzahl der TStates der einzelnen CPU-Befehle beeinflussen kann.
 */

package z80emu;

import java.lang.*;


public interface Z80InstrTStatesMngr
{
  public int z80IntructionProcessed( Z80CPU cpu, int pc, int tStates );
}

