/*
 * (c) 2009-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des KramerMC
 */

package jkcemu.emusys;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.base.*;
import z80emu.*;


public class KramerMC extends EmuSys implements
					Z80MaxSpeedListener,
					Z80PCListener,
					Z80TStatesListener
{
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
  private static byte[] rom0400 = null;
  private static byte[] rom8000 = null;
  private static byte[] romC000 = null;
  private static byte[] romFont = null;

  private byte[]       fontBytes;
  private byte[]       ramVideo;
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
    super( emuThread, props );
    this.fontBytes = readFontByProperty(
				props,
				"jkcemu.kramermmc.font.file",
				0x0800 );
    if( this.fontBytes == null ) {
      if( romFont == null ) {
	romFont = readResource( "/rom/kramermc/kramermcfont.bin" );
      }
      this.fontBytes = romFont;
    }
    if( rom0000 == null ) {
      rom0000 = readResource( "/rom/kramermc/rom_0000.bin" );
    }
    if( rom0400 == null ) {
      rom0400 = readResource( "/rom/kramermc/rom_0400.bin" );
    }
    if( rom8000 == null ) {
      rom8000 = readResource( "/rom/kramermc/rom_8000.bin" );
    }
    if( romC000 == null ) {
      romC000 = readResource( "/rom/kramermc/rom_c000.bin" );
    }
    this.ramVideo = new byte[ 0x0400 ];
    this.kbMatrix = new int[ 8 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio   = new Z80PIO( "PIO (IO-Adressen FC-FF)" );
    cpu.setInterruptSources( this.pio );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );

    this.pcListenerAdded = false;
    checkAddPCListener( props );
    z80MaxSpeedChanged( cpu );
  }


  public static String getBasicProgram( Z80MemView memory )
  {
    return SourceUtil.getKCBasicStyleProgram( memory, 0x1001, basicTokens );
  }


  public static int getDefaultSpeedKHz()
  {
    return 1500;
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    /*
     * Anzahl der Taktzyklen,
     * die eine Taste gedrueckt und danach wieder losgelassen werden soll,
     * bevor das Druecken der eigentlichen Taste emuliert wird.
     */
    this.kbTStates = cpu.getMaxSpeedKHz() * 100;
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


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
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


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    checkAddPCListener( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    return EmuUtil.getProperty( props, "jkcemu.system" ).equals( "KramerMC" );
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
  public int getCharColCount()
  {
    return 64;
  }


  @Override
  public int getCharHeight()
  {
    return 8;
  }


  @Override
  public int getCharRowCount()
  {
    return 16;
  }


  @Override
  public int getCharRowHeight()
  {
    return 8;
  }


  @Override
  public int getCharWidth()
  {
    return 6;
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
    if( rom0000 != null ) {
      if( addr < rom0000.length ) {
	rv   = (int) rom0000[ addr ] & 0xFF;
	done = true;
      }
    }
    if( !done && (addr >= 0x0400) && (rom0400 != null) ) {
      int idx = addr - 0x0400;
      if( idx < rom0400.length ) {
	rv   = (int) rom0400[ idx ] & 0xFF;
	done = true;
      }
    }
    if( !done && (addr >= 0x8000) && (rom8000 != null) ) {
      int idx = addr - 0x8000;
      if( idx < rom8000.length ) {
	rv   = (int) rom8000[ idx ] & 0xFF;
	done = true;
      }
    }
    if( !done && (addr >= 0xC000) && (romC000 != null) ) {
      int idx = addr - 0xC000;
      if( idx < romC000.length ) {
	rv   = (int) romC000[ idx ] & 0xFF;
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
  protected int getScreenChar( int chX, int chY )
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
    return "Kramer-MC";
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return true;
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
    String text = SourceUtil.getKCBasicStyleProgram(
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
  protected boolean pasteChar( char ch )
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
	try {
	  Thread.sleep( 50 );
	}
	catch( InterruptedException ex ) {}
      }
    }
    return rv;
  }


  @Override
  public int readIOByte( int port )
  {
    int value = 0xFF;
    switch( port & 0xFF ) {
      case 0xFC:
	value = this.pio.readPortA();
	break;

      case 0xFD:
	updKBColValue();
	value = this.pio.readPortB();
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
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
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
    int endAddr = SourceUtil.getKCBasicStyleEndAddr( this.emuThread, 0x1001 );
    if( endAddr >= 0x1001 ) {
      (new SaveDlg(
		this.screenFrm,
		0x1001,
		endAddr,
		'B',
		false,          // kein KC-BASIC
		false,		// kein RBASIC
		"BASIC-Programm speichern" )).setVisible( true );
    } else {
      showNoBasic();
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( rom0000 != null ) {
      if( addr < rom0000.length ) {
	done = true;
      }
    }
    if( !done && (addr >= 0x0400) && (rom0400 != null) ) {
      if( (addr - 0x0400) < rom0400.length ) {
	done = true;
      }
    }
    if( !done && (addr >= 0x8000) && (rom8000 != null) ) {
      if( (addr - 0x8000) < rom8000.length ) {
	done = true;
      }
    }
    if( !done && (addr >= 0xC000) && (romC000 != null) ) {
      if( (addr - 0xC000) < romC000.length ) {
	done = true;
      }
    }
    if( !done && (addr >= 0xFC00) ) {
      int idx = addr - 0xFC00;
      if( idx < this.ramVideo.length ) {
	this.ramVideo[ idx ] = (byte) value;
	this.screenFrm.setScreenDirty( true );
	done = true;
	rv   = true;
      }
    }
    if( !done ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
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
			int    begAddr,
			int    len,
			Object fileFmt,
			int    fileType )
  {
    if( (begAddr == 0x1001) && (fileFmt != null) ) {
      if( fileFmt.equals( FileInfo.HEADERSAVE ) ) {
	if( fileType == 'B' ) {
	  int topAddr = begAddr + len;
	  this.emuThread.setMemWord( 0x0C5E, topAddr );
	  this.emuThread.setMemWord( 0x0C60, topAddr );
	  this.emuThread.setMemWord( 0x0C62, topAddr );
	}
      }
    }
  }


  @Override
  public void writeIOByte( int port, int value )
  {
    switch( port & 0xFF ) {
      case 0xFC:
	this.pio.writePortA( value );
	this.kbRow = (this.pio.fetchOutValuePortA( false ) >> 1) & 0x07;
	break;

      case 0xFD:
	this.pio.writePortB( value );
	break;

      case 0xFE:
	this.pio.writeControlA( value );
	break;

      case 0xFF:
	this.pio.writeControlB( value );
	break;
    }
  }


	/* --- private Methoden --- */

  private synchronized void checkAddPCListener( Properties props )
  {
    boolean state = EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kramermc.catch_print_calls",
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

