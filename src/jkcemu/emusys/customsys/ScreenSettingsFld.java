/*
 * (c) 2020-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen zur Bildschirmausgabe
 * des benutzerdefinierten Computers
 */

package jkcemu.emusys.customsys;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HexDocument;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.emusys.CustomSys;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.SettingsFrm;


public class ScreenSettingsFld
			extends AbstractSettingsFld
			implements ChangeListener, DocumentListener
{
  private static final String LABEL_COLS = "Anzahl Textspalten:";
  private static final String LABEL_ROWS = "Anzahl Textzeilen:";
  private static final String LABEL_BEGADDR
				= "Anfangsadresse Bildspeicher (hex):";

  private HexDocument        docScreenBegAddr;
  private JTextField         fldScreenBegAddr;
  private JComboBox<String>  comboScreenRows;
  private JComboBox<String>  comboScreenCols;
  private JCheckBox          cbScreenEnabled;
  private JLabel             labelScreenBegAddr;
  private JLabel             labelScreenCols;
  private JLabel             labelScreenRows;
  private JLabel             labelAltFontBitOrder;
  private JRadioButton       rbAltFontBit0Left;
  private JRadioButton       rbAltFontBit0Right;
  private ROMFileSettingsFld fldAltFont;


  public ScreenSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );
    setLayout( new GridBagLayout() );


    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbScreenEnabled = GUIFactory.createCheckBox(
					"Bildschirmausgabe emulieren" );
    add( this.cbScreenEnabled, gbc );

    this.labelScreenCols  = GUIFactory.createLabel( LABEL_COLS );
    gbc.insets.left = 50;
    gbc.gridwidth   = 1;
    gbc.gridy++;
    add( this.labelScreenCols, gbc );

    this.comboScreenCols = GUIFactory.createComboBox(
				new String[] { "32", "40", "64", "80" } );
    this.comboScreenCols.setEditable( false );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboScreenCols, gbc );

    this.labelScreenRows  = GUIFactory.createLabel( LABEL_ROWS );
    gbc.insets.left = 50;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.labelScreenRows, gbc );

    this.comboScreenRows = GUIFactory.createComboBox(
				new String[] { "16", "24", "25", "32" } );
    this.comboScreenRows.setEditable( false );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboScreenRows, gbc );

    this.labelScreenBegAddr = GUIFactory.createLabel( LABEL_BEGADDR );
    gbc.insets.left   = 50;
    gbc.insets.bottom = 5;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( this.labelScreenBegAddr, gbc );

    this.docScreenBegAddr = new HexDocument( 4, LABEL_BEGADDR );
    this.fldScreenBegAddr = GUIFactory.createTextField(
						this.docScreenBegAddr,
						5 );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.fldScreenBegAddr, gbc );

    Font font = this.fldScreenBegAddr.getFont();
    if( font != null ) {
      this.comboScreenCols.setFont( font );
      this.comboScreenRows.setFont( font );
    }

    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.weightx       = 1.0;
    gbc.insets.top    = 15;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( GUIFactory.createSeparator(), gbc );

    JPanel panelAltFont = GUIFactory.createPanel( new GridBagLayout() );
    gbc.gridy++;
    add( panelAltFont, gbc );

    GridBagConstraints gbcAltFont = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 0, 0, 0, 0 ),
					0, 0 );

    this.fldAltFont = new ROMFileSettingsFld(
			settingsFrm,
			propPrefix + CustomSys.PROP_FONT_PREFIX,
			"Alternativer Zeichensatz:" );
    panelAltFont.add( this.fldAltFont, gbcAltFont );

    this.labelAltFontBitOrder = GUIFactory.createLabel( "Bit-Anordnung:" );
    gbcAltFont.fill           = GridBagConstraints.NONE;
    gbcAltFont.weightx        = 0.0;
    gbcAltFont.gridwidth      = 1;
    gbcAltFont.gridy++;
    panelAltFont.add( this.labelAltFontBitOrder, gbcAltFont );

    ButtonGroup grpAltFontBitOrder = new ButtonGroup();

    this.rbAltFontBit0Left = GUIFactory.createRadioButton(
						"Bit 0 links",
						true );
    grpAltFontBitOrder.add( this.rbAltFontBit0Left );
    gbcAltFont.insets.top  = 5;
    gbcAltFont.insets.left = 5;
    gbcAltFont.gridx++;
    panelAltFont.add( this.rbAltFontBit0Left, gbcAltFont );

    this.rbAltFontBit0Right = GUIFactory.createRadioButton( "Bit 0 rechts" );
    grpAltFontBitOrder.add( this.rbAltFontBit0Right );
    gbcAltFont.gridx++;
    panelAltFont.add( this.rbAltFontBit0Right, gbcAltFont );

    this.cbScreenEnabled.addActionListener( this );
    this.comboScreenCols.addActionListener( this );
    this.comboScreenRows.addActionListener( this );
    this.docScreenBegAddr.addDocumentListener( this );
    this.fldAltFont.addChangeListener( this );
    this.rbAltFontBit0Left.addActionListener( this );
    this.rbAltFontBit0Right.addActionListener( this );
    updScreenFieldsEnabled();
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource() == this.fldAltFont )
      updAltFontFieldsEnabled();
  }


	/* --- DocumentListener --- */

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
    boolean screenEnabled = this.cbScreenEnabled.isSelected();
    int screenCols        = EmuUtil.getInt( this.comboScreenCols );
    int screenRows        = EmuUtil.getInt( this.comboScreenRows );
    int screenBegAddr     = CustomSys.DEFAULT_SCREEN_BEGADDR;
    try {
      screenBegAddr = this.docScreenBegAddr.intValue();
    }
    catch( NumberFormatException ex ) {
      if( selected && screenEnabled ) {
	throw new UserInputException(
		LABEL_BEGADDR + CustomSysSettingsFld.TEXT_INVALID_ADDR );
      }
    }
    if( selected && screenEnabled ) {
      if( screenCols < 1 ) {
	throw new UserInputException(
		LABEL_COLS + " Ung\u00FCltiger Wert" );
      }
      if( screenRows < 1 ) {
	throw new UserInputException(
		LABEL_ROWS + CustomSysSettingsFld.TEXT_INVALID_VALUE );
      }
      if( (screenBegAddr + (screenCols * screenRows)) > 0x10000 ) {
	throw new UserInputException(
		"Bildwiederholspeicher ragt \u00FCber"
			+ " die Adresse FFFFh hinaus." );
      }
    }
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SCREEN_ENABLED,
		screenEnabled );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SCREEN_BEGADDR,
		screenBegAddr );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SCREEN_COLS,
		screenCols );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SCREEN_ROWS,
		screenRows );
    this.fldAltFont.applyInput( props, selected );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_ALT_FONT_BIT0_RIGHT,
		this.rbAltFontBit0Right.isSelected() );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.cbScreenEnabled ) {
      rv = true;
      updScreenFieldsEnabled();
      fireDataChanged();
    } else if( (src == this.comboScreenRows)
	       || (src == this.comboScreenCols)
	       || (src == this.rbAltFontBit0Left)
	       || (src == this.rbAltFontBit0Right) )
    {
      rv = true;
      fireDataChanged();
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    this.cbScreenEnabled.setSelected( CustomSys.emulatesScreen( props ) );
    this.docScreenBegAddr.setValue(
		CustomSys.getScreenBegAddr( props ), 4 );
    this.comboScreenCols.setSelectedItem(
		Integer.toString( CustomSys.getScreenCols( props ) ) );
    this.comboScreenRows.setSelectedItem(
		Integer.toString( CustomSys.getScreenRows( props ) ) );
    if( CustomSys.emulatesAltFontBit0Right( props ) ) {
      this.rbAltFontBit0Right.setSelected( true );
    } else {
      this.rbAltFontBit0Left.setSelected( true );
    }
    this.fldAltFont.updFields( props );
    updScreenFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void docChanged( DocumentEvent e )
  {
    if( e.getDocument() == this.docScreenBegAddr )
      fireDataChanged();
  }


  private void updAltFontFieldsEnabled()
  {
    boolean state = this.cbScreenEnabled.isSelected()
			&& (this.fldAltFont.getFile() != null);
    this.labelAltFontBitOrder.setEnabled( state );
    this.rbAltFontBit0Left.setEnabled( state );
    this.rbAltFontBit0Right.setEnabled( state );
  }


  private void updScreenFieldsEnabled()
  {
    boolean state = this.cbScreenEnabled.isSelected();
    this.labelScreenBegAddr.setEnabled( state );
    this.labelScreenCols.setEnabled( state );
    this.labelScreenRows.setEnabled( state );
    this.fldScreenBegAddr.setEnabled( state );
    this.comboScreenCols.setEnabled( state );
    this.comboScreenRows.setEnabled( state );
    this.fldAltFont.setEnabled( state );
    this.rbAltFontBit0Left.setEnabled( state );
    this.rbAltFontBit0Right.setEnabled( state );
    updAltFontFieldsEnabled();
  }
}
