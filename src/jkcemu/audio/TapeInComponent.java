/*
 * (c) 2016 Jens Mueller
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
import java.lang.*;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import jkcemu.base.FileNameFld;


public class TapeInComponent
			extends AbstractAudioIOComponent
			implements ChangeListener, DropTargetListener
{
  private boolean      wasEnabled;
  private boolean      maxSpeedTriggered;
  private JRadioButton btnFromLine;
  private JRadioButton btnFromFile;
  private JRadioButton btnFromLastFile;
  private JRadioButton btnChannel0;
  private JRadioButton btnChannel1;
  private JCheckBox    btnMonitor;
  private JLabel       labelChannel;
  private JLabel       labelFile;
  private JLabel       labelProgress;
  private FileNameFld  fldFile;
  private JTextField   fldProgressPos;
  private JSlider      progressSlider;
  private JButton      btnFilePlay;
  private JButton      btnFilePause;
  private JButton      btnMaxSpeed;


  public TapeInComponent( AudioFrm audioFrm, EmuThread emuThread )
  {
    super( audioFrm, emuThread );
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

    this.btnFromLine = new JRadioButton(
	"Audiodaten vom Sound-System lesen"
		+ " (z.B. Mikrofon- oder Line-In-Anschluss)",
	true );
    grpFct.add( this.btnFromLine );
    panelFct.add( this.btnFromLine, gbcFct );

    this.btnFromFile = new JRadioButton(
	"Audiodaten aus Sound- oder Tape-Datei lesen" );
    grpFct.add( this.btnFromFile );
    gbcFct.insets.top = 0;
    gbcFct.gridy++;
    panelFct.add( this.btnFromFile, gbcFct );

    this.btnFromLastFile = new JRadioButton(
	"Letzte Sound- oder Tape-Datei noch einmal lesen" );
    grpFct.add( this.btnFromLastFile );
    gbcFct.insets.bottom = 5;
    gbcFct.gridy++;
    panelFct.add( this.btnFromLastFile, gbcFct );


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
    gbcOpt.gridy++;
    panelOpt.add( this.labelFrameRate, gbcOpt );

    this.labelChannel    = new JLabel( "Aktiver Kanal:" );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.labelChannel, gbcOpt );

    this.comboMixer      = this.audioFrm.createMixerComboBox();
    gbcOpt.insets.top    = 5;
    gbcOpt.insets.bottom = 0;
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridy         = 0;
    gbcOpt.gridx++;
    panelOpt.add( this.comboMixer, gbcOpt );

    this.comboFrameRate = this.audioFrm.createFrameRateComboBox();
    this.comboFrameRate.setEditable( false );
    gbcOpt.gridy++;
    panelOpt.add( this.comboFrameRate, gbcOpt );

    ButtonGroup grpChannel = new ButtonGroup();

    this.btnChannel0 = new JRadioButton( "Links", true );
    grpChannel.add( this.btnChannel0 );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridwidth     = 1;
    gbcOpt.gridy++;
    panelOpt.add( this.btnChannel0, gbcOpt );

    this.btnChannel1 = new JRadioButton( "Rechts" );
    grpChannel.add( this.btnChannel1 );
    gbcOpt.gridx++;
    panelOpt.add( this.btnChannel1, gbcOpt );

    this.btnMonitor = new JCheckBox( "Mith\u00F6ren" );
    gbcOpt.anchor   = GridBagConstraints.EAST;
    gbcOpt.gridx++;
    panelOpt.add( this.btnMonitor, gbcOpt );


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

    this.labelFile = new JLabel( "Datei:" );
    panelStatus.add( this.labelFile, gbcStatus );

    this.labelFormat = new JLabel( "Format:" );
    gbcStatus.gridy++;
    panelStatus.add( this.labelFormat, gbcStatus );

    this.labelProgress      = new JLabel( "Fortschritt:" );
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

    this.fldFormat = new JTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.gridy++;
    panelStatus.add( this.fldFormat, gbcStatus );

    this.fldProgressPos = new JTextField( 7 );
    this.fldProgressPos.setEditable( false );
    gbcStatus.fill          = GridBagConstraints.NONE;
    gbcStatus.weightx       = 0.0;
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridwidth     = 1;
    gbcStatus.gridy++;
    panelStatus.add( this.fldProgressPos, gbcStatus );

    this.progressSlider = new JSlider( JSlider.HORIZONTAL, 0, 1000, 0 );
    this.progressSlider.setPaintLabels( false );
    this.progressSlider.setPaintTicks( false );
    this.progressSlider.setPaintTrack( true );
    this.progressSlider.setSnapToTicks( false );
    gbcStatus.fill    = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx = 1.0;
    gbcStatus.gridx++;
    panelStatus.add( this.progressSlider, gbcStatus );


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

    JPanel panelBtn = new JPanel( new GridLayout( 5, 1, 5, 5 ) );
    panelEast.add( panelBtn, gbcEast );

    this.btnEnable = new JButton( "Aktivieren" );
    panelBtn.add( this.btnEnable );

    this.btnDisable = new JButton( "Deaktivieren" );
    panelBtn.add( this.btnDisable );

    this.btnFilePlay = new JButton( "Abspielen" );
    panelBtn.add( this.btnFilePlay );

    this.btnFilePause = new JButton( "Pause" );
    panelBtn.add( this.btnFilePause );

    this.btnMaxSpeed = new JButton( "Turbo" );
    panelBtn.add( this.btnMaxSpeed );


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
    this.progressSlider.addChangeListener( this );
    this.btnEnable.addActionListener( this );
    this.btnDisable.addActionListener( this );
    this.btnFilePlay.addActionListener( this );
    this.btnFilePause.addActionListener( this );
    this.btnMaxSpeed.addActionListener( this );
    this.btnFromLine.addActionListener( this );
    this.btnFromFile.addActionListener( this );
    this.btnFromLastFile.addActionListener( this );
    this.btnChannel0.addActionListener( this );
    this.btnChannel1.addActionListener( this );
    this.btnMonitor.addActionListener( this );


    // Dateinamensfeld als DropTarget aktivieren
    (new DropTarget( this.fldFile, this )).setActive( true );

    // sonstiges
    updFieldsEnabled();
  }


  /*
   * Oeffnen einer Datei
   *
   * fileBytes kann null sein
   */
  public void openFile( File file, byte[] fileBytes, int offs )
  {
    if( file != null ) {
      EmuSys emuSys = this.emuThread.getEmuSys();
      if( (emuSys != null) && (this.emuThread.getTapeIn() == null) ) {
	if( emuSys.supportsTapeIn() ) {
	  int speedKHz = this.audioFrm.getAndCheckSpeed();
	  if( speedKHz > 0 ) {
	    this.btnFromFile.setSelected( true );
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


  public void resetFired()
  {
    if( this.emuThread.getTapeIn() != null ) {
      willReset();
    }
    if( this.wasEnabled && (this.emuThread.getTapeIn() == null) ) {
      if( this.btnFromLine.isSelected() ) {
	EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    try {
		      enableLine();
		    }
		    catch( IOException ex ) {}
		    updFieldsEnabled();
		  }
		} );
      }
      else if( this.btnFromFile.isSelected()
	       || this.btnFromLastFile.isSelected() )
      {
	final File file = this.fldFile.getFile();
	if( file != null ) {
	  EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    try {
		      enableFile( file, null, 0 );
		    }
		    catch( IOException ex ) {}
		    updFieldsEnabled();
		  }
		} );
	}
      }
    }
    updFieldsEnabled();
  }


  public void willReset()
  {
    this.wasEnabled = (this.emuThread.getTapeIn() != null);
    if( this.wasEnabled ) {
      doDisable();
    }
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource() == this.progressSlider ) {
      AudioIn audioIn = this.emuThread.getTapeIn();
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
    if( (this.emuThread.getTapeIn() != null) || !EmuUtil.isFileDrop( e ) )
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
    if( EmuUtil.isFileDrop( e ) ) {
      File file = EmuUtil.fileDrop( this, e );
      if( file != null ) {
	if( file.isFile() ) {
	  openFile( file, null, 0 );
	  done = true;
	}
      }
    }
    if( done ) {
      e.dropComplete( true );
    } else {
      e.rejectDrop();
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( (this.emuThread.getTapeIn() != null) || !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean doAction( ActionEvent e )
  {
    boolean rv  = true;
    Object  src = e.getSource();
    if( (src == this.btnChannel0) || (src == this.btnChannel1) ) {
      updChannel();
    } else if( src == this.btnMonitor ) {
      if( this.btnMonitor.isSelected() ) {
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
    AudioIn audioIn = this.emuThread.getTapeIn();
    this.emuThread.setTapeIn( null );
    if( this.maxSpeedTriggered ) {
      setMaxSpeed( false );
    }
    if( audioIn != null ) {
      audioIn.stopAudio();
      updFieldsEnabled();
    }
  }


  @Override
  protected void doEnable()
  {
    try {
      if( this.btnFromLine.isSelected() ) {
	enableLine();
      } else if( this.btnFromFile.isSelected() ) {
	File file = EmuUtil.showFileOpenDlg(
			this.audioFrm,
			"Sound- oder Tape-Datei \u00F6ffnen",
			Main.getLastDirFile( Main.FILE_GROUP_AUDIO ),
			AudioFile.getFileFilter(),
			EmuUtil.getTapeFileFilter() );
	if( file != null ) {
	  Main.setLastFile( file, Main.FILE_GROUP_AUDIO );
	  enableFile( file, null, 0 );
	}
      } else if( this.btnFromLastFile.isSelected() ) {
	File file = this.fldFile.getFile();
	if( file != null ) {
	  enableFile( file, null, 0 );
	}
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
  protected void updFieldsEnabled()
  {
    boolean supported  = false;
    boolean running    = false;
    boolean fromFile   = false;
    boolean fromLine   = false;
    boolean pause      = false;
    boolean progress   = false;
    boolean stereo     = false;
    String  formatText = null;
    File    file       = this.fldFile.getFile();
    EmuSys  emuSys     = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      supported = emuSys.supportsTapeIn();
    }
    AudioIn audioIn = this.emuThread.getTapeIn();
    if( audioIn != null ) {
      running  = true;
      fromLine = (audioIn instanceof AudioInLine);
      fromFile = (audioIn instanceof AudioInFile);
      pause    = audioIn.isPause();
      progress = audioIn.isProgressUpdateEnabled();
      stereo   = (audioIn.getChannels() > 1);
    } else {
      fromLine = this.btnFromLine.isSelected();
      fromFile = this.btnFromFile.isSelected()
			|| this.btnFromLastFile.isSelected();
    }
    this.btnEnable.setEnabled( supported && !running );
    this.btnDisable.setEnabled( running );
    this.btnFilePlay.setEnabled( running && fromFile && pause );
    this.btnFilePause.setEnabled( running && fromFile && !pause );
    this.btnMaxSpeed.setEnabled( running && fromFile && !pause );
    this.btnFromLine.setEnabled( supported && !running );
    this.btnFromFile.setEnabled( supported && !running );
    this.btnFromLastFile.setEnabled(
				supported && !running && (file != null) );
    if( this.btnFromLastFile.isSelected() && (file == null) ) {
      this.btnFromFile.setSelected( true );
    }
    this.labelMixer.setEnabled( supported && !running && fromLine );
    this.comboMixer.setEnabled( supported && !running && fromLine );
    this.labelFrameRate.setEnabled( supported && !running && fromLine );
    this.comboFrameRate.setEnabled( supported && !running && fromLine );
    this.labelChannel.setEnabled( supported && (!running || stereo) );
    this.btnChannel0.setEnabled( supported && (!running || stereo) );
    this.btnChannel1.setEnabled( supported && (!running || stereo) );
    this.btnMonitor.setEnabled( supported );
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
  }


	/* --- private Methoden --- */

  private void doMaxSpeed()
  {
    if( (this.emuThread.getTapeIn() != null)
	&& this.emuThread.getZ80CPU().isBrakeEnabled() )
    {
      this.maxSpeedTriggered = true;
      setMaxSpeed( true );
    }
  }


  private void doPlay()
  {
    AudioIn audioIn = this.emuThread.getTapeIn();
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
    AudioIn audioIn = this.emuThread.getTapeIn();
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


  private void enableFile(
			File   file,
			byte[] fileBytes,
			int    offs ) throws IOException
  {
    EmuSys emuSys = this.emuThread.getEmuSys();
    if( (emuSys != null) && (this.emuThread.getTapeIn() == null) ) {
      if( emuSys.supportsTapeIn() ) {
	int speedKHz = this.audioFrm.getAndCheckSpeed();
	if( speedKHz > 0 ) {
	  enableFile( speedKHz, file, fileBytes, offs );
	}
      }
    }
  }


  private void enableFile(
			int    speedKHz,
			File   file,
			byte[] fileBytes,
			int    offs ) throws IOException
  {
    AudioInFile audioInFile = new AudioInFile(
					this,
					this.emuThread.getZ80CPU(),
					speedKHz,
					file,
					fileBytes,
					offs );
    this.fldFormat.setText( audioInFile.getFormatText() );
    this.fldFile.setFile( file );
    this.emuThread.setTapeIn( audioInFile );
    updChannel();
    updFieldsEnabled();
    updMonitorEnabled();
  }


  private void enableLine() throws IOException
  {
    EmuSys emuSys = this.emuThread.getEmuSys();
    if( (emuSys != null) && (this.emuThread.getTapeIn() == null) ) {
      if( emuSys.supportsTapeIn() ) {
	int speedKHz = this.audioFrm.getAndCheckSpeed();
	if( speedKHz > 0 ) {
	  AudioIn audioIn = new AudioInLine(
		this,
		this.emuThread.getZ80CPU(),
		speedKHz,
		this.audioFrm.getSelectedFrameRate( this.comboFrameRate ),
		this.audioFrm.getSelectedMixer( this.comboMixer ) );
	  this.fldFormat.setText( audioIn.getFormatText() );
	  this.emuThread.setTapeIn( audioIn );
	  updChannel();
	  updFieldsEnabled();
	}
      }
    }
  }


  private void setMaxSpeed( boolean state )
  {
    if( state ) {
      AudioIn audioIn = this.emuThread.getTapeIn();
      if( audioIn != null ) {
	if( audioIn instanceof AudioInFile ) {
	  boolean monitorState = this.btnMonitor.isSelected();
	  if( monitorState ) {
	    this.btnMonitor.setSelected( false );
	    updMonitorEnabled();
	  }
	  if( this.audioFrm.getScreenFrm().setMaxSpeed( this, true ) ) {
	    this.btnMonitor.setEnabled( false );
	    this.btnMaxSpeed.setEnabled( false );
	  } else {
	    if( monitorState ) {
	      this.btnMonitor.setSelected( true );
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


  private void updChannel()
  {
    AudioIn audioIn  = this.emuThread.getTapeIn();
    if( audioIn != null ) {
      int channel = 0;
      if( this.btnChannel1.isSelected() ) {
	channel = 1;
      }
      if( channel >= audioIn.getChannels() ) {
	channel = 0;
	this.btnChannel0.setSelected( true );
      }
      audioIn.setSelectedChannel( channel );
    }
  }


  private void updMonitorEnabled()
  {
    boolean curState = false;
    boolean btnState = this.btnMonitor.isSelected();
    AudioIn audioIn  = this.emuThread.getTapeIn();
    if( audioIn != null ) {
      boolean err = false;
      curState    = audioIn.isMonitorActive();
      if( curState != btnState ) {
	if( btnState ) {
	  audioIn.openMonitorLine();
	  curState = audioIn.isMonitorActive();
	  if( !curState ) {
	    err = true;
	  }
	} else {
	  audioIn.closeMonitorLine();
	}
      } else {
	curState = audioIn.isMonitorActive();
      }
      if( !curState && (curState != btnState) ) {
	this.btnMonitor.setSelected( false );
      }
      if( err ) {
	BaseDlg.showErrorDlg(
		this,
		"Das Mith\u00F6ren ist nicht m\u00F6glich,\n"
			+ "da das \u00D6ffnen eines Audiokanals"
			+ " mit dem Format\n"
			+ "der Tape- bzw. Sound-Datei fehlgeschlagen ist." );
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
}
