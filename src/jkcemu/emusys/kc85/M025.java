/*
 * (c) 2011-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des 8K-User-PROM-Moduls M025
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.lang.*;


public class M025 extends AbstractKC85UserPROMModule
{
  public M025( int slot, int typeByte, Component owner, String fileName )
  {
    super( slot, typeByte, "M025", 1, 0x2000, owner, fileName );
  }


	/* --- ueberschriebene Methoden --- */

  // Steuerbyte: AAAxxxxM
  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr = (value << 8) & 0xE000;
  }
}
