/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Z9001-Tastatur
 */

package jkcemu.emusys.z9001;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.*;
import java.util.Arrays;
import jkcemu.base.*;
import jkcemu.emusys.Z9001;


public class Z9001KeyboardFld extends AbstractKeyboardFld<Z9001>
{
  private static final int FS_SMALL   = 9;
  private static final int FS_KEY     = 12;
  private static final int FS_SKEY    = 9;
  private static final int MARGIN_WIN = 15;
  private static final int MARGIN_FRM = 8;
  private static final int KEY_W      = 30;
  private static final int KEY_H      = KEY_W / 3;
  private static final int KEY_COL_W  = 45;
  private static final int KEY_ROW_H  = KEY_H * 13 / 3;
  private static final int LKEY_W     = KEY_COL_W + KEY_W;
  private static final int LED_W      = 12;
  private static final int Y_COLOR    = MARGIN_WIN + FS_SMALL;
  private static final int Y_KEY0     = Y_COLOR + 22;
  private static final int X_KEY0     = MARGIN_WIN;
  private static final int X_KEY1     = X_KEY0 + KEY_COL_W;
  private static final int X_KEY2     = X_KEY1 + KEY_COL_W;
  private static final int X_KEY3     = X_KEY2 + KEY_COL_W;
  private static final int X_KEY4     = X_KEY3 + KEY_COL_W;
  private static final int X_KEY5     = X_KEY4 + KEY_COL_W;
  private static final int X_KEY6     = X_KEY5 + KEY_COL_W;
  private static final int X_KEY7     = X_KEY6 + KEY_COL_W;
  private static final int X_KEY8     = X_KEY7 + KEY_COL_W;
  private static final int X_KEY9     = X_KEY8 + KEY_COL_W;
  private static final int X_KEY10    = X_KEY9 + KEY_COL_W;
  private static final int X_KEY11    = X_KEY10 + KEY_COL_W;
  private static final int X_KEY12    = X_KEY11 + (3 * KEY_W);
  private static final int X_KEY13    = X_KEY12 + KEY_COL_W;


  private Image   imgLeft;
  private Image   imgRight;
  private Image   imgUp;
  private Image   imgDown;
  private Image   imgFirst;
  private Image   imgLast;
  private Color   colorKeyLight;
  private Color   colorKeyDark;
  private Color   colorKeySelected;
  private Color   colorLEDGreenOn;
  private Color   colorLEDGreenOff;
  private Font    fontSmall;
  private Font    fontKey;
  private Font    fontSpecialKey;
  private KeyData resetKey;
  private int[]   kbMatrix;
  private int     curIdx;


