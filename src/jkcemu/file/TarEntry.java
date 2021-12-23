/*
 * (c) 2008-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * TAR-Kopfblock
 */

package jkcemu.file;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jkcemu.base.EmuUtil;


public class TarEntry
{
  public enum EntryType { DIRECTORY, SYMBOLIC_LINK, REGULAR_FILE, OTHER };

  private String                   entryName;
  private EntryType                entryType;
  private String                   typeText;
  private String                   linkTarget;
  private Set<PosixFilePermission> permissions;
  private long                     entrySize;
  private Long                     entryTimeMillis;
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


  public long getTimeMillis()
  {
    return this.entryTimeMillis;
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
    String  entryName       = null;
    Long    entryTimeMillis = null;
    Long    entrySize       = null;
    String  linkTarget      = null;
    byte[]  headerBuf       = new byte[ 0x200 ];
    boolean paxHeader       = false;
    do {

      // Block lesen
      if( !readFully( in, headerBuf, true ) ) {
	return null;
      }

      // Pax-Header pruefen
      int typeByte = (int) headerBuf[ 156 ] & 0xFF;
      if( typeByte == 'g' ) {
	// globaler Pax-Header -> Rest des Pax-Headers einfach ueberlesen
	paxHeader = true;
	in.skip( 0x200 );
      } else if( typeByte == 'x' ) {
	/*
	 * lokaler Pax-Header -> Rest des Pax-Headers lesen und versuchen,
	 * daraus den Namen (Pfad) und den Modifikationszeitpunk zu ermitteln
         */
	paxHeader = true;
	if( !readFully( in, headerBuf, false ) ) {
	  return null;
	}
	int pos = 0;
	while( pos < headerBuf.length ) {
	  int endPos = pos;
	  while( endPos < headerBuf.length ) {
	    byte b = headerBuf[ endPos ];
	    if( (b == (byte) 0) || (b == (byte) 0x0A) ) {
	      break;
	    }
	    endPos++;
	  }
	  if( endPos <= pos ) {
	    break;
	  }
	  try {
	    String text = new String(
				headerBuf,
				pos,
				endPos - pos,
				"US-ASCII" );
	    int idx = text.indexOf( " linkpath=" );
	    if( (idx >= 0) && ((idx + 10) < text.length()) ) {
	      linkTarget = text.substring( idx + 10 );
	      break;
	    }
	    idx = text.indexOf( " path=" );
	    if( (idx >= 0) && ((idx + 6) < text.length()) ) {
	      entryName = text.substring( idx + 6 );
	      break;
	    }
	    idx = text.indexOf( " mtime=" );
	    if( (idx >= 0) && ((idx + 7) < text.length()) ) {
	      entryTimeMillis = Long.valueOf( Math.round(
		      Double.parseDouble( text.substring( idx + 7 ) )
						      * 1000.0) );
	      break;
	    }
	    idx = text.indexOf( " size=" );
	    if( (idx >= 0) && ((idx + 6) < text.length()) ) {
	      entrySize = Long.valueOf( text.substring( idx + 6 ) );
	      break;
	    }
	  }
	  catch( NumberFormatException ex ) {}
	  catch( UnsupportedEncodingException ex ) {}
	  pos = endPos + 1;
	}
      } else {
	paxHeader = false;
      }
    } while( paxHeader );

    // Pruefsumme ermitteln und vergleichen
    String errMsg = null;
    Long   chkSum = EmuUtil.parseOctalNumber( headerBuf, 148, 156 );
    if( chkSum != null ) {
      Arrays.fill( headerBuf, 148, 156, (byte) 0x20 );
      int chkSumOrg      = chkSum.intValue();
      int chkSumSigned   = 0;
      int chkSumUnsigned = 0;
      int pos            = 0;
      while( pos < headerBuf.length ) {
	chkSumSigned   += (int) headerBuf[ pos ];
	chkSumUnsigned += ((int) headerBuf[ pos ]) & 0xFF;
	pos++;
      }
      if( (chkSumSigned != chkSumOrg) && (chkSumUnsigned != chkSumOrg) ) {
	errMsg = addLine(
			errMsg,
			"Pr\u00FCfsumme im TAR-Kopfblock fehlerhaft" );
      }
    }

    // Werte lesen
    if( entryName == null ) {
      StringBuilder buf = new StringBuilder( 100 );
      int           pos = 0;
      while( pos < 100 ) {
	int b = (int) headerBuf[ pos++ ] & 0xFF;
	if( b == 0 ) {
	  break;
	}
	if( b >= '\u0020' ) {
	  buf.append( (char) b );
	} else {
	  errMsg = addLine(
			errMsg,
			"Ung\u00FCltiges Zeichen im Namen des Eintrags" );
	}
      }
      entryName = buf.toString();
    }
    if( entryName.isEmpty() ) {
      errMsg = addLine( errMsg, "Name des Eintrags fehlt" );
    }
    String    typeText  = null;
    EntryType entryType = EntryType.OTHER;
    int       entryMode = 0;
    Long      tmpMode   = EmuUtil.parseOctalNumber( headerBuf, 100, 108 );
    if( tmpMode != null ) {
      entryMode = tmpMode.intValue();
    }
    if( entrySize == null ) {
      entrySize = EmuUtil.parseOctalNumber( headerBuf, 124, 136 );
    }
    if( entryTimeMillis == null ) {
      Long v = EmuUtil.parseOctalNumber( headerBuf, 136, 148 );
      if( v != null ) {
	entryTimeMillis = Long.valueOf( v.longValue() * 1000L );
      }
    }
    int typeByte = (int) headerBuf[ 156 ] & 0xFF;
    switch( typeByte ) {
      case 0:
      case '0':
	entryType = EntryType.REGULAR_FILE;
	typeText  = "Datei";
	break;

      case '1':
      case '2':
	{
	  StringBuilder buf = new StringBuilder( 128 );
	  int           pos = 157;
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
			entrySize != null ? entrySize.longValue() : 0,
			entryTimeMillis,
			errMsg );
  }


	/* --- private Methoden --- */

  private static String addLine( String baseText, String line )
  {
    return baseText != null ?
			baseText + "\n" + line
			: line;
  }


  /*
   * Die Methode liest einen Block.
   * Rueckgabe:
   *   true:  Block vollstaendig gelesen
   *   false: Dateiende (kein Byte gelesen)
   */
  private static boolean readFully(
			InputStream in,
			byte[]      buf,
			boolean     skipEmptyBlocks ) throws IOException
  {
    int     pos   = 0;
    boolean empty = true;
    do {
      pos = 0;
      while( pos < buf.length ) {
	int b = in.read();
	if( b < 0 ) {
	  if( empty && skipEmptyBlocks ) {
	    return false;
	  }
	  throw new EOFException( "Unerwartetes Ende der TAR-Datei" );
	}
	if( b != 0 ) {
	  empty = false;
	}
	buf[ pos++ ] = (byte) b;
      }
    } while( empty && skipEmptyBlocks );
    return true;
  }


	/* --- Konstruktor --- */

  private TarEntry(
		String                   entryName,
		EntryType                entryType,
		String                   typeText,
		String                   linkTarget,
		Set<PosixFilePermission> permissions,
		long                     entrySize,
		Long                     entryTimeMillis,
		String                   errMsg )
  {
    this.entryName       = entryName;
    this.entryType       = entryType;
    this.typeText        = typeText;
    this.linkTarget      = linkTarget;
    this.permissions     = permissions;
    this.entrySize       = entrySize;
    this.entryTimeMillis = entryTimeMillis;
    this.errMsg          = errMsg;
  }
}
