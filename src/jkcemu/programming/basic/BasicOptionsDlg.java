/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer BASIC-Compiler-Optionen
 */

package jkcemu.programming.basic;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuThread;
import jkcemu.base.HexDocument;
import jkcemu.base.IntegerDocument;
import jkcemu.base.LimitedDocument;
import jkcemu.base.UserInputException;
import jkcemu.programming.AbstractOptionsDlg;
import jkcemu.programming.PrgOptions;
import jkcemu.programming.basic.target.AC1Target;
import jkcemu.programming.basic.target.CPMTarget;
import jkcemu.programming.basic.target.HueblerGraphicsMCTarget;
import jkcemu.programming.basic.target.KC854Target;
import jkcemu.programming.basic.target.KC85Target;
import jkcemu.programming.basic.target.KramerMCTarget;
import jkcemu.programming.basic.target.LLC2HIRESTarget;
import jkcemu.programming.basic.target.SCCHTarget;
import jkcemu.programming.basic.target.Z1013PetersTarget;
import jkcemu.programming.basic.target.Z1013Target;
import jkcemu.programming.basic.target.Z9001KRTTarget;
import jkcemu.programming.basic.target.Z9001Target;


public class BasicOptionsDlg extends AbstractOptionsDlg
{
  private static final String textCodeBegAddr =
			"Anfangsadresse Programmcode:";
  private static final String textHeapSize =
			"Gr\u00F6\u00DFe Zeichenkettenspeicher:";

  private static final AbstractTarget[] targets = {
				new CPMTarget(),
				new SCCHTarget(),
				new AC1Target(),
				new LLC2HIRESTarget(),
				new HueblerGraphicsMCTarget(),
				new Z9001Target(),
				new Z9001KRTTarget(),
				new KC85Target(),
				new KC854Target(),
				new KramerMCTarget(),
				new Z1013Target(),
				new Z1013PetersTarget() };

  private EmuSys            emuSys;
  private JTabbedPane       tabbedPane;
  private JRadioButton      btnStackSystem;
  private JRadioButton      btnStackSeparate;
  private JRadioButton      btnBssTrailed;
  private JRadioButton      btnBssBegAddr;
  private JRadioButton      btnLangDE;
  private JRadioButton      btnLangEN;
  private JRadioButton      btnCheckAll;
  private JRadioButton      btnCheckNone;
  private JRadioButton      btnCheckCustom;
  private JRadioButton      btnBreakAlways;
  private JRadioButton      btnBreakInput;
  private JRadioButton      btnBreakNever;
  private JComboBox<Object> comboTarget;
  private JCheckBox         btnCheckBounds;
  private JCheckBox         btnCheckStack;
  private JCheckBox         btnOpenCrtEnabled;
  private JCheckBox         btnOpenLptEnabled;
  private JCheckBox         btnOpenFileEnabled;
  private JCheckBox         btnOpenVdipEnabled;
  private JCheckBox         btnInclBasicLines;
  private JCheckBox         btnPreferRelJumps;
  private JCheckBox         btnPrintLineNumOnAbort;
  private JCheckBox         btnShowAsm;
  private JCheckBox         btnWarnNonAsciiChars;
  private JCheckBox         btnWarnUnusedItems;
  private JTextField        fldAppName;
  private JTextField        fldCodeBegAddr;
  private JTextField        fldBssBegAddr;
  private JTextField        fldHeapSize;
  private JTextField        fldStackSize;
  private JLabel            labelAppName;
  private JLabel            labelBssBegAddrUnit;
  private JLabel            labelStackUnit;
  private LimitedDocument   docAppName;
  private HexDocument       docCodeBegAddr;
  private HexDocument       docBssBegAddr;
  private IntegerDocument   docHeapSize;
  private IntegerDocument   docStackSize;


