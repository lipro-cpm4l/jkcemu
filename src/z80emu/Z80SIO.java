/*
 * (c) 2008-2017 Jens Mueller
 *
 * Z80-Emulator
 *
 * Emulation der Z80 SIO
 *
 * Es wird nur der asynchrone Uebertragungsmodus emuliert.
 */

package z80emu;

import java.lang.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


public class Z80SIO implements Z80InterruptSource
{
  private static final int RECEIVER_INTERRUPT = 0x01;
  private static final int SENDER_INTERRUPT   = 0x02;
  private static final int EXTERNAL_INTERRUPT = 0x04;

  private static final int RECV_BUFFER_FILLED = 0x100;
  private static final int RECV_FIFO_OVERRUN  = 0x200;

  private static final int RR0_CHAR_RECEIVED       = 0x01;
  private static final int RR0_INTERRUPT_PENDING   = 0x02;
  private static final int RR0_SENDER_BUFFER_EMPTY = 0x04;
  private static final int RR0_DCD                 = 0x08;
  private static final int RR0_SYNC                = 0x10;
  private static final int RR0_CTS                 = 0x20;
  private static final int RR0_TX_UNDERRUN_EOM     = 0x40;
  private static final int RR0_BREAK_ABORT         = 0x80;

  private static final int RR1_SENDER_EMPTY     = 0x01;
  private static final int RR1_PARITY_ERROR     = 0x10;
  private static final int RR1_RX_OVERRUN_ERROR = 0x20;
  private static final int RR1_FRAMING_ERROR    = 0x40;
  private static final int RR1_END_OF_FRAME     = 0x80;

  private static final int WR_SET                         = 0x100;
  private static final int WR1_EXTERNAL_INTERRUPT_ENABLED = 0x01;
  private static final int WR1_SENDER_INTERRUPT_ENABLED   = 0x02;
  private static final int WR1_STATUS_AFFECTS_VECTOR      = 0x04;
  private static final int WR3_RX_ENABLED                 = 0x01;
  private static final int WR4_PARITY_ENABLED             = 0x01;
  private static final int WR5_TX_ENABLED                 = 0x08;

  private static final String TEXT_NOT_INITIALIZED  = "nicht initialisiert";
  private static final String TEXT_SENDER_INTERRUPT = "Sender-Interrupt";
  private static final String TEXT_EXTERNAL_INTERRUPT
					= "Externer Status-Interrupt";

  private String    title;
  private Channel   a;
  private Channel   b;
  private Channel[] channels;


  public Z80SIO( String title )
  {
    this.title    = title;
    this.a        = new Channel( 0 );
    this.b        = new Channel( 1 );
    this.channels = new Channel[] { this.a, this.b };
  }


  public void addChannelListener(
			Z80SIOChannelListener listener,
			int                   channelNum )
  {
    if( (channelNum >= 0) && (channelNum < this.channels.length) ) {
      Channel channel = this.channels[ channelNum ];
      if( channel.listeners == null ) {
	channel.listeners = new ArrayList<>();
      }
      channel.listeners.add( listener );
    }
  }


  public int availableA()
  {
    return this.a.available();
  }


  public int availableB()
  {
    return this.b.available();
  }


  public boolean isReadyReceiverA()
  {
    return this.a.isReadyReceiver();
  }


  public boolean isReadyReceiverB()
  {
    return this.b.isReadyReceiver();
  }


  public void clockPulseReceiverA()
  {
    this.a.clockPulseReceiver();
  }


  public void clockPulseReceiverB()
  {
    this.b.clockPulseReceiver();
  }


  public void clockPulseSenderA()
  {
    this.a.clockPulseSender();
  }


  public void clockPulseSenderB()
  {
    this.b.clockPulseSender();
  }


  public void putToReceiverA( int value )
  {
    this.a.putToReceiver( value );
  }


  public void putToReceiverB( int value )
  {
    this.b.putToReceiver( value );
  }


