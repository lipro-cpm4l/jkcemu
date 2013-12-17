/*
 * (c) 2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Laden von Bildern
 */

package jkcemu.image;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import javax.imageio.ImageIO;
import jkcemu.base.EmuUtil;
import jkcemu.text.TextUtil;


public class ImgLoader
{
  private static String[] fileExtensions = null;


  public static boolean accepts( File file )
  {
    boolean rv = false;
    if( file != null ) {
      String name = file.getName();
      if( name != null ) {
	name = name.toLowerCase();
	rv   = TextUtil.endsWith( name, getLowerFileExtensions() );
	if( !rv ) {
	  if( (file.length() == 0x4000) && name.endsWith( ".pix" ) ) {
	    // LLC2-HiRes-Datei
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
			ImageIO.getReaderFileSuffixes(),
			"pix", "scr" );
  }


  /*
   * Laden eines Bildes aus einer Datei
   *
   * Die Methode liefert null zurueck,
   * wenn das Dateiformat nicht unterstuetzt wird.
   * Ein OutOfMemoryError wird abgefangen und
   * dafuer in eine IOException geworfen.
   */
  public static BufferedImage load( File file ) throws IOException
  {
    BufferedImage img = null;
    if( file != null ) {
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
	in = new FileInputStream( file );
	if( ext != null ) {

	  // LLC2-HiRes-Bild
	  if( (len == 0x4000) && ext.equals( "pix" ) ) {
	    img = new BufferedImage(
				512,
				256,
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
	  if( (len > 7) && ext.equals( "scr" ) ) {
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
		if( h > 200 ) {
		  h = 200;
		}
		img = new BufferedImage(
				320,
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
	  img = ImageIO.read( in );
	}
      }
      catch( OutOfMemoryError ex ) {
	throw new IOException(
		"Es steht nicht gen\u00FCgend Speicher zur Verf\u00FCgung." );
      }
      finally {
	EmuUtil.doClose( in );
      }
    }
    return img;
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
