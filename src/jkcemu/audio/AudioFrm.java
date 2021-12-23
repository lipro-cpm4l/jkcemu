/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer die Audiofunktionen zur Emulation
 * des Kassettenrecorderanschlusses bzw. des Tonausgangs
 */

package jkcemu.audio;

import java.io.File;
import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.util.EventObject;
import java.util.Properties;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeSet;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.ScreenFrm;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;


public class AudioFrm extends BaseFrm implements ComponentListener
{
  private static final String HELP_PAGE = "/help/audio.htm";

  private static volatile AudioFrm instance = null;

  private ScreenFrm    screenFrm;
  private EmuThread    emuThread;
  private Mixer.Info[] mixers;
  private int          speedKHz;
  private Properties   autoEnableProps;
  private JMenuItem    mnuClose;
  private JMenuItem    mnuHelpContent;
  private JTabbedPane  tabbedPane;
  private TapeInFld    tabTapeIn;
  private TapeOutFld   tabTapeOut;


  public static void checkEnableAudio(
				ScreenFrm  screenFrm,
				Properties props )
  {
    if( props != null ) {
      boolean enabled = EmuUtil.getBooleanProperty(
				props,
				TapeInFld.PROP_PREFIX
					+ TapeInFld.PROP_ENABLED,
				false )
			|| EmuUtil.getBooleanProperty(
				props,
				TapeOutFld.PROP_PREFIX
					+ TapeOutFld.PROP_ENABLED,
				false );
      if( !enabled ) {
	int n = EmuUtil.getIntProperty(
			props,
			SoundFld.PROP_PREFIX + Main.PROP_COUNT,
			0 );
	for( int i = 0; i < n; i++ ) {
	  if( EmuUtil.getBooleanProperty(
		props,
		SoundFld.getPropPrefix( i ) + SoundFld.PROP_ENABLED,
		false ) )
	  {
	    enabled = true;
	    break;
	  }
	}
      }
      if( enabled ) {
	AudioFrm audioFrm = getSharedInstance( screenFrm );
	audioFrm.autoEnableProps = props;
	if( EmuUtil.getBooleanProperty(
			props,
			instance.getSettingsPrefix() + PROP_WINDOW_VISIBLE,
			true ) )
	{
	  EmuUtil.showFrame( audioFrm );
	}
      }
    }
  }


  public void checkOpenCPUSynchronLine() throws IOException
  {
    Z80CPU cpu = this.emuThread.getZ80CPU();
    if( !cpu.isActive() ) {
      throw new IOException( "Der emulierte Mikroprozessor ist gerade"
		+ " nicht aktiv.\n"
		+ "Aus diesem Grund kann kein Audiokanal ge\u00F6ffnet"
		+ " werden,\n"
		+ "der synchron zu diesem bedient werden soll." );
    }
    if( cpu.isPause() ) {
      throw new IOException( "Der emulierte Mikroprozessor ist gerade"
		+ " auf Pause gesetzt.\n"
		+ "Aus diesem Grund kann kein Audiokanal ge\u00F6ffnet"
		+ " werden,\n"
		+ "der synchron zu diesem bedient werden soll." );
    }
    AudioIO.checkOpenExclCPUSynchronLine();
  }


  public int getAndCheckSpeed()
  {
    int khz = this.emuThread.getZ80CPU().getMaxSpeedKHz();
    if( khz <= 0 ) {
      BaseDlg.showErrorDlg(
	this,
	"Sie m\u00FCssen die Geschwindigkeit des Emulators\n"
		+ "auf einen konkreten Wert begrenzen, da dieser\n"
		+ "als Zeitbasis f\u00FCr das Audiosystem dient.\n" );
    }
    return khz;
  }


  public ScreenFrm getScreenFrm()
  {
    return this.screenFrm;
  }


  public synchronized static AudioFrm getSharedInstance( ScreenFrm screenFrm )
  {
    if( instance == null ) {
      instance = new AudioFrm( screenFrm );
    }
    return instance;
  }


