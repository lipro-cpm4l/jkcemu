/*
 * (c) 2008-2021 Jens Mueller
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
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class ReplyTabSizeDlg extends BaseDlg
{
  private JTextArea textArea;
  private JSpinner  spinnerTabSize;
  private JCheckBox cbAsDefault;
  private JButton   btnApply;
  private JButton   btnClose;


  public static void showDlg( Frame owner, JTextArea textArea )
  {
    (new ReplyTabSizeDlg( owner, textArea )).setVisible( true );
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
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.btnApply.removeActionListener( this );
      this.btnClose.removeActionListener( this );
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


	/* --- Konstruktor --- */

  private ReplyTabSizeDlg(
		Frame     owner,
		JTextArea textArea )
  {
    super( owner, "Tabulatorbreite \u00E4ndern" );
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

    JPanel panelTabSize = GUIFactory.createPanel(
				new FlowLayout( FlowLayout.CENTER ) );
    add( panelTabSize, gbc );
    panelTabSize.add( GUIFactory.createLabel( "Tabulatorbreite:" ) );

    int tabSize = this.textArea.getTabSize();
    if( tabSize < 1 ) {
      tabSize = 8;
    }
    this.spinnerTabSize = GUIFactory.createSpinner(
				new SpinnerNumberModel( tabSize, 1, 99, 1 ) );
    panelTabSize.add( this.spinnerTabSize );

    this.cbAsDefault = GUIFactory.createCheckBox(
					"Als Standardwert setzen" );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( this.cbAsDefault, gbc );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel();
    panelBtn.setLayout( new GridLayout( 1, 2, 5, 5 ) );

    this.btnApply = GUIFactory.createButton( EmuUtil.TEXT_APPLY );
    panelBtn.add( this.btnApply );

    this.btnClose = GUIFactory.createButtonClose();
    panelBtn.add( this.btnClose );

    gbc.insets.top = 5;
    gbc.gridy++;
    add( panelBtn, gbc );


    // Listener
    this.btnApply.addActionListener( this );
    this.btnClose.addActionListener( this );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
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
	  if( this.cbAsDefault.isSelected() ) {
	    Main.setProperty(
			TextEditFrm.PROP_TABSIZE,
			String.valueOf( tabSize ) );
	  }
	}
      }
    }
  }
}
