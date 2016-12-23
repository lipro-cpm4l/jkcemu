/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Entfernen der Transparenz
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.lang.*;
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import jkcemu.base.BaseDlg;


public class RemoveTransparencyDlg extends BaseDlg
{
  private static int lastColorIdx = 0;

  private BufferedImage image;
  private BufferedImage appliedImage;
  private JRadioButton  btnKeepColor;
  private JRadioButton  btnToWhite;
  private JRadioButton  btnToGray;
  private JRadioButton  btnToBlack;
  private JButton       btnApply;
  private JButton       btnCancel;


  public static BufferedImage showDlg( Window owner, BufferedImage image )
  {
    RemoveTransparencyDlg dlg = new RemoveTransparencyDlg( owner, image );
    dlg.setVisible( true );
    return dlg.appliedImage;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnApply ) {
      rv = true;
      doApply();
    } else if( src == this.btnCancel ) {
      rv = true;
      doClose();
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private RemoveTransparencyDlg( Window owner, BufferedImage image )
  {
    super( owner, "Transparenz entfernen" );
    this.image        = image;
    this.appliedImage = null;


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


    add( new JLabel( "Transparente Bereiche f\u00FCllen mit:" ), gbc );

    ButtonGroup grpTransp = new ButtonGroup();

    this.btnKeepColor = new JRadioButton(
				"in den Pixeln gespeicherte Farbe",
				lastColorIdx == 0 );
    grpTransp.add( this.btnKeepColor );
    gbc.insets.left   = 50;
    gbc.insets.bottom = 0;
    gbc.gridy++;
    add( this.btnKeepColor, gbc );

    this.btnToWhite = new JRadioButton(
				"wei\u00DF",
				lastColorIdx == 1 );
    grpTransp.add( this.btnToWhite );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.btnToWhite, gbc );

    this.btnToGray = new JRadioButton(
				"grau",
				lastColorIdx == 2 );
    grpTransp.add( this.btnToGray );
    gbc.gridy++;
    add( this.btnToGray, gbc );

    this.btnToBlack = new JRadioButton(
				"schwarz",
				lastColorIdx == 3 );
    grpTransp.add( this.btnToBlack );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.btnToBlack, gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.insets.left = 5;
    gbc.insets.top  = 10;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnApply = new JButton( "OK" );
    panelBtn.add( this.btnApply );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
    this.btnApply.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- Aktionen --- */

  private void doApply()
  {
    int w = this.image.getWidth();
    int h = this.image.getHeight();
    if( (w > 0) && (h > 0) ) {
      BufferedImage tmpImg = new BufferedImage(
					w,
					h,
					BufferedImage.TYPE_3BYTE_BGR );
      Graphics g     = tmpImg.createGraphics();
      Color    color = null;
      if( this.btnToWhite.isSelected() ) {
	color        = Color.WHITE;
	lastColorIdx = 1;
      } else if( this.btnToGray.isSelected() ) {
	color        = Color.GRAY;
	lastColorIdx = 2;
      } else if( this.btnToBlack.isSelected() ) {
	color        = Color.BLACK;
	lastColorIdx = 3;
      } else {
	lastColorIdx = 0;
      }
      if( color != null ) {
	g.setColor( color );
	g.fillRect( 0, 0, w, h );
      }
      g.drawImage( this.image, 0, 0, this );
      g.dispose();
      this.appliedImage = tmpImg;
      doClose();
    }
  }
}
