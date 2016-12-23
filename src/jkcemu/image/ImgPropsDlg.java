/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anzeigen von Detailinformationen eines Bildes
 */

package jkcemu.image;

import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.lang.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.EventObject;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JLabel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;


public class ImgPropsDlg extends BaseDlg implements Runnable
{
  private static final String TEXT_IS_DETERMINING = "wird ermittelt...";

  private BufferedImage image;
  private int           wImg;
  private int           hImg;
  private int           transparentPixels;
  private int           translucentPixels;
  private boolean       gray;
  private boolean       monochrome;
  private Set<Integer>  totalColors;
  private Set<Integer>  visibleColors;
  private JLabel        labelColorCount;
  private JLabel        labelTransparency;
  private JButton       btnClose;


  public static void showDlg( Window owner, BufferedImage image, File file )
  {
    (new ImgPropsDlg( owner, image, file )).setVisible( true );
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      this.transparentPixels = 0;
      this.translucentPixels = 0;
      this.gray              = true;
      this.monochrome        = true;
      this.totalColors.clear();
      this.visibleColors.clear();
      for( int y = 0; y < this.hImg; y++ ) {
	for( int x = 0; x < this.wImg; x++ ) {
	  int v = image.getRGB( x, y );
	  int b = v & 0xFF;
	  int g = (v >> 8) & 0xFF;
	  int r = (v >> 16) & 0xFF;
	  int a = (v >> 24) & 0xFF;
	  if( a == 0 ) {
	    this.transparentPixels++;
	  } else if( a < 0xFF ) {
	    this.translucentPixels++;
	  }
	  if( (r != g) || (r != b) ) {
	    this.gray       = false;
	    this.monochrome = false;
	  }
	  if( ((r > 0) && (r < 0xFF))
		|| ((g > 0) && (g < 0xFF))
		|| ((b > 0) && (b < 0xFF)) )
	  {
	    this.monochrome = false;
	  }
	  totalColors.add( v );
	  if( a > 0 ) {
	    visibleColors.add( v );
	  }
	}
      }
    }
    finally {
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    colorsChecked();
			  }
			} );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnClose ) {
      rv = true;
      doClose();
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ImgPropsDlg( Window owner, BufferedImage image, File file )
  {
    super( owner, "Bildeigenschaften" );
    this.image             = image;
    this.wImg              = image.getWidth( this );
    this.hImg              = image.getHeight( this );
    this.transparentPixels = 0;
    this.translucentPixels = 0;
    this.gray              = false;
    this.monochrome        = false;
    this.totalColors       = new TreeSet<>();
    this.visibleColors     = new TreeSet<>();

    // Farben ermitteln
    int             indexedColors = -1;
    IndexColorModel icm           = ImgUtil.getIndexColorModel( image );
    if( icm != null ) {
      indexedColors = icm.getMapSize();
    }
    (new Thread(
		Main.getThreadGroup(),
		this,
		"JKCEMU imageviewer color counter" )).start();


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

    if( file != null ) {
      add( new JLabel( "Datei:" ), gbc );
      gbc.gridx++;
      add( new JLabel( file.getPath() ), gbc );
      gbc.insets.top = 0;

      long fileSize = file.length();
      if( fileSize > 0) {
	gbc.gridx = 0;
	gbc.gridy++;
	add( new JLabel( "Dateigr\u00F6\u00DFe:" ), gbc );
	gbc.gridx++;
	add(
	    new JLabel( EmuUtil.formatSize( fileSize, false, true ) ),
	    gbc );
	gbc.gridx = 0;
	gbc.gridy++;
      }
    }

    add( new JLabel( "Bildgr\u00F6\u00DFe:" ), gbc );
    StringBuilder buf = new StringBuilder( 64 );
    buf.append( EmuUtil.formatInt( this.wImg ) );
    buf.append( " x " );
    buf.append( EmuUtil.formatInt( this.hImg ) );
    buf.append( " = " );
    buf.append( EmuUtil.formatInt( this.wImg * this.hImg ) );
    buf.append( " Pixel" );
    gbc.gridx++;
    add( new JLabel( buf.toString() ), gbc );

    gbc.insets.top = 0;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( new JLabel( "Anzahl  Farben:" ), gbc );

    this.labelColorCount = new JLabel( TEXT_IS_DETERMINING );
    gbc.gridx++;
    add( this.labelColorCount, gbc );

    gbc.gridx = 0;
    gbc.gridy++;
    add( new JLabel( "Farbpalette:" ), gbc );
    gbc.gridx++;
    add(
	new JLabel(
		indexedColors > 0 ?
			String.format(
				"%d indexierte Farben",
				indexedColors )
			: "16,7 Mio Farben (nicht indexiert)" ),
	gbc );

    gbc.gridx = 0;
    gbc.gridy++;
    add( new JLabel( "Transparenz:" ), gbc );

    this.labelTransparency = new JLabel(
		image.getTransparency() == Transparency.OPAQUE ?
			"keine" : TEXT_IS_DETERMINING );
    gbc.gridx++;
    add( this.labelTransparency, gbc );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.top    = 15;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( this.btnClose, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


	/* --- private Methoden --- */

  private void colorsChecked()
  {
    StringBuilder buf = new StringBuilder( 128 );
    buf.append( EmuUtil.formatInt( this.totalColors.size() ) );
    if( this.visibleColors.size() != this.totalColors.size() ) {
      buf.append( ", davon " );
      buf.append( EmuUtil.formatInt( this.visibleColors.size() ) );
      buf.append( " sichtbar" );
    }
    if( this.monochrome ) {
      buf.append( " (schwarz/wei\u00DF)" );
    } else if( this.gray ) {
      buf.append( " (Graustufen)" );
    }
    this.labelColorCount.setText( buf.toString() );

    buf.setLength( 0 );
    if( (this.transparentPixels > 0) || (this.translucentPixels > 0) ) {
      float total = (float) (this.wImg * this.hImg);
      buf.append( EmuUtil.formatInt( this.transparentPixels ) );
      buf.append( " Pixel (" );
      buf.append( toPercent( (float) this.transparentPixels / total ) );
      buf.append( "%)" );
      if( this.translucentPixels > 0 ) {
	buf.append( " voll- und " );
	buf.append( EmuUtil.formatInt( this.translucentPixels ) );
	buf.append( " Pixel (" );
	buf.append( toPercent( (float) this.translucentPixels / total ) );
	buf.append( "%) teiltransparent" );
      } else {
	buf.append( " volltransparent" );
      }
    } else {
      buf.append( "keine" );
    }
    this.labelTransparency.setText( buf.toString() );

    pack();
  }


  private static String toPercent( float value )
  {
    String pattern = "##0.0";
    if( Math.round( value * 100F ) == 0 ) {
      pattern += "##";
    }
    NumberFormat numFmt = NumberFormat.getNumberInstance();
    if( numFmt instanceof DecimalFormat ) {
      ((DecimalFormat) numFmt).applyPattern( pattern );
    }
    return numFmt.format( value * 100F );
  }
}
