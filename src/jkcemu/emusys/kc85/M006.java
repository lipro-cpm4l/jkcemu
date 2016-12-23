/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des BASIC-Moduls M006
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;


public class M006 extends AbstractKC85Module
{
  private static byte[] rom = null;

  private int begAddr;


  public M006( int slot, EmuThread emuThread )
  {
    super( slot );
    this.begAddr = 0;
    if( rom == null ) {
      rom = EmuUtil.readResource(
				emuThread.getScreenFrm(),
				"/rom/kc85/m006.bin" );
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
    return "M006";
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
	&& (addr < (this.begAddr + 0x4000))
	&& (rom != null) )
    {
      int idx = addr - this.begAddr;
      if( idx < rom.length ) {
	rv = (int) rom[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr = (value << 8) & 0xC000;
  }
}
