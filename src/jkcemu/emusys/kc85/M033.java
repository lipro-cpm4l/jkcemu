/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des TypeStar-Moduls M033
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import jkcemu.base.*;


public class M033 extends AbstractKC85Module
{
  private static byte[] rom = null;

  private int baseAddr;
  private int segMask;


  public M033( int slot, EmuThread emuThread )
  {
    super( slot );
    this.baseAddr = 0;
    this.segMask  = 0;
    if( rom == null ) {
      rom = EmuUtil.readResource(
				emuThread.getScreenFrm(),
				"/rom/kc85/m033.bin" );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getModuleName()
  {
    return "M033";
  }


  @Override
  public int getTypeByte()
  {
    return 0x01;
  }


  @Override
  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled
	&& (addr >= this.baseAddr) && (addr < (this.baseAddr + 0x2000))
	&& (rom != null) )
    {
      int idx = (addr - this.baseAddr) | this.segMask;
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
    this.baseAddr = (value << 8) & 0xC000;
    this.segMask  = (value & 0x10) != 0 ? 0x2000 : 0;
  }
}

