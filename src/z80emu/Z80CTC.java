/*
 * (c) 2008-2009 Jens Mueller
 *
 * Z80-Emulator
 *
 * Emulation der Z80 CTC
 */

package z80emu;

import java.lang.*;
import java.util.ArrayList;
import java.util.Collection;


public class Z80CTC implements Z80InterruptSource, Z80TStatesListener
{
  private Z80CPU                     cpu;
  private Collection<Z80CTCListener> listeners;
  private int                        interruptVector;
  private Timer[]                    timer;


  public Z80CTC( Z80CPU cpu )
  {
    this.cpu             = cpu;
    this.listeners       = null;
    this.interruptVector = 0;
    this.timer           = new Timer[ 4 ];
    for( int i = 0; i < this.timer.length; i++ ) {
      this.timer[ i ] = new Timer( i );
    }
  }


  public synchronized void addCTCListener( Z80CTCListener listener )
  {
    if( this.listeners == null ) {
      this.listeners = new ArrayList<Z80CTCListener>();
    }
    this.listeners.add( listener );
  }


  public synchronized void removeCTCListener( Z80CTCListener listener )
  {
    if( this.listeners != null )
      this.listeners.remove( listener );
  }


  public synchronized int externalUpdate( int timerNum, boolean state )
  {
    return (timerNum >= 0) && (timerNum < this.timer.length) ?
			this.timer[ timerNum ].externalUpdate( state ) : 0;
  }


  public synchronized int externalUpdate( int timerNum, int pulses )
  {
    return (timerNum >= 0) && (timerNum < this.timer.length) ?
			this.timer[ timerNum ].externalUpdate( pulses ) : 0;
  }


  public synchronized int read( int timerNum )
  {
    return (timerNum >= 0) && (timerNum < this.timer.length) ?
			this.timer[ timerNum ].read() : 0xFF;
  }


  public synchronized void reset( boolean powerOn )
  {
    if( powerOn ) {
      this.interruptVector = 0;
    }
    reset();
  }


  public synchronized void systemUpdate( int pulses )
  {
    for( int i = 0; i < this.timer.length; i++ )
      this.timer[ i ].systemUpdate( pulses );
  }


  public synchronized void write( int timerNum, int value )
  {
    boolean done = false;
    if( (timerNum >= 0) && (timerNum < this.timer.length) ) {
      if( this.timer[ timerNum ].expectsCounterValue() ) {
	this.timer[ timerNum ].write( value );
	done = true;
      }
    }
    if( !done ) {
      if( (value & 0x01) == 0 ) {
	this.interruptVector = value & 0xF8;
      } else {
	if( (timerNum >= 0) && (timerNum < this.timer.length) ) {
	  this.timer[ timerNum ].write( value );
	}
      }
    }
  }


	/* --- Methoden fuer Z80InterruptSource --- */

  public synchronized int interruptAccepted()
  {
    int rv = 0;
    for( int i = 0; i < this.timer.length; i++ ) {
      if( this.timer[ i ].interruptRequested ) {
	this.timer[ i ].interruptRequested = false;
	this.timer[ i ].interruptPending   = true;
	rv = this.interruptVector + (i * 2);
	break;
      }
    }
    return rv;
  }


  public synchronized void interruptFinished()
  {
    for( int i = 0; i < this.timer.length; i++ ) {
      if( this.timer[ i ].interruptPending ) {
	this.timer[ i ].interruptPending = false;
	break;
      }
    }
  }


  public boolean isInterruptPending()
  {
    return this.timer[ 0 ].interruptPending
		|| this.timer[ 1 ].interruptPending
		|| this.timer[ 2 ].interruptPending
		|| this.timer[ 3 ].interruptPending;
  }


  public boolean isInterruptRequested()
  {
    boolean rv = false;
    for( int i = 0; i < this.timer.length; i++ ) {
      if( this.timer[ i ].interruptEnabled
		&& this.timer[ i ].interruptRequested )
      {
	rv = true;
	break;
      }
    }
    return rv;
  }


  public void reset()
  {
    for( int i = 0; i < this.timer.length; i++ )
      this.timer[ i ].reset();
  }


	/* --- Z80TStatesListener --- */

