/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse der KC85-Joystick-Module
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import jkcemu.base.EmuThread;
import jkcemu.joystick.JoystickThread;
import z80emu.*;


public abstract class KC85JoystickModule
			extends AbstractKC85Module
			implements Z80InterruptSource
{
  protected Z80PIO  pio;
  private   boolean lastBI;
  private   String  title;


  protected KC85JoystickModule( int slot )
  {
    super( slot );
    setStatus( 0x01 );		// immer aktiv
    this.lastBI = false;
    this.pio    = new Z80PIO( "PIO (Joystick)" );
    this.pio.putInValuePortA( 0xFF, false );
    this.title  = String.format(
			"%s im Schacht %02X",
			getModuleName(),
			slot );
  }


  public void setBIState( boolean bi )
  {
    if( bi != this.lastBI ) {
      this.lastBI = bi;
      if( bi ) {
	/*
	 * Das BI-Signal ist auf ASTB und BSTB gelegt,
	 * um zyklische Interrupts ausloesen zu koennen.
	 */
	this.pio.strobePortA();
	this.pio.strobePortB();
      }
    }
  }


  public void setJoystickAction( int joyNum, int actionMask )
  {
    if( joyNum == 0 ) {
      int value = 0xFF;
      if( (actionMask & JoystickThread.UP_MASK) != 0 ) {
	value ^= 0x01;
      }
      if( (actionMask & JoystickThread.DOWN_MASK) != 0 ) {
	value ^= 0x02;
      }
      if( (actionMask & JoystickThread.LEFT_MASK) != 0 ) {
	value ^= 0x04;
      }
      if( (actionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	value ^= 0x08;
      }
      if( (actionMask & JoystickThread.BUTTON2_MASK) != 0 ) {
	value ^= 0x10;
      }
      if( (actionMask & JoystickThread.BUTTON1_MASK) != 0 ) {
	value ^= 0x20;
      }
      this.pio.putInValuePortA( value, false );
    }
  }


	/* --- Z80InterruptSource --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf )
  {
    buf.append( "<h2>PIO (IO-Adressen 90-93)</h2>\n" );
    this.pio.appendStatusHTMLTo( buf );
  }


  @Override
  public synchronized int interruptAccept()
  {
    return this.pio.interruptAccept();
  }


  @Override
  public synchronized void interruptFinish()
  {
    if( this.pio.isInterruptAccepted() ) {
      this.pio.interruptFinish();
    }
  }


  @Override
  public boolean isInterruptAccepted()
  {
    return this.pio.isInterruptAccepted();
  }


  @Override
  public boolean isInterruptRequested()
  {
    return this.pio.isInterruptRequested();
  }


  @Override
  public void reset()
  {
    this.pio.reset();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int getTypeByte()
  {
    return 0xFF;		// kein Strukturbyte
  }


  @Override
  public int readIOByte( int port )
  {
    int rv = -1;
    port &= 0xFF;
    if( (port >= 0x90) && (port < 0x98) ) {
      switch( port & 0xFF ) {
	case 0x90:
	  rv = this.pio.readPortA();
	  break;

	case 0x91:
	  rv = this.pio.readPortB();
	  break;

	case 0x92:
	  rv = this.pio.readControlA();
	  break;

	case 0x93:
	  rv = this.pio.readControlB();
	  break;

	default:
	  rv = 0xFF;
      }
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel )
  {
    reset();
  }


  @Override
  public void setStatus( int value )
  {
    super.setStatus( 0x01 );		// immer aktiv
  }


  @Override
  public String toString()
  {
    return this.title;
  }


  @Override
  public boolean writeIOByte( int port, int value )
  {
    boolean rv = false;
    port &= 0xFF;
    if( (port >= 0x90) && (port < 0x98) ) {
      switch( port & 0xFF ) {
	case 0x90:
	  this.pio.writePortA( value );
	  break;

	case 0x91:
	  this.pio.writePortB( value );
	  break;

	case 0x92:
	  this.pio.writeControlA( value );
	  break;

	case 0x93:
	  this.pio.writeControlB( value );
	  break;
      }
      rv = true;
    }
    return rv;
  }
}

