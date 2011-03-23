/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des RAM-Moduls M022 (16 KByte Expander RAM)
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import java.util.Arrays;
import jkcemu.base.EmuThread;


public class M022 extends AbstractKC85Module
{
  private int     begAddr;
  private boolean readwrite;
  private byte[]  ram;


  public M022( int slot )
  {
    super( slot );
    this.begAddr   = 0;
    this.readwrite = false;
    this.ram       = new byte[ 0x4000 ];
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getModuleName()
  {
    return "M022";
  }


  @Override
  public int getTypeByte()
  {
    return 0xF4;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled
	&& (addr >= this.begAddr)
	&& (addr < (this.begAddr + this.ram.length)) )
    {
      rv = (int) this.ram[ addr - this.begAddr ] & 0xFF;
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON )
      Arrays.fill( this.ram, (byte) 0 );
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.readwrite = ((value & 0x02) != 0);
    this.begAddr   = (value << 8) & 0xC000;
  }


  @Override
  public int writeMemByte( int addr, int value )
  {
    int rv = 0;
    if( this.enabled
	&& (addr >= this.begAddr)
	&& (addr < (this.begAddr + this.ram.length)) )
    {
      if( this.readwrite ) {
	this.ram[ addr - this.begAddr ] = (byte) value;
	rv = 2;
      } else {
	rv = 1;
      }
    }
    return rv;
  }
}

