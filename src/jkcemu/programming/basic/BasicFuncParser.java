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
import java.util.concurrent.atomic.AtomicBoolean;
import jkcemu.programming.PrgException;


public class BasicFuncParser
{
  public static boolean checkParseIntFunction(
				BasicCompiler     compiler,
				CharacterIterator iter,
				String            name ) throws PrgException
  {
    boolean rv = false;
    if( name != null ) {
      rv = true;
      if( name.equals( "ABS" ) ) {
	parseABS( compiler, iter );
      } else if( name.equals( "ASC" ) ) {
	parseASC( compiler, iter );
      } else if( name.equals( "ASM" ) ) {
	compiler.parseASM( iter, true );
      } else if( name.equals( "DEEK" ) ) {
	parseDEEK( compiler, iter );
      } else if( name.equals( "EOF" ) ) {
	parseEOF( compiler, iter );
      } else if( name.equals( "HIBYTE" ) ) {
	parseHIBYTE( compiler, iter );
      } else if( name.equals( "IN" ) || name.equals( "INP" ) ) {
	parseIN( compiler, iter );
      } else if( name.equals( "INSTR" ) ) {
	parseINSTR( compiler, iter );
      } else if( name.equals( "IS_TARGET" ) ) {
	parseIS_TARGET( compiler, iter );
      } else if( name.equals( "JOYST" ) ) {
	parseJOYST( compiler, iter );
      } else if( name.equals( "LEN" ) ) {
	parseLEN( compiler, iter );
      } else if( name.equals( "LOBYTE" ) ) {
	parseLOBYTE( compiler, iter );
      } else if( name.equals( "MAX" ) ) {
	parseMAX( compiler, iter );
      } else if( name.equals( "MIN" ) ) {
	parseMIN( compiler, iter );
      } else if( name.equals( "PEEK" ) ) {
	parsePEEK( compiler, iter );
      } else if( name.equals( "POINT" ) ) {
	parsePOINT( compiler, iter );
      } else if( name.equals( "PTEST" ) ) {
	parsePTEST( compiler, iter );
      } else if( name.equals( "RND" ) ) {
	parseRND( compiler, iter );
      } else if( name.equals( "SGN" ) ) {
	parseSGN( compiler, iter );
      } else if( name.equals( "SQR" ) ) {
	parseSQR( compiler, iter );
      } else if( name.equals( "USR" ) ) {
	parseUSR( compiler, iter );
      } else if( name.equals( "USR0" ) ) {
	parseUSR( compiler, iter, 0 );
      } else if( name.equals( "USR1" ) ) {
	parseUSR( compiler, iter, 1 );
      } else if( name.equals( "USR2" ) ) {
	parseUSR( compiler, iter, 2 );
      } else if( name.equals( "USR3" ) ) {
	parseUSR( compiler, iter, 3 );
      } else if( name.equals( "USR4" ) ) {
	parseUSR( compiler, iter, 4 );
      } else if( name.equals( "USR5" ) ) {
	parseUSR( compiler, iter, 5 );
      } else if( name.equals( "USR6" ) ) {
	parseUSR( compiler, iter, 6 );
      } else if( name.equals( "USR7" ) ) {
	parseUSR( compiler, iter, 7 );
      } else if( name.equals( "USR8" ) ) {
	parseUSR( compiler, iter, 8 );
      } else if( name.equals( "USR9" ) ) {
	parseUSR( compiler, iter, 9 );
      } else if( name.equals( "VAL" ) ) {
	parseVAL( compiler, iter );
      } else {
	rv = false;
      }
    }
    return rv;
  }


