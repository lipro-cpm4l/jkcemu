/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Abrunden von Ecken eines Bildes
 */

package jkcemu.image;

import java.awt.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.BasicDlg;


public class RoundCornersDlg extends BasicDlg
{
  private int      numTopPixels;
  private int      numBottomPixels;
  private JSpinner spinnerTopPixels;
  private JSpinner spinnerBottomPixels;
  private JButton  btnOK;
  private JButton  btnCancel;


  public RoundCornersDlg(
			Window owner,
			int    nTopPixels,
			int    nBottomPixels )
  {
    super( owner, "Ecken abrunden" );
    this.numTopPixels    = 0;
    this.numBottomPixels = 0;

    if( nTopPixels < 0 ) {
      nTopPixels = 0;
    } else if( nTopPixels > ImgUtil.ROUND_PIXELS_MAX ) {
      nTopPixels = ImgUtil.ROUND_PIXELS_MAX;
    }
    if( nBottomPixels < 0 ) {
      nBottomPixels = 0;
    } else if( nBottomPixels > ImgUtil.ROUND_PIXELS_MAX ) {
      nBottomPixels = ImgUtil.ROUND_PIXELS_MAX;
    }


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

    add( new Label( "Obere Ecken:" ), gbc );

    this.spinnerTopPixels = new JSpinner(
			new SpinnerNumberModel(
				nTopPixels,
				0,
				ImgUtil.ROUND_PIXELS_MAX,
				1 ) );
    gbc.gridx++;
    add( this.spinnerTopPixels, gbc );

    gbc.gridx++;
    add( new Label( "Pixel" ), gbc );

    gbc.insets.top = 0;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( new Label( "Untere Ecken:" ), gbc );

    this.spinnerBottomPixels = new JSpinner(
			new SpinnerNumberModel(
				nBottomPixels,
				0,
				ImgUtil.ROUND_PIXELS_MAX,
				1 ) );
    gbc.gridx++;
    add( this.spinnerBottomPixels, gbc );

    gbc.gridx++;
    add( new Label( "Pixel" ), gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


  public int getNumTopPixels()
  {
    return this.numTopPixels;
  }


  public int getNumBottomPixels()
  {
    return this.numBottomPixels;
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
	  doApply();
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
  }


	/* --- private Konstruktoren und Methoden --- */

  private void doApply()
  {
    this.numTopPixels    = getInt( this.spinnerTopPixels );
    this.numBottomPixels = getInt( this.spinnerBottomPixels );
    doClose();
  }


  private static int getInt( JSpinner spinner )
  {
    int rv = 0;
    Object o = spinner.getValue();
    if( o != null ) {
      if( o instanceof Number ) {
	rv = ((Number) o).intValue();
      }
    }
    return rv;
  }
}

