/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Einstellung eines Schwellwertes
 * zwecks Umwandlung eines Bildes in monochrom
 */

package jkcemu.image;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.lang.*;
import java.util.EventObject;
import java.util.Hashtable;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;


public class ThresholdDlg extends BaseDlg implements ChangeListener
{
  private ImgFld        imgFld;
  private BufferedImage appliedImg;
  private BufferedImage grayImg;
  private int           wImg;
  private int           hImg;
  private byte[]        palette;
  private JSlider       slider;
  private JButton       btnApply;
  private JButton       btnCancel;


  public static BufferedImage showDlg( ImageFrm imageFrm )
  {
    ThresholdDlg dlg = new ThresholdDlg( imageFrm );
    dlg.setVisible( true );
    return dlg.appliedImg;
  }


	/* --- ChangeListener --- */

  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( src == this.slider ) {
      sliderChanged();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnCancel ) {
      rv = true;
      doClose();
    }
    else if( src == this.btnApply ) {
      rv = true;
      doApply();
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ThresholdDlg( ImageFrm imageFrm )
  {
    super( imageFrm, "Schwellwert" );
    this.imgFld     = imageFrm.getImgFld();
    this.palette    = new byte[ 256 ];
    this.appliedImg = null;
    this.grayImg    = null;

    BufferedImage orgImg = imgFld.getImage();
    if( orgImg != null ) {
      this.wImg = orgImg.getWidth();
      this.hImg = orgImg.getHeight();
      if( (this.wImg > 0) && (this.hImg > 0) ) {
	this.grayImg = new BufferedImage(
				this.wImg,
				this.hImg,
				BufferedImage.TYPE_BYTE_INDEXED,
				ImgUtil.getColorModelSortedGray() );
	Graphics g = this.grayImg.createGraphics();
	g.drawImage( orgImg, 0, 0, this );
	g.dispose();
      }
    }


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    // Schieberegler
    this.slider = new JSlider(
			SwingConstants.HORIZONTAL,
			0,
			256,
			128 );
    add( this.slider, gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.weightx     = 0.0;
    gbc.fill        = GridBagConstraints.NONE;
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


    // Bild anzeigen
    sliderChanged();

    // Listener
    this.slider.addChangeListener( this );
    this.btnApply.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    BufferedImage visibleImg = this.imgFld.getImage();
    if( visibleImg != null ) {
      this.appliedImg = new BufferedImage(
				visibleImg.getWidth(),
				visibleImg.getHeight(),
				BufferedImage.TYPE_BYTE_BINARY,
				ImgUtil.getColorModelBW() );
      Graphics g = this.appliedImg.createGraphics();
      g.drawImage( visibleImg, 0, 0, this );
      g.dispose();
    }
    doClose();
  }


  private void sliderChanged()
  {
    if( this.grayImg != null ) {
      int value = 256 - this.slider.getValue();
      for( int i = 0; i < this.palette.length; i++ ) {
	this.palette[ i ] = (byte) (i < value ? 0 : 0xFF);
      }
      this.imgFld.setImage(
			new BufferedImage(
				new IndexColorModel(
					8,
					this.palette.length,
					this.palette,
					this.palette,
					this.palette ),
				this.grayImg.getRaster(),
				false,
				new Hashtable<>() ) );
      this.imgFld.repaint();
    }
  }
}
