/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen des benutzerdefinierten Computers
 */

package jkcemu.emusys.customsys;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Document;
import jkcemu.base.AbstractSettingsFld;
import jkcemu.base.AutoInputSettingsFld;
import jkcemu.base.AutoLoadSettingsFld;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.HexDocument;
import jkcemu.base.ROMFileSettingsFld;
import jkcemu.base.SettingsFrm;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.CustomSys;


public class CustomSysSettingsFld
			extends AbstractSettingsFld
			implements
				DocumentListener,
				ListSelectionListener,
				MouseListener
{
  private static final String TEXT_INVALID_ADDR  = " Ung\u00FCltige Adresse";
  private static final String TEXT_INVALID_VALUE = " Ung\u00FCltiger Wert";
  private static final String LABEL_CLOCK_FREQUENCY = "Taktfrequenz:";
  private static final String LABEL_SCREEN_COLS     = "Anzahl Textspalten:";
  private static final String LABEL_SCREEN_ROWS     = "Anzahl Textzeilen:";
  private static final String LABEL_SCREEN_BEGADDR
				= "Anfangsadresse Bildspeicher (hex):";
  private static final String LABEL_FDC_DATA_IOADDR
				= "E/A-Adresse Datenregister (hex):";
  private static final String LABEL_FDC_STATUS_IOADDR
				= "E/A-Adresse Statusregister (hex):";
  private static final String LABEL_FDC_TC_IOADDR
				= "E/A-Adresse Terminal Count (hex):";
  private static final String LABEL_FDC_TC_IOBIT
				= "Terminal Count ausl\u00F6sen bei:";
  private static final String LABEL_UNUSED_PORT_VALUE
		= "Gelesener Wert von nicht belegten E/A-Adressen (hex):";

  private static final String[] SIO_CLOCK_ITEMS = {
					"CTC Ausgang 0",
					"CTC Ausgang 1",
					"CTC Ausgang 2" };

  private static final String[] SIO_OUT_ITEMS = {
					"nichts angeschlossen",
					"Drucker" };

  private JTabbedPane            tabbedPane;
  private JButton                btnRomAdd;
  private JButton                btnRomEdit;
  private JButton                btnRomRemove;
  private JButton                btnRomUp;
  private JButton                btnRomDown;
  private JTable                 tableRom;
  private CustomSysROMTableModel tableModelRom;
  private ListSelectionModel     selModelRom;
  private Document               docTitle;
  private Document               docSpeedMHz;
  private HexDocument            docScreenBegAddr;
  private HexDocument            docKeyboardIOAddr;
  private HexDocument            docCtcIOBaseAddr;
  private HexDocument            docPioIOBaseAddr;
  private HexDocument            docSioIOBaseAddr;
  private HexDocument            docKCNetIOBaseAddr;
  private HexDocument            docVdipIOBaseAddr;
  private HexDocument            docFdcDataIOAddr;
  private HexDocument            docFdcStatusIOAddr;
  private HexDocument            docFdcTCIOAddr;
  private HexDocument            docUnusedPortValue;
  private JTextField             fldTitle;
  private JTextField             fldScreenBegAddr;
  private JTextField             fldSpeedMHz;
  private JTextField             fldKeyboardIOAddr;
  private JTextField             fldCtcIOBaseAddr;
  private JTextField             fldPioIOBaseAddr;
  private JTextField             fldSioIOBaseAddr;
  private JTextField             fldKCNetIOBaseAddr;
  private JTextField             fldVdipIOBaseAddr;
  private JTextField             fldFdcDataIOAddr;
  private JTextField             fldFdcStatusIOAddr;
  private JTextField             fldFdcTCIOAddr;
  private JTextField             fldUnusedPortValue;
  private JComboBox<String>      comboScreenRows;
  private JComboBox<String>      comboScreenCols;
  private JComboBox<String>      comboSioClockA;
  private JComboBox<String>      comboSioClockB;
  private JComboBox<String>      comboSioOutA;
  private JComboBox<String>      comboSioOutB;
  private JComboBox<String>      comboFdcTCIOBit;
  private JCheckBox              btnKeyboardSwapCase;
  private JCheckBox              btnScreenEnabled;
  private JCheckBox              btnCtcEnabled;
  private JCheckBox              btnFdcEnabled;
  private JCheckBox              btnPioEnabled;
  private JCheckBox              btnSioEnabled;
  private JCheckBox              btnKCNetEnabled;
  private JCheckBox              btnVdipEnabled;
  private JRadioButton           btnKeyboardNone;
  private JRadioButton           btnKeyboardPortRaw;
  private JRadioButton           btnKeyboardPioAhs;
  private JRadioButton           btnKeyboardPioAbit7;
  private JRadioButton           btnKeyboardPioBhs;
  private JRadioButton           btnKeyboardPioBbit7;
  private JRadioButton           btnKeyboardSioA;
  private JRadioButton           btnKeyboardSioB;
  private JLabel                 labelFdcDataIOAddr;
  private JLabel                 labelFdcStatusIOAddr;
  private JLabel                 labelFdcTCIOAddr;
  private JLabel                 labelFdcTCBit;
  private JLabel                 labelScreenBegAddr;
  private JLabel                 labelScreenCols;
  private JLabel                 labelScreenRows;
  private JLabel                 labelSioClockA;
  private JLabel                 labelSioClockB;
  private JLabel                 labelSioOutA;
  private JLabel                 labelSioOutB;
  private JLabel                 labelVdipIOBaseAddr;
  private JPanel                 tabFDC;
  private JPanel                 tabGeneral;
  private JPanel                 tabIO;
  private JPanel                 tabKeyboard;
  private JPanel                 tabROM;
  private JPanel                 tabScreen;
  private GIDESettingsFld        tabGIDE;
  private AutoLoadSettingsFld    tabAutoLoad;
  private AutoInputSettingsFld   tabAutoInput;
  private ROMFileSettingsFld     fldAltFont;


  public CustomSysSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Bereich Allgemein
    this.tabGeneral = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Allgemein", this.tabGeneral );

    GridBagConstraints gbcGeneral = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tabGeneral.add(
		new JLabel( "Bezeichnung des Computers:" ),
		gbcGeneral );
    gbcGeneral.gridy++;
    this.tabGeneral.add( new JLabel( LABEL_CLOCK_FREQUENCY ), gbcGeneral );

    this.fldTitle        = new JTextField( 5 );
    this.docTitle        = this.fldTitle.getDocument();
    gbcGeneral.anchor    = GridBagConstraints.WEST;
    gbcGeneral.fill      = GridBagConstraints.HORIZONTAL;
    gbcGeneral.weightx   = 1.0;
    gbcGeneral.gridwidth = GridBagConstraints.REMAINDER;
    gbcGeneral.gridy     = 0;
    gbcGeneral.gridx++;
    this.tabGeneral.add( this.fldTitle, gbcGeneral );

    this.fldSpeedMHz     = new JTextField( 5 );
    this.docSpeedMHz     = this.fldSpeedMHz.getDocument();
    gbcGeneral.fill      = GridBagConstraints.NONE;
    gbcGeneral.weightx   = 0.0;
    gbcGeneral.gridwidth = 1;
    gbcGeneral.gridy++;
    this.tabGeneral.add( this.fldSpeedMHz, gbcGeneral );
    gbcGeneral.gridx++;
    this.tabGeneral.add( new JLabel( "MHz" ), gbcGeneral );

    if( this.docTitle != null ) {
      this.docTitle.addDocumentListener( this );
    }
    if( this.docSpeedMHz != null ) {
      this.docSpeedMHz.addDocumentListener( this );
    }


    // Tab ROM
    this.tabROM = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "ROM", this.tabROM );

    GridBagConstraints gbcRom = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tableModelRom = new CustomSysROMTableModel();

    this.tableRom = new JTable( this.tableModelRom );
    this.tableRom.setAutoCreateRowSorter( false );
    this.tableRom.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.tableRom.setColumnSelectionAllowed( false );
    this.tableRom.setDragEnabled( false );
    this.tableRom.setFillsViewportHeight( false );
    this.tableRom.setPreferredScrollableViewportSize( new Dimension( 1, 1 ) );
    this.tableRom.setRowSelectionAllowed( true );
    this.tableRom.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.tableRom.addMouseListener( this );

    this.tabROM.add( new JScrollPane( this.tableRom ), gbcRom );

    EmuUtil.setTableColWidths( this.tableRom, 100, 70, 300 );

    JPanel panelRomBtnRight = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
    gbcRom.fill             = GridBagConstraints.NONE;
    gbcRom.weightx          = 0.0;
    gbcRom.weighty          = 0.0;
    gbcRom.gridx++;
    this.tabROM.add( panelRomBtnRight, gbcRom );

    this.btnRomUp = createImageButton( "/images/nav/up.png", "Auf" );
    panelRomBtnRight.add( this.btnRomUp );

    this.btnRomDown = createImageButton( "/images/nav/down.png", "Ab" );
    panelRomBtnRight.add( this.btnRomDown );

    JPanel panelRomBtnBottom = new JPanel( new GridLayout( 1, 3, 5, 5 ) );
    gbcRom.gridx = 0;
    gbcRom.gridy++;
    this.tabROM.add( panelRomBtnBottom, gbcRom );

    this.btnRomAdd = new JButton( "Hinzuf\u00FCgen" );
    this.btnRomAdd.addActionListener( this );
    this.btnRomAdd.addKeyListener( this );
    panelRomBtnBottom.add( this.btnRomAdd );

    this.btnRomEdit = new JButton( "Bearbeiten" );
    this.btnRomEdit.addActionListener( this );
    this.btnRomEdit.addKeyListener( this );
    panelRomBtnBottom.add( this.btnRomEdit );

    this.btnRomRemove = new JButton( "Entfernen" );
    this.btnRomRemove.addActionListener( this );
    this.btnRomRemove.addKeyListener( this );
    panelRomBtnBottom.add( this.btnRomRemove );

    this.selModelRom = this.tableRom.getSelectionModel();
    if( this.selModelRom != null ) {
      this.selModelRom.addListSelectionListener( this );
      this.btnRomUp.setEnabled( false );
      this.btnRomDown.setEnabled( false );
      this.btnRomEdit.setEnabled( false );
      this.btnRomRemove.setEnabled( false );
    }


    // Tab Bildschirmausgabe
    this.tabScreen = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Bildschirmausgabe", this.tabScreen );

    GridBagConstraints gbcScreen = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnScreenEnabled = new JCheckBox( "Bildschirmausgabe emulieren" );
    this.tabScreen.add( this.btnScreenEnabled, gbcScreen );

    this.labelScreenCols  = new JLabel( LABEL_SCREEN_COLS );
    gbcScreen.insets.left = 50;
    gbcScreen.gridwidth   = 1;
    gbcScreen.gridy++;
    this.tabScreen.add( this.labelScreenCols, gbcScreen );

    this.comboScreenCols = new JComboBox<>(
				new String[] { "32", "40", "64", "80" } );
    this.comboScreenCols.setEditable( false );
    gbcScreen.insets.left = 5;
    gbcScreen.gridx++;
    this.tabScreen.add( this.comboScreenCols, gbcScreen );

    this.labelScreenRows  = new JLabel( LABEL_SCREEN_ROWS );
    gbcScreen.insets.left = 50;
    gbcScreen.gridx       = 0;
    gbcScreen.gridy++;
    this.tabScreen.add( this.labelScreenRows, gbcScreen );

    this.comboScreenRows = new JComboBox<>(
				new String[] { "16", "24", "25", "32" } );
    this.comboScreenRows.setEditable( false );
    gbcScreen.insets.left = 5;
    gbcScreen.gridx++;
    this.tabScreen.add( this.comboScreenRows, gbcScreen );

    this.labelScreenBegAddr = new JLabel( LABEL_SCREEN_BEGADDR );
    gbcScreen.insets.left   = 50;
    gbcScreen.insets.bottom = 5;
    gbcScreen.gridx         = 0;
    gbcScreen.gridy++;
    this.tabScreen.add( this.labelScreenBegAddr, gbcScreen );

    this.docScreenBegAddr = new HexDocument( 4, LABEL_SCREEN_BEGADDR );
    this.fldScreenBegAddr = new JTextField( this.docScreenBegAddr, "", 5 );
    gbcScreen.insets.left = 5;
    gbcScreen.gridx++;
    this.tabScreen.add( this.fldScreenBegAddr, gbcScreen );

    Font font = this.fldScreenBegAddr.getFont();
    if( font != null ) {
      this.comboScreenCols.setFont( font );
      this.comboScreenRows.setFont( font );
    }

    gbcScreen.fill          = GridBagConstraints.HORIZONTAL;
    gbcScreen.weightx       = 1.0;
    gbcScreen.insets.top    = 15;
    gbcScreen.insets.bottom = 10;
    gbcScreen.gridwidth     = GridBagConstraints.REMAINDER;
    gbcScreen.gridx         = 0;
    gbcScreen.gridy++;
    this.tabScreen.add( new JSeparator(), gbcScreen );

    this.fldAltFont = new ROMFileSettingsFld(
			settingsFrm,
			propPrefix + CustomSys.PROP_FONT_PREFIX,
			"Alternativer Zeichensatz:" );
    gbcScreen.gridy++;
    this.tabScreen.add( this.fldAltFont, gbcScreen );

    this.btnScreenEnabled.addActionListener( this );
    this.comboScreenCols.addActionListener( this );
    this.comboScreenRows.addActionListener( this );
    this.docScreenBegAddr.addDocumentListener( this );
    updScreenFieldsEnabled();


    // Tab Tastatur
    this.tabKeyboard = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Tastatur", this.tabKeyboard );

    GridBagConstraints gbcKeyboard = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.tabKeyboard.add(
		new JLabel( "Tastatur ist angeschlossen an:" ),
		gbcKeyboard );

    ButtonGroup grpKeyboard = new ButtonGroup();

    this.btnKeyboardPortRaw = new JRadioButton(
		"Einfaches Eingabetor an E/A-Adresse (hex):" );
    grpKeyboard.add( this.btnKeyboardPortRaw );
    gbcKeyboard.insets.left = 50;
    gbcKeyboard.gridwidth   = 1;
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.btnKeyboardPortRaw, gbcKeyboard );

    this.docKeyboardIOAddr = new HexDocument(
					2,
					this.btnKeyboardPortRaw.getText() );
    this.fldKeyboardIOAddr  = new JTextField( this.docKeyboardIOAddr, "", 3 );
    gbcKeyboard.insets.left = 5;
    gbcKeyboard.gridx++;
    this.tabKeyboard.add( this.fldKeyboardIOAddr, gbcKeyboard );

    this.btnKeyboardPioAhs = new JRadioButton(
		"PIO Port A mit Ready/Strobe-Handshake" );
    grpKeyboard.add( this.btnKeyboardPioAhs );
    gbcKeyboard.gridwidth   = GridBagConstraints.REMAINDER;
    gbcKeyboard.insets.top  = 0;
    gbcKeyboard.insets.left = 50;
    gbcKeyboard.gridx       = 0;
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.btnKeyboardPioAhs, gbcKeyboard );

    this.btnKeyboardPioAbit7 = new JRadioButton(
		"PIO Port A mit Strobe an Bit 7" );
    grpKeyboard.add( this.btnKeyboardPioAbit7 );
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.btnKeyboardPioAbit7, gbcKeyboard );

    this.btnKeyboardPioBhs = new JRadioButton(
		"PIO Port B mit Ready/Strobe-Handshake" );
    grpKeyboard.add( this.btnKeyboardPioBhs );
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.btnKeyboardPioBhs, gbcKeyboard );

    this.btnKeyboardPioBbit7 = new JRadioButton(
		"PIO Port B mit Strobe an Bit 7" );
    grpKeyboard.add( this.btnKeyboardPioBbit7 );
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.btnKeyboardPioBbit7, gbcKeyboard );

    this.btnKeyboardSioA = new JRadioButton( "SIO Kanal A" );
    grpKeyboard.add( this.btnKeyboardSioA );
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.btnKeyboardSioA, gbcKeyboard );

    this.btnKeyboardSioB = new JRadioButton( "SIO Kanal B" );
    grpKeyboard.add( this.btnKeyboardSioB );
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.btnKeyboardSioB, gbcKeyboard );

    this.btnKeyboardNone = new JRadioButton( "Keine Tastatur emulieren" );
    grpKeyboard.add( this.btnKeyboardNone );
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.btnKeyboardNone, gbcKeyboard );

    this.btnKeyboardSwapCase = new JCheckBox(
		"Gro\u00DF-/Kleinschreibung umkehren" );
    gbcKeyboard.insets.top    = 10;
    gbcKeyboard.insets.left   = 5;
    gbcKeyboard.insets.bottom = 5;
    gbcKeyboard.gridx         = 0;
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.btnKeyboardSwapCase, gbcKeyboard );

    this.btnKeyboardNone.addActionListener( this );
    this.btnKeyboardPortRaw.addActionListener( this );
    this.btnKeyboardPioAhs.addActionListener( this );
    this.btnKeyboardPioAbit7.addActionListener( this );
    this.btnKeyboardPioBhs.addActionListener( this );
    this.btnKeyboardPioBbit7.addActionListener( this );
    this.btnKeyboardSioA.addActionListener( this );
    this.btnKeyboardSioB.addActionListener( this );
    this.btnKeyboardSwapCase.addActionListener( this );
    this.docKeyboardIOAddr.addDocumentListener( this );
    updKeyboardFieldsEnabled();


    // Bereich E/A-Bausteine
    this.tabIO = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "E/A-Bausteine", this.tabIO );

    GridBagConstraints gbcIO = new GridBagConstraints(
					0, 0,
					2, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnPioEnabled = new JCheckBox(
		"PIO emulieren an E/A-Basisadresse (hex):" );
    this.tabIO.add( this.btnPioEnabled, gbcIO );

    this.docPioIOBaseAddr = new HexDocument(
					2,
					this.btnPioEnabled.getText() );
    this.fldPioIOBaseAddr = new JTextField( this.docPioIOBaseAddr,
					"",
					3 );
    gbcIO.insets.top = 2;
    gbcIO.gridwidth  = 1;
    gbcIO.gridx += 2;
    this.tabIO.add( this.fldPioIOBaseAddr, gbcIO );

    this.btnSioEnabled = new JCheckBox(
		"SIO emulieren an E/A-Basisadresse (hex):" );
    gbcIO.gridwidth = 2;
    gbcIO.gridx     = 0;
    gbcIO.gridy++;
    this.tabIO.add( this.btnSioEnabled, gbcIO );

    this.docSioIOBaseAddr = new HexDocument(
					2,
					this.btnSioEnabled.getText() );
    this.fldSioIOBaseAddr = new JTextField( this.docSioIOBaseAddr,
					"",
					3 );
    gbcIO.gridwidth = 1;
    gbcIO.gridx += 2;
    this.tabIO.add( this.fldSioIOBaseAddr, gbcIO );

    this.labelSioClockA = new JLabel( "Kanal A getaktet durch:" );
    gbcIO.insets.left   = 50;
    gbcIO.gridx         = 0;
    gbcIO.gridy++;
    this.tabIO.add( this.labelSioClockA, gbcIO );

    this.comboSioClockA = new JComboBox<>( SIO_CLOCK_ITEMS );
    this.comboSioClockA.setEditable( false );
    gbcIO.insets.left = 5;
    gbcIO.gridx++;
    this.tabIO.add( this.comboSioClockA, gbcIO );

    this.labelSioClockB = new JLabel( "Kanal B getaktet durch:" );
    gbcIO.insets.left   = 50;
    gbcIO.gridx         = 0;
    gbcIO.gridy++;
    this.tabIO.add( this.labelSioClockB, gbcIO );

    this.comboSioClockB = new JComboBox<>( SIO_CLOCK_ITEMS );
    this.comboSioClockB.setEditable( false );
    gbcIO.insets.left = 5;
    gbcIO.gridx++;
    this.tabIO.add( this.comboSioClockB, gbcIO );

    this.labelSioOutA = new JLabel( "Kanal A Ausgang:" );
    gbcIO.insets.left = 50;
    gbcIO.gridx       = 0;
    gbcIO.gridy++;
    this.tabIO.add( this.labelSioOutA, gbcIO );

    this.comboSioOutA = new JComboBox<>( SIO_OUT_ITEMS );
    this.comboSioOutA.setEditable( false );
    gbcIO.insets.left = 5;
    gbcIO.gridx++;
    this.tabIO.add( this.comboSioOutA, gbcIO );

    this.labelSioOutB = new JLabel( "Kanal B Ausgang:" );
    gbcIO.insets.left = 50;
    gbcIO.gridx       = 0;
    gbcIO.gridy++;
    this.tabIO.add( this.labelSioOutB, gbcIO );

    this.comboSioOutB = new JComboBox<>( SIO_OUT_ITEMS );
    this.comboSioOutB.setEditable( false );
    gbcIO.insets.left = 5;
    gbcIO.gridx++;
    this.tabIO.add( this.comboSioOutB, gbcIO );

    this.btnCtcEnabled = new JCheckBox(
		"CTC emulieren an E/A-Basisadresse (hex):" );
    gbcIO.gridwidth = 2;
    gbcIO.gridx     = 0;
    gbcIO.gridy++;
    this.tabIO.add( this.btnCtcEnabled, gbcIO );

    this.docCtcIOBaseAddr = new HexDocument(
					2,
					this.btnCtcEnabled.getText() );
    this.fldCtcIOBaseAddr = new JTextField( this.docCtcIOBaseAddr,
					"",
					3 );
    gbcIO.gridwidth = 1;
    gbcIO.gridx += 2;
    this.tabIO.add( this.fldCtcIOBaseAddr, gbcIO );

    this.btnKCNetEnabled = new JCheckBox(
		"KCNet emulieren an E/A-Basisadresse (hex):" );
    gbcIO.gridwidth = 2;
    gbcIO.gridx     = 0;
    gbcIO.gridy++;
    this.tabIO.add( this.btnKCNetEnabled, gbcIO );

    this.docKCNetIOBaseAddr = new HexDocument(
					2,
					this.btnKCNetEnabled.getText() );
    this.fldKCNetIOBaseAddr = new JTextField( this.docKCNetIOBaseAddr,
					"",
					3 );
    gbcIO.gridwidth = 1;
    gbcIO.gridx += 2;
    this.tabIO.add( this.fldKCNetIOBaseAddr, gbcIO );

    this.btnVdipEnabled = new JCheckBox(
		"USB (VDIP) emulieren an E/A-Basisadresse (hex):" );
    gbcIO.gridwidth = 2;
    gbcIO.gridx     = 0;
    gbcIO.gridy++;
    this.tabIO.add( this.btnVdipEnabled, gbcIO );

    this.docVdipIOBaseAddr = new HexDocument(
					2,
					this.btnVdipEnabled.getText() );
    this.fldVdipIOBaseAddr = new JTextField( this.docVdipIOBaseAddr,
					"",
					3 );
    gbcIO.gridwidth = 1;
    gbcIO.gridx += 2;
    this.tabIO.add( this.fldVdipIOBaseAddr, gbcIO );

    gbcIO.insets.top    = 10;
    gbcIO.insets.bottom = 5;
    gbcIO.gridwidth     = 2;
    gbcIO.gridx         = 0;
    gbcIO.gridy++;
    this.tabIO.add( new JLabel( LABEL_UNUSED_PORT_VALUE ), gbcIO );

    this.docUnusedPortValue = new HexDocument( 2, LABEL_UNUSED_PORT_VALUE );
    this.fldUnusedPortValue = new JTextField( this.docUnusedPortValue,
					"",
					3 );
    gbcIO.gridx += 2;
    this.tabIO.add( this.fldUnusedPortValue, gbcIO );

    this.btnPioEnabled.addActionListener( this );
    this.btnSioEnabled.addActionListener( this );
    this.btnCtcEnabled.addActionListener( this );
    this.btnKCNetEnabled.addActionListener( this );
    this.btnVdipEnabled.addActionListener( this );

    this.comboSioClockA.addActionListener( this );
    this.comboSioClockB.addActionListener( this );
    this.comboSioOutA.addActionListener( this );
    this.comboSioOutB.addActionListener( this );

    this.docPioIOBaseAddr.addDocumentListener( this );
    this.docSioIOBaseAddr.addDocumentListener( this );
    this.docCtcIOBaseAddr.addDocumentListener( this );
    this.docKCNetIOBaseAddr.addDocumentListener( this );
    this.docVdipIOBaseAddr.addDocumentListener( this );
    this.docUnusedPortValue.addDocumentListener( this );

    updPioFieldsEnabled();
    updSioFieldsEnabled();
    updCtcFieldsEnabled();
    updKCNetFieldsEnabled();
    updVdipFieldsEnabled();


    // Tab FDC
    this.tabFDC = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "FDC", this.tabFDC );

    GridBagConstraints gbcFdc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnFdcEnabled = new JCheckBox(
	"Floppy Disk Controller (FDC) mit 4 Diskettenlaufwerken emulieren" );
    this.tabFDC.add( this.btnFdcEnabled, gbcFdc );

    this.labelFdcDataIOAddr = new JLabel( LABEL_FDC_DATA_IOADDR );
    gbcFdc.insets.left      = 50;
    gbcFdc.gridwidth        = 1;
    gbcFdc.gridy++;
    this.tabFDC.add( this.labelFdcDataIOAddr, gbcFdc );

    this.labelFdcStatusIOAddr = new JLabel( LABEL_FDC_STATUS_IOADDR );
    gbcFdc.gridy++;
    this.tabFDC.add( this.labelFdcStatusIOAddr, gbcFdc );

    this.labelFdcTCIOAddr = new JLabel( LABEL_FDC_TC_IOADDR );
    gbcFdc.gridy++;
    this.tabFDC.add( this.labelFdcTCIOAddr, gbcFdc );

    this.labelFdcTCBit = new JLabel( LABEL_FDC_TC_IOBIT );
    gbcFdc.gridy++;
    this.tabFDC.add( this.labelFdcTCBit, gbcFdc );

    this.docFdcDataIOAddr = new HexDocument( 2, LABEL_FDC_DATA_IOADDR );
    this.fldFdcDataIOAddr = new JTextField( this.docFdcDataIOAddr, "", 3 );
    gbcFdc.insets.left    = 5;
    gbcFdc.gridy          = 1;
    gbcFdc.gridx++;
    this.tabFDC.add( this.fldFdcDataIOAddr, gbcFdc );

    this.docFdcStatusIOAddr = new HexDocument( 2, LABEL_FDC_STATUS_IOADDR );
    this.fldFdcStatusIOAddr = new JTextField(
					this.docFdcStatusIOAddr,
					"",
					3 );
    gbcFdc.gridy++;
    this.tabFDC.add( this.fldFdcStatusIOAddr, gbcFdc );

    this.docFdcTCIOAddr = new HexDocument( 2, LABEL_FDC_TC_IOADDR );
    this.fldFdcTCIOAddr = new JTextField( this.docFdcTCIOAddr, "", 3 );
    gbcFdc.gridy++;
    this.tabFDC.add( this.fldFdcTCIOAddr, gbcFdc );

    this.comboFdcTCIOBit = new JComboBox<>();
    this.comboFdcTCIOBit.setEditable( false );
    this.comboFdcTCIOBit.addItem( "jedem Ausgabebefehl" );
    for( int i = 0; i < 8; i++ ) {
      this.comboFdcTCIOBit.addItem( String.format( "Bit %d \u2192 H", i ) );
    }
    for( int i = 0; i < 8; i++ ) {
      this.comboFdcTCIOBit.addItem( String.format( "Bit %d \u2192 L", i ) );
    }
    gbcFdc.gridy++;
    this.tabFDC.add( this.comboFdcTCIOBit, gbcFdc );

    this.btnFdcEnabled.addActionListener( this );
    updFdcFieldsEnabled();


    // GIDE
    this.tabGIDE = new GIDESettingsFld(
				settingsFrm,
				propPrefix,
				CustomSys.DEFAULT_GIDE_IOBASEADDR,
				0x00, 0x10, 0x20, 0x30,
				0x40, 0x50, 0x60, 0x70,
				0x80, 0x90, 0xA0, 0xB0,
				0xC0, 0xD0, 0xE0, 0xF0 );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
			settingsFrm,
			propPrefix,
			CustomSys.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
			true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
			settingsFrm,
			propPrefix,
			CustomSys.DEFAULT_SWAP_KEY_CHAR_CASE,
			0,
			CustomSys.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );
  }


	/* --- DocumentEvent --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.selModelRom ) {
      int     nRows     = this.tableRom.getRowCount();
      int     nSelRows  = this.tableRom.getSelectedRowCount();
      int     selRowNum = this.tableRom.getSelectedRow();
      boolean stateOne  = (nSelRows == 1) && (selRowNum >= 0);
      boolean stateEdit = false;
      this.btnRomUp.setEnabled( (nSelRows == 1) && (selRowNum > 0) );
      this.btnRomDown.setEnabled( stateOne && (selRowNum < (nRows - 1)) );
      this.btnRomRemove.setEnabled( nSelRows > 0 );
      if( stateOne ) {
	int row = this.tableRom.convertRowIndexToModel( selRowNum );
	if( row >= 0 ) {
	  CustomSysROM rom = this.tableModelRom.getRow( row );
	  if( rom != null ) {
	    stateEdit = true;
	  }
	}
      }
      this.btnRomEdit.setEnabled( stateEdit );
    }
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getButton() == MouseEvent.BUTTON1)
	&& (e.getClickCount() > 1)
	&& (e.getComponent() == this.tableRom) )
    {
      doRomEdit();
      e.consume();
    }
  }


  @Override
  public void mouseEntered( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mouseExited( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    Component tab  = null;
    String    text = null;
    try {

      // Tab Allgemein
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_TITLE,
		this.fldTitle.getText() );
      String mhzText = this.fldSpeedMHz.getText();
      if( mhzText == null ) {
	mhzText = "";
      }
      try {
	int    khz = 0;
	Number mhz = this.settingsFrm.getSpeedFmt().parse( mhzText );
	if( mhz != null ) {
	  khz = Math.round( mhz.floatValue() * 1000F );
	}
	if( khz < 1 ) {
	  throw new UserInputException(
			LABEL_CLOCK_FREQUENCY + TEXT_INVALID_VALUE );
	}
	EmuUtil.setProperty(
			props,
			this.propPrefix + CustomSys.PROP_SPEED_KHZ,
			khz );
      }
      catch( ParseException ex ) {
	throw new UserInputException(
			LABEL_CLOCK_FREQUENCY + TEXT_INVALID_VALUE );
      }

      // Tab ROM
      boolean romOverlap = false;
      boolean romAt0000  = false;
      int     nBootRoms  = 0;
      int     nRoms      = this.tableModelRom.getRowCount();
      if( selected ) {
	for( int i = 0; i < nRoms; i++ ) {
	  CustomSysROM rom = this.tableModelRom.getRow( i );
	  if( rom != null ) {
	    if( rom.getBegAddr() == 0x0000 ) {
	      romAt0000 = true;
	    }
	    if( rom.isBootROM() && rom.isEnabledAfterReset()) {
	      nBootRoms++;
	    }
	    for( int k = i + 1; k < nRoms; k++ ) {
	      int          addr1 = rom.getBegAddr();
	      int          size1 = rom.getSize();
	      CustomSysROM rom2  = this.tableModelRom.getRow( k );
	      if( rom2 != null ) {
		int addr2 = rom2.getBegAddr();
		int size2 = rom2.getSize();
		if( ((addr1 <= addr2) && ((addr1 + size1) > addr2))
		    || ((addr2 <= addr1) && ((addr2 + size2) > addr1)) )
		{
		  romOverlap = true;
		  break;
		}
	      }
	    }
	  }
	}
	if( romOverlap ) {
	  if( !BaseDlg.showConfirmDlg(
		this.settingsFrm,
		"ROM-Bereiche \u00FCberlappen sich.\n"
			+ "Im Fall einer \u00DCberlappung ist der"
			+ " ROM-Bereich relevant,\n"
			+ "der in der Liste weiter oben steht." ) )
	  {
	    throw new UserCancelException();
	  }
	}
	if( !romAt0000 && (nBootRoms == 0) ) {
	  if( !BaseDlg.showConfirmDlg(
		this.settingsFrm,
		"An der Adesse 0000h befindet sich kein ROM\n"
			+ "und es ist auch kein Boot-ROM markiert.\n"
			+ "Nach RESET beginnt die Programmausf\u00FChrung"
			+ " somit im RAM!" ) )
	  {
	    throw new UserCancelException();
	  }
	}
      }
      int romIdx = 0;
      for( int i = 0; i < nRoms; i++ ) {
	CustomSysROM rom = this.tableModelRom.getRow( i );
	if( rom != null ) {
	  String fileName = rom.getFileName();
	  String prefix   = String.format(
				"%s%s%d.",
				this.propPrefix,
				CustomSys.PROP_ROM_PREFIX,
				romIdx++ );
	  EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_BEGADDR,
			rom.getBegAddr() );
	  EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_SIZE,
			rom.getSize() );
	  EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_FILE,
			fileName != null ? fileName : "" );
	  EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_SWITCH_IOADDR,
			rom.getSwitchIOAddr() );
	  EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_SWITCH_IOMASK,
			rom.getSwitchIOMask() );
	  EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_SWITCH_IOVALUE,
			rom.getSwitchIOValue() );
	  EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_ENABLE_ON_RESET,
			rom.getEnableOnReset() );
	  EmuUtil.setProperty(
			props,
			prefix + CustomSys.PROP_BOOT,
			rom.isBootROM() );
	}
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix
			+ CustomSys.PROP_ROM_PREFIX
			+ CustomSys.PROP_COUNT,
		romIdx );

      // Tab Bildschirmausgabe
      tab                   = this.tabScreen;
      boolean screenEnabled = this.btnScreenEnabled.isSelected();
      int screenCols        = EmuUtil.getInt( this.comboScreenCols );
      int screenRows        = EmuUtil.getInt( this.comboScreenRows );
      int screenBegAddr     = CustomSys.DEFAULT_SCREEN_BEGADDR;
      try {
	screenBegAddr = this.docScreenBegAddr.intValue();
      }
      catch( NumberFormatException ex ) {
	if( selected && screenEnabled ) {
	  throw new UserInputException(
			LABEL_SCREEN_BEGADDR + TEXT_INVALID_ADDR );
	}
      }
      if( selected && screenEnabled ) {
	if( screenCols < 1 ) {
	  throw new UserInputException(
			LABEL_SCREEN_COLS + " Ung\u00FCltiger Wert" );
	}
	if( screenRows < 1 ) {
	  throw new UserInputException(
			LABEL_SCREEN_ROWS + TEXT_INVALID_VALUE );
	}
	if( (screenBegAddr + (screenCols * screenRows)) > 0x10000 ) {
	  throw new UserInputException(
			"Bildwiederholspeicher ragt \u00FCber"
				+ " die Adresse FFFFh hinaus." );
	}
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SCREEN_ENABLED,
		screenEnabled );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SCREEN_BEGADDR,
		screenBegAddr );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SCREEN_COLS,
		screenCols );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SCREEN_ROWS,
		screenRows );
      this.fldAltFont.applyInput( props, selected );

      // Tastatur
      boolean stateKeyboardIOAddr = false;
      int     keyboardIOAddr      = CustomSys.DEFAULT_KEYBOARD_IOADDR;
      String  keyboardHW          = CustomSys.VALUE_NONE;
      if( this.btnKeyboardPortRaw.isSelected() ) {
	stateKeyboardIOAddr = true;
	keyboardHW          = CustomSys.VALUE_KEYBOARD_PORT_RAW;
      } else if( this.btnKeyboardPioAhs.isSelected() ) {
        keyboardHW = CustomSys.VALUE_KEYBOARD_PIO_A_HS;
      } else if( this.btnKeyboardPioAbit7.isSelected() ) {
        keyboardHW = CustomSys.VALUE_KEYBOARD_PIO_A_BIT7;
      } else if( this.btnKeyboardPioBhs.isSelected() ) {
        keyboardHW = CustomSys.VALUE_KEYBOARD_PIO_B_HS;
      } else if( this.btnKeyboardPioBbit7.isSelected() ) {
        keyboardHW = CustomSys.VALUE_KEYBOARD_PIO_B_BIT7;
      } else if( this.btnKeyboardSioA.isSelected() ) {
        keyboardHW = CustomSys.VALUE_KEYBOARD_SIO_A;
      } else if( this.btnKeyboardSioB.isSelected() ) {
        keyboardHW = CustomSys.VALUE_KEYBOARD_SIO_B;
      }
      try {
	keyboardIOAddr = this.docKeyboardIOAddr.intValue();
      }
      catch( NumberFormatException ex ) {
	if( selected && stateKeyboardIOAddr ) {
	  throw new UserInputException(
		this.btnKeyboardPortRaw.getText() + TEXT_INVALID_ADDR );
	}
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_KEYBOARD_HW,
		keyboardHW );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_KEYBOARD_IOADDR,
		keyboardIOAddr );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SWAP_KEY_CHAR_CASE,
		this.btnKeyboardSwapCase.isSelected() );

      // Tab E/A-Bausteine
      int pioIOBaseAddr = applyIOAddrInput(
				selected,
				this.btnPioEnabled,
				CustomSys.PROP_PIO_ENABLED,
				this.docPioIOBaseAddr,
				CustomSys.PROP_PIO_IOBASEADDR,
				props );
      int sioIOBaseAddr = applyIOAddrInput(
				selected,
				this.btnSioEnabled,
				CustomSys.PROP_SIO_ENABLED,
				this.docSioIOBaseAddr,
				CustomSys.PROP_SIO_IOBASEADDR,
				props );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SIO_A_CLOCK,
		this.comboSioClockA.getSelectedIndex() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SIO_B_CLOCK,
		this.comboSioClockB.getSelectedIndex() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SIO_A_OUT,
		getSioOutValue( this.comboSioOutA ) );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_SIO_B_OUT,
		getSioOutValue( this.comboSioOutB ) );
      int ctcIOBaseAddr = applyIOAddrInput(
				selected,
				this.btnCtcEnabled,
				CustomSys.PROP_CTC_ENABLED,
				this.docCtcIOBaseAddr,
				CustomSys.PROP_CTC_IOBASEADDR,
				props );
      int kcNetIOBaseAddr = applyIOAddrInput(
				selected,
				this.btnKCNetEnabled,
				CustomSys.PROP_KCNET_ENABLED,
				this.docKCNetIOBaseAddr,
				CustomSys.PROP_KCNET_IOBASEADDR,
				props );
      int vdipIOBaseAddr = applyIOAddrInput(
				selected,
				this.btnVdipEnabled,
				CustomSys.PROP_VDIP_ENABLED,
				this.docVdipIOBaseAddr,
				CustomSys.PROP_VDIP_IOBASEADDR,
				props );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_UNUSED_PORT_VALUE,
		this.docUnusedPortValue.intValue() );

      // Tab FDC
      boolean fdcEnabled = this.btnFdcEnabled.isSelected();
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_FDC_ENABLED,
		fdcEnabled );
      int fdcDataIOAddr = applyIOAddrInput(
				selected,
				LABEL_FDC_DATA_IOADDR,
				this.docFdcDataIOAddr,
				CustomSys.PROP_FDC_DATA_IOADDR,
				props );
      int fdcStatusIOAddr = applyIOAddrInput(
				selected,
				LABEL_FDC_STATUS_IOADDR,
				this.docFdcStatusIOAddr,
				CustomSys.PROP_FDC_STATUS_IOADDR,
				props );
      int fdcTCIOAddr = applyIOAddrInput(
				selected,
				LABEL_FDC_TC_IOADDR,
				this.docFdcTCIOAddr,
				CustomSys.PROP_FDC_TC_IOADDR,
				props );
      int fdcTCIOMask   = 0;
      int fdcTCIOValue  = 0;
      int fdcTCIOBitIdx = this.comboFdcTCIOBit.getSelectedIndex();
      if( fdcTCIOBitIdx > 0 ) {
	fdcTCIOMask = (0x01 << ((fdcTCIOBitIdx - 1) % 8));
	if( fdcTCIOBitIdx < 9 ) {
	  fdcTCIOValue = fdcTCIOMask;
	}
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_FDC_TC_IOMASK,
		fdcTCIOMask );
      EmuUtil.setProperty(
		props,
		this.propPrefix + CustomSys.PROP_FDC_TC_IOVALUE,
		fdcTCIOValue );

      // Tab GIDE
      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      // Tab AutoLoad
      tab = this.tabAutoLoad;
      this.tabAutoLoad.applyInput( props, selected );

      // Tab AutoInput
      tab = this.tabAutoInput;
      this.tabAutoInput.applyInput( props, selected );

      // Abhaengigkeiten und IO-Adresskonflikte pruefen
      if( selected ) {
	if( this.btnKeyboardPioAhs.isSelected()
	    && this.btnKeyboardPioAbit7.isSelected()
	    && this.btnKeyboardPioBhs.isSelected()
	    && this.btnKeyboardPioBbit7.isSelected()
	    && !this.btnPioEnabled.isSelected() )
	{
	  this.tabbedPane.setSelectedComponent( this.tabIO );
	  throw new UserInputException(
			"Wenn die Tastatur an einer PIO angeschlossen ist,\n"
				+ "muss auch eine PIO emuliert werden." );
	}
	if( this.btnKeyboardSioA.isSelected()
	    && this.btnKeyboardSioB.isSelected()
	    && !this.btnSioEnabled.isSelected() )
	{
	  this.tabbedPane.setSelectedComponent( this.tabIO );
	  throw new UserInputException(
			"Wenn die Tastatur an einer SIO angeschlossen ist,\n"
				+ "muss auch eine SIO emuliert werden." );
	}
	Map<Integer,String> ioAddrMap = new HashMap<>();
	tab = this.tabKeyboard;
	if( this.btnKeyboardPortRaw.isSelected() ) {
	  addToIOAddrMasp(
			ioAddrMap,
			keyboardIOAddr,
			1,
			"Eingabeport Tastatur" );
	}
	tab = this.tabIO;
	if( pioIOBaseAddr >= 0 ) {
	  addToIOAddrMasp( ioAddrMap, pioIOBaseAddr, 4, "PIO" );
	}
	if( sioIOBaseAddr >= 0 ) {
	  addToIOAddrMasp( ioAddrMap, sioIOBaseAddr, 4, "SIO" );
	}
	if( ctcIOBaseAddr >= 0 ) {
	  addToIOAddrMasp( ioAddrMap, ctcIOBaseAddr, 4, "CTC" );
	}
	if( kcNetIOBaseAddr >= 0 ) {
	  addToIOAddrMasp( ioAddrMap, kcNetIOBaseAddr, 4, "PIO" );
	}
	if( vdipIOBaseAddr >= 0 ) {
	  addToIOAddrMasp( ioAddrMap, vdipIOBaseAddr, 4, "USB" );
	}
	tab = this.tabFDC;
	if( fdcDataIOAddr >= 0 ) {
	  addToIOAddrMasp(
			ioAddrMap,
			fdcDataIOAddr,
			1,
			"FDC Datenregister" );
	}
	if( fdcStatusIOAddr >= 0 ) {
	  addToIOAddrMasp(
			ioAddrMap,
			fdcStatusIOAddr,
			1,
			"FDC Statusregister" );
	}
	if( fdcTCIOAddr >= 0 ) {
	  addToIOAddrMasp(
			ioAddrMap,
			fdcTCIOAddr,
			1,
			"FDC Terminal Count" );
	}
	tab = this.tabGIDE;
	int gideIOBaseAddr = this.tabGIDE.getAppliedIOBaseAddr();
	if( gideIOBaseAddr >= 0 ) {
	  addToIOAddrMasp(
			ioAddrMap,
			gideIOBaseAddr,
			16,
			"GIDE" );
	}
      }
    }
    catch( UserCancelException ex ) {}
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
      if( src == this.btnRomAdd ) {
	rv = true;
	doRomAdd();
      }
      else if( src == this.btnRomEdit ) {
	rv = true;
	doRomEdit();
      }
      else if( src == this.btnRomRemove ) {
	rv = true;
	doRomRemove();
      }
      else if( src == this.btnRomUp ) {
	rv = true;
	doRomMove( -1 );
      }
      else if( src == this.btnRomDown ) {
	rv = true;
	doRomMove( 1 );
      }
      else if( (src == this.btnKeyboardNone)
	       || (src == this.btnKeyboardPortRaw)
	       || (src == this.btnKeyboardPioAhs)
	       || (src == this.btnKeyboardPioAbit7)
	       || (src == this.btnKeyboardPioBhs)
	       || (src == this.btnKeyboardPioBbit7)
	       || (src == this.btnKeyboardSioA)
	       || (src == this.btnKeyboardSioB) )
      {
	rv = true;
	updKeyboardFieldsEnabled();
	fireDataChanged();
      }
      else if( src == this.btnScreenEnabled ) {
	rv = true;
	updScreenFieldsEnabled();
	fireDataChanged();
      }
      else if( src == this.btnPioEnabled ) {
	rv = true;
	updPioFieldsEnabled();
	fireDataChanged();
      }
      else if( src == this.btnSioEnabled ) {
	rv = true;
	updSioFieldsEnabled();
	fireDataChanged();
      }
      else if( src == this.btnCtcEnabled ) {
	rv = true;
	updCtcFieldsEnabled();
	fireDataChanged();
      }
      else if( src == this.btnKCNetEnabled ) {
	rv = true;
	updKCNetFieldsEnabled();
	fireDataChanged();
      }
      else if( src == this.btnVdipEnabled ) {
	rv = true;
	updVdipFieldsEnabled();
	fireDataChanged();
      }
      else if( src == this.btnFdcEnabled ) {
	rv = true;
	updFdcFieldsEnabled();
	fireDataChanged();
      }
      else if( (src instanceof AbstractButton)
	       || (src instanceof JComboBox) )
      {
	rv = true;
	fireDataChanged();
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
    this.settingsFrm.setWaitCursor( false );
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.fldAltFont.lookAndFeelChanged();
    this.tabGIDE.lookAndFeelChanged();
    this.tabAutoLoad.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    // Tab Allgemein
    this.fldTitle.setText( CustomSys.getTitle( props ) );
    this.fldSpeedMHz.setText(
	this.settingsFrm.getSpeedFmt().format(
		(float) CustomSys.getDefaultSpeedKHz( props ) / 1000F ) );

    // Tab ROM
    this.tableModelRom.setRows( CustomSys.getDeclaredROMs( props ) );

    // Tab Bildschirmausgabe
    this.btnScreenEnabled.setSelected( CustomSys.emulatesScreen( props ) );
    this.docScreenBegAddr.setValue(
		CustomSys.getScreenBegAddr( props ), 4 );
    this.comboScreenCols.setSelectedItem(
		Integer.toString( CustomSys.getScreenCols( props ) ) );
    this.comboScreenRows.setSelectedItem(
		Integer.toString( CustomSys.getScreenRows( props ) ) );
    this.fldAltFont.updFields( props );
    updScreenFieldsEnabled();

    // Tastatur
    switch( EmuUtil.getProperty(
			props,
			this.propPrefix + CustomSys.PROP_KEYBOARD_HW ) )
    {
      case CustomSys.VALUE_KEYBOARD_PORT_RAW:
	this.btnKeyboardPortRaw.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_PIO_A_HS:
	this.btnKeyboardPioAhs.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_PIO_A_BIT7:
	this.btnKeyboardPioAbit7.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_PIO_B_HS:
	this.btnKeyboardPioBhs.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_PIO_B_BIT7:
	this.btnKeyboardPioBbit7.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_SIO_A:
	this.btnKeyboardSioA.setSelected( true );
	break;
      case CustomSys.VALUE_KEYBOARD_SIO_B:
	this.btnKeyboardSioB.setSelected( true );
	break;
      default:
	this.btnKeyboardNone.setSelected( true );
    }
    this.docKeyboardIOAddr.setValue(
		CustomSys.getKeyboardIOAddr( props ),
		2 );
    this.btnKeyboardSwapCase.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + CustomSys.PROP_SWAP_KEY_CHAR_CASE,
			CustomSys.DEFAULT_SWAP_KEY_CHAR_CASE ) );
    updKeyboardFieldsEnabled();

    // E/A-Bausteine
    this.btnPioEnabled.setSelected( CustomSys.emulatesPIO( props ) );
    this.docPioIOBaseAddr.setValue(
		CustomSys.getPioIOBaseAddr( props ), 2 );
    updPioFieldsEnabled();

    this.btnSioEnabled.setSelected( CustomSys.emulatesSIO( props ) );
    this.docSioIOBaseAddr.setValue(
		CustomSys.getSioIOBaseAddr( props ), 2 );
    setSelectedSioClock(
		this.comboSioClockA,
		props,
		CustomSys.PROP_SIO_A_CLOCK );
    setSelectedSioClock(
		this.comboSioClockB,
		props,
		CustomSys.PROP_SIO_B_CLOCK );
    setSelectedSioOut(
		this.comboSioOutA,
		props,
		CustomSys.PROP_SIO_A_OUT );
    setSelectedSioOut(
		this.comboSioOutB,
		props,
		CustomSys.PROP_SIO_B_OUT );
    updSioFieldsEnabled();

    this.btnCtcEnabled.setSelected( CustomSys.emulatesCTC( props ) );
    this.docCtcIOBaseAddr.setValue(
		CustomSys.getCtcIOBaseAddr( props ), 2 );
    updCtcFieldsEnabled();

    this.btnKCNetEnabled.setSelected( CustomSys.emulatesKCNet( props ) );
    this.docKCNetIOBaseAddr.setValue(
		CustomSys.getKCNetIOBaseAddr( props ), 2 );
    updKCNetFieldsEnabled();

    this.btnVdipEnabled.setSelected( CustomSys.emulatesVDIP( props ) );
    this.docVdipIOBaseAddr.setValue(
		CustomSys.getVdipIOBaseAddr( props ), 2 );
    updVdipFieldsEnabled();

    this.docUnusedPortValue.setValue(
		CustomSys.getUnusedPortValue( props ), 2 );

    // Tab FDC
    this.btnFdcEnabled.setSelected( CustomSys.emulatesFDC( props ) );
    this.docFdcDataIOAddr.setValue(
		CustomSys.getFdcDataIOAddr( props ), 4 );
    this.docFdcStatusIOAddr.setValue(
		CustomSys.getFdcStatusIOAddr( props ), 4 );
    this.docFdcTCIOAddr.setValue(
		CustomSys.getFdcTCIOAddr( props ), 4 );
    int fdcTCIOBitIdx = 0;
    int fdcTCIOMask   = EmuUtil.getIntProperty(
		props,
		this.propPrefix + CustomSys.PROP_FDC_TC_IOMASK,
		0 ) & 0xFF;
    if( fdcTCIOMask != 0 ) {
      fdcTCIOBitIdx++;
      while( (fdcTCIOMask & 0x01) != 0 ) {
	fdcTCIOMask <<= 1;
	fdcTCIOBitIdx++;
      }
      if( (EmuUtil.getIntProperty(
		props,
		this.propPrefix + CustomSys.PROP_FDC_TC_IOVALUE,
		0xFF ) & 0xFF) == 0 )
      {
	fdcTCIOBitIdx += 8;
      }
    }

    // sonstige Tabs
    this.tabGIDE.updFields( props );
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );
  }


	/* --- Aktionen --- */

  private void doRomAdd()
  {
    boolean hasBootROM = false;
    int     nRoms      = this.tableModelRom.getRowCount();
    for( int i = 0; i < nRoms; i++ ) {
      CustomSysROM rom = this.tableModelRom.getRow( i );
      if( rom != null ) {
	if( rom.isBootROM() && rom.isEnabledAfterReset()) {
	  hasBootROM = true;
	  break;
	}
      }
    }
    CustomSysROM rom = CustomSysROMSettingsDlg.showNewROMDlg(
						this.settingsFrm,
						!hasBootROM );
    if( rom != null ) {
      this.tableModelRom.addRow( rom );
      fireDataChanged();
    }
  }


  private void doRomEdit()
  {
    int[] rows = this.tableRom.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int          row = this.tableRom.convertRowIndexToModel( rows[ 0 ] );
	CustomSysROM rom = this.tableModelRom.getRow( row );
	if( rom != null ) {
	  rom = CustomSysROMSettingsDlg.showDlg( this.settingsFrm, rom );
	  if( rom != null ) {
	    this.tableModelRom.setRow( row, rom );
	    fireDataChanged();
	  }
	}
      }
    }
  }


  private void doRomMove( int diffRows )
  {
    int[] rows = this.tableRom.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int nRows = this.tableModelRom.getRowCount();
	int row1  = rows[ 0 ];
	int row2  = row1 + diffRows;
	if( (row1 >= 0) && (row1 < nRows)
	    && (row2 >= 0) && (row2 < nRows) )
	{
	  CustomSysROM rom1 = this.tableModelRom.getRow( row1 );
	  CustomSysROM rom2 = this.tableModelRom.getRow( row2 );
	  if( (rom1 != null) && (rom2 != null) ) {
	    this.tableModelRom.setRow( row1, rom2 );
	    this.tableModelRom.setRow( row2, rom1 );
	    EmuUtil.fireSelectRow( this.tableRom, row2 );
	    fireDataChanged();
	  }
	}
      }
    }
  }


  private void doRomRemove()
  {
    int[] rows = this.tableRom.getSelectedRows();
    if( rows != null ) {
      if( rows.length > 0 ) {
	Arrays.sort( rows );
	for( int i = rows.length - 1; i >= 0; --i ) {
	  int row = this.tableRom.convertRowIndexToModel( rows[ i ] );
	  if( row >= 0 ) {
	    this.tableModelRom.removeRow( row );
	  }
	}
	fireDataChanged();
      }
    }
  }


	/* --- private Methoden --- */

  private void addToIOAddrMasp(
			Map<Integer,String> ioAddrMap,
			int                 ioAddr,
			int                 ioAddrCount,
			String              text ) throws UserInputException
  {
    for( int i = 0; i < ioAddrCount; i++ ) {
      String oldText = ioAddrMap.get( ioAddr );
      if( oldText != null ) {
	throw new UserInputException(
		String.format(
			"E/A-Adresskonflikt bei Adresse %02Xh:\n"
				+ "%s <> %s\n\n"
				+ "Den E/A-Komponenten m\u00FCssen"
				+ " unterschiedliche\n"
				+ "Adressen bzw. Adressbereiche zugewiesen"
				+ " werden.\n"
				+ "PIO, SIO, CTC, KCNet und USB verwenden"
				+ " jeweils 4\n"
				+ "und GIDE 16 aufeinanderfolgende Adressen.",
			ioAddr,
			oldText,
			text ) );
      }
      ioAddrMap.put( ioAddr++, text );
    }
  }


  private int applyIOAddrInput(
			boolean     selected,
			JCheckBox   btnEnabled,
			String      propEnabled,
			HexDocument docIOAddr,
			String      propIOAddr,
			Properties  props ) throws UserInputException
  {
    boolean enabled = btnEnabled.isSelected();
    EmuUtil.setProperty( props, this.propPrefix + propEnabled, enabled );
    return applyIOAddrInput(
			selected && enabled,
			btnEnabled.getText(),
			docIOAddr,
			propIOAddr,
			props );
  }


  private int applyIOAddrInput(
			boolean     selected,
			String      labelText,
			HexDocument docIOAddr,
			String      propIOAddr,
			Properties  props ) throws UserInputException
  {
    int rv      = -1;
    int ioAddr  = -1;
    try {
      ioAddr = docIOAddr.intValue();
      if( selected ) {
	if( ioAddr > 0xFF ) {
	  throw new UserInputException( labelText + TEXT_INVALID_ADDR );
	}
	rv = ioAddr;
      }
    }
    catch( NumberFormatException ex ) {
      if( selected ) {
	throw new UserInputException( labelText + TEXT_INVALID_ADDR );
      }
    }
    if( ioAddr >= 0 ) {
      EmuUtil.setProperty(
		props,
		this.propPrefix + propIOAddr,
		ioAddr );
    }
    return rv;
  }


  private void docChanged( DocumentEvent e )
  {
    Document doc = e.getDocument();
    if( (doc == this.docTitle)
	|| (doc == this.docSpeedMHz)
	|| (doc == this.docScreenBegAddr)
	|| (doc == this.docKeyboardIOAddr)
	|| (doc == this.docPioIOBaseAddr)
	|| (doc == this.docSioIOBaseAddr)
	|| (doc == this.docCtcIOBaseAddr)
	|| (doc == this.docKCNetIOBaseAddr)
	|| (doc == this.docVdipIOBaseAddr)
	|| (doc == this.docFdcDataIOAddr)
	|| (doc == this.docFdcStatusIOAddr)
	|| (doc == this.docFdcTCIOAddr)
	|| (doc == this.docUnusedPortValue) )
    {
      fireDataChanged();
    }
  }

  private static String getSioOutValue( JComboBox combo )
  {
    return combo.getSelectedIndex() == 1 ? CustomSys.VALUE_PRINTER : "";
  }


  private void setSelectedSioClock(
			JComboBox  combo,
			Properties props,
			String     propName )
  {
    int idx = EmuUtil.getIntProperty(
			props,
			this.propPrefix + propName,
			0 );
    if( (idx >= 0) && (idx < combo.getItemCount()) ) {
      try {
	combo.setSelectedIndex( idx );
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  private void setSelectedSioOut(
			JComboBox  combo,
			Properties props,
			String     propName )
  {
    int idx = 0;
    if( EmuUtil.getProperty(
		props,
		this.propPrefix + propName ).equals(
					CustomSys.VALUE_PRINTER ) )
    {
      idx = 1;
    }
    if( (idx >= 0) && (idx < combo.getItemCount()) ) {
      try {
	combo.setSelectedIndex( idx );
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  private void updCtcFieldsEnabled()
  {
    this.fldCtcIOBaseAddr.setEnabled(
			this.btnCtcEnabled.isSelected() );
  }


  private void updFdcFieldsEnabled()
  {
    boolean state = this.btnFdcEnabled.isSelected();
    this.labelFdcDataIOAddr.setEnabled( state );
    this.labelFdcStatusIOAddr.setEnabled( state );
    this.labelFdcTCIOAddr.setEnabled( state );
    this.labelFdcTCBit.setEnabled( state );
    this.fldFdcDataIOAddr.setEnabled( state );
    this.fldFdcStatusIOAddr.setEnabled( state );
    this.fldFdcTCIOAddr.setEnabled( state );
    this.comboFdcTCIOBit.setEnabled( state );
  }


  private void updKCNetFieldsEnabled()
  {
    this.fldKCNetIOBaseAddr.setEnabled(
			this.btnKCNetEnabled.isSelected() );
  }


  private void updKeyboardFieldsEnabled()
  {
    boolean state = this.btnKeyboardPortRaw.isSelected();
    this.fldKeyboardIOAddr.setEnabled( state );
  }


  private void updPioFieldsEnabled()
  {
    this.fldPioIOBaseAddr.setEnabled(
			this.btnPioEnabled.isSelected() );
  }


  private void updScreenFieldsEnabled()
  {
    boolean state = this.btnScreenEnabled.isSelected();
    this.labelScreenBegAddr.setEnabled( state );
    this.labelScreenCols.setEnabled( state );
    this.labelScreenRows.setEnabled( state );
    this.fldScreenBegAddr.setEnabled( state );
    this.comboScreenCols.setEnabled( state );
    this.comboScreenRows.setEnabled( state );
    this.fldAltFont.setEnabled( state );
  }


  private void updSioFieldsEnabled()
  {
    boolean state = this.btnSioEnabled.isSelected();
    this.fldSioIOBaseAddr.setEnabled( state );
    this.labelSioClockA.setEnabled( state );
    this.labelSioClockB.setEnabled( state );
    this.labelSioOutA.setEnabled( state );
    this.labelSioOutB.setEnabled( state );
    this.comboSioClockA.setEnabled( state );
    this.comboSioClockB.setEnabled( state );
    this.comboSioOutA.setEnabled( state );
    this.comboSioOutB.setEnabled( state );
  }


  private void updVdipFieldsEnabled()
  {
    this.fldVdipIOBaseAddr.setEnabled(
			this.btnVdipEnabled.isSelected() );
  }
}
