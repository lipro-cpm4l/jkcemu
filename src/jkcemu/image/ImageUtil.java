/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer Bilder
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import jkcemu.Main;
import jkcemu.base.EmuUtil;
import jkcemu.emusys.KCcompact;
import jkcemu.emusys.a5105.VIS;
import jkcemu.file.FileUtil;


public class ImageUtil
{
  public static final int   TRANSPARENT_ARGB  = 0x00FFFFFF;
  public static final Color TRANSPARENT_COLOR = new Color( TRANSPARENT_ARGB );

  public static final int A5105_W   = 320;
  public static final int A5105_H   = 200;
  public static final int AC1_W     = 384;
  public static final int AC1_H     = 256;
  public static final int KC85_COLS = 40;
  public static final int KC85_ROWS = 32;
  public static final int KC85_W    = KC85_COLS * 8;
  public static final int KC85_H    = KC85_ROWS * 8;
  public static final int LLC2_COLS = 64;
  public static final int LLC2_ROWS = 32;
  public static final int LLC2_W    = LLC2_COLS * 8;
  public static final int LLC2_H    = LLC2_ROWS * 8;
  public static final int LLC2_H2   = LLC2_H / 2 * 3;	// entzerrte Hoehe
  public static final int Z1013_W   = 256;
  public static final int Z1013_H   = 256;
  public static final int Z9001_W   = 320;
  public static final int Z9001_H   = 192;

  public static final int ROUND_PIXELS_MAX = 9;

  private static IndexColorModel cmBW         = null;
  private static IndexColorModel cmSortedGray = null;
  private static IndexColorModel cmA5105      = null;
  private static IndexColorModel cmCPC        = null;
  private static IndexColorModel cmKC854Hires = null;


  public static File chooseColorPaletteFile(
				Window owner,
				File   initialFile ) throws IOException
  {
    // Dateifilter erzeugen
    java.util.List<String> usedSuffixes = new ArrayList<>();
    String[]               iioSuffixes  = ImageIO.getReaderFileSuffixes();
    if( iioSuffixes != null ) {
      final String[] sortedPossibleSuffixes = {
				"bmp", "gif", "png", "tif", "tiff" };
      for( String s : iioSuffixes ) {
	s = s.toString();
	if( Arrays.binarySearch( sortedPossibleSuffixes, s ) >= 0 ) {
	  usedSuffixes.add( s );
	}
      }
    }

    // Vorauswahl
    if( initialFile == null ) {
      initialFile = Main.getLastDirFile( Main.FILE_GROUP_IMAGE );
    }

    // Dialog anzeigen
    return FileUtil.showFileOpenDlg(
	owner,
	"Farbpalette importieren",
	initialFile,
	createFileFilter(
		"Unterst\u00FCtzte Farbpaletten- und Bilddateien",
		usedSuffixes.toArray( new String[ usedSuffixes.size() ] ),
		IFFFile.getFileSuffixes(),
		JASCPaletteFile.getFileSuffixes() ) );
  }


  public static void copyToClipboard( Component owner, Image image )
  {
    try {
      if( image != null ) {
	Toolkit tk = EmuUtil.getToolkit( owner );
	if( tk != null ) {
	  Clipboard clp = tk.getSystemClipboard();
	  if( clp != null ) {
	    TransferableImage tImg = new TransferableImage( image );
	    clp.setContents( tImg, tImg );
	  }
	}
      }
    }
    catch( IllegalStateException ex ) {}
  }


  public static BufferedImage createBlackKC854HiresImage()
  {
    BufferedImage image = new BufferedImage(
				KC85_W,
				KC85_H,
				BufferedImage.TYPE_BYTE_BINARY,
				getColorModelKC854Hires() );
    fillBlack( image );
    return image;
  }


  public static BufferedImage createBlackKC85BWImage()
  {
    BufferedImage image = new BufferedImage(
				KC85_W,
				KC85_H,
				BufferedImage.TYPE_BYTE_BINARY,
				getColorModelBW() );
    fillBlack( image );
    return image;
  }


