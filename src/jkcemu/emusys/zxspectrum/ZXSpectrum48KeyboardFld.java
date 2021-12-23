/*
 * (c) 2013-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Tastatur des ZX Spectrum 48K
 */

package jkcemu.emusys.zxspectrum;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.emusys.ZXSpectrum;


public class ZXSpectrum48KeyboardFld extends AbstractKeyboardFld<ZXSpectrum>
{
  private static final int FONT_LARGE_H  = 12;
  private static final int FONT_LETTER_H = 18;
  private static final int FONT_H        = 9;
  private static final int KEY_W         = 45;
  private static final int KEY_H         = 30;
  private static final int COL_W         = KEY_W + 20;
  private static final int COL1_X1       = 15;
  private static final int COL2_X1       = COL1_X1 + 30;
  private static final int COL3_X1       = COL2_X1 + 20;
  private static final int COL4_X1       = COL1_X1;
  private static final int COL4_X2       = COL3_X1 + 35;
  private static final int ROW_H         = 60;
  private static final int ROW1_Y        = 10;
  private static final int ROW2_Y        = ROW1_Y + ROW_H + FONT_H + 1;
  private static final int ROW3_Y        = ROW2_Y + ROW_H;
  private static final int ROW4_Y        = ROW3_Y + ROW_H;

  private KeyData keyCapsShift;
  private KeyData keySymbolShift;
  private KeyData keyEnter;
  private KeyData keySpace;
  private Image   imgBG;
  private Image   imgG1;
  private Image   imgG2;
  private Image   imgG3;
  private Image   imgG4;
  private Image   imgG5;
  private Image   imgG6;
  private Image   imgG7;
  private Image   imgG8;
  private Image   imgLeft;
  private Image   imgRight;
  private Image   imgUp;
  private Image   imgDown;
  private Image   imgKey;
  private Image   imgKeyPressed;
  private Image   imgKeyCs;
  private Image   imgKeyCsPressed;
  private Image   imgKeySp;
  private Image   imgKeySpPressed;
  private Color   colorRed;
  private Color   colorGreen;
  private Color   colorBlue;
  private Color   colorMagenta;
  private Color   colorCyan;
  private Color   colorYellow;
  private Color   colorKeyNormal;
  private Color   colorKeyPressed;
  private Font    fontLarge;
  private Font    fontLetter;
  private Font    fontStd;
  private int[]   kbMatrix;
  private int     curIdx;
  private int     curX;
  private int     curY;


