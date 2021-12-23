/*
 * (c) 2011-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Z1013-Folienflachtastatur
 */

package jkcemu.emusys.z1013;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuUtil;
import jkcemu.emusys.Z1013;


public class Z1013KeyboardFld8x4 extends AbstractKeyboardFld<Z1013>
{
  private static final String[] keyImgResources = {
			"/images/keyboard/z1013/key_at.png",
			"/images/keyboard/z1013/key_a.png",
			"/images/keyboard/z1013/key_b.png",
			"/images/keyboard/z1013/key_c.png",
			"/images/keyboard/z1013/key_d.png",
			"/images/keyboard/z1013/key_e.png",
			"/images/keyboard/z1013/key_f.png",
			"/images/keyboard/z1013/key_g.png",
			"/images/keyboard/z1013/key_h.png",
			"/images/keyboard/z1013/key_i.png",
			"/images/keyboard/z1013/key_j.png",
			"/images/keyboard/z1013/key_k.png",
			"/images/keyboard/z1013/key_l.png",
			"/images/keyboard/z1013/key_m.png",
			"/images/keyboard/z1013/key_n.png",
			"/images/keyboard/z1013/key_o.png",
			"/images/keyboard/z1013/key_p.png",
			"/images/keyboard/z1013/key_q.png",
			"/images/keyboard/z1013/key_r.png",
			"/images/keyboard/z1013/key_s.png",
			"/images/keyboard/z1013/key_t.png",
			"/images/keyboard/z1013/key_u.png",
			"/images/keyboard/z1013/key_v.png",
			"/images/keyboard/z1013/key_w.png",
			"/images/keyboard/z1013/key_shift1.png",
			"/images/keyboard/z1013/key_shift2.png",
			"/images/keyboard/z1013/key_shift3.png",
			"/images/keyboard/z1013/key_shift4.png",
			"/images/keyboard/z1013/key_left.png",
			"/images/keyboard/z1013/key_space.png",
			"/images/keyboard/z1013/key_right.png",
			"/images/keyboard/z1013/key_enter.png" };

  private static final int MARGIN = 5;

  private int   keyWidth;
  private int   keyHeight;
  private int[] kbMatrix;


  public Z1013KeyboardFld8x4( Z1013 z1013 )
  {
    super( z1013, 32, true );
    this.keyWidth  = 0;
    this.keyHeight = 0;
    this.kbMatrix  = new int[ 8 ];

    int x = MARGIN;
    int y = MARGIN;
    Toolkit tk = EmuUtil.getToolkit( this );
    if( tk != null ) {
      int m = 0x01;
      for( int i = 0; i < keyImgResources.length; i++ ) {
	int col = i % 8;
	if( (i > 0) && (col == 0) ) {
	  m <<= 1;
	  x = MARGIN;
	  y += (this.keyHeight + MARGIN);
	}
	Image img = getImage( keyImgResources[ i ] );
	if( img != null ) {
	  if( this.keyWidth <= 0 ) {
	    this.keyWidth = Math.max( 0, img.getWidth( this ) );
	  }
	  if( this.keyHeight <= 0 ) {
	    this.keyHeight = Math.max( 0, img.getHeight( this ) );
	  }
	}
	boolean shift       = false;
	String  toolTipText = null;
	if( (m == 0x08) && (col < 4) ) {
	  shift = true;
	  toolTipText = String.format( "F%d", col + 1 );
	}
	this.keys[ i ] = new KeyData(
				x,
				y,
				this.keyWidth,
				this.keyHeight,
				null,
				null,
				null,
				null,
				img,
				col,
				m,
				shift,
				toolTipText );
	x += (this.keyWidth + MARGIN);
      }
    }
    setPreferredSize( new Dimension( x, y + this.keyHeight + MARGIN ) );
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
	  if( kbMatrix instanceof KeyboardMatrix8x4 ) {
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
    return "Z1013 Folienflachtastatur";
  }


  @Override
  protected void keySelectionChanged()
  {
    Z1013Keyboard  kb = this.emuSys.getZ1013Keyboard();
    KeyboardMatrix km = kb.getKeyboardMatrix();
    if( km instanceof KeyboardMatrix8x4 ) {
      Arrays.fill( this.kbMatrix, 0 );
      synchronized( this.selectedKeys ) {
	for( KeyData key : this.selectedKeys ) {
	  if( (key.col >= 0) && (key.col < this.kbMatrix.length) ) {
	    this.kbMatrix[ key.col ] |= key.value;
	  }
	}
      }
      ((KeyboardMatrix8x4) km).updKeyboardMatrix( this.kbMatrix );
      kb.putRowValuesToPIO();
    }
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    int w = getWidth();
    int h = getHeight();
    if( (w > 0) && (h > 0) && (this.keyWidth > 0) && (this.keyHeight > 0) ) {
      g.setColor( Color.GRAY );
      g.fillRect( 0, 0, w, h );
      for( KeyData key : this.keys ) {
	if( key.image != null ) {
	  if( isKeySelected( key ) ) {
	    g.setColor( Color.DARK_GRAY );
	    g.fillRect( key.x, key.y, this.keyWidth, this.keyHeight );
	  }
	  g.drawImage( key.image, key.x, key.y, this );
	}
      }
    }
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( accepts( emuSys ) ) {
      this.emuSys = (Z1013) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != Z1013/8x4" );
    }
  }
}
