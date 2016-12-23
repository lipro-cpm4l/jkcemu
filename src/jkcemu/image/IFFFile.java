/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Unterstuetzung fuer IFF/ILBM-Dateien
 */

package jkcemu.image;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.base.EmuUtil;


public class IFFFile
{
  private static final String[] fileSuffixes = { "iff", "ilbm", "lbm" };

  private static FileNameExtensionFilter imageFileFilter   = null;
  private static FileNameExtensionFilter paletteFileFilter = null;

  private BufferedImage   image;
  private IndexColorModel icm;


  public static boolean accept( File file )
  {
    return EmuUtil.accept( file, fileSuffixes );
  }


  public static String[] getFileSuffixes()
  {
    return fileSuffixes;
  }


  public static FileNameExtensionFilter getImageFileFilter()
  {
    if( imageFileFilter == null ) {
      imageFileFilter = ImgUtil.createFileFilter(
					"IFF/ILBM-Datei",
					fileSuffixes );
    }
    return imageFileFilter;
  }


  public static FileNameExtensionFilter getPaletteFileFilter()
  {
    if( paletteFileFilter == null ) {
      paletteFileFilter = ImgUtil.createFileFilter(
					"IFF/ILBM-Farbpalettendatei",
					fileSuffixes );
    }
    return paletteFileFilter;
  }


  public static BufferedImage readImage( File file ) throws IOException
  {
    BufferedImage image = readFile( file, false ).image;
    if( image == null ) {
      throw new IOException( "Die Datei enth\u00E4lt keine Bilddaten." );
    }
    return image;
  }


  public static IndexColorModel readPalette( File file ) throws IOException
  {
    IndexColorModel icm = readFile( file, true ).icm;
    if( icm == null ) {
      throw new IOException( "Die Datei enth\u00E4lt keine Farbpalette." );
    }
    return icm;
  }


