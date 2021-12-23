/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Emulation des Lautsprechers
 * bzw. Sound-Generatorausgangs
 */

package jkcemu.audio;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.etc.CPUSynchronSoundDevice;


public class SoundFld extends AbstractAudioOutFld
{
  public static final String PROP_PREFIX = "jkcemu.audio.sound.";

  private static final String PROP_STEREO = "stereo";

  private AbstractSoundDevice soundDevice;
  private String              propPrefix;
  private boolean             notified;
  private boolean             wasRecording;
  private JLabel              labelChannels;
  private JRadioButton        rbMono;
  private JRadioButton        rbStereo;
  private JButton             btnRecord;
  private JButton             btnPause;
  private JButton             btnDelete;


  public SoundFld(
		AudioFrm            audioFrm,
		EmuThread           emuThread,
		int                 deviceNum,
		AbstractSoundDevice soundDevice )
  {
    super( audioFrm, emuThread );
    this.propPrefix   = getPropPrefix( deviceNum );
    this.soundDevice  = soundDevice;
    this.notified     = false;
    this.wasRecording = false;

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.NORTHWEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    // Bereich Optionen
    JPanel panelOpt = GUIFactory.createPanel( new GridBagLayout() );
    panelOpt.setBorder( GUIFactory.createTitledBorder( "Optionen" ) );
    add( panelOpt, gbc );

    GridBagConstraints gbcOpt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.labelMixer = GUIFactory.createLabel( "Ausgabeger\u00E4t:" );
    panelOpt.add( this.labelMixer, gbcOpt );

    this.labelFrameRate  = GUIFactory.createLabel( "Abtastrate (Hz):" );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.labelFrameRate, gbcOpt );

    if( soundDevice.supportsStereo() ) {
      this.labelChannels = GUIFactory.createLabel( "Ausgang:" );
      gbcOpt.gridy++;
      panelOpt.add( this.labelChannels, gbcOpt );
    } else {
      this.labelChannels = null;
    }

    this.comboMixer      = AudioUtil.createMixerComboBox( false );
    gbcOpt.insets.bottom = 0;
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridy         = 0;
    gbcOpt.gridx++;
    panelOpt.add( this.comboMixer, gbcOpt );

