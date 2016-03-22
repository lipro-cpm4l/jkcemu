/*
 * (c) 2010-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Eingabe der Einruecktiefe
 */

package jkcemu.text;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.base.BasicDlg;


public class ReplyShiftWidthDlg extends BasicDlg
{
  private TextEditFrm textEditFrm;
  private JSpinner    spinnerShiftWidth;
  private JCheckBox   tglUseTabs;
  private JButton     btnApply;
  private JButton     btnClose;


  public ReplyShiftWidthDlg( TextEditFrm textEditFrm )
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

    JPanel panelShiftWidth = new JPanel( new FlowLayout( FlowLayout.CENTER ) );
    add( panelShiftWidth, gbc );
    panelShiftWidth.add( new JLabel( "Einr\u00FCcktiefe:" ) );

    int shiftWidth = this.textEditFrm.getShiftWidth();
    if( shiftWidth < 1 ) {
      shiftWidth = 1;
    } else if( shiftWidth > 99 ) {
      shiftWidth = 99;
    }
    this.spinnerShiftWidth = new JSpinner(
			new SpinnerNumberModel( shiftWidth, 1, 99, 1 ) );
    panelShiftWidth.add( this.spinnerShiftWidth );

    this.tglUseTabs = new JCheckBox(
				"Tabulatoren verwenden",
				this.textEditFrm.getShiftUseTabs() );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( this.tglUseTabs, gbc );

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
    if( (e.getWindow() == this) && (this.spinnerShiftWidth != null) ) {
      this.spinnerShiftWidth.requestFocus();
      Component c = this.spinnerShiftWidth.getEditor();
      if( c != null ) {
	if( c instanceof JTextComponent )
	  ((JTextComponent) c).selectAll();
      }
    }
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
	  boolean useTabs = this.tglUseTabs.isSelected();
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

