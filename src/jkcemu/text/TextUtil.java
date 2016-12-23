/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer Bearbeitung von Text
 */

package jkcemu.text;

import java.awt.Component;
import java.lang.*;
import jkcemu.base.BaseDlg;


public class TextUtil
{
  public static String emptyToNull( String text )
  {
    if( text != null ) {
      if( text.trim().isEmpty() ) {
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


  public static void showTextNotFound( Component owner )
  {
    BaseDlg.showInfoDlg(
                owner,
                "Text nicht gefunden!",
                "Text suchen" );
  }


  public static char toISO646DE( char ch )
  {
    if( ch == '\u00A7' ) {	// Paragraf-Zeichen
      ch = '@';
    } else {
      ch = umlautToISO646DE( ch );
    }
    return ch;
  }


  /*
   * Umwandlung eines Textes in Kleinbuchstaben,
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


  public static char toReverseCase( char ch )
  {
    if( Character.isUpperCase( ch ) ) {
      ch = Character.toLowerCase( ch );
    } else if( Character.isLowerCase( ch ) ) {
      ch = Character.toUpperCase( ch );
    }
    return ch;
  }


  public static String toReverseCase( String text )
  {
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	char[] buf = new char[ len ];
	for( int i = 0; i < len; i++ ) {
	  buf[ i ] = toReverseCase( text.charAt( i ) );
	}
	text = new String( buf );
      }
    }
    return text;
  }


  public static char umlautToISO646DE( char ch )
  {
    switch( ch ) {
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


	/* --- privater Konstruktor --- */

  private TextUtil()
  {
    // leer
  }
}
