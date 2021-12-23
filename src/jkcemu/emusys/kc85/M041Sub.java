/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines M041-Teilmoduls mit 16K ROM
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import jkcemu.file.FileUtil;


public class M041Sub extends AbstractKC85Module
{
  public static final String MODULE_NAME = "M041-Teilmodul";

  private int     typeByte;
  private int     begAddr;
  private String  fileName;
  private boolean swapped;
  private byte[]  rom;
  private M041Sub m041Sub0;


  public M041Sub(
		int       slot,
		int       typeByte,
		Component owner,
		String    fileName,
		M041Sub   m041Sub0 )
  {
    super( slot );
    this.typeByte = typeByte;
    this.fileName = fileName;
    this.m041Sub0 = m041Sub0;
    this.begAddr  = 0xC000;
    this.swapped  = false;
    this.rom      = null;
    reload( owner );
  }


	/* --- ueberschriebene Methoden --- */

  public void appendEtcInfoHTMLTo( StringBuilder buf )
  {
    buf.append( "8 KByte Bereiche" );
    if( !this.swapped ) {
      buf.append( " nicht" );
    }
    buf.append( " rotiert" );
  }


  @Override
  public int getBegAddr()
  {
    return this.begAddr;
  }


  @Override
  public String getFileName()
  {
    return this.fileName;
  }


  @Override
  public String getModuleName()
  {
    return MODULE_NAME;
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
      int idx = addr - this.begAddr;
      if( this.swapped ) {
	idx ^= 0x2000;
      }
      rv = getMemByteByIdx( idx );
    }
    return rv;
  }


  @Override
  public void reload( Component owner )
  {
    if( this.m041Sub0 == null ) {
      this.rom = FileUtil.readFile(
				owner,
				this.fileName,
				true,
				0x8000,
				"M041 ROM-Datei" );
    }
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr = (value << 8) & 0xC000;
    this.swapped = ((value & 0x10) != 0);
  }


	/* --- private Methoden --- */

  private int getMemByteByIdx( int idx )
  {
    int rv = -1;
    if( this.m041Sub0 != null ) {
      rv = this.m041Sub0.getMemByteByIdx(
				idx + ((this.slot & 0x03) * 0x4000) );
    } else if( this.rom != null ) {
      if( (idx >= 0) && (idx < this.rom.length) ) {
	rv = (int) this.rom[ idx ] & 0xFF;
      }
    }
    return rv;
  }
}
