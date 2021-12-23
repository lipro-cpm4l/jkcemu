/*
 * (c) 2008-2017 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer Ein-/Ausgabebefehle
 */

package z80emu;


public interface Z80IOSystem
{
  public int  readIOByte( int port, int tStates );
  public void writeIOByte( int port, int value, int tStates );
}

