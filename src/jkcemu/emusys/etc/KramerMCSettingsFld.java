/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die KramerMC-Einstellungen
 */

package jkcemu.emusys.etc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.*;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import jkcemu.base.AbstractSettingsFld;
import jkcemu.base.AutoInputSettingsFld;
import jkcemu.base.AutoLoadSettingsFld;
import jkcemu.base.EmuUtil;
import jkcemu.base.ROMFileSettingsFld;
import jkcemu.base.SettingsFrm;
import jkcemu.base.UserInputException;
import jkcemu.emusys.KramerMC;


public class KramerMCSettingsFld extends AbstractSettingsFld
{
  private JTabbedPane          tabbedPane;
  private JPanel               tabEtc;
  private AutoLoadSettingsFld  tabAutoLoad;
  private AutoInputSettingsFld tabAutoInput;
  private JCheckBox            btnCatchPrintCalls;
  private ROMFileSettingsFld   fldAltRom0000;
  private ROMFileSettingsFld   fldAltRom8000;
  private ROMFileSettingsFld   fldAltRomC000;
  private ROMFileSettingsFld   fldAltFont;


  public KramerMCSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
		settingsFrm,
		propPrefix,
		KramerMC.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
		true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
		settingsFrm,
		propPrefix,
		KramerMC.DEFAULT_SWAP_KEY_CHAR_CASE,
		KramerMC.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );

    // Tab Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.btnCatchPrintCalls = new JCheckBox(
		"Betriebssystemaufrufe f\u00FCr Druckerausgaben abfangen",
		false );
    this.btnCatchPrintCalls.addActionListener( this );
    this.tabEtc.add( this.btnCatchPrintCalls, gbcEtc );

    gbcEtc.fill          = GridBagConstraints.HORIZONTAL;
    gbcEtc.weightx       = 1.0;
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.bottom = 10;
    gbcEtc.gridy++;
    this.tabEtc.add( new JSeparator(), gbcEtc );

    this.fldAltRom0000 = new ROMFileSettingsFld(
			this.settingsFrm,
			this.propPrefix + KramerMC.PROP_ROM0_PREFIX,
			"Alternativer ROM-Inhalt f\u00FCr 0000h-0BFFh:" );
    gbcEtc.gridwidth     = 1;
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltRom0000, gbcEtc );

    this.fldAltRom8000 = new ROMFileSettingsFld(
			this.settingsFrm,
			this.propPrefix + KramerMC.PROP_ROM8_PREFIX,
			"Alternativer ROM-Inhalt f\u00FCr 8000h-AFFFh:" );
    gbcEtc.gridwidth     = 1;
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltRom8000, gbcEtc );

    this.fldAltRomC000 = new ROMFileSettingsFld(
			this.settingsFrm,
			this.propPrefix + KramerMC.PROP_ROMC_PREFIX,
			"Alternativer ROM-Inhalt f\u00FCr C000h-DFFFh:" );
    gbcEtc.gridwidth     = 1;
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltRomC000, gbcEtc );

    this.fldAltFont = new ROMFileSettingsFld(
			this.settingsFrm,
			this.propPrefix + KramerMC.PROP_FONT_PREFIX,
			"Alternativer Zeichensatz:" );
    gbcEtc.gridwidth     = 1;
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltFont, gbcEtc );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    Component tab = null;
    try {

      // Tab AutoLoad
      tab = this.tabAutoLoad;
      this.tabAutoLoad.applyInput( props, selected );

      // Tab AutoInput
      tab = this.tabAutoInput;
      this.tabAutoInput.applyInput( props, selected );

      // Tab Drucken
      tab = this.tabEtc;
      props.setProperty(
		this.propPrefix + KramerMC.PROP_CATCH_PRINT_CALLS,
		Boolean.toString(
			this.btnCatchPrintCalls.isSelected() ) );

      // alternative ROM-Inhalte
      this.fldAltRom0000.applyInput( props, selected );
      this.fldAltRom8000.applyInput( props, selected );
      this.fldAltRomC000.applyInput( props, selected );
      this.fldAltFont.applyInput( props, selected );
    }
    catch( UserInputException ex ) {
      if( tab != null ) {
	this.tabbedPane.setSelectedComponent( tab );
      }
      throw ex;
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    Object  src = e.getSource();
    boolean rv  = this.tabAutoLoad.doAction( e );
    if( !rv ) {
      rv = this.tabAutoInput.doAction( e );
    }
    if( !rv && (src instanceof AbstractButton) ) {
      fireDataChanged();
      rv = true;
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.tabAutoLoad.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
    this.fldAltRom0000.lookAndFeelChanged();
    this.fldAltRom8000.lookAndFeelChanged();
    this.fldAltRomC000.lookAndFeelChanged();
    this.fldAltFont.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );

    this.btnCatchPrintCalls.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + KramerMC.PROP_CATCH_PRINT_CALLS,
			false ) );

    this.fldAltRom0000.updFields( props );
    this.fldAltRom8000.updFields( props );
    this.fldAltRomC000.updFields( props );
    this.fldAltFont.updFields( props );
  }
}
