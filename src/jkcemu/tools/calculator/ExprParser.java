/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Parser fuer Ausdruecke
 */

package jkcemu.tools.calculator;

import java.lang.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.text.ParseException;


public class ExprParser
{
  private final static int MAX_BINARY_DIGITS      = 64;
  private final static int MAX_DECIMAL_DIGITS     = 1000;
  private final static int MAX_DECIMAL_EXP_DIGITS = 5;
  private final static int MAX_OCTAL_DIGITS       = 21;
  private final static int MAX_HEX_DIGITS         = 16;
  private final static int MIN_DIV_SCALE          = 16;

  private String            text;
  private CharacterIterator iter;


  public ExprParser()
  {
    this.text = null;
    this.iter = null;
  }


  public BigDecimal parseExpr( String text ) throws ParseException
  {
    BigDecimal value = null;
    try {
      this.text = (text != null ? text : "");
      this.iter = new StringCharacterIterator( this.text );
      value     = parseInclusiveORExpr().stripTrailingZeros();
      char ch   = skipSpaces();
      if( ch != CharacterIterator.DONE ) {
	fireUnexpectedChar( ch );
      }
    }
    catch( ArithmeticException ex ) {
      String msg   = "Arithmetischer Fehler";
      String exMsg = ex.getMessage();
      if( exMsg != null ) {
	if( !exMsg.isEmpty() ) {
	  msg = msg + ": " + exMsg;
	}
      }
      fireError( msg );
    }
    return value;
  }


	/* --- private Methoden --- */

