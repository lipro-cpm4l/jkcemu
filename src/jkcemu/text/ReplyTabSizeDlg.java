/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Eingabe der Tabulatorbreite
 */

package jkcemu.text;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.lang.*;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.base.BaseDlg;


public class ReplyTabSizeDlg extends BaseDlg
{
  private JTextArea textArea;
  private JSpinner  spinnerTabSize;
  private JCheckBox tglAsDefault;
  private JButton   btnApply;
  private JButton   btnClose;


  public ReplyTabSizeDlg(
		Frame     parent,
		JTextArea textArea )
  {
    super( parent, "Tabulatorbreite \u00E4ndern" );
    this.textArea = textArea;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    JPanel panelTabSize = new JPanel( new FlowLayout( FlowLayout.CENTER ) );
    add( panelTabSize, gbc );
    panelTabSize.add( new JLabel( "Tabulatorbreite:" ) );

    int tabSize = this.textArea.getTabSize();
    if( tabSize < 1 ) {
      tabSize = 8;
    }
    this.spinnerTabSize = new JSpinner(
				new SpinnerNumberModel( tabSize, 1, 99, 1 ) );
    panelTabSize.add( this.spinnerTabSize );

    this.tglAsDefault = new JCheckBox( "Als Standardwert setzen" );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( this.tglAsDefault, gbc );

    // Knoepfe
    JPanel panelBtn = new JPanel();
    panelBtn.setLayout( new GridLayout( 1, 2, 5, 5 ) );

    this.btnApply = new JButton( "\u00DCbernehmen" );
    this.btnApply.addActionListener( this );
    this.btnApply.addKeyListener( this );
    panelBtn.add( this.btnApply );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    this.btnClose.addKeyListener( this );
    panelBtn.add( this.btnClose );

    gbc.insets.top = 5;
    gbc.gridy++;
    add( panelBtn, gbc );

    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
        if( src.equals( this.btnApply ) ) {
	  rv = true;
	  doApply();
	}
	else if( src.equals( this.btnClose ) ) {
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this) && (this.spinnerTabSize != null) ) {
      this.spinnerTabSize.requestFocus();
      Component c = this.spinnerTabSize.getEditor();
      if( c != null ) {
	if( c instanceof JTextComponent )
	  ((JTextComponent) c).selectAll();
      }
    }
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    Object value = this.spinnerTabSize.getValue();
    if( value != null ) {
      if( value instanceof Number ) {
	int tabSize = ((Number) value).intValue();
	if( tabSize > 0 ) {
	  this.textArea.setTabSize( tabSize );
	  if( this.tglAsDefault.isSelected() ) {
	    Main.setProperty(
			TextEditFrm.PROP_TABSIZE,
			String.valueOf( tabSize ) );
	  }
	}
      }
    }
  }
}
