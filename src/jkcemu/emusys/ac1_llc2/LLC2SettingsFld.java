/*
 * (c) 2010-2013 Jens Mueller
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


public class LLC2SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane            tabbedPane;
  private SCCHModule1SettingsFld tabSCCH;
  private RAMFloppiesSettingsFld tabRF;
  private JCheckBox              btnFloppyDisk;
  private JCheckBox              btnJoystick;
  private JCheckBox              btnKCNet;
  private JCheckBox              btnVDIP;
  private ROMFileSettingsFld     fldAltOS;
  private ROMFileSettingsFld     fldAltFont;
  private GIDESettingsFld        tabGIDE;
  private JPanel                 tabEtc;


  public LLC2SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // SCCH-Modul 1
    this.tabSCCH = new SCCHModule1SettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "SCCH-Modul 1", this.tabSCCH );


    // RAM-Floppies
    this.tabRF = new RAMFloppiesSettingsFld(
		settingsFrm,
		propPrefix,
		"RAM-Floppy nach MP 3/88 (256 KByte) an IO-Adressen D0h-D7h",
		RAMFloppy.RFType.MP_3_1988,
		"RAM-Floppy nach MP 3/88 (256 KByte) an IO-Adressen B0h-B7h",
		RAMFloppy.RFType.MP_3_1988 );
    this.tabbedPane.addTab( "RAM-Floppies", this.tabRF );


    // GIDE
    this.tabGIDE = new GIDESettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );

    // Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnFloppyDisk = new JCheckBox( "Floppy-Disk-Modul", false );
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnFloppyDisk, gbcEtc );

    this.btnKCNet = new JCheckBox(
			"KCNet-kompatible Netzwerkkarte",
			false );
    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnKCNet, gbcEtc );

    this.btnVDIP = new JCheckBox(
			"USB-Anschluss (Vinculum VDIP Modul)",
			false );
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnVDIP, gbcEtc );

    this.btnJoystick     = new JCheckBox( "Joystick", false );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnJoystick, gbcEtc );

    gbcEtc.fill       = GridBagConstraints.HORIZONTAL;
    gbcEtc.weightx    = 1.0;
    gbcEtc.insets.top = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( new JSeparator(), gbcEtc );

    this.fldAltOS = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "os.",
		"Alternatives Monitorprogramm (0000h-0FFFh):" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    this.fldAltFont = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + "font.",
				"Alternativer Zeichensatz:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltFont, gbcEtc );


    // Listener
    this.btnFloppyDisk.addActionListener( this );
    this.btnKCNet.addActionListener( this );
    this.btnVDIP.addActionListener( this );
    this.btnJoystick.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    Component tab = null;
    try {
      tab = this.tabSCCH;
      this.tabSCCH.applyInput( props, selected );

      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

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
      this.fldAltOS.applyInput( props, selected );
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
    this.settingsFrm.setWaitCursor( true );

    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src instanceof AbstractButton ) {
        rv = true;
        fireDataChanged();
      }
    }
    if( !rv ) {
      rv = this.tabGIDE.doAction( e );
    }
    this.settingsFrm.setWaitCursor( false );
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.tabSCCH.lookAndFeelChanged();
    this.tabRF.lookAndFeelChanged();
    this.tabGIDE.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    this.tabSCCH.updFields( props );
    this.tabRF.updFields( props );
    this.tabGIDE.updFields( props );

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

    this.fldAltOS.updFields( props );
    this.fldAltFont.updFields( props );
  }
}
