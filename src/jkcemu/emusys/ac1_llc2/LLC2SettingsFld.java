/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die LLC2-Einstellungen
 */

package jkcemu.emusys.ac1_llc2;

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
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.RAMFloppy;
import jkcemu.base.UserInputException;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.LLC2;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.AutoLoadSettingsFld;
import jkcemu.settings.RAMFloppiesSettingsFld;
import jkcemu.settings.SettingsFrm;


public class LLC2SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane            tabbedPane;
  private SCCHModule1SettingsFld tabSCCH;
  private RAMFloppiesSettingsFld tabRF;
  private JCheckBox              cbFloppyDisk;
  private JCheckBox              cbJoystick;
  private JCheckBox              cbK1520Sound;
  private JCheckBox              cbKCNet;
  private JCheckBox              cbVDIP;
  private JCheckBox              cbPasteFast;
  private JCheckBox              cbRatio43;
  private ROMFileSettingsFld     fldAltOS;
  private ROMFileSettingsFld     fldAltFont;
  private GIDESettingsFld        tabGIDE;
  private JPanel                 tabExt;
  private JPanel                 tabEtc;
  private AutoLoadSettingsFld    tabAutoLoad;
  private AutoInputSettingsFld   tabAutoInput;


  public LLC2SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );
    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab SCCH-Modul 1
    this.tabSCCH = new SCCHModule1SettingsFld(
					settingsFrm,
					propPrefix + LLC2.PROP_SCCH_PREFIX );
    this.tabbedPane.addTab( "SCCH-Modul 1", this.tabSCCH );


    // Tab RAM-Floppies
    this.tabRF = new RAMFloppiesSettingsFld(
	settingsFrm,
	propPrefix,
	"RAM-Floppy nach MP 3/1988 (256 KByte) an E/A-Adressen D0h-D7h",
	RAMFloppy.RFType.MP_3_1988,
	"RAM-Floppy nach MP 3/1988 (256 KByte) an E/A-Adressen B0h-B7h",
	RAMFloppy.RFType.MP_3_1988 );
    this.tabbedPane.addTab( "RAM-Floppies", this.tabRF );


    // Tab GIDE
    this.tabGIDE = new GIDESettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );


    // Tab Erweiterungen
    this.tabExt = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Erweiterungen", this.tabExt );

    GridBagConstraints gbcExt = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbFloppyDisk = GUIFactory.createCheckBox( "Floppy-Disk-Modul" );
    gbcExt.gridy++;
    this.tabExt.add( this.cbFloppyDisk, gbcExt );

    this.cbJoystick  = GUIFactory.createCheckBox( "Joystick" );
    gbcExt.insets.top = 0;
    gbcExt.gridy++;
    this.tabExt.add( this.cbJoystick, gbcExt );

    this.cbK1520Sound = GUIFactory.createCheckBox( "K1520-Sound-Karte" );
    gbcExt.gridy++;
    this.tabExt.add( this.cbK1520Sound, gbcExt );

    this.cbKCNet = GUIFactory.createCheckBox(
				"KCNet-kompatible Netzwerkkarte" );
    gbcExt.gridy++;
    this.tabExt.add( this.cbKCNet, gbcExt );

    this.cbVDIP = GUIFactory.createCheckBox(
				"USB-Anschluss (Vinculum VDIP Modul)" );
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.cbVDIP, gbcExt );


    // Tab Sonstiges
    this.tabEtc = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbRatio43 = GUIFactory.createCheckBox(
				"Bildschirmausgabe im Format 4:3" );
    this.tabEtc.add( this.cbRatio43, gbcEtc );

    this.cbPasteFast = GUIFactory.createCheckBox(
		"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs" );
    gbcEtc.insets.top    = 0;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbPasteFast, gbcEtc );

    gbcEtc.fill          = GridBagConstraints.HORIZONTAL;
    gbcEtc.weightx       = 1.0;
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.bottom = 10;
    gbcEtc.gridy++;
    this.tabEtc.add( GUIFactory.createSeparator(), gbcEtc );

    this.fldAltOS = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + LLC2.PROP_OS_PREFIX,
		"Alternatives Monitorprogramm (0000h-0FFFh):" );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    this.fldAltFont = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + LLC2.PROP_FONT_PREFIX,
				"Alternativer Zeichensatz:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltFont, gbcEtc );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
				settingsFrm,
				propPrefix,
				LLC2.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
				true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
				settingsFrm,
				propPrefix,
				LLC2.getAutoInputCharSet(),
				LLC2.DEFAULT_SWAP_KEY_CHAR_CASE,
				LLC2.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.cbFloppyDisk.addActionListener( this );
    this.cbJoystick.addActionListener( this );
    this.cbK1520Sound.addActionListener( this );
    this.cbKCNet.addActionListener( this );
    this.cbVDIP.addActionListener( this );
    this.cbPasteFast.addActionListener( this );
    this.cbRatio43.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    Component tab = null;
    try {

      // Tab SCCH-Modul 1
      tab = this.tabSCCH;
      this.tabSCCH.applyInput( props, selected );

      // Tab RAM-Floppies
      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      // Tab GIDE
      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      // Tab Sonstiges
      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + LLC2.PROP_FDC_ENABLED,
		this.cbFloppyDisk.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + LLC2.PROP_JOYSTICK_ENABLED,
		this.cbJoystick.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + LLC2.PROP_K1520SOUND_ENABLED,
		this.cbK1520Sound.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + LLC2.PROP_KCNET_ENABLED,
		this.cbKCNet.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + LLC2.PROP_VDIP_ENABLED,
		this.cbVDIP.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + LLC2.PROP_PASTE_FAST,
		this.cbPasteFast.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + LLC2.PROP_SCREEN_RATIO,
		this.cbRatio43.isSelected() ?
				LLC2.VALUE_SCREEN_RATIO_43
				: LLC2.VALUE_SCREEN_RATIO_UNSCALED );
      this.fldAltOS.applyInput( props, selected );
      this.fldAltFont.applyInput( props, selected );

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
    this.settingsFrm.setWaitCursor( true );

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
    this.settingsFrm.setWaitCursor( false );
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    this.tabSCCH.updFields( props );
    this.tabRF.updFields( props );
    this.tabGIDE.updFields( props );
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );

    this.cbFloppyDisk.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + LLC2.PROP_FDC_ENABLED,
			false ) );

    this.cbJoystick.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + LLC2.PROP_JOYSTICK_ENABLED,
			false ) );

    this.cbK1520Sound.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + LLC2.PROP_K1520SOUND_ENABLED,
			false ) );

    this.cbKCNet.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + LLC2.PROP_KCNET_ENABLED,
			false ) );

    this.cbVDIP.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + LLC2.PROP_VDIP_ENABLED,
			false ) );

    this.cbPasteFast.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + LLC2.PROP_PASTE_FAST,
			false ) );

    this.cbRatio43.setSelected(
		EmuUtil.getProperty(
			props,
			this.propPrefix + LLC2.PROP_SCREEN_RATIO )
		.equals( LLC2.VALUE_SCREEN_RATIO_43 ) );

    this.fldAltOS.updFields( props );
    this.fldAltFont.updFields( props );
  }
}
