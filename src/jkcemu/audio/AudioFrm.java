/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer die Audiofunktionen zur Emulation
 * des Kassettenrecortderanschlusses bzw. des Tonausgangs
 */

package jkcemu.audio;

import java.io.File;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.EventObject;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuThread;
import jkcemu.base.HelpFrm;
import jkcemu.base.ScreenFrm;


public class AudioFrm extends AbstractAudioFrm
{
  private static final String HELP_PAGE = "/help/audio.htm";

  private static AudioFrm instance = null;

  private ScreenFrm         screenFrm;
  private EmuThread         emuThread;
  private Mixer.Info[]      mixers;
  private int               speedKHz;
  private JMenuItem         mnuClose;
  private JMenuItem         mnuHelpContent;
  private JTabbedPane       tabbedPane;
  private TapeInComponent   tabTapeIn;
  private TapeOutComponent  tabTapeOut;
  private SoundOutComponent tabSoundOut;


  public int getAndCheckSpeed()
  {
    int khz = this.emuThread.getZ80CPU().getMaxSpeedKHz();
    if( khz <= 0 ) {
      BaseDlg.showErrorDlg(
	this,
	"Sie m\u00Fcssen die Geschwindigkeit des Emulators\n"
		+ "auf einen konkreten Wert begrenzen, da dieser\n"
		+ "als Zeitbasis f\u00FCr das AudioSystem dient.\n" );
    }
    return khz;
  }


  public ScreenFrm getScreenFrm()
  {
    return this.screenFrm;
  }


  public static AudioFrm lazyGetInstance()
  {
    return instance;
  }


  public static AudioFrm open( ScreenFrm screenFrm )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new AudioFrm( screenFrm );
    }
    instance.toFront();
    instance.setVisible( true );
    return instance;
  }


  public void openFile( File file, byte[] fileBytes, int offs )
  {
    this.tabTapeIn.openFile( file, fileBytes, offs );
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
      HelpFrm.open( HELP_PAGE );
    }
    return rv;
  }


  @Override
  public void resetFired()
  {
    this.tabTapeIn.resetFired();
    this.tabTapeOut.resetFired();
    this.tabSoundOut.resetFired();
  }


  @Override
  public void willReset()
  {
    this.tabTapeIn.willReset();
    this.tabTapeOut.willReset();
    this.tabSoundOut.willReset();
  }


	/* --- Konstruktor --- */

  private AudioFrm( ScreenFrm screenFrm )
  {
    this.screenFrm = screenFrm;
    this.emuThread = screenFrm.getEmuThread();
    this.mixers    = AudioSystem.getMixerInfo();
    this.speedKHz  = 0;
    setTitle( "JKCEMU Audio/Kassette" );
    Main.updIcon( this );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Fensterinhalt
    setLayout( new BorderLayout() );
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );

    this.tabTapeIn = new TapeInComponent( this, this.emuThread );
    this.tabbedPane.addTab( "Eingang Kassette", this.tabTapeIn );

    this.tabTapeOut = new TapeOutComponent( this, this.emuThread );
    this.tabbedPane.addTab( "Ausgang Kassette", this.tabTapeOut );

    this.tabSoundOut = new SoundOutComponent( this, this.emuThread );
    this.tabbedPane.addTab(
			"Ausgang Sound-Generator/Lautsprecher",
			this.tabSoundOut );


    // sonstiges
    pack();
    setResizable( false );
    if( !applySettings( Main.getProperties(), false ) ) {
      setLocationByPlatform( true );
    }
  }
}
