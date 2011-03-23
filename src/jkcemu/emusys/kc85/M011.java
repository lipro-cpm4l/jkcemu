/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des RAM-Moduls M011 (64 KByte RAM)
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import java.util.Arrays;
import jkcemu.base.EmuThread;


public class M011 extends AbstractKC85Module
{
  private int     negMask;	// 1-Bits muessen in der Adresse negiert werden
  private boolean readwrite;
  private byte[]  ram;


  public M011( int slot )
  {
    super( slot );
    this.negMask   = 0;
    this.readwrite = false;
    this.ram       = new byte[ 0x10000 ];
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getModuleName()
  {
    return "M011";
  }


  @Override
  public int getTypeByte()
  {
    return 0xF6;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled ) {
      int idx = addr ^ this.negMask;
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
    this.negMask   = (value << 8) & 0xC000;
    this.readwrite = ((value & 0x02) != 0);
  }


  @Override
  public int writeMemByte( int addr, int value )
  {
    int rv = 0;
    if( this.enabled ) {
      int idx = addr ^ this.negMask;
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

