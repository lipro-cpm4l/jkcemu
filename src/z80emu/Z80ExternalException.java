/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Klasse fuer das externe Ausloesen einer Ausnahme
 *
 * Eine solche Ausnahme wird im Z80-Emulator nicht behandelt,
 * sondern nur durchgereicht.
 */

package z80emu;

import java.lang.*;


public class Z80ExternalException extends Exception
{
  private int addr;


  public Z80ExternalException( int addr )
  {
    this.addr = addr;
  }


  public Z80ExternalException()
  {
    this.addr = 0;
  }


  public int getAddress()
  {
    return this.addr;
  }
}

