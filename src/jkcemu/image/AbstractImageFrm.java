/*
 * (c) 2008-2014 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anzeige von Bildern
 */

package jkcemu.image;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class AbstractImageFrm extends BasicFrm implements ComponentListener
{
  protected Clipboard   clipboard;
  protected File        file;
  protected Image       image;
  protected ImgFld      imgFld;
  protected JScrollPane scrollPane;

  private JComboBox<String> comboScale;
  private boolean           scaleEnabled;


  protected AbstractImageFrm()
  {
    this.clipboard    = null;
    this.file         = null;
    this.image        = null;
    this.comboScale   = null;
    this.scaleEnabled = true;
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
    this.comboScale.setToolTipText( "Skalierungsfaktor" );
    this.comboScale.addActionListener( this );
    return this.comboScale;
  }


  protected void doSaveAs()
  {
    if( this.image != null ) {
      BufferedImage   imgToSave = null;
      ImgFld.Rotation rotation  = this.imgFld.getRotation();
      if( rotation == ImgFld.Rotation.NONE ) {
	imgToSave = getBufferedImage();
      } else {
	int w       = this.image.getWidth( this );
	int h       = this.image.getHeight( this );
	int imgType = BufferedImage.TYPE_CUSTOM;
	if( this.image instanceof BufferedImage ) {
	  imgType = ((BufferedImage) this.image).getType();
	  if( imgType == BufferedImage.TYPE_CUSTOM ) {
	    ColorModel cm = ((BufferedImage) this.image).getColorModel();
	    if( cm != null ) {
	      if( !cm.hasAlpha() ) {
		imgType = BufferedImage.TYPE_INT_RGB;
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
	  }
	  else if( option == JOptionPane.NO_OPTION ) {
	    imgToSave = getBufferedImage();
	  }
	} else {
	  imgToSave = getBufferedImage();
	}
      }
      if( imgToSave != null ) {
	File file = ImgSaver.saveImageAs(
			this,
			imgToSave,
			this.file != null ? this.file.getName() : null );
	if( file != null ) {
	  this.file = file;
	}
      }
    }
  }


  protected void doImgCopy()
  {
    if( (this.clipboard != null) && (this.image != null) ) {
      try {
	ImgSelection ims = new ImgSelection( this.image );
	this.clipboard.setContents( ims, ims );
      }
      catch( IllegalStateException ex ) {}
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


  protected void doScale()
  {
    if( this.scaleEnabled && (this.comboScale != null)) {
      Object o = this.comboScale.getSelectedItem();
      if( o != null ) {
	String s = o.toString();
	if( s != null ) {
	  int pos = s.indexOf( '%' );
	  if( pos >= 0 ) {
	    s = s.substring( 0, pos );
	  }
	  try {
	    int value = Integer.parseInt( s.trim() );
	    if( value >= 0 ) {
	      this.scrollPane.invalidate();
	      this.imgFld.setScale( (double) value / 100.0 );
	      this.scrollPane.validate();
	      this.scrollPane.repaint();
	      updDisplayScale();
	      updWindowSize();
	    }
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
  }


  protected void updDisplayScale()
  {
    this.scaleEnabled = false;
    this.comboScale.setSelectedItem(
		String.format(
			"%d %%",
			Math.round( this.imgFld.getScale() * 100.0 ) ) );
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    setScaleEnabled();
		  }
		} );
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
	doScale();
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private BufferedImage getBufferedImage()
  {
    BufferedImage rv = null;
    if( this.image != null ) {
      if( this.image instanceof BufferedImage ) {
	rv = (BufferedImage) this.image;
      } else {
	int w = this.image.getWidth( this );
        int h = this.image.getHeight( this );
        if( (w > 0) && (h > 0) ) {
          rv = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
          Graphics g = rv.createGraphics();
          g.drawImage( this.image, 0, 0, this );
          g.dispose();
	  this.image.flush();
	  this.image = rv;
        }
      }
    }
    return rv;
  }


  private void setScaleEnabled()
  {
    this.scaleEnabled = true;
  }
}

