/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Document fuer Eingabefeld fuer ganze Zahlen
 */

package jkcemu.base;

import java.lang.*;
import javax.swing.text.*;


public class IntegerDocument extends PlainDocument
{
  private JTextComponent textComp;
  private Integer        minValue;
  private Integer        maxValue;


  public IntegerDocument( JTextComponent textComp )
  {
    this.textComp = textComp;
    this.minValue = null;
    this.maxValue = null;
    this.textComp.setDocument( this );
  }


  public IntegerDocument(
		JTextComponent textComp,
		int            minValue,
		int            maxValue )
  {
    this( textComp );
    this.minValue = new Integer( minValue );
    this.maxValue = new Integer( maxValue );
  }


  public IntegerDocument(
		JTextComponent textComp,
		Integer        minValue,
		Integer        maxValue )
  {
    this( textComp );
    this.minValue = minValue;
    this.maxValue = maxValue;
  }


  public void clear()
  {
    this.textComp.setText( "" );
  }


  public int intValue() throws NumberFormatException
  {
    String s = this.textComp.getText();
    if( s == null ) {
      s = "";
    }
    return parseInt( s );
  }


  public Integer getInteger() throws NumberFormatException
  {
    Integer rv = null;
    String  s  = this.textComp.getText();
    if( s != null ) {
      s = s.trim();
      if( !s.isEmpty() ) {
        rv = new Integer( parseInt( s ) );
      }
    }
    return rv;
  }


  public void setValue( int value )
  {
    this.textComp.setText( String.valueOf( value ) );
  }


  public void setValue( Integer value )
  {
    if( value != null ) {
      setValue( value.intValue() );
    } else {
      clear();
    }
  }


	/* --- private Methoden --- */

  private int parseInt( String s ) throws NumberFormatException
  {
    int value = 0;
    try {
      value = Integer.parseInt( s );
    }
    catch( NumberFormatException ex ) {
      this.textComp.requestFocus();
      throw new NumberFormatException(
	"Ung\u00FCltiges Format!\nBitte geben Sie eine ganze Zahl ein." );
    }
    if( this.minValue != null ) {
      if( value < this.minValue.intValue() ) {
	this.textComp.requestFocus();
	throw new NumberFormatException(
		"Wert zu klein, min. " + this.minValue.toString() + "." );
      }
    }
    if( this.maxValue != null ) {
      if( value > this.maxValue.intValue() ) {
	this.textComp.requestFocus();
	throw new NumberFormatException(
		"Wert zu gro\u00DF, max. " + this.maxValue.toString() + "." );
      }
    }
    return value;
  }
}

