/*
 * (c) 2010-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen der Computer Z9001, KC85/1 und KC87
 */

package jkcemu.emusys.z9001;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.Z9001;


public class Z9001SettingsFld extends AbstractSettingsFld
{
  private static final int DEFAULT_AUTO_ACTION_WAIT_MILLIS = 2000;

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
  private JRadioButton                         btnMonoGraphNone;
  private JRadioButton                         btnMonoGraphKRT;
  private JRadioButton                         btnColorGraphNone;
  private JRadioButton                         btnColorGraphKRT;
  private JRadioButton                         btnColorGraphRobotron;
  private JCheckBox                            btnFontProgrammable;
  private JCheckBox                            btn80Chars;
  private JCheckBox                            btnFixedScreenSize;
  private JCheckBox                            btnRam16k4000;
  private JCheckBox                            btnRam16k8000;
  private JCheckBox                            btnRam64k;
  private JCheckBox                            btnRom16k4000;
  private JCheckBox                            btnRom32k4000;
  private JCheckBox                            btnRom16k8000;
  private JCheckBox                            btnRom10kC000;
  private JCheckBox                            btnRomMega;
  private JCheckBox                            btnRomBoot;
  private ROMFileSettingsFld                   fldRomModule;
  private JRadioButton                         btnCatchPrintCalls;
  private JRadioButton                         btnPrinterModule;
  private JRadioButton                         btnNoPrinter;
  private JCheckBox                            btnFloppyDisk;
  private JCheckBox                            btnPlotter;
  private JCheckBox                            btnKCNet;
  private JCheckBox                            btnVDIP;
  private JCheckBox                            btnRTC;
  private JCheckBox                            btnPasteFast;
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
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Grafik
    this.tabGraph = new JPanel( new GridBagLayout() );
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

    this.btnMonoGraphNone = new JRadioButton(
					"S/W, Blockgrafik",
					false );
    grpGraph.add( this.btnMonoGraphNone );
    this.tabGraph.add( this.btnMonoGraphNone, gbcGraph );

    this.btnMonoGraphKRT = new JRadioButton(
					"S/W, KRT-Vollgrafikerweiterung",
					false );
    grpGraph.add( this.btnMonoGraphKRT );
    gbcGraph.insets.top = 0;
    gbcGraph.gridy++;
    this.tabGraph.add( this.btnMonoGraphKRT, gbcGraph );

    this.btnColorGraphNone = new JRadioButton(
					"Farbe, Blockgrafik",
					true );
    grpGraph.add( this.btnColorGraphNone );
    gbcGraph.gridy++;
    this.tabGraph.add( this.btnColorGraphNone, gbcGraph );

    this.btnColorGraphKRT = new JRadioButton(
				"Farbe, KRT-Vollgrafikerweiterung",
				false );
    grpGraph.add( this.btnColorGraphKRT );
    gbcGraph.gridy++;
    this.tabGraph.add( this.btnColorGraphKRT, gbcGraph );

    this.btnColorGraphRobotron = new JRadioButton(
				"Farbe, Robotron-Vollgrafikerweiterung",
				false );
    grpGraph.add( this.btnColorGraphRobotron );
    gbcGraph.gridy++;
    this.tabGraph.add( this.btnColorGraphRobotron, gbcGraph );

    this.btnFontProgrammable = new JCheckBox(
				"Programmierbarer Zeichengenerator",
				false );
    gbcGraph.insets.top    = 10;
    gbcGraph.insets.bottom = 0;
    gbcGraph.gridy++;
    this.tabGraph.add( this.btnFontProgrammable, gbcGraph );

    this.btn80Chars = new JCheckBox(
				"40/80-Zeichen-Umschaltung",
				false );
    gbcGraph.insets.top = 0;
    gbcGraph.gridy++;
    this.tabGraph.add( this.btn80Chars, gbcGraph );

    this.btnFixedScreenSize = new JCheckBox(
		"Gleiche Fenstergr\u00F6\u00DFe in beiden Bildschirmmodi" );
    gbcGraph.insets.left   = 50;
    gbcGraph.insets.bottom = 5;
    gbcGraph.gridy++;
    this.tabGraph.add( this.btnFixedScreenSize, gbcGraph );


