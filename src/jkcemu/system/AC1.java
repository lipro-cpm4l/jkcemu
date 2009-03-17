/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des AC1
 */

package jkcemu.system;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Properties;
import javax.swing.JOptionPane;
import jkcemu.base.*;
import z80emu.*;


public class AC1 extends EmuSys implements Z80CTCListener
{
  /*
   * Diese Tabelle mappt die Tokens des AC1-BASIC-Interpreters
   * in ihre entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   */
  private static final String[] ac1Tokens = {
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
   * Diese Tabelle mappt die Tokens des SCCH-BASIC-Interpreters
   * in ihre entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0x80".
   */
  private static final String[] scchTokens = {
	"END",      "FOR",    "NEXT",     "DATA",	// 0x80
	"INPUT",    "DIM",    "READ",     "LET",
	"GOTO",     "RUN",    "IF",       "RESTORE",
	"GOSUB",    "RETURN", "REM",      "STOP",
	"OUT",      "ON",     "NULL",     "WAIT",	// 0x90
	"DEF",      "POKE",   "DOKE",     "AUTO",
	"LINES",    "CLS",    "WIDTH",    "BYE",
	"KEY",      "CALL",   "PRINT",    "CONT",
	"LIST",     "CLEAR",  "CLOAD",    "CSAVE",	// 0xA0
	"NEW",      "TAB(",   "TO",       "FN",
	"SPC(",     "THEN",   "NOT",      "STEP",
	"+",        "-",      "*",        "/",
	"^",        "AND",    "OR",       ">",		// 0xB0
	"=",        "<",      "SGN",      "INT",
	"ABS",      "USR",    "FRE",      "INP",
	"POS",      "SQR",    "RND",      "LN",
	"EXP",      "COS",    "SIN",      "TAN",	// 0xC0
	"ATN",      "PEEK",   "DEEK",     "POINT",
	"LEN",      "STR$",   "VAL",      "ASC",
	"CHR$",     "LEFT$",  "RIGHT$",   "MID$",
	"SET",      "RESET",  "RENUMBER", "LOCATE",	// 0xD0
	"SOUND",    "INKEY",  "MODE",     "TRON",
	"TROFF" };

  private static byte[] mon31_64x16   = null;
  private static byte[] mon31_64x32   = null;
  private static byte[] scchMon80     = null;
  private static byte[] scchMon1088   = null;
  private static byte[] minibasic     = null;
  private static byte[] gsbasic       = null;
  private static byte[] ccdFontBytes  = null;
  private static byte[] scchFontBytes = null;
  private static byte[] u402FontBytes = null;

  private byte[]  ramStatic;
  private byte[]  ramVideo;
  private byte[]  romMon;
  private byte[]  romBASIC;
  private byte[]  fontBytes;
  private Z80CTC  ctc;
  private Z80PIO  pio;
  private boolean mode64x16;
  private boolean lowerDRAMEnabled;
  private boolean gsbasicEnabled;
  private boolean keyboardUsed;


  public AC1( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    this.romMon           = null;
    this.romBASIC         = null;
    this.fontBytes        = null;
    this.mode64x16        = false;
    this.lowerDRAMEnabled = false;
    this.gsbasicEnabled   = false;

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
    } else {
      this.ramStatic = new byte[ 0x0800 ];
      this.ramVideo  = new byte[ 0x0800 ];
      if( this.gsbasic == null ) {
	gsbasic = readResource( "/rom/ac1/gsbasic.bin" );
      }
    }

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( cpu );
    this.pio   = new Z80PIO( cpu );
    cpu.setInterruptSources( this.ctc, this.pio );

    this.ctc.addCTCListener( this );
    cpu.addTStatesListener( this.ctc );

    this.keyboardUsed = false;
    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static String getBasicProgram( Component owner, Z80MemView memory )
  {
    String rv = null;
    String s1 = getBasicProgram( memory, 0x60F7, ac1Tokens );
    String s2 = getBasicProgram( memory, 0x60F7, scchTokens );
    if( (s1 != null) && (s2 != null) ) {
      if( s1.equals( s2 ) ) {
	rv = s1;
      } else {
	String[]    options = { "AC1-BASIC", "SCCH-BASIC", "Abbrechen" };
	JOptionPane pane    = new JOptionPane(
		"Wurde das BASIC-Programm mit dem originalen"
			+ " AC1-BASIC-Interpreter\n"
			+ "oder mit dem Grafik/Sound-BASIC-Interpreter"
			+ " von SCCH erstellt?",
		JOptionPane.QUESTION_MESSAGE );
	pane.setOptions( options );
	pane.setWantsInput( false );
	Object value = pane.getValue();
	if( value != null ) {
	  if( value.equals( options[ 0 ] ) ) {
	    rv = s1;
	  }
	  else if( value.equals( options[ 1 ] ) ) {
	    rv = s2;
	  }
	}
      }
    } else {
      if( s1 != null ) {
	rv = s1;
      }
      else if( s2 != null ) {
	rv = s2;
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
				memory.getMemWord( 0x18E9 ) + 1 );
  }


	/* --- Z80CTCListener --- */

  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    // Verbindung von Ausgang 0 auf Eingang 1 emulieren
    if( (ctc == this.ctc) && (timerNum == 0) )
      ctc.externalUpdate( 1, 1 );
  }


	/* --- ueberschriebene Methoden --- */

  public boolean canExtractScreenText()
  {
    return true;
  }


  public void die()
  {
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this.ctc );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  public int getAppStartStackInitValue()
  {
    return 0x2000;
  }


  public int getColorIndex( int x, int y )
  {
    int    rv        = BLACK;
    byte[] fontBytes = this.emuThread.getExtFontBytes();
    if( fontBytes == null ) {
      fontBytes = this.fontBytes;
    }
    if( fontBytes != null ) {
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
	  int fIdx = (((int) this.ramVideo[ vIdx ] & 0xFF) * 8) + rPix;
	  if( (fIdx >= 0) && (fIdx < fontBytes.length ) ) {
	    int m = 1;
	    int n = x % 6;
	    if( n > 0 ) {
	      m <<= n;
	    }
	    if( (fontBytes[ fIdx ] & m) != 0 ) {
	      rv = WHITE;
	    }
	  }
	}
      }
    }
    return rv;
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


