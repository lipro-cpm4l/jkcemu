/*
 * (c) 2010-2021 Jens Mueller
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
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.RAMFloppy;
import jkcemu.base.UserInputException;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.AC1;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.AutoLoadSettingsFld;
import jkcemu.settings.RAMFloppySettingsFld;
import jkcemu.settings.SettingsFrm;


public class AC1SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane            tabbedPane;
  private JRadioButton           rbMon31_64x16;
  private JRadioButton           rbMon31_64x32;
  private JRadioButton           rbMonSCCH80;
  private JRadioButton           rbMonSCCH1088;
  private JRadioButton           rbMon2010;
  private JRadioButton           rbCtcAllInSeries;
  private JRadioButton           rbCtcM1ToClk2;
  private JCheckBox              cbCtcT0ToSound;
  private JCheckBox              cbColor;
  private JCheckBox              cbFloppyDisk;
  private JCheckBox              cbJoystick;
  private JCheckBox              cbK1520Sound;
  private JCheckBox              cbKCNet;
  private JCheckBox              cbVDIP;
  private JCheckBox              cbPasteFast;
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
    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Modell
    this.tabModel = GUIFactory.createPanel( new GridBagLayout() );
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

    this.rbMon31_64x16 = GUIFactory.createRadioButton(
				"Ur-AC1 mit Monitorprogramm 3.1" );
    grpModel.add( this.rbMon31_64x16 );
    gbcModel.insets.top = 0;
    gbcModel.gridy++;
    this.tabModel.add( this.rbMon31_64x16, gbcModel );

    this.rbMon31_64x32 = GUIFactory.createRadioButton(
				"AC1-ACC mit Monitorprogramm 3.1" );
    grpModel.add( this.rbMon31_64x32 );
    gbcModel.gridy++;
    this.tabModel.add( this.rbMon31_64x32, gbcModel );

    this.rbMonSCCH80 = GUIFactory.createRadioButton(
				"AC1-SCCH mit Monitorprogramm 8.0" );
    grpModel.add( this.rbMonSCCH80 );
    gbcModel.gridy++;
    this.tabModel.add( this.rbMonSCCH80, gbcModel );

    this.rbMonSCCH1088 = GUIFactory.createRadioButton(
	"AC1-SCCH mit Monitorprogramm 10/88 und Zeichensatzumschaltung",
	true );
    grpModel.add( this.rbMonSCCH1088 );
    gbcModel.gridy++;
    this.tabModel.add( this.rbMonSCCH1088, gbcModel );

    this.rbMon2010 = GUIFactory.createRadioButton(
		"AC1-2010 mit Monitorprogramm f\u00FCr Farbgrafik" );
    grpModel.add( this.rbMon2010 );
    gbcModel.insets.bottom = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.rbMon2010, gbcModel );


    // Tab SCCH-Modul 1
    this.tabSCCH = new SCCHModule1SettingsFld(
				settingsFrm,
				propPrefix + AC1.PROP_SCCH_PREFIX);
    this.tabbedPane.addTab( "SCCH-Modul 1", this.tabSCCH );
    updSCCHFieldsEnabled();


    // Tab AC1-2010
    this.tab2010 = GUIFactory.createPanel( new GridBagLayout() );
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
	"RAM-Floppy nach MP 3/1988 (256 KByte) an E/A-Adressen E0h-E7h",
	RAMFloppy.RFType.MP_3_1988 );
    this.tabbedPane.addTab( "RAM-Floppy", this.tabRF );


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

    this.cbColor = GUIFactory.createCheckBox(
		"Farbgrafik mit Taktfrequenz- und Zeichensatzumschaltung" );
    this.tabExt.add( this.cbColor, gbcExt );

    this.cbFloppyDisk = GUIFactory.createCheckBox( "Floppy-Disk-Modul" );
    gbcExt.insets.top  = 0;
    gbcExt.gridy++;
    this.tabExt.add( this.cbFloppyDisk, gbcExt );

    this.cbJoystick = GUIFactory.createCheckBox( "Joystick" );
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


    // Tab CTC
    this.tabCtc = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "CTC", this.tabCtc );

    GridBagConstraints gbcCtc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.REMAINDER,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.tabCtc.add(
		GUIFactory.createLabel( "Kopplung der CTC-Kan\u00E4le:" ),
		gbcCtc );

    ButtonGroup grpCTC = new ButtonGroup();

    this.rbCtcAllInSeries = GUIFactory.createRadioButton(
		"Alle Kan\u00E4le in Reihe gekoppelt",
		true );
    grpCTC.add( this.rbCtcAllInSeries );
    gbcCtc.insets.left   = 50;
    gbcCtc.insets.bottom = 0;
    gbcCtc.gridy++;
    this.tabCtc.add( this.rbCtcAllInSeries, gbcCtc );

    this.rbCtcM1ToClk2 = GUIFactory.createRadioButton(
	"/M1 an Kanal 2, die anderen Kan\u00E4le in Reihe gekoppelt" );
    grpCTC.add( this.rbCtcM1ToClk2 );
    gbcCtc.insets.top = 0;
    gbcCtc.gridy++;
    this.tabCtc.add( this.rbCtcM1ToClk2, gbcCtc );

    this.cbCtcT0ToSound = GUIFactory.createCheckBox(
				"CTC-Kanal 0 zum Lautsprecher" );
    gbcCtc.insets.top    = 10;
    gbcCtc.insets.left   = 5;
    gbcCtc.insets.bottom = 5;
    gbcCtc.gridy++;
    this.tabCtc.add( this.cbCtcT0ToSound, gbcCtc );


    // Tab Sonstiges
    this.tabEtc = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.cbPasteFast = GUIFactory.createCheckBox(
		"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs" );
    this.tabEtc.add( this.cbPasteFast, gbcEtc );

    gbcEtc.insets.top    = 10;
    gbcEtc.insets.bottom = 10;
    gbcEtc.gridy++;
    this.tabEtc.add( GUIFactory.createSeparator(), gbcEtc );

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
				AC1.getAutoInputCharSet(),
				AC1.DEFAULT_SWAP_KEY_CHAR_CASE,
				AC1.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.rbMon31_64x16.addActionListener( this );
    this.rbMon31_64x32.addActionListener( this );
    this.rbMonSCCH80.addActionListener( this );
    this.rbMonSCCH1088.addActionListener( this );
    this.rbMon2010.addActionListener( this );
    this.cbColor.addActionListener( this );
    this.cbFloppyDisk.addActionListener( this );
    this.cbKCNet.addActionListener( this );
    this.cbVDIP.addActionListener( this );
    this.cbJoystick.addActionListener( this );
    this.cbK1520Sound.addActionListener( this );
    this.rbCtcAllInSeries.addActionListener( this );
    this.rbCtcM1ToClk2.addActionListener( this );
    this.cbCtcT0ToSound.addActionListener( this );
    this.cbPasteFast.addActionListener( this );
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
      if( this.rbMon31_64x16.isSelected() ) {
	os = AC1.VALUE_MON_31_64X16;
      }
      else if( this.rbMonSCCH80.isSelected() ) {
	os = AC1.VALUE_MON_SCCH80;
      }
      else if( this.rbMonSCCH1088.isSelected() ) {
	os = AC1.VALUE_MON_SCCH1088;
      }
      else if( this.rbMon2010.isSelected() ) {
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
		this.cbColor.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_FDC_ENABLED,
		this.cbFloppyDisk.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_JOYSTICK_ENABLED,
		this.cbJoystick.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_K1520SOUND_ENABLED,
		this.cbK1520Sound.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_KCNET_ENABLED,
		this.cbKCNet.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_VDIP_ENABLED,
		this.cbVDIP.isSelected() );

      // Tab CTC
      tab = this.tabCtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_M1_TO_CTC_CLK2,
		this.rbCtcM1ToClk2.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_CTC_T0_TO_SOUND,
		this.cbCtcT0ToSound.isSelected() );


      // Tab Sonstiges
      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + AC1.PROP_PASTE_FAST,
		this.cbPasteFast.isSelected() );
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
      if( (src == this.rbMon31_64x16)
	  || (src == this.rbMon31_64x32)
	  || (src == this.rbMonSCCH80)
	  || (src == this.rbMonSCCH1088)
	  || (src == this.rbMon2010) )
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
  public void updFields( Properties props )
  {
    String os = EmuUtil.getProperty(
			props,
			this.propPrefix + AC1.PROP_OS_VERSION );
    if( os.equals( AC1.VALUE_MON_31_64X16 ) ) {
      this.rbMon31_64x16.setSelected( true );
    } else if( os.equals( AC1.VALUE_MON_31_64X32 ) ) {
      this.rbMon31_64x32.setSelected( true );
    } else if( os.equals( AC1.VALUE_MON_SCCH80 ) ) {
      this.rbMonSCCH80.setSelected( true );
    } else if( os.equals( AC1.VALUE_MON_2010 ) ) {
      this.rbMon2010.setSelected( true );
    } else {
      this.rbMonSCCH1088.setSelected( true );
    }
    this.tabRF.updFields( props );
    this.tabSCCH.updFields( props );
    this.tabGIDE.updFields( props );
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );
    this.fldAltPio2Rom2010.updFields( props );
    this.fldRomBank2010.updFields( props );

    this.cbColor.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_COLOR,
			false ) );

    this.cbFloppyDisk.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_FDC_ENABLED,
			false ) );

    this.cbJoystick.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_JOYSTICK_ENABLED,
			false ) );

    this.cbK1520Sound.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_K1520SOUND_ENABLED,
			false ) );

    this.cbKCNet.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_KCNET_ENABLED,
			false ) );

    this.cbVDIP.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_VDIP_ENABLED,
			false ) );

    if( EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_M1_TO_CTC_CLK2,
			false ) )
    {
      this.rbCtcM1ToClk2.setSelected( true );
    } else {
      this.rbCtcAllInSeries.setSelected( true );
    }

    this.cbCtcT0ToSound.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + AC1.PROP_CTC_T0_TO_SOUND,
			false ) );

    this.cbPasteFast.setSelected(
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
    boolean state = this.rbMon2010.isSelected();
    this.fldAltPio2Rom2010.setEnabled( state );
    this.fldRomBank2010.setEnabled( state );
  }


  private void updSCCHFieldsEnabled()
  {
    this.tabSCCH.setEnabled( this.rbMonSCCH80.isSelected()
				|| this.rbMonSCCH1088.isSelected() );
  }
}
