/*
 * (c) 2010-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Auswahl eines Pruefsummen-/Hash-Algorithmuses
 */

package jkcemu.tools.hexedit;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;
import jkcemu.etc.CksCalculator;


public class ReplyCksAlgorithmDlg extends BaseDlg
{
  private String            approvedAlgorithm;
  private JComboBox<String> comboAlgorithm;
  private JButton           btnOK;
  private JButton           btnCancel;


  public static String askCksAlgorithm( Window owner, String preSelection )
  {
    ReplyCksAlgorithmDlg dlg = new ReplyCksAlgorithmDlg(
						owner,
						preSelection );
    dlg.setVisible( true );
    return dlg.approvedAlgorithm;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
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
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.comboAlgorithm.removeKeyListener( this );
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void keyPressed( KeyEvent e )
  {
    if( e.getComponent() == this.comboAlgorithm ) {
      if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
	doApprove();
	e.consume();
      }
    }
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    this.comboAlgorithm.requestFocus();
  }


	/* --- Konstruktor --- */

  private ReplyCksAlgorithmDlg( Window owner, String preSelection )
  {
    super( owner, "Pr\u00FCfsummen-/Hash-Algorithmus" );
    this.approvedAlgorithm = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );


    // Eingabebereich
    add( GUIFactory.createLabel( "Algorithmus:" ), gbc );

    this.comboAlgorithm = GUIFactory.createComboBox(
		CksCalculator.getAvailableAlgorithms() );
    this.comboAlgorithm.setEditable( false );
    if( preSelection != null ) {
      this.comboAlgorithm.setSelectedItem( preSelection );
    }
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.comboAlgorithm, gbc );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Listener
    this.comboAlgorithm.addKeyListener( this );
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- private Methoden --- */

  private void doApprove()
  {
    Object o = this.comboAlgorithm.getSelectedItem();
    if( o != null ) {
      this.approvedAlgorithm = o.toString();
      doClose();
    }
  }
}
