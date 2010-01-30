/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zum Erzeugen einer einfachen Abbilddatei
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


public class DiskImgCreateFrm
			extends BasicFrm
			implements
				ChangeListener,
				DropTargetListener,
				ListSelectionListener
{
  private ScreenFrm         screenFrm;
  private File              lastOutFile;
  private File              sysFile;
  private boolean           dataChanged;
  private JMenuItem         mnuClose;
  private JMenuItem         mnuFileAdd;
  private JMenuItem         mnuFileRemove;
  private JMenuItem         mnuSort;
  private JMenuItem         mnuSave;
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
  private JRadioButton      btnFmtKC85;
  private JRadioButton      btnFmtLLC2;
  private JRadioButton      btnFmtZ9001;
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
  private JTextField        fldSysFileName;
  private JLabel            labelStatus;
  private DropTarget        dropTargetFile1;
  private DropTarget        dropTargetFile2;
  private DropTarget        dropTargetSysFile;


  public DiskImgCreateFrm( ScreenFrm screenFrm )
  {
    this.screenFrm   = screenFrm;
    this.lastOutFile = null;
    this.sysFile     = null;
    this.dataChanged = false;
    setTitle( "JKCEMU CP/M-Diskettenabbilddatei erstellen" );
    Main.updIcon( this );


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

    this.mnuSave = createJMenuItem( "Abbilddatei speichern..." );
    mnuFile.add( this.mnuSave );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


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
				"Abbilddatei speichern" );
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

    EmuUtil.setTableColWidths( this.table, 100, 280, 70, 130, 40, 40, 40 );

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

    this.btnFmtKC85 = new JRadioButton(
			"MicroDOS, ML-DOS f\u00FCr HC900 und KC85/2..5",
			false );
    grpFmt.add( this.btnFmtKC85 );
    panelFmt.add( this.btnFmtKC85, gbcFmt );

    this.btnFmtZ9001 = new JRadioButton(
			"CP/A f\u00FCr KC85/1, KC87 und Z9001",
			false );
    grpFmt.add( this.btnFmtZ9001 );
    gbcFmt.insets.top = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.btnFmtZ9001, gbcFmt );

    this.btnFmtLLC2 = new JRadioButton( "CP/L f\u00FCr LLC2", false );
    grpFmt.add( this.btnFmtLLC2 );
    gbcFmt.gridy++;
    panelFmt.add( this.btnFmtLLC2, gbcFmt );

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

    this.fldSysFileName = new JTextField();
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
    this.btnFmtKC85.addActionListener( this );
    this.btnFmtLLC2.addActionListener( this );
    this.btnFmtZ9001.addActionListener( this );
    this.btnFmtEtc.addActionListener( this );
    this.comboSides.addActionListener( this );
    this.comboCyls.addActionListener( this );
    this.spinnerSysCyls.addChangeListener( this );
    this.comboSectPerCyl.addActionListener( this );
    this.comboSectorSize.addActionListener( this );
    this.comboBlockSize.addActionListener( this );
    this.spinnerDirBlocks.addChangeListener( this );


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
  }


	/* --- ChangeListener --- */

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

  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  public void drop( DropTargetDropEvent e )
  {
    try {
      Object src = e.getSource();
      if( (src == this.dropTargetFile1) || (src == this.dropTargetFile2) ) {
	if( EmuUtil.isFileDrop( e ) ) {
	  e.acceptDrop( DnDConstants.ACTION_COPY );  // Quelle nicht loeschen
	  Transferable t = e.getTransferable();
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
      } else if( src == this.dropTargetSysFile ) {
	File file = EmuUtil.fileDrop( this, e );
	if( file != null ) {
	  addSysFile( file );
	}
      }
    }
    catch( Exception ex ) {}
  }


  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- ListSelectionListener --- */

  public void valueChanged( ListSelectionEvent e )
  {
    updActionButtons();
  }


	/* --- ueberschriebene Methoden --- */

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
      else if( src == this.btnSysFileRemove ) {
	rv = true;
	doSysFileRemove();
      }
      else if( src == this.mnuHelpContent ) {
	rv = true;
	this.screenFrm.showHelp( "/help/disk/create_img_by_hand.htm" );
      }
      else if( (src == this.btnFmtKC85)
	       || (src == this.btnFmtLLC2)
	       || (src == this.btnFmtZ9001)
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
      rv = super.doClose();
    }
    if( rv ) {
      this.dataChanged = false;
      this.screenFrm.childFrameClosed( this );
    }
    return rv;
  }


	/* --- Aktionen --- */

  private void doSave()
  {
    boolean status = true;
    if( this.tableModel.getRowCount() == 0 ) {
      status = BasicDlg.showYesNoDlg(
		this,
		"Es wurden keine Dateien hinzugef\u00FCgt.\n"
			+ "M\u00F6chten Sie eine leere Abbilddatei"
			+ " erstellen?" );
    }
    if( status ) {
      File file = EmuUtil.showFileSaveDlg(
				this,
				"Abbilddatei speichern",
				this.lastOutFile != null ?
					this.lastOutFile
					: Main.getLastPathFile( "disk" ),
				EmuUtil.getPlainDiskFileFilter(),
				EmuUtil.getAnadiskFileFilter() );
      if( file != null ) {
	boolean anadisk   = false;
	boolean plainDisk = false;
	String  fileName  = file.getName();
	if( fileName != null ) {
	  String lowerName = fileName.toLowerCase();
	  anadisk =  EmuUtil.endsWith(
				lowerName,
				DiskUtil.anadiskFileExt )
			|| EmuUtil.endsWith(
				lowerName,
				DiskUtil.gzAnadiskFileExt );
	  plainDisk =  EmuUtil.endsWith(
				lowerName,
				DiskUtil.plainDiskFileExt )
			|| EmuUtil.endsWith(
				lowerName,
				DiskUtil.gzAnadiskFileExt );
	}
	if( anadisk || plainDisk ) {
	  OutputStream out = null;
	  try {
	    int sides       = getIntValue( this.comboSides );
	    int cyls        = getIntValue( this.comboCyls );
	    int sysCyls     = getIntValue( this.spinnerSysCyls );
	    int sectPerCyl  = getIntValue( this.comboSectPerCyl );
	    int sectorSize  = getIntValue( this.comboSectorSize );
	    int blockSize   = getIntValue( this.comboBlockSize ) * 1024;
	    int dirBlocks   = getIntValue( this.spinnerDirBlocks );
	    int diskSize    = sides * cyls * sectPerCyl * sectorSize;
	    int dstDirPos   = sysCyls * sides * sectPerCyl * sectorSize;
	    int dirSize     = dirBlocks * blockSize;
	    int begFileArea = dstDirPos + dirSize;
	    int dstFilePos  = begFileArea;
	    if( this.sysFile != null ) {
	      if( this.sysFile.length() > dstDirPos ) {
		  throw new IOException(
			"Die Datei f\u00FCr die Systemspuren"
				+ " ist zu gro\u00DF\n"
				+ "bzw. die Anzahl der Systemspuren"
				+ " ist zu klein!" );
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
	      Arrays.fill( diskBuf, dstFilePos, diskBuf.length, (byte) 0 );
	    }
	    if( this.sysFile != null ) {
	      readFile( this.sysFile, diskBuf, 0 );
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

		    int entryBegPos        = dstDirPos;
		    diskBuf[ dstDirPos++ ] = (byte) 0;

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
	    out = new FileOutputStream( file );
	    if( EmuUtil.isGZipFile( file ) ) {
	      out = new GZIPOutputStream( out );
	    }
	    if( anadisk ) {
	      int sizeCode = SectorData.getSizeCode( sectorSize );
	      int cyl      = 0;
	      int head     = 0;
	      int rec      = 1;
	      int pos      = 0;
	      while( pos < diskBuf.length ) {
		out.write( cyl );
		out.write( head );
		out.write( cyl );
		out.write( head );
		out.write( rec );
		out.write( sizeCode );
		out.write( sectorSize & 0xFF );
		out.write( sectorSize >> 8 );
		out.write(
			diskBuf,
			pos,
			Math.min( sectorSize, diskBuf.length - pos ) );
		pos += sectorSize;
		if( rec < sectPerCyl ) {
		  rec++;
		} else {
		  rec = 1;
		  head++;
		  if( head >= sides ) {
		    head = 0;
		    cyl++;
		  }
		}
	      }
	    } else {
	      out.write( diskBuf );
	    }
	    out.close();
	    out              = null;
	    this.dataChanged = false;
	    if( anadisk ) {
	      this.labelStatus.setText( "Anadisk-Datei gespeichert" );
	    } else {
	      this.labelStatus.setText( "Einfache Abbilddatei gespeichert" );
	    }
	    this.lastOutFile = file;
	    Main.setLastFile( file, "disk" );
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
		"Aus der Dateiendung der ausgew\u00E4hlten Datei"
			+ " kann JKCEMU nicht erkennen,\n"
			+ "ob Sie eine einfache Abbilddatei oder eine"
			+ " Anadisk-Datei erzeugen m\u00F6chten.\n"
			+ "W\u00E4hlen Sie bitte einen Dateinamen"
			+ " mit einer f\u00FCr das gew\u00FCnschte Format\n"
			+ "\u00FCblichen Dateiendung." );
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
    this.sysFile = null;
    this.fldSysFileName.setText( "" );
    this.btnSysFileRemove.setEnabled( false );
  }


	/* --- private Methoden --- */

  private boolean addFile( File file )
  {
    boolean rv   = false;
    long    size = -1L;
    if( file.isFile() ) {
      size = file.length();
      if( size < 1 ) {
	if( !BasicDlg.showYesNoDlg(
		this,
		"Die Datei ist leer!\n"
			+ "Trotzdem hinzuf\u00FCgen?" ) )
	{
	  file = null;
	}
      }
      else if( size > (1024 * 16 * 32) ) {
	BasicDlg.showErrorDlg( this, "Datei ist zu gro\u00DF!" );
	file = null;
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
      this.sysFile = file;
      this.fldSysFileName.setText( file.getPath() );
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
    SwingUtilities.invokeLater(
		new Runnable()
		{
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
		"Die Abbilddatei bietet nicht gen\u00FCgend Platz!" );
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
    if( this.btnFmtKC85.isSelected() ) {
      setValue( this.comboSides, 2 );
      setValue( this.comboCyls, 80 );
      setValue( this.spinnerSysCyls, 2 );
      setValue( this.comboSectPerCyl, 5 );
      setValue( this.comboSectorSize, 1024 );
      setValue( this.comboBlockSize, 2 );
      setValue( this.comboBlockNumSize, 16 );
      setValue( this.spinnerDirBlocks, 2 );
    } else if( this.btnFmtLLC2.isSelected() ) {
      setValue( this.comboSides, 1 );
      setValue( this.comboCyls, 80 );
      setValue( this.spinnerSysCyls, 0 );
      setValue( this.comboSectPerCyl, 5 );
      setValue( this.comboSectorSize, 1024 );
      setValue( this.comboBlockSize, 2 );
      setValue( this.comboBlockNumSize, 8 );
      setValue( this.spinnerDirBlocks, 1 );
    } else if( this.btnFmtZ9001.isSelected() ) {
      setValue( this.comboSides, 2 );
      setValue( this.comboCyls, 80 );
      setValue( this.spinnerSysCyls, 0 );
      setValue( this.comboSectPerCyl, 5 );
      setValue( this.comboSectorSize, 1024 );
      setValue( this.comboBlockSize, 2 );
      setValue( this.comboBlockNumSize, 16 );
      setValue( this.spinnerDirBlocks, 3 );
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
    this.btnSysFileRemove.setEnabled( state && (this.sysFile != null) );
  }
}
