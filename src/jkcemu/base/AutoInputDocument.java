/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dokument fuer den Eingabetext eines AutoInput-Eintrag
 */

package jkcemu.base;

import java.lang.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


public class AutoInputDocument extends PlainDocument
{
  private static char[][] codeMapping = {
				{ '\u0003', '\u21B1' },
				{ '\u0008', '\u2190' },
				{ '\t',     '\u2192' },
				{ '\n',     '\u2193' },
				{ '\u000B', '\u2191' },
				{ '\r',     '\u21B5' } };

  private static Map<Character,Character> raw2Visible = null;
  private static Map<Character,Character> visible2Raw = null;

  private boolean swapCase;


  public AutoInputDocument( boolean swapCase )
  {
    this.swapCase = swapCase;
  }


  public static String toRawText( String text )
  {
    String rv = null;
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	char[] a = new char[ len ];
	for( int i = 0; i < len; i++ ) {
	  char      ch  = text.charAt( i );
	  Character ch1 = getVisible2RawMap().get( ch );
	  if( ch1 != null ) {
	    ch = ch1.charValue();
	  }
	  a[ i ] = ch;
	}
	rv = new String( a );
      } else {
	rv = "";
      }
    }
    return rv;
  }


  public static String toVisibleText( String text )
  {
    String rv = null;
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	char[] a = new char[ len ];
	for( int i = 0; i < len; i++ ) {
	  char      ch  = text.charAt( i );
	  Character ch1 = getRaw2VisibleMap().get( ch );
	  if( ch1 != null ) {
	    ch = ch1.charValue();
	  }
	  a[ i ] = ch;
	}
	rv = new String( a );
      } else {
	rv = "";
      }
    }
    return rv;
  }


  public boolean getSwapCase()
  {
    return this.swapCase;
  }


  public void setSwapCase( boolean state )
  {
    this.swapCase = state;
  }


	/* --- ueberschriebene Methoden --- */

  /*
   * nur ASCII-Zeichen sowie im Mapping
   * eingetragene sichtbare Zeichen zulassen
   */
  @Override
  public synchronized void insertString(
				int          offs,
				String       s,
				AttributeSet a ) throws BadLocationException
  {
    if( s != null ) {
      int len = s.length();
      if( len > 0 ) {
	StringBuilder buf = new StringBuilder( len );
	for( int i = 0; i < len; i++ ) {
	  char ch = s.charAt( i );
	  if( (ch >= '\u0020') && (ch <= '\u007E') ) {
	    if( this.swapCase ) {
	      if( (ch >= 'A') && (ch <= 'Z') ) {
		ch = Character.toLowerCase( ch );
	      } else if( (ch >= 'a') && (ch <= 'z') ) {
		ch = Character.toUpperCase( ch );
	      }
	    }
	    buf.append( ch );
	  } else if( getVisible2RawMap().containsKey( ch ) ) {
	    buf.append( ch );
	  }
	}
	if( buf.length() > 0 ) {
	  super.insertString( offs, buf.toString(), a );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private static Map<Character,Character> getRaw2VisibleMap()
  {
    synchronized( AutoInputDocument.class ) {
      if( raw2Visible == null ) {
	raw2Visible = new HashMap<>();
	for( int i = 0; i < codeMapping.length; i++ ) {
	  raw2Visible.put( codeMapping[ i ][ 0 ], codeMapping[ i ][ 1 ] );
	}
      }
    }
    return raw2Visible;
  }


  private static Map<Character,Character> getVisible2RawMap()
  {
    synchronized( AutoInputDocument.class ) {
      if( visible2Raw == null ) {
	visible2Raw = new HashMap<>();
	for( int i = 0; i < codeMapping.length; i++ ) {
	  visible2Raw.put( codeMapping[ i ][ 1 ], codeMapping[ i ][ 0 ] );
	}
      }
    }
    return visible2Raw;
  }
}
