/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Sonstiges Einstellungen
 */

package jkcemu.settings;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.DesktopHelper;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.ScreenFrm;
import jkcemu.base.deviceio.WinDeviceIO;
import jkcemu.disk.HardDiskListDlg;
import jkcemu.file.FileUtil;


public class EtcSettingsFld extends AbstractSettingsFld
{
  private static final String MSG_DELETE_CONFIG_DIR_MANUALLY
		= "Beenden Sie bitte den Emulator und l\u00F6schen Sie\n"
			+ "das Konfigurationsverzeichnis selbst.";

  private boolean      notified;
  private JRadioButton rbFileDlgEmu;
  private JRadioButton rbFileDlgSwing;
  private JRadioButton rbFileDlgNative;
  private JRadioButton rbSRAMInit00;
  private JRadioButton rbSRAMInitRandom;
  private JCheckBox    cbClearRFsOnPowerOn;
  private JCheckBox    cbReloadROMsOnPowerOn;
  private JTextField   fldConfigDir;
  private JButton      btnOpenConfigDir;
  private JButton      btnDeleteConfigDir;


  public EtcSettingsFld( SettingsFrm settingsFrm )
  {
    super( settingsFrm );
    this.notified = false;

    setLayout( new BorderLayout() );

    JPanel panel = GUIFactory.createPanel( new GridBagLayout() );
    add( GUIFactory.createScrollPane( panel ), BorderLayout.CENTER );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    panel.add(
	GUIFactory.createLabel( "Zu verwendenter Dateiauswahldialog:" ),
	gbc );

    ButtonGroup grpFileDlg = new ButtonGroup();

    this.rbFileDlgEmu = GUIFactory.createRadioButton(
				"JKCEMU-eigener Dateiauswahldialog",
				true );
    grpFileDlg.add( this.rbFileDlgEmu );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridy++;
    panel.add( this.rbFileDlgEmu, gbc );

    this.rbFileDlgSwing = GUIFactory.createRadioButton(
	"Java/Swing (bildet den Dateiauswahldialog"
		+ " des jeweiligen Erscheinungsbildes nach)" );
    grpFileDlg.add( this.rbFileDlgSwing );
    gbc.gridy++;
    panel.add( this.rbFileDlgSwing, gbc );

    this.rbFileDlgNative = GUIFactory.createRadioButton(
			"Nativer Dateiauswahldialog des Betriebssystems" );
    grpFileDlg.add( this.rbFileDlgNative );
    gbc.gridy++;
    panel.add( this.rbFileDlgNative, gbc );

    gbc.insets.top  = 20;
    gbc.insets.left = 5;
    gbc.gridy++;
    panel.add(
	GUIFactory.createLabel(
		"Statische RAM-Bereiche (SRAM) initialisieren mit:" ),
	gbc );

    ButtonGroup grpSRAMInit = new ButtonGroup();

    this.rbSRAMInit00 = GUIFactory.createRadioButton( "Nullbytes", true );
    grpSRAMInit.add( this.rbSRAMInit00 );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridy++;
    panel.add( this.rbSRAMInit00, gbc );

    this.rbSRAMInitRandom = GUIFactory.createRadioButton(
			"Zufallsmuster (entspricht Originalverhalten)" );
    grpSRAMInit.add( this.rbSRAMInitRandom );
    gbc.gridy++;
    panel.add( this.rbSRAMInitRandom, gbc );

    this.cbClearRFsOnPowerOn = GUIFactory.createCheckBox(
		"RAM-Floppies bei jedem \"Einschalten\" l\u00F6schen" );
    gbc.insets.top  = 15;
    gbc.insets.left = 5;
    gbc.gridy++;
    panel.add( this.cbClearRFsOnPowerOn, gbc );

    this.cbReloadROMsOnPowerOn = GUIFactory.createCheckBox(
		"Eingebundene ROM-Dateien bei jedem \"Einschalten\""
			+ " neu laden" );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    panel.add( this.cbReloadROMsOnPowerOn, gbc );

    File configDir = Main.getConfigDir();
    if( configDir != null ) {
      gbc.insets.top    = 15;
      gbc.insets.bottom = 0;
      gbc.gridy++;
      panel.add(
	GUIFactory.createLabel( "JKCEMU-Konfigurationsverzeichnis"
				+ " (Einstellungen und Profile)" ),
	gbc );

      this.fldConfigDir = GUIFactory.createTextField();
      this.fldConfigDir.setEditable( false );
      this.fldConfigDir.setText( configDir.getPath() );
      gbc.fill       = GridBagConstraints.HORIZONTAL;
      gbc.weightx    = 1.0;
      gbc.insets.top = 0;
      gbc.gridy++;
      panel.add( this.fldConfigDir, gbc );

      gbc.fill          = GridBagConstraints.NONE;
      gbc.weightx       = 0.0;
      gbc.insets.top    = 5;
      gbc.insets.bottom = 5;
      gbc.gridwidth     = 1;
      gbc.gridx         = 0;
      gbc.gridy++;
      if( DesktopHelper.isOpenSupported() ) {
	this.btnOpenConfigDir = GUIFactory.createButton( EmuUtil.TEXT_OPEN );
	this.btnOpenConfigDir.setEnabled( configDir.exists() );
	panel.add( this.btnOpenConfigDir, gbc );
	gbc.insets.left = 0;
	gbc.gridx++;
      } else {
	this.btnOpenConfigDir = null;
      }
      this.btnDeleteConfigDir = GUIFactory.createButton(
		"Alle Einstellungen l\u00F6schen"
			+ " und JKCEMU zur\u00FCcksetzen..." );
      this.btnDeleteConfigDir.setEnabled( configDir.exists() );
      panel.add( this.btnDeleteConfigDir, gbc );
    } else {
      this.fldConfigDir       = null;
      this.btnOpenConfigDir   = null;
      this.btnDeleteConfigDir = null;
    }
  }


