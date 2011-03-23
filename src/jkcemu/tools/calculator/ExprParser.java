/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Parser fuer Ausdruecke
 */

package jkcemu.tools.calculator;

import java.lang.*;
import java.math.BigDecimal;
import java.text.*;


public class ExprParser
{
  private final static int MAX_BINARY_DIGITS        = 64;
  private final static int MAX_OCTAL_DIGITS         = 21;
  private final static int MAX_DECIMAL_DIGITS       = 18;
  private final static int MAX_DECIMAL_SCALE_DIGITS = 16;
  private final static int MAX_DECIMAL_EXP_DIGITS   = 8;
  private final static int MAX_HEX_DIGITS           = 16;


  private String            text;
  private CharacterIterator iter;


  public ExprParser()
  {
    this.text = null;
    this.iter = null;
  }


  public Number parseExpr( String text ) throws ParseException
  {
    this.text    = (text != null ? text : "");
    this.iter    = new StringCharacterIterator( this.text );
    Number value = parseInclusiveORExpr();
    char   ch    = skipSpaces();
    if( ch != CharacterIterator.DONE ) {
      fireError( "Unerwartetes Zeichen \'" + Character.toString( ch ) + "\'" );
    }
    return value;
  }


	/* --- private Methoden --- */

  private void checkInteger( Number value ) throws ParseException
  {
    if( isFloat( value ) )
      fireError( "Operation mit Flie\u00DFkommazahlen nicht m\u00F6glich" );
  }


  private void checkToken( char ch ) throws ParseException
  {
    if( skipSpaces() != ch ) {
      fireError( "\'" + Character.toString( ch ) + "\' erwartet" );
    }
    this.iter.next();
  }


  private void fireError( String msg ) throws ParseException
  {
    throw new ParseException( msg, this.iter.getIndex() );
  }


  private void fireNumberOverflow() throws ParseException
  {
    fireError( "\u00DFberlauf in Zahlenliteral" );
  }


  private static boolean isFloat( Number value )
  {
    return (value instanceof BigDecimal)
	   || (value instanceof Double)
	   || (value instanceof Float);
  }


  private static boolean isFloat( Number v1, Number v2 )
  {
    return isFloat( v1 ) || isFloat( v2 );
  }


  private boolean isHexDigit( char ch )
  {
    return ((ch >= 'A') && (ch <= 'F'))
		|| ((ch >= 'a') && (ch <= 'f'))
		|| ((ch >= '0') && (ch <= '9'));
  }


  private Number parseInclusiveORExpr() throws ParseException
  {
    Number value = parseExclusiveORExpr();
    char   ch    = skipSpaces();
    while( ch == '|' ) {
      checkInteger( value );
      this.iter.next();
      Number v2 = parseExclusiveORExpr();
      checkInteger( v2 );
      value = new Long( value.longValue() | v2.longValue() );
      ch    = skipSpaces();
    }
    return value;
  }


  private Number parseExclusiveORExpr() throws ParseException
  {
    Number value = parseAndExpr();
    char   ch    = skipSpaces();
    while( ch == '^' ) {
      checkInteger( value );
      this.iter.next();
      Number v2 = parseAndExpr();
      checkInteger( v2 );
      value = new Long( value.longValue() ^ v2.longValue() );
      ch    = skipSpaces();
    }
    return value;
  }


  private Number parseAndExpr() throws ParseException
  {
    Number value = parseAddExpr();
    char   ch    = skipSpaces();
    while( ch == '&' ) {
      checkInteger( value );
      this.iter.next();
      Number v2 = parseAddExpr();
      checkInteger( v2 );
      value = new Long( value.longValue() & v2.longValue() );
      ch    = skipSpaces();
    }
    return value;
  }


  private Number parseAddExpr() throws ParseException
  {
    Number value = parseMulExpr();
    char   ch    = skipSpaces();
    while( (ch == '+') || (ch == '-') ) {
      this.iter.next();
      Number v2 = parseMulExpr();
      if( ch == '+' ) {
	if( isFloat( value, v2 ) ) {
	  value = new Double( value.doubleValue() + v2.doubleValue() );
	} else {
	  value = new Long( value.longValue() + v2.longValue() );
	}
      } else if( ch == '-' ) {
	if( isFloat( value, v2 ) ) {
	  value = new Double( value.doubleValue() - v2.doubleValue() );
	} else {
	  value = new Long( value.longValue() - v2.longValue() );
	}
      }
      ch = skipSpaces();
    }
    return value;
  }


