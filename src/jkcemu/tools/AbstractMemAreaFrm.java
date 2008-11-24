/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abstraktes Fenster fuer die Anzeige von Speicherbereichen
 */

package jkcemu.tools;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.text.ParseException;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import jkcemu.Main;
import jkcemu.base.*;
import z80emu.Z80MemView;


public abstract class AbstractMemAreaFrm
				extends BasicFrm
				implements CaretListener
{
  protected Z80MemView memory;

  private File           lastFile;
  private JTextArea      textArea;
  private JTextComponent selectionFld;
  private JMenuItem      mnuAction;
  private JMenuItem      mnuSaveAs;
  private JMenuItem      mnuClose;
  private JMenuItem      mnuCopy;
  private JMenuItem      mnuSelectAll;
  private JTextField     fldBegAddr;
  private JTextField     fldEndAddr;
  private HexDocument    docBegAddr;
  private HexDocument    docEndAddr;


  protected AbstractMemAreaFrm(
			Z80MemView memory,
			String     title,
			String     actionText,
			KeyStroke  actionKeyStroke,
			int        resultCols )
  {
    setTitle( title );
    Main.updIcon( this );
    this.memory       = memory;
    this.lastFile     = null;
    this.selectionFld = null;
    this.textArea     = new JTextArea();
    this.textArea.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( 'D' );
    mnuBar.add( mnuFile );

    this.mnuAction = new JMenuItem( actionText );
    if( actionKeyStroke != null ) {
      this.mnuAction.setAccelerator( actionKeyStroke );
    }
    this.mnuAction.addActionListener( this );
    mnuFile.add( this.mnuAction );
    mnuFile.addSeparator();

    this.mnuSaveAs = new JMenuItem( "Speichern unter..." );
    this.mnuSaveAs.setAccelerator(
		KeyStroke.getKeyStroke(
			KeyEvent.VK_S,
			InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ) );
    this.mnuSaveAs.setEnabled( false );
    this.mnuSaveAs.addActionListener( this );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuClose = new JMenuItem( "Schlie\u00DFen" );
    this.mnuClose.addActionListener( this );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( 'B' );
    mnuBar.add( mnuEdit );

    this.mnuCopy = new JMenuItem( "Kopieren" );
    this.mnuCopy.setAccelerator( KeyStroke.getKeyStroke(
					KeyEvent.VK_C,
					InputEvent.CTRL_MASK ) );
    this.mnuCopy.setEnabled( false );
    this.mnuCopy.addActionListener( this );
    mnuEdit.add( this.mnuCopy );
    mnuEdit.addSeparator();

    this.mnuSelectAll = new JMenuItem( "Alles ausw\u00E4hlen" );
    this.mnuSelectAll.setEnabled( false );
    this.mnuSelectAll.addActionListener( this );
    mnuEdit.add( this.mnuSelectAll );


    // Fensterinhalt
    setLayout( new BorderLayout() );


    // Kopfbereich
    JPanel panelHead = new JPanel( new GridBagLayout() );
    add( panelHead, BorderLayout.NORTH );

    GridBagConstraints gbcHead = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    panelHead.add( new JLabel( "Anfangsadresse:" ), gbcHead );

    this.fldBegAddr = new JTextField( 4 );
    this.fldBegAddr.addActionListener( this );
    this.fldBegAddr.addCaretListener( this );
    gbcHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx = 0.5;
    gbcHead.gridx++;
    panelHead.add( this.fldBegAddr, gbcHead );

    gbcHead.fill    = GridBagConstraints.NONE;
    gbcHead.weightx = 0.0;
    gbcHead.gridx++;
    panelHead.add( new JLabel( "Endadresse:" ), gbcHead );

    this.fldEndAddr = new JTextField( 4 );
    this.fldEndAddr.addActionListener( this );
    this.fldEndAddr.addCaretListener( this );
    gbcHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx = 0.5;
    gbcHead.gridx++;
    panelHead.add( this.fldEndAddr, gbcHead );

    this.docBegAddr = new HexDocument( this.fldBegAddr, 4, "Anfangsadresse" );
    this.docEndAddr = new HexDocument( this.fldEndAddr, 4, "Endadresse" );


    // Ergebnisbereich
    this.textArea.setColumns( resultCols );
    this.textArea.setRows( 20 );
    this.textArea.setEditable( false );
    this.textArea.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.textArea.addCaretListener( this );
    add( new JScrollPane( this.textArea ), BorderLayout.CENTER );


    // sonstiges
    setResizable( true );
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setLocationByPlatform( true );
      this.fldBegAddr.setColumns( 0 );
      this.fldEndAddr.setColumns( 0 );
      this.textArea.setColumns( 0 );
      this.textArea.setRows( 0 );
    }
  }


  protected abstract void doAction();


  protected String getEndAddrText()
  {
    return this.fldEndAddr.getText();
  }


  protected int getBegAddr() throws NumberFormatException
  {
    return this.docBegAddr.intValue();
  }


  protected int getEndAddr() throws NumberFormatException
  {
    return this.docEndAddr.intValue();
  }


  protected void setResult( String text )
  {
    setText( text );
    this.textArea.requestFocus();
    if( text != null ) {
      if( text.length() > 0 ) {
	this.mnuSaveAs.setEnabled( true );
	this.mnuSelectAll.setEnabled( true );
      }
    }
  }


  protected void setText( String text )
  {
    try {
      this.textArea.setText( text );
      this.textArea.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }


	/* --- CaretListener --- */

  public void caretUpdate( CaretEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src instanceof JTextComponent ) {
	this.selectionFld = (JTextComponent) src;
	int begPos        = this.selectionFld.getSelectionStart();
	this.mnuCopy.setEnabled( (begPos >= 0)
		&& (begPos < this.selectionFld.getSelectionEnd()) );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  public boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.fldBegAddr ) {
	rv = true;
	this.fldEndAddr.requestFocus();
      }
      else if( (src == this.fldEndAddr) || (src == this.mnuAction) ) {
	rv = true;
	doAction();
      }
      else if( src == this.mnuSaveAs ) {
	rv = true;
	doSaveAs();
      }
      else if( src == this.mnuClose ) {
	rv = true;
	doClose();
      }
      else if( src == this.mnuCopy ) {
	rv = true;
	if( this.selectionFld != null ) {
	  this.selectionFld.copy();
	}
      }
      else if( src == this.mnuSelectAll ) {
	rv = true;
	this.textArea.requestFocus();
	this.textArea.selectAll();
      }
    }
    return rv;
  }


  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.fldBegAddr.setText( "" );
      this.fldEndAddr.setText( "" );
      setText( "" );
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void doSaveAs()
  {
    File file = EmuUtil.showFileSaveDlg(
				this,
				"Textdatei speichern",
				this.lastFile != null ?
					this.lastFile
					: Main.getLastPathFile( "text" ),
				EmuUtil.getTextFileFilter() );
    if( file != null ) {
      try {
	Writer out = null;
	try {
	  out = new BufferedWriter( new FileWriter( file ) );
	  this.textArea.write( out );
	  out.close();
	  out           = null;
	  this.lastFile = file;
	  Main.setLastFile( file, "text" );
	}
	finally {
	  if( out != null ) {
	    try {
	      out.close();
	    }
	    catch( IOException ex ) {}
	  }
	}
      }
      catch( IOException ex ) {
	BasicDlg.showErrorDlg( this, ex );
      }
    }
  }
}

