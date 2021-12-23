/*
 * (c) 2016-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Zuschneiden eines Bildes
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.CancelableProgressDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class CropDlg extends BaseDlg
			implements
				CancelableProgressDlg.Progressable,
				ChangeListener,
				Runnable
{
  private Object[][] ratios = {
    { "frei \u00E4nderbar",  null },
    { "beibehalten",         null },
    { "1:1 (Z1013)",         1F },
    { "1:2",                 0.5F },
    { "2:1",                 2F },
    { "2:3",                 divF( 2, 3 ) },
    { "3:2",                 divF( 3, 2 ) },
    { "3:4",                 divF( 3, 4 ) },
    { "4:3 (LLC2)",          divF( 4, 3 ) },
    { "16:9",                divF( 16, 9 ) },
    { "16:10 (A5105)",       divF( 16, 10 ) },
    { "AC1",                 divF( ImageUtil.AC1_W,   ImageUtil.AC1_H ) },
    { "KC85/1, KC87, Z9001", divF( ImageUtil.Z9001_W, ImageUtil.Z9001_H ) },
    { "KC85/2..5, HC900",    divF( ImageUtil.KC85_W,  ImageUtil.KC85_H ) } };

  private Object[][] selectionColors = {
	{ "rot",       Color.RED },
	{ "gr\u00FCn", Color.GREEN },
	{ "blau",      Color.BLUE },
	{ "gelb",      Color.YELLOW },
	{ "wei\u00DF", Color.WHITE },
	{ "schwarz",   Color.BLACK } };

  private ImageFrm              imageFrm;
  private ImageFld              imageFld;
  private int                   wImg;
  private int                   hImg;
  private int                   progressMax;
  private int                   progressValue;
  private CancelableProgressDlg progressDlg;
  private BufferedImage         image;
  private BufferedImage         cropImg;
  private Float                 orgRatio;
  private SpinnerNumberModel    spinnerModelX;
  private SpinnerNumberModel    spinnerModelY;
  private SpinnerNumberModel    spinnerModelWidth;
  private SpinnerNumberModel    spinnerModelHeight;
  private JSpinner              spinnerX;
  private JSpinner              spinnerY;
  private JSpinner              spinnerWidth;
  private JSpinner              spinnerHeight;
  private JComboBox<Object>     comboRatio;
  private JComboBox<Object>     comboSelectionColor;
  private JButton               btnCrop;
  private JButton               btnClose;


  public void setImageSize( int w, int h )
  {
    if( (w != this.wImg) || (h != this.hImg) ) {
      try {
	this.wImg = (w > 0 ? w : 0);
	this.hImg = (h > 0 ? h : 0);
	if( (this.hImg > 0) && (this.wImg > 0) ) {
	  this.orgRatio = (float) this.wImg / (float) this.hImg;
	} else {
	  this.orgRatio = null;
	}
	resetSelectedRatio();
	this.spinnerModelX.setMaximum( this.wImg > 0 ? (this.wImg - 1) : 0 );
	this.spinnerModelY.setMaximum( this.hImg > 0 ? (this.hImg - 1) : 0 );
	this.spinnerModelWidth.setMaximum( this.wImg );
	this.spinnerModelHeight.setMaximum( this.hImg );
	setValueXSilent( 0 );
	setValueYSilent( 0 );
	setValueWidthSilent( this.wImg );
	setValueHeightSilent( this.hImg );
      }
      catch( IllegalArgumentException ex ) {
	EmuUtil.logSysError( this, null, ex );
      }
    }
  }


  public void setSelectedArea( Rectangle r )
  {
    if( r != null ) {
      int w = (r.width > 0 ? r.width : 0);
      int h = (r.height > 0 ? r.height : 0);
      try {
	setValueXSilent( r.x > 0 ? r.x : 0 );
	setValueYSilent( r.y > 0 ? r.y : 0 );
	setValueWidthSilent( w <= this.wImg ? w : this.wImg );
	setValueHeightSilent( h <= this.hImg ? h : this.hImg );
      }
      catch( IllegalArgumentException ex ) {
	EmuUtil.logSysError( this, null, ex );
      }
    }
  }


  /*
   * Die Methode liefert die auf das Seitenverhaeltnis
   * angepasste Groesse zurueck oder null,
   * wenn nichts angepasst werden muss.
   */
  public Rectangle toRatio( int x, int y, int w, int h )
  {
    Rectangle rv    = null;
    Float     ratio = getSelectedRatio();
    if( ratio != null ) {
      int x1 = x;
      int y1 = y;
      int w1 = w;
      int h1 = h;

      // x1,y1 ggf. nach links oben verschieben
      if( w1 < 0 ) {
	x1 += w1;
	w1 = -w1;
      }
      if( x1 < 0 ) {
	w1 += x1;
	x1 = 0;
      }

      // Laenge oder Breiten auf Bildgroesse begrenzen
      if( h1 < 0 ) {
	y1 += h1;
	h1 = -h1;
      }
      if( y1 < 0 ) {
	h1 += y1;
	y1 = 0;
      }
      if( (x1 + w1) > this.wImg ) {
	w1 = this.wImg - x1;
      }
      if( (x1 + w1) > this.wImg ) {
	w1 = this.wImg - x1;
      }

      // korrekte Hoehe in Bezug zur Breite ermitteln
      int newH = getHByW( w1, ratio.floatValue() );
      if( newH != h1 ) {
	if( (h < 0) && ((y1 + h1 - newH) < 0) ) {
	  /*
	   * wenn neue Hoehe ueber oberen Bildrand hinausragt,
	   * dann begrenzen und Breite verringen
	   */
	  h1 += y1;
	  w1 = getWByH( h1, ratio.floatValue() );
	} else if( (y1 + newH) > this.hImg ) {
	  /*
	   * wenn neue Hoehe ueber unteren Bildrand hinausragt,
	   * dann Breite verringen
	   */
	  h1 = this.hImg - y1;
	  w1 = getWByH( h1, ratio.floatValue() );
	} else {
	  // wenn neue Hoehe passt -> uebernehmen
	  h1 = newH;
	}
	rv = new Rectangle(
			x,
			y,
			w > 0 ? w1 : -w1,
			h > 0 ? h1 : -h1 );
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  public CropDlg( ImageFrm imageFrm )
  {
    super( imageFrm, Dialog.ModalityType.MODELESS );
    this.imageFrm      = imageFrm;
    this.imageFld      = imageFrm.getImageFld();
    this.wImg          = 0;
    this.hImg          = 0;
    this.progressMax   = 0;
    this.progressValue = 0;
    this.progressDlg   = null;
    this.image         = null;
    this.cropImg       = null;
    this.orgRatio      = null;
    setTitle( "Zuschneiden" );


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

    add( GUIFactory.createLabel( "X:" ), gbc );

    this.spinnerModelX = new SpinnerNumberModel( 0, 0, 9999, 1 );
    this.spinnerX      = GUIFactory.createSpinner( this.spinnerModelX );
    gbc.insets.left    = 0;
    gbc.gridx++;
    add( this.spinnerX, gbc );

    gbc.insets.left = 20;
    gbc.gridx++;
    add( GUIFactory.createLabel( "Breite:" ), gbc );

    this.spinnerModelWidth = new SpinnerNumberModel( 0, 0, 9999, 1 );
    this.spinnerWidth      = GUIFactory.createSpinner(
						this.spinnerModelWidth );
    gbc.insets.left        = 0;
    gbc.gridx++;
    add( this.spinnerWidth, gbc );

    gbc.insets.left = 5;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Y:" ), gbc );

    this.spinnerModelY = new SpinnerNumberModel( 0, 0, 9999, 1 );
    this.spinnerY      = GUIFactory.createSpinner( this.spinnerModelY );
    gbc.insets.left    = 0;
    gbc.gridx++;
    add( this.spinnerY, gbc );

    gbc.insets.left = 20;
    gbc.gridx++;
    add( GUIFactory.createLabel( "H\u00F6he:" ), gbc );

    this.spinnerModelHeight = new SpinnerNumberModel( 0, 0, 9999, 1 );
    this.spinnerHeight      = GUIFactory.createSpinner(
						this.spinnerModelHeight );
    gbc.insets.left         = 0;
    gbc.gridx++;
    add( this.spinnerHeight, gbc );


    // Optionen
    JPanel panelOpt = GUIFactory.createPanel( new GridBagLayout() );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.insets.top  = 10;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( panelOpt, gbc );

    GridBagConstraints gbcOpt = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    panelOpt.add(
		GUIFactory.createLabel( "Seitenverh\u00E4ltnis:" ),
		gbcOpt );
    gbcOpt.gridy++;
    panelOpt.add(
		GUIFactory.createLabel( "Farbe f\u00FCr Auswahl:" ),
		gbcOpt );

    this.comboRatio = GUIFactory.createComboBox();
    this.comboRatio.setEditable( false );
    for( Object[] e : ratios ) {
      this.comboRatio.addItem( e[ 0 ] );
    }
    gbcOpt.anchor = GridBagConstraints.WEST;
    gbcOpt.gridy  = 0;
    gbcOpt.gridx++;
    panelOpt.add( this.comboRatio, gbcOpt );

    this.comboSelectionColor = GUIFactory.createComboBox();
    this.comboSelectionColor.setEditable( false );
    for( Object[] e : selectionColors ) {
      this.comboSelectionColor.addItem( e[ 0 ] );
    }
    gbcOpt.anchor = GridBagConstraints.WEST;
    gbcOpt.gridy++;
    panelOpt.add( this.comboSelectionColor, gbcOpt );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnCrop = GUIFactory.createButton( "Zuschneiden" );
    panelBtn.add( this.btnCrop );

    this.btnClose = GUIFactory.createButtonClose();
    panelBtn.add( this.btnClose );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Listener
    this.spinnerX.addChangeListener( this );
    this.spinnerY.addChangeListener( this );
    this.spinnerWidth.addChangeListener( this );
    this.spinnerHeight.addChangeListener( this );
    this.comboRatio.addActionListener( this );
    this.comboSelectionColor.addActionListener( this );
    this.btnCrop.addActionListener( this );
    this.btnClose .addActionListener( this );


    // sicherstellen, dass die ausgewaehlte Auswahlfarbe auch verwendet wird
    doSelectionColor();
  }


	/* --- CancelableProgressDlg.Progressable --- */

  @Override
  public int getProgressMax()
  {
    return this.progressMax;
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
    if( (src == this.spinnerX)
	|| (src == this.spinnerY)
	|| (src == this.spinnerWidth)
	|| (src == this.spinnerHeight) )
    {
      int x = EmuUtil.getInt( this.spinnerX );
      int y = EmuUtil.getInt( this.spinnerY );
      int w = EmuUtil.getInt( this.spinnerWidth );
      int h = EmuUtil.getInt( this.spinnerHeight );
      if( src == this.spinnerX ) {
	if( (x + w) > this.wImg ) {
	  x = Math.max( 0, this.wImg - w );
	  setValueXSilent( x );
	}
      } else if( src == this.spinnerY ) {
	if( (y + h) > this.hImg ) {
	  y = Math.max( 0, this.hImg - h );
	  setValueYSilent( y );
	}
      } else if( src == this.spinnerWidth ) {
	boolean changed = false;
	if( (x + w) > this.wImg ) {
	  w = Math.max( 0, this.wImg - x );
	  if( (w == 0) && (this.wImg > 0) ) {
	    w       = 1;
	    changed = true;
	  }
	}
	Float ratio = getSelectedRatio();
	if( ratio != null ) {
	  int newH = getHByW( w, ratio.floatValue() );
	  if( newH != h ) {
	    if( (y + newH) > this.hImg ) {
	      w       = getWByH( this.hImg - y, ratio.floatValue() );
	      newH    = getHByW( w, ratio.floatValue() );
	      changed = true;
	    }
	    h = newH;
	    setValueHeightSilent( h );
	  }
	}
	if( changed ) {
	  setValueWidthSilent( w );
	}
      } else if( src == this.spinnerHeight ) {
	boolean changed = false;
	if( (y + h) > this.hImg ) {
	  h = Math.max( 0, this.hImg - y );
	  if( (h == 0) && (this.hImg > 0) ) {
	    h       = 1;
	    changed = true;
	  }
	}
	Float ratio = getSelectedRatio();
	if( ratio != null ) {
	  int newW = getWByH( h, ratio.floatValue() );
	  if( newW != w ) {
	    if( (x + newW) > this.wImg ) {
	      h       = getHByW( this.wImg - x, ratio.floatValue() );
	      newW    = getWByH( h, ratio.floatValue() );
	      changed = true;
	    }
	    w = newW;
	    setValueWidthSilent( w );
	  }
	}
	if( changed ) {
	  setValueHeightSilent( h );
	}
      }
      this.imageFrm.setSelection( x, y, w, h );
    }
  }


	/* --- Runnable --- */

  /*
   * Die Methode prueft, ob der ausgeschnittene Bereich Transparenz enthaelt.
   * Falls nein, wird daraus ein nichttransparentes Bild erzeugt.
   */
  @Override
  public void run()
  {
    if( (this.cropImg != null) && (this.progressDlg != null) ) {
      try {
	boolean transparency = false;
	int     w            = this.cropImg.getWidth();
	int     h            = this.cropImg.getHeight();
	this.progressMax     = w * h;
	this.progressValue   = 0;
	for( int y = 0; y < h; y++ ) {
	  for( int x = 0; x < w; x++ ) {
	    if( ((this.cropImg.getRGB( x, y ) >> 24) & 0xFF) < 0xFF ) {
	      transparency = true;
	      break;
	    }
	    this.progressValue++;
	  }
	}
	if( !transparency ) {
	  BufferedImage tmpImg = new BufferedImage(
					w,
					h,
					BufferedImage.TYPE_3BYTE_BGR );
	  Graphics g = tmpImg.createGraphics();
	  g.drawImage( this.cropImg, 0, 0, this );
	  g.dispose();
	  this.cropImg = tmpImg;
	}
      }
      finally {
	this.progressDlg.fireProgressFinished();
	EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    showCroppedImage();
			  }
			} );
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.comboRatio ) {
      rv = true;
      ensureRatio();
    }
    else if( src == this.comboSelectionColor ) {
      rv = true;
      doSelectionColor();
    }
    else if( src == this.btnCrop ) {
      rv = true;
      doCrop();
    }
    else if( src == this.btnClose ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      resetSelectedRatio();
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static Float divF( int w, int h )
  {
    return (float) w / (float) h;
  }


  private void doCrop()
  {
    this.image = this.imageFld.getImage();
    if( this.image != null ) {
      boolean state = true;
      if( ensureRatio() ) {
	state = showYesNoDlg(
			this,
			"Der ausgew\u00E4hlte Bereich entsprach nicht"
				+ " dem gew\u00FCnschten\n"
				+ "Seitenverh\u00E4ltnis und wurde deshalb"
				+ " angepasst.\n"
				+ "M\u00F6chten Sie das Bild nun auf die"
				+ " angepassten Werte zuschneiden?" );
      }
      if( state ) {
	int x = EmuUtil.getInt( this.spinnerX );
	int y = EmuUtil.getInt( this.spinnerY );
	int w = EmuUtil.getInt( this.spinnerWidth );
	int h = EmuUtil.getInt( this.spinnerHeight );
	if( (x >= 0) && (y >= 0 ) && (w > 0) && (h > 0) ) {
	  Rectangle r = this.imageFld.toUnrotated( x, y, w, h );
	  if( r != null ) {
	    x = r.x;
	    y = r.y;
	    w = r.width;
	    h = r.height;
	    if( x < 0 ) {
	      w += x;
	      x = 0;
	    }
	    if( y < 0 ) {
	      h += y;
	      y = 0;
	    }
	    int srcW = this.image.getWidth();
	    if( (x + w) > srcW ) {
	      w = srcW - x;
	    }
	    int srcH = this.image.getHeight();
	    if( (y + h) > srcH ) {
	      h = srcH - y;
	    }
	    if( (w > 0) && (h > 0) ) {
	      doClose();
	      this.cropImg = this.image.getSubimage( x, y, w, h );
	      if( this.cropImg.getTransparency() == Transparency.OPAQUE ) {
		showCroppedImage();
	      } else {
		this.progressDlg = new CancelableProgressDlg(
						this,
						"Schneide Bild zu...",
						this );
		(new Thread(
			Main.getThreadGroup(),
			this,
			"JKCEMU image crop" )).start();
		this.progressDlg.setVisible( true );
	      }
	    }
	  }
	}
      }
    }
  }


  private void doSelectionColor()
  {
    int idx = this.comboSelectionColor.getSelectedIndex();
    if( (idx >= 0) && (idx < selectionColors.length) ) {
      Object o = selectionColors[ idx ][ 1 ];
      if( o instanceof Color ) {
	this.imageFrm.setSelectionColor( (Color) o );
      }
    }
  }


  /*
   * Die Methode stellt sicher, dass das Seitenverhaeltnis passt.
   * Rueckgabewert:
   *   true:  Seitenverhaeltnis musste angepasst werden (Werte geasendert)
   *   false: Seitenverhaeltnis hat gestimmt bzw. war egal
   */
  private boolean ensureRatio()
  {
    boolean rv    = false;
    Float   ratio = getSelectedRatio();
    if( ratio != null ) {
      Rectangle r = toRatio(
			EmuUtil.getInt( this.spinnerX ),
			EmuUtil.getInt( this.spinnerY ),
			EmuUtil.getInt( this.spinnerWidth ),
			EmuUtil.getInt( this.spinnerHeight ) );
      if( r != null ) {
	setValueWidthSilent( r.width );
	setValueHeightSilent( r.height );
	this.imageFrm.setSelection( r.x, r.y, r.width, r.height );
	rv = true;
      }
    }
    return rv;
  }


  private Float getSelectedRatio()
  {
    Float rv  = null;
    int   idx = this.comboRatio.getSelectedIndex();
    if( idx == 1 ) {
      rv = this.orgRatio;
    } else if( (idx >= 2) && (idx < ratios.length) ) {
      Object o = ratios[ idx ][ 1 ];
      if( o != null ) {
	if( o instanceof Float ) {
	  rv = (Float) o;
	}
      }
    }
    return rv;
  }


  private int getHByW( int w, float ratio )
  {
    int h = Math.round( (float) w / ratio );
    if( (h <= 0) && (this.hImg > 0) ) {
      h = 1;
    }
    return h;
  }


  private int getWByH( int h, float ratio )
  {
    int w = Math.round( (float) h * ratio );
    if( (w <= 0) && (this.wImg > 0) ) {
      w = 1;
    }
    return w;
  }


  private void resetSelectedRatio()
  {
    try {
      this.comboRatio.setSelectedIndex( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }


  private void setValueXSilent( int v )
  {
    this.spinnerX.removeChangeListener( this );
    this.spinnerX.setValue( v );
    this.spinnerX.addChangeListener( this );
  }


  private void setValueYSilent( int v )
  {
    this.spinnerY.removeChangeListener( this );
    this.spinnerY.setValue( v );
    this.spinnerY.addChangeListener( this );
  }


  private void setValueHeightSilent( int v )
  {
    this.spinnerHeight.removeChangeListener( this );
    this.spinnerHeight.setValue( v );
    this.spinnerHeight.addChangeListener( this );
  }


  private void setValueWidthSilent( int v )
  {
    this.spinnerWidth.removeChangeListener( this );
    this.spinnerWidth.setValue( v );
    this.spinnerWidth.addChangeListener( this );
  }


  private void showCroppedImage()
  {
    if( this.cropImg != null ) {
      this.imageFrm.showCroppedImage( this.cropImg );
    }
  }
}
