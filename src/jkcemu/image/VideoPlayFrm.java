/*
 * (c) 2010-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Wiedergabe eines aufgenommenen Bildschirmvideos (animierte GIF-Datei)
 */

package jkcemu.image;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.EventObject;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import jkcemu.Main;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class VideoPlayFrm extends BaseFrm
{
  private File      file;
  private Image     image;
  private JMenuItem mnuClose;
  private JMenuItem mnuPlayAgain;
  private JLabel    labelPlayer;


  public VideoPlayFrm()
  {
    this.file  = null;
    this.image = null;
    setTitle( "JKCEMU Videoplayer" );


    // Menu
    JMenu mnuFile = createMenuFile();

    this.mnuPlayAgain = createMenuItem( "Video erneut wiedergeben" );
    this.mnuPlayAgain.setEnabled( false );
    mnuFile.add( this.mnuPlayAgain );
    mnuFile.addSeparator();

    this.mnuClose = createMenuItemClose();
    mnuFile.add( this.mnuClose );

    setJMenuBar( GUIFactory.createMenuBar( mnuFile ) );


    // Fensterinhalt
    setLayout( new BorderLayout( 5, 5 ) );

    this.labelPlayer = GUIFactory.createLabel();
    this.labelPlayer.setHorizontalAlignment( JLabel.CENTER );
    this.labelPlayer.setVerticalAlignment( JLabel.CENTER );
    add(
	GUIFactory.createScrollPane( this.labelPlayer ),
	BorderLayout.CENTER );


    // sonstiges
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      setSize( 400, 300 );
      setScreenCentered();
    }
  }


  public void setFile( File file )
  {
    boolean done = false;
    if( file != null ) {
      if( file.canRead() ) {
	try {
	  Toolkit tk = EmuUtil.getToolkit( this );
	  if( tk != null ) {
	    Image image = tk.createImage( file.getPath() );
	    if( image != null ) {
	      this.labelPlayer.setText( "" );
	      this.labelPlayer.setIcon( new ImageIcon( image ) );
	      this.file  = file;
	      this.image = image;
	      this.mnuPlayAgain.setEnabled( true);
	      done = true;
	    }
	  }
	}
	catch( Exception ex ) {}
      }
      if( !done ) {
	this.mnuPlayAgain.setEnabled( false );
	this.labelPlayer.setIcon( null );
	this.labelPlayer.setText(
			"Video kann nicht wiedergegeben werden." );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.mnuPlayAgain ) {
      rv = true;
      if( this.file != null ) {
	setFile( file );
      }
    }
    else if( src == this.mnuClose ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public void setVisible( boolean state )
  {
    if( !state && (this.image != null) ) {
      this.labelPlayer.setIcon( null );
      this.image.flush();
      this.image = null;
    }
    super.setVisible( state );
  }
}
