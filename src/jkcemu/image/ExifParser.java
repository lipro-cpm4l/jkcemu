/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Parsen von EXIF-Datenps
 */

package jkcemu.image;

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import jkcemu.base.EmuUtil;


public class ExifParser
{
  public static final int DATA_TYPE_UBYTE      = 1;
  public static final int DATA_TYPE_ASCII      = 2;
  public static final int DATA_TYPE_UINT2      = 3;
  public static final int DATA_TYPE_UINT4      = 4;
  public static final int DATA_TYPE_URATIONAL  = 5;
  public static final int DATA_TYPE_SBYTE      = 6;
  public static final int DATA_TYPE_BYTE_ARRAY = 7;
  public static final int DATA_TYPE_SINT2      = 8;
  public static final int DATA_TYPE_SINT4      = 9;
  public static final int DATA_TYPE_SRATIONAL  = 10;
  public static final int DATA_TYPE_FLOAT4     = 11;
  public static final int DATA_TYPE_FLOAT8     = 12;
  public static final int ENTRY_SIZE           = 12;

  public static final byte[] PATTERN_JPEG = { (byte) 0xFF, (byte) 0xD8 };

  public static final byte[] PATTERN_EXIF = {
				0x45, 0x78, 0x69, 0x66, 0x00, 0x00 };

  public static final byte[] PATTERN_BIG_ENDIAN = {
				0x4D, 0x4D, 0x00, 0x2A, 0x00, 0x00,
				0x00, 0x08 };

  public static final byte[] PATTERN_LITTLE_ENDIAN = {
				0x49, 0x49, 0x2A, 0x00, 0x08, 0x00,
				0x00, 0x00 };


  /*
   * Es werden nur die in den nachfolgenden Tabellen
   * angegebenen Tag-IDs gelesen.
   * Insbesonder Herstellerspezifische IDs, aber auch solche,
   * die zu unbedeutend sind oder die zu viel Interpretationsspielraum
   * lassen, werden nicht gelesen.
   */
  private static final int[] ifd0Tags = {
	0x0100, 0x0101, 0x0103, 0x0106, 0x0107, 0x0108, 0x0109, 0x010A,
	0x010D, 0x010E, 0x010F, 0x0110, 0x0112, 0x011A, 0x011B, 0x011C,
	0x011D, 0x011E, 0x011F, 0x0128, 0x0129, 0x0131, 0x0132, 0x013B,
	0x013C, 0x013E, 0x013F, 0x0193, 0x0211, 0x0212, 0x0213, 0x0214,
	0x8298, 0x8769, 0x8825, 0x9C9C };

  private static final int[] subIfdTags = {
	0x829A, 0x829D, 0x8822, 0x8824, 0x8827, 0x9000, 0x9003, 0x9004,
	0x9101, 0x9102, 0x9201, 0x9202, 0x9203, 0x9204, 0x9205, 0x9206,
	0x9207, 0x9208, 0x9209, 0x920A, 0x9217, 0x9286, 0x9290, 0x9291,
	0x9292, 0x9400, 0x9401, 0x9402, 0x9403, 0x9404, 0x9405, 0xA000,
	0xA001, 0xA002, 0xA003, 0xA004, 0xA005, 0xA20E, 0xA20F, 0xA210,
	0xA211, 0xA215, 0xA217, 0xA300, 0xA301, 0xA401, 0xA402, 0xA403,
	0xA404, 0xA405, 0xA406, 0xA407, 0xA408, 0xA409, 0xA40A, 0xA40C,
	0xA420, 0xA430, 0xA431, 0xA432, 0xA433, 0xA434, 0xA435, 0xA460,
	0xA461, 0xA500 };

  private static final int[] gpsTags = {
	0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007,
	0x001D, 0x001E, 0x001F };

  private static final int[] interopTags = {
	0x0001, 0x0002, 0x1000, 0x1001, 0x1002 };

  private byte[]                 buf;
  private int                    tiffOffs;
  private int                    endPos;
  private int                    subIfdPos;
  private int                    gpsPos;
  private int                    interopPos;
  private boolean                bigEndian;
  private Map<Integer,ExifEntry> ifd0Map;
  private Map<Integer,ExifEntry> subIfdMap;
  private Map<Integer,ExifEntry> gpsMap;
  private Map<Integer,ExifEntry> interopMap;


