/*
 * (c) 2011-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer Netzwerk
 */

package jkcemu.net;

import java.io.*;
import java.lang.*;
import java.util.Hashtable;
import java.util.regex.PatternSyntaxException;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import jkcemu.Main;
import jkcemu.base.EmuUtil;


public class NetUtil
{
  public static byte[] getDNSServerIPAddr()
  {
    byte[] ipAddr = null;

    /*
     * Ermittlung des DNS-Servers entsprechend der
     * JNDI-Beschreibung von Java 1.5
     */
    try {
      Hashtable<String,String> env = new Hashtable<String,String>();
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
            for( String url : dnsServerURLs ) {
              if( (url.length() > 6) && url.startsWith( "dns://" ) ) {
		ipAddr = getIPAddr( url.substring( 6 ) );
		if( ipAddr != null ) {
		  break;
		}
              }
            }
          }
        }
      }
    }
    catch( Exception ex ) {}

    /*
     * Falls der DNS-Server nicht ermittelt werden konnte,
     * dann auf einem Unix/Linux-System die Datei /etc/resolv.conf auslesen
     */
    if( (ipAddr == null) && Main.isUnixLikeOS() ) {
      BufferedReader in = null;
      try {
	in = new BufferedReader( new FileReader( "/etc/resolv.conf" ) );
	String line = in.readLine();
	while( (ipAddr == null) && (line != null) ) {
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
		  ipAddr = getIPAddr( elems[ i ] );
		  if( ipAddr != null ) {
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
    return ipAddr;
  }


  public static byte[] getIPAddr( String text )
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


	/* --- private Konstruktoren und Methoden --- */

  private NetUtil()
  {
    // leer
  }
}

