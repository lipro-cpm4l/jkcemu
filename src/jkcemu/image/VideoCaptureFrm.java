/*
 * (c) 2010-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Aufnehmen eines Bildschirmvideos
 */

package jkcemu.image;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import jkcemu.Main;
import jkcemu.base.*;


public class VideoCaptureFrm extends BasicFrm implements Runnable
{
  private static final String DEFAULT_STATUS_TEXT = "Bereit";

  private static VideoCaptureFrm instance = null;

  private ScreenFrm          screenFrm;
  private volatile boolean   pause;
  private volatile boolean   focusedWindowOnly;
  private volatile boolean   waitForReset;
  private volatile boolean   running;
  private volatile long      recordedMillis;
  private int                waitForWindowMillis;
  private volatile int       frameMillis;
  private volatile int       captureWidth;
  private volatile int       captureHeight;
  private volatile boolean   capturing;
  private volatile Window    captureWindow;
  private boolean            fileCheckEnabled;
  private Robot              robot;
  private Thread             thread;
  private VideoPlayFrm       videoPlayFrm;
  private javax.swing.Timer  statusTimer;
  private JRadioButton       btnCaptureEmuSysScreen;
  private JRadioButton       btnCaptureScreenFrm;
  private JRadioButton       btnCaptureOtherWindow;
  private JLabel             labelWinSelectTime;
  private JLabel             labelWinSelectUnit;
  private JSpinner           spinnerWinSelectSec;
  private JLabel             labelFramesPerSec;
  private JComboBox<Integer> comboFramesPerSec;
  private JLabel             labelColorReduction;
  private JRadioButton       btnColorReductionSmooth;
  private JRadioButton       btnColorReductionHard;
  private JLabel             labelPlayCnt;
  private JRadioButton       btnPlayOnce;
  private JRadioButton       btnPlayInfinite;
  private JCheckBox          btnStartAfterReset;
  private JCheckBox          btnFocusedWindowOnly;
  private FileNameFld        fldFile;
  private JLabel             labelStatus;
  private JButton            btnFileSelect;
  private JButton            btnRecord;
  private JButton            btnPause;
  private JButton            btnStop;
  private JButton            btnPlay;
  private JButton            btnClose;


