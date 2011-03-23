/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Huebler-Grafik-MC
 */

package jkcemu.emusys;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.emusys.huebler.AbstractHueblerMC;
import z80emu.*;


public class HueblerGraphicsMC extends AbstractHueblerMC
{
  private static final String[] basicTokens = {
	"END",      "FOR",       "NEXT",     "DATA",		// 0x80
	"PRINT #",  "INPUT #",   "INPUT",    "DIM",
	"READ",     "LET",       "GO TO",    "FNEND",
	"IF",       "RESTORE",   "GO SUB",   "RETURN",
	"REM",      "STOP",      "OUT",      "ON",		// 0x90
	"NULL",     "WAIT",      "DEF",      "POKE",
	"PRINT",    "CLEAR",     "FNRETURN", "SAVE",
	"!",        "ELSE",      "LPRINT",   "TRACE",
	"LTRACE",   "RANDOMIZE", "SWITCH",   "LWIDTH",		// 0xA0
	"LNULL",    "WIDTH",     "LVAR",     "LLVAR",
	"\'",       "PRECISION", "CALL",     "ERASE",
	"SWAP",     "LINE",      "RUN",      "LOAD",
	"NEW",      "AUTO",      "COPY",     "ALOAD",		// 0xB0
	"AMERGE",   "ASAVE",     "LIST",     "LLIST",
	"RENUMBER", "DELETE",    "EDIT",     "DEG",
	"RAD",      "WHILE",     "WEND",     "REPEAT",
	"UNTIL",    "ERROR",     "RESUME",   "OPEN",		// 0xC0
	"CLOSE",    "CLS",       "CURSOR",   "DRAW",
	"MOVE",     "PLOT",      "PEN",      "PAGE",
	"SYSTEM",   "CONT",      "USING",    "PI",
	"TAB(",     "TO",        "FN",       "SPC(",		// 0xD0
	"THEN",     "NOT",       "STEP",     "+",
	"-",        "*",         "/",        "DIV",
	"MOD",      "^",         "AND",      "OR",
	"XOR",      ">",         "=",        "<" };		// 0xE0

  private static final String[] basicTokensFF = {
	"SGN",      "INT",       "FIX",      "ABS",		// 0x80
	"USR",      "FRE",       "INP",      "POS",
	"LPOS",     "EOF",       "SQR",      "RND",
	"LOG",      "EXP",       "COS",      "SIN",
	"TAN",      "ATN",       "PEEK",     "FRAC",		// 0x90
	"LGT",      "SQU",       "BIN$",     "HEX$",
	"LEN",      "STR$",      "VAL",      "ASC",
	"SPACE$",   "CHR$",      "LEFT$",    "RIGHT$",
	"MID$",     "INKEY$",    "STRING$",  "ERR",		// 0xA0
	"ERL",      "POINT",     "INSTR" };

  private static final String[] sysCallNames = {
			"START", "CI",    "RI",   "CO",
			"POO",   "LO",    "CSTS", "IOBYTE",
			"IOSET", "MEMSI", "MAIN" };

  private static byte[] monBytes      = null;
  private static byte[] monBasicBytes = null;

  private boolean romEnabled;
  private boolean basic;
  private int     videoBaseAddr;


  public HueblerGraphicsMC( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    if( hasBasic( props ) ) {
      monBasicBytes = readResource( "/rom/huebler/mon30p_hbasic33p.bin" );
      this.basic    = true;
    } else {
      monBytes      = readResource( "/rom/huebler/mon30.bin" );
      this.basic    = false;
    }
    this.videoBaseAddr = 0;
    createIOSystem();
    checkAddPCListener( props );
    reset( EmuThread.ResetLevel.POWER_ON, props );
  }


