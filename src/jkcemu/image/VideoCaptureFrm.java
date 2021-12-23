/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Aufnehmen eines Bildschirmvideos
 */

package jkcemu.image;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.ScreenFrm;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;


public class VideoCaptureFrm extends BaseFrm implements Runnable
{
  private static final String DEFAULT_FILE        = "jkcemu.gif";
  private static final String DEFAULT_STATUS_TEXT = "Bereit";
  private static final String HELP_PAGE = "/help/videocapture.htm";

  private static VideoCaptureFrm instance = null;

  private ScreenFrm          screenFrm;
  private volatile boolean   pause;
  private volatile boolean   focusedWindowOnly;
  private volatile boolean   force256Colors;
  private volatile boolean   smoothColorReduction;
  private volatile boolean   playInfinite;
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
  private String             robotMsg;
  private Thread             thread;
  private VideoPlayFrm       videoPlayFrm;
  private javax.swing.Timer  statusTimer;
  private JRadioButton       rbCaptureEmuSysScreen;
  private JRadioButton       rbCaptureScreenFrm;
  private JRadioButton       rbCaptureOtherWindow;
  private JLabel             labelWinSelectTime;
  private JLabel             labelWinSelectUnit;
  private JSpinner           spinnerWinSelectSec;
  private JLabel             labelFramesPerSec;
  private JComboBox<Integer> comboFramesPerSec;
  private JLabel             labelColorReduction;
  private JRadioButton       rbColorReductionSmooth;
  private JRadioButton       rbColorReductionHard;
  private JLabel             labelPlayCnt;
  private JRadioButton       rbPlayOnce;
  private JRadioButton       rbPlayInfinite;
  private JCheckBox          cbStartAfterReset;
  private JCheckBox          cbFocusedWindowOnly;
  private JCheckBox          cbForce256Colors;
  private FileNameFld        fldFile;
  private JLabel             labelStatus;
  private JButton            btnFileSelect;
  private JButton            btnRecord;
  private JButton            btnPause;
  private JButton            btnStop;
  private JButton            btnPlay;
  private JButton            btnDelete;
  private JButton            btnHelp;
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
	out = new BufferedOutputStream( new FileOutputStream( file ) );
	AnimatedGIFWriter animGIF = new AnimatedGIFWriter(
						out,
						this.force256Colors,
						this.smoothColorReduction,
						this.playInfinite );
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
	EmuUtil.closeSilently( out );
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
    try {
      Object  src = e.getSource();
      if( (src == this.rbCaptureEmuSysScreen)
	  || (src == this.rbCaptureScreenFrm)
	  || (src == this.rbCaptureOtherWindow)
	  || (src == this.cbForce256Colors) )
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
      else if( src == this.btnDelete ) {
	rv = true;
	doDelete();
      }
      else if( src == this.btnHelp ) {
	rv = true;
	HelpFrm.openPage( HELP_PAGE );
      }
      else if( src == this.btnClose ) {
	rv = true;
	doClose();
      }
    }
    catch( IOException ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      setIdle();
      closePlayer();
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
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    if( this.waitForReset ) {
      this.waitForReset = false;
      btnPause.setEnabled( true );
    }
  }


	/* --- Konstruktor --- */