    // Tab Speichermodule
    this.tabMem = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Speichermodule", this.tabMem );

    GridBagConstraints gbcMem = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnRam16k4000 = new JCheckBox(
				"16K RAM-Modul (4000h-7FFFh)",
				false );
    this.tabMem.add( this.btnRam16k4000, gbcMem );

    this.btnRam16k8000 = new JCheckBox(
				"16K RAM-Modul (8000h-BFFFh)",
				false );
    gbcMem.gridx++;
    this.tabMem.add( this.btnRam16k8000, gbcMem );

    this.btnRam64k = new JCheckBox(
			"64K RAM-Modul (2 x 4000h-7FFFh, 1 x 8000h-E7FFh)",
			false );
    gbcMem.insets.top = 0;
    gbcMem.gridwidth  = 2;
    gbcMem.gridx      = 0;
    gbcMem.gridy++;
    this.tabMem.add( this.btnRam64k, gbcMem );

    this.btnRom16k4000 = new JCheckBox(
				"16K ROM-Modul (4000h-7FFFh)",
				false );
    gbcMem.insets.top = 10;
    gbcMem.gridwidth  = 1;
    gbcMem.gridy++;
    this.tabMem.add( this.btnRom16k4000, gbcMem );

    this.btnRom16k8000 = new JCheckBox(
				"16K ROM-Modul (8000h-BFFFh)",
				false );
    gbcMem.gridx++;
    this.tabMem.add( this.btnRom16k8000, gbcMem );

    this.btnRom32k4000 = new JCheckBox(
				"32K ROM-Modul (4000h-BFFFh)",
				false );
    gbcMem.insets.top = 0;
    gbcMem.gridwidth  = 1;
    gbcMem.gridx      = 0;
    gbcMem.gridy++;
    this.tabMem.add( this.btnRom32k4000, gbcMem );

    this.btnRom10kC000 = new JCheckBox(
				"10K ROM-Modul (C000h-E7FFh)",
				false );
    gbcMem.gridx++;
    this.tabMem.add( this.btnRom10kC000, gbcMem );

    this.btnRomMega = new JCheckBox(
				"Mega-ROM-Modul (256 x C000h-E7FFh)",
				false );
    gbcMem.gridwidth = 2;
    gbcMem.gridx     = 0;
    gbcMem.gridy++;
    this.tabMem.add( this.btnRomMega, gbcMem );

    this.btnRomBoot = new JCheckBox(
		"Boot-ROM-Modul"
			+ " (C000h-E7FFh, nur mit Floppy-Disk-Modul sinnvoll)",
		false );
    gbcMem.gridy++;
    this.tabMem.add( this.btnRomBoot, gbcMem );

    this.fldRomModule = new ROMFileSettingsFld(
				settingsFrm,
				this.propPrefix + "rom_module.",
				DEFAULT_LABEL_ROM_MODULE );
    gbcMem.fill       = GridBagConstraints.HORIZONTAL;
    gbcMem.weightx    = 1.0;
    gbcMem.insets.top = 10;
    gbcMem.gridwidth  = GridBagConstraints.REMAINDER;
    gbcMem.gridy++;
    this.tabMem.add( this.fldRomModule, gbcMem );

