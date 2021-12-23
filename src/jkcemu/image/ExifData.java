/*
 * (c) 2020-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * EXIF-Daten
 */

package jkcemu.image;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import jkcemu.Main;


public class ExifData
{
  public static final int ENTRY_TYPE_SUBIFD  = 0x8769;
  public static final int ENTRY_TYPE_GPS     = 0x8825;
  public static final int ENTRY_TYPE_INTEROP = 0xA005;

  private static final int ENTRY_TYPE_IMG_W           = 0x0100;
  private static final int ENTRY_TYPE_IMG_H           = 0x0101;
  private static final int ENTRY_TYPE_DOC_NAME        = 0x010D;
  private static final int ENTRY_TYPE_IMG_DESC        = 0x010E;
  private static final int ENTRY_TYPE_ORIENTATION     = 0x0112;
  private static final int ENTRY_TYPE_RESOLUTION_X    = 0x011A;
  private static final int ENTRY_TYPE_RESOLUTION_Y    = 0x011B;
  private static final int ENTRY_TYPE_RESOLUTION_UNIT = 0x0128;
  private static final int ENTRY_TYPE_SOFTWARE        = 0x0131;
  private static final int ENTRY_TYPE_AUTHOR          = 0x013B;
  private static final int ENTRY_TYPE_COPYRIGHT       = 0x8298;
  private static final int ENTRY_TYPE_DATE            = 0x9003;
  private static final int ENTRY_TYPE_USER_COMMENT    = 0x9286;
  private static final int ENTRY_TYPE_WIN_COMMENT     = 0x9C9C;
  private static final int ENTRY_TYPE_CONTRAST        = 0xA408;
  private static final int ENTRY_TYPE_SATURATION      = 0xA409;
  private static final int ENTRY_TYPE_SHARPNESS       = 0xA40A;

  private static final byte[] PATTERN_ASCII_COMMENT = {
			0x41, 0x53, 0x43, 0x49, 0x49, 0x00, 0x00, 0x00 };

  private static final byte[] PATTERN_UTF16_COMMENT = {
			0x55, 0x4E, 0x49, 0x43, 0x4F, 0x44, 0x45, 0x00 };

  private static DateFormat   dateFmt = null;
  private static NumberFormat numFmt  = null;

  private Map<Integer,ExifEntry> ifd0Map;
  private Map<Integer,ExifEntry> subIfdMap;
  private Map<Integer,ExifEntry> interopMap;
  private Map<Integer,ExifEntry> gpsMap;
  private String                 gpsPosDegreeText;
  private String                 gpsPosNumericText;
  private boolean                gpsPosTextsDone;
  private boolean                bigEndian;
  private boolean                forChangedImg;
  private boolean                forResizedImg;
  private String                 timeZone;


  public ExifData()
  {
    this( isDefaultBigEndian() );
  }


  public ExifData( boolean bigEndian )
  {
    this.ifd0Map           = null;
    this.subIfdMap         = null;
    this.interopMap        = null;
    this.gpsMap            = null;
    this.gpsPosDegreeText  = null;
    this.gpsPosNumericText = null;
    this.gpsPosTextsDone   = false;
    this.bigEndian         = bigEndian;
    this.forChangedImg     = false;
    this.forResizedImg     = false;
    this.timeZone          = null;
  }


  public ExifData(
		Map<Integer,ExifEntry> ifd0Map,
		Map<Integer,ExifEntry> subIfdMap,
		Map<Integer,ExifEntry> gpsMap,
		Map<Integer,ExifEntry> interopMap,
		boolean                bigEndian )
  {
    this( bigEndian );
    this.ifd0Map         = ifd0Map;
    this.subIfdMap       = subIfdMap;
    this.gpsMap          = gpsMap;
    this.interopMap      = interopMap;
  }


  public ExifData copy()
  {
    ExifData exifData = new ExifData(
				copyOf( this.ifd0Map ),
				copyOf( this.subIfdMap ),
				copyOf( this.gpsMap ),
				copyOf( this.interopMap ),
				this.bigEndian );
    exifData.gpsPosDegreeText  = this.gpsPosDegreeText;
    exifData.gpsPosNumericText = this.gpsPosNumericText;
    exifData.gpsPosTextsDone   = this.gpsPosTextsDone;
    exifData.forChangedImg     = this.forChangedImg;
    exifData.forResizedImg     = this.forResizedImg;
    exifData.timeZone          = this.timeZone;
    return exifData;
  }


  public ExifData copyForNewEncoding( int w, int h )
  {
    ExifData exifData = copy();
    if( exifData.ifd0Map != null ) {

      // moeglicherweise nicht mehr passende Eintraege entfernen
      removeObsoleteEntriesFrom( exifData.ifd0Map );

      // Bildgroesse entfernen
      exifData.ifd0Map.remove( ENTRY_TYPE_IMG_W );
      exifData.ifd0Map.remove( ENTRY_TYPE_IMG_H );

      // Ausrichtung zuruecksetzen
      boolean   remove = true;
      ExifEntry entry  = exifData.ifd0Map.get( ENTRY_TYPE_ORIENTATION );
      if( entry != null ) {
	Number value = entry.getNumberValue();
	if( value != null ) {
	  if( value.intValue() == 1 ) {
	    remove = false;
	  }
	}
      }
      if( remove ) {
	exifData.ifd0Map.remove( ENTRY_TYPE_ORIENTATION );
      }
    } else {
      exifData.ifd0Map = new HashMap<>();
    }

    // Bildgroesse setzen
    if( (w > 0) && (w < 0x8000) && (h > 0) && (h < 0x8000) ) {
      exifData.ifd0Map.put(
		ENTRY_TYPE_IMG_W,
		createUInt2Entry( ENTRY_TYPE_IMG_W, w ) );
      exifData.ifd0Map.put(
		ENTRY_TYPE_IMG_H,
		createUInt2Entry( ENTRY_TYPE_IMG_H, h ) );
    }

    // Software-Eintrag anpassen
    setSoftwareEntry( exifData.ifd0Map );

    return exifData;
  }


