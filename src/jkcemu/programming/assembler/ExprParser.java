/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Parser fuer einen Ausdrucks
 */

package jkcemu.programming.assembler;

import java.lang.*;
import java.util.Map;
import jkcemu.programming.PrgException;


public class ExprParser
{
  private String               text;
  private int                  len;
  private int                  pos;
  private Map<String,AsmLabel> labels;
  private boolean              checkLabels;
  private boolean              labelsCaseSensitive;


  public static int parse(
			String               text,
			Map<String,AsmLabel> labels,
			boolean              checkLabels,
			boolean              labelsCaseSensitive )
							throws PrgException
  {
    return (new ExprParser(
			text,
			labels,
			checkLabels,
			labelsCaseSensitive )).parse();
  }


	/* --- private Methoden --- */

  private ExprParser(
		String               text,
		Map<String,AsmLabel> labels,
		boolean              checkLabels,
		boolean              labelsCaseSensitive )
  {
    this.text                = (text != null ? text : "");
    this.len                 = text.length();
    this.pos                 = 0;
    this.labels              = labels;
    this.checkLabels         = checkLabels;
    this.labelsCaseSensitive = labelsCaseSensitive;
  }


  private int parse() throws PrgException
  {
    this.pos = 0;
    int value = parseExpr();
    if( this.pos < this.len ) {
      throw new PrgException( "\'" + this.text.charAt( this.pos )
			+ "\': Unerwartetes Zeichen hinter Ausdruck" );
    }
    return value;
  }


  private int parseExpr() throws PrgException
  {
    int value = 0;
    int ch    = 0;
    if( this.pos < this.len ) {
      ch = this.text.charAt( this.pos );
      if( (ch == '-') || (ch == '+') )
	this.pos++;
    }
    value = parseUnaryExpr();
    if( ch == '-' ) {
      value = -value;
    }
    while( this.pos < this.len ) {
      ch = this.text.charAt( this.pos );
      if( ch == '+' ) {
	this.pos++;
	value += parseUnaryExpr();
      }
      else if( ch == '-' ) {
	this.pos++;
	value -= parseUnaryExpr();
      } else {
	break;
      }
    }
    return value;
  }


  private int parseUnaryExpr() throws PrgException
  {
    int value = 0;
    if( this.pos >= this.len ) {
      throw new PrgException( "Unerwartetes Ende des Arguments" );
    }
    if( this.text.regionMatches( true, this.pos, "LOW(", 0, 4 ) ) {
      this.pos += 4;
      value = parseExpr() & 0xFF;
      parseToken( ')' );
    }
    else if( this.text.regionMatches( true, this.pos, "L(", 0, 2 ) ) {
      this.pos += 2;
      value = parseExpr() & 0xFF;
      parseToken( ')' );
    }
    else if( this.text.regionMatches( true, this.pos, "HIGH(", 0, 5 ) ) {
      this.pos += 5;
      value = (parseExpr() >> 8) & 0xFF;
      parseToken( ')' );
    }
    else if( this.text.regionMatches( true, this.pos, "H(", 0, 2 ) ) {
      this.pos += 2;
      value = (parseExpr() >> 8) & 0xFF;
      parseToken( ')' );
    }
    else if( this.text.regionMatches( this.pos, "\'", 0, 1 ) ) {
      this.pos++;
      if( this.pos + 1 >= this.len ) {
	throw new PrgException( "Ung\u00FCltiges Zeichenliteral" );
      }
      value   = this.text.charAt( this.pos++ );
      char ch = this.text.charAt( this.pos++ );
      if( ch != '\'' ) {
	throw new PrgException(
		"\'" + ch + "\': Unerwartetes Zeichen im Zeichenliteral" );
      }
    } else {
      char ch = this.text.charAt( this.pos );
      if( (ch >= '0') && (ch <= '9') ) {
	value = parseNumber();
      }
      else if( ((ch >= 'A') && (ch <= 'Z'))
	       || ((ch >= 'a') && (ch <= 'z'))
	       || (ch == '_') )
      {
	value = parseLabel();
      } else {
	throw new PrgException(
		"\'" + ch + "\': Ung\u00FCltiges Zeichen im Argument" );
      }
    }
    return value;
  }