  public static BufferedImage createCompatibleImage(
					BufferedImage srcImg,
					int           w,
					int           h )
  {
    BufferedImage retImg = null;
    if( srcImg != null ) {
      if( w < 1 ) {
	w = srcImg.getWidth();
      }
      if( h < 1 ) {
	h = srcImg.getHeight();
      }
      if( (w > 0) && (h > 0) ) {
	IndexColorModel icm = getIndexColorModel( srcImg );
	if( icm != null ) {
	  retImg = new BufferedImage(
				w,
				h,
				icm.getMapSize() > 16 ?
					BufferedImage.TYPE_BYTE_INDEXED
					: BufferedImage.TYPE_BYTE_BINARY );
	}
	if( retImg == null ) {
	  retImg = new BufferedImage(
			w,
			h,
			srcImg.getTransparency() == Transparency.OPAQUE ?
					BufferedImage.TYPE_3BYTE_BGR
					: BufferedImage.TYPE_INT_ARGB );
	}
      }
    }
    return retImg;
  }


  public static FileNameExtensionFilter createFileFilter(
						String      text,
						String[]... suffixes )
  {
    SortedSet<String> sortedSuffixes = new TreeSet<>();
    if( suffixes != null ) {
      for( String[] a : suffixes ) {
	if( a != null ) {
	  for( String s : a ) {
	    if( s != null ) {
	      sortedSuffixes.add( s.toLowerCase() );
	    }
	  }
	}
      }
    }
    if( !sortedSuffixes.isEmpty() ) {
      StringBuilder buf = new StringBuilder( 256 );
      buf.append( text );
      boolean isFirst = true;
      for( String s : sortedSuffixes ) {
	if( isFirst ) {
	  buf.append( " (" );
	  isFirst = false;
	} else {
	  buf.append( "; " );
	}
	buf.append( "*." );
	buf.append( s );
      }
      if( !isFirst ) {
	buf.append( ')' );
      }
      text = buf.toString();
    }
    return new FileNameExtensionFilter(
			text,
			sortedSuffixes.toArray(
				new String[ sortedSuffixes.size() ] ) );
  }


  public static String createFileSuffixNotSupportedMsg(
						String[]... suffixes )
  {
    StringBuilder buf = new StringBuilder( 512 );
    buf.append( "Das durch die Dateiendung angegebene Format"
	+ " wird nicht unterst\u00FCtzt." );

    String suffixesText = createFileSuffixesText( suffixes );
    if( !suffixesText.isEmpty() ) {
      buf.append( "\nFolgende Dateiendungen sind m\u00F6glich:\n" );
      buf.append( suffixesText );
    }
    return buf.toString();
  }


  public static IndexColorModel createIndexColorModel(
					Collection<Integer> colors )
  {
    int     colorCnt = colors.size();
    byte[]  reds     = new byte[ colorCnt ];
    byte[]  greens   = new byte[ colorCnt ];
    byte[]  blues    = new byte[ colorCnt ];
    byte[]  alphas   = new byte[ colorCnt ];
    boolean hasAlpha = false;
    int     idx      = 0;
    for( Integer v : colors ) {
      if( idx >= colorCnt ) {
	break;
      }
      int argb = 0;
      if( v != null ) {
	argb = v.intValue();
      }
      int alpha = (argb >> 24) & 0xFF;
      if( alpha < 0xFF ) {
	hasAlpha = true;
      }
      alphas[ idx ] = (byte) alpha;
      reds[ idx ]   = (byte) ((argb >> 16) & 0xFF);
      greens[ idx ] = (byte) ((argb >> 8) & 0xFF);
      blues[ idx ]  = (byte) (argb & 0xFF);
      idx++;
    }
    if( !hasAlpha ) {
      alphas = null;
    }
    return createIndexColorModel( colorCnt, reds, greens, blues, alphas );
  }


  public static IndexColorModel createIndexColorModel(
						int    n,
						byte[] reds,
						byte[] greens,
						byte[] blues,
						byte[] alphas )
  {
    int bits = 8;
    if( n <= 16 ) {
      if( n <= 2 ) {
	bits = 1;
      } else if( n <= 4 ) {
	bits = 2;
      } else {
	bits = 4;
      }
    }
    return alphas != null ?
		new IndexColorModel( bits, n, reds, greens, blues, alphas )
		: new IndexColorModel( bits, n, reds, greens, blues );
  }


  public static BufferedImage createIndexedColorsImage(
					int                 w,
					int                 h,
					Collection<Integer> colors )
  {
    IndexColorModel icm =  createIndexColorModel( colors );
    return new BufferedImage(
			w,
			h,
			icm.getMapSize() > 16 ?
				BufferedImage.TYPE_BYTE_INDEXED
				: BufferedImage.TYPE_BYTE_BINARY,
			icm );
  }


