/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Abrunden der Ecken eines Bildes
 */

package jkcemu.image;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.lang.*;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;


public class RoundCornersDlg extends BaseDlg
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

    add( new JLabel( "Obere Ecken:" ), gbc );

    this.spinnerTopPixels = new JSpinner(
			new SpinnerNumberModel(
				nTopPixels,
				0,
				ImgUtil.ROUND_PIXELS_MAX,
				1 ) );
    gbc.insets.left = 0;
    gbc.gridx++;
    add( this.spinnerTopPixels, gbc );

    gbc.gridx++;
    add( new JLabel( "Pixel" ), gbc );

    gbc.insets.top  = 0;
    gbc.insets.left = 5;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( new JLabel( "Untere Ecken:" ), gbc );

    this.spinnerBottomPixels = new JSpinner(
			new SpinnerNumberModel(
				nBottomPixels,
				0,
				ImgUtil.ROUND_PIXELS_MAX,
				1 ) );
    gbc.insets.left = 0;
    gbc.gridx++;
    add( this.spinnerBottomPixels, gbc );

    gbc.gridx++;
    add( new JLabel( "Pixel" ), gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.insets.top  = 10;
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
    this.numTopPixels    = EmuUtil.getInt( this.spinnerTopPixels );
    this.numBottomPixels = EmuUtil.getInt( this.spinnerBottomPixels );
    doClose();
  }
}

