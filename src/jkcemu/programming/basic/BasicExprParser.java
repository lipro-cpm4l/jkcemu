/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Parser fuer BASIC-Ausdruecke
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.text.CharacterIterator;
import java.util.Arrays;
import jkcemu.programming.PrgException;


public class BasicExprParser
{
  public static Integer checkIntConstant(
				BasicCompiler compiler,
				String        name )
  {
    Integer rv = null;
    if( name != null ) {
      boolean status = true;
      int     value  = 0;
      if( name.equals( "E_CHANNEL_ALREADY_OPEN" ) ) {
	value = BasicLibrary.E_CHANNEL_ALREADY_OPEN;
      } else if( name.equals( "E_CHANNEL_CLOSED" ) ) {
	value = BasicLibrary.E_CHANNEL_CLOSED;
      } else if( name.equals( "E_DEVICE_LOCKED" ) ) {
	value = BasicLibrary.E_DEVICE_LOCKED;
      } else if( name.equals( "E_DEVICE_NOT_FOUND" ) ) {
	value = BasicLibrary.E_DEVICE_NOT_FOUND;
      } else if( name.equals( "E_DISK_FULL" ) ) {
	value = BasicLibrary.E_DISK_FULL;
      } else if( name.equals( "E_EOF" ) ) {
	value = BasicLibrary.E_EOF;
      } else if( name.equals( "E_ERROR" ) ) {
	value = BasicLibrary.E_ERROR;
      } else if( name.equals( "E_FILE_NOT_FOUND" ) ) {
	value = BasicLibrary.E_FILE_NOT_FOUND;
      } else if( name.equals( "E_INVALID" ) ) {
	value = BasicLibrary.E_INVALID;
      } else if( name.equals( "E_IO_ERROR" ) ) {
	value = BasicLibrary.E_IO_ERROR;
      } else if( name.equals( "E_IO_MODE" ) ) {
	value = BasicLibrary.E_IO_MODE;
      } else if( name.equals( "E_NO_DISK" ) ) {
	value = BasicLibrary.E_NO_DISK;
      } else if( name.equals( "E_OK" ) ) {
	value = BasicLibrary.E_OK;
      } else if( name.equals( "E_OVERFLOW" ) ) {
	value = BasicLibrary.E_OVERFLOW;
      } else if( name.equals( "E_PATH_NOT_FOUND" ) ) {
	value = BasicLibrary.E_PATH_NOT_FOUND;
      } else if( name.equals( "E_READ_ONLY" ) ) {
	value = BasicLibrary.E_READ_ONLY;
      } else if( name.equals( "FALSE" ) ) {
	value = 0;
      } else if( name.equals( "JOYST_BUTTONS" ) ) {
	value = (compiler.getTarget().getNamedValue( "JOYST_BUTTON1" )
		 | compiler.getTarget().getNamedValue( "JOYST_BUTTON2" ));
      } else if( name.equals( "PEN_NONE" ) ) {
	value = 0;
      } else if( name.equals( "PEN_NORMAL" ) ) {
	value = 1;
      } else if( name.equals( "PEN_RUBBER" ) ) {
	value = 2;
      } else if( name.equals( "PEN_XOR" ) ) {
	value = 3;
      } else if( name.equals( "TRUE" ) ) {
	value = 0xFFFF;
      } else if( compiler.getTarget().isReservedWord( name ) ) {
	value = compiler.getTarget().getNamedValue( name );
      } else {
	status = false;
      }
      if( status ) {
	rv = value;
      }
    }
    return rv;
  }


