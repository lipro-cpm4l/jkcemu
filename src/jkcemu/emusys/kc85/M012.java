/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des TEXOR-Moduls M012
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import jkcemu.base.EmuThread;


public class M012 extends AbstractROM8KModule
{
  public M012( int slot, EmuThread emuThread )
  {
    super( slot, emuThread, "M012", "/rom/kc85/m012.bin" );
  }
}

