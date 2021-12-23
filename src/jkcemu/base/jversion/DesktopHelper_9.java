/*
 * (c) 2019-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Desktop-Integration fuer Java 9 und hoeher
 */

package jkcemu.base.jversion;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.desktop.ScreenSleepEvent;
import java.awt.desktop.ScreenSleepListener;
import java.awt.desktop.SystemSleepEvent;
import java.awt.desktop.SystemSleepListener;
import java.awt.desktop.UserSessionEvent;
import java.awt.desktop.UserSessionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import jkcemu.Main;
import jkcemu.base.AboutDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.DesktopHelper;
import jkcemu.base.GUIFactory;
import jkcemu.base.ScreenFrm;


public class DesktopHelper_9
			extends DesktopHelper
			implements
				ScreenSleepListener,
				SystemSleepListener,
				UserSessionListener
{
  private ScreenFrm screenFrm;


  public DesktopHelper_9( final BaseFrm topFrm )
  {
    this.screenFrm = null;
    if( topFrm instanceof ScreenFrm ) {
      this.screenFrm = (ScreenFrm) topFrm;
    }
    if( this.desktop != null ) {

      // Standard.Menu
      if( this.desktop.isSupported( Desktop.Action.APP_MENU_BAR ) ) {
	try {
	  JMenuItem mnuQuit = GUIFactory.createMenuItem( "Beenden" );
	  mnuQuit.addActionListener( e->topFrm.doClose() );

	  JMenu mnuApp = GUIFactory.createMenu( Main.getAppName() );
	  mnuApp.add( mnuQuit );

	  this.desktop.setDefaultMenuBar(
			GUIFactory.createMenuBar( mnuApp ) );
	}
	catch( UnsupportedOperationException ex ) {}
      }

      // About-Handler
      if( this.desktop.isSupported( Desktop.Action.APP_ABOUT ) ) {
	try {
	  this.desktop.setAboutHandler( e->AboutDlg.fireOpen( topFrm ) );
	}
	catch( UnsupportedOperationException ex ) {}
      }

      // Quit-Handler
      if( this.desktop.isSupported( Desktop.Action.APP_QUIT_HANDLER ) ) {
	try {
	  this.desktop.setQuitHandler(
			(e,r)->{
				if( topFrm.doClose() ) {
				  r.performQuit();
				} else {
				  r.cancelQuit();
				}
			} );
	}
	catch( UnsupportedOperationException ex ) {}
      }

      // Listener zum Zuruecksetzen der Taktfrequenzberechnung
      if( this.screenFrm != null ) {
	this.desktop.addAppEventListener( this );
      }
    }

    // Icon
    if( Taskbar.isTaskbarSupported() ) {
      try {
	java.util.List<Image> iconImages = Main.getIconImages( topFrm );
	if( iconImages != null ) {
	  int n = iconImages.size();
	  if( n > 0 ) {
	    Taskbar.getTaskbar().setIconImage( iconImages.get( n - 1 ) );
	  }
	}
      }
      catch( UnsupportedOperationException ex ) {}
    }
  }


	/* --- ScreenSleepListener --- */

  @Override
  public void screenAboutToSleep( ScreenSleepEvent e )
  {
    // leer
  }

  @Override
  public void screenAwoke( ScreenSleepEvent e )
  {
    resetCPUSpeed();
  }


	/* --- SystemSleepListener --- */

  @Override
  public void systemAboutToSleep( SystemSleepEvent e )
  {
    // leer
  }

  @Override
  public void systemAwoke( SystemSleepEvent e )
  {
    resetCPUSpeed();
  }


	/* --- UserSessionListener --- */

  @Override
  public void userSessionActivated( UserSessionEvent e )
  {
    resetCPUSpeed();
  }

  @Override
  public void userSessionDeactivated( UserSessionEvent e )
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean isMoveToTrashSupportedInternal()
  {
    return this.desktop != null ?
		this.desktop.isSupported( Desktop.Action.MOVE_TO_TRASH )
		: false;
  }


  @Override
  protected void moveToTrashInternal( File file ) throws IOException
  {
    if( this.desktop == null ) {
      throwMoveToTrashNotSupported();
    }
    if( !this.desktop.moveToTrash( file ) ) {
      throw new IOException( file.getPath()
		+  ":\nKonnte nicht in den Papierkorb geworfen werden" );
    }
  }


	/* --- private Methoden --- */

  private void resetCPUSpeed()
  {
    if( this.screenFrm != null )
      this.screenFrm.getEmuThread().getZ80CPU().resetSpeed();
  }
}
