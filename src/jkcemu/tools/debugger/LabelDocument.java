/*
 * (c) 2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dokument zur Eingabe einer Marke
 */

package jkcemu.tools.debugger;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import jkcemu.base.UserInputException;
import jkcemu.programming.assembler.AsmLabel;
import jkcemu.text.TextUtil;


public class LabelDocument extends PlainDocument
{
  private boolean reverseCase;


  public LabelDocument()
  {
    this.reverseCase = false;
  }


  public String getLabel() throws UserInputException
  {
    String rv = null;
    try {
      String s = getText( 0, getLength() );
      if( s != null ) {
	if( !s.isEmpty() ) {
	  if( !AsmLabel.isIdentifierStart( s.charAt( 0 ) ) ) {
	    throw new UserInputException(
			"Name muss mit einem Buchstaben oder einem"
				+ " Unterstrich beginnen." );
	  }
	  rv = s;
	}
      }
    }
    catch( BadLocationException ex ) {}
    return rv;
  }


  public void setReverseCase( boolean state )
  {
    this.reverseCase = state;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void insertString(
			int          offs,
			String       s,
			AttributeSet a ) throws BadLocationException
  {
    if( s != null ) {
      int len = s.length();
      if( len > 0 ) {
	char[] buf = new char[ len ];
	int    pos = 0;
	for( int i = 0; i < len; i++ ) {
	  char ch = s.charAt( i );
	  if( AsmLabel.isIdentifierPart( ch ) ) {
	    if( this.reverseCase ) {
	      ch = TextUtil.toReverseCase( ch );
	    }
	    buf[ pos++ ] = ch;
	  }
	}
	if( pos > 0 ) {
	  super.insertString( offs, String.valueOf( buf, 0, pos ), a );
	}
      }
    }
  }
}
