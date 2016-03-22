/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die KC85-Tastatur
 */

package jkcemu.emusys.kc85;

import java.awt.*;
import java.lang.*;
import jkcemu.base.*;
import jkcemu.emusys.KC85;


public class KC85KeyboardFld extends AbstractKC85KeyboardFld
{
  private static final int FS_KEY     = 14;
  private static final int FS_NOSHIFT = 12;
  private static final int FS_SHIFT   = 10;
  private static final int MARGIN_X   = 15;
  private static final int MARGIN_Y   = 20;
  private static final int KEY_COL_W  = 50;
  private static final int KEY_ROW_H  = 50;
  private static final int KEY_PAD_X  = MARGIN_X + 1;
  private static final int KEY_PAD_Y  = MARGIN_Y + 1;
  private static final int KEY_PAD_W  = 620;
  private static final int KEY_PAD_H  = 272;
  private static final int X_CRS_MID  = 695;

  private Image   imgBG;
  private Image   imgKeySmall;
  private Image   imgKeySmallPressed;
  private Image   imgKeySpace;
  private Image   imgKeySpacePressed;
  private Image   imgLeft;
  private Image   imgRight;
  private Image   imgUp;
  private Image   imgDown;
  private Image   imgEnter;
  private Image   imgShift;
  private Image   imgShLock;
  private Color   colorBG;
  private Font    fontKey;
  private Font    fontNoShift;
  private Font    fontShift;
  private KeyData shiftKey;
  private KeyData spaceKey;
  private int     wKey;
  private int     hKey;
  private int     curIdx;
  private int     curX;
  private int     curY;


