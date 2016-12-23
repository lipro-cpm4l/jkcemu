/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer ein Schachbrett
 */

package jkcemu.etc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.lang.*;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;


public class ChessboardFld extends JComponent
{
  private static final int MARGIN          = 20;
  private static final int SQUARE_WIDTH    = 40;
  private static final int PREFERRED_WIDTH = (2 * MARGIN) + (8 * SQUARE_WIDTH);

  private static Map<EmuSys.Chessman,Image> imgMap = null;

  private EmuThread emuThread;
  private Color     colorWhiteSquare;
  private Color     colorBlackSquare;
  private boolean   swapped;


  public ChessboardFld( EmuThread emuThread )
  {
    this.emuThread        = emuThread;
    this.colorWhiteSquare = new Color( 255, 200, 200 );
    this.colorBlackSquare = new Color( 200, 150, 0 );
    this.swapped          = false;
    if( imgMap == null ) {
      imgMap = new HashMap<>();
      Toolkit tk = getToolkit();
      if( tk != null ) {
	addImage( imgMap, tk, EmuSys.Chessman.WHITE_PAWN,   "pawn_w.png" );
	addImage( imgMap, tk, EmuSys.Chessman.WHITE_KNIGHT, "knight_w.png" );
	addImage( imgMap, tk, EmuSys.Chessman.WHITE_BISHOP, "bishop_w.png" );
	addImage( imgMap, tk, EmuSys.Chessman.WHITE_ROOK,   "rook_w.png" );
	addImage( imgMap, tk, EmuSys.Chessman.WHITE_QUEEN,  "queen_w.png" );
	addImage( imgMap, tk, EmuSys.Chessman.WHITE_KING,   "king_w.png" );
	addImage( imgMap, tk, EmuSys.Chessman.BLACK_PAWN,   "pawn_b.png" );
	addImage( imgMap, tk, EmuSys.Chessman.BLACK_KNIGHT, "knight_b.png" );
	addImage( imgMap, tk, EmuSys.Chessman.BLACK_BISHOP, "bishop_b.png" );
	addImage( imgMap, tk, EmuSys.Chessman.BLACK_ROOK,   "rook_b.png" );
	addImage( imgMap, tk, EmuSys.Chessman.BLACK_QUEEN,  "queen_b.png" );
	addImage( imgMap, tk, EmuSys.Chessman.BLACK_KING,   "king_b.png" );

	// Bilder laden, damit die Bildgroesse bekannt ist
	Collection<Image> c = imgMap.values();
	if( c != null ) {
	  if( !c.isEmpty() ) {
	    MediaTracker mt = new MediaTracker( this );
	    int          i  = 0;
	    for( Image img : c ) {
	      mt.addImage( img, i++ );
	    }
	    try {
	      mt.waitForAll();
	    }
	    catch( InterruptedException ex ) {}
	  }
	}
      }
    }
    setFont( new Font( "SansSerif", Font.PLAIN, 12 ) );
  }


  public BufferedImage createImage()
  {
    BufferedImage img = new BufferedImage(
				PREFERRED_WIDTH,
				PREFERRED_WIDTH,
				BufferedImage.TYPE_INT_RGB );
    Graphics g = img.createGraphics();
    if( g != null ) {
      drawChessboard( g, 0, 0 );
      g.dispose();
    }
    return img;
  }


  public void swap()
  {
    this.swapped = !this.swapped;
    repaint();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Dimension getPreferredSize()
  {
    return new Dimension( PREFERRED_WIDTH, PREFERRED_WIDTH );
  }


  @Override
  public void paintComponent( Graphics g )
  {
    int x = Math.min( (getWidth() - PREFERRED_WIDTH) / 2, 0 );
    int y = Math.min( (getHeight() - PREFERRED_WIDTH) / 2, 0 );
    if( x < 0 ) {
      x = 0;
    }
    if( y < 0 ) {
      y = 0;
    }
    drawChessboard( g, x, y );
  }


	/* --- private Methoden --- */

  private static void addImage(
			Map<EmuSys.Chessman,Image> dstMap,
			Toolkit                    tk,
			EmuSys.Chessman            chessman,
			String                     fileName )
  {
    URL url = ChessboardFld.class.getResource( "/images/chess/" + fileName );
    if( url != null ) {
      Image img = tk.createImage( url );
      if( img != null ) {
	dstMap.put( chessman, img );
      }
    }
  }


  private void drawChessboard( Graphics g, int x, int y )
  {
    g.setColor( Color.white );
    g.fillRect( x, y, PREFERRED_WIDTH, PREFERRED_WIDTH );
    g.setColor( Color.black );
    g.drawRect( x, y, PREFERRED_WIDTH, PREFERRED_WIDTH );
    g.setFont( getFont() );
    for( int i = 0; i < 8; i++ ) {
      String s = Character.toString(
			(char) (this.swapped ? ('1' + i) : ('8' - i)) );
      int    tmpY = y + MARGIN + (i * SQUARE_WIDTH) + (SQUARE_WIDTH / 2) + 4;
      g.setColor( Color.black );
      g.drawString( s, x + 6, tmpY );
      g.drawString( s, x + MARGIN + (8 * SQUARE_WIDTH) + 6, tmpY );
      for( int k = 0; k < 8; k++ ) {
	g.setColor( ((i ^ k) & 0x01) == 0 ?
				this.colorWhiteSquare
				: this.colorBlackSquare );
	g.fillRect(
		x + MARGIN + (k * SQUARE_WIDTH),
		y + MARGIN + (i * SQUARE_WIDTH),
		SQUARE_WIDTH,
		SQUARE_WIDTH );
      }
    }
    FontMetrics fm = g.getFontMetrics();
    g.setColor( Color.black );
    for( int k = 0; k < 8; k++ ) {
      String s = Character.toString(
			(char) (this.swapped ? ('H' - k) : ('A' + k)) );
      int    w = (fm != null ? fm.stringWidth( s ) : 8);
      g.drawString(
		s,
		x + MARGIN + (k * SQUARE_WIDTH) + ((SQUARE_WIDTH - w) / 2),
		y + 16 );
      g.drawString(
		s,
		x + MARGIN + (k * SQUARE_WIDTH) + ((SQUARE_WIDTH - w) / 2),
		y + MARGIN + (8 * SQUARE_WIDTH) + 14 );
    }
    if( imgMap != null ) {
      EmuSys emuSys = this.emuThread.getEmuSys();
      if( emuSys != null ) {
	if( emuSys.supportsChessboard() ) {
	  for( int i = 0; i < 8; i++ ) {
	    for( int k = 0; k < 8; k++ ) {
	      EmuSys.Chessman chessman = emuSys.getChessman( i, k );
	      if( chessman != null ) {
		Image img = imgMap.get( chessman );
		if( img != null ) {
		  int w = img.getWidth( this );
		  int h = img.getHeight( this );
		  if( (w > 0) && (h > 0) ) {
		    int row = i;
		    int col = k;
		    if( this.swapped ) {
		      row = 7 - i;
		      col = 7 - k;
		    }
		    g.drawImage(
			img,
			x + MARGIN + (col * SQUARE_WIDTH)
						+ ((SQUARE_WIDTH - w) / 2),
			y + MARGIN + ((7 - row) * SQUARE_WIDTH)
						+ ((SQUARE_WIDTH - h) / 2),
			this );
		  }
		}
	      }
	    }
	  }
	}
      }
    }
  }
}
