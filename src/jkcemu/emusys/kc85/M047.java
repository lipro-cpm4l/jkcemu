/*
 * (c) 2015-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines 128K-User-PROM-Moduls M047 (16x8 KByte)
 */

package jkcemu.emusys.kc85;

import java.awt.Component;


public class M047 extends AbstractKC85UserPROMModule
{
  public M047( int slot, int typeByte, Component owner, String fileName )
  {
    super( slot, typeByte, "M047", 16, 0x2000, owner, fileName );
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
