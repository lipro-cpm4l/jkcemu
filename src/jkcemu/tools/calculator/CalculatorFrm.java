/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Rechner
 */

package jkcemu.tools.calculator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.HelpFrm;
import jkcemu.base.GUIFactory;


public class CalculatorFrm extends BaseFrm implements
						CaretListener,
						DocumentListener,
						FlavorListener,
						FocusListener
{
  public static final String TITLE = Main.APPNAME + " Rechner";

  private static final String DEFAULT_TEXT = "Geben Sie bitte ein Zeichen"
		+ " oder einen numerischen Ausdruck ein!";

  private static final String HELP_PAGE = "/help/tools/calculator.htm";


  private static CalculatorFrm instance = null;

  private ExprParser     parser;
  private Clipboard      clipboard;
  private boolean        clipboardHasText;
  private JTextComponent lastTextFld;
  private JMenuItem      mnuFileClose;
  private JMenuItem      mnuEditCut;
  private JMenuItem      mnuEditCopy;
  private JMenuItem      mnuEditPaste;
  private JMenuItem      mnuEditSelectAll;
  private JMenuItem      mnuHelpContent;
  private Document       docInput;
  private JTextField     fldInput;
  private JEditorPane    fldOutput;


  public static CalculatorFrm open()
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
        instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new CalculatorFrm();
    }
    instance.toFront();
    instance.setVisible( true );
    return instance;
  }


	/* --- DocumentListener --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- CaretListener --- */

  @Override
  public void caretUpdate( CaretEvent e )
  {
    updCutCopyButtons();
  }


	/* --- FlavorListener --- */

  @Override
  public void flavorsChanged( FlavorEvent e )
  {
    updClipboardStatus();
    updPasteButton();
  }


	/* --- FocusListener --- */

  @Override
  public void focusGained( FocusEvent e )
  {
    JTextComponent fld = null;
    Component      c   = e.getComponent();
    if( c != null ) {
      if( c instanceof JTextComponent ) {
	fld = (JTextComponent) c;
      }
    }
    this.lastTextFld = fld;
    updCutCopyButtons();
    updPasteButton();
  }


  @Override
  public void focusLost( FocusEvent e )
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
      if( src == this.fldInput ) {
	rv = true;
	updOutput();
      }
      else if( src == this.mnuFileClose ) {
	rv = true;
	doClose();
      }
      else if( src == this.mnuEditCut ) {
	rv = true;
	doEditCut();
      }
      else if( src == this.mnuEditCopy ) {
	rv = true;
	doEditCopy();
      }
      else if( src == this.mnuEditPaste ) {
	rv = true;
	doEditPaste();
      }
      else if( src == this.mnuEditSelectAll ) {
	rv = true;
	doEditSelectAll();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.openPage( HELP_PAGE );
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = false;
    if( Main.isTopFrm( this ) ) {
      rv = EmuUtil.closeOtherFrames( this );
      if( rv ) {
	rv = super.doClose();
      }
      if( rv ) {
	Main.exitSuccess();
      }
    } else {
      rv = super.doClose();
    }
    if( rv ) {
      // damit beim erneuten Oeffnen das Eingabefeld leer ist
      this.fldInput.setText( "" );
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.fldInput.requestFocus();
  }


	/* --- Aktionen im Menu Bearbeiten --- */

  private void doEditCut()
  {
    JTextComponent textFld = this.lastTextFld;
    if( textFld != null ) {
      textFld.cut();
    }
  }


  private void doEditCopy()
  {
    JTextComponent textFld = this.lastTextFld;
    if( textFld != null ) {
      textFld.copy();
    }
  }


  private void doEditPaste()
  {
    JTextComponent textFld = this.lastTextFld;
    if( textFld != null ) {
      textFld.paste();
    }
  }


  private void doEditSelectAll()
  {
    JTextComponent textFld = this.lastTextFld;
    if( textFld != null ) {
      textFld.selectAll();
    }
  }


	/* --- Konstruktor --- */

  private CalculatorFrm()
  {
    this.parser           = new ExprParser();
    this.lastTextFld      = null;
    this.clipboard        = null;
    this.clipboardHasText = false;
    setTitle( TITLE );


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    this.mnuFileClose = createMenuItemClose();
    mnuFile.add( this.mnuFileClose );


    // Menu Bearbeiten
    JMenu mnuEdit = createMenuEdit();

    this.mnuEditCut = createMenuItemCut( true );
    mnuEdit.add( this.mnuEditCut );

    this.mnuEditCopy = createMenuItemCopy( true );
    mnuEdit.add( this.mnuEditCopy );

    this.mnuEditPaste = createMenuItemPaste( true );
    mnuEdit.add( this.mnuEditPaste );
    mnuEdit.addSeparator();

    this.mnuEditSelectAll = createMenuItemSelectAll( true );
    mnuEdit.add( this.mnuEditSelectAll );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();

    this.mnuHelpContent = createMenuItem( "Hilfe zum Rechner..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menuleiste zusammenbauen
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuEdit, mnuHelp ) );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );


    // Bereich Eingabe
    JPanel panelInput = GUIFactory.createPanel( new GridBagLayout() );
    add( panelInput, gbc );

    panelInput.setBorder( GUIFactory.createTitledBorder( "Eingabe" ) );

    GridBagConstraints gbcInput = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    panelInput.add( GUIFactory.createLabel( "Ausdruck:" ), gbcInput );

    this.fldInput      = GUIFactory.createTextField();
    this.docInput      = this.fldInput.getDocument();
    gbcInput.fill      = GridBagConstraints.HORIZONTAL;
    gbcInput.weightx   = 1.0;
    gbcInput.gridwidth = GridBagConstraints.REMAINDER;
    gbcInput.gridx++;
    panelInput.add( this.fldInput, gbcInput );

    gbcInput.anchor        = GridBagConstraints.EAST;
    gbcInput.fill          = GridBagConstraints.NONE;
    gbcInput.insets.bottom = 0;
    gbcInput.weightx       = 0.0;
    gbcInput.gridwidth     = 1;
    gbcInput.gridy++;
    panelInput.add( GUIFactory.createLabel( "Bin\u00E4rzahl:" ), gbcInput );

    gbcInput.anchor = GridBagConstraints.WEST;
    gbcInput.gridx++;
    panelInput.add( GUIFactory.createLabel( "$..." ), gbcInput );

    gbcInput.anchor     = GridBagConstraints.EAST;
    gbcInput.insets.top = 0;
    gbcInput.gridx      = 1;
    gbcInput.gridy++;
    panelInput.add( GUIFactory.createLabel( "Oktalzahl:" ), gbcInput );

    gbcInput.anchor = GridBagConstraints.WEST;
    gbcInput.gridx++;
    panelInput.add( GUIFactory.createLabel( "...Q" ), gbcInput );

    gbcInput.anchor        = GridBagConstraints.EAST;
    gbcInput.insets.bottom = 5;
    gbcInput.gridx         = 1;
    gbcInput.gridy++;
    panelInput.add( GUIFactory.createLabel( "Hexadezimalzahl:" ), gbcInput );

    gbcInput.anchor = GridBagConstraints.WEST;
    gbcInput.gridx++;
    panelInput.add( GUIFactory.createLabel( "0x... oder ...H" ), gbcInput );


    // Bereich Ausgabe
    JPanel panelOutput = GUIFactory.createPanel( new BorderLayout() );
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( panelOutput, gbc );

    panelOutput.setBorder( GUIFactory.createTitledBorder( "Ausgabe" ) );

    this.fldOutput = GUIFactory.createEditorPane();
    this.fldOutput.setContentType( "text/html" );
    this.fldOutput.setText( DEFAULT_TEXT );
    this.fldOutput.setBorder( BorderFactory.createLoweredBevelBorder() );
    this.fldOutput.setEditable( false );
    panelOutput.add(
		GUIFactory.createScrollPane( this.fldOutput ),
		BorderLayout.CENTER );


    // Zwischenablage
    Toolkit tk = EmuUtil.getToolkit( this );
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
    }


    // Fenstergroesse
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      this.fldOutput.setPreferredSize( new Dimension( 300, 100 ) );
      pack();
      setScreenCentered();
      this.fldOutput.setPreferredSize( null );
    }


    // Listener
    this.fldInput.addActionListener( this );
    this.fldInput.addCaretListener( this );
    this.fldInput.addFocusListener( this );
    if( this.docInput != null ) {
      this.docInput.addDocumentListener( this );
    }
    this.fldOutput.addCaretListener( this );
    this.fldOutput.addFocusListener( this );
    if( this.clipboard != null ) {
      this.clipboard.addFlavorListener( this );
      this.mnuEditPaste.setEnabled( false );
      updClipboardStatus();
    }
  }


	/* --- private Methoden --- */

  private void docChanged( DocumentEvent e )
  {
    if( (this.docInput != null) && (e.getDocument() == this.docInput) )
      updOutput();
  }


  private void updOutput()
  {
    String result = DEFAULT_TEXT;
    String text   = this.fldInput.getText();
    if( text != null ) {
      int len = text.length();
      if( len > 0 ) {
	boolean       status = false;
	StringBuilder buf    = new StringBuilder( 256 );
	try {
	  if( len == 1 ) {
	    char ch = text.charAt( 0 );
	    if( Character.isDefined( ch ) ) {
	      appendResultRow(
			buf,
			"Unicode des Zeichen",
			(int) ch );
	    }
	  }
	  appendResultRow(
			buf,
			"Ergebnis des Ausdrucks",
			parser.parseExpr( text ) );
	  appendResultEnd( buf );
	}
	catch( ParseException ex ) {
	  if( buf.length() > 0 ) {
	    appendResultEnd( buf );
	  } else {
	    int pos = ex.getErrorOffset();
	    if( pos > len ) {
	      pos = len;
	    }
	    if( pos < 0 ) {
	      pos = 0;
	    }
	    buf.append( "<html>\n<p>" );
	    EmuUtil.appendHTML( buf, ex.getMessage() );
	    buf.append( "</p>\n<p>" );
	    EmuUtil.appendHTML( buf, text.substring( 0, pos ) );
	    buf.append( " <b>?</b> " );
	    EmuUtil.appendHTML( buf, text.substring( pos ) );
	    buf.append( "</p>\n</html>\n" );
	  }
	}
	result = buf.toString();
      }
    }
    this.fldOutput.setText( result );
    try {
      this.fldOutput.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }


  private void appendResultRow(
			StringBuilder buf,
			String        title,
			Number        value )
  {
    if( value != null ) {
      boolean isLong = false;
      long    lValue = 0;
      if( value instanceof BigDecimal ) {
	if( ((BigDecimal) value).scale() <= 0 ) {
	  try {
	    lValue = ((BigDecimal) value).longValueExact();
	    isLong = true;
	  }
	  catch( ArithmeticException ex ) {}
	}
      }
      if( value instanceof BigInteger ) {
	if( ((BigInteger) value).bitLength() < 64 ) {
	  lValue = value.longValue();
	  isLong = true;
	}
      }
      else if( (value instanceof Integer) || (value instanceof Long) ) {
	lValue = value.longValue();
	isLong = true;
      }
      if( buf.length() < 1 ) {
	buf.append( "<html>\n"
		+ "<table border=1>\n"
		+ "<tr><th nowrap></th><th nowrap>Hex</th>"
		+ "<th nowrap>Dezimal</th><th nowrap>Oktal</th>"
		+ "<th nowrap>Bin&auml;r</th>"
		+ "<th nowrap>Unicode-Zeichen</th></tr>\n" );
      }
      buf.append( "<tr><td nowrap>" );
      EmuUtil.appendHTML( buf, title );
      buf.append( ":</td><td>" );
      if( isLong ) {
	EmuUtil.appendHTML( buf, Long.toHexString( lValue ).toUpperCase() );
      }
      buf.append( "</td><td nowrap>" );
      String decText = null;
      if( value instanceof BigDecimal ) {
	if( Math.abs( ((BigDecimal) value).scale() ) < 10 ) {
	  decText = ((BigDecimal) value).toPlainString();
	}
      }
      if( decText == null ) {
	decText = value.toString();
      }
      EmuUtil.appendHTML( buf, decText );
      buf.append( "</td><td nowrap>" );
      if( isLong ) {
	EmuUtil.appendHTML( buf, Long.toOctalString( lValue ) );
      }
      buf.append( "</td><td nowrap>" );
      if( isLong ) {
	EmuUtil.appendHTML( buf, Long.toBinaryString( lValue ) );
      }
      buf.append( "</td><td nowrap>" );
      if( isLong ) {
	if( (lValue >= 0) && (lValue < Integer.MAX_VALUE) ) {
	  if( (lValue > '\u0020') && (lValue <= '\u007E') ) {
	    EmuUtil.appendHTML( buf, Character.toString( (char) lValue ) );
	  } else if( Character.isDefined( (int) lValue ) ) {
	    buf.append( String.format( "&#%d;", lValue ) );
	  }
	}
      }
      buf.append( "</td></tr>\n" );
    }
  }


  private void appendResultEnd( StringBuilder buf )
  {
    buf.append( "</table>\n</html>\n" );
  }


  private void updClipboardStatus()
  {
    boolean hasText = false;
    if( this.clipboard != null ) {
      try {
	hasText = this.clipboard.isDataFlavorAvailable(
					DataFlavor.stringFlavor );
      }
      catch( IllegalStateException ex ) {}
    }
    this.clipboardHasText = hasText;
  }


  private void updCutCopyButtons()
  {
    boolean editable     = false;
    boolean hasSelection = false;
    if( this.lastTextFld != null ) {
      editable     = this.lastTextFld.isEditable();
      hasSelection = ((this.lastTextFld.getSelectionStart() >= 0)
			&& (this.lastTextFld.getSelectionEnd()
				> this.lastTextFld.getSelectionStart()));
    }
    this.mnuEditCut.setEnabled( editable && hasSelection );
    this.mnuEditCopy.setEnabled( hasSelection );
  }


  private void updPasteButton()
  {
    boolean editable = (this.lastTextFld != null ?
				this.lastTextFld.isEditable()
				: false);
    this.mnuEditPaste.setEnabled( editable && this.clipboardHasText );
  }
}
