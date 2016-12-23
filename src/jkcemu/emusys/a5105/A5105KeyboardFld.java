/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die A5105-Tastatur
 */

package jkcemu.emusys.a5105;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.lang.*;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.emusys.A5105;
import jkcemu.image.ImgUtil;


public class A5105KeyboardFld extends AbstractKeyboardFld<A5105>
{
  private static final int TEXT_FONT_SIZE   = 9;
  private static final int LETTER_FONT_SIZE = 14;
  private static final int DIGIT_FONT_SIZE  = 12;
  private static final int LED_SIZE         = 8;
  private static final int KEY_SIZE         = 40;
  private static final int KEY_HALF_SIZE    = KEY_SIZE / 2;
  private static final int MEDIUM_KEY_SIZE  = KEY_SIZE / 4 * 5;
  private static final int LARGE_KEY_SIZE   = KEY_SIZE / 2 * 3;
  private static final int SPACE_KEY_SIZE   = KEY_SIZE * 8;
  private static final int MARGIN           = 20;

  private Image imgKey40x40;
  private Image imgKey50x40;
  private Image imgKey60x40;
  private Image imgKey320x40;
  private Image imgLeft;
  private Image imgRight;
  private Image imgUp;
  private Image imgDown;
  private Image imgHome;
  private Image imgShift;
  private Color colorLEDGreenOn;
  private Color colorLEDGreenOff;
  private Color colorLEDYellowOn;
  private Color colorLEDYellowOff;
  private Font  fontText;
  private Font  fontLetter;
  private Font  fontDigit;
  private int[] kbMatrix;
  private int   curIdx;
  private int   curX;
  private int   curY;
  private int   xRow1Left;
  private int   xRow1Right;
  private int   xRow3Right;


