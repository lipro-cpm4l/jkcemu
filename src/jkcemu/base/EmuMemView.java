/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Interface fuer Zugriff auf den Arbeitsspeicher
 */

package jkcemu.base;

import java.lang.*;
import z80emu.Z80MemView;


public interface EmuMemView extends Z80MemView
{
  public int getBasicMemByte( int addr );
}
