/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Parser fuer einen Ausdruck
 */

package jkcemu.programming.assembler;

import java.lang.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Map;
import jkcemu.programming.*;


public class ExprParser
{
  private static String[] sortedReservedWords = {
	"AND", "EQ", "GE", "GT", "HIGH", "LE", "LOW", "LT",
	"MOD", "NE", "NOT", "OR", "SHL", "SHR", "XOR" };

  private CharacterIterator    iter;
  private int                  instBegAddr;
  private Map<String,AsmLabel> labels;
  private boolean              checkLabels;
  private boolean              labelsCaseSensitive;


  public static boolean isReservedWord( String text )
  {
    return (Arrays.binarySearch( sortedReservedWords, text ) >= 0);
  }


  public static Integer parse(
			String               text,
			int                  instBegAddr,
			Map<String,AsmLabel> labels,
			boolean              checkLabels,
			boolean              labelsCaseSensitive )
							throws PrgException
  {
    return (new ExprParser(
			new StringCharacterIterator( text ),
			instBegAddr,
			labels,
			checkLabels,
			labelsCaseSensitive )).parse();
  }


  public static int parseNumber( CharacterIterator iter ) throws PrgException
  {
    int           value       = 0;
    StringBuilder buf         = new StringBuilder();
    boolean       enclosedHex = checkAndParseToken( iter, "X\'" );
    boolean       simpleHex   = checkAndParseToken( iter, "%" );
    char          ch          = skipSpaces( iter );
    while( ((ch >= '0') && (ch <= '9'))
	   || ((ch >= 'A') && (ch <= 'F'))
	   || ((ch >= 'a') && (ch <= 'f')) )
    {
      buf.append( (char) ch );
      ch = iter.next();
    }
    if( enclosedHex || simpleHex || (ch == 'H') || (ch == 'h') ) {
      if( enclosedHex && (ch != '\'') ) {
	throw new PrgException(
		"\' als Ende der Hexadezimalzahl erwartet" );
      }
      if( !simpleHex ) {
	ch = iter.next();
      }
      try {
	value = Integer.parseInt( buf.toString(), 16 );
      }
      catch( NumberFormatException ex ) {
	throw new PrgException(
			buf.toString() + ": Ung\u00FCltige Hexadezimalzahl" );
      }
    }
    else if( (ch == 'O') || (ch == 'o') || (ch == 'Q') || (ch == 'q') ) {
      ch = iter.next();
      try {
	value = Integer.parseInt( buf.toString(), 8 );
      }
      catch( NumberFormatException ex ) {
	throw new PrgException(
			buf.toString() + ": Ung\u00FCltige Oktalzahl" );
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


  public static char skipSpaces( CharacterIterator iter )
  {
    char ch = iter.current();
    while( (ch != CharacterIterator.DONE)
	   && PrgUtil.isWhitespace( ch ) )
    {
      ch = iter.next();
    }
    return ch;
  }


	/* --- Konstruktor --- */

  private ExprParser(
		CharacterIterator    iter,
		int                  instBegAddr,
		Map<String,AsmLabel> labels,
		boolean              checkLabels,
		boolean              labelsCaseSensitive )
  {
    this.iter                = iter;
    this.instBegAddr         = instBegAddr;
    this.labels              = labels;
    this.checkLabels         = checkLabels;
    this.labelsCaseSensitive = labelsCaseSensitive;
  }


	/* --- private Methoden --- */

  private boolean checkAndParseToken( String token )
  {
    return checkAndParseToken( this.iter, token );
  }


  private static boolean checkAndParseToken(
				CharacterIterator iter,
				String            token )
  {
    boolean rv  = true;
    int     len = token.length();
    if( len > 0 ) {
      char chSrc   = skipSpaces( iter );
      char chToken = CharacterIterator.DONE;
      int  idx     = iter.getIndex();
      for( int i = 0; i < len; i++ ) {
	chToken = token.charAt( i );
	if( Character.toUpperCase( chSrc )
			!= Character.toUpperCase( chToken ) )
	{
	  rv = false;
	  break;
	}
	chSrc = iter.next();
      }
      /*
       * Wenn das letzte Zeichen im Token ein Zeichen
       * fuer einen Bezeichner war,
       * darf das naechste Zeichen im Eingabestrom kein Zeichen
       * eines Bezeichners sein.
       */
      if( rv && AsmLabel.isIdentifierPart( chToken ) ) {
	if( AsmLabel.isIdentifierPart( chSrc ) ) {
	  rv = false;
	}
      }
      /*
       * Wenn das letzte Zeichen im Token eine spitze Klammer war
       * darf das naechste Zeichen im Eingabestrom keine spitze Klammer
       * und auch kein Gleichheitszeichen sein.
       */
      if( rv && ((chToken == '<') || (chToken == '>')) ) {
	char ch = iter.current();
	if( (ch == '<') || (ch == '>') || (ch == '=') ) {
	  rv = false;
	}
      }
      if( !rv ) {
	iter.setIndex( idx );
      }
    }
    return rv;
  }


  private Integer parse() throws PrgException
  {
    Integer value = parseExpr();
    char    ch    = skipSpaces();
    if( ch != CharacterIterator.DONE ) {
      throw new PrgException( "\'" + ch
			+ "\': Unerwartetes Zeichen hinter Ausdruck" );
    }
    return value;
  }


  private Integer parseExpr() throws PrgException
  {
    Integer value = parseXorExpr();
    for(;;) {
      if( checkAndParseToken( "OR" ) ) {
	Integer v2 = parseXorExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() | v2.intValue());
	}
      } else {
	break;
      }
    }
    return value;
  }


  private Integer parseXorExpr() throws PrgException
  {
    Integer value = parseAndExpr();
    for(;;) {
      if( checkAndParseToken( "XOR" ) ) {
	Integer v2 = parseAndExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() ^ v2.intValue());
	}
      } else {
	break;
      }
    }
    return value;
  }


  private Integer parseAndExpr() throws PrgException
  {
    Integer value = parseEqualityExpr();
    for(;;) {
      if( checkAndParseToken( "AND" ) ) {
	Integer v2 = parseEqualityExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() & v2.intValue());
	}
      } else {
	break;
      }
    }
    return value;
  }


  private Integer parseEqualityExpr() throws PrgException
  {
    Integer value = parseRelationalExpr();
    for(;;) {
      if( checkAndParseToken( "=" ) || checkAndParseToken( "EQ" ) ) {
	Integer v2 = parseRelationalExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() == v2.intValue() ? -1 : 0);
	}
      }
      else if( checkAndParseToken( "<>" ) || checkAndParseToken( "NE" ) ) {
	Integer v2 = parseRelationalExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() != v2.intValue() ? 0 : -1);
	}
      } else {
	break;
      }
    }
    return value;
  }


  private Integer parseRelationalExpr() throws PrgException
  {
    Integer value = parseShiftExpr();
    for(;;) {
      if( checkAndParseToken( "<=" ) || checkAndParseToken( "LE" ) ) {
	Integer v2 = parseShiftExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() <= v2.intValue() ? -1 : 0);
	}
      }
      else if( checkAndParseToken( "<" ) || checkAndParseToken( "LT" ) ) {
	Integer v2 = parseShiftExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() < v2.intValue() ? -1 : 0);
	}
      }
      else if( checkAndParseToken( ">=" ) || checkAndParseToken( "GE" ) ) {
	Integer v2 = parseShiftExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() >= v2.intValue() ? -1 : 0);
	}
      }
      else if( checkAndParseToken( ">" ) || checkAndParseToken( "GT" ) ) {
	Integer v2 = parseShiftExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() > v2.intValue() ? -1 : 0);
	}
      } else {
	break;
      }
    }
    return value;
  }


  private Integer parseShiftExpr() throws PrgException
  {
    Integer value = parseAddExpr();
    for(;;) {
      if( checkAndParseToken( "<<" ) || checkAndParseToken( "SHL" ) ) {
	Integer v2 = parseAddExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() << v2.intValue());
	}
      }
      else if( checkAndParseToken( ">>" ) || checkAndParseToken( "SHR" ) ) {
	Integer v2 = parseAddExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() >> v2.intValue());
	}
      } else {
	break;
      }
    }
    return value;
  }


  private Integer parseAddExpr() throws PrgException
  {
    Integer value = parseMulExpr();
    for(;;) {
      if( checkAndParseToken( "+" ) ) {
	Integer v2 = parseMulExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() + v2.intValue());
	}
      } else if( checkAndParseToken( "-" ) ) {
	Integer v2 = parseMulExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() - v2.intValue());
	}
      } else {
	break;
      }
    }
    return value;
  }


  private Integer parseMulExpr() throws PrgException
  {
    Integer value = parseUnaryExpr();
    for(;;) {
      if( checkAndParseToken( "*" ) ) {
	Integer v2 = parseUnaryExpr();
	if( (value != null) && (v2 != null) ) {
	  value = (value.intValue() * v2.intValue());
	}
      }
      else if( checkAndParseToken( "/" ) ) {
	Integer v2 = parseUnaryExpr();
	if( (value != null) && (v2 != null) ) {
	  if( v2.intValue() == 0 ) {
	    throw new PrgException( "Division durch 0" );
	  }
	  value = (value.intValue() / v2.intValue());
	}


	value /= parseUnaryExpr();
      }
      else if( checkAndParseToken( "MOD" ) ) {
	Integer v2 = parseUnaryExpr();
	if( (value != null) && (v2 != null) ) {
	  if( v2.intValue() == 0 ) {
	    throw new PrgException( "Modulo 0" );
	  }
	  value = (value.intValue() % v2.intValue());
	}
      } else {
	break;
      }
    }
    return value;
  }


  private Integer parseUnaryExpr() throws PrgException
  {
    Integer value = null;
    if( checkAndParseToken( "+" ) ) {
      value = parsePrimExpr();
    } else if( checkAndParseToken( "-" ) ) {
      Integer v2 = parsePrimExpr();
      if( v2 != null ) {
	value = -v2.intValue();
      }
    } else if( checkAndParseToken( "LOW" ) ) {
      parseToken( '(' );
      Integer v2 = parseExpr();
      if( v2 != null ) {
	value = (v2.intValue() & 0xFF);
      }
      parseToken( ')' );
    }
    else if( checkAndParseToken( "HIGH" ) ) {
      parseToken( '(' );
      Integer v2 = parseExpr();
      if( v2 != null ) {
	value = ((v2.intValue() >> 8) & 0xFF);
      }
      parseToken( ')' );
    }
    else if( checkAndParseToken( "NOT" ) ) {
      Integer v2 = parseExpr();
      if( v2 != null ) {
	value = ~v2.intValue();
      }
      parseToken( ')' );
    }
    else if( checkAndParseToken( "(" ) ) {
      value = parseExpr();
      parseToken( ')' );
    } else {
      value = parsePrimExpr();
    }
    return value;
  }


  private Integer parsePrimExpr() throws PrgException
  {
    Integer value = null;
    if( checkAndParseToken( "$" ) ) {
      value = this.instBegAddr;
    } else if( checkAndParseToken( "\'" ) ) {
      char ch = this.iter.current();
      if( ch == CharacterIterator.DONE ) {
	throw new PrgException( "Unerwartetes Ende des Zeichenliterals" );
      }
      value = Integer.valueOf( ch );
      ch    = this.iter.next();
      if( ch != '\'' ) {
	throw new PrgException(
		"\' als Ende des Zeichenliterals erwartet" );
      }
      this.iter.next();
    } else {
      boolean isNum = false;
      char    ch    = skipSpaces();
      if( (ch == 'X') || (ch == 'x') ) {
	isNum = (iter.next() == '\'');
	iter.previous();
      }
      if( isNum || (ch == '%') || ((ch >= '0') && (ch <= '9')) ) {
	value = parseNumber( this.iter );
      }
      else if( AsmLabel.isIdentifierStart( ch ) ) {
	value = parseLabel();
      } else {
	throw new PrgException(
		"\'" + ch + "\': Ung\u00FCltiges Zeichen im Argument" );
      }
    }
    return value;
  }


  private Integer parseLabel() throws PrgException
  {
    Integer       value = null;
    StringBuilder buf   = null;
    if( this.labels != null ) {
      buf = new StringBuilder();
    }
    char ch = skipSpaces();
    while( AsmLabel.isIdentifierPart( ch ) ) {
      if( buf != null ) {
	if( this.labelsCaseSensitive ) {
	  buf.append( ch );
	} else {
	  buf.append( Character.toUpperCase( ch ) );
	}
      }
      ch = this.iter.next();
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
	Object o = label.getLabelValue();
	if( o != null ) {
	  if( o instanceof Integer ) {
	    value = (Integer) o;
	  }
	}
      } else {
	if( this.checkLabels ) {
	  throw new PrgException( buf.toString() + ": Unbekannte Marke" );
	}
      }
    }
    return value;
  }


  private void parseToken( char token ) throws PrgException
  {
    if( skipSpaces() != token ) {
      throw new PrgException( "\'" + token + "\' erwartet" );
    }
    iter.next();
  }


  private char skipSpaces()
  {
    return skipSpaces( this.iter );
  }
}
