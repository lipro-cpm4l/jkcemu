/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Drehen eines Bildes
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.lang.*;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;


public class RotateDlg extends BaseDlg implements ChangeListener
{
  private static final int MIN_DEGREES = -180;
  private static final int MAX_DEGREES = 180;

  private static final Object[][] backgrounds = {
			{ "transparent", new Color( 128, 128, 128, 0) },
			{ "wei\u00DF",   Color.WHITE },
			{ "grau",        Color.GRAY },
			{ "schwarz",     Color.BLACK } };

  private static boolean suppressIndexedColorsMsg = false;
  private static int     lastBgColorIdx           = 0;

  private ImageFrm          imageFrm;
  private ImgFld            imgFld;
  private BufferedImage     appliedImg;
  private BufferedImage     orgImg;
  private BufferedImage     previewImg;
  private JSlider           slider;
  private JSpinner          spinner;
  private JComboBox<Object> comboBackground;
  private JButton           btnApply;
  private JButton           btnCancel;


  public static BufferedImage showDlg( ImageFrm imageFrm )
  {
    RotateDlg dlg = new RotateDlg( imageFrm );
    dlg.setVisible( true );
    return dlg.appliedImg;
  }


	/* --- ChangeListener --- */

  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( src == this.slider ) {
      this.spinner.removeChangeListener( this );
      this.spinner.setValue( this.slider.getValue() );
      this.spinner.addChangeListener( this );
      updPreview();
    } else if( src == this.spinner ) {
      this.slider.removeChangeListener( this );
      this.slider.setValue( Math.round( EmuUtil.getFloat( this.spinner ) ) );
      this.slider.addChangeListener( this );
      updPreview();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.comboBackground ) {
      rv = true;
      updPreview();
    }
    else if( src == this.btnCancel ) {
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

  private RotateDlg( ImageFrm imageFrm )
  {
    super( imageFrm, "Drehen" );
    this.imageFrm   = imageFrm;
    this.imgFld     = imageFrm.getImgFld();
    this.appliedImg = null;
    this.orgImg     = imgFld.getImage();
    this.previewImg = imgFld.getNewPreviewImage();


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


    // Spinner-Eingabefeld
    JPanel panelSpinner = new JPanel();
    panelSpinner.setLayout( new BoxLayout( panelSpinner, BoxLayout.X_AXIS ) );
    add( panelSpinner, gbc );

    panelSpinner.add( new JLabel( "Winkel:" ) );
    panelSpinner.add( Box.createHorizontalStrut( 5 ) );

    this.spinner = new JSpinner(
		new SpinnerNumberModel(
				0F,
				(float) MIN_DEGREES,
				(float) MAX_DEGREES,
				0.25F ) );
    panelSpinner.add( this.spinner );

    panelSpinner.add( Box.createHorizontalStrut( 5 ) );
    panelSpinner.add( new JLabel( "Grad" ) );


    // Schieberegler
    this.slider = new JSlider(
			SwingConstants.HORIZONTAL,
			MIN_DEGREES,
			MAX_DEGREES,
			0 );
    this.slider.setLabelTable(
		this.slider.createStandardLabels( MAX_DEGREES / 2 ) );
    this.slider.setPaintLabels( true );
    this.slider.setPaintTicks( true );
    this.slider.setPaintTrack( true );
    this.slider.setBorder( BorderFactory.createEtchedBorder() );
    gbc.weightx = 1.0;
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.gridy++;
    add( this.slider, gbc );


    // Hintergrund
    JPanel panelBackground = new JPanel();
    panelBackground.setLayout(
		new BoxLayout( panelBackground, BoxLayout.X_AXIS ) );
    gbc.insets.top = 10;
    gbc.weightx    = 0.0;
    gbc.fill       = GridBagConstraints.NONE;
    gbc.gridy++;
    add( panelBackground, gbc );

    panelBackground.add( new JLabel( "Hintergrund:" ) );
    panelBackground.add( Box.createHorizontalStrut( 5 ) );

    this.comboBackground = new JComboBox<>();
    this.comboBackground.setEditable( false );
    for( Object[] bg : backgrounds ) {
      this.comboBackground.addItem( bg[ 0 ] );
    }
    panelBackground.add( this.comboBackground );

    try {
      this.comboBackground.setSelectedIndex( lastBgColorIdx );
    }
    catch( IllegalArgumentException ex ) {}

    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnApply = new JButton( "Anwenden" );
    panelBtn.add( this.btnApply );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Bild anzeigen
    this.imgFld.setRotation( ImgFld.Rotation.NONE );
    updPreview();
    imageFrm.fireFitImage();


    // Listener
    this.slider.addChangeListener( this );
    this.spinner.addChangeListener( this );
    this.comboBackground.addActionListener( this );
    this.btnApply.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- private Methoden --- */

  private BufferedImage createRotatedImage( BufferedImage srcImg )
  {
    BufferedImage rotatedImg = null;
    if( srcImg != null ) {
      Color bgColor = null;
      int   bgIdx   = this.comboBackground.getSelectedIndex();
      if( (bgIdx >= 0) && (bgIdx < backgrounds.length) ) {
	Object o = backgrounds[ bgIdx ][ 1 ];
	if( o != null ) {
	  if( o instanceof Color ) {
	    bgColor = (Color) o;
	  }
	}
      }
      int imgType = BufferedImage.TYPE_INT_ARGB;
      if( bgColor != null ) {
	if( bgColor.getAlpha() > 128 ) {
	  imgType = BufferedImage.TYPE_3BYTE_BGR;
	}
      }
      if( bgColor == null ) {
	bgColor = ImgUtil.transparentColor;
      }
      IndexColorModel icm = ImgUtil.getIndexColorModel( srcImg );
      if( icm != null ) {
	/*
	 * Wenn die Hintergrundfarbe im IndexColorModel enthalten ist,
	 * kann es weiter verwendet werden.
	 */
	boolean found = false;
	int     n     = icm.getMapSize();
	for( int i = 0; i < n; i++ ) {
	  if( icm.getRGB( i ) == bgColor.getRGB() ) {
	    found = true;
	    break;
	  }
	}
	if( found ) {
	  imgType = (n > 16 ?
			BufferedImage.TYPE_BYTE_INDEXED
			: BufferedImage.TYPE_BYTE_BINARY);
	} else {
	  icm = null;
	}
      }
      int w      = srcImg.getWidth();
      int h      = srcImg.getHeight();
      int xOffs  = -(w / 2);
      int yOffs  = -(h / 2);
      int mOffs  = (int) Math.round(
			Math.sqrt( (double) ((w * w / 4) + (h * h / 4)) ) );
      int d      = 2 * mOffs;
      if( icm != null ) {
	rotatedImg = new BufferedImage( d, d, imgType, icm );
      } else {
	rotatedImg = new BufferedImage( d, d, imgType );
      }
      Graphics g = rotatedImg.createGraphics();
      if( bgColor != null ) {
	g.setColor( bgColor );
	g.fillRect( 0, 0, d, d );
      }
      if( g instanceof Graphics2D ) {
	g.translate( mOffs, mOffs );
	((Graphics2D) g).rotate( getRotateAngle() );
	g.drawImage( srcImg, xOffs, yOffs, this );
	g.dispose();
      }
    }
    return rotatedImg;
  }


  private void doApply()
  {
    BufferedImage rotatedImg = createRotatedImage( this.orgImg );
    if( (this.orgImg != null) && (rotatedImg != null) ) {
      /*
       * unnoetigen Rand entfernen,
       * dazu die Eckpunkte des gedrehten Rechtecks ermitteln
       */
      int w     = this.orgImg.getWidth();
      int h     = this.orgImg.getHeight();
      int xOffs = -(w / 2);
      int yOffs = -(h / 2);
      int mOffs = (int) Math.round(
		Math.sqrt( (double) ((w * w / 4) + (h * h / 4)) ) );
      double hypotenuse  = Math.sqrt( (double) ((w * w / 4) + (h * h / 4)) );
      double oldAngle    = Math.asin( (double) yOffs / hypotenuse );
      double rotateAngle = getRotateAngle();
      double newAngle0   = oldAngle - rotateAngle;
      double y0          = Math.sin( newAngle0 ) * hypotenuse;
      double x0          = Math.sqrt( (hypotenuse * hypotenuse) - (y0 * y0) );
      double newAngle1   = oldAngle + rotateAngle;
      double y1          = Math.sin( newAngle1 ) * hypotenuse;
      double x1          = Math.sqrt( (hypotenuse * hypotenuse) - (y1 * y1) );
      double x2          = Math.abs( x0 );
      double y2          = Math.abs( y0 );
      double x3          = Math.abs( x1 );
      double y3          = Math.abs( y1 );
      x0                 = -x2;
      y0                 = -y2;
      x1                 = -x3;
      y1                 = -y3;
      int newX           = (int) Math.round( Math.min( x0, x1 ) );
      int newY           = (int) Math.round( Math.min( y0, y1 ) );
      int newW           = (int) Math.round( Math.max( x2, x3 ) ) - newX;
      int newH           = (int) Math.round( Math.max( y2, y3 ) ) - newY;
      int whMax          = 2 * mOffs;
      newX += mOffs;
      newY += mOffs;
      if( newX < 0 ) {
	newX = 0;
      }
      if( newY < 0 ) {
	newY = 0;
      }
      if( newW > whMax ) {
	newX = whMax;
      }
      if( newY > whMax ) {
	newY = whMax;
      }
      this.appliedImg = rotatedImg.getSubimage( newX, newY, newW, newH );
      lastBgColorIdx  = this.comboBackground.getSelectedIndex();
      doClose();

      // Hinweis bzgl. indexiertem Farbmodell
      IndexColorModel orgICM = ImgUtil.getIndexColorModel( this.orgImg );
      if( (orgICM != null)
	  && (ImgUtil.getIndexColorModel( rotatedImg ) == null)
	  && !suppressIndexedColorsMsg )
      {
	StringBuilder buf = new StringBuilder( 512 );
	buf.append( "Das urspr\u00FCngliche Bild hat ein indexiertes"
		+ " Farbmodell mit " );
	buf.append( orgICM.getMapSize() );
	buf.append( " Farben.\n"
		+ "Durch die Drehung ist eine zus\u00E4tzliche Farbe"
		+ " f\u00FCr den Hintergrund hinzugekommen.\n"
		+ "Dadurch hat das gedrehte Bild kein indexiertes Farbmodel"
		+ " mehr.\n"
		+ "Falls Sie jedoch eins ben\u00F6tigen,"
		+ " k\u00F6nnen Sie mit dem Men\u00FCpunkt\n" );
	buf.append( this.imageFrm.getMenuPathTextReduceColors() );
	buf.append( "\nwieder eins erzeugen." );

	JCheckBox cb = new JCheckBox(
			"Diesen Hinweis zuk\u00FCnftig nicht mehr anzeigen",
			false );

	JOptionPane.showMessageDialog(
			this.imageFrm,
			new Object[] { buf.toString(), cb },
			"Hinweis",
			JOptionPane.INFORMATION_MESSAGE );
	suppressIndexedColorsMsg = cb.isSelected();
      }
    }
  }


  private double getRotateAngle()
  {
    return EmuUtil.getFloat( this.spinner ) / (double) MAX_DEGREES * Math.PI;
  }


  private void updPreview()
  {
    /*
     * Aus Performace-Gruenden soll das Vorschaubild
     * kein IndexColorModel haben.
     */
    BufferedImage rotatedImg = createRotatedImage( this.previewImg );
    if( rotatedImg != null ) {
      this.imgFld.setImage( rotatedImg );
      this.imgFld.repaint();
    }
  }
}
