/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * TAR-Kopfblock
 */

package jkcemu.filebrowser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;


public class TarEntry
{
  public enum EntryType { DIRECTORY, SYMBOLIC_LINK, REGULAR_FILE, OTHER };

  private String                   entryName;
  private EntryType                entryType;
  private String                   typeText;
  private String                   linkTarget;
  private Set<PosixFilePermission> permissions;
  private long                     entrySize;
  private long                     entryTime;
  private String                   errMsg;


  public String getErrorMsg()
  {
    return this.errMsg;
  }


  public String getLinkTarget()
  {
    return this.linkTarget;
  }


  public String getName()
  {
    return this.entryName;
  }


  public Set<PosixFilePermission> getPosixFilePermissions()
  {
    return this.permissions;
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
    return this.entryType.equals( EntryType.DIRECTORY );
  }


  public boolean isRegularFile()
  {
    return this.entryType.equals( EntryType.REGULAR_FILE );
  }


  public boolean isSymbolicLink()
  {
    return this.entryType.equals( EntryType.SYMBOLIC_LINK );
  }


  public static TarEntry readEntryHeader( InputStream in ) throws IOException
  {
    String  entryName = null;
    byte[]  headerBuf = new byte[ 0x200 ];
    boolean paxHeader = false;
    do {

      // Block lesen
      if( !readFilledBlock( in, headerBuf ) ) {
	return null;
      }

      // Pax-Header pruefen
      int typeByte = (int) headerBuf[ 156 ] & 0xFF;
      if( typeByte == 'g' ) {
	// globaler Pax-Header -> Rest des Pax-Headers einfach ueberlesen
	paxHeader = true;
	in.skip( 0x200L );
      } else if( typeByte == 'x' ) {
	/*
	 * lokaler Pax-Header -> Rest des Pax-Headers lesen und versuchen,
	 * daraus den Namen (Pfad) zu ermitteln
         */
	paxHeader = true;
	if( !readFilledBlock( in, headerBuf ) ) {
	  return null;
	}
	try {
	  int pos = 0;
	  while( pos < headerBuf.length ) {
	    int endPos = pos;
	    while( endPos < headerBuf.length ) {
	      byte b = headerBuf[ endPos ];
	      if( (b == (byte) 0)
		  || (b == (byte) 0x0A) || (b == (byte) 0x0D) )
	      {
		break;
	      }
	      endPos++;
	    }
	    if( endPos > pos ) {
	      String text = new String(
				headerBuf,
				pos,
				endPos - pos,
				"ISO-8859-1" );
	      int idx = text.indexOf( " path=" );
	      if( (idx >= 0) && ((idx + 6) < text.length()) ) {
		entryName = text.substring( idx + 6 );
		break;
	      }
	    }
	    pos = endPos + 1;
	  }
	}
	catch( Exception ex ) {}
      } else {
	paxHeader = false;
      }
    } while( paxHeader );

    // Pruefsumme ermitteln und vergleichen
    int chkSumOrg = parseOctalNumber( headerBuf, 148, 156 );
    int pos       = 148;
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
    String errMsg = null;
    if( entryName == null ) {
      StringBuilder buf = new StringBuilder( 100 );
      pos = 0;
      while( pos < 100 ) {
	int b = (int) headerBuf[ pos++ ] & 0xFF;
	if( b == 0 ) {
	  break;
	}
	if( b >= '\u0020' ) {
	  buf.append( (char) b );
	} else {
	  errMsg = "Ung\u00FCltiges Zeichen im Namen des Eintrags";
	}
      }
      entryName = buf.toString();
    }
    if( entryName.isEmpty() && (errMsg == null) ) {
      errMsg = "Name des Eintrags fehlt";
    }
    String    linkTarget = null;
    String    typeText   = null;
    EntryType entryType  = EntryType.OTHER;
    int       entryMode  = parseOctalNumber( headerBuf, 100, 108 );
    long      entrySize  = parseOctalNumber( headerBuf, 124, 136 );
    long      entryTime  = parseOctalNumber( headerBuf, 136, 148 ) * 1000L;
    int       typeByte   = (int) headerBuf[ 156 ] & 0xFF;
    switch(   typeByte ) {
      case 0:
      case '0':
	entryType = EntryType.REGULAR_FILE;
	typeText  = "Datei";
	break;

      case '1':
      case '2':
	{
	  StringBuilder buf = new StringBuilder( 128 );
	  pos                 = 157;
	  while( pos < 257 ) {
	    char ch = (char) ((int) headerBuf[ pos++ ] & 0xFF);
	    if( ch > 0 ) {
	      buf.append( ch );
	    } else {
	      break;
	    }
	  }
	  if( buf.length() > 0 ) {
	    linkTarget = buf.toString();
	  }
	  if( typeByte == '2' ) {
	    entryType = EntryType.SYMBOLIC_LINK;
	    typeText  = "Sym. Link: " + buf.toString();
	  } else {
	    typeText  = "Link: " + buf.toString();
	  }
	}
	break;

      case '3':
      case '4':
	typeText = "Ger\u00E4tedatei";
	break;

      case '5':
	entryType = EntryType.DIRECTORY;
	typeText  = "Verzeichnis";
	break;

      case '6':
	typeText = "FIFO";
	break;

      default:
	if( typeByte > 0x20 ) {
	  typeText = "Typ: " + Character.toString( (char) typeByte );
	} else {
	  typeText = "Typ: " + Integer.toString( typeByte );
	}
    }
    Set<PosixFilePermission> permissions = new HashSet<>();
    if( (entryMode & 0x001) != 0 ) {
      permissions.add( PosixFilePermission.OTHERS_EXECUTE );
    }
    if( (entryMode & 0x002) != 0 ) {
      permissions.add( PosixFilePermission.OTHERS_WRITE );
    }
    if( (entryMode & 0x004) != 0 ) {
      permissions.add( PosixFilePermission.OTHERS_READ );
    }
    if( (entryMode & 0x008) != 0 ) {
      permissions.add( PosixFilePermission.GROUP_EXECUTE );
    }
    if( (entryMode & 0x010) != 0 ) {
      permissions.add( PosixFilePermission.GROUP_WRITE );
    }
    if( (entryMode & 0x020) != 0 ) {
      permissions.add( PosixFilePermission.GROUP_READ );
    }
    if( (entryMode & 0x040) != 0 ) {
      permissions.add( PosixFilePermission.OWNER_EXECUTE );
    }
    if( (entryMode & 0x080) != 0 ) {
      permissions.add( PosixFilePermission.OWNER_WRITE );
    }
    if( (entryMode & 0x100) != 0 ) {
      permissions.add( PosixFilePermission.OWNER_READ );
    }
    return new TarEntry(
			entryName,
			entryType,
			typeText,
			linkTarget,
			permissions,
			entrySize,
			entryTime,
			errMsg );
  }


