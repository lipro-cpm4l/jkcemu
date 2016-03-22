/*
 * (c) 2010-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erzeugen animierter GIF-Dateien
 */

package jkcemu.image;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Arrays;


public class AnimatedGIFWriter implements ImageObserver
{
  public static class FrameData
  {
    public int    delayMillis;
    public int    colorDepth;
    public int    colorTabSize;
    public byte[] colorTab;
    public byte[] pixels;

    public FrameData(
		int    colorDepth,
		int    colorTabSize,
		byte[] colorTab,
		byte[] pixels )
    {
      this.delayMillis  = 0;
      this.colorDepth   = colorDepth;
      this.colorTabSize = colorTabSize;
      this.colorTab     = colorTab;
      this.pixels       = pixels;
    }
  };


  /*
   * Farbreduktion von 256 auf 6 Werte pro Primaerfarbe:
   * Der Farbwert (0-255) wird durch 43 dividiert (ergibt 0-5),
   * und anschliessend ueber die Tabelle wieder in den Wertebereich
   * 0-255 gemappt.
   * Man koennte im zweiten Schritt auch einfach wieder
   * mit 43 multiplizieren, nur wuerden dann die Farben immer
   * auf die untere Grenze des jeweiligen Bereichs gemappt werden,
   * was das Bild etwas dunkler erscheinen laesst.
   * Mit Hilfe der Tabelle wird dieser Effekt unterdrueckt.
   */
  private static final int[] color6Map = { 0, 51, 102, 153, 204, 255 };

  private OutputStream    out;
  private boolean         infinite;
  private boolean         firstFrame;
  private boolean         forceColorReduction;
  private boolean         smoothColorReduction;
  private int             curTransparencyIdx;
  private int             curColorDepth;
  private int             curColorTabSize;
  private byte[]          curColorTab;
  private byte[]          curPixels;
  private int             width;
  private int             height;
  private int             globalColorDepth;
  private int             globalColorTabSize;
  private byte[]          globalColorTab;
  private IndexColorModel indexColorModel255;
  private FrameData       prevFrame;


  public AnimatedGIFWriter(
		OutputStream out,
		boolean      smoothColorReduction,
		boolean      infinite ) throws IOException
  {
    this.out                  = out;
    this.infinite             = infinite;
    this.smoothColorReduction = smoothColorReduction;
    this.forceColorReduction  = false;
    this.firstFrame           = true;
    this.curTransparencyIdx   = -1;
    this.curColorDepth        = 0;
    this.curColorTabSize      = 0;
    this.curColorTab          = null;
    this.curPixels            = null;
    this.width                = 0;
    this.height               = 0;
    this.globalColorDepth     = 0;
    this.globalColorTabSize   = 0;
    this.globalColorTab       = null;
    this.indexColorModel255   = null;
    this.prevFrame            = null;
    writeASCII( "GIF89a" );
  }


  /*
   * Die Methode fuegt ein einzelnes Bild (Frame) hinzu.
   * Dieses wird nicht sofort in die Datei geschrieben,
   * sondern nur zwischengespeichert,
   * da erst beim naechsten Frame die Wartezeit dieses Frames
   * ermittelt werden kann.
   * Des Weiteren wird dieses Frame mit dem vorherigen verglichen.
   * Wenn beide gleich sind, wird von diesem Frame nur die Anzeigezeit
   * auf das vorherige Frame addiert.
   */
  public void addFrame(
			int           millisBefore,
			BufferedImage image ) throws IOException
  {
    boolean rv = false;
    if( this.out != null ) {
      if( this.prevFrame != null ) {
	this.prevFrame.delayMillis += millisBefore;
      }
      FrameData frame = createFrameData( image );
      if( frame != null ) {
	if( this.prevFrame != null ) {
	  if( (frame.colorDepth != this.prevFrame.colorDepth)
	      || (frame.colorTabSize != this.prevFrame.colorTabSize)
	      || !Arrays.equals( frame.colorTab, this.prevFrame.colorTab )
	      || !Arrays.equals( frame.pixels, this.prevFrame.pixels ) )
	  {
	    writeFrame( this.prevFrame );
	    this.prevFrame = frame;
	  }
	} else {
	  this.width              = image.getWidth();
	  this.height             = image.getHeight();
	  this.globalColorDepth   = frame.colorDepth;
	  this.globalColorTabSize = frame.colorTabSize;
	  this.globalColorTab     = frame.colorTab;
	  this.prevFrame          = frame;
	}
      }
    }
    image.flush();
  }


