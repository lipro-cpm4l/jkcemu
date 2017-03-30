/*
 * (c) 2009-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des Huebler-Grafik-MC
 */

package jkcemu.emusys;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.CRC32;
import jkcemu.Main;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileFormat;
import jkcemu.base.SaveDlg;
import jkcemu.base.SourceUtil;
import jkcemu.emusys.huebler.AbstractHueblerMC;
import jkcemu.etc.VDIP;
import jkcemu.net.KCNet;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;
import z80emu.Z80MemView;


public class HueblerGraphicsMC extends AbstractHueblerMC

{
  public static final String SYSNAME     = "HueblerGraphicsMC";
  public static final String SYSTEXT     = "H\u00FCbler-Grafik-MC";
  public static final String PROP_PREFIX = "jkcemu.hgmc.";
  public static final String PROP_BASIC  = "basic";

  public static final int     DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX = 1000;
  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE            = true;


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

  private static byte[]              monBytes         = null;
  private static byte[]              monBasicBytes    = null;
  private static Map<Long,Character> pixelCRC32ToChar = null;

  private byte[]  romBytes;
  private String  romFile;
  private boolean romEnabled;
  private boolean basic;
  private int     videoBaseAddr;
  private KCNet   kcNet;
  private VDIP    vdip;


  public HueblerGraphicsMC( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.romBytes      = null;
    this.romFile       = null;
    this.romEnabled    = true;
    this.videoBaseAddr = 0;
    createIOSystem();

    this.kcNet = null;
    if( emulatesKCNet( props ) ) {
      this.kcNet = new KCNet( "Netzwerk-PIO (E/A-Adressen C0h-C3h)" );
    }
    this.vdip = null;
    if( emulatesUSB( props ) ) {
      this.vdip = new VDIP(
			this.emuThread.getFileTimesViewFactory(),
			"USB-PIO (E/A-Adressen FCh-FFh)" );
    }
    Z80CPU cpu = emuThread.getZ80CPU();
    if( (this.kcNet != null) || (this.vdip != null) ) {
      java.util.List<Z80InterruptSource> iSources = new ArrayList<>();
      iSources.add( this.ctc );
      iSources.add( this.pio );
      if( this.kcNet != null ) {
	iSources.add( this.kcNet );
      }
      if( this.vdip != null ) {
	iSources.add( this.vdip );
      }
      try {
	cpu.setInterruptSources(
		iSources.toArray(
			new Z80InterruptSource[ iSources.size() ] ) );
      }
      catch( ArrayStoreException ex ) {}
      if( this.vdip != null ) {
	this.vdip.applySettings( props );
      }
    }
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );
    checkAddPCListener( props );
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }
    z80MaxSpeedChanged( cpu );
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


  public static boolean getDefaultSwapKeyCharCase()
  {
    return true;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    buf.append( "<h1>H&uuml;bler-Grafik-MC Status</h1>\n"
	+ "<table border=\"1\">\n"
	+ "<tr><td>ROM:</td><td>" );
    EmuUtil.appendOnOffText( buf, this.romEnabled );
    buf.append( "</td></tr>\n"
	+ "<tr><td>Bildwiederholspeicher:</td><td>" );
    buf.append(
	String.format(
		"%04Xh-%04Xh",
		this.videoBaseAddr,
		this.videoBaseAddr + 0x1FFF ) );
    buf.append( "</td></tr>\n"
	+ "</table>\n" );
  }


  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    checkAddPCListener( props );
    if( this.vdip != null ) {
      this.vdip.applySettings( props );
    }
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
    if( rv && hasBasic( props ) != this.basic ) {
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
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public void die()
  {
    super.die();
    Z80CPU cpu = this.emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    if( this.kcNet != null ) {
      this.kcNet.die();
    }
    if( this.vdip != null ) {
      this.vdip.die();
    }
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
  public CharRaster getCurScreenCharRaster()
  {
    return new CharRaster( 32, 25, 10, 8, 8, 0 );
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
      if( !this.basic ) {
	addr &= 0x3FFF;		// unvollstaendige Adressdekodierung
      }
      if( this.romBytes != null ) {
	if( addr < this.romBytes.length ) {
	  rv = (int) this.romBytes[ addr ] & 0xFF;
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
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int rv = -1;
    if( (chX >= 0) && (chX < 32) && (chY >= 0) && (chY < 32) ) {
      Map<Long,Character> crc32ToChar = getPixelCRC32ToCharMap();
      if( crc32ToChar != null ) {
	CRC32 crc1 = new CRC32();
	CRC32 crc2 = new CRC32();
	int   addr = this.videoBaseAddr + (chY * 32 * 10) + chX;
	for( int i = 0; i < 8; i++ ) {
	  int b = getMemByte( addr, false );
	  crc1.update( b );
	  crc2.update( ~b & 0xFF );
	  addr += 32;
	}
	Character ch = crc32ToChar.get( crc1.getValue() );
	if( ch == null ) {
	  // Zeichen invers?
	  ch = crc32ToChar.get( crc2.getValue() );
	}
	if( ch != null ) {
	  rv = ch.charValue();
	}
      }
    }
    return rv;
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
    return getDefaultSwapKeyCharCase();
  }


  @Override
  public String getTitle()
  {
    return SYSTEXT;
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
  public int readIOByte( int port, int tStates )
  {
    int rv = 0;
    switch( port & 0xFF ) {
      case 0xC0:
      case 0xC1:
      case 0xC2:
      case 0xC3:
	if( this.kcNet != null ) {
	  rv = this.kcNet.read( port );
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

      default:
	rv = super.readIOByte( port, tStates );
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
    return reassSysCallTable(
			memory,
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
    super.reset( resetLevel, props );
    if( resetLevel == EmuThread.ResetLevel.POWER_ON ) {
      if( isReloadExtROMsOnPowerOnEnabled( props ) ) {
	loadROMs( props );
      }
    }
    this.romEnabled = true;
  }


  @Override
  public void saveBasicProgram()
  {
    int endAddr = SourceUtil.getBasicEndAddr( this.emuThread, 0x3770 );
    if( endAddr >= 0x3770 ) {
      (new SaveDlg(
		this.screenFrm,
		0x3770,
		endAddr,
		"BASIC-Programm speichern",
		SaveDlg.BasicType.MS_DERIVED_BASIC,
		EmuUtil.getBasicFileFilter() )).setVisible( true );
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
  public boolean supportsSaveBasic()
  {
    return true;
  }


  @Override
  public void updSysCells(
			int        begAddr,
			int        len,
			FileFormat fileFmt,
			int        fileType )
  {
    if( fileFmt != null ) {
      if( (fileFmt.equals( FileFormat.BASIC_PRG )
		&& (begAddr == 0x3770)
		&& (len > 7))
	  || (fileFmt.equals( FileFormat.HEADERSAVE )
		&& (fileType == 'B')
		&& (begAddr <= 0x3770)
		&& ((begAddr + len) > 0x3777)) )
      {
	int tAddr = SourceUtil.getBasicEndAddr( this.emuThread, 0x3770 ) + 1;
	if( tAddr > 0x3770 ) {
	  this.emuThread.setMemWord( 0x0199, tAddr );
	  this.emuThread.setMemWord( 0x019B, tAddr );
	  this.emuThread.setMemWord( 0x019D, tAddr );
	}
      }
    }
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
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

      case 0xC0:
      case 0xC1:
      case 0xC2:
      case 0xC3:
	if( this.kcNet != null ) {
	  this.kcNet.write( port, value );
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

      default:
	super.writeIOByte( port, value, tStates );
    }
  }


  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    super.z80MaxSpeedChanged( cpu );
    if( this.kcNet != null ) {
      this.kcNet.z80MaxSpeedChanged( cpu );
    }
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    if( this.kcNet != null ) {
      this.kcNet.z80TStatesProcessed( cpu, tStates );
    }
  }


	/* --- private Methoden --- */

  private void checkAddPCListener( Properties props )
  {
    checkAddPCListener( props, this.propPrefix + PROP_CATCH_PRINT_CALLS );
  }


  private boolean hasBasic( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_BASIC,
				true );
  }


  private boolean emulatesKCNet( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_KCNET_ENABLED,
				false );
  }


  private boolean emulatesUSB( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PROP_VDIP_ENABLED,
				false );
  }


  private void ensureMonBytesLoaded()
  {
    if( monBytes == null )
      monBytes = readResource( "/rom/huebler/mon30.bin" );
  }


  private Map<Long,Character> getPixelCRC32ToCharMap()
  {
    if( pixelCRC32ToChar == null ) {
      ensureMonBytesLoaded();
      if( monBytes != null ) {
        if( monBytes.length >= 0x0E0D ) {
          Map<Long,Character> map  = new HashMap<>();
          CRC32               crc  = new CRC32();
          int                 addr = 0x0B0D;
          for( int c = 0x20; c <= 0x7E; c++ ) {
            crc.reset();
            crc.update( monBytes, addr, 8 );
            map.put( crc.getValue(), Character.valueOf( (char) c ) );
            addr += 8;
          }
          pixelCRC32ToChar = map;
        }
      }
    }
    return pixelCRC32ToChar;
  }


  private void loadROMs( Properties props )
  {
    this.romFile = EmuUtil.getProperty(
			props,
			this.propPrefix + PROP_ROM_PREFIX + PROP_FILE );
    this.romBytes = readROMFile( this.romFile, 0x8000, "ROM-Inhalt" );
    if( this.romBytes == null ) {
      if( hasBasic( props ) ) {
	monBasicBytes = readResource( "/rom/huebler/mon30p_hbasic33p.bin" );
	this.romBytes = monBasicBytes;
	this.basic    = true;
      } else {
	ensureMonBytesLoaded();
	this.romBytes = monBytes;
	this.basic = false;
      }
    }
  }
}
