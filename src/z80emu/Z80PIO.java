/*
 * (c) 2008-2017 Jens Mueller
 *
 * Z80-Emulator
 *
 * Emulation der Z80 PIO
 *
 * In der Betriebsart BYTE_INOUT wird das Ready-Signal von Port B,
 * welches in dem Fall fuer die Eingabe in Port A zustaendig ist,
 * nicht emuliert.
 */

package z80emu;

import java.lang.*;
import java.util.*;


public class Z80PIO implements Z80InterruptSource
{
  public enum PortInfo { A, B };

  public enum Mode {
		BYTE_OUT,
		BYTE_IN,
		BYTE_INOUT,
		BIT_INOUT };

  public enum Status {
		INTERRUPT_ENABLED,
		INTERRUPT_DISABLED,
		MODE_CHANGED,
		READY_FOR_INPUT,
		OUTPUT_AVAILABLE,
		OUTPUT_CHANGED };

  private enum Ctrl { NONE, IO_MASK, INTERRUPT_MASK };

  private String      title;
  private Z80PIO.Port portA;
  private Z80PIO.Port portB;


  public Z80PIO( String title )
  {
    this.title = title;
    this.portA = new Z80PIO.Port( PortInfo.A );
    this.portB = new Z80PIO.Port( PortInfo.B );
  }


  /*
   * Methoden, die in einem anderem Thread
   * (z.B. IO-Emulations-Thread)
   * aufgerufen werden koennen
   */
  public synchronized void addPIOPortListener(
				Z80PIOPortListener listener,
				PortInfo           portInfo )
  {
    switch( portInfo ) {
      case A:
	addPIOPortListener( listener, this.portA );
	break;

      case B:
	addPIOPortListener( listener, this.portB );
	break;
    }
  }


  public synchronized void removePIOPortListener(
				Z80PIOPortListener listener,
				PortInfo           portInfo )
  {
    switch( portInfo ) {
      case A:
	removePIOPortListener( listener, this.portA );
	break;

      case B:
	removePIOPortListener( listener, this.portB );
	break;
    }
  }


  /*
   * Diese Methoden bieten Zugriff auf den Interruptvektor
   */
  public synchronized int getInterruptVectorPortA()
  {
    return this.portA.interruptVector;
  }

  public synchronized int getInterruptVectorPortB()
  {
    return this.portB.interruptVector;
  }


  /*
   * Die beiden Methoden lesen die Werte, die die PIO zur Ausgabe
   * an den Ports bereithaelt (Seite IO-System).
   * Kann der Wert nicht gelesen werden, z.B. wenn bei Handshake
   * kein neuer Wert in das Ausgaberegister geschrieben wurde,
   * wird -1 zurueckgegeben.
   * Je nach Betriebsart wird ein Interrupt ausgeloest.
   */
  public synchronized int fetchOutValuePortA( boolean strobe )
  {
    return fetchOutValue( this.portA, strobe );
  }

  public synchronized int fetchOutValuePortB( boolean strobe )
  {
    return fetchOutValue( this.portB, strobe );
  }


  public synchronized Mode getModePortA()
  {
    return this.portA.mode;
  }

  public synchronized Mode getModePortB()
  {
    return this.portB.mode;
  }


  public synchronized boolean isReadyPortA()
  {
    return this.portA.ready;
  }


  public synchronized boolean isReadyPortB()
  {
    return this.portB.ready;
  }


  /*
   * Die beiden Methoden setzen die Werte an den Ports,
   * die die PIO uebernehmen soll (Seite IO-System).
   * Je nach Betriebsart wird ein Interrupt ausgeloest.
   *
   * Die anderen beiden Methoden dienen dazu,
   * nur die Werte einzelner Bits zu setzen.
   * Die zu setzenden Bits werden mit dem Parameter "mask" maskiert.
   * Ein Handshake wird nicht simuliert.
   *
   * Rueckgabewert:
   *	true:	Daten durch PIO uebernommen
   *	false:	Daten nicht uebernommen
   *		(CPU hat den vorherigen Wert noch nicht gelesen.)
   */
  public synchronized boolean putInValuePortA( int value, boolean strobe )
  {
    return putInValue( this.portA, value, 0xFF, strobe );
  }

