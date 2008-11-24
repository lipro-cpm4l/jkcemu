/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer Zugriff auf den Arbeitsspeicher
 */

package z80emu;

import java.lang.*;


public interface Z80Memory extends Z80MemView
{
  public void setMemByte( int addr, int value );
  public void setMemWord( int addr, int value );
}

