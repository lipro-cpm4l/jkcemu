/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer das Empfangen von Zustandsaenderungsereignissen
 */

package z80emu;

import java.lang.*;


public interface Z80StatusListener
{
  public void z80StatusChanged();
}

