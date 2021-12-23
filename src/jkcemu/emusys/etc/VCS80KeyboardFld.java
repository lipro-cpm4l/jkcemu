/*
 * (c) 2012-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die VCS80-Tastatur
 */

package jkcemu.emusys.etc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.emusys.VCS80;


public class VCS80KeyboardFld extends AbstractKeyboardFld<VCS80>
{
  private static final int MARGIN    = 10;
  private static final int FONT_SIZE = 18;
  private static final int KEY_SIZE  = 40;

  private Font  fontBtn;
  private int[] kbMatrix;
  private int   curIdx;
  private int   curX;
  private int   curY;


  public VCS80KeyboardFld( VCS80 vcs80 )
  {
    super( vcs80, 24, true );
    this.fontBtn  = new Font( Font.SANS_SERIF, Font.PLAIN, FONT_SIZE );
    this.kbMatrix = new int[ 8 ];
    this.curIdx   = 0;
    this.curX     = MARGIN;
    this.curY     = MARGIN;
    addKey( "C", 3, 0x20, null );
    addKey( "D", 2, 0x20, null );
    addKey( "E", 1, 0x20, null );
    addKey( "F", 0, 0x20, null );
    this.curX += (KEY_SIZE / 2);
    addKey( "PE", 0, 0x40, "P" );
    addKey( "ST", 1, 0x40, "S" );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "8", 7, 0x20, null );
    addKey( "9", 6, 0x20, null );
    addKey( "A", 5, 0x20, null );
    addKey( "B", 4, 0x20, null );
    this.curX += (KEY_SIZE / 2);
    addKey( "TR", 2, 0x40, "T" );
    addKey( "GO", 3, 0x40, "G" );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "4", 3, 0x10, null );
    addKey( "5", 2, 0x10, null );
    addKey( "6", 1, 0x10, null );
    addKey( "7", 0, 0x10, null );
    this.curX += (KEY_SIZE / 2);
    addKey( "RE", 4, 0x40, "R" );
    addKey( "MA", 5, 0x40, "M" );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( "0", 7, 0x10, null );
    addKey( "1", 6, 0x10, null );
    addKey( "2", 5, 0x10, null );
    addKey( "3", 4, 0x10, null );
    this.curX += (KEY_SIZE / 2);
    addKey( "A-", 6, 0x40, "-" );
    addKey( "A+", 7, 0x40, "+" );

    int h = this.curY + KEY_SIZE + MARGIN;
    setPreferredSize(
	new Dimension(
		(2 * MARGIN) + (6 * KEY_SIZE) + (KEY_SIZE / 2),
		(2 * MARGIN) + (4 * KEY_SIZE) ) );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof VCS80;
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
    if( emuSys instanceof VCS80 ) {
      this.emuSys = (VCS80) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != VCS80" );
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
}
