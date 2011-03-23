/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hex-Editor
 */

package jkcemu.tools.hexedit;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.naming.SizeLimitExceededException;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.print.*;


public class FileEditFrm
		extends AbstractHexCharFrm
		implements DropTargetListener
{
  private static final int BUF_EXTEND = 0x2000;

  private ScreenFrm screenFrm;
  private File      file;
  private byte[]    fileBytes;
  private int       fileLen;
  private int       savedPos;
  private boolean   dataChanged;
  private JMenuItem mnuNew;
  private JMenuItem mnuOpen;
  private JMenuItem mnuSave;
  private JMenuItem mnuSaveAs;
  private JMenuItem mnuPrintOptions;
  private JMenuItem mnuPrint;
  private JMenuItem mnuClose;
  private JMenuItem mnuTextCopy;
  private JMenuItem mnuBytesCopy;
  private JMenuItem mnuBytesInvert;
  private JMenuItem mnuBytesSave;
  private JMenuItem mnuBytesAppend;
  private JMenuItem mnuBytesInsert;
  private JMenuItem mnuBytesOverwrite;
  private JMenuItem mnuBytesRemove;
  private JMenuItem mnuFileInsert;
  private JMenuItem mnuFileAppend;
  private JMenuItem mnuSavePos;
  private JMenuItem mnuGotoSavedPos;
  private JMenuItem mnuSelectToSavedPos;
  private JMenuItem mnuChecksum;
  private JMenuItem mnuFind;
  private JMenuItem mnuFindNext;
  private JMenuItem mnuHelpContent;
  private JButton   btnNew;
  private JButton   btnOpen;
  private JButton   btnSave;
  private JButton   btnFind;


  public FileEditFrm( ScreenFrm screenFrm )
  {
    this.screenFrm   = screenFrm;
    this.file        = null;
    this.fileBytes   = new byte[ 0x100 ];
    this.fileLen     = 0;
    this.savedPos    = -1;
    this.dataChanged = false;
    updTitle();


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuNew = createJMenuItem( "Neu" );
    mnuFile.add( this.mnuNew );

    this.mnuOpen = createJMenuItem( "\u00D6ffnen..." );
    mnuFile.add( this.mnuOpen );
    mnuFile.addSeparator();

    this.mnuSave = createJMenuItem(
		"Speichern",
		KeyStroke.getKeyStroke( KeyEvent.VK_S, Event.CTRL_MASK ) );
    this.mnuSave.setEnabled( false );
    mnuFile.add( this.mnuSave );

    this.mnuSaveAs = createJMenuItem( "Speichern unter..." );
    this.mnuSaveAs.setEnabled( false );
    mnuFile.add( this.mnuSaveAs );
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

    this.mnuBytesCopy = createJMenuItem( "Ausgew\u00E4hlte Bytes kopieren" );
    this.mnuBytesCopy.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopy );

    this.mnuTextCopy = createJMenuItem( "Ausgew\u00E4hlten Text kopieren" );
    this.mnuTextCopy.setEnabled( false );
    mnuEdit.add( this.mnuTextCopy );
    mnuEdit.addSeparator();

    this.mnuBytesInsert = createJMenuItem(
		"Bytes einf\u00FCgen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_I, Event.CTRL_MASK ) );
    this.mnuBytesInsert.setEnabled( false );
    mnuEdit.add( this.mnuBytesInsert );

    this.mnuBytesOverwrite = createJMenuItem(
		"Bytes \u00FCberschreiben...",
		KeyStroke.getKeyStroke( KeyEvent.VK_O, Event.CTRL_MASK ) );
    this.mnuBytesOverwrite.setEnabled( false );
    mnuEdit.add( this.mnuBytesOverwrite );

    this.mnuBytesAppend = createJMenuItem(
		"Bytes am Ende anh\u00E4ngen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_E, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuBytesAppend );
    mnuEdit.addSeparator();

    this.mnuBytesSave = createJMenuItem(
				"Ausgew\u00E4hlte Bytes speichern..." );
    this.mnuBytesSave.setEnabled( false );
    mnuEdit.add( this.mnuBytesSave );

    this.mnuBytesInvert = createJMenuItem(
				"Ausgew\u00E4hlte Bytes invertieren" );
    this.mnuBytesInvert.setEnabled( false );
    mnuEdit.add( this.mnuBytesInvert );

    this.mnuBytesRemove = createJMenuItem(
		"Ausgew\u00E4hlte Bytes entfernen",
		KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
    this.mnuBytesRemove.setEnabled( false );
    mnuEdit.add( this.mnuBytesRemove );
    mnuEdit.addSeparator();

    this.mnuFileInsert = createJMenuItem( "Datei einf\u00FCgen..." );
    this.mnuFileInsert.setEnabled( false );
    mnuEdit.add( this.mnuFileInsert );

    this.mnuFileAppend = createJMenuItem( "Datei am Ende anh\u00E4ngen..." );
    mnuEdit.add( this.mnuFileAppend );
    mnuEdit.addSeparator();

    this.mnuSavePos = createJMenuItem( "Position merken" );
    this.mnuSavePos.setEnabled( false );
    mnuEdit.add( this.mnuSavePos );

    this.mnuGotoSavedPos = createJMenuItem(
				"Zur gemerkten Position springen" );
    this.mnuGotoSavedPos.setEnabled( false );
    mnuEdit.add( this.mnuGotoSavedPos );

    this.mnuSelectToSavedPos = createJMenuItem(
			"Bis zur gemerkten Position ausw\u00E4hlen" );
    this.mnuSelectToSavedPos.setEnabled( false );
    mnuEdit.add( this.mnuSelectToSavedPos );
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
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    add( toolBar, gbc );

    this.btnNew = createImageButton( "/images/file/new.png", "Neu" );
    toolBar.add( this.btnNew );

    this.btnOpen = createImageButton( "/images/file/open.png", "\u00D6ffnen" );
    toolBar.add( this.btnOpen );

    this.btnSave = createImageButton( "/images/file/save.png", "Speichern" );
    this.btnSave.setEnabled( false );
    toolBar.add( this.btnSave );
    toolBar.addSeparator();

    this.btnFind = createImageButton( "/images/edit/find.png", "Suchen" );
    this.btnFind.setEnabled( false );
    toolBar.add( this.btnFind );


    // Hex-ASCII-Anzeige
    gbc.anchor  = GridBagConstraints.CENTER;
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( createHexCharFld(), gbc );

    // Anzeige der Cursor-Position
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( createCaretPosFld( "Cursor-Position" ), gbc );

    // Anzeige der Dezimalwerte der Bytes ab Cursor-Position
    gbc.gridy++;
    add( createValueFld(), gbc );


    // Drag&Drop aktivieren
    (new DropTarget( this.hexCharFld, this )).setActive( true );


    // sonstiges
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );
  }


  public void newFile( byte[] data )
  {
    if( confirmDataSaved() )
      newFileInternal( data );
  }


  public void openFile( File file )
  {
    if( file != null ) {
      if( confirmDataSaved() ) {
	openFileInternal( file );
      }
    }
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // empty
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // empty
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    File file = EmuUtil.fileDrop( this, e );
    if( file != null )
      openFile( file );
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( (src == this.btnNew) || (src == this.mnuNew) ) {
	rv = true;
	if( confirmDataSaved() ) {
	  newFileInternal( null );
	}
      } else if( (src == this.btnOpen) || (src == this.mnuOpen) ) {
	rv = true;
	doOpen();
      } else if( (src == this.btnSave) || (src == this.mnuSave) ) {
	rv = true;
	doSave( false );
      } else if( src == this.mnuSaveAs ) {
	rv = true;
	doSave( true );
      } else if( src == this.mnuPrintOptions ) {
	rv = true;
	PrintOptionsDlg.showPrintOptionsDlg( this, true, true );
      } else if( src == this.mnuPrint ) {
	rv = true;
	PrintUtil.doPrint( this, this, "JKCEMU Hex-Editor" );
      } else if( src == this.mnuClose ) {
	rv = true;
	doClose();
      } else if( src == this.mnuBytesAppend ) {
	rv = true;
	doBytesAppend();
      } else if( src == this.mnuBytesCopy ) {
	rv = true;
	doBytesCopy();
      } else if( src == this.mnuBytesInvert ) {
	rv = true;
	doBytesInvert();
      } else if( src == this.mnuBytesSave ) {
	rv = true;
	doBytesSave();
      } else if( src == this.mnuBytesInsert ) {
	rv = true;
	doBytesInsert();
      } else if( src == this.mnuBytesOverwrite ) {
	rv = true;
	doBytesOverwrite();
      } else if( src == this.mnuBytesRemove ) {
	rv = true;
	doBytesRemove();
      } else if( src == this.mnuFileAppend ) {
	rv = true;
	doFileAppend();
      } else if( src == this.mnuFileInsert ) {
	rv = true;
	doFileInsert();
      } else if( src == this.mnuSavePos ) {
	rv = true;
	doSavePos();
      } else if( src == this.mnuTextCopy ) {
	rv = true;
	this.hexCharFld.copySelectedText();
      } else if( src == this.mnuGotoSavedPos ) {
	rv = true;
	doGotoSavedPos( false );
      } else if( src == this.mnuSelectToSavedPos ) {
	rv = true;
	doGotoSavedPos( true );
      } else if( src == this.mnuChecksum ) {
	rv = true;
	doChecksum();
      } else if( (src == this.btnFind) || (src == this.mnuFind) ) {
	rv = true;
	doFind();
      } else if( src == this.mnuFindNext ) {
	rv = true;
	doFindNext();
      } else if( src == this.mnuHelpContent ) {
	rv = true;
	this.screenFrm.showHelp( "/help/tools/hexeditor.htm" );
      } else {
	rv = super.doAction( e );
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = confirmDataSaved();
    if( rv ) {
      rv = super.doClose();
    }
    if( rv ) {
      // damit beim erneuten Oeffnen der Editor leer ist
      newFileInternal( null );
    }
    return rv;
  }


  @Override
  public byte getDataByte( int idx )
  {
    byte rv = (byte) 0;
    if( this.fileBytes != null ) {
      if( (idx >= 0) && (idx < this.fileBytes.length) ) {
	rv = this.fileBytes[ idx ];
      }
    }
    return rv;
  }


  @Override
  public int getDataLength()
  {
    return this.fileLen;
  }


  @Override
  protected void setContentActionsEnabled( boolean state )
  {
    this.mnuSaveAs.setEnabled( state );
    this.mnuPrint.setEnabled( state );
    this.mnuFind.setEnabled( state );
    this.btnFind.setEnabled( state );
  }


  @Override
  protected void setFindNextActionsEnabled( boolean state )
  {
    this.mnuFindNext.setEnabled( state );
  }


  @Override
  protected void setSelectedByteActionsEnabled( boolean state )
  {
    this.mnuTextCopy.setEnabled( state );
    this.mnuBytesCopy.setEnabled( state );
    this.mnuBytesInvert.setEnabled( state );
    this.mnuBytesSave.setEnabled( state );
    this.mnuBytesInsert.setEnabled( state );
    this.mnuBytesOverwrite.setEnabled( state );
    this.mnuBytesRemove.setEnabled( state );
    this.mnuFileInsert.setEnabled( state );
    this.mnuChecksum.setEnabled( state );
    this.mnuSavePos.setEnabled( state );
    this.mnuSelectToSavedPos.setEnabled( state && (this.savedPos >= 0) );
  }


	/* --- Aktionen --- */

  private void doBytesAppend()
  {
    ReplyBytesDlg dlg = new ReplyBytesDlg(
					this,
					"Bytes anh\u00E4ngen",
					this.lastInputFmt,
					this.lastBigEndian,
					null );
    dlg.setVisible( true );
    byte[] a = dlg.getApprovedBytes();
    if( a != null ) {
      if( a.length > 0 ) {
	int oldLen = this.fileLen;
	this.lastInputFmt  = dlg.getApprovedInputFormat();
	this.lastBigEndian = dlg.getApprovedBigEndian();
	try {
	  insertBytes( this.fileLen, a, 0 );
	}
	catch( SizeLimitExceededException ex ) {
	  BasicDlg.showErrorDlg( this, ex.getMessage() );
	}
	setDataChanged( true );
	updView();
	setSelection( oldLen, this.fileLen - 1 );
      }
    }
  }


  private void doBytesInsert()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
      ReplyBytesDlg dlg = new ReplyBytesDlg(
					this,
					"Bytes einf\u00FCgen",
					this.lastInputFmt,
					this.lastBigEndian,
					null );
      dlg.setVisible( true );
      byte[] a = dlg.getApprovedBytes();
      if( a != null ) {
	if( a.length > 0 ) {
	  this.lastInputFmt  = dlg.getApprovedInputFormat();
	  this.lastBigEndian = dlg.getApprovedBigEndian();
	  try {
	    insertBytes( caretPos, a, 0 );
	  }
	  catch( SizeLimitExceededException ex ) {
	    BasicDlg.showErrorDlg( this, ex.getMessage() );
	  }
	  setDataChanged( true );
	  updView();
	  setSelection( caretPos, caretPos + a.length - 1 );
	}
      }
    }
  }


  private void doBytesInvert()
  {
    int dataLen  = getDataLength();
    int caretPos = this.hexCharFld.getCaretPosition();
    int markPos  = this.hexCharFld.getMarkPosition();
    int m1       = -1;
    int m2       = -1;
    if( (caretPos >= 0) && (markPos >= 0) ) {
      m1 = Math.min( caretPos, markPos );
      m2 = Math.max( caretPos, markPos );
    } else {
      m1 = caretPos;
      m2 = caretPos;
    }
    if( m2 >= dataLen ) {
      m2 = dataLen - 1;
    }
    if( m1 >= 0 ) {
      String msg = null;
      if( m2 > m1 ) {
	msg = String.format(
		"M\u00F6chten Sie die %d ausgew\u00E4hlten Bytes"
			+ " invertieren?",
		m2 - m1 + 1);
      }
      else if( m2 == m1 ) {
	msg = String.format(
		"M\u00F6chten das ausgew\u00E4hlte Byte mit dem"
			+ " hexadezimalen Wert %02X invertieren?",
		this.fileBytes[ m1 ] );
      }
      if( msg != null ) {
	if( BasicDlg.showYesNoDlg( this, msg ) ) {
	  int p = m1;
	  while( p <= m2 ) {
	    this.fileBytes[ p ] = (byte) ~this.fileBytes[ p ];
	    p++;
	  }
	  setDataChanged( true );
	  updView();
	  setSelection( m1, m2 );
	}
      }
    }
  }


  private void doBytesOverwrite()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
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
	  try {
	    int src = 0;
	    int dst = caretPos;
	    while( (src < a.length) && (dst < this.fileLen) ) {
	      this.fileBytes[ dst++ ] = a[ src++ ];
	    }
	    if( src < a.length ) {
	      insertBytes( dst, a, src );
	    }
	  }
	  catch( SizeLimitExceededException ex ) {
	    BasicDlg.showErrorDlg( this, ex.getMessage() );
	  }
	  setDataChanged( true );
	  updView();
	  setSelection( caretPos, caretPos + a.length - 1 );
	}
      }
    }
  }


  private void doBytesRemove()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    int markPos  = this.hexCharFld.getMarkPosition();
    int m1       = -1;
    int m2       = -1;
    if( (caretPos >= 0) && (markPos >= 0) ) {
      m1 = Math.min( caretPos, markPos );
      m2 = Math.max( caretPos, markPos );
    } else {
      m1 = caretPos;
      m2 = caretPos;
    }
    if( m2 >= this.fileLen ) {
      m2 = this.fileLen - 1;
    }
    if( m1 >= 0 ) {
      String msg = null;
      if( m2 > m1 ) {
	msg = String.format(
		"M\u00F6chten Sie die %d ausgew\u00E4hlten Bytes entfernen?",
		m2 - m1 + 1);
      }
      else if( m2 == m1 ) {
	msg = String.format(
		"M\u00F6chten das ausgew\u00E4hlte Byte mit dem"
			+ " hexadezimalen Wert %02X entfernen?",
		this.fileBytes[ m1 ] );
      }
      if( msg != null ) {
	if( BasicDlg.showYesNoDlg( this, msg ) ) {
	  if( m2 + 1 < this.fileLen ) {
	    m2++;
	    while( m2 < this.fileLen ) {
	      this.fileBytes[ m1++ ] = this.fileBytes[ m2++ ];
	    }
	  }
	  this.fileLen = m1;
	  setDataChanged( true );
	  updView();
	  setCaretPosition( m1, false );
	}
      }
    }
  }


  private void doBytesSave()
  {
    int dataLen  = getDataLength();
    int caretPos = this.hexCharFld.getCaretPosition();
    int markPos  = this.hexCharFld.getMarkPosition();
    int m1       = -1;
    int m2       = -1;
    if( (caretPos >= 0) && (markPos >= 0) ) {
      m1 = Math.min( caretPos, markPos );
      m2 = Math.max( caretPos, markPos );
    } else {
      m1 = caretPos;
      m2 = caretPos;
    }
    if( m2 >= dataLen ) {
      m2 = dataLen - 1;
    }
    if( m1 >= 0 ) {
      int len = Math.min( m2, this.fileBytes.length ) - m1 + 1;
      if( len > 0 ) {
	File presetDir = null;
	if( this.file != null ) {
	  presetDir = this.file.getParentFile();
	}
	if( presetDir == null ) {
	  presetDir = Main.getLastPathFile( "software" );
	}
	File file = EmuUtil.showFileSaveDlg(
					this,
					"Datei speichern",
					presetDir );
	if( file != null ) {
	  try {
	    OutputStream out = null;
	    try {
	      out = new FileOutputStream( file );
	      out.write( this.fileBytes, m1, len  );
	      out.close();
	      out = null;
	    }
	    finally {
	      EmuUtil.doClose( out );
	    }
	    Main.setLastFile( file, "software" );
	  }
	  catch( Exception ex ) {
	    BasicDlg.showErrorDlg( this, ex );
	  }
	}
      }
    }
  }


  private void doFileAppend()
  {
    File file = EmuUtil.showFileOpenDlg(
				this,
				"Datei anh\u00E4ngen",
				Main.getLastPathFile( "software" ) );
    if( file != null ) {
      try {
	int  oldLen  = this.fileLen;
	long fileLen = file.length();
	if( (fileLen > 0)
	    && ((fileLen + (long) oldLen) > Integer.MAX_VALUE) )
	{
	  throwFileTooBig();
	}
	byte[] a = EmuUtil.readFile( file, Integer.MAX_VALUE );
	if( a != null ) {
	  if( a.length > 0 ) {
	    try {
	      insertBytes( this.fileLen, a, 0 );
	    }
	    catch( SizeLimitExceededException ex ) {
	      BasicDlg.showErrorDlg( this, ex.getMessage() );
	    }
	    setDataChanged( true );
	    updView();
	    setSelection( oldLen, this.fileLen - 1 );
	    Main.setLastFile( file, "software" );
	  }
	}
      }
      catch( IOException ex ) {
	BasicDlg.showErrorDlg( this, ex );
      }
    }
  }


  private void doFileInsert()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
      File file = EmuUtil.showFileOpenDlg(
				this,
				"Datei einf\u00FCgen",
				Main.getLastPathFile( "software" ) );
      if( file != null ) {
	try {
	  long fileLen = file.length();
	  if( (fileLen > 0)
	      && ((fileLen + (long) this.fileLen) > Integer.MAX_VALUE) )
	  {
	    throwFileTooBig();
	  }
	  byte[] a = EmuUtil.readFile( file, Integer.MAX_VALUE );
	  if( a != null ) {
	    if( a.length > 0 ) {
	      try {
		insertBytes( caretPos, a, 0 );
	      }
	      catch( SizeLimitExceededException ex ) {
		BasicDlg.showErrorDlg( this, ex.getMessage() );
	      }
	      setDataChanged( true );
	      updView();
	      setSelection( caretPos, caretPos + a.length - 1 );
	      Main.setLastFile( file, "software" );
	    }
	  }
	}
	catch( IOException ex ) {
	  BasicDlg.showErrorDlg( this, ex );
	}
      }
    }
  }


  private void doGotoSavedPos( boolean moveOp )
  {
    if( this.savedPos >= 0 ) {
      this.hexCharFld.setCaretPosition( this.savedPos, moveOp );
      updCaretPosFields();
    }
  }


  private void doOpen()
  {
    if( confirmDataSaved() ) {
      File file = EmuUtil.showFileOpenDlg(
				this,
				"Datei \u00F6ffnen",
				Main.getLastPathFile( "software" ) );
      if( file != null )
	openFileInternal( file );
    }
  }


  private boolean doSave( boolean forceFileDlg )
  {
    boolean rv   = false;
    File    file = this.file;
    if( forceFileDlg || (file == null) ) {
      file = EmuUtil.showFileSaveDlg(
		this,
		"Datei speichern",
		file != null ? file : Main.getLastPathFile( "software" ) );
    }
    if( file != null ) {
      try {
	OutputStream out = null;
	try {
	  out = new FileOutputStream( file );
	  if( (this.fileLen > 0) && (this.fileBytes.length > 0) ) {
	    out.write(
		this.fileBytes,
		0,
		Math.min( this.fileLen, this.fileBytes.length) );
	  }
	  out.close();
	  out       = null;
	  this.file = file;
	  rv        = true;
	  if( !setDataChanged( false ) ) {
	    updTitle();
	  }
	}
	finally {
	  EmuUtil.doClose( out );
	}
	Main.setLastFile( file, "software" );
      }
      catch( Exception ex ) {
	BasicDlg.showErrorDlg( this, ex );
      }
    }
    return rv;
  }


  private void doSavePos()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( caretPos >= 0 ) {
      this.savedPos = caretPos;
      this.mnuGotoSavedPos.setEnabled( true );
      this.mnuSelectToSavedPos.setEnabled( true );
    }
  }


	/* --- private Methoden --- */

  private boolean confirmDataSaved()
  {
    boolean rv = true;
    if( this.dataChanged ) {
      setState( Frame.NORMAL );
      toFront();
      String[] options = { "Speichern", "Verwerfen", "Abbrechen" };
      int      selOpt  = JOptionPane.showOptionDialog(
				this,
				"Die Datei wurde ge\u00E4ndert und nicht"
					+" gespeichert.\n"
					+ "M\u00F6chten Sie jetzt speichern?",
				"Daten ge\u00E4ndert",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE,
				null,
				options,
				"Speichern" );
      if( selOpt == 0 ) {
	rv = doSave( false );
      }
      else if( selOpt != 1 ) {
	rv = false;
      }
    }
    return rv;
  }


  private void insertBytes(
			int    dstPos,
			byte[] srcBuf,
			int    srcPos ) throws SizeLimitExceededException
  {
    if( (srcPos >= 0) && (srcPos < srcBuf.length) && (srcBuf.length > 0) ) {
      int diffLen = srcBuf.length - srcPos;
      int reqLen  = this.fileLen + diffLen;
      if( reqLen >= this.fileBytes.length ) {
	int n = Math.min( reqLen + BUF_EXTEND, Integer.MAX_VALUE );
	if( n < reqLen) {
	  throw new SizeLimitExceededException( "Die max. zul\u00E4ssige"
			+ " Dateigr\u00F6\u00DFe wurde erreicht." );
	}
	byte[] tmpBuf = new byte[ n ];
	if( dstPos > 0 ) {
	  System.arraycopy( this.fileBytes, 0, tmpBuf, 0, dstPos );
	}
	System.arraycopy( srcBuf, srcPos, tmpBuf, dstPos, diffLen );
	if( dstPos < this.fileLen ) {
	  System.arraycopy(
			this.fileBytes,
			dstPos,
			tmpBuf,
			dstPos + diffLen,
			this.fileLen - dstPos );
	}
	this.fileBytes = tmpBuf;
      } else {
	for( int i = this.fileLen - 1; i >= dstPos; --i ) {
	  this.fileBytes[ i + diffLen ] = this.fileBytes[ i ];
	}
	System.arraycopy( srcBuf, srcPos, this.fileBytes, dstPos, diffLen );
      }
      this.fileLen += diffLen;
    }
  }


  private void newFileInternal( byte[] data )
  {
    if( data != null ) {
      this.fileBytes = new byte[ data.length + 0x100 ];
      if( data.length > 0 ) {
	System.arraycopy( data, 0, this.fileBytes, 0, data.length );
      }
      this.fileLen = data.length;
    } else {
      this.fileBytes = new byte[ 0x100 ];
      this.fileLen   = 0;
    }
    this.file = null;
    setDataChanged( false );
    updTitle();
    updView();
    setCaretPosition( -1, false );
    this.hexCharFld.setYOffset( 0 );
  }


  private void openFileInternal( File file )
  {
    try {
      InputStream in = null;
      try {
	long len = file.length();
	if( len > Integer.MAX_VALUE ) {
	  throwFileTooBig();
	}
	if( len > 0 ) {
	  len = len * 10L / 9L;
	}
	if( len < BUF_EXTEND ) {
	  len = BUF_EXTEND;
	} else if( len > Integer.MAX_VALUE ) {
	  len = Integer.MAX_VALUE;
	}
	byte[] fileBytes = new byte[ (int) len ];
	int    fileLen   = 0;

	in = new FileInputStream( file );
	while( fileLen < fileBytes.length ) {
	  int n = in.read( fileBytes, fileLen, fileBytes.length - fileLen );
	  if( n <= 0 ) {
	    break;
	  }
	  fileLen += n;
	}
	if( fileLen >= fileBytes.length ) {
	  int b = in.read();
	  while( b != -1 ) {
	    if( fileLen >= fileBytes.length ) {
	      int n = Math.min( fileLen + BUF_EXTEND, Integer.MAX_VALUE );
	      if( fileLen >= n ) {
		throwFileTooBig();
	      }
	      byte[] a = new byte[ n ];
	      System.arraycopy( fileBytes, 0, a, 0, fileLen );
	      fileBytes = a;
	    }
	    fileBytes[ fileLen++ ] = (byte) b;
	    b = in.read();
	  }
	}
	in.close();

	this.file      = file;
	this.fileBytes = fileBytes;
	this.fileLen   = fileLen;
	this.savedPos  = -1;
	this.mnuGotoSavedPos.setEnabled( false );
	this.mnuSelectToSavedPos.setEnabled( false );
	if( !setDataChanged( false ) ) {
	  updTitle();
	}
	updView();
	setCaretPosition( -1, false );
	this.hexCharFld.setYOffset( 0 );
	Main.setLastFile( file, "software" );
      }
      finally {
	EmuUtil.doClose( in );
      }
    }
    catch( IOException ex ) {
      BasicDlg.showErrorDlg( this, ex );
    }
  }


  private boolean setDataChanged( boolean state )
  {
    boolean rv = false;
    if( state != this.dataChanged ) {
      this.dataChanged = state;
      updTitle();
      this.mnuSave.setEnabled( this.dataChanged );
      this.btnSave.setEnabled( this.dataChanged );
      rv = true;
    }
    return rv;
  }


  private static void throwFileTooBig() throws IOException
  {
    throw new IOException( "Datei ist zu gro\u00DF!" );
  }


  private void updTitle()
  {
    StringBuilder buf = new StringBuilder( 128 );
    buf.append( "JKCEMU Hex-Editor: " );
    if( this.file != null ) {
      buf.append( file.getPath() );
    } else {
      buf.append( "Neue Datei" );
    }
    setTitle( buf.toString() );
  }
}

