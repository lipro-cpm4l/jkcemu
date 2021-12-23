/*
 * (c) 2009-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des nichtsegmentierten RAM-Moduls
 */

package jkcemu.emusys.kc85;

import java.util.Arrays;
import jkcemu.base.EmuUtil;


public class KC85PlainRAMModule extends AbstractKC85Module
{
  private String  moduleName;
  private int     typeByte;
  private int     begAddr;
  private boolean cmos;
  private boolean readWrite;
  private byte[]  ram;


  public KC85PlainRAMModule(
			int     slot,
			int     typeByte,
			String  moduleName,
			int     ramSize,
			boolean cmos )
  {
    super( slot );
    this.typeByte   = typeByte;
    this.moduleName = moduleName;
    this.cmos       = cmos;
    this.begAddr    = 0;
    this.readWrite  = false;
    this.ram        = new byte[ ramSize ];
    if( cmos ) {
      Arrays.fill( this.ram, (byte) 0 );
    }
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
	&& (addr < (this.begAddr + this.ram.length)) )
    {
      rv = (int) this.ram[ addr - this.begAddr ] & 0xFF;
    }
    return rv;
  }


  @Override
  public void reset( boolean powerOn )
  {
    if( powerOn && !this.cmos )
      EmuUtil.initDRAM( this.ram );
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.readWrite = ((value & 0x02) != 0);
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
      if( this.readWrite ) {
	this.ram[ addr - this.begAddr ] = (byte) value;
	rv = 2;
      } else {
	rv = 1;
      }
    }
    return rv;
  }
}
