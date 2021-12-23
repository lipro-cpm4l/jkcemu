/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die ausgangsseitige Emulation
 * des Kassettenrecorderanschlusses
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class TapeOutFld extends AbstractAudioOutFld
{
  public static final String PROP_PREFIX = "jkcemu.audio.tape.out.";

  private static final String PROP_TO_LINE     = "to_line";
  private static final String PROP_TO_RECORDER = "to_recorder";

  private boolean   notified;
  private JCheckBox cbToLine;
  private JCheckBox cbToRecorder;


  public TapeOutFld( AudioFrm audioFrm, EmuThread emuThread )
  {
    super( audioFrm, emuThread );
    this.notified = false;

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
    JPanel panelFct = GUIFactory.createPanel( new GridBagLayout() );
    panelFct.setBorder( GUIFactory.createTitledBorder( "Funktion" ) );
    add( panelFct, gbc );

    GridBagConstraints gbcFct = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbToLine = GUIFactory.createCheckBox(
		"Audiodaten \u00FCber Sound-System ausgegeben",
		true );
    panelFct.add( this.cbToLine, gbcFct );

    this.cbToRecorder = GUIFactory.createCheckBox(
		"Audiodaten aufnehmen und in Datei speichern" );
    gbcFct.insets.top    = 0;
    gbcFct.insets.bottom = 5;
    gbcFct.gridy++;
    panelFct.add( this.cbToRecorder, gbcFct );


    // Bereich Optionen
    JPanel panelOpt = GUIFactory.createPanel( new GridBagLayout() );
    panelOpt.setBorder( GUIFactory.createTitledBorder( "Optionen" ) );
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

    this.labelMixer = GUIFactory.createLabel( "Ausgabeger\u00E4t:" );
    panelOpt.add( this.labelMixer, gbcOpt );

    this.labelFrameRate  = GUIFactory.createLabel( "Abtastrate (Hz):" );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.labelFrameRate, gbcOpt );

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
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.labelFormat = GUIFactory.createLabel( "Format:" );
    panelStatus.add( this.labelFormat, gbcStatus );

    this.labelDuration = GUIFactory.createLabel( "Aufnahmedauer:" );
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridy++;
    panelStatus.add( this.labelDuration, gbcStatus );

    this.fldFormat = GUIFactory.createTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.fill          = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx       = 1.0;
    gbcStatus.insets.bottom = 0;
    gbcStatus.gridy         = 0;
    gbcStatus.gridx++;
    panelStatus.add( this.fldFormat, gbcStatus );

    this.fldDuration = GUIFactory.createTextField();
    this.fldDuration.setEditable( false );
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridy++;
    panelStatus.add( this.fldDuration, gbcStatus );


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

    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 4, 1, 5, 5 ) );
    panelEast.add( panelBtn, gbcEast );

    this.btnEnable = GUIFactory.createButton( "Aktivieren" );
    panelBtn.add( this.btnEnable );

    this.btnDisable = GUIFactory.createButton( "Deaktivieren" );
    panelBtn.add( this.btnDisable );

    this.btnPlay = GUIFactory.createButton( EmuUtil.TEXT_PLAY );
    panelBtn.add( this.btnPlay );

    this.btnSave = GUIFactory.createButton( EmuUtil.TEXT_OPEN_SAVE );
    panelBtn.add( this.btnSave );


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
	boolean toLine = EmuUtil.getBooleanProperty(
					props,
					prefix + PROP_TO_LINE,
					false );
	boolean toRecorder = EmuUtil.getBooleanProperty(
					props,
					prefix + PROP_TO_RECORDER,
					false );
	if( !toLine && !toRecorder ) {
	  toLine = true;
	}
	this.cbToLine.setSelected( toLine );
	this.cbToRecorder.setSelected( toRecorder );
	updFieldsEnabled();
	rv = enableAudio();
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    }
    return rv;
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
      this.btnPlay.addActionListener( this );
      this.btnSave.addActionListener( this );
      this.cbToLine.addActionListener( this );
      this.cbToRecorder.addActionListener( this );
    }
  }


  @Override
  protected void audioFinished( AudioIO audioIO, String errorMsg )
  {
    super.audioFinished( audioIO, errorMsg );
    EmuSys emuSys = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      if( emuSys.getTapeOut() == audioIO ) {
	emuSys.setTapeOut( null );
      }
    }
  }


  @Override
  protected boolean doAction( ActionEvent e )
  {
    boolean rv  = true;
    Object  src = e.getSource();
    if( src == this.cbToLine ) {
      rv = true;
      if( !this.cbToLine.isSelected()
	  && !this.cbToRecorder.isSelected() )
      {
	this.cbToRecorder.setSelected( true );
      }
      updFieldsEnabled();
    } else if( src == this.cbToRecorder ) {
      rv = true;
      if( !this.cbToLine.isSelected()
	  && !this.cbToRecorder.isSelected() )
      {
	this.cbToLine.setSelected( true );
      }
      updFieldsEnabled();
    } else {
      rv = super.doAction( e );
    }
    return rv;
  }


  @Override
  protected void doEnable( boolean interactive )
  {
    try {
      this.audioFrm.checkOpenCPUSynchronLine();
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
    return PROP_PREFIX;
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
		getTapeOut() != null );
      EmuUtil.setProperty(
		props,
		prefix + PROP_TO_LINE,
		this.cbToLine.isSelected() );
      EmuUtil.setProperty(
		props,
		prefix + PROP_TO_RECORDER,
		this.cbToRecorder.isSelected() );
    }
  }


  @Override
  public void removeNotify()
  {
    if( this.notified ) {
      this.notified = false;
      this.btnEnable.removeActionListener( this );
      this.btnDisable.removeActionListener( this );
      this.btnPlay.removeActionListener( this );
      this.btnSave.removeActionListener( this );
      this.cbToLine.removeActionListener( this );
      this.cbToRecorder.removeActionListener( this );
    }
    super.removeNotify();
  }


  @Override
  public void updFieldsEnabled()
  {
    boolean  supported = false;
    boolean  running   = false;
    boolean  line      = this.cbToLine.isSelected();
    boolean  recorder  = this.cbToRecorder.isSelected();
    boolean  recorded  = (this.recordedData != null);
    AudioOut audioOut  = this.audioOut;
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
    this.cbToLine.setEnabled( supported && !running);
    this.cbToRecorder.setEnabled( supported && !running);
    this.labelMixer.setEnabled( supported && !running && line );
    this.comboMixer.setEnabled( supported && !running && line );
    this.labelFrameRate.setEnabled( supported && !running);
    this.comboFrameRate.setEnabled( supported && !running);
    if( !running && !recorded ) {
      this.fldFormat.setText( "" );
      this.fldDuration.setText( "" );
    }
    this.labelFormat.setEnabled( running);
    this.fldFormat.setEnabled( running);
    this.labelDuration.setEnabled( recorder && running );
    this.fldDuration.setEnabled( recorder && running );
    this.volumeBar.setVolumeBarState( running );
  }


	/* --- private Methoden --- */

  private boolean enableAudio() throws IOException
  {
    boolean rv     = false;
    EmuSys  emuSys = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      if( emuSys.supportsTapeOut() ) {
	int speedKHz = this.audioFrm.getAndCheckSpeed();
	if( (speedKHz > 0) && checkRecordedDataSaved() ) {
	  AudioOut audioOut = new AudioOut(
				this,
				this.emuThread.getZ80CPU(),
				speedKHz,
				getSelectedFrameRate(),
				this.cbToLine.isSelected(),
				true,
				false,
				getSelectedMixerInfo(),
				true );
	  if( this.cbToRecorder.isSelected() ) {
	    audioOut.setRecording( true );
	    this.durationTimer.start();
	  }
	  this.fldDuration.setText( audioOut.getDurationText() );
	  this.audioOut = audioOut;
	  emuSys.setTapeOut( audioOut );
	  updFieldsEnabled();
	  rv = true;
	}
      }
    }
    return rv;
  }


  private AudioOut getTapeOut()
  {
    EmuSys emuSys = this.emuThread.getEmuSys();
    return emuSys != null ? emuSys.getTapeOut() : null;
  }
}
