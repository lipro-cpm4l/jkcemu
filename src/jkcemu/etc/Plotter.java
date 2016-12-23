/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation eines Plotters
 */

package jkcemu.etc;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.lang.*;
import java.util.Properties;
import jkcemu.base.EmuUtil;
import jkcemu.base.UserInputException;


public class Plotter
{
  public static final String PROP_PAPER_COLOR = "jkcemu.plotter.paper.color";
  public static final String PROP_PEN_COLOR   = "jkcemu.plotter.pen.color";
  public static final String PROP_PEN_THICKNESS
					= "jkcemu.plotter.pen.thickness";

  public static final int DEFAULT_PEN_THICKNESS = 3;

  private Color           colorPaper;
  private Color           colorPen;
  private IndexColorModel colorModel;
  private BufferedImage   image;
  private PlotterFrm      plotterFrm;
  private int             width;
  private int             height;
  private int             curX;
  private int             curY;
  private int             paperRGB;
  private int             penRGB;
  private int             penThickness;
  private boolean         penState;
  private boolean         clean;


  public Plotter()
  {
    this.image        = null;
    this.plotterFrm   = null;
    this.width        = width;
    this.height       = height;
    this.curX         = 0;
    this.curY         = 0;
    this.paperRGB     = 0xFFFFFFFF;
    this.penRGB       = 0xFF000000;
    this.penThickness = DEFAULT_PEN_THICKNESS;
    this.penState     = false;
    this.clean        = true;
    this.colorPaper   = new Color( this.paperRGB );
    this.colorPen     = new Color( this.penRGB );
    createColorModel();
  }


  public void applySettings( Properties props )
  {
    String text = EmuUtil.getProperty( props, PROP_PAPER_COLOR );
    if( !text.isEmpty() ) {
      try {
	setPaperColor( new Color( (int) Long.parseLong( text, 16 ) ) );
      }
      catch( NumberFormatException ex ) {}
      catch( UserInputException ex ) {}
    }
    text = EmuUtil.getProperty( props, PROP_PEN_COLOR );
    if( !text.isEmpty() ) {
      try {
	setPenColor( new Color( (int) Long.parseLong( text, 16 ) ) );
      }
      catch( NumberFormatException ex ) {}
      catch( UserInputException ex ) {}
    }
    text = EmuUtil.getProperty( props, PROP_PEN_THICKNESS );
    if( !text.isEmpty() ) {
      try {
	int v = Integer.parseInt( text );
	if( (v >= 1) && (v <= 5) ) {
	  synchronized( this ) {
	    if( v != this.penThickness ) {
	      this.penThickness = v;
	      if( this.plotterFrm != null ) {
		this.plotterFrm.setPenThickness( this.penThickness );
	      }
	    }
	  }
	}
      }
      catch( NumberFormatException ex ) {}
    }
  }


  public synchronized void die()
  {
    if( this.image != null ) {
      this.image.flush();
      this.image = null;
      this.clean = true;
    }
  }


  public BufferedImage getBufferedImage()
  {
    return this.image;
  }


  public Color getPaperColor()
  {
    return this.colorPaper;
  }


  public Color getPenColor()
  {
    return this.colorPen;
  }


  public boolean isClean()
  {
    return this.clean;
  }


  public synchronized void movePen( int dx, int dy )
  {
    if( (dx != 0) || (dy != 0) ) {
      int x = this.curX + dx;
      if( x >= this.width ) {
	x = this.width - 1;
      }
      if( x < 0 ) {
	x = 0;
      }
      this.curX = x;

      int y = this.curY + dy;
      if( y >= this.height ) {
	y = this.height - 1;
      }
      if( y < 0 ) {
	y = 0;
      }
      this.curY = y;
      if( this.penState && (this.image != null) ) {
	if( (this.curX < this.image.getWidth())
	    && (this.curY < this.image.getHeight()) )
	{
	  if( (dx != dy) && ((Math.abs( dx ) > 1) || (Math.abs( dy ) > 1)) ) {
	    Graphics g = this.image.createGraphics();
	    int      n = this.penThickness;
	    if( n > 1 ) {
	      g.drawRect(
			this.curX - dx,
			this.curY - dy,
			dx + n - 1,
			dy + n - 1 );
	    } else {
	      g.drawLine(
			this.curX - dx,
			this.curY - dy,
			this.curX,
			this.curY );
	    }
	    g.dispose();
	  } else {
	    paintPixel();
	  }
	  this.clean = false;
	  if( this.plotterFrm != null ) {
	    this.plotterFrm.setDirty();
	  }
	}
      }
    }
  }


