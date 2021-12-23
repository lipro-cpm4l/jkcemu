/*
 * (c) 2008-2017 Jens Mueller
 *
 * Z80-Emulator
 *
 * Interface fuer Zugriff auf den Arbeitsspeicher
 */

package z80emu;


public interface Z80Memory extends Z80MemView
{
  public boolean setMemByte( int addr, int value );

  /*
   * Diese beiden Methoden werden von der Emulation aufgerufen,
   * d.h., wenn ein Speicherzugriff zu WAIT-States fuehrt,
   * muessen diese mit emuliert werden.
   */
  public int  readMemByte( int addr, boolean m1 );
  public void writeMemByte( int addr, int value );
}

