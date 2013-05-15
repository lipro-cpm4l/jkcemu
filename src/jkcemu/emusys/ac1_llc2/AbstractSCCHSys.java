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

