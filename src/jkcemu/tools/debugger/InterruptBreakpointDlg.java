/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anlegen eines Halte-/Log-Punktes auf eine Interrupt-Quelle
 */

package jkcemu.tools.debugger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JSeparator;
import jkcemu.base.GUIFactory;
import z80emu.Z80InterruptSource;


public class InterruptBreakpointDlg extends AbstractBreakpointDlg
{
  private JComboBox<Object> comboIntSource;


  public InterruptBreakpointDlg(
			DebugFrm             debugFrm,
			AbstractBreakpoint   breakpoint,
			Z80InterruptSource[] iSources )
  {
    super( debugFrm, "Interrupt-Quelle", breakpoint );

    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 10, 5, 0, 5 ),
						0, 0 );

    this.comboIntSource = GUIFactory.createComboBox();
    this.comboIntSource.setEditable( false );
    this.comboIntSource.addKeyListener( this );
    setInterruptSources( iSources );
    add( this.comboIntSource, gbc );

    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridy++;
    add( GUIFactory.createSeparator(), gbc );

    gbc.insets.bottom = 0;
    gbc.gridy++;
    add( createGeneralButtons(), gbc );


    // Vorbelegungen
    if( breakpoint != null ) {
      if( breakpoint instanceof InterruptBreakpoint ) {
	this.comboIntSource.setSelectedItem(
		((InterruptBreakpoint) breakpoint).getInterruptSource() );
      }
    }


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
  }


  public void setInterruptSources( Z80InterruptSource[] iSources )
  {
    if( iSources != null ) {
      if( iSources.length == 0 ) {
	iSources = null;
      }
    }
    if( iSources != null ) {
      this.comboIntSource.setModel(
		new DefaultComboBoxModel<>( (Object[]) iSources ) );
    } else {
      Object[] a = { "--- Keine Interrupt-Quelle vorhanden---" };
      this.comboIntSource.setModel( new DefaultComboBoxModel<>( a ) );
    }
    pack();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.comboIntSource ) {
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
    Object o = this.comboIntSource.getSelectedItem();
    if( o != null ) {
      if( o instanceof Z80InterruptSource ) {
	approveBreakpoint(
		new InterruptBreakpoint(
				this.debugFrm,
				(Z80InterruptSource) o ) );
      }
    }
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( this.comboIntSource != null )
      this.comboIntSource.requestFocus();
  }
}
