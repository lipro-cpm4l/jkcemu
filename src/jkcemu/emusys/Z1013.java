/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Z1013
 */

package jkcemu.emusys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.audio.AbstractSoundDevice;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.AbstractScreenDevice;
import jkcemu.base.AbstractScreenFrm;
import jkcemu.base.AutoInputCharSet;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuMemView;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuUtil;
import jkcemu.base.EmuThread;
import jkcemu.base.OptionDlg;
import jkcemu.base.RAMFloppy;
import jkcemu.base.SourceUtil;
import jkcemu.disk.FDC8272;
import jkcemu.disk.FloppyDiskDrive;
import jkcemu.disk.FloppyDiskFormat;
import jkcemu.disk.FloppyDiskInfo;
import jkcemu.disk.GIDE;
import jkcemu.emusys.z1013.KeyboardMatrix;
import jkcemu.emusys.z1013.KeyboardMatrix8x4;
import jkcemu.emusys.z1013.KeyboardMatrix8x8;
import jkcemu.emusys.z1013.Z1013GraphicPoppe;
import jkcemu.emusys.z1013.Z1013GraphicZX;
import jkcemu.emusys.z1013.Z1013Keyboard;
import jkcemu.emusys.z1013.Z1013KeyboardFld8x4;
import jkcemu.emusys.z1013.Z1013KeyboardFld8x8;
import jkcemu.etc.GraphicCCJena;
import jkcemu.etc.K1520Sound;
import jkcemu.etc.RTC7242X;
import jkcemu.file.FileFormat;
import jkcemu.file.SaveDlg;
import jkcemu.joystick.JoystickThread;
import jkcemu.net.KCNet;
import jkcemu.print.PrintMngr;
import jkcemu.text.TextUtil;
import jkcemu.usb.VDIP;
import z80emu.Z80AddressListener;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80MemView;
import z80emu.Z80PCListener;
import z80emu.Z80PIO;
import z80emu.Z80PIOPortListener;


