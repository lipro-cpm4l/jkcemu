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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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


public class IOSettingsFld
			extends AbstractSettingsFld
			implements DocumentListener
{
  private static final String LABEL_UNUSED_PORT_VALUE
		= "Gelesener Wert von nicht belegten E/A-Adressen (hex):";

  private static final String[] SIO_CLOCK_ITEMS = {
					"CTC Ausgang 0",
					"CTC Ausgang 1",
					"CTC Ausgang 2" };

  private static final String[] SIO_OUT_ITEMS = {
					"nichts angeschlossen",
					"Drucker" };

  private CustomSysSettingsFld csSettingsFld;
  private int                  pioIOBaseAddr;
  private HexDocument          docCtcIOBaseAddr;
  private HexDocument          docPioIOBaseAddr;
  private HexDocument          docSioIOBaseAddr;
  private HexDocument          docK1520SoundIOBaseAddr;
  private HexDocument          docKCNetIOBaseAddr;
  private HexDocument          docVdipIOBaseAddr;
  private HexDocument          docUnusedPortValue;
  private JTextField           fldCtcIOBaseAddr;
  private JTextField           fldPioIOBaseAddr;
  private JTextField           fldSioIOBaseAddr;
  private JTextField           fldK1520SoundIOBaseAddr;
  private JTextField           fldKCNetIOBaseAddr;
  private JTextField           fldVdipIOBaseAddr;
  private JTextField           fldUnusedPortValue;
  private JComboBox<String>    comboSioClockA;
  private JComboBox<String>    comboSioClockB;
  private JComboBox<String>    comboSioOutA;
  private JComboBox<String>    comboSioOutB;
  private JCheckBox            cbCtcEnabled;
  private JCheckBox            cbPioEnabled;
  private JCheckBox            cbSioEnabled;
  private JCheckBox            cbK1520SoundEnabled;
  private JCheckBox            cbKCNetEnabled;
  private JCheckBox            cbVdipEnabled;
  private JLabel               labelSioClockA;
  private JLabel               labelSioClockB;
  private JLabel               labelSioOutA;
  private JLabel               labelSioOutB;


  public IOSettingsFld(
		CustomSysSettingsFld csSettingsFld,
		String               propPrefix )
  {
    super( csSettingsFld.getSettingsFrm(), propPrefix );
    this.csSettingsFld = csSettingsFld;

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					2, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbPioEnabled = GUIFactory.createCheckBox(
		"PIO emulieren an E/A-Basisadresse (hex):" );
    add( this.cbPioEnabled, gbc );

    this.docPioIOBaseAddr = new HexDocument(
					2,
					this.cbPioEnabled.getText() );
    this.fldPioIOBaseAddr = GUIFactory.createTextField(
					this.docPioIOBaseAddr,
					3 );
    gbc.insets.top = 2;
    gbc.gridwidth  = 1;
    gbc.gridx += 2;
    add( this.fldPioIOBaseAddr, gbc );

    this.cbSioEnabled = GUIFactory.createCheckBox(
		"SIO emulieren an E/A-Basisadresse (hex):" );
    gbc.gridwidth = 2;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( this.cbSioEnabled, gbc );

    this.docSioIOBaseAddr = new HexDocument(
					2,
					this.cbSioEnabled.getText() );
    this.fldSioIOBaseAddr = GUIFactory.createTextField(
					this.docSioIOBaseAddr,
					3 );
    gbc.gridwidth = 1;
    gbc.gridx += 2;
    add( this.fldSioIOBaseAddr, gbc );

    this.labelSioClockA = GUIFactory.createLabel(
					"Kanal A getaktet durch:" );
    gbc.insets.left   = 50;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( this.labelSioClockA, gbc );

    this.comboSioClockA = GUIFactory.createComboBox( SIO_CLOCK_ITEMS );
    this.comboSioClockA.setEditable( false );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboSioClockA, gbc );

    this.labelSioClockB = GUIFactory.createLabel(
					"Kanal B getaktet durch:" );
    gbc.insets.left   = 50;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( this.labelSioClockB, gbc );

    this.comboSioClockB = GUIFactory.createComboBox( SIO_CLOCK_ITEMS );
    this.comboSioClockB.setEditable( false );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboSioClockB, gbc );

    this.labelSioOutA = GUIFactory.createLabel( "Kanal A Ausgang:" );
    gbc.insets.left = 50;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.labelSioOutA, gbc );

    this.comboSioOutA = GUIFactory.createComboBox( SIO_OUT_ITEMS );
    this.comboSioOutA.setEditable( false );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboSioOutA, gbc );

    this.labelSioOutB = GUIFactory.createLabel( "Kanal B Ausgang:" );
    gbc.insets.left = 50;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.labelSioOutB, gbc );

    this.comboSioOutB = GUIFactory.createComboBox( SIO_OUT_ITEMS );
    this.comboSioOutB.setEditable( false );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboSioOutB, gbc );

    this.cbCtcEnabled = GUIFactory.createCheckBox(
		"CTC emulieren an E/A-Basisadresse (hex):" );
    gbc.gridwidth = 2;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( this.cbCtcEnabled, gbc );

    this.docCtcIOBaseAddr = new HexDocument(
					2,
					this.cbCtcEnabled.getText() );
    this.fldCtcIOBaseAddr = GUIFactory.createTextField(
					this.docCtcIOBaseAddr,
					3 );
    gbc.gridwidth = 1;
    gbc.gridx += 2;
    add( this.fldCtcIOBaseAddr, gbc );

    this.cbK1520SoundEnabled = GUIFactory.createCheckBox(
		"K1520-Sound-Karte emulieren an E/A-Basisadresse (hex):" );
    gbc.gridwidth = 2;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( this.cbK1520SoundEnabled, gbc );

    this.docK1520SoundIOBaseAddr = new HexDocument(
					2,
					this.cbK1520SoundEnabled.getText() );
    this.fldK1520SoundIOBaseAddr = GUIFactory.createTextField(
					this.docK1520SoundIOBaseAddr,
					3 );
    gbc.gridwidth = 1;
    gbc.gridx += 2;
    add( this.fldK1520SoundIOBaseAddr, gbc );

    this.cbKCNetEnabled = GUIFactory.createCheckBox(
		"KCNet emulieren an E/A-Basisadresse (hex):" );
    gbc.gridwidth = 2;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( this.cbKCNetEnabled, gbc );

    this.docKCNetIOBaseAddr = new HexDocument(
					2,
					this.cbKCNetEnabled.getText() );
    this.fldKCNetIOBaseAddr = GUIFactory.createTextField(
					this.docKCNetIOBaseAddr,
					3 );
    gbc.gridwidth = 1;
    gbc.gridx += 2;
    add( this.fldKCNetIOBaseAddr, gbc );

    this.cbVdipEnabled = GUIFactory.createCheckBox(
		"USB (VDIP) emulieren an E/A-Basisadresse (hex):" );
    gbc.gridwidth = 2;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( this.cbVdipEnabled, gbc );

    this.docVdipIOBaseAddr = new HexDocument(
					2,
					this.cbVdipEnabled.getText() );
    this.fldVdipIOBaseAddr = GUIFactory.createTextField(
					this.docVdipIOBaseAddr,
					3 );
    gbc.gridwidth = 1;
    gbc.gridx += 2;
    add( this.fldVdipIOBaseAddr, gbc );

    gbc.insets.top    = 10;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = 2;
    gbc.gridx         = 0;
    gbc.gridy++;
    add(
		GUIFactory.createLabel( LABEL_UNUSED_PORT_VALUE ),
		gbc );

    this.docUnusedPortValue = new HexDocument( 2, LABEL_UNUSED_PORT_VALUE );
    this.fldUnusedPortValue = GUIFactory.createTextField(
					this.docUnusedPortValue,
					3 );
    gbc.gridx += 2;
    add( this.fldUnusedPortValue, gbc );

    this.cbPioEnabled.addActionListener( this );
    this.cbSioEnabled.addActionListener( this );
    this.cbCtcEnabled.addActionListener( this );
    this.cbK1520SoundEnabled.addActionListener( this );
    this.cbKCNetEnabled.addActionListener( this );
    this.cbVdipEnabled.addActionListener( this );

    this.comboSioClockA.addActionListener( this );
    this.comboSioClockB.addActionListener( this );
    this.comboSioOutA.addActionListener( this );
    this.comboSioOutB.addActionListener( this );

    this.docPioIOBaseAddr.addDocumentListener( this );
    this.docSioIOBaseAddr.addDocumentListener( this );
    this.docCtcIOBaseAddr.addDocumentListener( this );
    this.docK1520SoundIOBaseAddr.addDocumentListener( this );
    this.docKCNetIOBaseAddr.addDocumentListener( this );
    this.docVdipIOBaseAddr.addDocumentListener( this );
    this.docUnusedPortValue.addDocumentListener( this );

    updPioFieldsEnabled();
    updSioFieldsEnabled();
    updCtcFieldsEnabled();
    updK1520SoundFieldsEnabled();
    updKCNetFieldsEnabled();
    updVdipFieldsEnabled();
  }


  public boolean isPIOEnabled()
  {
    return this.cbPioEnabled.isSelected();
  }


  public boolean isSIOEnabled()
  {
    return this.cbSioEnabled.isSelected();
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
    if( selected
	&& this.cbSioEnabled.isSelected()
	&& !this.cbCtcEnabled.isSelected() )
    {
      throw new UserInputException(
		"Wenn eine SIO emuliert wird,\n"
			+ "m\u00FCssen Sie auch die Emulation der CTC"
			+ " aktivieren,\n"
			+ "da diese die SIO taktet." );
    }
    this.csSettingsFld.applyIOAddrInput(
				selected,
				"PIO",
				this.cbPioEnabled,
				CustomSys.PROP_PIO_ENABLED,
				this.docPioIOBaseAddr,
				4,
				CustomSys.PROP_PIO_IOBASEADDR,
				props );
    this.csSettingsFld.applyIOAddrInput(
				selected,
				"SIO",
				this.cbSioEnabled,
				CustomSys.PROP_SIO_ENABLED,
				this.docSioIOBaseAddr,
				4,
				CustomSys.PROP_SIO_IOBASEADDR,
				props );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SIO_A_CLOCK,
		this.comboSioClockA.getSelectedIndex() );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SIO_B_CLOCK,
		this.comboSioClockB.getSelectedIndex() );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SIO_A_OUT,
		getSioOutValue( this.comboSioOutA ) );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SIO_B_OUT,
		getSioOutValue( this.comboSioOutB ) );
    this.csSettingsFld.applyIOAddrInput(
				selected,
				"CTC",
				this.cbCtcEnabled,
				CustomSys.PROP_CTC_ENABLED,
				this.docCtcIOBaseAddr,
				4,
				CustomSys.PROP_CTC_IOBASEADDR,
				props );
    this.csSettingsFld.applyIOAddrInput(
				selected,
				"K1520-Sound-Karte",
				this.cbK1520SoundEnabled,
				CustomSys.PROP_K1520SOUND_ENABLED,
				this.docK1520SoundIOBaseAddr,
				4,
				CustomSys.PROP_K1520SOUND_IOBASEADDR,
				props );
    this.csSettingsFld.applyIOAddrInput(
				selected,
				"KCNet",
				this.cbKCNetEnabled,
				CustomSys.PROP_KCNET_ENABLED,
				this.docKCNetIOBaseAddr,
				4,
				CustomSys.PROP_KCNET_IOBASEADDR,
				props );
    this.csSettingsFld.applyIOAddrInput(
				selected,
				"USB",
				this.cbVdipEnabled,
				CustomSys.PROP_VDIP_ENABLED,
				this.docVdipIOBaseAddr,
				4,
				CustomSys.PROP_VDIP_IOBASEADDR,
				props );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_UNUSED_PORT_VALUE,
		this.docUnusedPortValue.intValue() );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.cbPioEnabled ) {
      rv = true;
      updPioFieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbSioEnabled ) {
      rv = true;
      updSioFieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbCtcEnabled ) {
      rv = true;
      updCtcFieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbK1520SoundEnabled ) {
      rv = true;
      updK1520SoundFieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbKCNetEnabled ) {
      rv = true;
      updKCNetFieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbVdipEnabled ) {
      rv = true;
      updVdipFieldsEnabled();
      fireDataChanged();
    }
    else if( (src == this.comboSioClockA)
	     || (src == this.comboSioClockB)
	     || (src == this.comboSioOutA)
	     || (src == this.comboSioOutB) )
    {
      rv = true;
      fireDataChanged();
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    this.cbPioEnabled.setSelected( CustomSys.emulatesPIO( props ) );
    this.docPioIOBaseAddr.setValue(
		CustomSys.getPioIOBaseAddr( props ), 2 );
    updPioFieldsEnabled();

    this.cbSioEnabled.setSelected( CustomSys.emulatesSIO( props ) );
    this.docSioIOBaseAddr.setValue(
		CustomSys.getSioIOBaseAddr( props ), 2 );
    setSelectedSioClock(
		this.comboSioClockA,
		props,
		CustomSys.PROP_SIO_A_CLOCK );
    setSelectedSioClock(
		this.comboSioClockB,
		props,
		CustomSys.PROP_SIO_B_CLOCK );
    setSelectedSioOut(
		this.comboSioOutA,
		props,
		CustomSys.PROP_SIO_A_OUT );
    setSelectedSioOut(
		this.comboSioOutB,
		props,
		CustomSys.PROP_SIO_B_OUT );
    updSioFieldsEnabled();

    this.cbCtcEnabled.setSelected( CustomSys.emulatesCTC( props ) );
    this.docCtcIOBaseAddr.setValue(
		CustomSys.getCtcIOBaseAddr( props ), 2 );
    updCtcFieldsEnabled();

    this.cbK1520SoundEnabled.setSelected(
		CustomSys.emulatesK1520SoundCard( props ) );
    this.docK1520SoundIOBaseAddr.setValue(
		CustomSys.getK1520SoundIOBaseAddr( props ), 2 );
    updK1520SoundFieldsEnabled();

    this.cbKCNetEnabled.setSelected( CustomSys.emulatesKCNetCard( props ) );
    this.docKCNetIOBaseAddr.setValue(
		CustomSys.getKCNetIOBaseAddr( props ), 2 );
    updKCNetFieldsEnabled();

    this.cbVdipEnabled.setSelected( CustomSys.emulatesVdipCard( props ) );
    this.docVdipIOBaseAddr.setValue(
		CustomSys.getVdipIOBaseAddr( props ), 2 );
    updVdipFieldsEnabled();

    this.docUnusedPortValue.setValue(
		CustomSys.getUnusedPortValue( props ), 2 );
  }


	/* --- private Methoden --- */

  private void docChanged( DocumentEvent e )
  {
    Document doc = e.getDocument();
    if( (doc == this.docPioIOBaseAddr)
	|| (doc == this.docSioIOBaseAddr)
	|| (doc == this.docCtcIOBaseAddr)
	|| (doc == this.docK1520SoundIOBaseAddr)
	|| (doc == this.docKCNetIOBaseAddr)
	|| (doc == this.docVdipIOBaseAddr)
	|| (doc == this.docUnusedPortValue) )
    {
      fireDataChanged();
    }
  }

  private static String getSioOutValue( JComboBox<String> combo )
  {
    return combo.getSelectedIndex() == 1 ? CustomSys.VALUE_PRINTER : "";
  }


  private void setSelectedSioClock(
			JComboBox<String> combo,
			Properties        props,
			String     propName )
  {
    int idx = EmuUtil.getIntProperty(
			props,
			this.propPrefix + propName,
			0 );
    if( (idx >= 0) && (idx < combo.getItemCount()) ) {
      try {
	combo.setSelectedIndex( idx );
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  private void setSelectedSioOut(
			JComboBox<String> combo,
			Properties        props,
			String            propName )
  {
    int idx = 0;
    if( EmuUtil.getProperty(
		props,
		this.propPrefix + propName ).equals(
					CustomSys.VALUE_PRINTER ) )
    {
      idx = 1;
    }
    if( (idx >= 0) && (idx < combo.getItemCount()) ) {
      try {
	combo.setSelectedIndex( idx );
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  private void updCtcFieldsEnabled()
  {
    this.fldCtcIOBaseAddr.setEnabled(
			this.cbCtcEnabled.isSelected() );
  }


  private void updK1520SoundFieldsEnabled()
  {
    this.fldK1520SoundIOBaseAddr.setEnabled(
			this.cbK1520SoundEnabled.isSelected() );
  }


  private void updKCNetFieldsEnabled()
  {
    this.fldKCNetIOBaseAddr.setEnabled(
			this.cbKCNetEnabled.isSelected() );
  }


  private void updPioFieldsEnabled()
  {
    this.fldPioIOBaseAddr.setEnabled(
			this.cbPioEnabled.isSelected() );
  }


  private void updSioFieldsEnabled()
  {
    boolean state = this.cbSioEnabled.isSelected();
    this.fldSioIOBaseAddr.setEnabled( state );
    this.labelSioClockA.setEnabled( state );
    this.labelSioClockB.setEnabled( state );
    this.labelSioOutA.setEnabled( state );
    this.labelSioOutB.setEnabled( state );
    this.comboSioClockA.setEnabled( state );
    this.comboSioClockB.setEnabled( state );
    this.comboSioOutA.setEnabled( state );
    this.comboSioOutB.setEnabled( state );
  }


  private void updVdipFieldsEnabled()
  {
    this.fldVdipIOBaseAddr.setEnabled(
			this.cbVdipEnabled.isSelected() );
  }
}