  public static FileNameExtensionFilter createA5105ImageFileFilter()
  {
    return new FileNameExtensionFilter(
				"A5105-Bilddateien",
				"scr" );
  }


  public static FileNameExtensionFilter createKC852ImageFileFilter()
  {
    return new FileNameExtensionFilter(
				"KC85/2,3-Bilddateien",
				"pic" );
  }


  public static FileNameExtensionFilter createKC854HiresImageFileFilter()
  {
    return new FileNameExtensionFilter(
				"KC85/4,5-HIRES-Bilddateien",
				"hip" );
  }


  public static FileNameExtensionFilter createKC854LowresImageFileFilter()
  {
    return new FileNameExtensionFilter(
				"KC85/4,5-LOWRES-Bilddateien",
				"pip" );
  }


  public static FileNameExtensionFilter createLLC2HiresImageFileFilter()
  {
    return new FileNameExtensionFilter(
				"LLC2-HIRES-Bilddateien",
				"pix" );
  }


  /*
   * Die Methode erzeugt die Bytes fuer den
   * KC85/4-Bildwiederholspeicher im HIRES-Modus.
   * Die erzeugten Bytes werden in die beiden uebergebenen Puffer
   * fuer Pixel- und den Farbbereich geschrieben.
   */
  public static void createKC854HiresMemBytes(
				BufferedImage image,
				byte[]        pixelBuf,
				byte[]        colorBuf )
  {
    IndexColorModel      icm     = getColorModelKC854Hires();
    Map<Integer,Integer> rgb2Idx = new HashMap<>();

    int pos = 0;
    int w   = image.getWidth();
    int h   = image.getHeight();
    int chW = KC85_W / KC85_COLS;
    for( int col = 0; col < KC85_COLS; col++ ) {
      for( int y = 0; y < KC85_H; y++ ) {
	int x = col * 8;
	int p = 0;
	int c = 0;
	int m = 0x80;
	for( int i = 0; i < chW; i++ ) {
	  int rgb = 0;
	  if( (x < w) && (y < h) ) {
	    rgb = image.getRGB( x, y );
	  }
	  Integer idx = rgb2Idx.get( rgb );
	  if( idx == null ) {
	    idx = getNearestIndex( icm, rgb );
	    rgb2Idx.put( rgb, idx );
	  }
	  int v = idx.intValue();
	  if( (v & 0x01) != 0 ) {
	    p |= m;
	  }
	  if( (v & 0x02) != 0 ) {
	    c |= m;
	  }
	  m >>= 1;
	  x++;
	}
	if( pos < pixelBuf.length ) {
	  pixelBuf[ pos ] = (byte) p;
	}
	if( pos < colorBuf.length ) {
	  colorBuf[ pos ] = (byte) c;
	}
	pos++;
      }
    }
  }


  /*
   * Die Methode erzeugt die Pixelbytes (schwarz/weiss)
   * fuer den KC85/2-Bildwiederholspeicher.
   * Die erzeugten Bytes werden in den uebergebenen Puffer geschrieben.
   */
  public static void createKC852MonochromeMemBytes(
				BufferedImage image,
				byte[]        pixelBuf )
  {
    Map<Integer,Integer> rgb2Idx = new HashMap<>();

    int w   = image.getWidth();
    int h   = image.getHeight();
    int chW = KC85_W / KC85_COLS;
    for( int y = 0; y < KC85_H; y++ ) {
      for( int col = 0; col < KC85_COLS; col++ ) {
	int pos = 0;
	if( col < 32 ) {
	  pos = ((y << 7) & 0x0180)		// Y0-Y1
		| ((y << 3) & 0x0060)		// Y2-Y3
		| ((y << 5) & 0x1E00)		// Y4-Y7
		| (col & 0x001F);		// COL0-COL5
	} else {
	  pos = ((y << 7) & 0x0180)		// Y0-Y1
		| ((y << 3) & 0x0660)		// Y2-Y3,Y6-Y7
		| ((y >> 1) & 0x0018)		// Y4-Y5
		| (col & 0x0007)		// COL0-COL2
		| 0x2000;
	}
	int x = col * 8;
	int p = 0;
	int m = 0x80;
	for( int i = 0; i < chW; i++ ) {
	  int rgb = 0;
	  if( (x < w) && (y < h) ) {
	    rgb = image.getRGB( x, y );
	  }
	  if( (GrayScaler.toGray( rgb ) & 0xFF) < 0x80 ) {
	    p |= m;
	  }
	  m >>= 1;
	  x++;
	}
	if( pos < pixelBuf.length ) {
	  pixelBuf[ pos ] = (byte) p;
	}
	pos++;
      }
    }
  }


