/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Ermittlung der Netzwerkkonfiguration
 */

package jkcemu.net;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;
import java.util.regex.PatternSyntaxException;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import jkcemu.Main;
import jkcemu.base.EmuUtil;


public class NetConfig
{
  private byte[] hwAddr;
  private byte[] ipAddr;
  private byte[] subnetMask;
  private byte[] dnsServerIpAddr;
  private byte[] manualIpAddr;
  private byte[] manualSubnetMask;
  private byte[] manualGatewayIpAddr;
  private byte[] manualDnsServerIpAddr;


  public byte[] getDnsServerIpAddr()
  {
    return this.dnsServerIpAddr;
  }


  public byte[] getHardwareAddr()
  {
    return this.hwAddr;
  }


  public byte[] getIpAddr()
  {
    return this.ipAddr;
  }


  public byte[] getSubnetMask()
  {
    return this.subnetMask;
  }


  public byte[] getManualDnsServerIpAddr()
  {
    return this.manualDnsServerIpAddr;
  }


  public byte[] getManualGatewayIpAddr()
  {
    return this.manualGatewayIpAddr;
  }


  public byte[] getManualIpAddr()
  {
    return this.manualIpAddr;
  }


  public byte[] getManualSubnetMask()
  {
    return this.manualSubnetMask;
  }