  private Number parseMulExpr() throws ParseException
  {
    Number value = parseUnaryExpr();
    char   ch    = skipSpaces();
    while( (ch == '*') || (ch == '/') || (ch == '%') ) {
      if( ch == '%' ) {
	checkInteger( value );
      }
      this.iter.next();
      Number v2 = parseUnaryExpr();
      if( ch == '*' ) {
	if( isFloat( value, v2 ) ) {
	  value = new Double( value.doubleValue() * v2.doubleValue() );
	} else {
	  value = new Long( value.longValue() * v2.longValue() );
	}
      } else if( ch == '/' ) {
	if( isFloat( value, v2 ) ) {
	  value = new Double( value.doubleValue() / v2.doubleValue() );
	} else {
	  long lVal = value.longValue() / v2.longValue();
	  if( (lVal * v2.longValue()) == value.longValue() ) {
	    value = new Long( lVal );
	  } else {
	    value = new Double( value.doubleValue() / v2.doubleValue() );
	  }
	}
      } else if( ch == '%' ) {
	checkInteger( v2 );
	value = new Long( value.longValue() % v2.longValue() );
      }
      ch = skipSpaces();
    }
    return value;
  }


  private Number parseUnaryExpr() throws ParseException
  {
    Number value = null;
    char   ch    = skipSpaces();
    if( ch == '+' ) {
      this.iter.next();
      value = parsePrimExpr();
    } else if( ch == '-' ) {
      this.iter.next();
      value = parsePrimExpr();
      if( isFloat( value ) ) {
	value = new Double( -value.doubleValue() );
      } else {
	value = new Long( -value.longValue() );
      }
    } else if( ch == '~' ) {
      this.iter.next();
      value = parsePrimExpr();
      checkInteger( value );
      value = new Long( ~value.longValue() );
    } else {
      value = parsePrimExpr();
    }
    return value;
  }


  private Number parsePrimExpr() throws ParseException
  {
    Number value = null;
    char   ch    = skipSpaces();
    if( ch == '(' ) {
      this.iter.next();
      value = parseInclusiveORExpr();
      checkToken( ')' );
    } else if( (ch == '$') || ((ch >= '0') && (ch <= '9')) ) {
      value = parseNumber();
    } else if( ((ch >= 'A') && (ch <= 'Z'))
	       || ((ch >= 'a') && (ch <= 'z')) )
    {
      value = parseIdentifier();
    } else {
      if( ch != CharacterIterator.DONE ) {
	fireError( "Unerwartetes Zeichen \'"
				+ Character.toString( ch ) + "\'" );
      } else {
	fireError( "Unerwartetes Ende" );
      }
    }
    return value;
  }


