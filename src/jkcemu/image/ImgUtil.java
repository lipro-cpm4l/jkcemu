/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer Bilder
 */

package jkcemu.image;

import java.awt.*;
import java.awt.image.*;
import java.lang.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.base.*;
import jkcemu.emusys.a5105.VIS;


public class ImgUtil
{
  public static final int ROUND_PIXELS_MAX = 9;

  private static IndexColorModel cmA5105 = null;
  private static IndexColorModel cmBW    = null;


  public static BufferedImage createBufferedImage(
					Component owner,
					Image     image )
  {
    BufferedImage retImg = null;
    if( image != null ) {
      ensureImageLoaded( owner, image );
      int w = image.getWidth( owner );
      int h = image.getHeight( owner );
      if( (w > 0) && (h > 0) ) {
	retImg = new BufferedImage( w, h, BufferedImage.TYPE_4BYTE_ABGR );
	Graphics g = retImg.createGraphics();
	if( g != null ) {
	  g.drawImage( image, 0, 0, owner );
	  g.dispose();
	} else {
	  retImg = null;
	}
      }
    }
    return retImg;
  }


  public static FileNameExtensionFilter createFileFilter(
						String[] suffixes1,
						String... suffixes2 )
  {
    FileNameExtensionFilter rv       = null;
    String[]                suffixes = null;
    if( (suffixes1 != null) && (suffixes2 != null) ) {
      if( (suffixes1.length > 0) && (suffixes2.length > 0) ) {
	suffixes = new String[ suffixes1.length + suffixes2.length ];
	int pos = 0;
	for( int i = 0; i < suffixes1.length; i++ ) {
	  suffixes[ pos++ ] = suffixes1[ i ];
	}
	for( int i = 0; i < suffixes2.length; i++ ) {
	  suffixes[ pos++ ] = suffixes2[ i ];
	}
      }
    }
    if( (suffixes == null) && (suffixes1 != null) ) {
      if( suffixes1.length > 0 ) {
	suffixes = suffixes1;
      }
    }
    if( suffixes == null ) {
      suffixes = suffixes2;
    }
    if( suffixes != null ) {
      rv = new FileNameExtensionFilter(
				"Unterst\u00FCtzte Bilddateien",
				suffixes );
    }
    return rv;
  }


  public static void ensureImageLoaded( Component owner, Image image )
  {
    if( image != null ) {
      boolean done  = false;
      Toolkit tk    = owner.getToolkit();
      if( tk != null ) {
	int flags = tk.checkImage( image, -1, -1, owner );
	if( (flags & (ImageObserver.ABORT
			| ImageObserver.ALLBITS
			| ImageObserver.ERROR
			| ImageObserver.FRAMEBITS)) != 0 )
	{
	  done = true;
	}
      }
      if( !done ) {
	MediaTracker mt = new MediaTracker( owner );
	mt.addImage( image, 0 );
	try {
	  mt.waitForID( 0 );
	}
	catch( InterruptedException ex ) {}
      }
    }
  }


  public static void fillBlack( BufferedImage img )
  {
    Graphics g = img.createGraphics();
    g.setColor( Color.black );
    g.fillRect( 0, 0, img.getWidth(), img.getHeight() );
    g.dispose();
  }


  public synchronized static IndexColorModel getColorModelA5105()
  {
    if( cmA5105 == null ) {
      cmA5105 = VIS.createColorModel( 1F );
    }
    return cmA5105;
  }


  public synchronized static IndexColorModel getColorModelBW()
  {
    if( cmBW == null ) {
      cmBW = new IndexColorModel(
			1,
			2,
			new byte[] { 0, (byte) 0xFF },
			new byte[] { 0, (byte) 0xFF },
			new byte[] { 0, (byte) 0xFF } );
    }
    return cmBW;
  }


