/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Assembler-Optionen
 */

package jkcemu.programming.assembler;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import jkcemu.base.EmuThread;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;
import jkcemu.programming.AbstractOptionsDlg;
import jkcemu.programming.PrgOptions;


public class AsmOptionsDlg extends AbstractOptionsDlg
{
  private JRadioButton rbSyntaxZilog;
  private JRadioButton rbSyntaxRobotron;
  private JRadioButton rbSyntaxBoth;
  private JCheckBox    cbAllowUndocInst;
  private JCheckBox    cbAsmListing;
  private JCheckBox    cbLabelsCaseSensitive;
  private JCheckBox    cbPrintLabels;
  private JCheckBox    cbLabelsToReass;
  private JCheckBox    cbLabelsToDebugger;
  private JCheckBox    cbFormatSource;
  private JCheckBox    cbReplaceTooLongRelJumps;
  private JCheckBox    cbWarnNonAsciiChars;
  private JRadioButton rbLabelsCreateOrUpdateBPs;
  private JRadioButton rbLabelsUpdateBPsOnly;


  public AsmOptionsDlg(
		Frame      owner,
		EmuThread  emuThread,
		PrgOptions options )
  {
    super( owner, emuThread, options, "Assembler-Optionen" );

    // Fensterinhalt
    setLayout( new GridBagLayout() );
    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Bereich Mnemonik/Syntax
    JPanel panelSyntax = GUIFactory.createPanel( new GridBagLayout() );
    panelSyntax.setBorder( GUIFactory.createTitledBorder(
						"Mnemonik/Syntax" ) );
    add( panelSyntax, gbc );

    GridBagConstraints gbcSyntax = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpSyntax = new ButtonGroup();

    this.rbSyntaxBoth = GUIFactory.createRadioButton(
				"Zilog- und Robotron-Mnemonik/-Syntax" );
    grpSyntax.add( this.rbSyntaxBoth );
    panelSyntax.add( this.rbSyntaxBoth, gbcSyntax );

    this.rbSyntaxZilog = GUIFactory.createRadioButton(
				"Nur Zilog-Mnemonik/-Syntax erlauben" );
    grpSyntax.add( this.rbSyntaxZilog );
    gbcSyntax.insets.top = 0;
    gbcSyntax.gridy++;
    panelSyntax.add( this.rbSyntaxZilog, gbcSyntax );

    this.rbSyntaxRobotron = GUIFactory.createRadioButton(
				"Nur Robotron-Mnemonik/-Syntax erlauben" );
    grpSyntax.add( this.rbSyntaxRobotron );
    gbcSyntax.insets.bottom = 5;
    gbcSyntax.gridy++;
    panelSyntax.add( this.rbSyntaxRobotron, gbcSyntax );

    this.cbAllowUndocInst = GUIFactory.createCheckBox(
				"Undokumentierte Befehle erlauben" );
    gbcSyntax.insets.top = 0;
    gbcSyntax.gridy++;
    panelSyntax.add( this.cbAllowUndocInst, gbcSyntax );


    // Bereich Marken
    JPanel panelLabel = GUIFactory.createPanel(
				new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    panelLabel.setBorder( GUIFactory.createTitledBorder( "Marken" ) );
    gbc.gridy++;
    add( panelLabel, gbc );

    this.cbLabelsCaseSensitive = GUIFactory.createCheckBox(
			"Gro\u00DF-/Kleinschreibung bei Marken beachten" );
    panelLabel.add( this.cbLabelsCaseSensitive );

    this.cbPrintLabels = GUIFactory.createCheckBox(
					"Markentabelle ausgeben" );
    panelLabel.add( this.cbPrintLabels );


    // Bereich Erzeugter Programmcode
    JPanel panelCodeDest = createCodeDestOptions( true );
    panelCodeDest.setBorder( GUIFactory.createTitledBorder(
						"Erzeugter Programmcode" ) );
    gbc.gridy++;
    add( panelCodeDest, gbc );


    // Bereich Sonstiges
    JPanel panelEtc = GUIFactory.createPanel( new GridBagLayout() );
    panelEtc.setBorder( GUIFactory.createTitledBorder( "Sonstiges" ) );
    gbc.gridy++;
    add( panelEtc, gbc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );


    this.cbWarnNonAsciiChars = GUIFactory.createCheckBox(
					"Bei Nicht-ASCII-Zeichen warnen" );
    panelEtc.add( this.cbWarnNonAsciiChars, gbcEtc );

    this.cbReplaceTooLongRelJumps = GUIFactory.createCheckBox(
		"Zu gro\u00DFe relative Spr\u00FCnge als absolute"
			+ " \u00FCbersetzen (nicht bei DJNZ)" );
    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    panelEtc.add( this.cbReplaceTooLongRelJumps, gbcEtc );

    this.cbFormatSource = GUIFactory.createCheckBox(
					"Quelltext formatieren" );
    gbcEtc.gridy++;
    panelEtc.add( this.cbFormatSource, gbcEtc );

    this.cbLabelsToDebugger = GUIFactory.createCheckBox(
				"Marken im Debugger verwenden" );
    this.cbLabelsToDebugger.setEnabled( false );
    gbcEtc.gridy++;
    panelEtc.add( this.cbLabelsToDebugger, gbcEtc );

    ButtonGroup grpLabelInDebugger = new ButtonGroup();

    this.rbLabelsCreateOrUpdateBPs = GUIFactory.createRadioButton(
		"Halte-/Log-Punkte und Variablen auf Marken anlegen"
				+ " bzw. aktualisieren" );
    grpLabelInDebugger.add( this.rbLabelsCreateOrUpdateBPs );
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    panelEtc.add( this.rbLabelsCreateOrUpdateBPs, gbcEtc );

    this.rbLabelsUpdateBPsOnly = GUIFactory.createRadioButton(
		"Nur vorhandene Halte-/Log-Punkte und Variablen"
				+ " aktualisieren" );
    grpLabelInDebugger.add( this.rbLabelsUpdateBPsOnly );
    gbcEtc.gridy++;
    panelEtc.add( this.rbLabelsUpdateBPsOnly, gbcEtc );

    this.cbLabelsToReass = GUIFactory.createCheckBox(
				"Marken im Reassembler verwenden" );
    this.cbLabelsToReass.setEnabled( false );
    gbcEtc.insets.left = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.cbLabelsToReass, gbcEtc );

    this.cbAsmListing = GUIFactory.createCheckBox(
					"Assembler-Listing erzeugen" );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.cbAsmListing, gbcEtc );


