/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Emulation von KC85-User-PROM-Modulen
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.lang.*;
import jkcemu.base.EmuUtil;


public abstract class AbstractKC85UserPROMModule extends AbstractKC85Module
{
  protected int begAddr;
  protected int segMask;

  private int    typeByte;
  private int    segSize;
  private int    fullSize;
  private String moduleName;
  private String fileName;
  private byte[] rom;


  public AbstractKC85UserPROMModule(
			int       slot,
			int       typeByte,
			String    moduleName,
			int       segCount,
			int       segSize,
			Component owner,
			String    fileName )
  {
    super( slot );
    this.typeByte   = typeByte;
    this.segSize    = segSize;
    this.fullSize   = segCount * segSize;
    this.moduleName = moduleName;
    this.fileName   = fileName;
    this.begAddr    = 0;
    this.segMask    = 0;
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
	&& (addr >= this.begAddr)
	&& (addr < (this.begAddr + this.segSize))
	&& (this.rom != null) )
    {
      int idx = (addr - this.begAddr) | this.segMask;
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
			this.fullSize,
			this.moduleName + " ROM-Datei" );
  }
}