  public ZXSpectrum48KeyboardFld( ZXSpectrum spectrum )
  {
    super( spectrum, 40, false );
    this.imgBG           = getImage( "bg.png" );
    this.imgG1           = getImage( "g1.png" );
    this.imgG2           = getImage( "g2.png" );
    this.imgG3           = getImage( "g3.png" );
    this.imgG4           = getImage( "g4.png" );
    this.imgG5           = getImage( "g5.png" );
    this.imgG6           = getImage( "g6.png" );
    this.imgG7           = getImage( "g7.png" );
    this.imgG8           = getImage( "g8.png" );
    this.imgLeft         = getImage( "left.png" );
    this.imgRight        = getImage( "right.png" );
    this.imgUp           = getImage( "up.png" );
    this.imgDown         = getImage( "down.png" );
    this.imgKey          = getImage( "key45.png" );
    this.imgKeyPressed   = getImage( "key45_pressed.png" );
    this.imgKeyCs        = getImage( "key65.png" );
    this.imgKeyCsPressed = getImage( "key65_pressed.png" );
    this.imgKeySp        = getImage( "key75.png" );
    this.imgKeySpPressed = getImage( "key75_pressed.png" );
    this.colorRed        = new Color( 192, 0, 0 );
    this.colorGreen      = new Color( 0, 192, 0 );
    this.colorBlue       = new Color( 0, 0, 255 );
    this.colorMagenta    = new Color( 192, 0, 192 );
    this.colorCyan       = new Color( 0, 192, 192 );
    this.colorYellow     = new Color( 192, 192, 0 );
    this.colorKeyNormal  = new Color( 150, 150, 150 );
    this.colorKeyPressed = new Color( 100, 100, 100 );
    this.fontLarge  = new Font( Font.SANS_SERIF, Font.BOLD, FONT_LARGE_H );
    this.fontLetter = new Font( Font.SANS_SERIF, Font.BOLD, FONT_LETTER_H );
    this.fontStd    = new Font( Font.SANS_SERIF, Font.PLAIN, FONT_H );
    this.kbMatrix   = new int[ 8 ];
    this.curIdx     = 0;
    this.curX       = COL1_X1;
    this.curY       = ROW1_Y + (2 * FONT_H) + 4;
    addKey( "1", this.imgG1,   "!",  this.colorRed, 3, 0x01 );
    addKey( "2", this.imgG2,   "@",  this.colorRed, 3, 0x02 );
    addKey( "3", this.imgG3,   "#",  this.colorRed, 3, 0x04 );
    addKey( "4", this.imgG4,   "$",  this.colorRed, 3, 0x08 );
    addKey( "5", this.imgG5,   "%",  this.colorRed, 3, 0x10 );
    addKey( "6", this.imgG6,   "&",  this.colorRed, 4, 0x10 );
    addKey( "7", this.imgG7,   "\'", this.colorRed, 4, 0x08 );
    addKey( "8", this.imgG8,   "(",  this.colorRed, 4, 0x04 );
    addKey( "9", (Image) null, ")",  this.colorRed, 4, 0x02 );
    addKey( "0", (Image) null, "_",  this.colorRed, 4, 0x01 );
    this.curX = COL2_X1;
    this.curY = ROW2_Y + FONT_H + 2;
    addKey( "Q", "<=",  "PLOT",   2, 0x01 );
    addKey( "W", "<>",  "DRAW",   2, 0x02 );
    addKey( "E", "<=",  "REM",    2, 0x04 );
    addKey( "R", "<",   "RUN",    2, 0x08 );
    addKey( "T", ">",   "RAND",   2, 0x10 );
    addKey( "Y", "AND", "RETURN", 5, 0x10 );
    addKey( "U", "OR",  "IF",     5, 0x08 );
    addKey( "I", "AT",  "INPUT",  5, 0x04 );
    addKey( "O", ";",   "POKE",   5, 0x02 );
    addKey( "P", "\"",  "PRINT",  5, 0x01 );
    this.curX = COL3_X1;
    this.curY = ROW3_Y + FONT_H + 2;
    addKey( "A", "STOP", "NEW",   1, 0x01 );
    addKey( "S", "NOT",  "SAVE",  1, 0x02 );
    addKey( "D", "STEP", "DIM",   1, 0x04 );
    addKey( "F", "TO",   "FOR",   1, 0x08 );
    addKey( "G", "THEN", "GOTO",  1, 0x10 );
    addKey( "H", "\u2191",    "GOSUB", 6, 0x10 );
    addKey( "J", "-",    "LOAD",  6, 0x08 );
    addKey( "K", "+",    "LIST",  6, 0x04 );
    addKey( "L", "=",    "LET",   6, 0x02 );
    this.keyEnter     = addKey( 6, 0x01, false );
    this.curY         = ROW4_Y + FONT_H + 2;
    this.keyCapsShift = new KeyData(
				COL4_X1,
				this.curY,
				COL4_X2 - COL4_X1 - 20,
				KEY_H,
				null,
				null,
				null,
				null,
				null,
				0,
				0x01,
				true,
				null );
    this.keys[ this.curIdx++ ] = this.keyCapsShift;
    this.curX = COL4_X2;
    addKey( "Z", ":",      "COPY",   0, 0x02 );
    addKey( "X", "\u00A3", "CLEAR",  0, 0x04 );
    addKey( "C", "?",      "CONT",   0, 0x08 );
    addKey( "V", "/",      "CLS",    0, 0x10 );
    addKey( "B", "*",      "BORDER", 7, 0x10 );
    addKey( "N", ",",      "NEXT",   7, 0x08 );
    addKey( "M", ".",      "PAUSE",  7, 0x04 );
    this.keySymbolShift = new KeyData(
				this.curX,
				this.curY,
				KEY_W,
				KEY_H,
				null,
				null,
				null,
				null,
				null,
				7,
				0x02,
				true,
				null );
    this.keys[ this.curIdx++ ] = this.keySymbolShift;
    this.curX += COL_W;
    this.keySpace = new KeyData(
				this.curX,
				this.curY,
				this.keyEnter.x + this.keyEnter.w - this.curX,
				KEY_H,
				null,
				null,
				null,
				null,
				null,
				7,
				0x01,
				false,
				null );
    this.keys[ this.curIdx++ ] = this.keySpace;
    this.curX += COL_W;

    setPreferredSize(
	new Dimension(
		this.keySpace.x + this.keySpace.w + COL1_X1,
		this.keySpace.y + this.keySpace.h + FONT_H + ROW1_Y ) );
    setShiftKeys( this.keyCapsShift, this.keySymbolShift );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof ZXSpectrum ?
				!((ZXSpectrum) emuSys).isMode128K()
				: false;
  }


  @Override
  public String getKeyboardName()
  {
    return "ZX Spectrum 48K Tastatur";
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
      super.mousePressed( e );
      synchronized( this.selectedKeys ) {
	if( hits( this.keyCapsShift, e )
	    || hits( this.keySymbolShift, e ) )
	{
	  if( isKeySelected( this.keyCapsShift )
	      && !this.keyCapsShift.locked
	      && isKeySelected( this.keySymbolShift )
	      && !this.keySymbolShift.locked )
	  {
	    this.selectedKeys.remove( this.keyCapsShift );
	    this.selectedKeys.remove( this.keySymbolShift );
	  }
	}
      }
    }
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    g.setColor( Color.BLACK );
    g.fillRect( 0, 0, getWidth(), getHeight() );

    // Hintergrund
    if( this.imgBG != null ) {
      g.drawImage( this.imgBG, 0, 0, this );
    }

