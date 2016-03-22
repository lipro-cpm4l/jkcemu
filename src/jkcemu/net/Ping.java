/*
 * (c) 2011-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Simulation eines Pings
 *
 * Die Java-Methode InetAdress.isReachable(...) testet mit TCP Echo (Port 7),
 * sofern ein ICMP Echo wegen fehlender Berechtigung nicht moeglich ist.
 * TCP Echo wird aber haeufig blockiert, weshalb gerade Server im Internet
 * oft als nicht erreichbar erkannt werden.
 * Aus diesem Grund wird zusaetzlich ein TCP-Verbindungsaufbau
 * zum Port 80 (HTTP) versucht.
 * In manchen Netzen lassen sich Verbindungen zu IP-Adressen aufbauen,
 * die keinem Rechner zugeordnet sind.
 * Ein Fehler tritt dann erst bei der eigentlichen Kommunikation auf.
 * Aus diesem Grund wird nicht nur der Verbindungsaufbau getestet,
 * sondern auch eine HTTP-Anfrage ausgefuehrt.
 */

package jkcemu.net;

import java.io.*;
import java.lang.*;
import java.net.*;
import jkcemu.Main;
import jkcemu.base.EmuUtil;


public class Ping
{
  private static final int    TIMEOUT_MILLIS    = 20000;
  private static final String httpRequestString = "GET / HTTP/1.1\r\n\r\n";

  private static byte[] httpRequestBytes = null;

  private InetAddress inetAddress;
  private byte[]      packageData;
  private Thread      thread1;
  private Thread      thread2;
  private Boolean     reachable;
  private boolean     err;


  public Ping( InetAddress inetAddress, byte[] packageData )
  {
    this.inetAddress = inetAddress;
    this.packageData = packageData;
    this.thread1     = null;
    this.thread2     = null;
    this.reachable   = null;
    this.err         = false;
  }


  public boolean checkError()
  {
    return this.err;
  }


  public InetAddress getInetAddress()
  {
    return this.inetAddress;
  }


  public byte[] getPackageData()
  {
    return this.packageData;
  }


  public Boolean getReachable()
  {
    return this.reachable;
  }


  public synchronized void start()
  {
    if( this.thread2 == null ) {
      this.thread1 = new Thread(
			Main.getThreadGroup(),
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    runThread1();
			  }
			},
			"JKCEMU ping 1" );
      this.thread1.start();
      this.thread2 = new Thread(
			Main.getThreadGroup(),
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    runThread2();
			  }
			},
			"JKCEMU ping 2" );
      this.thread2.start();
    }
  }


	/* --- private Methoden --- */

  public void runThread1()
  {
    Socket       socket = null;
    InputStream  in     = null;
    OutputStream out    = null;
    try {
      if( httpRequestBytes == null ) {
	httpRequestBytes = httpRequestString.getBytes( "US-ASCII" );
      }
      if( httpRequestBytes != null ) {
	socket = new Socket();
	socket.setSoTimeout( TIMEOUT_MILLIS );
	socket.connect(
		new InetSocketAddress( this.inetAddress, 80 ),
		TIMEOUT_MILLIS );

	out = socket.getOutputStream();
	out.write( httpRequestBytes );
	out.flush();

	in = socket.getInputStream();
 	if( in.read() >= 0 ) {
	  this.reachable = Boolean.TRUE;
	}
      }
    }
    catch( Exception ex ) {}
    finally {
      EmuUtil.doClose( in );
      EmuUtil.doClose( out );
      EmuUtil.doClose( socket );
    }
    this.thread1 = null;
  }


  public void runThread2()
  {
    boolean reachable = false;
    boolean err       = false;
    try {
      reachable = this.inetAddress.isReachable( TIMEOUT_MILLIS );
    }
    catch( Exception ex ) {
      err = true;
    }
    Thread thread = this.thread1;
    if( thread != null ) {
      try {
	thread.join();
      }
      catch( InterruptedException ex ) {}
    }
    if( this.reachable == null ) {
      this.err       = err;
      this.reachable = new Boolean( reachable );
    }
  }
}
