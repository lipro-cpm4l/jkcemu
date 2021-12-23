/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen des SCCH-Moduls 1
 */

package jkcemu.emusys.ac1_llc2;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.SettingsFrm;


public class SCCHModule1SettingsFld
			extends AbstractSettingsFld
			implements ChangeListener
{
  private ROMFileSettingsFld fldPrgX;
  private ROMFileSettingsFld fldRomDisk;
  private ROMFileSettingsFld fldBasicRom;
  private JLabel             labelBasicRomAddr;
  private JRadioButton       rbBasicRom2000;
  private JRadioButton       rbBasicRom4000;
  private JLabel             labelRomDiskAddr;
  private JRadioButton       rbRomDisk8000;
  private JRadioButton       rbRomDiskC000;


  public SCCHModule1SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    // Programmpaket X
    this.fldPrgX = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + AbstractSCCHSys.PROP_PROGRAM_X_PREFIX,
		"ROM-Datei f\u00FCr Programmpaket X (E000h-FFFFh):" );
    add( this.fldPrgX, gbc );

    // ROM-Disk
    this.fldRomDisk = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + AbstractSCCHSys.PROP_ROMDISK_PREFIX,
		"ROM-Datei f\u00FCr ROM-Disk"
			+ " (8000h-FFFFh bzw. C000h-FFFFh):" );
    gbc.insets.bottom = 0;
    gbc.gridy++;
    add( this.fldRomDisk, gbc );

    JPanel panelRomDiskAddr = GUIFactory.createPanel(
				new FlowLayout( FlowLayout.LEFT, 5, 0 ) );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( panelRomDiskAddr, gbc );

    this.labelRomDiskAddr = GUIFactory.createLabel(
				"ROM-Disk einblenden ab Adresse:" );
    panelRomDiskAddr.add( this.labelRomDiskAddr );

    ButtonGroup grpRomDiskAddr = new ButtonGroup();

    this.rbRomDisk8000 = GUIFactory.createRadioButton( "8000h" );
    grpRomDiskAddr.add( this.rbRomDisk8000 );
    panelRomDiskAddr.add( this.rbRomDisk8000 );

    this.rbRomDiskC000 = GUIFactory.createRadioButton( "C000h", true );
    grpRomDiskAddr.add( this.rbRomDiskC000 );
    panelRomDiskAddr.add( this.rbRomDiskC000 );
    updRomDiskAddrFieldsEnabled();

    // Trennlinie
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( GUIFactory.createSeparator(), gbc );

    // BASIC-ROM
    this.fldBasicRom = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + AbstractSCCHSys.PROP_BASIC_PREFIX,
		"Alternativer BASIC-ROM (2000h-5FFFh bzw. 4000h-5FFFh):" );
    gbc.insets.top    = 5;
    gbc.insets.bottom = 0;
    gbc.gridy++;
    add( this.fldBasicRom, gbc );

    JPanel panelBasicRomAddr = GUIFactory.createPanel(
				new FlowLayout( FlowLayout.LEFT, 5, 0 ) );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( panelBasicRomAddr, gbc );

    this.labelBasicRomAddr = GUIFactory.createLabel(
				"BASIC-ROM einblenden ab Adresse:" );
    panelBasicRomAddr.add( this.labelBasicRomAddr );
    ButtonGroup grpBasicRomAddr = new ButtonGroup();

    this.rbBasicRom2000 = GUIFactory.createRadioButton( "2000h" );
    grpBasicRomAddr.add( this.rbBasicRom2000 );
    panelBasicRomAddr.add( this.rbBasicRom2000 );

    this.rbBasicRom4000 = GUIFactory.createRadioButton( "4000h", true );
    grpBasicRomAddr.add( this.rbBasicRom4000 );
    panelBasicRomAddr.add( this.rbBasicRom4000 );
    updBasicRomAddrFieldsEnabled();


    // Sonstiges
    setEnabled( true );


    // Listener
    this.fldRomDisk.addChangeListener( this );
    this.rbRomDisk8000.addActionListener( this );
    this.rbRomDiskC000.addActionListener( this );
    this.fldBasicRom.addChangeListener( this );
    this.rbBasicRom2000.addActionListener( this );
    this.rbBasicRom4000.addActionListener( this );
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( src == this.fldRomDisk ) {
      updRomDiskAddrFieldsEnabled();
    } else if( src == this.fldBasicRom ) {
      updBasicRomAddrFieldsEnabled();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    this.fldPrgX.applyInput( props, selected );
    this.fldRomDisk.applyInput( props, selected );
    this.fldBasicRom.applyInput( props, selected );
    EmuUtil.setProperty(
		props,
		this.propPrefix + AbstractSCCHSys.PROP_BASIC_ADDR,
		this.rbBasicRom2000.isSelected() ? "2000" : "4000" );
    EmuUtil.setProperty(
		props,
		this.propPrefix + AbstractSCCHSys.PROP_ROMDISK_ADDR,
		this.rbRomDisk8000.isSelected() ? "8000" : "C000" );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src instanceof AbstractButton ) {
	rv = true;
	fireDataChanged();
      }
    }
    return rv;
  }


  @Override
  public void setEnabled( boolean state )
  {
    super.setEnabled( state );
    this.fldPrgX.setEnabled( state );
    this.fldRomDisk.setEnabled( state );
    this.fldBasicRom.setEnabled( state );
    updBasicRomAddrFieldsEnabled();
    updRomDiskAddrFieldsEnabled();
  }


  @Override
  public void updFields( Properties props )
  {
    this.fldPrgX.updFields( props );
    this.fldRomDisk.updFields( props );
    this.fldBasicRom.updFields( props );

    String text = EmuUtil.getProperty(
		props,
		this.propPrefix + AbstractSCCHSys.PROP_BASIC_ADDR );
    if( text.startsWith( "2000" ) ) {
      this.rbBasicRom2000.setSelected( true );
    } else {
      this.rbBasicRom4000.setSelected( true );
    }
    updBasicRomAddrFieldsEnabled();

    text = EmuUtil.getProperty(
		props,
		this.propPrefix + AbstractSCCHSys.PROP_ROMDISK_ADDR );
    if( text.startsWith( "8000" ) ) {
      this.rbRomDisk8000.setSelected( true );
    } else {
      this.rbRomDiskC000.setSelected( true );
    }
    updRomDiskAddrFieldsEnabled();
  }


  private void updBasicRomAddrFieldsEnabled()
  {
    boolean state = this.fldBasicRom.isEnabled();
    if( state && (this.fldBasicRom.getFile() == null) ) {
      state = false;
    }
    this.labelBasicRomAddr.setEnabled( state );
    this.rbBasicRom2000.setEnabled( state );
    this.rbBasicRom4000.setEnabled( state );
  }


  private void updRomDiskAddrFieldsEnabled()
  {
    boolean state = this.fldRomDisk.isEnabled();
    if( state && (this.fldRomDisk.getFile() == null) ) {
      state = false;
    }
    this.labelRomDiskAddr.setEnabled( state );
    this.rbRomDisk8000.setEnabled( state );
    this.rbRomDiskC000.setEnabled( state );
  }
}
