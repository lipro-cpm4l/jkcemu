/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anlegen eines Haltepunktes auf ein Eingabetor
 */

package jkcemu.tools.debugger;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;


public class InputBreakpointDlg extends AbstractBreakpointDlg
{
  private HexDocument docBegPort;
  private HexDocument docEndPort;
  private JTextField  fldBegPort;
  private JTextField  fldEndPort;


  public InputBreakpointDlg( Window owner, AbstractBreakpoint breakpoint )
  {
    super( owner, "Eingabetor", breakpoint );


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

    add( new JLabel( "Eingabeadresse (8 oder 16 Bit):" ), gbc );

    this.docBegPort = new HexDocument( 4 );
    this.fldBegPort = new JTextField( this.docBegPort, "", 4 );
    gbc.anchor      = GridBagConstraints.WEST;
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.gridx++;
    add( this.fldBegPort, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx   = 0;
    gbc.gridy++;
    add( new JLabel( "Bis Eingabeadresse (optional):" ), gbc );

    this.docEndPort = new HexDocument( 4 );
    this.fldEndPort = new JTextField( this.docEndPort, "", 4 );
    gbc.anchor      = GridBagConstraints.WEST;
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.gridx++;
    add( this.fldEndPort, gbc );

    gbc.anchor     = GridBagConstraints.CENTER;
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.insets.top = 5;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( createGeneralButtons(), gbc );


    // Vorbelegungen
    if( breakpoint != null ) {
      if( breakpoint instanceof InputBreakpoint ) {
	InputBreakpoint bp = (InputBreakpoint) breakpoint;
	int             n  = (bp.get8Bit() ? 2 : 4);
	this.docBegPort.setValue( bp.getBegPort(), n );
	int endPort = bp.getEndPort();
	if( endPort >= 0 ) {
	  this.docEndPort.setValue( endPort, n );
	}
      }
    }

    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Sonstiges
    this.fldBegPort.setColumns( 0 );
    this.fldEndPort.setColumns( 0 );
    this.fldBegPort.addActionListener( this );
    this.fldEndPort.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.fldBegPort ) {
	  rv = true;
	  this.fldEndPort.requestFocus();
	}
	else if( src == this.fldEndPort ) {
	  rv = true;
	  doApprove();
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
      boolean is8Bit  = false;
      int     begPort = this.docBegPort.intValue() & 0xFFFF;
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
      approveBreakpoint( new InputBreakpoint( is8Bit, begPort, endPort ) );
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
}

