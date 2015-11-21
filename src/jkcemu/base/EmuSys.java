/*
 * (c) 2008-2017 Jens Mueller
 * (c) 2014-2017 Stephan Linz
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer das emulierte System
 */

package jkcemu.base;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.image.ImageObserver;
import java.io.File;
import java.lang.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JOptionPane;
import jkcemu.Main;
import jkcemu.base.ScreenFrm;
import jkcemu.disk.FloppyDiskDrive;
import jkcemu.disk.FloppyDiskFormat;
import jkcemu.disk.FloppyDiskInfo;
import jkcemu.etc.Plotter;
import jkcemu.etc.VDIP;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;
import z80emu.Z80MaxSpeedListener;
import z80emu.Z80Memory;
import z80emu.Z80MemView;
import z80emu.Z80TStatesListener;


public abstract class EmuSys
			extends AbstractScreenDevice
			implements
				ImageObserver,
				Runnable,
				Z80MaxSpeedListener,
				Z80TStatesListener
{
  public static final String PROP_COLOR        = "color";
  public static final String PROP_COUNT        = "count";
  public static final String PROP_FDC_ENABLED  = "fdc.enabled";
  public static final String PROP_FILE         = "file";
  public static final String PROP_FONT_PREFIX  = "font.";
  public static final String PROP_FONT_FILE    = PROP_FONT_PREFIX + PROP_FILE;
  public static final String PROP_BASIC_PREFIX = "basic.";
  public static final String PROP_OS_PREFIX    = "os.";
  public static final String PROP_OS_FILE      = PROP_OS_PREFIX + PROP_FILE;
  public static final String PROP_OS_VERSION   = PROP_OS_PREFIX + "version";
  public static final String PROP_ROM_PREFIX   = "rom.";
  public static final String PROP_MODEL        = "model";
  public static final String PROP_CATCH_PRINT_CALLS = "catch_print_calls";
  public static final String PROP_FIXED_SCREEN_SIZE = "fixed_screen_size";
  public static final String PROP_KCNET_ENABLED     = "kcnet.enabled";
  public static final String PROP_ROMMEGA_ENABLED   = "rom_mega.enabled";
  public static final String PROP_PASTE_FAST        = "paste.fast";
  public static final String PROP_RF_PREFIX         = "ramfloppy.";
  public static final String PROP_RF1_PREFIX        = "ramfloppy.1.";
  public static final String PROP_RF2_PREFIX        = "ramfloppy.2.";
  public static final String PROP_RTC_ENABLED       = "rtc.enabled";
  public static final String PROP_VDIP_ENABLED      = "vdip.enabled";

  public static final String VALUE_NONE        = "none";
  public static final String VALUE_PREFIX_FILE = "file:";

  public static final int     DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX = 500;
  public static final boolean DEFAULT_SWAP_KEY_CHAR_CASE            = false;

  public enum Chessman {
		WHITE_PAWN,
		WHITE_KNIGHT,
		WHITE_BISHOP,
		WHITE_ROOK,
		WHITE_QUEEN,
		WHITE_KING,
		BLACK_PAWN,
		BLACK_KNIGHT,
		BLACK_BISHOP,
		BLACK_ROOK,
		BLACK_QUEEN,
		BLACK_KING };

  protected EmuThread                  emuThread;
  protected ScreenFrm                  screenFrm;
  protected String                     propPrefix;
  protected Thread                     pasteThread;
  protected volatile CharacterIterator pasteIter;
  protected volatile boolean           soundOutPhase;
  protected volatile boolean           tapeOutPhase;

  private int          curSoundOutTStates;
  private int          curTapeOutTStates;
  private int          soundOutTStates;
  private int          tapeOutTStates;
  private volatile int soundOutFrameRate;
  private volatile int tapeOutFrameRate;

  private static final int CHESSBOARD_SQUARE_WIDTH = 48;


  public EmuSys(
		EmuThread  emuThread,
		Properties props,
		String     propPrefix )
  {
    super( props );
    this.emuThread          = emuThread;
    this.screenFrm          = emuThread.getScreenFrm();
    this.pasteThread        = null;
    this.pasteIter          = null;
    this.propPrefix         = propPrefix;
    this.curSoundOutTStates = 0;
    this.soundOutTStates    = 0;
    this.soundOutFrameRate  = 0;
    this.soundOutPhase      = false;
    this.curTapeOutTStates  = 0;
    this.tapeOutTStates     = 0;
    this.tapeOutFrameRate   = 0;
    this.tapeOutPhase       = false;
  }


  public static void appendSpacesToCol(
				StringBuilder buf,
				int           begOfLine,
				int           col )
  {
    int n = begOfLine + col - buf.length();
    while( n > 0 ) {
      buf.append( (char) '\u0020' );
      --n;
    }
  }


  public void appendStatusHTMLTo( StringBuilder buf, Z80CPU cpu )
  {
    // leer
  }


  /*
   * Wenn sich die Einstellungen geaendert haben,
   * wird mit dieser Methode geprueft,
   * ob die neuen Einstellungen auf das gegebene EmuSys-Objekt angewendet
   * werden koennen, d.h. ob die neuen Einstellungen kompatibel sind
   * und somit die Methode applySettings(...) aufgerufen werden kann.
   * Wenn nein, wird ein neue neue EmuSys-Instanz angelegt.
   */
  public boolean canApplySettings( Properties props )
  {
    return false;
  }


  public AbstractKeyboardFld createKeyboardFld()
		throws UnsupportedOperationException, UserCancelException
  {
    throw new UnsupportedOperationException();
  }


  public void die()
  {
    // leer
  }


  protected void fillRandom( byte[] a )
  {
    EmuUtil.fillRandom( a, 0 );
  }


  /*
   * Die Methode zeigt einen Dialog mit Meldung an,
   * dass ein Zeichen nicht eingefuegt werden konnte.
   * Die Methode kann von jedem Thread heraus aufgerufen werden.
   */
  protected void fireShowCharNotPasted( final CharacterIterator iter )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    showCharNotPasted( iter );
		  }
		} );
  }


  /*
   * Die Methode liefert den Wert, auf den der Stackpointer vor einem
   * durch JKCEMU initiierten Programmstart gesetzt wird.
   * Bei einem negativen Wert wird der Stackpointer nicht gesetzt.
   */
  public int getAppStartStackInitValue()
  {
    return -1;
  }


  public boolean getAutoLoadInputOnSoftReset()
  {
    return true;
  }


  public int getBasicMemByte( int addr )
  {
    return getMemByte( addr, false );
  }


  public Chessman getChessman( int row, int col )
  {
    return null;
  }


  protected boolean getConvertKeyCharToISO646DE()
  {
    return true;
  }


  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return null;
  }


  public int getDefaultPromptAfterResetMillisMax()
  {
    return DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX;
  }


  protected long getDelayMillisAfterPasteChar()
  {
    return 150;
  }


  protected long getDelayMillisAfterPasteEnter()
  {
    return 250;
  }


  protected long getHoldMillisPasteChar()
  {
    return 100;
  }


  public String getHelpPage()
  {
    return null;
  }


  public Integer getLoadAddr()
  {
    return null;
  }


  public abstract int getMemByte( int addr, boolean m1 );


  public int getMemWord( int addr )
  {
    return (getMemByte( addr + 1, false ) << 8) | getMemByte( addr, false );
  }


  public int getResetStartAddress( EmuThread.ResetLevel resetLevel )
  {
    return 0;
  }


  public Plotter getPlotter()
  {
    return null;
  }


  public String getPropPrefix()
  {
    return this.propPrefix;
  }


  public AbstractScreenDevice getSecondScreenDevice()
  {
    return null;
  }


  public String getSecondSystemName()
  {
    return null;
  }


  public Z80CPU getSecondZ80CPU()
  {
    return null;
  }


  public Z80Memory getSecondZ80Memory()
  {
    return null;
  }


  public FloppyDiskInfo[] getSuitableFloppyDisks()
  {
    return null;
  }


  public int getSupportedFloppyDiskDriveCount()
  {
    return 0;
  }


  public int getSupportedJoystickCount()
  {
    return 0;
  }


  public boolean getSwapKeyCharCase()
  {
    return DEFAULT_SWAP_KEY_CHAR_CASE;
  }


  public File getUSBMemStickDirectory()
  {
    VDIP vdip = getVDIP();
    return vdip != null ? vdip.getMemStickDirectory() : null;
  }


  public boolean getUSBMemStickForceCurrentTimestamp()
  {
    VDIP vdip = getVDIP();
    return vdip != null ?
		vdip.getMemStickForceCurrentTimestamp()
		: true;
  }


  public boolean getUSBMemStickForceLowerCaseFileNames()
  {
    VDIP vdip = getVDIP();
    return vdip != null ?
		vdip.getMemStickForceLowerCaseFileNames()
		: false;
  }


  public boolean getUSBMemStickReadOnly()
  {
    VDIP vdip = getVDIP();
    return vdip != null ? vdip.getMemStickReadOnly() : false;
  }


  protected VDIP getVDIP()
  {
    return null;
  }


  public boolean hasKCBasicInROM()
  {
    return false;
  }


  public ScreenFrm getScreenFrm()
  {
    return this.screenFrm;
  }


  protected void initSRAM( byte[] a, Properties props )
  {
    if( a != null ) {
      if( EmuUtil.getProperty(
		props,
		EmuThread.PROP_SRAM_INIT ).equalsIgnoreCase(
					EmuThread.VALUE_SRAM_INIT_RANDOM ) )
      {
	fillRandom( a );
      } else {
	Arrays.fill( a, (byte) 0 );
      }
    }
  }


  public boolean isAutoScreenRefresh()
  {
    return false;
  }


  public boolean isPastingText()
  {
    return this.pasteThread != null;
  }


  protected boolean isReloadExtROMsOnPowerOnEnabled( Properties props )
  {
    return EmuUtil.getBooleanProperty(
			props,
			EmuThread.PROP_EXT_ROM_RELOAD_ON_POWER_ON,
			EmuThread.DEFAULT_EXT_ROM_RELOAD_ON_POWER_ON );
  }


  public boolean isSecondSystemRunning()
  {
    return false;
  }


  public void openBasicProgram()
  {
    showFunctionNotSupported();
  }


  public boolean keyPressed(
			int     keyCode,
			boolean ctrlDown,
			boolean shiftDown )
  {
    return false;
  }


  public void keyReleased()
  {
    // leer
  }


  public boolean keyTyped( char keyChar )
  {
    return false;
  }


  public void loadIntoSecondSystem(
			byte[] loadData,
			int    loadAddr,
			int    startAddr )
  {
    // leer
  }


  /*
   * Ueber diese Methode werden Dateien in den Arbeitsspeicher
   * geladen.
   * Soll bei Systemen mit Bank-Switching eine Datei in eine andere
   * als die gerade aktuelle Bank geladen werden,
   * muss die Methode ueberschrieben werden.
   */
  public void loadIntoMem(
			int        begAddr,
			byte[]     data,
			int        idx,
			int        len,
			FileFormat fileFmt,
			int        fileType )
  {
    if( data != null ) {
      int n   = len;
      int dst = begAddr;
      while( (idx < data.length) && (dst < 0x10000) && (n > 0) ) {
	setMemByte( dst++, data[ idx++ ] );
	--n;
      }
      updSysCells( begAddr, len, fileFmt, fileType );
    }
  }


  protected boolean pasteChar( char ch ) throws InterruptedException
  {
    boolean rv = false;
    switch( ch ) {
      case '\n':
      case '\r':
	rv = keyPressed( KeyEvent.VK_ENTER, false, false );
	break;

      case '\u0020':
	rv = keyPressed( KeyEvent.VK_SPACE, false, false );
	break;

      default:
	rv = keyTyped( ch );
    }
    if( rv ) {
      long millis = getHoldMillisPasteChar();
      if( millis > 0L ) {
	Thread.sleep( millis );
      }
      keyReleased();
    }
    return rv;
  }


  protected byte[] readFontByProperty(
				Properties props,
				String     propName,
				int        maxLen )
  {
    return props != null ?
		EmuUtil.readFile(
			this.emuThread.getScreenFrm(),
			props.getProperty( propName ),
			true,
			maxLen,
			"Zeichensatzdatei" )
		: null;
  }


  public int readIOByte( int port, int tStates )
  {
    return 0xFF;
  }


  public int readMemByte( int addr, boolean m1 )
  {
    return getMemByte( addr, m1 );
  }


  protected byte[] readResource( String resource )
  {
    return EmuUtil.readResource( this.screenFrm, resource );
  }


  protected byte[] readRAMFile( String fileName, int maxLen, String objName )
  {
    return EmuUtil.readFile(
			this.emuThread.getScreenFrm(),
			fileName,
			true,
			maxLen,
			objName );
  }


  protected byte[] readROMFile( String fileName, int maxLen, String objName )
  {
    return EmuUtil.readFile(
			this.emuThread.getScreenFrm(),
			fileName,
			true,
			maxLen,
			objName );
  }


  /*
   * Diese Methode reassembliert Bytes als Zeichenkette
   * bis einschliesslich das Byte, bei dem Bit 7 gesetzt ist.
   *
   * Rueckgabewert: Anzahl der reassemlierten Bytes
   */
  public static int reassStringBit7(
			Z80MemView    memory,
			int           addr,
			StringBuilder buf,
			boolean       sourceOnly,
			int           colMnemonic,
			int           colArgs )
  {
    int     rv   = 0;
    boolean loop = true;
    while( loop ) {
      int     a = addr;
      int     n = 0;
      long    r = 0;
      boolean c = false;
      while( (n < 5) && (a <= 0xFFFF) ) {
	int b = memory.getMemByte( a, false );
	if( n == 0 ) {
	  c = ((b >= 0x20) && (b < 0x7F));
	} else {
	  if( ((b >= 0x20) && (b < 0x7F)) != c ) {
	    break;
	  }
	}
	r = (r << 8) | b;
	n++;
	a++;
	if( (b & 0x80) != 0 ) {
	  loop = false;
	  break;
	}
      }
      if( a > 0xFFFF ) {
	loop = false;
      }
      if( n > 0 ) {
	int begOfLine = buf.length();
	if( !sourceOnly ) {
	  buf.append( String.format( "%04X ", addr ) );
	}
	long m1 = 0;
	for( int i = 0; i < n; i++ ) {
	  m1 = (m1 << 8) | (r & 0xFF);
	  r >>= 8;
	}
	long m2 = m1;
	if( !sourceOnly ) {
	  for( int i = 0; i < n; i++ ) {
	    buf.append( String.format( " %02X", (int) m1 & 0xFF ) );
	    m1 >>= 8;
	  }
	}
	appendSpacesToCol( buf, begOfLine, colMnemonic );
	buf.append( "DB" );
	appendSpacesToCol( buf, begOfLine, colArgs );

	boolean first = true;
	boolean quote = false;
	for( int i = 0; i < n; i++ ) {
	  int b = (int) m2 & 0xFF;
	  if( (b & 0x80) != 0 ) {
	    if( quote ) {
	      buf.append( (char) '\'' );
	      quote = false;
	    }
	    if( first ) {
	      first = false;
	    } else {
	      buf.append( (char) ',' );
	    }
	    if( (b >= 0xA0) && (b < 0xFF) ) {
	      buf.append( "80H+\'" );
	      buf.append( (char) (b & 0x7F) );
	      buf.append( (char) '\'' );
	    } else {
	      if( b >= 0xA0 ) {
		buf.append( (char) '0' );
	      }
	      buf.append( String.format( "%02XH", b ) );
	    }
	  } else {
	    if( (b >= 0x20) && (b < 0x7F) ) {
	      if( !quote ) {
		if( first ) {
		  first = false;
		} else {
		  buf.append( (char) ',' );
		}
		buf.append( (char) '\'' );
		quote = true;
	      }
	      buf.append( (char) b );
	    } else {
	      if( quote ) {
		buf.append( (char) '\'' );
		quote = false;
	      }
	      if( first ) {
		first = false;
	      } else {
		buf.append( (char) ',' );
	      }
	      buf.append( String.format( "%02XH", b ) );
	    }
	  }
	  m2 >>= 8;
	}
	if( quote ) {
	  buf.append( (char) '\'' );
	}
	buf.append( (char) '\n' );
	addr += n;
	rv   += n;
      }
    }
    return rv;
  }


  /*
   * Diese Methode reassembliert einen Aufruf in einer Sprungtabelle.
   *
   * Rueckgabewert: Anzahl der reassemlierten Bytes
   */
  protected int reassSysCallTable(
			Z80MemView    memory,
			int           addr,
			int           sysCallTableAddr,
			String[]      sysCallNames,
			StringBuilder buf,
			boolean       sourceOnly,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    int rv = 0;
    String s = null;
    int    b = memory.getMemByte( addr, true );
    switch( b ) {
      case 0xC3:
	s = "JP";
	break;
      case 0xCD:
	s = "CALL";
	break;
    }
    if( s != null ) {
      int w = memory.getMemWord( addr + 1 );
      if( w >= sysCallTableAddr ) {
	int m = w - sysCallTableAddr;
	int idx = m / 3;
	if( ((idx * 3) == m) && (idx < sysCallNames.length) ) {
	  int bol = buf.length();
	  if( !sourceOnly ) {
	    buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w & 0xFF,
				w >> 8 ) );
	  }
	  appendSpacesToCol( buf, bol, colMnemonic );
	  buf.append( s );
	  appendSpacesToCol( buf, bol, colArgs );
	  if( w >= 0xA000 ) {
	    buf.append( (char) '0' );
	  }
	  buf.append( String.format( "%04XH", w ) );
	  appendSpacesToCol( buf, bol, colRemark );
	  buf.append( (char) ';' );
	  buf.append( sysCallNames[ idx ] );
	  buf.append( (char) '\n' );
	  rv = 3;
	}
      }
    }
    return rv;
  }


  /*
   * Diese Methode wird vom Reassembler aufgerufen,
   * bevor der Befehl an der uebergebenen Adresse reassembliert wird.
   * Damit kann das emulierte System Einfluss auf die Reassemblierung
   * nehmen, z.B. bei Systemaufrufen.
   * Insbesondere wenn hinter Systemaufrufen Datenbytes stehen,
   * die nicht als Befehle uebersetzt werden sollen,
   * muss die Methode ueberschrieben werden.
   *
   * An an Puffer muessen immer vollstaendige Zeilen angehaengt werden.
   * Die Formatierung wird durch die restlichen drei Argumente angegeben.
   *
   * Rueckgabewert: Anzahl der reassemlierten Bytes
   */
  public int reassembleSysCall(
			Z80MemView    memory,
			int           addr,
			StringBuilder buf,
			boolean       sourceOnly,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    return 0;		// kein Byte reassembliert
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    this.curSoundOutTStates = 0;
    this.curTapeOutTStates  = 0;
    this.soundOutPhase      = false;
    this.tapeOutPhase       = false;
  }


  public void saveBasicProgram()
  {
    showFunctionNotSupported();
  }


  public boolean setBasicMemByte( int addr, int value )
  {
    return setMemByte( addr, value );
  }


  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    // leer
  }


  public void setJoystickAction( int joyNum, int actionMask )
  {
    // leer
  }


  public abstract boolean setMemByte( int addr, int value );


  protected void showNoBasic()
  {
    BaseDlg.showErrorDlg(
	this.screenFrm,
	"Es ist kein BASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }


  public void setUSBMemStickDirectory( File dirFile )
  {
    VDIP vdip = getVDIP();
    if( vdip != null ) {
      vdip.setMemStickDirectory( dirFile );
    }
  }


  public void setUSBMemStickForceCurrentTimestamp( boolean state )
  {
    VDIP vdip = getVDIP();
    if( vdip != null ) {
      vdip.setMemStickForceCurrentTimestamp( state );
    }
  }


  public void setUSBMemStickForceLowerCaseFileNames( boolean state )
  {
    VDIP vdip = getVDIP();
    if( vdip != null ) {
      vdip.setMemStickForceLowerCaseFileNames( state );
    }
  }


  public void setUSBMemStickReadOnly( boolean state )
  {
    VDIP vdip = getVDIP();
    if( vdip != null ) {
      vdip.setMemStickReadOnly( state );
    }
  }


  public void soundOutFrameRateChanged( int frameRate )
  {
    this.soundOutFrameRate = frameRate;
    updSoundOutTStates();
  }


  public boolean supportsAudio()
  {
    return (supportsTapeIn()
		|| supportsTapeOut()
		|| supportsSoundOutMono()
		|| supportsSoundOutStereo());
  }


  public boolean supportsChessboard()
  {
    return false;
  }


  public boolean supportsKeyboardFld()
  {
    return false;
  }


  public boolean supportsOpenBasic()
  {
    return false;
  }


  public boolean supportsPrinter()
  {
    return false;
  }


  public boolean supportsRAMFloppy1()
  {
    return false;
  }


  public boolean supportsRAMFloppy2()
  {
    return false;
  }


  public boolean supportsRAMFloppies()
  {
    return supportsRAMFloppy1() || supportsRAMFloppy2();
  }


  public boolean supportsSaveBasic()
  {
    return false;
  }


  public boolean supportsSoundOut8Bit()
  {
    return false;
  }


  public boolean supportsSoundOutMono()
  {
    return false;
  }


  public boolean supportsSoundOutStereo()
  {
    return false;
  }


  public boolean supportsTapeIn()
  {
    return false;
  }


  public boolean supportsTapeOut()
  {
    return false;
  }


  public boolean supportsUSB()
  {
    return (getVDIP() != null);
  }


  public void tapeOutFrameRateChanged( int frameRate )
  {
    this.tapeOutFrameRate = frameRate;
    updTapeOutTStates();
  }


  public void updKeyboardMatrix( int[] kbMatrix )
  {
    // leer
  }


  public void updDebugScreen()
  {
    // leer
  }


  public void updSysCells(
		int        begAddr,
		int        len,
		FileFormat fileFmt,
		int        fileType )
  {
    // leer
  }


  public void writeIOByte( int port, int value, int tStates )
  {
    // leer
  }


  public void writeMemByte( int addr, int value )
  {
    setMemByte( addr, value );
  }


	/* --- ImageObserver --- */

  @Override
  public boolean imageUpdate(
			Image img,
			int   flags,
			int   x,
			int   y,
			int   w,
			int   h )
  {
    return (flags & (ALLBITS | FRAMEBITS)) != 0;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      long    delay   = 0L;
      boolean isFirst = true;
      while( this.pasteThread != null ) {

	// kurze Wartezeit vor dem naechsten Zeichen
	if( delay > 0L ) {
	  Thread.sleep( delay );
	}

	// naechstes Zeichen holen und uebergeben
	CharacterIterator iter = this.pasteIter;
	if( iter != null ) {
	  char ch = '\u0000';
	  if( isFirst ) {
	    keyReleased();
	    Thread.sleep( 100 );
	    ch      = iter.first();
	    isFirst = false;
	  } else {
	    ch = iter.next();
	  }
	  if( ch == CharacterIterator.DONE ) {
	    cancelPastingText();
	  } else {
	    if( getConvertKeyCharToISO646DE() ) {
	      ch = TextUtil.toISO646DE( ch );
	    }
	    if( pasteChar( ch ) ) {
	      if( (ch == '\n') || (ch == '\r') ) {
		delay = getDelayMillisAfterPasteEnter();
	      } else {
		delay = getDelayMillisAfterPasteChar();
	      }
	    } else {
	      if( this.pasteThread != null ) {
		cancelPastingText();
		fireShowCharNotPasted( iter );
	      }
	    }
	  }
	}
      }
    }
    catch( InterruptedException ex ) {}
    finally {
      this.screenFrm.firePastingTextFinished();
    }
  }


	/* --- Z80MaxSpeedListener --- */

  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    if( cpu == this.emuThread.getZ80CPU() ) {
      updSoundOutTStates();
      updTapeOutTStates();
    }
  }


	/* --- Z80TStatesListener --- */

  @Override
  public void z80TStatesProcessed( Z80CPU cpu, int tStates )
  {
    if( this.soundOutTStates > 0 ) {
      if( this.curSoundOutTStates > 0 ) {
	this.curSoundOutTStates -= tStates;
      } else {
	this.curSoundOutTStates = this.soundOutTStates;
	this.emuThread.writeSoundOutPhase( this.soundOutPhase );
      }
    }
    if( this.tapeOutTStates > 0 ) {
      if( this.curTapeOutTStates > 0 ) {
	this.curTapeOutTStates -= tStates;
      } else {
	this.curTapeOutTStates = this.tapeOutTStates;
	this.emuThread.writeTapeOutPhase( this.tapeOutPhase );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public synchronized void cancelPastingText()
  {
    Thread thread = this.pasteThread;
    if( thread != null ) {
      try {
	thread.interrupt();
      }
      catch( Exception ex ) {}
      this.pasteThread = null;
    }
    if( this.pasteIter != null ) {
      this.pasteIter = null;
      this.screenFrm.firePastingTextFinished();
    }
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    keyReleased();
		  }
		} );
  }


  @Override
  public EmuThread getEmuThread()
  {
    return this.emuThread;
  }


  @Override
  public synchronized void startPastingText( String text )
  {
    boolean done = false;
    if( text != null ) {
      if( !text.isEmpty() ) {
	cancelPastingText();
	this.pasteIter   = new StringCharacterIterator( text );
	this.pasteThread = new Thread(
				Main.getThreadGroup(),
				this,
				"JKCEMU text paste" );
	this.pasteThread.start();
	done = true;
      }
    }
    if( !done ) {
      this.screenFrm.firePastingTextFinished();
    }
  }


	/* --- private Methoden --- */

  private void showCharNotPasted( CharacterIterator iter )
  {
    String title     = "Text einf\u00FCgen";
    int    remainLen = iter.getEndIndex() - iter.getIndex() - 1;
    if( remainLen > 0 ) {
      if( BaseDlg.showYesNoWarningDlg(
		this.screenFrm,
		String.format(
			"Das Zeichen mit dem hexadezimalen Code %02X\n"
				+ "kann nicht eingef\u00FCgt werden.\n"
				+ "M\u00F6chten Sie die restlichen"
				+ " Zeichen einf\u00FCgen?",
			(int) iter.current() ),
		title ) )
      {
	StringBuilder buf = new StringBuilder( remainLen );
	char          ch  = iter.next();
	while( ch != CharacterIterator.DONE ) {
	  buf.append( ch );
	  ch = iter.next();
	}
	if( buf.length() > 0 ) {
	  startPastingText( buf.toString() );
	}
      }
    } else {
      BaseDlg.showWarningDlg(
	this.screenFrm,
	String.format(
		"Das letzte Zeichen (hexadezimaler Code %02X)\n"
			+ "kann nicht eingef\u00FCgt werden.",
		(int) iter.current() ),
	title );
    }
  }


  private void showFunctionNotSupported()
  {
    BaseDlg.showErrorDlg(
	this.screenFrm,
	"Diese Funktion steht f\u00FCr das gerade emulierte System\n"
		+ "nicht zur Verf\u00FCgung." );
  }


  private void updSoundOutTStates()
  {
    int frameRate = this.soundOutFrameRate;
    if( (frameRate > 0)
	&& supportsSoundOutMono()
	&& !supportsSoundOutStereo()
	&& !supportsSoundOut8Bit() )
    {
      this.soundOutTStates = Math.min(
		this.emuThread.getZ80CPU().getMaxSpeedKHz() * 300 / frameRate,
		1 );
    } else {
      this.soundOutTStates = 0;
    }
  }


  private void updTapeOutTStates()
  {
    int frameRate = this.tapeOutFrameRate;
    if( (frameRate > 0) && supportsTapeOut() ) {
      this.tapeOutTStates = Math.min(
		this.emuThread.getZ80CPU().getMaxSpeedKHz() * 100 / frameRate,
		1 );
    } else {
      this.tapeOutTStates = 0;
    }
  }
}