  private int parseLabel() throws PrgException
  {
    int value = 0;

    StringBuilder buf = null;
    if( this.labels != null ) {
      buf = new StringBuilder();
    }
    while( this.pos < this.len ) {
      char ch = this.text.charAt( this.pos );
      if( ((ch >= 'A') && (ch <= 'Z'))
	       || ((ch >= 'a') && (ch <= 'z'))
	       || ((ch >= '0') && (ch <= '9'))
	       || (ch == '_') || (ch == '$') )
      {
	this.pos++;
	if( buf != null ) {
	  if( this.labelsCaseSensitive ) {
	    buf.append( ch );
	  } else {
	    buf.append( Character.toUpperCase( ch ) );
	  }
	}
      } else {
	break;
      }
    }
    if( buf != null ) {
      String labelText = buf.toString();
      String upperText = labelText;
      if( this.labelsCaseSensitive ) {
	upperText = labelText.toUpperCase();
      }
      if( AsmArg.isFlagCondition( upperText ) ) {
	throw new PrgException( labelText + ": Unerwartete Flag-Bedingung" );
      }
      if( AsmArg.isRegister( upperText ) ) {
	throw new PrgException( labelText + ": Unerwartete Register-Angabe" );
      }
      AsmLabel label = this.labels.get( labelText );
      if( label != null ) {
	value = label.getLabelValue();
      } else {
	if( this.checkLabels )
	  throw new PrgException( buf.toString() + ": Unbekannte Marke" );
      }
    }
    return value;
  }


  private int parseNumber() throws PrgException
  {
    int value = 0;
    int ch    = 0;

    StringBuilder buf = new StringBuilder();
    while( this.pos < this.len ) {
      ch = this.text.charAt( this.pos );
      if( ((ch >= '0') && (ch <= '9'))
	  || ((ch >= 'A') && (ch <= 'F'))
	  || ((ch >= 'a') && (ch <= 'f')) )
      {
	this.pos++;
	buf.append( (char) ch );
      } else {
	break;
      }
    }
    ch = 0;
    if( this.pos < this.len ) {
      ch = this.text.charAt( this.pos );
    }
    if( (ch == 'O') || (ch == 'o') || (ch == 'Q') || (ch == 'q') ) {
      this.pos++;
      try {
	value = Integer.parseInt( buf.toString(), 8 );
      }
      catch( NumberFormatException ex ) {
	throw new PrgException(
			buf.toString() + ": Ung\u00FCltige Oktalzahl" );
      }
    }
    else if( (ch == 'H') || (ch == 'h') ) {
      this.pos++;
      try {
	value = Integer.parseInt( buf.toString(), 16 );
      }
      catch( NumberFormatException ex ) {
	throw new PrgException(
			buf.toString() + ": Ung\u00FCltige Hexadezimalzahl" );
      }
    } else {
      boolean done = false;
      int     len  = buf.length();
      if( len > 1 ) {
	ch = buf.charAt( len - 1 );
	if( (ch == 'B') || (ch == 'b') ) {
	  done = true;
	  buf.setLength( len - 1 );
	  try {
	    value = Integer.parseInt( buf.toString(), 2 );
	  }
	  catch( NumberFormatException ex ) {
	    throw new PrgException(
			buf.toString() + ": Ung\u00FCltige Bin\u00E4rzahl" );
	  }
	}
      }
      if( !done ) {
	try {
	  value = Integer.parseInt( buf.toString() );
	}
	catch( NumberFormatException ex ) {
	  throw new PrgException(
			buf.toString() + ": Ung\u00FCltige Zahl" );
	}
      }
    }
    return value;
  }


  private void parseToken( char token ) throws PrgException
  {
    if( this.pos < this.len ) {
      if( this.text.charAt( this.pos ) == token ) {
	this.pos++;
	return;
      }
    }
    throw new PrgException( "\'" + token + "\' erwartet" );
  }
}

