/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die LC80-Tastatur
 */

package jkcemu.emusys.lc80;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.lang.*;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.emusys.LC80;


public class LC80KeyboardFld extends AbstractKeyboardFld<LC80>
{
  private static final int KEY_W     = 35;
  private static final int KEY_COL_W = 48;
  private static final int KEY_X0    = 36;
  private static final int KEY_X1    = KEY_X0 + KEY_COL_W;
  private static final int KEY_X2    = KEY_X1 + KEY_COL_W;
  private static final int KEY_X3    = KEY_X2 + KEY_COL_W;
  private static final int KEY_X4    = KEY_X3 + KEY_COL_W;

  private static final int KEY_H     = 23;
  private static final int KEY_ROW_H = 44;
  private static final int KEY_Y0    = 294;
  private static final int KEY_Y1    = KEY_Y0 + KEY_ROW_H;
  private static final int KEY_Y2    = KEY_Y1 + KEY_ROW_H;
  private static final int KEY_Y3    = KEY_Y2 + KEY_ROW_H;
  private static final int KEY_Y4    = KEY_Y3 + KEY_ROW_H;

  private Image   imgBgLC80;
  private Image   imgBgSC80;
  private KeyData nmiKey;
  private KeyData resetKey;
  private Color   colorKeySelected;
  private Color   colorKeyBorder;
  private Color   colorKeyLight;
  private int[]   kbMatrix;
  private int     curIdx;


  public LC80KeyboardFld( LC80 lc80 )
  {
    super( lc80, 25, true );
    this.imgBgLC80 = getImage( "/images/keyboard/lc80/bg_lc80.png" );
    this.imgBgSC80 = getImage( "/images/keyboard/lc80/bg_sc80.png" );
    this.kbMatrix  = new int[ 6 ];

    this.colorKeySelected = new Color( 100, 100, 100 );
    this.colorKeyBorder   = new Color( 80, 80, 80 );
    this.colorKeyLight    = Color.white;
    Color dark            = new Color( 20, 20, 20 );
    Color red             = new Color( 200, 0, 0 );
    Color light           = this.colorKeyLight;

    this.curIdx   = 0;
    this.resetKey = addKey( KEY_X0, KEY_Y0, red, -1, -1, "Esc" );	// RES
    this.nmiKey   = addKey( KEY_X0, KEY_Y1, red, -1, -1, "N" );		// NMI
    // ST bzw. Contr.
    addKey( KEY_X0, KEY_Y2, dark, 0, 0x40, "S oder F4#F4" );
    // LD bzw. RUN
    addKey( KEY_X0, KEY_Y3, dark, 0, 0x20, "L oder F3#R oder F3" );
    addKey( KEY_X0, KEY_Y4, dark, 0, 0x80, "X oder Enter" );		// EX
    addKey( KEY_X1, KEY_Y0, dark, 5, 0x80, "F1" );	// ADR bzw. New Game
    addKey( KEY_X1, KEY_Y1, light, 4, 0x80, "C#L" );	// C bzw. Lauefer
    addKey( KEY_X1, KEY_Y2, light, 3, 0x80, "8#H oder 8" );	// 8 bzw. H
    addKey( KEY_X1, KEY_Y3, light, 2, 0x80, "4#D oder 4" );	// 4 bzw. D
    addKey( KEY_X1, KEY_Y4, light, 1, 0x80, "0" );		// 0 bzw. SP
    addKey( KEY_X2, KEY_Y0, dark, 5, 0x40, "F2" );	// DAT bzw. Self Play
    addKey( KEY_X2, KEY_Y1, light, 4, 0x40, "D#T" );		// D bzw. Turm
    addKey( KEY_X2, KEY_Y2, light, 3, 0x40, "9" );		// 9
    addKey( KEY_X2, KEY_Y3, light, 2, 0x40, "5#E oder 5" );	// 5 bzw. E
    addKey( KEY_X2, KEY_Y4, light, 1, 0x40, "1#A oder 1" );	// 1 bzw. A
    addKey( KEY_X3, KEY_Y0, dark, 2, 0x20, "+#O oder +" );	// + bzw. Board
    addKey( KEY_X3, KEY_Y1, light, 3, 0x20, "E#M" );		// E bzw. Dame
    addKey( KEY_X3, KEY_Y2, light, 4, 0x20, "A#U" );		// A bzw. Bauer
    addKey( KEY_X3, KEY_Y3, light, 5, 0x20, "6#F oder 6" );	// 6 bzw. F
    addKey( KEY_X3, KEY_Y4, light, 1, 0x20, "2#B oder 2" );	// 2 bzw.B
    addKey( KEY_X4, KEY_Y0, dark, 5, 0x10, "-#W oder -" );	// - bzw. Color
    addKey( KEY_X4, KEY_Y1, light, 4, 0x10, "F#K" );	// F bzw. Koenig
    addKey( KEY_X4, KEY_Y2, light, 3, 0x10, "B#S" );	// B bzw. Springer
    addKey( KEY_X4, KEY_Y3, light, 2, 0x10, "7#G oder 7" );	// 7 bzw. G
    addKey( KEY_X4, KEY_Y4, light, 1, 0x10, "3#C oder 3" );	// 3 bzw. C
    setPreferredSize( new Dimension( 300, 574 ) );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof LC80;
  }


  @Override
  public String getToolTipText( MouseEvent e )
  {
    String rv = super.getToolTipText( e );
    if( rv != null ) {
      int pos = rv.indexOf( '#' );
      if( pos >= 0 ) {
	if( this.emuSys.isChessMode() ) {
	  if( (pos + 1) < rv.length() ) {
	    rv = rv.substring( pos + 1 );
	  } else {
	    rv = null;
	  }
	} else {
	  if( pos > 0 ) {
	    rv = rv.substring( 0, pos );
	  } else {
	    rv = null;
	  }
	}
      }
    }
    return rv;
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
    if( this.emuSys.isChessMode() ) {
      if( this.imgBgSC80 != null ) {
	g.drawImage( this.imgBgSC80, 0, 0, this );
      }
    } else {
      if( this.imgBgLC80 != null ) {
	g.drawImage( this.imgBgLC80, 0, 0, this );
      }
    }
    for( KeyData key : this.keys ) {
      Color c = (isKeySelected( key ) ? this.colorKeySelected : key.color);
      g.setColor( c );
      g.fillRect( key.x, key.y, key.w, key.h );
      if( c == this.colorKeyLight ) {
        g.setColor( this.colorKeyBorder );
	g.drawRect( key.x, key.y, key.w, key.h );
      }
    }
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( emuSys instanceof LC80 ) {
      this.emuSys = (LC80) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != LC80" );
    }
  }


	/* --- private Methoden --- */

  private KeyData addKey(
			int    x,
			int    y,
			Color  color,
			int    col,
			int    value,
			String toolTipText )
  {
    KeyData keyData = new KeyData(
				x,
				y,
				KEY_W,
				KEY_H,
				null,
				null,
				null,
				color,
				null,
				col,
				value,
				false,
				toolTipText );
    this.keys[ this.curIdx++ ] = keyData;
    return keyData;
  }
}