  public synchronized void newPage()
  {
    if( this.image != null ) {
      if( this.image.getColorModel() != this.colorModel ) {
	createBufferedImage();
      }
    }
    clearPage();
  }


  public synchronized void setPageSize( int width, int height )
  {
    this.width  = width;
    this.height = height;
    if( this.image != null ) {
      if( (width != this.image.getWidth())
	  || (height != this.image.getHeight()) )
      {
	createBufferedImage();
      }
    } else {
      createBufferedImage();
    }
  }


  public synchronized void setPaperColor( Color color )
						throws UserInputException
  {
    if( color.getRGB() != this.paperRGB ) {
      this.colorPaper = color;
      createColorModel();
      if( isClean() ) {
	createBufferedImage();
      } else {
	throw new UserInputException(
			"Die neue Papierfarbe wird erst auf der"
				+ " n\u00E4chsten Seite wirksam." );
      }
    }
  }


  public synchronized void setPenColor( Color color )
						throws UserInputException
  {
    if( color.getRGB() != this.penRGB ) {
      this.colorPen = color;
      createColorModel();
      if( isClean() ) {
	createBufferedImage();
      } else {
	throw new UserInputException(
			"Die neue Stiftfarbe wird erst auf der"
				+ " n\u00E4chsten Seite wirksam." );
      }
    }
  }


  public synchronized void setPenState( boolean state )
  {
    if( state != this.penState ) {
      this.penState = state;
      if( state ) {
	if( paintPixel() ) {
	  this.clean = false;
	  if( this.plotterFrm != null ) {
	    this.plotterFrm.setDirty();
	  }
	}
      }
    }
  }


  public void setPenThickness( int thk )
  {
    this.penThickness = thk;
  }


  public synchronized void setPlotterFrm( PlotterFrm frm )
  {
    this.plotterFrm = frm;
    if( this.plotterFrm != null ) {
      this.plotterFrm.setPenThickness( this.penThickness );
    }
  }


  public synchronized void reset()
  {
    this.curX     = 0;
    this.curY     = 0;
    this.penState = false;
  }


	/* --- private Methoden --- */

  private void clearPage()
  {
    if( this.image != null ) {
      Graphics g = this.image.createGraphics();
      g.setColor( this.colorPaper );
      g.fillRect( 0, 0, this.image.getWidth(), this.image.getHeight() );
      g.dispose();
      this.clean = true;
      if( this.plotterFrm != null ) {
	this.plotterFrm.setDirty();
      }
    }
  }


  private void createBufferedImage()
  {
    if( this.image != null ) {
      this.image.flush();
      this.image = null;
    }
    if( (this.width > 0) && (this.height > 0) ) {
      this.paperRGB = this.colorPaper.getRGB();
      this.penRGB   = this.colorPen.getRGB();
      this.image    = new BufferedImage(
				this.width,
				this.height,
				BufferedImage.TYPE_BYTE_INDEXED,
				this.colorModel );
      clearPage();
    }
  }


  private void createColorModel()
  {
    byte[] r = new byte[] {
			(byte) this.colorPaper.getRed(),
			(byte) this.colorPen.getRed() };

    byte[] g = new byte[] {
			(byte) this.colorPaper.getGreen(),
			(byte) this.colorPen.getGreen() };

    byte[] b = new byte[] {
			(byte) this.colorPaper.getBlue(),
			(byte) this.colorPen.getBlue() };

    if( this.colorPaper.getAlpha() > 0 ) {
      byte[] a = new byte[] {
			(byte) this.colorPaper.getAlpha(),
			(byte) 0xFF };
      this.colorModel = new IndexColorModel( 1, 2, r, g, b, a );
    } else {
      this.colorModel = new IndexColorModel( 1, 2, r, g, b );
    }
  }


  private boolean paintPixel()
  {
    boolean       rv    = false;
    BufferedImage image = this.image;
    if( image != null ) {
      int x = this.curX;
      int y = this.curY;
      if( (x >= 0) && (x < this.image.getWidth())
	  && (y >= 0) && (y < this.image.getHeight()) )
      {
	int n = this.penThickness;
	if( n > 1 ) {
	  for( int i = 0; i < n; i++ ) {
	    for( int k = 0; k < n; k++ ) {
	      this.image.setRGB( x + i, y + k, this.penRGB );
	    }
	  }
	} else {
	  this.image.setRGB( x, y, this.penRGB );
	}
	rv = true;
      }
    }
    return rv;
  }
}
