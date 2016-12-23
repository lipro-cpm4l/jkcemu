/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Emulation des Lautsprechers
 * bzw. Sound-Generatorausgangs
 */

package jkcemu.audio;

import java.awt.EventQueue;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.*;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;


public class SoundOutComponent extends AbstractAudioOutComponent
{
  private boolean      wasRecording;
  private JLabel       labelChannels;
  private JRadioButton btnMono;
  private JRadioButton btnStereo;
  private JButton      btnRecord;
  private JButton      btnPause;
  private JButton      btnDelete;


  public SoundOutComponent( AudioFrm audioFrm, EmuThread emuThread )
  {
    super( audioFrm, emuThread );
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
    JPanel panelOpt = new JPanel( new GridBagLayout() );
    panelOpt.setBorder( BorderFactory.createTitledBorder( "Optionen" ) );
    add( panelOpt, gbc );

    GridBagConstraints gbcOpt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.labelMixer = new JLabel( "Ger\u00E4t:" );
    panelOpt.add( this.labelMixer, gbcOpt );

    this.labelFrameRate = new JLabel( "Abtastrate (Hz):" );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.labelFrameRate, gbcOpt );

    this.labelChannels   = new JLabel( "Ausgang:" );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.labelChannels, gbcOpt );

    this.comboMixer      = this.audioFrm.createMixerComboBox();
    gbcOpt.insets.bottom = 0;
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridy         = 0;
    gbcOpt.gridx++;
    panelOpt.add( this.comboMixer, gbcOpt );

    this.comboFrameRate = this.audioFrm.createFrameRateComboBox();
    this.comboFrameRate.setEditable( false );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.comboFrameRate, gbcOpt );

    ButtonGroup grpChannels = new ButtonGroup();

    this.btnMono = new JRadioButton( "Mono" );
    grpChannels.add( this.btnMono );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridwidth     = 1;
    gbcOpt.gridy++;
    panelOpt.add( this.btnMono, gbcOpt );

    this.btnStereo = new JRadioButton( "Stereo", true );
    grpChannels.add( this.btnStereo );
    gbcOpt.gridx++;
    panelOpt.add( this.btnStereo, gbcOpt );


    // Bereich Status
    JPanel panelStatus = new JPanel( new GridBagLayout() );
    panelStatus.setBorder( BorderFactory.createTitledBorder( "Status" ) );
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

    this.labelFormat = new JLabel( "Format:" );
    panelStatus.add( this.labelFormat, gbcStatus );

    this.fldFormat = new JTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.fill    = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx = 1.0;
    gbcStatus.gridx++;
    panelStatus.add( this.fldFormat, gbcStatus );


    // Bereich Recorder
    JPanel panelRec = new JPanel( new GridBagLayout() );
    panelRec.setBorder( BorderFactory.createTitledBorder( "Recorder" ) );
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

    this.labelDuration = new JLabel( "Aufnahmedauer:" );
    gbcRec.gridy++;
    panelRec.add( this.labelDuration, gbcRec );

    this.fldDuration = new JTextField();
    this.fldDuration.setEditable( false );
    gbcRec.fill          = GridBagConstraints.HORIZONTAL;
    gbcRec.insets.bottom = 5;
    gbcRec.weightx = 1.0;
    gbcRec.gridy++;
    panelRec.add( this.fldDuration, gbcRec );

    JPanel panelRecBtn = new JPanel( new GridLayout( 1, 5, 5, 5 ) );
    gbcRec.anchor      = GridBagConstraints.CENTER;
    gbcRec.fill        = GridBagConstraints.NONE;
    gbcRec.weightx     = 0.0;
    gbcRec.gridy++;
    panelRec.add( panelRecBtn, gbcRec );

    this.btnRecord = EmuUtil.createImageButton(
				this.audioFrm,
				"/images/audio/record.png",
				"Aufnehmen" );
    panelRecBtn.add( this.btnRecord );

    this.btnPause = EmuUtil.createImageButton(
				this.audioFrm,
				"/images/audio/pause.png",
				"Pause" );
    panelRecBtn.add( this.btnPause );

    this.btnPlay = EmuUtil.createImageButton(
				this.audioFrm,
				"/images/audio/play.png",
				"Wiedergeben" );
    panelRecBtn.add( this.btnPlay );

    this.btnSave = EmuUtil.createImageButton(
				this.audioFrm,
				"/images/file/save_as.png",
				"Speichern unter..." );
    panelRecBtn.add( this.btnSave );

    this.btnDelete = EmuUtil.createImageButton(
				this.audioFrm,
				"/images/audio/delete.png",
				"L\u00F6schen" );
    panelRecBtn.add( this.btnDelete );


    // rechte Seite
    JPanel panelEast = new JPanel( new GridBagLayout() );
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

    JPanel panelBtn = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
    panelEast.add( panelBtn, gbcEast );

    this.btnEnable = new JButton( "Aktivieren" );
    panelBtn.add( this.btnEnable );

    this.btnDisable = new JButton( "Deaktivieren" );
    panelBtn.add( this.btnDisable );


    // Pegelanzeige
    this.volumeBar = new VolumeBar( SwingConstants.VERTICAL );
    this.volumeBar.setBorder( BorderFactory.createTitledBorder( "Pegel" ) );
    this.volumeBar.setPreferredSize( new Dimension( 1, 1 ) );
    gbcEast.insets.top = 20;
    gbcEast.fill       = GridBagConstraints.BOTH;
    gbcEast.weightx    = 1.0;
    gbcEast.weighty    = 1.0;
    gbcEast.gridy++;
    panelEast.add( this.volumeBar, gbcEast );


    // Listener
    this.btnEnable.addActionListener( this );
    this.btnDisable.addActionListener( this );
    this.btnRecord.addActionListener( this );
    this.btnPause.addActionListener( this );
    this.btnPlay.addActionListener( this );
    this.btnSave.addActionListener( this );
    this.btnDelete.addActionListener( this );


    // sonstiges
    updMonoStereo();
    updFieldsEnabled();
  }


  public void resetFired()
  {
    if( this.emuThread.getSoundOut() != null ) {
      willReset();
    }
    if( this.wasEnabled
	&& (this.audioOut == null)
	&& (this.recordedData == null) )
    {
      final EmuThread emuThread = this.emuThread;
      final boolean   recording = this.wasRecording;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    try {
		      enableLine();
		      if( recording ) {
			AudioOut audioOut = emuThread.getSoundOut();
			if( audioOut != null ) {
			  audioOut.setRecording( true );
			}
		      }
		    }
		    catch( IOException ex ) {}
		    updMonoStereo();
		    updFieldsEnabled();
		  }
		} );
    }
    updMonoStereo();
    updFieldsEnabled();
  }


	/* --- ueberschriebene Methoden --- */

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
  protected void doEnable()
  {
    try {
      enableLine();
    }
    catch( IOException ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
  }


  @Override
  protected void doDisable()
  {
    super.doDisable();
    updMonoStereo();
  }


  @Override
  protected void updFieldsEnabled()
  {
    boolean  supportsMono   = false;
    boolean  supportsStereo = false;
    boolean  running        = false;
    boolean  recording      = false;
    boolean  recorded       = (this.recordedData != null);
    AudioOut audioOut       = this.audioOut;
    if( audioOut != null ) {
      running   = true;
      recording = audioOut.isRecording();
    } else {
      EmuSys emuSys = this.emuThread.getEmuSys();
      if( emuSys != null ) {
	supportsMono   = emuSys.supportsSoundOutMono();
	supportsStereo = emuSys.supportsSoundOutStereo();
      }
    }
    boolean supported = (supportsMono || supportsStereo);
    this.btnEnable.setEnabled( supported && !running);
    this.btnDisable.setEnabled( running);
    this.btnRecord.setEnabled( !recording && running );
    this.btnPause.setEnabled( recording );
    this.btnPlay.setEnabled( recorded && !running );
    this.btnSave.setEnabled( recorded && !running );
    this.btnDelete.setEnabled( recorded && !running );
    this.labelMixer.setEnabled( supported && !running);
    this.comboMixer.setEnabled( supported && !running);
    this.labelFrameRate.setEnabled( supported && !running );
    this.comboFrameRate.setEnabled( supported && !running );
    this.btnMono.setEnabled( supportsMono && supportsStereo && !running );
    this.btnStereo.setEnabled( supportsMono && supportsStereo && !running );
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


  @Override
  public void willReset()
  {
    this.wasEnabled   = false;
    this.wasRecording = false;
    AudioOut audioOut = this.emuThread.getSoundOut();
    if( audioOut != null ) {
      boolean wasRecording = audioOut.isRecording();
      doDisable();
      this.wasEnabled   = true;
      this.wasRecording = wasRecording;
    }
  }


	/* --- private Methoden --- */

  private void doDelete()
  {
    if( this.recordedData != null ) {
      if( BaseDlg.showYesNoDlg( this, "Aufnahme verwerfen?" ) ) {
	this.recordedData = null;
	this.fldDuration.setText( "" );
	this.fldDuration.setEnabled( false );
	this.btnDelete.setEnabled( false );
      }
    }
  }


  private void enableLine() throws IOException
  {
    EmuSys emuSys = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      if( emuSys.supportsSoundOutMono()
	  || emuSys.supportsSoundOutStereo() )
      {
	int speedKHz = this.audioFrm.getAndCheckSpeed();
	if( (speedKHz > 0) && checkRecordedDataSaved() ) {
	  AudioOut audioOut = null;
	  try {
	    audioOut = new AudioOut(
				this,
				this.emuThread.getZ80CPU(),
				speedKHz,
				getSelectedFrameRate(),
				true,
				getSelectedMixer(),
				!emuSys.supportsSoundOut8Bit(),
				emuSys.supportsSoundOutStereo()
					&& this.btnStereo.isSelected() );
	    this.fldFormat.setText( audioOut.getFormatText() );
	    this.fldDuration.setText( audioOut.getDurationText() );
	    this.audioOut = audioOut;
	    this.emuThread.setSoundOut( audioOut );
	    this.durationTimer.start();
	    updFieldsEnabled();
	  }
	  catch( IOException ex ) {
	    audioOut.stopAudio();
	    throw ex;
	  }
	}
      }
    }
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


  private void updMonoStereo()
  {
    EmuSys emuSys = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      if( emuSys.supportsSoundOutMono()
	  && !emuSys.supportsSoundOutStereo() )
      {
	this.btnMono.setSelected( true );
      }
      else if( !emuSys.supportsSoundOutMono()
	       && emuSys.supportsSoundOutStereo() )
      {
	this.btnStereo.setSelected( true );
      }
    }
  }
}
