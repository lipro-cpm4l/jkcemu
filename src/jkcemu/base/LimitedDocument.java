/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dokument fuer Eingabefeld mit begrenzter Laenge
 */

package jkcemu.base;

import java.lang.*;
import javax.swing.text.*;


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
  public void insertString( int offs, String str, AttributeSet a )
						throws BadLocationException
  {
    if( (str != null) && (this.asciiOnly || this.swapCase) ) {
      char[] ary = str.toCharArray();
      if( ary != null ) {
	int n = 0;
	for( int i = 0; i < ary.length; i++ ) {
	  char ch = ary[ i ];
	  if( !this.asciiOnly || ((ch >= '\u0020') && (ch <= '\u007E')) ) {
	    if( this.swapCase ) {
	      if( Character.isUpperCase( ch ) ) {
		ch = Character.toLowerCase( ch );
	      }
	      else if( Character.isLowerCase( ch ) ) {
		ch = Character.toUpperCase( ch );
	      }
	    }
	    ary[ n++ ] = ch;
	  }
	}
	if( n > 0 ) {
	  str = new String( ary, 0, n );
	} else {
	  str = null;
	}
      }
    }
    if( str != null ) {
      int len = str.length();
      if( len > 0 ) {
	if( (this.maxLen > 0) && (len > this.maxLen - getLength()) ) {
	  len = this.maxLen - getLength();
	}
	if( len > 0 ) {
	  super.insertString( offs, str.substring( 0, len ), a );
	}
      }
    }
  }
}

