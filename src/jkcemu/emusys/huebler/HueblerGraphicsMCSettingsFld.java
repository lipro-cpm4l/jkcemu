/*
 * (c) 2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen des Huebler-Grafik-MC
 */

package jkcemu.emusys.huebler;

import java.awt.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;


public class HueblerGraphicsMCSettingsFld extends AbstractSettingsFld
{
  private JCheckBox btnKCNet;
  private JCheckBox btnVDIP;
  private JCheckBox btnBasic;
  private JCheckBox btnCatchPrintCalls;


  public HueblerGraphicsMCSettingsFld(
			SettingsFrm settingsFrm,
			String      propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    add( new JLabel( "Erweiterungen:" ), gbc );

    this.btnKCNet = new JCheckBox(
			"KCNet-kompatible Netzwerkkarte",
			false );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridy++;
    add( this.btnKCNet, gbc );

    this.btnVDIP = new JCheckBox(
			"USB-Anschluss (Vinculum VDIP Modul)",
			false );
    gbc.gridy++;
    add( this.btnVDIP, gbc );

    gbc.insets.top  = 20;
    gbc.insets.left = 5;
    gbc.gridy++;
    add( new JLabel( "Sonstiges:" ), gbc );

    this.btnBasic = new JCheckBox(
		"BASIC-Interpreter im ROM enthalten",
		true );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridy++;
    add( this.btnBasic, gbc );

    this.btnCatchPrintCalls = new JCheckBox(
		"Betriebssystemaufrufe f\u00FCr Druckerausgaben abfangen",
		true );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.btnCatchPrintCalls, gbc );


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
		this.propPrefix + "basic",
		this.btnBasic.isSelected() );

    EmuUtil.setProperty(
		props,
		this.propPrefix + "catch_print_calls",
		this.btnCatchPrintCalls.isSelected() );

    EmuUtil.setProperty(
		props,
		this.propPrefix + "kcnet.enabled",
		this.btnKCNet.isSelected() );

    EmuUtil.setProperty(
		props,
		this.propPrefix + "vdip.enabled",
		this.btnVDIP.isSelected() );
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
  public void updFields( Properties props )
  {
    this.btnBasic.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "basic",
			true ) );

    this.btnCatchPrintCalls.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "catch_print_calls",
			true ) );

    this.btnKCNet.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "kcnet.enabled",
			false ) );

    this.btnVDIP.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "vdip.enabled",
			false ) );
  }
}
