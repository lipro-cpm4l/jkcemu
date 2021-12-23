/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Unterschiede von Binaerdateien hexadezimal anzeigen
 */

package jkcemu.tools.hexdiff;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HTMLViewFrm;
import jkcemu.base.PopupMenuOwner;
import jkcemu.base.ReplyIntDlg;
import jkcemu.file.FileUtil;


public class HexDiffFrm extends HTMLViewFrm implements
						DropTargetListener,
						ListSelectionListener,
						PopupMenuOwner
{
  public static final String TITLE = Main.APPNAME + " Hex-Dateivergleicher";

  private static HexDiffFrm instance = null;

  private int              lastDiffs;
  private Vector<FileData> files;
  private JMenuItem        mnuFileAdd;
  private JMenuItem        mnuFileRemove;
  private JMenuItem        mnuMaxDiffs;
  private JPopupMenu       popupMnu;
  private JMenuItem        popupFileAdd;
  private JMenuItem        popupFileRemove;
  private JList<FileData>  listFiles;
  private JButton          btnFileAdd;
  private JButton          btnFileRemove;


  public static HexDiffFrm open()
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new HexDiffFrm();
    }
    instance.setVisible( true );
    instance.toFront();
    return instance;
  }


  public int addFiles( Collection<?> files )
  {
    int nAdded = 0;
    if( files != null ) {
      File curFile = null;
      try {
	boolean added = false;
	for( Object o : files ) {
	  if( o instanceof File ) {
	    if( ((File) o).isFile() ) {
	      curFile = (File) o;

	      boolean alreadyAdded = false;
	      for( FileData data : this.files ) {
		if( curFile.equals( data.getFile() ) ) {
		  alreadyAdded = true;
		  break;
		}
	      }
	      if( alreadyAdded ) {
		if( files.size() == 1 ) {
		  BaseDlg.showInfoDlg(
			this,
			"Die Datei wurde bereits hinzugef\u00FCgt." );
		}
	      } else {
		FileData fileData = new FileData( curFile );
		if( curFile.length() > 0 ) {
		  this.files.add( fileData );
		  nAdded++;
		} else {
		  String fName = curFile.getName();
		  if( fName == null ) {
		    fName = curFile.getPath();
		  }
		  if( BaseDlg.showOptionDlg(
			this,
			fName + ": Datei ist leer.",
			"Datei leer",
			"Weiter",
			EmuUtil.TEXT_CANCEL ) != 0 )
		  {
		    break;
		  }
		}
	      }
	    }
	  }
	}
	if( nAdded > 0 ) {
	  this.listFiles.setListData( this.files );
	  updResult();
	}
      }
      catch( IOException ex ) {
	BaseDlg.showOpenFileErrorDlg( this, curFile, ex );
      }
    }
    return nAdded;
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !FileUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    if( FileUtil.isFileDrop( e ) ) {
      e.acceptDrop( DnDConstants.ACTION_COPY ); // Quelle nicht loeschen
      Transferable t = e.getTransferable();
      if( t != null ) {
	try {
	  final Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
	  if( o != null ) {
	    if( o instanceof Collection ) {
	      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    addFiles( (Collection<?>) o );
			  }
			} );
	    }
	  }
	}
	catch( IOException ex ) {}
	catch( UnsupportedFlavorException ex ) {}
      }
      e.dropComplete( true );
    } else {
      e.rejectDrop();
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.listFiles ) {
      boolean state = (this.listFiles.getSelectedIndex() >= 0);
      this.btnFileRemove.setEnabled( state );
      this.mnuFileRemove.setEnabled( state );
      this.popupFileRemove.setEnabled( state );
    }
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.popupMnu;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( (src == this.btnFileAdd)
	  || (src == this.mnuFileAdd)
	  || (src == this.popupFileAdd) )
      {
	rv = true;
	doFileAdd();
      }
      else if( (src == this.btnFileRemove)
	       || (src == this.mnuFileRemove)
	       || (src == this.popupFileRemove) )
      {
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


  @Override
  public boolean doClose()
  {
    boolean rv = false;
    if( Main.isTopFrm( this ) ) {
      rv = EmuUtil.closeOtherFrames( this );
      if( rv ) {
	rv = super.doClose();
      }
      if( rv ) {
	Main.exitSuccess();
      }
    } else {
      rv = super.doClose();
    }
    if( rv ) {
      // damit beim erneuten Oeffnen das Fenster leer ist
      this.files.clear();
      this.listFiles.setListData( this.files );
      updResult();
    }
    return rv;
  }


  @Override
  protected boolean showPopupMenu( MouseEvent e )
  {
    boolean rv = false;
    if( e.getComponent() == this.listFiles ) {
      this.popupMnu.show( this.listFiles, e.getX(), e.getY() );
      rv = true;
    } else {
      rv = super.showPopupMenu( e );
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private HexDiffFrm()
  {
    setTitle( TITLE );
    this.files     = new Vector<>();
    this.lastDiffs = 0;


    // Menu
    JMenu mnuFile = createMenuFile();

    this.mnuFileAdd = createMenuItem( EmuUtil.TEXT_ADD_FILE );
    mnuFile.add( this.mnuFileAdd );

    this.mnuFileRemove = createMenuItem( "Datei entfernen" );
    this.mnuFileRemove.setEnabled( false );
    mnuFile.add( this.mnuFileRemove );
    mnuFile.addSeparator();

    JMenu mnuSettings = createMenuSettings();
    this.mnuMaxDiffs  = createMenuItem( "Max. Dateiunterschiede..." );
    mnuSettings.add( this.mnuMaxDiffs );

    createMenuBar(
		mnuFile,
		EmuUtil.TEXT_OPEN_FIND,
		null,
		mnuSettings,
		"Hilfe zum Hex-Dateivergleicher...",
		"/help/tools/hexdiff.htm" );


    // Kontextmenue fuer Dateibereich
    this.popupMnu = GUIFactory.createPopupMenu();

    this.popupFileAdd = createMenuItem( "Datei hinzuf\u00FCgen..." );
    this.popupMnu.add( this.popupFileAdd );

    this.popupFileRemove = createMenuItem( "Datei entfernen" );
    this.popupFileRemove.setEnabled( false );
    this.popupMnu.add( this.popupFileRemove );



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
    add( GUIFactory.createLabel( "Dateien:" ), gbc );

    this.listFiles = GUIFactory.createList();
    this.listFiles.setSelectionMode(
		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.listFiles.setVisibleRowCount( 2 );
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.gridx++;
    add( GUIFactory.createScrollPane( this.listFiles ), gbc );

    this.btnFileAdd = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_ADD_FILE );
    this.btnFileAdd.addActionListener( this );
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.gridheight = 1;
    gbc.gridx++;
    add( this.btnFileAdd, gbc );

    this.btnFileRemove = GUIFactory.createRelImageResourceButton(
					this,
					"file/delete.png",
					"Datei entfernen" );
    this.btnFileRemove.setEnabled( false );
    this.btnFileRemove.addActionListener( this );
    gbc.gridy++;
    add( this.btnFileRemove, gbc );

    this.listFiles.addListSelectionListener( this );
    this.listFiles.addMouseListener( this );


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
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      this.editorPane.setPreferredSize( new Dimension( 300, 300 ) );
      pack();
      this.editorPane.setPreferredSize( null );
      setScreenCentered();
    }
    updResult();
  }


	/* --- private Methoden --- */

  private void doFileAdd()
  {
    java.util.List<File> files = FileUtil.showMultiFileOpenDlg(
			this,
			"Dateien \u00F6ffnen",
			Main.getLastDirFile( Main.FILE_GROUP_HEXDIFF ) );
    if( files != null ) {
      if( !files.isEmpty() ) {
	if( addFiles( files ) > 0 ) {
	  Main.setLastFile( files.get( 0 ), Main.FILE_GROUP_HEXDIFF );
	}
      }
    }
  }


  private void doFileRemove()
  {
    int[] indices = this.listFiles.getSelectedIndices();
    if( indices != null ) {
      if( indices.length > 0 ) {
	Arrays.sort( indices );
	for( int i = indices.length - 1; i >= 0; --i ) {
	  int idx = indices[ i ];
	  if( (idx >= 0) && (idx <= this.files.size()) ) {
	    this.files.remove( idx );
	    this.listFiles.setListData( this.files );
	  }
	}
	updResult();
      }
    }
  }


  private void doMaxDiffs()
  {
    int         vOld = getMaxDiffs();
    ReplyIntDlg dlg  = new ReplyIntDlg(
			this,
			"Max. Dateiunterschiede (0 f\u00FCr unendlich):",
			vOld,
			0,
			null );
    dlg.setTitle( EmuUtil.TEXT_SETTINGS );
    dlg.setVisible( true );
    Integer v = dlg.getReply();
    if( v != null ) {
      Main.setProperty( "jkcemu.hexdiff.differences.max", v.toString() );
      if( (this.lastDiffs > 0) && (this.lastDiffs >= vOld) ) {
	updResult();
      }
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
		    buf.append( '\u0020' );
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
		buf.append( "Es wird nur der erste Unterschied"
						+ " angezeigt.\n" );
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
	BaseDlg.showErrorDlg( this, ex.getMessage() );
      }
    }
  }
}
