/*
 * (c) 2008-2020 Jens Mueller
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

import java.util.ArrayList;
import java.util.Collection;


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
		READY_FOR_INPUT,
		OUTPUT_AVAILABLE,
		OUTPUT_CHANGED };

  private enum Ctrl { NONE, INPUT_BIT_MASK, INTERRUPT_MASK };

  private String      title;
  private boolean     resetState;
  private Z80PIO.Port portA;
  private Z80PIO.Port portB;


  public Z80PIO( String title )
  {
    this.title      = title;
    this.resetState = true;
    this.portA      = new Z80PIO.Port( PortInfo.A );
    this.portB      = new Z80PIO.Port( PortInfo.B );
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
   * Diese Methoden bieten Zugriff auf den Interrupt-Vektor
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
   * Wenn die Ausgaenge hochohmig sind,
   * wird der Wert in "defaultValue" zurueckgeliefert.
   * "defaultValue" ist somit der Wert,
   * der ohne Ausgabe durch die PIO am PIO-Tor standardmaessig anliegt.
   * Ist der Parameter "strobe" gleich true, bedeutet das,
   * dass beim Auslesen die Strobe-Leitung aktiviert wurde.
   * Je nach Betriebsart und Interrupt-Freigabe
   * kann dadurch ein Interrupt ausgeloest werden.
   */
  public synchronized int fetchOutValuePortA( int defaultValue )
  {
    return fetchOutValue( this.portA, defaultValue, false );
  }

  public synchronized int fetchOutValuePortA(
					int     defaultValue,
					boolean strobe )
  {
    return fetchOutValue( this.portA, defaultValue, strobe );
  }

  public synchronized int fetchOutValuePortB( int defaultValue )
  {
    return fetchOutValue( this.portB, defaultValue, false );
  }

  public synchronized int fetchOutValuePortB(
					int     defaultValue,
					boolean strobe )
  {
    return fetchOutValue( this.portB, defaultValue, strobe );
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
  public synchronized int readDataA()
  {
    return readData( this.portA );
  }


  public synchronized int readDataB()
  {
    return readData( this.portB );
  }


  public synchronized void reset( boolean powerOn )
  {
    this.portA.reset( powerOn );
    this.portB.reset( powerOn );
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
      if( this.resetState ) {
	buf.append( " (RESET-Zustand)" );
      }
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>E/A-Maske:</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      buf.append( "<td>" );
      if( ports[ i ].mode == Mode.BIT_INOUT ) {
	int v = ports[ i ].inDirMask;
	for( int k = 0; k < 8; k++ ) {
	  buf.append( (v & 0x80) != 0 ? 'E' : 'A' );
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
      buf.append( String.format( "<td>%02Xh</td>", ports[ i ].inReg ) );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Ausgabe-Register:</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      buf.append( String.format( "<td>%02Xh</td>", ports[ i ].outReg ) );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Interrupt-Status:</td>" );
    for( int i = 0; i < ports.length; i++ ) {
      buf.append( "<td>" );
      if( ports[ i ].interruptAccepted ) {
	buf.append( "angenommen (wird gerade bedient)" );
      } else if( ports[ i ].interruptRequested ) {
	if( ports[ i ].interruptEnabled ) {
	  buf.append( "angemeldet" );
	} else { 
	  buf.append( "wird nach Freigabe angemeldet (interner IRQ)" );
	}
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
	  buf.append( (v & 0x80) == 0 ? ch : '-' );
	  v <<= 1;
	  --ch;
	}
	buf.append( ')' );
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
	buf.append( ports[ i ].interruptFireAtH ? 'H' : 'L' );
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
    if( !this.portA.interruptAccepted
	&& this.portA.interruptEnabled
	&& this.portA.interruptRequested )
    {
      this.portA.interruptAccepted  = true;
      this.portA.interruptRequested = false;
      rv = this.portA.interruptVector;
    }
    else if( !this.portB.interruptAccepted
	     && this.portB.interruptEnabled
	     && this.portB.interruptRequested )
    {
      this.portB.interruptAccepted  = true;
      this.portB.interruptRequested = false;
      rv = this.portB.interruptVector;
    }
    return rv;
  }


  @Override
  public synchronized boolean interruptFinish( int addr )
  {
    boolean rv = false;
    if( this.portA.interruptAccepted ) {
      this.portA.interruptAccepted = false;
      rv                           = true;
    }
    else if( this.portB.interruptAccepted ) {
      this.portB.interruptAccepted = false;
      rv                           = true;
    }
    return rv;
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


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.title != null ? this.title : "PIO";
  }


	/* --- private Methoden --- */

  private class Port
  {
    private PortInfo                       portInfo;
    private int                            outReg;
    private int                            inReg;
    private int                            inDirMask;
    private int                            valueMask;
    private boolean                        ready;
    private Mode                           mode;
    private Ctrl                           nextCtrl;
    private int                            interruptVector;
    private int                            interruptMask;
    private boolean                        interruptFireAtH;
    private boolean                        interruptBitsAnd;
    private boolean                        interruptEnabled;
    private volatile boolean               interruptAccepted;
    private volatile boolean               interruptRequested;
    private boolean                        interruptCondFulfilled;
    private Collection<Z80PIOPortListener> listeners;


    private Port( PortInfo portInfo )
    {
      this.portInfo  = portInfo;
      this.listeners = null;
      reset( true );
    }


    private void reset( boolean powerOn )
    {
      if( powerOn ) {
	this.interruptVector = 0;
        this.inReg           = 0;
      }
      this.mode                   = Mode.BYTE_IN;
      this.nextCtrl               = Ctrl.NONE;
      this.outReg                 = 0;
      this.inDirMask              = 0;
      this.valueMask              = 0;
      this.interruptMask          = 0;
      this.interruptFireAtH       = false;
      this.interruptBitsAnd       = false;
      this.interruptEnabled       = false;
      this.interruptAccepted      = false;
      this.interruptRequested     = false;
      this.interruptCondFulfilled = false;
      this.ready                  = false;
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


  private void checkBitIOInterrupt( Z80PIO.Port port )
  {
    int mask = port.valueMask & ~port.interruptMask & 0xFF;
    if( mask != 0 ) {
      boolean fulfilled = false;
      int     portValue = composeInOutBits( port );
      int     bitStates = 0;
      if( port.interruptFireAtH ) {
	bitStates = portValue;
      } else {
	bitStates = ~portValue;
      }
      bitStates &= mask;
      if( port.interruptBitsAnd ) {
	if( bitStates == mask ) {
	  fulfilled = true;
	}
      } else {
	if( bitStates != 0 ) {
	  fulfilled = true;
	}
      }

      /*
       * Interrupt-Flag nur setzen,
       * wenn die Interrupt-Bedingungen vorher nicht erfuellt waren
       */
      if( fulfilled && !port.interruptCondFulfilled ) {
	port.interruptRequested = true;
      }
      port.interruptCondFulfilled = fulfilled;
    }
  }


  private int composeInOutBits( Z80PIO.Port port )
  {
    return (port.inDirMask & port.inReg) | ((~port.inDirMask) & port.outReg);
  }


  private int fetchOutValue(
			Z80PIO.Port port,
			int         defaultValue,
			boolean     strobe )
  {
    int rv = defaultValue;
    switch( port.mode ) {
      case BYTE_OUT:
      case BYTE_INOUT:
	rv = port.outReg;
	break;
      case BIT_INOUT:
	rv = ((port.inDirMask & defaultValue)
			| ((~port.inDirMask) & port.outReg));
	break;
    }
    if( strobe ) {
      strobePort( port );
    }
    return rv;
  }


  private boolean putInValue(
			Z80PIO.Port port,
			int         value,
			int         mask,
			boolean     strobe )
  {
    boolean rv    = false;
    int     inReg = ((value & mask) | (port.inReg & ~mask)) & 0xFF;
    switch( port.mode ) {
      case BYTE_IN:
      case BYTE_INOUT:
	port.inReg     = inReg;
	port.valueMask = 0xFF;
	if( strobe ) {
	  strobePort( port );
	}
	rv = true;
	break;

      case BYTE_OUT:
	if( strobe ) {
	  strobePort( port );
	}
	rv = true;
	break;

      case BIT_INOUT:
	port.inReg = inReg;
	port.valueMask |= (mask & port.inDirMask & 0xFF);
	checkBitIOInterrupt( port );
	rv = true;
	break;
    }
    return rv;
  }


  private int readData( Z80PIO.Port port )
  {
    int value = 0xFF;
    switch( port.mode ) {
      case BYTE_OUT:
	value = port.outReg;
	break;

      case BYTE_IN:
      case BYTE_INOUT:
	value = port.inReg;
	if( (port.portInfo == PortInfo.A)
	    || ((port.portInfo == PortInfo.B)
		&& (port.mode == Mode.BYTE_IN)
		&& (this.portA.mode != Mode.BYTE_INOUT)) )
	{
	  port.ready = true;
	}
	break;

      case BIT_INOUT:
	value = composeInOutBits( port );
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
      port.interruptRequested = true;
    }
  }


  private void writeControl( Z80PIO.Port port, int value )
  {
    value &= 0xFF;

    // Das erste Control-Byte hebt den RESET-Zustand fuer beide Ports auf.
    this.resetState = false;

    boolean outChanged = false;
    switch( port.nextCtrl ) {
      case INPUT_BIT_MASK:
	port.nextCtrl = Ctrl.NONE;
	if( value != port.inDirMask ) {
	  port.inDirMask = value;
	  outChanged     = true;
	}
	break;

      case INTERRUPT_MASK:
	port.nextCtrl      = Ctrl.NONE;
	port.interruptMask = value;
	break;

      default:
	if( (value & 0x0F) == 0x0F ) {		// Betriebsart
	  Mode oldMode = port.mode;
	  switch( (value >> 6) & 0x03 ) {
	    case 0:
	      port.mode = Mode.BYTE_OUT;
	      break;

	    case 2:
	      port.mode = Mode.BYTE_INOUT;
	      break;

	    case 3:
	      port.mode     = Mode.BIT_INOUT;
	      port.nextCtrl = Ctrl.INPUT_BIT_MASK;
	      break;

	    default:
	      port.mode = Mode.BYTE_IN;
	      break;
	  }
	  if( port.mode != oldMode ) {
	    outChanged = true;
	  }

	  /*
	   * Das READY-Signal wird immer inaktiv, ausser,
	   * es handelt sich um Port B und Port A
	   * laeuft im Byte-IN/Out-Betrieb
	   */
	  if( (port.portInfo == PortInfo.A)
	      || ((port.portInfo == PortInfo.B)
		  && (this.portA.mode != Mode.BYTE_INOUT)) )
	  {
	    port.ready = false;
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
	    if( !port.interruptAccepted ) {
	      /*
	       * anstehenden aber noch nicht angenommenen Interrupt
	       * zuruecksetzen
	       */
	      port.interruptRequested = false;
	    }
	  }
	  port.interruptFireAtH = ((value & 0x20) != 0);
	  port.interruptBitsAnd = ((value & 0x40) != 0);
	  setInterruptEnabled( port, (value & 0x80) != 0 );
	}
    }
    if( outChanged ) {
      informListeners( port, Status.OUTPUT_CHANGED );
    }
  }


  private void writeData( Z80PIO.Port port, int value )
  {
    if( !this.resetState ) {
      int oldPortValue = composeInOutBits( port );
      port.outReg      = value;
      port.valueMask   = 0xFF;
      if( ((port.portInfo == PortInfo.A)
		&& ((port.mode == Mode.BYTE_OUT)
			|| (port.mode == Mode.BYTE_INOUT)))
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
	if( (oldPortValue & ~port.inDirMask)
		!= (port.outReg & ~port.inDirMask) )
	{
	  informListeners( port, Status.OUTPUT_CHANGED );
	}
	checkBitIOInterrupt( port );
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
