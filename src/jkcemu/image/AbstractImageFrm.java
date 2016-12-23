/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anzeige von Bildern
 */

package jkcemu.image;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import jkcemu.Main;
import jkcemu.base.BaseFrm;


public abstract class AbstractImageFrm
				extends BaseFrm
				implements ComponentListener
{
  protected Clipboard   clipboard;
  protected ImgFld      imgFld;
  protected JScrollPane scrollPane;

  private JComboBox<String> comboScale;


  protected AbstractImageFrm()
  {
    this.clipboard  = null;
    this.comboScale = null;
    Main.updIcon( this );

    Toolkit tk = getToolkit();
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
    }


    // Fensterinhalt
    setLayout( new BorderLayout( 0, 0 ) );


    /*
     * Bildanzeige
     * Anfangsgroesse im Verhaeltnis 4:3, aber so,
     * dass ein Schnappschuss der Bildschirmausgabe hineinpasst.
     */
    this.imgFld = new ImgFld( 342, 256 );
    this.scrollPane = new JScrollPane(
				this.imgFld,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
    add( this.scrollPane, BorderLayout.CENTER );


    // sonstiges
    addComponentListener( this );
  }


  protected JComboBox<String> createScaleComboBox(
					int    defaultScale,
					int... scaleFactors )
  {
    String defaultItem = null;
    this.comboScale    = new JComboBox<>();
    this.comboScale.setEditable( true );
    for( int f : scaleFactors ) {
      String s = String.format( "%d %%", f );
      this.comboScale.addItem( s );
      if( f == defaultScale ) {
	defaultItem = s;
      }
    }
    if( defaultItem != null ) {
      this.comboScale.setSelectedItem( defaultItem );
    }
    this.comboScale.setToolTipText( "Skalierung der Anzeige" );
    this.comboScale.addActionListener( this );
    return this.comboScale;
  }


  protected void doCopy()
  {
    if( this.clipboard != null ) {
      Image image = getImage();
      if( image != null ) {
	try {
	  ImgSelection ims = new ImgSelection( image );
	  this.clipboard.setContents( ims, ims );
	}
	catch( IllegalStateException ex ) {}
      }
    }
  }


  protected void doRotateLeft()
  {
    this.scrollPane.invalidate();
    switch( this.imgFld.getRotation() ) {
      case NONE:
	this.imgFld.setRotation( ImgFld.Rotation.LEFT );
	break;

      case LEFT:
	this.imgFld.setRotation( ImgFld.Rotation.DOWN );
	break;

      case RIGHT:
	this.imgFld.setRotation( ImgFld.Rotation.NONE );
	break;

      case DOWN:
	this.imgFld.setRotation( ImgFld.Rotation.RIGHT );
	break;
    }
    this.scrollPane.validate();
    this.scrollPane.repaint();
    updWindowSize();
  }


  protected void doRotateRight()
  {
    this.scrollPane.invalidate();
    switch( this.imgFld.getRotation() ) {
      case NONE:
	this.imgFld.setRotation( ImgFld.Rotation.RIGHT );
	break;

      case LEFT:
	this.imgFld.setRotation( ImgFld.Rotation.NONE );
	break;

      case RIGHT:
	this.imgFld.setRotation( ImgFld.Rotation.DOWN );
	break;

      case DOWN:
	this.imgFld.setRotation( ImgFld.Rotation.LEFT );
	break;
    }
    this.scrollPane.validate();
    this.scrollPane.repaint();
    updWindowSize();
  }


  protected void doScaleView()
  {
    double scale = getViewScale();
    if( scale > 0.0 ) {
      this.scrollPane.invalidate();
      this.imgFld.setScale( scale );
      this.scrollPane.validate();
      this.scrollPane.repaint();
      updViewScaleFld();
      updWindowSize();
    }
  }


  protected abstract BufferedImage getImage();


  protected double getViewScale()
  {
    double rv = -1.0;
    if( this.comboScale != null ) {
      Object o = this.comboScale.getSelectedItem();
      if( o != null ) {
	String s = o.toString();
	if( s != null ) {
	  int pos = s.indexOf( '%' );
	  if( pos >= 0 ) {
	    s = s.substring( 0, pos );
	  }
	  try {
	    int percent = Integer.parseInt( s.trim() );
	    if( percent > 0 ) {
	      rv = (double) percent / 100.0;
	    }
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
    return rv;
  }


  protected File saveAs( File presetFile )
  {
    File          rv    = null;
    BufferedImage image = getImage();
    if( image != null ) {
      ImgFld.Rotation rotation = this.imgFld.getRotation();
      if( rotation != ImgFld.Rotation.NONE ) {
	int w       = image.getWidth();
	int h       = image.getHeight();
	int imgType = BufferedImage.TYPE_CUSTOM;
	if( image instanceof BufferedImage ) {
	  imgType = image.getType();
	  if( imgType == BufferedImage.TYPE_CUSTOM ) {
	    ColorModel cm = image.getColorModel();
	    if( cm != null ) {
	      if( !cm.hasAlpha() ) {
		imgType = BufferedImage.TYPE_3BYTE_BGR;
	      }
	    }
	  }
	}
	if( imgType == BufferedImage.TYPE_CUSTOM ) {
	  imgType = BufferedImage.TYPE_INT_ARGB;
	}
	if( (w > 0) && (h > 0) ) {
	  int option = JOptionPane.showConfirmDialog(
				this,
				"Soll das Bild gedreht gespeichert werden,\n"
					+ "so wie Sie es gerade sehen?",
				"Bild gedreht",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE );
	  if( option == JOptionPane.YES_OPTION ) {
	    BufferedImage imgToSave = null;
	    if( (rotation == ImgFld.Rotation.LEFT)
		|| (rotation == ImgFld.Rotation.RIGHT) )
	    {
	      imgToSave = new BufferedImage( h, w, imgType );
	    } else {
	      imgToSave = new BufferedImage( w, h, imgType );
	    }
	    Graphics g = imgToSave.createGraphics();
	    this.imgFld.drawImage( g, 0, 0, w, h );
	    g.dispose();
	    image = imgToSave;
	  }
	}
      }
      rv = ImgSaver.saveImageAs( this, image, presetFile );
    }
    return rv;
  }


  protected void updViewScaleFld()
  {
    this.comboScale.removeActionListener( this );
    this.comboScale.setSelectedItem(
		String.format(
			"%d %%",
			Math.round( this.imgFld.getScale() * 100.0 ) ) );
    this.comboScale.addActionListener( this );
  }


  protected void updWindowSize()
  {
    // leer
  }


	/* --- ComponentListener --- */

  @Override
  public void componentHidden( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentMoved( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentResized( ComponentEvent e )
  {
    JViewport vp = this.scrollPane.getViewport();
    if( vp != null )
      this.imgFld.setViewportSize( vp.getExtentSize() );
  }


  @Override
  public void componentShown( ComponentEvent e )
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
    Object src = e.getSource();
      if( src == this.comboScale ) {
	rv = true;
	doScaleView();
      }
    }
    return rv;
  }
}
