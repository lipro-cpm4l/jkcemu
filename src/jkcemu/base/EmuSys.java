/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer das emulierte System
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.ImageObserver;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.JOptionPane;
import jkcemu.base.ScreenFrm;
import jkcemu.disk.*;
import z80emu.*;


public abstract class EmuSys implements ImageObserver
{
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

  protected static final int BLACK = 0;
  protected static final int WHITE = 1;

  protected EmuThread emuThread;
  protected ScreenFrm screenFrm;
  protected Color     colorWhite;
  protected Color     colorRedLight;
  protected Color     colorRedDark;
  protected Color     colorGreenLight;
  protected Color     colorGreenDark;

  private static final int CHESSBOARD_SQUARE_WIDTH = 48;

  // Basiskoordinaten eines horizontalen Segments einer 7-Segment-Anzeige
  private static final int[] base7SegHXPoints = { 0, 3, 31, 34, 31, 3, 0 };
  private static final int[] base7SegHYPoints = { 3, 0, 0, 3, 6, 6, 3 };

  // Basiskoordinaten eines vertikalen Segments einer 7-Segment-Anzeige
  private static final int[] base7SegVXPoints = { 3, 0, 4, 7, 10, 6, 3 };

  //private static final int[] base7SegVYPoints = { 40, 37, 8, 5, 8, 37, 40 };
  private static final int[] base7SegVYPoints = { 5, 2, -27, -30, -27, 2, 5 };

  private static int[] tmp7SegXPoints = new int[ base7SegHXPoints.length ];
  private static int[] tmp7SegYPoints = new int[ base7SegHYPoints.length ];


