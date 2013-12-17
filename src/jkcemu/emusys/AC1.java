/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des AC1
 */

package jkcemu.emusys;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.*;
import java.text.*;
import java.util.*;
import javax.swing.JOptionPane;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.emusys.ac1_llc2.AbstractSCCHSys;
import jkcemu.etc.VDIP;
import jkcemu.joystick.JoystickThread;
import jkcemu.net.KCNet;
import jkcemu.text.TextUtil;
import z80emu.*;


public class AC1
		extends AbstractSCCHSys
		implements
			FDC8272.DriveSelector,
			Z80PCListener,
			Z80TStatesListener
{
  /*
   * Die im Emulator integrierten SCCH-Monitore enthalten eine Routine
   * zur seriellen Ausgabe von Zeichen ueber Bit 2 des IO-Ports 8 (PIO 2).
   * In der Standardeinstellung soll diese Ausabe mit 9600 Bit/s erfolgen,
   * was bei einer Taktfrequenz von 2 MHz 208,3 Takte pro Bit entspricht.
   * Tatsaechlich werden aber in der Standardeinstellung pro Bit
   * 4 Ausgabe-Befehle auf dem Port getaetigt, die zusammen
   * 248 Takte benoetigen, was etwa 8065 Bit/s entspricht.
   * Damit im Emulator die Ausgabe auf den emulierten Drucker ueber diese
   * serielle Schnittstelle funktioniert,
   * wird somit der Drucker ebenfalls mit dieser Bitrate emuliert.
   * Ist allerdings eine externe ROM-Datei als Monitorprogramm eingebunden,
   * werden 9600 Baud emuliert.
   */
  private static int V24_TSTATES_PER_BIT_INTERN = 248;
  private static int V24_TSTATES_PER_BIT_EXTERN = 208;

  // Einprung zum Lesen eines Zeichens von der Tastatur
  private static int ADDR_INCH = 0x1802;

  private enum BasicType {
			AC1_MINI,
			AC1_8K,
			AC1_12K,
			SCCH,
			BACOBAS,
			ADDR_60F7 };

  /*
   * Diese Tabelle mappt die Tokens des AC1-8K-BASIC-Interpreters
   * in ihre entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   */
  private static final String[] ac1_8kTokens = {
	"END",      "FOR",    "NEXT",   "DATA",		// 0x80
	"INPUT",    "DIM",    "READ",   "LET",
	"GOTO",     "RUN",    "IF",     "RESTORE",
	"GOSUB",    "RETURN", "REM",    "STOP",
	"OUT",      "ON",     "NULL",   "WAIT",		// 0x90
	"DEF",      "POKE",   "DOKE",   "AUTO",
	"LINES",    "CLS",    "WIDTH",  "BYE",
	"RENUMBER", "CALL",   "PRINT",  "CONT",
	"LIST",     "CLEAR",  "CLOAD",  "CSAVE",	// 0xA0
	"NEW",      "TAB(",   "TO",     "FN",
	"SPC(",     "THEN",   "NOT",    "STEP",
	"+",        "-",      "*",      "/",
	"^",        "AND",    "OR",     ">",		// 0xB0
	"=",        "<",      "SGN",    "INT",
	"ABS",      "USR",    "FRE",    "INP",
	"POS",      "SQR",    "RND",    "LN",
	"EXP",      "COS",    "SIN",    "TAN",		// 0xC0
	"ATN",      "PEEK",   "DEEK",   "POINT",
	"LEN",      "STR$",   "VAL",    "ASC",
	"CHR$",     "LEFT$",  "RIGHT$", "MID$",
	"SET",      "RESET",  "WINDOW", "SCREEN",	// 0xD0
	"EDIT",     "ASAVE",  "ALOAD",  "TRON",
	"TROFF" };

  /*
   * Diese Tabelle mappt die Tokens des AC1-12K-BASIC-Interpreters
   * in ihre entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   */
  private static final String[] ac1_12kTokens = {
	"END",      "FOR",       "NEXT",   "DATA",	// 0x80
	"INPUT",    "DIM",       "READ",   "LET",
	"GO TO",    "RUN",       "IF",     "RESTORE",
	"GO SUB",   "RETURN",    "REM",    "STOP",
	"OUT",      "ON",        "NULL",   "WAIT",	// 0x90
	"DEF",      "POKE",      "BEEP",   "AUTO",
	"?",        "CLS",       "WIDTH",  "FNEND",
	"RENUMBER", "CALL",      "PRINT",  "CONT",
	"LIST",     "CLEAR",     "CLOAD",  "CSAVE",	// 0xA0
	"NEW",      "TAB(",      "TO",     "FN",
	"SPC(",     "THEN",      "NOT",    "STEP",
	"+",        "-",         "*",      "/",
	"^",        "AND",       "OR",     ">",		// 0xB0
	"=",        "<",         "SGN",    "INT",
	"ABS",      "USR",       "FRE",    "INP",
	"POS",      "SQR",       "RND",    "LN",
	"EXP",      "COS",       "SIN",    "TAN",	// 0xC0
	"ATN",      "PEEK",      "USING",  "POINT",
	"LEN",      "STR$",      "VAL",    "ASC",
	"CHR$",     "LEFT$",     "RIGHT$", "MID$",
	"SET",      "RESET",     "LPOS",   "INSTR",	// 0xD0
	"EDIT",     "LVAR",      "LLVAR",  "TRACE",
	"LTRACE",   "FNRETURN",  "!",      "ELSE",
	"LPRINT",   "RANDOMIZE", "LWIDTH", "LNULL",
	"\'",       "PRECISION", "KILL",   "EXCHANGE",	// 0xE0
	"LINE",     "CLOAD",     "COPY",   "LLIST",
	"DELETE",   "SWITCH",    "SCREEN" };

  /*
   * Diese Tabelle mappt die Tokens des BASOBAS-Interpreters
   * in ihre entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   */
  private static final String[] bacobasTokens = {
	"END",      "FOR",    "NEXT",   "DATA",		// 0x80
	"INPUT",    "DIM",    "READ",   "LET",
	"GOTO",     "RUN",    "IF",     "RESTORE",
	"GOSUB",    "RETURN", "REM",    "STOP",
	"OUT",      "ON",     "NULL",   "WAIT",		// 0x90
	"DEF",      "POKE",   "DOKE",   "AUTO",
	"LINES",    "CLS",    "WIDTH",  "BYE",
	"RENUMBER", "CALL",   "PRINT",  "CONT",
	"LIST",     "CLEAR",  "CLOAD",  "CSAVE",	// 0xA0
	"NEW",      "TAB(",   "TO",     "FN",
	"SPC(",     "THEN",   "NOT",    "STEP",
	"+",        "-",      "*",      "/",
	"^",        "AND",    "OR",     ">",		// 0xB0
	"=",        "<",      "SGN",    "INT",
	"ABS",      "USR",    "FRE",    "INP",
	"POS",      "SQR",    "RND",    "LN",
	"EXP",      "COS",    "SIN",    "TAN",		// 0xC0
	"ATN",      "PEEK",   "DEEK",   "POINT",
	"LEN",      "STR$",   "VAL",    "ASC",
	"CHR$",     "LEFT$",  "RIGHT$", "MID$",
	"SET",      "RESET",  "WINDOW", "SCREEN",	// 0xD0
	"EDIT",     "DSAVE",  "DLOAD",  "TRON",
	"TROFF",    "POS",    "BEEP",   "BRON",
	"BROFF",    "LPRINT", "LLIST",  "BACO",
	"CONV",     "TRANS",  "BLIST",  "BEDIT",
	"BSAVE",    "BLOAD",  "DELETE", "DIR" };

  private static final int[] ccdCharToUnicode = {
		'\u0020', '\u2598', '\u259D', '\u2580',		// 00h
		'\u2596', '\u258C', '\u259E', '\u259B',
		'\u2597', '\u259A', '\u2590', '\u259C',
		'\u2584', '\u2599', '\u259F', '\u2588',
		      -1,      -1,       -1,        -1,		// 10h
		'\u00B2', '\u00B3', '\u00A7', '\u00C4',
		'\u00D6', '\u00DC', '\u00E4', '\u00F6',
		'\u00FC', '\u00DF', '\u00B5', '\u03A9',
		'\u0020',      '!',     '\"',      '#',		// 20h
		     '$',      '%',      '&',     '\'',
		     '(',      ')',      '*',      '+',
		     ',',      '-',      '.',      '/',
		     '0',      '1',      '2',      '3',		// 30h
		     '4',      '5',      '6',      '7',
		     '8',      '9',      ':',      ';',
		     '<',      '=',      '>',      '?',
		     '@',      'A',      'B',      'C',		// 40h
		     'D',      'E',      'F',      'G',
		     'H',      'I',      'J',      'K',
		     'L',      'M',      'N',      'O',
		     'P',      'Q',      'R',      'S',		// 50h
		     'T',      'U',      'V',      'W',
		     'X',      'Y',      'Z',      '[',
		    '\\',      ']',      '^',      '_',
		     '`',      'a',      'b',      'c',		// 60h
		     'd',      'e',      'f',      'g',
		     'h',      'i',      'j',      'k',
		     'l',      'm',      'n',      'o',
		     'p',      'q',      'r',      's',		// 70h
		     't',      'u',      'v',      'w',
		     'x',      'y',      'z',      '{',
		     '|',      '}',      '~', '\u2592',
		'\u0020', '\u2598', '\u259D', '\u2580',		// 80h
		'\u2596', '\u258C', '\u259E', '\u259B',
		'\u2597', '\u259A', '\u2590', '\u259C',
		'\u2584', '\u2599', '\u259F', '\u2588',
		'\u2571', '\u2572',       -1,       -1,		// 90h
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
 		      -1,       -1, '\u2573', '\u2594',
		'\u0020', '\u2503', '\u253B', '\u2523',		// A0h
		'\u2533', '\u252B', '\u254B', '\u2517',
		'\u250F', '\u2513', '\u251B',       -1,
		     -1,        -1,       -1, '\u2501',
		'\u2191', '\u2193', '\u2190', '\u2192',		// B0h
		'\u25E2', '\u25E4', '\u25E3', '\u25E5',
		'\u25A1',       -1, '\u25CF',       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,		// C0h
		      -1,       -1,       -1,       -1,
		      -1, '\u2666', '\u2663', '\u2665',
		'\u2660',       -1,       -1,       -1,
		'\u2500', '\u2502', '\u2534', '\u251C',		// D0h
		'\u252C', '\u2524', '\u253C', '\u2514',
		'\u250C', '\u2510', '\u2518', '\u2592',
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,		// E0h
		      -1,       -1, '\u2689',       -1,
		      -1,       -1,       -1,       -1,
		'\u25CB',       -1,       -1, '\u0020',
		      -1,       -1, '\u258F', '\u258E',		// F0h
		'\u258D', '\u258C', '\u258B', '\u258A',
		'\u2588', '\u2587', '\u2586', '\u2585',
		'\u2584', '\u2583', '\u2582', '\u2581' };

  private static final int[] ac1_2010CharToUnicode = {
		'\u0020', '\u2598', '\u259D', '\u2580',		// 00h
		'\u2596', '\u258C', '\u259E', '\u259B',
		'\u2597', '\u259A', '\u2590', '\u259C',
		'\u2584', '\u2599', '\u259F', '\u2588',
		      -1,       -1,       -1,       -1,		// 10h
		'\u00B2', '\u00B3', '\u2689',       -1,
		      -1,       -1,      '[',      ']',
		     '{',      '}', '\u00B5', '\u03A9',
		'\u0020',      '!',     '\"',      '#',		// 20h
		     '$',      '%',      '&',     '\'',
		     '(',      ')',      '*',      '+',
		     ',',      '-',      '.',      '/',
		     '0',      '1',      '2',      '3',		// 30h
		     '4',      '5',      '6',      '7',
		     '8',      '9',      ':',      ';',
		     '<',      '=',      '>',      '?',
		'\u00A7',      'A',      'B',      'C',		// 40h
		     'D',      'E',      'F',      'G',
		     'H',      'I',      'J',      'K',
		     'L',      'M',      'N',      'O',
		     'P',      'Q',      'R',      'S',		// 50h
		     'T',      'U',      'V',      'W',
		     'X',      'Y',      'Z', '\u00C4',
		'\u00D6', '\u00DC',      '^',      '_',
		     '@',      'a',      'b',      'c',		// 60h
		     'd',      'e',      'f',      'g',
		     'h',      'i',      'j',      'k',
		     'l',      'm',      'n',      'o',
		     'p',      'q',      'r',      's',		// 70h
		     't',      'u',      'v',      'w',
		     'x',      'y',      'z', '\u00E4',
		'\u00F6', '\u00FC', '\u00DF',       -1,
		      -1,       -1,       -1,       -1,		// 80h
		      -1,       -1,       -1,       -1,
		      -1,       -1, '\u25CB', '\u25D8',
		'\u25EF',       -1, '\u25E4', '\u25E3',
		'\u2571', '\u2572',       -1,       -1,		// 90h
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		'\u2501', '\u2503', '\u253B', '\u2523',		// A0h
		'\u2533', '\u252B', '\u254B', '\u2517',
		'\u250F', '\u2513', '\u251B',       -1,
		      -1,       -1,       -1, '\u2573',
		'\u2598', '\u259D', '\u2597', '\u2596',		// B0h
		'\u258C', '\u2590', '\u2580', '\u2584',
		'\u259A', '\u259E', '\u259F', '\u2599',
		'\u259B', '\u259C', '\u25E2', '\u25E5',
		      -1,       -1,       -1,       -1,		// C0h
		'\u265F',       -1,       -1, '\u2592',
		      -1, '\u2666', '\u2663', '\u2665',
		'\u2660',       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,		// D0h
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,		// E0h
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,
		      -1,       -1,       -1,       -1,		// F0h
		      -1,       -1,       -1,       -1,
		'\u2581', '\u2582', '\u2583', '\u2584',
		'\u2585', '\u2586', '\u2587', '\u2588' };

  private static final int[] romBank2010SegLengths = {
				0x2000,		// F0
				0x2000,		// F1
				0x2000,		// F2
				0x2000,		// F3
				0x1000,		// F4
				0x1000,		// F5
				0x0800,		// F6
				0x0800,		// F7
				0x2000,		// F8
				0x2000,		// F9
				0x2000,		// FA
				0x2000,		// FB
				0x1000,		// FC
				0x1000,		// FD
				0x0800,		// FE
				0x0800 };	// FF

  private static byte[] mon31_64x16 = null;
  private static byte[] mon31_64x32 = null;
  private static byte[] monSCCH80   = null;
  private static byte[] monSCCH1088 = null;
  private static byte[] mon2010c    = null;
  private static byte[] minibasic   = null;
  private static byte[] gsbasic     = null;
  private static byte[] pio2Rom2010 = null;
  private static byte[] font2010    = null;
  private static byte[] fontACC     = null;
  private static byte[] fontSCCH    = null;
  private static byte[] fontU402    = null;

  private Color[]           colors;
  private byte[]            ramColor;
  private byte[]            ramVideo;
  private byte[]            ramStatic;
  private byte[]            ramModule3;
  private byte[]            fontBytes;
  private byte[]            osBytes;
  private byte[]            pio2Rom2010Bytes;
  private byte[]            romBank2010Bytes;
  private byte[]            scchBasicBytes;
  private byte[]            scchPrgXBytes;
  private byte[]            scchRomdiskBytes;
  private String            fontFile;
  private String            osFile;
  private String            osVersion;
  private String            pio2Rom2010File;
  private String            romBank2010File;
  private String            scchBasicFile;
  private String            scchPrgXFile;
  private String            scchRomdiskFile;
  private BasicType         lastBasicType;
  private RAMFloppy         ramFloppy;
  private Z80CTC            ctc;
  private Z80PIO            pio2;
  private GIDE              gide;
  private FDC8272           fdc;
  private FloppyDiskDrive[] fdDrives;
  private KCNet             kcNet;
  private VDIP              vdip;
  private boolean           fdcWaitEnabled;
  private boolean           tcEnabled;
  private boolean           mode64x16;
  private boolean           mode2010;
  private boolean           modeSCCH;
  private boolean           fontSwitchable;
  private boolean           inverseBySW;
  private boolean           inverseByKey;
  private boolean           extFont;
  private boolean           pio1B3State;
  private boolean           keyboardUsed;
  private volatile boolean  graphicKeyState;
  private volatile boolean  pasteFast;
  private boolean           lowerDRAMEnabled;
  private boolean           osRomEnabled;
  private boolean           scchRomdiskEnabled;
  private boolean           scchPrgXEnabled;
  private boolean           scchBasicEnabled;
  private boolean           rf32KActive;
  private boolean           rf32NegA15;
  private boolean           rfReadEnabled;
  private boolean           rfWriteEnabled;
  private int               rfAddr16to19;
  private int               scchRomdiskBegAddr;
  private int               scchRomdiskBankAddr;
  private int               regF0;
  private int               pio2Rom2010Offs;
  private int               romBank2010Offs;
  private int               romBank2010Len;
  private int               fontOffs;
  private boolean           v24BitOut;
  private int               v24BitNum;
  private int               v24ShiftBuf;
  private int               v24TStateCounter;
  private int               v24TStatesPerBit;


  public AC1( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.pasteFast      = false;
    this.fontSwitchable = false;
    this.mode64x16      = false;
    this.mode2010       = false;
    this.modeSCCH       = false;
    this.osVersion = EmuUtil.getProperty( props, "jkcemu.ac1.os.version" );
    if( this.osVersion.equals( "3.1_64x16" ) ) {
      this.mode64x16 = true;
    }
    else if( this.osVersion.equals( "SCCH8.0" ) ) {
      this.modeSCCH = true;
    }
    else if( this.osVersion.equals( "SCCH10/88" ) ) {
      this.modeSCCH       = true;
      this.fontSwitchable = true;
    }
    else if( this.osVersion.equals( "2010" ) ) {
      this.mode2010 = true;
    }
    this.osBytes             = null;
    this.osFile              = null;
    this.pio2Rom2010Bytes    = null;
    this.pio2Rom2010File     = null;
    this.romBank2010Bytes    = null;
    this.romBank2010File     = null;
    this.scchBasicBytes      = null;
    this.scchBasicFile       = null;
    this.scchPrgXBytes       = null;
    this.scchPrgXFile        = null;
    this.scchRomdiskBytes    = null;
    this.scchRomdiskFile     = null;
    this.scchRomdiskBegAddr  = getScchRomdiskBegAddr( props );
    this.scchRomdiskBankAddr = 0;
    this.pio2Rom2010Offs     = -1;
    this.romBank2010Offs     = -1;
    this.romBank2010Len      = 0;
    this.regF0               = 0;
    this.lastBasicType       = null;

    if( emulatesColors( props ) ) {
      this.fontSwitchable = true;
      this.mode64x16      = false;
      this.ramColor       = new byte[ 0x0800 ];
      this.colors         = new Color[ 16 ];
      createColors( props );
    } else {
      this.ramColor = null;
      this.colors   = null;
    }
    if( this.mode64x16 ) {
      this.ramStatic = new byte[ 0x0400 ];
      this.ramVideo  = new byte[ 0x0400 ];
    } else {
      this.ramStatic = new byte[ 0x0800 ];
      this.ramVideo  = new byte[ 0x0800 ];
    }

    this.ramModule3 = null;
    if( this.modeSCCH ) {
      // 1 MByte
      this.ramModule3 = this.emuThread.getExtendedRAM( 0x100000 );
    }

    this.ramFloppy = RAMFloppy.prepare(
				this.emuThread.getRAMFloppy1(),
				"AC1",
				RAMFloppy.RFType.MP_3_1988,
				"RAM-Floppy IO-Adressen E0h-E7h",
				props,
				"jkcemu.ac1.ramfloppy." );

    this.fdDrives  = null;
    this.fdc       = null;
    if( emulatesFloppyDisk( props ) ) {
      this.fdDrives = new FloppyDiskDrive[ 4 ];
      Arrays.fill( this.fdDrives, null );
      this.fdc = new FDC8272( this, 4 );
    }

    this.kcNet = null;
    if( emulatesKCNet( props ) ) {
      this.kcNet = new KCNet( "Netzwerk-PIO (IO-Adressen C0-C3)" );
    }

    this.vdip = null;
    if( emulatesUSB( props ) ) {
      this.vdip = new VDIP( "USB-PIO (IO-Adressen FC-FF)" );
    }

    this.joystickEnabled = emulatesJoystick( props );

    this.gide = GIDE.getGIDE( this.screenFrm, props, "jkcemu.ac1." );

    java.util.List<Z80InterruptSource> iSources
				= new ArrayList<Z80InterruptSource>();

    this.ctc   = new Z80CTC( "CTC (IO-Adressen 00-03)" );
    this.pio1  = new Z80PIO( "PIO (IO-Adressen 04-07)" );
    iSources.add( this.ctc );
    iSources.add( this.pio1 );
    if( this.modeSCCH || this.mode2010 ) {
      this.pio2 = new Z80PIO( "V24-PIO (IO-Adressen 08-0B)" );
      iSources.add( this.pio2 );
    }
    if( this.kcNet != null ) {
      iSources.add( this.kcNet );
    }
    if( this.vdip != null ) {
      iSources.add( this.vdip );
    }
    Z80CPU cpu = emuThread.getZ80CPU();
    try {
      cpu.setInterruptSources(
	iSources.toArray( new Z80InterruptSource[ iSources.size() ] ) );
    }
    catch( ArrayStoreException ex ) {}

    this.ctc.setTimerConnection( 0, 1 );
    this.ctc.setTimerConnection( 1, 2 );
    this.ctc.setTimerConnection( 2, 3 );
    cpu.addTStatesListener( this );
    if( this.fdc != null ) {
      this.fdc.setTStatesPerMilli( cpu.getMaxSpeedKHz() );
      cpu.addMaxSpeedListener( this.fdc );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80MaxSpeedChanged( cpu );
      cpu.addMaxSpeedListener( this.kcNet );
    }
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
    checkAddPCListener( props );
  }


  public static String getBasicProgram(
			Window   owner,
			LoadData loadData ) throws UserCancelException
  {
    String rv   = null;
    int    addr = loadData.getBegAddr();
    if( addr == 0x60F7 ) {
      String ac1_8k = SourceUtil.getKCBasicStyleProgram(
						loadData,
						addr,
						ac1_8kTokens );

      String scch = SourceUtil.getKCBasicStyleProgram(
						loadData,
						addr,
						scchTokens );

      String bacobas = SourceUtil.getKCBasicStyleProgram(
						loadData,
						addr,
						bacobasTokens );

      if( (ac1_8k != null) && (scch != null) && (bacobas != null) ) {
	if( ac1_8k.equals( scch ) && scch.equals( bacobas ) ) {
	  rv = ac1_8k;
	} else {
	  java.util.List<String> options = new ArrayList<String>( 4 );
	  java.util.List<String> texts   = new ArrayList<String>( 3 );
	  if( ac1_8k != null ) {
	    options.add( "8K-AC1-BASIC" );
	    texts.add( ac1_8k );
	  }
	  if( scch != null ) {
	    options.add( "SCCH-BASIC" );
	    texts.add( scch );
	  }
	  if( bacobas != null ) {
	    options.add( "BACOBAS" );
	    texts.add( bacobas );
	  }
	  int n = options.size();
	  if( n == 1 ) {
	    rv = texts.get( 0 );
	  }
	  else if( n > 1 ) {
	    try {
	      int v = OptionDlg.showOptionDlg(
			owner,
			"Das BASIC-Programm enth\u00E4lt Tokens,"
				+ " die von den einzelnen Interpretern\n"
				+ "als unterschiedliche Anweisungen"
				+ " verstanden werden.\n"
				+ "W\u00E4hlen Sie bitte den Interpreter aus,"
				+ " entsprechend dem die Tokens\n"
				+ "dekodiert werden sollen.",
			"BASIC-Interpreter",
			-1,
			options.toArray( new String[ n ] ) );
	      if( (v >= 0) && (v < texts.size()) ) {
		rv = texts.get( v );
	      }
	      if( rv == null ) {
		throw new UserCancelException();
	      }
	    }
	    catch( ArrayStoreException ex ) {
	      EmuUtil.exitSysError( owner, null, ex );
	    }
	  }
	}
      }
    }
    else if( addr == 0x6FB7 ) {
      rv = SourceUtil.getKCBasicStyleProgram( loadData, addr, ac1_12kTokens );
    }
    return rv;
  }


  public int getBorderColorIndex()
  {
    int rv = BLACK;
    if( this.colors != null ) {
      if( this.inverseBySW != this.inverseByKey ) {
	rv = this.colors.length - 1;
      } else {
	rv = 0;
      }
    } else {
      if( this.inverseBySW != this.inverseByKey ) {
	rv = WHITE;
      }
    }
    return rv;
  }


  public static int getDefaultSpeedKHz()
  {
    return 2000;
  }


  public static String getTinyBasicProgram( Z80MemView memory )
  {
    return SourceUtil.getTinyBasicProgram(
				memory,
				0x1950,
				memory.getMemWord( 0x18E9 ) );
  }


		/* --- FDC8272.DriveSelector --- */

  @Override
  public FloppyDiskDrive getFloppyDiskDrive( int driveNum )
  {
    FloppyDiskDrive rv = null;
    if( this.fdDrives != null ) {
      if( (driveNum >= 0) && (driveNum < this.fdDrives.length) ) {
	rv = this.fdDrives[ driveNum ];
      }
    }
    return rv;
  }


	/* --- Z80PCListener --- */

  @Override
  public synchronized void z80PCChanged( Z80CPU cpu, int pc )
  {
    if( this.pasteFast && (pc == ADDR_INCH) ) {
      CharacterIterator iter = this.pasteIter;
      if( iter != null ) {
	char ch = iter.next();
	while( ch != CharacterIterator.DONE ) {
	  if( (ch > 0) && (ch < 0x7F) ) {
	    cpu.setRegA( ch == '\n' ? '\r' : ch );
	    cpu.setRegPC( cpu.doPop() );
	    break;
	  }
	  ch = iter.next();
	}
	if( ch == CharacterIterator.DONE ) {
	  keyReleased();
	  cancelPastingText();
	}
      }
    }
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.z80TStatesProcessed( cpu, tStates );
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
    if( this.kcNet != null ) {
      this.kcNet.z80TStatesProcessed( cpu, tStates );
    }
    if( this.v24BitNum > 0 ) {
      synchronized( this ) {
	this.v24TStateCounter -= tStates;
	if( this.v24TStateCounter < 0 ) {
	  if( this.v24BitNum > 8 ) {
	    this.emuThread.getPrintMngr().putByte( this.v24ShiftBuf );
	    this.v24BitNum = 0;
	  } else {
	    this.v24ShiftBuf >>= 1;
	    if( this.v24BitOut ) {
	      this.v24ShiftBuf |= 0x80;
	    }
	    this.v24TStateCounter = this.v24TStatesPerBit;
	    this.v24BitNum++;
	  }
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    if( !this.mode64x16 ) {
      buf.append( "<h1>AC1 Status</h1>\n"
		+ "<table border=\"1\">\n"
		+ "<tr><td>Monitor ROM:</td><td>" );
      buf.append( !this.lowerDRAMEnabled && this.osRomEnabled ?
						"ein" : "aus" );
      buf.append( "</td></tr>\n"
		+ "<tr><td>SRAM / BWS</td><td>" );
      buf.append( this.lowerDRAMEnabled ? "aus" : "ein" );
      buf.append( "</td></tr>\n" );
      if( this.modeSCCH ) {
	buf.append( "<tr><td>ROM-Disk:</td><td>" );
	buf.append( this.scchRomdiskEnabled ? "ein" : "aus" );
	buf.append( "</td></tr>\n"
		+ "<tr><td>ROM-Disk Bank:</td><td>" );
	if( this.scchRomdiskBegAddr == 0x8000 ) {
	  buf.append( this.scchRomdiskBankAddr >> 15 );
	} else {
	  buf.append( this.scchRomdiskBankAddr >> 14 );
	}
	buf.append( "</td></tr>\n"
		+ "<tr><td>Programmpaket X ROM:</td><td>" );
	buf.append( this.scchPrgXEnabled ? "ein" : "aus" );
	buf.append( "</td></tr>\n"
		+ "<tr><td>SCCH-BASIC ROM:</td><td>" );
	buf.append( this.scchBasicEnabled ? "ein" : "aus" );
	buf.append( "</td></tr>\n" );
      }
      if( this.mode2010 ) {
	buf.append( "<tr><td>PIO2 ROM:</td><td>" );
	if( this.pio2Rom2010Offs >= 0 ) {
	  buf.append( "ein (2000h-27FFh)</td></tr>\n"
		+ "<tr><td>PIO2 ROM Segment-Offset:</td><td>" );
	  buf.append( String.format( "%Xh", this.pio2Rom2010Offs ) );
	} else {
	  buf.append( "aus" );
	}
	buf.append( "</td></tr>\n"
		+ "<tr><td>ROM-Bank:</td><td>" );
	if( (this.romBank2010Len > 0) && (this.romBank2010Offs >= 0) ) {
	  buf.append( "ein (A000h-" );
	  buf.append(
		String.format( "%04Xh)", 0xA000 + this.romBank2010Len ) );
	  buf.append( "</td></tr>"
		+ "<tr><td>ROM-Bank Segment-Offset:</td><td>" );
	  buf.append( String.format( "%Xh", this.romBank2010Offs ) );
	} else {
	  buf.append( "aus" );
	}
	buf.append( "</td></tr>\n" );
      }
    }
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    createColors( props );
    loadFont( props );
    checkAddPCListener( props );
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty( props, "jkcemu.system" ).equals( "AC1" );
    if( rv ) {
      rv = TextUtil.equals(
		this.osVersion,
		EmuUtil.getProperty( props, "jkcemu.ac1.os.version" ) );
    }
    if( rv ) {
      rv = TextUtil.equals(
		this.osFile,
		EmuUtil.getProperty( props, "jkcemu.ac1.os.file" ) );
    }
    if( this.modeSCCH ) {
      if( rv ) {
	rv = TextUtil.equals(
		this.scchBasicFile,
		EmuUtil.getProperty( props, "jkcemu.ac1.scch.basic.file" ) );
      }
      if( rv ) {
	rv = TextUtil.equals(
		this.scchPrgXFile,
		EmuUtil.getProperty(
				props,
				"jkcemu.ac1.scch.program_x.file" ) );
      }
      if( rv ) {
	rv = TextUtil.equals(
		this.scchRomdiskFile,
		EmuUtil.getProperty(
				props,
				"jkcemu.ac1.scch.romdisk.file" ) );
      }
    }
    if( this.mode2010 ) {
      if( rv ) {
	rv = TextUtil.equals(
		this.pio2Rom2010File,
		EmuUtil.getProperty(
				props,
				"jkcemu.ac1.2010.pio2rom.file" ) );
      }
      if( rv ) {
	rv = TextUtil.equals(
		this.romBank2010File,
		EmuUtil.getProperty(
				props,
				"jkcemu.ac1.2010.rombank.file" ) );
      }
    }
    if( rv ) {
      rv = RAMFloppy.complies(
			this.ramFloppy,
			"AC1",
			RAMFloppy.RFType.MP_3_1988,
			props,
			"jkcemu.ac1.ramfloppy." );
    }
    if( rv ) {
      rv = GIDE.complies( this.gide, props, "jkcemu.ac1." );
    }
    if( rv && (emulatesFloppyDisk( props ) != (this.fdc != null)) ) {
      rv = false;
    }
    if( rv && (emulatesColors( props ) != (this.ramColor != null)) ) {
      rv = false;
    }
    if( rv && (emulatesJoystick( props ) != this.joystickEnabled) ) {
      rv = false;
    }
    if( rv && (emulatesKCNet( props ) != (this.kcNet != null)) ) {
      rv = false;
    }
    if( rv && (emulatesUSB( props ) != (this.vdip != null)) ) {
      rv = false;
    }
    return rv;
  }


  @Override
  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    if( (this.ramColor != null) && (this.colors != null) ) {
      if( (colorIdx >= 0) && (colorIdx < this.colors.length) ) {
	color = this.colors[ colorIdx ];
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
    return this.ramColor != null ? 16 : 2;
  }


  @Override
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public void die()
  {
    if( this.gide != null ) {
      this.gide.die();
    }
    if( this.ramFloppy != null ) {
      this.ramFloppy.deinstall();
    }

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.pasteFast ) {
      cpu.removePCListener( this );
      this.pasteFast = false;
    }

    if( this.fdc != null ) {
      cpu.removeMaxSpeedListener( this.fdc );
      this.fdc.die();
    }
    if( this.kcNet != null ) {
      cpu.removeMaxSpeedListener( this.kcNet );
      this.kcNet.die();
    }
    if( this.vdip != null ) {
      this.vdip.die();
    }
  }


  public boolean emulates2010Mode()
  {
    return this.mode2010;
  }


  public boolean emulatesSCCHMode()
  {
    return this.modeSCCH;
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x2000;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    return this.mode64x16 ?
		new CharRaster( 64, 16, 16, 8, 6, 0 )
		: new CharRaster( 64, 32, 8, 8, 6, 0 );
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return 100;
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return 250;
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return 100;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/ac1.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( this.modeSCCH ) {
      if( !m1 && this.rfReadEnabled && (this.ramModule3 != null) ) {
	int idx = this.rfAddr16to19 | addr;
	if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	  rv = (int) this.ramModule3[ idx ] & 0xFF;
	}
	done = true;
      }
      if( !done && this.rf32KActive && (this.ramModule3 != null)
	  && (addr >= 0x4000) && (addr < 0xC000) )
      {
	int idx = this.rfAddr16to19 | addr;
	if( this.rf32NegA15 ) {
	  idx ^= 0x8000;
	}
	if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	  rv = (int) this.ramModule3[ idx ] & 0xFF;
	}
	done = true;
      }
      if( !done && this.scchBasicEnabled
	  && (addr >= 0x4000) && (addr < 0x6000) )
      {
	if( this.scchBasicBytes != null ) {
	  int idx = addr - 0x4000;
	  if( idx < this.scchBasicBytes.length ) {
	    rv = (int) this.scchBasicBytes[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
      if( !done && this.scchRomdiskEnabled
	  && (addr >= this.scchRomdiskBegAddr) )
      {
	if( this.scchRomdiskBytes != null ) {
	  int idx = this.scchRomdiskBankAddr
				| (addr - this.scchRomdiskBegAddr);
	  if( idx < this.scchRomdiskBytes.length ) {
	    rv = (int) this.scchRomdiskBytes[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
      if( !done && this.scchPrgXEnabled && (addr >= 0xE000) ) {
	if( this.scchPrgXBytes != null ) {
	  int idx = addr - 0xE000;
	  if( idx < this.scchPrgXBytes.length ) {
	    rv = (int) this.scchPrgXBytes[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
    }
    if( this.mode2010 ) {
      if( (addr >= 0x2000) && (addr < 0x2800)
	  && (this.pio2Rom2010Offs >= 0) )
      {
	if( this.pio2Rom2010Bytes != null ) {
	  int idx = addr - 0x2000 + this.pio2Rom2010Offs;
	  if( (idx >= 0) && (idx < this.pio2Rom2010Bytes.length) ) {
	    rv = (int) this.pio2Rom2010Bytes[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
      else if( (addr >= 0x8000) && (this.romBank2010Offs >= 0) ) {
	if( (addr >= 0xA000)
	    && (addr < (0xA000 + this.romBank2010Len))
	    && (this.romBank2010Bytes != null) )
	{
	  int idx = addr - 0xA000 + this.romBank2010Offs;
	  if( idx < this.romBank2010Bytes.length ) {
	    rv = (int) this.romBank2010Bytes[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
    }
    if( !done && !this.lowerDRAMEnabled && (addr < 0x2000) ) {
      if( this.osRomEnabled && (addr < 0x1000) ) {
	if( this.osBytes != null ) {
	  if( addr < this.osBytes.length ) {
	    rv   = (int) this.osBytes[ addr ] & 0xFF;
	    done = true;
	  }
	}
	if( !done && !this.modeSCCH && !this.mode2010
	    && (addr >= 0x0800) && (minibasic != null) )
	{
	  int idx = addr - 0x0800;
	  if( idx < minibasic.length ) {
	    rv = (int) minibasic[ idx ] & 0xFF;
	  }
	  done = true;
	}
      }
      else if( (addr >= 0x1000) && (addr < 0x1800) ) {
	int idx = addr - 0x1000;
	if( (this.ramColor != null) && ((this.regF0 & 0x04) != 0) ) {
	  if( idx < this.ramColor.length ) {
	    rv = (int) this.ramColor[ idx ] & 0xFF;
	  }
	} else {
	  if( idx < this.ramVideo.length ) {
	    rv = (int) this.ramVideo[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
      else if( addr >= 0x1800 ) {
	int idx = addr - 0x1800;
	if( idx < this.ramStatic.length ) {
	  rv = (int) this.ramStatic[ idx ] & 0xFF;
	}
	done = true;
      }
    }
    if( !done && !this.mode64x16 ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch  = -1;
    int idx = (this.mode64x16 ? 0x03FF : 0x07FF) - (chY * 64) - chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ] & 0xFF;
      if( this.mode64x16 && !this.fontSwitchable ) {
	// Zeichensatz U402
	b &= 0x3F;
	if( (b & 0x20) == 0 ) {
	  ch = b | 0x40;
	} else {
	  ch = b;
	}
      } else {
	int[] fontMap = ccdCharToUnicode;
	if( this.fontSwitchable ) {
	  if( this.fontOffs > 0 ) {
	    fontMap = scchCharToUnicode;
	  }
	} else {
	  if( this.modeSCCH ) {
	    fontMap = scchCharToUnicode;
	  } else if( this.mode2010 ) {
	    fontMap = ac1_2010CharToUnicode;
	  }
	}
	if( b < fontMap.length ) {
	  ch = fontMap[ b ];
	}
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return this.mode64x16 ? 248 : 256;
  }


  @Override
  public int getScreenWidth()
  {
    return 384;
  }


  @Override
  public int getSupportedFloppyDiskDriveCount()
  {
    return this.fdDrives != null ? this.fdDrives.length : 0;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  @Override
  public String getTitle()
  {
    return "AC1";
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
    boolean rv = false;
    int     ch = 0;
    switch( keyCode ) {
      case KeyEvent.VK_F1:
	this.inverseByKey = !this.inverseByKey;
	this.screenFrm.setScreenDirty( true );
	rv = true;
	break;

      case KeyEvent.VK_F2:
	if( this.modeSCCH || this.mode2010 ) {
	  this.graphicKeyState = !this.graphicKeyState;
	}
	rv = true;
	break;

      case KeyEvent.VK_LEFT:
	ch = 8;
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

      case KeyEvent.VK_SPACE:
	ch = 0x20;
	break;

      case KeyEvent.VK_BACK_SPACE:
	ch = ((this.modeSCCH || this.mode2010) ? 0x7F : 8);
	break;

      case KeyEvent.VK_DELETE:
	ch = ((this.modeSCCH || this.mode2010) ? 4 : 0x7F);
	break;

      case KeyEvent.VK_INSERT:
	ch = 5;
	break;

      case KeyEvent.VK_PAGE_UP:
	ch = 0x11;
	break;

      case KeyEvent.VK_PAGE_DOWN:
	ch = 0x15;
	break;

      case KeyEvent.VK_HOME:
	ch = 1;
	break;

      case KeyEvent.VK_END:
	ch = 0x1A;
	break;
    }
    if( ch > 0 ) {
      setKeyboardValue( ch | 0x80 );
      rv = true;
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    setKeyboardValue( 0 );
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      setKeyboardValue( ch | 0x80 );
      rv = true;
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
	case AC1_MINI:
	  preIdx = 0;
	  break;

	case AC1_8K:
	  preIdx = 1;
	  break;

	case AC1_12K:
	  preIdx = 2;
	  break;

	case SCCH:
	  preIdx = 3;
	  break;

	case BACOBAS:
	  preIdx = 4;
	  break;
      }
    }
    if( (preIdx < 0) && (this.modeSCCH || this.mode2010) ) {
      preIdx = 3;
    }
    switch( OptionDlg.showOptionDlg(
		this.screenFrm,
		"W\u00E4hlen Sie bitte den BASIC-Interpreter aus,\n"
			+ "dessen BASIC-Programm ge\u00F6ffnet werden soll.\n"
			+ "Die Auswahl des Interpreters ist auch deshalb"
			+ " notwendig,\n"
			+ "damit die Tokens richtig dekodiert werden.",
		"BASIC-Interpreter",
		preIdx,
		"Mini-BASIC",
		"8K-AC1-BASIC",
		"12K-AC1-BASIC",
		"SCCH-BASIC",
		"BACOBAS" ) )
    {
      case 0:
	bType = BasicType.AC1_MINI;
	text  = getTinyBasicProgram( this.emuThread );
	break;

      case 1:
	bType = BasicType.AC1_8K;
	text  = SourceUtil.getKCBasicStyleProgram(
					this.emuThread,
					0x60F7,
					ac1_8kTokens );
	break;

      case 2:
	bType = BasicType.AC1_12K;
	text  = SourceUtil.getKCBasicStyleProgram(
					this.emuThread,
					0x6FB7,
					ac1_12kTokens );
	break;

      case 3:
	bType = BasicType.SCCH;
	text  = SourceUtil.getKCBasicStyleProgram(
					this.emuThread,
					0x60F7,
					scchTokens );
	break;

      case 4:
	bType = BasicType.BACOBAS;
	text  = SourceUtil.getKCBasicStyleProgram(
					this.emuThread,
					0x60F7,
					bacobasTokens );
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
  public boolean paintScreen(
			Graphics g,
			int      xOffs,
			int      yOffs,
			int      screenScale )
  {
    if( this.fontBytes != null ) {
      int bgColorIdx = getBorderColorIndex();
      int wBase      = getScreenWidth();
      int hBase      = getScreenHeight();

      if( (xOffs > 0) || (yOffs > 0) ) {
	g.translate( xOffs, yOffs );
      }

      /*
       * Aus Gruenden der Performance werden nebeneinander liegende
       * weisse Punkte zusammengefasst und als Linie gezeichnet.
       */
      for( int y = 0; y < hBase; y++ ) {
	int     lastColorIdx = -1;
	int     xColorBeg    = -1;
	boolean inverse      = false;
	for( int x = 0; x < wBase; x++ ) {
	  int col  = x / 6;
	  int row  = 0;
	  int rPix = 0;
	  if( this.mode64x16 ) {
	    row  = y / 16;
	    rPix = y % 16;
	  } else {
	    row  = y / 8;
	    rPix = y % 8;
	  }
	  if( (rPix >= 0) && (rPix < 8) ) {
	    int vIdx = this.ramVideo.length - 1 - (row * 64) - col;
	    if( (vIdx >= 0) && (vIdx < this.ramVideo.length) ) {
	      int ch = (int) this.ramVideo[ vIdx ] & 0xFF;
	      if( this.modeSCCH || this.mode2010 ) {
		if( ch == 0x10 ) {
		  inverse = false;
		} else if( ch == 0x11 ) {
		  inverse = true;
		}
	      }
	      int fIdx = (ch * 8) + rPix + this.fontOffs;
	      if( (fIdx >= 0) && (fIdx < this.fontBytes.length ) ) {
		int m = 0x01;
		int n = x % 6;
		if( n > 0 ) {
		  m <<= n;
		}
		boolean pixel   = ((this.fontBytes[ fIdx ] & m) != 0);
		boolean invMode = (this.inverseBySW != this.inverseByKey);
		if( inverse != invMode ) {
		  pixel = !pixel;
		}
		int curColorIdx = (pixel ? WHITE : BLACK);
		if( this.ramColor != null ) {
		  int colorValue = this.ramColor[ vIdx ];
		  if( pixel ) {
		    curColorIdx = colorValue & 0x0F;
		  } else {
		    curColorIdx = (colorValue >> 4) & 0x0F;
		  }
		}
		if( curColorIdx != lastColorIdx ) {
		  if( (lastColorIdx >= 0)
		      && (lastColorIdx != bgColorIdx)
		      && (xColorBeg >= 0) )
		  {
		    g.setColor( getColor( lastColorIdx ) );
		    g.fillRect(
			xColorBeg * screenScale,
			y * screenScale,
			(x - xColorBeg) * screenScale,
			screenScale );
		  }
		  xColorBeg    = x;
		  lastColorIdx = curColorIdx;
		}
	      }
	    }
	  }
	}
	if( (lastColorIdx >= 0)
	    && (lastColorIdx != bgColorIdx)
	    && (xColorBeg >= 0) )
	{
	  g.setColor( getColor( lastColorIdx ) );
	  g.fillRect(
		xColorBeg * screenScale,
		y * screenScale,
		(wBase - xColorBeg) * screenScale,
		screenScale );
	}
      }
      if( (xOffs > 0) || (yOffs > 0) ) {
	g.translate( -xOffs, -yOffs );
      }
    }
    return true;
  }


  @Override
  public int readIOByte( int port )
  {
    int rv = 0xFF;
    if( (this.gide != null) && ((port & 0xF0) == 0x80) ) {
      int value = this.gide.read( port );
      if( value >= 0 ) {
	rv = value;
      }
    }
    else if( (port & 0xF8) == 0xE0 ) {
      if( this.ramFloppy != null ) {
	rv = this.ramFloppy.readByte( port & 0x07 );
      }
    } else {
      switch( port & 0xFF ) {
	case 0:
	case 1:
	case 2:
	case 3:
	  rv = this.ctc.read( port & 0x03 );
	  break;

	case 4:
	  synchronized( this.pio1 ) {
	    if( !this.keyboardUsed
		&& !(this.joystickEnabled && this.joystickSelected) )
	    {
	      this.pio1.putInValuePortA( 0, 0xFF );
	      this.keyboardUsed = true;
	    }
	  }
	  rv = this.pio1.readPortA();
	  break;

	case 5:
	  {
	    int v = 0x04;		// PIO B2: Grafiktaste, L-aktiv
	    if( this.graphicKeyState ) {
	      v = 0;
	    }
	    if( this.emuThread.readAudioPhase() ) {
	      v |= 0x80;
	    }
	    this.pio1.putInValuePortB( v, 0x84 );
	    rv = this.pio1.readPortB();
	  }
	  break;

	case 6:
	  rv = this.pio1.readControlA();
	  break;

	case 7:
	  rv = this.pio1.readControlB();
	  break;

	case 8:
	  if( this.pio2 != null ) {
	    // V24: CTS=L (empfangsbereit)
	    this.pio2.putInValuePortA( 0, 0x04 );
	    rv = this.pio2.readPortA();
	  }
	  break;

	case 9:
	  if( this.pio2 != null ) {
	    rv = this.pio2.readPortB();
	  }
	  break;

	case 0x0A:
	  if( this.pio2 != null ) {
	    rv = this.pio2.readControlA();
	  }
	  break;

	case 0x0B:
	  if( this.pio2 != null ) {
	    rv = this.pio2.readControlB();
	  }
	  break;

	case 0x40:
	  if( this.fdc != null ) {
	    rv             = this.fdc.readMainStatusReg();
	    this.tcEnabled = true;
	  }
	  break;

	case 0x41:
	  if( this.fdc != null ) {
	    rv             = this.fdc.readData();
	    this.tcEnabled = true;
	  }
	  break;

	case 0xC0:
	case 0xC1:
	case 0xC2:
	case 0xC3:
	  if( this.kcNet != null ) {
	    rv = this.kcNet.read( port );
	  }
	  break;

	case 0xF0:
	  if( this.ramColor != null ) {
	    rv = this.regF0;
	  }
	  break;

	case 0xFC:
	case 0xFD:
	case 0xFE:
	case 0xFF:
	  if( this.vdip != null ) {
	    rv = this.vdip.read( port );
	  }
	  break;
      }
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    super.reset( resetLevel, props );
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
      initSRAM( this.ramStatic, props );
      fillRandom( this.ramVideo );
    }
    if( this.ramColor != null ) {
      if( (this.osBytes == mon31_64x16)
	  || (this.osBytes == mon31_64x32)
	  || (this.osBytes == monSCCH80)
	  || (this.osBytes == monSCCH1088) )
      {
	Arrays.fill( this.ramColor, (byte) 0x0F );
      } else {
	if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
	  fillRandom( this.ramColor );
	}
      }
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.ctc.reset( true );
      this.pio1.reset( true );
      if( this.pio2 != null ) {
	this.pio2.reset( true );
      }
    } else {
      this.ctc.reset( false );
      this.pio1.reset( false );
      if( this.pio2 != null ) {
	this.pio2.reset( false );
      }
    }
    if( this.gide != null ) {
      this.gide.reset();
    }
    if( this.fdc != null ) {
      this.fdc.reset( resetLevel == EmuThread.ResetLevel.POWER_ON );
    }
    if( this.fdDrives != null ) {
      for( int i = 0; i < this.fdDrives.length; i++ ) {
	FloppyDiskDrive drive = this.fdDrives[ i ];
	if( drive != null ) {
	  drive.reset();
	}
      }
    }
    this.inverseBySW      = false;
    this.pio1B3State      = false;
    this.fdcWaitEnabled   = false;
    this.tcEnabled        = true;
    this.keyboardUsed     = false;
    this.graphicKeyState  = false;
    this.lowerDRAMEnabled = false;
    this.osRomEnabled     = true;
    this.scchPrgXEnabled  = false;
    this.scchBasicEnabled = false;
    this.rf32KActive      = false;
    this.rf32NegA15       = false;
    this.rfReadEnabled    = false;
    this.rfWriteEnabled   = false;
    this.rfAddr16to19     = 0;
    this.regF0            = 0;
    this.pio2Rom2010Offs  = -1;
    this.romBank2010Offs  = -1;
    this.fontOffs         = 0;
    this.v24BitOut        = true;	// V24: H-Pegel
    this.v24BitNum        = 0;
    this.v24ShiftBuf      = 0;
    this.v24TStateCounter = 0;
    if( (this.osBytes == monSCCH80)
	|| (this.osBytes == monSCCH1088)
	|| (this.osBytes == mon2010c) )
    {
      this.v24TStatesPerBit = V24_TSTATES_PER_BIT_INTERN;
    } else {
      this.v24TStatesPerBit = V24_TSTATES_PER_BIT_EXTERN;
    }
    this.pio1.putInValuePortB( 0, 0x04 );
  }


  @Override
  public void saveBasicProgram()
  {
    boolean   cancelled = false;
    int       begAddr   = -1;
    int       endAddr   = -1;
    int       hsType    = -1;
    BasicType bType     = null;
    int       preIdx    = -1;
    if( this.lastBasicType != null ) {
      switch( this.lastBasicType ) {
	case AC1_MINI:
	  preIdx = 0;
	  break;

	case AC1_8K:
	case SCCH:
	case BACOBAS:
	case ADDR_60F7:
	  preIdx = 1;
	  break;

	case AC1_12K:
	  preIdx = 2;
	  break;
      }
    }
    if( (preIdx < 0) && this.modeSCCH ) {
      preIdx = 1;
    }
    switch( OptionDlg.showOptionDlg(
		this.screenFrm,
		"W\u00E4hlen Sie bitte den BASIC-Interpreter aus,\n"
			+ "dessen BASIC-Programm gespeichert werden soll.",
		"BASIC-Interpreter",
		preIdx,
		"Mini-BASIC",
		"8K-AC1-BASIC, SCCH-BASIC oder BACOBAS",
		"12K-AC1-BASIC" ) )
    {
      case 0:
	bType   = BasicType.AC1_MINI;
	begAddr = 0x18C0;
	endAddr = this.emuThread.getMemWord( 0x18E9 );
	hsType  = 'b';
	break;

      case 1:
	bType = this.lastBasicType;
	if( (bType != BasicType.AC1_8K)
	    && (bType != BasicType.SCCH)
	    && (bType != BasicType.BACOBAS) )
	{
	  bType = BasicType.ADDR_60F7;
	}
	begAddr = 0x6000;
	endAddr = SourceUtil.getKCBasicStyleEndAddr( this.emuThread, 0x60F7 );
	hsType  = 'B';
	break;

      case 2:
	bType   = BasicType.AC1_12K;
	begAddr = 0x6FB7;
	endAddr = SourceUtil.getKCBasicStyleEndAddr( this.emuThread, begAddr );
	hsType  = 'B';
	break;

      default:
	cancelled = true;
    }
    if( !cancelled ) {
      if( (begAddr > 0) && (endAddr > begAddr) ) {
	this.lastBasicType = bType;
	(new SaveDlg(
		this.screenFrm,
		begAddr,
		endAddr,
		hsType,
		false,          // kein KC-BASIC
		false,		// kein RBASIC
		"BASIC-Programm speichern" )).setVisible( true );
      } else {
	showNoBasic();
      }
    }
  }


  @Override
  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    if( this.fdDrives != null ) {
      if( (idx >= 0) && (idx < this.fdDrives.length) ) {
	this.fdDrives[ idx ] = drive;
      }
    }
  }


  @Override
  public void setJoystickAction( int joyNum, int actionMask )
  {
    if( this.joystickEnabled && (joyNum == 0) ) {
      int value = 0;
      if( (actionMask & JoystickThread.UP_MASK) != 0 ) {
	value |= 0x01;
      }
      if( (actionMask & JoystickThread.DOWN_MASK) != 0 ) {
	value |= 0x02;
      }
      if( (actionMask & JoystickThread.LEFT_MASK) != 0 ) {
	value |= 0x04;
      }
      if( (actionMask & JoystickThread.RIGHT_MASK) != 0 ) {
	value |= 0x08;
      }
      if( (actionMask & JoystickThread.BUTTONS_MASK) != 0 ) {
	value |= 0x10;
      }
      this.joystickValue = value;
      synchronized( this.pio1 ) {
	if( this.joystickSelected ) {
	  this.pio1.putInValuePortA( this.joystickValue, 0xFF );
	}
      }
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( this.modeSCCH ) {
      if( this.rfWriteEnabled && (this.ramModule3 != null) ) {
	int idx = this.rfAddr16to19 | addr;
	if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	  this.ramModule3[ idx ] = (byte) value;
	  rv = true;
	}
	done = true;
      }
      if( !done && this.rf32KActive && (this.ramModule3 != null)
	  && (addr >= 0x4000) && (addr < 0xC000) )
      {
	int idx = this.rfAddr16to19 | addr;
	if( this.rf32NegA15 ) {
	  idx ^= 0x8000;
	}
	if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	  this.ramModule3[ idx ] = (byte) value;
	  rv = true;
	}
	done = true;
      }
      if( !done && (addr < 0x1000) ) {
	// Durchschreiben auf den DRAM
	this.emuThread.setRAMByte( addr, value );
	done = true;
	rv   = true;
      }
    }
    if( !done && !this.lowerDRAMEnabled && (addr < 0x2000) ) {
      if( addr < 0x1000 ) {
	if( !this.osRomEnabled ) {
	  this.emuThread.setRAMByte( addr, value );
	  rv = true;
	}
      }
      else if( (addr >= 0x1000) && (addr < 0x1800) ) {
	int idx = addr - 0x1000;
	if( (this.ramColor != null) && ((this.regF0 & 0x04) != 0) ) {
	  if( idx < this.ramColor.length ) {
	    this.ramColor[ idx ] = (byte) value;
	    this.screenFrm.setScreenDirty( true );
	    rv = true;
	  }
	} else {
	  if( idx < this.ramVideo.length ) {
	    this.ramVideo[ idx ] = (byte) value;
	    this.screenFrm.setScreenDirty( true );
	    rv = true;
	  }
	}
      }
      else if( (addr >= 0x1800) && (addr < 0x2000) ) {
	int idx = addr - 0x1800;
	if( idx < this.ramStatic.length ) {
	  this.ramStatic[ idx ] = (byte) value;
	  rv = true;
	}
      }
      done = true;
    }
    if( !done && !this.mode64x16 ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return this.extFont;
  }


  @Override
  public synchronized void startPastingText( String text )
  {
    boolean done = false;
    if( text != null ) {
      if( !text.isEmpty() ) {
	if( this.pasteFast ) {
	  cancelPastingText();
	  CharacterIterator iter = new StringCharacterIterator( text );
	  char              ch   = iter.first();
	  if( ch != CharacterIterator.DONE ) {
	    if( ch == '\n' ) {
	      ch = '\r';
	    }
	    /*
	     * Da sich die Programmausfuehrung i.d.R. bereits
	     * in der betreffenden Systemfunktion befindet,
	     * muss das erste Zeichen direkt an der Tastatur
	     * angelegt werden,
	     * damit der Systemaufruf beendet wird und somit
	     * der naechste Aufruf dann abgefangen werden kann.
	     */
	    keyTyped( ch );
	    long millis = (ch == '\r' ?
				getDelayMillisAfterPasteEnter()
				: getDelayMillisAfterPasteChar());
	    if( millis > 0 ) {
	      try {
		Thread.sleep( millis );
	      }
	      catch( InterruptedException ex ) {}
	      keyReleased();
	    }
	    this.pasteIter = iter;
	    done           = false;
	  }
	} else {
	  super.startPastingText( text );
	  done = false;
	}
      }
    }
    if( !done ) {
      this.screenFrm.firePastingTextFinished();
    }
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
  public boolean supportsPasteFromClipboard()
  {
    return true;
  }


  @Override
  public boolean supportsPrinter()
  {
    return this.pio2 != null;
  }


  @Override
  public boolean supportsRAMFloppy1()
  {
    return this.ramFloppy != null;
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
    if( fileFmt != null ) {
      if( fileFmt.equals( FileInfo.HEADERSAVE ) && (fileType == 'B') ) {
	if( begAddr == 0x60F7 ) {
	  int topAddr = begAddr + len;
	  this.emuThread.setMemWord( 0x60D2, topAddr );
	  this.emuThread.setMemWord( 0x60D4, topAddr );
	  this.emuThread.setMemWord( 0x60D6, topAddr );
	}
	else if( begAddr == 0x6FB7 ) {
	  int topAddr = begAddr + len;
	  this.emuThread.setMemWord( 0x415E, topAddr );
	  this.emuThread.setMemWord( 0x4160, topAddr );
	  this.emuThread.setMemWord( 0x4162, topAddr );
	}
      }
    }
  }


  @Override
  public void writeIOByte( int port, int value )
  {
    value &= 0xFF;
    if( (this.gide != null) && ((port & 0xF0) == 0x80) ) {
      this.gide.write( port, value );
    } else if( (port & 0xF8) == 0xE0 ) {
      if( this.ramFloppy != null ) {
	this.ramFloppy.writeByte( port & 0x07, value );
      }
    } else {
      switch( port & 0xFF ) {
	case 0:
	case 1:
	case 2:
	case 3:
	  this.ctc.write( port & 0x03, value );
	  break;

	case 4:
	  this.pio1.writePortA( value );
	  break;

	case 5:
	  this.pio1.writePortB( value );
	  synchronized( this.pio1 ) {
	    int v = this.pio1.fetchOutValuePortB( false );
	    this.emuThread.writeAudioPhase(
			(v & (this.emuThread.isSoundOutEnabled() ?
						0x01 : 0x40 )) != 0 );
	    if( this.joystickEnabled ) {
	      boolean joySelected = ((v & 0x02) == 0);
	      if( joySelected != this.joystickSelected ) {
		this.joystickSelected = joySelected;
		this.pio1.putInValuePortA(
				joySelected ?
					this.joystickValue
					: this.keyboardValue,
				0xFF );
	      }
	    }
	    if( this.fontSwitchable ) {
	      boolean state = ((v & 0x08) != 0);
	      if( this.colors != null ) {
		/*
		 * Die Farbgrafikkarte lauscht selbst am Bus, d.h.,
		 * es wird das Signal vor der PIO gelesen.
		 */
		state = ((value & 0x08) != 0);
	      }
	      if( state != this.pio1B3State ) {
		int offs = 0;
		if( this.fontBytes != null ) {
		  if( state && (this.fontBytes.length > 0x0800) ) {
		    offs = 0x0800;
		  }
		}
		if( offs != this.fontOffs ) {
		  this.fontOffs = offs;
		  this.screenFrm.setScreenDirty( true );
		}
		this.pio1B3State = state;
	      }
	    } else {
	      if( this.mode2010 ) {
		boolean state = ((v & 0x08) != 0);
		if( state != this.pio1B3State ) {
		  this.inverseBySW = state;
		  this.pio1B3State = state;
		  this.screenFrm.setScreenDirty( true );
		}
	      }
	    }
	  }
	  break;

	case 6:
	  this.pio1.writeControlA( value );
	  break;

	case 7:
	  this.pio1.writeControlB( value );
	  break;

	case 8:
	  if( this.pio2 != null ) {
	    this.pio2.writePortA( value );
	    synchronized( this ) {
	      boolean state = ((this.pio2.fetchOutValuePortA( false )
							  & 0x02) != 0);
	      /*
	       * fallende Flanke: Wenn gerade keine Ausgabe laeuft,
	       * dann beginnt jetzt eine.
	       */
	      if( !state && this.v24BitOut && (this.v24BitNum == 0) ) {
		this.v24ShiftBuf      = 0;
		this.v24TStateCounter = 3 * this.v24TStatesPerBit / 2;
		this.v24BitNum++;
	      }
	      this.v24BitOut = state;
	    }
	    this.pio2.writePortA( value );
	  }
	  break;

	case 9:
	  if( this.pio2 != null ) {
	    this.pio2.writePortB( value );
	  }
	  break;

	case 0x0A:
	  if( this.pio2 != null ) {
	    this.pio2.writeControlA( value );
	  }
	  break;

	case 0x0B:
	  if( this.pio2 != null ) {
	    this.pio2.writeControlB( value );
	  }
	  break;

	case 0x0E:
	  if( this.mode2010 ) {
	    if( value >= 0xF0 ) {
	      int seg  = value & 0x0F;
	      int offs = 0;
	      for( int i = 0; i < seg; i++ ) {
		offs += romBank2010SegLengths[ i ];
	      }
	      this.romBank2010Len  = romBank2010SegLengths[ seg ];
	      this.romBank2010Offs = offs;
	    } else {
	      this.romBank2010Len  = 0;
	      this.romBank2010Offs = -1;
	    }
	  }
	  break;

	case 0x0F:
	  if( this.mode2010 ) {
	    switch( value & 0x70 ) {
	      case 0x10:
		this.pio2Rom2010Offs = 0x0000;
		break;
	      case 0x20:
		this.pio2Rom2010Offs = 0x0800;
		break;
	      case 0x40:
		this.pio2Rom2010Offs = 0x1000;
		break;
	      case 0x60:
		this.pio2Rom2010Offs = 0x1800;
		break;
	      default:
		this.pio2Rom2010Offs = -1;
	    }
	  }
	  break;

	case 0x14:
	  if( this.modeSCCH ) {
	    this.scchBasicEnabled   = ((value & 0x02) != 0);
	    this.lowerDRAMEnabled   = ((value & 0x04) != 0);
	    this.scchRomdiskEnabled = ((value & 0x08) != 0);
	    if( this.scchRomdiskEnabled ) {
	      int bank = (value & 0x01) | ((value >> 3) & 0x0E);
	      if( this.scchRomdiskBegAddr == 0x8000 ) {
		this.scchRomdiskBankAddr = (bank << 15);
	      } else {
		this.scchRomdiskBankAddr = (bank << 14);
	      }
	      this.scchPrgXEnabled = false;
	    } else {
	      this.scchPrgXEnabled = ((value & 0x01) != 0);
	    }
	  }
	  break;

	case 0x15:
	  if( this.modeSCCH ) {
	    this.rfAddr16to19   = (value << 16) & 0xF0000;
	    this.rf32NegA15     = ((value & 0x10) != 0);
	    this.rf32KActive    = ((value & 0x20) != 0);
	    this.rfReadEnabled  = ((value & 0x40) != 0);
	    this.rfWriteEnabled = ((value & 0x80) != 0);
	  }
	  break;

	case 0x16:
	  if( this.modeSCCH ) {
	    this.osRomEnabled = false;
	  }
	  break;

	case 0x17:
	  if( this.modeSCCH ) {
	    this.osRomEnabled = true;
	  }
	  break;

	case 0x1C:
	case 0x1D:
	case 0x1E:
	case 0x1F:
	  if( !this.mode64x16 ) {
	    this.lowerDRAMEnabled = ((value & 0x01) != 0);
	  }
	  break;

	case 0x41:
	  if( this.fdc != null ) {
	    this.fdc.write( value );
	    this.tcEnabled = true;
	  }
	  break;

	case 0x42:
	case 0x43:
	  if( this.fdc != null ) {
	    if( this.fdcWaitEnabled ) {
	      // max. 20 Mikrosekunden warten
	      Z80CPU cpu     = this.emuThread.getZ80CPU();
	      int    tStates = cpu.getMaxSpeedKHz() / 50;
	      while( !this.fdc.isInterruptRequest() && (tStates > 0) ) {
		z80TStatesProcessed( cpu, 4 );
		tStates -= 4;
	      }
	    }
	  }
	  break;

	case 0x44:
	case 0x45:
	  if( this.fdc != null ) {
	    this.fdcWaitEnabled = ((value & 0x02) != 0);
	    if( ((value & 0x10) != 0) && this.tcEnabled ) {
	      this.fdc.fireTC();
	      this.tcEnabled = false;
	    }
	  }
	  break;

	case 0x48:
	case 0x49:
	  if( (this.fdc != null) && this.tcEnabled ) {
	    this.fdc.fireTC();
	    this.tcEnabled = false;
	  }
	  break;

	case 0xC0:
	case 0xC1:
	case 0xC2:
	case 0xC3:
	  if( this.kcNet != null ) {
	    this.kcNet.write( port, value );
	  }
	  break;

	case 0xF0:
	  if( this.colors != null ) {
	    Z80CPU cpu = this.emuThread.getZ80CPU();
	    if( (value & 0x01) != 0 ) {
	      if( ((this.regF0 & 0x01) == 0)
		  && (cpu.getMaxSpeedKHz() == 2000) )
	      {
		cpu.setMaxSpeedKHz( 4000 );
	      }
	    } else {
	      if( ((this.regF0 & 0x01) != 0)
		  && (cpu.getMaxSpeedKHz() == 4000) )
	      {
		cpu.setMaxSpeedKHz( 2000 );
	      }
	    }
	    if( (value & 0x02) != (this.regF0 & 0x02) ) {
	      this.inverseBySW = ((value & 0x02) != 0);
	      this.screenFrm.setScreenDirty( true );
	    }
	    this.regF0 = value;
	  }
	  break;

	case 0xFC:
	case 0xFD:
	case 0xFE:
	case 0xFF:
	  if( this.vdip != null ) {
	    this.vdip.write( port, value );
	  }
	  break;
      }
    }
  }


	/* --- private Methoden --- */

  private synchronized void checkAddPCListener( Properties props )
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    if( cpu != null ) {
      boolean pasteFast = EmuUtil.getBooleanProperty(
				props,
				"jkcemu.ac1.paste.fast",
				true );
      if( pasteFast != this.pasteFast ) {
	this.pasteFast = pasteFast;
	if( pasteFast ) {
	  cpu.addPCListener( this, ADDR_INCH );
	} else {
	  cpu.removePCListener( this );
	}
      }
    }
  }


  private void createColors( Properties props )
  {
    float f = getBrightness( props );
    if( (this.colors != null) && (f >= 0F) && (f <= 1F) ) {
      for( int i = 0; i < this.colors.length; i++ ) {
	int v = Math.round( ((i & 0x08) != 0 ? 255 : 160) * f );
	this.colors[ i ] = new Color(
		(i & 0x01) != 0 ? v : 0,
		(i & 0x02) != 0 ? v : 0,
		(i & 0x04) != 0 ? v : 0 );
      }
    }
  }


  private static boolean emulatesColors( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			"jkcemu.ac1.color",
			false );
  }


  private static boolean emulatesJoystick( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			"jkcemu.ac1.joystick.enabled",
			false );
  }


  private static boolean emulatesFloppyDisk( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			"jkcemu.ac1.floppydisk.enabled",
			false );
  }


  private boolean emulatesKCNet( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.ac1.kcnet.enabled",
				false );
  }


  private boolean emulatesUSB( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				"jkcemu.ac1.vdip.enabled",
				false );
  }


  private static int getScchRomdiskBegAddr( Properties props )
  {
    int rv = 0xC000;
    if( props != null ) {
      String text = EmuUtil.getProperty(
			props,
			"jkcemu.ac1.scch.romdisk.address.begin" );
      if( text != null ) {
	if( text.trim().equals( "8000" ) ) {
	  rv = 0x8000;
	}
      }
    }
    return rv;
  }


  private void loadFont( Properties props )
  {
    this.extFont   = true;
    this.fontBytes = readFontByProperty(
	props,
	"jkcemu.ac1.font.file",
	this.fontSwitchable ? 0x1000 : 0x0800 );

    if( this.fontBytes == null ) {
      this.extFont = false;
      if( this.fontSwitchable) {
	byte[] primaryFont = null;
	if( this.mode2010 ) {
	  if( font2010 == null ) {
	    font2010 = readResource( "/rom/ac1/font2010.bin" );
	  }
	  primaryFont = font2010;
	} else {
	  if( fontSCCH == null ) {
	    fontSCCH = readResource( "/rom/ac1/scchfont.bin" );
	  }
	  primaryFont = fontSCCH;
	}
	if( fontACC == null ) {
	  fontACC = readResource( "/rom/ac1/accfont.bin" );
	}
	if( (primaryFont != null) && (fontACC != null) ) {
	  /*
	   * Der SCCH-Zeichensatz soll aktiv sein,
	   * wenn auf PIO1 B3 eine 1 ausgegeben wird.
	   * Demzufolge muss der SCCH-Zeichensatz in den hinteren
	   * 2 KByte liegen
	   */
	  byte[] a = new byte[ 0x1000 ];
	  Arrays.fill( a, (byte) 0 );
	  System.arraycopy(
			fontACC,
			0,
			a,
			0,
			Math.min( fontACC.length, 0x0800 ) );
	  System.arraycopy(
			primaryFont,
			0,
			a,
			0x0800,
			Math.min( primaryFont.length, 0x0800 ) );
	  this.fontBytes = a;
	} else {
	  if( primaryFont != null ) {
	    this.fontBytes = primaryFont;
	  } else {
	    this.fontBytes = fontACC;
	  }
	}
      } else {
	if( this.mode64x16 ) {
	  if( fontU402 == null ) {
	    fontU402 = readResource( "/rom/ac1/u402bm513x4.bin" );
	  }
	  this.fontBytes = fontU402;
	} else if( this.modeSCCH ) {
	  if( fontSCCH == null ) {
	    fontSCCH = readResource( "/rom/ac1/scchfont.bin" );
	  }
	  this.fontBytes = fontSCCH;
	} else if( this.mode2010 ) {
	  if( font2010 == null ) {
	    font2010 = readResource( "/rom/ac1/font2010.bin" );
	  }
	  this.fontBytes = font2010;
	} else {
	  if( fontACC == null ) {
	    fontACC = readResource( "/rom/ac1/accfont.bin" );
	  }
	  this.fontBytes = fontACC;
	}
      }
    }
  }


  private void loadROMs( Properties props )
  {
    // OS-ROM
    this.osFile  = EmuUtil.getProperty( props, "jkcemu.ac1.os.file" );
    this.osBytes = readFile( this.osFile, 0x1000, "Monitorprogramm" );
    if( this.osBytes == null ) {
      if( this.modeSCCH ) {
	if( this.osVersion.startsWith( "SCCH8.0" ) ) {
	  if( monSCCH80 == null ) {
	    monSCCH80 = readResource( "/rom/ac1/scchmon_80g.bin" );
	  }
	  this.osBytes = monSCCH80;
	} else {
	  if( monSCCH1088 == null ) {
	    monSCCH1088 = readResource( "/rom/ac1/scchmon_1088g.bin" );
	  }
	  this.osBytes = monSCCH1088;
	}
      } else if( this.mode2010 ) {
	if( mon2010c == null ) {
	  mon2010c = readResource( "/rom/ac1/mon2010c.bin" );
	}
	this.osBytes = mon2010c;
      } else {
	if( this.mode64x16 ) {
	  if( mon31_64x16 == null ) {
	    mon31_64x16 = readResource( "/rom/ac1/mon_31_64x16.bin" );
	  }
	  this.osBytes = mon31_64x16;
	} else {
	  if( mon31_64x32 == null ) {
	    mon31_64x32 = readResource( "/rom/ac1/mon_31_64x32.bin" );
	  }
	  this.osBytes = mon31_64x32;
	}
      }
    }

    // Mini-BASIC
    if( !this.modeSCCH ) {
      if( minibasic == null ) {
	minibasic = readResource( "/rom/ac1/minibasic.bin" );
      }
    }

    // SCCH BASIC-ROM
    if( this.modeSCCH ) {
      this.scchBasicFile = EmuUtil.getProperty(
					props,
					"jkcemu.ac1.scch.basic.file" );
      this.scchBasicBytes = readFile(
				this.scchBasicFile,
				0x2000,
				"BASIC" );
      if( this.scchBasicBytes == null ) {
	if( gsbasic == null ) {
	  gsbasic = readResource( "/rom/ac1/gsbasic.bin" );
	}
	this.scchBasicBytes = gsbasic;
      }
    } else {
      this.scchBasicFile  = null;
      this.scchBasicBytes = null;
    }

    // SCCH Programmpaket X
    if( this.modeSCCH ) {
      this.scchPrgXFile  = EmuUtil.getProperty(
				props,
				"jkcemu.ac1.scch.program_x.file" );
      this.scchPrgXBytes = readFile(
				this.scchPrgXFile,
				0x2000,
				"Programmpaket X" );
    } else {
      this.scchPrgXFile  = null;
      this.scchPrgXBytes = null;
    }

    // SCCH ROM-Disk
    if( this.modeSCCH ) {
      this.scchRomdiskFile  = EmuUtil.getProperty(
				props,
				"jkcemu.ac1.scch.romdisk.file" );
      this.scchRomdiskBytes = readFile(
				this.scchRomdiskFile,
				0x40000,
				"SCCH-Modul 1 ROM-Disk" );
    } else {
      this.scchRomdiskFile  = null;
      this.scchRomdiskBytes = null;
    }


    // AC1-2010 ROM-Baenke
    if( this.mode2010 ) {
      this.pio2Rom2010File  = EmuUtil.getProperty(
				props,
				"jkcemu.ac1.2010.pio2rom.file" );
      this.pio2Rom2010Bytes = readFile(
				this.pio2Rom2010File,
				0x2000,
				"AC1-2010 PIO2 ROM" );
      if( this.pio2Rom2010Bytes == null ) {
	if( pio2Rom2010 == null ) {
	  pio2Rom2010 = readResource( "/rom/ac1/pio2rom2010.bin" );
	}
	this.pio2Rom2010Bytes = pio2Rom2010;
      }
      this.romBank2010File  = EmuUtil.getProperty(
				props,
				"jkcemu.ac1.2010.rombank.file" );
      this.romBank2010Bytes = readFile(
				this.romBank2010File,
				0x20000,
				"AC1-2010 ROM-Bank" );
    } else {
      this.pio2Rom2010File  = null;
      this.pio2Rom2010Bytes = null;
      this.romBank2010File  = null;
      this.romBank2010Bytes = null;
    }

    // Zeichensatz
    loadFont( props );
  }
}
