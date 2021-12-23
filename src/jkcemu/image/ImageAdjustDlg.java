/*
 * (c) 2016-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Einstellung von Helligkeit, Kontrast und und der Farben
 */

package jkcemu.image;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.EventObject;
import java.util.Hashtable;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.CancelableProgressDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class ImageAdjustDlg
			extends BaseDlg
			implements
				CancelableProgressDlg.Progressable,
				ChangeListener,
				Runnable
{
  private static final int   VALUE_MAX       = 100;
  private static final float FLOAT_VALUE_MAX = (float) VALUE_MAX;

  private ImageFld              imageFld;
  private CancelableProgressDlg progressDlg;
  private int                   progressValue;
  private int                   appliedBrightness;
  private float                 appliedContrast;
  private float                 appliedSaturation;
  private int                   appliedDiffR;
  private int                   appliedDiffG;
  private int                   appliedDiffB;
  private BufferedImage         appliedImg;
  private BufferedImage         orgImg;
  private BufferedImage         srcImg;
  private int                   wImg;
  private int                   hImg;
  private int                   srcColorCnt;
  private byte[]                srcReds;
  private byte[]                srcGreens;
  private byte[]                srcBlues;
  private byte[]                srcAlphas;
  private byte[]                previewReds;
  private byte[]                previewGreens;
  private byte[]                previewBlues;
  private JSlider               sliderBrightness;
  private JSlider               sliderContrast;
  private JSlider               sliderSaturation;
  private JSlider               sliderRed;
  private JSlider               sliderGreen;
  private JSlider               sliderBlue;
  private JSpinner              spinnerBrightness;
  private JSpinner              spinnerContrast;
  private JSpinner              spinnerSaturation;
  private JSpinner              spinnerRed;
  private JSpinner              spinnerGreen;
  private JSpinner              spinnerBlue;
  private JButton               btnApply;
  private JButton               btnReset;
  private JButton               btnCancel;
  private boolean               previewUpdEnabled;


  public static BufferedImage showDlg( ImageFrm imageFrm )
  {
    ImageFld      imageFld   = imageFrm.getImageFld();
    BufferedImage retImg     = null;
    BufferedImage previewImg = imageFld.getNewPreviewImage();
    if( previewImg != null ) {
      if( ImageUtil.getIndexColorModel( previewImg ) == null ) {
	previewImg = ColorReducer.work(
				imageFrm,
				"Erzeuge Vorschaubild...",
				previewImg,
				256,
				imageFld.getBackground(),
				Dithering.Algorithm.FLOYD_STEINBERG );
      }
      if( previewImg != null ) {
	ImageAdjustDlg dlg = new ImageAdjustDlg(
					imageFrm,
					previewImg,
					imageFld );
	dlg.setVisible( true );
	retImg = dlg.appliedImg;
      }
    }
    return retImg;
  }


	/* --- CancelableProgressDlg.Progressable --- */

  @Override
  public int getProgressMax()
  {
    return this.wImg * this.hImg;
  }

  @Override
  public int getProgressValue()
  {
    return this.progressValue;
  }


	/* --- ChangeListener --- */

  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( src == this.sliderBrightness ) {
      this.spinnerBrightness.removeChangeListener( this );
      this.spinnerBrightness.setValue( this.sliderBrightness.getValue() );
      this.spinnerBrightness.addChangeListener( this );
      updPreview();
    }
    else if( src == this.sliderContrast ) {
      this.spinnerContrast.removeChangeListener( this );
      this.spinnerContrast.setValue( this.sliderContrast.getValue() );
      this.spinnerContrast.addChangeListener( this );
      updPreview();
    }
    else if( src == this.sliderSaturation ) {
      this.spinnerSaturation.removeChangeListener( this );
      this.spinnerSaturation.setValue( this.sliderSaturation.getValue() );
      this.spinnerSaturation.addChangeListener( this );
      updPreview();
    }
    else if( src == this.sliderRed ) {
      this.spinnerRed.removeChangeListener( this );
      this.spinnerRed.setValue( this.sliderRed.getValue() );
      this.spinnerRed.addChangeListener( this );
      updPreview();
    }
    else if( src == this.sliderGreen ) {
      this.spinnerGreen.removeChangeListener( this );
      this.spinnerGreen.setValue( this.sliderGreen.getValue() );
      this.spinnerGreen.addChangeListener( this );
      updPreview();
    }
    else if( src == this.sliderBlue ) {
      this.spinnerBlue.removeChangeListener( this );
      this.spinnerBlue.setValue( this.sliderBlue.getValue() );
      this.spinnerBlue.addChangeListener( this );
      updPreview();
    }
    else if( src == this.spinnerBrightness ) {
      this.sliderBrightness.removeChangeListener( this );
      this.sliderBrightness.setValue(
			EmuUtil.getInt( this.spinnerBrightness ) );
      this.sliderBrightness.addChangeListener( this );
      updPreview();
    }
    else if( src == this.spinnerContrast ) {
      this.sliderContrast.removeChangeListener( this );
      this.sliderContrast.setValue( EmuUtil.getInt( this.spinnerContrast ) );
      this.sliderContrast.addChangeListener( this );
      updPreview();
    }
    else if( src == this.spinnerSaturation ) {
      this.sliderSaturation.removeChangeListener( this );
      this.sliderSaturation.setValue(
			EmuUtil.getInt( this.spinnerSaturation ) );
      this.sliderSaturation.addChangeListener( this );
      updPreview();
    }
    else if( src == this.spinnerRed ) {
      this.sliderRed.removeChangeListener( this );
      this.sliderRed.setValue( EmuUtil.getInt( this.spinnerRed ) );
      this.sliderRed.addChangeListener( this );
      updPreview();
    }
    else if( src == this.spinnerGreen ) {
      this.sliderGreen.removeChangeListener( this );
      this.sliderGreen.setValue( EmuUtil.getInt( this.spinnerGreen ) );
      this.sliderGreen.addChangeListener( this );
      updPreview();
    }
    else if( src == this.spinnerBlue ) {
      this.sliderBlue.removeChangeListener( this );
      this.sliderBlue.setValue( EmuUtil.getInt( this.spinnerBlue ) );
      this.sliderBlue.addChangeListener( this );
      updPreview();
    }
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    if( (this.orgImg != null) && (this.progressDlg != null) ) {
      try {
	BufferedImage newImg = new BufferedImage(
		this.wImg,
		this.hImg,
		this.orgImg.getTransparency() == Transparency.OPAQUE ?
					BufferedImage.TYPE_3BYTE_BGR
					: BufferedImage.TYPE_INT_ARGB );
	this.progressValue = 0;
	for( int y = 0; y < this.hImg; y++ ) {
	  for( int x = 0; x < this.wImg; x++ ) {
	    if( this.progressDlg.wasCancelled() ) {
	      break;
	    }
	    int argb = this.orgImg.getRGB( x, y );
	    newImg.setRGB(
		x,
		y,
		(argb & 0xFF000000) | convertRGB(
					(argb >> 16) & 0xFF,
					(argb >> 8) & 0xFF,
					argb & 0xFF,
					this.appliedBrightness,
					this.appliedContrast,
					this.appliedSaturation,
					this.appliedDiffR,
					this.appliedDiffG,
					this.appliedDiffB ) & 0x00FFFFFF );
	    this.progressValue++;
	  }
	}
	if( !this.progressDlg.wasCancelled() ) {
	  this.appliedImg = newImg;
	}
      }
      finally {
	this.progressDlg.fireProgressFinished();
      }
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
    if( src == this.btnCancel ) {
      rv = true;
      doClose();
    }
    else if( src == this.btnReset ) {
      rv = true;
      doReset();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.sliderBrightness.removeChangeListener( this );
      this.sliderContrast.removeChangeListener( this );
      this.sliderSaturation.removeChangeListener( this );
      this.sliderRed.removeChangeListener( this );
      this.sliderGreen.removeChangeListener( this );
      this.sliderBlue.removeChangeListener( this );
      this.btnApply.removeActionListener( this );
      this.btnReset.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ImageAdjustDlg(
		ImageFrm      imageFrm,
		BufferedImage srcImg,
		ImageFld      imageFld )
  {
    super( imageFrm, "Helligkeit, Kontrast, Farben" );
    this.imageFld          = imageFld;
    this.progressDlg       = null;
    this.progressValue     = 0;
    this.appliedBrightness = 0;
    this.appliedContrast   = 0F;
    this.appliedSaturation = 0F;
    this.appliedDiffR      = 0;
    this.appliedDiffG      = 0;
    this.appliedDiffB      = 0;
    this.appliedImg        = null;
    this.srcImg            = null;
    this.srcReds           = null;
    this.srcGreens         = null;
    this.srcBlues          = null;
    this.srcColorCnt       = 0;
    this.wImg              = 0;
    this.hImg              = 0;
    this.previewUpdEnabled = true;
    this.orgImg            = imageFrm.getImage();
    if( this.orgImg != null ) {
      this.wImg = this.orgImg.getWidth();
      this.hImg = this.orgImg.getHeight();
    }
    if( srcImg != null ) {
      IndexColorModel icm = ImageUtil.getIndexColorModel( srcImg );
      if( icm != null ) {
	this.srcColorCnt    = icm.getMapSize();
	this.srcReds        = new byte[ this.srcColorCnt ];
	this.srcGreens      = new byte[ this.srcColorCnt ];
	this.srcBlues       = new byte[ this.srcColorCnt ];
	this.srcAlphas      = new byte[ this.srcColorCnt ];
	icm.getReds( this.srcReds );
	icm.getGreens( this.srcGreens );
	icm.getBlues( this.srcBlues );
	icm.getAlphas( this.srcAlphas );
	this.previewReds   = new byte[ this.srcColorCnt ];
	this.previewGreens = new byte[ this.srcColorCnt ];
	this.previewBlues  = new byte[ this.srcColorCnt ];
	this.srcImg        = srcImg;
      }
    }

    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.sliderBrightness = createSlider();
    add( this.sliderBrightness, gbc );

    this.sliderContrast = createSlider();
    gbc.gridx++;
    add( this.sliderContrast, gbc );

    this.sliderSaturation = createSlider();
    gbc.gridx++;
    add( this.sliderSaturation, gbc );

    this.sliderRed  = createSlider();
    gbc.insets.left = 20;
    gbc.gridx++;
    add( this.sliderRed, gbc );

    this.sliderGreen = createSlider();
    gbc.insets.left  = 5;
    gbc.gridx++;
    add( this.sliderGreen, gbc );

    this.sliderBlue = createSlider();
    gbc.gridx++;
    add( this.sliderBlue, gbc );

    this.spinnerBrightness = createSpinner();
    gbc.fill               = GridBagConstraints.HORIZONTAL;
    gbc.gridx              = 0;
    gbc.gridy++;
    add( this.spinnerBrightness, gbc );

    this.spinnerContrast = createSpinner();
    gbc.gridx++;
    add( this.spinnerContrast, gbc );

    this.spinnerSaturation = createSpinner();
    gbc.gridx++;
    add( this.spinnerSaturation, gbc );

    this.spinnerRed = createSpinner();
    gbc.insets.left = 20;
    gbc.gridx++;
    add( this.spinnerRed, gbc );

    this.spinnerGreen = createSpinner();
    gbc.insets.left   = 5;
    gbc.gridx++;
    add( this.spinnerGreen, gbc );

    this.spinnerBlue = createSpinner();
    gbc.gridx++;
    add( this.spinnerBlue, gbc );

    gbc.weighty = 0.0;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.gridx   = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Helligkeit" ), gbc );

    gbc.gridx++;
    add( GUIFactory.createLabel( "Kontrast" ), gbc );

    gbc.gridx++;
    add( GUIFactory.createLabel( "Farbs\u00E4ttigung" ), gbc );

    gbc.insets.left = 20;
    gbc.gridx++;
    add( GUIFactory.createLabel( "Rot" ), gbc );

    gbc.insets.left = 5;
    gbc.gridx++;
    add( GUIFactory.createLabel( "Gr\u00FCn" ), gbc );

    gbc.gridx++;
    add( GUIFactory.createLabel( "Blau" ), gbc );


    // Knoepfe
    JPanel panelBtn   = GUIFactory.createPanel(
					new GridLayout( 1, 3, 5, 5 ) );
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnApply = GUIFactory.createButtonApply();
    panelBtn.add( this.btnApply );

    this.btnReset = GUIFactory.createButtonReset();
    panelBtn.add( this.btnReset );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Bild anzeigen
    updPreview();
    imageFrm.fireFitImage();


    // Listener
    this.sliderBrightness.addChangeListener( this );
    this.sliderContrast.addChangeListener( this );
    this.sliderSaturation.addChangeListener( this );
    this.sliderRed.addChangeListener( this );
    this.sliderGreen.addChangeListener( this );
    this.sliderBlue.addChangeListener( this );
    this.btnApply.addActionListener( this );
    this.btnReset.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- private Methoden --- */

  private int convertRGB(
			int   r,
			int   g,
			int   b,
			int   brightness,
			float contrast,
			float saturation,
			int   diffR,
			int   diffG,
			int   diffB )
  {
    r += (brightness + diffR);
    g += (brightness + diffG);
    b += (brightness + diffB);

    // Farbsaettigung
    int m = (r + b + g) / 3;
    r += Math.round( (float) (r - m) * saturation / FLOAT_VALUE_MAX );
    g += Math.round( (float) (g - m) * saturation / FLOAT_VALUE_MAX );
    b += Math.round( (float) (b - m) * saturation / FLOAT_VALUE_MAX );

    // Kontrast
    r += Math.round( (float) (r - 128) * contrast / FLOAT_VALUE_MAX );
    g += Math.round( (float) (g - 128) * contrast / FLOAT_VALUE_MAX );
    b += Math.round( (float) (b - 128) * contrast / FLOAT_VALUE_MAX );

    // neuer Wert fuer rot pruefen
    if( r < 0 ) {
      r = 0;
    } else if( r > 255 ) {
      r = 255;
    }

    // neuer Wert fuer gruen pruefen
    if( g < 0 ) {
      g = 0;
    } else if( g > 255 ) {
      g = 255;
    }

    // neuer Wert fuer blau pruefen
    if( b < 0 ) {
      b = 0;
    } else if( b > 255 ) {
      b = 255;
    }

    // RGB zusammenbauen
    return ((r << 16) & 0xFF0000) | ((g << 8) & 0x00FF00) | (b & 0x0000FF);
  }


  private JSlider createSlider()
  {
    JSlider slider = GUIFactory.createSlider(
			SwingConstants.VERTICAL,
			-VALUE_MAX,
			VALUE_MAX,
			0 );
    slider.setLabelTable( slider.createStandardLabels( 50 ) );
    slider.setPaintLabels( true );
    slider.setPaintTicks( true );
    slider.setPaintTrack( true );
    slider.setBorder( BorderFactory.createEtchedBorder() );
    return slider;
  }


  private JSpinner createSpinner()
  {
    return GUIFactory.createSpinner(
	new SpinnerNumberModel( 0, -VALUE_MAX, VALUE_MAX, 1 ) );
  }


  private void doApply()
  {
    if( (this.orgImg != null)
	&& (this.srcReds != null)
	&& (this.srcGreens != null)
	&& (this.srcBlues != null)
	&& (this.srcAlphas != null) )
    {
      this.appliedBrightness   = this.sliderBrightness.getValue();
      this.appliedContrast    = getContrast();
      this.appliedSaturation  = (float) this.sliderSaturation.getValue();
      this.appliedDiffR       = this.sliderRed.getValue();
      this.appliedDiffG       = this.sliderGreen.getValue();
      this.appliedDiffB       = this.sliderBlue.getValue();
      int             nColors = 0;
      IndexColorModel orgICM  = ImageUtil.getIndexColorModel( this.orgImg );
      if( orgICM != null ) {
	nColors = orgICM.getMapSize();
      }
      if( (nColors > 0) && (nColors <= this.srcColorCnt) ) {
	byte[] reds   = new byte[ this.srcColorCnt ];
	byte[] greens = new byte[ this.srcColorCnt ];
	byte[] blues  = new byte[ this.srcColorCnt ];
	for( int i = 0; i < this.srcColorCnt; i++ ) {
	  int rgb = convertRGB(
			(int) this.srcReds[ i ] & 0xFF,
			(int) this.srcGreens[ i ] & 0xFF,
			(int) this.srcBlues[ i ] & 0xFF,
			this.appliedBrightness,
			this.appliedContrast,
			this.appliedSaturation,
			this.appliedDiffR,
			this.appliedDiffG,
			this.appliedDiffB );
	  reds[ i ]   = (byte) (rgb >> 16);
	  greens[ i ] = (byte) (rgb >> 8);
	  blues[ i ]  = (byte) rgb;
	}
	this.appliedImg = new BufferedImage(
				new IndexColorModel(
					8,
					this.srcColorCnt,
					reds,
					greens,
					blues ),
				this.srcImg.getRaster(),
				false,
				new Hashtable<>() );
	doClose();
      } else {
	doClose();
	this.progressDlg = new CancelableProgressDlg(
					getOwner(),
					"Einstellungen anwenden...",
					this );
	(new Thread(
		Main.getThreadGroup(),
		this,
		"JKCEMU image adjuster" )).start();
	this.progressDlg.setVisible( true );
      }
    }
  }


  private void doReset()
  {
    this.previewUpdEnabled = false;
    this.sliderBrightness.setValue( 0 );
    this.sliderContrast.setValue( 0 );
    this.sliderSaturation.setValue( 0 );
    this.sliderRed.setValue( 0 );
    this.sliderGreen.setValue( 0 );
    this.sliderBlue.setValue( 0 );
    this.spinnerBrightness.setValue( 0 );
    this.spinnerContrast.setValue( 0 );
    this.spinnerSaturation.setValue( 0 );
    this.spinnerRed.setValue( 0 );
    this.spinnerGreen.setValue( 0 );
    this.spinnerBlue.setValue( 0 );
    this.previewUpdEnabled = true;
    updPreview();
  }


  private float getContrast()
  {
    float contrast = (float) this.sliderContrast.getValue();
    if( contrast < 0F ) {
      // damit bei minimalen Kontrast nicht alles grau ist
      contrast = contrast * 0.75F;
    }
    return contrast;
  }


  private void updPreview()
  {
    if( this.previewUpdEnabled
	&& (this.srcImg != null)
	&& (this.srcReds != null)
	&& (this.srcGreens != null)
	&& (this.srcBlues != null)
	&& (this.srcAlphas != null)
	&& (this.previewReds != null)
	&& (this.previewGreens != null)
	&& (this.previewBlues != null) )
    {
      int   brightness = this.sliderBrightness.getValue();
      float contrast   = getContrast();
      int   saturation = this.sliderSaturation.getValue();
      int   diffR      = this.sliderRed.getValue();
      int   diffG      = this.sliderGreen.getValue();
      int   diffB      = this.sliderBlue.getValue();
      for( int i = 0; i < this.srcColorCnt; i++ ) {
	int rgb = convertRGB(
			(int) this.srcReds[ i ] & 0xFF,
			(int) this.srcGreens[ i ] & 0xFF,
			(int) this.srcBlues[ i ] & 0xFF,
			brightness,
			contrast,
			(float) saturation,
			diffR,
			diffG,
			diffB );
	this.previewReds[ i ]   = (byte) (rgb >> 16);
	this.previewGreens[ i ] = (byte) (rgb >> 8);
	this.previewBlues[ i ]  = (byte) rgb;
      }
      this.imageFld.setImage(
			new BufferedImage(
				new IndexColorModel(
					8,
					this.srcColorCnt,
					this.previewReds,
					this.previewGreens,
					this.previewBlues ),
				this.srcImg.getRaster(),
				false,
				new Hashtable<>() ) );
      this.imageFld.repaint();
    }
  }
}
