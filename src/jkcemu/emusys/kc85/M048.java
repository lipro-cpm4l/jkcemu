/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines 256K-User-PROM-Moduls M048 (16x16 KByte)
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.lang.*;


public class M048 extends AbstractKC85UserPROMModule
{
  public M048( int slot, Component owner, String fileName )
  {
    super( slot, 0x73, "M048", 16, 0x4000, owner, fileName );
  }


	/* --- ueberschriebene Methoden --- */

  // Steuerbyte: AASSSSxM
  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr = (value << 8) & 0xC000;
    this.segMask = (value << 12) & 0x3C000;
  }
}
