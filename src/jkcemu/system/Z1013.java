/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Z1013
 */

package jkcemu.system;

import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.print.PrintMngr;
import jkcemu.system.z1013.*;
import z80emu.*;


public class Z1013 extends EmuSys implements
					Z80AddressListener,
					Z80PCListener
{
  public static final int MEM_ARG1   = 0x001B;
  public static final int MEM_HEAD   = 0x00E0;
  public static final int MEM_SCREEN = 0xEC00;
  public static final int MEM_OS     = 0xF000;

  public static final String[] basicTokens = {
    "END",       "FOR",      "NEXT",    "DATA",		// 0x80
    "INPUT",     "DIM",      "READ",    "LET",
    "GOTO",      "RUN",      "IF",      "RESTORE",
    "GOSUB",     "RETURN",   "REM",     "STOP",
    "OUT",       "ON",       "NULL",    "WAIT",		// 0x90
    "DEF",       "POKE",     "DOKE",    "AUTO",
    "LINES",     "CLS",      "WIDTH",   "BYE",
    "!",         "CALL",     "PRINT",   "CONT",
    "LIST",      "CLEAR",    "CLOAD",   "CSAVE",	// 0xA0
    "NEW",       "TAB(",     "TO",      "FN",
    "SPC(",      "THEN",     "NOT",     "STEP",
    "+",         "-",        "*",       "/",
    "^",         "AND",      "OR",      ">",		// 0xB0
    "=",         "<",        "SGN",     "INT",
    "ABS",       "USR",      "FRE",     "INP",
    "POS",       "SQR",      "RND",     "LN",
    "EXP",       "COS",      "SIN",     "TAN",		// 0xC0
    "ATN",       "PEEK",     "DEEK",    "PI",
    "LEN",       "STR$",     "VAL",     "ASC",
    "CHR$",      "LEFT$",    "RIGHT$",  "MID$",
    "LOAD",      "TRON",     "TROFF",   "EDIT",		// 0xD0
    "ELSE",      "INKEY$",   "JOYST",   "STRING$",
    "INSTR",     "RENUMBER", "DELETE",  "PAUSE",
    "BEEP",      "WINDOW",   "BORDER",  "INK",
    "PAPER",     "AT",       "HSAVE",   "HLOAD",	// 0xE0
    "PSET",      "PRES" };

  private static final String[] sysCallNames = {
			"OUTCH", "INCH",  "PRST7", "INHEX",
			"INKEY", "INLIN", "OUTHX", "OUTHL",
			"CSAVE", "CLOAD", "MEM",   "WIND",
			"OTHLS", "OUTDP", "OUTSP", "TRANS",
			"INSTR", "KILL",  "HEXUM", "ALPHA" };

  private enum BasicType { Z1013_TINY, KC_RAM, KC_ROM };

  private static byte[] mon202         = null;
  private static byte[] monA2          = null;
  private static byte[] monRB_K7659    = null;
  private static byte[] monRB_S6009    = null;
  private static byte[] monJM_1992     = null;
  private static byte[] z1013FontBytes = null;
  private static byte[] altFontBytes   = null;

  private Z80PIO           pio;
  private Keyboard         keyboard;
  private int              ramPixelBank;
  private int              ramEndAddr;
  private byte[][]         ramPixel;
  private byte[]           ramStatic;
  private byte[]           ramVideo;
  private byte[]           romOS;
  private boolean          romDisabled;
  private boolean          altFontEnabled;
  private boolean          mode4MHz;
  private boolean          mode64x16;
  private volatile boolean modeGraph;
  private volatile boolean pasteFast;
  private volatile int     charToPaste;
  private int[]            pcListenerAddrs;
  private BasicType        lastBasicType;


  public Z1013( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( z1013FontBytes == null ) {
      z1013FontBytes = readResource( "/rom/z1013/z1013font.bin" );
    }
    if( altFontBytes == null ) {
      altFontBytes = readResource( "/rom/z1013/altfont.bin" );
    }
    String monText = EmuUtil.getProperty( props, "jkcemu.z1013.monitor" );
    if( monText.equals( "A.2" ) ) {
      if( monA2 == null ) {
	monA2 = readResource( "/rom/z1013/mon_a2.bin" );
      }
      this.romOS = monA2;
    } else if( monText.equals( "RB_K7659" ) ) {
      if( monRB_K7659 == null ) {
	monRB_K7659 = readResource( "/rom/z1013/mon_rb_k7659.bin" );
      }
      this.romOS = monRB_K7659;
    } else if( monText.equals( "RB_S6009" ) ) {
      if( monRB_S6009 == null ) {
	monRB_S6009 = readResource( "/rom/z1013/mon_rb_s6009.bin" );
      }
      this.romOS = monRB_S6009;
    } else if( monText.equals( "JM_1992" ) ) {
      if( monJM_1992 == null ) {
	monJM_1992 = readResource( "/rom/z1013/mon_jm_1992.bin" );
      }
      this.romOS = monJM_1992;
    } else {
      if( mon202 == null ) {
	mon202 = readResource( "/rom/z1013/mon_202.bin" );
      }
      this.romOS = mon202;
    }
    this.romDisabled     = false;
    this.altFontEnabled  = false;
    this.mode4MHz        = false;
    this.mode64x16       = false;
    this.modeGraph       = false;
    this.pcListenerAddrs = null;
    this.lastBasicType   = null;
    this.charToPaste     = 0;
    this.ramPixelBank    = 0;
    this.ramEndAddr      = getRAMEndAddr( props );
    this.ramVideo        = new byte[ 0x0400 ];
    this.ramStatic       = null;
    if( this.ramEndAddr == 0x03FF ) {
      this.ramStatic = new byte[ 0x0400 ];
    }

    this.ramPixel = null;
    if( isGraphicEnabled( props ) ) {
      this.ramPixel = new byte[ 8 ][];
      for( int i = 0; i < this.ramPixel.length; i++ ) {
	this.ramPixel[ i ] = new byte[ 0x0400 ];
      }
    }

    Z80CPU cpu = this.emuThread.getZ80CPU();
    this.pio   = new Z80PIO( cpu );
    cpu.setInterruptSources( this.pio );
    cpu.addAddressListener( this );
    checkAddPCListener( props );

    this.keyboard = new Keyboard( this.pio );
    this.keyboard.applySettings( props );

    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "Z1013.01" ) ?
								1000 : 2000;
  }


  public Keyboard getKeyboard()
  {
    return this.keyboard;
  }


  public static String getTinyBasicProgram( Z80MemView memory )
  {
    return SourceUtil.getTinyBasicProgram(
				memory,
				0x1152,
				memory.getMemWord( 0x101F ) );
  }


	/* --- Z80AddressListener --- */

  public void z80AddressChanged( int addr )
  {
    // Verbindung A0 - PIO B5 emulieren
    this.pio.putInValuePortB( (addr << 5) & 0x20, 0x20 );
  }


	/* --- Z80PCListener --- */

  public void z80PCChanged( Z80CPU cpu, int pc )
  {
    boolean done = false;
    switch( pc ) {
      case 0xF119:
      case 0xF130:
      case 0xF25A:
	if( this.pasteFast && (this.charToPaste != 0) ) {
	  cpu.setRegA( this.charToPaste & 0xFF );
	  this.charToPaste = 0;
	  done = true;
	}
	break;

      case 0xFFCA:	// Ausgabe Register A an physischen Druckertreiber
      case 0xFFE8:	// Ausgabe Register A an logischen Druckertreiber
	this.emuThread.getPrintMngr().putByte( cpu.getRegA() );
	done = true;
	break;

      case 0xFFCD:	// Initialisierung des logischen Druckertreibers
      case 0xFFEB:	// Zuruecksetzen des logischen Druckertreibers
	this.emuThread.getPrintMngr().reset();
	done = true;
	break;

      case 0xFFDF:	// Ausgabe ARG1 an logischen Druckertreiber
	this.emuThread.getPrintMngr().putByte( getMemByte( MEM_ARG1, false ) );
	done = true;
	break;

      case 0xFFE5:	// Bildschirm drucken
	{
	  PrintMngr printMngr = this.emuThread.getPrintMngr();
	  int       addr      = MEM_SCREEN;
	  for( int i = 0; i < 32; i++ ) {
	    for( int k = 0; k < 32; k++ ) {
	      printMngr.putByte( getMemByte( addr++, false ) );
	    }
	    printMngr.putByte( '\r' );
	    printMngr.putByte( '\n' );
	  }
	}
	done = true;
	break;
    }
    if( done ) {
      cpu.setRegPC( cpu.doPop() );
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void applySettings( Properties props )
  {
    super.applySettings( props );
    checkAddPCListener( props );
  }


  public boolean canExtractScreenText()
  {
    return true;
  }


  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeAddressListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.pcListenerAddrs != null ) {
      cpu.removePCListener( this );
    }
  }


  public int getAppStartStackInitValue()
  {
    return 0x00B0;
  }


  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    int b  = 0;
    if( (this.ramPixel != null) && this.modeGraph ) {
      int bank = y % (this.mode64x16 ? 16 : 8);
      if( (bank >= 0) && (bank < this.ramPixel.length) ) {
	byte[] ram = this.ramPixel[ bank ];
	if( ram != null ) {
	  int idx = -1;
	  int col = x / 8;
	  if( this.mode64x16 ) {
	    int rPix = y % 16;
	    if( rPix < 8 ) {
	      int row = y / 16;
	      idx = (row * 64) + col;
	    }
	  } else {
	    int row = y / 8;
	    idx = (row * 32) + col;
	  }
	  if( (idx >= 0) && (idx < ram.length) ) {
	    b = (byte) ~ram[ idx ] & 0xFF;
	  }
	}
      }
    } else {
      byte[] fontBytes = this.emuThread.getExtFontBytes();
      if( fontBytes == null ) {
	if( this.altFontEnabled ) {
	  fontBytes = altFontBytes;
	} else {
	  fontBytes = z1013FontBytes;
	}
      }
      if( fontBytes != null ) {
	int col = x / 8;
	int offs = -1;
	int rPix = 0;
	if( this.mode64x16 ) {
	  int row = y / 16;
	  offs    = (row * 64) + col;
	  rPix    = y % 16;
	} else {
	  int row = y / 8;
	  offs    = (row * 32) + col;
	  rPix    = y % 8;
	}
	if( (rPix < 8) && (offs >= 0) && (offs < this.ramVideo.length) ) {
	  int idx = ((int) this.ramVideo[ offs ] & 0xFF) * 8 + rPix;
	  if( (idx >= 0) && (idx < fontBytes.length) ) {
	    b = fontBytes[ idx ];
	  }
	}
      }
    }
    if( b != 0 ) {
      int m = 0x80;
      int n = x % 8;
      if( n > 0 ) {
	m >>= n;
      }
      if( (b & m) != 0 ) {
	rv = WHITE;
      }
    }
    return rv;
  }


  public int getCharColCount()
  {
    return this.mode64x16 ? 64 : 32;
  }


  public int getCharHeight()
  {
    return 8;
  }


  public int getCharRowCount()
  {
    return this.mode64x16 ? 16 : 32;
  }


  public int getCharRowHeight()
  {
    return this.mode64x16 ? 16 : 8;
  }


  public int getCharWidth()
  {
    return 8;
  }


  public long getDelayMillisAfterPasteChar()
  {
    return this.pasteFast ? 0 : 150;
  }


  public long getDelayMillisAfterPasteEnter()
  {
    return this.pasteFast ? 0 : 200;
  }


  public long getHoldMillisPasteChar()
  {
    return this.pasteFast ? 0 : 80;
  }


  public String getHelpPage()
  {
    return "/help/z1013.htm";
  }


  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( !this.romDisabled && (this.romOS != null) ) {
      if( (addr >= MEM_OS) && (addr < MEM_OS + this.romOS.length) ) {
	rv   = (int) this.romOS[ addr - MEM_OS ] & 0xFF;
	done = true;
      }
    }
    if( (addr >= MEM_SCREEN) && (addr < MEM_SCREEN + this.ramVideo.length) ) {
      byte[] ram = null;
      if( (this.ramPixel != null) && this.modeGraph ) {
	if( (this.ramPixelBank >= 0)
	    && (this.ramPixelBank < this.ramPixel.length) )
	{
	  ram = this.ramPixel[ this.ramPixelBank ];
	}
      } else {
	ram = this.ramVideo;
      }
      if( ram != null ) {
	int idx = addr - MEM_SCREEN;
	if( (idx >= 0) && (idx < ram.length) ) {
	  rv = (int) ram[ idx ] & 0xFF;
	}
      }
      done = true;
    }
    if( !done && (this.ramStatic != null) ) {
      if( addr < this.ramStatic.length ) {
	rv   = (int) this.ramStatic[ addr ] & 0xFF;
	done = true;
      }
    }
    if( !done && (addr <= this.ramEndAddr) ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return MEM_OS;
  }


  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int idx = (chY * (this.mode64x16 ? 64 : 32)) + chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ] & 0xFF;
      if( (b >= 0x20) && (b < 0x7F) ) {
	ch = b;
      }
      else if( (ch >= 0xA0) && (ch < 0xFF) && this.altFontEnabled ) {
	ch = b & 0x7F; 		// 0xA0 bis 0xFE: invertierte Zeichen
      }
    }
    return ch;
  }


  public int getScreenHeight()
  {
    return this.mode64x16 ? 248 : 256;
  }


  public int getScreenWidth()
  {
    return this.mode64x16 ? 512 : 256;
  }


  public int getSupportedRAMFloppyCount()
  {
    return 2;
  }


  public String getTitle()
  {
    return "Z1013";
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  protected boolean isExtROMSwitchableAt( int addr )
  {
    return !this.romDisabled || (addr < MEM_OS);
  }


  public boolean isISO646DE()
  {
    return this.altFontEnabled;
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    return this.keyboard.setKeyCode( keyCode );
  }


  public void keyReleased()
  {
    this.keyboard.setKeyReleased();
  }


  public boolean keyTyped( char ch )
  {
    return this.keyboard.setKeyChar( ch );
  }


  public void openBasicProgram()
  {
    boolean   cancelled = false;
    BasicType bType     = null;
    String    text      = null;
    int       preIdx    = -1;
    if( this.lastBasicType != null ) {
      switch( this.lastBasicType ) {
	case Z1013_TINY:
	  preIdx = 0;
	  break;

	case KC_RAM:
	  preIdx = 1;
	  break;

	case KC_ROM:
	  preIdx = 2;
	  break;
      }
    }
    switch( OptionDlg.showOptionDlg(
		this.screenFrm,
		"W\u00E4hlen Sie bitte den BASIC-Interpreter aus,\n"
			+ "dessen BASIC-Programm ge\u00F6ffnet werden soll.",
		"BASIC-Interpreter",
		preIdx,
		"Z1013-Tiny-BASIC",
		"KC-BASIC (RAM-Version)",
		"KC-BASIC (ROM-Version)" ) )
    {
      case 0:
	bType = BasicType.Z1013_TINY;
	text  = getTinyBasicProgram( this.emuThread );
        break;

      case 1:
        bType = BasicType.KC_RAM;
	text  = SourceUtil.getKCBasicStyleProgram(
					this.emuThread,
					0x2C01,
					basicTokens );
	break;

      case 2:
        bType = BasicType.KC_ROM;
	text  = SourceUtil.getKCBasicStyleProgram(
					this.emuThread,
					0x0401,
					basicTokens );
	break;
    }
    if( !cancelled ) {
      if( text != null ) {
	this.lastBasicType = bType;
	this.screenFrm.openText( text );
      } else {
	showNoBasic();
      }
    }
  }


  public boolean pasteChar( char ch )
  {
    boolean rv = false;
    if( this.pasteFast ) {
      if( (ch > 0) && (ch <= 0xFF) ) {
	if( ch == '\n' ) {
	  ch = '\r';
	}
	try {
	  while( this.charToPaste != 0 ) {
	    Thread.sleep( 10 );
	  }
	  this.charToPaste = ch;
	  rv = true;
	}
	catch( InterruptedException ex ) {}
      }
    } else {
      rv = super.pasteChar( ch );
    }
    return rv;
  }


  public int readIOByte( int port )
  {
    int value = 0x0F;			// wird von unbelegten Ports gelesen

    if( (port & 0xFF) == 4 ) {
      value = 0;
      if( this.romDisabled ) {
	value |= 0x10;
      }
      if( this.altFontEnabled ) {
	value |= 0x20;
      }
      if( this.mode4MHz ) {
	value |= 0x40;
      }
      if( this.mode64x16 ) {
	value |= 0x80;
      }
    }
    else if( (port & 0xF8) == 0x98 ) {
      value = this.emuThread.getRAMFloppy1().readByte( port & 0x07 );
    }
    else if( (port & 0xF8) == 0x58 ) {
      value = this.emuThread.getRAMFloppy2().readByte( port & 0x07 );
    } else {
      switch( port & 0x1C ) {		// Adressleitungen ab A5 ignorieren
	case 0:				// IOSEL0 -> PIO
	  switch( port & 0x03 ) {
	    case 0:
	      value = this.pio.readPortA();
	      break;

	    case 1:
	      value = this.pio.readControlA();
	      break;

	    case 2:
	      this.pio.putInValuePortB(
			this.emuThread.readAudioPhase() ? 0x40 : 0, 0x40 );
	      value = this.pio.readPortB();
	      break;

	    case 3:
	      value = this.pio.readControlB();
	      break;
	  }
	  break;

	case 0x0C:				// IOSEL3 -> Vollgrafik ein
	  this.modeGraph = true;
	  this.screenFrm.setScreenDirty( true );
	  break;

	case 0x10:				// IOSEL3 -> Vollgrafik aus
	  this.modeGraph = true;
	  this.screenFrm.setScreenDirty( false );
	  break;
      }
    }
    z80AddressChanged( port );
    return value;
  }


  public int reassembleSysCall(
			int           addr,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    int rv  = 0;
    int bol = buf.length();
    int b   = this.emuThread.getMemByte( addr, true );
    if( b == 0xE7 ) {
      buf.append( String.format( "%04X  E7", addr ) );
      appendSpacesToCol( buf, bol, colMnemonic );
      buf.append( "RST" );
      appendSpacesToCol( buf, bol, colArgs );
      buf.append( "20H\n" );
      rv = 1;
    } else if( b == 0xCD ) {
      if( getMemWord( addr + 1 ) == 0x0020 ) {
	buf.append( String.format( "%04X  CD 00 20", addr ) );
	appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "CALL" );
	appendSpacesToCol( buf, bol, colArgs );
	buf.append( "0020H\n" );
	rv = 3;
      }
    }
    if( rv > 0 ) {
      addr += rv;

      bol = buf.length();
      b   = this.emuThread.getMemByte( addr, false );
      buf.append( String.format( "%04X  %02X", addr, b ) );
      appendSpacesToCol( buf, bol, colMnemonic );
      buf.append( "DB" );
      appendSpacesToCol( buf, bol, colArgs );
      if( b >= 0xA0 ) {
	buf.append( (char) '0' );
      }
      buf.append( String.format( "%02XH", b ) );
      if( (b >= 0) && (b < sysCallNames.length) ) {
	appendSpacesToCol( buf, bol, colRemark );
	buf.append( (char) ';' );
	buf.append( sysCallNames[ b ] );
      }
      buf.append( (char) '\n' );
      rv++;
      if( b == 2 ) {
	rv += reassStringBit7(
			this.emuThread,
			addr + 1,
			buf,
			colMnemonic,
			colArgs );
      }
    }
    return rv;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System oder
   * das Monitorprogramm aendert oder wenn der RAM
   * von 64 auf 16 KByte reduziert wird.
   */
  public boolean requiresReset( Properties props )
  {
    boolean rv = !EmuUtil.getProperty(
				props,
				"jkcemu.system" ).startsWith( "Z1013" );
    if( !rv ) {
      String monText = EmuUtil.getProperty( props, "jkcemu.z1013.monitor" );
      if( monText.equals( "A.2" ) ) {
	rv = (this.romOS != monA2);
      } else if( monText.equals( "RB_K7659" ) ) {
	rv = (this.romOS != monRB_K7659);
      } else if( monText.equals( "RB_S6009" ) ) {
	rv = (this.romOS != monRB_S6009);
      } else if( monText.equals( "JM_1992" ) ) {
	rv = (this.romOS != monJM_1992);
      } else {
	rv = (this.romOS != mon202);
      }
    }
    if( !rv && (getRAMEndAddr( props ) != this.ramEndAddr) ) {
      rv = true;
    }
    if( !rv && (isGraphicEnabled( props ) != (this.ramPixel != null)) ) {
      rv = true;
    }
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    boolean old64x16 = this.mode64x16;

    this.romDisabled    = false;
    this.altFontEnabled = false;
    this.mode64x16      = false;
    this.modeGraph      = false;
    if( this.mode4MHz ) {
      this.emuThread.updCPUSpeed( Main.getProperties() );
      this.mode4MHz = false;
    }
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( this.ramPixel != null ) {
	for( int i = 0; i < this.ramPixel.length; i++ ) {
	  fillRandom( this.ramPixel[ i ] );
	}
      }
      if( this.ramStatic != null ) {
	initSRAM( this.ramStatic, props );
      }
      fillRandom( this.ramVideo );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.pio.reset( true );
    } else {
      this.pio.reset( false );
    }
    this.keyboard.reset();

    if( old64x16 ) {
      this.screenFrm.fireScreenSizeChanged();
    }
  }


  public void saveBasicProgram()
  {
    boolean   cancelled = false;
    boolean   kcbasic   = false;
    int       begAddr   = -1;
    int       endAddr   = -1;
    int       hsType    = -1;
    BasicType bType     = null;
    int       preIdx    = -1;
    if( this.lastBasicType != null ) {
      switch( this.lastBasicType ) {
	case Z1013_TINY:
	  preIdx = 0;
	  break;

	case KC_RAM:
	  preIdx = 1;
	  break;

	case KC_ROM:
	  preIdx = 2;
	  break;
      }
    }
    switch( OptionDlg.showOptionDlg(
		this.screenFrm,
		"W\u00E4hlen Sie bitte den BASIC-Interpreter aus,\n"
			+ "dessen BASIC-Programm gespeichert werden soll.",
		"BASIC-Interpreter",
		preIdx,
		"Z1013-Tiny-BASIC",
		"KC-BASIC (RAM-Version)",
		"KC-BASIC (ROM-Version)" ) )
    {
      case 0:
	endAddr = this.emuThread.getMemWord( 0x101F );
	if( (endAddr > 0x1152)
	  && (this.emuThread.getMemByte( endAddr - 1, false ) == 0x0D) )
	{
	  this.lastBasicType = BasicType.Z1013_TINY;
	  begAddr            = 0x1000;
	  hsType             = 'b';
	}
	break;

      case 1:
	endAddr = SourceUtil.getKCBasicStyleEndAddr( this.emuThread, 0x2C01 );
	if( endAddr > 0x2C01 ) {
	  this.lastBasicType = BasicType.KC_RAM;
	  begAddr            = 0x2BC0;
	  hsType             = 'B';
	  kcbasic            = true;
	}
	break;

      case 2:
	endAddr = SourceUtil.getKCBasicStyleEndAddr( this.emuThread, 0x0401 );
	if( endAddr > 0x0401 ) {
	  this.lastBasicType = BasicType.KC_ROM;
	  begAddr            = 0x03C0;
	  hsType             = 'B';
	  kcbasic            = true;
	}
	break;
    }
    if( !cancelled ) {
      if( (begAddr > 0) && (endAddr > begAddr) ) {
	(new SaveDlg(
		this.screenFrm,
		begAddr,
		endAddr,
		hsType,
		kcbasic,
		"BASIC-Programm speichern" )).setVisible( true );
      } else {
	showNoBasic();
      }
    }
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( !this.romDisabled && (this.romOS != null) ) {
      if( (addr >= MEM_OS) && (addr < MEM_OS + this.romOS.length) ) {
	done = true;
      }
    }
    if( !done ) {
      if( (addr >= MEM_SCREEN)
	  && (addr < MEM_SCREEN + this.ramVideo.length) )
      {
	byte[] ram = null;
	if( (this.ramPixel != null) && this.modeGraph ) {
	  if( (this.ramPixelBank >= 0)
	      && (this.ramPixelBank < this.ramPixel.length) )
	  {
	    ram = this.ramPixel[ this.ramPixelBank ];
	  }
	} else {
	  ram = this.ramVideo;
	}
	if( ram != null ) {
	  int idx = addr - MEM_SCREEN;
	  if( (idx >= 0) && (idx < ram.length) ) {
	    ram[ idx ] = (byte) value;
	    this.screenFrm.setScreenDirty( true );
	    rv = true;
	  }
	}
	done = true;
      }
    }
    if( !done && (this.ramStatic != null) ) {
      if( addr < this.ramStatic.length ) {
	this.ramStatic[ addr ] = (byte) value;
	done = true;
      }
    }
    if( !done && (addr <= this.ramEndAddr) ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  public boolean supportsPasteFromClipboard()
  {
    return true;
  }


  public boolean supportsAudio()
  {
    return true;
  }


  public void updSysCells(
			int    begAddr,
			int    len,
			Object fileFmt,
			int    fileType )
  {
    SourceUtil.updKCBasicSysCells(
			this.emuThread,
			begAddr,
			len,
			fileFmt,
			fileType );
  }


  public void writeIOByte( int port, int value )
  {
    if( (port & 0xFF) == 4 ) {
      boolean oldAltFontEnabled = this.mode64x16;
      boolean oldMode64x16      = this.mode64x16;

      this.romDisabled    = ((value & 0x10) != 0);
      this.altFontEnabled = ((value & 0x20) != 0);
      this.mode64x16      = ((value & 0x80) != 0);

      Z80CPU cpu = this.emuThread.getZ80CPU();
      if( (value & 0x40) != 0 ) {
	if( !this.mode4MHz && (cpu.getMaxSpeedKHz() == 2000) ) {
	  cpu.setMaxSpeedKHz( 4000 );
	  this.mode4MHz = true;
	}
      } else {
	if( this.mode4MHz && (cpu.getMaxSpeedKHz() == 4000) ) {
	  cpu.setMaxSpeedKHz( 2000 );
	}
	this.mode4MHz = false;
      }

      if( (this.altFontEnabled != oldAltFontEnabled)
	  || (this.mode64x16 != oldMode64x16) )
      {
	this.screenFrm.setScreenDirty( true );
      }
      if( this.mode64x16 != oldMode64x16 ) {
	this.screenFrm.fireScreenSizeChanged();
      }
    }
    else if( (port & 0xF8) == 0x98 ) {
      this.emuThread.getRAMFloppy1().writeByte( port & 0x07, value );
    }
    else if( (port & 0xF8) == 0x58 ) {
      this.emuThread.getRAMFloppy2().writeByte( port & 0x07, value );
    } else {
      switch( port & 0x1C ) {		// Adressleitungen ab A5 ignorieren
	case 0:				// IOSEL0 -> PIO
	  switch( port & 0x03 ) {
	    case 0:
	      this.pio.writePortA( value );
	      break;

	    case 1:
	      this.pio.writeControlA( value );
	      break;

	    case 2:
	      this.pio.writePortB( value );
	      this.keyboard.putRowValuesToPIO();
	      this.emuThread.writeAudioPhase(
			(this.pio.fetchOutValuePortB( false ) & 0x80) != 0 );
	      break;

	    case 3:
	      this.pio.writeControlB( value );
	      break;
	  }
	  break;

	case 8:					// IOSEL2 -> Tastaturspalten
	  this.keyboard.setSelectedCol( value & 0x0F );
	  if( this.ramPixel != null ) {
	    value &= 0x0F;
	    if( value == 8 ) {
	      this.modeGraph = true;
	      this.screenFrm.setScreenDirty( true );
	    }
	    else if( value == 9 ) {
	      this.modeGraph = false;
	      this.screenFrm.setScreenDirty( true );
	    }
	    this.ramPixelBank = (value & 0x07);
	  }
	  break;

	case 0x0C:				// IOSEL3
	  if( this.ramPixel != null ) {
	    this.modeGraph = true;
	    this.screenFrm.setScreenDirty( true );
	  }
	  break;

	case 0x10:				// IOSEL3
	  if( this.ramPixel != null ) {
	    this.modeGraph = false;
	    this.screenFrm.setScreenDirty( true );
	  }
	  break;
      }
    }
    z80AddressChanged( port );
  }


	/* --- private Methoden --- */

  private synchronized void checkAddPCListener( Properties props )
  {
    this.pasteFast = EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.paste.fast",
				true );
    java.util.List<Integer> addrs = new ArrayList<Integer>();
    if( this.pasteFast ) {
      String monText = EmuUtil.getProperty( props, "jkcemu.z1013.monitor" );
      if( monText.equals( "A.2" ) ) {
	addrs.add( new Integer( 0xF119 ) );
      } else if( monText.equals( "JM_1992" ) ) {
	addrs.add( new Integer( 0xF25A ) );
      } else {
	addrs.add( new Integer( 0xF130 ) );
      }
    }
    if( EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.catch_print_calls",
				true ) )
    {
      addrs.add( new Integer( 0xFFCA ) );
      addrs.add( new Integer( 0xFFCD ) );
      addrs.add( new Integer( 0xFFDF ) );
      addrs.add( new Integer( 0xFFE5 ) );
      addrs.add( new Integer( 0xFFE8 ) );
      addrs.add( new Integer( 0xFFEB ) );
    }
    int[] a = null;
    int   n = addrs.size();
    if( n > 0 ) {
      a = new int[ n ];
      for( int i = 0; i < n; i++ ) {
	a[ i ] = addrs.get( i ).intValue();
      }
    }
    boolean state = false;
    if( (a != null) && (this.pcListenerAddrs != null) ) {
      state = Arrays.equals( a, this.pcListenerAddrs );
    } else {
      if( (a == null) && (this.pcListenerAddrs == null) ) {
	state = true;
      }
    }
    if( !state ) {
      Z80CPU cpu = this.emuThread.getZ80CPU();
      synchronized( this ) {
	if( this.pcListenerAddrs != null ) {
	  cpu.removePCListener( this );
	  this.pcListenerAddrs = null;
	}
	if( a != null ) {
	  cpu.addPCListener( this, a );
	  this.pcListenerAddrs = a;
	}
      }
    }
  }


  private static boolean isGraphicEnabled( Properties props )
  {
    return EmuUtil.getBooleanProperty( props, "jkcemu.z1013.graphic", false );
  }


  private static int getRAMEndAddr( Properties props )
  {
    int    rv      = 0xFFFF;
    String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( sysName.startsWith( "Z1013.01" )
	|| sysName.startsWith( "Z1013.16" ) )
    {
      rv = 0x3FFF;
    }
    else if( sysName.startsWith( "Z1013.12" ) ) {
      rv = 0x03FF;
    }
    return rv;
  }
}

