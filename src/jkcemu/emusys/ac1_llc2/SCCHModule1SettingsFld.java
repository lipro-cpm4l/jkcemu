/*
 * (c) 2010-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen des SCCH-Moduls 1
 */

package jkcemu.emusys.ac1_llc2;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.base.*;


public class SCCHModule1SettingsFld
			extends AbstractSettingsFld
			implements ChangeListener
{
  private ROMFileSettingsFld fldPrgX;
  private ROMFileSettingsFld fldRomDisk;
  private ROMFileSettingsFld fldBasicRom;
  private JLabel             labelBasicRomAddr;
  private JRadioButton       btnBasicRom2000;
  private JRadioButton       btnBasicRom4000;
  private JLabel             labelRomDiskAddr;
  private JRadioButton       btnRomDisk8000;
  private JRadioButton       btnRomDiskC000;


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
		propPrefix + "program_x.",
		"ROM-Datei f\u00FCr Programmpaket X (E000h-FFFFh):" );
    add( this.fldPrgX, gbc );

    // ROM-Disk
    this.fldRomDisk = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "romdisk.",
		"ROM-Datei f\u00FCr ROM-Disk"
			+ " (8000h-FFFFh bzw. C000h-FFFFh):" );
    gbc.insets.bottom = 0;
    gbc.gridy++;
    add( this.fldRomDisk, gbc );

    JPanel panelRomDiskAddr = new JPanel(
				new FlowLayout( FlowLayout.LEFT, 5, 0 ) );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( panelRomDiskAddr, gbc );

    this.labelRomDiskAddr = new JLabel( "ROM-Disk einblenden ab Adresse:" );
    panelRomDiskAddr.add( this.labelRomDiskAddr );

    ButtonGroup grpRomDiskAddr = new ButtonGroup();

    this.btnRomDisk8000 = new JRadioButton( "8000h", false );
    grpRomDiskAddr.add( this.btnRomDisk8000 );
    panelRomDiskAddr.add( this.btnRomDisk8000 );

    this.btnRomDiskC000 = new JRadioButton( "C000h", true );
    grpRomDiskAddr.add( this.btnRomDiskC000 );
    panelRomDiskAddr.add( this.btnRomDiskC000 );
    updRomDiskAddrFieldsEnabled();

    // Trennlinie
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    // BASIC-ROM
    this.fldBasicRom = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "basic.",
		"Alternativer BASIC-ROM (2000h-5FFFh bzw. 4000h-5FFFh):" );
    gbc.insets.top    = 5;
    gbc.insets.bottom = 0;
    gbc.gridy++;
    add( this.fldBasicRom, gbc );

    JPanel panelBasicRomAddr = new JPanel(
				new FlowLayout( FlowLayout.LEFT, 5, 0 ) );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( panelBasicRomAddr, gbc );

    this.labelBasicRomAddr = new JLabel( "BASIC-ROM einblenden ab Adresse:" );
    panelBasicRomAddr.add( this.labelBasicRomAddr );
    ButtonGroup grpBasicRomAddr = new ButtonGroup();

    this.btnBasicRom2000 = new JRadioButton( "2000h", false );
    grpBasicRomAddr.add( this.btnBasicRom2000 );
    panelBasicRomAddr.add( this.btnBasicRom2000 );

    this.btnBasicRom4000 = new JRadioButton( "4000h", true );
    grpBasicRomAddr.add( this.btnBasicRom4000 );
    panelBasicRomAddr.add( this.btnBasicRom4000 );
    updBasicRomAddrFieldsEnabled();


    // Sonstiges
    setEnabled( true );


    // Listener
    this.fldRomDisk.addChangeListener( this );
    this.btnRomDisk8000.addActionListener( this );
    this.btnRomDiskC000.addActionListener( this );
    this.fldBasicRom.addChangeListener( this );
    this.btnBasicRom2000.addActionListener( this );
    this.btnBasicRom4000.addActionListener( this );
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
		this.propPrefix + "basicrom.address.begin",
		this.btnBasicRom2000.isSelected() ? "2000" : "4000" );
    EmuUtil.setProperty(
		props,
		this.propPrefix + "romdisk.address.begin",
		this.btnRomDisk8000.isSelected() ? "8000" : "C000" );
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
			this.propPrefix + "basicrom.address.begin" );
    if( text.startsWith( "2000" ) ) {
      this.btnBasicRom2000.setSelected( true );
    } else {
      this.btnBasicRom4000.setSelected( true );
    }
    updBasicRomAddrFieldsEnabled();

    text = EmuUtil.getProperty(
			props,
			this.propPrefix + "romdisk.address.begin" );
    if( text.startsWith( "8000" ) ) {
      this.btnRomDisk8000.setSelected( true );
    } else {
      this.btnRomDiskC000.setSelected( true );
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
    this.btnBasicRom2000.setEnabled( state );
    this.btnBasicRom4000.setEnabled( state );
  }


  private void updRomDiskAddrFieldsEnabled()
  {
    boolean state = this.fldRomDisk.isEnabled();
    if( state && (this.fldRomDisk.getFile() == null) ) {
      state = false;
    }
    this.labelRomDiskAddr.setEnabled( state );
    this.btnRomDisk8000.setEnabled( state );
    this.btnRomDiskC000.setEnabled( state );
  }
}

