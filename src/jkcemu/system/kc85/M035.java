/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des RAM-Moduls M035 (1 MByte Segmented RAM)
 */

package jkcemu.system.kc85;

import java.lang.*;
import java.util.Arrays;
import jkcemu.base.EmuThread;


public class M035 extends AbstractKC85Module
{
  private int     segMask;
  private boolean readwrite;
  private byte[]  ram;


  public M035( int slot )
  {
    super( slot );
    this.segMask   = 0;
    this.readwrite = false;
    this.ram       = new byte[ 0x100000 ];
  }


	/* --- ueberschriebene Methoden --- */

  public String getModuleName()
  {
    return "M035";
  }


  public int getTypeByte()
  {
    return 0x7B;
  }


  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled && (addr >= 0x8000) && (addr < 0xC000) ) {
      int idx = (addr - 0x8000) | this.segMask;
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
    this.segMask   = (value << 12) & 0xFC000;
    this.readwrite = ((value & 0x02) != 0);
  }


  public int writeMemByte( int addr, int value )
  {
    int rv = 0;
    if( this.enabled && (addr >= 0x8000) && (addr < 0xC000) ) {
      int idx = (addr - 0x8000) | this.segMask;
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