  public String getAuthor()
  {
    return getStringValue( this.ifd0Map, ENTRY_TYPE_AUTHOR );
  }


  public String getCameraModel()
  {
    return getStringValue( this.ifd0Map, 0x0110 );
  }


  public String getCameraVendor()
  {
    return getStringValue( this.ifd0Map, 0x010F );
  }


  public String getColorSpace()
  {
    return getChoice( 
		this.subIfdMap,
		0xA001,
		null,
		"sRGB",
		"Adobe RGB" );
  }


  public String getComment()
  {
    String text = null;
    try {
      if( this.subIfdMap != null ) {
	ExifEntry entry = this.subIfdMap.get( ENTRY_TYPE_USER_COMMENT );
	if( entry != null ) {
	  byte[] a = entry.getByteArrayValue();
	  if( a != null ) {
	    if( a.length > 8 ) {
	      if( ExifParser.matchesAt( a, 0, PATTERN_ASCII_COMMENT ) ) {
		text = new String(
				a,
				8,
				a.length - 8,
				"ISO-8859-1" );
	      }
	      else if( ExifParser.matchesAt( a, 0, PATTERN_UTF16_COMMENT ) ) {
		text = new String(
				a,
				8,
				(a.length - 8) & ~1,	// gerade Laenge
				this.bigEndian ? "UTF-16BE" : "UTF-16LE" );
	      }
	    }
	  }
	}
      }
    }
    catch( UnsupportedEncodingException ex ) {}

    /*
     * Wenn im Standard-Kommentareintrag nichts steht,
     * dann den von Windows Explorer verwendeten Eintrag 0x9C9C pruefen.
     * Dieser verwendet immer UTF-16LE,
     * auch wenn die Bilddatei Big Endian hat.
     */
    if( text == null ) {
      try {
	if( this.ifd0Map != null ) {
	  ExifEntry entry = this.ifd0Map.get( ENTRY_TYPE_WIN_COMMENT );
	  if( entry != null ) {
	    byte[] a = entry.getByteArrayValue();
	    if( a != null ) {
	      if( a.length > 1 ) {
		text = new String(
				a,
				0,
				a.length & ~1,	// gerade Laenge
				"UTF-16LE" );
	      }
	    }
	  }
	}
      }
      catch( UnsupportedEncodingException ex ) {}
    }

    // Null-Zeichen als String-Ende werten
    if( text != null ) {
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
	if( text.charAt( i ) == '\u0000' ) {
	  text = text.substring( 0, i );
	  break;
	}
      }
      if( text.isEmpty() ) {
	text = null;
      }
    }
    return text;
  }


  public String getCompressedBitsPerPixelText()
  {
    return getFractionValueText( this.subIfdMap, 0x9102 );
  }


  public String getContrast()
  {
    return getChoice( 
		this.subIfdMap,
		ENTRY_TYPE_CONTRAST,
		"normal",
		"weich",
		"hart" );
  }


  public String getCopyright()
  {
    return getStringValue( this.ifd0Map, ENTRY_TYPE_COPYRIGHT );
  }


  public java.util.Date getDate()
  {
    java.util.Date date = null;
    String         text = getStringValue( this.subIfdMap, ENTRY_TYPE_DATE );
    if( text != null ) {
      try {
	date = getDateFmt().parse( text );
      }
      catch( ParseException ex ) {}
    }
    return date;
  }


  // Digital-Zoom
  public String getDigitalZoomText()
  {
    return getFractionValueText( this.subIfdMap, 0xA404 );
  }


  // Bildbeschreibung
  public String getDocumentName()
  {
    return getStringValue( this.ifd0Map, ENTRY_TYPE_DOC_NAME );
  }


  public String getExifVersion()
  {
    String rv = null;
    if( this.subIfdMap != null ) {
      ExifEntry entry = this.subIfdMap.get( 0x9000 );
      if( entry != null ) {
	byte[] a = entry.getByteArrayValue();
	if( a != null ) {
	  if( a.length == 4 ) {
	    try {
	      int v = Integer.parseInt( new String( a, "US-ASCII" ) );
	      if( v > 0 ) {
		rv = String.format( "%d.%02d", v / 100, v % 100 );
	      }
	    }
	    catch( NumberFormatException ex ) {}
	    catch( UnsupportedEncodingException ex ) {}
	  }
	}
      }
    }
    return rv;
  }


  // Belichtungsart
  public String getExposureModeText()
  {
    return getChoice(
		this.subIfdMap,
		0xA406,
		"automatische Belichtung",
		"manuelle Belichtung",
		"automatische Belichtungsreihe" );
  }


  // Belichtungsverfahren
  public String getExposureProgramm()
  {
    return getChoice(
		this.subIfdMap,
		0x8822,
		null,
		"manuell",
		"Programmautomatik",
		"Blendenvorwahl",
		"Zeitvorwahl",
		"Programmautomatik f\u00FCr langsame Motive",
		"Programmautomatik f\u00FCr schnelle Motive",
		"Portraitautomatik",
		"Landschaftsautomatik" );
  }


  // Belichtungszeit
  public String getExposureTimeText()
  {
    String rv = null;
    if( this.subIfdMap != null ) {
      ExifEntry entry = this.subIfdMap.get( 0x829A );
      if( entry != null ) {
	long[] values = entry.getRational( 0 );
	if( values != null ) {
	  long numerator   = values[ 0 ];
	  long denominator = values[ 1 ];
	  if( (numerator > 0) && (denominator > 0) ) {
	    if( numerator >= (3 * denominator) ) {
	      rv = String.format(
			"%d s",
			Math.round( (double) numerator
					/ (double) denominator ) );
	    } else if( (2 * numerator) > denominator ) {
	      rv = divAndFormat( numerator, denominator ) + " s";
	    } else {
	      rv = String.format(
			"1/%d s",
			Math.round( (double) denominator
					/ (double) numerator ) );
	    }
	  }
	}
      }
    }
    return rv;
  }


  // Blitzlicht
  public String getFlashMode()
  {
    String rv = null;
    if( this.subIfdMap != null ) {
      ExifEntry entry = this.subIfdMap.get( 0x9209 );
      if( entry != null ) {
	Number value = entry.getNumberValue();
	if( value != null ) {
	  int v = value.intValue();
	  if( (v & 0x01) != 0 ) {
	    StringBuilder buf = new StringBuilder();
	    buf.append( "Blitz" );
	    if( (v & 0x10) != 0 ) {
	      buf.append( " (Automatikmodus)" );
	    }
	    if( (v & 0x04) != 0 ) {
	      buf.append( (v & 0x02) != 0 ? " mit" : " ohne" );
	      buf.append( " Stroboskop" );
	    }
	    if( (v & 0x40) != 0 ) {
	      buf.append( ", Rote-Augen-Funktion" );
	    }
	    rv = buf.toString();
	  } else {
	    rv = "Kein Blitz";
	  }
	}
      }
    }
    return rv;
  }


  // Blendenzahl
  public String getFNumberText()
  {
    String rv = null;
    if( this.subIfdMap != null ) {
      ExifEntry entry = this.subIfdMap.get( 0x829D );
      if( entry != null ) {
	long[] values = entry.getRational( 0 );
	if( values != null ) {
	  if( (values[ 0 ] > 0) && (values[ 1 ] > 0) ) {
	    rv = "F/" + divAndFormat( values[ 0 ], values[ 1 ] );
	  }
	}
      }
    }
    return rv;
  }


  // 35mm-Brennweite
  public String getFocalLength35mmText()
  {
    return getRoundedNumberText( this.subIfdMap, 0xA405, null );
  }


  // Brennweite
  public String getFocalLengthText()
  {
    return getRoundedNumberText( this.subIfdMap, 0x920A, "mm" );
  }


  public String getGPSAltitudeText()
  {
    String rv = null;
    if( this.gpsMap != null ) {
      ExifEntry refEntry   = this.gpsMap.get( 0x0005 );
      ExifEntry valueEntry = this.gpsMap.get( 0x0006 );
      if( (refEntry != null) && (valueEntry != null) ) {
	Number ref   = refEntry.getNumberValue();
	long[] value = valueEntry.getRational( 0 );
	if( (ref != null) && (value != null) ) {
	  rv = String.format(
			"%s m %s Meeresspiegel",
			divAndFormat( value[ 0 ], value[ 1 ] ),
			ref.longValue() == 0 ? "\u00FCber" : "unter" );
	}
      }
    }
    return rv;
  }


  public String getGPSPosDegreeText()
  {
    gpsCheckCreatePosTexts();
    return this.gpsPosDegreeText;
  }


  public String getGPSPosNumericText()
  {
    gpsCheckCreatePosTexts();
    return this.gpsPosNumericText;
  }


  // Lichtempfindlichkeit
  public String getISOText()
  {
    String rv = null;
    if( this.subIfdMap != null ) {
      ExifEntry entry = this.subIfdMap.get( 0x8827 );
      if( entry != null ) {
	Number value = entry.getNumberValue();
	if( value != null ) {
	  rv = "ISO-" + format( value );
	}
      }
    }
    return rv;
  }


  // Bildbeschreibung
  public String getImageDesc()
  {
    return getStringValue( this.ifd0Map, ENTRY_TYPE_IMG_DESC );
  }


  // Bild-ID
  public String getImageID()
  {
    return getStringValue( this.subIfdMap, 0xA420 );
  }


  // Max. Blende
  public String getMaxApertureValue()
  {
    String rv = null;
    if( this.subIfdMap != null ) {
      ExifEntry entry = this.subIfdMap.get( 0x9205 );
      if( entry != null ) {
	Number value = entry.getNumberValue();
	if( value != null ) {
	  rv = format( value );
	}
      }
    }
    return rv;
  }


  // Modus der Belichtungsmessung
  public String getMeteringMode()
  {
    return getChoice(
		this.subIfdMap,
		0x9207,
		null,
		"Integral",
		"Mittenbetont",
		"Einpunkt",
		"Mehrpunkt",
		"Mehrfeld",
		"Teilbereich" );
  }


  // Ausloesung
  public String getResolutionText()
  {
    String rv      = null;
    String resUnit = getChoice(
			this.ifd0Map,
			ENTRY_TYPE_RESOLUTION_UNIT,
			null,
			"",
			"dpi",
			"Pixel/cm" );
    if( resUnit != null ) {
      String resX = getRoundedNumberText(
				this.ifd0Map,
				ENTRY_TYPE_RESOLUTION_X,
				null );
      String resY = getRoundedNumberText(
				this.ifd0Map,
				ENTRY_TYPE_RESOLUTION_Y,
				null );
      if( (resX != null) && (resY != null) ) {
	rv = String.format( "%s x %s %s", resX, resY, resUnit );
      }
    }
    return rv;
  }


  // Saettigung
  public String getSaturation()
  {
    return getChoice(
		this.subIfdMap,
		ENTRY_TYPE_SATURATION,
		"normal",
		"niedrig",
		"hoch" );
  }


  // Bilderfassungsart
  public String getSceneCaptureTypeText()
  {
    return getChoice(
		this.subIfdMap,
		0xA406,
		"Standard",
		"Landschaft",
		"Portrait",
		"Nacht" );
  }


  public ExifData getSharedCopyForChangedImage()
  {
    ExifData exifData = this;
    if( !this.forChangedImg ) {
      exifData = copy();
      if( exifData.ifd0Map != null ) {

	// moeglicherweise nicht mehr passende Eintraege loeschen
	removeObsoleteEntriesFrom( exifData.ifd0Map );

	// Software-Eintrag anpassen
	setSoftwareEntry( exifData.ifd0Map );
      }
      if( exifData.subIfdMap != null ) {

	// Infos ueber Kontrast, Saettigung und Schaerfe entfernen
	exifData.subIfdMap.remove( ENTRY_TYPE_CONTRAST );
	exifData.subIfdMap.remove( ENTRY_TYPE_SATURATION );
	exifData.subIfdMap.remove( ENTRY_TYPE_SHARPNESS );
      }
      exifData.forChangedImg = true;
    }
    return exifData;
  }


  public ExifData getSharedCopyForResizedImage()
  {
    ExifData exifData = this;
    if( !this.forResizedImg ) {
      exifData = copy();
      if( exifData.ifd0Map != null ) {

	// moeglicherweise nicht mehr passende Eintraege loeschen
	removeObsoleteEntriesFrom( exifData.ifd0Map );

	// Eintrage zu Aufloesung und Position entfernen
	exifData.ifd0Map.remove( ENTRY_TYPE_IMG_W );
	exifData.ifd0Map.remove( ENTRY_TYPE_IMG_H );
	exifData.ifd0Map.remove( ENTRY_TYPE_RESOLUTION_X );
	exifData.ifd0Map.remove( ENTRY_TYPE_RESOLUTION_Y );
	exifData.ifd0Map.remove( ENTRY_TYPE_RESOLUTION_UNIT );
	exifData.ifd0Map.remove( 0x011E );	// X pos
	exifData.ifd0Map.remove( 0x011F );	// Y pos

	// Software-Eintrag anpassen
	setSoftwareEntry( exifData.ifd0Map );
      }
      exifData.forResizedImg = true;
    }
    return exifData;
  }


  // Schaerfe
  public String getSharpness()
  {
    return getChoice(
		this.subIfdMap,
		ENTRY_TYPE_SHARPNESS,
		"normal",
		"weich",
		"hart" );
  }


  // Software
  public String getSoftware()
  {
    return getStringValue( this.ifd0Map, ENTRY_TYPE_SOFTWARE );
  }


  // Entfernungsmodus
  public String getSubjectDistanceRangeText()
  {
    return getChoice(
		this.subIfdMap,
		0xA40C,
		"Standard",
		"Makro",
		"Nahaufnahme",
		"Fernaufnahme" );
  }


  // Am Objektiv eingestellte Entfernung
  public String getSubjectDistanceText()
  {
    String rv = null;
    if( this.subIfdMap != null ) {
      ExifEntry entry = this.subIfdMap.get( 0x9206 );
      if( entry != null ) {
	Number value = entry.getNumberValue();
	if( value != null ) {
	  if( (value instanceof Float) || (value instanceof Double)
	      && (value.floatValue() < 2F) )
	  {
	    float cmValue = value.floatValue() / 100F;
	    if( cmValue >= 1F ) {
	      rv = String.format( "%d cm", (int) cmValue );
	    }
	  } else {
	    int mValue = value.intValue();
	    if( mValue > 0 ) {
	      rv = String.format( "%d m", mValue );
	    }
	  }
	}
      }
    }
    return rv;
  }


  /*
   * Zeitzone
   *
   * Es wird die Zeitzone zurueckgeliefert,
   * die mit setDate(...) gesetzt wurde.
   */
  public String getTimeZone()
  {
    return this.timeZone;
  }


  // Weissabgleich
  public String getWhiteBalanceText()
  {
    return getChoice(
		this.subIfdMap,
		0xA403,
		"automatisch",
		"manuell" );
  }


  public void setAuthor( byte[] textBytes )
  {
    setIfd0Text( ENTRY_TYPE_AUTHOR, textBytes );
  }


  public void setAuthor( String text )
  {
    setIfd0Text( ENTRY_TYPE_AUTHOR, text );
  }


  public void setComment( String text )
  {
    if( this.subIfdMap != null ) {
      this.subIfdMap.remove( ENTRY_TYPE_USER_COMMENT );
    }
    if( this.ifd0Map != null ) {
      this.ifd0Map.remove( ENTRY_TYPE_WIN_COMMENT );
    }
    if( text != null ) {
      try {
	text    = text.trim();
	int len = text.length();
	if( len > 0 ) {
	  boolean ascii = true;
	  for( int i = 0; i < len; i++ ) {
	    if( text.charAt( i ) > '\u007E' ) {
	      ascii = false;
	      break;
	    }
	  }
	  byte[] t = null;
	  byte[] p = null;
	  int    n = 0;
	  if( ascii ) {
	    t = text.getBytes( "US-ASCII" );
	    p = PATTERN_ASCII_COMMENT;
	    n = 1;
	  } else {
	    t = text.getBytes( this.bigEndian ? "UTF-16BE" : "UTF-16LE" );
	    p = PATTERN_UTF16_COMMENT;
	    n = 2;
	  }
	  byte[] a = new byte[ p.length + t.length + n ];
	  Arrays.fill( a, (byte) 0 );
	  System.arraycopy( p, 0, a, 0, p.length );
	  System.arraycopy( t, 0, a, p.length, t.length );
	  if( this.ifd0Map == null ) {
	    this.ifd0Map = new HashMap<>();
	  }
	  if( this.subIfdMap == null ) {
	    this.subIfdMap = new HashMap<>();
	  }
	  this.subIfdMap.put(
			ENTRY_TYPE_USER_COMMENT,
			new ExifEntry(
				ENTRY_TYPE_USER_COMMENT,
				ExifParser.DATA_TYPE_BYTE_ARRAY,
				a.length,
				a,
				0,
				a.length,
				this.bigEndian ) );
	}
      }
      catch( UnsupportedEncodingException ex ) {}
    }
  }


  public void setCopyright( byte[] textBytes )
  {
    setIfd0Text( ENTRY_TYPE_COPYRIGHT, textBytes );
  }


  public void setCopyright( String text )
  {
    setIfd0Text( ENTRY_TYPE_COPYRIGHT, text );
  }


  public void setDate( java.util.Date date, String timeZone )
  {
    if( date != null ) {
      if( this.ifd0Map == null ) {
	this.ifd0Map = new HashMap<>();
      }
      if( this.subIfdMap != null ) {
	this.subIfdMap.remove( ENTRY_TYPE_DATE );
      } else {
	this.subIfdMap = new HashMap<>();
      }
      try {
	setText(
		this.subIfdMap,
		ENTRY_TYPE_DATE,
		(new SimpleDateFormat(
				"yyyy:MM:dd HH:mm:ss",
				Locale.US )).format( date ).getBytes(
							"US-ASCII" ) );
	this.timeZone = timeZone;
      }
      catch( UnsupportedEncodingException ex ) {}
    }
  }


  public void setDocumentName( byte[] textBytes )
  {
    setIfd0Text( ENTRY_TYPE_DOC_NAME, textBytes );
  }


  public void setDocumentName( String text )
  {
    setIfd0Text( ENTRY_TYPE_DOC_NAME, text );
  }


  public void setImageDesc( byte[] textBytes )
  {
    setIfd0Text( ENTRY_TYPE_IMG_DESC, textBytes );
  }


  public void setImageDesc( String text )
  {
    setIfd0Text( ENTRY_TYPE_IMG_DESC, text );
  }


  public void setSoftware( String text )
  {
    setIfd0Text( ENTRY_TYPE_SOFTWARE, text );
  }


  /*
   * Schreiben der Exif-Daten aus TIFF-Tag
   *
   * Rueckgabe: Position des ersten Bytes hinter den Exif-Data
   */
  public int writeTiffTagTo( byte[] outBuf, int outOffs )
  {
    int rv = 0;
    try {
      /*
       * Exif-Verzeichnisse bereinigen und vervollstaendigen
       * (Bildgroesse und Verlinkung auf die Unterverzeichnisse)
       */
      if( this.ifd0Map != null ) {
	this.ifd0Map.remove( ENTRY_TYPE_SUBIFD );
	this.ifd0Map.remove( ENTRY_TYPE_GPS );
      }
      if( this.subIfdMap != null ) {
	this.subIfdMap.remove( ENTRY_TYPE_INTEROP );
      }
      ExifEntry subIfdEntry  = null;
      ExifEntry gpsEntry     = null;
      ExifEntry interopEntry = null;
      if( this.interopMap != null ) {
	if( !this.interopMap.isEmpty() ) {
	  interopEntry = createUInt4Entry( ENTRY_TYPE_INTEROP );
	  if( this.subIfdMap == null ) {
	    this.subIfdMap = new HashMap<>();
	  }
	  this.subIfdMap.put( ENTRY_TYPE_INTEROP, interopEntry );
	}
      }
      if( this.subIfdMap != null ) {
	if( !this.subIfdMap.isEmpty() ) {
	  subIfdEntry = createUInt4Entry( ENTRY_TYPE_SUBIFD );
	  if( this.ifd0Map == null ) {
	    this.ifd0Map = new HashMap<>();
	  }
	  this.ifd0Map.put( ENTRY_TYPE_SUBIFD, subIfdEntry );
	}
      }
      if( this.gpsMap != null ) {
	if( !this.gpsMap.isEmpty() ) {
	  gpsEntry = createUInt4Entry( ENTRY_TYPE_GPS );
	  if( this.ifd0Map == null ) {
	    this.ifd0Map = new HashMap<>();
	  }
	  this.ifd0Map.put( ENTRY_TYPE_GPS, gpsEntry );
	}
      }
      if( this.ifd0Map != null ) {
	int n = this.ifd0Map.size();
	if( n > 0 ) {

	  // TIFF Byte Order Markierung schreiben
	  byte[] bom = (this.bigEndian ?
				ExifParser.PATTERN_BIG_ENDIAN
				: ExifParser.PATTERN_LITTLE_ENDIAN);
	  System.arraycopy( bom, 0, outBuf, outOffs, bom.length );

	  /*
	   * Positionen der einzelnen Verzeichnisse
	   * und des Datenbereichs ermitteln,
	   * Die Positionen beziehen sich auf den Anfang des TIFF-Tags,
	   * d.h. ab outOffs.
	   */
	  AtomicInteger dirPos = new AtomicInteger( bom.length );
	  int pos = dirPos.get() + 2 + (n * ExifParser.ENTRY_SIZE);
	  if( subIfdEntry != null ) {
	    subIfdEntry.setInt4Value( pos );
	  }
	  if( this.subIfdMap != null ) {
	    n = this.subIfdMap.size();
	    if( n > 0 ) {
	      pos += (2 + (n * ExifParser.ENTRY_SIZE));
	    }
	  }
	  if( gpsEntry != null ) {
	    gpsEntry.setInt4Value( pos );
	  }
	  if( this.gpsMap != null ) {
	    n = this.gpsMap.size();
	    if( n > 0 ) {
	      pos += (2 + (n * ExifParser.ENTRY_SIZE));
	    }
	  }
	  if( interopEntry != null ) {
	    interopEntry.setInt4Value( pos );
	  }
	  if( this.interopMap != null ) {
	    n = this.interopMap.size();
	    if( n > 0 ) {
	      pos += (2 + (n * ExifParser.ENTRY_SIZE));
	    }
	  }
	  AtomicInteger dataPos = new AtomicInteger( pos );
	  writeDirTo(
		outBuf,
		outOffs,
		dirPos.get(),
		dirPos,
		dataPos,
		this.ifd0Map );
	  if( subIfdEntry != null ) {
	    writeDirTo(
		outBuf,
		outOffs,
		subIfdEntry.getInt4Value(),
		dirPos,
		dataPos,
		this.subIfdMap );
	  }
	  if( gpsEntry != null ) {
	    writeDirTo(
		outBuf,
		outOffs,
		gpsEntry.getInt4Value(),
		dirPos,
		dataPos,
		this.gpsMap );
	  }
	  if( interopEntry != null ) {
	    writeDirTo(
		outBuf,
		outOffs,
		interopEntry.getInt4Value(),
		dirPos,
		dataPos,
		this.interopMap );
	  }
	  rv = dataPos.get() + outOffs;
	}
      }
    }
    catch( RuntimeException ex ) {
      /*
       * Hier wird nur hineingesprungen,
       * wenn die Offsetberechnungen in den Exif-Daten falsch sind
       * (IllegalStateException) oder das Byte-Array zu klein ist.
       * In beiden Faellen sollen einfach keine Exif-Daten
       * geschrieben und kein Fehler gemeldet werden.
       */
      rv = 0;
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void gpsCheckCreatePosTexts()
  {
    if( !this.gpsPosTextsDone && (this.gpsMap != null) ) {
      String    latitudeRef    = getStringValue( this.gpsMap, 0x0001 );
      ExifEntry latitudeEntry  = this.gpsMap.get( 0x0002 );
      String    longitudeRef   = getStringValue( this.gpsMap, 0x0003 );
      ExifEntry longitudeEntry = this.gpsMap.get( 0x0004 );
      if( (latitudeRef != null) && (latitudeEntry != null)
	  && (longitudeRef != null) && (longitudeEntry != null) )
      {
	long[] laDeg = latitudeEntry.getRational( 0 );
	long[] laMin = latitudeEntry.getRational( 1 );
	long[] laSec = latitudeEntry.getRational( 2 );
	long[] loDeg = longitudeEntry.getRational( 0 );
	long[] loMin = longitudeEntry.getRational( 1 );
	long[] loSec = longitudeEntry.getRational( 2 );
	if( !latitudeRef.isEmpty()
	    && (laDeg != null)
	    && (laMin != null)
	    && (laSec != null)
	    && !longitudeRef.isEmpty()
	    && (loDeg != null)
	    && (loMin != null)
	    && (loSec != null) )
	{
	  char laRefChar = Character.toUpperCase( latitudeRef.charAt( 0 ) );
	  char loRefChar = Character.toUpperCase( longitudeRef.charAt( 0 ) );
	  if( ((laRefChar == 'N') || (laRefChar == 'S'))
	      && (laDeg[ 0 ] >= 0) && (laDeg[ 1 ] > 0)
	      && (laMin[ 0 ] >= 0) && (laMin[ 1 ] > 0)
	      && (laSec[ 0 ] >= 0) && (laSec[ 1 ] > 0)
	      && ((loRefChar == 'E') || (loRefChar == 'W'))
	      && (loDeg[ 0 ] >= 0) && (loDeg[ 1 ] > 0)
	      && (loMin[ 0 ] >= 0) && (loMin[ 1 ] > 0)
	      && (loSec[ 0 ] >= 0) && (loSec[ 1 ] > 0) )
	  {
	    double laValue = ((double) laDeg[ 0 ] / (double) laDeg[ 1 ])
		+ (((double) laMin[ 0 ] / (double) laMin[ 1 ]) / 60.0)
		+ (((double) laSec[ 0 ] / (double) laSec[ 1 ]) / 3600.0);
	    if( laRefChar == 'S' ) {
	      laValue = -laValue;
	    }
	    double loValue = ((double) loDeg[ 0 ] / (double) loDeg[ 1 ])
		+ (((double) loMin[ 0 ] / (double) loMin[ 1 ]) / 60.0)
		+ (((double) loSec[ 0 ] / (double) loSec[ 1 ]) / 3600.0);
	    if( laRefChar == 'W' ) {
	      loValue = -loValue;
	    }
	    this.gpsPosNumericText = String.format(
						Locale.US,
						"%f, %f",
						laValue,
						loValue );
	    Long laDegNum = divLong( laDeg[ 0 ], laDeg[ 1 ] );
	    Long laMinNum = divLong( laMin[ 0 ], laMin[ 1 ] );
	    Long laSecNum = divLong( laSec[ 0 ], laSec[ 1 ] );
	    Long loDegNum = divLong( loDeg[ 0 ], loDeg[ 1 ] );
	    Long loMinNum = divLong( loMin[ 0 ], loMin[ 1 ] );
	    Long loSecNum = divLong( loSec[ 0 ], loSec[ 1 ] );
	    if( (laDegNum != null)
		&& (laMinNum != null)
		&& (laSecNum != null)
		&& (loDegNum != null)
		&& (loMinNum != null)
		&& (loSecNum != null) )
	    {
	      this.gpsPosDegreeText = String.format(
			Locale.US,
			"%d\u00B0 %d' %d'' %c, %d\u00B0 %d' %d'' %c",
			laDegNum,
			laMinNum,
			laSecNum,
			laRefChar,
			loDegNum,
			loMinNum,
			loSecNum,
			loRefChar );
	    }
	  }
	}
      }
    }
    this.gpsPosTextsDone = true;
  }


  private static Map<Integer,ExifEntry> copyOf(
				Map<Integer,ExifEntry> srcMap )
  {
    Map<Integer,ExifEntry> dstMap = null;
    if( srcMap != null ) {
      dstMap = new HashMap<>();
      dstMap.putAll( srcMap );
    }
    return dstMap;
  }


  private ExifEntry createUInt2Entry( int entryType, int value )
  {
    byte[] dataBuf = new byte[ 2 ];
    if( this.bigEndian ) {
      dataBuf[ 0 ] = (byte) (value >> 8);
      dataBuf[ 1 ] = (byte) value;
    } else {
      dataBuf[ 0 ] = (byte) value;
      dataBuf[ 1 ] = (byte) (value >> 8);
    }
    return new ExifEntry(
			entryType,
			ExifParser.DATA_TYPE_UINT2,
			1,
			dataBuf,
			0,
			dataBuf.length,
			this.bigEndian );
  }


  private ExifEntry createUInt4Entry( int entryType )
  {
    return new ExifEntry(
			entryType,
			ExifParser.DATA_TYPE_UINT4,
			1,
			new byte[ 4 ],
			0,
			4,
			this.bigEndian );
  }


  private static String divAndFormat( long numerator, long denominator )
  {
    String rv = null;
    long   v  = numerator / denominator;
    if( (v * denominator) == numerator ) {
      rv = String.valueOf( v );
    } else {
      rv = getNumFmt().format( (double) numerator / (double) denominator );
    }
    return rv;
  }


  private static Long divLong( long numerator, long denominator )
  {
    long v = numerator / denominator;
    return (v * denominator) == numerator ? Long.valueOf( v ) : null;
  }


  private static String format( Number value )
  {
    String rv = "";
    if( value != null ) {
      if( value instanceof Double ) {
	rv = getNumFmt().format( value.doubleValue() );
      } else if( value instanceof Float ) {
	rv = getNumFmt().format( value.floatValue() );
      } else {
	rv = value.toString();
      }
    }
    return rv;
  }


  private String getChoice(
			Map<Integer,ExifEntry> map,
			int                    key,
			String...              items )
  {
    String rv = null;
    if( map != null ) {
      ExifEntry entry = map.get( key );
      if( entry != null ) {
	Number value = entry.getNumberValue();
	if( value != null ) {
	  int idx = value.intValue();
	  if( (idx >= 0) && (idx < items.length) ) {
	    rv = items[ idx ];
	  }
	}
      }
    }
    return rv;
  }


  private static DateFormat getDateFmt()
  {
    if( dateFmt == null ) {
      dateFmt = new SimpleDateFormat( "yyyy:MM:dd HH:mm:ss" );
    }
    return dateFmt;
  }


  private String getFractionValueText(
				Map<Integer,ExifEntry> map,
				int                    key )
  {
    String rv = null;
    if( map != null ) {
      ExifEntry entry = map.get( key );
      if( entry != null ) {
	long[] values = entry.getRational( 0 );
	if( values != null ) {
	  if( (values[ 0 ] > 0) && (values[ 1 ] > 0) ) {
	    rv = divAndFormat( values[ 0 ], values[ 1 ] );
	  }
	}
      }
    }
    return rv;
  }


  private static NumberFormat getNumFmt()
  {
    if( numFmt == null ) {
      numFmt = NumberFormat.getInstance();
      if( numFmt instanceof DecimalFormat ) {
	((DecimalFormat) numFmt).applyPattern( "###0.###" );
      }
    }
    return numFmt;
  }


  private String getRoundedNumberText(
				Map<Integer,ExifEntry> map,
				int                    key,
				String                 unitText )
  {
    String rv = null;
    if( map != null ) {
      ExifEntry entry = map.get( key );
      if( entry != null ) {
	Number value = entry.getNumberValue();
	if( value != null ) {
	  if( value instanceof Float ) {
	    rv = String.valueOf(
			Math.round( ((Float) value).floatValue() ) );
	  } else if( value instanceof Double ) {
	    rv = String.valueOf(
			Math.round( ((Double) value).doubleValue() ) );
	  } else {
	    rv = value.toString();
	  }
	  if( unitText != null ) {
	    rv = String.format( "%s %s", rv, unitText );
	  }
	}
      }
    }
    return rv;
  }


  private static String getStringValue(
				Map<Integer,ExifEntry> map,
				int                    key )
  {
    String text = null;
    if( map != null ) {
      ExifEntry entry = map.get( key );
      if( entry != null ) {
	text = entry.getStringValue();
      }
    }
    return text;
  }


  private static boolean hasEntries( Map<?,?> map )
  {
    return map != null ?
		!map.isEmpty()
		: false;
  }


  private static boolean isDefaultBigEndian()
  {
    boolean bigEndian = true;
    String  osArch    = System.getProperty( "os.arch" );
    if( osArch != null ) {
      if( osArch.toLowerCase().startsWith( "x86" ) ) {
	bigEndian = false;
      }
    }
    return bigEndian;
  }


  private static void removeObsoleteEntriesFrom(
				Map<Integer,ExifEntry> ifd0Map )
  {
    if( ifd0Map != null ) {
      ifd0Map.remove( 0x0103 );	// compression
      ifd0Map.remove( 0x0106 );	// photometric interpretation
      ifd0Map.remove( 0x0107 );	// thresholding
      ifd0Map.remove( 0x0108 );	// cell width
      ifd0Map.remove( 0x0109 );	// cell length
      ifd0Map.remove( 0x010A );	// fill order
      ifd0Map.remove( 0x013C );	// host computer
      ifd0Map.remove( 0x0193 );	// coding methods
      ifd0Map.remove( 0x0211 );	// YCbCr coefficients
      ifd0Map.remove( 0x0212 );	// YCbCr sub sampling
      ifd0Map.remove( 0x0213 );	// YCbCr positioning
    }
  }


  private void setIfd0Text( int entryType, byte[] textBytes )
  {
    if( this.ifd0Map != null ) {
      this.ifd0Map.remove( entryType );
    } else {
      this.ifd0Map = new HashMap<>();
    }
    setText( this.ifd0Map, entryType, textBytes );
  }


  private void setIfd0Text( int entryType, String text )
  {
    if( text != null ) {
      try {
	setIfd0Text( entryType, text.getBytes( "ISO-8859-1" ) );
      }
      catch( UnsupportedEncodingException ex ) {}
    }
  }


  private void setSoftwareEntry( Map<Integer,ExifEntry> ifd0Map )
  {
    if( ifd0Map != null ) {
      String text    = Main.APPNAME + "\u0020" + Main.VERSION;
      int    textLen = text.length();
      byte[] textBuf = new byte[ textLen + 1 ];
      for( int i = 0; i < textLen; i++ ) {
	textBuf[ i ] = (byte) text.charAt( i );
      }
      textBuf[ textLen ] = 0;
      ifd0Map.put(
		ENTRY_TYPE_SOFTWARE,
		new ExifEntry(
			ENTRY_TYPE_SOFTWARE,
			ExifParser.DATA_TYPE_ASCII,
			textBuf.length,
			textBuf,
			0,
			textBuf.length,
			this.bigEndian ) );
    }
  }


  private void setText(
		Map<Integer,ExifEntry> map,
		int                    entryType,
		byte[]                 textBytes )
  {
    if( map != null ) {
      map.remove( entryType );
      if( textBytes != null ) {
	int pos = 0;
	while( pos < textBytes.length ) {
	  if( textBytes[ pos ] == 0 ) {
	    break;
	  }
	  pos++;
	}
	if( pos > 0 ) {
	  pos++;
	  if( pos != textBytes.length ) {
	    textBytes = Arrays.copyOf( textBytes, pos );
	  }
	  map.put(
		entryType,
		new ExifEntry(
			entryType,
			ExifParser.DATA_TYPE_ASCII,
			textBytes.length,
			textBytes,
			0,
			textBytes.length,
			this.bigEndian ) );
	}
      }
    }
  }


  private static boolean startsWith( byte[] a, String pattern )
  {
    boolean rv   = false;
    int     pLen = pattern.length();
    if( a.length >= pLen ) {
      rv = true;
      for( int i = 0; i < pLen; i++ ) {
	char ch = pattern.charAt( i );
	if( ch != ((char) (a[ i ] & 0xFF)) ) {
	  rv = false;
	  break;
	}
      }
    }
    return rv;
  }


  private void writeDirTo(
			byte[]                 outBuf,
			int                    outOffs,
			int                    targetDirPos,
			AtomicInteger          dirPos,
			AtomicInteger          dataPos,
			Map<Integer,ExifEntry> map )
					throws IllegalStateException
  {
    if( map != null ) {
      if( dirPos.get() > targetDirPos ) {
	throw new IllegalStateException();
      }
      while( dirPos.get() < targetDirPos ) {
	outBuf[ dirPos.getAndIncrement() ] = 0x00;
      }
      int cntValue = 0;
      int cntPos   = outOffs + dirPos.getAndIncrement();
      dirPos.getAndIncrement();
      SortedSet<Integer> entryTypes = new TreeSet<>( map.keySet() );
      for( Integer entryType : entryTypes ) {
	ExifEntry entry = map.get( entryType );
	if( entry != null ) {
	  entry.writeDirEntryTo(
			outBuf,
			outOffs,
			dirPos.get(),
			dataPos.get() );
	  dirPos.addAndGet( ExifParser.ENTRY_SIZE );
	  dataPos.addAndGet(
		entry.writeExtDataTo(
				outBuf,
				outOffs,
				dataPos.get() ) );
	  cntValue++;
	}
      }
      if( this.bigEndian ) {
	outBuf[ cntPos++ ] = (byte) (cntValue >> 8);
	outBuf[ cntPos ]   = (byte) cntValue;
      } else {
	outBuf[ cntPos++ ] = (byte) cntValue;
	outBuf[ cntPos ]   = (byte) (cntValue >> 8);
      }
    }
  }
}