  public void removeChannelListener(
			Z80SIOChannelListener listener,
			int                   channelNum )
  {
    if( (channelNum >= 0) && (channelNum < this.channels.length) ) {
      Channel channel = this.channels[ channelNum ];
      if( channel.listeners != null ) {
	channel.listeners.remove( listener );
      }
    }
  }


  public int readControlA()
  {
    return this.a.readControl();
  }


  public int readControlB()
  {
    return this.b.readControl();
  }


  public int readDataA()
  {
    return this.a.readData();
  }


  public int readDataB()
  {
    return this.b.readData();
  }


  public void setClearToSendA( boolean state )
  {
    this.a.setCTS( state );
  }


  public void setClearToSendB( boolean state )
  {
    this.b.setCTS( state );
  }


  public void setDataCarrierDetectA( boolean state )
  {
    this.a.setDCD( state );
  }


  public void setDataCarrierDetectB( boolean state )
  {
    this.b.setDCD( state );
  }


  public void writeControlA( int value )
  {
    this.a.writeControl( value );
  }


  public void writeControlB( int value )
  {
    this.b.writeControl( value );
  }


  public void writeDataA( int value )
  {
    this.a.writeData( value );
  }


  public void writeDataB( int value )
  {
    this.b.writeData( value );
  }


	/* --- Methoden fuer Z80InterruptSource --- */