  public static void open( ScreenFrm screenFrm )
  {
    if( instance == null ) {
      instance = new VideoCaptureFrm( screenFrm );
    }
    EmuUtil.showFrame( instance );
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    this.recordedMillis = 0;
    boolean   delete    = false;
    Exception errEx     = null;
    File      file      = this.fldFile.getFile();
    if( file != null ) {
      OutputStream out = null;
      try {
	boolean smoothColorReduction = true;
	if( this.btnColorReductionSmooth != null ) {
	  smoothColorReduction = this.btnColorReductionSmooth.isSelected();
	}
	out = new BufferedOutputStream( new FileOutputStream( file ) );
	AnimatedGIFWriter animGIF = new AnimatedGIFWriter(
				out,
				smoothColorReduction,
				this.btnPlayInfinite.isSelected() );

	long begMillis = System.currentTimeMillis();
	long millis    = 0;
	try {
	  while( this.running ) {
	    millis += this.frameMillis;
	    long diffMillis = begMillis + millis - System.currentTimeMillis();
	    if( diffMillis > 0 ) {
	      Thread.sleep( diffMillis );
	    }
	    if( !this.pause && !this.waitForReset ) {
	      BufferedImage image = null;
	      if( this.focusedWindowOnly ) {
		Window window = this.captureWindow;
		if( window != null ) {
		  if( window.isFocused() ) {
		    image = createSnapshot();
		    if( !window.isFocused() ) {
		      image = null;
		    }
		  }
		}
	      } else {
		image = createSnapshot();
	      }
	      if( image != null ) {
		animGIF.addFrame( this.frameMillis, image );
		this.recordedMillis += this.frameMillis;
		this.capturing = true;
	      } else {
		this.capturing = false;
	      }
	    }
	  }
	}
	catch( InterruptedException ex ) {};
	if( !animGIF.finish() ) {
	  delete = true;
	}
        out.close();
	out = null;
      }
      catch( Exception ex ) {
	errEx = ex;
	if( out != null ) {
	  delete = true;
	}
      }
      finally {
	EmuUtil.doClose( out );
      }
      if( (file != null) && delete ) {
	file.delete();
      }
    }
    final Exception retEx = errEx;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    threadTerminated( retEx );
		  }
		} );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.btnCaptureEmuSysScreen)
	  || (src == this.btnCaptureScreenFrm)
	  || (src == this.btnCaptureOtherWindow) )
      {
	rv = true;
	updOptionFieldsEnabled();
      } else if( src == this.btnFileSelect ) {
	rv = true;
	doFileSelect();
      }
      else if( src == this.btnRecord ) {
	rv = true;
	doRecord();
      }
      else if( src == this.btnPause ) {
	rv = true;
	doPause();
      }
      else if( src == this.btnStop ) {
	rv = true;
	doStop();
      }
      else if( src == this.btnPlay ) {
	rv = true;
	doPlay();
      }
      else if( src == this.btnClose ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      setIdle();
      if( this.videoPlayFrm != null ) {
	this.videoPlayFrm.doClose();
      }
      if( this.thread != null ) {
	doStop();
	try {
	  this.thread.join( 1000 );
	}
	catch( InterruptedException ex ) {}
      }
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    pack();
  }


  @Override
  public void resetFired()
  {
    if( this.waitForReset ) {
      this.waitForReset = false;
      btnPause.setEnabled( true );
    }
  }


	/* --- Konstruktor --- */

  private VideoCaptureFrm( ScreenFrm screenFrm )
  {
    this.screenFrm           = screenFrm;
    this.pause               = false;
    this.waitForReset        = false;
    this.running             = false;
    this.capturing           = false;
    this.recordedMillis      = 0;
    this.frameMillis         = 100;
    this.waitForWindowMillis = 0;
    this.captureWidth        = 0;
    this.captureHeight       = 0;
    this.captureWindow       = null;
    this.fileCheckEnabled    = false;
    this.robot               = null;
    this.thread              = null;
    this.videoPlayFrm        = null;
    setTitle( "JKCEMU Bildschirmvideo" );
    Main.updIcon( this );

    this.statusTimer = new javax.swing.Timer(
			500,
			new ActionListener()
			{
			  @Override
			  public void actionPerformed(  ActionEvent e )
			  {
			    updStatusText();
			  }
			} );


    // Robot fuer Bildschirmabzuege anlegen
    try {
      GraphicsDevice        gd = null;
      GraphicsConfiguration gc = getGraphicsConfiguration();
      if( gc != null ) {
	gd = gc.getDevice();
      }
      if( gd != null ) {
	this.robot = new Robot( gd );
      } else {
	this.robot = new Robot();
      }
    }
    catch( AWTException ex ) {}
    catch( SecurityException ex ) {}


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    // Aufzunehmender Bereich, nur wenn Robot angelegt werden konnte
    if( this.robot != null ) {
      JPanel panelCaptureArea = new JPanel( new GridBagLayout() );
      panelCaptureArea.setBorder(
		BorderFactory.createTitledBorder( "Aufzunehmender Bereich" ) );
      add( panelCaptureArea, gbc );
      gbc.gridy++;

      GridBagConstraints gbcCaptureArea = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

      ButtonGroup grpCaptureArea = new ButtonGroup();

      this.btnCaptureEmuSysScreen = new JRadioButton(
		"Bildschirmausgabe des emulierten Systems ohne Fenster",
		true );
      grpCaptureArea.add( this.btnCaptureEmuSysScreen );
      this.btnCaptureEmuSysScreen.addActionListener( this );
      panelCaptureArea.add( this.btnCaptureEmuSysScreen, gbcCaptureArea );

      this.btnCaptureScreenFrm = new JRadioButton(
		"Bildschirmausgabe des emulierten Systems mit Fenster",
		false );
      grpCaptureArea.add( this.btnCaptureScreenFrm );
      this.btnCaptureScreenFrm.addActionListener( this );
      gbcCaptureArea.insets.top = 0;
      gbcCaptureArea.gridy++;
      panelCaptureArea.add( this.btnCaptureScreenFrm, gbcCaptureArea );

      this.btnCaptureOtherWindow = new JRadioButton(
					"Beliebiges JKCEMU-Fenster",
					false );
      grpCaptureArea.add( this.btnCaptureOtherWindow );
      this.btnCaptureOtherWindow.addActionListener( this );
      gbcCaptureArea.insets.bottom = 5;
      gbcCaptureArea.gridy++;
      panelCaptureArea.add( this.btnCaptureOtherWindow, gbcCaptureArea );
    } else {
      this.btnCaptureEmuSysScreen = null;
      this.btnCaptureScreenFrm    = null;
      this.btnCaptureOtherWindow  = null;
    }


    // Optionen
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

    if( this.btnCaptureOtherWindow != null ) {
      this.labelWinSelectTime = new JLabel( "Zeit f\u00FCr Fensterauswahl:" );
      panelOpt.add( this.labelWinSelectTime, gbcOpt );

      this.spinnerWinSelectSec = new JSpinner(
				new SpinnerNumberModel( 5, 1, 9, 1 ) );
      gbcOpt.fill = GridBagConstraints.HORIZONTAL;
      gbcOpt.gridx++;
      panelOpt.add( this.spinnerWinSelectSec, gbcOpt );

      this.labelWinSelectUnit = new JLabel( "Sekunden" );
      gbcOpt.gridx++;
      panelOpt.add( this.labelWinSelectUnit, gbcOpt );

      gbcOpt.fill  = GridBagConstraints.NONE;
      gbcOpt.gridx = 0;
      gbcOpt.gridy++;
    } else {
      this.labelWinSelectTime  = null;
      this.spinnerWinSelectSec = null;
      this.labelWinSelectUnit  = null;
    }

    this.labelFramesPerSec = new JLabel( "Bilder pro Sekunde:" );
    panelOpt.add( this.labelFramesPerSec, gbcOpt );

    Integer defaultFramesPerSec = new Integer( 10 );
    this.comboFramesPerSec      = new JComboBox<>();
    this.comboFramesPerSec.setEditable( false );
    this.comboFramesPerSec.addItem( new Integer( 5 ) );
    this.comboFramesPerSec.addItem( new Integer( 7 ) );
    this.comboFramesPerSec.addItem( defaultFramesPerSec );
    this.comboFramesPerSec.addItem( new Integer( 15 ) );
    this.comboFramesPerSec.addItem( new Integer( 20 ) );
    this.comboFramesPerSec.addItem( new Integer( 25 )  );
    this.comboFramesPerSec.addItem( new Integer( 30 )  );
    this.comboFramesPerSec.addItem( new Integer( 50 ) );
    this.comboFramesPerSec.setSelectedItem( defaultFramesPerSec );
    gbcOpt.fill = GridBagConstraints.HORIZONTAL;
    gbcOpt.gridx++;
    panelOpt.add( this.comboFramesPerSec, gbcOpt );

    if( this.robot != null ) {
      this.labelColorReduction = new JLabel( "Farbanpassung:" );
      gbcOpt.fill  = GridBagConstraints.NONE;
      gbcOpt.gridx = 0;
      gbcOpt.gridy++;
      panelOpt.add( this.labelColorReduction, gbcOpt );

      ButtonGroup grpColorReduction = new ButtonGroup();

      this.btnColorReductionSmooth = new JRadioButton( "weich", true );
      grpColorReduction.add( this.btnColorReductionSmooth );
      gbcOpt.gridx++;
      panelOpt.add( this.btnColorReductionSmooth, gbcOpt );

      this.btnColorReductionHard = new JRadioButton( "hart", false );
      grpColorReduction.add( this.btnColorReductionHard );
      gbcOpt.gridx++;
      panelOpt.add( this.btnColorReductionHard, gbcOpt );

      gbcOpt.insets.top = 0;
    } else {
      this.labelColorReduction     = null;
      this.btnColorReductionSmooth = null;
      this.btnColorReductionHard   = null;
    }

    this.labelPlayCnt = new JLabel( "Wiedergabe:" );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridwidth     = 1;
    gbcOpt.gridx         = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelPlayCnt, gbcOpt );

    ButtonGroup grpPlayCnt = new ButtonGroup();

    this.btnPlayOnce = new JRadioButton( "einmal", false );
    grpPlayCnt.add( this.btnPlayOnce );
    gbcOpt.gridx++;
    panelOpt.add( this.btnPlayOnce, gbcOpt );

    this.btnPlayInfinite = new JRadioButton(
					"st\u00E4ndig wiederholen",
					true );
    grpPlayCnt.add( this.btnPlayInfinite );
    gbcOpt.gridx++;
    panelOpt.add( this.btnPlayInfinite, gbcOpt );

    this.btnStartAfterReset = new JCheckBox(
					"Aufnahme erst nach RESET starten",
					false );
    gbcOpt.insets.bottom = (this.robot != null ? 0: 5);
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridx         = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.btnStartAfterReset, gbcOpt );

    if( this.robot != null ) {
      this.btnFocusedWindowOnly = new JCheckBox(
		"Automatische Pause bei inaktiven Fenster",
		false );
      gbcOpt.insets.bottom = 5;
      gbcOpt.insets.top    = 0;
      gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
      gbcOpt.gridx         = 0;
      gbcOpt.gridy++;
      panelOpt.add( this.btnFocusedWindowOnly, gbcOpt );
    } else {
      this.btnFocusedWindowOnly = null;
    }


    // Ausgabedatei
    JPanel panelFile = new JPanel( new GridBagLayout() );
    panelFile.setBorder(
		BorderFactory.createTitledBorder( "Ausgabedatei" ) );
    gbc.gridy++;
    add( panelFile, gbc );

    GridBagConstraints gbcFile = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.fldFile = new FileNameFld();
    this.fldFile.setText( "--- Bitte ausw\u00E4hlen ---" );
    panelFile.add( this.fldFile, gbcFile );

    this.btnFileSelect = createImageButton(
				"/images/file/open.png",
				"Ausw\u00E4hlen" );
    gbcFile.fill    = GridBagConstraints.NONE;
    gbcFile.weightx = 0.0;
    gbcFile.gridx++;
    panelFile.add( this.btnFileSelect, gbcFile );


    // Statuszeile
    gbc.insets.left  = 0;
    gbc.insets.right = 0;
    gbc.gridwidth    = GridBagConstraints.REMAINDER;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.labelStatus = new JLabel( DEFAULT_STATUS_TEXT );
    gbc.anchor       = GridBagConstraints.WEST;
    gbc.fill         = GridBagConstraints.NONE;
    gbc.weightx      = 0.0;
    gbc.insets.top   = 0;
    gbc.insets.left  = 5;
    gbc.insets.right = 5;
    gbc.gridy++;
    add( this.labelStatus, gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 5, 1, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.NORTHEAST;
    gbc.insets.top  = 5;
    gbc.gridwidth   = 1;
    gbc.gridheight  = (this.robot != null ? 3 : 2);
    gbc.gridy       = 0;
    gbc.gridx++;
    add( panelBtn, gbc );

    this.btnRecord = new JButton( "Aufnehmen" );
    this.btnRecord.setEnabled( false );
    this.btnRecord.addActionListener( this );
    panelBtn.add( btnRecord );

    this.btnPause = new JButton( "Pause" );
    this.btnPause.setEnabled( false );
    this.btnPause.addActionListener( this );
    panelBtn.add( btnPause );

    this.btnStop = new JButton( "Stopp" );
    this.btnStop.setEnabled( false );
    this.btnStop.addActionListener( this );
    panelBtn.add( btnStop );

    this.btnPlay = new JButton( "Wiedergabe" );
    this.btnPlay.setEnabled( false );
    this.btnPlay.addActionListener( this );
    panelBtn.add( btnPlay );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    panelBtn.add( btnClose );


    // Vorbelegung
    FileSystemView fsv = FileSystemView.getFileSystemView();
    if( fsv != null ) {
      File homeDir = fsv.getHomeDirectory();
      if( homeDir != null ) {
	this.fldFile.setFile( new File( homeDir, "jkcemu.gif" ) );
	this.fileCheckEnabled = true;
	this.btnRecord.setEnabled( true );
      }
    }


    // sonstiges
    updOptionFieldsEnabled();
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );
  }


	/* --- private Methoden --- */

  private void appendRecordedTimeText( StringBuilder buf )
  {
    int seconds = (int) (this.recordedMillis / 1000);
    int hours   = seconds / 3600;
    int minutes = seconds / 60;
    seconds %= 60;
    if( hours > 0 ) {
      buf.append( String.format( "%d:%02d:%02d", hours, minutes, seconds ) );
      buf.append( " Stunden" );
    } else {
      if( minutes > 0 ) {
	buf.append( String.format( "%d:%02d", minutes, seconds ) );
	buf.append( " Minuten" );
      } else {
	if( seconds == 1 ) {
	  buf.append( "1 Sekunde" );
	} else {
	  buf.append( seconds );
	  buf.append( " Sekunden" );
	}
      }
    }
    buf.append( " aufgenommen" );
  }


  private BufferedImage createSnapshot()
  {
    BufferedImage image  = null;
    Window        window = this.captureWindow;
    if( (window != null) && (this.robot != null) ) {
      int x = window.getX();
      int y = window.getY();
      int w = this.captureWidth;
      int h = this.captureHeight;
      if( (w < 1) || (h < 1) ) {
	w = window.getWidth();
	h = window.getHeight();
      }
      if( (w > 0) && (h > 0) ) {
	image = this.robot.createScreenCapture( new Rectangle( x, y, w, h ) );
      }
    } else {
      image = this.screenFrm.createSnapshot();
    }
    return image;
  }


  private void doFileSelect()
  {
    if( this.thread == null ) {
      File file = EmuUtil.showFileSaveDlg(
			this,
			"GIF-Datei speichern",
			this.fldFile.getFile(),
			EmuUtil.getGIFFileFilter() );
      if( file != null ) {
	if( !file.exists() || file.canWrite() ) {
	  String fileName = file.getName();
	  if( fileName != null ) {
	    if( fileName.toLowerCase().endsWith( ".gif" ) ) {
	      this.fldFile.setFile( file );
	      this.fileCheckEnabled = false;
	      this.btnRecord.setEnabled( true );
	    } else {
	      BasicDlg.showErrorDlg(
			this,
			"Das aufgenommene Bildschirmvideo kann nur in eine\n"
				+ "animierte GIF-Datei geschrieben werden.\n"
				+ "Der Dateiname muss deshalb mit \'.gif\'"
				+ " enden." );
	    }
	  }
	} else {
	  BasicDlg.showErrorDlg(
		this,
		"Datei kann nicht angelegt bzw. geschrieben werden" );
	}
      }
    }
  }


  private void doPause()
  {
    if( this.thread != null ) {
      this.btnPause.setEnabled( false );
      this.btnRecord.setEnabled( true );
      this.pause = true;
    }
  }


  private void doPlay()
  {
    if( this.videoPlayFrm != null ) {
      this.videoPlayFrm.setState( Frame.NORMAL );
    } else {
      this.videoPlayFrm = new VideoPlayFrm();
    }
    this.videoPlayFrm.setFile( this.fldFile.getFile() );
    this.videoPlayFrm.setVisible( true );
    this.videoPlayFrm.toFront();
  }


  private void doRecord()
  {
    if( this.thread != null ) {
      if( this.pause ) {
	this.btnPause.setEnabled( true );
	this.pause = false;
      }
    } else {
      File file = this.fldFile.getFile();
      if( file != null ) {
	boolean status = true;
	if( this.fileCheckEnabled && file.exists() ) {
	  if( JOptionPane.showConfirmDialog(
		this,
		"Sie \u00FCberschreiben die Datei\n" + file.getPath() + " !",
		"Warnung",
		JOptionPane.OK_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE ) != JOptionPane.OK_OPTION )
	  {
	    status = false;
	  }
	}
	if( status ) {
	  int frameMillis = 100;
	  Object o = this.comboFramesPerSec.getSelectedItem();
	  if( o != null ) {
	    if( o instanceof Integer ) {
	      frameMillis = Math.max( 1000 / ((Integer) o).intValue(), 10 );
	    }
	  }
	  if( this.videoPlayFrm != null ) {
	    this.videoPlayFrm.doClose();
	  }
	  this.frameMillis         = frameMillis;
	  this.waitForWindowMillis = 0;
	  this.waitForReset        = false;
	  this.pause               = false;
	  this.focusedWindowOnly   = false;
	  this.capturing           = false;
	  this.captureWidth        = 0;
	  this.captureHeight       = 0;
	  this.captureWindow       = null;
	  if( (this.btnCaptureEmuSysScreen != null)
	      && (this.btnCaptureScreenFrm != null)
	      && (this.btnCaptureOtherWindow != null) )
	  {
	    if( this.btnCaptureScreenFrm.isSelected() ) {
	      this.captureWindow = this.screenFrm;
	    }
	    else if( this.btnCaptureOtherWindow.isSelected() ) {
	      this.waitForWindowMillis = 5000;
	      if( this.spinnerWinSelectSec != null ) {
		o = this.spinnerWinSelectSec.getValue();
		if( o instanceof Number ) {
		  int v = ((Number) o).intValue();
		  if( (v >= 1) && (v < 10) ) {
		    this.waitForWindowMillis = (v + 1) * 1000;
		  }
		}
	      }
	    }
	    if( this.btnCaptureEmuSysScreen.isSelected()
			|| this.btnCaptureScreenFrm.isSelected() )
	    {
	      this.waitForReset = this.btnStartAfterReset.isSelected();
	    }
	    if( (this.btnCaptureScreenFrm.isSelected()
			|| this.btnCaptureOtherWindow.isSelected())
		&& (this.btnFocusedWindowOnly != null) )
	    {
	      this.focusedWindowOnly =
			this.btnFocusedWindowOnly.isSelected();
	    }
	    this.btnCaptureEmuSysScreen.setEnabled( false );
	    this.btnCaptureScreenFrm.setEnabled( false );
	    this.btnCaptureOtherWindow.setEnabled( false );
	  } else {
	    this.waitForReset = this.btnStartAfterReset.isSelected();
	  }
	  if( this.labelWinSelectTime != null ) {
	    this.labelWinSelectTime.setEnabled( false );
	  }
	  if( this.spinnerWinSelectSec != null ) {
	    this.spinnerWinSelectSec.setEnabled( false );
	  }
	  if( this.labelWinSelectUnit != null ) {
	    this.labelWinSelectUnit.setEnabled( false );
	  }
	  this.labelFramesPerSec.setEnabled( false );
	  this.comboFramesPerSec.setEnabled( false );
	  if( this.labelColorReduction != null ) {
	    this.labelColorReduction.setEnabled( false );
	  }
	  if( this.btnColorReductionSmooth != null ) {
	    this.btnColorReductionSmooth.setEnabled( false );
	  }
	  if( this.btnColorReductionHard != null ) {
	    this.btnColorReductionHard.setEnabled( false );
	  }
	  this.labelPlayCnt.setEnabled( false );
	  this.btnPlayOnce.setEnabled( false );
	  this.btnPlayInfinite.setEnabled( false );
	  this.btnStartAfterReset.setEnabled( false );
	  if( this.btnFocusedWindowOnly != null ) {
	    this.btnFocusedWindowOnly.setEnabled( false );
	  }
	  this.btnFileSelect.setEnabled( false );
	  this.btnRecord.setEnabled( false );
	  this.btnStop.setEnabled( true );
	  if( this.waitForWindowMillis <= 0 ) {
	    if( !this.waitForReset ) {
	      this.btnPause.setEnabled( true );
	    }
	    this.running = true;
	    this.thread  = new Thread(
				Main.getThreadGroup(),
				this,
				"JKCEMU screen video recorder" );
	    this.thread.start();
	  }
	  this.statusTimer.start();
	}
      }
    }
  }


  private void doStop()
  {
    if( (this.thread != null) || (this.waitForWindowMillis > 0) ) {
      this.btnPause.setEnabled( false );
      this.btnStop.setEnabled( false );
      this.running             = false;
      this.waitForWindowMillis = 0;
      Thread thread = this.thread;
      if( thread != null ) {
	thread.interrupt();
      } else {
	setIdle();
      }
    }
  }


  private void setIdle()
  {
    this.thread = null;
    this.statusTimer.stop();
    this.waitForWindowMillis = 0;
    this.waitForReset        = false;
    this.capturing           = false;
    this.captureWidth        = 0;
    this.captureHeight       = 0;
    this.captureWindow       = null;
    if( this.btnCaptureEmuSysScreen != null ) {
      this.btnCaptureEmuSysScreen.setEnabled( true );
    }
    if( this.btnCaptureScreenFrm != null ) {
      this.btnCaptureScreenFrm.setEnabled( true );
    }
    if( this.btnCaptureOtherWindow != null ) {
      this.btnCaptureOtherWindow.setEnabled( true );
    }
    this.labelFramesPerSec.setEnabled( true );
    this.comboFramesPerSec.setEnabled( true );
    if( this.labelColorReduction != null ) {
      this.labelColorReduction.setEnabled( true );
    }
    if( this.btnColorReductionSmooth != null ) {
      this.btnColorReductionSmooth.setEnabled( true );
    }
    if( this.btnColorReductionHard != null ) {
      this.btnColorReductionHard.setEnabled( true );
    }
    this.labelPlayCnt.setEnabled( true );
    this.btnPlayOnce.setEnabled( true );
    this.btnPlayInfinite.setEnabled( true );
    this.btnFileSelect.setEnabled( true );
    this.btnRecord.setEnabled( true );
    this.btnPause.setEnabled( false );
    this.btnStop.setEnabled( false );
    updOptionFieldsEnabled();
    this.labelStatus.setText( DEFAULT_STATUS_TEXT );
  }


  private void threadTerminated( Exception ex )
  {
    if( ex != null ) {
      if( ex instanceof IOException ) {
	BasicDlg.showErrorDlg( this, ex );
      } else {
	EmuUtil.exitSysError(
			this,
			"Aufnehmen des Bildschirmvideo fehlgeschlagen",
			ex );
      }
      this.btnPlay.setEnabled( false );
    } else {
      boolean state = false;
      File    file  = this.fldFile.getFile();
      if( file != null ) {
	state = file.isFile() && file.canRead();
      }
      this.btnPlay.setEnabled( state );
      this.fileCheckEnabled = true;
    }
    setIdle();
  }


  private void updOptionFieldsEnabled()
  {
    boolean stateEmuSys      = true;
    boolean stateScreenFrm   = true;
    boolean stateOtherWindow = false;
    if( this.btnCaptureEmuSysScreen != null ) {
      stateEmuSys = this.btnCaptureEmuSysScreen.isSelected();
    }
    if( this.btnCaptureScreenFrm != null ) {
      stateScreenFrm = this.btnCaptureScreenFrm.isSelected();
    }
    if( this.btnCaptureOtherWindow != null ) {
      stateOtherWindow = this.btnCaptureOtherWindow.isSelected();
    }
    if( this.labelWinSelectTime != null ) {
      this.labelWinSelectTime.setEnabled( stateOtherWindow );
    }
    if( this.spinnerWinSelectSec != null ) {
      this.spinnerWinSelectSec.setEnabled( stateOtherWindow );
    }
    if( this.labelWinSelectUnit != null ) {
      this.labelWinSelectUnit.setEnabled( stateOtherWindow );
    }
    if( this.labelColorReduction != null ) {
      this.labelColorReduction.setEnabled(
				stateScreenFrm || stateOtherWindow );
    }
    if( this.btnColorReductionSmooth != null ) {
      this.btnColorReductionSmooth.setEnabled(
				stateScreenFrm || stateOtherWindow );
    }
    if( this.btnColorReductionHard != null ) {
      this.btnColorReductionHard.setEnabled(
				stateScreenFrm || stateOtherWindow );
    }
    if( this.btnStartAfterReset != null ) {
      this.btnStartAfterReset.setEnabled( stateEmuSys || stateScreenFrm );
    }
    if( this.btnFocusedWindowOnly != null ) {
      this.btnFocusedWindowOnly.setEnabled(
				stateScreenFrm || stateOtherWindow );
    }
  }


  private void updStatusText()
  {
    String text = DEFAULT_STATUS_TEXT;
    if( this.thread != null ) {
      if( this.waitForReset ) {
	text = "Warte auf RESET...";
      } else if( this.pause ) {
	StringBuilder buf = new StringBuilder( 64 );
	appendRecordedTimeText( buf );
	buf.append( ", Pause" );
	text = buf.toString();
      } else {
	StringBuilder buf = new StringBuilder( 64 );
	appendRecordedTimeText( buf );
	if( this.focusedWindowOnly && !this.capturing ) {
	  buf.append( ", automatische Pause (Fenster inaktiv)" );
	} else {
	  buf.append( ", Aufnahme l\u00E4uft..." );
	}
	text = buf.toString();
      }
    } else {
      if( this.waitForWindowMillis > 0 ) {
	text = "Aufnahme startet jetzt...";
	this.waitForWindowMillis -= 500;
	if( this.waitForWindowMillis > 0 ) {
	  int seconds = this.waitForWindowMillis / 1000;
	  if( seconds == 1 ) {
	    text = "Aufzunehmendes Fenster aktivieren! Noch 1 Sekunde...";
	  } else if( seconds > 1 ) {
	    text = String.format(
			"Aufzunehmendes Fenster aktivieren!"
				+ " Noch %d Sekunden...",
			seconds );
	  }
	} else {
	  Window[] windows = Window.getWindows();
	  if( windows != null ) {
	    for( Window window : windows ) {
	      if( window.isFocused() ) {
		this.captureWindow = window;
	      }
	    }
	  }
	  if( this.captureWindow != null ) {
	    this.btnPause.setEnabled( true );
	    this.waitForWindowMillis = 0;
	    this.running = true;
	    this.thread  = new Thread( this, "JKCEMU screen video recorder" );
	    this.thread.start();
	  } else {
	    setIdle();
	    BasicDlg.showErrorDlg( this, "Kein JKCEMU-Fenster aktiv" );
	    text = DEFAULT_STATUS_TEXT;
	  }
	}
      }
    }
    this.labelStatus.setText( text );
  }
}

