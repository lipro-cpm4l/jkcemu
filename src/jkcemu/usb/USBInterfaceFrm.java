/*
 * (c) 2011-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer den USB-Anschluss
 */

package jkcemu.usb;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JMenu;
import javax.swing.JTabbedPane;
import jkcemu.Main;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.ScreenFrm;


public class USBInterfaceFrm extends BaseFrm
{
  private static final String ACTION_CLOSE = "close";
  private static final String ACTION_HELP  = "help";
  private static final String HELP_PAGE    = "/help/usb.htm";

  private static USBInterfaceFrm instance = null;

  private ScreenFrm   screenFrm;
  private JTabbedPane tabbedPane;


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  public static void open( ScreenFrm screenFrm )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new USBInterfaceFrm( screenFrm );
    }
    instance.toFront();
    instance.setVisible( true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props )
  {
    int n = this.tabbedPane.getTabCount();
    for( int i = 0; i < n; i++ ) {
      Component c = this.tabbedPane.getComponent( i );
      if( c != null ) {
	if( c instanceof USBInterfaceFld ) {
	  ((USBInterfaceFld) c).updFields();
	}
      }
    }
    return super.applySettings( props );
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    if( newEmuSys != null ) {
      this.tabbedPane.removeAll();
      for( VDIP vdip : newEmuSys.getVDIPs() ) {
	this.tabbedPane.addTab(
			vdip.getModuleTitle(),
			new USBInterfaceFld( this, vdip ) );
      }
      if( this.tabbedPane.getTabCount() < 1 ) {
	doClose();
      }
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e instanceof ActionEvent ) {
      String cmd = ((ActionEvent) e).getActionCommand();
      if( cmd != null ) {
	if( cmd.equals( ACTION_CLOSE ) ) {
	  rv = true;
	  doClose();
	}
	else if( cmd.equals( ACTION_HELP ) ) {
	  rv = true;
	  HelpFrm.openPage( HELP_PAGE );
	}
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private USBInterfaceFrm( ScreenFrm screenFrm )
  {
    this.screenFrm = screenFrm;
    setTitle( "JKCEMU USB-Anschluss" );


    // Menu Datei
    JMenu mnuFile = createMenuFile();
    mnuFile.add( createMenuItemClose( ACTION_CLOSE ) );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();
    mnuHelp.add(
	createMenuItem( "Hilfe zum USB-Anschluss...", ACTION_HELP ) );


    // Menu
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuHelp ) );


    // Fensterinhalt
    setLayout( new BorderLayout() );

    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );

    resetFired( screenFrm.getEmuSys(), null );


    // Fenstergroesse
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      pack();
      setLocationByPlatform( true );
    }
  }
}
