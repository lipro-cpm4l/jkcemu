/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer Reassembler
 */

package jkcemu.tools;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.print.*;
import z80emu.*;


public class ReassFrm extends BasicFrm implements CaretListener
{
  private static final int COL_MNEMONIC = 23;
  private static final int COL_ARGS     = 31;
  private static final int COL_REMARK   = 49;

  private ScreenFrm      screenFrm;
  private EmuSys         emuSys;
  private Z80Memory      memory;
  private File           lastFile;
  private String         textFind;
  private JTextArea      textArea;
  private JTextComponent selectionFld;
  private JMenuItem      mnuReass;
  private JMenuItem      mnuPrintOptions;
  private JMenuItem      mnuPrint;
  private JMenuItem      mnuSaveAs;
  private JMenuItem      mnuClose;
  private JMenuItem      mnuCopy;
  private JMenuItem      mnuFind;
  private JMenuItem      mnuFindNext;
  private JMenuItem      mnuSelectAll;
  private JMenuItem      mnuHelpContent;
  private JTextField     fldBegAddr;
  private JTextField     fldEndAddr;
  private HexDocument    docBegAddr;
  private HexDocument    docEndAddr;


  public ReassFrm(
		ScreenFrm screenFrm,
		EmuSys    emuSys,
		Z80Memory memory )
  {
    this.screenFrm    = screenFrm;
    this.emuSys       = emuSys;
    this.memory       = memory;
    this.lastFile     = null;
    this.textFind     = null;
    this.selectionFld = null;
    this.textArea     = new JTextArea();
    this.textArea.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
    setTitle( "JKCEMU Reassembler" );
    Main.updIcon( this );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( 'D' );
    mnuBar.add( mnuFile );

    this.mnuReass = createJMenuItem(
			"Reassemblieren",
			KeyStroke.getKeyStroke(
					KeyEvent.VK_R,
					InputEvent.CTRL_MASK ) );
    mnuFile.add( this.mnuReass );
    mnuFile.addSeparator();

    this.mnuPrintOptions = createJMenuItem( "Druckoptionen..." );
    mnuFile.add( this.mnuPrintOptions );

    this.mnuPrint = createJMenuItem(
			"Drucken...",
			KeyStroke.getKeyStroke(
				KeyEvent.VK_P,
				InputEvent.CTRL_MASK ) );
    this.mnuPrint.setEnabled( false );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuSaveAs = createJMenuItem(
		"Speichern unter...",
		KeyStroke.getKeyStroke(
			KeyEvent.VK_S,
			InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ) );
    this.mnuSaveAs.setEnabled( false );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( 'B' );
    mnuBar.add( mnuEdit );

    this.mnuCopy = createJMenuItem(
			"Kopieren",
			KeyStroke.getKeyStroke(
				KeyEvent.VK_C,
				InputEvent.CTRL_MASK ) );
    this.mnuCopy.setEnabled( false );
    mnuEdit.add( this.mnuCopy );
    mnuEdit.addSeparator();

    this.mnuFind = createJMenuItem(
			"Suchen...",
			KeyStroke.getKeyStroke(
				KeyEvent.VK_F,
				InputEvent.CTRL_MASK ) );
    this.mnuFind.setEnabled( false );
    mnuEdit.add( this.mnuFind );

    this.mnuFindNext = createJMenuItem(
		"Weitersuchen",
		KeyStroke.getKeyStroke(
			KeyEvent.VK_F,
			InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ) );
    this.mnuFindNext.setEnabled( false );
    mnuEdit.add( this.mnuFindNext );
    mnuEdit.addSeparator();

    this.mnuSelectAll = createJMenuItem( "Alles ausw\u00E4hlen" );
    this.mnuSelectAll.setEnabled( false );
    mnuEdit.add( this.mnuSelectAll );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


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
    this.textArea.setColumns( 58 );
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


  public Z80Memory getZ80Memory()
  {
    return this.memory;
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
      else if( (src == this.fldEndAddr) || (src == this.mnuReass) ) {
	rv = true;
	doReass();
      }
      else if( src == this.mnuPrintOptions ) {
	rv = true;
	PrintOptionsDlg.showPrintOptionsDlg( this, true, true );
      }
      else if( src == this.mnuPrint ) {
	rv = true;
	doPrint();
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
      else if( src == this.mnuFind ) {
	rv = true;
	doFind();
      }
      else if( src == this.mnuFindNext ) {
	rv = true;
	doFindNext();
      }
      else if( src == this.mnuSelectAll ) {
	rv = true;
	this.textArea.requestFocus();
	this.textArea.selectAll();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	this.screenFrm.showHelp( "/help/tools/reassembler.htm" );
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


	/* --- Aktionen --- */

  private void doFind()
  {
    String selectedText = this.textArea.getSelectedText();
    if( selectedText != null ) {
      if( selectedText.length() == 0 )
	selectedText = null;
    }

    String[]    options = { "Suchen", "Abbrechen" };
    JOptionPane pane    = new JOptionPane(
				"Suchen nach:",
				JOptionPane.PLAIN_MESSAGE );
    pane.setOptions( options );
    pane.setWantsInput( true );
    if( selectedText != null ) {
      pane.setInitialSelectionValue( selectedText );
    } else {
      if( this.textFind != null ) {
	pane.setInitialSelectionValue( this.textFind );
      }
    }
    pane.setInitialValue( options[ 0 ] );
    pane.createDialog( this, "Suchen" ).setVisible( true );
    Object value = pane.getValue();
    if( value != null ) {
      if( value.equals( options[ 0 ] ) ) {
	value = pane.getInputValue();
	if( value != null ) {
	  String text = value.toString();
	  if( text != null ) {
	    if( text.length() > 0 ) {
	      this.textFind = text;
	      findText( Math.max(
				this.textArea.getCaretPosition(),
				this.textArea.getSelectionEnd() ) );
	    }
	  }
	}
      }
    }
  }


  private void doFindNext()
  {
    if( this.textFind == null ) {
      doFind();
    } else {
      findText( Math.max(
			this.textArea.getCaretPosition(),
			this.textArea.getSelectionEnd() ) );
    }
  }


  private void doPrint()
  {
    PrintUtil.doPrint(
	this,
	new PlainTextPrintable(
		this.textArea.getText(),
		this.textArea.getTabSize(),
		this.lastFile != null ? this.lastFile.getName() : null ),
	"JKCEMU - Reassembler" );
  }


  private void doReass()
  {
    try {
      int    addr    = this.docBegAddr.intValue();
      int    endAddr = addr;
      String text    = this.fldEndAddr.getText();
      if( text != null ) {
	if( text.length() > 0 )
	  endAddr = this.docEndAddr.intValue();
      }
      StringBuilder buf = new StringBuilder( 0x4000 );
      while( addr <= endAddr ) {
	int len = 0;
	if( this.emuSys != null ) {
	  len = this.emuSys.reassembleSysCall(
					addr,
					buf,
					COL_MNEMONIC,
					COL_ARGS,
					COL_REMARK );
	}
	if( len > 0 ) {
	  addr += len;
	} else {
	  int begOfLine = buf.length();
	  buf.append( String.format( "%04X", addr ) );

	  int b0        = this.memory.getMemByte( addr, true );
	  boolean b1_m1 = ((b0 == 0xED) || (b0 == 0xDD) || (b0 == 0xFD));

	  Z80ReassInstr instruction = Z80Reassembler.reassInstruction(
				addr,
				b0,
				this.memory.getMemByte( addr + 1, b1_m1 ),
				this.memory.getMemByte( addr + 2, false ),
				this.memory.getMemByte( addr + 3, false ) );
	  if( instruction != null ) {
	    buf.append( (char) '\u0020' );
	    len = instruction.getLength();
	    for( int i = 0; i < len; i++ ) {
	      buf.append( (char) '\u0020' );
	      buf.append( String.format(
				"%02X",
				this.memory.getMemByte(
					addr++,
					(i == 0) || ((i == 1) && b1_m1) ) ) );
	    }

	    String s = instruction.getName();
	    if( s != null ) {
	      appendSpaces( buf, begOfLine + COL_MNEMONIC );
	      buf.append( s );

	      s = instruction.getArg1();
	      if( s != null ) {
		appendSpaces( buf, begOfLine + COL_ARGS );
		buf.append( s );

		s = instruction.getArg2();
		if( s != null ) {
		  buf.append( (char) ',' );
		  buf.append( s );
		}
	      }
	    }
	  } else {
	    buf.append( String.format(
				"  %02X",
				this.memory.getMemByte( addr++, true ) ) );
	  }
	  buf.append( (char) '\n' );
	}
      }
      setText( buf.toString() );
      this.textArea.requestFocus();
      if( text != null ) {
	if( text.length() > 0 ) {
	  this.mnuPrint.setEnabled( true );
	  this.mnuSaveAs.setEnabled( true );
	  this.mnuFind.setEnabled( true );
	  this.mnuSelectAll.setEnabled( true );
	}
      }
    }
    catch( NumberFormatException ex ) {
      BasicDlg.showErrorDlg( this, ex.getMessage(), "Eingabefehler" );
    }
  }


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


	/* --- private Methoden --- */

  private static void appendSpaces( StringBuilder buf, int endPos )
  {
    for( int i = buf.length(); i < endPos; i++ )
      buf.append( (char) '\u0020' );
  }


  private boolean findText( int startPos )
  {
    boolean rv       = false;
    String  textFind = this.textFind;

    if( (textFind != null) && (textFind.length() > 0) ) {
      this.mnuFindNext.setEnabled( true );

      String textBase = this.textArea.getText();
      if( textBase == null ) {
	textBase = "";
      }
      textFind = textFind.toUpperCase();
      textBase = textBase.toUpperCase();
      if( startPos < 0 ) {
	startPos = 0;
      }
      int len     = textBase.length();
      int posFind = -1;
      if( startPos < len ) {
	posFind = textBase.indexOf( textFind, startPos );
      }
      if( (posFind >= 0) && (posFind < len) ) {
	toFront();
	this.textArea.requestFocus();
	this.textArea.setCaretPosition( posFind );
	this.textArea.select( posFind, posFind + textFind.length() );
	rv = true;
      }
    }
    if( !rv ) {
      if( startPos > 0 ) {
	rv = findText( 0 );
      } else {
	BasicDlg.showInfoDlg( this, "Text nicht gefunden!", "Text suchen" );
      }
    }
    return rv;
  }


  private void setText( String text )
  {
    try {
      this.textArea.setText( text );
      this.textArea.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }
}

