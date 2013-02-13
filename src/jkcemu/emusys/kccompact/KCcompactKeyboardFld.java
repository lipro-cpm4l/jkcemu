/*
 * (c) 2011-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die KCcompact-Tastatur
 */

package jkcemu.emusys.kccompact;

import java.awt.*;
import java.lang.*;
import java.util.Arrays;
import jkcemu.base.*;
import jkcemu.emusys.KCcompact;


public class KCcompactKeyboardFld extends AbstractKeyboardFld
{
  private static final int TEXT_FONT_SIZE   = 10;
  private static final int LETTER_FONT_SIZE = 14;
  private static final int DIGIT_FONT_SIZE  = 12;
  private static final int LED_SIZE         = 8;
  private static final int KEY_SIZE         = 40;
  private static final int KEY_HALF_SIZE    = KEY_SIZE / 2;
  private static final int MEDIUM_KEY_SIZE  = KEY_SIZE / 4 * 5;
  private static final int LARGE_KEY_SIZE   = KEY_SIZE / 2 * 3;
  private static final int SPACE_KEY_SIZE   = KEY_SIZE * 8;
  private static final int MARGIN           = 20;

  private KCcompact kccompact;
  private Image     imgKey40x40;
  private Image     imgKey50x40;
  private Image     imgKey60x40;
  private Image     imgKey320x40;
  private Image     imgLeft;
  private Image     imgRight;
  private Image     imgUp;
  private Image     imgDown;
  private Image     imgPoint;
  private Font      fontText;
  private Font      fontLetter;
  private Font      fontDigit;
  private int[]     kbMatrix;
  private int       curIdx;
  private int       curX;
  private int       curY;
  private int       xRow1Left;
  private int       xRow1Right;
  private int       xRow3Right;


