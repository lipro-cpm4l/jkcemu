/*
 * (c) 2008 Jens Mueller
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
  private int maxLen;


  public LimitedDocument( int maxLen )
  {
    this.maxLen = maxLen;
  }


  public void setMaxLength( int maxLen )
  {
    this.maxLen = maxLen;
  }


	/* --- ueberschriebene Methoden --- */

  public void insertString( int offs, String str, AttributeSet a )
						throws BadLocationException
  {
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

