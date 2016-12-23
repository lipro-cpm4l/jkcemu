/*
 * (c) 2013-2016 Jens Mueller
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
import java.lang.*;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import jkcemu.base.AbstractSettingsFld;
import jkcemu.base.AutoInputSettingsFld;
import jkcemu.base.AutoLoadSettingsFld;
import jkcemu.base.EmuUtil;
import jkcemu.base.SettingsFrm;
import jkcemu.base.UserInputException;
import jkcemu.emusys.HueblerGraphicsMC;


public class HueblerGraphicsMCSettingsFld extends AbstractSettingsFld
{
  private JTabbedPane          tabbedPane;
  private JPanel               tabExt;
  private JPanel               tabEtc;
  private AutoLoadSettingsFld  tabAutoLoad;
  private AutoInputSettingsFld tabAutoInput;
  private JCheckBox            btnKCNet;
  private JCheckBox            btnVDIP;
  private JCheckBox            btnBasic;
  private JCheckBox            btnCatchPrintCalls;


  public HueblerGraphicsMCSettingsFld(
			SettingsFrm settingsFrm,
			String      propPrefix )
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
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnKCNet = new JCheckBox(
			"KCNet-kompatible Netzwerkkarte",
			false );
    this.tabExt.add( this.btnKCNet, gbcExt );

    this.btnVDIP = new JCheckBox(
			"USB-Anschluss (Vinculum VDIP Modul)",
			false );
    gbcExt.insets.top    = 0;
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.btnVDIP, gbcExt );


    // Tab Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnBasic = new JCheckBox(
		"BASIC-Interpreter im ROM enthalten",
		true );
    this.tabEtc.add( this.btnBasic, gbcEtc );

    this.btnCatchPrintCalls = new JCheckBox(
		"Betriebssystemaufrufe f\u00FCr Druckerausgaben abfangen",
		true );
    gbcEtc.insets.top    = 0;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnCatchPrintCalls, gbcEtc );


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
		HueblerGraphicsMC.DEFAULT_SWAP_KEY_CHAR_CASE,
		HueblerGraphicsMC.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.btnBasic.addActionListener( this );
    this.btnCatchPrintCalls.addActionListener( this );
    this.btnKCNet.addActionListener( this );
    this.btnVDIP.addActionListener( this );
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
		this.btnBasic.isSelected() );

    EmuUtil.setProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_CATCH_PRINT_CALLS,
		this.btnCatchPrintCalls.isSelected() );

    EmuUtil.setProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_KCNET_ENABLED,
		this.btnKCNet.isSelected() );

    EmuUtil.setProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_VDIP_ENABLED,
		this.btnVDIP.isSelected() );

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
  public void lookAndFeelChanged()
  {
    this.tabAutoLoad.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    this.btnBasic.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_BASIC,
		true ) );

    this.btnCatchPrintCalls.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_CATCH_PRINT_CALLS,
		true ) );

    this.btnKCNet.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_KCNET_ENABLED,
		false ) );

    this.btnVDIP.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + HueblerGraphicsMC.PROP_VDIP_ENABLED,
		false ) );

    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );
  }
}