  /*
   * Die Methode schreibt alle gepufferten Daten und beendet
   * das animierte GIF-Bild.
   * Der OutputStream wird nicht geschlossen.
   */
  public boolean finish() throws IOException
  {
    boolean rv = false;
    if( this.out != null ) {
      try {
	if( this.prevFrame != null ) {
	  writeFrame( this.prevFrame );
	  this.prevFrame = null;
	  this.out.write( 0x3B );		// GIF-Endekennung
	  this.out.flush();
	  rv = true;
	}
      }
      finally {
	this.out = null;
      }
    }
    return rv;
  }


	/* --- ImageObserver --- */

  @Override
  public boolean imageUpdate(
			Image image,
			int   flags,
			int   x,
			int   y,
			int   width,
			int   height)
  {
    return (flags & (ALLBITS | ERROR | ABORT)) == 0;
  }


	/* --- private Methoden --- */

  /*
   * Die Methode erzeugt aus einem Bild ein FrameData-Objekt,
   * in dem die Bild- und Farbdaten im benoetigten Format vorliegen.
   * Ist das nicht moeglich, wird null zurueckgeliefert.
   */
  private FrameData createFrameData( BufferedImage image )
  {
    FrameData rv         = null;
    this.curColorDepth   = 0;
    this.curColorTabSize = 0;
    this.curColorTab     = null;
    this.curPixels       = null;
    if( image != null ) {
      int w = image.getWidth();
      int h = image.getHeight();
      if( (w > 0) && (h > 0) ) {
	if( (this.width < 1) || (this.height < 1) ) {
	  this.width  = w;
	  this.height = h;
	}
	if( (w == this.width) && (h == this.height) ) {
	  /*
	   * Pruefen, ob die Bilddaten im benoetigten Format vorliegen,
	   * und wenn ja, dann diese auch entsprechend auslesen
	   */
	  extractCompatibleImageData( image );
	}
	if( ((this.curColorDepth < 1) || (this.curColorTabSize < 1)
		    || (this.curColorTab == null) || (this.curPixels == null))
	    && (!this.forceColorReduction || !this.smoothColorReduction) )
	{
	  /*
	   * Die Bilddaten liegen nicht im benoetigen Format vor.
	   * Wenn die Anzahl der Farben 256 nicht uebersteigt,
	   * kann man diese verlustfrei konvertieren.
	   * Dazu werden alle Pixel ausgelesen und eine
	   * Farb- sowie eine Pixelindextabelle aufgebaut.
	   * Bei der 257. Farbe wird das ganze noch einmal gemacht,
	   * allerdings mit Farbreduktion auf einen 6:6:6-Wuerfel,
	   * so dass max. 6^3=216 Farben uebrig bleiben koennen.
	   * Wenn bei einem Frame die Farbreduktion notwendig ist,
	   * wird davon ausgegangen, dass das auch bei den nachfolgenden
	   * Frames so sein wird und deshalb aus Gruenden der Performance
	   * gleich die verlustbehaftete Konvertierung gestartet wird.
	   */
	  int    tabPos       = 0;
	  int[]  sortedRGBs   = new int[ 256 ];
	  byte[] colorIndexes = new byte[ 256 ];
	  byte[] colorTab     = new byte[ 3 * 256 ];
	  byte[] pixels       = new byte[ this.width * this.height ];
	  do {
	    int nColors  = 0;
	    int pixelPos = 0;
	    for( int y = 0; (nColors >= 0) && (y < this.height); y++ ) {
	      for( int x = 0; x < this.width; x++ ) {
		int rgb = 0xFF000000;			// schwarz
		if( (x < w) && (y < h) ) {
		  rgb = image.getRGB( x, y );
		  if( this.forceColorReduction ) {
		    /*
		     * Jede Primaerfarbe wird von 256 auf 6 moegliche Werte
		     * reduziert, indem durch 43 geteilt (ergibt 0-5)
		     * und anschliessend ueber eine Tabelle wieder
		     * auf den Wertebereich 0-255 gemappt wird.
		     */
		    int r = ((rgb >> 16) & 0xFF) / 43;
		    int g = ((rgb >> 8) & 0xFF) / 43;
		    int b = (rgb & 0xFF) / 43;
		    rgb   = 0xFF000000
				| (color6Map[ r ] << 16)
				| (color6Map[ g ] << 8)
				| color6Map[ b ];
		  }
		}
		boolean found = false;
		int     pos   = 0;
		if( nColors > 0 ) {
		  pos = Arrays.binarySearch( sortedRGBs, 0, nColors, rgb );
		  if( pos >= 0 ) {
		    // Farbe bereits vorhanden -> nicht einfuegen
		    found = true;
		  } else {
		    if( nColors >= 256 ) {
		      nColors = -1;		// Tabelle zu klein
		      break;
		    }
		    pos = -pos - 1;
		    for( int i = nColors - 2; i >= pos; --i ) {
		      sortedRGBs[ i + 1 ]   = sortedRGBs[ i ];
		      colorIndexes[ i + 1 ] = colorIndexes[ i ];
		    }
		  }
		}
		if( !found ) {
		  sortedRGBs[ pos ]    = rgb;
		  colorIndexes[ pos ]  = (byte) (tabPos / 3);
		  colorTab[ tabPos++ ] = (byte) ((rgb >> 16) & 0xFF);
		  colorTab[ tabPos++ ] = (byte) ((rgb >> 8) & 0xFF);
		  colorTab[ tabPos++ ] = (byte) (rgb & 0xFF);
		  nColors++;
		}
		if( nColors > 0 ) {
		  pixels[ pixelPos++ ] = colorIndexes[ pos ];
		}
	      }
	    }
	    if( nColors > 0 ) {
	      if( nColors < 256 ) {
		Arrays.fill(
			colorTab,
			3 * nColors,
			colorTab.length,
			(byte) 0 );
	      }
	      this.curColorDepth   = 8;
	      this.curColorTabSize = colorTab.length;
	      this.curColorTab     = colorTab;
	      this.curPixels       = pixels;
	      break;
	    }
	    /*
	     * Auf Farbreduktion umschalten
	     * Ist "smoothColorReduction" aktiviert,
	     * wird dies ueber ein BufferedImage erledigt.
	     */
	    if( this.smoothColorReduction ) {
	      break;
	    }
	    if( this.forceColorReduction ) {
	      break;			// sollte niemals erreicht werden
	    }
	    this.forceColorReduction = true;
	  } while( !this.forceColorReduction );
	}
	if( ((this.curColorDepth < 1) || (this.curColorTabSize < 1)
		|| (this.curColorTab == null) || (this.curPixels == null))
	    && this.smoothColorReduction )
	{
	  BufferedImage convImg = new BufferedImage(
					this.width,
					this.height,
					BufferedImage.TYPE_BYTE_INDEXED,
					getIndexColorModel255() );
	  Graphics g = convImg.createGraphics();
	  if( (w < this.width) || (h < this.height) ) {
	    g.setColor( Color.black );
	    g.fillRect( 0, 0, this.width, this.height );
	  }
	  g.drawImage( image, 0, 0, this );
	  g.dispose();
	  extractCompatibleImageData( convImg );
	}
	if( (this.curColorDepth > 0) && (this.curColorTabSize > 0)
	    && (this.curColorTab != null) && (this.curPixels != null) )
	{
	  rv = new FrameData(
			this.curColorDepth,
			this.curColorTabSize,
			this.curColorTab,
			this.curPixels );
	}
      }
    }
    return rv;
  }


