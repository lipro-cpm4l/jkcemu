/*
 * (c) 2008 Jens Mueller
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
import jkcemu.programming.*;


public class BasicOptionsDlg extends AbstractOptionsDlg
{
  private static final int MIN_STACK_SIZE = 64;

  private static final String textBegAddr   = "Anfangsadresse:";
  private static final String textEndOfMem  = "Maximale Endadresse:";
  private static final String textArraySize =
				"Gr\u00F6\u00DFe des @-Variablen-Arrays:";

  private JTabbedPane     tabbedPane;
  private JRadioButton    btnStackSystem;
  private JRadioButton    btnStackSeparate;
  private JRadioButton    btnCheckAll;
  private JRadioButton    btnCheckNone;
  private JRadioButton    btnCheckCustom;
  private JRadioButton    btnBreakAnywhere;
  private JRadioButton    btnBreakInput;
  private JRadioButton    btnBreakNever;
  private JCheckBox       btnCheckStack;
  private JCheckBox       btnCheckArray;
  private JCheckBox       btnStrictAC1Basic;
  private JCheckBox       btnStrictZ1013Basic;
  private JCheckBox       btnPrintCalls;
  private JCheckBox       btnAllowLongVarNames;
  private JCheckBox       btnFormatSource;
  private JCheckBox       btnShowAsm;
  private JCheckBox       btnStructuredForNext;
  private JCheckBox       btnPreferRelJumps;
  private JTextField      fldAppName;
  private JTextField      fldArraySize;
  private JTextField      fldEndOfMem;
  private JTextField      fldStackSize;
  private JLabel          labelStackSize;
  private JLabel          labelStackUnit;
  private LimitedDocument docAppName;
  private HexDocument     docBegAddr;
  private IntegerDocument docArraySize;
  private HexDocument     docEndOfMem;
  private IntegerDocument docStackSize;


  public static PrgOptions showOptionsDlg(
					Frame      owner,
					EmuThread  emuThread,
					PrgOptions options )
  {
    BasicOptionsDlg dlg = new BasicOptionsDlg( owner, emuThread, options );
    dlg.setVisible( true );
    return dlg.getAppliedOptions();
  }


	/* --- ueberschriebene Methoden --- */

  protected boolean doAction( EventObject e )
  {
    boolean rv = super.doAction( e );
    if( !rv && (e != null) ) {
      Object src = e.getSource();
      if( (src == this.btnStackSystem)
	  || (src == this.btnStackSeparate) )
      {
	rv = true;
	updStackFieldsEnabled();
      }
      else if( src == this.btnCheckAll ) {
	rv = true;
	this.btnBreakInput.setSelected( true );
	this.btnCheckArray.setSelected( true );
	this.btnCheckStack.setSelected( true );
	updCheckFieldsEnabled();
      }
      else if( src == this.btnCheckNone ) {
	rv = true;
	this.btnBreakNever.setSelected( true );
	this.btnCheckArray.setSelected( false );
	this.btnCheckStack.setSelected( false );
	updCheckFieldsEnabled();
      }
      else if( src == this.btnCheckCustom ) {
	rv = true;
	updCheckFieldsEnabled();
      } else {
	rv = true;
	if( src instanceof JTextField )
	  ((JTextField) src).transferFocus();
      }
    }
    return rv;
  }


	/* --- private Konstruktoren und Methoden --- */

  private BasicOptionsDlg(
		Frame      owner,
		EmuThread  emuThread,
		PrgOptions options )
  {
    super( owner, emuThread, "BASIC-Compiler-Optionen" );


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

    this.tabbedPane = new JTabbedPane(
				JTabbedPane.TOP,
				JTabbedPane.SCROLL_TAB_LAYOUT );
    add( this.tabbedPane, gbc );


    // Bereich Allgemein
    JPanel panelGeneral = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Allgemein", panelGeneral );

    GridBagConstraints gbcGeneral = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    panelGeneral.add(
	new JLabel(
		"Programmname (nur relevant bei KC85/1, KC87 und Z9001):" ),
	gbcGeneral );

    this.docAppName = new LimitedDocument( 8 );
    this.fldAppName = new JTextField( this.docAppName, "", 8 );
    gbcGeneral.gridx++;
    panelGeneral.add( this.fldAppName, gbcGeneral );


    // Bereich Speicherbereiche
    JPanel panelMem = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Speicherbereich", panelMem );

    GridBagConstraints gbcMem = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 2, 5 ),
					0, 0 );

    panelMem.add( new JLabel( textBegAddr ), gbcMem );

    JTextField fld = new JTextField( 5 );
    fld.addActionListener( this );
    this.docBegAddr = new HexDocument( fld, 4, textBegAddr );
    gbcMem.gridx++;
    panelMem.add( fld, gbcMem );
    gbcMem.gridx++;
    panelMem.add( new JLabel( "hex" ), gbcMem );

    gbcMem.insets.top = 2;
    gbcMem.gridx      = 0;
    gbcMem.gridy++;
    panelMem.add( new JLabel( textEndOfMem), gbcMem );

    fld = new JTextField( 5 );
    fld.addActionListener( this );
    this.docEndOfMem = new HexDocument( fld, 4, textEndOfMem );
    gbcMem.gridx++;
    panelMem.add( fld, gbcMem );
    gbcMem.gridx++;
    panelMem.add( new JLabel( "hex" ), gbcMem );

    gbcMem.gridx = 0;
    gbcMem.gridy++;
    panelMem.add( new JLabel( textArraySize ), gbcMem );

    fld = new JTextField( 5 );
    fld.addActionListener( this );
    this.docArraySize    = new IntegerDocument( fld, new Integer( 0 ), null );
    gbcMem.insets.bottom = 5;
    gbcMem.gridx++;
    panelMem.add( fld, gbcMem );
    gbcMem.gridx++;
    panelMem.add( new JLabel( "Variablen" ), gbcMem );

    ButtonGroup grpStack = new ButtonGroup();

    this.btnStackSystem = new JRadioButton( "System-Stack verwenden", true );
    this.btnStackSystem.addActionListener( this );
    grpStack.add( this.btnStackSystem );
    gbcMem.gridwidth     = GridBagConstraints.REMAINDER;
    gbcMem.insets.top    = 5;
    gbcMem.insets.bottom = 0;
    gbcMem.insets.left   = 20;
    gbcMem.gridy         = 0;
    gbcMem.gridx += 2;
    panelMem.add( this.btnStackSystem, gbcMem );

    this.btnStackSeparate = new JRadioButton(
					"Eigener Stack-Bereich:",
					false );
    this.btnStackSeparate.addActionListener( this );
    grpStack.add( this.btnStackSeparate );
    gbcMem.insets.top = 0;
    gbcMem.gridy++;
    panelMem.add( this.btnStackSeparate, gbcMem );

    this.labelStackSize = new JLabel( "Gr\u00F6\u00DFe:" );
    gbcMem.gridwidth   = 1;
    gbcMem.insets.left = 50;
    gbcMem.gridy++;
    panelMem.add( this.labelStackSize, gbcMem );
    
    this.fldStackSize = new JTextField( 5 );
    this.fldStackSize.addActionListener( this );
    this.docStackSize = new IntegerDocument(
					this.fldStackSize,
					new Integer( MIN_STACK_SIZE ),
					null );
    gbcMem.insets.left = 5;
    gbcMem.gridx++;
    panelMem.add( this.fldStackSize, gbcMem );
    this.labelStackUnit = new JLabel( "Bytes" );
    gbcMem.gridx++;
    panelMem.add( this.labelStackUnit, gbcMem );


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

    this.btnCheckAll = new JRadioButton( "Max. Sicherheit", true );
    this.btnCheckAll.addActionListener( this );
    grpCheck.add( this.btnCheckAll );
    panelCheck.add( this.btnCheckAll, gbcCheck );

    this.btnCheckNone = new JRadioButton( "Max. Geschwindigkeit", false );
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

    this.btnBreakAnywhere = new JRadioButton(
		"CTRL-C bricht Programm ab",
		false );
    grpBreak.add( this.btnBreakAnywhere );
    gbcCheck.insets.top    = 5;
    gbcCheck.insets.bottom = 0;
    gbcCheck.insets.left   = 20;
    gbcCheck.gridy         = 0;
    gbcCheck.gridx++;
    panelCheck.add( this.btnBreakAnywhere, gbcCheck );

    this.btnBreakInput = new JRadioButton(
		"CTRL-C bricht Programm nur bei Eingaben ab",
		true );
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

    this.btnCheckArray = new JCheckBox(
		"Grenzen des @-Variablen-Arrays pr\u00FCfen",
		true );
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckArray, gbcCheck );

    this.btnCheckStack = new JCheckBox(
		"Stack bez\u00FCglich GOSUB/RETURN und FOR/NEXT pr\u00FCfen",
		true );
    gbcCheck.insets.bottom = 5;
    gbcCheck.gridy++;
    panelCheck.add( this.btnCheckStack, gbcCheck );


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

    this.btnPrintCalls = new JCheckBox(
				"Auf CALL-Anweisungen hinweisen",
				true );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnPrintCalls, gbcEtc );


    // Unterbereich Syntax
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.left   = 5;
    gbcEtc.insets.bottom = 0;
    gbcEtc.gridy++;
    panelEtc.add( new JLabel( "Syntax:" ), gbcEtc );

    this.btnAllowLongVarNames = new JCheckBox(
		"Variablennamen mit mehr als einem Zeichen L\u00E4nge"
			+ " erlauben" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnAllowLongVarNames, gbcEtc );

    this.btnStrictAC1Basic = new JCheckBox(
	"Abweichungen von der originalen AC1-Mini-BASIC-Syntax melden" );
    gbcEtc.gridy++;
    panelEtc.add( this.btnStrictAC1Basic, gbcEtc );

    this.btnStrictZ1013Basic = new JCheckBox(
	"Abweichungen von der originalen Z1013-Tiny-BASIC-Syntax melden" );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.btnStrictZ1013Basic, gbcEtc );


    // Unterbereich Quelltext
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.left   = 5;
    gbcEtc.insets.bottom = 0;
    gbcEtc.gridy++;
    panelEtc.add( new JLabel( "Quelltext:" ), gbcEtc );

    this.btnFormatSource = new JCheckBox( "BASIC-Quelltext formatieren" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnFormatSource, gbcEtc );

    this.btnShowAsm = new JCheckBox(
			"Erzeugten Assembler-Quelltext anzeigen" );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.btnShowAsm, gbcEtc );


    // Unterbereich Optimierung
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.left   = 5;
    gbcEtc.insets.bottom = 0;
    gbcEtc.gridy++;
    panelEtc.add( new JLabel( "Optimierung:" ), gbcEtc );

    this.btnStructuredForNext = new JCheckBox(
		"FOR/NEXT als strukturierte Schleife \u00FCbersetzten"
			+ " - siehe Hilfe!!!" );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.btnStructuredForNext, gbcEtc );

    this.btnPreferRelJumps = new JCheckBox(
				"Relative Spr\u00FCnge bevorzugen" );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.btnPreferRelJumps, gbcEtc );


    // Bereich Knoepfe
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( createButtons( "Compilieren" ), gbc );


    // Vorbelegungen
    BasicOptions basicOptions = null;
    if( options != null ) {
      if( options instanceof BasicOptions )
	basicOptions = (BasicOptions) options;
    }
    int stackSize = BasicOptions.DEFAULT_STACK_SIZE;
    if( basicOptions != null ) {
      stackSize = basicOptions.getStackSize();
      this.fldAppName.setText( basicOptions.getAppName() );
      this.docBegAddr.setValue( basicOptions.getBegAddr(), 4 );
      this.docArraySize.setValue( basicOptions.getArraySize() );
      this.docEndOfMem.setValue( basicOptions.getEndOfMemory(), 4 );
      switch( basicOptions.getBreakPossibility() ) {
	case BREAK_INPUT:
	  this.btnBreakInput.setSelected( true );
	  break;
	case BREAK_NEVER:
	  this.btnBreakNever.setSelected( true );
	  break;
	default:
	  this.btnBreakAnywhere.setSelected( true );
	  break;
      }
      this.btnCheckArray.setSelected( basicOptions.getCheckArray() );
      this.btnCheckStack.setSelected( basicOptions.getCheckStack() );
      this.btnPrintCalls.setSelected( basicOptions.getPrintCalls() );
      this.btnAllowLongVarNames.setSelected(
				basicOptions.getAllowLongVarNames() );
      this.btnStrictAC1Basic.setSelected(
				basicOptions.getStrictAC1MiniBASIC() );
      this.btnStrictZ1013Basic.setSelected(
				basicOptions.getStrictZ1013TinyBASIC() );
      this.btnFormatSource.setSelected( basicOptions.getFormatSource() );
      this.btnShowAsm.setSelected( basicOptions.getShowAsm() );
      this.btnStructuredForNext.setSelected(
				basicOptions.getStructuredForNext() );
      this.btnPreferRelJumps.setSelected(
				basicOptions.getPreferRelativeJumps() );
    } else {
      this.fldAppName.setText( "MYAPP" );
      this.docBegAddr.setValue(
		BasicOptions.getDefaultBegAddr(
			emuThread.getEmuSys().getSystemName() ), 4 );
      this.docArraySize.setValue( BasicOptions.DEFAULT_ARRAY_SIZE );
      this.docEndOfMem.setValue( BasicOptions.DEFAULT_END_OF_MEM, 4 );
      this.btnBreakInput.setSelected( true );
      this.btnCheckArray.setSelected( true );
      this.btnCheckStack.setSelected( true );
      this.btnPrintCalls.setSelected( true );
      this.btnAllowLongVarNames.setSelected( false );
      this.btnStrictAC1Basic.setSelected( false );
      this.btnStrictZ1013Basic.setSelected( false );
      this.btnFormatSource.setSelected( false );
      this.btnShowAsm.setSelected( false );
      this.btnStructuredForNext.setSelected( false );
      this.btnPreferRelJumps.setSelected( true );
    }
    if( this.btnBreakInput.isSelected()
	&& this.btnCheckArray.isSelected()
	&& this.btnCheckStack.isSelected() )
    {
      this.btnCheckAll.setSelected( true );
    }
    else if( this.btnBreakNever.isSelected()
	&& !this.btnCheckArray.isSelected()
	&& !this.btnCheckStack.isSelected() )
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
    updCodeDestFields( options );
    updCheckFieldsEnabled();
    updStackFieldsEnabled();


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


  protected void doApply()
  {
    String labelText = null;
    try {
      labelText   = textBegAddr;
      int begAddr = this.docBegAddr.intValue();

      labelText     = textArraySize;
      int arraySize = this.docArraySize.intValue();

      labelText    = textEndOfMem;
      int endOfMem = this.docEndOfMem.intValue();

      labelText     = "Stack-Gr\u00F6\u00DFe";
      int stackSize = 0;
      if( this.btnStackSeparate.isSelected() ) {
	stackSize = this.docStackSize.intValue();
      }
      labelText = null;

      BasicOptions.BreakPossibility breakPossibility =
			BasicOptions.BreakPossibility.BREAK_ALWAYS;
      if( this.btnBreakInput.isSelected() ) {
	breakPossibility = BasicOptions.BreakPossibility.BREAK_INPUT;
      }
      else if( this.btnBreakNever.isSelected() ) {
	breakPossibility = BasicOptions.BreakPossibility.BREAK_NEVER;
      }

      BasicOptions options = new BasicOptions( this.emuThread );
      options.setAppName( this.fldAppName.getText() );
      options.setBegAddr( begAddr );
      options.setArraySize( arraySize );
      options.setEndOfMemory( endOfMem );
      options.setStackSize( stackSize );
      options.setBreakPossibility( breakPossibility );
      options.setCheckArray( this.btnCheckArray.isSelected() );
      options.setCheckStack( this.btnCheckStack.isSelected() );
      options.setPrintCalls( this.btnPrintCalls.isSelected() );
      options.setAllowLongVarNames( this.btnAllowLongVarNames.isSelected() );
      options.setStrictAC1MiniBASIC( this.btnStrictAC1Basic.isSelected() );
      options.setStrictZ1013TinyBASIC( this.btnStrictZ1013Basic.isSelected() );
      options.setFormatSource( this.btnFormatSource.isSelected() );
      options.setShowAsm( this.btnShowAsm.isSelected() );
      options.setStructuredForNext( this.btnStructuredForNext.isSelected() );
      options.setPreferRelativeJumps( this.btnPreferRelJumps.isSelected() );
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
  }


  private void updCheckFieldsEnabled()
  {
    boolean state = this.btnCheckCustom.isSelected();
    this.btnBreakAnywhere.setEnabled( state );
    this.btnBreakInput.setEnabled( state );
    this.btnBreakNever.setEnabled( state );
    this.btnCheckArray.setEnabled( state );
    this.btnCheckStack.setEnabled( state );
  }


  private void updStackFieldsEnabled()
  {
    boolean state = this.btnStackSeparate.isSelected();
    this.fldStackSize.setEnabled( state );
    this.labelStackSize.setEnabled( state );
    this.labelStackUnit.setEnabled( state );
  }
}

