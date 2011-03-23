/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer Bilder
 */

package jkcemu.image;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.Main;
import jkcemu.base.*;


public class ImgUtil
{
  public static final int ROUND_PIXELS_MAX = 9;


  public static FileNameExtensionFilter createFileFilter( String[] suffixes )
  {
    FileNameExtensionFilter rv = null;
    if( suffixes != null ) {
      if( suffixes.length > 0 ) {
	rv = new FileNameExtensionFilter(
				"Unterst\u00FCtzte Bilddateien",
				suffixes );
      }
    }
    return rv;
  }


  public static void ensureImageLoaded( Component owner, Image image )
  {
    if( image != null ) {
      MediaTracker mt = new MediaTracker( owner );
      mt.addImage( image, 0 );
      try {
	mt.waitForID( 0 );
      }
      catch( InterruptedException ex ) {}
    }
  }


  public static BufferedImage roundCorners(
					Component owner,
					Image     image,
					int       nTopPixels,
					int       nBottomPixels )
  {
    BufferedImage retImg = null;
    if( (image != null) && ((nTopPixels > 0) || (nBottomPixels > 0)) ) {
      /*
       * Wenn das Bild ein BufferedImage ist und Transparenz beherrscht,
       * kann es verwendet werden.
       * Anderenfalls wird ein neues BufferedImage angelegt.
       */
      if( image instanceof BufferedImage ) {
	int t = ((BufferedImage) image).getType();
	if( (t == BufferedImage.TYPE_4BYTE_ABGR)
	    || (t == BufferedImage.TYPE_4BYTE_ABGR_PRE)
	    || (t == BufferedImage.TYPE_INT_ARGB)
	    || (t == BufferedImage.TYPE_INT_ARGB_PRE) )
	{
	  retImg = (BufferedImage) image;
	} else {
	  ColorModel cm = ((BufferedImage) image).getColorModel();
	  if( cm != null ) {
	    if( cm instanceof IndexColorModel ) {
	      if( ((IndexColorModel) cm).getTransparentPixel() >= 0 ) {
		retImg = (BufferedImage) image;
	      }
	    }
	  }
	}
      }
      if( retImg == null ) {
	ensureImageLoaded( owner, image );
	int w = image.getWidth( owner );
	int h = image.getHeight( owner );
	if( (w > 0) && (h > 0) ) {
	  retImg = new BufferedImage( w, h, BufferedImage.TYPE_4BYTE_ABGR );
	  Graphics g = retImg.createGraphics();
	  if( g != null ) {
	    g.drawImage( image, 0, 0, owner );
	    g.dispose();
	  } else {
	    retImg = null;
	  }
	}
      }
      if( retImg != null ) {
	int w = retImg.getWidth();
	int h = retImg.getHeight();
	int m = 2 * Math.max( nTopPixels, nBottomPixels );
	if( (w > m) && (h > m) ) {
	  int r = nTopPixels - 1;
	  for( int x = 0; x < nTopPixels; x++ ) {
	    int dx = nTopPixels - 1 - x;
	    for( int y = 0; y < nTopPixels; y++ ) {
	      int    dy = nTopPixels - 1 - y;
	      double d  = Math.sqrt( (double) ((dx * dx) + (dy * dy)) );
	      if( Math.round( d ) >= r ) {
		retImg.setRGB( x, y, 0 );
		retImg.setRGB( w - x - 1, y, 0 );
	      }
	    }
	  }
	  r = nBottomPixels - 1;
	  for( int x = 0; x < nBottomPixels; x++ ) {
	    int dx = nBottomPixels - 1 - x;
	    for( int y = 0; y < nBottomPixels; y++ ) {
	      int    dy = nBottomPixels - 1 - y;
	      double d  = Math.sqrt( (double) ((dx * dx) + (dy * dy)) );
	      if( Math.round( d ) >= r ) {
		retImg.setRGB( x, h - y - 1, 0 );
		retImg.setRGB( w - x - 1, h - y - 1, 0 );
	      }
	    }
	  }
	}
      }
    }
    return retImg;
  }


  public static File saveImage(
			Frame         owner,
			BufferedImage image,
			File          preSelection )
  {
    File rv   = null;
    File file = EmuUtil.showFileSaveDlg(
			owner,
			"Bilddatei speichern",
			preSelection != null ?
				preSelection
				: Main.getLastPathFile( "image" ),
			createFileFilter( ImageIO.getWriterFileSuffixes() ) );
    if( file != null ) {
      String fileName = file.getPath();
      if( fileName != null ) {
	try {
	  boolean formatOK      = false;
	  String  lowerFileName = fileName.toLowerCase();

	  // Grafikformat ermitteln
	  String   usedFmt = null;
	  String[] formats = ImageIO.getWriterFormatNames();
	  if( formats != null ) {
	    for( int i = 0; (usedFmt == null) && (i < formats.length); i++ ) {
	      String s = formats[ i ];
	      if( s != null ) {
		if( lowerFileName.endsWith( "." + s.toLowerCase() ) )
		  usedFmt = s;
	      }
	    }
	  }

	  /*
	   * Eigentliche wuerde sich die Funktion
	   * ImageIO.write( RenderedImage, String, File )
	   * gut eignen, doch wenn die Datei nicht angelegt werden kann,
	   * wird in der Funktion ein StackTrace geschrieben und
	   * anschliessend eine IllegalArgumentException
	   * statt einer IOException geworfen.
	   */
	  if( usedFmt != null ) {
	    OutputStream out = null;
	    try {
	      out      = new FileOutputStream( file );
	      formatOK = ImageIO.write( image, usedFmt, out );
	      out.close();
	      out = null;
	      Main.setLastFile( file, "screen" );
	    }
	    finally {
	      if( out != null ) {
		try {
		  out.close();
		}
		catch( IOException ex ) {}
	      }
	    }
	  }

	  // erfolgreich gespeichert oder Format war falsch
	  if( formatOK ) {
	    Main.setLastFile( file, "image" );
	    rv = file;
	  } else {
	    java.util.List<String> fmtList = new ArrayList<String>();

	    // Grafikformate hinzufuegen
	    if( formats != null ) {
	      for( int i = 0; i < formats.length; i++ ) {
		String s = formats[ i ];
		if( s != null ) {
		  s = s.toLowerCase();
		  if( !fmtList.contains( s ) )
		    fmtList.add( s );
		}
	      }
	    }

	    // Fehlermeldung anzeigen
	    try {
	      Collections.sort( fmtList );
	    }
	    catch( ClassCastException ex ) {}
	    if( fmtList.isEmpty() ) {
	      BasicDlg.showErrorDlg(
			owner,
			"Die Funktion wird auf dieser Plattform\n"
				+ "nicht unterst\u00FCtzt." );
	    } else {
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
		if( col >= 10 )
		  col = 0;
	      }
	      BasicDlg.showErrorDlg( owner, buf.toString() );
	    }
	  }
	}
	catch( Exception ex ) {
	  BasicDlg.showErrorDlg(
		owner,
		fileName + ":\nSpeichern der Datei fehlgeschlagen\n\n"
			+ ex.getMessage() );
	}
      }
    }
    return rv;
  }
}

