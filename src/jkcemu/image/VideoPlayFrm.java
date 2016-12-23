/*
 * (c) 2010-2016 Jens Mueller
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
import java.lang.*;
import java.util.EventObject;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import jkcemu.Main;
import jkcemu.base.BaseFrm;


public class VideoPlayFrm extends BaseFrm
{
  private JMenuItem mnuClose;
  private JLabel    labelPlayer;


  public VideoPlayFrm()
  {
    setTitle( "JKCEMU Videoplayer" );
    Main.updIcon( this );


    // Menu
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );

    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    setJMenuBar( mnuBar );


    // Fensterinhalt
    setLayout( new BorderLayout( 5, 5 ) );

    this.labelPlayer = new JLabel();
    this.labelPlayer.setHorizontalAlignment( JLabel.CENTER );
    this.labelPlayer.setVerticalAlignment( JLabel.CENTER );
    add( new JScrollPane( this.labelPlayer ), BorderLayout.CENTER );


    // sonstiges
    if( !applySettings( Main.getProperties(), true ) ) {
      setSize( 400, 300 );
      setScreenCentered();
    }
    setResizable( true );
  }


  public void setFile( File file )
  {
    boolean done = false;
    if( file != null ) {
      try {
	Toolkit tk = getToolkit();
	if( tk != null ) {
	  Image image = tk.createImage( file.getPath() );
	  if( image != null ) {
	    this.labelPlayer.setText( "" );
	    this.labelPlayer.setIcon( new ImageIcon( image ) );
	    done = true;
	  }
	}
      }
      catch( Exception ex ) {}
    }
    if( !done ) {
      this.labelPlayer.setText( "Video kann nicht wiedergegeben werden." );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.mnuClose ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }
}
