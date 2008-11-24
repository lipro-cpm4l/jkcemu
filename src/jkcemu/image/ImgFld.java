/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Anzeige eines Bildes
 */

package jkcemu.image;

import java.awt.*;
import java.awt.print.*;
import java.lang.*;


public class ImgFld extends Component implements Printable
{
  public static enum Rotation { NONE, LEFT, RIGHT, DOWN };

  private int       defaultWidth;
  private int       defaultHeight;
  private double    scale;
  private Rotation  rotation;
  private Image     image;
  private Dimension vpSize;


  public ImgFld( int defaultWidth, int defaultHeight )
  {
    this.defaultWidth  = defaultWidth;
    this.defaultHeight = defaultHeight;
    this.rotation      = Rotation.NONE;
    this.scale         = 1.0;
    this.image         = null;
    this.vpSize        = null;
  }


  public void drawImage( Graphics g, int x, int y, int w, int h )
  {
    if( (this.rotation != Rotation.NONE)&& (g instanceof Graphics2D) ) {
      Graphics2D g2d = (Graphics2D) g;
      switch( this.rotation ) {
	case LEFT:
	  x = -w;
	  g2d.rotate( -(Math.PI / 2.0) );
	  break;

	case RIGHT:
	  y = -h;
	  g2d.rotate( Math.PI / 2.0 );
	  break;

	case DOWN:
	  x = -w;
	  y = -h;
	  g2d.rotate( Math.PI );
      }
    }
    g.drawImage( image, x, y, w, h, this );
  }


  public Rotation getRotation()
  {
    return this.rotation;
  }


  public double getScale()
  {
    return this.scale;
  }


  public void setImage( Image image )
  {
    this.image = image;
    invalidate();
  }


  public void setRotation( Rotation rotation )
  {
    this.rotation = rotation;
    invalidate();
  }


  public void setScale( double scale )
  {
    this.scale = scale;
    invalidate();
  }


  public void setViewportSize( Dimension size )
  {
    this.vpSize = size;
  }


	/* --- Printable --- */

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

      int wImg = this.image.getWidth( this );
      int hImg = this.image.getHeight( this );

      if( (w > 0) && (h > 0) && (wImg > 0) && (hImg > 0) ) {
	if( (this.rotation == Rotation.LEFT)
	    || (this.rotation == Rotation.RIGHT) )
	{
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

  public Dimension getPreferredSize()
  {
    int w = this.defaultWidth;
    int h = this.defaultHeight;
    if( this.image != null ) {
      if( this.scale > 0.0 ) {
	w = (int) Math.round(
			(double) this.image.getWidth( this ) * this.scale );
	h = (int) Math.round(
			(double) this.image.getHeight( this ) * this.scale );
      } else {
	if( this.vpSize != null ) {
	  w = this.vpSize.width;
	  h = this.vpSize.height;
	}
      }
    }
    if( (this.rotation == Rotation.LEFT)
	|| (this.rotation == Rotation.RIGHT) )
    {
      int m = w;
      w     = h;
      h     = m;
    }
    return new Dimension( w > 0 ? w : 0, h > 0 ? h : 0 );
  }


  public void paint( Graphics g )
  {
    g.setPaintMode();
    Color color = getBackground();
    if( color != null ) {
      g.setColor( getBackground() );
      g.fillRect( 0, 0, getWidth(), getHeight() );
    }
    if( this.image != null ) {
      int w = this.image.getWidth( this );
      int h = this.image.getHeight( this );

      double scale = this.scale;
      if( scale <= 0.0 ) {
	if( this.vpSize != null ) {
	  scale = Math.min(
			(double) (this.vpSize.width - 1) / (double) w,
			(double) (this.vpSize.height - 1) / (double) h );
	}
      }
      if( scale > 0.0 ) {
	w = (int) Math.round( (double) w * scale );
	h = (int) Math.round( (double) h * scale );
      }
      drawImage( g, 0, 0, w, h );
    }
  }
}