  public static String getBasicProgram( Z80MemView memory )
  {
    StringBuilder buf = new StringBuilder( 0x4000 );

    int addr         = 0x3770;
    int nextLineAddr = memory.getMemWord( addr );
    while( (nextLineAddr > addr + 5)
	   && (memory.getMemByte( nextLineAddr - 1, false ) == 0) )
    {
      // Zeilennummer
      addr += 2;
      buf.append( memory.getMemWord( addr ) );
      addr += 2;

      // Anzahl Leerzeichen vor der Anweisung ermitteln
      int n = 0;
      while( addr < nextLineAddr ) {
	int ch = memory.getMemByte( addr, false );
	if( ch == '\u0020' ) {
	  n++;
	  addr++;
	} else {
	  if( ch != 0 ) {
	    for( int i = 0; i <= n; i++ ) {
	      buf.append( (char) '\u0020' );
	    }
	  }
	  break;
	}
      }

      // Programmzeile extrahieren
      String[] tokens = basicTokens;
      boolean  colon  = false;
      while( addr < nextLineAddr ) {
	int ch = memory.getMemByte( addr++, false );
	if( ch == 0 ) {
	  break;
	}
	if( ch == '\"' ) {
	  buf.append( (char) ch );
	  while( addr < nextLineAddr ) {
	    ch = memory.getMemByte( addr++, false );
	    if( ch == 0 ) {
	      break;
	    }
	    buf.append( (char) ch );
	    if( ch == '\"' ) {
	      break;
	    }
	  }
	} else {
	  if( colon && (ch == 0x9D) ) {
	    buf.append( "ELSE" );
	    colon = false;
	  } else {
	    if( colon ) {
	      buf.append( (char) ':' );
	      colon = false;
	    }
	    if( ch == ':' ) {
	      colon = true;
	    }
	    else if( ch == 0xFF ) {
	      tokens = basicTokensFF;
	    } else {
	      if( ch >= 0x80 ) {
		int pos = ch - 0x80;
		if( (pos >= 0) && (pos < tokens.length) ) {
		  buf.append( tokens[ pos ] );
		}
	      } else {
		buf.append( (char) ch );
	      }
	      tokens = basicTokens;
	    }
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


  public static int getDefaultSpeedKHz()
  {
    return 1500;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    checkAddPCListener( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			"jkcemu.system" ).equals( "HueblerGraphicsMC" );
    if( rv ) {
      if( hasBasic( props ) != this.basic ) {
	rv = false;
      }
    }
    return rv;
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return 0xFF6B;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int b = this.emuThread.getRAMByte(
			this.videoBaseAddr + (y * 32) + (x / 8) );
    int m = 0x01;
    int n = x % 8;
    if( n > 0 ) {
      m <<= n;
    }
    return (b & m) != 0 ? WHITE : BLACK;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/hgmc.htm";
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0xFF;
    if( this.romEnabled && (addr < 0x8000) ) {
      if( this.basic ) {
	if( addr < monBasicBytes.length ) {
	  rv = (int) monBasicBytes[ addr ] & 0xFF;
	}
      } else {
	addr &= 0x3FFF;		// unvollstaendige Adressdekodierung
	if( addr < monBytes.length ) {
	  rv = (int) monBytes[ addr ] & 0xFF;
	}
      }
    } else {
      rv = this.emuThread.getRAMByte( addr );
    }
    return rv;
  }


  @Override
  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return 0;
  }


  @Override
  public int getScreenHeight()
  {
    return 256;
  }


  @Override
  public int getScreenWidth()
  {
    return 256;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return true;
  }


  @Override
  public String getTitle()
  {
    return "H\u00FCbler-Grafik-MC";
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
      case KeyEvent.VK_HOME:
	ch = 1;
	break;

      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
	ch = 8;
	break;

      case KeyEvent.VK_TAB:
	ch = 9;
	break;

      case KeyEvent.VK_DOWN:
	ch = 0x0A;
	break;

      case KeyEvent.VK_ENTER:
	ch = 0x0D;
	break;

      case KeyEvent.VK_RIGHT:
	ch = 0x15;
	break;

      case KeyEvent.VK_UP:
	ch = 0x1A;
	break;

      case KeyEvent.VK_SPACE:
	ch = 0x20;
	break;

      case KeyEvent.VK_DELETE:
	ch = 0x7F;
	break;
    }
    if( ch > 0 ) {
      this.keyChar = ch;
      rv = true;
    }
    return rv;
  }


  @Override
  public void openBasicProgram()
  {
    String text = getBasicProgram( this.emuThread );
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoBasic();
    }
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
			sysCallNames,
			buf,
			sourceOnly,
			colMnemonic,
			colArgs,
			colRemark );
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    this.romEnabled = true;
    this.keyChar    = 0;
  }


  @Override
  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getKCBasicStyleEndAddr( this.emuThread, 0x3770 );
    if( endAddr >= 0x3770 ) {
      (new SaveDlg(
		this.screenFrm,
		0x3770,
		endAddr,
		'B',
		false,          // kein KC-BASIC
		false,		// kein RBASIC
		"BASIC-Programm speichern" )).setVisible( true );
    } else {
      showNoBasic();
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( !this.romEnabled || (addr >= 0x8000) ) {
      this.emuThread.setRAMByte( addr, value );
      if( (addr >= this.videoBaseAddr)
	  && (addr < (this.videoBaseAddr + 0x2000)) )
      {
	this.screenFrm.setScreenDirty( true );
      }
      rv = true;
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
    if( (begAddr == 0x3770) && (fileFmt != null) ) {
      if( fileFmt.equals( FileInfo.HEADERSAVE ) ) {
	if( fileType == 'B' ) {
	  int topAddr = begAddr + len;
	  this.emuThread.setMemWord( 0x0199, topAddr );
	  this.emuThread.setMemWord( 0x019B, topAddr );
	  this.emuThread.setMemWord( 0x019D, topAddr );
	}
      }
    }
  }


  @Override
  public void writeIOByte( int port, int value )
  {
    switch( port & 0xFC ) {
      case 0:
        this.romEnabled = false;
        break;

      case 4:
        this.romEnabled = true;
        break;

      case 0x10:
        this.videoBaseAddr = (value << 8) & 0xE000;
        this.screenFrm.setScreenDirty( true );
        break;

      default:
	super.writeIOByte( port, value );
    }
  }


	/* --- private Methoden --- */

  private void checkAddPCListener( Properties props )
  {
    checkAddPCListener( props, "jkcemu.hgmc.catch_print_calls" );
  }


  private static boolean hasBasic( Properties props )
  {
    return EmuUtil.getBooleanProperty( props, "jkcemu.hgmc.basic", true );
  }
}

