/*
 * (c) 2010-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Eingabe der Einruecktiefe
 */

package jkcemu.text;

import java.awt.Component;
import java.awt.FlowLayout;
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
import javax.swing.SpinnerNumberModel;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class ReplyShiftWidthDlg extends BaseDlg
{
  private TextEditFrm textEditFrm;
  private JSpinner    spinnerShiftWidth;
  private JCheckBox   cbUseTabs;
  private JButton     btnApply;
  private JButton     btnClose;


  public static void showDlg( TextEditFrm textEditFrm )
  {
    (new ReplyShiftWidthDlg( textEditFrm )).setVisible( true );
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
    if( (e.getWindow() == this) && (this.spinnerShiftWidth != null) ) {
      this.spinnerShiftWidth.requestFocus();
      Component c = this.spinnerShiftWidth.getEditor();
      if( c != null ) {
	if( c instanceof JTextComponent )
	  ((JTextComponent) c).selectAll();
      }
    }
  }


	/* --- Konstruktor --- */

  private ReplyShiftWidthDlg( TextEditFrm textEditFrm )
  {
    super( textEditFrm, "Einr\u00FCcktiefe \u00E4ndern" );
    this.textEditFrm = textEditFrm;


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

    JPanel panelShiftWidth = GUIFactory.createPanel(
				new FlowLayout( FlowLayout.CENTER ) );
    add( panelShiftWidth, gbc );
    panelShiftWidth.add( GUIFactory.createLabel( "Einr\u00FCcktiefe:" ) );

    int shiftWidth = this.textEditFrm.getShiftWidth();
    if( shiftWidth < 1 ) {
      shiftWidth = 1;
    } else if( shiftWidth > 99 ) {
      shiftWidth = 99;
    }
    this.spinnerShiftWidth = GUIFactory.createSpinner(
			new SpinnerNumberModel( shiftWidth, 1, 99, 1 ) );
    panelShiftWidth.add( this.spinnerShiftWidth );

    this.cbUseTabs = GUIFactory.createCheckBox(
				"Tabulatoren verwenden",
				this.textEditFrm.getShiftUseTabs() );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( this.cbUseTabs, gbc );


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


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Listener
    this.btnApply.addActionListener( this );
    this.btnClose.addActionListener( this );
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    Object value = this.spinnerShiftWidth.getValue();
    if( value != null ) {
      if( value instanceof Number ) {
	int shiftWidth = ((Number) value).intValue();
	if( shiftWidth > 0 ) {
	  this.textEditFrm.setShiftWidth( shiftWidth );
	  Main.setProperty(
			TextEditFrm.PROP_SHIFT_WIDTH,
			value.toString() );
	  boolean useTabs = this.cbUseTabs.isSelected();
	  this.textEditFrm.setShiftUseTabs( useTabs );
	  Main.setProperty(
			TextEditFrm.PROP_SHIFT_USE_TABS,
			Boolean.toString( useTabs ) );
	  doClose();
	}
      }
    }
  }
}
