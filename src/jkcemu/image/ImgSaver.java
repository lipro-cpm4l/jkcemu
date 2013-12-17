/*
 * (c) 2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Speichern von Bildern
 */

package jkcemu.image;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.imageio.ImageIO;
import jkcemu.Main;
import jkcemu.base.*;


public class ImgSaver
{
  public static javax.swing.filechooser.FileFilter createFileFilter()
  {
    return ImgUtil.createFileFilter(
			ImageIO.getWriterFileSuffixes(),
			"pix", "scr" );
  }


  public static File saveImageAs(
			Frame         owner,
			BufferedImage image,
			String        filename )
  {
    File preSelection = null;
    if( filename != null ) {
      if( !filename.isEmpty() ) {
	File dirFile = Main.getLastPathFile( "image" );
	if( dirFile != null ) {
	  preSelection = new File( dirFile, filename );
	} else {
	  preSelection = new File( filename );
	}
      }
    }
    File file = EmuUtil.showFileSaveDlg(
			owner,
			"Bilddatei speichern",
			preSelection,
			createFileFilter() );
    if( file != null ) {
      try {
	String fileName = file.getPath();
	if( fileName != null ) {
	  String lowerFileName = fileName.toLowerCase();
	  if( lowerFileName.endsWith( ".pix" ) ) {
	    saveImageLLC2HiRes( owner, image, file );
	  } else if( lowerFileName.endsWith( ".scr" ) ) {
	    saveImageA5105( owner, image, file );
	  } else {

	    // Grafikformat ermitteln
	    String   usedFmt = null;
	    String[] formats = ImageIO.getWriterFormatNames();
	    if( formats != null ) {
	      for( int i = 0; (usedFmt == null) && (i < formats.length); i++ ) {
		String s = formats[ i ];
		if( s != null ) {
		  if( lowerFileName.endsWith( "." + s.toLowerCase() ) ) {
		    usedFmt = s;
		  }
		}
	      }
	    }

	    /*
	     * Eigentlich wuerde sich die Funktion
	     * ImageIO.write( RenderedImage, String, File )
	     * gut eignen, doch wenn die Datei nicht angelegt werden kann,
	     * wird in der Funktion ein StackTrace geschrieben und
	     * anschliessend eine IllegalArgumentException
	     * statt einer IOException geworfen.
	     */
	    boolean formatOK = false;
	    if( usedFmt != null ) {
	      OutputStream out = null;
	      try {
		out      = new FileOutputStream( file );
		formatOK = ImageIO.write( image, usedFmt, out );
		out.close();
		out = null;
	      }
	      finally {
		EmuUtil.doClose( out );
	      }
	    }

	    // Fehlermeldung erzeugen
	    if( !formatOK ) {
	      Set<String> fmtList = new TreeSet<String>();

	      // Grafikformate hinzufuegen
	      if( formats != null ) {
		for( int i = 0; i < formats.length; i++ ) {
		  String s = formats[ i ];
		  if( s != null ) {
		    s = s.toLowerCase();
		    if( !fmtList.contains( s ) ) {
		      fmtList.add( s );
		    }
		  }
		}
	      }

	      // Fehlermeldung anzeigen
	      if( fmtList.isEmpty() ) {
		throw new IOException(
			"Die Funktion wird auf dieser Plattform\n"
				+ "nicht unterst\u00FCtzt." );
	      }
	      StringBuilder buf = new StringBuilder( 128 );

	      buf.append( "Das durch die Dateiendung angegebene Format\n"
			+ "wird nicht unterst\u00FCtzt.\n"
			+ "Folgende Dateiendungen sind m\u00F6glich:\n" );

	      int col = 0;
	      for( String s : fmtList ) {
		if( col > 0 ) {
		  buf.append( ", " );
		} else {
		  buf.append( (char) '\n' );
		}
		buf.append( s );
		col++;
		if( col >= 10 ) {
		  col = 0;
		}
	      }
	      throw new IOException( buf.toString() );
	    }
	  }
	}
	Main.setLastFile( file, "image" );
      }
      catch( IOException ex ) {
	BasicDlg.showErrorDlg( owner, ex.getMessage() );
      }
    }
    return file;
  }


	/* --- private Methoden --- */

