/*
 * (c) 2016-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dithering
 *
 * Die Klasse implemntiert die Dithering-Algorithmen:
 *   Floyd Steinberg
 *   Sierra-3-Zeilen
 *   Atkinson
 */

package jkcemu.image;

import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.Arrays;
import jkcemu.Main;
import jkcemu.base.CancelableProgressDlg;


public class Dithering
		implements CancelableProgressDlg.Progressable, Runnable
{
  public enum Algorithm { FLOYD_STEINBERG, SIERRA3, ATKINSON };

  private BufferedImage         srcImg;
  private BufferedImage         retImg;
  private IndexColorModel       icm;
  private Algorithm             algorithm;
  private int                   wImg;
  private int                   hImg;
  private short[][]             rDiffBuf;
  private short[][]             gDiffBuf;
  private short[][]             bDiffBuf;
  private volatile int          progressValue;
  private CancelableProgressDlg dlg;


  public Dithering(
		BufferedImage   srcImg,
		IndexColorModel icm,
		Algorithm       algorithm )
  {
    this.srcImg        = srcImg;
    this.icm           = icm;
    this.algorithm     = algorithm;
    this.wImg          = srcImg.getWidth();
    this.hImg          = srcImg.getHeight();
    this.progressValue = 0;
    this.dlg           = null;
    this.rDiffBuf      = null;
    this.gDiffBuf      = null;
    this.bDiffBuf      = null;
    this.retImg        = null;
  }


  public BufferedImage doDithering()
  {
    this.progressValue = 0;
    int diffRowCnt     = 3;
    if( algorithm.equals( Algorithm.FLOYD_STEINBERG ) ) {
      diffRowCnt = 2;
    }
    this.rDiffBuf = new short[ diffRowCnt ][];
    this.gDiffBuf = new short[ diffRowCnt ][];
    this.bDiffBuf = new short[ diffRowCnt ][];
    for( int i = 0; i < diffRowCnt; i++ ) {
      this.rDiffBuf[ i ] = new short[ this.wImg ];
      this.gDiffBuf[ i ] = new short[ this.wImg ];
      this.bDiffBuf[ i ] = new short[ this.wImg ];
      Arrays.fill( this.rDiffBuf[ i ], (short) 0 );
      Arrays.fill( this.gDiffBuf[ i ], (short) 0 );
      Arrays.fill( this.bDiffBuf[ i ], (short) 0 );
    }
    int transpColorARGB = -1;
    int transpColorIdx  = this.icm.getTransparentPixel();
    if( transpColorIdx >= 0 ) {
      transpColorARGB = this.icm.getRGB( transpColorIdx );
    }
    BufferedImage retImg = new BufferedImage(
				this.wImg,
				this.hImg,
				this.icm.getMapSize() > 16 ?
					BufferedImage.TYPE_BYTE_INDEXED
					: BufferedImage.TYPE_BYTE_BINARY,
				this.icm );
    for( int y = 0; y < this.hImg; y++ ) {
      for( int x = 0; x < this.wImg; x++ ) {
	if( this.dlg.wasCancelled() ) {
	  break;
	}
	int rgb1 = this.srcImg.getRGB( x, y );
	if( (transpColorARGB != -1) && (((rgb1 >> 24) & 0xFF) < 0x80) ) {
	  retImg.setRGB( x, y, transpColorARGB );
	} else {

	  // neue Soll-Farbe fuer das Pixel
	  int r1 = ((rgb1 >> 16) & 0xFF) + this.rDiffBuf[ 0 ][ x ];
	  if( r1 < 0 ) {
	    r1 = 0;
	  } else if( r1 > 0xFF ) {
	    r1 = 0xFF;
	  }
	  int g1 = ((rgb1 >> 8) & 0xFF) + this.gDiffBuf[ 0 ][ x ];
	  if( g1 < 0 ) {
	    g1 = 0;
	  } else if( g1 > 0xFF ) {
	    g1 = 0xFF;
	  }
	  int b1 = (rgb1 & 0xFF) + this.bDiffBuf[ 0 ][ x ];
	  if( b1 < 0 ) {
	    b1 = 0;
	  } else if( b1 > 0xFF ) {
	    b1 = 0xFF;
	  }

	  // neue Ist-Farbe fuer das Pixel
	  int rgb2 = this.icm.getRGB(
			ImageUtil.getNearestIndex(
					this.icm,
					r1,
					g1,
					b1 ) );
	  retImg.setRGB( x, y, rgb2 );

	  /*
	   * Abweichung zwischen Soll- und Ist-Farbe
	   * anteilig auf die naechsten Pixel verteilen
	   */
	  int rDiff = r1 - ((rgb2 >> 16) & 0xFF);
	  int gDiff = g1 - ((rgb2 >> 8) & 0xFF);
	  int bDiff = b1 - (rgb2 & 0xFF);
	  switch( this.algorithm ) {
	    case FLOYD_STEINBERG:
	      addDiff( 0, x + 1, rDiff, gDiff, bDiff, 0.4375F );    // 7/16
	      addDiff( 1, x - 1, rDiff, gDiff, bDiff, 0.1875F );    // 3/16
	      addDiff( 1, x,     rDiff, gDiff, bDiff, 0.3125F );    // 5/16
	      addDiff( 1, x + 1, rDiff, gDiff, bDiff, 0.0625F );    // 1/16
	      break;
	    case SIERRA3:
	      addDiff( 0, x + 1, rDiff, gDiff, bDiff, 0.15625F );   // 5/32
	      addDiff( 0, x + 2, rDiff, gDiff, bDiff, 0.09375F );   // 3/32
	      addDiff( 1, x - 2, rDiff, gDiff, bDiff, 0.0625F );    // 2/32
	      addDiff( 1, x - 1, rDiff, gDiff, bDiff, 0.125F );     // 4/32
	      addDiff( 1, x,     rDiff, gDiff, bDiff, 0.15625F );   // 5/32
	      addDiff( 1, x + 1, rDiff, gDiff, bDiff, 0.125F );     // 4/32
	      addDiff( 1, x + 2, rDiff, gDiff, bDiff, 0.0625F );    // 2/32
	      addDiff( 2, x - 1, rDiff, gDiff, bDiff, 0.0625F );    // 2/32
	      addDiff( 2, x,     rDiff, gDiff, bDiff, 0.09375F );   // 3/32
	      addDiff( 2, x + 1, rDiff, gDiff, bDiff, 0.0625F );    // 2/32
	      break;
	    case ATKINSON:
	      addDiff( 0, x + 1, rDiff, gDiff, bDiff, 0.125F );     // 1/8
	      addDiff( 0, x + 2, rDiff, gDiff, bDiff, 0.125F );     // 1/8
	      addDiff( 1, x - 1, rDiff, gDiff, bDiff, 0.125F );     // 1/8
	      addDiff( 1, x,     rDiff, gDiff, bDiff, 0.125F );     // 1/8
	      addDiff( 1, x + 1, rDiff, gDiff, bDiff, 0.125F );     // 1/8
	      addDiff( 2, x,     rDiff, gDiff, bDiff, 0.125F );     // 1/8
	      break;
	  }
	}
	this.progressValue++;
      }

      // Pufferzeilen fuer Differenzen rotieren
      short[] rRow = this.rDiffBuf[ 0 ];
      short[] gRow = this.gDiffBuf[ 0 ];
      short[] bRow = this.bDiffBuf[ 0 ];
      for( int i = 1; i < diffRowCnt; i++ ) {
	this.rDiffBuf[ i - 1 ] = this.rDiffBuf[ i ];
	this.gDiffBuf[ i - 1 ] = this.gDiffBuf[ i ];
	this.bDiffBuf[ i - 1 ] = this.bDiffBuf[ i ];
      }
      Arrays.fill( rRow, (short) 0 );
      Arrays.fill( gRow, (short) 0 );
      Arrays.fill( bRow, (short) 0 );
      this.rDiffBuf[ diffRowCnt - 1 ] = rRow;
      this.gDiffBuf[ diffRowCnt - 1 ] = gRow;
      this.bDiffBuf[ diffRowCnt - 1 ] = bRow;
    }
    return this.dlg.wasCancelled() ? null : retImg;
  }


  public static String getAlgorithmText( Algorithm algorithm )
  {
    String rv = "";
    switch( algorithm ) {
      case FLOYD_STEINBERG:
	rv = "Floyd-Steinberg (Standardverfahren)";
	break;
      case SIERRA3:
	rv = "Sierra-3 (feinere Abstufungen)";
	break;
      case ATKINSON:
	rv = "Atkinson (weniger Farbbluten, mehr Kontrast)";
	break;
    }
    return rv;
  }


  public void setDialog( CancelableProgressDlg dlg )
  {
    this.dlg = dlg;
  }


  public static BufferedImage work(
				Window          owner,
				BufferedImage   srcImg,
				IndexColorModel icm,
				Algorithm       algorithm )
  {
    Dithering instance = new Dithering( srcImg, icm, algorithm );
    instance.dlg       = new CancelableProgressDlg(
						owner,
						"Dithering...",
						instance );
    (new Thread(
		Main.getThreadGroup(),
		instance,
		"JKCEMU dithering" )).start();
    instance.dlg.setVisible( true );
    return instance.dlg.wasCancelled() ? null : instance.retImg;
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


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      this.retImg = doDithering();
    }
    finally {
      dlg.fireProgressFinished();
    }
  }


	/* --- private Methoden --- */

  private void addDiff( int rowIdx, int x, int r, int g, int b, float f )
  {
    if( rowIdx < this.rDiffBuf.length ) {
      if( (x >= 0) && (x < this.rDiffBuf[ rowIdx ].length) ) {
	this.rDiffBuf[ rowIdx ][ x ] += (short) Math.round( (float) r * f );
      }
    }
    if( rowIdx < this.gDiffBuf.length ) {
      if( (x >= 0) && (x < this.gDiffBuf[ rowIdx ].length) ) {
	this.gDiffBuf[ rowIdx ][ x ] += (short) Math.round( (float) g * f );
      }
    }
    if( rowIdx < this.bDiffBuf.length ) {
      if( (x >= 0) && (x < this.bDiffBuf[ rowIdx ].length) ) {
	this.bDiffBuf[ rowIdx ][ x ] += (short) Math.round( (float) b * f );
      }
    }
  }
}
