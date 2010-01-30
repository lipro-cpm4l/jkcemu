/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Darstellung des Bildschirminhaltes
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.lang.*;
import javax.swing.JComponent;


public class ScreenFld extends JComponent implements MouseMotionListener
{
  public static final int DEFAULT_MARGIN = 20;

  private ScreenFrm screenFrm;
  private Color     markXORColor;
  private EmuSys    emuSys;
  private Point     dragStart;
  private Point     dragEnd;
  private boolean   textSelected;
  private int       selectionCharX1;
  private int       selectionCharX2;
  private int       selectionCharY1;
  private int       selectionCharY2;
  private int       lastColorIdx;
  private int       screenScale;
  private int       margin;
  private int       xOffs;
  private int       yOffs;


  public ScreenFld( ScreenFrm screenFrm )
  {
    this.screenFrm       = screenFrm;
    this.markXORColor    = new Color( 192, 192, 0 );
    this.emuSys          = null;
    this.dragStart       = null;
    this.dragEnd         = null;
    this.textSelected    = false;
    this.selectionCharX1 = -1;
    this.selectionCharY1 = -1;
    this.selectionCharX2 = -1;
    this.selectionCharY2 = -1;
    this.lastColorIdx    = -1;
    this.screenScale     = 1;
    this.margin          = DEFAULT_MARGIN;
    this.xOffs           = 0;
    this.yOffs           = 0;
    addMouseMotionListener( this );
    updPreferredSize();
  }


  public void clearSelection()
  {
    this.dragStart       = null;
    this.dragEnd         = null;
    this.selectionCharX1 = -1;
    this.selectionCharY1 = -1;
    this.selectionCharX2 = -1;
    this.selectionCharY2 = -1;
    this.screenFrm.setScreenDirty( true );
  }


  public BufferedImage createBufferedImage()
  {
    BufferedImage img = null;

    int w = getWidth();
    int h = getHeight();
    if( (w > 0) && (h > 0) && (this.emuSys != null) ) {
      int nColors = this.emuSys.getColorCount();
      int value   = nColors - 1;
      int nBits   = 0;
      while( value > 0 ) {
	value >>= 1;
	nBits++;
      }
      byte[] r = new byte[ nColors ];
      byte[] g = new byte[ nColors ];
      byte[] b = new byte[ nColors ];
      for( int i = 0; i < nColors; i++ ) {
	Color color = this.emuSys.getColor( i );
	r[ i ] = (byte) color.getRed();
	g[ i ] = (byte) color.getGreen();
	b[ i ] = (byte) color.getBlue();
      }
      IndexColorModel cm = new IndexColorModel( nBits, nColors, r, g, b );
      img = new BufferedImage( w, h, BufferedImage.TYPE_BYTE_INDEXED, cm );
      Graphics graphics = img.createGraphics();
      paint( graphics, w, h, false );
      graphics.dispose();
    }
    return img;
  }


  public EmuSys getEmuSys()
  {
    return this.emuSys;
  }


  public int getMargin()
  {
    return this.margin;
  }


  public int getScreenScale()
  {
    return this.screenScale;
  }


  public String getSelectedText()
  {
    String screenText = null;
    if( this.emuSys != null ) {
      screenText = this.emuSys.getScreenText(
					this.selectionCharX1,
					this.selectionCharY1,
					this.selectionCharX2,
					this.selectionCharY2 );
    }
    return (this.selectionCharX1 >= 0)
		&& (this.selectionCharY1 >= 0)
		&& (this.selectionCharX2 >= 0)
		&& (this.selectionCharY2 >= 0) ?  screenText : null;
  }


  public void setEmuSys( EmuSys emuSys )
  {
    this.emuSys = emuSys;
    clearSelection();
    updPreferredSize();
  }


  public void setMargin( int margin )
  {
    this.margin = margin;
    updPreferredSize();
  }


  public void setScreenScale( int screenScale )
  {
    if( (screenScale > 0) && (screenScale != this.screenScale) ) {
      this.screenScale = screenScale;
      updPreferredSize();
    }
  }


  public void updPreferredSize()
  {
    clearSelection();
    if( this.emuSys != null ) {
      int margin = this.margin;
      if( margin < 0 ) {
	margin = 0;
      }
      setPreferredSize( new Dimension(
	(2 * margin) + (this.emuSys.getScreenWidth() * this.screenScale),
	(2 * margin) + (this.emuSys.getScreenHeight() * this.screenScale) ) );
    }
    Container parent = getParent();
    if( parent != null ) {
      parent.invalidate();
    } else {
      invalidate();
    }
  }


