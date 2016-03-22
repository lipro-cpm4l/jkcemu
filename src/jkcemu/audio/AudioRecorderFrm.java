/*
 * (c) 2014-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Audio Recorder
 */

package jkcemu.audio;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.EventObject;
import java.util.zip.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.base.*;


public class AudioRecorderFrm
		extends AbstractAudioFrm
		implements Runnable
{
  public class DataBuf extends ByteArrayOutputStream
  {
    public DataBuf( int size )
    {
      super( size );
    }

    public InputStream newInputStream()
    {
      return new ByteArrayInputStream( this.buf, 0, this.count );
    }
  }


  private final static String TEXT_START          = "Start";
  private final static String TEXT_STOP           = "Stop";
  private final static String DEFAULT_STATUS_TEXT = "Bereit";

  private static AudioRecorderFrm instance = null;

  private AudioFormat       audioFmt;
  private Thread            audioThread;
  private Mixer             mixer;
  private DataBuf           dataBuf;
  private boolean           dataSaved;
  private boolean           suppressAskDataSaved;
  private volatile boolean  recording;
  private volatile boolean  recEnabled;
  private volatile long     begMillis;
  private int               sampleRate;
  private int               sampleSizeInBits;
  private int               channels;
  private volatile int      dataLen;
  private javax.swing.Timer timerDuration;
  private JLabel            labelDuration;
  private JLabel            labelDurationValue;
  private JLabel            labelSampleSize;
  private JLabel            labelVolume;
  private JRadioButton      btn8Bit;
  private JRadioButton      btn16Bit;
  private JRadioButton      btnMono;
  private JRadioButton      btnStereo;
  private JButton           btnStartStop;
  private JButton           btnPlay;
  private JButton           btnSave;
  private JButton           btnHelp;
  private JButton           btnClose;


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
    if( (this.sampleRate > 0)
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
	 * laesst sich ein Audio-Kanal nicht unbedingt mit allen
	 * Kombinationen oeffnen.
	 * Aus diesem Grund werden die Kombinationen durchprobiert.
	 */
	for( int i = 0; (line == null) && (i < 2); i++ ) {
	  for( int k = 0; (line == null) && (k < 2); k++ ) {
	    try {
	      isSigned = (k == 1);
	      fmt      = new AudioFormat(
				(float) this.sampleRate,
				this.sampleSizeInBits,
				this.channels,
				isSigned,
				i == 1 );
	      DataLine.Info info = new DataLine.Info(
						TargetDataLine.class,
						fmt );
	      if( this.mixer != null ) {
		if( this.mixer.isLineSupported( info ) ) {
		  line = (TargetDataLine) this.mixer.getLine( info );
		}
	      } else {
		if( AudioSystem.isLineSupported( info ) ) {
		  line = (TargetDataLine) AudioSystem.getLine( info );
		}
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
	      setVolumeLimits( -128, 127 );
	    } else {
	      setVolumeLimits( 0, 255 );
	    }

	    /*
	     * Puffer fuer 100 ms anlegen und ermitteln,
	     * welche Bytes davon (Index) das jeweils hoechstwertige ist
	     */
	    int bufFrameCnt = this.sampleRate / 10;
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
	      this.dataLen += bufFrameCnt;

	      // Anzeige aktualisieren
	      int idx = hiIdx;
	      while( idx < audioBuf.length ) {
		if( isSigned ) {
		  updVolume( (int) audioBuf[ idx ] );
		} else {
		  updVolume( ((int) audioBuf[ idx ]) & 0xFF );
		}
		idx += frameSize;
	      }
	    }
	    out.finish();
	    out.close();
	  }
	}
	if( (this.dataLen == 0) && lineUnavailable ) {
	  errEx = new IOException( AudioUtil.ERROR_TEXT_LINE_UNAVAILABLE );
	}
      }
      catch( Exception ex ) {
	errEx = ex;
      }
      finally {
	EmuUtil.doClose( out );
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
    boolean rv  = false;
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
      HelpFrm.open( "/help/tools/audiorecorder.htm" );
    }
    else if( src == this.btnClose ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = false;
    if( this.recEnabled ) {
      doStartStop();
    }
    if( confirmDataSaved() ) {
      rv = super.doClose();
      if( rv ) {
	this.dataLen   = 0;
	this.dataSaved = true;
	updDuration();
	Main.checkQuit( this );
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private AudioRecorderFrm()
  {
    this.audioFmt             = null;
    this.audioThread          = null;
    this.mixer                = null;
    this.dataBuf              = null;
    this.suppressAskDataSaved = false;
    this.dataSaved            = true;
    this.dataLen              = 0;
    this.sampleRate           = 0;
    this.sampleSizeInBits     = 0;
    this.channels             = 0;
    this.begMillis            = -1;
    this.recording            = false;
    this.recEnabled           = false;
    setTitle( "JKCEMU Audio-Recorder" );


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
    add( this.labelMixer, gbc );

    gbc.gridy++;
    add( this.labelSampleRate, gbc );

    this.labelSampleSize = new JLabel( "Aufl\u00F6sung:" );
    gbc.gridy++;
    add( this.labelSampleSize, gbc );

    this.labelVolume = new JLabel( "Pegel:" );
    gbc.gridy += 2;
    add( this.labelVolume, gbc );

    this.labelDuration = new JLabel( "Aufgenommene Zeit:" );
    this.labelDuration.setEnabled( false );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.labelDuration, gbc );


    // Mixer
    gbc.anchor        = GridBagConstraints.WEST;
    gbc.gridy         = 0;
    gbc.insets.bottom = 0;
    gbc.gridx++;
    add( this.comboMixer, gbc );


    // Abtastrate
    gbc.gridy++;
    add( this.comboSampleRate, gbc );


    // Aufloesung
    JPanel panelBits = new JPanel();
    panelBits.setLayout( new BoxLayout( panelBits, BoxLayout.X_AXIS ) );
    gbc.gridy++;
    add( panelBits, gbc );

    ButtonGroup grpBits = new ButtonGroup();

    this.btn8Bit = new JRadioButton( "8 Bit", false );
    this.btn8Bit.setAlignmentX( Component.LEFT_ALIGNMENT );
    grpBits.add( this.btn8Bit );
    panelBits.add( this.btn8Bit );
    panelBits.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );

    this.btn16Bit = new JRadioButton( "16 Bit", true );
    this.btn16Bit.setAlignmentX( Component.LEFT_ALIGNMENT );
    grpBits.add( this.btn16Bit );
    panelBits.add( this.btn16Bit );


    // Mono/Stereo
    JPanel panelChannels = new JPanel();
    panelChannels.setLayout(
		new BoxLayout( panelChannels, BoxLayout.X_AXIS ) );
    gbc.gridy++;
    add( panelChannels, gbc );

    ButtonGroup grpChannels = new ButtonGroup();

    this.btnMono = new JRadioButton( "Mono", false );
    this.btnMono.setAlignmentX( Component.LEFT_ALIGNMENT );
    grpChannels.add( this.btnMono );
    panelChannels.add( this.btnMono );
    panelChannels.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );

    this.btnStereo = new JRadioButton( "Stereo", true );
    this.btnStereo.setAlignmentX( Component.LEFT_ALIGNMENT );
    grpChannels.add( this.btnStereo );
    panelChannels.add( this.btnStereo );


    // Pegel
    this.volumeBar.setOrientation( SwingConstants.HORIZONTAL );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridy++;
    add( this.volumeBar, gbc );


    // Dauer
    this.labelDurationValue = new JLabel();
    this.labelDurationValue.setEnabled( false );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.labelDurationValue, gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 5, 1, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.NORTHEAST;
    gbc.insets.left = 10;
    gbc.gridheight  = 6;
    gbc.gridy       = 0;
    gbc.gridx++;
    add( panelBtn, gbc );

    this.btnStartStop = new JButton( TEXT_START );
    panelBtn.add( this.btnStartStop );

    this.btnPlay = new JButton( "Wiedergeben..." );
    this.btnPlay.setEnabled( false );
    panelBtn.add( this.btnPlay );

    this.btnSave = new JButton( "Speichern..." );
    this.btnSave.setEnabled( false );
    panelBtn.add( this.btnSave );

    this.btnHelp = new JButton( "Hilfe" );
    panelBtn.add( this.btnHelp );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    panelBtn.add( this.btnClose );


    // sonstiges
    pack();
    if( !applySettings( Main.getProperties(), false ) ) {
      setScreenCentered();
    }
    setResizable( false );
    updFieldsEnabled();


    // Listener
    this.btnStartStop.addActionListener( this );
    this.btnPlay.addActionListener( this );
    this.btnSave.addActionListener( this );
    this.btnHelp.addActionListener( this );
    this.btnClose.addActionListener( this );


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

  private void doStartStop()
  {
    if( this.recEnabled ) {
      this.recEnabled = false;
      this.btnStartStop.setText( TEXT_START );
    } else {
      if( this.audioThread == null ) {
	if( confirmDataSaved() ) {
	  this.mixer      = getSelectedMixer();
	  this.sampleRate = getSelectedSampleRate();
	  if( this.sampleRate <= 0 ) {
	    this.sampleRate = 44100;
	  }
	  this.sampleSizeInBits = (this.btn8Bit.isSelected() ? 8 : 16);
	  this.channels         = (this.btnMono.isSelected() ? 1 : 2);
	  if( this.dataBuf != null ) {
	    this.dataBuf.reset();
	  } else {
	    this.dataBuf = new DataBuf( 0x100000 );
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
	  setVolumeBarState( true );
	  this.timerDuration.start();
	  this.btnStartStop.setText( TEXT_STOP );
	}
      } else {
	this.btnStartStop.setText( TEXT_STOP );
      }
    }
  }


  private void doPlay()
  {
    if( (this.audioFmt != null)
	&& (this.dataBuf != null)
	&& (this.dataLen > 0) )
    {
      try {
	AudioPlayer.play(
		this,
		createAudioInputStream(),
		"Wiedergabe der Aufnahme..." );
      }
      catch( Exception ex ) {
	BasicDlg.showErrorDlg( this, ex );
      }
    }
  }


  private void doSave()
  {
    if( (this.audioFmt != null)
	&& (this.dataBuf != null)
	&& (this.dataLen > 0) )
    {
      File file = EmuUtil.showFileSaveDlg(
			this,
			"Sound-Datei speichern",
			Main.getLastDirFile( "audio" ),
			AudioUtil.getAudioOutFileFilter() );
      if( file != null ) {
	AudioFileFormat.Type type = AudioUtil.getAudioFileType( this, file );
	if( type != null ) {
	  try {
	    AudioUtil.write( createAudioInputStream(), type, file );
	    Main.setLastFile( file, "audio" );
	    this.dataSaved = true;
	  }
	  catch( Exception ex ) {
	    BasicDlg.showErrorDlg( this, ex );
	  }
	}
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
    boolean rv = false;
    if( !this.suppressAskDataSaved
	&& (this.audioFmt != null)
	&& (this.dataBuf != null)
	&& (this.dataLen > 0)
	&& !this.dataSaved )
    {
      String[] options = { "Speichern", "Verwerfen", "Abbrechen" };
      String   msg     = "Was soll mit der noch nicht"
				+ " gespeicherten Aufnahme geschehen?";
      JCheckBox btnSuppress = new JCheckBox(
			"Diesen Dialog zuk\u00FCnftig nicht mehr anzeigen" );
      JOptionPane pane = new JOptionPane(
				new Object[] { msg, btnSuppress },
				JOptionPane.WARNING_MESSAGE );
      pane.setWantsInput( false );
      pane.setOptions( options );
      pane.setInitialValue( options[ 0 ] );
      setState( Frame.NORMAL );
      toFront();
      pane.createDialog(
		this,
		"Aufnahme nicht gespeichert" ).setVisible( true );
      Object value = pane.getValue();
      if( value != null ) {
	if( value.equals( options[ 0 ] ) ) {
	  doSave();
	  rv                        = this.dataSaved;
	  this.suppressAskDataSaved = btnSuppress.isSelected();
	}
	else if( value.equals( options[ 1 ] ) ) {
	  this.suppressAskDataSaved = btnSuppress.isSelected();
	  this.dataSaved            = true;
	  rv                        = true;
	}
      }
    } else {
      rv = true;
    }
    return rv;
  }


  private AudioInputStream createAudioInputStream() throws IOException
  {
    return new AudioInputStream(
		new GZIPInputStream( this.dataBuf.newInputStream() ),
		this.audioFmt,
		this.dataLen );
  }


  private void recFinished( Exception ex )
  {
    setVolumeBarState( false );
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
	BasicDlg.showErrorDlg( this, ex );
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


  private void updDuration()
  {
    int seconds = -1;
    if( this.dataBuf != null ) {
      if( this.recording && (this.begMillis > 0) ) {
	seconds = (int) ((System.currentTimeMillis() - this.begMillis)
								/ 1000);
      } else {
	if( this.audioFmt != null ) {
	  seconds = this.dataLen / (int) this.audioFmt.getSampleRate();
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
      if( minutes >= 45 ) {
	this.recEnabled = false;
      }
    }
  }


  private void updFieldsEnabled()
  {
    boolean state = (this.audioThread == null);
    setMixerState( state );
    setSampleRateState( state );
    this.labelSampleSize.setEnabled( state );
    this.btn8Bit.setEnabled( state );
    this.btn16Bit.setEnabled( state );
    this.btnMono.setEnabled( state );
    this.btnStereo.setEnabled( state );

    state = !state;
    this.labelVolume.setEnabled( state );
    this.volumeBar.setEnabled( state );
  }
}

