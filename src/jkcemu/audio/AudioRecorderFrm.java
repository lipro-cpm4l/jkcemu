/*
 * (c) 2014-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Audio Recorder
 */

package jkcemu.audio;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.EventObject;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.etc.ReadableByteArrayOutputStream;
import jkcemu.file.FileUtil;


public class AudioRecorderFrm extends BaseFrm implements Runnable
{
  public class PCMGZipDataBuf extends PCMDataStream
  {
    private ReadableByteArrayOutputStream dataBuf;

    public PCMGZipDataBuf(
		  int                           frameRate,
		  int                           sampleSizeInBits,
		  int                           channels,
		  boolean                       dataSigned,
		  boolean                       bigEndian,
		  ReadableByteArrayOutputStream dataBuf,
		  long                          pcmDataLen )
							throws IOException
    {
      super(
	  frameRate,
	  sampleSizeInBits,
	  channels,
	  dataSigned,
	  bigEndian,
	  new GZIPInputStream( dataBuf.newInputStream() ),
	  pcmDataLen );
      this.dataBuf = dataBuf;
    }

    @Override
    public synchronized void setFramePos( long framePos ) throws IOException
    {
      if( framePos < 0 ) {
	framePos = 0;
      }
      long curPos = getFramePos();
      if( framePos > curPos ) {
	if( !this.eof ) {
	  this.bufLen = 0;
	  this.bufPos = 0;
	  this.totalRead += this.in.skip(
				(framePos - curPos) * this.bytesPerFrame );
	}
      } else if( framePos < curPos ) {
	EmuUtil.closeSilently( this.in );
	this.eof    = false;
	this.bufLen = 0;
	this.bufPos = 0;
	this.in     = new GZIPInputStream(
				this.dataBuf.newInputStream() );
	if( framePos > 0 ) {
	  this.totalRead = this.in.skip( framePos * this.bytesPerFrame );
	} else {
	  this.totalRead = 0;
	}
      }
    }

    @Override
    public boolean supportsSetFramePos()
    {
      return true;
    }
  };


  public static final String TITLE = Main.APPNAME + " Audiorecorder";

  private static final String TEXT_START          = "Start";
  private static final String TEXT_STOP           = "Stop";
  private static final String DEFAULT_STATUS_TEXT = "Bereit";
  private static final String HELP_PAGE = "/help/tools/audiorecorder.htm";

  private static AudioRecorderFrm instance = null;

  private AudioFormat                   audioFmt;
  private Thread                        audioThread;
  private Mixer.Info                    mixerInfo;
  private VolumeBar                     volumeBar;
  private ReadableByteArrayOutputStream dataBuf;
  private boolean                       dataSaved;
  private volatile boolean              recording;
  private volatile boolean              recEnabled;
  private volatile long                 begMillis;
  private int                           frameRate;
  private int                           sampleSizeInBits;
  private int                           channels;
  private volatile int                  dataLen;
  private javax.swing.Timer             timerDuration;
  private JLabel                        labelDuration;
  private JLabel                        labelDurationValue;
  private JLabel                        labelMixer;
  private JLabel                        labelFrameRate;
  private JLabel                        labelSampleSize;
  private JLabel                        labelVolume;
  private JComboBox<Object>             comboMixer;
  private JComboBox<Object>             comboFrameRate;
  private JRadioButton                  rb8Bit;
  private JRadioButton                  rb16Bit;
  private JRadioButton                  rbMono;
  private JRadioButton                  rbStereo;
  private JButton                       btnStartStop;
  private JButton                       btnPlay;
  private JButton                       btnSave;
  private JButton                       btnHelp;
  private JButton                       btnClose;


