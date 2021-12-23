/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer die Einstellungen
 */

package jkcemu.settings;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.FontMngr;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.ProfileDlg;
import jkcemu.base.ScreenFrm;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.net.KCNet;
import jkcemu.net.KCNetSettingsFld;


public class SettingsFrm extends BaseFrm
{
  private static final String HELP_PAGE  = "/help/settings.htm";

  private static SettingsFrm instance = null;

  private ScreenFrm          screenFrm;
  private EmuThread          emuThread;
  private File               profileFile;
  private EmuSysSettingsFld  tabEmuSys;
  private ConfirmSettingsFld tabConfirm;
  private EtcSettingsFld     tabEtc;
  private FontSymSettingsFld tabFontSym;
  private KCNetSettingsFld   tabKCNet;
  private LAFSettingsFld     tabLAF;
  private UIScaleSettingsFld tabUIScale;
  private ScreenSettingsFld  tabScreen;
  private SpeedSettingsFld   tabSpeed;
  private JButton            btnApply;
  private JButton            btnLoad;
  private JButton            btnSave;
  private JButton            btnHelp;
  private JButton            btnClose;
  private JTabbedPane        tabbedPane;


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


  public void fireDataChanged( final boolean state )
  {
    fireDataChanged( state, 0 );
  }


  public void fireUpdSpeedTab()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updSpeedTab();
		  }
		} );
  }


  public ScreenFrm getScreenFrm()
  {
    return this.screenFrm;
  }


  public void updSpeedTab( Properties props )
  {
    if( props != null ) {
      this.tabSpeed.setDefaultSpeedKHz(
			EmuThread.getDefaultSpeedKHz( props ) );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props )
  {
    updFields( props );
    return super.applySettings( props );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
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
	  HelpFrm.openPage( HELP_PAGE );
	}
	else if( src == this.btnClose ) {
	  rv = true;
	  doClose();
	}
      }
    }
    catch( UserInputException ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
    return rv;
  }


  @Override
  public void setVisible( boolean state )
  {
    if( state && !isVisible() ) {
      updFields( Main.getProperties() );
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
    Component tab         = null;
    Component selectedTab = this.tabbedPane.getSelectedComponent();
    try {
      tab = this.tabEmuSys;
      this.tabEmuSys.applyInput( props, selectedTab == this.tabEmuSys );

      tab = this.tabSpeed;
      this.tabSpeed.applyInput( props, selectedTab == this.tabSpeed );

      tab = this.tabConfirm;
      this.tabConfirm.applyInput( props, selectedTab == this.tabConfirm );

      tab = this.tabScreen;
      this.tabScreen.applyInput( props, selectedTab == this.tabScreen );

      tab = this.tabFontSym;
      this.tabFontSym.applyInput( props, selectedTab == this.tabFontSym );

      tab = this.tabUIScale;
      this.tabUIScale.applyInput( props, selectedTab == this.tabUIScale );

      tab = this.tabKCNet;
      this.tabKCNet.applyInput( props, selectedTab == this.tabKCNet );

      tab = this.tabEtc;
      this.tabEtc.applyInput( props, selectedTab == this.tabEtc );

      /*
       * Das Look&Feel als letztes setzen,
       * Anderenfalls koennte noch eine nachfolgende Aktion einen Fehler
       * erzeugen und das Uebernehmen der Einstellungen abbrechen.
       * In dem Fall waere das neue Erscheinungsbild schon eingestellt,
       * was vom Programmverhalten her inkonsistent waere.
       */
      boolean lafChanged = false;
      tab                = this.tabLAF;
      if( this.tabLAF != null ) {
	this.tabLAF.applyInput( props, selectedTab == this.tabLAF );
	lafChanged = this.tabLAF.getAndResetLookAndFeelChanged();
      }

      // Aenderungen am Aussehen propagieren
      boolean fontsChanged = this.tabFontSym.hasFontsChanged();
      boolean symChanged   = this.tabFontSym.hasSymbolsChanged();
      if( lafChanged || fontsChanged || symChanged ) {
	FontMngr.putProperties( props );
	GUIFactory.putProperties( props );
	for( Window window : Window.getWindows() ) {
	  EmuUtil.updComponentTreeUI(
				window,
				props,
				lafChanged,
				fontsChanged,
				symChanged );
	}
	EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    checkPackWindows();
			  }
			} );
      }

      // neue Eigenschaften anwenden
      Properties appProps = Main.getProperties();
      if( appProps != null ) {
	appProps.clear();
	appProps.putAll( props );
      } else {
	appProps = props;
      }
      this.emuThread.applySettings( appProps );
      Main.applyProfileToFrames( this.profileFile, appProps, false, this );
      this.tabFontSym.resetChanged();
      updSpeedTab( appProps );

      /*
       * Das Aktualisieren der Schaltflaechen wird ueber die EventQueue
       * getan, damit evtl. noch in der Queue befindliche
       * DataChanged-Events unwirksam werden.
       *
       * Da sich in der Praxis gezeigt hat,
       * dass manchmal trotzdem wieder "setDataChanged(true)"
       * aufgerufen wurde, wird das Event zweimal nacheinander
       * in die EventQueue gestellt.
       */
      fireDataChanged( false, 1 );

      // Hinweis
      if( this.tabLAF != null ) {
	this.tabLAF.checkShowRestartMsg();
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


  private void doLoad()
  {
    ProfileDlg dlg = new ProfileDlg(
				this,
				"Profil laden",
				EmuUtil.TEXT_LOAD,
				Main.getProfileFile(),
				false );
    dlg.setVisible( true );
    Properties props = dlg.getSelectedProfileProps();
    if( props != null ) {
      updFields( props );
      setDataChanged( true );
      this.profileFile = dlg.getSelectedProfileFile();
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
				EmuUtil.TEXT_SAVE,
				this.profileFile,
				true );
    dlg.setVisible( true );
    File profileFile = dlg.getSelectedProfileFile();
    if( profileFile != null ) {

      // Eigenschaften sammeln
      Frame[] frms = Frame.getFrames();
      if( frms != null ) {
	for( int i = 0; i < frms.length; i++ ) {
	  Frame f = frms[ i ];
	  if( f != null ) {
	    if( f instanceof BaseFrm ) {
	      ((BaseFrm) f).putSettingsTo( props );
	    }
	  }
	}
      }
      Toolkit tk = EmuUtil.getToolkit( this );
      if( tk != null ) {
	Dimension screenSize = tk.getScreenSize();
	if( screenSize != null ) {
	  EmuUtil.setProperty(
			props,
			Main.PROP_SCREEN_HEIGHT,
			screenSize.height );
	  EmuUtil.setProperty(
			props,
			Main.PROP_SCREEN_WIDTH,
			screenSize.width );
	}
      }
      props.setProperty( Main.PROP_VERSION, Main.VERSION );

      // ggf. Verzeichnis anlegen
      File configDir = profileFile.getParentFile();
      if( configDir != null ) {
	if( configDir.exists() ) {
	  this.tabEtc.configDirExists();
	}
      }

      // eigentliches Speichern
      OutputStream out  = null;
      try {
	out = new FileOutputStream( profileFile );
	props.storeToXML( out, "JKCEMU Properties" );
	out.close();
	out = null;
	this.profileFile = profileFile;
	Main.setProfile( this.profileFile, props );
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg(
		this,
		"Die Einstellungen k\u00F6nnen nicht in die Datei\n\'"
			+ profileFile.getPath()
			+ "\'\ngespeichert werden." );
      }
      finally {
	EmuUtil.closeSilently( out );
      }
    }
  }


	/* --- Konstruktor --- */

  private SettingsFrm( ScreenFrm screenFrm )
  {
    setTitle( "JKCEMU Einstellungen" );
    this.screenFrm   = screenFrm;
    this.emuThread   = screenFrm.getEmuThread();
    this.profileFile = Main.getProfileFile();


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

    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, gbc );

    // Bereich System
    this.tabEmuSys = new EmuSysSettingsFld( this );
    this.tabbedPane.addTab( "System", tabEmuSys );

    // Bereich Geschwindigkeit
    this.tabSpeed = new SpeedSettingsFld( this );
    this.tabbedPane.addTab( "Geschwindigkeit", this.tabSpeed );

    // Bereich Bestaetigungen
    this.tabConfirm = new ConfirmSettingsFld( this );
    this.tabbedPane.addTab( "Best\u00E4tigungen", this.tabConfirm );

    // Bereich Bildschirmausgabe
    this.tabScreen = new ScreenSettingsFld( this );
    this.tabbedPane.addTab( "Bildschirmausgabe", this.tabScreen );

    // Bereich Schriften und Symbole
    this.tabFontSym = new FontSymSettingsFld( this );
    this.tabbedPane.addTab( "Schriften/Symbole", this.tabFontSym );

    // UI-Skalierung
    this.tabUIScale = new UIScaleSettingsFld( this );
    this.tabbedPane.addTab( "Fensterskalierung", this.tabUIScale );

    // Bereich Erscheinungsbild
    this.tabLAF = new LAFSettingsFld( this );
    if( this.tabLAF.containsLAFSettings() ) {
      this.tabbedPane.addTab( "Erscheinungsbild", this.tabLAF );
    } else {
      this.tabLAF = null;
    }

    // Bereich Netzwerk
    this.tabKCNet = new KCNetSettingsFld( this, KCNet.PROP_PREFIX );
    this.tabbedPane.addTab( "Netzwerk", this.tabKCNet );

    // Bereich Sonstiges
    this.tabEtc = new EtcSettingsFld( this );
    this.tabbedPane.addTab( "Sonstiges", tabEtc );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 5, 1, 5, 5 ) );

    gbc.anchor  = GridBagConstraints.NORTHEAST;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridx++;
    add( panelBtn, gbc );

    this.btnApply = GUIFactory.createButton( EmuUtil.TEXT_APPLY );
    this.btnApply.setEnabled( false );
    panelBtn.add( this.btnApply );

    this.btnLoad = GUIFactory.createButton( "Profil laden..." );
    panelBtn.add( this.btnLoad );

    this.btnSave = GUIFactory.createButton( "Profil speichern..." );
    panelBtn.add( this.btnSave );

    this.btnHelp = GUIFactory.createButtonHelp();
    panelBtn.add( this.btnHelp );

    this.btnClose = GUIFactory.createButtonClose();
    panelBtn.add( this.btnClose );


    // Listener
    this.btnApply.addActionListener( this );
    this.btnLoad.addActionListener( this );
    this.btnSave.addActionListener( this );
    this.btnHelp.addActionListener( this );
    this.btnClose.addActionListener( this );


    // sonstiges
    setResizable( true );
    if( !super.applySettings( Main.getProperties() ) ) {
      setLocationByPlatform( true );
      pack();
    }
  }


	/* --- private Methoden --- */

  private void checkPackWindows()
  {
    for( Window window : Window.getWindows() ) {
      if( window instanceof BaseFrm ) {
	if( ((BaseFrm) window).getPackOnUIUpdate() ) {
	  ((BaseFrm) window).pack();
	}
      } else if( window instanceof BaseDlg ) {
	if( ((BaseDlg) window).getPackOnUIUpdate() ) {
	  ((BaseDlg) window).pack();
	}
      } else if( window instanceof Frame ) {
	if( !((Frame) window).isResizable() ) {
	  ((Frame) window).pack();
	}
      } else if( window instanceof Dialog ) {
	if( !((Dialog) window).isResizable() ) {
	  ((Dialog) window).pack();
	}
      }
    }
  }


  private void fireDataChanged( final boolean state, final int loopCnt )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    if( loopCnt > 0 ) {
		      fireDataChanged( state, loopCnt - 1 );
		    } else {
		      setDataChanged( state );
		    }
		  }
		} );
  }


  private void setDataChanged( boolean state )
  {
    this.btnApply.setEnabled( state );
    if( this.btnSave != null ) {
      this.btnSave.setEnabled( !state );
    }
  }


  private void updFields( final Properties props )
  {
    this.tabEmuSys.updFields( props );
    this.tabSpeed.updFields( props );
    this.tabConfirm.updFields( props );
    this.tabScreen.updFields( props );
    this.tabFontSym.updFields( props );
    this.tabUIScale.updFields( props );
    if( this.tabLAF != null ) {
      this.tabLAF.updFields( props );
    }
    this.tabKCNet.updFields( props );
    this.tabEtc.updFields( props );
  }


  private void updSpeedTab()
  {
    updSpeedTab( this.tabEmuSys.getCurSettingsSilently() );
  }
}
