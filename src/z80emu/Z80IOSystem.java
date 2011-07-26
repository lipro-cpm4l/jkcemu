/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer Ein-/Ausgabebefehle
 */

package z80emu;

import java.lang.*;


public interface Z80IOSystem
{
  public int  readIOByte( int port );
  public void writeIOByte( int port, int value );
}

