/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Anzeige eines Bildes
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import javax.swing.JViewport;


public class ImgFld extends Component implements Printable
{
  public static enum Rotation { NONE, LEFT, RIGHT, DOWN };

  private int           defaultWidth;
  private int           defaultHeight;
  private double        curScale;
  private double        scale;
  private Rotation      rotation;
  private BufferedImage image;
  private Image         scaledImg;
  private Dimension     vpSize;
  private Color         selectionColor;
  private Rectangle     selection;
  private Rectangle     selectedArea;
  private double        savedScale;
  private Rotation      savedRotation;
  private BufferedImage savedImage;
  private Point         savedViewPos;


  public ImgFld( int defaultWidth, int defaultHeight )
  {
    this.defaultWidth   = defaultWidth;
    this.defaultHeight  = defaultHeight;
    this.rotation       = Rotation.NONE;
    this.scale          = 1.0;
    this.curScale       = this.scale;
    this.image          = null;
    this.scaledImg      = null;
    this.vpSize         = null;
    this.selectionColor = Color.red;
    this.selection      = null;
    this.selectedArea   = null;
    this.savedScale     = this.scale;
    this.savedRotation  = null;
    this.savedImage     = null;
    this.savedViewPos   = null;
  }


  public synchronized void drawImage( Graphics g, int x, int y, int w, int h )
  {
    if( (this.image != null) && (g != null) && (w > 0) && (h > 0) ) {
      Graphics g2 = g.create();
      if( !this.rotation.equals( Rotation.NONE )
	  && (g2 instanceof Graphics2D) )
      {
	switch( this.rotation ) {
	  case LEFT:
	    x = -w;
	    ((Graphics2D) g2).rotate( -(Math.PI / 2.0) );
	    break;

	  case RIGHT:
	    y = -h;
	    ((Graphics2D) g2).rotate( Math.PI / 2.0 );
	    break;

	  case DOWN:
	    x = -w;
	    y = -h;
	    ((Graphics2D) g2).rotate( Math.PI );
	    break;
	}
      }
      if( (w == this.image.getWidth()) && (h == this.image.getHeight()) ) {
        g2.drawImage( this.image, x, y, this );
	this.scaledImg = null;
      } else {
	/*
	 * Mit Dithering erstellte Bilder sehen mitunter haesslich aus,
	 * wenn diese mit einem einfachen Algorithmus skliert werden.
	 * Aus diesem Grund wird hier ein Algorithmus ausgewaehlt,
	 * der benachbarte Pixel miteinander verknuepft.
	 */
	if( this.scaledImg != null ) {
	  if( (this.scaledImg.getWidth( this ) != w)
	      || (this.scaledImg.getHeight( this ) != h) )
	  {
	    this.scaledImg = null;
	  }
	}
	if( this.scaledImg == null ) {
	  this.scaledImg = this.image.getScaledInstance(
						w,
						h,
						Image.SCALE_SMOOTH );
	}
	g2.drawImage( this.scaledImg, x, y, this );
      }
      g2.dispose();
    }
  }


  public double getCurScale()
  {
    return this.curScale;
  }


  public BufferedImage getImage()
  {
    return this.image;
  }


  /*
   * Die Methode erzeugt aus dem angezeigten Bild eine
   * evtl. herunterskalierte Version,
   * die als Vorschaubild verwendet werden kann.
   *
   * Wird kein Bild angezeigt, liefert die Methode null zurueck.
   */
  public BufferedImage getNewPreviewImage()
  {
    BufferedImage previewImg = null;
    if( this.image != null ) {
      Component parent = getParent();
      if( parent != null ) {
	float wParent = (float) parent.getWidth();
	float hParent = (float) parent.getHeight();
	float wImg    = (float) this.image.getWidth();
	float hImg    = (float) this.image.getHeight();
	if( (wParent > 0F) && (hParent > 0F) && (wImg > 0F) && (hImg > 0F) ) {
	  float scale = Math.max( wParent / wImg, hParent / hImg );
	  if( scale < 1F ) {
	    int w      = Math.round( wImg * scale );
	    int h      = Math.round( hImg * scale );
	    previewImg = ImgUtil.createCompatibleImage( this.image, w, h );
	    if( previewImg != null ) {
	      Graphics g = previewImg.createGraphics();
	      g.drawImage( this.image, 0, 0, w, h, this );
	      g.dispose();
	    }
	  }
	}
      }
    }
    return previewImg != null ? previewImg : this.image;
  }


