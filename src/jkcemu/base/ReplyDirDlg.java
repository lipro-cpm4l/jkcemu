/*
 * (c) 2010-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Abfrage eines Verzeichnisnamens
 */

package jkcemu.base;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jkcemu.file.DirSelectDlg;


public class ReplyDirDlg extends BaseDlg
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
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.textFld.removeActionListener( this );
      this.btnSelect.removeActionListener( this );
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
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


	/* --- Konstruktor --- */

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
    add( GUIFactory.createLabel( msg ), gbc );
    this.textFld = GUIFactory.createTextField();
    if( defaultText != null ) {
      this.textFld.setText( defaultText );
    }
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx++;
    add( this.textFld, gbc );

    this.btnSelect = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_SELECT );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( this.btnSelect, gbc );


    // Bereich Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );

    this.btnOK = GUIFactory.createButtonOK();
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );

    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( panelBtn, gbc );


    // Listener
    this.textFld.addActionListener( this );
    this.btnSelect.addActionListener( this );
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );


    // sonstiges
    pack();
    setParentCentered();
    setResizable( true );
  }


	/* --- private und Methoden --- */

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
    File   lastDir = null;
    String text    = this.textFld.getText();
    if( text != null ) {
      text = text.trim();
      if( !text.isEmpty() ) {
	lastDir = new File( text );
      }
    }
    File file = DirSelectDlg.selectDirectory( this, lastDir );
    if( file != null ) {
      this.textFld.setText( file.getPath() );
    }
  }
}
