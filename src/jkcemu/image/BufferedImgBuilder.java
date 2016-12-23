/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erzeugung eines BufferedImage aus einem Image
 */

package jkcemu.image;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.lang.*;
import java.util.Set;
import java.util.TreeSet;
import jkcemu.Main;
import jkcemu.base.CancelableProgressDlg;


public class BufferedImgBuilder
		implements CancelableProgressDlg.Progressable, Runnable
{
  private Window                owner;
  private Image                 srcImg;
  private BufferedImage         retImg;
  private CancelableProgressDlg dlg;
  private Thread                thread;
  private ImgEntry.Mode         mode;
  private volatile int          progressValue;
  private int                   wImg;
  private int                   hImg;


  public BufferedImgBuilder( Window owner )
  {
    this.owner         = owner;
    this.srcImg        = null;
    this.retImg        = null;
    this.dlg           = null;
    this.thread        = null;
    this.mode          = ImgEntry.Mode.UNSPECIFIED;
    this.progressValue = 0;
    this.wImg          = 0;
    this.hImg          = 0;
  }


  public BufferedImage buildFrom( Image srcImg, String title )
  {
    this.srcImg        = srcImg;
    this.retImg        = null;
    this.dlg           = null;
    this.thread        = null;
    this.mode          = ImgEntry.Mode.UNSPECIFIED;
    this.progressValue = 0;
    this.wImg          = 0;
    this.hImg          = 0;
    if( this.srcImg != null ) {
      if( this.srcImg instanceof BufferedImage ) {
	this.retImg = (BufferedImage) srcImg;
	this.mode   = ImgEntry.probeMode( this.retImg );
      } else {
	ImgUtil.ensureImageLoaded( this.owner, this.srcImg );
	this.wImg   = srcImg.getWidth( owner );
	this.hImg   = srcImg.getHeight( owner );
	this.dlg    = new CancelableProgressDlg( this.owner, title, this );
	this.thread = new Thread(
				Main.getThreadGroup(),
				this,
				"JKCEMU image builder" );
	this.thread.start();
	this.dlg.setVisible( true );
	if( this.dlg.wasCancelled() ) {
	  this.retImg = null;
	}
      }
    }
    return this.retImg;
  }


  public ImgEntry.Mode getMode()
  {
    return this.mode;
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
    BufferedImage retImg = null;
    try {
      if( (this.wImg > 0) && (this.hImg > 0) ) {

	/*
	 * erst einmal ein BufferedImage mit maximalen Farben
	 * und Transparenz erzeugen und das Bild darauf kopieren
	 */
	BufferedImage tmpImg = new BufferedImage(
					this.wImg,
					this.hImg,
					BufferedImage.TYPE_INT_ARGB );
	Graphics g1 = tmpImg.createGraphics();
	g1.drawImage( this.srcImg, 0, 0, this.owner );
	g1.dispose();

	/*
	 * Anzahl der Farben ermitteln und Transparenz pruefen
	 */
	boolean      gray   = true;
	boolean      mono   = true;
	boolean      transp = false;
	Set<Integer> colors = new TreeSet<>();
	for( int y = 0; y < this.hImg; y++ ) {
	  for( int x = 0; x < this.wImg; x++ ) {
	    int argb = tmpImg.getRGB( x, y );
	    if( ((argb >> 24) & 0xFF) < 0xFF ) {
	      transp = true;
	    }
	    colors.add( argb );
	    int c = (argb & 0xFF);
	    if( (c != ((argb >> 16) & 0xFF))
		|| (c != ((argb >> 8) & 0xFF)) ) {
	      gray = false;
	      mono = false;
	    }
	    if( (c != 0) && (c != 0xFF) ) {
	      mono = false;
	    }
	    this.progressValue++;
	    if( this.dlg.wasCancelled() ) {
	      break;
	    }
	  }
	}

	/*
	 * Wenn die Anzahl der Farben 256 nicht uebersteigt,
	 * dann das Bild in eins mit indexiertem Farbmodell umwandeln.
	 * Bei fehlender Transparenz das Bild ebensfalls umwandeln.
	 */
	int colorCnt = colors.size();
	if( (colorCnt > 0) && (colorCnt <= 256) ) {
	  byte[] reds   = new byte[ colorCnt ];
	  byte[] greens = new byte[ colorCnt ];
	  byte[] blues  = new byte[ colorCnt ];
	  byte[] alphas = (transp ? new byte[ colorCnt ] : null);
	  int    idx    = 0;
	  for( int c : colors ) {
	    if( idx >= colorCnt ) {
	      break;
	    }
	    if( alphas != null ) {
	      alphas[ idx ] = (byte) ((c >> 24) & 0xFF);
	    }
	    reds[ idx ]   = (byte) ((c >> 16) & 0xFF);
	    greens[ idx ] = (byte) ((c >> 8) & 0xFF);
	    blues[ idx ]  = (byte) (c & 0xFF);
	    idx++;
	  }
	  retImg = ImgUtil.createIndexedColorsImage(
						this.wImg,
						this.hImg,
						colors );
	  this.mode = ImgEntry.Mode.INDEXED_COLORS;
	}
	if( (retImg == null) && !transp ) {
	  retImg = new BufferedImage(
				this.wImg,
				this.hImg,
				BufferedImage.TYPE_3BYTE_BGR );
	}
	if( retImg != null ) {
	  Graphics g2 = retImg.createGraphics();
	  g2.drawImage( tmpImg, 0, 0, this.owner );
	  g2.dispose();
	}
	if( mono ) {
	  this.mode = ImgEntry.Mode.MONOCHROME;
	} else if( gray ) {
	  this.mode = ImgEntry.Mode.GRAY;
	}
      }
    }
    finally {
      if( !this.dlg.wasCancelled() ) {
	this.retImg = retImg;
      }
      dlg.fireProgressFinished();
    }
  }
}
