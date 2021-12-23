/*
 * (c) 2015-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines 32K-User-PROM-Moduls M045 (4x8 KByte)
 */

package jkcemu.emusys.kc85;

import java.awt.Component;


public class M045 extends AbstractKC85UserPROMModule
{
  public M045( int slot, int typeByte, Component owner, String fileName )
  {
    super( slot, typeByte, "M045", 4, 0x2000, owner, fileName );
  }


	/* --- ueberschriebene Methoden --- */

  // Steuerbyte: AASSxxxM
  @Override
  public void setStatus( int value )
  {
    super.setStatus( value );
    this.begAddr = (value << 8) & 0xC000;
    this.segMask = (value << 9) & 0x6000;
  }
}
