/*
 * (c) 2010-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die LLC2-Einstellungen
 */

package jkcemu.emusys.ac1_llc2;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.LLC2;


public class LLC2SettingsFld extends AbstractSettingsFld
{
  private static final int DEFAULT_AUTO_ACTION_WAIT_MILLIS = 800;

  private JTabbedPane            tabbedPane;
  private SCCHModule1SettingsFld tabSCCH;
  private RAMFloppiesSettingsFld tabRF;
  private JCheckBox              btnFloppyDisk;
  private JCheckBox              btnJoystick;
  private JCheckBox              btnKCNet;
  private JCheckBox              btnVDIP;
  private JCheckBox              btnPasteFast;
  private JCheckBox              btnRatio43;
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
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab SCCH-Modul 1
    this.tabSCCH = new SCCHModule1SettingsFld(
					settingsFrm,
					propPrefix + "scch." );
    this.tabbedPane.addTab( "SCCH-Modul 1", this.tabSCCH );


    // Tab RAM-Floppies
    this.tabRF = new RAMFloppiesSettingsFld(
		settingsFrm,
		propPrefix,
		"RAM-Floppy nach MP 3/88 (256 KByte) an E/A-Adressen D0h-D7h",
		RAMFloppy.RFType.MP_3_1988,
		"RAM-Floppy nach MP 3/88 (256 KByte) an E/A-Adressen B0h-B7h",
		RAMFloppy.RFType.MP_3_1988 );
    this.tabbedPane.addTab( "RAM-Floppies", this.tabRF );


    // Tab GIDE
    this.tabGIDE = new GIDESettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );


    // Tab Erweiterungen
    this.tabExt = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Erweiterungen", this.tabExt );

    GridBagConstraints gbcExt = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnFloppyDisk = new JCheckBox( "Floppy-Disk-Modul", false );
    gbcExt.gridy++;
    this.tabExt.add( this.btnFloppyDisk, gbcExt );

    this.btnKCNet     = new JCheckBox( "KCNet-kompatible Netzwerkkarte" );
    gbcExt.insets.top = 0;
    gbcExt.gridy++;
    this.tabExt.add( this.btnKCNet, gbcExt );

    this.btnVDIP = new JCheckBox( "USB-Anschluss (Vinculum VDIP Modul)" );
    gbcExt.gridy++;
    this.tabExt.add( this.btnVDIP, gbcExt );

    this.btnJoystick     = new JCheckBox( "Joystick" );
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.btnJoystick, gbcExt );


    // Tab Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnRatio43 = new JCheckBox(
				"Bildschirmausgabe im Format 4:3",
				false );
    this.tabEtc.add( this.btnRatio43, gbcEtc );

    this.btnPasteFast = new JCheckBox(
		"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs" );
    gbcEtc.insets.top    = 0;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnPasteFast, gbcEtc );

    gbcEtc.fill          = GridBagConstraints.HORIZONTAL;
    gbcEtc.weightx       = 1.0;
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.bottom = 10;
    gbcEtc.gridy++;
    this.tabEtc.add( new JSeparator(), gbcEtc );

    this.fldAltOS = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "os.",
		"Alternatives Monitorprogramm (0000h-0FFFh):" );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    this.fldAltFont = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + "font.",
				"Alternativer Zeichensatz:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltFont, gbcEtc );


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
					LLC2.getDefaultSwapKeyCharCase(),
					DEFAULT_AUTO_ACTION_WAIT_MILLIS );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.btnFloppyDisk.addActionListener( this );
    this.btnKCNet.addActionListener( this );
    this.btnVDIP.addActionListener( this );
    this.btnJoystick.addActionListener( this );
    this.btnPasteFast.addActionListener( this );
    this.btnRatio43.addActionListener( this );
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
		this.propPrefix + "floppydisk.enabled",
		this.btnFloppyDisk.isSelected() );
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
		this.propPrefix + "joystick.enabled",
		this.btnJoystick.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "paste.fast",
		this.btnPasteFast.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "screen.ratio",
		this.btnRatio43.isSelected() ? "4:3" : "unscaled" );
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
  public void lookAndFeelChanged()
  {
    this.fldAltOS.lookAndFeelChanged();
    this.fldAltFont.lookAndFeelChanged();
    this.tabSCCH.lookAndFeelChanged();
    this.tabRF.lookAndFeelChanged();
    this.tabGIDE.lookAndFeelChanged();
    this.tabAutoLoad.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    this.tabSCCH.updFields( props );
    this.tabRF.updFields( props );
    this.tabGIDE.updFields( props );
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );

    this.btnFloppyDisk.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "floppydisk.enabled",
			false ) );

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

    this.btnJoystick.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "joystick.enabled",
			false ) );

    this.btnPasteFast.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "paste.fast",
			false ) );

    this.btnRatio43.setSelected(
		EmuUtil.getProperty(
			props,
			this.propPrefix + "screen.ratio" ).startsWith( "4" ) );

    this.fldAltOS.updFields( props );
    this.fldAltFont.updFields( props );
  }
}
