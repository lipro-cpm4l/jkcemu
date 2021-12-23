/*
 * (c) 2011-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fensterklasse fuer eine Tastaturansicht
 */

package jkcemu.base;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import jkcemu.Main;


public class KeyboardFrm extends BaseFrm
{
  private static final String HELP_PAGE = "/help/keyboard.htm";

  private ScreenFrm                             screenFrm;
  private AbstractKeyboardFld<? extends EmuSys> keyboardFld;
  private JMenuItem                             mnuClose;
  private JCheckBoxMenuItem                     mnuHoldShiftBtn;
  private JSeparator                            mnuHoldShiftSep;
  private JMenuItem                             mnuHelpContent;


  public KeyboardFrm(
		ScreenFrm                             screenFrm,
		EmuSys                                emuSys,
		AbstractKeyboardFld<? extends EmuSys> keyboardFld )
  {
    this.screenFrm   = screenFrm;
    this.keyboardFld = keyboardFld;


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    this.mnuHoldShiftBtn = GUIFactory.createCheckBoxMenuItem(
			"Shift- und Control-Tasten gedr\u00FCckt halten",
			this.keyboardFld.getHoldShift() );
    this.mnuHoldShiftBtn.addActionListener( this );
    mnuFile.add( this.mnuHoldShiftBtn );

    this.mnuHoldShiftSep = GUIFactory.createSeparator();
    mnuFile.add( this.mnuHoldShiftSep );

    this.mnuClose = createMenuItemClose();
    mnuFile.add( this.mnuClose );


    // Menu Hilfe
    JMenu mnuHelp = createMenuHelp();

    this.mnuHelpContent = createMenuItem( "Hilfe zur Tastatur..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menuleiste
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuHelp ) );


    // Fensterinhalt
    setLayout( new BorderLayout() );

    this.keyboardFld = keyboardFld;
    this.keyboardFld.setFocusable( true );
    this.keyboardFld.setFocusTraversalKeysEnabled( false );
    this.keyboardFld.addKeyListener( this );
    add( this.keyboardFld, BorderLayout.CENTER );


    // sonstiges
    pack();
    setResizable( false );
    if( !applySettings( Main.getProperties() ) ) {
      setScreenCentered();
    }
    updWindowElements( emuSys );
  }


  public boolean accepts( EmuSys emuSys )
  {
    boolean rv = false;
    if( emuSys != null ) {
      if( this.keyboardFld.accepts( emuSys ) ) {
	try {
	  this.keyboardFld.setEmuSys( emuSys );
	  updWindowElements( emuSys );
	  rv = true;
	}
	catch( IllegalArgumentException ex ) {}
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.mnuClose ) {
	  rv = true;
	  doClose();
	}
	else if( src == this.mnuHoldShiftBtn ) {
	  rv = true;
	  this.keyboardFld.setHoldShift( this.mnuHoldShiftBtn.isSelected() );
	}
	else if( src == this.mnuHelpContent ) {
	  rv = true;
	  HelpFrm.openPage( HELP_PAGE );
	}
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.screenFrm.childFrameClosed( this );
    }
    return rv;
  }


  @Override
  public void keyPressed( KeyEvent e )
  {
    boolean done = false;
    if( (e.getKeyCode() == KeyEvent.VK_SHIFT)
	&& this.keyboardFld.getSelectionChangeOnShiftOnly() )
    {
      done = this.keyboardFld.changeShiftSelectionTo( true );
    }
    if( done ) {
      e.consume();
    } else {
      this.screenFrm.keyPressed( e );
    }
  }


  @Override
  public void keyReleased( KeyEvent e )
  {
    boolean done = false;
    if( (e.getKeyCode() == KeyEvent.VK_SHIFT)
	&& this.keyboardFld.getSelectionChangeOnShiftOnly() )
    {
      done = this.keyboardFld.changeShiftSelectionTo( false );
    }
    if( done ) {
      e.consume();
    } else {
      this.screenFrm.keyReleased( e );
    }
  }


  @Override
  public void keyTyped( KeyEvent e )
  {
    this.screenFrm.keyTyped( e );
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties newProps )
  {
    if( newEmuSys != null ) {
      if( this.keyboardFld.accepts( newEmuSys ) ) {
	this.keyboardFld.reset();
      } else {
	doClose();
      }
    } else {
      this.keyboardFld.reset();
    }
  }


  @Override
  public void windowActivated( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      Main.setWindowActivated( Main.WINDOW_MASK_KEYBOARD );
    }
  }


  @Override
  public void windowDeactivated( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      Main.setWindowDeactivated( Main.WINDOW_MASK_KEYBOARD );
    }
  }


	/* --- private Methoden --- */

  private void updWindowElements( EmuSys emuSys )
  {
    String kbName = this.keyboardFld.getKeyboardName();
    if( kbName != null ) {
      setTitle( String.format( "JKCEMU: %s", kbName ) );
    } else {
      setTitle( String.format( "JKCEMU: %s Tastatur", emuSys.getTitle() ) );
    }
    boolean state = this.keyboardFld.hasShiftKeys();
    this.mnuHoldShiftBtn.setVisible( state );
    this.mnuHoldShiftSep.setVisible( state );
  }
}
