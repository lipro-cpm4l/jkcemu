/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anzeige eines Schachbretts
 */

package jkcemu.base;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.image.ImgSelection;


public class ChessboardFrm extends BasicFrm
{
  private ScreenFrm     screenFrm;
  private JMenuItem     mnuClose;
  private JMenuItem     mnuCopy;
  private JMenuItem     mnuSwap;
  private JMenuItem     mnuHelpContent;
  private ChessboardFld chessboardFld;


  public ChessboardFrm( ScreenFrm screenFrm )
  {
    this.screenFrm = screenFrm;
    setTitle( "JKCEMU Schachbrett" );
    Main.updIcon( this );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );

    this.mnuCopy = createJMenuItem( "Schachbrett kopieren" );
    mnuEdit.add( this.mnuCopy );


    // Menu Ansicht
    JMenu mnuView = new JMenu( "Ansicht" );
    mnuView.setMnemonic( KeyEvent.VK_A );

    this.mnuSwap = createJMenuItem( "Seite wechseln" );
    mnuView.add( this.mnuSwap );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuEdit );
    mnuBar.add( mnuView );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


    // Fensterinhalt
    setLayout( new BorderLayout() );

    this.chessboardFld = new ChessboardFld( screenFrm.getEmuThread() );
    add( this.chessboardFld, BorderLayout.CENTER );


    // Fenstergroesse
    setLocationByPlatform( true );
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
    }
    setResizable( false );
  }


  public void repaintChessboard()
  {
    this.chessboardFld.repaint();
  }


	/* --- ueberschriebene Methoden --- */

  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.mnuClose ) {
	rv = true;
	doClose();
      }
      else if( src == this.mnuCopy ) {
	rv = true;
	doCopy();
      }
      else if( src == this.mnuSwap ) {
	rv = true;
	this.chessboardFld.swap();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	this.screenFrm.showHelp( "/help/chessboard.htm" );
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void doCopy()
  {
    try {
      Toolkit tk = getToolkit();
      if( tk != null ) {
	Clipboard clipboard = tk.getSystemClipboard();
	if( clipboard != null ) {
	  ImgSelection ims = new ImgSelection(
					this.chessboardFld.createImage() );
	  clipboard.setContents( ims, ims );
	}
      }
    }
    catch( IllegalStateException ex ) {}
  }
}

