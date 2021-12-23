/*
 * (c) 2009-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer Diskettenstation
 */

package jkcemu.disk;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.MenuElement;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.OptionDlg;
import jkcemu.base.PopupMenuOwner;
import jkcemu.base.ScreenFrm;
import jkcemu.emusys.A5105;
import jkcemu.emusys.KC85;
import jkcemu.emusys.KCcompact;
import jkcemu.emusys.NANOS;
import jkcemu.emusys.PCM;
import jkcemu.emusys.Z1013;
import jkcemu.emusys.Z9001;
import jkcemu.file.DirSelectDlg;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;
import jkcemu.tools.fileconverter.FileConvertFrm;


public class FloppyDiskStationFrm
			extends BaseFrm
			implements
				ChangeListener,
				DropTargetListener,
				PopupMenuOwner
{
  private static enum SourceType { DIRECTORY, DRIVE, FILE, RESOURCE };

  private static class DiskSource
  {
    private SourceType sourceType;
    private String     sourceName;

    private DiskSource( SourceType sourceType, String sourceName )
    {
      this.sourceType = sourceType;
      this.sourceName = sourceName;
    }
  };


  private static final String ACTION_CLOSE                = "close";
  private static final String ACTION_HELP                 = "help";
  private static final String ACTION_EXPORT_PREFIX        = "disk.export.";
  private static final String ACTION_NEW_ANADISK_FILE     = "file.anadsk.new";
  private static final String ACTION_NEW_CPCDISK_FILE     = "file.cpcdsk.new";
  private static final String ACTION_NEW_PLAIN_FILE       = "file.plain.new";
  private static final String ACTION_OPEN_SUITABLE_PREFIX = "disk.suitable.";
  private static final String ACTION_OPEN_ETC_PREFIX      = "disk.etc.";
  private static final String ACTION_OPEN_DIRECTORY       = "directory.open";
  private static final String ACTION_OPEN_DRIVE           = "drive.open";
  private static final String ACTION_OPEN_FILE            = "file.open";
  private static final String ACTION_REFRESH              = "refresh";

  private static final String TEXT_EJECT_LOAD  = "\u00D6ffnen/Laden";
  private static final String HELP_PAGE        = "/help/floppydisk.htm";
  private static final String PROP_PREFIX      = "jkcemu.floppydisk.";
  private static final String DRIVE_EMPTY_TEXT = "--- leer ---";
  private static final int    MAX_DRIVE_COUNT  = 4;

  private static volatile FloppyDiskStationFrm instance = null;

  private ScreenFrm         screenFrm;
  private EmuSys            emuSys;
  private Properties        autoOpenDisksProps;
  private FloppyDiskInfo[]  allDisks;
  private FloppyDiskInfo[]  suitableDisks;
  private FloppyDiskInfo[]  etcDisks;
  private FloppyDiskDrive[] drives;
  private FloppyDiskFormat  lastFmt;
  private File              lastDir;
  private volatile boolean  diskErrorShown;
  private int[]             driveAccessCounters;
  private int               driveCnt;
  private boolean           lastAutoRefresh;
  private boolean           lastForceLowerCase;
  private boolean           ledState;
  private JComponent        ledFld;
  private JTextArea[]       textAreas;
  private JTabbedPane       tabbedPane;
  private JButton           btnOpen;
  private JPopupMenu        popupMnu;
  private JMenuItem         popupMnuRefresh;
  private JMenuItem         popupMnuRemove;


  public static void checkOpenDisks( ScreenFrm screenFrm, Properties props )
  {
    FloppyDiskStationFrm frm = getLazyInstance();
    if( frm != null ) {
      frm.removeAllDisks();
    }
    boolean status = false;
    if( props != null ) {
      for( int i = 0; i < MAX_DRIVE_COUNT; i++ ) {
	DiskSource diskSource = getDiskSource( i, props );
	if( diskSource != null ) {
	  getSharedInstance( screenFrm ).openDisk( i, diskSource, props );
	  status = true;
	}
      }
    }
    if( status && EmuUtil.getBooleanProperty(
			props,
			instance.getSettingsPrefix() + PROP_WINDOW_VISIBLE,
			true ) )
    {
      EmuUtil.showFrame( getLazyInstance() );
    }
  }


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  public void fireShowDiskError(
			final AbstractFloppyDisk disk,
			final String             msg,
			final Exception          ex )
  {
    /*
     * Da bei Diskettenzugriffen i.d.R. mehrere Sektoren
     * unmittelbar hintereinander gelesen bzw. geschrieben werden,
     * wuerden bei Fehlern beim Zugriff auf das darunter liegende
     * Medium auch mehrere Fehlermeldungen erscheinen,
     * was recht unschoen ist.
     * Stattdessen soll pro Serie von Diskettenzugriffen,
     * d.h. bis zum Erloeschen der Zugriff-LED,
     * maximal nur eine Fehlermeldung erscheinen.
     */
    if( !this.diskErrorShown ) {
      this.diskErrorShown = true;

      final Component owner = this;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    selectDiskTab( disk );
		    BaseDlg.showErrorDlg( owner, msg, ex );
		  }
		} );
    }
  }


  public void fireDiskFormatChanged( final AbstractFloppyDisk disk )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    diskFormatChanged( disk );
		  }
		} );
  }


  public void fireDriveAccess( FloppyDiskDrive drive )
  {
    for( int i = 0; i < this.drives.length; i++ ) {
      if( this.drives[ i ] == drive ) {
	this.driveAccessCounters[ i ] = 10;
      }
    }
  }


  public static FloppyDiskStationFrm open( ScreenFrm screenFrm )
  {
    return EmuUtil.showFrame( getSharedInstance( screenFrm ) );
  }


  public synchronized static FloppyDiskStationFrm getSharedInstance(
						ScreenFrm  screenFrm )
  {
    if( instance == null ) {
      instance = new FloppyDiskStationFrm( screenFrm );
    }
    return instance;
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource() == this.tabbedPane ) {
      checkDriveAccessState();
      updRefreshBtn();
    }
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
    final File file = FileUtil.fileDrop( this, e );
    if( file != null ) {
      DropTargetContext context = e.getDropTargetContext();
      if( context != null ) {
	final Component c = context.getComponent();
	if( c != null ) {
	  EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    openDirOrFile( c, file );
			  }
			} );
	}
      }
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
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
    if( src == this.btnOpen ) {
      rv = true;
      showPopupMenu();
    }
    else if( src == this.popupMnuRemove ) {
      rv = true;
      doDiskRemove();
    }
    if( !rv && (e instanceof ActionEvent) ) {
      String cmd = ((ActionEvent) e).getActionCommand();
      if( cmd != null ) {
	if( cmd.equals( ACTION_CLOSE ) ) {
	  rv = true;
	  doClose();
	}
	else if( cmd.equals( ACTION_HELP ) ) {
	  rv = true;
	  HelpFrm.openPage( HELP_PAGE );
	}
	else if( cmd.startsWith( ACTION_OPEN_SUITABLE_PREFIX ) ) {
	  int len = ACTION_OPEN_SUITABLE_PREFIX.length();
	  if( cmd.length() > len ) {
	    doDiskOpen( cmd.substring( len ), this.suitableDisks );
	  }
	  rv = true;
	}
	else if( cmd.startsWith( ACTION_OPEN_ETC_PREFIX ) ) {
	  int len = ACTION_OPEN_ETC_PREFIX.length();
	  if( cmd.length() > len ) {
	    doDiskOpen( cmd.substring( len ), this.etcDisks );
	  }
	  rv = true;
	}
	else if( cmd.startsWith( ACTION_EXPORT_PREFIX ) ) {
	  int len = ACTION_EXPORT_PREFIX.length();
	  if( cmd.length() > len ) {
	    doDiskExport( cmd.substring( len ) );
	  }
	  rv = true;
	}
	else if( cmd.equals( ACTION_REFRESH ) ) {
	  rv = true;
	  doDiskRefresh();
	}
	else if( cmd.equals( ACTION_OPEN_DRIVE ) ) {
	  rv = true;
	  doDriveOpen();
	}
	else if( cmd.equals( ACTION_OPEN_DIRECTORY ) ) {
	  rv = true;
	  doDirectoryOpen();
	}
	else if( cmd.equals( ACTION_NEW_ANADISK_FILE ) ) {
	  rv = true;
	  doFileAnaDiskNew();
	}
	else if( cmd.equals( ACTION_NEW_CPCDISK_FILE ) ) {
	  rv = true;
	  doFileCPCDskNew();
	}
	else if( cmd.equals( ACTION_NEW_PLAIN_FILE ) ) {
	  rv = true;
	  doFilePlainNew();
	}
	else if( cmd.equals( ACTION_OPEN_FILE ) ) {
	  rv = true;
	  doFileOpen();
	}
      }
    }
    return rv;
  }


  @Override
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
		if( a[ i ].startsWith( PROP_PREFIX ) ) {
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
				getFloppyDiskPropPrefix( i ) );
      }
    }
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    if( newEmuSys != null ) {
      setEmuSys( newEmuSys );

      // ggf. Fenster schliessen
      if( this.emuSys.getSupportedFloppyDiskDriveCount() < 1 ) {
	doClose();
      }
    }
  }


  @Override
  public void updUI(
		Properties props,
		boolean    updLAF,
		boolean    updFonts,
		boolean    updIcons )
  {
    if( updIcons )
      updLEDSize( props );
  }


	/* --- Aktionen --- */

  private void doDirectoryOpen()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      File               lastDir = this.lastDir;
      AbstractFloppyDisk disk    = this.drives[ idx ].getDisk();
      if( disk != null ) {
	if( disk instanceof DirectoryFloppyDisk ) {
	  lastDir = ((DirectoryFloppyDisk) disk).getDirFile();
	}
      }
      File file = DirSelectDlg.selectDirectory( this, lastDir );
      if( file != null ) {
	if( file.isDirectory() ) {
	  openDirectory( idx, file );
	}
      }
    }
  }


  private void doDiskExport( String srcIdxText )
  {
    if( (srcIdxText != null) && (this.allDisks != null) ) {
      if( !srcIdxText.isEmpty() ) {
	try {
	  int srcIdx = Integer.parseInt( srcIdxText );
	  if( (srcIdx >= 0) && (srcIdx < this.allDisks.length) ) {
	    FloppyDiskInfo diskInfo = this.allDisks[ srcIdx ];
	    String         resource = diskInfo.getResource();
	    if( resource != null ) {
	      String  fName = "";
	      boolean gzip  = false;
	      int     pos   = resource.lastIndexOf( '/' );
	      if( pos >= 0 ) {
		if( (pos + 1) < resource.length() ) {
		  fName = resource.substring( pos + 1 );
		}
	      } else {
		fName = resource;
	      }
	      if( fName.endsWith( ".gz" ) ) {
		fName = fName.substring( 0, fName.length() - 3 );
		gzip  = true;
	      }
	      if( !fName.isEmpty() ) {
		File preSel = Main.getLastDirFile( Main.FILE_GROUP_DISK );
		if( preSel != null ) {
		  preSel = new File( preSel, fName );
		} else {
		  preSel = new File( fName );
		}
		File file = FileUtil.showFileSaveDlg(
					this,
					"Diskette exportieren",
					preSel,
					FileUtil.getAnaDiskFileFilter() );
		if( file != null ) {
		  OutputStream out = null;
		  InputStream  in  = null;
		  InputStream  is  = null;
		  try {
		    in = getClass().getResourceAsStream( resource );
		    if( in != null ) {
		      if( gzip ) {
			is = in;
			in = new GZIPInputStream( in );
		      }
		      out = new BufferedOutputStream(
				      new FileOutputStream( file ) );
		      int b = in.read();
		      while( b >= 0 ) {
			out.write( b );
			b = in.read();
		      }
		      out.close();
		      out = null;
		    }
		  }
		  finally {
		    EmuUtil.closeSilently( out );
		    EmuUtil.closeSilently( in );
		    EmuUtil.closeSilently( is );
		  }
		  setLastFile( file );

		  // Fertigmeldung und weitere Aktionen
		  int option = BaseDlg.showOptionDlg(
			this,
			"Die Diskettenabbilddatei wurde im"
				+ " AnaDisk-Format exportiert.\n"
				+ "Sie k\u00F6nnen nun die Datei"
				+ " in ein anderes Format konvertieren\n"
				+ "oder die in dem Diskettenabbild"
				+ " enthaltenen einzelnen Dateien entpacken.",
			"Export fertig",
			"Konvertieren",
			"Entpacken",
			EmuUtil.TEXT_CLOSE );
		  if( option == 0 ) {
		    FileConvertFrm.open( file );
		  } else if( option == 1 ) {
		    AbstractFloppyDisk disk = DiskUtil.readNonPlainDiskFile(
								this,
								file,
								true );
		    if( disk != null ) {
		      File outDir = FileUtil.askForOutputDir(
					this,
					file,
					"Entpacken nach:",
					diskInfo.toString() + " entpacken" );
		      if( outDir != null ) {
			FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
				this,
				true,
				null,
				FloppyDiskFormatDlg.Flag.APPLY_READONLY,
				FloppyDiskFormatDlg.Flag.FORCE_LOWERCASE );
			dlg.setTitle( "Entpacken" );
			dlg.setVisible( true );
			if( dlg.wasApproved() ) {
			  DiskUnpacker.unpackDisk(
				this,
				disk,
				diskInfo.toString(),
				outDir,
				diskInfo.getSysTracks(),
				diskInfo.getBlockSize(),
				diskInfo.getBlockNum16Bit(),
				dlg.getApplyReadOnly(),
				dlg.getForceLowerCase() );
			}
		      }
		    }
		  }
		}
	      }
	    }
	  }
	}
	catch( IOException ex ) {
	  BaseDlg.showErrorDlg( this, ex );
	}
	catch( NumberFormatException ex ) {}
      }
    }
  }


  private void doDiskOpen( String srcIdxText, FloppyDiskInfo[] disks )
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      if( (srcIdxText != null) && (disks != null) ) {
	if( !srcIdxText.isEmpty() ) {
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
      DriveSelectDlg dlg = new DriveSelectDlg(
				this,
				DeviceIO.MediaType.FLOPPYDISK );
      dlg.setVisible( true );
      String driveFileName = dlg.getSelectedDriveFileName();
      if( driveFileName != null ) {
	if( openDrive(
		idx,
		driveFileName,
		dlg.isReadOnlySelected() ) )
	{
	  Main.setLastDriveFileName( driveFileName );
	}
      }
    }
  }


  private void doFileOpen()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      File file = FileUtil.showFileOpenDlg(
			this,
			"Diskettenabbilddatei \u00F6ffnen",
			Main.getLastDirFile( Main.FILE_GROUP_DISK ),
			FileUtil.getPlainDiskFileFilter(),
			FileUtil.getAnaDiskFileFilter(),
			FileUtil.getCopyQMFileFilter(),
			FileUtil.getDskFileFilter(),
			FileUtil.getImageDiskFileFilter(),
			FileUtil.getTeleDiskFileFilter() );
      if( file != null ) {
	if( openFile( idx, file, null, true, null, null ) ) {
	  setLastFile( file );
	}
      }
    }
  }


  private void doFileAnaDiskNew()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      File file = FileUtil.showFileSaveDlg(
			this,
			"Neue AnaDisk-Datei anlegen",
			Main.getLastDirFile( Main.FILE_GROUP_DISK ),
			FileUtil.getAnaDiskFileFilter() );
      if( file != null ) {
	if( DiskUtil.checkFileExt( this, file, DiskUtil.anaDiskFileExt ) ) {
	  if( confirmNewFileNotFormatted() ) {
	    try {
	      if( setDisk(
			idx, 
			"AnaDisk-Datei: " + file.getPath(),
			AnaDisk.newFile( this, file ),
			null ) )
	      {
		setLastFile( file );
	      }
	    }
	    catch( IOException ex ) {
	      showError( ex );
	    }
	  }
	}
      }
    }
  }


  private void doFileCPCDskNew()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      File file = FileUtil.showFileSaveDlg(
			this,
			"Neue CPC-Disk-Datei anlegen",
			Main.getLastDirFile( Main.FILE_GROUP_DISK ),
			FileUtil.getDskFileFilter() );
      if( file != null ) {
	if( DiskUtil.checkFileExt( this, file, DiskUtil.dskFileExt ) ) {
	  if( confirmNewFileNotFormatted() ) {
	    try {
	      if( setDisk(
			idx, 
			"CPC-Disk-Datei: " + file.getPath(),
			CPCDisk.newFile( this, file ),
			null ) )
	      {
		setLastFile( file );
	      }
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
      File file = FileUtil.showFileSaveDlg(
			this,
			"Einfache Abbilddatei anlegen",
			Main.getLastDirFile( Main.FILE_GROUP_DISK ),
			FileUtil.getPlainDiskFileFilter() );
      if( file != null ) {
	if( DiskUtil.checkFileExt( this, file, DiskUtil.plainDiskFileExt ) ) {
	  if( confirmNewFileNotFormatted() ) {
	    try {
	      if( setDisk(
			idx, 
			"Einfache Abbilddatei: " + file.getPath(),
			PlainDisk.newFile( this, file ),
			null ) )
	      {
		setLastFile( file );
	      }
	    }
	    catch( IOException ex ) {
	      showError( ex );
	    }
	  }
	}
      }
    }
  }


  private void doDiskRefresh()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      AbstractFloppyDisk disk = this.drives[ idx ].getDisk();
      if( disk != null ) {
	if( disk instanceof DirectoryFloppyDisk ) {
	  ((DirectoryFloppyDisk) disk).fireRefresh();
	  BaseDlg.showSuppressableInfoDlg(
		this,
		"Vergessen Sie bitte nicht, umgehend auch in dem im Emulator"
			+ " laufenden Programm bzw. Betriebssystem\n"
			+ "die Diskette zu aktualisieren bzw. das erneute"
			+ " Einlesen des Directorys zu veranlassen!\n"
			+ "(bei CP/M-kompatiblen Betriebssystemen meistens"
			+ " mit CTRL-C bzw. Strg-C)" );
	}
      }
    }
  }


  private void doDiskRemove()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      if( this.drives[ idx ].getDisk() != null ) {
	removeDisk( idx );
      }
    }
  }


	/* --- Konstruktor --- */

  private FloppyDiskStationFrm( ScreenFrm screenFrm )
  {
    this.screenFrm          = screenFrm;
    this.autoOpenDisksProps = null;
    this.suitableDisks      = null;
    this.etcDisks           = null;
    this.lastDir            = null;
    this.lastFmt            = null;
    this.lastAutoRefresh    = false;
    this.lastForceLowerCase = false;
    this.ledState           = false;
    this.diskErrorShown     = false;
    setTitle( "JKCEMU Diskettenstation" );


    // Laufwerke anlegen
    Font font      = new Font( Font.SANS_SERIF, Font.PLAIN, 12 );
    this.textAreas = new JTextArea[ MAX_DRIVE_COUNT ];
    this.drives    = new FloppyDiskDrive[ MAX_DRIVE_COUNT ];
    for( int i = 0; i < MAX_DRIVE_COUNT; i++ ) {
      JTextArea textArea = GUIFactory.createTextArea( 5, 50 );
      textArea.setBorder( BorderFactory.createLoweredBevelBorder() );
      textArea.setFont( font );
      textArea.setEditable( false );
      textArea.setText( DRIVE_EMPTY_TEXT );
      (new DropTarget( textArea, this )).setActive( true );
      this.textAreas[ i ] = textArea;
      this.drives[ i ]    = new FloppyDiskDrive( this );
    }
    this.driveAccessCounters = new int[ this.drives.length ];
    Arrays.fill( this.driveAccessCounters, 0 );


    // Menu Datei
    JMenu mnuFile = createMenuFile();
    mnuFile.add( createMenuItemClose( ACTION_CLOSE ) );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();
    mnuHelp.add( createMenuItem(
			"Hilfe zu Diskettenlaufwerken...",
			ACTION_HELP ) );


    // Menu
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuHelp ) );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						2, 1,
						1.0, 1.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.BOTH,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, gbc );
    this.ledFld = new JPanel()
		{
		  public void paintComponent( Graphics g )
		  {
		    paintLED( g, getWidth(), getHeight() );
		  }
		};
    GUIFactory.initFont( this.ledFld );
    this.ledFld.setBorder( BorderFactory.createLoweredBevelBorder() );
    this.ledFld.setOpaque( true );
    updLEDSize( Main.getProperties() );
    gbc.anchor      = GridBagConstraints.WEST;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.weighty     = 0.0;
    gbc.insets.left = 50;
    gbc.gridwidth   = 1;
    gbc.gridy++;
    add( this.ledFld, gbc );

    this.btnOpen = GUIFactory.createRelImageResourceButton(
					this,
					"disk/eject.png",
					TEXT_EJECT_LOAD );
    gbc.anchor       = GridBagConstraints.EAST;
    gbc.insets.left  = 0;
    gbc.insets.right = 50;
    gbc.gridx++;
    add( this.btnOpen, gbc );


    // Fenstergroesse
    this.driveCnt = 1;	// erstmal ein Tab anlegen fuer die Fenstergroesse
    rebuildTabbedPane();
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      pack();
      setLocationByPlatform( true );
    }
    for( int i = 0; i < this.textAreas.length; i++ ) {
      this.textAreas[ i ].setPreferredSize( new Dimension( 1, 1 ) );
    }


    // Timer
    (new javax.swing.Timer(
		100,
		new ActionListener()
		{
		  public void actionPerformed( ActionEvent e )
		  {
		    checkDriveAccessState();
		  }
		} )).start();


    // Listener
    this.btnOpen.addActionListener( this );
    this.tabbedPane.addChangeListener( this );


    // Sonstiges
    setEmuSys( screenFrm.getEmuSys() );
    Runtime.getRuntime().addShutdownHook(
		new Thread( Main.getThreadGroup(), "JKCEMU disk closer" )
		{
		  @Override
		  public void run()
		  {
		    removeAllDisks();
		  }
		} );
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
		EmuUtil.TEXT_CANCEL };

    switch( BaseDlg.showOptionDlg(
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


  private void checkDriveAccessState()
  {
    boolean state  = false;
    int     tabIdx = this.tabbedPane.getSelectedIndex();
    for( int i = 0; i < this.driveAccessCounters.length; i++ ) {
      if( this.driveAccessCounters[ i ] > 0 ) {
	--this.driveAccessCounters[ i ];
	if( i == tabIdx ) {
	  state = true;
	}
      }
    }
    if( state != this.ledState ) {
      this.ledState = state;
      this.ledFld.repaint();
    }

    // Anzeige von Fehlermeildungen wieder ermoeglichen
    if( !this.ledState ) {
      this.diskErrorShown = false;
    }
  }


  private boolean confirmNewFileNotFormatted()
  {
    boolean  rv      = false;
    String[] options = { "Weiter", EmuUtil.TEXT_CANCEL };
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
	buf.append( '\n' );
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
		+ "\n(Umrechnung der Spurnummern)" );
    }
    return buf.toString();
  }


  private void createPopupMenu()
  {
    if( this.popupMnu != null ) {
      removeActionListenerFrom( this.popupMnu.getSubElements() );
      this.popupMnu.removeAll();
    } else {
      this.popupMnu = GUIFactory.createPopupMenu();
    }
    createPopupMenuItem(
		this.popupMnu,
		"Diskette \u00F6ffnen...",
		ACTION_OPEN_DRIVE );
    createPopupMenuItem(
		this.popupMnu,
		"Diskettenabbilddatei \u00F6ffnen...",
		ACTION_OPEN_FILE );
    createPopupMenuItem(
		this.popupMnu,
		"Verzeichnis \u00F6ffnen...",
		"directory.open" );
    this.popupMnuRefresh = createPopupMenuItem(
		this.popupMnu,
		"Emulierte Diskette aktualisieren",
		ACTION_REFRESH );
    this.popupMnu.addSeparator();
    createPopupMenuItem(
		this.popupMnu,
		"Neue einfache Abbilddatei anlegen...",
		ACTION_NEW_PLAIN_FILE );
    createPopupMenuItem(
		this.popupMnu,
		"Neue AnaDisk-Datei anlegen...",
		ACTION_NEW_ANADISK_FILE );
    createPopupMenuItem(
		this.popupMnu,
		"Neue CPC-Disk-Datei anlegen...",
		ACTION_NEW_CPCDISK_FILE );
    this.popupMnu.addSeparator();

    boolean hasSuitableDisks = false;
    if( this.suitableDisks != null ) {
      for( int i = 0; i < this.suitableDisks.length; i++ ) {
	createPopupMenuItem(
		this.popupMnu,
		this.suitableDisks[ i ].toString() + " einlegen",
		String.format( "%s%d", ACTION_OPEN_SUITABLE_PREFIX, i ) );
	hasSuitableDisks = true;
      }
    }
    if( this.etcDisks != null ) {
      if( this.etcDisks.length > 0 ) {
	JMenu mnuEtcDisks = null;
	if( hasSuitableDisks ) {
	  mnuEtcDisks = GUIFactory.createMenu( "Andere Diskette einlegen" );
	} else {
	  mnuEtcDisks = GUIFactory.createMenu( "Diskette einlegen" );
	}
	for( int i = 0; i < this.etcDisks.length; i++ ) {
	  mnuEtcDisks.add(
		createPopupMenuItem(
			null,
			this.etcDisks[ i ].toString(),
			String.format(
				"%s%d",
				ACTION_OPEN_ETC_PREFIX,
				i ) ) );
	}
	this.popupMnu.add( mnuEtcDisks );
      }
    }
    if( this.allDisks != null ) {
      if( this.allDisks.length > 0 ) {
	JMenu mnuExportDisks = GUIFactory.createMenu(
						"Diskette exportieren" );
	for( int i = 0; i < this.allDisks.length; i++ ) {
	  mnuExportDisks.add(
		createPopupMenuItem(
			null,
			this.allDisks[ i ].toString(),
			String.format(
				"%s%d",
				ACTION_EXPORT_PREFIX,
				i ) ) );
	}
	this.popupMnu.add( mnuExportDisks );
      }
    }
    this.popupMnu.addSeparator();
    this.popupMnuRemove = createPopupMenuItem(
				this.popupMnu,
				"Diskette/Abbilddatei schlie\u00DFen",
				null );
    updRefreshBtn();
  }


  private JMenuItem createPopupMenuItem(
				JPopupMenu popupMnu,
				String     text,
				String     actionCmd )
  {
    JMenuItem item = GUIFactory.createMenuItem( text );
    if( actionCmd != null ) {
      item.setActionCommand( actionCmd );
    }
    item.addActionListener( this );
    if( popupMnu != null ) {
      popupMnu.add( item );
    }
    return item;
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


  private static DiskSource getDiskSource(
				int        idx,
				Properties props )
  {
    DiskSource diskSource = null;
    if( props != null ) {
      String prefix  = getFloppyDiskPropPrefix( idx );
      String dirName = EmuUtil.getProperty(
			props,
			prefix + DirectoryFloppyDisk.PROP_DIRECTORY );
      if( !dirName.isEmpty() ) {
	diskSource = new DiskSource(
				SourceType.DIRECTORY,
				dirName );
      }
      if( diskSource == null ) {
	String driveName = EmuUtil.getProperty(
				props,
				prefix + PlainDisk.PROP_DRIVE );
	if( !dirName.isEmpty() ) {
	  diskSource = new DiskSource(
				SourceType.DRIVE,
				driveName );
	}
      }
      if( diskSource == null ) {
	String fileName = EmuUtil.getProperty(
				props,
				prefix + AbstractFloppyDisk.PROP_FILE );
	if( !fileName.isEmpty() ) {
	  diskSource = new DiskSource(
				SourceType.FILE,
				fileName );
	}
      }
      if( diskSource == null ) {
	String resource = EmuUtil.getProperty(
				props,
				prefix + AbstractFloppyDisk.PROP_RESOURCE );
	if( !resource.isEmpty() ) {
	  diskSource = new DiskSource(
				SourceType.RESOURCE,
				resource );
	}
      }
    }
    return diskSource;
  }


  private static String getFloppyDiskPropPrefix( int diskIdx )
  {
    return String.format( "%s%d.", PROP_PREFIX, diskIdx );
  }


  private static FloppyDiskStationFrm getLazyInstance()
  {
    return instance;
  }


  private void openDirectory( int idx, File file )
  {
    boolean readOnly            = false;
    FloppyDiskFormat defaultFmt = this.lastFmt;
    if( defaultFmt == null ) {
      if( this.emuSys != null ) {
	defaultFmt = this.emuSys.getDefaultFloppyDiskFormat();
      }
    }
    if( defaultFmt == null ) {
      defaultFmt = FloppyDiskFormat.FMT_780K;
    }
    FloppyDiskFormatDlg dlg = null;
    if( file.canWrite() ) {
      dlg = new FloppyDiskFormatDlg(
			this,
			shouldHDFormatsSelectable(),
			defaultFmt,
			FloppyDiskFormatDlg.Flag.FULL_FORMAT,
			FloppyDiskFormatDlg.Flag.READONLY,
			FloppyDiskFormatDlg.Flag.AUTO_REFRESH,
			FloppyDiskFormatDlg.Flag.FORCE_LOWERCASE );
    } else {
      dlg = new FloppyDiskFormatDlg(
			this,
			shouldHDFormatsSelectable(),
			defaultFmt,
			FloppyDiskFormatDlg.Flag.FULL_FORMAT,
			FloppyDiskFormatDlg.Flag.AUTO_REFRESH,
			FloppyDiskFormatDlg.Flag.FORCE_LOWERCASE );
      readOnly = true;
    }
    dlg.setAutoRefresh( this.lastAutoRefresh );
    dlg.setForceLowerCase( this.lastForceLowerCase );
    dlg.setVisible( true );
    FloppyDiskFormat fmt = dlg.getFormat();
    if( fmt != null ) {
      this.lastFmt            = fmt;
      this.lastAutoRefresh    = dlg.getAutoRefresh();
      this.lastForceLowerCase = dlg.getForceLowerCase();
      if( !readOnly ) {
	readOnly = dlg.getReadOnly();
      }
      boolean state = true;
      if( !readOnly ) {
	if( JOptionPane.showConfirmDialog(
		this,
		"Sie \u00F6ffnen das Verzeichnis ohne Schreibschutz.\n"
			+ "Schreibzugriffe auf die emulierte Diskette wirken"
			+ " sich somit direkt auf die Dateien in dem"
			+ " Verzeichnis aus.\n\n"
			+ "Wenn die falsche Directory-Gr\u00F6\u00DFe"
			+ " eingestellt ist\n"
			+ "oder die emulierte Diskette zu einem"
			+ " ung\u00FCnstigen Zeitpunkt aktualisiert wird\n"
			+ "oder nach dem Aktualisieren im Emulator das"
			+ " erneute Einlesen des Directorys"
			+ " nicht veranlasst wird,\n"
			+ "kann es zum ungewollten L\u00F6schen oder"
			+ " \u00DCberschreiben der in dem Verzeichnis"
			+ " liegenden Dateien kommen!\n\n"
			+ "Stellen Sie bitte sicher, dass die Dateien"
			+ " in dem Verzeichnis an einer anderen Stelle"
			+ " nochmals gesichert sind\n"
			+ "und lesen Sie in der Hilfe den Abschnitt"
			+ " \u00FCber die Emulation einer Diskette"
			+ " auf Basis eines Verzeichnisses!",
		"Warnung",
		JOptionPane.OK_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE ) != JOptionPane.OK_OPTION )
	{
	  state = false;
	}
      }
      if( state ) {
	setDisk(
		idx,
		"Verzeichnis: " + file.getPath(),
		new DirectoryFloppyDisk(
				this,
				fmt.getCylinders(),
				fmt.getSides(),
				fmt.getSectorsPerTrack(),
				fmt.getSectorSize(),
				fmt.getSysTracks(),
				fmt.getDirBlocks(),
				fmt.getBlockSize(),
				fmt.isBlockNum16Bit(),
				fmt.isDateStamperEnabled(),
				file,
				this.lastAutoRefresh,
				readOnly,
				this.lastForceLowerCase ),
		null );
	this.lastDir = file;
      }
    }
  }


  private void openDirOrFile( Component c, File file )
  {
    if( (c != null) && (file != null) ) {
      for( int i = 0;
	   (i < this.textAreas.length) && (i < this.drives.length);
	   i++ )
      {
	if( c == this.textAreas[ i ] ) {
	  if( file.isDirectory() ) {
	    openDirectory( i, file );
	  } else {
	    if( openFile( i, file, null, true, null, null ) ) {
	      setLastFile( file );
	    }
	  }
	  break;
	}
      }
    }
  }


  private boolean openDisk(
		int            idx,
		FloppyDiskInfo diskInfo,
		Boolean        skipOddCyls )
  {
    boolean rv = false;
    if( diskInfo != null ) {
      try {
	AbstractFloppyDisk disk = diskInfo.openDisk( this );
	if( disk != null ) {
	  rv = setDisk( idx, diskInfo.toString(), disk, skipOddCyls );
	}
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( isVisible() ? this : this.screenFrm, ex );
      }
    }
    return rv;
  }


  private boolean openDisk(
			int        idx,
			DiskSource diskSource,
			Properties props )
  {
    boolean rv = false;
    if( (diskSource != null) && (props != null) ) {
      String  prefix   = getFloppyDiskPropPrefix( idx );
      boolean readOnly = EmuUtil.getBooleanProperty(
				props,
				prefix + AbstractFloppyDisk.PROP_READONLY,
				true );
      Boolean skipOddCyls     = null;
      String  skipOddCylsText = EmuUtil.getProperty(
			props,
			prefix + FloppyDiskDrive.PROP_SKIP_ODD_CYLS );
      if( !skipOddCylsText.isEmpty() ) {
	skipOddCyls = Boolean.valueOf( skipOddCylsText );
      }
      FloppyDiskFormat fmt = new FloppyDiskFormat(
		EmuUtil.getIntProperty(
			props,
			prefix + AbstractFloppyDisk.PROP_CYLINDERS, 0 ),
		EmuUtil.getIntProperty(
			props,
			prefix + AbstractFloppyDisk.PROP_SIDES, 0 ),
		EmuUtil.getIntProperty(
			props,
			prefix + AbstractFloppyDisk.PROP_SECTORS_PER_TRACK,
			0 ),
		EmuUtil.getIntProperty(
			props,
			prefix + AbstractFloppyDisk.PROP_SECTORSIZE, 0 ),	
			1,
		EmuUtil.getIntProperty(
			props,
			prefix + DirectoryFloppyDisk.PROP_SYSTEM_TRACKS,
			0 ),
		EmuUtil.getIntProperty(
			props,
			prefix + DirectoryFloppyDisk.PROP_DIR_BLOCKS,
			2 ),
		EmuUtil.getIntProperty(
			props,
			prefix + DirectoryFloppyDisk.PROP_BLOCK_SIZE,
			0 ),
		(EmuUtil.getIntProperty(
			props,
			prefix + DirectoryFloppyDisk.PROP_BLOCK_NUMBER_SIZE,
			16 ) == 16),
		EmuUtil.getBooleanProperty(
			props,
			prefix + DirectoryFloppyDisk.PROP_DATESTAMPER,
			false ),
		null );
      switch( diskSource.sourceType ) {
	case DIRECTORY:
	  if( fmt != null ) {
	    File file = new File( diskSource.sourceName );
	    if( file.isDirectory() ) {
	      if( fmt != null ) {
		this.lastAutoRefresh = EmuUtil.getBooleanProperty(
			props,
			prefix + DirectoryFloppyDisk.PROP_AUTO_REFRESH,
			false );
		this.lastForceLowerCase = EmuUtil.getBooleanProperty(
			props,
			prefix + DirectoryFloppyDisk.PROP_FORCE_LOWERCASE,
			true );
		this.lastFmt = fmt;
		setDisk(
			idx,
			"Verzeichnis: " + diskSource.sourceName,
			new DirectoryFloppyDisk(
				this,
				fmt.getCylinders(),
				fmt.getSides(),
				fmt.getSectorsPerTrack(),
				fmt.getSectorSize(),
				fmt.getSysTracks(),
				fmt.getDirBlocks(),
				fmt.getBlockSize(),
				fmt.isBlockNum16Bit(),
				fmt.isDateStamperEnabled(),
				file,
				this.lastAutoRefresh,
				readOnly,
				this.lastForceLowerCase ),
		  skipOddCyls );
		rv = true;
	      }
	    }
	  }
	  break;

	case DRIVE:
	  rv = openDrive( idx, diskSource.sourceName, readOnly );
	  break;

	case FILE:
	  {
	    File file = new File( diskSource.sourceName );
	    if( file.isFile() ) {
	      rv = openFile( idx, file, readOnly, false, fmt, skipOddCyls );
	    }
	  }
	  break;

	case RESOURCE:
	  {
	    boolean done = false;
	    if( this.suitableDisks != null ) {
	      for( int i = 0; i < this.suitableDisks.length; i++ ) {
		if( diskSource.sourceName.equals(
				this.suitableDisks[ i ].getResource() ) )
		{
		  rv = openDisk( idx, this.suitableDisks[ i ], skipOddCyls );
		  done = true;
		  break;
		}
	      }
	    }
	    if( !done && (this.etcDisks != null) ) {
	      for( int i = 0; i < this.etcDisks.length; i++ ) {
		if( diskSource.sourceName.equals(
				this.etcDisks[ i ].getResource() ) )
		{
		  rv = openDisk( idx, this.etcDisks[ i ], skipOddCyls );
		  break;
		}
	      }
	    }
	  }
	  break;
      }
    }
    return rv;
  }


  private boolean openDrive(
			int     idx,
			String  fileName,
			boolean readOnly )
  {
    boolean                     rv        = false;
    boolean                     usb       = false;
    DeviceIO.RandomAccessDevice rad       = null;
    String                      errMsg    = null;
    String                      driveName = fileName;
    if( fileName.startsWith( "\\\\.\\" ) && (fileName.length() > 4) ) {
      driveName = fileName.substring( 4 );
    }
    try {
      rad = DeviceIO.openDeviceForRandomAccess( fileName, readOnly );
      if( rad == null ) {
	throw new IOException();
      }
      DeviceIO.DiskInfo diskInfo = rad.getDiskInfo();
      if( diskInfo != null ) {
	usb = diskInfo.isUSB();
      }
      rv = setDisk(
		idx,
		"Laufwerk: " + driveName,
		PlainDisk.createForDrive(
				this,
				fileName,
				rad,
				readOnly,
				DiskUtil.getFloppyDiskFormat( rad ) ),
		false );
    }
    catch( IOException ex ) {
      String msg = ex.getMessage();
      if( msg != null ) {
	if( driveName.equals( fileName ) ) {
	  errMsg = msg;
	} else {
	  errMsg = msg.replace( fileName, driveName );
	}
      } else {
	errMsg = "Diskette in Laufwerk "
			+ driveName
			+ " nicht gefunden oder nicht nutzbar";
      }
      if( usb
	  && !Main.isUnixLikeOS()
	  && !driveName.startsWith( "A:" )
	  && !driveName.startsWith( "B:" ) )
      {
	errMsg = msg + "\n\n"
		+ "M\u00F6glicherweise wurde das Diskettenlaufwerk vom"
		+ " Betriebssystem nicht richtig erkannt.\n"
		+ "In dem Fall sollten Sie die Diskette auswerfen lassen"
		+ " und das Laufwerk vom Computer trennen.\n"
		+ "Anschlie\u00DFend schlie\u00DFen Sie es ohne"
		+ " eingelegte Diskette erneut an.\n"
		+ "Legen Sie die Diskette erst wieder ein, nachdem das"
		+ " Betriebssystem das Laufwerk erkannt hat.\n"
		+ "Danach k\u00F6nnen Sie die Diskette im JKCEMU"
		+ " \u00F6ffnen.";
      }
    }
    finally {
      if( !rv ) {
	EmuUtil.closeSilently( rad );
      }
    }
    if( errMsg != null ) {
      BaseDlg.showErrorDlg( this, errMsg );
    }
    return rv;
  }


  private boolean openFile(
			int              idx,
			File             file,
			Boolean          readOnly,
			boolean          interactive,
			FloppyDiskFormat fmt,
			Boolean          skipOddCyls )
  {
    boolean rv = false;
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
	if( TextUtil.endsWith( fileName, DiskUtil.plainDiskFileExt ) ) {
	  rv = openPlainDiskFile(
				idx,
				file,
				fileLen,
				readOnly,
				interactive,
				fmt,
				skipOddCyls );
	} else if( TextUtil.endsWith(
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
	    EmuUtil.closeSilently( in );
	  }
	  if( fLen > 0 ) {
	    if( fmt == null ) {
	      FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			this,
			shouldHDFormatsSelectable(),
			FloppyDiskFormat.getFormatByDiskSize( fLen ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT );
	      dlg.setVisible( true );
	      fmt = dlg.getFormat();
	    }
	    if( fmt != null ) {
	      String              fName = file.getPath();
	      PlainDisk disk = PlainDisk.createForByteArray(
							this,
							fName,
							fBuf,
							fmt );
	      if( disk != null ) {
		rv = setDisk(
			idx,
			"Einfache Abbilddatei: " + fName,
			disk,
			skipOddCyls );
	      }
	    }
	  }
	} else if( TextUtil.endsWith( fileName, DiskUtil.anaDiskFileExt ) ) {
	  if( interactive && (readOnly == null) ) {
	    readOnly = askOpenFileReadOnly();
	  }
	  if( readOnly != null ) {
	    AnaDisk disk = null;
	    if( readOnly.booleanValue() ) {
	      disk = AnaDisk.readFile( this, file );
	    } else {
	      if( fileLen > 0 ) {
		disk = AnaDisk.openFile( this, file );
	      } else {
		disk = AnaDisk.newFile( this, file );
	      }
	    }
	    if( disk != null ) {
	      rv = setDisk(
			idx,
			"AnaDisk-Datei: " + file.getPath(),
			disk,
			skipOddCyls );
	    }
	  }
	} else if( TextUtil.endsWith( fileName, DiskUtil.dskFileExt ) ) {
	  if( interactive && (readOnly == null) ) {
	    readOnly = askOpenFileReadOnly();
	  }
	  if( readOnly != null ) {
	    CPCDisk disk = null;
	    if( readOnly.booleanValue() ) {
	      disk = CPCDisk.readFile( this, file );
	    } else {
	      if( fileLen > 0 ) {
		disk = CPCDisk.openFile( this, file );
	      } else {
		disk = CPCDisk.newFile( this, file );
	      }
	    }
	    if( disk != null ) {
	      rv = setDisk(
			idx,
			"CPC-Disk-Datei: " + file.getPath(),
			disk,
			skipOddCyls );
	    }
	  }
	} else {
	  AbstractFloppyDisk disk = DiskUtil.readNonPlainDiskFile(
								this,
								file,
								true );
	  if( disk != null ) {
	    String fmtText = disk.getFileFormatText();
	    if( fmtText == null ) {
	      fmtText = "Diskettenabbilddatei";
	    }
	    rv = setDisk(
			idx,
			fmtText + ": " + file.getPath(),
			disk,
			skipOddCyls );
	  } else {
	    boolean state = true;
	    if( interactive ) {
	      state = TextUtil.endsWith(
				fileName,
				DiskUtil.plainDiskFileExt );
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
	      rv = openPlainDiskFile(
				idx,
				file,
				fileLen,
				readOnly,
				interactive,
				fmt,
				skipOddCyls );
	    }
	  }
	}
      }
    }
    catch( IOException ex ) {
      showError( ex );
    }
    return rv;
  }


  private boolean openPlainDiskFile(
			int              idx,
			File             file,
			long             fileLen,
			Boolean          readOnly,
			boolean          interactive,
			FloppyDiskFormat fmt,
			Boolean          skipOddCyls ) throws IOException
  {
    boolean rv = false;
    if( interactive && (fmt == null) ) {
      FloppyDiskFormatDlg dlg = null;
      if( readOnly == null ) {
	dlg = new FloppyDiskFormatDlg(
			this,
			shouldHDFormatsSelectable(),
			FloppyDiskFormat.getFormatByDiskSize( fileLen ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT,
			FloppyDiskFormatDlg.Flag.READONLY );
	dlg.setVisible( true );
	fmt      = dlg.getFormat();
	readOnly = (dlg.getReadOnly() ? Boolean.TRUE : Boolean.FALSE);
      } else {
	dlg = new FloppyDiskFormatDlg(
			this,
			shouldHDFormatsSelectable(),
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
	if( !BaseDlg.showYesNoWarningDlg(
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
      PlainDisk disk = null;
      if( !readOnly.booleanValue() && (file.length() == 0) ) {
	disk = PlainDisk.newFile( this, file );
      } else {
	if( fmt != null ) {
	  disk = PlainDisk.openFile(
				this,
				file,
				readOnly.booleanValue(),
				fmt );
	}
      }
      if( disk != null ) {
	rv = setDisk(
		idx,
		"Einfache Diskettenabbilddatei: " + file.getPath(),
		disk,
		skipOddCyls );
      }
    }
    return rv;
  }


  private void paintLED( Graphics g, int w, int h )
  {
    if( (w > 0) && (h > 0) ) {
      g.setColor( this.ledState ? Color.RED : Color.GRAY );
      g.fillRect( 0, 0, w, h );
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


  private void removeActionListenerFrom( MenuElement[] elements )
  {
    if( elements != null ) {
      for( MenuElement e : elements ) {
	if( e instanceof AbstractButton ) {
	  ((AbstractButton) e).removeActionListener( this );
	}
	removeActionListenerFrom( e.getSubElements() );
      }
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
    updRefreshBtn();
  }


  private void selectDiskTab( AbstractFloppyDisk disk )
  {
    if( disk != null ) {
      int n = Math.min( this.drives.length, this.driveCnt );
      for( int i = 0; i < n; i++ ) {
	if( this.drives[ i ] != null ) {
	  if( this.drives[ i ].getDisk() == disk ) {
	    try {
	      this.tabbedPane.setSelectedIndex( i );
	    }
	    catch( IndexOutOfBoundsException ex ) {}
	  }
	}
      }
    }
  }


  private boolean setDisk(
			int                idx,
			String             mediaText,
			AbstractFloppyDisk disk,
			Boolean            skipOddCyls )
  {
    boolean rv = false;
    if( disk != null ) {
      if( DiskUtil.checkAndConfirmWarning( this, disk ) ) {
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
      } else {
	skipOddCyls = null;
      }
      if( skipOddCyls != null ) {
	removeDisk( idx );
	disk.setMediaText( mediaText );
	this.drives[ idx ].setDisk( disk, skipOddCyls.booleanValue() );
	this.textAreas[ idx ].setText(
		  createDiskInfoText( disk, skipOddCyls.booleanValue() ) );
	rv = true;
      } else {
	disk.closeSilently();
      }
    } else {
      removeDisk( idx );
      rv = true;
    }
    updRefreshBtn();
    return rv;
  }


  private void setEmuSys( EmuSys emuSys )
  {
    this.emuSys = emuSys;

    // Laufwerke dem EmuSys bekanntmachen
    int nDrives = this.emuSys.getSupportedFloppyDiskDriveCount();
    for( int i = 0; i < nDrives; i++ ) {
      emuSys.setFloppyDiskDrive(
		i,
		i < this.drives.length ? this.drives[ i ] : null );
    }

    // Anzahl der Laufwerke im Fenster anpassen
    if( nDrives != this.driveCnt ) {
      this.driveCnt = nDrives;
      rebuildTabbedPane();
    }

    // Menu mit verfuegbaren Disketten anpassen
    boolean differs       = true;
    boolean hasDefaultFmt = (emuSys.getDefaultFloppyDiskFormat() != null);
    FloppyDiskInfo[] suitableDisks = emuSys.getSuitableFloppyDisks();
    if( (suitableDisks != null) && (this.suitableDisks != null) ) {
      if( (suitableDisks.length > 0)
	  && (this.suitableDisks.length > 0) )
      {
	if( Arrays.equals( suitableDisks, this.suitableDisks ) ) {
	  differs = false;
	}
      }
    }
    this.suitableDisks = suitableDisks;
    if( differs ) {
      this.lastAutoRefresh = false;
      if( hasDefaultFmt ) {
	this.lastFmt = null;
      }
      this.allDisks = null;
      this.etcDisks = null;
      try {
	Set<FloppyDiskInfo> disks = new TreeSet<>();
	addFloppyDiskInfo( disks, A5105.getAvailableFloppyDisks() );
	addFloppyDiskInfo( disks, KC85.getAvailableFloppyDisks() );
	addFloppyDiskInfo( disks, KCcompact.getAvailableFloppyDisks() );
	addFloppyDiskInfo( disks, NANOS.getAvailableFloppyDisks() );
	addFloppyDiskInfo( disks, PCM.getAvailableFloppyDisks() );
	addFloppyDiskInfo( disks, Z1013.getAvailableFloppyDisks() );
	addFloppyDiskInfo( disks, Z9001.getAvailableFloppyDisks() );
	int nDisks = disks.size();
	if( nDisks > 0 ) {
	  this.allDisks = disks.toArray( new FloppyDiskInfo[ nDisks ] );
	}
	if( this.suitableDisks != null ) {
	  for( int i = 0; i < this.suitableDisks.length; i++ ) {
	    disks.remove( this.suitableDisks[ i ] );
	  }
	}
	nDisks = disks.size();
	if( nDisks > 0 ) {
	  this.etcDisks = disks.toArray( new FloppyDiskInfo[ nDisks ] );
	}
      }
      catch( Exception ex ) {}
      createPopupMenu();
    }
  }


  private void setLastFile( File file )
  {
    Main.setLastFile( file, Main.FILE_GROUP_DISK );
  }


  private boolean shouldHDFormatsSelectable()
  {
    return this.emuSys != null ? this.emuSys.supportsHDDisks() : false;
  }


  private void showError( Exception ex )
  {
    BaseDlg.showErrorDlg( isVisible() ? this : this.screenFrm, ex );
  }


  private void showPopupMenu()
  {
    if( this.popupMnu != null ) {
      if( this.popupMnuRemove != null ) {
	boolean state = false;
	int     idx   = this.tabbedPane.getSelectedIndex();
	if( (idx >= 0) && (idx < this.drives.length) ) {
	  state = (this.drives[ idx ].getDisk() != null);
	}
	this.popupMnuRemove.setEnabled( state );
      }
      this.popupMnu.show( this.btnOpen, 0, this.btnOpen.getHeight() );
    }
  }


  private void updLEDSize( Properties props )
  {
    Dimension size = GUIFactory.getLargeSymbolsEnabled( props ) ?
						new Dimension( 48, 24 )
						: new Dimension( 30, 15 );
    this.ledFld.setPreferredSize( size );
    this.ledFld.setMinimumSize( size );
    this.ledFld.setMaximumSize( size );
  }


  private void updRefreshBtn()
  {
    boolean state = false;
    int     idx   = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      if( this.drives[ idx ] != null ) {
	AbstractFloppyDisk disk = this.drives[ idx ].getDisk();
	if( disk != null ) {
	  if( disk instanceof DirectoryFloppyDisk ) {
	    state = true;
	  }
	}
      }
    }
    if( this.popupMnuRefresh != null ) {
      this.popupMnuRefresh.setEnabled( state );
    }
  }
}
