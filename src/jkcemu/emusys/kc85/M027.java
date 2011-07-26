/*
 * (c) 2010-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Development-Moduls M027
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import jkcemu.base.EmuThread;


public class M027 extends AbstractROM8KModule
{
  public M027( int slot, EmuThread emuThread )
  {
    super( slot, emuThread, "M027", "/rom/kc85/m027.bin" );
  }
}

