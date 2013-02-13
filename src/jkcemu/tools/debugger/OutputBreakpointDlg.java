/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anlegen eines Haltepunktes auf ein Ausgabetor
 */

package jkcemu.tools.debugger;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;


public class OutputBreakpointDlg extends AbstractBreakpointDlg
{
  private HexDocument docBegPort;
  private HexDocument docEndPort;
  private HexDocument docMask;
  private HexDocument docValue;
  private JLabel      labelValue1;
  private JLabel      labelValue2;
  private JComboBox   comboCond;
  private JCheckBox   btnCheckValue;
  private JTextField  fldBegPort;
  private JTextField  fldEndPort;
  private JTextField  fldMask;
  private JTextField  fldValue;


  public OutputBreakpointDlg( Window owner, AbstractBreakpoint breakpoint )
  {
    super( owner, "Ausgabetor", breakpoint );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    add( new JLabel( "Ausgabeadresse (8 oder 16 Bit):" ), gbc );

    this.docBegPort = new HexDocument( 4 );
    this.fldBegPort = new JTextField( this.docBegPort, "", 4 );
    gbc.anchor  = GridBagConstraints.WEST;
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx++;
    add( this.fldBegPort, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx   = 0;
    gbc.gridy++;
    add( new JLabel( "Bis Ausgabeadresse (optional):" ), gbc );

    this.docEndPort = new HexDocument( 4 );
    this.fldEndPort = new JTextField( this.docEndPort, "", 4 );
    gbc.anchor  = GridBagConstraints.WEST;
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx++;
    add( this.fldEndPort, gbc );

    gbc.insets.top = 5;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.btnCheckValue = new JCheckBox(
		"Zus\u00E4tzlich auszugebenden Wert pr\u00FCfen",
		false );
    gbc.insets.bottom = 0;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.gridy++;
    add( this.btnCheckValue, gbc );

    JPanel panelValue = new JPanel( new GridBagLayout() );
    gbc.anchor        = GridBagConstraints.WEST;
    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.weightx       = 1.0;
    gbc.insets.left   = 50;
    gbc.gridy++;
    add( panelValue, gbc );

    GridBagConstraints gbcValue = new GridBagConstraints(
                                                0, 0,
                                                1, 1,
                                                0.0, 0.0,
                                                GridBagConstraints.WEST,
                                                GridBagConstraints.NONE,
                                                new Insets( 0, 0, 0, 0 ),
                                                0, 0 );

    this.labelValue1 = new JLabel( "Nur anhalten wenn Wert UND" );
    panelValue.add( this.labelValue1, gbcValue );

    this.docMask = new HexDocument( 2 );
    this.fldMask = new JTextField( this.docMask, "", 2 );
    this.fldMask.setToolTipText( "Maske" );
    gbcValue.fill        = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx     = 0.5;
    gbcValue.insets.left = 5;
    gbcValue.gridx++;
    panelValue.add( this.fldMask, gbcValue );

    this.comboCond = new JComboBox( conditions );
    this.comboCond.setEditable( false );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panelValue.add( this.comboCond, gbcValue );

    this.docValue = new HexDocument( 2 );
    this.fldValue = new JTextField( this.docValue, "", 2 );
    this.fldValue.setToolTipText( "Vergleichswert" );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.5;
    gbcValue.gridx++;
    panelValue.add( this.fldValue, gbcValue );

    this.labelValue2 = new JLabel( "ist." );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panelValue.add( this.labelValue2, gbcValue );

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
    String  cond       = null;
    int     mask       = 0xFFFF;
    int     value      = 0;
    if( breakpoint != null ) {
      if( breakpoint instanceof OutputBreakpoint ) {
	OutputBreakpoint bp = (OutputBreakpoint) breakpoint;
	int              n  = (bp.get8Bit() ? 2 : 4);
	this.docBegPort.setValue( bp.getBegPort(), n );
	int endPort = bp.getEndPort();
	if( endPort >= 0 ) {
	  this.docEndPort.setValue( endPort, n );
	}
	mask  = bp.getMask();
	cond  = bp.getCondition();
	value = bp.getValue();
	if( cond != null ) {
	  valueState = true;
	}
      }
    }
    this.docMask.setValue( mask, 2 );
    this.comboCond.setSelectedItem( cond != null ? cond : "=" );
    this.docValue.setValue( value, 2 );
    this.btnCheckValue.setSelected( valueState );
    updCheckValueFieldsEnabled();


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Sonstiges
    this.fldBegPort.setColumns( 0 );
    this.fldEndPort.setColumns( 0 );
    this.fldMask.setColumns( 0 );
    this.fldValue.setColumns( 0 );
    this.fldBegPort.addActionListener( this );
    this.fldEndPort.addActionListener( this );
    this.btnCheckValue.addActionListener( this );
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
	if( src == this.btnCheckValue ) {
	  rv = true;
	  updCheckValueFieldsEnabled();
	}
	else if( src == this.fldBegPort ) {
	  rv = true;
	  this.fldEndPort.requestFocus();
	}
	else if( (src == this.fldEndPort) || (src == this.fldValue) ) {
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
    String fldName = "Ausgabeadresse";
    try {
      boolean status  = true;
      boolean is8Bit  = false;
      int     begPort = this.docBegPort.intValue();
      String  begText = this.fldBegPort.getText();
      if( begText != null ) {
        if( begText.trim().length() < 3 ) {
          is8Bit = true;
          begPort &= 0xFF;
        }
      }
      int     endPort    = -1;
      Integer tmpEndPort = this.docEndPort.getInteger();
      if( tmpEndPort != null ) {
        endPort = tmpEndPort.intValue() & 0xFFFF;
        if( is8Bit ) {
          endPort &= 0xFF;
        }
      }
      String cond  = null;
      int    mask  = 0xFF;
      int    value = 0;
      if( this.btnCheckValue.isSelected() ) {
	Object o = this.comboCond.getSelectedItem();
	if( o != null ) {
	  cond = o.toString();
	}
	if( cond != null ) {
	  fldName = "Maske";
	  mask    = this.docMask.intValue() & 0xFF;
	  fldName = "Wert";
	  value   = this.docValue.intValue() & 0xFF;
	  status  = checkMaskValue( true, mask, value );
	}
      }
      if( status ) {
	approveBreakpoint( new OutputBreakpoint(
					true,
					begPort,
					endPort,
					cond,
					mask,
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
    if( this.fldBegPort != null )
      this.fldBegPort.requestFocus();
  }


	/* --- private Konstruktoren und Methoden --- */

  private void updCheckValueFieldsEnabled()
  {
    boolean state = this.btnCheckValue.isSelected();
    this.labelValue1.setEnabled( state );
    this.labelValue2.setEnabled( state );
    this.fldMask.setEnabled( state );
    this.comboCond.setEnabled( state );
    this.fldValue.setEnabled( state );
  }
}

