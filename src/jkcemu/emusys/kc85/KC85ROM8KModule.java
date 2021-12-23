/*
 * (c) 2011-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines 8K-ROM-Moduls mit dem Strukturbyte 0xFB
 */

package jkcemu.emusys.kc85;

import java.util.HashMap;
import java.util.Map;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;


public class KC85ROM8KModule extends AbstractKC85Module
{
  private static Map<String,byte[]> map = new HashMap<>();

  private String moduleName;
  private int    begAddr;
  private byte[] rom;


  public KC85ROM8KModule(
		int       slot,
		EmuThread emuThread,
		String    moduleName,
		byte[]    rom )
  {
    super( slot );
    this.begAddr    = 0;
    this.moduleName = moduleName;
    this.rom        = rom;
  }


  public KC85ROM8KModule(
		int       slot,
		EmuThread emuThread,
		String    moduleName,
		String    resource )
  {
    super( slot );
    this.begAddr    = 0;
    this.moduleName = moduleName;
    this.rom        = map.get( resource );
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
  public int getTypeByte()
  {
    return 0xFB;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled
	&& (addr >= this.begAddr)
	&& (addr < (this.begAddr + 0x2000))
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
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr = (value << 8) & 0xE000;
  }
}
