/*
 * (c) 2008-2017 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer das Empfangen von verarbeiteten Taktzyklen
 */

package z80emu;


public interface Z80TStatesListener
{
  public void z80TStatesProcessed( Z80CPU cpu, int tStates );
}