  public BasicOptionsDlg(
		Frame      owner,
		EmuThread  emuThread,
		PrgOptions options )
  {
    super( owner, emuThread, "BASIC-Compiler-Optionen" );
    this.emuThread = emuThread;
    this.emuSys    = (emuThread != null ? emuThread.getEmuSys() : null);

    BasicOptions basicOptions = null;
    if( options != null ) {
      if( options instanceof BasicOptions ) {
	basicOptions = (BasicOptions) options;
      }
    }


    // vorausgewaehltes Zielsystem festlegen
    String  targetText    = null;
    boolean resetBegAddrs = false;
    if( basicOptions != null ) {
      targetText = basicOptions.getTargetText();
      if( targetText != null ) {
	/*
	 * Wenn sich das emulierte System geaendert hat,
	 * wird das Zielsystem entsprechend neu ausgewaehlt.
	 */
	EmuSys lastEmuSys = basicOptions.getEmuSys();
	if( (this.emuSys != null) && (lastEmuSys != null) ) {
	  if( this.emuSys != lastEmuSys ) {
	    targetText    = null;
	    resetBegAddrs = true;
	  }
	}
      }
    }


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, gbc );


    // Bereich Allgemein
    JPanel panelGeneral = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Allgemein", panelGeneral );

    GridBagConstraints gbcGeneral = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    panelGeneral.add( new JLabel( "Zielsystem:" ), gbcGeneral );
    gbcGeneral.gridy++;
    this.labelAppName = new JLabel( "Name des Programms:" );
    panelGeneral.add( this.labelAppName, gbcGeneral );
    gbcGeneral.gridy++;
    panelGeneral.add( new JLabel( textCodeBegAddr ), gbcGeneral );
    gbcGeneral.gridy++;
    panelGeneral.add( new JLabel( "Variablen/Speicherzellen:" ), gbcGeneral );
    gbcGeneral.gridy++;
    gbcGeneral.gridy++;
    panelGeneral.add( new JLabel( textHeapSize ), gbcGeneral );
    gbcGeneral.gridy++;
    panelGeneral.add( new JLabel( "Stack:" ), gbcGeneral );
    gbcGeneral.gridy++;
    gbcGeneral.gridy++;
    panelGeneral.add(
		new JLabel( "Sprache der Laufzeitausschriften:" ),
		gbcGeneral );

    boolean forceCodeToEmu  = false;
    int     presetTargetIdx = -1;
    if( targetText != null ) {
      for( int i = 0; i < targets.length; i++ ) {
	if( targetText.equals( targets[ i ].toString() ) ) {
	  presetTargetIdx = i;
	  break;
	}
      }
    } else if( emuSys != null ) {
      int lastLevel = 0;
      for( int i = 0; i < targets.length; i++ ) {
	int level = targets[ i ].getCompatibilityLevel( emuSys );
	if( level > lastLevel ) {
	  presetTargetIdx = i;
	  forceCodeToEmu  = true;
	  lastLevel       = level;
	}
      }
    }
    if( presetTargetIdx >= 0 ) {
      this.comboTarget = new JComboBox<>( (Object[]) targets );
      this.comboTarget.setEditable( false );
    } else {
      this.comboTarget = new JComboBox<>();
      this.comboTarget.setEditable( false );
      this.comboTarget.addItem( "Bitte ausw\u00E4hlen" );
      for( AbstractTarget target : targets ) {
	this.comboTarget.addItem( target );
      }
      presetTargetIdx = 0;
    }
    try {
      this.comboTarget.setSelectedIndex( presetTargetIdx );
    }
    catch( IllegalArgumentException ex ) {}
    gbcGeneral.anchor      = GridBagConstraints.WEST;
    gbcGeneral.insets.left = 0;
    gbcGeneral.gridwidth   = GridBagConstraints.REMAINDER;
    gbcGeneral.gridy       = 0;
    gbcGeneral.gridx++;
    panelGeneral.add( this.comboTarget, gbcGeneral );

    this.docAppName = new LimitedDocument( 8 );
    this.docAppName.setAsciiOnly( true );
    this.fldAppName = new JTextField( this.docAppName, "", 9 );
    gbcGeneral.gridy++;
    panelGeneral.add( this.fldAppName, gbcGeneral );

