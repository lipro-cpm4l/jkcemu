/*
 * (c) 2013-2017 Jens Mueller
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
import jkcemu.emusys.KC85;
import jkcemu.text.TextUtil;


public class ImgLoader
{
  private static final String[] kc85ImgFileSuffixes = { "hip", "pic", "pip" };

  private static String[] iioFileSuffixes = null;


  public static boolean accept( File file )
  {
    boolean rv = false;
    if( file != null ) {
      String name = file.getName();
      if( name != null ) {
	name = name.toLowerCase();
	rv   = TextUtil.endsWith( name, getIIOLowerFileSuffixes() );
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
	  else if( file.length() > 0x0080 ) {
	    for( String s : kc85ImgFileSuffixes ) {
	      if( name.endsWith( "." + s ) ) {
		// KC85/2..5-Datei
		rv = true;
	      }
	    }
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
			kc85ImgFileSuffixes,
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
	long   len  = file.length();
	String ext  = null;
	String name = file.getName();
	if( name != null ) {
	  int pos = name.lastIndexOf( '.' );
	  if( (pos >= 0) && ((pos + 1) < name.length()) ) {
	    ext = name.substring( pos + 1 ).toLowerCase();
	  }
	}
	if( ext != null ) {
	  if( ext.equals( "pic" ) ) {
	    byte[] irm = readKC85IRMFile( file, true );
	    if( irm != null ) {
	      if( (irm.length > 0) && (irm.length <= 0x2800) ) {
		mode = ImgEntry.Mode.MONOCHROME;
	      }
	      img = createImageByKC852IRM( irm );
	    }
	  } else if( ext.equals( "pif" ) ) {
	    img = createImageByKC854IRM(
			readKC85IRMFile( replaceExt( file, "pip" ), true ),
			readKC85IRMFile( file, true ),
			false );
	  } else if( ext.equals( "pip" ) ) {
	    byte[] colorBytes = null;
	    File   colorFile  = replaceExt( file, "pif" );
	    if( colorFile.exists() ) {
	      colorBytes = readKC85IRMFile( colorFile, true );
	    } else {
	      mode = ImgEntry.Mode.MONOCHROME;
	    }
	    img = createImageByKC854IRM(
			readKC85IRMFile( file, true ),
			colorBytes,
			false );
	  } else if( ext.equals( "hif" ) ) {
	    mode = ImgEntry.Mode.KC854_HIRES;
	    img  = createImageByKC854IRM(
			readKC85IRMFile( replaceExt( file, "hip" ), false ),
			readKC85IRMFile( file, false ),
			true );
	  } else if( ext.equals( "hip" ) ) {
	    mode = ImgEntry.Mode.KC854_HIRES;
	    img  = createImageByKC854IRM(
			readKC85IRMFile( file, false ),
			readKC85IRMFile( replaceExt( file, "hif" ), false ),
			true );
	  }
	}
	if( img == null ) {
	  InputStream in = null;
	  try {
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

  private static BufferedImage createBlackKC85BWImage()
  {
    BufferedImage img = new BufferedImage(
				ImgUtil.KC85_W,
				ImgUtil.KC85_H,
				BufferedImage.TYPE_BYTE_BINARY,
				ImgUtil.getColorModelBW() );
    ImgUtil.fillBlack( img );
    return img;
  }


  private static BufferedImage createBlackKC85LowresImage()
  {
    int    n = KC85.getRawColorCount();
    byte[] r = new byte[ n ];
    byte[] g = new byte[ n ];
    byte[] b = new byte[ n ];
    for( int i = 0; i < n; i++ ) {
      int rgb = KC85.getRawRGB( i );
      r[ i ]  = (byte) (rgb >> 16);
      g[ i ]  = (byte) (rgb >> 8);
      b[ i ]  = (byte) rgb;
    }
    BufferedImage img = new BufferedImage(
				ImgUtil.KC85_W,
				ImgUtil.KC85_H,
				BufferedImage.TYPE_BYTE_INDEXED,
				new IndexColorModel( 8, n, r, g, b ) );
    ImgUtil.fillBlack( img );
    return img;
  }


  private static BufferedImage createImageByKC852IRM( byte[] irm )
  {
    BufferedImage img = null;
    if( irm != null ) {
      if( irm.length > 0x2800 ) {
	img = createBlackKC85LowresImage();
      } else {
	img = createBlackKC85BWImage();
      }
      for( int y = 0; y < ImgUtil.KC85_H; y++ ) {
	for( int x = 0; x < ImgUtil.KC85_W; x++ ) {
	  int col  = x / 8;
	  int pIdx = -1;
	  int cIdx = -1;
	  if( col < 32 ) {
	    pIdx = ((y << 5) & 0x1E00)
			| ((y << 7) & 0x0180)
			| ((y << 3) & 0x0060)
			| (col & 0x001F);
	    cIdx = 0x2800 | ((y << 3) & 0x07E0) | (col & 0x001F);
	  } else {
	    pIdx = 0x2000
			| ((y << 3) & 0x0600)
			| ((y << 7) & 0x0180)
			| ((y << 3) & 0x0060)
			| ((y >> 1) & 0x0018)
			| (col & 0x0007);
	    cIdx = 0x3000
			| ((y << 1) & 0x0180)
			| ((y << 3) & 0x0060)
			| ((y >> 1) & 0x0018)
			| (col & 0x0007);
	  }
	  if( (pIdx >= 0) && (pIdx < irm.length) ) {
	    int p = irm[ pIdx ];
	    int m = 0x80;
	    int n = x % 8;
	    if( n > 0 ) {
	      m >>= n;
	    }
	    boolean fg = ((p & m) != 0);
	    if( (cIdx >= 0) && (cIdx < irm.length) ) {
	      img.setRGB(
			x,
			y,
			KC85.getRawRGB(
				getKC85ColorIndex( irm[ cIdx ], fg ) ) );
	    } else {
	      img.setRGB(
			x,
			y,
			fg ? 0xFF000000 : 0xFFFFFFFF );
	    }
	  }
	}
      }
    }
    return img;
  }


  private static BufferedImage createImageByKC854IRM(
						byte[]  pixels,
						byte[]  colors,
						boolean hires )
  {
    BufferedImage img = null;
    if( colors != null ) {
      if( hires ) {
	img = ImgUtil.createBlackKC854HiresImage();
      } else {
	img = createBlackKC85LowresImage();
      }
    } else {
      img = createBlackKC85BWImage();
    }
    if( (img != null) && (pixels != null) ) {
      for( int y = 0; y < ImgUtil.KC85_H; y++ ) {
	for( int col = 0; col < ImgUtil.KC85_COLS; col++ ) {
	  int idx = (col * 256) + y;
	  if( (idx >= 0) && (idx < pixels.length) ) {
	    int p = pixels[ idx ];
	    int c = 0x07;
	    if( colors != null ) {
	      if( idx < colors.length ) {
		c = (int) colors[ idx ] & 0xFF;
	      }
	    }
	    int m = 0x80;
	    int x = col * 8;
	    for( int i = 0; i < 8; i++ ) {
	      boolean fg = ((p & m) != 0);
	      if( c >= 0 ) {
		int colorIdx = 0;
		if( hires && (colors != null) ) {
		  if( fg ) {
		    if( (c & m) != 0 ) {
		      colorIdx = 7;               // weiss
		    } else {
		      colorIdx = 2;               // rot
		    }
		  } else {
		    if( (c & m) != 0 ) {
		      colorIdx = 5;               // tuerkis
		    } else {
		      colorIdx = 0;               // schwarz
		    }
		  }
		} else {
		  colorIdx = getKC85ColorIndex( c, fg );
		}
		img.setRGB( x, y, KC85.getRawRGB( colorIdx ) );
	      } else {
		img.setRGB(
			x,
			y,
			fg ? 0xFF000000 : 0xFFFFFFFF );
	      }
	      x++;
	      m >>= 1;
	    }
	  }
	}
      }
    }
    return img;
  }


  private static String[] getIIOLowerFileSuffixes()
  {
    if( iioFileSuffixes == null ) {
      iioFileSuffixes = ImageIO.getReaderFileSuffixes();
      if( iioFileSuffixes != null ) {
 	for( int i = 0; i < iioFileSuffixes.length; i++ ) {
 	  String s = iioFileSuffixes[ i ].toLowerCase();
 	  if( s.startsWith( "." ) ) {
 	    iioFileSuffixes[ i ] = s;
 	  } else {
 	    iioFileSuffixes[ i ] = "." + s;
 	  }
 	}
      }
    }
    return iioFileSuffixes;
  }


  private static int getKC85ColorIndex( int colorByte, boolean foreground )
  {
    if( (colorByte & 0x80) != 0 ) {
      foreground = false;
    }
    return foreground ? ((colorByte >> 3) & 0x0F) : ((colorByte & 0x07) + 16);
  }


  /*
   * Die Methode liest eine KC-Bilddatei ein und liefert die Bilddaten
   * (IRM-Inhalt) unkomprimiert zurueckt.
   * Anhand der Laenge des zurueckgelieferten Byte-Arrays kann erkannt
   * werden, ob die KC85/2..3-Farbbytes enthalten sind.
   *
   * Bei PIC-, PIF- und PIP-Dateien gibt das erste Byte im Datenbereich
   * Auskunft ueber den Dateityp (Typbyte):
   *  0: Pixel- und Farbbytes fuer KC85/2,3 unkomprimiert
   *  1: nur Pixelbytes fuer KC85/2,3 unkomprimiert
   *  2: Pixel- und Farbbytes fuer KC85/2,3 komprimiert
   *  3: nur Pixelbytes fuer KC85/2,3 komprimiert
   *  4: nur Farbbytes fuer KC85/4,5 unkomprimiert
   *  5: nur Pixelbytes fuer KC85/4,5 unkomprimiert
   *  6: nur Farbbytes fuer KC85/4,5 komprimiert
   *  7: nur Pixelbytes fuer KC85/4,5 komprimiert
   *
   * HIF- und HIP-Dateien haben kein Typbyte.
   * Allerdings gibt die Anzahl der Adressen Auskunft darueber,
   * ob die Datei komprimiert ist
   * (Startadresse vorhanden -> Datei komprimiert)
   *
   * Kompression:
   *   Die Kompressionsroutine ist in der Datei enthalten und
   *   wird normalerweise auf Adresse 4000h geladen und dort gestartet.
   *   Das eigentliche Entpacken erfolgt in einer Z80-Emulation.
   */
  private static byte[] readKC85IRMFile(
				File    file,
				boolean hasTypeByte ) throws IOException
  {
    byte[]      buf = null;
    InputStream in  = null;
    try {
      in = new BufferedInputStream( new FileInputStream( file ) );

      // Kopfblock einlesen
      byte[] header = new byte[ 0x80 ];
      if( EmuUtil.read( in, header ) != header.length ) {
	throwUnsupportedFormat();
      }

      // Groesse Datenbereich
      int t = -1;
      if( hasTypeByte ) {
	t = in.read();
	if( (t < 0) || (t > 7) ) {
	  throwUnsupportedFormat();
	}
	buf = new byte[ (t == 0) || (t == 2) ? 0x3200 : 0x2800 ];
      } else {
	buf = new byte[ 0x2800 ];
      }
      Arrays.fill( buf, (byte) 0 );

      // Kompression?
      boolean compression = false;
      if( hasTypeByte ) {
	if( (t == 2) || (t == 3) || (t == 6) || (t == 7) ) {
	  compression = true;
	  if( (((int) header[ 17 ] & 0xFF) != 0xFF)
	      || (((int) header[ 18 ] & 0xFF) != 0x3F) )
	  {
	    throwUnsupportedFormat();
	  }
	}
      } else {
	if( ((int) header[ 16 ] & 0xFF) >= 3 ) {
	  compression = true;
	  if( (((int) header[ 17 ] & 0xFF) != 0x00)
	      || (((int) header[ 18 ] & 0xFF) != 0x40) )
	  {
	    throwUnsupportedFormat();
	  }
	}
      }

      // Datenbereich einlesen
      if( compression ) {
	int addr      = EmuUtil.getWord( header, 0x0011 );
	int endAddr   = EmuUtil.getWord( header, 0x0013 );
	int startAddr = 0x4000;
	if( ((int) header[ 0x10 ] & 0xFF) > 2 ) {
	  startAddr = EmuUtil.getWord( header, 0x0015 );
	}
	KC85ImgUnpacker unpacker  =  new KC85ImgUnpacker();
	if( hasTypeByte ) {
	  unpacker.setMemByte( addr++, t );
	}
	while( addr < endAddr ) {
	  int b = in.read();
	  if( b < 0 ) {
	    break;
	  }
	  unpacker.setMemByte( addr++, b );
	}
	unpacker.unpack( startAddr );
	addr    = 0x8000;
	int pos = 0;
	while( pos < buf.length ) {
	  buf[ pos++ ] = (byte) unpacker.getMemByte( addr++, false );
	}
      } else {
	EmuUtil.read( in, buf );
      }
    }
    finally {
      EmuUtil.closeSilent( in );
    }
    return buf;
  }


  private static File replaceExt( File file, String ext )
  {
    File   dirFile = file.getParentFile();
    String fName   = file.getName();
    if( fName != null ) {
      int pos = fName.lastIndexOf( '.' );
      if( pos >= 0 ) {
	fName = fName.substring( 0, pos + 1 );
      } else {
	fName += ".";
      }
      fName += ext;
    } else {
      fName = ext;
    }
    return dirFile != null ? new File( dirFile, fName ) : new File( fName );
  }


  private static void throwUnsupportedFormat() throws IOException
  {
    throw new IOException( "Dateiformat nicht unterst\u00FCtzt" );
  }


	/* --- Konstruktor --- */

  private ImgLoader()
  {
    // nicht instanziierbar
  }
}
