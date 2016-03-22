/*
 * (c) 2010-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation fuer die Emulation eines SCCH-Systems
 */

package jkcemu.emusys.ac1_llc2;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.text.*;
import java.util.Properties;
import jkcemu.base.*;
import jkcemu.text.TextUtil;
import z80emu.*;


public abstract class AbstractSCCHSys
			extends EmuSys
			implements Z80PCListener
{
  // Einprung zum Lesen eines Zeichens von der Tastatur
  private static int ADDR_INCH = 0x1802;

  /*
   * Diese Tabelle mappt die Tokens des SCCH-BASIC-Interpreters
   * in ihre entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   *
   * Die Tokens ab PAUSE (Codes 0xD9 und hoeher) wurden von Rolf Weidlich
   * im Zuge seiner Weiterentwicklung des LLCBASIC-Interpreters eingefuehrt.
   */
  protected static final String[] scchTokens = {
	"END",      "FOR",    "NEXT",     "DATA",		// 0x80
	"INPUT",    "DIM",    "READ",     "LET",
	"GOTO",     "RUN",    "IF",       "RESTORE",
	"GOSUB",    "RETURN", "REM",      "STOP",
	"OUT",      "ON",     "NULL",     "WAIT",		// 0x90
	"DEF",      "POKE",   "DOKE",     "AUTO",
	"LINES",    "CLS",    "WIDTH",    "BYE",
	"KEY",      "CALL",   "PRINT",    "CONT",
	"LIST",     "CLEAR",  "CLOAD",    "CSAVE",		// 0xA0
	"NEW",      "TAB(",   "TO",       "FN",
	"SPC(",     "THEN",   "NOT",      "STEP",
	"+",        "-",      "*",        "/",
	"^",        "AND",    "OR",       ">",			// 0xB0
	"=",        "<",      "SGN",      "INT",
	"ABS",      "USR",    "FRE",      "INP",
	"POS",      "SQR",    "RND",      "LN",
	"EXP",      "COS",    "SIN",      "TAN",		// 0xC0
	"ATN",      "PEEK",   "DEEK",     "POINT",
	"LEN",      "STR$",   "VAL",      "ASC",
	"CHR$",     "LEFT$",  "RIGHT$",   "MID$",
	"SET",      "RESET",  "RENUMBER", "LOCATE",		// 0xD0
	"SOUND",    "INKEY",  "MODE",     "TRON",
	"TROFF",    "PAUSE",  "EDIT",     "DIR",
	"BLOAD",    "LPRINT", "LLIST",    "BACO",
	"CONV",     "TRANS",  "ALIST",    "AEDIT",		// 0xE0
	"ASAVE",    "ALOAD",  "INSTR",    "JOY",
	"TICKS",    "TIME$",  "PI",       "UPPER$",
        "SCREEN",   "GCLS",   "PSET",     "LINE",
	"DRAWTO",   "BOX",    "CIRCLE",   "TEXT",		// 0xF0
	"PLOAD",    "PSAVE",  "DLOAD",    "DSAVE",
	"DGET",     "DPUT",   "CD" };

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
  protected boolean          rf32KActive;
  protected boolean          rf32NegA15;
  protected boolean          rfReadEnabled;
  protected boolean          rfWriteEnabled;
  protected int              rfAddr16to19;
  protected byte[]           ramModule3;
  protected byte[]           scchBasicRomBytes;
  protected byte[]           scchPrgXRomBytes;
  protected byte[]           scchRomdiskBytes;
  protected String           scchBasicRomFile;
  protected String           scchPrgXRomFile;
  protected String           scchRomdiskFile;
  protected boolean          scchBasicRomEnabled;
  protected int              scchBasicRomBegAddr;
  protected boolean          scchPrgXRomEnabled;
  protected boolean          scchRomdiskEnabled;
  protected int              scchRomdiskBegAddr;
  protected int              scchRomdiskBankAddr;
  protected volatile boolean pasteFast;
  protected boolean          v24BitOut;
  protected int              v24BitNum;
  protected int              v24ShiftBuf;
  protected int              v24TStateCounter;
  protected int              v24TStatesPerBit;

  private byte[] gsbasic;


  protected AbstractSCCHSys(
		EmuThread  emuThread,
		Properties props,
		String     propPrefix )
  {
    super( emuThread, props, propPrefix );
    this.joystickEnabled     = emulatesJoystick( props );
    this.ramModule3          = null;
    this.scchBasicRomBegAddr = getScchBasicRomBegAddr( props );
    this.scchBasicRomBytes   = null;
    this.scchBasicRomFile    = null;
    this.scchPrgXRomBytes    = null;
    this.scchPrgXRomFile     = null;
    this.scchRomdiskBytes    = null;
    this.scchRomdiskFile     = null;
    this.scchRomdiskBegAddr  = getScchRomdiskBegAddr( props );
    this.scchRomdiskBankAddr = 0;
    this.pasteFast           = false;
    this.gsbasic             = null;
  }


  protected synchronized void checkAddPCListener( Properties props )
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    if( cpu != null ) {
      boolean pasteFast = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "paste.fast",
				false );
      if( pasteFast != this.pasteFast ) {
	this.pasteFast = pasteFast;
	if( pasteFast ) {
	  cpu.addPCListener( this, ADDR_INCH );
	} else {
	  cpu.removePCListener( this );
	}
      }
    }
  }


  protected boolean emulatesFloppyDisk( Properties props )
  { 
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "floppydisk.enabled",
			false );
  }


  protected boolean emulatesJoystick( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "joystick.enabled",
			false );
  }


  protected boolean emulatesKCNet( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "kcnet.enabled",
			false );
  }


  protected boolean emulatesUSB( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "vdip.enabled",
			false );
  }


  /*
   * Lesen eines Bytes aus dem Speicher
   *
   * Rueckgabewert:
   *   >= 0: gelesenes Byte
   *   < 0:  kein SCCH-Modul an der angegebenen Adresse aktiv
   */
  protected int getScchMemByte( int addr, boolean m1 )
  {
    int     rv   = 0xFF;
    boolean done = false;
    if( !m1 && this.rfReadEnabled && (this.ramModule3 != null) ) {
      int idx = this.rfAddr16to19 | addr;
      if( (idx >= 0) && (idx < this.ramModule3.length) ) {
        rv = (int) this.ramModule3[ idx ] & 0xFF;
      }
      done = true;
    }
    if( !done && this.rf32KActive && (this.ramModule3 != null)
        && (addr >= 0x4000) && (addr < 0xC000) )
    {
      int idx = this.rfAddr16to19 | addr;
      if( this.rf32NegA15 ) {
        idx ^= 0x8000;
      }
      if( (idx >= 0) && (idx < this.ramModule3.length) ) {
        rv = (int) this.ramModule3[ idx ] & 0xFF;
      }
      done = true;
    }
    if( !done && this.scchBasicRomEnabled
        && (addr >= this.scchBasicRomBegAddr) && (addr < 0x6000) )
    {
      if( this.scchBasicRomBytes != null ) {
        int idx = addr - this.scchBasicRomBegAddr;
        if( idx < this.scchBasicRomBytes.length ) {
          rv = (int) this.scchBasicRomBytes[ idx ] & 0xFF;
        }
      }
      done = true;
    }
    if( !done && this.scchRomdiskEnabled
        && (addr >= this.scchRomdiskBegAddr) )
    {
      if( this.scchRomdiskBytes != null ) {
        int idx = this.scchRomdiskBankAddr
                        | (addr - this.scchRomdiskBegAddr);
        if( idx < this.scchRomdiskBytes.length ) {
          rv = (int) this.scchRomdiskBytes[ idx ] & 0xFF;
        }
      }
      done = true;
    }
    if( !done && this.scchPrgXRomEnabled && (addr >= 0xE000) ) {
      if( this.scchPrgXRomBytes != null ) {
        int idx = addr - 0xE000;
        if( idx < this.scchPrgXRomBytes.length ) {
          rv = (int) this.scchPrgXRomBytes[ idx ] & 0xFF;
        }
      }
      done = true;
    }
    return done ? rv : -1;
  }


  protected void loadROMs( Properties props, String basicResource )
  {
    // SCCH BASIC-ROM
    this.scchBasicRomFile = EmuUtil.getProperty(
			props,
			this.propPrefix + "scch.basic.file" );
    this.scchBasicRomBytes = readROMFile(
			this.scchBasicRomFile,
			this.scchBasicRomBegAddr < 0x4000 ? 0x4000 : 0x2000,
			"BASIC" );
    if( this.scchBasicRomBytes == null ) {
      if( this.gsbasic == null ) {
	this.gsbasic = readResource( basicResource );
      }
      this.scchBasicRomBytes = this.gsbasic;
    }

    // SCCH Programmpaket X
    this.scchPrgXRomFile  = EmuUtil.getProperty(
			props,
			this.propPrefix + "scch.program_x.file" );
    this.scchPrgXRomBytes = readROMFile(
			this.scchPrgXRomFile,
			0x2000,
			"Programmpaket X" );

    // SCCH ROM-Disk
    this.scchRomdiskFile  = EmuUtil.getProperty(
			props,
			this.propPrefix + "scch.romdisk.file" );
    this.scchRomdiskBytes = readROMFile(
			this.scchRomdiskFile,
			0x40000,
			"SCCH-Modul 1 ROM-Disk" );
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


  /*
   * Setzen eines Bytes im Arbeitsspeicher
   *
   * Rueckgabewert:
   *   < 0: kein SCCH-Modul an der angegebenen Adresse aktiv
   *   = 0: SCCH-Modul an der Adresse zwar aktiv, aber kein Byte geaendert
   *   > 0: Byte im SCCH-modul geaendert
   */
  protected int setScchMemByte( int addr, int value )
  {
    int rv = -1;
    if( this.rfWriteEnabled && (this.ramModule3 != null) ) {
      int idx = this.rfAddr16to19 | addr;
      if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	this.ramModule3[ idx ] = (byte) value;
	rv = 1;
      } else {
	rv = 0;
      }
    }
    if( (rv < 0) && this.rf32KActive && (this.ramModule3 != null)
	&& (addr >= 0x4000) && (addr < 0xC000) )
    {
      int idx = this.rfAddr16to19 | addr;
      if( this.rf32NegA15 ) {
	idx ^= 0x8000;
      }
      if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	this.ramModule3[ idx ] = (byte) value;
	rv = 1;
      } else {
	rv = 0;
      }
    }
    return rv;
  }


	/* --- Z80PCListener --- */

  @Override
  public synchronized void z80PCChanged( Z80CPU cpu, int pc )
  {
    if( this.pasteFast && (pc == ADDR_INCH) ) {
      CharacterIterator iter = this.pasteIter;
      if( iter != null ) {
	char ch = iter.next();
	while( ch != CharacterIterator.DONE ) {
	  if( (ch > 0) && (ch < 0x7F) ) {
	    cpu.setRegA( ch == '\n' ? '\r' : ch );
	    cpu.setRegPC( cpu.doPop() );
	    break;
	  }
	  ch = iter.next();
	}
	if( ch == CharacterIterator.DONE ) {
	  cancelPastingText();
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = (emulatesJoystick( props ) == this.joystickEnabled);
    if( rv ) {
      rv = TextUtil.equals(
	this.scchBasicRomFile,
	EmuUtil.getProperty( props, this.propPrefix + "scch.basic.file" ) );
    }
    if( rv && (this.scchBasicRomFile != null)) {
      rv = (this.scchBasicRomBegAddr == getScchBasicRomBegAddr( props ));
    }
    if( rv ) {
      rv = TextUtil.equals(
	this.scchPrgXRomFile,
	EmuUtil.getProperty(
			props,
			this.propPrefix + "scch.program_x.file" ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
	this.scchRomdiskFile,
	EmuUtil.getProperty( props, this.propPrefix + "scch.romdisk.file" ) );
    }
    if( rv && (this.scchRomdiskFile != null)) {
      rv = (this.scchRomdiskBegAddr == getScchRomdiskBegAddr( props ));
    }
    return rv;
  }


  @Override
  public int getSupportedJoystickCount()
  {
    return this.joystickEnabled ? 1 : 0;
  }


  @Override
  public void openBasicProgram()
  {
    String text = SourceUtil.getBasicProgram(
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
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    int     ch = 0;
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
	ch = 8;
	break;

      case KeyEvent.VK_RIGHT:
	ch = 9;
	break;

      case KeyEvent.VK_DOWN:
	ch = 0x0A;
	break;

      case KeyEvent.VK_UP:
	ch = 0x0B;
	break;

      case KeyEvent.VK_ENTER:
	ch = 0x0D;
	break;

      case KeyEvent.VK_SPACE:
	ch = 0x20;
	break;

      case KeyEvent.VK_BACK_SPACE:
	ch = 0x7F;
	break;

      case KeyEvent.VK_DELETE:
	ch = 4;
	break;

      case KeyEvent.VK_INSERT:
	ch = 5;
	break;

      case KeyEvent.VK_PAGE_UP:
	ch = 0x11;
	break;

      case KeyEvent.VK_PAGE_DOWN:
	ch = 0x15;
	break;

      case KeyEvent.VK_HOME:
	ch = 1;
	break;

      case KeyEvent.VK_END:
	ch = 0x1A;
	break;
    }
    if( ch > 0 ) {
      setKeyboardValue( ch | 0x80 );
      rv = true;
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    setKeyboardValue( 0 );
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      setKeyboardValue( ch | 0x80 );
      rv = true;
    }
    return rv;
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
    this.joystickSelected    = false;
    this.joystickValue       = 0;
    this.keyboardValue       = 0;
    this.rf32KActive         = false;
    this.rf32NegA15          = false;
    this.rfReadEnabled       = false;
    this.rfWriteEnabled      = false;
    this.rfAddr16to19        = 0;
    this.scchBasicRomEnabled = false;
    this.scchPrgXRomEnabled  = false;
    this.scchRomdiskEnabled  = false;
    this.scchRomdiskBankAddr = 0;
    this.v24BitOut           = true;	// V24: H-Pegel
    this.v24BitNum           = 0;
    this.v24ShiftBuf         = 0;
    this.v24TStateCounter    = 0;
  }


  @Override
  public synchronized void startPastingText( String text )
  {
    boolean done = false;
    if( text != null ) {
      if( !text.isEmpty() ) {
	if( this.pasteFast ) {
	  CharacterIterator iter = new StringCharacterIterator( text );
	  char              ch   = iter.first();
	  if( ch != CharacterIterator.DONE ) {
	    if( ch == '\n' ) {
	      ch = '\r';
	    }
	    /*
	     * Da sich die Programmausfuehrung i.d.R. bereits
	     * in der betreffenden Systemfunktion befindet,
	     * muss das erste Zeichen direkt an der Tastatur
	     * angelegt werden,
	     * damit der Systemaufruf beendet wird und somit
	     * der naechste Aufruf dann abgefangen werden kann.
	     */
	    keyTyped( ch );
	    long millis = (ch == '\r' ?
				getDelayMillisAfterPasteEnter()
				: getDelayMillisAfterPasteChar());
	    if( millis > 0 ) {
	      try {
		Thread.sleep( millis );
	      }
	      catch( InterruptedException ex ) {}
	    }
	    this.pasteIter = iter;
	    done           = true;
	  }
	} else {
	  super.startPastingText( text );
	  done = true;
	}
      }
    }
    if( !done ) {
      this.screenFrm.firePastingTextFinished();
    }
  }


  @Override
  public boolean supportsOpenBasic()
  {
    return true;
  }


  @Override
  public boolean supportsPasteFromClipboard()
  {
    return true;
  }


	/* --- private Methoden --- */

  private int getScchBasicRomBegAddr( Properties props )
  {
    int rv = 0x4000;
    if( props != null ) {
      if( !EmuUtil.getProperty(
			props,
			this.propPrefix + "scch.basic.file" ).isEmpty() )
      {
	String text = EmuUtil.getProperty(
			props,
			this.propPrefix + "scch.basicrom.address.begin" );
	if( text != null ) {
	  if( text.trim().startsWith( "2000" ) ) {
	    rv = 0x2000;
	  }
	}
      }
    }
    return rv;
  }


  private int getScchRomdiskBegAddr( Properties props )
  {
    int rv = 0xC000;
    if( props != null ) {
      String text = EmuUtil.getProperty(
			props,
			this.propPrefix + "scch.romdisk.address.begin" );
      if( text != null ) {
	if( text.trim().startsWith( "8000" ) ) {
	  rv = 0x8000;
	}
      }
    }
    return rv;
  }
}