  @Override
  public void appendInterruptStatusHTMLTo( StringBuilder buf )
  {
    buf.append( "<table border=\"1\">\n"
	+ "<tr><th></th><th>Kanal&nbsp;A</th><th>Kanal&nbsp;B</th></tr>\n"
	+ "<tr><td>Vorteiler:</td>" );
    for( int i = 0; i < this.channels.length; i++ ) {
      buf.append( "<td>" );
      String s   = TEXT_NOT_INITIALIZED;
      int    wr4 = this.channels[ i ].wr[ 4 ];
      if( wr4 >= 0 ) {
	switch( wr4 & 0xC0 ) {
	  case 0x40:
	    s = "16";
	    break;
	  case 0x80:
	    s = "32";
	    break;
	  case 0xC0:
	    s = "64";
	    break;
	}
      }
      buf.append( s );
      buf.append( "</td>" );
    }
    buf.append( "</tr>\n"
	+ "<tr>\n"
	+ "<td valign=\"top\">Empf&auml;nger:</td>\n" );
    for( int i = 0; i < this.channels.length; i++ ) {
      buf.append( "<td valign=\"top\">\n" );
      Channel channel = this.channels[ i ];
      if( (channel.wr[ 3 ] >= 0) && (channel.wr[ 4 ] >= 0) ) {
	buf.append( channel.getRxDataBitCount() );
	buf.append( " Bits pro Zeichen<br/>\n"
			+ "Empfang" );
	if( (channel.wr[ 3 ] & WR3_RX_ENABLED) != 0 ) {
	  buf.append( " freigegeben" );
	} else {
	  buf.append( " gesperrt" );
	}
	buf.append( "<br/>\n" );
	if( channel.recvClocksRemain > 0 ) {
	  buf.append( "Zeichen wird gerade empfangen...<br/>\n" );
	}
	buf.append( "Empfangspuffer" );
	synchronized ( channel ) {
	  int n = channel.recvFifoLen;
	  if( n > 0 ) {
	    buf.append( (char) ':' );
	    for( int k = 0; k < n; k++ ) {
	      if( k > 0 ) {
		buf.append( (char) ',' );
	      }
	      int b = channel.recvFifo[ i ];
	      buf.append( String.format( " %02Xh", b & 0xFF ) );
	      if( (b & RECV_FIFO_OVERRUN) != 0 ) {
		buf.append( " overrun" );
	      }
	    }
	  } else {
	    buf.append( " leer" );
	  }
	}
	buf.append( "<br/>\n"
			+ "Interrupt" );
	switch( channel.wr[ 1 ] & 0x18 ) {
	  case 0x08:
	    if( channel.recvNextInterruptEnabled ) {
	      buf.append(
		" freigegeben f&uuml;r ersten/n&auml;chstes Zeichen" );
	    } else {
	      buf.append( " freigegeben f&uuml;r ersten Zeichen"
					+ " (schon vorbei)" );
	    }
	    break;
	  case 0x10:
	  case 0x18:
	    buf.append( " freigegeben f&uuml;r jedes Zeichen" );
	    break;
	  default:
	    buf.append( " gesperrt" );
	}
	if( (channel.interruptAccepted & RECEIVER_INTERRUPT) != 0 ) {
	  buf.append( "<br/>\n"
			+ "Interrupt angenommen" );
	}
	if( (channel.interruptRequest & RECEIVER_INTERRUPT) != 0 ) {
	  buf.append( "<br/>\n"
			+ "Interrupt angemeldet" );
	}
      } else {
	buf.append( TEXT_NOT_INITIALIZED );
      }
      buf.append( "\n"
		+ "</td>\n" );
    }
    buf.append( "</tr>\n"
	+ "<tr>\n"
	+ "<td valign=\"top\">Sender:</td>\n" );
    for( int i = 0; i < this.channels.length; i++ ) {
      buf.append( "<td valign=\"top\">\n" );
      Channel channel = this.channels[ i ];
      if( (channel.wr[ 4 ] >= 0) && (channel.wr[ 5 ] >= 0) ) {
	buf.append( channel.getTxDataBitCount() );
	buf.append( " Bits pro Zeichen<br/>\n"
			+ "Senden" );
	if( (channel.wr[ 5 ] & WR5_TX_ENABLED) != 0 ) {
	  buf.append( " freigegeben" );
	} else {
	  buf.append( " gesperrt" );
	}
	buf.append( "<br/>\n" );
	if( (channel.rr[ 0 ] & RR0_SENDER_BUFFER_EMPTY) != 0 ) {
	  buf.append( "Puffer leer" );
	} else {
	  buf.append( String.format(
				"Byte %02Xh im Puffer",
				channel.sendBuf ) );
	  if( channel.sendClocksRemain > 0 ) {
	    buf.append( ", wird gerade gesendet..." );
	  }
	}
	buf.append( "<br/>\n"
			+ TEXT_SENDER_INTERRUPT );
	buf.append( (channel.wr[ 1 ] & WR1_SENDER_INTERRUPT_ENABLED) != 0 ?
			" freigegeben" : " gesperrt" );
	if( (channel.interruptAccepted & SENDER_INTERRUPT) != 0 ) {
	  buf.append( "<br/>\n"
			+ TEXT_SENDER_INTERRUPT + " angenommen" );
	}
	if( (channel.interruptRequest & SENDER_INTERRUPT) != 0 ) {
	  buf.append( "<br/>\n"
			+ TEXT_SENDER_INTERRUPT + " angemeldet" );
	}
	buf.append( "<br/>\n"
			+ TEXT_EXTERNAL_INTERRUPT );
	buf.append( (channel.wr[ 1 ] & WR1_EXTERNAL_INTERRUPT_ENABLED) != 0 ?
			" freigegeben" : " gesperrt" );
	if( (channel.interruptAccepted & EXTERNAL_INTERRUPT) != 0 ) {
	  buf.append( "<br/>\n"
			+ TEXT_EXTERNAL_INTERRUPT + " angenommen" );
	}
	if( (channel.interruptRequest & EXTERNAL_INTERRUPT) != 0 ) {
	  buf.append( "<br/>\n"
			+ TEXT_EXTERNAL_INTERRUPT + " angemeldet" );
	}
      } else {
	buf.append( TEXT_NOT_INITIALIZED );
      }
      buf.append( "\n"
		+ "</td>\n" );
    }
    buf.append( "</tr>\n"
	+ "<tr><td>Interrupt-Vektor:</td><td colspan=\"2\">" );
    if( this.b.wr[ 2 ] >= 0 ) {
      buf.append( String.format( "%02Xh", getInterruptVector() ) );
      if( (this.b.wr[ 1 ] & WR1_STATUS_AFFECTS_VECTOR) != 0 ) {
	buf.append( " (ver&auml;ndert)" );
      }
    } else {
      buf.append( "nicht gesetzt" );
    }
    buf.append( "</td></tr>\n"
		+ "</table>\n" );
  }


