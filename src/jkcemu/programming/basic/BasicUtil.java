/*
 * (c) 2008-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer BASIC-Compiler
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.text.CharacterIterator;
import jkcemu.programming.*;


public class BasicUtil
{
  public static String checkIdentifier( CharacterIterator iter )
  {
    String rv = null;
    char   ch = skipSpaces( iter );
    if( ((ch >= 'A') && (ch <= 'Z'))
	|| ((ch >= 'a') && (ch <= 'z')) )
    {
      int pos = iter.getIndex();
      ch      = iter.next();
      while( ((ch >= 'A') && (ch <= 'Z'))
	     || ((ch >= 'a') && (ch <= 'z'))
	     || ((ch >= '0') && (ch <= '9'))
	     || (ch == '_') )
      {
	ch = iter.next();
      }
      int len = iter.getIndex() - pos;
      if( ch == '$' ) {
	len++;
      }
      StringBuilder buf = new StringBuilder( len );
      iter.setIndex( pos );
      ch = iter.current();
      do {
	buf.append( Character.toUpperCase( ch ) );
	ch = iter.next();
	--len;
      } while( len > 0 );
      rv = buf.toString();
    }
    return rv;
  }


  public static boolean checkKeyword(
				CharacterIterator iter,
				String            keyword )
  {
    return checkKeyword( iter, keyword, true, false );
  }


  public static boolean checkKeyword(
				CharacterIterator iter,
				String            keyword,
				boolean           fmtSource,
				boolean           space )
  {
    boolean rv = true;
    char    ch     = skipSpaces( iter );
    int     begPos = iter.getIndex();
    int     len    = keyword.length();
    for( int i = 0; i < len; i++ ) {
      if( keyword.charAt( i ) != Character.toUpperCase( ch ) ) {
	iter.setIndex( begPos );
	rv = false;
	break;
      }
      ch = iter.next();
    }
    if( rv ) {
      if( ((ch >= 'A') && (ch <= 'Z'))
	  || ((ch >= 'a') && (ch <= 'z'))
	  || (ch == '$') )
      {
	rv = false;
      }
    }
    return rv;
  }


  public static String checkStringLiteral(
				BasicCompiler     compiler,
				CharacterIterator iter ) throws PrgException
  {
    StringBuilder buf       = null;
    char          delimiter = skipSpaces( iter );
    if( delimiter == '\"' ) {
      buf        = new StringBuilder( 64 );
      char    ch = iter.next();
      while( (ch != CharacterIterator.DONE) && (ch != delimiter) ) {
	if( ch > 0xFF ) {
	  throw new PrgException(
		String.format(
			"\'%c\': 16-Bit-Unicodezeichen nicht erlaubt",
			ch ) );
	}
	if( compiler.getBasicOptions().getWarnNonAsciiChars()
	    && ((ch < '\u0020') || (ch > '\u007F')) )
	{
	  compiler.putWarningNonAsciiChar( ch );
	}
	buf.append( ch );
	ch = iter.next();
      }
      if( ch != delimiter ) {
	compiler.putWarning( "String-Literal nicht geschlossen" );
      }
      iter.next();
    }
    return buf != null ? buf.toString() : null;
  }


  public static boolean checkToken( CharacterIterator iter, char ch )
  {
    boolean rv = false;
    if( skipSpaces( iter ) == ch ) {
      iter.next();
      rv = true;
    }
    return rv;
  }


  public static String convertCodeToValueInBC( String text )
  {
    return convertCodeToValueInRR( text, "BC" );
  }


  public static String convertCodeToValueInDE( String text )
  {
    return convertCodeToValueInRR( text, "DE" );
  }


  public static boolean isOnly_LD_HL_xx( String text )
  {
    boolean rv = false;
    if( text != null ) {
      int tabPos = text.indexOf( '\t' );
      if( tabPos > 0 ) {
	text = text.substring( tabPos );
      }
      if( text.startsWith( "\tLD\tHL," ) ) {
	if( text.indexOf( '\n' ) == (text.length() - 1) ) {
	  rv = true;
	}
      }
      else if( text.startsWith( "\tLD\tH," )
	       || text.startsWith( "\tLD\tL," ) )
      {
	int len = text.length();
	int eol = text.indexOf( '\n' );
	if( (eol > 0) && ((eol + 1) < len) ) {
	  String line1 = text.substring( 0, eol + 1 );
	  String line2 = text.substring( eol + 1 );
	  if( line2.indexOf( '\n' ) == (line2.length() - 1) ) {
	    if( line1.startsWith( "\tLD\tH,(IY" )
		&& line2.startsWith( "\tLD\tL,(IY" ) )
	    {
	      rv = true;
	    }
	    else if( line1.startsWith( "\tLD\tL,(IY" )
		     && line2.startsWith( "\tLD\tH,(IY" ) )
	    {
	      rv = true;
	    }
	  }
	}
      }
    }
    return rv;
  }


  /*
   * Die Methode prueft, ob der uebergebene Text
   * eine einzelne "LD_HL,..."-Anweisung ist.
   */
  public static boolean isSingleInst_LD_HL_xx( String instText )
  {
    boolean rv = false;
    if( instText != null ) {
      int tabPos = instText.indexOf( '\t' );
      if( tabPos > 0 ) {
	instText = instText.substring( tabPos );
      }
      if( instText.startsWith( "\tLD\tHL," ) ) {
	int pos = instText.indexOf( '\n' );
	if( pos >= 0 ) {
	  if( pos == (instText.length() - 1) ) {
	    rv = true;
	  }
	} else {
	  rv = true;
	}
      }
    }
    return rv;
  }


  public static void parseToken(
			CharacterIterator iter,
			char              ch ) throws PrgException
  {
    if( skipSpaces( iter ) != ch ) {
      throw new PrgException( String.format( "\'%c\' erwartet", ch ) );
    }
    iter.next();
  }


  public static int parseNumber( CharacterIterator iter )
						throws PrgException
  {
    Integer value = readNumber( iter );
    if( value == null ) {
      throwNumberExpected();
    }
    return value.intValue();
  }


  public static int parseUsrNum( CharacterIterator iter ) throws PrgException
  {
    Integer usrNum = readNumber( iter );
    if( usrNum == null ) {
      throw new PrgException( "Nummer der USR-Funktion erwartet" );
    }
    if( (usrNum.intValue() < 0) || (usrNum.intValue() > 9) ) {
      throw new PrgException(
		"Ung\u00FCltige USR-Funktionsnummer (0...9 erlaubt)" );
    }
    return usrNum.intValue();
  }


  public static Integer readHex( CharacterIterator iter )
						throws PrgException
  {
    Integer rv = null;
    char    ch = iter.current();
    if( ((ch >= '0') && (ch <= '9'))
	|| ((ch >= 'A') && (ch <= 'F'))
	|| ((ch >= 'a') && (ch <= 'f')) )
    {
      int value = 0;
      while( ((ch >= '0') && (ch <= '9'))
	     || ((ch >= 'A') && (ch <= 'F'))
	     || ((ch >= 'a') && (ch <= 'f')) )
      {
	value <<= 4;
	if( (ch >= '0') && (ch <= '9') ) {
	  value |= (ch - '0');
	}
	else if( (ch >= 'A') && (ch <= 'F') ) {
	  value |= (ch - 'A' + 10);
	}
	else if( (ch >= 'a') && (ch <= 'f') ) {
	  value |= (ch - 'a' + 10);
	}
	value &= 0xFFFF;
	ch = iter.next();
      }
      rv = new Integer( value );
    }
    return rv;
  }


  public static Integer readNumber( CharacterIterator iter )
						throws PrgException
  {
    Integer rv = null;
    char    ch = skipSpaces( iter );

    if( (ch >= '0') && (ch <= '9') ) {
      int value = ch - '0';
      ch        = iter.next();
      while( (ch >= '0') && (ch <= '9') ) {
	value = (value * 10) + (ch - '0');
	if( value > BasicCompiler.MAX_INT_VALUE ) {
	  throw new PrgException( "Zahl zu gro\u00DF" );
	}
	ch = iter.next();
      }
      rv = new Integer( value );
    }
    return rv;
  }


  /*
   * Wenn die letzte erzeugte Code-Zeile das Laden
   * des HL-Registers mit einem konstanten Wert darstellt,
   * wird die Code-Zeile geloescht und der Wert zurueckgeliefert.
   */
  public static Integer removeLastCodeIfConstExpr(
					BasicCompiler compiler,
					int           pos )
  {
    Integer    rv      = null;
    AsmCodeBuf codeBuf = compiler.getCodeBuf();
    if( codeBuf != null ) {
      String instText = codeBuf.substring( pos );
      int    tabPos   = instText.indexOf( '\t' );
      if( tabPos > 0 ) {
	pos      = tabPos;
	instText = instText.substring( tabPos );
      }
      if( instText.startsWith( "\tLD\tHL," )
	  && instText.endsWith( "H\n" ) )
      {
	try {
	  int v = Integer.parseInt(
			instText.substring( 7, instText.length() - 2 ),
			16 );
	  if( (v & 0x8000) != 0 ) {
	    // negativer Wert
	    v = -(0x10000 - (v & 0xFFFF));
	  }
	  rv = new Integer( v );
	}
	catch( NumberFormatException ex ) {}
      }
      if( rv != null ) {
	codeBuf.setLength( pos );
      }
    }
    return rv;
  }


  public static boolean replaceLastCodeFrom_LD_HL_To_BC(
						BasicCompiler compiler )
  {
    return replaceLastCodeFrom_LD_HL_To_RR( compiler, "BC" );
  }


  public static boolean replaceLastCodeFrom_LD_HL_To_DE(
						BasicCompiler compiler )
  {
    return replaceLastCodeFrom_LD_HL_To_RR( compiler, "DE" );
  }


  public static char skipSpaces( CharacterIterator iter )
  {
    char ch = iter.current();
    while( (ch != CharacterIterator.DONE) && PrgUtil.isWhitespace( ch ) ) {
      ch = iter.next();
    }
    return ch;
  }


  public static void throwHexDigitExpected() throws PrgException
  {
    throw new PrgException( "Hexadezimalziffer erwartet" );
  }


  public static void throwIndexOutOfRange() throws PrgException
  {
    throw new PrgException(
		"Index au\u00DFerhalb des g\u00FCltigen Bereichs" );
  }


  public static void throwNumberExpected() throws PrgException
  {
    throw new PrgException( "Zahl erwartet" );
  }


  public static void throwStringExprExpected() throws PrgException
  {
    throw new PrgException( "String-Ausdruck erwartet" );
  }


  public static void throwUnexpectedChar( char ch ) throws PrgException
  {
    if( ch == CharacterIterator.DONE ) {
      throw new PrgException( "Unerwartetes Ende der Zeile" );
    }
    StringBuilder buf = new StringBuilder( 32 );
    if( ch >= '\u0020' ) {
      buf.append( (char) '\'' );
      buf.append( ch );
      buf.append( "\': " );
    }
    buf.append( "Unerwartetes Zeichen" );
    throw new PrgException( buf.toString() );
  }


	/* --- private Methoden --- */

  private static String convertCodeToValueInRR( String text, String rr )
  {
    String rv = null;
    if( (text != null) && (rr.length() == 2) ) {
      String label  = "";
      int    tabPos = text.indexOf( '\t' );
      if( tabPos > 0 ) {
	label = text.substring( 0, tabPos );
	text  = text.substring( tabPos );
      }
      if( text.startsWith( "\tLD\tHL," ) ) {
	if( text.indexOf( '\n' ) == (text.length() - 1) ) {
	  rv = String.format(
			"%s\tLD\t%s%s",
			label,
			rr,
			text.substring( 6 ) );
	}
      }
      else if( text.startsWith( "\tLD\tH," )
	       || text.startsWith( "\tLD\tL," ) )
      {
	int len = text.length();
	int eol = text.indexOf( '\n' );
	if( (eol > 0) && ((eol + 1) < len) ) {
	  String line1 = text.substring( 0, eol + 1 );
	  String line2 = text.substring( eol + 1 );
	  if( line2.indexOf( '\n' ) == (line2.length() - 1) ) {
	    if( line1.startsWith( "\tLD\tH,(IY" )
		&& line2.startsWith( "\tLD\tL,(IY" ) )
	    {
	      rv = String.format(
			"%s\tLD\t%c%s\tLD\t%c%s",
			label,
			rr.charAt( 0 ),
			line1.substring( 5 ),
			rr.charAt( 1 ),
			line2.substring( 5 ) );
	    }
	    else if( line1.startsWith( "\tLD\tL,(IY" )
		     && line2.startsWith( "\tLD\tH,(IY" ) )
	    {
	      rv = String.format(
			"%s\tLD\t%c%s\tLD\t%c%s",
			label,
			rr.charAt( 1 ),
			line1.substring( 5 ),
			rr.charAt( 0 ),
			line2.substring( 5 ) );
	    }
	  }
	}
      }
    }
    return rv;
  }


  private static boolean replaceLastCodeFrom_LD_HL_To_RR(
					BasicCompiler compiler,
					String     rr )
  {
    boolean rv = false;
    AsmCodeBuf codeBuf = compiler.getCodeBuf();
    if( codeBuf != null ) {
      int    pos      = codeBuf.getLastLinePos();
      String instText = codeBuf.substring( pos );
      int    tabPos   = instText.indexOf( '\t' );
      if( tabPos > 0 ) {
	pos      = tabPos;
	instText = instText.substring( tabPos );
      }
      if( instText.startsWith( "\tLD\tHL," ) ) {
	codeBuf.replace( pos + 4, pos + 6, rr );
	rv = true;
      } else {
	// Zugriff auf lokale Variable?
	if( (rr.length() == 2)
	    && (instText.startsWith( "\tLD\tH," )
		|| instText.startsWith( "\tLD\tL," )) )
	{
	  String line2 = codeBuf.cut( pos );
	  String line1 = codeBuf.cut( codeBuf.getLastLinePos() );
	  if( (line1.indexOf( "\tLD\tH,(IY" ) >= 0)
	      && (line2.indexOf( "\tLD\tL,(IY" ) >= 0) )
	  {
	    line1 = line1.replace(
			"\tLD\tH,(IY",
			String.format( "\tLD\t%c,(IY", rr.charAt( 0 ) ) );
	    line2 = line2.replace(
			"\tLD\tL,(IY",
			String.format( "\tLD\t%c,(IY", rr.charAt( 1 ) ) );
	    rv = true;
	  }
	  else if( (line1.indexOf( "\tLD\tL,(IY" ) >= 0)
		   && (line2.indexOf( "\tLD\tH,(IY" ) >= 0) )
	  {
	    line1 = line1.replace(
			"\tLD\tL,(IY",
			String.format( "\tLD\t%c,(IY", rr.charAt( 1 ) ) );
	    line2 = line2.replace(
			"\tLD\tH,(IY",
			String.format( "\tLD\t%c,(IY", rr.charAt( 0 ) ) );
	    rv = true;
	  }
	  codeBuf.append( line1 );
	  codeBuf.append( line2 );
	}
      }
    }
    return rv;
  }
}