  public Rotation getRotation()
  {
    return this.rotation;
  }


  public Dimension getRotatedImageSize()
  {
    Dimension rv = null;
    if( this.image != null ) {
      int w = this.image.getWidth();
      int h = this.image.getHeight();
      if( (w > 0) && (h > 0) ) {
	if( isRotated90Degrees() ) {
	  rv = new Dimension( h, w );
	} else {
	  rv = new Dimension( w, h);
	}
      }
    }
    return rv;
  }


  public double getScale()
  {
    return this.scale;
  }


  /*
   * Die Methode liefert die Koordinaten des ausgewaehlten Bereichs,
   * begrenzt auf die Bildgroesse, oder null zurueck.
   * Breite und Hoehe sind niemals kleiner 0.
   */
  public Rectangle getSelectedArea()
  {
    Rectangle rv        = null;
    Rectangle selection = this.selection;
    if( selection != null ) {
      rv = this.selectedArea;
      if( rv == null ) {
	Dimension size = getRotatedImageSize();
	if( size != null ) {
	  int x1 = selection.x;
	  int y1 = selection.y;
	  int w  = selection.width;
	  int h  = selection.height;
	  if( x1 < 0 ) {
	    w += x1;
	    x1 = 0;
	  }
	  if( y1 < 0 ) {
	    h += y1;
	    y1 = 0;
	  }
	  if( w < 0 ) {
	    x1 += w;
	    w = -w;
	  }
	  if( h < 0 ) {
	    y1 += h;
	    h = -h;
	  }
	  int x2 = x1 + w;
	  if( x2 > size.width ) {
	    x2 = size.width;
	  }
	  int y2 = y1 + h;
	  if( y2 > size.height ) {
	    y2 = size.height;
	  }
	  if( x1 < 0 ) {
	    x1 = 0;
	  }
	  if( y1 < 0 ) {
	    y1 = 0;
	  }
	  if( (x2 >= x1) && (y2 >= y1) ) {
	    rv = new Rectangle( x1, y1, x2 - x1, y2 - y1 );
	    this.selectedArea = rv;
	  }
	}
      }
    }
    return rv;
  }


  /*
   * Die Methode liefert die Koordinaten des ausgewaehlten Bereichs zurueck,
   * wobei die Breite und die Hoehe auch kleiner 0 sein koennen.
   */
  public Rectangle getSelection()
  {
    return this.selection;
  }


  public boolean isRotated90Degrees()
  {
    return this.rotation.equals( ImgFld.Rotation.LEFT )
		|| this.rotation.equals( ImgFld.Rotation.RIGHT );
  }


  public void save()
  {
    this.savedImage    = this.image;
    this.savedRotation = this.rotation;
    this.savedScale    = this.scale;
    this.savedViewPos  = null;

    Component p = getParent();
    if( p != null ) {
      if( p instanceof JViewport ) {
	this.savedViewPos = ((JViewport) p).getViewPosition();
      }
    }
  }


  public void restore()
  {
    this.image        = this.savedImage;
    this.rotation     = this.savedRotation;
    this.scale        = this.savedScale;
    this.scaledImg    = null;
    this.selection    = null;
    this.selectedArea = null;

    Component p = getParent();
    if( (p != null) && (this.savedViewPos != null) ) {
      if( p instanceof JViewport ) {
	((JViewport) p).setViewPosition( this.savedViewPos );
      }
    }
    invalidate();
  }


  public void setImage( BufferedImage image )
  {
    this.image        = image;
    this.scaledImg    = null;
    this.selection    = null;
    this.selectedArea = null;
    invalidate();
  }


  public void setRotation( Rotation rotation )
  {
    this.rotation     = rotation;
    this.selection    = null;
    this.selectedArea = null;
    invalidate();
  }


  public void setScale( double scale )
  {
    this.scale        = scale;
    this.selection    = null;
    this.selectedArea = null;
    invalidate();
  }


  public void setSelection( Rectangle r )
  {
    this.selection    = r;
    this.selectedArea = null;
    repaint();
  }


  public void setSelection( int x, int y, int w, int h )
  {
    setSelection( new Rectangle( x, y, w, h ) );
  }


  public void setSelectionColor( Color color )
  {
    this.selectionColor = color;
    repaint();
  }


  public void setViewportSize( Dimension size )
  {
    this.vpSize = size;
  }


