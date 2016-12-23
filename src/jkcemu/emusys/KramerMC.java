/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des KramerMC
 */

package jkcemu.emusys;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuMemView;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.SaveDlg;
import jkcemu.base.SourceUtil;
import jkcemu.base.FileFormat;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;
import z80emu.Z80MemView;
import z80emu.Z80PCListener;
import z80emu.Z80PIO;


public class KramerMC extends EmuSys implements Z80PCListener
{
  public static final String SYSNAME          = "KramerMC";
  public static final String SYSTEXT          = "Kramer-MC";
  public static final String PROP_PREFIX      = "jkcemu.kramermc.";
  public static final String PROP_ROM0_PREFIX = "rom.0000.";
  public static final String PROP_ROM8_PREFIX = "rom.8000.";
  public static final String PROP_ROMC_PREFIX = "rom.c000.";

  public static final int     DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX = 300;
  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE            = true;

  private static final int[][] kbMatrixNormal = {
		{ 0,   0,   ';', '<', 3,   0,   0,    ':'  },
		{ 'P', 'O', 0,   'K', 0,   'L', '0',  '9'  },
		{ 'I', 'U', 'N', 'H', 'M', 'J', '8',  '7'  },
		{ 'Z', 'T', 'V', 'F', 'B', 'G', '6',  '5'  },
		{ 'R', 'E', 'X', 'S', 'C', 'D', '4',  '3'  },
		{ 'W', 'Q', 0,   0,   'Y', 'A', '2',  '1'  },
		{ 0,   0,   0,   0,   0,   '>', '?',  '='  } };

  private static final int[][] kbMatrixShift = {
		{ 0,   0,   '+', ',', 0,   0,   0,    '*'  },
		{ 'p', 'o', 0,   'k', 0,   'l', 0x20, ')'  },
		{ 'i', 'u', 'n', 'h', 'm', 'j', '(',  '\'' },
		{ 'z', 't', 'v', 'f', 'b', 'g', '&',  '%'  },
		{ 'r', 'e', 'x', 's', 'c', 'd', '$',  '#'  },
		{ 'w', 'q', 0,   0,   'y', 'a', '\"', '!'  },
		{ 0,   0,   0,   0,   0,   '.', '/',  '-'  } };

  private static final String[] basicTokens = {
	"END",      "FOR",       "NEXT",      "DATA",		// 0x80
	"INPUT",    "DIM",       "READ",      "LET",
	"GO TO",    "FNEND",     "IF",        "RESTORE",
	"GO SUB",   "RETURN",    "REM",       "STOP",
	"OUT",      "ON",        "NULL",      "WAIT",		// 0x90
	"DEF",      "POKE",      "PRINT",     "?",
	"LISTEN",   "CLEAR",     "FNRETURN",  "SAVE",
	"!",        "USING",     "TAB(",      "TO",
	"FN",       "SPC(",      "THEN",      "NOT",		// 0xA0
	"STEP",     "+",         "-",         "*",
	"/",        "^",         "AND",       "OR",
	">",        "=",         "<",         "SGN",
	"INT",      "ABS",       "USR",       "FRE",		// 0xB0
	"INP",      "POS",       "SQR",       "RND",
	"LOG",      "EXP",       "COS",       "SIN",
	"TAN",      "ATN",       "PEEK",      "LEN",
	"STR$",     "VAL",       "ASC",       "CHR$",		// 0xC0
	"LEFT$",    "RIGHT$",    "MID$",      "LPOS",
	"INSTR",    "ELSE",      "LPRINT",    "TRACE",
	"LTRACE",   "RANDOMIZE", "SWITCH",    "LWIDTH",
	"LNULL",    "WIDTH",     "LVAR",      "LLVAL",		// 0xD0
	"SPEAK",    "\'",        "PRECISION", "CALL",
	"KILL",     "EXCHANGE",  "LINE",      "LOADGO",
	"RUN",      "LOAD",      "NEW",       "AUTO",
	"COPY",     "ALOADC",    "AMERGEC",   "ALOAD",		// 0xE0
	"AMERGE",   "ASAVE",     "LIST",      "LLIST",
	"RENUMBER", "DELETE",    "EDIT",      "CONT" };

