/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Tastatur des Poly-Computers 880
 */

package jkcemu.emusys.poly880;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.lang.*;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.emusys.Poly880;


public class Poly880KeyboardFld extends AbstractKeyboardFld<Poly880>
{
  private static final int MARGIN   = 10;
  private static final int KEY_SIZE = 60;

  private static final String[] notEmulatedMsg = {
		"Der Poly-Computer 880 bietet einen",
		"Schrittbetrieb auf Maschinenzyklusebene.",
		"Dieser wird von JKCEMU aber nicht emuliert.",
		"Aus diesem Grund haben die Tasten",
		"M/CYCL und CYCL keine Wirkung." };

  private Color   colorBg;
  private Font    fontFct;
  private Font    fontHex;
  private Font    fontReg;
  private Font    fontInfo;
  private Image   imgKeyRed;
  private Image   imgKeyGreen;
  private Image   imgKeyOrange;
  private Image   imgKeyWhite;
  private Image   imgKeySelected;
  private KeyData resetKey;
  private KeyData cyclKey;
  private KeyData mCyclKey;
  private KeyData monKey;
  private int[]   kbMatrix;
  private int     curIdx;
  private int     curX;
  private int     curY;


  public Poly880KeyboardFld( Poly880 poly880 )
  {
    super( poly880, 27, true );
    this.kbMatrix     = new int[ 8 ];
    this.colorBg      = new Color( 20, 20, 20 );
    this.fontInfo     = new Font( "SansSerif", Font.PLAIN, 18 );
    this.fontFct      = new Font( "SansSerif", Font.PLAIN, 16 );
    this.fontHex      = new Font( "SansSerif", Font.BOLD, 24 );
    this.fontReg      = new Font( "SansSerif", Font.PLAIN, 12 );
    this.imgKeyRed    = getImage( "/images/keyboard/poly880/key_red.png" );
    this.imgKeyGreen  = getImage( "/images/keyboard/poly880/key_green.png" );
    this.imgKeyOrange = getImage( "/images/keyboard/poly880/key_orange.png" );
    this.imgKeyWhite  = getImage( "/images/keyboard/poly880/key_white.png" );
    this.imgKeySelected = getImage(
				"/images/keyboard/poly880/key_selected.png" );

    this.curIdx   = 0;
    this.curX     = MARGIN;
    this.curY     = MARGIN;
    addHexKey( "C", "PC", 4, 0x80 );
    addHexKey( "D", null, 5, 0x80 );
    addHexKey( "E", null, 7, 0x80 );
    addHexKey( "F", null, 6, 0x80 );
    this.curX += (KEY_SIZE / 2);
    addWhiteKey( "GO", 0, 0x10, "G" );
    addWhiteKey( "MEM", 7, 0x10, "M" );
    this.curX += (KEY_SIZE / 2);
    this.resetKey = addKey( this.imgKeyRed, "RES", null, null, -1, -1, "Esc" );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addHexKey( "8", "IE", 4, 0x20 );
    addHexKey( "9", "IX", 5, 0x20 );
    addHexKey( "A", "IY", 7, 0x20 );
    addHexKey( "B", "SP", 6, 0x20 );
    this.curX += (KEY_SIZE / 2);
    addWhiteKey( "STEP", 6, 0x10, "S" );
    addWhiteKey( "REG", 4, 0x10, "R" );
    this.curX += (KEY_SIZE / 2);
    this.monKey = addKey(
			this.imgKeyOrange,
			"MON",
			null,
			null,
			-1,
			-1,
			"F2" );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addHexKey( "4", "AF\'/MI", 0, 0x80 );
    addHexKey( "5", "BC\'/MO", 3, 0x80 );
    addHexKey( "6", "DE\'", 1, 0x80 );
    addHexKey( "7", "HL\'", 2, 0x80 );
    this.curX += (KEY_SIZE / 2);
    addWhiteKey( "FCT", 5, 0x10, "F1" );
    addWhiteKey( "BACK", 3, 0x10, "- oder Backspace" );
    this.curX += (KEY_SIZE / 2);
    this.mCyclKey = addKey(
			this.imgKeyGreen,
			"M\nCYCL",
			null,
			null,
			-1,
			-1,
			null );

    this.curX = MARGIN;
    this.curY += KEY_SIZE;
    addHexKey( "0", "AF/PI", 0, 0x20 );
    addHexKey( "1", "BC/PO", 3, 0x20 );
    addHexKey( "2", "DE/ME", 1, 0x20 );
    addHexKey( "3", "HL/FL", 2, 0x20 );
    this.curX += KEY_SIZE;
    addKey( this.imgKeyRed, "EXEC", null, null, 2, 0x10, "X oder Enter" );
    this.curX += KEY_SIZE;
    this.cyclKey = addKey(
			this.imgKeyGreen,
			"CYCL",
			null,
			null,
			-1,
			-1,
			null );

    setPreferredSize(
		new Dimension(
			(2 * MARGIN) + (8 * KEY_SIZE),
			(2 * MARGIN) + (4 * KEY_SIZE) ) );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof Poly880;
  }


