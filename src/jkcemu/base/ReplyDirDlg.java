/*
 * (c) 2010-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Abfrage eines Verzeichnisnamens
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;


public class ReplyDirDlg extends BasicDlg
{
  private String     replyText;
  private JTextField textFld;
  private JButton    btnSelect;
  private JButton    btnOK;
  private JButton    btnCancel;


  public static String showReplyDirDlg(
				Window owner,
				String msg,
				String title,
				String defaultText )
  {
    ReplyDirDlg dlg = new ReplyDirDlg( owner, msg, title, defaultText );
    dlg.setVisible( true );
    return dlg.replyText;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnSelect ) {
	rv = true;
	doSelect();
      }
      else if( (src == this.btnOK) || (src == this.textFld) ) {
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
  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this) && (this.textFld != null) ) {
      this.textFld.requestFocus();
      this.textFld.selectAll();
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private ReplyDirDlg(
		Window owner,
		String msg,
		String title,
		String defaultText )
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
    this.textFld = new JTextField();
    if( defaultText != null ) {
      this.textFld.setText( defaultText );
    }
    this.textFld.addActionListener( this );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx++;
    add( this.textFld, gbc );

    this.btnSelect = createImageButton(
				"/images/file/open.png",
				"Ausw\u00E4hlen" );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( this.btnSelect, gbc );


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
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( panelBtn, gbc );


    // sonstiges
    pack();
    setParentCentered();
    setResizable( true );
  }


  private void doApprove()
  {
    String text = this.textFld.getText();
    if( text != null ) {
      text = text.trim();
      if( !text.isEmpty() ) {
	this.replyText = this.textFld.getText();
	doClose();
      }
    }
  }


  private void doSelect()
  {
    File file = DirSelectDlg.selectDirectory( this );
    if( file != null )
      this.textFld.setText( file.getPath() );
  }
}

