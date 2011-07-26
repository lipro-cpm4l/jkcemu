/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dokument fuer Eingabefeld fuer Hexadezimalzahlen
 */

package jkcemu.base;

import java.lang.*;
import javax.swing.text.*;


public class HexDocument extends PlainDocument
{
  private JTextComponent textComp;
  private int            maxLen;
  private String         preErrText;


  public HexDocument( JTextComponent textComp, int maxLen )
  {
    this( textComp, maxLen, null );
  }


  public HexDocument( JTextComponent textComp, int maxLen, String label )
  {
    this.textComp   = textComp;
    this.maxLen     = maxLen;
    this.preErrText = "";
    this.textComp.setDocument( this );
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
    this.textComp.setText( "" );
  }


  public Integer getInteger() throws NumberFormatException
  {
    Integer rv = null;
    String  s  = this.textComp.getText();
    if( s != null ) {
      if( s.length() > 0 )
	rv = new Integer( parseHex( s ) );
    }
    return rv;
  }


  public int intValue() throws NumberFormatException
  {
    String s = this.textComp.getText();
    if( s == null ) {
      s = "";
    }
    return parseHex( s );
  }


  public void setValue( int value, int numDigits )
  {
    String text = Integer.toHexString( value ).toUpperCase();
    if( numDigits > 0 ) {
      int len = text.length();
      if( len < numDigits ) {
	StringBuilder buf = new StringBuilder( numDigits );
	for( int i = len; i < numDigits; i++ ) {
	  buf.append( (char) '0' );
	}
	buf.append( text );
	text = buf.toString();
      }
    }
    this.textComp.setText( text );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void insertString( int offs, String str, AttributeSet a )
						throws BadLocationException
  {
    if( str != null ) {
      int len = str.length();
      if( len > 0 ) {
	// ungueltige Zeichen aussortieren
	int	pos = 0;
	char[]	buf = new char[ len ];
	for( int i = 0; i < len; i++ ) {
	  char ch = Character.toUpperCase( str.charAt( i ) );
	  if( ((ch >= '0') && (ch <= '9')) ||
	      ((ch >= 'A') && (ch <= 'F') ) )
	  {
	    buf[ pos++ ] = ch;
	  }
	}

	// Laenge pruefen
	if( pos > this.maxLen - getLength() )
	  pos = this.maxLen - getLength();

	// Text einfuegen
	if( pos > 0 )
	  super.insertString( offs, new String( buf, 0, pos ), a );
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
      this.textComp.requestFocus();
      throw new NumberFormatException(
		this.preErrText
			+ "Ung\u00FCltiges Format!\n"
			+ "Bitte geben Sie eine hexadezimale Zahl ein." );
    }
    return value;
  }
}

