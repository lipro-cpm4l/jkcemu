/*
 * (c) 2008-2021 Jens Mueller
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
import jkcemu.base.GUIFactory;
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
import jkcemu.programming.basic.target.KC85Caos48Target;
import jkcemu.programming.basic.target.KC85Target;
import jkcemu.programming.basic.target.KramerMCTarget;
import jkcemu.programming.basic.target.LLC2HIRESTarget;
import jkcemu.programming.basic.target.SCCHTarget;
import jkcemu.programming.basic.target.Z1013PetersTarget;
import jkcemu.programming.basic.target.Z1013Target;
import jkcemu.programming.basic.target.Z1013KRTTarget;
import jkcemu.programming.basic.target.Z1013ZXTarget;
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
				new KC85Caos48Target(),
				new KramerMCTarget(),
				new Z1013Target(),
				new Z1013PetersTarget(),
				new Z1013KRTTarget(),
				new Z1013ZXTarget() };

  private EmuSys            emuSys;
  private JTabbedPane       tabbedPane;
  private JRadioButton      rbAppTypeStandalone;
  private JRadioButton      rbAppTypeSub;
  private JRadioButton      rbStackSystem;
  private JRadioButton      rbStackSeparate;
  private JRadioButton      rbBssTrailed;
  private JRadioButton      rbBssBegAddr;
  private JRadioButton      rbLangDE;
  private JRadioButton      rbLangEN;
  private JRadioButton      rbCheckAll;
  private JRadioButton      rbCheckNone;
  private JRadioButton      rbCheckCustom;
  private JRadioButton      rbBreakAlways;
  private JRadioButton      rbBreakInput;
  private JRadioButton      rbBreakNever;
  private JComboBox<Object> comboTarget;
  private JCheckBox         cbCheckBounds;
  private JCheckBox         cbCheckStack;
  private JCheckBox         cbOpenCrtEnabled;
  private JCheckBox         cbOpenLptEnabled;
  private JCheckBox         cbOpenDiskEnabled;
  private JCheckBox         cbOpenVdipEnabled;
  private JCheckBox         cbInclBasicLines;
  private JCheckBox         cbInitVars;
  private JCheckBox         cbPreferRelJumps;
  private JCheckBox         cbPrintLineNumOnAbort;
  private JCheckBox         cbShowAsm;
  private JCheckBox         cbWarnImplicitDecls;
  private JCheckBox         cbWarnNonAsciiChars;
  private JCheckBox         cbWarnTooManyDigits;
  private JCheckBox         cbWarnUnusedItems;
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
    super( owner, emuThread, options, "BASIC-Compiler-Optionen" );
    this.emuThread = emuThread;
    this.emuSys    = (emuThread != null ? emuThread.getEmuSys() : null);


    // vorausgewaehltes Zielsystem festlegen
    String       targetText    = null;
    boolean      resetBegAddrs = false;
    BasicOptions basicOptions  = getBasicOptions( options );
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

    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, gbc );


    // Bereich Allgemein
    JPanel panelGeneral = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Allgemein", panelGeneral );

    GridBagConstraints gbcGeneral = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    panelGeneral.add( GUIFactory.createLabel( "Zielsystem:" ), gbcGeneral );
    gbcGeneral.gridy++;
    panelGeneral.add(
		GUIFactory.createLabel( "Art des Programms:" ),
		gbcGeneral );
    gbcGeneral.gridy++;
    this.labelAppName = GUIFactory.createLabel( "Name des Programms:" );
    panelGeneral.add( this.labelAppName, gbcGeneral );
    gbcGeneral.gridy++;
    panelGeneral.add(
		GUIFactory.createLabel( textCodeBegAddr ),
		gbcGeneral );
    gbcGeneral.gridy++;
    panelGeneral.add(
		GUIFactory.createLabel( "Variablen/Speicherzellen:" ),
		gbcGeneral );
    gbcGeneral.gridy++;
    gbcGeneral.gridy++;
    panelGeneral.add( GUIFactory.createLabel( textHeapSize ), gbcGeneral );
    gbcGeneral.gridy++;
    panelGeneral.add( GUIFactory.createLabel( "Stack:" ), gbcGeneral );
    gbcGeneral.gridy++;
    gbcGeneral.gridy++;
    panelGeneral.add(
		GUIFactory.createLabel( "Sprache der Ausschriften:" ),
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
      this.comboTarget = GUIFactory.createComboBox( (Object[]) targets );
      this.comboTarget.setEditable( false );
    } else {
      this.comboTarget = GUIFactory.createComboBox();
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

    JPanel panelAppType = GUIFactory.createPanel();
    panelAppType.setLayout(
		new BoxLayout( panelAppType, BoxLayout.X_AXIS ) );
    gbcGeneral.gridy++;
    panelGeneral.add( panelAppType, gbcGeneral );

    ButtonGroup grpAppType = new ButtonGroup();

    this.rbAppTypeStandalone = GUIFactory.createRadioButton(
				"Eigenst\u00E4ndiges Programm",
				true );
    grpAppType.add( this.rbAppTypeStandalone );
    panelAppType.add( this.rbAppTypeStandalone );
    panelAppType.add( Box.createHorizontalStrut( 5 ) );

    this.rbAppTypeSub = GUIFactory.createRadioButton( "Unterprogramm" );
    grpAppType.add( this.rbAppTypeSub );
    panelAppType.add( this.rbAppTypeSub );

    this.docAppName = new LimitedDocument( 8 );
    this.docAppName.setAsciiOnly( true );
    this.fldAppName = GUIFactory.createTextField( this.docAppName, 9 );
    gbcGeneral.gridy++;
    panelGeneral.add( this.fldAppName, gbcGeneral );

    this.docCodeBegAddr  = new HexDocument( 4 );
    this.fldCodeBegAddr  = GUIFactory.createTextField(
						this.docCodeBegAddr,
						5 );
    gbcGeneral.gridwidth = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.fldCodeBegAddr, gbcGeneral );

    gbcGeneral.gridx++;
    panelGeneral.add( GUIFactory.createLabel( "hex" ), gbcGeneral );

    ButtonGroup grpBss = new ButtonGroup();

    this.rbBssTrailed = GUIFactory.createRadioButton(
				"Direkt hinter Programmcode",
				true );
    grpBss.add( this.rbBssTrailed );
    gbcGeneral.gridwidth = GridBagConstraints.REMAINDER;
    gbcGeneral.gridx     = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.rbBssTrailed, gbcGeneral );

    JPanel panelBssBegAddr = GUIFactory.createPanel();
    panelBssBegAddr.setLayout(
		new BoxLayout( panelBssBegAddr, BoxLayout.X_AXIS ) );
    gbcGeneral.insets.top = 0;
    gbcGeneral.gridy++;
    panelGeneral.add( panelBssBegAddr, gbcGeneral );

    this.rbBssBegAddr = GUIFactory.createRadioButton( "Ab Adresse:" );
    grpBss.add( this.rbBssBegAddr );
    panelBssBegAddr.add( this.rbBssBegAddr );
    panelBssBegAddr.add( Box.createHorizontalStrut( 5 ) );

    this.docBssBegAddr = new HexDocument( 4 );
    this.fldBssBegAddr = GUIFactory.createTextField( this.docBssBegAddr, 5 );
    panelBssBegAddr.add( this.fldBssBegAddr );
    panelBssBegAddr.add( Box.createHorizontalStrut( 5 ) );

    this.labelBssBegAddrUnit = GUIFactory.createLabel( "hex" );
    panelBssBegAddr.add( this.labelBssBegAddrUnit );

    this.fldHeapSize = GUIFactory.createTextField( 5 );
    this.docHeapSize = new IntegerDocument(
		fldHeapSize,
		BasicOptions.MIN_HEAP_SIZE,
		BasicOptions.MAX_HEAP_SIZE );
    gbcGeneral.insets.top = 5;
    gbcGeneral.gridx      = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.fldHeapSize, gbcGeneral );

    gbcGeneral.gridx++;
    panelGeneral.add( GUIFactory.createLabel( "Bytes" ), gbcGeneral );

    ButtonGroup grpStack = new ButtonGroup();

    this.rbStackSystem = GUIFactory.createRadioButton(
				"Stack des aufrufenden Programms verwenden",
				true );
    grpStack.add( this.rbStackSystem );
    gbcGeneral.gridwidth = GridBagConstraints.REMAINDER;
    gbcGeneral.gridx     = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.rbStackSystem, gbcGeneral );

    JPanel panelStackSeparate = GUIFactory.createPanel();
    panelStackSeparate.setLayout(
		new BoxLayout( panelStackSeparate, BoxLayout.X_AXIS ) );
    gbcGeneral.insets.top = 0;
    gbcGeneral.gridy++;
    panelGeneral.add( panelStackSeparate, gbcGeneral );

    this.rbStackSeparate = GUIFactory.createRadioButton(
		"Eigener Stack-Bereich hinter Variablen/Speicherzellen:" );
    grpStack.add( this.rbStackSeparate );
    panelStackSeparate.add( this.rbStackSeparate );
    panelStackSeparate.add( Box.createHorizontalStrut( 5 ) );

    this.fldStackSize = GUIFactory.createTextField( 5 );
    this.docStackSize = new IntegerDocument(
		this.fldStackSize,
		BasicOptions.MIN_STACK_SIZE,
		null );
    panelStackSeparate.add( this.fldStackSize );
    panelStackSeparate.add( Box.createHorizontalStrut( 5 ) );

    this.labelStackUnit = GUIFactory.createLabel( "Bytes" );
    panelStackSeparate.add( this.labelStackUnit );

    JPanel panelLang = GUIFactory.createPanel();
    panelLang.setLayout( new BoxLayout( panelLang, BoxLayout.X_AXIS ) );
    gbcGeneral.insets.top    = 5;
    gbcGeneral.insets.bottom = 5;
    gbcGeneral.gridx         = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( panelLang, gbcGeneral );

    ButtonGroup grpLang = new ButtonGroup();

    this.rbLangDE = GUIFactory.createRadioButton( "Deutsch" );
    grpLang.add( this.rbLangDE );
    panelLang.add( this.rbLangDE );

    this.rbLangEN = GUIFactory.createRadioButton( "Englisch" );
    grpLang.add( this.rbLangEN );
    panelLang.add( this.rbLangEN );


    // Bereich Laufzeiteigenschaften
    JPanel panelCheck = GUIFactory.createPanel( new GridBagLayout() );
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

    this.rbCheckAll = GUIFactory.createRadioButton(
				"Compilieren f\u00FCr Test und Debugging",
				true );
    grpCheck.add( this.rbCheckAll );
    panelCheck.add( this.rbCheckAll, gbcCheck );

    this.rbCheckNone = GUIFactory.createRadioButton(
				"Compilieren f\u00FCr Produktiveinsatz",
				false );
    grpCheck.add( this.rbCheckNone );
    gbcCheck.insets.top = 0;
    gbcCheck.gridy++;
    panelCheck.add( this.rbCheckNone, gbcCheck );

    this.rbCheckCustom = GUIFactory.createRadioButton(
						"Benutzerdefiniert" );
    grpCheck.add( this.rbCheckCustom );
    gbcCheck.insets.bottom = 5;
    gbcCheck.gridy++;
    panelCheck.add( this.rbCheckCustom, gbcCheck );

    ButtonGroup grpBreak = new ButtonGroup();

    this.rbBreakAlways = GUIFactory.createRadioButton(
				"CTRL-C bricht Programm ab",
				true );
    grpBreak.add( this.rbBreakAlways );
    gbcCheck.insets.top    = 5;
    gbcCheck.insets.bottom = 0;
    gbcCheck.insets.left   = 20;
    gbcCheck.gridy         = 0;
    gbcCheck.gridx++;
    panelCheck.add( this.rbBreakAlways, gbcCheck );

    this.rbBreakInput = GUIFactory.createRadioButton(
			"CTRL-C bricht Programm nur bei Eingaben ab" );
    grpBreak.add( this.rbBreakInput );
    gbcCheck.insets.top = 0;
    gbcCheck.gridy++;
    panelCheck.add( this.rbBreakInput, gbcCheck );

    this.rbBreakNever = GUIFactory.createRadioButton(
				"CTRL-C bricht Programm nicht ab" );
    grpBreak.add( this.rbBreakNever );
    gbcCheck.gridy++;
    panelCheck.add( this.rbBreakNever, gbcCheck );

    this.cbCheckBounds = GUIFactory.createCheckBox(
					"Feldgrenzen pr\u00FCfen",
					true );
    gbcCheck.insets.top = 5;
    gbcCheck.gridy++;
    panelCheck.add( this.cbCheckBounds, gbcCheck );

    this.cbCheckStack = GUIFactory.createCheckBox(
		"Stack pr\u00FCfen (nur bei eigenst\u00E4ndigem Programm"
			+ " mit eigenem Stack-Bereich)",
		true );
    gbcCheck.insets.top = 0;
    gbcCheck.gridy++;
    panelCheck.add( this.cbCheckStack, gbcCheck );

    this.cbPrintLineNumOnAbort = GUIFactory.createCheckBox(
		"Bei Abbruch aufgrund eines Fehlers Zeilennummer ausgeben",
		true );
    gbcCheck.insets.bottom = 5;
    gbcCheck.gridy++;
    panelCheck.add( this.cbPrintLineNumOnAbort, gbcCheck );


    // Bereich Treiber
    JPanel panelDriver = GUIFactory.createPanel( new GridBagLayout() );
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
	GUIFactory.createLabel( "Bei Verwendung der OPEN-Anweisung"
			+ " folgende Treiber einbinden:" ),
	gbcDriver );

    this.cbOpenCrtEnabled = GUIFactory.createCheckBox(
		"CRT-Treiber (Ausgabekanal auf Bildschirm)" );
    gbcDriver.insets.top  = 0;
    gbcDriver.insets.left = 50;
    gbcDriver.gridy++;
    panelDriver.add( this.cbOpenCrtEnabled, gbcDriver );

    this.cbOpenLptEnabled = GUIFactory.createCheckBox(
		"LPT-Treiber (Ausgabekanal auf Drucker, nur relevant"
			+ " wenn vom Zielsystem unterst\u00FCtzt)" );
    gbcDriver.insets.bottom = 0;
    gbcDriver.gridy++;
    panelDriver.add( this.cbOpenLptEnabled, gbcDriver );

    this.cbOpenDiskEnabled = GUIFactory.createCheckBox(
		"DISK-Treiber (Zugriff auf Laufwerke, nur relevant"
			+ " bei Zielsystemen \'"
			+ CPMTarget.DISPLAY_TARGET_NAME
			+ "\' und \'"
			+ KC85Caos48Target.DISPLAY_TARGET_NAME
			+ "\')" );
    gbcDriver.gridy++;
    panelDriver.add( this.cbOpenDiskEnabled, gbcDriver );

    this.cbOpenVdipEnabled = GUIFactory.createCheckBox(
		"VDIP-Treiber (Zugriff auf USB-Speicher, nur relevant"
			+ " wenn vom Zielsystem unterst\u00FCtzt)" );
    gbcDriver.insets.bottom = 5;
    gbcDriver.gridy++;
    panelDriver.add( this.cbOpenVdipEnabled, gbcDriver );


    // Bereich Erzeugter Programmcode
    this.tabbedPane.addTab(
			"Erzeugter Programmcode",
			createCodeDestOptions( false ) );


    // Bereich Warnungen
    JPanel panelWarn = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Warnungen", panelWarn );

    GridBagConstraints gbcWarn = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    panelWarn.add( GUIFactory.createLabel( "Warnen bei:" ), gbcWarn );

    this.cbWarnImplicitDecls = GUIFactory.createCheckBox(
				"Impliziten Variablendeklarationen" );
    gbcWarn.insets.top  = 0;
    gbcWarn.insets.left = 50;
    gbcWarn.gridy++;
    panelWarn.add( this.cbWarnImplicitDecls, gbcWarn );

    this.cbWarnNonAsciiChars = GUIFactory.createCheckBox(
						"Nicht-ASCII-Zeichen" );
    gbcWarn.gridy++;
    panelWarn.add( this.cbWarnNonAsciiChars, gbcWarn );

    this.cbWarnUnusedItems = GUIFactory.createCheckBox(
		"Nicht verwendeten Funktionen, Prozeduren und Variablen" );
    gbcWarn.gridy++;
    panelWarn.add( this.cbWarnUnusedItems, gbcWarn );

    this.cbWarnTooManyDigits = GUIFactory.createCheckBox(
				"Zahlen mit zu vielen Nachkommastellen" );
    gbcWarn.insets.bottom = 5;
    gbcWarn.gridy++;
    panelWarn.add( this.cbWarnTooManyDigits, gbcWarn );


    // Bereich Sonstiges
    JPanel panelEtc = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", panelEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    // Unterbereich BASIC-Eigenschaften
    panelEtc.add( GUIFactory.createLabel( "BASIC-Eigenschaften:" ), gbcEtc );

    this.cbInitVars = GUIFactory.createCheckBox(
				"Variablen automatisch initialisieren" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.cbInitVars, gbcEtc );


    // Unterbereich Optimierung
    gbcEtc.insets.top  = 15;
    gbcEtc.insets.left = 5;
    gbcEtc.gridy++;
    panelEtc.add( GUIFactory.createLabel( "Optimierung:" ), gbcEtc );

    this.cbPreferRelJumps = GUIFactory.createCheckBox(
				"Relative Spr\u00FCnge bevorzugen" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.cbPreferRelJumps, gbcEtc );


    // Unterbereich Assembler-Code
    gbcEtc.insets.top  = 15;
    gbcEtc.insets.left = 5;
    gbcEtc.gridy++;
    panelEtc.add( GUIFactory.createLabel( "Assembler-Code:" ), gbcEtc );

    this.cbShowAsm = GUIFactory.createCheckBox(
				"Erzeugten Assembler-Code anzeigen" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.cbShowAsm, gbcEtc );

    this.cbInclBasicLines = GUIFactory.createCheckBox(
			"BASIC-Zeilen als Kommentare einf\u00FCgen" );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.cbInclBasicLines, gbcEtc );


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
      if( basicOptions.isAppTypeSubroutine() ) {
	this.rbAppTypeSub.setSelected( true );
      } else {
	this.rbAppTypeStandalone.setSelected( true );
      }
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
	  this.rbBreakInput.setSelected( true );
	  break;
	case NEVER:
	  this.rbBreakNever.setSelected( true );
	  break;
	default:
	  this.rbBreakAlways.setSelected( true );
	  break;
      }
      this.cbCheckBounds.setSelected( basicOptions.getCheckBounds() );
      this.cbCheckStack.setSelected( basicOptions.getCheckStack() );
      this.cbOpenCrtEnabled.setSelected( basicOptions.isOpenCrtEnabled() );
      this.cbOpenLptEnabled.setSelected( basicOptions.isOpenLptEnabled() );
      this.cbOpenDiskEnabled.setSelected( basicOptions.isOpenDiskEnabled() );
      this.cbOpenVdipEnabled.setSelected( basicOptions.isOpenVdipEnabled() );
      this.cbPreferRelJumps.setSelected(
				basicOptions.getPreferRelativeJumps() );
      this.cbPrintLineNumOnAbort.setSelected(
				basicOptions.getPrintLineNumOnAbort() );
      this.cbShowAsm.setSelected( basicOptions.getShowAssemblerText() );
      this.cbInclBasicLines.setSelected(
				basicOptions.getIncludeBasicLines() );
      this.cbInitVars.setSelected( basicOptions.getInitVars() );
      this.cbWarnImplicitDecls.setSelected(
				basicOptions.getWarnImplicitDecls() );
      this.cbWarnNonAsciiChars.setSelected(
				basicOptions.getWarnNonAsciiChars() );
      this.cbWarnTooManyDigits.setSelected(
				basicOptions.getWarnTooManyDigits() );
      this.cbWarnUnusedItems.setSelected(
				basicOptions.getWarnUnusedItems() );
    } else {
      this.rbAppTypeStandalone.setSelected( true );
      this.fldAppName.setText( BasicOptions.DEFAULT_APP_NAME );
      this.docHeapSize.setValue( BasicOptions.DEFAULT_HEAP_SIZE );
      this.rbBreakAlways.setSelected( true );
      this.cbCheckBounds.setSelected( true );
      this.cbCheckStack.setSelected( true );
      this.cbOpenCrtEnabled.setSelected( true );
      this.cbOpenLptEnabled.setSelected( true );
      this.cbOpenDiskEnabled.setSelected( true );
      this.cbOpenVdipEnabled.setSelected( true );
      this.cbPreferRelJumps.setSelected( true );
      this.cbPrintLineNumOnAbort.setSelected( true );
      this.cbShowAsm.setSelected( false );
      this.cbInclBasicLines.setSelected( true );
      this.cbInitVars.setSelected( true );
      this.cbWarnImplicitDecls.setSelected( false );
      this.cbWarnNonAsciiChars.setSelected( true );
      this.cbWarnTooManyDigits.setSelected( true );
      this.cbWarnUnusedItems.setSelected( true );
      resetBegAddrs = true;
    }
    if( langCode != null ) {
      if( langCode.equalsIgnoreCase( "DE" ) ) {
	this.rbLangDE.setSelected( true );
      } else {
	this.rbLangEN.setSelected( true );
      }
    } else {
      this.rbLangDE.setSelected( true );
    }
    if( this.rbBreakAlways.isSelected()
	&& this.cbCheckBounds.isSelected()
	&& this.cbCheckStack.isSelected()
	&& this.cbPrintLineNumOnAbort.isSelected() )
    {
      this.rbCheckAll.setSelected( true );
    }
    else if( this.rbBreakNever.isSelected()
	&& !this.cbCheckBounds.isSelected()
	&& !this.cbCheckStack.isSelected()
	&& !this.cbPrintLineNumOnAbort.isSelected() )
    {
      this.rbCheckNone.setSelected( true );
    } else {
      this.rbCheckCustom.setSelected( true );
    }
    if( stackSize > 0 ) {
      this.rbStackSeparate.setSelected( true );
      this.docStackSize.setValue( stackSize );
    } else {
      this.rbStackSystem.setSelected( true );
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
    this.rbAppTypeStandalone.addActionListener( this );
    this.rbAppTypeSub.addActionListener( this );
    this.fldCodeBegAddr.addActionListener( this );
    this.rbBssTrailed.addActionListener( this );
    this.rbBssBegAddr.addActionListener( this );
    this.fldBssBegAddr.addActionListener( this );
    this.fldHeapSize.addActionListener( this );
    this.rbStackSystem.addActionListener( this );
    this.rbStackSeparate.addActionListener( this );
    this.fldStackSize.addActionListener( this );
    this.rbLangDE.addActionListener( this );
    this.rbLangEN.addActionListener( this );
    this.rbCheckAll.addActionListener( this );
    this.rbCheckNone.addActionListener( this );
    this.rbCheckCustom.addActionListener( this );
    this.cbShowAsm.addActionListener( this );


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

      labelText      = "Anfangsadresse Variablen/Speicherzellen:";
      int bssBegAddr = -1;
      if( this.rbBssBegAddr.isSelected() ) {
	bssBegAddr = this.docBssBegAddr.intValue();
      }

      labelText    = textHeapSize;
      int heapSize = this.docHeapSize.intValue();

      labelText     = "Stack-Gr\u00F6\u00DFe:";
      int stackSize = 0;
      if( this.rbStackSeparate.isSelected() ) {
	stackSize = this.docStackSize.intValue();
      }
      labelText = null;

      BasicOptions.BreakOption breakOption = BasicOptions.BreakOption.ALWAYS;
      if( this.rbBreakInput.isSelected() ) {
	breakOption = BasicOptions.BreakOption.INPUT;
      }
      else if( this.rbBreakNever.isSelected() ) {
	breakOption = BasicOptions.BreakOption.NEVER;
      }

      String       appName = this.fldAppName.getText();
      BasicOptions options = new BasicOptions(
				getBasicOptions( this.oldOptions ) );
      options.setTarget( target );
      options.setAppTypeSubroutine( this.rbAppTypeSub.isSelected() );
      options.setAppName( appName != null ? appName.trim() : null );
      options.setCodeBegAddr( codeBegAddr );
      options.setBssBegAddr( bssBegAddr );
      options.setHeapSize( heapSize );
      options.setStackSize( stackSize );
      options.setLangCode( this.rbLangDE.isSelected() ? "DE" : "EN" );
      options.setBreakOption( breakOption );
      options.setCheckBounds( this.cbCheckBounds.isSelected() );
      options.setCheckStack( this.cbCheckStack.isSelected() );
      options.setOpenCrtEnabled( this.cbOpenCrtEnabled.isSelected() );
      options.setOpenLptEnabled( this.cbOpenLptEnabled.isSelected() );
      options.setOpenDiskEnabled( this.cbOpenDiskEnabled.isSelected() );
      options.setOpenVdipEnabled( this.cbOpenVdipEnabled.isSelected() );
      options.setPreferRelativeJumps( this.cbPreferRelJumps.isSelected() );
      options.setPrintLineNumOnAbort(
			this.cbPrintLineNumOnAbort.isSelected() );
      options.setShowAssemblerText( this.cbShowAsm.isSelected() );
      options.setIncludeBasicLines( this.cbInclBasicLines.isSelected() );
      options.setInitVars( this.cbInitVars.isSelected() );
      options.setWarnImplicitDecls( this.cbWarnImplicitDecls.isSelected() );
      options.setWarnNonAsciiChars( this.cbWarnNonAsciiChars.isSelected() );
      options.setWarnTooManyDigits( this.cbWarnTooManyDigits.isSelected() );
      options.setWarnUnusedItems( this.cbWarnUnusedItems.isSelected() );
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
      else if( (src == this.rbAppTypeStandalone)
	       || (src == this.rbAppTypeSub) )
      {
	rv = true;
	updAppNameFieldsEnabled();
      }
      else if( (src == this.rbBssTrailed)
	       || (src == this.rbBssBegAddr) )
      {
	rv = true;
	updBssFieldsEnabled();
      }
      else if( (src == this.rbStackSystem)
	       || (src == this.rbStackSeparate) )
      {
	rv = true;
	updStackFieldsEnabled();
      }
      else if( src == this.rbCheckAll ) {
	rv = true;
	this.rbBreakAlways.setSelected( true );
	this.cbCheckBounds.setSelected( true );
	this.cbCheckStack.setSelected( true );
	this.cbPrintLineNumOnAbort.setSelected( true );
	updCheckFieldsEnabled();
      }
      else if( src == this.rbCheckNone ) {
	rv = true;
	this.rbBreakNever.setSelected( true );
	this.cbCheckBounds.setSelected( false );
	this.cbCheckStack.setSelected( false );
	this.cbPrintLineNumOnAbort.setSelected( false );
	updCheckFieldsEnabled();
      }
      else if( src == this.rbCheckCustom ) {
	rv = true;
	updCheckFieldsEnabled();
      }
      else if( src == this.cbShowAsm ) {
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
	if( state ) {
	  int   targetIdx = 0;
	  this.emuSys     = curEmuSys;
	  if( emuSys != null ) {
	    int n = this.comboTarget.getItemCount();
	    for( int i = 0; i < n; i++ ) {
	      Object o = this.comboTarget.getItemAt( i );
	      if( o != null ) {
		if( o instanceof AbstractTarget ) {
		  if( ((AbstractTarget) o).getCompatibilityLevel(
							emuSys ) > 0 )
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
  }


	/* --- private Methoden --- */

  private static BasicOptions getBasicOptions( PrgOptions options )
  {
    BasicOptions basicOptions = null;
    if( options != null ) {
      if( options instanceof BasicOptions ) {
	basicOptions = (BasicOptions) options;
      }
    }
    return basicOptions;
  }


  private void updAppNameFieldsEnabled()
  {
    boolean state = false;
    if( !this.rbAppTypeSub.isSelected() ) {
      Object  obj = this.comboTarget.getSelectedItem();
      if( obj != null ) {
	if( obj instanceof AbstractTarget ) {
	  if( ((AbstractTarget) obj).getMaxAppNameLen() > 0 ) {
	    state = true;
	  }
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
      this.rbBssBegAddr.setSelected( true );
    } else {
      this.fldBssBegAddr.setText( "" );
      this.rbBssTrailed.setSelected( true );
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
    boolean state = this.rbBssBegAddr.isSelected();
    this.fldBssBegAddr.setEnabled( state );
    this.labelBssBegAddrUnit.setEnabled( state );
  }


  private void updCheckFieldsEnabled()
  {
    boolean state = this.rbCheckCustom.isSelected();
    this.rbBreakAlways.setEnabled( state );
    this.rbBreakInput.setEnabled( state );
    this.rbBreakNever.setEnabled( state );
    this.cbCheckBounds.setEnabled( state );
    this.cbCheckStack.setEnabled( state );
    this.cbPrintLineNumOnAbort.setEnabled( state );
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
    this.cbInclBasicLines.setEnabled( this.cbShowAsm.isSelected() );
  }


  private void updStackFieldsEnabled()
  {
    boolean state = this.rbStackSeparate.isSelected();
    this.fldStackSize.setEnabled( state );
    this.labelStackUnit.setEnabled( state );
  }
}
