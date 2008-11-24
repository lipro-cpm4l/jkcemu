/*
 * (c) 2008 Jens Mueller
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
  private Vector<ExtROM>              extROMs;
  private Map<String,AbstractButton>  lafClass2Button;
  private SpinnerNumberModel          spinnerModelMargin;
  private JPanel                      panelEtc;
  private JPanel                      panelLAF;
  private JPanel                      panelROM;
  private JPanel                      panelSys;
  private JPanel                      panelSysOpt;
  private JCheckBox                   btnConfirmReset;
  private JCheckBox                   btnConfirmPowerOn;
  private JCheckBox                   btnConfirmQuit;
  private JComboBox                   comboScreenRefresh;
  private JList                       listROM;
  private CardLayout                  cardLayoutSysOpt;
  private JRadioButton                btnSysKC85_1;
  private JRadioButton                btnSysKC87;
  private JRadioButton                btnSysZ1013;
  private JRadioButton                btnSysAC1;
  private JRadioButton                btnZ9001ram16k;
  private JRadioButton                btnZ9001ram32k;
  private JRadioButton                btnZ9001ram48k;
  private JCheckBox                   btnZ9001color;
  private JRadioButton                btnZ1013mon202;
  private JRadioButton                btnZ1013monA2;
  private JRadioButton                btnZ1013monRB_K7659;
  private JRadioButton                btnZ1013monRB_S6009;
  private JRadioButton                btnZ1013monJM_1992;
  private JRadioButton                btnZ1013ram16k;
  private JRadioButton                btnZ1013ram64k;
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

    this.tabbedPane = new JTabbedPane(
				JTabbedPane.TOP,
				JTabbedPane.SCROLL_TAB_LAYOUT );
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

    this.btnSysKC85_1 = new JRadioButton( "KC85/1 (Z9001)", true );
    this.btnSysKC85_1.addActionListener( this );
    grpSys.add( this.btnSysKC85_1 );
    this.panelSys.add( this.btnSysKC85_1, gbcSys );

    this.btnSysKC87 = new JRadioButton( "KC87", false );
    this.btnSysKC87.addActionListener( this );
    grpSys.add( this.btnSysKC87 );
    gbcSys.insets.top    = 0;
    gbcSys.insets.bottom = 5;
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysKC87, gbcSys );

    this.btnSysZ1013 = new JRadioButton( "Z1013", false );
    this.btnSysZ1013.addActionListener( this );
    grpSys.add( this.btnSysZ1013 );
    gbcSys.insets.top = 5;
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysZ1013, gbcSys );

    this.btnSysAC1 = new JRadioButton( "AC1", false );
    this.btnSysAC1.addActionListener( this );
    grpSys.add( this.btnSysAC1 );
    gbcSys.gridy++;
    this.panelSys.add( this.btnSysAC1, gbcSys );

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

    this.btnZ9001ram16k = new JRadioButton( "16 kByte RAM", true );
    this.btnZ9001ram16k.addActionListener( this );
    grpZ9001ram.add( this.btnZ9001ram16k );
    panelZ9001.add( this.btnZ9001ram16k, gbcZ9001 );

    this.btnZ9001ram32k = new JRadioButton(
			"32 kByte RAM (1 RAM-Modul gesteckt)", false );
    this.btnZ9001ram32k.addActionListener( this );
    grpZ9001ram.add( this.btnZ9001ram32k );
    gbcZ9001.insets.top = 0;
    gbcZ9001.gridy++;
    panelZ9001.add( this.btnZ9001ram32k, gbcZ9001 );

    this.btnZ9001ram48k = new JRadioButton(
			"48 kByte RAM (2 RAM-Module gesteckt)", false );
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

    ButtonGroup grpZ1013mon = new ButtonGroup();

    this.btnZ1013mon202 = new JRadioButton(
		"Monitorprogramm 2.02 / Folienflachtastatur",
		true );
    this.btnZ1013mon202.addActionListener( this );
    grpZ1013mon.add( this.btnZ1013mon202 );
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

    ButtonGroup grpZ1013ram = new ButtonGroup();

    this.btnZ1013ram16k = new JRadioButton( "16 kByte RAM", false );
    this.btnZ1013ram16k.addActionListener( this );
    grpZ1013ram.add( this.btnZ1013ram16k );
    gbcZ1013.insets.top    = 5;
    gbcZ1013.insets.bottom = 0;
    gbcZ1013.gridy++;
    panelZ1013.add( this.btnZ1013ram16k, gbcZ1013 );

    this.btnZ1013ram64k = new JRadioButton( "64 kByte RAM", true );
    this.btnZ1013ram64k.addActionListener( this );
    grpZ1013ram.add( this.btnZ1013ram64k );
    gbcZ1013.insets.top    = 0;
    gbcZ1013.insets.bottom = 5;
    gbcZ1013.gridy++;
    panelZ1013.add( this.btnZ1013ram64k, gbcZ1013 );


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

    this.btnROMAdd = new JButton( "Hinzuf\u00FCgen" );
    this.btnROMAdd.addActionListener( this );
    panelROMBtn.add( this.btnROMAdd );

    this.btnROMRemove = new JButton( "Entfernen" );
    this.btnROMRemove.setEnabled( false );
    this.btnROMRemove.addActionListener( this );
    panelROMBtn.add( this.btnROMRemove );


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

    this.btnConfirmReset = new JCheckBox(
		"Emulator zur\u00FCcksetzen (RESET)",
		true );
    this.btnConfirmReset.addActionListener( this );
    gbcConfirm.insets.top  = 0;
    gbcConfirm.insets.left = 50;
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
    setExtROMs( this.emuThread.getExtROMs() );


    // sonstiges
    if( !super.applySettings( Main.getProperties(), true ) ) {
      pack();
      setLocationByPlatform( true );
    }
    setResizable( true );
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
	else if( (src == this.btnSysKC85_1)
		 || (src == this.btnSysKC87)
		 || (src == this.btnSysZ1013)
		 || (src == this.btnSysAC1) )
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
      this.emuThread.applySettings( appProps, newExtROMs, forceReset );
      Main.applyProfileToFrames( this.profileFile, appProps, false, this );

      if( !this.btnSpeedValue.isSelected() ) {
	EmuSys emuSys = this.emuThread.getNextEmuSys();
	if( emuSys == null ) {
	  emuSys = this.emuThread.getEmuSys();
	}
	String sysName = props.getProperty( "jkcemu.system" );
	if( sysName != null ) {
	  setSpeedValueFld( EmuThread.getDefaultSpeedKHz( sysName ) );
	}
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


  private void doROMAdd()
  {
    File file = EmuUtil.showFileOpenDlg(
				this,
				"ROM-Image-Datei laden",
				Main.getLastPathFile( "rom" ),
				EmuUtil.getBinaryFileFilter() );
    if( file != null ) {
      try {
	ReplyHexDlg dlg = new ReplyHexDlg( this, "Anfangsadresse:", 4 );
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
    String valueSys = "";
    if( this.btnSysKC85_1.isSelected() ) {
      valueSys = "KC85/1";
    }
    else if( this.btnSysKC87.isSelected() ) {
      valueSys = "KC87";
    }
    else if( this.btnSysZ1013.isSelected() ) {
      valueSys = "Z1013";
    }
    else if( this.btnSysAC1.isSelected() ) {
      valueSys = "AC1";
    }
    props.setProperty( "jkcemu.system", valueSys );

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
    props.setProperty(
		"jkcemu.z1013.ram.kbyte",
		this.btnZ1013ram16k.isSelected() ? "16" : "64" );
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
    if( sysName.startsWith( "KC85/1" ) || sysName.startsWith( "Z9001" ) ) {
      this.btnSysKC85_1.setSelected( true );
    }
    else if( sysName.startsWith( "KC87" ) ) {
      this.btnSysKC87.setSelected( true );
    }
    else if( sysName.startsWith( "Z1013" ) ) {
      this.btnSysZ1013.setSelected( true );
    }
    else if( sysName.startsWith( "AC1" ) ) {
      this.btnSysAC1.setSelected( true );
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

    // Optionen fuer Z1013
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
    this.btnZ1013ram16k.setSelected(
			EmuUtil.getProperty(
				props,
				"jkcemu.z1013.ram.kbyte" ).equals( "16" ) );
    updSysOptCard();

    // Geschwindigkeit
    int    defaultKHz = EmuThread.getDefaultSpeedKHz( sysName );
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

    // Bestaetigungen
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
    if( this.btnSysKC85_1.isSelected() || this.btnSysKC87.isSelected() ) {
      cardName = "Z9001";
    }
    else if( this.btnSysZ1013.isSelected() ) {
      cardName = "Z1013";
    }
    this.cardLayoutSysOpt.show( this.panelSysOpt, cardName );
  }
}