  public KCcompactKeyboardFld( KCcompact kccompact )
  {
    super( 69 );
    this.kccompact    = kccompact;
    this.imgKey40x40  = getImage( "/images/keyboard/key40x40.png" );
    this.imgKey50x40  = getImage( "/images/keyboard/key50x40.png" );
    this.imgKey60x40  = getImage( "/images/keyboard/key60x40.png" );
    this.imgKey320x40 = getImage( "/images/keyboard/key320x40.png" );
    this.imgLeft      = getImage( "/images/keyboard/left.png" );
    this.imgRight     = getImage( "/images/keyboard/right.png" );
    this.imgUp        = getImage( "/images/keyboard/up.png" );
    this.imgDown      = getImage( "/images/keyboard/down.png" );
    this.imgPoint     = getImage( "/images/keyboard/point.png" );
    this.fontText     = new Font( "SansSerif", Font.PLAIN, TEXT_FONT_SIZE );
    this.fontLetter   = new Font( "SansSerif", Font.PLAIN, LETTER_FONT_SIZE );
    this.fontDigit    = new Font( "SansSerif", Font.PLAIN, DIGIT_FONT_SIZE );
    this.kbMatrix     = new int[ 10 ];
    this.curIdx       = 0;
    this.curX         = MARGIN;
    this.curY         = MARGIN;
    addKey( "F0", 1, 0x80 );
    this.curX += (KEY_SIZE / 2);
    this.xRow1Left = this.curX;
    addKey( "ESC", 8, 0x04 );
    addKey( "1", "!", 8, 0x01 );
    addKey( "2", "\"", 8, 0x02 );
    addKey( "3", "#", 7, 0x02 );
    addKey( "4", "$", 7, 0x01 );
    addKey( "5", "%", 6, 0x02 );
    addKey( "6", "&", 6, 0x01 );
    addKey( "7", "\'", 5, 0x02 );
    addKey( "8", "(", 5, 0x01 );
    addKey( "9", ")", 4, 0x02 );
    addKey( "0", "_", 4, 0x01 );
    addKey( "-", "=", 3, 0x02 );
    addKey( "^", "\u00A3", 9, 0x01 );		// Pfund
    addKey( "CLR", 2, 0x01 );
    this.xRow1Right = this.curX - 1;
    this.curX += (KEY_SIZE * 3 / 4);
    int xCrsMiddle = this.curX + (LARGE_KEY_SIZE / 2);
    addLargeKey( "DEL", 9, 0x80 );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "F1", 1, 0x20 );
    this.curX += KEY_SIZE;
    addKey( "TAB", 8, 0x10 );
    addKey( "Q", 8, 0x08 );
    addKey( "W", 7, 0x08 );
    addKey( "E", 7, 0x04 );
    addKey( "R", 6, 0x04 );
    addKey( "T", 6, 0x08 );
    addKey( "Y", 5, 0x08 );
    addKey( "U", 5, 0x04 );
    addKey( "I", 4, 0x08 );
    addKey( "O", 4, 0x04 );
    addKey( "P", 3, 0x08 );
    addKey( "@", "|", 3, 0x04 );
    addKey( "[", "{", 2, 0x02 );
    this.curX = xCrsMiddle - (KEY_SIZE / 2);
    addKey( this.imgUp, 0, 0x01 );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "F2", 1, 0x40 );
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
					8,
					0x40,
					false );
    this.curX += MEDIUM_KEY_SIZE;
    addKey( "A", 8, 0x20 );
    addKey( "S", 7, 0x10 );
    addKey( "D", 7, 0x20 );
    addKey( "F", 6, 0x20 );
    addKey( "G", 6, 0x10 );
    addKey( "H", 5, 0x10 );
    addKey( "J", 5, 0x20 );
    addKey( "K", 4, 0x20 );
    addKey( "L", 4, 0x10 );
    addKey( ":", "*", 3, 0x20 );
    addKey( ";", "+", 3, 0x10 );
    addKey( "]", "}", 2, 0x08 );
    this.xRow3Right = this.curX - 1;
    this.curX = xCrsMiddle - KEY_SIZE;
    addKey( this.imgLeft, 1, 0x01 );
    addKey( this.imgRight, 0, 0x02 );

    int w = this.curX + MARGIN;

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "F3", 0, 0x20 );
    this.curX += (KEY_SIZE / 2);
    addKey( this.imgPoint, 0, 0x08 );

    KeyData shiftKey1 = new KeyData(
				this.curX,
				this.curY,
				MEDIUM_KEY_SIZE,
				KEY_SIZE,
				"SHIFT",
				null,
				null,
				null,
				null,
				2,
				0x20,
				true );
    this.keys[ this.curIdx++ ] = shiftKey1;
    this.curX += MEDIUM_KEY_SIZE;
    addKey( "Z", 8, 0x80 );
    addKey( "X", 7, 0x80 );
    addKey( "C", 7, 0x40 );
    addKey( "V", 6, 0x80 );
    addKey( "B", 6, 0x40 );
    addKey( "N", 5, 0x40 );
    addKey( "M", 4, 0x40 );
    addKey( ",", "<", 4, 0x80 );
    addKey( ".", ">", 3, 0x80 );
    addKey( "/", "?", 3, 0x40 );

    KeyData shiftKey2 = new KeyData(
				this.curX,
				this.curY,
				LARGE_KEY_SIZE,
				KEY_SIZE,
				"SHIFT",
				null,
				null,
				null,
				null,
				2,
				0x20,
				true );
    this.keys[ this.curIdx++ ] = shiftKey2;
    this.curX = xCrsMiddle - (KEY_SIZE / 2);
    addKey( this.imgDown, 0, 0x04 );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "F4", 2, 0x10 );
    this.curX += ((KEY_SIZE / 2) + (2 * KEY_SIZE) - MEDIUM_KEY_SIZE);
    addKey( "ENTER", 0, 0x40 );
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
					2,
					0x80,
					true );
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
				5,
				0x80,
				false );
    this.curX += (8 * KEY_SIZE);
    addKey( "COPY", 1, 0x02 );
    addKey( "\\", "\u0060", 2, 0x40 );
    this.curX = xCrsMiddle - (LARGE_KEY_SIZE / 2);
    addLargeKey( "RETURN", 2, 0x04 );

    int h = this.curY + KEY_SIZE + MARGIN;
    setPreferredSize( new Dimension( w, h ) );
    setShiftKeys( shiftKey1, shiftKey2 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof KCcompact;
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
    this.kccompact.updKeyboardMatrix( this.kbMatrix );
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
	g.drawImage(
		key.image,
		key.x + (key.w - key.image.getWidth( this )) / 2,
		key.y + (key.h - key.image.getHeight( this )) / 2,
		this );
      } else {
	g.setColor( Color.black );
	if( key.text1 != null ) {
	  if( key.text2 != null ) {
	    g.setFont( this.fontDigit );
	    if( key.text2 != null ) {
	      g.drawString(
			key.text2,
			key.x + 8,
			key.y + 5 + DIGIT_FONT_SIZE );
	    }
	    g.drawString(
			key.text1,
			key.x + 22,
			key.y + KEY_SIZE - 10 );
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
			key.h,
			key.text1,
			TEXT_FONT_SIZE );
	    }
	  }
	}
      }
    }

    // linker LED-Block (nur Attrappe)
    g.setColor( Color.gray );
    g.drawLine(
        this.xRow1Left,
        MARGIN + KEY_SIZE,
        this.xRow1Left,
        MARGIN + (3 * KEY_SIZE ) );
    int x = this.xRow1Left + (KEY_HALF_SIZE - LED_SIZE) / 2;
    int y = MARGIN + KEY_SIZE + ((KEY_SIZE - LED_SIZE) / 2);
    g.setColor( Color.gray );
    g.fillOval( x, y, LED_SIZE, LED_SIZE );
    g.fillOval( x, y + KEY_SIZE, LED_SIZE, LED_SIZE );

    // rechter LED-Block (nur Attrappe)
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
    if( emuSys instanceof KCcompact ) {
      this.kccompact = (KCcompact) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != KCcompact" );
    }
  }


	/* --- private Methoden --- */

  private void addKey(
		Image image,
		int   col,
		int   value )
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
					false );
    this.curX += KEY_SIZE;
  }


  private void addKey(
		String text1,
		String text2,
		int    col,
		int    value )
  {
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					KEY_SIZE,
					KEY_SIZE,
					text1,
					text2,
					null,
					null,
					null,
					col,
					value,
					false );
    this.curX += KEY_SIZE;
  }


  private void addKey(
		String text1,
		int    col,
		int    value )
  {
    addKey( text1, null, col, value );
  }


  private void addLargeKey(
		String text1,
		int    col,
		int    value )
  {
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					LARGE_KEY_SIZE,
					KEY_SIZE,
					text1,
					null,
					null,
					null,
					null,
					col,
					value,
					false );
    this.curX += LARGE_KEY_SIZE;
  }
}
