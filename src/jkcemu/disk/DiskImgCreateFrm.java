/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zum manuellen Erzeugen einer Diskettenabbilddatei
 */

package jkcemu.disk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.*;
import java.util.Collection;
import java.util.EventObject;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileEntry;
import jkcemu.base.FileNameFld;
import jkcemu.base.FileTableModel;
import jkcemu.base.HelpFrm;
import jkcemu.base.NIOFileTimesViewFactory;
import jkcemu.base.ReplyTextDlg;
import jkcemu.text.TextUtil;


public class DiskImgCreateFrm
			extends BaseFrm
			implements
				ChangeListener,
				DropTargetListener,
				FlavorListener,
				ListSelectionListener
{
  private static final String HELP_PAGE = "/help/disk/creatediskimg.htm";

  private static DiskImgCreateFrm instance = null;

  private Clipboard                 clipboard;
  private File                      lastOutFile;
  private boolean                   dataChanged;
  private JMenuItem                 mnuClose;
  private JMenuItem                 mnuNew;
  private JMenuItem                 mnuFileAdd;
  private JMenuItem                 mnuFileRemove;
  private JMenuItem                 mnuSort;
  private JMenuItem                 mnuSave;
  private JMenuItem                 mnuChangeAttrs;
  private JMenuItem                 mnuChangeUser;
  private JMenuItem                 mnuPaste;
  private JMenuItem                 mnuSelectAll;
  private JMenuItem                 mnuHelpContent;
  private JButton                   btnFileAdd;
  private JButton                   btnFileRemove;
  private JButton                   btnSave;
  private JButton                   btnFileUp;
  private JButton                   btnFileDown;
  private JButton                   btnSysTrackFileSelect;
  private JButton                   btnSysTrackFileRemove;
  private JTabbedPane               tabbedPane;
  private FileTableModel            tableModel;
  private JTable                    table;
  private JScrollPane               scrollPane;
  private FloppyDiskFormatSelectFld fmtSelectFld;
  private JLabel                    labelSysTrackFile;
  private FileNameFld               fldSysTrackFileName;
  private JTextField                fldRemark;
  private JLabel                    labelStatus;
  private DropTarget                dropTargetFile1;
  private DropTarget                dropTargetFile2;
  private DropTarget                dropTargetSysTrackFile;


  public static void open()
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
        instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new DiskImgCreateFrm();
    }
    instance.toFront();
    instance.setVisible( true );
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src == this.tabbedPane ) {
	updActionButtons();
	updStatusText();
      }
      else if( src == this.fmtSelectFld ) {
	updSysTrackFileFieldsEnabled();
	updStatusText();
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
    Object src = e.getSource();
    if( (src == this.dropTargetFile1) || (src == this.dropTargetFile2) ) {
      if( EmuUtil.isFileDrop( e ) ) {
	e.acceptDrop( DnDConstants.ACTION_COPY );  // Quelle nicht loeschen
	pasteFiles( e.getTransferable() );
      }
    } else if( src == this.dropTargetSysTrackFile ) {
      File file = EmuUtil.fileDrop( this, e );
      if( file != null ) {
	addSysTrackFile( file );
      }
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- FlavorListener --- */

  @Override
  public void flavorsChanged( FlavorEvent e )
  {
    updPasteButton();
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    updActionButtons();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    updStatusText();
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.mnuClose ) {
      rv = true;
      doClose();
    }
    else if( (src == this.mnuFileAdd) || (src == this.btnFileAdd) ) {
      rv = true;
      doFileAdd();
    }
    else if( (src == this.mnuFileRemove) || (src == this.btnFileRemove) ) {
      rv = true;
      doFileRemove();
    }
    else if( src == this.mnuNew ) {
      rv = true;
      doNew();
    }
    else if( src == this.mnuSort ) {
      rv = true;
      this.tableModel.sortAscending( 0 );
    }
    else if( (src == this.mnuSave) || (src == this.btnSave) ) {
      rv = true;
      doSave();
    }
    else if( src == this.btnFileDown ) {
      rv = true;
      doFileDown();
    }
    else if( src == this.btnFileUp ) {
      rv = true;
      doFileUp();
    }
    else if( src == this.btnSysTrackFileSelect ) {
      rv = true;
      doSysTrackFileSelect();
    }
    else if( src == this.mnuChangeAttrs ) {
      rv = true;
      doChangeAttrs();
    }
    else if( src == this.mnuChangeUser ) {
      rv = true;
      doChangeUser();
    }
    else if( src == this.mnuPaste ) {
      rv = true;
      doPaste();
    }
    else if( src == this.mnuSelectAll ) {
      rv = true;
      doSelectAll();
    }
    else if( src == this.btnSysTrackFileRemove ) {
      rv = true;
      doSysTrackFileRemove();
    }
    else if( src == this.mnuHelpContent ) {
      rv = true;
      HelpFrm.open( HELP_PAGE );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = true;
    if( this.dataChanged ) {
      setState( Frame.NORMAL );
      toFront();
      if( !BaseDlg.showYesNoDlg(
		this,
		"Daten ge\u00E4ndert!\n"
			+ "Trotzdem schlie\u00DFen?" ) )
      {
	rv = false;
      }
    }
    if( rv ) {
      this.tableModel.clear( true );
      updStatusText();
      rv = super.doClose();
    }
    if( rv ) {
      this.dataChanged = false;
      Main.checkQuit( this );
    }
    return rv;
  }


	/* --- Aktionen --- */

  private void doChangeAttrs()
  {
    int[] rowNums = this.table.getSelectedRows();
    if( rowNums != null ) {
      if( rowNums.length > 0 ) {
	ChangeFileAttrsDlg dlg = new ChangeFileAttrsDlg( this );
	dlg.setVisible( true );
	Boolean readOnly = dlg.getReadOnlyValue();
	Boolean sysFile  = dlg.getSystemFileValue();
	Boolean archive  = dlg.getArchiveValue();
	if( (readOnly != null) || (sysFile != null) || (archive != null) ) {
	  for( int i = 0; i< rowNums.length; i++ ) {
	    int       rowNum = rowNums[ i ];
	    FileEntry entry  = this.tableModel.getRow( rowNums[ i ] );
	    if( entry != null ) {
	      if( readOnly != null ) {
		entry.setReadOnly( readOnly.booleanValue() );
	      }
	      if( sysFile != null ) {
		entry.setSystemFile( sysFile.booleanValue() );
	      }
	      if( archive != null ) {
		entry.setArchive( archive.booleanValue() );
	      }
	      this.tableModel.fireTableRowsUpdated( rowNum, rowNum );
	    }
	  }
	}
      }
    }
  }


  private void doChangeUser()
  {
    int[] rowNums = this.table.getSelectedRows();
    if( rowNums != null ) {
      if( rowNums.length > 0 ) {

	// voreingestellten User-Bereich ermitteln
	int value = -1;
	for( int i = 0; i< rowNums.length; i++ ) {
	  FileEntry entry = this.tableModel.getRow( rowNums[ i ] );
	  if( entry != null ) {
	    Integer userNum = entry.getUserNum();
	    if( userNum != null ) {
	      if( value >= 0 ) {
		if( userNum.intValue() != value ) {
		  value = -1;
		  break;
		}
	      } else {
		value = userNum.intValue();
	      }
	    }
	  }
	}
	if( (value < 0) || (value > 15) ) {
	  value = 0;
	}

	// Dialog anzeigen
	JPanel panel = new JPanel(
		new FlowLayout( FlowLayout.CENTER, 5, 5 ) );
	panel.add( new JLabel( "User-Bereich:" ) );

	JSpinner spinner = new JSpinner(
		new SpinnerNumberModel( value, 0, 15, 1 ) );
	panel.add( spinner );

	int option = JOptionPane.showConfirmDialog(
			this,
			panel,
			"User-Bereich \u00E4ndern",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE );
	if( option == JOptionPane.OK_OPTION ) {
	  Object o = spinner.getValue();
	  if( o != null ) {
	    if( o instanceof Number ) {
	      value = ((Number) o).intValue();
	      if( (value >= 0) && (value <= 15) ) {
		Integer userNum = value;
		for( int i = 0; i< rowNums.length; i++ ) {
		  int       rowNum = rowNums[ i ];
		  FileEntry entry  = this.tableModel.getRow( rowNum );
		  if( entry != null ) {
		    entry.setUserNum( userNum );
		    this.tableModel.fireTableRowsUpdated( rowNum, rowNum );
		  }
		}
	      }
	    }
	  }
	}
      }
    }
  }


  public void doNew()
  {
    boolean       status = true;
    StringBuilder buf    = new StringBuilder( 256 );
    if( this.dataChanged ) {
      buf.append( "Die letzten \u00C3nderungen wurden nicht gespeichert!" );
    }
    if( (this.tableModel.getRowCount() > 0)
	|| (this.fldSysTrackFileName.getFile() != null) )
    {
      if( buf.length() > 0 ) {
	buf.append( (char) '\n' );
      }
      buf.append( "Die hinzugef\u00FCgten Dateien werden entfernt." );
    }
    if( buf.length() > 0 ) {
      status = BaseDlg.showConfirmDlg( this, buf.toString() );
    }
    if( status ) {
      this.tableModel.clear( true );
      this.fldSysTrackFileName.setFile( null );
      this.dataChanged = false;
      this.lastOutFile = null;
      updTitle();
    }
  }


  private void doPaste()
  {
    if( this.clipboard != null ) {
      try {
	pasteFiles( this.clipboard.getContents( this ) );
      }
      catch( IllegalStateException ex ) {}
    }
  }


  private void doSelectAll()
  {
    int nRows = this.table.getRowCount();
    if( nRows > 0 ) {
      this.table.setRowSelectionInterval( 0, nRows - 1 );
    }
  }


  private void doSave()
  {
    boolean status = true;
    if( this.tableModel.getRowCount() == 0 ) {
      status = BaseDlg.showYesNoDlg(
		this,
		"Es wurden keine Dateien hinzugef\u00FCgt.\n"
			+ "M\u00F6chten Sie eine leere Diskettenabbilddatei"
			+ " erstellen?" );
    }
    int     sysTracks    = this.fmtSelectFld.getSysTracks();
    File    sysTrackFile = this.fldSysTrackFileName.getFile();
    if( (sysTrackFile != null) && (sysTracks == 0) ) {
      if( JOptionPane.showConfirmDialog(
		this,
		"Sie haben ein Format ohne Systemspuren ausgew\u00E4hlt,\n"
			+ "aber eine Datei f\u00FCr die Systemspuren"
			+ " angegeben.\n"
			+ "Diese Datei wird ignoriert.",
		"Warnung",
		JOptionPane.OK_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE ) != JOptionPane.OK_OPTION )
      {
	status = false;
      }
    }
    if( status ) {
      File file = EmuUtil.showFileSaveDlg(
			this,
			"Diskettenabbilddatei speichern",
			this.lastOutFile != null ?
				this.lastOutFile
				: Main.getLastDirFile( Main.FILE_GROUP_DISK ),
			EmuUtil.getPlainDiskFileFilter(),
			EmuUtil.getAnaDiskFileFilter(),
			EmuUtil.getCopyQMFileFilter(),
			EmuUtil.getDskFileFilter(),
			EmuUtil.getImageDiskFileFilter(),
			EmuUtil.getTeleDiskFileFilter() );
      if( file != null ) {
	boolean plainDisk  = false;
	boolean anaDisk    = false;
	boolean cpcDisk    = false;
	boolean copyQMDisk = false;
	boolean imageDisk  = false;
	boolean teleDisk   = false;
	String  fileName   = file.getName();
	if( fileName != null ) {
	  String lowerName = fileName.toLowerCase();
	  if( lowerName.endsWith( ".gz" ) ) {
	    lowerName = lowerName.substring( 0, lowerName.length() - 3 );
	  }
	  plainDisk =  TextUtil.endsWith(
				lowerName,
				DiskUtil.plainDiskFileExt );
	  anaDisk    =  TextUtil.endsWith(
				lowerName,
				DiskUtil.anaDiskFileExt );
	  copyQMDisk =  TextUtil.endsWith(
				lowerName,
				DiskUtil.copyQMFileExt );
	  cpcDisk    =  TextUtil.endsWith(
				lowerName,
				DiskUtil.dskFileExt );
	  imageDisk  =  TextUtil.endsWith(
				lowerName,
				DiskUtil.imageDiskFileExt );
	  teleDisk  =  TextUtil.endsWith(
				lowerName,
				DiskUtil.teleDiskFileExt );
	}
	if( plainDisk || anaDisk || copyQMDisk || cpcDisk
		|| imageDisk || teleDisk )
	{
	  boolean      cancelled = false;
	  OutputStream out       = null;
	  try {
	    int sides      = this.fmtSelectFld.getSides();
	    int cyls       = this.fmtSelectFld.getCylinders();
	    int sectPerCyl = this.fmtSelectFld.getSectorsPerCylinder();
	    int sectorSize = this.fmtSelectFld.getSectorSize();

	    DiskImgCreator diskImgCreator = new DiskImgCreator(
				new NIOFileTimesViewFactory(),
				sides,
				cyls,
				sysTracks,
				sectPerCyl,
				sectorSize,
				this.fmtSelectFld.isBlockNum16Bit(),
				this.fmtSelectFld.getBlockSize(),
				this.fmtSelectFld.getDirBlocks(),
				this.fmtSelectFld.isDateStamperEnabled() );
	    if( (sysTrackFile != null) && (sysTracks > 0) ) {
	      diskImgCreator.fillSysTracks( sysTrackFile );
	    }
	    int nRows = this.tableModel.getRowCount();
	    for( int i = 0; i < nRows; i++ ) {
	      FileEntry entry = this.tableModel.getRow( i );
	      if( entry != null ) {
		try {
		    diskImgCreator.addFile(
				entry.getUserNum(),
				entry.getName(),
				entry.getFile(),
				entry.isReadOnly(),
				entry.isSystemFile(),
				entry.isArchive() );
		}
		catch( IOException ex ) {
		  fireSelectRowInterval( i, i );
		  String msg = ex.getMessage();
		  if( msg != null ) {
		    if( msg.isEmpty() ) {
		      msg = null;
		    }
		  }
		  if( msg != null ) {
		    msg = entry.getName() + ":\n" + msg;
		  } else {
		    msg = entry.getName()
				+ " kann nicht hinzugef\u00FCgt werden.";
		  }
		  if( JOptionPane.showConfirmDialog(
			this,
			msg,
			"Fehler",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.ERROR_MESSAGE ) != JOptionPane.OK_OPTION )
		  {
		    cancelled = true;
		    break;
		  }
		}
	      }
	    }
	    if( !cancelled ) {
	      byte[] diskBuf = diskImgCreator.getPlainDiskByteBuffer();
	      if( plainDisk ) {
		out = EmuUtil.createOptionalGZipOutputStream( file );
		out.write( diskBuf );
		out.close();
		out = null;
	      } else {
		PlainDisk disk = PlainDisk.createForByteArray(
					this,
					file.getPath(),
					diskBuf,
					new FloppyDiskFormat(
						sides,
						cyls,
						sectPerCyl,
						sectorSize ),
					this.fmtSelectFld.getInterleave() );
		if( disk != null ) {
		  if( anaDisk ) {
		    AnaDisk.export( disk, file );
		  } else if( copyQMDisk ) {
		    CopyQMDisk.export( disk, file, this.fldRemark.getText() );
		  } else if( cpcDisk ) {
		    CPCDisk.export( disk, file );
		  } else if( imageDisk ) {
		    ImageDisk.export( disk, file, this.fldRemark.getText() );
		  } else if( teleDisk ) {
		    TeleDisk.export( disk, file, this.fldRemark.getText() );
		  }
		}
	      }
	      this.dataChanged = false;
	      this.lastOutFile = file;
	      updTitle();
	      Main.setLastFile( file, Main.FILE_GROUP_DISK );
	      this.labelStatus.setText( "Diskettenabbilddatei gespeichert" );
	    }
	  }
	  catch( Exception ex ) {
	    BaseDlg.showErrorDlg( this, ex );
	  }
	  finally {
	    EmuUtil.closeSilent( out );
	  }
	} else {
	  BaseDlg.showErrorDlg(
		this,
		"Aus der Dateiendung der ausgew\u00E4hlten Datei\n"
			+ "kann JKCEMU das Dateiformat nicht erkennen.\n"
			+ "W\u00E4hlen Sie bitte einen Dateinamen"
			+ " mit einer f\u00FCr\n"
			+ "das gew\u00FCnschte Format \u00FCblichen"
			+ " Dateiendung aus." );
	}
      }
    }
  }


  private void doFileAdd()
  {
    java.util.List<File> files = EmuUtil.showMultiFileOpenDlg(
			this,
			"Dateien hinzuf\u00FCgen",
			Main.getLastDirFile( Main.FILE_GROUP_SOFTWARE ) );
    if( files != null ) {
      int firstRowToSelect = this.table.getRowCount();
      for( File file : files ) {
	if( addFile( file ) ) {
	  Main.setLastFile( file, Main.FILE_GROUP_SOFTWARE );
	}
      }
      updSelectAllEnabled();
      fireSelectRowInterval( firstRowToSelect, this.table.getRowCount() - 1 );
    }
  }


  private void doFileRemove()
  {
    int[] rowNums = this.table.getSelectedRows();
    if( rowNums != null ) {
      if( rowNums.length > 0 ) {
	Arrays.sort( rowNums );
	for( int i = rowNums.length - 1; i >= 0; --i ) {
	  this.tableModel.removeRow( rowNums[ i ], true );
	  this.mnuSort.setEnabled( this.tableModel.getRowCount() > 1 );
	  this.dataChanged = true;
	}
	updSelectAllEnabled();
	updStatusText();
      }
    }
  }


  private void doFileDown()
  {
    final int[] rowNums = this.table.getSelectedRows();
    if( rowNums != null ) {
      if( rowNums.length > 0) {
	Arrays.sort( rowNums );
	if( (rowNums[ rowNums.length - 1 ] + 1)
		< this.tableModel.getRowCount() )
	{
	  for( int i = rowNums.length - 1 ; i >= 0; --i ) {
	    int       row = rowNums[ i ];
	    FileEntry e1  = this.tableModel.getRow( row );
	    FileEntry e2  = this.tableModel.getRow( row + 1 );
	    if( (e1 != null) && (e2 != null) ) {
	      this.tableModel.setRow( row, e2 );
	      this.tableModel.setRow( row + 1, e1 );
	      this.tableModel.fireTableDataChanged();
	      this.dataChanged = true;
	    }
	    rowNums[ i ]++;
	  }
	}
	fireSelectRows( rowNums );
      }
    }
  }


  private void doFileUp()
  {
    final int[] rowNums = this.table.getSelectedRows();
    if( rowNums != null ) {
      if( rowNums.length > 0) {
	Arrays.sort( rowNums );
	if( rowNums[ 0 ] > 0 ) {
	  for( int i = 0; i < rowNums.length; i++ ) {
	    int       row = rowNums[ i ];
	    FileEntry e1  = this.tableModel.getRow( row );
	    FileEntry e2  = this.tableModel.getRow( row - 1 );
	    if( (e1 != null) && (e2 != null) ) {
	      this.tableModel.setRow( row, e2 );
	      this.tableModel.setRow( row - 1, e1 );
	      this.tableModel.fireTableDataChanged();
	      this.dataChanged = true;
	    }
	    --rowNums[ i ];
	  }
	}
	fireSelectRows( rowNums );
      }
    }
  }


  private void doSysTrackFileSelect()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"Datei \u00FCffnen",
			Main.getLastDirFile( Main.FILE_GROUP_SOFTWARE ) );
    if( file != null ) {
      addSysTrackFile( file );
    }
  }


  private void doSysTrackFileRemove()
  {
    this.fldSysTrackFileName.setFile( null );
    this.btnSysTrackFileRemove.setEnabled( false );
  }


	/* --- Konstruktor --- */

  private DiskImgCreateFrm()
  {
    this.clipboard   = null;
    this.dataChanged = false;
    this.lastOutFile = null;
    updTitle();
    Main.updIcon( this );

    Toolkit tk = getToolkit();
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
    }


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuNew = createJMenuItem( "Neue Diskettenabbilddatei" );
    mnuFile.add( this.mnuNew );
    mnuFile.addSeparator();

    this.mnuFileAdd = createJMenuItem( "Hinzuf\u00FCgen..." );
    mnuFile.add( this.mnuFileAdd );

    this.mnuFileRemove = createJMenuItem(
			"Entfernen",
			KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
    mnuFile.add( this.mnuFileRemove );
    mnuFile.addSeparator();

    this.mnuSort = createJMenuItem( "Sortieren" );
    this.mnuSort.setEnabled( false );
    mnuFile.add( this.mnuSort );
    mnuFile.addSeparator();

    this.mnuSave = createJMenuItem( "Diskettenabbilddatei speichern..." );
    mnuFile.add( this.mnuSave );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuPaste = createJMenuItem( "Einf\u00FCgen" );
    mnuEdit.add( this.mnuPaste );
    mnuEdit.addSeparator();

    this.mnuChangeAttrs = createJMenuItem( "Dateiattribute \u00E4ndern..." );
    mnuEdit.add( this.mnuChangeAttrs );

    this.mnuChangeUser = createJMenuItem( "User-Bereich \u00E4ndern..." );
    mnuEdit.add( this.mnuChangeUser );
    mnuEdit.addSeparator();

    this.mnuSelectAll = createJMenuItem( "Alles ausw\u00E4hlem" );
    this.mnuSelectAll.setEnabled( false );
    mnuEdit.add( this.mnuSelectAll );


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

    this.btnFileAdd = createImageButton(
				"/images/file/open.png",
				"Hinzuf\u00FCgen" );
    toolBar.add( this.btnFileAdd );

    this.btnFileRemove = createImageButton(
				"/images/file/delete.png",
				"Entfernen" );
    toolBar.add( this.btnFileRemove );
    toolBar.addSeparator();

    this.btnSave = createImageButton(
				"/images/file/save_as.png",
				"Diskettenabbilddatei speichern" );
    toolBar.add( this.btnSave );
    toolBar.addSeparator();

    this.btnFileDown = createImageButton(
				"/images/nav/down.png",
				"Nach unten" );
    toolBar.add( this.btnFileDown );

    this.btnFileUp = createImageButton( "/images/nav/up.png", "Nach oben" );
    toolBar.add( this.btnFileUp );


    // TabbedPane
    this.tabbedPane = new JTabbedPane();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( this.tabbedPane, gbc );


    // Tabellenfeld
    this.tableModel = new FileTableModel(
				FileTableModel.Column.NAME,
				FileTableModel.Column.FILE,
				FileTableModel.Column.SIZE,
				FileTableModel.Column.LAST_MODIFIED,
				FileTableModel.Column.USER_NUM,
				FileTableModel.Column.READ_ONLY,
				FileTableModel.Column.SYSTEM_FILE,
				FileTableModel.Column.ARCHIVE );

    this.table = new JTable( this.tableModel );
    this.table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.table.setColumnSelectionAllowed( false );
    this.table.setPreferredScrollableViewportSize(
					new Dimension( 700, 300 ) );
    this.table.setRowSelectionAllowed( true );
    this.table.setRowSorter( null );
    this.table.setShowGrid( false );
    this.table.setShowHorizontalLines( false );
    this.table.setShowVerticalLines( false );
    this.table.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.table.addMouseListener( this );

    EmuUtil.setTableColWidths(
		this.table, 100, 280, 70, 130, 40, 40, 40, 40 );

    ListSelectionModel selectionModel = this.table.getSelectionModel();
    if( selectionModel != null ) {
      selectionModel.addListSelectionListener( this );
      updActionButtons();
    }

    this.scrollPane = new JScrollPane( this.table );
    this.tabbedPane.addTab( "Dateien", this.scrollPane );


    // Format
    JPanel panelFmt = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Format", new JScrollPane( panelFmt ) );

    GridBagConstraints gbcFmt = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.fmtSelectFld = new FloppyDiskFormatSelectFld( true );
    panelFmt.add( this.fmtSelectFld, gbcFmt );

    this.labelSysTrackFile = new JLabel( "Datei f\u00FCr Systemspuren:" );
    gbcFmt.insets.top    = 5;
    gbcFmt.insets.left   = 5;
    gbcFmt.insets.bottom = 5;
    gbcFmt.gridwidth     = 1;
    gbcFmt.gridx         = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelSysTrackFile, gbcFmt );

    this.fldSysTrackFileName = new FileNameFld();
    this.fldSysTrackFileName.setEditable( false );
    gbcFmt.fill      = GridBagConstraints.HORIZONTAL;
    gbcFmt.weightx   = 1.0;
    gbcFmt.gridwidth = 5;
    gbcFmt.gridx++;
    panelFmt.add( this.fldSysTrackFileName, gbcFmt );

    this.btnSysTrackFileSelect = createImageButton(
				"/images/file/open.png",
				"\u00D6ffnen" );
    gbcFmt.fill      = GridBagConstraints.NONE;
    gbcFmt.weightx   = 0.0;
    gbcFmt.gridwidth = 1;
    gbcFmt.gridx += 5;
    panelFmt.add( this.btnSysTrackFileSelect, gbcFmt );

    this.btnSysTrackFileRemove = createImageButton(
				"/images/file/delete.png",
				"\u00D6ffnen" );
    this.btnSysTrackFileRemove.setEnabled( false );
    gbcFmt.gridx++;
    panelFmt.add( this.btnSysTrackFileRemove, gbcFmt );


    // Kommentar
    JPanel panelRemark = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Kommentar", new JScrollPane( panelRemark ) );

    GridBagConstraints gbcRemark = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    panelRemark.add(
	new JLabel( "Kommentar zur Diskettenabbilddatei:" ),
	gbcRemark );

    this.fldRemark          = new JTextField( "Erzeugt mit JKCEMU" );
    gbcRemark.fill          = GridBagConstraints.HORIZONTAL;
    gbcRemark.weightx       = 1.0;
    gbcRemark.insets.top    = 0;
    gbcRemark.insets.bottom = 5;
    gbcRemark.gridy++;
    panelRemark.add( this.fldRemark, gbcRemark );

    JLabel label = new JLabel( "Achtung!" );
    Font   font  = label.getFont();
    if( font != null ) {
      label.setFont( font.deriveFont( Font.BOLD ) );
    }
    gbcRemark.fill          = GridBagConstraints.NONE;
    gbcRemark.weightx       = 0.0;
    gbcRemark.insets.top    = 10;
    gbcRemark.insets.bottom = 0;
    gbcRemark.gridy++;
    panelRemark.add( label, gbcRemark );

    gbcRemark.fill          = GridBagConstraints.NONE;
    gbcRemark.weightx       = 0.0;
    gbcRemark.insets.top    = 0;
    gbcRemark.insets.bottom = 5;
    gbcRemark.gridy++;
    panelRemark.add(
	new JLabel( "Ein Kommentar wird nur bei CopyQM-, ImageDisk-"
			+ " und TeleDisk-Dateien unterst\u00FCtzt." ),
	gbcRemark );


    // Statuszeile
    this.labelStatus = new JLabel();
    gbc.fill         = GridBagConstraints.HORIZONTAL;
    gbc.weighty      = 0.0;
    gbc.insets.top   = 0;
    gbc.gridy++;
    add( this.labelStatus, gbc );
    updStatusText();


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );


    // Listener
    this.tabbedPane.addChangeListener( this );
    this.fmtSelectFld.addChangeListener( this );
    if( this.clipboard != null ) {
      this.clipboard.addFlavorListener( this );
    }


    // Drop-Ziele
    this.dropTargetFile1        = new DropTarget( this.table, this );
    this.dropTargetFile2        = new DropTarget( this.scrollPane, this );
    this.dropTargetSysTrackFile = new DropTarget(
					this.fldSysTrackFileName,
					this );

    this.dropTargetFile1.setActive( true );
    this.dropTargetFile1.setActive( true );
    this.dropTargetSysTrackFile.setActive( true );


    // sonstiges
    updActionButtons();
    updBgColor();
    updPasteButton();
  }


	/* --- private Methoden --- */

  private boolean addFile( File file )
  {
    boolean rv   = false;
    boolean done = false;
    if( file.isDirectory() ) {
      String s = file.getName();
      if( s != null ) {
	try {
	  int userNum = Integer.parseInt( s );
	  if( (userNum >= 1) && (userNum <= 15) ) {
	    done = true;
	    if( BaseDlg.showYesNoDlg(
			this,
			"Sollen die Dateien im Verzeichnis " + s 
				+ "\nin der Benutzerebene " + s
				+ " hinzugef\u00FCgt werden?" ) )
	    {
	      File[] files = file.listFiles();
	      if( files != null ) {
		for( File f : files ) {
		  if( f.isFile() ) {
		    rv = addFileInternal( userNum, f );
		    if( !rv ) {
		      break;
		    }
		  }
		}
	      }
	    }
	  }
	}
	catch( NumberFormatException ex ) {}
      }
    }
    if( !done ) {
      rv = addFileInternal( 0, file );
    }
    return rv;
  }


  private boolean addFileInternal( int  userNum, File file )
  {
    boolean rv   = false;
    long    size = -1L;
    if( file.isFile() ) {
      size = file.length();
      if( size < 1 ) {
	String fName = file.getName();
	if( fName != null ) {
	  if( fName.isEmpty() ) {
	    fName = null;
	  }
	}
	if( fName == null ) {
	  fName = file.getPath();
	}
	if( !BaseDlg.showYesNoDlg(
		this,
		"Datei " + fName
			+ ":\nDie Datei ist leer!\n"
			+ "Trotzdem hinzuf\u00FCgen?" ) )
	{
	  file = null;
	}
      }
      else if( size > (1024 * 16 * 32) ) {
	BaseDlg.showErrorDlg( this, "Datei ist zu gro\u00DF!" );
	file = null;
      } else {
	String fileName = file.getName();
	if( fileName != null ) {
	  if( fileName.equalsIgnoreCase(
			DirectoryFloppyDisk.SYS_FILE_NAME ) )
	  {
	    if( BaseDlg.showYesNoDlg(
			this,
			DirectoryFloppyDisk.SYS_FILE_NAME + ":\n"
				+ "JKCEMU verwendet Dateien mit diesem Namen"
				+ " f\u00FCr den Inhalt der Systemspuren.\n"
				+ "M\u00F6chten Sie deshalb nun diese Datei"
				+ " f\u00FCr die Systemspuren verwenden,\n"
				+ "anstelle Sie als gew\u00F6hnliche Datei"
				+ " im Directory einzubinden?" ) )
	    {
	      this.fldSysTrackFileName.setFile( file );
	      this.btnSysTrackFileRemove.setEnabled( true );
	      file = null;
	      rv   = true;
	    }
	  }
	}
      }
    } else {
      BaseDlg.showErrorDlg(
		this,
		"Es k\u00F6nnen nur regul\u00E4re Dateien"
			+ " hinzugef\u00FCgt werden." );
      file = null;
    }
    if( file != null ) {
      String entryName = null;
      String fileName  = file.getName();
      if( fileName != null ) {
	entryName = createEntryName( fileName );
      }
      if( entryName == null ) {
	String title        = "Dateiname";
	String defaultReply = null;
	if( fileName != null ) {
	  if( !fileName.isEmpty() ) {
	    title = "Datei: " + fileName;
	  }
	  StringBuilder buf = new StringBuilder( 12 );
	  int           len = fileName.length();
	  int           n   = 0;
	  for( int i = 0; (n < 8) && (i < len); i++ ) {
	    char ch = Character.toUpperCase( fileName.charAt( i ) );
	    if( DiskUtil.isValidCPMFileNameChar( ch ) ) {
	      buf.append( ch );
	      n++;
	    }
	    else if( ch == '.' ) {
	      break;
	    }
	  }
	  if( n > 0 ) {
	    int pos = fileName.lastIndexOf( '.' );
	    if( pos > 0 ) {
	      boolean p = true;
	      n         = 0;
	      for( int i = pos + 1; (n < 3) && (i < len); i++ ) {
		char ch = Character.toUpperCase( fileName.charAt( i ) );
		if( DiskUtil.isValidCPMFileNameChar( ch ) ) {
		  if( p ) {
		    buf.append( (char) '.' );
		    p = false;
		  }
		  buf.append( ch );
		  n++;
		}
	      }
	    }
	  }
	  defaultReply = buf.toString();
	}
	String reply = null;
	do {
	  reply = ReplyTextDlg.showReplyTextDlg(
					this,
					"Dateiname im 8.3-Format:",
					title,
					defaultReply );
	  if( reply != null ) {
	    entryName = createEntryName( reply );
	    if( entryName == null ) {
	      BaseDlg.showErrorDlg(
			this,
			"Der eingegebene Name ist ung\u00FCltig." );
	    }
	  }
	} while( (reply != null) && (entryName == null) );
      }
      if( entryName != null ) {
	boolean exists = false;
	int     nRows  = this.tableModel.getRowCount();
	for( int i = 0; i < nRows; i++ ) {
	  FileEntry entry = this.tableModel.getRow( i );
	  if( entry != null ) {
	    if( entry.getName().equals( entryName ) ) {
	      this.table.setRowSelectionInterval( i, i );
	      exists = true;
	      break;
	    }
	  }
	}
	if( exists ) {
	  BaseDlg.showErrorDlg(
		this,
		"Es existiert bereits ein Eintrag mit diesem Namen." );
	} else {
	  FileEntry entry = new FileEntry( entryName );
	  entry.setUserNum( userNum );
	  if( size >= 0 ) {
	    entry.setSize( size );
	  }
	  long lastModified = file.lastModified();
	  if( lastModified != 0 ) {
	    entry.setLastModified( lastModified );
	  }
	  entry.setFile( file );
	  entry.setReadOnly( !file.canWrite() );
	  entry.setSystemFile( false );
	  entry.setArchive( false );
	  this.tableModel.addRow( entry, true );
	  updStatusText();
	  this.mnuSort.setEnabled( this.tableModel.getRowCount() > 1 );
	  this.dataChanged = true;
	  rv               = true;
	}
      }
    }
    return rv;
  }


  private void addSysTrackFile( File file )
  {
    String errMsg = null;
    if( !file.exists() ) {
      errMsg = "Datei nicht gefunden";
    }
    else if( !file.isFile() ) {
      errMsg = "Datei ist keine regul\u00E4re Datei";
    }
    if( errMsg != null ) {
      BaseDlg.showErrorDlg( this, errMsg );
    } else {
      this.fldSysTrackFileName.setFile( file );
      this.btnSysTrackFileRemove.setEnabled( true );
    }
  }


  private static String createEntryName( String fileName )
  {
    StringBuilder buf    = new StringBuilder( 12 );
    boolean       failed = false;
    boolean       point  = false;
    int           nPre   = 0;
    int           nPost  = 0;
    int           len    = fileName.length();
    for( int i = 0; !failed && (i < len); i++ ) {
      char ch = Character.toUpperCase( fileName.charAt( i ) );
      if( DiskUtil.isValidCPMFileNameChar( ch ) ) {
	if( !point && (nPre < 8) ) {
	  buf.append( ch );
	  nPre++;
	} else if( point && (nPost < 3) ) {
	  buf.append( ch );
	  nPost++;
	} else {
	  failed = true;
	}
      } else if( ch == '.' ) {
	if( !point && (nPre >= 1) && (nPre <= 8) ) {
	  buf.append( ch );
	  point = true;
	} else {
	  failed = true;
	}
      } else {
	failed = true;
      }
    }
    return failed ? null : buf.toString();
  }


  private void fireSelectRows( final int[] rowNums )
  {
    final JTable table = this.table;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    table.clearSelection();
		    for( int row : rowNums ) {
		      table.addRowSelectionInterval( row, row );
		    }
		  }
		} );
  }


  private void fireSelectRowInterval( final int begRow, final int endRow )
  {
    final JTable table = this.table;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    try {
		      int nRows = table.getRowCount();
		      if( (begRow >= 0)
			  && (begRow < nRows)
			  && (begRow <= endRow) )
		      {
			table.setRowSelectionInterval(
						begRow,
						Math.min( endRow, nRows -1 ) );
		      }
		    }
		    catch( IllegalArgumentException ex ) {}
		  }
		} );
  }


  private void pasteFiles( Transferable t )
  {
    try {
      if( t != null ) {
	Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
	if( o != null ) {
	  if( o instanceof Collection ) {
	    int firstRowToSelect = this.table.getRowCount();
	    for( Object item : (Collection) o ) {
	      if( item != null ) {
		File file = null;
		if( item instanceof File ) {
		  file = (File) item;
		} else if( item instanceof String ) {
		  file= new File( (String) item );
		}
		if( file != null ) {
		  if( !addFile( file ) ) {
		    break;
		  }
		}
	      }
	    }
	    fireSelectRowInterval(
			firstRowToSelect,
			this.table.getRowCount() - 1 );
	  }
	}
      }
    }
    catch( IOException ex ) {}
    catch( UnsupportedFlavorException ex ) {}
    finally {
      updSelectAllEnabled();
    }
  }


  private void updActionButtons()
  {
    boolean stateSelected = false;
    boolean stateDown     = false;
    boolean stateUp       = false;
    boolean fileTab       = (this.tabbedPane.getSelectedIndex() == 0);
    if( fileTab ) {
      int[] rowNums = this.table.getSelectedRows();
      if( rowNums != null ) {
	if( rowNums.length > 0 ) {
	  stateSelected = true;
	  Arrays.sort( rowNums );
	  if( (rowNums[ rowNums.length - 1 ] + 1)
				< this.tableModel.getRowCount() )
	  {
	    stateDown = true;
	  }
	  if( rowNums[ 0 ] > 0 ) {
	    stateUp = true;
	  }
	}
      }
    }
    this.btnFileAdd.setEnabled( fileTab );
    this.mnuFileRemove.setEnabled( stateSelected );
    this.btnFileRemove.setEnabled( stateSelected );
    this.btnFileDown.setEnabled( stateDown );
    this.btnFileUp.setEnabled( stateUp );
    this.mnuChangeAttrs.setEnabled( stateSelected );
    this.mnuChangeUser.setEnabled( stateSelected );
  }


  private void updBgColor()
  {
    Color     color = this.table.getBackground();
    JViewport vp    = this.scrollPane.getViewport();
    if( (color != null) && (vp != null) ) {
      vp.setBackground( color );
    }
  }


  private void updPasteButton()
  {
    boolean state = false;
    if( this.clipboard != null ) {
      try {
	state = this.clipboard.isDataFlavorAvailable(
					DataFlavor.javaFileListFlavor );
      }
      catch( IllegalStateException ex ) {}
    }
    this.mnuPaste.setEnabled( state );
  }


  private void updSelectAllEnabled()
  {
    this.mnuSelectAll.setEnabled( this.table.getRowCount() > 0 );
  }


  private void updStatusText()
  {
    String text = "Bereit";
    int    idx  = this.tabbedPane.getSelectedIndex();
    if( idx == 0 ) {
      long kbytes = 0;
      int  nRows  = this.tableModel.getRowCount();
      for( int i = 0; i < nRows; i++ ) {
	FileEntry entry = this.tableModel.getRow( i );
	if( entry != null ) {
	  Long size = entry.getSize();
	  if( size != null ) {
	    if( size.longValue() > 0 ) {
	      long kb = size.longValue() / 1024L;
	      if( (kb * 1024L) < size.longValue() ) {
		kb++;
	      }
	      kbytes += kb;
	    }
	  }
	}
      }
      StringBuilder buf = new StringBuilder( 64 );
      if( nRows == 1 ) {
	buf.append( "1 Datei" );
      } else {
	buf.append( nRows );
	buf.append( " Dateien" );
      }
      buf.append( " mit " );
      buf.append( kbytes );
      buf.append( " KByte hinzugef\u00FCgt" );
      text = buf.toString();
    } else if( idx == 1 ) {
      int sides      = this.fmtSelectFld.getSides();
      int cyls       = this.fmtSelectFld.getCylinders();
      int sysTracks  = this.fmtSelectFld.getSysTracks();
      int sectPerCyl = this.fmtSelectFld.getSectorsPerCylinder();
      int sectorSize = this.fmtSelectFld.getSectorSize();
      int kbytes     = sides * cyls * sectPerCyl * sectorSize / 1024;
      if( sysTracks > 0 ) {
	text = String.format(
			"%d/%dK Diskettenformat eingestellt",
			sides * (cyls - sysTracks) * sectPerCyl * sectorSize
								/ 1024,
			kbytes );

      } else {
	text = String.format( "%dK Diskettenformat eingestellt", kbytes );
      }
    }
    this.labelStatus.setText( text );
  }


  private void updSysTrackFileFieldsEnabled()
  {
    boolean state = (this.fmtSelectFld.getSysTracks() > 0);
    this.labelSysTrackFile.setEnabled( state );
    this.fldSysTrackFileName.setEnabled( state );
    this.btnSysTrackFileSelect.setEnabled( state );
    this.btnSysTrackFileRemove.setEnabled(
		state && (this.fldSysTrackFileName.getFile() != null) );
  }


  private void updTitle()
  {
    String text = "JKCEMU CP/M-Diskettenabbilddatei erstellen";
    if( this.lastOutFile != null ) {
      StringBuilder buf = new StringBuilder( 256 );
      buf.append( text );
      buf.append( ": " );
      String fName = this.lastOutFile.getName();
      if( fName != null ) {
	buf.append( fName );
      }
      text = buf.toString();
    }
    setTitle( text );
  }
}
