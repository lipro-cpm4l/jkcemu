/*
 * (c) 2016-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster mit einer sekundaeren Bildschirmanzeige
 */

package jkcemu.base;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import jkcemu.Main;


public class SecondaryScreenFrm extends AbstractScreenFrm
{
  private static final String PROP_SCALE = "scale";

  private static Point oldLocation = null;

  private JMenuItem mnuClose;
  private ScreenFrm screenFrm;


  public SecondaryScreenFrm(
			ScreenFrm            screenFrm,
			AbstractScreenDevice screenDevice )
  {
    this.screenFrm    = screenFrm;
    this.emuThread    = screenFrm.getEmuThread();
    this.copyEnabled  = screenDevice.supportsCopyToClipboard();
    this.pasteEnabled = screenDevice.supportsPasteFromClipboard();
    setTitle( screenDevice.getTitle() );


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    JMenu mnuScreen = createScreenMenu( this.copyEnabled );
    if( mnuScreen != null ) {
      mnuFile.add( mnuScreen );
      mnuFile.addSeparator();
    }

    this.mnuClose = createMenuItemClose();
    mnuFile.add( mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = createEditMenu( this.copyEnabled, this.pasteEnabled );


    // Menu zusammenbauen
    setJMenuBar( GUIFactory.createMenuBar(
					mnuFile,
					mnuEdit, 
					createScaleMenu() ) );


    // Popup-Menu
    if( this.copyEnabled || this.pasteEnabled ) {
      createPopupMenu( this.copyEnabled, this.pasteEnabled );
    }


    // Fensterinhalt
    setLayout( new BorderLayout() );

    // Bildschirmausgabe
    this.screenFld = new ScreenFld( this );
    this.screenFld.setFocusable( true );
    this.screenFld.setFocusTraversalKeysEnabled( false );
    this.screenFld.addMouseListener( this );
    add( this.screenFld, BorderLayout.CENTER );


    // sonstiges
    setScreenDevice( screenDevice );
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      if( oldLocation != null ) {
	setLocation( oldLocation );
      } else {
	setLocationByPlatform( true );
      }
      pack();
    }
  }


  public boolean accepts( AbstractScreenDevice device )
  {
    boolean rv = false;
    if( device != null ) {
      AbstractScreenDevice oldDevice = this.screenFld.getScreenDevice();
      if( device.getClass().equals( oldDevice.getClass() )
	  && (device.getScreenHeight() == oldDevice.getScreenHeight())
	  && (device.getScreenWidth() == oldDevice.getScreenWidth()) )
      {
	setScreenDevice( device );
	rv = true;
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props )
  {
    boolean rv      = super.applySettings( props );
    boolean changed = false;
    if( !isVisible() ) {
      int scale = EmuUtil.getIntProperty(
				props,
				getSettingsPrefix() + PROP_SCALE,
				1 );
      if( scale < 1 ) {
	scale = 1;
      }
      setScreenScale( scale );
      this.screenFld.setScreenScale( scale );
      changed = true;
    }
    int margin = EmuUtil.getIntProperty(
				props,
				PROP_PREFIX + PROP_SCREEN_MARGIN,
				ScreenFld.DEFAULT_MARGIN );
    if( (margin >= 0) && (margin != this.screenFld.getMargin()) ) {
      this.screenFld.setMargin( margin );
      changed = true;
    }
    if( changed ) {
      pack();
    }
    return rv;
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.mnuClose ) {
	rv = true;
	doClose();
      }
    }
    if( !rv ) {
      rv = super.doAction( e );
    }
    this.screenFld.requestFocus();
    return rv;
  }


  @Override
  public boolean doClose()
  {
    Point   location = getLocation();
    boolean rv       = super.doClose();
    if( rv ) {
      KeyListener l = this.screenFld.getScreenDevice().getKeyListener();
      if( l != null ) {
	this.screenFld.removeKeyListener( l );
      }
      this.screenFrm.childFrameClosed( this );
      oldLocation = location;
    }
    return rv;
  }


  @Override
  protected AbstractScreenDevice getScreenDevice()
  {
    return this.screenFld.getScreenDevice();
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );
      props.setProperty(
		getSettingsPrefix() + PROP_SCALE,
		String.valueOf( this.screenFld.getScreenScale() ) );
    }
  }


  @Override
  public void windowActivated( WindowEvent e )
  {
    super.windowActivated( e );
    if( e.getWindow() == this ) {
      Main.setWindowActivated( Main.WINDOW_MASK_SECONDARY_SCREEN );
    }
  }


  @Override
  public void windowDeactivated( WindowEvent e )
  {
    super.windowDeactivated( e );
    if( e.getWindow() == this ) {
      Main.setWindowDeactivated( Main.WINDOW_MASK_SECONDARY_SCREEN );
    }
  }


	/* --- private Methoden --- */

  private void setScreenDevice( AbstractScreenDevice screenDevice )
  {
    AbstractScreenDevice oldScreenDevice = this.screenFld.getScreenDevice();
    if( oldScreenDevice != null ) {
      KeyListener oldListener = oldScreenDevice.getKeyListener();
      if( oldListener != null ) {
	this.screenFld.removeKeyListener( oldListener );
      }
    }
    this.screenFld.setScreenDevice( screenDevice );
    KeyListener listener = screenDevice.getKeyListener();
    if( listener != null ) {
      this.screenFld.addKeyListener( listener );
    }
    screenDevice.setScreenFrm( this );
  }
}
