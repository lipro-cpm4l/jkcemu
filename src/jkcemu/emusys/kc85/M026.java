/*
 * (c) 2010-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Forth-Moduls M026
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import jkcemu.base.EmuThread;


public class M026 extends AbstractROM8KModule
{
  public M026( int slot, EmuThread emuThread )
  {
    super( slot, emuThread, "M026", "/rom/kc85/m026.bin" );
  }
}

