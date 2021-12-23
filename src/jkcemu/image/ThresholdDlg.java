/*
 * (c) 2016-2020 Jens Mueller
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
import java.awt.event.WindowEvent;
import java.util.EventObject;
import java.util.Hashtable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;


public class ThresholdDlg extends BaseDlg implements ChangeListener
{
  private ImageFld      imgFld;
  private BufferedImage appliedImg;
  private BufferedImage grayImg;
  private int           wImg;
  private int           hImg;
  private byte[]        palette;
  private JSlider       slider;
  private JButton       btnApply;
  private JButton       btnCancel;
  private JButton       btnFastLeft;
  private JButton       btnFastRight;
  private JButton       btnLeft;
  private JButton       btnRight;


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
    if( src == this.btnApply ) {
      rv = true;
      doApply();
    }
    else if( src == this.btnCancel ) {
      rv = true;
      doClose();
    }
    else if( src == this.btnFastLeft ) {
      rv = true;
      moveSlider( -10 );
    }
    else if( src == this.btnLeft ) {
      rv = true;
      moveSlider( -1 );
    }
    else if( src == this.btnFastRight ) {
      rv = true;
      moveSlider( 10 );
    }
    else if( src == this.btnRight ) {
      rv = true;
      moveSlider( 1 );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.btnFastLeft.removeActionListener( this );
      this.btnLeft.removeActionListener( this );
      this.btnRight.removeActionListener( this );
      this.btnFastRight.removeActionListener( this );
      this.btnApply.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (this.slider != null) && (e.getComponent() == this) )
      this.slider.requestFocus();
  }


	/* --- Konstruktor --- */

  private ThresholdDlg( ImageFrm imageFrm )
  {
    super( imageFrm, "Schwellwert" );
    this.imgFld     = imageFrm.getImageFld();
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
				ImageUtil.getColorModelSortedGray() );
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
					0.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    JPanel panelSlider = GUIFactory.createPanel();
    panelSlider.setLayout( new BoxLayout( panelSlider, BoxLayout.X_AXIS ) );
    add( panelSlider, gbc );

    // Schaltflaechen nach links
    this.btnFastLeft = GUIFactory.createRelImageResourceButton(
					this,
					"nav/left2.png",
					"Schnell nach links" );
    this.btnFastLeft.setMargin( new Insets( 0, 0, 0, 0 ) );
    panelSlider.add( this.btnFastLeft );
    panelSlider.add( Box.createHorizontalStrut( 5 ) );

    this.btnLeft = GUIFactory.createRelImageResourceButton(
					this,
					"nav/left1.png",
					"Nach links" );
    this.btnLeft.setMargin( new Insets( 0, 0, 0, 0 ) );
    panelSlider.add( this.btnLeft );
    panelSlider.add( Box.createHorizontalStrut( 5 ) );


    // Schieberegler
    this.slider = GUIFactory.createSlider(
			SwingConstants.HORIZONTAL,
			0,
			256,
			128 );
    this.slider.addChangeListener( this );
    panelSlider.add( this.slider );
    panelSlider.add( Box.createHorizontalStrut( 5 ) );


    // Schaltflaechen nach rechts
    this.btnRight = GUIFactory.createRelImageResourceButton(
					this,
					"nav/right1.png",
					"Nach rechts" );
    this.btnRight.setMargin( new Insets( 0, 0, 0, 0 ) );
    panelSlider.add( this.btnRight );
    panelSlider.add( Box.createHorizontalStrut( 5 ) );

    this.btnFastRight = GUIFactory.createRelImageResourceButton(
					this,
					"nav/right2.png",
					"Schnell nach rechts" );
    this.btnFastRight.setMargin( new Insets( 0, 0, 0, 0 ) );
    panelSlider.add( this.btnFastRight );


    // sonstige Schaltflaechen
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.insets.top  = 10;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnApply = GUIFactory.createButtonOK();
    panelBtn.add( this.btnApply );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Bild anzeigen
    sliderChanged();


    // Listener
    this.btnFastLeft.addActionListener( this );
    this.btnLeft.addActionListener( this );
    this.btnRight.addActionListener( this );
    this.btnFastRight.addActionListener( this );
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
				ImageUtil.getColorModelBW() );
      Graphics g = this.appliedImg.createGraphics();
      g.drawImage( visibleImg, 0, 0, this );
      g.dispose();
    }
    doClose();
  }


  private void moveSlider( int diffValue )
  {
    int value = this.slider.getValue() + diffValue;
    value     = Math.max( value, this.slider.getMinimum() );
    value     = Math.min( value, this.slider.getMaximum() );
    this.slider.setValue( value );
  }


  private void sliderChanged()
  {
    int sliderValue = this.slider.getValue();
    if( this.grayImg != null ) {
      int value = 256 - sliderValue;
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

    boolean state = (sliderValue > this.slider.getMinimum());
    this.btnFastLeft.setEnabled( state );
    this.btnLeft.setEnabled( state );

    state = (sliderValue < this.slider.getMaximum());
    this.btnFastRight.setEnabled( state );
    this.btnRight.setEnabled( state );
  }
}