  private void extractCompatibleImageData( BufferedImage image )
  {
    ColorModel cm = image.getColorModel();
    if( cm != null ) {
      if( cm instanceof IndexColorModel ) {
	IndexColorModel icm     = (IndexColorModel) cm;
	int             mapSize = icm.getMapSize();
	if( (mapSize > 0) && (mapSize <= 255) ) {
	  Raster raster = image.getRaster();
	  if( raster != null ) {
	    DataBuffer dataBuf = raster.getDataBuffer();
	    if( dataBuf != null ) {
	      if( dataBuf instanceof DataBufferByte ) {
		byte[] pixels = ((DataBufferByte) dataBuf).getData();
		if( pixels != null ) {
		  if( pixels.length == (this.width * this.height) ) {
		    this.curTransparencyIdx = mapSize;
		    this.curPixels          = pixels;
		    this.curColorDepth      = 8;
		    this.curColorTabSize    = 3 * 256;
		    this.curColorTab        = new byte[ 3 * mapSize ];

		    // Farbtabelle fuellen
		    int src = 0;
		    int dst = 0;
		    while( (src < mapSize)
			   && (dst < this.curColorTab.length) )
		    {
		      this.curColorTab[ dst++ ] = (byte) icm.getRed( src );
		      this.curColorTab[ dst++ ] = (byte) icm.getGreen( src );
		      this.curColorTab[ dst++ ] = (byte) icm.getBlue( src );
		      src++;
		    }
		  }
		}
	      }
	    }
	  }
	}
      }
    }
  }


