/*
 * (c) 2013-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Laden von Bildern
 */

package jkcemu.image;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import jkcemu.base.EmuUtil;
import jkcemu.base.RFC822DateParser;
import jkcemu.emusys.KC85;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


public class ImageLoader
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
	  rv = IFFFile.accept( file );
	}
	if( !rv ) {
	  if( (file.length() == 0x4000) && name.endsWith( ".pix" ) ) {
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
    return ImageUtil.createFileFilter(
			"Unterst\u00FCtzte Bilddateien",
			ImageIO.getReaderFileSuffixes(),
			IFFFile.getFileSuffixes(),
			kc85ImgFileSuffixes,
			new String[] { "pix", "scr" } );
  }


  /*
   * Laden eines Bildes aus einer Datei
   *
   * Die beiden Methoden liefert null zurueck,
   * wenn das Dateiformat nicht unterstuetzt wird.
   * Ein OutOfMemoryError wird abgefangen und
   * dafuer in eine IOException geworfen.
   */
  public static ImageEntry load( File file ) throws IOException
  {
    return load( file, null, null );
  }


  public static ImageEntry load(
				File   file,
				byte[] fileBytes,
				String fileName ) throws IOException
  {
    ImageEntry.Mode mode     = ImageEntry.Mode.UNSPECIFIED;
    BufferedImage   image    = null;
    ExifData        exifData = null;
    if( (file != null) || (fileBytes != null) ) {
      if( IFFFile.accept( file, fileBytes ) ) {
	IFFFile iffFile = IFFFile.readImage( file, fileBytes );
	image           = iffFile.getImage();
	exifData        = iffFile.getExifData();
	mode            = ImageUtil.probeMode( image );
      } else {
	long len = 0;
	if( fileBytes != null ) {
	  len = fileBytes.length;
	} else if( file != null ) {
	  fileName = file.getName();
	  len      = file.length();
	}
	String suffix = null;
	if( fileName != null ) {
	  int pos = fileName.lastIndexOf( '.' );
	  if( (pos >= 0) && ((pos + 1) < fileName.length()) ) {
	    suffix = fileName.substring( pos + 1 ).toLowerCase();
	  }
	}
	if( suffix != null ) {
	  if( suffix.equals( "pic" ) ) {
	    byte[] irm = readKC85IRMFile( file, fileBytes, true );
	    if( irm != null ) {
	      if( (irm.length > 0) && (irm.length <= 0x2800) ) {
		mode = ImageEntry.Mode.MONOCHROME;
	      }
	      image = createImageByKC852IRM( irm );
	    }
	  } else if( file != null ) {
	    /*
	     * Bei Bilder mit den Dateiendungen pip, pif, hip und hif
	     * sind in die Pixeldaten und die Farbdaten in zwei separate
	     * Dateien gespeichert, die beide eingelesen werden muessen.
	     * Aus diesem Grund koennen die Bilder nur geladen werden,
	     * wenn der Parameter file not null ist.
	     */
	    if( suffix.equals( "pif" ) ) {
	      image = createImageByKC854IRM(
			readKC85IRMFile(
				replaceExt( file, "pip" ),
				null,
				true ),
			readKC85IRMFile( file, null, true ),
			false );
	    } else if( suffix.equals( "pip" ) ) {
	      byte[] colorBytes = null;
	      File   colorFile  = replaceExt( file, "pif" );
	      if( colorFile.exists() ) {
		colorBytes = readKC85IRMFile( colorFile, null, true );
	      } else {
		mode = ImageEntry.Mode.MONOCHROME;
	      }
	      image = createImageByKC854IRM(
			readKC85IRMFile( file, null, true ),
			colorBytes,
			false );
	    } else if( suffix.equals( "hif" ) ) {
	      mode  = ImageEntry.Mode.KC854_HIRES;
	      image = createImageByKC854IRM(
			readKC85IRMFile(
				replaceExt( file, "hip" ),
				null,
				false ),
			readKC85IRMFile( file, null, false ),
			true );
	    } else if( suffix.equals( "hip" ) ) {
	      mode  = ImageEntry.Mode.KC854_HIRES;
	      image = createImageByKC854IRM(
			readKC85IRMFile( file, null, false ),
			readKC85IRMFile(
				replaceExt( file, "hif" ),
				null,
				false ),
			true );
	    }
	  }
	}
	if( image == null ) {
	  InputStream in = null;
	  try {
	    if( fileBytes != null ) {
	      exifData = ExifParser.parseFileBytes( fileBytes );
	      in       = new ByteArrayInputStream( fileBytes );
	    } else if( file != null ) {
	      if( FileUtil.accept( file, "jpeg", "jpg" ) ) {
		/*
		 * Manche JPEG-Bilder, z.B. einige von HUAWEI-Handys,
		 * bringen beim weiter unten angewendeten Lesen
		 * direkt ueber einen ImageReader einen Fehler.
		 * Diese lassen sich jedoch ueber ImageIO.read(...) lesen.
		 */
		try {
		  in = new JPEGExifInputStream(
				new BufferedInputStream(
					new FileInputStream( file ) ) );
		  image    = ImageIO.read( in );
		  exifData = ((JPEGExifInputStream) in).getExifData();
		}
		finally {
		  EmuUtil.closeSilently( in );
		  in = null;
		}
	      } else {
		in = new BufferedInputStream( new FileInputStream( file ) );
	      }
	    }
	    if( in != null ) {
	      if( suffix != null ) {

		// LLC2-HIRES-Bild
		if( (image == null)
		    && (len == 0x4000)
		    && suffix.equals( "pix" ) )
		{
		  mode  = ImageEntry.Mode.MONOCHROME;
		  image = new BufferedImage(
					ImageUtil.LLC2_W,
					ImageUtil.LLC2_H,
					BufferedImage.TYPE_BYTE_BINARY,
					ImageUtil.getColorModelBW() );
		  ImageUtil.fillBlack( image );
		  int pos = 0;
		  int b   = in.read();
		  while( (b >= 0) && (pos < 0x4000) ) {
		    int c = pos & 0x3F;
		    int x = (c << 3);
		    int y = ((pos >> 11) & 0x07) | ((pos >> 3) & 0xF8);
		    for( int i = 0; i < 8; i++ ) {
		      if( (b & 0x80) != 0 ) {
			image.setRGB( x, y, 0xFFFFFFFF );
		      }
		      b <<= 1;
		      x++;
		    }
		    pos++;
		    b = in.read();
		  }
		}

		// A5105-Bild (Screen 5, 320x200x16)
		if( (image == null)
		    && (len > 7)
		    && suffix.equals( "scr" ) )
		{
		  byte[] header = new byte[ 7 ];
		  if( in.read( header ) == header.length ) {
		    int addr    = EmuUtil.getWord( header, 1 );
		    int endAddr = EmuUtil.getWord( header, 3 );
		    if( (((int) header[ 0 ] & 0xFF) == 0xFD)
			&& (addr >= 0x4000)
			&& (addr < endAddr)
			&& (((addr & 0x3FFF) % 80) == 0) )
		    {
		      IndexColorModel cm = ImageUtil.getColorModelA5105();
		      int             h  = (endAddr - addr + 80) / 80;
		      if( h > ImageUtil.A5105_H ) {
			h = ImageUtil.A5105_H;
		      }
		      mode  = ImageEntry.Mode.A5105;
		      image = new BufferedImage(
					ImageUtil.A5105_W,
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
			  image.setRGB( x, y, cm.getRGB( idx ) );
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
	      if( image == null ) {
		/*
		 * Datei mittels ImageReader lesen,
		 * um auch die Meta-Daten lesen zu koennen.
		 */
		ImageInputStream imgIn = null;
		try {
		  imgIn = ImageIO.createImageInputStream( in );
		  Iterator<ImageReader> iter = ImageIO.getImageReaders(
								imgIn );
		  if( iter.hasNext() ) {
		    ImageReader reader = iter.next();
		    try {
		      reader.setInput( imgIn );
		      IIOImage iioImg = reader.readAll( 0, null );
		      if( iioImg != null ) {
			RenderedImage rImg = iioImg.getRenderedImage();
			if( rImg != null ) {
			  if( rImg instanceof BufferedImage ) {
			    image = (BufferedImage) rImg;
			  } else {
			    ColorModel cm = rImg.getColorModel();
			    Raster     r  = rImg.getData();
			    if( (cm != null) && (r != null) ) {
			      WritableRaster wr = null;
			      if( r instanceof WritableRaster ) {
				wr = (WritableRaster) r;
			      } else {
				wr = r.createCompatibleWritableRaster();
			      }
			      image = new BufferedImage(
						cm,
						wr,
						false,
						new Hashtable() );
			    }
			  }
			}
			if( image != null ) {
			  mode     = ImageUtil.probeMode( image );
			  exifData = parseMetaData( iioImg.getMetadata() );
			}
		      }
		    }
		    finally {
		      reader.dispose();
		    }
		  }
		}
		finally {
		  EmuUtil.closeSilently( imgIn );
		}
	      }
	    }
	  }
	  catch( OutOfMemoryError ex ) {
	    System.gc();
	    throw new IOException(
		"Es steht nicht gen\u00FCgend Speicher zur Verf\u00FCgung." );
	  }
	  finally {
	    EmuUtil.closeSilently( in );
	  }
	}
      }
    }
    return image != null ?
		new ImageEntry(
			image,
			exifData,
			ImageEntry.Action.INITIAL_LOADED,
			mode,
			ImageFld.Rotation.NONE,
			null,
			file,
			null )
		: null;
  }


	/* --- private Methoden --- */

  private static BufferedImage createBlackKC85BWImage()
  {
    BufferedImage image = new BufferedImage(
				ImageUtil.KC85_W,
				ImageUtil.KC85_H,
				BufferedImage.TYPE_BYTE_BINARY,
				ImageUtil.getColorModelBW() );
    ImageUtil.fillBlack( image );
    return image;
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
    BufferedImage image = new BufferedImage(
				ImageUtil.KC85_W,
				ImageUtil.KC85_H,
				BufferedImage.TYPE_BYTE_INDEXED,
				new IndexColorModel( 8, n, r, g, b ) );
    ImageUtil.fillBlack( image );
    return image;
  }


  private static BufferedImage createImageByKC852IRM( byte[] irm )
  {
    BufferedImage image = null;
    if( irm != null ) {
      if( irm.length > 0x2800 ) {
	image = createBlackKC85LowresImage();
      } else {
	image = createBlackKC85BWImage();
      }
      for( int y = 0; y < ImageUtil.KC85_H; y++ ) {
	for( int x = 0; x < ImageUtil.KC85_W; x++ ) {
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
	      image.setRGB(
			x,
			y,
			KC85.getRawRGB(
				getKC85ColorIndex( irm[ cIdx ], fg ) ) );
	    } else {
	      image.setRGB(
			x,
			y,
			fg ? 0xFF000000 : 0xFFFFFFFF );
	    }
	  }
	}
      }
    }
    return image;
  }


  private static BufferedImage createImageByKC854IRM(
						byte[]  pixels,
						byte[]  colors,
						boolean hires )
  {
    BufferedImage image = null;
    if( colors != null ) {
      if( hires ) {
	image = ImageUtil.createBlackKC854HiresImage();
      } else {
	image = createBlackKC85LowresImage();
      }
    } else {
      image = createBlackKC85BWImage();
    }
    if( (image != null) && (pixels != null) ) {
      for( int y = 0; y < ImageUtil.KC85_H; y++ ) {
	for( int col = 0; col < ImageUtil.KC85_COLS; col++ ) {
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
		image.setRGB( x, y, KC85.getRawRGB( colorIdx ) );
	      } else {
		image.setRGB(
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
    return image;
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


  private static ExifData parseGIFCommentsNode( Node commentsNode )
  {
    ExifData exifData = null;
    Node     node     = commentsNode.getFirstChild();
    while( node != null ) {
      String s = node.getNodeName();
      if( s != null ) {
	if( s.equals( "CommentExtension" ) ) {
	  NamedNodeMap attrs = node.getAttributes();
	  if( attrs != null ) {
	    Node attr = attrs.getNamedItem( "value" );
	    if( attr != null ) {
	      String comment = attr.getNodeValue();
	      if( comment != null ) {
		if( !comment.isEmpty() ) {
		  exifData = new ExifData();
		  exifData.setComment( comment );
		  break;
		}
	      }
	    }
	  }
	}
      }
      node = node.getNextSibling();
    }
    return exifData;
  }


  private static ExifData parsePNGTextNode( Node textNode )
  {
    ExifData exifData = null;
    boolean  filled   = false;
    Node     node     = textNode.getFirstChild();
    while( node != null ) {
      String s = node.getNodeName();
      if( s != null ) {
	if( s.equals( "tEXtEntry" ) ) {
	  NamedNodeMap attrs = node.getAttributes();
	  if( attrs != null ) {
	    Node keyAttr   = attrs.getNamedItem( "keyword" );
	    Node valueAttr = attrs.getNamedItem( "value" );
	    if( (keyAttr != null) && (valueAttr != null) ) {
	      String keyword = keyAttr.getNodeValue();
	      String value   = valueAttr.getNodeValue();
	      if( (keyword != null) && (value != null) ) {
		if( !value.isEmpty() ) {
		  if( exifData == null ) {
		    exifData = new ExifData();
		  }
		  switch( keyword ) {
		    case "Title":
		      exifData.setDocumentName( value );
		      filled = true;
		      break;
		    case "Description":
		      exifData.setImageDesc( value );
		      filled = true;
		      break;
		    case "Author":
		      exifData.setAuthor( value );
		      filled = true;
		      break;
		    case "Copyright":
		      exifData.setCopyright( value );
		      filled = true;
		      break;
		    case "Comment":
		      exifData.setComment( value );
		      filled = true;
		      break;
		    case "Software":
		      exifData.setSoftware( value );
		      filled = true;
		      break;
		    case "Creation Time":
		      StringBuilder  tz   = new StringBuilder();
		      java.util.Date date = RFC822DateParser.parse(
								value,
								tz );
		      if( date != null ) {
			exifData.setDate(
				date,
				tz.length() > 0 ? tz.toString() : null );
		      }
		      break;
		  }
		}
	      }
	    }
	  }
	}
      }
      node = node.getNextSibling();
    }
    return filled ? exifData : null;
  }


  private static ExifData parseMetaData( IIOMetadata metadata )
  {
    ExifData exifData = null;
    if( metadata != null ) {
      try {
	String fmtName = metadata.getNativeMetadataFormatName();
	if( fmtName != null ) {
	  Node rootNode = metadata.getAsTree( fmtName );
	  if( rootNode != null ) {
	    Node node = rootNode.getFirstChild();
	    while( node != null ) {
	      String s = node.getNodeName();
	      if( s != null ) {
		if( s.equals( "CommentExtensions" ) ) {
		  exifData = parseGIFCommentsNode( node );
		}
		else if( s.equals( "tEXt" ) ) {
		  exifData = parsePNGTextNode( node );
		}
		if( exifData != null ) {
		  break;
		}
	      }
	      node = node.getNextSibling();
	    }
	  }
	}
      }
      catch( DOMException ex1 ) {}
      catch( IllegalArgumentException ex2 ) {}
    }
    return exifData;
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
				byte[]  fileBytes,
				boolean hasTypeByte ) throws IOException
  {
    byte[]      buf = null;
    InputStream in  = null;
    try {
      if( fileBytes != null ) {
	in = new ByteArrayInputStream( fileBytes );
      } else if( file != null ) {
	in = new BufferedInputStream( new FileInputStream( file ) );
      } else {
	ImageUtil.throwNoFileConent();
      }

      // Kopfblock einlesen
      byte[] header = new byte[ 0x80 ];
      if( EmuUtil.read( in, header ) != header.length ) {
	ImageUtil.throwUnsupportedFormat();
      }

      // Groesse Datenbereich
      int t = -1;
      if( hasTypeByte ) {
	t = in.read();
	if( (t < 0) || (t > 7) ) {
	  ImageUtil.throwUnsupportedFormat();
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
	    ImageUtil.throwUnsupportedFormat();
	  }
	}
      } else {
	if( ((int) header[ 16 ] & 0xFF) >= 3 ) {
	  compression = true;
	  if( (((int) header[ 17 ] & 0xFF) != 0x00)
	      || (((int) header[ 18 ] & 0xFF) != 0x40) )
	  {
	    ImageUtil.throwUnsupportedFormat();
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
	KC85ImageUnpacker unpacker = new KC85ImageUnpacker();
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
      EmuUtil.closeSilently( in );
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


	/* --- Konstruktor --- */

  private ImageLoader()
  {
    // nicht instanziierbar
  }
}
