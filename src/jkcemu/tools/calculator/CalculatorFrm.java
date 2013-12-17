/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Rechner
 */

package jkcemu.tools.calculator;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.lang.*;
import java.math.*;
import java.text.ParseException;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import jkcemu.Main;
import jkcemu.base.*;


public class CalculatorFrm extends BasicFrm implements
						CaretListener,
						DocumentListener,
						FlavorListener,
						FocusListener
{
  private static final String defaultText = "Geben Sie bitte ein Zeichen"
		+ " oder einen numerischen Ausdruck ein!";

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


  public static void open()
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
      if( c instanceof JTextComponent )
	fld = (JTextComponent) c;
    }
    this.lastTextFld = fld;
    updCutCopyButtons();
    updPasteButton();
  }


  @Override
  public void focusLost( FocusEvent e )
  {
    // empty
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
	HelpFrm.open( "/help/tools/calculator.htm" );
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      Main.checkQuit( this );
    } else {
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
    if( textFld != null )
      textFld.cut();
  }


  private void doEditCopy()
  {
    JTextComponent textFld = this.lastTextFld;
    if( textFld != null )
      textFld.copy();
  }


  private void doEditPaste()
  {
    JTextComponent textFld = this.lastTextFld;
    if( textFld != null )
      textFld.paste();
  }


  private void doEditSelectAll()
  {
    JTextComponent textFld = this.lastTextFld;
    if( textFld != null )
      textFld.selectAll();
  }


	/* --- Konstruktor --- */

  private CalculatorFrm()
  {
    this.parser           = new ExprParser();
    this.lastTextFld      = null;
    this.clipboard        = null;
    this.clipboardHasText = false;
    setTitle( "JKCEMU Rechner" );
    Main.updIcon( this );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuFileClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuFileClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );

    this.mnuEditCut = createJMenuItem(
		"Ausschneiden",
		KeyStroke.getKeyStroke( KeyEvent.VK_X, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditCut );

    this.mnuEditCopy = createJMenuItem(
		"Kopieren",
		KeyStroke.getKeyStroke( KeyEvent.VK_C, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditCopy );

    this.mnuEditPaste = createJMenuItem(
		"Einf\u00FCgen",
		KeyStroke.getKeyStroke( KeyEvent.VK_V, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditPaste );
    mnuEdit.addSeparator();

    this.mnuEditSelectAll = createJMenuItem( "Alles ausw\u00E4hlen" );
    mnuEdit.add( this.mnuEditSelectAll );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menuleiste zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuEdit );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


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
    JPanel panelInput = new JPanel( new GridBagLayout() );
    add( panelInput, gbc );

    panelInput.setBorder( BorderFactory.createTitledBorder( "Eingabe" ) );

    GridBagConstraints gbcInput = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    panelInput.add( new JLabel( "Ausdruck:" ), gbcInput );

    this.fldInput = new JTextField();
    this.fldInput.addActionListener( this );
    this.fldInput.addCaretListener( this );
    this.fldInput.addFocusListener( this );
    this.docInput = this.fldInput.getDocument();
    if( this.docInput != null ) {
      this.docInput.addDocumentListener( this );
    }
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
    panelInput.add( new JLabel( "Bin\u00E4rzahl:" ), gbcInput );

    gbcInput.anchor = GridBagConstraints.WEST;
    gbcInput.gridx++;
    panelInput.add( new JLabel( "$..." ), gbcInput );

    gbcInput.anchor     = GridBagConstraints.EAST;
    gbcInput.insets.top = 0;
    gbcInput.gridx      = 1;
    gbcInput.gridy++;
    panelInput.add( new JLabel( "Oktalzahl:" ), gbcInput );

    gbcInput.anchor = GridBagConstraints.WEST;
    gbcInput.gridx++;
    panelInput.add( new JLabel( "...Q" ), gbcInput );

    gbcInput.anchor        = GridBagConstraints.EAST;
    gbcInput.insets.bottom = 5;
    gbcInput.gridx         = 1;
    gbcInput.gridy++;
    panelInput.add( new JLabel( "Hexadezimalzahl:" ), gbcInput );

    gbcInput.anchor = GridBagConstraints.WEST;
    gbcInput.gridx++;
    panelInput.add( new JLabel( "0x... oder ...H" ), gbcInput );


    // Bereich Ausgabe
    JPanel panelOutput = new JPanel( new BorderLayout() );
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( panelOutput, gbc );

    panelOutput.setBorder( BorderFactory.createTitledBorder( "Ausgabe" ) );

    this.fldOutput = new JEditorPane( "text/html", defaultText );
    this.fldOutput.setBorder( BorderFactory.createLoweredBevelBorder() );
    this.fldOutput.setEditable( false );
    this.fldOutput.addCaretListener( this );
    this.fldOutput.addFocusListener( this );
    panelOutput.add( new JScrollPane( this.fldOutput ), BorderLayout.CENTER );


    // Zwischenablage
    Toolkit tk = getToolkit();
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
      if( this.clipboard != null ) {
	this.clipboard.addFlavorListener( this );
	this.mnuEditPaste.setEnabled( false );
	updClipboardStatus();
      }
    }


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      this.fldOutput.setPreferredSize( new Dimension( 300, 100 ) );
      pack();
      setScreenCentered();
      this.fldOutput.setPreferredSize( null );
    }
    setResizable( true );
  }


	/* --- private Methoden --- */

  private void docChanged( DocumentEvent e )
  {
    if( (this.docInput != null) && (e.getDocument() == this.docInput) )
      updOutput();
  }


  private void updOutput()
  {
    String result = defaultText;
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
			new Integer( (int) ch ) );
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
	    appendText( buf, ex.getMessage() );
	    buf.append( "</p>\n<p>" );
	    appendText( buf, text.substring( 0, pos ) );
	    buf.append( " <b>?</b> " );
	    appendText( buf, text.substring( pos ) );
	    buf.append( "</p>\n</html>\n" );
	  }
	}
	result = buf.toString();
      }
    }
    this.fldOutput.setText( result );
  }


  private void appendResultRow( StringBuilder buf, String title, Number value )
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
      appendText( buf, title );
      buf.append( ":</td><td>" );
      if( isLong ) {
	appendText( buf, Long.toHexString( lValue ).toUpperCase() );
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
      appendText( buf, decText );
      buf.append( "</td><td nowrap>" );
      if( isLong ) {
	appendText( buf, Long.toOctalString( lValue ) );
      }
      buf.append( "</td><td nowrap>" );
      if( isLong ) {
	appendText( buf, Long.toBinaryString( lValue ) );
      }
      buf.append( "</td><td nowrap>" );
      if( isLong ) {
	if( (lValue >= 0) && (lValue < Integer.MAX_VALUE) ) {
	  if( (lValue > '\u0020') && (lValue <= '\u007E') ) {
	    appendText( buf, Character.toString( (char) lValue ) );
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


  private void appendText( StringBuilder buf, String text )
  {
    if( text != null ) {
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
	char ch = text.charAt( i );
	switch( ch ) {
	  case '<':
	    buf.append( "&lt;" );
	    break;

	  case '>':
	    buf.append( "&gt;" );
	    break;

	  case '\"':
	    buf.append( "&quot;" );
	    break;

	  case '&':
	    buf.append( "&amp;" );
	    break;

	  case '\u00C4':
	    buf.append( "&Auml;" );
	    break;

	  case '\u00D6':
	    buf.append( "&Ouml;" );
	    break;

	  case '\u00DC':
	    buf.append( "&Uuml;" );
	    break;

	  case '\u00E4':
	    buf.append( "&auml;" );
	    break;

	  case '\u00F6':
	    buf.append( "&ouml;" );
	    break;

	  case '\u00FC':
	    buf.append( "&uuml;" );
	    break;

	  case '\u00DF':
	    buf.append( "&szlig;" );
	    break;

	  default:
	    if( ch < '\u0020' ) {
	      buf.append( (char) '\u0020' );
	    }
	    else if( ch > '\u007E' ) {
	      buf.append( "&#" );
	      buf.append( String.valueOf( ch ) );
	      buf.append( (char) ';' );
	    } else {
	      buf.append( (char) ch );
	    }
	}
      }
    }
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

