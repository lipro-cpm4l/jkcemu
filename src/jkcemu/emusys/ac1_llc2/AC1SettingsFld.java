/*
 * (c) 2010-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die AC1-Einstellungen
 */

package jkcemu.emusys.ac1_llc2;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;
import jkcemu.disk.HardDiskSettingsFld;


public class AC1SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane            tabbedPane;
  private JRadioButton           btnMon31_64x16;
  private JRadioButton           btnMon31_64x32;
  private JRadioButton           btnMonSCCH80;
  private JRadioButton           btnMonSCCH1088;
  private JRadioButton           btnMonSCCH80_2010;
  private JCheckBox              btnFloppyDisk;
  private ROMFileSettingsFld     fldAltOS;
  private ROMFileSettingsFld     fldAltFont;
  private JPanel                 tabModel;
  private RAMFloppySettingsFld   tabRF;
  private HardDiskSettingsFld    tabGIDE;
  private SCCHModule1SettingsFld tabSCCH;
  private JPanel                 tabEtc;


  public AC1SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Modell
    this.tabModel = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Modell", this.tabModel );

    GridBagConstraints gbcModel = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpModel = new ButtonGroup();

    this.btnMon31_64x16 = new JRadioButton(
				"Ur-AC1 mit Monitorprogramm 3.1",
				false );
    grpModel.add( this.btnMon31_64x16 );
    gbcModel.insets.top = 0;
    gbcModel.gridy++;
    this.tabModel.add( this.btnMon31_64x16, gbcModel );

    this.btnMon31_64x32 = new JRadioButton(
				"AC1-ACC mit Monitorprogramm 3.1",
				false );
    grpModel.add( this.btnMon31_64x32 );
    gbcModel.gridy++;
    this.tabModel.add( this.btnMon31_64x32, gbcModel );

    this.btnMonSCCH80 = new JRadioButton(
				"AC1-SCCH mit Monitorprogramm 8.0",
				false );
    grpModel.add( this.btnMonSCCH80 );
    gbcModel.gridy++;
    this.tabModel.add( this.btnMonSCCH80, gbcModel );

    this.btnMonSCCH1088 = new JRadioButton(
	"AC1-SCCH mit Monitorprogramm 10/88 und Zeichensatzumschaltung",
	true );
    grpModel.add( this.btnMonSCCH1088 );
    gbcModel.gridy++;
    this.tabModel.add( this.btnMonSCCH1088, gbcModel );

    this.btnMonSCCH80_2010 = new JRadioButton(
		"AC1-2010 mit Monitorprogramm 8.0 und Inversschaltung",
		true );
    grpModel.add( this.btnMonSCCH80_2010 );
    gbcModel.insets.bottom = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.btnMonSCCH80_2010, gbcModel );


    // SCCH-Modul 1
    this.tabSCCH = new SCCHModule1SettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "SCCH-Modul 1", this.tabSCCH );
    updSCCHFieldsEnabled();


    // Bereich RAM-Floppy
    this.tabRF = new RAMFloppySettingsFld(
		settingsFrm,
		propPrefix + "ramfloppy.",
		"RAM-Floppy nach MP 3/88 (256 KByte) an IO-Adressen E0h-E7h",
		RAMFloppy.RFType.MP_3_1988 );
    this.tabbedPane.addTab( "RAM-Floppy", this.tabRF );


    // GIDE
    this.tabGIDE = new HardDiskSettingsFld(
					settingsFrm,
					propPrefix,
					0x80,
					0x20,
					0x30,
					0x50,
					0x60,
					0x70,
					0x80,
					0x90,
					0xA0,
					0xB0,
					0xC0,
					0xD0,
					0xF0 );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );
    updGIDEFieldsEnabled();


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
    this.btnMon31_64x16.addActionListener( this );
    this.btnMon31_64x32.addActionListener( this );
    this.btnMonSCCH80.addActionListener( this );
    this.btnMonSCCH1088.addActionListener( this );
    this.btnMonSCCH80_2010.addActionListener( this );
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
      tab = this.tabModel;

      String os = "3.1_64x32";
      if( this.btnMon31_64x16.isSelected() ) {
	os = "3.1_64x16";
      }
      else if( this.btnMonSCCH80.isSelected() ) {
	os = "SCCH8.0";
      }
      else if( this.btnMonSCCH1088.isSelected() ) {
	os = "SCCH10/88";
      }
      else if( this.btnMonSCCH80_2010.isSelected() ) {
	os = "SCCH8.0_2010";
      }
      props.setProperty( this.propPrefix + "os.version", os );

      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      tab = this.tabSCCH;
      this.tabSCCH.applyInput( props, selected );

      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		"jkcemu.ac1.floppydisk.enabled",
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
    this.settingsFrm.setWaitCursor( true );

    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( (src == this.btnMon31_64x16)
	  || (src == this.btnMon31_64x32)
	  || (src == this.btnMonSCCH80)
	  || (src == this.btnMonSCCH1088)
	  || (src == this.btnMonSCCH80_2010) )
      {
	rv = true;
	updSCCHFieldsEnabled();
	updGIDEFieldsEnabled();
	fireDataChanged();
      } else if( src instanceof AbstractButton ) {
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
    this.tabGIDE.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    String os = EmuUtil.getProperty( props, this.propPrefix + "os.version" );
    if( os.equals( "3.1_64x16" ) ) {
      this.btnMon31_64x16.setSelected( true );
    } else if( os.equals( "3.1_64x32" ) ) {
      this.btnMon31_64x32.setSelected( true );
    } else if( os.equals( "SCCH8.0" ) ) {
      this.btnMonSCCH80.setSelected( true );
    } else if( os.equals( "SCCH8.0_2010" ) ) {
      this.btnMonSCCH80_2010.setSelected( true );
    } else {
      this.btnMonSCCH1088.setSelected( true );
    }
    this.tabRF.updFields( props );
    this.tabGIDE.updFields( props );
    this.tabSCCH.updFields( props );

    this.btnFloppyDisk.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.ac1.floppydisk.enabled",
				false ) );

    this.fldAltOS.updFields( props );
    this.fldAltFont.updFields( props );

    updGIDEFieldsEnabled();
    updSCCHFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void updGIDEFieldsEnabled()
  {
    this.tabGIDE.setEnabled( this.btnMon31_64x32.isSelected()
				|| this.btnMonSCCH80.isSelected()
				|| this.btnMonSCCH1088.isSelected()
				|| this.btnMonSCCH80_2010.isSelected() );
  }


  private void updSCCHFieldsEnabled()
  {
    this.tabSCCH.setEnabled( this.btnMonSCCH80.isSelected()
				|| this.btnMonSCCH1088.isSelected()
				|| this.btnMonSCCH80_2010.isSelected() );
  }
}

