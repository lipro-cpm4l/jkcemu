/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines am LC80ex angeschlossenen TV-Terminals 1.2
 */

package jkcemu.emusys.lc80;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.AbstractScreenDevice;
import jkcemu.base.AbstractScreenFrm;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.emusys.LC80;
import jkcemu.text.TextUtil;


public class TVTerminal
		extends AbstractScreenDevice
		implements KeyListener
{
  private static final int ROW_COUNT = 25;
  private static final int COL_COUNT = 40;
  private static final String START_MSG
	= "TV Terminal      (c)SP/RW 2007/2016 V1.2RS232 8,N,1 9600 Bd\r\n";

  private static byte[] romFont = null;

  private LC80                       lc80;
  private boolean                    ignoreKeyChar;
  private volatile CharacterIterator pasteIter;
  private int                        cursorX;
  private int                        cursorY;
  private int                        screenByteBuf;
  private boolean                    screenSimpleNewLine;
  private byte[][]                   screenRows;


  public TVTerminal( LC80 lc80, Properties props )
  {
    super( props );
    this.lc80                = lc80;
    this.ignoreKeyChar       = false;
    this.pasteIter           = null;
    this.cursorX             = 0;
    this.cursorY             = 0;
    this.screenByteBuf       = 0;
    this.screenSimpleNewLine = false;
    this.screenRows          = new byte[ ROW_COUNT ][];
    for( int i = 0; i < this.screenRows.length; i++ ) {
      this.screenRows[ i ] = new byte[ COL_COUNT ];
      Arrays.fill( this.screenRows[ i ], (byte) 0x20 );
    }
    if( romFont == null ) {
      romFont = readResource( "/rom/lc80/tvterm_font.bin" );
    }
  }


  public void keyCharQueueEmpty()
  {
    CharacterIterator iter = this.pasteIter;
    if( iter != null ) {
      char ch = iter.current();
      iter.next();
      if( ch == CharacterIterator.DONE ) {
	this.pasteIter = null;
	AbstractScreenFrm screenFrm = getScreenFrm();
	if( screenFrm != null ) {
	  screenFrm.firePastingTextFinished();
	}
      } else {
	if( ch == '\n' ) {
	  ch = '\r';
	}
	this.lc80.putToSIOChannelB( toLC80exCode( ch ) );
      }
    }
  }


  public void reset()
  {
    this.pasteIter     = null;
    this.screenByteBuf = 0;
    clearScreen();
    int len = START_MSG.length();
    for( int i = 0; i < len; i++ ) {
      write( START_MSG.charAt( i ) );
    }
  }


  public void write( int b )
  {
    if( this.screenByteBuf == 0x13 ) {
      // Cursor auf Spalte positionieren
      if( (b >= 0) && (b < COL_COUNT) ) {
	this.cursorX       = b;
	this.screenByteBuf = 0;
      }
    } else if( this.screenByteBuf == 0x14 ) {
      // Cursor auf Zeile positionieren
      if( (b >= 0) && (b < ROW_COUNT) ) {
	this.cursorY       = b;
	this.screenByteBuf = 0;
      }
    } else {
      switch( b ) {
	case 0x01:		// Cursor in linke obere Ecke (Home)
	  this.cursorX = 0;
	  this.cursorY = 0;
	  break;

	case 0x08:		// Cursor links
	  cursorLeft();
	  break;

	case 0x09:		// Cursor rechts
	  cursorRight();
	  break;

	case 0x0A:		// neue Zeile
	  if( this.screenSimpleNewLine ) {
	    newLine();
	  } else {
	    if( this.cursorY < (ROW_COUNT - 1) ) {
	      this.cursorY++;
	    } else {
	      scrollScreen();
	    }
	  }
	  break;

	case 0x0C:		// Bildschirm loeschen
	  clearScreen();
	  break;

	case 0x0D:		// Wagenruecklauf
	  if( this.screenSimpleNewLine ) {
	    newLine();
	  } else {
	    this.cursorX = 0;
	  }
	  break;

	case 0x0F:		// Tabulator
	  {
	    int tmpX  = this.cursorX + 8;
	    int steps = tmpX / 8;
	    this.cursorX = (steps * 8) + (tmpX % 8);
	  }
	  break;

	case 0x11:		// Modus "0D oder 0Ah" fuer neue Zeile
	  this.screenSimpleNewLine = true;
	  break;

	case 0x12:		// Modus "0D und 0Ah" fuer neue Zeile
	  this.screenSimpleNewLine = false;
	  break;

	case 0x13:		// Cursor positionieren
	case 0x14:
	  this.screenByteBuf = b;
	  break;

	case 0x7F:		// letztes Zeichen loeschen
	  if( cursorLeft() ) {
	    boolean dirty = false;
	    synchronized( this.screenRows ) {
	      if( (this.cursorY >= 0)
		  && (this.cursorY < this.screenRows.length) )
	      {
		byte[] screenRow = this.screenRows[ this.cursorY ];
		if( (this.cursorX >= 0)
		    && (this.cursorX < screenRow.length) )
		{
		  screenRow[ this.cursorX ] = 0x20;
		  dirty                     = true;
		}
	      }
	    }
	    if( dirty ) {
	      setScreenDirty();
	    }
	  }
	  break;

	default:
	  {
	    boolean dirty = false;
	    synchronized( this.screenRows ) {
	      if( (this.cursorY >= 0)
		  && (this.cursorY < this.screenRows.length) )
	      {
		byte[] screenRow = this.screenRows[ this.cursorY ];
		if( (this.cursorX >= 0)
		    && (this.cursorX < screenRow.length) )
		{
		  screenRow[ this.cursorX ] = (byte) b;
		  dirty                     = true;
		}
	      }
	      cursorRight();
	    }
	    if( dirty ) {
	      setScreenDirty();
	    }
	  }
	  break;
      }
    }
  }


	/* --- KeyListener --- */

  @Override
  public void keyPressed( KeyEvent e )
  {
    if( !e.isAltDown() ) {
      int ch             = 0;
      this.ignoreKeyChar = false;
      switch( e.getKeyCode() ) {
	case KeyEvent.VK_F1:
	  ch = 0xF1;
	  break;
	case KeyEvent.VK_F2:
	  ch = 0xF2;
	  break;
	case KeyEvent.VK_F3:
	  ch = 0xF3;
	  break;
	case KeyEvent.VK_F4:
	  ch = 0xF4;
	  break;
	case KeyEvent.VK_F5:
	  ch = 0xF5;
	  break;
	case KeyEvent.VK_F6:
	  ch = 0xF6;
	  break;
	case KeyEvent.VK_F7:
	  ch = 0xF7;
	  break;
	case KeyEvent.VK_F8:
	  ch = 0xF8;
	  break;
	case KeyEvent.VK_F9:
	  ch = 0xF9;
	  break;
	case KeyEvent.VK_F10:
	  ch = 0xFA;
	  break;
	case KeyEvent.VK_F11:
	  ch = 0xFB;
	  break;
	case KeyEvent.VK_F12:
	  ch = 0xFC;
	  break;
	case KeyEvent.VK_DOWN:
	  ch = 0x0A;
	  break;
	case KeyEvent.VK_UP:
	  ch = 0x0B;
	  break;
	case KeyEvent.VK_LEFT:
	  ch = 0x08;
	  break;
	case KeyEvent.VK_RIGHT:
	  ch = 0x09;
	  break;
	case KeyEvent.VK_ENTER:
	  ch = '\r';
	  break;
	case KeyEvent.VK_SPACE:
	  ch = 0x20;
	  break;
	case KeyEvent.VK_BACK_SPACE:
	  ch = 0x7F;
	  break;
	case KeyEvent.VK_J:
	  if( e.isControlDown() ) {
	    ch = '\n';
	  }
	  break;
	case KeyEvent.VK_M:
	  if( e.isControlDown() ) {
	    ch = '\r';
	  }
	  break;
      }
      if( ch > 0 ) {
	this.lc80.putToSIOChannelB( ch );
	this.ignoreKeyChar = true;
	e.consume();
      }
    }
  }


  @Override
  public void keyReleased( KeyEvent e )
  {
    if( !e.isAltDown() ) {
      /*
       * Das Loslassen von F10 nicht melden,
       * da F10 von Java selbst verwendet wird.
       */
      if( e.getKeyCode() != KeyEvent.VK_F10 ) {
	this.ignoreKeyChar = false;
	e.consume();
      }
    }
  }


  @Override
  public void keyTyped( KeyEvent e )
  {
    if( !this.ignoreKeyChar && !e.isAltDown() ) {
      char ch = e.getKeyChar();
      if( (ch > 0) && (ch < 0x7F) ) {
	ch = toLC80exCode( TextUtil.toReverseCase( ch ) );
      } else {
	ch = 0;
      }
      if( ch > 0 ) {
	this.lc80.putToSIOChannelB( ch );
      }
      this.ignoreKeyChar = false;
      e.consume();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void cancelPastingText()
  {
    this.pasteIter = null;
    AbstractScreenFrm screenFrm = getScreenFrm();
    if( screenFrm != null ) {
      screenFrm.firePastingTextFinished();
    }
  }


  @Override
  public boolean canExtractScreenText()
  {
    return true;
  }


  @Override
  public int getColorIndex( int x, int y )
  {
    int rv   = BLACK;
    int rPix = y % 9;
    if( (rPix >= 0) && (rPix < 9) ) {
      byte[] fontBytes = romFont;
      if( fontBytes != null ) {
	int col = x / 8;
	int row = y / 9;
	synchronized( this.screenRows ) {
	  if( (row >= 0) && (row < this.screenRows.length) ) {
	    byte[] screenRow = this.screenRows[ row ];
	    if( screenRow != null ) {
	      if( (col >= 0) && (col < screenRow.length) ) {
		int ch = (int) screenRow[ col ] & 0xFF;
		if( (rPix == 8) && (ch >= 0x80) && (ch <= 0xBF) ) {
		  /*
		   * Bei den Blockgrafiksymbolen 80h-0BFh
		   * ist die neunte Pixelzeile gleich der achten.
		   */
		  rPix = 7;
		}
		if( rPix < 8 ) {
		  int b   = 0;
		  int fIdx = (ch * 8) + rPix;
		  if( (fIdx >= 0) && (fIdx < fontBytes.length) ) {
		    b = (int) fontBytes[ fIdx ] & 0xFF;
		  }
		  if( b != 0 ) {
		    int m = 0x80;
		    int n = x % 8;
		    if( n > 0 ) {
		      m >>= n;
		    }
		    if( (b & m) != 0 ) {
		      rv = WHITE;
		    }
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    return new CharRaster( COL_COUNT, ROW_COUNT, 9, 9, 8, 0 );
  }


  @Override
  public EmuThread getEmuThread()
  {
    return this.lc80.getEmuThread();
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch = -1;
    synchronized( this.screenRows ) {
      if( (chY >= 0) && (chY < this.screenRows.length) ) {
	byte[] screenRow = this.screenRows[ chY ];
	if( screenRow != null ) {
	  if( (chX >= 0) && (chX < screenRow.length) ) {
	    int b = (int) screenRow[ chX ] & 0xFF;
	    if( (b >= 0x20) && (b < 0x7F) ) {
	      ch = b;
	    } else {
	      switch( b ) {
		case 0xC0:		// Ae
		  ch = '\u00C4';
		  break;
		case 0xC1:		// Oe
		  ch = '\u00D6';
		  break;
		case 0xC2:		// Ue
		  ch = '\u00DC';
		  break;
		case 0xC3:		// ae
		  ch = '\u00E4';
		  break;
		case 0xC4:		// oe
		  ch = '\u00F6';
		  break;
		case 0xC5:		// ue
		  ch = '\u00FC';
		  break;
		case 0xC6:		// sz
		  ch = '\u00DF';
		  break;
		case 0xC7:		// Paragraph-Zeichen
		  ch = '\u00A7';
		  break;
		default:
		  ch = 0x20;
	      }
	    }
	  }
	}
      }
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return ROW_COUNT * 9;
  }


  @Override
  public int getScreenWidth()
  {
    return COL_COUNT * 8;
  }


  @Override
  public String getTitle()
  {
    return Main.APPNAME + ": TV Terminal 1.2";
  }


  @Override
  public void startPastingText( String text )
  {
    this.pasteIter = new StringCharacterIterator( text );
    keyCharQueueEmpty();
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


	/* --- private Methoden --- */

  private void clearScreen()
  {
    synchronized( this.screenRows ) {
      for( int i = 0; i < this.screenRows.length; i++ ) {
	Arrays.fill( this.screenRows[ i ], (byte) 0x20 );
      }
    }
    this.cursorX = 0;
    this.cursorY = 0;
    setScreenDirty();
  }


  private boolean cursorLeft()
  {
    boolean rv = false;
    if( this.cursorX > 0 ) {
      --this.cursorX;
      rv = true;
    } else {
      this.cursorX = COL_COUNT - 1;
      if( this.cursorY > 0 ) {
	--this.cursorY;
	rv = true;
      }
    }
    return rv;
  }


  private void cursorRight()
  {
    if( this.cursorX < (COL_COUNT - 1) ) {
      this.cursorX++;
    } else {
      newLine();
    }
  }


  private void newLine()
  {
    this.cursorX = 0;
    if( this.cursorY < (ROW_COUNT - 1) ) {
      this.cursorY++;
    } else {
      scrollScreen();
    }
  }


  private byte[] readResource( String resource )
  {
    return EmuUtil.readResource( this.lc80.getScreenFrm(), resource );
  }


  private void scrollScreen()
  {
    synchronized( this.screenRows ) {
      byte[] tmpRow = this.screenRows[ 0 ];
      for( int i = 1; i < this.screenRows.length; i++ ) {
	this.screenRows[ i - 1 ] = this.screenRows[ i ];
      }
      Arrays.fill( tmpRow, (byte) 0x20 );
      this.screenRows[ this.screenRows.length - 1 ] = tmpRow;
    }
  }

  private void setScreenDirty()
  {
    AbstractScreenFrm screenFrm = getScreenFrm();
    if( screenFrm != null ) {
      screenFrm.setScreenDirty( true );
    }
  }


  private static char toLC80exCode( char ch )
  {
    switch( ch ) {
      case '\u00C4':		// Ae
	ch = '\u00C0';
	break;
      case '\u00D6':		// Oe
	ch = '\u00C1';
	break;
      case '\u00DC':		// Ue
	ch = '\u00C2';
	break;
      case '\u00E4':		// ae
	ch = '\u00C3';
	break;
      case '\u00F6':		// oe
	ch = '\u00C4';
	break;
      case '\u00FC':		// ue
	ch = '\u00C5';
	break;
      case '\u00DF':		// sz
	ch = '\u00C6';
	break;
      case '\u00A7':		// Paragraph-Zeichen
	ch = '\u00C7';
	break;
    }
    return ch;
  }
}