  /*
   * Diese Methode liefert ein indexiertes Farbmodell mit 255 Farben.
   * Die 256. Farbe ist reserviert fuer die Transparenz und
   * darf deshalb nicht belegt werden.
   */
  private IndexColorModel getIndexColorModel255()
  {
    if( this.indexColorModel255 == null ) {
      byte[] r = new byte[ 256 ];
      byte[] g = new byte[ 256 ];
      byte[] b = new byte[ 256 ];
      int    p = 0;
      // 0-215: Farben (6*6*6=216)
      for( int i = 0; i < 6; i++ ) {
	for( int j = 0; j < 6; j++ ) {
	  for( int k = 0; k < 6; k++ ) {
	    r[ p ] = (byte) color6Map[ i ];
	    g[ p ] = (byte) color6Map[ j ];
	    b[ p ] = (byte) color6Map[ k ];
	    p++;
	  }
	}
      }
      // 216-253: Graustufen
      int d = 256 / (254 - p);
      int v = d;
      while( p < 254 ) {
	r[ p ] = (byte) v;
	g[ p ] = (byte) v;
	b[ p ] = (byte) v;
	v += d;
	p++;
      }
      // 254-255: weiss (davon Position 255 fuer Transparenz reserviert)
      this.indexColorModel255 = new IndexColorModel( 8, 255, r, g, b, 255 );
    }
    return this.indexColorModel255;
  }


  private void writeASCII( String s ) throws IOException
  {
    int n = s.length();
    for( int i = 0; i < n; i++ ) {
      this.out.write( (byte) s.charAt( i ) );
    }
  }


  private void writeColorTab(
			int    colorTabSize,
			byte[] colorTab ) throws IOException
  {
    this.out.write( colorTab );
    for( int i = colorTab.length; i < colorTabSize; i++ ) {
      this.out.write( 0 );
    }
  }


  private void writeFrame( FrameData frame ) throws IOException
  {
    if( this.firstFrame ) {

      // Logical Screen Descriptor
      writeWord( this.width );
      writeWord( this.height );
      /*
       * globale Farbtabelle:
       *   Bit 0-2: Groesse (Bits - 1)
       *   Bit 3:   sortiert (nein)
       *   Bit 4-6: Aufloesung
       *   Bit 7:   Farbtabelle folgt (ja)
       */
      this.out.write( 0xF0 | (this.globalColorDepth - 1) );
      this.out.write( 0 );		// Hintergrundfarbe
      this.out.write( 0 );		// Pixelzuordnung 1:1
      writeColorTab( this.globalColorTabSize, this.globalColorTab );

      // Kommentar
      final String comment = "Created by JKCEMU";
      this.out.write( 0x21 );		// Extension Block
      this.out.write( 0xFE );		// Kommentar
      this.out.write( comment.length() );
      writeASCII( comment );
      this.out.write( 0 );		// Blockende

      // Anzahl der Wiederholungen
      if( this.infinite ) {
	this.out.write( 0x21 );		// Extension-Kennung
	this.out.write( 0xFF );		// Extension-Label
	this.out.write( 11 );		// Blockgroesse
	writeASCII( "NETSCAPE2.0" );	// Applikations-ID
	this.out.write( 3 );		// Groesse Unterblock
	this.out.write( 1 );		// ID fuer Loop-Block
	writeWord( 0 );			// Anzahl Wiederholungen, 0: unendlich
	this.out.write( 0 );		// Blockende
      }
    }

    // Graphic Control Extension
    this.out.write( 0x21 );		// Extension-Kennung
    this.out.write( 0xF9 );		// GCE-Kennung
    this.out.write( 4 );		// Datenblockgroesse
    this.out.write( 0 );		// keine Transparenz
    writeWord( Math.round( frame.delayMillis / 10.0F ) );	// 1/100 sec.
    this.out.write( 0 );		// Transparenzfarbe (nicht benutzt)
    this.out.write( 0 );		// Blockende

    // Image Descriptor
    this.out.write( 0x2C );		// Kennung fuer Bildblock
    writeWord( 0 );			// Bildposition X-Koordinate
    writeWord( 0 );			// Bildposition Y-Koordinate
    writeWord( this.width );		// Bildbreite
    writeWord( this.height );		// Bildhoehe
    if( Arrays.equals( frame.colorTab, this.globalColorTab ) ) {
      this.out.write( 0 );		// keine lokale Farbtabelle
    } else {
      /*
       * lokale Farbtabelle:
       *   Bit 0-2: Groesse (Bits - 1)
       *   Bit 3-4: reserviert
       *   Bit 5:   sortiert (nein)
       *   Bit 6:   interlace (nein)
       *   Bit 7:   Farbtabelle folgt (ja)
       */
      this.out.write( 0x80 | (frame.colorDepth - 1) );
      writeColorTab( frame.colorTabSize, frame.colorTab );
    }

    // Pixeldaten schreiben
    (new LZWEncoder(
		frame.pixels,
		Math.max( frame.colorDepth, 2 ) )).encode( this.out );

    this.firstFrame = false;
  }


  /*
   * Die Methode schreibt einen 16-Bit-Wert (Littel Endian)
   */
  private void writeWord( int value ) throws IOException
  {
    this.out.write( value & 0xFF );
    this.out.write( (value >> 8) & 0xFF );
  }
}

