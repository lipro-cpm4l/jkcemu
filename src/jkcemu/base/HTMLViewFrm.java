/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zur Anzeige von HTML
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.EventObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import jkcemu.Main;
import jkcemu.base.PopupMenuOwner;
import jkcemu.print.PrintOptionsDlg;
import jkcemu.print.PrintUtil;
import jkcemu.text.TextFinder;


public class HTMLViewFrm extends BaseFrm implements
						CaretListener,
						PopupMenuOwner,
						Printable
{
  protected JEditorPane editorPane;
  protected JScrollPane scrollPane;

  private int        findPos             = 0;
  private Pattern    findPattern         = null;
  private String     findTitle           = null;
  private String     findTitle2          = null;
  private JMenuItem  mnuFilePrintOptions = null;
  private JMenuItem  mnuFilePrint        = null;
  private JMenuItem  mnuFileClose        = null;
  private JMenuItem  mnuEditCopy         = null;
  private JMenuItem  mnuEditFind         = null;
  private JMenuItem  mnuEditFindNext     = null;
  private JMenuItem  mnuEditFindPrev     = null;
  private JMenuItem  mnuEditFind2        = null;
  private JMenuItem  mnuEditSelectAll    = null;
  private JMenuItem  mnuHelpContent      = null;
  private JMenuItem  popupCopy           = null;
  private JMenuItem  popupFind           = null;
  private JMenuItem  popupFindNext       = null;
  private JMenuItem  popupFindPrev       = null;
  private JMenuItem  popupFind2          = null;
  private JMenuItem  popupSelectAll      = null;
  private JPopupMenu popupMnu            = null;


  protected HTMLViewFrm()
  {
    // leer
  }


  protected void createMenuBar(
			JMenu  mnuFile,
			String findTitle,
			String findTitle2,
			JMenu  mnuEtc,
			String helpMenuItem,
			String helpResource )
  {
    this.findTitle  = findTitle;
    this.findTitle2 = findTitle2;

    // Menu Datei
    if( mnuFile == null ) {
      mnuFile = createMenuFile();
    }

    this.mnuFilePrintOptions = createMenuItemOpenPrintOptions();
    mnuFile.add( this.mnuFilePrintOptions );

    this.mnuFilePrint = createMenuItemOpenPrint( true );
    mnuFile.add( this.mnuFilePrint );
    mnuFile.addSeparator();

    this.mnuFileClose = createMenuItemClose();
    mnuFile.add( this.mnuFileClose );


    // Menu Bearbeiten
    JMenu mnuEdit = createMenuEdit();

    this.mnuEditCopy = createMenuItemCopy( true );
    this.mnuEditCopy.setEnabled( false );
    mnuEdit.add( this.mnuEditCopy );
    mnuEdit.addSeparator();

    if( findTitle != null ) {
      this.mnuEditFind = createMenuItemWithStandardAccelerator(
							findTitle,
							KeyEvent.VK_F );
      this.mnuEditFind.setEnabled( false );
      mnuEdit.add( this.mnuEditFind );

      this.mnuEditFindNext = createMenuItemFindNext( true );
      this.mnuEditFindNext.setEnabled( false );
      mnuEdit.add( this.mnuEditFindNext );

      this.mnuEditFindPrev = createMenuItemFindPrev( true );
      this.mnuEditFindPrev.setEnabled( false );
      mnuEdit.add( this.mnuEditFindPrev );
      mnuEdit.addSeparator();
    }

    if( findTitle2 != null ) {
      this.mnuEditFind2 = createMenuItemWithStandardAccelerator(
							findTitle2,
							KeyEvent.VK_F,
							true );
      mnuEdit.add( this.mnuEditFind2 );
      mnuEdit.addSeparator();
    }

    this.mnuEditSelectAll = createMenuItemSelectAll( true );
    this.mnuEditSelectAll.setEnabled( false );
    mnuEdit.add( this.mnuEditSelectAll );


    // Menu Hilfe
    JMenu mnuHelp = null;
    if( (helpMenuItem != null) && (helpResource != null) ) {
      mnuHelp = createMenuHelp();

      this.mnuHelpContent = createMenuItem( helpMenuItem, helpResource );
      mnuHelp.add( this.mnuHelpContent );
    }


    // weiteres Menue und MenuBar
    setJMenuBar( GUIFactory.createMenuBar(
				mnuFile, mnuEdit, mnuEtc, mnuHelp ) );


    // Kontext-Menu
    this.popupMnu = GUIFactory.createPopupMenu();

    this.popupCopy = createMenuItem( EmuUtil.TEXT_COPY );
    this.popupCopy.setEnabled( false );
    this.popupMnu.add( this.popupCopy );
    this.popupMnu.addSeparator();

    if( findTitle != null ) {
      this.popupFind = createMenuItem( findTitle );
      this.popupFind.setEnabled( false );
      this.popupMnu.add( this.popupFind );

      this.popupFindNext = createMenuItemFindNext( false );
      this.popupFindNext.setEnabled( false );
      this.popupMnu.add( this.popupFindNext );

      this.popupFindPrev = createMenuItemFindPrev( false );
      this.popupFindPrev.setEnabled( false );
      this.popupMnu.add( this.popupFindPrev );
      this.popupMnu.addSeparator();
    }
    if( findTitle2 != null ) {
      this.popupFind2 = createMenuItem( findTitle2 );
      this.popupMnu.add( this.popupFind2 );
      this.popupMnu.addSeparator();
    }
    this.popupSelectAll = createMenuItemSelectAll( true );
    this.popupSelectAll.setEnabled( false );
    this.popupMnu.add( this.popupSelectAll );
  }


  protected void createEditorPane( Object constraints )
  {
    this.editorPane = GUIFactory.createEditorPane();
    this.editorPane.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.editorPane.setEditable( false );
    this.editorPane.addCaretListener( this );
    this.editorPane.addMouseListener( this );

    this.scrollPane = GUIFactory.createScrollPane( this.editorPane );
    if( constraints != null ) {
      add( this.scrollPane, constraints );
    } else {
      add( this.scrollPane );
    }
  }


  protected void doFind()
  {
    String initialText = this.editorPane.getSelectedText();
    if( initialText != null ) {
      if( initialText.isEmpty() ) {
	initialText = null;
      }
    }
    if( initialText == null ) {
      if( this.findPattern != null ) {
	initialText = this.findPattern.toString();
      }
    }
    String title = null;
    if( this.mnuEditFind != null ) {
      title = this.mnuEditFind.getText();
    }
    if( title != null ) {
      if( title.endsWith( "..." ) ) {
	title = title.substring( 0, title.length() - 3 );
      }
    } else {
      title = EmuUtil.TEXT_FIND;
    }
    doFind( showFindDlg( title, initialText ), true );
  }


  protected void doFind( String findText, boolean interactive )
  {
    if( findText != null ) {
      try {
	this.findPattern = Pattern.compile(
				findText,
				Pattern.LITERAL
					| Pattern.CASE_INSENSITIVE
					| Pattern.UNICODE_CASE );
	this.findPos = 0;
	if( this.mnuEditFind != null ) {
	  this.mnuEditFindNext.setEnabled( true );
	  this.mnuEditFindPrev.setEnabled( true );
	  this.popupFindNext.setEnabled( true );
	  this.popupFindPrev.setEnabled( true );
	}
	doFindNext( false, interactive );
      }
      catch( PatternSyntaxException ex ) {}
    }
  }


  protected void doFind2()
  {
    // leer
  }


  protected String showFindDlg( String title, String initialText )
  {
    String reply = null;

    final String[] options = { EmuUtil.TEXT_FIND, EmuUtil.TEXT_CANCEL };
    JOptionPane pane = new JOptionPane(
				EmuUtil.LABEL_SEARCH_FOR,
				JOptionPane.PLAIN_MESSAGE );
    pane.setOptions( options );
    pane.setInitialValue( options[ 0 ] );
    pane.setWantsInput( true );
    if( initialText != null ) {
      pane.setInitialSelectionValue( initialText );
    }
    pane.createDialog( this, title ).setVisible( true );
    if( pane.getValue() == options[ 0 ] ) {
      Object o = pane.getInputValue();
      if( o != null ) {
	String s = o.toString();
	if( s != null ) {
	  if( !s.isEmpty() ) {
	    reply = s;
	  }
	}
      }
    }
    return reply;
  }


  protected void doPrint()
  {
    PrintUtil.doPrint( this, this, getTitle() );
  }


  protected void doSelectAll()
  {
    final JEditorPane editorPane = this.editorPane;
    editorPane.requestFocus();
    EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    editorPane.selectAll();
		  }
		} );
  }


  protected void setContentActionFieldsEnabled( boolean state )
  {
    if( this.mnuEditFind != null ) {
      this.mnuEditFind.setEnabled( state );
      this.popupFind.setEnabled( state );
      if( !state ) {
	this.mnuEditFindNext.setEnabled( false );
	this.popupFindNext.setEnabled( false );
	this.popupFindPrev.setEnabled( false );
	this.mnuEditFindPrev.setEnabled( false );
      }
    }
    this.mnuEditSelectAll.setEnabled( state );
    this.popupSelectAll.setEnabled( state );
  }


  public void setHTMLText( String text )
  {
    this.editorPane.setContentType( "text/html" );
    this.editorPane.setText( text );
    try {
      this.editorPane.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}

    boolean state = false;
    if( text != null ) {
      state = !text.isEmpty();
    }
    setContentActionFieldsEnabled( state );
  }


	/* --- CaretListener --- */

  @Override
  public void caretUpdate( CaretEvent e )
  {
    int     a     = this.editorPane.getSelectionStart();
    int     b     = this.editorPane.getSelectionEnd();
    boolean state = ((a >= 0) && (a < b));
    this.mnuEditCopy.setEnabled( state );
    this.popupCopy.setEnabled( state );
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.popupMnu;
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
      else if( (src == this.mnuEditCopy)
	       || (src == this.popupCopy) )
      {
	rv = true;
	this.editorPane.copy();
      }
      else if( (src == this.mnuEditFind)
	       || (src == this.popupFind) )
      {
	rv = true;
	doFind();
      }
      else if( (src == this.mnuEditFindNext)
	       || (src == this.popupFindNext) )
      {
	rv = true;
	doFindNext( false, true );
      }
      else if( (src == this.mnuEditFindPrev)
	       || (src == this.popupFindPrev) )
      {
	rv = true;
	doFindNext( true, true );
      }
      else if( (src == this.mnuEditFind2)
	       || (src == this.popupFind2) )
      {
	rv = true;
	doFind2();
      }
      else if( (src == this.mnuEditSelectAll)
	       || (src == this.popupSelectAll) )
      {
	rv = true;
	doSelectAll();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.openPage( this.mnuHelpContent.getActionCommand() );
      }
    }
    return rv;
  }


  @Override
  protected boolean showPopupMenu( MouseEvent e )
  {
    boolean rv = false;
    if( e.getComponent() == this.editorPane ) {
      this.popupMnu.show( this.editorPane, e.getX(), e.getY() );
      rv = true;
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void doFindNext( boolean backward, boolean interactive )
  {
    if( this.findPattern != null ) {
      String   text = null;
      Document doc  = this.editorPane.getDocument();
      if( doc != null ) {
	try {
	  text = doc.getText( 0, doc.getLength() );
	}
	catch( BadLocationException ex ) {}
      }
      if( text == null ) {
	text = "";
      }
      if( this.findPos > text.length() ) {
	this.findPos = 0;
      }
      Matcher matcher = this.findPattern.matcher( text );
      boolean found   = findNext( matcher, backward );
      if( !found && (this.findPos > 0) ) {
	this.findPos = 0;
	found = findNext( matcher, backward );
      }
      if( !found && interactive ) {
	TextFinder.showTextNotFound( this );
      }
    } else {
      doFind();
    }
  }


  private boolean findNext( Matcher matcher, boolean backward )
  {
    boolean rv    = false;
    boolean found = false;
    if( backward ) {
      int curPos   = 0;
      int foundPos = -1;
      while( matcher.find( curPos ) ) {
	int pos = matcher.start();
	if( pos >= (this.findPos - 1) ) {
	  break;
	}
	foundPos = pos;
	curPos   = pos + 1;
      }
      if( foundPos < 0 ) {
	curPos = this.findPos;
	while( matcher.find( curPos ) ) {
	  int pos = matcher.start();
	  if( (foundPos >= 0) && (pos < foundPos) ) {
	    break;
	  }
	  foundPos = pos;
	  curPos   = pos + 1;
	}
      }
      if( foundPos >= 0 ) {
	found = matcher.find( foundPos );
      }
    } else {
      found = matcher.find( this.findPos );
    }
    if( found ) {
      final int a = matcher.start();
      final int b = matcher.end();
      if( (a >= 0) && (a < b) ) {
	final JEditorPane editorPane = this.editorPane;
        editorPane.requestFocus();
	EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    try {
		      editorPane.setCaretPosition( a );
		      editorPane.moveCaretPosition( b );
		    }
		    catch( IllegalArgumentException ex ) {}
		  }
		} );
	this.findPos = a + 1;
	rv           = true;
      }
    }
    return rv;
  }
}