  /*
   * Das IFF/ILBM-Format kann Transparenz speichern,
   * entweder in Form einer transparenten Farbe, einer Maske
   * oder in einem 32-Bit-Bild mit einem Alpha-Anteil.
   * Allerdings werden Dateien mit transparenten Bildern
   * nicht von allen Programmen unterstuetzt.
   * Manche zeigen das Bild falsch an (z.B. bei einer Maske)
   * oder stuerzen sogar ab (z.B. bei 32-Bit-Bildern).
   * Aus diesem Grund wird hier Transparenz nur in Form
   * einer transparenten Farbe gespeichert,
   * wenn das Bild indexierte Farben hat und es
   * inkl. der transparenten Farbe nicht mehr als 256 sind.
   */
  public static void writeImage(
			File          file,
			BufferedImage image ) throws IOException
  {
    int width  = image.getWidth();
    int height = image.getHeight();

    // Anzahl der Bitplanes ermitteln
    int             bitplaneCnt = 24;
    IndexColorModel icm         = ImgUtil.getIndexColorModel( image );
    if( icm != null ) {
      int colorCnt = icm.getMapSize();
      if( colorCnt > 0 ) {
	int bits = 1;
	int n    = 2;
	while( n < colorCnt ) {
	  bits++;
	  n <<= 1;
	}
	bitplaneCnt = bits;
      }
    }
    if( bitplaneCnt > 8 ) {
      icm = null;
    }

    // BMHD- und ggf. CMAP-Chunk erzeugen
    int           transpColor    = 0;
    AtomicInteger transpColorOut = null;
    if( image.getTransparency() != Transparency.OPAQUE ) {
      transpColorOut = new AtomicInteger( 0 );
    }
    ByteArrayOutputStream headerChunks = createHeaderChunks(
						width,
						height,
						bitplaneCnt,
						1,	// Kompression
						icm,
						transpColorOut );
    if( transpColorOut != null ) {
      if( transpColorOut.get() < 0 ) {
	transpColor = transpColorOut.get();
      }
    }
    if( (image.getTransparency() != Transparency.OPAQUE)
	&& (transpColor == 0) )
    {
      throw new IOException(
	"Das Bild enth\u00E4lt transparente Pixel,\n"
		+ "die nicht im IFF/ILBM-Format gespeichert"
		+ " werden k\u00F6nnen." );
    }

    // Body-Chunk erzeugen
    int bitplaneLineLen = (width + 7) / 8;
    if( (bitplaneLineLen % 2) != 0 ) {
      bitplaneLineLen++;
    }
    byte[][] bitplanes = new byte[ bitplaneCnt ][];
    for( int i = 0; i < bitplanes.length; i++ ) {
      bitplanes[ i ] = new byte[ bitplaneLineLen ];
    }
    ByteArrayOutputStream bodyChunk = new ByteArrayOutputStream( 0x8000 );
    for( int y = 0; y < height; y++ ) {
      for( int i = 0; i < bitplanes.length; i++ ) {
	Arrays.fill( bitplanes[ i ], (byte) 0 );
      }
      int x = 0;
      for( int pos = 0; pos < bitplaneLineLen; pos++ ) {
	int dstBit = 0x80;
	for( int b = 0; b < 8; b++ ) {
	  int v = 0;
	  if( x < width ) {
	    v = image.getRGB( x, y );
	  }
	  if( icm != null ) {
	    if( (transpColor > 0) && ((v >> 24) & 0xFF) < 0x80 ) {
	      v = transpColor;
	    } else {
	      v = ImgUtil.getNearestIndex( icm, v );
	    }
	  } else {
	    v = (v & 0xFF000000)
			| ((v >> 16) & 0x000000FF)
			| (v & 0x0000FF00)
			| ((v << 16) & 0x00FF0000);
	  }
	  int bitplaneMask = 0x01;
	  for( int i = 0; i < bitplaneCnt; i++ ) {
	    if( (v & bitplaneMask) != 0 ) {
	      bitplanes[ i ][ pos ] |= dstBit;
	    }
	    bitplaneMask <<= 1;
	  }
	  dstBit >>= 1;
	  x++;
	}
      }
      for( int i = 0; i < bitplanes.length; i++ ) {
	ImgUtil.writePackBits( bodyChunk, bitplanes[ i ] );
      }
    }

    // Datei schreiben
    writeFile( file, headerChunks, bodyChunk );
  }


  public static void writePalette(
				File            file,
				IndexColorModel icm ) throws IOException
  {
    writeFile( file, createHeaderChunks( 0, 0, 0, 0, icm, null ), null );
  }


	/* --- private Methoden --- */

  private static ByteArrayOutputStream createHeaderChunks(
			int             width,
			int             height,
			int             bitplaneCnt,
			int             compression,
			IndexColorModel icm,
			AtomicInteger   transpColorOut ) throws IOException
  {
    int transpColor = 0;
    int colorCnt    = 0;
    if( icm != null ) {
      colorCnt = icm.getMapSize();
      if( (transpColorOut != null) && (colorCnt > 0) ) {
	transpColor = icm.getTransparentPixel();
	if( transpColor < 1 ) {    // Farbe 0 nicht fuer Transparenz nehmen
	  if( colorCnt < 256 ) {
	    transpColor = colorCnt;
	    colorCnt++;
	  }
	}
      }
    }

    ByteArrayOutputStream buf = new ByteArrayOutputStream(
						37 + (3 * colorCnt) );

    // BMHD-Chunk erzeugen
    EmuUtil.writeASCII( buf, "BMHD" );
    EmuUtil.writeInt4BE( buf, 20 );		// Chunk-Laenge
    EmuUtil.writeInt2BE( buf, width );		// Bildbreite
    EmuUtil.writeInt2BE( buf, height );		// Bildhoehe
    EmuUtil.writeInt4BE( buf, 0 );		// x, y
    buf.write( bitplaneCnt );
    buf.write( transpColor > 0 ? 2 : 0 );	// Maskierung
    buf.write( compression );			// Kompression
    buf.write( 0 );				// Fuellbyte
    EmuUtil.writeInt2BE( buf, transpColor );	// Farbe fuer Transparenz
    buf.write( width > 0 ? 1 : 0 );		// Seitenverhaeltnis X
    buf.write( height > 0 ? 1 : 0 );		// Seitenverhaeltnis Y
    EmuUtil.writeInt2BE( buf, width );		// Seitenbreite
    EmuUtil.writeInt2BE( buf, height );		// Seitenhoehe

    // CMAP-Chunk
    if( icm != null ) {
      EmuUtil.writeASCII( buf, "CMAP" );
      EmuUtil.writeInt4BE( buf, 3 * colorCnt );	// Chunk-Laenge
      for( int i = 0; i < colorCnt; i++ ) {
	int rgb = 0x00FFFFFF;			// Transparenz: weiss
	if( i < icm.getMapSize() ) {
	  rgb = icm.getRGB( i );
	}
	buf.write( rgb >> 16 );
	buf.write( rgb >> 8 );
	buf.write( rgb );
      }
      if( (buf.size() % 2) != 0 ) {
	buf.write( 0 );			// Fuellbyte fuer gerade Byteanzahl
      }
    }

    // Rueckgabewerte
    if( transpColorOut != null ) {
      transpColorOut.set( transpColor );
    }
    return buf;
  }


