/*
 * (c) 2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Mapping zwischen langen und kurzen Dateinamen
 * in einem Virtuellen FAT-Dateisystem
 */

package jkcemu.usb;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class VFATFileNameMapper
{
  private Map<String,Integer> base2Num;
  private Map<String,String>  long2Short;
  private Map<String,String>  short2Long;


  public VFATFileNameMapper()
  {
    this.base2Num   = new HashMap<>();
    this.long2Short = new HashMap<>();
    this.short2Long = new HashMap<>();
  }


  public String getLongByShort( String shortName )
  {
    String longName = this.short2Long.get( shortName );
    return longName != null ? longName : shortName;
  }


  public boolean hasLongName( String shortName )
  {
    return this.short2Long.containsKey( shortName.toUpperCase() );
  }


  public static boolean isValidShortFileName( String fName )
  {
    boolean rv = false;
    if( fName != null ) {
      boolean dot   = false;
      int     nMain = 0;
      int     nExt  = 0;
      int     len   = fName.length();
      for( int i = 0; i < len; i++ ) {
	char ch = fName.charAt( i );
	if( ch == '.' ) {
	  if( dot ) {
	    nMain = 0;
	    break;
	  }
	  dot = true;
	} else if( isValidShortFileNameChar( ch ) ) {
	  if( dot ) {
	    nExt++;
	  } else {
	    nMain++;
	  }
	} else {
	  nMain = 0;
	  break;
	}
      }
      if( (nMain >= 1) && (nMain <= 8) ) {
	if( dot ) {
	  rv = ((nExt >= 1) && (nExt <= 3));
	} else {
	  rv = (nExt == 0);
	}
      }
    }
    return rv;
  }


  public static boolean isValidShortFileNameChar( char ch )
  {
    return ((ch >= 'A') && (ch <= 'Z'))
	      || ((ch >= 'a') && (ch <= 'z'))
	      || ((ch >= '0') && (ch <= '9'))
	      || (("$%\'-_@~\u0060!(){}^#&").indexOf( ch ) >= 0);
  }


  public String toShortName( String longName, File dirFile )
  {
    String shortName = this.long2Short.get( longName );
    if( shortName == null ) {
      int len = longName.length();
      if( len > 0 ) {
	if( isValidShortFileName( longName ) ) {
	  shortName = longName.toUpperCase();
	} else {

	  // Erweiterung fuer kurzen Dateinamen erzeugen
	  char[] extBuf = null;
	  int    tmpIdx = longName.lastIndexOf( '.' );
	  if( tmpIdx >= 0 ) {
	    tmpIdx++;
	    if( tmpIdx < len ) {
	      extBuf = new char[ Math.min( len - tmpIdx, 3 ) ];
	      for( int i = 0; i < extBuf.length; i++ ) {
		char ch = longName.charAt( tmpIdx++ );
		if( isValidShortFileNameChar( ch ) ) {
		  extBuf[ i ] = Character.toUpperCase( ch );
		} else {
		  extBuf[ i ] = '_';
		}
	      }
	    }
	  }

	  // max. 6-stelligen Basisname erzeugen
	  StringBuilder baseBuf = new StringBuilder( 8 );
	  for( int i = 0; (i < 6) && (i < len); i++ ) {
	    char ch = longName.charAt( i );
	    if( ch == '.' ) {
	      break;
	    }
	    if( isValidShortFileNameChar( ch ) ) {
	      baseBuf.append( Character.toUpperCase( ch ) );
	    } else {
	      baseBuf.append( '_' );
	    }
	  }

	  // kurzen Dateinamen erzeugen und testen
	  StringBuilder fullBuf = new StringBuilder( 12 );
	  while( baseBuf.length() > 0 ) {
	    String  baseName = baseBuf.toString();
	    Integer lastNum  = this.base2Num.get( baseName );
	    int     baseNum  = 1;
	    if( lastNum != null ) {
	      baseNum = lastNum.intValue() + 1;
	    }
	    if( baseNum > 9 ) {
	      baseBuf.setLength( baseBuf.length() - 1 );
	      continue;
	    }
	    this.base2Num.put( baseName, baseNum );
	    fullBuf.setLength( 0 );
	    fullBuf.append( baseName );
	    fullBuf.append( '~' );
	    fullBuf.append( baseNum );
	    if( extBuf != null ) {
	      fullBuf.append( '.' );
	      fullBuf.append( extBuf );
	    }
	    String tmpName = fullBuf.toString();
	    if( (new File( dirFile, tmpName )).exists() ) {
	      continue;
	    }
	    shortName = tmpName;
	    this.long2Short.put( longName, shortName );
	    this.short2Long.put( shortName, longName );
	    break;
	  }
	}
      }
    }
    return shortName;
  }
}
