/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer BASIC-Compiler-Optionen
 */

package jkcemu.programming.basic;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.emusys.*;
import jkcemu.programming.*;
import jkcemu.programming.basic.target.*;


public class BasicOptionsDlg extends AbstractOptionsDlg
{
  private static final String textCodeBegAddr =
			"Anfangsadresse Programmcode:";
  private static final String textBssBegAddr =
			"Anfangsadresse Speicherzelle:";
  private static final String textHeapSize =
			"Gr\u00F6\u00DFe des Zeichenkettenspeichers:";

  private static final AbstractTarget[] targets = {
				new CPMTarget(),
				new SCCHTarget(),
				new AC1Target(),
				new LLC2HIRESTarget(),
				new HueblerGraphicsMCTarget(),
				new Z9001Target(),
				new Z9001KRTTarget(),
				new KC85Target(),
				new KramerMCTarget(),
				new Z1013Target(),
				new Z1013PetersTarget() };

  private EmuSys          emuSys;
  private JTabbedPane     tabbedPane;
  private JRadioButton    btnStackSystem;
  private JRadioButton    btnStackSeparate;
  private JRadioButton    btnLangDE;
  private JRadioButton    btnLangEN;
  private JRadioButton    btnCheckAll;
  private JRadioButton    btnCheckNone;
  private JRadioButton    btnCheckCustom;
  private JRadioButton    btnBreakAlways;
  private JRadioButton    btnBreakInput;
  private JRadioButton    btnBreakNever;
  private JComboBox       comboTarget;
  private JCheckBox       btnCheckBounds;
  private JCheckBox       btnCheckStack;
  private JCheckBox       btnPreferRelJumps;
  private JCheckBox       btnPrintLineNumOnAbort;
  private JCheckBox       btnShowAsm;
  private JCheckBox       btnWarnNonAsciiChars;
  private JCheckBox       btnWarnUnusedItems;
  private JTextField      fldAppName;
  private JTextField      fldCodeBegAddr;
  private JTextField      fldBssBegAddr;
  private JTextField      fldHeapSize;
  private JTextField      fldStackSize;
  private JLabel          labelAppName;
  private JLabel          labelCodeBegAddr;
  private JLabel          labelCodeBegAddrUnit;
  private JLabel          labelBssBegAddr;
  private JLabel          labelBssBegAddrUnit;
  private JLabel          labelStackSize;
  private JLabel          labelStackUnit;
  private LimitedDocument docAppName;
  private HexDocument     docCodeBegAddr;
  private HexDocument     docBssBegAddr;
  private IntegerDocument docHeapSize;
  private IntegerDocument docStackSize;


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
    this.labelCodeBegAddr = new JLabel( textCodeBegAddr );
    panelGeneral.add( this.labelCodeBegAddr, gbcGeneral );
    gbcGeneral.gridy++;
    panelGeneral.add(
		new JLabel( "Gr\u00F6\u00DFe Zeichenkettenspeicher:" ),
		gbcGeneral );
    gbcGeneral.gridy++;
    panelGeneral.add( new JLabel( "Stack:" ), gbcGeneral );

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
      for( int i = 0; i < targets.length; i++ ) {
	if( targets[ i ].createsCodeFor( emuSys ) ) {
	  presetTargetIdx = i;
	  forceCodeToEmu  = true;
	  break;
	}
      }
    }
    if( presetTargetIdx >= 0 ) {
      this.comboTarget = new JComboBox( targets );
      this.comboTarget.setEditable( false );
    } else {
      this.comboTarget = new JComboBox();
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
    this.comboTarget.addActionListener( this );
    gbcGeneral.anchor      = GridBagConstraints.WEST;
    gbcGeneral.insets.left = 0;
    gbcGeneral.gridwidth   = GridBagConstraints.REMAINDER;
    gbcGeneral.gridy       = 0;
    gbcGeneral.gridx++;
    panelGeneral.add( this.comboTarget, gbcGeneral );

    this.docAppName      = new LimitedDocument( 8 );
    this.fldAppName      = new JTextField( this.docAppName, "", 0 );
    gbcGeneral.fill      = GridBagConstraints.HORIZONTAL;
    gbcGeneral.gridwidth = 2;
    gbcGeneral.gridy++;
    panelGeneral.add( this.fldAppName, gbcGeneral );

    this.docCodeBegAddr = new HexDocument( 4, textCodeBegAddr );
    this.fldCodeBegAddr = new JTextField( this.docCodeBegAddr, "", 0 );
    this.fldCodeBegAddr.addActionListener( this );
    gbcGeneral.gridwidth = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.fldCodeBegAddr, gbcGeneral );

    this.labelCodeBegAddrUnit = new JLabel( "hex" );
    gbcGeneral.fill       = GridBagConstraints.NONE;
    gbcGeneral.gridx++;
    panelGeneral.add( this.labelCodeBegAddrUnit, gbcGeneral );

    JTextField fldHeapSize = new JTextField();
    fldHeapSize.addActionListener( this );
    this.docHeapSize = new IntegerDocument(
				fldHeapSize,
				new Integer( BasicOptions.MIN_HEAP_SIZE ),
				new Integer( BasicOptions.MAX_HEAP_SIZE ) );
    gbcGeneral.fill  = GridBagConstraints.HORIZONTAL;
    gbcGeneral.gridx = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( fldHeapSize, gbcGeneral );
    gbcGeneral.fill         = GridBagConstraints.NONE;
    gbcGeneral.insets.right = 50;
    gbcGeneral.gridx++;
    panelGeneral.add( new JLabel( "Bytes" ), gbcGeneral );

    ButtonGroup grpStack = new ButtonGroup();

    this.btnStackSystem = new JRadioButton(
					"System-Stack verwenden",
					true );
    this.btnStackSystem.addActionListener( this );
    grpStack.add( this.btnStackSystem );
    gbcGeneral.insets.right = 5;
    gbcGeneral.gridwidth    = GridBagConstraints.REMAINDER;
    gbcGeneral.gridx        = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.btnStackSystem, gbcGeneral );

    this.btnStackSeparate = new JRadioButton(
					"Eigener Stack-Bereich:",
					false );
    this.btnStackSeparate.addActionListener( this );
    grpStack.add( this.btnStackSeparate );
    gbcGeneral.insets.top = 0;
    gbcGeneral.gridy++;
    panelGeneral.add( this.btnStackSeparate, gbcGeneral );

    this.labelStackSize    = new JLabel( "Gr\u00F6\u00DFe:" );
    gbcGeneral.anchor      = GridBagConstraints.EAST;
    gbcGeneral.insets.left = 50;
    gbcGeneral.gridwidth   = 1;
    gbcGeneral.gridy++;
    panelGeneral.add( this.labelStackSize, gbcGeneral );

    this.fldStackSize = new JTextField();
    this.fldStackSize.addActionListener( this );
    this.docStackSize = new IntegerDocument(
				this.fldStackSize,
				new Integer( BasicOptions.MIN_STACK_SIZE ),
				null );
    gbcGeneral.anchor      = GridBagConstraints.WEST;
    gbcGeneral.fill        = GridBagConstraints.HORIZONTAL;
    gbcGeneral.insets.left = 0;
    gbcGeneral.gridwidth   = 2;
    gbcGeneral.gridx++;
    panelGeneral.add( this.fldStackSize, gbcGeneral );

    this.labelStackUnit = new JLabel( "Bytes" );
    gbcGeneral.gridx++;
    panelGeneral.add( this.labelStackUnit, gbcGeneral );

    gbcGeneral.anchor        = GridBagConstraints.EAST;
    gbcGeneral.fill          = GridBagConstraints.NONE;
    gbcGeneral.insets.top    = 5;
    gbcGeneral.insets.left   = 5;
    gbcGeneral.insets.bottom = 5;
    gbcGeneral.gridwidth     = 1;
    gbcGeneral.gridx         = 0;
    gbcGeneral.gridy++;
    panelGeneral.add(
		new JLabel( "Sprache der Laufzeitausschriften:" ),
		gbcGeneral );

    JPanel panelLang       = new JPanel( new GridBagLayout() );
    gbcGeneral.anchor      = GridBagConstraints.WEST;
    gbcGeneral.insets.left = 0;
    gbcGeneral.gridwidth   = GridBagConstraints.REMAINDER;
    gbcGeneral.gridx++;
    panelGeneral.add( panelLang, gbcGeneral );

    GridBagConstraints gbcLang = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 0, 0, 0, 0 ),
					0, 0 );

    ButtonGroup grpLang = new ButtonGroup();

    this.btnLangDE = new JRadioButton( "Deutsch" );
    grpLang.add( this.btnLangDE );
    this.btnLangDE.addActionListener( this );
    panelLang.add( this.btnLangDE, gbcLang );

    this.btnLangEN = new JRadioButton( "Englisch" );
    grpLang.add( this.btnLangEN );
    this.btnLangEN.addActionListener( this );
    gbcLang.insets.left = 5;
    gbcLang.gridx++;
    panelLang.add( this.btnLangEN, gbcLang );


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
    this.btnCheckAll.addActionListener( this );
    grpCheck.add( this.btnCheckAll );
    panelCheck.add( this.btnCheckAll, gbcCheck );

    this.btnCheckNone = new JRadioButton(
				"Compilieren f\u00FCr Produktiveinsatz",
				false );
    this.btnCheckNone.addActionListener( this );
    grpCheck.add( this.btnCheckNone );
    gbcCheck.insets.top = 0;
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckNone, gbcCheck );

    this.btnCheckCustom = new JRadioButton( "Benutzerdefiniert", false );
    this.btnCheckCustom.addActionListener( this );
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
    gbcEtc.insets.top    = 0;
    gbcEtc.insets.left   = 50;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.btnShowAsm, gbcEtc );


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
      this.btnPreferRelJumps.setSelected(
				basicOptions.getPreferRelativeJumps() );
      this.btnPrintLineNumOnAbort.setSelected(
				basicOptions.getPrintLineNumOnAbort() );
      this.btnShowAsm.setSelected( basicOptions.getShowAssemblerText() );
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
      this.btnPreferRelJumps.setSelected( true );
      this.btnPrintLineNumOnAbort.setSelected( true );
      this.btnShowAsm.setSelected( false );
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
    updCodeDestFields( options, forceCodeToEmu );
    updAppNameFieldsEnabled();
    updCheckFieldsEnabled();
    updStackFieldsEnabled();


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

      labelText       = textHeapSize;
      int heapSize    = this.docHeapSize.intValue();

      labelText       = "Stack-Gr\u00F6\u00DFe:";
      int stackSize   = 0;
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

      BasicOptions options = new BasicOptions();
      options.setTarget( target );
      options.setAppName( this.fldAppName.getText() );
      options.setCodeBegAddr( codeBegAddr );
      options.setHeapSize( heapSize );
      options.setStackSize( stackSize );
      options.setLangCode( this.btnLangDE.isSelected() ? "DE" : "EN" );
      options.setBreakOption( breakOption );
      options.setCheckBounds( this.btnCheckBounds.isSelected() );
      options.setCheckStack( this.btnCheckStack.isSelected() );
      options.setPreferRelativeJumps( this.btnPreferRelJumps.isSelected() );
      options.setPrintLineNumOnAbort(
			this.btnPrintLineNumOnAbort.isSelected() );
      options.setShowAssemblerText( this.btnShowAsm.isSelected() );
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
		if( ((AbstractTarget) o).createsCodeFor( emuSys ) ) {
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
	state = ((AbstractTarget) obj).supportsAppName();
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
	  codeToEmu = ((AbstractTarget) o).createsCodeFor( this.emuSys );
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


  private void updStackFieldsEnabled()
  {
    boolean state = this.btnStackSeparate.isSelected();
    this.fldStackSize.setEnabled( state );
    this.labelStackSize.setEnabled( state );
    this.labelStackUnit.setEnabled( state );
  }
}

