/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer das Empfangen von Ereignissen,
 * wenn die Befehlsausfuehrung eine Adresse in einem
 * bestimmten Bereich erreicht hat.
 */

package z80emu;

import java.lang.*;


public interface Z80AddressRangeListener
{
  public void z80AddressReached( int addr ) throws Z80ExternalException;
}