    this.switchOffMap.put(
		this.btnRam16k4000,
		toArray(
			this.btnRam64k,
			this.btnRom16k4000,
			this.btnRom32k4000 ) );
    this.switchOffMap.put(
		this.btnRam16k8000,
		toArray(
			this.btnRam64k,
			this.btnRom32k4000,
			this.btnRom16k8000 ) );
    this.switchOffMap.put(
		this.btnRam64k,
		toArray(
			this.btnRam16k4000,
			this.btnRam16k8000,
			this.btnRom16k4000,
			this.btnRom32k4000,
			this.btnRom16k8000,
			this.btnRom10kC000 ) );
    this.switchOffMap.put(
		this.btnRom16k4000,
		toArray(
			this.btnRam16k4000,
			this.btnRam64k,
			this.btnRom32k4000,
			this.btnRom16k8000,
			this.btnRom10kC000,
			this.btnRomBoot,
			this.btnRomMega ) );
    this.switchOffMap.put(
		this.btnRom32k4000,
		toArray(
			this.btnRam16k4000,
			this.btnRam64k,
			this.btnRom16k4000,
			this.btnRom16k8000,
			this.btnRom10kC000,
			this.btnRomBoot,
			this.btnRomMega ) );
    this.switchOffMap.put(
		this.btnRom16k8000,
		toArray(
			this.btnRam16k8000,
			this.btnRam64k,
			this.btnRom16k4000,
			this.btnRom32k4000,
			this.btnRom10kC000,
			this.btnRomBoot,
			this.btnRomMega ) );
    this.switchOffMap.put(
		this.btnRom10kC000,
		toArray(
			this.btnRam64k,
			this.btnRom16k4000,
			this.btnRom32k4000,
			this.btnRom16k8000,
			this.btnRomBoot,
			this.btnRomMega ) );
    this.switchOffMap.put(
		this.btnRomBoot,
		toArray(
			this.btnRom16k4000,
			this.btnRom32k4000,
			this.btnRom16k8000,
			this.btnRom10kC000,
			this.btnRomMega ) );
    this.switchOffMap.put(
		this.btnRomMega,
		toArray(
			this.btnRom16k4000,
			this.btnRom32k4000,
			this.btnRom16k8000,
			this.btnRom10kC000,
			this.btnRomBoot ) );
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
    this.tabPrinter = new JPanel( new GridBagLayout() );
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

    this.btnCatchPrintCalls = new JRadioButton(
			"BOS-Aufrufe f\u00FCr Druckerausgaben abfangen",
			false );
    grpPrinter.add( this.btnCatchPrintCalls );
    this.tabPrinter.add( this.btnCatchPrintCalls, gbcPrinter );

    this.btnPrinterModule = new JRadioButton(
			"V.24-Druckermodul emulieren",
			false );
    grpPrinter.add( this.btnPrinterModule );
    gbcPrinter.insets.top = 0;
    gbcPrinter.gridy++;
    this.tabPrinter.add( this.btnPrinterModule, gbcPrinter );

    this.btnNoPrinter = new JRadioButton(
			"Keinen Drucker emulieren",
			true );
    grpPrinter.add( this.btnNoPrinter );
    gbcPrinter.insets.bottom = 5;
    gbcPrinter.gridy++;
    this.tabPrinter.add( this.btnNoPrinter, gbcPrinter );


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
    this.tabExt.add( this.btnFloppyDisk, gbcExt );

    this.btnPlotter = new JCheckBox( "Plotter XY4131 / XY4140", false );
    gbcExt.insets.top = 0;
    gbcExt.gridy++;
    this.tabExt.add( this.btnPlotter, gbcExt );

    this.btnKCNet = new JCheckBox( "KCNet-kompatible Netzwerkkarte", false );
    gbcExt.gridy++;
    this.tabExt.add( this.btnKCNet, gbcExt );

    this.btnVDIP = new JCheckBox(
		"USB-Anschluss (Vinculum VDIP Modul)",
		false );
    gbcExt.gridy++;
    this.tabExt.add( this.btnVDIP, gbcExt );

