/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Joystick-/Centronics-Moduls M021
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import jkcemu.base.EmuThread;
import jkcemu.print.PrintMngr;
import z80emu.*;


public class M021 extends KC85JoystickModule implements Z80TStatesListener
{
  private EmuThread    emuThread;
  private boolean      cenStrobe;
  private volatile int cenBusyTStateCounter;


  public M021( int slot, EmuThread emuThread )
  {
    super( slot );
    this.emuThread            = emuThread;
    this.cenStrobe            = false;
    this.cenBusyTStateCounter = 0;
    this.pio.putInValuePortA( 0, 0x40 );	// BUSY=0
  }


		/* --- Z80TStatesListener --- */

  @Override
  public synchronized void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( this.cenBusyTStateCounter > 0 ) {
      this.cenBusyTStateCounter -= tStates;
      if( this.cenBusyTStateCounter <= 0 ) {
	this.pio.putInValuePortA( 0, 0x40 );		// BUSY=0
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getModuleName()
  {
    return "M021";
  }


  @Override
  public boolean supportsPrinter()
  {
    return true;
  }


  @Override
  public boolean writeIOByte( int port, int value )
  {
    boolean rv = false;
    port &= 0xFF;
    if( (port >= 0x90) && (port < 0x98) ) {
      switch( port & 0xFF ) {
	case 0x90:
	  {
	    this.pio.writePortA( value );
	    PrintMngr pm = this.emuThread.getPrintMngr();
	    if( pm != null ) {
	      boolean strobe = ((this.pio.fetchOutValuePortA( false )
							& 0x80) == 0);
	      if( strobe != this.cenStrobe ) {
		this.cenStrobe = strobe;
		if( strobe ) {
		  pm.putByte( this.pio.fetchOutValuePortB( false ) );
		  this.pio.putInValuePortA( 0x40, 0x40 );	// BUSY=1
		  synchronized( this ) {
		    this.cenBusyTStateCounter =
			this.emuThread.getZ80CPU().getMaxSpeedKHz() / 20;
		    if( this.cenBusyTStateCounter < 1 ) {
		      this.cenBusyTStateCounter = 1;
		    }
		  }
		}
	      }
	    }
	  }
	  break;

	case 0x91:
	  this.pio.writePortB( value );
	  break;

	case 0x92:
	  this.pio.writeControlA( value );
	  // sicherstellen, dass der BUSY-Status nicht verlorengegangen ist
	  this.pio.putInValuePortA(
		this.cenBusyTStateCounter > 0 ? 0x40 : 0, 0x40 );
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

