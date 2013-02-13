/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Assembler-Optionen
 */

package jkcemu.programming.assembler;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.programming.*;


public class AsmOptionsDlg extends AbstractOptionsDlg
{
  private JRadioButton btnSyntaxZilog;
  private JRadioButton btnSyntaxRobotron;
  private JRadioButton btnSyntaxBoth;
  private JCheckBox    btnAllowUndocInst;
  private JCheckBox    btnLabelsCaseSensitive;
  private JCheckBox    btnPrintLabels;
  private JCheckBox    btnLabelsToDebugger;
  private JCheckBox    btnLabelsToReass;
  private JCheckBox    btnFormatSource;
  private JCheckBox    btnWarnNonAsciiChars;


  public AsmOptionsDlg( Frame owner, EmuThread emuThread, PrgOptions options )
  {
    super( owner, emuThread, "Assembler-Optionen" );


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
    JPanel panelSyntax = new JPanel( new GridBagLayout() );
    panelSyntax.setBorder( BorderFactory.createTitledBorder(
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

    this.btnSyntaxBoth = new JRadioButton(
				"Zilog- und Robotron-Mnemonik/-Syntax" );
    grpSyntax.add( this.btnSyntaxBoth );
    panelSyntax.add( this.btnSyntaxBoth, gbcSyntax );

    this.btnSyntaxZilog = new JRadioButton(
				"Nur Zilog-Mnemonik/-Syntax erlauben" );
    grpSyntax.add( this.btnSyntaxZilog );
    gbcSyntax.insets.top = 0;
    gbcSyntax.gridy++;
    panelSyntax.add( this.btnSyntaxZilog, gbcSyntax );

    this.btnSyntaxRobotron = new JRadioButton(
				"Nur Robotron-Mnemonik/-Syntax erlauben" );
    grpSyntax.add( this.btnSyntaxRobotron );
    gbcSyntax.insets.bottom = 5;
    gbcSyntax.gridy++;
    panelSyntax.add( this.btnSyntaxRobotron, gbcSyntax );

    this.btnAllowUndocInst = new JCheckBox(
				"Undokumentierte Befehle erlauben" );
    gbcSyntax.insets.top = 0;
    gbcSyntax.gridy++;
    panelSyntax.add( this.btnAllowUndocInst, gbcSyntax );


    // Bereich Marken
    JPanel panelLabel = new JPanel( new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    panelLabel.setBorder( BorderFactory.createTitledBorder( "Marken" ) );
    gbc.gridy++;
    add( panelLabel, gbc );

    this.btnLabelsCaseSensitive = new JCheckBox(
			"Gro\u00DF-/Kleinschreibung bei Marken beachten" );
    this.btnLabelsCaseSensitive.addActionListener( this );
    panelLabel.add( this.btnLabelsCaseSensitive );

    this.btnPrintLabels = new JCheckBox( "Markentabelle ausgeben" );
    this.btnPrintLabels.addActionListener( this );
    panelLabel.add( this.btnPrintLabels );


    // Bereich Erzeugter Programmcode
    JPanel panelCodeDest = createCodeDestOptions();
    panelCodeDest.setBorder( BorderFactory.createTitledBorder(
						"Erzeugter Programmcode" ) );
    gbc.gridy++;
    add( panelCodeDest, gbc );


    // Bereich Sonstiges
    JPanel panelEtc = new JPanel( new GridBagLayout() );
    panelEtc.setBorder( BorderFactory.createTitledBorder( "Sonstiges" ) );
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


    this.btnWarnNonAsciiChars = new JCheckBox(
					"Bei Nicht-ASCII-Zeichen warnen" );
    panelEtc.add( this.btnWarnNonAsciiChars, gbcEtc );

    this.btnFormatSource = new JCheckBox( "Quelltext formatieren" );
    gbcEtc.insets.top = 0;
    gbcEtc.gridy++;
    panelEtc.add( this.btnFormatSource, gbcEtc );

    this.btnLabelsToDebugger = new JCheckBox(
		"Im Debugger Haltepunkte auf Marken anlegen"
			+ " (nur bei Programmcode in Emulator laden)" );
    this.btnLabelsToDebugger.setEnabled( false );
    gbcEtc.gridy++;
    panelEtc.add( this.btnLabelsToDebugger, gbcEtc );

    this.btnLabelsToReass = new JCheckBox(
		"Marken im Reassembler verwenden"
			+ " (nur bei Programmcode in Emulator laden)" );
    this.btnLabelsToReass.setEnabled( false );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    panelEtc.add( this.btnLabelsToReass, gbcEtc );


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
	  this.btnSyntaxZilog.setSelected( true );
	  break;
	case ROBOTRON_ONLY:
	  this.btnSyntaxRobotron.setSelected( true );
	  break;
	default:
	  this.btnSyntaxBoth.setSelected( true );
	  break;
      }
      this.btnAllowUndocInst.setSelected( options.getAllowUndocInst() );
      this.btnLabelsCaseSensitive.setSelected(
					options.getLabelsCaseSensitive() );
      this.btnPrintLabels.setSelected( options.getPrintLabels() );
      this.btnWarnNonAsciiChars.setSelected( options.getWarnNonAsciiChars() );
      this.btnFormatSource.setSelected( options.getFormatSource() );
      this.btnLabelsToDebugger.setSelected( options.getLabelsToDebugger() );
      this.btnLabelsToReass.setSelected( options.getLabelsToReassembler() );
      updCodeDestFields( options, false );
    } else {
      this.btnSyntaxBoth.setSelected( true );
      this.btnAllowUndocInst.setSelected( false );
      this.btnLabelsCaseSensitive.setSelected( false );
      this.btnPrintLabels.setSelected( false );
      this.btnWarnNonAsciiChars.setSelected( true );
      this.btnFormatSource.setSelected( false );
      this.btnLabelsToDebugger.setSelected( false );
      this.btnLabelsToReass.setSelected( false );
      updCodeDestFields( options, true );
    }


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void codeToEmuChanged( boolean state )
  {
    this.btnLabelsToDebugger.setEnabled( state );
    this.btnLabelsToReass.setEnabled( state );
  }


  @Override
  protected void doApply()
  {
    try {
      Z80Assembler.Syntax syntax = Z80Assembler.Syntax.ALL;
      if( this.btnSyntaxZilog.isSelected() ) {
	syntax = Z80Assembler.Syntax.ZILOG_ONLY;
      }
      else if( this.btnSyntaxRobotron.isSelected() ) {
	syntax = Z80Assembler.Syntax.ROBOTRON_ONLY;
      }
      this.appliedOptions = new PrgOptions();
      this.appliedOptions.setAsmSyntax( syntax );
      this.appliedOptions.setAllowUndocInst(
				this.btnAllowUndocInst.isSelected() );
      this.appliedOptions.setLabelsCaseSensitive(
				this.btnLabelsCaseSensitive.isSelected() );
      this.appliedOptions.setPrintLabels( this.btnPrintLabels.isSelected() );
      this.appliedOptions.setLabelsToDebugger(
				this.btnLabelsToDebugger.isSelected() );
      this.appliedOptions.setLabelsToReassembler(
				this.btnLabelsToReass.isSelected() );
      this.appliedOptions.setFormatSource(
				this.btnFormatSource.isSelected() );
      this.appliedOptions.setWarnNonAsciiChars(
				this.btnWarnNonAsciiChars.isSelected() );
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
}

