/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die ausgangsseitige Emulation
 * des Kassettenrecorderanschlusses
 */

package jkcemu.audio;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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


public class TapeOutComponent extends AbstractAudioOutComponent
{
  private JRadioButton btnToLine;
  private JRadioButton btnToRecorder;


  public TapeOutComponent( AudioFrm audioFrm, EmuThread emuThread )
  {
    super( audioFrm, emuThread );

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
    add( panelFct, gbc );

    GridBagConstraints gbcFct = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpFct = new ButtonGroup();

    this.btnToLine = new JRadioButton(
		"Audiodaten \u00FCber Sound-System ausgegeben",
		true );
    grpFct.add( this.btnToLine );
    panelFct.add( this.btnToLine, gbcFct );

    this.btnToRecorder = new JRadioButton(
		"Audiodaten aufnehmen und in Datei speichern" );
    grpFct.add( this.btnToRecorder );
    gbcFct.insets.top    = 0;
    gbcFct.insets.bottom = 5;
    gbcFct.gridy++;
    panelFct.add( this.btnToRecorder, gbcFct );


    // Bereich Optionen
    JPanel panelOpt = new JPanel( new GridBagLayout() );
    panelOpt.setBorder( BorderFactory.createTitledBorder( "Optionen" ) );
    gbc.gridy++;
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
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.labelFormat = new JLabel( "Format:" );
    panelStatus.add( this.labelFormat, gbcStatus );

    this.labelDuration = new JLabel( "Aufnahmedauer:" );
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridy++;
    panelStatus.add( this.labelDuration, gbcStatus );

    this.fldFormat = new JTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.fill          = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx       = 1.0;
    gbcStatus.insets.bottom = 0;
    gbcStatus.gridy         = 0;
    gbcStatus.gridx++;
    panelStatus.add( this.fldFormat, gbcStatus );

    this.fldDuration = new JTextField();
    this.fldDuration.setEditable( false );
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridy++;
    panelStatus.add( this.fldDuration, gbcStatus );


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

    JPanel panelBtn = new JPanel( new GridLayout( 4, 1, 5, 5 ) );
    panelEast.add( panelBtn, gbcEast );

    this.btnEnable = new JButton( "Aktivieren" );
    panelBtn.add( this.btnEnable );

    this.btnDisable = new JButton( "Deaktivieren" );
    panelBtn.add( this.btnDisable );

    this.btnPlay = new JButton( "Abspielen" );
    panelBtn.add( this.btnPlay );

    this.btnSave = new JButton( "Speichern..." );
    panelBtn.add( this.btnSave );


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
    this.btnPlay.addActionListener( this );
    this.btnSave.addActionListener( this );
    this.btnToLine.addActionListener( this );
    this.btnToRecorder.addActionListener( this );


    // sonstiges
    updFieldsEnabled();
  }


  public void resetFired()
  {
    if( this.emuThread.getTapeOut() != null ) {
      willReset();
    }
    if( this.wasEnabled && (this.emuThread.getTapeOut() == null) ) {
      final EmuThread emuThread = this.emuThread;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    try {
		      enableAudio();
		    }
		    catch( IOException ex ) {}
		    updFieldsEnabled();
		  }
		} );
    }
    updFieldsEnabled();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void doEnable()
  {
    try {
      enableAudio();
    }
    catch( IOException ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
  }


  @Override
  protected void updFieldsEnabled()
  {
    boolean  supported    = false;
    boolean  running      = false;
    boolean  line         = this.btnToLine.isSelected();
    boolean  recorder     = this.btnToRecorder.isSelected();
    boolean  recorded     = (this.recordedData != null);
    String   formatText   = null;
    String   durationText = null;
    AudioOut audioOut     = this.audioOut;
    if( audioOut != null ) {
      running = true;
    } else {
      EmuSys emuSys = this.emuThread.getEmuSys();
      if( emuSys != null ) {
	supported  = emuSys.supportsTapeOut();
      }
    }
    this.btnEnable.setEnabled( supported && !running);
    this.btnDisable.setEnabled( running);
    this.btnPlay.setEnabled( recorded && !running );
    this.btnSave.setEnabled( recorded && !running );
    this.btnToLine.setEnabled( supported && !running);
    this.btnToRecorder.setEnabled( supported && !running);
    this.labelMixer.setEnabled( supported && !running && line );
    this.comboMixer.setEnabled( supported && !running && line );
    this.labelFrameRate.setEnabled( supported && !running);
    this.comboFrameRate.setEnabled( supported && !running);
    if( !running && (!recorder || !recorded) ) {
      this.fldFormat.setText( "" );
      this.fldDuration.setText( "" );
    }
    this.labelFormat.setEnabled( running);
    this.fldFormat.setEnabled( running);
    this.labelDuration.setEnabled( recorder && running );
    if( running && line ) {
      this.labelDuration.setText( "" );
    }
    this.fldDuration.setEnabled( recorder && running );
    this.volumeBar.setVolumeBarState( running );
  }


	/* --- private Methoden --- */

  private void enableAudio() throws IOException
  {
    EmuSys emuSys = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      if( emuSys.supportsTapeOut() ) {
	int speedKHz = this.audioFrm.getAndCheckSpeed();
	if( (speedKHz > 0) && checkRecordedDataSaved() ) {
	  AudioOut audioOut = null;
	  try {
	    if( this.btnToLine.isSelected() ) {
	      audioOut = new AudioOut(
				this,
				this.emuThread.getZ80CPU(),
				speedKHz,
				getSelectedFrameRate(),
				true,
				getSelectedMixer(),
				true,
				false );
	    } else if( this.btnToRecorder.isSelected() ) {
	      audioOut = new AudioOut(
				this,
				this.emuThread.getZ80CPU(),
				speedKHz,
				getSelectedFrameRate(),
				false,
				null,
				true,
				false );
	      audioOut.setRecording( true );
	      this.durationTimer.start();
	    }
	    if( audioOut != null ) {
	      this.fldFormat.setText( audioOut.getFormatText() );
	      this.fldDuration.setText( audioOut.getDurationText() );
	      this.audioOut = audioOut;
	      this.emuThread.setTapeOut( audioOut );
	      updFieldsEnabled();
	    }
	  }
	  catch( IOException ex ) {
	    audioOut.stopAudio();
	    throw ex;
	  }
	}
      }
    }
  }
}
