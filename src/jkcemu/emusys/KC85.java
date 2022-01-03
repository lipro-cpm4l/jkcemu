/*
 * (c) 2008-2022 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Computer HC900 und KC85/2..5
 */

package jkcemu.emusys;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.audio.AbstractSoundDevice;
import jkcemu.audio.AudioOut;
import jkcemu.base.AutoInputCharSet;
import jkcemu.base.BaseDlg;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.SourceUtil;
import jkcemu.base.UserCancelException;
import jkcemu.disk.FloppyDiskDrive;
import jkcemu.disk.FloppyDiskFormat;
import jkcemu.disk.FloppyDiskInfo;
import jkcemu.emusys.kc85.AbstractKC85KeyboardFld;
import jkcemu.emusys.kc85.AbstractKC85Module;
import jkcemu.emusys.kc85.D004;
import jkcemu.emusys.kc85.D005KeyboardFld;
import jkcemu.emusys.kc85.D008;
import jkcemu.emusys.kc85.KC85CharRecognizer;
import jkcemu.emusys.kc85.KC85JoystickModule;
import jkcemu.emusys.kc85.KC85KeyboardFld;
import jkcemu.emusys.kc85.KC85FrontFld;
import jkcemu.emusys.kc85.KC85PlainRAMModule;
import jkcemu.emusys.kc85.KC85ROM8KModule;
import jkcemu.emusys.kc85.KC85SegmentedRAMModule;
import jkcemu.emusys.kc85.M003;
import jkcemu.emusys.kc85.M006;
import jkcemu.emusys.kc85.M008;
import jkcemu.emusys.kc85.M011;
import jkcemu.emusys.kc85.M021;
import jkcemu.emusys.kc85.M025;
import jkcemu.emusys.kc85.M028;
import jkcemu.emusys.kc85.M033;
import jkcemu.emusys.kc85.M035;
import jkcemu.emusys.kc85.M040;
import jkcemu.emusys.kc85.M041Sub;
import jkcemu.emusys.kc85.M045;
import jkcemu.emusys.kc85.M046;
import jkcemu.emusys.kc85.M047;
import jkcemu.emusys.kc85.M048;
import jkcemu.emusys.kc85.M052;
import jkcemu.emusys.kc85.M066;
import jkcemu.etc.CPUSynchronSoundDevice;
import jkcemu.etc.PSGSoundDevice;
import jkcemu.file.FileFormat;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;
import jkcemu.usb.VDIP;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80CTCListener;
import z80emu.Z80InterruptSource;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80MemView;
import z80emu.Z80Memory;
import z80emu.Z80PIO;
import z80emu.Z80PIOPortListener;
import z80emu.Z80TStatesListener;