  @Override
  public synchronized int interruptAccept()
  {
    int rv = getInterruptVector();
    if( ((this.a.interruptAccepted & RECEIVER_INTERRUPT) == 0)
	&& ((this.a.interruptRequest & RECEIVER_INTERRUPT) != 0) )
    {
      this.a.interruptAccepted |= RECEIVER_INTERRUPT;
      this.a.interruptRequest &= ~RECEIVER_INTERRUPT;
    }
    else if( ((this.a.interruptAccepted & SENDER_INTERRUPT) == 0)
	     && ((this.a.interruptRequest & SENDER_INTERRUPT) != 0) )
    {
      this.a.interruptAccepted |= SENDER_INTERRUPT;
      this.a.interruptRequest &= ~SENDER_INTERRUPT;
    }
    else if( ((this.a.interruptAccepted & EXTERNAL_INTERRUPT) == 0)
	     && ((this.a.interruptRequest & EXTERNAL_INTERRUPT) != 0) )
    {
      this.a.interruptAccepted |= EXTERNAL_INTERRUPT;
      this.a.interruptRequest &= ~EXTERNAL_INTERRUPT;
    }
    else if( ((this.b.interruptAccepted & RECEIVER_INTERRUPT) == 0)
	     && ((this.b.interruptRequest & RECEIVER_INTERRUPT) != 0) )
    {
      this.b.interruptAccepted |= RECEIVER_INTERRUPT;
      this.b.interruptRequest &= ~RECEIVER_INTERRUPT;
    }
    else if( ((this.b.interruptAccepted & SENDER_INTERRUPT) == 0)
	     && ((this.b.interruptRequest & SENDER_INTERRUPT) != 0) )
    {
      this.b.interruptAccepted |= SENDER_INTERRUPT;
      this.b.interruptRequest &= ~SENDER_INTERRUPT;
    }
    else if( ((this.b.interruptAccepted & EXTERNAL_INTERRUPT) == 0)
	     && ((this.b.interruptRequest & EXTERNAL_INTERRUPT) != 0) )
    {
      this.b.interruptAccepted |= EXTERNAL_INTERRUPT;
      this.b.interruptRequest &= ~EXTERNAL_INTERRUPT;
    }
    return rv;
  }


  @Override
  public synchronized void interruptFinish()
  {
    if( (this.a.interruptAccepted & RECEIVER_INTERRUPT) != 0 ) {
      this.a.interruptAccepted &= ~RECEIVER_INTERRUPT;
    }
    else if( (this.a.interruptAccepted & SENDER_INTERRUPT) != 0 ) {
      this.a.interruptAccepted &= ~SENDER_INTERRUPT;
    }
    else if( (this.a.interruptAccepted & EXTERNAL_INTERRUPT) != 0 ) {
      this.a.interruptAccepted &= ~EXTERNAL_INTERRUPT;
    }
    else if( (this.b.interruptAccepted & RECEIVER_INTERRUPT) != 0 ) {
      this.b.interruptAccepted &= ~RECEIVER_INTERRUPT;
    }
    else if( (this.b.interruptAccepted & SENDER_INTERRUPT) != 0 ) {
      this.b.interruptAccepted &= ~SENDER_INTERRUPT;
    }
    else if( (this.b.interruptAccepted & EXTERNAL_INTERRUPT) != 0 ) {
      this.b.interruptAccepted &= ~EXTERNAL_INTERRUPT;
    }
  }


  @Override
  public boolean isInterruptAccepted()
  {
    return ((this.a.interruptAccepted
			& (RECEIVER_INTERRUPT
				| SENDER_INTERRUPT
				| EXTERNAL_INTERRUPT)) != 0)
	   || ((this.b.interruptAccepted
			& (RECEIVER_INTERRUPT
				| SENDER_INTERRUPT
				| EXTERNAL_INTERRUPT)) != 0);
  }


