/*
 * (c) 2008-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Darstellung des Bildschirminhaltes
 */

package jkcemu.base;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import javax.swing.JComponent;


public class ScreenFld extends JComponent implements MouseMotionListener
{
  public static final String PROP_BRIGHTNESS    = "jkcemu.brightness";
  public static final int    DEFAULT_BRIGHTNESS = 80;
  public static final int    DEFAULT_MARGIN     = 20;

  private AbstractScreenFrm             screenFrm;
  private volatile AbstractScreenDevice screenDevice;
  private volatile CharRaster           charRaster;
  private Point                         dragStart;
  private Point                         dragEnd;
  private Color                         markXORColor;
  private boolean                       textSelected;
  private int                           selectionCharX1;
  private int                           selectionCharX2;
  private int                           selectionCharY1;
  private int                           selectionCharY2;
  private int                           scale;
  private int                           margin;


  public ScreenFld( AbstractScreenFrm screenFrm )
  {
    this.screenFrm       = screenFrm;
    this.screenDevice    = null;
    this.dragStart       = null;
    this.dragEnd         = null;
    this.markXORColor    = new Color( 192, 192, 0 );
    this.textSelected    = false;
    this.selectionCharX1 = -1;
    this.selectionCharY1 = -1;
    this.selectionCharX2 = -1;
    this.selectionCharY2 = -1;
    this.scale           = 1;
    this.margin          = DEFAULT_MARGIN;
    addMouseMotionListener( this );
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
    BufferedImage        img          = null;
    int                  w            = getWidth();
    int                  h            = getHeight();
    AbstractScreenDevice screenDevice = this.screenDevice;
    if( (screenDevice != null) && (w > 0) && (h > 0) ) {
      int nColors = screenDevice.getColorCount();
      if( (nColors > 0) && (nColors <= 256) ) {
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
	  Color color = screenDevice.getColor( i );
	  r[ i ] = (byte) color.getRed();
	  g[ i ] = (byte) color.getGreen();
	  b[ i ] = (byte) color.getBlue();
	}
	IndexColorModel cm = new IndexColorModel( nBits, nColors, r, g, b );
	img = new BufferedImage(
			w,
			h,
			nBits > 4 ?
				BufferedImage.TYPE_BYTE_INDEXED
				: BufferedImage.TYPE_BYTE_BINARY,
			cm );
      } else {
	img = new BufferedImage(
			w,
			h,
			BufferedImage.TYPE_3BYTE_BGR );
      }
      Graphics graphics = img.createGraphics();
      paint( graphics, w, h, false );
      graphics.dispose();
    }
    return img;
  }


  public int getMargin()
  {
    return this.margin;
  }


  public AbstractScreenDevice getScreenDevice()
  {
    return this.screenDevice;
  }


  public int getScreenScale()
  {
    return this.scale;
  }


  public String getSelectedText()
  {
    String               screenText   = null;
    AbstractScreenDevice screenDevice = this.screenDevice;
    CharRaster           charRaster   = this.charRaster;
    if( (screenDevice != null) && (charRaster != null) ) {
      screenText = screenDevice.getScreenText(
					charRaster,
					this.selectionCharX1,
					this.selectionCharY1,
					this.selectionCharX2,
					this.selectionCharY2 );
    }
    return (this.selectionCharX1 >= 0)
		&& (this.selectionCharY1 >= 0)
		&& (this.selectionCharX2 >= 0)
		&& (this.selectionCharY2 >= 0) ? screenText : null;
  }


  public void setMargin( int margin )
  {
    this.margin = margin;
    updPreferredSize();
  }


  public void setScreenDevice( AbstractScreenDevice screenDevice )
  {
    this.screenDevice = screenDevice;
    clearSelection();
    updPreferredSize();
  }


  public void setScreenScale( int scale )
  {
    if( (scale > 0) && (scale != this.scale) ) {
      this.scale = scale;
      updPreferredSize();
    }
  }


  public void updPreferredSize()
  {
    clearSelection();
    AbstractScreenDevice device = this.screenDevice;
    if( device != null ) {
      int margin = this.margin;
      if( margin < 0 ) {
	margin = 0;
      }
      setPreferredSize(
	new Dimension(
		(2 * margin) + (device.getScreenWidth() * this.scale),
		(2 * margin) + (device.getScreenHeight() * this.scale) ) );
    }
    Container parent = getParent();
    if( parent != null ) {
      parent.invalidate();
    } else {
      invalidate();
    }
  }


	/* --- MouseMotionListener --- */

  @Override
  public void mouseDragged( MouseEvent e )
  {
    AbstractScreenDevice screenDevice = this.screenDevice;
    if( (e.getComponent() == this) && (screenDevice != null) ) {
      if( this.dragStart == null ) {
	this.charRaster = screenDevice.getCurScreenCharRaster();
	if( this.charRaster != null ) {
	  this.dragStart = new Point( e.getX(), e.getY() );
	  this.dragEnd   = null;
	  this.screenFrm.setScreenDirty( true );
	}
      } else {
	if( this.charRaster != null ) {
	  this.dragEnd = new Point( e.getX(), e.getY() );
	} else {
	  this.dragEnd   = null;
	  this.dragStart = null;
	}
	this.screenFrm.setScreenDirty( true );
      }
      e.consume();
    }
  }


  @Override
  public void mouseMoved( MouseEvent e )
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
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
  @Override
  public void update( Graphics g )
  {
    paint( g );
  }


	/* --- private Methoden --- */

  private void paint( Graphics g, int w, int h, boolean withMarking )
  {
    boolean              textSelected = false;
    AbstractScreenDevice screenDevice = this.screenDevice;
    if( (w > 0) && (h > 0) && (screenDevice != null) ) {

      // Vordergrund zentrieren
      int wBase = screenDevice.getScreenWidth();
      int hBase = screenDevice.getScreenHeight();

      int xOffs = (w - (wBase * this.scale)) / 2;
      if( xOffs < 0) {
	xOffs = 0;
      }
      int yOffs = (h - (hBase * this.scale)) / 2;
      if( yOffs < 0) {
	yOffs = 0;
      }

      // Hintergrund zeichnen
      int bgColorIdx = screenDevice.getBorderColorIndex();
      if( screenDevice.supportsBorderColorByLine() ) {
	int scale = this.scale;
	if( scale > 1 ) {
	  int line = (-yOffs / scale);
	  for( int y = 0; y < h; y += scale ) {
	    bgColorIdx = screenDevice.getBorderColorIndexByLine( line++ );
	    g.setColor( screenDevice.getColor( bgColorIdx ) );
	    g.fillRect( 0, y, w, y + scale );
	  }
	} else {
	  int line = -yOffs;
	  for( int y = 0; y < h; y++ ) {
	    bgColorIdx = screenDevice.getBorderColorIndexByLine( line++ );
	    g.setColor( screenDevice.getColor( bgColorIdx ) );
	    g.drawLine( 0, y, w, y );
	  }
	}
	/*
	 * Da es keine einheitliche Hintergrundfarbe gibt,
	 * darf sie beim Zeichnen des Vordergrundes
	 * nicht beruecksichtigt werden und
	 * wird hier deshalb auf einen ungueltigen Wert gesetzt.
	 */
	bgColorIdx = -1;
      } else {
	g.setColor( screenDevice.getColor( bgColorIdx ) );
	g.fillRect( 0, 0, w, h );
      }

      // Vordergrund zeichnen
      if( !screenDevice.paintScreen(
				g,
				xOffs,
				yOffs,
				this.scale ) )
      {
	if( (xOffs > 0) || (yOffs > 0) ) {
	  g.translate( xOffs, yOffs );
	}

	/*
	 * Aus Gruenden der Performance werden untereinander liegende
	 * Punkte zusammengefasst und als Linie gezeichnet.
	 */
	for( int x = 0; x < wBase; x++ ) {
	  int lastColorIdx = -1;
	  int yColorBeg    = -1;
	  for( int y = 0; y < hBase; y++ ) {
	    int curColorIdx = screenDevice.getColorIndex( x, y );
	    if( curColorIdx != lastColorIdx ) {
	      if( (lastColorIdx >= 0)
		  && (lastColorIdx != bgColorIdx)
		  && (yColorBeg >= 0) )
	      {
		g.setColor( screenDevice.getColor( lastColorIdx ) );
		g.fillRect(
			x * this.scale,
			yColorBeg * this.scale,
			this.scale,
			(y - yColorBeg) * this.scale );
	      }
	      yColorBeg    = y;
	      lastColorIdx = curColorIdx;
	    }
	  }
	  if( (lastColorIdx >= 0)
	      && (lastColorIdx != bgColorIdx)
	      && (yColorBeg >= 0) )
	  {
	    g.setColor( screenDevice.getColor( lastColorIdx ) );
	    g.fillRect(
		x * this.scale,
		yColorBeg * this.scale,
		this.scale,
		(hBase - yColorBeg) * this.scale );
	  }
	}
	if( (xOffs > 0) || (yOffs > 0) ) {
	  g.translate( -xOffs, -yOffs );
	}
      }

      // Markierter Text
      if( withMarking ) {
	CharRaster charRaster = this.charRaster;
	Point      dragStart  = this.dragStart;
	Point      dragEnd    = this.dragEnd;
	int        scale      = this.scale;
	if( (charRaster != null)
	    && (dragStart != null)
	    && (dragEnd != null)
	    && (scale > 0) )
	{
	  int nCols = this.charRaster.getColCount();
	  int nRows = this.charRaster.getRowCount();
	  int hRow  = this.charRaster.getRowHeight();
	  int hChar = this.charRaster.getCharHeight();
	  int wChar = this.charRaster.getCharWidth();
	  if( (nCols > 0) && (nRows > 0)
	      && (hRow > 0) && (hChar > 0) && (wChar > 0) )
	  {
	    int x1 = dragStart.x;
	    int y1 = dragStart.y;
	    int x2 = dragEnd.x;
	    int y2 = dragEnd.y;
	    xOffs += (this.charRaster.getXOffset() * scale);
	    yOffs += (this.charRaster.getYOffset() * scale);

	    // Zeichenpositionen berechnen
	    this.selectionCharX1 = Math.max(
			(x1 - xOffs) / scale, 0 ) / wChar;
	    this.selectionCharY1 = Math.max(
			(y1 - yOffs) / scale, 0 ) / hRow;
	    this.selectionCharX2 = Math.max(
			(x2 - xOffs) / scale, 0 ) / wChar;
	    this.selectionCharY2 = Math.max(
			(y2 - yOffs) / scale, 0 ) / hRow;
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
	    if( y1 < yOffs ) {
	      this.selectionCharX1 = 0;
	      this.selectionCharY1 = 0;
	    } else {
	      if( x1 > (xOffs + (scale * nCols * wChar)) ) {
		this.selectionCharX1 = 0;
		this.selectionCharY1++;
	      }
	    }
	    if( y2 > (yOffs + (scale * (((nRows - 1) * hRow) + hChar))) ) {
	      this.selectionCharX2 = nCols - 1;
	      this.selectionCharY2 = nRows - 1;
	    } else {
	      if( x2 < xOffs ) {
		this.selectionCharX2 = nCols - 1;
		--this.selectionCharY2;
	      }
	    }

	    // Markierter Text visualisieren
	    g.setColor( Color.WHITE );
	    g.setXORMode( this.markXORColor );
	    if( this.selectionCharY1 == this.selectionCharY2 ) {
	      g.fillRect(
			xOffs + (scale * this.selectionCharX1 * wChar),
			yOffs + (scale * this.selectionCharY1 * hRow),
			scale * (this.selectionCharX2
					- this.selectionCharX1 + 1) * wChar,
			scale * hChar );
	    } else {
	      g.fillRect(
			xOffs + (scale * this.selectionCharX1 * wChar),
			yOffs + (scale * this.selectionCharY1 * hRow),
			scale * (nCols - this.selectionCharX1) * wChar,
			scale * hRow );
	      if( this.selectionCharY1 + 1 < this.selectionCharY2 ) {
		g.fillRect(
			xOffs,
			yOffs + (scale * (this.selectionCharY1 + 1) * hRow),
			scale * nCols * wChar,
			scale * (this.selectionCharY2
					- this.selectionCharY1 - 1) * hRow );
	      }
	      g.fillRect(
			xOffs,
			yOffs + (scale * this.selectionCharY2 * hRow),
			scale * (this.selectionCharX2 + 1) * wChar,
			scale * hChar );
	    }
	    textSelected = true;
	  }
	}
      }
    }
    if( withMarking && (textSelected != this.textSelected) ) {
      this.textSelected = textSelected;
      this.screenFrm.setScreenTextSelected( textSelected );
    }
  }
}