  public static void lazyFireEnableAudio(
			final boolean     tapeIn,
			final boolean     tapeOut,
			final Set<String> soundDeviceNames )
  {
    if( (instance != null)
	&& (tapeIn || tapeOut || !soundDeviceNames.isEmpty()) )
    {
      /*
       * Das Aktivieren der Audiofunktionen muss solange verzoegert werden,
       * bis das eventuelle vorherige Deaktivieren fertig ist.
       */
      final AudioFrm instance1 = instance;
      (new java.util.Timer( "jkcemu audio timer" )).schedule(
		new TimerTask()
		{
		  @Override
		  public void run()
		  {
		    EventQueue.invokeLater(
				new Runnable()
				{
				  @Override
				  public void run()
				  {
				    instance1.enableAudio(
							tapeIn,
							tapeOut,
							soundDeviceNames );
				  }
				} );
		  }
		},
		300L );
    }
  }


  public static AudioFrm open( ScreenFrm screenFrm )
  {
    return EmuUtil.showFrame( getSharedInstance( screenFrm ) );
  }


  public void openFile( File file, byte[] fileBytes, int offs )
  {
    this.tabTapeIn.openFile( file, fileBytes, offs );
  }


	/* --- ComponentListener --- */

  @Override
  public void componentHidden( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentMoved( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentResized( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentShown( ComponentEvent e )
  {
    if( e.getSource() == this )
      pack();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.mnuClose ) {
      rv = true;
      doClose();
    }
    else if( src == this.mnuHelpContent ) {
      rv = true;
      HelpFrm.openPage( HELP_PAGE );
    }
    return rv;
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    super.putSettingsTo( props );
    this.tabTapeIn.putSettingsTo( props );
    this.tabTapeOut.putSettingsTo( props );

    int nSnds = 0;
    int nTabs = this.tabbedPane.getTabCount();
    for( int i = 0; i < nTabs; i++ ) {
      Component c = this.tabbedPane.getComponentAt( i );
      if( c != null ) {
	if( c instanceof SoundFld ) {
	  ((SoundFld) c).putSettingsTo( props );
	  nSnds++;
	}
      }
    }
    EmuUtil.setProperty(
		props,
                SoundFld.PROP_PREFIX + Main.PROP_COUNT,
		nSnds );
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    if( newEmuSys != null ) {

      // Titel des selektierten Tab merken
      String selectedTitle = null;
      try {
	int idx = this.tabbedPane.getSelectedIndex();
	if( idx >= 0 ) {
	  selectedTitle = this.tabbedPane.getTitleAt( idx );
	}
      }
      catch( IndexOutOfBoundsException ex ) {}

      // alte Tabs entfernen
      this.tabbedPane.removeAll();

      // benoetigte Tabs hinzufuegen
      int idxToSelect = -1;
      if( newEmuSys.supportsTapeIn() ) {
	this.tabbedPane.addTab( "Eingang Kassette", this.tabTapeIn );
	this.tabTapeIn.updFieldsEnabled();
	if( this.tabTapeIn.checkEnableAudio( this.autoEnableProps ) ) {
	  idxToSelect = this.tabbedPane.getTabCount() - 1;
	}
      }
      if( newEmuSys.supportsTapeOut() ) {
	this.tabbedPane.addTab( "Ausgang Kassette", this.tabTapeOut );
	this.tabTapeOut.updFieldsEnabled();
	if( this.tabTapeOut.checkEnableAudio( this.autoEnableProps ) ) {
	  idxToSelect = this.tabbedPane.getTabCount() - 1;
	}
      }
      int sndDevNum = 0;
      for( AbstractSoundDevice sndDev : newEmuSys.getSoundDevices() ) {
	SoundFld sndFld = new SoundFld(
				this,
				this.emuThread,
				sndDevNum++,
				sndDev );
	this.tabbedPane.addTab( sndDev.toString(), sndFld );
	if( sndFld.checkEnableAudio( this.autoEnableProps ) ) {
	  idxToSelect = this.tabbedPane.getTabCount() - 1;
	}
      }
      this.autoEnableProps = null;

      /*
       * Wenn kein Tab durch die Properties aktiviert
       * und deshalb auch selektiert wurde,
       * den letzten mit gleichem Namen wieder selektieren.
       * Wenn kein gleicher Name gefunden wird,
       * dann den letzten Tab auswaehlen, da dieser
       * wegen Tonausgabe am wahrscheinlichsten verwendet wird.
       */
      int nTabs = this.tabbedPane.getTabCount();
      try {
	if( (idxToSelect < 0) && (selectedTitle != null) ) {
	  for( int i = (nTabs - 1); i >= 0; --i ) {
	    if( TextUtil.equals(
			this.tabbedPane.getTitleAt( i ),
			selectedTitle ) )
	    {
	      idxToSelect = i;
	      break;
	    }
	  }
	}
	if( idxToSelect < 0 ) {
	  idxToSelect = nTabs - 1;
	}
      }
      catch( IndexOutOfBoundsException ex ) {}

      // Fenstergroesse anpassen oder Fenster schliessen
      if( nTabs > 0 ) {
	pack();
	if( idxToSelect >= 0 ) {
	  final int idx = idxToSelect;
	  EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    selectTab( idx );
			  }
			} );
	}
      } else {
	doClose();
      }
    }
  }


	/* --- private Methoden --- */

  private void enableAudio(
			boolean     tapeIn,
			boolean     tapeOut,
			Set<String> soundDeviceNames )
  {
    if( tapeIn ) {
      this.tabTapeIn.doEnable( false );
    }
    if( tapeOut ) {
      this.tabTapeOut.doEnable( false );
    }
    try {
      int n = this.tabbedPane.getTabCount();
      for( int i = 0; i < n; i++ ) {
	Component c = this.tabbedPane.getComponentAt( i );
	String    s = this.tabbedPane.getTitleAt( i );
	if( (c != null) && (s != null) ) {
	  if( c instanceof SoundFld ) {
	    if( soundDeviceNames.contains( s ) ) {
	      ((SoundFld) c).doEnable( false );
	    }
	  }
	}
      }
    }
    catch( IndexOutOfBoundsException ex ) {}
  }


  private void selectTab( int idx )
  {
    if( (idx >= 0) && (idx < this.tabbedPane.getTabCount()) ) {
      try {
	this.tabbedPane.setSelectedIndex( idx );
      }
      catch( IndexOutOfBoundsException ex ) {}
    }
  }


	/* --- Konstruktor --- */

  private AudioFrm( ScreenFrm screenFrm )
  {
    this.screenFrm       = screenFrm;
    this.emuThread       = screenFrm.getEmuThread();
    this.tabTapeIn       = new TapeInFld( this, this.emuThread );
    this.tabTapeOut      = new TapeOutFld( this, this.emuThread );
    this.speedKHz        = 0;
    this.autoEnableProps = null;
    setTitle( "JKCEMU Audio/Kassette" );


    // Menu Datei
    JMenu mnuFile = createMenuFile();
    this.mnuClose = createMenuItemClose();
    mnuFile.add( this.mnuClose );


    // Menu Hilfe
    JMenu mnuHelp       = createMenuHelp();
    this.mnuHelpContent = createMenuItem( "Hilfe zu Audio/Kassette..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuHelp ) );


    // Fensterinhalt
    setLayout( new BorderLayout() );
    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );


    // sonstiges
    resetFired( emuThread.getEmuSys(), Main.getProperties() );
    if( !applySettings( Main.getProperties() ) ) {
      setLocationByPlatform( true );
    }
    setResizable( true );
    addComponentListener( this );
  }
}
