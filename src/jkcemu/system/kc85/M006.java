/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des BASIC-Moduls M006
 */

package jkcemu.system.kc85;

import java.lang.*;
import jkcemu.base.*;


public class M006 extends AbstractKC85Module
{
  private static byte[] rom = null;


  public M006( int slot, EmuThread emuThread )
  {
    super( slot );
    if( rom == null ) {
      rom = EmuUtil.readResource(
				emuThread.getScreenFrm(),
				"/rom/kc85/m006.bin" );
    }
  }


	/* --- ueberschriebene Methoden --- */

  public String getModuleName()
  {
    return "M006";
  }


  public int getTypeByte()
  {
    return 0xFB;
  }


  public int readMemByte( int addr )
  {
    int rv = -1;
    if( this.enabled && (addr >= 0xC000) && (rom != null) ) {
      int idx = addr - 0xC000;
      if( idx < rom.length ) {
	rv = (int) rom[ idx ] & 0xFF;
      }
    }
    return rv;
  }
}