	/* --- MouseMotionListener --- */

  public void mouseDragged( MouseEvent e )
  {
    if( (e.getComponent() == this) && (this.emuSys != null) ) {
      if( this.emuSys.canExtractScreenText()
	  && (this.emuSys.getCharColCount() > 0)
	  && (this.emuSys.getCharRowCount() > 0)
	  && (this.emuSys.getCharRowHeight() > 0)
	  && (this.emuSys.getCharHeight() > 0)
	  && (this.emuSys.getCharWidth() > 0) )
      {
	if( this.dragStart == null ) {
	  this.dragStart = new Point( e.getX(), e.getY() );
	  this.dragEnd   = null;
	} else {
	  this.dragEnd = new Point( e.getX(), e.getY() );
	}
	this.screenFrm.setScreenDirty( true );
      }
      e.consume();
    }
  }


  public void mouseMoved( MouseEvent e )
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  public void paint( Graphics g )
  {
    this.screenFrm.setScreenDirty( false );
    paint( g, getWidth(), getHeight(), true );
  }


  /*
   * update(...) wird ueberschrieben,
   * da paint(...) die Komponente vollstaendig fuellt
   * und somit das standardmaessige Fuellen mit der Hintegrundfarbe
   * entfallen kann.
   */
  public void update( Graphics g )
  {
    paint( g );
  }


	/* --- private Methoden --- */

