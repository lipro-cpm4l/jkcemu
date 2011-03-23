/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines 8K-ROM-Moduls mit dem Strukturbyte 0xFB
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import java.util.*;
import jkcemu.base.*;


public class AbstractROM8KModule extends AbstractKC85Module
{
  private static Map<String,byte[]> map = new HashMap<String,byte[]>();

  private String moduleName;
  private int    baseAddr;
  private byte[] rom;


  public AbstractROM8KModule(
		int       slot,
		EmuThread emuThread,
		String    moduleName,
		byte[]    rom )
  {
    super( slot );
    this.baseAddr   = 0;
    this.moduleName = moduleName;
    this.rom        = rom;
  }


  public AbstractROM8KModule(
		int       slot,
		EmuThread emuThread,
		String    moduleName,
		String    resource )
  {
    super( slot );
    this.baseAddr = 0;
    this.rom      = map.get( resource );
    if( this.rom == null ) {
      this.rom = EmuUtil.readResource( emuThread.getScreenFrm(), resource );
      map.put( resource, this.rom );
    }
  }


  protected void setROM( byte[] rom )
  {
    this.rom = rom;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getModuleName()
  {
    return this.moduleName;
  }


  @Override
  public int getTypeByte()
  {
    return 0xFB;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled
	&& (addr >= this.baseAddr)
	&& (addr < (this.baseAddr + 0x2000))
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
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.baseAddr = (value << 8) & 0xE000;
  }
}

