/*
 * (c) 2010-2016 Jens Mueller
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
import java.lang.*;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import jkcemu.Main;
import jkcemu.base.BaseFrm;
import jkcemu.base.EmuThread;
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
	HelpFrm.open( HELP_PAGE );
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

    this.labelStatus0 = new JLabel( TEXT_NOT_CONNECTED );
    this.joyFld0      = new JoystickActionFld();
    this.btnConnect0  = new JButton( TEXT_CONNECT );
    this.btnConnect0.addActionListener( this );
    this.panel0 = createJoystickPanel(
				this.labelStatus0,
				this.joyFld0,
				this.btnConnect0,
				"Joystick 1" );
    add( this.panel0, gbc );

    this.labelStatus1 = new JLabel( TEXT_NOT_CONNECTED );
    this.joyFld1      = new JoystickActionFld();
    this.btnConnect1  = new JButton( TEXT_CONNECT );
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
