/*
 * (c) 2012-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die SC2-Tastatur
 */

package jkcemu.emusys.etc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import jkcemu.base.EmuSys;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.emusys.SC2;


public class SC2KeyboardFld extends AbstractKeyboardFld<SC2>
{
  private static final int MARGIN   = 5;
  private static final int LED_SIZE = 12;
  private static final int KEY_SIZE = 60;
  private static final int KEY_Y0   = 50;

  private Color   colorBg;
  private Color   colorLEDon;
  private Color   colorLEDoff;
  private Color   colorTextDark;
  private Color   colorTextLight;
  private Font    fontTitle;
  private Font    fontLabel;
  private Font    fontKey;
  private Image   imgLedFld;
  private Image   imgKeyDark;
  private Image   imgKeyLight;
  private Image   imgKeySelected;
  private KeyData resetKey;
  private int[]   kbMatrix;
  private int     curIdx;
  private int     curX;
  private int     curY;


  public SC2KeyboardFld( SC2 sc2 )
  {
    super( sc2, 15, true );
    this.kbMatrix       = new int[ 4 ];
    this.colorBg        = new Color( 20, 20, 20 );
    this.colorLEDon     = Color.RED;
    this.colorLEDoff    = new Color( 80, 50, 50 );
    this.colorTextDark  = new Color( 20, 20, 20 );
    this.colorTextLight = new Color( 180, 180, 180 );
    this.fontTitle      = new Font( Font.SANS_SERIF, Font.BOLD, 16 );
    this.fontLabel      = new Font( Font.SANS_SERIF, Font.BOLD, 12 );
    this.fontKey        = new Font( Font.SANS_SERIF, Font.BOLD, 16 );
    this.imgLedFld      = getImage( "/images/keyboard/sc2/led_field.png" );
    this.imgKeyDark     = getImage( "/images/keyboard/sc2/key_dark.png" );
    this.imgKeyLight    = getImage( "/images/keyboard/sc2/key_light.png" );
    this.imgKeySelected = getImage( "/images/keyboard/sc2/key_selected.png" );

    this.curIdx   = 0;
    this.curX     = MARGIN + (KEY_SIZE / 2);
    this.curY     = KEY_Y0;
    this.resetKey = addKey( this.imgKeyDark, "R", null, -1, -1, "Esc" );
    addKey( this.imgKeyDark, "K", null, 3, 0x10, "K oder +" );
    addKey( this.imgKeyDark, "W", null, 3, 0x20, "W" );
    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( this.imgKeyDark, "P", null, 3, 0x80, "P" );
    addKey( this.imgKeyDark, "T", null, 0, 0x20, "T" );
    addKey( this.imgKeyDark, "L", null, 0, 0x40, "L oder Backspace" );
    addKey( this.imgKeyDark, "Q", null, 0, 0x80, "Q oder Enter" );
    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( this.imgKeyLight, "A", "1", 1, 0x10, "A oder 1" );
    addKey( this.imgKeyLight, "B", "2", 1, 0x20, "B oder 2" );
    addKey( this.imgKeyLight, "C", "3", 1, 0x40, "C oder 3" );
    addKey( this.imgKeyLight, "D", "4", 1, 0x80, "D oder 4" );
    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addKey( this.imgKeyLight, "E", "5", 2, 0x10, "E oder 5" );
    addKey( this.imgKeyLight, "F", "6", 2, 0x20, "F oder 6" );
    addKey( this.imgKeyLight, "G", "7", 2, 0x40, "G oder 7" );
    addKey( this.imgKeyLight, "H", "8", 2, 0x80, "E oder 8" );
    setPreferredSize(
		new Dimension(
			(2 * MARGIN) + (4 * KEY_SIZE),
			KEY_Y0 + (4 * KEY_SIZE) + MARGIN ) );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof SC2;
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
	fireResetAfterDelay();
      }
      super.mousePressed( e );
    }
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    g.setColor( this.colorBg );
    g.fillRect( 0, 0, getWidth(), KEY_Y0 - MARGIN );
    g.setColor( Color.BLACK );
    g.fillRect(
	0,
	KEY_Y0 - MARGIN,
	getWidth(),
	getHeight() - KEY_Y0 + MARGIN );
    if( this.imgLedFld != null ) {
      g.drawImage( this.imgLedFld, MARGIN, KEY_Y0, this );
      g.drawImage(
		this.imgLedFld,
		MARGIN + (3 * KEY_SIZE) + (KEY_SIZE / 2),
		KEY_Y0,
		this );
    }
    g.setFont( this.fontTitle );
    g.setColor( this.colorTextLight );

    int y = MARGIN + this.fontTitle.getSize();
    g.drawString( "SCHACHCOMPUTER", MARGIN, y );

    FontMetrics fm = g.getFontMetrics();
    if( fm != null ) {
      String text = "SC2";
      g.drawString( text, getWidth() - MARGIN - fm.stringWidth( text ), y );
    }

    g.setFont( this.fontLabel );
    y += this.fontLabel.getSize();
    y += 5;
    g.drawString( "SCHACH", MARGIN, y );

    fm = g.getFontMetrics();
    if( fm != null ) {
      String text = "MATT";
      g.drawString( text, getWidth() - MARGIN - fm.stringWidth( text ), y );
    }

    g.setColor( this.emuSys.getLEDChessValue() ?
				this.colorLEDon : this.colorLEDoff );
    g.fillOval(
	MARGIN + (((KEY_SIZE / 2) - LED_SIZE) / 2),
	KEY_Y0 + ((KEY_SIZE - LED_SIZE) / 2),
	LED_SIZE,
	LED_SIZE );

    g.setColor( this.emuSys.getLEDMateValue() ?
				this.colorLEDon : this.colorLEDoff );
    g.fillOval(
	getWidth() - MARGIN - (KEY_SIZE / 2)
			+ (((KEY_SIZE / 2) - LED_SIZE) / 2),
	KEY_Y0 + ((KEY_SIZE - LED_SIZE) / 2),
	LED_SIZE,
	LED_SIZE );

    g.setFont( this.fontKey );
    fm = g.getFontMetrics();

    int xOffs = (KEY_SIZE / 2) - 6;
    for( KeyData key : this.keys ) {
      Image img = null;
      if( isKeySelected( key ) ) {
	img = this.imgKeySelected;
      } else {
	img = key.image;
      }
      if( img != null ) {
	g.drawImage( img, key.x, key.y, this );
      }
      if( key.text1 != null ) {
	if( fm != null ) {
	  xOffs = (KEY_SIZE - fm.stringWidth( key.text1 )) / 2;
	}
	if( key.text2 != null ) {
	  g.setColor( this.colorTextDark );
	  g.drawString(
		key.text1,
		key.x + xOffs,
		key.y + (KEY_SIZE / 2) - 2 );
	  if( fm != null ) {
	    xOffs = (KEY_SIZE - fm.stringWidth( key.text2 )) / 2;
	  }
	  g.drawString(
		key.text2,
		key.x + (KEY_SIZE / 2) - 5,
		key.y + (KEY_SIZE / 2) + 15 );
	} else {
	  g.setColor( this.colorTextLight );
	  g.drawString(
		key.text1,
		key.x + (KEY_SIZE / 2) - 6,
		key.y + (KEY_SIZE / 2) + 6 );
	}
      }
    }
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( emuSys instanceof SC2 ) {
      this.emuSys = (SC2) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != SC2" );
    }
  }


	/* --- private Methoden --- */

  private KeyData addKey(
			Image  image,
			String upperText,
			String lowerText,
			int    col,
			int    value,
			String toolTipText )
  {
    KeyData keyData = new KeyData(
				this.curX,
				this.curY,
				KEY_SIZE,
				KEY_SIZE,
				upperText,
				lowerText,
				null,
				null,
				image,
				col,
				value,
				false,
				toolTipText );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += KEY_SIZE;
    return keyData;
  }
}