    // Tasten
    for( int i = 0; i < this.keys.length; i++ ) {
      KeyData key      = this.keys[ i ];
      Image   img      = null;
      boolean selected = isKeySelected( key );
      if( key == this.keyCapsShift ) {
	img = (selected ? this.imgKeyCsPressed : this.imgKeyCs);
      } else if( key == this.keySpace ) {
	img = (selected ? this.imgKeySpPressed : this.imgKeySp);
      } else {
	img = (selected ? this.imgKeyPressed : this.imgKey);
      }
      if( img != null ) {
	g.drawImage( img, key.x, key.y, this );
      } else {
	g.setColor( selected ? this.colorKeyPressed : this.colorKeyNormal );
	g.fillRect( key.x, key.y, key.w, key.h );
      }
      if( key == this.keyEnter ) {
	g.setFont( this.fontStd );
	g.setColor( Color.WHITE );
	drawStringCenter(
		g,
		"ENTER",
		key.x + 5,
		key.y + ((KEY_H - FONT_H) / 2) + FONT_H,
		key.w - 10 );
      } else if( key == this.keyCapsShift ) {
	g.setFont( this.fontLarge );
	g.setColor( Color.WHITE );
	drawStringCenter(
		g,
		"CAPS",
		key.x + 5,
		key.y + (key.h / 2) - 2,
		key.w - 10 );
	drawStringCenter(
		g,
		"SHIFT",
		key.x + 5,
		key.y + (key.h / 2) + FONT_LARGE_H - 1,
		key.w - 10 );
      } else if( key == this.keySymbolShift ) {
	g.setFont( this.fontStd );
	g.setColor( this.colorRed );
	drawStringCenter(
		g,
		"SYMBOL",
		key.x + 5,
		key.y + (key.h / 2) - 2,
		key.w - 10 );
	drawStringCenter(
		g,
		"SHIFT",
		key.x + 5,
		key.y + (key.h / 2) + FONT_LARGE_H - 1,
		key.w - 10 );
      } else if( key == this.keySpace ) {
	g.setColor( Color.WHITE );
	g.setFont( this.fontStd );
	drawStringCenter(
		g,
		"BREAK",
		key.x + 5,
		key.y + (key.h / 2) - 3,
		key.w - 10 );
	g.setFont( this.fontLarge );
	drawStringCenter(
		g,
		"SPACE",
		key.x + 5,
		key.y + (key.h / 2) + FONT_LARGE_H - 1,
		key.w - 10 );
      } else {
	if( key.image != null ) {
	  g.drawImage( key.image, key.x + key.w - 15, key.y + 3, this );
	}
	g.setFont( this.fontLetter );
	if( key.text1 != null ) {
	  g.setColor( Color.WHITE );
	  if( i < 10 ) {
	    g.drawString( key.text1, key.x + 5, key.y + 20 );
	  } else {
	    g.drawString( key.text1, key.x + 3, key.y + FONT_LETTER_H - 1 );
	  }
	}
	g.setFont( this.fontStd );
	if( key.text2 != null ) {
	  g.setColor( this.colorRed );
	  drawStringRight(
		g,
		key.text2,
		key.x + 20,
		key.y + 3 + FONT_H,
		key.w - 23 );
	}
	if( key.text3 != null ) {
	  if( i < 10 ) {
	    g.setColor( this.colorRed );
	    g.drawString( key.text3, key.x + key.w - 14, key.y + KEY_H - 5 );
	  } else {
	    g.setColor( Color.WHITE );
	    drawStringRight(
		g,
		key.text3,
		key.x + 20,
		key.y + KEY_H - 3,
		key.w - 23 );
	  }
	}
      }
    }
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( emuSys instanceof ZXSpectrum ) {
      this.emuSys = (ZXSpectrum) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != ZXSpectrum" );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected Image getImage( String filename )
  {
    return super.getImage( "/images/keyboard/zxspectrum/48k/" + filename );
  }


	/* --- private Methoden --- */

  private KeyData addKey( int col, int value, boolean shift )
  {
    KeyData keyData = new KeyData(
				this.curX,
				this.curY,
				KEY_W,
				KEY_H,
				null,
				null,
				null,
				null,
				null,
				col,
				value,
				shift,
				null );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += COL_W;
    return keyData;
  }


  private void addKey(
		String text1,
		String text2,
		String text3,
		int    col,
		int    value )
  {
    KeyData keyData = new KeyData(
				this.curX,
				this.curY,
				KEY_W,
				KEY_H,
				text1,
				text2,
				text3,
				this.colorGreen,
				null,
				col,
				value,
				false,
				null );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += COL_W;
  }


  private void addKey(
		String text1,
		Image  image,
		String text3,
		Color  color,
		int    col,
		int    value )
  {
    KeyData keyData = new KeyData(
				this.curX,
				this.curY,
				KEY_W,
				KEY_H,
				text1,
				null,
				text3,
				color,
				image,
				col,
				value,
				false,
				null );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += COL_W;
  }
}
