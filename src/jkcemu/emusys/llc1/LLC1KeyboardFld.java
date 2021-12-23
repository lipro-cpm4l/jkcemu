/*
 * (c) 2012-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die LLC1-Hexadezimaltastatur
 */

package jkcemu.emusys.llc1;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.emusys.LLC1;


public class LLC1KeyboardFld extends AbstractKeyboardFld<LLC1>
{
  private static final int MARGIN    = 10;
  private static final int FONT_SIZE = 18;
  private static final int KEY_SIZE  = 50;

  private Font  fontBtn;
  private int[] kbMatrix;
  private int   curIdx;
  private int   curX;
  private int   curY;


  public LLC1KeyboardFld( LLC1 llc1 )
  {
    super( llc1, 22, true );
    this.fontBtn  = new Font( Font.SANS_SERIF, Font.PLAIN, FONT_SIZE );
    this.kbMatrix = new int[ 4 ];
    this.curIdx   = 0;
    this.curX     = MARGIN;
    this.curY     = MARGIN;
    addKey( "C", 0, 0x81 );
    addKey( "D", 1, 0x81 );
    addKey( "E", 2, 0x81 );
    addKey( "F", 3, 0x81 );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "8", 0, 0x04 );
    addKey( "9", 1, 0x04 );
    addKey( "A", 2, 0x04 );
    addKey( "B", 3, 0x04 );
    this.curX += (KEY_SIZE / 2);
    addKey( "EIN", 1, 0x82, "M" );
    addKey( "REG", 0, 0x82, "R" );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "4", 0, 0x02 );
    addKey( "5", 1, 0x02 );
    addKey( "6", 2, 0x02 );
    addKey( "7", 3, 0x02 );
    this.curX += (KEY_SIZE / 2);
    addKey( "HP", 2, 0x84, "H" );
    addKey( "ES", 0, 0x84, "S" );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "0", 0, 0x01 );
    addKey( "1", 1, 0x01 );
    addKey( "2", 2, 0x01 );
    addKey( "3", 3, 0x01 );
    this.curX += (KEY_SIZE / 2);
    addKey( "DL", 1, 0x84, "G oder J" );
    addKey( "ST", 2, 0x82, "X oder Enter" );

    setPreferredSize(
	new Dimension(
		(2 * MARGIN) + (6 * KEY_SIZE) + (KEY_SIZE / 2),
		(2 * MARGIN) + (4 * KEY_SIZE) ) );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof LLC1;
  }


  @Override
  public String getKeyboardName()
  {
    return "LLC1 Hexadezimaltastatur";
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
    g.setFont( this.fontBtn );;
    g.setPaintMode();
    g.setColor( Color.LIGHT_GRAY );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    for( KeyData key : this.keys ) {
      boolean selected = isKeySelected( key );
      if( selected ) {
	g.setColor( Color.GRAY );
	g.fillRect( key.x + 1, key.y + 1, key.w - 1, key.h - 1 );
      }
      g.setColor( Color.LIGHT_GRAY );
      g.draw3DRect( key.x + 1, key.y + 1, key.w - 1, key.h - 1, !selected );
      if( key.text1 != null ) {
	FontMetrics fm = g.getFontMetrics();
	if( fm != null ) {
	  g.setColor( Color.BLACK );
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
    if( emuSys instanceof LLC1 ) {
      this.emuSys = (LLC1) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != LLC1" );
    }
  }


  @Override
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
	      if( (key.col == col) && (key.value == kbMatrix[ col ]) ) {
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


	/* --- private Methoden --- */

  private void addKey( String text, int col, int value, String toolTipText )
  {
    this.keys[ this.curIdx++ ] = new KeyData(
					this.curX,
					this.curY,
					KEY_SIZE,
					KEY_SIZE,
					text,
					null,
					null,
					null,
					null,
					col,
					value,
					false,
					toolTipText );
    this.curX += KEY_SIZE;
  }


  private void addKey( String text, int col, int value )
  {
    addKey( text, col, value, null );
  }
}
