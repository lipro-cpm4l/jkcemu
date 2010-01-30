/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Computer HC900 und KC85/2..5
 */

package jkcemu.system;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.system.kc85.*;
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

  private static final int MEM_ARGC      = 0xB780;
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
			{ 0,   0,   0   },	// schwarz
			{ 0,   0,   255 },	// blau
			{ 255, 0,   0   },	// rot
			{ 255, 0,   255 },	// purpur
			{ 0,   255, 0   },	// gruen
			{ 0,   255, 255 },	// tuerkis
			{ 255, 255, 0   },	// gelb
			{ 255, 255, 255 },	// weiss

			// Vordergrundfarben mit 30 Grad Drehung im Farbkreis
			{ 0,   0,   0   },	// schwarz
			{ 75,  0,   180 },	// violett
			{ 180, 75,  0   },	// orange
			{ 180, 0,   138 },	// purpurrot
			{ 0,   180, 75  },	// gruenblau
			{ 0,   138, 180 },	// blaugruen
			{ 138, 255, 0   },	// gelbgruen
			{ 255, 255, 255 },	// weiss

			// Hintergrundfarben (30% dunkler)
			{ 0,   0,   0   },	// schwarz
			{ 0,   0,   180 },	// blau
			{ 180, 0,   0   },	// rot
			{ 180, 0,   180 },	// purpur
			{ 0,   180, 0   },	// gruen
			{ 0,   180, 180 },	// tuerkis
			{ 180, 180, 0   },	// gelb
			{ 180, 180, 180 } };	// weiss

  /*
   * Beim KC85/2..4 weicht der Zeichensatz vom ASCII-Standard etwas ab.
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
	123,   7,  56,  77,   9,  -1,  -1,  40,
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

  private static byte[] basic_c000  = null;
  private static byte[] hc900_e000  = null;
  private static byte[] hc900_f000  = null;
  private static byte[] caos22_e000 = null;
  private static byte[] caos22_f000 = null;
  private static byte[] caos31_e000 = null;
  private static byte[] caos31_f000 = null;
  private static byte[] caos42_c000 = null;
  private static byte[] caos42_e000 = null;
  private static byte[] caos42_f000 = null;
  private static byte[] caos44_c000 = null;
  private static byte[] caos44_e000 = null;
  private static byte[] caos44_f000 = null;

  private volatile boolean       blinkEnabled;
  private volatile boolean       blinkState;
  private volatile boolean       hiColorRes;
  private boolean                pasteFast;
  private boolean                basicC000Enabled;
  private boolean                caosC000Enabled;
  private boolean                caosE000Enabled;
  private boolean                irmEnabled;
  private boolean                ram0Enabled;
  private boolean                ram0Writeable;
  private boolean                ram4Enabled;
  private boolean                ram4Writeable;
  private boolean                ram8Enabled;
  private boolean                ram8Writeable;
  private boolean                ramColorEnabled;
  private boolean                screen1Enabled;
  private volatile boolean       screen1Visible;
  private boolean                audioOutPhaseL;
  private boolean                audioOutPhaseR;
  private boolean                audioInPhase;
  private int                    audioInTStates;
  private int                    ram8SegNum;
  private volatile int           keyNumPressed;
  private int                    keyNumProcessing;
  private int                    keyShiftBitCnt;
  private int                    keyShiftValue;
  private int                    keyTStates;
  private volatile int           tStatesLinePos0;
  private volatile int           tStatesLinePos1;
  private volatile int           tStatesLinePos2;
  private volatile int           tStatesPerLine;
  private int                    lineTStateCounter;
  private int                    lineCounter;
  private int                    kcTypeNum;
  private byte[]                 basicC000;
  private byte[]                 caosC000;
  private byte[]                 caosE000;
  private byte[]                 caosF000;
  private byte[]                 ram8;
  private byte[]                 ramColor0;
  private byte[]                 ramColor1;
  private byte[]                 ramPixel0;
  private byte[]                 ramPixel1;
  private int[]                  rgbValues;
  private Color[]                colors;
  private AbstractKC85Module[]   modules;
  private volatile BufferedImage screenImage;
  private volatile BufferedImage screenImage2;
  private String                 sysName;
  private Z80PIO                 pio;
  private Z80CTC                 ctc;
  private D004                   d004;
  private String                 d004RomProp;


  public KC85( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( this.sysName.startsWith( "HC900" ) ) {
      if( hc900_e000 == null ) {
	hc900_e000 = readResource( "/rom/kc85/hc900_e000.bin" );
      }
      if( hc900_f000 == null ) {
	hc900_f000 = readResource( "/rom/kc85/hc900_f000.bin" );
      }
      this.kcTypeNum = 2;
      this.basicC000 = null;
      this.caosC000  = null;
      this.caosE000  = hc900_e000;
      this.caosF000  = hc900_f000;
      this.sysName   = "HC900";
    } else if( this.sysName.startsWith( "KC85/2" ) ) {
      if( caos22_e000 == null ) {
	caos22_e000 = readResource( "/rom/kc85/caos22_e000.bin" );
      }
      if( caos22_f000 == null ) {
	caos22_f000 = readResource( "/rom/kc85/caos22_f000.bin" );
      }
      this.kcTypeNum = 2;
      this.basicC000 = null;
      this.caosC000  = null;
      this.caosE000  = caos22_e000;
      this.caosF000  = caos22_f000;
      this.sysName   = "KC85/2";
    } else {
      if( basic_c000 == null ) {
	basic_c000 = readResource( "/rom/kc85/basic_c000.bin" );
      }
      this.basicC000 = basic_c000;
      if( this.sysName.startsWith( "KC85/3" ) ) {
	if( caos31_e000 == null ) {
	  caos31_e000 = readResource( "/rom/kc85/caos31_e000.bin" );
	}
	if( caos31_f000 == null ) {
	  caos31_f000 = readResource( "/rom/kc85/caos31_f000.bin" );
	}
	this.kcTypeNum = 3;
	this.caosC000  = null;
	this.caosE000  = caos31_e000;
	this.caosF000  = caos31_f000;
	this.sysName   = "KC85/3";
      } else if( this.sysName.startsWith( "KC85/5" ) ) {
	if( caos44_c000 == null ) {
	  caos44_c000 = readResource( "/rom/kc85/caos44_c000.bin" );
	}
	if( caos44_e000 == null ) {
	  caos44_e000 = readResource( "/rom/kc85/caos44_e000.bin" );
	}
	if( caos44_f000 == null ) {
	  caos44_f000 = readResource( "/rom/kc85/caos44_f000.bin" );
	}
	this.kcTypeNum = 5;
	this.caosC000  = caos44_c000;
	this.caosE000  = caos44_e000;
	this.caosF000  = caos44_f000;
	this.sysName   = "KC85/5";
      } else {
	if( caos42_c000 == null ) {
	  caos42_c000 = readResource( "/rom/kc85/caos42_c000.bin" );
	}
	if( caos42_e000 == null ) {
	  caos42_e000 = readResource( "/rom/kc85/caos42_e000.bin" );
	}
	if( caos42_f000 == null ) {
	  caos42_f000 = readResource( "/rom/kc85/caos42_f000.bin" );
	}
	this.kcTypeNum = 4;
	this.caosC000  = caos42_c000;
	this.caosE000  = caos42_e000;
	this.caosF000  = caos42_f000;
	this.sysName   = "KC85/4";
      }
    }
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

    this.screenImage  = null;
    this.screenImage2 = null;
    this.rgbValues    = new int[ basicRGBValues.length ];
    this.colors       = new Color[ basicRGBValues.length ];
    createColors( props );
    applyPasteFast( props );

    Z80CPU cpu       = emuThread.getZ80CPU();
    this.ctc         = new Z80CTC( cpu );
    this.pio         = new Z80PIO( cpu );
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
	} else if( this.d004RomProp.equals( "3.2" ) ) {
	  resource = "/rom/kc85/d004_32.bin";
	}
	if( resource == null ) {
	  if( this.kcTypeNum > 4 ) {
	    resource = "/rom/kc85/d004_32.bin";
	  } else {
	    resource = "/rom/kc85/d004_20.bin";
	  }
	}
	romBytes = EmuUtil.readResource(
				this.emuThread.getScreenFrm(),
				resource );
      }
      this.d004 = new D004( this.emuThread.getScreenFrm(), romBytes );
    }
    this.modules = createModules( props );
    if( this.modules != null ) {
      try {
	java.util.List<Z80InterruptSource> iSources
				= new ArrayList<Z80InterruptSource>();
	iSources.add( this.ctc );
	iSources.add( this.pio );
	for( int i = 0; i < this.modules.length; i++ ) {
	  if( this.modules[ i ] instanceof Z80InterruptSource ) {
	    iSources.add( (Z80InterruptSource) this.modules[ i ] );
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

    reset( EmuThread.ResetLevel.POWER_ON, props );
    z80MaxSpeedChanged( cpu );
  }


  public static int getDefaultSpeedKHz()
  {
    return 1750;
  }


	/* --- Z80CTCListener --- */

  /*
   * CTC-Ausgaenge:
   *   Kanal 0: Tonausgabe rechts
   *   Kanal 1: Tonausgabe links
   *   Kanal 2: Blinken
   */
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
	  this.blinkState = !this.blinkState;
	  if( this.screenImage == null ) {
	    this.screenFrm.setScreenDirty( true );
	  }
	  break;
      }
    }
  }


	/* --- Z80MaxSpeedListener --- */

  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    int t = cpu.getMaxSpeedKHz() * 112 / 1750;
    this.tStatesLinePos0 = (int) Math.round( t * 32.0 / 112.0 );
    this.tStatesLinePos1 = (int) Math.round( t * 64.0 / 112.0 );
    this.tStatesLinePos2 = (int) Math.round( t * 96.0 / 112.0 );
    this.tStatesPerLine  = t;
  }


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.systemUpdate( tStates );

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
	  updScreenLine();
	  this.lineCounter++;
	} else {
	  this.lineCounter = 0;
	  if( this.screenImage != null ) {
	    this.screenFrm.setScreenDirty( true );
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
      this.ctc.externalUpdate( 0, h4 );
      this.ctc.externalUpdate( 1, h4 );
      this.ctc.externalUpdate( 2, bi );
      this.ctc.externalUpdate( 3, bi );
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

	/*
	 * Bei jedem Phasenwechsel wird ein Impuls an ASTB erzeugt,
	 * was je nach Betriebsart der PIO eine Ein- oder Ausgabe bedeutet
	 * und, das ist das eigentliche Ziel, einen Interrupt ausloest.
	 */
	switch( this.pio.getModePortA() ) {
	  case Z80PIO.MODE_BYTE_IN:
	    this.pio.putInValuePortA( 0xFF, true );
	    break;

	  case Z80PIO.MODE_BYTE_INOUT:
	  case Z80PIO.MODE_BYTE_OUT:
	    this.pio.fetchOutValuePortA( true );
	    break;
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
	int keyNum = this.keyNumPressed;
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
	if( this.pio.getModePortA() == Z80PIO.MODE_BYTE_INOUT ) {
	  // BSTB wird fuer die Eingabe bei Port A verwendet
	  this.pio.putInValuePortA( 0xFF, true );
	} else {
	  switch( this.pio.getModePortB() ) {
	    case Z80PIO.MODE_BYTE_IN:
	      this.pio.putInValuePortB( 0xFF, true );
	      break;

	    case Z80PIO.MODE_BYTE_OUT:
	      this.pio.fetchOutValuePortB( true );
	      break;
	  }
	}
	if( this.keyShiftBitCnt == 1 ) {
	  // letztes Bit verarbeitet
	  int keyNum = this.keyNumPressed;
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

  public void applySettings( Properties props )
  {
    super.applySettings( props );
    if( EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kc85.emulate_video_timing",
				true ) )
    {
      if( this.screenImage2 == null ) {
	this.screenImage2 = new BufferedImage(
					SCREEN_WIDTH,
					SCREEN_HEIGHT,
					BufferedImage.TYPE_INT_RGB );
      }
      this.screenImage = this.screenImage2;
    } else {
      this.screenImage = null;
    }
    createColors( props );
    applyPasteFast( props );
    if( this.d004 != null ) {
      this.d004.applySettings( props );
    }
  }


  public boolean canExtractScreenText()
  {
    return true;
  }


  public void die()
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    this.ctc.removeCTCListener( this );
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.d004 != null ) {
      this.d004.fireStop( false );
      this.d004.die();
    }
    if( this.modules != null ) {
      for( int i = 0; i < this.modules.length; i++ ) {
	this.modules[ i ].die();
      }
    }
  }


  public static FloppyDiskInfo[] getAvailableFloppyDisks()
  {
    return availableFloppyDisks;
  }


  public int getBorderColorIndex()
  {
    return 0;	// schwarz
  }


  public Color getColor( int colorIdx )
  {
    Color color = null;
    if( this.colors != null ) {
      if( (colorIdx >= 0) && (colorIdx < this.colors.length) )
	color = this.colors[ colorIdx ];
    }
    return color != null ? color : super.getColor( colorIdx );
  }


  public int getColorCount()
  {
    return rgbValues.length;
  }


  public int getColorIndex( int x, int y )
  {
    int rv  = 0;
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
    return rv;
  }


  public int getCharColCount()
  {
    return 40;
  }


  public int getCharHeight()
  {
    return 8;
  }


  public int getCharRowCount()
  {
    return 32;
  }


  public int getCharRowHeight()
  {
    return 8;
  }


  public int getCharWidth()
  {
    return 8;
  }


  public boolean getDefaultFloppyDiskBlockNum16Bit()
  {
    return true;
  }


  public int getDefaultFloppyDiskBlockSize()
  {
    return this.d004 != null ? 2048 : -1;
  }


  public int getDefaultFloppyDiskDirBlocks()
  {
    return this.d004 != null ? 2 : -1;
  }


  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return this.d004 != null ?
		FloppyDiskFormat.getFormat( 2, 80, 5, 1024 )
		: null;
  }


  public int getDefaultFloppyDiskSystemTracks()
  {
    return this.d004 != null ? 2 : -1;
  }


  public long getDelayMillisAfterPasteChar()
  {
    return this.pasteFast ? 0 : super.getDelayMillisAfterPasteChar();
  }


  public long getDelayMillisAfterPasteEnter()
  {
    return this.pasteFast ? 0 : super.getDelayMillisAfterPasteEnter();
  }


  public long getHoldMillisPasteChar()
  {
    return this.pasteFast ? 0 : super.getHoldMillisPasteChar();
  }


  public String getHelpPage()
  {
    return "/help/kc85.htm";
  }


  public int getMemByte( int addr, boolean m1 )
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
      if( this.irmEnabled ) {
	byte[] a = null;
	if( addr < 0xA800 ) {
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
	if( idx < this.basicC000.length ) {
	  rv = (int) basicC000[ idx ] & 0xFF;
	}
      }
    } else if( addr >= 0xE000 ) {
      if( this.caosE000Enabled ) {
	if( (addr < 0xF000) && (this.caosE000 != null) ) {
	  int idx = addr - 0xE000;
	  if( idx < caosE000.length ) {
	    rv = (int) caosE000[ idx ] & 0xFF;
	  }
	}
	else if( this.caosF000 != null ) {
	  int idx = addr - 0xF000;
	  if( idx < caosF000.length ) {
	    rv = (int) caosF000[ idx ] & 0xFF;
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


  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return resetLevel == EmuThread.ResetLevel.WARM_RESET ? 0xE000 : 0xF000;
  }


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


  public int getScreenHeight()
  {
    return SCREEN_HEIGHT;
  }


  public int getScreenWidth()
  {
    return SCREEN_WIDTH;
  }


  public String getSecondarySystemName()
  {
    return this.d004 != null ? "D004" : null;
  }


  public Z80CPU getSecondaryZ80CPU()
  {
    return this.d004 != null ? this.d004.getZ80CPU() : null;
  }


  public Z80Memory getSecondaryZ80Memory()
  {
    return this.d004 != null ? this.d004.getZ80Memory() : null;
  }


  public FloppyDiskInfo[] getSuitableFloppyDisks()
  {
    return this.d004 != null ? availableFloppyDisks : null;
  }


  public int getSupportedFloppyDiskDriveCount()
  {
    return this.d004 != null ?
		this.d004.getSupportedFloppyDiskDriveCount()
		: 0;
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  public String getTitle()
  {
    return this.sysName;
  }


  public boolean hasKCBasicInROM()
  {
    return (this.kcTypeNum > 2);
  }


  protected boolean isExtROMSwitchableAt( int addr )
  {
    boolean rv = true;
    if( (addr >= 0x8000) && (addr < 0xC000) ) {
      if( this.irmEnabled ) {
	rv = false;
      }
    }
    else if( (addr >= 0xC000) && (addr < 0xE000) ) {
      if( (this.kcTypeNum > 3) && !this.caosC000Enabled ) {
	rv = false;
      }
    }
    return rv;
  }


  public boolean isSecondarySystemRunning()
  {
    return this.d004 != null ? this.d004.isRunning() : false;
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    boolean rv = false;
    char    ch = 0;
    switch( keyCode ) {
      case KeyEvent.VK_F9:		// CLR
	ch = 1;
	break;

      case KeyEvent.VK_F7:		// BRK
	ch = 3;
	break;

      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
	ch = 7;
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

      case KeyEvent.VK_HOME:
	ch = 0x10;
	break;

      case KeyEvent.VK_F8:		// STOP
	ch = 0x13;
	break;

      case KeyEvent.VK_END:
	ch = 0x18;
	break;

      case KeyEvent.VK_INSERT:
	ch = 0x1A;
	break;

      case KeyEvent.VK_ESCAPE:
	ch = 0x1B;
	break;

      case KeyEvent.VK_DELETE:
	ch = 0x1F;
	break;

      case KeyEvent.VK_SPACE:
	ch = 0x20;
	break;

      case KeyEvent.VK_F1:
	ch = (char) (shiftDown ? 0xF7 : 0xF1);
	break;

      case KeyEvent.VK_F2:
	ch = (char) (shiftDown ? 0xF8 : 0xF2);
	break;

      case KeyEvent.VK_F3:
	ch = (char) (shiftDown ? 0xF9 : 0xF3);
	break;

      case KeyEvent.VK_F4:
	ch = (char) (shiftDown ? 0xFA : 0xF4);
	break;

      case KeyEvent.VK_F5:
	ch = (char) (shiftDown ? 0xFB : 0xF5);
	break;

      case KeyEvent.VK_F6:
	ch = (char) (shiftDown ? 0xFC : 0xF6);
	break;
    }
    if( ch > 0 ) {
      keyTyped( ch );
      rv = true;
    }
    return rv;
  }


  public void keyReleased()
  {
    this.keyNumPressed = -1;
  }


  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < char2KeyNum.length) ) {
      int keyNum = char2KeyNum[ ch ];
      if( keyNum >= 0 ) {
	this.keyNumPressed = (keyNum & 0xFE) | (~keyNum & 0x01);
	rv                 = true;
      }
    }
    return rv;
  }


  public void openBasicProgram()
  {
    SourceUtil.openKCBasicStyleProgram(
				this.screenFrm,
				getKCBasicBegAddr(),
				basicTokens );
  }


  public boolean paintScreen(
			Graphics g,
			int      xOffs,
			int      yOffs,
			int      screenScale )
  {
    boolean rv  = false;
    Image   img = this.screenImage;
    if( img != null ) {
      if( screenScale > 1 ) {
	g.drawImage(
		img,
		xOffs,
		yOffs,
		SCREEN_WIDTH * screenScale,
		SCREEN_HEIGHT * screenScale,
		this );
      } else {
	g.drawImage( img, xOffs, yOffs, this );
      }
      rv = true;
    }
    return rv;
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
	  Z80CPU cpu = emuThread.getZ80CPU();
	  int    ix  = cpu.getRegIX();
	  while( (getMemByte( ix + 8, false ) & 0x01) != 0 ) {
	    Thread.sleep( 10 );
	  }
	  setMemByte( ix + 13, ch );
	  setMemByte( ix + 8, getMemByte( ix + 8, false ) | 0x01 );
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


  public int reassembleSysCall(
			int           addr,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    int rv = 0;
    String s = null;
    int    b = this.emuThread.getMemByte( addr, true );
    switch( b ) {
      case 0xC3:
	s = "JP";
	break;
      case 0xCD:
	s = "CALL";
	break;
    }
    if( s != null ) {
      if( getMemWord( addr + 1 ) == 0xF003 ) {
	int idx = this.emuThread.getMemByte( addr + 3, false );
	int bol = buf.length();
	buf.append( String.format( "%04X  %02X 03 F0", addr, b ) );
	appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( s );
	appendSpacesToCol( buf, bol, colArgs );
	buf.append( "0F003H\n" );
	bol = buf.length();
	buf.append( String.format( "%04X  %02X", (addr + 3) & 0xFFFF, idx ) );
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
    return rv;
  }


  /*
   * Ein RESET ist erforderlich,
   * wenn sich das emulierte System oder die Modulliste aendert
   */
  public boolean requiresReset( Properties props )
  {
    boolean rv = true;
    if( this.sysName.equals(
		EmuUtil.getProperty( props, "jkcemu.system" ) ) )
    {
      boolean d004 = emulatesD004( props );
      if( d004 ) {
	if( this.d004RomProp.equals( getD004RomProp( props ) ) ) {
	  rv = false;
	}
      } else {
	rv = false;
      }
      if( !rv ) {
	java.util.List<String> modNames = new ArrayList<String>();
	if( props != null ) {
	  boolean loop    = true;
	  int     slot    = 8;
	  int     nRemain = EmuUtil.getIntProperty(
				props,
				"jkcemu.kc85.module.count",
				0 );
	  while( loop && (slot < 0x100) && (nRemain > 0) ) {
	    loop        = false;
	    String text = props.getProperty(
			  String.format( "jkcemu.kc85.module.%02X", slot ) );
	    if( text != null ) {
	      if( text.length() > 0 ) {
		modNames.add( text );
		loop = true;
	      }
	    }
	    if( loop ) {
	      slot += 4;
	      --nRemain;
	    }
	  }
	  if( d004 ) {
	    modNames.add( "D004" );
	  }
	}
	int n = modNames.size();
	AbstractKC85Module[] modules = this.modules;
	if( modules != null ) {
	  if( modules.length == n ) {
	    for( int i = 0; i < n; i++ ) {
	      if( !modules[ i ].getModuleName().equals( modNames.get( i ) ) ) {
		rv = true;
		break;
	      }
	    }
	  } else {
	    rv = true;
	  }
	} else {
	  if( n != 0 ) {
	    rv = true;
	  }
	}
      }
    }
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( this.ram8 != null ) {
	Arrays.fill( this.ram8, (byte) 0 );
      }
      Arrays.fill( this.ramColor0, (byte) 0 );
      Arrays.fill( this.ramColor1, (byte) 0 );
      Arrays.fill( this.ramPixel0, (byte) 0 );
      Arrays.fill( this.ramPixel1, (byte) 0 );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
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
      this.ctc.reset( true );
      this.pio.reset( true );
    } else {
      this.ctc.reset( false );
      this.pio.reset( false );
    }
    if( this.modules != null ) {
      for( int i = 0; i < this.modules.length; i++ ) {
	this.modules[ i ].reset( resetLevel );
      }
    }
    blinkEnabled           = false;
    blinkState             = false;
    this.audioOutPhaseL    = false;
    this.audioOutPhaseL    = false;
    this.audioInPhase      = this.emuThread.readAudioPhase();
    this.audioInTStates    = 0;
    this.lineTStateCounter = 0;
    this.lineCounter       = 0;
    this.keyNumPressed     = -1;
    this.keyNumProcessing  = -1;
    this.keyShiftBitCnt    = 0;
    this.keyShiftValue     = 0;
    this.keyTStates        = 0;
    updAudioOut();
  }


  public void saveBasicProgram()
  {
    SourceUtil.saveKCBasicStyleProgram(
				this.screenFrm,
				getKCBasicBegAddr() );
  }


  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    if( this.d004 != null )
      this.d004.setDrive( idx, drive );
  }


  public boolean setMemByte( int addr, int value )
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
      if( this.irmEnabled ) {
	byte[] a = null;
	if( addr < 0xA800 ) {
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
	    if( this.screenImage == null ) {
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
	if( (addr < 0xF000) && (this.caosE000 != null) ) {
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


  public boolean supportsAudio()
  {
    return true;
  }


  public boolean supportsCopyToClipboard()
  {
    return true;
  }


  public boolean supportsPasteFromClipboard()
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
	  this.screen1Visible  = ((value & 0x01) == 0);
	  this.ramColorEnabled = ((value & 0x02) != 0);
	  this.screen1Enabled  = ((value & 0x04) == 0);
	  this.hiColorRes      = ((value & 0x08) == 0);
	  this.ram8SegNum      = (value >> 4) & 0x0F;
	}
	break;

      case 0x86:
      case 0x87:
	if( this.kcTypeNum > 3 ) {
	  this.ram4Enabled     = ((value & 0x01) != 0);
	  this.ram4Writeable   = ((value & 0x02) != 0);
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
    this.pasteFast = EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kc85.paste.fast",
				true );
  }


  private void createColors( Properties props )
  {
    double brightness = getBrightness( props );
    if( (brightness >= 0.0) && (brightness <= 1.0) ) {
      for( int i = 0; i < basicRGBValues.length; i++ ) {
	int r = (int) Math.round( basicRGBValues[ i ][ 0 ] * brightness );
	int g = (int) Math.round( basicRGBValues[ i ][ 1 ] * brightness );
	int b = (int) Math.round( basicRGBValues[ i ][ 2 ] * brightness );

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
				"jkcemu.kc85.module.count",
				0 );
      if( nRemain > 0 ) {
	modules = new ArrayList<AbstractKC85Module>();
	int     slot = 8;
	boolean loop = true;
	while( loop && (slot < 0x100) && (nRemain > 0) ) {
	  loop        = false;
	  String text = props.getProperty(
			  String.format( "jkcemu.kc85.module.%02X", slot ) );
	  if( text != null ) {
	    if( text.equals( "M003" ) ) {
	      modules.add( new M003( slot, this.emuThread ) );
	      loop = true;
	    }
	    else if( text.equals( "M006" ) ) {
	      modules.add( new M006( slot, this.emuThread ) );
	      loop = true;
	    }
	    else if( text.equals( "M011" ) ) {
	      modules.add( new M011( slot ) );
	      loop = true;
	    }
	    else if( text.equals( "M022" ) ) {
	      modules.add( new M022( slot ) );
	      loop = true;
	    }
	    else if( text.equals( "M032" ) ) {
	      modules.add( new M032( slot ) );
	      loop = true;
	    }
	    else if( text.equals( "M034" ) ) {
	      modules.add( new M034( slot ) );
	      loop = true;
	    }
	    else if( text.equals( "M035" ) ) {
	      modules.add( new M035( slot ) );
	      loop = true;
	    }
	    else if( text.equals( "M035x4" ) ) {
	      modules.add( new M035( slot ) );
	      modules.add( new M035( slot + 1 ) );
	      modules.add( new M035( slot + 2 ) );
	      modules.add( new M035( slot + 3 ) );
	      loop = true;
	    }
	    else if( text.equals( "M036" ) ) {
	      modules.add( new M036( slot ) );
	      loop = true;
	    }
	  }
	  if( loop ) {
	    slot += 4;
	    --nRemain;
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


  private static boolean emulatesD004( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kc85.d004.enabled",
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


  private static String getD004RomProp( Properties props )
  {
    String s = EmuUtil.getProperty( props, "jkcemu.kc85.d004.rom" );
    return s.equals( "2.0" ) || s.equals( "3.2" ) || s.startsWith( "file:" ) ?
		s : "standard";
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


  private void updAudioOut()
  {
    if( this.emuThread.isLoudspeakerEmulationEnabled() ) {
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


  private void updScreenLine()
  {
    BufferedImage img = this.screenImage;
    int           y   = this.lineCounter;
    if( (img != null) && (y >= 0) && (y < 256) ) {
      int x = 0;
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
		img.setRGB( x++, y, rgbValues[ colorIdx ] );
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
		img.setRGB( x++, y, rgbValues[ colorIdx ] );
	      }
	      m >>= 1;
	    }
	  }
	}
      }
    }
  }
}
