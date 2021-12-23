/*
 * (c) 2016-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Reduzieren der Anzahl der Farben in einem Bild
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.Arrays;
import jkcemu.Main;
import jkcemu.base.CancelableProgressDlg;


public class ColorReducer
		implements CancelableProgressDlg.Progressable, Runnable
{
  private Window                owner;
  private BufferedImage         srcImg;
  private BufferedImage         retImg;
  private Color                 colorForTransp;
  private Dithering.Algorithm   dithAlgorithm;
  private int                   maxColors;
  private int                   wImg;
  private int                   hImg;
  private volatile int          progressValue;
  private Dithering             dithering;
  private CancelableProgressDlg dlg;


  public static BufferedImage work(
				Window              owner,
				String              title,
				BufferedImage       srcImg,
				int                 maxColors,
				Color               colorForTransp,
				Dithering.Algorithm dithAlgorithm )
  {
    ColorReducer instance = new ColorReducer(
					owner,
					srcImg,
					maxColors,
					colorForTransp,
					dithAlgorithm );
    instance.dlg = new CancelableProgressDlg( owner, title, instance );
    (new Thread(
		Main.getThreadGroup(),
		instance,
		"JKCEMU color reducer" )).start();
    instance.dlg.setVisible( true );
    return instance.dlg.wasCancelled() ? null : instance.retImg;
  }


	/* --- CancelableProgressDlg.Progressable --- */

  @Override
  public int getProgressMax()
  {
    return 2 * this.wImg * this.hImg;
  }

  @Override
  public int getProgressValue()
  {
    Dithering dithering = this.dithering;
    return dithering != null ?
		this.progressValue + dithering.getProgressValue()
		: this.progressValue;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      if( (this.wImg > 0) && (this.hImg > 0) ) {
	BufferedImage srcImg = this.srcImg;

	// ggf. Transparenz aufloesen
	if( (srcImg.getTransparency() != Transparency.OPAQUE)
	    && (colorForTransp != null) )
	{
	  BufferedImage tmpImg = new BufferedImage(
					this.wImg,
					this.hImg,
					BufferedImage.TYPE_3BYTE_BGR );
	  Graphics g = tmpImg.createGraphics();
	  g.setColor( this.colorForTransp );
	  g.fillRect( 0, 0, this.wImg, this.hImg );
	  g.drawImage( srcImg, 0, 0, this.owner );
	  g.dispose();
	  srcImg = tmpImg;
	}

	// OcTree aufbauen
	this.progressValue   = 0;
	OcTree  ocTree       = new OcTree();
	boolean transparency = false;
	for( int y = 0; y < this.hImg; y++ ) {
	  for( int x = 0; x < this.wImg; x++ ) {
	    if( this.dlg.wasCancelled() ) {
	      break;
	    }
	    int argb = srcImg.getRGB( x, y );
	    if( ((argb >> 24) & 0xFF) < 0x80 ) {
	      transparency = true;
	    } else {
	      ocTree.putPixel( argb );
	    }
	    this.progressValue++;
	  }
	}

	// Farben reduzieren
	int[] rgbs = ocTree.reduceColors(
		transparency ? (this.maxColors - 1) : this.maxColors );
	if( rgbs != null ) {
	  int colorCnt = rgbs.length;
	  if( transparency ) {
	    colorCnt++;
	  }

	  // IndexColorModel erzeugen
	  byte[] a = null;
	  byte[] r = new byte[ colorCnt ];
	  byte[] g = new byte[ colorCnt ];
	  byte[] b = new byte[ colorCnt ];
	  if( transparency ) {
	    // letzter Pixel Transparenz
	    a = new byte[ colorCnt ];
	    Arrays.fill( a, (byte) 0xFF );
	    a[ rgbs.length ] = (byte) (ImageUtil.TRANSPARENT_ARGB >> 24);
	    r[ rgbs.length ] = (byte) (ImageUtil.TRANSPARENT_ARGB >> 16);
	    g[ rgbs.length ] = (byte) (ImageUtil.TRANSPARENT_ARGB >> 8);
	    b[ rgbs.length ] = (byte) ImageUtil.TRANSPARENT_ARGB;
	  }
	  for( int i = 0; i < rgbs.length; i++ ) {
	    r[ i ] = (byte) ((rgbs[ i ] >> 16) & 0xFF);
	    g[ i ] = (byte) ((rgbs[ i ] >> 8) & 0xFF);
	    b[ i ] = (byte) (rgbs[ i ] & 0xFF);
	  }
	  IndexColorModel icm = ImageUtil.createIndexColorModel(
							colorCnt,
							r,
							g,
							b,
							a );
	  if( this.dithAlgorithm != null ) {
	    this.dithering = new Dithering( srcImg, icm, dithAlgorithm );
	    this.dithering.setDialog( this.dlg );
	    this.retImg = this.dithering.doDithering();

	  } else {

	    // Bild mit indexierten Farben erzeugen
	    BufferedImage newImg = new BufferedImage(
				this.wImg,
				this.hImg,
				colorCnt > 16 ?
					BufferedImage.TYPE_BYTE_INDEXED
					: BufferedImage.TYPE_BYTE_BINARY,
				icm );
	    for( int y = 0; y < this.hImg; y++ ) {
	      for( int x = 0; x < this.wImg; x++ ) {
		if( this.dlg.wasCancelled() ) {
		  break;
		}
		int argb = srcImg.getRGB( x, y );
		if( ((argb >> 24) & 0xFF) < 0x80 ) {
		  newImg.setRGB( x, y, ImageUtil.TRANSPARENT_ARGB );
		} else {
		  newImg.setRGB( x, y, argb );
		}
		this.progressValue++;
	      }
	    }
	    this.retImg = newImg;
	  }
	}
      }
    }
    finally {
      dlg.fireProgressFinished();
    }
  }


	/* --- Konstruktor --- */

  private ColorReducer(
		Window              owner,
		BufferedImage       srcImg,
		int                 maxColors,
		Color               colorForTransp,
		Dithering.Algorithm dithAlgorithm )
  {
    this.owner          = owner;
    this.srcImg         = srcImg;
    this.maxColors      = maxColors;
    this.colorForTransp = colorForTransp;
    this.dithAlgorithm  = dithAlgorithm;
    this.wImg           = srcImg.getWidth();
    this.hImg           = srcImg.getHeight();
    this.progressValue  = 0;
    this.dithering      = null;
    this.dlg            = null;
    this.retImg         = null;
  }
}
