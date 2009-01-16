/*
 * (c) 2008 Jens Mueller
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
import java.math.BigDecimal;
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

  private ScreenFrm      screenFrm;
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
  private JRadioButton   btnDecimal;
  private JRadioButton   btnHex;
  private JRadioButton   btnOctal;
  private JRadioButton   btnBinary;
  private JRadioButton   btnUpdOnEnter;
  private JRadioButton   btnUpdImmediately;
  private JEditorPane    fldOutput;


  public CalculatorFrm( ScreenFrm screenFrm )
  {
    this.screenFrm        = screenFrm;
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

    panelInput.add( new JLabel( "Formel/Ausdruck:" ), gbcInput );

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

    gbcInput.fill          = GridBagConstraints.NONE;
    gbcInput.insets.bottom = 0;
    gbcInput.weightx       = 0.0;
    gbcInput.gridwidth     = GridBagConstraints.REMAINDER;
    gbcInput.gridx         = 0;
    gbcInput.gridy++;
    panelInput.add(
	new JLabel("Basis der Zahlen, die nicht als Oktal- (...Q),"
			+ " Hexadezimal- (0x... oder ...H)" ),
	gbcInput );

    gbcInput.insets.top = 0;
    gbcInput.gridy++;
    panelInput.add(
	new JLabel( "oder Flie\u00DFkommazahl (Dezimalpunkt)"
			+ " gekennzeichnet sind:" ),
	gbcInput );

    JPanel panelRadix = new JPanel( new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    gbcInput.gridy++;
    panelInput.add( panelRadix, gbcInput );

    ButtonGroup grpRadix   = new ButtonGroup();
    this.btnDecimal = new JRadioButton( "Dezimal", true );
    this.btnDecimal.addActionListener( this );
    grpRadix.add( this.btnDecimal );
    panelRadix.add( this.btnDecimal );

    this.btnHex = new JRadioButton( "Hexadezimal", false );
    this.btnHex.addActionListener( this );
    grpRadix.add( this.btnHex );
    panelRadix.add( this.btnHex );

    this.btnOctal = new JRadioButton( "Oktal", false );
    this.btnOctal.addActionListener( this );
    grpRadix.add( this.btnOctal );
    panelRadix.add( this.btnOctal );

    this.btnBinary = new JRadioButton( "Bin\u00E4r", false );
    this.btnBinary.addActionListener( this );
    grpRadix.add( this.btnBinary );
    panelRadix.add( this.btnBinary );

    // Bereich Optionen
    this.btnUpdOnEnter   = null;
    this.btnUpdImmediately = null;
    if( this.docInput != null ) {
      JPanel panelOptions = new JPanel( new GridBagLayout() );
      gbc.gridy++;
      add( panelOptions, gbc );

      panelOptions.setBorder( BorderFactory.createTitledBorder( "Optionen" ) );

      GridBagConstraints gbcOptions = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

      ButtonGroup grpUpdMode = new ButtonGroup();

      this.btnUpdOnEnter = new JRadioButton( "Nach ENTER berechnen", true );
      grpUpdMode.add( this.btnUpdOnEnter );
      panelOptions.add( this.btnUpdOnEnter, gbcOptions );

      this.btnUpdImmediately = new JRadioButton(
				"Nach jedem Tastendruck berechnen",
				false );
      grpUpdMode.add( this.btnUpdImmediately );
      gbcOptions.insets.top    = 0;
      gbcOptions.insets.bottom = 5;
      gbcOptions.gridy++;
      panelOptions.add( this.btnUpdImmediately, gbcOptions );
    }


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


	/* --- DocumentListener --- */

  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- CaretListener --- */

  public void caretUpdate( CaretEvent e )
  {
    updCutCopyButtons();
  }


	/* --- FlavorListener --- */

  public void flavorsChanged( FlavorEvent e )
  {
    updClipboardStatus();
    updPasteButton();
  }


	/* --- FocusListener --- */

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


  public void focusLost( FocusEvent e )
  {
    // empty
  }


	/* --- ueberschriebene Methoden --- */

  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean rv = false;
    if( props != null ) {
      rv = super.applySettings( props, resizable );

      String radixText = props.getProperty( "jkcemu.calculator.radix" );
      if( radixText != null ) {
	if( radixText.equals( "2" ) ) {
	  this.btnBinary.setSelected( true );
	}
	else if( radixText.equals( "8" ) ) {
	  this.btnOctal.setSelected( true );
	}
	else if( radixText.equals( "16" ) ) {
	  this.btnHex.setSelected( true );
	}
      }

      if( this.docInput != null ) {
	boolean updImmediately = EmuUtil.parseBooleanProperty(
				props,
				"jkcemu.calculator.computes_immediately",
				true );
	if( updImmediately ) {
	  this.btnUpdImmediately.setSelected( true );
	} else {
	  this.btnUpdOnEnter.setSelected( true );
	}
      }
    }
    return rv;
  }


  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.fldInput ) {
	rv = true;
	updOutput();
      }
      if( (src == this.btnDecimal)
	  || (src == this.btnHex)
	  || (src == this.btnOctal)
	  || (src == this.btnBinary) )
      {
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
	this.screenFrm.showHelp( "/help/tools/calculator.htm" );
      }
    }
    return rv;
  }


  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );
      if( this.btnHex.isSelected() ) {
	props.setProperty( "jkcemu.calculator.radix", "16" );
      } else if( this.btnOctal.isSelected() ) {
	props.setProperty( "jkcemu.calculator.radix", "8" );
      } else if( this.btnBinary.isSelected() ) {
	props.setProperty( "jkcemu.calculator.radix", "2" );
      } else {
	props.setProperty( "jkcemu.calculator.radix", "10" );
      }
      props.setProperty(
		"jkcemu.calculator.computes_immediately",
		String.valueOf( isUpdImmediately() ) );
    }
  }


  public void windowClosed( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.screenFrm.childFrameClosed( this );
  }


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


	/* --- private Methoden --- */

  private void docChanged( DocumentEvent e )
  {
    if( (this.docInput != null)
	&& (e.getDocument() == this.docInput)
	&& isUpdImmediately() )
    {
      updOutput();
    }
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
	  ExprParser.Radix radix = ExprParser.Radix.DECIMAL;
	  if( this.btnHex.isSelected() ) {
	    radix = ExprParser.Radix.HEXADECIMAL;
	  }
	  else if( this.btnOctal.isSelected() ) {
	    radix = ExprParser.Radix.OCTAL;
	  }
	  else if( this.btnBinary.isSelected() ) {
	    radix = ExprParser.Radix.BINARY;
	  }
	  appendResultRow(
			buf,
			"Ergebnis des Ausdrucks",
			parser.parseExpr( text, radix ) );
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
      if( buf.length() < 1 ) {
	buf.append( "<html>\n"
		+ "<table border=1>\n"
		+ "<tr><th nowrap></th><th nowrap>Hex</th>"
		+ "<th nowrap>Dezimal</th><th nowrap>Oktal</th>"
		+ "<th nowrap>Bin&auml;r</th><th nowrap>ASCII</th></tr>\n" );
      }
      buf.append( "<tr><td nowrap>" );
      appendText( buf, title );
      buf.append( ":</td><td>" );
      if( !isFloat( value ) ) {
	appendText( buf, Long.toHexString( value.longValue() ).toUpperCase() );
      }
      buf.append( "</td><td nowrap>" );
      appendText( buf, value.toString() );
      buf.append( "</td><td nowrap>" );
      if( !isFloat( value ) ) {
	appendText( buf, Long.toOctalString( value.longValue() ) );
      }
      buf.append( "</td><td nowrap>" );
      if( !isFloat( value ) ) {
	appendText( buf, Long.toBinaryString( value.longValue() ) );
      }
      buf.append( "</td><td nowrap>" );
      if( !isFloat( value ) ) {
	long longValue = value.longValue();
	if( (longValue > '\u0020') && (longValue <= '\u007E') )
	  appendText( buf, Character.toString( (char) longValue ) );
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


  private static boolean isFloat( Number value )
  {
    return (value instanceof BigDecimal)
	   || (value instanceof Double)
	   || (value instanceof Float);
  }


  private boolean isUpdImmediately()
  {
    return this.btnUpdImmediately != null ?
			this.btnUpdImmediately.isSelected()
			: false;
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

