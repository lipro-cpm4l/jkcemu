/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Audio-Ausgang
 * Emulation des Anschlusses des Magnettonbandgeraetes
 */

package jkcemu.audio;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.Main;
import jkcemu.base.*;
import z80emu.*;


public class AudioFrm extends BasicFrm implements Z80MaxSpeedListener
{
  private static final int[] sampleRates = {
				96000, 48000, 44100, 32000,
				22050, 16000, 11025, 8000 };

  private ScreenFrm                          screenFrm;
  private EmuThread                          emuThread;
  private Z80CPU                             z80cpu;
  private javax.swing.filechooser.FileFilter readFileFilter;
  private javax.swing.filechooser.FileFilter writeFileFilter;
  private File                               curFile;
  private File                               lastFile;
  private AudioFormat                        audioFmt;
  private AudioIO                            audioIO;
  private JRadioButton                       btnSoundOut;
  private JRadioButton                       btnDataOut;
  private JRadioButton                       btnDataIn;
  private JRadioButton                       btnFileOut;
  private JRadioButton                       btnFileIn;
  private JRadioButton                       btnFileLastIn;
  private JLabel                             labelSampleRate;
  private JComboBox                          comboSampleRate;
  private JLabel                             labelChannel;
  private JRadioButton                       btnChannel0;
  private JRadioButton                       btnChannel1;
  private JCheckBox                          btnMonitorPlay;
  private JLabel                             labelFileName;
  private JTextField                         fldFileName;
  private JLabel                             labelFormat;
  private JTextField                         fldFormat;
  private JLabel                             labelProgress;
  private JProgressBar                       progressBar;
  private JButton                            btnEnable;
  private JButton                            btnDisable;
  private JButton                            btnHelp;
  private JButton                            btnClose;


  public AudioFrm( ScreenFrm screenFrm )
  {
    this.screenFrm       = screenFrm;
    this.emuThread       = screenFrm.getEmuThread();
    this.z80cpu          = this.emuThread.getZ80CPU();
    this.readFileFilter  = null;
    this.writeFileFilter = null;
    this.curFile         = null;
    this.lastFile        = null;
    this.audioFmt        = null;
    this.audioIO         = null;

    setTitle( "JKCEMU Audio/Kassette" );
    Main.updIcon( this );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.HORIZONTAL,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );


    // Bereich Funktion
    JPanel panelFct = new JPanel( new GridBagLayout() );
    panelFct.setBorder( BorderFactory.createTitledBorder( "Funktion" ) );

    GridBagConstraints gbcFct = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpFct = new ButtonGroup();

    this.btnSoundOut = new JRadioButton(
			"T\u00F6ne ausgeben (Emulation des Lautsprechers)",
			true );
    grpFct.add( this.btnSoundOut );
    this.btnSoundOut.addActionListener( this );
    panelFct.add( this.btnSoundOut, gbcFct );

    this.btnDataOut = new JRadioButton( "Daten am Audio-Ausgang ausgeben" );
    grpFct.add( this.btnDataOut );
    this.btnDataOut.addActionListener( this );
    gbcFct.insets.top = 0;
    gbcFct.gridy++;
    panelFct.add( this.btnDataOut, gbcFct );

