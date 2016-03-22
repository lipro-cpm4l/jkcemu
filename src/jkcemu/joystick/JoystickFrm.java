/*
 * (c) 2010-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster zur Anzeige/Verwaltung der Joysticks
 */

package jkcemu.joystick;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class JoystickFrm extends BasicFrm
{
  private static final String textNotEmulated  = "Nicht emuliert";
  private static final String textNotConnected = "Nicht verbunden";
  private static final String textConnected    = "Aktiv";
  private static final String textConnect      = "Verbinden";
  private static final String textDisconnect   = "Trennen";

  private static JoystickFrm instance = null;

  private EmuThread         emuThread;
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
    String btnText    = textConnect;
    String statusText = textNotEmulated;
    if( emulated ) {
      if( connected ) {
	btnText    = textDisconnect;
	statusText = textConnected;
      } else {
	statusText = textNotConnected;
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
    if( e != null ) {
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
	HelpFrm.open( "/help/joystick.htm" );
      }
    }
    return rv;
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    Component c = e.getComponent();
    if( c == this.joyFld0 ) {
      this.emuThread.setJoystickAction(
		0,
		this.joyFld0.getJoystickAction( e.getX(), e.getY() ) );
      e.consume();
    } else if( c == this.joyFld1 ) {
      this.emuThread.setJoystickAction(
		1,
		this.joyFld1.getJoystickAction( e.getX(), e.getY() ) );
      e.consume();
    } else {
      super.mousePressed( e );
    }
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    Component c = e.getComponent();
    if( c == this.joyFld0 ) {
      this.emuThread.setJoystickAction( 0, 0 );
      e.consume();
    } else if( c == this.joyFld1 ) {
      this.emuThread.setJoystickAction( 1, 0 );
      e.consume();
    } else {
      super.mousePressed( e );
    }
  }


  @Override
  public void windowActivated( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      Main.setWindowActivated( Main.WINDOW_MASK_JOYSTICK );
    }
  }


  @Override
  public void windowDeactivated( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      Main.setWindowDeactivated( Main.WINDOW_MASK_JOYSTICK );
    }
  }


	/* --- Konstruktor --- */

  private JoystickFrm( EmuThread emuThread )
  {
    this.emuThread = emuThread;
    setTitle( "JKCEMU Joysticks" );
    Main.updIcon( this );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


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

    this.labelStatus0 = new JLabel( textNotConnected );
    this.joyFld0      = new JoystickActionFld();
    this.btnConnect0  = new JButton( textConnect );
    this.btnConnect0.addActionListener( this );
    this.panel0 = createJoystickPanel(
				this.labelStatus0,
				this.joyFld0,
				this.btnConnect0,
				"Joystick 1" );
    add( this.panel0, gbc );

    this.labelStatus1 = new JLabel( textNotConnected );
    this.joyFld1      = new JoystickActionFld();
    this.btnConnect1  = new JButton( textConnect );
    this.btnConnect1.addActionListener( this );
    this.panel1 = createJoystickPanel(
				this.labelStatus1,
				this.joyFld1,
				this.btnConnect1,
				"Joystick 2" );
    gbc.gridx++;
    add( this.panel1, gbc );


    // Fenstergroesse
    pack();
    if( !applySettings( Main.getProperties(), false ) ) {
      setLocationByPlatform( true );
    }
    setResizable( false );


    // Groessenanpassungen der beiden Panels verhindern
    Dimension prefSize = this.panel0.getPreferredSize();
    if( prefSize != null ) {
      this.panel0.setPreferredSize( prefSize );
    }
    prefSize = this.panel1.getPreferredSize();
    if( prefSize != null ) {
      this.panel1.setPreferredSize( prefSize );
    }


    // sonstiges
    if( this.emuThread != null ) {
      this.emuThread.setJoystickFrm( this );
      this.joyFld0.addMouseListener( this );
      this.joyFld1.addMouseListener( this );
    }
  }


	/* --- private Methoden --- */

  private JPanel createJoystickPanel(
			JLabel            label,
			JoystickActionFld joyFld,
			JButton           btn,
			String            title )
  {
    JPanel panel = new JPanel( new GridBagLayout() );
    panel.setBorder( BorderFactory.createTitledBorder( title ) );

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
}

