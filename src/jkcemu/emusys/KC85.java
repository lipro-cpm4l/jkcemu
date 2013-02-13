/*
 * (c) 2008-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Computer HC900 und KC85/2..5
 */

package jkcemu.emusys;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.emusys.kc85.*;
import jkcemu.etc.VDIP;
import jkcemu.text.TextUtil;
import z80emu.*;


public class KC85 extends EmuSys implements
					Z80CTCListener,
					Z80MaxSpeedListener,
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
    "PAPER",     "AT",       "COLOR",   "SOUND",	// 0xE0
    "PSET",      "PRESET",   "BLOAD",   "VPEEK",
    "VPOKE",     "LOCATE",   "KEYLIST", "KEY",
    "SWITCH",    "PTEST",    "CLOSE",   "OPEN",
    "RANDOMIZE", "VGET$",    "LINE",    "CIRCLE",	// 0xF0
    "CSRLIN" };

  private static final int SUTAB         = 0xB7B0;
  private static final int SCREEN_WIDTH  = 320;
  private static final int SCREEN_HEIGHT = 256;

  private static final FloppyDiskInfo[] availableFloppyDisks = {
		new FloppyDiskInfo(
			"/disks/kc85/kc85caos.dump.gz",
			"KC85 CAOS Systemdiskette" ),
		new FloppyDiskInfo(
			"/disks/kc85/kc85microdos.dump.gz",
			"KC85 MicroDOS Systemdiskette" ) };

  private static final int[][] basicRGBValues = {

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
	 -1,  24,  41,  60,  -1,  -1,  -1,   6,		// 0x00
	 -1, 122, 118, 120,   9, 126,  -1,  25,
	  8, 121, 119,  76,  57,  -1,  -1,  -1,		// 0x10
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
	 31,  15,  81,  -1, 103,  -1,  -1,  -1,		// 0x78  xyz |
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0x80
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0x90
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0xA0
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0xB0
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0xC0
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0xD0
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,		// 0xE0
	 -1,  -1,  -1,  -1,  -1,  -1,  -1,  -1,
	 -1, 124,  12,  28, 108,  44,  92, 125,		// 0xF0  F0...F7
	 13,  29, 109,  45,  93,  -1,  -1,  -1 };	// 0xF8  F8...FC

  private static final String[] cpuFlags = {
			"NZ", "Z", "NC", "C", "PO", "PE", "P", "M" };

  private static final String[] sysCallNames = {
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

  private static Map<String,byte[]> resources = new HashMap<String,byte[]>();

  private static byte[] basic_x4_c000 = null;
  private static byte[] basic_c000    = null;
  private static byte[] hc900_e000    = null;
  private static byte[] hc900_f000    = null;
  private static byte[] caos22_e000   = null;
  private static byte[] caos22_f000   = null;
  private static byte[] caos31_e000   = null;
  private static byte[] caos42_c000   = null;
  private static byte[] caos42_e000   = null;
  private static byte[] caos45_c000   = null;
  private static byte[] caos45_e000   = null;

  private static Boolean defaultEmulateVideoTiming = null;

  private int                     kcTypeNum;
  private String                  propPrefix;
  private String                  basicFile;
  private String                  caosFileC;
  private String                  caosFileE;
  private String                  caosFileF;
  private String                  m052File;
  private Boolean                 kout;
  private volatile boolean        blinkEnabled;
  private volatile boolean        blinkState;
  private volatile boolean        hiColorRes;
  private boolean                 charSetUnknown;
  private boolean                 ctrlKeysDirect;
  private boolean                 umlautsTo2Codes;
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
  private boolean                 audioOutPhaseL;
  private boolean                 audioOutPhaseR;
  private boolean                 audioInPhase;
  private int                     audioInTStates;
  private int                     ram8SegNum;
  private int                     keyNumStageBuf;
  private int                     keyNumStageNum;
  private long                    keyNumStageMillis;
  private volatile int            keyNumPressed;
  private int                     keyNumProcessing;
  private int                     keyShiftBitCnt;
  private int                     keyShiftValue;
  private int                     keyTStates;
  private volatile int            tStatesLinePos0;
  private volatile int            tStatesLinePos1;
  private volatile int            tStatesLinePos2;
  private volatile int            tStatesPerLine;
  private int                     lineTStateCounter;
  private int                     lineCounter;
  private int                     basicSegAddr;
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
  private String                  sysName;
  private AbstractKC85KeyboardFld keyboardFld;
  private Z80PIO                  pio;
  private Z80CTC                  ctc;
  private KC85JoystickModule      joyModule;
  private VDIP                    vdip;
  private D004                    d004;
  private String                  d004RomProp;


  public KC85( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.charSetUnknown = false;
    this.ctrlKeysDirect = false;
    this.pasteFast      = false;
    this.keyboardFld    = null;
    this.basicFile      = null;
    this.caosFileC      = null;
    this.caosFileE      = null;
    this.caosFileF      = null;
    this.m052File       = null;

    this.sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( this.sysName.equals( "HC900" ) ) {
      this.kcTypeNum  = 2;
      this.propPrefix = "jkcemu.hc900.";
    } else if( this.sysName.equals( "KC85/2" ) ) {
      this.kcTypeNum  = 2;
      this.propPrefix = "jkcemu.kc85_2.";
    } else if( this.sysName.equals( "KC85/3" ) ) {
      this.kcTypeNum  = 3;
      this.propPrefix = "jkcemu.kc85_3.";
    } else if( this.sysName.equals( "KC85/4" ) ) {
      this.kcTypeNum  = 4;
      this.propPrefix = "jkcemu.kc85_4.";
    } else {
      this.kcTypeNum  = 5;
      this.propPrefix = "jkcemu.kc85_5.";
    }

    // emulierte Hardware konfigurieren
    this.ram8 = null;
    if( this.kcTypeNum == 4 ) {
      this.ram8 = this.emuThread.getExtendedRAM( 0x8000 );	// 32K
    } else if( this.kcTypeNum > 4 ) {
      this.ram8 = this.emuThread.getExtendedRAM( 0x38000 );	// 224K
    }
    this.ramColor0 = new byte[ 0x4000 ];
    this.ramColor1 = new byte[ 0x4000 ];
    this.ramPixel0 = new byte[ 0x4000 ];
    this.ramPixel1 = new byte[ 0x4000 ];

    Z80CPU cpu       = emuThread.getZ80CPU();
    this.ctc         = new Z80CTC( "CTC (IO-Adressen 8C-8F)" );
    this.pio         = new Z80PIO( "PIO (IO-Adressen 88-8B)" );
    this.joyModule   = null;
    this.d004        = null;
    this.d004RomProp = "";
    if( emulatesD004( props ) ) {
      byte[] romBytes  = null;
      this.d004RomProp = getD004RomProp( props );
      if( (this.d004RomProp.length() > 5)
	  && this.d004RomProp.startsWith( "file:" ) )
      {
	try {
	  romBytes = EmuUtil.readFile(
			new File( this.d004RomProp.substring( 5 ) ),
			16 * 1024 );
	}
	catch( IOException ex ) {
	    BasicDlg.showErrorDlg(
		this.emuThread.getScreenFrm(),
		"D004-ROM-Datei kann nicht geladen werden.\n"
			+ "Es wird der Standard-ROM verwendet.",
		ex );
	}
      }
      if( romBytes == null ) {
	String resource = null;
	if( this.d004RomProp.equals( "2.0" ) ) {
	  resource = "/rom/kc85/d004_20.bin";
	} else if( this.d004RomProp.equals( "3.3" ) ) {
	  if( this.kcTypeNum >= 4 ) {
	    resource = "/rom/kc85/d004_33_4.bin";
	  } else {
	    resource = "/rom/kc85/d004_33_3.bin";
	  }
	} else {
	  if( this.kcTypeNum > 4 ) {
	    resource = "/rom/kc85/d004_33_4.bin";
	  } else {
	    resource = "/rom/kc85/d004_20.bin";
	  }
	}
	romBytes = EmuUtil.readResource(
				this.emuThread.getScreenFrm(),
				resource );
      }
      this.d004 = new D004(
			this.emuThread.getScreenFrm(),
			props,
			this.propPrefix,
			romBytes );
    }
    this.vdip    = null;
    this.modules = createModules( props );
    if( this.modules != null ) {
      try {
	java.util.List<Z80InterruptSource> iSources
				= new ArrayList<Z80InterruptSource>();
	iSources.add( this.ctc );
	iSources.add( this.pio );
	for( int i = 0; i < this.modules.length; i++ ) {
	  AbstractKC85Module module = this.modules[ i ];
	  if( module instanceof M052 ) {
	    if( this.vdip == null ) {
	      this.vdip = ((M052) module).getVDIP();
	    }
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
    cpu.addTStatesListener( this );
    cpu.addMaxSpeedListener( this );

    this.screenBufUsed  = null;
    this.screenBufSaved = null;
    this.rgbValues      = new int[ basicRGBValues.length ];
    this.colors         = new Color[ basicRGBValues.length ];
    applySettings( props );
    z80MaxSpeedChanged( cpu );
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
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
    return (Main.availableProcessors() >= 2);
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    return sysName.equals( "KC85/4" ) || sysName.equals( "KC85/5" ) ?
								1773 : 1750;
  }


  public int getKCTypeNum()
  {
    return this.kcTypeNum;
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
	  this.audioOutPhaseR = !this.audioOutPhaseR;
	  updAudioOut();
	  break;

	case 1:
	  this.audioOutPhaseL = !this.audioOutPhaseL;
	  updAudioOut();
	  break;

	case 2:
	  this.blinkState  = !this.blinkState;
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
    int f = (this.kcTypeNum < 4 ? 1750 : 1773);
    int t = cpu.getMaxSpeedKHz() * 112 / f;
    this.tStatesLinePos0 = (int) Math.round( t * 32.0 / 112.0 );
    this.tStatesLinePos1 = (int) Math.round( t * 64.0 / 112.0 );
    this.tStatesLinePos2 = (int) Math.round( t * 96.0 / 112.0 );
    this.tStatesPerLine  = t;
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.z80TStatesProcessed( cpu, tStates );

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
     * Der Kassettenrecorderanschluss wird eingangsseitig emuliert,
     * indem zyklisch geschaut wird, ob sich die Eingangsphase geaendert hat.
     * Wenn ja, wird ein Impuls an der Strobe-Leitung der zugehoerigen PIO
     * emuliert.
     * Auf der einen Seite soll das Audiosystem nicht zu oft abgefragt
     * werden.
     * Auf der anderen Seite soll aber die Zykluszeit nicht so gross sein,
     * dass die Genauigkeit der Zeitmessung kuenstlich verschlechert wird.
     * Aus diesem Grund werden genau soviele Taktzyklen abgezaehlt,
     * wie auch der Vorteile der CTC mindestens zaehlen muss.
     */
    this.audioInTStates += tStates;
    if( this.audioInTStates > 15 ) {
      this.audioInTStates = 0;
      if( this.emuThread.readAudioPhase() != this.audioInPhase ) {
	this.audioInPhase = !this.audioInPhase;
	this.pio.strobePortA();
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


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    if( EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "emulate_video_timing",
				getDefaultEmulateVideoTiming() ) )
    {
      if( this.screenBufSaved == null ) {
	this.screenBufSaved = new byte[ 320 * 256 ];
      }
      this.screenBufUsed = this.screenBufSaved;
    } else {
      this.screenBufUsed = null;
    }
    this.umlautsTo2Codes = EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "umlauts_to_2_keycodes",
			false );
    createColors( props );
    applyPasteFast( props );
    if( this.d004 != null ) {
      this.d004.applySettings( props );
    }
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean hasD004 = emulatesD004( props );
    boolean rv      = EmuUtil.getProperty(
			props,
			"jkcemu.system" ).equals( this.sysName );
    if( rv && (this.kcTypeNum > 2)
	&& !TextUtil.equals(
		this.basicFile,
		getProperty( props, "rom.basic.file" ) ) )
    {
      rv = false;
    }
    if( rv && (this.kcTypeNum > 3)
	&& !TextUtil.equals(
		this.caosFileC,
		getProperty( props, "rom.caos_c.file" ) ) )
    {
      rv = false;
    }
    if( rv && !TextUtil.equals(
		this.caosFileE,
		getProperty( props, "rom.caos_e.file" ) ) )
    {
      rv = false;
    }
    if( rv && !TextUtil.equals(
		this.caosFileF,
		getProperty( props, "rom.caos_f.file" ) ) )
    {
      rv = false;
    }
    if( rv && !TextUtil.equals(
		this.m052File,
		getProperty( props, "rom.m052.file" ) ) )
    {
      rv = false;
    }
    if( rv ) {
      if( hasD004 ) {
	if( (this.d004 == null)
	    || !this.d004RomProp.equals( getD004RomProp( props ) ) )
	{
	  rv = false;
	}
      } else {
	if( this.d004 != null ) {
	  rv = false;
	}
      }
    }
    if( rv && (this.d004 != null) ) {
      rv = this.d004.canApplySettings( props );
    }
    if( rv ) {
      int                  nModules = 0;
      AbstractKC85Module[] modules  = this.modules;
      if( modules != null ) {
	nModules = modules.length;
	if( hasD004 && (nModules > 0) ) {
	  --nModules;
	}
      }
      if( props != null ) {
	int nRemain = EmuUtil.getIntProperty(
				props,
				this.propPrefix + "module.count",
				0 );
	if( nRemain != nModules ) {
	  rv = false;
	}
	if( rv && (nRemain > 0) && (modules != null) ) {
	  java.util.List<String[]> modEntries
				= new ArrayList<String[]>( nRemain + 1 );
	  if( props != null ) {
	    boolean loop    = true;
	    int     slot    = 8;
	    while( loop && (slot < 0x100) && (nRemain > 0) ) {
	      loop = false;

	      String prefix = String.format(
					"%smodule.%02X.",
					this.propPrefix,
					slot );
	      String modName = props.getProperty( prefix + "name" );
	      if( modName != null ) {
		if( !modName.isEmpty() ) {
		  String typeByte = props.getProperty( prefix + "typebyte" );
		  String fileName = props.getProperty( prefix + "file" );

		  String[] modEntry = new String[ 3 ];
		  modEntry[ 0 ]     = modName;
		  modEntry[ 1 ]     = typeByte;
		  modEntry[ 2 ]     = fileName;
		  modEntries.add( modEntry );
		  loop = true;
		}
	      }
	      if( loop ) {
		slot += 4;
		--nRemain;
	      }
	    }
	    if( hasD004 && (this.d004 != null) ) {
	      String[] modEntry = new String[ 3 ];
	      modEntry[ 0 ]     = this.d004.getModuleName();
	      modEntry[ 1 ]     = this.d004.getTypeByteText();
	      modEntry[ 2 ]     = null;
	      modEntries.add( modEntry );
	    }
	  }
	  int n = modEntries.size();
	  if( modules.length == n ) {
	    for( int i = 0; i < n; i++ ) {
	      String[] modEntry = modEntries.get( i );
	      if( modEntry == null ) {
		rv = false;
		break;
	      }
	      if( modEntry.length < 3 ) {
		rv = false;
		break;
	      }
	      if( !modules[ i ].equalsModule(
					modEntry[ 0 ],
					modEntry[ 1 ],
					modEntry[ 2 ] ) )
	      {
		rv = false;
		break;
	      }
	    }
	  } else {
	    rv = false;
	  }
	}
      } else {
	if( nModules > 0 ) {
	  rv = false;
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
  public AbstractKeyboardFld createKeyboardFld() throws UserCancelException
  {
    if( this.kcTypeNum >= 4 ) {
      switch( BasicDlg.showOptionDlg(
		this.screenFrm,
		"Welche Tastatur m\u00F6chten Sie sehen,\n"
			+ "die originale oder die D005?",
		"Tastaturauswahl",
		new String[] { "Original", "D005", "Abbrechen" } ) )
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
	  if( this.hiColorRes ) {
	    if( (p & m) != 0 ) {
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
	    rv = getColorIndex( c, (p & m) != 0 );
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
  public int getCharColCount()
  {
    return 40;
  }


  @Override
  public int getCharHeight()
  {
    return 8;
  }


  @Override
  public int getCharRowCount()
  {
    return 32;
  }


  @Override
  public int getCharRowHeight()
  {
    return 8;
  }


  @Override
  public int getCharWidth()
  {
    return 8;
  }


  @Override
  public boolean getDefaultFloppyDiskBlockNum16Bit()
  {
    return true;
  }


  @Override
  public int getDefaultFloppyDiskBlockSize()
  {
    return this.d004 != null ? 2048 : -1;
  }


  @Override
  public int getDefaultFloppyDiskDirBlocks()
  {
    return this.d004 != null ? 2 : -1;
  }


  @Override
  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return this.d004 != null ?
		FloppyDiskFormat.getFormat( 2, 80, 5, 1024 )
		: null;
  }


  @Override
  public int getDefaultFloppyDiskSystemTracks()
  {
    return this.d004 != null ? 2 : -1;
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
    return getMemByteInternal( addr, this.irmEnabled );
  }


  @Override
  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return resetLevel == EmuThread.ResetLevel.WARM_RESET ? 0xE000 : 0xF000;
  }


  @Override
  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int idx = 0x3200 + (chY * 40) + chX;
    if( (idx >= 0) && (idx < this.ramPixel0.length) ) {
      int b = (int) this.ramPixel0[ idx ] & 0xFF;
      switch( b ) {
	case 0:
	  ch = 0x20;
	  break;

	case 0x5B:			// volles Rechteck -> herausfiltern
	  ch = -1;
	  break;

	case 0x5C:
	  ch = '|';
	  break;

	case 0x5D:
	  ch = '\u00AC';		// Negationszeichen
	  break;

	case 0x60:
	  if( this.kcTypeNum > 2 ) {
	    ch = '\u00A9';		// Copyright-Symbol
	  } else {
	    ch = '@';
	  }
	  break;

	case 0x7B:
	  if( this.kcTypeNum > 2 ) {
	    ch = '\u00E4';		// ae-Umlaut
	  } else {
	    ch = '|' ;
	  }
	  break;

	case 0x7C:
	  if( this.kcTypeNum > 2 ) {
	    ch = '\u00F6';		// oe-Umlaut
	  } else {
	    ch = '\u00AC';		// Negationszeichen
	  }
	  break;

	case 0x7D:
	  if( this.kcTypeNum > 2 ) {
	    ch = '\u00FC';		// ue-Umlaut
	  } else {
	    ch = -1;
	  }
	  break;

	case 0x7E:
	  if( this.kcTypeNum > 2 ) {
	    ch = '\u00DF';		// sz-Umlaut
	  } else {
	    ch = -1;
	  }
	  break;

	default:
	  if( (b >= 0x20) && (b < 0x7F) ) {
	    ch = b;
	  }
      }
    }
    return ch;
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
  public String getSecondSystemName()
  {
    return this.d004 != null ? "D004" : null;
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


  public int getSupportedJoystickCount()
  {
    return this.joyModule != null ? 1 : 0;
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
  protected VDIP getVDIP()
  {
    return this.vdip;
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
    boolean rv     = false;
    int     keyNum = -1;
    switch( keyCode ) {
      case KeyEvent.VK_BACK_SPACE:
	keyNum = 6;
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

      case KeyEvent.VK_ENTER:
	keyNum = 126;
	break;

      case KeyEvent.VK_HOME:
	keyNum = (shiftDown ? 9 : 8);
	break;

      case KeyEvent.VK_INSERT:
	keyNum = (shiftDown ? 57 : 56);
	break;

      case KeyEvent.VK_TAB:
	if( this.ctrlKeysDirect ) {
	  pasteCharToKeyBuffer( '\u0009', false );
	  rv = true;
	} else {
	  keyNum = 122;
	}
	break;

      case KeyEvent.VK_ESCAPE:
	if( this.ctrlKeysDirect ) {
	  pasteCharToKeyBuffer( '\u001B', false );
	  rv = true;
	} else {
	  keyNum = 77;
	}
	break;

      case KeyEvent.VK_DELETE:
	keyNum = 40;
	break;

      case KeyEvent.VK_SPACE:
	keyNum = (shiftDown ? 71 : 70);
	break;

      case KeyEvent.VK_F1:
	keyNum = (shiftDown ? 125 : 124);
	break;

      case KeyEvent.VK_F2:
	keyNum = (shiftDown ? 13 : 12);
	break;

      case KeyEvent.VK_F3:
	keyNum = (shiftDown ? 29 : 28);
	break;

      case KeyEvent.VK_F4:
	keyNum = (shiftDown ? 109 : 108);
	break;

      case KeyEvent.VK_F5:
	keyNum = (shiftDown ? 45 : 44);
	break;

      case KeyEvent.VK_F6:
	keyNum = (shiftDown ? 93 : 92);
	break;

      case KeyEvent.VK_F7:		// BRK
	keyNum = 60;
	break;

      case KeyEvent.VK_F8:		// STOP
	keyNum = (shiftDown ? 77 : 76);
	break;

      case KeyEvent.VK_F9:		// CLR
	keyNum = (shiftDown ? 25 : 24);
	break;
    }
    if( keyNum >= 0 ) {
      this.keyNumPressed = keyNum;
      rv                 = true;
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    this.keyNumPressed = -1;
    updKeyboardFld();
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( this.ctrlKeysDirect && (ch > 0) && (ch < 0x20) ) {
      pasteCharToKeyBuffer( ch, false );
      rv = true;
    } else {
      rv = keyTypedInternal( ch );
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  public void loadIntoSecondSystem(
			byte[] loadData,
			int    loadAddr,
			int    startAddr )
  {
    if( this.d004 != null )
      this.d004.loadIntoRAM( loadData, loadAddr, startAddr );
  }


  /*
   * Dateien sollen nicht in den IRM geladen werden.
   */
  @Override
  public void loadMemByte( int addr, int value )
  {
    setMemByteInternal( addr, value, false );
  }


  @Override
  public void openBasicProgram()
  {
    SourceUtil.openKCBasicStyleProgram(
				this.screenFrm,
				getKCBasicBegAddr(),
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
	pasteCharToKeyBuffer( ch, true );
	rv = true;
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
	rv = this.pio.readPortA();
	break;

      case 0x89:
	rv = this.pio.readPortB();
	break;

      case 0x8A:
	rv = this.pio.readControlA();
	break;

      case 0x8B:
	rv = this.pio.readControlB();
	break;

      case 0x8C:
      case 0x8D:
      case 0x8E:
      case 0x8F:
	rv = this.ctc.read( port & 0x03 );
	break;

      default:
	if( this.modules != null ) {
	  for( int i = 0; i < this.modules.length; i++ ) {
	    int v = this.modules[ i ].readIOByte( port );
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
    if( memory == this.emuThread ) {
      int ix     = this.emuThread.getZ80CPU().getRegIX();
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
	      buf.append( (char) '\'' );
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
	    buf.append( (char) '\'' );
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
	    buf.append( (char) '\n' );
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
	  if( memory.getMemWord( addr + 1 ) == 0xF003 ) {
	    int bol = buf.length();
	    int idx = memory.getMemByte( addr + 3, false );
	    if( !sourceOnly ) {
	      buf.append( String.format( "%04X  %02X 03 F0", addr, b ) );
	    }
	    appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( s );
	    appendSpacesToCol( buf, bol, colArgs );
	    buf.append( "0F003H\n" );
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
	      buf.append( (char) '0' );
	    }
	    buf.append( String.format( "%02XH", idx ) );
	    if( (idx >= 0) && (idx < sysCallNames.length) ) {
	      appendSpacesToCol( buf, bol, colRemark );
	      buf.append( (char) ';' );
	      buf.append( sysCallNames[ idx ] );
	    }
	    buf.append( (char) '\n' );
	    rv = 4;
	  }
	}
      }
      if( rv == 0 ) {
	// direkter Aufruf einer Systemfunktion pruefen
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
	  int p = ((getMemByteInternal( SUTAB + 1, true ) << 8) & 0xFF00)
			| (getMemByteInternal( SUTAB, true ) & 0x00FF);
	  int idx = 0;
	  while( idx < sysCallNames.length ) {
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
		buf.append( (char) ',' );
	      }
	      if( a >= 0xA000 ) {
		buf.append( (char) '0' );
	      }
	      buf.append( String.format( "%04XH", a ) );
	      appendSpacesToCol( buf, bol, colRemark );
	      buf.append( (char) ';' );
	      buf.append( sysCallNames[ idx ] );
	      buf.append( (char) '\n' );
	      rv = 3;
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
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    boolean coldReset = false;
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      coldReset = true;
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
      if( this.ram8 != null ) {
	Arrays.fill( this.ram8, (byte) 0 );
      }
      Arrays.fill( this.ramColor0, (byte) 0 );
      Arrays.fill( this.ramColor1, (byte) 0 );
      Arrays.fill( this.ramPixel0, (byte) 0 );
      Arrays.fill( this.ramPixel1, (byte) 0 );
      if( this.modules != null ) {
	for( int i = 0; i < this.modules.length; i++ ) {
	  this.modules[ i ].clearRAM();
	}
      }
    }
    if( resetLevel == EmuThread.ResetLevel.COLD_RESET) {
      coldReset = true;
    }
    if( coldReset ) {
      this.basicSegAddr     = 0x3000;
      this.basicC000Enabled = false;
      this.caosC000Enabled  = false;
      this.caosE000Enabled  = true;
      this.hiColorRes       = false;
      this.irmEnabled       = true;
      this.ram0Enabled      = true;
      this.ram0Writeable    = true;
      this.ram4Enabled      = (this.kcTypeNum > 3);
      this.ram4Writeable    = this.ram4Enabled;
      this.ram8Enabled      = false;
      this.ram8SegNum       = 2;
      this.ram8Writeable    = false;
      this.ramColorEnabled  = false;
      this.screen1Enabled   = false;
      this.screen1Visible   = false;
    }
    this.kout                 = null;
    this.blinkEnabled         = false;
    this.blinkState           = false;
    this.biState              = false;
    this.h4State              = false;
    this.screenDirty          = true;
    this.screenRefreshEnabled = false;
    this.audioOutPhaseL       = false;
    this.audioOutPhaseL       = false;
    this.audioInPhase         = this.emuThread.readAudioPhase();
    this.audioInTStates       = 0;
    this.lineTStateCounter    = 0;
    this.lineCounter          = 0;
    this.keyNumStageBuf       = 0;
    this.keyNumStageNum       = 0;
    this.keyNumStageMillis    = -1;
    this.keyNumPressed        = -1;
    this.keyNumProcessing     = -1;
    this.keyShiftBitCnt       = 0;
    this.keyShiftValue        = 0;
    this.keyTStates           = 0;
    this.ctc.reset( coldReset );
    this.pio.reset( coldReset );
    updAudioOut();
  }


  @Override
  public void saveBasicProgram()
  {
    SourceUtil.saveKCBasicStyleProgram(
				this.screenFrm,
				getKCBasicBegAddr() );
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
    if( joyModule != null )
      joyModule.setJoystickAction( joyNum, actionMask );
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
    int m = 0;
    switch( port & 0xFF ) {
      case 0x80:
	if( this.modules != null ) {
	  int slot = (port >> 8) & 0xFF;
	  for( int i = 0; i < this.modules.length; i++ ) {
	    if( this.modules[ i ].getSlot() == slot ) {
	      this.modules[ i ].setStatus( value );
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
	  this.basicSegAddr    = (value << 8) & 0x6000;
	  this.caosC000Enabled = ((value & 0x80) != 0);
	}
	break;

      case 0x88:
	this.pio.writePortA( value );
	m = this.pio.fetchOutValuePortA( false );
	this.caosE000Enabled  = ((m & 0x01) != 0);
	this.ram0Enabled      = ((m & 0x02) != 0);
	this.irmEnabled       = ((m & 0x04) != 0);
	this.ram0Writeable    = ((m & 0x08) != 0);
	this.basicC000Enabled = ((m & 0x80) != 0);
	if( this.kcTypeNum >= 4 ) {
	  boolean kout = ((m & 0x10) != 0);
	  if( this.keyboardFld != null ) {
	    if( this.kout != null ) {
	      if( kout != this.kout.booleanValue() ) {
		this.keyboardFld.fireKOut();
	      }
	    } else {
	      this.keyboardFld.fireKOut();
	    }
	  }
	  this.kout = new Boolean( kout );
	}
	if( this.screenBufUsed != null ) {
	  this.screenDirty = true;
	} else {
	  this.screenFrm.setScreenDirty( true );
	}
	break;

      case 0x89:
	this.pio.writePortB( value );
	m = this.pio.fetchOutValuePortB( false );
	if( this.kcTypeNum > 3 ) {
	  if( (m & 0x01) == 0 ) {
	    this.audioOutPhaseL = false;
	    this.audioOutPhaseR = false;
	    updAudioOut();
	  }
	  this.ram8Enabled   = ((m & 0x20) != 0);
	  this.ram8Writeable = ((m & 0x40) != 0);
	}
	this.blinkEnabled = ((m & 0x80) != 0);
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
	this.ctc.write( port & 0x03, value );
	break;

      default:
	if( this.modules != null ) {
	  for( int i = 0; i < this.modules.length; i++ ) {
	    if( this.modules[ i ].writeIOByte( port, value ) ) {
	      break;
	    }
	  }
	}
    }
  }


	/* --- private Methoden --- */

  private void applyPasteFast( Properties props )
  {
    this.ctrlKeysDirect = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "control_keys.direct",
				false );
    this.pasteFast = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "paste.fast",
				true );
  }


  private void createColors( Properties props )
  {
    float brightness = getBrightness( props );
    if( (brightness >= 0F) && (brightness <= 1F) ) {
      for( int i = 0; i < basicRGBValues.length; i++ ) {
	int r = Math.round( basicRGBValues[ i ][ 0 ] * brightness );
	int g = Math.round( basicRGBValues[ i ][ 1 ] * brightness );
	int b = Math.round( basicRGBValues[ i ][ 2 ] * brightness );

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
				this.propPrefix + "module.count",
				0 );
      if( nRemain > 0 ) {
	modules = new ArrayList<AbstractKC85Module>();
	int     slot = 8;
	boolean loop = true;
	while( loop && (slot < 0x100) && (nRemain > 0) ) {
	  String prefix = String.format(
					"%smodule.%02X.",
					this.propPrefix,
					slot );
	  String moduleName = props.getProperty( prefix + "name" );
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
	      int    typeByte = 0xF7;
	      String text     = props.getProperty( prefix + "typebyte" );
	      if( text != null ) {
		if( text.equals( "FB" ) ) {
		  typeByte = 0xFB;
		}
	      }
	      modules.add(
		new M025(
			slot,
			this.emuThread,
			typeByte,
			this.screenFrm,
			props.getProperty( prefix + "file" ) ) );
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
	      int    typeByte = 0xF8;
	      String text     = props.getProperty( prefix + "typebyte" );
	      if( text != null ) {
		if( text.equals( "FC" ) ) {
		  typeByte = 0xFC;
		}
	      }
	      modules.add(
		new M028(
			slot,
			this.emuThread,
			typeByte,
			this.screenFrm,
			props.getProperty( prefix + "file" ) ) );
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
	      modules.add( new M035( slot ) );
	      modules.add( new M035( slot + 1 ) );
	      modules.add( new M035( slot + 2 ) );
	      modules.add( new M035( slot + 3 ) );
	    }
	    else if( moduleName.equals( "M036" ) ) {
	      modules.add( new KC85SegmentedRAMModule(
					slot, 0x78, "M036", 0x20000 ) );
	    }
	    else if( moduleName.equals( "M040" ) ) {
	      int    typeByte = 0xF7;
	      String text     = props.getProperty( prefix + "typebyte" );
	      if( text != null ) {
		if( text.equals( "1" ) || text.equals( "01" ) ) {
		  typeByte = 0x01;
		} else if( text.equals( "F8" ) ) {
		  typeByte = 0xF8;
		}
	      }
	      modules.add(
		new M040(
			slot,
			this.emuThread,
			typeByte,
			this.screenFrm,
			props.getProperty( prefix + "file" ) ) );
	    }
	    else if( moduleName.equals( "M052" ) ) {
	      modules.add( new M052(
		slot,
		this.emuThread.getScreenFrm(),
		props.getProperty( this.propPrefix + "rom.m052.file" ) ) );
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
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "d004.enabled",
				false );
  }


  private int getColorIndex( int colorByte, boolean foreground )
  {
    if( !this.hiColorRes
	&& this.blinkEnabled
	&& this.blinkState
	&& ((colorByte & 0x80) != 0) )
    {
      foreground = false;
    }
    return foreground ? ((colorByte >> 3) & 0x0F) : ((colorByte & 0x07) + 16);
  }


  private String getD004RomProp( Properties props )
  {
    String s = EmuUtil.getProperty(
			props,
			this.propPrefix + "d004.rom" );
    return s.equals( "2.0" )
		|| s.equals( "3.2" )
		|| s.equals( "3.3" )
		|| s.startsWith( "file:" ) ? s : "standard";
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


  private int getMemByteInternal( int addr, boolean irmEnabled )
  {
    addr &= 0xFFFF;

    int rv = -1;
    if( (addr >= 0) && (addr < 0x4000) ) {
      if( this.ram0Enabled ) {
	rv = this.emuThread.getRAMByte( addr );
      }
    } else if( (addr >= 0x4000) && (addr < 0x8000) ) {
      if( this.ram4Enabled ) {
	rv = this.emuThread.getRAMByte( addr );
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
      }
    } else if( (addr >= 0xC000) && (addr < 0xE000) ) {
      int idx = addr - 0xC000;
      if( this.caosC000Enabled && (this.caosC000 != null) ) {
	if( idx < this.caosC000.length ) {
	  rv = (int) this.caosC000[ idx ] & 0xFF;
	}
      } else if( this.basicC000Enabled && (this.basicC000 != null) ) {
	if( (this.kcTypeNum > 4) && (this.basicC000.length > 0x2000) ) {
	  idx += this.basicSegAddr;
	}
	if( idx < this.basicC000.length ) {
	  rv = (int) basicC000[ idx ] & 0xFF;
	}
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
      }
    }
    if( (rv < 0) && (this.modules != null) ) {
      for( int i = 0; i < this.modules.length; i++ ) {
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
   *   1. zweimal erster Tastencode lesen
   *   2. zweimal kein Tastencode lesen
   *   3. viermal zweiter Tastencode lesen
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


  /*
   * Deutsche Umlaute, geschweifte und eckige Klammern, Backslash
   * sowie Tilde koennen mit der originalen KC85-Tastatur
   * nicht eingegeben werden, da es dafuer keine Tastencodes gibt.
   * Aus diesem Grund werden diese Tasten je nach eingeschalteter Option
   * in 2-Byte-Tastencodes entsprechend des MicroDOS-Modes der D005 gemappt.
   */
  private boolean keyTypedInternal( char ch )
  {
    boolean rv = false;
    if( this.umlautsTo2Codes ) {
      rv = true;
      switch( ch ) {
	case '[':
	case '\u00C4':			// Ae
	  this.keyNumPressed = ((124 << 8) & 0xFF00) | 100;
	  break;
	case '\\':
	case '\u00D6':			// Oe
	  this.keyNumPressed = ((124 << 8) & 0xFF00) | 36;
	  break;
	case ']':
	case '\u00DC':			// Ue
	  this.keyNumPressed = ((124 << 8) & 0xFF00) | 84;
	  break;
	case '{':
	case '\u00E4':			// ae
	  this.keyNumPressed = ((124 << 8) & 0xFF00) | 116;
	  break;
	case '\u00F6':			// oe
	  this.keyNumPressed = ((124 << 8) & 0xFF00) | 4;
	  break;
	case '}':
	case '\u00FC':			// ue
	  this.keyNumPressed = ((124 << 8) & 0xFF00) | 20;
	  break;
	case '~':
	case '\u00DF':			// sz
	  this.keyNumPressed = ((124 << 8) & 0xFF00) | 42;
	  break;
	default:
	  rv = false;
      }
    }
    if( !rv ) {
      if( (ch > 0) && (ch < char2KeyNum.length) ) {
	int keyNum = char2KeyNum[ ch ];
	if( keyNum >= 0 ) {
	  this.keyNumPressed = keyNum;
	  rv                 = true;
	}
      }
    }
    return rv;
  }


  private void loadROMs( Properties props )
  {
    String resourceBasic = null;
    String resourceCaosC = null;
    String resourceCaosE = null;
    String resourceCaosF = null;
    if( this.kcTypeNum == 2 ) {
      if( this.sysName.equals( "HC900" ) ) {
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
      resourceBasic = "/rom/kc85/user45_c000.bin";
      resourceCaosC = "/rom/kc85/caos45_c000.bin";
      resourceCaosE = "/rom/kc85/caos45_e000.bin";
    }

    // BASIC-ROM
    this.basicFile = null;
    this.basicC000 = null;
    if( this.kcTypeNum > 2 ) {
      this.basicFile = getProperty( props, "rom.basic.file" );
      this.basicC000 = readFile(
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
    if( this.kcTypeNum > 3 ) {
      this.caosFileC = getProperty( props, "rom.caos_c.file" );
      this.caosC000  = readFile(
			this.caosFileC,
			this.kcTypeNum < 5 ? 0x1000 : 0x2000,
			"CAOS-ROM C" );
    }
    if( this.caosC000 != null ) {
      this.charSetUnknown = true;
    } else {
      this.caosC000 = getResource( resourceCaosC );
    }

    // CAOS-ROM E
    this.caosFileE = getProperty( props, "rom.caos_e.file" );
    this.caosE000  = readFile(
			this.caosFileE,
			this.kcTypeNum < 3 ? 0x0800 : 0x2000,
			"CAOS-ROM E oder E+F" );
    if( this.caosE000 != null ) {
      this.charSetUnknown = true;
    } else {
      this.caosE000 = getResource( resourceCaosE );
    }

    // CAOS-ROM F
    this.caosFileF = getProperty( props, "rom.caos_f.file" );
    this.caosF000  = readFile(
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
    this.m052File = getProperty( props, "rom.m052.file" );

    // Module
    AbstractKC85Module[] modules  = this.modules;
    if( modules != null ) {
      for( AbstractKC85Module module : modules ) {
	module.reload( this.screenFrm );
      }
    }
  }


  private void pasteCharToKeyBuffer( char ch, boolean forceWait )
  {
    try {
      Z80CPU cpu = emuThread.getZ80CPU();
      int    ix  = cpu.getRegIX();
      int    n   = 10;
      while( (n > 0) && (getMemByte( ix + 8, false ) & 0x01) != 0 ) {
	Thread.sleep( 10 );
	if( !forceWait ) {
	  --n;
	}
      }
      if( n > 0 ) {
	setMemByte( ix + 13, ch );
	setMemByte( ix + 8, getMemByte( ix + 8, false ) | 0x01 );
      }
    }
    catch( InterruptedException ex ) {}
  }


  private boolean setMemByteInternal( int addr, int value, boolean irmEnabled )
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
      int idx = addr - 0xC000;
      if( this.caosC000Enabled && (this.caosC000 != null) ) {
	if( idx < this.caosC000.length ) {
	  done = true;
	}
      } else if( this.basicC000Enabled && (this.basicC000 != null) ) {
	if( idx < this.basicC000.length ) {
	  done = true;
	}
      }
    } else if( addr >= 0xE000 ) {
      if( this.caosE000Enabled ) {
	if( ((this.kcTypeNum >= 3) || (addr < 0xF000))
	    && (this.caosE000 != null) )
	{
	  int idx = addr - 0xE000;
	  if( idx < caosE000.length ) {
	    done = true;
	  }
	}
	else if( this.caosF000 != null ) {
	  int idx = addr - 0xF000;
	  if( idx < caosF000.length ) {
	    done = true;
	  }
	}
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


  private void updAudioOut()
  {
    if( this.emuThread.isSoundOutEnabled() ) {
      int value = 0;
      if( this.audioOutPhaseL == this.audioOutPhaseR ) {
	value = (this.audioOutPhaseL ? 127 : -127);
	int m = this.pio.fetchOutValuePortB( false );
	if( (m & 0x10) != 0 ) {
	  value = (value * 30) / 100;		// 1.0 / (2.35 + 1.0)
	}
	if( (m & 0x08) != 0 ) {
	  value = (value * 46) / 100;		// 2.0 / (2.35 + 2.0)
	}
	if( (m & 0x04) != 0 ) {
	  value = (value * 62) / 100;		// 3.9 / (2.35 + 3.9)
	}
	if( (m & 0x02) != 0 ) {
	  value = (value * 78) / 100;		// 8.2 / (2.35 + 8.2)
	}
	if( (this.kcTypeNum < 4) && ((m & 0x01) != 0) ) {
	  value = (value * 87) / 100;		// 16.0 / (2.35 + 16.0)
	}
      }
      this.emuThread.writeAudioValue( (byte) value );
    } else {
      this.emuThread.writeAudioPhase( this.audioOutPhaseL );
    }
  }


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.keyNumPressed );
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
	      int colorIdx = 0;
	      if( this.hiColorRes ) {
		if( (p & m) != 0 ) {
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
		colorIdx = getColorIndex( c, (p & m) != 0 );
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
}
