/*
 * (c) 2013-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Anzeige eines Log-Textes
 */

package jkcemu.tools.fileconverter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.lang.*;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;


public class LogDlg extends BaseDlg
{
  private JTextArea textArea;
  private JButton   btnCopy;
  private JButton   btnClose;


  public static void showDlg(
			Window owner,
			String logText,
			String title )
  {
    (new LogDlg( owner, logText, title )).setVisible( true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.btnCopy ) {
	  rv = true;
	  doCopy();
	}
	else if( src == this.btnClose ) {
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private LogDlg( Window owner, String logText, String title )
  {
    super( owner, title );

    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    // Textbereich
    this.textArea = new JTextArea( 16, 40 );
    this.textArea.setEditable( false );
    if( logText != null ) {
      this.textArea.setText( logText );
    }
    add( new JScrollPane( this.textArea ), gbc );

    // Schaltflaechen
    JPanel panelBtns = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.gridy++;
    add( panelBtns, gbc );

    this.btnCopy = new JButton( "Kopieren" );
    this.btnCopy.addActionListener( this );
    panelBtns.add( this.btnCopy );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    panelBtns.add( this.btnClose );

    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
    this.textArea.setColumns( 0 );
    this.textArea.setRows( 0 );
  }


	/* --- Aktionen --- */

  private void doCopy()
  {
    String text = this.textArea.getSelectedText();
    if( text != null ) {
      if( text.isEmpty() ) {
	text = null;
      }
    }
    if( text == null ) {
      text = this.textArea.getText();
    }
    if( text != null ) {
      if( !text.isEmpty() ) {
	EmuUtil.copyToClipboard( this, text );
      }
    }
  }
}