  public synchronized boolean putInValuePortA( int value, int mask )
  {
    return putInValue( this.portA, value, mask, false );
  }

  public synchronized boolean putInValuePortB( int value, boolean strobe )
  {
    return putInValue( this.portB, value, 0xFF, strobe );
  }

  public synchronized boolean putInValuePortB( int value, int mask )
  {
    return putInValue( this.portB, value, mask, false );
  }


  /*
   * Diese beiden Methoden markieren einen Strobe-Impulse.
   */
  public void strobePortA()
  {
    strobePort( this.portA );
  }

  public void strobePortB()
  {
    strobePort( this.portB );
  }


  /*
   * Methoden, die im CPU-Emulations-Thread
   * aufgerufen werden koennen (CPU-Seite).
   */
  public synchronized int readControlA()
  {
    return readControl( this.portA );
  }


  public synchronized int readControlB()
  {
    return readControl( this.portB );
  }


  public synchronized int readDataA()
  {
    return readData( this.portA );
  }


  public synchronized int readDataB()
  {
    return readData( this.portB );
  }


  public synchronized void writeControlA( int value )
  {
    writeControl( this.portA, value );
  }


  public synchronized void writeControlB( int value )
  {
    writeControl( this.portB, value );
  }


  public synchronized void writeDataA( int value )
  {
    writeData( this.portA, value );
  }


  public synchronized void writeDataB( int value )
  {
    writeData( this.portB, value );
  }


	/* --- Methoden fuer Z80InterruptSource --- */

  @Override
  public void appendInterruptStatusHTMLTo( StringBuilder buf )
  {
    Port[] ports = { this.portA, this.portB };
    buf.append( "<table border=\"1\">\n"
	+ "<tr><th></th><th>Tor&nbsp;A</th><th>Tor&nbsp;B</th></tr>\n"
	+ "<tr><td>Betriebsart:</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      buf.append( "<td>" );
      switch( ports[ i ].mode ) {
	case BYTE_OUT:
	  buf.append( "Byte-Ausgabe" );
	  break;
	case BYTE_IN:
	  buf.append( "Byte-Eingabe" );
	  break;
	case BYTE_INOUT:
	  buf.append( "Byte-Ein-/Ausgabe" );
	  break;
	case BIT_INOUT:
	  buf.append( "Bit-Ein-/Ausgabe" );
	  break;
      }
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>E/A-Maske:</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      buf.append( "<td>" );
      if( ports[ i ].mode == Mode.BIT_INOUT ) {
	int v = ports[ i ].ioMask;
	for( int k = 0; k < 8; k++ ) {
	  buf.append( (char) ((v & 0x80) != 0 ? 'E' : 'A') );
	  v <<= 1;
	}
      } else {
	buf.append( "nicht relevant" );
      }
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Eingabe-Register:</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      buf.append( String.format( "<td>%02Xh</td>", ports[ i ].inValue ) );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Ausgabe-Register:</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      buf.append( String.format( "<td>%02Xh</td>", ports[ i ].outValue ) );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Interrupt-Status:</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      buf.append( "<td>" );
      if( ports[ i ].interruptAccepted ) {
	buf.append( "angenommen (wird gerade bedient)" );
      } else if( ports[ i ].interruptRequested ) {
	buf.append( "angemeldet" );
      } else if( ports[ i ].interruptEnabled ) {
	buf.append( "freigegeben" );
      } else {
	buf.append( "gesperrt" );
      }
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Interrupt-Maske (L-aktiv):</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      buf.append( "<td>" );
      if( ports[ i ].mode == Mode.BIT_INOUT ) {
	int v = ports[ i ].interruptMask;
	buf.append( String.format( "%02Xh (Bits: ", v ) );
	char ch = '7';
	for( int k = 0; k < 8; k++ ) {
	  buf.append( (char) ((v & 0x80) == 0 ? ch : '-') );
	  v <<= 1;
	  --ch;
	}
	buf.append( (char) ')' );
      } else {
	buf.append( "nicht relevant" );
      }
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Interrupt-Anforderung&nbsp;bei:</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      buf.append( "<td>" );
      if( ports[ i ].mode == Mode.BIT_INOUT ) {
	buf.append( (char) (ports[ i ].interruptFireAtH ? 'H' : 'L') );
	buf.append( "-Pegel, " );
	buf.append( ports[ i ].interruptBitsAnd ? "UND" : "ODER" );
	buf.append( "-verkn&uuml;pft" );
      } else {
	buf.append( "nicht relevant" );
      }
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Interrupt-Vektor:</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      int iv = ports[ i ].interruptVector;
      buf.append( String.format(
			"<td>%02Xh</td>",
			ports[ i ].interruptVector ) );
    }
    buf.append( "</tr>\n"
	+ "</table>\n" );
  }


