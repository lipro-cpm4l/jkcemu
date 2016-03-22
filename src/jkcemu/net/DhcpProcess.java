/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Simulation eines DHCP-Konfigurationsprozesses auf Server-Seite
 */

package jkcemu.net;

import java.io.*;
import java.lang.*;
import java.net.*;
import jkcemu.Main;
import jkcemu.base.EmuUtil;


public class DhcpProcess
{
  public static final int SERVER_PORT = 67;
  public static final int CLIENT_PORT = 68;

  private static final int MT_DISCOVER    = 1;
  private static final int MT_OFFER       = 2;
  private static final int MT_REQUEST     = 3;
  private static final int MT_ACK         = 5;
  private static final int TTL_DEFAULT    = 64;
  private static final int TIMEOUT_MILLIS = 1000;

  private static final String SERVER_NAME = "jkcemu-dhcp-server";
 
  private byte[]  clientIpAddr;
  private byte[]  serverIpAddr;
  private byte[]  answerBytes;
  private long    begMillis;
  private boolean answerValid;
  private boolean requestReceived;
  private boolean finished;


  public static DhcpProcess checkDiscover(
				W5100          w5100,
				DatagramPacket packet )
  {
    DhcpProcess dhcpProcess = null;
    byte[]      data        = packet.getData();
    if( (data != null) && (packet.getPort() == SERVER_PORT) ) {
      if( data.length > 240 ) {
	if( data[ 0 ] == (byte) 1 ) {		// message op code
	  int messageType    = -1;
	  int parmReqListBeg = -1;
	  int parmReqListLen = -1;
	  if( (data[ 236 ] == (byte) 0x63)	// options magic code
	      && (data[ 237 ] == (byte) 0x82)
	      && (data[ 238 ] == (byte) 0x53)
	      && (data[ 239 ] == (byte) 0x63) )
	  {
	    int idx = 240;
	    while( idx < data.length ) {
	      int option = (int) data[ idx++ ] & 0xFF;
	      if( option == 0xFF ) {
		break;
	      }
	      if( (option != 0) && (idx < data.length) ) {
		int len = (int) data[ idx++ ] & 0xFF;
		switch( option ) {
		  case 0x35:			// message type
		    if( idx < data.length ) {
		      messageType = (int) data[ idx ] & 0xFF;
		    }
		    break;
		  case 0x37:
		    parmReqListBeg = idx;
		    parmReqListLen = len;
		    break;
		}
		idx += len;
	      }
	    }
	  }
	  if( messageType == MT_DISCOVER ) {
	    NetConfig netConfig = w5100.getNetConfig();
	    if( netConfig != null ) {
	      byte[] clientIpAddr = netConfig.getIpAddr();
	      byte[] subnetMask   = netConfig.getSubnetMask();
	      if( (clientIpAddr != null) && (subnetMask != null) ) {
		if( (clientIpAddr.length == 4) && (subnetMask.length == 4) ) {

		  // IP-Adresse des simulierten DHCP-Servers ermitteln
		  byte[] serverIpAddr = new byte[ 4 ];
		  serverIpAddr[ 0 ]   = clientIpAddr[ 0 ];
		  serverIpAddr[ 1 ]   = clientIpAddr[ 1 ];
		  serverIpAddr[ 2 ]   = clientIpAddr[ 2 ];
		  if( clientIpAddr[ 3 ] == (byte) 99 ) {
		    serverIpAddr[ 3 ] = (byte) 111;
		  } else {
		    serverIpAddr[ 3 ] = (byte) 99;
		  }

		  // DHCP_OFFER erzeugen
		  try {
		    ByteArrayOutputStream buf = new ByteArrayOutputStream(
								0x0400 );

		    // message op code: reply
		    buf.write( 2 );

		    // htype, hlen
		    buf.write( data[ 1 ] );
		    buf.write( data[ 2 ] );

		    // hops
		    buf.write( 0 );

		    // xid
		    for( int i = 4; i < 8; i++ ) {
		      buf.write( data[ i ] );
		    }

		    // secs
		    buf.write( 0 );
		    buf.write( 0 );

		    // flags
		    buf.write( data[ 10 ] );
		    buf.write( data[ 11 ] );

		    // ciaddr
		    for( int i = 0; i < 4; i++ ) {
		      buf.write( 0 );
		    }

		    // "your" ip address
		    buf.write( clientIpAddr );

		    // server ip address
		    buf.write( serverIpAddr );

		    // giaddr, chaddr
		    for( int i = 24; i < 44; i++ ) {
		      buf.write( data[ i ] );
		    }

		    // sname
		    int len = SERVER_NAME.length();
		    for( int i = 0; i < len; i++ ) {
		      buf.write( SERVER_NAME.charAt( i ) );
		    }
		    for( int i = len; i < 64; i++ ) {
		      buf.write( 0 );
		    }

		    // file
		    for( int i = 0; i < 128; i++ ) {
		      buf.write( 0 );
		    }

		    // Optionen
		    writeBytesTo(
			buf,
			0x63, 0x82, 0x53, 0x63,		// options magic code
			// options magic code: DHCP_OFFER
			0x35, 0x01, 0x02,
			0x36, 0x04 );			// server identifier
		    buf.write( serverIpAddr );
		    writeBytesTo(
			buf,
			// ip address lease time: 86400 sec = 1 Tag
			0x33, 0x04, 0x00, 0x01, 0x51, 0x80,
			// renewal time value: 43200 sec = 1/2 Tag
			0x3A, 0x04, 0x00, 0x00, 0xA8, 0xC0,
			// rebinding time value: 21 Stunden
			0x3B, 0x04, 0x00, 0x01, 0x27, 0x50,
			0x01, 0x04 );			// subnet mask
		    buf.write( subnetMask );
		    byte[] dnsServerIpAddr = netConfig.getDnsServerIpAddr();
		    if( dnsServerIpAddr != null ) {
		      if( dnsServerIpAddr.length == 4 ) {
			writeBytesTo( buf, 0x06, 0x04 );
			buf.write( dnsServerIpAddr );
		      }
		    }

		    // weitere Optionen entsprechend request list
		    if( (parmReqListBeg > 0) && (parmReqListLen > 0) ) {
		      int idx = parmReqListBeg;
		      while( parmReqListLen > 0 ) {
			if( idx >= data.length ) {
			  break;
			}
			int option = (int) data[ idx++ ] & 0xFF;
			switch( option ) {
			  case 23:		// default ip time-to-live
			  case 37:		// default tcp time-to-live
			    {
			      buf.write( option );
			      buf.write( 1 );
			      buf.write( TTL_DEFAULT );
			    }
			    break;
			}
			--parmReqListLen;
		      }
		    }

		    // Optionen abschliessen
		    buf.write( 0xFF );

		    // DHCP-Prozess einleiten
		    dhcpProcess = new DhcpProcess(
						clientIpAddr,
						serverIpAddr,
						buf.toByteArray() );
		  }
		  catch( IOException ex ) {}
		}
	      }
	    }
	  }
	}
      }
    }
    return dhcpProcess;
  }


