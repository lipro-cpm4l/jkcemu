/*
 * (c) 2009-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des segmentierten RAM-Moduls
 */

package jkcemu.emusys.kc85;

import jkcemu.base.EmuUtil;


public class KC85SegmentedRAMModule extends AbstractKC85Module
{
  private String  moduleName;
  private int     typeByte;
  private int     begAddr;
  private int     segMask;
  private boolean readWrite;
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
    this.begAddr    = 0;
    this.segMask    = 0;
    this.readWrite  = false;
    this.ram        = new byte[ ramSize ];
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int getBegAddr()
  {
    return this.begAddr;
  }


  @Override
  public String getModuleName()
  {
    return this.moduleName;
  }


  @Override
  public Boolean getReadWrite()
  {
    return this.readWrite;
  }


  @Override
  public int getSegmentNum()
  {
    return (this.segMask >> 14) & 0x1F;
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
	&& (addr >= this.begAddr)
	&& (addr < (this.begAddr + 0x4000)) )
    {
      int idx = (addr - this.begAddr) | this.segMask;
      if( (idx >= 0) && (idx < this.ram.length) ) {
	rv = (int) this.ram[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  public void reset( boolean powerOn )
  {
    if( powerOn )
      EmuUtil.initDRAM( this.ram );
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr   = (value & 0x80) != 0 ? 0x8000 : 0x4000;
    this.segMask   = (value << 12) & (this.ram.length - 1) & 0x7C000;
    this.readWrite = ((value & 0x02) != 0);
  }


  @Override
  public int writeMemByte( int addr, int value )
  {
    int rv = 0;
    if( this.enabled
	&& (addr >= this.begAddr)
	&& (addr < (this.begAddr + 0x4000)) )
    {
      int idx = (addr - this.begAddr) | this.segMask;
      if( (idx >= 0) && (idx < this.ram.length) ) {
	if( this.readWrite ) {
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
