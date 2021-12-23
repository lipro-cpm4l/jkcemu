/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen des benutzerdefinierten Computers
 */

package jkcemu.emusys.customsys;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HexDocument;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.emusys.CustomSys;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.SettingsFrm;


public class KeyboardSettingsFld
			extends AbstractSettingsFld
			implements DocumentListener
{
  private CustomSysSettingsFld csSettingsFld;
  private boolean              connectedToPIO;
  private boolean              connectedToSIO;
  private HexDocument          docIOAddr;
  private JTextField           fldIOAddr;
  private JCheckBox            cbSwapCase;
  private JRadioButton         rbNoKeyboard;
  private JRadioButton         rbPortRaw;
  private JRadioButton         rbPioAhs;
  private JRadioButton         rbPioAbit7;
  private JRadioButton         rbPioBhs;
  private JRadioButton         rbPioBbit7;
  private JRadioButton         rbSioA;
  private JRadioButton         rbSioB;


  public KeyboardSettingsFld(
			CustomSysSettingsFld csSettingsFld,
			String               propPrefix )
  {
    super( csSettingsFld.getSettingsFrm(), propPrefix );
    this.csSettingsFld  = csSettingsFld;
    this.connectedToPIO = false;
    this.connectedToSIO = false;

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    add( GUIFactory.createLabel( "Tastatur ist angeschlossen an:" ), gbc );

    ButtonGroup grpKeyboard = new ButtonGroup();

    this.rbPortRaw = GUIFactory.createRadioButton(
			"Einfaches Eingabetor an E/A-Adresse (hex):" );
    grpKeyboard.add( this.rbPortRaw );
    gbc.insets.left = 50;
    gbc.gridwidth   = 1;
    gbc.gridy++;
    add( this.rbPortRaw, gbc );

    this.docIOAddr  = new HexDocument( 2, this.rbPortRaw.getText() );
    this.fldIOAddr  = GUIFactory.createTextField( this.docIOAddr, 3 );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.fldIOAddr, gbc );

    this.rbPioAhs = GUIFactory.createRadioButton(
			"PIO Port A mit Ready/Strobe-Handshake" );
    grpKeyboard.add( this.rbPioAhs );
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.rbPioAhs, gbc );

    this.rbPioAbit7 = GUIFactory.createRadioButton(
			"PIO Port A mit Strobe an Bit 7" );
    grpKeyboard.add( this.rbPioAbit7 );
    gbc.gridy++;
    add( this.rbPioAbit7, gbc );

    this.rbPioBhs = GUIFactory.createRadioButton(
			"PIO Port B mit Ready/Strobe-Handshake" );
    grpKeyboard.add( this.rbPioBhs );
    gbc.gridy++;
    add( this.rbPioBhs, gbc );

    this.rbPioBbit7 = GUIFactory.createRadioButton(
			"PIO Port B mit Strobe an Bit 7" );
    grpKeyboard.add( this.rbPioBbit7 );
    gbc.gridy++;
    add( this.rbPioBbit7, gbc );

    this.rbSioA = GUIFactory.createRadioButton( "SIO Kanal A" );
    grpKeyboard.add( this.rbSioA );
    gbc.gridy++;
    add( this.rbSioA, gbc );

    this.rbSioB = GUIFactory.createRadioButton( "SIO Kanal B" );
    grpKeyboard.add( this.rbSioB );
    gbc.gridy++;
    add( this.rbSioB, gbc );

    this.rbNoKeyboard = GUIFactory.createRadioButton(
					"Keine Tastatur emulieren" );
    grpKeyboard.add( this.rbNoKeyboard );
    gbc.gridy++;
    add( this.rbNoKeyboard, gbc );

    this.cbSwapCase = GUIFactory.createCheckBox(
			"Gro\u00DF-/Kleinschreibung umkehren" );
    gbc.insets.top    = 10;
    gbc.insets.left   = 5;
    gbc.insets.bottom = 5;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( this.cbSwapCase, gbc );

    this.rbNoKeyboard.addActionListener( this );
    this.rbPortRaw.addActionListener( this );
    this.rbPioAhs.addActionListener( this );
    this.rbPioAbit7.addActionListener( this );
    this.rbPioBhs.addActionListener( this );
    this.rbPioBbit7.addActionListener( this );
    this.rbSioA.addActionListener( this );
    this.rbSioB.addActionListener( this );
    this.cbSwapCase.addActionListener( this );
    this.docIOAddr.addDocumentListener( this );
    updFieldsEnabled();
  }


  public boolean isKeyboardConnectedToPIO()
  {
    return this.connectedToPIO;
  }


  public boolean isKeyboardConnectedToSIO()
  {
    return this.connectedToSIO;
  }


	/* --- DocumentEvent --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws
					UserCancelException,
					UserInputException
  {
    boolean connectedToPIO = false;
    boolean connectedToSIO = false;
    String  keyboardHW     = CustomSys.VALUE_NONE;
    if( this.rbPortRaw.isSelected() ) {
      keyboardHW = CustomSys.VALUE_KEYBOARD_PORT_RAW;
    } else if( this.rbPioAhs.isSelected() ) {
      connectedToPIO = true;
      keyboardHW     = CustomSys.VALUE_KEYBOARD_PIO_A_HS;
    } else if( this.rbPioAbit7.isSelected() ) {
      connectedToPIO = true;
      keyboardHW     = CustomSys.VALUE_KEYBOARD_PIO_A_BIT7;
    } else if( this.rbPioBhs.isSelected() ) {
      connectedToPIO = true;
      keyboardHW     = CustomSys.VALUE_KEYBOARD_PIO_B_HS;
    } else if( this.rbPioBbit7.isSelected() ) {
      connectedToPIO = true;
      keyboardHW     = CustomSys.VALUE_KEYBOARD_PIO_B_BIT7;
    } else if( this.rbSioA.isSelected() ) {
      connectedToSIO = true;
      keyboardHW     = CustomSys.VALUE_KEYBOARD_SIO_A;
    } else if( this.rbSioB.isSelected() ) {
      connectedToSIO = true;
      keyboardHW     = CustomSys.VALUE_KEYBOARD_SIO_B;
    }
    this.csSettingsFld.applyIOAddrInput(
				selected,
				"Eingabeport Tastatur",
				this.rbPortRaw,
				null,
				this.docIOAddr,
				1,
				CustomSys.PROP_KEYBOARD_IOADDR,
				props );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_KEYBOARD_HW,
		keyboardHW );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SWAP_KEY_CHAR_CASE,
		this.cbSwapCase.isSelected() );
    this.connectedToPIO = connectedToPIO;
    this.connectedToSIO = connectedToSIO;
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( (src == this.rbNoKeyboard)
	   || (src == this.rbPortRaw)
	   || (src == this.rbPioAhs)
	   || (src == this.rbPioAbit7)
	   || (src == this.rbPioBhs)
	   || (src == this.rbPioBbit7)
	   || (src == this.rbSioA)
	   || (src == this.rbSioB) )
      {
	rv = true;
	updFieldsEnabled();
	fireDataChanged();
      }
      else if( src == this.cbSwapCase ) {
	rv = true;
	fireDataChanged();
      }
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    switch( EmuUtil.getProperty(
			props,
			this.propPrefix + CustomSys.PROP_KEYBOARD_HW ) )
    {
      case CustomSys.VALUE_KEYBOARD_PORT_RAW:
	this.rbPortRaw.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_PIO_A_HS:
	this.rbPioAhs.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_PIO_A_BIT7:
	this.rbPioAbit7.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_PIO_B_HS:
	this.rbPioBhs.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_PIO_B_BIT7:
	this.rbPioBbit7.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_SIO_A:
	this.rbSioA.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_SIO_B:
	this.rbSioB.setSelected( true );
	break;
      default:
	this.rbNoKeyboard.setSelected( true );
    }
    this.docIOAddr.setValue( CustomSys.getKeyboardIOAddr( props ), 2 );
    this.cbSwapCase.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + CustomSys.PROP_SWAP_KEY_CHAR_CASE,
			CustomSys.DEFAULT_SWAP_KEY_CHAR_CASE ) );
    updFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void docChanged( DocumentEvent e )
  {
    if( e.getDocument() == this.docIOAddr )
      fireDataChanged();
  }


  private void updFieldsEnabled()
  {
    this.fldIOAddr.setEnabled( this.rbPortRaw.isSelected() );
  }
}
