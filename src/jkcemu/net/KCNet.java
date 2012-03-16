/*
 * (c) 2011-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation von KCNet
 */

package jkcemu.net;

import java.lang.*;
import java.util.Arrays;
import jkcemu.Main;
import z80emu.*;


public class KCNet implements
			Z80InterruptSource,
			Z80MaxSpeedListener,
			Z80PIOPortListener,
			Z80TStatesListener
{
  // KCNET Hardware-Version 1.2
  private static final int HW_VERSION = 0x0102;

  // KCNET Software-Version 1.2
  private static final int SW_VERSION = 0x0102;

  // Bereich, aus dem KCNET dynamische Portnummern liefert
  private static final int PORT_NUM_MIN = 49152;
  private static final int PORT_NUM_MAX = 65535;

  // KCNET-Kennung, aehnlich aber nicht identisch zum Original
  private static final String ID_TEXT = "###     KCNET     ### \r\n"
					+ " WIZnet TCP/IP-Stack  \r\n"
					+ "###   by JKCEMU   ### \r\n";

  private enum Command {
		NONE,
		WRITE_BYTES,
		READ_BYTES,
		WRITE_ADDR,
		READ_TIMER,
		WRITE_BYTE,
		READ_BYTE,
		WRITE_IP_ADDR,
		READ_IP_ADDR,
		READ_NEXT_PORT_NUM,
		READ_SW_VERSION,
		READ_HW_VERSION,
		READ_LINK_STATUS,
		READ_ID,
		READ_ERROR_CNT };

  private static Command[] commands = {
			Command.WRITE_BYTES,		// 0
			Command.READ_BYTES,		// 1
			Command.WRITE_ADDR,		// 2
			Command.READ_TIMER,		// 3
			Command.WRITE_BYTE,		// 4
			Command.READ_BYTE,		// 5
			Command.WRITE_IP_ADDR,		// 6
			Command.READ_IP_ADDR,		// 7
			Command.READ_NEXT_PORT_NUM,	// 8
			Command.READ_SW_VERSION,	// 9
			Command.READ_HW_VERSION,	// 10
			Command.READ_LINK_STATUS,	// 11
			Command.READ_ID,		// 12
			Command.READ_ERROR_CNT };	// 13

  private static byte[] idBytes = null;

  private String   title;
  private Command  cmd;
  private int      debugLevel;
  private int[]    args;
  private int      argIdx;
  private int      curAddr;
  private int      byteCnt;
  private int      errorCnt;
  private int      portSeqNum;
  private int      tStatesPerMilli;
  private long     tStatesCounterValue;
  private long     tStatesCounterWrap;
  private long     tStatesToTimeout;
  private int      resultPos;
  private byte[]   resultBytes;
  private byte[]   doubleByteBuf;
  private byte[]   emptyIPAddr;
  private byte[][] ipAddrMem;
  private byte[]   dnsIPAddr;
  private boolean  dnsChecked;
  private W5100    w5100;
  private Z80PIO   pio;


  public KCNet( String title )
  {
    this.title         = title;
    this.debugLevel    = 0;
    this.portSeqNum    = PORT_NUM_MIN;
    this.args          = new int[ 5 ];
    this.doubleByteBuf = new byte[ 2 ];
    this.emptyIPAddr   = new byte[ 4 ];
    Arrays.fill( this.emptyIPAddr, (byte) 0 );

    this.ipAddrMem  = new byte[ 8 ][];
    for( int i = 0; i < this.ipAddrMem.length; i++ ) {
      this.ipAddrMem[ i ] = new byte[ 4 ];
    }
    this.dnsChecked = false;
    this.dnsIPAddr  = null;
    this.w5100      = new W5100();
    this.pio        = new Z80PIO( title );
    this.pio.addPIOPortListener( this, Z80PIO.PortInfo.A );

    String text = System.getProperty( "jkcemu.debug.net" );
    if( text != null ) {
      try {
	this.debugLevel = Integer.parseInt( text );
      }
      catch( NumberFormatException ex ) {}
    }
  }


  public void die()
  {
    this.pio.removePIOPortListener( this, Z80PIO.PortInfo.A );
    this.w5100.die();
  }


  public int read( int port )
  {
    int rv = -1;
    switch( port & 0x03 ) {
      case 0x00:
	rv = this.pio.readPortA();
	if( this.debugLevel > 2 ) {
	  System.out.printf( "KCNet read: %02X\n", rv );
	}
	break;

      case 0x01:
	rv = this.pio.readPortB();
	break;

      case 0x02:
	rv = this.pio.readControlA();
	break;

      case 0x03:
	rv = this.pio.readControlB();
	break;
    }
    return rv;
  }


  public void write( int port, int value )
  {
    switch( port & 0x03 ) {
      case 0x00:
	if( this.debugLevel > 2 ) {
	  System.out.printf( "KCNet write: %02X\n", value );
	}
	this.pio.writePortA( value );
	break;

      case 0x01:
	this.pio.writePortB( value );
	break;

      case 0x02:
	this.pio.writeControlA( value );
	break;

      case 0x03:
	this.pio.writeControlB( value );
	break;
    }
  }


	/* --- Z80InterruptSource --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf )
  {
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
  public void reset( boolean powerOn )
  {
    if( this.debugLevel > 0 ) {
      System.out.printf( "KCNet reset: power_on=%b\n", powerOn );
    }
    this.pio.reset( powerOn );
    this.w5100.reset( powerOn );
    setIdle();
    if( powerOn ) {
      this.curAddr             = 0;
      this.errorCnt            = 0;
      this.tStatesCounterValue = 0;
      for( byte[] ipAddr : this.ipAddrMem ) {
	Arrays.fill( ipAddr, (byte) 0 );
      }
      byte[] ipAddr = NetUtil.getIPAddr(
			Main.getProperty( "jkcemu.kcnet.dns_server" ) );
      if( (ipAddr == null)
	  && Main.getBooleanProperty( "jkcemu.kcnet.auto_config", true ) )
      {
	if( !this.dnsChecked ) {
	  this.dnsChecked = true;
	  this.dnsIPAddr  = NetUtil.getDNSServerIPAddr();
	  if( this.dnsIPAddr != null ) {
	    if( this.dnsIPAddr.length != 4 ) {
	      this.dnsIPAddr = null;
	    }
	  }
	}
	ipAddr = this.dnsIPAddr;
      }
      if( ipAddr != null ) {
	if( ipAddr.length == 4 ) {
	  for( int i = 0; i < ipAddr.length; i++ ) {
	    this.ipAddrMem[ 0 ][ i ] = ipAddr[ i ];
	  }
	}
      }
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.tStatesPerMilli    = cpu.getMaxSpeedKHz();
    this.tStatesCounterWrap = this.tStatesPerMilli * 60000L;
  }


	/* --- Z80TStatesListener --- */

  @Override
  public synchronized void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( this.tStatesCounterWrap > 0 ) {
      long v = this.tStatesCounterValue + tStates;
      while( v > this.tStatesCounterWrap ) {
	v -= this.tStatesCounterWrap;
      }
      this.tStatesCounterValue = v;
      if( this.tStatesToTimeout > 0 ) {
	this.tStatesToTimeout -= tStates;
	if( this.tStatesToTimeout <= 0 ) {
	  if( this.debugLevel > 0 ) {
	    System.out.println( "KCNet timeout" );
	  }
	  this.errorCnt = (this.errorCnt + 1) & 0xFFFF;
	  reset( false );
	}
      }
    }
  }


	/* --- Z80PIOPortListener --- */

  @Override
  public void z80PIOPortStatusChanged(
				Z80PIO          pio,
				Z80PIO.PortInfo port,
				Z80PIO.Status   status )
  {
    if( (pio == this.pio) && (port == Z80PIO.PortInfo.A) ) {
      if( status == Z80PIO.Status.OUTPUT_AVAILABLE ) {
	stopTimeoutTimer();
	this.pio.putInValuePortB( 0x01, 0x01 );
	int value = this.pio.fetchOutValuePortA( true );
	this.pio.putInValuePortB( 0x00, 0x01 );
	writeByte( value );
      }
      else if( status == Z80PIO.Status.READY_FOR_INPUT ) {
	stopTimeoutTimer();
	this.pio.putInValuePortB( 0x00, 0x80 );
	int value = fetchNextResultByte();
	if( value >= 0 ) {
	  setResultByte( value );
	} else {
	  if( (this.cmd == Command.READ_BYTES) && (this.byteCnt > 0) ) {
	    value = readMemByte( this.curAddr );
	    incCurAddr();
	    --this.byteCnt;
	    setResultByte( value );
	  } else {
	    setIdle();
	  }
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public String toString()
  {
    return this.title;
  }


	/* --- private Methoden --- */

  private void writeByte( int value )
  {
    value &= 0xFF;
    switch( this.cmd ) {
      case NONE:
	this.argIdx = 0;
	if( (value >= 0) && (value < commands.length) ) {
	  Command cmd = commands[ value ];
	  if( this.debugLevel > 2 ) {
	    System.out.print( "  " );
	    System.out.println( cmd );
	  }
	  switch( cmd ) {
	    case READ_TIMER:
	      {
		long t = 0;
		long v = this.tStatesCounterValue;
		long n = this.tStatesPerMilli;
		if( (v > 0) && (n > 0) ) {
		  t = (v / n) % 60000L;
		}
		setResultInt16( (int) t );
	      }
	      break;
	    case READ_NEXT_PORT_NUM:
	      {
		int port = this.w5100.reservePort();
		if( port <= 0 ) {
		  port = this.portSeqNum++;
		  if( this.portSeqNum > PORT_NUM_MAX ) {
		    this.portSeqNum = PORT_NUM_MIN;
		  }
		}
		// Net Order, hoeherwertiges Byte zuerst!!!
		this.doubleByteBuf[ 0 ] = (byte) ((port >> 8) & 0xFF);
		this.doubleByteBuf[ 1 ] = (byte) (port & 0xFF);
		setResultBytes( this.doubleByteBuf );
	      }
	      break;
	    case READ_SW_VERSION:
	      setResultInt16( SW_VERSION );
	      break;
	    case READ_HW_VERSION:
	      setResultInt16( HW_VERSION );
	      break;
	    case READ_LINK_STATUS:
	      setResultByte( 1 );		// immer verbunden melden
	      break;
	    case READ_ID:
	      setResultBytes( getIDBytes() );
	      break;
	    case READ_ERROR_CNT:
	      setResultInt16( this.errorCnt );
	      break;
	    default:
	      this.cmd = cmd;
	      restartTimeoutTimer();
	  }
	}
	break;

      case WRITE_BYTES:
	if( this.argIdx == 0 ) {
	  this.args[ this.argIdx++ ] = value;
	  restartTimeoutTimer();
	} else if( this.argIdx == 1 ) {
	  this.byteCnt = ((value << 8) & 0xFF00) | (this.args[ 0 ] & 0x00FF);
	  if( this.byteCnt > 0 ) {
	    this.argIdx++;
	  } else {
	    setIdle();
	  }
	} else {
	  if( this.byteCnt > 0 ) {
	    writeMemByte( this.curAddr, value );
	    incCurAddr();
	    --this.byteCnt;
	  }
	  if( this.byteCnt > 0 ) {
	    restartTimeoutTimer();
	  } else {
	    setIdle();
	  }
	}
	break;

      case READ_BYTES:
	if( this.argIdx == 0 ) {
	  this.args[ this.argIdx++ ] = value;
	} else if( this.argIdx == 1 ) {
	  this.byteCnt = ((value << 8) & 0xFF00) | (this.args[ 0 ] & 0x00FF);
	  if( this.byteCnt > 0 ) {
	    setResultByte( readMemByte( this.curAddr ) );
	    incCurAddr();
	    --this.byteCnt;
	  } else {
	    setIdle();
	  }
	} else {
	  setIdle();
	}
	break;

      case WRITE_ADDR:
	if( this.argIdx == 0 ) {
	  this.args[ this.argIdx++ ] = value;
	  restartTimeoutTimer();
	} else if( this.argIdx == 1 ) {
	  this.curAddr = ((value << 8) & 0xFF00) | (this.args[ 0 ] & 0x00FF);
	  setIdle();
	} else {
	  setIdle();
	}
	break;

      case WRITE_BYTE:
	if( this.argIdx < 2 ) {
	  this.args[ this.argIdx++ ] = value;
	  restartTimeoutTimer();
	} else {
	  int addr = ((this.args[ 1 ] << 8) & 0xFF00)
					| (this.args[ 0 ] & 0x00FF);
	  writeMemByte( addr, value );
	  setIdle();
	}
	break;

      case READ_BYTE:
	if( this.argIdx == 0 ) {
	  this.args[ this.argIdx++ ] = value;
	  restartTimeoutTimer();
	} else {
	  setIdle();		// vor setResultByte(...)!
	  setResultByte(
		readMemByte(
			((value << 8) & 0xFF00)
					| (this.args[ 0 ] & 0x00FF) ) );
	}
	break;

      case WRITE_IP_ADDR:
	if( this.argIdx < 4 ) {
	  this.args[ this.argIdx++ ] = value;
	  restartTimeoutTimer();
	} else {
	  int idx = this.args[ 0 ];
	  if( idx < this.ipAddrMem.length ) {
	    this.ipAddrMem[ idx ][ 0 ] = (byte) this.args[ 1 ];
	    this.ipAddrMem[ idx ][ 1 ] = (byte) this.args[ 2 ];
	    this.ipAddrMem[ idx ][ 2 ] = (byte) this.args[ 3 ];
	    this.ipAddrMem[ idx ][ 3 ] = (byte) value;
	  }
	  setIdle();
	}
	break;

      case READ_IP_ADDR:
	if( this.argIdx == 0 ) {
	  setIdle();		// vor setResultBytes(...)!
	  if( value < this.ipAddrMem.length ) {
	    setResultBytes( this.ipAddrMem[ value ] );
	  } else {
	    setResultBytes( this.emptyIPAddr );
	  }
	} else {
	  setIdle();
	}
	break;

      default:
	setIdle();
    }
  }


  private synchronized int fetchNextResultByte()
  {
    int rv = -1;
    if( this.resultBytes != null ) {
      if( (this.resultPos >= 0)
	  && (this.resultPos < this.resultBytes.length) )
      {
	rv = (int) this.resultBytes[ this.resultPos++ ] & 0xFF;
	if( this.resultPos >= this.resultBytes.length ) {
	  this.resultBytes = null;
	  this.resultPos   = -1;
	}
      } else {
	this.resultBytes = null;
	this.resultPos   = -1;
      }
    }
    return rv;
  }


  private static byte[] getIDBytes()
  {
    if( idBytes == null ) {
      int    n = ID_TEXT.length();
      byte[] a = new byte[ n + 1 ];
      int    i = 0;
      while( i < n ) {
	a[ i ] = (byte) (ID_TEXT.charAt( i ) & 0xFF);
	i++;
      }
      a[ i ]  = (byte) 0;
      idBytes = a;
    }
    return idBytes;
  }


  private void incCurAddr()
  {
    this.curAddr = (this.curAddr + 1) & 0xFFFF;
  }


  private int readMemByte( int addr )
  {
    return addr >= 0x8000 ? this.w5100.readMemByte( addr & 0x7FFF ) : 0;
  }


  private void restartTimeoutTimer()
  {
    this.tStatesToTimeout = this.tStatesPerMilli * 524L;
  }


  private synchronized void setIdle()
  {
    stopTimeoutTimer();
    this.cmd         = Command.NONE;
    this.argIdx      = 0;
    this.byteCnt     = 0;
    this.resultPos   = -1;
    this.resultBytes = null;
    this.pio.putInValuePortB( 0x00, false );
  }


  private void setResultByte( int value )
  {
    this.pio.putInValuePortA( value, false );
    this.pio.putInValuePortB( 0x80, 0x80 );
    this.pio.putInValuePortA( value, true );
    restartTimeoutTimer();
  }


  private synchronized void setResultBytes( byte[] buf )
  {
    this.resultBytes = buf;
    this.resultPos   = 0;

    int value = fetchNextResultByte();
    if( value >= 0 ) {
      setResultByte( value );
    }
  }


  private void setResultInt16( int value )
  {
    this.doubleByteBuf[ 0 ] = (byte) (value & 0xFF);
    this.doubleByteBuf[ 1 ] = (byte) ((value >> 8) & 0xFF);
    setResultBytes( this.doubleByteBuf );
  }


  private void stopTimeoutTimer()
  {
    this.tStatesToTimeout = 0;
  }


  private void writeMemByte( int addr, int value )
  {
    if( addr >= 0x8000 ) {
      this.w5100.writeMemByte( addr & 0x7FFF, value );
    }
  }
}