  @Override
  public synchronized int interruptAccept()
  {
    int rv = 0;
    if( !this.portA.interruptAccepted && this.portA.interruptRequested ) {
      this.portA.interruptAccepted  = true;
      this.portA.interruptRequested = false;
      rv = this.portA.interruptVector;
    }
    else if( !this.portB.interruptAccepted && this.portB.interruptRequested )
    {
      this.portB.interruptAccepted  = true;
      this.portB.interruptRequested = false;
      rv = this.portB.interruptVector;
    }
    return rv;
  }


  @Override
  public synchronized void interruptFinish()
  {
    if( this.portA.interruptAccepted ) {
      this.portA.interruptAccepted     = false;
      this.portA.interruptCondRealized = false;
    }
    else if( this.portB.interruptAccepted ) {
      this.portB.interruptAccepted     = false;
      this.portB.interruptCondRealized = false;
    }
  }


  @Override
  public boolean isInterruptAccepted()
  {
    return this.portA.interruptAccepted || this.portB.interruptAccepted;
  }


  @Override
  public boolean isInterruptRequested()
  {
    boolean rv = (this.portA.interruptEnabled
				&& this.portA.interruptRequested);
    if( !rv && !this.portA.interruptAccepted ) {
      rv = (this.portB.interruptEnabled && this.portB.interruptRequested);
    }
    return rv;
  }


  @Override
  public synchronized void reset( boolean powerOn )
  {
    this.portA.reset( powerOn );
    this.portB.reset( powerOn );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.title;
  }


	/* --- private Methoden --- */

  private class Port
  {
    public PortInfo                       portInfo;
    public int                            inValue;
    public int                            outValue;
    public boolean                        ready;
    public Mode                           mode;
    public Ctrl                           nextCtrl;
    public int                            ioMask;
    public int                            interruptVector;
    public int                            interruptMask;
    public boolean                        interruptFireAtH;
    public boolean                        interruptBitsAnd;
    public boolean                        interruptEnabled;
    public volatile boolean               interruptAccepted;
    public volatile boolean               interruptRequested;
    public boolean                        interruptCondRealized;
    public Collection<Z80PIOPortListener> listeners;


    public Port( PortInfo portInfo )
    {
      this.portInfo  = portInfo;
      this.listeners = null;
      reset( true );
    }

    public void reset( boolean powerOn )
    {
      if( powerOn ) {
	this.interruptVector = 0;
      }
      this.inValue               = 0xFF;
      this.outValue              = 0xFF;
      this.mode                  = Mode.BYTE_IN;
      this.nextCtrl              = Ctrl.NONE;
      this.ioMask                = 0;
      this.interruptMask         = 0;
      this.interruptFireAtH      = false;
      this.interruptBitsAnd      = false;
      this.interruptEnabled      = false;
      this.interruptAccepted     = false;
      this.interruptRequested    = false;
      this.interruptCondRealized = false;
      this.ready                 = false;
    }
  }


  private synchronized void addPIOPortListener(
				Z80PIOPortListener listener,
				Z80PIO.Port        port )
  {
    if( port.listeners == null ) {
      port.listeners = new ArrayList<>();
    }
    port.listeners.add( listener );
  }


  private synchronized void removePIOPortListener(
				Z80PIOPortListener listener,
				Z80PIO.Port        port )
  {
    if( port.listeners != null ) {
      port.listeners.remove( listener );
    }
  }


  private int composeBitInValue( Z80PIO.Port port )
  {
    return (port.ioMask & port.inValue)
			| ((~port.ioMask) & port.outValue);
  }


  private void tryInterrupt( Z80PIO.Port port )
  {
    if( port.interruptEnabled ) {
      port.interruptRequested = true;
    }
  }


