/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Anzeige eines Bildes
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.*;


public class ImageCard extends Component
{
  private BufferedImage image;


  public ImageCard()
  {
    this.image = null;
  }


  public void setImage( BufferedImage image )
  {
    this.image = image;
    repaint();
  }


	/* --- ueberschriebene Methoden --- */

  public void paint( Graphics g )
  {
    if( this.image != null ) {
      int w    = getWidth();
      int h    = getHeight();
      int wImg = this.image.getWidth();
      int hImg = this.image.getHeight();
      if( (w > 0) && (h > 0) && (wImg > 0) && (hImg > 0) ) {
	if( (wImg > w) || (hImg > h) ) {
	  double scale = Math.min(
				(double) w / (double) wImg,
				(double) h / (double) hImg );
	  wImg = (int) Math.round( (double) wImg * scale );
	  hImg = (int) Math.round( (double) hImg * scale );
	  if( (wImg > 0) && (hImg > 0) ) {
	    g.drawImage( image, 0, 0, wImg, hImg, this );
	  }
	} else {
	  g.drawImage( image, 0, 0, this );
	}
      }
    }
  }
}

