/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Parser fuer BASIC-Ausdruecke
 */

package jkcemu.programming.basic;

import java.text.CharacterIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import jkcemu.programming.PrgException;


public class BasicFuncParser
{
  public static BasicCompiler.DataType checkParseNumericFunction(
				BasicCompiler          compiler,
				CharacterIterator      iter,
				ParseContext           context,
				BasicCompiler.DataType prefRetType,
				String                 name )
							throws PrgException
  {
    BasicCompiler.DataType rv = null;
    if( name != null ) {
      if( name.equals( "ABS" ) ) {
	rv = parseABS( compiler, iter, context, prefRetType );
      } else if( name.equals( "ASC" ) ) {
	rv = parseASC( compiler, iter, context );
      } else if( name.equals( "ASM" ) ) {
	compiler.parseASM( iter, true );
	rv = BasicCompiler.DataType.INT2;
      } else if( name.equals( "CDEC" ) ) {
	rv = parseCDEC( compiler, iter, context );
      } else if( name.equals( "CINT" ) ) {
	rv = parseCINT( compiler, iter, context );
      } else if( name.equals( "CLNG" ) ) {
	rv = parseCLNG( compiler, iter, context );
      } else if( name.equals( "DECVAL" ) ) {
	rv = parseDECVAL( compiler, iter, context );
      } else if( name.equals( "DEEK" ) ) {
	rv = parseDEEK( compiler, iter, context );
      } else if( name.equals( "EOF" ) ) {
	rv = parseEOF( compiler, iter, context );
      } else if( name.equals( "FRAC" ) ) {
	rv = parseFRAC( compiler, iter, context );
      } else if( name.equals( "HIBYTE" ) ) {
	rv = parseHIBYTE( compiler, iter, context );
      } else if( name.equals( "HIBYTE" ) ) {
	rv = parseHIWORD( compiler, iter, context );
      } else if( name.equals( "IN" ) || name.equals( "INP" ) ) {
	rv = parseIN( compiler, iter, context );
      } else if( name.equals( "INSTR" ) ) {
	rv = parseINSTR( compiler, iter, context );
      } else if( name.equals( "INTVAL" ) || name.equals( "VAL" ) ) {
	rv = parseXVAL(
		compiler,
		iter,
		context,
		BasicCompiler.DataType.INT2 );
      } else if( name.equals( "IS_TARGET" ) ) {
	rv = parseIS_TARGET( compiler, iter, context );
      } else if( name.equals( "JOYST" ) ) {
	rv = parseJOYST( compiler, iter, context );
      } else if( name.equals( "LEN" ) ) {
	rv = parseLEN( compiler, iter, context );
      } else if( name.equals( "LNGVAL" ) ) {
	rv = parseXVAL(
		compiler,
		iter,
		context,
		BasicCompiler.DataType.INT4 );
      } else if( name.equals( "LOBYTE" ) ) {
	rv = parseLOBYTE( compiler, iter, context );
      } else if( name.equals( "LOWORD" ) ) {
	rv = parseLOWORD( compiler, iter, context );
      } else if( name.equals( "MAX" ) ) {
	rv = parseMinMax(
			compiler,
			iter,
			context,
			prefRetType,
			"\tCALL\tI2_MAX_I2_I2\n",
			BasicLibrary.LibItem.I2_MAX_I2_I2,
			"\tCALL\tI4_MAX_I4_I4\n",
			BasicLibrary.LibItem.I4_MAX_I4_I4,
			"\tCALL\tD6_MAX_D6_D6\n",
			BasicLibrary.LibItem.D6_MAX_D6_D6 );
      } else if( name.equals( "MIN" ) ) {
	rv = parseMinMax(
			compiler,
			iter,
			context,
			prefRetType,
			"\tCALL\tI2_MIN_I2_I2\n",
			BasicLibrary.LibItem.I2_MIN_I2_I2,
			"\tCALL\tI4_MIN_I4_I4\n",
			BasicLibrary.LibItem.I4_MIN_I4_I4,
			"\tCALL\tD6_MIN_D6_D6\n",
			BasicLibrary.LibItem.D6_MIN_D6_D6 );
      } else if( name.equals( "PEEK" ) ) {
	rv = parsePEEK( compiler, iter, context );
      } else if( name.equals( "POINT" ) ) {
	rv = parsePOINT( compiler, iter, context );
      } else if( name.equals( "POS" ) ) {
	rv = parsePOS( compiler, iter, context );
      } else if( name.equals( "PTEST" ) ) {
	rv = parsePTEST( compiler, iter, context );
      } else if( name.equals( "RND" ) ) {
	rv = parseRND( compiler, iter, context );
      } else if( name.equals( "ROUND" ) ) {
	rv = parseROUND( compiler, iter, context );
      } else if( name.equals( "SCALE" ) ) {
	rv = parseSCALE( compiler, iter, context );
      } else if( name.equals( "SGN" ) ) {
	rv = parseSGN( compiler, iter, context );
      } else if( name.equals( "SQR" ) ) {
	rv = parseSQR( compiler, iter, context, prefRetType );
      } else if( name.equals( "STRPTR" ) ) {
	rv = parseSTRPTR( compiler, iter, context );
      } else if( name.equals( "TRUNC" ) ) {
	rv = parseTRUNC( compiler, iter, context );
      } else if( name.equals( "USR" ) ) {
	rv = parseUSR( compiler, iter, context );
      } else if( name.equals( "USR0" ) ) {
	rv = parseUSR( compiler, iter, context, 0 );
      } else if( name.equals( "USR1" ) ) {
	rv = parseUSR( compiler, iter, context, 1 );
      } else if( name.equals( "USR2" ) ) {
	rv = parseUSR( compiler, iter, context, 2 );
      } else if( name.equals( "USR3" ) ) {
	rv = parseUSR( compiler, iter, context, 3 );
      } else if( name.equals( "USR4" ) ) {
	rv = parseUSR( compiler, iter, context, 4 );
      } else if( name.equals( "USR5" ) ) {
	rv = parseUSR( compiler, iter, context, 5 );
      } else if( name.equals( "USR6" ) ) {
	rv = parseUSR( compiler, iter, context, 6 );
      } else if( name.equals( "USR7" ) ) {
	rv = parseUSR( compiler, iter, context, 7 );
      } else if( name.equals( "USR8" ) ) {
	rv = parseUSR( compiler, iter, context, 8 );
      } else if( name.equals( "USR9" ) ) {
	rv = parseUSR( compiler, iter, context, 9 );
      } else if( name.equals( "VARPTR" ) ) {
	rv = parseVARPTR( compiler, iter, context );
      }
    }
    return rv;
  }