  private int fetchOutValue( Z80PIO.Port port, boolean strobe )
  {
    int rv = -1;
    if( strobe ) {
      rv = port.outValue;
      if( ((port.portInfo == PortInfo.A)
	   && ((port.mode == Mode.BYTE_OUT)
	       || (port.mode == Mode.BYTE_INOUT)))
	  || ((port.portInfo == PortInfo.B)
	      && (port.mode == Mode.BYTE_OUT)
	      && (this.portA.mode != Mode.BYTE_INOUT)) )
      {
	port.ready = false;
	tryInterrupt( port );
      }
    } else {
      rv = port.outValue;
    }
    return rv;
  }


  private boolean putInValue(
			Z80PIO.Port port,
			int         value,
			int         mask,
			boolean     strobe )
  {
    boolean rv = false;
    switch( port.mode ) {
      case BYTE_IN:
      case BYTE_INOUT:
	port.inValue = (value & mask) | (port.inValue & ~mask);
	if( strobe
	    && ((port.portInfo == PortInfo.A)
		|| ((port.portInfo == PortInfo.B)
		    && (port.mode == Mode.BYTE_IN)
		    && (this.portA.mode != Mode.BYTE_INOUT))) )
	{
	  port.ready = false;
	  tryInterrupt( port );
	}
	rv = true;
	break;

      case BIT_INOUT:
	port.inValue = (value & mask) | (port.inValue & ~mask);
	if( port.interruptEnabled ) {
	  boolean iCondRealized = false;
	  int     ioMask        = port.ioMask & ~port.interruptMask;
	  if( ioMask != 0 ) {
	    int v = composeBitInValue( port );
	    int m = port.interruptFireAtH ? v : ~v;
	    if( port.interruptBitsAnd ) {
	      if( (m & ioMask) == ioMask ) {
		iCondRealized = true;
	      }
	    } else {
	      if( (m & ioMask) != 0 ) {
		iCondRealized = true;
	      }
	    }
	  }

	  /*
	   * Interrupt nur ausloesen, wenn die Interrupt-Bedingungen
	   * vorher nicht erfuellt waren
	   */
	  if( iCondRealized && !port.interruptCondRealized ) {
	    port.interruptCondRealized = true;
	    tryInterrupt( port );
	  }
	  port.interruptCondRealized = iCondRealized;
	}
	rv = true;
	break;
    }
    return rv;
  }


  private void strobePort( Z80PIO.Port port )
  {
    if( ((port.portInfo == PortInfo.A)
	 && ((port.mode == Mode.BYTE_IN)
	     || (port.mode == Mode.BYTE_OUT)
	     || (port.mode == Mode.BYTE_INOUT)))
	|| ((port.portInfo == PortInfo.B)
	    && ((port.mode == Mode.BYTE_IN) || (port.mode == Mode.BYTE_OUT))
	    && (this.portA.mode != Mode.BYTE_INOUT))
	|| ((port.portInfo == PortInfo.B)
	    && (this.portA.mode == Mode.BYTE_INOUT)) )
    {
      tryInterrupt( port );
    }
  }


  private int readControl( Z80PIO.Port port )
  {
    int rv = 0x3F;		// BYTE_OUT
    switch( port.mode ) {
      case BYTE_IN:
	rv = 0x7F;
	break;

      case BYTE_INOUT:
	rv = 0xBF;
	break;

      case BIT_INOUT:
	rv = 0xFF;
	break;
    }
    return rv;
  }


  private int readData( Z80PIO.Port port )
  {
    int value = 0xFF;
    switch( port.mode ) {
      case BYTE_OUT:
	value = port.outValue;
	break;

      case BYTE_IN:
      case BYTE_INOUT:
	value = port.inValue;
	if( (port.portInfo == PortInfo.A)
	    || ((port.portInfo == PortInfo.B)
		&& (port.mode == Mode.BYTE_IN)
		&& (this.portA.mode != Mode.BYTE_INOUT)) )
	{
	  port.ready = true;
	}
	break;

      case BIT_INOUT:
	value = composeBitInValue( port );
	break;
    }
    if( (port.mode == Mode.BYTE_IN)
	|| (port.mode == Mode.BYTE_INOUT)
	|| (port.mode == Mode.BIT_INOUT) )
    {
      informListeners( port, Status.READY_FOR_INPUT );
    }
    return value;
  }


