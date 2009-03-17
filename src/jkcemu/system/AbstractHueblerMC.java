/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse zur Emulation eines Huebler-Computers
 */

package jkcemu.system;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Properties;
import jkcemu.base.*;
import z80emu.*;


public abstract class AbstractHueblerMC extends EmuSys
{
  protected int    keyChar;
  protected Z80CTC ctc;
  protected Z80PIO pio;


  public AbstractHueblerMC( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
  }


  protected void createIOSystem()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu );
    this.pio   = new Z80PIO( cpu );
    cpu.setInterruptSources( this.ctc );
    cpu.addTStatesListener( this.ctc );
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    Z80CPU cpu = emuThread.getZ80CPU();
    if( this.ctc != null ) {
      cpu.removeTStatesListener( this.ctc );
    }
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  public void keyReleased()
  {
    this.keyChar = 0;
  }


  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      this.keyChar = ch;
      rv           = true;
    }
    return rv;
  }


  public int readIOByte( int port )
  {
    int rv = 0;
    switch( port & 0xFF ) {
      case 0x08:
      case 0x0A:
	rv = this.keyChar;
	break;

      case 0x09:
      case 0x0B:
	if( this.keyChar != 0 ) {
	  rv = 0xFF;
	}
	break;

      case 0x0C:
	rv = this.pio.readPortA();
	break;

      case 0x0D:
	rv = this.pio.readControlA();
	break;

      case 0x0E:
	rv = this.pio.readPortB();
	break;

      case 0x0F:
	rv = this.pio.readControlB();
	break;

      case 0x14:
      case 0x15:
      case 0x16:
      case 0x17:
	rv = this.ctc.read( port & 0x03 );
	break;
    }
    return rv;
  }


  public void writeIOByte( int port, int value )
  {
    switch( port & 0xFF ) {
      case 0x0C:
	this.pio.writePortA( value );
	break;

      case 0x0D:
	this.pio.writeControlA( value );
	break;

      case 0x0E:
	this.pio.writePortB( value );
	break;

      case 0x0F:
	this.pio.writeControlB( value );
	break;

      case 0x14:
      case 0x15:
      case 0x16:
      case 0x17:
	this.ctc.write( port & 0x03, value );
	break;
    }
  }
}