	/* --- private Konstruktoren und Methoden --- */

  private TarEntry(
		String                   entryName,
		EntryType                entryType,
		String                   typeText,
		String                   linkTarget,
		Set<PosixFilePermission> permissions,
		long                     entrySize,
		long                     entryTime,
		String                   errMsg )
  {
    this.entryName   = entryName;
    this.entryType   = entryType;
    this.typeText    = typeText;
    this.linkTarget  = linkTarget;
    this.permissions = permissions;
    this.entrySize   = entrySize;
    this.entryTime   = entryTime;
    this.errMsg      = errMsg;
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


  /*
   * Die Methode liest einen Block (512 Bytes).
   * Bloecke, die nur Null-Bytes enthalten, werden dabei ueberlesen.
   */
  private static boolean readFilledBlock(
				InputStream in,
				byte[]      buf ) throws IOException
  {
    int     pos   = 0;
    boolean empty = true;
    while( empty ) {
      pos = 0;
      while( pos < buf.length ) {
	int b = in.read();
	if( b < 0 ) {
	  if( empty ) {
	    return false;
	  }
	  throw new IOException( "Unerwartetes Ende der TAR-Datei" );
	}
	if( b != 0 ) {
	  empty = false;
	}
	buf[ pos++ ] = (byte) b;
      }
    }
    return true;
  }
}