    // Bereich Knoepfe
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( createButtons( "Assemblieren" ), gbc );


    // Vorbelegungen
    if( options != null ) {
      switch( options.getAsmSyntax() ) {
	case ZILOG_ONLY:
	  this.rbSyntaxZilog.setSelected( true );
	  break;
	case ROBOTRON_ONLY:
	  this.rbSyntaxRobotron.setSelected( true );
	  break;
	default:
	  this.rbSyntaxBoth.setSelected( true );
	  break;
      }
      this.cbAllowUndocInst.setSelected( options.getAllowUndocInst() );
      this.cbAsmListing.setSelected( options.getCreateAsmListing() );
      this.cbLabelsCaseSensitive.setSelected(
					options.getLabelsCaseSensitive() );
      this.cbPrintLabels.setSelected( options.getPrintLabels() );
      this.cbWarnNonAsciiChars.setSelected( options.getWarnNonAsciiChars() );
      this.cbReplaceTooLongRelJumps.setSelected(
				options.getReplaceTooLongRelJumps() );
      this.cbFormatSource.setSelected( options.getFormatSource() );
      this.cbLabelsToDebugger.setSelected( options.getLabelsToDebugger() );
      if( options.getLabelsUpdateBreakpointsOnly() ) {
	this.rbLabelsUpdateBPsOnly.setSelected( true );
      } else {
	this.rbLabelsCreateOrUpdateBPs.setSelected( true );
      }
      this.cbLabelsToReass.setSelected( options.getLabelsToReassembler() );
      updCodeDestFields( options, false );
    } else {
      this.rbSyntaxBoth.setSelected( true );
      this.cbAllowUndocInst.setSelected( false );
      this.cbAsmListing.setSelected( false );
      this.cbLabelsCaseSensitive.setSelected( false );
      this.cbPrintLabels.setSelected( false );
      this.cbWarnNonAsciiChars.setSelected( true );
      this.cbReplaceTooLongRelJumps.setSelected( false );
      this.cbFormatSource.setSelected( false );
      this.cbLabelsToDebugger.setSelected( false );
      this.rbLabelsCreateOrUpdateBPs.setSelected( true );
      this.cbLabelsToReass.setSelected( false );
      updCodeDestFields( options, true );
    }
    updLabelToDebuggerActionsEnabled();


