/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer einen AutoInput-Eintrag
 */

package jkcemu.base;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.lang.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;


public class AutoInputEntryDlg extends BaseDlg
{
  private static final String LABEL_WAIT_TIME = "Wartezeit vor Eingabe:";
  private static final String CMD_CHAR_PREFIX = "char.";

  private static int[] waitMillis = {
				0, 200, 500, 1000, 1500,
				2000, 3000, 4000, 5000,
				6000, 7000, 8000, 9000 };

  private static String[][] specialChars = {
	{ "\u0003", "Ctrl-C / Abbruch" },
	{ "\u0008", "Cursor links / Back Space" },
	{ "\t",     "Cursor rechts / Tabulator" },
	{ "\n",     "Cursor runter" },
	{ "\u000B", "Cursor hoch" },
	{ "\r",     "Enter / Return" } };

  private static NumberFormat waitFmt = null;

  private AutoInputEntry    appliedAutoInputEntry;
  private JComboBox<String> comboWaitSeconds;
  private AutoInputDocument docInputText;
  private JTextField        fldInputText;
  private JTextField        fldRemark;
  private JPopupMenu        mnuSpecialChars;
  private JButton           btnSpecialChars;
  private JButton           btnOK;
  private JButton           btnCancel;


  public static AutoInputEntry openNewEntryDlg(
					Window  owner,
					boolean swapKeyCharCase,
					int     defaultMillisToWait )
  {
    AutoInputEntryDlg dlg = new AutoInputEntryDlg(
					owner,
					swapKeyCharCase,
					"Neuer AutoInput-Eintrag" );
    dlg.setMillisToWait( defaultMillisToWait );
    dlg.setVisible( true );
    return dlg.appliedAutoInputEntry;
  }


  public static AutoInputEntry openEditEntryDlg(
					Window         owner,
					boolean        swapKeyCharCase,
					AutoInputEntry entry )
  {
    AutoInputEntryDlg dlg = new AutoInputEntryDlg(
					owner,
					swapKeyCharCase,
					"AutoInput-Eintrag bearbeiten" );
    dlg.setMillisToWait( entry.getMillisToWait() );
    dlg.setInputText( entry.getInputText() );
    dlg.fldRemark.setText( entry.getRemark() );
    dlg.setVisible( true );
    return dlg.appliedAutoInputEntry;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.btnSpecialChars ) {
	rv = true;
	this.mnuSpecialChars.show(
			getContentPane(),
			this.btnSpecialChars.getX(),
			this.btnSpecialChars.getY()
				+ this.btnSpecialChars.getHeight() );
      } else if( src == this.btnOK ) {
	rv = true;
	doApply();
      } else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      } else if( e instanceof ActionEvent ) {
	String cmd = ((ActionEvent) e).getActionCommand();
	if( cmd != null ) {
	  int prefixLen = CMD_CHAR_PREFIX.length();
	  if( cmd.startsWith( CMD_CHAR_PREFIX )
	      && (cmd.length() > prefixLen) )
	  {
	    try {
	      this.docInputText.insertString(
			this.fldInputText.getCaretPosition(),
			cmd.substring( prefixLen ),
			null );
	    }
	    catch( BadLocationException ex ) {}
	  }
	}
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private AutoInputEntryDlg(
			Window  owner,
			boolean swapKeyCharCase,
			String  title )
  {
    super( owner, title );
    this.appliedAutoInputEntry = null;

    // Format fuer Wartezeit
    if( waitFmt == null ) {
      waitFmt = NumberFormat.getNumberInstance();
      if( waitFmt instanceof DecimalFormat ) {
	((DecimalFormat) waitFmt).applyPattern( "#0.0" );
      }
    }


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    add( new JLabel( LABEL_WAIT_TIME ), gbc );

    this.comboWaitSeconds = new JComboBox<>();
    for( int millis : waitMillis ) {
      this.comboWaitSeconds.addItem(
			waitFmt.format( (double) millis / 1000.0 ) );
    }
    this.comboWaitSeconds.setEditable( true );
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx++;
    add( this.comboWaitSeconds, gbc );

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx++;
    add( new JLabel( "Sekunden" ), gbc );

    gbc.gridx = 0;
    gbc.gridy++;
    add( new JLabel( "Eingabetext:" ), gbc );

    this.docInputText = new AutoInputDocument( swapKeyCharCase );
    this.fldInputText = new JTextField( this.docInputText, "", 0 );
    gbc.weightx       = 1.0;
    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( this.fldInputText, gbc );

    Font font = this.fldInputText.getFont();
    if( font != null ) {
      this.comboWaitSeconds.setFont( font );
    }

    this.btnSpecialChars = new JButton(
		"Bet\u00E4tigung einer Steuertaste eingeben" );
    gbc.weightx = 0.0;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.gridy++;
    add( this.btnSpecialChars, gbc );

    gbc.gridwidth = 1;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( new JLabel( "Bemerkung:" ), gbc );

    this.fldRemark = new JTextField();
    gbc.weightx    = 1.0;
    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( this.fldRemark, gbc );

    JPanel panelBtn   = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.weightx       = 0.0;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Menu fuer Sonderzeichen
    this.mnuSpecialChars = new JPopupMenu();
    for( int i = 0; i < specialChars.length; i++ ) {
      String code = specialChars[ i ][ 0 ];
      if( code != null ) {
	if( !code.isEmpty() ) {
	  String s = AutoInputDocument.toVisibleText( code );
	  if( s != null ) {
	    JMenuItem item = new JMenuItem( specialChars[ i ][ 1 ] );
	    item.setActionCommand( CMD_CHAR_PREFIX + s );
	    item.addActionListener( this );
	    this.mnuSpecialChars.add( item );
	  }
	}
      }
    }


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Listeners
    this.btnSpecialChars.addActionListener( this );
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- Aktionen --- */

  private void doApply()
  {
    try {
      String inputText = this.fldInputText.getText();
      if( inputText != null ) {
	if( inputText.isEmpty() ) {
	  inputText = null;
	}
	if( inputText != null ) {
	  int  millis = 0;
	  try {
	    Object o = this.comboWaitSeconds.getSelectedItem();
	    if( o != null ) {
	      String s = o.toString();
	      if( s != null ) {
		Number value = waitFmt.parse( s );
		if( value != null ) {
		  millis = (int) Math.round( value.doubleValue() * 1000.0 );
		}
	      }
	    }
	    this.appliedAutoInputEntry = new AutoInputEntry(
				millis,
				AutoInputDocument.toRawText( inputText ),
				this.fldRemark.getText() );
	    doClose();
	  }
	  catch( ParseException ex ) {
	    throw new NumberFormatException(
			LABEL_WAIT_TIME + ": Ung\u00FCltiges Format" );
	  }
	} else {
	  showErrorDlg(
		this,
		"Eingabetext: Sie m\u00FCssen mindestens"
				+ " ein Zeichen eingeben!" );
	}
      }
    }
    catch( NumberFormatException ex ) {
      showErrorDlg( this, ex );
    }
  }


	/* --- private Methoden --- */

  private void setInputText( String text )
  {
    boolean swapCase = this.docInputText.getSwapCase();
    this.docInputText.setSwapCase( false );
    this.fldInputText.setText( AutoInputDocument.toVisibleText( text ) );
    this.docInputText.setSwapCase( swapCase );
  }


  private void setMillisToWait( int millis )
  {
    this.comboWaitSeconds.setSelectedItem(
			waitFmt.format( (double) millis / 1000.0 ) );
  }
}