  /*
   * Die Methode erzeugt die Pixelbytes (schwarz/weiss)
   * fuer den KC85/4-Bildwiederholspeicher im LOWRES-Modus.
   * Die erzeugten Bytes werden in den uebergebenen Puffer geschrieben.
   */
  public static void createKC854MonochromeMemBytes(
				BufferedImage image,
				byte[]        pixelBuf )
  {
    Map<Integer,Integer> rgb2Idx = new HashMap<>();

    int pos = 0;
    int w   = image.getWidth();
    int h   = image.getHeight();
    int chW = KC85_W / KC85_COLS;
    for( int col = 0; col < KC85_COLS; col++ ) {
      for( int y = 0; y < KC85_H; y++ ) {
	int x = col * 8;
	int p = 0;
	int m = 0x80;
	for( int i = 0; i < chW; i++ ) {
	  int rgb = 0;
	  if( (x < w) && (y < h) ) {
	    rgb = image.getRGB( x, y );
	  }
	  if( (GrayScaler.toGray( rgb ) & 0xFF) < 0x80 ) {
	    p |= m;
	  }
	  m >>= 1;
	  x++;
	}
	if( pos < pixelBuf.length ) {
	  pixelBuf[ pos ] = (byte) p;
	}
	pos++;
      }
    }
  }


  /*
   * Die Methode erzeugt die Bytes fuer den
   * LLC2-HIRES-Bildwiederholspeicher.
   * Es wird davon ausgegangen, dass das uebergebene Bild
   * dem LLC2-HIRES-Format entspricht.
   */
  public static byte[] createLLC2HiresMemBytes( BufferedImage image )
  {
    byte[] buf = new byte[ LLC2_COLS * LLC2_H ];
    int    pos = 0;
    int    w   = image.getWidth();
    int    h   = image.getHeight();
    int    chH = LLC2_H / LLC2_ROWS;
    int    chW = LLC2_W / LLC2_COLS;
    for( int pRow = 0; pRow < chH; pRow++ ) {
      for( int cRow = 0; cRow < LLC2_ROWS; cRow++ ) {
	int x = 0;
	int y = (cRow * chH) + pRow;
	for( int cCol = 0; cCol < LLC2_COLS; cCol++ ) {
	  int b = 0;
	  for( int pCol = 0; pCol < chW; pCol++ ) {
	    b <<= 1;
	    int rgb = 0;
	    if( (x < w) && (y < h) ) {
	      rgb = image.getRGB( x++, y );
	    }
	    if( (((rgb >> 16) & 0xFF)
		      + ((rgb >> 8) & 0xFF)
		      + (rgb & 0xFF)) >= 0x180 )
	    {
	      b |= 0x01;
	    }
	  }
	  buf[ pos++ ] = (byte) b;
	}
      }
    }
    return buf;
  }


  public static ExifData createScreenshotExifData()
  {
    ExifData exifData = new ExifData( false );
    exifData.setComment( Main.APPNAME + " Screenshot" );
    return exifData;
  }


  public static void ensureImageLoaded( Component owner, Image image )
  {
    if( image != null ) {
      boolean done  = false;
      Toolkit tk    = EmuUtil.getToolkit( owner );
      if( tk != null ) {
	int flags = tk.checkImage( image, -1, -1, owner );
	if( (flags & (ImageObserver.ABORT
			| ImageObserver.ALLBITS
			| ImageObserver.ERROR
			| ImageObserver.FRAMEBITS)) != 0 )
	{
	  done = true;
	}
      }
      if( !done ) {
	MediaTracker mt = new MediaTracker( owner );
	mt.addImage( image, 0 );
	try {
	  mt.waitForID( 0 );
	}
	catch( InterruptedException ex ) {}
      }
    }
  }


  public static void fillBlack( BufferedImage image )
  {
    Graphics g = image.createGraphics();
    g.setColor( Color.BLACK );
    g.fillRect( 0, 0, image.getWidth(), image.getHeight() );
    g.dispose();
  }


