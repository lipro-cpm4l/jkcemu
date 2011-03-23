/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Optionsdialog, bei dem die Optionen mit JRadioFields dargestellt werden
 * und somit zahlenmaessig mehr und textuell auch laenger sein koennen
 * als z.B. mit JOptionPane.
 */

package jkcemu.base;

import java.awt.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;


public class OptionDlg extends BasicDlg
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
	    add( new JLabel( msg.substring( pos, eol ) ), gbc );
	    pos = eol + 1;
	  } else {
	    add( new JLabel( msg.substring( pos ) ), gbc );
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
	JPanel panelOpt = new JPanel( new GridBagLayout() );
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
	  JRadioButton btn = new JRadioButton( options[ i ], optState );
	  grpOpt.add( btn );
	  btn.addActionListener( this );
	  this.optionBtns[ i ] = btn;
	  panelOpt.add( btn, gbcOpt );
	  gbcOpt.gridy++;
	}
      }
    }

    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.insets.top = 10;
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.setEnabled( okState );
    this.btnOK.addActionListener( this );
    this.btnOK.addKeyListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse
    pack();
    setResizable( false );
    setParentCentered();
  }
}

