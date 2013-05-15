/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Z1013
 */

package jkcemu.emusys;

import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.emusys.z1013.*;
import jkcemu.etc.*;
import jkcemu.joystick.JoystickThread;
import jkcemu.net.KCNet;
import jkcemu.print.PrintMngr;
import jkcemu.text.TextUtil;
import z80emu.*;


public class Z1013 extends EmuSys implements
					FDC8272.DriveSelector,
					Z80AddressListener,
					Z80PCListener,
					Z80TStatesListener
{
  public static final int MEM_ARG1 = 0x001B;
  public static final int MEM_HEAD = 0x00E0;

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

  private static final FloppyDiskInfo cpm64x16FloppyDisk =
		new FloppyDiskInfo(
			"/disks/z1013/z1013cpm64x16.dump.gz",
			"Z1013 CP/M Boot-Diskette (64x16 Zeichen)" );

  private static final FloppyDiskInfo cpm80x25FloppyDisk =
		new FloppyDiskInfo(
			"/disks/z1013/z1013cpm80x25.dump.gz",
			"Z1013 CP/M Boot-Diskette (80x25 Zeichen)" );

  private static final FloppyDiskInfo[] availableFloppyDisks = {
						cpm64x16FloppyDisk,
						cpm80x25FloppyDisk };

  private enum BasicType { Z1013_TINY, KC_RAM, KC_ROM };
  private enum UserPort {
			NONE,
			JOY_JUTE_6_1987,
			JOY_PRACTIC_4_1987,
			JOY_PRACTIC_1_1988,
			CENTR7_PRACTIC_2_1989,
			CENTR8_FA_10_1990 };

  private static byte[] mon202         = null;
  private static byte[] monA2          = null;
  private static byte[] monRB_K7659    = null;
  private static byte[] monRB_S6009    = null;
  private static byte[] monINCOM_K7669 = null;
  private static byte[] monJM_1992     = null;
  private static byte[] bl4_K7659      = null;
  private static byte[] modBasic       = null;
  private static byte[] fontStd        = null;
  private static byte[] fontAlt        = null;

  private Z80PIO              pio;
  private GIDE                gide;
  private FDC8272             fdc;
  private RTC7242X            rtc;
  private GraphicCCJ          graphCCJ;
  private Z1013Keyboard       keyboard;
  private AbstractKeyboardFld keyboardFld;
  private KCNet               kcNet;
  private VDIP                vdip;
  private RAMFloppy           ramFloppy1;
  private RAMFloppy           ramFloppy2;
  private int                 ramPixelBank;
  private int                 ramEndAddr;
  private byte[][]            ramPixel;
  private byte[]              ramStatic;
  private byte[]              ramVideo;
  private byte[]              romBasic;
  private byte[]              romMega;
  private byte[]              stdFontBytes;
  private byte[]              altFontBytes;
  private byte[]              osBytes;
  private String              osFile;
  private String              romBasicFile;
  private String              romMegaFile;
  private String              monCode;
  private boolean             romDisabled;
  private boolean             altFontEnabled;
  private boolean             graphCCJActive;
  private boolean             catchPrintCalls;
  private boolean             mode4MHz;
  private boolean             mode64x16;
  private volatile boolean    modeGraph;
  private volatile boolean    pasteFast;
  private volatile int        charToPaste;
  private int                 centrTStatesToAck;
  private int                 romMegaSeg;
  private int                 lastWrittenAddr;
  private int[]               pcListenerAddrs;
  private FloppyDiskDrive[]   floppyDiskDrives;
  private BasicType           lastBasicType;
  private UserPort            userPort;
  private volatile int        joy0ActionMask;
  private volatile int        joy1ActionMask;
  private volatile boolean    catchJoyCalls;


  public Z1013( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.stdFontBytes      = null;
    this.altFontBytes      = null;
    this.osBytes           = null;
    this.osFile            = null;
    this.monCode           = null;
    this.romBasic          = null;
    this.romBasicFile      = null;
    this.romMega           = null;
    this.romMegaFile       = null;
    this.romDisabled       = false;
    this.altFontEnabled    = false;
    this.graphCCJActive    = false;
    this.mode64x16         = false;
    this.mode4MHz          = false;
    this.modeGraph         = false;
    this.pcListenerAddrs   = null;
    this.lastBasicType     = null;
    this.userPort          = UserPort.NONE;
    this.lastWrittenAddr   = -1;
    this.pasteFast         = false;
    this.charToPaste       = 0;
    this.centrTStatesToAck = 0;
    this.romMegaSeg        = 0;
    this.ramPixelBank      = 0;
    this.ramEndAddr        = getRAMEndAddr( props );
    this.ramVideo          = new byte[ 0x0400 ];
    this.ramStatic         = null;
    if( this.ramEndAddr == 0x03FF ) {
      this.ramStatic = new byte[ 0x0400 ];
    }

    this.ramPixel = null;
    if( emulatesGraphic( props ) ) {
      this.ramPixel = new byte[ 8 ][];
      for( int i = 0; i < this.ramPixel.length; i++ ) {
	this.ramPixel[ i ] = new byte[ 0x0400 ];
      }
    }

    this.ramFloppy1 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy1(),
				"Z1013",
				RAMFloppy.RFType.MP_3_1988,
				"RAM-Floppy an IO-Adressen 98h-9Fh",
				props,
				"jkcemu.z1013.ramfloppy.1." );

    this.ramFloppy2 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy2(),
				"Z1013",
				RAMFloppy.RFType.MP_3_1988,
				"RAM-Floppy an IO-Adressen 58h-5Fh",
				props,
				"jkcemu.z1013.ramfloppy.2." );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    this.pio   = new Z80PIO( "PIO (IO-Adressen 00-03)" );
    cpu.addAddressListener( this );
    checkAddPCListener( props );

    if( emulatesFloppyDisk( props ) ) {
      this.floppyDiskDrives = new FloppyDiskDrive[ 4 ];
      Arrays.fill( this.floppyDiskDrives, null );
      this.fdc = new FDC8272( this, 4 );
      this.fdc.setTStatesPerMilli( cpu.getMaxSpeedKHz() );
      cpu.addMaxSpeedListener( this.fdc );
    } else {
      this.floppyDiskDrives = null;
      this.fdc              = null;
    }
    cpu.addTStatesListener( this );

    this.rtc = emulatesRTC( props ) ? new RTC7242X() : null;

    if( emulatesGraphCCJ( props ) ) {
      this.graphCCJ = new GraphicCCJ(
		this.screenFrm,
		EmuUtil.getProperty(
			props,
			"jkcemu.z1013.graph_ccj.font.file" ) );
    } else {
      this.graphCCJ = null;
    }

    if( emulatesKCNet( props ) ) {
      this.kcNet = new KCNet( "Netzwerk-PIO (IO-Adressen C0-C3)" );
    } else {
      this.kcNet = null;
    }

    if( emulatesUSB( props ) ) {
      this.vdip = new VDIP( "USB-PIO (IO-Adressen FC-FF)" );
    } else {
      this.vdip = null;
    }

    this.gide = GIDE.getGIDE( this.screenFrm, props, "jkcemu.z1013." );

    java.util.List<Z80InterruptSource> iSources
				= new ArrayList<Z80InterruptSource>();
    iSources.add( this.pio );
    if( this.kcNet != null ) {
      iSources.add( this.kcNet );
      cpu.addMaxSpeedListener( this.kcNet );
      this.kcNet.z80MaxSpeedChanged( cpu );
    }
    if( this.vdip != null ) {
      iSources.add( this.vdip );
    }
    try {
      cpu.setInterruptSources(
        iSources.toArray( new Z80InterruptSource[ iSources.size() ] ) );
    }
    catch( ArrayStoreException ex ) {}

    this.keyboardFld = null;
    this.keyboard    = new Z1013Keyboard( this.pio );
    this.keyboard.applySettings( props );

    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
    applyUserPortSettings( props );
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
  }


  public static FloppyDiskInfo[] getAvailableFloppyDisks()
  {
    return availableFloppyDisks;
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "Z1013.01" ) ?
								1000 : 2000;
  }


  public static String getTinyBasicProgram( Z80MemView memory )
  {
    return SourceUtil.getTinyBasicProgram(
				memory,
				0x1152,
				memory.getMemWord( 0x101F ) );
  }


  public Z1013Keyboard getZ1013Keyboard()
  {
    return this.keyboard;
  }


	/* --- FDC8272.DriveSelector --- */

  @Override
  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    FloppyDiskDrive rv = null;
    if( this.floppyDiskDrives != null ) {
      if( (driveNum >= 0) && (driveNum < this.floppyDiskDrives.length) ) {
	rv = this.floppyDiskDrives[ driveNum ];
      }
    }
    return rv;
  }


	/* --- Z80AddressListener --- */

  @Override
  public void z80AddressChanged( int addr )
  {
    // Verbindung A0 - PIO B5 emulieren
    this.pio.putInValuePortB( (addr << 5) & 0x20, 0x20 );
  }


	/* --- Z80PCListener --- */

  @Override
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

      case 0xFFBB:	// Abfrage Joysticks
	{
	  int[] masks = { this.joy1ActionMask, this.joy0ActionMask };
	  int   bc    = 0;
	  for( int i = 0; i < masks.length; i++ ) {
	    bc <<= 8;
	    int m = masks[ i ];
	    if( (m & JoystickThread.LEFT_MASK) != 0 ) {
	      bc |= 0x01;
	    }
	    if( (m & JoystickThread.RIGHT_MASK) != 0 ) {
	      bc |= 0x02;
	    }
	    if( (m & JoystickThread.DOWN_MASK) != 0 ) {
	      bc |= 0x04;
	    }
	    if( (m & JoystickThread.UP_MASK) != 0 ) {
	      bc |= 0x08;
	    }
	    if( (m & JoystickThread.BUTTONS_MASK) != 0 ) {
	      bc |= 0x10;
	    }
	  }
	  cpu.setRegBC( bc );
	  cpu.setFlagZero( bc == 0 );
	  cpu.setFlagCarry( false );
	}
	done = true;
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
	  int       addr      = 0xEC00;
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


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80TStatesProcessed( cpu, tStates );
    }
    if( this.centrTStatesToAck > 0 ) {
      this.centrTStatesToAck -= tStates;
      if( this.centrTStatesToAck <= 0 ) {
	switch( this.userPort ) {
	  case CENTR7_PRACTIC_2_1989:
	    // BUSY (Port A Bit 7) zuruecksetzen
	    this.pio.putInValuePortA( 0, 0x80 );
	    break;
	  case CENTR8_FA_10_1990:
	    this.pio.strobePortA();
	    break;
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
    loadFont( props );
    applyUserPortSettings( props );
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
				props,
				"jkcemu.system" ).startsWith( "Z1013" );
    if( rv ) {
      rv = TextUtil.equals(
		this.osFile,
		EmuUtil.getProperty( props,  "jkcemu.z1013.os.file" ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.monCode,
		EmuUtil.getProperty( props,  "jkcemu.z1013.monitor" ) );
    }
    if( rv && (getRAMEndAddr( props ) != this.ramEndAddr) ) {
      rv = false;
    }
    if( rv && emulatesModuleBasic( props ) == (this.romBasic != null) ) {
      if( (this.romBasic != null)
	  && !TextUtil.equals(
		this.romBasicFile,
		EmuUtil.getProperty(
			props,
			"jkcemu.z1013.rom_basic.file" ) ) )
      {
	rv = false;
      }
    } else {
      rv = false;
    }
    if( rv && emulatesModuleMegaROM( props ) == (this.romMega != null) ) {
      if( (this.romMega != null)
	  && !TextUtil.equals(
		this.romMegaFile,
		EmuUtil.getProperty(
			props,
			"jkcemu.z1013.rom_mega.file" ) ) )
      {
	rv = false;
      }
    } else {
      rv = false;
    }
    if( rv ) {
      rv = GIDE.complies( this.gide, props, "jkcemu.z1013." );
    }
    if( rv && emulatesFloppyDisk( props ) != (this.fdc != null) ) {
      rv = false;
    }
    if( rv && (emulatesRTC( props ) != (this.rtc != null)) ) {
      rv = false;
    }
    if( rv && (emulatesGraphCCJ( props ) != (this.graphCCJ != null)) ) {
      rv = false;
    }
    if( rv && (emulatesGraphic( props ) != (this.ramPixel != null)) ) {
      rv = false;
    }
    if( rv && (emulatesKCNet( props ) != (this.kcNet != null)) ) {
      rv = false;
    }
    if( rv && (emulatesUSB( props ) != (this.vdip != null)) ) {
      rv = false;
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy1,
			"Z1013",
			RAMFloppy.RFType.MP_3_1988,
			props,
			"jkcemu.z1013.ramfloppy.1." );
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy2,
			"Z1013",
			RAMFloppy.RFType.MP_3_1988,
			props,
			"jkcemu.z1013.ramfloppy.2." );
    }
    return rv;
  }


  @Override
  public boolean canExtractScreenText()
  {
    boolean rv = !this.modeGraph;
    if( (this.graphCCJ != null) && this.graphCCJActive ) {
      rv = this.graphCCJ.canExtractScreenText();
    }
    return rv;
  }


  @Override
  public AbstractKeyboardFld createKeyboardFld()
					throws UnsupportedOperationException
  {
    AbstractKeyboardFld kbFld    = null;
    KeyboardMatrix      kbMatrix = this.keyboard.getKeyboardMatrix();
    if( kbMatrix != null ) {
      if( kbMatrix instanceof KeyboardMatrix8x4 ) {
	kbFld = new Z1013KeyboardFld8x4( this );
      }
      else if( kbMatrix instanceof KeyboardMatrix8x8 ) {
	kbFld = new Z1013KeyboardFld8x8( this );
      }
    }
    this.keyboardFld = kbFld;
    if( kbFld == null ) {
      throw new UnsupportedOperationException();
    }
    return kbFld;
  }


  @Override
  public void die()
  {
    if( this.gide != null ) {
      this.gide.die();
    }
    if( this.ramFloppy1 != null ) {
      this.ramFloppy1.deinstall();
    }
    if( this.ramFloppy2 != null ) {
      this.ramFloppy2.deinstall();
    }

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeAddressListener( this );
    cpu.removeTStatesListener( this );
    if( this.fdc != null ) {
      cpu.removeMaxSpeedListener( this.fdc );
      this.fdc.die();
    }
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.pcListenerAddrs != null ) {
      cpu.removePCListener( this );
    }
    if( this.kcNet != null ) {
      cpu.removeMaxSpeedListener( this.kcNet );
      this.kcNet.die();
    }
    if( this.vdip != null ) {
      this.vdip.die();
    }
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x00B0;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = BLACK;
    if( (this.graphCCJ != null) && this.graphCCJActive ) {
      if( this.graphCCJ.getPixel( x, y ) ) {
	rv = WHITE;
      }
    } else {
      int b = 0;
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
	byte[] fontBytes = null;
	if( this.altFontEnabled ) {
	  fontBytes = this.altFontBytes;
	} else {
	  fontBytes = this.stdFontBytes;
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
    }
    return rv;
  }


  @Override
  public int getCharColCount()
  {
    int rv = 32;
    if( (this.graphCCJ != null) && this.graphCCJActive ) {
      rv = this.graphCCJ.getCharColCount();
    } else {
      if( this.mode64x16 ) {
	rv = 64;
      }
    }
    return rv;
  }


  @Override
  public int getCharHeight()
  {
    return ((this.graphCCJ != null) && this.graphCCJActive) ?
					this.graphCCJ.getCharRowHeight()
					: 8;
  }


  @Override
  public int getCharRowCount()
  {
    int rv = 32;
    if( (this.graphCCJ != null) && this.graphCCJActive ) {
      rv = this.graphCCJ.getCharRowCount();
    } else {
      if( this.mode64x16 ) {
	rv = 16;
      }
    }
    return rv;
  }


  @Override
  public int getCharRowHeight()
  {
    int rv = 8;
    if( (this.graphCCJ != null) && this.graphCCJActive ) {
      rv = this.graphCCJ.getCharRowHeight();
    } else {
      if( this.mode64x16 ) {
	rv = 16;
      }
    }
    return rv;
  }


  @Override
  public int getCharTopLine()
  {
    return (this.graphCCJ != null) && this.graphCCJActive ?
					this.graphCCJ.getCharTopLine()
					: 0;
  }


  @Override
  public int getCharWidth()
  {
    return ((this.graphCCJ != null) && this.graphCCJActive) ?
					this.graphCCJ.getCharWidth()
					: 8;
  }


  @Override
  public boolean getDefaultFloppyDiskBlockNum16Bit()
  {
    return true;
  }


  @Override
  public int getDefaultFloppyDiskBlockSize()
  {
    return this.fdc != null ? 2048 : -1;
  }


  @Override
  public int getDefaultFloppyDiskDirBlocks()
  {
    return this.fdc != null ? 2 : -1;
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return this.fdc != null ?
		FloppyDiskFormat.getFormat( 2, 80, 5, 1024 )
		: null;
  }


  @Override
  public int getDefaultFloppyDiskSystemTracks()
  {
    return this.fdc != null ? 2 : -1;
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return this.pasteFast ? 0 : 150;
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return this.pasteFast ? 0 : 200;
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return this.pasteFast ? 0 : 80;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/z1013.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( (addr >= 0xC000) && (addr < 0xEC00) ) {
      byte[] rom = this.romBasic;
      if( rom != null ) {
	int idx = addr - 0xC000;
	if( idx < rom.length ) {
	  rv   = (int) rom[ idx ] & 0xFF;
	  done = true;
	}
      } else {
	rom = this.romMega;
	if( (rom != null) && (addr < 0xE800) ) {
	  int idx = 0;
	  if( addr < 0xC800 ) {
	    idx = addr - 0xC000 + (this.romMegaSeg * 2048);
	  } else if( addr < 0xD000 ) {
	    idx = addr - 0xC800 + ((256 + this.romMegaSeg) * 2048);
	  } else if( addr < 0xD800 ) {
	    idx = addr - 0xD000 + (((2 * 256) + this.romMegaSeg) * 2048);
	  } else if( addr < 0xE000 ) {
	    idx = addr - 0xD800 + (((3 * 256) + this.romMegaSeg) * 2048);
	  } else {
	    idx = addr - 0xE000 + (((4 * 256) + this.romMegaSeg) * 2048);
	  }
	  if( (idx >= 0) && (idx < this.romMega.length) ) {
	    rv = (int) rom[ idx ] & 0xFF;
	  }
	  done = true;
	}
      }
    }
    else if( (addr >= 0xEC00) && (addr < (0xEC00 + this.ramVideo.length)) ) {
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
	int idx = addr - 0xEC00;
	if( (idx >= 0) && (idx < ram.length) ) {
	  rv = (int) ram[ idx ] & 0xFF;
	}
      }
      done = true;
    }
    else if( (addr >= 0xF000) && !this.romDisabled ) {
      byte[] rom = this.osBytes;
      if( rom != null ) {
	int idx = addr - 0xF000;
	if( idx < rom.length ) {
	  rv   = (int) rom[ idx ] & 0xFF;
	  done = true;
	}
      }
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


  @Override
  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return 0xF000;
  }


  @Override
  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    if( (this.graphCCJ != null) && this.graphCCJActive ) {
      ch = this.graphCCJ.getScreenChar( chX, chY );
    } else {
      int idx = (chY * (this.mode64x16 ? 64 : 32)) + chX;
      if( (idx >= 0) && (idx < this.ramVideo.length) ) {
	int b = (int) this.ramVideo[ idx ] & 0xFF;
	if( (b >= 0x20) && (b < 0x7F) ) {
	  ch = b;
	}
	else if( (ch >= 0xA0) && (ch < 0xFF) && this.altFontEnabled ) {
	  ch = b & 0x7F; 	// 0xA0 bis 0xFE: invertierte Zeichen
	}
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    int rv = 256;
    if( (this.graphCCJ != null) && this.graphCCJActive ) {
      rv = this.graphCCJ.getScreenHeight();
    } else {
      if( this.mode64x16 ) {
	rv = 248;
      }
    }
    return rv;
  }


  @Override
  public int getScreenWidth()
  {
    int rv = 256;
    if( (this.graphCCJ != null) && this.graphCCJActive ) {
      rv = this.graphCCJ.getScreenWidth();
    } else {
      if( this.mode64x16 ) {
	rv = 512;
      }
    }
    return rv;
  }


  @Override
  public FloppyDiskInfo[] getSuitableFloppyDisks()
  {
    FloppyDiskInfo[] disks = null;
    if( this.fdc != null ) {
      if( this.graphCCJ != null ) {
	disks = availableFloppyDisks;
      } else {
	disks      = new FloppyDiskInfo[ 1 ];
	disks[ 0 ] = cpm64x16FloppyDisk;
      }
    }
    return disks;
  }


  @Override
  public int getSupportedFloppyDiskDriveCount()
  {
    return this.floppyDiskDrives != null ? this.floppyDiskDrives.length : 0;
  }


  @Override
  public int getSupportedJoystickCount()
  {
    int rv = 0;
    if( this.catchJoyCalls ) {
      rv = 2;
    } else {
      switch( this.userPort ) {
	case JOY_JUTE_6_1987:
	  rv = 1;
	  break;
	case JOY_PRACTIC_4_1987:
	case JOY_PRACTIC_1_1988:
	  rv = 2;
	  break;
      }
    }
    return rv;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  @Override
  public String getTitle()
  {
    return "Z1013";
  }


  @Override
  protected VDIP getVDIP()
  {
    return this.vdip;
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = this.keyboard.setKeyCode( keyCode );
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    this.keyboard.setKeyReleased();
    updKeyboardFld();
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean hexMode = false;
    if( this.osBytes == mon202 ) {
      hexMode = (getMemByte( 0x0042, false ) == 0x48);
    }
    else if( this.osBytes == monJM_1992 ) {
      hexMode = (getMemByte( 0x003E, false ) == 0x48);
    }
    boolean rv = this.keyboard.setKeyChar( ch, hexMode );
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  @Override
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

      default:
	cancelled = true;
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


  @Override
  protected boolean pasteChar( char ch )
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


  @Override
  public int readIOByte( int port )
  {
    int rv = 0x0F;			// wird von unbelegten Ports gelesen

    port &= 0xFF;
    if( port == 4 ) {
      rv = 0;
      if( this.romDisabled ) {
	rv |= 0x10;
      }
      if( this.altFontEnabled ) {
	rv |= 0x20;
      }
      if( this.mode4MHz ) {
	rv |= 0x40;
      }
      if( this.mode64x16 ) {
	rv |= 0x80;
      }
    }
    else if( (port == 0x18) && (this.graphCCJ != null) ) {
      rv = this.graphCCJ.readStatus();
    }
    else if( (port == 0x19) && (this.graphCCJ != null) ) {
      rv = this.graphCCJ.readData();
    }
    else if( (port & 0xF0) == 0x70 ) {
      if( this.rtc != null ) {
	rv = this.rtc.read( port );
      }
    }
    if( (this.gide != null) && ((port & 0xF0) == 0x80) ) {
      int value = this.gide.read( port );
      if( value >= 0 ) {
	rv = value;
      }
    }
    else if( ((port & 0xF8) == 0x98) && (this.ramFloppy1 != null) ) {
      rv = this.ramFloppy1.readByte( port & 0x07 );
    }
    else if( ((port & 0xF8) == 0x58) && (this.ramFloppy2 != null) ) {
      rv = this.ramFloppy2.readByte( port & 0x07 );
    }
    else if( ((port & 0xF0) == 0xC0) && (this.kcNet != null) ) {
      rv = this.kcNet.read( port );
    }
    else if( ((port & 0xFC) == 0xDC) && (this.vdip != null) ) {
      rv = this.vdip.read( port );
    }
    else if( (port >= 0xF0) && (port <= 0xFF) ) {
      switch( port ) {
	case 0xF0:
	  if( this.fdc != null ) {
	    rv = this.fdc.readMainStatusReg();
	  }
	  break;

	case 0xF1:
	  if( this.fdc != null ) {
	    rv = this.fdc.readData();
	  }
	  break;

	case 0xF8:
	  if( this.fdc != null ) {
	    this.fdc.fireTC();
	  }
	  break;

	case 0xFA:
	  if( this.fdc != null ) {
	    this.fdc.reset( false );
	  }
	  break;

	case 0xFC:
	case 0xFD:
	case 0xFE:
	case 0xFF:
	  if( (this.romMega == null) && (this.vdip != null) ) {
	    rv = this.vdip.read( port );
	  }
	  break;
      }
    } else {
      switch( port & 0x1C ) {		// Adressleitungen ab A5 ignorieren
	case 0:				// IOSEL0 -> PIO
	  switch( port & 0x03 ) {
	    case 0:
	      rv = this.pio.readPortA();
	      break;

	    case 1:
	      rv = this.pio.readControlA();
	      break;

	    case 2:
	      this.pio.putInValuePortB(
			this.emuThread.readAudioPhase() ? 0x40 : 0, 0x40 );
	      rv = this.pio.readPortB();
	      break;

	    case 3:
	      rv = this.pio.readControlB();
	      break;
	  }
	  break;

	case 0x0C:				// IOSEL3 -> Vollgrafik ein
	  this.modeGraph = true;
	  this.screenFrm.setScreenDirty( true );
	  break;

	case 0x10:				// IOSEL3 -> Vollgrafik aus
	  this.modeGraph = false;
	  this.screenFrm.setScreenDirty( true );
	  break;
      }
    }
    z80AddressChanged( port );
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
    int rv  = 0;
    int bol = buf.length();
    int b   = memory.getMemByte( addr, true );
    if( b == 0xE7 ) {
      if( !sourceOnly ) {
	buf.append( String.format( "%04X  E7", addr ) );
      }
      appendSpacesToCol( buf, bol, colMnemonic );
      buf.append( "RST" );
      appendSpacesToCol( buf, bol, colArgs );
      buf.append( "20H\n" );
      rv = 1;
    } else if( b == 0xCD ) {
      if( memory.getMemWord( addr + 1 ) == 0x0020 ) {
	if( !sourceOnly ) {
	  buf.append( String.format( "%04X  CD 00 20", addr ) );
	}
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
      b   = memory.getMemByte( addr, false );
      if( !sourceOnly ) {
	buf.append( String.format( "%04X  %02X", addr, b ) );
      }
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
			memory,
			addr + 1,
			buf,
			sourceOnly,
			colMnemonic,
			colArgs );
      }
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    boolean old64x16 = this.mode64x16;

    this.centrTStatesToAck = 0;
    this.joy0ActionMask    = 0;
    this.joy1ActionMask    = 0;
    this.romMegaSeg        = 0;
    this.lastWrittenAddr   = -1;
    this.romDisabled       = false;
    this.altFontEnabled    = false;
    this.modeGraph         = false;
    if( this.mode64x16 || this.graphCCJActive ) {
      this.graphCCJActive = false;
      this.mode64x16      = false;
      this.screenFrm.fireScreenSizeChanged();
    }
    if( this.mode4MHz ) {
      this.emuThread.updCPUSpeed( Main.getProperties() );
      this.mode4MHz = false;
    }
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
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
    if( this.gide != null ) {
      this.gide.reset();
    }
    if( this.fdc != null ) {
      this.fdc.reset( resetLevel == EmuThread.ResetLevel.POWER_ON );
    }
    if( this.floppyDiskDrives != null ) {
      for( int i = 0; i < this.floppyDiskDrives.length; i++ ) {
	FloppyDiskDrive drive = this.floppyDiskDrives[ i ];
	if( drive != null ) {
	  drive.reset();
	}
      }
    }
    // sicherstellen, dass Drucker Empfamgsbereitschaft meldet
    if( this.userPort == UserPort.CENTR7_PRACTIC_2_1989 ) {
      this.pio.putInValuePortA( 0, 0x80 );
    }
  }


  @Override
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

      default:
	cancelled = true;
    }
    if( !cancelled ) {
      if( (begAddr > 0) && (endAddr > begAddr) ) {
	(new SaveDlg(
		this.screenFrm,
		begAddr,
		endAddr,
		hsType,
		kcbasic,
		false,		// kein RBASIC
		"BASIC-Programm speichern" )).setVisible( true );
      } else {
	showNoBasic();
      }
    }
  }


  @Override
  public void setJoystickAction( int joyNum, int actionMask )
  {
    if( joyNum == 0 ) {
      this.joy0ActionMask = actionMask;
    } else if( joyNum == 1 ) {
      this.joy1ActionMask = actionMask;
    }
    switch( this.userPort ) {
      case JOY_JUTE_6_1987:
	putJoyJuTe0687ValuesToPort();
	break;
      case JOY_PRACTIC_4_1987:
	putJoyPractic0487ValuesToPort();
	break;
      case JOY_PRACTIC_1_1988:
	putJoyPractic0188ValuesToPort();
	break;
    }
  }


  @Override
  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    if( this.floppyDiskDrives != null ) {
      if( (idx >= 0) && (idx < this.floppyDiskDrives.length) ) {
	this.floppyDiskDrives[ idx ] = drive;
      }
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( (addr >= 0xC000) && (addr < 0xEC00) ) {
      if( (this.romBasic != null)
	  || ((this.romMega != null) && (addr < 0xE800)) )
      {
	done = false;
      }
    }
    else if( (addr >= 0xEC00) && (addr < (0xEC00 + this.ramVideo.length)) ) {
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
	int idx = addr - 0xEC00;
	if( (idx >= 0) && (idx < ram.length) ) {
	  ram[ idx ] = (byte) value;
	  this.screenFrm.setScreenDirty( true );
	  rv = true;
	}
      }
      done = true;
    }
    else if( (addr >= 0xF000) && !this.romDisabled
	     && (this.osBytes != null) )
    {
      if( addr < (0xF000 + this.osBytes.length) ) {
	done = true;
      }
    }
    if( !done && (this.ramStatic != null) ) {
      if( addr < this.ramStatic.length ) {
	this.ramStatic[ addr ] = (byte) value;
	rv   = true;
	done = true;
      }
    }
    if( !done && (addr <= this.ramEndAddr) ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    boolean rv = true;
    if( (this.graphCCJ != null) && this.graphCCJActive ) {
      rv = this.graphCCJ.shouldAskConvertScreenChar();
    } else {
      if( this.altFontEnabled ) {
	if( this.altFontBytes == fontAlt ) {
	  rv = false;
	}
      } else {
	if( this.stdFontBytes == fontStd ) {
	  rv = false;
	}
      }
    }
    return rv;
  }


  @Override
  public boolean supportsAudio()
  {
    return true;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsKeyboardFld()
  {
    boolean        rv       = false;
    KeyboardMatrix kbMatrix = this.keyboard.getKeyboardMatrix();
    if( kbMatrix != null ) {
      if( (kbMatrix instanceof KeyboardMatrix8x4)
	  || (kbMatrix instanceof KeyboardMatrix8x8) )
      {
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
  public boolean supportsPasteFromClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsPrinter()
  {
    return this.catchPrintCalls
		|| (this.userPort == UserPort.CENTR7_PRACTIC_2_1989)
		|| (this.userPort == UserPort.CENTR8_FA_10_1990);
  }


  @Override
  public boolean supportsRAMFloppy1()
  {
    return this.ramFloppy1 != null;
  }


  @Override
  public boolean supportsRAMFloppy2()
  {
    return this.ramFloppy2 != null;
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
    SourceUtil.updKCBasicSysCells(
			this.emuThread,
			begAddr,
			len,
			fileFmt,
			fileType );
  }


  @Override
  public void writeIOByte( int port, int value )
  {
    port &= 0xFF;
    if( port == 4 ) {
      boolean oldAltFontEnabled = this.altFontEnabled;
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
	this.screenFrm.clearScreenSelection();
	this.screenFrm.fireScreenSizeChanged();
      }
    }
    else if( (port == 0x18) && (this.graphCCJ != null) ) {
      this.graphCCJ.writeArg( value );
    }
    else if( (port == 0x19) && (this.graphCCJ != null) ) {
      this.graphCCJ.writeCmd( value );
      if( !this.graphCCJActive ) {
	this.graphCCJActive = true;
	this.screenFrm.fireScreenSizeChanged();
      }
    }
    else if( (port & 0xF0) == 0x70 ) {
      if( this.rtc != null ) {
	this.rtc.write( port, value );
      }
    }
    else if( (this.gide != null) && ((port & 0xF0) == 0x80) ) {
      this.gide.write( port, value );
    }
    else if( ((port & 0xF8) == 0x98) && (this.ramFloppy1 != null) ) {
      this.ramFloppy1.writeByte( port & 0x07, value );
    }
    else if( ((port & 0xF8) == 0x58) && (this.ramFloppy2 != null) ) {
      this.ramFloppy2.writeByte( port & 0x07, value );
    }
    else if( ((port & 0xF0) == 0xC0) && (this.kcNet != null) ) {
      this.kcNet.write( port, value );
    }
    else if( ((port & 0xFC) == 0xDC) && (this.vdip != null) ) {
      this.vdip.write( port, value );
    }
    else if( (port >= 0xF0) && (port <= 0xFF) ) {
      switch( port ) {
	case 0xF1:
	  if( this.fdc != null ) {
	    this.fdc.write( value );
	  }
	  break;

	case 0xF8:
	  if( this.fdc != null ) {
	    this.fdc.fireTC();
	  }
	  break;

	case 0xFA:
	  if( this.fdc != null ) {
	    this.fdc.reset( false );
	  }
	  break;

	case 0xFC:
	case 0xFD:
	case 0xFE:
	case 0xFF:
	  if( this.romMega != null ) {
	    if( port == 0xFF ) {
	      this.romMegaSeg = value;
	    }
	  } else {
	    if( this.vdip != null ) {
	      this.vdip.write( port, value );
	    }
	  }
	  break;
      }
    } else {
      switch( port & 0x1C ) {		// Adressleitungen ab A5 ignorieren
	case 0:				// IOSEL0 -> PIO
	  switch( port & 0x03 ) {
	    case 0:
	      this.pio.writePortA( value );
	      if( this.userPort == UserPort.JOY_PRACTIC_1_1988 ) {
		putJoyPractic0188ValuesToPort();
	      }
	      else if( (this.userPort == UserPort.CENTR7_PRACTIC_2_1989)
		       || (this.userPort == UserPort.CENTR8_FA_10_1990) )
	      {
		if( this.pio.getModePortA() == Z80PIO.Mode.BYTE_OUT ) {
		  PrintMngr printMngr = this.emuThread.getPrintMngr();
		  if( this.userPort == UserPort.CENTR7_PRACTIC_2_1989 ) {
		    if( printMngr != null ) {
		      printMngr.putByte( value & 0x7F );
		    }
		    // BUSY (Port A Bit 7) setzen
		    this.pio.putInValuePortA( 0x80, 0x80 );
		  } else {
		    if( printMngr != null ) {
		      printMngr.putByte( value );
		    }
		  }
		  /*
		   * Drucker soll bei 2 MHz nach 1/100 Sekunde
		   * Bereitschaft melden
		   */
		  this.centrTStatesToAck = 20000;
		}
	      }
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

	case 0x10:				// IOSEL4
	  if( this.ramPixel != null ) {
	    this.modeGraph = false;
	    this.screenFrm.setScreenDirty( true );
	  }
	  break;
      }
    }
    z80AddressChanged( port );
  }


  public void writeMemByte( int addr, int value )
  {
    addr &= 0xFFFF;
    setMemByte( addr, value );
    /*
     * Wenn die zusaetztliche Grafikkarte aktiv ist
     * und der Adressbereich EC00-EFFF der Reihe nach beschrieben wird,
     * wird davon ausgegangen, dass wieder die Standardbildschirmausgabe
     * aktiv sein soll.
     * In dem Fall wird deshalb wieder zurueckgeschaltet.
     */
    if( (addr >= 0xEC00) && (addr < 0xF000) && this.graphCCJActive ) {
      if( addr == 0xEC00 ) {
	this.lastWrittenAddr = addr;
      } else if( addr == (this.lastWrittenAddr + 1) ) {
	this.graphCCJActive = false;
	this.screenFrm.fireScreenSizeChanged();
      } else {
	this.lastWrittenAddr = -1;
      }
    }
  }


	/* --- private Methoden --- */

  private void applyUserPortSettings( Properties props )
  {
    String text = EmuUtil.getProperty( props, "jkcemu.z1013.userport" );
    if( text.equals( "joystick:jute0687" ) ) {
      this.userPort = UserPort.JOY_JUTE_6_1987;
    } else if( text.equals( "joystick:practic0487" ) ) {
      this.userPort = UserPort.JOY_PRACTIC_4_1987;
    } else if( text.equals( "joystick:practic0188" ) ) {
      this.userPort = UserPort.JOY_PRACTIC_1_1988;
    } else if( text.equals( "centronics7:practic0289" ) ) {
      this.userPort = UserPort.CENTR7_PRACTIC_2_1989;
      this.pio.putInValuePortA( 0, 0x80 );	// Empfamgsbereitschaft
    } else if( text.equals( "centronics8:fa1090" ) ) {
      this.userPort = UserPort.CENTR8_FA_10_1990;
    } else {
      this.userPort = UserPort.NONE;
    }
  }


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
    this.catchPrintCalls = EmuUtil.getBooleanProperty(
					props,
					"jkcemu.z1013.catch_print_calls",
					true );
    if( this.catchPrintCalls ) {
      addrs.add( new Integer( 0xFFCA ) );
      addrs.add( new Integer( 0xFFCD ) );
      addrs.add( new Integer( 0xFFDF ) );
      addrs.add( new Integer( 0xFFE5 ) );
      addrs.add( new Integer( 0xFFE8 ) );
      addrs.add( new Integer( 0xFFEB ) );
    }
    this.catchJoyCalls = EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.catch_joystick_calls",
				true );
    if( this.catchJoyCalls ) {
      addrs.add( new Integer( 0xFFBB ) );
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


  private boolean emulatesFloppyDisk( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.floppydisk.enabled",
				false );
  }


  private static boolean emulatesGraphCCJ( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.graph_ccj.enabled",
				false );
  }


  private static boolean emulatesGraphic( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.graphic.enabled",
				false );
  }


  private boolean emulatesKCNet( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.kcnet.enabled",
				false );
  }


  private boolean emulatesModuleBasic( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.rom_basic.enabled",
				false );
  }


  private boolean emulatesModuleMegaROM( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.rom_mega.enabled",
				false );
  }


  private static boolean emulatesRTC( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.rtc.enabled",
				false );
  }


  private boolean emulatesUSB( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.z1013.vdip.enabled",
				false );
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


  private void loadFont( Properties props )
  {
    this.stdFontBytes = readFontByProperty(
					props,
					"jkcemu.z1013.font.file",
					0x1000 );
    if( this.stdFontBytes != null ) {
      if( this.stdFontBytes.length >= 0x1000 ) {
	this.altFontBytes = Arrays.copyOfRange(
					this.stdFontBytes,
					0x0800,
					0x1000 );
      } else {
	this.altFontBytes = this.stdFontBytes;
      }
    } else {
      if( fontStd == null ) {
	fontStd = readResource( "/rom/z1013/z1013font.bin" );
      }
      this.stdFontBytes = fontStd;
      if( fontAlt == null ) {
	fontAlt = readResource( "/rom/z1013/altfont.bin" );
      }
      this.altFontBytes = fontAlt;
    }
    if( this.graphCCJ != null ) {
      this.graphCCJ.loadFont(
		EmuUtil.getProperty(
				props,
				"jkcemu.z1013.graph_ccj.font.file" ) );
    }
  }


  private void loadROMs( Properties props )
  {
    this.monCode = EmuUtil.getProperty( props, "jkcemu.z1013.monitor" );
    this.osFile  = EmuUtil.getProperty( props, "jkcemu.z1013.os.file" );
    this.osBytes = readFile( this.osFile, 0x1000, "Monitorprogramm" );
    if( this.osBytes == null ) {
      if( this.monCode.equals( "A.2" ) ) {
	if( monA2 == null ) {
	  monA2 = readResource( "/rom/z1013/mon_a2.bin" );
	}
	this.osBytes = monA2;
      } else if( this.monCode.equals( "RB_K7659" ) ) {
	if( monRB_K7659 == null ) {
	  monRB_K7659 = readResource( "/rom/z1013/mon_rb_k7659.bin" );
	}
	this.osBytes = monRB_K7659;
      } else if( this.monCode.equals( "RB_S6009" ) ) {
	if( monRB_S6009 == null ) {
	  monRB_S6009 = readResource( "/rom/z1013/mon_rb_s6009.bin" );
	}
	this.osBytes = monRB_S6009;
      } else if( this.monCode.equals( "INCOM_K7669" ) ) {
	if( monINCOM_K7669 == null ) {
	  monINCOM_K7669 = readResource( "/rom/z1013/mon_incom_k7669.bin" );
	}
	this.osBytes = monINCOM_K7669;
      } else if( this.monCode.equals( "JM_1992" ) ) {
	if( monJM_1992 == null ) {
	  monJM_1992 = readResource( "/rom/z1013/mon_jm_1992.bin" );
	}
	this.osBytes = monJM_1992;
      } else if( this.monCode.equals( "BL4_K7659" ) ) {
	if( bl4_K7659 == null ) {
	  bl4_K7659 = readResource( "/rom/z1013/bl4_k7659.bin" );
	}
	this.osBytes = bl4_K7659;
      } else {
	if( mon202 == null ) {
	  mon202 = readResource( "/rom/z1013/mon_202.bin" );
	}
	this.osBytes = mon202;
      }
    }
    if( emulatesModuleBasic( props ) ) {
      this.romBasicFile = EmuUtil.getProperty(
					props,
					"jkcemu.z1013.rom_basic.file" );
      this.romBasic = readFile( this.romBasicFile, 0x2C00, "KC-BASIC-Modul" );
      if( this.romBasic == null ) {
	if( modBasic == null ) {
	  modBasic = readResource( "/rom/z1013/kcbasic.bin" );
	}
	this.romBasic = modBasic;
      }
    } else if( emulatesModuleMegaROM( props ) ) {
      this.romMegaFile = EmuUtil.getProperty(
					props,
					"jkcemu.z1013.rom_mega.file" );
      this.romMega = readFile( this.romMegaFile, 0x280000, "Mega-ROM-Modul" );
    }
    loadFont( props );
  }


  /*
   * Joystick-Anschluss nach Ju+Te 6/1987 (alle L-aktiv):
   *   Bit 4:   runter
   *   Bit 5:   links
   *   Bit 6:   rechts
   *   Bit 7:   hoch
   *
   *   Die Bits 0 bis 3 koennen fuer einen weiteren Joystick verwendet werden.
   *   Allerdings ist in dem Ju+Te-Heft eine Tabelle mit den Dezimalwerten
   *   angegeben, die bei nur einem Joystick gelesen werden.
   *   Demnach ist Bit 0 immer 1 (als Grund wird ein angeschlossener
   *   Piezosummer genannt), und die Bits 1 bis 3 sind immer 0.
   *   Um auch dieser Tabelle gerecht zu werden,
   *   wird hier nur ein Joystick emuliert und die Bits 0 bis 3 entsprechend
   *   den Werten der Tabelle gesetzt.
   *
   *   Zu den Aktionsknoepfen steht nur,
   *   dass mehrere Leitungen gleichzeitig selektiert werden sollen,
   *   die mit dem Hebel nicht gleichzeitig ausgeloest werden koennen,
   *   also z.B. rechts+links, oben+unten oder alle vier Richtungen.
   *   Es wird deshalb hier adaequat zum Joystick-Anschluss
   *   nach practic 4/1987 vorgegangen:
   *   Der Aktionsknopf waehlt alle vier Richtungen gleichzeitig aus.
   */
  private void putJoyJuTe0687ValuesToPort()
  {
    int value = 0xF1;
    if( (this.joy0ActionMask & JoystickThread.BUTTONS_MASK) != 0 ) {
      value ^= 0xF0;
    } else {
      if( (this.joy0ActionMask & JoystickThread.LEFT_MASK) != 0 ) {
	value ^= 0x20;
      }
      if( (this.joy0ActionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	value ^= 0x40;
      }
      if( (this.joy0ActionMask & JoystickThread.UP_MASK) != 0 ) {
	value ^= 0x80;
      }
      if( (this.joy0ActionMask & JoystickThread.DOWN_MASK) != 0 ) {
	value ^= 0x10;
      }
    }
    this.pio.putInValuePortA( value, 0xFF );
  }


  private void putJoyPractic0487ValuesToPort()
  {
    int value = 0;
    if( (this.joy0ActionMask & JoystickThread.BUTTONS_MASK) != 0 ) {
      value |= 0xF0;
    } else {
      if( (this.joy0ActionMask & JoystickThread.LEFT_MASK) != 0 ) {
	value |= 0x20;
      }
      if( (this.joy0ActionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	value |= 0x80;
      }
      if( (this.joy0ActionMask & JoystickThread.UP_MASK) != 0 ) {
	value |= 0x10;
      }
      if( (this.joy0ActionMask & JoystickThread.DOWN_MASK) != 0 ) {
	value |= 0x40;
      }
    }
    if( (this.joy1ActionMask & JoystickThread.BUTTONS_MASK) != 0 ) {
      value |= 0x0F;
    } else {
      if( (this.joy1ActionMask & JoystickThread.LEFT_MASK) != 0 ) {
	value |= 0x02;
      }
      if( (this.joy1ActionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	value |= 0x08;
      }
      if( (this.joy1ActionMask & JoystickThread.UP_MASK) != 0 ) {
	value |= 0x01;
      }
      if( (this.joy1ActionMask & JoystickThread.DOWN_MASK) != 0 ) {
	value |= 0x04;
      }
    }
    this.pio.putInValuePortA( ~value, 0xFF );
  }


  private void putJoyPractic0188ValuesToPort()
  {
    int value      = 0xFF;
    int joySel     = ~this.pio.fetchOutValuePortA( false );
    int actionMask = 0;
    if( (joySel & 0x20) != 0 ) {		// Joystick 1 selektiert
      actionMask |= this.joy0ActionMask;
    }
    if( (joySel & 0x40) != 0 ) {		// Joystick 2 selektiert
      actionMask |= this.joy1ActionMask;
    }
    if( (actionMask & JoystickThread.LEFT_MASK) != 0 ) {
      value ^= 0x01;
    }
    if( (actionMask & JoystickThread.RIGHT_MASK) != 0 ) {
      value ^= 0x02;
    }
    if( (actionMask & JoystickThread.DOWN_MASK) != 0 ) {
      value ^= 0x04;
    }
    if( (actionMask & JoystickThread.UP_MASK) != 0 ) {
      value ^= 0x08;
    }
    if( (actionMask & JoystickThread.BUTTONS_MASK) != 0 ) {
      value ^= 0x10;
    }
    this.pio.putInValuePortA( value, 0xFF );
  }


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null ) {
      KeyboardMatrix km = this.keyboard.getKeyboardMatrix();
      if( km != null ) {
	km.updKeyboardFld( this.keyboardFld );
      }
    }
  }
}