  @Override
  public boolean isInterruptRequested()
  {
    return ((this.a.interruptRequest
			& (RECEIVER_INTERRUPT
				| SENDER_INTERRUPT
				| EXTERNAL_INTERRUPT)) != 0)
	   || ((this.b.interruptRequest
			& (RECEIVER_INTERRUPT
				| SENDER_INTERRUPT
				| EXTERNAL_INTERRUPT)) != 0);
  }


  @Override
  public void reset( boolean powerOn )
  {
    this.a.reset( powerOn );
    this.b.reset( powerOn );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.title;
  }


	/* --- private Methoden --- */

  private class Channel
  {
    private int                               channelNum;
    private int                               interruptAccepted;
    private int                               interruptRequest;
    private boolean                           recvNextInterruptEnabled;
    private int                               recvBuf;
    private int                               recvClockDiv;
    private int                               recvClocksRemain;
    private int[]                             recvFifo;
    private int                               recvFifoLen;
    private int                               sendBuf;
    private int                               sendClockDiv;
    private int                               sendClocksRemain;
    private int[]                             rr;
    private int[]                             wr;
    private Boolean                           cts;
    private Boolean                           dcd;
    private Collection<Z80SIOChannelListener> listeners;


    private Channel( int channelNum )
    {
      this.channelNum = channelNum;
      this.recvFifo   = new int[ 3 ];
      this.rr         = new int[ 2 ];
      this.wr         = new int[ 8 ];
      this.cts        = null;
      this.dcd        = null;
      this.listeners  = null;
      reset( true );
    }


    private int available()
    {
      return this.recvFifoLen;
    }


    private void checkRequestExternalInterrupt(
					Boolean oldState,
					boolean newState )
    {
      if( (oldState != null)
	  && ((this.wr[ 1 ] & WR1_EXTERNAL_INTERRUPT_ENABLED) != 0) )
      {
	if( newState != oldState.booleanValue() ) {
	  this.interruptRequest |= EXTERNAL_INTERRUPT;
	  setInterruptPending();
	}
      }
    }


    private synchronized void clockPulseReceiver()
    {
      if( (this.wr[ 3 ] > 0)		// Empfaengereinstellungen
	  && (this.wr[ 4 ] > 0) )	// Vorteiler
      {
	if( this.recvClockDiv == 0 ) {
	  this.recvClockDiv = getClockDiv() - 1;
	}
	if( this.recvClockDiv > 0 ) {
	  --this.recvClockDiv;
	}
	if( this.recvClockDiv == 0 ) {

	  // Empfangsprozess beginnen?
	  if( (this.recvClocksRemain == 0)
	      && ((this.wr[ 3 ] & WR3_RX_ENABLED) != 0)
	      && ((this.recvBuf & RECV_BUFFER_FILLED) != 0) )
	  {
	    // Anzahl der zu ubertragenden Bits
	    this.recvClocksRemain = getRxDataBitCount()
					+ getParityAndStopBitCount();
	  }

	  // Empfangsprozess?
	  if( this.recvClocksRemain > 0 ) {
	    --this.recvClocksRemain;
	    if( this.recvClocksRemain == 0 ) {
	      int b        = this.recvBuf & 0xFF;
	      this.recvBuf = 0;
	      switch( this.wr[ 3 ] & 0xC0 ) {
		case 0x00:
		  b &= 0x1F;	// 5 Bits
		  break;
		case 0x80:
		  b &= 0x3F;	// 6 Bits
		  break;
		case 0x40:
		  b &= 0x7F;	// 7 Bits
		  break;
	      }
	      if( this.recvFifoLen < this.recvFifo.length ) {
		this.recvFifo[ this.recvFifoLen++ ] = b;
	      } else {
		this.recvFifo[ this.recvFifo.length - 1 ]
					= b | RECV_FIFO_OVERRUN;
	      }
	      int interruptMode = this.wr[ 1 ] & 0x18;
	      if( ((interruptMode == 0x08) && this.recvNextInterruptEnabled)
		  || (interruptMode == 0x10)
		  || (interruptMode == 0x18) )
	      {
		this.interruptRequest |= RECEIVER_INTERRUPT;
		setInterruptPending();
	      }
	      this.recvNextInterruptEnabled = false;
	    }
	  }
	}
      }
    }


