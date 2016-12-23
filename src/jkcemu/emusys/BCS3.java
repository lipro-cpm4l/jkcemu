/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des BCS3
 */

package jkcemu.emusys;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileFormat;
import jkcemu.base.SaveDlg;
import jkcemu.emusys.bcs3.BCS3KeyboardFld;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;
import z80emu.Z80CTC;
import z80emu.Z80CTCListener;
import z80emu.Z80InterruptSource;


public class BCS3 extends EmuSys implements Z80CTCListener
{
  public static final String SYSNAME        = "BCS3";
  public static final String PROP_PREFIX    = "jkcemu.bcs3.";
  public static final String PROP_RAM_KBYTE = "ram.kbyte";
  public static final String PROP_REMOVE_HSYNC_FROM_AUDIO
					= "remove_hsync_from_audio";

  public static final int     DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX = 300;
  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE            = true;

  public static final String VALUE_OS_VERSION_24    = "2.4";
  public static final String VALUE_OS_VERSION_31_29 = "3.1_29";
  public static final String VALUE_OS_VERSION_31_40 = "3.1_40";
  public static final String VALUE_OS_VERSION_33    = "3.3";


  /*
   * Die beiden Tabellen mappen die BASIC-SE2.4- und BASIC-SE3.1-Tokens
   * in die entsprechenden Texte.
   * Der Index fuer jede Tabelle ergibt sich aus "Wert des Tokens - 0xB0".
   */
  private static final String[] se24Tokens = {
	null,     null,   null,    null,		// 0xB0
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     null,   null,    null,		// 0xC0
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     null,   null,    null,
	null,     "THEN", "AND",   null,		// 0xD0
	null,     null,   "OR",    "PEEK(",
	null,     "IN(",  "RND(",  null,
	null,     "BYTE", "END",   null,
	"REM",    null,   "GOSUB", null,		// 0xE0
	"CLEAR",  null,   "IF",    null,
	"INPUT",  null,   "PRINT", null,
	"RETURN", null,   "GOTO",  null,
	"RUN",    null,   "LIST",  null,		// 0xF0
	"LET",    null,   "LOAD",  null,
	"SAVE",   null,   "POKE",  null,
	"OUT",    null,   "NEW",   null };

  /*
   * Diese Tabelle mappt die BCS3-BASIC-SE3.1-Tokens
   * in die entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0xB0".
   */
  private static final String[] se31Tokens = {
	"LEN(",    null, "INT(",   null,		// 0xB0
	"USR(",    null, null,     null,
	"INKEY$",  null, "STEP",   null,
	"TO",      null, "CHR$",   null,
	"THEN",    null, "AND",    null,		// 0xC0
	"OR",      null, "PEEK(",  null,
	"IN(",     null, "RND",    null,
	null,      null, "END",    null,
	"REM",     null, "GOSUB",  null,		// 0xD0
	"CLS",     null, "IF",     null,
	"INPUT",   null, "PRINT",  null,
	"RESTORE", null, "RETURN", null,
	"GOTO",    null, "RUN",    null,		// 0xE0
	"LIST",    null, "LET",    null,
	"LOAD",    null, "SAVE",   null,
	"POKE",    null, "OUT",    null,
	"FOR",     null, "NEXT",   null,		// 0xF0
	"DIM",     null, "PLOT",   null,
	"UNPLOT",  null, "NEW",    null,
	"READ",    null, "DATA",   null };

  /*
   * Diese Tabelle mappt die BCS3-S/P-BASIC-3.3-Tokens
   * in die entsprechenden Texte.
   * Der Index fuer die Tabelle ergibt sich aus "Wert des Tokens - 0xB0".
   */
  private static final String[] sp33Tokens = {
	null,      null,     null,        null,		// 0xB0
	null,      null,     null,        null,
	null,      null,     null,        null,
	"USR(",    "RND(",   "PEEK(",     "LEN(",
	"INT(",    "INKEY$", "IN(",       "CHR$(",	// 0xC0
	"ASC(",    "OR",     "AND",       "THEN",
	"STEP",    "TO",     "END",       null,
	"REM",     null,     "GOSUB",     null,
	"CLS",     null,     "IF",        null,		// 0xD0
	"INPUT",   null,     "PRINT",     null,
	"RESTORE", null,     "RETURN",    null,
	"GOTO",    null,     "CALL",      null,
	"RUN",     null,     "LIST",      null,		// 0xE0
	"LET",     null,     "LOAD",      null,
	"SAVE",    null,     "POKE",      null,
	"OUT",     null,     "FOR",       null,
	"NEXT",    null,     "DIM",       null,		// 0xF0
	"PLOT",    null,     "UNPLOT",    null,
	"NEW",     null,     "RANDOMIZE", null,
	"READ",    null,     "DATA",      null };

  private static int SCREEN_CHARS_PER_ROW_MAX = 42;
  private static int SCREEN_HIDDEN_LINES      = 60;
  private static int SCREEN_HEIGHT            = 320;

  private static int[] endInstBytesSE24 = { 0x0F, 0x27, 0xDE, 0x1E };
  private static int[] endInstBytesSE31 = { 0x0F, 0x27, 0xCE, 0x1E };
  private static int[] endInstBytesSP33 = { 0x00, 0x27, 0xCA, 0x1E };

  private static byte[] fontBytesSE24  = null;
  private static byte[] fontBytesSE31  = null;
  private static byte[] fontBytesSP33  = null;
  private static byte[] osBytesSE24    = null;
  private static byte[] osBytesSE31_29 = null;
  private static byte[] osBytesSE31_40 = null;
  private static byte[] osBytesSP33_29 = null;
  private static byte[] mcEdtitorSE31  = null;

