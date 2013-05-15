/*
 * (c) 2010-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen der Computer HC900 und KC85/2..5
 */

package jkcemu.emusys.kc85;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.KC85;


public class KC85SettingsFld
			extends AbstractSettingsFld
			implements ListSelectionListener, MouseListener
{
  private static final String moduleAddPrefix  = "kc85.module.add.";

  private static final String[][] modules = {
	{ "M003", "V.24 mit angeschlossenem Drucker" },
	{ "M006", "BASIC" },
	{ "M008", "Joystick" },
	{ "M011", "64 KByte RAM" },
	{ "M012", "TEXOR" },
	{ "M021", "Joystick / Centronics mit angeschlossenem Drucker" },
	{ "M022", "16 KByte Expander RAM" },
	{ "M025", "8 KByte User PROM" },
	{ "M026", "Forth" },
	{ "M027", "Development" },
	{ "M028", "16 KByte User PROM" },
	{ "M032", "256 KByte Segmented RAM" },
	{ "M033", "TypeStar" },
	{ "M034", "512 KByte Segmented RAM" },
	{ "M035", "1 MByte Segmented RAM" },
	{ "M035x4", "4 MByte Segmented RAM" },
	{ "M036", "128 KByte Segmented RAM" },
	{ "M040", "8/16 KByte User PROM" },
	{ "M052", "USB/Netzwerk" },
	{ "M120", "8 KByte CMOS RAM" },
	{ "M122", "16 KByte CMOS RAM" },
	{ "M124", "32 KByte CMOS RAM" } };

  private static final String[] altRomTitles2 = {
					"CAOS-ROM E000-E7FF",
					"CAOS-ROM F000-F7FF",
					"M052-ROM (USB/Netzwerk)" };

  private static final String[] altRomKeys2 = {
					"rom.caos_e.file",
					"rom.caos_f.file",
					"rom.m052.file" };

  private static final String[] altRomTitles3 = {
					"BASIC-ROM C000-DFFF",
					"CAOS-ROM E000-FFFF",
					"M052-ROM (USB/Netzwerk)" };

  private static final String[] altRomKeys3 = {
					"rom.basic.file",
					"rom.caos_e.file",
					"rom.m052.file" };

  private static final String[] altRomTitles4 = {
					"BASIC-ROM C000-DFFF",
					"CAOS-ROM C000-CFFF",
					"CAOS-ROM E000-FFFF",
					"M052-ROM (USB/Netzwerk)" };

  private static final String[] altRomKeys4 = {
					"rom.basic.file",
					"rom.caos_c.file",
					"rom.caos_e.file",
					"rom.m052.file" };

  private static final String[] altRomTitles5 = {
					"BASIC-/USER-ROM C000-DFFF (4x8K)",
					"CAOS-ROM C000-DFFF",
					"CAOS-ROM E000-FFFF",
					"M052-ROM (USB/Netzwerk)" };

  private static final String[] altRomKeys5 = {
					"rom.basic.file",
					"rom.caos_c.file",
					"rom.caos_e.file",
					"rom.m052.file" };

  private int                  kcTypeNum;
  private String               modulePropPrefix;
  private String[]             altRomTitles;
  private String[]             altRomKeys;
  private ListSelectionModel   selModelModule;
  private KC85ModuleTableModel tableModelModule;
  private JTable               tableModule;
  private JTabbedPane          tabbedPane;
  private JPanel               tabModule;
  private JPanel               tabD004;
  private JPanel               tabROM;
  private JPanel               tabEtc;
  private JPopupMenu           popupModule;
  private JButton              btnModuleAdd;
  private JButton              btnModuleEdit;
  private JButton              btnModuleRemove;
  private JButton              btnModuleUp;
  private JButton              btnModuleDown;
  private JCheckBox            btnD004Enabled;
  private JComboBox            comboD004Rom;
  private JLabel               labelD004Rom;
  private JLabel               labelD004Speed;
  private JRadioButton         btnD004Speed4MHz;
  private JRadioButton         btnD004Speed8MHz;
  private JRadioButton         btnD004Speed16MHz;
  private FileNameFld          fldD004RomFile;
  private JButton              btnD004RomFileSelect;
  private JButton              btnD004RomFileRemove;
  private GIDESettingsFld      tabGIDE;
  private FileNameFld[]        altRomTextFlds;
  private JButton[]            altRomSelectBtns;
  private JButton[]            altRomRemoveBtns;
  private JCheckBox            btnCtrlKeysDirect;
  private JCheckBox            btnPasteFast;
  private JCheckBox            btnUmlautsTo2Codes;
  private JCheckBox            btnVideoTiming;


  public KC85SettingsFld(
		SettingsFrm settingsFrm,
		String      propPrefix,
		int         kcTypeNum )
  {
    super( settingsFrm, propPrefix );
    this.kcTypeNum        = kcTypeNum;
    this.modulePropPrefix = propPrefix + "module.";

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

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Module
    this.tabModule = new JPanel( new GridBagLayout() );
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

    this.tableModule = new JTable( this.tableModelModule );
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

    this.tabModule.add( new JScrollPane( this.tableModule ), gbcModule );

    EmuUtil.setTableColWidths( this.tableModule, 60, 60, 300 );

    JPanel panelModBtnRight = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
    gbcModule.fill    = GridBagConstraints.NONE;
    gbcModule.weightx = 0.0;
    gbcModule.weighty = 0.0;
    gbcModule.gridx++;
    this.tabModule.add( panelModBtnRight, gbcModule );

    this.btnModuleUp = createImageButton( "/images/nav/up.png", "Auf" );
    panelModBtnRight.add( this.btnModuleUp );

    this.btnModuleDown = createImageButton( "/images/nav/down.png", "Ab" );
    panelModBtnRight.add( this.btnModuleDown );

    JPanel panelModBtnBottom = new JPanel( new GridLayout( 1, 3, 5, 5 ) );
    gbcModule.gridx = 0;
    gbcModule.gridy++;
    this.tabModule.add( panelModBtnBottom, gbcModule );

    this.btnModuleAdd = new JButton( "Hinzuf\u00FCgen" );
    this.btnModuleAdd.addActionListener( this );
    this.btnModuleAdd.addKeyListener( this );
    panelModBtnBottom.add( this.btnModuleAdd );

    this.btnModuleEdit = new JButton( "Bearbeiten" );
    this.btnModuleEdit.addActionListener( this );
    this.btnModuleEdit.addKeyListener( this );
    panelModBtnBottom.add( this.btnModuleEdit );

    this.btnModuleRemove = new JButton( "Entfernen" );
    this.btnModuleRemove.addActionListener( this );
    this.btnModuleRemove.addKeyListener( this );
    panelModBtnBottom.add( this.btnModuleRemove );

    this.selModelModule = this.tableModule.getSelectionModel();
    if( this.selModelModule != null ) {
      this.selModelModule.addListSelectionListener( this );
      this.btnModuleUp.setEnabled( false );
      this.btnModuleDown.setEnabled( false );
      this.btnModuleEdit.setEnabled( false );
      this.btnModuleRemove.setEnabled( false );
    }

    this.popupModule = new JPopupMenu();
    for( int i = 0; i < modules.length; i++ ) {
      String modName = modules[ i ][ 0 ];
      if( (kcTypeNum < 3) || !modName.equals( "M006" ) ) {
	JMenuItem modItem = new JMenuItem( String.format(
						"%s - %s",
						modName,
						modules[ i ][ 1 ] ) );
	modItem.setActionCommand( moduleAddPrefix + modName );
	modItem.addActionListener( this );
	this.popupModule.add( modItem );
      }
    }


    // Tab D004
    this.tabD004 = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "D004", this.tabD004 );

    GridBagConstraints gbcDisk = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnD004Enabled = new JCheckBox(
			"Floppy-Disk-Erweiterung D004 mit GIDE emulieren",
			false );
    this.btnD004Enabled.addActionListener( this );
    this.tabD004.add( this.btnD004Enabled, gbcDisk );

    this.labelD004Rom   = new JLabel( "D004-ROM:" );
    gbcDisk.insets.left = 50;
    gbcDisk.insets.top  = 20;
    gbcDisk.gridwidth   = 1;
    gbcDisk.gridy++;
    this.tabD004.add( this.labelD004Rom, gbcDisk );

    this.comboD004Rom = new JComboBox();
    this.comboD004Rom.setEditable( false );
    this.comboD004Rom.addItem( "Version 2.0" );
    this.comboD004Rom.addItem( "Version 3.3" );
    this.comboD004Rom.addItem( "Datei" );
    this.comboD004Rom.addActionListener( this );
    gbcDisk.insets.left = 0;
    gbcDisk.gridx++;
    this.tabD004.add( this.comboD004Rom, gbcDisk );

    this.fldD004RomFile = new FileNameFld();
    gbcDisk.fill        = GridBagConstraints.HORIZONTAL;
    gbcDisk.weightx     = 1.0;
    gbcDisk.insets.top  = 0;
    gbcDisk.insets.left = 50;
    gbcDisk.gridwidth   = 2;
    gbcDisk.gridx       = 0;
    gbcDisk.gridy++;
    this.tabD004.add( this.fldD004RomFile, gbcDisk );

    this.btnD004RomFileSelect = createImageButton(
					"/images/file/open.png",
					"ROM-Datei ausw\u00E4hlen" );
    gbcDisk.fill        = GridBagConstraints.NONE;
    gbcDisk.weightx     = 0.0;
    gbcDisk.insets.left = 0;
    gbcDisk.gridwidth   = 1;
    gbcDisk.gridx += 2;
    this.tabD004.add( this.btnD004RomFileSelect, gbcDisk );

    this.btnD004RomFileRemove = createImageButton(
					"/images/file/delete.png",
					"ROM-Datei entfernen" );
    gbcDisk.gridx++;
    this.tabD004.add( this.btnD004RomFileRemove, gbcDisk );

    this.labelD004Speed = new JLabel( "Taktfrequenz:" );
    gbcDisk.insets.left = 50;
    gbcDisk.insets.top  = 20;
    gbcDisk.gridwidth   = 1;
    gbcDisk.gridx       = 0;
    gbcDisk.gridy++;
    this.tabD004.add( this.labelD004Speed, gbcDisk );

    JPanel panelD004Speed = new JPanel(
			new FlowLayout( FlowLayout.LEFT, 10, 0 ) );
    gbcDisk.insets.top    = 0;
    gbcDisk.insets.bottom = 5;
    gbcDisk.gridwidth     = GridBagConstraints.REMAINDER;
    gbcDisk.gridy++;
    this.tabD004.add( panelD004Speed, gbcDisk );

    ButtonGroup grpD004Speed = new ButtonGroup();

    this.btnD004Speed4MHz = new JRadioButton( "4 MHz (original)", true );
    this.btnD004Speed4MHz.addActionListener( this );
    grpD004Speed.add( this.btnD004Speed4MHz );
    panelD004Speed.add( this.btnD004Speed4MHz );

    this.btnD004Speed8MHz = new JRadioButton( "8 MHz", false );
    this.btnD004Speed8MHz.addActionListener( this );
    grpD004Speed.add( this.btnD004Speed8MHz );
    panelD004Speed.add( this.btnD004Speed8MHz );

    this.btnD004Speed16MHz = new JRadioButton( "16 MHz", false );
    this.btnD004Speed16MHz.addActionListener( this );
    grpD004Speed.add( this.btnD004Speed16MHz );
    panelD004Speed.add( this.btnD004Speed16MHz );


    // Tab GIDE
    this.tabGIDE = new GIDESettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );


    // Tab ROM
    this.tabROM = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "ROM", this.tabROM );

    GridBagConstraints gbcROM = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.tabROM.add( new JLabel( "Alternative ROM-Inhalte:" ), gbcROM );

    this.altRomTextFlds   = new FileNameFld[ this.altRomTitles.length ];
    this.altRomSelectBtns = new JButton[ this.altRomTitles.length ];
    this.altRomRemoveBtns = new JButton[ this.altRomTitles.length ];
    for( int i = 0; i < this.altRomTitles.length; i++ ) {
      gbcROM.insets.left   = 50;
      gbcROM.insets.bottom = 0;
      gbcROM.gridx         = 0;
      gbcROM.gridy++;
      this.tabROM.add( new JLabel( this.altRomTitles[ i ] + ":" ), gbcROM );

      FileNameFld fld = new FileNameFld();
      gbcROM.fill          = GridBagConstraints.HORIZONTAL;
      gbcROM.weightx       = 1.0;
      gbcROM.insets.top    = 0;
      gbcROM.insets.bottom = 5;
      gbcROM.gridy++;
      this.tabROM.add( fld, gbcROM );
      this.altRomTextFlds[ i ] = fld;

      JButton btn = createImageButton(
				"/images/file/open.png",
				"ROM-Datei ausw\u00E4hlen" );
      gbcROM.fill        = GridBagConstraints.NONE;
      gbcROM.weightx     = 0.0;
      gbcROM.insets.left = 0;
      gbcROM.gridx++;
      this.tabROM.add( btn, gbcROM );
      this.altRomSelectBtns[ i ] = btn;

      btn = createImageButton(
			"/images/file/delete.png",
			"ROM-Datei entfernen" );
      gbcROM.gridx++;
      this.tabROM.add( btn, gbcROM );
      this.altRomRemoveBtns[ i ] = btn;
    }


    // Tab Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.btnUmlautsTo2Codes = new JCheckBox(
	"Tasten f\u00FCr \u00C4\u00D6\u00DC\u00E4\u00F6\u00FC[]{}\\~"
		+ " entsprechend MicroDOS-Mode der D005 behandeln",
	false );
    this.btnUmlautsTo2Codes.addActionListener( this );
    this.tabEtc.add( this.btnUmlautsTo2Codes, gbcEtc );

    this.btnCtrlKeysDirect = new JCheckBox(
			"Ctrl-A...Z, ESC und TAB direkt in den"
				+ " Tastaturpuffer schreiben",
			true );
    this.btnCtrlKeysDirect.addActionListener( this );
    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnCtrlKeysDirect, gbcEtc );

    this.btnPasteFast = new JCheckBox(
		"Einf\u00FCgen von Text direkt in den Tastaturpuffer",
		true );
    gbcEtc.gridy++;
    this.btnPasteFast.addActionListener( this );
    this.tabEtc.add( this.btnPasteFast, gbcEtc );

    gbcEtc.fill       = GridBagConstraints.HORIZONTAL;
    gbcEtc.weightx    = 1.0;
    gbcEtc.insets.top = 10;
    gbcEtc.gridy++;
    this.tabEtc.add( new JSeparator(), gbcEtc );

    gbcEtc.fill    = GridBagConstraints.NONE;
    gbcEtc.weightx = 0.0;
    gbcEtc.gridy++;
    this.tabEtc.add(
	new JLabel( "Die folgende Option ist f\u00FCr die korrekte"
			+ " Darstellung einiger Programme notwendig," ),
	gbcEtc );

    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    this.tabEtc.add(
	new JLabel( "ben\u00F6tigt aber relativ viel"
			+ " Rechenleistung. Sollte diese Leistung nicht zur" ),
	gbcEtc );

    gbcEtc.gridy++;
    this.tabEtc.add(
	new JLabel( "Verf\u00FCgung stehen,"
			+ " dann schalten Sie die Option bitte aus." ),
	gbcEtc );

    this.btnVideoTiming = new JCheckBox(
			"Zeitverhalten der Bildschirmsteuerung emulieren",
			KC85.getDefaultEmulateVideoTiming() );
    this.btnVideoTiming.addActionListener( this );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnVideoTiming, gbcEtc );


    // Drag&Drop ermoeglichen
    (new DropTarget( this.fldD004RomFile, this )).setActive( true );
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
      if( stateOne ) {
	int row = this.tableModule.convertRowIndexToModel( selRowNum );
	if( row >= 0 ) {
	  String[] cells = this.tableModelModule.getRow( row );
	  if( cells != null ) {
	    if( cells.length > 1 ) {
	      String moduleName = cells[ 1 ];
	      if( moduleName != null ) {
		if( moduleName.equals( "M025" )
		    || moduleName.equals( "M028" )
		    || moduleName.equals( "M040" ) )
		{
		  stateEdit = true;
		}
	      }
	    }
	  }
	}
      }
      this.btnModuleEdit.setEnabled( stateEdit );
    }
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getButton() == MouseEvent.BUTTON1)
	&& (e.getClickCount() > 1)
	&& (e.getComponent() == this.tableModule) )
    {
      doModuleEdit();
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
    Component tab = null;
    try {

      // Tab Module
      tab = this.tabModule;
      int nRows = this.tableModelModule.getRowCount();
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
	      String prefix = this.modulePropPrefix + slotText;
	      EmuUtil.setProperty(
				props,
				prefix + ".name",
				moduleText );
	      EmuUtil.setProperty(
				props,
				prefix + ".typebyte",
				typeByte );
	      EmuUtil.setProperty(
				props,
				prefix + ".file",
				fileName );
	    }
	  }
	}
      }
      props.setProperty(
		this.modulePropPrefix + "count",
		Integer.toString( nRows ) );

      // Tab D004
      tab = this.tabD004;
      boolean d004Enabled = this.btnD004Enabled.isSelected();
      props.setProperty(
		this.propPrefix + "d004.enabled",
		Boolean.toString( d004Enabled ) );
      String d004Rom = "standard";
      switch( this.comboD004Rom.getSelectedIndex() ) {
	case 0:
	  d004Rom = "2.0";
	  break;
	case 1:
	  d004Rom = "3.3";
	  break;
	case 2:
	  {
	    File file = this.fldD004RomFile.getFile();
	    if( (file == null) && d004Enabled ) {
	      if( selected ) {
		this.tabbedPane.setSelectedComponent( this.tabD004 );
		throw new UserInputException(
			"D004-ROM: Bitte w\u00E4hlen Sie eine ROM-Datei aus\n"
				+ "oder stellen Sie einen anderen D004-ROM"
				+ " ein." );
	      }
	    }
	    if( file != null ) {
	      d004Rom = "file:" + file.getPath();
	    }
	  }
	  break;
      }
      props.setProperty( this.propPrefix + "d004.rom", d004Rom );
      if( this.btnD004Speed8MHz.isSelected() ) {
	props.setProperty( this.propPrefix + "d004.maxspeed.khz", "8000" );
      } else if( this.btnD004Speed16MHz.isSelected() ) {
	props.setProperty( this.propPrefix + "d004.maxspeed.khz", "16000" );
      } else {
	props.setProperty( this.propPrefix + "d004.maxspeed.khz", "4000" );
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
      props.setProperty(
		this.propPrefix + "control_keys.direct",
		Boolean.toString( this.btnCtrlKeysDirect.isSelected() ) );
      props.setProperty(
		this.propPrefix + "paste.fast",
		Boolean.toString( this.btnPasteFast.isSelected() ) );
      props.setProperty(
		this.propPrefix + "umlauts_to_2_keycodes",
		Boolean.toString( this.btnUmlautsTo2Codes.isSelected() ) );
      props.setProperty(
		this.propPrefix + "emulate_video_timing",
		Boolean.toString( this.btnVideoTiming.isSelected() ) );
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
      } else if( src == this.btnModuleEdit ) {
	rv = true;
	doModuleEdit();
      } else if( src == this.btnModuleRemove ) {
	rv = true;
	doModuleRemove();
      } else if( src == this.btnModuleUp ) {
	rv = true;
	doModuleMove( -1 );
      } else if( src == this.btnModuleDown ) {
	rv = true;
	doModuleMove( 1 );
      } else if( (src == this.btnD004Enabled)
	       || (src == this.comboD004Rom)
	       || (src == this.btnD004Speed4MHz)
	       || (src == this.btnD004Speed8MHz)
	       || (src == this.btnD004Speed16MHz) )
      {
	rv = true;
	fireDataChanged();
	updD004FieldsEnabled();
      } else if( src == this.btnD004RomFileSelect ) {
	rv = true;
	doFileSelect(
	      this.fldD004RomFile,
	      this.btnD004RomFileRemove,
	      "D004-ROM-Datei ausw\u00E4hlen",
	      "rom" );
      } else if( src == this.btnD004RomFileRemove ) {
	rv = true;
	doD004RomFileRemove();
      } else if( e instanceof ActionEvent ) {
	String cmd = ((ActionEvent) e).getActionCommand();
	if( cmd != null ) {
	  if( cmd.startsWith( moduleAddPrefix ) ) {
	    int len = moduleAddPrefix.length();
	    if( cmd.length() > len ) {
	      addModule( cmd.substring( len ) );
	      rv = true;
	    }
	  }
	}
	fireDataChanged();
      }
      if( !rv ) {
	for( int i = 0; i < this.altRomSelectBtns.length; i++ ) {
	  if( src == this.altRomSelectBtns[ i ] ) {
	    rv = true;
	    doFileSelect(
		this.altRomTextFlds[ i ],
		this.altRomRemoveBtns[ i ],
		"ROM-Datei (" + this.altRomTitles[ i ] +") ausw\u00E4hlen",
		"rom" );
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
	  }
	}
      }
      if( !rv && (src instanceof AbstractButton) ) {
	rv = true;
	fireDataChanged();
      }
    }
    if( !rv ) {
      rv = this.tabGIDE.doAction( e );
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
      if( context.getComponent() == this.fldD004RomFile ) {
	if( !this.fldD004RomFile.isEnabled() ) {
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
  protected boolean fileDropped( Component c, File file )
  {
    boolean rv = false;
    if( c == this.fldD004RomFile ) {
      if( this.fldD004RomFile.isEnabled() ) {
	this.fldD004RomFile.setFile( file );
	this.btnD004RomFileRemove.setEnabled( file != null );
	Main.setLastFile( file, "rom" );
	fireDataChanged();
	rv = true;
      }
    } else {
      for( int i = 0; i < this.altRomTextFlds.length; i++ ) {
	if( c == this.altRomTextFlds[ i ] ) {
	  this.altRomTextFlds[ i ].setFile( file );
	  this.altRomRemoveBtns[ i ].setEnabled( file != null );
	  Main.setLastFile( file, "rom" );
	  fireDataChanged();
	  rv = true;
	  break;
	}
      }
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.tabGIDE.lookAndFeelChanged();
    if( this.popupModule != null ) {
      SwingUtilities.updateComponentTreeUI( this.popupModule );
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
				this.modulePropPrefix + "count",
				0 );
      int     slotNum = 8;
      boolean loop    = true;
      while( loop && (nRemain > 0) ) {
	String slotText = String.format( "%02X", slotNum );
	String prefix   = this.modulePropPrefix + slotText;
	loop = addModule(
		slotText,
		props.getProperty( prefix + ".name" ),
		props.getProperty( prefix + ".typebyte" ),
		props.getProperty( prefix + ".file" ) );
	--nRemain;
	slotNum += 4;
      }
    }

    // Tab D004
    this.btnD004Enabled.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "d004.enabled",
				false ) );
    try {
      String d004Rom = EmuUtil.getProperty(
					props,
					this.propPrefix + "d004.rom" );
      if( d004Rom.equals( "2.0" ) ) {
	this.comboD004Rom.setSelectedIndex( 0 );
      } else if( d004Rom.equals( "3.3" ) ) {
	this.comboD004Rom.setSelectedIndex( 1 );
      } else if( d004Rom.startsWith( "file:" ) ) {
	this.comboD004Rom.setSelectedIndex( 2 );
	if( d004Rom.length() > 5 ) {
	  this.fldD004RomFile.setFileName( d004Rom.substring( 5 ) );
	} else {
	  this.fldD004RomFile.setFile( null );
	}
      } else {
	this.comboD004Rom.setSelectedIndex( this.kcTypeNum > 4 ? 1 : 0 );
      }
    }
    catch( IllegalArgumentException ex ) {}
    String d004Speed = EmuUtil.getProperty(
				props,
				this.propPrefix + "d004.maxspeed.khz" );
    if( d004Speed.equals( "8000" ) ) {
      this.btnD004Speed8MHz.setSelected( true );
    } else if( d004Speed.equals( "16000" ) ) {
      this.btnD004Speed16MHz.setSelected( true );
    } else {
      this.btnD004Speed4MHz.setSelected( true );
    }
    updD004FieldsEnabled();

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
    this.btnCtrlKeysDirect.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "control_keys.direct",
				false ) );
    this.btnPasteFast.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "paste.fast",
				true ) );
    this.btnUmlautsTo2Codes.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "umlauts_to_2_keycodes",
				false ) );
    this.btnVideoTiming.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "emulate_video_timing",
				KC85.getDefaultEmulateVideoTiming() ) );
  }


	/* --- Aktionen --- */

  private void doFileSelect(
			FileNameFld fldFile,
			JButton     btnRemove,
			String      title,
			String      category )
  {
    File file = selectRomFile( fldFile.getFile(), title );
    if( file != null ) {
      fldFile.setFile( file );
      btnRemove.setEnabled( true );
      fireDataChanged();
    }
  }


  private void doD004RomFileRemove()
  {
    this.fldD004RomFile.setFile( null );
    this.btnD004RomFileRemove.setEnabled( false );
    fireDataChanged();
  }


  private void doModuleAdd()
  {
    this.popupModule.show(
		this.btnModuleAdd,
		0,
		this.btnModuleAdd.getHeight() );
  }


  private void doModuleEdit()
  {
    int[] viewRows = this.tableModule.getSelectedRows();
    if( viewRows != null ) {
      if( viewRows.length == 1 ) {
	int row = this.tableModule.convertRowIndexToModel( viewRows[ 0 ] );
	if( row >= 0 ) {
	  String[] cells = this.tableModelModule.getRow( row );
	  if( cells != null ) {
	    if( cells.length > 1 ) {
	      String moduleName = cells[ 1 ];
	      if( moduleName != null ) {
		if( moduleName.equals( "M025" )
		    || moduleName.equals( "M028" )
		    || moduleName.equals( "M040" ) )
		{
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
	int nRows = this.tableModelModule.getRowCount();
	for( int i = 0; i < nRows; i++ ) {
	  this.tableModelModule.setValueAt(
			String.format( "%02X", (i + 2) * 4 ), i, 0 );
	}
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
	    EmuUtil.fireSelectRow( this.tableModule, row2 );
	    fireDataChanged();
	  }
	}
      }
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
	  String s = modules[ i ][ 0 ];
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
	if( moduleName.equals( "M025" )
	    || moduleName.equals( "M028" )
	    || moduleName.equals( "M040" ) )
	{
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
	  if( typeByteText.equals( "F7" ) ) {
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
      }
      if( moduleDesc == null ) {
	for( String[] cells : modules ) {
	  if( cells.length > 1 ) {
	    if( cells[ 0 ].equals( moduleName ) ) {
	      moduleDesc = cells[ 1 ];
	    }
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


  private File selectRomFile( File oldFile, String title )
  {
    File rv   = null;
    File file = EmuUtil.showFileOpenDlg(
				this.settingsFrm,
				title,
				oldFile != null ?
					oldFile
					: Main.getLastPathFile( "rom" ),
				EmuUtil.getROMFileFilter() );
    if( file != null ) {
      String msg = null;
      if( file.exists() ) {
	if( file.isFile() ) {
	  if( file.canRead() ) {
	    if( file.length() > 0 ) {
	      rv = file;
	      Main.setLastFile( file, "rom" );
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
	BasicDlg.showErrorDlg( this, file.getPath() + ": " + msg );
      }
    }
    return rv;
  }


  private void updD004FieldsEnabled()
  {
    boolean state = this.btnD004Enabled.isSelected();
    this.tabGIDE.setEnabled( state );
    this.labelD004Rom.setEnabled( state );
    this.comboD004Rom.setEnabled( state );
    this.labelD004Speed.setEnabled( state );
    this.btnD004Speed4MHz.setEnabled( state );
    this.btnD004Speed8MHz.setEnabled( state );
    this.btnD004Speed16MHz.setEnabled( state );
    if( state && (this.comboD004Rom.getSelectedIndex() != 2) ) {
      state = false;
    }
    this.fldD004RomFile.setEnabled( state );
    this.btnD004RomFileSelect.setEnabled( state );
    if( state ) {
      state = (this.fldD004RomFile.getFile() != null);
    }
    this.btnD004RomFileRemove.setEnabled( state );
  }
}