    private synchronized void clockPulseSender()
    {
      if( (this.wr[ 4 ] > 0)		// Vorteiler
	  && (this.wr[ 5 ] > 0) )	// Sendereinstellungen
      {
	if( this.sendClockDiv == 0 ) {
	  this.sendClockDiv = getClockDiv() - 1;
	}
	if( this.sendClockDiv > 0 ) {
	  --this.sendClockDiv;
	}
	if( this.sendClockDiv == 0 ) {

	  // Sendeprozess beginnen?
	  if( (this.sendClocksRemain == 0)
	      && ((this.rr[ 0 ] & RR0_SENDER_BUFFER_EMPTY) == 0)
	      && ((this.wr[ 5 ] & WR5_TX_ENABLED) != 0) )
	  {

	    // Anzahl der zu ubertragenden Bits
	    this.sendClocksRemain = getTxDataBitCount()
					+ getParityAndStopBitCount();
	  }

	  // Sendeprozess?
	  if( this.sendClocksRemain > 0 ) {
	    --this.sendClocksRemain;
	    if( this.sendClocksRemain == 0 ) {
	      int b = this.sendBuf & 0xFF;
	      switch( this.wr[ 5 ] & 0x60 ) {
		case 0x00:
		  b &= 0x1F;	// 5 Bits
		  break;
		case 0x40:
		  b &= 0x3F;	// 6 Bits
		  break;
		case 0x20:
		  b &= 0x7F;	// 7 Bits
		  break;
	      }
	      this.rr[ 0 ] |= RR0_SENDER_BUFFER_EMPTY;
	      this.rr[ 1 ] |= RR1_SENDER_EMPTY;
	      if( (this.wr[ 1 ] & WR1_SENDER_INTERRUPT_ENABLED) != 0 ) {
		this.interruptRequest |= SENDER_INTERRUPT;
		setInterruptPending();
	      }
	      fireByteAvailable( this, b );
	    }
	  }
	}
      }
    }


    private int getClockDiv()
    {
      int rv = 1;
      switch( this.wr[ 4 ] & 0xC0 ) {
	case 0x40:
	  rv = 16;
	  break;
	case 0x80:
	  rv = 32;
	  break;
	case 0xC0:
	  rv = 64;
	  break;
      }
      return rv;
    }


    private int getParityAndStopBitCount()
    {
      int rv = 0;
      switch( this.wr[ 4 ] & 0x18 ) {
	case 0x08:		// 1 Stoppbit
	  rv = 1;
	  break;
	case 0x10:		// 1,5 Stoppbits
	case 0x18:		// 2 Stoppbits
	  rv = 2;
	  break;
      }
      if( (this.wr[ 4 ] & WR4_PARITY_ENABLED) != 0 ) {
	rv++;
      }
      return rv;
    }


    private int getRxDataBitCount()
    {
      int rv = 8;
      switch( this.wr[ 5 ] & 0xC0 ) {
	case 0x00:
	  rv = 5;
	  break;
	case 0x40:
	  rv = 7;
	  break;
	case 0x80:
	  rv = 6;
	  break;
      }
      return rv;
    }


    private int getTxDataBitCount()
    {
      int rv = 8;
      switch( this.wr[ 5 ] & 0x60 ) {
	case 0x00:
	  rv = 5;
	  break;
	case 0x20:
	  rv = 7;
	  break;
	case 0x40:
	  rv = 6;
	  break;
      }
      return rv;
    }