  private Z80CTC           ctc;
  private String           osFile;
  private byte[]           osBytes;
  private byte[]           fontBytes;
  private byte[]           screenChars;
  private byte[]           screenPixelsWriting;
  private byte[]           screenPixelsVisible;
  private byte[]           romF000;
  private byte[]           ram;
  private int              ramEndAddr;
  private int              osVersion;
  private int              screenActiveTStates;
  private int              screenColCnt;
  private int              screenColNum;
  private int              screenRowNum;
  private int              screenLineNum;
  private int              screenLineInChar;
  private int              screenChar0Y;
  private int              screenChar1Y;
  private int[]            kbMatrix;
  private long             lastTapeOutTStates;
  private boolean          lastTapeOutPhase;
  private boolean          removeHSyncFromAudio;
  private volatile boolean screenEnabled;
  private CharRaster       charRaster;
  private BCS3KeyboardFld  keyboardFld;


  public BCS3( EmuThread emuThread, Properties props )
  {
    super( emuThread, props, PROP_PREFIX );
    this.osFile    = null;
    this.osBytes   = null;
    this.fontBytes = null;
    this.romF000   = null;
    if( !isReloadExtROMsOnPowerOnEnabled( props ) ) {
      loadROMs( props );
    }

    String version = EmuUtil.getProperty(
				props,
				this.propPrefix + PROP_OS_VERSION );
    if( version.equals( VALUE_OS_VERSION_31_29 ) ) {
      this.osVersion = 31;
      if( this.osBytes == null ) {
	if( osBytesSE31_29 == null ) {
	  osBytesSE31_29 = readResource( "/rom/bcs3/se31_29.bin" );
	}
	this.osBytes = osBytesSE31_29;
      }
      if( this.romF000 == null ) {
	if( mcEdtitorSE31 == null ) {
	  mcEdtitorSE31 = readResource( "/rom/bcs3/se31mceditor.bin" );
	}
	this.romF000 = mcEdtitorSE31;
      }
    } else if( version.equals( VALUE_OS_VERSION_31_40 ) ) {
      this.osVersion = 31;
      if( this.osBytes == null ) {
	if( osBytesSE31_40 == null ) {
	  osBytesSE31_40 = readResource( "/rom/bcs3/se31_40.bin" );
	}
	this.osBytes = osBytesSE31_40;
      }
    } else if( version.equals( "3.3" ) ) {
      this.osVersion = 33;
      if( this.osBytes == null ) {
	if( osBytesSP33_29 == null ) {
	  osBytesSP33_29 = readResource( "/rom/bcs3/sp33_29.bin" );
	}
	this.osBytes = osBytesSP33_29;
      }
    } else {
      this.osVersion = 24;
      if( this.osBytes == null ) {
	if( osBytesSE24 == null ) {
	  osBytesSE24 = readResource( "/rom/bcs3/se24.bin" );
	}
	this.osBytes = osBytesSE24;
      }
    }
    this.ram                  = new byte[ 0x0400 ];
    this.ramEndAddr           = getRAMEndAddr( props );
    this.kbMatrix             = new int[ 10 ];
    this.screenEnabled        = false;
    this.screenActiveTStates  = 0;
    this.screenColCnt         = 0;
    this.screenColNum         = 0;
    this.screenRowNum         = -1;
    this.screenLineNum        = 0;
    this.screenLineInChar     = 0;
    this.screenChar0Y         = -1;
    this.screenChar1Y         = -1;
    this.lastTapeOutTStates   = 0;
    this.lastTapeOutPhase     = false;
    this.removeHSyncFromAudio = getRemoveHSyncFromAudio( props );
    this.charRaster           = null;
    this.keyboardFld          = null;

    // Pixelpuffer zur Anzeige
    this.screenPixelsVisible = new byte[
	(SCREEN_HEIGHT - SCREEN_HIDDEN_LINES) * SCREEN_CHARS_PER_ROW_MAX ];

    // Pixelpuffer, auf den aktuell geschrieben wird
    this.screenPixelsWriting = new byte[ this.screenPixelsVisible.length ];

    /*
     * Die auf dem Bildschirm ausgegebenen Zeichen werden zusaetzlich
     * in einen Textspeicher geschrieben,
     * damit sie von dort in die Zwischenablage kopiert werden koennen.
     * Da ein Zeichen hardwaremaessig fest 8 Pixel hoch ist,
     * genuegt 1/8 der Groesse des Pixelspeichers.
     */
    this.screenChars = new byte[ this.screenPixelsWriting.length / 8 ];

    Z80CPU cpu = emuThread.getZ80CPU();
    this.ctc   = new Z80CTC( "CTC" );
    cpu.setInterruptSources( this.ctc );
    this.ctc.setTimerConnection( 0, 1 );
    this.ctc.setTimerConnection( 1, 2 );
    this.ctc.addCTCListener( this );
    cpu.addMaxSpeedListener( this );
    cpu.addTStatesListener( this );
  }