  private Number parseNumber() throws ParseException
  {
    char ch = skipSpaces();

    // auf Binaerzahl pruefen
    if( ch == '$' ) {
      ch = this.iter.next();
      if( (ch == '0') || (ch == '1') ) {
	long binValue = 0L;
	do {
	  binValue <<= 1;
	  if( ch == '1' ) {
	    binValue |= 1;
	  }
	  ch = this.iter.next();
	} while( (ch == '0') || (ch == '1') );
	return new Long( binValue );
      } else {
	fireError( "Bin\u00E4rzahl hinter \'$\' erwartet" );
      }
    }

    // auf Hexadezimalzahl mit vorangestelltem '0x' pruefen
    if( ch == '0' ) {
      ch = this.iter.next();
      if( (ch == 'x') || (ch == 'X') ) {
	ch = this.iter.next();
	if( isHexDigit( ch ) ) {
	  long hexValue = 0L;
	  do {
	    hexValue <<= 4;
	    if( (ch >= '0') && (ch <= '9') ) {
	      hexValue |= (ch - '0');
	    } else if( (ch >= 'A') && (ch <= 'F') ) {
	      hexValue |= (ch - 'A' + 10);
	    } else if( (ch >= 'a') && (ch <= 'f') ) {
	      hexValue |= (ch - 'a' + 10);
	    }
	    ch = this.iter.next();
	  } while( isHexDigit( ch ) );
	  return new Long( hexValue );
	} else {
	  fireError( "Hexadezimalziffern hinter \'0x\' erwartet" );
	}
      }
    }

    // Zahl parsen mit evtl. nachgestellter Basis
    boolean isBinary  = true;
    boolean isOctal   = true;
    boolean isDecimal = true;
    boolean isHex     = true;

    long binaryValue   = 0;
    long octalValue    = 0;
    long decimalValue  = 0;
    long hexValue      = 0;

    int nPre = 0;
    while( isHexDigit( ch ) ) {
      nPre++;
      if( (ch == '0') || (ch == '1') ) {
	binaryValue  = (binaryValue << 1) | (ch - '0');
	octalValue   = (octalValue << 3) | (ch - '0');
	decimalValue = (decimalValue * 10) + ch - '0';
	hexValue     = (hexValue << 4) | (ch - '0');
      } else if( (ch >= '2') && (ch <= '7') ) {
	isBinary     = false;
	octalValue   = (octalValue << 3) | (ch - '0');
	decimalValue = (decimalValue * 10) + ch - '0';
	hexValue     = (hexValue << 4) | (ch - '0');
      } else if( (ch == '8') || (ch == '9') ) {
	isBinary     = false;
	isOctal      = false;
	decimalValue = (decimalValue * 10) + ch - '0';
	hexValue     = (hexValue << 4) | (ch - '0');
      } else if( (ch >= 'A') && (ch <= 'F') ) {
	isBinary  = false;
	isOctal   = false;
	isDecimal = false;
	hexValue  = (hexValue << 4) | (ch - 'A' + 10);
      } else if( (ch >= 'a') && (ch <= 'f') ) {
	isBinary  = false;
	isOctal   = false;
	isDecimal = false;
	hexValue  = (hexValue << 4) | (ch - 'a' + 10);
      }
      ch = this.iter.next();
    }

    // auf Fliesskommazahl pruefen
    if( ch == '.' ) {
      if( isDecimal ) {
	if( nPre > MAX_DECIMAL_DIGITS ) {
	  fireNumberOverflow();
	}
	double value = (double) decimalValue;
	int    p     = 0;
	ch           = iter.next();
	if( (ch >= '0') && (ch <= '9') ) {
	  int  nPost = 0;
	  long q     = 1;
	  long s     = 0;
	  while( (ch >= '0') && (ch <= '9') ) {
	    nPost++;
	    if( nPost > MAX_DECIMAL_SCALE_DIGITS ) {
	      fireError( "Zu viele Nachkommastellen" );
	    }
	    q *= 10;
	    s  = (s * 10) + (long) (ch - '0');
	    ch = iter.next();
	  }
	  value += ((double) s / (double) q);
	}
	if( (ch == 'e') || (ch == 'E') ) {
	  boolean negExp = false;
	  ch             = iter.next();
	  if( ch == '+' ) {
	    ch = iter.next();
	  }
	  else if( ch == '-' ) {
	    negExp = true;
	    ch = iter.next();
	  }
	  if( (ch < '0') || (ch > '9') ) {
	    fireError( "Ung\u00FCltiger Exponent in Flie\u00DFkommazahl" );
	  }
	  int nExp     = 0;
	  int expValue = 0;
	  while( (ch >= '0') && (ch <= '9') ) {
	    nExp++;
	    if( nExp > MAX_DECIMAL_EXP_DIGITS ) {
	      fireNumberOverflow();
	    }
	    expValue = (expValue * 10) + (ch - '0');
	    ch       = iter.next();
	  }
	  if( negExp ) {
	    value /= Math.pow( 10.0, (double) expValue );
	  } else {
	    value *= Math.pow( 10.0, (double) expValue );
	  }
	}
	return new Double( value );
      }
      fireError( "Ung\u00FCltige Flie\u00DFkommazahl" );
    }

    // auf Oktalzahl bzw. Hexadezimalzahl pruefen
    if( (ch == 'Q') || (ch == 'q') ) {
      this.iter.next();
      if( isOctal ) {
	if( nPre > MAX_OCTAL_DIGITS ) {
	  fireNumberOverflow();
	}
	return new Long( octalValue );
      } else {
	fireError( "Ung\u00FCltige Oktalzahl" );
      }
    } else if( (ch == 'H') || (ch == 'h') ) {
      if( nPre > MAX_HEX_DIGITS ) {
	fireNumberOverflow();
      }
      this.iter.next();
      return new Long( hexValue );
    }

    // kann nur noch ganze Dezimalzahl sein
    if( !isDecimal ) {
      fireError( "Ziffer 0 bis 9 erwartet" );
    }
    if( nPre > MAX_DECIMAL_DIGITS ) {
      fireNumberOverflow();
    }
    return new Long( decimalValue );
  }