    private synchronized boolean isReadyReceiver()
    {
      boolean rv = false;
      if( (this.wr[ 3 ] > 0)			// Empfaengereinstellungen
	  && (this.wr[ 4 ] > 0)			// Vorteiler
	  && (this.recvClocksRemain == 0)	// kein laufender Prozess
	  && ((this.wr[ 3 ] & WR3_RX_ENABLED) != 0) )
      {
	rv = true;
      }
      return rv;
    }


    private boolean isSpecialReceiveCondition()
    {
      boolean rv = ((this.rr[ 0 ] & (RR1_RX_OVERRUN_ERROR
					| RR1_FRAMING_ERROR
					| RR1_END_OF_FRAME)) != 0);
      if( ((this.rr[ 0 ] & RR1_PARITY_ERROR) != 0)
	  && ((this.wr[ 1 ] & 0x18) == 0x10) )
      {
	rv = true;
      }
      return rv;
    }


    private void putToReceiver( int value )
    {
      if( this.recvClocksRemain == 0 ) {
	this.recvBuf = value | RECV_BUFFER_FILLED;
      }
    }


    private synchronized int readControl()
    {
      int rv     = 0;
      int regNum = this.wr[ 0 ] & 0x07;
      if( (regNum >= 0) && (regNum < 2) ) {
	rv = this.rr[ regNum ];
	if( regNum == 0 ) {
	  if( this.recvFifoLen > 0 ) {
	    rv |= RR0_CHAR_RECEIVED;
	  }
	  if( this.cts != null ) {
	    if( this.cts.booleanValue() ) {
	      rv |= RR0_CTS;
	    }
	  }
	  if( this.dcd != null ) {
	    if( this.dcd.booleanValue() ) {
	      rv |= RR0_DCD;
	    }
	  }
	}
      } else if( (regNum == 2) && (this.channelNum == 1) ) {
	rv = getInterruptVector();
      }
      return rv;
    }


    private synchronized int readData()
    {
      int rv = 0;
      if( this.recvFifoLen > 0 ) {
	rv = this.recvFifo[ 0 ] & 0xFF;
	for( int i = 1; i < this.recvFifo.length; i++ ) {
	  this.recvFifo[ i - 1 ] = this.recvFifo[ i ];
	}
	this.recvFifo[ this.recvFifo.length - 1 ] = 0;
	--this.recvFifoLen;
	this.rr[ 1 ] &= ~RR1_RX_OVERRUN_ERROR;
	if( this.recvFifoLen > 0 ) {
	  if( (this.recvFifo[ 0 ] & RECV_FIFO_OVERRUN) != 0 ) {
	    this.rr[ 1 ] |= RR1_RX_OVERRUN_ERROR;
	  }
	}
      }
      return rv;
    }


    private void reset( boolean powerOn )
    {
      this.interruptAccepted = 0;
      this.interruptRequest  = 0;

      this.recvNextInterruptEnabled = true;
      this.recvBuf                  = 0;
      this.recvClockDiv             = 0;
      this.recvClocksRemain         = 0;
      this.recvFifoLen              = 0;
      Arrays.fill( this.recvFifo, 0 );

      this.sendBuf          = 0;
      this.sendClockDiv     = 0;
      this.sendClocksRemain = 0;

      this.rr[ 0 ] = RR0_SENDER_BUFFER_EMPTY | RR0_TX_UNDERRUN_EOM;
      this.rr[ 1 ] = RR1_SENDER_EMPTY;
      // WR2 bis WR5 als nicht initialisiert markieren
      for( int i = 0; i < this.wr.length; i++ ) {
	this.wr[ i ] = ((i>= 2) && (i <= 5) ? -1 : 0);
      }

      if( powerOn ) {
	this.cts = false;
	this.dcd = false;
      }
    }


    private void resetSenderInterrupt()
    {
      this.interruptRequest &= ~SENDER_INTERRUPT;
    }


    private synchronized void setCTS( boolean state )
    {
      Boolean oldState = this.cts;
      this.cts         = state;
      checkRequestExternalInterrupt( oldState, state );
    }


