/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des RAM-Moduls M032 (256 KByte Segmented RAM)
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import java.util.Arrays;
import jkcemu.base.EmuThread;


public class M032 extends AbstractKC85Module
{
  private int     baseAddr;
  private int     segMask;
  private boolean readwrite;
  private byte[]  ram;


  public M032( int slot )
  {
    super( slot );
    this.baseAddr  = 0;
    this.segMask   = 0;
    this.readwrite = false;
    this.ram       = new byte[ 0x40000 ];
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getModuleName()
  {
    return "M032";
  }


  @Override
  public int getTypeByte()
  {
    return 0x79;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled
	&& (addr >= this.baseAddr)
	&& (addr < (this.baseAddr + 0x4000)) )
    {
      int idx = (addr - this.baseAddr) | this.segMask;
      if( (idx >= 0) && (idx < this.ram.length) ) {
	rv = (int) this.ram[ idx ] & 0xFF;
      }
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
    this.baseAddr  = (value & 0x80) != 0 ? 0x8000 : 0x4000;
    this.segMask   = (value << 12) & 0x3C000;
    this.readwrite = ((value & 0x02) != 0);
  }


  @Override
  public int writeMemByte( int addr, int value )
  {
    int rv = 0;
    if( this.enabled
	&& (addr >= this.baseAddr)
	&& (addr < (this.baseAddr + 0x4000)) )
    {
      int idx = (addr - this.baseAddr) | this.segMask;
      if( (idx >= 0) && (idx < this.ram.length) ) {
	if( this.readwrite ) {
	  this.ram[ idx ] = (byte) value;
	  rv = 2;
	} else {
	  rv = 1;
	}
      }
    }
    return rv;
  }
}

