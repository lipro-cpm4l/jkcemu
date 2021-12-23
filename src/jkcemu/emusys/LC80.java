/*
 * (c) 2009-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des LC80
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;
import jkcemu.base.AutoInputCharSet;
import jkcemu.base.EmuMemView;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.SourceUtil;
import jkcemu.emusys.lc80.LC80KeyboardFld;
import jkcemu.emusys.lc80.TVTerminal;
import jkcemu.file.FileFormat;
import jkcemu.file.FileUtil;
import jkcemu.file.SaveDlg;
import jkcemu.text.TextUtil;
import jkcemu.usb.VDIP;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80HaltStateListener;
import z80emu.Z80InterruptSource;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80PCListener;
import z80emu.Z80PIO;
import z80emu.Z80SIO;
import z80emu.Z80SIOChannelListener;
import z80emu.Z80PIOPortListener;


public class LC80 extends EmuSys implements
					Z80HaltStateListener,
					Z80MaxSpeedListener,
					Z80PCListener,
					Z80PIOPortListener,
					Z80SIOChannelListener
{
  public static final String SYSNAME              = "LC80";
  public static final String SYSNAME_LC80_U505    = "LC80_U505";
  public static final String SYSNAME_LC80_2716    = "LC80_2716";
  public static final String SYSNAME_LC80_2       = "LC80_2";
  public static final String SYSNAME_LC80_E       = "LC80_E";
  public static final String SYSNAME_LC80_EX      = "LC80_EX";
  public static final String SYSNAME_LC80         = "LC80";
  public static final String SYSTEXT              = "LC-80";
  public static final String SYSTEXT_LC80_2       = "LC-80.2";
  public static final String SYSTEXT_LC80_E       = "LC-80e";
  public static final String SYSTEXT_LC80_EX      = "LC-80ex";
  public static final String PROP_PREFIX          = "jkcemu.lc80.";
  public static final String PROP_ROM_A000_PREFIX = "rom_a000.";
  public static final String PROP_ROM_C000_PREFIX = "rom_c000.";

  public static final int     FUNCTION_KEY_COUNT                    = 4;
  public static final int     DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX = 15000;
  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE            = true;

  private static final String[] basicTokens = {
	"END",      "FOR",    "NEXT",     "DATA",		// 0x80
	"INPUT",    "DIM",    "READ",     "LET",
	"GOTO",     "RUN",    "IF",       "RESTORE",
	"GOSUB",    "RETURN", "REM",      "STOP",
	"OUT",      "ON",     "NULL",     "WAIT",		// 0x90
	"DEF",      "POKE",   "DOKE",     "AUTO",
	"LINES",    "CLS",    "WIDTH",    "BYE",
	null,       "CALL",   "PRINT",    "CONT",
	"LIST",     "CLEAR",  "LOAD",     "SAVE",		// 0xA0
	"NEW",      "TAB(",   "TO",       "FN",
	"SPC(",     "THEN",   "NOT",      "STEP",
	"+",        "-",      "*",        "/",
	"^",        "AND",    "OR",       ">",			// 0xB0
	"=",        "<",      "SGN",      "INT",
	"ABS",      "USR",    "FRE",      "INP",
	"POS",      "SQR",    "RND",      "LN",
	"EXP",      "COS",    "SIN",      "TAN",		// 0xC0
	"ATN",      "PEEK",   "DEEK",     null,
	"LEN",      "STR$",   "VAL",      "ASC",
	"CHR$",     "LEFT$",  "RIGHT$",   "MID$",
	null,       null,     "RENUMBER", "LOCATE",		// 0xD0
	"SOUND",    "INKEY",  "MODE",     "TRON",
	"TROFF",    "FILES",  "LFILES",   "LLIST",
	"LPRINT",   "TIME$",  "DATE$",    "EDIT" };

  private static AutoInputCharSet autoInputCharSet = null;

  private static byte[] lc80_u505   = null;
  private static byte[] lc80_2716   = null;
  private static byte[] lc80_2      = null;
  private static byte[] lc80e_0000  = null;
  private static byte[] lc80e_c000  = null;
  private static byte[] lc80ex_a000 = null;
  private static byte[] lc80ex_c000 = null;

  private String           romOSFile;
  private String           romA000File;
  private String           romC000File;
  private byte[]           romOS;
  private byte[]           romA000;
  private byte[]           romC000;
  private byte[]           ram;
  private int[]            kbMatrix;
  private int[]            digitStatus;
  private int[]            digitValues;
  private int              curDigitValue;
  private int              sioTStatesCounter;
  private volatile int     pioSysBValue;
  private long             curDisplayTStates;
  private long             displayCheckTStates;
  private boolean          tapeOutLED;
  private volatile boolean tapeOutState;
  private boolean          chessComputer;
  private boolean          chessMode;
  private boolean          haltState;
  private TVTerminal       tvTerm;
  private VDIP             vdip;
  private LC80KeyboardFld  keyboardFld;
  private Z80CTC           ctc;
  private Z80PIO           pioSys;
  private Z80PIO           pioUser;
  private Z80SIO           sio;
  private String           sysName;


  public LC80( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.romOS       = null;
    this.romOSFile   = null;
    this.romA000     = null;
    this.romA000File = null;
    this.romA000     = null;
    this.romC000File = null;
    this.sysName     = EmuUtil.getProperty( props, EmuThread.PROP_SYSNAME );
    if( this.sysName.equals( SYSNAME_LC80_U505 ) ) {
      this.ram = new byte[ 0x0400 ];
    } else if( this.sysName.equals( SYSNAME_LC80_EX ) ) {
      this.ram = new byte[ 0x7000 ];
    } else {
      this.ram = new byte[ 0x1000 ];
    }
    this.chessComputer       = this.sysName.equals( SYSNAME_LC80_E );
    this.tapeOutLED          = false;
    this.tapeOutState        = false;
    this.chessMode           = false;
    this.haltState           = false;
    this.curDisplayTStates   = 0;
    this.displayCheckTStates = 0;
    this.pioSysBValue        = 0xFF;
    this.curDigitValue       = 0xFF;
    this.digitValues         = new int[ 6 ];
    this.digitStatus         = new int[ 6 ];
    this.kbMatrix            = new int[ 6 ];
    this.keyboardFld         = null;

    Z80CPU cpu   = emuThread.getZ80CPU();
    this.pioSys  = new Z80PIO( "System-PIO" );
    this.pioUser = new Z80PIO( "User-PIO" );
    this.ctc     = new Z80CTC( "CTC" );
    this.sio     = null;
    this.vdip    = null;
    this.tvTerm  = null;
    if( this.sysName.equals( SYSNAME_LC80_EX ) ) {
      this.tvTerm = new TVTerminal( this, props );
      this.sio    = new Z80SIO( "SIO" );
      cpu.setInterruptSources(
		this.ctc, this.pioUser, this.pioSys, this.sio );
      // VDIP nicht in Interrupt-Logik enthalten!
      this.vdip = new VDIP( 0, this.emuThread.getZ80CPU(), "USB-PIO" );
    } else {
      cpu.setInterruptSources( this.ctc, this.pioUser, this.pioSys );
    }
    cpu.addMaxSpeedListener( this );
    cpu.addHaltStateListener( this );
    cpu.addTStatesListener( this );
    this.pioSys.addPIOPortListener( this, Z80PIO.PortInfo.A );
    this.pioSys.addPIOPortListener( this, Z80PIO.PortInfo.B );
    if( this.sio != null ) {
      this.sio.addChannelListener( this, 0 );
      this.sio.addChannelListener( this, 1 );
    }
    if( this.chessComputer ) {
      cpu.addPCListener( this, 0x0000, 0xC800 );
    }
    z80MaxSpeedChanged( cpu );
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
  }


  public static AutoInputCharSet getAutoInputCharSet()
  {
    if( autoInputCharSet == null ) {
      autoInputCharSet = new AutoInputCharSet();
      autoInputCharSet.addHexChars();
      autoInputCharSet.addKeyChar( 0x1B, "RESET" );
      autoInputCharSet.addKeyChar( 'N', "NMI" );
      autoInputCharSet.addKeyChar( 'S', "ST" );
      autoInputCharSet.addKeyChar( 'L', "LD" );
      autoInputCharSet.addKeyChar( 'X', "EX" );
      autoInputCharSet.addKeyChar( 0xF1, "ADR" );
      autoInputCharSet.addKeyChar( 0xF2, "DAT" );
      autoInputCharSet.addKeyChar( '+', "A+" );
      autoInputCharSet.addKeyChar( '-', "A-" );
    }
    return autoInputCharSet;
  }


  public static String getBasicProgram( EmuMemView memory )
  {
    return SourceUtil.getBasicProgram( memory, 0x2400, basicTokens );
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    int    rv      = 900;
    String sysName = EmuUtil.getProperty( props, EmuThread.PROP_SYSNAME );
    if( sysName.equals( SYSNAME_LC80_E ) ) {
      rv = 1800;
    } else if( sysName.equals( SYSNAME_LC80_EX ) ) {
      rv = 1843;
    }
    return rv;
  }


  public boolean isChessMode()
  {
    return this.chessMode;
  }


  public void putToSIOChannelB( int value )
  {
    if( this.sio != null )
      this.sio.putToReceiverB( value );
  }


  public void updKeyboardMatrix( int[] kbMatrix )
  {
    synchronized( this.kbMatrix ) {
      int n = Math.min( kbMatrix.length, this.kbMatrix.length );
      int i = 0;
      while( i < n ) {
        this.kbMatrix[ i ] = kbMatrix[ i ];
        i++;
      }
      while( i < this.kbMatrix.length ) {
        this.kbMatrix[ i ] = 0;
        i++;
      }
      putKBMatrixRowValueToPort();
    }
  }


	/* --- Z80HaltStateListener --- */

  @Override
  public void z80HaltStateChanged( Z80CPU cpu, boolean haltState )
  {
    if( haltState != this.haltState ) {
      this.haltState = haltState;
      this.screenFrm.setScreenDirty( true );
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.displayCheckTStates = cpu.getMaxSpeedKHz() * 50;
  }


	/* --- Z80PCListener --- */

  @Override
  public void z80PCChanged( Z80CPU cpu, int pc )
  {
    switch( pc ) {
      case 0x0000:
	setChessMode( false );
	break;
      case 0xC800:
	setChessMode( true );
	break;
    }
  }


	/* --- Z80PIOPortListener --- */

  @Override
  public void z80PIOPortStatusChanged(
				Z80PIO          pio,
				Z80PIO.PortInfo port,
				Z80PIO.Status   status )
  {
    if( (pio == this.pioSys)
	&& ((status == Z80PIO.Status.OUTPUT_AVAILABLE)
	    || (status == Z80PIO.Status.OUTPUT_CHANGED)) )
    {
      if( port == Z80PIO.PortInfo.A ) {
	this.curDigitValue = toDigitValue(
			this.pioSys.fetchOutValuePortA( 0x00 ) );
	putKBMatrixRowValueToPort();
	updDisplay();
      }
      else if( port == Z80PIO.PortInfo.B ) {
	this.pioSysBValue    = this.pioSys.fetchOutValuePortB( 0x03 );
	boolean tapeOutPhase = ((this.pioSysBValue & 0x02) != 0);
	if( tapeOutPhase != this.tapeOutPhase ) {
	  this.tapeOutPhase = tapeOutPhase;
	  this.tapeOutState = true;
	  this.screenFrm.setScreenDirty( true );
	}
	putKBMatrixRowValueToPort();
	updDisplay();
      }
    }
  }


	/* --- Z80SIOChannelListener --- */

  @Override
  public void z80SIOByteSent( Z80SIO sio, int channel, int value )
  {
    if( (sio == this.sio) && (this.sio != null) ) {
      if( channel == 0 ) {
	this.emuThread.getPrintMngr().putByte( value );
	this.sio.setClearToSendA( false );
	this.sio.setClearToSendA( true );
      } else if( channel == 1 ) {
	if( this.tvTerm != null ) {
	  this.tvTerm.write( value );
	  this.sio.setClearToSendB( false );
	  this.sio.setClearToSendB( true );
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    if( this.tvTerm != null ) {
      this.tvTerm.applySettings( props );
    }
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( this.sysName );
    if( rv ) {
      rv = TextUtil.equals(
		this.romOSFile,
		EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_OS_FILE ) );
    }
    if( rv && this.sysName.equals( SYSNAME_LC80_EX ) ) {
      rv = TextUtil.equals(
	this.romA000File,
	EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_ROM_A000_PREFIX + PROP_FILE ) );
    }
    if( rv && (this.sysName.equals( SYSNAME_LC80_E )
	       || this.sysName.equals( SYSNAME_LC80_EX )) )
    {
      rv = TextUtil.equals(
	this.romC000File,
	EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_ROM_C000_PREFIX + PROP_FILE ) );
    }
    return rv;
  }


  @Override
  public LC80KeyboardFld createKeyboardFld()
  {
    this.keyboardFld = new LC80KeyboardFld( this );
    return this.keyboardFld;
  }


  @Override
  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeHaltStateListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.chessComputer ) {
      cpu.removePCListener( this );
    }
    this.pioSys.removePIOPortListener( this, Z80PIO.PortInfo.A );
    this.pioSys.removePIOPortListener( this, Z80PIO.PortInfo.B );
    if( this.sio != null ) {
      this.sio.removeChannelListener( this, 0 );
      this.sio.removeChannelListener( this, 1 );
    }
    if( this.vdip != null ) {
      this.vdip.die();
    }
    super.die();
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x23EA;
  }


  @Override
  public boolean getAutoLoadInputOnSoftReset()
  {
    return false;
  }


  @Override
  public Chessman getChessman( int row, int col )
  {
    Chessman rv = null;
    if( this.chessMode
	&& (row >= 0) && (row < 8) && (col >= 0) && (col < 8) )
    {
      switch( getMemByte( 0x2715 + (row * 10) + col, false ) & 0x8F ) {
	case 1:
	  rv = Chessman.WHITE_PAWN;
	  break;

	case 2:
	  rv = Chessman.WHITE_KNIGHT;
	  break;

	case 3:
	  rv = Chessman.WHITE_BISHOP;
	  break;

	case 4:
	  rv = Chessman.WHITE_ROOK;
	  break;

	case 5:
	  rv = Chessman.WHITE_QUEEN;
	  break;

	case 6:
	  rv = Chessman.WHITE_KING;
	  break;

	case 0x81:
	  rv = Chessman.BLACK_PAWN;
	  break;

	case 0x82:
	  rv = Chessman.BLACK_KNIGHT;
	  break;

	case 0x83:
	  rv = Chessman.BLACK_BISHOP;
	  break;

	case 0x84:
	  rv = Chessman.BLACK_ROOK;
	  break;

	case 0x85:
	  rv = Chessman.BLACK_QUEEN;
	  break;

	case 0x86:
	  rv = Chessman.BLACK_KING;
	  break;
      }
    }
    return rv;
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = Color.BLACK;
    switch( colorIdx ) {
      case 1:
	color = this.colorGreenDark;
	break;

      case 2:
	color = this.colorGreenLight;
	break;

      case 3:
	color = this.colorRedDark;
	break;

      case 4:
	color = this.colorRedLight;
	break;
    }
    return color;
  }


  @Override
  public int getColorCount()
  {
    return 5;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/lc80.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    if( (this.romA000 != null) || (this.romC000 != null) ) {
      addr &= 0xFFFF;
    } else {
      addr &= 0x3FFF;		// unvollstaendige Adressdekodierung
    }

    int rv = 0xFF;
    if( addr < 0x2000 ) {
      if( this.romOS != null ) {
	if( addr < this.romOS.length ) {
	  rv = (int) this.romOS[ addr ] & 0xFF;
	}
      }
    }
    else if( (addr >= 0x2000) && (addr < 0xA000) ) {
      int idx = addr - 0x2000;
      if( idx < this.ram.length ) {
	if( (idx >= 0x5FF9) && (idx < 0x6000) ) {
	  if( ((int) this.ram[ 0x5FF8 ] & 0x40) == 0 ) {
	    updTimekeeperRAM();
	  }
	}
	rv = (int) this.ram[ idx ] & 0xFF;
      }
    }
    else if( (addr >= 0xA000) && (addr < 0xC000) ) {
      if( this.romA000 != null ) {
	int idx = addr - 0xA000;
	if( idx < this.romA000.length ) {
	  rv = (int) this.romA000[ idx ] & 0xFF;
	}
      }
    }
    else if( addr >= 0xC000 ) {
      if( this.romC000 != null ) {
	int idx = addr - 0xC000;
	if( idx < this.romC000.length ) {
	  rv = (int) this.romC000[ idx ] & 0xFF;
	}
      }
    }
    return rv;
  }


  @Override
  public int getScreenHeight()
  {
    return 85;
  }


  @Override
  public int getScreenWidth()
  {
    return 39 + (65 * this.digitValues.length);
  }


  @Override
  public TVTerminal getSecondScreenDevice()
  {
    return this.tvTerm;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return DEFAULT_SWAP_KEY_CHAR_CASE;
  }


  @Override
  public String getTitle()
  {
    String rv = SYSTEXT;
    switch( this.sysName ) {
      case SYSNAME_LC80_2:
	rv = SYSTEXT_LC80_2;
	break;
      case SYSNAME_LC80_E:
	rv = SYSTEXT_LC80_E;
	break;
      case SYSNAME_LC80_EX:
	rv = SYSTEXT_LC80_EX;
	break;
    }
    return rv;
  }


  @Override
  public VDIP[] getVDIPs()
  {
    return this.vdip != null ?
			new VDIP[] { this.vdip }
			: super.getVDIPs();
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.kbMatrix ) {
      switch( keyCode ) {
	case KeyEvent.VK_F1:
	  this.kbMatrix[ 5 ] = 0x80;		// ADR / NEW GAME
	  rv = true;
	  break;

	case KeyEvent.VK_F2:
	  this.kbMatrix[ 5 ] = 0x40;		// DAT / SELF PLAY / SW
	  rv = true;
	  break;

	case KeyEvent.VK_F3:
	  this.kbMatrix[ 0 ] = 0x20;		// LD / RAN
	  rv = true;
	  break;

	case KeyEvent.VK_F4:
	  this.kbMatrix[ 0 ] = 0x40;		// ST / Contr.
	  rv = true;
	  break;

	case KeyEvent.VK_ENTER:
	  this.kbMatrix[ 0 ] = 0x80;		// EX
	  rv = true;
	  break;
      }
    }
    if( rv ) {
      putKBMatrixRowValueToPort();
      updKeyboardFld();
    } else {
      if( keyCode == KeyEvent.VK_ESCAPE ) {
	this.emuThread.fireReset( false );
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    synchronized( this.kbMatrix ) {
      Arrays.fill( this.kbMatrix, 0 );
    }
    putKBMatrixRowValueToPort();
    updKeyboardFld();
  }


  @Override
  public boolean keyTyped( char keyChar )
  {
    boolean rv = false;
    synchronized( this.kbMatrix ) {
      switch( keyChar ) {
	case 0xF1:
	  this.kbMatrix[ 5 ] = 0x80;		// ADR / NEW GAME
	  rv = true;
	  break;

	case '\u00F2':
	  this.kbMatrix[ 5 ] = 0x40;		// DAT / SELF PLAY / SW
	  rv = true;
	  break;

	case '\u00F3':
	  this.kbMatrix[ 0 ] = 0x20;		// LD / RAN
	  rv = true;
	  break;

	case '\u00F4':
	  this.kbMatrix[ 0 ] = 0x40;		// ST / Contr.
	  rv = true;
	  break;
      }
      if( !rv ) {
	switch( Character.toUpperCase( keyChar ) ) {
	  case '\u001B':
	    this.emuThread.fireReset( false );
	    rv = true;
	    break;

	  case '0':
	    this.kbMatrix[ 1 ] = 0x80;
	    rv = true;
	    break;

	  case '1':
	    this.kbMatrix[ 1 ] = 0x40;
	    rv = true;
	    break;

	  case '2':
	    this.kbMatrix[ 1 ] = 0x20;
	    rv = true;
	    break;

	  case '3':
	    this.kbMatrix[ 1 ] = 0x10;
	    rv = true;
	    break;

	  case '4':
	    this.kbMatrix[ 2 ] = 0x80;
	    rv = true;
	    break;

	  case '5':
	    this.kbMatrix[ 2 ] = 0x40;
	    rv = true;
	    break;

	  case '6':
	    this.kbMatrix[ 5 ] = 0x20;
	    rv = true;
	    break;

	  case '7':
	    this.kbMatrix[ 2 ] = 0x10;
	    rv = true;
	    break;

	  case '8':
	    this.kbMatrix[ 3 ] = 0x80;
	    rv = true;
	    break;

	  case '9':
	    this.kbMatrix[ 3 ] = 0x40;
	    rv = true;
	    break;

	  case '-':
	    this.kbMatrix[ 5 ] = 0x10;
	    rv = true;
	    break;

	  case '+':
	    this.kbMatrix[ 2 ] = 0x20;
	    rv = true;
	    break;

	  case 'A':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 1 ] = 0x40;		// auch 1
	    } else {
	      this.kbMatrix[ 4 ] = 0x20;
	    }
	    rv = true;
	    break;

	  case 'B':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 1 ] = 0x20;		// auch 2
	    } else {
	      this.kbMatrix[ 3 ] = 0x10;
	    }
	    rv = true;
	    break;

	  case 'C':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 1 ] = 0x10;		// auch 3
	    } else {
	      this.kbMatrix[ 4 ] = 0x80;
	    }
	    rv = true;
	    break;

	  case 'D':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 2 ] = 0x80;		// auch 4
	    } else {
	      this.kbMatrix[ 4 ] = 0x40;
	    }
	    rv = true;
	    break;

	  case 'E':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 2 ] = 0x40;		// auch 5
	    } else {
	      this.kbMatrix[ 3 ] = 0x20;
	    }
	    rv = true;
	    break;

	  case 'F':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 5 ] = 0x20;		// auch 6
	    } else {
	      this.kbMatrix[ 4 ] = 0x10;
	    }
	    rv = true;
	    break;

	  case 'G':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 2 ] = 0x10;		// auch 7
	      rv = true;
	    }
	    break;

	  case 'H':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 3 ] = 0x80;		// auch 8
	    } else {
	      this.kbMatrix[ 5 ] = 0x40;		// DAT
	    }
	    rv = true;
	    break;

	  case 'K':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 4 ] = 0x10;		// Koenig
	      rv = true;
	    }
	    break;

	  case 'L':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 4 ] = 0x80;		// Laeufer
	    } else {
	      this.kbMatrix[ 0 ] = 0x20;		// LD
	    }
	    rv = true;
	    break;

	  case 'M':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 3 ] = 0x20;		// Dame
	    } else {
	      this.kbMatrix[ 5 ] = 0x80;		// ADR
	    }
	    rv = true;
	    break;

	  case 'N':
	    this.emuThread.getZ80CPU().fireNMI();
	    rv = true;
	    break;

	  case 'O':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 2 ] = 0x20;		// BOARD
	      rv = true;
	    }
	    break;

	  case 'P':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 5 ] = 0x40;		// SELF PLAY / SW
	      rv = true;
	    }
	    break;

	  case 'R':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 0 ] = 0x20;		// RAN
	      rv = true;
	    }
	    break;

	  case 'S':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 3 ] = 0x10;		// Springer
	    } else {
	      this.kbMatrix[ 0 ] = 0x40;		// ST
	    }
	    rv = true;
	    break;

	  case 'T':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 4 ] = 0x40;		// Turm
	      rv = true;
	    }
	    break;

	  case 'U':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 4 ] = 0x20;		// Bauer
	      rv = true;
	    }
	    break;

	  case 'V':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 5 ] = 0x80;		// NEW GAME
	      rv = true;
	    }
	    break;

	  case 'W':
	    if( this.chessComputer && this.chessMode ) {
	      this.kbMatrix[ 5 ] = 0x10;		// COLOR (WHITE)
	      rv = true;
	    }
	    break;

	  case 'X':
	    this.kbMatrix[ 0 ] = 0x80;		// EX
	    rv = true;
	    break;
	}
      }
    }
    if( rv ) {
      putKBMatrixRowValueToPort();
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  public void loadROMs( Properties props )
  {
    this.romOSFile = EmuUtil.getProperty(
				props,
				this.propPrefix + PROP_OS_FILE );
    this.romOS = readROMFile( this.romOSFile, 0x2000, "Monitorprogramm" );
    if( this.romOS == null ) {
      if( this.sysName.equals( SYSNAME_LC80_U505 ) ) {
	if( lc80_u505 == null ) {
	  lc80_u505 = readResource( "/rom/lc80/lc80_u505.bin" );
	}
	this.romOS = lc80_u505;
      } else if( this.sysName.equals( SYSNAME_LC80_2 ) ) {
	if( lc80_2 == null ) {
	  lc80_2 = readResource( "/rom/lc80/lc80_2.bin" );
	}
	this.romOS = lc80_2;
      } else if( this.sysName.equals( SYSNAME_LC80_E )
		 || this.sysName.equals( SYSNAME_LC80_EX ) )
      {
	if( lc80e_0000 == null ) {
	  lc80e_0000 = readResource( "/rom/lc80/lc80e_0000.bin" );
	}
	this.romOS = lc80e_0000;
      } else {
	if( lc80_2716 == null ) {
	  lc80_2716 = readResource( "/rom/lc80/lc80_2716.bin" );
	}
	this.romOS = lc80_2716;
      }
    }
    if( this.sysName.equals( SYSNAME_LC80_EX ) ) {
      this.romA000File = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROM_A000_PREFIX + PROP_FILE );
      this.romA000 = readROMFile(
			this.romA000File,
			0x2000,
			"ROM A000h / LLCTOOLS" );
      if( this.romA000 == null ) {
	if( lc80ex_a000 == null ) {
	  lc80ex_a000 = readResource( "/rom/lc80/lc80ex_a000.bin" );
	}
	this.romA000 = lc80ex_a000;
      }
    } else {
      this.romA000 = null;
    }
    if( this.sysName.equals( SYSNAME_LC80_E )
	|| this.sysName.equals( SYSNAME_LC80_EX ) )
    {
      this.romC000File = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROM_C000_PREFIX + PROP_FILE );
      this.romC000 = readROMFile(
			this.romC000File,
			0x4000,
			"ROM C0000" );
      if( this.romC000 == null ) {
	if( this.sysName.equals( SYSNAME_LC80_E ) ) {
	  if( lc80e_c000 == null ) {
	    lc80e_c000 = readResource( "/rom/lc80/lc80e_c000.bin" );
	  }
	  this.romC000 = lc80e_c000;
	} else if( this.sysName.equals( SYSNAME_LC80_EX ) ) {
	  if( lc80ex_c000 == null ) {
	    lc80ex_c000 = readResource( "/rom/lc80/lc80ex_c000.bin" );
	  }
	  this.romC000 = lc80ex_c000;
	}
      }
    } else {
      this.romC000 = null;
    }
  }


  @Override
  public void openBasicProgram()
  {
    String text = SourceUtil.getBasicProgram(
					this.emuThread,
					0x2400,
					basicTokens );
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoBasic();
    }
  }


  @Override
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    // LED fuer Tonausgabe, L-aktiv
    if( this.tapeOutLED
	|| (!this.tapeOutPhase && !this.tapeOutState) )
    {
      g.setColor( this.colorGreenLight );
    } else {
      g.setColor( this.colorGreenDark );
    }
    g.fillArc(
	x,
	y,
	20 * screenScale,
	20 * screenScale,
	0,
	360 );

    // LED fuer HALT-Zustand
    g.setColor( this.haltState ? this.colorRedLight : this.colorRedDark );
    g.fillArc(
	x,
	y + (30 * screenScale),
	20 * screenScale,
	20 * screenScale,
	0,
	360 );

    // 7-Segment-Anzeige
    x += (50 * screenScale);
    synchronized( this.digitValues ) {
      for( int i = 0; i < this.digitValues.length; i++ ) {
	paint7SegDigit(
		g,
		x,
		y,
		this.digitValues[ i ],
		this.colorGreenDark,
		this.colorGreenLight,
		screenScale );
	x += (65 * screenScale);
      }
    }
    return true;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    port &= 0xFF;
    if( (port & 0x04) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  rv &= this.pioUser.readDataA();
	  break;
	case 1:
	  rv &= this.pioUser.readDataB();
	  break;
      }
    }
    if( (port & 0x08) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  rv &= this.pioSys.readDataA();
	  break;
	case 1:
	  rv &= this.pioSys.readDataB();
	  break;
      }
    }
    if( (port & 0x10) == 0 ) {
      rv &= this.ctc.read( port & 0x03, tStates );
    }
    if( ((port & 0x20) == 0) && (this.sio != null) ) {
      switch( port & 0x03 ) {
	case 0:
	  rv &= this.sio.readDataA();
	  break;
	case 1:
	  rv &= this.sio.readDataB();
	  if( (this.sio.availableB() == 0) && (this.tvTerm != null) ) {
	    this.tvTerm.processNextQueuedKeyChar();
	  }
	  break;
	case 2:
	  rv &= this.sio.readControlA();
	  break;
	case 3:
	  rv &= this.sio.readControlB();
	  break;
      }
    }
    if( ((port & 0x40) == 0) && (this.vdip != null) ) {
      rv &= this.vdip.read( port & 0x03 );
    }
    return rv;
  }


  @Override
  public void reset( boolean powerOn, Properties props )
  {
    super.reset( powerOn, props );
    if( powerOn ) {
      initSRAM( this.ram, props );
      if( (this.sio != null) && (this.ram.length >= 0x6000) ) {
	this.ram[ 0x5FF8 ] = 0;		// Timekeeper RAM Control
      }
    }
    this.pioSys.reset( powerOn );
    this.pioUser.reset( powerOn );
    this.ctc.reset( powerOn );
    if( this.sio != null ) {
      this.sio.reset( powerOn );
      this.sio.setClearToSendA( true );
      this.sio.setClearToSendB( true );
    }
    if( this.vdip != null ) {
      this.vdip.reset( powerOn );
    }
    if( this.tvTerm != null ) {
      this.tvTerm.reset();
    }
    synchronized( this.kbMatrix ) {
      Arrays.fill( this.kbMatrix, 0 );
    }
    synchronized( this.digitValues ) {
      Arrays.fill( this.digitStatus, 0 );
      Arrays.fill( this.digitValues, 0 );
    }
    setChessMode( false );
    this.sioTStatesCounter = 0;
  }


  @Override
  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getBasicEndAddr( this.emuThread, 0x2400 );
    if( endAddr >= 0x2400 ) {
      (new SaveDlg(
		this.screenFrm,
		0x2400,
		endAddr,
		"LC-80ex-BASIC-Programm speichern",
		SaveDlg.BasicType.MS_DERIVED_BASIC_HS,
		FileUtil.getHeadersaveFileFilter() )).setVisible( true );
    } else {
      showNoBasic();
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    if( this.romC000 != null ) {
      addr &= 0xFFFF;
    } else {
      addr &= 0x3FFF;		// unvollstaendige Adressdekodierung
    }
    boolean rv = false;
    if( (addr >= 0x2000) && (addr < 0xC000) ) {
      int idx = addr - 0x2000;
      if( idx < this.ram.length ) {
	if( (idx < 0x5FF8) || (idx > 0x6000) ) {
	  this.ram[ idx ] = (byte) value;
	} else if( idx == 0x5FF8 ) {
	  if( (((int) this.ram[ 0x5FF8 ] & 0x40) == 0)
	      && ((value & 0x40) != 0) )
	  {
	    updTimekeeperRAM();
	  }
	  this.ram[ idx ] = (byte) value;
	}
	if( this.chessComputer && (addr >= 0x2715) && (addr < 0x2763) ) {
          this.screenFrm.setChessboardDirty( true );
        }
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public boolean supportsOpenBasic()
  {
    return true;
  }


  @Override
  public boolean supportsSaveBasic()
  {
    return (this.sio != null);
  }


  @Override
  public boolean supportsChessboard()
  {
    return this.chessComputer;
  }


  @Override
  public boolean supportsKeyboardFld()
  {
    return true;
  }


  @Override
  public boolean supportsPrinter()
  {
    return (this.sio != null);
  }


  @Override
  public boolean supportsTapeIn()
  {
    return true;
  }


  @Override
  public boolean supportsTapeOut()
  {
    return true;
  }


  @Override
  public void tapeInPhaseChanged()
  {
    this.pioSys.putInValuePortB( this.tapeInPhase ? 1 : 0, false );
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
		&& (begAddr == 0x2400)
		&& (len > 7))
	  || (fileFmt.equals( FileFormat.HEADERSAVE )
		&& (fileType == 'B')
		&& (begAddr <= 0x2400)
		&& ((begAddr + len) > 0x2407)) )
      {
	int topAddr = begAddr + len;
	this.emuThread.setMemWord( 0x20D2, topAddr );
	this.emuThread.setMemWord( 0x20D4, topAddr );
	this.emuThread.setMemWord( 0x20D6, topAddr );
      }
    }
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    port &= 0xFF;
    if( (port & 0x04) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  this.pioUser.writeDataA( value );
	  break;
	case 1:
	  this.pioUser.writeDataB( value );
	  break;
	case 2:
	  this.pioUser.writeControlA( value );
	  break;
	case 3:
	  this.pioUser.writeControlB( value );
	  break;
      }
    }
    if( (port & 0x08) == 0 ) {
      switch( port & 0x03 ) {
	case 0:
	  this.pioSys.writeDataA( value );
	  break;
	case 1:
	  this.pioSys.writeDataB( value );
	  break;
	case 2:
	  this.pioSys.writeControlA( value );
	  break;
	case 3:
	  this.pioSys.writeControlB( value );
	  break;
      }
    }
    if( (port & 0x10) == 0 ) {
      this.ctc.write( port & 0x03, value, tStates );
    }
    if( ((port & 0x20) == 0) && (this.sio != null) ) {
      switch( port & 0x03 ) {
	case 0:
	  this.sio.writeDataA( value );
	  break;
	case 1:
	  this.sio.writeDataB( value );
	  checkAndFireOpenSecondScreen();
	  break;
	case 2:
	  this.sio.writeControlA( value );
	  break;
	case 3:
	  this.sio.writeControlB( value );
	  break;
      }
    }
    if( ((port & 0x40) == 0) && (this.vdip != null) ) {
      this.vdip.write( port & 0x03, value );
    }
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    this.ctc.z80TStatesProcessed( cpu, tStates );
    if( this.displayCheckTStates > 0 ) {
      this.curDisplayTStates += tStates;
      if( this.curDisplayTStates > this.displayCheckTStates ) {
	boolean dirty = false;
	synchronized( this.digitValues ) {
	  for( int i = 0; i < this.digitValues.length; i++ ) {
	    if( this.digitStatus[ i ] > 0 ) {
	      --this.digitStatus[ i ];
	    } else {
	      if( this.digitValues[ i ] != 0 ) {
		this.digitValues[ i ] = 0;
		dirty = true;
	      }
	    }
	  }
	}
	if( this.tapeOutState ) {
	  this.tapeOutLED = !this.tapeOutLED;
	  dirty           = true;
	} else {
	  if( this.tapeOutLED ) {
	    this.tapeOutLED = false;
	    dirty           = true;
	  }
	}
	if( dirty ) {
	  this.screenFrm.setScreenDirty( true );
	}
	this.tapeOutState      = false;
	this.curDisplayTStates = 0;
      }
    }
    if( this.sio != null ) {
      /*
       * SIO-Takt mit 1:12-Teiler vom Systemtakt ableiten
       */
      this.sioTStatesCounter += tStates;
      while( this.sioTStatesCounter >= 12 ) {
	this.sioTStatesCounter -= 12;
	this.sio.clockPulseSenderA();		// Drucker
	this.sio.clockPulseSenderB();		// Terminal
	this.sio.clockPulseReceiverB();		// Terminal
      }
    }
  }


	/* --- private Methoden --- */

  private void putKBMatrixRowValueToPort()
  {
    int v = 0;
    synchronized( this.kbMatrix ) {
      int m = 0x04;
      for( int i = 0; i < this.kbMatrix.length; i++ ) {
	if( (this.pioSysBValue & m) == 0 ) {
	  v |= this.kbMatrix[ i ];
	}
	m <<= 1;
      }
    }
    this.pioUser.putInValuePortB( ~v, 0xF0 );
  }


  private void setChessMode( boolean state )
  {
    if( state != this.chessMode ) {
      this.chessMode = state;
      if( this.keyboardFld != null ) {
        this.keyboardFld.invalidate();
        this.keyboardFld.repaint();
        this.keyboardFld.validate();
      }
    }
  }


  private static byte toBcdByte( int value )
  {
    return (byte) (((((value / 10) % 10) << 4) & 0xF0)
				| ((value % 10) & 0x0F));
  }


  /*
   * Eingang: L-Aktiv
   *   Bit: 0 -> B
   *   Bit: 1 -> F
   *   Bit: 2 -> A
   *   Bit: 3 -> G
   *   Bit: 4 -> P
   *   Bit: 5 -> C
   *   Bit: 6 -> E
   *   Bit: 7 -> D
   *
   * Ausgang: H-Aktiv
   *   Bit: 0 -> A
   *   Bit: 1 -> B
   *   Bit: 2 -> C
   *   Bit: 3 -> D
   *   Bit: 4 -> E
   *   Bit: 5 -> F
   *   Bit: 6 -> G
   *   Bit: 7 -> P
   */
  private int toDigitValue( int value )
  {
    int rv = 0;
    if( (value & 0x01) == 0 ) {
      rv |= 0x02;
    }
    if( (value & 0x02) == 0 ) {
      rv |= 0x20;
    }
    if( (value & 0x04) == 0 ) {
      rv |= 0x01;
    }
    if( (value & 0x08) == 0 ) {
      rv |= 0x40;
    }
    if( (value & 0x10) == 0 ) {
      rv |= 0x80;
    }
    if( (value & 0x20) == 0 ) {
      rv |= 0x04;
    }
    if( (value & 0x40) == 0 ) {
      rv |= 0x10;
    }
    if( (value & 0x80) == 0 ) {
      rv |= 0x08;
    }
    return rv;
  }


  private void updDisplay()
  {
    boolean dirty = false;
    synchronized( this.digitValues ) {
      int m = 0x80;
      for( int i = 0; i < this.digitValues.length; i++ ) {
	if( (this.pioSysBValue & m) == 0 ) {
	  this.digitStatus[ i ] = 2;
	  if( this.digitValues[ i ] != this.curDigitValue ) {
	    this.digitValues[ i ] = this.curDigitValue;
	    dirty = true;
	  }
	}
	m >>= 1;
      }
    }
    if( dirty )
      this.screenFrm.setScreenDirty( true );
  }


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.kbMatrix );
  }


  private void updTimekeeperRAM()
  {
    if( this.ram.length >= 0x6000 ) {
      Calendar cal       = Calendar.getInstance();
      this.ram[ 0x5FF9 ] = toBcdByte( cal.get( Calendar.SECOND ) );
      this.ram[ 0x5FFA ] = toBcdByte( cal.get( Calendar.MINUTE ) );
      this.ram[ 0x5FFB ] = toBcdByte( cal.get( Calendar.HOUR_OF_DAY ) );
      this.ram[ 0x5FFC ] = toBcdByte( cal.get( Calendar.DAY_OF_WEEK ) );
      this.ram[ 0x5FFD ] = toBcdByte( cal.get( Calendar.DAY_OF_MONTH ) );
      this.ram[ 0x5FFE ] = toBcdByte( cal.get( Calendar.MONTH ) + 1 );
      this.ram[ 0x5FFF ] = toBcdByte( cal.get( Calendar.YEAR ) % 100 );
    }
  }
}
