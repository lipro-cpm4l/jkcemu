/*
 * (c) 2013-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen des Huebler-Grafik-MC
 */

package jkcemu.emusys.huebler;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import jkcemu.base.AutoInputCharSet;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;
import jkcemu.emusys.HueblerGraphicsMC;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.AutoLoadSettingsFld;
import jkcemu.settings.SettingsFrm;


public class HueblerGraphicsMCSettingsFld extends AbstractSettingsFld
{
  private JTabbedPane          tabbedPane;
  private JPanel               tabExt;
  private JPanel               tabEtc;
  private AutoLoadSettingsFld  tabAutoLoad;
  private AutoInputSettingsFld tabAutoInput;
  private JCheckBox            cbKCNet;
  private JCheckBox            cbVDIP;
  private JCheckBox            cbBasic;
  private JCheckBox            cbCatchPrintCalls;


  public HueblerGraphicsMCSettingsFld(
			SettingsFrm settingsFrm,
			String      propPrefix )
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
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbKCNet = GUIFactory.createCheckBox(
			"KCNet-kompatible Netzwerkkarte" );
    this.tabExt.add( this.cbKCNet, gbcExt );

    this.cbVDIP = GUIFactory.createCheckBox(
			"USB-Anschluss (Vinculum VDIP Modul)" );
    gbcExt.insets.top    = 0;
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.cbVDIP, gbcExt );


    // Tab Sonstiges
    this.tabEtc = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbBasic = GUIFactory.createCheckBox(
		"BASIC-Interpreter im ROM enthalten",
		true );
    this.tabEtc.add( this.cbBasic, gbcEtc );

    this.cbCatchPrintCalls = GUIFactory.createCheckBox(
		"Betriebssystemaufrufe f\u00FCr Druckerausgaben abfangen",
		true );
    gbcEtc.insets.top    = 0;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbCatchPrintCalls, gbcEtc );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
		settingsFrm,
		propPrefix,
		HueblerGraphicsMC.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
		true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
		settingsFrm,
		propPrefix,
		AutoInputCharSet.getStdCharSet(),
		HueblerGraphicsMC.DEFAULT_SWAP_KEY_CHAR_CASE,
		HueblerGraphicsMC.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.cbBasic.addActionListener( this );
    this.cbCatchPrintCalls.addActionListener( this );
    this.cbKCNet.addActionListener( this );
    this.cbVDIP.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    EmuUtil.setProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_BASIC,
		this.cbBasic.isSelected() );

    EmuUtil.setProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_CATCH_PRINT_CALLS,
		this.cbCatchPrintCalls.isSelected() );

    EmuUtil.setProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_KCNET_ENABLED,
		this.cbKCNet.isSelected() );

    EmuUtil.setProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_VDIP_ENABLED,
		this.cbVDIP.isSelected() );

    this.tabAutoLoad.applyInput( props, selected );
    this.tabAutoInput.applyInput( props, selected );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      rv = this.tabAutoLoad.doAction( e );
      if( !rv ) {
	rv = this.tabAutoInput.doAction( e );
      }
      if( !rv && (src instanceof AbstractButton) ) {
        rv = true;
        fireDataChanged();
      }
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    this.cbBasic.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_BASIC,
		true ) );

    this.cbCatchPrintCalls.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_CATCH_PRINT_CALLS,
		true ) );

    this.cbKCNet.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_KCNET_ENABLED,
		false ) );

    this.cbVDIP.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_VDIP_ENABLED,
		false ) );

    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );
  }
}