  private static IFFFile readFile(
				File    file,
				boolean paletteOnly ) throws IOException
  {
    BufferedImage   image = null;
    IndexColorModel icm   = null;
    InputStream     in    = null;
    try {
      in = new BufferedInputStream( new FileInputStream( file ) );

      // Datei lesen
      byte[] fileHdr         = new byte[ 12 ];
      byte[] cmap            = null;
      byte[] body            = null;
      int    width           = -1;
      int    height          = -1;
      int    bitplanes       = 0;
      int    compression     = 0;
      int    masking         = 0;
      int    transpColor     = 0;
      int    bitplaneLineLen = 0;
      if( EmuUtil.read( in, fileHdr ) == fileHdr.length ) {
	if( EmuUtil.isTextAt( "FORM", fileHdr, 0 )
	    && EmuUtil.isTextAt( "ILBM", fileHdr, 8 ) )
	{
	  byte[] chunkHdr = new byte[ 8 ];
	  while( EmuUtil.read( in, chunkHdr ) == chunkHdr.length ) {
	    int chunkLen = (int) EmuUtil.getInt4BE( chunkHdr, 4 );
	    if( chunkLen < 0 ) {
	      break;
	    }
	    if( EmuUtil.isTextAt( "BMHD", chunkHdr, 0 ) ) {
	      byte[] bmhd = new byte[ chunkLen ];
	      if( EmuUtil.read( in, bmhd ) != bmhd.length ) {
		break;
	      }
	      if( width < 0 ) {
		width           = EmuUtil.getInt2BE( bmhd, 0 );
		height          = EmuUtil.getInt2BE( bmhd, 2 );
		bitplanes       = (int) bmhd[ 8 ] & 0xFF;
		masking         = (int) bmhd[ 9 ] & 0xFF;
		compression     = (int) bmhd[ 10 ] & 0xFF;
		transpColor     = EmuUtil.getInt2BE( bmhd, 12 );
		bitplaneLineLen = (width + 7) / 8;
		if( (bitplaneLineLen % 2) != 0 ) {
		  bitplaneLineLen++;		// auf 16 Bit ausrichten
		}
	      }
	    } else if( EmuUtil.isTextAt( "CMAP", chunkHdr, 0 ) ) {
	      cmap = new byte[ chunkLen ];
	      if( EmuUtil.read( in, cmap ) != cmap.length ) {
		break;
	      }
	    } else if( EmuUtil.isTextAt( "BODY", chunkHdr, 0 ) ) {
	      body = readIFFBody(
				in,
				chunkLen,
				height * bitplanes * bitplaneLineLen,
				compression );

	    } else {
	      in.skip( chunkLen );
	    }
	    if( (chunkLen % 2) != 0 ) {
	      in.read();
	    }
	    if( paletteOnly && (cmap != null) ) {
	      break;
	    }
	    if( (width >= 0) && (body != null)
		&& ((cmap != null) || (bitplanes > 8)) )
	    {
	      break;
	    }
	  }
	}
      }

      // IndexColorModel erzeugen, wenn Farbpalette vorhanden ist
      int[] rgbs    = null;
      int   imgType = BufferedImage.TYPE_3BYTE_BGR;
      if( bitplanes == 32 ) {
	imgType = BufferedImage.TYPE_INT_ARGB;
      }
      if( cmap != null ) {
	int colorCnt = cmap.length / 3;
	if( colorCnt > 0 ) {
	  int bits = 8;
	  imgType  = BufferedImage.TYPE_BYTE_INDEXED;
	  if( colorCnt <= 16 ) {
	    imgType = BufferedImage.TYPE_BYTE_BINARY;
	    if( colorCnt <= 2 ) {
	      bits = 1;
	    } else if( colorCnt <= 4 ) {
	      bits = 2;
	    } else {
	      bits = 4;
	    }
	  }
	  byte[] alphas = null;
	  byte[] reds   = new byte[ colorCnt ];
	  byte[] greens = new byte[ colorCnt ];
	  byte[] blues  = new byte[ colorCnt ];
	  rgbs          = new int[ colorCnt ];
	  int    srcIdx = 0;
	  int    dstIdx = 0;
	  while( ((srcIdx + 2) < cmap.length) && (dstIdx < colorCnt) ) {
	    int r = (int) cmap[ srcIdx++ ] & 0xFF;
	    int g = (int) cmap[ srcIdx++ ] & 0xFF;
	    int b = (int) cmap[ srcIdx++ ] & 0xFF;
	    reds[ dstIdx ]   = (byte) r;
	    greens[ dstIdx ] = (byte) g;
	    blues[ dstIdx ]  = (byte) b;
	    rgbs[ dstIdx ]   = 0xFF000000
					| ((r << 16) & 0x00FF0000)
					| ((g << 8) & 0x0000FF00)
					| (b & 0x000000FF);
	    dstIdx++;
	  }
	  if( (masking == 2)
	      && (transpColor > 0)
	      && (transpColor < colorCnt) )
	  {
	    alphas = new byte[ colorCnt ];
	    Arrays.fill( alphas, (byte) 0xFF );
	    alphas[ transpColor ] = (byte) 0;
	    rgbs[ transpColor ] &= 0x00FFFFFF;
	  }
	  icm = ImgUtil.createIndexColorModel(
					colorCnt,
					reds,
					greens,
					blues,
					alphas );
	}
      }

      // Bild erzeugen und Pixel setzen
      if( (width > 0) && (height > 0) && (bitplanes > 0)
	  && (bitplaneLineLen > 0) && (body != null) )
      {
	if( icm != null ) {
	  image = new BufferedImage( width, height, imgType, icm );
	} else {
	  image = new BufferedImage( width, height, imgType );
	}
	int vMask = (1 << (bitplanes - 1));
	for( int y = 0; y < height; y++ ) {
	  int begOfLine = y * bitplanes * bitplaneLineLen;
	  if( masking == 1 ) {
	    // Bitplanes fuer Maskierung ueberlesen
	    begOfLine += (y * bitplaneLineLen);
	  }
	  for( int x = 0; x < width; x++ ) {
	    int m = (0x80 >> (x % 8));
	    int v = 0;
	    for( int i = 0; i < bitplanes; i++ ) {
	      v >>= 1;
	      int idx = begOfLine + (i * bitplaneLineLen) + (x / 8);
	      if( idx < body.length ) {
		if( ((int) body[ idx ] & m) != 0 ) {
		  v |= vMask;
		}
	      }
	    }
	    boolean transparent = false;
	    if( masking == 1 ) {
	      // Masken-Bitplane auswerten
	      int idx = begOfLine + (bitplanes * bitplaneLineLen) + (x / 8);
	      if( idx < body.length ) {
		if( ((int) body[ idx ] & m) == 0 ) {
		  transparent = true;
		}
	      }
	    }
	    if( rgbs != null ) {
	      image.setRGB(
			x,
			y,
			v < rgbs.length ? rgbs[ v ] : 0x00FFFFFF );
	    } else {
	      if( (bitplanes == 24) || (bitplanes == 32) ) {
		if( bitplanes == 24 ) {
		  v |= 0xFF000000;
		}
		if( transparent ) {
		  v &= 0x00FFFFFF;
		}
		image.setRGB(
			x,
			y,
			(v & 0xFF000000)
				| ((v << 16) & 0x00FF0000)
				| (v & 0x0000FF00)
				| ((v >> 16) & 0x000000FF) );
	      } else {
		// Grauwert
		if( bitplanes > 8 ) {
		  v >>= (bitplanes - 8);
		} else if( bitplanes < 8 ) {
		  v <<= (bitplanes - 8);
		}
		if( transparent ) {
		  v &= 0x00FFFFFF;
		} else {
		  v |= 0xFF000000;
		}
		image.setRGB(
			x,
			y,
			(v & 0xFF000000)
				| ((v << 16) & 0x00FF0000)
				| ((v << 8) & 0x0000FF00)
				| (v & 0x000000FF) );
	      }
	    }
	  }
	}
      }
    }
    finally {
      EmuUtil.closeSilent( in );
    }
    return new IFFFile( image, icm );
  }


