/*
 * (c) 2009-2012 Jens Mueller
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
  private static ChessboardFrm instance = null;

  private JMenuItem     mnuClose;
  private JMenuItem     mnuCopy;
  private JMenuItem     mnuSwap;
  private JMenuItem     mnuHelpContent;
  private ChessboardFld chessboardFld;


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  public static void open( EmuThread emuThread )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new ChessboardFrm( emuThread );
    }
    instance.toFront();
    instance.setVisible( true );
  }


  public static void repaintChessboard()
  {
    if( instance != null )
      instance.chessboardFld.repaint();
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
	HelpFrm.open( "/help/chessboard.htm" );
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ChessboardFrm( EmuThread emuThread )
  {
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

    this.chessboardFld = new ChessboardFld( emuThread );
    add( this.chessboardFld, BorderLayout.CENTER );


    // Fenstergroesse
    setLocationByPlatform( true );
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
    }
    setResizable( false );
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