public class Z1013 extends EmuSys implements
					FDC8272.DriveSelector,
					Z80AddressListener,
					Z80MaxSpeedListener,
					Z80PCListener,
					Z80PIOPortListener
{
  public static final String SYSNAME                = "Z1013";
  public static final String SYSNAME_Z1013_01       = "Z1013.01";
  public static final String SYSNAME_Z1013_12       = "Z1013.12";
  public static final String SYSNAME_Z1013_16       = "Z1013.16";
  public static final String SYSNAME_Z1013_64       = "Z1013.64";
  public static final String PROP_PREFIX            = "jkcemu.z1013.";
  public static final String PROP_CATCH_JOY_CALLS   = "catch_joystick_calls";
  public static final String PROP_GRA2_FONT_PREFIX  = "graphic2.font.";
  public static final String PROP_GRA_CCJ_ENABLED   = "graphic_ccj.enabled";
  public static final String PROP_GRA_POPPE_ENABLED = "graphic_poppe.enabled";
  public static final String PROP_GRA_KRT_ENABLED   = "graphic_krt.enabled";
  public static final String PROP_GRA_ZX_ENABLED    = "graphic_zx.enabled";
  public static final String PROP_PETERS_ENABLED    = "peterscard.enabled";
  public static final String PROP_MONITOR           = "monitor";
  public static final String PROP_EXTROM_PREFIX     = "ext_rom.";
  public static final String PROP_BASIC_ENABLED     = "basic.enabled";
  public static final String PROP_MEGA_ENABLED      = "mega.enabled";
  public static final String PROP_8000_ENABLED      = "8000.enabled";
  public static final String PROP_8000_ON_RESET     = "8000.on_reset";
  public static final String PROP_USERPORT          = "userport";
  public static final String VALUE_MON_202          = "2.02";
  public static final String VALUE_MON_A2           = "A.2";
  public static final String VALUE_MON_RB_K7659     = "RB_K7659";
  public static final String VALUE_MON_RB_S6009     = "RB_S6009";
  public static final String VALUE_MON_INCOM_K7669  = "INCOM_K7669";
  public static final String VALUE_MON_JM_1992      = "JM_1992";
  public static final String VALUE_MON_BL4_K7659    = "BL4_K7659";
  public static final String VALUE_CENTR7_PRAC0289  = "centr7:practic0289";
  public static final String VALUE_CENTR8_FA1090    = "centr8:fa1090";
  public static final String VALUE_JOYST_JUTE0687   = "joyst:jute0687";
  public static final String VALUE_JOYST_PRAC0487   = "joyst:practic0487";
  public static final String VALUE_JOYST_PRAC0188   = "joyst:practic0188";

  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE = true;

  public static final int FUNCTION_KEY_COUNT = 4;
  public static final int MEM_ARG1           = 0x001B;
  public static final int MEM_HEAD           = 0x00E0;

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

  /*
   * Mapping der Zeichen ab Code 14 (Schachfiguren)
   *
   * Da die Z1013-Schachfiguren aus zwei uebereinander
   * stehenden Zeichen bestehen,
   * im Unicode aber es nur jeweils ein Zeichen dafuer gibt,
   * wird das obere Zeichen in das Unicode-Zeichen gemappt
   * und das untere in ein Leerzeichen.
   */
  private static final int[] char14ToUnicode = {
		'\u265F', '\u265C', '\u265E', '\u0020', '\u265D',
		'\u0020', '\u265B', '\u265A', '\u0020',
		'\u2659', '\u2656', '\u2658', '\u0020', '\u2657',
		'\u0020', '\u2655', '\u2654', '\u0020' };

  private static final FloppyDiskInfo cpm64x16FloppyDisk =
		new FloppyDiskInfo(
			"/disks/z1013/z1013cpm64x16.dump.gz",
			"Z1013 CP/M Boot-Diskette (64x16 Zeichen)",
			2, 2048, true );

  private static final FloppyDiskInfo cpm80x25FloppyDisk =
		new FloppyDiskInfo(
			"/disks/z1013/z1013cpm80x25.dump.gz",
			"Z1013 CP/M Boot-Diskette (80x25 Zeichen)",
			2, 2048, true );

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

  private static AutoInputCharSet autoInputCharSet = null;

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

  private Z80PIO                     pio;
  private GIDE                       gide;
  private FDC8272                    fdc;
  private RTC7242X                   rtc;
  private GraphicCCJena              graphicCCJena;
  private Z1013GraphicPoppe          graphicPoppe;
  private Z1013GraphicZX             graphicZX;
  private Z1013Keyboard              keyboard;
  private AbstractKeyboardFld<Z1013> keyboardFld;
  private K1520Sound                 k1520Sound;
  private KCNet                      kcNet;
  private VDIP                       vdip;
  private RAMFloppy                  ramFloppy1;
  private RAMFloppy                  ramFloppy2;
  private int                        ramBankKRT;
  private int                        ramEndAddr;
  private byte[][]                   ramKRT;
  private byte[]                     ramVideo;
  private byte[]                     rom8000;
  private byte[]                     romBasic;
  private byte[]                     romMega;
  private byte[]                     stdFontBytes;
  private byte[]                     altFontBytes;
  private byte[]                     osBytes;
  private String                     osFile;
  private String                     extRomFile;
  private String                     monCode;
  private String                     sysName;
  private boolean                    rom8000Enabled;
  private boolean                    romOSEnabled;
  private boolean                    videoEnabled;
  private boolean                    petersCardEnabled;
  private boolean                    altFontEnabled;
  private boolean                    catchPrintCalls;
  private boolean                    mode4MHz;
  private boolean                    mode64x16;
  private volatile boolean           modeKRT;
  private volatile boolean           fixedScreenSize;
  private volatile boolean           pasteFast;
  private volatile int               charToPaste;
  private int                        centrTStatesToAck;
  private int                        io4Value;
  private int                        romMegaSeg;
  private int                        lastWrittenAddr;
  private int[]                      pcListenerAddrs;
  private FloppyDiskDrive[]          floppyDiskDrives;
  private BasicType                  lastBasicType;
  private UserPort                   userPort;
  private volatile int               joy0ActionMask;
  private volatile int               joy1ActionMask;
  private volatile boolean           catchJoyCalls;


  public Z1013( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.stdFontBytes      = null;
    this.altFontBytes      = null;
    this.osBytes           = null;
    this.osFile            = null;
    this.monCode           = null;
    this.extRomFile        = null;
    this.rom8000           = null;
    this.romBasic          = null;
    this.romMega           = null;
    this.romOSEnabled      = true;
    this.videoEnabled      = true;
    this.petersCardEnabled = false;
    this.altFontEnabled    = false;
    this.rom8000Enabled    = false;
    this.mode64x16         = false;
    this.mode4MHz          = false;
    this.modeKRT           = false;
    this.pcListenerAddrs   = null;
    this.lastBasicType     = null;
    this.userPort          = UserPort.NONE;
    this.lastWrittenAddr   = -1;
    this.pasteFast         = false;
    this.charToPaste       = 0;
    this.centrTStatesToAck = 0;
    this.io4Value          = 0;
    this.romMegaSeg        = 0;
    this.ramBankKRT        = 0;
    this.ramEndAddr        = getRAMEndAddr( props );
    this.ramVideo          = new byte[ 0x0400 ];

    this.sysName = EmuUtil.getProperty( props, EmuThread.PROP_SYSNAME );
    this.petersCardEnabled = emulatesPetersCard( props );
    this.fixedScreenSize   = isFixedScreenSize( props );

    this.ramKRT = null;
    if( emulatesGraphicKRT( props ) ) {
      this.ramKRT = new byte[ 8 ][];
      for( int i = 0; i < this.ramKRT.length; i++ ) {
	this.ramKRT[ i ] = new byte[ 0x0400 ];
      }
    }

    this.ramFloppy1 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy1(),
				"Z1013",
				RAMFloppy.RFType.MP_3_1988,
				"RAM-Floppy an E/A-Adressen 98h-9Fh",
				props,
				PROP_PREFIX + PROP_RF1_PREFIX );

    this.ramFloppy2 = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy2(),
				"Z1013",
				RAMFloppy.RFType.MP_3_1988,
				"RAM-Floppy an E/A-Adressen 58h-5Fh",
				props,
				PROP_PREFIX + PROP_RF2_PREFIX );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    this.pio   = new Z80PIO( "PIO (E/A-Adressen 00h-03h)" );
    cpu.addAddressListener( this );
    cpu.addMaxSpeedListener( this );
    checkAddPCListener( props );

    if( emulatesFloppyDisk( props ) ) {
      this.floppyDiskDrives = new FloppyDiskDrive[ 4 ];
      Arrays.fill( this.floppyDiskDrives, null );
      this.fdc = new FDC8272( this, 4 );
    } else {
      this.floppyDiskDrives = null;
      this.fdc              = null;
    }
    cpu.addTStatesListener( this );

    this.rtc = emulatesRTC( props ) ? new RTC7242X() : null;

    if( emulatesGraphicZX( props ) ) {
      this.graphicZX = new Z1013GraphicZX( this, props );
    } else {
      this.graphicZX = null;
    }

    if( (this.graphicZX == null) && emulatesGraphicCCJena( props ) ) {
      this.graphicCCJena = new GraphicCCJena( this, props );
    } else {
      this.graphicCCJena = null;
    }

    if( (this.graphicZX == null)
	&& (this.graphicCCJena == null)
	&& emulatesGraphicPoppe( props ) )
    {
      this.graphicPoppe = new Z1013GraphicPoppe( this, props );
      this.graphicPoppe.setFixedScreenSize( true );
    } else {
      this.graphicPoppe = null;
    }

    if( emulatesK1520Sound( props ) ) {
      this.k1520Sound = new K1520Sound( this, 0x38 );
    } else {
      this.k1520Sound = null;
    }

    if( emulatesKCNet( props ) ) {
      this.kcNet = new KCNet( "Netzwerk-PIO (E/A-Adressen C0h-C3h)" );
    } else {
      this.kcNet = null;
    }

    if( emulatesVDIP( props ) ) {
      this.vdip = new VDIP(
			0,
			this.emuThread.getZ80CPU(),
			"USB-PIO (E/A-Adressen FCh-FFh)" );
    } else {
      this.vdip = null;
    }

    this.gide = GIDE.getGIDE( this.screenFrm, props, this.propPrefix );

    java.util.List<Z80InterruptSource> iSources = new ArrayList<>();
    if( this.graphicZX != null ) {
      /*
       * ist nicht in der Daisy Chain und kann deshalb von einer anderen
       * Interrupt-Quelle nicht blockiert werden -> hochste Prioritaet
       */
      iSources.add( this.graphicZX );
    }
    iSources.add( this.pio );
    if( this.k1520Sound != null ) {
      iSources.add( this.k1520Sound );
    }
    if( this.kcNet != null ) {
      iSources.add( this.kcNet );
    }
    if( this.vdip != null ) {
      iSources.add( this.vdip );
    }
    try {
      cpu.setInterruptSources(
        iSources.toArray( new Z80InterruptSource[ iSources.size() ] ) );
    }
    catch( ArrayStoreException ex ) {}

    this.pio.addPIOPortListener( this, Z80PIO.PortInfo.B );

    this.keyboardFld = null;
    this.keyboard    = new Z1013Keyboard( this.pio );
    this.keyboard.applySettings( props );

    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
    z80MaxSpeedChanged( cpu );
    applyUserPortSettings( props );
  }


  public static AutoInputCharSet getAutoInputCharSet()
  {
    if( autoInputCharSet == null ) {
      autoInputCharSet = new AutoInputCharSet();
      autoInputCharSet.addAsciiChars();
      autoInputCharSet.addEnterChar();
      autoInputCharSet.addCursorChars();
      autoInputCharSet.addSpecialChar( 3, "CTRL-C", "CTRL-C / S4-K" );
    }
    return autoInputCharSet;
  }


  public boolean emulatesGraphicKRT()
  {
    return this.ramKRT != null;
  }


  public boolean emulatesGraphicZX()
  {
    return this.graphicZX != null;
  }


  public static FloppyDiskInfo[] getAvailableFloppyDisks()
  {
    return availableFloppyDisks;
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return EmuUtil.getProperty(
		props,
		EmuThread.PROP_SYSNAME ).startsWith( SYSNAME_Z1013_01 ) ?
								1000 : 2000;
  }


  public static String getTinyBasicProgram( EmuMemView memory )
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


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    if( this.graphicZX != null ) {
      this.graphicZX.z80MaxSpeedChanged( cpu );
    }
    if( this.fdc != null ) {
      this.fdc.z80MaxSpeedChanged( cpu );
    }
    if( this.k1520Sound != null ) {
      this.k1520Sound.z80MaxSpeedChanged( cpu );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80MaxSpeedChanged( cpu );
    }
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


	/* --- Z80PIOPortListener --- */

  @Override
  public void z80PIOPortStatusChanged(
				Z80PIO          pio,
				Z80PIO.PortInfo port,
				Z80PIO.Status   status )
  {
    if( (pio == this.pio)
	&& (port == Z80PIO.PortInfo.B)
	&& ((status == Z80PIO.Status.OUTPUT_AVAILABLE)
	    || (status == Z80PIO.Status.OUTPUT_CHANGED)) )
    {
      this.keyboard.putRowValuesToPIO();
      this.tapeOutPhase = ((this.pio.fetchOutValuePortB( 0x00 ) & 0x80) != 0);
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    buf.append( "<h1>Z1013 Status</h1>\n"
	+ "<table border=\"1\">\n"
	+ "<tr><td>Monitor-ROM" );
    byte[] rom = this.osBytes;
    if( rom != null ) {
      if( rom.length > 0 ) {
	buf.append( String.format(
			" (F000h-F%03X)",
			Math.min( rom.length, 0x1000 ) - 1 ) );
      }
    }
    buf.append( ":</td><td>" );
    EmuUtil.appendOnOffText( buf, this.romOSEnabled );
    buf.append( "</td></tr>\n" );
    if( this.rom8000 != null ) {
      buf.append( "<tr><td>27K-ROM (8000h-EBFFh):</td><td>" );
      buf.append( this.rom8000Enabled ? EmuUtil.TEXT_ON : EmuUtil.TEXT_OFF );
      buf.append( "</td></tr>\n" );
    }
    if( this.romMega != null ) {
      buf.append( "<tr><td>Mega-ROM (C000h-E7FFh):</td><td>Bank " );
      buf.append( this.romMegaSeg );
      buf.append( "</td></tr>\n" );
    }
    if( this.ramKRT != null ) {
      buf.append( "<tr><td>KRT-Grafik:</td><td>" );
      if( this.modeKRT ) {
	int bank = this.ramBankKRT;
	if( (bank >= 0) && (bank < this.ramKRT.length) ) {
	  buf.append( "Bank " );
	  buf.append( bank );
	  buf.append( " ein" );
	}
      } else {
	buf.append( EmuUtil.TEXT_OFF );
      }
      buf.append( "</td></tr>\n" );
    }
    buf.append( "</td></tr>\n"
	+ "<tr><td>Bildwiederholspeicher:</td><td>" );
    EmuUtil.appendOnOffText( buf, this.videoEnabled );
    buf.append( "</td></tr>\n"
	+ "<tr><td>Bildausgabe:</td><td>" );
    boolean done = false;
    if( this.ramKRT != null ) {
      if( this.modeKRT ) {
	buf.append( "KRT-Grafik" );
	if( this.mode64x16 ) {
	  buf.append( " (64x16 Modus aktiv)" );
	}
	done = true;
      } else {
	buf.append( "Standard-BWS, " );
      }
    }
    if( !done ) {
      buf.append( this.mode64x16 ? "64x16" : "32x32" );
      buf.append( " Zeichen" );
      if( this.altFontEnabled ) {
	buf.append( ", alternativer Zeichensatz" );
      }
    }
    buf.append( "</td></tr>\n"
	+ "</table>\n" );
    if( this.graphicPoppe != null ) {
      buf.append( "<br/><br/>\n"
	+ "<h2>Zweite Anzeigeeinheit: Farbgrafikkarte</h2>\n"
	+ "<table border=\"1\">\n" );
      this.graphicPoppe.appendStatusHTMLTo( buf );
      buf.append( "</table>\n" );
    }
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );

    boolean state = isFixedScreenSize( props );
    if( state != this.fixedScreenSize ) {
      this.fixedScreenSize = state;
      this.screenFrm.fireScreenSizeChanged();
    }
    checkAddPCListener( props );
    loadFonts( props );
    applyUserPortSettings( props );
    if( this.graphicZX != null ) {
      this.graphicZX.applySettings( props );
    }
    if( this.graphicCCJena != null ) {
      this.graphicCCJena.applySettings( props );
    }
    if( this.graphicPoppe != null ) {
      this.graphicPoppe.applySettings( props );
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
		this.osFile,
		EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_OS_FILE ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.monCode,
		EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_MONITOR ) );
    }
    String extRomFile = EmuUtil.getProperty(
				props,
				this.propPrefix
					+ PROP_EXTROM_PREFIX
					+ PROP_FILE );
    if( rv && emulatesModuleBasic( props ) == (this.romBasic != null) ) {
      if( (this.romBasic != null)
	  && !TextUtil.equals( extRomFile, this.extRomFile ) )
      {
	rv = false;
      }
    } else {
      rv = false;
    }
    if( rv && emulatesModuleMegaROM( props ) == (this.romMega != null) ) {
      if( (this.romMega != null)
	  && !TextUtil.equals( extRomFile, this.extRomFile ) )
      {
	rv = false;
      }
    } else {
      rv = false;
    }
    if( rv && emulatesROM8000( props ) == (this.rom8000 != null) ) {
      if( (this.rom8000 != null)
	  && !TextUtil.equals( extRomFile, this.extRomFile ) )
      {
	rv = false;
      }
    } else {
      rv = false;
    }
    if( rv && (this.rom8000 == null) ) {
      if( emulatesPetersCard( props ) != this.petersCardEnabled ) {
	rv = false;
      }
    }
    if( rv && (getRAMEndAddr( props ) != this.ramEndAddr) ) {
      rv = false;
    }
    if( rv ) {
      rv = GIDE.complies( this.gide, props, this.propPrefix );
    }
    if( rv && emulatesFloppyDisk( props ) != (this.fdc != null) ) {
      rv = false;
    }
    if( rv && (emulatesRTC( props ) != (this.rtc != null)) ) {
      rv = false;
    }
    if( rv
	&& (emulatesGraphicCCJena( props ) != (this.graphicCCJena != null)) )
    {
      rv = false;
    }
    if( rv && (emulatesGraphicKRT( props ) != (this.ramKRT != null)) ) {
      rv = false;
    }
    if( rv
	&& (emulatesGraphicPoppe( props ) != (this.graphicPoppe != null)) )
    {
      rv = false;
    }
    if( rv && (emulatesGraphicZX( props ) != (this.graphicZX != null)) ) {
      rv = false;
    }
    if( rv && (emulatesK1520Sound( props ) != (this.k1520Sound != null)) ) {
      rv = false;
    }
    if( rv && (emulatesKCNet( props ) != (this.kcNet != null)) ) {
      rv = false;
    }
    if( rv && (emulatesVDIP( props ) != (this.vdip != null)) ) {
      rv = false;
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy1,
			"Z1013",
			RAMFloppy.RFType.MP_3_1988,
			props,
			PROP_PREFIX + PROP_RF1_PREFIX );
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy2,
			"Z1013",
			RAMFloppy.RFType.MP_3_1988,
			props,
			PROP_PREFIX + PROP_RF2_PREFIX );
    }
    if( rv && (this.userPort != getUserPort( props )) ) {
      rv = false;
    }
    return rv;
  }


  @Override
  public boolean canExtractScreenText()
  {
    return !this.modeKRT;
  }


  @Override
  public AbstractKeyboardFld<Z1013> createKeyboardFld()
					throws UnsupportedOperationException
  {
    AbstractKeyboardFld<Z1013> kbFld    = null;
    KeyboardMatrix             kbMatrix = this.keyboard.getKeyboardMatrix();
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
    if( this.ramFloppy1 != null ) {
      this.ramFloppy1.deinstall();
    }
    if( this.ramFloppy2 != null ) {
      this.ramFloppy2.deinstall();
    }

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeAddressListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.removeTStatesListener( this );
    if( this.fdc != null ) {
      this.fdc.die();
    }
    cpu.setInterruptSources( (Z80InterruptSource[]) null );

    this.pio.removePIOPortListener( this, Z80PIO.PortInfo.B );
    if( this.pcListenerAddrs != null ) {
      cpu.removePCListener( this );
    }
    if( this.gide != null ) {
      this.gide.die();
    }
    if( this.k1520Sound != null ) {
      this.k1520Sound.die();
    }
    if( this.kcNet != null ) {
      this.kcNet.die();
    }
    if( this.vdip != null ) {
      this.vdip.die();
    }
    super.die();
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
    if( (this.fixedScreenSize || this.screenFrm.isFullScreenMode())
	&& !this.mode64x16 )
    {
      x -= 128;
    }
    if( (x >= 0) && (x < (this.mode64x16 ? 512 : 256)) ) {
      int b = 0;
      if( (this.ramKRT != null) && this.modeKRT ) {
	int bank = y % (this.mode64x16 ? 16 : 8);
	if( (bank >= 0) && (bank < this.ramKRT.length) ) {
	  byte[] ram = this.ramKRT[ bank ];
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
  public CharRaster getCurScreenCharRaster()
  {
    CharRaster rv = null;
    if( this.mode64x16 ) {
      rv = new CharRaster( 64, 16, 16, 8, 8, 0, 0 );
    } else {
      rv = new CharRaster(
		32, 32,
		8, 8, 8,
		this.fixedScreenSize
			|| this.screenFrm.isFullScreenMode() ? 128 : 0,
		0 );
    }
    return rv;
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return FloppyDiskFormat.FMT_780K;
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

    int     rv     = 0xFF;
    boolean undone = true;
    if( (this.graphicZX != null)
	&& (addr >= 0x4000) && (addr < 0x5800)
	&& (this.ramEndAddr < 0x5800) )
    {
      /*
       * von der ZX-Spectrum-kompatiblen Grafikkarte wird nur gelesen,
       * wenn an der Stelle kein anderer RAM verfuegbar ist.
       */
      rv = this.graphicZX.read( addr );
      if( rv >= 0 ) {
	undone = false;
      } else {
	rv = 0xFF;
      }
    }
    if( undone && (addr >= 0xC000) && (addr < 0xEC00) ) {
      byte[] rom = this.romBasic;
      if( rom != null ) {
	int idx = addr - 0xC000;
	if( idx < rom.length ) {
	  rv     = (int) rom[ idx ] & 0xFF;
	  undone = false;
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
	  if( idx < this.romMega.length ) {
	    rv = (int) rom[ idx ] & 0xFF;
	  }
	  undone = false;
	}
      }
    }
    if( undone && this.videoEnabled
	&& (addr >= 0xEC00) && (addr < 0xF000) )
    {
      byte[] ram = null;
      if( (this.ramKRT != null) && this.modeKRT ) {
	if( (this.ramBankKRT >= 0)
	    && (this.ramBankKRT < this.ramKRT.length) )
	{
	  ram = this.ramKRT[ this.ramBankKRT ];
	}
      } else {
	ram = this.ramVideo;
      }
      if( ram != null ) {
	int idx = addr - 0xEC00;
	if( idx < ram.length ) {
	  rv = (int) ram[ idx ] & 0xFF;
	}
      }
      undone = false;
    }
    if( undone && this.romOSEnabled && (addr >= 0xF000) ) {
      byte[] rom = this.osBytes;
      if( rom != null ) {
	int idx = addr - 0xF000;
	if( idx < rom.length ) {
	  rv     = (int) rom[ idx ] & 0xFF;
	  undone = false;
	}
      }
    }
    if( undone && this.rom8000Enabled && (this.rom8000 != null)
	&& (addr >= 0x8000) )
    {
      int idx = addr - 0x8000;
      if( idx < this.rom8000.length ) {
	rv = (int) this.rom8000[ idx ] & 0xFF;
      }
      undone = false;
    }
    if( undone && (addr <= this.ramEndAddr) ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  @Override
  public int getResetStartAddress( boolean powerOn )
  {
    return 0xF000;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch  = -1;
    int idx = (chY * (this.mode64x16 ? 64 : 32)) + chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ] & 0xFF;
      if( (b >= 0x20) && (b < 0x7F) ) {
	ch = b;
      } else {
	if( this.altFontEnabled ) {
	  if( (b >= 0xA0) && (b < 0xFF) ) {
	    ch = b & 0x7F; 	// 0xA0 bis 0xFE: invertierte Zeichen
	  }
	} else {
	  if( (b >= 0x0E) && (b < 0x20) ) {
	    // Schachfiguren
	    idx = b - 0x0E;
	    if( (idx >= 0) && (idx < char14ToUnicode.length) ) {
	      ch = char14ToUnicode[ idx ];
	    }
	  } else {
	    // sonstige Grafikzeichen wie beim Z9001
	    ch = Z9001.toUnicode( b );
	  }
	}
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return (!this.fixedScreenSize
		&& !this.screenFrm.isFullScreenMode()
		&& this.mode64x16) ? 248 : 256;
  }


  @Override
  public int getScreenWidth()
  {
    return (this.mode64x16
		|| this.fixedScreenSize
		|| this.screenFrm.isFullScreenMode()) ? 512 : 256;
  }


  @Override
  public AbstractScreenDevice getSecondScreenDevice()
  {
    AbstractScreenDevice screenDevice = this.graphicZX;
    if( screenDevice == null ) {
      screenDevice = this.graphicCCJena;
    }
    if( screenDevice == null ) {
      screenDevice = this.graphicPoppe;
    }
    return screenDevice;
  }


  @Override
  public AbstractSoundDevice[] getSoundDevices()
  {
    return this.k1520Sound != null ?
	new AbstractSoundDevice[] { this.k1520Sound.getSoundDevice() }
	: super.getSoundDevices();
  }


  @Override
  public FloppyDiskInfo[] getSuitableFloppyDisks()
  {
    FloppyDiskInfo[] disks = null;
    if( this.fdc != null ) {
      if( this.graphicCCJena != null ) {
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
    return DEFAULT_SWAP_KEY_CHAR_CASE;
  }


  @Override
  public String getTitle()
  {
    return this.sysName;
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
  public void loadROMs( Properties props )
  {
    this.monCode = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_MONITOR );
    this.osFile  = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_OS_FILE );
    this.osBytes = readROMFile( this.osFile, 0x1000, "Monitorprogramm" );
    if( this.osBytes == null ) {
      if( this.monCode.equals( VALUE_MON_A2 ) ) {
	if( monA2 == null ) {
	  monA2 = readResource( "/rom/z1013/mon_a2.bin" );
	}
	this.osBytes = monA2;
      } else if( this.monCode.equals( VALUE_MON_RB_K7659 ) ) {
	if( monRB_K7659 == null ) {
	  monRB_K7659 = readResource( "/rom/z1013/mon_rb_k7659.bin" );
	}
	this.osBytes = monRB_K7659;
      } else if( this.monCode.equals( VALUE_MON_RB_S6009 ) ) {
	if( monRB_S6009 == null ) {
	  monRB_S6009 = readResource( "/rom/z1013/mon_rb_s6009.bin" );
	}
	this.osBytes = monRB_S6009;
      } else if( this.monCode.equals( VALUE_MON_INCOM_K7669 ) ) {
	if( monINCOM_K7669 == null ) {
	  monINCOM_K7669 = readResource( "/rom/z1013/mon_incom_k7669.bin" );
	}
	this.osBytes = monINCOM_K7669;
      } else if( this.monCode.equals( VALUE_MON_JM_1992 ) ) {
	if( monJM_1992 == null ) {
	  monJM_1992 = readResource( "/rom/z1013/mon_jm_1992.bin" );
	}
	this.osBytes = monJM_1992;
      } else if( this.monCode.equals( VALUE_MON_BL4_K7659 ) ) {
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
      this.romBasic = readExtROMFile( props, 0x2C00, "KC-BASIC-Modul" );
      if( this.romBasic == null ) {
	if( modBasic == null ) {
	  modBasic = readResource( "/rom/z1013/kcbasic.bin" );
	}
	this.romBasic = modBasic;
      }
    } else if( emulatesModuleMegaROM( props ) ) {
      this.romMega = readExtROMFile( props, 0x280000, "Mega-ROM-Modul" );
    } else if( emulatesROM8000( props ) ) {
      this.rom8000 = readExtROMFile(
				props,
				0x8000,
				"32K-ROM entsprechend Z1013-128" );
    }
    loadFonts( props );
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
	text  = SourceUtil.getBasicProgram(
					this.emuThread,
					0x2C01,
					basicTokens );
	break;

      case 2:
        bType = BasicType.KC_ROM;
	text  = SourceUtil.getBasicProgram(
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
  protected boolean pasteChar( char ch ) throws InterruptedException
  {
    boolean rv = false;
    if( this.pasteFast ) {
      if( (ch > 0) && (ch <= 0xFF) ) {
	if( ch == '\n' ) {
	  ch = '\r';
	}
	while( this.charToPaste != 0 ) {
	  Thread.sleep( 10 );
	}
	this.charToPaste = ch;
	rv = true;
      }
    } else {
      rv = super.pasteChar( ch );
    }
    return rv;
  }


  @Override
  public void informPastingTextStatusChanged( boolean pasting )
  {
    this.screenFrm.pastingTextStatusChanged( pasting );
    if( this.graphicCCJena != null ) {
      AbstractScreenFrm screenFrm = this.graphicCCJena.getScreenFrm();
      if( screenFrm != null ) {
	screenFrm.pastingTextStatusChanged( pasting );
      }
    }
    if( this.graphicPoppe != null ) {
      AbstractScreenFrm screenFrm = this.graphicPoppe.getScreenFrm();
      if( screenFrm != null ) {
	screenFrm.pastingTextStatusChanged( pasting );
      }
    }
  }


  @Override
  public int readIOByte( int port16, int tStates )
  {
    // wird von unbelegten Ports gelesen (Pull Down Widerstaende)
    int rv = 0;

    int portGroup = port16 & 0xF0;
    int port8     = port16 & 0xFF;
    if( (port8 == 4)
	&& (this.petersCardEnabled || (this.rom8000 != null)) )
    {
      rv = this.io4Value;
    }
    else if( (this.graphicCCJena != null) && (port8 == 0x18) ) {
      rv = this.graphicCCJena.readStatus();
    }
    else if( (this.graphicCCJena != null) && (port8 == 0x19) ) {
      rv = this.graphicCCJena.readData();
    }
    if( (this.graphicPoppe != null) && (portGroup == 0x10) ) {
      rv = this.graphicPoppe.readIOByte( port8 );
    }
    else if( (this.k1520Sound != null) && ((port8 & 0xF8) == 0x38) ) {
      rv = this.k1520Sound.read( port8, tStates );
    }
    if( (this.gide != null)
	&& ((portGroup == 0x40) || (portGroup == 0x80)) )
    {
      int value = this.gide.read( port16 );
      if( value >= 0 ) {
	rv = value;
      }
    }
    else if( (this.rtc != null) && (portGroup == 0x70) ) {
      rv = this.rtc.read( port16 );
    }
    else if( (this.ramFloppy1 != null) && ((port8 & 0xF8) == 0x98) ) {
      rv = this.ramFloppy1.readByte( port8 & 0x07 );
    }
    else if( (this.ramFloppy2 != null) && ((port8 & 0xF8) == 0x58) ) {
      rv = this.ramFloppy2.readByte( port8 & 0x07 );
    }
    else if( (this.kcNet != null) && (portGroup == 0xC0) ) {
      rv = this.kcNet.read( port16 );
    }
    else if( (this.vdip != null) && ((port8 & 0xFC) == 0xDC) ) {
      rv = this.vdip.read( port16 );
    }
    else if( portGroup == 0xF0 ) {
      switch( port8 ) {
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
	    rv = this.vdip.read( port16 );
	  }
	  break;
      }
    } else {
      // Adressleitungen ab A5 ignorieren
      switch( port8 & 0x1C ) {
	case 0:				// IOSEL0 -> PIO
	  switch( port8 & 0x03 ) {
	    case 0:
	      rv = this.pio.readDataA();
	      break;

	    case 2:
	      rv = this.pio.readDataB();
	      break;
	  }
	  break;

	case 0x0C:				// IOSEL3 -> Vollgrafik ein
	  if( this.ramKRT != null ) {
	    this.modeKRT = true;
	    this.screenFrm.setScreenDirty( true );
	  }
	  break;

	case 0x10:				// IOSEL4 -> Vollgrafik aus
	  if( this.ramKRT != null ) {
	    this.modeKRT = false;
	    this.screenFrm.setScreenDirty( true );
	  }
	  break;
      }
    }
    z80AddressChanged( port16 );
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
	buf.append( '0' );
      }
      buf.append( String.format( "%02XH", b ) );
      if( (b >= 0) && (b < sysCallNames.length) ) {
	appendSpacesToCol( buf, bol, colRemark );
	buf.append( ';' );
	buf.append( sysCallNames[ b ] );
      }
      buf.append( '\n' );
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
  public void reset( boolean powerOn, Properties props )
  {
    super.reset( powerOn, props );

    boolean old64x16 = this.mode64x16;

    this.centrTStatesToAck = 0;
    this.io4Value          = 0;
    this.joy0ActionMask    = 0;
    this.joy1ActionMask    = 0;
    this.lastWrittenAddr   = -1;
    this.romOSEnabled      = true;
    this.videoEnabled      = true;
    this.modeKRT           = false;
    this.romMegaSeg        = 0;
    this.rom8000Enabled    = EmuUtil.getBooleanProperty(
					props,
					this.propPrefix
						+ PROP_EXTROM_PREFIX
						+ PROP_8000_ON_RESET,
					false );
    if( this.petersCardEnabled ) {
      if( this.mode64x16 ) {
	this.mode64x16 = false;
	this.screenFrm.fireScreenSizeChanged();
      }
      if( this.mode4MHz ) {
	this.emuThread.updCPUSpeed( Main.getProperties() );
	this.mode4MHz = false;
      }
    }
    if( powerOn ) {
      if( this.ramEndAddr == 0x03FF ) {
	// bei Z1013.12 statischer RAM
	this.emuThread.initSRAM( props );
      } else {
	initDRAM();
      }
      if( this.ramKRT != null ) {
	for( int i = 0; i < this.ramKRT.length; i++ ) {
	  fillRandom( this.ramKRT[ i ] );
	}
      }
      fillRandom( this.ramVideo );
    }
    this.pio.reset( powerOn );
    this.keyboard.reset();
    if( this.graphicZX != null ) {
      this.graphicZX.reset( powerOn, props );
    }
    if( this.graphicCCJena != null ) {
      this.graphicCCJena.reset( powerOn, props );
    }
    if( this.graphicPoppe != null ) {
      this.graphicPoppe.reset( powerOn, props );
    }
    if( this.fdc != null ) {
      this.fdc.reset( powerOn );
    }
    if( this.floppyDiskDrives != null ) {
      for( int i = 0; i < this.floppyDiskDrives.length; i++ ) {
	FloppyDiskDrive drive = this.floppyDiskDrives[ i ];
	if( drive != null ) {
	  drive.reset();
	}
      }
    }
    if( this.gide != null ) {
      this.gide.reset();
    }
    if( this.k1520Sound != null ) {
      this.k1520Sound.reset( powerOn );
    }
    if( this.kcNet != null ) {
      this.kcNet.reset( powerOn );
    }
    if( this.vdip != null ) {
      this.vdip.reset( powerOn );
    }
    // sicherstellen, dass Drucker Empfamgsbereitschaft meldet
    if( this.userPort == UserPort.CENTR7_PRACTIC_2_1989 ) {
      this.pio.putInValuePortA( 0, 0x80 );
    }
  }


  @Override
  public void saveBasicProgram()
  {
    boolean           cancelled      = false;
    int               begAddr        = -1;
    int               endAddr        = -1;
    BasicType         z1013BasicType = null;
    SaveDlg.BasicType dstBasicType   = SaveDlg.BasicType.NO_BASIC;
    String            title          = "KC-BASIC-Programm speichern";
    int               preIdx         = -1;
    if( this.lastBasicType != null ) {
      switch( this.lastBasicType ) {
	case Z1013_TINY:
	  title  = "Tiny-BASIC-Programm speichern";
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
	  z1013BasicType = BasicType.Z1013_TINY;
	  dstBasicType   = SaveDlg.BasicType.TINYBASIC;
	  begAddr        = 0x1000;
	}
	break;

      case 1:
	endAddr = SourceUtil.getBasicEndAddr( this.emuThread, 0x2C01 );
	if( endAddr > 0x2C01 ) {
	  z1013BasicType = BasicType.KC_RAM;
	  dstBasicType   = SaveDlg.BasicType.KCBASIC;
	  begAddr        = 0x2C01;
	}
	break;

      case 2:
	endAddr = SourceUtil.getBasicEndAddr( this.emuThread, 0x0401 );
	if( endAddr > 0x0401 ) {
	  z1013BasicType = BasicType.KC_ROM;
	  dstBasicType   = SaveDlg.BasicType.KCBASIC;
	  begAddr        = 0x0401;
	}
	break;

      default:
	cancelled = true;
    }
    if( !cancelled ) {
      if( (begAddr > 0) && (endAddr > begAddr) ) {
	this.lastBasicType = z1013BasicType;
	(new SaveDlg(
		this.screenFrm,
		begAddr,
		endAddr,
		title,
		dstBasicType,
		null )).setVisible( true );
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

    boolean rv     = false;
    boolean undone = true;
    if( (this.graphicZX != null) && (addr >= 0x4000) && (addr < 0x5800) ) {
      if( this.graphicZX.write( addr, value ) ) {
	checkAndFireOpenSecondScreen();
	rv = true;
	if( this.ramEndAddr < 0x5800 ) {
	  /*
	   * nur auf erledigt setzen,
	   * wenn es keinen parallel liegenden RAM gibt
	   */
	  undone = false;
	}
      }
    }
    if( undone && (addr >= 0xC000) && (addr < 0xEC00) ) {
      if( (this.romBasic != null)
	  || ((this.romMega != null) && (addr < 0xE800)) )
      {
	undone = false;
      }
    }
    if( undone && this.videoEnabled
	&& (addr >= 0xEC00) && (addr < 0xF000) )
    {
      byte[] ram = null;
      if( (this.ramKRT != null) && this.modeKRT ) {
	if( (this.ramBankKRT >= 0)
	    && (this.ramBankKRT < this.ramKRT.length) )
	{
	  ram = this.ramKRT[ this.ramBankKRT ];
	}
      } else {
	ram = this.ramVideo;
      }
      if( ram != null ) {
	int idx = addr - 0xEC00;
	if( idx < ram.length ) {
	  ram[ idx ] = (byte) value;
	  this.screenFrm.setScreenDirty( true );
	  rv = true;
	}
      }
      undone = false;
    }
    if( undone && this.romOSEnabled && (this.osBytes != null)
	&& (addr >= 0xF000) )
    {
      if( addr < (0xF000 + this.osBytes.length) ) {
	undone = false;
      }
    }
    if( undone && this.rom8000Enabled && (this.rom8000 != null)
	&& (addr >= 0x8000) )
    {
      undone = false;
    }
    if( undone && (addr <= this.ramEndAddr) ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    if( (this.graphicPoppe != null)
	&& (addr >= 0xE800) && (addr < 0xF000) )
    {
      /*
       * Die Grafikkarte wird unabhaengig
       * von der Speicherkonfiguration auf der Z1013-Grundplatine
       * im Adressbereich E800h-EFFFh immer beschrieben.
       */
      this.graphicPoppe.writeMemByte( addr, value );
      if( addr < 0xEC00 ) {
	checkAndFireOpenSecondScreen();
      }
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    boolean rv = true;
    if( this.altFontEnabled ) {
      if( this.altFontBytes == fontAlt ) {
	rv = false;
      }
    } else {
      if( this.stdFontBytes == fontStd ) {
	rv = false;
      }
    }
    return rv;
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
    this.pio.putInValuePortB( this.tapeInPhase ? 0x40 : 0, 0x40 );
  }


  @Override
  public void updSysCells(
			int        begAddr,
			int        len,
			FileFormat fileFmt,
			int        fileType )
  {
    SourceUtil.updKCBasicSysCells(
			this.emuThread,
			begAddr,
			len,
			fileFmt,
			fileType );
  }


  @Override
  public void writeIOByte( int port16, int value, int tStates )
  {
    int portGroup = port16 & 0xF0;
    int port8     = port16 & 0xFF;
    if( port8 == 4 ) {
      if( this.petersCardEnabled ) {
	boolean oldAltFontEnabled = this.altFontEnabled;
	boolean oldMode64x16      = this.mode64x16;
	this.romOSEnabled         = ((value & 0x10) == 0);
	this.altFontEnabled       = ((value & 0x20) != 0);
	this.mode64x16            = ((value & 0x80) != 0);
	if( (this.altFontEnabled != oldAltFontEnabled)
	    || (this.mode64x16 != oldMode64x16) )
	{
	  this.screenFrm.setScreenDirty( true );
	}
	if( (this.mode64x16 != oldMode64x16)
	    && !this.fixedScreenSize
	    && !this.screenFrm.isFullScreenMode() )
	{
	  this.screenFrm.clearScreenSelection();
	  this.screenFrm.fireScreenSizeChanged();
	}
	this.io4Value = value & 0xF0;
      } else if( this.rom8000 != null ) {
	this.romOSEnabled   = ((value & 0x10) == 0);
	this.videoEnabled   = this.romOSEnabled;
	this.rom8000Enabled = ((value & 0x20) != 0);
	this.io4Value       = value & 0x60;
      }
      if( this.petersCardEnabled || (this.rom8000 != null) ) {
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
      }
    }
    else if( ((port8 == 0x18) || (port8 == 0x19))
	     && (this.graphicCCJena != null) )
    {
      if( port8 == 0x18 ) {
	this.graphicCCJena.writeArg( value );
      } else {
	this.graphicCCJena.writeCmd( value );
      }
      checkAndFireOpenSecondScreen();
    }
    if( (this.graphicPoppe != null) && (portGroup == 0x10) ) {
      this.graphicPoppe.writeIOByte( port8, value );
      checkAndFireOpenSecondScreen();
    }
    else if( (this.k1520Sound != null) && ((port8 & 0xF8) == 0x38) ) {
      this.k1520Sound.write( port8, value, tStates );
    }
    if( (this.gide != null)
	&& ((portGroup == 0x40) || (portGroup == 0x80)) )
    {
      this.gide.write( port16, value );
    }
    else if( (this.rtc != null) && (portGroup == 0x70) ) {
      this.rtc.write( port16, value );
    }
    else if( (this.ramFloppy1 != null) && ((port8 & 0xF8) == 0x98) ) {
      this.ramFloppy1.writeByte( port8 & 0x07, value );
    }
    else if( (this.ramFloppy2 != null) && ((port8 & 0xF8) == 0x58) ) {
      this.ramFloppy2.writeByte( port8 & 0x07, value );
    }
    else if( (this.kcNet != null) && (portGroup == 0xC0) ) {
      this.kcNet.write( port16, value );
    }
    else if( (this.vdip != null) && ((port8 & 0xFC) == 0xDC) ) {
      this.vdip.write( port16, value );
    }
    else if( portGroup == 0xF0 ) {
      switch( port8 ) {
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
	    if( port8 == 0xFF ) {
	      this.romMegaSeg = value;
	    }
	  } else {
	    if( this.vdip != null ) {
	      this.vdip.write( port16, value );
	    }
	  }
	  break;
      }
    } else {
      // Adressleitungen ab A5 ignorieren
      switch( port8 & 0x1C ) {
	case 0:				// IOSEL0 -> PIO
	  switch( port8 & 0x03 ) {
	    case 0:
	      this.pio.writeDataA( value );
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
	      this.pio.writeDataB( value );
	      break;

	    case 3:
	      this.pio.writeControlB( value );
	      break;
	  }
	  break;

	case 8:					// IOSEL2 -> Tastaturspalten
	  this.keyboard.setSelectedCol( value & 0x0F );
	  if( this.ramKRT != null ) {
	    value &= 0x0F;
	    if( value == 8 ) {
	      this.modeKRT = true;
	      this.screenFrm.setScreenDirty( true );
	    }
	    else if( value == 9 ) {
	      this.modeKRT = false;
	      this.screenFrm.setScreenDirty( true );
	    }
	    this.ramBankKRT = (value & 0x07);
	  }
	  break;

	case 0x0C:				// IOSEL3
	  if( this.ramKRT != null ) {
	    this.modeKRT = true;
	    this.screenFrm.setScreenDirty( true );
	  }
	  break;

	case 0x10:				// IOSEL4
	  if( this.ramKRT != null ) {
	    this.modeKRT = false;
	    this.screenFrm.setScreenDirty( true );
	  }
	  break;
      }
    }
    z80AddressChanged( port16 );
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    if( this.graphicZX != null ) {
      this.graphicZX.z80TStatesProcessed( cpu, tStates );
    }
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
    if( this.k1520Sound != null ) {
      this.k1520Sound.z80TStatesProcessed( cpu, tStates );
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


	/* --- private Methoden --- */

  private void applyUserPortSettings( Properties props )
  {
    this.userPort = getUserPort( props );
    if( this.userPort == UserPort.CENTR7_PRACTIC_2_1989 ) {
      this.pio.putInValuePortA( 0, 0x80 );	// Empfangsbereitschaft
    }
  }


  private synchronized void checkAddPCListener( Properties props )
  {
    this.pasteFast = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_PASTE_FAST,
				true );
    java.util.List<Integer> addrs = new ArrayList<>();
    if( this.pasteFast ) {
      String monText = EmuUtil.getProperty(
				props,
				this.propPrefix + PROP_MONITOR );
      if( monText.equals( VALUE_MON_A2 ) ) {
	addrs.add( 0xF119 );
      } else if( monText.equals( VALUE_MON_JM_1992 ) ) {
	addrs.add( 0xF25A );
      } else {
	addrs.add( 0xF130 );
      }
    }
    this.catchPrintCalls = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_CATCH_PRINT_CALLS,
				true );
    if( this.catchPrintCalls ) {
      addrs.add( 0xFFCA );
      addrs.add( 0xFFCD );
      addrs.add( 0xFFDF );
      addrs.add( 0xFFE5 );
      addrs.add( 0xFFE8 );
      addrs.add( 0xFFEB );
    }
    this.catchJoyCalls = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_CATCH_JOY_CALLS,
				true );
    if( this.catchJoyCalls ) {
      addrs.add( 0xFFBB );
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


  private boolean emulatesGraphicCCJena( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_GRA_CCJ_ENABLED,
			false );
  }


  private boolean emulatesGraphicKRT( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_GRA_KRT_ENABLED,
			false );
  }


  private boolean emulatesGraphicPoppe( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_GRA_POPPE_ENABLED,
			false );
  }


  private boolean emulatesGraphicZX( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_GRA_ZX_ENABLED,
			false );
  }


  private boolean emulatesModuleBasic( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix
				+ PROP_EXTROM_PREFIX
				+ PROP_BASIC_ENABLED,
			false );
  }


  private boolean emulatesModuleMegaROM( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix
				+ PROP_EXTROM_PREFIX
				+ PROP_MEGA_ENABLED,
			false );
  }


  private boolean emulatesPetersCard( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_PETERS_ENABLED,
			false );
  }


  private boolean emulatesROM8000( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix
				+ PROP_EXTROM_PREFIX
				+ PROP_8000_ENABLED,
			false );
  }


  private boolean emulatesRTC( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_RTC_ENABLED,
			false );
  }


  private static int getRAMEndAddr( Properties props )
  {
    int rv = 0xFFFF;
    switch( EmuUtil.getProperty( props, EmuThread.PROP_SYSNAME ) ) {
      case SYSNAME_Z1013_01:
      case SYSNAME_Z1013_16:
	rv = 0x3FFF;
	break;
      case SYSNAME_Z1013_12:
	rv = 0x03FF;
	break;
    }
    return rv;
  }


  private static UserPort getUserPort( Properties props )
  {
    UserPort userPort = UserPort.NONE;
    String   text     = EmuUtil.getProperty(
				props,
				PROP_PREFIX + PROP_USERPORT );
    if( text.equals( VALUE_JOYST_JUTE0687 ) ) {
      userPort = UserPort.JOY_JUTE_6_1987;
    } else if( text.equals( VALUE_JOYST_PRAC0487 ) ) {
      userPort = UserPort.JOY_PRACTIC_4_1987;
    } else if( text.equals( VALUE_JOYST_PRAC0188 ) ) {
      userPort = UserPort.JOY_PRACTIC_1_1988;
    } else if( text.equals( VALUE_CENTR7_PRAC0289 ) ) {
      userPort = UserPort.CENTR7_PRACTIC_2_1989;
    } else if( text.equals( VALUE_CENTR8_FA1090 ) ) {
      userPort = UserPort.CENTR8_FA_10_1990;
    }
    return userPort;
  }


  private boolean isFixedScreenSize( Properties props )
  {
    return this.petersCardEnabled
	   && EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_FIXED_SCREEN_SIZE,
			false );
  }


  private void loadFonts( Properties props )
  {
    this.stdFontBytes = readFontByProperty(
				props,
				this.propPrefix + PROP_FONT_FILE,
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
    if( this.graphicCCJena != null ) {
      this.graphicCCJena.loadFontByProperty(
				props,
				this.propPrefix
					+ PROP_GRA2_FONT_PREFIX
					+ PROP_FILE );
    }
    if( this.graphicPoppe != null ) {
      this.graphicPoppe.load8x6FontByProperty(
				props,
				this.propPrefix
					+ PROP_GRA2_FONT_PREFIX
					+ PROP_FILE );
      this.graphicPoppe.set8x8FontBytes( this.stdFontBytes);
    }
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
    int joySel     = ~this.pio.fetchOutValuePortA( value );
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


  private byte[] readExtROMFile(
			Properties props,
			int        maxLen,
			String     objName )
  {
    this.extRomFile = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_EXTROM_PREFIX + PROP_FILE );
    return readROMFile( this.extRomFile, maxLen, objName );
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
