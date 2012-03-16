/*
 * (c) 2008-2010 Jens Mueller
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
  private boolean swapCase;


  public LimitedDocument( int maxLen )
  {
    this.maxLen   = maxLen;
    this.swapCase = false;
  }


  public LimitedDocument( int maxLen, boolean swapCase )
  {
    this.maxLen   = maxLen;
    this.swapCase = swapCase;
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
    if( (str != null) && this.swapCase ) {
      char[] ary = str.toCharArray();
      if( ary != null ) {
	for( int i = 0; i < ary.length; i++ ) {
	  char ch = ary[ i ];
	  if( Character.isUpperCase( ch ) ) {
	    ary[ i ] = Character.toLowerCase( ch );
	  }
	  else if( Character.isLowerCase( ch ) ) {
	    ary[ i ] = Character.toUpperCase( ch );
	  }
	}
	str = new String( ary );
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

