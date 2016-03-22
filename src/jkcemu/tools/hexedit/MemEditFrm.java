/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Speichereditor
 */

package jkcemu.tools.hexedit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.print.*;
import z80emu.Z80Memory;


public class MemEditFrm extends AbstractHexCharFrm
{
  private Z80Memory   memory;
  private int         begAddr;
  private int         endAddr;
  private int         savedAddr;
  private File        lastFile;
  private String      textFind;
  private JMenuItem   mnuRefresh;
  private JMenuItem   mnuClose;
  private JMenuItem   mnuBytesCopyHex;
  private JMenuItem   mnuBytesCopyAscii;
  private JMenuItem   mnuBytesCopyDump;
  private JMenuItem   mnuPrintOptions;
  private JMenuItem   mnuPrint;
  private JMenuItem   mnuOverwrite;
  private JMenuItem   mnuSaveAddr;
  private JMenuItem   mnuGotoSavedAddr;
  private JMenuItem   mnuSelectToSavedAddr;
  private JMenuItem   mnuChecksum;
  private JMenuItem   mnuFind;
  private JMenuItem   mnuFindNext;
  private JMenuItem   mnuHelpContent;
  private JTextField  fldBegAddr;
  private JTextField  fldEndAddr;
  private HexDocument docBegAddr;
  private HexDocument docEndAddr;


  public MemEditFrm( Z80Memory memory )
  {
    this.memory    = memory;
    this.begAddr   = -1;
    this.endAddr   = -1;
    this.savedAddr = -1;
    this.lastFile  = null;
    this.textFind  = null;
    setTitle( "JKCEMU Speichereditor" );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuRefresh = createJMenuItem( "Aktualisieren" );
    mnuFile.add( this.mnuRefresh );
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

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuBytesCopyHex = createJMenuItem(
		"Ausgw\u00E4hlte Bytes als Hexadezimalzahlen kopieren" );
    this.mnuBytesCopyHex.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopyHex );

