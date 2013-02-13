/*
 * (c) 2011-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fensterklasse fuer eine Tastaturansicht
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class KeyboardFrm extends BasicFrm
{
  private ScreenFrm           screenFrm;
  private AbstractKeyboardFld keyboardFld;
  private JMenuItem           mnuClose;
  private JCheckBoxMenuItem   mnuHoldShiftBtn;
  private JSeparator          mnuHoldShiftSep;
  private JMenuItem           mnuHelpContent;


  public KeyboardFrm(
		ScreenFrm           screenFrm,
		EmuSys              emuSys,
		AbstractKeyboardFld keyboardFld )
  {
    this.screenFrm   = screenFrm;
    this.keyboardFld = keyboardFld;
    Main.updIcon( this );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuHoldShiftBtn = new JCheckBoxMenuItem(
			"Shift- und Control-Tasten gedr\u00FCckt halten",
			this.keyboardFld.getHoldShift() );
    this.mnuHoldShiftBtn.addActionListener( this );
    mnuFile.add( this.mnuHoldShiftBtn );

    this.mnuHoldShiftSep = new JSeparator();
    mnuFile.add( this.mnuHoldShiftSep );

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menuleiste
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


    // Fensterinhalt
    setLayout( new BorderLayout() );

    this.keyboardFld = keyboardFld;
    this.keyboardFld.setFocusable( true );
    this.keyboardFld.setFocusTraversalKeysEnabled( false );
    this.keyboardFld.addKeyListener( this );
    add( this.keyboardFld, BorderLayout.CENTER );


    // sonstiges
    pack();
    if( !applySettings( Main.getProperties(), false ) ) {
      setScreenCentered();
    }
    setResizable( false );
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
	  HelpFrm.open( "/help/keyboard.htm" );
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
    if( e.getKeyCode() == KeyEvent.VK_SHIFT ) {
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
    if( e.getKeyCode() == KeyEvent.VK_SHIFT ) {
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
  public void resetFired()
  {
    this.keyboardFld.reset();
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