  public static boolean checkParseStringFunction(
				BasicCompiler     compiler,
				CharacterIterator iter,
				String            name ) throws PrgException
  {
    boolean rv = false;
    if( name != null ) {
      rv = true;
      if( name.equals( "BIN$" ) ) {
	parseStrBIN( compiler, iter );
      } else if( name.equals( "CHR$" ) ) {
	parseStrCHR( compiler, iter );
      } else if( name.equals( "ERR$" ) ) {
	parseStrERR( compiler, iter );
      } else if( name.equals( "HEX$" ) ) {
	parseStrHEX( compiler, iter );
      } else if( name.equals( "INKEY$" ) ) {
	parseStrINKEY( compiler, iter );
      } else if( name.equals( "INPUT$" ) ) {
	parseStrINPUT( compiler, iter );
      } else if( name.equals( "LEFT$" ) ) {
	parseStrLEFT( compiler, iter );
      } else if( name.equals( "LOWER$" ) || name.equals( "LCASE$" ) ) {
	parseStrLOWER( compiler, iter );
      } else if( name.equals( "LTRIM$" ) ) {
	parseStrLTRIM( compiler, iter );
      } else if( name.equals( "MEMSTR$" ) ) {
	parseStrMEMSTR( compiler, iter );
      } else if( name.equals( "MID$" ) ) {
	parseStrMID( compiler, iter );
      } else if( name.equals( "MIRROR$" ) ) {
	parseStrMIRROR( compiler, iter );
      } else if( name.equals( "RIGHT$" ) ) {
	parseStrRIGHT( compiler, iter );
      } else if( name.equals( "RTRIM$" ) ) {
	parseStrRTRIM( compiler, iter );
      } else if( name.equals( "SPACE$" ) ) {
	parseStrSPACE( compiler, iter );
      } else if( name.equals( "STR$" ) ) {
	parseStrSTR( compiler, iter );
      } else if( name.equals( "STRING$" ) ) {
	parseStrSTRING( compiler, iter );
      } else if( name.equals( "TRIM$" ) ) {
	parseStrTRIM( compiler, iter );
      } else if( name.equals( "UPPER$" ) || name.equals( "UCASE$" ) ) {
	parseStrUPPER( compiler, iter );
      } else {
	rv = false;
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static void parseABS(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tCALL\tABSHL\n" );
    compiler.addLibItem( BasicLibrary.LibItem.ABS_NEG_HL );
  }


  private static void parseASC(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter );
    BasicUtil.parseToken( iter, ')' );
    compiler.getCodeBuf().append( "\tLD\tL,(HL)\n"
				+ "\tLD\tH,00H\n" );
  }


  private static void parseDEEK(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    int        pos    = asmOut.length();
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    Integer addr = BasicUtil.removeLastCodeIfConstExpr( compiler, pos );
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
  }


  private static void parseEOF(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicUtil.checkToken( iter, '#' );
    compiler.parseIOChannelNumToPtrFldAddrInHL( iter, null );
    compiler.getCodeBuf().append( "\tCALL\tIOEOF\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.IOEOF );
  }


  private static void parseHIBYTE(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tLD\tL,H\n"
				+ "\tLD\tH,00H\n" );
  }


  private static void parseIN(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseExpr( compiler, iter );
    BasicUtil.parseToken( iter, ')' );
    if( !BasicUtil.replaceLastCodeFrom_LD_HL_To_BC( compiler ) ) {
      asmOut.append( "\tLD\tB,H\n"
		+ "\tLD\tC,L\n" );
    }
    asmOut.append( "\tIN\tL,(C)\n"
		+ "\tLD\tH,0\n" );
  }


  private static void parseINSTR(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut        = compiler.getCodeBuf();
    boolean    hasStartPos   = false;
    Integer    startPosValue = null;
    String     startPosText  = null;
    BasicUtil.parseToken( iter, '(' );
    String text = BasicUtil.checkStringLiteral( compiler, iter );
    if( text == null ) {
      hasStartPos = !BasicExprParser.checkParseStringPrimExpr(
							compiler, iter );
    }
    if( hasStartPos ) {
      int pos = asmOut.length();
      BasicExprParser.parseExpr( compiler, iter );
      startPosValue = BasicUtil.removeLastCodeIfConstExpr( compiler, pos );
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
	BasicExprParser.parseStringPrimExpr( compiler, iter );
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
      BasicExprParser.parseStringPrimExpr( compiler, iter );
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
      asmOut.append( "\tCALL\tF_INSTRN\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_INSTRN );
    } else {
      asmOut.append( "\tCALL\tF_INSTR\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_INSTR );
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseIS_TARGET(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    int[]      targetIDs = compiler.getTarget().getTargetIDs();
    AsmCodeBuf asmOut    = compiler.getCodeBuf();
    int        pos       = asmOut.length();
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    Integer value = BasicUtil.removeLastCodeIfConstExpr( compiler, pos );
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
	asmOut.append( "\tCALL\tO_EQ \n" );
	compiler.addLibItem( BasicLibrary.LibItem.O_EQ );
      } else if( targetIDs.length > 1 ) {
	asmOut.append( "\tCALL\tF_IS_TARGET\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_IS_TARGET );
      }
    }
  }


  private static void parseJOYST(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tCALL\tF_JOY\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_JOY );
  }


  private static void parseLEN(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    String text = BasicUtil.checkStringLiteral( compiler, iter );
    if( text != null ) {
      asmOut.append_LD_HL_nn( text.length() );
    } else {
      BasicExprParser.parseStringPrimExpr( compiler, iter );
      asmOut.append( "\tCALL\tF_LEN\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_LEN );
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseLOBYTE(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tLD\tH,00H\n" );
  }


  private static void parseMAX(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseExpr( compiler, iter );
    while( BasicUtil.checkToken( iter, ',' ) ) {
      asmOut.append( "\tPUSH\tHL\n" );
      BasicExprParser.parseExpr( compiler, iter );
      String label = compiler.nextLabel();
      asmOut.append( "\tPOP\tDE\n"
	+ "\tCALL\tCPHLDE\n"
	+ "\tJR\tNC," );
      asmOut.append( label );
      asmOut.append( "\n"
	+ "\tEX\tDE,HL\n" );
      asmOut.append( label );
      asmOut.append( ":\n" );
    }
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.CPHLDE );
  }


  private static void parseMIN(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseExpr( compiler, iter );
    while( BasicUtil.checkToken( iter, ',' ) ) {
      asmOut.append( "\tPUSH\tHL\n" );
      BasicExprParser.parseExpr( compiler, iter );
      String label = compiler.nextLabel();
      asmOut.append( "\tPOP\tDE\n"
		+ "\tCALL\tCPHLDE\n"
		+ "\tJR\tC," );
      asmOut.append( label );
      asmOut.append( "\n"
		+ "\tEX\tDE,HL\n" );
      asmOut.append( label );
      asmOut.append( ":\n" );
    }
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.CPHLDE );
  }


  private static void parsePEEK(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tLD\tL,(HL)\n"
				+ "\tLD\tH,0\n" );
  }


  private static void parseRND(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tCALL\tF_RND\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_RND );
  }


  private static void parsePOINT(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parse2ArgsTo_DE_HL( compiler, iter );
    BasicUtil.parseToken( iter, ')' );
    compiler.getCodeBuf().append( "\tCALL\tXPOINT\n" );
    compiler.addLibItem( BasicLibrary.LibItem.XPOINT );
  }


  private static void parsePTEST(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parse2ArgsTo_DE_HL( compiler, iter );
    BasicUtil.parseToken( iter, ')' );
    compiler.getCodeBuf().append( "\tCALL\tXPTEST\n" );
    compiler.addLibItem( BasicLibrary.LibItem.XPTEST );
  }


  private static void parseSGN(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tCALL\tF_SGN\n" );
    compiler.addLibItem( BasicLibrary.LibItem.F_SGN );
  }


  private static void parseSQR(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    int        pos    = asmOut.length();
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    Integer value = BasicUtil.removeLastCodeIfConstExpr( compiler, pos );
    if( value != null ) {
      if( value.intValue() < 0 ) {
	throw new PrgException(
			"Wurzel aus negativer Zahl nicht m\u00F6glich" );
      }
      asmOut.append_LD_HL_nn(
		(int) Math.floor( Math.sqrt( value.doubleValue() ) ) );
    } else {
      asmOut.append( "\tCALL\tF_SQR\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_SQR );
    }
  }


  private static void parseUSR(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    int usrNum = BasicUtil.parseUsrNum( iter );
    parseUSR( compiler, iter, usrNum );
  }


  private static void parseUSR(
			BasicCompiler     compiler,
			CharacterIterator iter,
			int               usrNum ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicExprParser.parseEnclosedExpr( compiler, iter );
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
    compiler.addLibItem( BasicLibrary.LibItem.E_USR );
  }


  private static void parseVAL(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter );
    if( BasicUtil.checkToken( iter, ',' ) ) {
      int pos = asmOut.length();
      BasicExprParser.parseExpr( compiler, iter );
      Integer radix = BasicUtil.removeLastCodeIfConstExpr( compiler, pos );
      if( radix != null ) {
	switch( radix.intValue() ) {
	  case 2:
	    asmOut.append( "\tCALL\tF_VLB\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.F_VLB );
	    break;
	  case 10:
	    asmOut.append( "\tCALL\tF_VLI\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.F_VLI );
	    break;
	  case 16:
	    asmOut.append( "\tCALL\tF_VLH\n" );
	    compiler.addLibItem( BasicLibrary.LibItem.F_VLH );
	    break;
	  default:
	    throw new PrgException(
		"Zahlenbasis in VAL-Funktion nicht unterst\u00FCtzt" );
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
	asmOut.append( "\tCALL\tF_VAL\n" );
	compiler.addLibItem( BasicLibrary.LibItem.F_VAL );
      }
    } else {
      asmOut.append( "\tCALL\tF_VLI\n" );
      compiler.addLibItem( BasicLibrary.LibItem.F_VLI );
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrBIN(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseExpr( compiler, iter );
    if( BasicUtil.checkToken( iter, ',' ) ) {
      int pos = asmOut.length();
      BasicExprParser.parseExpr( compiler, iter );
      Integer value = BasicUtil.removeLastCodeIfConstExpr( compiler, pos );
      if( value != null ) {
	if( value.intValue() < 0 ) {
	  compiler.putWarningOutOfRange();
	}
	asmOut.append_LD_BC_nn( value.intValue() );
      } else {
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
      }
      asmOut.append( "\tCALL\tS_BINN\n" );
    } else {
      asmOut.append( "\tCALL\tS_BIN\n" );
    }
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.S_BIN );
  }


  private static void parseStrCHR(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    int pos = asmOut.length();
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    Integer value = BasicUtil.removeLastCodeIfConstExpr( compiler, pos );
    if( value != null ) {
      asmOut.append_LD_A_n( value.intValue() );
      asmOut.append( "\tCALL\tS_CHRA\n" );
    } else {
      asmOut.append( "\tCALL\tS_CHRL\n" );
    }
    compiler.addLibItem( BasicLibrary.LibItem.S_CHR );
  }


  private static void parseStrERR(
			BasicCompiler     compiler,
			CharacterIterator iter )  throws PrgException
  {
    compiler.getCodeBuf().append( "\tLD\tHL,(M_ERT)\n" );
    compiler.addLibItem( BasicLibrary.LibItem.M_ERT );
  }


  private static void parseStrHEX(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseExpr( compiler, iter );
    if( BasicUtil.checkToken( iter, ',' ) ) {
      int pos = asmOut.length();
      BasicExprParser.parseExpr( compiler, iter );
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
      asmOut.append( "\tCALL\tS_HEXN\n" );
      compiler.addLibItem( BasicLibrary.LibItem.S_HEXN );
    } else {
      asmOut.append( "\tCALL\tS_HEX\n" );
      compiler.addLibItem( BasicLibrary.LibItem.S_HEX );
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrINKEY(
			BasicCompiler     compiler,
			CharacterIterator iter )  throws PrgException
  {
    compiler.getCodeBuf().append( "\tCALL\tS_INKY\n" );
    compiler.addLibItem( BasicLibrary.LibItem.S_INKY );
  }


  private static void parseStrINPUT(
			BasicCompiler     compiler,
			CharacterIterator iter )  throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    int pos = asmOut.length();
    BasicExprParser.parseExpr( compiler, iter );
    if( BasicUtil.checkToken( iter, ',' ) ) {
      String oldCntCode = asmOut.cut( pos );
      String newCntCode = BasicUtil.convertCodeToValueInBC( oldCntCode );
      AtomicBoolean isConstChannelNum = new AtomicBoolean();
      BasicUtil.checkToken( iter, '#' );
      compiler.parseIOChannelNumToPtrFldAddrInHL( iter, isConstChannelNum );
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
    } else {
      Integer cnt = BasicUtil.removeLastCodeIfConstExpr( compiler, pos );
      if( cnt != null ) {
	if( cnt.intValue() == 1 ) {
	  asmOut.append( "\tCALL\tS_INCH\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.S_INCH );
	} else {
	  asmOut.append( "\tCALL\tS_INP\n" );
	  compiler.addLibItem( BasicLibrary.LibItem.S_INP );
	}
      } else {
	asmOut.append( "\tCALL\tS_INP\n" );
	compiler.addLibItem( BasicLibrary.LibItem.S_INP );
      }
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrLEFT(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter );
    BasicUtil.parseToken( iter, ',' );
    int pos = asmOut.length();
    BasicExprParser.parseExpr( compiler, iter );
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
    asmOut.append( "\tCALL\tS_LEFT\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.S_LEFT );
  }


  private static void parseStrLOWER(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter );
    BasicUtil.parseToken( iter, ')' );
    compiler.getCodeBuf().append( "\tCALL\tS_LWR\n" );
    compiler.addLibItem( BasicLibrary.LibItem.S_LWR );
  }


  private static void parseStrLTRIM(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tCALL\tS_LTRIM\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.S_LTRIM );
  }


  private static void parseStrMEMSTR(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseEnclosedExpr( compiler, iter );
  }


  private static void parseStrMIRROR(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
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
      BasicExprParser.parseStringPrimExpr( compiler, iter );
      asmOut.append( "\tCALL\tS_MIRR\n" );
      compiler.addLibItem( BasicLibrary.LibItem.S_MIRR );
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrMID(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter );
    BasicUtil.parseToken( iter, ',' );
    int pos = asmOut.length();
    BasicExprParser.parseExpr( compiler, iter );
    String oldCode2 = asmOut.cut( pos );
    String newCode2 = BasicUtil.convertCodeToValueInDE( oldCode2 );
    if( BasicUtil.checkToken( iter, ',' ) ) {
      BasicExprParser.parseExpr( compiler, iter );
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
      asmOut.append( "\tCALL\tS_MIDN\n" );
      compiler.addLibItem( BasicLibrary.LibItem.S_MIDN );
    } else {
      if( newCode2 != null ) {
	asmOut.append( newCode2 );
      } else {
	asmOut.append( "\tPUSH\tHL\n" );
	asmOut.append( oldCode2 );
	asmOut.append( "\tEX\tDE,HL\n"
				+ "\tPOP\tHL\n" );
      }
      asmOut.append( "\tCALL\tS_MID\n" );
      compiler.addLibItem( BasicLibrary.LibItem.S_MID );
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrRIGHT(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter );
    BasicUtil.parseToken( iter, ',' );
    int pos = asmOut.length();
    BasicExprParser.parseExpr( compiler, iter );
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
    asmOut.append( "\tCALL\tS_RIGHT\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.S_RIGHT );
  }


  private static void parseStrRTRIM(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tCALL\tS_RTRIM\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.S_RTRIM );
  }


  private static void parseStrSPACE(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    int pos = asmOut.length();
    BasicExprParser.parseEnclosedExpr( compiler, iter );
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
		+ "\tCALL\tS_STC\n" );
    compiler.addLibItem( BasicLibrary.LibItem.S_STC );
  }


  private static void parseStrSTR(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    BasicExprParser.parseEnclosedExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tCALL\tS_STR\n" );
    compiler.addLibItem( BasicLibrary.LibItem.S_STR );
  }


  private static void parseStrSTRING(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    AsmCodeBuf asmOut = compiler.getCodeBuf();
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    int pos = asmOut.length();
    BasicExprParser.parseExpr( compiler, iter );
    BasicUtil.parseToken( iter, ',' );
    Integer cnt = BasicUtil.removeLastCodeIfConstExpr( compiler, pos );
    if( cnt != null ) {
      if( BasicExprParser.checkParseStringPrimExpr( compiler, iter ) ) {
	asmOut.append_LD_BC_nn( cnt.intValue() );
	asmOut.append( "\tCALL\tS_STS\n" );
	compiler.addLibItem( BasicLibrary.LibItem.S_STS );
      } else {
	BasicExprParser.parseExpr( compiler, iter );
	asmOut.append_LD_BC_nn( cnt.intValue() );
	asmOut.append( "\tCALL\tS_STC\n" );
	compiler.addLibItem( BasicLibrary.LibItem.S_STC );
      }
    } else {
      String  oldCode1 = asmOut.cut( pos );
      String  newCode1 = BasicUtil.convertCodeToValueInBC( oldCode1 );
      boolean isStr    = false;
      if( BasicExprParser.checkParseStringPrimExpr( compiler, iter ) ) {
	isStr = true;
      } else {
	BasicExprParser.parseExpr( compiler, iter );
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
	asmOut.append( "\tCALL\tS_STS\n" );
	compiler.addLibItem( BasicLibrary.LibItem.S_STS );
      } else {
	asmOut.append( "\tCALL\tS_STC\n" );
	compiler.addLibItem( BasicLibrary.LibItem.S_STC );
      }
    }
    BasicUtil.parseToken( iter, ')' );
  }


  private static void parseStrTRIM(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter );
    compiler.getCodeBuf().append( "\tCALL\tS_TRIM\n" );
    BasicUtil.parseToken( iter, ')' );
    compiler.addLibItem( BasicLibrary.LibItem.S_TRIM );
  }


  private static void parseStrUPPER(
			BasicCompiler     compiler,
			CharacterIterator iter ) throws PrgException
  {
    compiler.lockTmpStrBuf();
    BasicUtil.parseToken( iter, '(' );
    BasicExprParser.parseStringPrimExpr( compiler, iter );
    BasicUtil.parseToken( iter, ')' );
    compiler.getCodeBuf().append( "\tCALL\tS_UPR\n" );
    compiler.addLibItem( BasicLibrary.LibItem.S_UPR );
  }
}
