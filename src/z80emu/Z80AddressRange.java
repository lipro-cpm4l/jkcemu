/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Beschreibung eines Adressbereichs
 */

package z80emu;

import java.lang.*;


public class Z80AddressRange
{
  private int addr;
  private int len;


  public Z80AddressRange( int addr, int len )
  {
    this.addr = addr;
    this.len  = len;
  }


  public int getAddress()
  {
    return this.addr;
  }


  public int getLength()
  {
    return this.len;
  }


  public boolean isInAddressRange( int addr )
  {
    return (addr >= this.addr) && (addr < this.addr + this.len);
  }
}

