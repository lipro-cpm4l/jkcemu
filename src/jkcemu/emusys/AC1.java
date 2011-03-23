/*
 * (c) 2008-2011 Jens Mueller
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
import java.util.*;
import javax.swing.JOptionPane;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.emusys.ac1_llc2.AbstractSCCHSys;
import jkcemu.joystick.JoystickThread;
import z80emu.*;


public class AC1
		extends AbstractSCCHSys
		implements
			FDC8272.DriveSelector,
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
   */
  private static int V24_TSTATES_PER_BIT = 248;

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

  private static byte[] os31_64x16 = null;
  private static byte[] os31_64x32 = null;
  private static byte[] scchOS80   = null;
  private static byte[] scchOS1088 = null;
  private static byte[] minibasic  = null;
  private static byte[] gsbasic    = null;
  private static byte[] font2010   = null;
  private static byte[] fontCCD    = null;
  private static byte[] fontSCCH   = null;
  private static byte[] fontU402   = null;

  private byte[]            ramStatic;
  private byte[]            ramVideo;
  private byte[]            ramModule3;
  private byte[]            fontBytes;
  private byte[]            osBytes;
  private byte[]            basicBytes;
  private byte[]            prgXBytes;
  private byte[]            romdiskBytes;
  private String            fontFile;
  private String            osFile;
  private String            osVersion;
  private String            basicFile;
  private String            prgXFile;
  private String            romdiskFile;
  private BasicType         lastBasicType;
  private RAMFloppy         ramFloppy;
  private Z80CTC            ctc;
  private Z80PIO            pio1;
  private Z80PIO            pio2;
  private GIDE              gide;
  private FDC8272           fdc;
  private FloppyDiskDrive[] fdDrives;
  private boolean           fdcWaitEnabled;
  private boolean           mode64x16;
  private boolean           scchMode;
  private boolean           fontSwitchable;
  private boolean           inverseSwitchable;
  private boolean           screenInverseMode;
  private boolean           pio1B3State;
  private boolean           keyboardUsed;
  private volatile boolean  graphicKeyState;
  private volatile boolean  joystickSelected;
  private boolean           lowerDRAMEnabled;
  private boolean           romEnabled;
  private boolean           romdiskEnabled;
  private boolean           prgXEnabled;
  private boolean           basicEnabled;
  private boolean           rf32KActive;
  private boolean           rf32NegA15;
  private boolean           rfReadEnabled;
  private boolean           rfWriteEnabled;
  private int               rfAddr16to19;
  private int               gideIOAddr;
  private int               romdiskBegAddr;
  private int               romdiskBankAddr;
  private int               joystickValue;
  private int               keyboardValue;
  private int               fontOffs;
  private boolean           v24BitOut;
  private int               v24BitNum;
  private int               v24ShiftBuf;
  private int               v24TStateCounter;


  public AC1( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.fontSwitchable    = false;
    this.inverseSwitchable = false;
    this.mode64x16         = false;
    this.scchMode          = true;
    this.osVersion = EmuUtil.getProperty( props, "jkcemu.ac1.os.version" );
    if( this.osVersion.startsWith( "3.1" ) ) {
      this.scchMode = false;
      if( this.osVersion.startsWith( "3.1_64x16" ) ) {
	this.mode64x16 = true;
      }
    }
    else if( this.osVersion.equals( "SCCH10/88" ) ) {
      this.fontSwitchable = true;
    }
    else if( this.osVersion.equals( "SCCH8.0_2010" ) ) {
      this.inverseSwitchable = true;
    }
    this.osBytes         = null;
    this.osFile          = null;
    this.basicBytes      = null;
    this.basicFile       = null;
    this.prgXBytes       = null;
    this.prgXFile        = null;
    this.romdiskBytes    = null;
    this.romdiskFile     = null;
    this.romdiskBegAddr  = getROMDiskBegAddr( props );
    this.romdiskBankAddr = 0;
    this.lastBasicType   = null;

    this.ramModule3 = null;
    if( this.mode64x16 ) {
      this.ramStatic = new byte[ 0x0400 ];
      this.ramVideo  = new byte[ 0x0400 ];
    } else {
      this.ramStatic = new byte[ 0x0800 ];
      this.ramVideo  = new byte[ 0x0800 ];
      if( this.scchMode ) {
	this.ramModule3 = this.emuThread.getExtendedRAM( 0x100000 ); // 1 MByte
      }
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

    this.gide = GIDE.getGIDE( this.screenFrm, props, "jkcemu.ac1." );
    updGideIOAddr( props );

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( "CTC (IO-Adressen 00-03)" );
    this.pio1  = new Z80PIO( "PIO (IO-Adressen 04-07)" );
    if( this.scchMode ) {
      this.pio2 = new Z80PIO( "V24-PIO (IO-Adressen 08-0B)" );
      cpu.setInterruptSources( this.ctc, this.pio1, this.pio2 );
    } else {
      this.pio2 = null;
      cpu.setInterruptSources( this.ctc, this.pio1 );
    }
    this.ctc.setTimerConnection( 0, 1 );
    cpu.addTStatesListener( this );
    if( this.fdc != null ) {
      this.fdc.setTStatesPerMilli( cpu.getMaxSpeedKHz() );
      cpu.addMaxSpeedListener( this.fdc );
    }

    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
    reset( EmuThread.ResetLevel.POWER_ON, props );
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
    return this.screenInverseMode ? WHITE : BLACK;
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


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.z80TStatesProcessed( cpu, tStates );
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
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
	    this.v24TStateCounter = V24_TSTATES_PER_BIT;
	    this.v24BitNum++;
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
    loadFont( props );
    updGideIOAddr( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty( props, "jkcemu.system" ).equals( "AC1" );
    if( rv ) {
      rv = EmuUtil.equals(
		this.osVersion,
		EmuUtil.getProperty( props, "jkcemu.ac1.os.version" ) );
    }
    if( this.scchMode ) {
      if( rv ) {
	rv = EmuUtil.equals(
		this.osFile,
		EmuUtil.getProperty( props,  "jkcemu.ac1.os.file" ) );
      }
      if( rv ) {
	rv = EmuUtil.equals(
		this.basicFile,
		EmuUtil.getProperty( props,  "jkcemu.ac1.basic.file" ) );
      }
      if( rv ) {
	rv = EmuUtil.equals(
		this.prgXFile,
		EmuUtil.getProperty( props,  "jkcemu.ac1.program_x.file" ) );
      }
      if( rv ) {
	rv = EmuUtil.equals(
		this.romdiskFile,
		EmuUtil.getProperty( props,  "jkcemu.ac1.romdisk.file" ) );
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
      if( emulatesFloppyDisk( props ) != (this.fdc != null) ) {
	rv = false;
      }
    }
    if( rv ) {
      rv = GIDE.complies( this.gide, props, "jkcemu.ac1." );
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
    if( this.gide != null ) {
      this.gide.die();
    }
    if( this.ramFloppy != null ) {
      this.ramFloppy.deinstall();
    }
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.fdc != null ) {
      cpu.removeMaxSpeedListener( this.fdc );
    }
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0x2000;
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
    return this.mode64x16 ? 16 : 32;
  }


  @Override
  public int getCharRowHeight()
  {
    return this.mode64x16 ? 16 : 8;
  }


  @Override
  public int getCharWidth()
  {
    return 6;
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return 60;
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return 150;
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return 60;
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
    if( this.scchMode ) {
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
	  if( (idx & 0x8000) != 0 ) {
	    idx &= 0xF7FFF;
	  } else {
	    idx |= 0x8000;
	  }
	}
	if( (idx >= 0) && (idx < this.ramModule3.length) ) {
	  rv = (int) this.ramModule3[ idx ] & 0xFF;
	}
	done = true;
      }
      if( !done && this.basicEnabled
	  && (addr >= 0x4000) && (addr < 0x6000) )
      {
	if( this.basicBytes != null ) {
	  int idx = addr - 0x4000;
	  if( idx < this.basicBytes.length ) {
	    rv = (int) this.basicBytes[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
      if( !done && this.romdiskEnabled && (addr >= this.romdiskBegAddr) ) {
	if( this.romdiskBytes != null ) {
	  int idx = this.romdiskBankAddr | (addr - this.romdiskBegAddr);
	  if( idx < this.romdiskBytes.length ) {
	    rv = (int) this.romdiskBytes[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
      if( !done && this.prgXEnabled && (addr >= 0xE000) ) {
	if( this.prgXBytes != null ) {
	  int idx = addr - 0xE000;
	  if( idx < this.prgXBytes.length ) {
	    rv = (int) this.prgXBytes[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
    }
    if( !done && !this.lowerDRAMEnabled && (addr < 0x2000) ) {
      if( this.romEnabled && (addr < 0x1000) ) {
	if( this.osBytes != null ) {
	  if( addr < this.osBytes.length ) {
	    rv   = (int) this.osBytes[ addr ] & 0xFF;
	    done = true;
	  }
	}
	if( !done && !this.scchMode
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
	if( idx < this.ramVideo.length ) {
	  rv = (int) this.ramVideo[ idx ] & 0xFF;
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
  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int idx = (this.mode64x16 ? 0x03FF : 0x07FF) - (chY * 64) - chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ];
      if( (b >= 0x20) && (b < 0x7F) ) {
	ch = b;
	if( this.fontBytes == fontCCD ) {

	  // Umlaute im Zeichensatz des Computerclubs Dessau
	  switch( b ) {
	    case 0x16:		// Paragraf-Zeichen
	      ch = '\u00A7';
	      break;

	    case 0x17:		// Ae-Umlaut
	      ch = '\u00C4';
	      break;

	    case 0x18:		// Oe-Umlaut
	      ch = '\u00D6';
	      break;

	    case 0x19:		// Ue-Umlaut
	      ch = '\u00DC';
	      break;

	    case 0x1A:		// ae-Umlaut
	      ch = '\u00E4';
	      break;

	    case 0x1B:		// oe-Umlaut
	      ch = '\u00F6';
	      break;

	    case 0x1C:		// ue-Umlaut
	      ch = '\u00FC';
	      break;

	    case 0x1D:		// sz
	      ch = '\u00DF';
	      break;
	  }
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
  public int getSupportedJoystickCount()
  {
    return 1;
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
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    int     ch = 0;
    switch( keyCode ) {
      case KeyEvent.VK_F1:
	this.screenInverseMode = !this.screenInverseMode;
	this.screenFrm.setScreenDirty( true );
	rv = true;
	break;

      case KeyEvent.VK_F2:
	if( this.scchMode ) {
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
	ch = ((this.scchMode && !this.graphicKeyState) ? 0x7F : 8);
	break;

      case KeyEvent.VK_DELETE:
	ch = 0x7F;
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
    if( (preIdx < 0) && this.scchMode ) {
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
					0x60B7,
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
	      if( this.scchMode ) {
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
		boolean pixel = ((this.fontBytes[ fIdx ] & m) != 0);
		if( inverse != this.screenInverseMode ) {
		  pixel = !pixel;
		}
		int curColorIdx = (pixel ? 1 : 0);
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
    if( (this.gide != null) && ((port & 0xF0) == this.gideIOAddr) ) {
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
	    if( !this.keyboardUsed && !this.joystickSelected ) {
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
	    rv = this.fdc.readMainStatusReg();
	  }
	  break;

	case 0x41:
	  if( this.fdc != null ) {
	    rv = this.fdc.readData();
	  }
	  break;
      }
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
      initSRAM( this.ramStatic, props );
      fillRandom( this.ramVideo );
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
    this.screenInverseMode = false;
    this.pio1B3State       = false;
    this.fdcWaitEnabled    = false;
    this.keyboardUsed      = false;
    this.joystickSelected  = false;
    this.graphicKeyState   = false;
    this.lowerDRAMEnabled  = false;
    this.lowerDRAMEnabled  = false;
    this.romEnabled        = true;
    this.prgXEnabled       = false;
    this.basicEnabled      = false;
    this.rf32KActive       = false;
    this.rf32NegA15        = false;
    this.rfReadEnabled     = false;
    this.rfWriteEnabled    = false;
    this.rfAddr16to19      = 0;
    this.joystickValue     = 0;
    this.keyboardValue     = 0;
    this.fontOffs          = 0;
    this.v24BitOut         = true;	// V24: H-Pegel
    this.v24BitNum         = 0;
    this.v24ShiftBuf       = 0;
    this.v24TStateCounter  = 0;
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
    if( (preIdx < 0) && this.scchMode ) {
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
	begAddr = 0x60F7;
	endAddr = SourceUtil.getKCBasicStyleEndAddr( this.emuThread, begAddr );
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
    if( this.scchMode ) {
      if( joyNum == 0 ) {
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
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( this.scchMode ) {
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
	  if( (idx & 0x8000) != 0 ) {
	    idx &= 0xF7FFF;
	  } else {
	    idx |= 0x8000;
	  }
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
	if( !this.romEnabled ) {
	  this.emuThread.setRAMByte( addr, value );
	  rv = true;
	}
      }
      else if( (addr >= 0x1000) && (addr < 0x1800) ) {
	int idx = addr - 0x1000;
	if( idx < this.ramVideo.length ) {
	  this.ramVideo[ idx ] = (byte) value;
	  this.screenFrm.setScreenDirty( true );
	  rv = true;
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
    return (this.fontBytes != fontU402)
		&& (this.fontBytes != fontCCD)
		&& (this.fontBytes != fontSCCH)
		&& (this.fontBytes != font2010);
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
    if( (this.gide != null) && ((port & 0xF0) == this.gideIOAddr) ) {
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
	    if( this.scchMode ) {
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
	    if( this.fontSwitchable || this.inverseSwitchable ) {
	      boolean state = ((v & 0x08) != 0);
	      if( state != this.pio1B3State ) {
		this.pio1B3State = state;
		if( this.fontSwitchable ) {
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
		}
		if( this.inverseSwitchable ) {
		  this.screenInverseMode = !this.screenInverseMode;
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
		this.v24TStateCounter = 3 * V24_TSTATES_PER_BIT / 2;
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

	case 0x14:
	  if( this.scchMode ) {
	    this.basicEnabled     = ((value & 0x02) != 0);
	    this.lowerDRAMEnabled = ((value & 0x04) != 0);
	    this.romdiskEnabled   = ((value & 0x08) != 0);
	    if( this.romdiskEnabled ) {
	      int bank = (value & 0x01) | ((value >> 3) & 0x0E);
	      if( this.romdiskBegAddr == 0x8000 ) {
		this.romdiskBankAddr = (bank << 15);
	      } else {
		this.romdiskBankAddr = (bank << 14);
	      }
	      this.prgXEnabled = false;
	    } else {
	      this.prgXEnabled = ((value & 0x01) != 0);
	    }
	  }
	  break;

	case 0x15:
	  if( this.scchMode ) {
	    this.rfAddr16to19   = (value << 16) & 0xF0000;
	    this.rf32NegA15     = ((value & 0x10) != 0);
	    this.rf32KActive    = ((value & 0x20) != 0);
	    this.rfReadEnabled  = ((value & 0x40) != 0);
	    this.rfWriteEnabled = ((value & 0x80) != 0);
	  }
	  break;

	case 0x16:
	  if( this.scchMode ) {
	    this.romEnabled = false;
	  }
	  break;

	case 0x17:

	  if( this.scchMode ) {
	    this.romEnabled = true;
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
	  }
	  break;

	case 0x42:
	case 0x43:
	  if( this.fdc != null ) {
	    if( this.fdcWaitEnabled ) {
	      this.fdc.processTillNextIORequest();
	    }
	  }
	  break;

	case 0x44:
	case 0x45:
	  if( this.fdc != null ) {
	    this.fdcWaitEnabled = ((value & 0x02) != 0);
	    if( (value & 0x10) != 0 ) {
	      this.fdc.fireTC();
	    }
	  }
	  break;

	case 0x48:
	  if( this.fdc != null ) {
	    this.fdc.fireTC();
	  }
	  break;
      }
    }
  }


	/* --- private Methoden --- */

  private static boolean emulatesFloppyDisk( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			"jkcemu.ac1.floppydisk.enabled",
			false );
  }


  private static int getROMDiskBegAddr( Properties props )
  {
    int rv = 0xC000;
    if( props != null ) {
      String text = EmuUtil.getProperty(
			props,
			"jkcemu.ac1.romdisk.address.begin" );
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
    this.fontBytes = readFontByProperty(
				props,
				"jkcemu.ac1.font.file",
				this.fontSwitchable ? 0x1000 : 0x0800 );
    if( this.fontBytes == null ) {
      if( this.scchMode ) {
	if( EmuUtil.getProperty(
			props,
			"jkcemu.ac1.os.version" ).equals( "SCCH8.0_2010" ) )
	{
	  if( font2010 == null ) {
	    font2010 = readResource( "/rom/ac1/font2010.bin" );
	  }
	  this.fontBytes = font2010;
	} else {
	  if(fontSCCH == null ) {
	    fontSCCH = readResource( "/rom/ac1/scchfont.bin" );
	  }
	  this.fontBytes = fontSCCH;
	  if( (fontSCCH != null) && this.fontSwitchable ) {
	    if( fontCCD == null ) {
	      fontCCD = readResource( "/rom/ac1/ccdfont.bin" );
	    }
	    if( fontCCD != null ) {
	      /*
	       * Der SCCH-Zeichensatz soll aktiv sein,
	       * wenn aun PIO1 B3 eine 1 ausgegeben wird.
	       * Demzufolge muss der SCCH-Zeichensatz in den hinteren
	       * 2 KByte liegen
	       */
	      byte[] a = new byte[ 0x1000 ];
	      Arrays.fill( a, (byte) 0 );
	      System.arraycopy(
			fontCCD,
			0,
			a,
			0,
			Math.min( fontCCD.length, 0x0800 ) );
	      System.arraycopy(
			fontSCCH,
			0,
			a,
			0x0800,
			Math.min( fontSCCH.length, 0x0800 ) );
	      this.fontBytes = a;
	    }
	  }
	}
      } else {
	if( this.mode64x16 ) {
	  if( fontU402 == null ) {
	    fontU402 = readResource( "/rom/ac1/u402bm513x4.bin" );
	  }
	  this.fontBytes = fontU402;
	} else {
	  if( fontCCD == null ) {
	    fontCCD = readResource( "/rom/ac1/ccdfont.bin" );
	  }
	  this.fontBytes = fontCCD;
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
      if( this.scchMode ) {
	if( this.osVersion.startsWith( "SCCH8.0" ) ) {
	  if( scchOS80 == null ) {
	    scchOS80 = readResource( "/rom/ac1/scchmon_80g.bin" );
	  }
	  this.osBytes = scchOS80;
	} else {
	  if( scchOS1088 == null ) {
	    scchOS1088 = readResource( "/rom/ac1/scchmon_1088g.bin" );
	  }
	  this.osBytes = scchOS1088;
	}
      } else {
	if( this.mode64x16 ) {
	  if( os31_64x16 == null ) {
	    os31_64x16 = readResource( "/rom/ac1/mon_31_64x16.bin" );
	  }
	  this.osBytes = os31_64x16;
	} else {
	  if( os31_64x32 == null ) {
	    os31_64x32 = readResource( "/rom/ac1/mon_31_64x32.bin" );
	  }
	  this.osBytes = os31_64x32;
	}
      }
    }

    // Mini-BASIC
    if( !this.scchMode ) {
      if( minibasic == null ) {
	minibasic = readResource( "/rom/ac1/minibasic.bin" );
      }
    }

    // BASIC-ROM
    if( this.scchMode ) {
      this.basicFile = EmuUtil.getProperty(
					props,
					"jkcemu.ac1.basic.file" );
      this.basicBytes = readFile( this.basicFile, 0x2000, "BASIC" );
      if( this.basicBytes == null ) {
	if( gsbasic == null ) {
	  gsbasic = readResource( "/rom/ac1/gsbasic.bin" );
	}
	this.basicBytes = gsbasic;
      }
    } else {
      this.basicFile  = null;
      this.basicBytes = null;
    }

    // Programmpaket X
    if( this.scchMode ) {
      this.prgXFile = EmuUtil.getProperty(
				props,
				"jkcemu.ac1.program_x.file" );
      this.prgXBytes = readFile( this.prgXFile, 0x2000, "Programmpaket X" );
    } else {
      this.prgXFile  = null;
      this.prgXBytes = null;
    }

    // ROM-Disk
    if( this.scchMode ) {
      this.romdiskFile = EmuUtil.getProperty(
				props,
				"jkcemu.ac1.romdisk.file" );
      this.romdiskBytes = readFile( this.romdiskFile, 0x40000, "ROM-Disk" );
    } else {
      this.romdiskFile  = null;
      this.romdiskBytes = null;
    }

    // Zeichensatz
    loadFont( props );
  }


  private void setKeyboardValue( int value )
  {
    this.keyboardValue = value;
    synchronized( this.pio1 ) {
      if( !this.joystickSelected ) {
	this.pio1.putInValuePortA( this.keyboardValue, 0xFF );
      }
    }
  }


  private void updGideIOAddr( Properties props )
  {
    int ioAddr = -1;
    try {
      String s = EmuUtil.getProperty( props, "jkcemu.ac1.gide.io_address" );
      if( s != null ) {
	s = s.trim().toUpperCase();
	int len = s.length();
	if( (len > 1) && s.endsWith( "H" ) ) {
	  s = s.substring( 0, len - 1 );
	}
	ioAddr = Integer.parseInt( s, 16 );
      }
    }
    catch( NumberFormatException ex ) {}
    this.gideIOAddr = ioAddr;
  }
}
