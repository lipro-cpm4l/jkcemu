/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anlegen eines Haltepunktes auf eine Adresse
 */

package jkcemu.tools.debugger;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import jkcemu.base.*;


public class PCBreakpointDlg extends AbstractBreakpointDlg
{
  private static String[] registers = {
				"A", "B", "C", "D", "E", "H", "L",
				"BC", "DE", "HL",
				"IX", "IXH", "IXL",
				"IY", "IYH", "IYL",
				"SP" };

  private HexDocument docAddr;
  private HexDocument docMask;
  private HexDocument docValue;
  private JLabel      labelReg1;
  private JLabel      labelReg2;
  private JLabel      labelReg3;
  private JComboBox   comboReg;
  private JComboBox   comboCond;
  private JCheckBox   btnCheckReg;
  private JTextField  fldAddr;
  private JTextField  fldMask;
  private JTextField  fldValue;


  public PCBreakpointDlg( Window owner, AbstractBreakpoint breakpoint )
  {
    super( owner, "Programmadresse", breakpoint );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    add( new JLabel( "Adresse:" ), gbc );

    this.docAddr = new HexDocument( 4 );
    this.fldAddr = new JTextField( this.docAddr, "", 0 );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx++;
    add( this.fldAddr, gbc );

    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.btnCheckReg = new JCheckBox(
		"Zus\u00E4tzlich Registerinhalt vor Befehlsausf\u00FChrung"
			+ " pr\u00FCfen",
		false );
    gbc.insets.bottom = 0;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.gridy++;
    add( this.btnCheckReg, gbc );

    JPanel panelReg = new JPanel( new GridBagLayout() );
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.gridx++;
    gbc.gridy++;
    add( panelReg, gbc );

    GridBagConstraints gbcReg = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );

    this.labelReg1 = new JLabel( "Nur anhalten wenn Register" );
    panelReg.add( this.labelReg1, gbcReg );

    this.comboReg = new JComboBox( registers );
    this.comboReg.setEditable( false );
    gbcReg.insets.left = 5;
    gbcReg.gridx++;
    panelReg.add( this.comboReg, gbcReg );

    this.labelReg2 = new JLabel( "UND" );
    gbcReg.gridx++;
    panelReg.add( this.labelReg2, gbcReg );

    this.docMask = new HexDocument( 4 );
    this.fldMask = new JTextField( this.docMask, "", 4 );
    this.fldMask.setToolTipText( "Maske" );
    gbcReg.fill    = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx = 0.5;
    gbcReg.gridx++;
    panelReg.add( this.fldMask, gbcReg );

    this.comboCond = new JComboBox( conditions );
    this.comboCond.setEditable( false );
    gbcReg.fill    = GridBagConstraints.NONE;
    gbcReg.weightx = 0.0;
    gbcReg.gridx++;
    panelReg.add( this.comboCond, gbcReg );

    this.docValue = new HexDocument( 4 );
    this.fldValue = new JTextField( this.docValue, "", 4 );
    this.fldValue.setToolTipText( "Vergleichswert" );
    gbcReg.fill    = GridBagConstraints.HORIZONTAL;
    gbcReg.weightx = 0.5;
    gbcReg.gridx++;
    panelReg.add( this.fldValue, gbcReg );

    this.labelReg3 = new JLabel( "ist." );
    gbcReg.fill    = GridBagConstraints.NONE;
    gbcReg.weightx = 0.0;
    gbcReg.gridx++;
    panelReg.add( this.labelReg3, gbcReg );

    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    gbc.anchor     = GridBagConstraints.CENTER;
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.insets.top = 5;
    gbc.gridy++;
    add( createGeneralButtons(), gbc );


    // Vorbelegungen
    boolean valueState = false;
    String  reg        = null;
    String  cond       = null;
    int     mask       = 0xFFFF;
    int     value      = 0;
    if( breakpoint != null ) {
      if( breakpoint instanceof PCBreakpoint ) {
	PCBreakpoint bp = (PCBreakpoint) breakpoint;
	this.docAddr.setValue( bp.getAddress(), 4 );
	reg   = bp.getRegister();
	mask  = bp.getMask();
	cond  = bp.getCondition();
	value = bp.getValue();
	if( (reg != null) && (cond != null) ) {
	  valueState = true;
	}
      }
    }
    this.comboReg.setSelectedItem( reg != null ? reg : "A" );
    regChanged();
    this.docMask.setValue( mask, this.docMask.getMaxLength() );
    this.comboCond.setSelectedItem( cond != null ? cond : "=" );
    this.docValue.setValue( value, this.docValue.getMaxLength() );
    this.btnCheckReg.setSelected( valueState );
    updCheckRegFieldsEnabled();


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Sonstiges
    this.fldMask.setColumns( 0 );
    this.fldValue.setColumns( 0 );
    this.fldAddr.addActionListener( this );
    this.btnCheckReg.addActionListener( this );
    this.comboReg.addActionListener( this );
    this.fldMask.addActionListener( this );
    this.fldValue.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.btnCheckReg ) {
	  rv = true;
	  updCheckRegFieldsEnabled();
	}
	else if( src == this.comboReg ) {
	  rv = true;
	  regChanged();
	}
	else if( (src == this.fldAddr) || (src == this.fldValue) ) {
	  rv = true;
	  doApprove();
	}
	else if( src == this.fldMask ) {
	  rv = true;
	  this.comboCond.requestFocus();
	}
      }
    }
    if( !rv ) {
      rv = super.doAction( e );
    }
    return rv;
  }


  @Override
  protected void doApprove()
  {
    String fldName = "Adresse";
    try {
      boolean status = true;
      int     addr   = this.docAddr.intValue();
      String  reg    = null;
      String  cond   = null;
      int     mask   = 0xFFFF;
      int     value  = 0;
      if( this.btnCheckReg.isSelected() ) {
	Object o = this.comboReg.getSelectedItem();
	if( o != null ) {
	  reg = o.toString();
	}
	o = this.comboCond.getSelectedItem();
	if( o != null ) {
	  cond = o.toString();
	}
	if( (reg != null) && (cond != null) ) {
	  boolean is8Bit = true;
	  if( reg.length() == 2 ) {
	    is8Bit = false;
	  }
	  int m   = (is8Bit ? 0xFF : 0xFFFF);
	  fldName = "Maske";
	  mask    = this.docMask.intValue() & m;
	  fldName = "Wert";
	  value   = this.docValue.intValue() & m;
	  status  = checkMaskValue( is8Bit, mask, value );
	}
      }
      if( status ) {
	approveBreakpoint( new PCBreakpoint(
					addr,
					reg,
					mask,
					cond,
					value ) );
      }
    }
    catch( NumberFormatException ex ) {
      showInvalidFmt( fldName );
    }
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( this.fldAddr != null ) {
      this.fldAddr.requestFocus();
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private void regChanged()
  {
    int    n = 2;
    Object o = this.comboReg.getSelectedItem();
    if( o != null ) {
      String s = o.toString();
      if( s != null ) {
	if( s.length() == 2 ) {
	  n = 4;
	}
      }
    }
    this.docMask.setMaxLength( n, 'F' );
    this.docValue.setMaxLength( n, '0' );
  }


  private void updCheckRegFieldsEnabled()
  {
    boolean state = this.btnCheckReg.isSelected();
    this.labelReg1.setEnabled( state );
    this.labelReg2.setEnabled( state );
    this.labelReg3.setEnabled( state );
    this.comboReg.setEnabled( state );
    this.fldMask.setEnabled( state );
    this.comboCond.setEnabled( state );
    this.fldValue.setEnabled( state );
  }
}
