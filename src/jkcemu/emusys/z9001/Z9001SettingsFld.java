/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen der Computer Z9001, KC85/1 und KC87
 */

package jkcemu.emusys.z9001;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.RAMFloppy;
import jkcemu.base.UserInputException;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.Z9001;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.AutoLoadSettingsFld;
import jkcemu.settings.RAMFloppiesSettingsFld;
import jkcemu.settings.SettingsFrm;


public class Z9001SettingsFld extends AbstractSettingsFld
{
  private static final String DEFAULT_LABEL_ROM_MODULE =
				"Inhalt des ROM-Moduls:";

  private JTabbedPane                          tabbedPane;
  private JPanel                               tabEtc;
  private JPanel                               tabExt;
  private JPanel                               tabGraph;
  private JPanel                               tabMem;
  private JPanel                               tabPrinter;
  private GIDESettingsFld                      tabGIDE;
  private RAMFloppiesSettingsFld               tabRF;
  private AutoLoadSettingsFld                  tabAutoLoad;
  private AutoInputSettingsFld                 tabAutoInput;
  private JRadioButton                         rbMonoGraphNone;
  private JRadioButton                         rbMonoGraphKRT;
  private JRadioButton                         rbColorGraphNone;
  private JRadioButton                         rbColorGraphKRT;
  private JRadioButton                         rbColorGraphRobotron;
  private JCheckBox                            cbFontProgrammable;
  private JCheckBox                            cb80Chars;
  private JCheckBox                            cbFixedScreenSize;
  private JCheckBox                            cbRam16k4000;
  private JCheckBox                            cbRam16k8000;
  private JCheckBox                            cbRam64k;
  private JCheckBox                            cbRom16k4000;
  private JCheckBox                            cbRom32k4000;
  private JCheckBox                            cbRom16k8000;
  private JCheckBox                            cbRom10kC000;
  private JCheckBox                            cbRomMega;
  private JCheckBox                            cbRomBoot;
  private ROMFileSettingsFld                   fldRomModule;
  private JRadioButton                         rbCatchPrintCalls;
  private JRadioButton                         rbPrinterModule;
  private JRadioButton                         rbNoPrinter;
  private JCheckBox                            cbFloppyDisk;
  private JCheckBox                            cbK1520Sound;
  private JCheckBox                            cbKCNet;
  private JCheckBox                            cbPlotter;
  private JCheckBox                            cbRTC;
  private JCheckBox                            cbVDIP;
  private JCheckBox                            cbPasteFast;
  private ROMFileSettingsFld                   fldAltOS;
  private ROMFileSettingsFld                   fldAltBASIC;
  private ROMFileSettingsFld                   fldAltFont;
  private Map<AbstractButton,AbstractButton[]> switchOffMap;


