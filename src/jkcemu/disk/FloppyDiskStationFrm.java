/*
 * (c) 2009-2011 Jens Mueller
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
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.emusys.*;


public class FloppyDiskStationFrm
			extends BasicFrm
			implements
				ChangeListener,
				DropTargetListener
{
  private static final String DRIVE_EMPTY_TEXT = "--- leer ---";
  private static final int    MAX_DRIVE_COUNT  = 4;

  private ScreenFrm         screenFrm;
  private EmuSys            emuSys;
  private FloppyDiskInfo[]  allDisks;
  private FloppyDiskInfo[]  suitableDisks;
  private FloppyDiskInfo[]  etcDisks;
  private FloppyDiskDrive[] drives;
  private FloppyDiskFormat  lastFmt;
  private volatile boolean  diskErrorShown;
  private int[]             driveAccessCounters;
  private int               driveCnt;
  private int               lastSysTracks;
  private int               lastBlockSize;
  private int               lastDirBlocks;
  private boolean           lastBlockNum16Bit;
  private boolean           lastAutoRefresh;
  private boolean           lastForceLowerCase;
  private boolean           refreshInfoEnabled;
  private boolean           ledState;
  private JComponent        ledFld;
  private JTextArea[]       textAreas;
  private JTabbedPane       tabbedPane;
  private JButton           btnOpen;
  private JPopupMenu        mnuPopup;
  private JMenuItem         mnuPopupRefresh;
  private JMenuItem         mnuPopupRemove;


  public FloppyDiskStationFrm( ScreenFrm screenFrm )
  {
    this.screenFrm          = screenFrm;
    this.emuSys             = null;
    this.suitableDisks      = null;
    this.etcDisks           = null;
    this.lastFmt            = null;
    this.lastSysTracks      = -1;
    this.lastBlockSize      = -1;
    this.lastDirBlocks      = -1;
    this.lastBlockNum16Bit  = false;
    this.lastAutoRefresh    = false;
    this.lastForceLowerCase = false;
    this.ledState           = false;
    this.diskErrorShown     = false;
    this.refreshInfoEnabled = true;
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
      this.drives[ i ]    = new FloppyDiskDrive( this );
    }
    this.driveAccessCounters = new int[ this.drives.length ];
    Arrays.fill( this.driveAccessCounters, 0 );


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
						2, 1,
						1.0, 1.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.BOTH,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, gbc );

    Dimension ledSize = new Dimension( 30, 15 );
    this.ledFld = new JPanel()
		{
		  public void paintComponent( Graphics g )
		  {
		    paintLED( g, getWidth(), getHeight() );
		  }
		};
    this.ledFld.setBorder( BorderFactory.createLoweredBevelBorder() );
    this.ledFld.setOpaque( true );
    this.ledFld.setPreferredSize( ledSize );
    this.ledFld.setMinimumSize( ledSize );
    this.ledFld.setMaximumSize( ledSize );
    gbc.anchor      = GridBagConstraints.WEST;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.weighty     = 0.0;
    gbc.insets.left = 50;
    gbc.gridwidth   = 1;
    gbc.gridy++;
    add( this.ledFld, gbc );

    this.btnOpen = createImageButton(
				"/images/disk/eject.png",
				"\u00D6ffnen/Laden" );
    gbc.anchor       = GridBagConstraints.EAST;
    gbc.insets.left  = 0;
    gbc.insets.right = 50;
    gbc.gridx++;
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
    this.tabbedPane.addChangeListener( this );


    // Sonstiges
    Runtime.getRuntime().addShutdownHook(
		new Thread( "JKCEMU disk closer" )
		{
		  public void run()
		  {
		    removeAllDisks();
		  }
		} );
  }


  public void fireShowDiskError(
			final AbstractFloppyDisk disk,
			final String             msg,
			final Exception          ex )
  {
    /*
     * Da bei Diskettenzugriffen i.d.R. mehrere Sektoren
     * unmittelbar hintereinander gelesen bzw. geschrieben werden,
     * wurden im bei Fehlern beim Zugriff auf das darunter liegende
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
		  public void run()
		  {
		    selectDiskTab( disk );
		    BasicDlg.showErrorDlg( owner, msg, ex );
		  }
		} );
    }
  }


  public void fireDiskFormatChanged( final AbstractFloppyDisk disk )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
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
    for( int i = 0; i < MAX_DRIVE_COUNT; i++ ) {
      if( !openDisk( i, props ) ) {
	removeDisk( i );
      }
    }
  }


  public void setDriveCount( int n )
  {
    if( n != this.driveCnt ) {
      this.driveCnt = n;
      EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    rebuildTabbedPane();
		  }
		} );
    }
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
	      if( file.isDirectory() ) {
		openDirectory( i, file );
	      } else {
		openFile( i, file, null, true, null, null );
	      }
	      break;
	    }
	  }
	}
      }
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    EmuSys emuSys = null;
    EmuThread emuThread = this.screenFrm.getEmuThread();
    if( emuThread != null ) {
      emuSys = emuThread.getEmuSys();
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
      this.allDisks = null;
      this.etcDisks = null;
      try {
	Set<FloppyDiskInfo> disks = new TreeSet<FloppyDiskInfo>();
	addFloppyDiskInfo( disks, A5105.getAvailableFloppyDisks() );
	addFloppyDiskInfo( disks, KC85.getAvailableFloppyDisks() );
	addFloppyDiskInfo( disks, Z1013.getAvailableFloppyDisks() );
	addFloppyDiskInfo( disks, Z9001.getAvailableFloppyDisks() );
	int n = disks.size();
	if( n > 0 ) {
	  this.allDisks = disks.toArray( new FloppyDiskInfo[ n ] );
	}
	if( this.suitableDisks != null ) {
	  for( int i = 0; i < this.suitableDisks.length; i++ ) {
	    disks.remove( this.suitableDisks[ i ] );
	  }
	}
	n = disks.size();
	if( n > 0 ) {
	  this.etcDisks = disks.toArray( new FloppyDiskInfo[ n ] );
	}
      }
      catch( Exception ex ) {}
      createPopupMenu();
    }
    return super.applySettings( props, resizable );
  }


  @Override
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
	  else if( cmd.startsWith( "disk.export." )
		   && (cmd.length() > 12) )
	  {
	    rv = true;
	    doDiskExport( cmd.substring( 12 ), this.allDisks );
	  }
	  else if( cmd.equals( "disk.refresh" ) ) {
	    rv = true;
	    doDiskRefresh();
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


  @Override
  public void lookAndFeelChanged()
  {
    if( this.mnuPopup != null )
      SwingUtilities.updateComponentTreeUI( this.mnuPopup );
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
	  openDirectory( idx, file );
	}
      }
    }
  }


  private void doDiskExport( String srcIdxText, FloppyDiskInfo[] disks )
  {
    int idx = this.tabbedPane.getSelectedIndex();
    if( (idx >= 0) && (idx < this.drives.length) ) {
      if( (srcIdxText != null) && (disks != null) ) {
	if( !srcIdxText.isEmpty() ) {
	  try {
	    int srcIdx = Integer.parseInt( srcIdxText );
	    if( (srcIdx >= 0) && (srcIdx < disks.length) ) {
	      String resource = disks[ srcIdx ].getResource();
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
		  File preSel = Main.getLastPathFile( "disk" );
		  if( preSel != null ) {
		    preSel = new File( preSel, fName );
		  } else {
		    preSel = new File( fName );
		  }
		  File file = EmuUtil.showFileSaveDlg(
					this,
					"Diskette exportieren",
					preSel,
					EmuUtil.getAnadiskFileFilter() );
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
		      EmuUtil.doClose( out );
		      EmuUtil.doClose( in );
		      EmuUtil.doClose( is );
		    }
		    Main.setLastFile( file, "disk" );
		    BasicDlg.showInfoDlg( this, "Export fertig" );
		  }
		}
	      }
	    }
	  }
	  catch( IOException ex ) {
	    BasicDlg.showErrorDlg( this, ex );
	  }
	  catch( NumberFormatException ex ) {}
	}
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
      DriveSelectDlg dlg = new DriveSelectDlg( this, true );
      dlg.setVisible( true );
      String driveFileName = dlg.getSelectedDriveFileName();
      if( driveFileName != null ) {
	openDrive(
		idx,
		driveFileName,
		dlg.isReadOnlySelected(),
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
			EmuUtil.getCopyQMFileFilter(),
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
	      if( setDisk(
			idx, 
			"Anadisk-Datei: " + file.getPath(),
			AnadiskFloppyDisk.newFile( this, file ),
			null ) )
	      {
		Main.setLastFile( file, "disk" );
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
      File file = EmuUtil.showFileSaveDlg(
			this,
			"Einfache Abbilddatei anlegen",
			Main.getLastPathFile( "disk" ),
			EmuUtil.getPlainDiskFileFilter() );
      if( file != null ) {
	if( DiskUtil.checkFileExt( this, file, DiskUtil.plainDiskFileExt ) ) {
	  if( confirmNewFileNotFormatted() ) {
	    try {
	      if( setDisk(
			idx, 
			"Einfache Abbilddatei: " + file.getPath(),
			PlainFileFloppyDisk.newFile( this, file ),
			null ) )
	      {
		Main.setLastFile( file, "disk" );
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
	  if( this.refreshInfoEnabled ) {
	    showRefreshInfo();
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
    this.mnuPopupRefresh = createJMenuItem(
				"Emulierte Diskette aktualisieren",
				"disk.refresh" );
    this.mnuPopup.add( this.mnuPopupRefresh );
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
	  mnuEtcDisks = new JMenu( "Andere Diskette einlegen" );
	} else {
	  mnuEtcDisks = new JMenu( "Diskette einlegen" );
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
    if( this.allDisks != null ) {
      if( this.allDisks.length > 0 ) {
	JMenu mnuExportDisks = new JMenu( "Diskette exportieren" );
	for( int i = 0; i < this.allDisks.length; i++ ) {
	  mnuExportDisks.add(
		createJMenuItem(
			this.allDisks[ i ].toString(),
			String.format( "disk.export.%d", i ) ) );
	}
	this.mnuPopup.add( mnuExportDisks );
      }
    }
    this.mnuPopup.addSeparator();
    this.mnuPopupRemove = createJMenuItem(
				"Diskette/Abbilddatei schlie\u00DFen" );
    this.mnuPopup.add( this.mnuPopupRemove );

    updRefreshBtn();
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


  private void openDirectory( int idx, File file )
  {
    int              sysTracks     = -1;
    int              blockSize     = -1;
    int              dirBlocks     = -1;
    boolean          blockNum16Bit = false;
    boolean          readOnly      = false;
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
    FloppyDiskFormatDlg dlg = null;
    if( file.canWrite() ) {
      dlg = new FloppyDiskFormatDlg(
		this,
		defaultFmt,
		FloppyDiskFormatDlg.Flag.PHYS_FORMAT,
		FloppyDiskFormatDlg.Flag.SYSTEM_TRACKS,
		FloppyDiskFormatDlg.Flag.BLOCK_SIZE,
		FloppyDiskFormatDlg.Flag.BLOCK_NUM_SIZE,
		FloppyDiskFormatDlg.Flag.DIR_BLOCKS,
		FloppyDiskFormatDlg.Flag.READONLY,
		FloppyDiskFormatDlg.Flag.AUTO_REFRESH,
		FloppyDiskFormatDlg.Flag.FORCE_LOWERCASE );
    } else {
      dlg = new FloppyDiskFormatDlg(
		this,
		defaultFmt,
		FloppyDiskFormatDlg.Flag.PHYS_FORMAT,
		FloppyDiskFormatDlg.Flag.SYSTEM_TRACKS,
		FloppyDiskFormatDlg.Flag.BLOCK_SIZE,
		FloppyDiskFormatDlg.Flag.BLOCK_NUM_SIZE,
		FloppyDiskFormatDlg.Flag.DIR_BLOCKS,
		FloppyDiskFormatDlg.Flag.AUTO_REFRESH );
      readOnly = true;
    }
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
    dlg.setForceLowerCase( this.lastForceLowerCase );
    dlg.setVisible( true );
    FloppyDiskFormat fmt = dlg.getFormat();
    if( fmt != null ) {
      this.lastFmt            = fmt;
      this.lastSysTracks      = dlg.getSystemTracks();
      this.lastBlockSize      = dlg.getBlockSize();
      this.lastBlockNum16Bit  = dlg.getBlockNum16Bit();
      this.lastDirBlocks      = dlg.getDirBlocks();
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
			+ " nochmals gesichert sind!",
		"Warnung",
		JOptionPane.OK_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE ) != JOptionPane.OK_OPTION )
	{
	  state = false;
	}
      }
      if( state ) {
	File sysFile = new File( file, DirectoryFloppyDisk.SYS_FILE_NAME );
	if( this.lastSysTracks > 0 ) {
	  if( !sysFile.isFile() && !sysFile.exists() ) {
	    if( this.lastSysTracks == 1 ) {
	      BasicDlg.showInfoDlg(
		this,
		"Sie haben ein Diskettenformat mit einer Systemspur"
			+ " ausgew\u00E4hlt.\n"
			+ "In dem Verzeichnis gibt es aber die Datei "
			+ DirectoryFloppyDisk.SYS_FILE_NAME
			+ " nicht,\n"
			+ "aus der JKCEMU den Inhalt der Systemspur"
			+ " liest\n"
			+ "Diese ist deshalb leer." );
	    } else {
	      BasicDlg.showInfoDlg(
		this,
		"Sie haben ein Diskettenformat mit Systemspuren"
			+ " ausgew\u00E4hlt.\n"
			+ "In dem Verzeichnis gibt es aber die Datei "
			+ DirectoryFloppyDisk.SYS_FILE_NAME
			+ " nicht,\n"
			+ "aus der JKCEMU den Inhalt der Systemspuren"
			+ " liest\n"
			+ "Diese sind deshalb leer." );
	    }
	  }
	} else {
	  if( sysFile.exists() && sysFile.isFile() ) {
	    if( JOptionPane.showConfirmDialog(
		this,
		"Sie haben ein Diskettenformat ohne Systemspuren"
			+ " ausgew\u00E4hlt.\n"
			+ "In dem Verzeichnis gibt es aber die Datei "
			+ DirectoryFloppyDisk.SYS_FILE_NAME
			+ ",\n"
			+ "die bei JKCEMU f\u00FCr die Systemspuren steht.\n"
			+ "Best\u00E4tigen Sie bitte, dass die emulierte"
			+ " Diskette\n"
			+ "keine Systemspuren enthalten soll.",
		"Hinweis",
		JOptionPane.OK_CANCEL_OPTION,
		JOptionPane.INFORMATION_MESSAGE ) != JOptionPane.OK_OPTION )
	    {
	      state = false;
	    }
	  }
	}
      }
      if( state ) {
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
				this.lastAutoRefresh,
				readOnly,
				this.lastForceLowerCase ),
		null );
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


  private boolean openDisk( int idx, Properties props )
  {
    boolean rv = false;
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
				this.lastAutoRefresh,
				EmuUtil.getBooleanProperty(
						props,
						prefix + "readonly",
						true ),
				EmuUtil.getBooleanProperty(
						props,
						prefix + "force_lowercase",
						true ) ),
		skipOddCyls );
	      rv = true;
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
	  rv = true;
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
	    rv = true;
	  }
	}
	else if( !resource.isEmpty() ) {
	  boolean done = false;
	  if( this.suitableDisks != null ) {
	    for( int i = 0; i < this.suitableDisks.length; i++ ) {
	      if( resource.equals( this.suitableDisks[ i ].getResource() ) ) {
		openDisk( idx, this.suitableDisks[ i ], skipOddCyls );
		done = true;
		rv   = true;
		break;
	      }
	    }
	  }
	  if( !done && (this.etcDisks != null) ) {
	    for( int i = 0; i < this.etcDisks.length; i++ ) {
	      if( resource.equals( this.etcDisks[ i ].getResource() ) ) {
		openDisk( idx, this.etcDisks[ i ], skipOddCyls );
		rv = true;
		break;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void openDrive(
			int              idx,
			String           fileName,
			boolean          readOnly,
			boolean          interactive,
			FloppyDiskFormat fmt,
			Boolean          skipOddCyls )
  {
    DeviceIO.RandomAccessDevice rad       = null;
    boolean                     done      = true;
    String                      errMsg    = null;
    String                      driveName = fileName;
    if( fileName.startsWith( "\\\\.\\" ) && (fileName.length() > 4) ) {
      driveName = fileName.substring( 4 );
    }
    try {
      rad = DeviceIO.openDeviceForRandomAccess( fileName, readOnly );
      Main.setLastDriveFileName( fileName );

      // Format erfragen
      int     diskSize   = DiskUtil.readDiskSize( rad );
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
	    diskSizeOK = DiskUtil.equalsDiskSize( rad, fmt.getDiskSize() );
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
      if( (fmt != null) && (rad != null) ) {
	done = setDisk(
		idx,
		"Laufwerk: " + driveName,
		PlainFileFloppyDisk.createForDrive(
					this,
					fileName,
					rad,
					readOnly,
					fmt ),
		skipOddCyls );
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
	EmuUtil.doClose( rad );
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
	      if( setDisk(
			idx,
			"Anadisk-Datei: " + file.getPath(),
			disk,
			skipOddCyls ) )
	      {
		Main.setLastFile( file, "disk" );
	      }
	    }
	  }
	} else if( EmuUtil.endsWith( fileName, DiskUtil.gzAnadiskFileExt ) ) {
	  AnadiskFloppyDisk disk = AnadiskFloppyDisk.readFile( this, file );
	  if( disk != null ) {
	    if( setDisk(
			idx,
			"Komprimierte Anadisk-Datei: " + file.getPath(),
			disk,
			skipOddCyls ) )
	    {
	      Main.setLastFile( file, "disk" );
	    }
	  }
	} else if( EmuUtil.endsWith( fileName, DiskUtil.copyQMFileExt ) ) {
	  CopyQMFloppyDisk disk = CopyQMFloppyDisk.readFile( this, file );
	  if( disk != null ) {
	    if( setDisk(
			idx,
			"CopyQM-Datei: " + file.getPath(),
			disk,
			skipOddCyls ) )
	    {
	      Main.setLastFile( file, "disk" );
	    }
	  }
	} else if( EmuUtil.endsWith(
				fileName,
				DiskUtil.gzCopyQMFileExt ) )
	{
	  CopyQMFloppyDisk disk = CopyQMFloppyDisk.readFile( this, file );
	  if( disk != null ) {
	    if( setDisk(
			idx,
			"Komprimierte CopyQM-Datei: " + file.getPath(),
			disk,
			skipOddCyls ) )
	    {
	      Main.setLastFile( file, "disk" );
	    }
	  }
	} else if( EmuUtil.endsWith( fileName, DiskUtil.telediskFileExt ) ) {
	  TelediskFloppyDisk disk = TelediskFloppyDisk.readFile(
								this,
								file );
	  if( disk != null ) {
	    if( setDisk(
			idx,
			"Teledisk-Datei: " + file.getPath(),
			disk,
			skipOddCyls ) )
	    {
	      Main.setLastFile( file, "disk" );
	    }
	  }
	} else if( EmuUtil.endsWith(
				fileName,
				DiskUtil.gzTelediskFileExt ) )
	{
	  TelediskFloppyDisk disk = TelediskFloppyDisk.readFile( this, file );
	  if( disk != null ) {
	    if( setDisk(
			idx,
			"Komprimierte Teledisk-Datei: " + file.getPath(),
			disk,
			skipOddCyls ) )
	    {
	      Main.setLastFile( file, "disk" );
	    }
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
		if( setDisk(
			idx,
			"Komprimierte einfache Abbilddatei: " + fName,
			disk,
			skipOddCyls ) )
		{
		  Main.setLastFile( file, "disk" );
		}
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
		if( setDisk(
			idx,
			"Einfache Diskettenabbilddatei: " + file.getPath(),
			disk,
			skipOddCyls ) )
		{
		  Main.setLastFile( file, "disk" );
		}
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


  private void paintLED( Graphics g, int w, int h )
  {
    if( (w > 0) && (h > 0) ) {
      g.setColor( this.ledState ? Color.red : Color.gray );
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
	disk.doClose();
      }
    } else {
      removeDisk( idx );
      rv = true;
    }
    updRefreshBtn();
    return rv;
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


  private void showRefreshInfo()
  {
    JPanel panel = new JPanel( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.BOTH,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panel.add(
	new JLabel(
		"Vergessen Sie bitte nicht, umgehend auch in dem im Emulator"
			+ " laufenden Programm bzw. Betriebssystem" ),
	gbc );

    gbc.insets.top = 0;
    gbc.gridy++;
    panel.add(
	new JLabel(
		"die Diskette zu aktualisieren bzw. das erneute Einlesen"
			+ " des Directorys zu veranlassen!" ),
	gbc );

    gbc.gridy++;
    panel.add(
	new JLabel(
		"(bei CP/M-kompatiblen Betriebssystemen meistens"
			+ " mit CTRL-C bzw. Strg-C)" ),
	gbc );

    JCheckBox cb = new JCheckBox(
			"Diese Meldung zuk\u00FCnftig nicht mehr anzeigen",
			false );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    panel.add( cb, gbc );

    JOptionPane.showMessageDialog(
			this,
			panel,
			"Wichtiger Hinweis",
			JOptionPane.WARNING_MESSAGE );

    if( cb.isSelected() ) {
      this.refreshInfoEnabled = false;
    }
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
    this.mnuPopupRefresh.setEnabled( state );
  }
}

