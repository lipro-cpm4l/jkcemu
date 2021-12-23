/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer BASIC-Compiler
 */

package jkcemu.programming.basic;

import java.text.CharacterIterator;
import jkcemu.programming.PrgException;
import jkcemu.programming.PrgUtil;


public class BasicUtil
{
  public static void appendSetError(
			BasicCompiler compiler,
			int           errCode,
			String        errTextDE,
			String        errTextEN )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERROR_NUM ) ) {
      buf.append_LD_HL_nn( errCode );
      buf.append( "\tLD\t(M_ERROR_NUM),HL\n" );
    }
    if( compiler.usesLibItem( BasicLibrary.LibItem.M_ERROR_TEXT ) ) {
      buf.append_LD_HL_xx(
	compiler.getStringLiteralLabel(
		compiler.isLangCode( "DE" ) ? errTextDE : errTextEN ) );
      buf.append( "\tLD\t(M_ERROR_TEXT),HL\n" );
    }
  }


  public static void appendSetError( BasicCompiler compiler )
  {
    appendSetError( compiler, BasicLibrary.E_ERROR, "Fehler", "Error" );
  }


  public static void appendSetErrorChannelAlreadyOpen( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_CHANNEL_ALREADY_OPEN,
		"Kanal bereits geoeffnet",
		"Channel already open" );
  }


  public static void appendSetErrorChannelClosed( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_CHANNEL_CLOSED,
		"Kanal geschlossen",
		"Channel closed" );
  }


  public static void appendSetErrorDirFull( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_DIR_FULL,
		"Directory voll",
		"Directory full" );
  }


  public static void appendSetErrorDiskFull( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_DISK_FULL,
		"Speichermedium voll",
		"Disk full" );
  }


  public static void appendSetErrorDeviceLocked( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_DEVICE_LOCKED,
		"Geraet bereits in Benutzung",
		"Device locked" );
  }


  public static void appendSetErrorDeviceNotFound( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_DEVICE_NOT_FOUND,
		"Geraet nicht gefunden",
		"Device not found" );
  }


  public static void appendSetErrorFileNotFound( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_FILE_NOT_FOUND,
		"Datei nicht gefunden",
		"File not found" );
  }


  public static void appendSetErrorReadOnly( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_READ_ONLY,
		"Datei oder Medium schreibgeschuetzt",
		"File or media write protected" );
  }


  public static void appendSetErrorHardware( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_HARDWARE,
		"Hardware-Fehler",
		"Hardware error" );
  }


  public static void appendSetErrorIOError( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_IO_ERROR,
		"Ein-/Ausgabefehler",
		"IO error" );
  }


  public static void appendSetErrorInvalidChars( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_INVALID,
		"Ungueltige Zeichen",
		"Invalid characters" );
  }


  public static void appendSetErrorInvalidFileName( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_INVALID,
		"Ungueltiger Dateiname",
		"Invalid filename" );
  }


  public static void appendSetErrorIOMode( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_IO_MODE,
		"Ungueltige Betriebsart",
		"Invalid IO mode" );
  }


  public static void appendSetErrorNoDisk( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_NO_DISK,
		"Kein Speichermedium vorhanden",
		"No disk" );
  }


  public static void appendSetErrorMediaChanged( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_MEDIA_CHANGED,
		"Medium gewechselt",
		"Media changed" );
  }


  public static void appendSetErrorNumericOverflow( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_OVERFLOW,
		"Numerischer Ueberlauf",
		"Numeric overflow" );
  }


  public static void appendSetErrorDigitsTruncated( BasicCompiler compiler )
  {
    appendSetError(
		compiler,
		BasicLibrary.E_DIGITS_TRUNCATED,
		"Ziffern abgeschnitten",
		"Digits truncated" );
  }


  public static void check8BitChar( char ch ) throws PrgException
  {
    if( (ch == 0) || (ch > 0xFF) ) {
      throw new PrgException( "Zeichen \'" + ch
		+ "\' au\u00DFerhalb des 8-Bit-Wertebereichs" );
    }
  }


  public static boolean checkComma( CharacterIterator iter )
  {
    return checkToken( iter, ',' );
  }


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
      if( ch == BasicCompiler.TYPE_STRING_SUFFIX ) {
	iter.next();
      }
      int           len = iter.getIndex() - pos;
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
    boolean rv     = false;
    int     begPos = iter.getIndex();
    String  text   = checkIdentifier( iter );
    if( text != null ) {
      rv = text.equalsIgnoreCase( keyword );
    }
    if( !rv ) {
      iter.setIndex( begPos );
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


  public static boolean checkToken( CharacterIterator iter, char token )
  {
    boolean rv = false;
    if( skipSpaces( iter ) == token ) {
      iter.next();
      rv = true;
    }
    return rv;
  }


  public static boolean checkToken( CharacterIterator iter, String token )
  {
    boolean rv     = true;
    char    ch     = skipSpaces( iter );
    int     begPos = iter.getIndex();
    int     len    = token.length();
    for( int i = 0; i < len; i++ ) {
      if( token.charAt( i ) != ch ) {
	iter.setIndex( begPos );
	rv = false;
	break;
      }
      ch = iter.next();
    }
    return rv;
  }


  public static String convertCodeToValueInA( String text )
  {
    String rv = null;
    if( text != null ) {
      int eol = text.indexOf( '\n' );
      if( eol > 0 ) {
	String label  = "";
	int    tabPos = text.indexOf( '\t' );
	if( tabPos > 0 ) {
	  label = text.substring( 0, tabPos );
	  text  = text.substring( tabPos );
	}
	int len = text.length();
	if( text.startsWith( "\tLD\tHL,(" ) ) {
	  if( eol == (len - 1) ) {
	    rv = String.format(
			"%s\tLD\tA%s",
			label,
			text.substring( 6 ) );
	  }
	}
	else if( text.startsWith( "\tLD\tHL," ) ) {
	  if( text.endsWith( "H\n" ) ) {
	    try {
	      int v = Integer.parseInt(
				text.substring( 7, len - 2 ),
				16 ) & 0xFF;
	      if( v == 0 ) {
		rv = String.format( "%s\tXOR\tA\n", label );
	      } else if( v >= 0xA0 ) {
		rv = String.format( "%s\tLD\tA,0%02XH\n", label, v );
	      } else {
		rv = String.format( "%s\tLD\tA,%02XH\n", label, v );
	      }
	    }
	    catch( NumberFormatException ex ) {}
	  }
	}
	else if( text.startsWith( "\tLD\tH," )
		 || text.startsWith( "\tLD\tL," ) )
	{
	  if( (eol + 1) < len ) {
	    String line1 = text.substring( 0, eol + 1 );
	    String line2 = text.substring( eol + 1 );
	    if( line2.indexOf( '\n' ) == (line2.length() - 1) ) {
	      String line = null;
	      if( line2.startsWith( "\tLD\tL,(IY" ) ) {
		line = line2;
	      } else if( line1.startsWith( "\tLD\tL,(IY" ) ) {
		line = line1;
	      }
	      if( line != null ) {
		rv = String.format(
			"%s\tLD\t%A%s",
			label,
			line.substring( 5 ) );
	      }
	    }
	  }
	}
      }
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


  /*
   * Die Methode prueft, ob der zuletzt erzeugte Programmcode
   * das Laden von M_ACCU mit einem DECIMAL-Wert ist.
   * Wenn ja, wird versucht, diesen Code in das Laden
   * M_OP1 von umzuwandeln.
   * In dem Fall wird der alte Programmcode entfernt
   * und der neue zurueckgeliefert, aber nicht angehaengt.
   */
  public static String convertLastCodeToD6LoadOp1( BasicCompiler compiler )
  {
    AsmCodeBuf asmOut    = compiler.getCodeBuf();
    String     ldOp1Code = null;
    String     lastLine  = asmOut.cutLastLine();
    if( lastLine.equals( "\tCALL\tD6_LD_ACCU_MEM\n" ) ) {
      String preLine = asmOut.cutLastLine();
      if( BasicUtil.isOnly_LD_HL_xx( preLine ) ) {
	ldOp1Code = preLine + "\tCALL\tD6_LD_OP1_MEM\n";
	compiler.addLibItem( BasicLibrary.LibItem.D6_LD_OP1_MEM );
      } else {
	asmOut.append( preLine );
      }
    } else if( lastLine.startsWith( "\tDB\t" ) ) {
      if( asmOut.cutIfEndsWith( "\tCALL\tD6_LD_ACCU_NNNNNN\n" ) ) {
	ldOp1Code = "\tCALL\tD6_LD_OP1_NNNNNN\n" + lastLine;
	compiler.addLibItem( BasicLibrary.LibItem.D6_LD_OP1_NNNNNN );
      }
    }
    if( ldOp1Code == null ) {
      asmOut.append( lastLine );
    }
    return ldOp1Code;
  }


  public static boolean endsWithStringSuffix( String text )
  {
    boolean rv = false;
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	if( text.charAt( len - 1 ) == BasicCompiler.TYPE_STRING_SUFFIX ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


  public static void ensureNumericType( BasicCompiler.DataType dataType )
							throws PrgException
  {
    boolean ok = false;
    if( dataType != null ) {
      ok = (dataType.equals( BasicCompiler.DataType.DEC6 )
		|| dataType.equals( BasicCompiler.DataType.INT2 )
		|| dataType.equals( BasicCompiler.DataType.INT4 ));
    }
    if( !ok ) {
      throwNumericExprExpected();
    }
  }


  public static int getDataTypeSize( BasicCompiler.DataType dataType )
  {
    int rv = 2;
    switch( dataType ) {
      case INT4:
	rv = 4;
	break;
      case DEC6:
	rv = 6;
	break;
    }
    return rv;
  }


  public static BasicCompiler.DataType getDefaultTypeBySuffix( String name )
  {
    return BasicUtil.endsWithStringSuffix( name ) ?
				BasicCompiler.DataType.STRING
				: BasicCompiler.DataType.INT2;
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


  public static int parseInt2Number( CharacterIterator iter )
						throws PrgException
  {
    Number value = readNumber( iter );
    if( value == null ) {
      throwNumberExpected();
    }
    if( !(value instanceof Integer) ) {
      throw new PrgException( "Integer-Zahl erwartet" );
    }
    return value.intValue();
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


  public static BasicCompiler.DataType parseTypeDecl(
				String            name,
				CharacterIterator iter ) throws PrgException
  {
    BasicCompiler.DataType rv  = null;
    boolean                str = endsWithStringSuffix( name );
    if( BasicUtil.checkKeyword( iter, "AS" ) ) {
      String typeName = checkIdentifier( iter );
      if( typeName == null ) {
	throw new PrgException( "INTEGER, LONG oder STRING erwartet" );
      }
      switch( typeName ) {
	case "DECIMAL":
	  if( str ) {
	    throw new PrgException(
		String.format(
			"Bezeichner f\u00FCr eine Decimal-Variable/Funktion"
				+ " darf nicht auf '%c' enden",
			BasicCompiler.TYPE_STRING_SUFFIX ) );
	  }
	  rv = BasicCompiler.DataType.DEC6;
	  break;
	case "INTEGER":
	  if( str ) {
	    throw new PrgException(
		String.format(
			"Bezeichner f\u00FCr eine Integer-Variable/Funktion"
				+ " darf nicht auf '%c' enden",
			BasicCompiler.TYPE_STRING_SUFFIX ) );
	  }
	  rv = BasicCompiler.DataType.INT2;
	  break;
	case "LONG":
	  if( str ) {
	    throw new PrgException(
		String.format(
			"Bezeichner f\u00FCr eine Long-Variable/Funktion"
				+ " darf nicht auf '%c' enden",
			BasicCompiler.TYPE_STRING_SUFFIX ) );
	  }
	  rv = BasicCompiler.DataType.INT4;
	  break;
	case "STRING":
	  if( !str ) {
	    throw new PrgException(
		String.format(
			"Bezeichner f\u00FCr eine String-Variable/Funktion"
				+ " muss auf '%c' enden",
			BasicCompiler.TYPE_STRING_SUFFIX ) );
	  }
	  rv = BasicCompiler.DataType.STRING;
	  break;
	default:
	  throw new PrgException( "\'" + typeName
			+ "\': Ung\u00FCltige Typbezeichnung" );
      }
    }
    if( rv == null ) {
      rv = (str ? BasicCompiler.DataType.STRING
			: BasicCompiler.DataType.INT2);
    }
    return rv;
  }


  public static int parseUsrNum( CharacterIterator iter ) throws PrgException
  {
    Number usrNum = readNumber( iter );
    if( usrNum == null ) {
      throw new PrgException( "Nummer der USR-Funktion erwartet" );
    }
    if( !(usrNum instanceof Integer)
	|| (usrNum.intValue() < 0) || (usrNum.intValue() > 9) )
    {
      throw new PrgException(
		"Ung\u00FCltige USR-Funktionsnummer (0...9 erlaubt)" );
    }
    return usrNum.intValue();
  }


  public static Number readHex( CharacterIterator iter )
						throws PrgException
  {
    Number rv = null;
    char   ch = iter.current();
    if( ((ch >= '0') && (ch <= '9'))
	|| ((ch >= 'A') && (ch <= 'F'))
	|| ((ch >= 'a') && (ch <= 'f')) )
    {
      long value = 0;
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
	if( value > 0xFFFFFFFFL ) {
	  throwNumberTooBig();
	}
	ch = iter.next();
      }
      if( value > 0xFFFF ) {
	rv = Long.valueOf( value );
      } else {
	rv = Integer.valueOf( (int) value );
      }
    }
    return rv;
  }


  public static Number readNumber( CharacterIterator iter )
						throws PrgException
  {
    Number rv = null;
    char   ch = skipSpaces( iter );
    if( (ch >= '0') && (ch <= '9') ) {
      long value = ch - '0';
      ch         = iter.next();
      while( (ch >= '0') && (ch <= '9') ) {
	value = (value * 10) + (ch - '0');
	if( value > BasicCompiler.MAX_LONG_VALUE ) {
	  throwNumberTooBig();
	}
	ch = iter.next();
      }
      if( value > 0x7FFF ) {
	rv = Long.valueOf( value );
      } else {
	rv = Integer.valueOf( (int) value );
      }
    }
    return rv;
  }


  /*
   * Wenn die letzte erzeugte Code-Zeile das Laden
   * des HL-Registers mit einem konstanten Wert darstellt,
   * wird die Code-Zeile geloescht und der Wert zurueckgeliefert.
   */
  public static Integer removeLastCodeIfConstExpr(
					AsmCodeBuf codeBuf,
					int        pos )
  {
    Integer rv = null;
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
	  rv = v;
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


  public static void throwBasicLineExprExpected() throws PrgException
  {
    throw new PrgException( "BASIC-Zeilennummer oder Marke erwartet" );
  }


  public static void throwDataTypeMismatch() throws PrgException
  {
    throw new PrgException( "Falscher Datentyp" );
  }


  public static void throwDimTooSmall() throws PrgException
  {
    throw new PrgException( "Dimension zu klein" );
  }


  public static void throwDivisionByZero() throws PrgException
  {
    throw new PrgException( "Division durch 0" );
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


  public static void throwInt2ExprExpected() throws PrgException
  {
    throw new PrgException( "Integer-Ausdruck erwartet" );
  }


  public static void throwIntOrLongExprExpected() throws PrgException
  {
    throw new PrgException( "Integer- oder Long-Ausdruck erwartet" );
  }


  public static void throwIntOrLongVarExpected() throws PrgException
  {
    throw new PrgException( "Integer- oder Long-Ausdruck erwartet" );
  }


  public static void throwInvalidElemSize( int elemSize ) throws PrgException
  {
    throw new PrgException(
		String.format(
			"Ung\u00FCltige Elementgr\u00F6\u00DFe: %d",
			elemSize ) );
  }


  public static void throwIOChannelNumOutOfRange() throws PrgException
  {
    throw new PrgException(
		"Kanalnummer au\u00DFerhalb des g\u00FCltigen Bereichs" );
  }


  public static void throwNoConstExpr() throws PrgException
  {
    throw new PrgException( "Kein konstanter Ausdruck" );
  }


  public static void throwNumberExpected() throws PrgException
  {
    throw new PrgException( "Zahl erwartet" );
  }


  public static void throwNumberTooBig() throws PrgException
  {
    throw new PrgException( "Zahl zu gro\u00DF" );
  }


  public static void throwNumericExprExpected() throws PrgException
  {
    throw new PrgException( "Numerischer Ausdruck erwartet" );
  }


  public static void throwOp1DataTypeNotAllowed() throws PrgException
  {
    throw new PrgException( "Operation auf der linken Seite"
			+ " mit diesem Datentyp nicht erlaubt" );
  }


  public static void throwOp2DataTypeNotAllowed() throws PrgException
  {
    throw new PrgException( "Operation auf der rechten Seite"
			+ " mit diesem Datentyp nicht erlaubt" );
  }


  public static void throwStringExprExpected() throws PrgException
  {
    throw new PrgException( "String-Ausdruck erwartet" );
  }


  public static void throwStringLitOrVarExpected() throws PrgException
  {
    throw new PrgException( "String-Literal oder String-Variable erwartet" );
  }


  public static void throwStringVarExpected() throws PrgException
  {
    throw new PrgException( "String-Variable erwartet" );
  }


  public static void throwUnexpectedChar( char ch ) throws PrgException
  {
    if( ch == CharacterIterator.DONE ) {
      throw new PrgException( "Unerwartetes Ende der Zeile" );
    }
    StringBuilder buf = new StringBuilder( 32 );
    if( ch >= '\u0020' ) {
      buf.append( '\'' );
      buf.append( ch );
      buf.append( "\': " );
    }
    buf.append( "Unerwartetes Zeichen" );
    throw new PrgException( buf.toString() );
  }


  public static void throwUnknownIdentifier( String name ) throws PrgException
  {
    throw new PrgException( name + ": Unbekannter Bezeichner" );
  }


  public static void throwVarExpected() throws PrgException
  {
    throw new PrgException( "Variable erwartet" );
  }


  public static void throwVarNameExpected() throws PrgException
  {
    throw new PrgException( "Name einer Variable erwartet" );
  }


  /*
   * Die Methode testet, ob in dem uebergebenen Assemblercode
   * der zweite CPU-Registersatz (EXX-Befehl) verwendet wird.
   * Wenn CALL-Befehle enthalten sind, wird davaon ausgegangen,
   * dass in den aufgerufenen Routinen
   * der zweite Registersatz verwendet wird.
   */
  public static boolean usesSecondCpuRegSet( String asmCode )
  {
    return (asmCode.indexOf( "\tEXX\n" ) >= 0)
	   || (asmCode.indexOf( "\tCALL\t" ) >= 0);
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
      int len = text.length();
      int eol = text.indexOf( '\n' );
      if( text.startsWith( "\tLD\tHL," ) ) {
	if( eol == (len - 1) ) {
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