  public Z9001SettingsFld(
		SettingsFrm settingsFrm,
		String      propPrefix,
		boolean     kc87 )
  {
    super( settingsFrm, propPrefix );
    this.switchOffMap = new HashMap<>();

    setLayout( new BorderLayout() );
    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Grafik
    this.tabGraph = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Grafik", this.tabGraph );

    GridBagConstraints gbcGraph = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpGraph = new ButtonGroup();

    this.rbMonoGraphNone = GUIFactory.createRadioButton(
					"S/W, Blockgrafik" );
    grpGraph.add( this.rbMonoGraphNone );
    this.tabGraph.add( this.rbMonoGraphNone, gbcGraph );

    this.rbMonoGraphKRT = GUIFactory.createRadioButton(
					"S/W, KRT-Vollgrafikerweiterung" );
    grpGraph.add( this.rbMonoGraphKRT );
    gbcGraph.insets.top = 0;
    gbcGraph.gridy++;
    this.tabGraph.add( this.rbMonoGraphKRT, gbcGraph );

    this.rbColorGraphNone = GUIFactory.createRadioButton(
					"Farbe, Blockgrafik",
					true );
    grpGraph.add( this.rbColorGraphNone );
    gbcGraph.gridy++;
    this.tabGraph.add( this.rbColorGraphNone, gbcGraph );

    this.rbColorGraphKRT = GUIFactory.createRadioButton(
				"Farbe, KRT-Vollgrafikerweiterung" );
    grpGraph.add( this.rbColorGraphKRT );
    gbcGraph.gridy++;
    this.tabGraph.add( this.rbColorGraphKRT, gbcGraph );

    this.rbColorGraphRobotron = GUIFactory.createRadioButton(
				"Farbe, Robotron-Vollgrafikerweiterung" );
    grpGraph.add( this.rbColorGraphRobotron );
    gbcGraph.gridy++;
    this.tabGraph.add( this.rbColorGraphRobotron, gbcGraph );

    this.cbFontProgrammable = GUIFactory.createCheckBox(
				"Programmierbarer Zeichengenerator" );
    gbcGraph.insets.top    = 10;
    gbcGraph.insets.bottom = 0;
    gbcGraph.gridy++;
    this.tabGraph.add( this.cbFontProgrammable, gbcGraph );

    this.cb80Chars = GUIFactory.createCheckBox(
					"40/80-Zeichen-Umschaltung" );
    gbcGraph.insets.top = 0;
    gbcGraph.gridy++;
    this.tabGraph.add( this.cb80Chars, gbcGraph );

    this.cbFixedScreenSize = GUIFactory.createCheckBox(
		"Gleiche Fenstergr\u00F6\u00DFe in beiden Bildschirmmodi" );
    gbcGraph.insets.left   = 50;
    gbcGraph.insets.bottom = 5;
    gbcGraph.gridy++;
    this.tabGraph.add( this.cbFixedScreenSize, gbcGraph );


    // Tab Speichermodule
    this.tabMem = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Speichermodule", this.tabMem );

    GridBagConstraints gbcMem = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbRam16k4000 = GUIFactory.createCheckBox(
					"16K RAM-Modul (4000h-7FFFh)" );
    this.tabMem.add( this.cbRam16k4000, gbcMem );

    this.cbRam16k8000 = GUIFactory.createCheckBox(
					"16K RAM-Modul (8000h-BFFFh)" );
    gbcMem.gridx++;
    this.tabMem.add( this.cbRam16k8000, gbcMem );

    this.cbRam64k = GUIFactory.createCheckBox(
		"64K RAM-Modul (2 x 4000h-7FFFh, 1 x 8000h-E7FFh)" );
    gbcMem.insets.top = 0;
    gbcMem.gridwidth  = 2;
    gbcMem.gridx      = 0;
    gbcMem.gridy++;
    this.tabMem.add( this.cbRam64k, gbcMem );

    this.cbRom16k4000 = GUIFactory.createCheckBox(
					"16K ROM-Modul (4000h-7FFFh)" );
    gbcMem.insets.top  = 10;
    gbcMem.gridwidth   = 1;
    gbcMem.gridy++;
    this.tabMem.add( this.cbRom16k4000, gbcMem );

    this.cbRom16k8000 = GUIFactory.createCheckBox(
					"16K ROM-Modul (8000h-BFFFh)" );
    gbcMem.gridx++;
    this.tabMem.add( this.cbRom16k8000, gbcMem );

    this.cbRom32k4000 = GUIFactory.createCheckBox(
					"32K ROM-Modul (4000h-BFFFh)" );
    gbcMem.insets.top  = 0;
    gbcMem.gridwidth   = 1;
    gbcMem.gridx       = 0;
    gbcMem.gridy++;
    this.tabMem.add( this.cbRom32k4000, gbcMem );

    this.cbRom10kC000 = GUIFactory.createCheckBox(
					"10K ROM-Modul (C000h-E7FFh)" );
    gbcMem.gridx++;
    this.tabMem.add( this.cbRom10kC000, gbcMem );

    this.cbRomMega  = GUIFactory.createCheckBox(
				"Mega-ROM-Modul (256 x C000h-E7FFh)" );
    gbcMem.gridwidth = 2;
    gbcMem.gridx     = 0;
    gbcMem.gridy++;
    this.tabMem.add( this.cbRomMega, gbcMem );

    this.cbRomBoot = GUIFactory.createCheckBox( "Boot-ROM-Modul"
		+ " (C000h-E7FFh, nur mit Floppy-Disk-Modul sinnvoll)" );
    gbcMem.gridy++;
    this.tabMem.add( this.cbRomBoot, gbcMem );

    this.fldRomModule = new ROMFileSettingsFld(
				settingsFrm,
				this.propPrefix + Z9001.PROP_ROM_MOD_PREFIX,
				DEFAULT_LABEL_ROM_MODULE );
    gbcMem.fill       = GridBagConstraints.HORIZONTAL;
    gbcMem.weightx    = 1.0;
    gbcMem.insets.top = 10;
    gbcMem.gridwidth  = GridBagConstraints.REMAINDER;
    gbcMem.gridy++;
    this.tabMem.add( this.fldRomModule, gbcMem );

    this.switchOffMap.put(
		this.cbRam16k4000,
		toArray(
			this.cbRam64k,
			this.cbRom16k4000,
			this.cbRom32k4000 ) );
    this.switchOffMap.put(
		this.cbRam16k8000,
		toArray(
			this.cbRam64k,
			this.cbRom32k4000,
			this.cbRom16k8000 ) );
    this.switchOffMap.put(
		this.cbRam64k,
		toArray(
			this.cbRam16k4000,
			this.cbRam16k8000,
			this.cbRom16k4000,
			this.cbRom32k4000,
			this.cbRom16k8000,
			this.cbRom10kC000 ) );
    this.switchOffMap.put(
		this.cbRom16k4000,
		toArray(
			this.cbRam16k4000,
			this.cbRam64k,
			this.cbRom32k4000,
			this.cbRom16k8000,
			this.cbRom10kC000,
			this.cbRomBoot,
			this.cbRomMega ) );
    this.switchOffMap.put(
		this.cbRom32k4000,
		toArray(
			this.cbRam16k4000,
			this.cbRam64k,
			this.cbRom16k4000,
			this.cbRom16k8000,
			this.cbRom10kC000,
			this.cbRomBoot,
			this.cbRomMega ) );
    this.switchOffMap.put(
		this.cbRom16k8000,
		toArray(
			this.cbRam16k8000,
			this.cbRam64k,
			this.cbRom16k4000,
			this.cbRom32k4000,
			this.cbRom10kC000,
			this.cbRomBoot,
			this.cbRomMega ) );
    this.switchOffMap.put(
		this.cbRom10kC000,
		toArray(
			this.cbRam64k,
			this.cbRom16k4000,
			this.cbRom32k4000,
			this.cbRom16k8000,
			this.cbRomBoot,
			this.cbRomMega ) );
    this.switchOffMap.put(
		this.cbRomBoot,
		toArray(
			this.cbRom16k4000,
			this.cbRom32k4000,
			this.cbRom16k8000,
			this.cbRom10kC000,
			this.cbRomMega ) );
    this.switchOffMap.put(
		this.cbRomMega,
		toArray(
			this.cbRom16k4000,
			this.cbRom32k4000,
			this.cbRom16k8000,
			this.cbRom10kC000,
			this.cbRomBoot ) );
    updMemFieldsEnabled();


    // Tab RAM-Floppies
    this.tabRF = new RAMFloppiesSettingsFld(
			settingsFrm,
			propPrefix,
			"RAM-Floppy an E/A-Adressen 20h/21h",
			RAMFloppy.RFType.ADW,
			"RAM-Floppy an E/A-Adressen 24h/25h",
			RAMFloppy.RFType.ADW );
    this.tabbedPane.addTab( "RAM-Floppies", this.tabRF );


    // Tab Drucker
    this.tabPrinter = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Drucker", this.tabPrinter );

    GridBagConstraints gbcPrinter = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpPrinter = new ButtonGroup();

    this.rbCatchPrintCalls = GUIFactory.createRadioButton(
			"BOS-Aufrufe f\u00FCr Druckerausgaben abfangen" );
    grpPrinter.add( this.rbCatchPrintCalls );
    this.tabPrinter.add( this.rbCatchPrintCalls, gbcPrinter );

    this.rbPrinterModule = GUIFactory.createRadioButton(
			"V.24-Druckermodul emulieren" );
    grpPrinter.add( this.rbPrinterModule );
    gbcPrinter.insets.top = 0;
    gbcPrinter.gridy++;
    this.tabPrinter.add( this.rbPrinterModule, gbcPrinter );

    this.rbNoPrinter = GUIFactory.createRadioButton(
			"Keinen Drucker emulieren",
			true );
    grpPrinter.add( this.rbNoPrinter );
    gbcPrinter.insets.bottom = 5;
    gbcPrinter.gridy++;
    this.tabPrinter.add( this.rbNoPrinter, gbcPrinter );


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

    this.cbRTC = GUIFactory.createCheckBox( "Echtzeituhr" );
    this.tabExt.add( this.cbRTC, gbcExt );

    this.cbFloppyDisk = GUIFactory.createCheckBox( "Floppy-Disk-Modul" );
    gbcExt.insets.top = 0;
    gbcExt.gridy++;
    this.tabExt.add( this.cbFloppyDisk, gbcExt );

    this.cbK1520Sound = GUIFactory.createCheckBox( "K1520-Sound-Karte" );
    gbcExt.gridy++;
    this.tabExt.add( this.cbK1520Sound, gbcExt );

    this.cbKCNet = GUIFactory.createCheckBox(
				"KCNet-kompatible Netzwerkkarte" );
    gbcExt.gridy++;
    this.tabExt.add( this.cbKCNet, gbcExt );

    this.cbPlotter = GUIFactory.createCheckBox( "Plotter XY4131 / XY4140" );
    gbcExt.gridy++;
    this.tabExt.add( this.cbPlotter, gbcExt );

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
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.cbPasteFast = GUIFactory.createCheckBox(
		"Einf\u00FCgen von Text direkt in den Tastaturpuffer" );
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
			propPrefix + Z9001.PROP_OS_PREFIX,
			"Alternatives Betriebssystem (F000h-FFFFh):" );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    if( kc87 ) {
      this.fldAltBASIC = new ROMFileSettingsFld(
			settingsFrm,
			propPrefix + Z9001.PROP_BASIC_PREFIX,
			"Alternativer BASIC-ROM (C000h-E7FFh):" );
      gbcEtc.gridy++;
      this.tabEtc.add( this.fldAltBASIC, gbcEtc );
    } else {
      this.fldAltBASIC = null;
    }

