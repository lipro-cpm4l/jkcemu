/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Skalieren eines Bildes
 */

package jkcemu.image;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.lang.*;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;


public class ScaleDlg extends BaseDlg implements ChangeListener
{
  private static boolean lastKeepColorTab = false;
  private static boolean lastKeepRatio    = true;

  private BufferedImage image;
  private Double        ratio;
  private boolean       rotated90Degrees;
  private BufferedImage scaledImage;
  private JSpinner      spinnerWidth;
  private JSpinner      spinnerHeight;
  private JSpinner      lastChangeSource;
  private JCheckBox     btnKeepRatio;
  private JButton       btnScale;
  private JButton       btnCancel;


  public static BufferedImage showDlg(
				ImageFrm      imageFrm,
				BufferedImage image )
  {
    ScaleDlg dlg = new ScaleDlg( imageFrm, image );
    dlg.setVisible( true );
    return dlg.scaledImage;
  }


	/* --- ChangeListener --- */

  public void stateChanged( ChangeEvent e )
  {
    if( this.ratio != null ) {
      if( this.btnKeepRatio.isSelected() ) {
	keepRatio( e.getSource() );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnScale ) {
      rv = true;
      doScale();
    }
    else if( src == this.btnCancel ) {
      rv = true;
      doClose();
    }
    else if( src == this.btnKeepRatio ) {
      rv = true;
      doKeepRatio();
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ScaleDlg( ImageFrm imageFrm, BufferedImage image )
  {
    super( imageFrm, "Skalieren" );
    this.image            = image;
    this.ratio            = null;
    this.scaledImage      = null;
    this.lastChangeSource = null;
    this.rotated90Degrees = imageFrm.getImgFld().isRotated90Degrees();


    // Bildgroesse
    int w = image.getWidth();
    int h = image.getHeight();
    if( (w > 0) && (h > 0) ) {
      if( this.rotated90Degrees ) {
	int m = w;
	w     = h;
	h     = m;
      }
      this.ratio = ((double) w / (double) h);
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

    add( new JLabel( "Neue Breite:" ), gbc );

    this.spinnerWidth = new JSpinner(
				new SpinnerNumberModel(
					w > 0 ? w : 640,
					1,
					Math.max( w, 9999 ),
					1 ) );
    gbc.insets.left = 0;
    gbc.gridx++;
    add( this.spinnerWidth, gbc );

    gbc.gridx++;
    add( new JLabel( "Pixel" ), gbc );

    gbc.insets.left = 5;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( new JLabel( "Neue H\u00F6he:" ), gbc );

    this.spinnerHeight = new JSpinner(
				new SpinnerNumberModel(
					h > 0 ? h : 480,
					1,
					Math.max( w, 9999 ),
					1 ) );
    gbc.insets.left = 0;
    gbc.gridx++;
    add( this.spinnerHeight, gbc );

    gbc.gridx++;
    add( new JLabel( "Pixel" ), gbc );

    this.btnKeepRatio = new JCheckBox( "Seitenverh\u00E4ltnis beibehalten" );
    if( this.ratio != null ) {
      this.btnKeepRatio.setSelected( lastKeepRatio );
    } else {
      this.btnKeepRatio.setEnabled( false );
    }
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.insets.left = 5;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.btnKeepRatio, gbc );

    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnScale = new JButton( "Skalieren" );
    panelBtn.add( this.btnScale );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Listener
    if( this.btnKeepRatio.isEnabled() ) {
      this.btnKeepRatio.addActionListener( this );
      this.spinnerWidth.addChangeListener( this );
      this.spinnerHeight.addChangeListener( this );
    }
    this.btnScale.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- private Methoden --- */

  private void doKeepRatio()
  {
    if( (this.ratio != null) && this.btnKeepRatio.isSelected() ) {
      keepRatio( this.lastChangeSource != null ?
			this.lastChangeSource : this.spinnerWidth );
    }
  }


  private void doScale()
  {
    int w = EmuUtil.getInt( this.spinnerWidth );
    int h = EmuUtil.getInt( this.spinnerHeight );
    if( (w > 0) && (h > 0) ) {
      if( this.rotated90Degrees ) {
	int m = w;
	w     = h;
	h     = m;
      }
      int imgType = this.image.getType();
      if( (imgType != BufferedImage.TYPE_BYTE_GRAY)
	  && (imgType != BufferedImage.TYPE_USHORT_GRAY) )
      {
	if( this.image.getTransparency() == Transparency.OPAQUE ) {
	  imgType = BufferedImage.TYPE_3BYTE_BGR;
	} else {
	  imgType = BufferedImage.TYPE_INT_ARGB;
	}
      }
      BufferedImage scaledImg = new BufferedImage( w, h, imgType );
      Graphics      g         = scaledImg.createGraphics();
      g.drawImage(
		this.image.getScaledInstance( w, h, Image.SCALE_SMOOTH ),
		0,
		0,
		this );
      g.dispose();
      this.scaledImage = scaledImg;
      if( this.btnKeepRatio.isEnabled() ) {
	lastKeepRatio = this.btnKeepRatio.isSelected();
      }
      doClose();
    }
  }


  private void keepRatio( Object source )
  {
    if( source == this.spinnerWidth ) {
      this.lastChangeSource = this.spinnerWidth;
      this.spinnerHeight.removeChangeListener( this );
      EmuUtil.setValue(
		this.spinnerHeight,
		(int) Math.round(
			(double) EmuUtil.getInt( this.spinnerWidth )
					/ this.ratio.doubleValue() ) );
      this.spinnerHeight.addChangeListener( this );
    }
    else if( source == this.spinnerHeight ) {
      this.lastChangeSource = this.spinnerHeight;
      this.spinnerWidth.removeChangeListener( this );
      EmuUtil.setValue(
		this.spinnerWidth,
		(int) Math.round(
			(double) EmuUtil.getInt( this.spinnerHeight )
					* this.ratio.doubleValue() ) );
      this.spinnerWidth.addChangeListener( this );
    }
  }
}
