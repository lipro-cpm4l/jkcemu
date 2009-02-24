/*
 * (c) 2008-2009 Jens Mueller
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
  public static final int DEFAULT_SCREEN_REFRESH_MS = 50;

  private static final int MAX_MARGIN = 199;

  private ScreenFrm                   screenFrm;
  private EmuThread                   emuThread;
  private File                        profileFile;
  private ExtFile                     extFont;
  private Vector<ExtROM>              extROMs;
  private Map<String,AbstractButton>  lafClass2Button;
  private SpinnerNumberModel          spinnerModelMargin;
  private JPanel                      panelEtc;
  private JPanel                      panelFont;
  private JPanel                      panelLAF;
  private JPanel                      panelROM;
  private JPanel                      panelRF;
  private JPanel                      panelSys;
  private JPanel                      panelSysOpt;
  private JCheckBox                   btnConfirmNMI;
  private JCheckBox                   btnConfirmReset;
  private JCheckBox                   btnConfirmPowerOn;
  private JCheckBox                   btnConfirmQuit;
  private JComboBox                   comboScreenRefresh;
  private JList                       listROM;
  private CardLayout                  cardLayoutSysOpt;
  private JRadioButton                btnSysAC1;
  private JRadioButton                btnSysBCS3;
  private JRadioButton                btnSysKC85_1;
  private JRadioButton                btnSysKC85_2;
  private JRadioButton                btnSysKC85_3;
  private JRadioButton                btnSysKC85_4;
  private JRadioButton                btnSysKC87;
  private JRadioButton                btnSysHEMC;
  private JRadioButton                btnSysKramerMC;
  private JRadioButton                btnSysZ1013;
  private JRadioButton                btnAC1mon31_64x16;
  private JRadioButton                btnAC1mon31_64x32;
  private JRadioButton                btnAC1monSCCH80;
  private JRadioButton                btnAC1monSCCH1088;
  private JRadioButton                btnBCS3se24_27;
  private JRadioButton                btnBCS3se31_29;
  private JRadioButton                btnBCS3se31_40;
  private JRadioButton                btnBCS3se32_29;
  private JRadioButton                btnBCS3ram1k;
  private JRadioButton                btnBCS3ram17k;
  private JCheckBox                   btnKC85VideoTiming;
  private JRadioButton                btnZ9001ram16k;
  private JRadioButton                btnZ9001ram32k;
  private JRadioButton                btnZ9001ram48k;
  private JCheckBox                   btnZ9001color;
  private JRadioButton                btnZ1013_01;
  private JRadioButton                btnZ1013_12;
  private JRadioButton                btnZ1013_16;
  private JRadioButton                btnZ1013_64;
  private JRadioButton                btnZ1013mon202;
  private JRadioButton                btnZ1013monA2;
  private JRadioButton                btnZ1013monRB_K7659;
  private JRadioButton                btnZ1013monRB_S6009;
  private JRadioButton                btnZ1013monJM_1992;
  private JRadioButton                btnSpeedDefault;
  private JRadioButton                btnSpeedValue;
  private JRadioButton                btnSpeedUnlimited;
  private JRadioButton                btnFileDlgEmu;
  private JRadioButton                btnFileDlgNative;
  private JPanel                      panelSpeed;
  private JLabel                      labelSpeedUnit;
  private JTextField                  fldSpeed;
  private Document                    docSpeed;
  private NumberFormat                fmtSpeed;
  private JButton                     btnExtFontSelect;
  private JButton                     btnExtFontRemove;
  private JTextField                  fldExtFontFile;
  private JButton                     btnRFAFileSelect;
  private JButton                     btnRFAFileRemove;
  private JTextField                  fldRFAFile;
  private JButton                     btnRFBFileSelect;
  private JButton                     btnRFBFileRemove;
  private JTextField                  fldRFBFile;
  private JTextField                  fldProfileDir;
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
    this.panelSys = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "System", this.panelSys );

    GridBagConstraints gbcSys = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpSys = new ButtonGroup();

    this.btnSysKC85_1 = new JRadioButton( "KC85/1 (Z9001)", false );
    this.btnSysKC85_1.addActionListener( this );
    grpSys.add( this.btnSysKC85_1 );
    this.panelSys.add( this.btnSysKC85_1, gbcSys );

    this.btnSysKC85_2 = new JRadioButton( "KC85/2 (HC900)", false );
    this.btnSysKC85_2.addActionListener( this );
    grpSys.add( this.btnSysKC85_2 );
    gbcSys.insets.top = 0;
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysKC85_2, gbcSys );

    this.btnSysKC85_3 = new JRadioButton( "KC85/3", false );
    this.btnSysKC85_3.addActionListener( this );
    grpSys.add( this.btnSysKC85_3 );
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysKC85_3, gbcSys );

    this.btnSysKC85_4 = new JRadioButton( "KC85/4", true );
    this.btnSysKC85_4.addActionListener( this );
    grpSys.add( this.btnSysKC85_4 );
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysKC85_4, gbcSys );

    this.btnSysKC87 = new JRadioButton( "KC87", false );
    this.btnSysKC87.addActionListener( this );
    grpSys.add( this.btnSysKC87 );
    gbcSys.insets.bottom = 5;
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysKC87, gbcSys );

    this.btnSysAC1 = new JRadioButton( "AC1", false );
    this.btnSysAC1.addActionListener( this );
    grpSys.add( this.btnSysAC1 );
    gbcSys.insets.top    = 5;
    gbcSys.insets.bottom = 0;
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysAC1, gbcSys );

    this.btnSysBCS3 = new JRadioButton( "BCS3", false );
    this.btnSysBCS3.addActionListener( this );
    grpSys.add( this.btnSysBCS3 );
    gbcSys.insets.top = 0;
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysBCS3, gbcSys );

    this.btnSysHEMC = new JRadioButton( "H\u00FCbler/Evert-MC", false );
    this.btnSysHEMC.addActionListener( this );
    grpSys.add( this.btnSysHEMC );
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysHEMC, gbcSys );

    this.btnSysKramerMC = new JRadioButton( "Kramer-MC", false );
    this.btnSysKramerMC.addActionListener( this );
    grpSys.add( this.btnSysKramerMC );
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysKramerMC, gbcSys );

    this.btnSysZ1013 = new JRadioButton( "Z1013", false );
    this.btnSysZ1013.addActionListener( this );
    grpSys.add( this.btnSysZ1013 );
    gbcSys.insets.bottom = 5;
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysZ1013, gbcSys );


    // Optionen
    this.cardLayoutSysOpt = new CardLayout( 5, 5);

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
    panelSys.add( this.panelSysOpt, gbcSys );


    JPanel panelNoOpt = new JPanel( new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    this.panelSysOpt.add( panelNoOpt, "noopt" );
    panelNoOpt.add( new JLabel( "Keine Optionen verf\u00FCgbar" ) );


    // Optionen fuer AC1
    JPanel panelAC1 = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelAC1, "AC1" );

    GridBagConstraints gbcAC1 = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpAC1mon = new ButtonGroup();

    panelAC1.add( new JLabel( "64x16 Zeichen, 1 KByte SRAM:" ), gbcAC1 );

    this.btnAC1mon31_64x16 = new JRadioButton(
		"Monitorprogramm 3.1 / Zeichensatz U402 BM513 (Ur-AC1)",
		false );
    this.btnAC1mon31_64x16.addActionListener( this );
    grpAC1mon.add( this.btnAC1mon31_64x16 );
    gbcAC1.insets.top  = 0;
    gbcAC1.insets.left = 50;
    gbcAC1.gridy++;
    panelAC1.add( this.btnAC1mon31_64x16, gbcAC1 );

    gbcAC1.insets.top  = 10;
    gbcAC1.insets.left = 5;
    gbcAC1.gridy++;
    panelAC1.add(
	new JLabel( "64x32 Zeichen, 2 KByte SRAM, 64 KByte DRAM:" ),
	gbcAC1 );

    this.btnAC1mon31_64x32 = new JRadioButton(
		"Monitorprogramm 3.1 / CC-Dessau-Zeichensatz",
		true );
    this.btnAC1mon31_64x32.addActionListener( this );
    grpAC1mon.add( this.btnAC1mon31_64x32 );
    gbcAC1.insets.top  = 0;
    gbcAC1.insets.left = 50;
    gbcAC1.gridy++;
    panelAC1.add( this.btnAC1mon31_64x32, gbcAC1 );

    this.btnAC1monSCCH80 = new JRadioButton(
		"SCCH-Monitorprogramm 8.0 / SCCH-Zeichensatz",
		false );
    this.btnAC1monSCCH80.addActionListener( this );
    grpAC1mon.add( this.btnAC1monSCCH80 );
    gbcAC1.gridy++;
    panelAC1.add( this.btnAC1monSCCH80, gbcAC1 );

    this.btnAC1monSCCH1088 = new JRadioButton(
		"SCCH-Monitorprogramm 10/88 / SCCH-Zeichensatz",
		false );
    this.btnAC1monSCCH1088.addActionListener( this );
    grpAC1mon.add( this.btnAC1monSCCH1088 );
    gbcAC1.insets.bottom = 5;
    gbcAC1.gridy++;
    panelAC1.add( this.btnAC1monSCCH1088, gbcAC1 );


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
		"2 KByte BASIC-SE 2.4 / 2,5 MHz / 27 Zeichen pro Zeile",
		true );
    this.btnBCS3se24_27.addActionListener( this );
    grpBCS3os.add( this.btnBCS3se24_27 );
    panelBCS3.add( this.btnBCS3se24_27, gbcBCS3 );

    this.btnBCS3se31_29 = new JRadioButton(
		"4 KByte BASIC-SE 3.1 / 2,5 MHz / 29 Zeichen pro Zeile",
		false );
    this.btnBCS3se31_29.addActionListener( this );
    grpBCS3os.add( this.btnBCS3se31_29 );
    gbcBCS3.insets.top = 0;
    gbcBCS3.gridy++;
    panelBCS3.add( this.btnBCS3se31_29, gbcBCS3 );

    this.btnBCS3se31_40 = new JRadioButton(
		"4 KByte BASIC-SE 3.1 / 3,5 MHz / 40 Zeichen pro Zeile",
		false );
    this.btnBCS3se31_40.addActionListener( this );
    grpBCS3os.add( this.btnBCS3se31_40 );
    gbcBCS3.insets.top = 0;
    gbcBCS3.gridy++;
    panelBCS3.add( this.btnBCS3se31_40, gbcBCS3 );

    this.btnBCS3se32_29 = new JRadioButton(
		"4 KByte S/P-BASIC V3.2 / 2,5 MHz / 29 Zeichen pro Zeile",
		false );
    this.btnBCS3se32_29.addActionListener( this );
    grpBCS3os.add( this.btnBCS3se32_29 );
    gbcBCS3.gridy++;
    panelBCS3.add( this.btnBCS3se32_29, gbcBCS3 );

    ButtonGroup grpBCS3ram = new ButtonGroup();

    this.btnBCS3ram1k = new JRadioButton( "1 KByte RAM", true );
    this.btnBCS3ram1k.addActionListener( this );
    grpBCS3ram.add( this.btnBCS3ram1k );
    gbcBCS3.insets.top = 10;
    gbcBCS3.gridy++;
    panelBCS3.add( this.btnBCS3ram1k, gbcBCS3 );

    this.btnBCS3ram17k = new JRadioButton(
				"17 KByte RAM (16 KByte RAM-Erweiterung)",
				false );
    this.btnBCS3ram17k.addActionListener( this );
    grpBCS3ram.add( this.btnBCS3ram17k );
    gbcBCS3.insets.top    = 0;
    gbcBCS3.insets.bottom = 5;
    gbcBCS3.gridy++;
    panelBCS3.add( this.btnBCS3ram17k, gbcBCS3 );


    // Optionen fuer KC85/2..4
    JPanel panelKC85 = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelKC85, "KC85" );

    GridBagConstraints gbcKC85 = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelKC85.add(
	new JLabel( "Die folgende Option ben\u00F6tigt relativ viel"
						+ " Rechenleistung." ),
	gbcKC85 );

    gbcKC85.insets.top = 0;
    gbcKC85.gridy++;
    panelKC85.add(
	new JLabel( "Sollte diese Leistung zur Verf\u00FCgung stehen," ),
	gbcKC85 );

    gbcKC85.gridy++;
    panelKC85.add(
	new JLabel( "dann schalten Sie die Option bitte aus." ),
	gbcKC85 );

    this.btnKC85VideoTiming = new JCheckBox(
			"Zeitverhalten der Bildschirmsteuerung emulieren",
			true );
    this.btnKC85VideoTiming.addActionListener( this );
    gbcKC85.insets.top    = 5;
    gbcKC85.insets.bottom = 5;
    gbcKC85.gridy++;
    panelKC85.add( this.btnKC85VideoTiming, gbcKC85 );


    // Optionen fuer Z1013
    JPanel panelZ1013 = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelZ1013, "Z1013" );

    GridBagConstraints gbcZ1013 = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpZ1013hw = new ButtonGroup();

    this.btnZ1013_01 = new JRadioButton(
				"Z1013.01 / 1 MHz / 16 KByte RAM",
				false );
    this.btnZ1013_01.addActionListener( this );
    grpZ1013hw.add( this.btnZ1013_01 );
    panelZ1013.add( this.btnZ1013_01, gbcZ1013 );

    this.btnZ1013_12 = new JRadioButton(
				"Z1013.12 / 2 MHz / 1 KByte RAM",
				false );
    this.btnZ1013_12.addActionListener( this );
    grpZ1013hw.add( this.btnZ1013_12 );
    gbcZ1013.insets.top = 0;
    gbcZ1013.gridy++;
    panelZ1013.add( this.btnZ1013_12, gbcZ1013 );

    this.btnZ1013_16 = new JRadioButton(
				"Z1013.16 / 2 MHz / 16 KByte RAM",
				false );
    this.btnZ1013_16.addActionListener( this );
    grpZ1013hw.add( this.btnZ1013_16 );
    gbcZ1013.gridy++;
    panelZ1013.add( this.btnZ1013_16, gbcZ1013 );

    this.btnZ1013_64 = new JRadioButton(
				"Z1013.64 / 2 MHz / 64 KByte RAM",
				false );
    this.btnZ1013_64.addActionListener( this );
    grpZ1013hw.add( this.btnZ1013_64 );
    gbcZ1013.gridy++;
    panelZ1013.add( this.btnZ1013_64, gbcZ1013 );

    ButtonGroup grpZ1013mon = new ButtonGroup();

    this.btnZ1013mon202 = new JRadioButton(
		"Monitorprogramm 2.02 / Folienflachtastatur",
		true );
    this.btnZ1013mon202.addActionListener( this );
    grpZ1013mon.add( this.btnZ1013mon202 );
    gbcZ1013.insets.top = 10;
    gbcZ1013.gridy++;
    panelZ1013.add( this.btnZ1013mon202, gbcZ1013 );

    this.btnZ1013monA2 = new JRadioButton(
		"Monitorprogramm A.2 / Alpha-Tastatur",
		false );
    this.btnZ1013monA2.addActionListener( this );
    grpZ1013mon.add( this.btnZ1013monA2 );
    gbcZ1013.insets.top = 0;
    gbcZ1013.gridy++;
    panelZ1013.add( this.btnZ1013monA2, gbcZ1013 );

    this.btnZ1013monRB_K7659 = new JRadioButton(
		"Brosig-Monitorprogramm 2.028 / Tastatur K7659",
		false );
    this.btnZ1013monRB_K7659.addActionListener( this );
    grpZ1013mon.add( this.btnZ1013monRB_K7659 );
    gbcZ1013.gridy++;
    panelZ1013.add( this.btnZ1013monRB_K7659, gbcZ1013 );

    this.btnZ1013monRB_S6009 = new JRadioButton(
		"Brosig-Monitorprogramm 2.028 / Tastatur S6009",
		false );
    this.btnZ1013monRB_S6009.addActionListener( this );
    grpZ1013mon.add( this.btnZ1013monRB_S6009 );
    gbcZ1013.gridy++;
    panelZ1013.add( this.btnZ1013monRB_S6009, gbcZ1013 );

    this.btnZ1013monJM_1992 = new JRadioButton(
		"M\u00FCller-Monitorprogramm 1992 / Folienflachtastatur",
		false );
    this.btnZ1013monJM_1992.addActionListener( this );
    grpZ1013mon.add( this.btnZ1013monJM_1992 );
    gbcZ1013.insets.bottom = 5;
    gbcZ1013.gridy++;
    panelZ1013.add( this.btnZ1013monJM_1992, gbcZ1013 );


    // Optionen fuer Z9001
    JPanel panelZ9001 = new JPanel( new GridBagLayout() );
    this.panelSysOpt.add( panelZ9001, "Z9001" );

    GridBagConstraints gbcZ9001 = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpZ9001ram = new ButtonGroup();

    this.btnZ9001ram16k = new JRadioButton( "16 KByte RAM", true );
    this.btnZ9001ram16k.addActionListener( this );
    grpZ9001ram.add( this.btnZ9001ram16k );
    panelZ9001.add( this.btnZ9001ram16k, gbcZ9001 );

    this.btnZ9001ram32k = new JRadioButton(
			"32 KByte RAM (1 RAM-Modul gesteckt)", false );
    this.btnZ9001ram32k.addActionListener( this );
    grpZ9001ram.add( this.btnZ9001ram32k );
    gbcZ9001.insets.top = 0;
    gbcZ9001.gridy++;
    panelZ9001.add( this.btnZ9001ram32k, gbcZ9001 );

    this.btnZ9001ram48k = new JRadioButton(
			"48 KByte RAM (2 RAM-Module gesteckt)", false );
    this.btnZ9001ram48k.addActionListener( this );
    grpZ9001ram.add( this.btnZ9001ram48k );
    gbcZ9001.insets.bottom = 5;
    gbcZ9001.gridy++;
    panelZ9001.add( this.btnZ9001ram48k, gbcZ9001 );

    this.btnZ9001color = new JCheckBox( "Farbmodul", true );
    this.btnZ9001color.addActionListener( this );
    gbcZ9001.insets.top = 5;
    gbcZ9001.gridy++;
    panelZ9001.add( this.btnZ9001color, gbcZ9001 );


    // Bereich Geschwindigkeit
    this.panelSpeed = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Geschwindigkeit", this.panelSpeed );

    GridBagConstraints gbcSpeed = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.panelSpeed.add(
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
    this.panelSpeed.add( this.btnSpeedDefault, gbcSpeed );

    this.btnSpeedValue = new JRadioButton(
				"Begrenzen auf folgenden Wert:",
				false );
    this.btnSpeedValue.addActionListener( this );
    grpSpeed.add( this.btnSpeedValue );
    gbcSpeed.gridwidth = 1;
    gbcSpeed.gridy++;
    this.panelSpeed.add( this.btnSpeedValue, gbcSpeed );

    this.fldSpeed = new JTextField( 5 );
    this.docSpeed = this.fldSpeed.getDocument();
    if( this.docSpeed != null ) {
      this.docSpeed.addDocumentListener( this );
    }
    gbcSpeed.insets.left = 5;
    gbcSpeed.gridx++;
    this.panelSpeed.add( this.fldSpeed, gbcSpeed );

    this.labelSpeedUnit = new JLabel( "MHz" );
    gbcSpeed.insets.left = 5;
    gbcSpeed.gridx++;
    this.panelSpeed.add( this.labelSpeedUnit, gbcSpeed );

    this.btnSpeedUnlimited = new JRadioButton( "Nicht begrenzen", false );
    this.btnSpeedUnlimited.addActionListener( this );
    grpSpeed.add( this.btnSpeedUnlimited );
    gbcSpeed.insets.left   = 50;
    gbcSpeed.insets.bottom = 5;
    gbcSpeed.gridwidth     = GridBagConstraints.REMAINDER;
    gbcSpeed.gridx         = 0;
    gbcSpeed.gridy++;
    this.panelSpeed.add( this.btnSpeedUnlimited, gbcSpeed );


    // Bereich Zeichensatz
    this.panelFont = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Zeichensatz", this.panelFont );

    GridBagConstraints gbcFont = new GridBagConstraints(
						0, 0,
						3, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.panelFont.add(
	new JLabel( "Sie k\u00F6nnen hier eine Zeichensatzdatei angeben," ),
	gbcFont );

    gbcFont.insets.top = 0;
    gbcFont.gridy++;
    this.panelFont.add(
	new JLabel( "die anstelle der im Emulator integrierten"
			+ " Zeichens\u00E4tze verwendet werden soll." ),
	gbcFont );

    gbcFont.insets.top = 10;
    gbcFont.gridy++;
    this.panelFont.add(
	new JLabel( "Diese Funktion hat jedoch keine Wirkung bei"
				+ " KC85/2...4," ),
	gbcFont );

    gbcFont.insets.top = 0;
    gbcFont.gridy++;
    this.panelFont.add(
	new JLabel( "da dort der Zeichensatz im Betriebssystem"
				+ " enthalten ist." ),
	gbcFont );

    gbcFont.insets.top = 10;
    gbcFont.gridy++;
    this.panelFont.add( new JLabel( "Name der Zeichensatzdatei:" ), gbcFont );

    this.fldExtFontFile = new JTextField();
    this.fldExtFontFile.setEditable( false );
    gbcFont.fill          = GridBagConstraints.HORIZONTAL;
    gbcFont.weightx       = 1.0;
    gbcFont.insets.top    = 0;
    gbcFont.insets.bottom = 5;
    gbcFont.gridwidth     = 1;
    gbcFont.gridy++;
    this.panelFont.add( this.fldExtFontFile, gbcFont );

    this.btnExtFontSelect = createImageButton(
					"/images/file/open.png",
					"Ausw\u00E4hlen" );
    gbcFont.fill    = GridBagConstraints.NONE;
    gbcFont.weightx = 0.0;
    gbcFont.gridx++;
    this.panelFont.add( this.btnExtFontSelect, gbcFont );

    this.btnExtFontRemove = createImageButton(
					"/images/file/delete.png",
					"Entfernen" );
    gbcFont.gridx++;
    this.panelFont.add( this.btnExtFontRemove, gbcFont );


    // Bereich ROM
    this.panelROM = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "ROM", this.panelROM );

    GridBagConstraints gbcROM = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.panelROM.add( new JLabel( "Eingebundene ROM-Images:" ), gbcROM );

    this.listROM = new JList();
    this.listROM.setDragEnabled( false );
    this.listROM.setLayoutOrientation( JList.VERTICAL );
    this.listROM.addListSelectionListener( this );
    gbcROM.fill          = GridBagConstraints.BOTH;
    gbcROM.weightx       = 1.0;
    gbcROM.weighty       = 1.0;
    gbcROM.insets.top    = 0;
    gbcROM.insets.right  = 0;
    gbcROM.insets.bottom = 5;
    gbcROM.gridy++;
    this.panelROM.add( new JScrollPane( this.listROM ), gbcROM );

    JPanel panelROMBtn = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
    gbcROM.fill         = GridBagConstraints.NONE;
    gbcROM.weightx      = 0.0;
    gbcROM.weighty      = 0.0;
    gbcROM.insets.right = 5;
    gbcROM.gridheight   = 1;
    gbcROM.gridx++;
    this.panelROM.add( panelROMBtn, gbcROM );

    this.btnROMAdd = createImageButton(
				"/images/file/open.png",
				"Hinzuf\u00FCgen" );
    panelROMBtn.add( this.btnROMAdd );

    this.btnROMRemove = createImageButton(
				"/images/file/delete.png",
				"Entfernen" );
    this.btnROMRemove.setEnabled( false );
    panelROMBtn.add( this.btnROMRemove );


    // Bereich RAM-Floppy
    this.panelRF = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "RAM-Floppies", this.panelRF );

    GridBagConstraints gbcRF = new GridBagConstraints(
						0, 0,
						3, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.panelRF.add(
	new JLabel( "Sie k\u00F6nnen hier Dateien angeben,"
		+ " mit denen die RAM-Floppies initialisiert werden." ),
	gbcRF );

    gbcRF.insets.top = 0;
    gbcRF.gridy++;
    this.panelRF.add(
	new JLabel( "Die jeweilige Datei wird automatisch"
		+ " in die RAM-Floppy geladen." ),
	gbcRF );

    gbcRF.insets.top = 20;
    gbcRF.gridy++;
    this.panelRF.add(
	new JLabel( "Name der Initialisierungsdatei f\u00FCr RAM-Floppy A:" ),
	gbcRF );

    this.fldRFAFile = new JTextField();
    this.fldRFAFile.setEditable( false );
    gbcRF.fill       = GridBagConstraints.HORIZONTAL;
    gbcRF.weightx    = 1.0;
    gbcRF.insets.top = 0;
    gbcRF.gridwidth  = 1;
    gbcRF.gridy++;
    this.panelRF.add( this.fldRFAFile, gbcRF );

    this.btnRFAFileSelect = createImageButton(
					"/images/file/open.png",
					"Ausw\u00E4hlen" );
    gbcRF.fill    = GridBagConstraints.NONE;
    gbcRF.weightx = 0.0;
    gbcRF.gridx++;
    this.panelRF.add( this.btnRFAFileSelect, gbcRF );

    this.btnRFAFileRemove = createImageButton(
					"/images/file/delete.png",
					"Entfernen" );
    this.btnRFAFileRemove.setEnabled( false );
    gbcRF.gridx++;
    this.panelRF.add( this.btnRFAFileRemove, gbcRF );

    gbcRF.insets.top = 20;
    gbcRF.gridwidth  = 3;
    gbcRF.gridx      = 0;
    gbcRF.gridy++;
    this.panelRF.add(
	new JLabel( "Name der Initialisierungsdatei f\u00FCr RAM-Floppy B:" ),
	gbcRF );

    this.fldRFBFile = new JTextField();
    this.fldRFBFile.setEditable( false );
    gbcRF.fill          = GridBagConstraints.HORIZONTAL;
    gbcRF.weightx       = 1.0;
    gbcRF.insets.top    = 0;
    gbcRF.insets.bottom = 5;
    gbcRF.gridwidth     = 1;
    gbcRF.gridy++;
    this.panelRF.add( this.fldRFBFile, gbcRF );

    this.btnRFBFileSelect = createImageButton(
					"/images/file/open.png",
					"Ausw\u00E4hlen" );
    gbcRF.fill    = GridBagConstraints.NONE;
    gbcRF.weightx = 0.0;
    gbcRF.gridx++;
    this.panelRF.add( this.btnRFBFileSelect, gbcRF );

    this.btnRFBFileRemove = createImageButton(
					"/images/file/delete.png",
					"Entfernen" );
    this.btnRFBFileRemove.setEnabled( false );
    gbcRF.gridx++;
    this.panelRF.add( this.btnRFBFileRemove, gbcRF );


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
    this.panelLAF = null;
    this.grpLAF   = new ButtonGroup();
    this.lafs     = UIManager.getInstalledLookAndFeels();
    if( this.lafs != null ) {
      if( this.lafs.length > 1 ) {
	this.panelLAF = new JPanel( new GridBagLayout() );
	this.tabbedPane.addTab( "Erscheinungsbild", this.panelLAF );

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
	    this.panelLAF.add( btn, gbcLAF );
	    gbcLAF.insets.top = 0;
	    gbcLAF.gridy++;
	    this.lafClass2Button.put( clName, btn );
	  }
	}
      }
    }


    // Bereich Sonstiges
    this.panelEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.panelEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.panelEtc.add( new JLabel( "Dateiauswahldialog:" ), gbcEtc );

    ButtonGroup grpFileDlg = new ButtonGroup();

    this.btnFileDlgEmu = new JRadioButton(
				"JKCEMU-Dateiauswahldialog verwenden",
				true );
    grpFileDlg.add( this.btnFileDlgEmu );
    this.btnFileDlgEmu.addActionListener( this );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    this.panelEtc.add( this.btnFileDlgEmu, gbcEtc );

    this.btnFileDlgNative = new JRadioButton(
			"Dateiauswahlbox des Betriebssystems verwenden",
			false );
    grpFileDlg.add( this.btnFileDlgNative );
    this.btnFileDlgNative.addActionListener( this );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.panelEtc.add( this.btnFileDlgNative, gbcEtc );

    gbcEtc.insets.top  = 5;
    gbcEtc.insets.left = 5;
    gbcEtc.gridwidth   = 1;
    gbcEtc.gridy++;
    this.panelEtc.add( new JLabel( "Rand um Bildschirmausgabe:" ), gbcEtc );

    this.spinnerModelMargin = new SpinnerNumberModel( 20, 0, MAX_MARGIN, 1 );
    this.spinnerMargin = new JSpinner( this.spinnerModelMargin );
    this.spinnerMargin.addChangeListener( this );

    gbcEtc.fill = GridBagConstraints.HORIZONTAL;
    gbcEtc.gridx++;
    this.panelEtc.add( this.spinnerMargin, gbcEtc );

    gbcEtc.fill = GridBagConstraints.NONE;
    gbcEtc.gridx++;
    this.panelEtc.add( new JLabel( "Pixel" ), gbcEtc );

    gbcEtc.gridx = 0;
    gbcEtc.gridy++;
    this.panelEtc.add(
		new JLabel( "Zykluszeit f\u00FCr Bildschirmaktualisierung:" ),
		gbcEtc );

    this.comboScreenRefresh = new JComboBox();
    this.comboScreenRefresh.setEditable( false );
    this.comboScreenRefresh.addItem( "10" );
    this.comboScreenRefresh.addItem( "20" );
    this.comboScreenRefresh.addItem( "30" );
    this.comboScreenRefresh.addItem( "50" );
    this.comboScreenRefresh.addItem( "100" );
    this.comboScreenRefresh.addItem( "200" );
    this.comboScreenRefresh.addActionListener( this );
    gbcEtc.anchor = GridBagConstraints.WEST;
    gbcEtc.fill   = GridBagConstraints.HORIZONTAL;
    gbcEtc.gridx++;
    this.panelEtc.add( this.comboScreenRefresh, gbcEtc );

    gbcEtc.fill = GridBagConstraints.NONE;
    gbcEtc.gridx++;
    this.panelEtc.add( new JLabel( "ms" ), gbcEtc );

    Font font       = null;
    File profileDir = Main.getProfileDir();
    if( profileDir != null ) {
      gbcEtc.insets.bottom = 0;
      gbcEtc.gridwidth     = GridBagConstraints.REMAINDER;
      gbcEtc.gridx         = 0;
      gbcEtc.gridy++;
      this.panelEtc.add(
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
      this.panelEtc.add( this.fldProfileDir, gbcEtc );

      font = this.fldProfileDir.getFont();
    }

    // gleicher Font von JSpinner und JComboBox
    if( font != null ) {
      this.spinnerMargin.setFont( font );
    } else {
      font = this.spinnerMargin.getFont();
    }
    if( font != null ) {
      this.comboScreenRefresh.setFont( font );
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
    if( e.getSource() == this.spinnerMargin )
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
    this.btnROMRemove.setEnabled( this.listROM.getSelectedIndex() >= 0 );
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
		 || (src == this.btnSysHEMC)
		 || (src == this.btnSysKC85_1)
		 || (src == this.btnSysKC85_2)
		 || (src == this.btnSysKC85_3)
		 || (src == this.btnSysKC85_4)
		 || (src == this.btnSysKC87)
		 || (src == this.btnSysKramerMC)
		 || (src == this.btnSysZ1013) )
	{
	  rv = true;
	  updSysOptCard();
	  setDataChanged();
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
	else if( src == this.btnRFAFileSelect ) {
	  rv = true;
	  doRAMFloppyFileSelect( this.fldRFAFile, this.btnRFAFileRemove );
	}
	else if( src == this.btnRFAFileRemove ) {
	  rv = true;
	  doRAMFloppyFileRemove( this.fldRFAFile, this.btnRFAFileRemove );
	}
	else if( src == this.btnRFBFileSelect ) {
	  rv = true;
	  doRAMFloppyFileSelect( this.fldRFBFile, this.btnRFBFileRemove );
	}
	else if( src == this.btnRFBFileRemove ) {
	  rv = true;
	  doRAMFloppyFileRemove( this.fldRFBFile, this.btnRFBFileRemove );
	}
	else if( (src instanceof JCheckBox)
		 || (src instanceof JComboBox)
		 || (src instanceof JRadioButton) )
	{
	  rv = true;
	  setDataChanged();
	}
      }
    }
    return rv;
  }


  public void lookAndFeelChanged()
  {
    pack();
  }


	/* --- Aktionen --- */

  private void doApply()
  {
    Properties props = new Properties();
    boolean    state = applySpeed( props );
    if( state ) {
      state = applyROM( props );
    }
    if( state ) {
      state = applyLAF( props );
    }
    if( state ) {
      applyEtc( props );
      applyFont( props );
      applyRF( props );
      applySys( props );

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
       * pruefen, ob ein RESET erforderlich ist,
       * Dazu die ROMs vergleichen,
       * falls diese den Betriebssystembereich ueberdecken
       */
      boolean forceReset = false;
      if( !forceReset ) {
	int nNewExtROMs   = 0;
	int newExtROMAddr = -1;
	if( newExtROMs != null ) {
	  nNewExtROMs = newExtROMs.length;
	  if( nNewExtROMs > 0 )
	    newExtROMAddr = newExtROMs[ 0 ].getBegAddress();
	}
	ExtROM[] oldExtROMs    = this.emuThread.getExtROMs();
	int      nOldExtROMs   = 0;
	int      oldExtROMAddr = -1;
	if( oldExtROMs != null ) {
	  nOldExtROMs = oldExtROMs.length;
	  if( nOldExtROMs > 0 )
	    oldExtROMAddr = oldExtROMs[ 0 ].getBegAddress();
	}
	EmuSys emuSys = this.emuThread.getEmuSys();
	if( emuSys != null ) {
	  if( (nNewExtROMs > 0) || (nOldExtROMs > 0) ) {
	    if( ((newExtROMAddr >= emuSys.getMinOSAddress())
			&& (newExtROMAddr <= emuSys.getMaxOSAddress()))
		|| ((oldExtROMAddr >= emuSys.getMinOSAddress())
			&& (oldExtROMAddr <= emuSys.getMaxOSAddress())) )
	    {
	      if( nNewExtROMs == nOldExtROMs ) {
		if( (newExtROMs != null) && (oldExtROMs != null) ) {
		  for( int i = 0; i < newExtROMs.length; i++ ) {
		    if( !newExtROMs[ i ].equals( oldExtROMs[ i ] ) ) {
		      forceReset = true;
		      break;
		    }
		  }
		} else {
		  forceReset = true;
		}
	      } else {
		forceReset = true;
	      }
	    }
	  }
	} else {
	  forceReset = true;
	}
      }

      // zuerst neue Eigenschaften setzen
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
      if( this.btnSave != null )
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
      if( oldFileName.length() < 1 )
	oldFileName = null;
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


  private void doRAMFloppyFileSelect( JTextField fldFile, JButton btnRemove )
  {
    String oldFileName = fldFile.getText();
    if( oldFileName != null ) {
      if( oldFileName.length() < 1 )
	oldFileName = null;
    }
    File file = EmuUtil.showFileOpenDlg(
				this,
				"RAM-Floppy laden",
				oldFileName != null ?
					new File( oldFileName )
					: Main.getLastPathFile( "ramfloppy" ),
				EmuUtil.getBinaryFileFilter() );
    if( file != null ) {
      if( file.exists() ) {
	if( file.canRead() ) {
	  fldFile.setText( file.getPath() );
	  btnRemove.setEnabled( true );
	  setDataChanged();
	  Main.setLastFile( file, "ramfloppy" );
	} else {
	  BasicDlg.showErrorDlg(
			this,
			file.getPath() + ": Datei nicht lesbar" );
	}
      } else {
	BasicDlg.showErrorDlg(
			this,
			file.getPath() + ": Datei nicht gefunden" );
      }
    }
  }


  private void doRAMFloppyFileRemove( JTextField fldFile, JButton btnRemove )
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
    int idx = this.listROM.getSelectedIndex();
    if( (idx >= 0) && (idx < this.extROMs.size()) ) {
      this.extROMs.remove( idx );
      this.listROM.setListData( this.extROMs );
      setDataChanged();
    }
  }


	/* --- private Methoden --- */

  private void applyEtc( Properties props )
  {
    props.setProperty(
		"jkcemu.filedialog",
		this.btnFileDlgNative.isSelected() ? "native" : "jkcemu" );

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
  }


  private void applyFont( Properties props )
  {
    props.setProperty(
		"jkcemu.font.file.name",
		this.extFont != null ? this.extFont.getFile().getPath() : "" );
  }


  private boolean applyLAF( Properties props )
  {
    boolean     rv = true;
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
	      rv = false;
	      if( this.panelLAF != null ) {
		this.tabbedPane.setSelectedComponent( this.panelLAF );
	      }
	      BasicDlg.showErrorDlg(
			this,
			"Das Erscheinungsbild kann nicht"
				+ " eingestellt werden." );
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void applyRF( Properties props )
  {
    String fileName = this.fldRFAFile.getText();
    props.setProperty(
		"jkcemu.ramfloppy.a.file.name",
		fileName != null ? fileName : "" );

    fileName = this.fldRFBFile.getText();
    props.setProperty(
		"jkcemu.ramfloppy.b.file.name",
		fileName != null ? fileName : "" );
  }


  private boolean applyROM( Properties props )
  {
    boolean rv = true;
    int     n  = 0;
    int     a  = -1;
    for( ExtROM rom : this.extROMs ) {
      n++;
      int begAddr = rom.getBegAddress();
      if( begAddr <= a ) {
	this.tabbedPane.setSelectedComponent( this.panelROM );
	BasicDlg.showErrorDlg(
		this,
		String.format(
			"ROM an Adresse %04X \u00FCberschneidet sich"
				+ " mit vorherigem ROM.",
			begAddr ) );
	rv = false;
	break;
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
    return rv;
  }


  private boolean applySpeed( Properties props )
  {
    boolean rv = false;
    if( this.btnSpeedValue.isSelected() ) {
      String msg  = "Sie m\u00FCssen einen Wert f\u00FCr"
				+ " die max. Geschwindigkeit eingeben.";
      String text = this.fldSpeed.getText();
      if( text != null ) {
	if( text.length() > 0 ) {
	  msg = "Die eingegebene max. Geschwindigkeit ist ung\u00FCltig.";
	  try {
	    Number mhzValue = this.fmtSpeed.parse( text );
	    if( mhzValue != null ) {
	      long khzValue = Math.round( mhzValue.doubleValue() * 1000.0 );
	      if( khzValue > 0 ) {
		props.setProperty(
				"jkcemu.maxspeed.khz",
				String.valueOf( khzValue ) );
		rv = true;
	      }
	    }
	  }
	  catch( ParseException ex ) {}
	}
      }
      if( !rv ) {
	this.tabbedPane.setSelectedComponent( this.panelSpeed );
	BasicDlg.showErrorDlg( this, msg );
      }
    } else if( this.btnSpeedUnlimited.isSelected() ) {
      props.setProperty( "jkcemu.maxspeed.khz", "unlimited" );
      rv = true;
    } else {
      props.setProperty( "jkcemu.maxspeed.khz", "default" );
      rv = true;
    }
    return rv;
  }


  private void applySys( Properties props )
  {
    // System
    String valueSys = "";
    if( this.btnSysAC1.isSelected() ) {
      valueSys = "AC1";
    }
    else if( this.btnSysBCS3.isSelected() ) {
      valueSys = "BCS3";
    }
    else if( this.btnSysHEMC.isSelected() ) {
      valueSys = "HueblerEvertMC";
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
    else if( this.btnSysKC87.isSelected() ) {
      valueSys = "KC87";
    }
    else if( this.btnSysKramerMC.isSelected() ) {
      valueSys = "KramerMC";
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
    props.setProperty( "jkcemu.system", valueSys );


    // Optionen fuer AC1
    String ac1mon = "3.1_64x32";
    if( this.btnAC1mon31_64x16.isSelected() ) {
      ac1mon = "3.1_64x16";
    }
    else if( this.btnAC1monSCCH80.isSelected() ) {
      ac1mon = "SCCH8.0";
    }
    else if( this.btnAC1monSCCH1088.isSelected() ) {
      ac1mon = "SCCH10/88";
    }
    props.setProperty( "jkcemu.ac1.monitor", ac1mon );


    // Optionen fuer BCS3
    if( this.btnBCS3se31_29.isSelected() ) {
      props.setProperty( "jkcemu.bcs3.os.version", "3.1" );
      props.setProperty( "jkcemu.bcs3.chars_per_line", "29" );
    } else if( this.btnBCS3se31_40.isSelected() ) {
      props.setProperty( "jkcemu.bcs3.os.version", "3.1" );
      props.setProperty( "jkcemu.bcs3.chars_per_line", "40" );
    } else if( this.btnBCS3se32_29.isSelected() ) {
      props.setProperty( "jkcemu.bcs3.os.version", "3.2" );
      props.setProperty( "jkcemu.bcs3.chars_per_line", "29" );
    } else {
      props.setProperty( "jkcemu.bcs3.os.version", "2.4" );
      props.setProperty( "jkcemu.bcs3.chars_per_line", "27" );
    }
    if( this.btnBCS3ram17k.isSelected() ) {
      props.setProperty( "jkcemu.bcs3.ram.kbyte", "17" );
    } else {
      props.setProperty( "jkcemu.bcs3.ram.kbyte", "1" );
    }


    // Optionen fuer KC85/2..4
    props.setProperty(
		"jkcemu.kc85.emulate_video_timing",
		Boolean.toString( this.btnKC85VideoTiming.isSelected() ) );


    // Optionen fuer Z1013
    String z1013mon = "2.02";
    if( this.btnZ1013monA2.isSelected() ) {
      z1013mon = "A.2";
    }
    else if( this.btnZ1013monRB_K7659.isSelected() ) {
      z1013mon = "RB_K7659";
    }
    else if( this.btnZ1013monRB_S6009.isSelected() ) {
      z1013mon = "RB_S6009";
    }
    else if( this.btnZ1013monJM_1992.isSelected() ) {
      z1013mon = "JM_1992";
    }
    props.setProperty( "jkcemu.z1013.monitor", z1013mon );


    // Optionen fuer Z9001
    String z9001ram = "16";
    if( this.btnZ9001ram32k.isSelected() ) {
      z9001ram = "32";
    }
    else if( this.btnZ9001ram48k.isSelected() ) {
      z9001ram = "48";
    }
    props.setProperty( "jkcemu.z9001.ram.kbyte", z9001ram );
    props.setProperty(
		"jkcemu.z9001.color",
		Boolean.toString( this.btnZ9001color.isSelected() ) );
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
	  if( frm instanceof BasicFrm )
	    ((BasicFrm) frm).lookAndFeelChanged();
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


  private void updFields(
		Properties  props,
		LookAndFeel laf,
		String      screenRefreshMillis )
  {
    // System
    String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( sysName.startsWith( "KC85/2" ) || sysName.startsWith( "HC900" ) ) {
      this.btnSysKC85_2.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/3" ) ) {
      this.btnSysKC85_3.setSelected( true );
    }
    else if( sysName.startsWith( "KC85/4" ) ) {
      this.btnSysKC85_4.setSelected( true );
    }
    else if( sysName.startsWith( "KC87" ) ) {
      this.btnSysKC87.setSelected( true );
    }
    else if( sysName.startsWith( "AC1" ) ) {
      this.btnSysAC1.setSelected( true );
    }
    else if( sysName.startsWith( "BCS3" ) ) {
      this.btnSysBCS3.setSelected( true );
    }
    else if( sysName.startsWith( "HueblerEvertMC" ) ) {
      this.btnSysHEMC.setSelected( true );
    }
    else if( sysName.startsWith( "KramerMC" ) ) {
      this.btnSysKramerMC.setSelected( true );
    }
    else if( sysName.startsWith( "Z1013" ) ) {
      this.btnSysZ1013.setSelected( true );
    } else {
      this.btnSysKC85_1.setSelected( true );
    }


    // Optionen fuer AC1
    String ac1mon = EmuUtil.getProperty( props, "jkcemu.ac1.monitor" );
    if( ac1mon.equals( "3.1_64x16" ) ) {
      this.btnAC1mon31_64x16.setSelected( true );
    } else if( ac1mon.equals( "SCCH8.0" ) ) {
      this.btnAC1monSCCH80.setSelected( true );
    } else if( ac1mon.equals( "SCCH10/88" ) ) {
      this.btnAC1monSCCH1088.setSelected( true );
    } else {
      this.btnAC1mon31_64x32.setSelected( true );
    }


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
    } else if( bcs3Version.equals( "3.2" ) ) {
      this.btnBCS3se32_29.setSelected( true );
    } else {
      this.btnBCS3se24_27.setSelected( true );
    }
    if( EmuUtil.getProperty(
		props,
		"jkcemu.bcs3.ram.kbyte" ).equals( "17" ) )
    {
      this.btnBCS3ram17k.setSelected( true );
    } else {
      this.btnBCS3ram1k.setSelected( true );
    }


    // Optionen fuer KC85/2..4
    this.btnKC85VideoTiming.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kc85.emulate_video_timing",
				true ) );


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
      this.btnZ1013monA2.setSelected( true );
    } else if( monText.equals( "RB_K7659" ) ) {
      this.btnZ1013monRB_K7659.setSelected( true );
    } else if( monText.equals( "RB_S6009" ) ) {
      this.btnZ1013monRB_S6009.setSelected( true );
    } else if( monText.equals( "JM_1992" ) ) {
      this.btnZ1013monJM_1992.setSelected( true );
    } else {
      this.btnZ1013mon202.setSelected( true );
    }


    // Optionen fuer Z9001
    String ramText = EmuUtil.getProperty( props, "jkcemu.z9001.ram.kbyte" );
    if( ramText.equals( "32" ) ) {
      this.btnZ9001ram32k.setSelected( true );
    } else if( ramText.equals( "48" ) ) {
      this.btnZ9001ram48k.setSelected( true );
    } else {
      this.btnZ9001ram16k.setSelected( true );
    }
    this.btnZ9001color.setSelected(
	EmuUtil.getBooleanProperty( props, "jkcemu.z9001.color", true ) );


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


    // RAM-Floppies
    String rfAFileName = EmuUtil.getProperty(
					props,
					"jkcemu.ramfloppy.a.file.name" );
    this.fldRFAFile.setText( rfAFileName );
    this.btnRFAFileRemove.setEnabled( rfAFileName.length() > 0 );

    String rfBFileName = EmuUtil.getProperty(
					props,
					"jkcemu.ramfloppy.b.file.name" );
    this.fldRFBFile.setText( rfBFileName );
    this.btnRFBFileRemove.setEnabled( rfBFileName.length() > 0 );


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
    else if( this.btnSysZ1013.isSelected() ) {
      cardName = "Z1013";
    }
    else if( this.btnSysKC85_1.isSelected() || this.btnSysKC87.isSelected() ) {
      cardName = "Z9001";
    }
    else if( this.btnSysKC85_2.isSelected()
	     || this.btnSysKC85_3.isSelected()
	     || this.btnSysKC85_4.isSelected() )
    {
      cardName = "KC85";
    }
    this.cardLayoutSysOpt.show( this.panelSysOpt, cardName );
  }
}

