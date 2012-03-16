/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des segmentierten RAM-Moduls
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import java.util.Arrays;
import jkcemu.base.EmuThread;


public class KC85SegmentedRAMModule extends AbstractKC85Module
{
  private String  moduleName;
  private int     typeByte;
  private int     baseAddr;
  private int     segMask;
  private boolean readwrite;
  private byte[]  ram;


  public KC85SegmentedRAMModule(
			int     slot,
			int     typeByte,
			String  moduleName,
			int     ramSize )
  {
    super( slot );
    this.typeByte   = typeByte;
    this.moduleName = moduleName;
    this.baseAddr   = 0;
    this.segMask    = 0;
    this.readwrite  = false;
    this.ram        = new byte[ ramSize ];
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void clearRAM()
  {
    Arrays.fill( this.ram, (byte) 0 );
  }


  @Override
  public String getModuleName()
  {
    return this.moduleName;
  }


  @Override
  public int getTypeByte()
  {
    return this.typeByte;
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
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.baseAddr  = (value & 0x80) != 0 ? 0x8000 : 0x4000;
    this.segMask   = (value << 12) & (this.ram.length - 1) & 0x7C000;
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

