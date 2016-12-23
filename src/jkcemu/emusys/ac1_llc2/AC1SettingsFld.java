/*
 * (c) 2010-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die AC1-Einstellungen
 */

package jkcemu.emusys.ac1_llc2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import jkcemu.base.AbstractSettingsFld;
import jkcemu.base.AutoInputSettingsFld;
import jkcemu.base.AutoLoadSettingsFld;
import jkcemu.base.EmuUtil;
import jkcemu.base.RAMFloppy;
import jkcemu.base.RAMFloppySettingsFld;
import jkcemu.base.ROMFileSettingsFld;
import jkcemu.base.SettingsFrm;
import jkcemu.base.UserInputException;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.AC1;


public class AC1SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane            tabbedPane;
  private JRadioButton           btnMon31_64x16;
  private JRadioButton           btnMon31_64x32;
  private JRadioButton           btnMonSCCH80;
  private JRadioButton           btnMonSCCH1088;
  private JRadioButton           btnMon2010;
  private JRadioButton           btnCTCAllSerial;
  private JRadioButton           btnCTCM1ToClk2;
  private JCheckBox              btnColor;
  private JCheckBox              btnFloppyDisk;
  private JCheckBox              btnJoystick;
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
  private GIDESettingsFld        tabGIDE;
  private SCCHModule1SettingsFld tabSCCH;
  private JPanel                 tabExt;
  private JPanel                 tabCtc;
  private JPanel                 tabEtc;
  private AutoLoadSettingsFld    tabAutoLoad;
  private AutoInputSettingsFld   tabAutoInput;


  public AC1SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Modell
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


    // Tab SCCH-Modul 1
    this.tabSCCH = new SCCHModule1SettingsFld(
				settingsFrm,
				propPrefix + AC1.PROP_SCCH_PREFIX);
    this.tabbedPane.addTab( "SCCH-Modul 1", this.tabSCCH );
    updSCCHFieldsEnabled();


    // Tab AC1-2010
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
		propPrefix + AC1.PROP_2010_PIO2ROM_PREFIX,
		"Alternativer Inhalt der ROM-B\u00E4nke auf der PIO2-Karte"
					+ " (4 x 2000h-27FFh):" );
    this.tab2010.add( this.fldAltPio2Rom2010, gbc2010 );

    this.fldRomBank2010 = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + AC1.PROP_2010_ROMBANK_PREFIX,
		"Inhalt der weiteren 16 ROM-B\u00E4nke (16 x ab A000h):" );
    gbc2010.gridy++;
    this.tab2010.add( this.fldRomBank2010, gbc2010 );
    upd2010FieldsEnabled();


    // Tab RAM-Floppy
    this.tabRF = new RAMFloppySettingsFld(
		settingsFrm,
		propPrefix + AC1.PROP_RF_PREFIX,
		"RAM-Floppy nach MP 3/88 (256 KByte) an E/A-Adressen E0h-E7h",
		RAMFloppy.RFType.MP_3_1988 );
    this.tabbedPane.addTab( "RAM-Floppy", this.tabRF );


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

    this.btnColor = new JCheckBox(
		"Farbgrafik mit Taktfrequenz- und Zeichensatzumschaltung",
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
    gbcExt.gridy++;
    this.tabExt.add( this.btnVDIP, gbcExt );

    this.btnJoystick     = new JCheckBox( "Joystick", false );
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.btnJoystick, gbcExt );


    // Tab CTC
    this.tabCtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "CTC", this.tabCtc );

    GridBagConstraints gbcCtc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.REMAINDER,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tabCtc.add( new JLabel( "Kopplung der CTC-Kan\u00E4le:" ), gbcCtc );

    ButtonGroup grpCTC = new ButtonGroup();

    this.btnCTCAllSerial = new JRadioButton(
		"Alle Kan\u00E4le in Reihe gekoppelt",
		true );
    grpCTC.add( this.btnCTCAllSerial );
    gbcCtc.insets.left   = 50;
    gbcCtc.insets.bottom = 0;
    gbcCtc.gridy++;
    this.tabCtc.add( this.btnCTCAllSerial, gbcCtc );

    this.btnCTCM1ToClk2 = new JRadioButton(
		"/M1 an Kanal 2, die anderen Kan\u00E4le in Reihe gekoppelt",
		false );
    grpCTC.add( this.btnCTCM1ToClk2 );
    gbcCtc.insets.top    = 0;
    gbcCtc.insets.bottom = 5;
    gbcCtc.gridy++;
    this.tabCtc.add( this.btnCTCM1ToClk2, gbcCtc );


    // Tab Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.btnPasteFast = new JCheckBox(
		"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs" );
    this.tabEtc.add( this.btnPasteFast, gbcEtc );

    gbcEtc.insets.top    = 10;
    gbcEtc.insets.bottom = 10;
    gbcEtc.gridy++;
    this.tabEtc.add( new JSeparator(), gbcEtc );

    this.fldAltOS = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + AC1.PROP_OS_PREFIX,
		"Alternatives Monitorprogramm (0000h-0FFFh):" );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    this.fldAltFont = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + AC1.PROP_FONT_PREFIX,
				"Alternativer Zeichensatz:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltFont, gbcEtc );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
				settingsFrm,
				propPrefix,
				AC1.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
				true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
				settingsFrm,
				propPrefix,
				AC1.DEFAULT_SWAP_KEY_CHAR_CASE,
				AC1.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


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
    this.btnJoystick.addActionListener( this );
    this.btnCTCAllSerial.addActionListener( this );
    this.btnCTCM1ToClk2.addActionListener( this );
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

      // Tab Modell
      String os = AC1.VALUE_MON_31_64X32;
      if( this.btnMon31_64x16.isSelected() ) {
	os = AC1.VALUE_MON_31_64X16;
      }
      else if( this.btnMonSCCH80.isSelected() ) {
	os = AC1.VALUE_MON_SCCH80;
      }
      else if( this.btnMonSCCH1088.isSelected() ) {
	os = AC1.VALUE_MON_SCCH1088;
      }
      else if( this.btnMon2010.isSelected() ) {
	os = AC1.VALUE_MON_2010;
      }
      props.setProperty( this.propPrefix + AC1.PROP_OS_VERSION, os );

      // Tab RAM-Floppy
      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      // Tab GIDE
      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      // Tab SCCH-Modul 1
      tab = this.tabSCCH;
      this.tabSCCH.applyInput( props, selected );

      // AC1-2010
      tab = this.tab2010;
      this.fldAltPio2Rom2010.applyInput( props, selected );
      this.fldRomBank2010.applyInput( props, selected );

      // Erweiterungen
      tab = this.tabExt;
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_COLOR,
		this.btnColor.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_FDC_ENABLED,
		this.btnFloppyDisk.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_KCNET_ENABLED,
		this.btnKCNet.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_VDIP_ENABLED,
		this.btnVDIP.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_JOYSTICK_ENABLED,
		this.btnJoystick.isSelected() );

      // Tab Sonstiges
      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_M1_TO_CTC_CLK2,
		this.btnCTCM1ToClk2.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_PASTE_FAST,
		this.btnPasteFast.isSelected() );
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
      if( (src == this.btnMon31_64x16)
	  || (src == this.btnMon31_64x32)
	  || (src == this.btnMonSCCH80)
	  || (src == this.btnMonSCCH1088)
	  || (src == this.btnMon2010) )
      {
	rv = true;
	upd2010FieldsEnabled();
	updSCCHFieldsEnabled();
	fireDataChanged();
      }
      if( !rv ) {
	rv = this.tabGIDE.doAction( e );
      }
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
    this.fldAltPio2Rom2010.lookAndFeelChanged();
    this.fldRomBank2010.lookAndFeelChanged();
    this.tabSCCH.lookAndFeelChanged();
    this.tabRF.lookAndFeelChanged();
    this.tabGIDE.lookAndFeelChanged();
    this.tabAutoLoad.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    String os = EmuUtil.getProperty(
			props,
			this.propPrefix + AC1.PROP_OS_VERSION );
    if( os.equals( AC1.VALUE_MON_31_64X16 ) ) {
      this.btnMon31_64x16.setSelected( true );
    } else if( os.equals( AC1.VALUE_MON_31_64X32 ) ) {
      this.btnMon31_64x32.setSelected( true );
    } else if( os.equals( AC1.VALUE_MON_SCCH80 ) ) {
      this.btnMonSCCH80.setSelected( true );
    } else if( os.equals( AC1.VALUE_MON_2010 ) ) {
      this.btnMon2010.setSelected( true );
    } else {
      this.btnMonSCCH1088.setSelected( true );
    }
    this.tabRF.updFields( props );
    this.tabSCCH.updFields( props );
    this.tabGIDE.updFields( props );
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );
    this.fldAltPio2Rom2010.updFields( props );
    this.fldRomBank2010.updFields( props );

    this.btnColor.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_COLOR,
			false ) );

    this.btnFloppyDisk.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_FDC_ENABLED,
			false ) );

    this.btnKCNet.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_KCNET_ENABLED,
			false ) );

    this.btnVDIP.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_VDIP_ENABLED,
			false ) );

    this.btnJoystick.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_JOYSTICK_ENABLED,
			false ) );

    if( EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_M1_TO_CTC_CLK2,
			false ) )
    {
      this.btnCTCM1ToClk2.setSelected( true );
    } else {
      this.btnCTCAllSerial.setSelected( true );
    }

    this.btnPasteFast.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_PASTE_FAST,
			false ) );

    this.fldAltOS.updFields( props );
    this.fldAltFont.updFields( props );

    updSCCHFieldsEnabled();
    upd2010FieldsEnabled();
  }


	/* --- private Methoden --- */

  private void upd2010FieldsEnabled()
  {
    boolean state = this.btnMon2010.isSelected();
    this.fldAltPio2Rom2010.setEnabled( state );
    this.fldRomBank2010.setEnabled( state );
  }


  private void updSCCHFieldsEnabled()
  {
    this.tabSCCH.setEnabled( this.btnMonSCCH80.isSelected()
				|| this.btnMonSCCH1088.isSelected() );
  }
}
