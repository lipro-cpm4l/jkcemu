/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Z9001 und Nachfolger (KC85/1, KC87)
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.event.*;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.joystick.JoystickThread;
import z80emu.*;


public class Z9001 extends EmuSys implements
					ActionListener,
					FDC8272.DriveSelector,
					Z80CTCListener,
					Z80MaxSpeedListener,
					Z80PCListener,
					Z80SIOChannelListener,
					Z80TStatesListener
{
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
    "PAPER",     "AT",       "PSET",    "LINE",		// 0xE0
    "CIRCLE",    "!",        "PAINT",   "LABEL",
    "SIZE",      "ZERO",     "HOME",    "!",
    "GCLS",      "SCALE",    "SCREEN",  "POINT",
    "XPOS",      "!",        "YPOS" };			// 0xF0

  private static final String[] biosCallNames = {
			"INIT",  "WBOOT", "CONST", "CONIN",
			"COOUT", "LIST",  "PUNCH", "READER",
			"GSTIK", "BOSER", "STIME", "GTIME",
			"SDMA",  "READ",  "WRITE", "LLIST",
			"GCURS", "SCURS", "BOSER", "GIOBY",
			"SIOBY", "GMEM",  "SMEM" };

  private static final int basicRGBValues[][] = {
				{ 0,   0,   0 },
				{ 255, 0,   0 },
				{ 0,   255, 0 },
				{ 255, 255, 0 },
				{ 0,   0,   255 },
				{ 255, 0,   255 },
				{ 0,   255, 255 },
				{ 255, 255, 255 } };

  private static final int[][] kbMatrixNormal = {
		{ '0', '1', '2', '3', '4', '5', '6', '7' },
		{ '8', '9', ':', ';', ',', '=', '.', '?' },
		{ '@', 'A', 'B',  'C', 'D', 'E', 'F', 'G'  },
		{ 'H', 'I', 'J',  'K', 'L', 'M', 'N', 'O'  },
		{ 'P', 'Q', 'R',  'S', 'T', 'U', 'V', 'W'  },
		{ 'X', 'Y', 'Z',  0,   0,   0,   '^', 0    } };

  private static final int[][] kbMatrixShift = {
		{ '_', '!', '\"', '#', '$', '%', '&', '\'' },
		{ '(', ')', '*',  '+', '<', '-', '>', '/'  },
		{ 0,   'a', 'b', 'c', 'd', 'e', 'f', 'g' },
		{ 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o' },
		{ 'p', 'q', 'r', 's', 't', 'u', 'v', 'w' },
		{ 'x', 'y', 'z', 0,   0,   0  , 0, 0   } };

  private static final int GRAPHIC_NONE     = 0;
  private static final int GRAPHIC_ROBOTRON = 1;
  private static final int GRAPHIC_KRT      = 2;

  private static final FloppyDiskInfo[] availableFloppyDisks = {
		new FloppyDiskInfo(
			"/disks/z9001/z9cpasys.dump.gz",
			"Z9001 CP/A Systemdiskette" ) };

  private static byte[] os11            = null;
  private static byte[] os12            = null;
  private static byte[] os13            = null;
  private static byte[] basic86         = null;
  private static byte[] bootROMBytes    = null;
  private static byte[] megaROMBytes    = null;
  private static byte[] printerModBytes = null;
  private static byte[] z9001FontBytes  = null;

  private byte[]            fontBytes;
  private byte[]            romOS;
  private byte[]            romBasic;
  private byte[]            romBoot;
  private byte[]            romMega;
  private byte[]            rom16k4000;
  private byte[]            rom32k4000;
  private byte[]            rom16k8000;
  private byte[]            rom10kC000;
  private byte[]            ramFont;
  private byte[]            ramColor;
  private byte[]            ramColor2;
  private byte[]            ramVideo;
  private byte[]            ramVideo2;
  private byte[]            ramPixel;
  private byte[]            ramExt;
  private boolean           ram16k4000;
  private boolean           ram16k8000;
  private boolean           ram64k;
  private boolean           romModuleEnabled;
  private int               megaROMSeg;
  private int               fontOffs;
  private int               graphType;
  private int               graphBank;
  private int               graphAddrL;
  private int               graphBgColor;
  private int               graphFgColor;
  private boolean           graphBorder;
  private boolean           graphMode;
  private boolean           kc87;
  private boolean           pasteFast;
  private boolean           printerModule;
  private boolean           pcListenerAdded;
  private boolean           mode20Rows;
  private boolean           c80Active;
  private boolean           c80Enabled;
  private boolean           c80MemSwap;
  private boolean           colorSwap;
  private boolean           fdcReset;
  private boolean           fdcTC;
  private boolean           rf1ReadOnly;
  private boolean           rf2ReadOnly;
  private boolean           ram4000ExtEnabled;
  private boolean           ramC000Enabled;
  private boolean           ramFontActive;
  private boolean           ramFontEnabled;
  private boolean           audioOutPhase;
  private boolean           audioInPhase;
  private int               audioInTStates;
  private int               lineNum;
  private int               lineTStates;
  private int               tStatesPerLine;
  private int               tStatesVisible;
  private int               borderColorIdx;
  private int               joy0ActionMask;
  private int               joy1ActionMask;
  private int[]             kbMatrix;
  private String            sysName;
  private String            propPrefix;
  private String            romOSFile;
  private String            romBasicFile;
  private String            romModuleFile;
  private RAMFloppy         ramFloppy1;
  private RAMFloppy         ramFloppy2;
  private Z80PIO            pio90;
  private Z80PIO            pio88;
  private Z80CTC            ctc80;
  private Z80CTC            ctcA8;		// CTC im Druckermodul
  private Z80SIO            sioB0;		// SIO im Druckermodul
  private FDC8272           fdc;
  private javax.swing.Timer blinkTimer;
  private Color[]           colors;
  private FloppyDiskDrive[] floppyDiskDrives;


  public Z9001( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.romOS         = null;
    this.romOSFile     = null;
    this.romBasic      = null;
    this.romBasicFile  = null;
    this.rom16k4000    = null;
    this.rom32k4000    = null;
    this.rom16k8000    = null;
    this.rom10kC000    = null;
    this.romBoot       = null;
    this.romMega       = null;
    this.romModuleFile = null;
    this.ram16k4000    = false;
    this.ram16k8000    = false;
    this.ram64k        = false;

    this.sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( this.sysName.equals( "KC87" ) ) {
      this.kc87       = true;
      this.propPrefix = "jkcemu.kc87.";
    } else if( this.sysName.equals( "KC85/1" ) ) {
      this.kc87       = false;
      this.propPrefix = "jkcemu.kc85_1.";
    } else {
      this.kc87       = false;
      this.propPrefix = "jkcemu.z9001.";
    }

    if( emulatesFloppyDisk( props ) ) {
      this.floppyDiskDrives = new FloppyDiskDrive[ 2 ];
      Arrays.fill( this.floppyDiskDrives, null );
      this.fdc = new FDC8272( this, 4 );
    } else {
      this.floppyDiskDrives = null;
      this.fdc              = null;
    }
    this.printerModule = emulatesPrinterModule( props );
    if( this.printerModule ) {
      if( printerModBytes == null ) {
	printerModBytes = readResource( "/rom/z9001/modprinter.bin" );
      }
    }
    this.c80Enabled = emulates80CharsMode( props );
    this.c80Active  = false;
    this.ramVideo   = new byte[ 0x0400 ];
    this.ramVideo2  = (this.c80Enabled ? new byte[ 0x0400 ] : null);
    this.ram16k4000 = emulatesRAM16K4000( props );
    this.ram16k8000 = emulatesRAM16K8000( props );
    this.ram64k     = emulatesRAM64K( props );
    this.ramExt     = null;
    if( this.ram64k ) {
      this.ramExt = this.emuThread.getExtendedRAM( 0x4000 );
    }

    this.ramFloppy1 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy1(),
				this.sysName,
				RAMFloppy.RFType.ADW,
				"RAM-Floppy an IO-Adressen 20h/21h",
				props,
				this.propPrefix + "ramfloppy.1." );

    this.ramFloppy2 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy2(),
				this.sysName,
				RAMFloppy.RFType.ADW,
				"RAM-Floppy an IO-Adressen 24h/25h",
				props,
				this.propPrefix + "ramfloppy.2." );

    this.lineNum         = 0;
    this.lineTStates     = 0;
    this.audioInTStates  = 0;
    this.audioInPhase    = this.emuThread.readAudioPhase();
    this.audioOutPhase   = false;
    this.pcListenerAdded = false;
    this.mode20Rows      = false;
    this.colorSwap       = false;
    this.borderColorIdx  = 0;
    this.colors          = new Color[ basicRGBValues.length ];
    createColors( props );
    applyPasteFast( props );

    this.kbMatrix = new int[ 8 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.pio90 = new Z80PIO( "Tastatur-PIO (IO-Adressen 90-93)" );
    this.pio88 = new Z80PIO( "System-PIO (IO-Adressen 88-8B)" );
    this.ctc80 = new Z80CTC( "System-CTC (IO-Adressen 80-83)" );
    if( this.printerModule ) {
      this.ctcA8 = new Z80CTC( "Druckermodul-CTC (IO-Adressen A8-AB)" );
      this.sioB0 = new Z80SIO( "Druckermodul-SIO (IO-Adressen B0-B3)" );
      cpu.setInterruptSources(
		this.pio90, this.pio88, this.ctc80, this.ctcA8, this.sioB0 );
      this.sioB0.addChannelListener( this, 0 );
    } else {
      this.ctcA8 = null;
      this.sioB0 = null;
      cpu.setInterruptSources( this.pio90, this.pio88, this.ctc80 );
    }
    this.ctc80.setTimerConnection( 2, 3 );
    this.ctc80.addCTCListener( this );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );
    checkAddPCListener( props );

    this.ramPixel  = null;
    this.graphType = getGraphicType( props );
    if( this.graphType == GRAPHIC_ROBOTRON ) {
      this.ramPixel = new byte[ 0x1800 ];
    } else if( this.graphType == GRAPHIC_KRT ) {
      this.ramPixel = new byte[ 0x2000 ];
    }

    if( getColorMode( props ) ) {
      this.ramColor   = new byte[ 0x0400 ];
      this.ramColor2  = (this.c80Enabled ? new byte[ 0x0400 ] : null);
      this.blinkTimer = new javax.swing.Timer( 200, this );
      this.blinkTimer.start();
    } else {
      this.ramColor   = null;
      this.ramColor2  = null;
      this.blinkTimer = null;
    }

    if( emulatesProgrammableFont( props ) ) {
      this.ramFont = new byte[ 0x0400 ];
    } else {
      this.ramFont = null;
    }

    z80MaxSpeedChanged( cpu );
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static FloppyDiskInfo[] getAvailableFloppyDisks()
  {
    return availableFloppyDisks;
  }


  public static int getDefaultSpeedKHz()
  {
    return 2458;	// eigentlich 2,4576 MHz
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    if( (e.getSource() == this.blinkTimer)
	&& !this.emuThread.getZ80CPU().isPause() )
    {
      this.colorSwap = !this.colorSwap;
      this.screenFrm.setScreenDirty( true );
    }
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


	/* --- Z80CTCListener --- */

  @Override
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( (ctc == this.ctc80) && (timerNum == 0) ) {
      this.audioOutPhase = !this.audioOutPhase;
      if( this.emuThread.isSoundOutEnabled() ) {
	if( (this.pio88.fetchOutValuePortA( false ) & 0x80) != 0 ) {
	  this.emuThread.writeAudioPhase( this.audioOutPhase );
	}
      } else {
	this.emuThread.writeAudioPhase( this.audioOutPhase );
      }
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    int maxSpeedKHz = cpu.getMaxSpeedKHz();
    this.tStatesPerLine = maxSpeedKHz * 20 / 312;
    this.tStatesVisible = (int) Math.round( this.tStatesPerLine / 2 );
    if( this.fdc != null ) {
      this.fdc.setTStatesPerMilli( maxSpeedKHz );
    }
  }


	/* --- Z80PCListener --- */

  @Override
  public void z80PCChanged( Z80CPU cpu, int pc )
  {
    if( (pc == 0x0005) && (cpu.getRegC() == 5) ) {
      this.emuThread.getPrintMngr().putByte( cpu.getRegE() );
      cpu.setFlagCarry( false );
      cpu.setRegPC( cpu.doPop() );
    }
  }


	/* --- Z80SIOChannelListener --- */

  @Override
  public void z80SIOChannelByteAvailable( Z80SIO sio, int channel, int value )
  {
    if( this.printerModule && (sio == this.sioB0) && (channel == 0) )
      this.emuThread.getPrintMngr().putByte( value );
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc80.z80TStatesProcessed( cpu, tStates );
    if( this.ctcA8 != null ) {
      this.ctcA8.z80TStatesProcessed( cpu, tStates );
    }
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
    this.audioInTStates += tStates;

    /*
     * Der Kassettenrecorderanschluss eingangsseitig wird emuliert,
     * indem zyklisch geschaut wird, ob sich die Eingangsphase geaendert hat.
     * Wenn ja, wird ein Impuls an der Strobe-Leitung der zugehoerigen PIO
     * emuliert.
     * Auf der einen Seite soll das Audiosystem nicht zu oft abgefragt
     * werden.
     * Auf der anderen Seite sollen aber die Zykluszeit nicht so gross werden,
     * dass die Genauigkeit der Zeitmessung kuenstlich verschlechert wird.
     * Aus diesem Grund werden genau soviele Taktzyklen abgezaehlt,
     * wie auch der Vorteile der CTC mindestens zaehlen muss.
     */
    if( this.audioInTStates > 15 ) {
      this.audioInTStates = 0;
      if( this.emuThread.readAudioPhase() != this.audioInPhase ) {
	this.audioInPhase = !this.audioInPhase;
	this.pio88.strobePortA();
      }
    }

    // Zugriffe auf den Bildwiederhol- und Farbspeicher verlangsamen
    if( (this.tStatesPerLine > 0) && (this.tStatesVisible > 0) ) {
      this.lineTStates += tStates;
      if( this.lineTStates >= this.tStatesPerLine ) {
	this.lineTStates %= this.tStatesPerLine;
	if( this.lineNum < 311 ) {
	  this.lineNum++;
	} else {
	  this.lineNum = 0;
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
    createColors( props );
    loadFont( props );
    applyPasteFast( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = this.sysName.equals(
			EmuUtil.getProperty( props, "jkcemu.system" ) );
    if( rv ) {
      if( emulatesRAM16K4000( props ) != this.ram16k4000 ) {
	rv = false;
      }
    }
    if( rv ) {
      if( emulatesRAM16K8000( props ) != this.ram16k8000 ) {
	rv = false;
      }
    }
    if( rv ) {
      if( emulatesRAM64K( props ) != this.ram64k ) {
	rv = false;
      }
    }
    if( rv ) {
      rv = EmuUtil.equals(
		this.romOSFile,
		EmuUtil.getProperty( props,  propPrefix + "os.file" ) );
    }
    if( rv && this.kc87 ) {
      rv = EmuUtil.equals(
		this.romBasicFile,
		EmuUtil.getProperty( props,  propPrefix + "basic.file" ) );
    }
    if( rv ) {
      rv = equalsROMModule(
		this.rom16k4000,
		this.romModuleFile,
		props,
		this.propPrefix + "rom_16k_4000.enabled",
		this.propPrefix + "rom_module.file" );
    }
    if( rv ) {
      rv = equalsROMModule(
		this.rom32k4000,
		this.romModuleFile,
		props,
		this.propPrefix + "rom_32k_4000.enabled",
		this.propPrefix + "rom_module.file" );
    }
    if( rv ) {
      rv = equalsROMModule(
		this.rom16k8000,
		this.romModuleFile,
		props,
		this.propPrefix + "rom_16k_8000.enabled",
		this.propPrefix + "rom_module.file" );
    }
    if( rv ) {
      rv = equalsROMModule(
		this.rom10kC000,
		this.romModuleFile,
		props,
		this.propPrefix + "rom_10k_c000.enabled",
		this.propPrefix + "rom_module.file" );
    }
    if( rv ) {
      rv = equalsROMModule(
		this.romBoot,
		this.romModuleFile,
		props,
		this.propPrefix + "rom_boot.enabled",
		this.propPrefix + "rom_module.file" );
    }
    if( rv ) {
      rv = equalsROMModule(
		this.romMega,
		this.romModuleFile,
		props,
		this.propPrefix + "rom_mega.enabled",
		this.propPrefix + "rom_module.file" );
    }
    if( rv ) {
      if( emulatesFloppyDisk( props ) != (this.fdc != null) ) {
	rv = false;
      }
    }
    if( rv ) {
      if( emulatesPrinterModule( props ) != this.printerModule ) {
	rv = false;
      }
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy1,
			this.sysName,
			RAMFloppy.RFType.ADW,
			props,
			this.propPrefix + "ramfloppy.1." );
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy2,
			this.sysName,
			RAMFloppy.RFType.ADW,
			props,
			this.propPrefix + "ramfloppy.2." );
    }
    if( rv ) {
      if( getColorMode( props ) != (this.ramColor != null) ) {
	rv = false;
      }
    }
    if( rv ) {
      if( getGraphicType( props ) != this.graphType ) {
	rv = false;
      }
    }
    if( rv ) {
      if( emulatesProgrammableFont( props ) != (this.ramFont != null) ) {
	rv = false;
      }
    }
    if( rv ) {
      if( emulates80CharsMode( props ) != this.c80Enabled ) {
	rv = false;
      }
    }
    return rv;
  }


  @Override
  public boolean canExtractScreenText()
  {
    return !this.graphMode;
  }


  @Override
  public void die()
  {
    if( this.blinkTimer != null ) {
      this.blinkTimer.stop();
    }
    if( this.sioB0 != null ) {
      this.sioB0.removeChannelListener( this, 0 );
    }
    this.ctc80.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeMaxSpeedListener( this );
    cpu.removeTStatesListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.pcListenerAdded ) {
      cpu.removePCListener( this );
    }
    if( this.ramFloppy1 != null ) {
      this.ramFloppy1.deinstall();
    }
    if( this.ramFloppy2 != null ) {
      this.ramFloppy2.deinstall();
    }
  }


  @Override
  public int getBorderColorIndex()
  {
    return this.borderColorIdx;
  }


  @Override
  public int getCharColCount()
  {
    return this.graphMode ? -1 : (this.c80Active ? 80 : 40);
  }


  @Override
  public int getCharHeight()
  {
    return this.graphMode ? -1 : 8;
  }


  @Override
  public int getCharRowCount()
  {
    return this.graphMode ? -1 : (this.mode20Rows ? 20 : 24);
  }


  @Override
  public int getCharRowHeight()
  {
    return this.graphMode ? -1 : (this.mode20Rows ? 9 : 8);
  }


  @Override
  public int getCharWidth()
  {
    return this.graphMode ? -1 : 8;
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    if( this.ramColor != null ) {
      if( (colorIdx >= 0) && (colorIdx < colors.length) ) {
	color = colors[ colorIdx ];
      }
    } else {
      if( colorIdx > 0 ) {
	color = Color.white;
      }
    }
    return color;
  }


  @Override
  public int getColorCount()
  {
    return this.ramColor != null ? 8 : 2;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = 0;
    if( this.graphMode
	&& (this.graphType == GRAPHIC_ROBOTRON)
	&& (this.ramPixel != null) )
    {
      boolean done = false;
      x -= 32;		// Grafikausgabe ueber Alpha-Ausgabe zentrieren
      if( (x >= 0) && (x < 256) ) {
	int col = x / 8;
	int idx = (y * 32) + col;
	if( (idx >= 0) && (idx < this.ramPixel.length) ) {
	  int m = 0x80;
	  int n = x % 8;
	  if( n > 0 ) {
	    m >>= n;
	  }
	  byte b = this.ramPixel[ idx ];
	  rv   = ((int) b & m) != 0 ? this.graphFgColor : this.graphBgColor;
	  done = true;
	}
      }
      if( !done && this.graphBorder ) {
	rv = this.borderColorIdx;
      }
    } else {
      int    b    = 0;
      int    col  = x / 8;
      byte[] vram = this.ramVideo;
      if( this.c80Active && (this.ramVideo2 != null) ) {
	if( (col & 0x01) != 0 ) {
	  vram = this.ramVideo2;
	}
	col /= 2;
      }
      int row  = 0;
      int yChr = 0;
      int rMax = 0;
      if( this.mode20Rows ) {
	row  = y / 9;
	yChr = y % 9;
	rMax = 20;
      } else {
	row  = y / 8;
	yChr = y % 8;
	rMax = 24;
      }
      if( (yChr < 8) && (row < rMax) ) {
	int offs = (row * 40) + col;
	if( this.graphMode
	    && (this.graphType == GRAPHIC_KRT)
	    && (this.ramPixel != null) )
	{
	  int idx = (yChr * 0x0400) + offs;
	  if( (idx >= 0) && (idx < this.ramPixel.length) ) {
	    b = this.ramPixel[ idx ];
	  }
	} else {
	  if( this.fontBytes != null ) {
	    if( (offs >= 0) && (offs < vram.length) ) {
	      int ch  = (int) vram[ offs ] & 0xFF;
	      int idx = (ch * 8) + yChr;
	      if( this.ramFontActive
		  && (this.ramFont != null)
		  && (ch >= 0x80) )
	      {
		idx -= 0x0400;
		if( (idx >= 0) && (idx < this.ramFont.length ) ) {
		  b = this.ramFont[ idx ];
		}
	      } else {
		idx += this.fontOffs;
		if( (idx >= 0) && (idx < this.fontBytes.length ) ) {
		  b = this.fontBytes[ idx ];
		}
	      }
	    }
	  }
	}
	int m = 0x80;
	int n = x % 8;
	if( n > 0 ) {
	  m >>= n;
	}
	if( (b & m) != 0 ) {
	  rv = 1;
	}
	if( this.ramColor != null ) {
	  byte[] cram = this.ramColor;
	  if( (this.ramColor2 != null) && (vram == this.ramVideo2) ) {
	    cram = this.ramColor2;
	  }
	  int colorInfo = 0;
	  if( (offs >= 0) && (offs < cram.length) ) {
	    colorInfo = (int) cram[ offs ] & 0xFF;
	  }
	  if( ((colorInfo & 0x80) != 0) && this.colorSwap ) {
	    rv = (rv != 0 ? 0 : 1);
	  }
	  if( rv != 0 ) {
	    rv = (colorInfo >> 4) & 0x07;
	  } else {
	    rv = colorInfo & 0x07;
	  }
	}
      } else {
	if( this.ramColor != null ) {
	  rv = this.borderColorIdx;
	}
      }
    }
    return rv;
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
    return this.fdc != null ? 3 : -1;
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
    return this.fdc != null ? 0 : -1;
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return this.pasteFast ? 0 : 100;
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return this.pasteFast ? 0 : 200;
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return this.pasteFast ? 0 : 60;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/z9001.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    return getMemByteInternal( addr, false );
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
    if( !this.graphMode ) {
      byte[] vram = this.ramVideo;
      if( this.c80Active && (this.ramVideo2 != null) ) {
	if( (chX & 0x01) != 0 ) {
	  vram = this.ramVideo2;
	}
	chX /= 2;
      }
      int idx = (chY * 40) + chX;
      if( (idx >= 0) && (idx < vram.length) ) {
	int b = (int) vram[ idx ] & 0xFF;
	if( this.fontOffs > 0 ) {
	  switch( b ) {
	    case 0x0B:			// Ae
	      ch = '\u00C4';
	      break;
	    case 0x0C:			// Oe
	      ch = '\u00D6';
	      break;
	    case 0x0D:			// Ue
	      ch = '\u00DC';
	      break;
	    case 0x1B:			// ae
	      ch = '\u00E4';
	      break;
	    case 0x1C:			// oe
	      ch = '\u00F6';
	      break;
	    case 0x1D:			// ue
	      ch = '\u00FC';
	      break;
	    case 0x1E:			// sz
	      ch = '\u00DF';
	      break;
	  }
	}
	if( (ch < 0) && (b >= 0x20) && (b < 0x7F) ) {
	  ch = b;
	}
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return 192;
  }


  @Override
  public int getScreenWidth()
  {
    return this.c80Active && !this.graphMode ? 640 : 320;
  }


  @Override
  public FloppyDiskInfo[] getSuitableFloppyDisks()
  {
    return this.fdc != null ? availableFloppyDisks : null;
  }


  @Override
  public int getSupportedFloppyDiskDriveCount()
  {
    return this.floppyDiskDrives != null ? this.floppyDiskDrives.length : 0;
  }


  @Override
  public int getSupportedJoystickCount()
  {
    return 2;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  @Override
  public String getTitle()
  {
    return this.sysName;
  }


  @Override
  public boolean hasKCBasicInROM()
  {
    return this.romBasic != null;
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
	case KeyEvent.VK_BACK_SPACE:
	  this.kbMatrix[ 0 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_LEFT:
	  if( ctrlDown ) {
	    this.kbMatrix[ 3 ] = 0x20;		// Zeilenanfang
	  } else {
	    if( shiftDown ) {
	      this.kbMatrix[ 0 ] = 0x80;	// Shift
	    }
	    this.kbMatrix[ 0 ] = 0x40;		// Links
	  }
	  rv = true;
	  break;

	case KeyEvent.VK_RIGHT:
	  if( ctrlDown ) {
	    this.kbMatrix[ 3 ] = 0x20;		// Zeilenende
	    this.kbMatrix[ 0 ] = 0x80;		// (Shift+Zeilenanfang)
	  } else {
	    if( shiftDown ) {
	      this.kbMatrix[ 0 ] = 0x80;	// Shift
	    }
	    this.kbMatrix[ 1 ] = 0x40;		// Rechts
	  }
	  rv = true;
	  break;

	case KeyEvent.VK_DOWN:
	  if( shiftDown ) {
  	    this.kbMatrix[ 0 ] = 0x80;		// Shift
	  }
	  this.kbMatrix[ 2 ] = 0x40;		// Runter
	  rv = true;
	  break;

	case KeyEvent.VK_UP:
	  if( shiftDown ) {
  	    this.kbMatrix[ 0 ] = 0x80;		// Shift
	  }
	  this.kbMatrix[ 3 ] = 0x40;		// Hoch
	  rv = true;
	  break;

	case KeyEvent.VK_ESCAPE:
	  if( shiftDown ) {
	    this.kbMatrix[ 0 ] = 0x80;		// CR LN (Shift+ESC)
	  }
	  this.kbMatrix[ 4 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_ENTER:
	  this.kbMatrix[ 5 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_SPACE:
	  this.kbMatrix[ 7 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_INSERT:
	  this.kbMatrix[ 5 ] = 0x20;
	  rv = true;
	  break;

	case KeyEvent.VK_DELETE:		// DELETE (Shift-INSERT)
	  this.kbMatrix[ 0 ] = 0x80;		// Shift
	  this.kbMatrix[ 5 ] = 0x20;		// INSERT
	  rv = true;
	  break;

	case KeyEvent.VK_F1:			// LIST
	  this.kbMatrix[ 4 ] = 0x80;
	  rv = true;
	  break;

	case KeyEvent.VK_F2:			// RUN
	  this.kbMatrix[ 5 ] = 0x80;
	  rv = true;
	  break;

	case KeyEvent.VK_F3:			// STOP
	  this.kbMatrix[ 6 ] = 0x40;
	  rv = true;
	  break;

	case KeyEvent.VK_PAUSE:			// PAUSE
	case KeyEvent.VK_F4:
	  this.kbMatrix[ 4 ] = 0x20;
	  rv = true;
	  break;

	case KeyEvent.VK_F5:			// CONT (Shift-PAUSE)
	  this.kbMatrix[ 0 ] = 0x80;		// Shift
	  this.kbMatrix[ 4 ] = 0x20;		// PAUSE
	  rv = true;
	  break;

	case KeyEvent.VK_F6:			// CR LN (Shift-ESC)
	  this.kbMatrix[ 0 ] = 0x80;		// Shift
	  this.kbMatrix[ 4 ] = 0x40;		// ESC
	  rv = true;
	  break;

	case KeyEvent.VK_F7:			// COLOR
	  this.kbMatrix[ 1 ] = 0x80;
	  rv = true;
	  break;

	case KeyEvent.VK_F8:			// GRAPHIC
	  this.kbMatrix[ 3 ] = 0x80;
	  rv = true;
	  break;
      }
    }
    if( rv ) {
      putKeyboardMatrixValuesToPorts();
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    synchronized( this.kbMatrix ) {
      Arrays.fill( this.kbMatrix, 0 );
    }
    putKeyboardMatrixValuesToPorts();
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    synchronized( this.kbMatrix ) {
      if( (ch >= 1) && (ch <='\u0020') ) {
	if( setCharInKBMatrix( ch + 0x40, kbMatrixNormal ) ) {
	  this.kbMatrix[ 2 ] |= 0x80;		// Control
	  rv = true;
	}
	else if( setCharInKBMatrix( ch + 0x40, kbMatrixShift ) ) {
	  this.kbMatrix[ 2 ] |= 0x80;		// Control
	  rv = true;
	}
      } else {
	if( setCharInKBMatrix( ch, kbMatrixNormal ) ) {
	  rv = true;
	} else if( setCharInKBMatrix( ch, kbMatrixShift ) ) {
	  this.kbMatrix[ 0 ] |= 0x80;		// Shift
	  rv = true;
	}
      }
    }
    if( rv ) {
      putKeyboardMatrixValuesToPorts();
    }
    return rv;
  }


  @Override
  public void openBasicProgram()
  {
    SourceUtil.openKCBasicStyleProgram(
				this.screenFrm,
				this.kc87 ? 0x0401 : 0x2C01,
				basicTokens );
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
	  while( getMemByte( 0x0025, false ) != 0 ) {
	    Thread.sleep( 10 );
	  }
	  setMemByte( 0x0024, ch );
	  setMemByte( 0x0025, ch );
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
    int rv = 0xFF;
    switch( port & 0xFF ) {
      case 4:
	this.ram4000ExtEnabled = false;
	break;

      case 5:
	if( this.ramExt != null ) {
	  this.ram4000ExtEnabled = true;
	}
	break;

      case 6:
	this.ramC000Enabled = false;
	break;

      case 7:
	if( this.ramExt != null ) {
	  this.ramC000Enabled = true;
	}
	break;

      case 0x10:
      case 0x98:
      case 0x9A:
      case 0x9C:
      case 0x9E:
	if( this.fdc != null ) {
	  rv = this.fdc.readMainStatusReg();
	}
	break;

      case 0x11:
      case 0x99:
      case 0x9B:
      case 0x9D:
      case 0x9F:
	if( this.fdc != null ) {
	  rv = this.fdc.readData();
	}
	break;

      case 0x12:	// Terminal Count bei Rossendorf Floppy Disk Modul
      case 0x13:
        if( this.fdc != null ) {
          this.fdc.fireTC();
        }
        break;

      case 0x20:
	if( this.ramFloppy1 != null ) {
	  rv = this.ramFloppy1.readByte( port );
	}
	break;

      case 0x24:
	if( this.ramFloppy2 != null ) {
	  rv = this.ramFloppy2.readByte( port );
	}
	break;

      case 0x80:				// A2=0
      case 0x81:
      case 0x82:
      case 0x83:
      case 0x84:				// A2=1
      case 0x85:
      case 0x86:
      case 0x87:
	rv = this.ctc80.read( port & 0x03 );
	break;

      case 0x88:				// A2=0
      case 0x8C:				// A2=1
	rv = this.pio88.readPortA();
	break;

      case 0x89:				// A2=0
      case 0x8D:				// A2=1
	rv = this.pio88.readPortB();
	break;

      case 0x8A:				// A2=0
      case 0x8E:				// A2=1
	rv = this.pio88.readControlA();
	break;

      case 0x8B:				// A2=0
      case 0x8F:				// A2=1
	rv = this.pio88.readControlB();
	break;

      case 0x90:				// A2=0
      case 0x94:				// A2=1
	rv = this.pio90.readPortA();
	break;

      case 0x91:				// A2=0
      case 0x95:				// A2=1
	rv = this.pio90.readPortB();
	break;

      case 0x92:				// A2=0
      case 0x96:				// A2=1
	rv = this.pio90.readControlA();
	break;

      case 0x93:				// A2=0
      case 0x97:				// A2=1
	rv = this.pio90.readControlB();
	break;

      case 0xA8:				// A2=0
      case 0xA9:
      case 0xAA:
      case 0xAB:
      case 0xAC:				// A2=1
      case 0xAD:
      case 0xAE:
      case 0xAF:
	if( this.ctcA8 != null ) {
	  rv = this.ctcA8.read( port & 0x03 );
	}
	break;

      case 0xB0:				// A2=0
      case 0xB4:				// A2=1
	if( this.sioB0 != null ) {
	  rv = this.sioB0.readDataA();
	}
	break;

      case 0xB1:				// A2=0
      case 0xB5:				// A2=1
	if( this.sioB0 != null ) {
	  rv = this.sioB0.readDataB();
	}
	break;

      case 0xB2:				// A2=0
      case 0xB6:				// A2=1
	if( this.sioB0 != null ) {
	  rv = this.sioB0.readControlA();
	}
	break;

      case 0xB3:				// A2=0
      case 0xB7:				// A2=1
	if( this.sioB0 != null ) {
	  rv = this.sioB0.readControlB();
	}
	break;

      case 0xB8:
	if( this.ramPixel != null ) {
	  if( this.graphType == GRAPHIC_ROBOTRON ) {
	    rv = this.graphBgColor | (this.graphFgColor << 4);
	    if( this.graphMode ) {
	      rv |= 0x08;
	    }
	    if( this.graphBorder ) {
	      rv |= 0x40;
	    }
	  } else if( this.graphType == GRAPHIC_KRT ) {
	    rv = this.graphBank;
	    if( this.graphMode ) {
	      rv |= 0x08;
	    }
	  }
	}
	break;

      case 0xBA:
	if( (this.ramPixel != null)
	    && (this.graphType == GRAPHIC_ROBOTRON) )
	{
	  int addr = (port & 0xFF00) | this.graphAddrL;
	  if( (addr >= 0) && (addr < this.ramPixel.length) ) {
	    rv = (int) this.ramPixel[ addr ] & 0xFF;
	  }
	}
	break;
    }
    return rv;
  }


  @Override
  public int readMemByte( int addr, boolean m1 )
  {
    return getMemByteInternal( addr, true );
  }


  @Override
  public int reassembleSysCall(
			int           addr,
			StringBuilder buf,
			boolean       sourceOnly,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    return reassSysCallTable(
			addr,
			0xF000,
			biosCallNames,
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
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
      if( (this.romBoot != null) || (this.romMega != null) ) {
	/*
	 * Der originale DRAM enthaelt nach dem Einschalten
	 * abwechselnd die Bytes FF und 00.
	 * Wenn nur 00-Bytes vorhanden sind,
	 * wird der ROM-Modul nicht aktiviert.
	 * Aus diesem diesem Grund wird hier das originale Byte-Muster
	 * nachgebildet.
	 */
	for( int i = 0; i < 0x4000; i++ ) {
	  this.emuThread.setRAMByte( i, (i & 0x01) != 0 ? 0 : 0xFF );
	}
      }
      fillRandom( this.ramVideo );
      if( this.ramVideo2 != null ) {
	fillRandom( this.ramVideo2 );
      }
      if( this.ramColor != null ) {
	fillRandom( this.ramColor );
      }
      if( this.ramColor2 != null ) {
	fillRandom( this.ramColor2 );
      }
      if( this.ramPixel != null ) {
	fillRandom( this.ramPixel );
      }
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.ctc80.reset( true );
      this.pio88.reset( true );
      this.pio90.reset( true );
      if( this.ctcA8 != null ) {
	this.ctcA8.reset( true );
      }
    } else {
      this.ctc80.reset( false );
      this.pio88.reset( false );
      this.pio90.reset( false );
      if( this.ctcA8 != null ) {
	this.ctcA8.reset( false );
      }
    }
    if( this.sioB0 != null ) {
      this.sioB0.reset();
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
    if( this.blinkTimer != null ) {
      this.blinkTimer.restart();
    }
    Arrays.fill( this.kbMatrix, 0 );
    this.joy0ActionMask    = 0;
    this.joy1ActionMask    = 0;
    this.megaROMSeg        = 0;
    this.fontOffs          = 0;
    this.graphAddrL        = 0;
    this.graphBgColor      = 0;
    this.graphFgColor      = 0;
    this.graphBorder       = false;
    this.graphMode         = false;
    this.c80MemSwap        = false;
    this.fdcReset          = false;
    this.fdcTC             = false;
    this.rf1ReadOnly       = false;
    this.rf2ReadOnly       = false;
    this.ram4000ExtEnabled = false;
    this.ramC000Enabled    = false;
    this.ramFontActive     = false;
    this.ramFontEnabled    = false;
    this.romModuleEnabled  = ((this.romBoot != null)
					|| (this.romMega != null));
    upd80CharsMode( false );
    updScreenConfig( 0 );
  }


  @Override
  public void saveBasicProgram()
  {
    SourceUtil.saveKCBasicStyleProgram(
				this.screenFrm,
				this.kc87 ? 0x0401 : 0x2C01 );
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
  public void setJoystickAction( int joyNum, int actionMask )
  {
    if( joyNum == 0 ) {
      this.joy0ActionMask = actionMask;
    } else if( joyNum == 1 ) {
      this.joy1ActionMask = actionMask;
    }
    putKeyboardMatrixValuesToPorts();
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    return setMemByteInternal( addr, value, false );
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return this.fontBytes != z9001FontBytes;
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
    return this.printerModule || this.pcListenerAdded;
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
    switch( port & 0xFF ) {
      case 4:
	this.ram4000ExtEnabled = false;
	break;

      case 5:
	if( this.ramExt != null ) {
	  this.ram4000ExtEnabled = true;
	}
	break;

      case 6:
	this.ramC000Enabled = false;
	break;

      case 7:
	if( this.ramExt != null ) {
	  this.ramC000Enabled = true;
	}
	break;

      case 0x11:	// FDC DATA bei Rossendorf Floppy Disk Modul
      case 0x99:	// FDC DATA bei Robotron Floppy Disk Modul
      case 0x9B:
      case 0x9D:
      case 0x9F:
	if( this.fdc != null ) {
	  this.fdc.write( value );
	}
	break;

      case 0x12:	// Terminal Count bei Rossendorf Floppy Disk Modul
      case 0x13:
        if( this.fdc != null ) {
          this.fdc.fireTC();
        }
	break;

      case 0x20:
      case 0x21:
	if( this.ramFloppy1 != null ) {
	  this.ramFloppy1.writeByte( port, value );
	}
	break;

      case 0x24:
      case 0x25:
	if( this.ramFloppy2 != null ) {
	  this.ramFloppy2.writeByte( port, value );
	}
	break;

      case 0x80:				// A2=0
      case 0x81:
      case 0x82:
      case 0x83:
      case 0x84:				// A2=1
      case 0x85:
      case 0x86:
      case 0x87:
	this.ctc80.write( port & 0x03, value );
	break;

      case 0x88:				// A2=0
      case 0x8C:				// A2=1
	this.pio88.writePortA( value );
	int v = this.pio88.fetchOutValuePortA( false );
	updScreenConfig( v );
	if( this.emuThread.isSoundOutEnabled()
	    && ((v & 0x80) != 0) )
	{
	  this.emuThread.writeAudioPhase( this.audioOutPhase );
	}
	break;

      case 0x89:				// A2=0
      case 0x8D:				// A2=1
	this.pio88.writePortB( value );
	break;

      case 0x8A:				// A2=0
      case 0x8E:				// A2=1
	this.pio88.writeControlA( value );
	break;

      case 0x8B:				// A2=0
      case 0x8F:				// A2=1
	this.pio88.writeControlB( value );
	break;

      case 0x90:				// A2=0
      case 0x94:				// A2=1
	this.pio90.writePortA( value );		// Tastatur Spalten
	this.pio90.putInValuePortB( getKeyboardRowValue(), 0xFF );
	break;

      case 0x91:				// A2=0
      case 0x95:				// A2=1
	this.pio90.writePortB( value );		// Tastatur Zeilen
	this.pio90.putInValuePortA( getKeyboardColValue(), 0xFF );
	break;

      case 0x92:				// A2=0
      case 0x96:				// A2=1
	this.pio90.writeControlA( value );
	break;

      case 0x93:				// A2=0
      case 0x97:				// A2=1
	this.pio90.writeControlB( value );
	break;

      /*
       * Die Ports A0h und A1h werden sowohl vom Robotron Floppy-Disk-Modul
       * als auch von der 80-Zeichensteuerung verwendet.
       * In der Emulation muss deshalb eine Prioritisierung
       * vorgenommen werden:
       * Wenn das Floppy-Disk-Modul emuliert wird,
       * werden diese beiden Ports auch fuer das Floppy-Disk-Modul verwendet.
       */
      case 0xA0:	// Zusatzregister des Robotron Floppy Disk Moduls
      case 0xA1:
      case 0xA2:
      case 0xA3:
      case 0xA4:
      case 0xA5:
      case 0xA6:
      case 0xA7:
	if( this.fdc != null ) {
	  boolean tc = ((value & 0x10) != 0);
	  if( tc && (tc != this.fdcTC) ) {
	    this.fdc.fireTC();
	  }
	  boolean res = ((value & 0x20) != 0);
	  if( res && (res != this.fdcReset) ) {
	    this.fdc.reset( false );
	  }
	  this.fdcTC    = tc;
	  this.fdcReset = res;
	} else {
	  if( this.c80Enabled && ((port & 0xFE) == 0xA0) ) {
	    this.c80MemSwap = ((port & 0x01) != 0);
	  }
	}
	break;

      /*
       * Die Ports A8h und A9h werden sowohl vom Drucker-Modul
       * als auch von der 80-Zeichensteuerung verwendet.
       * In der Emulation muss deshalb eine Prioritisierung
       * vorgenommen werden:
       * Wenn das Drucker-Modul emuliert wird,
       * werden diese beiden Ports auch fuer das Drucker-Modul verwendet.
       */
      case 0xA8:				// A2=0
      case 0xA9:
      case 0xAA:
      case 0xAB:
      case 0xAC:				// A2=1
      case 0xAD:
      case 0xAE:
      case 0xAF:
	if( this.ctcA8 != null ) {
	  this.ctcA8.write( port & 0x03, value );
	} else {
	  if( this.c80Enabled && ((port & 0xFE) == 0xA8) ) {
	    upd80CharsMode( (port & 0x01) != 0 );
	  }
	}
	break;

      case 0xB0:				// A2=0
      case 0xB4:				// A2=1
	if( this.sioB0 != null ) {
	  this.sioB0.writeDataA( value );
	}
	break;

      case 0xB1:				// A2=0
      case 0xB5:				// A2=1
	if( this.sioB0 != null ) {
	  this.sioB0.writeDataB( value );
	}
	break;

      case 0xB2:				// A2=0
      case 0xB6:				// A2=1
	if( this.sioB0 != null ) {
	  this.sioB0.writeControlA( value );
	}
	break;

      case 0xB3:				// A2=0
      case 0xB7:				// A2=1
	if( this.sioB0 != null ) {
	  this.sioB0.writeControlB( value );
	}
	break;

      case 0xB8:
	if( this.ramPixel != null ) {
	  if( this.graphType == GRAPHIC_ROBOTRON ) {
	    this.graphBgColor = value & 0x07;
	    this.graphFgColor = (value >> 4) & 0x07;
	    this.graphBorder  = ((value & 0x80) != 0);
	    this.graphMode    = ((value & 0x08) != 0);
	    this.screenFrm.setScreenDirty( true );
	  } else if( this.graphType == GRAPHIC_KRT ) {
	    this.graphBank = value & 0x07;
	    this.graphMode = ((value & 0x08) != 0);
	    this.screenFrm.setScreenDirty( true );
	  }
	}
	break;

      case 0xB9:
	if( (this.ramPixel != null)
	    && (this.graphType == GRAPHIC_ROBOTRON) )
	{
	  this.graphAddrL = value & 0xFF;
	}
	break;

      case 0xBA:
	if( (this.ramPixel != null)
	    && (this.graphType == GRAPHIC_ROBOTRON) )
	{
	  int addr = (port & 0xFF00) | this.graphAddrL;
	  if( (addr >= 0) && (addr < this.ramPixel.length) ) {
	    this.ramPixel[ addr ] = (byte) value;
	    this.screenFrm.setScreenDirty( true );
	  }
	}
	break;

      case 0xBB:
	if( this.fontBytes != null ) {
	  if( this.fontOffs > 0 ) {
	    this.fontOffs = 0;
	  } else {
	    if( this.fontBytes.length > 0x0800 ) {
	      this.fontOffs = 0x0800;
	    }
	  }
	}
	break;

      case 0xBC:
	if( this.c80Enabled ) {
	  upd80CharsMode( false );
	}
	break;

      case 0xBD:
	if( this.c80Enabled ) {
	  upd80CharsMode( true );
	}
	break;

      case 0xBE:
	if( this.c80Enabled ) {
	  this.c80MemSwap = false;
	}
	break;

      case 0xBF:
	if( this.c80Enabled ) {
	  this.c80MemSwap = true;
	}
	break;

      case 0xFF:
	this.megaROMSeg = (value & 0xFF);
	break;
    }
  }


  @Override
  public void writeMemByte( int addr, int value )
  {
    if( this.ramFont != null ) {
      switch( addr ) {
	case 0xEBFC:
	  this.ramFontActive  = false;
	  this.ramFontEnabled = true;
	  this.screenFrm.setScreenDirty( true );
	  break;
	case 0xEBFE:
	  this.ramFontActive  = true;
	  this.ramFontEnabled = false;
	  this.screenFrm.setScreenDirty( true );
	  break;
	case 0xEBFF:
	  this.ramFontActive  = false;
	  this.ramFontEnabled = false;
	  this.screenFrm.setScreenDirty( true );
	  break;
      }
    }
    if( addr >= 0xF800 ) {
      this.romModuleEnabled = ((addr & 0x0400) == 0);
    }
    setMemByteInternal( addr, value, true );
  }


	/* --- private Methoden --- */

  private void adjustVideoRAMAccessTStates()
  {
    if( (this.lineNum < 192)
	&& (this.tStatesPerLine > 0)
	&& (this.tStatesVisible > 0)
	&& (this.lineTStates < this.tStatesVisible) )
    {
      this.emuThread.getZ80CPU().addWaitStates(
			this.tStatesVisible - this.lineTStates );
    }
  }


  private void applyPasteFast( Properties props )
  {
    this.pasteFast = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "paste.fast",
				true );
  }


  private synchronized void checkAddPCListener( Properties props )
  {
    boolean state = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "catch_print_calls", true );
    if( state != this.pcListenerAdded ) {
      Z80CPU cpu = this.emuThread.getZ80CPU();
      if( state ) {
	cpu.addPCListener( this, 0x0005 );
      } else {
	cpu.removePCListener( this );
      }
      this.pcListenerAdded = state;
    }
  }


  private void createColors( Properties props )
  {
    double brightness = getBrightness( props );
    if( (brightness >= 0.0) && (brightness <= 1.0) ) {
      for( int i = 0; i < this.colors.length; i++ ) {
	this.colors[ i ] = new Color(
		(int) Math.round( basicRGBValues[ i ][ 0 ] * brightness ),
		(int) Math.round( basicRGBValues[ i ][ 1 ] * brightness ),
		(int) Math.round( basicRGBValues[ i ][ 2 ] * brightness ) );
      }
    }
  }


  private boolean emulates80CharsMode( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "80_chars.enabled",
				false );
  }


  private boolean emulatesRAM16K4000( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "ram_16k_4000.enabled",
				false );
  }


  private boolean emulatesRAM16K8000( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "ram_16k_8000.enabled",
				false );
  }


  private boolean emulatesRAM64K( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "ram_64k.enabled",
				false );
  }


  private boolean emulatesROM16K4000( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_16k_4000.enabled",
				false );
  }


  private boolean emulatesROM32K4000( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_32k_4000.enabled",
				false );
  }


  private boolean emulatesROM16K8000( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_16k_8000.enabled",
				false );
  }


  private boolean emulatesROM10KC000( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_10k_c000.enabled",
				false );
  }


  private boolean emulatesBootROM( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_boot.enabled",
				false );
  }


  private boolean emulatesFloppyDisk( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "floppydisk.enabled",
				false );
  }


  private boolean emulatesMegaROM( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_mega.enabled",
				false );
  }


  private boolean emulatesPrinterModule( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "printer_module.enabled",
				false );
  }


  private boolean emulatesProgrammableFont( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "font.programmable",
				false );
  }


  private static boolean equalsROMModule(
				byte[]     currentBytes,
				String     currentFile,
				Properties props,
				String     propEnabled,
				String     propFile )
  {
    boolean rv = false;
    if( EmuUtil.getBooleanProperty( props, propEnabled, false ) ) {
      if( (currentBytes != null)
	  && EmuUtil.equals(
		currentFile,
		EmuUtil.getProperty( props, propFile ) ) )
      {
	rv = true;
      }
    } else {
      if( currentBytes == null ) {
	rv = true;
      }
    }
    return rv;
  }


  private boolean getColorMode( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "color",
				true );
  }


  private int getGraphicType( Properties props )
  {
    int    rv        = GRAPHIC_NONE;
    String graphType = EmuUtil.getProperty(
			props,
			this.propPrefix + "graphic.type" ).toLowerCase();
    if( graphType.equals( "robotron" ) ) {
      rv = GRAPHIC_ROBOTRON;
    }
    else if( graphType.equals( "krt" ) ) {
      rv = GRAPHIC_KRT;
    }
    return rv;
  }


  private int getKeyboardColValue()
  {
    int rv       = 0;
    int rowValue = ~this.pio90.fetchOutValuePortB( false );
    int mask     = 0x01;
    synchronized( this.kbMatrix ) {
      for( int i = 0; i < this.kbMatrix.length; i++ ) {
	if( (rowValue & this.kbMatrix[ i ]) != 0 ) {
	  rv |= mask;
	}
	mask <<= 1;
      }
    }
    if( (rowValue & 0x40) != 0 ) {
      if( (this.joy0ActionMask & JoystickThread.LEFT_MASK) != 0 ) {
	rv |= 0x01;
      }
      if( (this.joy0ActionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	rv |= 0x02;
      }
      if( (this.joy0ActionMask & JoystickThread.DOWN_MASK) != 0 ) {
	rv |= 0x04;
      }
      if( (this.joy0ActionMask & JoystickThread.UP_MASK) != 0 ) {
	rv |= 0x08;
      }
      if( (this.joy0ActionMask & JoystickThread.BUTTONS_MASK) != 0 ) {
	rv |= 0x10;
      }
    }
    if( (rowValue & 0x80) != 0 ) {
      if( (this.joy1ActionMask & JoystickThread.LEFT_MASK) != 0 ) {
	rv |= 0x01;
      }
      if( (this.joy1ActionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	rv |= 0x02;
      }
      if( (this.joy1ActionMask & JoystickThread.DOWN_MASK) != 0 ) {
	rv |= 0x04;
      }
      if( (this.joy1ActionMask & JoystickThread.UP_MASK) != 0 ) {
	rv |= 0x08;
      }
      if( (this.joy1ActionMask & JoystickThread.BUTTONS_MASK ) != 0 ) {
	rv |= 0x10;
      }
    }
    return ~rv & 0xFF;
  }


  private int getKeyboardRowValue()
  {
    int colValue = ~this.pio90.fetchOutValuePortA( false );
    int rv       = 0;
    int mask     = 0x01;
    synchronized( this.kbMatrix ) {
      for( int i = 0; i < this.kbMatrix.length; i++ ) {
	if( (colValue & mask) != 0 ) {
	  rv |= this.kbMatrix[ i ];
	}
	mask <<= 1;
      }
    }
    if( (colValue & 0x01) != 0 ) {
      if( (this.joy0ActionMask & JoystickThread.LEFT_MASK) != 0 ) {
	rv |= 0x40;
      }
      if( (this.joy1ActionMask & JoystickThread.LEFT_MASK) != 0 ) {
	rv |= 0x80;
      }
    }
    if( (colValue & 0x02) != 0 ) {
      if( (this.joy0ActionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	rv |= 0x40;
      }
      if( (this.joy1ActionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	rv |= 0x80;
      }
    }
    if( (colValue & 0x04) != 0 ) {
      if( (this.joy0ActionMask & JoystickThread.DOWN_MASK) != 0 ) {
	rv |= 0x40;
      }
      if( (this.joy1ActionMask & JoystickThread.DOWN_MASK) != 0 ) {
	rv |= 0x80;
      }
    }
    if( (colValue & 0x08) != 0 ) {
      if( (this.joy0ActionMask & JoystickThread.UP_MASK) != 0 ) {
	rv |= 0x40;
      }
      if( (this.joy1ActionMask & JoystickThread.UP_MASK) != 0 ) {
	rv |= 0x80;
      }
    }
    if( (colValue & 0x10) != 0 ) {
      if( (this.joy0ActionMask & JoystickThread.BUTTONS_MASK) != 0 ) {
	rv |= 0x40;
      }
      if( (this.joy1ActionMask & JoystickThread.BUTTONS_MASK) != 0 ) {
	rv |= 0x80;
      }
    }
    return ~rv & 0xFF;
  }


  private int getMemByteInternal( int addr, boolean emuWaitStates )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( addr < 0x4000 ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    else if( (addr >= 0x4000) && (addr < 0x8000) ) {
      if( this.rom16k4000 != null ) {
	int idx = addr - 0x4000;
	if( idx < this.rom16k4000.length ) {
	  rv = (int) this.rom16k4000[ idx ] & 0xFF;
	}
      } else if( this.rom32k4000 != null ) {
	int idx = addr - 0x4000;
	if( idx < this.rom32k4000.length ) {
	  rv = (int) this.rom32k4000[ idx ] & 0xFF;
	}
      } else if( this.ram64k ) {
	if( this.ram4000ExtEnabled && (this.ramExt != null) ) {
	  int idx = addr - 0x4000;
	  if( idx < this.ramExt.length ) {
	    rv = (int) this.ramExt[ idx ] & 0xFF;
	  }
	} else {
	  rv = this.emuThread.getRAMByte( addr );
	}
      } else if( this.ram16k4000 ) {
	rv = this.emuThread.getRAMByte( addr );
      }
    }
    else if( (addr >= 0x8000) && (addr < 0xC000) ) {
      if( this.rom32k4000 != null ) {
	int idx = addr - 0x4000;
	if( idx < this.rom32k4000.length ) {
	  rv = (int) this.rom32k4000[ idx ] & 0xFF;
	}
      } else if( this.rom16k8000 != null ) {
	int idx = addr - 0x8000;
	if( idx < this.rom16k8000.length ) {
	  rv = (int) this.rom16k8000[ idx ] & 0xFF;
	}
      } else if( this.ram16k8000 || this.ram64k ) {
	rv = this.emuThread.getRAMByte( addr );
      } else if( this.printerModule && (printerModBytes != null) ) {
	/*
	 * Der ROM des Druckermoduls ist nur bei einer RAM-Bestueckung
	 * bis max. 32 KByte aktiv.
	 * Deshalb wird dieser ROM erst nach dem RAM abgefragt.
	 */
	int idx = addr - 0xB800;
	if( (idx >= 0) && (idx < printerModBytes.length) ) {
	  rv = (int) this.printerModBytes[ idx ] & 0xFF;
	}
      }
    }
    else if( (addr >= 0xC000) && (addr < 0xE800) ) {
      if( this.ram64k && this.ramC000Enabled ) {
	rv = this.emuThread.getRAMByte( addr );
      } else if( this.romModuleEnabled && (this.romBoot != null) ) {
	int idx = addr - 0xC000;
	if( idx < this.romBoot.length ) {
	  rv = (int) this.romBoot[ idx ] & 0xFF;
	}
      } else if( this.romModuleEnabled && (this.romMega != null) ) {
	int idx = 0;
	if( addr < 0xC800 ) {
	  idx = addr - 0xC000 + (this.megaROMSeg * 2048);
	} else if( addr < 0xD000 ) {
	  idx = addr - 0xC800 + ((256 + this.megaROMSeg) * 2048);
	} else if( addr < 0xD800 ) {
	  idx = addr - 0xD000 + (((2 * 256) + this.megaROMSeg) * 2048);
	} else if( addr < 0xE000 ) {
	  idx = addr - 0xD800 + (((3 * 256) + this.megaROMSeg) * 2048);
	} else {
	  idx = addr - 0xE000 + (((4 * 256) + this.megaROMSeg) * 2048);
	}
	if( (idx >= 0) && (idx < this.romMega.length) ) {
	  rv = (int) this.romMega[ idx ] & 0xFF;
	}
      } else if( this.rom10kC000 != null ) {
	int idx = addr - 0xC000;
	if( idx < this.rom10kC000.length ) {
	  rv = (int) this.rom10kC000[ idx ] & 0xFF;
	}
      } else if( this.kc87 && (this.romBasic != null) ) {
	int idx = addr - 0xC000;
	if( idx < this.romBasic.length ) {
	  rv = (int) this.romBasic[ idx ] & 0xFF;
	}
      }
    }
    else if((addr >= 0xE800) && (addr < 0xEC00) ) {
      if( this.ramFontEnabled && (this.ramFont != null) ) {
	int idx = addr - 0xE800;
	if( idx < this.ramFont.length ) {
	  rv = (int) this.ramFont[ addr - 0xE800 ] & 0xFF;
	}
      } else if( this.ramColor != null ) {
	if( this.c80MemSwap ) {
	  rv = (int) this.ramColor2[ addr - 0xE800 ] & 0xFF;
	} else {
	  rv = (int) this.ramColor[ addr - 0xE800 ] & 0xFF;
	}
	if( emuWaitStates ) {
	  adjustVideoRAMAccessTStates();
	}
      }
    }
    else if( (addr >= 0xEC00) && (addr < 0xF000) ) {
      int idx = addr - 0xEC00;
      if( (this.ramPixel != null)
	  && (this.graphType == GRAPHIC_KRT)
	  && this.graphMode )
      {
	idx += (this.graphBank * 0x0400);
	if( (idx >= 0) && (idx < this.ramPixel.length) ) {
	  rv = (int) this.ramPixel[ idx ] & 0xFF;
	}
      } else {
	if( this.c80MemSwap ) {
	  rv = (int) this.ramVideo2[ idx ] & 0xFF;
	} else {
	  rv = (int) this.ramVideo[ idx ] & 0xFF;
	}
      }
      if( emuWaitStates ) {
	adjustVideoRAMAccessTStates();
      }
    }
    else if( (addr >= 0xF000) && (this.romOS != null) ) {
      int idx = addr - 0xF000;
      if( idx < this.romOS.length ) {
	rv = (int) this.romOS[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  private void loadFont( Properties props )
  {
    this.fontBytes = readFontByProperty(
				props,
				this.propPrefix + "font.file",
				0x1000 );
    if( this.fontBytes == null ) {
      if( z9001FontBytes == null ) {
	z9001FontBytes = readResource( "/rom/z9001/z9001font.bin" );
      }
      this.fontBytes = z9001FontBytes;
    }
  }


  private void loadROMs( Properties props )
  {
    this.romOSFile = EmuUtil.getProperty(
				props,
				this.propPrefix + "os.file" );
    this.romOS = readFile( this.romOSFile, 0x1000, "Betriebssystem" );
    if( this.romOS == null ) {
      if( this.sysName.equals( "KC87" ) ) {
	if( os13 == null ) {
	  os13 = readResource( "/rom/z9001/os13.bin" );
	}
	this.romOS = os13;
      } else if( this.sysName.equals( "KC85/1" ) ) {
	if( os12 == null ) {
	  os12 = readResource( "/rom/z9001/os12.bin" );
	}
	this.romOS = os12;
      } else {
	if( os11 == null ) {
	  os11 = readResource( "/rom/z9001/os11.bin" );
	}
	this.romOS = os11;
      }
    }

    // BASIC-ROM
    if( this.kc87 ) {
      this.romBasicFile = EmuUtil.getProperty(
				props,
				this.propPrefix + "basic.file" );
      this.romBasic = readFile( this.romBasicFile, 0x2800, "BASIC" );
      if( this.romBasic == null ) {
	if( basic86 == null ) {
	  basic86 = readResource( "/rom/z9001/basic86.bin" );
	}
	this.romBasic = basic86;
      }
    } else {
      this.romBasicFile = null;
      this.romBasic     = null;
    }

    // Modul-ROM
    this.romModuleFile = props.getProperty(
			      this.propPrefix + "rom_module.file" );
    if( emulatesROM16K4000( props ) ) {
      this.rom16k4000 = readFile(
			this.romModuleFile,
			0x4000,
			"ROM-Modul 4000h-7FFFh" );
    }
    else if( emulatesROM32K4000( props ) ) {
      this.rom32k4000 = readFile(
			this.romModuleFile,
			0x8000,
			"ROM-Modul 4000h-BFFFh" );
    }
    else if( emulatesROM16K8000( props ) ) {
      this.rom16k8000 = readFile(
			this.romModuleFile,
			0x4000,
			"ROM-Modul 8000h-BFFFh" );
    }
    else if( emulatesROM10KC000( props ) ) {
      this.rom10kC000 = readFile(
			this.romModuleFile,
			0x4000,
			"ROM-Modul C000h-E7FFh" );
    }
    else if( emulatesBootROM( props ) ) {
      this.romBoot = readFile(
			this.romModuleFile,
			0x4000,
			"Boot-ROM-Modul" );
      if( this.romBoot == null ) {
	if( bootROMBytes == null ) {
	  bootROMBytes = readResource( "/rom/z9001/bootrom.bin" );
	}
	this.romBoot = bootROMBytes;
      }
    }
    else if( emulatesMegaROM( props ) ) {
      this.romMega = readFile(
			this.romModuleFile,
			256 * 10240,
			"Mega-ROM-Modul" );
      if( this.romMega == null ) {
	if( megaROMBytes == null ) {
	  megaROMBytes = readResource( "/rom/z9001/megarom.bin.gz" );
	}
	this.romMega = megaROMBytes;
      }
    }

    // Zeichensatz
    loadFont( props );
  }


  private void putKeyboardMatrixValuesToPorts()
  {
    this.pio90.putInValuePortB( getKeyboardRowValue(), 0xFF );
    this.pio90.putInValuePortA( getKeyboardColValue(), 0xFF );
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


  private boolean setMemByteInternal(
				int     addr,
				int     value,
				boolean emuWaitStates )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( addr < 0x4000 ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    else if( (addr >= 0x4000) && (addr < 0x8000) ) {
      if( (this.rom16k4000 == null) && (this.rom32k4000 == null) ) {
	if( this.ram64k ) {
	  if( this.ram4000ExtEnabled && (this.ramExt != null) ) {
	    int idx = addr - 0x4000;
	    if( idx < this.ramExt.length ) {
	      this.ramExt[ idx ] = (byte) value;
	      rv = true;
	    }
	  } else {
	    this.emuThread.setRAMByte( addr, value );
	    rv = true;
	  }
	} else if( this.ram16k4000 ) {
	  this.emuThread.setRAMByte( addr, value );
	  rv = true;
	}
      }
    }
    else if( (addr >= 0x8000) && (addr < 0xC000) ) {
      if( (this.rom32k4000 == null) && (this.rom16k8000 == null)
	  && (this.ram16k8000 || this.ram64k) )
      {
	this.emuThread.setRAMByte( addr, value );
	rv = true;
      }
    }
    else if( (addr >= 0xC000) && (addr < 0xE800) ) {
      if( this.ram64k && this.ramC000Enabled ) {
	this.emuThread.setRAMByte( addr, value );
	rv = true;
      }
    }
    else if((addr >= 0xE800) && (addr < 0xEC00) ) {
      int idx = addr - 0xE800;
      if( this.ramFontEnabled && (this.ramFont != null) ) {
	if( idx < this.ramFont.length ) {
	  this.ramFont[ idx ] = (byte) value;
	  rv = true;
	}
      } else if( this.ramColor != null ) {
	if( this.c80MemSwap ) {
	  this.ramColor2[ idx ] = (byte) value;
	} else {
	  this.ramColor[ idx ] = (byte) value;
	}
	this.screenFrm.setScreenDirty( true );
	rv = true;
	if( emuWaitStates ) {
	  adjustVideoRAMAccessTStates();
	}
      }
    }
    else if( (addr >= 0xEC00) && (addr < 0xF000) ) {
      int idx = addr - 0xEC00;
      if( (this.ramPixel != null)
	  && (this.graphType == GRAPHIC_KRT)
	  && this.graphMode )
      {
	idx += (this.graphBank * 0x0400);
	if( (idx >= 0) && (idx < this.ramPixel.length) ) {
	  this.ramPixel[ idx ] = (byte) value;
	  this.screenFrm.setScreenDirty( true );
	  rv = true;
	}
      } else {
	if( this.c80MemSwap ) {
	  this.ramVideo2[ idx ] = (byte) value;
	} else {
	  this.ramVideo[ idx ] = (byte) value;
	}
	this.screenFrm.setScreenDirty( true );
	rv = true;
      }
      if( emuWaitStates ) {
	adjustVideoRAMAccessTStates();
      }
    }
    return rv;
  }


  private void upd80CharsMode( boolean state )
  {
    if( this.c80Active != state ) {
      this.c80Active = state;
      this.screenFrm.fireScreenSizeChanged();
      this.screenFrm.setScreenDirty( true );
    }
  }


  private void updScreenConfig( int value )
  {
    boolean mode20Rows = ((value & 0x04) != 0);
    int     colorIdx   = (value >> 3) & 0x07;
    if( (mode20Rows != this.mode20Rows)
	|| (colorIdx != this.borderColorIdx) )
    {
      this.mode20Rows     = mode20Rows;
      this.borderColorIdx = colorIdx;
      this.screenFrm.setScreenDirty( true );
    }
  }
}