  public synchronized boolean fillAnswerToClientInto(
			DatagramPacket packet ) throws IOException
  {
    boolean rv = false;
    if( this.answerValid ) {
      packet.setAddress( InetAddress.getByAddress( this.serverIpAddr ) );
      packet.setPort( SERVER_PORT );
      packet.setData( this.answerBytes );
      this.answerValid = false;
      if( this.requestReceived ) {
	this.finished = true;
      }
      rv = true;
    }
    return rv;
  }


  public boolean hasFinished()
  {
    return this.finished;
  }


  public boolean processRequest( DatagramPacket packet )
  {
    byte[] data = packet.getData();
    if( (data != null) && (packet.getPort() == SERVER_PORT) ) {
      if( data.length > 240 ) {
	if( data[ 0 ] == (byte) 1 ) {		// message op code
	  int messageType = -1;
	  int serverIdIdx = 20;
	  if( (data[ 236 ] == (byte) 0x63)	// options magic code
	      && (data[ 237 ] == (byte) 0x82)
	      && (data[ 238 ] == (byte) 0x53)
	      && (data[ 239 ] == (byte) 0x63) )
	  {
	    int idx = 240;
	    while( idx < data.length ) {
	      int option = (int) data[ idx++ ] & 0xFF;
	      if( option == 0xFF ) {
		break;
	      }
	      if( (option != 0) && (idx < data.length) ) {
		int len = (int) data[ idx++ ] & 0xFF;
		switch( option ) {
		  case 0x35:			// message type
		    if( idx < data.length ) {
		      messageType = (int) data[ idx ] & 0xFF;
		    }
		    break;
		  case 0x36:			// server identifier
		    if( (idx + 3) < data.length ) {
		      if( len == 4 ) {
			/*
			 * wenn siaddr nicht gesetzt ist,
			 * dann Parameter "server identifier" verwenden
			 */
			boolean hasIpAddr = false;
			for( int i = 20; i < 24; i++ ) {
			  if( data[ i ] != (byte) 0 ) {
			    hasIpAddr = true;
			    break;
			  }
			}
			if( !hasIpAddr ) {
			  serverIdIdx = idx;
			}
		      }
		    }
		    break;
		}
		idx += len;
	      }
	    }
	  }
	  if( (messageType == MT_REQUEST)
	      && EmuUtil.equalsRegion(
			data, 4,
			this.answerBytes, 4,
			4 )				// xid
	      && EmuUtil.equalsRegion(
			data, serverIdIdx,
			this.answerBytes, 20,
			4 )				// server id
	      && EmuUtil.equalsRegion(
			data, 28,
			this.answerBytes, 28,
			16 ) )				// chaddr
	  {
	    // flags und ciaddr kopieren
	    for( int i = 10; i < 16; i++ ) {
	      this.answerBytes[ i ] = data[ i ];
	    }

	    // giaddr kopieren
	    for( int i = 24; i < 28; i++ ) {
	      this.answerBytes[ i ] = data[ i ];
	    }

	    // message type
	    this.answerBytes[ 242 ] = MT_ACK;

	    // request als empfangen vermerken
	    this.requestReceived = true;
	    this.answerValid     = true;
	  }
	}
      }
    }
    return this.requestReceived;
  }


  public boolean isTimeout()
  {
    return (this.begMillis + TIMEOUT_MILLIS) < System.currentTimeMillis();
  }


	/* --- Konstruktor --- */

  private DhcpProcess(
		byte[] clientIpAddr,
		byte[] serverIpAddr,
		byte[] answerBytes )
  {
    this.clientIpAddr    = clientIpAddr;
    this.serverIpAddr    = serverIpAddr;
    this.answerBytes     = answerBytes;
    this.answerValid     = true;
    this.requestReceived = false;
    this.finished        = false;
    this.begMillis       = System.currentTimeMillis();
  }


	/* --- private Methoden --- */

  private static void writeBytesTo(
				OutputStream out,
				int...       values ) throws IOException
  {
    for( int v : values ) {
      out.write( v );
    }
  }
}