    // Listener
    this.cbLabelsToDebugger.addActionListener( this );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void codeToEmuChanged( boolean state )
  {
    this.cbLabelsToDebugger.setEnabled( state );
    this.cbLabelsToReass.setEnabled( state );
    updLabelToDebuggerActionsEnabled();
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = super.doAction( e );
    if( !rv && (e.getSource() == this.cbLabelsToDebugger) ) {
      updLabelToDebuggerActionsEnabled();
      rv = true;
    }
    return rv;
  }


  @Override
  protected void doApply()
  {
    try {
      Z80Assembler.Syntax syntax = Z80Assembler.Syntax.ALL;
      if( this.rbSyntaxZilog.isSelected() ) {
	syntax = Z80Assembler.Syntax.ZILOG_ONLY;
      }
      else if( this.rbSyntaxRobotron.isSelected() ) {
	syntax = Z80Assembler.Syntax.ROBOTRON_ONLY;
      }
      this.appliedOptions = new PrgOptions( this.oldOptions );
      this.appliedOptions.setAsmSyntax( syntax );
      this.appliedOptions.setAllowUndocInst(
			this.cbAllowUndocInst.isSelected() );
      this.appliedOptions.setCreateAsmListing(
			this.cbAsmListing.isSelected() );
      this.appliedOptions.setLabelsCaseSensitive(
			this.cbLabelsCaseSensitive.isSelected() );
      this.appliedOptions.setPrintLabels( this.cbPrintLabels.isSelected() );
      this.appliedOptions.setLabelsToDebugger(
			this.cbLabelsToDebugger.isSelected() );
      this.appliedOptions.setLabelsUpdateBreakpointsOnly(
			this.rbLabelsUpdateBPsOnly.isSelected() );
      this.appliedOptions.setLabelsToReassembler(
			this.cbLabelsToReass.isSelected() );
      this.appliedOptions.setFormatSource(
			this.cbFormatSource.isSelected() );
      this.appliedOptions.setReplaceTooLongRelJumps(
			this.cbReplaceTooLongRelJumps.isSelected() );
      this.appliedOptions.setWarnNonAsciiChars(
			this.cbWarnNonAsciiChars.isSelected() );
      try {
	applyCodeDestOptionsTo( this.appliedOptions );
	doClose();
      }
      catch( UserInputException ex ) {
	showErrorDlg( this, "Erzeugter Programmcode:\n" + ex.getMessage() );
      }
    }
    catch( NumberFormatException ex ) {
      showErrorDlg( this, ex.getMessage() );
    }
  }


	/* --- private Methoden --- */

  private void updLabelToDebuggerActionsEnabled()
  {
    boolean state = this.cbLabelsToDebugger.isEnabled()
			&& this.cbLabelsToDebugger.isSelected();
    this.rbLabelsCreateOrUpdateBPs.setEnabled( state );
    this.rbLabelsUpdateBPsOnly.setEnabled( state );
  }
}
