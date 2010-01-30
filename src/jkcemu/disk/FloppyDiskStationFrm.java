/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer Diskettenstation
 */

package jkcemu.disk;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.system.*;


public class FloppyDiskStationFrm
			extends BasicFrm
			implements DropTargetListener
{
  private static final String DRIVE_EMPTY_TEXT = "--- leer ---";
  private static final int    MAX_DRIVE_COUNT  = 4;

  private ScreenFrm         screenFrm;
  private EmuSys            emuSys;
  private FloppyDiskInfo[]  suitableDisks;
  private FloppyDiskInfo[]  etcDisks;
  private FloppyDiskDrive[] drives;
  private FloppyDiskFormat  lastFmt;
  private int               lastSysTracks;
  private int               lastBlockSize;
  private boolean           lastBlockNum16Bit;
  private boolean           lastAutoRefresh;
  private int               lastDirBlocks;
  private int               driveCnt;
  private JTextArea[]       textAreas;
  private JTabbedPane       tabbedPane;
  private JButton           btnOpen;
  private JPopupMenu        mnuPopup;
  private JMenuItem         mnuPopupRemove;


  public FloppyDiskStationFrm( ScreenFrm screenFrm )
  {
    this.screenFrm         = screenFrm;
    this.emuSys            = null;
    this.suitableDisks     = null;
    this.etcDisks          = null;
    this.lastFmt           = null;
    this.lastSysTracks     = -1;
    this.lastBlockSize     = -1;
    this.lastDirBlocks     = -1;
    this.lastBlockNum16Bit = false;
    this.lastAutoRefresh   = false;
    setTitle( "JKCEMU Diskettenstation" );
    Main.updIcon( this );


    // Laufwerke anlegen
    Font font      = new Font( "SansSerif", Font.PLAIN, 12 );
    this.textAreas = new JTextArea[ MAX_DRIVE_COUNT ];
    this.drives    = new FloppyDiskDrive[ MAX_DRIVE_COUNT ];
    for( int i = 0; i < MAX_DRIVE_COUNT; i++ ) {
      JTextArea textArea = new JTextArea( 5, 50 );
      textArea.setBorder( BorderFactory.createLoweredBevelBorder() );
      textArea.setFont( font );
      textArea.setEditable( false );
      textArea.setText( DRIVE_EMPTY_TEXT );
      (new DropTarget( textArea, this )).setActive( true );
      this.textAreas[ i ] = textArea;
      this.drives[ i ]    = new FloppyDiskDrive();
    }


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuFile.add( createJMenuItem( "Schlie\u00DFen", "close" ) );
    mnuBar.add( mnuFile );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuHelp.add( createJMenuItem( "Hilfe...", "help" ) );
    mnuBar.add( mnuHelp );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 1.0,
						GridBagConstraints.SOUTHEAST,
						GridBagConstraints.BOTH,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, gbc );

    this.btnOpen = createImageButton(
				"/images/disk/eject.png",
				"\u00D6ffnen/Laden" );
    gbc.fill     = GridBagConstraints.NONE;
    gbc.weightx  = 0.0;
    gbc.weighty  = 0.0;
    gbc.gridy++;
    add( this.btnOpen, gbc );


    // Fenstergroesse
    this.driveCnt = 1;	// erstmal ein Tab anlegen fuer die Fenstergroesse
    rebuildTabbedPane();
    setLocationByPlatform( true );
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
    }
    setResizable( true );
    for( int i = 0; i < this.textAreas.length; i++ ) {
      this.textAreas[ i ].setPreferredSize( new Dimension( 1, 1 ) );
    }


    // Sonstiges
    Runtime.getRuntime().addShutdownHook(
		new Thread()
		{
		  public void run()
		  {
		    removeAllDisks();
		  }
		} );
  }


  public void fireDiskFormatChanged( final AbstractFloppyDisk disk )
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    diskFormatChanged( disk );
		  }
		} );
  }


  public FloppyDiskDrive getDrive( int driveNum )
  {
    return (driveNum >= 0)
	   && (driveNum < this.drives.length)
	   && (driveNum < this.driveCnt) ?
				this.drives[ driveNum ]
				: null;
  }


  public void openDisks( Properties props )
  {
    for( int i = 0; i < MAX_DRIVE_COUNT; i++ )
      openDisk( i, props );
  }


  public void setDriveCount( int n )
  {
    if( n != this.driveCnt ) {
      this.driveCnt = n;
      SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    rebuildTabbedPane();
		  }
		} );
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
    File file = EmuUtil.fileDrop( this, e );
    if( file != null ) {
      DropTargetContext context = e.getDropTargetContext();
      if( context != null ) {
	Component c = context.getComponent();
	if( c != null ) {
	  for( int i = 0;
	       (i < this.textAreas.length) && (i < this.drives.length);
	       i++ )
	  {
	    if( c == this.textAreas[ i ] ) {
	      openFile( i, file, null, true, null, null );
	      break;
	    }
	  }
	}
      }
    }
  }


  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- ueberschriebene Methoden --- */

  public boolean applySettings( Properties props, boolean resizable )
  {
    EmuSys emuSys = null;
    EmuThread emuThread = this.screenFrm.getEmuThread();
    if( emuThread != null ) {
      emuSys = emuThread.getNextEmuSys();
      if( emuSys == null ) {
	emuSys = emuThread.getEmuSys();
      }
    }
    boolean          differs       = true;
    boolean          hasDefaultFmt = false;
    FloppyDiskInfo[] suitableDisks = null;
    if( emuSys != null ) {
      hasDefaultFmt = (emuSys.getDefaultFloppyDiskFormat() != null);
      suitableDisks = emuSys.getSuitableFloppyDisks();
    }
    if( (suitableDisks != null) && (this.suitableDisks != null) ) {
      if( (suitableDisks.length > 0)
	  && (this.suitableDisks.length > 0) )
      {
	if( Arrays.equals( suitableDisks, this.suitableDisks ) ) {
	  differs = false;
	}
      }
    }
    this.emuSys        = emuSys;
    this.suitableDisks = suitableDisks;
    if( differs ) {
      this.lastAutoRefresh = false;
      if( hasDefaultFmt ) {
	this.lastFmt       = null;
	this.lastSysTracks = -1;
	this.lastBlockSize = -1;
	this.lastDirBlocks = -1;
      }
      this.etcDisks = null;
      try {
	Set<FloppyDiskInfo> etcDisks = new TreeSet<FloppyDiskInfo>();
	addFloppyDiskInfo( etcDisks, KC85.getAvailableFloppyDisks() );
	addFloppyDiskInfo( etcDisks, Z9001.getAvailableFloppyDisks() );
	if( this.suitableDisks != null ) {
	  for( int i = 0; i < this.suitableDisks.length; i++ ) {
	    etcDisks.remove( this.suitableDisks[ i ] );
	  }
	}
	int n = etcDisks.size();
	if( n > 0 ) {
	  this.etcDisks = etcDisks.toArray( new FloppyDiskInfo[ n ] );
	}
      }
      catch( Exception ex ) {}
      createPopupMenu();
    }
    return super.applySettings( props, resizable );
  }


  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnOpen ) {
	rv = true;
	showPopup();
      }
      else if( src == this.mnuPopupRemove ) {
	rv = true;
	doDiskRemove();
      }
      if( !rv && (e instanceof ActionEvent) ) {
	String cmd = ((ActionEvent) e).getActionCommand();
	if( cmd != null ) {
	  if( cmd.equals( "close" ) ) {
	    rv = true;
	    doClose();
	  }
	  else if( cmd.equals( "help" ) ) {
	    rv = true;
	    this.screenFrm.showHelp( "/help/floppydisk.htm" );
	  }
	  else if( cmd.startsWith( "disk.open.suitable." )
		   && (cmd.length() > 19) )
	  {
	    rv = true;
	    doDiskOpen( cmd.substring( 19 ), this.suitableDisks );
	  }
	  else if( cmd.startsWith( "disk.open.etc." )
		   && (cmd.length() > 14) )
	  {
	    rv = true;
	    doDiskOpen( cmd.substring( 14 ), this.etcDisks );
	  }
	  else if( cmd.equals( "drive.open" ) ) {
	    rv = true;
	    doDriveOpen();
	  }
	  else if( cmd.equals( "directory.open" ) ) {
	    rv = true;
	    doDirectoryOpen();
	  }
	  else if( cmd.equals( "file.anadisk.new" ) ) {
	    rv = true;
	    doFileAnadiskNew();
	  }
	  else if( cmd.equals( "file.plain.new" ) ) {
	    rv = true;
	    doFilePlainNew();
	  }
	  else if( cmd.equals( "file.open" ) ) {
	    rv = true;
	    doFileOpen();
	  }
	}
      }
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
    super.putSettingsTo( props );
    if( props != null ) {
      try {
	Set<String> propNames = props.stringPropertyNames();
	if( propNames != null ) {
	  int n = propNames.size();
	  if( n > 0 ) {
	    String[] a = propNames.toArray( new String[ n ] );
	    if( a != null ) {
	      for( int i = 0; i < a.length; i++ ) {
		if( a[ i ].startsWith( "jkcemu.floppydisk." ) ) {
		  props.remove( a[ i ] );
		}
	      }
	    }
	  }
	}
      }
      catch( ArrayStoreException ex ) {}
      for( int i = 0; i < this.drives.length; i++ ) {
	this.drives[ i ].putSettingsTo(
			props,
			String.format( "jkcemu.floppydisk.%d.", i ) );
      }
    }
  }


	/* --- Aktionen --- */

  private void doDirectoryOpen()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      File file = DirSelectDlg.selectDirectory( this );
      if( file != null ) {
	if( file.isDirectory() ) {
	  int              sysTracks     = -1;
	  int              blockSize     = -1;
	  boolean          blockNum16Bit = false;
	  int              dirBlocks     = -1;
	  FloppyDiskFormat defaultFmt    = this.lastFmt;
	  if( lastFmt != null ) {
	    sysTracks     = this.lastSysTracks;
	    blockSize     = this.lastBlockSize;
	    blockNum16Bit = this.lastBlockNum16Bit;
	    dirBlocks     = this.lastDirBlocks;
	  } else {
	    if( this.emuSys != null ) {
	      defaultFmt    = this.emuSys.getDefaultFloppyDiskFormat();
	      sysTracks     = this.emuSys.getDefaultFloppyDiskSystemTracks();
	      blockSize     = this.emuSys.getDefaultFloppyDiskBlockSize();
	      blockNum16Bit = this.emuSys.getDefaultFloppyDiskBlockNum16Bit();
	      dirBlocks     = this.emuSys.getDefaultFloppyDiskDirBlocks();
	    }
	  }
	  FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			this,
			defaultFmt,
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT,
			FloppyDiskFormatDlg.Flag.SYSTEM_TRACKS,
			FloppyDiskFormatDlg.Flag.BLOCK_SIZE,
			FloppyDiskFormatDlg.Flag.BLOCK_NUM_SIZE,
			FloppyDiskFormatDlg.Flag.DIR_BLOCKS,
			FloppyDiskFormatDlg.Flag.AUTO_REFRESH );
	  if( sysTracks >= 0 ) {
	    dlg.setSystemTracks( sysTracks );
	  }
	  if( blockSize > 0 ) {
	    dlg.setBlockSize( blockSize );
	  }
	  dlg.setBlockNum16Bit( blockNum16Bit );
	  if( dirBlocks > 0 ) {
	    dlg.setDirBlocks( dirBlocks );
	  }
	  dlg.setAutoRefresh( this.lastAutoRefresh );
	  dlg.setVisible( true );
	  FloppyDiskFormat fmt = dlg.getFormat();
	  if( fmt != null ) {
	    this.lastFmt           = fmt;
	    this.lastSysTracks     = dlg.getSystemTracks();
	    this.lastBlockSize     = dlg.getBlockSize();
	    this.lastBlockNum16Bit = dlg.getBlockNum16Bit();
	    this.lastDirBlocks     = dlg.getDirBlocks();
	    this.lastAutoRefresh   = dlg.getAutoRefresh();
	    setDisk(
		idx,
		"Verzeichnis: " + file.getPath(),
		new DirectoryFloppyDisk(
				this,
				fmt.getSides(),
				fmt.getCylinders(),
				fmt.getSectorsPerCylinder(),
				fmt.getSectorSize(),
				this.lastSysTracks,
				this.lastBlockSize,
				this.lastBlockNum16Bit,
				this.lastDirBlocks,
				file,
				this.lastAutoRefresh ),
				null );
	  }
	}
      }
    }
  }


  private void doDiskOpen( String srcIdxText, FloppyDiskInfo[] disks )
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      if( (srcIdxText != null) && (disks != null) ) {
	if( srcIdxText.length() > 0 ) {
	  try {
	    int srcIdx = Integer.parseInt( srcIdxText );
	    if( (srcIdx >= 0) && (srcIdx < disks.length) ) {
	      openDisk( idx, disks[ srcIdx ], Boolean.FALSE );
	    }
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
  }


  private void doDriveOpen()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      boolean        readOnly = (File.separatorChar != '/');
      DriveSelectDlg dlg      = new DriveSelectDlg( this, !readOnly );
      dlg.setVisible( true );
      String fileName = dlg.getSelectedDriveFileName();
      if( fileName != null ) {
	openDrive(
		idx,
		fileName,
		readOnly || dlg.isReadOnlySelected(),
		true,
		null,
		null );
      }
    }
  }


  private void doFileOpen()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      File file = EmuUtil.showFileOpenDlg(
			this,
			"Diskettenabbilddatei \u00F6ffnen",
			Main.getLastPathFile( "disk" ),
			EmuUtil.getPlainDiskFileFilter(),
			EmuUtil.getAnadiskFileFilter(),
			EmuUtil.getTelediskFileFilter() );
      if( file != null ) {
	openFile( idx, file, null, true, null, null );
      }
    }
  }


  private void doFileAnadiskNew()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      File file = EmuUtil.showFileSaveDlg(
			this,
			"Neue Anadisk-Datei anlegen",
			Main.getLastPathFile( "disk" ),
			EmuUtil.getAnadiskFileFilter() );
      if( file != null ) {
	if( DiskUtil.checkFileExt( this, file, DiskUtil.anadiskFileExt ) ) {
	  if( confirmNewFileNotFormatted() ) {
	    try {
	      setDisk(
		idx, 
		"Anadisk-Datei: " + file.getPath(),
		AnadiskFloppyDisk.newFile( this, file ),
		null );
	      Main.setLastFile( file, "disk" );
	    }
	    catch( IOException ex ) {
	      showError( ex );
	    }
	  }
	}
      }
    }
  }


  private void doFilePlainNew()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      File file = EmuUtil.showFileSaveDlg(
			this,
			"Einfache Abbilddatei anlegen",
			Main.getLastPathFile( "disk" ),
			EmuUtil.getPlainDiskFileFilter() );
      if( file != null ) {
	if( DiskUtil.checkFileExt( this, file, DiskUtil.plainDiskFileExt ) ) {
	  if( confirmNewFileNotFormatted() ) {
	    try {
	      setDisk(
		idx, 
		"Einfache Abbilddatei: " + file.getPath(),
		PlainFileFloppyDisk.newFile( this, file ),
		null );
	      Main.setLastFile( file, "disk" );
	    }
	    catch( IOException ex ) {
	      showError( ex );
	    }
	  }
	}
      }
    }
  }


  private void doDiskRemove()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      if( this.drives[ idx ].getDisk() != null ) {
	if( EmuUtil.parseBoolean(
		Main.getProperty( "jkcemu.confirm.disk.remove" ),
		true ) )
	{
	  removeDisk( idx );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private void addFloppyDiskInfo(
			Collection<FloppyDiskInfo> dst,
			FloppyDiskInfo[]           srcAry )
  {
    if( srcAry != null ) {
      for( int i = 0; i < srcAry.length; i++ ) {
	dst.add( srcAry[ i ] );
      }
    }
  }


  private Boolean askOpenFileReadOnly()
  {
    Boolean  rv      = null;
    String[] options = {
		"Nur Lesen",
		"Lesen & Schreiben",
		"Abbrechen" };

    switch( BasicDlg.showOptionDlg(
			this,
			"Soll die Datei nur zum Lesen oder auch zum\n"
				+ "Lesen und Schreiben ge\u00F6ffnet werden?",
			"Abfrage Schreibschutz",
			options ) )
    {
      case 0:
	rv = Boolean.TRUE;
	break;
      case 1:
	rv = Boolean.FALSE;
	break;
    }
    return rv;
  }


  private boolean confirmNewFileNotFormatted()
  {
    boolean  rv      = false;
    String[] options = { "Weiter", "Abbrechen" };
    JOptionPane pane = new JOptionPane(
	"Es wird jetzt eine Datei ohne Inhalt angelegt.\n"
		+ "Vergessen Sie bitte nicht, die emulierte Diskette\n"
		+ "vom emulierten System aus zu formatieren,\n"
		+ "damit die neue Datei einen Inhalt bekommt\n"
		+ "und somit genutzt werden kann.",
	JOptionPane.INFORMATION_MESSAGE );
    pane.setOptions( options );
    pane.setWantsInput( false );
    pane.createDialog( this, "Hinweis" ).setVisible( true );
    Object value = pane.getValue();
    if( value != null ) {
      if( value.equals( options[ 0 ] ) ) {
	rv = true;
      }
    }
    return rv;
  }


  private String createDiskInfoText(
			AbstractFloppyDisk disk,
			boolean            skipOddCyls )
  {
    StringBuilder buf       = new StringBuilder( 256 );
    String        mediaText = disk.getMediaText();
    if( mediaText != null ) {
      if( !mediaText.isEmpty() ) {
	buf.append( mediaText );
	buf.append( (char) '\n' );
      }
    }
    buf.append( disk.getFormatText() );
    String remark = disk.getRemark();
    if( remark != null ) {
      remark = remark.trim();
      if( !remark.isEmpty() ) {
	buf.append( "\n\n" );
	buf.append( remark );
      }
    }
    if( skipOddCyls ) {
      buf.append( "\n\nEmulation einer SD-Diskette in einem DD-Laufwerk"
		+ "\n(Umrechnung der Spurnummer)" );
    }
    return buf.toString();
  }


  private JButton createOpenButton()
  {
    return createImageButton( "/images/disk/eject.png", "\u00D6ffnen/Laden" );
  }


  private void createPopupMenu()
  {
    this.mnuPopup = new JPopupMenu();
    this.mnuPopup.add( createJMenuItem(
				"Diskette \u00F6ffnen...",
				"drive.open" ) );
    this.mnuPopup.add( createJMenuItem(
				"Diskettenabbilddatei \u00F6ffnen...",
				"file.open" ) );
    this.mnuPopup.add( createJMenuItem(
				"Verzeichnis \u00F6ffnen...",
				"directory.open" ) );
    this.mnuPopup.addSeparator();
    this.mnuPopup.add( createJMenuItem(
				"Neue einfache Abbilddatei anlegen...",
				"file.plain.new" ) );
    this.mnuPopup.add( createJMenuItem(
				"Neue Anadisk-Datei anlegen...",
				"file.anadisk.new" ) );
    this.mnuPopup.addSeparator();

    boolean hasSuitableDisks = false;
    if( this.suitableDisks != null ) {
      for( int i = 0; i < this.suitableDisks.length; i++ ) {
	this.mnuPopup.add(
		createJMenuItem(
			this.suitableDisks[ i ].toString() + " einlegen",
			String.format( "disk.open.suitable.%d", i ) ) );
	hasSuitableDisks = true;
      }
    }
    if( this.etcDisks != null ) {
      if( this.etcDisks.length > 0 ) {
	JMenu mnuEtcDisks = null;
	if( hasSuitableDisks ) {
	  mnuEtcDisks = new JMenu( "Weitere Disketten" );
	} else {
	  mnuEtcDisks = new JMenu( "Disketten" );
	}
	for( int i = 0; i < this.etcDisks.length; i++ ) {
	  mnuEtcDisks.add(
		createJMenuItem(
			this.etcDisks[ i ].toString(),
			String.format( "disk.open.etc.%d", i ) ) );
	}
	this.mnuPopup.add( mnuEtcDisks );
      }
    }
    this.mnuPopup.addSeparator();
    this.mnuPopupRemove = createJMenuItem(
				"Diskette/Abbilddatei schlie\u00DFen" );
    this.mnuPopup.add( this.mnuPopupRemove );
  }


  private void diskFormatChanged( AbstractFloppyDisk disk )
  {
    if( disk != null ) {
      for( int i = 0; i < this.drives.length; i++ ) {
	FloppyDiskDrive drive = this.drives[ i ];
	if( disk == drive.getDisk() ) {
	  this.textAreas[ i ].setText(
		createDiskInfoText( disk, drive.getSkipOddCylinders() ) );
	}
      }
    }
  }


  private void openDisk(
		int            idx,
		FloppyDiskInfo diskInfo,
		Boolean        skipOddCyls )
  {
    if( diskInfo != null ) {
      try {
	AbstractFloppyDisk disk = diskInfo.openDisk( this );
	if( disk != null ) {
	  setDisk( idx, diskInfo.toString(), disk, skipOddCyls );
	}
      }
      catch( IOException ex ) {
	BasicDlg.showErrorDlg( isVisible() ? this : this.screenFrm, ex );
      }
    }
  }


  private void openDisk( int idx, Properties props )
  {
    if( props != null ) {
      String prefix    = String.format( "jkcemu.floppydisk.%d.", idx );
      String dirName   = EmuUtil.getProperty( props, prefix + "directory" );
      String driveName = EmuUtil.getProperty( props, prefix + "drive" );
      String fileName  = EmuUtil.getProperty( props, prefix + "file" );
      String resource  = EmuUtil.getProperty( props, prefix + "resource" );
      if( !dirName.isEmpty()
	  || !driveName.isEmpty()
	  || !fileName.isEmpty()
	  || !resource.isEmpty() )
      {
	Boolean skipOddCyls     = null;
	String  skipOddCylsText = EmuUtil.getProperty(
					props,
					prefix + "skip_odd_cylinders" );
	if( !skipOddCylsText.isEmpty() ) {
	  skipOddCyls = Boolean.valueOf( skipOddCylsText );
	}
	FloppyDiskFormat fmt = FloppyDiskFormat.getFormat(
		EmuUtil.getIntProperty( props, prefix + "sides", 0 ),
		EmuUtil.getIntProperty( props, prefix + "cylinders", 0 ),
		EmuUtil.getIntProperty(
				props,
				prefix + "sectors_per_cylinder",
				0 ),
		EmuUtil.getIntProperty( props, prefix + "sectorsize", 0 ) );
	if( !dirName.isEmpty() && (fmt != null) ) {
	  File file = new File( dirName );
	  if( file.isDirectory() ) {
	    if( fmt != null ) {
	      this.lastSysTracks = EmuUtil.getIntProperty(
					props,
					prefix + "system_tracks",
					0 );
	      this.lastBlockSize = EmuUtil.getIntProperty(
					props,
					prefix + "block_size",
					0 );
	      this.lastBlockNum16Bit = (EmuUtil.getIntProperty(
                                        props,
                                        prefix + "block_number_size",
                                        16 ) == 16);
	      this.lastDirBlocks = EmuUtil.getIntProperty(
					props,
					prefix + "dir_blocks",
					2 );
	      this.lastAutoRefresh = EmuUtil.getBooleanProperty(
					props,
					prefix + "auto_refresh",
					false );
	      this.lastFmt = fmt;
	      setDisk(
		idx,
		"Verzeichnis: " + dirName,
		new DirectoryFloppyDisk(
				this,
				fmt.getSides(),
				fmt.getCylinders(),
				fmt.getSectorsPerCylinder(),
				fmt.getSectorSize(),
				this.lastSysTracks,
				this.lastBlockSize,
				this.lastBlockNum16Bit,
				this.lastDirBlocks,
				file,
				this.lastAutoRefresh ),
		skipOddCyls );
	    }
	  }
	}
	else if( !driveName.isEmpty() ) {
	  openDrive(
		idx,
		driveName,
		EmuUtil.getBooleanProperty(
					props,
					prefix + "readonly",
					true ),
		false,
		fmt,
		skipOddCyls );
	}
	else if( !fileName.isEmpty() ) {
	  File file = new File( fileName );
	  if( file.isFile() ) {
	    openFile(
		idx,
		file,
		new Boolean( EmuUtil.getBooleanProperty(
						props,
						prefix + "readonly",
						true ) ),
		false,
		fmt,
		skipOddCyls );
	  }
	}
	else if( !resource.isEmpty() ) {
	  boolean done = false;
	  if( this.suitableDisks != null ) {
	    for( int i = 0; i < this.suitableDisks.length; i++ ) {
	      if( resource.equals( this.suitableDisks[ i ].getResource() ) ) {
		openDisk( idx, this.suitableDisks[ i ], skipOddCyls );
		done = true;
		break;
	      }
	    }
	  }
	  if( !done && (this.etcDisks != null) ) {
	    for( int i = 0; i < this.etcDisks.length; i++ ) {
	      if( resource.equals( this.etcDisks[ i ].getResource() ) ) {
		openDisk( idx, this.etcDisks[ i ], skipOddCyls );
		break;
	      }
	    }
	  }
	}
      }
    }
  }


  private void openDrive(
			int              idx,
			String           fileName,
			boolean          readOnly,
			boolean          interactive,
			FloppyDiskFormat fmt,
			Boolean          skipOddCyls )
  {
    RandomAccessFile raf       = null;
    boolean          done      = true;
    String           errMsg    = null;
    String           driveName = fileName;
    if( fileName.startsWith( "\\\\.\\" ) && (fileName.length() > 4) ) {
      driveName = fileName.substring( 4 );
    }
    try {
      raf = new RandomAccessFile( fileName, readOnly ? "r" : "rw" );

      // Format erfragen
      int     diskSize   = DiskUtil.readDiskSize( raf );
      boolean diskSizeOK = false;
      if( fmt == null ) {
	if( interactive ) {
	  FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			this,
			FloppyDiskFormat.getFormatByDiskSize( diskSize ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT );
	  dlg.setVisible( true );
	  fmt = dlg.getFormat();
	} else {
	  fmt = FloppyDiskFormat.getFormatByDiskSize( diskSize );
	}
      }
      if( fmt != null ) {
	if (diskSize > 0 ) {
	  diskSizeOK = (diskSize == fmt.getDiskSize());
	} else {
	  int selectedDiskSize = fmt.getDiskSize();
	  if( selectedDiskSize != diskSize ) {
	    diskSizeOK = DiskUtil.equalsDiskSize( raf, fmt.getDiskSize() );
	  }
	}
	if( !diskSizeOK ) {
	  if( interactive ) {
	    if( !BasicDlg.showYesNoWarningDlg(
			this,
			"Das ausgew\u00E4hlte Diskettenformat scheint nicht"
				+ " zu passen.\n"
				+ "M\u00F6chten Sie trotzdem fortsetzen?",
			"Diskettenformat" ) )
	    {
	      fmt = null;
	    }
	  } else {
	    fmt = null;
	  }
	}
      }
      if( fmt != null ) {
	if( !readOnly ) {
	  /*
	   * Schreibzugriffe nur gestatten,
	   * wenn die Diskettengroesse mit dem ausgewaehlten Format
	   * uebereinstimmt und somit das Format mit einer gewissen
	   * Wahrscheinlichkeit auch richtig ist.
	   */
	  if( !diskSizeOK ) {
	    if( interactive ) {
	      BasicDlg.showInfoDlg(
			this,
			"Da das ausgew\u00E4hlte Diskettenformat"
				+ " m\u00F6glicherweise falsch ist,\n"
				+ "wird das Laufwerk nur zum Lesen"
				+ " ge\u00F6ffnet." );
	    }
	    readOnly = true;
	  }
	}
      }
      if( (fmt != null) && (raf != null) ) {
	setDisk(
		idx,
		"Laufwerk: " + driveName,
		PlainFileFloppyDisk.createForDrive(
					this,
					fileName,
					raf,
					readOnly,
					fmt ),
		skipOddCyls );
	done = true;
      }
    }
    catch( IOException ex ) {
      String msg = ex.getMessage();
      if( (msg != null) && driveName.equals( fileName ) ) {
	errMsg = msg;
      } else {
	errMsg = String.format(
			"Laufwerk %s kann nicht ge\u00F6ffnet werden.",
			driveName );
      }
    }
    finally {
      if( !done ) {
	EmuUtil.doClose( raf );
      }
    }
    if( errMsg != null ) {
      BasicDlg.showErrorDlg( this, errMsg );
    }
  }


  private void openFile(
			int              idx,
			File             file,
			Boolean          readOnly,
			boolean          interactive,
			FloppyDiskFormat fmt,
			Boolean          skipOddCyls )
  {
    if( ((readOnly == null) && !interactive) || !file.canWrite() ) {
      readOnly = Boolean.TRUE;
    }
    try {
      String fileName = file.getName();
      long   fileLen  = file.length();
      if( file.exists() &&(fileName != null)
	  && (idx >= 0) && (idx < MAX_DRIVE_COUNT) )
      {
	fileName = fileName.toLowerCase();
	if( EmuUtil.endsWith( fileName, DiskUtil.anadiskFileExt ) ) {
	  if( interactive && (readOnly == null) ) {
	    readOnly = askOpenFileReadOnly();
	  }
	  if( readOnly != null ) {
	    AnadiskFloppyDisk disk = null;
	    if( readOnly.booleanValue() ) {
	      disk = AnadiskFloppyDisk.readFile( this, file );
	    } else {
	      if( fileLen > 0 ) {
		disk = AnadiskFloppyDisk.openFile( this, file );
	      } else {
		disk = AnadiskFloppyDisk.newFile( this, file );
	      }
	    }
	    if( disk != null ) {
	      setDisk(
		idx,
		"Anadisk-Datei: " + file.getPath(),
		disk,
		skipOddCyls );
	      Main.setLastFile( file, "disk" );
	    }
	  }
	} else if( EmuUtil.endsWith( fileName, DiskUtil.gzAnadiskFileExt ) ) {
	  AnadiskFloppyDisk disk = AnadiskFloppyDisk.readFile( this, file );
	  if( disk != null ) {
	    setDisk(
		idx,
		"Komprimierte Anadisk-Datei: " + file.getPath(),
		disk,
		skipOddCyls );
	    Main.setLastFile( file, "disk" );
	  }
	} else if( EmuUtil.endsWith( fileName, DiskUtil.telediskFileExt ) ) {
	  TelediskFloppyDisk disk = TelediskFloppyDisk.readFile(
								this,
								file );
	  if( disk != null ) {
	    setDisk(
		idx,
		"Teledisk-Datei: " + file.getPath(),
		disk,
		skipOddCyls );
	    Main.setLastFile( file, "disk" );
	  }
	} else if( EmuUtil.endsWith(
				fileName,
				DiskUtil.gzTelediskFileExt ) )
	{
	  TelediskFloppyDisk disk = TelediskFloppyDisk.readFile( this, file );
	  if( disk != null ) {
	    setDisk(
		idx,
		"Komprimierte Teledisk-Datei: " + file.getPath(),
		disk,
		skipOddCyls );
	    Main.setLastFile( file, "disk" );
	  }
	} else if( EmuUtil.endsWith(
				fileName,
				DiskUtil.gzPlainDiskFileExt ) )
	{
	  int         fLen = 0;
	  byte[]      fBuf = null;
	  InputStream in   = null;
	  try {
	    in   = new GZIPInputStream( new FileInputStream( file ) );
	    fBuf = new byte[ FloppyDiskFormat.getMaxDiskSize() ];
	    fLen = EmuUtil.read( in, fBuf );
	  }
	  finally {
	    EmuUtil.doClose( in );
	  }
	  if( fLen > 0 ) {
	    if( fmt == null ) {
	      FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			this,
			FloppyDiskFormat.getFormatByDiskSize( fLen ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT );
	      dlg.setVisible( true );
	      fmt = dlg.getFormat();
	    }
	    if( fmt != null ) {
	      String              fName = file.getPath();
	      PlainFileFloppyDisk disk
			= PlainFileFloppyDisk.createForByteArray(
								this,
								fName,
								fBuf,
								fmt );
	      if( disk != null ) {
		setDisk(
			idx,
			"Komprimierte einfache Abbilddatei: " + fName,
			disk,
			skipOddCyls );
		Main.setLastFile( file, "disk" );
	      }
	    }
	  }
	} else {
	  boolean state = true;
	  if( interactive ) {
	    state = EmuUtil.endsWith( fileName, DiskUtil.plainDiskFileExt );
	    if( !state ) {
	      StringBuilder buf = new StringBuilder( 512 );
	      int           pos = fileName.lastIndexOf( '.' );
	      if( (pos >= 0) && (pos < (fileName.length() - 1)) ) {
		buf.append( "Dateiendung \'" );
		buf.append( fileName.substring( pos ) );
		buf.append( "\': Unbekannter Dateityp\n\n" );
	      }
	      buf.append( "JKCEMU kann den Dateityp nicht erkennen,\n"
			+ "da die Dateiendung keiner der bei"
			+ " Diskettenabbilddateien\n"
			+ "\u00FCblicherweise verwendeten entspricht.\n"
			+ "Die Datei wird deshalb als einfache Abbilddatei"
			+ " ge\u00F6ffnet." );
	      if( JOptionPane.showConfirmDialog(
			this,
			buf.toString(),
			"Dateiformat",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE )
					== JOptionPane.OK_OPTION )
	      {
		state = true;
	      }
	    }
	  }
	  if( state ) {
	    if( interactive && (fmt == null) ) {
	      FloppyDiskFormatDlg dlg = null;
	      if( readOnly == null ) {
		dlg = new FloppyDiskFormatDlg(
			this,
			FloppyDiskFormat.getFormatByDiskSize( fileLen ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT,
			FloppyDiskFormatDlg.Flag.READONLY );
		dlg.setVisible( true );
		fmt      = dlg.getFormat();
		readOnly = (dlg.getReadOnly() ? Boolean.TRUE : Boolean.FALSE);
	      } else {
		dlg = new FloppyDiskFormatDlg(
			this,
			FloppyDiskFormat.getFormatByDiskSize( fileLen ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT );
		dlg.setVisible( true );
		fmt = dlg.getFormat();
	      }
	    }
	    if( (fmt != null) && (readOnly != null) ) {
	      if( (fileLen >= 0) && (fileLen != fmt.getDiskSize()) ) {
		StringBuilder buf = new StringBuilder( 512 );
		buf.append( "Das von Ihnen ausgew\u00E4hlte"
			+ " Diskettenformat scheint nicht zu passen.\n" );
		if( !readOnly.booleanValue() ) {
		  buf.append( "Sie k\u00F6nnen trotzdem fortsetzen,"
			+ "allerdings wird dann\n"
			+ "die Datei nur mit Schreibschutz ge\u00F6ffnet.\n\n"
			+ "M\u00F6chten Sie fortsetzen?" );
		} else {
		  buf.append( "M\u00F6chten Sie trotzdem fortsetzen?" );
		}
		if( !BasicDlg.showYesNoWarningDlg(
					this,
					buf.toString(),
					"Diskettenformat" ) )
		{
		  fmt = null;
		}
		readOnly = Boolean.TRUE;
	      }
	    }
	    if( readOnly != null ) {
	      PlainFileFloppyDisk disk = null;
	      if( !readOnly.booleanValue() && (file.length() == 0) ) {
		disk = PlainFileFloppyDisk.newFile( this, file );
	      } else {
		if( fmt != null ) {
		  disk = PlainFileFloppyDisk.openFile(
					this,
					file,
					readOnly.booleanValue(),
					fmt );
		}
	      }
	      if( disk != null ) {
		setDisk(
			idx,
			"Einfache Diskettenabbilddatei: " + file.getPath(),
			disk,
			skipOddCyls );
		Main.setLastFile( file, "disk" );
	      }
	    }
	  }
	}
      }
    }
    catch( IOException ex ) {
      showError( ex );
    }
  }


  private void rebuildTabbedPane()
  {
    this.tabbedPane.removeAll();
    int n = Math.min( this.textAreas.length, this.driveCnt );
    for( int i = 0; i < n; i++ ) {
      this.tabbedPane.addTab(
			String.format( "Laufwerk %d", i + 1 ),
			this.textAreas[ i ] );
    }
  }


  private void removeAllDisks()
  {
    for( int i = 0; i < this.drives.length; i++ ) {
      removeDisk( i );
    }
  }


  private void removeDisk( int idx )
  {
    AbstractFloppyDisk disk = this.drives[ idx ].getDisk();
    if( disk != null ) {
      disk.setOwner( null );
      this.drives[ idx ].removeDisk();
    }
    this.textAreas[ idx ].setText( DRIVE_EMPTY_TEXT );
  }


  private void setDisk(
		int                idx,
		String             mediaText,
		AbstractFloppyDisk disk,
		Boolean            skipOddCyls )
  {
    if( disk != null ) {
      if( skipOddCyls == null ) {
	int cyls = disk.getCylinders();
	if( (cyls > 0) && (cyls < 50) ) {
	  switch( OptionDlg.showOptionDlg(
		this,
		"Soll ein 40- oder 80-Spuren-Laufwerk emuliert werden?\n\n"
			+ "Wenn die im Emulator laufende Software von einem"
			+ " 80-Spuren-Laufwerk ausgeht,\n"
			+ "wird sie bei einer Diskette mit einfacher Dichte"
			+ " pro Spurwechsel zwei Schrittimpulse senden.\n"
			+ "JKCEMU muss das wissen, um auf die richtige"
			+ " Spur schlie\u00DFen zu k\u00F6nnen.",
		"Laufwerkstyp",
		-1,
		"40-Spuren-Laufwerk (z.B. K5600.10 / MFS 1.2)",
		"80-Spuren-Laufwerk (z.B. K5600.20 / MFS 1.4,"
						+ " K5601 / MFS 1.6)" ) )
	  {
	    case 0:
	      skipOddCyls = Boolean.FALSE;
	      break;
	    case 1:
	      skipOddCyls = Boolean.TRUE;
	      break;
	  }
	} else {
	  skipOddCyls = Boolean.FALSE;
	}
      }
    }
    if( skipOddCyls != null ) {
      removeDisk( idx );
      disk.setMediaText( mediaText );
      this.drives[ idx ].setDisk( disk, skipOddCyls.booleanValue() );
      this.textAreas[ idx ].setText(
		createDiskInfoText( disk, skipOddCyls.booleanValue() ) );
    } else {
      disk.doClose();
    }
  }


  private void showError( Exception ex )
  {
    BasicDlg.showErrorDlg( isVisible() ? this : this.screenFrm, ex );
  }


  private void showPopup()
  {
    if( this.mnuPopup != null ) {
      if( this.mnuPopupRemove != null ) {
	boolean state = false;
	int     idx   = this.tabbedPane.getSelectedIndex();
	if( (idx >= 0) && (idx < this.drives.length) ) {
	  state = (this.drives[ idx ].getDisk() != null);
	}
	this.mnuPopupRemove.setEnabled( state );
      }
      this.mnuPopup.show( this.btnOpen, 0, this.btnOpen.getHeight() );
    }
  }
}

