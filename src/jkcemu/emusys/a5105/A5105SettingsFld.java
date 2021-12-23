/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die A5105-Einstellungen
 */

package jkcemu.emusys.a5105;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.RAMFloppy;
import jkcemu.base.UserInputException;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.A5105;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.AutoLoadSettingsFld;
import jkcemu.settings.RAMFloppiesSettingsFld;
import jkcemu.settings.SettingsFrm;


public class A5105SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane            tabbedPane;
  private JPanel                 tabEtc;
  private RAMFloppiesSettingsFld tabRF;
  private GIDESettingsFld        tabGIDE;
  private AutoLoadSettingsFld    tabAutoLoad;
  private AutoInputSettingsFld   tabAutoInput;
  private JCheckBox              cbFloppyDisk;
  private JCheckBox              cbK1520Sound;
  private JCheckBox              cbKCNet;
  private JCheckBox              cbVDIP;
  private JCheckBox              cbPasteFast;
  private JCheckBox              cbFixedScreenSize;


  public A5105SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );
    this.tabbedPane = GUIFactory.createTabbedPane();
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


    // Tab GIDE
    this.tabGIDE = new GIDESettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );


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

    this.cbFloppyDisk = GUIFactory.createCheckBox(
					"Floppy-Disk-Station",
					true );
    this.tabEtc.add( this.cbFloppyDisk, gbcEtc );

    this.cbK1520Sound = GUIFactory.createCheckBox( "K1520-Sound-Karte" );
    gbcEtc.insets.top  = 0;
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbK1520Sound, gbcEtc );

    this.cbKCNet = GUIFactory.createCheckBox(
				"KCNet-kompatible Netzwerkkarte" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbKCNet, gbcEtc );

    this.cbVDIP = GUIFactory.createCheckBox(
				"USB-Anschluss (Vinculum VDIP Modul)" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbVDIP, gbcEtc );

    this.cbPasteFast = GUIFactory.createCheckBox(
		"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs" );
    gbcEtc.insets.top = 20;
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbPasteFast, gbcEtc );

    this.cbFixedScreenSize = GUIFactory.createCheckBox(
		"Gleiche Fenstergr\u00F6\u00DFe in allen Bildschirmmodi" );
    gbcEtc.insets.top    = 0;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbFixedScreenSize, gbcEtc );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
				settingsFrm,
				propPrefix,
				A5105.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
				true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
				settingsFrm,
				propPrefix,
				A5105.getAutoInputCharSet(),
				A5105.DEFAULT_SWAP_KEY_CHAR_CASE,
				A5105.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.cbFloppyDisk.addActionListener( this );
    this.cbK1520Sound.addActionListener( this );
    this.cbKCNet.addActionListener( this );
    this.cbVDIP.addActionListener( this );
    this.cbPasteFast.addActionListener( this );
    this.cbFixedScreenSize.addActionListener( this );
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

      // Tab GIDE
      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      // Tab Sonstiges
      EmuUtil.setProperty(
		props,
		this.propPrefix + A5105.PROP_FDC_ENABLED,
		this.cbFloppyDisk.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + A5105.PROP_K1520SOUND_ENABLED,
		this.cbK1520Sound.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + A5105.PROP_KCNET_ENABLED,
		this.cbKCNet.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + A5105.PROP_VDIP_ENABLED,
                this.cbVDIP.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + A5105.PROP_PASTE_FAST,
		this.cbPasteFast.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + A5105.PROP_FIXED_SCREEN_SIZE,
		this.cbFixedScreenSize.isSelected() );

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
      rv = this.tabGIDE.doAction( e );
      if( !rv ) {
	rv = this.tabAutoLoad.doAction( e );
      }
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
    this.tabRF.updFields( props );
    this.tabGIDE.updFields( props );
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );

    this.cbFloppyDisk.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + A5105.PROP_FDC_ENABLED,
			true ) );

    this.cbK1520Sound.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + A5105.PROP_K1520SOUND_ENABLED,
			false ) );

    this.cbKCNet.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + A5105.PROP_KCNET_ENABLED,
			false ) );

    this.cbVDIP.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + A5105.PROP_VDIP_ENABLED,
			false ) );

    this.cbPasteFast.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + A5105.PROP_PASTE_FAST,
			true ) );

    this.cbFixedScreenSize.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + A5105.PROP_FIXED_SCREEN_SIZE,
			false ) );
  }
}
