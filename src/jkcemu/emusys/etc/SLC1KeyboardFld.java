/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die SLC1-Tastatur
 */

package jkcemu.emusys.etc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.lang.*;
import java.util.Arrays;
import jkcemu.base.AbstractKeyboardFld;
import jkcemu.base.EmuSys;
import jkcemu.emusys.SLC1;


public class SLC1KeyboardFld extends AbstractKeyboardFld<SLC1>
{
  private static final int MARGIN         = 10;
  private static final int FONT_SIZE_MAIN = 18;
  private static final int FONT_SIZE_SUB  = 12;
  private static final int KEY_DIST       = 70;
  private static final int KEY_SIZE       = 60;

  private Font  fontMain;
  private Font  fontSub;
  private int[] kbMatrix;
  private int   curIdx;
  private int   curX;
  private int   curY;


  public SLC1KeyboardFld( SLC1 slc1 )
  {
    super( slc1, 12, true );
    this.fontMain = new Font( "SansSerif", Font.BOLD, FONT_SIZE_MAIN );
    this.fontSub  = new Font( "SansSerif", Font.PLAIN, FONT_SIZE_SUB );
    this.kbMatrix = new int[ 3 ];
    this.curIdx   = 0;
    this.curX     = MARGIN;
    this.curY     = MARGIN;
    addKey( "C#Seq",     null, "#BG", 2, 0x10, "Esc#S" );
    addKey( "A#\u00B11", null, "#SS", 2, 0x20, "F1#+ oder -" );
    addKey( "St#Fu",     null, "#DP", 2, 0x40, "S#F2" );
    addKey( "Z#Adr",     null, "#BP", 2, 0x80, "Z#F1" );

    this.curX = MARGIN;
    this.curY += KEY_DIST;
    addKey( "H#7", "8#F", "-#GO",           1, 0x80, "H oder 8#7 oder F" );
    addKey( "G#6", "7#E", "-#BL",           1, 0x40, "G oder 7#6 oder E" );
    addKey( "F#5", "6#D", "K\u00F6nig#DEL", 1, 0x20, "F oder 6#5 oder D" );
    addKey( "E#4", "5#C", "Dame#INS",       1, 0x10, "E oder 5#4 oder C" );

    this.curX = MARGIN;
    this.curY += KEY_DIST;
    addKey( "D#3", "4#B", "Turm#",        0, 0x10, "D oder 4#3 oder B" );
    addKey( "C#2", "3#A", "L\u00E4ufer#", 0, 0x20, "C oder 3#2 oder A" );
    addKey( "B#1", "2#9", "Springer#",    0, 0x40, "B oder 2#1 oder 9" );
    addKey( "A#0", "1#8", "Bauer#",       0, 0x80, "A oder 1#0 oder 8" );

    setPreferredSize(
	new Dimension(
		(2 * MARGIN) + (3 * KEY_DIST) + KEY_SIZE,
		(2 * MARGIN) + (2 * KEY_DIST) + KEY_SIZE ) );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accepts( EmuSys emuSys )
  {
    return emuSys instanceof SLC1;
  }


  @Override
  public String getToolTipText( MouseEvent e )
  {
    String rv = super.getToolTipText( e );
    if( rv != null ) {
      rv = getText( rv, this.emuSys.isChessMode() );
      if( rv != null ) {
	if( rv.isEmpty() ) {
	  rv = null;
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
  protected void paintComponent( Graphics g )
  {
    boolean chessMode = this.emuSys.isChessMode();
    g.setPaintMode();
    g.setColor( Color.lightGray );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    g.setColor( Color.black );

    for( KeyData key : this.keys ) {
      boolean selected = isKeySelected( key );
      if( selected ) {
	g.setColor( Color.gray );
	g.fillRect( key.x + 1, key.y + 1, key.w - 1, key.h - 1 );
      }
      g.setColor( Color.lightGray );
      g.draw3DRect( key.x + 1, key.y + 1, key.w - 1, key.h - 1, !selected );
      g.setColor( Color.black );
      String subText = getText( key.text3, chessMode );
      if( subText != null ) {
	if( subText.isEmpty() ) {
	  subText = null;
	}
      }
      if( key.text1 != null ) {
	int y = key.y + FONT_SIZE_MAIN + 8;
	g.setFont( this.fontMain );
	if( key.text2 != null ) {
	  drawLeft( g, key.x, y, getText( key.text1, chessMode ) );
	  drawRight( g, key.x, y, getText( key.text2, chessMode ) );
	} else {
	  if( subText == null ) {
	    y = key.y + ((key.h - FONT_SIZE_MAIN) / 2) + FONT_SIZE_MAIN - 2;
	  }
	  drawCentered( g, key.x, y, getText( key.text1, chessMode ) );
	}
      }
      if( subText != null ) {
	g.setFont( this.fontSub );
	drawCentered( g, key.x, key.y + key.h - 6, subText );
      }
    }
  }


  @Override
  public void setEmuSys( EmuSys emuSys )
  {
    if( emuSys instanceof SLC1 ) {
      this.emuSys = (SLC1) emuSys;
    } else {
      throw new IllegalArgumentException( "EmuSys != SLC1" );
    }
  }


	/* --- private Methoden --- */

  private KeyData addKey(
			String text1,
			String text2,
			String text3,
			int    col,
			int    value,
			String toolTipText )
  {
    KeyData keyData = new KeyData(
				this.curX,
				this.curY,
				KEY_SIZE,
				KEY_SIZE,
				text1,
				text2,
				text3,
				null,
				null,
				col,
				value,
				false,
				toolTipText );
    this.keys[ this.curIdx++ ] = keyData;
    this.curX += KEY_DIST;
    return keyData;
  }


  private static void drawCentered( Graphics g, int x, int y, String s )
  {
    if( s != null ) {
      FontMetrics fm = g.getFontMetrics();
      if( fm != null ) {
	g.drawString( s, x + ((KEY_SIZE - fm.stringWidth( s )) / 2) + 2, y );
      }
    }
  }


  private static void drawLeft( Graphics g, int x, int y, String s )
  {
    if( s != null ) {
      g.drawString( s, x + 8, y );
    }
  }


  private static void drawRight( Graphics g, int x, int y, String s )
  {
    if( s != null ) {
      FontMetrics fm = g.getFontMetrics();
      if( fm != null ) {
	g.drawString( s, x + KEY_SIZE - fm.stringWidth( s )- 8 , y );
      }
    }
  }


  private static String getText( String text, boolean chessMode )
  {
    String rv = "";
    if( text != null ) {
      int pos = text.indexOf( '#' );
      if( pos >= 0 ) {
	if( chessMode ) {
	  rv = text.substring( 0, pos );
	} else {
	  if( (pos + 1) < text.length() ) {
	    rv = text.substring( pos + 1 );
	  }
	}
      } else {
	rv = text;
      }
    }
    return rv;
  }
}
