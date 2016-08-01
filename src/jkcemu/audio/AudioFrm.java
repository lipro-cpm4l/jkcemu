/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer die Audiofunktionen zur Emulation
 * des Kassettenrecortderanschlusses bzw. des Tonausgangs
 */

package jkcemu.audio;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.Main;
import jkcemu.base.*;
import z80emu.*;


public class AudioFrm
		extends AbstractAudioFrm
		implements
			DropTargetListener,
			Z80MaxSpeedListener
{
  private static AudioFrm instance = null;

  private ScreenFrm         screenFrm;
  private EmuThread         emuThread;
  private Z80CPU            z80cpu;
  private AudioFormat       audioFmt;
  private AudioIO           audioIO;
  private File              curFile;
  private File              lastFile;
  private boolean           maxSpeedTriggered;
  private volatile int      usedKHz;
  private boolean           blinkState;
  private Color             blinkColor0;
  private Color             blinkColor1;
  private javax.swing.Timer blinkTimer;
  private javax.swing.Timer disableTimer;
  private JRadioButton      btnSoundOut;
  private JRadioButton      btnDataOut;
  private JRadioButton      btnDataIn;
  private JRadioButton      btnFileOut;
  private JRadioButton      btnFileIn;
  private JRadioButton      btnLastFileIn;
  private JLabel            labelChannel;
  private JRadioButton      btnChannel0;
  private JRadioButton      btnChannel1;
  private JCheckBox         btnMonitor;
  private JLabel            labelFileName;
  private FileNameFld       fileNameFld;
  private JLabel            labelFormat;
  private JTextField        fldFormat;
  private JLabel            labelProgress;
  private JProgressBar      progressBar;
  private JPanel            panelVolume;
  private JButton           btnEnable;
  private JButton           btnDisable;
  private JButton           btnHelp;
  private JButton           btnClose;
  private JButton           btnMaxSpeed;
  private JButton           btnPlay;
  private JButton           btnPause;


  public static AudioFrm open( ScreenFrm screenFrm )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new AudioFrm( screenFrm );
    }
    instance.toFront();
    instance.setVisible( true );
    return instance;
  }


  public void fireFinished()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    audioFinished();
		  }
		} );
  }


  public void fireProgressUpdate( final float value )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updProgressBar( value );
		  }
		} );
  }


  public void fireReopenLine()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    reopenLine();
		  }
		} );
  }


  public EmuThread getEmuThread()
  {
    return this.emuThread;
  }


  public void openFile( File file, byte[] fileBytes, int offs )
  {
    stopAudio();
    if( checkSpeed() ) {
      this.btnFileIn.setSelected( true );
      updOptFields();
      enableAudioInFile( this.usedKHz, file, fileBytes, offs );
    }
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( (this.audioIO != null) || !EmuUtil.isFileDrop( e ) )
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
    if( (this.audioIO == null) && EmuUtil.isFileDrop( e ) ) {
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
    if( (this.audioIO != null) || !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- Z80MaxSpeedListener --- */

  /*
   * Wenn sich die Emulationsgeschwindigkeit aendert,
   * stimmt die Synchronisation mit dem Audiosystem nicht mehr.
   */
  @Override
  public void z80MaxSpeedChanged( Z80CPU cpu )
  {
    if( this.z80cpu.getMaxSpeedKHz() != this.usedKHz )
      fireReopenLine();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    stopBlinking();
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.btnSoundOut)
	  || (src == this.btnDataOut)
	  || (src == this.btnDataIn)
	  || (src == this.btnFileOut)
	  || (src == this.btnFileIn)
	  || (src == this.btnLastFileIn) )
      {
	rv = true;
	updOptFields();
      }
      else if( (src == this.btnChannel0) || (src == this.btnChannel1) ) {
	rv = true;
	updChannel();
      }
      else if( src == this.btnMonitor ) {
	rv = true;
	if( this.btnMonitor.isSelected() ) {
	  setMaxSpeed( false );
	}
	updMonitorEnabled();
      }
      else if( src == this.btnEnable ) {
	rv = true;
	doEnable();
      }
      else if( src == this.btnDisable ) {
	rv = true;
	doDisable();
      }
      else if( src == this.btnPlay ) {
	rv = true;
	doPlay();
      }
      else if( src == this.btnPause ) {
	rv = true;
	doPause();
      }
      else if( src == this.btnMaxSpeed ) {
	rv = true;
	doMaxSpeed();
      }
      else if( src == this.btnHelp ) {
	rv = true;
	HelpFrm.open( "/help/audio.htm" );
      }
      else if( src == this.btnClose ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }


  @Override
  public boolean doQuit()
  {
    stopBlinking();
    doDisable();
    return doClose();
  }


	/* --- Konstruktor --- */

  private AudioFrm( ScreenFrm screenFrm )
  {
    this.screenFrm         = screenFrm;
    this.emuThread         = screenFrm.getEmuThread();
    this.z80cpu            = this.emuThread.getZ80CPU();
    this.mixers            = AudioSystem.getMixerInfo();
    this.audioFmt          = null;
    this.audioIO           = null;
    this.curFile           = null;
    this.lastFile          = null;
    this.maxSpeedTriggered = false;
    this.usedKHz           = -1;
    setTitle( "JKCEMU Audio/Kassette" );


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
	"T\u00F6ne ausgeben (Emulation des Lautsprechers/Sound-Generators)",
	true );
    grpFct.add( this.btnSoundOut );
    this.btnSoundOut.addActionListener( this );
    panelFct.add( this.btnSoundOut, gbcFct );

    this.btnDataOut = new JRadioButton( "Daten am Audioausgang ausgeben" );
    grpFct.add( this.btnDataOut );
    this.btnDataOut.addActionListener( this );
    gbcFct.insets.top = 0;
    gbcFct.gridy++;
    panelFct.add( this.btnDataOut, gbcFct );

    this.btnDataIn = new JRadioButton( "Daten vom Audioeingang lesen" );
    grpFct.add( this.btnDataIn );
    this.btnDataIn.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnDataIn, gbcFct );

    this.btnFileOut = new JRadioButton( "Sound- oder Tape-Datei speichern" );
    grpFct.add( this.btnFileOut );
    this.btnFileOut.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnFileOut, gbcFct );

    this.btnFileIn = new JRadioButton( "Sound- oder Tape-Datei lesen" );
    grpFct.add( this.btnFileIn );
    this.btnFileIn.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnFileIn, gbcFct );

    this.btnLastFileIn = new JRadioButton(
			"Letzte Sound-/Tape-Datei (noch einmal) lesen" );
    grpFct.add( this.btnLastFileIn );
    this.btnLastFileIn.addActionListener( this );
    gbcFct.insets.bottom = 5;
    gbcFct.gridy++;
    panelFct.add( this.btnLastFileIn, gbcFct );

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

    panelOpt.add( this.labelMixer, gbcOpt );

    gbcOpt.gridwidth = GridBagConstraints.REMAINDER;
    gbcOpt.gridx++;
    panelOpt.add( this.comboMixer, gbcOpt );

    gbcOpt.insets.top = 2;
    gbcOpt.gridwidth  = 1;
    gbcOpt.gridx      = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelSampleRate, gbcOpt );

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

    this.btnMonitor = new JCheckBox( "Mith\u00F6ren", false );
    this.btnMonitor.addActionListener( this );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridx         = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.btnMonitor, gbcOpt );

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

    this.fileNameFld = new FileNameFld();
    this.fileNameFld.setEditable( false );
    gbcStatus.anchor  = GridBagConstraints.WEST;
    gbcStatus.fill    = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx = 1.0;
    gbcStatus.gridy   = 0;
    gbcStatus.gridx++;
    panelStatus.add( this.fileNameFld, gbcStatus );

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


    // linker Bereich
    JPanel panelLeft = new JPanel( new GridBagLayout() );
    gbc.fill       = GridBagConstraints.VERTICAL;
    gbc.weighty    = 1.0;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.gridy      = 0;
    gbc.gridx++;
    add( panelLeft, gbc );

    GridBagConstraints gbcLeft = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.btnEnable = new JButton( "Aktivieren" );
    this.btnEnable.setToolTipText( "Audiofunktion aktivieren" );
    this.btnEnable.addActionListener( this );
    panelLeft.add( this.btnEnable, gbcLeft );

    this.btnDisable = new JButton( "Deaktivieren" );
    this.btnDisable.setToolTipText( "Audiofunktion deaktivieren" );
    this.btnDisable.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnDisable, gbcLeft );

    this.btnPlay = new JButton( "Abspielen" );
    this.btnPlay.setToolTipText( "Abspielen starten" );
    this.btnPlay.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnPlay, gbcLeft );

    this.btnPause = new JButton( "Pause" );
    this.btnPause.setToolTipText( "Abspielen anhalten" );
    this.btnPause.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnPause, gbcLeft );

    this.btnMaxSpeed = new JButton( "Turbo" );
    this.btnMaxSpeed.setToolTipText( "Auf max. Geschwindigkeit schalten" );
    this.btnMaxSpeed.setEnabled( false );
    this.btnMaxSpeed.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnMaxSpeed, gbcLeft );

    this.btnHelp = new JButton( "Hilfe" );
    this.btnHelp.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnHelp, gbcLeft );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    gbcLeft.gridy++;
    panelLeft.add( this.btnClose, gbcLeft );


    // Pegelanzeige
    this.panelVolume = new JPanel( new GridBagLayout() );
    this.panelVolume.setBorder( BorderFactory.createTitledBorder( "Pegel" ) );
    gbcLeft.fill       = GridBagConstraints.BOTH;
    gbcLeft.weighty    = 1.0;
    gbcLeft.insets.top = 30;
    gbcLeft.gridy++;
    panelLeft.add( panelVolume, gbcLeft );

    GridBagConstraints gbcVolume = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.VERTICAL,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );
    this.panelVolume.add( this.volumeBar, gbcVolume );

    // Blinken
    this.blinkState  = false;
    this.blinkColor0 = this.btnPlay.getForeground();
    this.blinkColor1 = new Color( 0, 180, 0 );
    this.blinkTimer  = new javax.swing.Timer(
			800,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    changeBlinkState();
			  }
			} );

    /*
     * Timer, der nach dem Ende einer eingelesenen Datei
     * die Audiofunktion deaktiviert
     */
    this.disableTimer = new javax.swing.Timer(
			10000,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    doDisable();
			  }
			} );

    // Initialzustand
    setVolumeLimits( 0, 255 );
    setAudioState( false, null );

    // sonstiges
    pack();
    if( !applySettings( Main.getProperties(), false ) ) {
      setScreenCentered();
    }
    setResizable( false );
    this.z80cpu.addMaxSpeedListener( this );
    (new DropTarget( this.fileNameFld, this )).setActive( true );
  }


	/* --- private Methoden --- */

  private void audioFinished()
  {
    File   file      = null;
    byte[] fileBytes = null;
    int    speedKHz  = 0;
    if( (this.audioFmt != null) && (this.audioIO != null) ) {
      if( this.audioIO instanceof AudioInFile ) {
	// Datei noch einmal oeffnen
	speedKHz  = ((AudioInFile) this.audioIO).getSpeedKHz();
	file      = ((AudioInFile) this.audioIO).getFile();
	fileBytes = ((AudioInFile) this.audioIO).getFileBytes();
      }
    }
    doDisable();
    if( (speedKHz > 0) && (file != null) ) {
      if( enableAudioInFileInternal( speedKHz, file, fileBytes, 0 ) ) {
	this.disableTimer.start();
      }
    }
  }


  private void changeBlinkState()
  {
    if( this.btnPlay.isEnabled()
	&& (this.blinkColor0 != null)
	&& (this.blinkColor1 != null) )
    {
      if( this.blinkState ) {
	this.btnPlay.setForeground( this.blinkColor0 );
      } else {
	this.btnPlay.setForeground( this.blinkColor1 );
      }
      this.blinkState = !this.blinkState;
    }
  }


  private boolean checkSpeed()
  {
    boolean rv  = false;
    int     khz = this.z80cpu.getMaxSpeedKHz();
    if( khz > 0 ) {
      this.usedKHz = khz;
      rv           = true;
    } else {
      BasicDlg.showErrorDlg(
		this,
		"Sie m\u00Fcssen die Geschwindigkeit des Emulators\n"
			+ "auf einen konkreten Wert begrenzen, da dieser\n"
			+ "als Zeitbasis f\u00FCr das AudioSystem dient.\n" );
    }
    return rv;
  }


  private void doEnable()
  {
    if( checkSpeed() ) {
      if( this.btnSoundOut.isSelected() || this.btnDataOut.isSelected() ) {
	doEnableAudioOutLine( this.usedKHz, this.btnDataOut.isSelected() );
      }
      else if( this.btnDataIn.isSelected() ) {
	doEnableAudioInLine( this.usedKHz );
      }
      else if( this.btnFileOut.isSelected() ) {
	doEnableAudioOutFile( this.usedKHz );
      }
      else if( this.btnFileIn.isSelected() ) {
	doEnableAudioInFile( this.usedKHz, null );
      }
      else if( this.btnLastFileIn.isSelected() ) {
	doEnableAudioInFile( this.usedKHz, this.lastFile );
      }
    }
  }


  private void doEnableAudioInFile( int speedKHz, File file )
  {
    if( file == null ) {
      file = EmuUtil.showFileOpenDlg(
				this,
				"Sound- oder Tape-Datei \u00F6ffnen",
				Main.getLastDirFile( "audio" ),
				AudioUtil.getAudioInFileFilter(),
				EmuUtil.getTapeFileFilter() );
    }
    if( file != null ) {
      enableAudioInFile( speedKHz, file, null, 0 );
    }
  }


  private void doEnableAudioInLine( int speedKHz )
  {
    stopAudio();
    AudioInLine audioInLine = new AudioInLine( this, this.z80cpu );
    this.audioFmt           = audioInLine.startAudio(
					getSelectedMixer(),
					speedKHz,
					getSelectedSampleRate() );
    if( this.audioFmt != null ) {
      this.audioIO = audioInLine;
      updChannel();
      this.emuThread.setAudioIn( audioInLine );
      setAudioState( true, null );
    } else {
      audioInLine.stopAudio();
      showError( audioInLine.getAndClearErrorText() );
    }
  }


  private void doEnableAudioOutFile( int speedKHz )
  {
    File file = EmuUtil.showFileSaveDlg(
				this,
				"Sound- oder Tape-Datei speichern",
				Main.getLastDirFile( "audio" ),
				AudioUtil.getAudioOutFileFilter(),
				EmuUtil.getCswFileFilter(),
				EmuUtil.getCdtFileFilter(),
				EmuUtil.getTzxFileFilter() );
    if( file != null ) {
      AudioFileFormat.Type fileType   = null;
      boolean              csw        = false;
      boolean              tzx        = false;
      int                  sampleRate = getSelectedSampleRate();
      if( sampleRate == 0 ) {
	  sampleRate = 44100;
      }
      String fName = file.getName();
      if( fName != null ) {
	fName = fName.toUpperCase();
	csw   = fName.endsWith( ".CSW" );
	tzx   = (fName.endsWith( ".CDT" ) || fName.endsWith( ".TZX" ));
      }
      if( tzx ) {
	if( sampleRate == 0 ) {
	  sampleRate = 44100;
	}
	if( (sampleRate != 22050) && (sampleRate != 44100) ) {
	  tzx = false;
	  BasicDlg.showErrorDlg(
		this,
		"Bei dem Dateityp sind nur die Abtastfrequenzen\n"
			+ "22050 Hz und 44100 Hz m\u00F6glich." );
	}
      } else if( !csw ) {
	fileType = AudioUtil.getAudioFileType( this, file );
      }
      if( csw || tzx || (fileType != null) ) {
	stopAudio();
	AudioOutFile audioOutFile = new AudioOutFile(
					this,
					this.z80cpu,
					file,
					fileType );
	this.audioFmt = audioOutFile.startAudio(
					speedKHz,
					getSelectedSampleRate() );
	if( this.audioFmt != null ) {
	  this.audioIO   = audioOutFile;
	  this.curFile   = file;
	  this.lastFile  = file;
	  this.emuThread.setAudioOut( audioOutFile );
	  Main.setLastFile( file, "audio" );
	  if( csw || tzx ) {
	    setAudioState(
			true,
			String.format(
				"%s-Datei, %d Hz",
				csw ? "CSW" : "CDT/TZX",
				sampleRate ) );
	  } else {
	    setAudioState( true, null );
	  }
	  updMonitorEnabled();
	} else {
	  audioOutFile.stopAudio();
	  showError( audioOutFile.getAndClearErrorText() );
	}
      }
    }
  }


  private void doEnableAudioOutLine( int speedKHz, boolean forDataTransfer )
  {
    stopAudio();
    boolean soundOut = this.btnSoundOut.isSelected();
    EmuSys  emuSys   = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      AudioOutLine audioOutLine = new AudioOutLine(
						this,
						this.z80cpu,
						soundOut );
      this.audioFmt = audioOutLine.startAudio(
				getSelectedMixer(),
				speedKHz,
				getSelectedSampleRate(),
				soundOut && emuSys.supportsStereoSound() );
      if( this.audioFmt != null ) {
	this.audioIO = audioOutLine;
	this.emuThread.setAudioOut( audioOutLine );
	setAudioState( true, null );
      } else {
	audioOutLine.stopAudio();
	showError( audioOutLine.getAndClearErrorText() );
      }
    }
  }


  private void doDisable()
  {
    stopAudio();
    this.curFile  = null;
    this.audioFmt = null;
    if( this.maxSpeedTriggered ) {
      setMaxSpeed( false );
    }
    setAudioState( false, null );
  }


  private void doMaxSpeed()
  {
    if( this.z80cpu.isBrakeEnabled() ) {
      this.maxSpeedTriggered = true;
      setMaxSpeed( true );
    }
  }


  private void doPlay()
  {
    AudioIO audioIO = this.audioIO;
    if( audioIO != null ) {
      if( audioIO instanceof AudioInFile ) {
	if( this.disableTimer.isRunning() ) {
	  this.disableTimer.stop();
	}
	((AudioInFile) audioIO).setPause( false );
	this.btnPlay.setEnabled( false );
	this.btnPause.setEnabled( true );
	this.btnMaxSpeed.setEnabled( true );
      }
    }
  }


  private void doPause()
  {
    AudioIO audioIO = this.audioIO;
    if( audioIO != null ) {
      if( audioIO instanceof AudioInFile ) {
	if( this.maxSpeedTriggered ) {
	  setMaxSpeed( false );
	}
	((AudioInFile) audioIO).setPause( true );
	this.btnMonitor.setEnabled( true );
	this.btnPlay.setEnabled( true );
	this.btnPause.setEnabled( false );
	this.btnMaxSpeed.setEnabled( false );
	this.btnMonitor.setEnabled( true );
      }
    }
  }


  private void enableAudioInFile(
			int     speedKHz,
			File    file,
			byte[]  fileBytes,
			int     offs )
  {
    if( !enableAudioInFileInternal( speedKHz, file, fileBytes, offs ) ) {
      showError( this.audioIO.getAndClearErrorText() );
    }
  }


  private boolean enableAudioInFileInternal(
				int     speedKHz,
				File    file,
				byte[]  fileBytes,
				int     offs )
  {
    boolean rv = false;
    stopAudio();
    AudioInFile audioInFile = new AudioInFile(
					this,
					this.z80cpu,
					file,
					fileBytes,
					offs );
    this.audioFmt = audioInFile.startAudio( speedKHz );
    this.audioIO  = audioInFile;
    if( this.audioFmt != null ) {
      updChannel();
      this.emuThread.setAudioIn( audioInFile );
      this.curFile  = file;
      this.lastFile = file;
      Main.setLastFile( file, "audio" );
      setAudioState( true, audioInFile.getSpecialFormatText() );
      updMonitorEnabled();
      this.btnPlay.requestFocus();
      this.blinkTimer.restart();
      rv = true;
    }
    return rv;
  }


  private void reopenLine()
  {
    boolean state = (this.audioFmt != null);
    doDisable();
    if( state ) {
      int khz = this.z80cpu.getMaxSpeedKHz();
      if( khz > 0 ) {
	this.usedKHz = khz;
	if( this.btnSoundOut.isSelected()
	    || this.btnDataOut.isSelected() )
	{
	  doEnableAudioOutLine( khz, this.btnDataOut.isSelected() );
	}
	else if( this.btnDataIn.isSelected() ) {
	  doEnableAudioInLine( khz );
	}
      }
    }
  }


  private void setAudioState( boolean state, String specialFormatText )
  {
    if( state && (this.audioFmt != null) ) {
      this.labelFormat.setEnabled( true );
      if( specialFormatText != null ) {
	this.fldFormat.setText( specialFormatText );
      } else {
	this.fldFormat.setText(
		AudioUtil.getAudioFormatText( this.audioFmt ) );
      }
      setVolumeBarState( true );
    } else {
      this.labelFormat.setEnabled( false );
      this.fldFormat.setText( "" );
      setVolumeBarState( false );
    }

    if( state && (this.curFile != null) ) {
      this.labelFileName.setEnabled( true );
      this.fileNameFld.setFile( this.curFile );
    } else {
      this.labelFileName.setEnabled( false );
      this.fileNameFld.setFile( null );
    }

    boolean progressState = false;
    if( state ) {
      AudioIO audioIO = this.audioIO;
      if( audioIO != null ) {
	progressState = audioIO.isProgressUpdateEnabled();
      }
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

    boolean fileIn = false;
    boolean pause  = false;
    if( state ) {
      AudioIO audioIO = this.audioIO;
      if( audioIO != null ) {
	if( audioIO instanceof AudioInFile ) {
	  fileIn = true;
	  pause  = ((AudioInFile) audioIO).isPause();
	}
      }
    } else {
      if( this.disableTimer.isRunning() ) {
	this.disableTimer.stop();
      }
    }
    this.btnPlay.setEnabled( fileIn && pause );
    this.btnPause.setEnabled( fileIn && !pause );
    if( !state ) {
      this.btnMaxSpeed.setEnabled( false );
    }
    this.volumeBar.setEnabled( state );

    state = !state;
    this.btnEnable.setEnabled( state );
    this.btnEnable.setEnabled( state );
    this.btnSoundOut.setEnabled( state );
    this.btnDataOut.setEnabled( state );
    this.btnDataIn.setEnabled( state );
    this.btnFileOut.setEnabled( state );
    this.btnFileIn.setEnabled( state );
    this.btnLastFileIn.setEnabled( state && (this.lastFile != null) );
    updOptFields();
  }


  private void setMaxSpeed( boolean state )
  {
    if( state ) {
      AudioIO audioIO = this.audioIO;
      if( audioIO != null ) {
	if( audioIO instanceof AudioInFile ) {
	  if( this.btnMonitor.isSelected() ) {
	    this.btnMonitor.setSelected( false );
	    updMonitorEnabled();
	  }
	  this.btnMonitor.setEnabled( false );
	  this.btnMaxSpeed.setEnabled( false );
	  this.screenFrm.setMaxSpeed( true );
	}
      }
    } else {
      this.screenFrm.setMaxSpeed( false );
      this.maxSpeedTriggered = false;
    }
  }


  private void stopAudio()
  {
    AudioIO audioIO = this.audioIO;
    this.audioIO    = null;
    this.emuThread.setAudioIn( null );
    this.emuThread.setAudioOut( null );
    if( audioIO != null ) {
      audioIO.stopAudio();
      String errorText = audioIO.getAndClearErrorText();
      if( errorText != null ) {
	BasicDlg.showErrorDlg( this, errorText );
      }
    }
  }


  public void stopBlinking()
  {
    if( this.blinkTimer.isRunning() ) {
      this.blinkTimer.stop();
    }
    if( this.blinkState && (this.blinkColor0 != null) ) {
      this.btnPlay.setForeground( this.blinkColor0 );
      this.blinkState = false;
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


  private void updMonitorEnabled()
  {
    boolean curState = false;
    boolean btnState = this.btnMonitor.isSelected();
    AudioIO audioIO  = this.audioIO;
    if( audioIO != null ) {
      boolean err = false;
      curState    = audioIO.isMonitorActive();
      if( curState != btnState ) {
	if( btnState ) {
	  audioIO.openMonitorLine();
	  curState = audioIO.isMonitorActive();
	  if( !curState ) {
	    err = false;
	  }
	} else {
	  audioIO.closeMonitorLine();
	}
      } else {
	curState = audioIO.isMonitorActive();
      }
      if( !curState && (curState != btnState) ) {
	this.btnMonitor.setSelected( false );
      }
      if( err ) {
	BasicDlg.showErrorDlg(
		this,
		"Das Mith\u00F6ren ist nicht m\u00F6glich,\n"
			+ "da das \u00D6ffnen eines Audiokanals"
			+ " mit dem Format\n"
			+ "der Sound-Datei fehlgeschlagen ist." );
      }
    }
  }


  private void updOptFields()
  {
    AudioIO     audioIO  = this.audioIO;
    AudioFormat audioFmt = this.audioFmt;

    // Mixer
    boolean state = ((audioIO == null)
	&& (this.btnSoundOut.isSelected()
	    || this.btnDataOut.isSelected()
	    || this.btnDataIn.isSelected()));
    setMixerState( state );

    // Sample-Rate
    state = ((audioIO == null)
	&& (this.btnSoundOut.isSelected()
	    || this.btnDataOut.isSelected()
	    || this.btnDataIn.isSelected()
	    || this.btnFileOut.isSelected()));
    setSampleRateState( state );

    // Kanalauswahl
    state = false;
    if( (audioIO != null) && (audioFmt != null) ) {
      if( audioIO instanceof AudioIn ) {
	if( audioFmt.getChannels() > 1 ) {
	  state = true;
	}
      }
    } else {
      state = (this.btnDataIn.isSelected() || this.btnFileIn.isSelected());
    }
    this.labelChannel.setEnabled( state );
    this.btnChannel0.setEnabled( state );
    this.btnChannel1.setEnabled( state );

    // Mithoeren
    state = (this.btnFileOut.isSelected()
	    || this.btnFileIn.isSelected()
	    || this.btnLastFileIn.isSelected());
    this.btnMonitor.setEnabled( state );
  }


  private void updProgressBar( float value )
  {
    int intVal = Math.round(
	value * (float) this.progressBar.getMaximum() ) + 1;

    if( intVal < this.progressBar.getMinimum() ) {
      intVal = this.progressBar.getMinimum();
    }
    else if( intVal > this.progressBar.getMaximum() ) {
      intVal = this.progressBar.getMaximum();
    }
    this.progressBar.setValue( intVal );
  }
}