  public int getMemByte( int addr )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( !this.lowerDRAMEnabled ) {
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
	    rv   = (int) this.ramVideo[ idx ] & 0xFF;
	    done = true;
	  }
	}
	else if( (addr >= 0x1800) && (addr < 0x2000) ) {
	  int idx = addr - 0x1800;
	  if( idx < this.ramStatic.length ) {
	    rv   = (int) this.ramStatic[ idx ] & 0xFF;
	    done = true;
	  }
	}
      }
    }
    if( !done && this.gsbasicEnabled
	&& (addr >= 0x4000) && (addr < 0x6000)
	&& (gsbasic != null) )
    {
      int idx = addr - 0x4000;
      if( idx < gsbasic.length ) {
	rv   = (int) gsbasic[ idx ] & 0xFF;
	done = true;
      }
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
      ch = (int) this.ramVideo[ idx ];
      if( this.fontBytes == ccdFontBytes ) {

	// Umlaute im Zeichensatz des Computerclubs Dessau
	switch( ch ) {
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
    return !this.lowerDRAMEnabled || (addr >= 0x2000);
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
    String[] tokens = scchTokens;
    if( (this.romMon == this.mon31_64x16)
	|| (this.romMon == this.mon31_64x32) )
    {
      tokens = ac1Tokens;
    }
    String text = getBasicProgram(
				this.emuThread,
				0x60F7,
				ac1Tokens );
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoAC1Basic();
    }
  }


  public void openTinyBasicProgram()
  {
    if( this.romBASIC != null ) {
      String text = getTinyBasicProgram( this.emuThread );
      if( text != null ) {
	this.screenFrm.openText( text );
      } else {
	showNoMiniBasic();
      }
    } else {
      super.openTinyBasicProgram();
    }
  }


  public int reassembleSysCall(
			int           addr,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    int rv  = 0;
    int bol = buf.length();
    int b   = this.emuThread.getMemByte( addr );
    switch( b ) {
      case 0xCF:
	buf.append( String.format( "%04X  %02X", addr, b ) );
	appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "RST" );
	appendSpacesToCol( buf, bol, colArgs );
	buf.append( "08H" );
	appendSpacesToCol( buf, bol, colRemark );
	buf.append( ";INCH\n" );
	rv = 1;
	break;

      case 0xD7:
	buf.append( String.format( "%04X  %02X", addr, b ) );
	appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "RST" );
	appendSpacesToCol( buf, bol, colArgs );
	buf.append( "10H" );
	appendSpacesToCol( buf, bol, colRemark );
	buf.append( ";OUTCH\n" );
	rv = 1;
	break;

      case 0xDF:
	buf.append( String.format( "%04X  %02X", addr, b ) );
	appendSpacesToCol( buf, bol, colMnemonic );
	buf.append( "RST" );
	appendSpacesToCol( buf, bol, colArgs );
	buf.append( "18H" );
	appendSpacesToCol( buf, bol, colRemark );
	buf.append( ";OUTS\n" );
	rv = 1 + reassStringBit7( addr + 1, buf, colMnemonic, colArgs );
	break;

      case 0xC3:
      case 0xCD:
	int w = getMemWord( addr + 1 );
	switch( w ) {
	  case 0x0008:
	  case 0x1802:
	    buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	    appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( "CALL" );
	    appendSpacesToCol( buf, bol, colArgs );
	    buf.append( String.format( "%04XH", w ) );
	    appendSpacesToCol( buf, bol, colRemark );
	    buf.append( ";INCH\n" );
	    rv = 3;
	    break;

	  case 0x0010:
	  case 0x1805:
	    buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	    appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( "CALL" );
	    appendSpacesToCol( buf, bol, colArgs );
	    buf.append( String.format( "%04XH", w ) );
	    appendSpacesToCol( buf, bol, colRemark );
	    buf.append( ";OUTCH\n" );
	    rv = 3;
	    break;

	  case 0x0018:
	  case 0x1808:
	    buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w >> 8,
				w & 0xFF ) );
	    appendSpacesToCol( buf, bol, colMnemonic );
	    buf.append( "CALL" );
	    appendSpacesToCol( buf, bol, colArgs );
	    buf.append( String.format( "%04XH", w ) );
	    appendSpacesToCol( buf, bol, colRemark );
	    buf.append( ";OUTS\n" );
	    rv = 3 + reassStringBit7( addr + 3, buf, colMnemonic, colArgs );
	    break;
	}
	break;
    }
    return rv;
  }


  public int readIOByte( int port )
  {
    int rv = 0xFF;
    if( (port & 0xF8) == 0xE0 ) {
      if( !this.mode64x16 ) {
	rv = this.emuThread.getRAMFloppyA().readByte( port & 0x07 );
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
    return rv;
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      fillRandom( this.ramStatic );
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
    this.keyboardUsed = false;
  }


  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getKCStyleBasicEndAddr( this.emuThread, 0x60F7 );
    if( endAddr >= 0x60F7 ) {
      (new SaveDlg(
		this.screenFrm,
		0x60F7,
		endAddr,
		'B',
		false,          // kein KC-BASIC
		"AC1-BASIC-Programm speichern" )).setVisible( true );
    } else {
      showNoAC1Basic();
    }
  }


  public void saveTinyBasicProgram()
  {
    if( this.romBASIC != null ) {
      int endAddr = this.emuThread.getMemWord( 0x18E9 ) + 1;
      if( endAddr > 0x1950 ) {
	(new SaveDlg(
		this.screenFrm,
		0x18C0,
		endAddr,
		'b',
		false,          // kein KC-BASIC
                "AC1-MiniBASIC-Programm speichern" )).setVisible( true );
      } else {
	showNoMiniBasic();
      }
    } else {
      super.saveTinyBasicProgram();
    }
  }


  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv   = false;
    boolean done = false;
    if( !this.lowerDRAMEnabled ) {
      if( this.romMon != null ) {
	if( addr < this.romMon.length ) {
	  done = true;
	}
      }
      if( !done && (addr >= 0x0800) && (this.romBASIC != null) ) {
	if( addr - 0x800 < this.romBASIC.length ) {
	  done = true;
	}
      }
      if( !done ) {
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
      }
    }
    if( !done && this.gsbasicEnabled && (addr >= 0x4000) && (addr < 0x6000) ) {
      done = true;
    }
    if( !this.mode64x16 && !done ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  public boolean supportsRAMFloppyA()
  {
    return true;
  }


  public void updSysCells(
			int    begAddr,
			int    len,
			Object fileFmt,
			int    fileType )
  {
    if( (begAddr == 0x60F7) && (fileFmt != null) ) {
      if( fileFmt.equals( FileInfo.HEADERSAVE ) ) {
	if( fileType == 'B' ) {
	  int topAddr = begAddr + len;
	  this.emuThread.setMemWord( 0x60D2, topAddr );
	  this.emuThread.setMemWord( 0x60D4, topAddr );
	  this.emuThread.setMemWord( 0x60D6, topAddr );
	}
      }
    }
  }


  public void writeIOByte( int port, int value )
  {
    if( (port & 0xF8) == 0xE0 ) {
      if( !this.mode64x16 ) {
	this.emuThread.getRAMFloppyA().writeByte( port & 0x07, value );
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
	  if( !this.mode64x16 ) {
	    this.gsbasicEnabled   = ((value & 0x02) != 0);
	    this.lowerDRAMEnabled = ((value & 0x04) != 0);
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
      }
    }
  }


	/* --- private Methoden --- */

  private static String getBasicProgram(
				Z80MemView memory,
				int        addr,
				String[]   tokens )
  {
    StringBuilder buf = new StringBuilder( 0x4000 );

    int nextLineAddr = memory.getMemWord( addr );
    while( (nextLineAddr > addr + 5)
	   && (memory.getMemByte( nextLineAddr - 1 ) == 0) )
    {
      // Zeilennummer
      addr += 2;
      buf.append( memory.getMemWord( addr ) );
      addr += 2;

      // Anzahl Leerzeichen vor der Anweisung ermitteln
      boolean sep = true;
      int     n   = 0;
      while( addr < nextLineAddr ) {
	int ch = memory.getMemByte( addr );
	if( ch == '\u0020' ) {
	  n++;
	  addr++;
	} else {
	  if( (ch != 0) && (n > 0) ) {
	    for( int i = 0; i <= n; i++ ) {
	      buf.append( (char) '\u0020' );
	    }
	    sep = false;
	  }
	  break;
	}
      }

      // Programmzeile extrahieren
      while( addr < nextLineAddr ) {
	int ch = memory.getMemByte( addr++ );
	if( ch == 0 ) {
	  break;
	}
	if( ch == '\"' ) {
	  if( sep ) {
	    buf.append( (char) '\u0020' );
	  }
	  buf.append( (char) ch );
	  while( addr < nextLineAddr ) {
	    ch = memory.getMemByte( addr++ );
	    if( ch == 0 ) {
	      break;
	    }
	    buf.append( (char) ch );
	    if( ch == '\"' ) {
	      break;
	    }
	  }
	} else {
	  if( ch >= 0x80 ) {
	    int pos = ch - 0x80;
	    if( (pos >= 0) && (pos < tokens.length) ) {
	      String s = tokens[ pos ];
	      if( s != null ) {
		int len = s.length();
		if( len > 0 ) {
		  if( isIdentifierChar( buf.charAt( buf.length() - 1 ) )
		      && isIdentifierChar( s.charAt( 0 ) ) )
		  {
		    buf.append( (char) '\u0020' );
		  }
		  buf.append( s );
		  if( isIdentifierChar( s.charAt( len - 1 ) ) ) {
		    sep = true;
		  } else {
		    sep = false;
		  }
		}
		ch = 0;
	      }
	    }
	  }
	  if( ch > 0 ) {
	    if( sep
		&& (isIdentifierChar( ch )
			|| (ch == '\'')
			|| (ch == '\"')) )
	    {
	      buf.append( (char) '\u0020' );
	    }
	    buf.append( (char) ch );
	    sep = false;
	  }
	}
      }
      buf.append( (char) '\n' );

      // naechste Zeile
      addr         = nextLineAddr;
      nextLineAddr = memory.getMemWord( addr );
    }
    return buf.length() > 0 ? buf.toString() : null;
  }


  private static boolean isIdentifierChar( int ch )
  {
    return ((ch >= 'A') && (ch <= 'Z'))
		|| ((ch >= 'a') && (ch <= 'z'))
		|| ((ch >= '0') && (ch <= '9'));
  }


  private void showNoAC1Basic()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Es ist kein AC1-BASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }


  private void showNoMiniBasic()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Es ist kein AC1-MiniBASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }
}

