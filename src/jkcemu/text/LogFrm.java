/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zur Ausgabe von Log-Meldungen
 */

package jkcemu.text;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.base.*;


public class LogFrm extends BasicFrm implements Appendable, CaretListener
{
  private EditText  correspondingEditText;
  private JMenuItem mnuFileClose;
  private JMenuItem mnuEditCopy;
  private JMenuItem mnuEditSelectAll;
  private JTextArea fldText;


  public LogFrm( EditText correspondingEditText, String title )
  {
    this.correspondingEditText = correspondingEditText;
    setTitle( title );
    Main.updIcon( this );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuFileClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuFileClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );

    this.mnuEditCopy = createJMenuItem(
		"Kopieren",
		KeyStroke.getKeyStroke( KeyEvent.VK_C, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuEditCopy );
    mnuEdit.addSeparator();

    this.mnuEditSelectAll = createJMenuItem( "Alles ausw\u00E4hlen" );
    mnuEdit.add( this.mnuEditSelectAll );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuEdit );
    setJMenuBar( mnuBar );


    // Fensterinhalt
    setLayout( new BorderLayout() );

    this.fldText = new JTextArea();
    this.fldText.setEditable( false );
    this.fldText.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.fldText.addCaretListener( this );
    this.fldText.addMouseListener( this );
    add(
	new JScrollPane(
		this.fldText,
		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS ),
	BorderLayout.CENTER );

    Font font = this.fldText.getFont();
    if( font != null ) {
      this.fldText.setFont(
		new Font( "Monospaced", font.getStyle(), font.getSize() ) );
    } else {
      this.fldText.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
    }


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      setBoundsToDefaults();
    }
    setResizable( true );
  }


  public void reset( EditText correspondingEditText, String title )
  {
    setTitle( title );
    this.correspondingEditText = correspondingEditText;
    this.fldText.setText( "" );
  }


	/* --- Appendable --- */

  public Appendable append( char ch )
  {
    appendToText( String.valueOf( ch ) );
    return this;
  }


  public Appendable append( CharSequence csq )
  {
    appendToText( csq != null ? csq.toString() : "null" );
    return this;
  }


  public Appendable append( CharSequence csq, int begPos, int endPos )
  {
    if( csq != null ) {
      try {
	if( csq != null ) {
	  String text = csq.toString();
	  appendToText( text.substring( begPos, endPos ) );
	} else {
	  appendToText( "null" );
	}
      }
      catch( IndexOutOfBoundsException ex ) {}
    }
    return this;
  }


	/* --- CaretListener --- */

  public void caretUpdate( CaretEvent e )
  {
    int selStart = this.fldText.getSelectionStart();
    this.mnuEditCopy.setEnabled(
	(selStart >= 0) && (selStart < this.fldText.getSelectionEnd()) );
  }


	/* --- ueberschriebene Methoden --- */

  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.mnuFileClose ) {
	rv = true;
	doClose();
      }
      else if( src == this.mnuEditCopy ) {
	rv = true;
	this.fldText.copy();
      }
      else if( src == this.mnuEditSelectAll ) {
	rv = true;
	this.fldText.selectAll();
      }
    }
    return rv;
 }


  public void mouseClicked( MouseEvent e )
  {
    if( (e.getComponent() == this.fldText)
	&& (e.getClickCount() > 1) )
    {
      Point pt = e.getPoint();
      if( pt != null ) {
	int pos = this.fldText.viewToModel( pt );
	if( pos >= 0 ) {
	  String text = this.fldText.getText();
	  if( text != null ) {
	    int len = text.length();
	    if( pos < len ) {
	      while( pos > 0 ) {
		if( text.charAt( pos - 1 ) == '\n' ) {
		  break;
		}
		--pos;
	      }
	      int eol = text.indexOf( '\n', pos );
	      if( eol >= pos ) {
		processLineAction( text.substring( pos, eol ) );
	      } else {
		processLineAction( text.substring( pos ) );
	      }
	    }
	  }
	}
      }
      e.consume();
    } else {
      super.mouseClicked( e );
    }
  }


  public void windowClosed( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      EditText editText = correspondingEditText;
      if( editText != null ) {
	EditFrm editFrm = editText.getEditFrm();
	if( editFrm != null )
	  editFrm.doPrgCancel();
      }
    }
  }


	/* --- private Methoden --- */

  private void appendToText( final String text )
  {
    final JTextArea fldText = this.fldText;
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    fldText.append( text != null ? text : "null" );
		  }
		} );
  }


  /*
   * Diese Methode wird ausgefuehrt,
   * wenn auf die uebergebene Zeile doppelt geklickt wurde.
   */
  private void processLineAction( String text )
  {
    EditText editText = this.correspondingEditText;
    if( (editText != null) && (text != null) ) {
      int pos = text.indexOf( "Zeile" );
      if( pos >= 0 ) {
	pos += 5;		// Position hinter dem Wort Zeile
	int len = text.length();
	while( pos < len ) {
	  if( !Character.isWhitespace( text.charAt( pos ) ) ) {
	    break;
	  }
	  pos++;
	}
	if( pos < len ) {
	  char ch = text.charAt( pos++ );
	  if( (ch >= '0') && (ch <= '9') ) {
	    int lineNum = ch - '0';
	    while( pos < len ) {
	      ch = text.charAt( pos++ );
	      if( (ch < '0') || (ch > '9') ) {
		break;
	      }
	      lineNum = (lineNum * 10) + (ch - '0');
	    }
	    editText.gotoLine( lineNum );
	  }
	}
      }
    }
  }
}