  public synchronized void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    systemUpdate( tStates );
  }


	/* --- private Klasse --- */

  private class Timer
  {
    private int     timerNum;
    private int     ignoreInternalPulses;
    private int     counterInit;
    private int     counter;
    private int     preCounter;
    private boolean pre256;
    private boolean extMode;
    private boolean slope;
    private boolean waitForTrigger;
    private boolean interruptEnabled;
    private boolean interruptPending;
    private boolean interruptRequested;
    private boolean nextIsCounterValue;
    private boolean running;
    private Boolean lastInSlope;


    public Timer( int timerNum )
    {
      this.timerNum = timerNum;
      reset();
    }


    public boolean expectsCounterValue()
    {
      return this.nextIsCounterValue;
    }


    public int externalUpdate( boolean slope )
    {
      int rv = 0;
      if( slope == this.slope ) {
        Boolean lastInSlope = this.lastInSlope;
        if( lastInSlope != null ) {
	  if( slope != lastInSlope.booleanValue() )
	    rv = externalUpdate2( 1 );
	}
      }
      this.lastInSlope = slope ? Boolean.TRUE : Boolean.FALSE;
      return rv;
    }


    public int externalUpdate( int pulses )
    {
      this.lastInSlope = null;
      return externalUpdate2( pulses );
    }


    public int read()
    {
      return this.counter & 0xFF;
    }


    public void reset()
    {
      this.ignoreInternalPulses = 0;
      this.counterInit          = 0x100;
      this.counter              = this.counterInit;
      this.preCounter           = 0;
      this.pre256               = false;
      this.extMode              = false;
      this.slope                = false;
      this.waitForTrigger       = false;
      this.interruptEnabled     = false;
      this.interruptPending     = false;
      this.interruptRequested   = false;
      this.nextIsCounterValue   = false;
      this.running              = false;
      this.lastInSlope          = null;
    }


    public void systemUpdate( int pulses )
    {
      if( pulses > 0 ) {
	if( pulses < this.ignoreInternalPulses ) {
	  this.ignoreInternalPulses -= pulses;
	} else {
	  pulses -= this.ignoreInternalPulses;
	  this.ignoreInternalPulses = 0;
	  if( (pulses > 0) && !this.extMode ) {
	    updCounter( updPreCounter( pulses ) );
	  }
	}
      }
    }


    public void write( int value )
    {
      value &= 0xFF;
      if( this.nextIsCounterValue ) {
	this.counterInit        = (value > 0 ? value : 0x100);
	this.nextIsCounterValue = false;
	if( !this.running && (this.extMode || !this.waitForTrigger) ) {
	  /*
	   * Die Taktzyklen des Ausgabebefehls,
	   * mit dem der CTC-Kanal programmiert wird,
	   * duerfen nicht alle zum Herunterzaehlen des Kanals
	   * verwendet werden.
	   * Aus diesem Grund werden einige der naechsten
	   * internen Taktzyklen ignoriert.
	   */
	  this.ignoreInternalPulses = 8;
	  this.preCounter           = 0;
	  this.counter              = this.counterInit;
	  this.running              = true;
	}
      } else {
	this.interruptEnabled   = ((value & 0x80) != 0);
	this.extMode            = ((value & 0x40) != 0);
	this.pre256             = ((value & 0x20) != 0);
	this.slope              = ((value & 0x10) != 0);
	this.waitForTrigger     = ((value & 0x08) != 0);
	this.nextIsCounterValue = ((value & 0x04) != 0);
	if( (value & 0x02) != 0 ) {
	  this.running = false;
	}
	this.lastInSlope = null;
      }
    }


    private int externalUpdate2( int pulses )
    {
      int rv = 0;
      if( pulses > 0 ) {
	if( this.extMode ) {
	  rv = updCounter( pulses );
	} else {
	  if( this.waitForTrigger && !this.running ) {
	    this.preCounter     = 0;
	    this.counter        = this.counterInit;
	    this.waitForTrigger = false;
	    this.running        = true;
	  }
	}
      }
      return rv;
    }


    private int updCounter( int pulses )
    {
      int rv = 0;
      while( this.running && (pulses > 0) ) {
	if( pulses < this.counter ) {
	  this.counter -= pulses;
	  pulses = 0;
	} else {
	  pulses -= this.counter;
	  this.counter = this.counterInit;
	  rv++;
	  informListeners( this.timerNum );
	  if( this.interruptEnabled && !this.interruptPending ) {
	    this.interruptRequested = true;
	  }
	}
      }
      return rv;
    }


    private int updPreCounter( int pulses )
    {
      int rv = 0;
      while( this.running && (pulses > 0) ) {
	if( this.preCounter == 0 ) {
	  this.preCounter = this.pre256 ? 256 : 16;
	}
	if( pulses < this.preCounter ) {
	  this.preCounter -= pulses;
	  pulses = 0;
	} else {
	  pulses -= this.preCounter;
	  this.preCounter = 0;
	  rv++;
	}
      }
      return rv;
    }
  }


	/* --- private Methoden --- */

  private void informListeners( int timerNum )
  {
    Collection<Z80CTCListener> listeners = this.listeners;
    if( listeners != null ) {
      for( Z80CTCListener listener : listeners )
	listener.z80CTCUpdate( this, timerNum );
    }
  }
}

