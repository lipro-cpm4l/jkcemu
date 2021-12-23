/*
 * (c) 2016-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Kopieren der Pixel von einem Bild in ein anderes
 *
 * Das Image liesse sich schneller mit
 * createGraphics().drawImage(...) kopieren.
 * Allerdings hat sich gezeigt,
 * dass dabei die Transparenz verloren geht,
 * wenn das Ziel-Image ein IndexColorModel hat,
 * auch wenn dieses transparente Farben enthaelt.
 * Ausserdem wird u.U. auch gerastet, was nicht immer gewollt ist.
 * Aus diesem Grund wird hier Pixel fuer Pixel kopiert.
 */

package jkcemu.image;

import java.awt.Window;
import java.awt.image.BufferedImage;
import jkcemu.Main;
import jkcemu.base.CancelableProgressDlg;


public class ImageCopier
		implements CancelableProgressDlg.Progressable, Runnable
{
  private BufferedImage         srcImg;
  private BufferedImage         dstImg;
  private int                   wImg;
  private int                   hImg;
  private volatile int          progressValue;
  private CancelableProgressDlg dlg;


  public static boolean work(
			    Window        owner,
			    String        title,
			    BufferedImage srcImg,
			    BufferedImage dstImg )
  {
    ImageCopier instance = new ImageCopier( srcImg, dstImg );
    instance.dlg         = new CancelableProgressDlg(
						owner,
						title,
						instance );
    (new Thread(
		Main.getThreadGroup(),
		instance,
		"JKCEMU image copier" )).start();
    instance.dlg.setVisible( true );
    return !instance.dlg.wasCancelled();
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
      for( int y = 0; y < this.hImg; y++ ) {
	for( int x = 0; x < this.wImg; x++ ) {
	  if( this.dlg.wasCancelled() ) {
	    break;
	  }
	  this.dstImg.setRGB( x, y, this.srcImg.getRGB( x, y ) );
	  this.progressValue++;
	}
      }
    }
    finally {
      dlg.fireProgressFinished();
    }
  }


	/* --- Konstruktor --- */

  private ImageCopier( BufferedImage srcImg, BufferedImage dstImg )
  {
    this.srcImg        = srcImg;
    this.dstImg        = dstImg;
    this.wImg          = Math.min( srcImg.getWidth(), dstImg.getWidth() );
    this.hImg          = Math.min( srcImg.getHeight(), dstImg.getHeight() );
    this.progressValue = 0;
    this.dlg           = null;
  }
}
