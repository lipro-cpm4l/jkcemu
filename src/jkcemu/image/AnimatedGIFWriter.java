/*
 * (c) 2010-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erzeugen animierter GIF-Dateien
 */

package jkcemu.image;

import java.io.IOException;
import java.io.OutputStream;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.ImageObserver;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import jkcemu.Main;


public class AnimatedGIFWriter implements ImageObserver
{
  private static class FrameData
  {
    private BufferedImage image;
    private int           delayMillis;
    private int           colorDepth;
    private byte[]        reds;
    private byte[]        greens;
    private byte[]        blues;
    private byte[]        pixels;

    private FrameData(
		BufferedImage image,
		int           colorDepth,
		byte[]        reds,
		byte[]        greens,
		byte[]        blues,
		byte[]        pixels )
    {
      this.image       = image;
      this.delayMillis = 0;
      this.colorDepth  = colorDepth;
      this.reds        = reds;
      this.greens      = greens;
      this.blues       = blues;
      this.pixels      = pixels;
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
   *
   * Die Werte in der Tabelle entsprechen den Web Safe Colors.
   */
  private static final int[] color6Map = { 0, 0x33, 0x66, 0x99, 0xCC, 0xFF };

  private boolean                      infinite;
  private boolean                      force256Colors;
  private boolean                      smoothColorReduction;
  private boolean                      firstFrame;
  private int                          width;
  private int                          height;
  private int                          globalColorDepth;
  private byte[]                       globalReds;
  private byte[]                       globalGreens;
  private byte[]                       globalBlues;
  private IndexColorModel              defaultColorModel;
  private FrameData                    prevFrame;
  private MemoryCacheImageOutputStream out;
  private ImageWriter                  imgWriter;


  public AnimatedGIFWriter(
		OutputStream out,
		boolean      force256Colors,
		boolean      smoothColorReduction,
		boolean      infinite ) throws IOException
  {
    this.infinite             = infinite;
    this.force256Colors       = force256Colors;
    this.smoothColorReduction = smoothColorReduction;
    this.firstFrame           = true;
    this.width                = 0;
    this.height               = 0;
    this.globalColorDepth     = 0;
    this.globalReds           = null;
    this.globalGreens         = null;
    this.globalBlues          = null;
    this.defaultColorModel    = null;
    this.prevFrame            = null;
    this.imgWriter            = null;

    Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName( "gif" );
    while( iter.hasNext() ) {
      ImageWriter imgWriter = iter.next();
      if( imgWriter.canWriteSequence() ) {
	this.imgWriter = imgWriter;
      }
    }
    if( this.imgWriter == null ) {
      throwAnimatedGIFFailed(
		"Keinen passenden GIF-ImageWriter gefunden" );
    }
    this.out = new MemoryCacheImageOutputStream( out );
    this.imgWriter.setOutput( this.out );
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
    if( this.prevFrame != null ) {
      this.prevFrame.delayMillis += millisBefore;
    }
    FrameData frame = createFrameData( image );
    if( this.prevFrame != null ) {
      if( (frame.colorDepth == this.prevFrame.colorDepth)
	  && (frame.pixels != null) && (this.prevFrame.pixels != null)
	  && Arrays.equals( frame.reds,   this.prevFrame.reds )
	  && Arrays.equals( frame.greens, this.prevFrame.greens )
	  && Arrays.equals( frame.blues,  this.prevFrame.blues ) )
      {
	if( Arrays.equals( frame.pixels,  this.prevFrame.pixels ) ) {
	} else {
	  writeFrame( this.prevFrame );
	  this.prevFrame = frame;
	}
      } else {
	writeFrame( this.prevFrame );
	this.prevFrame = frame;
      }
    } else {
      this.width            = image.getWidth();
      this.height           = image.getHeight();
      this.globalColorDepth = frame.colorDepth;
      this.globalReds       = frame.reds;
      this.globalGreens     = frame.greens;
      this.globalBlues      = frame.blues;
      this.prevFrame        = frame;
    }
    image.flush();
  }


  /*
   * Die Methode schreibt alle gepufferten Daten und beendet
   * das animierte GIF-Bild.
   * Der OutputStream wird nicht geschlossen.
   *
   * Rueckgabe:
   *   true:  GIF-Datei enthaelt mindestens 1 Bild
   *   false: GIF-Datei enthaelt keine Bilder
   */
  public boolean finish() throws IOException
  {
    boolean rv = false;
    if( this.prevFrame != null ) {
      writeFrame( this.prevFrame );
      this.prevFrame = null;
      this.imgWriter.endWriteSequence();
      this.out.flushBefore( this.out.length() );
      rv = true;
    }
    this.imgWriter.dispose();
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

  private static void appendColorTab(
				IIOMetadataNode parent,
				String          nodeName,
				byte[]          reds,
				byte[]          greens,
				byte[]          blues )
  {
    if( (reds != null) && (greens != null) && (blues != null) ) {
      int nEntries = Math.min(
			reds.length,
			Math.min( greens.length, blues.length ) );
      if( nEntries > 0 ) {
	IIOMetadataNode node = new IIOMetadataNode( nodeName );
	node.setAttribute( "backgroundColorIndex", "0" );
	node.setAttribute(
			"sizeOf" + nodeName,
			Integer.toString( nEntries ) );
	node.setAttribute( "sortFlag", "FALSE" );
	for( int i = 0; i < nEntries; i++ ) {
	  IIOMetadataNode child = new IIOMetadataNode( "ColorTableEntry" );
	  child.setAttribute( "index", Integer.toString( i ) );
	  child.setAttribute(
			"red",
			Integer.toString( (int) reds[ i ] & 0xFF ) );
	  child.setAttribute(
			"green",
			Integer.toString( (int) greens[ i ] & 0xFF ) );
	  child.setAttribute(
			"blue",
			Integer.toString( (int) blues[ i ] & 0xFF ) );
	  node.appendChild( child );
	}
	parent.appendChild( node );
      }
    }
  }


  /*
   * Die Methode erzeugt aus einem Bild ein FrameData-Objekt,
   * in dem die Bild- und Farbdaten im benoetigten Format vorliegen.
   */
  private FrameData createFrameData( BufferedImage image )
  {
    boolean forceNewImg = this.force256Colors;

    /*
     * wenn Farbtabelle vorhanden,
     * dann diese bei Bedarf auf ein Vielfaches von 2 erweitern
     */
    byte[]          reds       = null;
    byte[]          greens     = null;
    byte[]          blues      = null;
    int             colorDepth = 0;
    IndexColorModel icm        = null;
    if( !forceNewImg ) {
      icm = ImageUtil.getIndexColorModel( image );
      if( icm != null ) {
	int nColors = icm.getMapSize();
	if( (nColors >= 2) && (nColors <= 256) ) {
	  int newSize = 1;
	  int m       = nColors - 1;
	  while( m != 0 ) {
	    colorDepth++;
	    newSize <<= 1;
	    m >>= 1;
	  }
	  reds   = new byte[ newSize ];
	  greens = new byte[ newSize ];
	  blues  = new byte[ newSize ];
	  for( int i = 0; i < nColors; i++ ) {
	    reds[ i ]   = (byte) icm.getRed( i );
	    greens[ i ] = (byte) icm.getGreen( i );
	    blues[ i ]  = (byte) icm.getBlue( i );
	  }
	  for( int i = nColors; i < newSize; i++ ) {
	    reds[ i ]   = (byte) 0;
	    greens[ i ] = (byte) 0;
	    blues[ i ]  = (byte) 0;
	  }
	  try {
	    IndexColorModel newICM = new IndexColorModel(
						colorDepth,
						newSize,
						reds,
						greens,
						blues );
	
	    BufferedImage newImg = new BufferedImage(
						newICM,
						image.getRaster(),
						false,
						null );
	    image.flush();
	    image = newImg;
	  }
	  catch( IllegalArgumentException | RasterFormatException ex ) {
	    forceNewImg = true;
	  }
	} else {
	  forceNewImg = true;
	}
      }
    }

    // ggf. Image zuschneiden
    int wImg = image.getWidth();
    int hImg = image.getHeight();
    if( !forceNewImg
	&& (((this.width > 0) && (wImg != this.width))
		|| ((this.height > 0) && (hImg != this.height))) )
    {
      BufferedImage newImg = null;
      if( (wImg >= this.width) && (hImg >= this.height) ) {
	try {
	  newImg = image.getSubimage( 0, 0, this.width, this.height );
	  image.flush();
	  image = newImg;
	  wImg  = this.width;
	  hImg  = this.height;
	}
	catch( RasterFormatException ex ) {}
      }
      if( newImg == null ) {
	forceNewImg = true;
      }
    }
    if( (this.width < 1) || (this.height < 1) ) {
      this.width  = wImg;
      this.height = hImg;
    }

    /*
     * wenn keine Farbtabelle vorhanden,
     * dann Standardfarbtabelle nehmen
     */
    if( forceNewImg || (icm == null) ) {
      icm                  = getDefaultColorModel();
      colorDepth           = 8;
      boolean       done   = false;
      BufferedImage newImg = new BufferedImage(
					this.width,
					this.height,
					BufferedImage.TYPE_BYTE_INDEXED,
					icm );
      if( !this.smoothColorReduction ) {
	// harte, pixelweise Farbumrechnung
	DataBuffer buffer = newImg.getRaster().getDataBuffer();
	if( buffer != null ) {
	  if( (buffer instanceof DataBufferByte)
	      && (buffer.getSize() == (this.width * this.height)) )
	  {
	    DataBufferByte bb  = (DataBufferByte) buffer;
	    int            dst = 0;
	    for( int y = 0; y < this.height; y++ ) {
	      for( int x = 0; x < this.width; x++ ) {
		int rIdx = 0;
		int gIdx = 0;
		int bIdx = 0;
		if( (x < wImg) && (y < hImg) ) {
		  int rgb = image.getRGB( x, y );
		  rIdx    = ((rgb >> 16) & 0xFF) / 43;
		  gIdx    = ((rgb >> 8) & 0xFF) / 43;
		  bIdx    = (rgb & 0xFF) / 43;
		}
		bb.setElem( dst++, (rIdx * 36) + (gIdx * 6) + bIdx );
	      }
	    }
	    done = true;
	  }
	}
      }
      if( !done ) {
	// weiche Farbumrechnung, bei der u.U. gerastert wird
	Graphics g = newImg.createGraphics();
	if( (wImg > this.width) || (hImg > this.height) ) {
	  g.setColor( Color.BLACK );
	  g.fillRect( 0, 0, this.width, this.height );
	}
	g.drawImage( image, 0, 0, this );
	g.dispose();
      }
      image.flush();
      image = newImg;
    }

    // Frame-Daten ermitteln
    if( (reds == null) || (greens == null) || (blues == null) ) {
      int mapSize = icm.getMapSize();
      colorDepth  = 1;
      if( mapSize > 16 ) {
	colorDepth = 8;
      } else if( mapSize > 4 ) {
	colorDepth = 4;
      } else if( mapSize > 2 ) {
	colorDepth = 2;
      }
      reds   = new byte[ mapSize ];
      greens = new byte[ mapSize ];
      blues  = new byte[ mapSize ];
      icm.getReds( reds );
      icm.getGreens( greens );
      icm.getBlues( blues );
    }
    byte[] pixels = null;
    Raster raster = image.getRaster();
    if( raster != null ) {
      DataBuffer buffer = raster.getDataBuffer();
      if( buffer != null ) {
	if( buffer instanceof DataBufferByte ) {
	  byte[] p = ((DataBufferByte) buffer).getData();
	  if( p != null ) {
	    if( p.length > 0 ) {
	      pixels = p;
	    }
	  }
	}
      }
    }
    return new FrameData(
			image,
			colorDepth,
			reds,
			greens,
			blues,
			pixels );
  }


  /*
   * Die Methode liefert ein indexiertes Farbmodell,
   * welches verwendet wird, wenn das Bild keins hat.
   * Das Farbmodell enthaelt 6*6*6=216 belegte Eintraege,
   * die sortiert sind, damit man bei der Ermittlung des Farbindexes
   * diesen direkt berechnen kann.
   */
  private synchronized IndexColorModel getDefaultColorModel()
  {
    if( this.defaultColorModel == null ) {
      byte[] reds   = new byte[ 256 ];
      byte[] greens = new byte[ 256 ];
      byte[] blues  = new byte[ 256 ];
      int    idx    = 0;
      for( int r = 0; r < 6; r++ ) {
	for( int g = 0; g < 6; g++ ) {
	  for( int b = 0; b < 6; b++ ) {
	    reds[ idx ]   = (byte) color6Map[ r ];
	    greens[ idx ] = (byte) color6Map[ g ];
	    blues[ idx ]  = (byte) color6Map[ b ];
	    idx++;
	  }
	}
      }
      while( idx < reds.length ) {
	reds[ idx ]   = (byte) 0;
	greens[ idx ] = (byte) 0;
	blues[ idx ]  = (byte) 0;
	idx++;
      }
      this.defaultColorModel = new IndexColorModel(
						8,
						256,
						reds,
						greens,
						blues );
    }
    return this.defaultColorModel;
  }


  private static void setFromTree(
			IIOMetadata      metadata,
			org.w3c.dom.Node root ) throws IOException
  {
    try {
      metadata.setFromTree(
			metadata.getNativeMetadataFormatName(),
			root );
    }
    catch( Exception ex ) {
      throwAnimatedGIFFailed( ex.getMessage() );
    }
  }


  private static void throwAnimatedGIFFailed( String detailMsg )
							throws IOException
  {
    StringBuilder buf = new StringBuilder( 512 );
    buf.append( "Mit der verwendeten Java-Laufzeitumgebung k\u00F6nnen\n"
		+ "keine animierten GIF-Dateien erzeugt werden." );
    if( detailMsg != null ) {
      if( !detailMsg.isEmpty() ) {
	buf.append( "\n\nDetails:\n" );
	buf.append( detailMsg );
      }
    }
    throw new IOException( buf.toString() );
  }


  private void writeFrame( FrameData frame ) throws IOException
  {
    if( this.firstFrame ) {

      // StreamMetadata fuer globale Einstellungen
      IIOMetadata streamMetadata = this.imgWriter.getDefaultStreamMetadata(
								null );
      if( streamMetadata == null ) {
	throwAnimatedGIFFailed( "DefaultStreamMetadata == null" );
      }
      IIOMetadataNode streamRoot = new IIOMetadataNode(
			streamMetadata.getNativeMetadataFormatName() );

      // Version
      IIOMetadataNode gifVersion = new IIOMetadataNode( "Version" );
      gifVersion.setAttribute( "value", "89a" );
      streamRoot.appendChild( gifVersion );

      // Logical Screen Descriptor
      IIOMetadataNode lsd = new IIOMetadataNode( "LogicalScreenDescriptor" );
      lsd.setAttribute(
		"logicalScreenWidth",
		Integer.toString( this.width ) );
      lsd.setAttribute(
		"logicalScreenHeight",
		Integer.toString( this.height ) );
      lsd.setAttribute(
		"colorResolution",
		Integer.toString( this.globalColorDepth ) );
      lsd.setAttribute( "pixelAspectRatio", "0" );	// Pixelzuordnung 1:1
      streamRoot.appendChild( lsd );

      /*
       * Global Color Table
       *
       * Der ImageWriter schreibt die hier angegebene Farbpalette
       * nur dann als globale in die GIF-Datei,
       * wenn die Farbtiefe 8 Bit betraegt.
       * In allen anderen Faellen uebernimmt er eine lokale Farbpalette
       * als globale.
       * Da dieses Verhalten aber nicht dokumentiert ist,
       * wird hier die globale Farbpalette trotzdem immer angegeben.
       */
      appendColorTab(
		streamRoot,
		"GlobalColorTable",
		this.globalReds,
		this.globalGreens,
		this.globalBlues );

      // Bildsequenz beginnen
      setFromTree( streamMetadata, streamRoot );
      this.imgWriter.prepareWriteSequence( streamMetadata );
    }

    // ImageMetadata fuer jedes einzelne Bild
    IIOMetadata imgMetadata = this.imgWriter.getDefaultImageMetadata(
		ImageTypeSpecifier.createFromRenderedImage( frame.image ),
		null );
    if( imgMetadata == null ) {
      throwAnimatedGIFFailed( "DefaultImageMetadata == null" );
    }
    IIOMetadataNode imgRoot = new IIOMetadataNode(
			imgMetadata.getNativeMetadataFormatName() );

    /*
     * CommentsExtensions und ApplicationExtensions sind nur
     * in den Bild-Metadaten moeglich.
     * Aus diesem Grund werden sie beim ersten Bild angegeben.
     */
    if( this.firstFrame ) {

      // Kommentar
      IIOMetadataNode commentNode = new IIOMetadataNode(
						"CommentExtension" );
      commentNode.setAttribute( "value", "Created by " + Main.APPNAME );

      IIOMetadataNode commentsNode = new IIOMetadataNode(
						"CommentExtensions" );
      commentsNode.appendChild( commentNode );
      imgRoot.appendChild( commentsNode );

      // Anzahl der Wiederholungen
      if( this.infinite ) {
	IIOMetadataNode appNode = new IIOMetadataNode(
						"ApplicationExtension" );
	appNode.setAttribute( "applicationID", "NETSCAPE" );
	appNode.setAttribute( "authenticationCode", "2.0" );
	appNode.setUserObject(
		new byte[] { 
			(byte) 0x01,	// ID fuer Loop-Block
			(byte) 0x00,	// Wiederholungen, 0: unendlich
			(byte) 0x00 } );	// Blockende

	IIOMetadataNode appsNode = new IIOMetadataNode(
					"ApplicationExtensions" );
	appsNode.appendChild( appNode );
	imgRoot.appendChild( appsNode );
      }
    }

    // Graphic Control Extension
    IIOMetadataNode gce = new IIOMetadataNode( "GraphicControlExtension" );
    gce.setAttribute( "disposalMethod", "none" );
    gce.setAttribute( "userInputFlag", "FALSE" );
    gce.setAttribute( "transparentColorFlag", "FALSE" );
    gce.setAttribute(
		"delayTime",
		Integer.toString( Math.round( frame.delayMillis / 10.0F ) ) );
    gce.setAttribute( "transparentColorIndex", "0" );	// nicht benutzt
    imgRoot.appendChild( gce );

    // Image descriptor
    IIOMetadataNode imd = new IIOMetadataNode( "ImageDescriptor" );
    imd.setAttribute( "imageLeftPosition", "0" );
    imd.setAttribute( "imageTopPosition", "0" );
    imd.setAttribute(
		"imageWidth",
		Integer.toString( this.width ) );
    imd.setAttribute(
		"imageHeight",
		Integer.toString( this.height ) );
    imd.setAttribute( "interlaceFlag", "FALSE" );
    imgRoot.appendChild( imd );

    /*
     * Auch wenn die lokale Farbpalette sich nicht von der globalen
     * unterscheidet, muss sie hier in dem Fall angegeben werden,
     * wenn die Farbtiefe weniger als 8 Bit ist.
     * Anderenfalls ist in der GIF-Datei ueberhaupt keine Farbpalette
     * enthalten und die Anzeige ist dann nur schwarz/weiss.
     * Jedoch uebernimmt der ImageWriter die hier angegebene
     * lokale Farbpalette bei Bedarf auch als globale in die GIF-Datei.
     */
    if( (this.globalColorDepth < 8)
	&& (!Arrays.equals( frame.reds, this.globalReds )
		|| !Arrays.equals( frame.greens, this.globalGreens )
		|| !Arrays.equals( frame.blues, this.globalBlues )) )
    {
      appendColorTab(
		imgRoot,
		"LocalColorTable",
		frame.reds,
		frame.greens,
		frame.blues );
    }

    // Bild hinzufuegen
    setFromTree( imgMetadata, imgRoot );
    try {
      this.imgWriter.writeToSequence(
			new IIOImage( frame.image, null, imgMetadata ),
			null );
    }
    catch( UnsupportedOperationException ex ) {
      throwAnimatedGIFFailed( "Die Java-Laufzeitumgebung unterst\u00FCtzt"
		+ " keine animierten GIF-Dateien." );
    }
    this.firstFrame = false;
  }
}
