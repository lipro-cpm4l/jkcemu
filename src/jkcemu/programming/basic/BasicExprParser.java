/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Parser fuer BASIC-Ausdruecke
 */

package jkcemu.programming.basic;

import java.text.CharacterIterator;
import jkcemu.programming.PrgException;


public class BasicExprParser
{
  public static NumericValue checkConstant(
				BasicCompiler compiler,
				String        name ) throws PrgException
  {
    NumericValue rv = null;
    if( name != null ) {
      if( name.equals( "E_CHANNEL_ALREADY_OPEN" ) ) {
	rv = NumericValue.from( BasicLibrary.E_CHANNEL_ALREADY_OPEN );
      } else if( name.equals( "E_CHANNEL_CLOSED" ) ) {
	rv = NumericValue.from( BasicLibrary.E_CHANNEL_CLOSED );
      } else if( name.equals( "E_DEVICE_LOCKED" ) ) {
	rv = NumericValue.from( BasicLibrary.E_DEVICE_LOCKED );
      } else if( name.equals( "E_DEVICE_NOT_FOUND" ) ) {
	rv = NumericValue.from( BasicLibrary.E_DEVICE_NOT_FOUND );
      } else if( name.equals( "E_DIGITS_TRUNCATED" ) ) {
	rv = NumericValue.from( BasicLibrary.E_DIGITS_TRUNCATED );
      } else if( name.equals( "E_DISK_FULL" ) ) {
	rv = NumericValue.from( BasicLibrary.E_DISK_FULL );
      } else if( name.equals( "E_EOF" ) ) {
	rv = NumericValue.from( BasicLibrary.E_EOF );
      } else if( name.equals( "E_ERROR" ) ) {
	rv = NumericValue.from( BasicLibrary.E_ERROR );
      } else if( name.equals( "E_FILE_NOT_FOUND" ) ) {
	rv = NumericValue.from( BasicLibrary.E_FILE_NOT_FOUND );
      } else if( name.equals( "E_INVALID" ) ) {
	rv = NumericValue.from( BasicLibrary.E_INVALID );
      } else if( name.equals( "E_IO_ERROR" ) ) {
	rv = NumericValue.from( BasicLibrary.E_IO_ERROR );
      } else if( name.equals( "E_IO_MODE" ) ) {
	rv = NumericValue.from( BasicLibrary.E_IO_MODE );
      } else if( name.equals( "E_NO_DISK" ) ) {
	rv = NumericValue.from( BasicLibrary.E_NO_DISK );
      } else if( name.equals( "E_OK" ) ) {
	rv = NumericValue.from( BasicLibrary.E_OK );
      } else if( name.equals( "E_OVERFLOW" ) ) {
	rv = NumericValue.from( BasicLibrary.E_OVERFLOW );
      } else if( name.equals( "E_PATH_NOT_FOUND" ) ) {
	rv = NumericValue.from( BasicLibrary.E_PATH_NOT_FOUND );
      } else if( name.equals( "E_READ_ONLY" ) ) {
	rv = NumericValue.from( BasicLibrary.E_READ_ONLY );
      } else if( name.equals( "FALSE" ) ) {
	rv = NumericValue.from( 0 );
      } else if( name.equals( "JOYST_BUTTONS" ) ) {
	rv = NumericValue.from(
		compiler.getTarget().getNamedValue( "JOYST_BUTTON1" )
		| compiler.getTarget().getNamedValue( "JOYST_BUTTON2" ) );
      } else if( name.equals( "PEN_NONE" ) ) {
	rv = NumericValue.from( 0 );
      } else if( name.equals( "PEN_NORMAL" ) ) {
	rv = NumericValue.from( 1 );
      } else if( name.equals( "PEN_ERASER" ) ) {
	rv = NumericValue.from( 2 );
      } else if( name.equals( "PEN_XOR" ) ) {
	rv = NumericValue.from( 3 );
      } else if( name.equals( "ROUND_HALF_DOWN" ) ) {
	rv = NumericValue.from( BasicLibrary.ROUND_HALF_DOWN );
      } else if( name.equals( "ROUND_HALF_EVEN" ) ) {
	rv = NumericValue.from( BasicLibrary.ROUND_HALF_EVEN );
      } else if( name.equals( "ROUND_HALF_UP" ) ) {
	rv = NumericValue.from( BasicLibrary.ROUND_HALF_UP );
      } else if( name.equals( "TRUE" ) ) {
	rv = NumericValue.from( -1 );
      } else if( compiler.getTarget().isReservedWord( name ) ) {
	rv = NumericValue.from( compiler.getTarget().getNamedValue( name ) );
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
  public static Integer checkParseInt2ConstExpr(
				BasicCompiler     compiler,
				CharacterIterator iter )
  {
    Integer rv     = null;
    int     begPos = iter.getIndex();
    try {
      int value = parseInt2ConstShiftExpr( compiler, iter );
      if( BasicUtil.checkToken( iter, '=' ) ) {
	rv = (value == parseInt2ConstShiftExpr( compiler, iter ) ? -1 : 0);
      } else if( BasicUtil.checkToken( iter, "<>" ) ) {
	rv = (value != parseInt2ConstShiftExpr( compiler, iter ) ? -1 : 0);
      } else if( BasicUtil.checkToken( iter, "<=" ) ) {
	rv = (value <= parseInt2ConstShiftExpr( compiler, iter ) ? -1 : 0);
      } else if( BasicUtil.checkToken( iter, '<' ) ) {
	rv = (value < parseInt2ConstShiftExpr( compiler, iter ) ? -1 : 0);
      } else if( BasicUtil.checkToken( iter, ">=" ) ) {
	rv = (value >= parseInt2ConstShiftExpr( compiler, iter ) ? -1 : 0);
      } else if( BasicUtil.checkToken( iter, '>' ) ) {
	rv = (value > parseInt2ConstShiftExpr( compiler, iter ) ? -1 : 0);
      } else {
	rv = Integer.valueOf( value );
      }
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
				CharacterIterator iter,
				ParseContext      context )
							throws PrgException
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
      rv = checkParseStringPrimVarExpr( compiler, iter, context );
    }
    return rv;
  }


  public static boolean checkParseStringPrimVarExpr(
				BasicCompiler     compiler,
				CharacterIterator iter,
				ParseContext      context )
							throws PrgException
  {
    boolean rv     = false;
    int     begPos = iter.getIndex();
    String  name   = BasicUtil.checkIdentifier( iter );
    if( name != null ) {
      rv = BasicFuncParser.checkParseStringFunction(
						compiler,
						iter,
						context,
						name );
      if( !rv ) {
	if( BasicUtil.endsWithStringSuffix( name ) ) {
	  CallableEntry entry = compiler.getCallableEntry( name );
	  if( entry != null ) {
	    if( entry instanceof FunctionEntry ) {
	      if( ((FunctionEntry) entry).getReturnType().equals(
					BasicCompiler.DataType.STRING ) )
	      {
		compiler.parseCallableCall( iter, context, entry );
		rv = true;
	      }
	    }
	  } else {
	    SimpleVarInfo varInfo = compiler.checkVariable(
					iter,
					context,
					name,
					BasicCompiler.AccessMode.READ );
	    if( varInfo != null ) {
	      if( varInfo.getDataType() == BasicCompiler.DataType.STRING ) {
		varInfo.writeCode_LD_Reg_Var( compiler );
		rv = true;
	      }
	    }
	  }
	}
      }
    }
    if( !rv ) {
      iter.setIndex( begPos );
    }
    return rv;
  }


  public static void parse2Int2ArgsTo_DE_HL(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    int        pos    = asmOut.length();
    parseInt2Expr( compiler, iter, context );
    BasicUtil.parseToken( iter, ',' );
    Integer v1 = BasicUtil.removeLastCodeIfConstExpr( asmOut, pos );
    if( v1 != null ) {
      parseInt2Expr( compiler, iter, context );
      asmOut.append_LD_DE_nn( v1.intValue() );
    } else {
      // wird durch Optimizer optimiert
      asmOut.append( "\tPUSH\tHL\n" );
      parseInt2Expr( compiler, iter, context );
      asmOut.append( "\tPOP\tDE\n" );
    }
  }


  /*
   * Parsen einer Operation mit zwei Operanden
   */
  public static BasicCompiler.DataType parseBiOpNumExpr(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			ParseContext           context,
			BasicCompiler.DataType prefRetType,
			OpParser               subLevelParser,
			OpInfo...              ops ) throws PrgException
  {
    AsmCodeBuf             asmOut = compiler.getCodeBuf();
    BasicCompiler.DataType rv     = subLevelParser.parseOp(
							compiler,
							iter,
							context,
							prefRetType );
    for(;;) {
      boolean done = false;
      char   ch    = BasicUtil.skipSpaces( iter );
      for( OpInfo opInfo : ops ) {
	boolean found    = false;
	String  operator = opInfo.getOperator();
	char    ch0      = Character.toUpperCase( operator.charAt( 0 ) );
	if( (ch0 >= 'A') && (ch0 <= 'Z') ) {
	  found = BasicUtil.checkKeyword( iter, operator );
	} else {
	  found = BasicUtil.checkToken( iter, operator );
	}
	if( found ) {

	  // ggf. aussagekraeftige Fehlermeldung
	  switch( rv ) {
	    case DEC6:
	      if( opInfo.getAsmCodeD6() == null ) {
		BasicUtil.throwOp1DataTypeNotAllowed();
	      }
	      break;
	    case INT4:
	      if( opInfo.getAsmCodeI4() == null ) {
		BasicUtil.throwOp1DataTypeNotAllowed();
	      }
	      break;
	  }

	  int                    pos = asmOut.length();
	  BasicCompiler.DataType dt2 = subLevelParser.parseOp(
							compiler,
							iter,
							context,
							prefRetType );
	  String oldOp2Code = asmOut.cut( pos );

	  if( rv.equals( BasicCompiler.DataType.DEC6 )
	      || dt2.equals( BasicCompiler.DataType.DEC6 ) )
	  {
	    // Dec6-Operation
	    String asmCodeD6 = opInfo.getAsmCodeD6();
	    if( asmCodeD6 == null ) {
	      BasicUtil.throwOp2DataTypeNotAllowed();
	    }

	    // Op1
	    switch( rv ) {
	      case INT2:
		asmOut.append( "\tCALL\tF_D6_CDEC_I2\n" );
		compiler.addLibItem( BasicLibrary.LibItem.F_D6_CDEC_I2 );
		context.setMAccuDirty();
		break;
	      case INT4:
		asmOut.append( "\tCALL\tF_D6_CDEC_I4\n" );
		compiler.addLibItem( BasicLibrary.LibItem.F_D6_CDEC_I4 );
		context.setMAccuDirty();
		break;
	      case DEC6:
		// leer
		break;
	      default:
		BasicUtil.throwNumericExprExpected();
	    }
	    String ldOp1Code = BasicUtil.convertLastCodeToD6LoadOp1(
								compiler );
	    if( ldOp1Code == null ) {
	      asmOut.append_PUSH_D6Accu( compiler );
	    }

	    // Op2
	    asmOut.append( oldOp2Code );
	    switch( dt2 ) {
	      case INT2:
		asmOut.append( "\tCALL\tF_D6_CDEC_I2\n" );
		compiler.addLibItem( BasicLibrary.LibItem.F_D6_CDEC_I2 );
		context.setMAccuDirty();
		break;
	      case INT4:
		asmOut.append( "\tCALL\tF_D6_CDEC_I4\n" );
		compiler.addLibItem( BasicLibrary.LibItem.F_D6_CDEC_I4 );
		context.setMAccuDirty();
		break;
	      case DEC6:
		// leer
		break;
	      default:
		BasicUtil.throwNumericExprExpected();
	    }
	    if( ldOp1Code != null ) {
	      asmOut.append( ldOp1Code );
	    } else {
	      asmOut.append_POP_D6Op1( compiler );
	    }

	    // Operation
	    asmOut.append( asmCodeD6 );
	    BasicLibrary.LibItem libItemD6 = opInfo.getLibItemD6();
	    if( libItemD6 != null ) {
	      compiler.addLibItem( libItemD6 );
	    }
	    compiler.setErrVarsSet();
	    context.setMAccuDirty();
	    rv = BasicCompiler.DataType.DEC6;
	  }
	  else if( rv.equals( BasicCompiler.DataType.INT4 )
		   || dt2.equals( BasicCompiler.DataType.INT4 ) )
	  {
	    // Int4-Operation
	    String asmCodeI4 = opInfo.getAsmCodeI4();
	    if( asmCodeI4 == null ) {
	      BasicUtil.throwOp2DataTypeNotAllowed();
	    }
	    switch( rv ) {
	      case INT2:
		asmOut.append( "\tCALL\tI4_HL_TO_DEHL\n" );
		compiler.addLibItem( BasicLibrary.LibItem.I4_HL_TO_DEHL );
		break;
	      case INT4:
		// leer
		break;
	      default:
		BasicUtil.throwNumericExprExpected();
	    }
	    switch( dt2 ) {
	      case INT2:
		asmOut.append_PUSH_DEHL();
		asmOut.append( oldOp2Code );
		asmOut.append( "\tCALL\tI4_HL_TO_DEHL\n" );
		asmOut.append_POP_DE2HL2( compiler );
		compiler.addLibItem( BasicLibrary.LibItem.I4_HL_TO_DEHL );
		break;
	      case INT4:
		if( BasicUtil.usesSecondCpuRegSet( oldOp2Code ) ) {
		  asmOut.append_PUSH_DEHL();
		  asmOut.append( oldOp2Code );
		  asmOut.append_POP_DE2HL2( compiler );
		} else {
		  asmOut.append( "\tEXX\n" );
		  asmOut.append( oldOp2Code );
		}
		break;
	      default:
		BasicUtil.throwIntOrLongExprExpected();
	    }
	    asmOut.append( asmCodeI4 );
	    compiler.addLibItem( opInfo.getLibItemI4() );
	    rv = BasicCompiler.DataType.INT4;
	  }
	  else if( rv.equals( BasicCompiler.DataType.INT2 )
		   && dt2.equals( BasicCompiler.DataType.INT2 ) )
	  {
	    // Int2-Operation
	    String newOp2Code = BasicUtil.convertCodeToValueInDE(
						      oldOp2Code );
	    if( newOp2Code != null ) {
	      asmOut.append( newOp2Code );
	    } else {
	      asmOut.append( "\tPUSH\tHL\n" );
	      asmOut.append( oldOp2Code );
	      asmOut.append( "\tPOP\tDE\n" );
	      if( !opInfo.isCommutative() ) {
		asmOut.append( "\tEX\tDE,HL\n" );
	      }
	    }
	    asmOut.append( opInfo.getAsmCodeI2() );
	    compiler.addLibItem( opInfo.getLibItemI2() );
	    rv = BasicCompiler.DataType.INT2;
	  } else {
	    BasicUtil.throwNumericExprExpected();
	  }
	  done = true;
	  break;
	}
      }
      if( !done ) {
	break;
      }
    }
    return rv;
  }


  public static void parseEnclosedInt2Expr(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    parseInt2Expr( compiler, iter, context );
    BasicUtil.parseToken( iter, ')' );
  }


  public static void parseEnclosedInt4Expr(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    parseInt4Expr( compiler, iter, context );
    BasicUtil.parseToken( iter, ')' );
  }


  public static void parseDec6Expr(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    switch( parseNumericExpr(
			compiler,
			iter,
			context,
			BasicCompiler.DataType.DEC6 ) )
    {
      case INT2:
	compiler.getCodeBuf().append( "\tCALL\tF_D6_CDEC_I2\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_D6_CDEC_I2 );
	context.setMAccuDirty();
	break;
      case INT4:
	compiler.getCodeBuf().append( "\tCALL\tF_D6_CDEC_I4\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_D6_CDEC_I4 );
	context.setMAccuDirty();
	break;
      case DEC6:
	// leer
	break;
      default:
	BasicUtil.throwNumericExprExpected();
    }
  }


  public static void parseInt2Expr(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    parseInt2Expr( compiler, iter, new ParseContext() );
  }


  public static void parseInt2Expr(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    if( !parseNumericExpr(
			compiler,
			iter,
			context,
			BasicCompiler.DataType.INT2 ).equals(
					BasicCompiler.DataType.INT2 ) )
    {
      BasicUtil.throwInt2ExprExpected();
    }
  }


  public static void parseInt4Expr(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    switch( parseNumericExpr(
			compiler,
			iter,
			context,
			BasicCompiler.DataType.INT4 ) )
    {
      case INT2:
	compiler.getCodeBuf().append( "\tCALL\tI4_HL_TO_DEHL\n" );
	compiler.addLibItem( BasicLibrary.LibItem.I4_HL_TO_DEHL );
	break;
      case INT4:
	// leer
	break;
      default:
	BasicUtil.throwIntOrLongExprExpected();
    }
  }


  public static BasicCompiler.DataType parseNumericExpr(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
  {
    return parseBiOpNumExpr(
		compiler,
		iter,
		context,
		prefRetType,
		new OpParser() {
			@Override
			public BasicCompiler.DataType parseOp(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
			{
			  return parseNotExpr(
					compiler,
					iter,
					context,
					prefRetType );
			}
		},
		new OpInfo(
			"AND",
			"\tCALL\tI2_AND_I2_I2\n",
			BasicLibrary.LibItem.I2_AND_I2_I2,
			"\tCALL\tI4_AND_I4_I4\n",
			BasicLibrary.LibItem.I4_AND_I4_I4,
			null,
			null,
			true ),
		new OpInfo(
			"OR",
			"\tCALL\tI2_OR_I2_I2\n",
			BasicLibrary.LibItem.I2_OR_I2_I2,
			"\tCALL\tI4_OR_I4_I4\n",
			BasicLibrary.LibItem.I4_OR_I4_I4,
			null,
			null,
			true ),
		new OpInfo(
			"XOR",
			"\tCALL\tI2_XOR_I2_I2\n",
			BasicLibrary.LibItem.I2_XOR_I2_I2,
			"\tCALL\tI4_XOR_I4_I4\n",
			BasicLibrary.LibItem.I4_XOR_I4_I4,
			null,
			null,
			true ) );
  }


  public static void parseStringPrimExpr(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    String text = BasicUtil.checkStringLiteral( compiler, iter );
    if( text != null ) {
      AsmCodeBuf asmOut = compiler.getCodeBuf();
      String     label  = compiler.getStringLiteralLabel( text );
      asmOut.append( "\tLD\tHL," );
      asmOut.append( label );
      asmOut.newLine();
    } else {
      if( !checkParseStringPrimVarExpr( compiler, iter, context ) ) {
	BasicUtil.throwStringExprExpected();
      }
    }
  }


	/* --- private Methoden --- */

  private static BasicCompiler.DataType checkNumericDirectValue(
				BasicCompiler compiler,
				ParseContext  context,
				String        name ) throws PrgException
  {
    BasicCompiler.DataType rv = null;
    if( name != null ) {
      AsmCodeBuf asmOut = compiler.getCodeBuf();
      if( name.equals( "TOP" ) ) {
	asmOut.append( "\tLD\tHL,MTOP\n" );
	compiler.addLibItem( BasicLibrary.LibItem.MTOP );
	rv = BasicCompiler.DataType.INT2;
      } else {
	NumericValue value = checkConstant( compiler, name );
	if( value != null ) {
	  value.writeCode_LD_Reg_DirectValue( compiler );
	  rv = value.getDataType();
	}
      }
    }
    return rv;
  }


  private static int parseInt2ConstShiftExpr(
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
    NumericValue value = NumericValue.checkLiteral(
						compiler,
						iter,
						BasicCompiler.DataType.INT2,
						false );
    if( value == null ) {
      String name = BasicUtil.checkIdentifier( iter );
      if( name != null ) {
	value = checkConstant( compiler, name );
      }
    }
    if( value == null ) {
      BasicUtil.throwNoConstExpr();
    }
    if( !value.getDataType().equals( BasicCompiler.DataType.INT2 ) ) {
      BasicUtil.throwInt2ExprExpected();
    }
    return value.intValue();
  }


  private static BasicCompiler.DataType parseNotExpr(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
  {
    boolean                isNot = BasicUtil.checkKeyword( iter, "NOT" );
    BasicCompiler.DataType rv    = parseCondExpr(
						compiler,
						iter,
						context,
						prefRetType );
    if( isNot ) {
      switch( rv ) {
	case INT2:
	  compiler.getCodeBuf().append( "\tCALL\tI2_NOT_I2\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.I2_NOT_I2 );
	  break;
	case INT4:
	  compiler.getCodeBuf().append( "\tCALL\tI4_NOT_I4\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.I4_NOT_I4 );
	  break;
	default:
	  BasicUtil.throwIntOrLongExprExpected();
      }
    }
    return rv;
  }


  private static BasicCompiler.DataType parseCondExpr(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
  {
    AsmCodeBuf             asmOut = compiler.getCodeBuf();
    BasicCompiler.DataType rv     = BasicCompiler.DataType.INT2;
    if( checkParseStringPrimExpr( compiler, iter, context ) ) {

      // String-Vergleich
      String               asmLabel = null;
      BasicLibrary.LibItem libItem  = null;
      if( BasicUtil.checkToken( iter, '=' ) ) {
	asmLabel = "I2_EQ_S_S";
	libItem  = BasicLibrary.LibItem.I2_EQ_S_S;
      } else if( BasicUtil.checkToken( iter, "<>" ) ) {
	asmLabel = "I2_NE_S_S";
	libItem  = BasicLibrary.LibItem.I2_NE_S_S;
      } else if( BasicUtil.checkToken( iter, "<=" ) ) {
	asmLabel = "I2_LE_S_S";
	libItem  = BasicLibrary.LibItem.I2_LE_S_S;
      } else if( BasicUtil.checkToken( iter, '<' ) ) {
	asmLabel = "I2_LT_S_S";
	libItem  = BasicLibrary.LibItem.I2_LT_S_S;
      } else if( BasicUtil.checkToken( iter, ">=" ) ) {
	asmLabel = "I2_GE_S_S";
	libItem  = BasicLibrary.LibItem.I2_GE_S_S;
      } else if( BasicUtil.checkToken( iter, '>' ) ) {
	asmLabel = "I2_GT_S_S";
	libItem  = BasicLibrary.LibItem.I2_GT_S_S;
      }
      if( (libItem != null) && (asmLabel != null) ) {
	asmOut.append( "\tPUSH\tHL\n" );
	parseStringPrimExpr( compiler, iter, context );
	asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\t" );
	asmOut.append( asmLabel );
	asmOut.newLine();
	compiler.addLibItem( libItem );
      } else {
	throwCondOpExpected();
      }

    } else {

      // prufen auf numerischen Vergleich
      rv = parseShiftExpr( compiler, iter, context, prefRetType );

      String               asmLabelI2        = null;
      String               asmLabelI2Swapped = null;
      String               asmLabelI4        = null;
      String               asmLabelI4Swapped = null;
      String               asmLabelD6        = null;
      BasicLibrary.LibItem libItemI2         = null;
      BasicLibrary.LibItem libItemI2Swapped  = null;
      BasicLibrary.LibItem libItemI4         = null;
      BasicLibrary.LibItem libItemI4Swapped  = null;
      BasicLibrary.LibItem libItemD6         = null;
      if( BasicUtil.checkToken( iter, '=' ) ) {
	asmLabelI2        = "I2_EQ_I2_I2";
	asmLabelI2Swapped = "I2_EQ_I2_I2";
	asmLabelI4        = "I2_EQ_I4_I4";
	asmLabelI4Swapped = "I2_EQ_I4_I4";
	asmLabelD6        = "I2_EQ_D6_D6";
	libItemI2         = BasicLibrary.LibItem.I2_EQ_I2_I2;
	libItemI2Swapped  = BasicLibrary.LibItem.I2_EQ_I2_I2;
	libItemI4         = BasicLibrary.LibItem.I2_EQ_I4_I4;
	libItemI4Swapped  = BasicLibrary.LibItem.I2_EQ_I4_I4;
	libItemD6         = BasicLibrary.LibItem.I2_EQ_D6_D6;
      } else if( BasicUtil.checkToken( iter, "<>" ) ) {
	asmLabelI2        = "I2_NE_I2_I2";
	asmLabelI2Swapped = "I2_NE_I2_I2";
	asmLabelI4        = "I2_NE_I4_I4";
	asmLabelI4Swapped = "I2_NE_I4_I4";
	asmLabelD6        = "I2_NE_D6_D6";
	libItemI2         = BasicLibrary.LibItem.I2_NE_I2_I2;
	libItemI2Swapped  = BasicLibrary.LibItem.I2_NE_I2_I2;
	libItemI4         = BasicLibrary.LibItem.I2_NE_I4_I4;
	libItemI4Swapped  = BasicLibrary.LibItem.I2_NE_I4_I4;
	libItemD6         = BasicLibrary.LibItem.I2_NE_D6_D6;
      } else if( BasicUtil.checkToken( iter, "<=" ) ) {
	asmLabelI2        = "I2_LE_I2_I2";
	asmLabelI2Swapped = "I2_GT_I2_I2";
	asmLabelI4        = "I2_LE_I4_I4";
	asmLabelI4Swapped = "I2_GT_I4_I4";
	asmLabelD6        = "I2_LE_D6_D6";
	libItemI2         = BasicLibrary.LibItem.I2_LE_I2_I2;
	libItemI2Swapped  = BasicLibrary.LibItem.I2_GT_I2_I2;
	libItemI4         = BasicLibrary.LibItem.I2_LE_I4_I4;
	libItemI4Swapped  = BasicLibrary.LibItem.I2_GT_I4_I4;
	libItemD6         = BasicLibrary.LibItem.I2_LE_D6_D6;
      } else if( BasicUtil.checkToken( iter, '<' ) ) {
	asmLabelI2        = "I2_LT_I2_I2";
	asmLabelI2Swapped = "I2_GE_I2_I2";
	asmLabelI4        = "I2_LT_I4_I4";
	asmLabelI4Swapped = "I2_GE_I4_I4";
	asmLabelD6        = "I2_LT_D6_D6";
	libItemI2         = BasicLibrary.LibItem.I2_LT_I2_I2;
	libItemI2Swapped  = BasicLibrary.LibItem.I2_GE_I2_I2;
	libItemI4         = BasicLibrary.LibItem.I2_LT_I4_I4;
	libItemI4Swapped  = BasicLibrary.LibItem.I2_GE_I4_I4;
	libItemD6         = BasicLibrary.LibItem.I2_LT_D6_D6;
      } else if( BasicUtil.checkToken( iter, ">=" ) ) {
	asmLabelI2        = "I2_GE_I2_I2";
	asmLabelI2Swapped = "I2_LT_I2_I2";
	asmLabelI4        = "I2_GE_I4_I4";
	asmLabelI4Swapped = "I2_LT_I4_I4";
	asmLabelD6        = "I2_GE_D6_D6";
	libItemI2         = BasicLibrary.LibItem.I2_GE_I2_I2;
	libItemI2Swapped  = BasicLibrary.LibItem.I2_LT_I2_I2;
	libItemI4         = BasicLibrary.LibItem.I2_GE_I4_I4;
	libItemI4Swapped  = BasicLibrary.LibItem.I2_LT_I4_I4;
	libItemD6         = BasicLibrary.LibItem.I2_GE_D6_D6;
      } else if( BasicUtil.checkToken( iter, '>' ) ) {
	asmLabelI2        = "I2_GT_I2_I2";
	asmLabelI2Swapped = "I2_LE_I2_I2";
	asmLabelI4        = "I2_GT_I4_I4";
	asmLabelI4Swapped = "I2_LE_I4_I4";
	asmLabelD6        = "I2_GT_D6_D6";
	libItemI2         = BasicLibrary.LibItem.I2_GT_I2_I2;
	libItemI2Swapped  = BasicLibrary.LibItem.I2_LE_I2_I2;
	libItemI4         = BasicLibrary.LibItem.I2_GT_I4_I4;
	libItemI4Swapped  = BasicLibrary.LibItem.I2_LE_I4_I4;
	libItemD6         = BasicLibrary.LibItem.I2_GT_D6_D6;
      }
      if( (asmLabelI2 != null)           && (libItemI2 != null)
	  && (asmLabelI2Swapped != null) && (libItemI2Swapped != null)
	  && (asmLabelI4 != null)        && (libItemI4 != null)
	  && (asmLabelI4Swapped != null) && (libItemI4Swapped != null)
	  && (asmLabelD6 != null)        && (libItemD6 != null) )
      {
	int                    pos = asmOut.length();
	BasicCompiler.DataType dt2 = parseShiftExpr(
						compiler,
						iter,
						context,
						prefRetType );
	if( rv.equals( BasicCompiler.DataType.DEC6 )
	    || dt2.equals( BasicCompiler.DataType.DEC6 ) )
	{
	  String oldOp2Code = asmOut.cut( pos );

	  // Op1
	  switch( rv ) {
	    case INT2:
	      asmOut.append( "\tCALL\tF_D6_CDEC_I2\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.F_D6_CDEC_I2 );
	      context.setMAccuDirty();
	      break;
	    case INT4:
	      asmOut.append( "\tCALL\tF_D6_CDEC_I4\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.F_D6_CDEC_I4 );
	      context.setMAccuDirty();
	      break;
	    case DEC6:
	      // leer
	      break;
	    default:
	      BasicUtil.throwNumericExprExpected();
	  }
	  String ldOp1Code = BasicUtil.convertLastCodeToD6LoadOp1( compiler );
	  if( ldOp1Code == null ) {
	    asmOut.append_PUSH_D6Accu( compiler );
	  }

	  // Op2
	  asmOut.append( oldOp2Code );
	  switch( dt2 ) {
	    case INT2:
	      asmOut.append( "\tCALL\tF_D6_CDEC_I2\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.F_D6_CDEC_I2 );
	      context.setMAccuDirty();
	      break;
	    case INT4:
	      asmOut.append( "\tCALL\tF_D6_CDEC_I4\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.F_D6_CDEC_I4 );
	      context.setMAccuDirty();
	      break;
	    case DEC6:
	      // leer
	      break;
	    default:
	      BasicUtil.throwNumericExprExpected();
	  }
	  if( ldOp1Code != null ) {
	    asmOut.append( ldOp1Code );
	  } else {
	    asmOut.append_POP_D6Op1( compiler );
	  }

	  // Operation
	  asmOut.append( "\tCALL\t" );
	  asmOut.append( asmLabelD6 );
	  asmOut.newLine();
	  compiler.addLibItem( libItemD6 );
	  compiler.setErrVarsSet();
	  context.setMAccuDirty();
	  rv = BasicCompiler.DataType.INT2;
	}
	else if( rv.equals( BasicCompiler.DataType.INT4 )
		 || dt2.equals( BasicCompiler.DataType.INT4 ) )
	{
	  // 32-Bit-Operation
	  String oldOp2Code = asmOut.cut( pos );
	  switch( rv ) {
	    case INT2:
	      asmOut.append( "\tCALL\tI4_HL_TO_DEHL\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.I4_HL_TO_DEHL );
	      break;
	    case INT4:
	      // leer
	      break;
	    default:
	      BasicUtil.throwIntOrLongExprExpected();
	  }
	  boolean regs2Used = BasicUtil.usesSecondCpuRegSet( oldOp2Code );
	  if( regs2Used ) {
	    asmOut.append_PUSH_DEHL();
	  } else {
	    asmOut.append( "\tEXX\n" );
	  }
	  asmOut.append( oldOp2Code );
	  switch( dt2 ) {
	    case INT2:
	      asmOut.append( "\tCALL\tI4_HL_TO_DEHL\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.I4_HL_TO_DEHL );
	      break;
	    case INT4:
	      // leer
	      break;
	    default:
	      BasicUtil.throwIntOrLongExprExpected();
	  }
	  if( regs2Used ) {
	    asmOut.append_POP_DE2HL2( compiler );
	  }
	  asmOut.append( "\tCALL\t" );
	  asmOut.append( asmLabelI4 );
	  asmOut.newLine();
	  compiler.addLibItem( libItemI4 );
	  rv = BasicCompiler.DataType.INT2;
	}
	else if( rv.equals( BasicCompiler.DataType.INT2 )
		 || dt2.equals( BasicCompiler.DataType.INT2 ) )
	{
	  // 16-Bit-Operation
	  String oldCode = asmOut.cut( pos );
	  String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	  if( newCode != null ) {
	    asmOut.append( newCode );
	    asmOut.append( "\tCALL\t" );
	    asmOut.append( asmLabelI2 );
	    asmOut.newLine();
	    compiler.addLibItem( libItemI2 );
	  } else {
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tPOP\tDE\n"
			+ "\tCALL\t" );
	    asmOut.append( asmLabelI2Swapped );
	    asmOut.newLine();
	    compiler.addLibItem( libItemI2Swapped );
	  }
	  rv = BasicCompiler.DataType.INT2;
	}
      }
    }
    return rv;
  }


  private static BasicCompiler.DataType parseShiftExpr(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
  {
    AsmCodeBuf             asmOut = compiler.getCodeBuf();
    BasicCompiler.DataType rv     = parseAddExpr(
						compiler,
						iter,
						context,
						prefRetType );
    for(;;) {
      if( BasicUtil.checkKeyword( iter, "SHL" ) ) {
	if( !rv.equals( BasicCompiler.DataType.INT2 )
	    && !rv.equals( BasicCompiler.DataType.INT4 ) )
	{
	  BasicUtil.throwOp1DataTypeNotAllowed();
	}
	int pos = asmOut.length();
	if( !parseAddExpr(
			compiler,
			iter,
			context,
			BasicCompiler.DataType.INT2 ).equals(
					BasicCompiler.DataType.INT2 ) )
	{
	  BasicUtil.throwInt2ExprExpected();
	}
	String oldCode = asmOut.cut( pos );
	if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	  asmOut.append( "\tSLA\tL\n"
			+ "\tRL\tH\n" );
	  if( rv.equals( BasicCompiler.DataType.INT4 ) ) {
	    asmOut.append( "\tEXX\n"
			+ "\tRL\tL\n"
			+ "\tRL\tH\n"
			+ "\tEXX\n" );
	  }
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
	  if( rv.equals( BasicCompiler.DataType.INT4 ) ) {
	    asmOut.append( "\tCALL\tI4_SHL_I4_I2\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.I4_SHL_I4_I2 );
	  } else {
	    asmOut.append( "\tCALL\tI2_SHL_I2_I2\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.I2_SHL_I2_I2 );
	  }
	}
      } else if( BasicUtil.checkKeyword( iter, "SHR" ) ) {
	if( !rv.equals( BasicCompiler.DataType.INT2 )
	    && !rv.equals( BasicCompiler.DataType.INT4 ) )
	{
	  BasicUtil.throwOp1DataTypeNotAllowed();
	}
	int pos = asmOut.length();
	if( !parseAddExpr(
			compiler,
			iter,
			context,
			BasicCompiler.DataType.INT2 ).equals(
					BasicCompiler.DataType.INT2 ) )
	{
	  BasicUtil.throwInt2ExprExpected();
	}
	String oldCode = asmOut.cut( pos );
	if( oldCode.equals( "\tLD\tHL,0001H\n" ) ) {
	  asmOut.append( "\tSRL\tH\n"
			+ "\tRR\tL\n" );
	  if( rv.equals( BasicCompiler.DataType.INT4 ) ) {
	    asmOut.append( "\tEXX\n"
			+ "\tRR\tL\n"
			+ "\tRR\tH\n"
			+ "\tEXX\n" );
	  }
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
	  if( rv.equals( BasicCompiler.DataType.INT4 ) ) {
	    asmOut.append( "\tCALL\tI4_SHR_I4_I2\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.I4_SHR_I4_I2 );
	  } else {
	    asmOut.append( "\tCALL\tI2_SHR_I2_I2\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.I2_SHR_I2_I2 );
	  }
	}
      } else {
	break;
      }
    }
    return rv;
  }


  private static BasicCompiler.DataType parseAddExpr(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
  {
    BasicCompiler.DataType dataType = parseBiOpNumExpr(
		compiler,
		iter,
		context,
		prefRetType,
		new OpParser() {
			@Override
			public BasicCompiler.DataType parseOp(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
			{
			  return parseMulExpr(
					compiler,
					iter,
					context,
					prefRetType );
			}
		},
		new OpInfo(
			"+",
			"\tCALL\tI2_ADD_I2_I2\n",
			BasicLibrary.LibItem.I2_ADD_I2_I2,
			"\tCALL\tI4_ADD_I4_I4\n",
			BasicLibrary.LibItem.I4_ADD_I4_I4,
			"\tCALL\tD6_ADD_D6_D6\n",
			BasicLibrary.LibItem.D6_ADD_D6_D6,
			true ),
		new OpInfo(
			"-",
			"\tCALL\tI2_SUB_I2_I2\n",
			BasicLibrary.LibItem.I2_SUB_I2_I2,
			"\tCALL\tI4_SUB_I4_I4\n",
			BasicLibrary.LibItem.I4_SUB_I4_I4,
			"\tCALL\tD6_SUB_D6_D6\n",
			BasicLibrary.LibItem.D6_SUB_D6_D6,
			false ),
		new OpInfo(
			"ADD",
			"\tADD\tHL,DE\n",
			null,
			null,
			null,
			null,
			null,
			true ),
		new OpInfo(
			"SUB",
			"\tOR\tA\n"
				+ "\tSBC\tHL,DE\n",
			null,
			null,
			null,
			null,
			null,
			false ) );

    // Optimieren
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    boolean    done   = false;

    // "X ADD Y" optimieren, wenn 1 <= Y <= 3
    for( int v = 1; v < 4; v++ ) {
      if( asmOut.cutIfEndsWith(
			String.format(
				"\tLD\tDE,%04XH\n"
					+ "\tADD\tHL,DE\n",
				v  ) ) )
      {
	for( int i = 0; i < v; i++ ) {
	  asmOut.append( "\tINC\tHL\n" );
	}
	done = true;
	break;
      }
    }

    // "X SUB Y" optimieren, wenn 1 <= Y <= 5
    if( !done ) {
      for( int v = 1; v < 6; v++ ) {
	if( asmOut.cutIfEndsWith(
			String.format(
				"\tLD\tDE,%04XH\n"
					+ "\tOR\tA\n"
					+ "\tSBC\tHL,DE\n",
				v  ) ) )
	{
	  for( int i = 0; i < v; i++ ) {
	    asmOut.append( "\tDEC\tHL\n" );
	  }
	  break;
	}
      }
    }
    return dataType;
  }


  private static BasicCompiler.DataType parseMulExpr(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
  {
    return parseBiOpNumExpr(
		compiler,
		iter,
		context,
		prefRetType,
		new OpParser() {
			@Override
			public BasicCompiler.DataType parseOp(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
			{
			  return parseUnaryExpr(
					compiler,
					iter,
					context,
					prefRetType );
			}
		},
		new OpInfo(
			"*",
			"\tCALL\tI2_MUL_I2_I2\n",
			BasicLibrary.LibItem.I2_MUL_I2_I2,
			"\tCALL\tI4_MUL_I4_I4\n",
			BasicLibrary.LibItem.I4_MUL_I4_I4,
			"\tCALL\tD6_MUL_D6_D6\n",
			BasicLibrary.LibItem.D6_MUL_D6_D6,
			true ),
		new OpInfo(
			"/",
			"\tCALL\tI2_DIV_I2_I2\n",
			BasicLibrary.LibItem.I2_DIV_I2_I2,
			"\tCALL\tI4_DIV_I4_I4\n",
			BasicLibrary.LibItem.I4_DIV_I4_I4,
			"\tCALL\tD6_DIV_D6_D6\n",
			BasicLibrary.LibItem.D6_DIV_D6_D6,
			false ),
		new OpInfo(
			"MOD",
			"\tCALL\tI2_MOD_I2_I2\n",
			BasicLibrary.LibItem.I2_MOD_I2_I2,
			"\tCALL\tI4_MOD_I4_I4\n",
			BasicLibrary.LibItem.I4_MOD_I4_I4,
			null,
			null,
			false ) );
  }


  private static BasicCompiler.DataType parseUnaryExpr(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
  {
    BasicCompiler.DataType rv     = null;
    AsmCodeBuf             asmOut = compiler.getCodeBuf();
    char                   ch     = BasicUtil.skipSpaces( iter );
    if( ch == '-' ) {
      iter.next();
      NumericValue value = NumericValue.readNumber(
						compiler,
						iter,
						prefRetType );
      if( value != null ) {
	value = value.negate();
	value.writeCode_LD_Reg_DirectValue( compiler );
	rv = value.getDataType();
      } else {
	rv = parsePrimExpr( compiler, iter, context, prefRetType );
	switch( rv ) {
	  case INT2:
	    asmOut.append( "\tCALL\tI2_NEG_HL\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.I2_NEG_HL );
	    break;
	  case INT4:
	    asmOut.append( "\tCALL\tI4_NEG_DEHL\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.I4_NEG_DEHL );
	    break;
	  case DEC6:
	    asmOut.append( "\tCALL\tD6_NEG_ACCU\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.D6_NEG_ACCU );
	    context.setMAccuDirty();
	    break;
	}
      }
    } else {
      if( ch == '+' ) {
	iter.next();
      }
      rv = parsePrimExpr( compiler, iter, context, prefRetType );
    }
    if( rv == null ) {
      BasicUtil.throwNumericExprExpected();
    }
    return rv;
  }


  private static BasicCompiler.DataType parsePrimExpr(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType )
							throws PrgException
  {
    BasicCompiler.DataType rv     = null;
    AsmCodeBuf             asmOut = compiler.getCodeBuf();
    char                   ch     = BasicUtil.skipSpaces( iter );
    if( ch == '(' ) {
      iter.next();
      rv = parseNumericExpr( compiler, iter, context, prefRetType );
      BasicUtil.parseToken( iter, ')' );
    } else {
      NumericValue value = NumericValue.checkLiteral(
						compiler,
						iter,
						prefRetType,
						true );
      if( value != null ) {
	rv = value.getDataType();
	value.writeCode_LD_Reg_DirectValue( compiler );
      } else {
	String name = BasicUtil.checkIdentifier( iter );
	if( name != null ) {
	  // auf Konstante pruefen
	  rv = checkNumericDirectValue( compiler, context, name );
	  if( rv == null ) {
	    // auf Systemvariable pruefen
	    AbstractTarget target = compiler.getTarget();
	    if( name.equals( "CRSLIN" ) ) {
	      asmOut.append( "\tCALL\tXCRSLIN\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.XCRSLIN );
	      rv = BasicCompiler.DataType.INT2;
	    } else if( name.equals( "CRSPOS" ) ) {
	      asmOut.append( "\tCALL\tXCRSPOS\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.XCRSPOS );
	      rv = BasicCompiler.DataType.INT2;
	    } else if( name.equals( "ERR" ) ) {
	      asmOut.append( "\tLD\tHL,(M_ERROR_NUM)\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.M_ERROR_NUM );
	      rv = BasicCompiler.DataType.INT2;
	    } else if( name.equals( "H_CHAR" ) ) {
	      target.appendHCharTo( asmOut );
	      rv = BasicCompiler.DataType.INT2;
	    } else if( name.equals( "H_PIXEL" ) ) {
	      target.appendHPixelTo( asmOut );
	      rv = BasicCompiler.DataType.INT2;
	    } else if( name.equals( "W_CHAR" ) ) {
	      target.appendWCharTo( asmOut );
	      rv = BasicCompiler.DataType.INT2;
	    } else if( name.equals( "W_PIXEL" ) ) {
	      target.appendWPixelTo( asmOut );
	      rv = BasicCompiler.DataType.INT2;
	    } else if( name.equals( "XPOS" ) ) {
	      asmOut.append( "\tLD\tHL,(M_XPOS)\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.M_XYPOS );
	      rv = BasicCompiler.DataType.INT2;
	    } else if( name.equals( "YPOS" ) ) {
	      asmOut.append( "\tLD\tHL,(M_YPOS)\n" );
	      compiler.addLibItem( BasicLibrary.LibItem.M_XYPOS );
	      rv = BasicCompiler.DataType.INT2;
	    } else {
	      // auf Systemfunktion pruefen
	      rv = BasicFuncParser.checkParseNumericFunction(
							compiler,
							iter,
							context,
							prefRetType,
							name );
	      if( rv == null ) {
		// auf benutzerdefinierte Funktion pruefen
		CallableEntry entry = compiler.getCallableEntry( name );
		if( entry != null ) {
		  if( entry instanceof FunctionEntry ) {
		    rv = ((FunctionEntry) entry).getReturnType();
		    BasicUtil.ensureNumericType( rv );
		    compiler.parseCallableCall( iter, context, entry );
		  } else {
		    throw new PrgException( "Aufruf einer Prozedur"
				+ " an dieser Stelle nicht erlaubt" );
		  }
		} else {
		  SimpleVarInfo varInfo = compiler.checkVariable(
					iter,
					context,
					name,
					BasicCompiler.AccessMode.READ );
		  if( varInfo == null ) {
		    BasicUtil.throwUnknownIdentifier( name );
		  }
		  rv = varInfo.getDataType();
		  varInfo.writeCode_LD_Reg_Var( compiler );
		  BasicUtil.ensureNumericType( rv );
		}
	      }
	    }
	  }
	} else {
	  BasicUtil.throwUnexpectedChar( BasicUtil.skipSpaces( iter ) );
	}
      }
    }
    if( rv == null ) {
      BasicUtil.throwNumericExprExpected();
    }
    return rv;
  }


  private static void throwCondOpExpected() throws PrgException
  {
    throw new PrgException( "Vergleichsoperator erwartet" );
  }
}