  private static byte[] readIFFBody(
				InputStream in,
				int         chunkLen,
				int         dataLen,
				int         compression ) throws IOException
  {
    byte[] data = null;
    if( compression == 0 ) {
      data = new byte[ chunkLen ];
      EmuUtil.read( in, data );
    } else if( compression == 1 ) {
      data = new byte[ dataLen ];
      Arrays.fill( data, (byte) 0 );
      int idx = 0;
      while( chunkLen > 0 ) {
	int b0 = in.read();
	if( b0 >= 0 ) {
	  --chunkLen;
	  if( b0 < 128 )  {
	    int n = b0 + 1;
	    for( int i = 0; (i < n) && (chunkLen > 0); i++ ) {
	      int b1 = in.read();
	      if( b1 < 0 ) {
		break;
	      }
	      --chunkLen;
	      if( idx < data.length ) {
		data[ idx++ ] = (byte) b1;
	      }
	    }
	  } else if( b0 > 128 ) {
	    if( chunkLen > 0 ) {
	      int n  = 257 - b0;
	      int b1 = in.read();
	      if( b1 >= 0 ) {
		--chunkLen;
		for( int i = 0; (i < n) && (idx < data.length); i++ ) {
		  data[ idx++ ] = (byte) b1;
		}
	      }
	    }
	  }
	}
      }
    } else {
      throw new IOException(
		String.format(
			"Kompressionsmethode %d nicht unterst\u00FCtzt",
			compression ) );
    }
    return data;
  }


  private static void writeFile(
			File                  file,
			ByteArrayOutputStream header,
			ByteArrayOutputStream body ) throws IOException
  {
    OutputStream out = null;
    try {
      int formLen = 4;
      if( header != null ) {
	formLen += header.size();
      }
      if( body != null ) {
	formLen += (8 + body.size());
      }
      if( (formLen % 2) != 0 ) {
	formLen++;
      }
      out = new FileOutputStream( file );
      EmuUtil.writeASCII( out, "FORM" );
      EmuUtil.writeInt4BE( out, formLen );
      EmuUtil.writeASCII( out, "ILBM" );
      if( header != null ) {
	header.writeTo( out );
      }
      if( body != null ) {
	EmuUtil.writeASCII( out, "BODY" );
	EmuUtil.writeInt4BE( out, body.size() );
	body.writeTo( out );
	if( (body.size() % 2) != 0 ) {
	  body.write( 0 );		// Fuellbyte fuer gerade Byteanzahl
	}
      }
      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilent( out );
    }
  }


	/* --- Konstruktor --- */

  private IFFFile( BufferedImage image, IndexColorModel icm )
  {
    this.image = image;
    this.icm   = icm;
  }
}
