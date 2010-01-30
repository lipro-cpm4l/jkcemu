/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer ein KC85-Modul
 */

package jkcemu.system.kc85;

import java.lang.*;
import jkcemu.base.EmuThread;


public abstract class AbstractKC85Module
{
  protected int     slot;
  protected boolean enabled;


  protected AbstractKC85Module( int slot )
  {
    this.slot    = slot;
    this.enabled = false;
  }


  public void die()
  {
    // leer;
  }


  public int getSlot()
  {
    return this.slot;
  }


  public abstract String getModuleName();
  public abstract int    getTypeByte();


  public boolean isEnabled()
  {
    return this.enabled;
  }


  /*
   * Rueckgabewert:
   *  -1: Modul bedient diesen Lesevorgang nicht.
   */
  public int readIOByte( int port )
  {
    return -1;
  }


  /*
   * Rueckgabewert:
   *  -1: Modul bedient diesen Lesevorgang nicht.
   */
  public int readMemByte( int addr )
  {
    return -1;
  }


  public void reset( EmuThread.ResetLevel resetLevel )
  {
    // leer
  }


  public void setStatus( int value )
  {
    this.enabled = ((value & 0x01) != 0);
  }


  /*
   * Rueckgabewert:
   *  false: Modul bedient diesen Schreibvorgang nicht.
   */
  public boolean writeIOByte( int port, int value )
  {
    return false;
  }


  /*
   * Rueckgabewert:
   *  0: Modul bedient diesen Schreibvorgang nicht.
   *  1: Modul bedient prinzipiell diesen Schreibvorgang,
   *     es ist jedoch der Schreibschutz aktiv.
   *  2: Modul hat den Schreibvorgang ausgefuehrt.
   */
  public int writeMemByte( int addr, int value )
  {
    return 0;
  }
}

