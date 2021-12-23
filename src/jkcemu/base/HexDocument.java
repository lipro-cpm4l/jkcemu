/*
 * (c) 2008-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dokument fuer Eingabefeld fuer Hexadezimalzahlen
 */

package jkcemu.base;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


public class HexDocument extends PlainDocument
{
  private int    maxLen;
  private String preErrText;


  public HexDocument( int maxLen )
  {
    this( maxLen, null );
  }


  public HexDocument( int maxLen, String label )
  {
    this.maxLen     = maxLen;
    this.preErrText = "";
    if( label != null ) {
      if( label.endsWith( ":" ) ) {
	this.preErrText = label + "\n";
      } else {
	this.preErrText = label + ":\n";
      }
    }
  }


  public void clear()
  {
    int len = getLength();
    if( len > 0 ) {
      try {
	remove( 0, len );
      }
      catch( BadLocationException ex ) {}
    }
  }


  public Integer getInteger() throws NumberFormatException
  {
    Integer rv = null;
    try {
      String s = getText( 0, getLength() );
      if( s != null ) {
	if( !s.isEmpty() ) {
	  rv = Integer.valueOf( parseHex( s ) );
	}
      }
    }
    catch( BadLocationException ex ) {}
    return rv;
  }


  public int getMaxLength()
  {
    return this.maxLen;
  }


  public int intValue() throws NumberFormatException
  {
    String s = null;
    try {
      s = getText( 0, getLength() );
    }
    catch( BadLocationException ex ) {}
    return parseHex( s != null ? s : "" );
  }


  public void setMaxLength( int maxLen )
  {
    if( maxLen > 0 ) {
      try {
	int n = getLength() - maxLen;
	if( n > 0 ) {
	  remove( 0, n );	// zuviele Zeichen links herausloeschen
	}
	this.maxLen = maxLen;
      }
      catch( BadLocationException ex ) {}
    }
  }


  public void setMaxLength( int maxLen, char leadingChar )
  {
    setMaxLength( maxLen );
    int n = maxLen - getLength();
    if( n > 0 ) {
      try {
	String leadingStr = Character.toString( leadingChar );
	while( n > 0 ) {
	  insertString( 0, leadingStr, null );
	  --n;
	}
      }
      catch( BadLocationException ex ) {}
    }
  }


  public void setValue( int value, int numDigits )
  {
    String text = Integer.toHexString( value ).toUpperCase();
    if( numDigits > 0 ) {
      int len = text.length();
      if( len < numDigits ) {
	StringBuilder buf = new StringBuilder( numDigits );
	for( int i = len; i < numDigits; i++ ) {
	  buf.append( '0' );
	}
	buf.append( text );
	if( (numDigits > this.maxLen) && (this.maxLen > 0) ) {
	  text = buf.substring( numDigits - this.maxLen );
	} else {
	  text = buf.toString();
	}
      }
    }
    try {
      int len = getLength();
      if( len > 0 ) {
	remove( 0, len );
      }
      insertString( 0, text, null );
    }
    catch( BadLocationException ex ) {}
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void insertString(
			int          offs,
			String       s,
			AttributeSet a ) throws BadLocationException
  {
    if( s != null ) {

      // ungueltige Zeichen aussortieren
      char[] buf = s.toCharArray();
      int    pos = 0;
      for( char ch : buf ) {
	if( ((ch >= '0') && (ch <= '9')) ||
	    ((ch >= 'A') && (ch <= 'F') ) )
	{
	  buf[ pos++ ] = ch;
	}
	else if( (ch >= 'a') && (ch <= 'f') )
	{
	  buf[ pos++ ] = (char) (ch - 'a' + 'A');
	}
      }

      // Laenge pruefen
      if( pos > this.maxLen - getLength() ) {
	pos = this.maxLen - getLength();
      }

      // Text einfuegen
      if( pos > 0 ) {
	super.insertString( offs, String.valueOf( buf, 0, pos ), a );
      }
    }
  }


	/* --- private Methoden --- */

  private int parseHex( String s ) throws NumberFormatException
  {
    int value = 0;
    try {
      value = Integer.parseInt( s.trim(), 16 );
    }
    catch( NumberFormatException ex ) {
      throw new NumberFormatException(
		this.preErrText
			+ "Ung\u00FCltiges Format!\n"
			+ "Bitte geben Sie eine hexadezimale Zahl ein." );
    }
    return value;
  }
}