  public synchronized static IndexColorModel getColorModelA5105()
  {
    if( cmA5105 == null ) {
      cmA5105 = VIS.createColorModel( 1F );
    }
    return cmA5105;
  }


  public synchronized static IndexColorModel getColorModelCPC()
  {
    if( cmCPC == null ) {
      byte[] r = new byte[ KCcompact.rawRGBValues.length ];
      byte[] g = new byte[ KCcompact.rawRGBValues.length ];
      byte[] b = new byte[ KCcompact.rawRGBValues.length ];
      for( int i = 0; i < KCcompact.rawRGBValues.length; i++ ) {
	r[ i ] = (byte) (KCcompact.rawRGBValues[ i ] >> 16);
	g[ i ] = (byte) (KCcompact.rawRGBValues[ i ] >> 8);
	b[ i ] = (byte) KCcompact.rawRGBValues[ i ];
      }
      cmCPC = new IndexColorModel(
			8,
			KCcompact.rawRGBValues.length,
			r,
			g,
			b );
    }
    return cmCPC;
  }


  public synchronized static IndexColorModel getColorModelBW()
  {
    if( cmBW == null ) {
      cmBW = new IndexColorModel(
			1,
			2,
			new byte[] { 0, (byte) 0xFF },
			new byte[] { 0, (byte) 0xFF },
			new byte[] { 0, (byte) 0xFF } );
    }
    return cmBW;
  }


  public synchronized static IndexColorModel getColorModelSortedGray()
  {
    if( cmSortedGray == null ) {
      byte[] a = new byte[ 256 ];
      for( int i = 0; i < a.length; i++ ) {
	a[ i ] = (byte) i;
      }
      cmSortedGray = new IndexColorModel( 8, 256, a, a, a );
    }
    return cmSortedGray;
  }


  public synchronized static IndexColorModel getColorModelKC854Hires()
  {
    if( cmKC854Hires == null ) {
      cmKC854Hires = new IndexColorModel(
		    8,
		    4,
		    new byte[] { 0, (byte) 255,          0, (byte) 255 },
		    new byte[] { 0,          0, (byte) 255, (byte) 255 },
		    new byte[] { 0,          0, (byte) 255, (byte) 255 } );
    }
    return cmKC854Hires;
  }


  public static IndexColorModel getIndexColorModel( BufferedImage image )
  {
    IndexColorModel icm = null;
    if( image != null ) {
      ColorModel cm = image.getColorModel();
      if( cm != null ) {
	if( cm instanceof IndexColorModel ) {
	  icm = (IndexColorModel) cm;
	}
      }
    }
    return icm;
  }


  /*
   * Die beiden Methoden suchen in einem IndexColorModel den Index
   * der Farbe, die dem uebergebenen RGB-Wert am naechsten kommt.
   */
  public static int getNearestIndex(
				IndexColorModel icm,
				int             r,
				int             g,
				int             b )
  {
    int    idx    = -1;
    long   diff   = 0;
    int    size   = icm.getMapSize();
    byte[] reds   = new byte[ size ];
    byte[] greens = new byte[ size ];
    byte[] blues  = new byte[ size ];
    icm.getReds( reds );
    icm.getGreens( greens );
    icm.getBlues( blues );
    for( int i = 0; i < size; i++ ) {
      long diffR = Math.abs( ((int) reds[ i ] & 0xFF) - r );
      long diffG = Math.abs( ((int) greens[ i ] & 0xFF) - g );
      long diffB = Math.abs( ((int) blues[ i ] & 0xFF) - b );
      long d     = (diffR * diffR) + (diffG * diffG) + (diffB * diffB);
      if( (idx < 0) || (d < diff) ) {
	idx  = i;
	diff = d;
      }
    }
    return idx >= 0 ? idx : 0;
  }


  public static int getNearestIndex( IndexColorModel icm, int rgb )
  {
    return getNearestIndex(
			icm,
			(rgb >> 16) & 0xFF,
			(rgb >> 8) & 0xFF,
			rgb & 0xFF );
  }


