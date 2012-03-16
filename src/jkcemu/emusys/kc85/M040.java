/*
 * (c) 2011 Jens Mueller
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
  private int    baseAddr;
  private int    baseAddrMask;
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
    this.baseAddr = 0;
    if( typeByte == 0xF8 ) {
      this.baseAddrMask = 0xC000;
      this.romLen       = 0x4000;
    } else {
      this.baseAddrMask = 0xE000;
      this.romLen       = 0x2000;
    }
    reload( owner );
  }


	/* --- ueberschriebene Methoden --- */

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
	&& (addr >= this.baseAddr)
	&& (addr < (this.baseAddr + this.romLen))
	&& (this.rom != null) )
    {
      int idx = addr - this.baseAddr;
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
			0x4000,
			"M040 ROM-Datei" );
    if( (this.typeByte == 0x01) && (this.rom != null) ) {
      if( this.rom.length > 0x2000 ) {
	this.baseAddrMask = 0xC000;
	this.romLen       = 0x4000;
      } else {
	this.baseAddrMask = 0xE000;
	this.romLen       = 0x2000;
      }
    }
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.baseAddr = (value << 8) & this.baseAddrMask;
  }
}

