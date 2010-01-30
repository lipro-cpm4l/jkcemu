/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer die Einstellungen
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.Document;
import jkcemu.Main;


public class SettingsFrm extends BasicFrm
			implements
				ChangeListener,
				DocumentListener,
				ListSelectionListener
{
  public static final int DEFAULT_BRIGHTNESS = 80;

  private static final int MAX_MARGIN = 199;

  private static final String kc85ModAddPrefix  = "kc85.module.add.";
  private static final String kc85ModPropPrefix = "jkcemu.kc85.module.";

  private static final String[][] kc85Modules = {
			{ "M003", "V.24 mit Drucker" },
			{ "M006", "BASIC (nur bis KC85/3 verwendbar)" },
			{ "M011", "64 KByte RAM" },
			{ "M022", "16 KByte Expander RAM" },
			{ "M032", "256 KByte Segmented RAM" },
			{ "M034", "512 KByte Segmented RAM" },
			{ "M035", "1 MByte Segmented RAM" },
			{ "M035x4", "4 MByte Segmented RAM" },
			{ "M036", "128 KByte Segmented RAM" } };

  private ScreenFrm                   screenFrm;
  private EmuThread                   emuThread;
  private File                        profileFile;
  private ExtFile                     extFont;
  private Vector<ExtROM>              extROMs;
  private Map<String,AbstractButton>  lafClass2Button;
  private SpinnerNumberModel          spinnerModelMargin;
  private JPanel                      tabFont;
  private JPanel                      tabLAF;
  private JPanel                      tabROM;
  private JPanel                      tabRF;
  private JPanel                      tabScreen;
  private JPanel                      tabSpeed;
  private JPanel                      tabSys;
  private JPanel                      panelSysOpt;
  private JCheckBox                   btnConfirmNMI;
  private JCheckBox                   btnConfirmReset;
  private JCheckBox                   btnConfirmPowerOn;
  private JCheckBox                   btnConfirmQuit;
  private JComboBox                   comboScreenRefresh;
  private JList                       listROM;
  private CardLayout                  cardLayoutSysOpt;
  private String                      curSysOptCard;
  private JRadioButton                btnSysAC1;
  private JRadioButton                btnSysBCS3;
  private JRadioButton                btnSysC80;
  private JRadioButton                btnSysHC900;
  private JRadioButton                btnSysHEMC;
  private JRadioButton                btnSysHGMC;
  private JRadioButton                btnSysKC85_1;
  private JRadioButton                btnSysKC85_2;
  private JRadioButton                btnSysKC85_3;
  private JRadioButton                btnSysKC85_4;
  private JRadioButton                btnSysKC85_5;
  private JRadioButton                btnSysKC87;
  private JRadioButton                btnSysKramerMC;
  private JRadioButton                btnSysLC80;
  private JRadioButton                btnSysLLC1;
  private JRadioButton                btnSysLLC2;
  private JRadioButton                btnSysPCM;
  private JRadioButton                btnSysPoly880;
  private JRadioButton                btnSysSC2;
  private JRadioButton                btnSysSLC1;
  private JRadioButton                btnSysVCS80;
  private JRadioButton                btnSysZ1013;
  private JRadioButton                btnSysZ9001;
  private JRadioButton                btnAC1Mon31_64x16;
  private JRadioButton                btnAC1Mon31_64x32;
  private JRadioButton                btnAC1MonSCCH80;
  private JRadioButton                btnAC1MonSCCH1088;
  private JLabel                      labelAC1RomdiskBegAddr;                 
  private JRadioButton                btnAC1Romdisk8000;                      
  private JRadioButton                btnAC1RomdiskC000;
  private JRadioButton                btnBCS3se24_27;
  private JRadioButton                btnBCS3se31_29;
  private JRadioButton                btnBCS3se31_40;
  private JRadioButton                btnBCS3sp33_29;
  private JRadioButton                btnBCS3Ram1k;
  private JRadioButton                btnBCS3Ram17k;
  private JCheckBox                   btnHGMCBasic;
  private JCheckBox                   btnHGMCCatchPrintCalls;
  private JCheckBox                   btnHEMCCatchPrintCalls;
  private JCheckBox                   btnKramerMCCatchPrintCalls;
  private ListSelectionModel          selModelKC85Module;
  private ModuleTableModel            tableModelKC85Module;
  private JTable                      tableKC85Module;
  private JPopupMenu                  popupKC85Module;
  private JButton                     btnKC85ModAdd;
  private JButton                     btnKC85ModRemove;
  private JCheckBox                   btnKC85D004Enabled;
  private JComboBox                   comboKC85D004Rom;
  private JLabel                      labelKC85D004Rom;
  private JLabel                      labelKC85D004Speed;
  private JRadioButton                btnKC85D004Speed4MHz;
  private JRadioButton                btnKC85D004Speed8MHz;
  private JRadioButton                btnKC85D004Speed16MHz;
  private JTextField                  fldKC85D004RomFile;
  private JButton                     btnKC85D004RomFileSelect;
  private JButton                     btnKC85D004RomFileRemove;
  private JCheckBox                   btnKC85PasteFast;
  private JCheckBox                   btnKC85VideoTiming;
  private JRadioButton                btnLC80_U505;
  private JRadioButton                btnLC80_2716;
  private JRadioButton                btnLC80_2;
  private JRadioButton                btnLC80e;
  private JCheckBox                   btnPCMAutoLoadBDOS;
  private JRadioButton                btnZ9001MonoGraphNone;
  private JRadioButton                btnZ9001MonoGraphKRT;
  private JRadioButton                btnZ9001ColorGraphNone;
  private JRadioButton                btnZ9001ColorGraphKRT;
  private JRadioButton                btnZ9001ColorGraphRobotron;
  private JRadioButton                btnZ9001Ram16k;
  private JRadioButton                btnZ9001Ram32k;
  private JRadioButton                btnZ9001Ram48k;
  private JRadioButton                btnZ9001Ram74k;
  private JRadioButton                btnZ9001CatchPrintCalls;
  private JRadioButton                btnZ9001PrinterModule;
  private JRadioButton                btnZ9001NoPrinter;
  private JCheckBox                   btnZ9001FloppyDisk;
  private JCheckBox                   btnZ9001PasteFast;
  private JRadioButton                btnZ1013_01;
  private JRadioButton                btnZ1013_12;
  private JRadioButton                btnZ1013_16;
  private JRadioButton                btnZ1013_64;
  private JRadioButton                btnZ1013Mon202;
  private JRadioButton                btnZ1013MonA2;
  private JRadioButton                btnZ1013MonRB_K7659;
  private JRadioButton                btnZ1013MonRB_S6009;
  private JRadioButton                btnZ1013MonJM_1992;
  private JCheckBox                   btnZ1013Graphic;
  private JCheckBox                   btnZ1013CatchPrintCalls;
  private JCheckBox                   btnZ1013PasteFast;
  private JRadioButton                btnSpeedDefault;
  private JRadioButton                btnSpeedValue;
  private JRadioButton                btnSpeedUnlimited;
  private JRadioButton                btnSRAMInit00;
  private JRadioButton                btnSRAMInitRandom;
  private JRadioButton                btnFileDlgEmu;
  private JRadioButton                btnFileDlgNative;
  private JCheckBox                   btnDirectCopyPaste;
  private JLabel                      labelSpeedUnit;
  private JTextField                  fldSpeed;
  private Document                    docSpeed;
  private NumberFormat                fmtSpeed;
  private JButton                     btnExtFontSelect;
  private JButton                     btnExtFontRemove;
  private JTextField                  fldExtFontFile;
  private JButton                     btnRF1Select;
  private JButton                     btnRF1Remove;
  private JTextField                  fldRF1File;
  private JButton                     btnRF2Select;
  private JButton                     btnRF2Remove;
  private JTextField                  fldRF2File;
  private JTextField                  fldProfileDir;
  private JSlider                     sliderBrightness;
  private JSpinner                    spinnerMargin;
  private ButtonGroup                 grpLAF;
  private UIManager.LookAndFeelInfo[] lafs;
  private JButton                     btnApply;
  private JButton                     btnLoad;
  private JButton                     btnSave;
  private JButton                     btnHelp;
  private JButton                     btnClose;
  private JButton                     btnROMAdd;
  private JButton                     btnROMRemove;
  private JTextField                  fldROMPrgXFile;
  private JButton                     btnROMPrgXSelect;
  private JButton                     btnROMPrgXRemove;
  private JTextField                  fldROMDiskFile;
  private JButton                     btnROMDiskSelect;
  private JButton                     btnROMDiskRemove;
  private JTabbedPane                 tabbedPane;


  public SettingsFrm( ScreenFrm screenFrm )
  {
    setTitle( "JKCEMU Einstellungen" );
    Main.updIcon( this );
    this.screenFrm       = screenFrm;
    this.emuThread       = screenFrm.getEmuThread();
    this.extFont         = null;
    this.extROMs         = new Vector<ExtROM>();
    this.lafClass2Button = new Hashtable<String,AbstractButton>();
    this.profileFile     = Main.getProfileFile();
    this.fmtSpeed        = NumberFormat.getNumberInstance();
    if( this.fmtSpeed instanceof DecimalFormat ) {
      ((DecimalFormat) this.fmtSpeed).applyPattern( "#0.0##" );
    }


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.WEST,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, gbc );


    // Bereich System
    this.tabSys = new JPanel( new BorderLayout() );
    this.tabbedPane.addTab( "System", tabSys );

    JPanel panelSys = new JPanel( new GridBagLayout() );
    tabSys.add( new JScrollPane( panelSys ), BorderLayout.WEST );

    GridBagConstraints gbcSys = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpSys = new ButtonGroup();

    this.btnSysAC1 = new JRadioButton( "AC1", true );
    this.btnSysAC1.addActionListener( this );
    grpSys.add( this.btnSysAC1 );
    panelSys.add( this.btnSysAC1, gbcSys );

    this.btnSysBCS3 = new JRadioButton( "BCS3", false );
    this.btnSysBCS3.addActionListener( this );
    grpSys.add( this.btnSysBCS3 );
    gbcSys.insets.top = 0;
    gbcSys.gridy++;
    panelSys.add( this.btnSysBCS3, gbcSys );

    this.btnSysC80 = new JRadioButton( "C-80", false );
    this.btnSysC80.addActionListener( this );
    grpSys.add( this.btnSysC80 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysC80, gbcSys );

    this.btnSysHC900 = new JRadioButton( "HC900", false );
    this.btnSysHC900.addActionListener( this );
    grpSys.add( this.btnSysHC900 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysHC900, gbcSys );

    this.btnSysHEMC = new JRadioButton( "H\u00FCbler/Evert-MC", false );
    this.btnSysHEMC.addActionListener( this );
    grpSys.add( this.btnSysHEMC );
    gbcSys.gridy++;
    panelSys.add( this.btnSysHEMC, gbcSys );

    this.btnSysHGMC = new JRadioButton( "H\u00FCbler-Grafik-MC", false );
    this.btnSysHGMC.addActionListener( this );
    grpSys.add( this.btnSysHGMC );
    gbcSys.gridy++;
    panelSys.add( this.btnSysHGMC, gbcSys );

    this.btnSysKC85_1 = new JRadioButton( "KC85/1", false );
    this.btnSysKC85_1.addActionListener( this );
    grpSys.add( this.btnSysKC85_1 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC85_1, gbcSys );

    this.btnSysKC85_2 = new JRadioButton( "KC85/2", false );
    this.btnSysKC85_2.addActionListener( this );
    grpSys.add( this.btnSysKC85_2 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC85_2, gbcSys );

    this.btnSysKC85_3 = new JRadioButton( "KC85/3", false );
    this.btnSysKC85_3.addActionListener( this );
    grpSys.add( this.btnSysKC85_3 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC85_3, gbcSys );

    this.btnSysKC85_4 = new JRadioButton( "KC85/4", false );
    this.btnSysKC85_4.addActionListener( this );
    grpSys.add( this.btnSysKC85_4 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC85_4, gbcSys );

    this.btnSysKC85_5 = new JRadioButton( "KC85/5", false );
    this.btnSysKC85_5.addActionListener( this );
    grpSys.add( this.btnSysKC85_5 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC85_5, gbcSys );

    this.btnSysKC87 = new JRadioButton( "KC87", false );
    this.btnSysKC87.addActionListener( this );
    grpSys.add( this.btnSysKC87 );
    gbcSys.insets.bottom = 5;
    gbcSys.gridy++;
    panelSys.add( this.btnSysKC87, gbcSys );

    this.btnSysKramerMC = new JRadioButton( "Kramer-MC", false );
    this.btnSysKramerMC.addActionListener( this );
    grpSys.add( this.btnSysKramerMC );
    gbcSys.insets.top    = 5;
    gbcSys.insets.bottom = 0;
    gbcSys.gridy         = 0;
    gbcSys.gridx++;
    panelSys.add( this.btnSysKramerMC, gbcSys );

    this.btnSysLC80 = new JRadioButton( "LC-80", false );
    this.btnSysLC80.addActionListener( this );
    grpSys.add( this.btnSysLC80 );
    gbcSys.insets.top = 0;
    gbcSys.gridy++;
    panelSys.add( this.btnSysLC80, gbcSys );

    this.btnSysLLC1 = new JRadioButton( "LLC1", false );
    this.btnSysLLC1.addActionListener( this );
    grpSys.add( this.btnSysLLC1 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysLLC1, gbcSys );

    this.btnSysLLC2 = new JRadioButton( "LLC2", false );
    this.btnSysLLC2.addActionListener( this );
    grpSys.add( this.btnSysLLC2 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysLLC2, gbcSys );

    this.btnSysPCM = new JRadioButton( "PC/M", false );
    this.btnSysPCM.addActionListener( this );
    grpSys.add( this.btnSysPCM );
    gbcSys.gridy++;
    panelSys.add( this.btnSysPCM, gbcSys );

    this.btnSysPoly880 = new JRadioButton( "Poly-880", false );
    this.btnSysPoly880.addActionListener( this );
    grpSys.add( this.btnSysPoly880 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysPoly880, gbcSys );

    this.btnSysSC2 = new JRadioButton( "SC2", false );
    this.btnSysSC2.addActionListener( this );
    grpSys.add( this.btnSysSC2 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysSC2, gbcSys );

    this.btnSysSLC1 = new JRadioButton( "SLC1", false );
    this.btnSysSLC1.addActionListener( this );
    grpSys.add( this.btnSysSLC1 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysSLC1, gbcSys );

    this.btnSysVCS80 = new JRadioButton( "VCS80", false );
    this.btnSysVCS80.addActionListener( this );
    grpSys.add( this.btnSysVCS80 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysVCS80, gbcSys );

    this.btnSysZ1013 = new JRadioButton( "Z1013", false );
    this.btnSysZ1013.addActionListener( this );
    grpSys.add( this.btnSysZ1013 );
    gbcSys.gridy++;
    panelSys.add( this.btnSysZ1013, gbcSys );

    this.btnSysZ9001 = new JRadioButton( "Z9001", false );
    this.btnSysZ9001.addActionListener( this );
    grpSys.add( this.btnSysZ9001 );
    gbcSys.insets.bottom = 5;
    gbcSys.gridy++;
    panelSys.add( this.btnSysZ9001, gbcSys );


    // Optionen
    this.cardLayoutSysOpt = new CardLayout( 5, 5);
    this.curSysOptCard    = null;

    this.panelSysOpt = new JPanel( this.cardLayoutSysOpt );
    this.panelSysOpt.setBorder(
		BorderFactory.createTitledBorder( "Optionen" ) );
    gbcSys.anchor     = GridBagConstraints.CENTER;
    gbcSys.fill       = GridBagConstraints.BOTH;
    gbcSys.weightx    = 1.0;
    gbcSys.weighty    = 1.0;
    gbcSys.gridheight = GridBagConstraints.REMAINDER;
    gbcSys.gridy      = 0;
    gbcSys.gridx++;
    tabSys.add( this.panelSysOpt, BorderLayout.CENTER );


    JPanel panelNoOpt = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelNoOpt, "noopt" );

    GridBagConstraints gbcNoOpt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelNoOpt.add(
		new JLabel( "Keine Optionen verf\u00FCgbar" ),
		gbcNoOpt );


    // Optionen fuer AC1
    JPanel panelAC1 = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelAC1, "AC1" );

    GridBagConstraints gbcAC1 = new GridBagConstraints(
					0, 0,
					3, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpAC1mon = new ButtonGroup();

    panelAC1.add(
	new JLabel( "Ur-AC1 (64x16 Zeichen, 1 KByte SRAM):" ),
	gbcAC1 );

    this.btnAC1Mon31_64x16 = new JRadioButton(
		"Monitorprogramm 3.1, Zeichensatz U402 BM513",
		false );
    this.btnAC1Mon31_64x16.addActionListener( this );
    grpAC1mon.add( this.btnAC1Mon31_64x16 );
    gbcAC1.insets.top  = 0;
    gbcAC1.insets.left = 50;
    gbcAC1.gridy++;
    panelAC1.add( this.btnAC1Mon31_64x16, gbcAC1 );

    gbcAC1.insets.top  = 10;
    gbcAC1.insets.left = 5;
    gbcAC1.gridy++;
    panelAC1.add(
	new JLabel( "Standard-AC1 (64x32 Zeichen, 64 KByte DRAM,"
			+ " RAM-Floppy, Floppy Disk):" ),
	gbcAC1 );

    this.btnAC1Mon31_64x32 = new JRadioButton(
		"Monitorprogramm 3.1, Zeichensatz CC-Dessau",
		true );
    this.btnAC1Mon31_64x32.addActionListener( this );
    grpAC1mon.add( this.btnAC1Mon31_64x32 );
    gbcAC1.insets.top  = 0;
    gbcAC1.insets.left = 50;
    gbcAC1.gridy++;
    panelAC1.add( this.btnAC1Mon31_64x32, gbcAC1 );

    gbcAC1.insets.top  = 10;
    gbcAC1.insets.left = 5;
    gbcAC1.gridy++;
    panelAC1.add(
	new JLabel( "SCCH-AC1 (zus\u00E4tzlich SCCH-Module 1 (ROM-Disk)"
			+ " und 3 (RAM-Disk)):" ),
	gbcAC1 );

    this.btnAC1MonSCCH80 = new JRadioButton(
		"SCCH-Monitorprogramm 8.0, SCCH-Zeichensatz",
		false );
    this.btnAC1MonSCCH80.addActionListener( this );
    grpAC1mon.add( this.btnAC1MonSCCH80 );
    gbcAC1.insets.top  = 0;
    gbcAC1.insets.left = 50;
    gbcAC1.gridy++;
    panelAC1.add( this.btnAC1MonSCCH80, gbcAC1 );

    this.btnAC1MonSCCH1088 = new JRadioButton(
		"SCCH-Monitorprogramm 10/88, SCCH-Zeichensatz",
		false );
    this.btnAC1MonSCCH1088.addActionListener( this );
    grpAC1mon.add( this.btnAC1MonSCCH1088 );
    gbcAC1.gridy++;
    panelAC1.add( this.btnAC1MonSCCH1088, gbcAC1 );

    this.labelAC1RomdiskBegAddr = new JLabel(
			"ROM-Disk einblenden ab Adresse:" );
    gbcAC1.insets.top    = 5;
    gbcAC1.insets.bottom = 5;
    gbcAC1.gridwidth     = 1;
    gbcAC1.gridy++;
    panelAC1.add( this.labelAC1RomdiskBegAddr, gbcAC1 );

    ButtonGroup grpAC1Romdisk = new ButtonGroup();

    this.btnAC1Romdisk8000 = new JRadioButton( "8000h", false );
    this.btnAC1Romdisk8000.addActionListener( this );
    grpAC1Romdisk.add( this.btnAC1Romdisk8000 );
    gbcAC1.insets.left = 5;
    gbcAC1.gridx++;
    panelAC1.add( this.btnAC1Romdisk8000, gbcAC1 );

    this.btnAC1RomdiskC000 = new JRadioButton( "C000h", false );
    this.btnAC1RomdiskC000.addActionListener( this );
    grpAC1Romdisk.add( this.btnAC1RomdiskC000 );
    gbcAC1.gridx++;
    panelAC1.add( this.btnAC1RomdiskC000, gbcAC1 );


    // Optionen fuer BCS3
    JPanel panelBCS3 = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelBCS3, "BCS3" );

    GridBagConstraints gbcBCS3 = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpBCS3os = new ButtonGroup();

    this.btnBCS3se24_27 = new JRadioButton(
		"2 KByte BASIC-SE 2.4, 2,5 MHz, 27 Zeichen pro Zeile",
		true );
    this.btnBCS3se24_27.addActionListener( this );
    grpBCS3os.add( this.btnBCS3se24_27 );
    panelBCS3.add( this.btnBCS3se24_27, gbcBCS3 );

    this.btnBCS3se31_29 = new JRadioButton(
		"4 KByte BASIC-SE 3.1, 2,5 MHz, 29 Zeichen pro Zeile",
		false );
    this.btnBCS3se31_29.addActionListener( this );
    grpBCS3os.add( this.btnBCS3se31_29 );
    gbcBCS3.insets.top = 0;
    gbcBCS3.gridy++;
    panelBCS3.add( this.btnBCS3se31_29, gbcBCS3 );

    this.btnBCS3se31_40 = new JRadioButton(
		"4 KByte BASIC-SE 3.1, 3,5 MHz, 40 Zeichen pro Zeile",
		false );
    this.btnBCS3se31_40.addActionListener( this );
    grpBCS3os.add( this.btnBCS3se31_40 );
    gbcBCS3.insets.top = 0;
    gbcBCS3.gridy++;
    panelBCS3.add( this.btnBCS3se31_40, gbcBCS3 );

    this.btnBCS3sp33_29 = new JRadioButton(
		"4 KByte S/P-BASIC V3.3, 2,5 MHz, 29 Zeichen pro Zeile",
		false );
    this.btnBCS3sp33_29.addActionListener( this );
    grpBCS3os.add( this.btnBCS3sp33_29 );
    gbcBCS3.gridy++;
    panelBCS3.add( this.btnBCS3sp33_29, gbcBCS3 );

    ButtonGroup grpBCS3ram = new ButtonGroup();

    this.btnBCS3Ram1k = new JRadioButton( "1 KByte RAM", true );
    this.btnBCS3Ram1k.addActionListener( this );
    grpBCS3ram.add( this.btnBCS3Ram1k );
    gbcBCS3.insets.top = 10;
    gbcBCS3.gridy++;
    panelBCS3.add( this.btnBCS3Ram1k, gbcBCS3 );

    this.btnBCS3Ram17k = new JRadioButton(
				"17 KByte RAM (16 KByte RAM-Erweiterung)",
				false );
    this.btnBCS3Ram17k.addActionListener( this );
    grpBCS3ram.add( this.btnBCS3Ram17k );
    gbcBCS3.insets.top    = 0;
    gbcBCS3.insets.bottom = 5;
    gbcBCS3.gridy++;
    panelBCS3.add( this.btnBCS3Ram17k, gbcBCS3 );


    // Optionen fuer Huebler/Evert-MC
    JPanel panelHEMC = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelHEMC, "HEMC" );

    GridBagConstraints gbcHEMC = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.btnHEMCCatchPrintCalls = new JCheckBox(
		"Betriebssystemaufrufe f\u00FCr Druckerausgaben abfangen",
		true );
    this.btnHEMCCatchPrintCalls.addActionListener( this );
    panelHEMC.add( this.btnHEMCCatchPrintCalls, gbcHEMC );


    // Optionen fuer Huebler-Grafik-MC
    JPanel panelHGMC = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelHGMC, "HGMC" );

    GridBagConstraints gbcHGMC = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.btnHGMCBasic = new JCheckBox(
				"BASIC-Interpreter im ROM enthalten",
				true );
    this.btnHGMCBasic.addActionListener( this );
    panelHGMC.add( this.btnHGMCBasic, gbcHGMC );

    this.btnHGMCCatchPrintCalls = new JCheckBox(
		"Betriebssystemaufrufe f\u00FCr Druckerausgaben abfangen",
		true );
    this.btnHGMCCatchPrintCalls.addActionListener( this );
    gbcHGMC.insets.top    = 0;
    gbcHGMC.insets.bottom = 5;
    gbcHGMC.gridy++;
    panelHGMC.add( this.btnHGMCCatchPrintCalls, gbcHGMC );


    // Optionen fuer KC85/2..5
    JTabbedPane paneKC85 = new JTabbedPane( JTabbedPane.TOP );
    this.panelSysOpt.add( paneKC85, "KC85" );

    JPanel panelKC85Module = new JPanel( new GridBagLayout() );
    paneKC85.addTab( "Module", panelKC85Module );

    GridBagConstraints gbcKC85Module = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 1.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.BOTH,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.tableModelKC85Module = new ModuleTableModel();

    this.tableKC85Module = new JTable( this.tableModelKC85Module );
    this.tableKC85Module.setAutoCreateRowSorter( false );
    this.tableKC85Module.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.tableKC85Module.setColumnSelectionAllowed( false );
    this.tableKC85Module.setDragEnabled( false );
    this.tableKC85Module.setFillsViewportHeight( false );
    this.tableKC85Module.setPreferredScrollableViewportSize(
				new Dimension( 1, 1 ) );
    this.tableKC85Module.setRowSelectionAllowed( true );
    this.tableKC85Module.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );

    panelKC85Module.add(
		new JScrollPane( this.tableKC85Module ),
		gbcKC85Module );

    EmuUtil.setTableColWidths( this.tableKC85Module, 60, 60, 220 );

    JPanel panelKC85ModBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbcKC85Module.fill    = GridBagConstraints.NONE;
    gbcKC85Module.weightx = 0.0;
    gbcKC85Module.weighty = 0.0;
    gbcKC85Module.gridy++;
    panelKC85Module.add( panelKC85ModBtn, gbcKC85Module );

    this.btnKC85ModAdd = new JButton( "Hinzuf\u00FCgen" );
    this.btnKC85ModAdd.addActionListener( this );
    this.btnKC85ModAdd.addKeyListener( this );
    panelKC85ModBtn.add( this.btnKC85ModAdd );

    this.btnKC85ModRemove = new JButton( "Entfernen" );
    this.btnKC85ModRemove.addActionListener( this );
    this.btnKC85ModRemove.addKeyListener( this );
    panelKC85ModBtn.add( this.btnKC85ModRemove );

    this.selModelKC85Module = this.tableKC85Module.getSelectionModel();
    if( this.selModelKC85Module != null ) {
      this.selModelKC85Module.addListSelectionListener( this );
      this.btnKC85ModRemove.setEnabled( false );
    }

    this.popupKC85Module = new JPopupMenu();
    for( int i = 0; i < kc85Modules.length; i++ ) {
      String    modName = kc85Modules[ i ][ 0 ];
      JMenuItem modItem = new JMenuItem( String.format(
						"%s - %s",
						modName,
						kc85Modules[ i ][ 1 ] ) );
      modItem.setActionCommand( kc85ModAddPrefix + modName );
      modItem.addActionListener( this );
      this.popupKC85Module.add( modItem );
    }

    JPanel panelKC85Disk = new JPanel( new GridBagLayout() );
    paneKC85.addTab( "D004", panelKC85Disk );

    GridBagConstraints gbcKC85Disk = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnKC85D004Enabled = new JCheckBox(
			"Floppy-Disk-Erweiterung D004 emulieren",
			false );
    this.btnKC85D004Enabled.addActionListener( this );
    panelKC85Disk.add( this.btnKC85D004Enabled, gbcKC85Disk );

    this.labelKC85D004Rom   = new JLabel( "D004-ROM:" );
    gbcKC85Disk.insets.left = 50;
    gbcKC85Disk.insets.top  = 20;
    gbcKC85Disk.gridwidth   = 1;
    gbcKC85Disk.gridy++;
    panelKC85Disk.add( this.labelKC85D004Rom, gbcKC85Disk );

    this.comboKC85D004Rom = new JComboBox();
    this.comboKC85D004Rom.setEditable( false );
    this.comboKC85D004Rom.addItem( "Standard" );
    this.comboKC85D004Rom.addItem( "Version 2.0" );
    this.comboKC85D004Rom.addItem( "Version 3.2" );
    this.comboKC85D004Rom.addItem( "Datei" );
    this.comboKC85D004Rom.addActionListener( this );
    gbcKC85Disk.insets.left = 5;
    gbcKC85Disk.gridx++;
    panelKC85Disk.add( this.comboKC85D004Rom, gbcKC85Disk );

    this.fldKC85D004RomFile = new JTextField();
    this.fldKC85D004RomFile.setEditable( false );
    gbcKC85Disk.fill        = GridBagConstraints.HORIZONTAL;
    gbcKC85Disk.weightx     = 1.0;
    gbcKC85Disk.insets.top  = 0;
    gbcKC85Disk.insets.left = 50;
    gbcKC85Disk.gridwidth   = 2;
    gbcKC85Disk.gridx       = 0;
    gbcKC85Disk.gridy++;
    panelKC85Disk.add( this.fldKC85D004RomFile, gbcKC85Disk );

    this.btnKC85D004RomFileSelect = createImageButton(
					"/images/file/open.png",
					"Ausw\u00E4hlen" );
    gbcKC85Disk.fill        = GridBagConstraints.NONE;
    gbcKC85Disk.weightx     = 0.0;
    gbcKC85Disk.insets.left = 5;
    gbcKC85Disk.gridwidth   = 1;
    gbcKC85Disk.gridx += 2;
    panelKC85Disk.add( this.btnKC85D004RomFileSelect, gbcKC85Disk );

    this.btnKC85D004RomFileRemove = createImageButton(
					"/images/file/delete.png",
					"Entfernen" );
    gbcKC85Disk.gridx++;
    panelKC85Disk.add( this.btnKC85D004RomFileRemove, gbcKC85Disk );

    this.labelKC85D004Speed = new JLabel( "Taktfrequenz:" );
    gbcKC85Disk.insets.left = 50;
    gbcKC85Disk.insets.top  = 20;
    gbcKC85Disk.gridwidth   = 1;
    gbcKC85Disk.gridx       = 0;
    gbcKC85Disk.gridy++;
    panelKC85Disk.add( this.labelKC85D004Speed, gbcKC85Disk );

    JPanel panelKC85D004Speed = new JPanel(
			new FlowLayout( FlowLayout.LEFT, 10, 0 ) );
    gbcKC85Disk.insets.top    = 0;
    gbcKC85Disk.insets.bottom = 5;
    gbcKC85Disk.gridwidth     = GridBagConstraints.REMAINDER;
    gbcKC85Disk.gridy++;
    panelKC85Disk.add( panelKC85D004Speed, gbcKC85Disk );

    ButtonGroup grpKC85D004Speed = new ButtonGroup();

    this.btnKC85D004Speed4MHz = new JRadioButton( "4 MHz (original)", true );
    this.btnKC85D004Speed4MHz.addActionListener( this );
    grpKC85D004Speed.add( this.btnKC85D004Speed4MHz );
    panelKC85D004Speed.add( this.btnKC85D004Speed4MHz, gbcSys );

    this.btnKC85D004Speed8MHz = new JRadioButton( "8 MHz", false );
    this.btnKC85D004Speed8MHz.addActionListener( this );
    grpKC85D004Speed.add( this.btnKC85D004Speed8MHz );
    panelKC85D004Speed.add( this.btnKC85D004Speed8MHz, gbcSys );

    this.btnKC85D004Speed16MHz = new JRadioButton( "16 MHz", false );
    this.btnKC85D004Speed16MHz.addActionListener( this );
    grpKC85D004Speed.add( this.btnKC85D004Speed16MHz );
    panelKC85D004Speed.add( this.btnKC85D004Speed16MHz, gbcSys );

    JPanel panelKC85Etc = new JPanel( new GridBagLayout() );
    paneKC85.addTab( "Sonstiges", panelKC85Etc );

    GridBagConstraints gbcKC85Etc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.btnKC85PasteFast = new JCheckBox(
			"Einf\u00FCgen von Text direkt in den Tastaturpuffer",
			true );
    this.btnKC85PasteFast.addActionListener( this );
    panelKC85Etc.add( this.btnKC85PasteFast, gbcKC85Etc );

    gbcKC85Etc.fill       = GridBagConstraints.HORIZONTAL;
    gbcKC85Etc.weightx    = 1.0;
    gbcKC85Etc.insets.top = 10;
    gbcKC85Etc.gridy++;
    panelKC85Etc.add( new JSeparator(), gbcKC85Etc );

    gbcKC85Etc.fill    = GridBagConstraints.NONE;
    gbcKC85Etc.weightx = 0.0;
    gbcKC85Etc.gridy++;
    panelKC85Etc.add(
	new JLabel( "Die folgende Option zeigt nur in wenigen"
				+ " F\u00E4llen eine Wirkung," ),
	gbcKC85Etc );

    gbcKC85Etc.insets.top = 0;
    gbcKC85Etc.gridy++;
    panelKC85Etc.add(
	new JLabel( "ben\u00F6tigt daf\u00FCr aber relativ viel"
				+ " Rechenleistung. Sollte diese Leistung" ),
	gbcKC85Etc );

    gbcKC85Etc.gridy++;
    panelKC85Etc.add(
	new JLabel( "nicht zur Verf\u00FCgung stehen,"
				+ " dann schalten Sie die Option bitte aus." ),
	gbcKC85Etc );

    this.btnKC85VideoTiming = new JCheckBox(
			"Zeitverhalten der Bildschirmsteuerung emulieren",
			true );
    this.btnKC85VideoTiming.addActionListener( this );
    gbcKC85Etc.insets.top    = 5;
    gbcKC85Etc.insets.bottom = 5;
    gbcKC85Etc.gridy++;
    panelKC85Etc.add( this.btnKC85VideoTiming, gbcKC85Etc );


    // Optionen fuer Kramer-MC
    JPanel panelKramerMC = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelKramerMC, "KramerMC" );

    GridBagConstraints gbcKramerMC = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.btnKramerMCCatchPrintCalls = new JCheckBox(
		"Betriebssystemaufrufe f\u00FCr Druckerausgaben abfangen",
		false );
    this.btnKramerMCCatchPrintCalls.addActionListener( this );
    panelKramerMC.add( this.btnKramerMCCatchPrintCalls, gbcKramerMC );


    // Optionen fuer LC80
    JPanel panelLC80 = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelLC80, "LC80" );

    GridBagConstraints gbcLC80 = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpLC80 = new ButtonGroup();

    this.btnLC80_U505 = new JRadioButton(
		"LC-80, 2 KByte ROM (2xU505), 1 KByte RAM",
		false );
    this.btnLC80_U505.addActionListener( this );
    grpLC80.add( this.btnLC80_U505 );
    panelLC80.add( this.btnLC80_U505, gbcLC80 );

    this.btnLC80_2716 = new JRadioButton(
		"LC-80, 2 KByte ROM (2716), 4 KByte RAM",
		true );
    this.btnLC80_2716.addActionListener( this );
    grpLC80.add( this.btnLC80_2716 );
    gbcLC80.insets.top = 0;
    gbcLC80.gridy++;
    panelLC80.add( this.btnLC80_2716, gbcLC80 );

    this.btnLC80_2 = new JRadioButton(
		"LC-80.2, 4 KByte ROM mit Buschendorf-Monitor, 4 KByte RAM",
		false );
    this.btnLC80_2.addActionListener( this );
    grpLC80.add( this.btnLC80_2 );
    gbcLC80.gridy++;
    panelLC80.add( this.btnLC80_2, gbcLC80 );

    this.btnLC80e = new JRadioButton(
		"LC-80e, 12 KByte ROM mit Schachprogramm SC-80, 4 KByte RAM",
		false );
    this.btnLC80e.addActionListener( this );
    grpLC80.add( this.btnLC80e );
    gbcLC80.insets.bottom = 5;
    gbcLC80.gridy++;
    panelLC80.add( this.btnLC80e, gbcLC80 );


    // Optionen fuer PC/M
    JPanel panelPCM = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelPCM, "PCM" );

    GridBagConstraints gbcPCM = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.btnPCMAutoLoadBDOS = new JCheckBox(
					"Bei RESET automatisch BDOS laden",
					true );
    this.btnPCMAutoLoadBDOS.addActionListener( this );
    panelPCM.add( this.btnPCMAutoLoadBDOS, gbcPCM );


    // Optionen fuer Z1013
    JTabbedPane paneZ1013 = new JTabbedPane( JTabbedPane.TOP );
    this.panelSysOpt.add( paneZ1013, "Z1013" );

    JPanel panelZ1013Model = new JPanel( new GridBagLayout() );
    paneZ1013.addTab( "Modell", panelZ1013Model );

    GridBagConstraints gbcZ1013Model = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpZ1013Model = new ButtonGroup();

    this.btnZ1013_01 = new JRadioButton(
				"Z1013.01 (1 MHz, 16 KByte RAM)",
				false );
    this.btnZ1013_01.addActionListener( this );
    grpZ1013Model.add( this.btnZ1013_01 );
    panelZ1013Model.add( this.btnZ1013_01, gbcZ1013Model );

    this.btnZ1013_12 = new JRadioButton(
				"Z1013.12 (2 MHz, 1 KByte RAM)",
				false );
    this.btnZ1013_12.addActionListener( this );
    grpZ1013Model.add( this.btnZ1013_12 );
    gbcZ1013Model.insets.top = 0;
    gbcZ1013Model.gridy++;
    panelZ1013Model.add( this.btnZ1013_12, gbcZ1013Model );

    this.btnZ1013_16 = new JRadioButton(
				"Z1013.16 (2 MHz, 16 KByte RAM)",
				false );
    this.btnZ1013_16.addActionListener( this );
    grpZ1013Model.add( this.btnZ1013_16 );
    gbcZ1013Model.gridy++;
    panelZ1013Model.add( this.btnZ1013_16, gbcZ1013Model );

    this.btnZ1013_64 = new JRadioButton(
				"Z1013.64 (2 MHz, 64 KByte RAM)",
				false );
    this.btnZ1013_64.addActionListener( this );
    grpZ1013Model.add( this.btnZ1013_64 );
    gbcZ1013Model.gridy++;
    panelZ1013Model.add( this.btnZ1013_64, gbcZ1013Model );

    JPanel panelZ1013Mon = new JPanel( new GridBagLayout() );
    paneZ1013.addTab( "Monitorprogramm / Tastatur", panelZ1013Mon );

    GridBagConstraints gbcZ1013Mon = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpZ1013Mon = new ButtonGroup();

    this.btnZ1013Mon202 = new JRadioButton(
		"Monitorprogramm 2.02 / Folienflachtastatur",
		true );
    this.btnZ1013Mon202.addActionListener( this );
    grpZ1013Mon.add( this.btnZ1013Mon202 );
    panelZ1013Mon.add( this.btnZ1013Mon202, gbcZ1013Mon );

    this.btnZ1013MonA2 = new JRadioButton(
		"Monitorprogramm A.2 / Alpha-Tastatur",
		false );
    this.btnZ1013MonA2.addActionListener( this );
    grpZ1013Mon.add( this.btnZ1013MonA2 );
    gbcZ1013Mon.insets.top = 0;
    gbcZ1013Mon.gridy++;
    panelZ1013Mon.add( this.btnZ1013MonA2, gbcZ1013Mon );

    this.btnZ1013MonRB_K7659 = new JRadioButton(
		"Brosig-Monitorprogramm 2.028 / Tastatur K7659",
		false );
    this.btnZ1013MonRB_K7659.addActionListener( this );
    grpZ1013Mon.add( this.btnZ1013MonRB_K7659 );
    gbcZ1013Mon.gridy++;
    panelZ1013Mon.add( this.btnZ1013MonRB_K7659, gbcZ1013Mon );

    this.btnZ1013MonRB_S6009 = new JRadioButton(
		"Brosig-Monitorprogramm 2.028 / Tastatur S6009",
		false );
    this.btnZ1013MonRB_S6009.addActionListener( this );
    grpZ1013Mon.add( this.btnZ1013MonRB_S6009 );
    gbcZ1013Mon.gridy++;
    panelZ1013Mon.add( this.btnZ1013MonRB_S6009, gbcZ1013Mon );

    this.btnZ1013MonJM_1992 = new JRadioButton(
		"M\u00FCller-Monitorprogramm 1992 / Folienflachtastatur",
		false );
    this.btnZ1013MonJM_1992.addActionListener( this );
    grpZ1013Mon.add( this.btnZ1013MonJM_1992 );
    gbcZ1013Mon.gridy++;
    panelZ1013Mon.add( this.btnZ1013MonJM_1992, gbcZ1013Mon );

    JPanel panelZ1013Etc = new JPanel( new GridBagLayout() );
    paneZ1013.addTab( "Sonstiges", panelZ1013Etc );

    GridBagConstraints gbcZ1013Etc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.btnZ1013Graphic = new JCheckBox(
		"Vollgrafikerweiterung nach KRT 11 und FA 7/91 emulieren",
		false );
    this.btnZ1013Graphic.addActionListener( this );
    gbcZ1013Etc.gridy++;
    panelZ1013Etc.add( this.btnZ1013Graphic, gbcZ1013Etc );

    this.btnZ1013CatchPrintCalls = new JCheckBox(
		"Sprungverteileraufrufe f\u00FCr Druckerausgaben abfangen",
		true );
    this.btnZ1013CatchPrintCalls.addActionListener( this );
    gbcZ1013Etc.insets.top = 0;
    gbcZ1013Etc.gridy++;
    panelZ1013Etc.add( this.btnZ1013CatchPrintCalls, gbcZ1013Etc );

    this.btnZ1013PasteFast = new JCheckBox(
		"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs",
		true );
    this.btnZ1013PasteFast.addActionListener( this );
    gbcZ1013Etc.insets.top    = 0;
    gbcZ1013Etc.insets.bottom = 5;
    gbcZ1013Etc.gridy++;
    panelZ1013Etc.add( this.btnZ1013PasteFast, gbcZ1013Etc );


    // Optionen fuer Z9001
    JTabbedPane paneZ9001 = new JTabbedPane( JTabbedPane.TOP );
    this.panelSysOpt.add( paneZ9001, "Z9001" );

    JPanel panelZ9001Graph = new JPanel( new GridBagLayout() );
    paneZ9001.addTab( "Grafik", panelZ9001Graph );

    GridBagConstraints gbcZ9001Graph = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpZ9001Graph = new ButtonGroup();

    this.btnZ9001MonoGraphNone = new JRadioButton(
					"S/W, Blockgrafik",
					false );
    this.btnZ9001MonoGraphNone.addActionListener( this );
    grpZ9001Graph.add( this.btnZ9001MonoGraphNone );
    panelZ9001Graph.add( this.btnZ9001MonoGraphNone, gbcZ9001Graph );

    this.btnZ9001MonoGraphKRT = new JRadioButton(
					"S/W, KRT-Vollgrafikerweiterung",
					false );
    this.btnZ9001MonoGraphKRT.addActionListener( this );
    grpZ9001Graph.add( this.btnZ9001MonoGraphKRT );
    gbcZ9001Graph.insets.top = 0;
    gbcZ9001Graph.gridy++;
    panelZ9001Graph.add( this.btnZ9001MonoGraphKRT, gbcZ9001Graph );

    this.btnZ9001ColorGraphNone = new JRadioButton(
					"Farbe, Blockgrafik",
					true );
    this.btnZ9001ColorGraphNone.addActionListener( this );
    grpZ9001Graph.add( this.btnZ9001ColorGraphNone );
    gbcZ9001Graph.gridy++;
    panelZ9001Graph.add( this.btnZ9001ColorGraphNone, gbcZ9001Graph );

    this.btnZ9001ColorGraphKRT = new JRadioButton(
				"Farbe, KRT-Vollgrafikerweiterung",
				false );
    this.btnZ9001ColorGraphKRT.addActionListener( this );
    grpZ9001Graph.add( this.btnZ9001ColorGraphKRT );
    gbcZ9001Graph.gridy++;
    panelZ9001Graph.add( this.btnZ9001ColorGraphKRT, gbcZ9001Graph );

    this.btnZ9001ColorGraphRobotron = new JRadioButton(
				"Farbe, Robotron-Vollgrafikerweiterung",
				false );
    this.btnZ9001ColorGraphRobotron.addActionListener( this );
    grpZ9001Graph.add( this.btnZ9001ColorGraphRobotron );
    gbcZ9001Graph.insets.bottom = 5;
    gbcZ9001Graph.gridy++;
    panelZ9001Graph.add( this.btnZ9001ColorGraphRobotron, gbcZ9001Graph );

    JPanel panelZ9001Ram = new JPanel( new GridBagLayout() );
    paneZ9001.addTab( "RAM", panelZ9001Ram );

    GridBagConstraints gbcZ9001Ram = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpZ9001Ram = new ButtonGroup();

    this.btnZ9001Ram16k = new JRadioButton( "16 KByte RAM", true );
    this.btnZ9001Ram16k.addActionListener( this );
    grpZ9001Ram.add( this.btnZ9001Ram16k );
    panelZ9001Ram.add( this.btnZ9001Ram16k, gbcZ9001Ram );

    this.btnZ9001Ram32k = new JRadioButton(
			"32 KByte RAM (ein 16K-RAM-Modul gesteckt)", false );
    this.btnZ9001Ram32k.addActionListener( this );
    grpZ9001Ram.add( this.btnZ9001Ram32k );
    gbcZ9001Ram.insets.top = 0;
    gbcZ9001Ram.gridy++;
    panelZ9001Ram.add( this.btnZ9001Ram32k, gbcZ9001Ram );

    this.btnZ9001Ram48k = new JRadioButton(
			"48 KByte RAM (zwei 16K-RAM-Module gesteckt)", false );
    this.btnZ9001Ram48k.addActionListener( this );
    grpZ9001Ram.add( this.btnZ9001Ram48k );
    gbcZ9001Ram.gridy++;
    panelZ9001Ram.add( this.btnZ9001Ram48k, gbcZ9001Ram );

    this.btnZ9001Ram74k = new JRadioButton(
			"74 KByte RAM (ein 64K-RAM-Modul gesteckt)",
			false );
    this.btnZ9001Ram74k.addActionListener( this );
    grpZ9001Ram.add( this.btnZ9001Ram74k );
    gbcZ9001Ram.insets.bottom = 5;
    gbcZ9001Ram.gridy++;
    panelZ9001Ram.add( this.btnZ9001Ram74k, gbcZ9001Ram );

    JPanel panelZ9001Printer = new JPanel( new GridBagLayout() );
    paneZ9001.addTab( "Drucker", panelZ9001Printer );

    GridBagConstraints gbcZ9001Printer = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpZ9001Printer = new ButtonGroup();

    this.btnZ9001CatchPrintCalls = new JRadioButton(
			"BOS-Aufrufe f\u00FCr Druckerausgaben abfangen",
			true );
    this.btnZ9001CatchPrintCalls.addActionListener( this );
    grpZ9001Printer.add( this.btnZ9001CatchPrintCalls );
    panelZ9001Printer.add( this.btnZ9001CatchPrintCalls, gbcZ9001Printer );

    this.btnZ9001PrinterModule = new JRadioButton(
			"V.24-Druckermodul emulieren",
			false );
    this.btnZ9001PrinterModule.addActionListener( this );
    grpZ9001Printer.add( this.btnZ9001PrinterModule );
    gbcZ9001Printer.insets.top = 0;
    gbcZ9001Printer.gridy++;
    panelZ9001Printer.add( this.btnZ9001PrinterModule, gbcZ9001Printer );

    this.btnZ9001NoPrinter = new JRadioButton(
			"Keinen Drucker emulieren",
			false );
    this.btnZ9001NoPrinter.addActionListener( this );
    grpZ9001Printer.add( this.btnZ9001NoPrinter );
    gbcZ9001Printer.insets.bottom = 5;
    gbcZ9001Printer.gridy++;
    panelZ9001Printer.add( this.btnZ9001NoPrinter, gbcZ9001Printer );

    JPanel panelZ9001Etc = new JPanel( new GridBagLayout() );
    paneZ9001.addTab( "Sonstiges", panelZ9001Etc );

    GridBagConstraints gbcZ9001Etc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.btnZ9001FloppyDisk = new JCheckBox(
			"Boot-ROM-Modul und Floppy-Disk-Modul",
			false );
    this.btnZ9001FloppyDisk.addActionListener( this );
    panelZ9001Etc.add( this.btnZ9001FloppyDisk, gbcZ9001Etc );

    this.btnZ9001PasteFast = new JCheckBox(
			"Einf\u00FCgen von Text direkt in den Tastaturpuffer",
			true );
    this.btnZ9001PasteFast.addActionListener( this );
    gbcZ9001Etc.insets.top    = 0;
    gbcZ9001Etc.insets.bottom = 5;
    gbcZ9001Etc.gridy++;
    panelZ9001Etc.add( this.btnZ9001PasteFast, gbcZ9001Etc );


    // Bereich Geschwindigkeit
    this.tabSpeed = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Geschwindigkeit", this.tabSpeed );

    GridBagConstraints gbcSpeed = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.tabSpeed.add(
		new JLabel( "Geschwindigkeit des emulierten Systems:" ),
		gbcSpeed );

    ButtonGroup grpSpeed = new ButtonGroup();
    this.btnSpeedDefault = new JRadioButton(
				"Begrenzen auf Originalgeschwindigkeit",
				true );
    this.btnSpeedDefault.addActionListener( this );
    grpSpeed.add( this.btnSpeedDefault );
    gbcSpeed.insets.top  = 0;
    gbcSpeed.insets.left = 50;
    gbcSpeed.gridy++;
    this.tabSpeed.add( this.btnSpeedDefault, gbcSpeed );

    this.btnSpeedValue = new JRadioButton(
				"Begrenzen auf folgenden Wert:",
				false );
    this.btnSpeedValue.addActionListener( this );
    grpSpeed.add( this.btnSpeedValue );
    gbcSpeed.gridwidth = 1;
    gbcSpeed.gridy++;
    this.tabSpeed.add( this.btnSpeedValue, gbcSpeed );

    this.fldSpeed = new JTextField( 5 );
    this.docSpeed = this.fldSpeed.getDocument();
    if( this.docSpeed != null ) {
      this.docSpeed.addDocumentListener( this );
    }
    gbcSpeed.insets.left = 5;
    gbcSpeed.gridx++;
    this.tabSpeed.add( this.fldSpeed, gbcSpeed );

    this.labelSpeedUnit = new JLabel( "MHz" );
    gbcSpeed.insets.left = 5;
    gbcSpeed.gridx++;
    this.tabSpeed.add( this.labelSpeedUnit, gbcSpeed );

    this.btnSpeedUnlimited = new JRadioButton(
		"Nicht begrenzen (keine Audio- und Diskettenemulation)",
		false );
    this.btnSpeedUnlimited.addActionListener( this );
    grpSpeed.add( this.btnSpeedUnlimited );
    gbcSpeed.insets.left   = 50;
    gbcSpeed.insets.bottom = 5;
    gbcSpeed.gridwidth     = GridBagConstraints.REMAINDER;
    gbcSpeed.gridx         = 0;
    gbcSpeed.gridy++;
    this.tabSpeed.add( this.btnSpeedUnlimited, gbcSpeed );


    // Bereich Zeichensatz
    this.tabFont = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Zeichensatz", this.tabFont );

    GridBagConstraints gbcFont = new GridBagConstraints(
						0, 0,
						3, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.tabFont.add(
	new JLabel( "Sie k\u00F6nnen hier eine Zeichensatzdatei angeben," ),
	gbcFont );

    gbcFont.insets.top = 0;
    gbcFont.gridy++;
    this.tabFont.add(
	new JLabel( "die anstelle der im Emulator integrierten"
			+ " Zeichens\u00E4tze verwendet werden soll." ),
	gbcFont );

    gbcFont.insets.top = 10;
    gbcFont.gridy++;
    this.tabFont.add(
	new JLabel( "Diese Funktion hat jedoch keine Wirkung"
			+ " bei Systemen mit Vollgrafik," ),
	gbcFont );

    gbcFont.insets.top = 0;
    gbcFont.gridy++;
    this.tabFont.add(
	new JLabel( "da dort der Zeichensatz im jeweiligen Betriebssystem"
			+ " enthalten ist." ),
	gbcFont );

    gbcFont.insets.top = 10;
    gbcFont.gridy++;
    this.tabFont.add( new JLabel( "Name der Zeichensatzdatei:" ), gbcFont );

    this.fldExtFontFile = new JTextField();
    this.fldExtFontFile.setEditable( false );
    gbcFont.fill          = GridBagConstraints.HORIZONTAL;
    gbcFont.weightx       = 1.0;
    gbcFont.insets.top    = 0;
    gbcFont.insets.bottom = 5;
    gbcFont.gridwidth     = 1;
    gbcFont.gridy++;
    this.tabFont.add( this.fldExtFontFile, gbcFont );

    this.btnExtFontSelect = createImageButton(
					"/images/file/open.png",
					"Ausw\u00E4hlen" );
    gbcFont.fill    = GridBagConstraints.NONE;
    gbcFont.weightx = 0.0;
    gbcFont.gridx++;
    this.tabFont.add( this.btnExtFontSelect, gbcFont );

    this.btnExtFontRemove = createImageButton(
					"/images/file/delete.png",
					"Entfernen" );
    gbcFont.gridx++;
    this.tabFont.add( this.btnExtFontRemove, gbcFont );


    // Bereich ROM
    this.tabROM = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "ROM", this.tabROM );

    GridBagConstraints gbcROM = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.tabROM.add( new JLabel( "Eingebundene ROM-Images:" ), gbcROM );

    this.listROM = new JList();
    this.listROM.setDragEnabled( false );
    this.listROM.setLayoutOrientation( JList.VERTICAL );
    this.listROM.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.listROM.addListSelectionListener( this );
    gbcROM.fill          = GridBagConstraints.BOTH;
    gbcROM.weightx       = 1.0;
    gbcROM.weighty       = 1.0;
    gbcROM.insets.top    = 0;
    gbcROM.insets.right  = 0;
    gbcROM.insets.bottom = 5;
    gbcROM.gridwidth     = 2;
    gbcROM.gridy++;
    this.tabROM.add( new JScrollPane( this.listROM ), gbcROM );

    JPanel tabROMBtn = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
    gbcROM.fill         = GridBagConstraints.NONE;
    gbcROM.weightx      = 0.0;
    gbcROM.weighty      = 0.0;
    gbcROM.insets.right = 5;
    gbcROM.gridheight   = 1;
    gbcROM.gridx += 2;
    this.tabROM.add( tabROMBtn, gbcROM );

    this.btnROMAdd = createImageButton(
				"/images/file/open.png",
				"Hinzuf\u00FCgen" );
    tabROMBtn.add( this.btnROMAdd );

    this.btnROMRemove = createImageButton(
				"/images/file/delete.png",
				"Entfernen" );
    this.btnROMRemove.setEnabled( false );
    tabROMBtn.add( this.btnROMRemove );

    gbcROM.insets.top = 10;
    gbcROM.gridx      = 0;
    gbcROM.gridy++;
    this.tabROM.add(
	new JLabel( "ROM-Image f\u00FCr Programmpaket X"
					+ " (nur SCCH-AC1 und LLC2):" ),
	gbcROM );

    this.fldROMPrgXFile = new JTextField();
    this.fldROMPrgXFile.setEditable( false );
    gbcROM.fill       = GridBagConstraints.HORIZONTAL;
    gbcROM.weightx    = 1.0;
    gbcROM.insets.top = 0;
    gbcROM.gridwidth  = 1;
    gbcROM.gridy++;
    this.tabROM.add( this.fldROMPrgXFile, gbcROM );

    this.btnROMPrgXSelect = createImageButton(
					"/images/file/open.png",
					"Ausw\u00E4hlen" );
    gbcROM.fill    = GridBagConstraints.NONE;
    gbcROM.weightx = 0.0;
    gbcROM.gridx++;
    this.tabROM.add( this.btnROMPrgXSelect, gbcROM );

    this.btnROMPrgXRemove = createImageButton(
					"/images/file/delete.png",
					"Entfernen" );
    gbcROM.gridx++;
    this.tabROM.add( this.btnROMPrgXRemove, gbcROM );

    gbcROM.insets.top = 10;
    gbcROM.gridx      = 0;
    gbcROM.gridy++;
    tabROM.add(
	new JLabel( "ROM-Image f\u00FCr ROM-Disk (nur SCCH-AC1 und LLC2):" ),
	gbcROM );

    this.fldROMDiskFile = new JTextField();
    this.fldROMDiskFile.setEditable( false );
    gbcROM.fill          = GridBagConstraints.HORIZONTAL;
    gbcROM.weightx       = 1.0;
    gbcROM.insets.top    = 0;
    gbcROM.insets.bottom = 5;
    gbcROM.gridwidth     = 1;
    gbcROM.gridy++;
    tabROM.add( this.fldROMDiskFile, gbcROM );

    this.btnROMDiskSelect = createImageButton(
					"/images/file/open.png",
					"Ausw\u00E4hlen" );
    gbcROM.fill    = GridBagConstraints.NONE;
    gbcROM.weightx = 0.0;
    gbcROM.gridx++;
    tabROM.add( this.btnROMDiskSelect, gbcROM );

    this.btnROMDiskRemove = createImageButton(
					"/images/file/delete.png",
					"Entfernen" );
    gbcROM.gridx++;
    tabROM.add( this.btnROMDiskRemove, gbcROM );


    // Bereich RAM-Floppy
    this.tabRF = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "RAM-Floppies", this.tabRF );

    GridBagConstraints gbcRF = new GridBagConstraints(
						0, 0,
						3, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.tabRF.add(
	new JLabel( "Sie k\u00F6nnen hier Dateien angeben,"
		+ " mit denen die RAM-Floppies initialisiert werden." ),
	gbcRF );

    gbcRF.insets.top = 0;
    gbcRF.gridy++;
    this.tabRF.add(
	new JLabel( "Die jeweilige Datei wird automatisch"
		+ " in die RAM-Floppy geladen." ),
	gbcRF );

    gbcRF.insets.top = 20;
    gbcRF.gridy++;
    this.tabRF.add(
	new JLabel( "Name der Initialisierungsdatei f\u00FCr RAM-Floppy 1:" ),
	gbcRF );

    this.fldRF1File = new JTextField();
    this.fldRF1File.setEditable( false );
    gbcRF.fill       = GridBagConstraints.HORIZONTAL;
    gbcRF.weightx    = 1.0;
    gbcRF.insets.top = 0;
    gbcRF.gridwidth  = 1;
    gbcRF.gridy++;
    this.tabRF.add( this.fldRF1File, gbcRF );

    this.btnRF1Select = createImageButton(
					"/images/file/open.png",
					"Ausw\u00E4hlen" );
    gbcRF.fill    = GridBagConstraints.NONE;
    gbcRF.weightx = 0.0;
    gbcRF.gridx++;
    this.tabRF.add( this.btnRF1Select, gbcRF );

    this.btnRF1Remove = createImageButton(
					"/images/file/delete.png",
					"Entfernen" );
    this.btnRF1Remove.setEnabled( false );
    gbcRF.gridx++;
    this.tabRF.add( this.btnRF1Remove, gbcRF );

    gbcRF.insets.top = 20;
    gbcRF.gridwidth  = 3;
    gbcRF.gridx      = 0;
    gbcRF.gridy++;
    this.tabRF.add(
	new JLabel( "Name der Initialisierungsdatei f\u00FCr RAM-Floppy 2:" ),
	gbcRF );

    this.fldRF2File = new JTextField();
    this.fldRF2File.setEditable( false );
    gbcRF.fill          = GridBagConstraints.HORIZONTAL;
    gbcRF.weightx       = 1.0;
    gbcRF.insets.top    = 0;
    gbcRF.insets.bottom = 5;
    gbcRF.gridwidth     = 1;
    gbcRF.gridy++;
    this.tabRF.add( this.fldRF2File, gbcRF );

    this.btnRF2Select = createImageButton(
					"/images/file/open.png",
					"Ausw\u00E4hlen" );
    gbcRF.fill    = GridBagConstraints.NONE;
    gbcRF.weightx = 0.0;
    gbcRF.gridx++;
    this.tabRF.add( this.btnRF2Select, gbcRF );

    this.btnRF2Remove = createImageButton(
					"/images/file/delete.png",
					"Entfernen" );
    this.btnRF2Remove.setEnabled( false );
    gbcRF.gridx++;
    this.tabRF.add( this.btnRF2Remove, gbcRF );


    // Bereich Bildschirmausgabe
    this.tabScreen= new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Bildschirmausgabe", this.tabScreen );

    GridBagConstraints gbcScreen = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 10, 5, 10, 5 ),
					0, 0 );

    this.tabScreen.add( new JLabel( "Helligkeit [%]:" ), gbcScreen );

    this.sliderBrightness = new JSlider(
					SwingConstants.HORIZONTAL,
					0,
					100,
					DEFAULT_BRIGHTNESS );
    this.sliderBrightness.setMajorTickSpacing( 20 );
    this.sliderBrightness.setPaintLabels( true );
    this.sliderBrightness.setPaintTrack( true );
    this.sliderBrightness.setSnapToTicks( false );
    this.sliderBrightness.addChangeListener( this );
    gbcScreen.anchor    = GridBagConstraints.WEST;
    gbcScreen.fill      = GridBagConstraints.HORIZONTAL;
    gbcScreen.weightx   = 1.0;
    gbcScreen.gridwidth = GridBagConstraints.REMAINDER;
    gbcScreen.gridx++;
    this.tabScreen.add( this.sliderBrightness, gbcScreen );

    gbcScreen.anchor    = GridBagConstraints.EAST;
    gbcScreen.fill      = GridBagConstraints.NONE;
    gbcScreen.weightx   = 0.0;
    gbcScreen.gridwidth = 1;
    gbcScreen.gridx     = 0;
    gbcScreen.gridy++;
    this.tabScreen.add( new JLabel( "Rand:" ), gbcScreen );

    this.spinnerModelMargin = new SpinnerNumberModel( 20, 0, MAX_MARGIN, 1 );
    this.spinnerMargin = new JSpinner( this.spinnerModelMargin );
    this.spinnerMargin.addChangeListener( this );

    gbcScreen.anchor = GridBagConstraints.WEST;
    gbcScreen.fill   = GridBagConstraints.HORIZONTAL;
    gbcScreen.gridx++;
    this.tabScreen.add( this.spinnerMargin, gbcScreen );

    gbcScreen.fill = GridBagConstraints.NONE;
    gbcScreen.gridx++;
    this.tabScreen.add( new JLabel( "Pixel" ), gbcScreen );

    gbcScreen.anchor = GridBagConstraints.EAST;
    gbcScreen.gridx  = 0;
    gbcScreen.gridy++;
    this.tabScreen.add( new JLabel( "Aktualisierungszyklus:" ), gbcScreen );

    this.comboScreenRefresh = new JComboBox();
    this.comboScreenRefresh.setEditable( false );
    this.comboScreenRefresh.addItem( "10" );
    this.comboScreenRefresh.addItem( "20" );
    this.comboScreenRefresh.addItem( "30" );
    this.comboScreenRefresh.addItem( "50" );
    this.comboScreenRefresh.addItem( "100" );
    this.comboScreenRefresh.addItem( "200" );
    this.comboScreenRefresh.addActionListener( this );
    gbcScreen.anchor = GridBagConstraints.WEST;
    gbcScreen.fill   = GridBagConstraints.HORIZONTAL;
    gbcScreen.gridx++;
    this.tabScreen.add( this.comboScreenRefresh, gbcScreen );

    gbcScreen.fill = GridBagConstraints.NONE;
    gbcScreen.gridx++;
    this.tabScreen.add( new JLabel( "ms" ), gbcScreen );

    this.btnDirectCopyPaste = new JCheckBox(
		"Direktes \"Kopieren & Einf\u00FCgen\" durch Dr\u00FCcken"
			+ " der mittleren Maustaste",
		true );
    this.btnDirectCopyPaste.addActionListener( this );
    gbcScreen.anchor     = GridBagConstraints.CENTER;
    gbcScreen.insets.top = 10;
    gbcScreen.gridwidth = GridBagConstraints.REMAINDER;
    gbcScreen.gridx     = 0;
    gbcScreen.gridy++;
    this.tabScreen.add( this.btnDirectCopyPaste, gbcScreen );

    // gleicher Font von JSpinner und JComboBox
    Font font = this.comboScreenRefresh.getFont();
    if( font != null ) {
      this.spinnerMargin.setFont( font );
    }


    // Bereich Bestaetigungen
    JPanel panelConfirm = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Best\u00E4tigungen", panelConfirm );

    GridBagConstraints gbcConfirm = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelConfirm.add(
		new JLabel( "Folgende Aktionen m\u00FCssen in einem"
				+ " Dialog best\u00E4tigt werden:" ),
		gbcConfirm );

    this.btnConfirmNMI = new JCheckBox(
		"Nicht maskierbarer Interrupt (NMI)",
		true );
    this.btnConfirmNMI.addActionListener( this );
    gbcConfirm.insets.top  = 0;
    gbcConfirm.insets.left = 50;
    gbcConfirm.gridy++;
    panelConfirm.add( this.btnConfirmNMI, gbcConfirm );

    this.btnConfirmReset = new JCheckBox(
		"Emulator zur\u00FCcksetzen (RESET)",
		true );
    this.btnConfirmReset.addActionListener( this );
    gbcConfirm.gridy++;
    panelConfirm.add( this.btnConfirmReset, gbcConfirm );

    this.btnConfirmPowerOn = new JCheckBox(
		"Einschalten emulieren (Arbeitsspeicher l\u00F6schen)",
		true );
    this.btnConfirmPowerOn.addActionListener( this );
    gbcConfirm.gridy++;
    panelConfirm.add( this.btnConfirmPowerOn, gbcConfirm );

    this.btnConfirmQuit = new JCheckBox( "Emulator beenden", true );
    this.btnConfirmQuit.addActionListener( this );
    gbcConfirm.insets.bottom = 5;
    gbcConfirm.gridy++;
    panelConfirm.add( this.btnConfirmQuit, gbcConfirm );


    // Bereich Erscheinungsbild
    this.tabLAF = null;
    this.grpLAF = new ButtonGroup();
    this.lafs   = UIManager.getInstalledLookAndFeels();
    if( this.lafs != null ) {
      if( this.lafs.length > 1 ) {
	this.tabLAF = new JPanel( new GridBagLayout() );
	this.tabbedPane.addTab( "Erscheinungsbild", this.tabLAF );

	GridBagConstraints gbcLAF = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

	for( int i = 0; i < this.lafs.length; i++ ) {
	  String clName = this.lafs[ i ].getClassName();
	  if( clName != null ) {
	    JRadioButton btn = new JRadioButton( this.lafs[ i ].getName() );
	    this.grpLAF.add( btn );
	    btn.setActionCommand( clName );
	    btn.addActionListener( this );
	    if( i == this.lafs.length - 1 ) {
	      gbcLAF.insets.bottom = 5;
	    }
	    this.tabLAF.add( btn, gbcLAF );
	    gbcLAF.insets.top = 0;
	    gbcLAF.gridy++;
	    this.lafClass2Button.put( clName, btn );
	  }
	}
      }
    }


    // Bereich Sonstiges
    JPanel tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    tabEtc.add( new JLabel( "Dateiauswahldialog:" ), gbcEtc );

    ButtonGroup grpFileDlg = new ButtonGroup();

    this.btnFileDlgEmu = new JRadioButton(
				"JKCEMU-Dateiauswahldialog verwenden",
				true );
    grpFileDlg.add( this.btnFileDlgEmu );
    this.btnFileDlgEmu.addActionListener( this );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    tabEtc.add( this.btnFileDlgEmu, gbcEtc );

    this.btnFileDlgNative = new JRadioButton(
			"Dateiauswahldialog des Betriebssystems verwenden",
			false );
    grpFileDlg.add( this.btnFileDlgNative );
    this.btnFileDlgNative.addActionListener( this );
    gbcEtc.gridy++;
    tabEtc.add( this.btnFileDlgNative, gbcEtc );

    gbcEtc.insets.top    = 20;
    gbcEtc.insets.left   = 5;
    gbcEtc.insets.bottom = 0;
    gbcEtc.gridy++;
    tabEtc.add(
	new JLabel( "Statische RAM-Bereiche (SRAM) initialisieren mit:" ),
	gbcEtc );

    ButtonGroup grpSRAMInit = new ButtonGroup();

    this.btnSRAMInit00 = new JRadioButton( "Null-Bytes", true );
    grpSRAMInit.add( this.btnSRAMInit00 );
    this.btnSRAMInit00.addActionListener( this );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    tabEtc.add( this.btnSRAMInit00, gbcEtc );

    this.btnSRAMInitRandom = new JRadioButton(
			"Zufallsmuster (entspricht Originalverhalten)",
			false );
    grpSRAMInit.add( this.btnSRAMInitRandom );
    this.btnSRAMInitRandom.addActionListener( this );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    tabEtc.add( this.btnSRAMInitRandom, gbcEtc );

    File profileDir = Main.getProfileDir();
    if( profileDir != null ) {
      gbcEtc.insets.top    = 15;
      gbcEtc.insets.left   = 5;
      gbcEtc.insets.bottom = 0;
      gbcEtc.gridy++;
      tabEtc.add(
		new JLabel( "Profile werden gespeichert im Verzeichnis:" ),
		gbcEtc );

      this.fldProfileDir = new JTextField();
      this.fldProfileDir.setEditable( false );
      this.fldProfileDir.setText( profileDir.getPath() );
      gbcEtc.fill          = GridBagConstraints.HORIZONTAL;
      gbcEtc.weightx       = 1.0;
      gbcEtc.insets.top    = 0;
      gbcEtc.insets.bottom = 5;
      gbcEtc.gridy++;
      tabEtc.add( this.fldProfileDir, gbcEtc );
    }


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 5, 1, 5, 5 ) );

    gbc.anchor  = GridBagConstraints.NORTHEAST;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridx++;
    add( panelBtn, gbc );

    this.btnApply = new JButton( "\u00DCbernehmen" );
    this.btnApply.setEnabled( false );
    this.btnApply.addActionListener( this );
    this.btnApply.addKeyListener( this );
    panelBtn.add( this.btnApply );

    this.btnLoad = new JButton( "Profil laden" );
    this.btnLoad.addActionListener( this );
    this.btnLoad.addKeyListener( this );
    panelBtn.add( this.btnLoad );

    this.btnSave = new JButton( "Profil speichern" );
    this.btnSave.addActionListener( this );
    this.btnSave.addKeyListener( this );
    panelBtn.add( this.btnSave );

    this.btnHelp = new JButton( "Hilfe" );
    this.btnHelp.addActionListener( this );
    this.btnHelp.addKeyListener( this );
    panelBtn.add( this.btnHelp );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    this.btnClose.addKeyListener( this );
    panelBtn.add( this.btnClose );


    // Voreinstellungen
    updFields(
	Main.getProperties(),
	UIManager.getLookAndFeel(),
	Integer.toString( this.screenFrm.getScreenRefreshMillis() ) );
    setExtFont( this.emuThread.getExtFont() );
    setExtROMs( this.emuThread.getExtROMs() );


    // sonstiges
    if( !super.applySettings( Main.getProperties(), true ) ) {
      pack();
      setLocationByPlatform( true );
    }
    setResizable( true );
  }


  public void setExtFont( ExtFile extFont )
  {
    this.extFont = extFont;
    if( this.extFont != null ) {
      this.fldExtFontFile.setText( this.extFont.getFile().getPath() );
      this.btnExtFontRemove.setEnabled( true );
    } else {
      this.fldExtFontFile.setText( "" );
      this.btnExtFontRemove.setEnabled( false );
    }
  }


  public void setExtROMs( ExtROM[] extROMs )
  {
    this.extROMs.clear();
    if( extROMs != null ) {
      for( int i = 0; i < extROMs.length; i++ ) {
	this.extROMs.add( extROMs[ i ] );
      }
      try {
	Collections.sort( this.extROMs );
      }
      catch( ClassCastException ex ) {}
      this.listROM.setListData( this.extROMs );
    }
  }


	/* --- ChangeListener --- */

  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( (src == this.sliderBrightness) || (src == this.spinnerMargin) )
      setDataChanged();
  }


	/* --- DocumentListener --- */

  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- ListSelectionListener --- */

  public void valueChanged( ListSelectionEvent e )
  {
    Object src = e.getSource();
    if( src == this.listROM ) {
      this.btnROMRemove.setEnabled( this.listROM.getSelectedIndex() >= 0 );
    }
    else if( src == this.selModelKC85Module ) {
      this.btnKC85ModRemove.setEnabled(
			this.tableKC85Module.getSelectedRowCount() > 0 );
    }
  }


	/* --- ueberschriebene Methoden --- */

  public boolean applySettings( Properties props, boolean resizable )
  {
    updFields( props, UIManager.getLookAndFeel(), null );
    return super.applySettings( props, resizable );
  }


  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      if( e != null ) {
	Object src = e.getSource();
	if( src != null ) {
	  if( src == this.btnROMAdd ) {
	    rv = true;
	    doROMAdd();
	  }
	  else if( src == this.btnROMRemove ) {
	    rv = true;
	    doROMRemove();
	  }
	  else if( src == this.btnROMPrgXSelect ) {
	    rv = true;
	    doFileSelect(
			this.fldROMPrgXFile,
			this.btnROMPrgXRemove,
			"ROM-Datei f\u00FCr Programmpaket X ausw\u00E4hlen",
			"rom" );
	  }
	  else if( src == this.btnROMPrgXRemove ) {
	    rv = true;
	    doFileRemove( this.fldROMPrgXFile, this.btnROMPrgXRemove );
	  }
	  else if( src == this.btnROMDiskSelect ) {
	    rv = true;
	    doFileSelect(
			this.fldROMDiskFile,
			this.btnROMDiskRemove,
			"ROM-Datei f\u00FCr ROM-Disk ausw\u00E4hlen",
			"rom" );
	  }
	  else if( src == this.btnROMDiskRemove ) {
	    rv = true;
	    doFileRemove( this.fldROMDiskFile, this.btnROMDiskRemove );
	  }
	  else if( src == this.btnApply ) {
	    rv = true;
	    doApply();
	  }
	  else if( src == this.btnLoad ) {
	    rv = true;
	    doLoad();
	  }
	  else if( src == this.btnSave ) {
	    rv = true;
	    doSave();
	  }
	  else if( src == this.btnHelp ) {
	    rv = true;
	    this.screenFrm.showHelp( "/help/settings.htm" );
	  }
	  else if( src == this.btnClose ) {
	    rv = true;
	    doClose();
	  }
	  else if( (src == this.btnSysAC1)
		   || (src == this.btnSysBCS3)
		   || (src == this.btnSysC80)
		   || (src == this.btnSysHC900)
		   || (src == this.btnSysHEMC)
		   || (src == this.btnSysHGMC)
		   || (src == this.btnSysKC85_1)
		   || (src == this.btnSysKC85_2)
		   || (src == this.btnSysKC85_3)
		   || (src == this.btnSysKC85_4)
		   || (src == this.btnSysKC85_5)
		   || (src == this.btnSysKC87)
		   || (src == this.btnSysKramerMC)
		   || (src == this.btnSysLC80)
		   || (src == this.btnSysLLC1)
		   || (src == this.btnSysLLC2)
		   || (src == this.btnSysPCM)
		   || (src == this.btnSysPoly880)
		   || (src == this.btnSysSC2)
		   || (src == this.btnSysSLC1)
		   || (src == this.btnSysVCS80)
		   || (src == this.btnSysZ1013)
		   || (src == this.btnSysZ9001) )
	  {
	    rv = true;
	    updSysOptCard();
	    setDataChanged();
	  }
	  else if( (src == this.btnAC1Mon31_64x16)                              
		   || (src == this.btnAC1Mon31_64x32)                           
		   || (src == this.btnAC1MonSCCH80)                             
		   || (src == this.btnAC1MonSCCH1088) )                         
	  {                                                                     
	    rv = true;                                                          
	    updAC1FieldsEnabled();                                              
	    setDataChanged();                                                   
	  }
	  else if( src == this.btnKC85ModAdd ) {
	    rv = true;
	    doKC85ModAdd();
	  }
	  else if( src == this.btnKC85ModRemove ) {
	    rv = true;
	    doKC85ModRemove();
	  }
	  else if( (src == this.btnKC85D004Enabled)
		   || (src == this.comboKC85D004Rom)
		   || (src == this.btnKC85D004Speed4MHz)
		   || (src == this.btnKC85D004Speed8MHz)
		   || (src == this.btnKC85D004Speed16MHz) )
	  {
	    rv = true;
	    setDataChanged();
	    updKC85D004FieldsEnabled();
	  }
	  else if( src == this.btnKC85D004RomFileSelect ) {
	    rv = true;
	    doFileSelect(
			this.fldKC85D004RomFile,
			this.btnKC85D004RomFileRemove,
			"D004-ROM-Datei ausw\u00E4hlen",
			"rom" );
	  }
	  else if( src == this.btnKC85D004RomFileRemove ) {
	    rv = true;
	    doKC85D004RomFileRemove();
	  }
	  else if( (src == this.btnSpeedDefault)
		   || (src == this.btnSpeedValue)
		   || (src == this.btnSpeedUnlimited) )
	  {
	    rv = true;
	    updSpeedFieldsEnabled();
	    setDataChanged();
	  }
	  else if( src == this.btnExtFontSelect ) {
	    rv = true;
	    doExtFontSelect();
	  }
	  else if( src == this.btnExtFontRemove ) {
	    rv = true;
	    doExtFontRemove();
	  }
	  else if( src == this.btnRF1Select ) {
	    rv = true;
	    doFileSelect(
			this.fldRF1File,
			this.btnRF1Remove,
			"RAM-Floppy 1 laden",
			"ramfloppy" );
	  }
	  else if( src == this.btnRF1Remove ) {
	    rv = true;
	    doFileRemove( this.fldRF1File, this.btnRF1Remove );
	  }
	  else if( src == this.btnRF2Select ) {
	    rv = true;
	    doFileSelect(
			this.fldRF2File,
			this.btnRF2Remove,
			"RAM-Floppy 2 laden",
			"ramfloppy" );
	  }
	  else if( src == this.btnRF2Remove ) {
	    rv = true;
	    doFileRemove( this.fldRF2File, this.btnRF2Remove );
	  }
	  else if( (src instanceof JCheckBox)
		   || (src instanceof JComboBox)
		   || (src instanceof JRadioButton) )
	  {
	    rv = true;
	    setDataChanged();
	  }
	  else if( e instanceof ActionEvent ) {
	    String cmd = ((ActionEvent) e).getActionCommand();
	    if( cmd != null ) {
	      if( cmd.startsWith( kc85ModAddPrefix ) ) {
		int len = kc85ModAddPrefix.length();
		if( cmd.length() > len ) {
		  if( addKC85Module( null, cmd.substring( len ) ) ) {
		    setDataChanged();
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    catch( UserInputException ex ) {
      BasicDlg.showErrorDlg( this, ex );
    }
    return rv;
  }


  public void setVisible( boolean state )
  {
    if( state && !isVisible() ) {
      updFields(
		Main.getProperties(),
		UIManager.getLookAndFeel(),
		Integer.toString( this.screenFrm.getScreenRefreshMillis() ) );
      setExtFont( this.emuThread.getExtFont() );
      setExtROMs( this.emuThread.getExtROMs() );
    }
    super.setVisible( state );
  }


  public void lookAndFeelChanged()
  {
    pack();
    if( this.popupKC85Module != null )
      SwingUtilities.updateComponentTreeUI( this.popupKC85Module );
  }


	/* --- Aktionen --- */

  private void doApply() throws UserInputException
  {
    Properties props = new Properties();
    applyConfirm( props );
    applyEtc( props );
    applyFont( props );
    applyRF( props );
    applyROM( props );
    applyScreen( props );
    applySpeed( props );
    applySys( props );

    /*
     * Das Look&Feel als letztes setzen,
     * Anderenfalls koennte noch eine nachfolgende Aktion einen Fehler
     * erzeugen und das Uebernehmen der Einstellungen abbrechen.
     * In dem Fall waere das neue Erscheinungsbild schon eingestellt,
     * was vom Programmverhalten her inkonsistent waere.
     */
    applyLAF( props );

    // Array mit neuen ROMs erzeugen
    ExtROM[] newExtROMs = null;
    if( !this.extROMs.isEmpty() ) {
      try {
	newExtROMs = this.extROMs.toArray(
				new ExtROM[ this.extROMs.size() ] );
      }
      catch( ArrayStoreException ex ) {}
    }

    /*
     * Wenn sich die eingebundenen ROM-Images geaendert haben,
     * ist ein RESET erforderlich.
     */
    boolean forceReset = false;
    if( !forceReset ) {
      int nNewExtROMs = 0;
      if( newExtROMs != null ) {
	nNewExtROMs = newExtROMs.length;
      }
      ExtROM[] oldExtROMs  = this.emuThread.getExtROMs();
      int      nOldExtROMs = 0;
      if( oldExtROMs != null ) {
	nOldExtROMs = oldExtROMs.length;
      }
      if( nNewExtROMs == nOldExtROMs ) {
	if( (newExtROMs != null) && (oldExtROMs != null) ) {
	  for( int i = 0; i < newExtROMs.length; i++ ) {
	    if( !newExtROMs[ i ].equals( oldExtROMs[ i ] ) ) {
	      forceReset = true;
	      break;
	    }
	  }
	}
      } else {
	forceReset = true;
      }
    }

    // neue Eigenschaften anwenden
    Properties appProps = Main.getProperties();
    if( appProps != null ) {
      appProps.putAll( props );
    } else {
      appProps = props;
    }
    this.emuThread.applySettings(
				appProps,
				this.extFont,
				newExtROMs,
				forceReset );
    Main.applyProfileToFrames( this.profileFile, appProps, false, this );

    if( !this.btnSpeedValue.isSelected() ) {
      EmuSys emuSys = this.emuThread.getNextEmuSys();
      if( emuSys == null ) {
	emuSys = this.emuThread.getEmuSys();
      }
      setSpeedValueFld( EmuThread.getDefaultSpeedKHz( props ) );
    }

    this.btnApply.setEnabled( false );
    if( this.btnSave != null ) {
      this.btnSave.setEnabled( true );
    }
  }


  private void doLoad()
  {
    ProfileDlg dlg = new ProfileDlg(
				this,
				"Profil laden",
				"Laden",
				Main.getProfileFile(),
				false );
    dlg.setVisible( true );
    File file = dlg.getSelectedProfile();
    if( file != null ) {
      Properties props = Main.loadProperties( file );
      if( props != null ) {
        updFields( props, null, null );
	setExtFont( EmuUtil.readExtFont( this.screenFrm, props ) );
	setExtROMs( EmuUtil.readExtROMs( this.screenFrm, props ) );
        setDataChanged();
        this.profileFile = file;
      }
    }
  }


  private void doSave()
  {
    Properties props = Main.getProperties();
    if( props == null ) {
      props = new Properties();
    }

    // Profile-Auswahlbox
    ProfileDlg dlg = new ProfileDlg(
				this,
				"Profil speichern",
				"Speichern",
				this.profileFile,
				true );
    dlg.setVisible( true );
    File profileFile = dlg.getSelectedProfile();
    if( profileFile != null ) {

      // Eigenschaften sammeln
      Frame[] frms = Frame.getFrames();
      if( frms != null ) {
	for( int i = 0; i < frms.length; i++ ) {
	  Frame f = frms[ i ];
	  if( f != null ) {
	    if( f instanceof BasicFrm )
	      ((BasicFrm) f).putSettingsTo( props );
	  }
	}
      }

      // ggf. Verzeichnis anlegen
      File profileDir = profileFile.getParentFile();
      if( profileDir != null ) {
	profileDir.mkdirs();
      }

      // eigentliches Speichern
      FileWriter out  = null;
      try {
	out = new FileWriter( profileFile );
	props.store( out, "JKCEMU Profileigenschaften" );
	out.close();
	out = null;
	this.profileFile = profileFile;
	Main.setProfile( this.profileFile, props );
      }
      catch( IOException ex ) {
	BasicDlg.showErrorDlg(
		this,
		"Die Einstellungen k\u00F6nnen nicht in die Datei\n\'"
			+ profileFile.getPath()
			+ "\'\ngespeichert werden." );
      }
      finally {
	EmuUtil.doClose( out );
      }
    }
  }


  private void doExtFontSelect()
  {
    String oldFileName = this.fldExtFontFile.getText();
    if( oldFileName != null ) {
      if( oldFileName.isEmpty() ) {
	oldFileName = null;
      }
    }
    File file = EmuUtil.showFileOpenDlg(
				this,
				"Zeichensatzdatei laden",
				oldFileName != null ?
					new File( oldFileName )
					: Main.getLastPathFile( "rom" ),
				EmuUtil.getBinaryFileFilter() );
    if( file != null ) {
      try {
	this.extFont = new ExtFile( file );
	this.fldExtFontFile.setText( file.getPath() );
	this.btnExtFontRemove.setEnabled( true );
	setDataChanged();
	Main.setLastFile( file, "rom" );
      }
      catch( Exception ex ) {
	BasicDlg.showErrorDlg( this, ex );
      }
    }
  }


  private void doExtFontRemove()
  {
    this.extFont = null;
    this.fldExtFontFile.setText( "" );
    this.btnExtFontRemove.setEnabled( false );
    setDataChanged();
  }


  private void doFileSelect(
			JTextField fldFile,
			JButton    btnRemove,
			String     title,
			String     category )
  {
    String oldFileName = fldFile.getText();
    if( oldFileName != null ) {
      if( oldFileName.isEmpty() ) {
	oldFileName = null;
      }
    }
    File file = EmuUtil.showFileOpenDlg(
				this,
				title,
				oldFileName != null ?
					new File( oldFileName )
					: Main.getLastPathFile( category ),
				EmuUtil.getBinaryFileFilter() );
    if( file != null ) {
      String msg = null;
      if( file.exists() ) {
	if( file.canRead() ) {
	  if( file.length() > 0 ) {
	    fldFile.setText( file.getPath() );
	    btnRemove.setEnabled( true );
	    setDataChanged();
	    Main.setLastFile( file, category );
	  } else {
	    msg = "Datei ist leer";
	  }
	} else {
	  msg = "Datei nicht lesbar";
	}
      } else {
	msg = "Datei nicht gefunden";
      }
      if( msg != null ) {
	BasicDlg.showErrorDlg( this, file.getPath() + ": " + msg );
      }
    }
  }


  private void doFileRemove( JTextField fldFile, JButton btnRemove )
  {
    fldFile.setText( "" );
    btnRemove.setEnabled( false );
    setDataChanged();
  }


  private void doROMAdd()
  {
    File file = EmuUtil.showFileOpenDlg(
				this,
				"ROM-Image-Datei laden",
				Main.getLastPathFile( "rom" ),
				EmuUtil.getBinaryFileFilter() );
    if( file != null ) {
      Integer begAddr  = null;
      String  fileName = file.getName();
      if( fileName != null ) {
	int len = fileName.length();
	int pos = fileName.indexOf( '_' );
	while( (pos >= 0) && ((pos + 4) < len) ) {
	  pos++;
	  int v = 0;
	  for( int i = 0; i < 4; i++ ) {
	    char ch = fileName.charAt( pos++ );
	    if( (ch >= '0') && (ch <= '9') ) {
	      v = (v << 4) | (ch - '0');
	    }
	    else if( (ch >= 'A') && (ch <= 'F') ) {
	      v = (v << 4) | (ch - 'A') + 10;
	    }
	    else if( (ch >= 'a') && (ch <= 'f') ) {
	      v = (v << 4) | (ch - 'F') + 10;
	    } else {
	      v = -1;
	      break;
	    }
	  }
	  if( v >= 0 ) {
	    begAddr = new Integer( v );
	    break;
	  }
	  if( pos + 4 < len ) {
	    pos = fileName.indexOf( '_', pos );
	  } else {
	    pos = -1;
	  }
	}
      }
      try {
	ReplyHexDlg dlg = new ReplyHexDlg(
					this,
					"Anfangsadresse:",
					4,
					begAddr );
	dlg.setVisible( true );
	Integer addr = dlg.getReply();
	if( addr != null ) {
	  ExtROM rom = new ExtROM( file );
	  rom.setBegAddress( addr.intValue() );
	  this.extROMs.add( rom );
	  try {
	    Collections.sort( this.extROMs );
	  }
	  catch( ClassCastException ex ) {}
	  this.listROM.setListData( this.extROMs );
	  setDataChanged();

	  int idx = this.extROMs.indexOf( rom );
	  if( idx >= 0 ) {
	    this.listROM.setSelectedIndex( idx );
	  }
	  Main.setLastFile( file, "rom" );
	}
      }
      catch( Exception ex ) {
	BasicDlg.showErrorDlg( this, ex );
      }
    }
  }


  private void doROMRemove()
  {
    int[] rows = this.listROM.getSelectedIndices();
    if( rows != null ) {
      if( rows.length > 0 ) {
	Arrays.sort( rows );
	for( int i = rows.length - 1; i >= 0; --i ) {
	  this.extROMs.remove( rows[ i ] );
	}
	this.listROM.setListData( this.extROMs );
	setDataChanged();
      }
    }
  }


  private void doROMPrgXRemove()
  {
    this.fldROMPrgXFile.setText( "" );
    this.btnROMPrgXRemove.setEnabled( false );
    setDataChanged();
  }


  private void doROMDiskRemove()
  {
    this.fldROMDiskFile.setText( "" );
    this.btnROMDiskRemove.setEnabled( false );
    setDataChanged();
  }


  private void doKC85D004RomFileRemove()
  {
    this.fldKC85D004RomFile.setText( "" );
    this.btnKC85D004RomFileRemove.setEnabled( false );
    setDataChanged();
  }


  private void doKC85ModAdd()
  {
    this.popupKC85Module.show(
		this.btnKC85ModAdd,
		0,
		this.btnKC85ModAdd.getHeight() );
  }


  private void doKC85ModRemove()
  {
    int[] rows = this.tableKC85Module.getSelectedRows();
    if( rows != null ) {
      if( rows.length > 0 ) {
	Arrays.sort( rows );
	for( int i = rows.length - 1; i >= 0; --i ) {
	  int row = this.tableKC85Module.convertRowIndexToModel( rows[ i ] );
	  if( row >= 0 ) {
	    this.tableModelKC85Module.removeRow( row );
	  }
	}
	int nRows = this.tableModelKC85Module.getRowCount();
	for( int i = 0; i < nRows; i++ ) {
	  this.tableModelKC85Module.setValueAt(
			String.format( "%02X", (i + 2) * 4 ), i, 0 );
	}
	setDataChanged();
      }
    }
  }


	/* --- private Methoden --- */

  private boolean addKC85Module( String slotText, String modName )
  {
    boolean rv    = false;
    int     nRows = this.tableModelKC85Module.getRowCount();
    if( (modName != null) && (nRows < 62) ) {
      if( modName.length() > 0 ) {
	for( int i = 0; i < kc85Modules.length; i++ ) {
	  String s = kc85Modules[ i ][ 0 ];
	  if( modName.equals( s ) ) {
	    if( slotText == null ) {
	      slotText = String.format( "%02X", (nRows + 2) * 4 );
	    }
	    this.tableModelKC85Module.addRow(
					slotText,
					modName,
					kc85Modules[ i ][ 1 ] );
	    rv = true;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  private void applyConfirm( Properties props )
  {
    props.setProperty(
		"jkcemu.confirm.nmi",
		Boolean.toString( this.btnConfirmNMI.isSelected() ) );
    props.setProperty(
		"jkcemu.confirm.reset",
		Boolean.toString( this.btnConfirmReset.isSelected() ) );
    props.setProperty(
		"jkcemu.confirm.power_on",
		Boolean.toString( this.btnConfirmPowerOn.isSelected() ) );
    props.setProperty(
		"jkcemu.confirm.quit",
		Boolean.toString( this.btnConfirmQuit.isSelected() ) );
  }


  private void applyEtc( Properties props )
  {
    props.setProperty(
		"jkcemu.filedialog",
		this.btnFileDlgNative.isSelected() ? "native" : "jkcemu" );
    props.setProperty(
		"jkcemu.sram.init",
		this.btnSRAMInit00.isSelected() ? "00" : "random" );
  }


  private void applyFont( Properties props )
  {
    props.setProperty(
		"jkcemu.font.file.name",
		this.extFont != null ? this.extFont.getFile().getPath() : "" );
  }


  private void applyLAF( Properties props ) throws UserInputException
  {
    ButtonModel bm = this.grpLAF.getSelection();
    if( bm != null ) {
      String lafClassName = bm.getActionCommand();
      if( lafClassName != null ) {
	if( lafClassName.length() > 0 ) {
	  boolean     lafChanged = true;
	  LookAndFeel oldLAF     = UIManager.getLookAndFeel();
	  if( oldLAF != null ) {
	    if( lafClassName.equals( oldLAF.getClass().getName() ) ) {
	      lafChanged = false;
	    }
	  }
	  if( lafChanged ) {
	    try {
	      UIManager.setLookAndFeel( lafClassName );
	      SwingUtilities.invokeLater(
				new Runnable()
				{
				  public void run()
				  {
				    informLAFChanged();
				  }
				} );
	      props.setProperty(
			"jkcemu.lookandfeel.classname",
			lafClassName );
	    }
	    catch( Exception ex ) {
	      if( this.tabLAF != null ) {
		this.tabbedPane.setSelectedComponent( this.tabLAF );
	      }
	      throw new UserInputException(
		"Das Erscheinungsbild kann nicht eingestellt werden." );
	    }
	  }
	}
      }
    }
  }


  private void applyRF( Properties props )
  {
    String fileName = this.fldRF1File.getText();
    props.setProperty(
		"jkcemu.ramfloppy.1.file.name",
		fileName != null ? fileName : "" );

    fileName = this.fldRF2File.getText();
    props.setProperty(
		"jkcemu.ramfloppy.2.file.name",
		fileName != null ? fileName : "" );
  }


  private void applyROM( Properties props ) throws UserInputException
  {
    boolean rv = true;
    int     n  = 0;
    int     a  = -1;
    for( ExtROM rom : this.extROMs ) {
      n++;
      int begAddr = rom.getBegAddress();
      if( begAddr <= a ) {
	this.tabbedPane.setSelectedComponent( this.tabROM );
	throw new UserInputException(
		String.format(
			"ROM an Adresse %04X \u00FCberschneidet sich"
				+ " mit vorherigem ROM.",
			begAddr ) );
      }
      props.setProperty(
		String.format( "jkcemu.rom.%d.address", n ),
		String.format( "%04X", begAddr ) );
      props.setProperty(
		String.format( "jkcemu.rom.%d.file", n ),
		rom.getFile().getPath() );
      a = rom.getEndAddress();
    }
    props.setProperty( "jkcemu.rom.count", Integer.toString( n ) );
    String fileName = this.fldROMPrgXFile.getText();
    props.setProperty(
		"jkcemu.program_x.file.name",
		fileName != null ? fileName : "" );
    fileName = this.fldROMDiskFile.getText();
    props.setProperty(
		"jkcemu.romdisk.file.name",
		fileName != null ? fileName : "" );
  }


  private void applyScreen( Properties props )
  {
    props.setProperty(
		"jkcemu.brightness",
		String.valueOf( this.sliderBrightness.getValue() ) );

    Object obj = this.spinnerMargin.getValue();
    props.setProperty(
		"jkcemu.screen.margin",
		obj != null ? obj.toString() : "0" );

    obj = this.comboScreenRefresh.getSelectedItem();
    if( obj != null ) {
      String text = obj.toString();
      if( text != null )
	props.setProperty( "jkcemu.screen.refresh.ms", text );
    }

    props.setProperty(
		"jkcemu.copy_and_paste.direct",
		Boolean.toString( this.btnDirectCopyPaste.isSelected() ) );
  }


  private void applySpeed( Properties props ) throws UserInputException
  {
    if( this.btnSpeedValue.isSelected() ) {
      boolean done = false;
      String  msg  = "Sie m\u00FCssen einen Wert f\u00FCr"
				+ " die max. Geschwindigkeit eingeben.";
      String  text = this.fldSpeed.getText();
      if( text != null ) {
	if( !text.isEmpty() ) {
	  msg = "Die eingegebene max. Geschwindigkeit ist ung\u00FCltig.";
	  try {
	    Number mhzValue = this.fmtSpeed.parse( text );
	    if( mhzValue != null ) {
	      long khzValue = Math.round( mhzValue.doubleValue() * 1000.0 );
	      if( khzValue > 0 ) {
		props.setProperty(
				"jkcemu.maxspeed.khz",
				String.valueOf( khzValue ) );
		done = true;
	      }
	    }
	  }
	  catch( ParseException ex ) {}
	}
      }
      if( !done ) {
	this.tabbedPane.setSelectedComponent( this.tabSpeed );
	throw new UserInputException( msg );
      }
    } else if( this.btnSpeedUnlimited.isSelected() ) {
      props.setProperty( "jkcemu.maxspeed.khz", "unlimited" );
    } else {
      props.setProperty( "jkcemu.maxspeed.khz", "default" );
    }
  }


  private void applySys( Properties props ) throws UserInputException
  {
    // System
    String valueSys = "";
    if( this.btnSysAC1.isSelected() ) {
      valueSys = "AC1";
    }
    else if( this.btnSysBCS3.isSelected() ) {
      valueSys = "BCS3";
    }
    else if( this.btnSysC80.isSelected() ) {
      valueSys = "C80";
    }
    else if( this.btnSysHC900.isSelected() ) {
      valueSys = "HC900";
    }
    else if( this.btnSysHEMC.isSelected() ) {
      valueSys = "HueblerEvertMC";
    }
    else if( this.btnSysHGMC.isSelected() ) {
      valueSys = "HueblerGraphicsMC";
    }
    else if( this.btnSysKC85_1.isSelected() ) {
      valueSys = "KC85/1";
    }
    else if( this.btnSysKC85_2.isSelected() ) {
      valueSys = "KC85/2";
    }
    else if( this.btnSysKC85_3.isSelected() ) {
      valueSys = "KC85/3";
    }
    else if( this.btnSysKC85_4.isSelected() ) {
      valueSys = "KC85/4";
    }
    else if( this.btnSysKC85_5.isSelected() ) {
      valueSys = "KC85/5";
    }
    else if( this.btnSysKC87.isSelected() ) {
      valueSys = "KC87";
    }
    else if( this.btnSysKramerMC.isSelected() ) {
      valueSys = "KramerMC";
    }
    else if( this.btnSysLC80.isSelected() ) {
      if( this.btnLC80_U505.isSelected() ) {
	valueSys = "LC80_U505";
      } else if( this.btnLC80_2.isSelected() ) {
	valueSys = "LC80.2";
      } else if( this.btnLC80e.isSelected() ) {
	valueSys = "LC80e";
      } else {
	valueSys = "LC80_2716";
      }
    }
    else if( this.btnSysLLC1.isSelected() ) {
      valueSys = "LLC1";
    }
    else if( this.btnSysLLC2.isSelected() ) {
      valueSys = "LLC2";
    }
    else if( this.btnSysPCM.isSelected() ) {
      valueSys = "PCM";
    }
    else if( this.btnSysPoly880.isSelected() ) {
      valueSys = "Poly880";
    }
    else if( this.btnSysSC2.isSelected() ) {
      valueSys = "SC2";
    }
    else if( this.btnSysSLC1.isSelected() ) {
      valueSys = "SLC1";
    }
    else if( this.btnSysVCS80.isSelected() ) {
      valueSys = "VCS80";
    }
    else if( this.btnSysZ1013.isSelected() ) {
      if( this.btnZ1013_01.isSelected() ) {
	valueSys = "Z1013.01";
      } else if( this.btnZ1013_12.isSelected() ) {
	valueSys = "Z1013.12";
      } else if( this.btnZ1013_16.isSelected() ) {
	valueSys = "Z1013.16";
      } else {
	valueSys = "Z1013.64";
      }
    }
    else if( this.btnSysZ9001.isSelected() ) {
      valueSys = "Z9001";
    }
    props.setProperty( "jkcemu.system", valueSys );

    // Optionen fuer AC1
    String ac1mon = "3.1_64x32";
    if( this.btnAC1Mon31_64x16.isSelected() ) {
      ac1mon = "3.1_64x16";
    }
    else if( this.btnAC1MonSCCH80.isSelected() ) {
      ac1mon = "SCCH8.0";
    }
    else if( this.btnAC1MonSCCH1088.isSelected() ) {
      ac1mon = "SCCH10/88";
    }
    props.setProperty( "jkcemu.ac1.monitor", ac1mon );
    props.setProperty(                                                        
		"jkcemu.ac1.romdisk.address.begin",
		this.btnAC1Romdisk8000.isSelected() ? "8000" : "C000" );

    // Optionen fuer BCS3
    if( this.btnBCS3se31_29.isSelected() ) {
      props.setProperty( "jkcemu.bcs3.os.version", "3.1" );
      props.setProperty( "jkcemu.bcs3.chars_per_line", "29" );
    } else if( this.btnBCS3se31_40.isSelected() ) {
      props.setProperty( "jkcemu.bcs3.os.version", "3.1" );
      props.setProperty( "jkcemu.bcs3.chars_per_line", "40" );
    } else if( this.btnBCS3sp33_29.isSelected() ) {
      props.setProperty( "jkcemu.bcs3.os.version", "3.3" );
      props.setProperty( "jkcemu.bcs3.chars_per_line", "29" );
    } else {
      props.setProperty( "jkcemu.bcs3.os.version", "2.4" );
      props.setProperty( "jkcemu.bcs3.chars_per_line", "27" );
    }
    if( this.btnBCS3Ram17k.isSelected() ) {
      props.setProperty( "jkcemu.bcs3.ram.kbyte", "17" );
    } else {
      props.setProperty( "jkcemu.bcs3.ram.kbyte", "1" );
    }

    // Optionen fuer Huebler/Evert-MC
    props.setProperty(
		"jkcemu.hemc.catch_print_calls",
		Boolean.toString( this.btnHEMCCatchPrintCalls.isSelected() ) );

    // Optionen fuer Huebler-Grafik-MC
    props.setProperty(
		"jkcemu.hgmc.basic",
		Boolean.toString( this.btnHGMCBasic.isSelected() ) );
    props.setProperty(
		"jkcemu.hgmc.catch_print_calls",
		Boolean.toString( this.btnHGMCCatchPrintCalls.isSelected() ) );

    // Optionen fuer KC85/2..5
    int nRows = this.tableModelKC85Module.getRowCount();
    for( int i = 0; i < nRows; i++ ) {
      Object slotObj = this.tableModelKC85Module.getValueAt( i, 0 );
      Object modObj  = this.tableModelKC85Module.getValueAt( i, 1 );
      if( (slotObj != null) && (modObj != null) ) {
	String slotText = slotObj.toString();
	String modText  = modObj.toString();
	if( (slotText != null) && (modText != null) ) {
	  if( (slotText.length() > 0) && (modText.length() > 0) ) {
	    props.setProperty( kc85ModPropPrefix + slotText, modText );
	  }
	}
      }
    }
    props.setProperty(
		kc85ModPropPrefix + "count",
		Integer.toString( nRows ) );
    boolean d004Enabled = this.btnKC85D004Enabled.isSelected();
    props.setProperty(
		"jkcemu.kc85.d004.enabled",
		Boolean.toString( d004Enabled ) );
    String kc85D004Rom = "standard";
    switch( this.comboKC85D004Rom.getSelectedIndex() ) {
      case 1:
	kc85D004Rom = "2.0";
	break;
      case 2:
	kc85D004Rom = "3.2";
	break;
      case 3:
	kc85D004Rom = this.fldKC85D004RomFile.getText();
	if( kc85D004Rom != null ) {
	  if( kc85D004Rom.isEmpty()
	      && d004Enabled
	      && (this.curSysOptCard != null) )
	  {
	    if( this.curSysOptCard.equals( "KC85" ) ) {
	      this.tabbedPane.setSelectedComponent( this.tabSys );
	      throw new UserInputException(
			"D004-ROM: Bitte w\u00E4hlen Sie eine ROM-Datei aus\n"
				+ "oder stellen Sie einen anderen D004-ROM"
				+ " ein." );
	    }
	  }
	  kc85D004Rom = "file:" + kc85D004Rom;
	}
	break;
    }
    props.setProperty( "jkcemu.kc85.d004.rom", kc85D004Rom );
    if( this.btnKC85D004Speed8MHz.isSelected() ) {
      props.setProperty( "jkcemu.kc85.d004.maxspeed.khz", "8000" );
    } else if( this.btnKC85D004Speed16MHz.isSelected() ) {
      props.setProperty( "jkcemu.kc85.d004.maxspeed.khz", "16000" );
    } else {
      props.setProperty( "jkcemu.kc85.d004.maxspeed.khz", "4000" );
    }
    props.setProperty(
		"jkcemu.kc85.paste.fast",
		Boolean.toString( this.btnKC85PasteFast.isSelected() ) );
    props.setProperty(
		"jkcemu.kc85.emulate_video_timing",
		Boolean.toString( this.btnKC85VideoTiming.isSelected() ) );

    // Optionen fuer Kramer-MC
    props.setProperty(
		"jkcemu.kramermc.catch_print_calls",
		Boolean.toString(
			this.btnKramerMCCatchPrintCalls.isSelected() ) );

    // Optionen fuer PC/M
    props.setProperty(
		"jkcemu.pcm.auto_load_bdos",
		Boolean.toString( this.btnPCMAutoLoadBDOS.isSelected() ) );

    // Optionen fuer Z1013
    String z1013mon = "2.02";
    if( this.btnZ1013MonA2.isSelected() ) {
      z1013mon = "A.2";
    }
    else if( this.btnZ1013MonRB_K7659.isSelected() ) {
      z1013mon = "RB_K7659";
    }
    else if( this.btnZ1013MonRB_S6009.isSelected() ) {
      z1013mon = "RB_S6009";
    }
    else if( this.btnZ1013MonJM_1992.isSelected() ) {
      z1013mon = "JM_1992";
    }
    props.setProperty( "jkcemu.z1013.monitor", z1013mon );
    props.setProperty(
		"jkcemu.z1013.graphic",
		Boolean.toString( this.btnZ1013Graphic.isSelected() ) );
    props.setProperty(
		"jkcemu.z1013.catch_print_calls",
		Boolean.toString(
			this.btnZ1013CatchPrintCalls.isSelected() ) );
    props.setProperty(
		"jkcemu.z1013.paste.fast",
		Boolean.toString(
			this.btnZ1013PasteFast.isSelected() ) );

    // Optionen fuer Z9001
    boolean z9001Color = false;
    String  z9001Graph = "none";
    if( this.btnZ9001MonoGraphKRT.isSelected() ) {
      z9001Graph = "krt";
    } else if( this.btnZ9001ColorGraphNone.isSelected() ) {
      z9001Color = true;
    } else if( this.btnZ9001ColorGraphKRT.isSelected() ) {
      z9001Color = true;
      z9001Graph = "krt";
    } else if( this.btnZ9001ColorGraphRobotron.isSelected() ) {
      z9001Color = true;
      z9001Graph = "robotron";
    }
    props.setProperty( "jkcemu.z9001.color", Boolean.toString( z9001Color ) );
    props.setProperty( "jkcemu.z9001.graphic.type", z9001Graph );
    String z9001ram = "16";
    if( this.btnZ9001Ram32k.isSelected() ) {
      z9001ram = "32";
    }
    else if( this.btnZ9001Ram48k.isSelected() ) {
      z9001ram = "48";
    }
    else if( this.btnZ9001Ram74k.isSelected() ) {
      z9001ram = "74";
    }
    props.setProperty( "jkcemu.z9001.ram.kbyte", z9001ram );
    props.setProperty(
		"jkcemu.z9001.catch_print_calls",
		Boolean.toString(
			this.btnZ9001CatchPrintCalls.isSelected() ) );
    props.setProperty(
		"jkcemu.z9001.printer_module.enabled",
		Boolean.toString(
			this.btnZ9001PrinterModule.isSelected() ) );
    props.setProperty(
		"jkcemu.z9001.floppy_disk.enabled",
		Boolean.toString( this.btnZ9001FloppyDisk.isSelected() ) );
    props.setProperty(
		"jkcemu.z9001.paste.fast",
		Boolean.toString( this.btnZ9001PasteFast.isSelected() ) );
  }


  private static boolean differs( String s1, String s2 )
  {
    if( s1 == null ) {
      s1 = "";
    }
    return !s1.equals( s2 != null ? s2 : "" );
  }


  public void docChanged( DocumentEvent e )
  {
    if( e.getDocument() == this.docSpeed )
      setDataChanged();
  }


  private void informLAFChanged()
  {
    Frame[] frames = Frame.getFrames();
    if( frames != null ) {
      for( int i = 0; i< frames.length; i++ ) {
	Frame frm = frames[ i ];
	if( frm != null ) {
	  SwingUtilities.updateComponentTreeUI( frm );
	  if( frm instanceof BasicFrm ) {
	    ((BasicFrm) frm).lookAndFeelChanged();
	    if( !frm.isResizable() ) {
	      frm.pack();
	    }
	  }
	}
      }
    }
  }


  private void setDataChanged()
  {
    this.btnApply.setEnabled( true );
    if( this.btnSave != null )
      this.btnSave.setEnabled( false );
  }


  private void setSpeedValueFld( int khzValue )
  {
    this.fldSpeed.setText(
		this.fmtSpeed.format( (double) khzValue / 1000.0 ) );
  }


  public void updAC1FieldsEnabled()
  {
    boolean state = (this.btnAC1MonSCCH80.isSelected()
			|| this.btnAC1MonSCCH1088.isSelected());
    this.labelAC1RomdiskBegAddr.setEnabled( state );
    this.btnAC1Romdisk8000.setEnabled( state );
    this.btnAC1RomdiskC000.setEnabled( state );
  }


  private void updFields(
		Properties  props,
		LookAndFeel laf,
		String      screenRefreshMillis )
  {
    // System
    String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( sysName.startsWith( "BCS3" ) ) {
      this.btnSysBCS3.setSelected( true );
    }
    else if( sysName.startsWith( "C80" ) ) {
      this.btnSysC80.setSelected( true );
    }
    else if( sysName.startsWith( "HC900" ) ) {
      this.btnSysHC900.setSelected( true );
    }
    else if( sysName.startsWith( "HueblerEvertMC" ) ) {
      this.btnSysHEMC.setSelected( true );
    }
    else if( sysName.startsWith( "HueblerGraphicsMC" ) ) {
      this.btnSysHGMC.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/1" ) ) {
      this.btnSysKC85_1.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/2" ) ) {
      this.btnSysKC85_2.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/3" ) ) {
      this.btnSysKC85_3.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/4" ) ) {
      this.btnSysKC85_4.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/5" ) ) {
      this.btnSysKC85_5.setSelected( true );
    }
    else if( sysName.startsWith( "KC87" ) ) {
      this.btnSysKC87.setSelected( true );
    }
    else if( sysName.startsWith( "KramerMC" ) ) {
      this.btnSysKramerMC.setSelected( true );
    }
    else if( sysName.startsWith( "LC80" ) ) {
      this.btnSysLC80.setSelected( true );
    }
    else if( sysName.startsWith( "LLC1" ) ) {
      this.btnSysLLC1.setSelected( true );
    }
    else if( sysName.startsWith( "LLC2" ) ) {
      this.btnSysLLC2.setSelected( true );
    }
    else if( sysName.startsWith( "PCM" ) ) {
      this.btnSysPCM.setSelected( true );
    }
    else if( sysName.startsWith( "Poly880" ) ) {
      this.btnSysPoly880.setSelected( true );
    }
    else if( sysName.startsWith( "SC2" ) ) {
      this.btnSysSC2.setSelected( true );
    }
    else if( sysName.startsWith( "SLC1" ) ) {
      this.btnSysSLC1.setSelected( true );
    }
    else if( sysName.startsWith( "VCS80" ) ) {
      this.btnSysVCS80.setSelected( true );
    }
    else if( sysName.startsWith( "Z1013" ) ) {
      this.btnSysZ1013.setSelected( true );
    }
    else if( sysName.startsWith( "Z9001" ) ) {
      this.btnSysZ9001.setSelected( true );
    } else {
      this.btnSysAC1.setSelected( true );
    }


    // Optionen fuer AC1
    String ac1mon = EmuUtil.getProperty( props, "jkcemu.ac1.monitor" );
    if( ac1mon.equals( "3.1_64x16" ) ) {
      this.btnAC1Mon31_64x16.setSelected( true );
    } else if( ac1mon.equals( "SCCH8.0" ) ) {
      this.btnAC1MonSCCH80.setSelected( true );
    } else if( ac1mon.equals( "SCCH10/88" ) ) {
      this.btnAC1MonSCCH1088.setSelected( true );
    } else {
      this.btnAC1Mon31_64x32.setSelected( true );
    }
    if( EmuUtil.getProperty(
		props,
		"jkcemu.ac1.romdisk.address.begin" ).equals( "8000" ) )
    {
      this.btnAC1Romdisk8000.setSelected( true );
    } else {
      this.btnAC1RomdiskC000.setSelected( true );
    }
    updAC1FieldsEnabled();


    // Optionen fuer BCS3
    String bcs3Version = EmuUtil.getProperty(
				props,
				"jkcemu.bcs3.os.version" );
    if( bcs3Version.equals( "3.1" ) ) {
      if( EmuUtil.getProperty(
			props,
			"jkcemu.bcs3.chars_per_line" ).equals( "40" ) )
      {
	this.btnBCS3se31_40.setSelected( true );
      } else {
	this.btnBCS3se31_29.setSelected( true );
      }
    } else if( bcs3Version.equals( "3.3" ) ) {
      this.btnBCS3sp33_29.setSelected( true );
    } else {
      this.btnBCS3se24_27.setSelected( true );
    }
    if( EmuUtil.getProperty(
		props,
		"jkcemu.bcs3.ram.kbyte" ).equals( "17" ) )
    {
      this.btnBCS3Ram17k.setSelected( true );
    } else {
      this.btnBCS3Ram1k.setSelected( true );
    }


    // Optionen fuer Huebler/Evert-MC
    this.btnHEMCCatchPrintCalls.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.hemc.catch_print_calls",
				true ) );


    // Optionen fuer Huebler-Grafik-MC
    this.btnHGMCBasic.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.hgmc.basic",
				true ) );
    this.btnHGMCCatchPrintCalls.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.hgmc.catch_print_calls",
				true ) );


    // Optionen fuer Kramer-MC
    this.btnKramerMCCatchPrintCalls.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kramermc.catch_print_calls",
				false ) );


    // Optionen fuer KC85/2..5
    this.tableModelKC85Module.clear();
    if( props != null ) {
      int nRemain = EmuUtil.getIntProperty(
				props,
				kc85ModPropPrefix + "count",
				0 );
      int     slotNum = 8;
      boolean loop    = true;
      while( loop && (nRemain > 0) ) {
	String slotText = String.format( "%02X", slotNum );
	loop = addKC85Module(
			slotText,
			props.getProperty( kc85ModPropPrefix + slotText ) );
	--nRemain;
	slotNum += 4;
      }
    }
    this.btnKC85D004Enabled.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kc85.d004.enabled",
				false ) );
    try {
      String d004Rom = EmuUtil.getProperty( props, "jkcemu.kc85.d004.rom" );
      if( d004Rom.equals( "2.0" ) ) {
	this.comboKC85D004Rom.setSelectedIndex( 1 );
      } else if( d004Rom.equals( "3.2" ) ) {
	this.comboKC85D004Rom.setSelectedIndex( 2 );
      } else if( d004Rom.startsWith( "file:" ) ) {
	this.comboKC85D004Rom.setSelectedIndex( 3 );
	if( d004Rom.length() > 5 ) {
	  this.fldKC85D004RomFile.setText( d004Rom.substring( 5 ) );
	} else {
	  this.fldKC85D004RomFile.setText( "" );
	}
      } else {
	this.comboKC85D004Rom.setSelectedIndex( 0 );
      }
    }
    catch( IllegalArgumentException ex ) {}
    String d004Speed = EmuUtil.getProperty(
				props,
				"jkcemu.kc85.d004.maxspeed.khz" );
    if( d004Speed.equals( "8000" ) ) {
      this.btnKC85D004Speed8MHz.setSelected( true );
    } else if( d004Speed.equals( "16000" ) ) {
      this.btnKC85D004Speed16MHz.setSelected( true );
    } else {
      this.btnKC85D004Speed4MHz.setSelected( true );
    }
    updKC85D004FieldsEnabled();
    this.btnKC85PasteFast.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kc85.paste.fast",
				true ) );
    this.btnKC85VideoTiming.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kc85.emulate_video_timing",
				true ) );


    // Optionen fuer PC/M
    this.btnPCMAutoLoadBDOS.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.pcm.auto_load_bdos",
				true ) );


    // Optionen fuer LC80
    if( sysName.startsWith( "LC80_U505" ) ) {
      this.btnLC80_U505.setSelected( true );
    } else if( sysName.startsWith( "LC80.2" ) ) {
      this.btnLC80_2.setSelected( true );
    } else if( sysName.startsWith( "LC80e" ) ) {
      this.btnLC80e.setSelected( true );
    } else {
      this.btnLC80_2716.setSelected( true );
    }


    // Optionen fuer Z1013
    if( sysName.startsWith( "Z1013.01" ) ) {
      this.btnZ1013_01.setSelected( true );
    } else if( sysName.startsWith( "Z1013.12" ) ) {
      this.btnZ1013_12.setSelected( true );
    } else if( sysName.startsWith( "Z1013.16" ) ) {
      this.btnZ1013_16.setSelected( true );
    } else {
      this.btnZ1013_64.setSelected( true );
    }

    String monText = EmuUtil.getProperty( props, "jkcemu.z1013.monitor" );
    if( monText.equals( "A.2" ) ) {
      this.btnZ1013MonA2.setSelected( true );
    } else if( monText.equals( "RB_K7659" ) ) {
      this.btnZ1013MonRB_K7659.setSelected( true );
    } else if( monText.equals( "RB_S6009" ) ) {
      this.btnZ1013MonRB_S6009.setSelected( true );
    } else if( monText.equals( "JM_1992" ) ) {
      this.btnZ1013MonJM_1992.setSelected( true );
    } else {
      this.btnZ1013Mon202.setSelected( true );
    }
    this.btnZ1013Graphic.setSelected(
	EmuUtil.getBooleanProperty( props, "jkcemu.z1013.graphic", false ) );
    this.btnZ1013CatchPrintCalls.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			"jkcemu.z1013.catch_print_calls",
			true ) );
    this.btnZ1013PasteFast.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			"jkcemu.z1013.paste.fast",
			true ) );


    // Optionen fuer Z9001
    boolean z9001Color = EmuUtil.getBooleanProperty(
					props, "jkcemu.z9001.color", true );
    String z9001GraphType = EmuUtil.getProperty(
					props, "jkcemu.z9001.graphic.type" );
    if( z9001Color ) {
      if( z9001GraphType.equals( "krt" ) ) {
	this.btnZ9001ColorGraphKRT.setSelected( true );
      } else if( z9001GraphType.equals( "robotron" ) ) {
	this.btnZ9001ColorGraphRobotron.setSelected( true );
      } else {
	this.btnZ9001ColorGraphNone.setSelected( true );
      }
    } else {
      if( z9001GraphType.equals( "krt" ) ) {
	this.btnZ9001MonoGraphKRT.setSelected( true );
      } else {
	this.btnZ9001MonoGraphNone.setSelected( true );
      }
    }
    String ramText = EmuUtil.getProperty( props, "jkcemu.z9001.ram.kbyte" );
    if( ramText.equals( "32" ) ) {
      this.btnZ9001Ram32k.setSelected( true );
    } else if( ramText.equals( "48" ) ) {
      this.btnZ9001Ram48k.setSelected( true );
    } else if( ramText.equals( "74" ) ) {
      this.btnZ9001Ram74k.setSelected( true );
    } else {
      this.btnZ9001Ram16k.setSelected( true );
    }
    if( EmuUtil.getBooleanProperty(
			props,
			"jkcemu.z9001.printer_module.enabled",
			false ) )
    {
      this.btnZ9001PrinterModule.setSelected( true );
    } else if( EmuUtil.getBooleanProperty(
			props,
			"jkcemu.z9001.catch_print_calls",
			true ) )
    {
      this.btnZ9001CatchPrintCalls.setSelected( true );
    } else {
      this.btnZ9001NoPrinter.setSelected( true );
    }
    this.btnZ9001FloppyDisk.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			"jkcemu.z9001.floppy_disk.enabled",
			false ) );
    this.btnZ9001PasteFast.setSelected(
	EmuUtil.getBooleanProperty( props, "jkcemu.z9001.paste.fast", true ) );


    // Optionen anpassen
    updSysOptCard();


    // Geschwindigkeit
    int    defaultKHz = EmuThread.getDefaultSpeedKHz( props );
    String speedText  = EmuUtil.getProperty(
					props,
					"jkcemu.maxspeed.khz" ).toLowerCase();
    if( speedText.equals( "unlimited" ) ) {
      this.btnSpeedUnlimited.setSelected( true );
      setSpeedValueFld( defaultKHz );
    } else {
      boolean done = false;
      if( speedText.length() > 0 ) {
	try {
	  int value = Integer.parseInt( speedText );
	  if( (value > 0) && (value != defaultKHz) ) {
	    setSpeedValueFld( value );
	    this.btnSpeedValue.setSelected( true );
	    done = true;
	  }
	}
	catch( NumberFormatException ex ) {}
      }
      if( !done ) {
	setSpeedValueFld( defaultKHz );
	this.btnSpeedDefault.setSelected( true );
      }
    }
    updSpeedFieldsEnabled();


    // ROM
    String fileName = EmuUtil.getProperty(
				props,
				"jkcemu.program_x.file.name" );
    this.fldROMPrgXFile.setText( fileName );
    this.btnROMPrgXRemove.setEnabled( fileName.length() > 0 );

    fileName = EmuUtil.getProperty( props, "jkcemu.romdisk.file.name" );
    this.fldROMDiskFile.setText( fileName );
    this.btnROMDiskRemove.setEnabled( fileName.length() > 0 );


    // RAM-Floppies
    fileName = EmuUtil.getProperty( props, "jkcemu.ramfloppy.1.file.name" );
    this.fldRF1File.setText( fileName );
    this.btnRF1Remove.setEnabled( fileName.length() > 0 );

    fileName = EmuUtil.getProperty( props, "jkcemu.ramfloppy.2.file.name" );
    this.fldRF2File.setText( fileName );
    this.btnRF2Remove.setEnabled( fileName.length() > 0 );


    // Bildschirmausgabe
    int brightness = EmuUtil.getIntProperty(
					props,
					"jkcemu.brightness",
					DEFAULT_BRIGHTNESS );
    if( (brightness >= 0) && (brightness <= 100) ) {
      this.sliderBrightness.setValue( brightness );
    }
    try {
      int margin = EmuUtil.getIntProperty( props, "jkcemu.screen.margin", 20 );
      if( (margin >= 0) && (margin <= MAX_MARGIN) )
	this.spinnerModelMargin.setValue( new Integer( margin ) );
    }
    catch( IllegalArgumentException ex ) {}
    if( screenRefreshMillis == null ) {
      screenRefreshMillis = props.getProperty( "jkcemu.screen.refresh.ms" );
    }
    if( screenRefreshMillis != null ) {
      screenRefreshMillis = screenRefreshMillis.trim();
      if( screenRefreshMillis.length() > 0 )
	this.comboScreenRefresh.setSelectedItem( screenRefreshMillis );
    }
    this.btnDirectCopyPaste.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				"jkcemu.copy_and_paste.direct",
				true ) );


    // Bestaetigungen
    this.btnConfirmNMI.setSelected(
	EmuUtil.getBooleanProperty( props, "jkcemu.confirm.nmi", true ) );
    this.btnConfirmReset.setSelected(
	EmuUtil.getBooleanProperty( props, "jkcemu.confirm.reset", true ) );
    this.btnConfirmPowerOn.setSelected(
	EmuUtil.getBooleanProperty( props, "jkcemu.confirm.power_on", true ) );
    this.btnConfirmQuit.setSelected(
	EmuUtil.getBooleanProperty( props, "jkcemu.confirm.quit", true ) );


    // Erscheinungsbild
    String lafClassName = null;
    if( laf != null ) {
      lafClassName = laf.getClass().getName();
    }
    if( laf == null ) {
      lafClassName = props.getProperty( "jkcemu.lookandfeel.classname" );
    }
    if( lafClassName != null ) {
      AbstractButton btn = this.lafClass2Button.get( lafClassName );
      if( btn != null )
	btn.setSelected( true );
    }


    // sonstiges
    if( EmuUtil.getProperty(
		props,
		"jkcemu.filedialog" ).toLowerCase().equals( "native" ) )
    {
      this.btnFileDlgNative.setSelected( true );
    } else {
      this.btnFileDlgEmu.setSelected( true );
    }
    if( EmuUtil.getProperty(
		props,
		"jkcemu.sram.init" ).toLowerCase().startsWith( "r" ) )
    {
      this.btnSRAMInitRandom.setSelected( true );
    } else {
      this.btnSRAMInit00.setSelected( true );
    }
  }


  private void updKC85D004FieldsEnabled()
  {
    boolean state = this.btnKC85D004Enabled.isSelected();
    this.labelKC85D004Rom.setEnabled( state );
    this.comboKC85D004Rom.setEnabled( state );
    this.labelKC85D004Speed.setEnabled( state );
    this.btnKC85D004Speed4MHz.setEnabled( state );
    this.btnKC85D004Speed8MHz.setEnabled( state );
    this.btnKC85D004Speed16MHz.setEnabled( state );
    if( state && (this.comboKC85D004Rom.getSelectedIndex() != 3) ) {
      state = false;
    }
    this.fldKC85D004RomFile.setEnabled( state );
    this.btnKC85D004RomFileSelect.setEnabled( state );
    if( state ) {
      String fileName = this.fldKC85D004RomFile.getText();
      if( fileName != null ) {
	if( fileName.isEmpty() ) {
	  state = false;
	}
      } else {
	state = false;
      }
    }
    this.btnKC85D004RomFileRemove.setEnabled( state );
  }


  public void updSpeedFieldsEnabled()
  {
    boolean state = this.btnSpeedValue.isSelected();
    this.fldSpeed.setEnabled( state );
    this.labelSpeedUnit.setEnabled( state );
  }


  public void updSysOptCard()
  {
    String cardName = "noopt";
    if( this.btnSysAC1.isSelected() ) {
      cardName = "AC1";
    }
    else if( this.btnSysBCS3.isSelected() ) {
      cardName = "BCS3";
    }
    else if( this.btnSysHEMC.isSelected() ) {
      cardName = "HEMC";
    }
    else if( this.btnSysHGMC.isSelected() ) {
      cardName = "HGMC";
    }
    else if( this.btnSysHC900.isSelected()
	     || this.btnSysKC85_2.isSelected()
	     || this.btnSysKC85_3.isSelected()
	     || this.btnSysKC85_4.isSelected()
	     || this.btnSysKC85_5.isSelected() )
    {
      cardName = "KC85";
    }
    else if( this.btnSysKramerMC.isSelected() ) {
      cardName = "KramerMC";
    }
    else if( this.btnSysLC80.isSelected() ) {
      cardName = "LC80";
    }
    else if( this.btnSysPCM.isSelected() ) {
      cardName = "PCM";
    }
    else if( this.btnSysZ1013.isSelected() ) {
      cardName = "Z1013";
    }
    else if( this.btnSysKC85_1.isSelected()
	     || this.btnSysKC87.isSelected()
	     || this.btnSysZ9001.isSelected() )
    {
      cardName = "Z9001";
    }
    this.cardLayoutSysOpt.show( this.panelSysOpt, cardName );
    this.curSysOptCard = cardName;
  }
}

