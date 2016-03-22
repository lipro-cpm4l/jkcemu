/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines 128K-User-PROM-Moduls M047 (16x8 KByte)
 */

package jkcemu.emusys.kc85;

import java.awt.Component;
import java.lang.*;


public class M047 extends AbstractKC85UserPROMModule
{
  public M047( int slot, Component owner, String fileName )
  {
    super( slot, 0x72, "M047", 16, 0x2000, owner, fileName );
  }


	/* --- ueberschriebene Methoden --- */

  // Steuerbyte: AASSSSxM
  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr = (value << 8) & 0xC000;
    this.segMask = (value << 11) & 0x1E000;
  }
}