    this.fldAltFont = new ROMFileSettingsFld(
			settingsFrm,
			propPrefix + Z9001.PROP_FONT_PREFIX,
			"Alternativer Zeichensatz:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltFont, gbcEtc );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
		settingsFrm,
		propPrefix,
		Z9001.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
		true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
		settingsFrm,
		propPrefix,
		Z9001.getAutoInputCharSet(),
		Z9001.DEFAULT_SWAP_KEY_CHAR_CASE,
		Z9001.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.rbMonoGraphNone.addActionListener( this );
    this.rbMonoGraphKRT.addActionListener( this );
    this.rbColorGraphNone.addActionListener( this );
    this.rbColorGraphKRT.addActionListener( this );
    this.rbColorGraphRobotron.addActionListener( this );
    this.cbFontProgrammable.addActionListener( this );
    this.cb80Chars.addActionListener( this );
    this.cbFixedScreenSize.addActionListener( this );
    this.cbRam16k4000.addActionListener( this );
    this.cbRam16k8000.addActionListener( this );
    this.cbRam64k.addActionListener( this );
    this.cbRom16k4000.addActionListener( this );
    this.cbRom32k4000.addActionListener( this );
    this.cbRom16k8000.addActionListener( this );
    this.cbRom10kC000.addActionListener( this );
    this.cbRomBoot.addActionListener( this );
    this.cbRomMega.addActionListener( this );
    this.rbCatchPrintCalls.addActionListener( this );
    this.rbPrinterModule.addActionListener( this );
    this.rbNoPrinter.addActionListener( this );
    this.cbFloppyDisk.addActionListener( this );
    this.cbK1520Sound.addActionListener( this );
    this.cbKCNet.addActionListener( this );
    this.cbPlotter.addActionListener( this );
    this.cbRTC.addActionListener( this );
    this.cbVDIP.addActionListener( this );
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

      // Tab Grafik
      tab = this.tabGraph;
      boolean color = false;
      String  graph = Z9001.VALUE_NONE;
      if( this.rbMonoGraphKRT.isSelected() ) {
	graph = "krt";
      } else if( this.rbColorGraphNone.isSelected() ) {
	color = true;
      } else if( this.rbColorGraphKRT.isSelected() ) {
	color = true;
	graph = "krt";
      } else if( this.rbColorGraphRobotron.isSelected() ) {
	color = true;
	graph = "robotron";
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_COLOR,
		color );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_GRAPHIC_TYPE,
		graph );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_FONT_PROGRAMMABLE,
		this.cbFontProgrammable.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_80CHARS_ENABLED,
		this.cb80Chars.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_FIXED_SCREEN_SIZE,
		this.cbFixedScreenSize.isSelected() );

      // Tab Speichermodule
      tab = this.tabMem;
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_RAM16K4000_ENABLED,
		this.cbRam16k4000.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_RAM16K8000_ENABLED,
		this.cbRam16k8000.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_RAM64K_ENABLED,
		this.cbRam64k.isSelected() );

      File    file            = this.fldRomModule.getFile();
      boolean stateRom16k4000 = this.cbRom16k4000.isSelected();
      boolean stateRom32k4000 = this.cbRom32k4000.isSelected();
      boolean stateRom16k8000 = this.cbRom16k8000.isSelected();
      boolean stateRom10kC000 = this.cbRom10kC000.isSelected();
      if( (stateRom16k4000
		|| stateRom32k4000
		|| stateRom16k8000
		|| stateRom10kC000)
	  && (file == null)
	  && selected )
      {
	throw new UserInputException(
			"Datei f\u00FCr ROM-Modul nicht ausgew\u00E4hlt" );
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_ROM_MOD_PREFIX + Z9001.PROP_FILE,
		file != null ? file.getPath() : file );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_ROM16K4000_ENABLED,
		stateRom16k4000 );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_ROM32K4000_ENABLED,
		stateRom32k4000 );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_ROM16K8000_ENABLED,
		stateRom16k8000 );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_ROM10KC000_ENABLED,
		stateRom10kC000 );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_ROMBOOT_ENABLED,
		this.cbRomBoot.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_ROMMEGA_ENABLED,
		this.cbRomMega.isSelected() );

      // Tab RAM-Floppies
      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      // Tab Drucker
      tab = this.tabPrinter;
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_CATCH_PRINT_CALLS,
		this.rbCatchPrintCalls.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_PRINTER_MOD_ENABLED,
		this.rbPrinterModule.isSelected() );

      // Tab GIDE
      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      // Tab Erweiterungen
      tab = this.tabExt;
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_FDC_ENABLED,
		this.cbFloppyDisk.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_K1520SOUND_ENABLED,
		this.cbK1520Sound.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_KCNET_ENABLED,
		this.cbKCNet.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_PLOTTER_ENABLED,
		this.cbPlotter.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_RTC_ENABLED,
		this.cbRTC.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_VDIP_ENABLED,
		this.cbVDIP.isSelected() );

      // Tab Sonstiges
      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + Z9001.PROP_PASTE_FAST,
		this.cbPasteFast.isSelected() );

      this.fldAltOS.applyInput( props, selected );
      if( this.fldAltBASIC != null ) {
	this.fldAltBASIC.applyInput( props, selected );
      }
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
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src instanceof AbstractButton ) {
	AbstractButton btn = (AbstractButton) src;
	if( btn.isSelected() ) {
	  AbstractButton[] switchOffBtns = this.switchOffMap.get( btn );
	  if( switchOffBtns != null ) {
	    for( int i = 0; i < switchOffBtns.length; i++ ) {
	      switchOffBtns[ i ].setSelected( false );
	    }
	  }
	}
	updMemFieldsEnabled();
	fireDataChanged();
	rv = true;
      }
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
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    // Tab Grafik
    boolean color = EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + Z9001.PROP_COLOR,
			true );
    String text = EmuUtil.getProperty(
			props,
			this.propPrefix + Z9001.PROP_GRAPHIC_TYPE );
    if( color ) {
      if( text.equals( Z9001.VALUE_GRAPHIC_KRT ) ) {
	this.rbColorGraphKRT.setSelected( true );
      } else if( text.equals( Z9001.VALUE_GRAPHIC_ROBOTRON ) ) {
	this.rbColorGraphRobotron.setSelected( true );
      } else {
	this.rbColorGraphNone.setSelected( true );
      }
    } else {
      if( text.equals( Z9001.VALUE_GRAPHIC_KRT ) ) {
	this.rbMonoGraphKRT.setSelected( true );
      } else {
	this.rbMonoGraphNone.setSelected( true );
      }
    }
    this.cbFontProgrammable.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_FONT_PROGRAMMABLE,
		false ) );
    this.cb80Chars.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_80CHARS_ENABLED,
				false ) );
    this.cbFixedScreenSize.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_FIXED_SCREEN_SIZE,
		false ) );

    // Tab Speichermodule
    this.cbRam16k4000.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_RAM16K4000_ENABLED,
		false ) );
    this.cbRam16k8000.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_RAM16K8000_ENABLED,
		false ) );
    this.cbRam64k.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_RAM64K_ENABLED,
		false ) );
    this.cbRom16k4000.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_ROM16K4000_ENABLED,
		false ) );
    this.cbRom32k4000.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_ROM32K4000_ENABLED,
		false ) );
    this.cbRom16k8000.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_ROM16K8000_ENABLED,
		false ) );
    this.cbRom10kC000.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_ROM10KC000_ENABLED,
		false ) );
    this.cbRomBoot.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_ROMBOOT_ENABLED,
		false ) );
    this.cbRomMega.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_ROMMEGA_ENABLED,
		false ) );
    this.fldRomModule.updFields( props );
    updMemFieldsEnabled();

    // Tab RAM-Flopies
    this.tabRF.updFields( props );

    // Tab Drucker
    if( EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_PRINTER_MOD_ENABLED,
		false ) )
    {
      this.rbPrinterModule.setSelected( true );
    } else if( EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_CATCH_PRINT_CALLS,
		false ) )
    {
      this.rbCatchPrintCalls.setSelected( true );
    } else {
      this.rbNoPrinter.setSelected( true );
    }

    // Tab GIDE
    this.tabGIDE.updFields( props );

    // Tab Erweiterungen
   this.cbFloppyDisk.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_FDC_ENABLED,
		false ) );
    this.cbK1520Sound.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_K1520SOUND_ENABLED,
		false ) );
    this.cbKCNet.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_KCNET_ENABLED,
		false ) );
    this.cbPlotter.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_PLOTTER_ENABLED,
		false ) );
    this.cbRTC.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_RTC_ENABLED,
		false ) );
    this.cbVDIP.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_VDIP_ENABLED,
		false ) );

    // Tab Sonstiges
    this.cbPasteFast.setSelected(
	EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + Z9001.PROP_PASTE_FAST,
		true ) );
    this.fldAltOS.updFields( props );
    if( this.fldAltBASIC != null ) {
      this.fldAltBASIC.updFields( props );
    }
    this.fldAltFont.updFields( props );

    // Tab AutoLoad
    this.tabAutoLoad.updFields( props );

    // Tab AutoInput
    this.tabAutoInput.updFields( props );
  }


	/* --- private Methoden --- */

  private static AbstractButton[] toArray( AbstractButton... btns )
  {
    return btns;
  }


  private void updMemFieldsEnabled()
  {
    String  label = DEFAULT_LABEL_ROM_MODULE;
    boolean state = false;
    if( this.cbRom16k4000.isSelected()
	|| this.cbRom32k4000.isSelected()
	|| this.cbRom16k8000.isSelected()
	|| this.cbRom10kC000.isSelected() )
    {
      state = true;
    } else if( this.cbRomBoot.isSelected() ) {
      label = "Alternativer Inhalt des Boot-ROM-Moduls:";
      state = true;
    } else if( this.cbRomMega.isSelected() ) {
      label = "Alternativer Inhalt des Mega-ROM-Moduls:";
      state = true;
    }
    this.fldRomModule.setLabelText( label );
    this.fldRomModule.setEnabled( state );
    this.cbFixedScreenSize.setEnabled( this.cb80Chars.isSelected() );
  }
}