  public A5105KeyboardFld( A5105 a5105 )
  {
    super( a5105, 69, true );
    this.imgKey40x40  = getImage( "/images/keyboard/key40x40.png" );
    this.imgKey50x40  = getImage( "/images/keyboard/key50x40.png" );
    this.imgKey60x40  = getImage( "/images/keyboard/key60x40.png" );
    this.imgKey320x40 = getImage( "/images/keyboard/key320x40.png" );
    this.imgLeft      = getImage( "/images/keyboard/a5105/left.png" );
    this.imgRight     = getImage( "/images/keyboard/a5105/right.png" );
    this.imgUp        = getImage( "/images/keyboard/a5105/up.png" );
    this.imgDown      = getImage( "/images/keyboard/a5105/down.png" );
    this.imgHome      = getImage( "/images/keyboard/a5105/home.png" );
    this.imgShift     = getImage( "/images/keyboard/a5105/shift.png" );

    this.colorLEDGreenOn   = Color.green;
    this.colorLEDGreenOff  = new Color( 60, 120, 60 );
    this.colorLEDYellowOn  = Color.yellow;
    this.colorLEDYellowOff = new Color( 120, 120, 0 );

    this.fontText   = new Font( "SansSerif", Font.PLAIN, TEXT_FONT_SIZE );
    this.fontLetter = new Font( "SansSerif", Font.PLAIN, LETTER_FONT_SIZE );
    this.fontDigit  = new Font( "SansSerif", Font.PLAIN, DIGIT_FONT_SIZE );
    this.kbMatrix   = new int[ 9 ];
    this.curIdx     = 0;
    this.curX       = MARGIN;
    this.curY       = MARGIN;
    addKey( "PF 1", null, 7, 0x02, "F1" );
    this.curX += (KEY_SIZE / 2);
    this.xRow1Left = this.curX;
    addKey( this.imgHome, 8, 0x02, "F6 oder Pos1" );
    addKey( "1", "!", 0, 0x02 );
    addKey( "2", "\"", 0, 0x04 );
    addKey( "3", "\\", 0, 0x08 );
    addKey( "4", "$", 0, 0x10 );
    addKey( "5", "%", 0, 0x20 );
    addKey( "6", "&", 0, 0x40 );
    addKey( "7", "/", 0, 0x80 );
    addKey( "8", "(", 1, 0x01 );
    addKey( "9", ")", 1, 0x02 );
    addKey( "0", "=", 0, 0x01 );
    addKey( "?", "\u00DF", 2, 0x02 );
    addKey( "#", "^", 1, 0x80 );
    addKey( "\'", "\u0060", 2, 0x01 );
    this.xRow1Right = this.curX - 1;
    this.curX += (KEY_SIZE * 3 / 4);
    int xCrsMiddle = this.curX + (LARGE_KEY_SIZE / 2);
    addLargeKey( "STOP", 7, 0x10, "F7" );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "PF 2", null, 7, 0x01, "F2" );
    this.curX += KEY_SIZE;
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					KEY_SIZE,
					KEY_SIZE,
					"CTRL",
					null,
					null,
					null,
					null,
					6,
					0x04,
					true,
					null );
    this.curX += KEY_SIZE;
    addKey( "Q", 4, 0x40 );
    addKey( "W", 5, 0x10 );
    addKey( "E", 3, 0x04 );
    addKey( "R", 4, 0x80 );
    addKey( "T", 5, 0x02 );
    addKey( "Z", 5, 0x80 );
    addKey( "U", 5, 0x04 );
    addKey( "I", 3, 0x40 );
    addKey( "O", 4, 0x10 );
    addKey( "P", 4, 0x20 );
    addKey( "\u00DC", 1, 0x40 );
    addKey( "<", ">", 1, 0x04 );
    this.curX = xCrsMiddle - (KEY_SIZE / 2);
    addKey( this.imgUp, 8, 0x20 );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "PF 3", null, 6, 0x80, "F3" );
    this.curX += KEY_SIZE;
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					MEDIUM_KEY_SIZE,
					KEY_SIZE,
					"CAPS\nLOCK",
					null,
					null,
					null,
					null,
					6,
					0x08,
					false,
					null );
    this.curX += MEDIUM_KEY_SIZE;
    addKey( "A", 2, 0x40 );
    addKey( "S", 5, 0x01 );
    addKey( "D", 3, 0x02 );
    addKey( "F", 3, 0x08 );
    addKey( "G", 3, 0x10 );
    addKey( "H", 3, 0x20 );
    addKey( "J", 3, 0x80 );
    addKey( "K", 4, 0x01 );
    addKey( "L", 4, 0x02 );
    addKey( "\u00D6", 1, 0x10 );
    addKey( "\u00C4", 1, 0x20 );
    addKey( "+", "*", 1, 0x08 );
    this.xRow3Right = this.curX - 1;
    this.curX = xCrsMiddle - KEY_SIZE;
    addKey( this.imgLeft, 8, 0x10 );
    addKey( this.imgRight, 8, 0x80 );

    int w = this.curX + MARGIN;

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "PF 4", null, 6, 0x40, "F4" );
    this.curX += (KEY_SIZE / 2);
    addKey( "ESC", 6, 0x02 );

    KeyData shiftKey1 = new KeyData(
			this.curX,
			this.curY,
			MEDIUM_KEY_SIZE,
			KEY_SIZE,
			null,
			null,
			null,
			null,
			this.imgShift,
			6,
			0x01,
			true,
			null );
    this.keys[ this.curIdx++ ] = shiftKey1;
    this.curX += MEDIUM_KEY_SIZE;
    addKey( "Y", 5, 0x40 );
    addKey( "X", 5, 0x20 );
    addKey( "C", 3, 0x01 );
    addKey( "V", 5, 0x08 );
    addKey( "B", 2, 0x80 );
    addKey( "N", 4, 0x08 );
    addKey( "M", 4, 0x04 );
    addKey( ",", ";", 2, 0x04 );
    addKey( ".", ":", 2, 0x08 );
    addKey( "-", "_", 2, 0x10 );

    KeyData shiftKey2 = new KeyData(
			this.curX,
			this.curY,
			LARGE_KEY_SIZE,
			KEY_SIZE,
			null,
			null,
			null,
			null,
			this.imgShift,
			6,
			0x01,
			true,
			null );
    this.keys[ this.curIdx++ ] = shiftKey2;
    this.curX = xCrsMiddle - (KEY_SIZE / 2);
    addKey( this.imgDown, 8, 0x40 );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "PF 5", null, 6, 0x20, "F5" );
    this.curX += ((KEY_SIZE / 2) + (2 * KEY_SIZE) - MEDIUM_KEY_SIZE);
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					KEY_SIZE,
					KEY_SIZE,
					"GRAPH",
					null,
					null,
					null,
					null,
					7,
					0x04,
					true,
					null );
    this.curX += KEY_SIZE;
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					KEY_SIZE,
					KEY_SIZE,
					"ALT",
					null,
					null,
					null,
					null,
					6,
					0x10,
					true,
					null );
    this.curX += KEY_SIZE;
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					8 * KEY_SIZE,
					KEY_SIZE,
					null,
					null,
					null,
					null,
					null,
					8,
					0x01,
					false,
					null );
    this.curX += (8 * KEY_SIZE);
    addKey( "INS\nMODE", null, 8, 0x04, "Einfg" );
    addKey( "DEL", null, 8, 0x08, "Entf" );
    this.curX = xCrsMiddle - (LARGE_KEY_SIZE / 2);
    addLargeKey( "ENTER", 7, 0x80, null );

    int h = this.curY + KEY_SIZE + MARGIN;
    setPreferredSize( new Dimension( w, h ) );
    setShiftKeys( shiftKey1, shiftKey2 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof A5105;
  }


  @Override
  public boolean getSelectionChangeOnShiftOnly()
  {
    return true;
  }


  @Override
  protected void keySelectionChanged()
  {
    Arrays.fill( this.kbMatrix, 0 );
    synchronized( this.selectedKeys ) {
      for( KeyData key : this.selectedKeys ) {
	if( (key.col >= 0) && (key.col < this.kbMatrix.length) ) {
	  this.kbMatrix[ key.col ] |= key.value;
	}
      }
    }
    this.emuSys.updKeyboardMatrix( this.kbMatrix );
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    g.setPaintMode();
    g.setColor( Color.lightGray );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    for( KeyData key : this.keys ) {
      if( isKeySelected( key ) ) {
	g.setColor( Color.gray );
	g.fillRect( key.x, key.y, key.w, key.h );
      }
      switch( key.w ) {
	case KEY_SIZE:
	  if( this.imgKey40x40 != null ) {
	    g.drawImage( this.imgKey40x40, key.x, key.y, this );
	  }
	  break;
	case MEDIUM_KEY_SIZE:
	  if( this.imgKey50x40 != null ) {
	    g.drawImage( this.imgKey50x40, key.x, key.y, this );
	  }
	  break;
	case LARGE_KEY_SIZE:
	  if( this.imgKey60x40 != null ) {
	    g.drawImage( this.imgKey60x40, key.x, key.y, this );
	  }
	  break;
	case SPACE_KEY_SIZE:
	  if( this.imgKey320x40 != null ) {
	    g.drawImage( this.imgKey320x40, key.x, key.y, this );
	  }
	  break;
      }
      if( key.image != null ) {
	g.drawImage( key.image, key.x, key.y, this );
      } else {
	g.setColor( Color.black );
	if( key.text1 != null ) {
	  if( key.text2 != null ) {
	    g.setFont( this.fontDigit );
	    drawMultiLineString(
			g,
			key.x,
			key.y,
			key.w,
			key.text2,
			8,
			5 + DIGIT_FONT_SIZE,
			DIGIT_FONT_SIZE );
	    drawMultiLineString(
			g,
			key.x,
			key.y,
			key.w,
			key.text1,
			8,
			KEY_SIZE - 9,
			DIGIT_FONT_SIZE );
	  } else {
	    if( key.text1.length() == 1 ) {
	      g.setFont( this.fontLetter );
	      g.drawString(
			key.text1,
			key.x + 8,
			key.y + 6 + LETTER_FONT_SIZE );
	    } else {
	      g.setFont( this.fontText );
	      drawMultiLineString(
			g,
			key.x,
			key.y,
			key.w,
			key.text1,
			8,
			8 + TEXT_FONT_SIZE,
			TEXT_FONT_SIZE );
	    }
	  }
	}
      }
    }

    // linker LED-Block
    g.setColor( Color.gray );
    g.drawLine(
	this.xRow1Left,
	MARGIN + KEY_SIZE,
	this.xRow1Left,
	MARGIN + (3 * KEY_SIZE ) );
    int x = this.xRow1Left + (KEY_HALF_SIZE - LED_SIZE) / 2;
    int y = MARGIN + KEY_SIZE + ((KEY_SIZE - LED_SIZE) / 2);
    g.setColor( this.emuSys.getTapeLED() ?
				this.colorLEDGreenOn
				: this.colorLEDGreenOff );
    g.fillOval( x, y, LED_SIZE, LED_SIZE );
    g.setColor( this.emuSys.getCapsLockLED() ?
				this.colorLEDYellowOn
				: this.colorLEDYellowOff );
    g.fillOval( x, y + KEY_SIZE, LED_SIZE, LED_SIZE );

    // rechter LED-Block
    y = MARGIN + (2 * KEY_SIZE );
    g.setColor( Color.gray );
    g.drawLine( this.xRow1Right, MARGIN + KEY_SIZE, this.xRow1Right, y );
    g.drawLine( this.xRow1Right, y, this.xRow3Right, y );
    g.setColor( Color.red );
    x = this.xRow1Right - KEY_HALF_SIZE + ((KEY_HALF_SIZE - LED_SIZE) / 2);
    y = MARGIN + KEY_SIZE + ((KEY_SIZE - LED_SIZE) / 2);
    g.fillOval( x, y, LED_SIZE, LED_SIZE );
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( emuSys instanceof A5105 ) {
      this.emuSys = (A5105) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != A5105" );
    }
  }


	/* --- private Methoden --- */

  private void addKey(
		Image  image,
		int    col,
		int    value,
		String toolTipText )
  {
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					KEY_SIZE,
					KEY_SIZE,
					null,
					null,
					null,
					null,
					image,
					col,
					value,
					false,
					toolTipText );
    this.curX += KEY_SIZE;
  }


  private void addKey( Image image, int col, int value )
  {
    addKey( image, col, value, null );
  }


  private void addKey(
		String textNormal,
		String textShift,
		int    col,
		int    value,
		String toolTipText )
  {
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					KEY_SIZE,
					KEY_SIZE,
					textNormal,
					textShift,
					null,
					null,
					null,
					col,
					value,
					false,
					toolTipText );
    this.curX += KEY_SIZE;
  }


  private void addKey(
		String textNormal,
		String textShift,
		int    col,
		int    value )
  {
    addKey( textNormal, textShift, col, value, null );
  }


  private void addKey(
		String textNormal,
		int    col,
		int    value )
  {
    addKey( textNormal, null, col, value, null );
  }


  private void addLargeKey(
		String textNormal,
		int    col,
		int    value,
		String toolTipText )
  {
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					LARGE_KEY_SIZE,
					KEY_SIZE,
					textNormal,
					null,
					null,
					null,
					null,
					col,
					value,
					false,
					toolTipText );
    this.curX += LARGE_KEY_SIZE;
  }


  private void drawMultiLineString(
				Graphics g,
				int      xBtn,
				int      yBtn,
				int      wBtn,
				String   text,
				int      xText,
				int      yText,
				int      fontSize )
  {
    if( text != null ) {
      String line1 = null;
      String line2 = null;
      int    eol   = text.indexOf( '\n' );
      if( eol >= 0 ) {
	line1 = text.substring( 0, eol );
	if( (eol + 1) < text.length() ) {
	  line2 = text.substring( eol + 1 );
	}
      } else {
	line1 = text;
      }
      if( line1 != null ) {
	int         w  = -1;
	FontMetrics fm = g.getFontMetrics();
	if( fm != null ) {
	  w = fm.stringWidth( line1 );
	  if( line2 != null ) {
	    w = Math.max( w, fm.stringWidth( line2 ) );
	  }
	}
        if( (wBtn - (2 * xText)) < w ) {
	  xText = (wBtn - w) / 2;
	}
	g.drawString( line1, xBtn + xText, yBtn + yText );
	if( line2 != null ) {
	  yText += (fontSize + 1);
	  g.drawString( line2, xBtn + xText, yBtn + yText );
	}
      }
    }
  }
}
