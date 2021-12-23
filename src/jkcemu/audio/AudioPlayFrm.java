/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Audio Player
 */

package jkcemu.audio;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.EventObject;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class AudioPlayFrm
			extends BaseFrm
			implements ChangeListener, Runnable
{
  private static final int    PROGRESS_MAX   = 1000;
  private static final double D_PROGRESS_MAX = (double) PROGRESS_MAX;

  private volatile static String lastMixer = null;

  private volatile PCMDataSource pcm;
  private volatile long          frameCount;
  private volatile double        dFrameCount;
  private volatile Mixer.Info    mixerInfo;
  private volatile boolean       runEnabled;
  private boolean                cancelled;
  private boolean                pause;
  private Thread                 thread;
  private javax.swing.Timer      timer;
  private JComboBox<Object>      comboMixer;
  private JProgressBar           progressBar;
  private JSlider                sliderFramePos;
  private JButton                btnCancel;
  private JButton                btnPause;
  private JButton                btnPlay;
  private JButton                btnReplay;


  public static void open(
			PCMDataSource pcm,
			String        title )
  {
    if( (pcm != null) && (title != null) ) {
      (new AudioPlayFrm( pcm, title )).setVisible( true );
    }
  }


  public static void open( Component owner, File file )
  {
    if( file != null ) {
      try {
	PCMDataSource pcm = AudioUtil.openAudioOrTapeFile( file );
	if( pcm != null ) {
	  (new AudioPlayFrm(
		pcm,
		"Wiedergabe von " + file.getName() + "..." )
	  ).setVisible( true );
	}
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( owner, ex );
      }
    }
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( (this.sliderFramePos != null)
	&& (e.getSource() == this.sliderFramePos)
	&& (this.dFrameCount > 0.0) )
    {
      long framePos = Math.round( (double) this.sliderFramePos.getValue()
						/ D_PROGRESS_MAX
						* this.dFrameCount );
      if( framePos < 0 ) {
	framePos = 0;
      } else if( framePos > this.frameCount ) {
	framePos = this.frameCount;
      }
      try {
	this.pcm.setFramePos( framePos );
      }
      catch( IOException ex ) {}
    }
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    boolean          endReached = true;
    String           errText    = null;
    SourceDataLine   line       = null;
    AudioInputStream ais        = null;
    try {
      if( this.pcm != null ) {
	boolean signed           = this.pcm.isSigned();
	int     channels         = this.pcm.getChannels();
	int     sampleSizeInBits = this.pcm.getSampleSizeInBits();
	if( sampleSizeInBits < 8 ) {
	  sampleSizeInBits = 8;
	}
	ais = new AudioInputStream(
			new PCMConverterInputStream(
					this.pcm,
					signed,
					false,
					true ),
			new AudioFormat(
					(float) this.pcm.getFrameRate(),
					sampleSizeInBits,
					channels,
					signed,
					false ),
			this.pcm.getFrameCount() );
	AudioFormat fmt = ais.getFormat();
	if( (fmt != null) && (ais.getFrameLength() > 0) ) {
	  Mixer.Info mixerInfo = this.mixerInfo;
	  if( mixerInfo != null ) {
	    line      = AudioSystem.getSourceDataLine( fmt, mixerInfo );
	    lastMixer = mixerInfo.getName();
	  } else {
	    line      = AudioSystem.getSourceDataLine( fmt );
	    lastMixer = null;
	  }
	  if( line != null ) {
	    if( !line.isOpen() ) {
	      line.open();
	    }
	    if( !line.isActive() ) {
	      line.start();
	    }
	    int bufSize = (int) fmt.getFrameRate() / 8
					* fmt.getFrameSize()
					* fmt.getChannels();
	    if( bufSize < 256 ) {
	      bufSize = 256;
	    }
	    byte[] buf    = new byte[ bufSize ];
	    int    nBytes = this.pcm.read( buf, 0, buf.length );
	    while( this.runEnabled && (nBytes > 0) ) {
	      line.write( buf, 0, nBytes );
	      nBytes = this.pcm.read( buf, 0, buf.length );
	    }
	    if( this.runEnabled ) {
	      line.drain();
	    } else {
	      line.flush();
	    }
	    if( nBytes > 0 ) {
	      endReached = false;
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {
      StringBuilder buf = new StringBuilder( 256 );
      buf.append( "Wiedergabe nicht m\u00F6glich" );
      String msg = ex.getMessage();
      if( msg != null ) {
	if( !msg.isEmpty() ) {
	  buf.append( "\n\n" );
	  Mixer.Info mixerInfo = this.mixerInfo;
	  if( mixerInfo != null ) {
	    String s = mixerInfo.getName();
	    if( s != null ) {
	      if( !s.isEmpty() ) {
		buf.append( s );
		buf.append( ":\n" );
	      }
	    }
	  }
	  buf.append( msg );
	}
      }
      errText = buf.toString();
    }

    EmuUtil.closeSilently( ais );
    if( line != null ) {
      try {
	line.stop();
      }
      catch( Exception ex ) {}
      try {
	line.close();
      }
      catch( Exception ex ) {}
    }

    final boolean endReached1 = endReached;
    final String  errText1    = errText;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    threadFinished( errText1, endReached1 );
		  }
		} );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.comboMixer ) {
      rv              = true;
      this.runEnabled = false;
    } else if( src == this.btnCancel ) {
      rv = true;
      doClose();
    } else if( src == this.btnPause ) {
      rv = true;
      doPause();
    } else if( src == this.btnPlay ) {
      rv = true;
      doPlay( false );
    } else if( src == this.btnReplay ) {
      rv = true;
      doPlay( true );
    } else if( src == this.timer ) {
      rv = true;
      doTimer();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = false;
    if( this.thread != null ) {
      this.cancelled  = true;
      this.runEnabled = false;
    } else {
      rv = super.doClose();
      if( rv ) {
	EmuUtil.closeSilently( this.pcm );
	if( this.timer.isRunning() ) {
	  this.timer.stop();
	}
	this.comboMixer.removeActionListener( this );
	if( this.sliderFramePos != null ) {
	  this.sliderFramePos.removeChangeListener( this );
	}
	if( this.btnReplay != null ) {
	  this.btnReplay.removeActionListener( this );
	}
	this.btnPause.removeActionListener( this );
	this.btnPlay.removeActionListener( this );
	this.btnCancel.removeActionListener( this );
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private AudioPlayFrm( PCMDataSource pcm, String title )
  {
    setTitle( Main.APPNAME + " Audioplayer" );
    this.pcm         = pcm;
    this.frameCount  = pcm.getFrameCount();
    this.dFrameCount = (double) frameCount;
    this.mixerInfo   = null;
    this.runEnabled  = false;
    this.cancelled   = false;
    this.pause       = false;
    this.thread      = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    JPanel panelMixer = GUIFactory.createPanel();
    add( panelMixer, gbc );

    panelMixer.setLayout( new BoxLayout( panelMixer, BoxLayout.X_AXIS ) );
    panelMixer.add( GUIFactory.createLabel( "Ausgabeger\u00E4t:" ) );
    panelMixer.add( Box.createHorizontalStrut( 5 ) );

    this.comboMixer = AudioUtil.createMixerComboBox( false );
    panelMixer.add( this.comboMixer );
    EmuUtil.setSelectedItem( this.comboMixer, lastMixer );

    gbc.insets.top = 15;
    gbc.gridy++;
    add( GUIFactory.createLabel( title ), gbc );

    this.progressBar    = null;
    this.sliderFramePos = null;
    this.btnReplay      = null;
    gbc.fill            = GridBagConstraints.HORIZONTAL;
    gbc.weightx         = 1.0;
    gbc.insets.top      = 0;
    gbc.gridy++;
    if( pcm.supportsSetFramePos() ) {
      this.sliderFramePos = GUIFactory.createSlider(
					SwingConstants.HORIZONTAL,
					0,
					PROGRESS_MAX,
					0 );
      this.sliderFramePos.setPaintLabels( false );
      this.sliderFramePos.setPaintTicks( false );
      this.sliderFramePos.setPaintTrack( true );
      add( this.sliderFramePos, gbc );

      this.btnReplay = GUIFactory.createRelImageResourceButton(
					this,
					"audio/replay.png",
					"Wiederholen" );
    } else {
      this.progressBar = GUIFactory.createProgressBar(
					SwingConstants.HORIZONTAL,
					0,
					PROGRESS_MAX );
      add( this.progressBar, gbc );
    }

    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = 1;
    gbc.gridy++;
    if( this.btnReplay != null ) {
      add( this.btnReplay, gbc );
      gbc.gridx++;
    }

    this.btnPause = GUIFactory.createRelImageResourceButton(
					this,
					"audio/pause.png",
					"Pause" );
    add( this.btnPause, gbc );

    this.btnPlay = GUIFactory.createRelImageResourceButton(
					this,
					"audio/play.png",
					"Wiedergeben" );
    this.btnPlay.setEnabled( false );
    gbc.gridx++;
    add( this.btnPlay, gbc );

    this.btnCancel  = GUIFactory.createButton( EmuUtil.TEXT_CANCEL );
    gbc.anchor      = GridBagConstraints.EAST;
    gbc.weightx     = 1.0;
    gbc.insets.left = 20;
    gbc.gridx++;
    add( this.btnCancel, gbc );


    // Fenstergroesse und -position
    pack();
    setResizable( false );
    setLocationByPlatform( true );


    // Listener
    this.comboMixer.addActionListener( this );
    if( this.sliderFramePos != null ) {
      this.sliderFramePos.addChangeListener( this );
    }
    if( this.btnReplay != null ) {
      this.btnReplay.addActionListener( this );
    }
    this.btnPause.addActionListener( this );
    this.btnPlay.addActionListener( this );
    this.btnCancel.addActionListener( this );


    // Timer
    this.timer = new javax.swing.Timer( 200, this );
    this.timer.setInitialDelay( 500 );
    this.timer.start();
  }


	/* --- private Methoden --- */

  private void doPause()
  {
    this.btnPause.setEnabled( false );
    this.btnPlay.setEnabled( true );
    this.pause      = true;
    this.runEnabled = false;
    updProgressValue();
  }


  private void doPlay( boolean replay )
  {
    if( replay ) {
      try {
	this.pcm.setFramePos( 0 );
	setProgressValue( 0 );
      }
      catch( IOException ex ) {}
    }
    this.btnPause.setEnabled( true );
    this.btnPlay.setEnabled( false );
    this.pause      = false;
    this.runEnabled = true;
  }


  private void doTimer()
  {
    if( !this.cancelled && !this.pause ) {
      if( this.thread == null ) {
	this.mixerInfo  = AudioUtil.getSelectedMixerInfo( this.comboMixer );
	this.runEnabled = true;
	this.thread     = new Thread(
				Main.getThreadGroup(),
				this,
				Main.APPNAME + " audio player" );
	this.thread.start();
      }
      updProgressValue();
    }
  }


  private void setProgressValue( int value )
  {
    if( this.progressBar != null ) {
      this.progressBar.setValue( value );
    }
    if( this.sliderFramePos != null ) {
      this.sliderFramePos.removeChangeListener( this );
      this.sliderFramePos.setValue( value );
      this.sliderFramePos.addChangeListener( this );
    }
  }


  private void threadFinished( String errText, boolean endReached )
  {
    this.thread = null;
    if( errText != null ) {
      doPause();
      BaseDlg.showErrorDlg( this, errText );
    } else if( this.cancelled || endReached ) {
      doClose();
    }
  }


  private void updProgressValue()
  {
    int value = 0;
    if( this.dFrameCount > 0.0 ) {
      value = (int) Math.round( (double) this.pcm.getFramePos()
						/ this.dFrameCount
						* D_PROGRESS_MAX );
    }
    setProgressValue( value );
  }
}