  public EmuSys( EmuThread emuThread, Properties props )
  {
    this.emuThread  = emuThread;
    this.screenFrm  = emuThread.getScreenFrm();
    createColors( props );
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


  public void applySettings( Properties props )
  {
    createColors( props );
  }


  public boolean canExtractScreenText()
  {
    return false;
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
   * Die Methode liefert den Wert, auf den der Stackpointer vor einem
   * durch JKCEMU initiierten Programmstart gesetzt wird.
   * Bei einem negativen Wert wird der Stackpointer nicht gesetzt.
   */
  public int getAppStartStackInitValue()
  {
    return -1;
  }


  public int getBorderColorIndex()
  {
    return BLACK;
  }


  /*
   * Die Helligkeit wird logarithmisch gewertet,
   * damit man auch im unteren und mittleren Einstellbereich
   * noch etwas sieht.
   */
  protected static double getBrightness( Properties props )
  {
    int value = EmuUtil.getIntProperty(
				props,
				"jkcemu.brightness",
				SettingsFrm.DEFAULT_BRIGHTNESS );
    double rv = 1.0;
    if( (value > 0) && (value < 100) ) {
      rv = 1.0 - Math.abs( Math.log10( (double) (value + 10) / 110.0 ) );
    } else {
      rv = (double) value / 100.0;
    }
    if( rv < 0.0 ) {
      rv = 0.0;
    } else if( rv > 1.0 ) {
      rv = 1.0;
    }
    return rv;
  }


  public Chessman getChessman( int row, int col )
  {
    return null;
  }


  public Color getColor( int colorIdx )
  {
    return colorIdx == WHITE ? this.colorWhite : Color.black;
  }


  public int getColorCount()
  {
    return 2;		// schwarz/weiss
  }


  public int getColorIndex( int x, int y )
  {
    return WHITE;
  }


  public int getCharColCount()
  {
    return -1;
  }


  public int getCharHeight()
  {
    return -1;
  }


  public int getCharRowCount()
  {
    return -1;
  }


  public int getCharRowHeight()
  {
    return -1;
  }


  public int getCharWidth()
  {
    return -1;
  }


  /*
   * Die Methoden "getDefaultFloppyDisk..." geben das Standardformat
   * fuer Disketten an,
   * sofern ueberhaupt ein Format als Standard bezeichnet werden kann.
   */
  public boolean getDefaultFloppyDiskBlockNum16Bit()
  {
    return false;
  }


  public int getDefaultFloppyDiskBlockSize()
  {
    return -1;
  }


  public int getDefaultFloppyDiskDirBlocks()
  {
    return -1;
  }


  public FloppyDiskFormat getDefaultFloppyDiskFormat()
  {
    return null;
  }


  public int getDefaultFloppyDiskSystemTracks()
  {
    return -1;
  }


  public long getDelayMillisAfterPasteChar()
  {
    return 150;
  }


  public long getDelayMillisAfterPasteEnter()
  {
    return 250;
  }


  public long getHoldMillisPasteChar()
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


  /*
   * Die Methode liefert entsprechend der eingestellten Helligkeit
   * den max. Wert fuer die jeweiligen Primaerfarben.
   */
  public static int getMaxRGBValue( Properties props )
  {
    int    value      = 255 * SettingsFrm.DEFAULT_BRIGHTNESS / 100;
    double brightness = getBrightness( props );
    if( (brightness >= 0.0) && (brightness <= 1.0) ) {
      value = (int) Math.round( 255 * brightness );
    }
    return value;
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


  protected int getScreenChar( int chX, int chY )
  {
    return -1;
  }


  public String getScreenText()
  {
    return getScreenText( 0, 0, getCharColCount() - 1, getCharRowCount() - 1 );
  }


  public String getScreenText( int chX1, int chY1, int chX2, int chY2 )
  {
    String rv = null;
    if( (chX1 >= 0) && (chY1 >= 0) ) {
      int nCols = getCharColCount();
      int nRows = getCharRowCount();
      if( (nCols > 0) && (nRows > 0) ) {
	if( chY2 >= nRows ) {
	  chY2 = nRows - 1;
	}
	StringBuilder buf     = new StringBuilder( nRows * (nCols + 1) );
	int           nSpaces = 0;
	while( (chY1 < chY2)
	       || ((chY1 == chY2) && (chX1 <= chX2)) )
	{
	  int b = getScreenChar( chX1, chY1 );
	  if( (b == 0) || b == 0x20 ) {
	    if( chY1 < chY2 ) {
	      nSpaces++;
	    } else {
	      buf.append( (char) '\u0020' );
	    }
	  } else {
	    while( nSpaces > 0 ) {
	      buf.append( (char) '\u0020' );
	      --nSpaces;
	    }
	    buf.append( (char) (b > 0 ? b : '_') );
	  }
	  chX1++;
	  if( chX1 >= nCols ) {
	    buf.append( (char) '\n' );
	    nSpaces = 0;
	    chX1    = 0;
	    chY1++;
	  }
	}
	if( buf.length() > 0 ) {
	 rv = buf.toString();
	}
      }
    }
    return rv;
  }


  public String getSecondarySystemName()
  {
    return null;
  }


  public Z80CPU getSecondaryZ80CPU()
  {
    return null;
  }


  public Z80Memory getSecondaryZ80Memory()
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


  public int getSupportedRAMFloppyCount()
  {
    return 0;
  }


  public boolean getSwapKeyCharCase()
  {
    return false;
  }


  public abstract int    getScreenHeight();
  public abstract int    getScreenWidth();
  public abstract String getTitle();


  public boolean hasKCBasicInROM()
  {
    return false;
  }


  protected void initSRAM( byte[] a, Properties props )
  {
    if( a != null ) {
      if( EmuUtil.getProperty(
		props,
		"jkcemu.sram.init" ).toLowerCase().startsWith( "r" ) )
      {
	fillRandom( a );
      } else {
	Arrays.fill( a, (byte) 0 );
      }
    }
  }


  /*
   * Die Methode besagt, * ob an der uebergebenen Adresse
   * ein extern eingebundenes ROM-Image eingeblendet werden kann.
   * Abgeleitete Klassen koennen die Methode ueberschreiben,
   * um z.B. bei Bankswitching das extern eingebundene ROM-Image
   * nur in der ROM-Ebene einzublenden.
   */
  protected boolean isExtROMSwitchableAt( int addr )
  {
    return true;
  }


  /*
   * Diese Methode besagt, ob der Zeichensatz Zeichen
   * nach der deutschen Variante von ISO646 enthaelt.
   */
  public boolean isISO646DE()
  {
    return false;
  }


  public boolean isSecondarySystemRunning()
  {
    return false;
  }


  public void openBasicProgram()
  {
    showFunctionNotSupported();
  }


  public boolean keyPressed( int keyCode, boolean shiftDown )
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


  /*
   * Malen einer Stelle einer 7-Segment-Anzeige
   * in der Basisgroesse 50x85 (BxH)
   *
   * Kodierung der Segmente
   *   A: Bit0
   *   B: Bit1
   *   C: Bit2
   *   D: Bit3
   *   E: Bit4
   *   F: Bit6
   *   G: Bit7
   *   P: Bit8
   */
  protected static void paint7SegDigit(
				Graphics g,
				int      x,
				int      y,
				int      v,
				Color    d,
				Color    l,
				int      f )
  {
    paint7SegH( g, x + (14 * f), y + (0 * f), f, (v & 0x01) != 0 ? l : d );
    paint7SegV( g, x + (44 * f), y + (35 * f), f, (v & 0x02) != 0 ? l : d );
    paint7SegV( g, x + (40 * f), y + (75 * f), f, (v & 0x04) != 0 ? l : d );
    paint7SegH( g, x + (6 * f), y + (80 * f), f, (v & 0x08) != 0 ? l : d );
    paint7SegV( g, x + (0 * f), y + (75 * f), f, (v & 0x10) != 0 ? l : d );
    paint7SegV( g, x + (4 * f), y + (35 * f), f, (v & 0x20) != 0 ? l : d );
    paint7SegH( g, x + (10 * f), y + (40 * f), f, (v & 0x40) != 0 ? l : d );
    g.setColor( (v & 0x80) != 0 ? l : d );
    g.fillArc(
	x + (47 * f),
	y + (80 * f),
	5 * f,
	5 * f,
	0,
	360 );
  }


  /*
   * Durch Ueberschreiben dieser Methode hat das emulierte System
   * die Moeglichkeit,
   * selbst die Bildschirmausgabe grafisch darzustellen.
   * Wenn nicht (Rueckgabewert false) werden die Methoden getColorCount()
   * und getColorIndex( x, y ) ausgerufen.
   */
  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    return false;
  }


  public boolean pasteChar( char ch )
  {
    boolean rv = false;
    switch( ch ) {
      case '\n':
      case '\r':
	rv = keyPressed( KeyEvent.VK_ENTER, false );
	break;

      case '\u0020':
	rv = keyPressed( KeyEvent.VK_SPACE, false );
	break;

      default:
	rv = keyTyped( ch );
    }
    if( rv ) {
      long millis = getHoldMillisPasteChar();
      if( millis > 0L ) {
	try {
	  Thread.sleep( millis );
	}
	catch( InterruptedException ex ) {}
      }
      keyReleased();
    }
    return rv;
  }


  protected byte[] readFile( String fileName, int maxLen, String objName )
  {
    byte[] rv = null;
    if( fileName != null ) {
      if( !fileName.isEmpty() ) {
	try {
	  rv = EmuUtil.readFile( new File( fileName ), maxLen );
	}
	catch( IOException ex ) {
	  String msg = ex.getMessage();
	  BasicDlg.showErrorDlg(
			this.emuThread.getScreenFrm(),
			String.format(
				"%s kann nicht geladen werden%s%s",
				objName,
				msg != null ? ":\n" : ".",
				msg != null ? msg : "" ) );
	  rv = null;
	}
      }
    }
    return rv;
  }


  protected byte[] readFileByProperty(
				Properties props,
				String     propName,
				int        maxLen,
				String     objName )
  {
    return props != null ?
		readFile(
			props.getProperty( propName ),
			maxLen,
			objName )
		: null;
  }


  public int readIOByte( int port )
  {
    return 0xFF;
  }


  public int readMemByte( int addr, boolean m1 )
  {
    return getMemByte( addr, m1 );
  }


  protected byte[] readResource( String resource )
  {
    return EmuUtil.readResource( this.emuThread.getScreenFrm(), resource );
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
	buf.append( String.format( "%04X ", addr ) );
	addr = a;

	long m1 = 0;
	for( int i = 0; i < n; i++ ) {
	  m1 = (m1 << 8) | (r & 0xFF);
	  r >>= 8;
	}
	long m2 = m1;

	for( int i = 0; i < n; i++ ) {
	  buf.append( String.format( " %02X", (int) m1 & 0xFF ) );
	  m1 >>= 8;
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
	rv += n;
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
			int           addr,
			int           sysCallTableAddr,
			String[]      sysCallNames,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    int rv = 0;
    String s = null;
    int    b = getMemByte( addr, true );
    switch( b ) {
      case 0xC3:
	s = "JP";
	break;
      case 0xCD:
	s = "CALL";
	break;
    }
    if( s != null ) {
      int w = getMemWord( addr + 1 );
      if( w >= sysCallTableAddr ) {
	int m = w - sysCallTableAddr;
	int idx = m / 3;
	if( ((idx * 3) == m) && (idx < sysCallNames.length) ) {
	  int bol = buf.length();
	  buf.append( String.format(
				"%04X  %02X %02X %02X",
				addr,
				b,
				w & 0xFF,
				w >> 8 ) );
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
			int           addr,
			StringBuilder buf,
			int           colMnemonic,
			int           colArgs,
			int           colRemark )
  {
    return 0;		// kein Byte reassembliert
  }


  public boolean requiresReset( Properties props )
  {
    return true;
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    // leer
  }


  public void saveBasicProgram()
  {
    showFunctionNotSupported();
  }


  public void setFloppyDiskDrive( int idx, FloppyDiskDrive drive )
  {
    // leer
  }


  public abstract boolean setMemByte( int addr, int value );


  protected void showNoBasic()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Es ist kein BASIC-Programm im entsprechenden\n"
		+ "Adressbereich des Arbeitsspeichers vorhanden." );
  }


  public boolean supportsAudio()
  {
    return false;
  }


  public boolean supportsChessboard()
  {
    return false;
  }


  public boolean supportsCopyToClipboard()
  {
    return false;
  }


  public boolean supportsPasteFromClipboard()
  {
    return false;
  }


  public void updSysCells(
		int    begAddr,
		int    len,
		Object fileFmt,
		int    fileType )
  {
    // leer
  }


  public void writeIOByte( int port, int value )
  {
    // leer
  }


  public void writeMemByte( int addr, int value )
  {
    setMemByte( addr, value );
  }


	/* --- ImageObserver --- */

  public boolean imageUpdate( Image img, int flags, int x, int y, int w, int h )
  {
    return (flags & (ALLBITS | FRAMEBITS)) != 0;
  }


	/* --- private Methoden --- */

  private void createColors( Properties props )
  {
    int value            = getMaxRGBValue( props );
    this.colorWhite      = new Color( value, value, value );
    this.colorRedLight   = new Color( value, 0, 0 );
    this.colorRedDark    = new Color( value / 5, 0, 0 );
    this.colorGreenLight = new Color( 0, value, 0 );
    this.colorGreenDark  = new Color( 0, value / 8, 0 );
  }


  /*
   * Zeichnen eines horizontalen Segments einer 7-Segment-Anzeige
   * Laenge: 35, Hoehe: 7
   *
   * Parameter:
   *   x, y: linke Spitze
   */
  private static void paint7SegH(
			Graphics g,
			int      x,
			int      y,
			int      f,
			Color    color )
  {
    paint7Seg( g, x, y, f, base7SegHXPoints, base7SegHYPoints, color );
  }


  /*
   * Zeichnen eines vertikalen Segments einer 7-Segment-Anzeige
   * Hoehe:35 , Breite: 7 + 4 durch Neigung
   *
   * Parameter:
   *   x, y: untere Spitze
   */
  private static void paint7SegV(
			Graphics g,
			int      x,
			int      y,
			int      f,
			Color    color )
  {
    paint7Seg( g, x, y, f, base7SegVXPoints, base7SegVYPoints, color );
  }


  private static void paint7Seg(
			Graphics g,
			int      x,
			int      y,
			int      f,
			int[]    baseXPoints,
			int[]    baseYPoints,
			Color    color )
  {
    if( color != null ) {
      for( int i = 0; i < tmp7SegXPoints.length; i++ ) {
	tmp7SegXPoints[ i ] = x + (baseXPoints[ i ] * f);
	tmp7SegYPoints[ i ] = y + (baseYPoints[ i ] * f);
      }
      g.setColor( color );
      g.fillPolygon( tmp7SegXPoints, tmp7SegYPoints, tmp7SegXPoints.length );
    }
  }


  private void showFunctionNotSupported()
  {
    BasicDlg.showErrorDlg(
	this.screenFrm,
	"Diese Funktion steht f\u00Fcr das gerade emulierte System\n"
		+ "nicht zur Verf\u00FCgung." );
  }
}
