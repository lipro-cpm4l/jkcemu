/*
 * (c) 2009-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zum manuellen Erzeugen einer Diskettenabbilddatei
 */

package jkcemu.disk;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.text.TextUtil;


public class DiskImgCreateFrm
			extends BasicFrm
			implements
				ChangeListener,
				DropTargetListener,
				FlavorListener,
				ListSelectionListener
{
  private static DiskImgCreateFrm instance = null;

  private Clipboard         clipboard;
  private File              lastOutFile;
  private boolean           dataChanged;
  private JMenuItem         mnuClose;
  private JMenuItem         mnuFileAdd;
  private JMenuItem         mnuFileRemove;
  private JMenuItem         mnuSort;
  private JMenuItem         mnuSave;
  private JMenuItem         mnuChangeUser;
  private JMenuItem         mnuPaste;
  private JMenuItem         mnuHelpContent;
  private JButton           btnFileAdd;
  private JButton           btnFileRemove;
  private JButton           btnSave;
  private JButton           btnFileUp;
  private JButton           btnFileDown;
  private JButton           btnSysFileSelect;
  private JButton           btnSysFileRemove;
  private JTabbedPane       tabbedPane;
  private FileTableModel    tableModel;
  private JTable            table;
  private JScrollPane       scrollPane;
  private JRadioButton      btnFmt800K;
  private JRadioButton      btnFmt780K;
  private JRadioButton      btnFmt720K;
  private JRadioButton      btnFmt624K;
  private JRadioButton      btnFmtCPL;
  private JRadioButton      btnFmtEtc;
  private JLabel            labelSides;
  private JLabel            labelCyls;
  private JLabel            labelSysCyls;
  private JLabel            labelSectPerCyl;
  private JLabel            labelSectorSize;
  private JLabel            labelSectorSizeUnit;
  private JLabel            labelBlockSize;
  private JLabel            labelBlockSizeUnit;
  private JLabel            labelBlockNumSize;
  private JLabel            labelBlockNumSizeUnit;
  private JLabel            labelDirBlocks;
  private JLabel            labelDirEntriesInfo;
  private JLabel            labelSysFile;
  private JComboBox         comboSides;
  private JComboBox         comboCyls;
  private JSpinner          spinnerSysCyls;
  private JComboBox         comboSectPerCyl;
  private JComboBox         comboSectorSize;
  private JComboBox         comboBlockSize;
  private JComboBox         comboBlockNumSize;
  private JSpinner          spinnerDirBlocks;
  private FileNameFld       fldSysFileName;
  private JLabel            labelStatus;
  private DropTarget        dropTargetFile1;
  private DropTarget        dropTargetFile2;
  private DropTarget        dropTargetSysFile;


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
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.tabbedPane ) {
	  updActionButtons();
	  updStatusText();
	}
	else if( src == this.spinnerSysCyls ) {
	  updSysFileFieldsEnabled();
	  updStatusText();
	}
	else if( src == this.spinnerDirBlocks ) {
	  updDirEntriesInfo();
	}
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
    } else if( src == this.dropTargetSysFile ) {
      File file = EmuUtil.fileDrop( this, e );
      if( file != null ) {
	addSysFile( file );
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
    boolean rv = false;
    updStatusText();
    if( e != null ) {
      Object src = e.getSource();
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
      else if( src == this.btnSysFileSelect ) {
	rv = true;
	doSysFileSelect();
      }
      else if( src == this.mnuChangeUser ) {
	rv = true;
	doChangeUser();
      }
      else if( src == this.mnuPaste ) {
	rv = true;
	doPaste();
      }
      else if( src == this.btnSysFileRemove ) {
	rv = true;
	doSysFileRemove();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.open( "/help/disk/creatediskimg.htm" );
      }
      else if( (src == this.btnFmt800K)
	       || (src == this.btnFmt780K)
	       || (src == this.btnFmt720K)
	       || (src == this.btnFmt624K)
	       || (src == this.btnFmtCPL)
	       || (src == this.btnFmtEtc) )
      {
	rv = true;
	updFmtDetailsFields();
      }
      else if( (src == this.comboSides)
	       || (src == this.comboCyls)
	       || (src == this.comboSectPerCyl)
	       || (src == this.comboSectorSize) )
      {
	rv = true;
	updStatusText();
      }
      else if( src == this.comboBlockSize ) {
	rv = true;
	updDirEntriesInfo();
      }
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
      if( !BasicDlg.showYesNoDlg(
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
		Integer userNum = new Integer( value );
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


  private void doPaste()
  {
    if( this.clipboard != null ) {
      try {
	pasteFiles( this.clipboard.getContents( this ) );
      }
      catch( IllegalStateException ex ) {}
    }
  }


  private void doSave()
  {
    boolean status = true;
    if( this.tableModel.getRowCount() == 0 ) {
      status = BasicDlg.showYesNoDlg(
		this,
		"Es wurden keine Dateien hinzugef\u00FCgt.\n"
			+ "M\u00F6chten Sie eine leere Diskettenabbilddatei"
			+ " erstellen?" );
    }
    if( status ) {
      File file = EmuUtil.showFileSaveDlg(
				this,
				"Diskettenabbilddatei speichern",
				this.lastOutFile != null ?
					this.lastOutFile
					: Main.getLastPathFile( "disk" ),
				EmuUtil.getPlainDiskFileFilter(),
				EmuUtil.getAnaDiskFileFilter(),
				EmuUtil.getDskFileFilter(),
				EmuUtil.getImageDiskFileFilter() );
      if( file != null ) {
	boolean plainDisk = false;
	boolean anaDisk   = false;
	boolean cpcDisk   = false;
	boolean imageDisk = false;
	String  fileName  = file.getName();
	if( fileName != null ) {
	  String lowerName = fileName.toLowerCase();
	  plainDisk =  TextUtil.endsWith(
				lowerName,
				DiskUtil.plainDiskFileExt );
	  anaDisk   =  TextUtil.endsWith(
				lowerName,
				DiskUtil.anaDiskFileExt );
	  cpcDisk   =  TextUtil.endsWith(
				lowerName,
				DiskUtil.dskFileExt );
	  imageDisk =  TextUtil.endsWith(
				lowerName,
				DiskUtil.imageDiskFileExt );
	}
	if( plainDisk || anaDisk || cpcDisk || imageDisk ) {
	  OutputStream out = null;
	  try {
	    int  sides       = getIntValue( this.comboSides );
	    int  cyls        = getIntValue( this.comboCyls );
	    int  sysCyls     = getIntValue( this.spinnerSysCyls );
	    int  sectPerCyl  = getIntValue( this.comboSectPerCyl );
	    int  sectorSize  = getIntValue( this.comboSectorSize );
	    int  blockSize   = getIntValue( this.comboBlockSize ) * 1024;
	    int  dirBlocks   = getIntValue( this.spinnerDirBlocks );
	    int  diskSize    = sides * cyls * sectPerCyl * sectorSize;
	    int  dstDirPos   = sysCyls * sides * sectPerCyl * sectorSize;
	    int  dirSize     = dirBlocks * blockSize;
	    int  begFileArea = dstDirPos + dirSize;
	    int  dstFilePos  = begFileArea;
	    File sysFile     = null;
	    if( dstDirPos > 0 ) {
	      sysFile = this.fldSysFileName.getFile();
	      if( sysFile != null ) {
		if( sysFile.length() > dstDirPos ) {
		  throw new IOException(
			"Die Datei f\u00FCr die Systemspuren"
				+ " ist zu gro\u00DF!" );
		}
	      }
	    }
	    if( diskSize < (dstFilePos + (calcFileAreaKBytes( blockSize )
						* 1024)) )
	    {
	      throw new IOException(
			"Das ausgew\u00E4hlten Diskettenformat bietet nicht"
				+ " gen\u00FCgend Platz\n"
				+ "f\u00FCr die hinzugef\u00FCgten"
				+ " Dateien." );
	    }
	    byte[] diskBuf = new byte[ diskSize ];
	    if( dstDirPos > 0 ) {
	      Arrays.fill(
			diskBuf,
			0,
			Math.min( dstDirPos, diskBuf.length ),
			(byte) 0 );
	    }
	    if( dirSize > 0 ) {
	      Arrays.fill(
			diskBuf,
			dstDirPos,
			Math.min( dstFilePos, diskBuf.length ),
			(byte) 0xE5 );
	    }
	    if( dstFilePos < diskBuf.length ) {
	      Arrays.fill( diskBuf, dstFilePos, diskBuf.length, (byte) 0x1A );
	    }
	    if( sysFile != null ) {
	      readFile( sysFile, diskBuf, 0 );
	    }
	    boolean blockNum16Bit = false;
	    int     extendSize    = 16 * blockSize;
	    if( getIntValue( this.comboBlockNumSize ) == 16 ) {
	      blockNum16Bit = true;
	      extendSize    = 8 * blockSize;
	    }
	    int blkNum = dirBlocks;
	    int nRows  = this.tableModel.getRowCount();
	    for( int i = 0; i < nRows; i++ ) {
	      FileEntry entry = this.tableModel.getRow( i );
	      if( entry != null ) {
		String entryName = entry.getName();
		if( entryName != null ) {
		  int nBlocks = 0;
		  int nBytes  = readFile(
					entry.getFile(),
					diskBuf,
					dstFilePos );
		  if( nBytes > 0 ) {
		    nBlocks = nBytes / blockSize;
		    if( (nBlocks * blockSize) < nBytes ) {
		      nBlocks++;
		    }
		  }
		  dstFilePos += (nBlocks *  blockSize);

		  int extendNum = 0;
		  do {
		    if( dstDirPos >= begFileArea ) {
		      throw new IOException( "Directory zu klein" );
		    }

		    int entryBegPos = dstDirPos;

		    diskBuf[ dstDirPos ] = (byte) 0;
		    Integer userNum      = entry.getUserNum();
		    if( userNum != null ) {
		      if( (userNum.intValue() > 0)
			  && (userNum.intValue() <= 15) )
		      {
			diskBuf[ dstDirPos ] = userNum.byteValue();
		      }
		    }
		    dstDirPos++;

		    int[] sizes = { 8, 3 };
		    int   len   = entryName.length();
		    int   pos   = 0;
		    for( int k = 0; k < sizes.length; k++ ) {
		      int n = sizes[ k ];
		      while( (pos < len) && (n > 0) ) {
			char ch = entryName.charAt( pos++ );
			if( ch == '.' ) {
			  break;
			}
			diskBuf[ dstDirPos++ ] = (byte) ch;
			--n;
		      }
		      while( (n > 0) ) {
			diskBuf[ dstDirPos++ ] = (byte) '\u0020';
			--n;
		      }
		      if( pos < len ) {
			if( entryName.charAt( pos ) == '.' ) {
			  pos++;
			}
		      }
		    }
		    diskBuf[ dstDirPos++ ] = (byte) (extendNum & 0x1F);
		    diskBuf[ dstDirPos++ ] = (byte) 0;
		    diskBuf[ dstDirPos++ ] = (byte) ((extendNum >> 5) & 0x3F);
		    extendNum++;

		    if( entry.isReadOnly() ) {
		      diskBuf[ entryBegPos + 9 ] |= 0x80;
		    }
		    if( entry.isSystemFile() ) {
		      diskBuf[ entryBegPos + 10 ] |= 0x80;
		    }
		    if( entry.isArchived() ) {
		      diskBuf[ entryBegPos + 11 ] |= 0x80;
		    }
		    int nTmp = nBytes;
		    if( nTmp > extendSize ) {
		      nTmp = extendSize;
		      nBytes -= extendSize;
		    }
		    int entrySizeCode = (nTmp + 127) / 128;
		    if( (entrySizeCode & 0xFF) != entrySizeCode ) {
		      String msg = entry.getName() + ": Datei zu gro\u00DF";
		      if( !blockNum16Bit ) {
			msg = msg + "\nBei 8-Bit-Blocknummern betr\u00E4gt"
				+ " die max. Dateigr\u00F6\u00DFe 32640 Byte.";
		      }
		      throw new IOException( msg );
		    }
		    diskBuf[ dstDirPos++ ] = (byte) entrySizeCode;
		    if( blockNum16Bit ) {
		      for( int k = 0; k < 8; k++ ) {
			if( nBlocks > 0 ) {
			  if( blkNum > 0xFFFF ) {
			    throwBlockNumOverflow();
			  }
			  diskBuf[ dstDirPos++ ] = (byte) blkNum;
			  diskBuf[ dstDirPos++ ] = (byte) (blkNum >> 8);
			  blkNum++;
			  --nBlocks;
			} else {
			  diskBuf[ dstDirPos++ ] = (byte) 0;
			  diskBuf[ dstDirPos++ ] = (byte) 0;
			}
		      }
		    } else {
		      for( int k = 0; k < 16; k++ ) {
			if( nBlocks > 0 ) {
			  if( blkNum > 0xFF ) {
			    throwBlockNumOverflow();
			  }
			  diskBuf[ dstDirPos++ ] = (byte) blkNum;
			  blkNum++;
			  --nBlocks;
			} else {
			  diskBuf[ dstDirPos++ ] = (byte) 0;
			}
		      }
		    }
		  } while( nBlocks > 0 );
		}
	      }
	    }
	    if( plainDisk ) {
	      out = new FileOutputStream( file );
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
						sectorSize ) );
	      if( disk != null ) {
		if( anaDisk ) {
		  AnaDisk.export( disk, file );
		} else if( cpcDisk ) {
		  CPCDisk.export( disk, file );
		} else if( imageDisk ) {
		  ImageDisk.export( disk, file, "Created by JKCEMU" );
		}
	      }
	    }
	    this.dataChanged = false;
	    this.lastOutFile = file;
	    Main.setLastFile( file, "disk" );
	    this.labelStatus.setText( "Diskettenabbilddatei gespeichert" );
	  }
	  catch( Exception ex ) {
	    BasicDlg.showErrorDlg( this, ex );
	  }
	  finally {
	    EmuUtil.doClose( out );
	  }
	} else {
	  BasicDlg.showErrorDlg(
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
    File file = EmuUtil.showFileOpenDlg(
			this,
			"Datei hinzuf\u00FCgen",
			Main.getLastPathFile( "software" ) );
    if( file != null ) {
      if( addFile( file ) ) {
	Main.setLastFile( file, "software" );
      }
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
	updStatusText();
      }
    }
  }


  private void doFileDown()
  {
    int[] rowNums = this.table.getSelectedRows();
    if( rowNums != null ) {
      if( rowNums.length == 1 ) {
	int row = rowNums[ 0 ];
	if( (row >= 0) && (row < (this.tableModel.getRowCount() - 1)) ) {
	  FileEntry e1 = this.tableModel.getRow( row );
	  FileEntry e2 = this.tableModel.getRow( row + 1 );
	  if( (e1 != null) && (e2 != null) ) {
	    this.tableModel.setRow( row, e2 );
	    this.tableModel.setRow( row + 1, e1 );
	    this.tableModel.fireTableDataChanged();
	    fireSelectRow( row + 1 );
	    this.dataChanged = true;
	  }
	}
      }
    }
  }


  private void doFileUp()
  {
    int[] rowNums = this.table.getSelectedRows();
    if( rowNums != null ) {
      if( rowNums.length == 1 ) {
	int row = rowNums[ 0 ];
	if( (row >= 1) && (row < this.tableModel.getRowCount()) ) {
	  FileEntry e1 = this.tableModel.getRow( row - 1);
	  FileEntry e2 = this.tableModel.getRow( row );
	  if( (e1 != null) && (e2 != null) ) {
	    this.tableModel.setRow( row, e1 );
	    this.tableModel.setRow( row - 1, e2 );
	    this.tableModel.fireTableDataChanged();
	    fireSelectRow( row - 1 );
	    this.dataChanged = true;
	  }
	}
      }
    }
  }


  private void doSysFileSelect()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"Datei \u00FCffnen",
			Main.getLastPathFile( "software" ) );
    if( file != null ) {
      addSysFile( file );
    }
  }


  private void doSysFileRemove()
  {
    this.fldSysFileName.setFile( null );
    this.btnSysFileRemove.setEnabled( false );
  }


	/* --- Konstruktor --- */

  private DiskImgCreateFrm()
  {
    this.clipboard   = null;
    this.lastOutFile = null;
    this.dataChanged = false;
    setTitle( "JKCEMU CP/M-Diskettenabbilddatei erstellen" );
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

    this.mnuChangeUser = createJMenuItem( "User-Bereich \u00E4ndern..." );
    mnuEdit.add( this.mnuChangeUser );


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
				FileTableModel.Column.ARCHIVED );

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

    ButtonGroup grpFmt = new ButtonGroup();

    this.btnFmt800K = new JRadioButton(
			"800K (A5105, KC85/1, KC87, Z9001)",
			false );
    grpFmt.add( this.btnFmt800K );
    panelFmt.add( this.btnFmt800K, gbcFmt );

    this.btnFmt780K = new JRadioButton(
			"780K (A5105, AC1 mit ZDOS, HC900, KC85/2..5, Z1013)",
			false );
    grpFmt.add( this.btnFmt780K );
    gbcFmt.insets.top = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.btnFmt780K, gbcFmt );

    this.btnFmt720K = new JRadioButton(
			"720K (A5105, KC85/1, KC87, Z9001)",
			false );
    grpFmt.add( this.btnFmt720K );
    gbcFmt.gridy++;
    panelFmt.add( this.btnFmt720K, gbcFmt );

    this.btnFmt624K = new JRadioButton( "624K (PC/M)", false );
    grpFmt.add( this.btnFmt624K );
    gbcFmt.gridy++;
    panelFmt.add( this.btnFmt624K, gbcFmt );

    this.btnFmtCPL = new JRadioButton( "LLC2 mit CP/L", false );
    grpFmt.add( this.btnFmtCPL );
    gbcFmt.gridy++;
    panelFmt.add( this.btnFmtCPL, gbcFmt );

    this.btnFmtEtc = new JRadioButton( "Sonstiges Format:", true );
    grpFmt.add( this.btnFmtEtc );
    gbcFmt.gridy++;
    panelFmt.add( this.btnFmtEtc, gbcFmt );

    this.labelSides = new JLabel( "Seiten:" );
    gbcFmt.insets.left = 50;
    gbcFmt.gridwidth   = 1;
    gbcFmt.gridy++;
    panelFmt.add( this.labelSides, gbcFmt );

    this.comboSides    = createJComboBox( 1, 2 );
    gbcFmt.insets.left = 5;
    gbcFmt.gridx++;
    panelFmt.add( this.comboSides, gbcFmt );

    this.labelCyls     = new JLabel( "Spuren:" );
    gbcFmt.insets.left = 50;
    gbcFmt.gridx       = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelCyls, gbcFmt );

    this.comboCyls     = createJComboBox( 40, 80 );
    gbcFmt.insets.left = 5;
    gbcFmt.gridx++;
    panelFmt.add( this.comboCyls, gbcFmt );

    this.labelSysCyls  = new JLabel( "davon Systemspuren:" );
    gbcFmt.gridx += 2;
    panelFmt.add( this.labelSysCyls, gbcFmt );

    this.spinnerSysCyls = new JSpinner( new SpinnerNumberModel( 0, 0, 9, 1 ) );
    gbcFmt.insets.left  = 5;
    gbcFmt.gridx++;
    panelFmt.add( this.spinnerSysCyls, gbcFmt );

    this.labelSectPerCyl = new JLabel( "Sektoren pro Spur:" );
    gbcFmt.insets.left   = 50;
    gbcFmt.gridx         = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelSectPerCyl, gbcFmt );

    this.comboSectPerCyl = createJComboBox( 5, 8, 9, 15, 16, 18, 36 );
    gbcFmt.insets.left   = 5;
    gbcFmt.gridx++;
    panelFmt.add( this.comboSectPerCyl, gbcFmt );

    this.labelSectorSize = new JLabel( "Sektorgr\u00F6\u00DFe:" );
    gbcFmt.insets.left   = 50;
    gbcFmt.gridx         = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelSectorSize, gbcFmt );

    this.comboSectorSize = createJComboBox( 256, 512, 1024 );
    gbcFmt.insets.left  = 5;
    gbcFmt.gridx++;
    panelFmt.add( this.comboSectorSize, gbcFmt );

    this.labelSectorSizeUnit = new JLabel( "Byte" );
    gbcFmt.gridx++;
    panelFmt.add( this.labelSectorSizeUnit, gbcFmt );

    this.labelBlockSize = new JLabel( "Blockgr\u00F6\u00DFe:" );
    gbcFmt.insets.left  = 50;
    gbcFmt.gridx        = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelBlockSize, gbcFmt );

    this.comboBlockSize = createJComboBox( 1, 2, 4, 8, 16 );
    gbcFmt.insets.left  = 5;
    gbcFmt.gridx++;
    panelFmt.add( this.comboBlockSize, gbcFmt );

    this.labelBlockSizeUnit = new JLabel( "KByte" );
    gbcFmt.gridx++;
    panelFmt.add( this.labelBlockSizeUnit, gbcFmt );

    this.labelBlockNumSize = new JLabel( "Blocknummern:" );
    gbcFmt.anchor = GridBagConstraints.EAST;
    gbcFmt.gridx++;
    panelFmt.add( this.labelBlockNumSize, gbcFmt );

    this.comboBlockNumSize = createJComboBox( 8, 16 );
    gbcFmt.anchor        = GridBagConstraints.WEST;
    gbcFmt.insets.left   = 5;
    gbcFmt.insets.bottom = 5;
    gbcFmt.gridx++;
    panelFmt.add( this.comboBlockNumSize, gbcFmt );

    this.labelBlockNumSizeUnit = new JLabel( "Bit" );
    gbcFmt.gridx++;
    panelFmt.add( this.labelBlockNumSizeUnit, gbcFmt );

    this.labelDirBlocks = new JLabel( "Directory:" );
    gbcFmt.insets.left  = 50;
    gbcFmt.gridx        = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelDirBlocks, gbcFmt );

    this.spinnerDirBlocks = new JSpinner(
			new SpinnerNumberModel( 2, 1, 9, 1 ) );
    gbcFmt.insets.left  = 5;
    gbcFmt.gridx++;
    panelFmt.add( this.spinnerDirBlocks, gbcFmt );

    this.labelDirEntriesInfo = new JLabel( "Bl\u00F6cke" );
    gbcFmt.gridwidth         = GridBagConstraints.REMAINDER;
    gbcFmt.gridx++;
    panelFmt.add( this.labelDirEntriesInfo, gbcFmt );

    this.labelSysFile  = new JLabel( "Datei f\u00FCr Systemspuren:" );
    gbcFmt.insets.top  = 5;
    gbcFmt.insets.left = 5;
    gbcFmt.gridwidth   = 1;
    gbcFmt.gridx       = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelSysFile, gbcFmt );

    this.fldSysFileName = new FileNameFld();
    this.fldSysFileName.setEditable( false );
    gbcFmt.fill      = GridBagConstraints.HORIZONTAL;
    gbcFmt.weightx   = 1.0;
    gbcFmt.gridwidth = 5;
    gbcFmt.gridx++;
    panelFmt.add( this.fldSysFileName, gbcFmt );

    this.btnSysFileSelect = createImageButton(
				"/images/file/open.png",
				"\u00D6ffnen" );
    gbcFmt.fill      = GridBagConstraints.NONE;
    gbcFmt.weightx   = 0.0;
    gbcFmt.gridwidth = 1;
    gbcFmt.gridx += 5;
    panelFmt.add( this.btnSysFileSelect, gbcFmt );

    this.btnSysFileRemove = createImageButton(
				"/images/file/delete.png",
				"\u00D6ffnen" );
    this.btnSysFileRemove.setEnabled( false );
    gbcFmt.gridx++;
    panelFmt.add( this.btnSysFileRemove, gbcFmt );

    updFmtDetailsFields();
    updDirEntriesInfo();


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
    this.btnFmt800K.addActionListener( this );
    this.btnFmt780K.addActionListener( this );
    this.btnFmt720K.addActionListener( this );
    this.btnFmt624K.addActionListener( this );
    this.btnFmtCPL.addActionListener( this );
    this.btnFmtEtc.addActionListener( this );
    this.comboSides.addActionListener( this );
    this.comboCyls.addActionListener( this );
    this.spinnerSysCyls.addChangeListener( this );
    this.comboSectPerCyl.addActionListener( this );
    this.comboSectorSize.addActionListener( this );
    this.comboBlockSize.addActionListener( this );
    this.spinnerDirBlocks.addChangeListener( this );
    if( this.clipboard != null ) {
      this.clipboard.addFlavorListener( this );
    }


    // Drop-Ziele
    this.dropTargetFile1   = new DropTarget( this.table, this );
    this.dropTargetFile2   = new DropTarget( this.scrollPane, this );
    this.dropTargetSysFile = new DropTarget( this.fldSysFileName, this );

    this.dropTargetFile1.setActive( true );
    this.dropTargetFile1.setActive( true );
    this.dropTargetSysFile.setActive( true );


    // sonstiges
    updActionButtons();
    updBgColor();
    updPasteButton();
  }


	/* --- private Methoden --- */

  private boolean addFile( File file )
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
	if( !BasicDlg.showYesNoDlg(
		this,
		"Datei " + fName
			+ ":\nDie Datei ist leer!\n"
			+ "Trotzdem hinzuf\u00FCgen?" ) )
	{
	  file = null;
	}
      }
      else if( size > (1024 * 16 * 32) ) {
	BasicDlg.showErrorDlg( this, "Datei ist zu gro\u00DF!" );
	file = null;
      } else {
	String fileName = file.getName();
	if( fileName != null ) {
	  if( fileName.equalsIgnoreCase(
			DirectoryFloppyDisk.SYS_FILE_NAME ) )
	  {
	    if( BasicDlg.showYesNoDlg(
			this,
			"Datei " + DirectoryFloppyDisk.SYS_FILE_NAME + ":\n"
				+ "JKCEMU verwendet Dateien mit diesem Namen"
				+ " f\u00FCr den Inhalt der Systemspuren.\n"
				+ "M\u00F6chten Sie deshalb nun diese Datei"
				+ " f\u00FCr die Systemspuren verwenden,\n"
				+ "anstelle Sie als gew\u00F6hnliche Datei"
				+ " im Directory einzubinden?" ) )
	    {
	      this.fldSysFileName.setFile( file );
	      this.btnSysFileRemove.setEnabled( true );
	      file = null;
	      rv   = true;
	    }
	  }
	}
      }
    } else {
      BasicDlg.showErrorDlg(
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
	      BasicDlg.showErrorDlg(
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
	  BasicDlg.showErrorDlg(
		this,
		"Es existiert bereits ein Eintrag mit diesem Namen." );
	} else {
	  FileEntry entry = new FileEntry( entryName );
	  entry.setUserNum( new Integer( 0 ) );
	  if( size >= 0 ) {
	    entry.setSize( new Long( size ) );
	  }
	  long lastModified = file.lastModified();
	  if( lastModified != 0 ) {
	    entry.setLastModified( new Long( lastModified ) );
	  }
	  entry.setFile( file );
	  entry.setReadOnly( !file.canWrite() );
	  entry.setSystemFile( false );
	  entry.setArchived( false );
	  this.tableModel.addRow( entry, true );
	  nRows = this.tableModel.getRowCount();
	  fireSelectRow( nRows - 1 );
	  updStatusText();
	  this.mnuSort.setEnabled( nRows > 1 );
	  this.dataChanged = true;
	  rv               = true;
	}
      }
    }
    return rv;
  }


  private void addSysFile( File file )
  {
    String errMsg = null;
    if( !file.exists() ) {
      errMsg = "Datei nicht gefunden";
    }
    else if( !file.isFile() ) {
      errMsg = "Datei ist keine regul\u00E4re Datei";
    }
    if( errMsg != null ) {
      BasicDlg.showErrorDlg( this, errMsg );
    } else {
      this.fldSysFileName.setFile( file );
      this.btnSysFileRemove.setEnabled( true );
    }
  }


  private int calcFileAreaKBytes( int blockSize )
  {
    int kbytes = 0;
    int nRows  = this.tableModel.getRowCount();
    for( int i = 0; i < nRows; i++ ) {
      FileEntry entry = this.tableModel.getRow( i );
      if( entry != null ) {
	Long size = entry.getSize();
	if( size != null ) {
	  if( size.longValue() > 0 ) {
	    int nBlocks = (int) (size.longValue() / blockSize);
	    if( ((long) nBlocks * (long) blockSize) < size.longValue() ) {
	      nBlocks++;
	    }
	    kbytes += (nBlocks * blockSize / 1024);
	  }
	}
      }
    }
    return kbytes;
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


  private static JComboBox createJComboBox( int... items )
  {
    JComboBox combo = new JComboBox();
    combo.setEditable( false );
    if( items != null ) {
      for( int i = 0; i < items.length; i++ ) {
	combo.addItem( new Integer( items[ i ] ) );
      }
    }
    return combo;
  }


  private void fireSelectRow( final int row )
  {
    final JTable table = this.table;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    if( (row >= 0) && (row < table.getRowCount()) ) {
		      table.setRowSelectionInterval( row, row );
		    }
		  }
		} );
  }


  private int getIntValue( JComboBox combo )
  {
    int rv = 0;
    if( combo != null ) {
      Object o = combo.getSelectedItem();
      if( o != null ) {
	if( o instanceof Number ) {
	  rv = ((Number) o).intValue();
	}
      }
    }
    return rv;
  }


  private int getIntValue( JSpinner spinner )
  {
    int rv = 0;
    if( spinner != null ) {
      Object o = spinner.getValue();
      if( o != null ) {
	if( o instanceof Number ) {
	  rv = ((Number) o).intValue();
	}
      }
    }
    return rv;
  }


  private void pasteFiles( Transferable t )
  {
    try {
      if( t != null ) {
	Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
	if( o != null ) {
	  if( o instanceof Collection ) {
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
	  }
	}
      }
    }
    catch( IOException ex ) {}
    catch( UnsupportedFlavorException ex ) {}
  }


  private int readFile( File file, byte[] buf, int pos ) throws IOException
  {
    int rv = 0;
    if( file != null ) {
      InputStream in = null;
      try {
	in = new FileInputStream( file );

	int n = 0;
	do {
	  rv  += n;
	  pos += n;

	  int len = buf.length - pos;
	  if( len > 0 ) {
	    n = in.read( buf, pos, len );
	  } else {
	    break;
	  }
	} while( n > 0 );
	if( pos >= buf.length ) {
	  if( in.read() == -1 ) {
	    throw new IOException(
		"Die Diskettenabbilddatei bietet nicht gen\u00FCgend Platz!" );
	  }
	}
      }
      finally {
	EmuUtil.doClose( in );
      }
    }
    return rv;
  }


  private static void setValue( JComboBox combo, int value )
  {
    if( combo != null )
      combo.setSelectedItem( new Integer( value ) );
  }


  private static void setValue( JSpinner spinner, int value )
  {
    if( spinner != null ) {
      try {
	spinner.setValue( new Integer( value ) );
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  private static void throwBlockNumOverflow() throws IOException
  {
    throw new IOException( "Blocknummern&uuml;berlauf:\n"
	+ "Die Anzahl der m&ouml;glichen Blocknummern reicht nicht aus." );
  }


  private void updActionButtons()
  {
    int     rowNum  = -1;
    int     rowCnt  = 0;
    int     nSel    = 0;
    boolean fileTab = (this.tabbedPane.getSelectedIndex() == 0);
    if( fileTab ) {
      nSel = this.table.getSelectedRowCount();
      if( nSel == 1 ) {
	rowNum = this.table.getSelectedRow();
	rowCnt = this.table.getRowCount();
      }
    }
    this.btnFileAdd.setEnabled( fileTab );
    this.mnuFileRemove.setEnabled( nSel > 0 );
    this.btnFileRemove.setEnabled( nSel > 0 );
    this.btnFileDown.setEnabled( (rowNum >= 0) && (rowNum < (rowCnt - 1)) );
    this.btnFileUp.setEnabled( (rowNum >= 1) && (rowNum < rowCnt) );
    this.mnuChangeUser.setEnabled( nSel > 0 );
  }


  private void updBgColor()
  {
    Color     color = this.table.getBackground();
    JViewport vp    = this.scrollPane.getViewport();
    if( (color != null) && (vp != null) )
      vp.setBackground( color );
  }


  private void updDirEntriesInfo()
  {
    if( this.labelDirEntriesInfo != null ) {
      this.labelDirEntriesInfo.setText(
		String.format(
			"Bl\u00F6cke (%d Eintr\u00E4ge)",
			getIntValue( this.comboBlockSize ) * 1024
				* getIntValue( this.spinnerDirBlocks )
				/ 32 ) );
    }
  }


  private void updFmtDetailsFields()
  {
    boolean state = false;
    if( this.btnFmt800K.isSelected() ) {
      setValue( this.comboSides, 2 );
      setValue( this.comboCyls, 80 );
      setValue( this.spinnerSysCyls, 0 );
      setValue( this.comboSectPerCyl, 5 );
      setValue( this.comboSectorSize, 1024 );
      setValue( this.comboBlockSize, 2 );
      setValue( this.comboBlockNumSize, 16 );
      setValue( this.spinnerDirBlocks, 3 );
    } else if( this.btnFmt780K.isSelected() ) {
      setValue( this.comboSides, 2 );
      setValue( this.comboCyls, 80 );
      setValue( this.spinnerSysCyls, 2 );
      setValue( this.comboSectPerCyl, 5 );
      setValue( this.comboSectorSize, 1024 );
      setValue( this.comboBlockSize, 2 );
      setValue( this.comboBlockNumSize, 16 );
      setValue( this.spinnerDirBlocks, 2 );
    } else if( this.btnFmt720K.isSelected() ) {
      setValue( this.comboSides, 2 );
      setValue( this.comboCyls, 80 );
      setValue( this.spinnerSysCyls, 0 );
      setValue( this.comboSectPerCyl, 9 );
      setValue( this.comboSectorSize, 512 );
      setValue( this.comboBlockSize, 2 );
      setValue( this.comboBlockNumSize, 16 );
      setValue( this.spinnerDirBlocks, 3 );
    } else if( this.btnFmt624K.isSelected() ) {
      setValue( this.comboSides, 2 );
      setValue( this.comboCyls, 80 );
      setValue( this.spinnerSysCyls, 2 );
      setValue( this.comboSectPerCyl, 16 );
      setValue( this.comboSectorSize, 256 );
      setValue( this.comboBlockSize, 2 );
      setValue( this.comboBlockNumSize, 16 );
      setValue( this.spinnerDirBlocks, 2 );
    } else if( this.btnFmtCPL.isSelected() ) {
      setValue( this.comboSides, 1 );
      setValue( this.comboCyls, 80 );
      setValue( this.spinnerSysCyls, 0 );
      setValue( this.comboSectPerCyl, 5 );
      setValue( this.comboSectorSize, 1024 );
      setValue( this.comboBlockSize, 2 );
      setValue( this.comboBlockNumSize, 8 );
      setValue( this.spinnerDirBlocks, 1 );
    } else if( this.btnFmtEtc.isSelected() ) {
      state = true;
    }
    this.labelSides.setEnabled( state );
    this.comboSides.setEnabled( state );
    this.labelCyls.setEnabled( state );
    this.comboCyls.setEnabled( state );
    this.labelSysCyls.setEnabled( state );
    this.spinnerSysCyls.setEnabled( state );
    this.labelSectPerCyl.setEnabled( state );
    this.comboSectPerCyl.setEnabled( state );
    this.labelSectorSize.setEnabled( state );
    this.comboSectorSize.setEnabled( state );
    this.labelSectorSizeUnit.setEnabled( state );
    this.labelBlockSize.setEnabled( state );
    this.comboBlockSize.setEnabled( state );
    this.labelBlockSizeUnit.setEnabled( state );
    this.labelBlockNumSize.setEnabled( state );
    this.comboBlockNumSize.setEnabled( state );
    this.labelBlockNumSize.setEnabled( state );
    this.labelBlockNumSizeUnit.setEnabled( state );
    this.labelDirBlocks.setEnabled( state );
    this.labelDirEntriesInfo.setEnabled( state );
    this.spinnerDirBlocks.setEnabled( state );
    this.labelDirEntriesInfo.setEnabled( state );
    updSysFileFieldsEnabled();
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
      int sides      = getIntValue( this.comboSides );
      int cyls       = getIntValue( this.comboCyls );
      int sysCyls    = getIntValue( this.spinnerSysCyls );
      int sectPerCyl = getIntValue( this.comboSectPerCyl );
      int sectorSize = getIntValue( this.comboSectorSize );
      int kbytes     = sides * cyls * sectPerCyl * sectorSize / 1024;
      if( sysCyls > 0 ) {
	text = String.format(
			"%d/%dK Diskettenformat eingestellt",
			sides * (cyls - sysCyls) * sectPerCyl * sectorSize
								/ 1024,
			kbytes );

      } else {
	text = String.format( "%dK Diskettenformat eingestellt", kbytes );
      }
    }
    this.labelStatus.setText( text );
  }


  private void updSysFileFieldsEnabled()
  {
    boolean state = (getIntValue( this.spinnerSysCyls ) > 0);
    this.labelSysFile.setEnabled( state );
    this.fldSysFileName.setEnabled( state );
    this.btnSysFileSelect.setEnabled( state );
    this.btnSysFileRemove.setEnabled(
		state && (this.fldSysFileName.getFile() != null) );
  }
}