  /*
   * Diese Methode prueft, ob ein Ausdruck einen konstanten Wert hat.
   * Wenn ja, ist der Ausdruck nach Rueckkehr aus der Methode geparst.
   *
   * In dieser Methode wird nicht der gesamte Syntaxbaum abgebildet.
   * Es reicht, nur die Elemente zu pruefen,
   * die sinnvollerweise vorkommen koennen.
   * Das sind:
   *   Zahlen-Literal
   *   Konstante
   *   Vergleich einer Konstante mit einer Zahl oder einer anderen Konstante
   *
   * Wenn andere Faelle, die hier nicht erkannt werden,
   * auch einen konstanten Wert zurueckliefern sollten,
   * wird dafuer normaler Programmcode erzeugt.
   */
  public static Integer checkParseConstExpr(
				BasicCompiler     compiler,
				CharacterIterator iter )
  {
    Integer rv     = null;
    int     begPos = iter.getIndex();
    try {
      int value = parseConstNotExpr( compiler, iter );
      for(;;) {
	if( BasicUtil.checkKeyword( iter, "AND" ) ) {
	  rv &= parseConstNotExpr( compiler, iter );
	} else if( BasicUtil.checkKeyword( iter, "OR" ) ) {
	  rv |= parseConstNotExpr( compiler, iter );
	} else if( BasicUtil.checkKeyword( iter, "XOR" ) ) {
	  rv ^= parseConstNotExpr( compiler, iter );
	} else {
	  break;
	}
	value &= 0xFFFF;
      }
      rv = value;
    }
    catch( PrgException ex ) {
      rv = null;
    }
    finally {
      if( rv == null ) {
	iter.setIndex( begPos );
      }
    }
    return rv;
  }


  public static boolean checkParseStringPrimExpr(
				BasicCompiler     compiler,
				CharacterIterator iter ) throws PrgException
  {
    boolean rv   = false;
    String  text = BasicUtil.checkStringLiteral( compiler, iter );
    if( text != null ) {
      AsmCodeBuf asmOut = compiler.getCodeBuf();
      asmOut.append( "\tLD\tHL," );
      asmOut.append( compiler.getStringLiteralLabel( text ) );
      asmOut.newLine();
      rv = true;
    } else {
      rv = checkParseStringPrimVarExpr( compiler, iter );
    }
    return rv;
  }


  public static boolean checkParseStringPrimVarExpr(
				BasicCompiler     compiler,
				CharacterIterator iter ) throws PrgException
  {
    boolean rv     = false;
    int     begPos = iter.getIndex();
    String  name   = BasicUtil.checkIdentifier( iter );
    if( name != null ) {
      rv = BasicFuncParser.checkParseStringFunction( compiler, iter, name );
      if( !rv ) {
	if( name.endsWith( "$" ) ) {
	  CallableEntry entry = compiler.getCallableEntry( name );
	  if( entry != null ) {
	    if( entry instanceof FunctionEntry ) {
	      if( ((FunctionEntry) entry).getReturnType()
					!= BasicCompiler.DataType.STRING )
	      {
		BasicUtil.throwStringExprExpected();
	      }
	      compiler.parseCallableCall( iter, entry );
	    } else {
	      throw new PrgException(
		"Aufruf einer Prozedur an der Stelle nicht erlaubt" );
	    }
	  } else {
	    BasicExprParser.parseVariableExpr(
					compiler,
					iter,
					name,
					BasicCompiler.DataType.STRING );
	  }
	  rv = true;
	}
      }
    }
    if( !rv ) {
      iter.setIndex( begPos );
    }
    return rv;
  }


  public static void parse2ArgsTo_DE_HL(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    int        pos    = asmOut.length();
    BasicExprParser.parseExpr( compiler, iter );
    BasicUtil.parseToken( iter, ',' );
    Integer v1 = BasicUtil.removeLastCodeIfConstExpr( compiler, pos );
    if( v1 != null ) {
      BasicExprParser.parseExpr( compiler, iter );
      asmOut.append_LD_DE_nn( v1.intValue() );
    } else {
      String oldCode1 = asmOut.cut( pos );
      String newCode1 = BasicUtil.convertCodeToValueInDE( oldCode1 );
      BasicExprParser.parseExpr( compiler, iter );
      String code2 = asmOut.cut( pos );
      if( BasicUtil.isOnly_LD_HL_xx( code2 ) ) {
        if( newCode1 != null ) {
          asmOut.append( newCode1 );
        } else {
          asmOut.append( oldCode1 );
          asmOut.append( "\tEX\tDE,HL\n" );
        }
        asmOut.append( code2 );
      } else {
        asmOut.append( oldCode1 );
        asmOut.append( "\tPUSH\tHL\n" );
        asmOut.append( code2 );
        asmOut.append( "\tPOP\tDE\n" );
      }
    }
  }


