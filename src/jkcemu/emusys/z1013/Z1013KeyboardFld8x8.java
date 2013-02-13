/*
 * (c) 2011-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Z1013-Alphatastatur
 */

package jkcemu.emusys.z1013;

import java.awt.*;
import java.lang.*;
import java.util.Arrays;
import jkcemu.base.*;
import jkcemu.emusys.Z1013;


public class Z1013KeyboardFld8x8 extends AbstractKeyboardFld
{
  private static final int MARGIN            = 20;
  private static final int TEXT_FONT_SIZE    = 11;
  private static final int LETTER_FONT_SIZE  = 14;
  private static final int DIGIT_FONT_SIZE   = 12;
  private static final int KEY_SIZE          = 40;
  private static final int MEDIUM_KEY_SIZE   = 50;
  private static final int LARGE_KEY_SIZE    = 60;
  private static final int SPACE_KEY_SIZE    = 320;
  private static final int DOUBLE_KEY_HEIGHT = 80;

  private Z1013 z1013;
  private Image imgLeft;
  private Image imgRight;
  private Image imgUp;
  private Image imgDown;
  private Font  fontText;
  private Font  fontLetter;
  private Font  fontDigit;
  private int[] kbMatrix;
  private int   curIdx;
  private int   curX;
  private int   curY;


