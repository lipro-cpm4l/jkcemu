/*
 * (c) 2009-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Optionsdialog, bei dem die Optionen mit JRadioFields dargestellt werden
 * und somit zahlenmaessig mehr und textuell auch laenger sein koennen
 * als z.B. mit JOptionPane.
 */

package jkcemu.base;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


public class OptionDlg extends BaseDlg
{
  private int            selectedIdx;
  private JRadioButton[] optionBtns;
  private JButton        btnOK;
  private JButton        btnCancel;


  public static int showOptionDlg(
			Window    window,
			String    msg,
			String    title,
			int       preselectedIdx,
			String... options )
  {
    OptionDlg dlg = new OptionDlg(
				window,
				msg,
				title,
				preselectedIdx,
				options );
    dlg.setVisible( true );
    return dlg.selectedIdx;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.btnOK ) {
	  rv = true;
	  if( this.optionBtns != null ) {
	    for( int i = 0; i < this.optionBtns.length; i++ ) {
	      if( this.optionBtns[ i ].isSelected() ) {
		this.selectedIdx = i;
		doClose();
	      }
	    }
	  }
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  this.selectedIdx = -1;
	  doClose();
	}
	else if( src instanceof JRadioButton ) {
	  rv = true;
	  if( ((JRadioButton) src).isSelected() ) {
	    this.btnOK.setEnabled( true );
	  }
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
     if( this.optionBtns != null ) {
	for( JRadioButton rb : this.optionBtns ) {
	  rb.removeActionListener( this );
	}
      }
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


	/* --- private Konstruktoren --- */

  private OptionDlg(
		Window    owner,
		String    msg,
		String    title,
		int       preselectedIdx,
		String... options )
  {
    super( owner, title );
    this.selectedIdx = -1;


    // Fensterinhalt
    setLayout( new GridBagLayout() );
    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    // Text
    if( msg != null ) {
      int pos = 0;
      int len = msg.length();
      while( pos < len ) {
	int eol = msg.indexOf( '\n', pos );
	if( eol == pos ) {		// Leerzeile
	  gbc.insets.top += 12;
	  pos = eol + 1;
	} else {
	  if( eol > pos ) {
	    add( GUIFactory.createLabel( msg.substring( pos, eol ) ), gbc );
	    pos = eol + 1;
	  } else {
	    add( GUIFactory.createLabel( msg.substring( pos ) ), gbc );
	    pos = len;
	  }
	  gbc.insets.top = 0;
	  gbc.gridy++;
	}
      }
    }
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.top    = 15;
    gbc.insets.bottom = 5;

    // Optionen
    boolean okState = false;
    this.optionBtns = null;
    if( options != null ) {
      if( options.length > 0 ) {
	JPanel panelOpt = GUIFactory.createPanel( new GridBagLayout() );
	add( panelOpt, gbc );
	gbc.gridy++;

	ButtonGroup        grpOpt = new ButtonGroup();
	GridBagConstraints gbcOpt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );

	this.optionBtns = new JRadioButton[ options.length ];
	for( int i = 0; i < options.length; i++ ) {
	  boolean optState = false;
	  if( i == preselectedIdx ) {
	    optState = true;
	    okState  = true;
	  }
	  JRadioButton rb = GUIFactory.createRadioButton(
							options[ i ],
							optState );
	  grpOpt.add( rb );
	  this.optionBtns[ i ] = rb;
	  panelOpt.add( rb, gbcOpt );
	  gbcOpt.gridy++;
	}
      }
    }

    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.insets.top = 10;
    add( panelBtn, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    this.btnOK.setEnabled( okState );
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse
    pack();
    setResizable( false );
    setParentCentered();


    // Listener
    if( this.optionBtns != null ) {
      for( JRadioButton rb : this.optionBtns ) {
	rb.addActionListener( this );
      }
    }
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }
}
