/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer die Einstellungen
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.Document;
import jkcemu.Main;
import jkcemu.emusys.a5105.A5105SettingsFld;
import jkcemu.emusys.ac1_llc2.*;
import jkcemu.emusys.etc.*;
import jkcemu.emusys.huebler.HueblerGraphicsMCSettingsFld;
import jkcemu.emusys.kc85.KC85SettingsFld;
import jkcemu.emusys.kccompact.KCcompactSettingsFld;
import jkcemu.emusys.lc80.LC80SettingsFld;
import jkcemu.emusys.poly880.Poly880SettingsFld;
import jkcemu.emusys.z1013.Z1013SettingsFld;
import jkcemu.emusys.z9001.Z9001SettingsFld;
import jkcemu.emusys.zxspectrum.ZXSpectrumSettingsFld;
import jkcemu.net.KCNetSettingsFld;


public class SettingsFrm extends BasicFrm
			implements
				ChangeListener,
				DocumentListener
{
  public static final int DEFAULT_BRIGHTNESS = 80;

  private static final int MAX_MARGIN = 199;

  private static SettingsFrm instance = null;

  private ScreenFrm                    screenFrm;
  private EmuThread                    emuThread;
  private File                         profileFile;
  private Map<String,AbstractButton>   lafClass2Button;
  private SpinnerNumberModel           spinnerModelMargin;
  private KCNetSettingsFld             tabKCNet;
  private JPanel                       tabConfirm;
  private JPanel                       tabEtc;
  private JPanel                       tabLAF;
  private JPanel                       tabScreen;
  private JPanel                       tabSpeed;
  private JPanel                       tabSys;
  private JPanel                       panelSysOpt;
  private JCheckBox                    btnConfirmNMI;
  private JCheckBox                    btnConfirmReset;
  private JCheckBox                    btnConfirmPowerOn;
  private JCheckBox                    btnConfirmQuit;
  private JCheckBox                    btnClearRFsOnPowerOn;
  private JCheckBox                    btnReloadROMsOnPowerOn;
  private JComboBox<String>            comboScreenRefresh;
  private CardLayout                   cardLayoutSysOpt;
  private String                       curSysOptCard;
  private JRadioButton                 btnSysA5105;
  private JRadioButton                 btnSysAC1;
  private JRadioButton                 btnSysBCS3;
  private JRadioButton                 btnSysC80;
  private JRadioButton                 btnSysHC900;
  private JRadioButton                 btnSysHEMC;
  private JRadioButton                 btnSysHGMC;
  private JRadioButton                 btnSysKC85_1;
  private JRadioButton                 btnSysKC85_2;
  private JRadioButton                 btnSysKC85_3;
  private JRadioButton                 btnSysKC85_4;
  private JRadioButton                 btnSysKC85_5;
  private JRadioButton                 btnSysKC87;
  private JRadioButton                 btnSysKCcompact;
  private JRadioButton                 btnSysKramerMC;
  private JRadioButton                 btnSysLC80;
  private JRadioButton                 btnSysLLC1;
  private JRadioButton                 btnSysLLC2;
  private JRadioButton                 btnSysNANOS;
  private JRadioButton                 btnSysPCM;
  private JRadioButton                 btnSysPoly880;
  private JRadioButton                 btnSysSC2;
  private JRadioButton                 btnSysSLC1;
  private JRadioButton                 btnSysVCS80;
  private JRadioButton                 btnSysZ1013;
  private JRadioButton                 btnSysZ9001;
  private JRadioButton                 btnSysZXSpectrum;
  private JCheckBox                    btnHEMCCatchPrintCalls;
  private JCheckBox                    btnKramerMCCatchPrintCalls;
  private JRadioButton                 btnLC80_U505;
  private JRadioButton                 btnLC80_2716;
  private JRadioButton                 btnLC80_2;
  private JRadioButton                 btnLC80e;
  private A5105SettingsFld             a5105SettingsFld;
  private AC1SettingsFld               ac1SettingsFld;
  private LLC2SettingsFld              llc2SettingsFld;
  private BCS3SettingsFld              bcs3SettingsFld;
  private HueblerGraphicsMCSettingsFld hgmcSettingsFld;
  private KC85SettingsFld              hc900SettingsFld;
  private Z9001SettingsFld             kc85_1_SettingsFld;
  private KC85SettingsFld              kc85_2_SettingsFld;
  private KC85SettingsFld              kc85_3_SettingsFld;
  private KC85SettingsFld              kc85_4_SettingsFld;
  private KC85SettingsFld              kc85_5_SettingsFld;
  private KCcompactSettingsFld         kcCompactSettingsFld;
  private LC80SettingsFld              lc80SettingsFld;
  private NANOSSettingsFld             nanosSettingsFld;
  private PCMSettingsFld               pcmSettingsFld;
  private Poly880SettingsFld           poly880SettingsFld;
  private Z1013SettingsFld             z1013SettingsFld;
  private Z9001SettingsFld             kc87SettingsFld;
  private Z9001SettingsFld             z9001SettingsFld;
  private ZXSpectrumSettingsFld        zxSpectrumSettingsFld;
  private JRadioButton                 btnSpeedDefault;
  private JRadioButton                 btnSpeedValue;
  private JRadioButton                 btnSRAMInit00;
  private JRadioButton                 btnSRAMInitRandom;
  private JRadioButton                 btnFileDlgEmu;
  private JRadioButton                 btnFileDlgNative;
  private JCheckBox                    btnDirectCopyPaste;
  private JLabel                       labelSpeedUnit;
  private JTextField                   fldSpeed;
  private Document                     docSpeed;
  private NumberFormat                 fmtSpeed;
  private JTextField                   fldConfigDir;
  private JButton                      btnDeleteConfigDir;
  private JSlider                      sliderBrightness;
  private JSpinner                     spinnerMargin;
  private ButtonGroup                  grpLAF;
  private UIManager.LookAndFeelInfo[]  lafs;
  private JButton                      btnApply;
  private JButton                      btnLoad;
  private JButton                      btnSave;
  private JButton                      btnHelp;
  private JButton                      btnClose;
  private JTabbedPane                  tabbedPane;


  public static void open( ScreenFrm screenFrm )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new SettingsFrm( screenFrm );
    }
    instance.toFront();
    instance.setVisible( true );
  }


  public void fireDataChanged()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    setDataChanged();
		  }
		} );
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( (src == this.sliderBrightness) || (src == this.spinnerMargin) )
      setDataChanged();
  }


	/* --- DocumentListener --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    updFields( props, UIManager.getLookAndFeel(), null );
    return super.applySettings( props, resizable );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      if( e != null ) {
	Object src = e.getSource();
	if( src != null ) {
	  if( src == this.btnApply ) {
	    rv = true;
	    doApply();
	  }
	  else if( src == this.btnLoad ) {
	    rv = true;
	    doLoad();
	  }
	  else if( src == this.btnSave ) {
	    rv = true;
	    doSave();
	  }
	  else if( src == this.btnHelp ) {
	    rv = true;
	    HelpFrm.open( "/help/settings.htm" );
	  }
	  else if( src == this.btnClose ) {
	    rv = true;
	    doClose();
	  }
	  else if( src == this.btnDeleteConfigDir ) {
	    rv = true;
	    doDeleteConfigDir();
	  }
	  else if( (src == this.btnSysA5105)
		   || (src == this.btnSysAC1)
		   || (src == this.btnSysBCS3)
		   || (src == this.btnSysC80)
		   || (src == this.btnSysHC900)
		   || (src == this.btnSysHEMC)
		   || (src == this.btnSysHGMC)
		   || (src == this.btnSysKC85_1)
		   || (src == this.btnSysKC85_2)
		   || (src == this.btnSysKC85_3)
		   || (src == this.btnSysKC85_4)
		   || (src == this.btnSysKC85_5)
		   || (src == this.btnSysKC87)
		   || (src == this.btnSysKCcompact)
		   || (src == this.btnSysKramerMC)
		   || (src == this.btnSysLC80)
		   || (src == this.btnSysLLC1)
		   || (src == this.btnSysLLC2)
		   || (src == this.btnSysNANOS)
		   || (src == this.btnSysPCM)
		   || (src == this.btnSysPoly880)
		   || (src == this.btnSysSC2)
		   || (src == this.btnSysSLC1)
		   || (src == this.btnSysVCS80)
		   || (src == this.btnSysZ1013)
		   || (src == this.btnSysZ9001)
		   || (src == this.btnSysZXSpectrum) )
	  {
	    rv = true;
	    updSysOptCard();
	    setDataChanged();
	  }
	  else if( (src == this.btnSpeedDefault)
		   || (src == this.btnSpeedValue) )
	  {
	    rv = true;
	    updSpeedFieldsEnabled();
	    setDataChanged();
	  }
	  else if( (src instanceof JCheckBox)
		   || (src instanceof JComboBox)
		   || (src instanceof JRadioButton) )
	  {
	    rv = true;
	    setDataChanged();
	  }
	}
      }
    }
    catch( UserInputException ex ) {
      BasicDlg.showErrorDlg( this, ex );
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.tabKCNet.lookAndFeelChanged();
    this.a5105SettingsFld.lookAndFeelChanged();
    this.ac1SettingsFld.lookAndFeelChanged();
    this.llc2SettingsFld.lookAndFeelChanged();
    this.hc900SettingsFld.lookAndFeelChanged();
    this.hgmcSettingsFld.lookAndFeelChanged();
    this.kc85_1_SettingsFld.lookAndFeelChanged();
    this.kc85_2_SettingsFld.lookAndFeelChanged();
    this.kc85_3_SettingsFld.lookAndFeelChanged();
    this.kc85_4_SettingsFld.lookAndFeelChanged();
    this.kc85_5_SettingsFld.lookAndFeelChanged();
    this.z1013SettingsFld.lookAndFeelChanged();
    this.kc87SettingsFld.lookAndFeelChanged();
    this.z9001SettingsFld.lookAndFeelChanged();
    this.zxSpectrumSettingsFld.lookAndFeelChanged();
  }


  @Override
  public void setVisible( boolean state )
  {
    if( state && !isVisible() ) {
      updFields(
		Main.getProperties(),
		UIManager.getLookAndFeel(),
		Integer.toString( this.screenFrm.getScreenRefreshMillis() ) );
    }
    super.setVisible( state );
  }


	/* --- Aktionen --- */

  private void doApply() throws UserInputException
  {
    Properties props    = new Properties();
    Properties oldProps = Main.getProperties();
    if( oldProps != null ) {
      props.putAll( oldProps );
    }
    Component tab = null;
    try {
      tab = this.tabConfirm;
      applyConfirm( props );

      tab = this.tabEtc;
      applyEtc( props );

      tab = this.tabKCNet;
      this.tabKCNet.applyInput(
		props,
		this.tabbedPane.getSelectedComponent() == this.tabKCNet );

      tab = this.tabScreen;
      applyScreen( props );

      tab = this.tabSpeed;
      applySpeed( props );

      tab = this.tabSys;
      applySys( props );

      /*
       * Das Look&Feel als letztes setzen,
       * Anderenfalls koennte noch eine nachfolgende Aktion einen Fehler
       * erzeugen und das Uebernehmen der Einstellungen abbrechen.
       * In dem Fall waere das neue Erscheinungsbild schon eingestellt,
       * was vom Programmverhalten her inkonsistent waere.
       */
      tab = this.tabLAF;
      applyLAF( props );

      // neue Eigenschaften anwenden
      Properties appProps = Main.getProperties();
      if( appProps != null ) {
	appProps.putAll( props );
      } else {
	appProps = props;
      }
      this.emuThread.applySettings( appProps );
      Main.applyProfileToFrames( this.profileFile, appProps, false, this );

      if( !this.btnSpeedValue.isSelected() ) {
	EmuSys emuSys = this.emuThread.getEmuSys();
	setSpeedValueFld( EmuThread.getDefaultSpeedKHz( props ) );
      }

      this.btnApply.setEnabled( false );
      if( this.btnSave != null ) {
	this.btnSave.setEnabled( true );
      }
    }
    catch( UserCancelException ex ) {}
    catch( UserInputException ex ) {
      if( tab != null ) {
	this.tabbedPane.setSelectedComponent( tab );
      }
      throw ex;
    }
  }


  private void doDeleteConfigDir()
  {
    File configDir = Main.getConfigDir();
    if( configDir != null ) {
      if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie das JKCEMU-Konfigurationsverzeichnis"
			+ " l\u00F6schen?\n"
			+ "Dabei gehen alle gespeicherten Einstellungen"
			+ " und Profile sowie eventuell\n"
			+ "selbst erfasste Stammdaten f\u00FCr die"
			+ " zu emulierenden Festplatten verloren.\n"
			+ "Beim n\u00E4chsten Start meldet sich der Emulator"
			+ " dann so,\n"
			+ "als w\u00FCrde er das erste mal"
			+ " gestartet werden.\n" ) )
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
		      && !s.equals( "harddisks.csv" )
		      && !s.equals( "jkcemu_win32.dll" )
		      && !s.equals( "jkcemu_win64.dll" )
		      && !s.endsWith( "lastdirs.xml" )
		      && !s.endsWith( "wdnsfile.exe" )
		      && !(s.startsWith( "prf_" ) && s.endsWith( ".xml" )) )
		  {
		    state = false;
		    break;
		  }
		}
	      }
	    }
	    if( !state ) {
	      state = BasicDlg.showYesNoWarningDlg(
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
	      done = deleteDir( configDir );
	    }
	  }
	}
	if( state ) {
	  if( done ) {
	    if( this.btnDeleteConfigDir != null ) {
	      this.btnDeleteConfigDir.setEnabled( false );
	    }
	    if( BasicDlg.showYesNoWarningDlg(
		this,
		"Wenn Sie sichergehen wollen,\n"
			+ "dass keine alten Einstellungen \u00FCbernommen"
			+ " werden,\n"
			+ "sollten Sie jetzt den Emulator beenden.\n\n"
			+ "M\u00F6chten Sie den Emulator jetzt beenden?",
		"Hinweis" ) )
	    {
	      this.screenFrm.doQuit();
	    }
	  } else {
	    BasicDlg.showErrorDlg(
		this,
		"Das JKCEMU-Konfigurationsverzeichnis konnte nicht"
			+ " gel\u00F6scht werden.\n"
			+ "M\u00F6glicherweise sind einige Dateien"
			+ " durch den Emulator selbst gesperrt.\n"
			+ "Schlie\u00DFen Sie bitte JKCEMU und"
			+ " l\u00F6schen Sie das Verzeichnis per Hand." );
	    if( Desktop.isDesktopSupported() ) {
	      try {
		Desktop.getDesktop().open( configDir );
	      }
	      catch( Exception ex ) {}
	    }
	  }
	}
      }
    }
  }


  private void doLoad()
  {
    ProfileDlg dlg = new ProfileDlg(
				this,
				"Profil laden",
				"Laden",
				Main.getProfileFile(),
				false );
    dlg.setVisible( true );
    File file = dlg.getSelectedProfile();
    if( file != null ) {
      Properties props = Main.loadProperties( file );
      if( props != null ) {
        updFields( props, null, null );
        setDataChanged();
        this.profileFile = file;
      }
    }
  }


  private void doSave()
  {
    Properties props = Main.getProperties();
    if( props == null ) {
      props = new Properties();
    }

    // Profile-Auswahlbox
    ProfileDlg dlg = new ProfileDlg(
				this,
				"Profil speichern",
				"Speichern",
				this.profileFile,
				true );
    dlg.setVisible( true );
    File profileFile = dlg.getSelectedProfile();
    if( profileFile != null ) {

      // Eigenschaften sammeln
      Frame[] frms = Frame.getFrames();
      if( frms != null ) {
	for( int i = 0; i < frms.length; i++ ) {
	  Frame f = frms[ i ];
	  if( f != null ) {
	    if( f instanceof BasicFrm ) {
	      ((BasicFrm) f).putSettingsTo( props );
	    }
	  }
	}
      }

      // ggf. Verzeichnis anlegen
      File configDir = profileFile.getParentFile();
      if( configDir != null ) {
	if( configDir.exists() ) {
	  if( this.btnDeleteConfigDir != null ) {
	    this.btnDeleteConfigDir.setEnabled( true );
	  }
	}
      }

      // eigentliches Speichern
      OutputStream out  = null;
      try {
	out = new FileOutputStream( profileFile );
	props.storeToXML( out, "JKCEMU Profil" );
	out.close();
	out = null;
	this.profileFile = profileFile;
	Main.setProfile( this.profileFile, props );
      }
      catch( IOException ex ) {
	BasicDlg.showErrorDlg(
		this,
		"Die Einstellungen k\u00F6nnen nicht in die Datei\n\'"
			+ profileFile.getPath()
			+ "\'\ngespeichert werden." );
      }
      finally {
	EmuUtil.doClose( out );
      }
    }
  }


	/* --- Konstruktor --- */

  private SettingsFrm( ScreenFrm screenFrm )
  {
    setTitle( "JKCEMU Einstellungen" );
    Main.updIcon( this );
    this.screenFrm       = screenFrm;
    this.emuThread       = screenFrm.getEmuThread();
    this.lafClass2Button = new HashMap<>();
    this.profileFile     = Main.getProfileFile();
    this.fmtSpeed        = NumberFormat.getNumberInstance();
    if( this.fmtSpeed instanceof DecimalFormat ) {
      ((DecimalFormat) this.fmtSpeed).applyPattern( "#0.0##" );
    }


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.WEST,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, gbc );


    // Bereich System
    this.tabSys = new JPanel( new BorderLayout() );
    this.tabbedPane.addTab( "System", tabSys );

    JPanel panelSys = new JPanel( new GridBagLayout() );
    tabSys.add( new JScrollPane( panelSys ), BorderLayout.WEST );

    GridBagConstraints gbcSys = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpSys = new ButtonGroup();

    this.btnSysA5105 = new JRadioButton( "A5105 (BIC / ALBA PC)", true );
    this.btnSysA5105.addActionListener( this );
    grpSys.add( this.btnSysA5105 );
    panelSys.add( this.btnSysA5105, gbcSys );

    this.btnSysAC1 = new JRadioButton( "AC1", false );
    this.btnSysAC1.addActionListener( this );
    grpSys.add( this.btnSysAC1 );
    gbcSys.insets.top = 0;
    gbcSys.gridy++;
    panelSys.add( this.btnSysAC1, gbcSys );

    this.btnSysBCS3 = new JRadioButton( "BCS3", false );
    this.btnSysBCS3.addActionListener( this );
    grpSys.add( this.btnSysBCS3 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysBCS3, gbcSys );

    this.btnSysC80 = new JRadioButton( "C-80", false );
    this.btnSysC80.addActionListener( this );
    grpSys.add( this.btnSysC80 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysC80, gbcSys );

    this.btnSysHC900 = new JRadioButton( "HC900", false );
    this.btnSysHC900.addActionListener( this );
    grpSys.add( this.btnSysHC900 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysHC900, gbcSys );

    this.btnSysHEMC = new JRadioButton( "H\u00FCbler/Evert-MC", false );
    this.btnSysHEMC.addActionListener( this );
    grpSys.add( this.btnSysHEMC );
    gbcSys.gridy++;
    panelSys.add( this.btnSysHEMC, gbcSys );

    this.btnSysHGMC = new JRadioButton( "H\u00FCbler-Grafik-MC", false );
    this.btnSysHGMC.addActionListener( this );
    grpSys.add( this.btnSysHGMC );
    gbcSys.gridy++;
    panelSys.add( this.btnSysHGMC, gbcSys );

    this.btnSysKC85_1 = new JRadioButton( "KC85/1", false );
    this.btnSysKC85_1.addActionListener( this );
    grpSys.add( this.btnSysKC85_1 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC85_1, gbcSys );

    this.btnSysKC85_2 = new JRadioButton( "KC85/2", false );
    this.btnSysKC85_2.addActionListener( this );
    grpSys.add( this.btnSysKC85_2 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC85_2, gbcSys );

    this.btnSysKC85_3 = new JRadioButton( "KC85/3", false );
    this.btnSysKC85_3.addActionListener( this );
    grpSys.add( this.btnSysKC85_3 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC85_3, gbcSys );

    this.btnSysKC85_4 = new JRadioButton( "KC85/4", false );
    this.btnSysKC85_4.addActionListener( this );
    grpSys.add( this.btnSysKC85_4 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC85_4, gbcSys );

    this.btnSysKC85_5 = new JRadioButton( "KC85/5", false );
    this.btnSysKC85_5.addActionListener( this );
    grpSys.add( this.btnSysKC85_5 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC85_5, gbcSys );

    this.btnSysKC87 = new JRadioButton( "KC87", false );
    this.btnSysKC87.addActionListener( this );
    grpSys.add( this.btnSysKC87 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC87, gbcSys );

    this.btnSysKCcompact = new JRadioButton( "KC compact", false );
    this.btnSysKCcompact.addActionListener( this );
    grpSys.add( this.btnSysKCcompact );
    gbcSys.insets.bottom = 5;
    gbcSys.gridy++;
    panelSys.add( this.btnSysKCcompact, gbcSys );

    this.btnSysKramerMC = new JRadioButton( "Kramer-MC", false );
    this.btnSysKramerMC.addActionListener( this );
    grpSys.add( this.btnSysKramerMC );
    gbcSys.insets.top    = 5;
    gbcSys.insets.bottom = 0;
    gbcSys.gridy         = 0;
    gbcSys.gridx++;
    panelSys.add( this.btnSysKramerMC, gbcSys );

    this.btnSysLC80 = new JRadioButton( "LC-80", false );
    this.btnSysLC80.addActionListener( this );
    grpSys.add( this.btnSysLC80 );
    gbcSys.insets.top = 0;
    gbcSys.gridy++;
    panelSys.add( this.btnSysLC80, gbcSys );

    this.btnSysLLC1 = new JRadioButton( "LLC1", false );
    this.btnSysLLC1.addActionListener( this );
    grpSys.add( this.btnSysLLC1 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysLLC1, gbcSys );

    this.btnSysLLC2 = new JRadioButton( "LLC2", false );
    this.btnSysLLC2.addActionListener( this );
    grpSys.add( this.btnSysLLC2 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysLLC2, gbcSys );

    this.btnSysNANOS = new JRadioButton( "NANOS", false );
    this.btnSysNANOS.addActionListener( this );
    grpSys.add( this.btnSysNANOS );
    gbcSys.gridy++;
    panelSys.add( this.btnSysNANOS, gbcSys );

    this.btnSysPCM = new JRadioButton( "PC/M", false );
    this.btnSysPCM.addActionListener( this );
    grpSys.add( this.btnSysPCM );
    gbcSys.gridy++;
    panelSys.add( this.btnSysPCM, gbcSys );

    this.btnSysPoly880 = new JRadioButton( "Poly-880", false );
    this.btnSysPoly880.addActionListener( this );
    grpSys.add( this.btnSysPoly880 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysPoly880, gbcSys );

    this.btnSysSC2 = new JRadioButton( "SC2", false );
    this.btnSysSC2.addActionListener( this );
    grpSys.add( this.btnSysSC2 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysSC2, gbcSys );

    this.btnSysSLC1 = new JRadioButton( "SLC1", false );
    this.btnSysSLC1.addActionListener( this );
    grpSys.add( this.btnSysSLC1 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysSLC1, gbcSys );

    this.btnSysVCS80 = new JRadioButton( "VCS80", false );
    this.btnSysVCS80.addActionListener( this );
    grpSys.add( this.btnSysVCS80 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysVCS80, gbcSys );

    this.btnSysZ1013 = new JRadioButton( "Z1013", false );
    this.btnSysZ1013.addActionListener( this );
    grpSys.add( this.btnSysZ1013 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysZ1013, gbcSys );

    this.btnSysZ9001 = new JRadioButton( "Z9001", false );
    this.btnSysZ9001.addActionListener( this );
    grpSys.add( this.btnSysZ9001 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysZ9001, gbcSys );

    this.btnSysZXSpectrum = new JRadioButton( "ZX Spectrum", false );
    this.btnSysZXSpectrum.addActionListener( this );
    grpSys.add( this.btnSysZXSpectrum );
    gbcSys.gridy++;
    panelSys.add( this.btnSysZXSpectrum, gbcSys );


    // Optionen
    this.cardLayoutSysOpt = new CardLayout( 5, 5);
    this.curSysOptCard    = null;

    this.panelSysOpt = new JPanel( this.cardLayoutSysOpt );
    this.panelSysOpt.setBorder(
		BorderFactory.createTitledBorder( "Optionen" ) );
    gbcSys.anchor     = GridBagConstraints.CENTER;
    gbcSys.fill       = GridBagConstraints.BOTH;
    gbcSys.weightx    = 1.0;
    gbcSys.weighty    = 1.0;
    gbcSys.gridheight = GridBagConstraints.REMAINDER;
    gbcSys.gridy      = 0;
    gbcSys.gridx++;
    tabSys.add( this.panelSysOpt, BorderLayout.CENTER );


    JPanel panelNoOpt = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelNoOpt, "noopt" );

    GridBagConstraints gbcNoOpt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelNoOpt.add(
		new JLabel( "Keine Optionen verf\u00FCgbar" ),
		gbcNoOpt );


    // Optionen fuer A5105
    this.a5105SettingsFld = new A5105SettingsFld( this, "jkcemu.a5105." );
    this.panelSysOpt.add( this.a5105SettingsFld, "A5105" );


    // Optionen fuer AC1
    this.ac1SettingsFld = new AC1SettingsFld( this, "jkcemu.ac1." );
    this.panelSysOpt.add( this.ac1SettingsFld, "AC1" );


    // Optionen fuer BCS3
    this.bcs3SettingsFld = new BCS3SettingsFld( this, "jkcemu.bcs3." );
    this.panelSysOpt.add( this.bcs3SettingsFld, "BCS3" );


    // Optionen fuer HC900
    this.hc900SettingsFld = new KC85SettingsFld( this, "jkcemu.hc900.", 2 );
    this.panelSysOpt.add( this.hc900SettingsFld, "HC900" );


    // Optionen fuer Huebler/Evert-MC
    JPanel panelHEMC = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelHEMC, "HEMC" );

    GridBagConstraints gbcHEMC = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.btnHEMCCatchPrintCalls = new JCheckBox(
		"Betriebssystemaufrufe f\u00FCr Druckerausgaben abfangen",
		true );
    this.btnHEMCCatchPrintCalls.addActionListener( this );
    panelHEMC.add( this.btnHEMCCatchPrintCalls, gbcHEMC );


    // Optionen fuer Huebler-Grafik-MC
    this.hgmcSettingsFld = new HueblerGraphicsMCSettingsFld(
							this,
							"jkcemu.hgmc." );
    this.panelSysOpt.add( this.hgmcSettingsFld, "HGMC" );


    // Optionen fuer KC85/1..5
    this.kc85_1_SettingsFld = new Z9001SettingsFld(
					this,
					"jkcemu.kc85_1.",
					false );
    this.panelSysOpt.add( this.kc85_1_SettingsFld, "KC85/1" );

    this.kc85_2_SettingsFld = new KC85SettingsFld( this, "jkcemu.kc85_2.", 2 );
    this.panelSysOpt.add( this.kc85_2_SettingsFld, "KC85/2" );

    this.kc85_3_SettingsFld = new KC85SettingsFld( this, "jkcemu.kc85_3.", 3 );
    this.panelSysOpt.add( this.kc85_3_SettingsFld, "KC85/3" );

    this.kc85_4_SettingsFld = new KC85SettingsFld( this, "jkcemu.kc85_4.", 4 );
    this.panelSysOpt.add( this.kc85_4_SettingsFld, "KC85/4" );

    this.kc85_5_SettingsFld = new KC85SettingsFld( this, "jkcemu.kc85_5.", 5 );
    this.panelSysOpt.add( this.kc85_5_SettingsFld, "KC85/5" );


    // Optionen fuer KC87
    this.kc87SettingsFld = new Z9001SettingsFld(
					this,
					"jkcemu.kc87.",
					true );
    this.panelSysOpt.add( this.kc87SettingsFld, "KC87" );


    // Optionen fuer KC compact
    this.kcCompactSettingsFld = new KCcompactSettingsFld(
						this,
						"jkcemu.kccompact." );
    this.panelSysOpt.add( this.kcCompactSettingsFld, "KCcompact" );


    // Optionen fuer Kramer-MC
    JPanel panelKramerMC = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelKramerMC, "KramerMC" );

    GridBagConstraints gbcKramerMC = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.btnKramerMCCatchPrintCalls = new JCheckBox(
		"Betriebssystemaufrufe f\u00FCr Druckerausgaben abfangen",
		false );
    this.btnKramerMCCatchPrintCalls.addActionListener( this );
    panelKramerMC.add( this.btnKramerMCCatchPrintCalls, gbcKramerMC );


    // Optionen fuer LC80
    this.lc80SettingsFld = new LC80SettingsFld( this, "jkcemu.lc80." );
    this.panelSysOpt.add( this.lc80SettingsFld, "LC80" );


    // Optionen fuer LLC2
    this.llc2SettingsFld = new LLC2SettingsFld( this, "jkcemu.llc2." );
    this.panelSysOpt.add( this.llc2SettingsFld, "LLC2" );


    // Optionen fuer NANOS
    this.nanosSettingsFld = new NANOSSettingsFld( this, "jkcemu.nanos." );
    this.panelSysOpt.add( this.nanosSettingsFld, "NANOS" );


    // Optionen fuer PC/M
    this.pcmSettingsFld = new PCMSettingsFld( this, "jkcemu.pcm." );
    this.panelSysOpt.add( this.pcmSettingsFld, "PC/M" );


    // Optionen fuer Poly880
    this.poly880SettingsFld = new Poly880SettingsFld(
					this,
					"jkcemu.poly880." );
    this.panelSysOpt.add( this.poly880SettingsFld, "Poly880" );


    // Optionen fuer Z1013
    this.z1013SettingsFld = new Z1013SettingsFld( this, "jkcemu.z1013." );
    this.panelSysOpt.add( this.z1013SettingsFld, "Z1013" );


    // Optionen fuer Z9001
    this.z9001SettingsFld = new Z9001SettingsFld(
					this,
					"jkcemu.z9001.",
					false );
    this.panelSysOpt.add( this.z9001SettingsFld, "Z9001" );


    // Optionen fuer ZXSpectrum
    this.zxSpectrumSettingsFld = new ZXSpectrumSettingsFld(
					this,
					"jkcemu.zxspectrum." );
    this.panelSysOpt.add( this.zxSpectrumSettingsFld, "ZXSpectrum" );


    // Bereich Geschwindigkeit
    this.tabSpeed = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Geschwindigkeit", this.tabSpeed );

    GridBagConstraints gbcSpeed = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.tabSpeed.add(
		new JLabel( "Geschwindigkeit des emulierten Systems:" ),
		gbcSpeed );

    ButtonGroup grpSpeed = new ButtonGroup();
    this.btnSpeedDefault = new JRadioButton(
				"Begrenzen auf Originalgeschwindigkeit",
				true );
    this.btnSpeedDefault.addActionListener( this );
    grpSpeed.add( this.btnSpeedDefault );
    gbcSpeed.insets.top  = 0;
    gbcSpeed.insets.left = 50;
    gbcSpeed.gridy++;
    this.tabSpeed.add( this.btnSpeedDefault, gbcSpeed );

    this.btnSpeedValue = new JRadioButton( "Begrenzen auf:", false );
    this.btnSpeedValue.addActionListener( this );
    grpSpeed.add( this.btnSpeedValue );
    gbcSpeed.insets.bottom = 5;
    gbcSpeed.gridwidth     = 1;
    gbcSpeed.gridy++;
    this.tabSpeed.add( this.btnSpeedValue, gbcSpeed );

    this.fldSpeed = new JTextField( 5 );
    this.docSpeed = this.fldSpeed.getDocument();
    if( this.docSpeed != null ) {
      this.docSpeed.addDocumentListener( this );
    }
    gbcSpeed.insets.left = 5;
    gbcSpeed.gridx++;
    this.tabSpeed.add( this.fldSpeed, gbcSpeed );

    this.labelSpeedUnit = new JLabel( "MHz" );
    gbcSpeed.insets.left = 5;
    gbcSpeed.gridx++;
    this.tabSpeed.add( this.labelSpeedUnit, gbcSpeed );


    // Bereich Bildschirmausgabe
    this.tabScreen= new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Bildschirmausgabe", this.tabScreen );

    GridBagConstraints gbcScreen = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 10, 5, 10, 5 ),
					0, 0 );

    this.tabScreen.add( new JLabel( "Helligkeit [%]:" ), gbcScreen );

    this.sliderBrightness = new JSlider(
					SwingConstants.HORIZONTAL,
					0,
					100,
					DEFAULT_BRIGHTNESS );
    this.sliderBrightness.setMajorTickSpacing( 20 );
    this.sliderBrightness.setPaintLabels( true );
    this.sliderBrightness.setPaintTrack( true );
    this.sliderBrightness.setSnapToTicks( false );
    this.sliderBrightness.addChangeListener( this );
    gbcScreen.anchor    = GridBagConstraints.WEST;
    gbcScreen.fill      = GridBagConstraints.HORIZONTAL;
    gbcScreen.weightx   = 1.0;
    gbcScreen.gridwidth = GridBagConstraints.REMAINDER;
    gbcScreen.gridx++;
    this.tabScreen.add( this.sliderBrightness, gbcScreen );

    gbcScreen.anchor    = GridBagConstraints.EAST;
    gbcScreen.fill      = GridBagConstraints.NONE;
    gbcScreen.weightx   = 0.0;
    gbcScreen.gridwidth = 1;
    gbcScreen.gridx     = 0;
    gbcScreen.gridy++;
    this.tabScreen.add( new JLabel( "Rand:" ), gbcScreen );

    this.spinnerModelMargin = new SpinnerNumberModel( 20, 0, MAX_MARGIN, 1 );
    this.spinnerMargin = new JSpinner( this.spinnerModelMargin );
    this.spinnerMargin.addChangeListener( this );

    gbcScreen.anchor = GridBagConstraints.WEST;
    gbcScreen.fill   = GridBagConstraints.HORIZONTAL;
    gbcScreen.gridx++;
    this.tabScreen.add( this.spinnerMargin, gbcScreen );

    gbcScreen.fill = GridBagConstraints.NONE;
    gbcScreen.gridx++;
    this.tabScreen.add( new JLabel( "Pixel" ), gbcScreen );

    gbcScreen.anchor = GridBagConstraints.EAST;
    gbcScreen.gridx  = 0;
    gbcScreen.gridy++;
    this.tabScreen.add( new JLabel( "Aktualisierungszyklus:" ), gbcScreen );

    this.comboScreenRefresh = new JComboBox<>();
    this.comboScreenRefresh.setEditable( false );
    this.comboScreenRefresh.addItem( "10" );
    this.comboScreenRefresh.addItem( "20" );
    this.comboScreenRefresh.addItem( "30" );
    this.comboScreenRefresh.addItem( "50" );
    this.comboScreenRefresh.addItem( "100" );
    this.comboScreenRefresh.addItem( "200" );
    this.comboScreenRefresh.addActionListener( this );
    gbcScreen.anchor = GridBagConstraints.WEST;
    gbcScreen.fill   = GridBagConstraints.HORIZONTAL;
    gbcScreen.gridx++;
    this.tabScreen.add( this.comboScreenRefresh, gbcScreen );

    gbcScreen.fill = GridBagConstraints.NONE;
    gbcScreen.gridx++;
    this.tabScreen.add( new JLabel( "ms" ), gbcScreen );

    this.btnDirectCopyPaste = new JCheckBox(
		"Direktes \"Kopieren & Einf\u00FCgen\" durch Dr\u00FCcken"
			+ " der mittleren Maustaste",
		true );
    this.btnDirectCopyPaste.addActionListener( this );
    gbcScreen.anchor     = GridBagConstraints.CENTER;
    gbcScreen.insets.top = 10;
    gbcScreen.gridwidth = GridBagConstraints.REMAINDER;
    gbcScreen.gridx     = 0;
    gbcScreen.gridy++;
    this.tabScreen.add( this.btnDirectCopyPaste, gbcScreen );

    // gleicher Font von JSpinner und JComboBox
    Font font = this.comboScreenRefresh.getFont();
    if( font != null ) {
      this.spinnerMargin.setFont( font );
    }


    // Bereich Bestaetigungen
    this.tabConfirm = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Best\u00E4tigungen", this.tabConfirm );

    GridBagConstraints gbcConfirm = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.tabConfirm.add(
		new JLabel( "Folgende Aktionen m\u00FCssen in einem"
				+ " Dialog best\u00E4tigt werden:" ),
		gbcConfirm );

    this.btnConfirmNMI = new JCheckBox(
		"Nicht maskierbarer Interrupt (NMI)",
		true );
    this.btnConfirmNMI.addActionListener( this );
    gbcConfirm.insets.top  = 0;
    gbcConfirm.insets.left = 50;
    gbcConfirm.gridy++;
    this.tabConfirm.add( this.btnConfirmNMI, gbcConfirm );

    this.btnConfirmReset = new JCheckBox(
		"Emulator zur\u00FCcksetzen (RESET)",
		false );
    this.btnConfirmReset.addActionListener( this );
    gbcConfirm.gridy++;
    this.tabConfirm.add( this.btnConfirmReset, gbcConfirm );

    this.btnConfirmPowerOn = new JCheckBox(
		"Einschalten emulieren (Arbeitsspeicher l\u00F6schen)",
		false );
    this.btnConfirmPowerOn.addActionListener( this );
    gbcConfirm.gridy++;
    this.tabConfirm.add( this.btnConfirmPowerOn, gbcConfirm );

    this.btnConfirmQuit = new JCheckBox( "Emulator beenden", false );
    this.btnConfirmQuit.addActionListener( this );
    gbcConfirm.insets.bottom = 5;
    gbcConfirm.gridy++;
    this.tabConfirm.add( this.btnConfirmQuit, gbcConfirm );


    // Bereich Erscheinungsbild
    this.tabLAF = null;
    this.grpLAF = new ButtonGroup();
    this.lafs   = UIManager.getInstalledLookAndFeels();
    if( this.lafs != null ) {
      if( this.lafs.length > 1 ) {
	this.tabLAF = new JPanel( new GridBagLayout() );
	this.tabbedPane.addTab( "Erscheinungsbild", this.tabLAF );

	GridBagConstraints gbcLAF = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

	this.tabLAF.add(
		new JLabel( "Das Aussehen von JKCEMU k\u00F6nnen Sie"
				+ " durch Auswahl eines Erscheinungsbildes" ),
		gbcLAF );

	gbcLAF.insets.top    = 0;
	gbcLAF.insets.bottom = 5;
	gbcLAF.gridy++;
	this.tabLAF.add(
		new JLabel( "an Ihren pers\u00F6nlichen Geschmack"
				+ " anpassen:" ),
		gbcLAF );

	gbcLAF.insets.left   = 50;
	gbcLAF.insets.bottom = 0;
	for( int i = 0; i < this.lafs.length; i++ ) {
	  String clName = this.lafs[ i ].getClassName();
	  if( clName != null ) {
	    JRadioButton btn = new JRadioButton( this.lafs[ i ].getName() );
	    this.grpLAF.add( btn );
	    btn.setActionCommand( clName );
	    btn.addActionListener( this );
	    if( i == this.lafs.length - 1 ) {
	      gbcLAF.insets.bottom = 5;
	    }
	    gbcLAF.gridy++;
	    this.tabLAF.add( btn, gbcLAF );
	    this.lafClass2Button.put( clName, btn );
	  }
	}
      }
    }


    // Bereich Netzwerk
    this.tabKCNet = new KCNetSettingsFld( this, "jkcemu.net." );
    this.tabbedPane.addTab( "Netzwerk", this.tabKCNet );


    // Bereich Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.tabEtc.add( new JLabel( "Dateiauswahldialog:" ), gbcEtc );

    ButtonGroup grpFileDlg = new ButtonGroup();

    this.btnFileDlgEmu = new JRadioButton(
				"JKCEMU-Dateiauswahldialog verwenden",
				true );
    grpFileDlg.add( this.btnFileDlgEmu );
    this.btnFileDlgEmu.addActionListener( this );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnFileDlgEmu, gbcEtc );

    this.btnFileDlgNative = new JRadioButton(
			"Dateiauswahldialog des Betriebssystems verwenden",
			false );
    grpFileDlg.add( this.btnFileDlgNative );
    this.btnFileDlgNative.addActionListener( this );
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnFileDlgNative, gbcEtc );

    gbcEtc.insets.top  = 20;
    gbcEtc.insets.left = 5;
    gbcEtc.gridy++;
    this.tabEtc.add(
	new JLabel( "Statische RAM-Bereiche (SRAM) initialisieren mit:" ),
	gbcEtc );

    ButtonGroup grpSRAMInit = new ButtonGroup();

    this.btnSRAMInit00 = new JRadioButton( "Null-Bytes", true );
    grpSRAMInit.add( this.btnSRAMInit00 );
    this.btnSRAMInit00.addActionListener( this );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnSRAMInit00, gbcEtc );

    this.btnSRAMInitRandom = new JRadioButton(
			"Zufallsmuster (entspricht Originalverhalten)",
			false );
    grpSRAMInit.add( this.btnSRAMInitRandom );
    this.btnSRAMInitRandom.addActionListener( this );
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnSRAMInitRandom, gbcEtc );

    this.btnClearRFsOnPowerOn = new JCheckBox(
		"RAM-Floppies bei jedem \"Einschalten\" l\u00F6schen",
		false );
    this.btnClearRFsOnPowerOn.addActionListener( this );
    gbcEtc.insets.top  = 15;
    gbcEtc.insets.left = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnClearRFsOnPowerOn, gbcEtc );

    this.btnReloadROMsOnPowerOn = new JCheckBox(
		"Eingebundene ROM-Dateien bei jedem \"Einschalten\""
			+ " neu laden",
		false );
    this.btnReloadROMsOnPowerOn.addActionListener( this );
    gbcEtc.insets.top    = 0;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnReloadROMsOnPowerOn, gbcEtc );

    File configDir = Main.getConfigDir();
    if( configDir != null ) {
      gbcEtc.insets.top    = 15;
      gbcEtc.insets.bottom = 0;
      gbcEtc.gridy++;
      this.tabEtc.add(
		new JLabel( "JKCEMU-Konfigurationsverzeichnis"
			+ " (Einstellungen und Profile)" ),
		gbcEtc );

      this.fldConfigDir = new JTextField();
      this.fldConfigDir.setEditable( false );
      this.fldConfigDir.setText( configDir.getPath() );
      gbcEtc.fill          = GridBagConstraints.HORIZONTAL;
      gbcEtc.weightx       = 1.0;
      gbcEtc.insets.top    = 0;
      gbcEtc.insets.bottom = 5;
      gbcEtc.gridwidth     = 1;
      gbcEtc.gridy++;
      this.tabEtc.add( this.fldConfigDir, gbcEtc );

      this.btnDeleteConfigDir = new JButton( "L\u00F6schen" );
      this.btnDeleteConfigDir.setEnabled( configDir.exists() );
      this.btnDeleteConfigDir.addActionListener( this );
      gbcEtc.fill        = GridBagConstraints.NONE;
      gbcEtc.weightx     = 0.0;
      gbcEtc.insets.left = 0;
      gbcEtc.gridx++;
      this.tabEtc.add( this.btnDeleteConfigDir, gbcEtc );
    } else {
      this.fldConfigDir       = null;
      this.btnDeleteConfigDir = null;
    }


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 5, 1, 5, 5 ) );

    gbc.anchor  = GridBagConstraints.NORTHEAST;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridx++;
    add( panelBtn, gbc );

    this.btnApply = new JButton( "\u00DCbernehmen" );
    this.btnApply.setEnabled( false );
    this.btnApply.addActionListener( this );
    this.btnApply.addKeyListener( this );
    panelBtn.add( this.btnApply );

    this.btnLoad = new JButton( "Profil laden..." );
    this.btnLoad.addActionListener( this );
    this.btnLoad.addKeyListener( this );
    panelBtn.add( this.btnLoad );

    this.btnSave = new JButton( "Profil speichern..." );
    this.btnSave.addActionListener( this );
    this.btnSave.addKeyListener( this );
    panelBtn.add( this.btnSave );

    this.btnHelp = new JButton( "Hilfe" );
    this.btnHelp.addActionListener( this );
    this.btnHelp.addKeyListener( this );
    panelBtn.add( this.btnHelp );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    this.btnClose.addKeyListener( this );
    panelBtn.add( this.btnClose );


    // Voreinstellungen
    updFields(
	Main.getProperties(),
	UIManager.getLookAndFeel(),
	Integer.toString( this.screenFrm.getScreenRefreshMillis() ) );


    // sonstiges
    if( !super.applySettings( Main.getProperties(), true ) ) {
      pack();
      setLocationByPlatform( true );
    }
    setResizable( true );
  }


	/* --- private Methoden --- */

  private void applyConfirm( Properties props )
  {
    props.setProperty(
		"jkcemu.confirm.nmi",
		Boolean.toString( this.btnConfirmNMI.isSelected() ) );
    props.setProperty(
		"jkcemu.confirm.reset",
		Boolean.toString( this.btnConfirmReset.isSelected() ) );
    props.setProperty(
		"jkcemu.confirm.power_on",
		Boolean.toString( this.btnConfirmPowerOn.isSelected() ) );
    props.setProperty(
		"jkcemu.confirm.quit",
		Boolean.toString( this.btnConfirmQuit.isSelected() ) );
  }


  private void applyEtc( Properties props )
  {
    props.setProperty(
		"jkcemu.filedialog",
		this.btnFileDlgNative.isSelected() ? "native" : "jkcemu" );
    props.setProperty(
		"jkcemu.sram.init",
		this.btnSRAMInit00.isSelected() ? "00" : "random" );
    props.setProperty(
		"jkcemu.ramfloppy.clear_on_power_on",
		Boolean.toString(
			this.btnClearRFsOnPowerOn.isSelected() ) );
    props.setProperty(
		"jkcemu.external_rom.reload_on_power_on",
		Boolean.toString(
			this.btnReloadROMsOnPowerOn.isSelected() ) );
  }


  private void applyLAF( Properties props ) throws UserInputException
  {
    ButtonModel bm = this.grpLAF.getSelection();
    if( bm != null ) {
      String lafClassName = bm.getActionCommand();
      if( lafClassName != null ) {
	if( lafClassName.length() > 0 ) {
	  boolean     lafChanged = true;
	  LookAndFeel oldLAF     = UIManager.getLookAndFeel();
	  if( oldLAF != null ) {
	    if( lafClassName.equals( oldLAF.getClass().getName() ) ) {
	      lafChanged = false;
	    }
	  }
	  if( lafChanged ) {
	    try {
	      UIManager.setLookAndFeel( lafClassName );
	      EventQueue.invokeLater(
				new Runnable()
				{
				  public void run()
				  {
				    informLAFChanged();
				  }
				} );
	      props.setProperty(
			"jkcemu.lookandfeel.classname",
			lafClassName );
	    }
	    catch( Exception ex ) {
	      throw new UserInputException(
		"Das Erscheinungsbild kann nicht eingestellt werden." );
	    }
	  }
	}
      }
    }
  }


  private void applyScreen( Properties props )
  {
    props.setProperty(
		"jkcemu.brightness",
		String.valueOf( this.sliderBrightness.getValue() ) );

    Object obj = this.spinnerMargin.getValue();
    props.setProperty(
		"jkcemu.screen.margin",
		obj != null ? obj.toString() : "0" );

    obj = this.comboScreenRefresh.getSelectedItem();
    if( obj != null ) {
      String text = obj.toString();
      if( text != null ) {
	props.setProperty( "jkcemu.screen.refresh.ms", text );
      }
    }

    props.setProperty(
		"jkcemu.copy_and_paste.direct",
		Boolean.toString( this.btnDirectCopyPaste.isSelected() ) );
  }


  private void applySpeed( Properties props ) throws UserInputException
  {
    if( this.btnSpeedValue.isSelected() ) {
      boolean done = false;
      String  msg  = "Sie m\u00FCssen einen Wert f\u00FCr"
				+ " die max. Geschwindigkeit eingeben.";
      String  text = this.fldSpeed.getText();
      if( text != null ) {
	if( !text.isEmpty() ) {
	  msg = "Die eingegebene max. Geschwindigkeit ist ung\u00FCltig.";
	  try {
	    Number mhzValue = this.fmtSpeed.parse( text );
	    if( mhzValue != null ) {
	      long khzValue = Math.round( mhzValue.doubleValue() * 1000.0 );
	      if( khzValue > 0 ) {
		props.setProperty(
				"jkcemu.maxspeed.khz",
				String.valueOf( khzValue ) );
		done = true;
	      }
	    }
	  }
	  catch( ParseException ex ) {}
	}
      }
      if( !done ) {
	throw new UserInputException( msg );
      }
    } else {
      props.setProperty( "jkcemu.maxspeed.khz", "default" );
    }
  }


  private void applySys( Properties props ) throws
					UserCancelException,
					UserInputException
  {
    // System
    String valueSys = "";
    if( this.btnSysA5105.isSelected() ) {
      valueSys = "A5105";
    }
    else if( this.btnSysAC1.isSelected() ) {
      valueSys = "AC1";
    }
    else if( this.btnSysBCS3.isSelected() ) {
      valueSys = "BCS3";
    }
    else if( this.btnSysC80.isSelected() ) {
      valueSys = "C80";
    }
    else if( this.btnSysHC900.isSelected() ) {
      valueSys = "HC900";
    }
    else if( this.btnSysHEMC.isSelected() ) {
      valueSys = "HueblerEvertMC";
    }
    else if( this.btnSysHGMC.isSelected() ) {
      valueSys = "HueblerGraphicsMC";
    }
    else if( this.btnSysKC85_1.isSelected() ) {
      valueSys = "KC85/1";
    }
    else if( this.btnSysKC85_2.isSelected() ) {
      valueSys = "KC85/2";
    }
    else if( this.btnSysKC85_3.isSelected() ) {
      valueSys = "KC85/3";
    }
    else if( this.btnSysKC85_4.isSelected() ) {
      valueSys = "KC85/4";
    }
    else if( this.btnSysKC85_5.isSelected() ) {
      valueSys = "KC85/5";
    }
    else if( this.btnSysKC87.isSelected() ) {
      valueSys = "KC87";
    }
    else if( this.btnSysKCcompact.isSelected() ) {
      valueSys = "KCcompact";
    }
    else if( this.btnSysKramerMC.isSelected() ) {
      valueSys = "KramerMC";
    }
    else if( this.btnSysLC80.isSelected() ) {
      valueSys = this.lc80SettingsFld.getModelText();
    }
    else if( this.btnSysLLC1.isSelected() ) {
      valueSys = "LLC1";
    }
    else if( this.btnSysLLC2.isSelected() ) {
      valueSys = "LLC2";
    }
    else if( this.btnSysNANOS.isSelected() ) {
      valueSys = "NANOS";
    }
    else if( this.btnSysPCM.isSelected() ) {
      valueSys = "PC/M";
    }
    else if( this.btnSysPoly880.isSelected() ) {
      valueSys = "Poly880";
    }
    else if( this.btnSysSC2.isSelected() ) {
      valueSys = "SC2";
    }
    else if( this.btnSysSLC1.isSelected() ) {
      valueSys = "SLC1";
    }
    else if( this.btnSysVCS80.isSelected() ) {
      valueSys = "VCS80";
    }
    else if( this.btnSysZ1013.isSelected() ) {
      valueSys = this.z1013SettingsFld.getModelText();
    }
    else if( this.btnSysZ9001.isSelected() ) {
      valueSys = "Z9001";
    }
    else if( this.btnSysZXSpectrum.isSelected() ) {
      valueSys = "ZXSpectrum";
    }
    props.setProperty( "jkcemu.system", valueSys );

    // Optionen fuer A5105
    this.a5105SettingsFld.applyInput( props, valueSys.equals( "A5105" ) );

    // Optionen fuer AC1
    this.ac1SettingsFld.applyInput( props, valueSys.equals( "AC1" ) );

    // Optionen fuer BCS3
    this.bcs3SettingsFld.applyInput( props, valueSys.equals( "BCS3" ) );

    // Optionen fuer HC900
    this.hc900SettingsFld.applyInput( props, valueSys.equals( "HC900" ) );

    // Optionen fuer Huebler/Evert-MC
    props.setProperty(
		"jkcemu.hemc.catch_print_calls",
		Boolean.toString( this.btnHEMCCatchPrintCalls.isSelected() ) );

    // Optionen fuer Huebler-Grafik-MC
    this.hgmcSettingsFld.applyInput(
				props,
				valueSys.equals( "HueblerGraphicsMC" ) );

    // Optionen fuer KC85/1..5
    this.kc85_1_SettingsFld.applyInput( props, valueSys.equals( "KC85/1" ) );
    this.kc85_2_SettingsFld.applyInput( props, valueSys.equals( "KC85/2" ) );
    this.kc85_3_SettingsFld.applyInput( props, valueSys.equals( "KC85/3" ) );
    this.kc85_4_SettingsFld.applyInput( props, valueSys.equals( "KC85/4" ) );
    this.kc85_5_SettingsFld.applyInput( props, valueSys.equals( "KC85/5" ) );

    // Optionen fuer KC87
    this.kc87SettingsFld.applyInput( props, valueSys.equals( "KC87" ) );

    // Optionen fuer KC compact
    this.kcCompactSettingsFld.applyInput(
					props,
					valueSys.equals( "KCcompact" ) );

    // Optionen fuer Kramer-MC
    props.setProperty(
		"jkcemu.kramermc.catch_print_calls",
		Boolean.toString(
			this.btnKramerMCCatchPrintCalls.isSelected() ) );

    // Optionen fuer LC80
    this.lc80SettingsFld.applyInput(
		props,
		valueSys.startsWith( "LC80" ) );

    // Optionen fuer LLC2
    this.llc2SettingsFld.applyInput( props, valueSys.equals( "LLC2" ) );

    // Optionen fuer NANOS
    this.nanosSettingsFld.applyInput( props, valueSys.equals( "NANOS" ) );

    // Optionen fuer PC/M
    this.pcmSettingsFld.applyInput( props, valueSys.equals( "PC/M" ) );

    // Optionen fuer Poly880
    this.poly880SettingsFld.applyInput( props, valueSys.equals( "Poly880" ) );

    // Optionen fuer Z1013
    this.z1013SettingsFld.applyInput(
		props,
		valueSys.startsWith( "Z1013" ) );

    // Optionen fuer Z9001
    this.z9001SettingsFld.applyInput( props, valueSys.equals( "Z9001" ) );

    // Optionen fuer ZXSpectrum
    this.zxSpectrumSettingsFld.applyInput(
		props,
		valueSys.equals( "ZXSpectrum" ) );
  }


  private static boolean deleteDir( File dirFile )
  {
    /*
     * Sollte durch einen symbolischen Link eine Ringkettung bestehen,
     * wuerde der Stack ueberlaufen.
     * Um das zu verhindern, wird versucht, das Verzeichnis
     * respektive dem ggf. verhandenen symbolischen Link direkt zu loeschen,
     * was bei einem symbolischen Link auch moeglich ist.
     * Damit wird eine evtl. vorhandene Ringkettung unterbrochen.
     */
    boolean rv = dirFile.delete();
    if( !rv ) {
      // direktes Loeschen fehlgeschlagen -> rekursiv versuchen
      rv = true;
      File[]  files = dirFile.listFiles();
      if( files != null ) {
	for( int i = 0; rv && (i < files.length); i++ ) {
	  boolean skip  = false;
	  File    file  = files[ i ];
	  String  fName = file.getName();
	  if( fName != null ) {
	    if( fName.equals( "." ) || fName.equals( ".." ) ) {
	      skip = true;
	    }
	  }
	  if( !skip ) {
	    if( file.isDirectory() ) {
	      rv = deleteDir( file );
	    } else if( file.isFile() ) {
	      rv = file.delete();
	    } else {
	      rv = false;
	    }
	  }
	}
      }
      if( rv ) {
	rv = dirFile.delete();
      }
    }
    return rv;
  }


  private static boolean differs( String s1, String s2 )
  {
    if( s1 == null ) {
      s1 = "";
    }
    return !s1.equals( s2 != null ? s2 : "" );
  }


  private void docChanged( DocumentEvent e )
  {
    if( e.getDocument() == this.docSpeed )
      setDataChanged();
  }


  private void informLAFChanged()
  {
    Frame[] frames = Frame.getFrames();
    if( frames != null ) {
      for( int i = 0; i< frames.length; i++ ) {
	Frame frm = frames[ i ];
	if( frm != null ) {
	  SwingUtilities.updateComponentTreeUI( frm );
	  if( frm instanceof BasicFrm ) {
	    ((BasicFrm) frm).lookAndFeelChanged();
	    if( !frm.isResizable() ) {
	      frm.pack();
	    }
	  }
	}
      }
    }
  }


  private void setDataChanged()
  {
    this.btnApply.setEnabled( true );
    if( this.btnSave != null )
      this.btnSave.setEnabled( false );
  }


  private void setSpeedValueFld( int khzValue )
  {
    this.fldSpeed.setText(
		this.fmtSpeed.format( (double) khzValue / 1000.0 ) );
  }


  private void updFields(
		Properties  props,
		LookAndFeel laf,
		String      screenRefreshMillis )
  {
    // System
    String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( sysName.startsWith( "AC1" ) ) {
      this.btnSysAC1.setSelected( true );
    }
    else if( sysName.startsWith( "BCS3" ) ) {
      this.btnSysBCS3.setSelected( true );
    }
    else if( sysName.startsWith( "C80" ) ) {
      this.btnSysC80.setSelected( true );
    }
    else if( sysName.startsWith( "HC900" ) ) {
      this.btnSysHC900.setSelected( true );
    }
    else if( sysName.startsWith( "HueblerEvertMC" ) ) {
      this.btnSysHEMC.setSelected( true );
    }
    else if( sysName.startsWith( "HueblerGraphicsMC" ) ) {
      this.btnSysHGMC.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/1" ) ) {
      this.btnSysKC85_1.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/2" ) ) {
      this.btnSysKC85_2.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/3" ) ) {
      this.btnSysKC85_3.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/4" ) ) {
      this.btnSysKC85_4.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/5" ) ) {
      this.btnSysKC85_5.setSelected( true );
    }
    else if( sysName.startsWith( "KC87" ) ) {
      this.btnSysKC87.setSelected( true );
    }
    else if( sysName.startsWith( "KCcompact" ) ) {
      this.btnSysKCcompact.setSelected( true );
    }
    else if( sysName.startsWith( "KramerMC" ) ) {
      this.btnSysKramerMC.setSelected( true );
    }
    else if( sysName.startsWith( "LC80" ) ) {
      this.btnSysLC80.setSelected( true );
    }
    else if( sysName.startsWith( "LLC1" ) ) {
      this.btnSysLLC1.setSelected( true );
    }
    else if( sysName.startsWith( "LLC2" ) ) {
      this.btnSysLLC2.setSelected( true );
    }
    else if( sysName.startsWith( "NANOS" ) ) {
      this.btnSysNANOS.setSelected( true );
    }
    else if( sysName.startsWith( "PC/M" ) ) {
      this.btnSysPCM.setSelected( true );
    }
    else if( sysName.startsWith( "Poly880" ) ) {
      this.btnSysPoly880.setSelected( true );
    }
    else if( sysName.startsWith( "SC2" ) ) {
      this.btnSysSC2.setSelected( true );
    }
    else if( sysName.startsWith( "SLC1" ) ) {
      this.btnSysSLC1.setSelected( true );
    }
    else if( sysName.startsWith( "VCS80" ) ) {
      this.btnSysVCS80.setSelected( true );
    }
    else if( sysName.startsWith( "Z1013" ) ) {
      this.btnSysZ1013.setSelected( true );
    }
    else if( sysName.startsWith( "Z9001" ) ) {
      this.btnSysZ9001.setSelected( true );
    }
    else if( sysName.startsWith( "ZXSpectrum" ) ) {
      this.btnSysZXSpectrum.setSelected( true );
    } else {
      this.btnSysA5105.setSelected( true );
    }

    // Optionen fuer A5105
    this.a5105SettingsFld.updFields( props );

    // Optionen fuer AC1
    this.ac1SettingsFld.updFields( props );

    // Optionen fuer BCS3
    this.bcs3SettingsFld.updFields( props );

    // Optionen fuer HC900
    this.hc900SettingsFld.updFields( props );

    // Optionen fuer Huebler/Evert-MC
    this.btnHEMCCatchPrintCalls.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.hemc.catch_print_calls",
				true ) );

    // Optionen fuer Huebler-Grafik-MC
    this.hgmcSettingsFld.updFields( props );

    // Optionen fuer Kramer-MC
    this.btnKramerMCCatchPrintCalls.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kramermc.catch_print_calls",
				false ) );

    // Optionen fuer KC85/1..5
    this.kc85_1_SettingsFld.updFields( props );
    this.kc85_2_SettingsFld.updFields( props );
    this.kc85_3_SettingsFld.updFields( props );
    this.kc85_4_SettingsFld.updFields( props );
    this.kc85_5_SettingsFld.updFields( props );

    // Optionen fuer KC87
    this.kc87SettingsFld.updFields( props );

    // Optionen fuer KC compact
    this.kcCompactSettingsFld.updFields( props );

    // Optionen fuer LLC2
    this.llc2SettingsFld.updFields( props );

    // Optionen fuer NANOS
    this.nanosSettingsFld.updFields( props );

    // Optionen fuer PC/M
    this.pcmSettingsFld.updFields( props );

    // Optionen fuer Poly880
    this.poly880SettingsFld.updFields( props );

    // Optionen fuer LC80
    this.lc80SettingsFld.updFields( props );

    // Optionen fuer Z1013
    this.z1013SettingsFld.updFields( props );

    // Optionen fuer Z9001
    this.z9001SettingsFld.updFields( props );

    // Optionen fuer ZXSpectrum
    this.zxSpectrumSettingsFld.updFields( props );

    // Optionen anpassen
    updSysOptCard();


    // Geschwindigkeit
    boolean done = false;
    int     defaultKHz = EmuThread.getDefaultSpeedKHz( props );
    String  speedText  = EmuUtil.getProperty(
					props,
					"jkcemu.maxspeed.khz" ).toLowerCase();
    if( !speedText.isEmpty() ) {
      try {
	int value = Integer.parseInt( speedText );
	if( (value > 0) && (value != defaultKHz) ) {
	  setSpeedValueFld( value );
	  this.btnSpeedValue.setSelected( true );
	  done = true;
	}
      }
      catch( NumberFormatException ex ) {}
    }
    if( !done ) {
      setSpeedValueFld( defaultKHz );
      this.btnSpeedDefault.setSelected( true );
    }
    updSpeedFieldsEnabled();


    // Bildschirmausgabe
    int brightness = EmuUtil.getIntProperty(
					props,
					"jkcemu.brightness",
					DEFAULT_BRIGHTNESS );
    if( (brightness >= 0) && (brightness <= 100) ) {
      this.sliderBrightness.setValue( brightness );
    }
    try {
      int margin = EmuUtil.getIntProperty( props, "jkcemu.screen.margin", 20 );
      if( (margin >= 0) && (margin <= MAX_MARGIN) ) {
	this.spinnerModelMargin.setValue( new Integer( margin ) );
      }
    }
    catch( IllegalArgumentException ex ) {}

    if( screenRefreshMillis == null ) {
      screenRefreshMillis = props.getProperty( "jkcemu.screen.refresh.ms" );
    }
    if( screenRefreshMillis != null ) {
      screenRefreshMillis = screenRefreshMillis.trim();
      if( screenRefreshMillis.isEmpty() ) {
	screenRefreshMillis = null;
      }
    }
    if( screenRefreshMillis == null ) {
      screenRefreshMillis = Integer.toString(
		ScreenFrm.getDefaultScreenRefreshMillis() );
    }
    this.comboScreenRefresh.setSelectedItem( screenRefreshMillis );

    this.btnDirectCopyPaste.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				"jkcemu.copy_and_paste.direct",
				true ) );


    // Bestaetigungen
    this.btnConfirmNMI.setSelected(
	EmuUtil.getBooleanProperty( props, "jkcemu.confirm.nmi", true ) );
    this.btnConfirmReset.setSelected(
	EmuUtil.getBooleanProperty( props, "jkcemu.confirm.reset", false ) );
    this.btnConfirmPowerOn.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			"jkcemu.confirm.power_on",
			false ) );
    this.btnConfirmQuit.setSelected(
	EmuUtil.getBooleanProperty( props, "jkcemu.confirm.quit", false ) );


    // Erscheinungsbild
    String lafClassName = null;
    if( laf != null ) {
      lafClassName = laf.getClass().getName();
    }
    if( laf == null ) {
      lafClassName = props.getProperty( "jkcemu.lookandfeel.classname" );
    }
    if( lafClassName != null ) {
      AbstractButton btn = this.lafClass2Button.get( lafClassName );
      if( btn != null ) {
	btn.setSelected( true );
      }
    }


    // Netzwerk
    this.tabKCNet.updFields( props );


    // Sonstiges
    if( EmuUtil.getProperty(
		props,
		"jkcemu.filedialog" ).toLowerCase().equals( "native" ) )
    {
      this.btnFileDlgNative.setSelected( true );
    } else {
      this.btnFileDlgEmu.setSelected( true );
    }
    if( EmuUtil.getProperty(
		props,
		"jkcemu.sram.init" ).toLowerCase().startsWith( "r" ) )
    {
      this.btnSRAMInitRandom.setSelected( true );
    } else {
      this.btnSRAMInit00.setSelected( true );
    }
    this.btnClearRFsOnPowerOn.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				"jkcemu.ramfloppy.clear_on_power_on",
				false ) );
    this.btnReloadROMsOnPowerOn.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				"jkcemu.external_rom.reload_on_power_on",
				false ) );
  }


  private void updSpeedFieldsEnabled()
  {
    boolean state = this.btnSpeedValue.isSelected();
    this.fldSpeed.setEnabled( state );
    this.labelSpeedUnit.setEnabled( state );
  }


  private void updSysOptCard()
  {
    String cardName = "noopt";
    if( this.btnSysA5105.isSelected() ) {
      cardName = "A5105";
    }
    else if( this.btnSysAC1.isSelected() ) {
      cardName = "AC1";
    }
    else if( this.btnSysBCS3.isSelected() ) {
      cardName = "BCS3";
    }
    else if( this.btnSysHC900.isSelected() ) {
      cardName = "HC900";
    }
    else if( this.btnSysHEMC.isSelected() ) {
      cardName = "HEMC";
    }
    else if( this.btnSysHGMC.isSelected() ) {
      cardName = "HGMC";
    }
    else if( this.btnSysKC85_1.isSelected() ) {
      cardName = "KC85/1";
    }
    else if( this.btnSysKC85_2.isSelected() ) {
      cardName = "KC85/2";
    }
    else if( this.btnSysKC85_3.isSelected() ) {
      cardName = "KC85/3";
    }
    else if( this.btnSysKC85_4.isSelected() ) {
      cardName = "KC85/4";
    }
    else if( this.btnSysKC85_5.isSelected() ) {
      cardName = "KC85/5";
    }
    else if( this.btnSysKC87.isSelected() ) {
      cardName = "KC87";
    }
    else if( this.btnSysKCcompact.isSelected() ) {
      cardName = "KCcompact";
    }
    else if( this.btnSysKramerMC.isSelected() ) {
      cardName = "KramerMC";
    }
    else if( this.btnSysLC80.isSelected() ) {
      cardName = "LC80";
    }
    else if( this.btnSysNANOS.isSelected() ) {
      cardName = "NANOS";
    }
    else if( this.btnSysPCM.isSelected() ) {
      cardName = "PC/M";
    }
    else if( this.btnSysPoly880.isSelected() ) {
      cardName = "Poly880";
    }
    else if( this.btnSysLLC2.isSelected() ) {
      cardName = "LLC2";
    }
    else if( this.btnSysZ1013.isSelected() ) {
      cardName = "Z1013";
    }
    else if( this.btnSysZ9001.isSelected() ) {
      cardName = "Z9001";
    }
    else if( this.btnSysZXSpectrum.isSelected() ) {
      cardName = "ZXSpectrum";
    }
    this.cardLayoutSysOpt.show( this.panelSysOpt, cardName );
    this.curSysOptCard = cardName;
  }
}

