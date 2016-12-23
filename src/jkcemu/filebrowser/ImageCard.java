/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Anzeige eines Bildes
 */

package jkcemu.filebrowser;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.lang.*;


public class ImageCard extends Component
{
  private Image image;


  public ImageCard()
  {
    this.image = null;
  }


  public void setImage( Image image )
  {
    if( this.image != null ) {
      if( image != this.image ) {
	this.image.flush();
      }
    }
    this.image = image;
    repaint();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void paint( Graphics g )
  {
    if( this.image != null ) {
      int w    = getWidth();
      int h    = getHeight();
      int wImg = this.image.getWidth( this );
      int hImg = this.image.getHeight( this );
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