  private void checkInteger( BigDecimal value ) throws ParseException
  {
    if( value.scale() > 0 )
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


  private void fireUnexpectedChar( char ch ) throws ParseException
  {
    fireError( String.format( "Unerwartetes Zeichen \'%c\'", ch ) );
  }


  private boolean isHexDigit( char ch )
  {
    return ((ch >= 'A') && (ch <= 'F'))
		|| ((ch >= 'a') && (ch <= 'f'))
		|| ((ch >= '0') && (ch <= '9'));
  }


  private void fireHexOverflow() throws ParseException
  {
    fireError( "Hexadezimalzahl zu gro\u00DF" );
  }


  private BigDecimal parseInclusiveORExpr() throws ParseException
  {
    BigDecimal value = parseExclusiveORExpr();
    char       ch    = skipSpaces();
    while( ch == '|' ) {
      checkInteger( value );
      this.iter.next();
      BigDecimal v2 = parseExclusiveORExpr();
      checkInteger( v2 );
      value = new BigDecimal( toLong( value ) | toLong( v2 ) );
      ch    = skipSpaces();
    }
    return value;
  }


  private BigDecimal parseExclusiveORExpr() throws ParseException
  {
    BigDecimal value = parseAndExpr();
    char       ch    = skipSpaces();
    while( ch == '^' ) {
      checkInteger( value );
      this.iter.next();
      BigDecimal v2 = parseAndExpr();
      checkInteger( v2 );
      value = new BigDecimal( toLong( value ) ^ toLong( v2 ) );
      ch    = skipSpaces();
    }
    return value;
  }


  private BigDecimal parseAndExpr() throws ParseException
  {
    BigDecimal value = parseAddExpr();
    char       ch    = skipSpaces();
    while( ch == '&' ) {
      checkInteger( value );
      this.iter.next();
      BigDecimal v2 = parseAddExpr();
      checkInteger( v2 );
      value = new BigDecimal( toLong( value ) & toLong( v2 ) );
      ch    = skipSpaces();
    }
    return value;
  }


  private BigDecimal parseAddExpr() throws ParseException
  {
    BigDecimal value = parseMulExpr();
    char       ch    = skipSpaces();
    while( (ch == '+') || (ch == '-') ) {
      this.iter.next();
      BigDecimal v2 = parseMulExpr();
      if( ch == '+' ) {
	value = value.add( v2 );
      } else if( ch == '-' ) {
	value = value.subtract( v2 );
      }
      ch = skipSpaces();
    }
    return value;
  }


  private BigDecimal parseMulExpr() throws ParseException
  {
    BigDecimal value = parseUnaryExpr();
    char       ch    = skipSpaces();
    while( (ch == '*') || (ch == '/') || (ch == '%') ) {
      if( ch == '%' ) {
	checkInteger( value );
      }
      this.iter.next();
      BigDecimal v2 = parseUnaryExpr();
      if( ch == '*' ) {
	value = value.multiply( v2 );
      } else if( ch == '/' ) {
	if( v2.compareTo( BigDecimal.ZERO ) == 0 ) {
	  fireError( "Division durch 0" );
	}
	int scale1 = Math.max( value.precision(), value.scale() );
	int scale2 = Math.max( v2.precision(), v2.scale() );
	value = value.divide(
			v2,
			new MathContext(
				Math.max( scale1, scale2 ) + MIN_DIV_SCALE,
				RoundingMode.HALF_EVEN ) );
      } else if( ch == '%' ) {
	long l2 = toLong( v2 );
	if( l2 == 0 ) {
	  fireError( "Module durch 0" );
	}
	value = new BigDecimal( toLong( value ) % l2 );
      }
      ch = skipSpaces();
    }
    return value;
  }


  private BigDecimal parseUnaryExpr() throws ParseException
  {
    BigDecimal value = null;
    char       ch    = skipSpaces();
    if( ch == '+' ) {
      this.iter.next();
      value = parsePrimExpr();
    } else if( ch == '-' ) {
      this.iter.next();
      value = parsePrimExpr().negate();
    } else if( ch == '~' ) {
      this.iter.next();
      value = parsePrimExpr();
      checkInteger( value );
      value = new BigDecimal( ~toLong( value ) );
    } else {
      value = parsePrimExpr();
    }
    return value;
  }


  private BigDecimal parsePrimExpr() throws ParseException
  {
    BigDecimal value = null;
    char       ch    = skipSpaces();
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
	fireUnexpectedChar( ch );
      } else {
	fireError( "Unerwartetes Ende" );
      }
    }
    return value;
  }


  private BigDecimal parseNumber() throws ParseException
  {
    char ch     = skipSpaces();
    int  begPos = this.iter.getIndex();
    int  digits = 0;

    // auf Binaerzahl pruefen
    if( ch == '$' ) {
      ch = this.iter.next();
      if( (ch == '0') || (ch == '1') ) {
	long binValue = 0L;
	do {
	  if( (ch != '0') || (digits > 0) ) {
	    digits++;
	  }
	  binValue <<= 1;
	  if( ch == '1' ) {
	    binValue |= 1;
	  }
	  ch = this.iter.next();
	} while( (ch == '0') || (ch == '1') );
	if( digits > MAX_BINARY_DIGITS ) {
	  fireError( "Bin\u00E4rzahl zu gro\u00DF" );
	}
	return new BigDecimal( binValue );
      } else {
	fireError( "Bin\u00E4rzahl hinter \'$\' erwartet" );
      }
    }

    // auf Hexadezimalzahl mit vorangestelltem '0x' pruefen
    digits = 0;
    if( ch == '0' ) {
      ch = this.iter.next();
      if( (ch == 'x') || (ch == 'X') ) {
	ch = this.iter.next();
	if( isHexDigit( ch ) ) {
	  long hexValue = 0L;
	  do {
	    if( (ch != '0') || (digits > 0) ) {
	      digits++;
	    }
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
	  if( digits > MAX_HEX_DIGITS ) {
	    fireHexOverflow();
	  }
	  return new BigDecimal( hexValue );
	} else {
	  fireError( "Hexadezimalziffern hinter \'0x\' erwartet" );
	}
      }
    }

    // auf Binaer-, Oktalzahl bzw. Hexadezimalzahl pruefen
    boolean isBinary = true;
    boolean isOctal  = true;
    boolean isHex    = true;

    long binaryValue = 0;
    long octalValue  = 0;
    long hexValue    = 0;

    digits = 0;
    while( isHexDigit( ch ) ) {
      if( (ch != '0') || (digits > 0) ) {
	digits++;
      }
      if( (ch == '0') || (ch == '1') ) {
	binaryValue = (binaryValue << 1) | (ch - '0');
	octalValue  = (octalValue << 3) | (ch - '0');
	hexValue    = (hexValue << 4) | (ch - '0');
      } else if( (ch >= '2') && (ch <= '7') ) {
	isBinary   = false;
	octalValue = (octalValue << 3) | (ch - '0');
	hexValue   = (hexValue << 4) | (ch - '0');
      } else if( (ch == '8') || (ch == '9') ) {
	isBinary = false;
	isOctal  = false;
	hexValue = (hexValue << 4) | (ch - '0');
      } else if( (ch >= 'A') && (ch <= 'F') ) {
	isBinary = false;
	isOctal  = false;
	hexValue = (hexValue << 4) | (ch - 'A' + 10);
      } else if( (ch >= 'a') && (ch <= 'f') ) {
	isBinary = false;
	isOctal  = false;
	hexValue = (hexValue << 4) | (ch - 'a' + 10);
      }
      ch = this.iter.next();
    }
    if( isOctal && ((ch == 'Q') || (ch == 'q')) ) {
      this.iter.next();
      if( isOctal ) {
	if( digits > MAX_OCTAL_DIGITS ) {
	  fireError( "Oktalzahl zu gro\u00DF" );
	}
	return new BigDecimal( octalValue );
      } else {
	fireError( "Ung\u00FCltige Oktalzahl" );
      }
    } else if( isHex && ((ch == 'H') || (ch == 'h')) ) {
      if( digits > MAX_HEX_DIGITS ) {
	fireHexOverflow();
      }
      this.iter.next();
      return new BigDecimal( hexValue );
    }

    /*
     * auf Dezimalzahl pruefen
     *
     * Theoretisch kann BigDecimal mit beliebig vielen Stellen umgehen.
     * In der Prxis geht das aber bei zu grossen Zahlen zu Lasten
     * der Performance, weshalb das ganze Java-System zeitweise
     * zum Stillstand kommen kann.
     * Aus diesem wird hier die Groesse einer Zahl
     * (Anzahl der Vor- und Nachkommastelle sowie Zehnerpotenz) begrenzt.
     */
    String errMsg = "Ung\u00FCltige Dezimalzahl";
    this.iter.setIndex( begPos );
    ch     = this.iter.current();
    digits = 0;
    while( (ch >= '0') && (ch <= '9') ) {
      if( (ch != '0') || (digits > 0) ) {
	digits++;
	if( digits > MAX_DECIMAL_DIGITS ) {
	  fireError( "Zahl zu gro\u00DF" );
	}
      }
      ch = this.iter.next();
    }
    if( ch == '.' ) {
      errMsg = "Ung\u00FCltige Flie\u00DFkommazahl";
      ch = this.iter.next();
      if( (ch < '0') || (ch > '9') ) {
	fireError( errMsg );
      }
      digits = 1;
      ch = this.iter.next();
      while( (ch >= '0') && (ch <= '9') ) {
	digits++;
	if( digits > MAX_DECIMAL_DIGITS ) {
	  fireError( "Zu viele Nachkommastellen" );
	}
	ch = this.iter.next();
      }
    }
    if( (ch == 'e') || (ch == 'E') ) {
      ch = this.iter.next();
      if( (ch == '-') || (ch == '+') ) {
	ch = this.iter.next();
      }
      if( (ch < '0') || (ch > '9') ) {
	fireError( errMsg );
      }
      digits = 0;
      while( (ch >= '0') && (ch <= '9') ) {
	if( (ch != '0') || (digits > 0) ) {
	  digits++;
	  if( digits > MAX_DECIMAL_EXP_DIGITS ) {
	    fireError( "Exponent zu gro\u00DF" );
	  }
	}
	ch = this.iter.next();
      }
    }
    BigDecimal value  = null;
    int        endPos = this.iter.getIndex();
    if( endPos > begPos ) {
      try {
	value = new BigDecimal( this.text.substring( begPos, endPos ) );
      }
      catch( NumberFormatException ex ) {}
    }
    if( value == null ) {
      fireError( errMsg );
    }
    return value;
  }


  private BigDecimal parseIdentifier() throws ParseException
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

    BigDecimal value      = null;
    String     identifier = buf.toString().toUpperCase();
    if( identifier.equals( "PI" ) ) {
      value = new BigDecimal( Math.PI );
    }
    else if( identifier.equals( "E" ) ) {
      value = new BigDecimal( Math.E );
    }
    else if( identifier.equals( "ABS" ) ) {
      BigDecimal arg = parseFunctionArg();
      if( arg.compareTo( BigDecimal.ZERO ) < 0 ) {
	value = arg.negate();
      } else {
	value = arg;
      }
    }
    else if( identifier.equals( "ACOS" )
	     || identifier.equals( "ARCCOS" ) )
    {
      value = new BigDecimal( Math.acos( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "ASIN" )
	     || identifier.equals( "ARCSIN" ) )
    {
      value = new BigDecimal( Math.asin( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "ATAN" )
	     || identifier.equals( "ARCTAN" ) )
    {
      value = new BigDecimal( Math.atan( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "COS" ) ) {
      value = new BigDecimal( Math.cos( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "COSH" ) ) {
      value = new BigDecimal( Math.cosh( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "EXP" ) ) {
      value = new BigDecimal( Math.exp( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "LOG" ) ) {
      value = new BigDecimal( Math.log( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "LOG10" ) ) {
      value = new BigDecimal(
			Math.log10( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "MAX" ) ) {
      checkToken( '(' );
      value = parseInclusiveORExpr();
      while( skipSpaces() == ',' ) {
	this.iter.next();
	BigDecimal tmpValue = parseInclusiveORExpr();
	if( tmpValue.compareTo( value ) > 0 ) {
	  value = tmpValue;
	}
      }
      checkToken( ')' );
    }
    else if( identifier.equals( "MIN" ) ) {
      checkToken( '(' );
      value = parseInclusiveORExpr();
      while( skipSpaces() == ',' ) {
	this.iter.next();
	BigDecimal tmpValue = parseInclusiveORExpr();
	if( tmpValue.compareTo( value ) < 0 ) {
	  value = tmpValue;
	}
      }
      checkToken( ')' );
    }
    else if( identifier.equals( "POW" ) ) {
      checkToken( '(' );
      BigDecimal a = parseInclusiveORExpr();
      checkToken( ',' );
      BigDecimal b = parseInclusiveORExpr();
      checkToken( ')' );
      try {
	if( b.scale() == 0 ) {
	  long v2 = b.longValue();
	  if( (v2 >= 0) && (v2 <= 999999999L) ) {
	    value = a.pow( (int) v2 );
	  }
	}
      }
      catch( ArithmeticException ex ) {}
      if( value == null ) {
	value = new BigDecimal(
			Math.pow( a.doubleValue(), b.doubleValue() ) );
      }
    }
    else if( identifier.equals( "RND" )
	     || identifier.equals( "RANDOM" ) )
    {
      checkToken( '(' );
      value = new BigDecimal( Math.random() );
      checkToken( ')' );
    }
    else if( identifier.equals( "ROUND" ) ) {
      BigDecimal v = parseFunctionArg();
      try {
	value = v.round( new MathContext(
				v.precision() - v.scale(),
				RoundingMode.HALF_EVEN ) );
      }
      catch( ArithmeticException ex ) {}
      if( value == null ) {
	value = new BigDecimal( Math.round( v.doubleValue() ) );
      }
    }
    else if( identifier.equals( "SIG" )
	     || identifier.equals( "SIGNUM" ) )
    {
      int v = parseFunctionArg().compareTo( BigDecimal.ZERO );
      if( v < 0 ) {
	value = new BigDecimal( -1 );
      } else if( v > 0 ) {
	value = new BigDecimal( 1 );
      } else {
	value = BigDecimal.ZERO;
      }
    }
    else if( identifier.equals( "SIN" ) ) {
      value = new BigDecimal( Math.sin( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "SINH" ) ) {
      value = new BigDecimal( Math.sinh( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "SQR" )
	     || identifier.equals( "SQRT" ) )
    {
      value = new BigDecimal( Math.sqrt( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "TAN" ) ) {
      value = new BigDecimal( Math.tan( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "TANH" ) ) {
      value = new BigDecimal( Math.tanh( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "TO_DEGREES" )
	     || identifier.equals( "TODEGREES" ) )
    {
      value = new BigDecimal(
		Math.toDegrees( parseFunctionArg().doubleValue() ) );
    }
    else if( identifier.equals( "TO_RADIANS" )
	     || identifier.equals( "TORADIANS" ) )
    {
      value = new BigDecimal(
		Math.toRadians( parseFunctionArg().doubleValue() ) );
    }
    if( value == null ) {
      fireError( "\'" + buf.toString() + "\': Unbekannter Bezeichner" );
    }
    return value;
  }


  private BigDecimal parseFunctionArg() throws ParseException
  {
    checkToken( '(' );
    BigDecimal value = parseInclusiveORExpr();
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


  private long toLong( BigDecimal value ) throws ParseException
  {
    checkInteger( value );
    return value.longValueExact();
  }
}
