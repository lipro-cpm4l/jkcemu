/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Eingabe-Dialog
 */

package jkcemu.base;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.lang.*;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class AbstractReplyDlg extends BaseDlg
{
  protected JTextField replyTextField;

  private String  replyText;
  private JButton btnOK;
  private JButton btnCancel;


  protected AbstractReplyDlg(
			Window owner,
			String msg,
			String title,
			String defaultReply )
  {
    super( owner, title );
    this.replyText = null;

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

    // Bereich Eingabe
    add( new JLabel( msg ), gbc );
    this.replyTextField = new JTextField();
    if( defaultReply != null ) {
      this.replyTextField.setText( defaultReply );
    }
    this.replyTextField.addActionListener( this );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx++;
    add( this.replyTextField, gbc );

    // Bereich Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    this.btnOK.addKeyListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );

    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = 2;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( panelBtn, gbc );
  }


  /*
   * Diese Methode wird aufgerufen, wenn der OK-Knopf gedrueckt wird.
   * Wenn die Eingabe in Ordnung ist, muss true zurueckgeliefert werden,
   * damit der Dialog geschlossen wird.
   */
  protected boolean approveSelection()
  {
    return true;
  }


  /*
   * Die Methode setzt die Fenstergroesse und -position.
   */
  protected void fitWindowBounds()
  {
    pack();
    setParentCentered();
    setResizable( true );
  }


  public String getReplyText()
  {
    return this.replyText;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.btnOK) || (src == this.replyTextField) ) {
	rv = true;
	if( approveSelection() ) {
	  this.replyText = this.replyTextField.getText();
	  doClose();
	}
      }
      else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      }
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this) && (this.replyTextField != null) ) {
      this.replyTextField.requestFocus();
      this.replyTextField.selectAll();
    }
  }
}
