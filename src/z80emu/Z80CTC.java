/*
 * (c) 2008-2013 Jens Mueller
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
  private String                     title;
  private Collection<Z80CTCListener> listeners;
  private int                        interruptVector;
  private Timer[]                    timer;


  public Z80CTC( String title )
  {
    this.title           = title;
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


  /*
   * Diese Methode stellt eine Verbindung
   * vom Ausgang fromTimerNum zum Eingang toTimerNum her.
   */
  public void setTimerConnection( int fromTimerNum, int toTimerNum )
  {
    if( (fromTimerNum >= 0) && (fromTimerNum < this.timer.length) ) {
      this.timer[ fromTimerNum ].toTimer    = this.timer[ toTimerNum ];
      this.timer[ toTimerNum ].fromTimerNum = fromTimerNum;
    }
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


	/* --- Z80InterruptSource --- */

  @Override
  public void appendInterruptStatusHTMLTo( StringBuilder buf )
  {
    boolean showISR = false;
    buf.append( "<table border=\"1\">\n"
	+ "<tr><th></th><th>Kanal&nbsp;0</th><th>Kanal&nbsp;1</th>"
	+ "<th>Kanal&nbsp;2</th><th>Kanal&nbsp;3</th></tr>\n"
	+ "<tr><td>Status:</td>" );
    for( int i = 0; i < this.timer.length; i++ ) {
      buf.append( "<td>" );
      if( this.timer[ i ].running ) {
	buf.append( "l&auml;uft" );
      } else if( this.timer[ i ].waitForTrigger ) {
	buf.append( "wartet auf Trigger-Impuls" );
      } else {
	buf.append( "angehalten" );
      }
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Vorteiler:</td>" );
    for( int i = 0; i < this.timer.length; i++ ) {
      buf.append( "<td>" );
      if( this.timer[ i ].extMode ) {
	buf.append( "nicht aktiv" );
      } else {
	buf.append( this.timer[ i ].preCounter );
	if( this.timer[ i ].pre256 ) {
	  buf.append( "/256" );
	} else {
	  buf.append( "/16" );
	}
      }
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Z&auml;hler:</td>" );
    for( int i = 0; i < this.timer.length; i++ ) {
      buf.append( "<td>" );
      buf.append( this.timer[ i ].counter & 0xFF );
      buf.append( (char) '/' );
      buf.append( this.timer[ i ].counterInit );
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Takt:</td>" );
    for( int i = 0; i < this.timer.length; i++ ) {
      buf.append( "<td>" );
      if( this.timer[ i ].extMode ) {
	if( this.timer[ i ].fromTimerNum >= 0 ) {
	  buf.append( "Kanal " );
	  buf.append( this.timer[ i ].fromTimerNum );
	} else {
	  buf.append( "extern" );
	}
      } else {
	buf.append( "Systemtakt" );
      }
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Interrupt-Status:</td>" );
    for( int i = 0; i < this.timer.length; i++ ) {
      buf.append( "<td>" );
      if( this.timer[ i ].interruptAccepted ) {
	buf.append( "angenommen (wird gerade bedient)" );
	showISR = true;
      } else if( this.timer[ i ].interruptRequested ) {
	buf.append( "angemeldet" );
	showISR = true;
      } else if( this.timer[ i ].interruptEnabled ) {
	buf.append( "freigegeben" );
	showISR = true;
      } else {
	buf.append( "gesperrt" );
      }
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Interrupt-Vektor:</td><td colspan=\"4\">" );
    buf.append( String.format( "%02Xh", this.interruptVector ) );
    buf.append( "</td></tr>\n"
	+ "</table>\n" );
  }


  @Override
  public synchronized int interruptAccept()
  {
    int rv = 0;
    for( int i = 0; i < this.timer.length; i++ ) {
      if( this.timer[ i ].interruptRequested ) {
	this.timer[ i ].interruptAccepted  = true;
	this.timer[ i ].interruptRequested = false;
	rv = this.interruptVector + (i * 2);
	break;
      }
    }
    return rv;
  }


  @Override
  public synchronized void interruptFinish()
  {
    for( int i = 0; i < this.timer.length; i++ ) {
      if( this.timer[ i ].interruptAccepted ) {
	this.timer[ i ].interruptAccepted = false;
	break;
      }
    }
  }


  @Override
  public boolean isInterruptAccepted()
  {
    return this.timer[ 0 ].interruptAccepted
		|| this.timer[ 1 ].interruptAccepted
		|| this.timer[ 2 ].interruptAccepted
		|| this.timer[ 3 ].interruptAccepted;
  }


  @Override
  public boolean isInterruptRequested()
  {
    boolean rv = false;
    for( int i = 0; i < this.timer.length; i++ ) {
      if( this.timer[ i ].interruptAccepted ) {
	break;
      }
      if( this.timer[ i ].interruptEnabled
		&& this.timer[ i ].interruptRequested )
      {
	rv = true;
	break;
      }
    }
    return rv;
  }


  @Override
  public synchronized void reset( boolean powerOn )
  {
    if( powerOn ) {
      this.interruptVector = 0;
    }
    for( int i = 0; i < this.timer.length; i++ ) {
      this.timer[ i ].reset();
    }
  }


	/* --- Z80TStatesListener --- */

  @Override
  public synchronized void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( tStates > 0 ) {
      for( int i = 0; i < this.timer.length; i++ ) {
	this.timer[ i ].systemUpdate( tStates );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.title;
  }


	/* --- private Klasse --- */

  private class Timer
  {
    private int     timerNum;
    private int     fromTimerNum;
    private Timer   toTimer;
    private int     ignoreInternalPulses;
    private int     counterInit;
    private int     counter;
    private int     preCounter;
    private boolean pre256;
    private boolean extMode;
    private boolean slope;
    private boolean waitForTrigger;
    private boolean ignoreNextInternalPulses;
    private boolean interruptEnabled;
    private boolean interruptAccepted;
    private boolean interruptRequested;
    private boolean nextIsCounterValue;
    private boolean running;
    private Boolean lastInSlope;


    private Timer( int timerNum )
    {
      this.timerNum     = timerNum;
      this.fromTimerNum = -1;
      this.toTimer      = null;
      reset();
    }


    private boolean expectsCounterValue()
    {
      return this.nextIsCounterValue;
    }


    private int externalUpdate( boolean slope )
    {
      int rv = 0;
      if( slope == this.slope ) {
        Boolean lastInSlope = this.lastInSlope;
        if( lastInSlope != null ) {
	  if( slope != lastInSlope.booleanValue() ) {
	    rv = externalUpdateIntern( 1 );
	  }
	}
      }
      this.lastInSlope = slope ? Boolean.TRUE : Boolean.FALSE;
      return rv;
    }


    private int externalUpdate( int pulses )
    {
      this.lastInSlope = null;
      return externalUpdateIntern( pulses );
    }


    private int read()
    {
      return this.counter & 0xFF;
    }


    private void reset()
    {
      this.ignoreInternalPulses     = 0;
      this.counterInit              = 0x100;
      this.counter                  = this.counterInit;
      this.preCounter               = 0;
      this.pre256                   = false;
      this.extMode                  = false;
      this.slope                    = false;
      this.waitForTrigger           = false;
      this.ignoreNextInternalPulses = false;
      this.interruptEnabled         = false;
      this.interruptAccepted        = false;
      this.interruptRequested       = false;
      this.nextIsCounterValue       = false;
      this.running                  = false;
      this.lastInSlope              = null;
    }


    private void systemUpdate( int pulses )
    {
      if( this.ignoreNextInternalPulses ) {
	this.ignoreInternalPulses += pulses;
	this.ignoreNextInternalPulses = false;
      }
      if( pulses > this.ignoreInternalPulses ) {
	pulses -= this.ignoreInternalPulses;
	this.ignoreInternalPulses = 0;
	if( !this.extMode ) {
	  updCounter( updPreCounter( pulses ) );
	}
      } else {
	this.ignoreInternalPulses -= pulses;
      }
    }


    private void write( int value )
    {
      value &= 0xFF;
      if( this.nextIsCounterValue ) {
	this.counterInit        = (value > 0 ? value : 0x100);
	this.nextIsCounterValue = false;
	if( !this.running && (this.extMode || !this.waitForTrigger) ) {
	  /*
	   * Die Taktzyklen des Ausgabebefehls,
	   * mit dem der CTC-Kanal programmiert wird,
	   * duerfen nicht zum Herunterzaehlen des Kanals
	   * verwendet werden.
	   * Aus diesem Grund werden die naechsten Taktzyklen,
	   * d.h., die des Ausgabebefahls, ignoriert,
	   * Da die CTC erst einen Taktzyklus nach der Programmierung
	   * mit dem Zaehlen beginnt,
	   * wird zusaetzlich noch ein Taktzyklus ignoriert.
	   */
	  this.ignoreNextInternalPulses = true;
	  this.ignoreInternalPulses     = 1;
	  this.preCounter               = 0;
	  this.counter                  = this.counterInit;
	  this.running                  = true;
	}
      } else {
	this.interruptAccepted  = false;
	this.interruptRequested = false;
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


    private int externalUpdateIntern( int pulses )
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
	  if( this.interruptEnabled ) {
	    this.interruptRequested = true;
	  }
	  if( this.toTimer != null ) {
	    this.toTimer.externalUpdate( 1 );
	  }
	  informListeners( this.timerNum );
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
      for( Z80CTCListener listener : listeners ) {
	listener.z80CTCUpdate( this, timerNum );
      }
    }
  }
}

