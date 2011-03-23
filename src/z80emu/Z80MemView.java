/*
 * (c) 2008-2009 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer die Sicht auf den Arbeitsspeicher
 */

package z80emu;

import java.lang.*;


public interface Z80MemView
{
  public int getMemByte( int addr, boolean m1 );
  public int getMemWord( int addr );
}