    this.comboFrameRate = AudioUtil.createFrameRateComboBox();
    this.comboFrameRate.setEditable( false );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.comboFrameRate, gbcOpt );

    if( soundDevice.supportsStereo() ) {
      ButtonGroup grpChannels = new ButtonGroup();

      this.rbMono = GUIFactory.createRadioButton( "Mono", true );
      grpChannels.add( this.rbMono );
      gbcOpt.insets.bottom = 5;
      gbcOpt.gridwidth     = 1;
      gbcOpt.gridy++;
      panelOpt.add( this.rbMono, gbcOpt );

      this.rbStereo = GUIFactory.createRadioButton( "Stereo" );
      grpChannels.add( this.rbStereo );
      gbcOpt.gridx++;
      panelOpt.add( this.rbStereo, gbcOpt );

      this.rbStereo.setSelected( true );
    } else {
      this.rbMono   = null;
      this.rbStereo = null;
    }


    // Bereich Status
    JPanel panelStatus = GUIFactory.createPanel( new GridBagLayout() );
    panelStatus.setBorder( GUIFactory.createTitledBorder( "Status" ) );
    gbc.gridy++;
    add( panelStatus, gbc );

    GridBagConstraints gbcStatus = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.labelFormat = GUIFactory.createLabel( "Format:" );
    panelStatus.add( this.labelFormat, gbcStatus );

    this.fldFormat = GUIFactory.createTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.fill    = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx = 1.0;
    gbcStatus.gridx++;
    panelStatus.add( this.fldFormat, gbcStatus );


    // Bereich Recorder
    JPanel panelRec = GUIFactory.createPanel( new GridBagLayout() );
    panelRec.setBorder( GUIFactory.createTitledBorder( "Recorder" ) );
    gbc.gridy++;
    add( panelRec, gbc );

    GridBagConstraints gbcRec = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.labelDuration = GUIFactory.createLabel( "Aufnahmedauer:" );
    gbcRec.gridy++;
    panelRec.add( this.labelDuration, gbcRec );

    this.fldDuration = GUIFactory.createTextField();
    this.fldDuration.setEditable( false );
    gbcRec.fill          = GridBagConstraints.HORIZONTAL;
    gbcRec.insets.bottom = 5;
    gbcRec.weightx = 1.0;
    gbcRec.gridy++;
    panelRec.add( this.fldDuration, gbcRec );

    JPanel panelRecBtn = GUIFactory.createPanel(
					new GridLayout( 1, 5, 5, 5 ) );
    gbcRec.anchor      = GridBagConstraints.CENTER;
    gbcRec.fill        = GridBagConstraints.NONE;
    gbcRec.weightx     = 0.0;
    gbcRec.gridy++;
    panelRec.add( panelRecBtn, gbcRec );

    this.btnRecord = GUIFactory.createRelImageResourceButton(
					this,
					"audio/record.png",
					EmuUtil.TEXT_RECORD );
    panelRecBtn.add( this.btnRecord );

    this.btnPause = GUIFactory.createRelImageResourceButton(
					this,
					"audio/pause.png",
					"Pause" );
    panelRecBtn.add( this.btnPause );

    this.btnPlay = GUIFactory.createRelImageResourceButton(
					this,
					"audio/play.png",
					"Wiedergeben" );
    panelRecBtn.add( this.btnPlay );

    this.btnSave = GUIFactory.createRelImageResourceButton(
					this,
					"file/save_as.png",
					EmuUtil.TEXT_OPEN_SAVE );
    panelRecBtn.add( this.btnSave );

    this.btnDelete = GUIFactory.createRelImageResourceButton(
					this,
					"audio/delete.png",
					EmuUtil.TEXT_DELETE );
    panelRecBtn.add( this.btnDelete );


    // rechte Seite
    JPanel panelEast = GUIFactory.createPanel( new GridBagLayout() );
    gbc.fill         = GridBagConstraints.VERTICAL;
    gbc.gridy        = 0;
    gbc.gridheight   = 3;
    gbc.gridx++;
    add( panelEast, gbc );

    GridBagConstraints gbcEast = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTH,
						GridBagConstraints.NONE,
						new Insets( 5, 0, 0, 0 ),
						0, 0 );

    JPanel panelBtn = GUIFactory.createPanel(
					new GridLayout( 2, 1, 5, 5 ) );
    panelEast.add( panelBtn, gbcEast );

    this.btnEnable = GUIFactory.createButton( "Aktivieren" );
    panelBtn.add( this.btnEnable );

    this.btnDisable = GUIFactory.createButton( "Deaktivieren" );
    panelBtn.add( this.btnDisable );


    // Pegelanzeige
    this.volumeBar = new VolumeBar( SwingConstants.VERTICAL );
    this.volumeBar.setBorder( GUIFactory.createTitledBorder( "Pegel" ) );
    this.volumeBar.setPreferredSize( new Dimension( 1, 1 ) );
    gbcEast.insets.top = 20;
    gbcEast.fill       = GridBagConstraints.BOTH;
    gbcEast.weightx    = 1.0;
    gbcEast.weighty    = 1.0;
    gbcEast.gridy++;
    panelEast.add( this.volumeBar, gbcEast );


    // Schaltflaechen aktivieren/deaktivieren
    updFieldsEnabled();
  }


  public boolean checkEnableAudio( Properties props )
  {
    boolean rv     = false;
    String  prefix = getSettingsPrefix();
    if( EmuUtil.getBooleanProperty(
				props,
				prefix + PROP_ENABLED,
				false ) )
    {
      try {
	setSelectedFrameRate( getFrameRate( props, prefix ) );
	setSelectedMixerByName( getMixerName( props, prefix ) );
	if( (this.rbMono != null) && (this.rbStereo != null) ) {
	  if( EmuUtil.getBooleanProperty(
				props,
				prefix + PROP_STEREO,
				true ) )
	  {
	    this.rbStereo.setSelected( true );
	  } else {
	    this.rbMono.setSelected( true );
	  }
	}
	rv = enableAudio();
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    }
    return rv;
  }


  public static String getPropPrefix( int deviceNum )
  {
    return String.format( "%s%d.", PROP_PREFIX, deviceNum );
  }


  public AbstractSoundDevice getSoundDevice()
  {
    return this.soundDevice;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.btnEnable.addActionListener( this );
      this.btnDisable.addActionListener( this );
      this.btnRecord.addActionListener( this );
      this.btnPause.addActionListener( this );
      this.btnPlay.addActionListener( this );
      this.btnSave.addActionListener( this );
      this.btnDelete.addActionListener( this );
    }
  }


  @Override
  protected void audioFinished( AudioIO audioIO, String errorMsg )
  {
    super.audioFinished( audioIO, errorMsg );
    if( this.soundDevice.getAudioOut() == audioIO ) {
      this.soundDevice.setAudioOut( null );
    }
  }


  @Override
  protected boolean doAction( ActionEvent e )
  {
    boolean rv  = true;
    Object  src = e.getSource();
    if( src == this.btnRecord ) {
      setRecording( true );
    } else if( src == this.btnPause ) {
      setRecording( false );
    } else if( src == this.btnDelete ) {
      doDelete();
    } else {
      rv = super.doAction( e );
    }
    return rv;
  }


  @Override
  protected void doEnable( boolean interactive )
  {
    try {
      if( this.soundDevice instanceof CPUSynchronSoundDevice ) {
	this.audioFrm.checkOpenCPUSynchronLine();
      }
      enableAudio();
    }
    catch( IOException ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
  }


  @Override
  protected void formatChanged( AudioIO audioIO, String formatText )
  {
    if( (formatText != null) || (this.recordedData == null) )
      this.fldFormat.setText( formatText != null ? formatText : "" );
  }


  @Override
  protected String getSettingsPrefix()
  {
    return this.propPrefix;
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    super.putSettingsTo( props );
    if( props != null ) {
      String prefix = getSettingsPrefix();
      EmuUtil.setProperty(
		props,
		prefix + PROP_ENABLED,
		this.soundDevice.getAudioOut() != null );
      if( this.rbStereo != null ) {
	EmuUtil.setProperty(
		props,
		prefix + PROP_STEREO,
		this.rbStereo.isSelected() );
      }
    }
  }


  @Override
  public void removeNotify()
  {
    if( this.notified ) {
      this.notified = false;
      this.btnEnable.removeActionListener( this );
      this.btnDisable.removeActionListener( this );
      this.btnRecord.removeActionListener( this );
      this.btnPause.removeActionListener( this );
      this.btnPlay.removeActionListener( this );
      this.btnSave.removeActionListener( this );
      this.btnDelete.removeActionListener( this );
    }
    super.removeNotify();
  }


  @Override
  public void updFieldsEnabled()
  {
    boolean  supportsStereo = false;
    boolean  running        = false;
    boolean  recording      = false;
    boolean  recorded       = (this.recordedData != null);
    AudioOut audioOut       = this.audioOut;
    if( audioOut != null ) {
      running   = true;
      recording = audioOut.isRecording();
    } else {
      supportsStereo = this.soundDevice.supportsStereo();
    }
    this.btnEnable.setEnabled( !running);
    this.btnDisable.setEnabled( running);
    this.btnRecord.setEnabled( !recording && running );
    this.btnPause.setEnabled( recording );
    this.btnPlay.setEnabled( recorded && !running );
    this.btnSave.setEnabled( recorded && !running );
    this.btnDelete.setEnabled( recorded && !running );
    this.labelMixer.setEnabled( !running);
    this.comboMixer.setEnabled( !running);
    this.labelFrameRate.setEnabled( !running );
    this.comboFrameRate.setEnabled( !running );
    if( this.labelChannels != null ) {
      this.labelChannels.setEnabled( supportsStereo && !running );
    }
    if( this.rbMono != null ) {
      this.rbMono.setEnabled( supportsStereo && !running );
    }
    if( this.rbStereo != null ) {
      this.rbStereo.setEnabled( supportsStereo && !running );
    }
    if( !running && !recorded ) {
      this.fldFormat.setText( "" );
      this.fldDuration.setText( "" );
    }
    this.labelFormat.setEnabled( running);
    this.fldFormat.setEnabled( running);
    this.labelDuration.setEnabled( recorded || running );
    this.fldDuration.setEnabled( recorded || running );
    this.volumeBar.setVolumeBarState( running );
  }


	/* --- private Methoden --- */

  private void doDelete()
  {
    if( this.recordedData != null ) {
      if( BaseDlg.showYesNoDlg( this, "Aufnahme verwerfen?" ) ) {
	this.recordedData = null;
	if( this.audioOut == null ) {
	  this.fldFormat.setText( "" );
	}
	updFieldsEnabled();
      }
    }
  }


  private boolean enableAudio() throws IOException
  {
    boolean rv       = false;
    int     speedKHz = this.audioFrm.getAndCheckSpeed();
    if( (speedKHz > 0) && checkRecordedDataSaved() ) {
      boolean stereo = false;
      if( this.rbStereo != null ) {
	stereo = this.rbStereo.isSelected();
      }
      AudioOut audioOut = new AudioOut(
				this,
				this.emuThread.getZ80CPU(),
				speedKHz,
				getSelectedFrameRate(),
				true,
				this.soundDevice.isSingleBitMode(),
				stereo,
				getSelectedMixerInfo(),
				false );
      this.fldDuration.setText( audioOut.getDurationText() );
      this.audioOut = audioOut;
      this.soundDevice.setAudioOut( audioOut );
      this.durationTimer.start();
      updFieldsEnabled();
      rv = true;
    }
    return rv;
  }


  private void setRecording( boolean state )
  {
    AudioOut audioOut = this.audioOut;
    if( audioOut != null ) {
      audioOut.setRecording( state );
      this.btnRecord.setEnabled( !state );
      this.btnPause.setEnabled( state );
    }
  }
}