    private synchronized void setDCD( boolean state )
    {
      Boolean oldState = this.dcd;
      this.dcd         = state;
      checkRequestExternalInterrupt( oldState, state );
    }


    private synchronized void writeControl( int value )
    {
      int regNum        = this.wr[ 0 ] & 0x07;
      this.wr[ regNum ] = value & 0xFF;
      if( (regNum == 1) && ((value & 0x18) != 0) ) {
	this.recvNextInterruptEnabled = true;
      }
      switch( regNum ) {
	case 0:
	  switch( value & 0x38 ) {
	    case 0x10:			// RESET Extern/Status Interrupts
	      this.interruptRequest &= ~EXTERNAL_INTERRUPT;
	      break;
	    case 0x18:			// Kanal-RESET
	      reset( false );
	      break;
	    case 0x20:			// erneute Interruptfreigabe
	      this.recvNextInterruptEnabled = true;
	      break;
	    case 0x24:			// RESET Sender Interrupt
	      resetSenderInterrupt();
	      break;
	    case 0x30:			// Fehler-RESET
	      this.rr[ 1 ] &= ~RR1_PARITY_ERROR;
	      this.rr[ 1 ] &= ~RR1_RX_OVERRUN_ERROR;
	      break;
	    case 0x38:			// RETI (nur Kanal A)
	      if( this.channelNum == 0 ) {
		interruptFinish();
	      }
	      break;
	  }
	  break;
      }
      if( regNum != 0 ) {
	this.wr[ 0 ] &= 0xF8;		// Registerzeiger zuruecksetzen
      }
      if( (value & 0xC0) == 0xC0 ) {
	this.rr[ 0 ] &= ~RR0_TX_UNDERRUN_EOM;
      }
    }


    private synchronized void writeData( int value )
    {
      this.sendBuf = value;
      resetSenderInterrupt();
      this.rr[ 0 ] &= ~RR0_SENDER_BUFFER_EMPTY;
      this.rr[ 1 ] &= ~RR1_SENDER_EMPTY;
    }
  }


  private synchronized void fireByteAvailable( Channel channel, int value )
  {
    if( channel.listeners != null ) {
      for( Z80SIOChannelListener listener : channel.listeners ) {
 	listener.z80SIOByteSent( this, channel.channelNum, value );
      }
    }
  }


  private int getInterruptVector()
  {
    int rv = this.b.wr[ 2 ] & 0xFF;
    if( (this.b.wr[ 1 ] >= 0)
	&& (this.b.wr[ 1 ] & WR1_STATUS_AFFECTS_VECTOR) != 0 )
    {
      int ivOffs = 0x06;
      if( (this.a.interruptRequest & RECEIVER_INTERRUPT) != 0 ) {
	if( this.a.isSpecialReceiveCondition() ) {
	  ivOffs = 0x0E;
	} else {
	  ivOffs = 0x0C;
	}
      }
      else if( (this.a.interruptRequest & SENDER_INTERRUPT) != 0 ) {
	ivOffs = 0x08;
      }
      else if( (this.a.interruptRequest & EXTERNAL_INTERRUPT) != 0 ) {
	ivOffs = 0x0A;
      }
      else if( (this.b.interruptRequest & RECEIVER_INTERRUPT) != 0 ) {
	if( this.b.isSpecialReceiveCondition() ) {
	  ivOffs = 0x06;
	} else {
	  ivOffs = 0x04;
	}
      }
      else if( (this.b.interruptRequest & SENDER_INTERRUPT) != 0 ) {
	ivOffs = 0x00;
      }
      else if( (this.b.interruptRequest & EXTERNAL_INTERRUPT) != 0 ) {
	ivOffs = 0x02;
      }
      rv &= 0xF1;
      rv |= ivOffs;
    }
    return rv;
  }


  private void setInterruptPending()
  {
    this.a.rr[ 0 ] |= RR0_INTERRUPT_PENDING;
  }
}
