/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer Swing-Frames
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import jkcemu.Main;


public class BaseFrm extends JFrame implements
					ActionListener,
					MouseListener,
					KeyListener,
					WindowListener
{
  public static final String PROP_WINDOW_X         = "window.x";
  public static final String PROP_WINDOW_Y         = "window.y";
  public static final String PROP_WINDOW_WIDTH     = "window.width";
  public static final String PROP_WINDOW_HEIGHT    = "window.height";
  public static final String PROP_WINDOW_ICONIFIED = "window.iconified";
  public static final String PROP_WINDOW_MAXIMIZED = "window.maximized";
  public static final String PROP_WINDOW_VISIBLE   = "window.visible";


  private boolean                   notified;
  private java.util.List<JMenuItem> menuItems;


  protected BaseFrm()
  {
    this.notified  = false;
    this.menuItems = new ArrayList<>();
    setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
    addWindowListener( this );
    Main.frameCreated( this );
    Main.updIcon( this );
  }


  /*
   * Rueckgabewert: true, wenn Fensterposition bzw. -groesse geaendert wurde
   */
  public boolean applySettings( Properties props )
  {
    boolean rv = false;
    if( (props != null) && !isVisible() ) {
      String prefix = getSettingsPrefix();

      int x = EmuUtil.getIntProperty(
				props,
				prefix + PROP_WINDOW_X,
				Integer.MIN_VALUE );
      int y = EmuUtil.getIntProperty(
				props,
				prefix + PROP_WINDOW_Y,
				Integer.MIN_VALUE );
      int w = EmuUtil.getIntProperty(
				props,
				prefix + PROP_WINDOW_WIDTH,
				0 );
      int h = EmuUtil.getIntProperty(
				props,
				prefix + PROP_WINDOW_HEIGHT,
				0 );
      /*
       * Wenn sich die Bildschirmgroesse geandert hat, wird geprueft,
       * ob die linke obere Ecke des Fensters sichtbar sein wird.
       * Wenn nicht, wird die gespeicherte Fensterposition ignoriert
       */
      if( (x > Integer.MIN_VALUE) && (y > Integer.MIN_VALUE) ) {
	Dimension screenSize = getScreenSize();
	if( screenSize != null ) {
	  int wScreen = EmuUtil.getIntProperty(
					props,
					Main.PROP_SCREEN_WIDTH,
					0 );
	  int hScreen = EmuUtil.getIntProperty(
					props,
					Main.PROP_SCREEN_HEIGHT,
					0 );
	  if( (hScreen > 0) && (wScreen > 0)
	      && ((screenSize.width != wScreen)
			|| (screenSize.height != hScreen)) )
	  {
	    /*
	     * Bildschirmgroesse unterscheidet sich von der
	     * im Profil gespeicherten -> pruefen,
	     * ob die obere linke Ecke des Fensters sichtbar ist
	     * (dabei auch eine moegliche Systemleiste links oder unten
	     * beruecksichtigen),
	     * Wenn nicht, dann die gespeicherte Fensterposition ignorieren
	     */
	    if( (x < 0) || (y < 0)
		|| (x > (screenSize.width - 100))
		|| (y > (screenSize.height - 100)) )
	    {
	      x = Integer.MIN_VALUE;
	      y = Integer.MIN_VALUE;
	    }
	  }
	}
      }
      if( (x > Integer.MIN_VALUE) && (y > Integer.MIN_VALUE) ) {
	if( isResizable() && (w > 0) && (h > 0) ) {
	  setBounds( x, y, w, h );
	} else {
	  setLocation( x, y );
	}
	rv = true;
      } else {
	setLocationByPlatform( true );
	if( isResizable() && (w > 0) && (h > 0) ) {
	  setSize( w, h );
	}
      }
      int frmState = 0;
      if( EmuUtil.getBooleanProperty(
			props,
			prefix + PROP_WINDOW_ICONIFIED,
			false ) )
      {
	frmState |= Frame.ICONIFIED;
      }
      if( EmuUtil.getBooleanProperty(
			props,
			prefix + PROP_WINDOW_MAXIMIZED,
			false ) )
      {
	frmState |= Frame.MAXIMIZED_BOTH;
      }
      setExtendedState( frmState != 0 ? frmState : Frame.NORMAL );
    }
    return rv;
  }


  /*
   * Diese folgenden Methoden erzeugen ein JMenu
   * mit der entsprechenden Mnemonic-Taste.
   */
  protected static JMenu createMenuEdit()
  {
    JMenu menu = GUIFactory.createMenu( "Bearbeiten" );
    menu.setMnemonic( KeyEvent.VK_B );
    return menu;
  }


  protected static JMenu createMenuFile()
  {
    JMenu menu = GUIFactory.createMenu( "Datei" );
    menu.setMnemonic( KeyEvent.VK_D );
    return menu;
  }


  protected static JMenu createMenuHelp()
  {
    JMenu menu = GUIFactory.createMenu( "Hilfe" );
    menu.setMnemonic( KeyEvent.VK_H );
    return menu;
  }


  public static JMenu createMenuSettings()
  {
    JMenu menu = GUIFactory.createMenu( EmuUtil.TEXT_SETTINGS );
    menu.setMnemonic( KeyEvent.VK_E );
    return menu;
  }


  protected JMenuItem createMenuItem( String text )
  {
    JMenuItem item = GUIFactory.createMenuItem( text );
    this.menuItems.add( item );
    return item;
  }


  protected JMenuItem createMenuItem( String text, String actionCmd )
  {
    JMenuItem item = createMenuItem( text );
    item.setActionCommand( actionCmd );
    return item;
  }


  protected JMenuItem createMenuItemClose()
  {
    return createMenuItem( EmuUtil.TEXT_CLOSE );
  }


  protected JMenuItem createMenuItemClose( String actionCmd )
  {
    JMenuItem item = createMenuItemClose();
    item.setActionCommand( actionCmd );
    return item;
  }


  protected JMenuItem createMenuItemCopy( boolean withAccelerator )
  {
    JMenuItem item = createMenuItem( EmuUtil.TEXT_COPY );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator( item, KeyEvent.VK_C, false );
    }
    return item;
  }


  protected JMenuItem createMenuItemCut( boolean withAccelerator )
  {
    JMenuItem item = createMenuItem( EmuUtil.TEXT_CUT );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator( item, KeyEvent.VK_X, false );
    }
    return item;
  }


  protected JMenuItem createMenuItemFindNext( boolean withAccelerator )
  {
    JMenuItem item = createMenuItem( EmuUtil.TEXT_FIND_NEXT );
    if( withAccelerator ) {
      EmuUtil.setDirectAccelerator( item, KeyEvent.VK_F3, false );
    }
    return item;
  }


  protected JMenuItem createMenuItemFindPrev( boolean withAccelerator )
  {
    JMenuItem item = createMenuItem( EmuUtil.TEXT_FIND_PREV );
    if( withAccelerator ) {
      EmuUtil.setDirectAccelerator( item, KeyEvent.VK_F3, true );
    }
    return item;
  }


  protected JMenuItem createMenuItemOpenFind( boolean withAccelerator )
  {
    JMenuItem item = createMenuItem( EmuUtil.TEXT_OPEN_FIND );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator( item, KeyEvent.VK_F, false );
    }
    return item;
  }


  protected JMenuItem createMenuItemOpenPrint( boolean withAccelerator )
  {
    JMenuItem item = createMenuItem( EmuUtil.TEXT_OPEN_PRINT );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator( item, KeyEvent.VK_P, false );
    }
    return item;
  }


  protected JMenuItem createMenuItemOpenPrintOptions()
  {
    return createMenuItem( "Druckoptionen..." );
  }


  protected JMenuItem createMenuItemPaste( boolean withAccelerator )
  {
    JMenuItem item = createMenuItem( EmuUtil.TEXT_PASTE );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator( item, KeyEvent.VK_V, false );
    }
    return item;
  }


  protected JMenuItem createMenuItemSaveAs( boolean withAccelerator )
  {
    JMenuItem item = createMenuItem( "Speichern unter..." );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator( item, KeyEvent.VK_S, true );
    }
    return item;
  }


  protected JMenuItem createMenuItemSelectAll( boolean withAccelerator )
  {
    JMenuItem item = createMenuItem( EmuUtil.TEXT_SELECT_ALL );
    if( withAccelerator ) {
      EmuUtil.setStandardAccelerator( item, KeyEvent.VK_A, false );
    }
    return item;
  }


  protected JMenuItem createMenuItemWithDirectAccelerator(
						String text,
						int    keyCode )
  {
    JMenuItem item = createMenuItem( text );
    EmuUtil.setDirectAccelerator( item, keyCode, false );
    return item;
  }


  protected JMenuItem createMenuItemWithDirectAccelerator(
						String  text,
						int     keyCode,
						boolean shiftDown )
  {
    JMenuItem item = createMenuItem( text );
    EmuUtil.setDirectAccelerator( item, keyCode, shiftDown );
    return item;
  }


  protected JMenuItem createMenuItemWithDirectAccelerator(
						String  text,
						String  actionCmd,
						int     keyCode )
  {
    JMenuItem item = createMenuItem( text, actionCmd );
    EmuUtil.setDirectAccelerator( item, keyCode, false );
    return item;
  }


  protected JMenuItem createMenuItemWithDirectAccelerator(
						String  text,
						String  actionCmd,
						int     keyCode,
						boolean shiftDown )
  {
    JMenuItem item = createMenuItem( text, actionCmd );
    EmuUtil.setDirectAccelerator( item, keyCode, shiftDown );
    return item;
  }


  protected JMenuItem createMenuItemWithStandardAccelerator(
						String text,
						int    keyCode )
  {
    JMenuItem item = createMenuItem( text );
    EmuUtil.setStandardAccelerator( item, keyCode, false );
    return item;
  }


  protected JMenuItem createMenuItemWithStandardAccelerator(
						String  text,
						int     keyCode,
						boolean shiftDown )
  {
    JMenuItem item = createMenuItem( text );
    EmuUtil.setStandardAccelerator( item, keyCode, shiftDown );
    return item;
  }


  /*
   * Die Methode fordert das Schliessen des Dialoges an und
   * liefert true zurueck, wenn der Dialog auch tatsaechlich
   * geschlossen wurde.
   */
  public boolean doClose()
  {
    /*
     * Eigenschaften des Fensters merken,
     * damit beim erneuten Oeffnen dieses wieder
     * auf der gleichen Position erscheint.
     */
    putSettingsTo( Main.getProperties() );
    setVisible( false );
    dispose();
    return true;
  }


  /*
   * Die Methode wird aufgerufen, wenn das Fenster geschlossen
   * und zusaetzlich auch alle sonst im Hintergrund weiterlaufenden
   * Aktivitaeten beendet werden sollen.
   */
  public boolean doQuit()
  {
    return doClose();
  }


  public void fireRepaint()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    repaint();
		  }
		} );
  }


  public boolean getPackOnUIUpdate()
  {
    return !isResizable();
  }


  public String getSettingsPrefix()
  {
    return getClass().getName() + ".";
  }


  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      String prefix = getSettingsPrefix();

      Point location = getLocation();
      if( location != null ) {
	props.setProperty(
			prefix + PROP_WINDOW_X,
			String.valueOf( location.x ) );
	props.setProperty(
			prefix + PROP_WINDOW_Y,
			String.valueOf( location.y ) );
      }

      if( isResizable() ) {
	Dimension size = getSize();
	if( size != null ) {
	  if( (size.width > 0) && (size.height > 0) ) {
	    props.setProperty(
			prefix + PROP_WINDOW_WIDTH,
			String.valueOf( size.width ) );
	    props.setProperty(
			prefix + PROP_WINDOW_HEIGHT,
			String.valueOf( size.height ) );
	  }
	}
      }

      int frameState = getExtendedState();
      EmuUtil.setProperty(
		props,
		prefix + PROP_WINDOW_ICONIFIED,
		(frameState & Frame.ICONIFIED) == Frame.ICONIFIED );
      EmuUtil.setProperty(
		props,
		prefix + PROP_WINDOW_MAXIMIZED,
		(frameState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH );
      EmuUtil.setProperty(
		props,
		prefix + PROP_WINDOW_VISIBLE,
		isVisible() );
    }
  }


  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    // leer
  }


  /*
   * Die Methode setzt die Groesse und Position des Fensters
   * auf einen Standardwert.
   */
  public void setBoundsToDefaults()
  {
    Dimension screenSize = getScreenSize();
    if( screenSize != null ) {
      setBounds(
	screenSize.width  / 4,
	screenSize.height / 4,
	screenSize.width  / 2,
	screenSize.height / 2 );
    } else {
      setSize( 200, 200 );
    }
  }


  /*
   * Die Methode zentriert das Frame auf dem Bildschirm.
   */
  public void setScreenCentered()
  {
    Dimension screenSize = getScreenSize();
    if( screenSize != null ) {
      Dimension mySize = getSize();
      int       x      = (screenSize.width - mySize.width) / 2;
      int       y      = (screenSize.height - mySize.height) / 2;
      setLocation( x >= 0 ? x : 0, y >= 0 ? y : 0 );
    }
  }


  /*
   * Die Methode schaltet den Warte-Cursor ein bzw. aus.
   */
  public void setWaitCursor( boolean state )
  {
    Component c = getGlassPane();
    if( c != null ) {
      c.setVisible( state );
      c.setCursor( state ?
	Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) : null );
    }
  }


  /*
   * Diese Methode wird aufgerufen,
   * wenn sich das Erscheinungsbild, Schriften oder Symbolgroessen aendern
   */
  public void updUI(
		Properties props,
		boolean    updLAF,
		boolean    updFonts,
		boolean    updIcons )
  {
    // leer
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    doActionInternal( e );
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( showPopupMenuInternal( e ) ) {
      e.consume();
    } else if( e.getClickCount() > 1 ) {
      if( doActionInternal( e ) ) {
	e.consume();
      }
    }
  }

  @Override
  public void mouseEntered( MouseEvent e )
  {
    // leer
  }

  @Override
  public void mouseExited( MouseEvent e )
  {
    // leer
  }

  @Override
  public void mousePressed( MouseEvent e )
  {
    if( showPopupMenuInternal( e ) )
      e.consume();
  }

  @Override
  public void mouseReleased( MouseEvent e )
  {
    if( showPopupMenuInternal( e ) )
      e.consume();
  }


	/* --- KeyListener --- */

  @Override
  public void keyPressed( KeyEvent e )
  {
    if( e != null ) {
      if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
	if( doActionInternal( e ) ) {
	  e.consume();
	}
      }
    }
  }

  @Override
  public void keyReleased( KeyEvent e )
  {
    // leer
  }

  @Override
  public void keyTyped( KeyEvent e )
  {
    // leer
  }


	/* --- WindowListener --- */

  @Override
  public void windowActivated( WindowEvent e )
  {
    // leer
  }

  @Override
  public void windowClosed( WindowEvent e )
  {
    // leer
  }

  @Override
  public void windowClosing( WindowEvent e )
  {
    doClose();
  }

  @Override
  public void windowDeactivated( WindowEvent e )
  {
    // leer
  }

  @Override
  public void windowDeiconified( WindowEvent e )
  {
    // leer
  }

  @Override
  public void windowIconified( WindowEvent e )
  {
    // leer
  }

  @Override
  public void windowOpened( WindowEvent e )
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
      for( JMenuItem item : this.menuItems ) {
	item.addActionListener( this );
      }
    }
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      for( JMenuItem item : this.menuItems ) {
	item.removeActionListener( this );
      }
    }
  }


  @Override
  public void toFront()
  {
    int m = getExtendedState();
    if( (m & Frame.ICONIFIED) != 0 ) {
      m &= ~Frame.ICONIFIED;
      setExtendedState( m );
    }
    super.toFront();
  }


	/* --- zu ueberschreibende Methoden --- */

  protected boolean doAction( EventObject e )
  {
    return false;
  }


  /*
   * Rueckgabewert:
   *   true:  Popup wurde angezeigt und Event soll konsumiert werden.
   *   false: Popup nicht angezeigt, Event nicht konsumieren
   */
  protected boolean showPopupMenu( MouseEvent e )
  {
    return false;
  }


	/* --- private Methoden --- */

  private boolean doActionInternal( EventObject e )
  {
    boolean rv = false;
    setWaitCursor( true );
    try {
      rv = doAction( e );
    }
    catch( Exception ex ) {
      EmuUtil.checkAndShowError( this, null, ex );
    }
    setWaitCursor( false );
    return rv;
  }


  private Dimension getScreenSize()
  {
    Toolkit tk = EmuUtil.getToolkit( this );
    return tk != null ? tk.getScreenSize() : null;
  }


  private boolean showPopupMenuInternal( MouseEvent e )
  {
    boolean rv = false;
    if( e != null ) {
      if( e.isPopupTrigger() ) {
	rv = showPopupMenu( e );
      }
    }
    return rv;
  }
}