  public Z9001KeyboardFld( Z9001 z9001 )
  {
    super( z9001, 65, true );
    this.imgLeft  = getImage( "/images/keyboard/z9001/left.png" );
    this.imgRight = getImage( "/images/keyboard/z9001/right.png" );
    this.imgUp    = getImage( "/images/keyboard/z9001/up.png" );
    this.imgDown  = getImage( "/images/keyboard/z9001/down.png" );
    this.imgFirst = getImage( "/images/keyboard/z9001/first.png" );
    this.imgLast  = getImage( "/images/keyboard/z9001/last.png" );

    this.colorKeyLight    = new Color( 180, 180, 180 );
    this.colorKeyDark     = new Color( 20, 20, 20 );
    this.colorKeySelected = new Color( 120, 120, 120 );
    this.colorLEDGreenOn  = Color.green;
    this.colorLEDGreenOff = new Color( 60, 120, 60 );
    this.fontSmall        = new Font( "SansSerif", Font.PLAIN, FS_SMALL );
    this.fontKey          = new Font( "SansSerif", Font.BOLD, FS_KEY );
    this.fontSpecialKey   = new Font( "SansSerif", Font.PLAIN, FS_SKEY );
    this.kbMatrix         = new int[ 8 ];
    this.curIdx           = 0;

    int y = Y_KEY0;
							// CONTR
    addKey( X_KEY0, y, KEY_W, KEY_H, this.colorKeyLight, 2, 0x80, true, null );
    addDarkKey( X_KEY1, y, 1, 0x01 );			// 1
    addDarkKey( X_KEY2, y, 2, 0x01 );			// 2
    addDarkKey( X_KEY3, y, 3, 0x01 );			// 3
    addDarkKey( X_KEY4, y, 4, 0x01 );			// 4
    addDarkKey( X_KEY5, y, 5, 0x01 );			// 5
    addDarkKey( X_KEY6, y, 6, 0x01 );			// 6
    addDarkKey( X_KEY7, y, 7, 0x01 );			// 7
    addDarkKey( X_KEY8, y, 0, 0x02 );			// 8
    addDarkKey( X_KEY9, y, 1, 0x02 );			// 9
    addDarkKey( X_KEY10, y, 0, 0x01 );			// 0
    addDarkKey( X_KEY11, y, 0, 0x04 );			// @
    addLightKey( X_KEY12, y, 4, 0x20, "F4" );		// PAUSE
    this.resetKey = new KeyData(                                              
				X_KEY13,                                        
				y,                                              
				LKEY_W,                                         
				KEY_H,                                          
				null,                                           
				null,                                           
				null,                                           
				new Color( 200, 0, 0 ),                         
				null,                                           
				-1,                                             
				0,                                              
				false,
				null );
    this.keys[ this.curIdx++ ] = this.resetKey;		// RESET

    y += KEY_ROW_H;
    addLightKey( X_KEY0, y, 1, 0x80, "F7" );		// COLOR
    addDarkKey( X_KEY1, y, 1, 0x10 );			// Q
    addDarkKey( X_KEY2, y, 7, 0x10 );			// W
    addDarkKey( X_KEY3, y, 5, 0x04 );			// E
    addDarkKey( X_KEY4, y, 2, 0x10 );			// R
    addDarkKey( X_KEY5, y, 4, 0x10 );			// T
    addDarkKey( X_KEY6, y, 2, 0x20 );			// Z
    addDarkKey( X_KEY7, y, 5, 0x10 );			// U
    addDarkKey( X_KEY8, y, 1, 0x08 );			// I
    addDarkKey( X_KEY9, y, 7, 0x08 );			// O
    addDarkKey( X_KEY10, y, 0, 0x10 );			// P
    addDarkKey( X_KEY11, y, 6, 0x20 );			// ^
    addLightKey( X_KEY12, y, 5, 0x20, "Einfg" );	// INS
    addLightKey( X_KEY13, y, 4, 0x80, "F1" );		// LIST

    y += KEY_ROW_H;
    addLightKey( X_KEY0, y, 3, 0x80, "F8" );		// GRAPHIC
    addDarkKey( X_KEY1, y, 1, 0x04 );			// A
    addDarkKey( X_KEY2, y, 3, 0x10 );			// S
    addDarkKey( X_KEY3, y, 4, 0x04 );			// D
    addDarkKey( X_KEY4, y, 6, 0x04 );			// F
    addDarkKey( X_KEY5, y, 7, 0x04 );			// G
    addDarkKey( X_KEY6, y, 0, 0x08 );			// H
    addDarkKey( X_KEY7, y, 2, 0x08 );			// J
    addDarkKey( X_KEY8, y, 3, 0x08 );			// K
    addDarkKey( X_KEY9, y, 4, 0x08 );			// L
    addDarkKey( X_KEY10, y, 2, 0x02 );			// :
    addDarkKey( X_KEY11, y, 7, 0x02 );			// ?
    addLightKey( X_KEY12, y, 4, 0x40, "Esc" );		// ESC
    addLongLightKey( X_KEY13, y, 5, 0x80, "F2" );	// RUN

    y += KEY_ROW_H;
    addDarkKey( X_KEY1, y, 1, 0x20 );			// Y
    addDarkKey( X_KEY2, y, 0, 0x20 );			// X
    addDarkKey( X_KEY3, y, 3, 0x04 );			// C
    addDarkKey( X_KEY4, y, 6, 0x10 );			// V
    addDarkKey( X_KEY5, y, 2, 0x04 );			// B
    addDarkKey( X_KEY6, y, 6, 0x08 );			// N
    addDarkKey( X_KEY7, y, 5, 0x08 );			// M
    addDarkKey( X_KEY8, y, 4, 0x02 );			// ,
    addDarkKey( X_KEY9, y, 6, 0x02 );			// .
    addDarkKey( X_KEY10, y, 3, 0x02 );			// ;
    addDarkKey( X_KEY11, y, 5, 0x02 );			// =
    addLightKey( X_KEY12, y, 3, 0x20, "Strg + \u2190" ); // Zeilenanfang
    addLongLightKey( X_KEY13, y, 6, 0x40, "F3" );	// STOP

    y += KEY_ROW_H;
    KeyData shiftKey1 = addKey(
				X_KEY0,
				y,
				LKEY_W,
				KEY_H,
				this.colorKeyLight,
				0,
				0x80,
				true,
				null );				// SHIFT 1
    addLightKey( X_KEY2, y, 0, 0x40 );				// LEFT
    addLightKey( X_KEY3, y, 1, 0x40 );				// RIGHT
    addLongLightKey( X_KEY4, y, 7, 0x40, "Leerzeichen" );	// SPACE 1
    addLongLightKey( X_KEY6, y, 7, 0x40, "Leerzeichen" );	// SPACE 2
    addLightKey( X_KEY8, y, 3, 0x40 );				// UP
    addLightKey( X_KEY9, y, 2, 0x40 );				// DOWN

    KeyData shiftKey2 = addKey(
				X_KEY10,
				y,
				LKEY_W,
				KEY_H,
				this.colorKeyLight,
				0,
				0x80,
				true,
				null );			// SHIFT 2
    addLightKey( X_KEY12, y, 6, 0x80 );			// SHIFT LOCK
    addLongLightKey( X_KEY13, y, 5, 0x40, "Enter" );	// ENTER

    int h = Y_KEY0 + (4 * KEY_ROW_H) + KEY_H + 2 + MARGIN_WIN;
    if( this.imgDown != null ) {
      h += Math.max( FS_KEY, this.imgDown.getHeight( this ) );
    } else {
      h += FS_KEY;
    }
    setPreferredSize( new Dimension( X_KEY13 + LKEY_W + MARGIN_WIN, h ) );
    setShiftKeys( shiftKey1, shiftKey2 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof Z9001;
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
  public void mousePressed( MouseEvent e )
  {
    if( e.getComponent() == this ) {
      if( hits( this.resetKey, e ) ) {
	fireWarmResetAfterDelay();
      }
      super.mousePressed( e );
    }
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    int y = Y_COLOR;
    g.setFont( this.fontSmall );
    drawCenter( g, X_KEY1, y, KEY_W, "BLACK" );
    drawCenter( g, X_KEY2, y, KEY_W, "RED" );
    drawCenter( g, X_KEY3, y, KEY_W, "GREEN" );
    drawCenter( g, X_KEY4, y, KEY_W, "YELLOW" );
    drawCenter( g, X_KEY5, y, KEY_W, "BLUE" );
    drawCenter( g, X_KEY6, y, KEY_W, "MAGENTA" );
    drawCenter( g, X_KEY7, y, KEY_W, "CYAN" );
    drawCenter( g, X_KEY8, y, KEY_W, "WHITE" );

    g.setFont( this.fontKey );
    y = Y_KEY0 - 5;
    drawCenter( g, X_KEY1, y, KEY_W, "!" );
    drawCenter( g, X_KEY2, y, KEY_W, "\"" );
    drawCenter( g, X_KEY3, y, KEY_W, "#" );
    drawCenter( g, X_KEY4, y, KEY_W, "$" );
    drawCenter( g, X_KEY5, y, KEY_W, "%" );
    drawCenter( g, X_KEY6, y, KEY_W, "&" );
    drawCenter( g, X_KEY7, y, KEY_W, "\'" );
    drawCenter( g, X_KEY8, y, KEY_W, "(" );
    drawCenter( g, X_KEY9, y, KEY_W, ")" );
    drawCenter( g, X_KEY10, y, KEY_W, "_" );

    y += (2 * KEY_ROW_H);
    drawCenter( g, X_KEY10, y, KEY_W, "*" );
    drawCenter( g, X_KEY11, y, KEY_W, "/" );

    y += KEY_ROW_H;
    drawCenter( g, X_KEY8, y, KEY_W, "<" );
    drawCenter( g, X_KEY9, y, KEY_W, ">" );
    drawCenter( g, X_KEY10, y, KEY_W, "+" );
    drawCenter( g, X_KEY11, y, KEY_W, "-" );

    y = Y_KEY0 + KEY_H + FS_KEY + 1;
    drawCenter( g, X_KEY1, y, KEY_W, "1" );
    drawCenter( g, X_KEY2, y, KEY_W, "2" );
    drawCenter( g, X_KEY3, y, KEY_W, "3" );
    drawCenter( g, X_KEY4, y, KEY_W, "4" );
    drawCenter( g, X_KEY5, y, KEY_W, "5" );
    drawCenter( g, X_KEY6, y, KEY_W, "6" );
    drawCenter( g, X_KEY7, y, KEY_W, "7" );
    drawCenter( g, X_KEY8, y, KEY_W, "8" );
    drawCenter( g, X_KEY9, y, KEY_W, "9" );
    drawCenter( g, X_KEY10, y, KEY_W, "0" );
    drawCenter( g, X_KEY11, y, KEY_W, "@" );

    y += KEY_ROW_H;
    drawCenter( g, X_KEY1, y, KEY_W, "Q" );
    drawCenter( g, X_KEY2, y, KEY_W, "W" );
    drawCenter( g, X_KEY3, y, KEY_W, "E" );
    drawCenter( g, X_KEY4, y, KEY_W, "R" );
    drawCenter( g, X_KEY5, y, KEY_W, "T" );
    drawCenter( g, X_KEY6, y, KEY_W, "Z" );
    drawCenter( g, X_KEY7, y, KEY_W, "U" );
    drawCenter( g, X_KEY8, y, KEY_W, "I" );
    drawCenter( g, X_KEY9, y, KEY_W, "O" );
    drawCenter( g, X_KEY10, y, KEY_W, "P" );
    drawCenter( g, X_KEY11, y, KEY_W, "^" );
  
    y += KEY_ROW_H;
    drawCenter( g, X_KEY1, y, KEY_W, "A" );
    drawCenter( g, X_KEY2, y, KEY_W, "S" );
    drawCenter( g, X_KEY3, y, KEY_W, "D" );
    drawCenter( g, X_KEY4, y, KEY_W, "F" );
    drawCenter( g, X_KEY5, y, KEY_W, "G" );
    drawCenter( g, X_KEY6, y, KEY_W, "H" );
    drawCenter( g, X_KEY7, y, KEY_W, "J" );
    drawCenter( g, X_KEY8, y, KEY_W, "K" );
    drawCenter( g, X_KEY9, y, KEY_W, "L" );
    drawCenter( g, X_KEY10, y, KEY_W, ":" );
    drawCenter( g, X_KEY11, y, KEY_W, "?" );

    y += KEY_ROW_H;
    drawCenter( g, X_KEY1, y, KEY_W, "Y" );
    drawCenter( g, X_KEY2, y, KEY_W, "X" );
    drawCenter( g, X_KEY3, y, KEY_W, "C" );
    drawCenter( g, X_KEY4, y, KEY_W, "V" );
    drawCenter( g, X_KEY5, y, KEY_W, "B" );
    drawCenter( g, X_KEY6, y, KEY_W, "N" );
    drawCenter( g, X_KEY7, y, KEY_W, "M" );
    drawCenter( g, X_KEY8, y, KEY_W, "," );
    drawCenter( g, X_KEY9, y, KEY_W, "." );
    drawCenter( g, X_KEY10, y, KEY_W, ";" );
    drawCenter( g, X_KEY11, y, KEY_W, "=" );

    if( this.imgLast != null ) {
      int hImg = this.imgLast.getHeight( this );
      if( hImg > 0 ) {
	drawCenter(
		g,
		X_KEY12,
		y - FS_KEY - KEY_H - hImg - 4,
		KEY_W,
		this.imgLast );
      }
    }
    drawCenter( g, X_KEY12, y - FS_KEY + 3, KEY_W, this.imgFirst );

    g.setFont( this.fontSpecialKey );
    y = Y_KEY0 - 5;
    drawCenter( g, X_KEY12, y, KEY_W, "CONT" );

    y += KEY_ROW_H;
    drawCenter( g, X_KEY12, y, KEY_W, "DEL" );

    y += KEY_ROW_H;
    drawCenter( g, X_KEY12, y, KEY_W, "CL LN" );

    y = Y_KEY0 + KEY_H + FS_SKEY + 1;
    drawCenter( g, X_KEY0, y, KEY_W, "CONTR" );
    drawCenter( g, X_KEY12, y, KEY_W, "PAUSE" );
    drawRight( g, X_KEY13, y, LKEY_W, "RESET" );

    y += KEY_ROW_H;
    drawCenter( g, X_KEY0, y, KEY_W, "COLOR" );
    drawCenter( g, X_KEY12, y, KEY_W, "INS" );
    drawRight( g, X_KEY13, y, KEY_W, "LIST" );

    y += KEY_ROW_H;
    drawCenter( g, X_KEY12, y, KEY_W, "ESC" );
    drawRight( g, X_KEY13, y, LKEY_W, "RUN" );

    y += KEY_ROW_H;
    drawRight( g, X_KEY13, y, LKEY_W, "STOP" );

    y += KEY_ROW_H;
    drawRight( g, X_KEY13, y, LKEY_W, "ENTER" );

    y = Y_KEY0 + (4 * KEY_ROW_H) + KEY_H + 3;
    drawCenter( g, X_KEY2, y, KEY_W, this.imgLeft );
    drawCenter( g, X_KEY3, y, KEY_W, this.imgRight );
    drawCenter( g, X_KEY8, y, KEY_W, this.imgUp );
    drawCenter( g, X_KEY9, y, KEY_W, this.imgDown );

    y += (FS_SKEY - 2);
    g.drawString( "SHIFT", X_KEY0, y );
    drawRight( g, X_KEY10, y, LKEY_W, "SHIFT" );
    drawRight( g, X_KEY12, y, KEY_W, "SHIFT LOCK" );

    // Power-LED
    int x = X_KEY13 + LKEY_W - KEY_W;
    y = Y_KEY0 + KEY_ROW_H - ((KEY_W - KEY_H) / 2);
    g.setColor( Color.black );
    g.drawRect( x, y, KEY_W, KEY_W );
    g.setColor( Color.red );
    x += ((KEY_W - LED_W) / 2);
    y += ((KEY_W - LED_W) / 2);
    g.fillOval( x, y, LED_W, LED_W );

    // GRAPHIC-LED
    x = X_KEY0 - MARGIN_FRM;
    y = Y_KEY0 + (2 * KEY_ROW_H) - MARGIN_FRM;
    g.setColor( Color.black );
    g.drawRect(
	x,
	y,
	KEY_W + (2 * MARGIN_FRM) - 1,
	KEY_ROW_H + KEY_H + (2 * MARGIN_FRM) - 1 );

    y = Y_KEY0 + (2 * KEY_ROW_H)
			+ KEY_H
			+ ((KEY_ROW_H - KEY_H - LED_W) / 2)
			+ FS_SKEY;
    drawCenter( g, X_KEY0, y, KEY_W, "GRAPHIC" );

    g.setColor( this.emuSys.getGraphicLED() ?
			this.colorLEDGreenOn : this.colorLEDGreenOff );
    x = X_KEY0 + ((KEY_W - LED_W) / 2);
    y = Y_KEY0 + (3 * KEY_ROW_H) - ((LED_W - KEY_H) / 2);
    g.fillOval( x, y, LED_W, LED_W );



    for( KeyData key : this.keys ) {
      g.setColor( isKeySelected( key ) ? this.colorKeySelected : key.color );
      g.fillRect( key.x, key.y, key.w, key.h );
      g.setColor( Color.black );
      g.drawRect( key.x, key.y, key.w, key.h );
    }
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( emuSys instanceof Z9001 ) {
      this.emuSys = (Z9001) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != Z9001" );
    }
  }


	/* --- private Methoden --- */

  private KeyData addKey(
			int     x,
			int     y,
			int     w,
			int     h,
			Color   color,
			int     col,
			int     value,
			boolean shift,
			String  toolTipText )
  {
    KeyData keyData = new KeyData(
				x,
				y,
				w,
				h,
				null,
				null,
				null,
				color,
				null,
				col,
				value,
				shift,
				toolTipText );
    this.keys[ this.curIdx++ ] = keyData;
    return keyData;
  }


  private void addDarkKey( int x, int y, int col, int value )
  {
    addKey( x, y, KEY_W, KEY_H, this.colorKeyDark, col, value, false, null );
  }


  private void addLightKey( int x, int y, int col, int value )
  {
    addKey( x, y, KEY_W, KEY_H, this.colorKeyLight, col, value, false, null );
  }


  private void addLightKey(
		int    x,
		int    y,
		int    col,
		int    value,
		String toolTipText )
  {
    addKey(
	x,
	y,
	KEY_W,
	KEY_H,
	this.colorKeyLight,
	col,
	value,
	false,
	toolTipText );
  }


  private void addLongLightKey(
			int    x,
			int    y,
			int    col,
			int    value,
			String toolTipText )
  {
    addKey(
	x,
	y,
	LKEY_W,
	KEY_H,
	this.colorKeyLight,
	col,
	value,
	false,
	toolTipText );
  }


  private void drawCenter( Graphics g, int x, int y, int w, String text )
  {
    FontMetrics fm = g.getFontMetrics();
    if( fm != null ) {
      g.drawString( text, x + ((w - fm.stringWidth( text )) / 2), y );
    } else {
      g.drawString( text, x, y );
    }
  }


  private void drawCenter( Graphics g, int x, int y, int w, Image img )
  {
    if( img != null ) {
      int wImg = img.getWidth( this );
      if( wImg > 0 ) {
	g.drawImage( img, x + ((w - wImg) / 2), y, this );
      }
    }
  }


  private void drawRight( Graphics g, int x, int y, int w, String text )
  {
    FontMetrics fm = g.getFontMetrics();
    if( fm != null ) {
      g.drawString( text, x + (w - fm.stringWidth( text )), y );
    } else {
      g.drawString( text, x, y );
    }
  }
}