    this.btnRTC = new JCheckBox( "Echtzeituhr", false );
    this.btnRTC.addActionListener( this );
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.btnRTC, gbcExt );


    // Tab Sonstiges
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
		"Einf\u00FCgen von Text direkt in den Tastaturpuffer",
		true );
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
		"Alternatives Betriebssystem (F000h-FFFFh):" );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    if( kc87 ) {
      this.fldAltBASIC = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "basic.",
		"Alternativer BASIC-ROM (C000h-E7FFh):" );
      gbcEtc.gridy++;
      this.tabEtc.add( this.fldAltBASIC, gbcEtc );
    } else {
      this.fldAltBASIC = null;
    }

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
					Z9001.getDefaultSwapKeyCharCase(),
					DEFAULT_AUTO_ACTION_WAIT_MILLIS );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.btnMonoGraphNone.addActionListener( this );
    this.btnMonoGraphKRT.addActionListener( this );
    this.btnColorGraphNone.addActionListener( this );
    this.btnColorGraphKRT.addActionListener( this );
    this.btnColorGraphRobotron.addActionListener( this );
    this.btnFontProgrammable.addActionListener( this );
    this.btn80Chars.addActionListener( this );
    this.btnFixedScreenSize.addActionListener( this );
    this.btnRam16k4000.addActionListener( this );
    this.btnRam16k8000.addActionListener( this );
    this.btnRam64k.addActionListener( this );
    this.btnRom16k4000.addActionListener( this );
    this.btnRom32k4000.addActionListener( this );
    this.btnRom16k8000.addActionListener( this );
    this.btnRom10kC000.addActionListener( this );
    this.btnRomBoot.addActionListener( this );
    this.btnRomMega.addActionListener( this );
    this.btnCatchPrintCalls.addActionListener( this );
    this.btnPrinterModule.addActionListener( this );
    this.btnNoPrinter.addActionListener( this );
    this.btnFloppyDisk.addActionListener( this );
    this.btnPlotter.addActionListener( this );
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

      // Tab Grafik
      tab = this.tabGraph;
      boolean color = false;
      String  graph = "none";
      if( this.btnMonoGraphKRT.isSelected() ) {
	graph = "krt";
      } else if( this.btnColorGraphNone.isSelected() ) {
	color = true;
      } else if( this.btnColorGraphKRT.isSelected() ) {
	color = true;
	graph = "krt";
      } else if( this.btnColorGraphRobotron.isSelected() ) {
	color = true;
	graph = "robotron";
      }
      EmuUtil.setProperty( props, this.propPrefix + "color", color );
      EmuUtil.setProperty( props, this.propPrefix + "graphic.type", graph );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "font.programmable",
		this.btnFontProgrammable.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "80_chars.enabled",
		this.btn80Chars.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "fixed_screen_size",
		this.btnFixedScreenSize.isSelected() );

      // Tab Speichermodule
      tab = this.tabMem;
      EmuUtil.setProperty(
		props,
		this.propPrefix + "ram_16k_4000.enabled",
		this.btnRam16k4000.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "ram_16k_8000.enabled",
		this.btnRam16k8000.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "ram_64k.enabled",
		this.btnRam64k.isSelected() );

      File    file            = this.fldRomModule.getFile();
      boolean stateRom16k4000 = this.btnRom16k4000.isSelected();
      boolean stateRom32k4000 = this.btnRom32k4000.isSelected();
      boolean stateRom16k8000 = this.btnRom16k8000.isSelected();
      boolean stateRom10kC000 = this.btnRom10kC000.isSelected();
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
			this.propPrefix + "rom_module.file",
			file != null ? file.getPath() : file );
      EmuUtil.setProperty(
			props,
			this.propPrefix + "rom_16k_4000.enabled",
			stateRom16k4000 );
      EmuUtil.setProperty(
			props,
			this.propPrefix + "rom_32k_4000.enabled",
			stateRom32k4000 );
      EmuUtil.setProperty(
			props,
			this.propPrefix + "rom_16k_8000.enabled",
			stateRom16k8000 );
      EmuUtil.setProperty(
			props,
			this.propPrefix + "rom_10k_c000.enabled",
			stateRom10kC000 );
      EmuUtil.setProperty(
			props,
			this.propPrefix + "rom_boot.enabled",
			this.btnRomBoot.isSelected() );
      EmuUtil.setProperty(
			props,
			this.propPrefix + "rom_mega.enabled",
			this.btnRomMega.isSelected() );

      // Tab RAM-Floppies
      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      // Tab Drucker
      tab = this.tabPrinter;
      EmuUtil.setProperty(
		props,
		this.propPrefix + "catch_print_calls",
		this.btnCatchPrintCalls.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "printer_module.enabled",
		this.btnPrinterModule.isSelected() );

      // Tab GIDE
      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      // Tab Erweiterungen
      tab = this.tabExt;
      EmuUtil.setProperty(
		props,
		this.propPrefix + "floppydisk.enabled",
		this.btnFloppyDisk.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "plotter.enabled",
		this.btnPlotter.isSelected() );
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
		this.propPrefix + "rtc.enabled",
		this.btnRTC.isSelected() );

      // Tab Sonstiges
      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + "paste.fast",
		this.btnPasteFast.isSelected() );

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
  public void lookAndFeelChanged()
  {
    this.fldRomModule.lookAndFeelChanged();
    this.fldAltOS.lookAndFeelChanged();
    if( this.fldAltBASIC != null ) {
      this.fldAltBASIC.lookAndFeelChanged();
    }
    this.fldAltFont.lookAndFeelChanged();
    this.tabGIDE.lookAndFeelChanged();
    this.tabAutoLoad.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    // Tab Grafik
    boolean color = EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "color",
				true );
    String text = EmuUtil.getProperty(
				props,
				this.propPrefix + "graphic.type" );
    if( color ) {
      if( text.equals( "krt" ) ) {
	this.btnColorGraphKRT.setSelected( true );
      } else if( text.equals( "robotron" ) ) {
	this.btnColorGraphRobotron.setSelected( true );
      } else {
	this.btnColorGraphNone.setSelected( true );
      }
    } else {
      if( text.equals( "krt" ) ) {
	this.btnMonoGraphKRT.setSelected( true );
      } else {
	this.btnMonoGraphNone.setSelected( true );
      }
    }
    this.btnFontProgrammable.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "font.programmable",
				false ) );
    this.btn80Chars.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "80_chars.enabled",
				false ) );
    this.btnFixedScreenSize.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "fixed_screen_size",
				false ) );

    // Tab Speichermodule
    this.btnRam16k4000.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "ram_16k_4000.enabled",
				false ) );
    this.btnRam16k8000.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "ram_16k_8000.enabled",
				false ) );
    this.btnRam64k.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "ram_64k.enabled",
				false ) );
    this.btnRom16k4000.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_16k_4000.enabled",
				false ) );
    this.btnRom32k4000.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_32k_4000.enabled",
				false ) );
    this.btnRom16k8000.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_16k_8000.enabled",
				false ) );
    this.btnRom10kC000.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_10k_c000.enabled",
				false ) );
    this.btnRomBoot.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_boot.enabled",
				false ) );
    this.btnRomMega.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "rom_mega.enabled",
				false ) );
    this.fldRomModule.updFields( props );
    updMemFieldsEnabled();

    // Tab RAM-Flopies
    this.tabRF.updFields( props );

    // Tab Drucker
    if( EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "printer_module.enabled",
				false ) )
    {
      this.btnPrinterModule.setSelected( true );
    } else if( EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "catch_print_calls",
				false ) )
    {
      this.btnCatchPrintCalls.setSelected( true );
    } else {
      this.btnNoPrinter.setSelected( true );
    }

    // Tab GIDE
    this.tabGIDE.updFields( props );

    // Tab Erweiterungen
    this.btnFloppyDisk.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "floppydisk.enabled",
				false ) );
    this.btnPlotter.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "plotter.enabled",
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
    this.btnRTC.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "rtc.enabled", false ) );

    // Tab Sonstiges
    this.btnPasteFast.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "paste.fast",
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
    if( this.btnRom16k4000.isSelected()
	|| this.btnRom32k4000.isSelected()
	|| this.btnRom16k8000.isSelected()
	|| this.btnRom10kC000.isSelected() )
    {
      state = true;
    } else if( this.btnRomBoot.isSelected() ) {
      label = "Alternativer Inhalt des Boot-ROM-Moduls:";
      state = true;
    } else if( this.btnRomMega.isSelected() ) {
      label = "Alternativer Inhalt des Mega-ROM-Moduls:";
      state = true;
    }
    this.fldRomModule.setLabelText( label );
    this.fldRomModule.setEnabled( state );
    this.btnFixedScreenSize.setEnabled( this.btn80Chars.isSelected() );
  }
}
