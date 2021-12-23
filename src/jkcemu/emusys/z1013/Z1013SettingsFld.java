/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Z1013-Einstellungen
 */

package jkcemu.emusys.z1013;

import java.awt.Component;
import java.awt.BorderLayout;
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
import javax.swing.JTabbedPane;
import javax.swing.JSeparator;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.RAMFloppy;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.Z1013;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.AutoLoadSettingsFld;
import jkcemu.settings.RAMFloppiesSettingsFld;
import jkcemu.settings.RAMFloppySettingsFld;
import jkcemu.settings.SettingsFrm;


public class Z1013SettingsFld extends AbstractSettingsFld
{
  private static final String LABEL_ALT_FONT = "Alternativer Zeichensatz:";
  private static final String LABEL_EXT_ROM  = "Inhalt der ROM-Erweiterung:";

  private JTabbedPane            tabbedPane;
  private JRadioButton           rbZ1013_01;
  private JRadioButton           rbZ1013_12;
  private JRadioButton           rbZ1013_16;
  private JRadioButton           rbZ1013_64;
  private JRadioButton           rbMon202;
  private JRadioButton           rbMonA2;
  private JRadioButton           rbMonRB_K7659;
  private JRadioButton           rbMonRB_S6009;
  private JRadioButton           rbMonINCOM_K7669;
  private JRadioButton           rbMonJM_1992;
  private JRadioButton           rbBL4_K7659;
  private JRadioButton           rbPortNone;
  private JRadioButton           rbPortJoyJuTe_6_87;
  private JRadioButton           rbPortJoyPractic_4_87;
  private JRadioButton           rbPortJoyPractic_1_88;
  private JRadioButton           rbPortCentr7Practic_2_89;
  private JRadioButton           rbPortCentr8FA_10_90;
  private JCheckBox              cbExtRomBasic;
  private JCheckBox              cbExtRomMega;
  private JCheckBox              cbExtRom8000;
  private JCheckBox              cbExtRom8000onReset;
  private JCheckBox              cbPetersCard;
  private JCheckBox              cbFixedScreenSize;
  private JCheckBox              cbFloppyDisk;
  private JCheckBox              cbRTC;
  private JCheckBox              cbK1520Sound;
  private JCheckBox              cbKCNet;
  private JCheckBox              cbVDIP;
  private JCheckBox              cbGraphicCCJ;
  private JCheckBox              cbGraphicKRT;
  private JCheckBox              cbGraphicPoppe;
  private JCheckBox              cbGraphicZX;
  private JCheckBox              cbCatchPrintCalls;
  private JCheckBox              cbCatchJoyCalls;
  private JCheckBox              cbPasteFast;
  private ROMFileSettingsFld     fldAltOS;
  private ROMFileSettingsFld     fldAltFont;
  private ROMFileSettingsFld     fldAltFont2;
  private ROMFileSettingsFld     fldExtRom;
  private JPanel                 tabModel;
  private JPanel                 tabMon;
  private JPanel                 tabUserPort;
  private RAMFloppiesSettingsFld tabRF;
  private GIDESettingsFld        tabGIDE;
  private JPanel                 tabExtRom;
  private JPanel                 tabExtGraph;
  private JPanel                 tabExtEtc;
  private JPanel                 tabEtc;
  private AutoLoadSettingsFld    tabAutoLoad;
  private AutoInputSettingsFld   tabAutoInput;


  public Z1013SettingsFld( SettingsFrm settingsFrm, String propPrefix )
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

    this.rbZ1013_01 = GUIFactory.createRadioButton(
				"Z1013.01 (1 MHz, 16 KByte RAM)" );
    grpModel.add( this.rbZ1013_01 );
    this.tabModel.add( this.rbZ1013_01, gbcModel );

    this.rbZ1013_12 = GUIFactory.createRadioButton(
				"Z1013.12 (2 MHz, 1 KByte RAM)" );
    grpModel.add( this.rbZ1013_12 );
    gbcModel.insets.top = 0;
    gbcModel.gridy++;
    this.tabModel.add( this.rbZ1013_12, gbcModel );

    this.rbZ1013_16 = GUIFactory.createRadioButton(
				"Z1013.16 (2 MHz, 16 KByte RAM)" );
    grpModel.add( this.rbZ1013_16 );
    gbcModel.gridy++;
    this.tabModel.add( this.rbZ1013_16, gbcModel );

    this.rbZ1013_64 = GUIFactory.createRadioButton(
				"Z1013.64 (2 MHz, 64 KByte RAM)",
				true );
    grpModel.add( this.rbZ1013_64 );
    gbcModel.gridy++;
    this.tabModel.add( this.rbZ1013_64, gbcModel );