  public Rectangle toUnrotated( int x, int y, int w, int h )
  {
    Rectangle rv = null;
    if( this.image != null ) {
      int wImg = this.image.getWidth();
      int hImg = this.image.getHeight();
      if( (wImg > 0) && (hImg > 0) ) {
	switch( this.rotation ) {
	  case LEFT:
	    rv = new Rectangle( wImg - y - h, x, h, w );
	    break;
	  case RIGHT:
	    rv = new Rectangle( y, hImg - x - w, h, w );
	    break;
	  case DOWN:
	    rv = new Rectangle( wImg - x - w, hImg - y - h, w, h );
	    break;
	  default:
	    rv = new Rectangle( x, y, w, h );
	    break;
	}
      }
    }
    return rv;
  }


	/* --- Printable --- */

  @Override
  public int print( Graphics g, PageFormat pf, int pageNum )
  {
    int rv = NO_SUCH_PAGE;
    if( (g != null) && (pf != null) && (pageNum == 0)
	&& (this.image != null) )
    {
      int x = (int) pf.getImageableX() + 1;
      int y = (int) pf.getImageableY() + 1;
      int w = (int) pf.getImageableWidth() - 1;
      int h = (int) pf.getImageableHeight() - 1;

      int wImg = this.image.getWidth();
      int hImg = this.image.getHeight();

      if( (w > 0) && (h > 0) && (wImg > 0) && (hImg > 0) ) {
	if( isRotated90Degrees() ) {
	  int m = w;
	  w     = h;
	  h     = m;
	}
	if( (wImg > w) || (hImg > h) ) {
	  double scale = Math.min(
				(double) w / (double) wImg,
				(double) h / (double) hImg );
	  wImg = (int) Math.round( (double) wImg * scale );
	  hImg = (int) Math.round( (double) hImg * scale );
	}
	drawImage( g, x, y, wImg, hImg );
	rv = PAGE_EXISTS;
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Dimension getPreferredSize()
  {
    Dimension rv = super.getPreferredSize();
    if( !isPreferredSizeSet() ) {
      int w = this.defaultWidth;
      int h = this.defaultHeight;
      if( this.image != null ) {
	if( this.scale > 0.0 ) {
	  w = (int) Math.round(
			(double) this.image.getWidth() * this.scale );
	  h = (int) Math.round(
			(double) this.image.getHeight() * this.scale );
	} else {
	  if( this.vpSize != null ) {
	    w = this.vpSize.width;
	    h = this.vpSize.height;
	  }
	}
      }
      if( isRotated90Degrees() ) {
	int m = w;
	w     = h;
	h     = m;
      }
      rv = new Dimension( w > 0 ? w : 0, h > 0 ? h : 0 );
    }
    return rv;
  }


  @Override
  public boolean isFocusable()
  {
    /*
     * Die Komponente soll den Focus bekommen koennen,
     * damit die umgebende JScrollPane auch mit der Tastaur bedienbar ist.
     */
    return true;
  }


  @Override
  public void paint( Graphics g )
  {
    // Hintergrund malen
    g.setPaintMode();
    Color color = getBackground();
    if( color != null ) {
      g.setColor( getBackground() );
      g.fillRect( 0, 0, getWidth(), getHeight() );
    }

    // Bild malen
    if( this.image != null ) {
      int wImg = this.image.getWidth();
      int hImg = this.image.getHeight();
      if( (wImg > 0) && (hImg > 0) ) {
	double scale = this.scale;
	if( scale <= 0.0 ) {
	  if( this.vpSize != null ) {
	    scale = Math.min(
			(double) (this.vpSize.width - 1) / (double) wImg,
			(double) (this.vpSize.height - 1) / (double) hImg );
	  }
	}
	int wScaled = wImg;
	int hScaled = hImg;
	if( scale > 0.0 ) {
	  wScaled       = (int) Math.round( (double) wImg * scale );
	  hScaled       = (int) Math.round( (double) hImg * scale );
	  this.curScale = scale;
	} else {
	  this.curScale = 1.0;
	}
	drawImage( g, 0, 0, wScaled, hScaled );

	// Rechteck des Auswahl malen
	Rectangle r = getSelectedArea();
	if( r != null ) {
	  g.setColor( this.selectionColor );
	  g.drawRect(
		(int) Math.round( (double) r.x * this.curScale ),
		(int) Math.round( (double) r.y * this.curScale ),
		(int) Math.round( (double) (r.width - 1) * this.curScale ),
		(int) Math.round( (double) (r.height - 1) * this.curScale ) );
	}
      }
    }
  }
}
