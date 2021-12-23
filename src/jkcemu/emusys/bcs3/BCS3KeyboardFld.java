/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die BCS3-Tastatur
 */

package jkcemu.emusys.bcs3;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.emusys.BCS3;


public class BCS3KeyboardFld extends AbstractKeyboardFld<BCS3>
{
  private static final int MARGIN   = 20;
  private static final int KEY_SIZE = 50;

  private Image imgLeft;
  private Font  font;
  private int[] kbMatrix;
  private int   curIdx;
  private int   curX;
  private int   curY;


  public BCS3KeyboardFld( BCS3 bcs3 )
  {
    super( bcs3, 40, true );
    this.font     = new Font( Font.SANS_SERIF, Font.PLAIN, 12 );
    this.imgLeft  = getImage( "/images/keyboard/left.png" );
    this.kbMatrix = new int[ 10 ];
    this.curIdx   = 0;
    this.curX     = MARGIN;
    this.curY     = MARGIN;
    addKey( "1", "!",  1, 0x08 );
    addKey( "2", "\"", 2, 0x08 );
    addKey( "3", "#",  3, 0x08 );
    addKey( "4", "$",  4, 0x08 );
    addKey( "5", "%",  5, 0x08 );
    addKey( "6", "&",  6, 0x08 );
    addKey( "7", "\'", 7, 0x08 );
    addKey( "8", "(",  8, 0x08 );
    addKey( "9", ")",  9, 0x08 );
    addKey( "0",       0, 0x08 );

    this.curX = MARGIN + (KEY_SIZE / 5);
    this.curY += KEY_SIZE;

    addKey( "Q", "@", 6, 0x02 );
    addKey( "W",      2, 0x01 );
    addKey( "E", "*", 4, 0x04 );
    addKey( "R",      7, 0x02 );
    addKey( "T",      9, 0x02 );
    addKey( "Z",      5, 0x01 );
    addKey( "U",      0, 0x01 );
    addKey( "I", ".", 8, 0x04 );
    addKey( "O", ">", 4, 0x02 );
    addKey( "P", "?", 5, 0x02 );

    this.curX = MARGIN + (KEY_SIZE * 2 / 5);
    this.curY += KEY_SIZE;
    addKey( "A",      0, 0x04 );
    addKey( "S",      8, 0x02 );
    addKey( "D",      3, 0x04 );
    addKey( "F", "+", 5, 0x04 );
    addKey( "G", ",", 6, 0x04 );
    addKey( "H", "-", 7, 0x04 );
    addKey( "J", "/", 9, 0x04 );
    addKey( "K", ":", 0, 0x02 );
    addKey( "L", ";", 1, 0x02 );
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					KEY_SIZE,
					KEY_SIZE,
					null,
					null,
					null,
					null,
					this.imgLeft,
					7,
					0x01,
					false,
					null );
    int w = this.curX + KEY_SIZE + MARGIN;

    this.curX = MARGIN;
    this.curY += KEY_SIZE;

    KeyData shiftKey = new KeyData(
				this.curX,
				this.curY,
				KEY_SIZE,
				KEY_SIZE,
				"SHIFT",
				null,
				null,
				null,
				null,
				9,
				0x01,
				true,
				null );
    this.keys[ this.curIdx++ ] = shiftKey;
    this.curX += KEY_SIZE;
    addKey( "Y",      4, 0x01 );
    addKey( "X",      3, 0x01 );
    addKey( "C",      2, 0x04 );
    addKey( "V",      1, 0x01 );
    addKey( "B",      1, 0x04 );
    addKey( "N", "=", 3, 0x02 );
    addKey( "M", "<", 2, 0x02 );
    addKey( "SP",     6, 0x01 );
    addKey( "ENTER",  8, 0x01 );

    int h = this.curY + KEY_SIZE + MARGIN;
    setPreferredSize( new Dimension( w, h ) );
    setShiftKeys( shiftKey );
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
    return emuSys instanceof BCS3;
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
    g.setColor( Color.LIGHT_GRAY );
    g.setFont( this.font );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    for( KeyData key : this.keys ) {
      boolean selected = isKeySelected( key );
      if( selected ) {
	g.setColor( Color.GRAY );
	g.fillRect( key.x + 1, key.y + 1, key.w - 1, key.h - 1 );
      }
      g.setColor( Color.LIGHT_GRAY );
      g.draw3DRect( key.x + 1, key.y + 1, key.w - 1, key.h - 1, !selected );
      if( key.image != null ) {
	g.drawImage(
		key.image,
		key.x + ((key.w - key.image.getWidth( this )) / 2) + 1,
		key.y + ((key.h - key.image.getHeight( this )) / 4 * 3) + 1,
		this );
      }
      g.setColor( Color.BLACK );
      if( key.text1 != null ) {
	drawCentered( g, key.text1, key.x, key.y, key.w );
      }
      if( key.text2 != null ) {
	drawCentered(
		g,
		key.text2,
		key.x,
		key.y - (KEY_SIZE / 2) + 2,
		key.w );
      }
    }
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( emuSys instanceof BCS3 ) {
      this.emuSys = (BCS3) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != BCS3" );
    }
  }


	/* --- private Methoden --- */

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
					false,
					null );
    this.curX += KEY_SIZE;
  }


  private void addKey(
		String textNormal,
		int    col,
		int    value )
  {
    addKey( textNormal, null, col, value );
  }


  private void drawCentered( Graphics g, String text, int x, int y, int w )
  {
    if( text != null ) {
      FontMetrics fm = g.getFontMetrics();
      if( fm != null ) {
	int wText = fm.stringWidth( text );
	x += ((w - wText) / 2);
      } else {
	x += 5;
      }
      g.drawString( text, x + 2, y + KEY_SIZE - 5 );
    }
  }
}
