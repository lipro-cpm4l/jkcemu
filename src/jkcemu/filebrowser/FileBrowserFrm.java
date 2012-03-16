/*
 * (c) 2008-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Datei-Browser
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.awt.datatransfer.*;
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
import jkcemu.emusys.ac1_llc2.*;
import jkcemu.emusys.kc85.KCAudioDataStream;
import jkcemu.emusys.z1013.Z1013AudioDataStream;
import jkcemu.text.EditFrm;


public class FileBrowserFrm extends BasicFrm
					implements
						DragGestureListener,
						FlavorListener,
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
  private JMenuItem            mnuFileConvert;
  private JMenuItem            mnuFileUnpack;
  private JMenuItem            mnuFilePackGZip;
  private JMenuItem            mnuFilePackZip;
  private JMenuItem            mnuFilePackTar;
  private JMenuItem            mnuFilePackTgz;
  private JMenuItem            mnuFileAudioIn;
  private JMenuItem            mnuFilePlay;
  private JMenuItem            mnuFilePlayAC1;
  private JMenuItem            mnuFilePlayAC1Basic;
  private JMenuItem            mnuFilePlaySCCH;
  private JMenuItem            mnuFilePlayKC85;
  private JMenuItem            mnuFilePlayZ1013;
  private JMenuItem            mnuFilePlayZ1013HS;
  private JMenuItem            mnuFilePlayZ9001;
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
  private JMenuItem            mnuEditPathCopy;
  private JMenuItem            mnuEditFileCopy;
  private JMenuItem            mnuEditFilePaste;
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
  private JMenuItem            mnuPopupConvert;
  private JMenuItem            mnuPopupAudioIn;
  private JMenuItem            mnuPopupPlay;
  private JMenuItem            mnuPopupPlayAC1;
  private JMenuItem            mnuPopupPlayAC1Basic;
  private JMenuItem            mnuPopupPlaySCCH;
  private JMenuItem            mnuPopupPlayKC85;
  private JMenuItem            mnuPopupPlayZ1013;
  private JMenuItem            mnuPopupPlayZ1013HS;
  private JMenuItem            mnuPopupPlayZ9001;
  private JMenuItem            mnuPopupUnpack;
  private JMenuItem            mnuPopupPackGZip;
  private JMenuItem            mnuPopupPackZip;
  private JMenuItem            mnuPopupPackTar;
  private JMenuItem            mnuPopupPackTgz;
  private JMenuItem            mnuPopupShowImage;
  private JMenuItem            mnuPopupRAMFloppy1Load;
  private JMenuItem            mnuPopupRAMFloppy2Load;
  private JMenuItem            mnuPopupChecksum;
  private JMenuItem            mnuPopupPathCopy;
  private JMenuItem            mnuPopupFileCopy;
  private JMenuItem            mnuPopupFilePaste;
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
  private Clipboard            clipboard;
  private boolean              filePasteState;


  public FileBrowserFrm( ScreenFrm screenFrm )
  {
    this.screenFrm      = screenFrm;
    this.lastActiveFld  = null;
    this.clipboard      = null;
    this.filePasteState = false;
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
				"Im Hex-Dateivergleicher \u00F6ffnen..." );
    mnuFile.add( this.mnuFileDiffHex );

    this.mnuFileConvert = createJMenuItem(
				"Im Dateikonverter \u00F6ffnen..." );
    mnuFile.add( this.mnuFileConvert );

    this.mnuFileChecksum = createJMenuItem(
				"Pr\u00FCfsumme/Hash-Wert berechnen..." );
    mnuFile.add( this.mnuFileChecksum );

    this.mnuFileAudioIn = createJMenuItem(
				"In Audio/Kassette \u00F6ffnen..." );
    mnuFile.add( this.mnuFileAudioIn );

    this.mnuFilePlay = createJMenuItem( "Wiedergeben" );
    mnuFile.add( this.mnuFilePlay );

    JMenu mnuFilePlayAs = new JMenu( "Wiedergeben im" );
    mnuFile.add( mnuFilePlayAs );

    this.mnuFilePlayAC1 = createJMenuItem( "AC1-Format" );
    mnuFilePlayAs.add( this.mnuFilePlayAC1 );

    this.mnuFilePlayAC1Basic = createJMenuItem( "AC1-BASIC-Format" );
    mnuFilePlayAs.add( this.mnuFilePlayAC1Basic );

    this.mnuFilePlaySCCH = createJMenuItem( "AC1/LLC2-TurboSave-Format" );
    mnuFilePlayAs.add( this.mnuFilePlaySCCH );

    this.mnuFilePlayKC85 = createJMenuItem(
				"KC-Format (HC900, KC85/2..5, KC-BASIC)" );
    mnuFilePlayAs.add( this.mnuFilePlayKC85 );

    this.mnuFilePlayZ9001 = createJMenuItem(
				"KC-Format (KC85/1, KC87, Z9001)" );
    mnuFilePlayAs.add( this.mnuFilePlayZ9001 );

    this.mnuFilePlayZ1013 = createJMenuItem( "Z1013-Format" );
    mnuFilePlayAs.add( this.mnuFilePlayZ1013 );

    this.mnuFilePlayZ1013HS = createJMenuItem( "Z1013-Headersave-Format" );
    mnuFilePlayAs.add( this.mnuFilePlayZ1013HS );

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


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );

    this.mnuEditPathCopy = createJMenuItem(
		"Vollst\u00E4ndiger Datei-/Verzeichnisname kopieren" );
    mnuEdit.add( this.mnuEditPathCopy );

    this.mnuEditFileCopy = createJMenuItem( "Kopieren" );
    mnuEdit.add( this.mnuEditFileCopy );

    this.mnuEditFilePaste = createJMenuItem( "Einf\u00FCgen" );
    mnuEdit.add( this.mnuEditFilePaste );


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
    mnuBar.add( mnuEdit );
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
				"Im Hex-Dateivergleicher \u00F6ffnen" );
    this.mnuPopup.add( this.mnuPopupDiffHex );

    this.mnuPopupConvert = createJMenuItem(
				"Im Dateikonverter \u00F6ffnen" );
    this.mnuPopup.add( this.mnuPopupConvert );

    this.mnuPopupChecksum = createJMenuItem(
				"Pr\u00FCfsumme/Hash-Wert berechnen..." );
    this.mnuPopup.add( this.mnuPopupChecksum );

    this.mnuPopupAudioIn = createJMenuItem(
				"In Audio/Kassette \u00F6ffnen..." );
    this.mnuPopup.add( this.mnuPopupAudioIn );

    this.mnuPopupPlay = createJMenuItem( "Wiedergeben" );
    this.mnuPopup.add( this.mnuPopupPlay );

    JMenu mnuPopupPlayAs = new JMenu( "Wiedergeben im" );
    this.mnuPopup.add( mnuPopupPlayAs );

    this.mnuPopupPlayAC1 = createJMenuItem( "AC1-Format" );
    mnuPopupPlayAs.add( this.mnuPopupPlayAC1 );

    this.mnuPopupPlayAC1Basic = createJMenuItem( "AC1-BASIC-Format" );
    mnuPopupPlayAs.add( this.mnuPopupPlayAC1Basic );

    this.mnuPopupPlaySCCH = createJMenuItem( "AC1/LLC2-TurboSave-Format" );
    mnuPopupPlayAs.add( this.mnuPopupPlaySCCH );

    this.mnuPopupPlayKC85 = createJMenuItem(
				"KC-Format (HC900, KC85/2..5, KC-BASIC)" );
    mnuPopupPlayAs.add( this.mnuPopupPlayKC85 );

    this.mnuPopupPlayZ9001 = createJMenuItem(
				"KC-Format (KC85/1, KC87, Z9001)" );
    mnuPopupPlayAs.add( this.mnuPopupPlayZ9001 );

    this.mnuPopupPlayZ1013 = createJMenuItem( "Z1013-Format" );
    mnuPopupPlayAs.add( this.mnuPopupPlayZ1013 );

    this.mnuPopupPlayZ1013HS = createJMenuItem( "Z1013-Headersave-Format" );
    mnuPopupPlayAs.add( this.mnuPopupPlayZ1013HS );

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

    this.mnuPopupPathCopy = createJMenuItem(
		"Vollst\u00E4ndiger Datei-/Verzeichnisname kopieren" );
    mnuPopup.add( this.mnuPopupPathCopy );

    this.mnuPopupFileCopy = createJMenuItem( "Kopieren" );
    mnuPopup.add( this.mnuPopupFileCopy );

    this.mnuPopupFilePaste = createJMenuItem( "Einf\u00FCgen" );
    mnuPopup.add( this.mnuPopupFilePaste );
    this.mnuPopup.addSeparator();

    this.mnuPopupCreateDir = createJMenuItem( "Verzeichnis erstellen..." );
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
    DefaultTreeSelectionModel selModel = new DefaultTreeSelectionModel();
    selModel.setSelectionMode(
		TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );

    this.rootNode = new FileNode( null, null, true );
    this.rootNode.refresh(
			null,
			true,
			this.mnuHiddenFiles.isSelected(),
			getFileComparator() );

    this.treeModel = new DefaultTreeModel( this.rootNode );
    this.tree      = new JTree( this.treeModel );
    this.tree.setSelectionModel( selModel );
    this.tree.setEditable( false );
    this.tree.setRootVisible( false );
    this.tree.setScrollsOnExpand( true );
    this.tree.setShowsRootHandles( true );
    this.tree.setCellRenderer( new FileTreeCellRenderer() );
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
      this.table.addKeyListener( this );
      this.table.addMouseListener( this );
      ListSelectionModel lsm = this.table.getSelectionModel();
      if( lsm != null ) {
	lsm.addListSelectionListener( this );
      }
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


    // Zwischenablage
    this.clipboard = null;
    Toolkit tk = getToolkit();
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
      if( this.clipboard != null ) {
	this.clipboard.addFlavorListener( this );
      }
    }


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
    updFilePasteState( true );
  }


  public void fireDirectoryChanged( final File dirFile )
  {
    EventQueue.invokeLater(
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
    EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    showErrorMsgInternal( msg );
		  }
		} );
  }


	/* --- DragGestureListener --- */

  @Override
  public void dragGestureRecognized( DragGestureEvent e )
  {
    Collection<File> files = getSelectedFiles();
    if( files != null ) {
      if( !files.isEmpty() ) {
	try {
	  e.startDrag( null, new FileListSelection( files ) );
	}
	catch( InvalidDnDOperationException ex ) {}
      }
    }
  }


	/* --- FlavorListener --- */

  @Override
  public void flavorsChanged( FlavorEvent e )
  {
    if( e.getSource() == this.clipboard )
      updFilePasteState( false );
  }


	/* --- FocusListener --- */

  @Override
  public void focusGained( FocusEvent e )
  {
    if( !e.isTemporary() ) {
      this.lastActiveFld = e.getComponent();
      updActionButtons();
    }
  }


  @Override
  public void focusLost( FocusEvent e )
  {
    // leer
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    updActionButtons();
  }


	/* --- TreeSelectionListener --- */

  @Override
  public void valueChanged( TreeSelectionEvent e )
  {
    if( e.getSource() == this.tree ) {
      updPreview();
      updActionButtons();
    }
  }


	/* --- TreeWillExpandListener --- */

  @Override
  public void treeWillCollapse( TreeExpansionEvent e )
					throws ExpandVetoException
  {
    // leer
  }


  @Override
  public void treeWillExpand( TreeExpansionEvent e )
					throws ExpandVetoException
  {
    TreePath treePath = e.getPath();
    if( treePath != null ) {
      setWaitCursor( true );
      Object o = treePath.getLastPathComponent();
      if( o != null ) {
	if( o instanceof FileNode ) {
	  refreshNode( (FileNode) o, true );
	}
      }
      setWaitCursor( false );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
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


  @Override
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
	  else if( (src == this.mnuFileConvert)
		   || (src == this.mnuPopupConvert) )
	  {
	    rv = true;
	    doFileConvert();
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
	  else if( (src == this.mnuFilePlayAC1)
		   || (src == this.mnuPopupPlayAC1) )
	  {
	    rv = true;
	    doFilePlayAC1();
	  }
	  else if( (src == this.mnuFilePlayAC1Basic)
		   || (src == this.mnuPopupPlayAC1Basic) )
	  {
	    rv = true;
	    doFilePlayAC1Basic();
	  }
	  else if( (src == this.mnuFilePlaySCCH)
		   || (src == this.mnuPopupPlaySCCH) )
	  {
	    rv = true;
	    doFilePlaySCCH();
	  }
	  else if( (src == this.mnuFilePlayKC85)
		   || (src == this.mnuPopupPlayKC85) )
	  {
	    rv = true;
	    doFilePlayKC( 1 );
	  }
	  else if( (src == this.mnuFilePlayZ1013)
		   || (src == this.mnuPopupPlayZ1013) )
	  {
	    rv = true;
	    doFilePlayZ1013( false );
	  }
	  else if( (src == this.mnuFilePlayZ1013HS)
		   || (src == this.mnuPopupPlayZ1013HS) )
	  {
	    rv = true;
	    doFilePlayZ1013( true );
	  }
	  else if( (src == this.mnuFilePlayZ9001)
		   || (src == this.mnuPopupPlayZ9001) )
	  {
	    rv = true;
	    doFilePlayKC( 0 );
	  }
	  else if( (src == this.mnuFileAudioIn)
		   || (src == this.mnuPopupAudioIn) )
	  {
	    rv = true;
	    doFileAudioIn();
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
	  else if( (src == this.mnuEditPathCopy)
		   || (src == this.mnuPopupPathCopy) )
	  {
	    rv = true;
	    doEditPathCopy();
	  }
	  else if( (src == this.mnuEditFileCopy)
		   || (src == this.mnuPopupFileCopy) )
	  {
	    rv = true;
	    doEditFileCopy();
	  }
	  else if( (src == this.mnuEditFilePaste)
		   || (src == this.mnuPopupFilePaste) )
	  {
	    rv = true;
	    doEditFilePaste();
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


  @Override
  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null )
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
  }


  @Override
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


  @Override
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


  @Override
  public void mousePressed( MouseEvent e )
  {
    if( checkPopup( e ) )
      e.consume();
    else
      super.mousePressed( e );
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    if( checkPopup( e ) )
      e.consume();
    else
      super.mouseReleased( e );
  }


  @Override
  public void windowClosed( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.screenFrm.childFrameClosed( this );
  }


	/* --- Aktionen --- */

  private void doEditFileCopy()
  {
    try {
      if( this.clipboard != null ) {
      Collection<File> files = getSelectedFiles();
	if( files != null ) {
	  if( !files.isEmpty() ) {
	    FileListSelection fls = new FileListSelection( files );
	    this.clipboard.setContents( fls, fls );
	  }
	}
      }
    }
    catch( IllegalStateException ex ) {}
  }


  private void doEditFilePaste()
  {
    if( this.clipboard != null ) {
      File     dstDir   = null;
      FileNode fileNode = getSelectedFileNode();
      if( fileNode != null ) {
	File file = fileNode.getFile();
	if( file != null ) {
	  if( file.isDirectory() ) {
	    dstDir = file;
	  }
	}
      }
      if( (fileNode != null) && (dstDir != null) ) {
	boolean changed = false;
	try {
	  Object o = this.clipboard.getData( DataFlavor.javaFileListFlavor );
	  if( o != null ) {
	    if( o instanceof File ) {
	      changed = pasteFile( dstDir, (File) o );
	    }
	    else if( o instanceof java.util.List ) {
	      for( Object e : (java.util.List) o ) {
		if( e != null ) {
		  if( e instanceof File ) {
		    if( pasteFile( dstDir, (File) e ) ) {
		      changed = true;
		    } else {
		      break;
		    }
		  }
		}
	      }
	    }
	  }
	}
	catch( IllegalStateException ex ) {}
	catch( UnsupportedFlavorException ex ) {}
	catch( IOException ex ) {}
	if( changed ) {
	  fileNode.refresh(
		this.treeModel,
		false,
		this.mnuHiddenFiles.isSelected(),
		getFileComparator() );
	}
      } else {
	BasicDlg.showErrorDlg(
		this,
		"Zielverzeichnis nicht ausgew\u00E4hlt" );
      }
    }
  }


  private void doEditPathCopy()
  {
    try {
      FileNode fileNode = getSelectedFileNode();
      if( fileNode != null ) {
	File file = fileNode.getFile();
	if( file != null ) {
	  String fileName = file.getPath();
	  if( fileName != null ) {
	    if( !fileName.isEmpty() ) {
	      StringSelection ss = new StringSelection( fileName );
	      this.clipboard.setContents( ss, ss );
	    }
	  }
	}
      }
    }
    catch( IllegalStateException ex ) {}
  }


  private void doFileAction( Object src ) throws IOException
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      if( (src == this.table) && !fileNode.isLeaf() ) {
	TreeNode[] path = this.treeModel.getPathToRoot( fileNode );
	if( path != null ) {
	  setWaitCursor( true );
	  this.tree.setSelectionPath( new TreePath( path ) );
	  refreshNode( fileNode, true );
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
	      AudioPlayer.play( this, file );
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
		String fileFmt = fileInfo.getFileFormat();
		if( fileFmt != null ) {
		  if( !fileFmt.isEmpty() && !fileFmt.equals( FileInfo.BIN ) ) {
		    loadable = true;
		  }
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
	if( file.isFile() ) {
	  this.screenFrm.openTextFile( file );
	}
      }
    }
  }


  private void doFileEditHex()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	if( file.isFile() ) {
	  this.screenFrm.openHexEditor( file );
	}
      }
    }
  }


  private void doFileDiffHex()
  {
    java.util.List<File> files = getSelectedFiles();
    if( files != null ) {
      int n = files.size();
      if( n > 0 ) {
	this.screenFrm.addToHexDiff( files );
      }
    }
  }


  private void doFileConvert()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	if( file.isFile() ) {
	  this.screenFrm.openFileConverter( file );
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
	  else if( fileNode.isPlainDiskFile() ) {
	    DiskUtil.unpackPlainDiskFile( this, file );
	  }
	  else if( fileNode.isNonPlainDiskFile() ) {
	    AbstractFloppyDisk disk = DiskUtil.readNonPlainDiskFile(
								this,
								file );
	    if( disk != null ) {
	      if( DiskUtil.checkAndConfirmWarning( this, disk ) ) {
		DiskUtil.unpackDisk( this, file, disk );
	      }
	    }
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
      if( fileNode.isAudioFile() ) {
	AudioPlayer.play( this, fileNode.getFile() );
      }
    }
  }


  private void doFilePlayAC1() throws IOException
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      if( fileNode.isBinFile() ) {
	File file = fileNode.getFile();
	if( file != null ) {
	  String           title = "AC1-Wiedergabe von " + file.getName();
	  ReplyFileHeadDlg dlg   = new ReplyFileHeadDlg(
			this,
			file.getName(),
			"Wiedergeben",
			title,
			ReplyFileHeadDlg.Option.BEGIN_ADDRESS,
			ReplyFileHeadDlg.Option.START_ADDRESS,
			ReplyFileHeadDlg.Option.FILE_NAME_16 );
	  dlg.setVisible( true );
	  if( dlg.wasApproved() ) {
	    int startAddr = dlg.getApprovedStartAddress();
	    if( startAddr < 0 ) {
	      startAddr = 0;
	    }
	    AudioPlayer.play(
		this,
		new AC1AudioDataStream(
			false,
			EmuUtil.readFile( file, 0x10000 ),
			dlg.getApprovedFileName(),
			dlg.getApprovedBeginAddress(),
			startAddr ),
		title + "..." );
	  }
	}
      }
    }
  }


  private void doFilePlayAC1Basic() throws IOException
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      if( fileNode.isBinFile() || fileNode.isHeadersaveFile() ) {
	File file = fileNode.getFile();
	if( file != null ) {
	  byte[] buf  = EmuUtil.readFile( file, 0x10000 );
	  int    len  = buf.length;
	  if( len > 0 ) {
	    String fileName = file.getName();
	    int    offs     = 0;
	    if( len > 32 ) {
	      if( (buf[ 13 ] == (byte) 0xD3)
		  && (buf[ 14 ] == (byte) 0xD3)
		  && (buf[ 15 ] == (byte) 0xD3) )
	      {
		offs += 32;
		len  -= 32;
		String s = EmuUtil.extractSingleAsciiLine( buf, 16, 16 );
		if( s != null ) {
		  fileName = s;
		}
	      }
	    }
	    String title = "AC1-BASIC-Wiedergabe von " + file.getName();
	    ReplyFileHeadDlg dlg = new ReplyFileHeadDlg(
			this,
			fileName,
			"Wiedergeben",
			title,
			ReplyFileHeadDlg.Option.FILE_NAME_6 );
	    dlg.setVisible( true );
	    if( dlg.wasApproved() ) {
	      AudioPlayer.play(
			this,
			new AC1AudioDataStream(
				true,
				buf,
				offs,
				len,
				dlg.getApprovedFileName(),
				-1,
				-1 ),
			title + "..." );
	    }
	  }
	}
      }
    }
  }


  private void doFilePlaySCCH() throws IOException
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      if( fileNode.isBinFile() || fileNode.isHeadersaveFile() ) {
	File file = fileNode.getFile();
	if( file != null ) {
	  byte[] buf = EmuUtil.readFile( file, 0x10000 );
	  int    len = buf.length;
	  if( len > 0 ) {
	    ReplyFileHeadDlg.Option[] options = null;
	    String fileName = file.getName();
	    int    offs     = 0;
	    int    begAddr  = -1;
	    int    endAddr  = -1;
	    int    fType    = -1;
	    if( len > 32 ) {
	      if( (buf[ 13 ] == (byte) 0xD3)
		  && (buf[ 14 ] == (byte) 0xD3)
		  && (buf[ 15 ] == (byte) 0xD3) )
	      {
		offs += 32;
		len  -= 32;
		begAddr = EmuUtil.getWord( buf, 0 );
		endAddr = EmuUtil.getWord( buf, 2 );
		if( (begAddr == 0x60F7) && (buf[ 12 ] == (byte) 'B') ) {
		  fType   = 'B';
		  options = new ReplyFileHeadDlg.Option[] {
			ReplyFileHeadDlg.Option.FILE_NAME_16 };
		} else {
		  options = new ReplyFileHeadDlg.Option[] {
			ReplyFileHeadDlg.Option.FILE_NAME_16,
			ReplyFileHeadDlg.Option.SCCH_FILE_TYPE };
		}
		String s = EmuUtil.extractSingleAsciiLine( buf, 16, 16 );
		if( s != null ) {
		  fileName = s;
		}
	      }
	    }
	    if( options == null ) {
	      options = new ReplyFileHeadDlg.Option[] {
				ReplyFileHeadDlg.Option.BEGIN_ADDRESS,
				ReplyFileHeadDlg.Option.END_ADDRESS,
				ReplyFileHeadDlg.Option.FILE_NAME_16,
				ReplyFileHeadDlg.Option.SCCH_FILE_TYPE };
	    }
	    String title = "AC1/LLC2-TurboSave-Wiedergabe von "
							+ file.getName();
	    ReplyFileHeadDlg dlg = new ReplyFileHeadDlg(
						this,
						fileName,
						"Wiedergeben",
						title,
						options );
	    dlg.setVisible( true );
	    if( dlg.wasApproved() ) {
	      if( begAddr < 0 ) {
		begAddr = dlg.getApprovedBeginAddress();
	      }
	      if( endAddr < 0 ) {
		endAddr = dlg.getApprovedEndAddress();
		if( endAddr < 0 ) {
		  endAddr = begAddr + len - 1;
		}
	      }
	      if( fType < 0 ) {
		fType = dlg.getApprovedSCCHFileType();
	      }
	      AudioPlayer.play(
			this,
			new SCCHAudioDataStream(
				buf,
				offs,
				len,
				dlg.getApprovedFileName(),
				(char) fType,
				begAddr,
				endAddr ),
			title + "..." );
	    }
	  }
	}
      }
    }
  }


  private void doFilePlayKC( int firstBlkNum ) throws IOException
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      File file = fileNode.getFile();
      if( file != null ) {
	String title = "KC-Wiedergabe von " + file.getName() + "...";
	if( fileNode.isKCBasicHeadFile() ) {
	  AudioPlayer.play(
		this,
		new KCAudioDataStream(
			false,
			1,
			EmuUtil.readFile( file, 0x10000 ) ),
		title );
	}
	else if( fileNode.isKCBasicFile() ) {
	  byte[] fileBytes = EmuUtil.readFile( file, 0x10000 );
	  if( fileBytes != null ) {
	    if( fileBytes.length > 0 ) {
	      ReplyFileHeadDlg dlg = new ReplyFileHeadDlg(
			this,
			file.getName(),
			"Wiedergeben",
			title,
			ReplyFileHeadDlg.Option.FILE_NAME_8 );
	      dlg.setVisible( true );
	      if( dlg.wasApproved() ) {
		String name  = dlg.getApprovedFileName();
		byte[] buf   = new byte[ fileBytes.length + 11 ];
		int    dst   = 0;
		buf[ dst++ ] = (byte) 0xD3;
		buf[ dst++ ] = (byte) 0xD3;
		buf[ dst++ ] = (byte) 0xD3;
		if( name != null ) {
		  int len = name.length();
		  int src = 0;
		  while( (dst < 11) && (src < len) ) {
		    buf[ dst++ ] = (byte) (name.charAt( src++ ) & 0x7F);
		  }
		}
		while( dst < 11 ) {
		  buf[ dst++ ] = (byte) 0x20;
		}
		AudioPlayer.play(
			this,
			new KCAudioDataStream( false, 1, buf ),
			title );
	      }
	    }
	  }
	}
	else if( fileNode.isKCSysFile() ) {
	  AudioPlayer.play(
		this,
		new KCAudioDataStream(
			false,
			firstBlkNum,
			EmuUtil.readFile( file, 0x10000 ) ),
		title );
	}
	else if( (fileNode.isKC85TapFile() && (firstBlkNum == 1))
		 || (fileNode.isZ9001TapFile() && (firstBlkNum == 0)) )
	{
	  AudioPlayer.play(
		this,
		new KCAudioDataStream(
			true,
			0,
			EmuUtil.readFile( file, 0x10110 ) ),
		title );
	}
      }
    }
  }


  private void doFilePlayZ1013( boolean headersave ) throws IOException
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      if( fileNode.isBinFile()
	  || (headersave && fileNode.isHeadersaveFile()) )
      {
	File file = fileNode.getFile();
	if( file != null ) {
	  AudioPlayer.play(
		this,
		new Z1013AudioDataStream(
				headersave,
				EmuUtil.readFile( file, 0x10020 ) ),
		String.format(
			"Z1013%s-Wiedergabe von %s...",
			headersave ? "-Headersave" : "",
			file.getName() ) );
	}
      }
    }
  }


  private void doFileAudioIn()
  {
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      if( fileNode.isAudioFile()
	  || fileNode.isKC85TapFile()
	  || fileNode.isZ9001TapFile() )
      {
	File file = fileNode.getFile();
	if( file != null ) {
	  this.screenFrm.openAudioInFile( file );
	}
      }
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
	    refreshNode( (FileNode) parent, false );
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
    if( this.lastActiveFld != null ) {
      if( this.lastActiveFld == this.table ) {
	TableModel tm = this.table.getModel();
	if( tm != null ) {
	  if( tm instanceof FileTableModel ) {
	    int[] rows = this.table.getSelectedRows();
	    if( rows != null ) {
	      if( rows.length > 0 ) {
		Arrays.sort( rows );
		int           firstRow = rows[ 0 ];
		Set<FileNode> parents  = new HashSet<FileNode>();
		File[]        files    = new File[ rows.length ];
		Arrays.fill( files, null );
		for( int i = 0; i < rows.length; i++ ) {
		  int mRow = this.table.convertRowIndexToModel( rows[ i ] );
		  if( mRow >= 0 ) {
		    FileEntry entry = ((FileTableModel) tm).getRow( mRow );
		    if( entry != null ) {
		      files[ i ] = entry.getFile();
		      if( entry instanceof ExtendedFileEntry ) {
			FileNode fileNode
				= ((ExtendedFileEntry) entry).getFileNode();
			if( fileNode != null ) {
			  TreeNode parent = fileNode.getParent();
			  if( parent != null ) {
			    if( parent instanceof FileNode ) {
			      parents.add( (FileNode) parent );
			    }
			  }
			  files[ i ] = fileNode.getFile();
			}
		      }
		    }
		  }
		}
		if( EmuUtil.deleteFiles( this, files ) ) {
		  refreshNodes( parents );
		  updPreview();
		  this.table.clearSelection();
		  updActionButtons();
		  if( firstRow >= 0 ) {
		    final int row = firstRow;
		    EventQueue.invokeLater(
				new Runnable()
				{
				  public void run()
				  {
				    selectTableRow( row );
				  }
				} );
		  }
		}
	      }
	    }
	  }
	}
      } else {
	Collection<FileNode> nodes = getSelectedTreeFileNodes();
	if( nodes != null ) {
	  int n = nodes.size();
	  if( n > 0 ) {
	    int      firstSelSubIdx = -1;
	    TreePath firstSelParent = null;
	    TreePath firstSelPath   = this.tree.getSelectionPath();
	    if( firstSelPath != null ) {
	      firstSelParent = firstSelPath.getParentPath();
	      if( firstSelParent != null ) {
		Object parent = firstSelParent.getLastPathComponent();
		Object child  = firstSelPath.getLastPathComponent();
		if( (parent != null) && (child != null) ) {
		  if( parent instanceof FileNode ) {
		    if( !nodes.contains( parent ) ) {
		      firstSelSubIdx = this.treeModel.getIndexOfChild(
								parent,
								child );
		    }
		  }
		}
	      }
	    }
	    Set<FileNode>    parents = new HashSet<FileNode>();
	    Collection<File> files   = new ArrayList<File>( n );
	    for( FileNode node : nodes ) {
	      File file = node.getFile();
	      if( file != null ) {
		Object parent = node.getParent();
		if( parent != null ) {
		  if( parent instanceof FileNode ) {
		    parents.add( (FileNode) parent );
		  }
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
		refreshNodes( parents );
		this.tree.clearSelection();
		if( (firstSelParent != null) && (firstSelSubIdx >= 0) ) {
		  Object o = firstSelParent.getLastPathComponent();
		  if( o != null ) {
		    if( o instanceof TreeNode ) {
		      if( firstSelSubIdx < ((TreeNode) o).getChildCount() ) {
			TreeNode child = ((TreeNode) o).getChildAt(
							firstSelSubIdx );
			if( child != null ) {
			  this.tree.addSelectionPath(
				firstSelParent.pathByAddingChild( child ) );
			}
		      }
		    }
		  }
		}
		updPreview();
		updActionButtons();
	      }
	    }
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
    FileNode fileNode = getSelectedFileNode();
    if( fileNode != null ) {
      refreshNode( fileNode, false );
    } else {
      refreshNode( this.rootNode, false );
    }
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
	  if( file != null ) {
	    rv.add( file );
	  }
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


  private boolean pasteFile( File dstDir, File srcFile )
  {
    boolean done = false;
    if( srcFile.isFile() ) {
      String orgName = srcFile.getName();
      if( orgName != null ) {
	if( !orgName.isEmpty() ) {
	  String  fileName = orgName;
	  File    dstFile  = null;
	  while( fileName != null ) {
	    dstFile = new File( dstDir, fileName );
	    if( dstFile.exists() ) {
	      dstFile = null;

	      String text = JOptionPane.showInputDialog(
			this,
			"Einf\u00FCgen von " + orgName + ":\n"
				+ "Es existiert bereits eine Datei oder"
				+ " ein Verzeichnis mit diesem Namen.\n"
				+ "Geben Sie deshalb bitte einen anderen"
				+ " Dateinamen ein.\n\n"
				+ "Name der Datei:",
			"Einf\u00FCgen einer Datei",
			JOptionPane.WARNING_MESSAGE );
	      if( text != null ) {
		if( text.isEmpty() ) {
		  fileName = orgName;
		} else {
		  fileName = text;
		}
	      } else {
		fileName = null;
	      }
	    } else {
	      break;
	    }
	  }
	  if( dstFile != null ) {
	    try {
	      done = EmuUtil.copyFile( srcFile, dstFile );
	    }
	    catch( Exception ex ) {
	      BasicDlg.showErrorDlg( this, ex );
	    }
	  }
	}
      }
    } else {
      if( srcFile.isDirectory() ) {
	BasicDlg.showErrorDlg(
		this,
		"Das Einf\u00FCgen von Verzeichnissen\n"
			+ "wird nicht unterst\u00FCtzt." );
      } else {
	BasicDlg.showErrorDlg(
		this,
		"Nicht regul\u00E4re Dateien k\u00F6nnen nicht\n"
			+ "eingef\u00FCgt werden." );
      }
    }
    return done;
  }


  private void refreshNode( FileNode node, boolean forceLoadChildren )
  {
    if( node != null ) {
      node.refresh(
		this.treeModel,
		forceLoadChildren,
		this.mnuHiddenFiles.isSelected(),
		getFileComparator() );
    }
  }


  private void refreshNodes( Collection<FileNode> nodes )
  {
    if( nodes != null ) {
      for( FileNode node : nodes ) {
	refreshNode( node, false );
      }
    }
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


  private void selectTableRow( int row )
  {
    if( (row >= 0) && (row < this.table.getRowCount()) ) {
      this.table.addRowSelectionInterval( row, row );
    }
  }


  private void showErrorMsgInternal( String msg )
  {
    BasicDlg.showErrorDlg( this, msg );
  }


  private void updActionButtons()
  {
    int                  nNodes    = 0;
    int                  nFiles    = 0;
    int                  nDirs     = 0;
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
	  else if( tmpFile.isDirectory() ) {
	    nDirs++;
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
    boolean stateOneDir    = ((nDirs == 1) && (nDirs == nNodes));
    boolean stateOneFile   = ((nFiles == 1) && (nFiles == nNodes));
    boolean stateFilesOnly = ((nFiles > 0) && (nFiles == nNodes));
    boolean stateDirsFiles = ((nNodes > 0) && ((nFiles + nDirs) == nNodes));
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

      boolean isHS          = fileNode.isHeadersaveFile();
      boolean isBin         = fileNode.isBinFile();
      boolean isKCBasicHead = fileNode.isKCBasicHeadFile();
      boolean isKCBasic     = fileNode.isKCBasicFile();
      boolean isKCSys       = fileNode.isKCSysFile();
      boolean isKC85Tap     = fileNode.isKC85TapFile();
      boolean isZ9001Tap    = fileNode.isZ9001TapFile();
      boolean isBasic60F7   = false;
      if( isHS ) {
	FileInfo info = fileNode.getFileInfo();
	if( info != null ) {
	  if( (info.getBegAddr() == 0x60F7) && (info.getFileType() == 'B') ) {
	    isBasic60F7 = true;
	  }
	}
      }

      this.mnuFilePlayAC1.setEnabled( isBin );
      this.mnuPopupPlayAC1.setEnabled( isBin );

      this.mnuFilePlayAC1Basic.setEnabled( isBasic60F7 );
      this.mnuPopupPlayAC1Basic.setEnabled( isBasic60F7 );

      this.mnuFilePlaySCCH.setEnabled( isBin || isBasic60F7 );
      this.mnuPopupPlaySCCH.setEnabled( isBin || isBasic60F7 );

      this.mnuFilePlayKC85.setEnabled(
			isKCBasicHead || isKCBasic || isKCSys || isKC85Tap );
      this.mnuPopupPlayKC85.setEnabled(
			isKCBasicHead || isKCBasic || isKCSys || isKC85Tap );

      this.mnuFilePlayZ9001.setEnabled( isKCSys || isZ9001Tap );
      this.mnuPopupPlayZ9001.setEnabled( isKCSys || isZ9001Tap );

      this.mnuFilePlayZ1013.setEnabled( isBin );
      this.mnuPopupPlayZ1013.setEnabled( isBin );

      this.mnuFilePlayZ1013HS.setEnabled( isHS );
      this.mnuPopupPlayZ1013HS.setEnabled( isHS );

      this.mnuFileAudioIn.setEnabled( isAudio || isKC85Tap || isZ9001Tap );
      this.mnuPopupAudioIn.setEnabled( isAudio || isKC85Tap || isZ9001Tap );

      boolean isText = (stateOneFile && !isImage && !isAudio);
      this.mnuFileEditText.setEnabled( isText );
      this.mnuPopupEditText.setEnabled( isText );
      this.btnEditText.setEnabled( isText );

      boolean isUnpackable = (fileNode.isArchiveFile()
					|| fileNode.isCompressedFile()
					|| fileNode.isNonPlainDiskFile()
					|| fileNode.isPlainDiskFile());
      this.mnuFileUnpack.setEnabled( isUnpackable );
      this.mnuPopupUnpack.setEnabled( isUnpackable );

      this.mnuFileCreateDir.setEnabled( stateOneDir );
      this.mnuPopupCreateDir.setEnabled( stateOneDir );

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

      this.mnuFilePlayAC1.setEnabled( false );
      this.mnuPopupPlayAC1.setEnabled( false );

      this.mnuFilePlayAC1Basic.setEnabled( false );
      this.mnuPopupPlayAC1Basic.setEnabled( false );

      this.mnuFilePlaySCCH.setEnabled( false );
      this.mnuPopupPlaySCCH.setEnabled( false );

      this.mnuFilePlayKC85.setEnabled( false );
      this.mnuPopupPlayKC85.setEnabled( false );

      this.mnuFilePlayZ9001.setEnabled( false );
      this.mnuPopupPlayZ9001.setEnabled( false );

      this.mnuFilePlayZ1013.setEnabled( false );
      this.mnuPopupPlayZ1013.setEnabled( false );

      this.mnuFilePlayZ1013HS.setEnabled( false );
      this.mnuPopupPlayZ1013HS.setEnabled( false );

      this.mnuFileAudioIn.setEnabled( false );
      this.mnuPopupAudioIn.setEnabled( false );

      this.mnuFileEditText.setEnabled( false );
      this.mnuPopupEditText.setEnabled( false );
      this.btnEditText.setEnabled( false );

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

    this.mnuFileConvert.setEnabled( stateOneFile );
    this.mnuPopupConvert.setEnabled( stateOneFile );

    this.mnuFileChecksum.setEnabled( stateFilesOnly );
    this.mnuPopupChecksum.setEnabled( stateFilesOnly );

    this.mnuFilePackGZip.setEnabled( stateOneFile );
    this.mnuPopupPackGZip.setEnabled( stateOneFile );

    this.mnuFilePackTar.setEnabled( stateDirsFiles );
    this.mnuPopupPackTar.setEnabled( stateDirsFiles );

    this.mnuFilePackTgz.setEnabled( stateDirsFiles );
    this.mnuPopupPackTgz.setEnabled( stateDirsFiles );

    this.mnuFilePackZip.setEnabled( stateDirsFiles );
    this.mnuPopupPackZip.setEnabled( stateDirsFiles );

    this.mnuFileRAMFloppy1Load.setEnabled( stateOneFile );
    this.mnuPopupRAMFloppy1Load.setEnabled( stateOneFile );

    this.mnuFileRAMFloppy2Load.setEnabled( stateOneFile );
    this.mnuPopupRAMFloppy2Load.setEnabled( stateOneFile );

    this.mnuFileLastModified.setEnabled( stateEntries );
    this.mnuPopupLastModified.setEnabled( stateEntries );

    this.mnuFileDelete.setEnabled( stateDirsFiles );
    this.mnuPopupDelete.setEnabled( stateDirsFiles );

    this.mnuEditPathCopy.setEnabled( stateOneDir || stateOneFile );
    this.mnuPopupPathCopy.setEnabled( stateOneDir || stateOneFile );

    this.mnuEditFileCopy.setEnabled( stateEntries );
    this.mnuPopupFileCopy.setEnabled( stateEntries );

    this.mnuEditFilePaste.setEnabled( stateOneDir && this.filePasteState );
    this.mnuPopupFilePaste.setEnabled( stateOneDir && this.filePasteState );
  }


  private void updFilePasteState( boolean force )
  {
    boolean state = false;
    try {
      if( this.clipboard != null ) {
	state = this.clipboard.isDataFlavorAvailable(
					DataFlavor.javaFileListFlavor );
      }
    }
    catch( IllegalStateException ex ) {}
    this.filePasteState = state;
    this.mnuEditFilePaste.setEnabled( state );
    this.mnuPopupFilePaste.setEnabled( state );
  }


  private void updPreview()
  {
    FileNode fileNode = null;
    if( this.tree.getSelectionCount() == 1 ) {
      Object o = this.tree.getLastSelectedPathComponent();
      if( o != null ) {
	if( o instanceof FileNode ) {
	  fileNode = (FileNode) o;
	  refreshNode( fileNode, true );
	}
      }
    }
    this.filePreviewFld.setFileNode(
				fileNode,
				getPreviewMaxFileSize(),
				this.mnuSortCaseSensitive.isSelected() );
  }
}