  public static AudioRecorderFrm open()
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new AudioRecorderFrm();
    }
    instance.toFront();
    instance.setVisible( true );
    return instance;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    Exception      errEx   = null;
    TargetDataLine line    = null;
    if( (this.frameRate > 0)
	&& (this.sampleSizeInBits > 0)
	&& (this.channels > 0)
	&& (this.dataBuf != null) )
    {
      GZIPOutputStream out = null;
      try {
	AudioFormat fmt             = null;
	boolean     isSigned        = false;
	boolean     lineUnavailable = false;

	/*
	 * Bzgl. signed / unsigned und little endian / big endian
	 * laesst sich ein Audiokanal nicht unbedingt mit allen
	 * Kombinationen oeffnen.
	 * Aus diesem Grund werden die Kombinationen durchprobiert.
	 */
	for( int i = 0; (line == null) && (i < 2); i++ ) {
	  for( int k = 0; (line == null) && (k < 2); k++ ) {
	    try {
	      isSigned = (k == 1);
	      fmt      = new AudioFormat(
				(float) this.frameRate,
				this.sampleSizeInBits,
				this.channels,
				isSigned,
				i == 1 );
	      Mixer.Info mixerInfo = this.mixerInfo;
	      if( mixerInfo != null ) {
		line = AudioSystem.getTargetDataLine( fmt, mixerInfo );
	      } else {
		line = AudioSystem.getTargetDataLine( fmt );
	      }
	      if( line != null ) {
		line.open( fmt );
		line.flush();
		line.start();
		this.recording = true;
	      }
	    }
	    catch( Exception ex ) {
	      close( line );
	      line = null;
	      fmt  = null;
	      if( ex instanceof LineUnavailableException ) {
		lineUnavailable = true;
	      }
	    }
	  }
	}
	if( (fmt != null) && (line != null) ) {
	  int frameSize = fmt.getFrameSize();
	  if( frameSize > 0 ) {
	    this.dataSaved = false;
	    this.dataLen   = 0;
	    this.dataBuf.reset();

	    // intern gespeicherte Daten komprimieren
	    out = new GZIPOutputStream( this.dataBuf );

	    /*
	     * Wertebereich der Pegelanzeige,
	     * Bei 16 Bit wird nur das hoechstwertige Byte verwendet.
	     */
	    if( isSigned ) {
	      this.volumeBar.setVolumeLimits( -128, 127 );
	    } else {
	      this.volumeBar.setVolumeLimits( 0, 255 );
	    }

	    /*
	     * Puffer fuer 100 ms anlegen und ermitteln,
	     * welche Bytes davon (Index) das jeweils hoechstwertige ist
	     */
	    int bufFrameCnt = this.frameRate / 10;
	    if( bufFrameCnt < 1 ) {
	      bufFrameCnt = 1;
	    }
	    byte[] audioBuf = new byte[ frameSize * bufFrameCnt ];
	    int    hiIdx = 0;
	    if( (frameSize > 1) && !fmt.isBigEndian() ) {
	      hiIdx = frameSize - 1;
	    }

	    // Aufnahme beginnen
	    this.audioFmt  = fmt;
	    this.begMillis = System.currentTimeMillis();
	    while( this.recEnabled ) {
	      if( line.read( audioBuf, 0, audioBuf.length )
						  != audioBuf.length )
	      {
		break;
	      }
	      out.write( audioBuf );
	      this.dataLen += audioBuf.length;

	      // Anzeige aktualisieren
	      int idx = hiIdx;
	      while( idx < audioBuf.length ) {
		if( isSigned ) {
		  this.volumeBar.updVolume( (int) audioBuf[ idx ] );
		} else {
		  this.volumeBar.updVolume( ((int) audioBuf[ idx ]) & 0xFF );
		}
		idx += frameSize;
	      }
	    }
	    out.finish();
	    out.close();
	  }
	}
	if( (this.dataLen == 0) && lineUnavailable ) {
	  errEx = new IOException( AudioIO.ERROR_LINE_UNAVAILABLE );
	}
      }
      catch( Exception ex ) {
	errEx = ex;
      }
      catch( OutOfMemoryError e ) {
	out          = null;
	this.dataLen = 0;
	this.dataBuf.resetAndFreeMem();
	System.gc();
	errEx = new IOException( AudioIO.ERROR_RECORDING_OUT_OF_MEMORY );
      }
      finally {
	EmuUtil.closeSilently( out );
	close( line );
      }
    }
    final Exception retEx = (this.recEnabled ? errEx : null);
    EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    recFinished( retEx );
		  }
		} );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      Object  src = e.getSource();
      if( src == this.btnStartStop ) {
	rv = true;
	doStartStop();
      }
      else if( src == this.btnPlay ) {
	rv = true;
	doPlay();
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
    catch( Exception ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = false;
    if( this.recEnabled ) {
      try {
	doStartStop();
      }
      catch( IOException ex ) {}
    }
    if( confirmDataSaved() ) {
      if( Main.isTopFrm( this ) ) {
	rv = EmuUtil.closeOtherFrames( this );
	if( rv ) {
	  rv = super.doClose();
	}
	if( rv ) {
	  Main.exitSuccess();
	}
      } else {
	rv = super.doClose();
      }
      if( rv ) {
	this.dataLen   = 0;
	this.dataSaved = true;
	updDuration();
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private AudioRecorderFrm()
  {
    this.audioFmt         = null;
    this.audioThread      = null;
    this.mixerInfo        = null;
    this.dataBuf          = null;
    this.dataSaved        = true;
    this.dataLen          = 0;
    this.frameRate        = 0;
    this.sampleSizeInBits = 0;
    this.channels         = 0;
    this.begMillis        = -1;
    this.recording        = false;
    this.recEnabled       = false;
    setTitle( TITLE );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    // Labels
    this.labelMixer = GUIFactory.createLabel( "Ger\u00E4t:" );
    add( this.labelMixer, gbc );

    this.labelFrameRate = GUIFactory.createLabel( "Abtastrate (Hz):" );
    gbc.gridy++;
    add( this.labelFrameRate, gbc );

    this.labelSampleSize = GUIFactory.createLabel( "Aufl\u00F6sung:" );
    gbc.gridy++;
    add( this.labelSampleSize, gbc );

    this.labelVolume = GUIFactory.createLabel( "Pegel:" );
    gbc.gridy += 2;
    add( this.labelVolume, gbc );

    this.labelDuration = GUIFactory.createLabel( "Aufgenommene Zeit:" );
    this.labelDuration.setEnabled( false );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.labelDuration, gbc );


    // Mixer
    this.comboMixer   = AudioUtil.createMixerComboBox( true );
    gbc.anchor        = GridBagConstraints.WEST;
    gbc.gridy         = 0;
    gbc.insets.bottom = 0;
    gbc.gridwidth     = 3;
    gbc.gridx++;
    add( this.comboMixer, gbc );


    // Abtastrate
    this.comboFrameRate = AudioUtil.createFrameRateComboBox();
    gbc.gridy++;
    add( this.comboFrameRate, gbc );


    // Aufloesung
    ButtonGroup grpBits = new ButtonGroup();

    this.rb8Bit = GUIFactory.createRadioButton( "8 Bit" );
    grpBits.add( this.rb8Bit );
    gbc.gridwidth = 1;
    gbc.gridy++;
    add( this.rb8Bit, gbc );

    this.rb16Bit = GUIFactory.createRadioButton( "16 Bit", true );
    grpBits.add( this.rb16Bit );
    gbc.gridx++;
    add( this.rb16Bit, gbc );
    --gbc.gridx;


    // Mono/Stereo
    ButtonGroup grpChannels = new ButtonGroup();

    this.rbMono = GUIFactory.createRadioButton( "Mono", true );
    grpChannels.add( this.rbMono );
    gbc.gridy++;
    add( this.rbMono, gbc );

    this.rbStereo = GUIFactory.createRadioButton( "Stereo" );
    grpChannels.add( this.rbStereo );
    gbc.gridx++;
    add( this.rbStereo, gbc );
    --gbc.gridx;


    // Pegel
    this.volumeBar = new VolumeBar( SwingConstants.HORIZONTAL );
    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weightx    = 1.0;
    gbc.gridwidth  = 3;
    gbc.gridy++;
    add( this.volumeBar, gbc );


    // Dauer
    this.labelDurationValue = GUIFactory.createLabel();
    this.labelDurationValue.setEnabled( false );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.labelDurationValue, gbc );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 5, 1, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.NORTHEAST;
    gbc.insets.left = 10;
    gbc.gridheight  = 6;
    gbc.gridy       = 0;
    gbc.gridx += 3;
    add( panelBtn, gbc );

    this.btnStartStop = GUIFactory.createButton( TEXT_START );
    panelBtn.add( this.btnStartStop );

    this.btnPlay = GUIFactory.createButton( EmuUtil.TEXT_PLAY );
    this.btnPlay.setEnabled( false );
    panelBtn.add( this.btnPlay );

    this.btnSave = GUIFactory.createButton( EmuUtil.TEXT_OPEN_SAVE );
    this.btnSave.setEnabled( false );
    panelBtn.add( this.btnSave );

    this.btnHelp = GUIFactory.createButtonHelp();
    panelBtn.add( this.btnHelp );

    this.btnClose = GUIFactory.createButtonClose();
    panelBtn.add( this.btnClose );


    // Listener
    this.btnStartStop.addActionListener( this );
    this.btnPlay.addActionListener( this );
    this.btnSave.addActionListener( this );
    this.btnHelp.addActionListener( this );
    this.btnClose.addActionListener( this );


    // sonstiges
    pack();
    setResizable( false );
    if( !applySettings( Main.getProperties() ) ) {
      setScreenCentered();
    }
    updFieldsEnabled();


    // Timer
    this.timerDuration = new javax.swing.Timer(
			1000,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    updDuration();
			  }
			} );
  }


	/* --- Aktionen --- */

  private void doStartStop() throws IOException
  {
    if( this.recEnabled ) {
      this.recEnabled = false;
      this.btnStartStop.setText( TEXT_START );
    } else {
      if( this.audioThread == null ) {
	if( confirmDataSaved() ) {
	  this.mixerInfo = AudioUtil.getSelectedMixerInfo( this.comboMixer );
	  this.frameRate = AudioUtil.getSelectedFrameRate(
						this.comboFrameRate );
	  if( this.frameRate <= 0 ) {
	    this.frameRate = 44100;
	  }
	  this.sampleSizeInBits = (this.rb8Bit.isSelected() ? 8 : 16);
	  this.channels         = (this.rbMono.isSelected() ? 1 : 2);
	  if( this.dataBuf != null ) {
	    this.dataBuf.reset();
	  } else {
	    this.dataBuf = new ReadableByteArrayOutputStream( 0x100000 );
	  }
	  this.dataLen   = 0;
	  this.dataSaved = false;
	  this.btnPlay.setEnabled( false );
	  this.btnSave.setEnabled( false );
	  this.recEnabled  = true;
	  this.audioThread = new Thread(
					Main.getThreadGroup(),
					this,
					"JKCEMU sound recorder" );
	  this.audioThread.start();
	  this.labelDuration.setEnabled( true );
	  this.labelDurationValue.setEnabled( true );
	  this.begMillis = -1;
	  updDuration();
	  updFieldsEnabled();
	  this.volumeBar.setVolumeBarState( true );
	  this.timerDuration.start();
	  this.btnStartStop.setText( TEXT_STOP );
	}
      } else {
	this.btnStartStop.setText( TEXT_STOP );
      }
    }
  }


  private void doPlay() throws IOException
  {
    if( (this.audioFmt != null)
	&& (this.dataBuf != null)
	&& (this.dataLen > 0) )
    {
      AudioPlayFrm.open(
		createPCMDataSource(),
		"Wiedergabe der Aufnahme..." );
    }
  }


  private void doSave() throws IOException
  {
    if( (this.audioFmt != null)
	&& (this.dataBuf != null)
	&& (this.dataLen > 0) )
    {
      File file = FileUtil.showFileSaveDlg(
			this,
			"Sound-Datei speichern",
			Main.getLastDirFile( Main.FILE_GROUP_AUDIO ),
			AudioFile.getFileFilter() );
      if( file != null ) {
	AudioFile.write( createPCMDataSource(), file );
	Main.setLastFile( file, Main.FILE_GROUP_AUDIO );
	this.dataSaved = true;
      }
    }
  }


	/* --- private Methoden --- */

  private void close( DataLine line )
  {
    if( line != null ) {
      this.recording = false;
      line.stop();
      line.flush();
      line.close();
    }
  }


  private boolean confirmDataSaved()
  {
    boolean rv = true;
    if( (this.audioFmt != null)
	&& (this.dataBuf != null)
	&& (this.dataLen > 0)
	&& !this.dataSaved )
    {
      rv = BaseDlg.showSuppressableConfirmDlg(
		this,
		"Die Aufnahme wurde noch nicht gespeichert\n"
			+ "und wird somit verworfen." );
    }
    return rv;
  }


  private PCMDataSource createPCMDataSource() throws IOException
  {
    return new PCMGZipDataBuf(
			Math.round( this.audioFmt.getSampleRate() ),
			this.audioFmt.getSampleSizeInBits(),
			this.audioFmt.getChannels(),
			this.audioFmt.getEncoding()
				== AudioFormat.Encoding.PCM_SIGNED,
			this.audioFmt.isBigEndian(),
			this.dataBuf,
			this.dataLen );
  }


  private void setFrameRateState( boolean state )
  {
    this.labelFrameRate.setEnabled( state );
    this.comboFrameRate.setEnabled( state );
  }


  private void setMixerState( boolean state )
  {
    this.labelMixer.setEnabled( state );
    this.comboMixer.setEnabled( state );
  }


  private void recFinished( Exception ex )
  {
    this.volumeBar.setVolumeBarState( false );
    this.timerDuration.stop();
    this.recEnabled  = false;
    this.audioThread = null;
    updDuration();
    updFieldsEnabled();
    boolean state = false;
    if( this.dataLen > 0 ) {
      this.dataSaved = false;
      if( (this.audioFmt != null) && (this.dataBuf != null) ) {
	state = true;
      }
      if( ex != null ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    } else {
      showError( ex != null ? ex.getMessage() : null );
    }
    this.labelDuration.setEnabled( state );
    this.labelDurationValue.setEnabled( state );
    this.btnStartStop.setText( TEXT_START );
    this.btnPlay.setEnabled( state );
    this.btnSave.setEnabled( state );
  }


  private void showError( String errorText )
  {
    if( errorText == null ) {
      if( this.frameRate > 0 ) {
	errorText = "Es konnte kein Audiokanal mit den angegebenen"
				+ " Optionen ge\u00F6ffnet werden.";
      } else {
	errorText = "Es konnte kein Audiokanal ge\u00F6ffnet werden.";
      }
    }
    BaseDlg.showErrorDlg( this, errorText );
  }


  private void updDuration()
  {
    int seconds = -1;
    if( this.dataBuf != null ) {
      if( this.recording && (this.begMillis > 0) ) {
	seconds = (int) ((System.currentTimeMillis() - this.begMillis)
								/ 1000);
      } else {
	if( this.audioFmt != null ) {
	  int bps = (this.audioFmt.getSampleSizeInBits() + 7) / 8;
	  seconds = this.dataLen
			/ Math.round( this.audioFmt.getSampleRate() )
			/ this.audioFmt.getChannels()
			/ bps;
	}
      }
    }
    String text = "";
    if( seconds >= 0 ) {
      int minutes = seconds / 60;
      seconds     = seconds % 60;
      if( minutes > 0 ) {
	text = String.format( "%d:%02d Minuten", minutes, seconds );
      } else {
	if( seconds == 1 ) {
	  text = "1 Sekunde";
	} else {
	  text = String.format( "%d Sekunden", seconds );
	}
      }
      this.labelDurationValue.setText( text );
      if( minutes >= AudioUtil.RECORDING_MINUTES_MAX ) {
	this.recEnabled = false;
      }
    }
  }


  private void updFieldsEnabled()
  {
    boolean state = (this.audioThread == null);
    setMixerState( state );
    setFrameRateState( state );
    this.labelSampleSize.setEnabled( state );
    this.rb8Bit.setEnabled( state );
    this.rb16Bit.setEnabled( state );
    this.rbMono.setEnabled( state );
    this.rbStereo.setEnabled( state );

    state = !state;
    this.labelVolume.setEnabled( state );
    this.volumeBar.setEnabled( state );
  }
}
