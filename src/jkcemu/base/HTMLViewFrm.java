/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zur Anzeige von HTML
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.print.*;


public class HTMLViewFrm extends BasicFrm implements
						CaretListener,
						Printable
{
  protected ScreenFrm   screenFrm;
  protected JEditorPane editorPane;
  protected JScrollPane scrollPane;

  private JMenuItem mnuFilePrintOptions = null;
  private JMenuItem mnuFilePrint        = null;
  private JMenuItem mnuFileClose        = null;
  private JMenuItem mnuEditCopy         = null;
  private JMenuItem mnuEditSelectAll    = null;
  private JMenuItem mnuHelpContent      = null;


  protected HTMLViewFrm( ScreenFrm screenFrm )
  {
    this.screenFrm = screenFrm;
    Main.updIcon( this );
  }


  protected void createMenuBar(
			JMenu  mnuFile,
			JMenu  mnuEtc,
			String helpResource )
  {
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );

    // Menu Datei
    if( mnuFile == null ) {
      mnuFile = new JMenu( "Datei" );
      mnuFile.setMnemonic( KeyEvent.VK_D );
    }
    mnuBar.add( mnuFile );

    this.mnuFilePrintOptions = createJMenuItem( "Druckoptionen..." );
    mnuFile.add( this.mnuFilePrintOptions );

    this.mnuFilePrint = createJMenuItem(
		"Drucken...",
		KeyStroke.getKeyStroke( KeyEvent.VK_P, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFilePrint );
    mnuFile.addSeparator();

    this.mnuFileClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuFileClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuEditCopy = createJMenuItem(
		"Kopieren",
		KeyStroke.getKeyStroke( KeyEvent.VK_C, Event.CTRL_MASK ) );
    this.mnuEditCopy.setEnabled( false );
    mnuEdit.add( this.mnuEditCopy );
    mnuEdit.addSeparator();

    this.mnuEditSelectAll = createJMenuItem( "Alles ausw\u00E4hlen" );
    mnuEdit.add( this.mnuEditSelectAll );

    // weiteres Menue
    if( mnuEtc != null ) {
      mnuBar.add( mnuEtc );
    }

    // Menu Hilfe
    if( helpResource != null ) {
      JMenu mnuHelp = new JMenu( "?" );
      mnuBar.add( mnuHelp );

      this.mnuHelpContent = createJMenuItem( "Hilfe..." );
      this.mnuHelpContent.setActionCommand( helpResource );
      mnuHelp.add( this.mnuHelpContent );
    }
  }


  protected void createEditorPane( Object constraints )
  {
    this.editorPane = new JEditorPane();
    this.editorPane.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.editorPane.setEditable( false );
    this.editorPane.addCaretListener( this );

    this.scrollPane = new JScrollPane( this.editorPane );
    add( this.scrollPane, constraints );
  }


  protected void doPrint()
  {
    PrintUtil.doPrint( this, this, getTitle() );
  }


  public void setHTMLText( String text )
  {
    this.editorPane.setContentType( "text/html" );
    this.editorPane.setText( text );
    try {
      this.editorPane.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }


	/* --- CaretListener --- */

  @Override
  public void caretUpdate( CaretEvent e )
  {
    int a = this.editorPane.getSelectionStart();
    int b = this.editorPane.getSelectionEnd();
    this.mnuEditCopy.setEnabled( (a >= 0) && (a < b) );
  }


	/* --- Printable --- */

  @Override
  public int print(
		Graphics   g,
		PageFormat pf,
		int        pageNum ) throws PrinterException
  {
    double x = pf.getImageableX();
    double y = pf.getImageableY();
    double w = pf.getImageableWidth();
    double h = pf.getImageableHeight();

    if( Main.getPrintPageNum() ) {
      h -= 20;
    }

    if( (w < 1.0) || (h < 1.0) ) {
      throw new PrinterException(
		"Die Seite hat keinen bedruckbaren Bereich,\n"
			+ "da die R\u00E4nder zu gro\u00DF sind." );
    }


    // Skalierungsfaktort berechnen, damit die Breite stimmt
    double scale = w / this.editorPane.getWidth();

    // eigentliches Drucken
    int rv    = NO_SUCH_PAGE;
    int yOffs = pageNum * (int) Math.round( h / scale );
    if( (yOffs >= 0) && (yOffs < this.editorPane.getHeight()) ) {

      // Fusszeile erzeugen, bevor skaliert wird
      if( Main.getPrintPageNum() ) {
	PrintUtil.printCenteredPageNum( g, pf, 10, pageNum + 1 );
      }

      // Skalieren und zu druckender Bereich markieren
      if( (scale < 1.0) && (g instanceof Graphics2D) ) {
	((Graphics2D) g).scale( scale, scale );

	x /= scale;
	y /= scale;
	w /= scale;
	h /= scale;
      }

      // Seite drucken
      int xInt = (int) Math.round( x );
      int yInt = (int) Math.round( y );
      int wInt = (int) Math.round( w );
      int hInt = (int) Math.round( h );

      g.clipRect( xInt, yInt, wInt + 1, hInt + 1 );
      g.translate( xInt, (yInt - yOffs) );
      this.editorPane.print( g );

      rv = PAGE_EXISTS;
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.mnuFilePrintOptions ) {
	rv = true;
	PrintOptionsDlg.showPrintOptionsDlg( this, false, false );
      }
      else if( src == this.mnuFilePrint ) {
	rv = true;
	doPrint();
      }
      else if( src == this.mnuFileClose ) {
	rv = true;
	doClose();
      }
      else if( src == this.mnuEditCopy ) {
	rv = true;
	this.editorPane.copy();
      }
      else if( src == this.mnuEditSelectAll ) {
	rv = true;
	this.editorPane.selectAll();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	this.screenFrm.showHelp( this.mnuHelpContent.getActionCommand() );
      }
    }
    return rv;
  }


  @Override
  public void windowClosed( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.screenFrm.childFrameClosed( this );
  }
}

