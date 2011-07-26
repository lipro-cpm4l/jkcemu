/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer das Empfangen von Ereignissen,
 * wenn sich die am Adressbus anliegende Adresse geaendert hat.
 */

package z80emu;

import java.lang.*;


public interface Z80AddressListener
{
  public void z80AddressChanged( int addr );
}