  public static ImageEntry.Mode probeMode( BufferedImage image )
  {
    ImageEntry.Mode mode = ImageEntry.Mode.UNSPECIFIED;
    if( image != null ) {
      IndexColorModel icm = getIndexColorModel( image );
      if( icm != null ) {
	int n = icm.getMapSize();
	if( n > 0 ) {
	  boolean gray = true;
	  boolean mono = true;
	  for( int i = 0; i < n; i++ ) {
	    int rgb = icm.getRGB( i );
	    int c   = rgb & 0xFF;
	    if( (c != ((rgb >> 16) & 0xFF)) || (c != ((rgb >> 8) & 0xFF)) ) {
	      gray = false;
	      mono = false;
	      break;
	    }
	    if( (c != 0) && (c != 0xFF) ) {
	      mono = false;
	    }
	  }
	  if( mono ) {
	    mode = ImageEntry.Mode.MONOCHROME;
	  } else if( gray ) {
	    mode = ImageEntry.Mode.GRAY;
	  }
	}
      } else {
	int imgType = image.getType();
	if( (imgType == BufferedImage.TYPE_BYTE_GRAY)
	    || (imgType == BufferedImage.TYPE_USHORT_GRAY) )
	{
	  mode = ImageEntry.Mode.GRAY;
	}
      }
    }
    return mode;
  }


  public static IndexColorModel readColorPaletteFile( File file )
							throws IOException
  {
    IndexColorModel icm = null;
    if( IFFFile.accept( file ) ) {
      icm = IFFFile.readPalette( file );
    } else if( JASCPaletteFile.accept( file ) ) {
      icm = JASCPaletteFile.read( file );
    } else {
      ImageEntry entry = ImageLoader.load( file );
      if( entry != null ) {
	icm = getIndexColorModel( entry.getImage() );
      }
    }
    return icm;
  }


  public static BufferedImage roundCorners(
					Window        owner,
					BufferedImage image,
					int           nTopPixels,
					int           nBottomPixels )
  {
    BufferedImage newImg = null;
    if( (image != null) && ((nTopPixels > 0) || (nBottomPixels > 0)) ) {
      int w = image.getWidth();
      int h = image.getHeight();
      if( (w > 0) && (h > 0) ) {
	/*
	 * Neues BufferedImage mit Transparenz anlegen,
	 * Wenn das Image ein IndexColorModel mit mindestens
	 * einem volltransparenten Pixel hat,
	 * dann dieses verwenden.
	 */
	IndexColorModel icm = getIndexColorModel( image );
	if( icm != null ) {
	  if( icm.getTransparentPixel() >= 0 ) {
	    int imgType = image.getType();
	    if( imgType != BufferedImage.TYPE_BYTE_BINARY ) {
	      imgType = BufferedImage.TYPE_BYTE_INDEXED;
	    }
	    newImg = new BufferedImage( w, h, imgType, icm );
	  }
	}
	if( newImg == null ) {
	  newImg = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
	}
	if( ImageCopier.work(
			owner,
			"Runde Ecken ab...",
			image,
			newImg ) )
	{
	  int m = 2 * Math.max( nTopPixels, nBottomPixels );
	  if( (w > m) && (h > m) ) {
	    int r = nTopPixels - 1;
	    for( int x = 0; x < nTopPixels; x++ ) {
	      int dx = nTopPixels - 1 - x;
	      for( int y = 0; y < nTopPixels; y++ ) {
		int    dy = nTopPixels - 1 - y;
		double d  = Math.sqrt( (double) ((dx * dx) + (dy * dy)) );
		if( Math.round( d ) >= r ) {
		  newImg.setRGB( x, y, 0 );
		  newImg.setRGB( w - x - 1, y, 0 );
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
		  newImg.setRGB( x, h - y - 1, 0 );
		  newImg.setRGB( w - x - 1, h - y - 1, 0 );
		}
	      }
	    }
	  }
	} else {
	  newImg = null;
	}
      }
    }
    return newImg;
  }


  public static void throwNoColorTabInFile() throws IOException
  {
    throw new IOException( "Die Datei enth\u00E4lt keine Farbpalette." );
  }


  public static void throwNoFileConent() throws IOException
  {
    throw new IOException( "Datei ohne Inhalt" );
  }


  public static void throwUnsupportedFormat() throws IOException
  {
    throw new IOException( "Dateiformat nicht unterst\u00FCtzt" );
  }


  public static byte[] toPackBits(
				byte[]  srcBytes,
				boolean kc85 ) throws IOException
  {
    ByteArrayOutputStream buf = new ByteArrayOutputStream(
			srcBytes.length > 32 ?  srcBytes.length : 32 );
    writePackBits( buf, srcBytes, kc85 );
    buf.write( 0x80 );			// Endekennung
    return buf.toByteArray();
  }


