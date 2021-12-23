/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die eingangsseitige Emulation
 * des Kassettenrecorderanschlusses
 */

package jkcemu.audio;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;


public class TapeInFld
		extends AbstractAudioIOFld
		implements ChangeListener, DropTargetListener
{
  public static final String PROP_PREFIX = "jkcemu.audio.tape.in.";

  private static final String PROP_CHANNEL            = "channel";
  private static final String PROP_FILE               = "file";
  private static final String PROP_MONITOR_ENABLED    = "monitor.enabled";
  private static final String PROP_MONITOR_MIXER_NAME = "monitor.mixer.name";
  private static final String PROP_SOURCE             = "source";
  private static final String VALUE_FILE              = "file";
  private static final String VALUE_LINE              = "line";

  private byte[]            fileBytes;
  private boolean           notified;
  private boolean           wasEnabled;
  private boolean           maxSpeedTriggered;
  private JRadioButton      rbFromLine;
  private JRadioButton      rbFromFile;
  private JRadioButton      rbFromLastFile;
  private JRadioButton      rbChannel0;
  private JRadioButton      rbChannel1;
  private JComboBox<Object> comboMonitorMixer;
  private JLabel            labelChannel;
  private JLabel            labelMonitor;
  private JLabel            labelFile;
  private JLabel            labelProgress;
  private FileNameFld       fldFile;
  private JTextField        fldProgressPos;
  private JSlider           progressSlider;
  private JButton           btnFilePlay;
  private JButton           btnFilePause;
  private JButton           btnMaxSpeed;
  private DropTarget        dropTarget;


  public TapeInFld( AudioFrm audioFrm, EmuThread emuThread )
  {
    super( audioFrm, emuThread );
    this.fileBytes         = null;
    this.notified          = false;
    this.wasEnabled        = false;
    this.maxSpeedTriggered = false;

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

    ButtonGroup grpFct = new ButtonGroup();

    this.rbFromLine = GUIFactory.createRadioButton(
	"Audiodaten vom Sound-System lesen"
		+ " (z.B. Mikrofon- oder Line-In-Anschluss)",
	true );
    grpFct.add( this.rbFromLine );
    panelFct.add( this.rbFromLine, gbcFct );

    this.rbFromFile = GUIFactory.createRadioButton(
	"Audiodaten aus Sound- oder Tape-Datei lesen" );
    grpFct.add( this.rbFromFile );
    gbcFct.insets.top = 0;
    gbcFct.gridy++;
    panelFct.add( this.rbFromFile, gbcFct );

    this.rbFromLastFile = GUIFactory.createRadioButton(
	"Letzte Sound- oder Tape-Datei noch einmal lesen" );
    grpFct.add( this.rbFromLastFile );
    gbcFct.insets.bottom = 5;
    gbcFct.gridy++;
    panelFct.add( this.rbFromLastFile, gbcFct );


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

    this.labelMixer = GUIFactory.createLabel( "Eingabeger\u00E4t:" );
    panelOpt.add( this.labelMixer, gbcOpt );

    this.labelFrameRate = GUIFactory.createLabel( "Abtastrate (Hz):" );
    gbcOpt.gridy++;
    panelOpt.add( this.labelFrameRate, gbcOpt );

    this.labelChannel = GUIFactory.createLabel( "Aktiver Kanal:" );
    gbcOpt.gridy++;
    panelOpt.add( this.labelChannel, gbcOpt );

    this.labelMonitor = GUIFactory.createLabel( "Mith\u00F6ren \u00FCber:" );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.labelMonitor, gbcOpt );

    this.comboMixer      = AudioUtil.createMixerComboBox( true );
    gbcOpt.insets.top    = 5;
    gbcOpt.insets.bottom = 0;
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridy         = 0;
    gbcOpt.gridx++;
    panelOpt.add( this.comboMixer, gbcOpt );

    this.comboFrameRate = AudioUtil.createFrameRateComboBox();
    this.comboFrameRate.setEditable( false );
    gbcOpt.gridy++;
    panelOpt.add( this.comboFrameRate, gbcOpt );

    ButtonGroup grpChannel = new ButtonGroup();

    this.rbChannel0 = GUIFactory.createRadioButton( "Links", true );
    grpChannel.add( this.rbChannel0 );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridwidth     = 1;
    gbcOpt.gridy++;
    panelOpt.add( this.rbChannel0, gbcOpt );

    this.rbChannel1 = GUIFactory.createRadioButton( "Rechts" );
    grpChannel.add( this.rbChannel1 );
    gbcOpt.gridx++;
    panelOpt.add( this.rbChannel1, gbcOpt );

    this.comboMonitorMixer = GUIFactory.createComboBox();
    this.comboMonitorMixer.setEditable( false );
    this.comboMonitorMixer.addItem( "--- nicht mith\u00F6ren ---" );
    AudioUtil.appendMixerItemsTo( this.comboMonitorMixer, false );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridy++;
    panelOpt.add( this.comboMonitorMixer, gbcOpt );


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

    this.labelFile = GUIFactory.createLabel( "Datei:" );
    panelStatus.add( this.labelFile, gbcStatus );

    this.labelFormat = GUIFactory.createLabel( "Format:" );
    gbcStatus.gridy++;
    panelStatus.add( this.labelFormat, gbcStatus );

    this.labelProgress      = GUIFactory.createLabel( "Fortschritt:" );
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridy++;
    panelStatus.add( this.labelProgress, gbcStatus );

    this.fldFile            = new FileNameFld();
    gbcStatus.fill          = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx       = 1.0;
    gbcStatus.insets.bottom = 0;
    gbcStatus.gridwidth     = GridBagConstraints.REMAINDER;
    gbcStatus.gridy         = 0;
    gbcStatus.gridx++;
    panelStatus.add( this.fldFile, gbcStatus );

    this.fldFormat = GUIFactory.createTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.gridy++;
    panelStatus.add( this.fldFormat, gbcStatus );

    this.fldProgressPos = GUIFactory.createTextField( 7 );
    this.fldProgressPos.setEditable( false );
    gbcStatus.fill          = GridBagConstraints.NONE;
    gbcStatus.weightx       = 0.0;
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridwidth     = 1;
    gbcStatus.gridy++;
    panelStatus.add( this.fldProgressPos, gbcStatus );

    this.progressSlider = GUIFactory.createSlider(
					JSlider.HORIZONTAL,
					0,
					1000,
					0 );
    this.progressSlider.setPaintLabels( false );
    this.progressSlider.setPaintTicks( false );
    this.progressSlider.setPaintTrack( true );
    this.progressSlider.setSnapToTicks( false );
    gbcStatus.fill    = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx = 1.0;
    gbcStatus.gridx++;
    panelStatus.add( this.progressSlider, gbcStatus );


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

    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 5, 1, 5, 5 ) );
    panelEast.add( panelBtn, gbcEast );

    this.btnEnable = GUIFactory.createButton( "Aktivieren" );
    panelBtn.add( this.btnEnable );

    this.btnDisable = GUIFactory.createButton( "Deaktivieren" );
    panelBtn.add( this.btnDisable );

    this.btnFilePlay = GUIFactory.createButton( "Abspielen" );
    panelBtn.add( this.btnFilePlay );

    this.btnFilePause = GUIFactory.createButton( "Pause" );
    panelBtn.add( this.btnFilePause );

    this.btnMaxSpeed = GUIFactory.createButton( "Turbo" );
    panelBtn.add( this.btnMaxSpeed );


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
    updProgressPos( null );


    // Dateinamensfeld als DropTarget aktivieren
    this.dropTarget = new DropTarget( this.fldFile, this );


    // Schaltflaechen aktivieren/deaktivieren
    updFieldsEnabled();
  }


  public boolean checkEnableAudio( Properties props )
  {
    boolean rv = false;
    if( props != null ) {
      String  prefix = getSettingsPrefix();
      if( EmuUtil.getBooleanProperty(
				props,
				prefix + PROP_ENABLED,
				false ) )
      {
	try {
	  setSelectedFrameRate( getFrameRate( props, prefix ) );
	  setSelectedMixerByName( getMixerName( props, prefix ) );
	  if( EmuUtil.getIntProperty(
				props,
				prefix + PROP_CHANNEL, 0 ) == 1 )
	  {
	    this.rbChannel1.setSelected( true );
	  } else {
	    this.rbChannel0.setSelected( true );
	  }
	  if( EmuUtil.getProperty(
		props,
		prefix + PROP_SOURCE ).toLowerCase().equals( VALUE_FILE ) )
	  {
	    String fileName = EmuUtil.getProperty( props, prefix + PROP_FILE );
	    if( !fileName.isEmpty() ) {
	      File file = new File( fileName );
	      this.fldFile.setFile( file );
	      this.rbFromLastFile.setSelected( true );
	      if( EmuUtil.getBooleanProperty(
					props,
					prefix + PROP_MONITOR_ENABLED,
					false ) )
	      {
		setSelectedMixerByName(
			props.getProperty(
				prefix + PROP_MONITOR_MIXER_NAME ) );
		if( this.comboMonitorMixer.getSelectedIndex() < 1 ) {
		  try {
		    this.comboMonitorMixer.setSelectedIndex( 1 );
		  }
		  catch( IllegalArgumentException ex ) {}
		}
	      } else {
		resetMonitorMixer();
	      }
	      updFieldsEnabled();
	      rv = enableFile( file, null, 0 );
	    }
	  } else {
	    this.rbFromLine.setSelected( true );
	    updFieldsEnabled();
	    rv = enableLine();
	  }
	}
	catch( IOException ex ) {
	  BaseDlg.showErrorDlg( this, ex );
	}
      }
    }
    return rv;
  }


  /*
   * Oeffnen einer Datei
   *
   * fileBytes kann null sein
   */
  public void openFile( File file, byte[] fileBytes, int offs )
  {
    if( (file != null) || (fileBytes != null) ) {
      EmuSys emuSys = this.emuThread.getEmuSys();
      if( (emuSys != null) && (getTapeIn() == null) ) {
	if( emuSys.supportsTapeIn() ) {
	  int speedKHz = this.audioFrm.getAndCheckSpeed();
	  if( speedKHz > 0 ) {
	    this.rbFromFile.setSelected( true );
	    try {
	      enableFile( speedKHz, file, fileBytes, offs );
	    }
	    catch( IOException ex ) {
	      BaseDlg.showErrorDlg( this, ex );
	    }
	  }
	}
      }
    }
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource() == this.progressSlider ) {
      AudioIn audioIn = getTapeIn();
      if( audioIn != null ) {
	if( audioIn instanceof AudioInFile ) {
	  AudioInFile audioInFile = (AudioInFile) audioIn;
	  if( audioInFile.supportsSetFramePos() ) {
	    long frameCount = audioInFile.getFrameCount();
	    if( frameCount > 0 ) {
	      int vMin  = this.progressSlider.getMinimum();
	      int range = this.progressSlider.getMaximum() - vMin;
	      if( range > 0 ) {
		int value = this.progressSlider.getValue();
		try {
		  audioInFile.setFramePos(
			Math.round( (double) (value - vMin)
					/ (double) range
					* (double) frameCount ) );
		  updProgressPos( audioInFile );
		}
		catch( IOException ex ) {
		  updProgressSlider( audioInFile );
		  BaseDlg.showErrorDlg(
			this,
			"\u00C4ndern der Abspielposition bei\n"
				+ " dieser Datei nicht m\u00F6glich" );
		}
	      }
	    }
	  }
	}
      }
    }
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( (getTapeIn() != null) || !FileUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    boolean done = false;
    if( FileUtil.isFileDrop( e ) ) {
      final File file = FileUtil.fileDrop( this, e );
      if( file != null ) {
	if( file.isFile() ) {
	  EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    openFile( file, null, 0 );
			  }
			} );
	}
      }
      done = true;
    }
    if( !done ) {
      e.rejectDrop();
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.progressSlider.addChangeListener( this );
      this.btnEnable.addActionListener( this );
      this.btnDisable.addActionListener( this );
      this.btnFilePlay.addActionListener( this );
      this.btnFilePause.addActionListener( this );
      this.btnMaxSpeed.addActionListener( this );
      this.rbFromLine.addActionListener( this );
      this.rbFromFile.addActionListener( this );
      this.rbFromLastFile.addActionListener( this );
      this.rbChannel0.addActionListener( this );
      this.rbChannel1.addActionListener( this );
      this.comboMonitorMixer.addActionListener( this );
      this.dropTarget.setActive( true );
    }
  }


  @Override
  protected void audioFinished( AudioIO audioIO, String errorMsg )
  {
    super.audioFinished( audioIO, errorMsg );
    EmuSys emuSys = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      if( emuSys.getTapeIn() == audioIO ) {
	emuSys.setTapeIn( null );
      }
    }
    updFieldsEnabled();
  }


  @Override
  public boolean doAction( ActionEvent e )
  {
    boolean rv  = true;
    Object  src = e.getSource();
    if( (src == this.rbChannel0) || (src == this.rbChannel1) ) {
      updSelectedChannel();
    } else if( src == this.comboMonitorMixer ) {
      if( this.comboMonitorMixer.getSelectedIndex() > 0 ) {
	setMaxSpeed( false );
      }
      updMonitorEnabled();
    } else if( src == this.btnFilePlay ) {
      doPlay();
    } else if( src == this.btnFilePause ) {
      doPause();
    } else if( src == this.btnMaxSpeed ) {
      doMaxSpeed();
    } else {
      rv = super.doAction( e );
    }
    return rv;
  }


  @Override
  protected void doDisable()
  {
    AudioIn audioIn = getTapeIn();
    if( audioIn != null ) {
      audioIn.fireStop();
    }
    if( this.maxSpeedTriggered ) {
      setMaxSpeed( false );
    }
  }


  @Override
  protected void doEnable( boolean interactive )
  {
    try {
      if( this.rbFromLine.isSelected() ) {
	this.audioFrm.checkOpenCPUSynchronLine();
	enableLine();
      } else if( this.rbFromFile.isSelected() ) {
	if( (getTapeIn() == null)
	    && this.emuThread.getEmuSys().supportsTapeIn() )
	{
	  if( interactive ) {
	    File file = FileUtil.showFileOpenDlg(
			this.audioFrm,
			"Sound- oder Tape-Datei \u00F6ffnen",
			Main.getLastDirFile( Main.FILE_GROUP_AUDIO ),
			AudioFile.getFileFilter(),
			FileUtil.getTapeFileFilter() );
	    if( file != null ) {
	      Main.setLastFile( file, Main.FILE_GROUP_AUDIO );
	      enableFile( file, null, 0 );
	    }
	  } else {
	    enableLastFile();
	  }
	}
      } else if( this.rbFromLastFile.isSelected() ) {
	enableLastFile();
      }
    }
    catch( IOException ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
  }


  @Override
  public void fireProgressUpdate( final AudioInFile audioInFile )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updProgressSlider( audioInFile );
		  }
		} );
  }


  @Override
  protected void formatChanged( AudioIO audioIO, String formatText )
  {
    this.fldFormat.setText( formatText != null ? formatText : "" );
    updSelectedChannel();
    updChannelFieldsEnabled();
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
		getTapeIn() != null );
      EmuUtil.setProperty(
		props,
		prefix + PROP_SOURCE,
		this.rbFromFile.isSelected() ? VALUE_FILE : VALUE_LINE );
      EmuUtil.setProperty(
		props,
		prefix + PROP_CHANNEL,
		this.rbChannel1.isSelected() ? 1 : 0 );
      File file = null;
      if( this.rbFromFile.isSelected()
	  || this.rbFromLastFile.isSelected() )
      {
	file = this.fldFile.getFile();
      }
      EmuUtil.setProperty(
		props,
		prefix + PROP_FILE,
		file != null ? file.getPath() : "" );
      if( this.comboMonitorMixer.getSelectedIndex() > 0 ) {
	EmuUtil.setProperty(
		props,
		prefix + PROP_MONITOR_ENABLED,
		true );
	EmuUtil.setProperty(
		props,
		prefix + PROP_MONITOR_MIXER_NAME,
		this.comboMonitorMixer.getSelectedItem() );
      } else {
	EmuUtil.setProperty(
		props,
		prefix + PROP_MONITOR_ENABLED,
		false );
	EmuUtil.setProperty(
		props,
		prefix + PROP_MONITOR_MIXER_NAME,
		null );
      }
    }
  }


  @Override
  public void removeNotify()
  {
    if( this.notified ) {
      this.notified = false;
      this.dropTarget.setActive( false );
      this.progressSlider.removeChangeListener( this );
      this.btnEnable.removeActionListener( this );
      this.btnDisable.removeActionListener( this );
      this.btnFilePlay.removeActionListener( this );
      this.btnFilePause.removeActionListener( this );
      this.btnMaxSpeed.removeActionListener( this );
      this.rbFromLine.removeActionListener( this );
      this.rbFromFile.removeActionListener( this );
      this.rbFromLastFile.removeActionListener( this );
      this.rbChannel0.removeActionListener( this );
      this.rbChannel1.removeActionListener( this );
      this.comboMonitorMixer.removeActionListener( this );
    }
    super.removeNotify();
  }


  @Override
  public void updFieldsEnabled()
  {
    boolean supported  = false;
    boolean running    = false;
    boolean fromFile   = false;
    boolean fromLine   = false;
    boolean pause      = false;
    boolean progress   = false;
    String  formatText = null;
    File    file       = this.fldFile.getFile();
    EmuSys  emuSys     = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      supported = emuSys.supportsTapeIn();
    }
    AudioIn audioIn = getTapeIn();
    if( audioIn != null ) {
      running  = true;
      fromLine = (audioIn instanceof AudioInLine);
      fromFile = (audioIn instanceof AudioInFile);
      pause    = audioIn.isPause();
      progress = fromFile && (this.fldFile.getFile() != null);
    } else {
      fromLine = this.rbFromLine.isSelected();
      fromFile = this.rbFromFile.isSelected()
			|| this.rbFromLastFile.isSelected();
      this.fldFormat.setText( "" );
    }
    this.btnEnable.setEnabled( supported && !running );
    this.btnDisable.setEnabled( running );
    this.btnFilePlay.setEnabled( running && fromFile && pause );
    this.btnFilePause.setEnabled( running && fromFile && !pause );
    this.btnMaxSpeed.setEnabled( running && fromFile && !pause );
    this.rbFromLine.setEnabled( supported && !running );
    this.rbFromFile.setEnabled( supported && !running );
    this.rbFromLastFile.setEnabled(
				supported && !running && (file != null) );
    if( this.rbFromLastFile.isSelected() && (file == null) ) {
      this.rbFromFile.setSelected( true );
    }
    this.labelMixer.setEnabled( supported && !running && fromLine );
    this.comboMixer.setEnabled( supported && !running && fromLine );
    this.labelFrameRate.setEnabled( supported && !running && fromLine );
    this.comboFrameRate.setEnabled( supported && !running && fromLine );
    this.labelMonitor.setEnabled( supported && fromFile );
    this.comboMonitorMixer.setEnabled( supported && fromFile );
    this.labelFile.setEnabled( running && fromFile );
    this.fldFile.setEnabled( running && fromFile );
    this.labelFormat.setEnabled( running );
    this.fldFormat.setEnabled( running );
    if( !progress ) {
      this.progressSlider.setValue( this.progressSlider.getMinimum() );
      updProgressPos( null );
    }
    this.labelProgress.setEnabled( progress );
    this.fldProgressPos.setEnabled( progress );
    this.progressSlider.setEnabled( progress );
    this.volumeBar.setVolumeBarState( running );
    updChannelFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void doMaxSpeed()
  {
    if( (getTapeIn() != null)
	&& this.emuThread.getZ80CPU().isBrakeEnabled() )
    {
      this.maxSpeedTriggered = true;
      setMaxSpeed( true );
    }
  }


  private void doPlay()
  {
    AudioIn audioIn = getTapeIn();
    if( audioIn != null ) {
      if( audioIn instanceof AudioInFile ) {
	((AudioInFile) audioIn).setPause( false );
	this.btnFilePlay.setEnabled( false );
	this.btnFilePause.setEnabled( true );
	this.btnMaxSpeed.setEnabled( true );
      }
    }
  }


  private void doPause()
  {
    AudioIn audioIn = getTapeIn();
    if( audioIn != null ) {
      if( audioIn instanceof AudioInFile ) {
	if( this.maxSpeedTriggered ) {
	  setMaxSpeed( false );
	}
	((AudioInFile) audioIn).setPause( true );
	this.btnFilePlay.setEnabled( true );
	this.btnFilePause.setEnabled( false );
	this.btnMaxSpeed.setEnabled( false );
      }
    }
  }


  private boolean enableFile(
			File   file,
			byte[] fileBytes,
			int    offs ) throws IOException
  {
    boolean rv     = false;
    EmuSys  emuSys = this.emuThread.getEmuSys();
    if( (emuSys != null) && (getTapeIn() == null) ) {
      if( emuSys.supportsTapeIn() ) {
	int speedKHz = this.audioFrm.getAndCheckSpeed();
	if( speedKHz > 0 ) {
	  rv = enableFile( speedKHz, file, fileBytes, offs );
	}
      }
    }
    return rv;
  }


  private boolean enableFile(
			int    speedKHz,
			File   file,
			byte[] fileBytes,
			int    offs ) throws IOException
  {
    boolean rv     = false;
    EmuSys  emuSys = this.emuThread.getEmuSys();
    if( (emuSys != null) && (getTapeIn() == null) ) {
      if( emuSys.supportsTapeIn() ) {
	
	this.fldFile.setFile( file );
	this.fileBytes = fileBytes;
	emuSys.setTapeIn( new AudioInFile(
					this,
					this.emuThread.getZ80CPU(),
					speedKHz,
					file,
					fileBytes,
					offs ) );
	updFieldsEnabled();
	updMonitorEnabled();
	rv = true;
      }
    }
    return rv;
  }


  private void enableLastFile() throws IOException
  {
    File file = this.fldFile.getFile();
    if( (file != null) || (this.fileBytes != null) ) {
      enableFile( file, this.fileBytes, 0 );
    }
  }


  private boolean enableLine() throws IOException
  {
    boolean rv     = false;
    EmuSys  emuSys = this.emuThread.getEmuSys();
    if( (emuSys != null) && (getTapeIn() == null) ) {
      if( emuSys.supportsTapeIn() ) {
	int speedKHz = this.audioFrm.getAndCheckSpeed();
	if( speedKHz > 0 ) {
	  emuSys.setTapeIn(
	      new AudioInLine(
		this,
		this.emuThread.getZ80CPU(),
		speedKHz,
		AudioUtil.getSelectedFrameRate( this.comboFrameRate ),
		getSelectedMixerInfo() ) );
	  updSelectedChannel();
	  updFieldsEnabled();
	  rv = true;
	}
      }
    }
    return rv;
  }


  private AudioIn getTapeIn()
  {
    return emuThread.getEmuSys().getTapeIn();
  }


  private void resetMonitorMixer()
  {
    // keine Events ausloesen, wenn sich nichts aendert
    if( this.comboMonitorMixer.getSelectedIndex() > 0 ) {
      try {
	this.comboMonitorMixer.setSelectedIndex( 0 );
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  private void setMaxSpeed( boolean state )
  {
    if( state ) {
      AudioIn audioIn = getTapeIn();
      if( audioIn != null ) {
	if( audioIn instanceof AudioInFile ) {
	  if( this.audioFrm.getScreenFrm().setMaxSpeed( this, true ) ) {
	    if( this.comboMonitorMixer.getSelectedIndex() > 0 ) {
	      resetMonitorMixer();
	      updMonitorEnabled();
	    }
	  }
	}
      }
    } else {
      this.audioFrm.getScreenFrm().setMaxSpeed( this, false );
      this.maxSpeedTriggered = false;
    }
  }


  private void updChannelFieldsEnabled()
  {
    boolean supported = false;
    boolean running   = false;
    boolean stereo    = false;
    EmuSys  emuSys    = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      supported = emuSys.supportsTapeIn();
    }
    AudioIn audioIn = getTapeIn();
    if( audioIn != null ) {
      running = true;
      stereo  = (audioIn.getChannels() > 1);
    }
    this.labelChannel.setEnabled( supported && (!running || stereo) );
    this.rbChannel0.setEnabled( supported && (!running || stereo) );
    this.rbChannel1.setEnabled( supported && (!running || stereo) );
  }


  private void updMonitorEnabled()
  {
    AudioIn audioIn = getTapeIn();
    if( audioIn != null ) {
      if( audioIn.supportsMonitor() ) {
	if( this.comboMonitorMixer.getSelectedIndex() > 0 ) {
	  audioIn.setMonitorEnabled(
		true,
		AudioUtil.getSelectedMixerInfo( this.comboMonitorMixer ) );
	} else {
	  audioIn.setMonitorEnabled( false, null );
	}
      } else {
	resetMonitorMixer();
      }
    }
  }


  private void updProgressPos( AudioInFile audioInFile )
  {
    int seconds = 0;
    if( audioInFile != null ) {
      int frameRate = audioInFile.getFrameRate();
      if( frameRate > 0 ) {
	seconds = (int) (audioInFile.getFramePos() / frameRate);
	if( seconds < 0 ) {
	  seconds = 0;
	}
      }
    }
    this.fldProgressPos.setText(
			String.format(
				"%d:%02d:%02d",
				seconds / 3600,
				(seconds / 60) % 60,
				seconds % 60 ) );
  }


  private void updProgressSlider( AudioInFile audioInFile )
  {
    int minValue = this.progressSlider.getMinimum();
    int maxValue = this.progressSlider.getMaximum();
    int value    = minValue;
    int range    = maxValue - minValue;
    if( (range > 0) && (audioInFile != null) ) {
      long frameCnt = audioInFile.getFrameCount();
      if( frameCnt > 0 ) {
	value = minValue + Math.round(
				(float) audioInFile.getFramePos()
						/ (float) frameCnt
						* (float) range );
	if( value < minValue ) {
	  value = minValue;
	} else if( value > maxValue ) {
	  value = maxValue;
	}
      }
    }
    this.progressSlider.removeChangeListener( this );
    this.progressSlider.setValue( value );
    this.progressSlider.addChangeListener( this );
    updProgressPos( audioInFile );
  }


  private void updSelectedChannel()
  {
    AudioIn audioIn = getTapeIn();
    if( audioIn != null ) {
      int channels = audioIn.getChannels();
      if( channels > 0 ) {
	int channel = 0;
	if( this.rbChannel1.isSelected() ) {
	  channel = 1;
	}
	if( channel >= audioIn.getChannels() ) {
	  channel = 0;
	  this.rbChannel0.setSelected( true );
	}
	audioIn.setSelectedChannel( channel );
      }
    }
  }
}