  @Override
  public String getKeyboardName()
  {
    return "Poly-Computer Tastatur";
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
      } else if( hits( this.monKey, e ) ) {
	this.emuSys.fireMonKey();
      }
      super.mousePressed( e );
    }
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    boolean info = false;
    g.setColor( this.colorBg );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    for( KeyData key : this.keys ) {
      Image image = null;
      if( isKeySelected( key ) ) {
	if( (key == this.mCyclKey) || (key == this.cyclKey) ) {
	  info  = true;
	  image = key.image;
	} else {
	  image = this.imgKeySelected;
	}
      } else {
	image = key.image;
      }
      if( image != null ) {
	g.drawImage( image, key.x, key.y, this );
      }
      g.setColor( Color.black );
      if( key.text1 != null ) {
	g.setFont( this.fontFct );
	FontMetrics fm = g.getFontMetrics();
	if( fm != null ) {
	  int eol = key.text1.indexOf( '\n' );
	  if( eol >= 0 ) {
	    String s = key.text1.substring( 0, eol );
	    g.drawString(
		s,
		key.x + ((key.w - fm.stringWidth( s )) / 2),
		key.y + (key.w / 2) - 4 );
	    s = key.text1.substring( eol + 1 );
	    g.drawString(
		s,
		key.x + ((key.w - fm.stringWidth( s )) / 2),
		key.y + (key.w / 2) + this.fontFct.getSize() );
	  } else {
	    int fh = this.fontFct.getSize();
	    g.drawString(
		key.text1,
		key.x + ((key.w - fm.stringWidth( key.text1 )) / 2),
		key.y + fh - 2 + ((KEY_SIZE - fh) / 2) );
	  }
	}
      }
      if( key.text2 != null ) {
	g.setFont( this.fontHex );
	FontMetrics fm = g.getFontMetrics();
	if( fm != null ) {
	  g.drawString(
		key.text2,
		key.x + ((key.w - fm.stringWidth( key.text2 )) / 2),
		key.y + 8 + this.fontHex.getSize() );
	}
      }
      if( key.text3 != null ) {
	g.setFont( this.fontReg );
	g.drawString(
		key.text3,
		key.x + 8,
		key.y + KEY_SIZE - 8 );
      }
    }
    if( info ) {
      g.setFont( this.fontInfo );
      FontMetrics fm = g.getFontMetrics();
      if( fm != null ) {
	int r = this.fontInfo.getSize() + 1;
	int h = notEmulatedMsg.length * r;
	int w = 0;
	for( String s : notEmulatedMsg ) {
	  int wLine = fm.stringWidth( s );
	  if( wLine > w ) {
	    w = wLine;
	  }
	}
	h += 20;
	w += 20;
	int x = (getWidth() - w) / 2;
	int y = (getHeight() - h) / 2;
	g.setColor( Color.yellow );
	g.fillRect( x, y, w, h );
	g.setColor( Color.black );
	x += 10;
	y += 8;
	y += r;
	for( String s : notEmulatedMsg ) {
	  g.drawString( s, x, y );
	  y += r;
	}
      }
    }
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( emuSys instanceof Poly880 ) {
      this.emuSys = (Poly880) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != Poly880" );
    }
  }


	/* --- private Methoden --- */

  private KeyData addKey(
			Image  image,
			String textFct,
			String textHex,
			String textReg,
			int    col,
			int    value,
			String toolTipText )
  {
    KeyData keyData = new KeyData(
				this.curX,
				this.curY,
				KEY_SIZE,
				KEY_SIZE,
				textFct,
				textHex,
				textReg,
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


  private void addHexKey( String textHex, String textReg, int col, int value )
  {
    addKey( this.imgKeyOrange, null, textHex, textReg, col, value, null );
  }


  private void addWhiteKey(
			String text,
			int    col,
			int    value,
			String toolTipText )
  {
    addKey( this.imgKeyWhite, text, null, null, col, value, toolTipText );
  }


  private void addWhiteKey( String text, int col, int value )
  {
    addKey( this.imgKeyWhite, text, null, null, col, value, null );
  }
}