  private VideoCaptureFrm( ScreenFrm screenFrm )
  {
    this.screenFrm            = screenFrm;
    this.pause                = false;
    this.waitForReset         = false;
    this.running              = false;
    this.capturing            = false;
    this.focusedWindowOnly    = false;
    this.force256Colors       = false;
    this.smoothColorReduction = false;
    this.playInfinite         = false;
    this.recordedMillis       = 0;
    this.frameMillis          = 100;
    this.waitForWindowMillis  = 0;
    this.captureWidth         = 0;
    this.captureHeight        = 0;
    this.captureWindow        = null;
    this.fileCheckEnabled     = false;
    this.robot                = null;
    this.robotMsg             = null;
    this.thread               = null;
    this.videoPlayFrm         = null;
    setTitle( "JKCEMU Bildschirmvideo" );

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
    catch( Exception ex ) {
      this.robotMsg = ex.getMessage();
    }

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

    JPanel panelCaptureArea = GUIFactory.createPanel( new GridBagLayout() );
    panelCaptureArea.setBorder(
		GUIFactory.createTitledBorder( "Aufzunehmender Bereich" ) );
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

    this.rbCaptureEmuSysScreen = GUIFactory.createRadioButton(
		"Bildschirmausgabe des emulierten Systems ohne Fenster",
		true );
    grpCaptureArea.add( this.rbCaptureEmuSysScreen );
    panelCaptureArea.add( this.rbCaptureEmuSysScreen, gbcCaptureArea );

    this.rbCaptureScreenFrm = GUIFactory.createRadioButton(
		"Bildschirmausgabe des emulierten Systems mit Fenster" );
    grpCaptureArea.add( this.rbCaptureScreenFrm );
    gbcCaptureArea.insets.top = 0;
    gbcCaptureArea.gridy++;
    panelCaptureArea.add( this.rbCaptureScreenFrm, gbcCaptureArea );

    this.rbCaptureOtherWindow = GUIFactory.createRadioButton(
					"Beliebiges JKCEMU-Fenster" );
    grpCaptureArea.add( this.rbCaptureOtherWindow );
    gbcCaptureArea.insets.bottom = 5;
    gbcCaptureArea.gridy++;
    panelCaptureArea.add( this.rbCaptureOtherWindow, gbcCaptureArea );


    // Optionen
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

    this.labelWinSelectTime = GUIFactory.createLabel(
					"Zeit f\u00FCr Fensterauswahl:" );
    panelOpt.add( this.labelWinSelectTime, gbcOpt );

    this.spinnerWinSelectSec = GUIFactory.createSpinner(
				new SpinnerNumberModel( 5, 1, 9, 1 ) );
    gbcOpt.fill = GridBagConstraints.HORIZONTAL;
    gbcOpt.gridx++;
    panelOpt.add( this.spinnerWinSelectSec, gbcOpt );

    this.labelWinSelectUnit = GUIFactory.createLabel( "Sekunden" );
    gbcOpt.gridx++;
    panelOpt.add( this.labelWinSelectUnit, gbcOpt );

    this.labelFramesPerSec = GUIFactory.createLabel( "Bilder pro Sekunde:" );
    gbcOpt.fill            = GridBagConstraints.NONE;
    gbcOpt.gridx           = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelFramesPerSec, gbcOpt );

    Integer defaultFramesPerSec = 10;
    this.comboFramesPerSec      = GUIFactory.createComboBox();
    this.comboFramesPerSec.setEditable( false );
    this.comboFramesPerSec.addItem( 5 );
    this.comboFramesPerSec.addItem( 7 );
    this.comboFramesPerSec.addItem( defaultFramesPerSec );
    this.comboFramesPerSec.addItem( 15 );
    this.comboFramesPerSec.addItem( 20 );
    this.comboFramesPerSec.addItem( 25 );
    this.comboFramesPerSec.addItem( 30 );
    this.comboFramesPerSec.addItem( 50 );
    this.comboFramesPerSec.setSelectedItem( defaultFramesPerSec );
    gbcOpt.fill = GridBagConstraints.HORIZONTAL;
    gbcOpt.gridx++;
    panelOpt.add( this.comboFramesPerSec, gbcOpt );

