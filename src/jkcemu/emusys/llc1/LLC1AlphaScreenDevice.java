/*
 * (c) 2016-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des LLC1
 */

package jkcemu.emusys.llc1;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Properties;
import jkcemu.Main;
import jkcemu.base.AbstractScreenDevice;
import jkcemu.base.AbstractScreenFrm;
import jkcemu.base.CharRaster;
import jkcemu.base.EmuThread;
import jkcemu.emusys.LLC1;
import jkcemu.text.TextUtil;


public class LLC1AlphaScreenDevice
			extends AbstractScreenDevice
			implements KeyListener
{
  private LLC1 llc1;


  public LLC1AlphaScreenDevice( LLC1 llc1, Properties props )
  {
    super( props );
    this.llc1 = llc1;
  }


	/* --- KeyListener --- */

  @Override
  public void keyPressed( KeyEvent e )
  {
    int ch = 0;
    switch( e.getKeyCode() ) {
      case KeyEvent.VK_DOWN:
	ch = 0x04;
	break;

      case KeyEvent.VK_UP:
	ch = 0x24;
	break;

      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_BACK_SPACE:
	ch = 0x08;
	break;

      case KeyEvent.VK_RIGHT:
	ch = 0x09;
	break;

      case KeyEvent.VK_ENTER:
	ch = 0x0D;
	break;

      case KeyEvent.VK_SPACE:
	ch = 0x40;
	break;
    }
    if( ch > 0 ) {
      this.llc1.putAlphaKeyChar( ch );
      e.consume();
    }
  }


  @Override
  public void keyReleased( KeyEvent e )
  {
    this.llc1.putAlphaKeyChar( 0 );
    e.consume();
  }


  @Override
  public void keyTyped( KeyEvent e )
  {
    char ch = e.getKeyChar();
    if( (ch > 0) && (ch < 0x7F) ) {
      ch = TextUtil.toReverseCase( ch );
    } else {
      ch = 0;
    }
    if( ch > 0 ) {
      this.llc1.putAlphaKeyChar( ch );
      e.consume();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void cancelPastingText()
  {
    this.llc1.cancelPastingAlphaText();
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
    int rPix = y % 14;
    if( (rPix >= 0) && (rPix < 8) ) {
      byte[] fontBytes = this.llc1.getAlphaScreenFontBytes();
      if( fontBytes != null ) {
	int col = x / 8;
	int row = y / 14;
	int ch  = this.llc1.getMemByte( 0x1C00 + (row * 64) + col, false );
	int b   = 0;
	if( (rPix == 7) && ((ch & 0x80) != 0) ) {
	  b = 0xFF;		// Cursor als Strich in 7. Pixelzeile
	} else {
	  int fIdx = (rPix * 128) + (ch & 0x7F);
	  if( (fIdx >= 0) && (fIdx < fontBytes.length) ) {
	    b = (int) fontBytes[ fIdx ] & 0xFF;
	  }
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
    return rv;
  }


  @Override
  public CharRaster getCurScreenCharRaster()
  {
    return new CharRaster( 64, 16, 14, 8, 8 );
  }


  @Override
  public EmuThread getEmuThread()
  {
    return this.llc1.getEmuThread();
  }


  @Override
  public KeyListener getKeyListener()
  {
    return this;
  }


  @Override
  protected int getScreenChar( CharRaster chRaster, int chX, int chY )
  {
    int ch = -1;
    int b  = this.llc1.getMemByte( 0x1C00 + (chY * 64) + chX, false );
    switch( b ) {
      case 0x00:
      case 0x01:
      case 0x02:
      case 0x03:
      case 0x04:
      case 0x0F:
      case 0x20:
      case 0x21:
      case 0x22:
      case 0x23:
      case 0x24:
      case 0x40:
      case 0x5F:
      case 0x60:
	ch = '\u0020';
	break;
      case 0x0E:
	ch = '\u00B7';
	break;
      case 0x10:
	ch = '/';
	break;
      case 0x11:
	ch = ';';
	break;
      case 0x12:
	ch = '\"';
	break;
      case 0x13:
	ch = '=';
	break;
      case 0x14:
	ch = '%';
	break;
      case 0x15:
	ch = '&';
	break;
      case 0x16:
	ch = '(';
	break;
      case 0x17:
	ch = ')';
	break;
      case 0x18:
	ch = '_';
	break;
      case 0x19:
	/*
	 * Dieser Code stellt beim LLC1 optische das Paragraph-Zeichen dar.
	 * Er wird aber im BASIC fuer das Variablen-Array verwendet,
	 * was normalerweise durch den Klammeraffen dargestellt wird.
	 * Aus diesem Grund wird es hier auch auf den Klammeraffen gemappt.
	 */
	ch = '@';
	break;
      case 0x1A:
	ch = ':';
	break;
      case 0x1B:
	ch = '#';
	break;
      case 0x1C:
	ch = '*';
	break;
      case 0x1D:
	ch = '\'';
	break;
      case 0x1E:
	ch = '!';
	break;
      case 0x1F:
	ch = '?';
	break;
      case 0x3A:
	ch = '\u00AC';
	break;
      case 0x3B:
	ch = '$';
	break;
      case 0x3C:
	ch = '+';
	break;
      case 0x3D:
	ch = '-';
	break;
      case 0x3E:
	ch = '.';
	break;
      case 0x3F:
	ch = ',';
	break;
      case 0x5B:
	ch = ']';
	break;
      case 0x5C:
	ch = '[';
	break;
      case 0x5E:
	ch = '\u0060';
	break;
      case 0x7B:
	ch = '>';
	break;
      case 0x7C:
	ch = '<';
	break;
      case 0x7D:
	ch = '|';
	break;
      case 0x7E:
	ch = '^';
	break;
      default:
	if( ((b >= '0') && (b <= '9'))
	    || ((b >= 'A') && (b <= 'Z'))
	    || ((b >= 'a') && (b <= 'z')) )
	{
	  ch = b;
	}
    }
    return ch;
  }


  @Override
  public int getScreenHeight()
  {
    return 16 * 14;
  }


  @Override
  public int getScreenWidth()
  {
    return 64 * 8;
  }


  @Override
  public String getTitle()
  {
    return Main.APPNAME + ": LLC1 Alphanumerischer Bildschirm";
  }


  @Override
  public void startPastingText( String text )
  {
    this.llc1.startPastingAlphaText( text );
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
}
