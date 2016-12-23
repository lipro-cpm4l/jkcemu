/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Invertieren der Farben in einem Bild
 */

package jkcemu.image;

import java.awt.Transparency;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.lang.*;
import java.util.Hashtable;
import jkcemu.Main;
import jkcemu.base.CancelableProgressDlg;


public class ColorInverter
		implements CancelableProgressDlg.Progressable, Runnable
{
  private Window                owner;
  private BufferedImage         srcImg;
  private BufferedImage         retImg;
  private int                   wImg;
  private int                   hImg;
  private volatile int          progressValue;
  private CancelableProgressDlg dlg;


  public static BufferedImage work(
				Window        owner,
				BufferedImage srcImg )
  {
    BufferedImage   retImg = null;
    IndexColorModel icm    = ImgUtil.getIndexColorModel( srcImg );
    if( icm != null ) {
      int colorCnt = icm.getMapSize();
      if( colorCnt > 0 ) {
	boolean transp = false;
	byte[]  alphas = new byte[ colorCnt ];
	byte[]  reds   = new byte[ colorCnt ];
	byte[]  greens = new byte[ colorCnt ];
	byte[]  blues  = new byte[ colorCnt ];
	for( int i = 0; i < colorCnt; i++ ) {
	  int argb = icm.getRGB( i );
	  int a    = (argb >> 24) & 0xFF;
	  if( a < 0xFF ) {
	    transp = true;
	  }
	  alphas[ i ] = (byte) a;
	  reds[ i ]   = (byte) (0xFF - ((argb >> 16) & 0xFF));
	  greens[ i ] = (byte) (0xFF - ((argb >> 8) & 0xFF));
	  blues[ i ]  = (byte) (0xFF - (argb & 0xFF));
	}
	if( !transp ) {
	  alphas = null;
	}
	retImg = new BufferedImage(
			ImgUtil.createIndexColorModel(
						colorCnt,
						reds,
						greens,
						blues,
						alphas ),
			srcImg.getRaster(),
			false,
			new Hashtable<>() );
      }
    }
    if( retImg == null ) {
      ColorInverter instance = new ColorInverter( owner, srcImg );
      instance.dlg = new CancelableProgressDlg(
					owner,
					"Invertiere...",
					instance );
      (new Thread(
		Main.getThreadGroup(),
		instance,
		"JKCEMU color inverter" )).start();
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
      if( (this.wImg > 0) && (this.hImg > 0) ) {
	BufferedImage newImg = new BufferedImage(
		this.wImg,
		this.hImg,
		srcImg.getTransparency() == Transparency.OPAQUE ?
				BufferedImage.TYPE_3BYTE_BGR
				: BufferedImage.TYPE_INT_ARGB );
	for( int y = 0; y < this.hImg; y++ ) {
	  for( int x = 0; x < this.wImg; x++ ) {
	    int argb = this.srcImg.getRGB( x, y );
	    int r    = (byte) (0xFF - ((argb >> 16) & 0xFF));
	    int g    = (byte) (0xFF - ((argb >> 8) & 0xFF));
	    int b    = (byte) (0xFF - (argb & 0xFF));
	    newImg.setRGB(
			x,
			y,
			(argb & 0xFF000000)
				| ((r << 16) & 0x00FF0000)
				| ((g << 8) & 0x0000FF00)
				| (b & 0x000000FF) );
	    this.progressValue++;
	    if( this.dlg.wasCancelled() ) {
	      break;
	    }
	  }
	}
	this.retImg = newImg;
      }
    }
    finally {
      dlg.fireProgressFinished();
    }
  }


	/* --- Konstruktor --- */

  private ColorInverter( Window owner, BufferedImage srcImg )

  {
    this.owner          = owner;
    this.srcImg         = srcImg;
    this.wImg           = srcImg.getWidth();
    this.hImg           = srcImg.getHeight();
    this.progressValue  = 0;
    this.dlg            = null;
    this.retImg         = null;
  }
}
