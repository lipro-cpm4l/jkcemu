/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Poly880-Einstellungen
 */

package jkcemu.emusys.poly880;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;
import jkcemu.emusys.Poly880;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.SettingsFrm;


public class Poly880SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane        tabbedPane;
  private JPanel             tabExt;
  private JPanel             tabEtc;
  private JCheckBox          cbRAM8000;
  private JCheckBox          cbInversedROM;
  private ROMFileSettingsFld fldAltROM0;
  private ROMFileSettingsFld fldAltROM1;
  private ROMFileSettingsFld fldROM2;
  private ROMFileSettingsFld fldROM3;


  public Poly880SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );
    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Erweiterungen
    this.tabExt = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Erweiterungen", this.tabExt );

    GridBagConstraints gbcExt = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.cbRAM8000 = GUIFactory.createCheckBox(
					"32 KByte RAM-Erweiterung" );
    this.cbRAM8000.addActionListener( this );
    this.tabExt.add( this.cbRAM8000, gbcExt );

    gbcExt.fill          = GridBagConstraints.HORIZONTAL;
    gbcExt.weightx       = 1.0;
    gbcExt.insets.top    = 10;
    gbcExt.insets.bottom = 10;
    gbcExt.gridy++;
    this.tabExt.add( GUIFactory.createSeparator(), gbcExt );

    this.fldROM2 = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + Poly880.PROP_ROM2000_PREFIX,
				"ROM-Erweiterung 2000h-23FFh:" );
    gbcExt.insets.top    = 5;
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.fldROM2, gbcExt );

    this.fldROM3 = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + Poly880.PROP_ROM3000_PREFIX,
				"ROM-Erweiterung 3000h-33FFh:" );
    gbcExt.gridy++;
    this.tabExt.add( this.fldROM3, gbcExt );


    // Tab Sonstiges
    this.tabEtc = GUIFactory.createPanel( new GridBagLayout() );
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
		propPrefix + Poly880.PROP_ROM0000_PREFIX,
		"Alternativer ROM-Inhalt 0000h-03FFh:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltROM0, gbcEtc );

    this.fldAltROM1 = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + Poly880.PROP_ROM1000_PREFIX,
		"Alternativer ROM-Inhalt 1000h-13FFh:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltROM1, gbcEtc );

    this.cbInversedROM = GUIFactory.createCheckBox(
		"ROM-Bytes sind negiert (entsprechend dem Original)" );
    this.cbInversedROM.addActionListener( this );
    gbcEtc.weightx = 0.0;
    gbcEtc.fill    = GridBagConstraints.NONE;
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbInversedROM, gbcEtc );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    EmuUtil.setProperty(
		props,
		this.propPrefix + Poly880.PROP_RAM8000_ENABLED,
		this.cbRAM8000.isSelected() );

    this.fldAltROM0.applyInput( props, selected );
    this.fldAltROM1.applyInput( props, selected );
    this.fldROM2.applyInput( props, selected );
    this.fldROM3.applyInput( props, selected );

    EmuUtil.setProperty(
	props,
	this.propPrefix + Poly880.PROP_ROM_PREFIX + Poly880.PROP_NEGATED,
	this.cbInversedROM.isSelected() );
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
  public void updFields( Properties props )
  {
    this.cbRAM8000.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Poly880.PROP_RAM8000_ENABLED,
			false ) );

    this.fldAltROM0.updFields( props );
    this.fldAltROM1.updFields( props );
    this.fldROM2.updFields( props );
    this.fldROM3.updFields( props );

    this.cbInversedROM.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix
				+ Poly880.PROP_ROM_PREFIX
				+ Poly880.PROP_NEGATED,
			false ) );
  }
}
