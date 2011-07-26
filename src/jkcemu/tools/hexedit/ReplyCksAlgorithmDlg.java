/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Auswahl eines Pruefsummen-/Hash-Algorithmuses
 */

package jkcemu.tools.hexedit;

import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;


public class ReplyCksAlgorithmDlg extends BasicDlg
{
  private String    approvedAlgorithm;
  private JComboBox comboAlgorithm;
  private JButton   btnOK;
  private JButton   btnCancel;


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


	/* --- private Methoden --- */

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
    add( new JLabel( "Algorithmus:" ), gbc );

    this.comboAlgorithm = new JComboBox(
		CksCalculator.getAvailableAlgorithms() );
    this.comboAlgorithm.setEditable( false );
    if( preSelection != null ) {
      this.comboAlgorithm.setSelectedItem( preSelection );
    }
    this.comboAlgorithm.addKeyListener( this );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.comboAlgorithm, gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


  private void doApprove()
  {
    Object o = this.comboAlgorithm.getSelectedItem();
    if( o != null ) {
      this.approvedAlgorithm = o.toString();
      doClose();
    }
  }
}