  private static void saveImageLLC2HiRes(
			Component     owner,
			BufferedImage image,
			File          file ) throws IOException
  {
    final int    OUT_W = 512;
    final int    OUT_H = 256;
    OutputStream out   = null;
    try {
      out = new FileOutputStream( file );

      // temporaeres Bild mit der richtigen Groesse und Farbanzahl erzeugen
      BufferedImage tmpImg = new BufferedImage(
				OUT_W,
				OUT_H,
				BufferedImage.TYPE_BYTE_BINARY,
				ImgUtil.getColorModelBW() );
      ImgUtil.fillBlack( tmpImg );
      int tmpX = 0;
      int tmpY = 0;
      int tmpW = 0;
      int tmpH = 0;
      int srcH = image.getHeight();
      int srcW = image.getWidth();
      if( srcW > (2 * srcH) ) {
	tmpW = OUT_W;
	tmpH = Math.round( (float) OUT_W / (float) srcW * (float) srcH );
	tmpY = (OUT_H - tmpH) / 2;
      } else {
	tmpH = OUT_H;
	tmpW = Math.round( (float) OUT_H / (float) srcH * (float) srcW );
	tmpX = (OUT_W - tmpW) / 2;
      }
      Graphics g = tmpImg.createGraphics();
      g.drawImage( image, tmpX, tmpY, tmpW, tmpH, owner );
      g.dispose();
      tmpImg.flush();

      // temporaeres Bild speichern
      for( int pRow = 0; pRow < 8; pRow++ ) {
	for( int cRow = 0; cRow < 32; cRow++ ) {
	  int x = 0;
	  int y = (cRow * 8) + pRow;
	  for( int cCol = 0; cCol < 64; cCol++ ) {
	    int b = 0;
	    for( int pCol = 0; pCol < 8; pCol++ ) {
	      b <<= 1;
	      int rgb = tmpImg.getRGB( x++, y );
	      if( (((rgb >> 16) & 0xFF)
			+ ((rgb >> 8) & 0xFF)
			+ (rgb & 0xFF)) >= 0x180 )
	      {
		b |= 0x01;
	      }
	    }
	    out.write( b );
	  }
	}
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.doClose( out );
    }
  }


  private static void saveImageA5105(
			Component     owner,
			BufferedImage image,
			File          file ) throws IOException
  {
    final int    OUT_W     = 320;
    final int    OUT_H_MAX = 200;
    OutputStream out       = null;
    try {
      out = new FileOutputStream( file );

      // temporaeres Bild mit der richtigen Groesse und Farbanzahl erzeugen
      int tmpX = 0;
      int tmpY = 0;
      int tmpW = 0;
      int srcH = image.getHeight();
      int srcW = image.getWidth();
      int outH = Math.round( (float) OUT_W / (float) srcW * (float) srcH );
      if( outH < OUT_H_MAX ) {
	tmpW = OUT_W;
      } else {
	outH = OUT_H_MAX;
	tmpW = Math.round( (float) outH / (float) srcH * (float) srcW );
	tmpX = (OUT_W - tmpW) / 2;
      }
      IndexColorModel cm     = ImgUtil.getColorModelA5105();
      BufferedImage   tmpImg = new BufferedImage(
					OUT_W,
					outH,
					BufferedImage.TYPE_BYTE_BINARY,
					cm );
      ImgUtil.fillBlack( tmpImg );
      Graphics g = tmpImg.createGraphics();
      g.drawImage( image, tmpX, tmpY, tmpW, outH, owner );
      g.dispose();
      tmpImg.flush();

      // temporaeres Bild speichern
      Map<Integer,Integer> rgb2Idx = new HashMap<Integer,Integer>();
      out.write( 0xFD );		// Kennung Videospeicher
      out.write( 0x00 );		// Anfangsadresse Grafikseite 0
      out.write( 0x40 );
      int wordCnt = outH * 80;
      int endAddr = 0x4000 + wordCnt - 1;
      out.write( endAddr & 0xFF );	// Endadresse
      out.write( (endAddr >> 8) & 0xFF );
      out.write( 0x00 );		// Startadresse
      out.write( 0x40 );
      for( int y = 0; y < outH; y++ ) {
	int x = 0;
	for( int i = 0; i < 80; i++ ) {		// 80 Datenworte
	  int b0 = 0;
	  int b1 = 0;
	  for( int p = 0; p < 4; p++ ) {	// je 4 Pixel
	    b0 >>= 1;
	    b1 >>= 1;
	    Integer rgb = new Integer( tmpImg.getRGB( x++, y ) );
	    Integer idx = rgb2Idx.get( rgb );
	    if( idx == null ) {
	      idx = new Integer( ImgUtil.getNearestIndex(
						cm,
						rgb.intValue() ) );
	      rgb2Idx.put( rgb, idx );
	    }
	    int v = idx.intValue();
	    if( (v & 0x01) != 0 ) {
	      b0 |= 0x08;
	    }
	    if( (v & 0x02) != 0 ) {
	      b0 |= 0x80;
	    }
	    if( (v & 0x04) != 0 ) {
	      b1 |= 0x08;
	    }
	    if( (v & 0x08) != 0 ) {
	      b1 |= 0x80;
	    }
	  }
	  out.write( b0 );
	  out.write( b1 );
	}
      }
      out.write( 0x1A );
      int len  = 8 + (2 * wordCnt);
      while( (len & 0x007F) != 0 ) {
	out.write( 0 );
	len++;
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.doClose( out );
    }
  }


	/* --- Konstruktor --- */

  private ImgSaver()
  {
    // nicht instanziierbar
  }
}
