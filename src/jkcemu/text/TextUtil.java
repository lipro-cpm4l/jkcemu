/*
 * (c) 2012-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer Bearbeitung von Text
 */

package jkcemu.text;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.swing.text.JTextComponent;


public class TextUtil
{
  private static Method modelToViewMethod = null;
  private static Method viewToModelMethod = null;


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


  public static boolean endsWithIgnoreCase( String text, String suffix )
  {
    boolean rv       = false;
    int     startPos = text.length() - suffix.length();
    if( startPos >= 0 ) {
      if( startPos > 0 ) {
	text = text.substring( startPos );
      }
      rv = text.equalsIgnoreCase( suffix );
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



  public static String getFirstLine( String text )
  {
    if( text != null ) {
      int eol = text.indexOf( '\n' );
      if( eol >= 0 ) {
	text = text.substring( 0, eol );
      }
    }
    return text;
  }

  public static boolean isTextSelected( JTextComponent c )
  {
    int selBeg = c.getSelectionStart();
    return (selBeg >= 0) && (selBeg < c.getSelectionEnd());
  }


  /*
   * Ab Java 9 ersetzt die Methode JTextComponent.modelToView2D(...)
   * die Methode JTextComponent.modelToView2(...).
   * Aud diesem Grund wird die Funktionalitaet in einer Wrapper-Methode
   * gekapselt.
   */
  public static Rectangle2D modelToView( JTextComponent textComp, int pos )
  {
    Rectangle2D rv = null;
    if( modelToViewMethod == null ) {
      try {
	modelToViewMethod = textComp.getClass().getMethod(
							"modelToView2D",
							int.class );
	if( !isPublic( modelToViewMethod ) ) {
	  modelToViewMethod = null;
	}
      }
      catch( NoSuchMethodException ex ) {}
    }
    if( modelToViewMethod == null ) {
      try {
	modelToViewMethod = textComp.getClass().getMethod(
							"modelToView",
							int.class );
	if( !isPublic( modelToViewMethod ) ) {
	  modelToViewMethod = null;
	}
      }
      catch( NoSuchMethodException ex ) {}
    }
    if( modelToViewMethod != null ) {
      try {
	Object obj = modelToViewMethod.invoke( textComp, pos );
	if( obj != null ) {
	  if( obj instanceof Rectangle2D ) {
	    rv = (Rectangle2D) obj;
	  }
	}
      }
      catch( Exception ex ) {}
    }
    return rv;
  }


  public static boolean startsWithIgnoreCase( String text, String prefix )
  {
    int prefixLen = prefix.length();
    if( prefixLen < text.length() ) {
      text = text.substring( 0, prefixLen );
    }
    return text.equalsIgnoreCase( prefix );
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


  /*
   * Ab Java 9 ersetzt die Methode JTextComponent.viewToModel2D(...)
   * die Methode JTextComponent.viewToModel(...).
   * Aud diesem Grund wird die Funktionalitaet in einer Wrapper-Methode
   * gekapselt.
   */
  public static int viewToModel( JTextComponent textComp, Point point )
  {
    int rv = -1;
    if( viewToModelMethod == null ) {
      try {
	viewToModelMethod = textComp.getClass().getMethod(
							"viewToModel2D",
							Point2D.class );
	if( !isPublic( viewToModelMethod ) ) {
	  viewToModelMethod = null;
	}
      }
      catch( NoSuchMethodException ex ) {}
    }
    if( viewToModelMethod == null ) {
      try {
	viewToModelMethod = textComp.getClass().getMethod(
							"viewToModel",
							Point.class );
	if( !isPublic( viewToModelMethod ) ) {
	  viewToModelMethod = null;
	}
      }
      catch( NoSuchMethodException ex ) {}
    }
    if( viewToModelMethod != null ) {
      try {
	Object pos = viewToModelMethod.invoke( textComp, point );
	if( pos != null ) {
	  if( pos instanceof Number ) {
	    rv = ((Number) pos).intValue();
	  }
	}
      }
      catch( Exception ex ) {}
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static boolean isPublic( Method method )
  {
    boolean rv = false;
    if( method != null ) {
      if( (method.getModifiers() & Modifier.PUBLIC) != 0 ) {
	rv = true;
      }
    }
    return rv;
  }


	/* --- privater Konstruktor --- */

  private TextUtil()
  {
    // leer
  }
}
