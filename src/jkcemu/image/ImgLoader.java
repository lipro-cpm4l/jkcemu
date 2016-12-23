/*
 * (c) 2013-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Laden von Bildern
 */

package jkcemu.image;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.*;
import java.util.Arrays;
import javax.imageio.ImageIO;
import jkcemu.base.EmuUtil;
import jkcemu.text.TextUtil;


public class ImgLoader
{
  private static String[] fileExtensions = null;


  public static boolean accept( File file )
  {
    boolean rv = false;
    if( file != null ) {
      String name = file.getName();
      if( name != null ) {
	name = name.toLowerCase();
	rv   = TextUtil.endsWith( name, getLowerFileExtensions() );
	if( !rv ) {
	  if( name.endsWith( ".iff" )
	      || name.endsWith( ".ilbm" )
	      || name.endsWith( ".lbm" ) )
	  {
	    // IFF-ILBM-Datei
	    rv = true;
	  }
	  else if( (file.length() == 0x4000) && name.endsWith( ".pix" ) ) {
	    // LLC2-HIRES-Datei
	    rv = true;
	  }
	  else if( (file.length() > 7) && name.endsWith( ".scr" ) ) {
	    // A5105-Bild
	    rv = true;
	  }
	}
      }
    }
    return rv;
  }


  public static javax.swing.filechooser.FileFilter createFileFilter()
  {
    return ImgUtil.createFileFilter(
			"Unterst\u00FCtzte Bilddateien",
			ImageIO.getReaderFileSuffixes(),
			IFFFile.getFileSuffixes(),
			new String[] { "pix", "scr" } );
  }


  /*
   * Laden eines Bildes aus einer Datei
   *
   * Die Methode liefert null zurueck,
   * wenn das Dateiformat nicht unterstuetzt wird.
   * Ein OutOfMemoryError wird abgefangen und
   * dafuer in eine IOException geworfen.
   */
  public static ImgEntry load( File file ) throws IOException
  {
    ImgEntry.Mode mode = ImgEntry.Mode.UNSPECIFIED;
    BufferedImage img  = null;
    if( file != null ) {
      if( IFFFile.accept( file ) ) {
	img  = IFFFile.readImage( file );
	mode = ImgEntry.probeMode( img );
      } else {
	InputStream in = null;
	try {
	  long   len  = file.length();
	  String ext  = null;
	  String name = file.getName();
	  if( name != null ) {
	    int pos = name.lastIndexOf( '.' );
	    if( (pos >= 0) && ((pos + 1) < name.length()) ) {
	      ext = name.substring( pos + 1 ).toLowerCase();
	    }
	  }

	  // Bild laden
	  in = new BufferedInputStream( new FileInputStream( file ) );
	  if( ext != null ) {

	    // LLC2-HIRES-Bild
	    if( (img == null) && (len == 0x4000) && ext.equals( "pix" ) ) {
	      mode = ImgEntry.Mode.MONOCHROME;
	      img  = new BufferedImage(
				ImgUtil.LLC2_W,
				ImgUtil.LLC2_H,
				BufferedImage.TYPE_BYTE_BINARY,
				ImgUtil.getColorModelBW() );
	      ImgUtil.fillBlack( img );
	      int pos = 0;
	      int b   = in.read();
	      while( (b >= 0) && (pos < 0x4000) ) {
		int c = pos & 0x3F;
		int x = (c << 3);
		int y = ((pos >> 11) & 0x07) | ((pos >> 3) & 0xF8);
		for( int i = 0; i < 8; i++ ) {
		  if( (b & 0x80) != 0 ) {
		    img.setRGB( x, y, 0xFFFFFFFF );
		  }
		  b <<= 1;
		  x++;
		}
		pos++;
		b = in.read();
	      }
	    }

	    // A5105-Bild (Screen 5, 320x200x16)
	    if( (img == null) && (len > 7) && ext.equals( "scr" ) ) {
	      byte[] header = new byte[ 7 ];
	      if( in.read( header ) == header.length ) {
		int addr    = EmuUtil.getWord( header, 1 );
		int endAddr = EmuUtil.getWord( header, 3 );
		if( (((int) header[ 0 ] & 0xFF) == 0xFD)
		    && (addr >= 0x4000)
		    && (addr < endAddr)
		    && (((addr & 0x3FFF) % 80) == 0) )
		{
		  IndexColorModel cm = ImgUtil.getColorModelA5105();
		  int             h  = (endAddr - addr + 80) / 80;
		  if( h > ImgUtil.A5105_H ) {
		    h = ImgUtil.A5105_H;
		  }
		  mode = ImgEntry.Mode.A5105;
		  img  = new BufferedImage(
				ImgUtil.A5105_W,
				h,
				BufferedImage.TYPE_BYTE_BINARY,
				cm );
		  int x  = 0;
		  int y  = 0;
		  int b0 = in.read();
		  int b1 = in.read();
		  while( (addr < endAddr)
			 && (y < h)
			 && (b0 >= 0) && (b1 >= 0) )
		  {
		    int m0 = 0x01;
		    int m1 = 0x10;
		    for( int i = 0; i < 4; i++ ) {
		      int idx = 0;
		      if( (b0 & m0) != 0 ) {
			idx |= 0x01;
		      }
		      if( (b0 & m1) != 0 ) {
			idx |= 0x02;
		      }
		      if( (b1 & m0) != 0 ) {
			idx |= 0x04;
		      }
		      if( (b1 & m1) != 0 ) {
			idx |= 0x08;
		      }
		      img.setRGB( x, y, cm.getRGB( idx ) );
		      m0 <<= 1;
		      m1 <<= 1;
		      x++;
		    }
		    if( x >= 320 ) {
		      x = 0;
		      y++;
		    }
		    addr++;
		    b0 = in.read();
		    b1 = in.read();
		  }
		}
	      }
	    }
	  }
	  if( img == null ) {
	    img  = ImageIO.read( in );
	    mode = ImgEntry.probeMode( img );
	  }
	}
	catch( OutOfMemoryError ex ) {
	  System.gc();
	  throw new IOException(
		"Es steht nicht gen\u00FCgend Speicher zur Verf\u00FCgung." );
	}
	finally {
	  EmuUtil.closeSilent( in );
	}
      }
    }
    return img != null ?
		new ImgEntry(
			img,
			mode,
			ImgFld.Rotation.NONE,
			null,
			file,
			null )
		: null;
  }


	/* --- private Methoden --- */

  private static String[] getLowerFileExtensions()
  {
    if( fileExtensions == null ) {
      fileExtensions = ImageIO.getReaderFileSuffixes();
      if( fileExtensions != null ) {
 	for( int i = 0; i < fileExtensions.length; i++ ) {
 	  String ext = fileExtensions[ i ];
 	  if( ext.startsWith( "." ) ) {
 	    fileExtensions[ i ] = ext.toLowerCase();
 	  } else {
 	    fileExtensions[ i ] = "." + ext.toLowerCase();
 	  }
 	}
      }
    }
    return fileExtensions;
  }


	/* --- Konstruktor --- */

  private ImgLoader()
  {
    // nicht instanziierbar
  }
}
