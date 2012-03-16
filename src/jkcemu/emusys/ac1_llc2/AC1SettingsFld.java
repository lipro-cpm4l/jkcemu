/*
 * (c) 2010-2012 Jens Mueller
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
  private JRadioButton           btnMon2010;
  private JCheckBox              btnColor;
  private JCheckBox              btnFloppyDisk;
  private JCheckBox              btnKCNet;
  private JCheckBox              btnVDIP;
  private JCheckBox              btnPasteFast;
  private ROMFileSettingsFld     fldAltOS;
  private ROMFileSettingsFld     fldAltFont;
  private ROMFileSettingsFld     fldAltPio2Rom2010;
  private ROMFileSettingsFld     fldRomBank2010;
  private JPanel                 tab2010;
  private JPanel                 tabModel;
  private RAMFloppySettingsFld   tabRF;
  private HardDiskSettingsFld    tabGIDE;
  private SCCHModule1SettingsFld tabSCCH;
  private JPanel                 tabExt;
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

    this.btnMon2010 = new JRadioButton(
		"AC1-2010 mit Monitorprogramm f\u00FCr Farbgrafik",
		true );
    grpModel.add( this.btnMon2010 );
    gbcModel.insets.bottom = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.btnMon2010, gbcModel );


    // SCCH-Modul 1
    this.tabSCCH = new SCCHModule1SettingsFld(
					settingsFrm,
					propPrefix + "scch.");
    this.tabbedPane.addTab( "SCCH-Modul 1", this.tabSCCH );
    updSCCHFieldsEnabled();


    // AC1-2010
    this.tab2010 = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "AC1-2010", this.tab2010 );

    GridBagConstraints gbc2010 = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.fldAltPio2Rom2010 = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "2010.pio2rom.",
		"Alternativer Inhalt des ROMs auf der PIO2-Karte"
					+ " (4 x 2000h-27FFh):" );
    this.tab2010.add( this.fldAltPio2Rom2010, gbc2010 );

    this.fldRomBank2010 = new ROMFileSettingsFld(
			settingsFrm,
			propPrefix + "2010.rombank.",
			"Inhalt der ROM-Bank (16 x ab A000h):" );
    gbc2010.gridy++;
    this.tab2010.add( this.fldRomBank2010, gbc2010 );
    upd2010FieldsEnabled();


    // Bereich RAM-Floppy
    this.tabRF = new RAMFloppySettingsFld(
		settingsFrm,
		propPrefix + "ramfloppy.",
		"RAM-Floppy nach MP 3/88 (256 KByte) an IO-Adressen E0h-E7h",
		RAMFloppy.RFType.MP_3_1988 );
    this.tabbedPane.addTab( "RAM-Floppy", this.tabRF );


    // GIDE
    this.tabGIDE = new HardDiskSettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );
    updGIDEFieldsEnabled();


    // Erweiterungen
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

    this.btnColor = new JCheckBox(
			"Farbgrafik mit Taktfrequenzumschaltung",
			false );
    this.tabExt.add( this.btnColor, gbcExt );

    this.btnFloppyDisk = new JCheckBox( "Floppy-Disk-Modul", false );
    gbcExt.insets.top = 0;
    gbcExt.gridy++;
    this.tabExt.add( this.btnFloppyDisk, gbcExt );

    this.btnKCNet = new JCheckBox( "KCNet-kompatible Netzwerkkarte", false );
    gbcExt.gridy++;
    this.tabExt.add( this.btnKCNet, gbcExt );

    this.btnVDIP = new JCheckBox(
			"USB-Anschluss (Vinculum VDIP Modul)",
			false );
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.btnVDIP, gbcExt );


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

    this.btnPasteFast = new JCheckBox(
		"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs",
		true );
    this.tabEtc.add( this.btnPasteFast, gbcEtc );

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
    this.btnMon2010.addActionListener( this );
    this.btnColor.addActionListener( this );
    this.btnFloppyDisk.addActionListener( this );
    this.btnKCNet.addActionListener( this );
    this.btnVDIP.addActionListener( this );
    this.btnPasteFast.addActionListener( this );
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
      else if( this.btnMon2010.isSelected() ) {
	os = "2010";
      }
      props.setProperty( this.propPrefix + "os.version", os );

      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      tab = this.tabSCCH;
      this.tabSCCH.applyInput( props, selected );

      tab = this.tab2010;
      this.fldAltPio2Rom2010.applyInput( props, selected );
      this.fldRomBank2010.applyInput( props, selected );

      tab = this.tabExt;
      EmuUtil.setProperty(
		props,
		this.propPrefix + "color",
		this.btnColor.isSelected() );
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

      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + "paste.fast",
		this.btnPasteFast.isSelected() );
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
	  || (src == this.btnMon2010) )
      {
	rv = true;
	upd2010FieldsEnabled();
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
    } else if( os.equals( "2010" ) ) {
      this.btnMon2010.setSelected( true );
    } else {
      this.btnMonSCCH1088.setSelected( true );
    }
    this.tabRF.updFields( props );
    this.tabGIDE.updFields( props );
    this.tabSCCH.updFields( props );
    this.fldAltPio2Rom2010.updFields( props );
    this.fldRomBank2010.updFields( props );

    this.btnColor.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "color",
			false ) );

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

    this.btnPasteFast.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "ac1.paste.fast",
			false ) );

    this.fldAltOS.updFields( props );
    this.fldAltFont.updFields( props );

    updSCCHFieldsEnabled();
    upd2010FieldsEnabled();
    updGIDEFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void upd2010FieldsEnabled()
  {
    boolean state = this.btnMon2010.isSelected();
    this.fldAltPio2Rom2010.setEnabled( state );
    this.fldRomBank2010.setEnabled( state );
  }


  private void updGIDEFieldsEnabled()
  {
    this.tabGIDE.setEnabled( this.btnMon31_64x32.isSelected()
				|| this.btnMonSCCH80.isSelected()
				|| this.btnMonSCCH1088.isSelected()
				|| this.btnMon2010.isSelected() );
  }


  private void updSCCHFieldsEnabled()
  {
    this.tabSCCH.setEnabled( this.btnMonSCCH80.isSelected()
				|| this.btnMonSCCH1088.isSelected() );
  }
}
