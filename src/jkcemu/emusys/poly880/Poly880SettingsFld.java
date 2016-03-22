/*
 * (c) 2012-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Poly880-Einstellungen
 */

package jkcemu.emusys.poly880;

import java.awt.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;


public class Poly880SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane        tabbedPane;
  private JPanel             tabExt;
  private JPanel             tabEtc;
  private JCheckBox          btnRAM8000;
  private JCheckBox          btnInversedROM;
  private ROMFileSettingsFld fldAltROM0;
  private ROMFileSettingsFld fldAltROM1;
  private ROMFileSettingsFld fldROM2;
  private ROMFileSettingsFld fldROM3;


  public Poly880SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Erweiterungen
    this.tabExt = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Erweiterungen", this.tabExt );

    GridBagConstraints gbcExt = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.btnRAM8000 = new JCheckBox( "32 KByte RAM-Erweiterung" );
    this.btnRAM8000.addActionListener( this );
    this.tabExt.add( this.btnRAM8000, gbcExt );

    gbcExt.fill          = GridBagConstraints.HORIZONTAL;
    gbcExt.weightx       = 1.0;
    gbcExt.insets.top    = 10;
    gbcExt.insets.bottom = 10;
    gbcExt.gridy++;
    this.tabExt.add( new JSeparator(), gbcExt );

    this.fldROM2 = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + "rom_2000.",
				"ROM-Erweiterung 2000h-23FFh:" );
    gbcExt.insets.top    = 5;
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.fldROM2, gbcExt );

    this.fldROM3 = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + "rom_3000.",
				"ROM-Erweiterung 3000h-33FFh:" );
    gbcExt.gridy++;
    this.tabExt.add( this.fldROM3, gbcExt );


    // Tab Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.fldAltROM0 = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "rom_0000.",
		"Alternativer ROM-Inhalt 0000h-03FFh:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltROM0, gbcEtc );

    this.fldAltROM1 = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "rom_1000.",
		"Alternativer ROM-Inhalt 1000h-13FFh:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltROM1, gbcEtc );

    this.btnInversedROM = new JCheckBox(
		"ROM-Bytes sind negiert (entsprechend dem Original)" );
    this.btnInversedROM.addActionListener( this );
    gbcEtc.weightx = 0.0;
    gbcEtc.fill    = GridBagConstraints.NONE;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnInversedROM, gbcEtc );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    EmuUtil.setProperty(
		props,
		this.propPrefix + "ram_8000.enabled",
		this.btnRAM8000.isSelected() );

    this.fldAltROM0.applyInput( props, selected );
    this.fldAltROM1.applyInput( props, selected );
    this.fldROM2.applyInput( props, selected );
    this.fldROM3.applyInput( props, selected );

    EmuUtil.setProperty(
		props,
		this.propPrefix + "rom.negated",
		this.btnInversedROM.isSelected() );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src instanceof AbstractButton ) {
	fireDataChanged();
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.fldAltROM0.lookAndFeelChanged();
    this.fldAltROM1.lookAndFeelChanged();
    this.fldROM2.lookAndFeelChanged();
    this.fldROM3.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    this.btnRAM8000.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "ram_8000.enabled",
				false ) );

    this.fldAltROM0.updFields( props );
    this.fldAltROM1.updFields( props );
    this.fldROM2.updFields( props );
    this.fldROM3.updFields( props );

    this.btnInversedROM.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom.negated",
				false ) );
  }
}

