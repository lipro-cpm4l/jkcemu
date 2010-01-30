/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des AC1
 */

package jkcemu.system;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.JOptionPane;
import jkcemu.base.*;
import jkcemu.disk.*;
import z80emu.*;


public class AC1 extends EmuSys implements
					FDC8272.DriveSelector,
					Z80CTCListener,
					Z80TStatesListener
{
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

  private static byte[] mon31_64x16   = null;
  private static byte[] mon31_64x32   = null;
  private static byte[] scchMon80     = null;
  private static byte[] scchMon1088   = null;
  private static byte[] minibasic     = null;
  private static byte[] gsbasic       = null;
  private static byte[] ccdFontBytes  = null;
  private static byte[] scchFontBytes = null;
  private static byte[] u402FontBytes = null;

  private byte[]            ramStatic;
  private byte[]            ramVideo;
  private byte[]            ramExtended;
  private byte[]            romMon;
  private byte[]            romBASIC;
  private byte[]            fontBytes;
  private byte[]            prgXBytes;
  private byte[]            romdiskBytes;
  private String            prgXFileName;
  private String            romdiskFileName;
  private BasicType         lastBasicType;
  private Z80CTC            ctc;
  private Z80PIO            pio;
  private FDC8272           fdc;
  private FloppyDiskDrive[] fdDrives;
  private boolean           fdcWaitEnabled;
  private boolean           mode64x16;
  private boolean           scchMode;
  private boolean           keyboardUsed;
  private boolean           lowerDRAMEnabled;
  private boolean           romdiskEnabled;
  private boolean           prgXEnabled;
  private boolean           gsbasicEnabled;
  private boolean           rf32KActive;
  private boolean           rf32NegA15;
  private boolean           rfReadEnabled;
  private boolean           rfWriteEnabled;
  private int               rfAddr16to19;
  private int               romdiskBegAddr;
  private int               romdiskBankAddr;


  public AC1( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.ramExtended     = null;
    this.romMon          = null;
    this.romBASIC        = null;
    this.fontBytes       = null;
    this.prgXBytes       = null;
    this.romdiskBytes    = null;
    this.prgXFileName    = null;
    this.romdiskFileName = null;
    this.lastBasicType   = null;
    this.mode64x16       = false;
    this.scchMode        = false;
    this.romdiskBegAddr  = getROMDiskBegAddr( props );
    this.romdiskBankAddr = 0;

    String mon = EmuUtil.getProperty( props, "jkcemu.ac1.monitor" );
    if( mon.startsWith( "SCCH" ) ) {
      if( scchFontBytes == null ) {
	scchFontBytes = readResource( "/rom/ac1/scchfont.bin" );
      }
      this.fontBytes = scchFontBytes;
      if( mon.equals( "SCCH8.0" ) ) {
	if( scchMon80 == null ) {
	  scchMon80 = readResource( "/rom/ac1/scchmon_80.bin" );
	}
	this.romMon = scchMon80;
      } else {
	if( scchMon1088 == null ) {
	  scchMon1088 = readResource( "/rom/ac1/scchmon_1088.bin" );
	}
	this.romMon = scchMon1088;
      }
      this.ramExtended = this.emuThread.getExtendedRAM( 0x100000 );
      this.scchMode    = true;
      lazyReadPrgX( props );
      lazyReadRomdisk( props );
    } else {
      if( minibasic == null ) {
	minibasic = readResource( "/rom/ac1/minibasic.bin" );
      }
      this.romBASIC = minibasic;
      if( mon.startsWith( "3.1_64x16" ) ) {
	if( u402FontBytes == null ) {
	  u402FontBytes = readResource( "/rom/ac1/u402bm513x4.bin" );
	}
	this.fontBytes = u402FontBytes;
	if( mon31_64x16 == null ) {
	  mon31_64x16 = readResource( "/rom/ac1/mon_31_64x16.bin" );
	}
	this.romMon    = mon31_64x16;
	this.mode64x16 = true;
      } else {
	if( ccdFontBytes == null ) {
	  ccdFontBytes = readResource( "/rom/ac1/ccdfont.bin" );
	}
	this.fontBytes = ccdFontBytes;
	if( mon31_64x32 == null ) {
	  mon31_64x32 = readResource( "/rom/ac1/mon_31_64x32.bin" );
	}
	this.romMon = mon31_64x32;
      }
    }
    if( this.mode64x16 ) {
      this.ramStatic = new byte[ 0x0400 ];
      this.ramVideo  = new byte[ 0x0400 ];
      this.fdDrives  = null;
      this.fdc       = null;
    } else {
      this.ramStatic = new byte[ 0x0800 ];
      this.ramVideo  = new byte[ 0x0800 ];
      if( gsbasic == null ) {
	gsbasic = readResource( "/rom/ac1/gsbasic.bin" );
      }
      this.fdDrives = new FloppyDiskDrive[ 4 ];
      Arrays.fill( this.fdDrives, null );
      this.fdc = new FDC8272( this, 4 );
    }

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu );
    this.pio   = new Z80PIO( cpu );
    cpu.setInterruptSources( this.ctc, this.pio );

    this.ctc.addCTCListener( this );
    cpu.addTStatesListener( this );
    if( this.fdc != null ) {
      cpu.addMaxSpeedListener( this.fdc );
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

      String scch    = AC1LLC2Util.getSCCHBasicProgram( loadData );
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


	/* --- Z80CTCListener --- */

  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    // Verbindung von Ausgang 0 auf Eingang 1 emulieren
    if( (ctc == this.ctc) && (timerNum == 0) )
      ctc.externalUpdate( 1, 1 );
  }


	/* --- Z80TStatesListener --- */

  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    this.ctc.systemUpdate( tStates );
    if( this.fdc != null ) {
      this.fdc.z80TStatesProcessed( cpu, tStates );
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void applySettings( Properties props )
  {
    super.applySettings( props );
    lazyReadPrgX( props );
    lazyReadRomdisk( props );
  }


  public boolean canExtractScreenText()
  {
    return true;
  }


  public void die()
  {
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
    if( this.fdc != null ) {
      cpu.removeMaxSpeedListener( this.fdc );
    }
  }


  public int getAppStartStackInitValue()
  {
    return 0x2000;
  }


  public int getCharColCount()
  {
    return 64;
  }


  public int getCharHeight()
  {
    return 8;
  }


  public int getCharRowCount()
  {
    return this.mode64x16 ? 16 : 32;
  }


  public int getCharRowHeight()
  {
    return this.mode64x16 ? 16 : 8;
  }


  public int getCharWidth()
  {
    return 6;
  }


  public long getDelayMillisAfterPasteChar()
  {
    return 50;
  }


  public long getDelayMillisAfterPasteEnter()
  {
    return 150;
  }


  public long getHoldMillisPasteChar()
  {
    return 50;
  }


  public String getHelpPage()
  {
    return "/help/ac1.htm";
  }


  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( this.scchMode ) {
      if( !m1 && this.rfReadEnabled ) {
	if( this.ramExtended != null ) {
	  int idx = this.rfAddr16to19 | addr;
	  if( idx < this.ramExtended.length ) {
	    rv = (int) this.ramExtended[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
      if( !done && this.rf32KActive && (addr >= 0x4000) && (addr < 0xC000) ) {
	if( this.ramExtended != null ) {
	  int idx = this.rfAddr16to19 | addr;
	  if( this.rf32NegA15 ) {
	    if( (idx & 0x8000) != 0 ) {
	      idx &= 0xF7FFF;
	    } else {
	      idx |= 0x8000;
	    }
	  }
	  if( idx < this.ramExtended.length ) {
	    rv = (byte) this.ramExtended[ idx ] & 0xFF;
	  }
	}
	done = true;
      }
      if( !done && this.gsbasicEnabled
	  && (addr >= 0x4000) && (addr < 0x6000) )
      {
	if( gsbasic != null ) {
	  int idx = addr - 0x4000;
	  if( idx < gsbasic.length ) {
	    rv = (int) gsbasic[ idx ] & 0xFF;
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
      if( this.romMon != null ) {
	if( addr < this.romMon.length ) {
	  rv   = (int) this.romMon[ addr ] & 0xFF;
	  done = true;
	}
      }
      if( !done && (addr >= 0x0800) && (this.romBASIC != null) ) {
	int idx = addr - 0x0800;
	if( idx < this.romBASIC.length ) {
	  rv   = (int) this.romBASIC[ idx ] & 0xFF;
	  done = true;
	}
      }
      if( !done ) {
	if( (addr >= 0x1000) && (addr < 0x1800) ) {
	  int idx = addr - 0x1000;
	  if( idx < this.ramVideo.length ) {
	    rv = (int) this.ramVideo[ idx ] & 0xFF;
	  }
	}
	else if( (addr >= 0x1800) && (addr < 0x2000) ) {
	  int idx = addr - 0x1800;
	  if( idx < this.ramStatic.length ) {
	    rv = (int) this.ramStatic[ idx ] & 0xFF;
	  }
	}
      }
      done = true;
    }
    if( !done && !this.mode64x16 ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  protected int getScreenChar( int chX, int chY )
  {
    int ch  = -1;
    int idx = (this.mode64x16 ? 0x03FF : 0x07FF) - (chY * 64) - chX;
    if( (idx >= 0) && (idx < this.ramVideo.length) ) {
      int b = (int) this.ramVideo[ idx ];
      if( (b >= 0x20) && (b < 0x7F) ) {
	ch = b;
	if( this.fontBytes == ccdFontBytes ) {

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


  public int getScreenHeight()
  {
    return this.mode64x16 ? 248 : 256;
  }


  public int getScreenWidth()
  {
    return 384;
  }


  public int getSupportedFloppyDiskDriveCount()
  {
    return this.fdDrives != null ? this.fdDrives.length : 0;
  }


  public int getSupportedRAMFloppyCount()
  {
    return this.mode64x16 ? 0 : 1;
  }


  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  public String getTitle()
  {
    return "AC1";
  }


  protected boolean isExtROMSwitchableAt( int addr )
  {
    boolean rv = false;
    if( !this.rfReadEnabled ) {
      rv = true;
      if( (addr < 0x2000) && this.lowerDRAMEnabled ) {
	rv = false;
      }
      if( rv && (addr >= this.romdiskBegAddr) && this.romdiskEnabled ) {
	rv = false;
      }
      if( rv && (addr >= 0xE000) && this.prgXEnabled ) {
	rv = false;
      }
    }
    return rv;
  }


  public boolean isISO646DE()
  {
    return this.fontBytes == this.scchFontBytes;
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
  {
    boolean rv = false;
    int     ch = 0;
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
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

      case KeyEvent.VK_DELETE:
	ch = 0x7F;
	break;
    }
    if( ch > 0 ) {
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      rv = true;
    }
    return rv;
  }


  public void keyReleased()
  {
    this.pio.putInValuePortA( 0, 0xFF );
  }


  public boolean keyTyped( char ch )
  {
    boolean rv = false;
    if( (ch > 0) && (ch < 0x7F) ) {
      this.pio.putInValuePortA( ch | 0x80, 0xFF );
      rv = true;
    }
    return rv;
  }


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
	text  = AC1LLC2Util.getSCCHBasicProgram( this.emuThread );
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


  public boolean paintScreen(
			Graphics g,
			int      xOffs,
			int      yOffs,
			int      screenScale )
  {
    byte[] fontBytes = this.emuThread.getExtFontBytes();
    if( fontBytes == null ) {
      fontBytes = this.fontBytes;
    }
    if( fontBytes != null ) {
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
	      int fIdx = (ch * 8) + rPix;
	      if( (fIdx >= 0) && (fIdx < fontBytes.length ) ) {
		int m = 0x01;
		int n = x % 6;
		if( n > 0 ) {
		  m <<= n;
		}
		boolean pixel = ((fontBytes[ fIdx ] & m) != 0);
		if( inverse ) {
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


  public int reassembleSysCall(
			int           addr,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    return AC1LLC2Util.reassembleSysCall(
				this.emuThread,
				addr,
				buf,
				colMnemonic,
				colArgs,
				colRemark );
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    if( (port & 0xF8) == 0xE0 ) {
      if( !this.mode64x16 ) {
	rv = this.emuThread.getRAMFloppy1().readByte( port & 0x07 );
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
	  if( !this.keyboardUsed ) {
	    this.pio.putInValuePortA( 0, 0xFF );
	    this.keyboardUsed = true;
	  }
	  rv = this.pio.readPortA();
	  break;

	case 5:
	  this.pio.putInValuePortB(
			this.emuThread.readAudioPhase() ? 0x80 : 0, 0x80 );
	  rv = this.pio.readPortB();
	  break;

	case 6:
	  rv = this.pio.readControlA();
	  break;

	case 7:
	  rv = this.pio.readControlB();
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


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System
   * oder das Monitorprogramm aendert
   */
  public boolean requiresReset( Properties props )
  {
    boolean rv =  !EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "AC1" );
    if( !rv ) {
      String mon = EmuUtil.getProperty( props, "jkcemu.ac1.monitor" );
      if( mon.equals( "3.1_64x16" ) ) {
	if( this.romMon != mon31_64x16 ) {
	  rv = true;
	}
      }
      else if( mon.equals( "SCCH8.0" ) ) {
	if( this.romMon != scchMon80 ) {
	  rv = true;
	}
      }
      else if( mon.equals( "SCCH10/88" ) ) {
	if( this.romMon != scchMon1088 ) {
	  rv = true;
	}
      } else {
	if( this.romMon != mon31_64x32 ) {
	  rv = true;
	}
      }
    }
    if( !rv && this.scchMode ) {
      if( this.romdiskBegAddr != getROMDiskBegAddr( props ) ) {
	rv = true;
      }
    }
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      initSRAM( this.ramStatic, props );
      fillRandom( this.ramVideo );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      this.ctc.reset( true );
      this.pio.reset( true );
    } else {
      this.ctc.reset( false );
      this.pio.reset( false );
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
    this.fdcWaitEnabled   = false;
    this.keyboardUsed     = false;
    this.lowerDRAMEnabled = false;
    this.romdiskEnabled   = false;
    this.prgXEnabled      = false;
    this.gsbasicEnabled   = false;
    this.rf32KActive      = false;
    this.rf32NegA15       = false;
    this.rfReadEnabled    = false;
    this.rfWriteEnabled   = false;
    this.rfAddr16to19     = 0;
  }


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
		"BASIC-Programm speichern" )).setVisible( true );
      } else {
	showNoBasic();
      }
    }
  }


  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    if( this.fdDrives != null ) {
      if( (idx >= 0) && (idx < this.fdDrives.length) ) {
	this.fdDrives[ idx ] = drive;
      }
    }
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( this.scchMode ) {
      if( this.rfWriteEnabled ) {
	if( this.ramExtended != null ) {
	  int idx = this.rfAddr16to19 | addr;
	  if( idx < this.ramExtended.length ) {
	    this.ramExtended[ idx ] = (byte) value;
	    rv = true;
	  }
	}
	done = true;
      }
      if( !done && this.rf32KActive && (addr >= 0x4000) && (addr < 0xC000) ) {
	if( this.ramExtended != null ) {
	  int idx = this.rfAddr16to19 | addr;
	  if( this.rf32NegA15 ) {
	    if( (idx & 0x8000) != 0 ) {
	      idx &= 0xF7FFF;
	    } else {
	      idx |= 0x8000;
	    }
	  }
	  if( idx < this.ramExtended.length ) {
	    this.ramExtended[ idx ] = (byte) value;
	    rv = true;
	  }
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
      if( (addr >= 0x1000) && (addr < 0x1800) ) {
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


  public void writeIOByte( int port, int value )
  {
    if( (port & 0xF8) == 0xE0 ) {
      if( !this.mode64x16 ) {
	this.emuThread.getRAMFloppy1().writeByte( port & 0x07, value );
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
	  this.pio.writePortA( value );
	  break;

	case 5:
	  this.pio.writePortB( value );
	  this.emuThread.writeAudioPhase(
		(this.pio.fetchOutValuePortB( false )
			& (this.emuThread.isLoudspeakerEmulationEnabled() ?
							0x01 : 0x40 )) != 0 );
	  break;

	case 6:
	  this.pio.writeControlA( value );
	  break;

	case 7:
	  this.pio.writeControlB( value );
	  break;

	case 0x14:
	  if( this.scchMode ) {
	    this.gsbasicEnabled   = ((value & 0x02) != 0);
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

  private void lazyReadPrgX( Properties props )
  {
    String fName = EmuUtil.getProperty( props, "jkcemu.program_x.file.name" );
    if( EmuUtil.differs( fName, this.prgXFileName ) ) {
      this.prgXFileName = fName;
      this.prgXBytes    = readFile( fName, 0x2000, "Programmpaket X" );
    }
  }


  private void lazyReadRomdisk( Properties props )
  {
    String fName = EmuUtil.getProperty( props, "jkcemu.romdisk.file.name" );
    if( EmuUtil.differs( fName, this.romdiskFileName ) ) {
      this.romdiskFileName = fName;
      this.romdiskBytes    = readFile(
				fName,
				this.romdiskBegAddr == 0x8000 ?
							0x80000
							: 0x40000,
				"ROM-Disk" );
    }
  }


  private static int getROMDiskBegAddr( Properties props )
  {
    int rv = 0xC000;
    if( props != null ) {
      String text = props.getProperty( "jkcemu.ac1.romdisk.address.begin" );
      if( text != null ) {
	if( text.trim().equals( "8000" ) ) {
	  rv = 0x8000;
	}
      }
    }
    return rv;
  }
}

