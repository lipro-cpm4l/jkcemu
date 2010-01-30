/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des RAM-Moduls M036 (128 KByte Segmented RAM)
 */

package jkcemu.system.kc85;

import java.lang.*;
import java.util.Arrays;
import jkcemu.base.EmuThread;


public class M036 extends AbstractKC85Module
{
  private int     baseAddr;
  private int     segMask;
  private boolean readwrite;
  private byte[]  ram;


  public M036( int slot )
  {
    super( slot );
    this.baseAddr  = 0;
    this.segMask   = 0;
    this.readwrite = false;
    this.ram       = new byte[ 0x20000 ];
  }


	/* --- ueberschriebene Methoden --- */

  public String getModuleName()
  {
    return "M036";
  }


  public int getTypeByte()
  {
    return 0x78;
  }


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


  public void reset( EmuThread.ResetLevel resetLevel )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON )
      Arrays.fill( this.ram, (byte) 0 );
  }


  public void setStatus( int value )
  {
    super.setStatus( value );
    this.baseAddr  = (value & 0x80) != 0 ? 0x8000 : 0x4000;
    this.segMask   = (value << 12) & 0x1C000;
    this.readwrite = ((value & 0x02) != 0);
  }


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
