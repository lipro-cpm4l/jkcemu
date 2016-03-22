/*
 * (c) 2011-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines 8K/16K-User-PROM-Moduls M040
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;


public class M040 extends AbstractKC85Module
{
  private int    typeByte;
  private int    begAddr;
  private int    begAddrMask;
  private int    romLen;
  private byte[] rom;
  private String fileName;


  public M040(
	    int       slot,
	    EmuThread emuThread,
	    int       typeByte,
	    Component owner,
	    String    fileName )
  {
    super( slot );
    this.typeByte = typeByte;
    this.fileName = fileName;
    this.begAddr  = 0;
    if( typeByte == 0xF8 ) {
      this.begAddrMask = 0xC000;
      this.romLen      = 0x4000;
    } else {
      this.begAddrMask = 0xE000;
      this.romLen      = 0x2000;
    }
    reload( owner );
  }


	/* --- ueberschriebene Methoden --- */

  public void appendEtcInfoHTMLTo( StringBuilder buf )
  {
    buf.append( "ROM-Gr\u00F6\u00DFe: " );
    buf.append( this.romLen / 1024 );
    buf.append( " KByte" );
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
    return "M040";
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
	&& (addr < (this.begAddr + this.romLen))
	&& (this.rom != null) )
    {
      int idx = addr - this.begAddr;
      if( idx < this.rom.length ) {
	rv = (int) this.rom[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  public void reload( Component owner )
  {
    this.rom = EmuUtil.readFile(
			owner,
			this.fileName,
			true,
			0x4000,
			"M040 ROM-Datei" );
    if( (this.typeByte == 0x01) && (this.rom != null) ) {
      if( this.rom.length > 0x2000 ) {
	this.begAddrMask = 0xC000;
	this.romLen      = 0x4000;
      } else {
	this.begAddrMask = 0xE000;
	this.romLen      = 0x2000;
      }
    }
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr = (value << 8) & this.begAddrMask;
  }
}
