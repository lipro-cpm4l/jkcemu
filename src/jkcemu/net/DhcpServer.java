/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Wrapper-Klasse fuer DatagramSocket und MulticastSocket
 *
 * Der Sinn der Wrapper-Klasse besteht darin,
 * DHCP-Pakete abfangen und simulieren zu koennen.
 */

package jkcemu.net;

import java.io.IOException;
import java.lang.*;
import java.net.DatagramPacket;


public class DhcpServer
{
  private W5100       w5100;
  private DhcpProcess dhcpProcess;


  public DhcpServer( W5100 w5100 )
  {
    this.w5100 = w5100;
  }


  public synchronized boolean receive( DatagramPacket packet )
							throws IOException
  {
    boolean rv = false;
    if( this.dhcpProcess != null ) {
      if( this.dhcpProcess.isTimeout() ) {
	this.dhcpProcess = null;
      }
    }
    if( this.dhcpProcess != null ) {
      if( this.dhcpProcess.fillAnswerToClientInto( packet ) ) {
	if( this.dhcpProcess.hasFinished() ) {
	  this.dhcpProcess = null;
	}
	rv = true;
      }
    }
    return rv;
  }


  public synchronized void send( DatagramPacket packet ) throws IOException
  {
    if( this.dhcpProcess != null ) {
      if( this.dhcpProcess.isTimeout() ) {
	this.dhcpProcess = null;
      }
    }
    DhcpProcess dhcpProcess = DhcpProcess.checkDiscover( this.w5100, packet );
    if( dhcpProcess != null ) {
      this.dhcpProcess = dhcpProcess;
    } else {
      if( this.dhcpProcess != null ) {
	if( !this.dhcpProcess.processRequest( packet ) ) {
	  this.dhcpProcess = null;
	}
      }
    }
  }
}