  public static String getBasicProgram( byte[] data )
  {
    String rv = null;
    if( data != null ) {
      String[] tokens      = null;
      int      endLineNum  = -1;
      int      lastLineNum = -1;
      int      pos         = 0;
      while( pos < data.length - 3 ) {
	if( equalsRange( data, pos, endInstBytesSE24 ) ) {
	  tokens     = se24Tokens;
	  endLineNum = 9999;
	  break;
	}
	if( equalsRange( data, pos, endInstBytesSE31 ) ) {
	  tokens     = se31Tokens;
	  endLineNum = 9999;
	  break;
	}
	if( equalsRange( data, pos, endInstBytesSP33 ) ) {
	  tokens     = sp33Tokens;
	  endLineNum = 9984;
	  break;
	}
	int lineNum = EmuUtil.getWord( data, pos );
	if( lineNum <= lastLineNum ) {
	  break;
	}
	pos += 2;
	boolean found = false;
	while( !found && (pos < data.length) ) {
	  if( data[ pos++ ] == (byte) 0x1E )
	    found = true;
	}
	if( !found ) {
	  break;
	}
	lastLineNum = lineNum;
      }
      if( (tokens != null) && (endLineNum > 0) ) {
	pos = 0;
	int lineNum = EmuUtil.getWord( data, pos );
	if( (lineNum >= 0) && (lineNum < endLineNum) ) {
	  StringBuilder buf = new StringBuilder( 0x2000 );
	  while( (lineNum >= 0) && (lineNum <= endLineNum) ) {
	    buf.append( lineNum );
	    pos += 2;
	    boolean sep = false;
	    boolean spc = true;
	    int     ch  = (int) data[ pos++ ] & 0xFF;
	    while( (ch != 0) && (ch != 0x1E) ) {
	      if( spc ) {
		buf.append( (char) '\u0020' );
		sep = false;
		spc = false;
	      }
	      if( ch >= 0xB0 ) {
		int idx = ch - 0xB0;
		if( (idx >= 0) && (idx < tokens.length) ) {
		  String s = tokens[ idx ];
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
	      if( ch != 0 ) {
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
	      ch = (int) data[ pos++ ] & 0xFF;
	    }
	    buf.append( (char) '\n' );
	    if( ch == 0 ) {
	      break;
	    }
	    if( lineNum == endLineNum ) {
	      break;
	    }
	    lineNum = EmuUtil.getWord( data, pos );
	  }
	  if( buf.length() > 0 ) {
	    rv = buf.toString();
	  }
	}
      }
    }
    return rv;
  }


  public static int getDefaultSpeedKHz( Properties props )
  {
    return EmuUtil.getProperty(
		props,
		PROP_PREFIX + PROP_OS_VERSION )
			.equals( VALUE_OS_VERSION_31_40 ) ? 3500 : 2500;
  }


	/* --- Z80CTCListener --- */

  @Override
  public void z80CTCUpdate( Z80CTC ctc, int timerNum )
  {
    if( ctc == this.ctc ) {
      switch( timerNum ) {
	case 0:
	  /*
	   * Zeilensynchronimpuls und Audioausgabe in einem:
	   *  - Rest der Zeile im Textpuffer leeren
	   *  - Spaltenzaehler auf 0 setzen
	   *  - Pixelzeilenzaehler erhoehen
	   *  - Phasenwechsel am Audioausgang
	   *  - CPU aus Wait erwachen
	   */
	  if( (this.screenLineNum == 0) && (this.screenRowNum >= 0) ) {
	    int pos = (this.screenRowNum * SCREEN_CHARS_PER_ROW_MAX)
						+ this.screenColNum;
	    while( (pos < this.screenChars.length)
		   && (this.screenColNum < SCREEN_CHARS_PER_ROW_MAX) )
	    {
	      this.screenChars[ pos++ ] = (byte) 0x20;
	      this.screenColNum++;
	    }
	  }
	  this.screenColNum = 0;
	  if( this.screenLineNum < SCREEN_HEIGHT ) {
	    this.screenLineNum++;
	  }
	  if( this.screenLineInChar < 7 ) {
	    this.screenLineInChar++;
	  } else {
	    this.screenLineInChar = 0;
	  }
	  updTapeOutPhase( (this.screenLineInChar & 0x01) != 0 );
	  this.emuThread.getZ80CPU().setWaitMode( false );
	  break;

	case 1:
	  /*
	   * Pixelzeilenzaehler innerhalb eines Zeichens zuruecksetzen
	   * sowie Audioausgang aktualisieren
	   */
	  this.screenLineInChar = 0;
	  updTapeOutPhase( (this.screenLineInChar & 0x01) != 0 );
	  break;

	case 2:
	  /*
	   * Bildsynchronimpuls und Audioausgabe in einem:
	   *  - Rest im Textpuffer leeren
	   *  - Bildschirmraster ermitteln
	   *  - Pixelzeilenzaehler auf 0 setzen
	   *  - Spaltenzaehler auf 0 setzen
	   *  - Pixelpuffer mit dem der Anzeige tauschen
	   *  - neuen Pixelpuffer leeren
	   */
	  if( this.screenRowNum >= 0 ) {
	    int pos = (this.screenRowNum * SCREEN_CHARS_PER_ROW_MAX)
						+ this.screenColNum;
	    if( pos < this.screenChars.length ) {
	      Arrays.fill(
			this.screenChars,
			pos,
			this.screenChars.length,
			(byte) 0x20 );
	    }
	  } else {
	    Arrays.fill( this.screenChars, (byte) 0x20 );
	  }

	  // Char-Raster ermitteln
	  boolean rasChanged = false;
	  int     rowCount   = this.screenRowNum + 1;
	  int     rowHeight  = this.screenChar1Y - this.screenChar0Y;
	  int     topLine    = this.screenChar0Y - SCREEN_HIDDEN_LINES - 6;

	  // Pixelpuffer tauschen und CharRaster aktualisieren
	  synchronized( this ) {
	    byte[] buf               = this.screenPixelsVisible;
	    this.screenPixelsVisible = this.screenPixelsWriting;
	    this.screenPixelsWriting = buf;
	    if( this.charRaster != null ) {
	      if( (this.charRaster.getColCount() != this.screenColCnt)
		  || (this.charRaster.getRowCount() != rowCount)
		  || (this.charRaster.getRowHeight() != rowHeight)
		  || (this.charRaster.getTopLine() != topLine) )
	      {
		rasChanged = true;
	      }
	    }
	    if( rasChanged || (this.charRaster == null) ) {
	      if( (this.screenColCnt > 0)
		  && (rowCount > 0)
		  && (rowHeight > 0) )
	      {
		if( this.charRaster == null ) {
		  rasChanged = true;
		}
		this.charRaster = new CharRaster(
					this.screenColCnt,
					rowCount,
					rowHeight,
					Math.min( rowHeight, 8 ),
					8,
					topLine );
	      }
	    }
	  }

	  // naechsten Frame initialisieren
	  this.screenColCnt  = 0;
	  this.screenColNum  = 0;
	  this.screenRowNum  = -1;
	  this.screenLineNum = 0;
	  this.screenChar0Y  = -1;
	  this.screenChar1Y  = -1;
	  Arrays.fill( this.screenPixelsWriting, (byte) 0 );

	  /*
	   * Bildschirmausgabe einschalten,
	   * wenn Bildsynchronimpulse kommen
	   */
	  this.screenActiveTStates = 100000;
	  this.screenEnabled       = true;

	  // Bildschirm und ggf. Copy-Button aktualisieren
	  this.screenFrm.setScreenDirty( true );
	  if( rasChanged ) {
	    this.screenFrm.clearScreenSelection();
	    this.screenFrm.fireUpdScreenTextActionsEnabled();
	  }
	  break;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applySettings( Properties props )
  {
    super.applySettings( props );
    loadFont( props );
    this.removeHSyncFromAudio = getRemoveHSyncFromAudio( props );
  }


  @Override
  public boolean canApplySettings( Properties props )
  {
    boolean rv = EmuUtil.getProperty(
			props,
			EmuThread.PROP_SYSNAME ).equals( SYSNAME );
    if( rv ) {
      rv = TextUtil.equals(
		this.osFile,
		EmuUtil.getProperty(
				props,
				this.propPrefix + PROP_OS_FILE ) );
    }
    if( rv ) {
      String version = EmuUtil.getProperty(
				props,
				this.propPrefix + PROP_OS_VERSION );
      if( version.equals( VALUE_OS_VERSION_31_29 ) ) {
	if( this.osBytes != osBytesSE31_29 ) {
	  rv = false;
	}
      } else if( version.equals( VALUE_OS_VERSION_31_40 ) ) {
	if( this.osBytes != osBytesSE31_40 ) {
	  rv = false;
	}
      } else if( version.equals( VALUE_OS_VERSION_33 ) ) {
	if( this.osBytes != osBytesSP33_29 ) {
	  rv = false;
	}
      } else {
	if( this.osBytes != osBytesSE24 ) {
	  rv = false;
	}
      }
    }
    if( rv ) {
      if( getRAMEndAddr( props ) != this.ramEndAddr ) {
	rv = false;
      }
    }
    return rv;
  }


  @Override
  public synchronized boolean canExtractScreenText()
  {
    return this.charRaster != null;
  }


  @Override
  public AbstractKeyboardFld createKeyboardFld()
  {
    this.keyboardFld = new BCS3KeyboardFld( this );
    return this.keyboardFld;
  }


  @Override
  public void die()
  {
    this.ctc.removeCTCListener( this );

    Z80CPU cpu = emuThread.getZ80CPU();
    cpu.removeTStatesListener( this );
    cpu.removeMaxSpeedListener( this );
    cpu.setInterruptSources( (Z80InterruptSource[]) null );
  }


  @Override
  public int getAppStartStackInitValue()
  {
    return this.osBytes == this.osBytesSE24 ? 0x3C50 : 0x3C80;
  }


  @Override
  public int getBorderColorIndex()
  {
    return this.screenEnabled ? WHITE : BLACK;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv = WHITE;
    if( this.screenEnabled ) {
      synchronized( this ) {
	int col = x / 8;
	int idx = y * (SCREEN_CHARS_PER_ROW_MAX) + col;
	if( (idx >= 0) && (idx < this.screenPixelsVisible.length) ) {
	  int m = 0x80;
	  int n = x % 8;
	  if( n > 0 ) {
	    m >>= n;
	  }
	  if( (this.screenPixelsVisible[ idx ] & m) != 0 ) {
	    rv = BLACK;
	  }
	}
      }
    } else {
      rv = BLACK;
    }
    return rv;
  }


  @Override
  public synchronized CharRaster getCurScreenCharRaster()
  {
    return this.charRaster;
  }


  @Override
  protected long getDelayMillisAfterPasteChar()
  {
    return 80;
  }


  @Override
  protected long getDelayMillisAfterPasteEnter()
  {
    return 200;
  }


  @Override
  protected long getHoldMillisPasteChar()
  {
    return 60;
  }


  @Override
  public String getHelpPage()
  {
    return "/help/bcs3.htm";
  }


  @Override
  public Integer getLoadAddr()
  {
    Integer rv = null;
    if( this.osBytes == osBytesSE24 ) {
      rv = 0x3DA1;
    }
    else if( (this.osBytes == osBytesSE31_29)
	     || (this.osBytes == osBytesSE31_40)
	     || (this.osBytes == osBytesSP33_29) )
    {
      rv = getMemWord( 0x3C00 );
    }
    return rv;
  }


  @Override
  public int getMemByte( int addr, boolean m1 )
  {
    addr &= 0xFFFF;

    int rv = 0x0F;
    if( addr < 0x4000 ) {
      addr &= 0xDFFF;		// A13=0
      if( (addr >= 0x1000) && (addr < 0x1400)) {
	// Abfrage Tastatur und Kassettenrecordereingang
	rv    = 0x00;
	int a = ~addr;
	int m = 0x01;
	for( int i = 0; i < this.kbMatrix.length; i++ ) {
	  if( (a & m) != 0 ) {
	    rv |= this.kbMatrix[ i ];
	  }
	  m <<= 1;
	}
	if( this.emuThread.readTapeInPhase() ) {
	  rv |= 0x80;
	} else {
	  rv &= 0x7F;
	}
      }
      else if( (addr >= 0x1800) && (addr < 0x1C00)) {
	/*
	 * Zugriff auf den Bildwiederholspeicher mit Auswertung von Bit 7:
	 * Wenn Bit 7 nicht gesetzt ist, liest die CPU Null-Bytes
	 * waehrend die Datenbytes auf dem Bildschirm ausgegeben werden.
	 * Wenn Bit 7 gesetzt ist, liest die CPU die Datenbytes
	 * und nichts wird auf dem Bildschirm ausgegeben.
	 */
	rv      = 0;
	int idx = addr - 0x1800;
	if( (idx >= 0) && (idx < this.ram.length) ) {
	  int ch = (int) (this.ram[ idx ] & 0xFF);
	  if( (ch & 0x80) != 0 ) {
	    rv = ch;
	  }
	}
      }
      else if( (addr >= 0x1C00) && (addr < 0x2000)) {
	int idx = addr - 0x1C00;
	if( (idx >= 0) && (idx < this.ram.length) ) {
	  rv = (int) (this.ram[ idx ] & 0xFF);
	}
      }
      else if( this.osBytes != null ) {
	if( addr < this.osBytes.length ) {
	  rv = (int) this.osBytes[ addr ] & 0xFF;
	}
      }
    }
    else if( addr <= this.ramEndAddr ) {
      rv = this.emuThread.getRAMByte( addr );
    }
    else if( (addr >= 0xF000) && (this.romF000 != null) ) {
      int idx = addr - 0xF000;
      if( idx < this.romF000.length ) {
	rv = (int) this.romF000[ idx ] & 0xFF;
      }
    }
    return rv;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch  = -1;
    int b   = -1;
    int idx = (chY * SCREEN_CHARS_PER_ROW_MAX) + chX;
    if( (idx >= 0 ) && (idx < this.screenChars.length) ) {
      b = (int) this.screenChars[ idx ] & 0xFF;
    }
    if( (b >= 0) && (b < 0x10) ) {
      if( this.osVersion < 32 ) {
	ch = '\u0020';
      }
    } else if( (b >= 10) && (b < 0x20) ) {
      if( this.osVersion < 32 ) {
	ch = '\u0020';
      } else {
	switch( b ) {
	  case 0x14:
	    ch = '\u2665';
	    break;
	  case 0x15:
	    ch = '\u2660';
	    break;
	  case 0x16:
	    ch = '\u2666';
	    break;
	  case 0x17:
	    ch = '\u2663';
	    break;
	  case 0x18:
	    ch = '\u25CF';
	    break;
	  case 0x19:
	    ch = '\u25E2';
	    break;
	  case 0x1A:
	    ch = '\u25E5';
	    break;
	  case 0x1B:
	    ch = '\u25E4';
	    break;
	  case 0x1C:
	    ch = '\u25E3';
	    break;
	  case 0x1D:
	    ch = '\u25A1';
	    break;
	  case 0x1E:
	    ch = '\u0020';
	    break;
	  case 0x1F:
	    ch = '\u03A9';
	    break;
	}
      }
    } else if( (b >= 0x20) && (b <= 0x7F) ) {
      switch( b ) {
	case 0x5D:
	  if( this.osVersion >= 32 ) {
	    ch = ']';
	  }
	  break;
	case 0x5F:			// invertiertes Groesserzeichen
	  break;
	case 0x60:
	  if( this.osVersion < 32 ) {
	    ch = '\u0020';
	  } else {
	    ch = b;
	  }
	  break;
	case 0x7B:
	  ch = '\u00E4';			// ae
	  break;
	case 0x7C:
	  ch = '\u00F6';			// ae
	  break;
	case 0x7D:
	  ch = '\u00FC';			// ae
	  break;
	case 0x7E:
	  ch = '|';
	  break;
	case 0x7F:
	  if( this.osVersion < 32 ) {
	    ch = '\u25A0';			// ausgefuelltes Feld
	  } else {
	    ch = '\u00B7';			// Punkt in der Mitte
	  }
	  break;
	default:
	  ch = b;
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return SCREEN_HEIGHT - SCREEN_HIDDEN_LINES;
  }


  @Override
  public int getScreenWidth()
  {
    return SCREEN_CHARS_PER_ROW_MAX * 8;
  }


  @Override
  public boolean getSwapKeyCharCase()
  {
    return DEFAULT_SWAP_KEY_CHAR_CASE;
  }


  @Override
  public String getTitle()
  {
    return SYSNAME;
  }


  @Override
  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    boolean rv = false;
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
	this.kbMatrix[ 7 ] = 0x01;
	rv                 = true;
	break;

      case KeyEvent.VK_ENTER:
	this.kbMatrix[ 8 ] = 0x01;
	rv                 = true;
	break;

      case KeyEvent.VK_SPACE:
	this.kbMatrix[ 6 ] = 0x01;
	rv                 = true;
    }
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  public void keyReleased()
  {
    Arrays.fill( this.kbMatrix, 0 );
    updKeyboardFld();
  }


  @Override
  public boolean keyTyped( char ch )
  {
    boolean rv        = false;
    boolean shiftMode = false;
    int     idx       = -1;
    int     value     = 0;
    if( (ch >= 0x20) && (ch <= 0x29) ) {
      idx       = ch - 0x20;
      value     = 0x08;
      shiftMode = true;
    } else if( (ch >= 0x29) && (ch <= 0x2F) ) {
      idx       = ch - 0x26;
      value     = 0x04;
      shiftMode = true;
    } else if( (ch >= '0') && (ch <= '9') ) {
      idx   = ch - '0';
      value = 0x08;
    } else if( (ch >= 0x3A) && (ch <= 0x40) ) {
      idx       = ch - 0x3A;
      value     = 0x02;
      shiftMode = true;
    } else {
      if( (ch >= 'a') && (ch <= 'z') ) {
	ch = Character.toUpperCase( ch );
      }
      if( (ch >= 'A') && (ch <= 'J') ) {
	idx   = ch - 'A';
	value = 0x04;
      }
      else if( (ch >= 'K') && (ch <= 'T') ) {
	idx   = ch - 'K';
	value = 0x02;
      }
      else if( (ch >= 'U') && (ch <= 'Z') ) {
	idx   = ch - 'U';
	value = 0x01;
      }
    }
    if( (idx >= 0) && (idx < this.kbMatrix.length) ) {
      if( shiftMode && (idx == 9) ) {
	this.kbMatrix[ idx ] = value | 0x01;
      } else {
	if( shiftMode ) {
	  this.kbMatrix[ 9 ] = 0x01;
	}
	this.kbMatrix[ idx ] = value;
      }
      rv = true;
    }
    if( rv ) {
      updKeyboardFld();
    }
    return rv;
  }


  @Override
  public void openBasicProgram()
  {
    String   text       = null;
    String[] tokens     = null;
    int      addr       = -1;
    int      endLineNum = -1;
    if( this.osBytes == osBytesSE24 ) {
      addr       = 0x3DA1;
      endLineNum = 9999;
      tokens     = se24Tokens;
    }
    else if( (this.osBytes == osBytesSE31_29)
	     || (this.osBytes == osBytesSE31_40) )
    {
      addr       = getMemWord( 0x3C00 );
      endLineNum = 9999;
      tokens     = se31Tokens;
    }
    else if( this.osBytes == osBytesSP33_29 ) {
      addr       = getMemWord( 0x3C00 );
      endLineNum = 9984;
      tokens     = sp33Tokens;
    }
    if( (addr >= 0) && (endLineNum >= 0) && (tokens != null) ) {
      int lineNum = getMemWord( addr );
      if( (lineNum >= 0) && (lineNum < endLineNum) ) {
	StringBuilder buf = new StringBuilder( 0x2000 );
	while( (lineNum >= 0) && (lineNum <= endLineNum) ) {
	  buf.append( lineNum );
	  addr += 2;
	  boolean sep = false;
	  boolean spc = true;
	  int     ch  = this.emuThread.getMemByte( addr++, false );
	  while( (ch != 0) && (ch != 0x1E) ) {
	    if( spc ) {
	      buf.append( (char) '\u0020' );
	      sep = false;
	      spc = false;
	    }
	    if( ch >= 0xB0 ) {
	      int idx = ch - 0xB0;
	      if( (idx >= 0) && (idx < tokens.length) ) {
		String s = tokens[ idx ];
		if( s != null ) {
		  int len = s.length();
		  if( len > 0 ) {
		    char preCh = buf.charAt( buf.length() - 1 );
		    if( (preCh != ':') && (preCh != '\u0020')
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
	    if( ch != 0 ) {
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
	    ch = this.emuThread.getMemByte( addr++, false );
	  }
	  buf.append( (char) '\n' );
	  if( ch == 0 ) {
	    break;
	  }
	  if( lineNum == endLineNum ) {
	    break;
	  }
	  lineNum = getMemWord( addr );
	}
	if( buf.length() > 0 ) {
	  text = buf.toString();
	}
      }
    }
    if( text != null ) {
      this.screenFrm.openText( text );
    } else {
      showNoBasic();
    }
  }


  @Override
  public int readIOByte( int port, int tStates )
  {
    int rv = 0xFF;
    if( (port & 0x04) == 0 ) {		// A2=0
      rv = this.ctc.read( port & 0x03, tStates );
    }
    return rv;
  }


  @Override
  public int readMemByte( int addr, boolean m1 )
  {
    int rv = getMemByte( addr, m1 );

    addr &= 0xDFFF;		// A13=0
    if( (addr >= 0x1400) && (addr < 0x1800)) {
      this.emuThread.getZ80CPU().setWaitMode( true );
    }
    else if( (addr >= 0x1800) && (addr < 0x1C00)) {
      /*
       * Zugriff auf den Bildwiederholspeicher mit Auswertung von Bit 7:
       * Wenn Bit 7 nicht gesetzt ist, liest die CPU Null-Bytes
       * waehrend die Datenbytes auf dem Bildschirm ausgegeben werden.
       * Wenn Bit 7 gesetzt ist, liest die CPU die Datenbytes
       * und nichts wird auf dem Bildschirm ausgegeben.
       */
      int idx = addr - 0x1800;
      if( (idx >= 0) && (idx < this.ram.length) ) {
	int ch = (int) (this.ram[ idx ] & 0xFF);
	if( ((ch & 0x80) == 0)
	    && (this.screenLineNum >= SCREEN_HIDDEN_LINES) )
	{
	  if( this.fontBytes != null ) {
	    int dstPos = ((this.screenLineNum - SCREEN_HIDDEN_LINES)
					* SCREEN_CHARS_PER_ROW_MAX)
				+ this.screenColNum;
	    if( dstPos < this.screenPixelsWriting.length ) {
	      /*
	       * Die Pixeldaten in der Zeichensatzdatei sind vertikal
	       * um eine Pixelzeile verschoben.
	       */
	      int srcPos = (ch * 8) + ((this.screenLineInChar - 1) & 0x07);
	      if( (srcPos >= 0) && (srcPos < this.fontBytes.length) ) {
		this.screenPixelsWriting[ dstPos ] = this.fontBytes[ srcPos ];
		if( this.screenLineInChar == 0 ) {

		  // Anzahl Textzeilen aktualisieren
		  if( this.screenColNum == 0 ) {
		    this.screenRowNum++;
		  }

		  // Y-Anfangsposition der ersten beiden Textzeilen ermitteln
		  if( this.screenChar0Y <= 0 ) {
		    this.screenChar0Y = this.screenLineNum;
		  }
		  else if( (this.screenChar0Y > 0)
			   && (this.screenLineNum > this.screenChar0Y)
			   && (this.screenChar1Y <= 0) )
		  {
		    this.screenChar1Y = this.screenLineNum;
		  }

		  // ausgegebenes Zeichen in Textpuffer schreiben
		  int pos = (this.screenRowNum * SCREEN_CHARS_PER_ROW_MAX)
					+ this.screenColNum;
		  if( (pos >= 0) && (pos < this.screenChars.length) ) {
		    this.screenChars[ pos ] = (byte) ch;
		  }

		  // ggf. Anzahl der horizontalen Zeichen aktualisieren
		  if( this.screenColNum >= this.screenColCnt ) {
		    this.screenColCnt = this.screenColNum + 1;
		  }
		}
	      }
	    }
	  }
	  if( this.screenColNum < SCREEN_CHARS_PER_ROW_MAX ) {
	    this.screenColNum++;
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    super.reset( resetLevel, props );
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	&& isReloadExtROMsOnPowerOnEnabled( props ) )
    {
      loadROMs( props );
    }
    if( (resetLevel == EmuThread.ResetLevel.POWER_ON)
	|| (resetLevel == EmuThread.ResetLevel.COLD_RESET) )
    {
      initSRAM( this.ram, props );
      this.ctc.reset( true );
    } else {
      this.ctc.reset( false );
    }
    this.screenEnabled       = false;
    this.screenActiveTStates = 0;
    this.screenColCnt        = 0;
    this.screenColNum        = 0;
    this.screenRowNum        = -1;
    this.screenLineNum       = 0;
    this.screenLineInChar    = 0;
    this.screenChar0Y        = -1;
    this.screenChar1Y        = -1;
    this.lastTapeOutTStates  = 0;
    this.lastTapeOutPhase    = false;
    this.charRaster          = null;
    Arrays.fill( this.kbMatrix, 0 );
    Arrays.fill( this.screenChars, (byte) 0x20 );
  }


  @Override
  public void saveBasicProgram()
  {
    int begAddr    = -1;
    int endLineNum = -1;
    if( this.osBytes == osBytesSE24 ) {
      begAddr    = 0x3DA1;
      endLineNum = 9999;
    }
    else if( (this.osBytes == osBytesSE31_29)
	     || (this.osBytes == osBytesSE31_40) )
    {
      begAddr    = getMemWord( 0x3C00 );
      endLineNum = 9999;
    }
    else if( this.osBytes == osBytesSP33_29 ) {
      begAddr    = getMemWord( 0x3C00 );
      endLineNum = 9984;
    }
    int endAddr = begAddr;
    if( (begAddr >= 0) && (endLineNum >= 0) ) {
      int lineNum = getMemWord( begAddr );
      if( (lineNum >= 0) && (lineNum < endLineNum) ) {
	int addr = begAddr;
	while( (lineNum >= 0) && (lineNum <= endLineNum) ) {
	  addr += 2;
	  int ch = this.emuThread.getMemByte( addr++, false );
	  while( (ch != 0) && (ch != 0x1E) ) {
	    ch = this.emuThread.getMemByte( addr++, false );
	  }
	  if( ch == 0x1E ) {
	    endAddr = addr;
	  } else {
	    break;
	  }
	  lineNum = getMemWord( addr );
	}
      }
    }
    if( endAddr > begAddr + 1 ) {
      (new SaveDlg(
		this.screenFrm,
		begAddr,
		endAddr - 1,
		"BASIC-Programm speichern",
		SaveDlg.BasicType.OTHER_BASIC,
		EmuUtil.getBinaryFileFilter() )).setVisible( true );
    } else {
      showNoBasic();
    }
  }


  @Override
  public boolean setMemByte( int addr, int value )
  {
    addr &= 0xFFFF;

    boolean rv = false;
    if( addr < 0x4000 ) {
      addr &= 0xDFFF;		// A13=0
      if( (addr >= 0x1400) && (addr < 0x1800)) {
	this.emuThread.getZ80CPU().setWaitMode( true );
      }
      else if( (addr >= 0x1C00) && (addr < 0x2000)) {
	int idx = addr - 0x1C00;
	if( (idx >= 0) && (idx < this.ram.length) ) {
	  this.ram[ idx ] = (byte) value;
	}
	rv = true;
      }
    }
    else if( addr <= this.ramEndAddr ) {
      this.emuThread.setRAMByte( addr, value );
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean shouldAskConvertScreenChar()
  {
    return (this.fontBytes != fontBytesSE24)
		&& (this.fontBytes != fontBytesSE31)
		&& (this.fontBytes != fontBytesSP33);
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
  public void updKeyboardMatrix( int[] kbMatrix )
  {
    synchronized( this.kbMatrix ) {
      int n = Math.min( kbMatrix.length, this.kbMatrix.length );
      int i = 0;
      while( i < n ) {
	this.kbMatrix[ i ] = kbMatrix[ i ];
	i++;
      }
      while( i < this.kbMatrix.length ) {
	this.kbMatrix[ i ] = 0;
	i++;
      }
    }
  }


  @Override
  public void updSysCells(
                        int        begAddr,
                        int        len,
                        FileFormat fileFmt,
                        int        fileType )
  {
    int   prgBegAddr   = -1;
    int[] endInstBytes = null;
    if( this.osBytes == osBytesSE24 ) {
      prgBegAddr   = 0x3DA1;
      endInstBytes = endInstBytesSE24;
    }
    else if( (this.osBytes == osBytesSE31_29)
	     || (this.osBytes == osBytesSE31_40) )
    {
      prgBegAddr   = getMemWord( 0x3C00 );
      endInstBytes = endInstBytesSE31;
    }
    else if( this.osBytes == osBytesSP33_29 ) {
      prgBegAddr   = getMemWord( 0x3C00 );
      endInstBytes = endInstBytesSP33;
    }
    if( (prgBegAddr >= 0)
	&& (begAddr == prgBegAddr)
	&& (endInstBytes != null) )
    {
      boolean state = false;
      if( fileFmt != null ) {
	if( fileFmt.equals( FileFormat.HEADERSAVE ) ) {
	  if( fileType == 'B' ) {
	    state = true;
	  }
	}
	else if( fileFmt.equals( FileFormat.BASIC_PRG )
		 || fileFmt.equals( FileFormat.KCC )
		 || fileFmt.equals( FileFormat.INTELHEX )
		 || fileFmt.equals( FileFormat.BIN ) )
	{
	  state = true;
	}
      }
      if( state ) {
	for( int addr = begAddr; addr < 0x10000; addr++ ) {
	  int a = addr;
	  int b = 0;
	  while( b < endInstBytes.length ) {
	    if( ((int) endInstBytes[ b ] & 0xFF)
				!= this.emuThread.getMemByte( a, false ) )
	    {
	      b = -1;
	      break;
	    }
	    a++;
	    b++;
	  }
	  if( b == endInstBytes.length ) {
	    int prgLen = addr - begAddr + endInstBytes.length;
	    if( this.osBytes == osBytesSE24 ) {
	      this.emuThread.setMemWord( 0x3C06, prgLen );
	    } else {
	      this.emuThread.setMemWord( 0x3C02, prgLen );
	    }
	    break;
	  }
	}
      }
    }
  }


  @Override
  public void writeIOByte( int port, int value, int tStates )
  {
    if( (port & 0x04) == 0 )		// A2=0 
      this.ctc.write( port & 0x03, value, tStates );
  }


  @Override
  public void writeMemByte( int addr, int value )
  {
    setMemByte( addr, value );

    addr &= 0xDFFF;			// A13=0
    if( (addr >= 0x1400) && (addr < 0x1800)) {
      this.emuThread.getZ80CPU().setWaitMode( true );
    }
  }


  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    super.z80TStatesProcessed( cpu, tStates );
    this.ctc.z80TStatesProcessed( cpu, tStates );

    // Bildschirmausgabe ausschalten, wenn keine Bildsynchronimpulse kommen
    if( this.screenActiveTStates > 0 ) {
      this.screenActiveTStates -= tStates;
      if( this.screenActiveTStates <= 0 ) {
	if( this.screenEnabled ) {
	  this.screenEnabled = false;
	  this.screenFrm.setScreenDirty( true );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private static boolean equalsRange( byte[] data, int pos, int[] pattern )
  {
    int idx = 0;
    while( (pos < data.length) && (idx < pattern.length) ) {
      if( ((int) data[ pos ] & 0xFF) != pattern[ idx ] ) {
	break;
      }
      pos++;
      idx++;
    }
    return idx == pattern.length;
  }


  private static int getRAMEndAddr( Properties props )
  {
    int ramEndAddr = 0x3FFF;
    try {
      int kByte = Integer.parseInt(
			EmuUtil.getProperty(
				props, PROP_PREFIX + PROP_RAM_KBYTE ) );
      if( kByte > 1 ) {
	ramEndAddr += ((kByte - 1) * 1024);
	if( ramEndAddr > 0xEFFF ) {
	  ramEndAddr = 0xEFFF;
	}
      }
    }
    catch( NumberFormatException ex ) {}
    return ramEndAddr;
  }


  private static boolean getRemoveHSyncFromAudio( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			PROP_PREFIX + PROP_REMOVE_HSYNC_FROM_AUDIO,
			true );
  }


  private static boolean isIdentifierChar( int ch )
  {
    return ((ch >= 'A') && (ch <= 'Z'))
		|| ((ch >= 'a') && (ch <= 'z'))
		|| ((ch >= '0') && (ch <= '9'));
  }


  private void loadFont( Properties props )
  {
    this.fontBytes = readFontByProperty(
				props,
				this.propPrefix + PROP_FONT_FILE,
				0x0400 );
    if( this.fontBytes == null ) {
      if( this.osVersion == 31 ) {
	if( fontBytesSE31 == null ) {
	  fontBytesSE31 = readResource( "/rom/bcs3/se31font.bin" );
	}
	this.fontBytes = fontBytesSE31;
      } else if( this.osVersion == 33 ) {
	if( fontBytesSP33 == null ) {
	  fontBytesSP33 = readResource( "/rom/bcs3/sp33font.bin" );
	}
	this.fontBytes = fontBytesSP33;
      } else {
	if( fontBytesSE24 == null ) {
	  fontBytesSE24 = readResource( "/rom/bcs3/se24font.bin" );
	}
	this.fontBytes = fontBytesSE24;
      }
    }
  }


  private void loadROMs( Properties props )
  {
    this.osFile  = EmuUtil.getProperty(
				props,
				this.propPrefix + PROP_OS_FILE );
    this.osBytes = readROMFile( this.osFile, 0x1000, "Betriebssystem" );
    loadFont( props );
  }


  private void updTapeOutPhase( boolean phase )
  {
    if( this.removeHSyncFromAudio ) {
      Z80CPU cpu = this.emuThread.getZ80CPU();
      long t = cpu.getProcessedTStates();
      long d = cpu.calcTStatesDiff( this.lastTapeOutTStates, t );
      this.lastTapeOutTStates = t;
      if( (d > 300) && (d < 40000) ) {
	this.tapeOutPhase = phase;
      }
    } else {
      this.tapeOutPhase = phase;
    }
  }


  private void updKeyboardFld()
  {
    if( this.keyboardFld != null )
      this.keyboardFld.updKeySelection( this.kbMatrix );
  }
}
