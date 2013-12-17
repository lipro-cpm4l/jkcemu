/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die C80-Tastatur
 */

package jkcemu.emusys.etc;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.*;
import java.util.Arrays;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.emusys.C80;


public class C80KeyboardFld extends AbstractKeyboardFld<C80>
{
  private static final int MARGIN    = 10;
  private static final int FONT_SIZE = 16;
  private static final int KEY_COL_W = 60;
  private static final int KEY_ROW_H = 40;
  private static final int KEY_W     = 50;
  private static final int KEY_H     = 30;

  private Font    fontBtn;
  private KeyData resetKey;
  private KeyData nmiKey;
  private int[]   kbMatrix;
  private int     curIdx;
  private int     curX;
  private int     curY;


  public C80KeyboardFld( C80 c80 )
  {
    super( c80, 24, true );
    this.fontBtn  = new Font( "SansSerif", Font.PLAIN, FONT_SIZE );
    this.kbMatrix = new int[ 8 ];
    this.curIdx   = 0;
    this.curX     = MARGIN;
    this.curY     = MARGIN;
    addKey( "D", 2, 0x01 );
    addKey( "E", 2, 0x02 );
    addKey( "F", 2, 0x04 );
    this.resetKey = addKey( "RES", -1, -1, "Esc" );

    this.curX = MARGIN;
    this.curY += KEY_ROW_H;
    addKey( "A", 3, 0x01 );
    addKey( "B", 3, 0x02 );
    addKey( "C", 3, 0x04 );
    this.nmiKey = addKey( "BRK", -1, -1, "N" );

    this.curX = MARGIN;
    this.curY += KEY_ROW_H;
    addKey( "7",  4, 0x01 );
    addKey( "8",  4, 0x02 );
    addKey( "9",  4, 0x04 );
    addKey( "GO", 1, 0x01, "G" );

    this.curX = MARGIN;
    this.curY += KEY_ROW_H;
    addKey( "4",   5, 0x01 );
    addKey( "5",   5, 0x02 );
    addKey( "6",   5, 0x04 );
    addKey( "REG", 0, 0x01, "R" );

    this.curX = MARGIN;
    this.curY += KEY_ROW_H;
    addKey( "1", 6, 0x01 );
    addKey( "2", 6, 0x02 );
    addKey( "3", 6, 0x04 );
    addKey( "-", 1, 0x02, "-" );

    this.curX = MARGIN;
    this.curY += KEY_ROW_H;
    addKey( "FCN", 7, 0x01, "F1" );
    addKey( "0",   7, 0x02 );
    addKey( "MEM", 7, 0x04, "M" );
    addKey( "+",   0, 0x02, "+ oder Enter" );

    int h = this.curY + KEY_ROW_H + MARGIN;
    setPreferredSize(
	new Dimension(
		(2 * MARGIN) + (3 * KEY_COL_W) + KEY_W,
		(2 * MARGIN) + (5 * KEY_ROW_H) + KEY_H ) );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof C80;
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
      } else if( hits( this.nmiKey, e ) ) {
	fireNMIAfterDelay();
      }
      super.mousePressed( e );
    }
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    g.setPaintMode();
    g.setColor( Color.lightGray );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    g.setFont( this.fontBtn );
    for( KeyData key : this.keys ) {
      boolean selected = isKeySelected( key );
      if( selected ) {
	g.setColor( Color.gray );
	g.fillRect( key.x + 1, key.y + 1, key.w - 1, key.h - 1 );
      }
      g.setColor( Color.lightGray );
      g.draw3DRect( key.x + 1, key.y + 1, key.w - 1, key.h - 1, !selected );
      if( key.text1 != null ) {
	FontMetrics fm = g.getFontMetrics();
	if( fm != null ) {
	  g.setColor( Color.black );
	  g.drawString(
		key.text1,
		key.x + ((key.w - fm.stringWidth( key.text1 )) / 2) + 1,
		key.y + FONT_SIZE + ((key.h - FONT_SIZE) / 2) - 1 );
	}
      }
    }
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( emuSys instanceof C80 ) {
      this.emuSys = (C80) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != C80" );
    }
  }


	/* --- private Methoden --- */

  private KeyData addKey( String text, int col, int value, String toolTipText )
  {
    KeyData keyData = new KeyData(
				this.curX,
				this.curY,
				KEY_W,
				KEY_H,
				text,
				null,
				null,
				null,
				null,
				col,
				value,
				false,
				toolTipText );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += KEY_COL_W;
    return keyData;
  }


  private KeyData addKey( String text, int col, int value )
  {
    return addKey( text, col, value, null );
  }
}
