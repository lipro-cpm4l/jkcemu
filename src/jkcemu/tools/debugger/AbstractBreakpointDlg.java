/*
 * (c) 2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Dialog zum Anlegen und Bearbeiten eines Haltepunktes
 */

package jkcemu.tools.debugger;

import java.awt.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;


public abstract class AbstractBreakpointDlg extends BasicDlg
{
  protected static String[] conditions = { "<", "<=", "<>", "=", ">=" , ">" };

  protected AbstractBreakpoint breakpoint;

  private AbstractBreakpoint approvedBreakpoint;
  private JCheckBox          btnEnabled;
  private JButton            btnOK;
  private JButton            btnCancel;


  protected AbstractBreakpointDlg(
			Window             owner,
			String             watchedObj,
			AbstractBreakpoint breakpoint )
  {
    super(
	owner,
	breakpoint != null ?
		("Haltepunkt auf " + watchedObj + " bearbeiten")
		: ("Neuer Haltepunkt auf " + watchedObj) );
    this.breakpoint         = breakpoint;
    this.approvedBreakpoint = null;
  }


  protected void approveBreakpoint( AbstractBreakpoint breakpoint )
  {
    breakpoint.setEnabled( this.btnEnabled.isSelected() );
    this.approvedBreakpoint = breakpoint;
    doClose();
  }


  protected boolean checkMaskValue( boolean is8Bit, int mask, int value )
  {
    boolean rv = true;
    int     m  = (is8Bit ? 0xFF : 0xFFFF);
    if( mask == 0 ) {
      BasicDlg.showErrorDlg(
		this,
		"Die Maske 00 blendet den zu testenden Wert"
			+ " vollst\u00E4ndig aus." );
      rv = false;
    } else if( (~mask & value & m) != 0 ) {
      rv = BasicDlg.showYesNoWarningDlg(
		this,
		String.format(
			"Es ist i.d.R. nicht sinnvoll, wenn im"
				+ " Vergleichswert (rechtes Eingabefeld)"
				+ " Bits gesetzt sind,\n"
				+ "die mit der Maske (linkes Eingabefeld)"
				+ " vom zu testenden Wert ausgeblendet"
				+ " werden.\n"
				+ "Ein zur Maske passender Vergleichswert"
				+ " w\u00E4re z.B. hexadezimal %02X.\n"
				+ "Sollen trotzdem die von Ihnen angegeben"
				+ " Werte verwendet werden?",
			mask & value & m ),
		"Best\u00E4tigung" );
    }
    return rv;
  }


  protected JPanel createGeneralButtons()
  {
    JPanel panel = new JPanel( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.btnEnabled = new JCheckBox( "Haltepunkt aktiv", true );
    panel.add( this.btnEnabled, gbc );

    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.gridy++;
    panel.add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );

    if( this.breakpoint != null ) {
      this.btnEnabled.setSelected( this.breakpoint.isEnabled() );
    }

    return panel;
  }


  protected abstract void doApprove();


  public AbstractBreakpoint getApprovedBreakpoint()
  {
    return this.approvedBreakpoint;
  }


  protected void showInvalidFmt( String fldName )
  {
    BasicDlg.showErrorDlg(
		this,
		fldName + " hat ung\u00FCltiges Format." );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.btnOK ) {
	rv = true;
	doApprove();
      }
      else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }
}

