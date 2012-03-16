/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer Bearbeitung von Text
 */

package jkcemu.text;

import java.lang.*;


public class TextUtil
{
  public static String emptyToNull( String text )
  {
    if( text != null ) {
      boolean empty = true;
      int     len   = text.length();
      for( int i = 0; i < len; i++ ) {
	if( !Character.isWhitespace( text.charAt( i ) ) ) {
	  empty = false;
	  break;
	}
      }
      if( empty ) {
	text = null;
      }
    }
    return text;
  }


  public static boolean endsWith( String text, String[] extensions )
  {
    boolean rv = false;
    if( (text != null) && (extensions != null) ) {
      for( int i = 0; i < extensions.length; i++ ) {
	if( text.endsWith( extensions[ i ] ) ) {
	  rv = true;
	  break;
	}
      }
    }
    return rv;
  }


  public static boolean equals( String s1, String s2 )
  {
    if( s1 == null ) {
      s1 = "";
    }
    return s1.equals( s2 != null ? s2 : "" );
  }


  public static boolean equalsIgnoreCase( String s1, String s2 )
  {
    if( s1 == null ) {
      s1 = "";
    }
    return s1.equalsIgnoreCase( s2 != null ? s2 : "" );
  }


  public static char toISO646DE( char ch )
  {
    switch( ch ) {
      case '\u00A7':  // Paragraf-Zeichen
	ch = '@';
	break;

      case '\u00C4':  // Ae
	ch = '[';
	break;

      case '\u00D6':  // Oe
	ch = '\\';
	break;

      case '\u00DC':  // Ue
	ch = ']';
	break;

      case '\u00E4':  // ae
	ch = '{';
	break;

      case '\u00F6':  // oe
	ch = '|';
	break;

      case '\u00FC':  // ue
	ch = '}';
	break;

      case '\u00DF':  // sz
	ch = '~';
	break;
    }
    return ch;
  }


  /*
   * Umwandlung eines Textes in Grossbuchstaben,
   * wobei sichergestellt ist,
   * dass sich die Anzahl der Zeichen nicht aendert.
   */
  public static String toLowerCase( String text )
  {
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	char[] buf = new char[ len ];
	for( int i = 0; i < len; i++ ) {
	  buf[ i ] = Character.toLowerCase( text.charAt( i ) );
	}
	text = new String( buf );
      }
    }
    return text;
  }


  public static String toReverseCase( String text )
  {
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	char[] buf = new char[ len ];
	for( int i = 0; i < len; i++ ) {
	  char ch = text.charAt( i );
	  if( Character.isUpperCase( ch ) ) {
	    buf[ i ] = Character.toLowerCase( ch );
	  } else if( Character.isLowerCase( ch ) ) {
	    buf[ i ] = Character.toUpperCase( ch );
	  } else {
	    buf[ i ] = ch;
	  }
	}
	text = new String( buf );
      }
    }
    return text;
  }


  /*
   * Umwandlung eines Textes in Grossbuchstaben,
   * wobei sichergestellt ist,
   * dass sich die Anzahl der Zeichen nicht aendert.
   */
  public static String toUpperCase( String text )
  {
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	char[] buf = new char[ len ];
	for( int i = 0; i < len; i++ ) {
	  buf[ i ] = Character.toUpperCase( text.charAt( i ) );
	}
	text = new String( buf );
      }
    }
    return text;
  }


  public static String wordStarToPlainText( byte[] fileBytes )
  {
    String text = null;
    if( fileBytes != null ) {
      if( fileBytes.length > 0 ) {
	StringBuilder buf = new StringBuilder( fileBytes.length );
	int           pos = 0;
	boolean       bol = true;	// Zeilenanfang
	boolean       ign = false;	// Zeile ignorieren
	while( pos < fileBytes.length ) {
	  int b = 0;
	  if( fileBytes != null ) {
	    b = (int) fileBytes[ pos ] & 0xFF;
	  }
	  else if( text != null ) {
	    b = text.charAt( pos );
	  }
	  if( bol && (b == '.') ) {
	    ign = true;
	  }
	  b &= 0x7F;
	  if( (b == '\n') || (b == '\t') || (b >= 0x20) ) {
	    if( !ign ) {
	      buf.append( (char) b );
	    }
	    if( b == '\n' ) {
	      ign = false;
	      bol = true;
	    } else {
	      bol = false;
	    }
	  }
	  pos++;
	}
	text = buf.toString();
      } else {
	text = "";
      }
    }
    return text;
  }


	/* --- privater Konstruktor --- */

  private TextUtil()
  {
    // leer
  }
}