  public static boolean matchesAt( byte[] buf, int offs, byte... pattern )
  {
    boolean rv   = true;
    int     pIdx = 0;
    while( (offs < buf.length) && (pIdx < pattern.length) ) {
      if( buf[ offs ] != pattern[ pIdx ] ) {
	rv = false;
	break;
      }
      offs++;
      pIdx++;
    }
    if( pIdx < pattern.length ) {
      rv = false;
    }
    return rv;
  }


  public static ExifData parseExif(
				byte[] buf,
				int    exifOffs,
				int    endPos )
  {
    ExifData rv        = null;
    Boolean  bigEndian = null;
    if( matchesAt( buf, exifOffs, PATTERN_EXIF ) ) {
      int tiffOffs = exifOffs + 6;
      if( matchesAt( buf, tiffOffs, PATTERN_BIG_ENDIAN ) ) {
	bigEndian = Boolean.TRUE;
      } else if( matchesAt( buf, tiffOffs, PATTERN_LITTLE_ENDIAN ) ) {
	bigEndian = Boolean.FALSE;
      }
      if( bigEndian != null ) {
	rv = parseExifTiff(
			buf,
			tiffOffs,
			buf.length,
			bigEndian.booleanValue() );
      }
    }
    return rv;
  }


  public static ExifData parseFileBytes( byte[] buf )
  {
    ExifData rv = null;
    if( buf != null ) {
      if( matchesAt( buf, 0, PATTERN_JPEG ) ) {
	int pos = 0;
	while( (pos + 3) < buf.length ) {
	  if( buf[ pos ] != (byte) 0xFF ) {
	    // kein JPEG-Start-Tag
	    break;
	  }
	  byte t = buf[ pos++ ];
	  if( t == (byte) 0xD9 ) {
	    // JPEG-Ende-Tag
	    break;
	  }
	  /*
	   * Restlaenge des Tags
	   *
	   * EXIF-Daten muessen mindestens 28 Bytes lang sein:
	   *    6 Bytes EXIF-Kennung
	   *    8 Bytes Byte-Order-Kennung
	   *    2 Bytes Anzahl Eintraege in FD0
	   *   12 Bytes erster Eintrag in FD0
	   */
	  int len = EmuUtil.getInt2BE( buf, pos ) - 2;
	  if( len < 28 ) {
	    break;
	  }
	  if( t == (byte) 0xE1 ) {
	    // EXIF-Tag
	    rv = parseExif( buf, pos, buf.length );
	    break;
	  }
	  // Tag ueberspringen
	  pos += len;
	}
      }
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ExifParser(
		byte[]  buf,
		int     tiffOffs,
		int     endPos,
		boolean bigEndian )
  {
    this.buf        = buf;
    this.tiffOffs   = tiffOffs;
    this.endPos     = endPos;
    this.bigEndian  = bigEndian;
    this.ifd0Map    = new HashMap<>();
    this.subIfdMap  = null;
    this.subIfdPos  = -1;
    this.gpsMap     = null;
    this.gpsPos     = -1;
    this.interopMap = null;
    this.interopPos = -1;
  }


	/* --- private Methoden --- */

  private static ExifData parseExif(
				InputStream in,
				int         len ) throws IOException
  {
    ExifData rv = null;

    // EXIF-Daten lesen
    byte[] buf   = new byte[ len ];
    int    nRead = 0;
    while( nRead < buf.length ) {
      int nTmp = in.read( buf, nRead, buf.length - nRead );
      if( nTmp <= 0 ) {
	break;
      }
      nRead += nTmp;
    }
    if( nRead == buf.length ) {
      rv = parseExif( buf, 0, buf.length );
    }
    return rv;
  }


  private static ExifData parseExifTiff(
				byte[]  buf,
				int     tiffOffs,
				int     endPos,
				boolean bigEndian )
  {
    return (new ExifParser( buf, tiffOffs, endPos, bigEndian )).parseExif();
  }


  private ExifData parseExif()
  {
    parseDir( this.tiffOffs + 8, this.ifd0Map, ifd0Tags );
    if( this.subIfdPos >= 0 ) {
      this.subIfdMap = new HashMap<>();
      parseDir( this.subIfdPos, this.subIfdMap, subIfdTags );
    }
    if( this.gpsPos >= 0 ) {
      this.gpsMap = new HashMap<>();
      parseDir( this.gpsPos, this.gpsMap, gpsTags );
    }
    if( this.interopPos >= 0 ) {
      this.interopMap = new HashMap<>();
      parseDir( this.interopPos, this.interopMap, interopTags );
    }
    return new ExifData(
			this.ifd0Map,
			this.subIfdMap,
			this.gpsMap,
			this.interopMap,
			this.bigEndian );
  }
    

  private void parseDir( int pos, Map<Integer,ExifEntry> dstMap, int[] tags )
  {
    int nEntries = getInt2( pos );
    pos += 2;
    for( int i = 0; i < nEntries; i++ ) {
      if( (pos + ENTRY_SIZE) > this.endPos ) {
	break;
      }
      int entryType = getInt2( pos );
      if( Arrays.binarySearch( tags, entryType ) >= 0 ) {
	int dataType  = getInt2( pos + 2 );
	int dataCnt   = (int) getInt4( pos + 4 );
	int typeSize  = 0;
	switch( dataType ) {
	  case DATA_TYPE_UBYTE:
	  case DATA_TYPE_ASCII:
	  case DATA_TYPE_SBYTE:
	  case DATA_TYPE_BYTE_ARRAY:
	    typeSize = 1;
	    break;
	  case DATA_TYPE_UINT2:
	  case DATA_TYPE_SINT2:
	    typeSize = 2;
	    break;
	  case DATA_TYPE_UINT4:
	  case DATA_TYPE_SINT4:
	  case DATA_TYPE_FLOAT4:
	    typeSize = 4;
	    break;
	  case DATA_TYPE_URATIONAL:
	  case DATA_TYPE_SRATIONAL:
	  case DATA_TYPE_FLOAT8:
	    typeSize = 8;
	    break;
	}
	int dataPos = pos + 8;
	int dataLen = dataCnt * typeSize;
	if( dataLen > 4 ) {
	  dataPos = (int) getInt4( dataPos ) + this.tiffOffs;
	}
	if( (dataCnt >= 0)
	    && ((dataPos + dataLen) <= this.endPos)
	    && (dataLen <= Integer.MAX_VALUE) )
	{
	  if( (entryType == ExifData.ENTRY_TYPE_SUBIFD)
	      && (dstMap == this.ifd0Map) )
	  {
	    if( (dataType == 4) && (dataLen == 4) ) {
	      this.subIfdPos = (int) getInt4( pos + 8 ) + this.tiffOffs;
	    }
	  }
	  else if( (entryType == ExifData.ENTRY_TYPE_GPS)
		   && (dstMap == this.ifd0Map) )
	  {
	    if( (dataType == 4) && (dataLen == 4) ) {
	      this.gpsPos = (int) getInt4( pos + 8 ) + this.tiffOffs;
	    }
	  }
	  else if( (entryType == ExifData.ENTRY_TYPE_INTEROP)
		   && (dstMap == this.subIfdMap) )
	  {
	    if( (dataType == 4) && (dataLen == 4) ) {
	      this.interopPos = (int) getInt4( pos + 8 ) + this.tiffOffs;
	    }
	  }
	  dstMap.put(
		entryType,
		new ExifEntry(
			entryType,
			dataType,
			dataCnt,
			this.buf,
			dataPos,
			dataLen,
			this.bigEndian ) );
	}
      }
      pos = pos + ENTRY_SIZE;
    }
  }


  private int getInt2( int pos )
  {
    return this.bigEndian ?
			EmuUtil.getInt2BE( this.buf, pos )
			: EmuUtil.getInt2LE( this.buf, pos );
  }


  private long getInt4( int pos )
  {
    return this.bigEndian ?
			EmuUtil.getInt4BE( this.buf, pos )
			: EmuUtil.getInt4LE( this.buf, pos );
  }
}
