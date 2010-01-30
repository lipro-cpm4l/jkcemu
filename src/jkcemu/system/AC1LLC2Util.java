/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fuer Ac1 und LLC2 gemeinsam genutzte Funktionen
 */

package jkcemu.system;

import java.lang.*;
import jkcemu.base.*;
import z80emu.Z80MemView;


public class AC1LLC2Util
{
  /*
   * Diese Tabelle mappt die Tokens des SCCH-BASIC-Interpreters
   * in ihre entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   */
  private static final String[] scchTokens = {
	"END",      "FOR",    "NEXT",     "DATA",       // 0x80
	"INPUT",    "DIM",    "READ",     "LET",
	"GOTO",     "RUN",    "IF",       "RESTORE",
	"GOSUB",    "RETURN", "REM",      "STOP",
	"OUT",      "ON",     "NULL",     "WAIT",       // 0x90
	"DEF",      "POKE",   "DOKE",     "AUTO",
	"LINES",    "CLS",    "WIDTH",    "BYE",
	"KEY",      "CALL",   "PRINT",    "CONT",
	"LIST",     "CLEAR",  "CLOAD",    "CSAVE",      // 0xA0
	"NEW",      "TAB(",   "TO",       "FN",
	"SPC(",     "THEN",   "NOT",      "STEP",
	"+",        "-",      "*",        "/",
	"^",        "AND",    "OR",       ">",          // 0xB0
	"=",        "<",      "SGN",      "INT",
	"ABS",      "USR",    "FRE",      "INP",
	"POS",      "SQR",    "RND",      "LN",
	"EXP",      "COS",    "SIN",      "TAN",        // 0xC0
	"ATN",      "PEEK",   "DEEK",     "POINT",
	"LEN",      "STR$",   "VAL",      "ASC",
	"CHR$",     "LEFT$",  "RIGHT$",   "MID$",
	"SET",      "RESET",  "RENUMBER", "LOCATE",     // 0xD0
	"SOUND",    "INKEY",  "MODE",     "TRON",
	"TROFF" };


  public static String getSCCHBasicProgram( Z80MemView memory )
  {
    return SourceUtil.getKCBasicStyleProgram( memory, 0x60F7, scchTokens );
  }


  public static int reassembleSysCall(
				Z80MemView    memory,
				int           addr,
				StringBuilder buf,
				int           colMnemonic,
				int           colArgs,
				int           colRemark )
  {
    int    rv  = 0;
    int    bol = buf.length();
    int    b   = memory.getMemByte( addr, true );
    int    w   = 0;
    String s   = null;
    switch( b ) {
      case 0xCF:
	buf.append( String.format( "%04X  %02X", addr, b ) );
	EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "RST" );
	EmuSys.appendSpacesToCol( buf, bol, colArgs );
	buf.append( "08H" );
	EmuSys.appendSpacesToCol( buf, bol, colRemark );
	buf.append( ";INCH\n" );
	rv = 1;
	break;

      case 0xD7:
	buf.append( String.format( "%04X  %02X", addr, b ) );
	EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "RST" );
	EmuSys.appendSpacesToCol( buf, bol, colArgs );
	buf.append( "10H" );
	EmuSys.appendSpacesToCol( buf, bol, colRemark );
	buf.append( ";OUTCH\n" );
	rv = 1;
	break;

      case 0xDF:
	buf.append( String.format( "%04X  %02X", addr, b ) );
	EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "RST" );
	EmuSys.appendSpacesToCol( buf, bol, colArgs );
	buf.append( "18H" );
	EmuSys.appendSpacesToCol( buf, bol, colRemark );
	buf.append( ";OUTS\n" );
	rv = 1 + EmuSys.reassStringBit7(
				memory,
				addr + 1,
				buf,
				colMnemonic,
				colArgs );
	break;

      case 0xC3:
	w = memory.getMemWord( addr + 1 );
	s = null;
	switch( w ) {
	  case 0x0008:
	  case 0x1802:
	    s = "INCH";
	    break;

	  case 0x0010:
	  case 0x1805:
	    s = "OUTCH";
	    break;

	  case 0x0018:
	  case 0x1808:
	    s = "OUTS";
	    break;
	}
	if( s != null ) {
	  buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	  EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	  buf.append( "JP" );
	  EmuSys.appendSpacesToCol( buf, bol, colArgs );
	  buf.append( String.format( "%04XH", w ) );
	  EmuSys.appendSpacesToCol( buf, bol, colRemark );
	  buf.append( (char) ';' );
	  buf.append( s );
	  buf.append( (char) '\n' );
	  rv = 3;
	}
	break;

      case 0xCD:
	w = memory.getMemWord( addr + 1 );
	switch( w ) {
	  case 0x0008:
	  case 0x1802:
	    buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	    EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( "CALL" );
	    EmuSys.appendSpacesToCol( buf, bol, colArgs );
	    buf.append( String.format( "%04XH", w ) );
	    EmuSys.appendSpacesToCol( buf, bol, colRemark );
	    buf.append( ";INCH\n" );
	    rv = 3;
	    break;

	  case 0x0010:
	  case 0x1805:
	    buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	    EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( "CALL" );
	    EmuSys.appendSpacesToCol( buf, bol, colArgs );
	    buf.append( String.format( "%04XH", w ) );
	    EmuSys.appendSpacesToCol( buf, bol, colRemark );
	    buf.append( ";OUTCH\n" );
	    rv = 3;
	    break;

	  case 0x0018:
	  case 0x1808:
	    buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	    EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( "CALL" );
	    EmuSys.appendSpacesToCol( buf, bol, colArgs );
	    buf.append( String.format( "%04XH", w ) );
	    EmuSys.appendSpacesToCol( buf, bol, colRemark );
	    buf.append( ";OUTS\n" );
	    rv = 3 + EmuSys.reassStringBit7(
					memory,
					addr + 3,
					buf,
					colMnemonic,
					colArgs );
	    break;
	}
	break;
    }
    return rv;
  }


	/* --- private Konstruktoren und Methoden --- */

  private AC1LLC2Util()
  {
    // leer
  }
}

