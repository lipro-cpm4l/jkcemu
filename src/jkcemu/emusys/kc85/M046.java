/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines 64K-User-PROM-Moduls M046 (8x8 KByte)
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.lang.*;


public class M046 extends AbstractKC85UserPROMModule
{
  public M046( int slot, Component owner, String fileName )
  {
    super( slot, 0x71, "M046", 8, 0x2000, owner, fileName );
  }


	/* --- ueberschriebene Methoden --- */

  // Steuerbyte: AASSxSxM
  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr = (value << 8) & 0xC000;
    this.segMask = ((value << 10) & 0xC000) | ((value << 11) & 0x2000);
  }
}
