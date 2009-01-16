/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Darstellung des Bildschirminhaltes
 */

package jkcemu.base;

import java.awt.*;
import java.awt.image.*;
import java.lang.*;
import javax.swing.JComponent;


public class ScreenFld extends JComponent
{
  public static final int DEFAULT_MARGIN = 20;

  private ScreenFrm screenFrm;
  private EmuSys    emuSys;
  private int       lastColorIdx;
  private int       margin;
  private int       screenScale;


  public ScreenFld( ScreenFrm screenFrm )
  {
    this.screenFrm    = screenFrm;
    this.emuSys       = screenFrm.getEmuThread().getEmuSys();
    this.lastColorIdx = -1;
    this.margin       = DEFAULT_MARGIN;
    this.screenScale  = 1;
    updPreferredSize();
  }


  public BufferedImage createBufferedImage()
  {
    BufferedImage img = null;

    int w = getWidth();
    int h = getHeight();
    if( (w > 0) && (h > 0) ) {
      int nColors = this.emuSys.getColorCount();
      int value   = nColors - 1;
      int nBits   = 0;
      while( value > 0 ) {
	value >>= 1;
	nBits++;
      }
      byte[] r = new byte[ nColors ];
      byte[] g = new byte[ nColors ];
      byte[] b = new byte[ nColors ];
      for( int i = 0; i < nColors; i++ ) {
	Color color = this.emuSys.getColor( i );
	r[ i ] = (byte) color.getRed();
	g[ i ] = (byte) color.getGreen();
	b[ i ] = (byte) color.getBlue();
      }
      IndexColorModel cm = new IndexColorModel( nBits, nColors, r, g, b );
      img = new BufferedImage( w, h, BufferedImage.TYPE_BYTE_INDEXED, cm );
      Graphics graphics = img.createGraphics();
      paint( graphics, w, h );
      graphics.dispose();
    }
    return img;
  }


  public EmuSys getEmuSys()
  {
    return this.emuSys;
  }


  public int getMargin()
  {
    return this.margin;
  }


  public int getScreenScale()
  {
    return this.screenScale;
  }


  public void setEmuSys( EmuSys emuSys )
  {
    this.emuSys = emuSys;
  }


  public void setMargin( int margin )
  {
    this.margin = margin;
    updPreferredSize();
  }


  public void setScreenScale( int screenScale )
  {
    if( (screenScale > 0) && (screenScale != this.screenScale) ) {
      this.screenScale = screenScale;
      updPreferredSize();
    }
  }


  public void updPreferredSize()
  {
    int margin = this.margin;
    if( margin < 0 ) {
      margin = 0;
    }
    setPreferredSize( new Dimension(
	(2 * margin) + (this.emuSys.getScreenBaseWidth() * this.screenScale),
	(2 * margin) + (this.emuSys.getScreenBaseHeight()
						* this.screenScale) ) );

    Container parent = getParent();
    if( parent != null ) {
      parent.invalidate();
    } else {
      invalidate();
    }
  }


	/* --- ueberschriebene Methoden --- */

  public void paint( Graphics g )
  {
    this.screenFrm.setScreenDirty( false );
    paint( g, getWidth(), getHeight() );
  }


  /*
   * update(...) wird ueberschrieben,
   * da paint(...) die Komponente vollstaendig fuellt
   * und somit das standardmaessige Fuellen mit der Hintegrundfarbe
   * entfallen kann.
   */
  public void update( Graphics g )
  {
    paint( g );
  }


	/* --- private Methoden --- */

  private Point getOffset( int wBase, int hBase )
  {
    int w = getWidth();
    if( w < 1 ) {
      w = wBase * this.screenScale;
    }

    int h = getHeight();
    if( h < 1 ) {
      h = hBase * this.screenScale;
    }

    int xOffset = (w - (wBase * this.screenScale)) / 2;
    if( xOffset < 0 ) {
      xOffset = 0;
    }
    int yOffset = (h - (hBase * this.screenScale)) / 2;
    if( yOffset < 0 ) {
      yOffset = 0;
    }
    return new Point( xOffset, yOffset );
  }


  private void paint( Graphics g, int w, int h )
  {
    int wBase = this.emuSys.getScreenBaseWidth();
    int hBase = this.emuSys.getScreenBaseHeight();

    // Hintergrund
    int bgColorIdx = this.emuSys.getBorderColorIndex();
    g.setColor( this.emuSys.getColor( bgColorIdx ) );
    g.fillRect( 0, 0, w, h );

    // Vordergrund zentrieren
    Point offset = getOffset( wBase, hBase );
    if( (offset.x > 0) && (offset.y > 0) ) {
      g.translate( offset.x, offset.y );
    }

    /*
     * Aus Greunden der Performance werden untereinander liegende
     * Punkte zusammengefasst und als Linie gezeichnet.
     */
    for( int x = 0; x < wBase; x++ ) {
      int lastColorIdx = -1;
      int yColorBeg    = -1;
      for( int y = 0; y < hBase; y++ ) {
	int curColorIdx = this.emuSys.getColorIndex( x, y );
	if( curColorIdx != lastColorIdx ) {
	  if( (lastColorIdx >= 0)
	      && (lastColorIdx != bgColorIdx)
	      && (yColorBeg >= 0) )
	  {
	    g.setColor( this.emuSys.getColor( lastColorIdx ) );
	    g.fillRect(
		x * this.screenScale,
		yColorBeg * this.screenScale,
		this.screenScale,
		(y - yColorBeg) * this.screenScale );
	  }
	  yColorBeg    = y;
	  lastColorIdx = curColorIdx;
	}
      }
      if( (lastColorIdx >= 0)
	  && (lastColorIdx != bgColorIdx)
	  && (yColorBeg >= 0) )
      {
	g.setColor( this.emuSys.getColor( lastColorIdx ) );
	g.fillRect(
		x * this.screenScale,
		yColorBeg * this.screenScale,
		this.screenScale,
		(hBase - yColorBeg) * this.screenScale );
      }
    }
  }
}

