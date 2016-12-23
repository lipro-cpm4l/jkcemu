/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dokument fuer Eingabefeld mit begrenzter Laenge
 */

package jkcemu.base;

import java.lang.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


public class LimitedDocument extends PlainDocument
{
  private int     maxLen;
  private boolean asciiOnly;
  private boolean swapCase;


  public LimitedDocument( int maxLen )
  {
    this.maxLen    = maxLen;
    this.asciiOnly = false;
    this.swapCase  = false;
  }


  public LimitedDocument()
  {
    this( 0 );
  }


  public int getMaxLength()
  {
    return this.maxLen;
  }


  public void setAsciiOnly( boolean state )
  {
    this.asciiOnly = state;
  }


  public void setMaxLength( int maxLen )
  {
    this.maxLen = maxLen;
  }


  public void setSwapCase( boolean state )
  {
    this.swapCase = state;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void insertString(
			int          offs,
			String       s,
			AttributeSet a ) throws BadLocationException
  {
    if( (s != null) && (this.asciiOnly || this.swapCase) ) {
      char[] buf = s.toCharArray();
      if( buf != null ) {
	int n = 0;
	for( int i = 0; i < buf.length; i++ ) {
	  char ch = buf[ i ];
	  if( !this.asciiOnly || ((ch >= '\u0020') && (ch <= '\u007E')) ) {
	    if( this.swapCase ) {
	      if( Character.isUpperCase( ch ) ) {
		ch = Character.toLowerCase( ch );
	      }
	      else if( Character.isLowerCase( ch ) ) {
		ch = Character.toUpperCase( ch );
	      }
	    }
	    buf[ n++ ] = ch;
	  }
	}
	if( n > 0 ) {
	  s = new String( buf, 0, n );
	} else {
	  s = null;
	}
      }
    }
    if( s != null ) {
      int len = s.length();
      if( len > 0 ) {
	if( (this.maxLen > 0) && (len > this.maxLen - getLength()) ) {
	  len = this.maxLen - getLength();
	}
	if( len > 0 ) {
	  super.insertString( offs, s.substring( 0, len ), a );
	}
      }
    }
  }
}
