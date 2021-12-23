/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zur Ausgabe von Log-Meldungen
 */

package jkcemu.text;

import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import jkcemu.Main;
import jkcemu.base.BaseFrm;
import jkcemu.base.GUIFactory;


public class LogFrm extends BaseFrm implements Appendable, CaretListener
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


    // Menu Datei
    JMenu mnuFile     = createMenuFile();
    this.mnuFileClose = createMenuItemClose();
    mnuFile.add( this.mnuFileClose );


    // Menu Bearbeiten
    JMenu mnuEdit = createMenuEdit();

    this.mnuEditCopy = createMenuItemCopy( true );
    mnuEdit.add( this.mnuEditCopy );
    mnuEdit.addSeparator();

    this.mnuEditSelectAll = createMenuItemSelectAll( true );
    mnuEdit.add( this.mnuEditSelectAll );


    // Menu zusammenbauen
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuEdit ) );


    // Fensterinhalt
    setLayout( new BorderLayout() );

    this.fldText = GUIFactory.createCodeArea( 10, 50 );
    this.fldText.setEditable( false );
    this.fldText.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.fldText.addCaretListener( this );
    this.fldText.addMouseListener( this );
    add(
	GUIFactory.createScrollPane(
		this.fldText,
		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS ),
	BorderLayout.CENTER );

    // Fenstergroesse
    pack();
    this.fldText.setColumns( 0 );
    this.fldText.setRows( 0 );
    setResizable( true );
  }


  public void reset( EditText correspondingEditText, String title )
  {
    setTitle( title );
    this.correspondingEditText = correspondingEditText;
    this.fldText.setText( "" );
  }


	/* --- Appendable --- */

  @Override
  public Appendable append( char ch )
  {
    appendToText( String.valueOf( ch ) );
    return this;
  }


  @Override
  public Appendable append( CharSequence csq )
  {
    appendToText( csq != null ? csq.toString() : "null" );
    return this;
  }


  @Override
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

  @Override
  public void caretUpdate( CaretEvent e )
  {
    int selStart = this.fldText.getSelectionStart();
    this.mnuEditCopy.setEnabled(
	(selStart >= 0) && (selStart < this.fldText.getSelectionEnd()) );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
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


  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getComponent() == this.fldText)
	&& (e.getClickCount() > 1) )
    {
      Point point = e.getPoint();
      if( point != null ) {
	int pos = TextUtil.viewToModel( this.fldText, point );
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


  @Override
  public void windowClosed( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      EditText editText = correspondingEditText;
      if( editText != null ) {
	TextEditFrm textEditFrm = editText.getTextEditFrm();
	if( textEditFrm != null ) {
	  textEditFrm.doPrgCancel();
	}
      }
    }
  }


	/* --- private Methoden --- */

  private void appendToText( final String text )
  {
    final JTextArea fldText = this.fldText;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
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
  private void processLineAction( String line )
  {
    EditText editText = this.correspondingEditText;
    if( (editText != null) && (line != null) ) {
      int pos = line.indexOf( "Zeile" );
      if( pos >= 0 ) {
	/*
	 * Wenn vor dem Wort 'Zeile' ein Doppelpunkt steht,
	 * enthaelt die Zeile einen Dateinamen, d.h.,
	 * der Fehler befindet sich in einer inkludierten Quelltextdatei.
	 * In dem Fall nur dann zu der entsprechenden Zeile
	 * im aktuellen Quelltext springen,
	 * wenn der Dateiname uebereinstimmt.
	 */
	boolean fileOK = true;
	int     dpPos  = line.indexOf( ':' );
	if( (dpPos >= 0) && (dpPos < pos) ) {
	  fileOK = false;
	  if( dpPos > 0 ) {
	    File file = editText.getFile();
	    if( file != null ) {
	      String fileName = file.getName();
	      if( fileName != null ) {
		fileOK = fileName.equals( line.substring( 0, dpPos ) );
	      }
	    }
	  }
	}
	if( fileOK ) {
	  pos += 5;		// Position hinter dem Wort Zeile
	  int len = line.length();
	  while( pos < len ) {
	    if( !Character.isWhitespace( line.charAt( pos ) ) ) {
	      break;
	    }
	    pos++;
	  }
	  if( pos < len ) {
	    char ch = line.charAt( pos++ );
	    if( (ch >= '0') && (ch <= '9') ) {
	      int lineNum = ch - '0';
	      while( pos < len ) {
		ch = line.charAt( pos++ );
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
}