  /*
   * Der PackBits-Algorithmus erzeugt eine Datei,
   * die aus einzelnen Bereichen besteht.
   * Das jeweile erstes Byte eines solchen Bereichs hat folgende Bedeutung:
   *   00h..7Fh: (Byte + 1) gibt die Anzahl der nachfolgenden Bytes,
   *             die kopiert werden
   *             (Bei 0 werden 1 Byte und bei 0x7F 128 Bytes kopiert.)
   *   80h:      Ende der gepackten Datei
   *   81h..FFh: (257 - Byte) gibt die Anzahl an,
   *             die das nachfolgende Byte vervielfaeltigt wird
   *             (0x81: 128 mal, 0xFF: 2 mal)
   *
   * Beim KC85/2..5 muessen zwei aufeinanderfolgende Bytes
   * mit dem Wert 7Fh  vermieden werden,
   * da diese auf einer 100h-Adresse einen Kommando einleiten.
   * Aus diesem Grund wird beim Byte 7Fh immer ein einzelner Kopierbereich
   * erzeugt und ausserdem sichergestellt,
   * dass keine 127 Bytes in einem Bereich kopiert werden.
   */
  public static void writePackBits(
				OutputStream out,
				byte[]       buf,
				boolean      kc85 ) throws IOException
  {
    int maxCopiedBytes = (kc85 ? 127 : 128);
    int idx            = 0;
    while( idx < buf.length ) {

      // naechstes Vorkommen von min. 3 gleichen Bytes suchen
      int idx1 = idx;
      int idx2 = idx;
      while( idx1 < buf.length ) {
	idx2 = idx1 + 1;
	while( idx2 < buf.length ) {
	  if( kc85 && (buf[ idx1 ] == 0x7F) ) {
	    break;
	  }
	  if( buf[ idx1 ] != buf[ idx2 ] ) {
	    break;
	  }
	  idx2++;
	}
	if( (idx2 - idx1) > 2 ) {
	  break;		// gleiche Bytes gefunden: x1 bis x2
	}
	idx1++;
      }
      int nFound = idx2 - idx1;
      if( nFound < 2 ) {
	idx1 = buf.length;
	idx2 = idx1;
      }

      // Bytes kopieren
      int nCopy = idx1 - idx;
      if( nCopy > 0 ) {
	if( nCopy > maxCopiedBytes ) {
	  nCopy  = maxCopiedBytes;
	  idx1   = idx + nCopy;
	  idx2   = idx1;
	  nFound = 0;		// keine Kompression in diesem Durchlauf
	}
	out.write( nCopy - 1 );
	while( idx < idx1 ) {
	  out.write( buf[ idx++ ] );
	}
      }

      // Komprimieren
      if( nFound > 1 ) {
	if( nFound > 128 ) {
	  nFound = 128;
	  idx2   = idx1 + nFound;
	}
	out.write( 257 - nFound );
	out.write( buf[ idx1 ] );
	idx = idx2;
      }
    }
  }


  public static void writeToFile( byte[] data, File file ) throws IOException
  {
    if( data == null ) {
      throw new IOException(
		"Das Bild kann nicht in dem Format exportiert werden." );
    }
    OutputStream out = null;
    try {
      out = new FileOutputStream( file );
      out.write( data );
      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilently( out );
    }
  }


	/* --- private Methoden --- */

  public static String createFileSuffixesText( String[]... suffixes )
  {
    StringBuilder     buf            = new StringBuilder( 256 );
    SortedSet<String> sortedSuffixes = new TreeSet<>();
    if( suffixes != null ) {
      for( String[] a : suffixes ) {
	if( a != null ) {
	  for( String s : a ) {
	    if( s != null ) {
	      sortedSuffixes.add( s.toLowerCase() );
	    }
	  }
	}
      }
    }
    if( !sortedSuffixes.isEmpty() ) {
      for( String s : sortedSuffixes ) {
	if( buf.length() > 0 ) {
	  buf.append( "; " );
	}
	buf.append( "*." );
	buf.append( s );
      }
    }
    return buf.toString();
  }


	/* --- Konstruktor --- */

  private ImageUtil()
  {
    // leer
  }
}