  private Number parseIdentifier() throws ParseException
  {
    StringBuilder buf = new StringBuilder();
    char          ch  = skipSpaces();
    while( ((ch >= '0') && (ch <= '9'))
	   || ((ch >= 'A') && (ch <= 'Z'))
	   || ((ch >= 'a') && (ch <= 'z'))
	   || (ch == '_') )
    {
      buf.append( ch );
      ch = this.iter.next();
    }

    Number value      = null;
    String identifier = buf.toString().toUpperCase();
    if( identifier.equals( "PI" ) ) {
      value = new Double( Math.PI );
    }
    else if( identifier.equals( "PI" ) ) {
      value = new Double( Math.PI );
    }
    else if( identifier.equals( "E" ) ) {
      value = new Double( Math.E );
    }
    else if( identifier.equals( "ABS" ) ) {
      Number arg = parseFunctionArg();
      if( isFloat( arg ) ) {
	value = new Double( Math.abs( arg.doubleValue() ) );
      } else {
	value = new Long( Math.abs( arg.longValue() ) );
      }
    }
    else if( identifier.equals( "ACOS" )
	     || identifier.equals( "ARCCOS" ) )
    {
      value = new Double( Math.acos( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "ASIN" )
	     || identifier.equals( "ARCSIN" ) )
    {
      value = new Double( Math.asin( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "ATAN" )
	     || identifier.equals( "ARCTAN" ) )
    {
      value = new Double( Math.atan( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "COS" ) ) {
      value = new Double( Math.cos( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "COSH" ) ) {
      value = new Double( Math.cosh( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "EXP" ) ) {
      value = new Double( Math.exp( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "LOG" ) ) {
      value = new Double( Math.log( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "LOG10" ) ) {
      value = new Double( Math.log10( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "MAX" ) ) {
      checkToken( '(' );
      value = parseInclusiveORExpr();
      while( skipSpaces() == ',' ) {
	this.iter.next();
	Number tmpValue = parseInclusiveORExpr();
	if( tmpValue.doubleValue() > value.doubleValue() )
	  value = tmpValue;
      }
      checkToken( ')' );
    }
    else if( identifier.equals( "MIN" ) ) {
      checkToken( '(' );
      value = parseInclusiveORExpr();
      while( skipSpaces() == ',' ) {
	this.iter.next();
	Number tmpValue = parseInclusiveORExpr();
	if( tmpValue.doubleValue() < value.doubleValue() )
	  value = tmpValue;
      }
      checkToken( ')' );
    }
    else if( identifier.equals( "POW" ) ) {
      checkToken( '(' );
      Number a = parseInclusiveORExpr();
      checkToken( ',' );
      Number b = parseInclusiveORExpr();
      checkToken( ')' );
      if( (a != null) && (b != null) )
	value = Math.pow( a.doubleValue(), b.doubleValue() );
    }
    else if( identifier.equals( "RND" )
	     || identifier.equals( "RANDOM" ) )
    {
      checkToken( '(' );
      value = new Double( Math.random() );
      checkToken( ')' );
    }
    else if( identifier.equals( "ROUND" ) ) {
      value = new Long( Math.round( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "SIG" )
	     || identifier.equals( "SIGNUM" ) )
    {
      Number arg = parseFunctionArg();
      if( isFloat( arg ) ) {
	value = new Double( Math.signum( arg.doubleValue() ) );
      } else {
	long lv = arg.longValue();
	if( lv < 0 ) {
	  value = new Long( -1L );
	} else if( lv > 0 ) {
	  value = new Long( 1L );
	} else {
	  value = new Long( 0L );
	}
      }
    }
    else if( identifier.equals( "SIN" ) ) {
      value = new Double( Math.sin( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "SINH" ) ) {
      value = new Double( Math.sinh( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "SQR" )
	     || identifier.equals( "SQRT" ) )
    {
      value = new Double( Math.sqrt( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "TAN" ) ) {
      value = new Double( Math.tan( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "TANH" ) ) {
      value = new Double( Math.tanh( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "TO_DEGREES" )
	     || identifier.equals( "TODEGREES" ) )
    {
      value = new Double( Math.toDegrees( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "TO_RADIANS" )
	     || identifier.equals( "TORADIANS" ) )
    {
      value = new Double( Math.toRadians( parseFunctionArg().doubleValue() ) );
    }
    if( value == null ) {
      fireError( "\'" + buf.toString() + "\': Unbekannter Bezeichner" );
    }
    return value;
  }


  private Number parseFunctionArg() throws ParseException
  {
    checkToken( '(' );
    Number value = parseInclusiveORExpr();
    checkToken( ')' );
    return value;
  }


  private char skipSpaces()
  {
    char ch = this.iter.current();
    while( (ch == '\u0020') || (ch == '\t') ) {
      ch = this.iter.next();
    }
    return ch;
  }
}

