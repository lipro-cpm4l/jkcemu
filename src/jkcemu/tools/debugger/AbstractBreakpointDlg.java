/*
 * (c) 2013-2021 Jens Mueller
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
import java.util.Collection;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;


public abstract class AbstractBreakpointDlg extends BaseDlg
{
  protected static String[] conditions = { "<", "<=", "<>", "=", ">=" , ">" };

  protected DebugFrm           debugFrm;
  protected AbstractBreakpoint oldBP;

  private static boolean lastStopEnabled = true;
  private static boolean lastLogEnabled  = false;

  private boolean            notified;
  private AbstractBreakpoint approvedBreakpoint;
  private JCheckBox          cbStopEnabled;
  private JCheckBox          cbLogEnabled;
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
    this.oldBP              = breakpoint;
    this.approvedBreakpoint = null;
    this.notified           = false;
  }


  protected void approveBreakpoint( AbstractBreakpoint breakpoint )
  {
    if( breakpoint instanceof ImportableBreakpoint ) {

      /*
       * Sicherstellen, dass eskeinen weiteren Halte-/Log-Punkt
       * mit dem gleichen Namen gibt
       */
      String name = ((ImportableBreakpoint) breakpoint).getName();
      if( name != null ) {
	AbstractBreakpoint otherBP = null;
	if( breakpoint instanceof PCBreakpoint ) {
	  otherBP = this.debugFrm.getPCBreakpointByName( name );
	} else if( breakpoint instanceof MemoryBreakpoint ) {
	  otherBP = this.debugFrm.getMemoryBreakpointByName( name );
	}
	if( (otherBP != null) && (otherBP != this.oldBP) ) {
	  if( !showConfirmDlg(
			this,
			"Ein Halte-Log-Punkt mit dem Namen existiert"
				+ " bereits.\n"
				+ "Dieser wird aktualisiert." ) )
	  {
	    breakpoint = null;
	  }
	}
      }

      /*
       * Markierung fuer importiert uebernehmen, aber nur,
       * wenn sich die Marke und die Adresse nicht geaendert haben.
       */
      if( (breakpoint != null) && (this.oldBP != null) ) {
	if( this.oldBP instanceof ImportableBreakpoint ) {
	  if( (((ImportableBreakpoint) breakpoint).getAddress()
			== ((ImportableBreakpoint) this.oldBP).getAddress())
	      && (((ImportableBreakpoint) breakpoint).getName()
			== ((ImportableBreakpoint) this.oldBP).getName()) )
	  {
	    ((ImportableBreakpoint) breakpoint).setImported(
			((ImportableBreakpoint) this.oldBP).getImported() );
	  }
	}
      }
    }

    // Status uebernehmen
    if( breakpoint != null ) {
      breakpoint.setStopEnabled( this.cbStopEnabled.isSelected() );
      breakpoint.setLogEnabled( this.cbLogEnabled.isSelected() );
      this.approvedBreakpoint = breakpoint;
      doClose();
      lastStopEnabled = breakpoint.isStopEnabled();
      lastLogEnabled  = breakpoint.isLogEnabled();
    }
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
    JPanel panel = GUIFactory.createPanel( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.cbStopEnabled = GUIFactory.createCheckBox(
		"Programmausf\u00FChrung anhalten (Haltepunkt)",
		lastStopEnabled );
    panel.add( this.cbStopEnabled, gbc );

    this.cbLogEnabled = GUIFactory.createCheckBox(
				"Log-Meldung erzeugen (Log-Punkt)",
				lastLogEnabled );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    panel.add( this.cbLogEnabled, gbc );

    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.insets.left = 5;
    gbc.gridy++;
    panel.add( panelBtn, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );

    if( this.notified ) {
      this.btnOK.addActionListener( this );
      this.btnCancel.addActionListener( this );
    }

    if( this.oldBP != null ) {
      this.cbStopEnabled.setSelected( this.oldBP.isStopEnabled() );
      this.cbLogEnabled.setSelected( this.oldBP.isLogEnabled() );
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
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      if( this.btnOK != null ) {
	this.btnOK.addActionListener( this );
      }
      if( this.btnCancel != null ) {
	this.btnCancel.addActionListener( this );
      }
    }
  }


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


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      if( this.btnOK != null ) {
	this.btnOK.removeActionListener( this );
      }
      if( this.btnCancel != null ) {
	this.btnCancel.removeActionListener( this );
      }
    }
  }
}