  private void paint( Graphics g, int w, int h, boolean withMarking )
  {
    boolean textSelected = false;
    if( (w > 0) && (h > 0) && (this.emuSys != null) ) {

      // Hintergrund
      int bgColorIdx = this.emuSys.getBorderColorIndex();
      g.setColor( this.emuSys.getColor( bgColorIdx ) );
      g.fillRect( 0, 0, w, h );

      // Vordergrund zentrieren
      int wBase = this.emuSys.getScreenWidth();
      int hBase = this.emuSys.getScreenHeight();

      this.xOffs = (w - (wBase * this.screenScale)) / 2;
      if( this.xOffs < 0) {
	this.xOffs = 0;
      }
      this.yOffs = (h - (hBase * this.screenScale)) / 2;
      if( this.yOffs < 0) {
	this.yOffs = 0;
      }

      if( !this.emuSys.paintScreen(
				g,
				this.xOffs,
				this.yOffs,
				this.screenScale ) )
      {
	if( (this.xOffs > 0) || (this.yOffs > 0) ) {
	  g.translate( this.xOffs, this.yOffs );
	}

	/*
	 * Aus Gruenden der Performance werden untereinander liegende
	 * Punkte zusammengefasst und als Linie gezeichnet.
	 */
	for( int x = 0; x < wBase; x++ ) {
	  int lastColorIdx = -1;
	  int yColorBeg    = -1;
	  for( int y = 0; y < hBase; y++ ) {
	    int curColorIdx = this.emuSys.getColorIndex( x, y );
	    if( curColorIdx != lastColorIdx ) {
	      if( (lastColorIdx >= 0)
		  && (lastColorIdx != bgColorIdx)
		  && (yColorBeg >= 0) )
	      {
		g.setColor( this.emuSys.getColor( lastColorIdx ) );
		g.fillRect(
			x * this.screenScale,
			yColorBeg * this.screenScale,
			this.screenScale,
			(y - yColorBeg) * this.screenScale );
	      }
	      yColorBeg    = y;
	      lastColorIdx = curColorIdx;
	    }
	  }
	  if( (lastColorIdx >= 0)
	      && (lastColorIdx != bgColorIdx)
	      && (yColorBeg >= 0) )
	  {
	    g.setColor( this.emuSys.getColor( lastColorIdx ) );
	    g.fillRect(
		x * this.screenScale,
		yColorBeg * this.screenScale,
		this.screenScale,
		(hBase - yColorBeg) * this.screenScale );
	  }
	}
	if( (this.xOffs > 0) || (this.yOffs > 0) ) {
	  g.translate( -this.xOffs, -this.yOffs );
	}
      }

      // Markierter Text
      if( withMarking
	  && (this.screenScale > 0)
	  && (this.dragStart != null)
	  && (this.dragEnd != null) )
      {
	int nCols = this.emuSys.getCharColCount();
	int nRows = this.emuSys.getCharRowCount();
	int hRow  = this.emuSys.getCharRowHeight();
	int hChar = this.emuSys.getCharHeight();
	int wChar = this.emuSys.getCharWidth();
	if( (nCols > 0) && (nRows > 0)
	    && (hRow > 0) && (hChar > 0) && (wChar > 0) )
	{
	  int x1 = this.dragStart.x;
	  int y1 = this.dragStart.y;
	  int x2 = this.dragEnd.x;
	  int y2 = this.dragEnd.y;

	  // Zeichenpositionen berechnen
	  this.selectionCharX1 = Math.max(
			(x1 - this.xOffs) / this.screenScale, 0 ) / wChar;
	  this.selectionCharY1 = Math.max(
			(y1 - this.yOffs) / this.screenScale, 0 ) / hRow;
	  this.selectionCharX2 = Math.max(
			(x2 - this.xOffs) / this.screenScale, 0 ) / wChar;
	  this.selectionCharY2 = Math.max(
			(y2 - this.yOffs) / this.screenScale , 0 ) / hRow;
	  if( this.selectionCharX1 >= nCols ) {
	    this.selectionCharX1 = nCols - 1;
	  }
	  if( this.selectionCharY1 >= nRows ) {
	    this.selectionCharY1 = nRows - 1;
	  }
	  if( this.selectionCharX2 >= nCols ) {
	    this.selectionCharX2 = nCols - 1;
	  }
	  if( this.selectionCharY2 >= nRows ) {
	    this.selectionCharY2 = nRows - 1;
	  }

	  // Koordinaten tauschen, wenn Endpunkt vor Startpunkt liegt
	  if( (this.selectionCharY1 > this.selectionCharY2)
	      || ((this.selectionCharY1 == this.selectionCharY2)
		  && (this.selectionCharX1 > this.selectionCharX2)) )
	  {
	    int m = this.selectionCharX1;
	    this.selectionCharX1 = this.selectionCharX2;
	    this.selectionCharX2 = m;

	    m = this.selectionCharY1;
	    this.selectionCharY1 = this.selectionCharY2;
	    this.selectionCharY2 = m;

	    m  = x1;
	    x1 = x2;
	    x2 = m;

	    m  = y1;
	    y1 = y2;
	    y2 = m;
	  }

	  /*
	   * Koordinaten anpassen,
	   * wenn Endpunkt ausserhalb der Bildschirmausgabe liegt
	   */
	  if( y1 < this.yOffs ) {
	    this.selectionCharX1 = 0;
	    this.selectionCharY1 = 0;
	  } else {
	    if( x1 > (this.xOffs + (this.screenScale * nCols * wChar)) ) {
	      this.selectionCharX1 = 0;
	      this.selectionCharY1++;
	    }
	  }
	  if( y2 > (this.yOffs + (this.screenScale
					* (((nRows - 1) * hRow) + hChar))) )
	  {
	    this.selectionCharX2 = nCols - 1;
	    this.selectionCharY2 = nRows - 1;
	  } else {
	    if( x2 < this.xOffs ) {
	      this.selectionCharX2 = nCols - 1;
	      --this.selectionCharY2;
	    }
	  }

	  // Markierter Text visualisieren
	  g.setColor( Color.white );
	  g.setXORMode( this.markXORColor );
	  if( this.selectionCharY1 == this.selectionCharY2 ) {
	    g.fillRect(
		this.xOffs + (this.screenScale * this.selectionCharX1 * wChar),
		this.yOffs + (this.screenScale * this.selectionCharY1 * hRow),
		this.screenScale * (this.selectionCharX2
					- this.selectionCharX1 + 1) * wChar,
		this.screenScale * hChar );
	  } else {
	    g.fillRect(
		this.xOffs + (this.screenScale * this.selectionCharX1 * wChar),
		this.yOffs + (this.screenScale * this.selectionCharY1 * hRow),
		this.screenScale * (nCols - this.selectionCharX1) * wChar,
		this.screenScale * hRow );
	    if( this.selectionCharY1 + 1 < this.selectionCharY2 ) {
	      g.fillRect(
		this.xOffs,
		this.yOffs + (this.screenScale * (this.selectionCharY1 + 1)
								* hRow),
		this.screenScale * nCols * wChar,
		this.screenScale * (this.selectionCharY2
					- this.selectionCharY1 - 1) * hRow );
	    }
	    g.fillRect(
		this.xOffs,
		this.yOffs + (this.screenScale * this.selectionCharY2 * hRow),
		this.screenScale * (this.selectionCharX2 + 1) * wChar,
		this.screenScale * hChar );
	  }
	  textSelected = true;
	}
      }
    }
    if( withMarking && (textSelected != this.textSelected) ) {
      this.textSelected = textSelected;
      this.screenFrm.setScreenTextSelected( textSelected );
    }
  }
}