  private static final String[] sysCallNames = {
			"CI", "RI",   "CO",    "WO",
			"LO", "CSTS", "IOCHK", "IOSET" };

  private static byte[] rom0000 = null;
  private static byte[] rom8000 = null;
  private static byte[] romC000 = null;
  private static byte[] romFont = null;

  private String       rom0000File;
  private String       rom8000File;
  private String       romC000File;
  private byte[]       fontBytes;
  private byte[]       ramVideo;
  private byte[]       rom0000Bytes;
  private byte[]       rom8000Bytes;
  private byte[]       romC000Bytes;
  private int[]        kbMatrix;
  private int          kbRow;
  private int          kbTStates;
  private int          kbTStateCounter;
  private volatile int kbStatus;
  private boolean      shiftPressed;
  private boolean      ctrlPressed;
  private boolean      printerSupported;
  private boolean      pcListenerAdded;
  private Z80PIO       pio;


  public KramerMC( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.rom0000File  = null;
    this.rom0000Bytes = null;
    this.rom8000File  = null;
    this.rom8000Bytes = null;
    this.romC000File  = null;
    this.romC000Bytes = null;
    this.ramVideo     = new byte[ 0x0400 ];
    this.kbMatrix     = new int[ 8 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio   = new Z80PIO( "PIO (E/A-Adressen FC-FF)" );
    cpu.setInterruptSources( this.pio );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    this.pcListenerAdded = false;
    checkAddPCListener( props );
    z80MaxSpeedChanged( cpu );

    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
  }


  public static String getBasicProgram( EmuMemView memory )
  {
    return SourceUtil.getBasicProgram( memory, 0x1001, basicTokens );
  }


  public static int getDefaultSpeedKHz()
  {
    return 1500;
  }


	/* --- Z80PCListener --- */

  @Override
  public void z80PCChanged( Z80CPU cpu, int pc )
  {
    if( pc == 0x00EC ) {
      this.emuThread.getPrintMngr().putByte( cpu.getRegC() );
      cpu.setRegPC( cpu.doPop() );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    checkAddPCListener( props );
    loadFont( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
    if( rv ) {
      rv = TextUtil.equals(
		this.rom0000File,
		EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROM0_PREFIX + PROP_FILE ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.rom8000File,
		EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROM8_PREFIX + PROP_FILE ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.romC000File,
		EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROMC_PREFIX + PROP_FILE ) );
    }
    return rv;
  }


  @Override
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.pcListenerAdded ) {
      cpu.removePCListener( this );
    }
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x0FCE;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    if( this.fontBytes != null ) {
      int row = y / 8;
      int col = x / 6;
      int idx = (this.emuThread.getMemByte( 0xFC00 + (row * 64) + col, false )
								* 8) + (y % 8);
      if( (idx >= 0) && (idx < this.fontBytes.length ) ) {
	int m = 0x80;
	int n = x % 6;
	if( n > 0 ) {
	  m >>= n;
	}
	if( (this.fontBytes[ idx ] & m) != 0 ) {
	  rv = WHITE;
	}
      }
    }
    return rv;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    return new CharRaster( 64, 16, 8, 8, 6, 0 );
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return 150;
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return 250;
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return 150;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/kramermc.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( (addr < 0x0C00) && (this.rom0000Bytes != null) ) {
      if( addr < this.rom0000Bytes.length ) {
	rv   = (int) this.rom0000Bytes[ addr ] & 0xFF;
	done = true;
      }
    }
    if( !done && (addr >= 0x8000) && (addr < 0xB000)
	&& (this.rom8000Bytes != null) ) {
      int idx = addr - 0x8000;
      if( idx < this.rom8000Bytes.length ) {
	rv   = (int) this.rom8000Bytes[ idx ] & 0xFF;
	done = true;
      }
    }
    if( !done && (addr >= 0xC000) && (addr < 0xE000)
	&& (this.romC000Bytes != null) ) {
      int idx = addr - 0xC000;
      if( idx < this.romC000Bytes.length ) {
	rv   = (int) this.romC000Bytes[ idx ] & 0xFF;
	done = true;
      }
    }
    if( !done && (addr >= 0xFC00) ) {
      int idx = addr - 0xFC00;
      if( idx < this.ramVideo.length ) {
	rv   = (int) this.ramVideo[ idx ] & 0xFF;
	done = true;
      }
    }
    if( !done ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  @Override
  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return 0;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch  = -1;
    int idx = (chY * 64) + chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ] & 0xFF;
      if( (b >= 0x20) && (b < 0x7F) ) {
	ch = b;
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return 128;
  }


  @Override
  public int getScreenWidth()
  {
    return 384;
  }


  @Override
  public String getTitle()
  {
    return SYSTEXT;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return DEFAULT_SWAP_KEY_CHAR_CASE;
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.kbMatrix ) {
      if( this.kbStatus == 0 ) {
	switch( keyCode ) {
	  case KeyEvent.VK_BACK_SPACE:
	  case KeyEvent.VK_LEFT:
	    this.kbMatrix[ 1 ] = 0x01;
	    rv = true;
	    break;

	  case KeyEvent.VK_RIGHT:
	    this.kbMatrix[ 0 ] = 0x40;
	    rv = true;
	    break;

	  case KeyEvent.VK_DOWN:
	    this.kbMatrix[ 6 ] = 0x01;
	    rv = true;
	    break;

	  case KeyEvent.VK_UP:
	    this.kbMatrix[ 0 ] = 0x01;
	    rv = true;
	    break;

	  case KeyEvent.VK_ENTER:
	    this.kbMatrix[ 4 ] = 0x02;
	    rv = true;
	    break;

	  case KeyEvent.VK_SPACE:
	    this.kbMatrix[ 1 ] = 0x40;
	    rv = true;
	    break;

	  case KeyEvent.VK_DELETE:
	    this.kbMatrix[ 2 ] = 0x02;
	    rv = true;
	    break;
	}
	if( rv ) {
	  this.kbTStateCounter = 0;
	  this.kbStatus        = 1;
	  this.shiftPressed    = false;
	  this.ctrlPressed     = false;
	}
      }
    }
    return rv;
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    synchronized( this.kbMatrix ) {
      if( (ch >= 1) && (ch <='\u0020') && (ch != 3) ) {
	if( setCharInKBMatrix( ch + 0x40, kbMatrixShift ) ) {
	  this.kbTStateCounter = 0;
	  this.kbStatus        = 3;
	  this.shiftPressed    = false;
	  this.ctrlPressed     = true;
	  rv                   = true;
	}
      } else {
	if( setCharInKBMatrix( ch, kbMatrixNormal ) ) {
	  this.kbTStateCounter = 0;
	  this.kbStatus        = 1;
	  this.shiftPressed    = false;
	  this.ctrlPressed     = false;
	  rv                   = true;
	} else {
	  if( setCharInKBMatrix( ch, kbMatrixShift ) ) {
	    this.kbTStateCounter = 0;
	    this.kbStatus        = 3;
	    this.shiftPressed    = true;
	    this.ctrlPressed     = false;
	    rv                   = true;
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public void openBasicProgram()
  {
    String text = SourceUtil.getBasicProgram(
					this.emuThread,
					0x1001,
					basicTokens );
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoBasic();
    }
  }


  @Override
  protected boolean pasteChar( char ch ) throws InterruptedException
  {
    boolean rv = false;
    switch( ch ) {
      case '\n':
      case '\r':
	rv = keyPressed( KeyEvent.VK_ENTER, false, false );
	break;

      case '\u0020':
	rv = keyPressed( KeyEvent.VK_SPACE, false, false );
	break;

      default:
	rv = keyTyped( ch );
    }
    if( rv ) {
      while( this.kbStatus > 0 ) {
	Thread.sleep( 50 );
      }
    }
    return rv;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int value = 0xFF;
    switch( port & 0xFF ) {
      case 0xFC:
	value = this.pio.readDataA();
	break;

      case 0xFD:
	updKBColValue();
	value = this.pio.readDataB();
	break;

      case 0xFE:
	value = this.pio.readControlA();
	break;

      case 0xFF:
	value = this.pio.readControlB();
	break;
    }
    return value;
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
    return reassSysCallTable(
			memory,
			addr,
			0x00E0,
			sysCallNames,
			buf,
			sourceOnly,
			colMnemonic,
			colArgs,
			colRemark );
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    super.reset( resetLevel, props );
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
      fillRandom( ramVideo );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.pio.reset( true );
    } else {
      this.pio.reset( false );
    }
    synchronized( this.kbMatrix ) {
      Arrays.fill( this.kbMatrix, 0 );
      this.kbRow           = 0;
      this.kbTStateCounter = 0;
      this.kbStatus        = 0;
      this.shiftPressed    = false;
      this.ctrlPressed     = false;
    }
  }


  @Override
  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getBasicEndAddr( this.emuThread, 0x1001 );
    if( endAddr >= 0x1001 ) {
      (new SaveDlg(
		this.screenFrm,
		0x1001,
		endAddr,
		"BASIC-Programm speichern",
		SaveDlg.BasicType.MS_DERIVED_BASIC,
		EmuUtil.getBasicFileFilter() )).setVisible( true );
    } else {
      showNoBasic();
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( ((addr >= 0x0C00) && (addr < 0x8000))
	|| ((addr >= 0xB000) && (addr < 0xC000))
	|| ((addr >= 0xE000) && (addr < 0xFC00)) )
    {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    } else if( addr >= 0xFC00 ) {
      int idx = addr - 0xFC00;
      if( idx < this.ramVideo.length ) {
	this.ramVideo[ idx ] = (byte) value;
	this.screenFrm.setScreenDirty( true );
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return this.fontBytes != romFont;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
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


  @Override
  public boolean supportsPrinter()
  {
    return this.pcListenerAdded;
  }


  @Override
  public boolean supportsSaveBasic()
  {
    return true;
  }


  @Override
  public void updSysCells(
			int        begAddr,
			int        len,
			FileFormat fileFmt,
			int        fileType )
  {
    if( fileFmt != null ) {
      if( (fileFmt.equals( FileFormat.BASIC_PRG )
		&& (begAddr == 0x1001)
		&& (len > 7))
	  || (fileFmt.equals( FileFormat.HEADERSAVE )
		&& (fileType == 'B')
		&& (begAddr <= 0x1001)
		&& ((begAddr + len) > 0x1007)) )
      {
	int topAddr = begAddr + len;
	if( topAddr > 0x1001 ) {
	  this.emuThread.setMemWord( 0x0C5E, topAddr );
	  this.emuThread.setMemWord( 0x0C60, topAddr );
	  this.emuThread.setMemWord( 0x0C62, topAddr );
	}
      }
    }
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    switch( port & 0xFF ) {
      case 0xFC:
	this.pio.writeDataA( value );
	this.kbRow = (this.pio.fetchOutValuePortA( false ) >> 1) & 0x07;
	break;

      case 0xFD:
	this.pio.writeDataB( value );
	break;

      case 0xFE:
	this.pio.writeControlA( value );
	break;

      case 0xFF:
	this.pio.writeControlB( value );
	break;
    }
  }


  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    super.z80MaxSpeedChanged( cpu );

    /*
     * Anzahl der Taktzyklen,
     * die eine Taste gedrueckt und danach wieder losgelassen werden soll,
     * bevor das Druecken der eigentlichen Taste emuliert wird.
     */
    this.kbTStates = cpu.getMaxSpeedKHz() * 100;
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    if( this.kbStatus > 0 ) {
      synchronized( this.kbMatrix ) {
	if( this.kbStatus > 0 ) {
	  this.kbTStateCounter += tStates;
	  if( this.kbTStateCounter > this.kbTStates ) {
	    this.kbTStateCounter = 0;
	    this.shiftPressed    = false;
	    this.ctrlPressed     = false;
	    --this.kbStatus;
	    if( this.kbStatus == 0 ) {
	      Arrays.fill( this.kbMatrix, 0 );
	    }
	  }
	}
      }
    }
  }


	/* --- private Methoden --- */

  private synchronized void checkAddPCListener( Properties props )
  {
    boolean state = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_CATCH_PRINT_CALLS,
				false );
    if( state != this.pcListenerAdded ) {
      Z80CPU cpu = this.emuThread.getZ80CPU();
      if( state ) {
	cpu.addPCListener( this, 0x00EC );
      } else {
	cpu.removePCListener( this );
      }
      this.pcListenerAdded = state;
    }
  }


  private void loadFont( Properties props )
  {
    this.fontBytes = readFontByProperty(
				props,
				this.propPrefix + PROP_FONT_FILE,
				0x0800 );
    if( this.fontBytes == null ) {
      if( romFont == null ) {
	romFont = readResource( "/rom/kramermc/kramermcfont.bin" );
      }
      this.fontBytes = romFont;
    }
  }


  private void loadROMs( Properties props )
  {
    this.rom0000File  = EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_ROM0_PREFIX + PROP_FILE );
    this.rom0000Bytes = readROMFile(
				this.rom0000File,
				0x0C00,
				"ROM (0000-0BFF)" );
    if( this.rom0000Bytes == null ) {
      if( rom0000 == null ) {
	rom0000 = readResource( "/rom/kramermc/rom_0000.bin" );
      }
      this.rom0000Bytes = rom0000;
    }

    this.rom8000File  = EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_ROM8_PREFIX + PROP_FILE );
    this.rom8000Bytes = readROMFile(
				this.rom8000File,
				0x3000,
				"ROM (8000-AFFF)" );
    if( this.rom8000Bytes == null ) {
      if( rom8000 == null ) {
	rom8000 = readResource( "/rom/kramermc/rom_8000.bin" );
      }
      this.rom8000Bytes = rom8000;
    }

    this.romC000File  = EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_ROMC_PREFIX + PROP_FILE );
    this.romC000Bytes = readROMFile(
				this.romC000File,
				0x2000,
				"ROM (C000-DFFF)" );
    if( this.romC000Bytes == null ) {
      if( romC000 == null ) {
	romC000 = readResource( "/rom/kramermc/rom_c000.bin" );
      }
      this.romC000Bytes = romC000;
    }

    loadFont( props );
  }


  private boolean setCharInKBMatrix( int ch, int[][] matrixChars )
  {
    boolean rv   = false;
    int     mask = 1;
    for( int row = 0; row < matrixChars.length; row++ ) {
      for( int col = 0;
	   (col < matrixChars[ row ].length) && (col < this.kbMatrix.length);
	   col++ )
      {
	if( matrixChars[ row ][ col ] == ch ) {
	  this.kbMatrix[ col ] = mask;
	  rv = true;
	  break;
	}
      }
      mask <<= 1;
    }
    return rv;
  }


  private void updKBColValue()
  {
    int colValue = 0xFF;
    synchronized( this.kbMatrix ) {
      if( this.kbStatus == 1 ) {
	int rowMask = 1;
	for( int i = 0; i < this.kbRow; i++ ) {
	  rowMask <<= 1;
	}
	int colMask = 0xFE;
	for( int k = 0; k < this.kbMatrix.length; k++ ) {
	  if( (this.kbMatrix[ k ] & rowMask) != 0 ) {
	    colValue &= colMask;
	  }
	  colMask = (colMask << 1) | 0x01;
	}
      }
      else if( this.kbStatus == 3 ) {
	if( (this.kbRow == 6) && this.ctrlPressed ) {
	  colValue &= 0xF7;
	}
	if( (this.kbRow == 0) && this.shiftPressed ) {
	  colValue &= 0xDF;
	}
      }
    }
    this.pio.putInValuePortB( colValue, 0xFF );
  }
}
