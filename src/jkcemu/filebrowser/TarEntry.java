/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * TAR-Kopfblock
 */

package jkcemu.filebrowser;

import java.io.*;
import java.lang.*;


public class TarEntry
{
  private String  entryName;
  private String  typeText;
  private boolean typeFile;
  private boolean typeDir;
  private int     entryMode;
  private long    entrySize;
  private long    entryTime;
  private String  errMsg;


  public String getErrorMsg()
  {
    return this.errMsg;
  }


  public int getMode()
  {
    return this.entryMode;
  }


  public String getName()
  {
    return this.entryName;
  }


  public long getSize()
  {
    return this.entrySize;
  }


  public long getTime()
  {
    return this.entryTime;
  }


  public String getTypeText()
  {
    return this.typeText;
  }


  public boolean isDirectory()
  {
    return this.typeDir;
  }


  public boolean isFile()
  {
    return this.typeFile;
  }


  public static TarEntry readEntryHeader( InputStream in ) throws IOException
  {
    /*
     * Kopfblock lesen
     * Dabei Bloecke ueberlesen, die nur aus Null-Bytes bestehen
     */
    byte[]  headerBuf = new byte[ 512 ];
    int     pos       = 0;
    boolean empty     = true;
    while( empty ) {
      pos = 0;
      while( pos < headerBuf.length ) {
	int b = in.read();
	if( b == -1 ) {
	  if( pos > 0 ) {
	    throw new IOException( "Unerwartetes Ende der TAR-Datei" );
	  } else {
	    return null;
	  }
	}
	if( b != 0 ) {
	  empty = false;
	}
	headerBuf[ pos++ ] = (byte) b;
      }
    }

    // Pruefsumme ermitteln und vergleichen
    int chkSumOrg = parseOctalNumber( headerBuf, 148, 156 );
    pos = 148;
    while( pos < 156 ) {
      headerBuf[ pos++ ] = (byte) 0x20;
    }
    int chkSumSigned   = 0;
    int chkSumUnsigned = 0;
    pos = 0;
    while( pos < headerBuf.length ) {
      chkSumSigned   += (int) headerBuf[ pos ];
      chkSumUnsigned += ((int) headerBuf[ pos ]) & 0xFF;
      pos++;
    }
    if( (chkSumSigned != chkSumOrg) && (chkSumUnsigned != chkSumOrg) ) {
      throw new IOException(
		"Kein TAR-Kopfblock (Pr\u00FCfsumme differiert)" );
    }

    // Werte lesen
    String  errMsg    = null;
    String  entryName = null;
    String  linkName  = null;
    String  typeText  = null;
    boolean typeDir   = false;
    boolean typeFile  = false;

    StringBuilder buf = new StringBuilder( 100 );
    pos = 0;
    while( pos < 100 ) {
      char ch = (char) ((int) headerBuf[ pos++ ] & 0xFF);
      if( ch == 0 ) {
	break;
      }
      if( ch >= '\u0020' ) {
	buf.append( ch );
      } else {
	errMsg = "Ung\u00FCltiges Zeichen im Namen des Eintrags";
      }
    }
    if( buf.length() > 0 ) {
      entryName = buf.toString();
    } else {
      if( errMsg == null )
	errMsg = "Name des Eintrags fehlt";
    }
    int  entryMode = parseOctalNumber( headerBuf, 100, 108 );
    long entrySize = parseOctalNumber( headerBuf, 124, 136 );
    long entryTime = parseOctalNumber( headerBuf, 136, 148 ) * 1000L;
    int  entryType = (int) headerBuf[ 156 ] & 0xFF;
    switch( entryType ) {
      case 0:
      case '0':
      case '7':
	typeText = "Datei";
	typeFile = true;
	break;

      case '1':
      case '2':
	buf = new StringBuilder( 128 );
	if( entryType == '2' ) {
	  buf.append( "Sym. Link" );
	} else {
	  buf.append( "Link" );
	}
	int len = buf.length();
	buf.append( ": " );
	boolean hasLinkName = false;
	pos                 = 157;
	while( pos < 257 ) {
	  char ch = (char) ((int) headerBuf[ pos++ ] & 0xFF);
	  if( ch > 0 ) {
	    buf.append( ch );
	    hasLinkName = true;
	  } else {
	    break;
	  }
	}
	if( !hasLinkName ) {
	  buf.setLength( len );
	}
	typeText = buf.toString();
	break;

      case '3':
      case '4':
	typeText = "Ger\u00E4tedatei";
	break;

      case '5':
	typeText = "Verzeichnis";
	typeDir  = true;
	break;

      case '6':
	typeText = "FIFO";
	break;

      default:
	if( entryType > 0x20 ) {
	  typeText = "Typ: " + Character.toString( (char) entryType );
	} else {
	  typeText = "Typ: " + Integer.toString( entryType );
	}
    }
    return new TarEntry(
			entryName,
			typeText,
			typeFile,
			typeDir,
			entryMode,
			entrySize,
			entryTime,
			errMsg );
  }


	/* --- private Konstruktoren und Methoden --- */

  private TarEntry(
		String  entryName,
		String  typeText,
		boolean typeFile,
		boolean typeDir,
		int     entryMode,
		long    entrySize,
		long    entryTime,
		String  errMsg )
  {
    this.entryName = entryName;
    this.typeText  = typeText;
    this.typeFile  = typeFile;
    this.typeDir   = typeDir;
    this.entryMode = entryMode;
    this.entrySize = entrySize;
    this.entryTime = entryTime;
    this.errMsg    = errMsg;
  }


  private static int parseOctalNumber( byte[] buf, int pos, int endPos )
  {
    while( (pos < endPos) && (buf[ pos ] == '\u0020') ) {
      pos++;
    }
    int  value = 0;
    byte ch    = buf[ pos ];
    while( (pos < endPos) && (ch >= '0') && (ch <= '7') ) {
      value = (value << 3) | (ch & 0x07);
      ch    = buf[ pos++ ];
    }
    return value;
  }
}