    this.cbStartAfterReset = GUIFactory.createCheckBox(
				"Aufnahme erst nach RESET starten" );
    gbcOpt.insets.top = 10;
    gbcOpt.gridwidth  = GridBagConstraints.REMAINDER;
    gbcOpt.gridx      = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.cbStartAfterReset, gbcOpt );

    this.cbFocusedWindowOnly = GUIFactory.createCheckBox(
		"Automatische Pause bei inaktivem Fenster" );
    gbcOpt.insets.top = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.cbFocusedWindowOnly, gbcOpt );

    this.cbForce256Colors = GUIFactory.createCheckBox(
		"Ausgabedatei immer mit 256 Farben erzeugen" );
    this.cbForce256Colors.setToolTipText(
		"Diese Option ist sinnvoll, wenn sich w\u00E4hrend"
			+ " der Aufnahme die Anzahl der Farben"
			+ " \u00E4ndert." );
    gbcOpt.gridy++;
    panelOpt.add( this.cbForce256Colors, gbcOpt );

    this.labelColorReduction = GUIFactory.createLabel( "Farbanpassung:" );
    gbcOpt.insets.top        = 5;
    gbcOpt.fill              = GridBagConstraints.NONE;
    gbcOpt.gridwidth         = 1;
    gbcOpt.gridx             = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelColorReduction, gbcOpt );

    ButtonGroup grpColorReduction = new ButtonGroup();

    this.rbColorReductionSmooth = GUIFactory.createRadioButton( "weich" );
    grpColorReduction.add( this.rbColorReductionSmooth );
    gbcOpt.gridx++;
    panelOpt.add( this.rbColorReductionSmooth, gbcOpt );

    this.rbColorReductionHard = GUIFactory.createRadioButton(
							"hart",
							true );
    grpColorReduction.add( this.rbColorReductionHard );
    gbcOpt.gridx++;
    panelOpt.add( this.rbColorReductionHard, gbcOpt );

    this.labelPlayCnt    = GUIFactory.createLabel( "Wiedergabe:" );
    gbcOpt.insets.top    = 0;
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridx         = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelPlayCnt, gbcOpt );

    ButtonGroup grpPlayCnt = new ButtonGroup();

    this.rbPlayOnce = GUIFactory.createRadioButton( "einmal" );
    grpPlayCnt.add( this.rbPlayOnce );
    gbcOpt.gridx++;
    panelOpt.add( this.rbPlayOnce, gbcOpt );

    this.rbPlayInfinite = GUIFactory.createRadioButton(
					"st\u00E4ndig wiederholen",
					true );
    grpPlayCnt.add( this.rbPlayInfinite );
    gbcOpt.gridx++;
    panelOpt.add( this.rbPlayInfinite, gbcOpt );


    // Ausgabedatei
    JPanel panelFile = GUIFactory.createPanel( new GridBagLayout() );
    panelFile.setBorder(
		GUIFactory.createTitledBorder( "Ausgabedatei" ) );
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

    this.btnFileSelect = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_SELECT );
    gbcFile.fill    = GridBagConstraints.NONE;
    gbcFile.weightx = 0.0;
    gbcFile.gridx++;
    panelFile.add( this.btnFileSelect, gbcFile );


    // Statuszeile
    gbc.insets.left  = 0;
    gbc.insets.right = 0;
    gbc.gridwidth    = GridBagConstraints.REMAINDER;
    gbc.gridy++;
    add( GUIFactory.createSeparator(), gbc );

    this.labelStatus = GUIFactory.createLabel( DEFAULT_STATUS_TEXT );
    gbc.anchor       = GridBagConstraints.WEST;
    gbc.fill         = GridBagConstraints.NONE;
    gbc.weightx      = 0.0;
    gbc.insets.top   = 0;
    gbc.insets.left  = 5;
    gbc.insets.right = 5;
    gbc.gridy++;
    add( this.labelStatus, gbc );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 7, 1, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.NORTHEAST;
    gbc.insets.top  = 5;
    gbc.gridwidth   = 1;
    gbc.gridheight  = 3;
    gbc.gridy       = 0;
    gbc.gridx++;
    add( panelBtn, gbc );

    this.btnRecord = GUIFactory.createButton( EmuUtil.TEXT_RECORD );
    this.btnRecord.setEnabled( false );
    panelBtn.add( btnRecord );

    this.btnPause = GUIFactory.createButton( "Pause" );
    this.btnPause.setEnabled( false );
    panelBtn.add( btnPause );

    this.btnStop = GUIFactory.createButton( "Stopp" );
    this.btnStop.setEnabled( false );
    panelBtn.add( btnStop );

    this.btnPlay = GUIFactory.createButton( "Wiedergabe" );
    this.btnPlay.setEnabled( false );
    panelBtn.add( btnPlay );

    this.btnDelete = GUIFactory.createButton( EmuUtil.TEXT_DELETE );
    this.btnDelete.setEnabled( false );
    panelBtn.add( btnDelete );

    this.btnHelp = GUIFactory.createButtonHelp();
    panelBtn.add( btnHelp );

    this.btnClose = GUIFactory.createButtonClose();
    panelBtn.add( btnClose );


    // Vorbelegung
    File homeDir = FileUtil.getHomeDirFile();
    if( homeDir != null ) {
      this.fldFile.setFile( new File( homeDir, DEFAULT_FILE ) );
      this.fileCheckEnabled = true;
      this.btnRecord.setEnabled( true );
    }


    // Listener
    this.rbCaptureEmuSysScreen.addActionListener( this );
    this.rbCaptureScreenFrm.addActionListener( this );
    this.rbCaptureOtherWindow.addActionListener( this );
    this.cbForce256Colors.addActionListener( this );
    this.btnFileSelect.addActionListener( this );
    this.btnRecord.addActionListener( this );
    this.btnPause.addActionListener( this );
    this.btnStop.addActionListener( this );
    this.btnPlay.addActionListener( this );
    this.btnDelete.addActionListener( this );
    this.btnHelp.addActionListener( this );
    this.btnClose.addActionListener( this );


    // sonstiges
    updOptionFieldsEnabled();
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      pack();
      setScreenCentered();
    }
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


  private void checkFileExists( File file ) throws IOException
  {
    if( file != null ) {
      if( !file.exists() ) {
	this.btnPlay.setEnabled( false );
	this.btnDelete.setEnabled( false );
	throw new IOException( "Die Ausgabedatei existiert nicht mehr." );
      }
    }
  }


  private void closePlayer()
  {
    if( this.videoPlayFrm != null ) {
      this.videoPlayFrm.doClose();
    }
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


  private void doDelete() throws IOException
  {
    boolean deleted = false;
    File    file    = this.fldFile.getFile();
    if( file != null ) {
      checkFileExists( file );
      closePlayer();
      if( BaseDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie die Ausgabedatei mit dem"
			+ " aufgenommenen Video l\u00F6schen?" ) )
      {
	if( !file.delete() ) {
	  throw new IOException(
		"Ausgabedatei konnte nicht gel\u00F6scht werden." );
	}
	deleted = true;
      }
    } else {
      // sollte nie vorkommen
    }
    if( deleted ) {
      this.btnPlay.setEnabled( false );
      this.btnDelete.setEnabled( false );
    }
  }


  private void doFileSelect()
  {
    if( this.thread == null ) {
      File file = FileUtil.showFileSaveDlg(
			this,
			"GIF-Datei speichern",
			this.fldFile.getFile(),
			FileUtil.getGIFFileFilter() );
      if( file != null ) {
	if( !file.exists() || file.canWrite() ) {
	  String fileName = file.getName();
	  if( fileName != null ) {
	    if( fileName.toLowerCase().endsWith( ".gif" ) ) {
	      this.fldFile.setFile( file );
	      this.fileCheckEnabled = false;
	      this.btnRecord.setEnabled( true );
	    } else {
	      BaseDlg.showErrorDlg(
			this,
			"Das aufgenommene Bildschirmvideo kann nur in eine\n"
				+ "animierte GIF-Datei geschrieben werden.\n"
				+ "Der Dateiname muss deshalb mit \'.gif\'"
				+ " enden." );
	    }
	  }
	} else {
	  BaseDlg.showErrorDlg(
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


  private void doPlay() throws IOException
  {
    File file = this.fldFile.getFile();
    if( file != null ) {
      checkFileExists( file );
      if( !file.canRead() ) {
	this.btnPlay.setEnabled( false );
	throw new IOException( "Die Ausgabedatei ist nicht lesbar." );
      }
      if( this.videoPlayFrm != null ) {
	this.videoPlayFrm.setState( Frame.NORMAL );
      } else {
	this.videoPlayFrm = new VideoPlayFrm();
      }
      this.videoPlayFrm.setFile( this.fldFile.getFile() );
      this.videoPlayFrm.setVisible( true );
      this.videoPlayFrm.toFront();
    } else {
      // sollte nie vorkommen
      this.btnPlay.setEnabled( false );
      this.btnDelete.setEnabled( false );
    }
  }


  private void doRecord() throws IOException
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
	  closePlayer();
	  this.frameMillis          = frameMillis;
	  this.waitForWindowMillis  = 0;
	  this.waitForReset         = false;
	  this.pause                = false;
	  this.focusedWindowOnly    = false;
	  this.force256Colors       = false;
	  this.smoothColorReduction = false;
	  this.playInfinite         = false;
	  this.capturing            = false;
	  this.captureWidth         = 0;
	  this.captureHeight        = 0;
	  this.captureWindow        = null;

	  boolean fromEmuSys    = this.rbCaptureEmuSysScreen.isSelected();
	  boolean fromScreenFrm = this.rbCaptureScreenFrm.isSelected();
	  boolean fromOtherWin  = this.rbCaptureOtherWindow.isSelected();

	  if( (fromScreenFrm || fromOtherWin) && (this.robot == null) ) {
	    StringBuilder buf = new StringBuilder( 512 );
	    buf.append( "Das Aufnehmen eines Fensters ist nicht"
			+ " m\u00F6glich,\n"
			+ "da die Erstellung von Bildschirmfotos"
			+ " (Screenshots) fehlschl\u00E4gt." );
	    if( this.robotMsg != null ) {
	      if( !this.robotMsg.isEmpty() ) {
		buf.append( "\n\nDetails:\n" );
		buf.append( this.robotMsg );
	      }
	    }
	    throw new IOException( buf.toString() );
	  }

	  if( fromScreenFrm ) {
	    this.captureWindow = this.screenFrm;
	  }
	  else if( fromOtherWin ) {
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
	  if( fromEmuSys || fromScreenFrm ) {
	    this.waitForReset = this.cbStartAfterReset.isSelected();
	  }
	  if( fromScreenFrm || fromOtherWin ) {
	    this.focusedWindowOnly = this.cbFocusedWindowOnly.isSelected();
	  }
	  if( fromEmuSys ) {
	    this.force256Colors = this.cbForce256Colors.isSelected();
	  }
	  if( fromEmuSys
		|| fromScreenFrm
		|| this.cbForce256Colors.isSelected() )
	  {
	    this.smoothColorReduction
			= this.rbColorReductionSmooth.isSelected();
	  }
	  this.playInfinite = this.rbPlayInfinite.isSelected();

	  this.rbCaptureEmuSysScreen.setEnabled( false );
	  this.rbCaptureScreenFrm.setEnabled( false );
	  this.rbCaptureOtherWindow.setEnabled( false );
	  this.labelWinSelectTime.setEnabled( false );
	  this.spinnerWinSelectSec.setEnabled( false );
	  this.labelWinSelectUnit.setEnabled( false );
	  this.labelFramesPerSec.setEnabled( false );
	  this.comboFramesPerSec.setEnabled( false );
	  this.cbStartAfterReset.setEnabled( false );
	  this.cbFocusedWindowOnly.setEnabled( false );
	  this.cbForce256Colors.setEnabled( false );
	  this.labelColorReduction.setEnabled( false );
	  this.rbColorReductionSmooth.setEnabled( false );
	  this.rbColorReductionHard.setEnabled( false );
	  this.labelPlayCnt.setEnabled( false );
	  this.rbPlayOnce.setEnabled( false );
	  this.rbPlayInfinite.setEnabled( false );
	  this.btnFileSelect.setEnabled( false );
	  this.btnRecord.setEnabled( false );
	  this.btnStop.setEnabled( true );
	  this.btnPlay.setEnabled( false );
	  this.btnDelete.setEnabled( false );
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
    this.rbCaptureEmuSysScreen.setEnabled( true );
    this.rbCaptureScreenFrm.setEnabled( true );
    this.rbCaptureOtherWindow.setEnabled( true );
    this.labelFramesPerSec.setEnabled( true );
    this.comboFramesPerSec.setEnabled( true );
    this.labelPlayCnt.setEnabled( true );
    this.rbPlayOnce.setEnabled( true );
    this.rbPlayInfinite.setEnabled( true );
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
      this.btnPlay.setEnabled( false );
      this.btnDelete.setEnabled( false );
      EmuUtil.checkAndShowError(
			this,
			"Aufnehmen des Bildschirmvideo fehlgeschlagen",
			ex );
    } else {
      boolean state = false;
      File    file  = this.fldFile.getFile();
      if( file != null ) {
	state = file.isFile() && file.canRead();
      }
      this.btnPlay.setEnabled( state );
      this.btnDelete.setEnabled( state );
      this.fileCheckEnabled = true;
    }
    setIdle();
  }


  private void updOptionFieldsEnabled()
  {
    boolean fromEmuSys    = this.rbCaptureEmuSysScreen.isSelected();
    boolean fromScreenFrm = this.rbCaptureScreenFrm.isSelected();
    boolean fromOtherWin  = this.rbCaptureOtherWindow.isSelected();

    this.labelWinSelectTime.setEnabled( fromOtherWin );
    this.spinnerWinSelectSec.setEnabled( fromOtherWin );
    this.labelWinSelectUnit.setEnabled( fromOtherWin );
    this.cbStartAfterReset.setEnabled( fromEmuSys || fromScreenFrm );
    this.cbFocusedWindowOnly.setEnabled( fromScreenFrm || fromOtherWin );
    this.cbForce256Colors.setEnabled( fromEmuSys );

    boolean stateColorReduction =
		((fromEmuSys && this.cbForce256Colors.isSelected())
			|| fromScreenFrm
			|| fromOtherWin);
    this.labelColorReduction.setEnabled( stateColorReduction );
    this.rbColorReductionSmooth.setEnabled( stateColorReduction );
    this.rbColorReductionHard.setEnabled( stateColorReduction );
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
	    BaseDlg.showErrorDlg( this, "Kein JKCEMU-Fenster aktiv" );
	    text = DEFAULT_STATUS_TEXT;
	  }
	}
      }
    }
    this.labelStatus.setText( text );
  }
}
