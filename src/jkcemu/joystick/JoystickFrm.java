/*
 * (c) 2010-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zur Anzeige/Verwaltung der Joysticks
 */

package jkcemu.joystick;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import jkcemu.Main;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;


public class JoystickFrm extends BaseFrm
{
  private static final String HELP_PAGE          = "/help/joystick.htm";
  private static final String TEXT_NOT_EMULATED  = "Nicht emuliert";
  private static final String TEXT_NOT_CONNECTED = "Nicht verbunden";
  private static final String TEXT_CONNECTED     = "Aktiv";
  private static final String TEXT_CONNECT       = "Verbinden";
  private static final String TEXT_DISCONNECT    = "Trennen";

  private static JoystickFrm instance = null;

  private EmuThread         emuThread;
  private boolean           joyActionByKey;
  private JPanel            panel0;
  private JPanel            panel1;
  private JLabel            labelStatus0;
  private JLabel            labelStatus1;
  private JoystickActionFld joyFld0;
  private JoystickActionFld joyFld1;
  private JButton           btnConnect0;
  private JButton           btnConnect1;
  private JMenuItem         mnuClose;
  private JMenuItem         mnuHelpContent;


  public static void open( EmuThread emuThread )
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new JoystickFrm( emuThread );
    }
    instance.toFront();
    instance.setVisible( true );
  }


  public static int getJoystickActionByKeyCode( int keyCode )
  {
    int action = 0;
    switch( keyCode ) {
      case KeyEvent.VK_LEFT:
	action = JoystickThread.LEFT_MASK;
	break;
      case KeyEvent.VK_RIGHT:
	action = JoystickThread.RIGHT_MASK;
	break;
      case KeyEvent.VK_UP:
	action = JoystickThread.UP_MASK;
	break;
      case KeyEvent.VK_DOWN:
	action = JoystickThread.DOWN_MASK;
	break;
      case KeyEvent.VK_F1:
      case KeyEvent.VK_ENTER:
	action = JoystickThread.BUTTON1_MASK;
	break;
      case KeyEvent.VK_F2:
	action = JoystickThread.BUTTON2_MASK;
	break;
    }
    return action;
  }


  public void setJoystickAction( int joyNum, int actionMask )
  {
    if( joyNum == 0 ) {
      this.joyFld0.setJoystickAction( actionMask );
    } else if( joyNum == 1 ) {
      this.joyFld1.setJoystickAction( actionMask );
    }
  }


  public void setJoystickState(
			int     joyNum,
			boolean emulated,
			boolean connected )
  {
    String btnText    = TEXT_CONNECT;
    String statusText = TEXT_NOT_EMULATED;
    if( emulated ) {
      if( connected ) {
	btnText    = TEXT_DISCONNECT;
	statusText = TEXT_CONNECTED;
      } else {
	statusText = TEXT_NOT_CONNECTED;
      }
    }
    if( joyNum == 0 ) {
      this.labelStatus0.setText( statusText );
      this.joyFld0.setEnabled( emulated );
      this.btnConnect0.setText( btnText );
      this.btnConnect0.setEnabled( emulated );
    } else if( joyNum == 1 ) {
      this.labelStatus1.setText( statusText );
      this.joyFld1.setEnabled( emulated );
      this.btnConnect1.setText( btnText );
      this.btnConnect1.setEnabled( emulated );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    Object src = e.getSource();
    if( src == this.btnConnect0 ) {
      rv = true;
      doConnect( 0 );
    }
    else if( src == this.btnConnect1 ) {
      rv = true;
      doConnect( 1 );
    }
    else if( src == this.mnuClose ) {
      rv = true;
      doClose();
    }
    else if( src == this.mnuHelpContent ) {
      rv = true;
      HelpFrm.openPage( HELP_PAGE );
    }
    return rv;
  }


  @Override
  public void keyPressed( KeyEvent e )
  {
    Component c = e.getComponent();
    if( !e.isShiftDown() ) {
      if( processJoystickAction( 0, e ) ) {
	e.consume();
      }
    } else if( e.isShiftDown() ) {
      if( processJoystickAction( 1, e ) ) {
	e.consume();
      }
    }
    if( !e.isConsumed() ) {
      super.keyPressed( e );
    }
  }


  @Override
  public void keyReleased( KeyEvent e )
  {
    super.keyPressed( e );
    if( this.joyActionByKey ) {
      this.joyActionByKey = false;
      if( this.emuThread != null ) {
	this.emuThread.setJoystickAction( 0, 0 );
	this.emuThread.setJoystickAction( 1, 0 );
      }
    }
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    Component c = e.getComponent();
    if( (c == this.joyFld0) && (this.emuThread != null) ) {
      this.emuThread.setJoystickAction(
		0,
		this.joyFld0.getJoystickAction( e.getX(), e.getY() ) );
      e.consume();
    } else if( (c == this.joyFld1) && (this.emuThread != null) ) {
      this.emuThread.setJoystickAction(
		1,
		this.joyFld1.getJoystickAction( e.getX(), e.getY() ) );
      e.consume();
    }
    if( !e.isConsumed() ) {
      super.mousePressed( e );
    }
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    Component c = e.getComponent();
    if( (c == this.joyFld0) && (this.emuThread != null) ) {
      this.emuThread.setJoystickAction( 0, 0 );
      e.consume();
    } else if( (c == this.joyFld1) && (this.emuThread != null) ) {
      this.emuThread.setJoystickAction( 1, 0 );
      e.consume();
    }
    if( !e.isConsumed() ) {
      super.mousePressed( e );
    }
  }


  @Override
  public void resetFired( EmuSys newEmuSys, Properties props )
  {
    if( newEmuSys != null ) {
      if( newEmuSys.getSupportedJoystickCount() < 1 ) {
	doClose();
      }
    }
  }


  @Override
  public void windowActivated( WindowEvent e )
  {
    super.windowActivated( e );
    if( e.getWindow() == this ) {
      Main.setWindowActivated( Main.WINDOW_MASK_JOYSTICK );
    }
  }


  @Override
  public void windowDeactivated( WindowEvent e )
  {
    super.windowDeactivated( e );
    if( e.getWindow() == this ) {
      Main.setWindowDeactivated( Main.WINDOW_MASK_JOYSTICK );
    }
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    super.windowOpened( e );
    if( (e.getWindow() == this) && (this.joyFld0 != null) )
      this.joyFld0.requestFocus();
  }


	/* --- Konstruktor --- */

  private JoystickFrm( EmuThread emuThread )
  {
    this.emuThread      = emuThread;
    this.joyActionByKey = true;
    setTitle( "JKCEMU Joysticks" );


    // Menu Datei
    JMenu mnuFile = createMenuFile();
    this.mnuClose = createMenuItemClose();
    mnuFile.add( this.mnuClose );


    // Menu Hilfe
    JMenu mnuHelp       = createMenuHelp();
    this.mnuHelpContent = createMenuItem( "Hilfe zu Joysticks..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuHelp ) );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.WEST,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.labelStatus0 = GUIFactory.createLabel( TEXT_NOT_CONNECTED );
    this.joyFld0      = new JoystickActionFld();
    this.joyFld0.setFocusable( true );
    this.btnConnect0 = GUIFactory.createButton( TEXT_CONNECT );
    this.panel0      = createJoystickPanel(
				this.labelStatus0,
				this.joyFld0,
				this.btnConnect0,
				"Joystick 1" );
    add( this.panel0, gbc );

    this.labelStatus1 = GUIFactory.createLabel( TEXT_NOT_CONNECTED );
    this.joyFld1      = new JoystickActionFld();
    this.joyFld1.setFocusable( true );
    this.btnConnect1 = GUIFactory.createButton( TEXT_CONNECT );
    this.panel1      = createJoystickPanel(
				this.labelStatus1,
				this.joyFld1,
				this.btnConnect1,
				"Joystick 2" );
    gbc.gridx++;
    add( this.panel1, gbc );


    // Fenstergroesse
    pack();
    setResizable( false );
    if( !applySettings( Main.getProperties() ) ) {
      setLocationByPlatform( true );
    }


    // Groessenanpassungen der beiden Panels verhindern
    Dimension prefSize = this.panel0.getPreferredSize();
    if( prefSize != null ) {
      this.panel0.setPreferredSize( prefSize );
    }
    prefSize = this.panel1.getPreferredSize();
    if( prefSize != null ) {
      this.panel1.setPreferredSize( prefSize );
    }


    // Listener
    this.joyFld0.addKeyListener( this );
    this.joyFld1.addKeyListener( this );
    this.joyFld0.addMouseListener( this );
    this.joyFld1.addMouseListener( this );
    this.btnConnect0.addActionListener( this );
    this.btnConnect1.addActionListener( this );


    // sonstiges
    if( this.emuThread != null ) {
      this.emuThread.setJoystickFrm( this );
    }
  }


	/* --- private Methoden --- */

  private JPanel createJoystickPanel(
			JLabel            label,
			JoystickActionFld joyFld,
			JButton           btn,
			String            title )
  {
    JPanel panel = GUIFactory.createPanel( new GridBagLayout() );
    panel.setBorder( GUIFactory.createTitledBorder( title ) );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );
    panel.add( label );
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.gridy++;
    panel.add( joyFld, gbc );
    gbc.gridy++;
    panel.add( btn, gbc );
    return panel;
  }


  private void doConnect( int joyNum )
  {
    if( this.emuThread != null )
      this.emuThread.changeJoystickConnectState( joyNum );
  }


  private boolean processJoystickAction( int joyNum, KeyEvent e )
  {
    boolean rv = false;
    if( this.emuThread != null ) {
      int action = getJoystickActionByKeyCode( e.getKeyCode() );
      if( action != 0 ) {
	this.emuThread.setJoystickAction( joyNum, action );
	this.joyActionByKey = true;
	rv                  = true;
      }
    }
    return rv;
  }
}
