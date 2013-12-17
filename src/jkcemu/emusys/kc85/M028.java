/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines 16K-User-PROM-Moduls M028
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;


public class M028 extends AbstractKC85Module
{
  private int    typeByte;
  private int    begAddr;
  private String fileName;
  private byte[] rom;


  public M028(
	    int       slot,
	    EmuThread emuThread,
	    int       typeByte,
	    Component owner,
	    String    fileName )
  {
    super( slot );
    this.typeByte = typeByte;
    this.begAddr  = 0;
    this.fileName = fileName;
    reload( owner );
  }


	/* --- ueberschriebene Methoden --- */

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
    return "M028";
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
	&& (addr < (this.begAddr + 0x4000))
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
			0x4000,
			"M028 ROM-Datei" );
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr = (value << 8) & 0xC000;
  }
}