    this.docCodeBegAddr  = new HexDocument( 4 );
    this.fldCodeBegAddr  = new JTextField( this.docCodeBegAddr, "", 5 );
    gbcGeneral.gridwidth = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.fldCodeBegAddr, gbcGeneral );

    gbcGeneral.gridx++;
    panelGeneral.add( new JLabel( "hex" ), gbcGeneral );

    ButtonGroup grpBss = new ButtonGroup();

    this.btnBssTrailed = new JRadioButton(
				"direkt hinter Programmcode",
				true );
    grpBss.add( this.btnBssTrailed );
    gbcGeneral.gridwidth = GridBagConstraints.REMAINDER;
    gbcGeneral.gridx     = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.btnBssTrailed, gbcGeneral );

    JPanel panelBssBegAddr = new JPanel();
    panelBssBegAddr.setLayout(
		new BoxLayout( panelBssBegAddr, BoxLayout.X_AXIS ) );
    gbcGeneral.insets.top = 0;
    gbcGeneral.gridy++;
    panelGeneral.add( panelBssBegAddr, gbcGeneral );

    this.btnBssBegAddr = new JRadioButton( "Ab Adresse:" );
    grpBss.add( this.btnBssBegAddr );
    panelBssBegAddr.add( this.btnBssBegAddr );
    panelBssBegAddr.add( Box.createRigidArea( new Dimension( 5, 0 ) ) );

    this.docBssBegAddr = new HexDocument( 4 );
    this.fldBssBegAddr = new JTextField( this.docBssBegAddr, "", 5 );
    panelBssBegAddr.add( this.fldBssBegAddr );
    panelBssBegAddr.add( Box.createRigidArea( new Dimension( 5, 0 ) ) );

    this.labelBssBegAddrUnit = new JLabel( "hex" );
    panelBssBegAddr.add( this.labelBssBegAddrUnit );

    this.fldHeapSize = new JTextField( 5 );
    this.docHeapSize = new IntegerDocument(
		fldHeapSize,
		BasicOptions.MIN_HEAP_SIZE,
		BasicOptions.MAX_HEAP_SIZE );
    gbcGeneral.insets.top = 5;
    gbcGeneral.gridx      = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.fldHeapSize, gbcGeneral );

    gbcGeneral.gridx++;
    panelGeneral.add( new JLabel( "Bytes" ), gbcGeneral );

    ButtonGroup grpStack = new ButtonGroup();

    this.btnStackSystem = new JRadioButton(
					"System-Stack verwenden",
					true );
    grpStack.add( this.btnStackSystem );
    gbcGeneral.gridwidth = GridBagConstraints.REMAINDER;
    gbcGeneral.gridx     = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.btnStackSystem, gbcGeneral );

    JPanel panelStackSeparate = new JPanel();
    panelStackSeparate.setLayout(
		new BoxLayout( panelStackSeparate, BoxLayout.X_AXIS ) );
    gbcGeneral.insets.top = 0;
    gbcGeneral.gridy++;
    panelGeneral.add( panelStackSeparate, gbcGeneral );

    this.btnStackSeparate = new JRadioButton(
					"Eigener Stack-Bereich:",
					false );
    grpStack.add( this.btnStackSeparate );
    panelStackSeparate.add( this.btnStackSeparate );
    panelStackSeparate.add( Box.createRigidArea( new Dimension( 5, 0 ) ) );

    this.fldStackSize = new JTextField( 5 );
    this.docStackSize = new IntegerDocument(
		this.fldStackSize,
		BasicOptions.MIN_STACK_SIZE,
		null );
    panelStackSeparate.add( this.fldStackSize );
    panelStackSeparate.add( Box.createRigidArea( new Dimension( 5, 0 ) ) );

    this.labelStackUnit = new JLabel( "Bytes" );
    panelStackSeparate.add( this.labelStackUnit );

    JPanel panelLang = new JPanel();
    panelLang.setLayout( new BoxLayout( panelLang, BoxLayout.X_AXIS ) );
    gbcGeneral.insets.top = 5;
    gbcGeneral.gridx      = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( panelLang, gbcGeneral );

    ButtonGroup grpLang = new ButtonGroup();

    this.btnLangDE = new JRadioButton( "Deutsch" );
    grpLang.add( this.btnLangDE );
    panelLang.add( this.btnLangDE );

    this.btnLangEN = new JRadioButton( "Englisch" );
    grpLang.add( this.btnLangEN );
    panelLang.add( this.btnLangEN );


    // Bereich Laufzeiteigenschaften
    JPanel panelCheck = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Laufzeiteigenschaften", panelCheck );

    GridBagConstraints gbcCheck = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpCheck = new ButtonGroup();

    this.btnCheckAll = new JRadioButton(
				"Compilieren f\u00FCr Test und Debugging",
				true );
    grpCheck.add( this.btnCheckAll );
    panelCheck.add( this.btnCheckAll, gbcCheck );

    this.btnCheckNone = new JRadioButton(
				"Compilieren f\u00FCr Produktiveinsatz",
				false );
    grpCheck.add( this.btnCheckNone );
    gbcCheck.insets.top = 0;
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckNone, gbcCheck );

    this.btnCheckCustom = new JRadioButton( "Benutzerdefiniert", false );
    grpCheck.add( this.btnCheckCustom );
    gbcCheck.insets.bottom = 5;
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckCustom, gbcCheck );

    ButtonGroup grpBreak = new ButtonGroup();

    this.btnBreakAlways = new JRadioButton(
		"CTRL-C bricht Programm ab",
		true );
    grpBreak.add( this.btnBreakAlways );
    gbcCheck.insets.top    = 5;
    gbcCheck.insets.bottom = 0;
    gbcCheck.insets.left   = 20;
    gbcCheck.gridy         = 0;
    gbcCheck.gridx++;
    panelCheck.add( this.btnBreakAlways, gbcCheck );

    this.btnBreakInput = new JRadioButton(
		"CTRL-C bricht Programm nur bei Eingaben ab",
		false );
    grpBreak.add( this.btnBreakInput );
    gbcCheck.insets.top = 0;
    gbcCheck.gridy++;
    panelCheck.add( this.btnBreakInput, gbcCheck );

    this.btnBreakNever = new JRadioButton(
		"CTRL-C bricht Programm nicht ab",
		false );
    grpBreak.add( this.btnBreakNever );
    gbcCheck.gridy++;
    panelCheck.add( this.btnBreakNever, gbcCheck );

    this.btnCheckBounds = new JCheckBox( "Feldgrenzen pr\u00FCfen", true );
    gbcCheck.insets.top = 5;
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckBounds, gbcCheck );

    this.btnCheckStack = new JCheckBox( "Stack pr\u00FCfen", true );
    gbcCheck.insets.top = 0;
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckStack, gbcCheck );

    this.btnPrintLineNumOnAbort = new JCheckBox(
		"Bei Abbruch aufgrund eines Fehlers Zeilennummer ausgeben",
		true );
    gbcCheck.insets.bottom = 5;
    gbcCheck.gridy++;
    panelCheck.add( this.btnPrintLineNumOnAbort, gbcCheck );


    // Bereich Treiber
    JPanel panelDriver = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Treiber", panelDriver );

    GridBagConstraints gbcDriver = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    // Unterbereich OPEN-Anweisung
    panelDriver.add(
	new JLabel( "Bei Verwendung der OPEN-Anweisung"
			+ " folgende Treiber einbinden:" ),
	gbcDriver );

    this.btnOpenCrtEnabled = new JCheckBox(
		"CRT-Treiber (Ausgabekanal auf Bildschirm)" );
    gbcDriver.insets.top  = 0;
    gbcDriver.insets.left = 50;
    gbcDriver.gridy++;
    panelDriver.add( this.btnOpenCrtEnabled, gbcDriver );

    this.btnOpenLptEnabled = new JCheckBox(
		"LPT-Treiber (Ausgabekanal auf Drucker, nur relevant"
			+ " wenn vom Zielsystem unterst\u00FCtzt)" );
    gbcDriver.insets.bottom = 0;
    gbcDriver.gridy++;
    panelDriver.add( this.btnOpenLptEnabled, gbcDriver );

    this.btnOpenFileEnabled = new JCheckBox(
		"FILE-Treiber (Zugriff auf Dateisystem, nur relevant"
			+ " bei Zielsystem \'CP/M-kompatibel\')" );
    gbcDriver.gridy++;
    panelDriver.add( this.btnOpenFileEnabled, gbcDriver );

    this.btnOpenVdipEnabled = new JCheckBox(
		"VDIP-Treiber (Zugriff auf USB-Speicher, nur relevant"
			+ " wenn vom Zielsystem unterst\u00FCtzt)" );
    gbcDriver.insets.bottom = 5;
    gbcDriver.gridy++;
    panelDriver.add( this.btnOpenVdipEnabled, gbcDriver );


    // Bereich Erzeugter Programmcode
    this.tabbedPane.addTab(
			"Erzeugter Programmcode",
			createCodeDestOptions() );


    // Bereich Sonstiges
    JPanel panelEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", panelEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    // Unterbereich Fehlervermeidung
    panelEtc.add( new JLabel( "Fehlervermeidung:" ), gbcEtc );

    this.btnWarnNonAsciiChars = new JCheckBox(
					"Bei Nicht-ASCII-Zeichen warnen" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnWarnNonAsciiChars, gbcEtc );

    this.btnWarnUnusedItems = new JCheckBox(
		"Bei nicht verwendeten Funktionen, Prozeduren"
					+ " und Variablen warnen" );
    gbcEtc.gridy++;
    panelEtc.add( this.btnWarnUnusedItems, gbcEtc );


    // Unterbereich Optimierung
    gbcEtc.insets.top  = 15;
    gbcEtc.insets.left = 5;
    gbcEtc.gridy++;
    panelEtc.add( new JLabel( "Optimierung:" ), gbcEtc );

    this.btnPreferRelJumps = new JCheckBox(
				"Relative Spr\u00FCnge bevorzugen" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnPreferRelJumps, gbcEtc );


    // Unterbereich Assembler-Code
    gbcEtc.insets.top  = 15;
    gbcEtc.insets.left = 5;
    gbcEtc.gridy++;
    panelEtc.add( new JLabel( "Assembler-Code:" ), gbcEtc );

    this.btnShowAsm = new JCheckBox( "Erzeugten Assembler-Code anzeigen" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnShowAsm, gbcEtc );

    this.btnInclBasicLines = new JCheckBox(
			"BASIC-Zeilen als Kommentare einf\u00FCgen" );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.btnInclBasicLines, gbcEtc );


    // Bereich Knoepfe
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.weighty       = 0.0;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( createButtons( "Compilieren" ), gbc );


    // Vorbelegungen
    String langCode  = null;
    int    stackSize = BasicOptions.DEFAULT_STACK_SIZE;
    if( basicOptions != null ) {
      langCode  = basicOptions.getLangCode();
      stackSize = basicOptions.getStackSize();
      this.fldAppName.setText( basicOptions.getAppName() );
      if( !updBegAddrs(
		basicOptions.getCodeBegAddr(),
		basicOptions.getBssBegAddr() ) )
      {
	resetBegAddrs = true;
      }
      this.docHeapSize.setValue( basicOptions.getHeapSize() );
      switch( basicOptions.getBreakOption() ) {
	case INPUT:
	  this.btnBreakInput.setSelected( true );
	  break;
	case NEVER:
	  this.btnBreakNever.setSelected( true );
	  break;
	default:
	  this.btnBreakAlways.setSelected( true );
	  break;
      }
      this.btnCheckBounds.setSelected( basicOptions.getCheckBounds() );
      this.btnCheckStack.setSelected( basicOptions.getCheckStack() );
      this.btnOpenCrtEnabled.setSelected( basicOptions.isOpenCrtEnabled() );
      this.btnOpenLptEnabled.setSelected( basicOptions.isOpenLptEnabled() );
      this.btnOpenFileEnabled.setSelected( basicOptions.isOpenFileEnabled() );
      this.btnOpenVdipEnabled.setSelected( basicOptions.isOpenVdipEnabled() );
      this.btnPreferRelJumps.setSelected(
				basicOptions.getPreferRelativeJumps() );
      this.btnPrintLineNumOnAbort.setSelected(
				basicOptions.getPrintLineNumOnAbort() );
      this.btnShowAsm.setSelected( basicOptions.getShowAssemblerText() );
      this.btnInclBasicLines.setSelected(
				basicOptions.getIncludeBasicLines() );
      this.btnWarnNonAsciiChars.setSelected(
				basicOptions.getWarnNonAsciiChars() );
      this.btnWarnUnusedItems.setSelected(
				basicOptions.getWarnUnusedItems() );
    } else {
      this.fldAppName.setText( BasicOptions.DEFAULT_APP_NAME );
      this.docHeapSize.setValue( BasicOptions.DEFAULT_HEAP_SIZE );
      this.btnBreakAlways.setSelected( true );
      this.btnCheckBounds.setSelected( true );
      this.btnCheckStack.setSelected( true );
      this.btnOpenCrtEnabled.setSelected( true );
      this.btnOpenLptEnabled.setSelected( true );
      this.btnOpenFileEnabled.setSelected( true );
      this.btnOpenVdipEnabled.setSelected( true );
      this.btnPreferRelJumps.setSelected( true );
      this.btnPrintLineNumOnAbort.setSelected( true );
      this.btnShowAsm.setSelected( false );
      this.btnInclBasicLines.setSelected( true );
      this.btnWarnNonAsciiChars.setSelected( true );
      this.btnWarnUnusedItems.setSelected( true );
      resetBegAddrs = true;
    }
    if( langCode != null ) {
      if( langCode.equalsIgnoreCase( "DE" ) ) {
	this.btnLangDE.setSelected( true );
      } else {
	this.btnLangEN.setSelected( true );
      }
    } else {
      this.btnLangDE.setSelected( true );
    }
    if( this.btnBreakAlways.isSelected()
	&& this.btnCheckBounds.isSelected()
	&& this.btnCheckStack.isSelected()
	&& this.btnPrintLineNumOnAbort.isSelected() )
    {
      this.btnCheckAll.setSelected( true );
    }
    else if( this.btnBreakNever.isSelected()
	&& !this.btnCheckBounds.isSelected()
	&& !this.btnCheckStack.isSelected()
	&& !this.btnPrintLineNumOnAbort.isSelected() )
    {
      this.btnCheckNone.setSelected( true );
    } else {
      this.btnCheckCustom.setSelected( true );
    }
    if( stackSize > 0 ) {
      this.btnStackSeparate.setSelected( true );
      this.docStackSize.setValue( stackSize );
    } else {
      this.btnStackSystem.setSelected( true );
    }
    this.docAppName.setSwapCase( true );
    if( resetBegAddrs ) {
      updBegAddrsFromSelectedTarget();
    }
    updCodeToEmuFields();
    updCodeDestFields( options, forceCodeToEmu );
    updAppNameFieldsEnabled();
    updCheckFieldsEnabled();
    updStackFieldsEnabled();
    updInclBasicLinesEnabled();


    // Listener
    this.comboTarget.addActionListener( this );
    this.fldCodeBegAddr.addActionListener( this );
    this.btnBssTrailed.addActionListener( this );
    this.btnBssBegAddr.addActionListener( this );
    this.fldBssBegAddr.addActionListener( this );
    this.fldHeapSize.addActionListener( this );
    this.btnStackSystem.addActionListener( this );
    this.btnStackSeparate.addActionListener( this );
    this.fldStackSize.addActionListener( this );
    this.btnLangDE.addActionListener( this );
    this.btnLangEN.addActionListener( this );
    this.btnCheckAll.addActionListener( this );
    this.btnCheckNone.addActionListener( this );
    this.btnCheckCustom.addActionListener( this );
    this.btnShowAsm.addActionListener( this );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
  }


  protected void doApply()
  {
    String labelText = null;
    try {
      AbstractTarget target = null;
      Object         obj    = this.comboTarget.getSelectedItem();
      if( obj != null ) {
	if( obj instanceof AbstractTarget ) {
	  target = (AbstractTarget) obj;
	}
      }
      if( target == null ) {
	throw new UserInputException(
			"Sie m\u00FCssen ein Zielsystem ausw\u00E4hlen!" );
      }

      labelText       = textCodeBegAddr;
      int codeBegAddr = this.docCodeBegAddr.intValue();
      int actualAddr  = codeBegAddr;

      labelText      = "Anfangsadresse Variablen/Speicherzellen:";
      int bssBegAddr = -1;
      if( this.btnBssBegAddr.isSelected() ) {
	bssBegAddr = this.docBssBegAddr.intValue();
      }

      labelText    = textHeapSize;
      int heapSize = this.docHeapSize.intValue();

      labelText     = "Stack-Gr\u00F6\u00DFe:";
      int stackSize = 0;
      if( this.btnStackSeparate.isSelected() ) {
	stackSize = this.docStackSize.intValue();
      }
      labelText = null;

      BasicOptions.BreakOption breakOption = BasicOptions.BreakOption.ALWAYS;
      if( this.btnBreakInput.isSelected() ) {
	breakOption = BasicOptions.BreakOption.INPUT;
      }
      else if( this.btnBreakNever.isSelected() ) {
	breakOption = BasicOptions.BreakOption.NEVER;
      }

      String       appName = this.fldAppName.getText();
      BasicOptions options = new BasicOptions();
      options.setTarget( target );
      options.setAppName( appName != null ? appName.trim() : null );
      options.setCodeBegAddr( codeBegAddr );
      options.setBssBegAddr( bssBegAddr );
      options.setHeapSize( heapSize );
      options.setStackSize( stackSize );
      options.setLangCode( this.btnLangDE.isSelected() ? "DE" : "EN" );
      options.setBreakOption( breakOption );
      options.setCheckBounds( this.btnCheckBounds.isSelected() );
      options.setCheckStack( this.btnCheckStack.isSelected() );
      options.setOpenCrtEnabled( this.btnOpenCrtEnabled.isSelected() );
      options.setOpenLptEnabled( this.btnOpenLptEnabled.isSelected() );
      options.setOpenFileEnabled( this.btnOpenFileEnabled.isSelected() );
      options.setOpenVdipEnabled( this.btnOpenVdipEnabled.isSelected() );
      options.setPreferRelativeJumps( this.btnPreferRelJumps.isSelected() );
      options.setPrintLineNumOnAbort(
			this.btnPrintLineNumOnAbort.isSelected() );
      options.setShowAssemblerText( this.btnShowAsm.isSelected() );
      options.setIncludeBasicLines( this.btnInclBasicLines.isSelected() );
      options.setWarnNonAsciiChars( this.btnWarnNonAsciiChars.isSelected() );
      options.setWarnUnusedItems( this.btnWarnUnusedItems.isSelected() );
      try {
	applyCodeDestOptionsTo( options );
	this.appliedOptions = options;
	doClose();
      }
      catch( UserInputException ex ) {
	showErrorDlg( this, "Erzeugter Programmcode:\n" + ex.getMessage() );
      }
    }
    catch( NumberFormatException ex ) {
      String msg = ex.getMessage();
      if( labelText != null ) {
	msg = labelText + " " + msg;
      }
      showErrorDlg( this, msg );
    }
    catch( UserInputException ex ) {
      showErrorDlg( this, ex );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = super.doAction( e );
    if( !rv && (e != null) ) {
      Object src = e.getSource();
      if( src == this.comboTarget ) {
	rv = true;
	updAppNameFieldsEnabled();
	updBegAddrsFromSelectedTarget();
	updCodeToEmuFields();
      }
      else if( (src == this.btnBssTrailed)
	       || (src == this.btnBssBegAddr) )
      {
	rv = true;
	updBssFieldsEnabled();
      }
      else if( (src == this.btnStackSystem)
	       || (src == this.btnStackSeparate) )
      {
	rv = true;
	updStackFieldsEnabled();
      }
      else if( src == this.btnCheckAll ) {
	rv = true;
	this.btnBreakAlways.setSelected( true );
	this.btnCheckBounds.setSelected( true );
	this.btnCheckStack.setSelected( true );
	this.btnPrintLineNumOnAbort.setSelected( true );
	updCheckFieldsEnabled();
      }
      else if( src == this.btnCheckNone ) {
	rv = true;
	this.btnBreakNever.setSelected( true );
	this.btnCheckBounds.setSelected( false );
	this.btnCheckStack.setSelected( false );
	this.btnPrintLineNumOnAbort.setSelected( false );
	updCheckFieldsEnabled();
      }
      else if( src == this.btnCheckCustom ) {
	rv = true;
	updCheckFieldsEnabled();
      }
      else if( src == this.btnShowAsm ) {
	rv = true;
	updInclBasicLinesEnabled();
      } else {
	rv = true;
	if( src instanceof JTextField ) {
	  ((JTextField) src).transferFocus();
	}
      }
    }
    return rv;
  }


  @Override
  public void settingsChanged()
  {
    super.settingsChanged();
    if( this.emuThread != null ) {
      EmuSys curEmuSys = this.emuThread.getEmuSys();
      if( curEmuSys != null ) {
	boolean state = true;
	if( this.emuSys != null ) {
	  if( this.emuSys.equals( curEmuSys ) ) {
	    state = false;
	  }
	}
	int   targetIdx = 0;
	this.emuSys     = curEmuSys;
	if( emuSys != null ) {
	  int n = this.comboTarget.getItemCount();
	  for( int i = 0; i < n; i++ ) {
	    Object o = this.comboTarget.getItemAt( i );
	    if( o != null ) {
	      if( o instanceof AbstractTarget ) {
		if( ((AbstractTarget) o).getCompatibilityLevel( emuSys ) > 0 )
		{
		  targetIdx = i;
		  break;
		}
	      }
	    }
	  }
	}
	try {
	  this.comboTarget.setSelectedIndex( targetIdx );
	}
	catch( IllegalArgumentException ex ) {}
      }
    }
  }


	/* --- private Methoden --- */

  private void updAppNameFieldsEnabled()
  {
    boolean state = false;
    Object  obj   = this.comboTarget.getSelectedItem();
    if( obj != null ) {
      if( obj instanceof AbstractTarget ) {
	if( ((AbstractTarget) obj).getMaxAppNameLen() > 0 ) {
	  state = true;
	}
      }
    }
    this.labelAppName.setEnabled( state );
    this.fldAppName.setEnabled( state );
  }


  private boolean updBegAddrs( int codeBegAddr, int bssBegAddr )
  {
    boolean rv = false;
    if( codeBegAddr >= 0 ) {
      this.docCodeBegAddr.setValue( codeBegAddr, 4 );
      rv = true;
    } else {
      this.fldCodeBegAddr.setText( "" );
    }
    if( bssBegAddr >= 0 ) {
      this.docBssBegAddr.setValue( bssBegAddr, 4 );
      this.btnBssBegAddr.setSelected( true );
    } else {
      this.fldBssBegAddr.setText( "" );
      this.btnBssTrailed.setSelected( true );
    }
    updBssFieldsEnabled();
    return rv;
  }


  private void updBegAddrsFromSelectedTarget()
  {
    int     codeBegAddr = -1;
    Object  obj         = this.comboTarget.getSelectedItem();
    if( obj != null ) {
      if( obj instanceof AbstractTarget ) {
	codeBegAddr = ((AbstractTarget) obj).getDefaultBegAddr();
      }
    }
    updBegAddrs( codeBegAddr, -1 );
  }


  private void updBssFieldsEnabled()
  {
    boolean state = this.btnBssBegAddr.isSelected();
    this.fldBssBegAddr.setEnabled( state );
    this.labelBssBegAddrUnit.setEnabled( state );
  }


  private void updCheckFieldsEnabled()
  {
    boolean state = this.btnCheckCustom.isSelected();
    this.btnBreakAlways.setEnabled( state );
    this.btnBreakInput.setEnabled( state );
    this.btnBreakNever.setEnabled( state );
    this.btnCheckBounds.setEnabled( state );
    this.btnCheckStack.setEnabled( state );
    this.btnPrintLineNumOnAbort.setEnabled( state );
  }


  private void updCodeToEmuFields()
  {
    boolean codeToEmu = false;
    if( this.emuSys != null ) {
      Object o = this.comboTarget.getSelectedItem();
      if( o != null ) {
	if( o instanceof AbstractTarget ) {
	  if( ((AbstractTarget) o).getCompatibilityLevel( this.emuSys ) > 0 )
	  {
	    codeToEmu = true;
	  }
	}
      }
    }
    /*
     * Programmcode nicht in Emulator laden, wenn das aktuell
     * emulierte System vom Compiler nicht unterstuetzt wird.
     */
    if( !codeToEmu ) {
      setCodeToEmu( false );
    }
  }


  private void updInclBasicLinesEnabled()
  {
    this.btnInclBasicLines.setEnabled( this.btnShowAsm.isSelected() );
  }


  private void updStackFieldsEnabled()
  {
    boolean state = this.btnStackSeparate.isSelected();
    this.fldStackSize.setEnabled( state );
    this.labelStackUnit.setEnabled( state );
  }
}
