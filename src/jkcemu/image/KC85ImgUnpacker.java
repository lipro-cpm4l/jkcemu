/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Entpacken von komprimierten KC85-Bildern
 */

package jkcemu.image;

import java.lang.*;
import java.util.Arrays;
import jkcemu.base.EmuUtil;
import z80emu.Z80CPU;
import z80emu.Z80Memory;
import z80emu.Z80PCListener;
import z80emu.Z80TStatesListener;


public class KC85ImgUnpacker implements
				Z80Memory,
				Z80PCListener,
				Z80TStatesListener

{
  private boolean cancelled;
  private long    totalTStates;
  private Z80CPU  cpu;
  private byte[]  memory;


  public KC85ImgUnpacker()
  {
    this.cancelled    = false;
    this.totalTStates = 0;
    this.cpu          = new Z80CPU( this, null );
    this.memory       = new byte[ 0x10000 ];
    Arrays.fill( this.memory, (byte) 0xFF );
  }


  public synchronized boolean unpack( int startAddr )
  {
    this.cancelled    = false;
    this.totalTStates = 0;
    this.cpu.addPCListener( this, 0x0038 );
    this.cpu.addTStatesListener( this );
    this.cpu.setRegSP( 0x0000 );
    this.cpu.setRegPC( startAddr );
    this.cpu.run();
    this.cpu.removeTStatesListener( this );
    this.cpu.removePCListener( this );
    return !this.cancelled;
  }


	/* --- Z80TStatesListener --- */

  /*
   * Die Entpackroutine benoetigt ueblicherweise deutlich weniger
   * als 500000 Taktzyklen.
   * Zur Sicherheit wird nach 2 Mio. Taktzyklen abgebrochen.
   */
  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.totalTStates += tStates;
    if( this.totalTStates > 2000000 ) {
      this.cancelled = true;
      this.cpu.fireExit();
    }
  }


	/* --- Z80Memory --- */

  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    return (int) this.memory[ addr & 0xFFFF ] & 0xFF;
  }


  @Override
  public int getMemWord( int addr )
  {
    return EmuUtil.getWord( this.memory, addr & 0xFFFF );
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    this.memory[ addr & 0xFFFF ] = (byte) value;
    return true;
  }


  @Override
  public int  readMemByte( int addr, boolean m1 )
  {
    return getMemByte( addr, m1 );
  }


  public void writeMemByte( int addr, int value )
  {
    setMemByte( addr, value );
  }


	/* --- Z80PCListener --- */

  @Override
  public void z80PCChanged( Z80CPU cpu, int addr )
  {
    this.cpu.fireExit();
  }
}