    this.mnuBytesCopyAscii = createJMenuItem(
		"Ausgw\u00E4hlte Bytes als ASCII-Text kopieren" );
    this.mnuBytesCopyAscii.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopyAscii );

    this.mnuBytesCopyDump = createJMenuItem(
		"Ausgw\u00E4hlte Bytes als Hex-ASCII-Dump kopieren" );
    this.mnuBytesCopyDump.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopyDump );
    mnuEdit.addSeparator();

    this.mnuOverwrite = createJMenuItem(
		"Bytes \u00FCberschreiben...",
		KeyStroke.getKeyStroke( KeyEvent.VK_O, Event.CTRL_MASK ) );
    this.mnuOverwrite.setEnabled( false );
    mnuEdit.add( this.mnuOverwrite );
    mnuEdit.addSeparator();

    this.mnuSaveAddr = createJMenuItem( "Adresse merken" );
    this.mnuSaveAddr.setEnabled( false );
    mnuEdit.add( this.mnuSaveAddr );

    this.mnuGotoSavedAddr = createJMenuItem(
                                "Zur gemerkten Adresse springen" );
    this.mnuGotoSavedAddr.setEnabled( false );
    mnuEdit.add( this.mnuGotoSavedAddr );

    this.mnuSelectToSavedAddr = createJMenuItem(
                                "Bis zur gemerkten Adresse ausw\u00E4hlen" );
    this.mnuSelectToSavedAddr.setEnabled( false );
    mnuEdit.add( this.mnuSelectToSavedAddr );
    mnuEdit.addSeparator();

    this.mnuChecksum = createJMenuItem( "Pr\u00FCfsumme/Hash-Wert..." );
    this.mnuChecksum.setEnabled( false );
    mnuEdit.add( this.mnuChecksum );
    mnuEdit.addSeparator();

    this.mnuFind = createJMenuItem(
		"Suchen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_F, Event.CTRL_MASK ) );
    this.mnuFind.setEnabled( false );
    mnuEdit.add( this.mnuFind );

    this.mnuFindNext = createJMenuItem(
		"Weitersuchen",
		KeyStroke.getKeyStroke( KeyEvent.VK_F3, 0 ) );
    this.mnuFindNext.setEnabled( false );
    mnuEdit.add( this.mnuFindNext );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


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

    // Adresseingabe
    add( new JLabel( "Anfangsadresse:" ), gbc );

    this.docBegAddr = new HexDocument( 4, "Anfangsadresse" );
    this.fldBegAddr = new JTextField( this.docBegAddr, "", 4 );
    this.fldBegAddr.addActionListener( this );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.gridx++;
    add( this.fldBegAddr, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( new JLabel( "Endadresse:" ), gbc );

    this.docEndAddr = new HexDocument( 4, "Endadresse" );
    this.fldEndAddr = new JTextField( this.docEndAddr, "", 4 );
    this.fldEndAddr.addActionListener( this );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.gridx++;
    add( this.fldEndAddr, gbc );

    // Hex-ASCII-Anzeige
    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.fill      = GridBagConstraints.BOTH;
    gbc.weightx   = 1.0;
    gbc.weighty   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( createHexCharFld(), gbc );
    this.hexCharFld.setPreferredSize(
	new Dimension( this.hexCharFld.getDefaultPreferredWidth(), 300 ) );

    // Anzeige der Cursor-Position
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( createCaretPosFld( "Adresse" ), gbc );

    // Anzeige der Dezimalwerte der Bytes ab Cursor-Position
    gbc.gridy++;
    add( createValueFld(), gbc );


    // sonstiges
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );
    this.hexCharFld.setPreferredSize( null );
  }


  public Z80Memory getZ80Memory()
  {
    return this.memory;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.fldBegAddr ) {
	rv = true;
	this.fldEndAddr.requestFocus();
      } else if( (src == this.fldEndAddr) || (src == this.mnuRefresh) ) {
	rv = true;
	doRefresh();
      } else if( src == this.mnuPrintOptions ) {
	rv = true;
	PrintOptionsDlg.showPrintOptionsDlg( this, true, false );
      } else if( src == this.mnuPrint ) {
	rv = true;
	PrintUtil.doPrint( this, this, "JKCEMU Speichereditor" );
      } else if( src == this.mnuClose ) {
	rv = true;
	doClose();
      } else if( src == this.mnuBytesCopyHex ) {
	rv = true;
	this.hexCharFld.copySelectedBytesAsHex();
      } else if( src == this.mnuBytesCopyAscii ) {
	rv = true;
	this.hexCharFld.copySelectedBytesAsAscii();
      } else if( src == this.mnuBytesCopyDump ) {
	rv = true;
	this.hexCharFld.copySelectedBytesAsDump();
      } else if( src == this.mnuOverwrite ) {
	rv = true;
	doBytesOverwrite();
      } else if( src == this.mnuSaveAddr ) {
        rv = true;
        doSaveAddr();
      } else if( src == this.mnuGotoSavedAddr ) {
        rv = true;
        doGotoSavedAddr( false );
      } else if( src == this.mnuSelectToSavedAddr ) {
        rv = true;
        doGotoSavedAddr( true );
      } else if( src == this.mnuChecksum ) {
	rv = true;
	doChecksum();
      } else if( src == this.mnuFind ) {
	rv = true;
	doFind();
      } else if( src == this.mnuFindNext ) {
	rv = true;
	doFindNext();
      } else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.open( "/help/tools/memeditor.htm" );
      } else {
	rv = super.doAction( e );
      }
    }
    return rv;
  }


  @Override
  public int getAddrOffset()
  {
    return this.begAddr > 0 ? this.begAddr : 0;
  }


  @Override
  public int getDataByte( int idx )
  {
    int rv = 0;
    if( (this.begAddr >= 0) && ((this.begAddr + idx) <= this.endAddr) ) {
      rv = this.memory.getMemByte( this.begAddr + idx, false );
    }
    return rv;
  }


  @Override
  public int getDataLength()
  {
    return (this.begAddr >= 0) && (this.begAddr <= this.endAddr) ?
					this.endAddr - this.begAddr + 1
					: 0;
  }


  @Override
  protected void setContentActionsEnabled( boolean state )
  {
    this.mnuPrint.setEnabled( state );
    this.mnuFind.setEnabled( state );
  }


  @Override
  protected void setFindNextActionsEnabled( boolean state )
  {
    this.mnuFindNext.setEnabled( state );
  }


  @Override
  protected void setSelectedByteActionsEnabled( boolean state )
  {
    this.mnuBytesCopyHex.setEnabled( state );
    this.mnuBytesCopyAscii.setEnabled( state );
    this.mnuBytesCopyDump.setEnabled( state );
    this.mnuOverwrite.setEnabled( state );
    this.mnuChecksum.setEnabled( state );
    this.mnuSaveAddr.setEnabled( state );
    this.mnuSelectToSavedAddr.setEnabled( state && (this.savedAddr >= 0) );
  }


	/* --- Aktionen --- */

  private void doBytesOverwrite()
  {
    if( (this.begAddr >= 0) && (this.begAddr <= this.endAddr) ) {
      int caretPos = this.hexCharFld.getCaretPosition();
      if( (caretPos >= 0) && (this.begAddr + caretPos <= this.endAddr) ) {
	ReplyBytesDlg dlg = new ReplyBytesDlg(
					this,
					"Bytes \u00FCberschreiben",
					this.lastInputFmt,
					this.lastBigEndian,
					null );
	dlg.setVisible( true );
	byte[] a = dlg.getApprovedBytes();
	if( a != null ) {
	  if( a.length > 0 ) {
	    this.lastInputFmt  = dlg.getApprovedInputFormat();
	    this.lastBigEndian = dlg.getApprovedBigEndian();

	    boolean failed = false;
	    int     src    = 0;
	    int     addr   = this.begAddr + caretPos;
	    while( src < a.length ) {
	      if( addr > 0xFFFF ) {
		BasicDlg.showWarningDlg(
			this,
			"Die von Ihnen eingegebenen Bytes gehen \u00FCber"
				+ " die Adresse FFFF hinaus.\n"
				+ "Es werden nur die Bytes bis FFFF"
				+ " ge\u00E4ndert." );
		break;
	      } else {
		if( !this.memory.setMemByte( addr, a[ src ] ) ) {
		  String msg = String.format(
			"Die Speicherzelle mit der Adresse %04X\n"
				+  "konnte nicht ge\u00E4ndert werden.",
			addr );
		  if( src == (a.length - 1) ) {
		    BasicDlg.showErrorDlg( this, msg );
		  } else {
		    boolean     cancel  = true;
		    String[]    options = { "Weiter", "Abbrechen" };
		    JOptionPane pane    = new JOptionPane(
						msg,
						JOptionPane.ERROR_MESSAGE );
		    pane.setOptions( options );
		    pane.createDialog( this, "Fehler" ).setVisible( true );
		    Object value = pane.getValue();
		    if( value != null ) {
		      if( value.equals( options[ 0 ] ) ) {
			cancel = false;
		      }
		    }
		    if( cancel ) {
		      break;
		    }
		  }
		}
	      }
	      addr++;
	      src++;
	    }
	    if( addr > this.endAddr ) {
	      this.endAddr = addr - 1;
	      this.docEndAddr.setValue( this.endAddr, 4 );
	    }
	    updView();
	    setSelection( caretPos, caretPos + a.length - 1 );
	  }
	}
      }
    }
  }


  private void doGotoSavedAddr( boolean moveOp )
  {
    if( (this.savedAddr >= 0)
	&& (this.savedAddr >= this.begAddr)
	&& (this.savedAddr <= this.endAddr) )
    {
      this.hexCharFld.setCaretPosition(
				this.savedAddr - this.begAddr,
				moveOp );
      updCaretPosFields();
    }
  }


  private void doRefresh()
  {
    try {
      int begAddr  = this.docBegAddr.intValue() & 0xFFFF;
      this.endAddr = this.docEndAddr.intValue() & 0xFFFF;
      this.begAddr = begAddr;
      updView();
    }
    catch( NumberFormatException ex ) {
      BasicDlg.showErrorDlg( this, ex.getMessage(), "Eingabefehler" );
    }
  }


  private void doSaveAddr()
  {
    if( this.begAddr >= 0 ) {
      int caretPos = this.hexCharFld.getCaretPosition();
      if( caretPos >= 0 ) {
	this.savedAddr = this.begAddr + caretPos;
	this.mnuGotoSavedAddr.setEnabled( true );
	this.mnuSelectToSavedAddr.setEnabled( true );
      }
    }
  }
}
