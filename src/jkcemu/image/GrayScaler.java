/*
 * (c) 2016-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Umwandeln eines Bildes in Graustufen
 *
 * Wenn das Bild Transparenz enthaelt,
 * erfolgt die Grauwandlung Pixel fuer Pixel.
 * Da das allerdings eine gewisse Zeit dauerert,
 * wird dies in einem separaten Thread erledigt.
 */

package jkcemu.image;

import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.Set;
import java.util.TreeSet;
import jkcemu.Main;
import jkcemu.base.CancelableProgressDlg;


public class GrayScaler
		implements CancelableProgressDlg.Progressable, Runnable
{
  private BufferedImage         srcImg;
  private BufferedImage         retImg;
  private int                   wImg;
  private int                   hImg;
  private volatile int          progressValue;
  private CancelableProgressDlg dlg;


  /*
   * Umwandlung eines RGB-Wertes in einen RGB-Grauwert
   *
   * Das anteilige Verhaeltnis der Primaerfarben
   * entspricht der Empfehlung ITU-R BT.601
   */
  public static int toGray( int argb )
  {
    int c = Math.round( ((float) ((argb >> 16) & 0xFF) * 0.299F)
			+ ((float) ((argb >> 8) & 0xFF) * 0.587F)
			+ ((float) (argb & 0xFF) * 0.114F) );
    return (argb & 0xFF000000)
		| ((c << 16) & 0x00FF0000)
		| ((c << 8) & 0x0000FF00)
		| (c & 0x000000FF);
  }


  public static BufferedImage toGray(
				Window        owner,
				BufferedImage srcImg )
  {
    BufferedImage retImg = null;
    if( srcImg.getTransparency() == Transparency.OPAQUE ) {
      int          w      = srcImg.getWidth();
      int          h      = srcImg.getHeight();
      Set<Integer> colors = getIndexedColors( srcImg );
      if( !colors.isEmpty() && (colors.size() <= 256) ) {
	retImg = ImageUtil.createIndexedColorsImage( w, h, colors );
      }
      if( retImg == null ) {
	retImg = new BufferedImage(
			w,
			h,
			BufferedImage.TYPE_BYTE_INDEXED,
			ImageUtil.getColorModelSortedGray() );
      }
      Graphics g = retImg.createGraphics();
      g.drawImage( srcImg, 0, 0, owner );
      g.dispose();
    } else {
      GrayScaler instance = new GrayScaler( srcImg );
      instance.dlg        = new CancelableProgressDlg(
						owner,
						"Graustufen erzeugen",
						instance );
      (new Thread(
		Main.getThreadGroup(),
		instance,
		"JKCEMU gray scaler" )).start();
      instance.dlg.setVisible( true );
      if( !instance.dlg.wasCancelled() ) {
	retImg = instance.retImg;
      }
    }
    return retImg;
  }


	/* --- CancelableProgressDlg.Progressable --- */

  @Override
  public int getProgressMax()
  {
    return this.wImg * this.hImg;
  }

  @Override
  public int getProgressValue()
  {
    return this.progressValue;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      BufferedImage retImg = null;
      Set<Integer>  colors = getIndexedColors( this.srcImg );
      if( !colors.isEmpty() && (colors.size() <= 256) ) {
	retImg = ImageUtil.createIndexedColorsImage(
						this.wImg,
						this.hImg,
						colors );
      }
      if( retImg == null ) {
	retImg = new BufferedImage(
			this.wImg,
			this.hImg,
			BufferedImage.TYPE_INT_ARGB );
      }
      for( int y = 0; y < this.hImg; y++ ) {
	for( int x = 0; x < this.wImg; x++ ) {
	  if( this.dlg.wasCancelled() ) {
	    break;
	  }
	  retImg.setRGB( x, y, toGray( this.srcImg.getRGB( x, y ) ) );
	  this.progressValue++;
	}
      }
      if( !this.dlg.wasCancelled() ) {
	this.retImg = retImg;
      }
    }
    finally {
      dlg.fireProgressFinished();
    }
  }


	/* --- Konstruktor --- */

  private GrayScaler( BufferedImage srcImg )
  {
    this.srcImg        = srcImg;
    this.retImg        = null;
    this.wImg          = srcImg.getWidth();
    this.hImg          = srcImg.getHeight();
    this.progressValue = 0;
    this.dlg           = null;
  }


	/* --- private Methoden --- */

  private static Set<Integer> getIndexedColors( BufferedImage image )
  {
    Set<Integer>    colors = new TreeSet<>();
    IndexColorModel icm    = ImageUtil.getIndexColorModel( image );
    if( icm != null ) {
      int n = icm.getMapSize();
      for( int i = 0; i < n; i++ ) {
	colors.add( toGray( icm.getRGB( i ) ) );
      }
    }
    return colors;
  }
}