  public KC85KeyboardFld( KC85 kc85 )
  {
    super( kc85, 64 );
    this.imgKeySmall        = getImage( "key_small_gray.png" );
    this.imgKeySmallPressed = getImage( "key_small_pressed.png" );
    this.imgKeySpacePressed = getImage( "key_space_pressed.png" );
    this.imgLeft            = getImage( "left.png" );
    this.imgRight           = getImage( "right.png" );
    this.imgUp              = getImage( "up.png" );
    this.imgDown            = getImage( "down.png" );
    this.imgEnter           = getImage( "enter.png" );
    this.imgShift           = getImage( "shift.png" );
    this.imgShLock          = getImage( "shiftlock.png" );
    prepareLayout();
    this.wKey = 0;
    this.hKey = 0;
    if( this.imgKeySmall != null ) {
      this.wKey = this.imgKeySmall.getWidth( this );
      this.hKey = this.imgKeySmall.getHeight( this );
    }
    this.fontKey     = new Font( "SansSerif", Font.BOLD, FS_KEY );
    this.fontNoShift = new Font( "SansSerif", Font.BOLD, FS_NOSHIFT );
    this.fontShift   = new Font( "SansSerif", Font.BOLD, FS_SHIFT );
    this.curIdx      = 0;

    int dist  = (KEY_PAD_W - ((KEY_COL_W * 11) + this.wKey)) / 3;
    int xKey0 = KEY_PAD_X + (2 * dist);
    this.curX = xKey0;
    this.curY = KEY_PAD_Y + 16 + ((KEY_PAD_H - 35) / 10) - (this.hKey / 2);
    addKey( "F1", 124, "F1" );
    addKey( "F2", 12, "F2" );
    addKey( "F3", 28, "F3" );
    addKey( "F4", 108, "F4" );
    addKey( "F5", 44, "F5" );
    addKey( "F6", 92, "F6" );
    addKey( "BRK", 60, "F7" );
    addKey( "STOP", 76, "F8" );
    addKey( "INS", 56, "Einfg" );
    addKey( "DEL", 40, "Entf" );
    addKey( "CLR", 24, "F9" );
    addKey( "HOME", 8, "Pos1" );

    this.curX = xKey0;
    this.curY += KEY_ROW_H;
    addKey( "1", "!", 116 );
    addKey( "2", "\"", 4 );
    addKey( "3", "#", 20 );
    addKey( "4", "$", 100 );
    addKey( "5", "%", 36 );
    addKey( "6", "&", 84 );
    addKey( "7", "\'", 52 );
    addKey( "8", "(", 68 );
    addKey( "9", ")", 58 );
    addKey( "0", "@", 42 );
    addKey( ":", "*", 26 );
    addKey( "-", "=", 10 );
    this.curX += KEY_COL_W;
    int xUp = X_CRS_MID - (this.wKey / 2);
    addKey(
	xUp,
	this.curY,
	this.wKey,
	null,
	null,
	this.imgUp,
	120,
	null );

    this.curX = xKey0 + ((KEY_COL_W + this.wKey) / 2) - (this.wKey / 2);
    this.curY += KEY_ROW_H;
    addKey( "Q", 112 );
    addKey( "W", 0 );
    addKey( "E", 16 );
    addKey( "R", 96 );
    addKey( "T", 32 );
    addKey( "Z", 80 );
    addKey( "U", 48 );
    addKey( "I", 64 );
    addKey( "O", 54 );
    addKey( "P", 38 );
    addKey( "^", "\u00AC", 22 );
    this.curX += KEY_COL_W;

    this.curX = xUp - (KEY_COL_W / 2);
    addKey(
	this.curX,
	this.curY,
	this.wKey,
	null,
	null,
	this.imgLeft,
	6,
	null );
    int wPref = this.curX + this.wKey + MARGIN_X;
    addKey(
	this.curX,
	this.curY,
	this.wKey,
	null,
	null,
	this.imgRight,
	122,
	null );

    int xKey3 = KEY_PAD_X + dist;
    this.curX = xKey3;
    this.curY += KEY_ROW_H;
    addKey(
	this.curX,
	this.curY,
	this.wKey,
	null,
	null,
	this.imgShLock,
	114,
	null );
    addKey( "A", 2 );
    addKey( "S", 18 );
    addKey( "D", 98 );
    addKey( "F", 34 );
    addKey( "G", 82 );
    addKey( "H", 50 );
    addKey( "J", 66 );
    addKey( "K", 72 );
    addKey( "L", 88 );
    addKey( "+", ";", 104 );
    addKey( "_", "|", 102 );
    this.curX = xUp;
    addKey(
	this.curX,
	this.curY,
	this.wKey,
	null,
	null,
	this.imgDown,
	118,
	null );

    int xKey4 = xKey3 + ((KEY_COL_W + this.wKey) / 2) - (this.wKey / 2);
    this.curX = xKey4;
    this.curY += KEY_ROW_H;
    this.shiftKey = new KeyData(
				this.curX,
				this.curY,
				this.wKey,
				this.hKey,
				null,
				null,
				null,
				null,
				this.imgShift,
				0,
				-1,
				true,
				null );
    this.keys[ this.curIdx++ ] = this.shiftKey;
    this.curX += KEY_COL_W;

    addKey( "Y", 14 );
    addKey( "X", 30 );
    addKey( "C", 110 );
    addKey( "V", 46 );
    addKey( "B", 94 );
    addKey( "N", 62 );
    addKey( "M", 78 );
    addKey( ",", "<", 74 );
    addKey( ".", ">", 90 );
    addKey( "/", "?", 106 );
    this.curX = xUp;
    addKey(
	this.curX,
	this.curY,
	this.wKey,
	null,
	null,
	this.imgEnter,
	126,
	"Enter" );

    int wSpaceKey = 0;
    int hSpaceKey = 0;
    if( this.imgKeySpace != null ) {
      wSpaceKey = this.imgKeySpace.getWidth( this );
      hSpaceKey = this.imgKeySpace.getHeight( this );
    }
    this.spaceKey = new KeyData(
		xKey4 + ((((KEY_COL_W * 10) + this.wKey) - wSpaceKey) / 2),
		KEY_PAD_Y + KEY_PAD_H + hSpaceKey,
		wSpaceKey,
		hSpaceKey,
		null,
		null,
		null,
		null,
		null,
		0,
		70,
		false,
		null );
    this.keys[ this.curIdx++ ] = this.spaceKey;

    setPreferredSize(
		new Dimension(
			wPref,
			KEY_PAD_Y + KEY_PAD_H + (2 * hSpaceKey) + MARGIN_Y) );
    setShiftKeys( this.shiftKey );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof KC85;
  }


  @Override
  protected void keySelectionChanged()
  {
    int     keyNum = -1;
    boolean shift  = false;
    synchronized( this.selectedKeys ) {
      for( KeyData key : this.selectedKeys ) {
	if( key.shift ) {
	  shift = true;
	} else {
	  if( key.value >= 0 ) {
	    keyNum = key.value;
	  }
	}
      }
    }
    if( shift && (keyNum >= 0) ) {
      keyNum++;
    }
    ((KC85) this.emuSys ).setKeyNumPressed( keyNum );
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    g.setColor( this.colorBG );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    if( this.imgBG != null ) {
      g.drawImage( this.imgBG, MARGIN_X, MARGIN_Y, this );
    }
    for( KeyData key : this.keys ) {
      drawKey( g, key, isKeySelected( key ) );
    }
  }


  @Override
  protected Image getImage( String resource )
  {
    return super.getImage( "/images/keyboard/kc85/" + resource );
  }


