/*
 * (c) 2010-2011 Jens Mueller
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


public class LLC2SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane            tabbedPane;
  private SCCHModule1SettingsFld tabSCCH;
  private RAMFloppiesSettingsFld tabRF;
  private JCheckBox              btnFloppyDisk;
  private ROMFileSettingsFld     fldAltOS;
  private ROMFileSettingsFld     fldAltFont;
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

    // Sonstiges
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

    this.btnFloppyDisk = new JCheckBox(
				"Floppy-Disk-Modul emulieren ",
				false );
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnFloppyDisk, gbcEtc );

    gbcEtc.fill    = GridBagConstraints.HORIZONTAL;
    gbcEtc.weightx = 1.0;
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

      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		"jkcemu.llc2.floppydisk.enabled",
		this.btnFloppyDisk.isSelected() );
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
  public void lookAndFeelChanged()
  {
    this.tabSCCH.lookAndFeelChanged();
    this.tabRF.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    this.tabSCCH.updFields( props );
    this.tabRF.updFields( props );

    this.btnFloppyDisk.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.llc2.floppydisk.enabled",
				false ) );

    this.fldAltOS.updFields( props );
    this.fldAltFont.updFields( props );
  }
}
