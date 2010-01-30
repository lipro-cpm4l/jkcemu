/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Datei-Browser
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.zip.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import javax.swing.tree.*;
import jkcemu.Main;
import jkcemu.audio.*;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.text.EditFrm;


public class FileBrowserFrm extends BasicFrm
					implements
						DragGestureListener,
						FocusListener,
						ListSelectionListener,
						TreeSelectionListener,
						TreeWillExpandListener
{
  private ScreenFrm            screenFrm;
  private JMenuItem            mnuFileLoadIntoEmuOpt;
  private JMenuItem            mnuFileLoadIntoEmu;
  private JMenuItem            mnuFileStartInEmu;
  private JMenuItem            mnuFileEditText;
  private JMenuItem            mnuFileEditHex;
  private JMenuItem            mnuFileDiffHex;
  private JMenuItem            mnuFileExportToPlainDisk;
  private JMenuItem            mnuFileExportToAnadisk;
  private JMenuItem            mnuFileExportToSound;
  private JMenuItem            mnuFileUnpack;
  private JMenuItem            mnuFilePackGZip;
  private JMenuItem            mnuFilePackZip;
  private JMenuItem            mnuFilePackTar;
  private JMenuItem            mnuFilePackTgz;
  private JMenuItem            mnuFilePlay;
  private JMenuItem            mnuFileShowImage;
  private JMenuItem            mnuFileRAMFloppy1Load;
  private JMenuItem            mnuFileRAMFloppy2Load;
  private JMenuItem            mnuFileChecksum;
  private JMenuItem            mnuFileCreateDir;
  private JMenuItem            mnuFileRename;
  private JMenuItem            mnuFileDelete;
  private JMenuItem            mnuFileLastModified;
  private JMenuItem            mnuFileProp;
  private JMenuItem            mnuFileRefresh;
  private JMenuItem            mnuFileClose;
  private JCheckBoxMenuItem    mnuHiddenFiles;
  private JCheckBoxMenuItem    mnuSortCaseSensitive;
  private JCheckBoxMenuItem    mnuLoadOnDoubleClick;
  private JRadioButtonMenuItem mnuNoPreview;
  private JRadioButtonMenuItem mnuPreviewMaxFileSize100K;
  private JRadioButtonMenuItem mnuPreviewMaxFileSize1M;
  private JRadioButtonMenuItem mnuPreviewMaxFileSize10M;
  private JRadioButtonMenuItem mnuPreviewMaxFileSize100M;
  private JRadioButtonMenuItem mnuPreviewNoFileSizeLimit;
  private JMenuItem            mnuHelpContent;
  private JPopupMenu           mnuPopup;
  private JMenuItem            mnuPopupLoadIntoEmuOpt;
  private JMenuItem            mnuPopupLoadIntoEmu;
  private JMenuItem            mnuPopupStartInEmu;
  private JMenuItem            mnuPopupEditText;
  private JMenuItem            mnuPopupEditHex;
  private JMenuItem            mnuPopupDiffHex;
  private JMenuItem            mnuPopupPlay;
  private JMenuItem            mnuPopupExportToPlainDisk;
  private JMenuItem            mnuPopupExportToAnadisk;
  private JMenuItem            mnuPopupExportToSound;
  private JMenuItem            mnuPopupUnpack;
  private JMenuItem            mnuPopupPackGZip;
  private JMenuItem            mnuPopupPackZip;
  private JMenuItem            mnuPopupPackTar;
  private JMenuItem            mnuPopupPackTgz;
  private JMenuItem            mnuPopupShowImage;
  private JMenuItem            mnuPopupRAMFloppy1Load;
  private JMenuItem            mnuPopupRAMFloppy2Load;
  private JMenuItem            mnuPopupChecksum;
  private JMenuItem            mnuPopupCreateDir;
  private JMenuItem            mnuPopupRename;
  private JMenuItem            mnuPopupDelete;
  private JMenuItem            mnuPopupLastModified;
  private JMenuItem            mnuPopupProp;
  private JButton              btnLoadIntoEmu;
  private JButton              btnStartInEmu;
  private JButton              btnEditText;
  private JButton              btnShowImage;
  private JButton              btnPlay;
  private JSplitPane           splitPane;
  private JTable               table;
  private JTree                tree;
  private DefaultTreeModel     treeModel;
  private FileNode             rootNode;
  private FilePreviewFld       filePreviewFld;
  private Component            lastActiveFld;


  public FileBrowserFrm( ScreenFrm screenFrm )
  {
    this.screenFrm     = screenFrm;
    this.lastActiveFld = null;
    setTitle( "JKCEMU Datei-Browser" );
    Main.updIcon( this );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuFileLoadIntoEmuOpt = createJMenuItem(
		"In Emulator laden mit...",
		KeyStroke.getKeyStroke( KeyEvent.VK_L, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFileLoadIntoEmuOpt );

    this.mnuFileLoadIntoEmu = createJMenuItem(
		"In Emulator laden",
		KeyStroke.getKeyStroke(
				KeyEvent.VK_L,
				Event.CTRL_MASK | Event.SHIFT_MASK) );
    mnuFile.add( this.mnuFileLoadIntoEmu );

    this.mnuFileStartInEmu = createJMenuItem(
		"Im Emulator starten",
		KeyStroke.getKeyStroke( KeyEvent.VK_R, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFileStartInEmu );

    this.mnuFileEditText = createJMenuItem(
		"Im Texteditor \u00F6ffnen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_E, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFileEditText );

    this.mnuFileShowImage = createJMenuItem(
		"Im Bildbetrachter anzeigen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_B, Event.CTRL_MASK ) );
    mnuFile.add( this.mnuFileShowImage );

    this.mnuFileEditHex = createJMenuItem( "Im Hex-Editor \u00F6ffnen..." );
    mnuFile.add( this.mnuFileEditHex );

    this.mnuFileDiffHex = createJMenuItem(
			"Zum Hex-Dateivergleicher hinzuf\u00FCgen..." );
    mnuFile.add( this.mnuFileDiffHex );

    this.mnuFileChecksum = createJMenuItem(
				"Pr\u00FCfsumme/Hash-Wert berechnen..." );
    mnuFile.add( this.mnuFileChecksum );

    this.mnuFilePlay = createJMenuItem( "Wiedergeben" );
    mnuFile.add( this.mnuFilePlay );

    JMenu mnuExport = new JMenu( "Exportieren in" );
    mnuFile.add( mnuExport );

    this.mnuFileExportToPlainDisk = createJMenuItem(
				"einfache Diskettenabbilddatei..." );
    mnuExport.add( this.mnuFileExportToPlainDisk );

    this.mnuFileExportToAnadisk = createJMenuItem( "Anadisk-Datei..." );
    mnuExport.add( this.mnuFileExportToAnadisk );

    this.mnuFileExportToSound = createJMenuItem( "Sound-Datei..." );
    mnuExport.add( this.mnuFileExportToSound );

    this.mnuFileUnpack = createJMenuItem( "Entpacken..." );
    mnuFile.add( this.mnuFileUnpack );

    JMenu mnuFilePack = new JMenu( "Packen in" );
    mnuFile.add( mnuFilePack );

    this.mnuFilePackTar = createJMenuItem( "TAR-Archiv..." );
    mnuFilePack.add( this.mnuFilePackTar );

    this.mnuFilePackTgz = createJMenuItem( "TGZ-Archiv..." );
    mnuFilePack.add( this.mnuFilePackTgz );

    this.mnuFilePackZip = createJMenuItem( "ZIP-Archiv..." );
    mnuFilePack.add( this.mnuFilePackZip );
    mnuFilePack.addSeparator();

    this.mnuFilePackGZip = createJMenuItem( "GZip-Datei..." );
    mnuFilePack.add( this.mnuFilePackGZip );
    mnuFile.addSeparator();

    this.mnuFileRAMFloppy1Load = createJMenuItem( "In RAM-Floppy 1 laden" );
    mnuFile.add( this.mnuFileRAMFloppy1Load );

    this.mnuFileRAMFloppy2Load = createJMenuItem( "In RAM-Floppy 2 laden" );
    mnuFile.add( this.mnuFileRAMFloppy2Load );
    mnuFile.addSeparator();

    this.mnuFileCreateDir = createJMenuItem( "Verzeichnis erstellen..." );
    mnuFile.add( this.mnuFileCreateDir );

    this.mnuFileRename = createJMenuItem( "Umbenennen..." );
    mnuFile.add( this.mnuFileRename );

    this.mnuFileDelete = createJMenuItem(
			"L\u00F6schen",
			KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
    mnuFile.add( this.mnuFileDelete );

    this.mnuFileLastModified = createJMenuItem(
				"\u00C4nderungszeitpunkt setzen..." );
    mnuFile.add( this.mnuFileLastModified );
    mnuFile.addSeparator();

    this.mnuFileProp = createJMenuItem( "Eigenschaften..." );
    mnuFile.add( this.mnuFileProp );
    mnuFile.addSeparator();

    this.mnuFileRefresh = createJMenuItem( "Aktualisieren" );
    mnuFile.add( this.mnuFileRefresh );
    mnuFile.addSeparator();

    this.mnuFileClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuFileClose );


    // Menu Einstellungen
    JMenu mnuSettings = new JMenu( "Einstellungen" );
    mnuSettings.setMnemonic( KeyEvent.VK_E );

    this.mnuHiddenFiles = new JCheckBoxMenuItem(
					"Versteckte Dateien anzeigen",
					false );
    this.mnuHiddenFiles.addActionListener( this );
    mnuSettings.add( this.mnuHiddenFiles );

    this.mnuSortCaseSensitive = new JCheckBoxMenuItem(
			"Gro\u00DF-/Kleinschreibung bei Sortierung beachten",
			true );
    this.mnuSortCaseSensitive.addActionListener( this );
    mnuSettings.add( this.mnuSortCaseSensitive );

    this.mnuLoadOnDoubleClick = new JCheckBoxMenuItem(
		"Doppelklick f\u00FChrt Laden/Starten ohne Nachfrage aus",
		false );
    this.mnuLoadOnDoubleClick.addActionListener( this );
    mnuSettings.add( this.mnuLoadOnDoubleClick );
    mnuSettings.addSeparator();

    JMenu mnuSettingsPreview = new JMenu(
			"Max. Dateigr\u00F6\u00DFe f\u00FCr Vorschau" );
    mnuSettings.add( mnuSettingsPreview );

    ButtonGroup grpPreviewMaxFileSize = new ButtonGroup();

    this.mnuNoPreview = new JRadioButtonMenuItem( "Keine Vorschau" );
    this.mnuNoPreview.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuNoPreview );
    mnuSettingsPreview.add( this.mnuNoPreview );

    this.mnuPreviewMaxFileSize100K = new JRadioButtonMenuItem( "100 KByte" );
    this.mnuPreviewMaxFileSize100K.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize100K );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize100K );

    this.mnuPreviewMaxFileSize1M = new JRadioButtonMenuItem( "1 MByte" );
    this.mnuPreviewMaxFileSize1M.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize1M );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize1M );

    this.mnuPreviewMaxFileSize10M = new JRadioButtonMenuItem( "10 MByte" );
    this.mnuPreviewMaxFileSize10M.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize10M );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize10M );

    this.mnuPreviewMaxFileSize100M = new JRadioButtonMenuItem( "100 MByte" );
    this.mnuPreviewMaxFileSize100M.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewMaxFileSize100M );
    mnuSettingsPreview.add( this.mnuPreviewMaxFileSize100M );

    this.mnuPreviewNoFileSizeLimit = new JRadioButtonMenuItem( "Unbegrenzt" );
    this.mnuPreviewNoFileSizeLimit.addActionListener( this );
    grpPreviewMaxFileSize.add( this.mnuPreviewNoFileSizeLimit );
    mnuSettingsPreview.add( this.mnuPreviewNoFileSizeLimit );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuSettings );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


    // Popup-Menu
    this.mnuPopup = new JPopupMenu();

    this.mnuPopupLoadIntoEmuOpt = createJMenuItem( "In Emulator laden mit..." );
    this.mnuPopup.add( this.mnuPopupLoadIntoEmuOpt );

    this.mnuPopupLoadIntoEmu = createJMenuItem( "In Emulator laden" );
    this.mnuPopup.add( this.mnuPopupLoadIntoEmu );

    this.mnuPopupStartInEmu = createJMenuItem( "Im Emulator starten" );
    this.mnuPopup.add( this.mnuPopupStartInEmu );

    this.mnuPopupEditText = createJMenuItem( "Im Texteditor \u00F6ffnen" );
    this.mnuPopup.add( this.mnuPopupEditText );

    this.mnuPopupShowImage = createJMenuItem( "Im Bildbetrachter anzeigen" );
    this.mnuPopup.add( this.mnuPopupShowImage );

    this.mnuPopupEditHex = createJMenuItem( "Im Hex-Editor \u00F6ffnen" );
    this.mnuPopup.add( this.mnuPopupEditHex );

    this.mnuPopupDiffHex = createJMenuItem(
			"Zum Hex-Dateivergleicher hinzuf\u00FCgen..." );
    this.mnuPopup.add( this.mnuPopupDiffHex );

    this.mnuPopupChecksum = createJMenuItem(
				"Pr\u00FCfsumme/Hash-Wert berechnen..." );
    this.mnuPopup.add( this.mnuPopupChecksum );

    this.mnuPopupPlay = createJMenuItem( "Wiedergeben" );
    this.mnuPopup.add( this.mnuPopupPlay );

    JMenu mnuPopupExport = new JMenu( "Exportieren in" );
    this.mnuPopup.add( mnuPopupExport );

    this.mnuPopupExportToPlainDisk = createJMenuItem(
				"einfache Diskettenabbilddatei..." );
    mnuPopupExport.add( this.mnuPopupExportToPlainDisk );

    this.mnuPopupExportToAnadisk = createJMenuItem( "Anadisk-Datei..." );
    mnuPopupExport.add( this.mnuPopupExportToAnadisk );

    this.mnuPopupExportToSound = createJMenuItem( "Sound-Datei..." );
    mnuPopupExport.add( this.mnuPopupExportToSound );

    this.mnuPopupUnpack = createJMenuItem( "Entpacken..." );
    this.mnuPopup.add( this.mnuPopupUnpack );

    JMenu mnuPopupPack = new JMenu( "Packen in" );
    this.mnuPopup.add( mnuPopupPack );

    this.mnuPopupPackTar = createJMenuItem( "TAR-Archiv..." );
    mnuPopupPack.add( this.mnuPopupPackTar );

    this.mnuPopupPackTgz = createJMenuItem( "TGZ-Archiv..." );
    mnuPopupPack.add( this.mnuPopupPackTgz );

    this.mnuPopupPackZip = createJMenuItem( "Zip-Archiv..." );
    mnuPopupPack.add( this.mnuPopupPackZip );
    mnuPopupPack.addSeparator();

    this.mnuPopupPackGZip = createJMenuItem( "GZip-Datei..." );
    mnuPopupPack.add( this.mnuPopupPackGZip );
    this.mnuPopup.addSeparator();

    this.mnuPopupRAMFloppy1Load = createJMenuItem( "In RAM-Floppy 1 laden" );
    this.mnuPopup.add( this.mnuPopupRAMFloppy1Load );

    this.mnuPopupRAMFloppy2Load = createJMenuItem( "In RAM-Floppy 2 laden" );
    this.mnuPopup.add( this.mnuPopupRAMFloppy2Load );
    this.mnuPopup.addSeparator();

    this.mnuPopupCreateDir = createJMenuItem( "Verszeichnis erstellen..." );
    this.mnuPopup.add( this.mnuPopupCreateDir );

    this.mnuPopupRename = createJMenuItem( "Umbenennen..." );
    this.mnuPopup.add( this.mnuPopupRename );

    this.mnuPopupDelete = createJMenuItem(
			"L\u00F6schen",
			KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
    this.mnuPopup.add( this.mnuPopupDelete );

    this.mnuPopupLastModified = createJMenuItem(
					"\u00C4nderungszeitpunkt setzen..." );
    this.mnuPopup.add( this.mnuPopupLastModified );
    this.mnuPopup.addSeparator();

    this.mnuPopupProp = createJMenuItem( "Eigenschaften..." );
    this.mnuPopup.add( this.mnuPopupProp );


    // Fensterinhalt
    setLayout( new GridBagLayout() );
    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );


    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    add( toolBar, gbc );

    this.btnLoadIntoEmu = createImageButton(
			"/images/file/load.png",
			"In Emulator laden" );
    toolBar.add( this.btnLoadIntoEmu );

    this.btnStartInEmu = createImageButton(
			"/images/file/start.png",
			"Im Emulator starten" );
    toolBar.add( this.btnStartInEmu );

    this.btnEditText = createImageButton(
			"/images/file/edit.png",
			"Im Texteditor \u00F6ffnen" );
    toolBar.add( this.btnEditText );

    this.btnShowImage = createImageButton(
			"/images/file/image.png",
			"Im Bildbetrachter anzeigen" );
    toolBar.add( this.btnShowImage );

    this.btnPlay = createImageButton(
			"/images/file/play.png",
			"Wiedergeben" );
    toolBar.add( this.btnPlay );


    // Dateibaum
    DefaultTreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
    selectionModel.setSelectionMode(
		TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );

    this.rootNode = new FileNode( null, null, true );
    this.rootNode.refresh(
			null,
			true,
			this.mnuHiddenFiles.isSelected(),
			getFileComparator() );

    this.treeModel = new DefaultTreeModel( this.rootNode );
    this.tree      = new JTree( this.treeModel );
    this.tree.setSelectionModel( selectionModel );
    this.tree.setEditable( false );
    this.tree.setRootVisible( false );
    this.tree.setScrollsOnExpand( true );
    this.tree.setShowsRootHandles( true );
    this.tree.addFocusListener( this );
    this.tree.addKeyListener( this );
    this.tree.addMouseListener( this );
    this.tree.addTreeSelectionListener( this );
    this.tree.addTreeWillExpandListener( this );


    // Dateivorschau
    this.filePreviewFld = new FilePreviewFld( this );
    this.filePreviewFld.setBorder( BorderFactory.createEtchedBorder() );

    this.table = this.filePreviewFld.getJTable();
    if( this.table != null ) {
      this.table.addFocusListener( this );
      this.table.addMouseListener( this );
      ListSelectionModel lsm = this.table.getSelectionModel();
      if( lsm != null )
	lsm.addListSelectionListener( this );
    }


    // Anzeigebereich
    this.splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane( this.tree ),
				this.filePreviewFld );
    this.splitPane.setContinuousLayout( false );
    gbc.anchor  = GridBagConstraints.CENTER;
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( this.splitPane, gbc );


    // Dateibaum als Drag-Quelle
    DragSource dragSource = DragSource.getDefaultDragSource();
    dragSource.createDefaultDragGestureRecognizer(
			this.tree,
			DnDConstants.ACTION_COPY,
			this );


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      setBoundsToDefaults();
    }
    setResizable( true );


    // sonstiges
    updActionButtons();
  }


  public void fireDirectoryChanged( final File dirFile )
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    refreshNodeFor( dirFile );
		  }
		} );
  }


  public void showErrorMsg( final String msg )
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    showErrorMsgInternal( msg );
		  }
		} );
  }


	/* --- DragGestureListener --- */

  public void dragGestureRecognized( DragGestureEvent e )
  {
    TreePath[] treePaths = this.tree.getSelectionPaths();
    if( treePaths != null ) {
      Collection<File> files = new ArrayList<File>();
      for( int i = 0; i < treePaths.length; i++ ) {
	Object o = treePaths[ i ].getLastPathComponent();
	if( o != null ) {
	  if( o instanceof FileNode ) {
	    File file = ((FileNode) o).getFile();
	    if( file != null )
	      files.add( file );
	  }
	}
      }
      if( !files.isEmpty() ) {
	try {
	  e.startDrag( null, new FileListSelection( files ) );
	}
	catch( InvalidDnDOperationException ex ) {}
      }
    }
  }


	/* --- FocusListener --- */

  public void focusGained( FocusEvent e )
  {
    if( !e.isTemporary() ) {
      this.lastActiveFld = e.getComponent();
      updActionButtons();
    }
  }


  public void focusLost( FocusEvent e )
  {
    // leer
  }


	/* --- ListSelectionListener --- */

  public void valueChanged( ListSelectionEvent e )
  {
    updActionButtons();
  }


	/* --- TreeSelectionListener --- */

  public void valueChanged( TreeSelectionEvent e )
  {
    if( e.getSource() == this.tree ) {
      updPreview();
      updActionButtons();
    }
  }


	/* --- TreeWillExpandListener --- */

  public void treeWillCollapse( TreeExpansionEvent e )
					throws ExpandVetoException
  {
    // leer
  }


  public void treeWillExpand( TreeExpansionEvent e )
					throws ExpandVetoException
  {
    TreePath treePath = e.getPath();
    if( treePath != null ) {
      setWaitCursor( true );
      Object o = treePath.getLastPathComponent();
      if( o != null ) {
	if( o instanceof FileNode ) {
	  ((FileNode) o).refresh(
				this.treeModel,
				true,
				this.mnuHiddenFiles.isSelected(),
				getFileComparator() );
	}
      }
      setWaitCursor( false );
    }
  }


	/* --- ueberschriebene Methoden --- */

  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean rv = super.applySettings( props, resizable );
    if( !isVisible() ) {
      this.mnuHiddenFiles.setSelected(
		EmuUtil.parseBooleanProperty(
				props,
				"jkcemu.filebrowser.show_hidden_files",
				false ) );
      this.mnuSortCaseSensitive.setSelected(
		EmuUtil.parseBooleanProperty(
				props,
				"jkcemu.filebrowser.sort_case_sensitive",
				false ) );
      this.mnuLoadOnDoubleClick.setSelected(
		EmuUtil.parseBooleanProperty(
				props,
				"jkcemu.filebrowser.load_on_double_click",
				false ) );
      String previewMaxFileSize = null;
      if( props != null ) {
	previewMaxFileSize = props.getProperty(
				"jkcemu.filebrowser.preview.max_file_size" );
      }
      if( previewMaxFileSize != null ) {
	if( previewMaxFileSize.equals( "no_preview" ) ) {
	  this.mnuNoPreview.setSelected( true );
	} else if( previewMaxFileSize.equals( "100K" ) ) {
	  this.mnuPreviewMaxFileSize100K.setSelected( true );
	} else if( previewMaxFileSize.equals( "1M" ) ) {
	  this.mnuPreviewMaxFileSize1M.setSelected( true );
	} else if( previewMaxFileSize.equals( "10M" ) ) {
	  this.mnuPreviewMaxFileSize10M.setSelected( true );
	} else if( previewMaxFileSize.equals( "100M" ) ) {
	  this.mnuPreviewMaxFileSize100M.setSelected( true );
	} else {
	  this.mnuPreviewNoFileSizeLimit.setSelected( true );
	}
      } else {
	this.mnuPreviewNoFileSizeLimit.setSelected( true );
      }
      int splitPos = EmuUtil.parseIntProperty(
				props,
				"jkcemu.filebrowser.split.position",
				-1,
				-1 );
      if( splitPos >= 0 ) {
	this.splitPane.setDividerLocation( splitPos );
      } else {
	Component c = this.splitPane.getLeftComponent();
	if( c != null ) {
	  int       xDiv = c.getWidth() / 2;
	  Dimension size = this.tree.getPreferredSize();
	  if( size != null ) {
	    int xTmp = (size.width * 3) + 50;
	    if( xTmp > xDiv )
	      xDiv = xTmp;
	  }
	  this.splitPane.setDividerLocation( xDiv );
	}
      }
      doFileRefresh();
      updPreview();
    }
    return rv;
  }


  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      if( e != null ) {
	Object src = e.getSource();
	if( src != null ) {
	  if( (src == this.table) || (src == this.tree) ) {
	    rv = true;
	    doFileAction( src );
	  }
	  else if( src == this.mnuFileRefresh ) {
	    rv = true;
	    doFileRefresh();
	  }
	  else if( (src == this.mnuFileLoadIntoEmuOpt)
		   || (src == this.mnuPopupLoadIntoEmuOpt) )
	  {
	    rv = true;
	    doFileLoadIntoEmu( true, false );
	  }
	  else if( (src == this.mnuFileLoadIntoEmu)
		   || (src == this.mnuPopupLoadIntoEmu)
		   || (src == this.btnLoadIntoEmu) )
	  {
	    rv = true;
	    doFileLoadIntoEmu( false, false );
	  }
	  else if( (src == this.mnuFileStartInEmu)
		   || (src == this.mnuPopupStartInEmu)
		   || (src == this.btnStartInEmu) )
	  {
	    rv = true;
	    doFileLoadIntoEmu( false, true );
	  }
	  else if( (src == this.mnuFileEditText)
		   || (src == this.mnuPopupEditText)
		   || (src == this.btnEditText) )
	  {
	    rv = true;
	    doFileEditText();
	  }
	  else if( (src == this.mnuFileShowImage)
		   || (src == this.mnuPopupShowImage)
		   || (src == this.btnShowImage) )
	  {
	    rv = true;
	    doFileShowImage();
	  }
	  else if( (src == this.mnuFileEditHex)
		   || (src == this.mnuPopupEditHex) )
	  {
	    rv = true;
	    doFileEditHex();
	  }
	  else if( (src == this.mnuFileDiffHex)
		   || (src == this.mnuPopupDiffHex) )
	  {
	    rv = true;
	    doFileDiffHex();
	  }
	  else if( (src == this.mnuFileChecksum)
		   || (src == this.mnuPopupChecksum) )
	  {
	    rv = true;
	    this.screenFrm.openFileChecksumFrm( getSelectedFiles() );
	  }
	  else if( (src == this.mnuFilePlay)
		   || (src == this.mnuPopupPlay)
		   || (src == this.btnPlay) )
	  {
	    rv = true;
	    doFilePlay();
	  }
	  else if( (src == this.mnuFileExportToPlainDisk)
		   || (src == this.mnuPopupExportToPlainDisk) )
	  {
	    rv = true;
	    doFileExportToPlainDisk();
	  }
	  else if( (src == this.mnuFileExportToAnadisk)
		   || (src == this.mnuPopupExportToAnadisk) )
	  {
	    rv = true;
	    doFileExportToAnadisk();
	  }
	  else if( (src == this.mnuFileExportToSound)
		   || (src == this.mnuPopupExportToSound) )
	  {
	    rv = true;
	    doFileExportToSound();
	  }
	  else if( (src == this.mnuFileUnpack)
		   || (src == this.mnuPopupUnpack) )
	  {
	    rv = true;
	    doFileUnpack( null );
	  }
	  else if( (src == this.mnuFilePackGZip)
		   || (src == this.mnuPopupPackGZip) )
	  {
	    rv = true;
	    doFilePackGZip();
	  }
	  else if( (src == this.mnuFilePackTar)
		   || (src == this.mnuPopupPackTar) )
	  {
	    rv = true;
	    doFilePackTar( false );
	  }
	  else if( (src == this.mnuFilePackTgz)
		   || (src == this.mnuPopupPackTgz) )
	  {
	    rv = true;
	    doFilePackTar( true );
	  }
	  else if( (src == this.mnuFilePackZip)
		   || (src == this.mnuPopupPackZip) )
	  {
	    rv = true;
	    doFilePackZip();
	  }
	  else if( (src == this.mnuFileRAMFloppy1Load)
		   || (src == this.mnuPopupRAMFloppy1Load) )
	  {
	    rv = true;
	    doFileRAMFloppyLoad(
			this.screenFrm.getEmuThread().getRAMFloppy1() );
	  }
	  else if( (src == this.mnuFileRAMFloppy2Load)
		   || (src == this.mnuPopupRAMFloppy2Load) )
	  {
	    rv = true;
	    doFileRAMFloppyLoad(
			this.screenFrm.getEmuThread().getRAMFloppy2() );
	  }
	  else if( (src == this.mnuFileCreateDir)
		   || (src == this.mnuPopupCreateDir) )
	  {
	    rv = true;
	    doFileCreateDir();
	  }
	  else if( (src == this.mnuFileRename)
		   || (src == this.mnuPopupRename) )
	  {
	    rv = true;
	    doFileRename();
	  }
	  else if( (src == this.mnuFileDelete)
		   || (src == this.mnuPopupDelete) )
	  {
	    rv = true;
	    doFileDelete();
	  }
	  else if( (src == this.mnuFileLastModified)
		   || (src == this.mnuPopupLastModified) )
	  {
	    rv = true;
	    doFileLastModified();
	  }
	  else if( (src == this.mnuFileProp)
		   || (src == this.mnuPopupProp) )
	  {
	    rv = true;
	    doFileProp();
	  }
	  else if( src == this.mnuFileClose ) {
	    rv = true;
	    doClose();
	  }
	  else if( (src == this.mnuHiddenFiles)
		   || (src == this.mnuSortCaseSensitive) )
	  {
	    rv = true;
	    doFileRefresh();
	    updPreview();
	  }
	  else if( src == this.mnuHelpContent ) {
	    rv = true;
	    this.screenFrm.showHelp( "/help/tools/filebrowser.htm" );
	  }
	}
      }
    }
    catch( IOException ex ) {
      BasicDlg.showErrorDlg( this, ex );
    }
    return rv;
  }


  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null )
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
  }


  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );
      props.setProperty(
		"jkcemu.filebrowser.show_hidden_files",
		String.valueOf( this.mnuHiddenFiles.isSelected() ) );
      props.setProperty(
		"jkcemu.filebrowser.sort_case_sensitive",
		String.valueOf( this.mnuSortCaseSensitive.isSelected() ) );
      props.setProperty(
		"jkcemu.filebrowser.load_on_double_click",
		String.valueOf( this.mnuLoadOnDoubleClick.isSelected() ) );

      String previewMaxFileSize = "unlimited";
      if( this.mnuNoPreview.isSelected() ) {
	previewMaxFileSize = "no_preview";
      } else if( this.mnuPreviewMaxFileSize100K.isSelected() ) {
	previewMaxFileSize = "100K";
      } else if( this.mnuPreviewMaxFileSize1M.isSelected() ) {
	previewMaxFileSize = "1M";
      } else if( this.mnuPreviewMaxFileSize10M.isSelected() ) {
	previewMaxFileSize = "10M";
      } else if( this.mnuPreviewMaxFileSize100M.isSelected() ) {
	previewMaxFileSize = "100M";
      }
      props.setProperty(
		"jkcemu.filebrowser.preview.max_file_size",
		previewMaxFileSize );

      props.setProperty(
		"jkcemu.filebrowser.split.position",
		String.valueOf( this.splitPane.getDividerLocation() ) );
    }
  }


  public void mouseClicked( MouseEvent e )
  {
    if( checkPopup( e ) ) {
      e.consume();
    } else {
      boolean done = false;
      if( (e.getClickCount() > 1) && (e.getButton() == MouseEvent.BUTTON1) ) {
	Component c = e.getComponent();
	if( c != null ) {
	  if( (c == this.tree) || (c instanceof JTable) ) {
	    try {
	      doFileAction( c );
	    }
	    catch( IOException ex ) {
	      BasicDlg.showErrorDlg( this, ex );
	    }
	    e.consume();
	  }
	}
      }
      if( !done ) {
	super.mouseClicked( e );
      }
    }
  }


  public void mousePressed( MouseEvent e )
  {
    if( checkPopup( e ) )
      e.consume();
    else
      super.mousePressed( e );
  }


  public void mouseReleased( MouseEvent e )
  {
    if( checkPopup( e ) )
      e.consume();
    else
      super.mouseReleased( e );
  }


  public void windowClosed( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.screenFrm.childFrameClosed( this );
  }


	/* --- Aktionen --- */

  private void doFileAction( Object src ) throws IOException
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      if( (src == this.table) && !fileNode.isLeaf() ) {
	TreeNode[] path = this.treeModel.getPathToRoot( fileNode );
	if( path != null ) {
	  setWaitCursor( true );
	  this.tree.setSelectionPath( new TreePath( path ) );
	  fileNode.refresh(
			this.treeModel,
			true,
			this.mnuHiddenFiles.isSelected(),
			getFileComparator() );
	  this.filePreviewFld.setFileNode(
				fileNode,
				getPreviewMaxFileSize(),
				this.mnuSortCaseSensitive.isSelected() );
	  setWaitCursor( false );
	}
      } else {
	File file = fileNode.getFile();
	if( file != null ) {
	  if( file.isFile() ) {
	    if( fileNode.isArchiveFile() || fileNode.isCompressedFile() ) {
	      doFileUnpack( fileNode );
	    } else if( fileNode.isAudioFile() ) {
	      AudioFilePlayer.play( this, file );
	    } else if( fileNode.isImageFile() ) {
	      this.screenFrm.showImageFile( file );
	    } else if( fileNode.isTextFile() ) {
	      this.screenFrm.openTextFile( file );
	    } else if( fileNode.fileNameEndsWith( ".prj" ) ) {
	      Properties props = EditFrm.loadProject( file );
	      if( props != null ) {
		this.screenFrm.openProject( file, props );
	      } else {
		throw new IOException(
			"Die PRJ-Datei ist keine JKCEMU-Projektdatei." );
	      }
	    } else {
	      boolean  loadable = false;
	      FileInfo fileInfo = fileNode.getFileInfo();
	      if( fileInfo != null ) {
		if( fileInfo.getBegAddr() >= 0 ) {
		  loadable = true;
		}
	      }
	      if( !loadable ) {
		String fName = file.getName();
		if( fName != null ) {
		  if( fName.toLowerCase().endsWith( ".bin" ) ) {
		    loadable = true;
		  }
		}
	      }
	      if( loadable ) {
		LoadDlg.loadFile(
			this,		// owner
			this.screenFrm,
			file,
			!this.mnuLoadOnDoubleClick.isSelected(),
			true,		// startEnabled
			fileNode.isStartableFile() );
	      } else {
		if( file.canRead() ) {
		  try {
		    if( Desktop.isDesktopSupported() ) {
		      Desktop desktop = Desktop.getDesktop();
		      if( desktop != null ) {
			if( desktop.isSupported( Desktop.Action.OPEN ) ) {
			  desktop.open( file );
			}
		      }
		    }
		  }
		  catch( Exception ex ) {
		    BasicDlg.showErrorDlg( this, ex );
		  }
		}
	      }
	    }
	  }
	}
      }
    }
  }


  private void doFileLoadIntoEmu( boolean interactive, boolean startSelected )
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	if( file.isFile() ) {
	  LoadDlg.loadFile(
			this,		// owner
			this.screenFrm,
			file,
			interactive,
			true,		// startEnabled
			startSelected );
	}
      }
    }
  }


  private void doFileEditText()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	if( file.isFile() )
	  this.screenFrm.openTextFile( file );
      }
    }
  }


  private void doFileEditHex()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	if( file.isFile() )
	  this.screenFrm.openHexEditor( file );
      }
    }
  }


  private void doFileDiffHex()
  {
    java.util.List<File> files = getSelectedFiles();
    if( files != null ) {
      int n = files.size();
      if( n > 0 )
	this.screenFrm.addToHexDiff( files );
    }
  }


  private void doFileExportToAnadisk()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	AbstractFloppyDisk disk    = null;
	File               outFile = null;
	try {
	  if( fileNode.isPlainDiskFile() ) {
	    FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			this,
			FloppyDiskFormat.getFormatByDiskSize( file.length() ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT );
	    dlg.setVisible( true );
	    FloppyDiskFormat fmt = dlg.getFormat();
	    if( fmt != null ) {
	      disk = PlainFileFloppyDisk.openFile( this, file, true, fmt );
	    }
	  }
	  else if( fileNode.isTelediskFile() ) {
	    disk = TelediskFloppyDisk.readFile( this, file );
	  }
	  if( disk != null ) {
	    outFile = EmuUtil.showFileSaveDlg(
				this,
				"Anadisk-Datei speichern",
				EmuUtil.getDestFile(
					file,
					".dump",
					Main.getLastPathFile( "disk" ) ),
				EmuUtil.getAnadiskFileFilter() );
	    if( outFile != null ) {
	      if( DiskUtil.checkFileExt(
				this,
				outFile,
				DiskUtil.anadiskFileExt ) )
	      {
		AnadiskFloppyDisk.export( disk, outFile );
		fireDirectoryChanged( outFile.getParentFile() );
		showExportFinished();
	      }
	    }
	  }
	}
	catch( IOException ex ) {
	  if( outFile != null ) {
	    outFile.delete();
	  }
	  BasicDlg.showErrorDlg( this, ex );
	}
	finally {
	  if( disk != null ) {
	    disk.doClose();
	  }
	}
      }
    }
  }


  private void doFileExportToPlainDisk()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	AbstractFloppyDisk disk    = null;
	File               outFile = null;
	try {
	  if( fileNode.isAnadiskFile() ) {
	    disk = AnadiskFloppyDisk.readFile( this, file );
	  }
	  else if( fileNode.isTelediskFile() ) {
	    disk = TelediskFloppyDisk.readFile( this, file );
	  }

	  else if( fileNode.isPlainDiskFile() ) {
	    FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			this,
			FloppyDiskFormat.getFormatByDiskSize( file.length() ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT );
	    dlg.setVisible( true );
	    FloppyDiskFormat fmt = dlg.getFormat();
	    if( fmt != null ) {
	      disk = PlainFileFloppyDisk.openFile( this, file, true, fmt );
	    }
	  }


	  if( disk != null ) {
	    outFile = EmuUtil.showFileSaveDlg(
				this,
				"Einfache Abbilddatei speichern",
				EmuUtil.getDestFile(
					file,
					".img",
					Main.getLastPathFile( "disk" ) ),
				EmuUtil.getPlainDiskFileFilter() );
	    if( outFile != null ) {
	      if( DiskUtil.checkFileExt(
				this,
				outFile,
				DiskUtil.plainDiskFileExt ) )
	      {
		PlainFileFloppyDisk.export( disk, outFile );
		fireDirectoryChanged( outFile.getParentFile() );
		showExportFinished();
	      }
	    }
	  }
	}
	catch( IOException ex ) {
	  if( outFile != null ) {
	    outFile.delete();
	  }
	  BasicDlg.showErrorDlg( this, ex );
	}
	finally {
	  if( disk != null ) {
	    disk.doClose();
	  }
	}
      }
    }
  }


  private void doFileExportToSound()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      if( fileNode.isTAPFile() ) {
	File file = fileNode.getFile();
	if( file != null ) {
	  File outFile = EmuUtil.showFileSaveDlg(
				this,
				"Sound-Datei speichern",
				Main.getLastPathFile( "audio" ),
				AudioUtil.getAudioOutFileFilter() );
	  if( outFile != null ) {
	    AudioFileFormat.Type fileType = AudioUtil.getAudioFileType(
								this,
								outFile );
	    if( fileType != null ) {
	      Exception        ex = null;
	      AudioInputStream in = null;
	      try {
		KCTapAudioInputStream tapIn = new KCTapAudioInputStream(
				EmuUtil.readFile( file, 0x100000 ) );
		in = new AudioInputStream(
				tapIn,
				tapIn.getAudioFormat(),
				tapIn.getFrameLength() );
		AudioSystem.write( in, fileType, outFile );
		fireDirectoryChanged( outFile.getParentFile() );
		showExportFinished();
	      }
	      catch( Exception ex1 ) {
		ex = ex1;
	      }
	      finally {
		EmuUtil.doClose( in );
	      }
	      if( ex != null ) {
		BasicDlg.showErrorDlg( this, ex );
	      }
	    }
	  }
	}
      }
    }
  }


  private void doFileUnpack( FileNode fileNode ) throws IOException
  {
    if( fileNode == null ) {
      fileNode = getSelectedFileNode();
    }
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	String fileName = file.getName();
	if( (fileName != null) && file.isFile() ) {
	  String upperName = fileName.toUpperCase();
	  if( upperName.endsWith( ".GZ" ) ) {
	    File outFile = askForOutputFile(
			file,
			"Entpackte Datei speichern",
			fileName.substring( 0, fileName.length() - 3 ) );
	    if( outFile != null ) {
	      GZipUnpacker.unpackFile( this, file, outFile );
	    }
	  }
	  else if( upperName.endsWith( ".TAR" )
		   || upperName.endsWith( ".TGZ" ) )
	  {
	    File outDir = EmuUtil.askForOutputDir(
				this,
				file,
				"Entpacken nach:",
				"Archiv-Datei entpacken" );
	    if( outDir != null ) {
	      TarUnpacker.unpackFile(
			this,
			file,
			outDir,
			upperName.endsWith( ".TGZ" ) );
	    }
	  }
	  else if( upperName.endsWith( ".JAR" )
		   || upperName.endsWith( ".ZIP" ) )
	  {
	    File outDir = EmuUtil.askForOutputDir(
				this,
				file,
				"Entpacken nach:",
				"Archiv-Datei entpacken" );
	    if( outDir != null ) {
	      ZipUnpacker.unpackFile( this, file, outDir );
	    }
	  }
	  else if( fileNode.isAnadiskFile() ) {
	    AbstractFloppyDisk disk = AnadiskFloppyDisk.readFile(
								this,
								file );
	    if( disk != null ) {
	      DiskUtil.unpackDisk( this, file, disk, "Anadisk-Datei" );
	    }
	  }
	  else if( fileNode.isTelediskFile() ) {
	    AbstractFloppyDisk disk = TelediskFloppyDisk.readFile(
								this,
								file );
	    if( disk != null ) {
	      DiskUtil.unpackDisk( this, file, disk, "Teledisk-Datei" );
	    }
	  }
	  else if( fileNode.isPlainDiskFile() ) {
	    DiskUtil.unpackPlainDiskFile( this, file );
	  }
	}
      }
    }
  }


  private void doFilePackGZip()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	if( file.isFile() ) {
	  String fileName = file.getName();
	  if( fileName != null ) {
	    fileName += ".gz";
	  }
	  File outFile = askForOutputFile(
				file,
				"GZip-Datei speichern",
				fileName );
	  if( outFile != null )
	    GZipPacker.packFile( this, file, outFile );
	}
      }
    }
  }


  private void doFilePackTar( boolean compression )
  {
    java.util.List<File> files = getSelectedFiles();
    if( files != null ) {
      if( !files.isEmpty() ) {
	File   firstFile = files.get( 0 );
	String fileName  = firstFile.getName();
	if( fileName != null ) {
	  int pos = fileName.indexOf( '.' );
	  if( (pos == 0) && (fileName.length() > 1) ) {
	    pos = fileName.indexOf( '.', 1 );
	  }
	  if( pos >= 0 ) {
	    fileName = fileName.substring( 0, pos );
	  }
	  if( fileName.length() > 0 ) {
	    if( compression ) {
	      fileName += ".tgz";
	    } else {
	      fileName += ".tar";
	    }
	  }
	  File outFile = askForOutputFile(
				firstFile,
				compression ?
					"TGZ-Datei speichern"
					: "TAR-Datei speichern",
				fileName );
	  if( outFile != null )
	    TarPacker.packFiles( this, files, outFile, compression );
	}
      }
    }
  }


  private void doFilePackZip()
  {
    java.util.List<File> files = getSelectedFiles();
    if( files != null ) {
      if( !files.isEmpty() ) {
	File   firstFile = files.get( 0 );
	String fileName  = firstFile.getName();
	if( fileName != null ) {
	  int pos = fileName.indexOf( '.' );
	  if( (pos == 0) && (fileName.length() > 1) ) {
	    pos = fileName.indexOf( '.', 1 );
	  }
	  if( pos >= 0 ) {
	    fileName = fileName.substring( 0, pos );
	  }
	  if( fileName.length() > 0 ) {
	    fileName += ".zip";
	  }
	  File outFile = askForOutputFile(
				firstFile,
				"ZIP-Datei speichern",
				fileName );
	  if( outFile != null )
	    ZipPacker.packFiles( this, files, outFile );
	}
      }
    }
  }


  private void doFilePlay()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      if( fileNode.isAudioFile() )
	AudioFilePlayer.play( this, fileNode.getFile() );
    }
  }


  private void doFileShowImage()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      if( fileNode.isImageFile() )
	this.screenFrm.showImageFile( fileNode.getFile() );
    }
  }


  private void doFileRAMFloppyLoad( RAMFloppy ramFloppy )
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	boolean state = true;
	if( ramFloppy.hasDataChanged() ) {
	  if( BasicDlg.showYesNoDlg(
		this,
		"Die Daten in der RAM-Floppy sind nicht gespeichert.\n"
			+ "M\u00F6chten Sie trotzdem die RAM-Floppy mit der\n"
			+ "ausgew\u00E5hlten Datei laden?" ) )
	  {
	    state = true;
	  }
	}
	if( state ) {
	  try {
	    ramFloppy.load( file );
	    Main.setLastFile( file, "ramfloppy" );
	  }
	  catch( IOException ex ) {
	    BasicDlg.showErrorDlg(
		this,
		"Die RAM-Floppy kann nicht geladen werden.\n\n"
						+ ex.getMessage() );
	  }
	}
      }
    }
  }


  private void doFileCreateDir()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      TreeNode parent = fileNode;
      File     file   = fileNode.getFile();
      if( file != null ) {
	if( !file.isDirectory() ) {
	  file   = file.getParentFile();
	  parent = fileNode.getParent();
	}
      }
      if( (parent != null) && (file != null) ) {
	File dirFile = EmuUtil.createDir( this, file );
	if( dirFile != null ) {
	  if( parent instanceof FileNode ) {
	    ((FileNode) parent).refresh(
					this.treeModel,
					false,
					this.mnuHiddenFiles.isSelected(),
					getFileComparator() );
	  }
	  selectNode( parent, dirFile );
	  updActionButtons();
	}
      }
    }
  }


  private void doFileRename()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = EmuUtil.renameFile( this, fileNode.getFile() );
      if( file != null ) {
	fileNode.setFile( file );
	this.treeModel.nodeChanged( fileNode );
	selectNode( fileNode );
	updActionButtons();
      }
    }
  }


  private void doFileDelete()
  {
    Collection<FileNode> nodes = getSelectedFileNodes();
    if( nodes != null ) {
      int n = nodes.size();
      if( n > 0 ) {
	Set<FileNode>    parents = new HashSet<FileNode>();
	Collection<File> files   = new ArrayList<File>( n );
	for( FileNode node : nodes ) {
	  File file = node.getFile();
	  if( file != null ) {
	    Object parent = node.getParent();
	    if( parent != null ) {
	      if( parent instanceof FileNode )
		parents.add( (FileNode) parent );
	    }
	    files.add( file );
	  }
	}
	n = files.size();
	if( n > 0 ) {
	  if( EmuUtil.deleteFiles(
				this,
				files.toArray( new File[ n ] ) ) )
	  {
	    for( FileNode parent : parents ) {
	      parent.refresh(
			this.treeModel,
			false,
			this.mnuHiddenFiles.isSelected(),
			getFileComparator() );
	    }
	    updPreview();
	    updActionButtons();
	  }
	}
      }
    }
  }


  private void doFileLastModified()
  {
    TreePath[] treePaths = this.tree.getSelectionPaths();
    if( treePaths != null ) {
      java.util.List<File> files = new ArrayList<File>();
      for( int i = 0; i < treePaths.length; i++ ) {
	Object o = treePaths[ i ].getLastPathComponent();
	if( o != null ) {
	  if( o instanceof FileNode ) {
	    File file = ((FileNode) o).getFile();
	    if( file != null )
	      files.add( file );
	  }
	}
      }
      (new LastModifiedDlg( this, files )).setVisible( true );
    }
  }


  private void doFileRefresh()
  {
    this.rootNode.refresh(
			this.treeModel,
			false,
			this.mnuHiddenFiles.isSelected(),
			getFileComparator() );
    updActionButtons();
  }


  private void doFileProp()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null )
	(new FilePropDlg( this, file )).setVisible( true );
    }
  }


	/* --- private Methoden --- */

  private File askForOutputFile(
			File   srcFile,
			String title,
			String presetName )
  {
    File preSelection = null;
    File parentFile   = srcFile.getParentFile();
    if( presetName != null ) {
      if( parentFile != null ) {
	preSelection = new File( parentFile, presetName );
      } else {
	preSelection = new File( presetName );
      }
    } else {
      preSelection = parentFile;
    }
    File file = EmuUtil.showFileSaveDlg( this, title, preSelection );
    if( file != null ) {
      if( file.exists() ) {
	if( file.equals( srcFile ) ) {
	  BasicDlg.showErrorDlg(
		this,
		"Die Ausgabedatei kann nicht\n"
			+ "mit der Quelldatei identisch sein." );
	  file = null;
	}
	else if( file.isFile() ) {
	  if( !BasicDlg.showYesNoDlg(
		this,
		file.getPath() + " existiert bereits.\n"
			+ "M\u00F6chten Sie die Datei \u00FCberschreiben?" ) )
	  {
	    file = null;
	  }
	} else {
	  BasicDlg.showErrorDlg(
		this,
		file.getPath() + " existiert bereits\n"
			+ "und kann nicht als Datei angelegt werden." );
	  file = null;
	}
      }
    }
    return file;
  }


  private boolean checkPopup( MouseEvent e )
  {
    boolean status = false;
    if( e != null ) {
      if( e.isPopupTrigger() ) {
	Component c = e.getComponent();
	int       x = e.getX();
	int       y = e.getY();
	if( c != null ) {

	   /*
	    * Wenn die Zeile unter der Click-Punkt selektiert ist,
	    * wird das Popupmenu angezeigt,
	    * anderenfalls wird erst einmal selektiert.
	    */
	  boolean selected = false;
	  if( c == this.table ) {
	    int row = this.table.rowAtPoint( new Point( x, y ) );
	    if( row >= 0 ) {
	      if( this.table.isRowSelected( row ) ) {
		selected = true;
	      } else {
		this.table.setRowSelectionInterval( row, row );
	      }
	      this.lastActiveFld = this.table;
	      this.table.requestFocus();
	    }
	  }
	  else if( c == this.tree ) {
	    int row = this.tree.getRowForLocation( x, y );
	    if( row >= 0 ) {
	      if( this.tree.isRowSelected( row ) ) {
		selected = true;
	      } else {
		this.tree.setSelectionRow( row );
	      }
	      this.lastActiveFld = this.tree;
	      this.tree.requestFocus();
	    }
	  }
	  if( selected ) {
	    updActionButtons();
	    this.mnuPopup.show( c, x, y );
	  }
	  status = true;
	}
      }
    }
    return status;
  }


  private FileComparator getFileComparator()
  {
    return this.mnuSortCaseSensitive.isSelected() ?
			FileComparator.getCaseSensitiveInstance()
			: FileComparator.getIgnoreCaseInstance();
  }


  private long getPreviewMaxFileSize()
  {
    long rv = 0;
    if( this.mnuNoPreview.isSelected() ) {
      rv = -1;
    } else if( this.mnuPreviewMaxFileSize100K.isSelected() ) {
      rv = 100 * 1024;
    } else if( this.mnuPreviewMaxFileSize1M.isSelected() ) {
      rv = 1024 * 1024;
    } else if( this.mnuPreviewMaxFileSize10M.isSelected() ) {
      rv = 10 * 1024 * 1024;
    } else if( this.mnuPreviewMaxFileSize100M.isSelected() ) {
      rv = 100 * 1024 * 1024;
    }
    return rv;
  }


  private FileNode getSelectedFileNode()
  {
    FileNode rv = null;
    if( this.lastActiveFld != null ) {
      if( this.lastActiveFld == this.table ) {
	TableModel tm = this.table.getModel();
	if( tm != null ) {
	  if( tm instanceof FileTableModel ) {
	    if( this.table.getSelectedRowCount() == 1 ) {
	      int viewRow = this.table.getSelectedRow();
	      if( viewRow >= 0 ) {
		int modelRow = this.table.convertRowIndexToModel( viewRow );
		if( modelRow >= 0 ) {
		  FileEntry entry = ((FileTableModel) tm).getRow( modelRow );
		  if( entry != null ) {
		    if( entry instanceof ExtendedFileEntry ) {
			rv = ((ExtendedFileEntry) entry).getFileNode();
		    }
		  }
		}
	      }
	    }
	  }
	}
      } else if( this.lastActiveFld == this.tree ) {
	if( this.tree.getSelectionCount() == 1 ) {
	  Object o = this.tree.getLastSelectedPathComponent();
	  if( o != null ) {
	    if( o instanceof FileNode )
	      rv = (FileNode) o;
	  }
	}
      }
    }
    return rv;
  }


  private java.util.List<FileNode> getSelectedFileNodes()
  {
    java.util.List<FileNode> rv = null;
    if( this.lastActiveFld != null ) {
      if( this.lastActiveFld == this.table ) {
	TableModel tm = this.table.getModel();
	if( tm != null ) {
	  if( tm instanceof FileTableModel ) {
	    int[] rowNums = this.table.getSelectedRows();
	    if( rowNums != null ) {
	      if( rowNums.length > 0 ) {
		rv = new ArrayList<FileNode>( rowNums.length );
		for( int i = 0; i < rowNums.length; i++ ) {
		  int modelRow = this.table.convertRowIndexToModel(
							rowNums[ i ] );
		  if( modelRow >= 0 ) {
		    FileEntry entry = ((FileTableModel) tm).getRow( modelRow );
		    if( entry != null ) {
		      if( entry instanceof ExtendedFileEntry ) {
			FileNode fileNode =
				((ExtendedFileEntry) entry).getFileNode();
			if( fileNode != null ) {
			  rv.add( fileNode );
			}
		      }
		    }
		  }
		}
	      }
	    }
	  }
	}
      } else {
	rv = getSelectedTreeFileNodes();
      }
    }
    return rv;
  }


  private java.util.List<File> getSelectedFiles()
  {
    java.util.List<File> rv    = null;
    Collection<FileNode> nodes = getSelectedFileNodes();
    if( nodes != null ) {
      int n = nodes.size();
      if( n > 0 ) {
	rv = new ArrayList<File>( n );
	for( FileNode node : nodes ) {
	  File file = node.getFile();
	  if( file != null )
	    rv.add( file );
	}
      }
    }
    return rv;
  }


  /*
   * Die Methode ermittelt die ausgewaehlten Knoten im Baum.
   * Wenn Vater und Nachkommen ausgewaehlt sind,
   * wird nur der Vater ermittelt.
   */
  private java.util.List<FileNode> getSelectedTreeFileNodes()
  {
    java.util.List<FileNode> rv            = null;
    TreePath[]               selectedPaths = this.tree.getSelectionPaths();
    if( selectedPaths != null ) {
      if( selectedPaths.length > 0 ) {
	Collection<TreePath> treePaths = new ArrayList<TreePath>(
							selectedPaths.length );
	for( int i = 0; i < selectedPaths.length; i++ ) {
	  TreePath           path = selectedPaths[ i ];
	  Iterator<TreePath> iter = treePaths.iterator();
	  if( (path != null) && (iter != null) ) {
	    try {
	      while( iter.hasNext() ) {
		TreePath tmpPath = iter.next();
		if( tmpPath.equals( path ) || tmpPath.isDescendant( path ) ) {
		  path = null;
		  break;
		} else if( path.isDescendant( tmpPath ) ) {
		  iter.remove();
		}
	      }
	    }
	    catch( NoSuchElementException ex1 ) {}
	    catch( UnsupportedOperationException ex2 ) {}
	    if( path != null ) {
	      treePaths.add( path );
	    }
	  }
	}
	int n = treePaths.size();
	if( n > 0 ) {
	  rv = new ArrayList<FileNode>( n );
	  for( TreePath path : treePaths ) {
	    Object o = path.getLastPathComponent();
	    if( o != null ) {
	      if( o instanceof FileNode )
		rv.add( (FileNode) o );
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void refreshNodeFor( File file )
  {
    if( file != null ) {
      this.rootNode.refreshNodeFor(
			file,
			this.treeModel,
			false,
			this.mnuHiddenFiles.isSelected(),
			getFileComparator() );
    }
  }


  private void selectNode( TreeNode node )
  {
    if( node != null ) {
      TreeNode[] path = this.treeModel.getPathToRoot( node );
      if( path != null )
	this.tree.setSelectionPath( new TreePath( path ) );
    }
  }


  private void selectNode( TreeNode parent, File file )
  {
    if( (parent != null) && (file != null) ) {
      Object      childNode = null;
      Enumeration children  = parent.children();
      if( children != null ) {
	try {
	  while( children.hasMoreElements() ) {
	    Object o = children.nextElement();
	    if( o != null ) {
	      if( o instanceof FileNode ) {
		File f = ((FileNode) o).getFile();
		if( f != null ) {
		  if( f.equals( file ) ) {
		    childNode = o;
		    break;
		  }
		}
	      }
	    }
	  }
	}
	catch( NoSuchElementException ex ) {}
      }
      if( childNode != null ) {
	Object[] parentPath = this.treeModel.getPathToRoot( parent );
	if( parentPath != null ) {
	  Object[] path = new Object[ parentPath.length + 1 ];
	  System.arraycopy( parentPath, 0, path, 0, parentPath.length );
	  path[ path.length - 1 ] = childNode;
	  this.tree.setSelectionPath( new TreePath( path ) );
	}
      }
    }
  }


  private void showErrorMsgInternal( String msg )
  {
    BasicDlg.showErrorDlg( this, msg );
  }


  private void showExportFinished()
  {
    BasicDlg.showInfoDlg( this, "Export erfolgreich beendet" );
  }


  private void updActionButtons()
  {
    int                  nNodes    = 0;
    int                  nFiles    = 0;
    File                 file      = null;
    FileNode             fileNode  = null;
    Collection<FileNode> fileNodes = getSelectedFileNodes();
    if( fileNodes != null ) {
      nNodes = fileNodes.size();
      for( FileNode tmpNode : fileNodes ) {
	fileNode     = tmpNode;
	File tmpFile = tmpNode.getFile();
	if( tmpFile != null ) {
	  if( tmpFile.isFile() ) {
	    file = tmpFile;
	    nFiles++;
	  }
	}
      }
    }
    if( nNodes > 1 ) {
      fileNode = null;
      file     = null;
    }
    if( nFiles > 1 ) {
      file = null;
    }
    boolean stateEntries   = (nNodes > 0);
    boolean stateOneEntry  = (nNodes == 1);
    boolean stateFilesOnly = ((nFiles > 0) && (nFiles == nNodes));
    boolean stateOneFile   = ((nFiles == 1) && (nFiles == nNodes));
    if( fileNode != null ) {
      boolean isStartable = fileNode.isStartableFile();
      this.mnuFileStartInEmu.setEnabled( isStartable );
      this.mnuPopupStartInEmu.setEnabled( isStartable );
      this.btnStartInEmu.setEnabled( isStartable );

      boolean isImage = fileNode.isImageFile();
      this.mnuFileShowImage.setEnabled( isImage );
      this.mnuPopupShowImage.setEnabled( isImage );
      this.btnShowImage.setEnabled( isImage );

      boolean isAudio = fileNode.isAudioFile();
      this.mnuFilePlay.setEnabled( isAudio );
      this.mnuPopupPlay.setEnabled( isAudio );
      this.btnPlay.setEnabled( isAudio );

      boolean isText = (stateOneFile && !isImage && !isAudio);
      this.mnuFileEditText.setEnabled( isText );
      this.mnuPopupEditText.setEnabled( isText );
      this.btnEditText.setEnabled( isText );

      boolean isPlainDisk = fileNode.isPlainDiskFile();
      boolean isAnadisk   = fileNode.isAnadiskFile();
      boolean isTeledisk  = fileNode.isTelediskFile();

      this.mnuFileExportToPlainDisk.setEnabled( isAnadisk || isTeledisk );
      this.mnuPopupExportToPlainDisk.setEnabled( isAnadisk || isTeledisk );

      this.mnuFileExportToAnadisk.setEnabled( isPlainDisk || isTeledisk );
      this.mnuPopupExportToAnadisk.setEnabled( isPlainDisk || isTeledisk );

      boolean isTAP = fileNode.isTAPFile();
      this.mnuFileExportToSound.setEnabled( isTAP );
      this.mnuPopupExportToSound.setEnabled( isTAP );

      boolean isUnpackable = (fileNode.isArchiveFile()
					|| fileNode.isCompressedFile()
					|| isPlainDisk
					|| isAnadisk
					|| isTeledisk);
      this.mnuFileUnpack.setEnabled( isUnpackable );
      this.mnuPopupUnpack.setEnabled( isUnpackable );

      boolean stateDir = false;
      if( file != null ) {
	if( (fileNode.getParent() != null) || file.isDirectory() )
	  stateDir = true;
      }
      this.mnuFileCreateDir.setEnabled( stateDir );
      this.mnuPopupCreateDir.setEnabled( stateDir );

      this.mnuFileRename.setEnabled( stateOneEntry );
      this.mnuPopupRename.setEnabled( stateOneEntry );

      this.mnuFileProp.setEnabled( stateOneEntry );
      this.mnuPopupProp.setEnabled( stateOneEntry );

    } else {

      this.mnuFileStartInEmu.setEnabled( false );
      this.mnuPopupStartInEmu.setEnabled( false );
      this.btnStartInEmu.setEnabled( false );

      this.mnuFileShowImage.setEnabled( false );
      this.mnuPopupShowImage.setEnabled( false );
      this.btnShowImage.setEnabled( false );

      this.mnuFilePlay.setEnabled( false );
      this.mnuPopupPlay.setEnabled( false );
      this.btnPlay.setEnabled( false );

      this.mnuFileEditText.setEnabled( false );
      this.mnuPopupEditText.setEnabled( false );
      this.btnEditText.setEnabled( false );

      this.mnuFileExportToPlainDisk.setEnabled( false );
      this.mnuPopupExportToPlainDisk.setEnabled( false );

      this.mnuFileExportToSound.setEnabled( false );
      this.mnuPopupExportToSound.setEnabled( false );

      this.mnuFileUnpack.setEnabled( false );
      this.mnuPopupUnpack.setEnabled( false );

      this.mnuFileCreateDir.setEnabled( false );
      this.mnuPopupCreateDir.setEnabled( false );

      this.mnuFileRename.setEnabled( false );
      this.mnuPopupRename.setEnabled( false );

      this.mnuFileProp.setEnabled( false );
      this.mnuPopupProp.setEnabled( false );
    }
    this.mnuFileLoadIntoEmuOpt.setEnabled( stateOneFile );
    this.mnuFileLoadIntoEmu.setEnabled( stateOneFile );
    this.mnuPopupLoadIntoEmuOpt.setEnabled( stateOneFile );
    this.mnuPopupLoadIntoEmu.setEnabled( stateOneFile );
    this.btnLoadIntoEmu.setEnabled( stateOneFile );

    this.mnuFileEditHex.setEnabled( stateOneFile );
    this.mnuPopupEditHex.setEnabled( stateOneFile );

    this.mnuFileDiffHex.setEnabled( stateFilesOnly );
    this.mnuPopupDiffHex.setEnabled( stateFilesOnly );

    this.mnuFileChecksum.setEnabled( stateFilesOnly );
    this.mnuPopupChecksum.setEnabled( stateFilesOnly );

    this.mnuFilePackGZip.setEnabled( stateOneFile );
    this.mnuPopupPackGZip.setEnabled( stateOneFile );

    this.mnuFilePackTar.setEnabled( stateEntries );
    this.mnuPopupPackTar.setEnabled( stateEntries );

    this.mnuFilePackTgz.setEnabled( stateEntries );
    this.mnuPopupPackTgz.setEnabled( stateEntries );

    this.mnuFilePackZip.setEnabled( stateEntries );
    this.mnuPopupPackZip.setEnabled( stateEntries );

    this.mnuFileRAMFloppy1Load.setEnabled( stateOneFile );
    this.mnuPopupRAMFloppy1Load.setEnabled( stateOneFile );

    this.mnuFileRAMFloppy2Load.setEnabled( stateOneFile );
    this.mnuPopupRAMFloppy2Load.setEnabled( stateOneFile );

    this.mnuFileLastModified.setEnabled( stateEntries );
    this.mnuPopupLastModified.setEnabled( stateEntries );

    this.mnuFileDelete.setEnabled( stateEntries );
    this.mnuPopupDelete.setEnabled( stateEntries );
  }


  private void updPreview()
  {
    FileNode fileNode = null;
    if( this.tree.getSelectionCount() == 1 ) {
      Object o = this.tree.getLastSelectedPathComponent();
      if( o != null ) {
	if( o instanceof FileNode ) {
	  fileNode = (FileNode) o;
	  fileNode.refresh(
			this.treeModel,
			true,
			this.mnuHiddenFiles.isSelected(),
			getFileComparator() );
	}
      }
    }
    this.filePreviewFld.setFileNode(
				fileNode,
				getPreviewMaxFileSize(),
				this.mnuSortCaseSensitive.isSelected() );
  }
}