    this.btnDataIn = new JRadioButton( "Daten vom Audio-Eingang lesen" );
    grpFct.add( this.btnDataIn );
    this.btnDataIn.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnDataIn, gbcFct );

    this.btnFileOut = new JRadioButton( "Sound-Datei speichern" );
    grpFct.add( this.btnFileOut );
    this.btnFileOut.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnFileOut, gbcFct );

    this.btnFileIn = new JRadioButton( "Sound-Datei lesen" );
    grpFct.add( this.btnFileIn );
    this.btnFileIn.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnFileIn, gbcFct );

    this.btnFileLastIn = new JRadioButton(
				"Letzte Sound-Datei (noch einmal) lesen" );
    grpFct.add( this.btnFileLastIn );
    this.btnFileLastIn.addActionListener( this );
    gbcFct.insets.bottom = 5;
    gbcFct.gridy++;
    panelFct.add( this.btnFileLastIn, gbcFct );

    add( panelFct, gbc );


    // Bereich Optionen
    JPanel panelOpt = new JPanel( new GridBagLayout() );
    panelOpt.setBorder( BorderFactory.createTitledBorder( "Optionen" ) );

    GridBagConstraints gbcOpt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 2, 5 ),
						0, 0 );

    this.labelSampleRate = new JLabel( "Abtastrate (Hz):" );
    panelOpt.add( this.labelSampleRate, gbcOpt );

    this.comboSampleRate = new JComboBox();
    this.comboSampleRate.setEditable( false );
    this.comboSampleRate.addItem( "Standard" );
    for( int i = 0; i < this.sampleRates.length; i++ ) {
      this.comboSampleRate.addItem( String.valueOf( this.sampleRates[ i ] ) );
    }
    gbcOpt.gridwidth = GridBagConstraints.REMAINDER;
    gbcOpt.gridx++;
    panelOpt.add( this.comboSampleRate, gbcOpt );

    this.labelChannel = new JLabel( "Aktiver Kanal:" );
    gbcOpt.insets.top = 2;
    gbcOpt.gridwidth  = 1;
    gbcOpt.gridx      = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelChannel, gbcOpt );

    ButtonGroup grpChannel = new ButtonGroup();

    this.btnChannel0 = new JRadioButton( "Links", true );
    grpChannel.add( this.btnChannel0 );
    this.btnChannel0.addActionListener( this );
    gbcOpt.gridx++;
    panelOpt.add( this.btnChannel0, gbcOpt );

    this.btnChannel1 = new JRadioButton( "Rechts", false );
    grpChannel.add( this.btnChannel1 );
    this.btnChannel1.addActionListener( this );
    gbcOpt.gridx++;
    panelOpt.add( this.btnChannel1, gbcOpt );

    this.btnMonitorPlay  = new JCheckBox( "Mith\u00F6ren", false );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridx         = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.btnMonitorPlay, gbcOpt );

    gbc.gridy++;
    add( panelOpt, gbc );


    // Bereich Status
    JPanel panelStatus = new JPanel( new GridBagLayout() );
    panelStatus.setBorder( BorderFactory.createTitledBorder( "Status" ) );

    GridBagConstraints gbcStatus = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.labelFileName = new JLabel( "Datei:" );
    panelStatus.add( this.labelFileName, gbcStatus );

    this.labelFormat = new JLabel( "Format:" );
    gbcStatus.gridy++;
    panelStatus.add( this.labelFormat, gbcStatus );

    this.labelProgress = new JLabel( "Fortschritt:" );
    gbcStatus.gridy++;
    panelStatus.add( this.labelProgress, gbcStatus );

    this.fldFileName = new JTextField();
    this.fldFileName.setEditable( false );
    gbcStatus.anchor  = GridBagConstraints.WEST;
    gbcStatus.fill    = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx = 1.0;
    gbcStatus.gridy   = 0;
    gbcStatus.gridx++;
    panelStatus.add( this.fldFileName, gbcStatus );

    this.fldFormat = new JTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.gridy++;
    panelStatus.add( this.fldFormat, gbcStatus );

    this.progressBar = new JProgressBar( JProgressBar.HORIZONTAL, 0, 100 );
    this.progressBar.setBorderPainted( true );
    this.progressBar.setStringPainted( false );
    this.progressBar.setValue( 0 );
    gbcStatus.gridy++;
    panelStatus.add( this.progressBar, gbcStatus );

    gbc.gridy++;
    add( panelStatus, gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 4, 1, 5, 5 ) );

    this.btnEnable = new JButton( "Aktivieren" );
    this.btnEnable.addActionListener( this );
    panelBtn.add( this.btnEnable );

    this.btnDisable = new JButton( "Deaktivieren" );
    this.btnDisable.addActionListener( this );
    panelBtn.add( this.btnDisable );

    this.btnHelp = new JButton( "Hilfe" );
    this.btnHelp.addActionListener( this );
    panelBtn.add( this.btnHelp );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    panelBtn.add( this.btnClose );

    gbc.fill       = GridBagConstraints.NONE;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.gridy      = 0;
    gbc.gridx++;
    add( panelBtn, gbc );


    // sonstiges
    pack();
    if( !applySettings( Main.getProperties(), false ) ) {
      setScreenCentered();
    }
    setResizable( false );
    setAudioState( false );
    this.z80cpu.addMaxSpeedListener( this );
  }


  public void fireDisable()
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    doDisable();
		  }
		} );
  }


  public void fireProgressUpdate( final double value )
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    updProgressBar( value );
		  }
		} );
  }


  public void doQuit()
  {
    doDisable();
    doClose();
  }


	/* --- Methoden fuer Z80MaxSpeedListener --- */

  /*
   * Wenn sich die Emulationsgeschwindigkeit aendert,
   * stimmt die Synchronisation mit dem Audio-System nicht mehr.
   * Aus diesem Grund die Audiokanaele schliessen und
   * bei Ein-/Ausgabe ueber das Sound-System wieder oeffnen.
   */
  public void z80MaxSpeedChanged()
  {
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    maxSpeedChanged();
		  }
		} );
  }


	/* --- ueberschriebene Methoden --- */

  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();

      if( (src == this.btnSoundOut)
	  || (src == this.btnDataOut)
	  || (src == this.btnDataIn)
	  || (src == this.btnFileOut)
	  || (src == this.btnFileIn)
	  || (src == this.btnFileLastIn) )
      {
	rv = true;
	updOptFields();
      }
      else if( (src == this.btnChannel0) || (src == this.btnChannel1) ) {
	rv = true;
	updChannel();
      }
      else if( src == this.btnEnable ) {
	rv = true;
	doEnable();
      }
      else if( src == this.btnDisable ) {
	rv = true;
	doDisable();
      }
      else if( src == this.btnHelp ) {
	rv = true;
	this.screenFrm.showHelp( "/help/audio.htm" );
      }
      else if( src == this.btnClose ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static javax.swing.filechooser.FileFilter createFileFilter(
						String                  text,
						AudioFileFormat.Type... fmts )
  {
    javax.swing.filechooser.FileFilter rv = null;
    if( fmts != null ) {
      if( fmts.length > 0 ) {
	Collection<String> suffixes = new ArrayList<String>( fmts.length );
	for( int i = 0; i < fmts.length; i++ ) {
	  String suffix = fmts[ i ].getExtension();
	  if( suffix != null ) {
	    if( suffix.length() > 0 )
	      suffixes.add( suffix );
	  }
	}
	if( !suffixes.isEmpty() ) {
	  try {
	    rv = new FileNameExtensionFilter(
			text,
			suffixes.toArray( new String[ suffixes.size() ] ) );
	  }
	  catch( ArrayStoreException ex ) {}
	}
      }
    }
    return rv;
  }


  private void doEnable()
  {
    int speedKHz = this.z80cpu.getMaxSpeedKHz();
    if( speedKHz > 0 ) {
      if( this.btnSoundOut.isSelected() || this.btnDataOut.isSelected() ) {
	doEnableAudioOutLine( speedKHz, this.btnDataOut.isSelected() );
      }
      else if( this.btnDataIn.isSelected() ) {
	doEnableAudioInLine( speedKHz );
      }
      else if( this.btnFileOut.isSelected() ) {
	doEnableAudioOutFile( speedKHz );
      }
      else if( this.btnFileIn.isSelected() ) {
	doEnableAudioInFile( speedKHz, null );
      }
      else if( this.btnFileLastIn.isSelected() ) {
	doEnableAudioInFile( speedKHz, this.lastFile );
      }
    } else {
      BasicDlg.showErrorDlg(
	this,
	"Sie m\u00Fcssen die Geschwindigkeit des Emulators\n"
		+ "auf einen konkreten Wert begrenzen, da dieser\n"
		+ "als Zeitbasis f\u00FCr das AudioSystem dient.\n" );
    }
  }


  private void doEnableAudioInFile( int speedKHz, File file )
  {
    if( file == null ) {
      if( this.readFileFilter == null ) {
	this.readFileFilter = createFileFilter(
					"Audiodateien",
					AudioFileFormat.Type.AIFC,
					AudioFileFormat.Type.AIFF,
					AudioFileFormat.Type.AU,
					AudioFileFormat.Type.SND,
					AudioFileFormat.Type.WAVE );
      }
      file = EmuUtil.showFileOpenDlg(
				this,
				"Sound-Datei \u00F6ffnen",
				Main.getLastPathFile( "audio" ),
				this.readFileFilter );
    }
    if( file != null ) {
      stopAudio();
      boolean monitorPlay = this.btnMonitorPlay.isSelected();
      AudioIn audioIn     = new AudioInFile(
					this.z80cpu,
					this,
					file,
					monitorPlay );
      this.audioFmt = audioIn.startAudio( speedKHz, getSampleRate() );
      this.audioIO  = audioIn;
      if( this.audioFmt != null ) {
	updChannel();
	this.emuThread.setAudioIn( audioIn );
	this.curFile  = file;
	this.lastFile = file;
	Main.setLastFile( file, "audio" );
	setAudioState( true );
	if( audioIn.isMonitorPlayActive() != monitorPlay )
	  showErrorNoMonitorPlay();
      } else {
	showError( this.audioIO.getErrorText() );
      }
    }
  }


  private void doEnableAudioInLine( int speedKHz )
  {
    stopAudio();
    AudioIn audioIn = new AudioInLine( this.z80cpu );
    this.audioFmt   = audioIn.startAudio( speedKHz, getSampleRate() );
    this.audioIO    = audioIn;
    if( this.audioFmt != null ) {
      updChannel();
      this.emuThread.setAudioIn( audioIn );
      setAudioState( true );
    } else {
      showError( this.audioIO.getErrorText() );
    }
  }


  private void doEnableAudioOutFile( int speedKHz )
  {
    if( this.writeFileFilter == null ) {
      this.writeFileFilter = createFileFilter(
					"Unterst\u00FCtzte Audiodateien",
					AudioSystem.getAudioFileTypes() );
    }
    File file = EmuUtil.showFileSaveDlg(
				this,
				"Sound-Datei speichern",
				Main.getLastPathFile( "audio" ),
				this.writeFileFilter );
    if( file != null ) {
      AudioFileFormat.Type fileType = getAudioFileType( file );
      if( fileType != null ) {
	stopAudio();
	boolean  monitorPlay = this.btnMonitorPlay.isSelected();
	AudioOut audioOut    = new AudioOutFile(
					this.z80cpu,
					this,
					file,
					fileType,
					monitorPlay );
	this.audioFmt = audioOut.startAudio( speedKHz, getSampleRate() );
	this.audioIO  = audioOut;
	if( this.audioFmt != null ) {
	  this.emuThread.setAudioOut( audioOut );
	  this.curFile  = file;
	  this.lastFile = file;
	  Main.setLastFile( file, "audio" );
	  setAudioState( true );
	  if( audioOut.isMonitorPlayActive() != monitorPlay )
	    showErrorNoMonitorPlay();
	} else {
	  showError( this.audioIO.getErrorText() );
	}
      }
    }
  }


  private void doEnableAudioOutLine( int speedKHz, boolean forDataTransfer )
  {
    stopAudio();
    AudioOut audioOut = new AudioOutLine(
				this.z80cpu,
				this.btnSoundOut.isSelected() );
    this.audioFmt = audioOut.startAudio( speedKHz, getSampleRate() );
    this.audioIO  = audioOut;
    if( this.audioFmt != null ) {
      this.emuThread.setAudioOut( audioOut );
      setAudioState( true );
    } else {
      showError( this.audioIO.getErrorText() );
    }
  }


  private void doDisable()
  {
    stopAudio();
    this.curFile  = null;
    this.audioFmt = null;
    setAudioState( false );
  }


  private AudioFileFormat.Type getAudioFileType( File file )
  {
    Collection<AudioFileFormat.Type> types =
				new ArrayList<AudioFileFormat.Type>();
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.AIFC ) ) {
      types.add( AudioFileFormat.Type.AIFC );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.AIFF ) ) {
      types.add( AudioFileFormat.Type.AIFF );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.AU ) ) {
      types.add( AudioFileFormat.Type.AU );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.SND ) ) {
      types.add( AudioFileFormat.Type.SND );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.WAVE ) ) {
      types.add( AudioFileFormat.Type.WAVE );
    }

    String fileName = file.getName();
    if( fileName != null ) {
      fileName = fileName.toUpperCase( Locale.ENGLISH );
      for( AudioFileFormat.Type fileType : types ) {
	String ext = fileType.getExtension();
	if( ext != null ) {
	  ext = ext.toUpperCase();
	  if( !ext.startsWith( "." ) ) {
	    ext = "." + ext;
	  }
	  if( fileName.endsWith( ext ) )
	    return fileType;
	}
      }
    }

    StringBuilder buf = new StringBuilder( 64 );
    buf.append( "Das Dateiformat wird nicht unterst\u00FCtzt." );
    if( !types.isEmpty() ) {
      buf.append( "\nM\u00F6gliche Dateiendungen sind:\n" );
      String delim = null;
      for( AudioFileFormat.Type fileType : types ) {
	String ext = fileType.getExtension();
	if( ext != null ) {
	  if( delim != null ) {
	    buf.append( delim );
	  }
	  buf.append( ext );
	  delim = ", ";
	}
      }
    }
    BasicDlg.showErrorDlg( this, buf.toString() );
    return null;
  }


  private int getSampleRate()
  {
    int i = this.comboSampleRate.getSelectedIndex() - 1;  // 0: automatisch
    return ((i >= 0) && (i < this.sampleRates.length)) ?
					this.sampleRates[ i ] : 0;
  }


  private void maxSpeedChanged()
  {
    boolean state = (this.audioFmt != null);
    doDisable();
    if( state ) {
      int speedKHz = this.z80cpu.getMaxSpeedKHz();
      if( speedKHz > 0 ) {
	if( this.btnSoundOut.isSelected() || this.btnDataOut.isSelected() ) {
	  doEnableAudioOutLine( speedKHz, this.btnDataOut.isSelected() );
	}
	else if( this.btnDataIn.isSelected() ) {
	  doEnableAudioInLine( speedKHz );
	}
      }
    }
  }


  private void setAudioState( boolean state )
  {
    if( state && (this.audioFmt != null) ) {
      this.labelFormat.setEnabled( true );
      this.fldFormat.setText( AudioUtil.getAudioFormatText( this.audioFmt ) );
    } else {
      this.labelFormat.setEnabled( false );
      this.fldFormat.setText( "" );
    }

    if( state && (this.curFile != null) ) {
      this.labelFileName.setEnabled( true );
      this.fldFileName.setText( this.curFile.getPath() );
    } else {
      this.labelFileName.setEnabled( false );
      this.fldFileName.setText( "" );
    }

    boolean progressState = false;
    if( state ) {
      AudioIO audioIO = this.audioIO;
      if( audioIO != null )
	progressState = audioIO.isProgressUpdateEnabled();
    }
    if( progressState ) {
      this.labelProgress.setEnabled( true );
      this.progressBar.setEnabled( true );
    } else {
      this.progressBar.setValue( this.progressBar.getMinimum() );
      this.progressBar.setEnabled( false );
      this.labelProgress.setEnabled( false );
    }

    this.btnDisable.setEnabled( state );
    state = !state;
    this.btnEnable.setEnabled( state );
    this.btnSoundOut.setEnabled( state );
    this.btnDataOut.setEnabled( state );
    this.btnDataIn.setEnabled( state );
    this.btnFileOut.setEnabled( state );
    this.btnFileIn.setEnabled( state );
    this.btnFileLastIn.setEnabled( state && (this.lastFile != null) );
    updOptFields();
  }


  private void showError( String errorText )
  {
    if( errorText == null ) {
      if( this.comboSampleRate.getSelectedIndex() > 0 ) {
	errorText = "Es kann kein Audiokanal mit den angegebenen"
				+ " Optionen ge\u00F6ffnet werden.";
      } else {
	errorText = "Es kann kein Audiokanal ge\u00F6ffnet werden.";
      }
    }
    BasicDlg.showErrorDlg( this, errorText );
  }


  private void showErrorNoMonitorPlay()
  {
    BasicDlg.showErrorDlg(
	this,
	"Das Mith\u00F6ren ist nicht m\u00F6glich,\n"
		+ "da das \u00D6ffnen eines Audiokanals mit dem Format\n"
		+ "der Sound-Datei fehlgeschlagen ist." );
  }


  private void stopAudio()
  {
    AudioIO audioIO = this.audioIO;
    this.audioIO    = null;
    this.emuThread.setAudioIn( null );
    this.emuThread.setAudioOut( null );
    if( audioIO != null ) {
      audioIO.stopAudio();
      String errorText = audioIO.getErrorText();
      if( errorText != null )
	BasicDlg.showErrorDlg( this, errorText );
    }
  }


  private void updChannel()
  {
    AudioIO     audioIO  = this.audioIO;
    AudioFormat audioFmt = this.audioFmt;
    if( (audioIO != null) && (audioFmt != null) ) {
      if( audioIO instanceof AudioIn ) {
	int channel = 0;
	if( this.btnChannel1.isSelected() ) {
	  channel = 1;
	}
	if( channel >= audioFmt.getChannels() ) {
	  channel = 0;
	  this.btnChannel0.setSelected( true );
	}
	((AudioIn) audioIO).setSelectedChannel( channel );
      }
    }
  }


  private void updOptFields()
  {
    AudioIO     audioIO  = this.audioIO;
    AudioFormat audioFmt = this.audioFmt;

    // Sample-Rate
    boolean state = ((audioIO == null)
	&& (this.btnSoundOut.isSelected()
	    || this.btnDataOut.isSelected()
	    || this.btnDataIn.isSelected()
	    || this.btnFileOut.isSelected()));

    this.labelSampleRate.setEnabled( state );
    this.comboSampleRate.setEnabled( state );

    // Kanalauswahl
    state = false;
    if( (audioIO != null) && (audioFmt != null) ) {
      if( audioIO instanceof AudioIn ) {
	if( audioFmt.getChannels() > 1 )
	  state = true;
      }
    } else {
      state = (this.btnDataIn.isSelected()
	       || this.btnFileIn.isSelected()
	       || this.btnFileLastIn.isSelected());
    }
    this.labelChannel.setEnabled( state );
    this.btnChannel0.setEnabled( state );
    this.btnChannel1.setEnabled( state );

    // Mithoeren
    state = ((audioIO == null)
        && (this.btnFileOut.isSelected()
	    || this.btnFileIn.isSelected()
	    || this.btnFileLastIn.isSelected()));
    this.btnMonitorPlay.setEnabled( state );
  }


  private void updProgressBar( double value )
  {
    int intVal = (int) Math.round(
	value * (double) this.progressBar.getMaximum() ) + 1;

    if( intVal < this.progressBar.getMinimum() ) {
      intVal = this.progressBar.getMinimum();
    }
    else if( intVal > this.progressBar.getMaximum() ) {
      intVal = this.progressBar.getMaximum();
    }
    this.progressBar.setValue( intVal );
  }
}