  public static boolean checkParseStringFunction(
				BasicCompiler     compiler,
				CharacterIterator iter,
				ParseContext      context,
				String            name ) throws PrgException
  {
    boolean rv = false;
    if( name != null ) {
      rv = true;
      if( name.equals( "BIN$" ) ) {
	parseStrBinary(
		compiler,
		iter,
		context,
		"F_S_BIN_I2",
		BasicLibrary.LibItem.F_S_BIN_I2,
		"F_S_BIN_I2_I2",
		BasicLibrary.LibItem.F_S_BIN_I2_I2,
		"F_S_BIN_I4",
		BasicLibrary.LibItem.F_S_BIN_I4,
		"F_S_BIN_I4_I2",
		BasicLibrary.LibItem.F_S_BIN_I4_I2 );
      } else if( name.equals( "CHR$" ) ) {
	parseStrCHR( compiler, iter, context );
      } else if( name.equals( "DATETIME$" ) ) {
	parseStrDATETIME( compiler, iter, context );
      } else if( name.equals( "ERR$" ) ) {
	parseStrERR( compiler, iter );
      } else if( name.equals( "HEX$" ) ) {
	parseStrBinary(
		compiler,
		iter,
		context,
		"F_S_HEX_I2",
		BasicLibrary.LibItem.F_S_HEX_I2,
		"F_S_HEX_I2_I2",
		BasicLibrary.LibItem.F_S_HEX_I2_I2,
		"F_S_HEX_I4",
		BasicLibrary.LibItem.F_S_HEX_I4,
		"F_S_HEX_I4_I2",
		BasicLibrary.LibItem.F_S_HEX_I4_I2 );
      } else if( name.equals( "INKEY$" ) ) {
	parseStrINKEY( compiler, iter );
      } else if( name.equals( "INPUT$" ) ) {
	parseStrINPUT( compiler, iter, context );
      } else if( name.equals( "LEFT$" ) ) {
	parseStrLEFT( compiler, iter, context );
      } else if( name.equals( "LOWER$" ) || name.equals( "LCASE$" ) ) {
	parseStrLOWER( compiler, iter, context );
      } else if( name.equals( "LTRIM$" ) ) {
	parseStrLTRIM( compiler, iter, context );
      } else if( name.equals( "MEMSTR$" ) ) {
	parseStrMEMSTR( compiler, iter, context );
      } else if( name.equals( "MID$" ) ) {
	parseStrMID( compiler, iter, context );
      } else if( name.equals( "MIRROR$" ) ) {
	parseStrMIRROR( compiler, iter, context );
      } else if( name.equals( "OCT$" ) ) {
	parseStrBinary(
		compiler,
		iter,
		context,
		"F_S_OCT_I2",
		BasicLibrary.LibItem.F_S_OCT_I2,
		"F_S_OCT_I2_I2",
		BasicLibrary.LibItem.F_S_OCT_I2_I2,
		"F_S_OCT_I4",
		BasicLibrary.LibItem.F_S_OCT_I4,
		"F_S_OCT_I4_I2",
		BasicLibrary.LibItem.F_S_OCT_I4_I2 );
      } else if( name.equals( "RIGHT$" ) ) {
	parseStrRIGHT( compiler, iter, context );
      } else if( name.equals( "RTRIM$" ) ) {
	parseStrRTRIM( compiler, iter, context );
      } else if( name.equals( "SPACE$" ) ) {
	parseStrSPACE( compiler, iter, context );
      } else if( name.equals( "STR$" ) ) {
	parseStrSTR( compiler, iter, context );
      } else if( name.equals( "STRING$" ) ) {
	parseStrSTRING( compiler, iter, context );
      } else if( name.equals( "TRIM$" ) ) {
	parseStrTRIM( compiler, iter, context );
      } else if( name.equals( "UPPER$" ) || name.equals( "UCASE$" ) ) {
	parseStrUPPER( compiler, iter, context );
      } else {
	rv = false;
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static BasicCompiler.DataType parseABS(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			ParseContext           context,
			BasicCompiler.DataType prefRetType )
						throws PrgException
  {
    BasicCompiler.DataType rv = BasicCompiler.DataType.INT2;
    BasicUtil.parseToken( iter, '(' );
    switch( BasicExprParser.parseNumericExpr(
					compiler,
					iter,
					context,
					prefRetType ))
    {
      case INT2:
	compiler.getCodeBuf().append( "\tCALL\tI2_ABS_HL\n" );
	compiler.addLibItem( BasicLibrary.LibItem.I2_ABS_HL );
	break;
      case INT4:
	compiler.getCodeBuf().append( "\tCALL\tI4_ABS_DEHL\n" );
	compiler.addLibItem( BasicLibrary.LibItem.I4_ABS_DEHL );
	rv = BasicCompiler.DataType.INT4;
	break;
      case DEC6:
	compiler.getCodeBuf().append( "\tCALL\tF_D6_ABS_D6\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_D6_ABS_D6 );
	context.setMAccuDirty();
	rv = BasicCompiler.DataType.DEC6;
	break;
      default:
	BasicUtil.throwNumericExprExpected();
    }
    BasicUtil.parseToken( iter, ')' );
    return rv;
  }


  private static BasicCompiler.DataType parseASC(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    BasicUtil.parseToken( iter, ')' );
    compiler.getCodeBuf().append( "\tLD\tL,(HL)\n"
				+ "\tLD\tH,00H\n" );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseCDEC(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    switch( BasicExprParser.parseNumericExpr(
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
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.DEC6;
  }


  private static BasicCompiler.DataType parseCINT(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    switch( BasicExprParser.parseNumericExpr(
					compiler,
					iter,
					context,
					BasicCompiler.DataType.INT2 ) )
    {
      case INT2:
	// leer
	break;
      case INT4:
	compiler.getCodeBuf().append( "\tCALL\tF_I2_CINT_I4\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_I2_CINT_I4 );
	break;
      case DEC6:
	compiler.getCodeBuf().append( "\tCALL\tF_I2_CINT_D6\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_I2_CINT_D6 );
	context.setMAccuDirty();
	break;
      default:
	BasicUtil.throwNumericExprExpected();
    }
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseCLNG(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    switch( BasicExprParser.parseNumericExpr(
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
      case DEC6:
	compiler.getCodeBuf().append( "\tCALL\tF_I4_CLNG_D6\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_I4_CLNG_D6 );
	context.setMAccuDirty();
	break;
      default:
	BasicUtil.throwNumericExprExpected();
    }
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.INT4;
  }


  private static BasicCompiler.DataType parseDECVAL(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			ParseContext           context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    compiler.getCodeBuf().append( "\tCALL\tF_D6_VAL_S\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_D6_VAL_S );
    compiler.setErrVarsSet();
    context.setMAccuDirty();
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.DEC6;
  }


  private static BasicCompiler.DataType parseDEEK(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    int        pos    = asmOut.length();
    BasicExprParser.parseEnclosedInt2Expr( compiler, iter, context );
    Integer addr = BasicUtil.removeLastCodeIfConstExpr( asmOut, pos );
    if( addr != null ) {
      asmOut.append( "\tLD\tHL,(" );
      asmOut.appendHex4( addr.intValue() );
      asmOut.append( ")\n" );
    } else {
      asmOut.append( "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n" );
    }
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseEOF(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicUtil.checkToken( iter, '#' );
    compiler.parseIOChannelNumToPtrFldAddrInHL( iter, context, null );
    compiler.getCodeBuf().append( "\tCALL\tIOEOF\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.IOEOF );
    compiler.setErrVarsSet();
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseFRAC(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseDec6Expr( compiler, iter, context );
    asmOut.append( "\tCALL\tF_D6_FRAC_D6\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_D6_FRAC_D6 );
    context.setMAccuDirty();
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.DEC6;
  }


  private static BasicCompiler.DataType parseHIBYTE(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicExprParser.parseEnclosedInt2Expr( compiler, iter, context );
    compiler.getCodeBuf().append( "\tLD\tL,H\n"
				+ "\tLD\tH,00H\n" );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseHIWORD(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicExprParser.parseEnclosedInt4Expr( compiler, iter, context );
    compiler.getCodeBuf().append( "\tCALL\tF_I4_HIWORD_I4\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_I4_HIWORD_I4 );
    return BasicCompiler.DataType.INT4;
  }


  private static BasicCompiler.DataType parseIN(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseInt2Expr( compiler, iter, context );
    BasicUtil.parseToken( iter, ')' );
    if( !BasicUtil.replaceLastCodeFrom_LD_HL_To_BC( compiler ) ) {
      asmOut.append( "\tLD\tB,H\n"
		+ "\tLD\tC,L\n" );
    }
    asmOut.append( "\tIN\tL,(C)\n"
		+ "\tLD\tH,0\n" );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseINSTR(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut        = compiler.getCodeBuf();
    boolean    hasStartPos   = false;
    Integer    startPosValue = null;
    String     startPosText  = null;
    BasicUtil.parseToken( iter, '(' );
    String text = BasicUtil.checkStringLiteral( compiler, iter );
    if( text == null ) {
      hasStartPos = !BasicExprParser.checkParseStringPrimExpr(
							compiler,
							iter,
							context );
    }
    if( hasStartPos ) {
      int pos = asmOut.length();
      BasicExprParser.parseInt2Expr( compiler, iter, context );
      startPosValue = BasicUtil.removeLastCodeIfConstExpr( asmOut, pos );
      if( startPosValue != null ) {
	if( startPosValue.intValue() <= 0 ) {
	  BasicUtil.throwIndexOutOfRange();
	}
      } else {
	String oldCode = asmOut.cut( pos ); 
	startPosText   = BasicUtil.convertCodeToValueInBC( oldCode );
	if( startPosText == null ) {
	  asmOut.append( oldCode );
	  asmOut.append( "\tPUSH\tHL\n" );
	}
      }
      BasicUtil.parseToken( iter, ',' );
      text = BasicUtil.checkStringLiteral( compiler, iter );
      if( text == null ) {
	BasicExprParser.parseStringPrimExpr( compiler, iter, context );
      }
    }
    BasicUtil.parseToken( iter, ',' );
    String pattern = BasicUtil.checkStringLiteral( compiler, iter );
    if( pattern != null ) {
      if( text != null ) {
	asmOut.append( "\tLD\tHL," );
	asmOut.append( compiler.getStringLiteralLabel( text ) );
	asmOut.newLine();
      }
      asmOut.append( "\tLD\tDE," );
      asmOut.append( compiler.getStringLiteralLabel( pattern ) );
      asmOut.newLine();
    } else {
      if( text == null ) {
	asmOut.append( "\tPUSH\tHL\n" );
      }
      BasicExprParser.parseStringPrimExpr( compiler, iter, context );
      asmOut.append( "\tEX\tDE,HL\n" );
      if( text != null ) {
	asmOut.append( "\tLD\tHL," );
	asmOut.append( compiler.getStringLiteralLabel( text ) );
	asmOut.newLine();
      } else {
	asmOut.append( "\tPOP\tHL\n" );
      }
    }
    if( hasStartPos ) {
      if( startPosValue != null ) {
	asmOut.append_LD_BC_nn( startPosValue.intValue() );
      } else if( startPosText != null ) {
	asmOut.append( startPosText );
      } else {
	asmOut.append( "\tPOP\tBC\n" );
      }
      asmOut.append( "\tCALL\tF_I2_INSTR_N\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_I2_INSTR_N );
    } else {
      asmOut.append( "\tCALL\tF_I2_INSTR\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_I2_INSTR );
    }
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseIS_TARGET(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    int[]      targetIDs = compiler.getTarget().getTargetIDs();
    AsmCodeBuf asmOut    = compiler.getCodeBuf();
    int        pos       = asmOut.length();
    BasicExprParser.parseEnclosedInt2Expr( compiler, iter, context );
    Integer value = BasicUtil.removeLastCodeIfConstExpr( asmOut, pos );
    if( value != null ) {
      int  rv = 0;
      if( targetIDs != null ) {
	for( int targetID : targetIDs ) {
	  if( targetID == value.intValue() ) {
	    rv = -1;
	    break;
	  }
	}
      }
      asmOut.append_LD_HL_nn( rv );
    } else {
      if( targetIDs.length == 1 ) {
	asmOut.append_LD_DE_nn( targetIDs[ 0 ] );
	asmOut.append( "\tCALL\tI2_EQ_I2_I2 \n" );
	compiler.addLibItem( BasicLibrary.LibItem.I2_EQ_I2_I2 );
      } else if( targetIDs.length > 1 ) {
	asmOut.append( "\tCALL\tF_I2_IS_TARGET_I2\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_I2_IS_TARGET_I2 );
      }
    }
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseJOYST(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicExprParser.parseEnclosedInt2Expr( compiler, iter, context );
    compiler.getCodeBuf().append( "\tCALL\tF_I2_JOY_I2\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_I2_JOY_I2 );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseLEN(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    String text = BasicUtil.checkStringLiteral( compiler, iter );
    if( text != null ) {
      asmOut.append_LD_HL_nn( text.length() );
    } else {
      BasicExprParser.parseStringPrimExpr( compiler, iter, context );
      asmOut.append( "\tCALL\tF_I2_LEN\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_I2_LEN );
    }
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseLOBYTE(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    switch( BasicExprParser.parseNumericExpr(
					compiler,
					iter,
					context,
					BasicCompiler.DataType.INT2 ) )
    {
      case INT2:
      case INT4:
	compiler.getCodeBuf().append( "\tLD\tH,00H\n" );
	break;
      default:
	BasicUtil.throwIntOrLongExprExpected();
    }
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseLOWORD(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicExprParser.parseEnclosedInt4Expr( compiler, iter, context );
    compiler.getCodeBuf().append( "\tCALL\tF_I4_LOWORD_I4\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_I4_LOWORD_I4 );
    return BasicCompiler.DataType.INT4;
  }


  private static BasicCompiler.DataType parseMinMax(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			ParseContext           context,
			BasicCompiler.DataType prefRetType,
			String                 asmCodeI2,
			BasicLibrary.LibItem   libItemI2,
			String                 asmCodeI4,
			BasicLibrary.LibItem   libItemI4,
			String                 asmCodeD6,
			BasicLibrary.LibItem   libItemD6 )
						throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicCompiler.DataType rv = BasicExprParser.parseBiOpNumExpr(
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
			  return BasicExprParser.parseNumericExpr(
							compiler,
							iter,
							context,
							prefRetType );
			}
		},
		new OpInfo(
			",",
			asmCodeI2,
			libItemI2,
			asmCodeI4,
			libItemI4,
			asmCodeD6,
			libItemD6,
			false ) );
    BasicUtil.parseToken( iter, ')' );
    return rv;
  }


  private static BasicCompiler.DataType parsePEEK(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicExprParser.parseEnclosedInt2Expr( compiler, iter, context );
    compiler.getCodeBuf().append( "\tLD\tL,(HL)\n"
				+ "\tLD\tH,0\n" );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseRND(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicExprParser.parseEnclosedInt2Expr( compiler, iter, context );
    compiler.getCodeBuf().append( "\tCALL\tF_I2_RND_I2\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_I2_RND_I2 );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parsePOINT(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parse2Int2ArgsTo_DE_HL( compiler, iter, context );
    BasicUtil.parseToken( iter, ')' );
    compiler.getCodeBuf().append( "\tCALL\tXPOINT\n" );
    compiler.addLibItem( BasicLibrary.LibItem.XPOINT );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parsePOS(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    if( BasicUtil.skipSpaces( iter ) != ')' ) {
      int pos = buf.length();
      BasicExprParser.parseInt2Expr( compiler, iter, context );
      buf.cut( pos );
    }
    BasicUtil.parseToken( iter, ')' );
    buf.append( "\tCALL\tXCRSPOS\n" );
    compiler.addLibItem( BasicLibrary.LibItem.XCRSPOS );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parsePTEST(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parse2Int2ArgsTo_DE_HL( compiler, iter, context );
    BasicUtil.parseToken( iter, ')' );
    compiler.getCodeBuf().append( "\tCALL\tXPTEST\n" );
    compiler.addLibItem( BasicLibrary.LibItem.XPTEST );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseROUND(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseDec6Expr( compiler, iter, context );
    if( BasicUtil.checkComma( iter ) ) {
      int pos1 = asmOut.length();
      BasicExprParser.parseInt2Expr( compiler, iter, context );  
      Integer libRoundMode  = null;
      Integer userRoundMode = BasicUtil.removeLastCodeIfConstExpr(
								asmOut,
								pos1 );
      if( userRoundMode != null ) {
	switch( userRoundMode.intValue() ) {
	  case BasicLibrary.ROUND_HALF_DOWN:
	    libRoundMode = -1;
	    break;
	  case BasicLibrary.ROUND_HALF_EVEN:
	    libRoundMode = 0;
	    break;
	  case BasicLibrary.ROUND_HALF_UP:
	    libRoundMode = 1;
	    break;
	}
      }
      if( (userRoundMode != null) && (libRoundMode == null) ) {
	compiler.putWarning( "Rundungsmodus hat ung\u00FCltigen Wert" );
      }
      if( BasicUtil.checkComma( iter ) ) {
	int pos2 = asmOut.length();
	BasicExprParser.parseInt2Expr( compiler, iter, context );
	if( libRoundMode != null ) {
	  asmOut.append_LD_A_n( libRoundMode.intValue() );
	  asmOut.append( "\tCALL\tD6_ROUND_UTIL_HL\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.D6_ROUND_UTIL_HL );
	} else {
	  String digitsCode = asmOut.cut( pos2 );
	  String modeCode   = asmOut.cut( pos1 );
	  if( BasicUtil.isSingleInst_LD_HL_xx( digitsCode ) ) {
	    String newModeCode = BasicUtil.convertCodeToValueInDE(
							modeCode );
	    if( newModeCode != null ) {
	      asmOut.append( newModeCode );
	    } else {
	      asmOut.append( modeCode );
	      asmOut.append( "\tEX\tDE,HL\n" );
	    }
	    asmOut.append( digitsCode );
	  } else {
	    asmOut.append( modeCode );
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( digitsCode );
	    asmOut.append( "\tPOP\tDE\n" );
	  }
	  asmOut.append( "\tCALL\tF_D6_ROUND_D6_I2_I2\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.F_D6_ROUND_D6_I2_I2 );
	}
      } else {
	if( libRoundMode != null ) {
	  asmOut.append_LD_A_n( libRoundMode.intValue() );
	  asmOut.append( "\tCALL\tD6_ROUND_UTIL_00\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.D6_ROUND_UTIL_00 );
	} else {
	  asmOut.append( "\tCALL\tF_D6_ROUND_D6_I2\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.F_D6_ROUND_D6_I2 );
	}
      }
    } else {
      asmOut.append( "\tCALL\tF_D6_ROUND_D6\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_D6_ROUND_D6 );
    }
    context.setMAccuDirty();
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.DEC6;
  }


  private static BasicCompiler.DataType parseSCALE(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseDec6Expr( compiler, iter, context );
    asmOut.append( "\tCALL\tF_I2_SCALE_D6\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_I2_SCALE_D6 );
    context.setMAccuDirty();
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseSGN(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    switch( BasicExprParser.parseNumericExpr(
					compiler,
					iter,
					context,
					null ) )
    {
      case INT2:
	compiler.getCodeBuf().append( "\tCALL\tF_I2_SGN_I2\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_I2_SGN_I2 );
	break;
      case INT4:
	compiler.getCodeBuf().append( "\tCALL\tF_I2_SGN_I4\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_I2_SGN_I4 );
	break;
      case DEC6:
	compiler.getCodeBuf().append( "\tCALL\tF_I2_SGN_D6\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_I2_SGN_D6 );
	context.setMAccuDirty();
	break;
      default:
	BasicUtil.throwNumericExprExpected();
    }
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseSQR(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			ParseContext           context,
			BasicCompiler.DataType prefRetType )
						throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicCompiler.DataType dataType = BasicExprParser.parseNumericExpr(
							compiler,
							iter,
							context,
							prefRetType );
    switch( dataType ) {
      case INT2:
	compiler.getCodeBuf().append( "\tCALL\tF_I2_SQR_I2\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_I2_SQR_I2 );
	break;
      case INT4:
	compiler.getCodeBuf().append( "\tCALL\tF_I4_SQR_I4\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_I4_SQR_I4 );
	break;
      default:
	BasicUtil.throwIntOrLongExprExpected();
    }
    BasicUtil.parseToken( iter, ')' );
    return dataType;
  }


  private static BasicCompiler.DataType parseSTRPTR(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseTRUNC(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseDec6Expr( compiler, iter, context );
    if( BasicUtil.checkComma( iter ) ) {
      int pos = asmOut.length();
      context.resetMAccuDirty();
      BasicExprParser.parseInt2Expr( compiler, iter, context );
      if( context.isMAccuDirty() ) {
	asmOut.insert_PUSH_D6Accu( pos, compiler );
	asmOut.append( "\tEXX\n" );
	asmOut.append_POP_D6Accu( compiler );
	asmOut.append( "\tEXX\n" );
      }
      asmOut.append( "\tCALL\tF_D6_TRUNC_D6_I2\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_D6_TRUNC_D6_I2 );
   } else {
      asmOut.append( "\tCALL\tF_D6_TRUNC_D6\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_D6_TRUNC_D6 );
    }
    compiler.setErrVarsSet();
    context.setMAccuDirty();
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.DEC6;
  }


  private static BasicCompiler.DataType parseUSR(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    int usrNum = BasicUtil.parseUsrNum( iter );
    return parseUSR( compiler, iter, context, usrNum );
  }


  private static BasicCompiler.DataType parseUSR(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context,
			int               usrNum ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicExprParser.parseEnclosedInt2Expr( compiler, iter, context );
    if( !BasicUtil.replaceLastCodeFrom_LD_HL_To_DE( compiler ) ) {
      asmOut.append( "\tEX\tDE,HL\n" );
    }
    boolean insideCallable = (compiler.getEnclosingCallableEntry() != null);
    if( insideCallable ) {
      asmOut.append( "\tPUSH\tIY\n" );
    }
    asmOut.append( "\tLD\tHL,(" );
    asmOut.append( compiler.getUsrLabel( usrNum ) );
    asmOut.append( ")\n"
		+ "\tCALL\tJP_HL\n" );
    if( insideCallable ) {
      asmOut.append( "\tPOP\tIY\n" );
    }
    compiler.addLibItem( BasicLibrary.LibItem.JP_HL );
    compiler.addLibItem( BasicLibrary.LibItem.E_USR_FUNCTION_NOT_DEFINED );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseVARPTR(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    String varName = BasicUtil.checkIdentifier( iter );
    if( varName == null ) {
      BasicUtil.throwVarNameExpected();
    }
    /*
     * Da man mit der Variablenadresse direkt
     * auf die Variable zugreifen kann,
     * wird die Variable als gelesen und beschrieben markiert.
     */
    SimpleVarInfo varInfo = compiler.checkVariable(
				iter,
				context,
				varName,
				BasicCompiler.AccessMode.READ_WRITE );
    if( varInfo != null ) {
      varInfo.ensureAddrInHL( compiler.getCodeBuf() );
    } else {
      BasicUtil.throwVarNameExpected();
    }
    BasicUtil.parseToken( iter, ')' );
    return BasicCompiler.DataType.INT2;
  }


  private static BasicCompiler.DataType parseXVAL(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			ParseContext           context,
			BasicCompiler.DataType retType ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    if( BasicUtil.checkComma( iter ) ) {
      int pos = asmOut.length();
      BasicExprParser.parseInt2Expr( compiler, iter, context );
      Integer radix = BasicUtil.removeLastCodeIfConstExpr( asmOut, pos );
      if( radix != null ) {
	switch( radix.intValue() ) {
	  case 2:
	    switch( retType ) {
	      case INT2:
		asmOut.append( "\tCALL\tF_I2_VAL_BIN_S\n" );
		compiler.addLibItem( BasicLibrary.LibItem.F_I2_VAL_BIN_S );
		break;
	      case INT4:
		asmOut.append( "\tCALL\tF_I4_VAL_BIN_S\n" );
		compiler.addLibItem( BasicLibrary.LibItem.F_I4_VAL_BIN_S );
		break;
	    }
	    break;
	  case 10:
	    switch( retType ) {
	      case INT2:
		asmOut.append( "\tCALL\tF_I2_VAL_DEC_S\n" );
		compiler.addLibItem( BasicLibrary.LibItem.F_I2_VAL_DEC_S );
		break;
	      case INT4:
		asmOut.append( "\tCALL\tF_I4_VAL_DEC_S\n" );
		compiler.addLibItem( BasicLibrary.LibItem.F_I4_VAL_DEC_S );
		break;
	    }
	    break;
	  case 16:
	    switch( retType ) {
	      case INT2:
		asmOut.append( "\tCALL\tF_I2_VAL_HEX_S\n" );
		compiler.addLibItem( BasicLibrary.LibItem.F_I2_VAL_HEX_S );
		break;
	      case INT4:
		asmOut.append( "\tCALL\tF_I4_VAL_HEX_S\n" );
		compiler.addLibItem( BasicLibrary.LibItem.F_I4_VAL_HEX_S );
		break;
	    }
	    break;
	  default:
	    throw new PrgException( "Zahlenbasis nicht unterst\u00FCtzt" );
	}
      } else {
	String oldCode = asmOut.cut( pos );
	String newCode = BasicUtil.convertCodeToValueInDE( oldCode );
	if( newCode != null ) {
	  asmOut.append( newCode );
	} else {
	  asmOut.append( "\tPUSH\tHL\n" );
	  asmOut.append( oldCode );
	  asmOut.append( "\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n" );
	}
	switch( retType ) {
	  case INT2:
	    asmOut.append( "\tCALL\tF_I2_VAL_S_I2\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.F_I2_VAL_S_I2 );
	    break;
	  case INT4:
	    asmOut.append( "\tCALL\tF_I4_VAL_S_I2\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.F_I4_VAL_S_I2 );
	    break;
	}
      }
    } else {
      switch( retType ) {
	case INT2:
	  asmOut.append( "\tCALL\tF_I2_VAL_DEC_S\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.F_I2_VAL_DEC_S );
	  break;
	case INT4:
	  asmOut.append( "\tCALL\tF_I4_VAL_DEC_S\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.F_I4_VAL_DEC_S );
	  break;
	case DEC6:
	  asmOut.append( "\tCALL\tF_D6_VAL_S\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.F_D6_VAL_S );
	  context.setMAccuDirty();
	  break;
      }
    }
    compiler.setErrVarsSet();
    BasicUtil.parseToken( iter, ')' );
    return retType;
  }


  private static void parseStrBinary(
			BasicCompiler        compiler,
			CharacterIterator    iter,
			ParseContext         context,
			String               label_I2,
			BasicLibrary.LibItem libItem_I2,
			String               label_I2_I2,
			BasicLibrary.LibItem libItem_I2_I2,
			String               label_I4,
			BasicLibrary.LibItem libItem_I4,
			String               label_I4_I2,
			BasicLibrary.LibItem libItem_I4_I2 )
						throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicCompiler.DataType dataType = BasicExprParser.parseNumericExpr(
					compiler,
					iter,
					context,
					BasicCompiler.DataType.INT2 );
    if( BasicUtil.checkComma( iter ) ) {
      int pos = asmOut.length();
      BasicExprParser.parseInt2Expr( compiler, iter, context );
      String oldCode = asmOut.cut( pos );
      String newCode = BasicUtil.convertCodeToValueInBC( oldCode );
      if( newCode != null ) {
	asmOut.append( newCode );
      } else {
	switch( dataType ) {
	  case INT2:
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n"
			+ "\tPOP\tHL\n" );
	    break;
	  case INT4:
	    asmOut.append( "\tEXX\n"
			+ "\tPUSH\tHL\n"
			+ "\tEXX\n"
			+ "\tPUSH\n" );
	    asmOut.append( oldCode );
	    asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n"
			+ "\tPOP\tHL\n"
			+ "\tEXX\n"
			+ "\tPOP\tHL\n"
			+ "\tEXX\n" );
	    break;
	}
      }
      switch( dataType ) {
	case INT2:
	  asmOut.append( "\tCALL\t" );
	  asmOut.append( label_I2_I2 );
	  asmOut.newLine();
	  compiler.addLibItem( libItem_I2_I2 );
	  break;
	case INT4:
	  asmOut.append( "\tCALL\t" );
	  asmOut.append( label_I4_I2 );
	  asmOut.newLine();
	  compiler.addLibItem( libItem_I4_I2 );
	  break;
      }
    } else {
      switch( dataType ) {
	case INT2:
	  asmOut.append( "\tCALL\t" );
	  asmOut.append( label_I2 );
	  asmOut.newLine();
	  compiler.addLibItem( libItem_I2 );
	  break;
	case INT4:
	  asmOut.append( "\tCALL\t" );
	  asmOut.append( label_I4 );
	  asmOut.newLine();
	  compiler.addLibItem( libItem_I4 );
	  break;
      }
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrCHR(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    int pos = asmOut.length();
    BasicExprParser.parseEnclosedInt2Expr( compiler, iter, context );
    String oldCode = asmOut.cut( pos );
    String newCode = BasicUtil.convertCodeToValueInA( oldCode );
    if( newCode != null ) {
      asmOut.append( newCode );
      asmOut.append( "\tCALL\tF_S_CHR_A\n" );
    } else {
      asmOut.append( oldCode );
      asmOut.append( "\tCALL\tF_S_CHR_L\n" );
    }
    compiler.addLibItem( BasicLibrary.LibItem.F_S_CHR );
  }


  private static void parseStrDATETIME(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    asmOut.append( "\tCALL\tF_S_DATETIME_S\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_S_DATETIME_S );
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrERR(
			BasicCompiler     compiler,
			CharacterIterator iter )  throws PrgException
  {
    compiler.getCodeBuf().append( "\tLD\tHL,(M_ERROR_TEXT)\n" );
    compiler.addLibItem( BasicLibrary.LibItem.M_ERROR_TEXT );
  }


  private static void parseStrINKEY(
			BasicCompiler     compiler,
			CharacterIterator iter )  throws PrgException
  {
    compiler.getCodeBuf().append( "\tCALL\tS_INKEY\n" );
    compiler.addLibItem( BasicLibrary.LibItem.S_INKEY );
  }


  private static void parseStrINPUT(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context )  throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    int pos = asmOut.length();
    BasicExprParser.parseInt2Expr( compiler, iter, context );
    if( BasicUtil.checkComma( iter ) ) {
      String oldCntCode = asmOut.cut( pos );
      String newCntCode = BasicUtil.convertCodeToValueInBC( oldCntCode );
      AtomicBoolean isConstChannelNum = new AtomicBoolean();
      BasicUtil.checkToken( iter, '#' );
      compiler.parseIOChannelNumToPtrFldAddrInHL(
					iter,
					context,
					isConstChannelNum );
      if( newCntCode != null ) {
	asmOut.append( newCntCode );
      } else {
	String channelCode = asmOut.cut( pos );
	if( isConstChannelNum.get() ) {
	  asmOut.append( oldCntCode );
	  asmOut.append( "\tLD\tB,H\n"
			  + "\tLD\tC,L\n" );
	  asmOut.append( channelCode );
	} else {
	  asmOut.append( oldCntCode );
	  asmOut.append( "\tPUSH\tHL\n" );
	  asmOut.append( channelCode );
	  asmOut.append( "\tPOP\tBC\n" );
	}
      }
      asmOut.append( "\tCALL\tIOINX\n" );
      compiler.addLibItem( BasicLibrary.LibItem.IOINX );
      compiler.setErrVarsSet();
    } else {
      Integer cnt = BasicUtil.removeLastCodeIfConstExpr( asmOut, pos );
      if( cnt != null ) {
	if( cnt.intValue() == 1 ) {
	  asmOut.append( "\tCALL\tS_INCHAR\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.S_INCHAR );
	} else {
	  asmOut.append_LD_HL_nn( cnt );
	  asmOut.append( "\tCALL\tS_INPUT_N\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.S_INPUT_N );
	}
      } else {
	asmOut.append( "\tCALL\tS_INPUT_N\n" );
	compiler.addLibItem( BasicLibrary.LibItem.S_INPUT_N );
      }
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrLEFT(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    BasicUtil.parseToken( iter, ',' );
    int pos = asmOut.length();
    BasicExprParser.parseInt2Expr( compiler, iter, context );
    String oldCode = asmOut.cut( pos );
    String newCode = BasicUtil.convertCodeToValueInBC( oldCode );
    if( newCode != null ) {
      asmOut.append( newCode );
    } else {
      asmOut.append( "\tPUSH\tHL\n" );
      asmOut.append( oldCode );
      asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n"
			+ "\tPOP\tHL\n" );
    }
    asmOut.append( "\tCALL\tF_S_LEFT\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.F_S_LEFT );
  }


  private static void parseStrLOWER(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    BasicUtil.parseToken( iter, ')' );
    compiler.getCodeBuf().append( "\tCALL\tF_S_LOWER\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_S_LOWER );
  }


  private static void parseStrLTRIM(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    compiler.getCodeBuf().append( "\tCALL\tF_S_LTRIM\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.F_S_LTRIM );
  }


  private static void parseStrMEMSTR(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    BasicExprParser.parseEnclosedInt2Expr( compiler, iter, context );
  }


  private static void parseStrMIRROR(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    String text = BasicUtil.checkStringLiteral( compiler, iter );
    if( text != null ) {
      if( text.isEmpty() ) {
	asmOut.append_LD_HL_xx( BasicLibrary.EMPTY_STRING_LABEL );
	compiler.addLibItem( BasicLibrary.LibItem.EMPTY_STRING );
      } else {
	asmOut.appendStringLiteral(
		(new StringBuilder( text ).reverse()).toString(),
		"00H" );
      }
    } else {
      compiler.lockTmpStrBuf();
      BasicExprParser.parseStringPrimExpr( compiler, iter, context );
      asmOut.append( "\tCALL\tF_S_MIRROR\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_S_MIRROR );
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrMID(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    BasicUtil.parseToken( iter, ',' );
    int pos = asmOut.length();
    BasicExprParser.parseInt2Expr( compiler, iter, context );
    String oldCode2 = asmOut.cut( pos );
    String newCode2 = BasicUtil.convertCodeToValueInDE( oldCode2 );
    if( BasicUtil.checkComma( iter ) ) {
      BasicExprParser.parseInt2Expr( compiler, iter, context );
      String oldCode3 = asmOut.cut( pos );
      String newCode3 = BasicUtil.convertCodeToValueInBC( oldCode3 );
      if( (newCode2 != null) && (newCode3 != null) ) {
	asmOut.append( newCode2 );
	asmOut.append( newCode3 );
      } else {
	asmOut.append( "\tPUSH\tHL\n" );
	if( newCode2 != null ) {
	  asmOut.append( oldCode3 );
	  asmOut.append( "\tLD\tB,H\n"
				+ "\tLD\tC,L\n" );
	  asmOut.append( newCode2 );
	} else {
	  if( newCode3 != null ) {
	    asmOut.append( oldCode2 );
	    asmOut.append( "\tEX\tDE,HL\n" );
	    asmOut.append( newCode3 );
	  } else {
	    asmOut.append( oldCode2 );
	    asmOut.append( "\tPUSH\tHL\n" );
	    asmOut.append( oldCode3 );
	    asmOut.append( "\tLD\tB,H\n"
				+ "\tLD\tC,L\n"
				+ "\tPOP\tDE\n" );
	  }
	}
	asmOut.append( "\tPOP\tHL\n" );
      }
      asmOut.append( "\tCALL\tF_S_MID_N\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_S_MID_N );
    } else {
      if( newCode2 != null ) {
	asmOut.append( newCode2 );
      } else {
	asmOut.append( "\tPUSH\tHL\n" );
	asmOut.append( oldCode2 );
	asmOut.append( "\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n" );
      }
      asmOut.append( "\tCALL\tF_S_MID\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_S_MID );
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrRIGHT(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    BasicUtil.parseToken( iter, ',' );
    int pos = asmOut.length();
    BasicExprParser.parseInt2Expr( compiler, iter, context );
    String oldCode = asmOut.cut( pos );
    String newCode = BasicUtil.convertCodeToValueInBC( oldCode );
    if( newCode != null ) {
      asmOut.append( newCode );
    } else {
      asmOut.append( "\tPUSH\tHL\n" );
      asmOut.append( oldCode );
      asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n"
			+ "\tPOP\tHL\n" );
    }
    asmOut.append( "\tCALL\tF_S_RIGHT\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.F_S_RIGHT );
  }


  private static void parseStrRTRIM(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    compiler.getCodeBuf().append( "\tCALL\tF_S_RTRIM\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.F_S_RTRIM );
  }


  private static void parseStrSPACE(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    int pos = asmOut.length();
    BasicExprParser.parseEnclosedInt2Expr( compiler, iter, context );
    String oldCode = asmOut.cut( pos );
    String newCode = BasicUtil.convertCodeToValueInBC( oldCode );
    if( newCode != null ) {
      asmOut.append( newCode );
    } else {
      asmOut.append( oldCode );
      asmOut.append( "\tLD\tB,H\n"
		+ "\tLD\tC,L\n" );
    }
    asmOut.append( "\tLD\tL,20H\n"
		+ "\tCALL\tF_S_STRING_I2_C\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_S_STRING_I2_C );
  }


  private static void parseStrSTR(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    switch( BasicExprParser.parseNumericExpr(
					compiler,
					iter,
					context,
					null ) )
    {
      case INT2:
	asmOut.append( "\tCALL\tS_STR_I2\n" );
	compiler.addLibItem( BasicLibrary.LibItem.S_STR_I2 );
	break;
      case INT4:
	asmOut.append( "\tCALL\tS_STR_I4\n" );
	compiler.addLibItem( BasicLibrary.LibItem.S_STR_I4 );
	break;
      case DEC6:
	if( asmOut.cutIfEndsWith( "\tCALL\tD6_LD_ACCU_MEM\n" ) ) {
	  asmOut.append( "\tCALL\tS_STR_D6MEM\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.S_STR_D6MEM );
	} else {
	  asmOut.append( "\tCALL\tS_STR_D6\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.S_STR_D6 );
	  context.setMAccuDirty();
	}
	break;
      default:
	BasicUtil.throwNumericExprExpected();
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrSTRING(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    int pos = asmOut.length();
    BasicExprParser.parseInt2Expr( compiler, iter, context );
    BasicUtil.parseToken( iter, ',' );
    Integer cnt = BasicUtil.removeLastCodeIfConstExpr( asmOut, pos );
    if( cnt != null ) {
      if( BasicExprParser.checkParseStringPrimExpr(
						compiler,
						iter,
						context ) )
      {
	asmOut.append_LD_BC_nn( cnt.intValue() );
	asmOut.append( "\tCALL\tF_S_STRING_I2_S\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_S_STRING_I2_S );
      } else {
	BasicExprParser.parseInt2Expr( compiler, iter, context );
	asmOut.append_LD_BC_nn( cnt.intValue() );
	asmOut.append( "\tCALL\tF_S_STRING_I2_C\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_S_STRING_I2_C );
      }
    } else {
      String  oldCode1 = asmOut.cut( pos );
      String  newCode1 = BasicUtil.convertCodeToValueInBC( oldCode1 );
      boolean isStr    = false;
      if( BasicExprParser.checkParseStringPrimExpr(
						compiler,
						iter,
						context ) )
      {
	isStr = true;
      } else {
	BasicExprParser.parseInt2Expr( compiler, iter, context );
      }
      String code2 = asmOut.cut( pos );
      if( BasicUtil.isOnly_LD_HL_xx( code2 ) ) {
	if( newCode1 != null ) {
	  asmOut.append( newCode1 );
	} else {
	  asmOut.append( oldCode1 );
	  asmOut.append( "\tLD\tB,H\n"
			+ "\tLD\tC,L\n" );
	}
	asmOut.append( code2 );
      } else {
	if( newCode1 != null ) {
	  asmOut.append( code2 );
	  asmOut.append( newCode1 );
	} else {
	  asmOut.append( oldCode1 );
	  asmOut.append( "\tPUSH\tHL\n" );
	  asmOut.append( code2 );
	  asmOut.append( "\tPOP\tBC\n" );
	}
      }
      if( isStr ) {
	asmOut.append( "\tCALL\tF_S_STRING_I2_S\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_S_STRING_I2_S );
      } else {
	asmOut.append( "\tCALL\tF_S_STRING_I2_C\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_S_STRING_I2_C );
      }
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrTRIM(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    compiler.getCodeBuf().append( "\tCALL\tF_S_TRIM\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.F_S_TRIM );
  }


  private static void parseStrUPPER(
			BasicCompiler     compiler,
			CharacterIterator iter,
			ParseContext      context ) throws PrgException
  {
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter, context );
    BasicUtil.parseToken( iter, ')' );
    compiler.getCodeBuf().append( "\tCALL\tF_S_UPPER\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_S_UPPER );
  }
}