    // Tab Monitorprogramm / Tastatur
    this.tabMon = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Monitorprogramm / Tastatur", this.tabMon );

    GridBagConstraints gbcMon = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpMon = new ButtonGroup();

    this.rbMon202 = GUIFactory.createRadioButton(
		"Monitorprogramm 2.02 / Folienflachtastatur",
		true );
    grpMon.add( this.rbMon202 );
    this.tabMon.add( this.rbMon202, gbcMon );

    this.rbMonA2 = GUIFactory.createRadioButton(
		"Monitorprogramm A.2 / Alphatastatur" );
    grpMon.add( this.rbMonA2 );
    gbcMon.insets.top = 0;
    gbcMon.gridy++;
    this.tabMon.add( this.rbMonA2, gbcMon );

    this.rbMonRB_K7659 = GUIFactory.createRadioButton(
		"Brosig-Monitorprogramm 2.028 / Tastatur K7659" );
    grpMon.add( this.rbMonRB_K7659 );
    gbcMon.gridy++;
    this.tabMon.add( this.rbMonRB_K7659, gbcMon );

    this.rbMonRB_S6009 = GUIFactory.createRadioButton(
		"Brosig-Monitorprogramm 2.028 / Tastatur S6009" );
    grpMon.add( this.rbMonRB_S6009 );
    gbcMon.gridy++;
    this.tabMon.add( this.rbMonRB_S6009, gbcMon );

    this.rbMonINCOM_K7669 = GUIFactory.createRadioButton(
		"INCOM-Monitorprogramm 2.2 / Tastatur K7669" );
    grpMon.add( this.rbMonINCOM_K7669 );
    gbcMon.gridy++;
    this.tabMon.add( this.rbMonINCOM_K7669, gbcMon );

    this.rbMonJM_1992 = GUIFactory.createRadioButton(
		"M\u00FCller-Monitorprogramm 1992 / Folienflachtastatur" );
    grpMon.add( this.rbMonJM_1992 );
    gbcMon.gridy++;
    this.tabMon.add( this.rbMonJM_1992, gbcMon );

    this.rbBL4_K7659 = GUIFactory.createRadioButton(
		"Boot Lader 4 / Tastatur K7659 (Boot-Diskette einlegen!)" );
    grpMon.add( this.rbBL4_K7659 );
    gbcMon.insets.bottom = 5;
    gbcMon.gridy++;
    this.tabMon.add( this.rbBL4_K7659, gbcMon );


    // Tab Anwendertor
    this.tabUserPort = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Anwendertor", this.tabUserPort );

    GridBagConstraints gbcUserPort = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.tabUserPort.add(
	GUIFactory.createLabel(
		"Am Anwendertor (User Port) emulierte Hardware:" ),
	gbcUserPort );

    ButtonGroup grpPort = new ButtonGroup();

