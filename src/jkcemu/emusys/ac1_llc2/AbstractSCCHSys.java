/*
 * (c) 2010-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation fuer die Emulation eines SCCH-Systems
 */

package jkcemu.emusys.ac1_llc2;

import java.lang.*;
import java.util.Properties;
import jkcemu.base.*;
import z80emu.*;


public abstract class AbstractSCCHSys extends EmuSys
{
  /*
   * Diese Tabelle mappt die Tokens des SCCH-BASIC-Interpreters
   * in ihre entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   */
  protected static final String[] scchTokens = {
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

  protected static final int[] scchCharToUnicode = {
		'\u0020', '\u2598', '\u259D', '\u2580',		// 00h
		'\u2596', '\u258C', '\u259E', '\u259B',
		'\u2597', '\u259A', '\u2590', '\u259C',
		'\u2584', '\u2599', '\u259F', '\u2588',
		      -1,       -1, '\u25A0', '\u2572',		// 10h
		'\u2571', '\u254B', '\u2592',       -1,
		      -1,       -1,       -1, '\u25A1',
		'\u25A3', '\u25C6', '\u25AB', '\u25AA',
		'\u0020',      '!',     '\"',      '#',		// 20h
		     '$',      '%',      '&',     '\'',
		     '(',      ')',      '*',      '+',
		     ',',      '-',      '.',      '/',
		     '0',      '1',      '2',      '3',		// 30h
		     '4',      '5',      '6',      '7',
		     '8',      '9',      ':',      ';',
		     '<',      '=',      '>',      '?',
		'\u00A7',      'A',      'B',      'C',		// 40h
		     'D',      'E',      'F',      'G',
		     'H',      'I',      'J',      'K',
		     'L',      'M',      'N',      'O',
		     'P',      'Q',      'R',      'S',		// 50h
		     'T',      'U',      'V',      'W',
		     'X',      'Y',      'Z', '\u00C4',
		'\u00D6', '\u00DC',      '^',      '_',
		     '@',      'a',      'b',      'c',		// 60h
		     'd',      'e',      'f',      'g',
		     'h',      'i',      'j',      'k',
		     'l',      'm',      'n',      'o',
		     'p',      'q',      'r',      's',		// 70h
		     't',      'u',      'v',      'w',
		     'x',      'y',      'z', '\u00E4',
		'\u00F6', '\u00FC', '\u00DF',       -1,
		      -1,       -1,       -1,       -1,		// 80h
		      -1,       -1,       -1,       -1,
		      -1,       -1, '\u25CB', '\u25D8',
		'\u25EF',       -1, '\u25E4', '\u25E3',
		'\u2571', '\u2572',       -1,       -1,		// 90h
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		'\u2501', '\u2503', '\u253B', '\u2523',		// A0h
		'\u2533', '\u252B', '\u254B', '\u2517',
		'\u250F', '\u2513', '\u251B',       -1,
		      -1,       -1,       -1, '\u2573',
		'\u2598', '\u259D', '\u2597', '\u2596',		// B0h
		'\u258C', '\u2590', '\u2580', '\u2584',
		'\u259A', '\u259E', '\u259F', '\u2599',
		'\u259B', '\u259C', '\u25E2', '\u25E5',
		      -1,       -1,       -1,       -1,		// C0h
		'\u265F',       -1,       -1, '\u2592',
		      -1, '\u2666', '\u2663', '\u2665',
		'\u2660',       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,		// D0h
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,		// E0h
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,		// F0h
		      -1,       -1,       -1,       -1,
		'\u2581', '\u2582', '\u2583', '\u2584',
		'\u2585', '\u2586', '\u2587', '\u2588' };

  protected Z80PIO           pio1;
  protected volatile boolean joystickEnabled;
  protected volatile boolean joystickSelected;
  protected int              joystickValue;
  protected int              keyboardValue;


  protected AbstractSCCHSys( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
  }


  protected void setKeyboardValue( int value )
  {
    this.keyboardValue = value;
    synchronized( this.pio1 ) {
      if( !(this.joystickEnabled && this.joystickSelected) ) {
	this.pio1.putInValuePortA( this.keyboardValue, 0xFF );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int getSupportedJoystickCount()
  {
    return this.joystickEnabled ? 1 : 0;
  }


  @Override
  public void openBasicProgram()
  {
    String text = SourceUtil.getKCBasicStyleProgram(
						this.emuThread,
						0x60F7,
						scchTokens );
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoBasic();
    }
  }


  @Override
  public int reassembleSysCall(
			Z80MemView    memory,
			int           addr,
			StringBuilder buf,
			boolean       sourceOnly,
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
	if( !sourceOnly ) {
	  buf.append( String.format( "%04X  %02X", addr, b ) );
	}
	EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "RST" );
	EmuSys.appendSpacesToCol( buf, bol, colArgs );
	buf.append( "08H" );
	EmuSys.appendSpacesToCol( buf, bol, colRemark );
	buf.append( ";INCH\n" );
	rv = 1;
	break;

      case 0xD7:
	if( !sourceOnly ) {
	  buf.append( String.format( "%04X  %02X", addr, b ) );
	}
	EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "RST" );
	EmuSys.appendSpacesToCol( buf, bol, colArgs );
	buf.append( "10H" );
	EmuSys.appendSpacesToCol( buf, bol, colRemark );
	buf.append( ";OUTCH\n" );
	rv = 1;
	break;

      case 0xDF:
	if( !sourceOnly ) {
	  buf.append( String.format( "%04X  %02X", addr, b ) );
	}
	EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "RST" );
	EmuSys.appendSpacesToCol( buf, bol, colArgs );
	buf.append( "18H" );
	EmuSys.appendSpacesToCol( buf, bol, colRemark );
	buf.append( ";OUTS\n" );
	rv = 1 + EmuSys.reassStringBit7(
				this.emuThread,
				addr + 1,
				buf,
				sourceOnly,
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
	  if( !sourceOnly ) {
	    buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	  }
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
	    if( !sourceOnly ) {
	      buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	    }
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
	    if( !sourceOnly ) {
	      buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	    }
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
	    if( !sourceOnly ) {
	      buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	    }
	    EmuSys.appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( "CALL" );
	    EmuSys.appendSpacesToCol( buf, bol, colArgs );
	    buf.append( String.format( "%04XH", w ) );
	    EmuSys.appendSpacesToCol( buf, bol, colRemark );
	    buf.append( ";OUTS\n" );
	    rv = 3 + EmuSys.reassStringBit7(
					this.emuThread,
					addr + 3,
					buf,
					sourceOnly,
					colMnemonic,
					colArgs );
	    break;
	}
	break;
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    this.joystickSelected = false;
    this.joystickValue    = 0;
    this.keyboardValue    = 0;
  }


  @Override
  public boolean supportsOpenBasic()
  {
    return true;
  }
}

