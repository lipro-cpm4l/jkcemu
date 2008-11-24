/*
 * (c) 2008 Jens Mueller
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
  private Z80CPU                     z80cpu;
  private Z80InterruptSource         preIntSource;
  private Collection<Z80CTCListener> listeners;
  private int                        interruptVector;
  private Timer[]                    timer;




  public Z80CTC( Z80CPU z80cpu, Z80InterruptSource preIntSource )
  {
    this.z80cpu          = z80cpu;
    this.preIntSource    = preIntSource;
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


  public int externalUpdate( int timerNum, int pulses )
  {
    return (timerNum >= 0) && (timerNum < this.timer.length) ?
			this.timer[ timerNum ].externalUpdate( pulses ) : 0;
  }


  public int read( int timerNum )
  {
    return (timerNum >= 0) && (timerNum < this.timer.length) ?
			this.timer[ timerNum ].read() : 0xFF;
  }


  public void reset( boolean powerOn )
  {
    if( powerOn ) {
      this.interruptVector = 0;
    }
    for( int i = 0; i < this.timer.length; i++ ) {
      this.timer[ i ].reset();
    }
  }


  public void systemUpdate( int pulses )
  {
    for( int i = 0; i < this.timer.length; i++ )
      this.timer[ i ].systemUpdate( pulses );
  }


  public void write( int timerNum, int value )
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
	this.interruptVector = value & 0xFF;
      } else {
	if( (timerNum >= 0) && (timerNum < this.timer.length) ) {
	  this.timer[ timerNum ].write( value );
	}
      }
    }
  }


	/* --- Methoden fuer Z80InterruptSource --- */

  public int z80GetInterruptVector()
  {
    int rv = 0;
    for( int i = 0; i < this.timer.length; i++ ) {
      if( this.timer[ i ].interruptPending ) {
	rv = this.interruptVector + (2 * i);
	break;
      }
    }
    return rv;
  }


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    systemUpdate( tStates );
  }


  /*
   * Normalerweise erkennt der Interrupt-ausloesende Baustein selbst,
   * wenn der Befehl RETI abgearbeitet wird und die
   * Interrupt-Routine beendet ist.
   * Hier im Emulator wird durch die CPU-Emulation diese Methode
   * aufgerufen, wenn RETI verarbeitet wird.
   */
  public void z80InterruptFinished()
  {
    for( int i = 0; i < this.timer.length; i++ )
      this.timer[ i ].interruptFinished();
  }


  /*
   * Diese Methode besagt, ob die CTC einen Interrupt ausgeloest hat,
   * deren Behandlungsroutine noch nicht beendet ist.
   * Uebertragen bedeutet das, ob die CTC die
   * Interrupt-Prioritaeten-Kette unterbrochen hat.
   */
  public boolean z80IsInterruptPending()
  {
    boolean rv = false;
    for( int i = 0; i < this.timer.length; i++ ) {
      if( this.timer[ i ].isInterruptPending() ) {
	rv = true;
	break;
      }
    }
    if( !rv && this.preIntSource != null ) {
      rv = this.preIntSource.z80IsInterruptPending();
    }
    return rv;
  }


	/* --- private Klasse --- */

  private class Timer
  {
    private int              timerNum;
    private volatile int     counterInit;
    private volatile int     counter;
    private volatile int     preCounter;
    private volatile boolean pre256;
    private volatile boolean extMode;
    private volatile boolean waitForTrigger;
    private volatile boolean interruptEnabled;
    private volatile boolean interruptPending;
    private volatile boolean nextIsCounterValue;
    private volatile boolean running;


    public Timer( int timerNum )
    {
      this.timerNum = timerNum;
      reset();
    }


    public void interruptFinished()
    {
      this.interruptPending = false;
    }


    public boolean isInterruptPending()
    {
      return this.interruptPending;
    }


    public boolean expectsCounterValue()
    {
      return this.nextIsCounterValue;
    }


    public int externalUpdate( int pulses )
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


    public int read()
    {
      return this.counter & 0xFF;
    }


    public void reset()
    {
      this.counterInit        = 0x100;
      this.counter            = this.counterInit;
      this.preCounter         = 0;
      this.pre256             = false;
      this.extMode            = false;
      this.waitForTrigger     = false;
      this.interruptEnabled   = false;
      this.interruptPending   = false;
      this.nextIsCounterValue = false;
      this.running            = false;
    }


    public void systemUpdate( int pulses )
    {
      if( (pulses > 0) && !this.extMode )
	updCounter( updPreCounter( pulses ) );
    }


    public void write( int value )
    {
      value &= 0xFF;
      if( this.nextIsCounterValue ) {
	this.counterInit        = (value > 0 ? value : 0x100);
	this.nextIsCounterValue = false;
	if( !this.waitForTrigger && !this.running ) {
	  this.preCounter = 0;
	  this.counter    = this.counterInit;
	  this.running    = true;
	}
      } else {
	this.interruptEnabled   = ((value & 0x80) != 0);
	this.extMode            = ((value & 0x40) != 0);
	this.pre256             = ((value & 0x20) != 0);
	this.waitForTrigger     = ((value & 0x08) != 0);
	this.nextIsCounterValue = ((value & 0x04) != 0);
	this.running            = ((value & 0x02) == 0);
      }
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
	  if( this.interruptEnabled ) {
	    if( !z80IsInterruptPending() ) {
	      this.interruptPending = true;
	      fireInterrupt();
	    }
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

  private void fireInterrupt()
  {
    this.z80cpu.fireInterrupt( this );
  }


  private void informListeners( int timerNum )
  {
    Collection<Z80CTCListener> listeners = this.listeners;
    if( listeners != null ) {
      synchronized( listeners ) {
	for( Z80CTCListener listener : listeners )
	  listener.z80CTCUpdate( this, timerNum );
      }
    }
  }
}

