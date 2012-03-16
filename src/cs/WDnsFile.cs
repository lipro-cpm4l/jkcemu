/*
 * (c) 2012 Jens Mueller
 *
 * Das Programm schreibt die IP-Adressen der eingetragenen DNS-Server
 * in eine Datei, deren Namen als Parameter uebergeben wird.
 * Jede Zeile hat das gleiche Format wie die Nameserver-Eintraege
 * in der auf Unix/Linux-Systemen ueblichen Datei "/etc/resolf.conf".
 */

using System;
using System.IO;
using System.Net;
using System.Net.NetworkInformation;


public class WDnsFile
{
  public static void Main( string[] args )
  {
    try {
      if( args.Length > 0 ) {
	using( StreamWriter sw = new StreamWriter( args[ 0 ] ) ) {
	  foreach( NetworkInterface adapter in
		  NetworkInterface.GetAllNetworkInterfaces() )
	  {
	    IPInterfaceProperties adapterProps = adapter.GetIPProperties();
	    IPAddressCollection dnsServers = adapterProps.DnsAddresses;
	    if( dnsServers.Count > 0 ) {
	      foreach( IPAddress dns in dnsServers ) {
		sw.Write( "nameserver " );
		sw.WriteLine( dns.ToString() );
	      }
	    }
	  }
	}
      }
    }
    catch( Exception ) {}
  }
}