  private void writeControl( Z80PIO.Port port, int value )
  {
    switch( port.nextCtrl ) {
      case IO_MASK:
	port.ioMask   = value;
	port.nextCtrl = Ctrl.NONE;
	break;

      case INTERRUPT_MASK:
	port.interruptMask = value;
	port.nextCtrl      = Ctrl.NONE;
	break;

      default:
	if( (value & 0x0F) == 0x0F ) {		// Betriebsart
	  Mode oldMode = port.mode;
	  switch( (value >> 6) & 0x03 ) {
	    case 0:
	      port.mode = Mode.BYTE_OUT;
	      if( (port.portInfo == PortInfo.A)
		  || ((port.portInfo == PortInfo.B)
		      && (this.portA.mode != Mode.BYTE_INOUT)) )
	      {
		port.ready = false;
	      }
	      break;

	    case 2:
	      port.mode = Mode.BYTE_INOUT;
	      if( port.portInfo == PortInfo.A ) {
		port.ready = false;
	      }
	      break;

	    case 3:
	      port.mode     = Mode.BIT_INOUT;
	      port.nextCtrl = Ctrl.IO_MASK;
	      port.ready    = false;
	      break;

	    default:
	      port.mode = Mode.BYTE_IN;
	      if( (port.portInfo == PortInfo.A)
		  || ((port.portInfo == PortInfo.B)
		      && (this.portA.mode != Mode.BYTE_INOUT)) )
	      {
		port.ready = true;
	      }
	      break;
	  }
	  if( oldMode != port.mode ) {
	    informListeners( port, Status.MODE_CHANGED );
	    if( oldMode == Mode.BYTE_OUT ) {
	      informListeners( port, Status.READY_FOR_INPUT );
	    }
	  }
	}
	else if( (value & 0x01) == 0 ) {	// Interrupt-Vektor
	  port.interruptVector = value;
	}
	else if( (value & 0x0F) == 0x03 ) {	// Interrupt-Freigabe
	  setInterruptEnabled( port, (value & 0x80) != 0 );
	}
	else if( (value & 0x0F) == 0x07 ) {	// Interrupt-Steuerwort
	  if( (value & 0x10) != 0 ) {
	    port.nextCtrl = Ctrl.INTERRUPT_MASK;
	  }
	  port.interruptFireAtH      = ((value & 0x20) != 0);
	  port.interruptBitsAnd      = ((value & 0x40) != 0);
	  port.interruptCondRealized = false;
	  setInterruptEnabled( port, (value & 0x80) != 0 );
	}
    }
  }


  private void writeData( Z80PIO.Port port, int value )
  {
    int oldValue = port.outValue;
    port.outValue = value;
    if( ((port.portInfo == PortInfo.A)
	 && ((port.mode == Mode.BYTE_OUT) || (port.mode == Mode.BYTE_INOUT)))
	|| ((port.portInfo == PortInfo.B)
	    && (port.mode == Mode.BYTE_OUT)
	    && (this.portA.mode != Mode.BYTE_INOUT)) )
    {
      port.ready = true;
    }
    if( (port.mode == Mode.BYTE_OUT) || (port.mode == Mode.BYTE_INOUT) ) {
      informListeners( port, Status.OUTPUT_AVAILABLE );
    }
    else if( port.mode == Mode.BIT_INOUT ) {
      if( oldValue != value ) {
	informListeners( port, Status.OUTPUT_CHANGED );
      }
    }
  }


  private void informListeners( Z80PIO.Port port, Status status )
  {
    Collection<Z80PIOPortListener> listeners = port.listeners;
    if( listeners != null ) {
      for( Z80PIOPortListener listener : listeners ) {
	listener.z80PIOPortStatusChanged( this, port.portInfo, status );
      }
    }
  }


  private void setInterruptEnabled( Z80PIO.Port port, boolean state )
  {
    boolean oldState = port.interruptEnabled;
    port.interruptEnabled = state;
    if( state != oldState ) {
      informListeners(
	port,
	state ? Status.INTERRUPT_ENABLED : Status.INTERRUPT_DISABLED );
    }
  }
}

