/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Aufnehmen eines Bildschirmfotos
 */

package jkcemu.image;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
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
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.ScreenFrm;


public class ImageCaptureFrm extends BaseFrm
{
  private static final String DEFAULT_STATUS_TEXT = "Bereit";

  private static ImageCaptureFrm instance = null;

  private ScreenFrm         screenFrm;
  private int               waitForWindowMillis;
  private Robot             robot;
  private javax.swing.Timer statusTimer;
  private JRadioButton      rbCaptureEmuSysScreen;
  private JRadioButton      rbCaptureScreenFrm;
  private JRadioButton      rbCaptureOtherWindow;
  private JLabel            labelWinSelectTime;
  private JLabel            labelWinSelectUnit;
  private JSpinner          spinnerWinSelectSec;
  private JLabel            labelStatus;
  private JButton           btnTakePhoto;
  private JButton           btnClose;


  public static void open( ScreenFrm screenFrm )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new ImageCaptureFrm( screenFrm );
    }
    instance.toFront();
    instance.setVisible( true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( (src == this.rbCaptureEmuSysScreen)
	|| (src == this.rbCaptureScreenFrm)
	|| (src == this.rbCaptureOtherWindow) )
    {
      rv = true;
      updFieldsEnabled();
    }
    else if( src == this.btnTakePhoto ) {
      rv = true;
      doTakePhoto();
    }
    else if( src == this.btnClose ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      if( this.statusTimer.isRunning() ) {
	this.statusTimer.stop();
      }
      this.btnTakePhoto.setEnabled( true );
      updFieldsEnabled();
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ImageCaptureFrm( ScreenFrm screenFrm )
  {
    this.screenFrm           = screenFrm;
    this.waitForWindowMillis = 0;
    this.robot               = null;
    setTitle( "JKCEMU Bildschirmfoto" );

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
					3, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );


    // aufzunehmender Bereich
    ButtonGroup grpCaptureArea = new ButtonGroup();

    this.rbCaptureEmuSysScreen = GUIFactory.createRadioButton(
		"Bildschirmausgabe des emulierten Systems ohne Fenster",
		true );
    grpCaptureArea.add( this.rbCaptureEmuSysScreen );
    add( this.rbCaptureEmuSysScreen, gbc );

    this.rbCaptureScreenFrm = GUIFactory.createRadioButton(
		"Bildschirmausgabe des emulierten Systems mit Fenster" );
    grpCaptureArea.add( this.rbCaptureScreenFrm );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.rbCaptureScreenFrm, gbc );

    this.rbCaptureOtherWindow = GUIFactory.createRadioButton(
					"Beliebiges JKCEMU-Fenster" );
    grpCaptureArea.add( this.rbCaptureOtherWindow );
    gbc.gridy++;
    add( this.rbCaptureOtherWindow, gbc );

    this.labelWinSelectTime = GUIFactory.createLabel(
					"Zeit f\u00FCr Fensterauswahl:" );
    gbc.insets.left   = 50;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = 1;
    gbc.gridy++;
    add( this.labelWinSelectTime, gbc );

    this.spinnerWinSelectSec = GUIFactory.createSpinner(
				new SpinnerNumberModel( 3, 1, 9, 1 ) );
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.spinnerWinSelectSec, gbc );

    this.labelWinSelectUnit = GUIFactory.createLabel( "Sekunden" );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( this.labelWinSelectUnit, gbc );


    // Statuszeile
    gbc.fill         = GridBagConstraints.HORIZONTAL;
    gbc.weightx      = 1.0;
    gbc.insets.left  = 0;
    gbc.insets.right = 0;
    gbc.gridwidth    = GridBagConstraints.REMAINDER;
    gbc.gridx        = 0;
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
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 2, 1, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.NORTHEAST;
    gbc.insets.top  = 5;
    gbc.gridwidth   = 1;
    gbc.gridheight  = 4;
    gbc.gridy       = 0;
    gbc.gridx       = 3;
    add( panelBtn, gbc );

    this.btnTakePhoto = GUIFactory.createButton( EmuUtil.TEXT_RECORD );
    panelBtn.add( btnTakePhoto );

    this.btnClose = GUIFactory.createButtonClose();
    panelBtn.add( btnClose );


    // Listener
    this.rbCaptureEmuSysScreen.addActionListener( this );
    this.rbCaptureScreenFrm.addActionListener( this );
    this.rbCaptureOtherWindow.addActionListener( this );
    this.btnTakePhoto.addActionListener( this );
    this.btnClose.addActionListener( this );


    // sonstiges
    updFieldsEnabled();
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      pack();
      setScreenCentered();
    }
  }


	/* --- private Methoden --- */

  private void doTakePhoto()
  {
    this.waitForWindowMillis = 0;
    if( this.rbCaptureEmuSysScreen.isSelected() ) {
      fireTakePhoto( null );
    }
    if( this.rbCaptureScreenFrm.isSelected() ) {
      fireTakePhoto( this.screenFrm );
    }
    else if( this.rbCaptureOtherWindow.isSelected() ) {
      this.waitForWindowMillis = 5000;
      if( this.spinnerWinSelectSec != null ) {
	Object o = this.spinnerWinSelectSec.getValue();
	if( o instanceof Number ) {
	  int v = ((Number) o).intValue();
	  if( (v >= 1) && (v < 10) ) {
	    this.waitForWindowMillis = (v + 1) * 1000;
	  }
	}
      }
      this.labelWinSelectTime.setEnabled( false );
      this.spinnerWinSelectSec.setEnabled( false );
      this.labelWinSelectUnit.setEnabled( false );
      this.btnTakePhoto.setEnabled( false );
      this.statusTimer.start();
    }
  }


  private void fireTakePhoto( final Window captureWindow )
  {
    if( captureWindow != null ) {
      EmuUtil.frameToFront( captureWindow );
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    takePhoto( captureWindow );
			  }
			} );
    } else {
      takePhoto( null );
    }
  }


  private void takePhoto( Window captureWindow )
  {
    BufferedImage image  = null;
    if( captureWindow != null ) {
      if( this.robot != null ) {
	int x = captureWindow.getX();
	int y = captureWindow.getY();
	int w = captureWindow.getWidth();
	int h = captureWindow.getHeight();
	if( x < 0 ) {
	  w += x;
	  x = 0;
	}
	if( y < 0 ) {
	  h += y;
	  y = 0;
	}
	if( (w > 0) && (h > 0) ) {
	  image = this.robot.createScreenCapture(
					new Rectangle( x, y, w, h ) );
	}
      } else {
	BaseDlg.showErrorDlg(
			this,
			"Aufnehmen des Bildschirmfotos fehlgeschlagen" );
      }
    } else {
      image = this.screenFrm.createSnapshot();
    }
    if( image != null ) {
      ImageFrm.open(
		image,
		ImageUtil.createScreenshotExifData(),
		"Bildschirmfoto" );
    }
  }


  private void updFieldsEnabled()
  {
    boolean state = this.rbCaptureOtherWindow.isSelected();
    this.labelWinSelectTime.setEnabled( state );
    this.labelWinSelectUnit.setEnabled( state );
    this.spinnerWinSelectSec.setEnabled( state );
  }


  private void updStatusText()
  {
    boolean timerEnabled = false;
    String  text         = DEFAULT_STATUS_TEXT;
    if( this.waitForWindowMillis > 0 ) {
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
	timerEnabled = true;
      } else {
	Window   captureWindow = null;
	Window[] windows       = Window.getWindows();
	if( windows != null ) {
	  for( Window window : windows ) {
	    if( window.isFocused() ) {
	      captureWindow = window;
	      break;
	    }
	  }
	}
	if( captureWindow != null ) {
	  fireTakePhoto( captureWindow );
	} else {
	  BaseDlg.showErrorDlg( this, "Kein JKCEMU-Fenster aktiv" );
	  text = DEFAULT_STATUS_TEXT;
	}
	this.btnTakePhoto.setEnabled( true );
	updFieldsEnabled();
      }
    }
    this.labelStatus.setText( text );
    if( !timerEnabled ) {
      this.statusTimer.stop();
    }
  }
}