  public static NetConfig readNetConfig()
  {
    NetConfig netConfig  = null;
    int       debugLevel = 0;
    String    text       = System.getProperty( "jkcemu.debug.net" );
    if( text != null ) {
      try {
	debugLevel = Integer.parseInt( text );
      }
      catch( NumberFormatException ex ) {}
    }

    /*
     * Es wird ein Netzwerk-Interface mit IPv4-und Hardware-Adresse gesucht.
     * Ist das nicht moeglich,
     * wird nur nach einem Interface mit IPv4-Adresse gesucht.
     */
    byte[] hwAddr      = null;
    byte[] otherHWAddr = null;
    byte[] ipAddr      = null;
    short  nwPrefixLen = 0;
    try {
      Enumeration<NetworkInterface> nwInterfaces
			= NetworkInterface.getNetworkInterfaces() ;
      while( nwInterfaces.hasMoreElements() && (hwAddr == null) ) {
	NetworkInterface nwIf = nwInterfaces.nextElement();
	if( !nwIf.isVirtual() ) {
	  boolean isUp = nwIf.isUp();
	  if( isUp ) {
	    java.util.List<InterfaceAddress> ifAddrs
					= nwIf.getInterfaceAddresses();
	    if( ifAddrs != null ) {
	      for( InterfaceAddress ifAddr : ifAddrs ) {
		InetAddress inetAddr = ifAddr.getAddress();
		if( inetAddr != null ) {
		  byte[] tmpIpAddr = inetAddr.getAddress();
		  if( tmpIpAddr != null ) {
		    if( tmpIpAddr.length == 4 ) {
		      ipAddr           = tmpIpAddr;
		      nwPrefixLen      = ifAddr.getNetworkPrefixLength();
		      byte[] tmpHWAddr = nwIf.getHardwareAddress();
		      if( tmpHWAddr != null ) {
			if( tmpHWAddr.length == 6 ) {
			  hwAddr = tmpHWAddr;
			  break;
			}
		      }
		    }
		  }
		}
	      }
	    }
	  }
	  if( (hwAddr == null) && (isUp || (otherHWAddr == null)) ) {
	    /*
	     * vorsorglich die MAC-Adresse merken,
	     * falls sich die passende nicht finden laesst.
	     * Die MAC-Adresse einer aktiven Schnittstelle wird dabei
	     * der MAC-Adresse einer passiven vorgezogen.
	     */
	    byte[] tmpHWAddr = nwIf.getHardwareAddress();
	    if( tmpHWAddr != null ) {
	      if( tmpHWAddr.length == 6 ) {
		otherHWAddr = tmpHWAddr;
	      }
	    }
	  }
	}
      }
    }
    catch( IOException ex ) {
      if( debugLevel > 0 ) {
	ex.printStackTrace( System.out );
      }
    }
    if( hwAddr == null ) {
      hwAddr = otherHWAddr;
    }

    /*
     * Netzwerkmaske ermitteln
     *
     * In gemischten IPv4- und IPv6-Umgebungen kann es vorkommen,
     * dass die IPv6-Netzwerkmaske gefunden wurde.
     * Aus diesem Grund wird hier geprueft,
     * ob die Netzwerkmaske einer von IPv4 ueblichen entspricht.
     * Wenn nein, wird einfach 255.255.255.0 gesetzt.
     * Das ist moeglich, da in der Emulation die Netzwerkmaske
     * nicht verwendet wird.
     * Sie muss nur vorhanden sein, damit die Netzwerkprogramme
     * ein konfiguriertes Netzwerk erkennen.
     */
    byte[] subnetMask = new byte[ 4 ];
    if( (nwPrefixLen >= 16) && (nwPrefixLen < 32) ) {
      long m = (0xFFFFFFFF00000000L >>> nwPrefixLen);
      for( int i = 3; i >= 0; --i ) {
	subnetMask[ i ] = (byte) (m & 0xFF);
	m >>= 8;
      }
    } else {
      subnetMask[ 0 ] = (byte) 255;
      subnetMask[ 1 ] = (byte) 255;
      subnetMask[ 2 ] = (byte) 255;
      subnetMask[ 3 ] = (byte) 0;
    }

    /*
     * Ermittlung des Dns-Servers entsprechend der
     * JNDI-Beschreibung von Java 1.5
     *
     * Wenn ein Laptop in verschiedenen Netzwerkumgebungen verwendet wird,
     * kann es vorkommen, dass auch DNS-Server einer gerade nicht
     * verwendeten Netzwerkumgebung gefunden werden.
     * Aus diesem Grund wird aus den gefundenen DNS-Server der ausgewaehlt,
     * der am "naechsten" an der lokalen IP-Adresse liegt.
     */
    byte[] dnsServerIpAddr = null;
    try {
      Hashtable<String,String> env = new Hashtable<>();
      env.put(
	Context.INITIAL_CONTEXT_FACTORY,
	"com.sun.jndi.dns.DnsContextFactory" );
      Object o = (new InitialDirContext( env )).getEnvironment().get(
					"java.naming.provider.url" );
      if( o != null ) {
	String s = o.toString();
	if( s != null ) {
	  String[] dnsServerURLs = s.split( "\\s" );
	  if( dnsServerURLs != null ) {
	    int lastEqualBytes = 0;
	    for( String url : dnsServerURLs ) {
	      if( (url.length() > 6) && url.startsWith( "dns://" ) ) {
		byte[] tmpIpAddr = getIpAddr( url.substring( 6 ) );
		if( tmpIpAddr != null ) {
		  if( ipAddr != null ) {
		    int nEq = 0;
		    int len = Math.min( tmpIpAddr.length, ipAddr.length );
		    for( int i = 0; i < len; i++ ) {
		      if( tmpIpAddr[ i ] != ipAddr[ i ] ) {
			break;
		      }
		      nEq++;
		    }
		    if( (lastEqualBytes == 0) || (nEq > lastEqualBytes) ) {
		      dnsServerIpAddr = tmpIpAddr;
		      lastEqualBytes  = nEq;
		    }
		  } else {
		    dnsServerIpAddr = tmpIpAddr;
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {}

    /*
     * Falls der Dns-Server nicht ermittelt werden konnte,
     * dann auf einem Unix/Linux-System die Datei /etc/resolv.conf auslesen
     */
    if( (dnsServerIpAddr == null) && Main.isUnixLikeOS() ) {
      BufferedReader in = null;
      try {
	in = new BufferedReader( new FileReader( "/etc/resolv.conf" ) );
	String line = in.readLine();
	while( (dnsServerIpAddr == null) && (line != null) ) {
	  line    = line.trim().toLowerCase();
	  int eol = line.indexOf( '#' );
	  if( eol != 0 ) {
	    if( eol > 0 ) {
	      line = line.substring( 0, eol );
	    }
	    if( line.startsWith( "nameserver" ) ) {
	      String[] elems = line.split( "\\s" );
	      if( elems != null ) {
		for( int i = 1; i < elems.length; i++ ) {
		  dnsServerIpAddr = getIpAddr( elems[ i ] );
		  if( dnsServerIpAddr != null ) {
		    break;
		  }
		}
	      }
	    }
	  }
	  line = in.readLine();
	}
      }
      catch( IOException ex ) {}
      catch( PatternSyntaxException ex ) {}
      finally {
	EmuUtil.doClose( in );
      }
    }

    /*
     * Manuelle Einstellungen lesen und uebernehmen
     */
    byte[] manualIpAddr = getIpAddrByProp( "jkcemu.kcnet.ip_address" );
    if( manualIpAddr != null ) {
      ipAddr = manualIpAddr;
    }
    byte[] manualSubnetMask = getIpAddrByProp( "jkcemu.kcnet.subnet_mask" );
    if( manualSubnetMask != null ) {
      subnetMask = manualSubnetMask;
    }
    byte[] manualGatewayIpAddr = getIpAddrByProp( "jkcemu.kcnet.gateway" );
    byte[] manualDnsServerIpAddr = getIpAddrByProp(
					"jkcemu.kcnet.dns_server" );
    if( manualDnsServerIpAddr != null ) {
      dnsServerIpAddr = manualDnsServerIpAddr;
    }
    return new NetConfig(
			hwAddr,
			ipAddr,
			subnetMask,
			dnsServerIpAddr,
			manualIpAddr,
			manualSubnetMask,
			manualGatewayIpAddr,
			manualDnsServerIpAddr );
  }


  public static byte[] getIpAddr( String text )
  {
    byte[] ipAddr = null;
    if( text != null ) {
      text = text.trim();
      if( !text.isEmpty() ) {
	try {
	  String[] elems = text.split( "\\.", 5 );
	  if( elems != null ) {
	    if( elems.length == 4 ) {
	      ipAddr = new byte[ elems.length ];
	      for( int i = 0; i < elems.length; i++ ) {
		int v = Integer.parseInt( elems[ i ] );
		if( (v < 0) || (v > 255) ) {
		  ipAddr = null;
		  break;
		}
		ipAddr[ i ] = (byte) v;
	      }
	    }
	  }
	}
	catch( NumberFormatException ex ) {}
	catch( PatternSyntaxException ex ) {}
      }
    }
    return ipAddr;
  }


	/* --- Konstruktor --- */

  private NetConfig(
		byte[] hwAddr,
		byte[] ipAddr,
		byte[] subnetMask,
		byte[] dnsServerIpAddr,
		byte[] manualIpAddr,
		byte[] manualSubnetMask,
		byte[] manualGatewayIpAddr,
		byte[] manualDnsServerIpAddr )
  {
    this.hwAddr                = hwAddr;
    this.ipAddr                = ipAddr;
    this.subnetMask            = subnetMask;
    this.dnsServerIpAddr       = dnsServerIpAddr;
    this.manualIpAddr          = manualIpAddr;
    this.manualSubnetMask      = manualSubnetMask;
    this.manualGatewayIpAddr   = manualGatewayIpAddr;
    this.manualDnsServerIpAddr = manualDnsServerIpAddr;
  }


	/* --- private Methoden --- */

  private static byte[] getIpAddrByProp( String propName )
  {
    String text = Main.getProperty( propName );
    return text != null ? getIpAddr( text ) : null;
  }
}
