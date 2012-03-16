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
import java.util.regex.PatternSyntaxException;
import jkcemu.Main;
import jkcemu.base.EmuUtil;


public class NetUtil
{
  public static byte[] getDNSServerIPAddr()
  {
    byte[] ipAddr      = null;
    String dnsFileName = null;
    if( Main.isUnixLikeOS() ) {
      ipAddr = getDNSServerIPAddr( "/etc/resolv.conf" );
    }
    if( ipAddr == null ) {
      /*
       * Externes C#-Programm aufrufen,
       * welches eine Textdatei mit den DNS-Servern erzeugt
       */
      File configDir = Main.getConfigDir();
      if( configDir != null ) {
	File wdnsFile = new File( configDir, "wdnsfile.exe" );
	if( !wdnsFile.exists() ) {
	  InputStream  in  = null;
	  OutputStream out = null;
	  try {
	    in = Main.class.getResourceAsStream( "/cs/wdnsfile.exe" );
	    if( in != null ) {
	      int b = in.read();
	      if( b >= 0 ) {
		out = new FileOutputStream( wdnsFile );
		while( b >= 0 ) {
		  out.write( b );
		  b = in.read();
		}
		out.close();
		out = null;
	      }
	    }
	  }
	  catch( IOException ex ) {}
	  finally {
	    EmuUtil.doClose( in );
	    EmuUtil.doClose( out );
	  }
	}
	if( wdnsFile.exists() ) {
	  File dnsFile = new File( configDir, "dns.lst" );
	  dnsFile.delete();
	  try {
	    String[] cmd = null;
	    if( Main.isUnixLikeOS() ) {
	      cmd = new String[] {
				"mono",
				wdnsFile.getPath(),
				dnsFile.getPath() };
	    } else {
	      cmd = new String[] { wdnsFile.getPath(), dnsFile.getPath() };
	    }
	    Runtime.getRuntime().exec( cmd ).waitFor();
	    ipAddr = getDNSServerIPAddr( dnsFile.getPath() );
	  }
	  catch( Exception ex ) {}
	  dnsFile.delete();
	}
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


  private static byte[] getDNSServerIPAddr( String dnsFileName )
  {
    byte[] ipAddr = null;
    if( dnsFileName != null ) {
      BufferedReader in = null;
      try {
	in = new BufferedReader( new FileReader( dnsFileName ) );
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
}
