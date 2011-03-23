/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Unterschiede von Binaerdateien hexadezimal anzeigen
 */

package jkcemu.tools.hexdiff;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.base.*;


public class HexDiffFrm extends HTMLViewFrm implements
						DropTargetListener,
						ListSelectionListener
{
  private int              lastDiffs;
  private Vector<FileData> files;
  private JMenuItem        mnuFileAdd;
  private JMenuItem        mnuFileRemove;
  private JMenuItem        mnuMaxDiffs;
  private JList            listFiles;
  private JButton          btnFileAdd;
  private JButton          btnFileRemove;


  public HexDiffFrm( ScreenFrm screenFrm )
  {
    super( screenFrm );
    setTitle( "JKCEMU Hex-Dateivergleicher" );
    this.files     = new Vector<FileData>();
    this.lastDiffs = 0;


    // Menu
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuFileAdd = createJMenuItem( "Datei hinzuf\u00FCgen..." );
    mnuFile.add( this.mnuFileAdd );

    this.mnuFileRemove = createJMenuItem( "Datei entfernen" );
    this.mnuFileRemove.setEnabled( false );
    mnuFile.add( this.mnuFileRemove );
    mnuFile.addSeparator();

    JMenu mnuSettings = new JMenu( "Einstellungen" );
    mnuSettings.setMnemonic( KeyEvent.VK_E );

    this.mnuMaxDiffs = createJMenuItem( "Max. Dateiunterschiede..." );
    mnuSettings.add( this.mnuMaxDiffs );

    createMenuBar( mnuFile, mnuSettings, "/help/tools/hexdiff.htm" );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 2,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    // Dateiliste
    add( new JLabel( "Dateien:" ), gbc );

    this.listFiles = new JList();
    this.listFiles.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    this.listFiles.setVisibleRowCount( 2 );
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.gridx++;
    add( new JScrollPane( this.listFiles ), gbc );

    this.btnFileAdd = createImageButton(
				"/images/file/open.png",
				"Datei hinzuf\u00FCgen" );
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.gridheight = 1;
    gbc.gridx++;
    add( this.btnFileAdd, gbc );

    this.btnFileRemove = createImageButton(
				"/images/file/delete.png",
				"Datei entfernen" );
    this.btnFileRemove.setEnabled( false );
    gbc.gridy++;
    add( this.btnFileRemove, gbc );

    this.listFiles.addListSelectionListener( this );


    // Ergebnis
    gbc.fill      = GridBagConstraints.BOTH;
    gbc.weightx   = 1.0;
    gbc.weighty   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    createEditorPane( gbc );


    // Drag&Drop aktivieren
    (new DropTarget( this.listFiles, this )).setActive( true );
    (new DropTarget( this.editorPane, this )).setActive( true );
    (new DropTarget( this.scrollPane, this )).setActive( true );


    // sonstiges
    if( !applySettings( Main.getProperties(), true ) ) {
      this.editorPane.setPreferredSize( new Dimension( 300, 300 ) );
      pack();
      this.editorPane.setPreferredSize( null );
      setScreenCentered();
    }
    setResizable( true );
    updResult();
  }


  public void addFiles( Collection<File> files )
  {
    if( files != null ) {
      File curFile = null;
      try {
	boolean added = false;
	for( File file : files ) {
	  curFile = file;

	  boolean alreadyAdded = false;
	  for( FileData data : this.files ) {
	    if( file.equals( data.getFile() ) ) {
	      alreadyAdded = true;
	      break;
	    }
	  }
	  if( alreadyAdded ) {
	    BasicDlg.showErrorDlg(
			this,
			file.getPath()
				+ ":\nDie Datei wurde bereits vorher"
				+ " schon hinzugef\u00FCgt." );
	  } else {
	    FileData fileData = new FileData( file );
	    if( file.length() > 0 ) {
	      this.files.add( fileData );
	      added = true;
	    } else {
	      BasicDlg.showInfoDlg( this, "Die Datei ist leer." );
	    }
	  }
	}
	if( added ) {
	  this.listFiles.setListData( this.files );
	  updResult();
	}
      }
      catch( IOException ex ) {
	BasicDlg.showOpenFileErrorDlg( this, curFile, ex );
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
      addFile( file );
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.listFiles ) {
      boolean state = (this.listFiles.getSelectedIndex() >= 0);
      this.mnuFileRemove.setEnabled( state );
      this.btnFileRemove.setEnabled( state );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( (src == this.mnuFileAdd) || (src == this.btnFileAdd) ) {
	rv = true;
	doFileAdd();
      }
      else if( (src == this.mnuFileRemove) || (src == this.btnFileRemove) ) {
	rv = true;
	doFileRemove();
      }
      else if( src == this.mnuMaxDiffs ) {
	rv = true;
	doMaxDiffs();
      }
    }
    if( rv == false ) {
      rv = super.doAction( e );
    }
    return rv;
  }


	/* --- private Methoden --- */

  private boolean addFile( File file )
  {
    boolean rv           = false;
    boolean alreadyAdded = false;
    for( FileData data : this.files ) {
      if( file.equals( data.getFile() ) ) {
	alreadyAdded = true;
	break;
      }
    }
    if( alreadyAdded ) {
      BasicDlg.showErrorDlg(
		this,
		"Die Datei wurde bereits vorher schon hinzugef\u00FCgt." );
    } else {
      try {
	FileData fileData = new FileData( file );
	if( file.length() > 0 ) {
	  this.files.add( fileData );
	  this.listFiles.setListData( this.files );
	  updResult();
	  rv = true;
	} else {
	  BasicDlg.showInfoDlg( this, "Die Datei ist leer." );
	}
      }
      catch( IOException ex ) {
	BasicDlg.showOpenFileErrorDlg( this, file, ex );
      }
    }
    return rv;
  }


  private void doFileAdd()
  {
    File file = EmuUtil.showFileOpenDlg(
				this,
				"Datei \u00F6ffnen",
				Main.getLastPathFile( "software" ) );
    if( file != null ) {
      if( addFile( file ) )
	Main.setLastFile( file, "software" );
    }
  }


  private void doFileRemove()
  {
    int idx = this.listFiles.getSelectedIndex();
    if( (idx >= 0) && (idx <= this.files.size()) ) {
      this.files.remove( idx );
      this.listFiles.setListData( this.files );
      updResult();
    }
  }


  private void doMaxDiffs()
  {
    int         vOld = getMaxDiffs();
    ReplyIntDlg dlg  = new ReplyIntDlg(
			this,
			"Max. Dateiunterschiede (0 f\u00FCr unendlich):",
			new Integer( vOld ),
			new Integer( 0 ),
			null );
    dlg.setTitle( "Einstellung" );
    dlg.setVisible( true );
    Integer v = dlg.getReply();
    if( v != null ) {
      Main.setProperty( "jkcemu.hexdiff.differences.max", v.toString() );
      if( (this.lastDiffs > 0) && (this.lastDiffs >= vOld) )
	updResult();
    }
  }


  private int getMaxDiffs()
  {
    int rv = 100;
    String s = Main.getProperty( "jkcemu.hexdiff.differences.max" );
    if( s != null ) {
      try {
	int v = Integer.parseInt( s );
	if( v >= 0 ) {
	  rv = v;
	}
      }
      catch( NumberFormatException ex ) {}
    }
    return rv;
  }


  private void updResult()
  {
    setHTMLText( "" );

    int nFiles = this.files.size();
    if( nFiles > 1 ) {
      StringBuilder buf      = null;
      int           maxDiffs = getMaxDiffs();
      int           nDiffs   = 0;
      int[]         rowBytes = new int[ nFiles ];
      try {
	try {
	  int     pos  = 0;
	  boolean loop = true;
	  while( loop && ((maxDiffs <= 0) || (nDiffs < maxDiffs)) ) {
	    loop = false;

	    // Byte der ersten Datei lesen
	    boolean differs = false;
	    boolean allEOF  = true;
	    rowBytes[ 0 ]   = this.files.get( 0 ).read();
	    if( rowBytes[ 0 ] >= 0 ) {
	      loop   = true;
	      allEOF = false;
	    } else {
	      differs = true;
	    }

	    // Bytes der anderen Dateien lesen und vergleichen
	    for( int i = 1; i < nFiles; i++ ) {
	      int b = this.files.get( i ).read();
	      if( b >= 0 ) {
		loop   = true;
		allEOF = false;
		if( rowBytes[ 0 ] != b ) {
		  differs = true;
		}
	      } else {
		differs = true;
	      }
	      rowBytes[ i ] = b;
	    }

	    // Ausgabe bei Unterschiedlichkeit
	    if( differs && !allEOF ) {
	      if( buf == null ) {
		buf = new StringBuilder( 0x10000 );
		buf.append( "<htm>\n"
				+ "<table border=\"1\">\n"
				+ "<tr><th>Position</th>" );
		for( int i = 0; i < nFiles; i++ ) {
		  buf.append( "<th>Datei " );
		  buf.append( i + 1 );
		  buf.append( "</th>" );
		}
		buf.append( "</tr>\n" );
	      }
	      buf.append( "<tr><td>" );
	      buf.append( String.format( "%08X", pos ) );
	      buf.append( "</td>" );
	      for( int i = 0; i < nFiles; i++ ) {
		buf.append( "<td>" );
		int b = rowBytes[ i ];
		if( b >= 0 ) {
		  buf.append( String.format( "%02X", b ) );
		  if( (b > 0x20) && (b < 0x7F) ) {
		    buf.append( (char) '\u0020' );
		    buf.append( (char) b );
		  }
		}
		buf.append( "</td>" );
	      }
	      buf.append( "</tr>\n" );
	      nDiffs++;
	    }
	    pos++;
	  }
	  if( buf != null ) {
	    buf.append( "</table>\n" );
	    if( (maxDiffs > 0) && (nDiffs >= maxDiffs) ) {
	      buf.append( "<br>\n" );
	      if( maxDiffs == 1 ) {
		buf.append( "Es wird nur der erste Unterschied angezeigt.\n" );
	      } else {
		buf.append( "Es werden nur die ersten " );
		buf.append( maxDiffs );
		buf.append( " Unterschiede angezeigt.\n" );
	      }
	    } else {
	      if( nDiffs == 1 ) {
		buf.append( "1 unterschiedliches Byte\n" );
	      } else {
		buf.append( nDiffs );
		buf.append( " unterschiedliche Bytes\n" );
	      }
	    }
	    buf.append( "</html>\n" );
	    setHTMLText( buf.toString() );
	  } else {
	    setHTMLText(
		"<html>Der Inhalt der Dateien ist identisch.</html>" );
	  }
	  this.lastDiffs = nDiffs;
	}
	finally {
	  for( FileData fileData : this.files ) {
	    try {
	      fileData.close();
	    }
	    catch( IOException ex ) {}
	  }
	}
      }
      catch( IOException ex ) {
	BasicDlg.showErrorDlg( this, ex.getMessage() );
      }
    }
  }
}

