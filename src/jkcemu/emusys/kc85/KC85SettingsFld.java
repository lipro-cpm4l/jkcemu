/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen der Computer HC900 und KC85/2..5
 */

package jkcemu.emusys.kc85;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.PopupMenusOwner;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.KC85;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.AutoLoadSettingsFld;
import jkcemu.settings.SettingsFrm;


public class KC85SettingsFld
			extends AbstractSettingsFld
			implements
				ListSelectionListener,
				MouseListener,
				PopupMenusOwner
{
  private static final String ACTION_MODULE_ADD_PREFIX = "kc85.module.add.";

  private static final String MT_RAM = "RAM";
  private static final String MT_ROM = "ROM";
  private static final String MT_ETC = "ETC";

  private static final int MOD_IDX_TYPE = 0;
  private static final int MOD_IDX_NAME = 1;
  private static final int MOD_IDX_DESC = 2;

  private static final String[][] modules = {
	{ MT_ETC, "M003", "V.24 mit angeschlossenem Drucker" },
	{ MT_ROM, "M006", "BASIC" },
	{ MT_ETC, "M008", "Joystick" },
	{ MT_RAM, "M011", "64 KByte RAM" },
	{ MT_ROM, "M012", "TEXOR" },
	{ MT_ETC, "M021", "Joystick/Centronics mit angeschlossenem Drucker" },
	{ MT_RAM, "M022", "16 KByte Expander RAM" },
	{ MT_ROM, "M025", "8 KByte User PROM" },
	{ MT_ROM, "M026", "Forth" },
	{ MT_ROM, "M027", "Development" },
	{ MT_ROM, "M028", "16 KByte User PROM" },
	{ MT_RAM, "M032", "256 KByte Segmented RAM" },
	{ MT_ROM, "M033", "TypeStar" },
	{ MT_RAM, "M034", "512 KByte Segmented RAM" },
	{ MT_RAM, "M035", "1 MByte Segmented RAM" },
	{ MT_RAM, "M035x4", "4 MByte Segmented RAM" },
	{ MT_RAM, "M036", "128 KByte Segmented RAM" },
	{ MT_ROM, "M040", "8/16 KByte User PROM" },
	{ MT_ROM, "M041", "2 x 16 KByte EEPROM" },
	{ MT_ROM, "M045", "4x8 KByte User PROM" },
	{ MT_ROM, "M046", "8x8 KByte User PROM" },
	{ MT_ROM, "M047", "16x8 KByte User PROM" },
	{ MT_ROM, "M048", "16x16 KByte User PROM" },
	{ MT_ETC, "M052", M052.DESCRIPTION },
	{ MT_ETC, "M066", "Sound" },
	{ MT_RAM, "M120", "8 KByte CMOS RAM" },
	{ MT_RAM, "M122", "16 KByte CMOS RAM" },
	{ MT_RAM, "M124", "32 KByte CMOS RAM" } };

  private static final String[] altRomTitles2 = {
				"CAOS-ROM E000-E7FF",
				"CAOS-ROM F000-F7FF",
				"ROM f\u00FCr erstes M052 (USB/Netzwerk)",
				"ROM f\u00FCr weitere M052 (nur USB)" };

  private static final String[] altRomKeys2 = {
				KC85.PROP_ROM_CAOS_E_FILE,
				KC85.PROP_ROM_CAOS_F_FILE,
				KC85.PROP_ROM_M052_FILE,
				KC85.PROP_ROM_M052USB_FILE };

  private static final String[] altRomTitles3 = {
				"BASIC-ROM C000-DFFF",
				"CAOS-ROM E000-FFFF",
				"ROM f\u00FCr erstes M052 (USB/Netzwerk)",
				"ROM f\u00FCr weitere M052 (nur USB)" };

  private static final String[] altRomKeys3 = {
				KC85.PROP_ROM_BASIC_FILE,
				KC85.PROP_ROM_CAOS_E_FILE,
				KC85.PROP_ROM_M052_FILE,
				KC85.PROP_ROM_M052USB_FILE };

  private static final String[] altRomTitles4 = {
				"BASIC-ROM C000-DFFF",
				"CAOS-ROM C000-CFFF (oder C000-DFFF)",
				"CAOS-ROM E000-FFFF",
				"ROM f\u00FCr erstes M052 (USB/Netzwerk)",
				"ROM f\u00FCr weitere M052 (nur USB)" };

  private static final String[] altRomKeys4 = {
				KC85.PROP_ROM_BASIC_FILE,
				KC85.PROP_ROM_CAOS_C_FILE,
				KC85.PROP_ROM_CAOS_E_FILE,
				KC85.PROP_ROM_M052_FILE,
				KC85.PROP_ROM_M052USB_FILE };

  private static final String[] altRomTitles5 = {
				"BASIC-/USER-ROM C000-DFFF (4x8K)",
				"CAOS-ROM C000-DFFF",
				"CAOS-ROM E000-FFFF",
				"ROM f\u00FCr erstes M052 (USB/Netzwerk)",
				"ROM f\u00FCr weitere M052 (nur USB)" };

  private static final String[] altRomKeys5 = {
				KC85.PROP_ROM_BASIC_FILE,
				KC85.PROP_ROM_CAOS_C_FILE,
				KC85.PROP_ROM_CAOS_E_FILE,
				KC85.PROP_ROM_M052_FILE,
				KC85.PROP_ROM_M052USB_FILE };

  private int                  kcTypeNum;
  private String               modulePropPrefix;
  private String[]             altRomTitles;
  private String[]             altRomKeys;
  private ListSelectionModel   selModelModule;
  private KC85ModuleTableModel tableModelModule;
  private JTable               tableModule;
  private JTabbedPane          tabbedPane;
  private JPanel               tabModule;
  private JPanel               tabDiskStation;
  private JPanel               tabROM;
  private JPanel               tabEtc;
  private AutoLoadSettingsFld  tabAutoLoad;
  private AutoInputSettingsFld tabAutoInput;
  private JPopupMenu           popupModuleAdd;
  private JPopupMenu           popupModuleChange;
  private JMenuItem            mnuModuleEdit;
  private JMenuItem            mnuModuleIntoD001orD002;
  private JMenuItem            mnuModuleIntoDiskStation;
  private JButton              btnModuleAdd;
  private JButton              btnModuleChange;
  private JButton              btnModuleRemove;
  private JButton              btnModuleUp;
  private JButton              btnModuleDown;
  private JRadioButton         rbDiskStationNone;
  private JRadioButton         rbDiskStationD004_20;
  private JRadioButton         rbDiskStationD004_33;
  private JRadioButton         rbDiskStationD008;
  private JLabel               labelDiskStationRom;
  private JLabel               labelDiskStationSpeed;
  private JRadioButton         rbDiskStationSpeedDefault;
  private JRadioButton         rbDiskStationSpeed8MHz;
  private JRadioButton         rbDiskStationSpeed16MHz;
  private FileNameFld          fldDiskStationRomFile;
  private JButton              btnDiskStationRomFileSelect;
  private JButton              btnDiskStationRomFileRemove;
  private GIDESettingsFld      tabGIDE;
  private FileNameFld[]        altRomTextFlds;
  private JButton[]            altRomSelectBtns;
  private JButton[]            altRomRemoveBtns;
  private JCheckBox            cbKeysDirectToBuf;
  private JCheckBox            cbPasteFast;
  private JCheckBox            cbVideoTiming;


  public KC85SettingsFld(
		SettingsFrm settingsFrm,
		String      propPrefix,
		int         kcTypeNum )
  {
    super( settingsFrm, propPrefix );
    this.kcTypeNum        = kcTypeNum;
    this.modulePropPrefix = propPrefix + KC85.PROP_MODULE_PREFIX;

    if( kcTypeNum < 3 ) {
      this.altRomTitles = altRomTitles2;
      this.altRomKeys   = altRomKeys2;
    } else if( kcTypeNum == 3 ) {
      this.altRomTitles = altRomTitles3;
      this.altRomKeys   = altRomKeys3;
    } else if( kcTypeNum == 4 ) {
      this.altRomTitles = altRomTitles4;
      this.altRomKeys   = altRomKeys4;
    } else {
      this.altRomTitles = altRomTitles5;
      this.altRomKeys   = altRomKeys5;
    }

    setLayout( new BorderLayout() );

    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Module
    this.tabModule = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Module", this.tabModule );

    GridBagConstraints gbcModule = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tableModelModule = new KC85ModuleTableModel();

    this.tableModule = GUIFactory.createTable( this.tableModelModule );
    this.tableModule.setAutoCreateRowSorter( false );
    this.tableModule.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
    this.tableModule.setColumnSelectionAllowed( false );
    this.tableModule.setDragEnabled( false );
    this.tableModule.setFillsViewportHeight( false );
    this.tableModule.setPreferredScrollableViewportSize(
						new Dimension( 1, 1 ) );
    this.tableModule.setRowSelectionAllowed( true );
    this.tableModule.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    this.tableModule.addMouseListener( this );

    this.tabModule.add(
		GUIFactory.createScrollPane( this.tableModule ),
		gbcModule );

    EmuUtil.setTableColWidths( this.tableModule, 60, 60, 300 );

    JPanel panelModBtnRight = GUIFactory.createPanel(
					new GridLayout( 2, 1, 5, 5 ) );
    gbcModule.fill    = GridBagConstraints.NONE;
    gbcModule.weightx = 0.0;
    gbcModule.weighty = 0.0;
    gbcModule.gridx++;
    this.tabModule.add( panelModBtnRight, gbcModule );

    this.btnModuleUp = GUIFactory.createRelImageResourceButton(
						this,
						"nav/up.png",
						"Auf" );
    this.btnModuleUp.addActionListener( this );
    panelModBtnRight.add( this.btnModuleUp );

    this.btnModuleDown = GUIFactory.createRelImageResourceButton(
						this,
						"nav/down.png",
						"Ab" );
    this.btnModuleDown.addActionListener( this );
    panelModBtnRight.add( this.btnModuleDown );

    JPanel panelModBtnBottom = GUIFactory.createPanel(
					new GridLayout( 1, 3, 5, 5 ) );
    gbcModule.gridx = 0;
    gbcModule.gridy++;
    this.tabModule.add( panelModBtnBottom, gbcModule );

    this.btnModuleAdd = GUIFactory.createButtonAdd();
    this.btnModuleAdd.addActionListener( this );
    this.btnModuleAdd.addKeyListener( this );
    panelModBtnBottom.add( this.btnModuleAdd );

    this.btnModuleChange = GUIFactory.createButton( "\u00C4ndern" );
    this.btnModuleChange.addActionListener( this );
    this.btnModuleChange.addKeyListener( this );
    panelModBtnBottom.add( this.btnModuleChange );

    this.btnModuleRemove = GUIFactory.createButtonRemove();
    this.btnModuleRemove.addActionListener( this );
    this.btnModuleRemove.addKeyListener( this );
    panelModBtnBottom.add( this.btnModuleRemove );

    this.selModelModule = this.tableModule.getSelectionModel();
    if( this.selModelModule != null ) {
      this.selModelModule.addListSelectionListener( this );
      this.btnModuleUp.setEnabled( false );
      this.btnModuleDown.setEnabled( false );
      this.btnModuleChange.setEnabled( false );
      this.btnModuleRemove.setEnabled( false );
    }

    this.popupModuleAdd = GUIFactory.createPopupMenu();

    JMenu mnuModuleRAM = GUIFactory.createMenu( "RAM-Module" );
    this.popupModuleAdd.add( mnuModuleRAM );

    JMenu mnuModuleROM = GUIFactory.createMenu( "ROM-Module" );
    this.popupModuleAdd.add( mnuModuleROM );

    JMenu mnuModuleEtc = GUIFactory.createMenu( "Sonstige Module" );
    this.popupModuleAdd.add( mnuModuleEtc );

    for( int i = 0; i < modules.length; i++ ) {
      String modName = modules[ i ][ MOD_IDX_NAME ];
      if( (kcTypeNum < 3) || !modName.equals( "M006" ) ) {
	JMenuItem modItem = GUIFactory.createMenuItem(
				String.format(
					"%s - %s",
					modName,
					modules[ i ][ MOD_IDX_DESC ] ) );
	modItem.setActionCommand( ACTION_MODULE_ADD_PREFIX + modName );
	modItem.addActionListener( this );
	switch( modules[ i ][ MOD_IDX_TYPE ] ) {
	  case MT_RAM:
	    mnuModuleRAM.add( modItem );
	    break;
	  case MT_ROM:
	    mnuModuleROM.add( modItem );
	    break;
	  case MT_ETC:
	    mnuModuleEtc.add( modItem );
	    break;
	}
      }
    }

    this.popupModuleChange = GUIFactory.createPopupMenu();

    this.mnuModuleEdit = GUIFactory.createMenuItem( "Bearbeiten..." );
    this.mnuModuleEdit.addActionListener( this );
    this.popupModuleChange.add( this.mnuModuleEdit );

    this.mnuModuleIntoD001orD002 = GUIFactory.createMenuItem(
						"In D001/D002 stecken" );
    this.mnuModuleIntoD001orD002.addActionListener( this );
    this.popupModuleChange.add( this.mnuModuleIntoD001orD002 );

    this.mnuModuleIntoDiskStation = GUIFactory.createMenuItem(
						"In D004/D008 stecken" );
    this.mnuModuleIntoDiskStation.addActionListener( this );
    this.popupModuleChange.add( this.mnuModuleIntoDiskStation );


    // Tab D004/D008
    this.tabDiskStation = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "D004/D008", this.tabDiskStation );

    GridBagConstraints gbcDiskStation = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpDiskStation = new ButtonGroup();

    this.rbDiskStationNone = GUIFactory.createRadioButton(
		"Keine Floppy-Disk-Erweiterung emulieren",
		true );
    grpDiskStation.add( this.rbDiskStationNone );
    this.rbDiskStationNone.addActionListener( this );
    this.tabDiskStation.add( this.rbDiskStationNone, gbcDiskStation );

    this.rbDiskStationD004_20 = GUIFactory.createRadioButton(
		"Floppy-Disk-Erweiterung D004 mit Original-ROM-Version 2.0"
					+ " (optional mit GIDE)" );
    grpDiskStation.add( this.rbDiskStationD004_20 );
    this.rbDiskStationD004_20.addActionListener( this );
    gbcDiskStation.insets.top = 0;
    gbcDiskStation.gridy++;
    this.tabDiskStation.add( this.rbDiskStationD004_20, gbcDiskStation );

    this.rbDiskStationD004_33 = GUIFactory.createRadioButton(
		"Floppy-Disk-Erweiterung D004 mit ROM-Version 3.31"
					+ " (optional mit GIDE)" );
    grpDiskStation.add( this.rbDiskStationD004_33 );
    this.rbDiskStationD004_33.addActionListener( this );
    gbcDiskStation.insets.top = 0;
    gbcDiskStation.gridy++;
    this.tabDiskStation.add( this.rbDiskStationD004_33, gbcDiskStation );

    this.rbDiskStationD008 = GUIFactory.createRadioButton(
		"Floppy-Disk-Erweiterung D008 (immer mit GIDE)" );
    grpDiskStation.add( this.rbDiskStationD008 );
    this.rbDiskStationD008.addActionListener( this );
    gbcDiskStation.gridy++;
    this.tabDiskStation.add( this.rbDiskStationD008, gbcDiskStation );

    this.labelDiskStationRom = GUIFactory.createLabel(
				"Alternativer D004/D008-ROM-Inhalt:" );
    gbcDiskStation.insets.top = 20;
    gbcDiskStation.gridy++;
    this.tabDiskStation.add( this.labelDiskStationRom, gbcDiskStation );

    this.fldDiskStationRomFile = new FileNameFld();
    gbcDiskStation.fill        = GridBagConstraints.HORIZONTAL;
    gbcDiskStation.weightx     = 1.0;
    gbcDiskStation.insets.top  = 0;
    gbcDiskStation.gridwidth   = 1;
    gbcDiskStation.gridy++;
    this.tabDiskStation.add( this.fldDiskStationRomFile, gbcDiskStation );

    this.btnDiskStationRomFileSelect
		= GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_SELECT_ROM_FILE );
    this.btnDiskStationRomFileSelect.addActionListener( this );
    gbcDiskStation.fill        = GridBagConstraints.NONE;
    gbcDiskStation.weightx     = 0.0;
    gbcDiskStation.insets.left = 0;
    gbcDiskStation.gridx++;
    this.tabDiskStation.add(
		this.btnDiskStationRomFileSelect,
		gbcDiskStation );

    this.btnDiskStationRomFileRemove
		= GUIFactory.createRelImageResourceButton(
					this,
					"file/delete.png",
					EmuUtil.TEXT_REMOVE_ROM_FILE );
    this.btnDiskStationRomFileRemove.addActionListener( this );
    gbcDiskStation.gridx++;
    this.tabDiskStation.add(
		this.btnDiskStationRomFileRemove,
		gbcDiskStation );

    this.labelDiskStationSpeed = GUIFactory.createLabel( "Taktfrequenz:" );
    gbcDiskStation.insets.left = 5;
    gbcDiskStation.insets.top  = 20;
    gbcDiskStation.gridwidth   = GridBagConstraints.REMAINDER;
    gbcDiskStation.gridx       = 0;
    gbcDiskStation.gridy++;
    this.tabDiskStation.add( this.labelDiskStationSpeed, gbcDiskStation );

    JPanel panelDiskStationSpeed = GUIFactory.createPanel(
			new FlowLayout( FlowLayout.LEFT, 10, 0 ) );
    gbcDiskStation.insets.top    = 0;
    gbcDiskStation.insets.bottom = 5;
    gbcDiskStation.gridwidth     = GridBagConstraints.REMAINDER;
    gbcDiskStation.gridy++;
    this.tabDiskStation.add( panelDiskStationSpeed, gbcDiskStation );

    ButtonGroup grpDiskStationSpeed = new ButtonGroup();

    this.rbDiskStationSpeedDefault = GUIFactory.createRadioButton(
							"Original",
							true );
    this.rbDiskStationSpeedDefault.addActionListener( this );
    grpDiskStationSpeed.add( this.rbDiskStationSpeedDefault );
    panelDiskStationSpeed.add( this.rbDiskStationSpeedDefault );

    this.rbDiskStationSpeed8MHz = GUIFactory.createRadioButton( "8 MHz" );
    this.rbDiskStationSpeed8MHz.addActionListener( this );
    grpDiskStationSpeed.add( this.rbDiskStationSpeed8MHz );
    panelDiskStationSpeed.add( this.rbDiskStationSpeed8MHz );

    this.rbDiskStationSpeed16MHz = GUIFactory.createRadioButton( "16 MHz" );
    this.rbDiskStationSpeed16MHz.addActionListener( this );
    grpDiskStationSpeed.add( this.rbDiskStationSpeed16MHz );
    panelDiskStationSpeed.add( this.rbDiskStationSpeed16MHz );


    // Tab GIDE
    this.tabGIDE = new GIDESettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );


    // Tab ROM
    this.tabROM = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "ROM", this.tabROM );

    GridBagConstraints gbcROM = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.tabROM.add(
		GUIFactory.createLabel( "Alternative ROM-Inhalte:" ),
		gbcROM );

    this.altRomTextFlds   = new FileNameFld[ this.altRomTitles.length ];
    this.altRomSelectBtns = new JButton[ this.altRomTitles.length ];
    this.altRomRemoveBtns = new JButton[ this.altRomTitles.length ];
    for( int i = 0; i < this.altRomTitles.length; i++ ) {
      gbcROM.insets.left   = 50;
      gbcROM.insets.bottom = 0;
      gbcROM.gridx         = 0;
      gbcROM.gridy++;
      this.tabROM.add(
		GUIFactory.createLabel( this.altRomTitles[ i ] + ":" ),
		gbcROM );

      FileNameFld fld      = new FileNameFld();
      gbcROM.fill          = GridBagConstraints.HORIZONTAL;
      gbcROM.weightx       = 1.0;
      gbcROM.insets.top    = 0;
      gbcROM.insets.bottom = 5;
      gbcROM.gridy++;
      this.tabROM.add( fld, gbcROM );
      this.altRomTextFlds[ i ] = fld;

      JButton btn = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_SELECT_ROM_FILE );
      btn.addActionListener( this );
      gbcROM.fill        = GridBagConstraints.NONE;
      gbcROM.weightx     = 0.0;
      gbcROM.insets.left = 0;
      gbcROM.gridx++;
      this.tabROM.add( btn, gbcROM );
      this.altRomSelectBtns[ i ] = btn;

      btn = GUIFactory.createRelImageResourceButton(
					this,
					"file/delete.png",
					EmuUtil.TEXT_REMOVE_ROM_FILE );
      btn.addActionListener( this );
      gbcROM.gridx++;
      this.tabROM.add( btn, gbcROM );
      this.altRomRemoveBtns[ i ] = btn;
    }


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

    this.cbKeysDirectToBuf = GUIFactory.createCheckBox(
			"Schnellere Tastatureingaben durch direktes"
				+ " Schreiben in den Tastaturpuffer" );
    this.cbKeysDirectToBuf.addActionListener( this );
    this.tabEtc.add( this.cbKeysDirectToBuf, gbcEtc );

    this.cbPasteFast = GUIFactory.createCheckBox(
		"Einf\u00FCgen von Text aus der Zwischenablage"
			+ " direkt in den Tastaturpuffer",
		true );
    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    this.cbPasteFast.addActionListener( this );
    this.tabEtc.add( this.cbPasteFast, gbcEtc );

    gbcEtc.fill       = GridBagConstraints.HORIZONTAL;
    gbcEtc.weightx    = 1.0;
    gbcEtc.insets.top = 10;
    gbcEtc.gridy++;
    this.tabEtc.add( GUIFactory.createSeparator(), gbcEtc );

    gbcEtc.fill    = GridBagConstraints.NONE;
    gbcEtc.weightx = 0.0;
    gbcEtc.gridy++;
    this.tabEtc.add(
	GUIFactory.createLabel( "Die folgende Option ist"
		+ " f\u00FCr die korrekte Darstellung"
		+ " einiger Programme notwendig," ),
	gbcEtc );

    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    this.tabEtc.add(
	GUIFactory.createLabel( "ben\u00F6tigt aber relativ viel"
		+ " Rechenleistung. Sollte diese Leistung nicht zur" ),
	gbcEtc );

    gbcEtc.gridy++;
    this.tabEtc.add(
	GUIFactory.createLabel( "Verf\u00FCgung stehen,"
		+ " dann schalten Sie die Option bitte aus." ),
	gbcEtc );

    this.cbVideoTiming = GUIFactory.createCheckBox(
			"Zeitverhalten der Bildschirmsteuerung emulieren",
			KC85.getDefaultEmulateVideoTiming() );
    this.cbVideoTiming.addActionListener( this );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.cbVideoTiming, gbcEtc );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
		settingsFrm,
		propPrefix,
		this.kcTypeNum < 4 ?
			KC85.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX_2
			: KC85.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX_4,
		true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
		settingsFrm,
		propPrefix,
		KC85.getAutoInputCharSet(),
		KC85.DEFAULT_SWAP_KEY_CHAR_CASE,
		this.kcTypeNum < 4 ?
			KC85.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX_2
			: KC85.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX_4 );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Drag&Drop ermoeglichen
    (new DropTarget( this.fldDiskStationRomFile, this )).setActive( true );
    for( int i = 0; i < this.altRomTextFlds.length; i++ ) {
      (new DropTarget( this.altRomTextFlds[ i ], this )).setActive( true );
    }
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.selModelModule ) {
      int     nRows     = this.tableModule.getRowCount();
      int     nSelRows  = this.tableModule.getSelectedRowCount();
      int     selRowNum = this.tableModule.getSelectedRow();
      boolean stateOne  = (nSelRows == 1) && (selRowNum >= 0);
      boolean stateEdit = false;
      this.btnModuleUp.setEnabled( (nSelRows == 1) && (selRowNum > 0) );
      this.btnModuleDown.setEnabled( stateOne && (selRowNum < (nRows - 1)) );
      this.btnModuleRemove.setEnabled( nSelRows > 0 );
      this.btnModuleChange.setEnabled( stateOne );
    }
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( e.getComponent() == this.tableModule ) {
      if( e.isPopupTrigger() ) {
	showModuleChangePopup( e );
      }
      else if( (e.getButton() == MouseEvent.BUTTON1)
	       && (e.getClickCount() > 1) )
      {
	doModuleEdit();
	e.consume();
      }
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
    if( (e.getComponent() == this.tableModule) && e.isPopupTrigger() )
      showModuleChangePopup( e );
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    if( (e.getComponent() == this.tableModule) && e.isPopupTrigger() )
      showModuleChangePopup( e );
  }


	/* --- PopupMenusOwner --- */

  @Override
  public JPopupMenu[] getPopupMenus()
  {
    return new JPopupMenu[] {
			this.popupModuleAdd,
			this.popupModuleChange };
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws
					UserCancelException,
					UserInputException
  {
    Component tab = null;
    try {

      // Tab Module
      EmuUtil.removePropertiesByPrefix( props, this.modulePropPrefix );
      tab                     = this.tabModule;
      int nDiskStationModules = 0;
      int nRows               = this.tableModelModule.getRowCount();
      for( int i = 0; i < nRows; i++ ) {
	Object slotObj   = this.tableModelModule.getValueAt( i, 0 );
	Object moduleObj = this.tableModelModule.getValueAt( i, 1 );
	Object typeByte  = this.tableModelModule.getValueAt( i, 3 );
	Object fileName  = this.tableModelModule.getValueAt( i, 4 );
	if( (slotObj != null) && (moduleObj != null) ) {
	  String slotText   = slotObj.toString();
	  String moduleText = moduleObj.toString();
	  if( (slotText != null) && (moduleText != null) ) {
	    if( !slotText.isEmpty() && !moduleText.isEmpty() ) {
	      String prefix = String.format(
					"%s%s.",
					this.modulePropPrefix,
					slotText );
	      EmuUtil.setProperty(
				props,
				prefix + KC85.PROP_NAME,
				moduleText );
	      EmuUtil.setProperty(
				props,
				prefix + KC85.PROP_TYPEBYTE,
				typeByte );
	      EmuUtil.setProperty(
				props,
				prefix + KC85.PROP_FILE,
				fileName );
	      if( KC85ModuleTableModel.isDiskStationSlot( slotText ) ) {
		nDiskStationModules++;
	      }
	    }
	  }
	}
      }
      props.setProperty(
		this.modulePropPrefix + KC85.PROP_COUNT,
		Integer.toString( nRows ) );
      props.setProperty(
		this.modulePropPrefix + KC85.PROP_DISKSTATION_MODULE_COUNT,
		Integer.toString( nDiskStationModules ) );
      if( !isDiskStationEnabled() && (nDiskStationModules > 0) ) {
	throw new UserInputException(
		"Sie k\u00F6nnen nur dann Module in die D004-Sch\u00E4chte"
			+ "F0 und F4 stecken,\n"
			+ "wenn Sie die D004-Emulation aktivieren." );
      }

      // Tab D004/D008
      tab                   = this.tabDiskStation;
      String diskStation    = KC85.VALUE_NONE;
      String diskStationRom = KC85.VALUE_DEFAULT;
      if( this.rbDiskStationD004_20.isSelected() ) {
	diskStation    = KC85.VALUE_D004;
	diskStationRom = KC85.VALUE_ROM_20;
      } else if( this.rbDiskStationD004_33.isSelected() ) {
	diskStation    = KC85.VALUE_D004;
	diskStationRom = KC85.VALUE_ROM_33;
      } else if( this.rbDiskStationD008.isSelected() ) {
	diskStation = KC85.VALUE_D008;
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix + KC85.PROP_DISKSTATION,
		diskStation );
      File diskStationRomFile = this.fldDiskStationRomFile.getFile();
      if( diskStationRomFile != null ) {
	diskStationRom = KC85.VALUE_PREFIX_FILE
				+ diskStationRomFile.getPath();
      }
      props.setProperty(
		this.propPrefix + KC85.PROP_DISKSTATION_ROM,
		diskStationRom );
      if( this.rbDiskStationSpeed8MHz.isSelected() ) {
	props.setProperty(
		this.propPrefix + KC85.PROP_DISKSTATION_MAXSPEED_KHZ,
		"8000" );
      } else if( this.rbDiskStationSpeed16MHz.isSelected() ) {
	props.setProperty(
		this.propPrefix + KC85.PROP_DISKSTATION_MAXSPEED_KHZ,
		"16000" );
      } else {
	props.setProperty(
		this.propPrefix + KC85.PROP_DISKSTATION_MAXSPEED_KHZ,
		KC85.VALUE_DEFAULT );
      }

      // Tab GIDE
      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      // Tab ROM
      tab = this.tabROM;
      for( int i = 0; i < this.altRomKeys.length; i++ ) {
	File file = this.altRomTextFlds[ i ].getFile();
	props.setProperty(
		this.propPrefix + this.altRomKeys[ i ],
		file != null ? file.getPath() : "" );
      }

      // Tab Sonstiges
      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + KC85.PROP_KEYS_DIRECT_TO_BUFFER,
		this.cbKeysDirectToBuf.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + KC85.PROP_PASTE_FAST,
		this.cbPasteFast.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + KC85.PROP_EMULATE_VIDEO_TIMING,
		this.cbVideoTiming.isSelected() );

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
    boolean rv = false;
    this.settingsFrm.setWaitCursor( true );

    Object src = e.getSource();
    if( src != null ) {
      if( src == this.btnModuleAdd ) {
	rv = true;
	doModuleAdd();
      } else if( src == this.btnModuleChange ) {
	rv = true;
	doModuleChange();
      } else if( src == this.btnModuleRemove ) {
	rv = true;
	doModuleRemove();
      } else if( src == this.btnModuleUp ) {
	rv = true;
	doModuleMove( -1 );
      } else if( src == this.btnModuleDown ) {
	rv = true;
	doModuleMove( 1 );
      } else if( src == this.mnuModuleEdit ) {
	rv = true;
	doModuleEdit();
      } else if( src == this.mnuModuleIntoD001orD002 ) {
	rv = true;
	doModuleIntoD001orD002();
      } else if( src == this.mnuModuleIntoDiskStation ) {
	rv = true;
	doModuleIntoDiskStation();
      } else if( (src == this.rbDiskStationNone)
	       || (src == this.rbDiskStationD004_20)
	       || (src == this.rbDiskStationD004_33)
	       || (src == this.rbDiskStationD008)
	       || (src == this.rbDiskStationSpeedDefault)
	       || (src == this.rbDiskStationSpeed8MHz)
	       || (src == this.rbDiskStationSpeed16MHz) )
      {
	rv = true;
	fireDataChanged();
	updDiskStationFieldsEnabled();
      } else if( src == this.btnDiskStationRomFileSelect ) {
	rv = true;
	doRomFileSelect(
	      this.fldDiskStationRomFile,
	      this.btnDiskStationRomFileRemove,
	      "D004/D008-ROM-Datei ausw\u00E4hlen" );
      } else if( src == this.btnDiskStationRomFileRemove ) {
	rv = true;
	doDiskStationRomFileRemove();
      } else if( src == this.cbKeysDirectToBuf ) {
	rv = true;
	updPasteFastEnabled();
	fireDataChanged();
      } else if( e instanceof ActionEvent ) {
	String cmd = ((ActionEvent) e).getActionCommand();
	if( cmd != null ) {
	  if( cmd.startsWith( ACTION_MODULE_ADD_PREFIX ) ) {
	    int len = ACTION_MODULE_ADD_PREFIX.length();
	    if( cmd.length() > len ) {
	      addModule( cmd.substring( len ) );
	      fireDataChanged();
	      rv = true;
	    }
	  }
	}
      }
      if( !rv ) {
	for( int i = 0; i < this.altRomSelectBtns.length; i++ ) {
	  if( src == this.altRomSelectBtns[ i ] ) {
	    rv = true;
	    doRomFileSelect(
		this.altRomTextFlds[ i ],
		this.altRomRemoveBtns[ i ],
		"ROM-Datei (" + this.altRomTitles[ i ] +") ausw\u00E4hlen" );
	    break;
	  }
	}
      }
      if( !rv ) {
	for( int i = 0; i < this.altRomRemoveBtns.length; i++ ) {
	  if( src == this.altRomRemoveBtns[ i ] ) {
	    rv = true;
	    this.altRomTextFlds[ i ].setFile( null );
	    this.altRomRemoveBtns[ i ].setEnabled( false );
	    fireDataChanged();
	  }
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
      if( !rv && (src instanceof AbstractButton) ) {
	rv = true;
	fireDataChanged();
      }
    }
    this.settingsFrm.setWaitCursor( false );
    return rv;
  }


  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    boolean           rejected = false;
    DropTargetContext context  = e.getDropTargetContext();
    if( context != null ) {
      if( context.getComponent() == this.fldDiskStationRomFile ) {
	if( !this.fldDiskStationRomFile.isEnabled() ) {
	  e.rejectDrag();
	  rejected = true;
	}
      }
    }
    if( !rejected ) {
      super.dragEnter( e );
    }
  }


  @Override
  protected void fileDropped( Component c, File file )
  {
    if( c == this.fldDiskStationRomFile ) {
      if( this.fldDiskStationRomFile.isEnabled() ) {
	this.fldDiskStationRomFile.setFile( file );
	this.btnDiskStationRomFileRemove.setEnabled( file != null );
	Main.setLastFile( file, Main.FILE_GROUP_ROM );
	fireDataChanged();
      }
    } else {
      for( int i = 0; i < this.altRomTextFlds.length; i++ ) {
	if( c == this.altRomTextFlds[ i ] ) {
	  this.altRomTextFlds[ i ].setFile( file );
	  this.altRomRemoveBtns[ i ].setEnabled( file != null );
	  Main.setLastFile( file, Main.FILE_GROUP_ROM );
	  fireDataChanged();
	  break;
	}
      }
    }
  }


  @Override
  public void updFields( Properties props )
  {
    // Tab Module
    this.tableModelModule.clear();
    if( props != null ) {
      int nRemain = EmuUtil.getIntProperty(
				props,
				this.modulePropPrefix + KC85.PROP_COUNT,
				0 );
      int nDiskStationModules = EmuUtil.getIntProperty(
				props,
				this.modulePropPrefix
					+ KC85.PROP_DISKSTATION_MODULE_COUNT,
				0 );
      int     slotNum = 8;
      boolean loop    = true;
      while( loop && (nRemain > 0) && (slotNum < 0x100) ) {
	if( nRemain == nDiskStationModules ) {
	  slotNum = 0xF0;
	}
	String slotText = String.format( "%02X", slotNum );
	String prefix   = String.format(
				"%s%s.",
				this.modulePropPrefix,
				slotText );
	loop = addModule(
		slotText,
		props.getProperty( prefix + KC85.PROP_NAME ),
		props.getProperty( prefix + KC85.PROP_TYPEBYTE ),
		props.getProperty( prefix + KC85.PROP_FILE ) );
	--nRemain;
	slotNum += 4;
      }
    }

    // Tab D004/D008
    boolean diskStationRom20   = false;
    String  diskStationRomFile = null;
    try {
      String romType = EmuUtil.getProperty(
			props,
			this.propPrefix + KC85.PROP_DISKSTATION_ROM );
      if( romType.equals( KC85.VALUE_ROM_20 ) ) {
	diskStationRom20 = true;
      }
      else if( romType.toLowerCase().startsWith( KC85.VALUE_PREFIX_FILE ) ) {
	diskStationRomFile = 
			romType.substring( KC85.VALUE_PREFIX_FILE.length() );
      }
    }
    catch( IllegalArgumentException ex ) {}
    this.fldDiskStationRomFile.setFileName(
		diskStationRomFile != null ? diskStationRomFile : null );

    boolean diskStationRomState = true;
    switch( EmuUtil.getProperty(
			props,
			this.propPrefix + KC85.PROP_DISKSTATION ) )
    {
      case KC85.VALUE_D004:
	if( diskStationRom20 ) {
	  this.rbDiskStationD004_20.setSelected( true );
	} else {
	  this.rbDiskStationD004_33.setSelected( true );
	}
	break;
      case KC85.VALUE_D008:
	this.rbDiskStationD008.setSelected( true );
	break;
      default:
	this.rbDiskStationNone.setSelected( true );
	diskStationRomState = false;
	break;
    }
    this.labelDiskStationRom.setEnabled( diskStationRomState );
    this.fldDiskStationRomFile.setEnabled( diskStationRomState );

    switch( EmuUtil.getProperty(
		props,
		this.propPrefix + KC85.PROP_DISKSTATION_MAXSPEED_KHZ ) )
    {
      case "8000":
	this.rbDiskStationSpeed8MHz.setSelected( true );
	break;
      case "16000":
	this.rbDiskStationSpeed16MHz.setSelected( true );
	break;
      default:
	this.rbDiskStationSpeedDefault.setSelected( true );
    }
    updDiskStationFieldsEnabled();

    // Tab GIDE
    this.tabGIDE.updFields( props );

    // Tab ROM
    for( int i = 0; i < this.altRomKeys.length; i++ ) {
      String fileName = EmuUtil.getProperty(
				props,
				this.propPrefix + this.altRomKeys[ i ] );
      File file = (fileName.isEmpty() ? null : new File( fileName ));
      this.altRomTextFlds[ i ].setFile( file );
      this.altRomRemoveBtns[ i ].setEnabled( file != null );
    }

    // Tab Sonstiges
    this.cbKeysDirectToBuf.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + KC85.PROP_KEYS_DIRECT_TO_BUFFER,
			false ) );
    this.cbPasteFast.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + KC85.PROP_PASTE_FAST,
			true ) );
    this.cbVideoTiming.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + KC85.PROP_EMULATE_VIDEO_TIMING,
			KC85.getDefaultEmulateVideoTiming() ) );
    updPasteFastEnabled();

    // Tab AutoLoad
    this.tabAutoLoad.updFields( props );

    // Tab AutoInput
    this.tabAutoInput.updFields( props );
  }


	/* --- Aktionen --- */

  private void doDiskStationRomFileRemove()
  {
    this.fldDiskStationRomFile.setFile( null );
    this.btnDiskStationRomFileRemove.setEnabled( false );
    fireDataChanged();
  }


  private void doModuleAdd()
  {
    this.popupModuleAdd.show(
		this.btnModuleAdd,
		0,
		this.btnModuleAdd.getHeight() );
  }


  private void doModuleChange()
  {
    showModuleChangePopup(
		this.btnModuleChange,
		0,
		this.btnModuleChange.getHeight() );
  }


  private void doModuleEdit()
  {
    int row = getSelectedModuleModelRow();
    if( row >= 0 ) {
      String[] cells = this.tableModelModule.getRow( row );
      if( cells != null ) {
	if( cells.length > 1 ) {
	  String moduleName = cells[ 1 ];
	  if( moduleName != null ) {
	    if( isEditableROMModule( moduleName ) ) {
	      KC85UserPROMSettingsDlg dlg = new KC85UserPROMSettingsDlg(
				this.settingsFrm,
				moduleName,
				cells.length > 3 ? cells[ 3 ] : null,
				cells.length > 4 ? cells[ 4 ] : null );
	      dlg.setVisible( true );
	      String approvedFileName     = dlg.getApprovedFileName();
	      String approvedTypeByteText = dlg.getApprovedTypeByteText();
	      if( approvedTypeByteText != null ) {
		if( !approvedTypeByteText.isEmpty() ) {
		  if( cells.length > 2 ) {
		    cells[ 2 ] = createModuleDesc(
						moduleName,
						approvedTypeByteText,
						approvedFileName );
		  }
		  if( cells.length > 3 ) {
		    cells[ 3 ] = approvedTypeByteText;
		  }
		  if( cells.length > 4 ) {
		    cells[ 4 ] = approvedFileName;
		  }
		  this.tableModelModule.fireTableRowsUpdated( row, row );
		  fireDataChanged();
		}
	      }
	    }
	  }
	}
      }
    }
  }


  private void doModuleIntoD001orD002()
  {
    int row = getSelectedModuleModelRow();
    if( row >= 0 ) {
      /*
       * Wenn das letzte Modul nicht mehr in der D004/D008 stecken soll,
       * kann das vorletzte Modul auch nicht dort drin stecken.
       */
      int begRow = row;
      if( (row > 0) && (row == (this.tableModelModule.getRowCount() - 1)) ) {
	begRow = row - 1;
      }
      for( int i = begRow; i <= row; i++ ) {
	String[] rowData = this.tableModelModule.getRow( i );
	if( rowData != null ) {
	  if( rowData.length > 0 ) {
	    if( this.tableModelModule.isDiskStationSlot( i ) ) {
	      rowData[ 0 ] = "00";
	      this.tableModelModule.recalcSlots();
	      fireDataChanged();
	    }
	  }
	}
      }
    }
  }


  private void doModuleIntoDiskStation()
  {
    int row = getSelectedModuleModelRow();
    if( row >= (this.tableModelModule.getRowCount() - 2) ) {
      String[] rowData = this.tableModelModule.getRow( row );
      if( rowData != null ) {
	if( rowData.length > 0 ) {
	  if( !this.tableModelModule.isDiskStationSlot( row ) ) {
	    rowData[ 0 ] = "F0";
	    this.tableModelModule.recalcSlots();
	    fireDataChanged();
	  }
	}
      }
    }
  }


  private void doModuleRemove()
  {
    int[] rows = this.tableModule.getSelectedRows();
    if( rows != null ) {
      if( rows.length > 0 ) {
	Arrays.sort( rows );
	for( int i = rows.length - 1; i >= 0; --i ) {
	  int row = this.tableModule.convertRowIndexToModel( rows[ i ] );
	  if( row >= 0 ) {
	    this.tableModelModule.removeRow( row );
	  }
	}
	this.tableModelModule.recalcSlots();
	fireDataChanged();
      }
    }
  }


  private void doModuleMove( int diffRows )
  {
    int[] rows = this.tableModule.getSelectedRows();
    if( rows != null ) {
      if( rows.length == 1 ) {
	int nRows = this.tableModelModule.getRowCount();
	int row1  = rows[ 0 ];
	int row2  = row1 + diffRows;
	if( (row1 >= 0) && (row1 < nRows)
	    && (row2 >= 0) && (row2 < nRows) )
	{
	  String[] rowData1 = this.tableModelModule.getRow( row1 );
	  String[] rowData2 = this.tableModelModule.getRow( row2 );
	  if( (rowData1 != null) && (rowData2 != null) ) {
	    int n = Math.min( rowData1.length, rowData2.length );
	    for( int i = 1; i < n; i++ ) {
	      String m      = rowData1[ i ];
	      rowData1[ i ] = rowData2[ i ];
	      rowData2[ i ] = m;
	    }
	    this.tableModelModule.fireTableRowsUpdated(
						Math.min( row1, row2 ),
						Math.max( row1, row2 ) );
	    this.tableModelModule.updateDescriptions();
	    EmuUtil.fireSelectRow( this.tableModule, row2 );
	    fireDataChanged();
	  }
	}
      }
    }
  }


  private void doRomFileSelect(
			FileNameFld fldFile,
			JButton     btnRemove,
			String      title )
  {
    File file = selectRomFile( fldFile.getFile(), title );
    if( file != null ) {
      fldFile.setFile( file );
      btnRemove.setEnabled( true );
      Main.setLastFile( file, Main.FILE_GROUP_ROM );
      fireDataChanged();
    }
  }


	/* --- private Methoden --- */

  private boolean addModule(
			String slotText,
			String moduleName,
			String typeByteText,
			String fileName )
  {
    boolean rv    = false;
    int     nRows = this.tableModelModule.getRowCount();
    if( (moduleName != null) && (nRows < 60) ) {
      if( !moduleName.isEmpty() ) {
	for( int i = 0; i < modules.length; i++ ) {
	  String s = modules[ i ][ MOD_IDX_NAME ];
	  if( moduleName.equals( s ) ) {
	    if( slotText == null ) {
	      slotText = String.format( "%02X", (nRows + 2) * 4 );
	    }
	    this.tableModelModule.addRow(
				slotText,
				moduleName,
				createModuleDesc(
					moduleName,
					typeByteText,
					fileName ),
				typeByteText,
				fileName );
	    this.tableModelModule.recalcSlots();
	    rv = true;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  private void addModule( String moduleName )
  {
    if( moduleName != null ) {
      if( !moduleName.isEmpty() ) {
	String typeText = null;
	String fileName = null;
	if( isEditableROMModule( moduleName ) ) {
	  boolean                 ok  = false;
	  KC85UserPROMSettingsDlg dlg = new KC85UserPROMSettingsDlg(
							this.settingsFrm,
							moduleName,
							null,
							null );
	  dlg.setVisible( true );
	  typeText   = dlg.getApprovedTypeByteText();
	  fileName   = dlg.getApprovedFileName();
	  if( (typeText != null) && (fileName != null) ) {
	    if( !typeText.isEmpty() && !fileName.isEmpty() ) {
	      ok = true;
	    }
	  }
	  if( !ok ) {
	    moduleName = null;
	  }
	}
	if( moduleName != null ) {
	  if( addModule( null, moduleName, typeText, fileName ) ) {
	    EmuUtil.fireSelectRow(
		      this.tableModule,
		      this.tableModelModule.getRowCount() - 1 );
	  }
	}
      }
    }
  }


  private static String createModuleDesc(
				String moduleName,
				String typeByteText,
				String fileName )
  {
    String moduleDesc = null;
    if( moduleName != null ) {
      if( typeByteText != null ) {
	if( moduleName.equals( "M025" ) ) {
	  if( typeByteText.equals( "1" )
	      || typeByteText.equals( "01" ) )
	  {
	    moduleDesc = "8K Autostart User PROM";
	  } else if( typeByteText.equals( "F7" ) ) {
	    moduleDesc = "8K User PROM";
	  } else if( typeByteText.equals( "FB" ) ) {
	    moduleDesc = "8 KByte ROM";
	  }
	}
	else if( moduleName.equals( "M028" ) ) {
	  if( typeByteText.equals( "F8" ) ) {
	    moduleDesc = "16K User PROM";
	  } else if( typeByteText.equals( "FC" ) ) {
	    moduleDesc = "16K ROM";
	  }
	}
	else if( moduleName.equals( "M040" ) ) {
	  if( typeByteText.equals( "1" )
	      || typeByteText.equals( "01" ) )
	  {
	    moduleDesc = "8/16K Autostart User PROM";
	  } else if( typeByteText.equals( "F7" ) ) {
	    moduleDesc = "8K User ROM";
	  } if( typeByteText.equals( "F8" ) ) {
	    moduleDesc = "16K User PROM";
	  }
	}
	else if( moduleName.equals( "M041" ) ) {
	  if( typeByteText.equals( "1" )
	      || typeByteText.equals( "01" ) )
	  {
	    moduleDesc = "16K Autostart User PROM";
	  } else if( typeByteText.equals( "F1" ) ) {
	    moduleDesc = "16K EEPROM";
	  } else if( typeByteText.equals( "F8" ) ) {
	    moduleDesc = "16K PROM/EPROM";
	  } if( typeByteText.equals( "FC" ) ) {
	    moduleDesc = "16K ROM";
	  }
	}
	else if( moduleName.equals( "M045" ) ) {
	  if( typeByteText.equals( "1" )
	      || typeByteText.equals( "01" ) )
	  {
	    moduleDesc = "4x8K Autostart User PROM";
	  } else if( typeByteText.equals( "70" ) ) {
	    moduleDesc = "4x8K User PROM";
	  }
	}
	else if( moduleName.equals( "M046" ) ) {
	  if( typeByteText.equals( "1" )
	      || typeByteText.equals( "01" ) )
	  {
	    moduleDesc = "8x8K Autostart User PROM";
	  } else if( typeByteText.equals( "71" ) ) {
	    moduleDesc = "8x8K User PROM";
	  }
	}
	else if( moduleName.equals( "M047" ) ) {
	  if( typeByteText.equals( "1" )
	      || typeByteText.equals( "01" ) )
	  {
	    moduleDesc = "16x8K Autostart User PROM";
	  } else if( typeByteText.equals( "72" ) ) {
	    moduleDesc = "16x8K User PROM";
	  }
	}
	else if( moduleName.equals( "M048" ) ) {
	  if( typeByteText.equals( "1" )
	      || typeByteText.equals( "01" ) )
	  {
	    moduleDesc = "16x16K Autostart User PROM";
	  } else if( typeByteText.equals( "73" ) ) {
	    moduleDesc = "16x16K User PROM";
	  }
	}
      }
      if( moduleDesc == null ) {
	for( int i = 0; i < modules.length; i++ ) {
	  if( modules[ i ][ MOD_IDX_NAME ].equals( moduleName ) ) {
	    moduleDesc = modules[ i ][ MOD_IDX_DESC ];
	  }
	}
      }
      if( (moduleDesc != null) && (fileName != null) ) {
	if( !fileName.isEmpty() ) {
	  moduleDesc = String.format( "%s: %s", moduleDesc, fileName );
	}
      }
    }
    return moduleDesc;
  }


  private int getSelectedModuleModelRow()
  {
    int   row      = -1;
    int[] viewRows = this.tableModule.getSelectedRows();
    if( viewRows != null ) {
      if( viewRows.length == 1 ) {
	row = this.tableModule.convertRowIndexToModel( viewRows[ 0 ] );
      }
    }
    return row;
  }


  private boolean isDiskStationEnabled()
  {
    return this.rbDiskStationD004_20.isSelected()
			|| this.rbDiskStationD004_33.isSelected()
			|| this.rbDiskStationD008.isSelected();
  }


  private static boolean isEditableROMModule( String moduleName )
  {
    return moduleName.equals( "M025" )
		|| moduleName.equals( "M028" )
		|| moduleName.equals( "M040" )
		|| moduleName.equals( "M041" )
		|| moduleName.equals( "M045" )
		|| moduleName.equals( "M046" )
		|| moduleName.equals( "M047" )
		|| moduleName.equals( "M048" );
  }


  private File selectRomFile( File oldFile, String title )
  {
    File rv   = null;
    File file = FileUtil.showFileOpenDlg(
			this.settingsFrm,
			title,
			oldFile != null ?
				oldFile
				: Main.getLastDirFile( Main.FILE_GROUP_ROM ),
			FileUtil.getROMFileFilter() );
    if( file != null ) {
      String msg = null;
      if( file.exists() ) {
	if( file.isFile() ) {
	  if( file.canRead() ) {
	    if( file.length() > 0 ) {
	      rv = file;
	      Main.setLastFile( file, Main.FILE_GROUP_ROM );
	    } else {
	      msg = "Datei ist leer";
	    }
	  } else {
	    msg = "Datei nicht lesbar";
	  }
	} else {
	  msg = "Datei ist keine regul\u00E4re Datei";
	}
      } else {
	msg = "Datei nicht gefunden";
      }
      if( msg != null ) {
	BaseDlg.showErrorDlg( this, file.getPath() + ": " + msg );
      }
    }
    return rv;
  }


  private void showModuleChangePopup( MouseEvent e )
  {
    Component c = e.getComponent();
    if( c != null ) {
      showModuleChangePopup( c, e.getX(), e.getY() );
    }
  }


  private void showModuleChangePopup( Component c, int x, int y )
  {
    boolean inDiskStation  = false;
    boolean canDiskStation = false;
    boolean editable       = false;
    int     row            = getSelectedModuleModelRow();
    if( row >= 0 ) {
      String[] rowData = this.tableModelModule.getRow( row );
      if( rowData != null ) {
	if( rowData.length > 1 ) {
	  String moduleName = rowData[ 1 ];
	  if( moduleName != null ) {
	    editable = isEditableROMModule( moduleName );
	  }
	  inDiskStation = KC85ModuleTableModel.isDiskStationSlot(
							rowData[ 0 ] );
	}
      }
    }
    if( row >= (this.tableModelModule.getRowCount() - 2) ) {
      canDiskStation = true;
    }
    this.mnuModuleEdit.setEnabled( editable );
    this.mnuModuleIntoD001orD002.setEnabled( inDiskStation );
    this.mnuModuleIntoDiskStation.setEnabled(
		!inDiskStation && canDiskStation && isDiskStationEnabled() );
    this.popupModuleChange.show( c, x, y );
  }


  private void updDiskStationFieldsEnabled()
  {
    boolean d4_20 = this.rbDiskStationD004_20.isSelected();
    boolean d4_33 = this.rbDiskStationD004_33.isSelected();
    boolean state = (d4_20 || d4_33 || this.rbDiskStationD008.isSelected());
    this.tabGIDE.setEnabledEx( state, d4_20 || d4_33 );
    this.labelDiskStationRom.setEnabled( state );
    this.fldDiskStationRomFile.setEnabled( state );
    this.btnDiskStationRomFileSelect.setEnabled( state );
    this.btnDiskStationRomFileRemove.setEnabled(
		state & (this.fldDiskStationRomFile.getFile() != null) );
    this.labelDiskStationSpeed.setEnabled( state );
    this.rbDiskStationSpeedDefault.setEnabled( state );
    this.rbDiskStationSpeed8MHz.setEnabled( state );
    this.rbDiskStationSpeed16MHz.setEnabled( state );
  }


  private void updPasteFastEnabled()
  {
    if( this.cbPasteFast != null ) {
      this.cbPasteFast.setEnabled(
		!this.cbKeysDirectToBuf.isSelected() );
    }
  }
}