  public static int getNearestIndex( IndexColorModel cm, int rgb )
  {
    int    idx    = -1;
    long   diff   = 0;
    int    r      = (rgb >> 16) & 0xFF;
    int    g      = (rgb >> 8) & 0xFF;
    int    b      = rgb & 0xFF;
    int    size   = cm.getMapSize();
    byte[] reds   = new byte[ size ];
    byte[] greens = new byte[ size ];
    byte[] blues  = new byte[ size ];
    cm.getReds( reds );
    cm.getGreens( greens );
    cm.getBlues( blues );
    for( int i = 0; i < size; i++ ) {
      long diffR = Math.abs( ((int) reds[ i ] & 0xFF) - r );
      long diffG = Math.abs( ((int) greens[ i ] & 0xFF) - g );
      long diffB = Math.abs( ((int) blues[ i ] & 0xFF) - b );
      long d     = (diffR * diffR) + (diffG * diffG) + (diffB * diffB);
      if( (idx < 0) || (d < diff) ) {
	idx  = i;
	diff = d;
      }
    }
    return idx >= 0 ? idx : 0;
  }


  public static boolean hasAlpha( BufferedImage image )
  {
    boolean rv = false;
    if( image != null ) {
      int t = image.getType();
      if( (t == BufferedImage.TYPE_4BYTE_ABGR)
	  || (t == BufferedImage.TYPE_4BYTE_ABGR_PRE)
	  || (t == BufferedImage.TYPE_INT_ARGB)
	  || (t == BufferedImage.TYPE_INT_ARGB_PRE) )
      {
	rv = true;
      }
    }
    return rv;
  }


  public static BufferedImage roundCorners(
					Component owner,
					Image     image,
					int       nTopPixels,
					int       nBottomPixels )
  {
    BufferedImage retImg = null;
    if( (image != null) && ((nTopPixels > 0) || (nBottomPixels > 0)) ) {
      /*
       * Wenn das Bild ein BufferedImage ist und Transparenz beherrscht,
       * kann es verwendet werden.
       * Anderenfalls wird ein neues BufferedImage angelegt.
       */
      if( image instanceof BufferedImage ) {
	if( hasAlpha( (BufferedImage) image ) ) {
	  retImg = (BufferedImage) image;
	} else {
	  ColorModel cm = ((BufferedImage) image).getColorModel();
	  if( cm != null ) {
	    if( cm instanceof IndexColorModel ) {
	      if( ((IndexColorModel) cm).getTransparentPixel() >= 0 ) {
		retImg = (BufferedImage) image;
	      }
	    }
	  }
	}
      }
      if( retImg == null ) {
	retImg = createBufferedImage( owner, image );
      }
      if( retImg != null ) {
	int w = retImg.getWidth();
	int h = retImg.getHeight();
	int m = 2 * Math.max( nTopPixels, nBottomPixels );
	if( (w > m) && (h > m) ) {
	  int r = nTopPixels - 1;
	  for( int x = 0; x < nTopPixels; x++ ) {
	    int dx = nTopPixels - 1 - x;
	    for( int y = 0; y < nTopPixels; y++ ) {
	      int    dy = nTopPixels - 1 - y;
	      double d  = Math.sqrt( (double) ((dx * dx) + (dy * dy)) );
	      if( Math.round( d ) >= r ) {
		retImg.setRGB( x, y, 0 );
		retImg.setRGB( w - x - 1, y, 0 );
	      }
	    }
	  }
	  r = nBottomPixels - 1;
	  for( int x = 0; x < nBottomPixels; x++ ) {
	    int dx = nBottomPixels - 1 - x;
	    for( int y = 0; y < nBottomPixels; y++ ) {
	      int    dy = nBottomPixels - 1 - y;
	      double d  = Math.sqrt( (double) ((dx * dx) + (dy * dy)) );
	      if( Math.round( d ) >= r ) {
		retImg.setRGB( x, h - y - 1, 0 );
		retImg.setRGB( w - x - 1, h - y - 1, 0 );
	      }
	    }
	  }
	}
      }
    }
    return retImg;
  }


	/* --- Konstruktor --- */

  private ImgUtil()
  {
    // nicht instanziierbar
  }
}