  public void configDirExists()
  {
    if( this.btnDeleteConfigDir != null )
      this.btnDeleteConfigDir.setEnabled( true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.rbFileDlgEmu.addActionListener( this );
      this.rbFileDlgSwing.addActionListener( this );
      this.rbFileDlgNative.addActionListener( this );
      this.rbSRAMInit00.addActionListener( this );
      this.rbSRAMInitRandom.addActionListener( this );
      this.cbClearRFsOnPowerOn.addActionListener( this );
      this.cbReloadROMsOnPowerOn.addActionListener( this );
      if( this.btnOpenConfigDir != null ) {
	this.btnOpenConfigDir.addActionListener( this );
      }
      if( this.btnDeleteConfigDir != null ) {
	this.btnDeleteConfigDir.addActionListener( this );
      }
    }
  }


  @Override
  public void applyInput( Properties props, boolean selected )
  {
    String value = FileUtil.VALUE_FILEDIALOG_JKCEMU;
    if( this.rbFileDlgSwing.isSelected() ) {
      value = FileUtil.VALUE_FILEDIALOG_SWING;
    } else if( this.rbFileDlgNative.isSelected() ) {
      value = FileUtil.VALUE_FILEDIALOG_NATIVE;
    }
    props.setProperty( FileUtil.PROP_FILEDIALOG, value );
    props.setProperty(
		EmuUtil.PROP_SRAM_INIT,
		this.rbSRAMInit00.isSelected() ?
			EmuUtil.VALUE_SRAM_INIT_00
			: EmuUtil.VALUE_SRAM_INIT_RANDOM );
    props.setProperty(
		EmuThread.PROP_RF_CLEAR_ON_POWER_ON,
		Boolean.toString(
			this.cbClearRFsOnPowerOn.isSelected() ) );
    props.setProperty(
		EmuThread.PROP_EXT_ROM_RELOAD_ON_POWER_ON,
		Boolean.toString(
			this.cbReloadROMsOnPowerOn.isSelected() ) );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.btnOpenConfigDir ) {
	rv = true;
	doOpenConfigDir( false );
      }
      else if( src == this.btnDeleteConfigDir ) {
	rv = true;
	doDeleteConfigDir();
      }
      else if( src instanceof JToggleButton ) {
	rv = true;
	fireDataChanged();
      }
    }
    return rv;
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.rbFileDlgEmu.removeActionListener( this );
      this.rbFileDlgSwing.removeActionListener( this );
      this.rbFileDlgNative.removeActionListener( this );
      this.rbSRAMInit00.removeActionListener( this );
      this.rbSRAMInitRandom.removeActionListener( this );
      this.cbClearRFsOnPowerOn.removeActionListener( this );
      this.cbReloadROMsOnPowerOn.removeActionListener( this );
      if( this.btnOpenConfigDir != null ) {
	this.btnOpenConfigDir.removeActionListener( this );
      }
      if( this.btnDeleteConfigDir != null ) {
	this.btnDeleteConfigDir.removeActionListener( this );
      }
    }
  }


  @Override
  public void updFields( Properties props )
  {
    switch( EmuUtil.getProperty( props, FileUtil.PROP_FILEDIALOG ) ) {
      case FileUtil.VALUE_FILEDIALOG_NATIVE:
	this.rbFileDlgNative.setSelected( true );
	break;
      case FileUtil.VALUE_FILEDIALOG_SWING:
	this.rbFileDlgSwing.setSelected( true );
	break;
      default:
	this.rbFileDlgEmu.setSelected( true );
    }
    if( EmuUtil.isSRAMInit00( props ) ) {
      this.rbSRAMInit00.setSelected( true );
    } else {
      this.rbSRAMInitRandom.setSelected( true );
    }
    this.cbClearRFsOnPowerOn.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			EmuThread.PROP_RF_CLEAR_ON_POWER_ON,
			EmuThread.DEFAULT_RF_CLEAR_ON_POWER_ON ) );
    this.cbReloadROMsOnPowerOn.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			EmuThread.PROP_EXT_ROM_RELOAD_ON_POWER_ON,
			EmuThread.DEFAULT_EXT_ROM_RELOAD_ON_POWER_ON ) );
  }


	/* --- Aktionen --- */

  private void doDeleteConfigDir()
  {
    File configDir = Main.getConfigDir();
    if( configDir != null ) {
      if( BaseDlg.showYesNoDlg(
		this,
		"Sie k\u00F6nnen das JKCEMU-Konfigurationsverzeichnis"
			+ " mit allen Einstellungen\n"
			+ "l\u00F6schen und so den Emulator auf den Zustand"
			+ " zur\u00FCcksetzen,\n"
			+ "als w\u00FCrde er das erste mal auf diesem"
			+ " Computer gestartet werden.\n\n"
			+ "M\u00F6chten Sie das Konfigurationsverzeichnis"
			+ " jetzt l\u00F6schen?\n"
			+ "Dabei gehen alle gespeicherten Einstellungen"
			+ " und Profile sowie eventuell\n"
			+ "selbst erfasste Stammdaten f\u00FCr die"
			+ " zu emulierenden Festplatten verloren." ) )
      {
	boolean done  = false;
	boolean state = true;
	if( configDir.isDirectory() ) {
	  /*
	   * Auf Dateien und Unterverzeichniss pruefen,
	   * die offensichtlich nicht von JKCEMU stammen
	   */
	  File[] files = configDir.listFiles();
	  if( files != null ) {
	    for( int i = 0; i < files.length; i++ ) {
	      File file = files[ i ];
	      if( file.isDirectory() ) {
		state = false;
		break;
	      } else {
		String s = file.getName();
		if( s != null ) {
		  if( !s.equals( "." )
		      && !s.equals( ".." )
		      && !s.equals( HardDiskListDlg.HARDDISKS_FILE )
		      && !s.equals( WinDeviceIO.LIBNAME_WIN32 )
		      && !s.equals( WinDeviceIO.LIBNAME_WIN64 )
		      && !s.equals( WinDeviceIO.UPDNAME_WIN32 )
		      && !s.equals( WinDeviceIO.UPDNAME_WIN64 )
		      && !s.endsWith( Main.LASTDIRS_FILE )
		      && !(s.startsWith( "prf_" ) && s.endsWith( ".xml" )) )
		  {
		    state = false;
		    break;
		  }
		}
	      }
	    }
	    if( !state ) {
	      state = BaseDlg.showYesNoWarningDlg(
			this,
			"Das JKCEMU-Datenverzeichnis enth\u00E4lt Dateien"
				+ " und/oder Unterverzeichnisse,\n"
				+ "die offensichtlich nicht von JKCEMU"
				+ " stammen.\n"
				+ "Soll das Verzeichnis trotzdem"
				+ " gel\u00F6scht werden?",
			"Warnung" );
	    }
	    if( state ) {
	      DeviceIO.LibInfo libInfo = DeviceIO.getLibInfo();
	      if( libInfo != null ) {
		File libFile = libInfo.getFile();
		if( (libInfo.getStatus() != DeviceIO.LibStatus.NOT_USED)
		    && (libFile != null) )
		{
		  File libDir = libFile.getParentFile();
		  if( libDir != null ) {
		    if( libDir.equals( configDir ) ) {
		      BaseDlg.showErrorDlg(
			this,
			"Im Konfigurationsverzeichnis befindet sich"
				+ " eine Bibliothek,\n"
				+ "die durch JKCEDMU selbst verwendet wird.\n"
				+ "Aus diesem Grund kann das Verzeichnis"
				+ " erst nach\n"
				+ "dem Schlie\u00DFen gel\u00F6scht"
				+ " werden.\n\n"
				+ MSG_DELETE_CONFIG_DIR_MANUALLY );
		      doOpenConfigDir( true );
		      state = false;
		    }
		  }
		}
	      }
	    }
	    if( state ) {
	      done = deleteDir( configDir );
	    }
	  }
	}
	if( state ) {
	  if( done ) {
	    if( this.btnDeleteConfigDir != null ) {
	      this.btnDeleteConfigDir.setEnabled( false );
	    }
	    if( BaseDlg.showYesNoWarningDlg(
		this,
		"Wenn Sie sichergehen wollen,\n"
			+ "dass keine alten Einstellungen \u00FCbernommen"
			+ " werden,\n"
			+ "sollten Sie jetzt den Emulator beenden.\n\n"
			+ "M\u00F6chten Sie den Emulator jetzt beenden?",
		"Hinweis" ) )
	    {
	      this.settingsFrm.getScreenFrm().doQuit();
	    }
	  } else {
	    BaseDlg.showErrorDlg(
		this,
		"Das JKCEMU-Konfigurationsverzeichnis"
			+ " konnte nicht gel\u00F6scht werden.\n"
			+ MSG_DELETE_CONFIG_DIR_MANUALLY );
	    doOpenConfigDir( true );
	  }
	}
      }
    }
  }


  private void doOpenConfigDir( boolean suppressErrMsg )
  {
    boolean done      = false;
    File    configDir = Main.getConfigDir();
    if( configDir != null ) {
      try {
	DesktopHelper.open( configDir );
	done = true;
      }
      catch( IOException ex ) {}
    }
    if( !done && !suppressErrMsg ) {
      BaseDlg.showErrorDlg(
		this,
		"Das Konfigurationsverzeichnis konnte nicht"
			+ " ge\u00F6ffnet werden." );
    }
  }


	/* --- private Methoden --- */

  private static boolean deleteDir( File dirFile )
  {
    boolean rv = false;
    try {
      Files.walkFileTree(
		dirFile.toPath(),
		new SimpleFileVisitor<Path>()
		{
		  @Override
		  public FileVisitResult postVisitDirectory(
						Path        path,
						IOException ex )
					throws IOException
		  {
		    if( ex != null ) {
		      throw ex;
		    }
		    Files.deleteIfExists( path );
		    return FileVisitResult.CONTINUE;
		  }

		  @Override
		  public FileVisitResult visitFile(
						Path                path,
						BasicFileAttributes attrs )
					throws IOException
		  {
		    Files.deleteIfExists( path );
		    return FileVisitResult.CONTINUE;
		  }
		} );
      rv = true;
    }
    catch( Exception ex ) {}
    return rv;
  }
}
