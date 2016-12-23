/*
 * (c) 2013-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Dialog zum Anlegen und Bearbeiten
 * eines Halte-/Log-Punktes
 */

package jkcemu.tools.debugger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.lang.*;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import jkcemu.base.BaseDlg;


public abstract class AbstractBreakpointDlg extends BaseDlg
{
  protected static String[] conditions = { "<", "<=", "<>", "=", ">=" , ">" };

  protected DebugFrm           debugFrm;
  protected AbstractBreakpoint breakpoint;

  private static boolean lastStopEnabled = true;
  private static boolean lastLogEnabled  = false;

  private AbstractBreakpoint approvedBreakpoint;
  private JCheckBox          btnStopEnabled;
  private JCheckBox          btnLogEnabled;
  private JButton            btnOK;
  private JButton            btnCancel;


  protected AbstractBreakpointDlg(
			DebugFrm           debugFrm,
			String             watchedObj,
			AbstractBreakpoint breakpoint )
  {
    super(
	debugFrm,
	breakpoint != null ?
		("Halte-/Log-Punkt auf " + watchedObj + " bearbeiten")
		: ("Neuer Halte-/Log-Punkt auf " + watchedObj) );
    this.debugFrm           = debugFrm;
    this.breakpoint         = breakpoint;
    this.approvedBreakpoint = null;
  }


  protected void approveBreakpoint( AbstractBreakpoint breakpoint )
  {
    breakpoint.setStopEnabled( this.btnStopEnabled.isSelected() );
    breakpoint.setLogEnabled( this.btnLogEnabled.isSelected() );
    this.approvedBreakpoint = breakpoint;
    doClose();
    lastStopEnabled = breakpoint.isStopEnabled();
    lastLogEnabled  = breakpoint.isLogEnabled();
  }


  protected boolean checkMaskValue( boolean is8Bit, int mask, int value )
  {
    boolean rv = true;
    int     m  = (is8Bit ? 0xFF : 0xFFFF);
    if( mask == 0 ) {
      showErrorDlg(
		this,
		"Die Maske 00 blendet den zu testenden Wert"
			+ " vollst\u00E4ndig aus." );
      rv = false;
    } else if( (~mask & value & m) != 0 ) {
      rv = showYesNoWarningDlg(
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
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.btnStopEnabled = new JCheckBox(
		"Programmausf\u00FChrung anhalten (Haltepunkt)",
		lastStopEnabled );
    panel.add( this.btnStopEnabled, gbc );

    this.btnLogEnabled = new JCheckBox(
				"Log-Meldung erzeugen (Log-Punkt)",
				lastLogEnabled );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    panel.add( this.btnLogEnabled, gbc );

    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.insets.left = 5;
    gbc.gridy++;
    panel.add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );

    if( this.breakpoint != null ) {
      this.btnStopEnabled.setSelected( this.breakpoint.isStopEnabled() );
      this.btnLogEnabled.setSelected( this.breakpoint.isLogEnabled() );
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
    showErrorDlg(
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