    this.rbPortNone = GUIFactory.createRadioButton(
		"Keine angeschlossene Hardware emulieren",
		true );
    grpPort.add( this.rbPortNone );
    gbcUserPort.insets.left   = 50;
    gbcUserPort.insets.top    = 0;
    gbcUserPort.insets.bottom = 0;
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.rbPortNone, gbcUserPort );

    this.rbPortJoyJuTe_6_87 = GUIFactory.createRadioButton(
		"1 Spielhebel nach Ju+Te 6/1987" );
    grpPort.add( this.rbPortJoyJuTe_6_87 );
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.rbPortJoyJuTe_6_87, gbcUserPort );

    this.rbPortJoyPractic_4_87 = GUIFactory.createRadioButton(
		"2 Spielhebel nach practic 4/1987" );
    grpPort.add( this.rbPortJoyPractic_4_87 );
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.rbPortJoyPractic_4_87, gbcUserPort );

    this.rbPortJoyPractic_1_88 = GUIFactory.createRadioButton(
		"2 Spielhebel nach practic 1/1988" );
    grpPort.add( this.rbPortJoyPractic_1_88 );
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.rbPortJoyPractic_1_88, gbcUserPort );

    this.rbPortCentr7Practic_2_89 = GUIFactory.createRadioButton(
		"Drucker an 7-Bit-Centronics-Anschluss"
					+ " nach practic 2/1989" );
    grpPort.add( this.rbPortCentr7Practic_2_89 );
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.rbPortCentr7Practic_2_89, gbcUserPort );

    this.rbPortCentr8FA_10_90 = GUIFactory.createRadioButton(
		"Drucker an 8-Bit-Centronics-Anschluss nach FA 10/1990" );
    grpPort.add( this.rbPortCentr8FA_10_90 );
    gbcUserPort.insets.bottom = 5;
    gbcUserPort.gridy++;
    this.tabUserPort.add( this.rbPortCentr8FA_10_90, gbcUserPort );


    // Tab GIDE
    this.tabGIDE = new GIDESettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );


    // Tab RAM-Floppies
    this.tabRF = new RAMFloppiesSettingsFld(
	settingsFrm,
	propPrefix,
	"RAM-Floppy nach MP 3/1988 (256 KByte) an E/A-Adressen 98h-9Fh",
	RAMFloppy.RFType.MP_3_1988,
	"RAM-Floppy nach MP 3/1988 (256 KByte) an E/A-Adressen 58h-5Fh",
	RAMFloppy.RFType.MP_3_1988 );
    this.tabbedPane.addTab( "RAM-Floppies", this.tabRF );


    // Tab ROM-Erweiterungen
    this.tabExtRom = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "ROM-Erweiterungen", this.tabExtRom );

    GridBagConstraints gbcExtRom = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.cbExtRomBasic = GUIFactory.createCheckBox(
				"KC-BASIC-Modul (C000h-EBFFh)" );
    this.tabExtRom.add( this.cbExtRomBasic, gbcExtRom );

    this.cbExtRomMega = GUIFactory.createCheckBox(
				"Mega-ROM-Modul (256 x C000h-E7FFh)" );
    gbcExtRom.insets.top = 0;
    gbcExtRom.gridy++;
    this.tabExtRom.add( this.cbExtRomMega, gbcExtRom );

    this.cbExtRom8000 = GUIFactory.createCheckBox(
		"32K-ROM entsprechend Z1013-128 (8000h-FFFFh)" );
    gbcExtRom.gridy++;
    this.tabExtRom.add( this.cbExtRom8000, gbcExtRom );

    this.cbExtRom8000onReset = GUIFactory.createCheckBox(
					"Nach RESET eingeblendet" );
    gbcExtRom.insets.left = 50;
    gbcExtRom.gridy++;
    this.tabExtRom.add( this.cbExtRom8000onReset, gbcExtRom );

    this.fldExtRom = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + Z1013.PROP_EXTROM_PREFIX,
		LABEL_EXT_ROM );
    gbcExtRom.insets.top    = 10;
    gbcExtRom.insets.left   = 5;
    gbcExtRom.insets.bottom = 5;
    gbcExtRom.fill          = GridBagConstraints.HORIZONTAL;
    gbcExtRom.weightx       = 1.0;
    gbcExtRom.gridy++;
    this.tabExtRom.add( this.fldExtRom, gbcExtRom );


    // Tab Grafik-Erweiterungen
    this.tabExtGraph = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Grafik-Erweiterungen", this.tabExtGraph );

    GridBagConstraints gbcExtGraph = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.tabExtGraph.add(
	GUIFactory.createLabel( "Prim\u00E4re Bildschirmausgabe:" ),
	gbcExtGraph );

    this.cbGraphicKRT = GUIFactory.createCheckBox(
		"Vollgrafik nach KRT 11 und FA 7/1991" );
    gbcExtGraph.insets.top  = 0;
    gbcExtGraph.insets.left = 50;
    gbcExtGraph.gridy++;
    this.tabExtGraph.add( this.cbGraphicKRT, gbcExtGraph );

    this.cbPetersCard = GUIFactory.createCheckBox(
		"Peters-Platine (32x32 und 64x16 Zeichen)" );
    gbcExtGraph.gridy++;
    this.tabExtGraph.add( this.cbPetersCard, gbcExtGraph );

    this.cbFixedScreenSize = GUIFactory.createCheckBox(
	"Gleiche Fenstergr\u00F6\u00DFe bei 32x32 und 64x16 Zeichen" );
    gbcExtGraph.insets.left = 100;
    gbcExtGraph.gridy++;
    this.tabExtGraph.add( this.cbFixedScreenSize, gbcExtGraph );

    gbcExtGraph.insets.top  = 10;
    gbcExtGraph.insets.left = 5;
    gbcExtGraph.gridy++;
    this.tabExtGraph.add(
	GUIFactory.createLabel( "Zus\u00E4tzliche Bildschirmausgabe"
				+ " (zweite Anzeigeeinheit):" ),
	gbcExtGraph );

    this.cbGraphicZX = GUIFactory.createCheckBox(
	"ZX-Spectrum-kompatible S/W-Vollgrafik nach practic 2/1988" );
    gbcExtGraph.insets.top  = 0;
    gbcExtGraph.insets.left = 50;
    gbcExtGraph.gridy++;
    this.tabExtGraph.add( this.cbGraphicZX, gbcExtGraph );

    this.cbGraphicCCJ = GUIFactory.createCheckBox(
		"Grafikkarte des CC Jena (80x25 Zeichen)" );
    gbcExtGraph.gridy++;
    this.tabExtGraph.add( this.cbGraphicCCJ, gbcExtGraph );

    this.cbGraphicPoppe = GUIFactory.createCheckBox(
		"Farbgrafikkarte (32x32 und 64x32 Zeichen)" );
    gbcExtGraph.gridy++;
    this.tabExtGraph.add( this.cbGraphicPoppe, gbcExtGraph );

    this.fldAltFont2 = new ROMFileSettingsFld(
			settingsFrm,
			this.propPrefix + Z1013.PROP_GRA2_FONT_PREFIX,
			LABEL_ALT_FONT );
    gbcExtGraph.insets.top    = 10;
    gbcExtGraph.insets.bottom = 5;
    gbcExtGraph.weightx       = 1.0;
    gbcExtGraph.gridy++;
    this.tabExtGraph.add( this.fldAltFont2, gbcExtGraph );


    // Tab Sonstige Erweiterungen
    this.tabExtEtc = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstige Erweiterungen", this.tabExtEtc );

    GridBagConstraints gbcExtEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbFloppyDisk = GUIFactory.createCheckBox( "Floppy-Disk-Modul" );
    this.tabExtEtc.add( this.cbFloppyDisk, gbcExtEtc );

    this.cbKCNet = GUIFactory.createCheckBox(
				"KCNet-kompatible Netzwerkkarte" );
    gbcExtEtc.insets.top = 0;
    gbcExtEtc.gridy++;
    this.tabExtEtc.add( this.cbKCNet, gbcExtEtc );

    this.cbK1520Sound = GUIFactory.createCheckBox( "K1520-Sound-Karte" );
    gbcExtEtc.gridy++;
    this.tabExtEtc.add( this.cbK1520Sound, gbcExtEtc );

    this.cbVDIP = GUIFactory.createCheckBox(
				"USB-Anschluss (Vinculum VDIP Modul)" );
    gbcExtEtc.gridy++;
    this.tabExtEtc.add( this.cbVDIP, gbcExtEtc );

    this.cbRTC = GUIFactory.createCheckBox( "Echtzeituhr (RTC)" );
    gbcExtEtc.gridy++;
    this.tabExtEtc.add( this.cbRTC, gbcExtEtc );


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

    this.cbCatchPrintCalls = GUIFactory.createCheckBox(
	"Sprungverteileraufrufe f\u00FCr Druckerausgaben abfangen" );
    this.tabEtc.add( this.cbCatchPrintCalls, gbcEtc );

    this.cbCatchJoyCalls = GUIFactory.createCheckBox(
	"Sprungverteileraufrufe f\u00FCr Joystick-Abfragen abfangen" );
    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbCatchJoyCalls, gbcEtc );

    this.cbPasteFast = GUIFactory.createCheckBox(
	"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs" );
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
		propPrefix + Z1013.PROP_OS_PREFIX,
                "Alternatives Monitorprogramm (F000h-FFFFh):" );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    this.fldAltFont = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + Z1013.PROP_FONT_PREFIX,
		LABEL_ALT_FONT );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltFont, gbcEtc );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
			settingsFrm,
			propPrefix,
			Z1013.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
			true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
			settingsFrm,
			propPrefix,
			Z1013.getAutoInputCharSet(),
			Z1013.DEFAULT_SWAP_KEY_CHAR_CASE,
			Z1013.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Aktivierung der Schaltflaechen
    updExtRomFieldsEnabled();
    updPetersCardDependFieldsEnabled();
    updAltFont2FieldsEnabled();


    // Listener
    this.rbZ1013_01.addActionListener( this );
    this.rbZ1013_12.addActionListener( this );
    this.rbZ1013_16.addActionListener( this );
    this.rbZ1013_64.addActionListener( this );

    this.rbMon202.addActionListener( this );
    this.rbMonA2.addActionListener( this );
    this.rbMonRB_K7659.addActionListener( this );
    this.rbMonRB_S6009.addActionListener( this );
    this.rbMonINCOM_K7669.addActionListener( this );
    this.rbMonJM_1992.addActionListener( this );
    this.rbBL4_K7659.addActionListener( this );

    this.rbPortNone.addActionListener( this );
    this.rbPortJoyJuTe_6_87.addActionListener( this );
    this.rbPortJoyPractic_4_87.addActionListener( this );
    this.rbPortJoyPractic_1_88.addActionListener( this );
    this.rbPortCentr7Practic_2_89.addActionListener( this );
    this.rbPortCentr8FA_10_90.addActionListener( this );

    this.cbExtRomBasic.addActionListener( this );
    this.cbExtRomMega.addActionListener( this );
    this.cbExtRom8000.addActionListener( this );
    this.cbExtRom8000onReset.addActionListener( this );

    this.cbPetersCard.addActionListener( this );
    this.cbGraphicPoppe.addActionListener( this );
    this.cbFixedScreenSize.addActionListener( this );
    this.cbFloppyDisk.addActionListener( this );
    this.cbKCNet.addActionListener( this );
    this.cbK1520Sound.addActionListener( this );
    this.cbVDIP.addActionListener( this );
    this.cbRTC.addActionListener( this );
    this.cbGraphicKRT.addActionListener( this );
    this.cbGraphicZX.addActionListener( this );
    this.cbGraphicCCJ.addActionListener( this );

    this.cbCatchPrintCalls.addActionListener( this );
    this.cbCatchJoyCalls.addActionListener( this );
    this.cbPasteFast.addActionListener( this );
  }


  public String getModelSysName()
  {
    String rv = Z1013.SYSNAME_Z1013_64;
    if( this.rbZ1013_01.isSelected() ) {
      rv = Z1013.SYSNAME_Z1013_01;
    } else if( this.rbZ1013_12.isSelected() ) {
      rv = Z1013.SYSNAME_Z1013_12;
    } else if( this.rbZ1013_16.isSelected() ) {
      rv = Z1013.SYSNAME_Z1013_16;
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws
					UserCancelException,
					UserInputException
  {
    Component tab  = null;
    String    text = null;
    try {

      // Tab Monitorprogramm / Tastatur
      tab  = this.tabModel;
      text = Z1013.VALUE_MON_202;
      if( this.rbMonA2.isSelected() ) {
	text = Z1013.VALUE_MON_A2;
      }
      else if( this.rbMonRB_K7659.isSelected() ) {
	text = Z1013.VALUE_MON_RB_K7659;
      }
      else if( this.rbMonRB_S6009.isSelected() ) {
	text = Z1013.VALUE_MON_RB_S6009;
      }
      else if( this.rbMonINCOM_K7669.isSelected() ) {
	text = Z1013.VALUE_MON_INCOM_K7669;
      }
      else if( this.rbMonJM_1992.isSelected() ) {
	text = Z1013.VALUE_MON_JM_1992;
      }
      else if( this.rbBL4_K7659.isSelected() ) {
	text = Z1013.VALUE_MON_BL4_K7659;
      }
      EmuUtil.setProperty(
		props,
		Z1013.PROP_PREFIX + Z1013.PROP_MONITOR,
		text );

      // Tab Anwendertor
      tab  = this.tabUserPort;
      text = Z1013.VALUE_NONE;
      if( this.rbPortJoyJuTe_6_87.isSelected() ) {
	text = Z1013.VALUE_JOYST_JUTE0687;
      } else if( this.rbPortJoyPractic_4_87.isSelected() ) {
	text = Z1013.VALUE_JOYST_PRAC0487;
      } else if( this.rbPortJoyPractic_1_88.isSelected() ) {
	text = Z1013.VALUE_JOYST_PRAC0188;
      } else if( this.rbPortCentr7Practic_2_89.isSelected() ) {
	text = Z1013.VALUE_CENTR7_PRAC0289;
      } else if( this.rbPortCentr8FA_10_90.isSelected() ) {
	text = Z1013.VALUE_CENTR8_FA1090;
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_USERPORT,
		text );

      // Tab GIDE
      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      // Tab RAM-Floppies
      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      // Tab ROM-Erweiterungen
      tab  = this.tabExtRom;
      EmuUtil.setProperty(
		props,
		this.propPrefix
			+ Z1013.PROP_EXTROM_PREFIX
			+ Z1013.PROP_BASIC_ENABLED,
		this.cbExtRomBasic.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix
			+ Z1013.PROP_EXTROM_PREFIX
			+ Z1013.PROP_MEGA_ENABLED,
		this.cbExtRomMega.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix
			+ Z1013.PROP_EXTROM_PREFIX
			+ Z1013.PROP_8000_ENABLED,
		this.cbExtRom8000.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix
			+ Z1013.PROP_EXTROM_PREFIX
			+ Z1013.PROP_8000_ON_RESET,
		this.cbExtRom8000onReset.isSelected() );
      this.fldExtRom.applyInput( props, selected );
      if( selected && (this.fldExtRom.getFile() == null) ) {
	if( this.cbExtRomMega.isSelected() ) {
	  throw new UserInputException(
		"Das Mega-ROM-Modul hat keinen Inhalt.\n"
			+ "Bitte w\u00E4hlen Sie eine ROM-Datei aus!" );
	}
	if( this.cbExtRom8000.isSelected() ) {
	  throw new UserInputException(
		"Der 32K-ROM entsprechend Z1013-128 hat keinen Inhalt.\n"
			+ "Bitte w\u00E4hlen Sie eine ROM-Datei aus!" );
	}
      }

      // Tab Grafik-Erweiterungen
      tab = this.tabExtGraph;
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_GRA_KRT_ENABLED,
		this.cbGraphicKRT.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_PETERS_ENABLED,
		this.cbPetersCard.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_FIXED_SCREEN_SIZE,
		this.cbFixedScreenSize.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_GRA_ZX_ENABLED,
		this.cbGraphicZX.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_GRA_CCJ_ENABLED,
		this.cbGraphicCCJ.isSelected()
			&& !this.cbGraphicZX.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_GRA_POPPE_ENABLED,
		this.cbGraphicPoppe.isSelected()
			&& !this.cbGraphicCCJ.isSelected()
			&& !this.cbGraphicZX.isSelected() );
      this.fldAltFont2.applyInput( props, selected );

      // Tab Sonstige Erweiterungen
      tab = this.tabExtEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_FDC_ENABLED,
		this.cbFloppyDisk.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_RTC_ENABLED,
		this.cbRTC.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_K1520SOUND_ENABLED,
		this.cbK1520Sound.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_KCNET_ENABLED,
		this.cbKCNet.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_VDIP_ENABLED,
		this.cbVDIP.isSelected() );

      // Tab Sonstiges
      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_CATCH_PRINT_CALLS,
		this.cbCatchPrintCalls.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_CATCH_JOY_CALLS,
		this.cbCatchJoyCalls.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z1013.PROP_PASTE_FAST,
		this.cbPasteFast.isSelected() );
      this.fldAltOS.applyInput( props, selected );
      this.fldAltFont.applyInput( props, selected );

      // Tab AutoLoad
      tab = this.tabAutoLoad;
      this.tabAutoLoad.applyInput( props, selected );

      // Tab AutoInput
      tab = this.tabAutoInput;
      this.tabAutoInput.applyInput( props, selected );

      // ggf. Warnung ausgeben
      if( selected
	  && this.rbMonA2.isSelected()
	  && this.cbExtRomBasic.isSelected()
	  && (this.fldExtRom.getFile() == null) )
      {
	confirmConflictSettings(
		"Das im ROM-Modul enthaltene KC-BASIC l\u00E4uft nicht"
			+ " mit dem Monitorprogramm A.2 zusammen.\n"
			+ "Sie sollten deshalb entweder ein anderes"
			+ " Monitorprogramm ausw\u00E4hlen"
			+ " oder f\u00FCr das ROM-Modul\n"
			+ "einen alternativen, an das Monitorprogramm"
			+ " angepassten Inhalt einbinden." );
      }
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
    if( (src == this.rbZ1013_01)
	|| (src == this.rbZ1013_12)
	|| (src == this.rbZ1013_16)
	|| (src == this.rbZ1013_64) )
    {
      rv = true;
      fireDataChanged();
      this.settingsFrm.fireUpdSpeedTab();
    }
    else if( src == this.cbExtRomBasic ) {
      rv = true;
      if( this.cbExtRomBasic.isSelected() ) {
	if( this.cbExtRomMega.isSelected() ) {
	  this.cbExtRomMega.setSelected( false );
	}
	if( this.cbExtRom8000.isSelected() ) {
	  this.cbExtRom8000.setSelected( false );
	}
      }
      updExtRomFieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbExtRomMega ) {
      rv = true;
      if( this.cbExtRomMega.isSelected() ) {
	if( this.cbExtRomBasic.isSelected() ) {
	  this.cbExtRomBasic.setSelected( false );
	}
	if( this.cbExtRom8000.isSelected() ) {
	  this.cbExtRom8000.setSelected( false );
	}
      }
      updExtRomFieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbExtRom8000 ) {
      rv = true;
      if( this.cbExtRom8000.isSelected() ) {
	if( this.cbExtRomBasic.isSelected() ) {
	  this.cbExtRomBasic.setSelected( false );
	}
	if( this.cbExtRomMega.isSelected() ) {
	  this.cbExtRomMega.setSelected( false );
	}
	if( this.cbPetersCard.isSelected() ) {
	  this.cbPetersCard.setSelected( false );
	  updPetersCardDependFieldsEnabled();
	  BaseDlg.fireShowSuppressableInfoDlg(
		this,
		"Die Peters-Platine wurde deaktiviert,"
			+ " da diese und die ROM-Erweiterung\n"
			+ "entsprechend Z1013-128"
			+ " die E/A-Adresse 4 unterschiedlich verwenden." );
	}
      }
      updExtRomFieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbPetersCard ) {
      rv = true;
      if( this.cbPetersCard.isSelected()
	  && this.cbExtRom8000.isSelected() )
      {
	this.cbExtRom8000.setSelected( false );
	updExtRomFieldsEnabled();
	BaseDlg.fireShowSuppressableInfoDlg(
		this,
		"Die ROM-Erweiterung ab Adresse 8000h entsprechend"
			+ " Z1013-128 wurde deaktiviert,\n"
			+ "da diese und die Peters-Platine"
			+ " die E/A-Adresse 4 unterschiedlich verwenden." );
      }
      updPetersCardDependFieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbGraphicCCJ ) {
      rv = true;
      if( this.cbGraphicCCJ.isSelected() ) {
	if( this.cbGraphicPoppe.isSelected() ) {
	  this.cbGraphicPoppe.setSelected( false );
	}
	if( this.cbGraphicZX.isSelected() ) {
	  this.cbGraphicZX.setSelected( false );
	}
      }
      updAltFont2FieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbGraphicPoppe ) {
      rv = true;
      if( this.cbGraphicPoppe.isSelected() ) {
	if( this.cbGraphicCCJ.isSelected() ) {
	  this.cbGraphicCCJ.setSelected( false );
	}
	if( this.cbGraphicZX.isSelected() ) {
	  this.cbGraphicZX.setSelected( false );
	}
      }
      updAltFont2FieldsEnabled();
      fireDataChanged();
    }
    else if( src == this.cbGraphicZX ) {
      rv = true;
      if( this.cbGraphicZX.isSelected() ) {
	if( this.cbGraphicCCJ.isSelected() ) {
	  this.cbGraphicCCJ.setSelected( false );
	  updAltFont2FieldsEnabled();
	}
	if( this.cbGraphicPoppe.isSelected() ) {
	  this.cbGraphicPoppe.setSelected( false );
	  updAltFont2FieldsEnabled();
	}
      }
      fireDataChanged();
    }
    else if( src instanceof AbstractButton ) {
      rv = true;
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
    this.settingsFrm.setWaitCursor( false );
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    // Tab Monitorprogramm / Tastatur
    switch( EmuUtil.getProperty( props, EmuThread.PROP_SYSNAME ) ) {
      case Z1013.SYSNAME_Z1013_01:
	this.rbZ1013_01.setSelected( true );
	break;
      case Z1013.SYSNAME_Z1013_12:
	this.rbZ1013_12.setSelected( true );
	break;
      case Z1013.SYSNAME_Z1013_16:
	this.rbZ1013_16.setSelected( true );
	break;
      default:
	this.rbZ1013_64.setSelected( true );
    }
    String mon = EmuUtil.getProperty(
			props,
			Z1013.PROP_PREFIX + Z1013.PROP_MONITOR );
    if( mon.equals( Z1013.VALUE_MON_A2 ) ) {
      this.rbMonA2.setSelected( true );
    } else if( mon.equals( Z1013.VALUE_MON_RB_K7659 ) ) {
      this.rbMonRB_K7659.setSelected( true );
    } else if( mon.equals( Z1013.VALUE_MON_RB_S6009 ) ) {
      this.rbMonRB_S6009.setSelected( true );
    } else if( mon.equals( Z1013.VALUE_MON_INCOM_K7669 ) ) {
      this.rbMonINCOM_K7669.setSelected( true );
    } else if( mon.equals( Z1013.VALUE_MON_JM_1992 ) ) {
      this.rbMonJM_1992.setSelected( true );
    } else if( mon.equals( Z1013.VALUE_MON_BL4_K7659 ) ) {
      this.rbBL4_K7659.setSelected( true );
    } else {
      this.rbMon202.setSelected( true );
    }

    // Tab Anwendertor
    switch( EmuUtil.getProperty(
			props,
			this.propPrefix + Z1013.PROP_USERPORT ) )
    {
      case Z1013.VALUE_JOYST_JUTE0687:
	this.rbPortJoyJuTe_6_87.setSelected( true );
	break;
      case Z1013.VALUE_JOYST_PRAC0487:
	this.rbPortJoyPractic_4_87.setSelected( true );
	break;
      case Z1013.VALUE_JOYST_PRAC0188:
	this.rbPortJoyPractic_1_88.setSelected( true );
	break;
      case Z1013.VALUE_CENTR7_PRAC0289:
	this.rbPortCentr7Practic_2_89.setSelected( true );
	break;
      case Z1013.VALUE_CENTR8_FA1090:
	this.rbPortCentr8FA_10_90.setSelected( true );
	break;
      default:
	this.rbPortNone.setSelected( true );
    }

    // Tab GIDE
    this.tabGIDE.updFields( props );

    // Tab RAM-Floppies
    this.tabRF.updFields( props );

    // Tab ROM-Erweiterungen
    this.cbExtRomBasic.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix
				+ Z1013.PROP_EXTROM_PREFIX
				+ Z1013.PROP_BASIC_ENABLED,
			false ) );
    this.cbExtRomMega.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix
				+ Z1013.PROP_EXTROM_PREFIX
				+ Z1013.PROP_MEGA_ENABLED,
			false )
		&& !this.cbExtRomBasic.isSelected() );
    this.cbExtRom8000.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix
				+ Z1013.PROP_EXTROM_PREFIX
				+ Z1013.PROP_8000_ENABLED,
			false )
		&& !this.cbExtRomBasic.isSelected()
		&& !this.cbExtRomMega.isSelected() );
    this.cbExtRom8000onReset.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix
				+ Z1013.PROP_EXTROM_PREFIX
				+ Z1013.PROP_8000_ON_RESET,
			false ) );
    this.fldExtRom.updFields( props );
    updExtRomFieldsEnabled();

    // Tab Grafik-Erweiterungen
    this.cbGraphicKRT.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_GRA_KRT_ENABLED,
			false ) );
    this.cbPetersCard.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_PETERS_ENABLED,
			false )
		&& !this.cbExtRom8000.isSelected() );
    this.cbFixedScreenSize.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_FIXED_SCREEN_SIZE,
			false ) );
    this.cbGraphicZX.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_GRA_ZX_ENABLED,
			false ) );
    this.cbGraphicCCJ.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_GRA_CCJ_ENABLED,
			false )
		&& !this.cbGraphicZX.isSelected() );
    this.cbGraphicPoppe.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_GRA_POPPE_ENABLED,
			false )
		&& !this.cbGraphicZX.isSelected()
		&& !this.cbGraphicCCJ.isSelected() );
    updPetersCardDependFieldsEnabled();
    updAltFont2FieldsEnabled();
    this.fldAltFont2.updFields( props );

    // Tab Sonstige Erweiterungen
    this.cbFloppyDisk.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_FDC_ENABLED,
			false ) );
    this.cbKCNet.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_KCNET_ENABLED,
			false ) );
    this.cbK1520Sound.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_K1520SOUND_ENABLED,
			false ) );
    this.cbVDIP.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_VDIP_ENABLED,
			false ) );
    this.cbRTC.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_RTC_ENABLED,
			false ) );

    // Tab Sonstiges
    this.cbCatchPrintCalls.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_CATCH_PRINT_CALLS,
			true ) );
    this.cbCatchJoyCalls.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_CATCH_JOY_CALLS,
			true ) );
    this.cbPasteFast.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z1013.PROP_PASTE_FAST,
			true ) );
    this.fldAltOS.updFields( props );
    this.fldAltFont.updFields( props );

    // Tab AutoLoad
    this.tabAutoLoad.updFields( props );

    // Tab AutoInput
    this.tabAutoInput.updFields( props );
  }


	/* --- private Methoden --- */

  private void updAltFont2FieldsEnabled()
  {
    String text = LABEL_ALT_FONT;
    if( this.cbGraphicPoppe.isSelected() ) {
      text = "Alternativer Zeichensatz im Modus 64x32 Zeichen:";
    } else if( this.cbGraphicCCJ.isSelected() ) {
      text = "Alternativer Zeichensatz der Gafikkarte des CC Jena:";
    }
    this.fldAltFont2.setLabelText( text );
    this.fldAltFont2.setEnabled(
		this.cbGraphicPoppe.isSelected()
			|| this.cbGraphicCCJ.isSelected() );
  }


  private void updExtRomFieldsEnabled()
  {
    boolean rom8000 = false;
    String  text    = null;
    if( this.cbExtRomBasic.isSelected() ) {
      text = "Alternativer Inhalt des KC-BASIC-Moduls:";
    } else if( this.cbExtRomMega.isSelected() ) {
      text = "Inhalt des Mega-ROM-Moduls:";
    } else if( this.cbExtRom8000.isSelected() ) {
      rom8000 = true;
      text    = "Inhalt des 32K-ROMs:";
    }
    this.cbExtRom8000onReset.setEnabled( rom8000 );
    if( text != null ) {
      this.fldExtRom.setLabelText( text );
      this.fldExtRom.setEnabled( true );
    } else {
      this.fldExtRom.setLabelText( LABEL_EXT_ROM );
      this.fldExtRom.setEnabled( false );
    }
  }


  private void updPetersCardDependFieldsEnabled()
  {
    this.cbFixedScreenSize.setEnabled(
		this.cbPetersCard.isSelected() );
  }
}
