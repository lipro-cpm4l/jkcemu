/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Wrapper-Klasse fuer DatagramSocket und MulticastSocket
 *
 * Der Sinn der Wrapper-Klasse besteht darin,
 * DHCP-Pakete abfangen und simulieren zu koennen.
 */

package jkcemu.net;

import java.io.Closeable;
import java.io.IOException;
import java.lang.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;


public class EmuDatagramSocket implements AutoCloseable, Closeable
{
  private DatagramSocket datagramSocket;
  private int            port;


  public static EmuDatagramSocket createDatagramSocket()
						throws SocketException
  {
    return new EmuDatagramSocket( new DatagramSocket(), 0 );
  }


  public static EmuDatagramSocket createDatagramSocket( int port )
						throws SocketException
  {
    EmuDatagramSocket ds = null;
    if( port == DhcpProcess.CLIENT_PORT ) {
      ds = new EmuDatagramSocket( null, port );
    } else {
      ds = new EmuDatagramSocket( new DatagramSocket( port ), port );
    }
    return ds;
  }


  public static EmuDatagramSocket createMulticastSocket()
						throws IOException
  {
    return new EmuDatagramSocket( new MulticastSocket(), 0 );
  }


  public static EmuDatagramSocket createMulticastSocket( int port )
						throws IOException
  {
    EmuDatagramSocket ds = null;
    if( port == DhcpProcess.CLIENT_PORT ) {
      ds = new EmuDatagramSocket( null, port );
    } else {
      ds = new EmuDatagramSocket( new MulticastSocket( port ), port );
    }
    return ds;
  }


  public int getLocalPort()
  {
    return this.datagramSocket != null ?
			this.datagramSocket.getLocalPort()
			: this.port;
  }


  public boolean isMulticastSocket()
  {
    boolean rv = false;
    if( this.datagramSocket != null ) {
      if( this.datagramSocket instanceof MulticastSocket ) {
	rv = true;
      }
    }
    return rv;
  }


  public void joinGroup( InetAddress multicastAddr ) throws IOException
  {
    if( this.datagramSocket != null ) {
      if( this.datagramSocket instanceof MulticastSocket ) {
	((MulticastSocket) this.datagramSocket).joinGroup( multicastAddr );
      }
    }
  }


  public boolean receive(
			W5100          w5100,
			DatagramPacket packet ) throws IOException
  {
    boolean rv = false;
    if( this.datagramSocket != null ) {
      this.datagramSocket.receive( packet );
      rv = true;
    } else {
      rv = w5100.getDhcpServer().receive( packet );
    }
    return rv;
  }


  public void send( W5100 w5100, DatagramPacket packet ) throws IOException
  {
    if( this.datagramSocket != null ) {
      this.datagramSocket.send( packet );
    } else {
      w5100.getDhcpServer().send( packet );
    }
  }


  public void setTimeToLive( int ttl ) throws IOException
  {
    if( this.datagramSocket != null ) {
      if( this.datagramSocket instanceof MulticastSocket ) {
	((MulticastSocket) this.datagramSocket).setTimeToLive( ttl );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void close()
  {
    if( this.datagramSocket != null )
      this.datagramSocket.close();
  }


	/* --- Konstruktor --- */

  private EmuDatagramSocket( DatagramSocket datagramSocket, int port )
  {
    this.datagramSocket = datagramSocket;
    this.port           = port;
  }
}