  @Override
  public void setEmuSys( EmuSys emuSys ) throws IllegalArgumentException
  {
    if( emuSys instanceof KC85 ) {
      this.emuSys = (KC85) emuSys;
      prepareLayout();
      repaint();
    } else {
      throw new IllegalArgumentException( "EmuSys != KC85" );
    }
  }


  @Override
  public void updKeySelection( int keyNum )
  {
    boolean dirty = false;
    synchronized( this.selectedKeys ) {
      dirty = !this.selectedKeys.isEmpty();
      this.selectedKeys.clear();
      if( keyNum >= 0 ) {
	for( KeyData key : this.keys ) {
	  if( key.value == keyNum ) {
	    this.selectedKeys.add( key );
	    dirty = true;
	  }
	  else if( (key.value >= 0) && ((key.value + 1) == keyNum) ) {
	    this.selectedKeys.add( this.shiftKey );
	    this.selectedKeys.add( key );
	    dirty = true;
	  }
	}
      }
    }
    if( dirty ) {
      repaint();
    }
  }


	/* --- private Methoden --- */

  private void addKey(
		int     x,
		int     y,
		int     w,
		String  textNormal,
		String  textShift,
		Image   image,
		int     value,
		String  toolTipText )
  {
    this.keys[ this.curIdx++ ] = new KeyData(
					x,
					y,
					w,
					this.hKey,
					textNormal,
					textShift,
					null,
					null,
					image,
					0,
					value,
					false,
					toolTipText );
    this.curX += KEY_COL_W;
  }


  private void addKey( String textNormal, int value )
  {
    addKey(
	this.curX,
	this.curY,
	this.wKey,
	textNormal,
	null,
	null,
	value,
	null );
  }


  private void addKey( String textNormal, int value, String toolTipText )
  {
    addKey(
	this.curX,
	this.curY,
	this.wKey,
	textNormal,
	null,
	null,
	value,
	toolTipText );
  }


  private void addKey( String textNormal, String textShift, int value )
  {
    addKey(
	this.curX,
	this.curY,
	this.wKey,
	textNormal,
	textShift,
	null,
	value,
	null );
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


  private void drawCenter( Graphics g, int x, int y, int w, int h, Image img )
  {
    if( img != null ) {
      int wImg = img.getWidth( this );
      int hImg = img.getHeight( this );
      if( (wImg > 0) && (h > 0) ) {
	g.drawImage(
		img,
		x + ((w - wImg) / 2),
		y + ((h - hImg) / 2),
		this );
      }
    }
  }


  private void drawKey( Graphics g, KeyData key, boolean selected )
  {
    if( key == this.spaceKey ) {
      if( selected ) {
	if( this.imgKeySpacePressed != null ) {
	  g.drawImage( this.imgKeySpacePressed, key.x, key.y, this );
	}
      } else {
	if( this.imgKeySpace != null ) {
	  g.drawImage( this.imgKeySpace, key.x, key.y, this );
	}
      }
    } else {
      if( selected ) {
	if( this.imgKeySmallPressed != null ) {
	  g.drawImage( this.imgKeySmallPressed, key.x, key.y, this );
	}
      } else {
	if( this.imgKeySmall != null ) {
	  g.drawImage( this.imgKeySmall, key.x, key.y, this );
	}
      }
      g.setColor( Color.black );
      if( key.image != null ) {
	drawCenter( g, key.x, key.y, key.w, key.h, key.image );
      } else if( key.text1 != null ) {
	if( key.text2 != null ) {
	  drawCenter(
		g,
		key.x + 6,
		key.y + key.h - 6,
		(key.w / 2) - 4,
		key.text1 );
	  drawCenter(
		g,
		key.x + (key.w / 2) + 1,
		key.y + 5 + FS_SHIFT,
		(key.w / 2) - 4,
		key.text2 );
	} else {
	  drawCenter(
		g,
		key.x,
		key.y + ((key.h - FS_KEY) / 2) + FS_KEY - 2,
		key.w,
		key.text1 );
	}
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


  private void prepareLayout()
  {
    if( ((KC85) this.emuSys ).getKCTypeNum() >= 4 ) {
      this.imgBG       = getImage( "bg_gray.png" );
      this.imgKeySpace = getImage( "key_space_gray.png" );
      this.colorBG     = new Color( 230, 230, 220 );
    } else {
      if( this.emuSys.getTitle().startsWith( "HC" ) ) {
	this.imgBG = getImage( "bg_hc900.png" );
      } else {
	this.imgBG = getImage( "bg_dark.png" );
      }
      this.imgKeySpace = getImage( "key_space_dark.png" );
      this.colorBG     = new Color( 40, 45, 50 );
    }
  }
}