  public Z1013KeyboardFld8x8( Z1013 z1013 )
  {
    super( 59 );
    this.z1013      = z1013;
    this.imgLeft    = getImage( "/images/keyboard/left.png" );
    this.imgRight   = getImage( "/images/keyboard/right.png" );
    this.imgUp      = getImage( "/images/keyboard/up.png" );
    this.imgDown    = getImage( "/images/keyboard/down.png" );
    this.fontText   = new Font( "SansSerif", Font.PLAIN, TEXT_FONT_SIZE );
    this.fontLetter = new Font( "SansSerif", Font.PLAIN, LETTER_FONT_SIZE );
    this.fontDigit  = new Font( "SansSerif", Font.PLAIN, DIGIT_FONT_SIZE );
    this.kbMatrix   = new int[ 8 ];
    this.curIdx     = 0;
    this.curX       = MARGIN;
    this.curY       = MARGIN;
    addKey( "\\", "|", 5, 0x40 );
    addKey( "1", "!", 0, 0x01 );
    addKey( "2", "\"", 0, 0x10 );
    addKey( "3", "#", 1, 0x01 );
    addKey( "4", "$", 1, 0x10 );
    addKey( "5", "%", 2, 0x01 );
    addKey( "6", "&", 2, 0x10 );
    addKey( "7", "\'", 3, 0x01 );
    addKey( "8", "(", 3, 0x10 );
    addKey( "9", ")", 4, 0x01 );
    addKey( "0", "", 4, 0x10 );
    addKey( "-", "=", 5, 0x01 );
    addKey( "^", "~", 5, 0x08 );
    addKey( "Graph\nE/A", 6, 0x01 );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;

    KeyData controlKey = new KeyData(
				this.curX,
				this.curY,
				LARGE_KEY_SIZE,
				KEY_SIZE,
				"CTRL",
				null,
				null,
				null,
				null,
				6,
				0x20,
				true );
    this.keys[ this.curIdx++ ] = controlKey;
    this.curX += LARGE_KEY_SIZE;
    addKey( "Q", 0, 0x02 );
    addKey( "W", 0, 0x20 );
    addKey( "E", 1, 0x02 );
    addKey( "R", 1, 0x20 );
    addKey( "T", 2, 0x02 );
    addKey( "Z", 2, 0x20 );
    addKey( "U", 3, 0x02 );
    addKey( "I", 3, 0x20 );
    addKey( "O", 4, 0x02 );
    addKey( "P", 4, 0x20 );
    addKey( "@", "\u0060", 5, 0x02 );
    addKey( "[", "{", 5, 0x10 );

    this.curX = MARGIN + (KEY_SIZE / 4);
    this.curY += KEY_SIZE;
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					LARGE_KEY_SIZE,
					KEY_SIZE,
					"Shift\nLock",
					null,
					null,
					null,
					null,
					7,
					0x80,
					false );
    this.curX += LARGE_KEY_SIZE;
    addKey( "A", 0, 0x04 );
    addKey( "S", 0, 0x40 );
    addKey( "D", 1, 0x04 );
    addKey( "F", 1, 0x40 );
    addKey( "G", 2, 0x04 );
    addKey( "H", 2, 0x40 );
    addKey( "J", 3, 0x04 );
    addKey( "K", 3, 0x40 );
    addKey( "L", 4, 0x04 );
    addKey( "+", ";", 4, 0x40 );
    addKey( "*", ":", 5, 0x04 );
    addKey( "]", "}", 5, 0x20 );
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					KEY_SIZE,
					DOUBLE_KEY_HEIGHT,
					"ENT",
					null,
					null,
					null,
					null,
					6,
					0x02,
					false );
    int w = this.curX + KEY_SIZE + MARGIN;

    this.curX = MARGIN;
    this.curY += KEY_SIZE;

    KeyData shiftKey1 = new KeyData(
				this.curX,
				this.curY,
				MEDIUM_KEY_SIZE,
				KEY_SIZE,
				"Shift",
				null,
				null,
				null,
				null,
				7,
				0x40,
				true );
    this.keys[ this.curIdx++ ] = shiftKey1;
    this.curX += MEDIUM_KEY_SIZE;
    addKey( "_", 5, 0x80 );
    addKey( "Y", 0, 0x08 );
    addKey( "X", 0, 0x80 );
    addKey( "C", 1, 0x08 );
    addKey( "V", 1, 0x80 );
    addKey( "B", 2, 0x08 );
    addKey( "N", 2, 0x80 );
    addKey( "M", 3, 0x08 );
    addKey( ",", "<", 3, 0x80 );
    addKey( ".", ">", 4, 0x08 );
    addKey( "/", "?", 4, 0x80 );

    KeyData shiftKey2 = new KeyData(
				this.curX,
				this.curY,
				LARGE_KEY_SIZE,
				KEY_SIZE,
				"Shift",
				null,
				null,
				null,
				null,
				7,
				0x40,
				true );
    this.keys[ this.curIdx++ ] = shiftKey2;

    this.curX = MARGIN + KEY_SIZE + (KEY_SIZE / 2);
    this.curY += KEY_SIZE;
    addKey( this.imgUp, 6, 0x40 );
    addKey( this.imgLeft, 6, 0x04 );
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
				6,
				0x10,
				false );
    this.curX += (8 * KEY_SIZE);
    addKey( this.imgRight, 6, 0x08 );
    addKey( this.imgDown, 6, 0x80 );

    int h = this.curY + KEY_SIZE + MARGIN;
    setPreferredSize( new Dimension( w, h ) );
    setShiftKeys( shiftKey1, shiftKey2 );
  }


  public void updKeySelection( int[] kbMatrix )
  {
    boolean dirty = false;
    synchronized( this.selectedKeys ) {
      dirty = !this.selectedKeys.isEmpty();
      this.selectedKeys.clear();
      if( kbMatrix != null ) {
	for( int col = 0; col < kbMatrix.length; col++ ) {
	  if( kbMatrix[ col ] != 0 ) {
	    for( KeyData key : this.keys ) {
	      if( (key.col == col)
		  && ((key.value & kbMatrix[ col ]) != 0) )
	      {
		dirty      = true;
		key.locked = false;
		this.selectedKeys.add( key );
	      }
	    }
	  }
	}
      }
    }
    if( dirty ) {
      repaint();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    boolean rv = false;
    if( emuSys instanceof Z1013 ) {
      Z1013Keyboard keyboard = ((Z1013) emuSys).getZ1013Keyboard();
      if( keyboard != null ) {
	KeyboardMatrix kbMatrix = keyboard.getKeyboardMatrix();
	if( kbMatrix != null ) {
	  if( kbMatrix instanceof KeyboardMatrix8x8 ) {
	    rv = true;
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public String getKeyboardName()
  {
    return "Z1013 Alphatastatur";
  }


  @Override
  protected void keySelectionChanged()
  {
    Z1013Keyboard  kb = this.z1013.getZ1013Keyboard();
    KeyboardMatrix km = kb.getKeyboardMatrix();
    if( km instanceof KeyboardMatrix8x8 ) {
      Arrays.fill( this.kbMatrix, 0 );
      synchronized( this.selectedKeys ) {
	for( KeyData key : this.selectedKeys ) {
	  if( (key.col >= 0) && (key.col < this.kbMatrix.length) ) {
	    this.kbMatrix[ key.col ] |= key.value;
	  }
	}
      }
      ((KeyboardMatrix8x8) km).updKeyboardMatrix( this.kbMatrix );
    }
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    g.setPaintMode();
    g.setColor( Color.lightGray );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    for( KeyData key : this.keys ) {
      boolean selected = isKeySelected( key );
      if( selected ) {
	g.setColor( Color.gray );
	g.fillRect( key.x + 1, key.y + 1, key.w - 1, key.h - 1 );
      }
      g.setColor( Color.lightGray );
      g.draw3DRect( key.x + 1, key.y + 1, key.w - 1, key.h - 1, !selected );
      if( key.image != null ) {
	g.drawImage(
		key.image,
		key.x + ((key.w - key.image.getWidth( this )) / 2) + 1,
		key.y + ((key.h - key.image.getHeight( this )) / 2) + 1,
		this );
      } else {
	g.setColor( Color.black );
	if( key.text1 != null ) {
	  if( key.text2 != null ) {
	    g.setFont( this.fontDigit );
	    g.drawString(
			key.text1,
			key.x + 8,
			key.y + KEY_SIZE - 8 );
	    g.drawString(
			key.text2,
			key.x + 20,
			key.y + 5 + DIGIT_FONT_SIZE );
	  } else {
	    if( key.text1.length() == 1 ) {
	      g.setFont( this.fontLetter );
	      drawMultiLineString(
				g,
				key.x,
				key.y,
				key.w,
				key.h,
				key.text1,
				LETTER_FONT_SIZE );
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
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( accepts( emuSys ) ) {
      this.z1013 = (Z1013) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != Z1013/8x8" );
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
		String textNormal,
		String textShift,
		int    col,
		int    value )
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
					false );
    this.curX += KEY_SIZE;
  }


  private void addKey(
		String textNormal,
		int    col,
		int    value )
  {
    addKey( textNormal, null, col, value );
  }
}

