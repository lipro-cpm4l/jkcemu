/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Anzeige eines Bildes
 *
 * Mit Dithering erstellte Bilder sehen haesslich aus,
 * wenn sie mit einem einfachen Skalierungsalgorithmus verkleinert werden.
 * Aus diesem Grund wird hier im Fall einer notwendigen Verkleinerung
 * eine mit dem Smooth-Algorithmus skalierte neue Bildinstanz
 * erzeugt, temporaer behalten und verwendet.
 * Sobald eine andere Skalierung notwendig ist,
 * wird die skalierte temporaere Bildinstanz freigegeben
 * und eine neue Instanz erzeugt.
 */

package jkcemu.tools.filebrowser;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Stack;


public class ImageCard extends Component
{
  private Stack<Image> forFlushing;
  private Image        orgImage;
  private Image        scaledImage;
  private int          scaledW;
  private int          scaledH;


  public ImageCard()
  {
    this.forFlushing = new Stack<>();
    this.orgImage    = null;
    this.scaledImage = null;
    this.scaledW     = 0;
    this.scaledH     = 0;
  }


  /*
   * Diese Methode wird in einem anderen als den Event-Thread aufgerufen.
   */
  public void setImage( Image image )
  {
    synchronized( this.forFlushing ) {
      if( (this.orgImage != null) && (image != this.orgImage) ) {
	/*
	 * Image.flush() nur im Event-Thread aufrufen,
	 * d.h. in der paint-Methode
	 */
	this.forFlushing.push( this.orgImage );
	if( this.scaledImage != null ) {
	  this.forFlushing.push( this.scaledImage );
	}
	EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    flushImages();
			  }
			} );
      }
      this.orgImage    = image;
      this.scaledImage = null;
    }
    repaint();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void paint( Graphics g )
  {
    Image image = this.orgImage;
    if( image != null ) {
      int   w     = getWidth();
      int   h     = getHeight();
      int   wImg  = image.getWidth( this );
      int   hImg  = image.getHeight( this );
      if( (w > 0) && (h > 0) && (wImg > 0) && (hImg > 0) ) {
	synchronized( this.forFlushing ) {
	  if( (wImg > w) || (hImg > h) ) {
	    double scale = Math.min(
				(double) w / (double) wImg,
				(double) h / (double) hImg );
	  
	    wImg = (int) Math.round( (double) wImg * scale );
	    hImg = (int) Math.round( (double) hImg * scale );
	    if( this.scaledImage != null ) {
	      if( (wImg != this.scaledW) || (hImg != this.scaledH) ) {
		this.scaledImage.flush();
		this.scaledImage = null;
	      }
	    }
	    if( (this.scaledImage == null) && (wImg > 0) && (hImg > 0) ) {
	      this.scaledW     = wImg;
	      this.scaledH     = hImg;
	      this.scaledImage = image.getScaledInstance(
						wImg,
						hImg,
						Image.SCALE_SMOOTH );
	    }
	    if( this.scaledImage != null ) {
	      image = this.scaledImage;
	    }
	  }
	}
	g.drawImage( image, 0, 0, this );
      }
    }
  }


	/* --- private Methoden --- */

  private void flushImages()
  {
    synchronized( this.forFlushing ) {
      while( !this.forFlushing.empty() ) {
	this.forFlushing.pop().flush();
      }
    }
  }
}
