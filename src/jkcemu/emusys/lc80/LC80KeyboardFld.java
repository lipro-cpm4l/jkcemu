/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die LC80-Tastatur
 */

package jkcemu.emusys.lc80;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.*;
import java.util.Arrays;
import jkcemu.base.*;
import jkcemu.emusys.LC80;


public class LC80KeyboardFld extends AbstractKeyboardFld
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

  private ScreenFrm screenFrm;
  private LC80      lc80;
  private Image     imgBgLC80;
  private Image     imgBgSC80;
  private KeyData   nmiKey;
  private KeyData   resetKey;
  private Color     colorKeySelected;
  private Color     colorKeyBorder;
  private Color     colorKeyLight;
  private int[]     kbMatrix;
  private int       curIdx;


  public LC80KeyboardFld( ScreenFrm screenFrm, LC80 lc80 )
  {
    super( 25 );
    this.screenFrm = screenFrm;
    this.lc80      = lc80;
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
    this.resetKey = addKey( KEY_X0, KEY_Y0, red, -1, -1 );	// RES
    this.nmiKey   = addKey( KEY_X0, KEY_Y1, red, -1, -1 );	// NMI
    addKey( KEY_X0, KEY_Y2, dark, 0, 0x40 );			// ST
    addKey( KEY_X0, KEY_Y3, dark, 0, 0x20 );			// LD
    addKey( KEY_X0, KEY_Y4, dark, 0, 0x80 );			// EX
    addKey( KEY_X1, KEY_Y0, dark, 5, 0x80 );			// ADR
    addKey( KEY_X1, KEY_Y1, light, 4, 0x80 );			// C
    addKey( KEY_X1, KEY_Y2, light, 3, 0x80 );			// 8
    addKey( KEY_X1, KEY_Y3, light, 2, 0x80 );			// 4
    addKey( KEY_X1, KEY_Y4, light, 1, 0x80 );			// 0
    addKey( KEY_X2, KEY_Y0, dark, 5, 0x40 );			// DAT
    addKey( KEY_X2, KEY_Y1, light, 4, 0x40 );			// D
    addKey( KEY_X2, KEY_Y2, light, 3, 0x40 );			// 9
    addKey( KEY_X2, KEY_Y3, light, 2, 0x40 );			// 5
    addKey( KEY_X2, KEY_Y4, light, 1, 0x40 );			// 1
    addKey( KEY_X3, KEY_Y0, dark, 2, 0x20 );			// +
    addKey( KEY_X3, KEY_Y1, light, 3, 0x20 );			// E
    addKey( KEY_X3, KEY_Y2, light, 4, 0x20 );			// A
    addKey( KEY_X3, KEY_Y3, light, 5, 0x20 );			// 6
    addKey( KEY_X3, KEY_Y4, light, 1, 0x20 );			// 2
    addKey( KEY_X4, KEY_Y0, dark, 5, 0x10 );			// -
    addKey( KEY_X4, KEY_Y1, light, 4, 0x10 );			// F
    addKey( KEY_X4, KEY_Y2, light, 3, 0x10 );			// B
    addKey( KEY_X4, KEY_Y3, light, 2, 0x10 );			// 7
    addKey( KEY_X4, KEY_Y4, light, 1, 0x10 );			// 3
    setPreferredSize( new Dimension( 300, 574 ) );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof LC80;
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
    this.lc80.updKeyboardMatrix( this.kbMatrix );
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    if( e.getComponent() == this ) {
      if( hits( this.resetKey, e ) ) {
	this.screenFrm.fireReset( EmuThread.ResetLevel.WARM_RESET );
	e.consume();
      } else if( hits( this.nmiKey, e ) ) {
	EmuThread emuThread = this.screenFrm.getEmuThread();
	if( emuThread != null ) {
	  emuThread.getZ80CPU().fireNMI();
	}
      } else {
	super.mousePressed( e );
      }
    }
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    if( this.lc80.isChessMode() ) {
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
      this.lc80 = (LC80) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != LC80" );
    }
  }


	/* --- private Methoden --- */

  private KeyData addKey( int x, int y, Color color, int col, int value )
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
				false );
    this.keys[ this.curIdx++ ] = keyData;
    return keyData;
  }
}

