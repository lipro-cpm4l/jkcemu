/*
 * (c) 2010-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die A5105-Einstellungen
 */

package jkcemu.emusys.a5105;

import java.awt.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;
import jkcemu.emusys.A5105;


public class A5105SettingsFld extends AbstractSettingsFld
{
  private static final int DEFAULT_AUTO_ACTION_WAIT_MILLIS = 3000;

  private JTabbedPane            tabbedPane;
  private JPanel                 tabEtc;
  private RAMFloppiesSettingsFld tabRF;
  private AutoLoadSettingsFld    tabAutoLoad;
  private AutoInputSettingsFld   tabAutoInput;
  private JCheckBox              btnFloppyDisk;
  private JCheckBox              btnKCNet;
  private JCheckBox              btnVDIP;
  private JCheckBox              btnPasteFast;
  private JCheckBox              btnFixedScreenSize;


  public A5105SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab RAM-Floppies
    this.tabRF = new RAMFloppiesSettingsFld(
			settingsFrm,
			propPrefix,
			"RAM-Floppy an E/A-Adressen 20h/21h",
			RAMFloppy.RFType.ADW,
			"RAM-Floppy an E/A-Adressen 24h/25h",
			RAMFloppy.RFType.ADW );
    this.tabbedPane.addTab( "RAM-Floppies", this.tabRF );


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

    this.btnFloppyDisk = new JCheckBox( "Floppy-Disk-Station", true );
    this.tabEtc.add( this.btnFloppyDisk, gbcEtc );

    this.btnKCNet = new JCheckBox( "KCNet-kompatible Netzwerkkarte", false );
    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnKCNet, gbcEtc );

    this.btnVDIP = new JCheckBox(
			"USB-Anschluss (Vinculum VDIP Modul)",
			false );
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnVDIP, gbcEtc );

    this.btnPasteFast = new JCheckBox(
		"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs",
		true );
    this.btnPasteFast.addActionListener( this );
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnPasteFast, gbcEtc );

    this.btnFixedScreenSize = new JCheckBox(
		"Gleiche Fenstergr\u00F6\u00DFe in allen Bildschirmmodi" );
    this.btnFixedScreenSize.addActionListener( this );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnFixedScreenSize, gbcEtc );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
					settingsFrm,
					propPrefix,
					DEFAULT_AUTO_ACTION_WAIT_MILLIS,
					true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
					settingsFrm,
					propPrefix,
					A5105.getDefaultSwapKeyCharCase(),
					DEFAULT_AUTO_ACTION_WAIT_MILLIS );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.btnFloppyDisk.addActionListener( this );
    this.btnKCNet.addActionListener( this );
    this.btnVDIP.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    Component tab = null;
    try {

      // Tab RAM-Floppies
      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      // Tab Sonstiges
      EmuUtil.setProperty(
		props,
		this.propPrefix + "floppydisk.enabled",
		Boolean.toString( this.btnFloppyDisk.isSelected() ) );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "kcnet.enabled",
		this.btnKCNet.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "vdip.enabled",
                this.btnVDIP.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "paste.fast",
		Boolean.toString( this.btnPasteFast.isSelected() ) );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "fixed_screen_size",
		this.btnFixedScreenSize.isSelected() );

      // Tab AutoLoad
      tab = this.tabAutoLoad;
      this.tabAutoLoad.applyInput( props, selected );

      // Tab AutoInput
      tab = this.tabAutoInput;
      this.tabAutoInput.applyInput( props, selected );
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
    this.tabRF.lookAndFeelChanged();
    this.tabAutoLoad.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    this.tabRF.updFields( props );
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );

    this.btnFloppyDisk.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "floppydisk.enabled",
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

    this.btnPasteFast.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "paste.fast",
			true ) );

    this.btnFixedScreenSize.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "fixed_screen_size",
			false ) );
  }
}
