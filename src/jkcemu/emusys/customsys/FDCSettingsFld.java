/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die FDC-Einstellungen
 * des benutzerdefinierten Computers
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


public class FDCSettingsFld
			extends AbstractSettingsFld
			implements DocumentListener
{
  private static final String LABEL_DATA_IOADDR
				= "E/A-Adresse Datenregister (hex):";
  private static final String LABEL_STATUS_IOADDR
				= "E/A-Adresse Statusregister (hex):";
  private static final String LABEL_TC_IOADDR
				= "E/A-Adresse Terminal Count (hex):";
  private static final String LABEL_TC_IOBIT
				= "Terminal Count ausl\u00F6sen bei:";

  private CustomSysSettingsFld csSettingsFld;
  private HexDocument          docDataIOAddr;
  private HexDocument          docStatusIOAddr;
  private HexDocument          docTcIOAddr;
  private JTextField           fldDataIOAddr;
  private JTextField           fldStatusIOAddr;
  private JTextField           fldTcIOAddr;
  private JComboBox<String>    comboTcBit;
  private JCheckBox            cbFdcEnabled;
  private JLabel               labelDataIOAddr;
  private JLabel               labelStatusIOAddr;
  private JLabel               labelTcIOAddr;
  private JLabel               labelTcBit;


  public FDCSettingsFld(
		CustomSysSettingsFld csSettingsFld,
		String               propPrefix )
  {
    super( csSettingsFld.getSettingsFrm(), propPrefix );
    this.csSettingsFld = csSettingsFld;

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbFdcEnabled = GUIFactory.createCheckBox(
	"Floppy Disk Controller (FDC) mit 4 Diskettenlaufwerken emulieren" );
    add( this.cbFdcEnabled, gbc );

    this.labelDataIOAddr = GUIFactory.createLabel( LABEL_DATA_IOADDR );
    gbc.insets.left      = 50;
    gbc.gridwidth        = 1;
    gbc.gridy++;
    add( this.labelDataIOAddr, gbc );

    this.labelStatusIOAddr = GUIFactory.createLabel( LABEL_STATUS_IOADDR );
    gbc.gridy++;
    add( this.labelStatusIOAddr, gbc );

    this.labelTcIOAddr = GUIFactory.createLabel( LABEL_TC_IOADDR );
    gbc.gridy++;
    add( this.labelTcIOAddr, gbc );

    this.labelTcBit = GUIFactory.createLabel( LABEL_TC_IOBIT );
    gbc.gridy++;
    add( this.labelTcBit, gbc );

    this.docDataIOAddr = new HexDocument( 2, LABEL_DATA_IOADDR );
    this.fldDataIOAddr = GUIFactory.createTextField( this.docDataIOAddr, 3 );
    gbc.insets.left    = 5;
    gbc.gridy          = 1;
    gbc.gridx++;
    add( this.fldDataIOAddr, gbc );

    this.docStatusIOAddr = new HexDocument( 2, LABEL_STATUS_IOADDR );
    this.fldStatusIOAddr = GUIFactory.createTextField(
					this.docStatusIOAddr,
					3 );
    gbc.gridy++;
    add( this.fldStatusIOAddr, gbc );

    this.docTcIOAddr = new HexDocument( 2, LABEL_TC_IOADDR );
    this.fldTcIOAddr = GUIFactory.createTextField(
					this.docTcIOAddr,
					3 );
    gbc.gridy++;
    add( this.fldTcIOAddr, gbc );

    this.comboTcBit = GUIFactory.createComboBox();
    this.comboTcBit.setEditable( false );
    this.comboTcBit.addItem( "jedem Ausgabebefehl" );
    for( int i = 0; i < 8; i++ ) {
      this.comboTcBit.addItem( String.format( "Bit %d \u2192 H", i ) );
    }
    for( int i = 0; i < 8; i++ ) {
      this.comboTcBit.addItem( String.format( "Bit %d \u2192 L", i ) );
    }
    gbc.gridy++;
    add( this.comboTcBit, gbc );

    this.cbFdcEnabled.addActionListener( this );
    updFieldsEnabled();
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
    boolean fdcEnabled = this.cbFdcEnabled.isSelected();
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_FDC_ENABLED,
		fdcEnabled );
    this.csSettingsFld.applyIOAddrInput(
				selected,
				"FDC Datenregister",
				this.docDataIOAddr,
				CustomSys.PROP_FDC_DATA_IOADDR,
				props );
    this.csSettingsFld.applyIOAddrInput(
				selected,
				"FDC Statusregister",
				this.docStatusIOAddr,
				CustomSys.PROP_FDC_STATUS_IOADDR,
				props );
    this.csSettingsFld.applyIOAddrInput(
				selected,
				"FDC Terminal Count",
				this.docTcIOAddr,
				CustomSys.PROP_FDC_TC_IOADDR,
				props );
    int tcIOMask   = 0;
    int tcIOValue  = 0;
    int tcIOBitIdx = this.comboTcBit.getSelectedIndex();
    if( tcIOBitIdx > 0 ) {
      tcIOMask = (0x01 << ((tcIOBitIdx - 1) % 8));
      if( tcIOBitIdx < 9 ) {
	tcIOValue = tcIOMask;
      }
    }
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_FDC_TC_IOMASK,
		tcIOMask );
    EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_FDC_TC_IOVALUE,
		tcIOValue );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.cbFdcEnabled ) {
      rv = true;
      updFieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.comboTcBit ) {
      rv = true;
      fireDataChanged();
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    this.cbFdcEnabled.setSelected( CustomSys.emulatesFDC( props ) );
    this.docDataIOAddr.setValue(
		CustomSys.getFdcDataIOAddr( props ), 4 );
    this.docStatusIOAddr.setValue(
		CustomSys.getFdcStatusIOAddr( props ), 4 );
    this.docTcIOAddr.setValue(
		CustomSys.getFdcTCIOAddr( props ), 4 );

    int tcBitIdx  = 0;
    int tcIOValue = CustomSys.getFdcTCIOValue( props );
    int tcIOMask  = CustomSys.getFdcTCIOMask( props );
    if( tcIOMask != 0 ) {
      int m = 0x01;
      for( int i = 1; i <= 8; i++ ) {
	if( (tcIOMask & m) != 0 ) {
	  tcBitIdx = i;
	  if( (tcIOValue & m) == 0 ) {
	    tcBitIdx += 8;
	  }
	  break;
	}
	m <<= 1;
      }
    }
    try {
      this.comboTcBit.setSelectedIndex( tcBitIdx );
    }
    catch( IllegalArgumentException ex ) {}
    updFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void docChanged( DocumentEvent e )
  {
    Document doc = e.getDocument();
    if( (doc == this.docDataIOAddr)
	|| (doc == this.docStatusIOAddr)
	|| (doc == this.docTcIOAddr) )
    {
      fireDataChanged();
    }
  }


  private void updFieldsEnabled()
  {
    boolean state = this.cbFdcEnabled.isSelected();
    this.labelDataIOAddr.setEnabled( state );
    this.labelStatusIOAddr.setEnabled( state );
    this.labelTcIOAddr.setEnabled( state );
    this.labelTcBit.setEnabled( state );
    this.fldDataIOAddr.setEnabled( state );
    this.fldStatusIOAddr.setEnabled( state );
    this.fldTcIOAddr.setEnabled( state );
    this.comboTcBit.setEnabled( state );
  }
}