public class KC85 extends EmuSys implements
					Z80CTCListener,
					Z80MaxSpeedListener,
					Z80PIOPortListener
{
  public static final String SYSNAME_HC900  = "HC900";
  public static final String SYSNAME_KC85_2 = "KC85_2";
  public static final String SYSNAME_KC85_3 = "KC85_3";
  public static final String SYSNAME_KC85_4 = "KC85_4";
  public static final String SYSNAME_KC85_5 = "KC85_5";

  public static final String SYSTEXT_HC900  = "HC900";
  public static final String SYSTEXT_KC85_2 = "KC85/2";
  public static final String SYSTEXT_KC85_3 = "KC85/3";
  public static final String SYSTEXT_KC85_4 = "KC85/4";
  public static final String SYSTEXT_KC85_5 = "KC85/5";

  public static final String PROP_PREFIX_HC900  = "jkcemu.hc900.";
  public static final String PROP_PREFIX_KC85_2 = "jkcemu.kc85_2.";
  public static final String PROP_PREFIX_KC85_3 = "jkcemu.kc85_3.";
  public static final String PROP_PREFIX_KC85_4 = "jkcemu.kc85_4.";
  public static final String PROP_PREFIX_KC85_5 = "jkcemu.kc85_5.";

  public static final String PROP_DISKSTATION = "diskstation";
  public static final String PROP_DISKSTATION_MODULE_COUNT
					= "diskstation.module_count";
  public static final String PROP_DISKSTATION_ROM 
					= "diskstation.rom";
  public static final String PROP_DISKSTATION_MAXSPEED_KHZ
					= "diskstation.maxspeed.khz";

  public static final String PROP_NAME             = "name";
  public static final String PROP_TYPEBYTE         = "typebyte";
  public static final String PROP_MODULE_PREFIX    = "module.";
  public static final String PROP_ROM_BASIC_FILE   = "rom.basic.file";
  public static final String PROP_ROM_CAOS_C_FILE  = "rom.caos_c.file";
  public static final String PROP_ROM_CAOS_E_FILE  = "rom.caos_e.file";
  public static final String PROP_ROM_CAOS_F_FILE  = "rom.caos_f.file";
  public static final String PROP_ROM_M052_FILE    = "rom.m052.file";
  public static final String PROP_ROM_M052USB_FILE = "rom.m052usb.file";

  public static final String PROP_EMULATE_VIDEO_TIMING
					= "emulate_video_timing";

  public static final String PROP_KEYS_DIRECT_TO_BUFFER
					 = "keys.direct_to_buffer";

  public static final String VALUE_D004   = "D004";
  public static final String VALUE_D008   = "D008";
  public static final String VALUE_ROM_20 = "2.0";
  public static final String VALUE_ROM_35 = "3.5";

  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE              = true;
  public static final int     DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX_2 = 5000;
  public static final int     DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX_4 = 3000;

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
    "PAPER",     "AT",       "COLOR",   "SOUND",	// 0xE0
    "PSET",      "PRESET",   "BLOAD",   "VPEEK",
    "VPOKE",     "LOCATE",   "KEYLIST", "KEY",
    "SWITCH",    "PTEST",    "CLOSE",   "OPEN",
    "RANDOMIZE", "VGET$",    "LINE",    "CIRCLE",	// 0xF0
    "CSRLIN",    "DEVICE",   "FILES",   "CHDIR" };

  private static final int DEFAULT_SPEED_2_KHZ = 1750;
  private static final int DEFAULT_SPEED_4_KHZ = 1773;
  private static final int SUTAB               = 0xB7B0;
  private static final int SCREEN_WIDTH        = 320;
  private static final int SCREEN_HEIGHT       = 256;

  private static final FloppyDiskInfo[] availableFloppyDisks = {
		new FloppyDiskInfo(
			"/disks/kc85/kc85caos.dump.gz",
			"KC85 CAOS Systemdiskette",
			2, 2048, true ),
		new FloppyDiskInfo(
			"/disks/kc85/kc85microdos.dump.gz",
			"KC85 MicroDOS Systemdiskette",
			2, 2048, true ) };

  private static final int[][] rawRGBValues = {

			// primaere Vordergrundfarben
			{   0,   0,   0 },	// schwarz
			{   0,   0, 255 },	// blau
			{ 255,   0,   0 },	// rot
			{ 255,   0, 255 },	// purpur
			{   0, 255,   0 },	// gruen
			{   0, 255, 255 },	// tuerkis
			{ 255, 255,   0 },	// gelb
			{ 255, 255, 255 },	// weiss

			// Vordergrundfarben mit gemischten Primaerfarben
			{   0,   0,   0 },	// schwarz
			{ 160,   0, 255 },	// violett
			{ 255, 160,   0 },	// orange
			{ 255,   0, 160 },	// purpurrot
			{   0, 255, 160 },	// gruenblau
			{   0, 160, 255 },	// blaugruen
			{ 160, 255,   0 },	// gelbgruen
			{ 255, 255, 255 },	// weiss

			// Hintergrundfarben (dunkler)
			{   0,   0,   0 },	// schwarz
			{   0,   0, 160 },	// blau
			{ 160,   0,   0 },	// rot
			{ 160,   0, 160 },	// purpur
			{   0, 160,   0 },	// gruen
			{   0, 160, 160 },	// tuerkis
			{ 160, 160,   0 },	// gelb
			{ 160, 160, 160 } };	// weiss

  /*
   * Beim KC85/2..5 weicht der Zeichensatz vom ASCII-Standard etwas ab.
   * Deshalb werden hier nur die Tastencodes gemappt,
   * die auf dem emulierten Rechner zur Anzeige des inhaltlich
   * gleichen Zeichens fuehren.
   *
   * Konkret bestehen folgende Unterschiede:
   *
   * HEX  ASCII  Mapping  KC
   * 5B   [               Vollzeichen
   * 5C   \        +-->   |
   * 5D   ]        |      Negationszeichen
   * 60   `        |      Copyright
   * 7B   {        |      Umlaut ae
   * 7C   |      --+      Umlaut oe
   * 7D   }               Umlaut ue
   * 7E   ~               Umlaut sz
   */
  private static int[] char2KeyNum = {
	 -1,  24,  41,  60,  61, 115,  -1,  -1,		// 0x00
	  6, 122, 118, 120,   9, 126, 127,  25,
	  8, 121, 119,  76,  57,  -1, 114,  -1,		// 0x10
	123,   7,  56,  77,  86,  87,  -1,  40,
	 70, 117,   5,  21, 101,  37,  85,  53,		// 0x20   !"#$%&'
	 69,  59,  27, 104,  74,  10,  90, 106,		// 0x28  ()*+,-./
	 42, 116,   4,  20, 100,  36,  84,  52,		// 0x30  01234567
	 68,  58,  26, 105,  75,  11,  91, 107,		// 0x38  89:;<=>?
	 43,   2,  94, 110,  98,  16,  34,  82,		// 0x40  @ABCDEFG
	 50,  64,  66,  72,  88,  78,  62,  54,		// 0x48  HIJKLMNO
	 38, 112,  96,  18,  32,  48,  46,   0,		// 0x50  PQRSTUVW
	 30,  14,  80,  -1,  -1,  -1,  22, 102,		// 0x58  XYZ   ^_
	 -1,   3,  95, 111,  99,  17,  35,  83,		// 0x60   abcdefg
	 51,  65,  67,  73,  89,  79,  63,  55,		// 0x68  hijklmno
	 39, 113,  97,  19,  33,  49,  47,   1,		// 0x70  pqrstuvw
	 31,  15,  81,  -1, 103,  -1,  -1,  -1 };	// 0x78  xyz |

  private static final int PRE_F1_MASK = (124 << 8) & 0xFF00;

  private static final String[] cpuFlags = {
			"NZ", "Z", "NC", "C", "PO", "PE", "P", "M" };

  private static final String[] stdSysCallNames = {
			"CRT",   "MBO",   "UOT1",  "UOT2",
			"KBD",   "MBI",   "USIN1", "USIN2",
			"ISRO",  "CSRO",  "ISRI",  "CSRI",
			"KBDS",  "BYE",   "KBDZ",  "COLOR",
			"LOAD",  "VERIF", "LOOP",  "NORM",
			"WAIT",  "LARG",  "INTB",  "INLIN",
			"RHEX",  "ERRM",  "HLHX",  "HLDE",
			"AHEX",  "ZSUCH", "SOUT",  "SIN",
			"NOUT",  "NIN",   "GARG",  "OSTR",
			"OCHR",  "CUCP",  "MODU",  "JUMP",
			"LDMA",  "LDAM",  "BRKT",  "SPACE",
			"CRLF",  "HOME",  "MODI",  "PUDE",
			"PUSE",  "SIXD",  "DABR",  "TCIF",
			"PADR",  "TON",   "SAVE",  "MBIN",
			"MBOUT", "KEY",   "KEYLI", "DISP",
			"WININ", "WINAK", "LINE",  "CIRCLE",
			"SQR",   "MULT",  "CSTBT", "INIEA",
			"INIME", "ZKOUT", "MENU",  "V24OUT",
			"V24DUP" };

  private static final String[] pv7SysCallNames = {
			"MBO",    "MBI",  "ISRO",  "CSRO",
			"ISRI",   "CSRI", "DRVER", "DRV:USR",
			"DIRANZ", "CD",   "ERA",   "REN" };

  private static AutoInputCharSet   autoInputCharSet = null;
  private static Map<String,byte[]> resources        = new HashMap<>();
  private static Boolean            defaultEmulateVideoTiming = null;

  private int                     kcTypeNum;
  private String                  sysName;
  private String                  basicFile;
  private String                  caosFileC;
  private String                  caosFileE;
  private String                  caosFileF;
  private String                  m052RomFile;
  private String                  m052usbRomFile;
  private Boolean                 kout;
  private volatile boolean        blinkEnabled;
  private volatile boolean        blinkState;
  private volatile boolean        hiColorRes;
  private boolean                 charSetUnknown;
  private boolean                 keyDirectToBuf;
  private boolean                 pasteFast;
  private boolean                 basicC000Enabled;
  private boolean                 caosC000Enabled;
  private boolean                 caosE000Enabled;
  private boolean                 irmEnabled;
  private boolean                 ram0Enabled;
  private boolean                 ram0Writeable;
  private boolean                 ram4Enabled;
  private boolean                 ram4Writeable;
  private boolean                 ram8Enabled;
  private boolean                 ram8Writeable;
  private boolean                 ramColorEnabled;
  private boolean                 screenDirty;
  private boolean                 screenRefreshEnabled;
  private boolean                 screen1Enabled;
  private volatile boolean        screen1Visible;
  private boolean                 biState;
  private boolean                 h4State;
  private boolean                 soundPhaseL;
  private boolean                 soundPhaseR;
  private int                     ram8SegNum;
  private int                     keyNumStageBuf;
  private int                     keyNumStageNum;
  private long                    keyNumStageMillis;
  private volatile int            keyNumPressed;
  private int                     keyNumProcessing;
  private int                     keyShiftBitCnt;
  private int                     keyShiftValue;
  private int                     keyTStates;
  private int                     lastIX;
  private volatile int            tStatesLinePos0;
  private volatile int            tStatesLinePos1;
  private volatile int            tStatesLinePos2;
  private volatile int            tStatesPerLine;
  private int                     lineTStateCounter;
  private int                     lineCounter;
  private int                     basicSegNum;
  private byte[]                  basicC000;
  private byte[]                  caosC000;
  private byte[]                  caosE000;
  private byte[]                  caosF000;
  private byte[]                  screenBufUsed;
  private byte[]                  screenBufSaved;
  private byte[]                  ram8;
  private byte[]                  ramColor0;
  private byte[]                  ramColor1;
  private byte[]                  ramPixel0;
  private byte[]                  ramPixel1;
  private int[]                   rgbValues;
  private Color[]                 colors;
  private AbstractKC85Module[]    modules;
  private KC85CharRecognizer      charRecognizer;
  private AbstractKC85KeyboardFld keyboardFld;
  private KC85FrontFld            frontFld;
  private Z80PIO                  pio;
  private Z80CTC                  ctc;
  private KC85JoystickModule      joyModule;
  private CPUSynchronSoundDevice  d001SoundDevice;
  private PSGSoundDevice          m066SoundDevice;
  private VDIP[]                  vdips;
  private D004                    d004;


  public KC85( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, "" );
    this.tapeOutPhase   = false;
    this.soundPhaseL    = false;
    this.soundPhaseR    = false;
    this.charSetUnknown = false;
    this.keyDirectToBuf = false;
    this.pasteFast      = false;
    this.keyboardFld    = null;
    this.frontFld       = null;
    this.basicFile      = null;
    this.caosFileC      = null;
    this.caosFileE      = null;
    this.caosFileF      = null;
    this.m052RomFile    = null;
    this.m052usbRomFile = null;

    this.sysName = EmuUtil.getProperty( props, EmuThread.PROP_SYSNAME );
    switch( this.sysName ) {
      case SYSNAME_HC900:
	this.kcTypeNum  = 2;
	this.propPrefix = PROP_PREFIX_HC900;
	break;
      case SYSNAME_KC85_2:
	this.kcTypeNum  = 2;
	this.propPrefix = PROP_PREFIX_KC85_2;
	break;
      case SYSNAME_KC85_3:
	this.kcTypeNum  = 3;
	this.propPrefix = PROP_PREFIX_KC85_3;
	break;
      case SYSNAME_KC85_4:
	this.kcTypeNum  = 4;
	this.propPrefix = PROP_PREFIX_KC85_4;
	break;
      default:
	this.kcTypeNum  = 5;
	this.propPrefix = PROP_PREFIX_KC85_5;
    }

    // emulierte Hardware konfigurieren
    this.ram8 = null;
    if( this.kcTypeNum == 4 ) {
      this.ram8 = new byte[ 0x8000 ];		// 32K
    } else if( this.kcTypeNum > 4 ) {
      this.ram8 = new byte[ 0x38000 ];		// 224K
    }
    this.ramColor0 = new byte[ 0x4000 ];
    this.ramColor1 = new byte[ 0x4000 ];
    this.ramPixel0 = new byte[ 0x4000 ];
    this.ramPixel1 = new byte[ 0x4000 ];

    Z80CPU cpu       = emuThread.getZ80CPU();
    this.ctc         = new Z80CTC( "CTC (E/A-Adressen 8Ch-8Fh)" );
    this.pio         = new Z80PIO( "PIO (E/A-Adressen 88h-8Bh)" );
    this.joyModule   = null;
    this.d004        = null;
    if( emulatesD008( props ) ) {
      this.d004 = new D008( this, props, this.propPrefix );
    } else if( emulatesD004( props ) ) {
      this.d004 = new D004( this, props, this.propPrefix );
    }
    this.d001SoundDevice = new CPUSynchronSoundDevice(
				"Tongeneratoren im Grundger\u00E4t",
				true,
				false );
    this.m066SoundDevice = null;
    this.vdips           = new VDIP[ 0 ];
    this.modules         = createModules( props );
    if( this.modules != null ) {
      try {
	java.util.List<Z80InterruptSource> iSources = new ArrayList<>();
	iSources.add( this.ctc );
	iSources.add( this.pio );
	for( int i = 0; i < this.modules.length; i++ ) {
	  AbstractKC85Module module = this.modules[ i ];
	  if( (module instanceof M066)
	      && (this.m066SoundDevice == null) )
	  {
	    this.m066SoundDevice = ((M066) module).getSoundDevice();
	  }
	  if( module instanceof Z80InterruptSource ) {
	    iSources.add( (Z80InterruptSource) module );
	  }
	  if( module instanceof Z80MaxSpeedListener ) {
	    ((Z80MaxSpeedListener) module).z80MaxSpeedChanged( cpu );
	    cpu.addMaxSpeedListener( (Z80MaxSpeedListener) module );
	  }
	  if( module instanceof Z80TStatesListener ) {
	    cpu.addTStatesListener( (Z80TStatesListener) module );
	  }
	}
	cpu.setInterruptSources(
		iSources.toArray(
			new Z80InterruptSource[ iSources.size() ] ) );
      }
      catch( ArrayStoreException ex ) {}
    } else {
      cpu.setInterruptSources( this.ctc, this.pio );
    }
    this.ctc.addCTCListener( this );
    this.pio.addPIOPortListener( this, Z80PIO.PortInfo.A );
    this.pio.addPIOPortListener( this, Z80PIO.PortInfo.B );
    cpu.addTStatesListener( this );
    cpu.addMaxSpeedListener( this );

    this.screenBufUsed  = null;
    this.screenBufSaved = null;
    this.rgbValues      = new int[ rawRGBValues.length ];
    this.colors         = new Color[ rawRGBValues.length ];
    this.charRecognizer = new KC85CharRecognizer();
    applySettings( props );
    z80MaxSpeedChanged( cpu );
  }


  public static AutoInputCharSet getAutoInputCharSet()
  {
    if( autoInputCharSet == null ) {
      autoInputCharSet = new AutoInputCharSet();
      autoInputCharSet.addCharRange( '\u0020', '\u005A' );
      autoInputCharSet.addChar( '\u005E' );
      autoInputCharSet.addChar( '\u005F' );
      autoInputCharSet.addCharRange( '\u0061', '\u007A' );
      autoInputCharSet.addEnterChar();
      for( int i = 1; i <= 6; i++ ) {
	autoInputCharSet.addKeyChar( 0xF0 + i, String.format( "F%d", i ) );
      }
      autoInputCharSet.addKeyChar(  3, "BRK" );
      autoInputCharSet.addKeyChar( 19, "STOP" );
      autoInputCharSet.addKeyChar( 26, "INS" );
      autoInputCharSet.addKeyChar( 31, "DEL" );
      autoInputCharSet.addKeyChar(  1, "CLR" );
      autoInputCharSet.addKeyChar( 16, "HOME" );
      autoInputCharSet.addCursorChars();
    }
    return autoInputCharSet;
  }


  public static FloppyDiskInfo[] getAvailableFloppyDisks()
  {
    return availableFloppyDisks;
  }


  /*
   * Da die Option zur Emulation des Video-Timings viel Rechenzeit benoetigt,
   * soll sie standardmaessig nur dann aktiviert werden,
   * wenn genuegend Rechenleistung zur Verfuegung steht.
   * Als Indikator dient die Anzahl der zur Verfuegung stehenden Prozessoren
   * bzw- Prozessorkerne.
   */
  public static boolean getDefaultEmulateVideoTiming()
  {
    return (Runtime.getRuntime().availableProcessors() >= 2);
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    int rv = DEFAULT_SPEED_4_KHZ;
    switch( EmuUtil.getProperty( props, EmuThread.PROP_SYSNAME ) ) {
      case SYSNAME_HC900:
      case SYSNAME_KC85_2:
      case SYSNAME_KC85_3:
	rv = DEFAULT_SPEED_2_KHZ;
	break;
    }
    return rv;
  }


  public int getKCTypeNum()
  {
    return this.kcTypeNum;
  }


  public static int getRawColorCount()
  {
    return rawRGBValues.length;
  }


  public static int getRawRGB( int colorIdx )
  {
    int rv = 0xFF000000;
    if( (colorIdx >= 0) && (colorIdx < rawRGBValues.length) ) {
      int[] rgb = rawRGBValues[ colorIdx ];
      if( rgb.length >= 3 ) {
	rv |= ((rgb[ 0 ] << 16) & 0x00FF0000);
	rv |= ((rgb[ 1 ] << 8)  & 0x0000FF00);
	rv |= (rgb[ 2 ] & 0x000000FF);
      }
    }
    return rv;
  }


  /*
   * Die Methode teilt dem KC85-System den Code
   * der aktuell gedrueckten Taste mit.
   * -1 steht fuer keine Taste gedrueckt.
   * Bei Zwei-Byte-Tastencodes steht das erste Byte in den Bits 8 bis 15.
   */
  public void setKeyNumPressed( int keyNum )
  {
    this.keyNumPressed = keyNum;
  }


  public void setFrontFldDirty()
  {
    if( this.frontFld != null )
      this.frontFld.setScreenDirty( true );
  }


	/* --- Z80CTCListener --- */

  /*
   * CTC-Ausgaenge:
   *   Kanal 0: Tonausgabe rechts
   *   Kanal 1: Tonausgabe links
   *   Kanal 2: Blinken
   */
  @Override
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc ) {
      switch( timerNum ) {
	case 0:
	  this.soundPhaseR = !this.soundPhaseR;
	  updSoundValues();
	  break;

	case 1:
	  this.soundPhaseL  = !this.soundPhaseL;
	  this.tapeOutPhase = this.soundPhaseL;
	  updSoundValues();
	  break;

	case 2:
	  this.blinkState = !this.blinkState;
	  if( this.screenBufUsed != null ) {
	    this.screenDirty = true;
	  } else {
	    this.screenFrm.setScreenDirty( true );
	  }
	  break;
      }
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    this.d001SoundDevice.z80MaxSpeedChanged( cpu );

    int f = (this.kcTypeNum < 4 ? DEFAULT_SPEED_2_KHZ : DEFAULT_SPEED_4_KHZ);
    int t = cpu.getMaxSpeedKHz() * 112 / f;
    this.tStatesLinePos0 = (int) Math.round( t * 32.0 / 112.0 );
    this.tStatesLinePos1 = (int) Math.round( t * 64.0 / 112.0 );
    this.tStatesLinePos2 = (int) Math.round( t * 96.0 / 112.0 );
    this.tStatesPerLine  = t;
  }


	/* --- Z80PIOPortListener --- */

  @Override
  public void z80PIOPortStatusChanged(
				Z80PIO          pio,
				Z80PIO.PortInfo port,
				Z80PIO.Status   status )
  {
    if( (pio == this.pio)
	&& ((status == Z80PIO.Status.OUTPUT_AVAILABLE)
	    || (status == Z80PIO.Status.OUTPUT_CHANGED)) )
    {
      if( port == Z80PIO.PortInfo.A ) {			// IO-Adresse 88h
	int m = this.pio.fetchOutValuePortA( 0xFF );
	this.caosE000Enabled  = ((m & 0x01) != 0);
	this.ram0Enabled      = ((m & 0x02) != 0);
	this.irmEnabled       = ((m & 0x04) != 0);
	this.ram0Writeable    = ((m & 0x08) != 0);
	this.basicC000Enabled = ((m & 0x80) != 0);

	boolean kout    = ((m & 0x10) != 0);
	Boolean oldKout = this.kout;
	if( this.kcTypeNum < 4 ) {
	  if( oldKout == null ) {
	    oldKout = Boolean.TRUE;
	  }
	  if( oldKout.booleanValue() && !kout ) {
	    this.emuThread.getZ80CPU().fireNMI();
	  }
	} else {
	  if( this.keyboardFld != null ) {
	    if( this.kout != null ) {
	      if( kout != this.kout.booleanValue() ) {
		this.keyboardFld.fireKOut();
	      }
	    } else {
	      this.keyboardFld.fireKOut();
	    }
	  }
	}
	this.kout = kout;

	if( this.screenBufUsed != null ) {
	  this.screenDirty = true;
	} else {
	  this.screenFrm.setScreenDirty( true );
	}
	if( this.frontFld != null ) {
	  this.frontFld.setPioAValue( m );
	}
      }
      else if( port == Z80PIO.PortInfo.B ) {		// IO-Adresse 89h
	int m = this.pio.fetchOutValuePortB( 0xFF );
	if( this.kcTypeNum > 3 ) {
	  if( (m & 0x01) == 0 ) {
	    this.tapeOutPhase = false;
	    this.soundPhaseL  = false;
	    this.soundPhaseR  = false;
	    updSoundValues();
	  }
	  this.ram8Enabled   = ((m & 0x20) != 0);
	  this.ram8Writeable = ((m & 0x40) != 0);
	  if( this.frontFld != null ) {
	    this.frontFld.setRAM8Enabled( this.ram8Enabled );
	  }
	}
	this.blinkEnabled = ((m & 0x80) != 0);
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    if( cpu == this.emuThread.getZ80CPU() ) {
      int pioA = this.pio.fetchOutValuePortA( 0xFF );

      // Status Grundgeraet
      buf.append( "<h1>" );
      EmuUtil.appendHTML( buf, getTitle() );
      buf.append( " Status</h1>\n"
		+ "<table border=\"1\">\n" );
      if( this.kcTypeNum >= 4 ) {
	buf.append( "<tr><td>CAOS ROM C:</td><td>" );
	EmuUtil.appendOnOffText( buf, this.caosC000Enabled );
	buf.append( "</td></tr>\n" );
      }
      buf.append( "<tr><td>CAOS ROM" );
      if( this.kcTypeNum >= 4 ) {
	buf.append( " E" );
      }
      buf.append( ":</td><td>" );
      EmuUtil.appendOnOffText( buf, this.caosE000Enabled );
      buf.append( "</td></tr>\n" );
      if( this.kcTypeNum >= 3 ) {
	buf.append( "<tr><td>BASIC" );
	if( this.kcTypeNum > 4 ) {
	  buf.append( "/USER" );
	}
	buf.append( "-ROM:</td><td>" );
	if( this.basicC000Enabled ) {
	  if( (this.kcTypeNum >= 4) && this.caosC000Enabled ) {
	    buf.append( "von CAOS C &uuml;berdeckt" );
	  } else {
	    if( this.kcTypeNum > 4 ) {
	      buf.append( "Bank " );
	      buf.append( this.basicSegNum );
	      buf.append( '\u0020' );
	    }
	    buf.append( EmuUtil.TEXT_ON );
	  }
	} else {
	  buf.append( EmuUtil.TEXT_OFF );
	}
	buf.append( "</td></tr>\n" );
      }
      buf.append( "<tr><td>RAM 0:</td><td>" );
      if( this.ram0Enabled ) {
	buf.append( EmuUtil.TEXT_ON );
	if( !this.ram0Writeable ) {
	  buf.append( "(schreibgesch&uuml;tzt)" );
	}
      } else {
	buf.append( EmuUtil.TEXT_OFF );
      }
      buf.append( "</td></tr>\n" );
      if( this.kcTypeNum >= 4 ) {
	buf.append( "<tr><td>RAM 4:</td><td>" );
	if( this.ram4Enabled ) {
	  buf.append( EmuUtil.TEXT_ON );
	  if( !this.ram4Writeable ) {
	    buf.append( "(schreibgesch&uuml;tzt)" );
	  }
	} else {
	  buf.append( EmuUtil.TEXT_OFF );
	}
	buf.append( "</td></tr>\n"
		+ "<tr><td>RAM 8:</td><td>" );
	if( this.ram8Enabled ) {
	  if( this.kcTypeNum > 4 ) {
	    buf.append( "Bank " );
	    buf.append( this.ram8SegNum );
	    buf.append( '\u0020' );
	  }
	  buf.append( EmuUtil.TEXT_ON );
	  if( !this.ram8Writeable ) {
	    buf.append( "(schreibgesch&uuml;tzt)" );
	  }
	  if( this.irmEnabled ) {
	    buf.append( ", aber vom IRM &uuml;berdeckt" );
	  }
	} else {
	  buf.append( EmuUtil.TEXT_OFF );
	}
	buf.append( "</td></tr>\n" );
      }
      buf.append( "<tr><td>IRM:</td><td>" );
      if( this.irmEnabled ) {
	if( this.kcTypeNum >= 4 ) {
	  buf.append( "Bank " );
	  buf.append( this.screen1Enabled ? "1" : "0" );
	  buf.append( this.ramColorEnabled ? " Farb" : " Pixel" );
	  buf.append( "ebene " );
	}
	buf.append( EmuUtil.TEXT_ON );
      } else {
	buf.append( EmuUtil.TEXT_OFF );
      }
      buf.append( "</td></tr>\n" );
      if( this.kcTypeNum >= 4 ) {
	buf.append( "<tr><td>Bildausgabe:</td><td>IRM-Bank " );
	buf.append( this.screen1Visible ? "1" : "0" );
	buf.append( "</td></tr>\n"
		+ "<tr><td>Hohe Farbaufl&ouml;sung:</td><td>" );
	EmuUtil.appendOnOffText( buf, this.hiColorRes );
	buf.append( "</td></tr>\n" );
      }
      buf.append( "<tr><td>Blinken:</td><td>" );
      EmuUtil.appendOnOffText( buf, this.blinkEnabled );
      buf.append( "</td></tr>\n" );
      if( this.kcTypeNum >= 4 ) {
	buf.append( "<tr><td>K&nbsp;OUT:</td><td>" );
	buf.append( (pioA >> 4) & 0x01 );
	buf.append( "</td></tr>\n" );
      }
      buf.append( "<tr><td>Motorschaltspannung:</td><td>" );
      EmuUtil.appendOnOffText( buf, (pioA & 0x40) != 0 );
      buf.append( "</td></tr>\n"
		+ "<tr><td>" );
      buf.append( this.kcTypeNum >= 4 ? "SYSTEM" : "TAPE" );
      buf.append( "-LED:</td><td>" );
      EmuUtil.appendOnOffText( buf, (pioA & 0x20) != 0 );
      buf.append( "</td></tr>\n"
		+ "</table>\n" );

      // Status der Module
      AbstractKC85Module[] modules  = this.modules;
      if( modules != null ) {
	if( modules.length > 0 ) {
	  buf.append( "<h2>Module</h2>\n"
		+ "<table border=\"1\">\n"
		+ "<tr><th>Schacht</th><th>Modul</th><th>Strukturbyte</th>"
		+ "<th>Status</th><th>Adresse</th><th>Segment</th>"
		+ "<th>Sonstiges</th></tr>\n" );
	  for( AbstractKC85Module module : modules ) {
	    buf.append( "<tr><td>" );
	    buf.append( String.format( "%02Xh", module.getSlot() ) );
	    buf.append( "</td><td>" );
	    buf.append( module.getModuleName() );
	    buf.append( "</td><td>" );
	    String s = module.getTypeByteText();
	    if( s != null ) {
	      buf.append( s );
	    }
	    buf.append( "</td><td>" );
	    if( module.isSwitchable() ) {
	      if( module.isEnabled() ) {
		Boolean readWrite = module.getReadWrite();
		if( readWrite != null ) {
		  buf.append( readWrite.booleanValue() ? "RW" : "RO" );
		} else {
		  buf.append( EmuUtil.TEXT_ON );
		}
	      } else {
		buf.append( EmuUtil.TEXT_OFF );
	      }
	    } else {
	      buf.append( EmuUtil.TEXT_ON + " (nicht schaltbar)");
	    }
	    buf.append( "</td><td>" );
	    int begAddr = module.getBegAddr();
	    if( begAddr >= 0 ) {
	      buf.append( String.format( "%04Xh", begAddr ) );
	    }
	    buf.append( "</td><td>" );
	    int segNum = module.getSegmentNum();
	    if( segNum >= 0 ) {
	      buf.append( segNum );
	    }
	    buf.append( "</td><td>" );
	    module.appendEtcInfoHTMLTo( buf );
	    buf.append( "</td></tr>\n" );
	  }
	  buf.append( "</table>\n" );
	}
      }
    }
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    if( EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PROP_EMULATE_VIDEO_TIMING,
			getDefaultEmulateVideoTiming() ) )
    {
      if( this.screenBufSaved == null ) {
	this.screenBufSaved = new byte[ 320 * 256 ];
      }
      this.screenBufUsed = this.screenBufSaved;
    } else {
      this.screenBufUsed = null;
    }
    createColors( props );
    applyPasteFast( props );
    if( this.d004 != null ) {
      this.d004.applySettings( props );
    }
    for( VDIP vdip : this.vdips ) {
      vdip.applySettings( props );
    }
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean hasDiskStation = (emulatesD004( props )
					|| emulatesD008( props ));
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( this.sysName );
    if( rv && (this.kcTypeNum > 2)
	&& !TextUtil.equals(
		this.basicFile,
		getProperty( props, PROP_ROM_BASIC_FILE ) ) )
    {
      rv = false;
    }
    if( rv && (this.kcTypeNum > 3)
	&& !TextUtil.equals(
		this.caosFileC,
		getProperty( props, PROP_ROM_CAOS_C_FILE ) ) )
    {
      rv = false;
    }
    if( rv && !TextUtil.equals(
		this.caosFileE,
		getProperty( props, PROP_ROM_CAOS_E_FILE ) ) )
    {
      rv = false;
    }
    if( rv && !TextUtil.equals(
		this.caosFileF,
		getProperty( props, PROP_ROM_CAOS_F_FILE ) ) )
    {
      rv = false;
    }
    if( rv && !TextUtil.equals(
		this.m052RomFile,
		getProperty( props, PROP_ROM_M052_FILE ) ) )
    {
      rv = false;
    }
    if( rv && !TextUtil.equals(
		this.m052usbRomFile,
		getProperty( props, PROP_ROM_M052USB_FILE ) ) )
    {
      rv = false;
    }
    if( rv ) {
      if( this.d004 != null ) {
	rv = this.d004.canApplySettings( props );
      } else {
	if( hasDiskStation ) {
	  rv = false;
	}
      }
    }
    if( rv ) {
      /*
       * Module vergleichen,
       * Dabei M035x4 in 4 x M035 und M041 in 2 x M041Sub umwandeln
       */
      if( props != null ) {
	int nRemain = EmuUtil.getIntProperty(
				props,
				this.propPrefix
					+ PROP_MODULE_PREFIX
					+ PROP_COUNT,
				0 );
	int nD004Modules = EmuUtil.getIntProperty(
				props,
				this.propPrefix
					+ PROP_MODULE_PREFIX
					+ PROP_DISKSTATION_MODULE_COUNT,
				0 );
	java.util.List<String[]> modEntries = new ArrayList<>( nRemain + 1 );
	boolean                  loop       = true;
	boolean                  firstM052  = true;
	int                      slot       = 8;
	while( loop
	       && (nRemain > 0)
	       && ((slot < 0xF0) || ((this.d004 != null) && (slot < 0x100))) )
	{
	  loop = false;

	  if( nRemain == nD004Modules ) {
	    slot = 0xF0;
	  }
	  String prefix = String.format(
				"%s%s%02X.",
				this.propPrefix,
				PROP_MODULE_PREFIX,
				slot );
	  String modName = props.getProperty( prefix + PROP_NAME );
	  if( modName != null ) {
	    if( !modName.isEmpty() ) {
	      String typeByte = props.getProperty( prefix + PROP_TYPEBYTE );
	      String fileName = props.getProperty( prefix + PROP_FILE );
	      int    nItems   = 1;
	      if( modName.equals( "M035x4" ) ) {
		modName = "M035";
		nItems  = 4;
	      } else if( modName.equals( "M041" ) ) {
		modName = M041Sub.MODULE_NAME;
		nItems  = 2;
	      } else if( modName.equals( "M052" ) ) {
		if( firstM052 ) {
		  firstM052 = false;
		  fileName  = this.m052RomFile;
		} else {
		  fileName  = this.m052usbRomFile;
		}
	      }
	      for( int i = 0; i < nItems; i++ ) {
		String[] modEntry = new String[ 4 ];
		modEntry[ 0 ]     = String.valueOf( slot + i );
		modEntry[ 1 ]     = modName;
		modEntry[ 2 ]     = typeByte;
		modEntry[ 3 ]     = fileName;
		modEntries.add( modEntry );
	      }
	      --nRemain;
	      slot += 4;
	      loop = true;
	    }
	  }
	}
	if( hasDiskStation && (this.d004 != null) ) {
	  String[] modEntry = new String[ 4 ];
	  modEntry[ 0 ]     = String.valueOf( this.d004.getSlot() );
	  modEntry[ 1 ]     = this.d004.getModuleName();
	  modEntry[ 2 ]     = this.d004.getTypeByteText();
	  modEntry[ 3 ]     = null;
	  modEntries.add( modEntry );
	}
	AbstractKC85Module[] modules = this.modules;
	if( modules != null ) {
	  if( modEntries.size() == modules.length ) {
	    for( int i = 0; i < modules.length; i++ ) {
	      String[] modEntry = modEntries.get( i );
	      if( !modules[ i ].equalsModule(
					modEntry[ 0 ],
					modEntry[ 1 ],
					modEntry[ 2 ],
					modEntry[ 3 ] ) )
	      {
		rv = false;
		break;
	      }
	    }
	  } else {
	    rv = false;
	  }
	} else {
	  if( !modEntries.isEmpty() ) {
	    rv = false;
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public AbstractKC85KeyboardFld createKeyboardFld()
					throws UserCancelException
  {
    if( this.kcTypeNum >= 4 ) {
      switch( BaseDlg.showOptionDlg(
		this.screenFrm,
		"Welche Tastatur m\u00F6chten Sie sehen,\n"
			+ "die originale oder die D005?",
		"Tastaturauswahl",
		"Original",
		"D005",
		EmuUtil.TEXT_CANCEL ) )
      {
	case 0:
	  this.keyboardFld = new KC85KeyboardFld( this );
	  break;
	case 1:
	  this.keyboardFld = new D005KeyboardFld( this );
	  break;
	default:
	  throw new UserCancelException();
      }
    } else {
      this.keyboardFld = new KC85KeyboardFld( this );
    }
    return this.keyboardFld;
  }


  @Override
  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    this.pio.removePIOPortListener( this, Z80PIO.PortInfo.B );
    this.pio.removePIOPortListener( this, Z80PIO.PortInfo.A );
    this.ctc.removeCTCListener( this );
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.d004 != null ) {
      this.d004.fireStop();
      this.d004.die();
    }
    if( this.modules != null ) {
      for( int i = 0; i < this.modules.length; i++ ) {
	AbstractKC85Module module = this.modules[ i ];
	if( module instanceof Z80TStatesListener ) {
	  cpu.removeTStatesListener( (Z80TStatesListener) module );
	}
	if( module instanceof Z80MaxSpeedListener ) {
	  cpu.removeMaxSpeedListener( (Z80MaxSpeedListener) module );
	}
	module.die();
      }
    }
    this.d001SoundDevice.fireStop();
    super.die();
  }


  @Override
  public boolean getAutoLoadInputOnSoftReset()
  {
    return false;
  }


  @Override
  public int getBasicMemByte( int addr )
  {
    return getMemByteInternal( addr, false, false, false );
  }


  @Override
  public int getBorderColorIndex()
  {
    return 0;	// schwarz
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = null;
    if( this.colors != null ) {
      if( (colorIdx >= 0) && (colorIdx < this.colors.length) ) {
	color = this.colors[ colorIdx ];
      }
    }
    return color != null ? color : super.getColor( colorIdx );
  }


  @Override
  public int getColorCount()
  {
    return rgbValues.length;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int    rv        = 0;
    byte[] screenBuf = this.screenBufUsed;
    if( screenBuf != null ) {
      int idx = (y * 320) + x;
      if( (idx >= 0) && (idx < screenBuf.length) ) {
	rv = (int) screenBuf[ idx ];
      }
    } else {
      int col = x / 8;
      if( this.kcTypeNum > 3 ) {
	boolean screen1  = this.screen1Visible;
	byte[]  ramPixel = screen1Visible ? this.ramPixel1 : this.ramPixel0;
	byte[]  ramColor = screen1Visible ? this.ramColor1 : this.ramColor0;
	int    idx       = (col * 256) + y;
	if( (idx >= 0) && (idx < ramPixel.length) ) {
	  int p = ramPixel[ idx ];
	  int c = ramColor[ idx ];
	  int m = 0x80;
	  int n = x % 8;
	  if( n > 0 ) {
	    m >>= n;
	  }
	  boolean pState = ((p & m) != 0);
	  if( this.hiColorRes ) {
	    if( this.blinkEnabled && this.blinkState && ((c & 0x80) != 0) ) {
	      pState = false;
	    }
	    if( pState ) {
	      if( (c & m) != 0 ) {
		rv = 7;			// weiss
	      } else {
		rv = 2;			// rot
	      }
	    } else {
	      if( (c & m) != 0 ) {
		rv = 5;			// tuerkis
	      } else {
		rv = 0;			// schwarz
	      }
	    }
	  } else {
	    rv = getColorIndex( c, pState );
	  }
	}
      } else {
	int pIdx = -1;
	int cIdx = -1;
	if( col < 32 ) {
	  pIdx = ((y << 5) & 0x1E00)
		| ((y << 7) & 0x0180)
		| ((y << 3) & 0x0060)
		| (col & 0x001F);
	  cIdx = 0x2800 | ((y << 3) & 0x07E0) | (col & 0x001F);
	} else {
	  pIdx = 0x2000
		| ((y << 3) & 0x0600)
		| ((y << 7) & 0x0180)
		| ((y << 3) & 0x0060)
		| ((y >> 1) & 0x0018)
		| (col & 0x0007);
	  cIdx = 0x3000
		| ((y << 1) & 0x0180)
		| ((y << 3) & 0x0060)
		| ((y >> 1) & 0x0018)
		| (col & 0x0007);
	}
	if( (pIdx >= 0) && (pIdx < this.ramPixel0.length)
	    && (cIdx >= 0) && (cIdx < this.ramPixel0.length) )
	{
	  int p = this.ramPixel0[ pIdx ];
	  int c = this.ramPixel0[ cIdx ];
	  int m = 0x80;
	  int n = x % 8;
	  if( n > 0 ) {
	    m >>= n;
	  }
	  rv = getColorIndex( c, (p & m) != 0 );
	}
      }
    }
    return rv;
  }


  @Override
  public boolean getConvertKeyCharToISO646DE()
  {
    return false;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    copyPixelsToCharRecognizer();
    return this.charRecognizer.recognizeCharRaster();
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    FloppyDiskFormat rv = FloppyDiskFormat.FMT_780K_I3;
    if( this.d004 != null ) {
      if( this.d004 instanceof D008 ) {
	rv = FloppyDiskFormat.FMT_1738K_I3_DS;
      }
    }
    return rv;
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return this.pasteFast ? 0 : super.getDelayMillisAfterPasteChar();
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return this.pasteFast ? 0 : super.getDelayMillisAfterPasteEnter();
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return this.pasteFast ? 0 : super.getHoldMillisPasteChar();
  }


  @Override
  public String getHelpPage()
  {
    return "/help/kc85.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    return getMemByteInternal( addr, this.irmEnabled, m1, false );
  }


  @Override
  public int getResetStartAddress( boolean powerOn )
  {
    return powerOn ? 0xF000 : 0xE000;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int col, int row )
  {
    copyPixelsToCharRecognizer();
    return this.charRecognizer.getChar( chRaster, row, col );
  }


  @Override
  public int getScreenHeight()
  {
    return SCREEN_HEIGHT;
  }


  @Override
  public int getScreenWidth()
  {
    return SCREEN_WIDTH;
  }


  @Override
  public KC85FrontFld getSecondScreenDevice()
  {
    if( this.frontFld == null ) {
      this.frontFld = new KC85FrontFld(
				this,
				this.d004,
				this.modules,
				Main.getProperties() );
      this.frontFld.setPioAValue( this.pio.fetchOutValuePortA( 0xFF ) );
    }
    return this.frontFld;
  }


  @Override
  public String getSecondSystemName()
  {
    return this.d004 != null ? this.d004.getModuleName() : null;
  }


  @Override
  public Z80CPU getSecondZ80CPU()
  {
    return this.d004 != null ? this.d004.getZ80CPU() : null;
  }


  @Override
  public Z80Memory getSecondZ80Memory()
  {
    return this.d004 != null ? this.d004.getZ80Memory() : null;
  }


  @Override
  public AbstractSoundDevice[] getSoundDevices()
  {
    return this.m066SoundDevice != null ?
		new AbstractSoundDevice[] {
				this.d001SoundDevice,
				this.m066SoundDevice }
		: new AbstractSoundDevice[] { this.d001SoundDevice };
  }


  @Override
  public FloppyDiskInfo[] getSuitableFloppyDisks()
  {
    return this.d004 != null ? availableFloppyDisks : null;
  }


  @Override
  public int getSupportedFloppyDiskDriveCount()
  {
    return this.d004 != null ?
		this.d004.getSupportedFloppyDiskDriveCount()
		: 0;
  }


  @Override
  public int getSupportedJoystickCount()
  {
    return this.joyModule != null ? 1 : 0;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return DEFAULT_SWAP_KEY_CHAR_CASE;
  }


  @Override
  public String getTitle()
  {
    String rv = SYSTEXT_KC85_5;
    switch( this.sysName ) {
      case SYSNAME_HC900:
	rv = SYSTEXT_HC900;
	break;
      case SYSNAME_KC85_2:
	rv = SYSTEXT_KC85_2;
	break;
      case SYSNAME_KC85_3:
	rv = SYSTEXT_KC85_3;
	break;
      case SYSNAME_KC85_4:
	rv = SYSTEXT_KC85_4;
	break;
    }
    return rv;
  }


  @Override
  public VDIP[] getVDIPs()
  {
    return this.vdips;
  }


  @Override
  public boolean hasKCBasicInROM()
  {
    return (this.kcTypeNum > 2);
  }


  @Override
  public boolean isAutoScreenRefresh()
  {
    return this.screenBufUsed != null;
  }


  @Override
  public boolean isSecondSystemRunning()
  {
    return this.d004 != null ? this.d004.isRunning() : false;
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv           = false;
    int     pcKeyTabAddr = getKeyTabAddrIfPCMode();
    boolean pcMode       = (pcKeyTabAddr >= 0);
    int     ch           = -1;
    int     keyNum       = -1;
    switch( keyCode ) {
      case KeyEvent.VK_BACK_SPACE:
	keyNum = (pcMode ? 40 : 24);
	break;

      case KeyEvent.VK_LEFT:
	keyNum = (shiftDown ? 7 : 6);
	break;

      case KeyEvent.VK_RIGHT:
	keyNum = (shiftDown ? 123 : 122);
	break;

      case KeyEvent.VK_DOWN:
	keyNum = (shiftDown ? 119 : 118);
	break;

      case KeyEvent.VK_UP:
	keyNum = (shiftDown ? 121 : 120);
	break;

      case KeyEvent.VK_END:
	keyNum = 123;
	break;

      case KeyEvent.VK_PAGE_DOWN:
	keyNum = 119;
	break;

      case KeyEvent.VK_PAGE_UP:
	keyNum = 121;
	break;

      case KeyEvent.VK_ENTER:
	keyNum = (shiftDown ? 127 : 126);
	break;

      case KeyEvent.VK_HOME:
	keyNum = (shiftDown ? 9 : 8);
	break;

      case KeyEvent.VK_INSERT:
	keyNum = (shiftDown ? 57 : 56);
	break;

      case KeyEvent.VK_TAB:
	keyNum = (pcMode ? 71 : 115);
	break;

      case KeyEvent.VK_ESCAPE:
	keyNum = (pcMode ? 125 : 77);
	break;

      case KeyEvent.VK_DELETE:
	if( pcMode ) {
	  if( ctrlDown ) {
	    keyNum |= PRE_F1_MASK;
	  } else {
	    keyNum = 24;
	  }
	} else {
	  keyNum = (shiftDown ? 41 : 40);
	}
	break;

      case KeyEvent.VK_SPACE:
	keyNum = (shiftDown ? 71 : 70);
	if( pcMode && ctrlDown ) {
	  keyNum |= PRE_F1_MASK;
	}
	break;

      case KeyEvent.VK_F1:
	keyNum = (shiftDown ? 125 : 124);
	if( pcMode ) {
	  ch = (shiftDown ? 0xF7 : 0xF1);
	}
	break;

      case KeyEvent.VK_F2:
	keyNum = (shiftDown ? 13 : 12);
	break;

      case KeyEvent.VK_F3:
	keyNum = (shiftDown ? 29 : 28);
	break;

      case KeyEvent.VK_F4:
	keyNum = (shiftDown ? 109 : 108);
	if( pcMode && ctrlDown ) {
	  keyNum |= PRE_F1_MASK;
	}
	break;

      case KeyEvent.VK_F5:
	keyNum = (shiftDown ? 45 : 44);
	if( pcMode && ctrlDown ) {
	  keyNum |= PRE_F1_MASK;
	}
	break;

      case KeyEvent.VK_F6:
	keyNum = (shiftDown ? 93 : 92);
	break;

      case KeyEvent.VK_F7:			// BRK
	keyNum = (shiftDown ? 61 : 60);
	break;

      case KeyEvent.VK_F8:			// STOP
	keyNum = (shiftDown ? 77 : 76);
	break;

      case KeyEvent.VK_F9:			// CLR
	keyNum = (shiftDown ? 25 : 24);
	break;

      case KeyEvent.VK_F11:			// LIST
	keyNum = 86;
	break;

      case KeyEvent.VK_F12:			// RUN
	keyNum = 87;
	break;

      case KeyEvent.VK_1:			// 1
	if( pcMode && ctrlDown ) {
	  keyNum = PRE_F1_MASK | 52;		// F1 7
	}
	break;

      case KeyEvent.VK_2:			// 2
	if( pcMode && ctrlDown ) {
	  keyNum = PRE_F1_MASK | 68;		// F1 8
	}
    }
    if( keyNum >= 0 ) {
      if( this.keyDirectToBuf ) {
	if( ch <= 0 ) {
	  ch = keyNumToChar( keyNum, pcKeyTabAddr, ctrlDown );
	}
	if( ch > 0 ) {
	  rv = putCharToKeyBuffer( (char) ch );
	}
      }
      if( !rv ) {
	setKeyNumPressed( keyNum );
      }
      updKeyboardFld( keyNum );
      rv = true;
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    this.keyNumPressed = -1;
    updKeyboardFld( -1 );
    if( this.keyDirectToBuf ) {
      int ix = getRegIX();
      if( ix >= 0 ) {
	setMemByte( ix + 13, 0 );
	setMemByte( ix + 8, getMemByte( ix + 8, false ) & 0xFE );
      }
    }
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv           = false;
    int     pcKeyTabAddr = getKeyTabAddrIfPCMode();
    boolean pcMode       = (pcKeyTabAddr >= 0);
    int     keyNum       = charToKeyNum( ch, pcMode );
    if( keyNum >= 0 ) {
      if( this.keyDirectToBuf ) {
	int ch1 = ch;
	if( keyNum == (keyNum & 0xFF) ) {
	  ch1 = keyNumToChar( keyNum, pcKeyTabAddr, ch < '\u0020' );
	} else {
	  if( ch > 0 ) {
	    ch1 = TextUtil.umlautToISO646DE( ch );
	  }
	}
	if( ch1 > 0 ) {
	  rv = putCharToKeyBuffer( (char) ch1 );
	}
      }
      if( !rv ) {
	setKeyNumPressed( keyNum );
	rv = true;
      }
    } else {
      if( (ch > 0) && this.keyDirectToBuf ) {
	rv = putCharToKeyBuffer( TextUtil.umlautToISO646DE( ch ) );
      }
    }
    updKeyboardFld( keyNum );
    return rv;
  }


  /*
   * KC-BASIC-Programme sollen bei Adressen ab 8000h nicht in den
   * IRM geladen werden, auch wenn dieser gerade eingeblendet ist.
   */
  @Override
  public void loadIntoMem(
			int           begAddr,
			byte[]        dataBytes,
			int           dataOffs,
			int           dataLen,
			FileFormat    fileFmt,
			int           fileType,
			StringBuilder rvStatusMsg )
  {
    if( dataBytes != null ) {
      boolean done       = false;
      boolean irmEnabled = this.irmEnabled;
      if( fileFmt != null ) {
	if( fileFmt.equals( FileFormat.KCB )
	    || fileFmt.equals( FileFormat.KCTAP_BASIC_PRG )
	    || fileFmt.equals( FileFormat.KCTAP_BASIC_DATA )
	    || fileFmt.equals( FileFormat.KCTAP_BASIC_ASC )
	    || fileFmt.equals( FileFormat.KCBASIC_HEAD_PRG )
	    || fileFmt.equals( FileFormat.KCBASIC_HEAD_DATA )
	    || fileFmt.equals( FileFormat.KCBASIC_HEAD_ASC )
	    || fileFmt.equals( FileFormat.KCBASIC_PRG ) )
	{
	  irmEnabled = false;
	}
	else if( fileFmt.equals( FileFormat.COM ) && (this.d004 != null) ) {
	  this.d004.loadIntoRAM( dataBytes, dataOffs, begAddr );
	  if( rvStatusMsg != null ) {
	    int endAddr = begAddr + dataLen - 1;
	    rvStatusMsg.append(
		String.format(
			"Datei in %s nach %04X-%04X geladen",
			this.d004.getModuleName(),
			begAddr,
			endAddr > 0xFFFF ? 0xFFFF : endAddr ) );
	  }
	  done = true;
	}
      }
      if( !done ) {
	int n   = dataLen;
	int dst = begAddr;
	while( (dataOffs < dataBytes.length)
	       && (dst < 0x10000)
	       && (n > 0) )
	{
	  setMemByteInternal( dst++, dataBytes[ dataOffs++ ], irmEnabled );
	  --n;
	}
	updSysCells( begAddr, dataLen, fileFmt, fileType );
      }
    }
  }


  @Override
  public void loadIntoSecondSystem( byte[] dataBytes, int begAddr )
  {
    if( this.d004 != null )
      this.d004.loadIntoRAM( dataBytes, 0, begAddr );
  }


  @Override
  public void loadROMs( Properties props )
  {
    String resourceBasic = null;
    String resourceCaosC = null;
    String resourceCaosE = null;
    String resourceCaosF = null;
    if( this.kcTypeNum == 2 ) {
      if( this.sysName.equals( SYSNAME_HC900 ) ) {
	resourceCaosE = "/rom/kc85/hc900_e000.bin";
	resourceCaosF = "/rom/kc85/hc900_f000.bin";
      } else {
	resourceCaosE = "/rom/kc85/caos22_e000.bin";
	resourceCaosF = "/rom/kc85/caos22_f000.bin";
      }
    } else if( this.kcTypeNum == 3 ) {
      resourceBasic = "/rom/kc85/basic_c000.bin";
      resourceCaosE = "/rom/kc85/caos31_e000.bin";
    } else if( this.kcTypeNum == 4 ) {
      resourceBasic = "/rom/kc85/basic_c000.bin";
      resourceCaosC = "/rom/kc85/caos42_c000.bin";
      resourceCaosE = "/rom/kc85/caos42_e000.bin";
    } else {
      resourceBasic = "/rom/kc85/user48_c000.bin";
      resourceCaosC = "/rom/kc85/caos48_c000.bin";
      resourceCaosE = "/rom/kc85/caos48_e000.bin";
    }

    // BASIC-ROM
    this.basicFile = null;
    this.basicC000 = null;
    if( this.kcTypeNum > 2 ) {
      this.basicFile = getProperty( props, PROP_ROM_BASIC_FILE );
      this.basicC000 = readROMFile(
				this.basicFile,
				this.kcTypeNum > 4 ? 0x8000 : 0x2000,
				"BASIC-ROM" );
    }
    if( this.basicC000 == null ) {
      this.basicC000 = getResource( resourceBasic );
    }

    // CAOS-ROM C
    this.caosFileC = null;
    this.caosC000  = null;
    if( this.kcTypeNum >= 4 ) {
      /*
       * Beim KC85/4 ist der ROM C normalerweise nur 4 KByte gross.
       * Hier werden aber bis zu 8 KByte erlaubt,
       * da der Emulator absichtlich auch die Moeglichkeit bietet,
       * auf diese Art und Weise einen 8 KByte grossen ROM C zu emulieren.
       */
      this.caosFileC = getProperty( props, PROP_ROM_CAOS_C_FILE );
      this.caosC000  = readROMFile(
				this.caosFileC,
				0x2000,
				"CAOS-ROM C" );
    }
    if( this.caosC000 != null ) {
      this.charSetUnknown = true;
    } else {
      this.caosC000 = getResource( resourceCaosC );
    }

    // CAOS-ROM E
    this.caosFileE = getProperty( props, PROP_ROM_CAOS_E_FILE );
    this.caosE000  = readROMFile(
				this.caosFileE,
				this.kcTypeNum < 3 ? 0x0800 : 0x2000,
				"CAOS-ROM E oder E+F" );
    if( this.caosE000 != null ) {
      this.charSetUnknown = true;
    } else {
      this.caosE000 = getResource( resourceCaosE );
    }

    // CAOS-ROM F
    this.caosFileF = getProperty( props, PROP_ROM_CAOS_F_FILE );
    this.caosF000  = readROMFile(
				this.caosFileF,
				this.kcTypeNum < 3 ? 0x0800 : 0x1000,
				"CAOS-ROM F" );
    if( this.caosF000 != null ) {
      this.charSetUnknown = true;
    }
    if( (this.caosE000 != null) && (this.caosF000 == null) ) {
      if( this.caosE000.length > 0x1000 ) {
	this.caosF000 = new byte[ this.caosE000.length - 0x1000 ];
	System.arraycopy(
			this.caosE000,
			0x1000,
			this.caosF000,
			0,
			this.caosF000.length );
      }
    }
    if( this.caosF000 == null ) {
      this.caosF000 = getResource( resourceCaosF );
    }

    // M052-ROM
    this.m052RomFile    = getProperty( props, PROP_ROM_M052_FILE );
    this.m052usbRomFile = getProperty( props, PROP_ROM_M052USB_FILE );

    // Module
    AbstractKC85Module[] modules  = this.modules;
    if( modules != null ) {
      for( AbstractKC85Module module : modules ) {
	module.reload( this.screenFrm );
      }
    }
  }


  @Override
  public void openBasicProgram()
  {
    SourceUtil.openKCBasicProgram(
				this.screenFrm,
				getKCBasicBegAddr(),
				basicTokens );
  }


  @Override
  protected boolean pasteChar( char ch ) throws InterruptedException
  {
    boolean rv = false;
    if( this.pasteFast || this.keyDirectToBuf ) {
      if( (ch > 0) && (ch <= 0xFF) ) {
	if( ch == '\n' ) {
	  ch = '\r';
	}
	int ix = getRegIX();
	if( ix >= 0 ) {
	  if( (getMemByte( ix + 8, false ) & 0x01) != 0 ) {
	    do {
	      Thread.sleep( 10 );
	    } while( (getMemByte( ix + 8, false ) & 0x01) != 0 );
	  }
	  setMemByte( ix + 13, ch );
	  setMemByte( ix + 8, getMemByte( ix + 8, false ) | 0x01 );
	  rv = true;
	}
      }
    }
    if( !rv ) {
      rv = super.pasteChar( ch );
    }
    return rv;
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    switch( port & 0xFF ) {
      case 0x80:
	if( this.modules != null ) {
	  int slot = (port >> 8) & 0xFF;
	  for( int i = 0; i < this.modules.length; i++ ) {
	    if( this.modules[ i ].getSlot() == slot ) {
	      rv = this.modules[ i ].getTypeByte();
	      break;
	    }
	  }
	}
	break;

      case 0x88:
	rv = this.pio.readDataA();
	break;

      case 0x89:
	rv = this.pio.readDataB();
	break;

      case 0x8C:
      case 0x8D:
      case 0x8E:
      case 0x8F:
	rv = this.ctc.read( port & 0x03, tStates );
	break;

      default:
	if( this.modules != null ) {
	  for( int i = 0; i < this.modules.length; i++ ) {
	    int v = this.modules[ i ].readIOByte( port, tStates );
	    if( v >= 0 ) {
	      rv = v;
	      break;
	    }
	  }
	}
    }
    return rv;
  }


  @Override
  public int readMemByte( int addr, boolean m1 )
  {
    return getMemByteInternal( addr, this.irmEnabled, m1, true );
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
    int rv = 0;
    int ix = getRegIX();
    if( (ix >= 0) && (memory == this.emuThread) ) {
      int prolog = memory.getMemByte( ix + 9, false );
      int b      = memory.getMemByte( addr, false );
      if( (addr <= 0xFFFE) && ((b == 0x7F) || (b == prolog)) ) {
	if( memory.getMemByte( addr + 1, false ) == b ) {

	  // Prolog-Bytes gefunden
	  int bot = buf.length();
	  int bol = bot;
	  if( !sourceOnly ) {
	    buf.append( String.format( "%04X  %02X %02X", addr, b, b ) );
	  }
	  appendSpacesToCol( buf, bol, colMnemonic );
	  buf.append( "DB" );
	  appendSpacesToCol( buf, bol, colArgs );
	  if( b >= 0xA0 ) {
	    buf.append( String.format( "0%02XH,0%02XH", b, b ) );
	  } else {
	    buf.append( String.format( "%02XH,%02XH", b, b ) );
	  }
	  appendSpacesToCol( buf, bol, colRemark );
	  buf.append( ";MENU ITEM\n" );

	  // Menuewort parsen
	  bol = buf.length();
	  long m = 0;
	  int  n = 0;
	  int  rowAddr = addr + 2;
	  int  curAddr = rowAddr;
	  b = memory.getMemByte( curAddr++, false );
	  while( (curAddr < 0xFFFF)
		 && (b >= 0x30)
		 && ((b < 0x60) || ((b < 0x7F) && (this.kcTypeNum > 4))) )
	  {
	    if( n == 5 ) {
	      if( !sourceOnly ) {
		buf.append( String.format(
				"%04X  %02X %02X %02X %02X %02X",
				rowAddr,
				(m >> 32) & 0xFF,
				(m >> 24) & 0xFF,
				(m >> 16) & 0xFF,
				(m >> 8) & 0xFF,
				m & 0xFF ) );

	      }
	      appendSpacesToCol( buf, bol, colMnemonic );
	      buf.append( "DB" );
	      appendSpacesToCol( buf, bol, colArgs );
	      buf.append( '\'' );
	      buf.append( (char) ((m >> 32) & 0xFF) );
	      buf.append( (char) ((m >> 24) & 0xFF) );
	      buf.append( (char) ((m >> 16) & 0xFF) );
	      buf.append( (char) ((m >> 8) & 0xFF) );
	      buf.append( (char) (m & 0xFF) );
	      buf.append( "\'\n" );
	      rowAddr = curAddr - 1;
	      n       = 0;
	      bol     = buf.length();
	    }
	    m = (m << 8) | (b & 0xFF);
	    n++;
	    b = memory.getMemByte( curAddr++, false );
	  }
	  if( n > 0 ) {
	    if( !sourceOnly ) {
	      buf.append( String.format( "%04X ", rowAddr ) );
	      for( int i = 0; i < n; i++ ) {
		buf.append( String.format(
				" %02X",
				(m >> ((n - i - 1) * 8)) & 0xFF ) );
	      }
	    }
	    appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( "DB" );
	    appendSpacesToCol( buf, bol, colArgs );
	    buf.append( '\'' );
	    for( int i = 0; i < n; i++ ) {
	      buf.append( (char) ((m >> ((n - i - 1) * 8)) & 0xFF) );
	    }
	    buf.append( "\'\n" );
	  }
	  if( (curAddr >= (addr + 4))
	      && (b >= 0)
	      && ((b <= 1) || ((b <= 0x1F) && (this.kcTypeNum > 4))) )
	  {
	    // Epilog gefunden
	    bol = buf.length();
	    if( !sourceOnly ) {
	      buf.append( String.format( "%04X  %02X", curAddr - 1, b ) );
	    }
	    appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( "DB" );
	    appendSpacesToCol( buf, bol, colArgs );
	    buf.append( String.format( "%02XH", b ) );
	    buf.append( '\n' );
	    rv = curAddr - addr;
	  } else {
	    // war doch ein Menueeintrag -> Text loeschen
	    buf.setLength( bot );
	  }
	}
      }
      if( rv == 0 ) {
	// Aufruf des Sprungverteilers pruefen
	b = memory.getMemByte( addr, true );
	String s = null;
	switch( b ) {
	  case 0xC3:
	    s = "JP";
	    break;
	  case 0xCD:
	    s = "CALL";
	    break;
	}
	if( s != null ) {
	  String[] sysCallNames = null;
	  int      destAddr     = memory.getMemWord( addr + 1 );
	  if( destAddr == 0xF003 ) {
	    sysCallNames = stdSysCallNames;
	  } else if( destAddr == 0xF021 ) {
	    sysCallNames = pv7SysCallNames;
	  }
	  if( sysCallNames != null ) {
	    int bol = buf.length();
	    int idx = memory.getMemByte( addr + 3, false );
	    if( !sourceOnly ) {
	      buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				destAddr & 0xFF,
				(destAddr >> 8) & 0xFF ) );
	    }
	    appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( s );
	    appendSpacesToCol( buf, bol, colArgs );
	    buf.append( String.format( "0%04XH\n", destAddr ) );
	    bol = buf.length();
	    if( !sourceOnly ) {
	      buf.append( String.format(
				"%04X  %02X",
				(addr + 3) & 0xFFFF,
				idx ) );
	    }
	    appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( "DB" );
	    appendSpacesToCol( buf, bol, colArgs );
	    if( idx >= 0xA0 ) {
	      buf.append( '0' );
	    }
	    buf.append( String.format( "%02XH", idx ) );
	    if( (idx >= 0) && (idx < sysCallNames.length) ) {
	      String funcName = sysCallNames[ idx ];
	      if( funcName != null ) {
		appendSpacesToCol( buf, bol, colRemark );
		buf.append( ';' );
		buf.append( funcName );
	      }
	    }
	    buf.append( '\n' );
	    rv = 4;
	    if( idx == 0x23 ) {
	      // hinter OSTR folgenden String reassemblieren
	      rv += reassembleStringTerm0(
					memory,
					addr + rv,
					buf,
					sourceOnly,
					colMnemonic,
					colArgs );
	    }
	  }
	}
      }
      if( rv == 0 ) {
	// direkten Aufruf einer Systemfunktion pruefen
	b = memory.getMemByte( addr, true );
	String s1 = null;
	String s2 = null;
	String s3 = null;
	if( (b & 0xC7) == 0xC2 ) {
	  s1 = "JP";
	} else if( (b & 0xC7) == 0xC4 ) {
	  s1 = "CALL";
	}
	if( s1 != null ) {
	  int idx = ((b >> 3) & 0x07);
	  if( (idx >= 0) && (idx < cpuFlags.length) ) {
	    s2 = cpuFlags[ idx ];
	  } else {
	    s1 = null;
	  }
	} else {
	  if( b == 0xC3 ) {
	    s1 = "JP";
	  } else if( b == 0xCD ) {
	    s1 = "CALL";
	  }
	}
	if( s1 != null ) {
	  int a = memory.getMemWord( addr + 1 );
	  int p = ((getMemByteInternal( SUTAB + 1, true, false, false ) << 8)
								& 0xFF00)
			| (getMemByteInternal( SUTAB, true, false, false )
								& 0x00FF);
	  int idx = 0;
	  while( idx < stdSysCallNames.length ) {
	    if( a == memory.getMemWord( p ) ) {
	      int bol = buf.length();
	      if( !sourceOnly ) {
		buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				(a >> 8) & 0xFF,
				a & 0xFF ) );
	      }
	      appendSpacesToCol( buf, bol, colMnemonic );
	      buf.append( s1 );
	      appendSpacesToCol( buf, bol, colArgs );
	      if( s2 != null ) {
		buf.append( s2 );
		buf.append( ',' );
	      }
	      if( a >= 0xA000 ) {
		buf.append( '0' );
	      }
	      buf.append( String.format( "%04XH", a ) );
	      appendSpacesToCol( buf, bol, colRemark );
	      buf.append( ';' );
	      buf.append( stdSysCallNames[ idx ] );
	      buf.append( '\n' );
	      rv = 3;
	      if( idx == 0x23 ) {
		// hinter OSTR folgenden String reassemblieren
		rv += reassembleStringTerm0(
					memory,
					addr + rv,
					buf,
					sourceOnly,
					colMnemonic,
					colArgs );
	      }
	      break;
	    }
	    idx++;
	    p += 2;
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public void reset( boolean powerOn, Properties props )
  {
    super.reset( powerOn, props );

    if( powerOn ) {
      initDRAM();
      if( this.ram8 != null ) {
	EmuUtil.initDRAM( this.ram8 );
      }
      EmuUtil.initDRAM( this.ramColor0 );
      EmuUtil.initDRAM( this.ramColor1 );
      EmuUtil.initDRAM( this.ramPixel0 );
      EmuUtil.initDRAM( this.ramPixel1 );
    }
    if( this.modules != null ) {
      for( int i = 0; i < this.modules.length; i++ ) {
	this.modules[ i ].reset( powerOn );
      }
    }
    this.basicSegNum          = 0;
    this.basicC000Enabled     = false;
    this.caosC000Enabled      = false;
    this.caosE000Enabled      = true;
    this.kout                 = null;
    this.blinkEnabled         = false;
    this.blinkState           = false;
    this.biState              = false;
    this.h4State              = false;
    this.hiColorRes           = false;
    this.irmEnabled           = true;
    this.ram0Enabled          = true;
    this.ram0Writeable        = true;
    this.ram4Enabled          = (this.kcTypeNum > 3);
    this.ram4Writeable        = this.ram4Enabled;
    this.ram8Enabled          = false;
    this.ram8SegNum           = 2;
    this.ram8Writeable        = false;
    this.ramColorEnabled      = false;
    this.screen1Enabled       = false;
    this.screen1Visible       = false;
    this.screenDirty          = true;
    this.screenRefreshEnabled = false;
    this.tapeOutPhase         = false;
    this.soundPhaseL          = false;
    this.soundPhaseR          = false;
    this.lineTStateCounter    = 0;
    this.lineCounter          = 0;
    this.lastIX               = -1;
    this.keyNumStageBuf       = 0;
    this.keyNumStageNum       = 0;
    this.keyNumStageMillis    = -1;
    this.keyNumPressed        = -1;
    this.keyNumProcessing     = -1;
    this.keyShiftBitCnt       = 0;
    this.keyShiftValue        = 0;
    this.keyTStates           = 0;
    this.ctc.reset( powerOn );
    this.pio.reset( powerOn );
    this.d001SoundDevice.reset();
    setFrontFldDirty();
    updSoundValues();
  }


  @Override
  public void saveBasicProgram()
  {
    SourceUtil.saveKCBasicProgram( this.screenFrm, getKCBasicBegAddr() );
  }


  @Override
  public boolean setBasicMemByte( int addr, int value )
  {
    return setMemByteInternal( addr, value, false );
  }


  @Override
  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    if( this.d004 != null )
      this.d004.setDrive( idx, drive );
  }


  @Override
  public void setJoystickAction( int joyNum, int actionMask )
  {
    KC85JoystickModule joyModule = this.joyModule;
    if( joyModule != null ) {
      joyModule.setJoystickAction( joyNum, actionMask );
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    return setMemByteInternal( addr, value, this.irmEnabled );
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return this.charSetUnknown;
  }


  @Override
  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsHDDisks()
  {
    return this.d004 != null ? this.d004.supportsHDDisks() : false;
  }


  @Override
  public boolean supportsKeyboardFld()
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
    boolean              rv      = false;
    AbstractKC85Module[] modules = this.modules;
    if( modules != null ) {
      for( AbstractKC85Module module : modules ) {
	if( module.supportsPrinter() ) {
	  rv = true;
	  break;
	}
      }
    }
    return rv;
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
    this.pio.strobePortA();
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
  public void writeIOByte( int port, int value, int tStates )
  {
    int m = 0;
    switch( port & 0xFF ) {
      case 0x80:
	if( this.modules != null ) {
	  int slot = (port >> 8) & 0xFF;
	  for( int i = 0; i < this.modules.length; i++ ) {
	    AbstractKC85Module module = this.modules[ i ];
	    if( module.getSlot() == slot ) {
	      this.modules[ i ].setStatus( value );
	      setFrontFldDirty();
	      break;
	    }
	  }
	}
	break;

      case 0x84:
      case 0x85:
	if( this.kcTypeNum > 3 ) {
	  this.screen1Visible  = ((value & 0x01) != 0);
	  this.ramColorEnabled = ((value & 0x02) != 0);
	  this.screen1Enabled  = ((value & 0x04) != 0);
	  this.hiColorRes      = ((value & 0x08) == 0);
	  this.ram8SegNum      = (value >> 4) & 0x0F;
	  if( this.screenBufUsed != null ) {
	    this.screenDirty = true;
	  } else {
	    this.screenFrm.setScreenDirty( true );
	  }
	}
	break;

      case 0x86:
      case 0x87:
	if( this.kcTypeNum > 3 ) {
	  this.ram4Enabled     = ((value & 0x01) != 0);
	  this.ram4Writeable   = ((value & 0x02) != 0);
	  this.basicSegNum     = (~value >> 5) & 0x03;
	  this.caosC000Enabled = ((value & 0x80) != 0);
	}
	break;

      case 0x88:
	this.pio.writeDataA( value );
	break;

      case 0x89:
	this.pio.writeDataB( value );
	break;

      case 0x8A:
	this.pio.writeControlA( value );
	break;

      case 0x8B:
	this.pio.writeControlB( value );
	break;

      case 0x8C:
      case 0x8D:
      case 0x8E:
      case 0x8F:
	this.ctc.write( port & 0x03, value, tStates );
	break;

      default:
	if( this.modules != null ) {
	  for( int i = 0; i < this.modules.length; i++ ) {
	    if( this.modules[ i ].writeIOByte( port, value, tStates ) ) {
	      if( this.modules[ i ] == this.d004 ) {
		setFrontFldDirty();
	      }
	      break;
	    }
	  }
	}
    }
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    this.ctc.z80TStatesProcessed( cpu, tStates );
    this.d001SoundDevice.z80TStatesProcessed( cpu, tStates );

    /*
     * CTC-Eingaenge:
     *   Kanal 0 und 1: h4-Signal (doppelte Zeilensynchronfrequenz),
     *                  pro Pixelzeile 32+32+32+16=112 Takte
     *   Kanal 2 und 3: Bildsynchronimpulse,
     *                  256*112 Takte Low, 56*112 Takte High
     */
    int tStatesPerLine = this.tStatesPerLine;
    if( tStatesPerLine > 0 ) {
      this.lineTStateCounter += tStates;
      if( this.lineTStateCounter >= tStatesPerLine ) {
	this.lineTStateCounter %= tStatesPerLine;
	if( this.lineCounter < 311 ) {
	  if( this.screenRefreshEnabled ) {
	    updScreenLine();
	    this.screenFrm.setScreenDirty( true );
	  }
	  this.lineCounter++;
	} else {
	  this.lineCounter = 0;
	  if( this.screenDirty && (this.screenBufUsed != null) ) {
	    this.screenDirty = false;
	    this.screenFrm.fireRepaint();
	    this.screenRefreshEnabled = true;
	  } else {
	    this.screenRefreshEnabled = false;
	  }
	}
      }
      boolean bi = (this.lineCounter < 256);	// "bi" entspricht /BI
      boolean h4 = false;
      if( ((this.lineTStateCounter >= this.tStatesLinePos0)
		&& (this.lineTStateCounter < this.tStatesLinePos1))
	  || (this.lineTStateCounter >= this.tStatesLinePos2) )
      {
	h4 = true;
      }
      if( h4 != this.h4State ) {
	this.h4State = h4;
        this.ctc.externalUpdate( 0, h4 );
        this.ctc.externalUpdate( 1, h4 );
      }
      if( bi != this.biState ) {
	this.biState = bi;
	this.ctc.externalUpdate( 2, bi );
	this.ctc.externalUpdate( 3, bi );
	KC85JoystickModule joyModule = this.joyModule;
	if( joyModule != null ) {
	  joyModule.setBIState( bi );
	}
      }
    }

    /*
     * Beim Tastaturanschluss wird entsprechend der Pulsabstaende
     * des Bitmusters der Tastennummer jeweils ein Impuls
     * an der Strobe-Leitung der zugehoerigen PIO emuliert.
     * Die Zeitabstaende entsprechen denen des Schaltkreises U807.
     * Die angegebenen Takte beziehen sich auf die Standardtaktfrequenz.
     * Wird eine andere Taktfrequenz eingestellt,
     * laeuft alles mit der anderen Taktfrequenz,
     * die Zeitverhaeltnisse untereineinander bleiben damit immer gleich.
     */
    if( this.keyShiftBitCnt <= 0 ) {
      if( this.keyTStates > 0 ) {
	this.keyTStates -= tStates;
      }
      if( this.keyTStates <= 0 ) {
	int keyNum = getSingleKeyNum() ^ 0x01;
	if( keyNum >= 0 ) {
	  this.keyNumProcessing = keyNum;
	  this.keyShiftValue    = keyNum;
	  this.keyShiftBitCnt   = 8;
	}
	this.keyTStates = 0;
      }
    }
    if( this.keyShiftBitCnt > 0 ) {
      boolean pulse = false;
      if( this.keyShiftBitCnt == 8 ) {
	pulse = true;				// Startimpuls
      }
      else if( this.keyShiftBitCnt < 8 ) {
	if( (this.keyShiftValue & 0x01) != 0 ) {
	  // 1-Bit: 7,14 ms -> 12496 Takte
	  if( this.keyTStates >= 12496 ) {
	    pulse = true;
	    this.keyShiftValue >>= 1;		// Bit verarbeitet
	  }
	} else {
	  // 0-Bit: 5,12 ms -> 8960 Takte
	  if( this.keyTStates >= 8960 ) {
	    pulse = true;
	    this.keyShiftValue >>= 1;		// Bit verarbeitet
	  }
	}
      }
      if( pulse ) {
	this.pio.strobePortB();
	if( this.keyShiftBitCnt == 1 ) {
	  // letztes Bit verarbeitet
	  int keyNum = getSingleKeyNum() ^ 0x01;
	  if( keyNum >= 0 ) {
	    if( keyNum == this.keyNumProcessing ) {
	      // Wortabstand 14,43 ms -> 25253 Takte
	      this.keyTStates = 25253;
	    } else {
	      // Doppelwortabstand 19,46 ms -> 34055 Takte
	      this.keyTStates = 34055;
	    }
	  } else {
	    this.keyTStates = 0;
	  }
	  this.keyShiftBitCnt = 0;
	} else {
	  this.keyTStates = 0;
	  --this.keyShiftBitCnt;
	}
      } else {
	this.keyTStates += tStates;
      }
    }
  }


	/* --- private Methoden --- */

  private void applyPasteFast( Properties props )
  {
    this.keyDirectToBuf = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_KEYS_DIRECT_TO_BUFFER,
				false );
    this.pasteFast = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_PASTE_FAST,
				true );
  }


  /*
   * Die Methode wandelt ein Zeichencode in einen Tastencode um.
   * Im PC-Mode koenen 2-Byte-Tastencodes zurueckgegeben werden.
   */
  private int charToKeyNum( char ch, boolean pcMode )
  {
    int keyNum = -1;
    if( pcMode ) {
      if( (ch > '\u0000') && (ch < '\u0020') ) {
	keyNum = PRE_F1_MASK | char2KeyNum[ ch + 0x40 ];
      } else {
	switch( ch ) {
	  case '[':
	  case '\u00C4':			// Ae
	    keyNum = PRE_F1_MASK | 100;
	    break;
	  case '\\':
	  case '\u00D6':			// Oe
	    keyNum = PRE_F1_MASK | 36;
	    break;
	  case ']':
	  case '\u00DC':			// Ue
	    keyNum = PRE_F1_MASK | 84;
	    break;
	  case '{':
	  case '\u00E4':			// ae
	    keyNum = PRE_F1_MASK | 116;
	    break;
	  case '\u00F6':			// oe
	    keyNum = PRE_F1_MASK | 4;
	    break;
	  case '}':
	  case '\u00FC':			// ue
	    keyNum = PRE_F1_MASK | 20;
	    break;
	  case '~':
	  case '\u00DF':			// sz
	    keyNum = PRE_F1_MASK | 42;
	    break;
	}
      }
    }
    if( keyNum < 0 ) {
      if( (ch > 0) && (ch < char2KeyNum.length) ) {
	keyNum = char2KeyNum[ ch ];
      }
    }
    return keyNum;
  }


  private void copyPixelsToCharRecognizer()
  {
    if( this.kcTypeNum > 3 ) {
      byte[] ramPixel = this.screen1Visible ? this.ramPixel1 : this.ramPixel0;
      for( int col = 0; col < 40; col++ ) {
	for( int y = 0; y < 256; y++ ) {
	  this.charRecognizer.setPixelByte(
					col,
					y,
					ramPixel[ (col << 8) | y ] );
	}
      }
    } else {
      for( int col = 0; col < 32; col++ ) {
	for( int y = 0; y < 256; y++ ) {
	  this.charRecognizer.setPixelByte(
			col,
			y,
			this.ramPixel0[ ((y << 5) & 0x1E00)
					| ((y << 7) & 0x0180)
					| ((y << 3) & 0x0060)
					| (col & 0x001F) ] );
	}
      }
      for( int col = 32; col < 40; col++ ) {
	for( int y = 0; y < 256; y++ ) {
	  this.charRecognizer.setPixelByte(
			col,
			y,
			this.ramPixel0[ 0x2000
					| ((y << 3) & 0x0600)
					| ((y << 7) & 0x0180)
					| ((y << 3) & 0x0060)
					| ((y >> 1) & 0x0018)
					| (col & 0x0007) ] );
	}
      }
    }
  }


  private void createColors( Properties props )
  {
    float brightness = getBrightness( props );
    if( (brightness >= 0F) && (brightness <= 1F) ) {
      for( int i = 0; i < rawRGBValues.length; i++ ) {
	int r = Math.round( rawRGBValues[ i ][ 0 ] * brightness );
	int g = Math.round( rawRGBValues[ i ][ 1 ] * brightness );
	int b = Math.round( rawRGBValues[ i ][ 2 ] * brightness );

	this.rgbValues[ i ] = (r << 16) | (g << 8) | b;
	this.colors[ i ]    = new Color( this.rgbValues[ i ] );
      }
    }
  }


  private AbstractKC85Module[] createModules( Properties props )
  {
    AbstractKC85Module[]               rv      = null;
    java.util.List<AbstractKC85Module> modules = null;
    if( props != null ) {
      int nRemain = EmuUtil.getIntProperty(
				props,
				this.propPrefix
					+ PROP_MODULE_PREFIX
					+ PROP_COUNT,
				0 );
      int nD004Modules = EmuUtil.getIntProperty(
				props,
				this.propPrefix
					+ PROP_MODULE_PREFIX
					+ PROP_DISKSTATION_MODULE_COUNT,
				0 );
      if( nRemain > 0 ) {
	modules                    = new ArrayList<>();
	int                  slot  = 8;
	boolean              loop  = true;
	java.util.List<VDIP> vdips = new ArrayList<>();
	while( loop
	       && (nRemain > 0)
	       && ((slot < 0xF0) || ((this.d004 != null) && (slot < 0x100))) )
	{
	  if( nRemain == nD004Modules ) {
	    slot = 0xF0;
	  }
	  String prefix = String.format(
				"%s%s%02X.",
				this.propPrefix,
				PROP_MODULE_PREFIX,
				slot );
	  String moduleName = props.getProperty( prefix + PROP_NAME );
	  if( moduleName != null ) {
	    if( moduleName.equals( "M003" ) ) {
	      modules.add( new M003( slot, this.emuThread ) );
	    }
	    else if( moduleName.equals( "M006" ) ) {
	      modules.add( new M006( slot, this.emuThread ) );
	    }
	    else if( moduleName.equals( "M008" ) ) {
	      M008 m008 = new M008( slot );
	      if( this.joyModule == null ) {
		this.joyModule = m008;
	      }
	      modules.add( m008 );
	    }
	    else if( moduleName.equals( "M011" ) ) {
	      modules.add( new M011( slot ) );
	    }
	    else if( moduleName.equals( "M012" ) ) {
	      modules.add( new KC85ROM8KModule(
					slot,
					this.emuThread,
					"M012",
					"/rom/kc85/m012.bin" ) );
	    }
	    else if( moduleName.equals( "M021" ) ) {
	      M021 m021 = new M021( slot, this.emuThread );
	      if( this.joyModule == null ) {
		this.joyModule = m021;
	      }
	      modules.add( m021 );
	    }
	    else if( moduleName.equals( "M022" ) ) {
	      modules.add( new KC85PlainRAMModule(
					slot, 0xF4, "M022", 0x4000, false ) );
	    }
	    else if( moduleName.equals( "M025" ) ) {
	      modules.add(
		new M025(
			slot,
			getTypeByteProp( props, prefix, 0xF7, 0xFB, 0x01 ),
			this.screenFrm,
			props.getProperty( prefix + PROP_FILE ) ) );
	    }
	    else if( moduleName.equals( "M026" ) ) {
	      modules.add( new KC85ROM8KModule(
					slot,
					this.emuThread,
					"M026",
					"/rom/kc85/m026.bin" ) );
	    }
	    else if( moduleName.equals( "M027" ) ) {
	      modules.add( new KC85ROM8KModule(
					slot,
					this.emuThread,
					"M027",
					"/rom/kc85/m027.bin" ) );
	    }
	    else if( moduleName.equals( "M028" ) ) {
	      modules.add(
		new M028(
			slot,
			getTypeByteProp( props, prefix, 0xF8, 0xFC ),
			this.screenFrm,
			props.getProperty( prefix + PROP_FILE ) ) );
	    }
	    else if( moduleName.equals( "M032" ) ) {
	      modules.add( new KC85SegmentedRAMModule(
					slot, 0x79, "M032", 0x40000 ) );
	    }
	    else if( moduleName.equals( "M033" ) ) {
	      modules.add( new M033( slot, this.emuThread ) );
	    }
	    else if( moduleName.equals( "M034" ) ) {
	      modules.add( new KC85SegmentedRAMModule(
					slot, 0x7A, "M034", 0x80000 ) );
	    }
	    else if( moduleName.equals( "M035" ) ) {
	      modules.add( new M035( slot ) );
	    }
	    else if( moduleName.equals( "M035x4" ) ) {
	      for( int i = 0; i < 4; i++ ) {
		M035 m035 = new M035( slot + i );
		m035.setExternalModuleName( moduleName );
		modules.add( m035 );
	      }
	    }
	    else if( moduleName.equals( "M036" ) ) {
	      modules.add( new KC85SegmentedRAMModule(
					slot, 0x78, "M036", 0x20000 ) );
	    }
	    else if( moduleName.equals( "M040" ) ) {
	      modules.add(
		new M040(
			slot,
			getTypeByteProp( props, prefix, 0xF7, 0xF8, 0x01 ),
			this.screenFrm,
			props.getProperty( prefix + PROP_FILE ) ) );
	    }
	    else if( moduleName.equals( "M041" ) ) {
	      int typeByte = getTypeByteProp(
					props,
					prefix,
					0xF1,
					0xF8, 0xFC, 0x01 );
	      String  fileName = props.getProperty( prefix + PROP_FILE );
	      M041Sub m041Sub0 = new M041Sub(
					slot,
					typeByte,
					this.screenFrm,
					fileName,
					null );
	      m041Sub0.setExternalModuleName( moduleName );
	      modules.add( m041Sub0 );

	      M041Sub m041Sub1 = new M041Sub(
					slot + 1,
					typeByte,
					this.screenFrm,
					fileName,
					m041Sub0 );
	      m041Sub1.setExternalModuleName( moduleName );
	      modules.add( m041Sub1 );
	    }
	    else if( moduleName.equals( "M045" ) ) {
	      modules.add(
		new M045(
			slot,
			getTypeByteProp( props, prefix, 0x70, 0x01 ),
			this.screenFrm,
			props.getProperty( prefix + PROP_FILE ) ) );
	    }
	    else if( moduleName.equals( "M046" ) ) {
	      modules.add(
		new M046(
			slot,
			getTypeByteProp( props, prefix, 0x71, 0x01 ),
			this.screenFrm,
			props.getProperty( prefix + PROP_FILE ) ) );
	    }
	    else if( moduleName.equals( "M047" ) ) {
	      modules.add(
		new M047(
			slot,
			getTypeByteProp( props, prefix, 0x72, 0x01 ),
			this.screenFrm,
			props.getProperty( prefix + PROP_FILE ) ) );
	    }
	    else if( moduleName.equals( "M048" ) ) {
	      modules.add(
		new M048(
			slot,
			getTypeByteProp( props, prefix, 0x73, 0x01 ),
			this.screenFrm,
			props.getProperty( prefix + PROP_FILE ) ) );
	    }
	    else if( moduleName.equals( "M052" ) ) {
	      M052 m052    = null;	
	      int  vdipNum = vdips.size();
	      if( vdipNum == 0 ) {
		m052 = new M052(
			slot,
			this.emuThread.getScreenFrm(),
			this.emuThread.getZ80CPU(),
			vdipNum,
			false,
			props.getProperty( this.propPrefix
						+ PROP_ROM_M052_FILE ) );
	      } else {
		m052 = new M052(
			slot,
			this.emuThread.getScreenFrm(),
			this.emuThread.getZ80CPU(),
			vdipNum,
			true,
			props.getProperty( this.propPrefix
					+ PROP_ROM_M052USB_FILE ) );
	      }
	      vdips.add( m052.getVDIP() );
	      modules.add( m052 );
	    }
	    else if( moduleName.equals( "M066" ) ) {
	      modules.add( new M066( slot, this ) );
	    }
	    else if( moduleName.equals( "M120" ) ) {
	      modules.add( new KC85PlainRAMModule(
					slot, 0xF0, "M120", 0x2000, true ) );
	    }
	    else if( moduleName.equals( "M122" ) ) {
	      modules.add( new KC85PlainRAMModule(
					slot, 0xF1, "M122", 0x4000, true ) );
	    }
	    else if( moduleName.equals( "M124" ) ) {
	      modules.add( new KC85PlainRAMModule(
					slot, 0xF2, "M124", 0x8000, true ) );
	    } else {
	      loop = false;
	    }
	    if( loop ) {
	      slot += 4;
	      --nRemain;
	    }
	  } else {
	    break;
	  }
	}
	try {
	  this.vdips = vdips.toArray( new VDIP[ vdips.size() ] );
	}
	catch( ArrayStoreException ex ) {}
      }
    }
    if( this.d004 != null ) {
      if( modules != null ) {
	modules.add( this.d004 );
      } else {
	rv      = new AbstractKC85Module[ 1 ];
	rv[ 0 ] = this.d004;
      }
    }
    if( modules != null ) {
      int n = modules.size();
      if( n > 0 ) {
	try {
	  rv = modules.toArray( new AbstractKC85Module[ n ] );
	}
	catch( ArrayStoreException ex ) {}
      }
    }
    return rv;
  }


  private boolean emulatesD004( Properties props )
  {
    return EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_DISKSTATION ).equals( VALUE_D004 );
  }


  private boolean emulatesD008( Properties props )
  {
    return EmuUtil.getProperty(
		props,
		this.propPrefix + PROP_DISKSTATION ).equals( VALUE_D008 );
  }


  private int getColorIndex( int colorByte, boolean foreground )
  {
    if( this.blinkEnabled && this.blinkState && ((colorByte & 0x80) != 0) ) {
      foreground = false;
    }
    return foreground ? ((colorByte >> 3) & 0x0F) : ((colorByte & 0x07) + 16);
  }


  private int getKCBasicBegAddr()
  {
    int rv = 0x2C01;			// RAM-BASIC
    if( this.kcTypeNum > 2 ) {
      rv = 0x0401;			// ROM-BASIC
    } else {
      if( this.modules != null ) {
	for( int i = 0; i < this.modules.length; i++ ) {
	  AbstractKC85Module m = this.modules[ i ];
	  if( (m instanceof M006) && m.isEnabled() ) {
	    rv = 0x0401;		// ROM-BASIC
	  }
	}
      }
    }
    return rv;
  }


  /*
   * Die Methode liefert die Anfangsadresse der Tastaturtabelle,
   * wenn sich der KC85 im PC-Mode befindet,
   * d.h. wenn gerade MicroDOS oder ein anderes CP/M-kompatibles
   * Betriebssystem in der D004 ausgefuehrt werden.
   * Ist der PC-Mode nicht aktiv, liefert die Methode -1.
   */
  private int getKeyTabAddrIfPCMode()
  {
    int rv = -1;
    if( this.d004 != null ) {
      if( this.d004.isRunning() ) {
	/*
	 * Wenn die D004 laeuft, kann trotzdem der CAOS-Modus aktiv sein.
	 * Zur Entscheidungsfindung wird der Interrupt-Vektor
	 * von PIO Port B geprueft.
	 * Wenn dieser in den ROM zeigt, muss CAOS aktiv sein.
	 */
	boolean caos    = false;
	int     iVector = this.pio.getInterruptVectorPortB();
	int     ptrAddr = -1;
	switch( this.emuThread.getZ80CPU().getInterruptMode() ) {
	  case 0:
	    if( getMemByte( 0x0038, false ) == 0xC3 ) {
	      ptrAddr = 0x0039;
	    }
	    break;
	  case 1:
	    if( (iVector & 0xC7) == 0xC7 ) {	// RST-Befehl?
	      int rstAddr = iVector & 0x0038;
	      if( getMemByte( rstAddr, false ) == 0xC3 ) {
		ptrAddr = rstAddr + 1;
	      }
	    }
	    break;
	  case 2:
	    ptrAddr = ((this.emuThread.getZ80CPU().getRegI() << 8) & 0xFF00)
					      | (iVector & 0x00FF);
	    break;
	}
	if( ptrAddr >= 0 ) {
	  if( getMemWord( ptrAddr ) >= 0xE000 ) {
	    caos = true;
	  }
	}
	if( !caos ) {
	  /*
	   * Bei MicroDOS und ML-DOS steht der Zeiger
	   * auf die Tastaturtabelle in der D004
	   * auf Adresse FFB4h/FFB5h.
	   * Die Tastaturtabelle selbst liegt jedoch im Grundgeraet.
	   */
	  rv = this.d004.getZ80Memory().getMemWord( 0xFFB4 );
	}
      }
    }
    return rv;
  }


  private int getMemByteInternal(
			int     addr,
			boolean irmEnabled,
			boolean m1,
			boolean enabledWaitStates )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( (addr >= 0) && (addr < 0x4000) ) {
      if( this.ram0Enabled ) {
	rv   = this.emuThread.getRAMByte( addr );
	done = true;
      }
    } else if( (addr >= 0x4000) && (addr < 0x8000) ) {
      if( this.ram4Enabled ) {
	rv   = this.emuThread.getRAMByte( addr );
	done = true;
      }
    } else if( (addr >= 0x8000) && (addr < 0xC000) ) {
      int idx = addr - 0x8000;
      if( irmEnabled ) {
	byte[] a = null;
	if( (addr < 0xA800)
	    || (this.caosC000Enabled && !this.caosE000Enabled) )
	{
	  if( this.screen1Enabled ) {
	    a = this.ramColorEnabled ? this.ramColor1 : this.ramPixel1;
	  } else {
	    a = this.ramColorEnabled ? this.ramColor0 : this.ramPixel0;
	  }
	} else {
	  a = this.ramPixel0;
	}
	if( a != null ) {
	  if( idx < a.length ) {
	    rv = (int) a[ idx ] & 0xFF;
	  }
	}
	done = true;
      } else if( this.ram8Enabled && (this.ram8 != null) ) {
	if( this.kcTypeNum == 4 ) {
	  if( (this.ram8SegNum & 0x01) != 0 ) {
	    idx += 0x4000;
	  }
	  if( idx < this.ram8.length ) {
	    rv = (int) this.ram8[ idx ] & 0xFF;
	  }
	} else if( this.kcTypeNum > 4 ) {
	  if( this.ram8SegNum == 0 ) {
	    rv = this.emuThread.getRAMByte( addr - 0x8000 );
	  } else if( this.ram8SegNum == 1 ) {
	    rv = this.emuThread.getRAMByte( addr - 0x4000 );
	  } else {
	    idx = ((this.ram8SegNum - 2) * 0x4000) + addr - 0x8000;
	    if( (idx >= 0) && (idx < this.ram8.length) ) {
	      rv = (int) this.ram8[ idx ] & 0xFF;
	    }
	  }
	}
	done = true;
      }
    } else if( (addr >= 0xC000) && (addr < 0xE000) ) {
      int idx = addr - 0xC000;
      if( (this.kcTypeNum >= 4) && this.caosC000Enabled ) {
	if( this.caosC000 != null ) {
	  if( idx < this.caosC000.length ) {
	    rv   = (int) this.caosC000[ idx ] & 0xFF;
	  }
	  /*
	   * Wenn beim KC85/4 die ROM_C-Datei > 4K ist,
	   * dann volle 8K emulieren.
	   */
	  if( (addr < 0xD000)
	      || (this.caosC000.length > 0x1000)
	      || (this.kcTypeNum > 4) )
	  {
	    done = true;
	  }
	} else {
	  if( (addr < 0xD000) || (this.kcTypeNum > 4) ) {
	    done = true;
	  }
	}
      } else if( this.basicC000Enabled && (this.basicC000 != null) ) {
	if( (this.kcTypeNum > 4) && (this.basicC000.length > 0x2000) ) {
	  switch( this.basicSegNum ) {
	    case 0:
	      idx += 0x6000;
	      break;
	    case 1:
	      idx += 0x2000;
	      break;
	    case 2:
	      idx += 0x4000;
	      break;
	  }
	}
	if( idx < this.basicC000.length ) {
	  rv = (int) basicC000[ idx ] & 0xFF;
	}
	done = true;
      }
    } else if( addr >= 0xE000 ) {
      if( this.caosE000Enabled ) {
	if( (this.kcTypeNum >= 3) || (addr < 0xF000) ) {
	  if( this.caosE000 != null ) {
	    int idx = addr - 0xE000;
	    if( idx < this.caosE000.length ) {
	      rv = (int) this.caosE000[ idx ] & 0xFF;
	    }
	  }
	} else {
	  if( this.caosF000 != null ) {
	    int idx = addr - 0xF000;
	    if( idx < this.caosF000.length ) {
	      rv = (int) this.caosF000[ idx ] & 0xFF;
	    }
	  }
	}
	if( (this.kcTypeNum >= 3) || ((addr & 0x0800) == 0) ) {
	  done = true;
	}
      }
    }
    if( !done && (this.modules != null) ) {
      for( int i = 0; i < this.modules.length; i++ ) {
	if( m1 && enabledWaitStates
	    && (this.modules[ i ].getSlot() >= 0x10) )
	{
	  // Wait-State bei M1-Zugriff auf Module in einem D002
	  this.emuThread.getZ80CPU().addWaitStates( 1 );
	}
	int v = this.modules[ i ].readMemByte( addr );
	if( v >= 0 ) {
	  rv = v;
	  break;
	}
      }
    }
    return rv >= 0 ? rv : 0xFF;
  }


  private String getProperty( Properties props, String keyword )
  {
    return EmuUtil.getProperty( props, this.propPrefix + keyword );
  }


  /*
   * IX zeigt auf bestimmte Systemzellen.
   * Es koennte aber sein, dass das Anwendungsprogramm
   * den Interrupt sperrt und das IX-Register temporaer
   * anders verwendet.
   * Aus diesem Grund wird das IX-Register nur dann ausgelesen,
   * wenn der Interrupt nicht gesperrt ist.
   * Da aber aus Performance-Gruenden auf eine Synchronisierung
   * der CPU-Emulation verzichtet wird,
   * wird der Interrupt-Status zur Sicherheit hier
   * vor und nach dem Auslesen des IX-Status getestet.
   * Ist der Interrupt gerade gesperrt, wird der letzte gelesene
   * IX-Wert genommen.
   *
   * Rueckgabewert:
   *  >= 0: IX-Register
   *    -1: Register konnte nicht gelesen werden
   */
  private int getRegIX()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    if( cpu.getIFF1() ) {
      int ix = cpu.getRegIX();
      if( cpu.getIFF1() ) {
	this.lastIX = ix;
      }
    }
    return this.lastIX;
  }


  private byte[] getResource( String resource )
  {
    byte[] rv = null;
    if( resource != null ) {
      if( resources.containsKey( resource ) ) {
	rv = resources.get( resource );
      } else {
	rv = EmuUtil.readResource( this.screenFrm, resource );
	resources.put( resource, rv );
      }
    }
    return rv;
  }


  /*
   * 2-Byte-Tastencodes werden in keyNumStageBuf zwischengespeichert,
   * damit daraus die Einzelcodes entsprechend folgender
   * zeitlicher Abfolge gelesen werden koennen:
   *   1. zweimal ersten Tastencode lesen
   *   2. zweimal kein Tastencode lesen
   *   3. viermal zweiten Tastencode lesen
   *   4. etwas laengere Pause
   * Gesteuert wird das ganze durch Zustandsnummern in keyNumStageNum,
   * wobei der erste Zustand (erstes mal Senden des ersten Tastencode)
   * sofort eingenommen und somit nicht in keyNumStageNum gefuehrt wird.
   */
  private int getSingleKeyNum()
  {
    int rv = -1;
    if( this.keyNumStageNum > 0 ) {
      switch( this.keyNumStageNum ) {
	case 1:
	  if( System.currentTimeMillis() < this.keyNumStageMillis ) {
	    this.keyNumStageNum++;	// im Wartezustand verbleiben
	  } else {
	    this.keyNumStageMillis = -1L;
	  }
	  break;
	case 2:
	case 3:
	case 4:
	case 5:
	  rv = this.keyNumStageBuf & 0xFF;		// 2. Tastencode
	  break;
	case 8:
	  rv = (this.keyNumStageBuf >> 8) & 0xFF;	// 1. Tastencode
	  break;
      }
      --this.keyNumStageNum;
    } else {
      int keyNum = this.keyNumPressed;
      if( keyNum >= 0 ) {
	if( (keyNum & 0xFF00) != 0 ) {
	  // 2-Byte-Tastencode
	  this.keyNumStageBuf    = keyNum;
	  this.keyNumStageNum    = 8;
	  this.keyNumStageMillis = System.currentTimeMillis() + 300L;
	  rv = (keyNum >> 8) & 0xFF;
	} else {
	  // 1-Byte-Tastencode
	  rv = keyNum & 0xFF;
	}
      }
    }
    return rv;
  }


  private static int getTypeByteProp(
			Properties props,
			String     prefix,
			int        defaultTypeByte,
			int...     allowedTypeBytes )
  {
    int typeByte = defaultTypeByte;
    if( allowedTypeBytes != null ) {
      String text     = props.getProperty( prefix + PROP_TYPEBYTE );
      if( text != null ) {
	try {
	  int b = Integer.parseInt( text, 16 );
	  for( int atb : allowedTypeBytes ) {
	    if( b == atb ) {
	      typeByte = b;
	      break;
	    }
	  }
	}
	catch( NumberFormatException ex ) {}
      }
    }
    return typeByte;
  }


  private int keyNumToChar( int keyNum, int pcKeyTabAddr, boolean ctrlDown )
  {
    int ch = -1;
    if( keyNum >= 0 ) {
      int keyNum2 = keyNum & 0xFF;
      if( pcKeyTabAddr >= 0 ) {
	int tabIdx = (keyNum2 / 2) * 3;
	if( ctrlDown ) {
	  // Ctrl-Ebene
	  tabIdx += 2;
	} if( (keyNum2 % 2) != 0 ) {
	  // Shift-Ebene
	  tabIdx++;
	}
	ch = getMemByte( pcKeyTabAddr + tabIdx, false );
      } else {
	int ix = getRegIX();
	if( ix >= 0 ) {
	  ch = getMemByte( getMemWord( ix + 14 ) + keyNum2, false );
	}
      }
    }
    return ch;
  }


  private boolean putCharToKeyBuffer( char ch )
  {
    boolean rv = false;
    if( ch <= 0xFF ) {
      int ix = getRegIX();
      if( ix >= 0 ) {
	setMemByte( ix + 13, ch );
	setMemByte( ix + 8, getMemByte( ix + 8, false ) | 0x01 );
	rv = true;
      }
    }
    return rv;
  }


  private int reassembleStringTerm0(
			Z80MemView    memory,
			int           addr,
			StringBuilder buf,
			boolean       sourceOnly,
			int           colMnemonic,
			int           colArgs )
  {
    int     rv   = 0;
    int[]   bBuf = new int[ 4 ];
    boolean loop = true;
    do {
      int n = 0;
      while( n < bBuf.length ) {
	int b       = memory.getMemByte( addr + n, false );
	bBuf[ n++ ] = b;
	if( b == 0 ) {
	  loop = false;
	  break;
	}
      }
      int bol = buf.length();
      if( !sourceOnly ) {
	buf.append( String.format( "%04X ", addr ) );
	for( int i = 0; i < n; i++ ) {
	  buf.append( String.format( " %02X", bBuf[ i ] ) );
	}
      }
      appendSpacesToCol( buf, bol, colMnemonic );
      buf.append( "DB" );
      appendSpacesToCol( buf, bol, colArgs );
      boolean quoted = false;
      for( int i = 0; i < n; i++ ) {
	int b = bBuf[ i ];
	if( (b >= 0x20) && (b < 0x7F) ) {
	  if( !quoted ) {
	    if( i > 0 ) {
	      buf.append( ',' );
	    }
	    buf.append( '\'' );
	    quoted = true;
	  }
	  buf.append( (char) b );
	} else {
	  if( quoted ) {
	    buf.append( '\'' );
	    quoted = false;
	  }
	  if( i > 0 ) {
	    buf.append( ',' );
	  }
	  buf.append( String.format( "%02XH", b ) );
	}
      }
      if( quoted ) {
	buf.append( '\'' );
      }
      buf.append( '\n' );
      addr += n;
      rv += n;
    } while( loop );
    return rv;
  }


  private boolean setMemByteInternal(
				int     addr,
				int     value,
				boolean irmEnabled )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( (addr >= 0) && (addr < 0x4000) ) {
      if( this.ram0Enabled ) {
	if( this.ram0Writeable ) {
	  this.emuThread.setRAMByte( addr, value );
	  rv = true;
	}
	done = true;
      }
    } else if( (addr >= 0x4000) && (addr < 0x8000) ) {
      if( this.ram4Enabled ) {
	if( this.ram4Writeable ) {
	  this.emuThread.setRAMByte( addr, value );
	  rv = true;
	}
	done = true;
      }
    } else if( (addr >= 0x8000) && (addr < 0xC000) ) {
      int idx = addr - 0x8000;
      if( irmEnabled ) {
	byte[] a = null;
	if( (addr < 0xA800)
	    || (this.caosC000Enabled && !this.caosE000Enabled) )
	{
	  if( this.screen1Enabled ) {
	    a = this.ramColorEnabled ? this.ramColor1 : this.ramPixel1;
	  } else {
	    a = this.ramColorEnabled ? this.ramColor0 : this.ramPixel0;
	  }
	} else {
	  a = this.ramPixel0;
	}
	if( a != null ) {
	  if( idx < a.length ) {
	    a[ idx ] = (byte) value;
	    if( this.screenBufUsed != null ) {
	      this.screenDirty = true;
	    } else {
	      this.screenFrm.setScreenDirty( true );
	    }
	    rv = true;
	  }
	}
	done = true;
      } else if( this.ram8Enabled && (this.ram8 != null) ) {
	if( this.ram8Writeable ) {
	  if( this.kcTypeNum == 4 ) {
	    if( (this.ram8SegNum & 0x01) != 0 ) {
	      idx += 0x4000;
	    }
	    if( idx < this.ram8.length ) {
	      this.ram8[ idx ] = (byte) value;
	      rv = true;
	    }
	  } else if( this.kcTypeNum > 4 ) {
	    if( this.ram8SegNum == 0 ) {
	      this.emuThread.setRAMByte( addr - 0x8000, value );
	    } else if( this.ram8SegNum == 1 ) {
	      this.emuThread.setRAMByte( addr - 0x4000, value );
	    } else {
	      idx = ((this.ram8SegNum - 2) * 0x4000) + addr - 0x8000;
	      if( (idx >= 0) && (idx < this.ram8.length) ) {
		this.ram8[ idx ] = (byte) value;
	      }
	    }
	  }
	}
	done = true;
      }
    } else if( (addr >= 0xC000) && (addr < 0xE000) ) {
      if( (this.kcTypeNum >= 4) && this.caosC000Enabled ) {
	if( this.caosC000 != null ) {
	  /*
	   * Wenn beim KC85/4 die ROM-C_Datei > 4K ist,
	   * dann voll 8K emulieren.
	   */
	  if( (addr < 0xD000)
	      || (this.caosC000.length > 0x1000)
	      || (this.kcTypeNum > 4) )
	  {
	    done = true;
	  }
	} else {
	  if( (addr < 0xD000) || (this.kcTypeNum > 4) ) {
	    done = true;
	  }
	}
      }
    } else if( addr >= 0xE000 ) {
      if( this.caosE000Enabled
	  && ((this.kcTypeNum >= 3) || ((addr & 0x0800) == 0)) )
      {
	done = true;
      }
    }
    if( !done && (this.modules != null) ) {
      for( int i = 0; i < this.modules.length; i++ ) {
	int v = this.modules[ i ].writeMemByte( addr, value );
	if( v == 1 ) {
	  break;
	}
	else if( v > 1 ) {
	  rv = true;
	  break;
	}
      }
    }
    return rv;
  }


  private void updKeyboardFld( int keyNum )
  {
    if( this.keyboardFld != null ) {
      this.keyboardFld.updKeySelection( keyNum );
    }
  }


  private void updScreenLine()
  {
    byte[] screenBuf = this.screenBufUsed;
    int    y         = this.lineCounter;
    if( (screenBuf != null) && (y >= 0) && (y < 256) ) {
      int linePos = y * 320;
      int x       = 0;
      for( int col = 0; col < 40; col++ ) {
	if( this.kcTypeNum > 3 ) {
	  boolean screen1  = this.screen1Visible;
	  byte[]  ramPixel = screen1 ? this.ramPixel1 : this.ramPixel0;
	  byte[]  ramColor = screen1 ? this.ramColor1 : this.ramColor0;
	  int     idx      = (col * 256) + y;
	  if( (idx >= 0) && (idx < ramPixel.length) ) {
	    int p = ramPixel[ idx ];
	    int c = ramColor[ idx ];
	    int m = 0x80;
	    for( int i = 0; (i < 8) && (x < SCREEN_WIDTH); i++ ) {
	      int     colorIdx = 0;
	      boolean pState   = ((p & m) != 0);
	      if( this.hiColorRes ) {
		if( this.blinkEnabled
		    && this.blinkState
		    && ((c & 0x80) != 0) )
		{
		  pState = false;
		}
		if( pState ) {
		  if( (c & m) != 0 ) {
		    colorIdx = 7;               // weiss
		  } else {
		    colorIdx = 2;               // rot
		  }
		} else {
		  if( (c & m) != 0 ) {
		    colorIdx = 5;               // tuerkis
		  } else {
		    colorIdx = 0;               // schwarz
		  }
		}
	      } else {
		colorIdx = getColorIndex( c, pState );
	      }
	      if( (colorIdx >= 0) && (colorIdx < rgbValues.length) ) {
		screenBuf[ linePos + x ] = (byte) colorIdx;
		x++;
	      }
	      m >>= 1;
	    }
	  }
	} else {
	  int pIdx = -1;
	  int cIdx = -1;
	  if( col < 32 ) {
	    pIdx = ((y << 5) & 0x1E00)
			| ((y << 7) & 0x0180)
			| ((y << 3) & 0x0060)
			| (col & 0x001F);
	    cIdx = 0x2800 | ((y << 3) & 0x07E0) | (col & 0x001F);
	  } else {
	    pIdx = 0x2000
			| ((y << 3) & 0x0600)
			| ((y << 7) & 0x0180)
			| ((y << 3) & 0x0060)
			| ((y >> 1) & 0x0018)
			| (col & 0x0007);
	    cIdx = 0x3000
			| ((y << 1) & 0x0180)
			| ((y << 3) & 0x0060)
			| ((y >> 1) & 0x0018)
			| (col & 0x0007);
	  }
	  if( (pIdx >= 0) && (pIdx < this.ramPixel0.length)
	      && (cIdx >= 0) && (cIdx < this.ramPixel0.length) )
	  {
	    int p = this.ramPixel0[ pIdx ];
	    int c = this.ramPixel0[ cIdx ];
	    int m = 0x80;
	    for( int i = 0; (i < 8) && (x < SCREEN_WIDTH); i++ ) {
	      int colorIdx = getColorIndex( c, (p & m) != 0 );
	      if( (colorIdx >= 0) && (colorIdx < rgbValues.length) ) {
		screenBuf[ linePos + x ] = (byte) colorIdx;
		x++;
	      }
	      m >>= 1;
	    }
	  }
	}
      }
    }
  }


  private void updSoundValues()
  {
    int valueL = (this.soundPhaseL ? 0 : AudioOut.MAX_USED_UNSIGNED_VALUE );
    int valueR = (this.soundPhaseR ? 0 : AudioOut.MAX_USED_UNSIGNED_VALUE );
    int valueM = (valueL + valueR) / 2;
    int volume = this.pio.fetchOutValuePortB( 0xFF );
    if( (volume & 0x10) != 0 ) {
      valueM = (valueM * 30) / 100;	// 1.0 / (2.35 + 1.0)
    }
    if( (volume & 0x08) != 0 ) {
      valueM = (valueM * 46) / 100;	// 2.0 / (2.35 + 2.0)
    }
    if( (volume & 0x04) != 0 ) {
      valueM = (valueM * 62) / 100;	// 3.9 / (2.35 + 3.9)
    }
    if( (volume & 0x02) != 0 ) {
      valueM = (valueM * 78) / 100;	// 8.2 / (2.35 + 8.2)
    }
    if( (this.kcTypeNum < 4) && ((volume & 0x01) != 0) ) {
      valueM = (valueM * 87) / 100;	// 16.0 / (2.35 + 16.0)
    }
    this.d001SoundDevice.setCurValues( valueM, valueL, valueR );
  }
}