  public static void parseEnclosedExpr(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    parseExpr( compiler, iter );
    BasicUtil.parseToken( iter, ')' );
  }


  public static void parseExpr(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    parseNotExpr( compiler, iter );
    for(;;) {
      if( BasicUtil.checkKeyword( iter, "AND" ) ) {
	int pos = asmOut.length();
	parseNotExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  asmOut.append( newCode );
	} else {
	  asmOut.append( "\tPUSH\tHL\n" );
	  asmOut.append( oldCode );
	  asmOut.append( "\tPOP\tDE\n" );
	}
	asmOut.append( "\tCALL\tO_AND\n" );
	compiler.addLibItem( BasicLibrary.LibItem.O_AND );
      } else if( BasicUtil.checkKeyword( iter, "OR" ) ) {
	int pos = asmOut.length();
	parseNotExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	if( !oldCode.equals( "\tLD\tHL,0000H\n" ) ) {
	  String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    asmOut.append( newCode );
	  } else {
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tPOP\tDE\n" );
	  }
	  asmOut.append( "\tCALL\tO_OR\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.O_OR );
	}
      } else if( BasicUtil.checkKeyword( iter, "XOR" ) ) {
	int pos = asmOut.length();
	parseNotExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  asmOut.append( newCode );
	} else {
	  asmOut.append( "\tPUSH\tHL\n" );
	  asmOut.append( oldCode );
	  asmOut.append( "\tPOP\tDE\n" );
	}
	asmOut.append( "\tCALL\tO_XOR\n" );
	compiler.addLibItem( BasicLibrary.LibItem.O_XOR );
      } else {
	break;
      }
    }
  }


  public static void parseStringPrimExpr(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    String text = BasicUtil.checkStringLiteral( compiler, iter );
    if( text != null ) {
      AsmCodeBuf asmOut = compiler.getCodeBuf();
      String     label  = compiler.getStringLiteralLabel( text );
      asmOut.append( "\tLD\tHL," );
      asmOut.append( label );
      asmOut.newLine();
    } else {
      if( !checkParseStringPrimVarExpr( compiler, iter ) ) {
	BasicUtil.throwStringExprExpected();
      }
    }
  }


  /*
   * Die Methode parst eine Variable.
   * Der Variablenname wurde zu dem Zeitpunkt bereits gelesen,
   * weshalb er uebergeben wird.
   */
  public static void parseVariableExpr(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			String                 varName,
			BasicCompiler.DataType dataType ) throws PrgException
  {
    SimpleVarInfo varInfo = compiler.checkVariable( iter, varName );
    if( varInfo != null ) {
      if( varInfo.getDataType() != dataType ) {
	varInfo = null;
      }
    }
    if( varInfo == null ) {
      if( dataType == BasicCompiler.DataType.STRING ) {
	throw new PrgException(
			"String-Variable oder String-Funktion erwartet" );
      } else {
	throw new PrgException(
			"Numerische Variable oder Funktion erwartet" );
      }
    }
    varInfo.ensureValueInHL( compiler.getCodeBuf() );
  }


	/* --- private Methoden --- */

  private static void check8BitChar( char ch ) throws PrgException
  {
    if( (ch == 0) || (ch > 0xFF) ) {
      throw new PrgException( "Zeichen \'" + ch
		+ "\' au\u00DFerhalb des 8-Bit-Wertebereiches" );
    }
  }


  private static boolean checkAppend_LD_HL_Constant(
					BasicCompiler compiler,
					String        name )
  {
    boolean rv = false;
    if( name != null ) {
      AsmCodeBuf asmOut = compiler.getCodeBuf();
      if( name.equals( "TOP" ) ) {
	rv = true;
	asmOut.append( "\tLD\tHL,MTOP\n" );
	compiler.addLibItem( BasicLibrary.LibItem.MTOP );
      } else {
	Integer value = BasicExprParser.checkIntConstant( compiler, name );
	if( value != null ) {
	  asmOut.append_LD_HL_nn( value.intValue() );
	  rv = true;
	}
      }
    }
    return rv;
  }


  private static Integer checkIntLiteral(
			BasicCompiler     compiler,
			CharacterIterator iter,
			boolean           enableWarnings )
						throws PrgException
						
  {
    Integer rv = null;
    char ch = BasicUtil.skipSpaces( iter );
    if( (ch >= '0') && (ch <= '9') ) {
      rv = BasicUtil.readNumber( iter );
      if( rv == null ) {
	BasicUtil.throwNumberExpected();
      }
    }
    else if( ch == '&' ) {
      ch = iter.next();
      if( (ch == 'B') || (ch == 'b') ) {
	ch = iter.next();
	if( (ch == '0') || (ch == '1') ) {
	  int value = 0;
	  while( (ch == '0') || (ch == '1') ) {
	    value <<= 1;
	    if( ch == '1' ) {
	      value |= 1;
	    }
	    ch = iter.next();
	  }
	  rv = value;
	} else {
	  throw new PrgException( "0 oder 1 erwartet" );
	}
      } else if( (ch == 'H') || (ch == 'h') ) {
	iter.next();
	rv = BasicUtil.readHex( iter );
	if( rv == null ) {
	  BasicUtil.throwHexDigitExpected();
	}
      } else {
	throw new PrgException( "B oder H erwartet" );
      }
    }
    else if( ch == '\'' ) {
      ch = iter.next();
      iter.next();
      if( enableWarnings
	  && compiler.getBasicOptions().getWarnNonAsciiChars()
	  && ((ch < '\u0020') || (ch > '\u007F')) )
      {
	compiler.putWarningNonAsciiChar( ch );
      }
      BasicUtil.parseToken( iter, '\'' );
      check8BitChar( ch );
      rv = Integer.valueOf( ch );
    }
    return rv;
  }


  private static int parseConstNotExpr(
				BasicCompiler     compiler,
				CharacterIterator iter )
						throws PrgException
  {
    int rv = 0;
    if( BasicUtil.checkKeyword( iter, "NOT" ) ) {
      rv = (~parseConstCondExpr( compiler, iter ) & 0xFFFF);
    } else {
      rv = parseConstCondExpr( compiler, iter );
    }
    return rv;
  }


  private static int parseConstCondExpr(
				BasicCompiler     compiler,
				CharacterIterator iter )
						throws PrgException
  {
    int  rv = parseConstShiftExpr( compiler, iter );
    char ch = BasicUtil.skipSpaces( iter );
    if( ch == '<' ) {
      ch = iter.next();
      if( ch == '=' ) {
	iter.next();
	rv = (rv <= parseConstShiftExpr( compiler, iter ) ? -1 : 0);
      } else if( ch == '>' ) {
	iter.next();
	rv = (rv != parseConstShiftExpr( compiler, iter ) ? -1 : 0);
      } else {
	rv = (rv < parseConstShiftExpr( compiler, iter ) ? -1 : 0);
      }
    }
    else if( ch == '=' ) {
      iter.next();
      rv = (rv == parseConstShiftExpr( compiler, iter ) ? -1 : 0);
    }
    else if( ch == '>' ) {
      ch = iter.next();
      if( ch == '=' ) {
	iter.next();
	rv = (rv >= parseConstShiftExpr( compiler, iter ) ? -1 : 0);
      } else {
	rv = (rv > parseConstShiftExpr( compiler, iter ) ? -1 : 0);
      }
    }
    return rv & 0xFFFF;
  }


  private static int parseConstShiftExpr(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    /*
     * Der Einfachheit halber wird hier nicht der ganze Syntax-Baum
     * abgebildet, sondern nur die sinnvollen Faelle.
     * Das sind an dieser Stelle:
     *   Zahlen-Literal
     *   Konstante
     */
    Integer value = checkIntLiteral( compiler, iter, false );
    if( value == null ) {
      String name = BasicUtil.checkIdentifier( iter );
      if( name != null ) {
	value = checkIntConstant( compiler, name );
      }
    }
    if( value == null ) {
      throwNoConstExpr();
    }
    return value.intValue();
  }


  private static void parseNotExpr(
				BasicCompiler     compiler,
				CharacterIterator iter )
						throws PrgException
  {
    if( BasicUtil.checkKeyword( iter, "NOT" ) ) {
      parseCondExpr( compiler, iter );
      compiler.getCodeBuf().append( "\tCALL\tO_NOT\n" );
      compiler.addLibItem( BasicLibrary.LibItem.O_NOT );
    } else {
      parseCondExpr( compiler, iter );
    }
  }


  private static void parseCondExpr(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    if( checkParseStringPrimExpr( compiler, iter ) ) {

      // String-Vergleich
      char ch = BasicUtil.skipSpaces( iter );
      if( ch == '<' ) {
	ch = iter.next();
	if( ch == '=' ) {
	  iter.next();
	  asmOut.append( "\tPUSH\tHL\n" );
	  parseStringPrimExpr( compiler, iter );
	  asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STLE\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.O_STLE );
	} else if( ch == '>' ) {
	  iter.next();
	  asmOut.append( "\tPUSH\tHL\n" );
	  parseStringPrimExpr( compiler, iter );
	  asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STNE\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.O_STNE );
	} else {
	  asmOut.append( "\tPUSH\tHL\n" );
	  parseStringPrimExpr( compiler, iter );
	  asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STLT\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.O_STLT );
	}
      } else if( ch == '=' ) {
	iter.next();
	asmOut.append( "\tPUSH\tHL\n" );
	parseStringPrimExpr( compiler, iter );
	asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STEQ\n" );
	compiler.addLibItem( BasicLibrary.LibItem.O_STEQ );
      } else if( ch == '>' ) {
	ch = iter.next();
	if( ch == '=' ) {
	  iter.next();
	  asmOut.append( "\tPUSH\tHL\n" );
	  parseStringPrimExpr( compiler, iter );
	  asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STGE\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.O_STGE );
	} else {
	  asmOut.append( "\tPUSH\tHL\n" );
	  parseStringPrimExpr( compiler, iter );
	  asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_STGT\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.O_STGT );
	}
      } else {
	throwCondOpExpected();
      }

    } else {

      // prufen auf numerischen Vergleich
      parseShiftExpr( compiler, iter );
      char ch = BasicUtil.skipSpaces( iter );
      if( ch == '<' ) {
	ch = iter.next();
	if( ch == '=' ) {
	  iter.next();
	  int pos = asmOut.length();
	  parseShiftExpr( compiler, iter );
	  String oldCode = asmOut.cut( pos );
	  String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    asmOut.append( newCode );
	    asmOut.append( "\tCALL\tO_LE\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_LE );
	  } else {
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tPOP\tDE\n"
			+ "\tCALL\tO_GE\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_GE );
	  }
	} else if( ch == '>' ) {
	  iter.next();
	  int pos = asmOut.length();
	  parseShiftExpr( compiler, iter );
	  String oldCode = asmOut.cut( pos );
	  String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    asmOut.append( newCode );
	  } else {
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tPOP\tDE\n" );
	  }
	  asmOut.append( "\tCALL\tO_NE\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.O_NE );
	} else {
	  int pos = asmOut.length();
	  parseShiftExpr( compiler, iter );
	  String oldCode = asmOut.cut( pos );
	  String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    asmOut.append( newCode );
	    asmOut.append( "\tCALL\tO_LT\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_LT );
	  } else {
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tPOP\tDE\n"
			+ "\tCALL\tO_GT\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_GT );
	  }
	}
      }
      else if( ch == '=' ) {
	iter.next();
	int pos = asmOut.length();
	parseShiftExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  asmOut.append( newCode );
	} else {
	  asmOut.append( "\tPUSH\tHL\n" );
	  asmOut.append( oldCode );
	  asmOut.append( "\tPOP\tDE\n" );
	}
	asmOut.append( "\tCALL\tO_EQ\n" );
	compiler.addLibItem( BasicLibrary.LibItem.O_EQ );
      }
      else if( ch == '>' ) {
	ch = iter.next();
	if( ch == '=' ) {
	  iter.next();
	  int pos = asmOut.length();
	  parseShiftExpr( compiler, iter );
	  String oldCode = asmOut.cut( pos );
	  String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    asmOut.append( newCode );
	    asmOut.append( "\tCALL\tO_GE\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_GE );
	  } else {
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tPOP\tDE\n"
				+ "\tCALL\tO_LE\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_LE );
	  }
	} else {
	  int pos = asmOut.length();
	  parseShiftExpr( compiler, iter );
	  String oldCode = asmOut.cut( pos );
	  String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    asmOut.append( newCode );
	    asmOut.append( "\tCALL\tO_GT\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_GT );
	  } else {
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tO_LT\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_LT );
	  }
	}
      }
    }
  }


  private static void parseShiftExpr(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    parseAddExpr( compiler, iter );
    for(;;) {
      if( BasicUtil.checkKeyword( iter, "SHL" ) ) {
	int pos = asmOut.length();
	parseAddExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	  asmOut.append( "\tSLA\tL\n"
		  + "\tRL\tH\n" );
	} else {
	  String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    asmOut.append( newCode );
	  } else {
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n" );
	  }
	  asmOut.append( "\tCALL\tO_SHL\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.O_SHL );
	}
      } else if( BasicUtil.checkKeyword( iter, "SHR" ) ) {
	int pos = asmOut.length();
	parseAddExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	  asmOut.append( "\tSRL\tH\n"
			+ "\tRR\tL\n" );
	} else {
	  String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    asmOut.append( newCode );
	  } else {
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n" );
	  }
	  asmOut.append( "\tCALL\tO_SHR\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.O_SHR );
	}
      } else {
	break;
      }
    }
  }


  private static void parseAddExpr(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    parseMulExpr( compiler, iter );
    for(;;) {
      char ch = BasicUtil.skipSpaces( iter );
      if( ch == '+' ) {
	iter.next();
	int pos = asmOut.length();
	parseMulExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	if( !oldCode.equals( "\tLD\tHL,0000H\n" ) ) {
	  if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	    asmOut.append( "\tCALL\tO_INC\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_INC );
	  } else if( oldCode.equals( "\tLD\tHL,0FFFFH\n" ) ) {
	    asmOut.append( "\tCALL\tO_DEC\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_DEC );
	  } else {
	    String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	    if( newCode != null ) {
	      asmOut.append( newCode );
	    } else {
	      asmOut.append( "\tPUSH\tHL\n" );
	      asmOut.append( oldCode );
	      asmOut.append( "\tPOP\tDE\n" );
	    }
	    asmOut.append( "\tCALL\tO_ADD\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_ADD );
	  }
	}
      } else if( ch == '-' ) {
	iter.next();
	int pos = asmOut.length();
	parseMulExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	if( !oldCode.equals( "\tLD\tHL,0000H\n" ) ) {
	  if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	    asmOut.append( "\tCALL\tO_DEC\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_DEC );
	  } else if( oldCode.equals( "\tLD\tHL,0FFFFH\n" ) ) {
	    asmOut.append( "\tCALL\tO_INC\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_INC );
	  } else {
	    String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	    if( newCode != null ) {
	      asmOut.append( newCode );
	    } else {
	      asmOut.append( "\tPUSH\tHL\n" );
	      asmOut.append( oldCode );
	      asmOut.append( "\tPOP\tDE\n"
				+ "\tEX\tDE,HL\n" );
	    }
	    asmOut.append( "\tCALL\tO_SUB\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.O_SUB );
	  }
	}
      } else if( BasicUtil.checkKeyword( iter, "ADD" ) ) {
	int pos = asmOut.length();
	parseMulExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	if( !oldCode.equals( "\tLD\tHL,0000H\n" ) ) {
	  if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	    asmOut.append( "\tINC\tHL\n" );
	  } else if( oldCode.equals( "\tLD\tHL,0FFFFH\n" ) ) {
	    asmOut.append( "\tDEC\tHL\n" );
	  } else {
	    String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	    if( newCode != null ) {
	      asmOut.append( newCode );
	    } else {
	      asmOut.append( "\tPUSH\tHL\n" );
	      asmOut.append( oldCode );
	      asmOut.append( "\tPOP\tDE\n" );
	    }
	    asmOut.append( "\tADD\tHL,DE\n" );
	  }
	}
      } else if( BasicUtil.checkKeyword( iter, "SUB" ) ) {
	int pos = asmOut.length();
	parseMulExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	if( !oldCode.equals( "\tLD\tHL,0000H\n" ) ) {
	  if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	    asmOut.append( "\tDEC\tHL\n" );
	  } else if( oldCode.equals( "\tLD\tHL,0FFFFH\n" ) ) {
	    asmOut.append( "\tINC\tHL\n" );
	  } else {
	    String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	    if( newCode != null ) {
	      asmOut.append( newCode );
	    } else {
	      asmOut.append( "\tPUSH\tHL\n" );
	      asmOut.append( oldCode );
	      asmOut.append( "\tPOP\tDE\n"
				+ "\tEX\tDE,HL\n" );
	    }
	    asmOut.append( "\tOR\tA\n"
				+ "\tSBC\tHL,DE\n" );
	  }
	}
      } else {
	break;
      }
    }
  }


  private static void parseMulExpr(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    parseUnaryExpr( compiler, iter );
    for(;;) {
      char ch = BasicUtil.skipSpaces( iter );
      if( ch == '*' ) {
	iter.next();
	int pos = asmOut.length();
	parseUnaryExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  asmOut.append( newCode );
	} else {
	  asmOut.append( "\tPUSH\tHL\n" );
	  asmOut.append( oldCode );
	  asmOut.append( "\tPOP\tDE\n" );
	}
	asmOut.append( "\tCALL\tO_MUL\n" );
	compiler.addLibItem( BasicLibrary.LibItem.O_MUL );
      } else if( ch == '/' ) {
	iter.next();
	int pos = asmOut.length();
	parseUnaryExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  asmOut.append( newCode );
	} else {
	  asmOut.append( "\tPUSH\tHL\n" );
	  asmOut.append( oldCode );
	  asmOut.append( "\tPOP\tDE\n"
			+ "\tEX\tDE,HL\n" );
	}
	asmOut.append( "\tCALL\tO_DIV\n" );
	compiler.addLibItem( BasicLibrary.LibItem.O_DIV );
      } else if( BasicUtil.checkKeyword( iter, "MOD" ) ) {
	int pos = asmOut.length();
	parseUnaryExpr( compiler, iter );
	String oldCode = asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  asmOut.append( newCode );
	} else {
	  asmOut.append( "\tPUSH\tHL\n" );
	  asmOut.append( oldCode );
	  asmOut.append( "\tPOP\tDE\n"
			+ "\tEX\tDE,HL\n" );
	}
	asmOut.append( "\tCALL\tO_MOD\n" );
	compiler.addLibItem( BasicLibrary.LibItem.O_MOD );
      } else {
	break;
      }
    }
  }


  private static void parseUnaryExpr(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    char ch = BasicUtil.skipSpaces( iter );
    if( ch == '-' ) {
      iter.next();
      AsmCodeBuf asmOut = compiler.getCodeBuf();
      Integer    value  = BasicUtil.readNumber( iter );
      if( value != null ) {
	asmOut.append_LD_HL_nn( -value.intValue() );
      } else {
	parsePrimExpr( compiler, iter );
	asmOut.append( "\tCALL\tNEGHL\n" );
	compiler.addLibItem( BasicLibrary.LibItem.ABS_NEG_HL );
      }
    } else {
      if( ch == '+' ) {
	iter.next();
      }
      parsePrimExpr( compiler, iter );
    }
  }


  private static void parsePrimExpr(
				BasicCompiler     compiler,
				CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    char      ch      = BasicUtil.skipSpaces( iter );
    if( ch == '(' ) {
      iter.next();
      parseExpr( compiler, iter );
      BasicUtil.parseToken( iter, ')' );
    } else {
      Integer value = checkIntLiteral( compiler, iter, true );
      if( value != null ) {
	asmOut.append_LD_HL_nn( value.intValue() );
      } else {
	String name = BasicUtil.checkIdentifier( iter );
	if( name != null ) {
	  // auf Konstante pruefen
	  if( !checkAppend_LD_HL_Constant( compiler, name ) ) {
	    // auf Systemvariable pruefen
	    AbstractTarget target = compiler.getTarget();
	    if( name.equals( "ERR" ) ) {
	      asmOut.append( "\tLD\tHL,(M_ERN)\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.M_ERN );
	    } else if( name.equals( "H_CHAR" ) ) {
	      target.appendHCharTo( asmOut );
	    } else if( name.equals( "H_PIXEL" ) ) {
	      target.appendHPixelTo( asmOut );
	    } else if( name.equals( "W_CHAR" ) ) {
	      target.appendWCharTo( asmOut );
	    } else if( name.equals( "W_PIXEL" ) ) {
	      target.appendWPixelTo( asmOut );
	    } else if( name.equals( "XPOS" ) ) {
	      asmOut.append( "\tLD\tHL,(M_XPOS)\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.M_XYPO );
	    } else if( name.equals( "YPOS" ) ) {
	      asmOut.append( "\tLD\tHL,(M_YPOS)\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.M_XYPO );
	    } else {
	      // auf Systemfunktion pruefen
	      if( !BasicFuncParser.checkParseIntFunction(
						  compiler, iter, name ) )
	      {
		// auf benutzerdefinierte Funktion pruefen
		CallableEntry entry = compiler.getCallableEntry( name );
		if( entry != null ) {
		  if( entry instanceof FunctionEntry ) {
		    if( ((FunctionEntry) entry).getReturnType()
					  != BasicCompiler.DataType.INTEGER )
		    {
		      throwIntExprExpected();
		    }
		    compiler.parseCallableCall( iter, entry );
		  } else {
		    throw new PrgException(
			  "Aufruf einer Prozedur an der Stelle nicht erlaubt" );
		  }
		} else {
		  if( name.endsWith( "$" ) ) {
		    throwIntExprExpected();
		  }
		  parseVariableExpr(
			  compiler,
			  iter,
			  name,
			  BasicCompiler.DataType.INTEGER );
		}
	      }
	    }
	  }
	} else {
	  BasicUtil.throwUnexpectedChar( BasicUtil.skipSpaces( iter ) );
	}
      }
    }
  }


  private static void throwCondOpExpected() throws PrgException
  {
    throw new PrgException( "Vergleichsoperator erwartet" );
  }


  private static void throwIntExprExpected() throws PrgException
  {
    throw new PrgException( "Integer-Ausdruck erwartet" );
  }


  private static void throwNoConstExpr() throws PrgException
  {
    throw new PrgException( "Kein konstanter Ausdruck" );
  }
}
