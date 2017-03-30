/*
 * (c) 2010-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette, deren Daten in einer CopyQM-Datei vorliegen
 *
 * Die Informationen ueber das CopyQM-Format sowie die hier verwendete
 * CRC-Tabelle stammen aus dem Projekt LIBDSK, (c) 2001-2005 John Elliott
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;
import jkcemu.text.CharConverter;


public class CopyQMDisk extends AbstractFloppyDisk
{
  private final static long[] crcTable = {
	0x00000000L, 0x77073096L, 0xEE0E612CL, 0x990951BAL,
	0x076DC419L, 0x706AF48FL, 0xE963A535L, 0x9E6495A3L,
	0x0EDB8832L, 0x79DCB8A4L, 0xE0D5E91EL, 0x97D2D988L,
	0x09B64C2BL, 0x7EB17CBDL, 0xE7B82D07L, 0x90BF1D91L,
	0x1DB71064L, 0x6AB020F2L, 0xF3B97148L, 0x84BE41DEL,
	0x1ADAD47DL, 0x6DDDE4EBL, 0xF4D4B551L, 0x83D385C7L,
	0x136C9856L, 0x646BA8C0L, 0xFD62F97AL, 0x8A65C9ECL,
	0x14015C4FL, 0x63066CD9L, 0xFA0F3D63L, 0x8D080DF5L,
	0x3B6E20C8L, 0x4C69105EL, 0xD56041E4L, 0xA2677172L,
	0x3C03E4D1L, 0x4B04D447L, 0xD20D85FDL, 0xA50AB56BL,
	0x35B5A8FAL, 0x42B2986CL, 0xDBBBC9D6L, 0xACBCF940L,
	0x32D86CE3L, 0x45DF5C75L, 0xDCD60DCFL, 0xABD13D59L,
	0x26D930ACL, 0x51DE003AL, 0xC8D75180L, 0xBFD06116L,
	0x21B4F4B5L, 0x56B3C423L, 0xCFBA9599L, 0xB8BDA50FL,
	0x2802B89EL, 0x5F058808L, 0xC60CD9B2L, 0xB10BE924L,
	0x2F6F7C87L, 0x58684C11L, 0xC1611DABL, 0xB6662D3DL,
	0x76DC4190L, 0x01DB7106L, 0x98D220BCL, 0xEFD5102AL,
	0x71B18589L, 0x06B6B51FL, 0x9FBFE4A5L, 0xE8B8D433L,
	0x7807C9A2L, 0x0F00F934L, 0x9609A88EL, 0xE10E9818L,
	0x7F6A0DBBL, 0x086D3D2DL, 0x91646C97L, 0xE6635C01L,
	0x6B6B51F4L, 0x1C6C6162L, 0x856530D8L, 0xF262004EL,
	0x6C0695EDL, 0x1B01A57BL, 0x8208F4C1L, 0xF50FC457L,
	0x65B0D9C6L, 0x12B7E950L, 0x8BBEB8EAL, 0xFCB9887CL,
	0x62DD1DDFL, 0x15DA2D49L, 0x8CD37CF3L, 0xFBD44C65L,
	0x4DB26158L, 0x3AB551CEL, 0xA3BC0074L, 0xD4BB30E2L,
	0x4ADFA541L, 0x3DD895D7L, 0xA4D1C46DL, 0xD3D6F4FBL,
	0x4369E96AL, 0x346ED9FCL, 0xAD678846L, 0xDA60B8D0L,
	0x44042D73L, 0x33031DE5L, 0xAA0A4C5FL, 0xDD0D7CC9L,
	0x5005713CL, 0x270241AAL, 0xBE0B1010L, 0xC90C2086L,
	0x5768B525L, 0x206F85B3L, 0xB966D409L, 0xCE61E49FL,
	0x5EDEF90EL, 0x29D9C998L, 0xB0D09822L, 0xC7D7A8B4L,
	0x59B33D17L, 0x2EB40D81L, 0xB7BD5C3BL, 0xC0BA6CADL,
	0xEDB88320L, 0x9ABFB3B6L, 0x03B6E20CL, 0x74B1D29AL,
	0xEAD54739L, 0x9DD277AFL, 0x04DB2615L, 0x73DC1683L,
	0xE3630B12L, 0x94643B84L, 0x0D6D6A3EL, 0x7A6A5AA8L,
	0xE40ECF0BL, 0x9309FF9DL, 0x0A00AE27L, 0x7D079EB1L,
	0xF00F9344L, 0x8708A3D2L, 0x1E01F268L, 0x6906C2FEL,
	0xF762575DL, 0x806567CBL, 0x196C3671L, 0x6E6B06E7L,
	0xFED41B76L, 0x89D32BE0L, 0x10DA7A5AL, 0x67DD4ACCL,
	0xF9B9DF6FL, 0x8EBEEFF9L, 0x17B7BE43L, 0x60B08ED5L,
	0xD6D6A3E8L, 0xA1D1937EL, 0x38D8C2C4L, 0x4FDFF252L,
	0xD1BB67F1L, 0xA6BC5767L, 0x3FB506DDL, 0x48B2364BL,
	0xD80D2BDAL, 0xAF0A1B4CL, 0x36034AF6L, 0x41047A60L,
	0xDF60EFC3L, 0xA867DF55L, 0x316E8EEFL, 0x4669BE79L,
	0xCB61B38CL, 0xBC66831AL, 0x256FD2A0L, 0x5268E236L,
	0xCC0C7795L, 0xBB0B4703L, 0x220216B9L, 0x5505262FL,
	0xC5BA3BBEL, 0xB2BD0B28L, 0x2BB45A92L, 0x5CB36A04L,
	0xC2D7FFA7L, 0xB5D0CF31L, 0x2CD99E8BL, 0x5BDEAE1DL,
	0x9B64C2B0L, 0xEC63F226L, 0x756AA39CL, 0x026D930AL,
	0x9C0906A9L, 0xEB0E363FL, 0x72076785L, 0x05005713L,
	0x95BF4A82L, 0xE2B87A14L, 0x7BB12BAEL, 0x0CB61B38L,
	0x92D28E9BL, 0xE5D5BE0DL, 0x7CDCEFB7L, 0x0BDBDF21L,
	0x86D3D2D4L, 0xF1D4E242L, 0x68DDB3F8L, 0x1FDA836EL,
	0x81BE16CDL, 0xF6B9265BL, 0x6FB077E1L, 0x18B74777L,
	0x88085AE6L, 0xFF0F6A70L, 0x66063BCAL, 0x11010B5CL,
	0x8F659EFFL, 0xF862AE69L, 0x616BFFD3L, 0x166CCF45L,
	0xA00AE278L, 0xD70DD2EEL, 0x4E048354L, 0x3903B3C2L,
	0xA7672661L, 0xD06016F7L, 0x4969474DL, 0x3E6E77DBL,
	0xAED16A4AL, 0xD9D65ADCL, 0x40DF0B66L, 0x37D83BF0L,
	0xA9BCAE53L, 0xDEBB9EC5L, 0x47B2CF7FL, 0x30B5FFE9L,
	0xBDBDF21CL, 0xCABAC28AL, 0x53B39330L, 0x24B4A3A6L,
	0xBAD03605L, 0xCDD70693L, 0x54DE5729L, 0x23D967BFL,
	0xB3667A2EL, 0xC4614AB8L, 0x5D681B02L, 0x2A6F2B94L,
	0xB40BBE37L, 0xC30C8EA1L, 0x5A05DF1BL, 0x2D02EF8DL };

  private String         fileName;
  private String         remark;
  private java.util.Date diskDate;
  private byte[]         diskBytes;
  private int            sectorSizeCode;
  private int            sectorOffset;
  private int            skew;


  public static String export(
			AbstractFloppyDisk disk,
			File               file,
			String             remark ) throws IOException
  {
    StringBuilder msgBuf = null;
    OutputStream  out    = null;
    try {
      boolean hasDeleted     = false;
      int     diskSize       = disk.getDiskSize();
      int     sides          = disk.getSides();
      int     cyls           = disk.getCylinders();
      int     sectorsPerCyl  = disk.getSectorsPerCylinder();
      int     sectorSize     = disk.getSectorSize();
      int     totalSectorCnt = sides * cyls * sectorsPerCyl;

      // Sektorffset ermitteln
      int sectorOffset = disk.getSectorOffset();

      // Interleave ermitteln
      int interleave     = 1;
      int firstSectorNum = -1;
      if( cyls > 0 ) {
	int cylSectors = disk.getSectorsOfCylinder( 0, 0 );
	if( cylSectors > 2 ) {
	  SectorData sector = disk.getSectorByIndex( 0, 0, 0 );
	  if( sector != null ) {
	    int nextSectorNum = sector.getSectorNum() + 1;
	    for( int i = 1; i < cylSectors; i++ ) {
	      sector = disk.getSectorByIndex( 0, 0, i );
	      if( sector != null ) {
		if( sector.getSectorNum() == nextSectorNum ) {
		  interleave = i;
		  break;
		}
	      }
	    }
	  }
	}
      }

      // Skew ermitteln
      int skew = 0;
      if( (cyls > 1) && (firstSectorNum > 0)  ) {
	int cylSectors = disk.getSectorsOfCylinder( 1, 0 );
	if( cylSectors > 1 ) {
	  for( int i = 0; i < cylSectors; i++ ) {
	    SectorData sector = disk.getSectorByIndex( 1, 0, i );
	    if( sector == null ) {
	      break;
	    }
	    if( sector.getSectorNum() == firstSectorNum ) {
	      skew = i;
	    }
	  }
	}
      }

      // Datenbereich erzeugen
      ByteArrayOutputStream dataBuf = new ByteArrayOutputStream( diskSize );
      for( int cyl = 0; cyl < cyls; cyl++ ) {
	for( int head = 0; head < sides; head++ ) {
	  int cylSectors = disk.getSectorsOfCylinder( cyl, head );
	  if( cylSectors != sectorsPerCyl ) {
	    if( msgBuf == null ) {
	      msgBuf = new StringBuilder( 1024 );
	    }
	    msgBuf.append(
		String.format(
			"Seite %d, Spur %d: %d anstelle von %d Sektoren"
				+ " vorhanden",
			head + 1,
			cyl,
			cylSectors,
			sectorsPerCyl ) );
	  }
	  for( int i = 0; i < sectorsPerCyl; i++ ) {
	    SectorData sector = disk.getSectorByID(
						cyl,
						head,
						cyl,
						head,
						i + 1 + sectorOffset,
						-1 );
	    if( sector == null ) {
	      throw new IOException(
		String.format(
			"Seite %d, Spur %d: Sektor %d nicht gefunden"
				+ "\n\nDas CopyQM-Format unterst\u00FCtzt"
				+ " keine freie Sektoranordnung.",
			head + 1,
			cyl,
			i + 1  ) );
	    }
	    if( sector.checkError()
		|| sector.hasBogusID()
		|| sector.isDeleted() )
	    {
	      if( msgBuf == null ) {
		msgBuf = new StringBuilder( 1024 );
	      }
	      msgBuf.append(
			String.format(
				"Seite %d, Spur %d, Sektor %d:",
				head + 1,
				cyl,
				sector.getSectorNum() ) );
	      boolean appended = false;
	      if( sector.hasBogusID() ) {
		msgBuf.append( " Sektor-ID generiert" );
		appended = true;
	      }
	      if( sector.checkError() ) {
		if( appended ) {
		  msgBuf.append( (char) ',' );
		}
		msgBuf.append( " CRC-Fehler" );
		appended = true;
	      }
	      if( sector.isDeleted() ) {
		if( appended ) {
		  msgBuf.append( (char) ',' );
		}
		msgBuf.append( " als gel\u00F6scht markiert" );
		appended   = true;
		hasDeleted = true;
	      }
	      msgBuf.append( (char) '\n' );
	    }
	    if( sector.getDataLength() > sectorSize ) {
	      throw new IOException(
		String.format(
			"Seite %d, Spur %d: Sektor %d ist zu gro\u00DF."
				+ "\n\nDas CopyQM-Format unterst\u00FCtzt"
				+ " keine \u00FCbergro\u00DFen Sektoren.",
			head + 1,
			cyl,
			sector.getSectorNum() ) );
	    }
	    int n = sector.writeTo( dataBuf, sectorSize );
	    while( n < sectorSize ) {
	      dataBuf.write( 0 );
	      n++;
	    }
	  }
	}
      }
      byte[] dataBytes = dataBuf.toByteArray();

      // Kommentar aufbereiten
      int                   remarkLen = 0;
      ByteArrayOutputStream remarkBuf = null;
      if( remark == null ) {
	remark = disk.getRemark();
      }
      if( remark != null ) {
	int len = remark.length();
	if( len > 0 ) {
	  CharConverter cc = new CharConverter(
					CharConverter.Encoding.CP850 );
	  remarkBuf = new ByteArrayOutputStream( len );
	  for( int i = 0; i < len; i++ ) {
	    char ch = remark.charAt( i );
	    if( (ch == '\u0000') || (ch == '\u001A') ) {
	      break;
	    }
	    ch = (char) cc.toCharsetByte( ch );
	    if( (ch > '\u0000') && (remarkBuf.size() < 0x7FFE) ) {
	      remarkBuf.write( ch );
	    }
	  }
	  remarkLen = remarkBuf.size();
	}
      }

      // Kopfbereich erzeugen
      ByteArrayOutputStream headerBuf  = new ByteArrayOutputStream( 256 );
      EmuUtil.writeASCII( headerBuf, "CQ" );
      headerBuf.write( 0x14 );
      headerBuf.write( sectorSize );
      headerBuf.write( sectorSize >> 8 );

      // im Blind-Mode 6 Nullbytes
      for( int i = 0; i < 6; i++ ) {
	headerBuf.write( 0 );
      }

      /*
       * Das folgende Byte gibt im DOS-Mode die Anzahl der Sektoren
       * bei kleiner Diskettengroesse an.
       * Bei von CopyQM im Blind Mode erzeugten Dateien
       * wurden folgende Werte vorgefunden:
       *    800K-Format (2x80x5x1024):  5Ah / 90 dez
       *    640K-Format (2x80x16x256):  70h / 112 dez
       *    720K-Format (2x80x9x512):   62h / 98 dez
       *   1440K-Format (2x80x18x512):  74h / 116 dez
       *   1760K-Format (2x80x11x1024): 66h / 102 dez
       *
       * Da die Bedeutung des Bytes im Blind Mode unbekannt ist,
       * wurde hier eine Formel erfunden,
       * die die oben genannten Werte erzeugt.
       */
      headerBuf.write( cyls + (sides * sectorsPerCyl) );

      // im Blind-Mode 4 Nullbytes
      for( int i = 0; i < 4; i++ ) {
	headerBuf.write( 0 );
      }

      headerBuf.write( sectorsPerCyl );
      headerBuf.write( sectorsPerCyl >> 8 );
      headerBuf.write( sides );
      headerBuf.write( sides >> 8 );

      // im Blind-Mode 8 Nullbytes
      for( int i = 0; i < 8; i++ ) {
	headerBuf.write( 0 );
      }

      // Beschreibung Medium
      EmuUtil.writeASCII( headerBuf, String.valueOf( diskSize / 1024 ) );
      headerBuf.write( 'K' );
      if( sides == 1 ) {
	EmuUtil.writeASCII( headerBuf, " Single-Sided" );
      } else if( sides == 2 ) {
	EmuUtil.writeASCII( headerBuf, " Double-Sided" );
      }
      for( int i = headerBuf.size(); i < 0x58; i++ ) {
	headerBuf.write( 0 );
      }

      // Mode
      headerBuf.write( 1 );		// Blind Mode

      // 0:=DD, 1=HD, 2=ED
      headerBuf.write( diskSize / 1024 / 1024 );

      // Anzahl Zylinder
      headerBuf.write( cyls );		// in der erzeugten Datei
      headerBuf.write( cyls );		// auf der Quell-Diskette

      // CRC der Daten
      long dataCRC = computeCRC( dataBytes, dataBytes.length );
      for( int i = 0; i < 4; i++ ) {
	headerBuf.write( (int) dataCRC );
	dataCRC >>= 8;
      }

      // Volume Label
      EmuUtil.writeASCII( headerBuf, "** NONE **" );
      headerBuf.write( 0 );

      // Uhrzeit und Datum
      Calendar       cal      = new GregorianCalendar();
      java.util.Date diskDate = disk.getDiskDate();
      if( diskDate != null ) {
	cal.clear();
	cal.setTime( diskDate );
      }
      int time = ((cal.get( Calendar.HOUR_OF_DAY ) << 11) & 0xF800)
			| ((cal.get( Calendar.MINUTE ) << 5) & 0x07E0)
			| ((cal.get( Calendar.SECOND ) / 2) & 0x001F);
      int date = (((cal.get( Calendar.YEAR ) - 1980) << 9) & 0xFE00)
			| (((cal.get( Calendar.MONTH ) + 1) << 5) & 0x01E0)
			| (cal.get( Calendar.DAY_OF_MONTH ) & 0x001F);
      headerBuf.write( time );
      headerBuf.write( time >> 8 );
      headerBuf.write( date );
      headerBuf.write( date >> 8 );

      // Kommentarlaenge
      headerBuf.write( remarkLen );
      headerBuf.write( remarkLen >> 8 );

      // Sektoroffset
      headerBuf.write( sectorOffset );

      // 2 Nullbytes, Bedeutung unbekannt
      headerBuf.write( 0 );
      headerBuf.write( 0 );

      // Interleave
      headerBuf.write( interleave );

      // Skew
      headerBuf.write( skew );

      /*
       * Laufwerkstyp:
       *   Es kann auf Basis der Parameter nur ein wahrscheinlicher
       *   Laufwerkstyp ermittelt werden.
       */
      int driveType = 1;			// 5.25 Zoll, 360 KByte
      if( disk.getDiskSize() >= (360 * 1024) ) {
	driveType = 2;				// 5.25 Zoll, 48 tpi
      }
      if( (disk.getCylinders() >= 50)
	  && (disk.getSectorSize() == 512) )
      {
	int n = disk.getSectorsPerCylinder();
	if( (n >= 8) && (n <= 10) ) {
	  driveType = 3;			// 3.5 Zoll DD
	} else if( n >= 17 ) {
	  driveType = 4;			// 3.5 Zoll HD
	}
      }
      headerBuf.write( driveType );

      // 13 Nullbytes, Bedeutung unbekannt
      for( int i = 0; i < 13; i++ ) {
	headerBuf.write( 0 );
      }

      // Pruefsumme fuer Kopfbereich
      byte[] headerBytes = headerBuf.toByteArray();
      int    headerCks   = 0;
      for( byte b : headerBytes ) {
	headerCks += ((int) b & 0xFF);
      }

      // Datei schreiben
      out = EmuUtil.createOptionalGZipOutputStream( file );
      if( headerBytes != null ) {
	out.write( headerBytes );
	out.write( -headerCks );
	if( (remarkBuf != null) && (remarkLen > 0) ) {
	  remarkBuf.writeTo( out );
	}
	if( dataBytes != null ) {
	  int maxSegLen = sectorSize * sectorsPerCyl;
	  if( maxSegLen >= 0x8000 ) {
	    maxSegLen = sectorSize;
	  }
	  if( maxSegLen >= 0x8000 ) {
	    maxSegLen = 0x7FFF;
	  }
	  int segBegPos = 0;
	  while( segBegPos < dataBytes.length ) {
	    int  p = segBegPos;
	    byte b = dataBytes[ p++ ];
	    int  n = 1;
	    while( (n < maxSegLen) && (p < dataBytes.length) ) {
	      if( dataBytes[ p ] != b ) {
		break;
	      }
	      n++;
	      p++;
	    }
	    int nEqSectors = n / sectorSize;
	    if( nEqSectors > 0 ) {
	      n       = nEqSectors * sectorSize;
	      int len = -n;
	      out.write( len );
	      out.write( len >> 8 );
	      out.write( b );
	    } else {
	      n = Math.min( sectorSize, dataBytes.length - segBegPos );
	      out.write( n );
	      out.write( n >> 8 );
	      out.write( dataBytes, segBegPos, n );
	    }
	    segBegPos += n;
	  }
	}
      }
      out.close();
      out = null;

      if( msgBuf != null ) {
	msgBuf.append( "\nDie angezeigten Informationen k\u00F6nnen"
		+ " in einer CopyQM-Datei nicht gespeichert werden\n"
		+ "und sind deshalb in der erzeugten Datei"
		+ " nicht mehr enthalten.\n" );
	if( hasDeleted ) {
	  msgBuf.append( "\nGel\u00F6schte Sektoren werden"
		+ " in CopyQM-Dateien nicht unterst\u00FCtzt\n"
		+ "und sind deshalb als normale Sektoren enthalten.\n" );
	}
      }
    }
    finally {
      EmuUtil.closeSilent( out );
    }
    return msgBuf != null ? msgBuf.toString() : null;
  }


  public static boolean isCopyQMFileHeader( byte[] header )
  {
    boolean rv = false;
    if( header != null ) {
      if( header.length >= 3 ) {
	if( (header[ 0 ] == 'C')
	    && (header[ 1 ] == 'Q')
	    && (header[ 2 ] == 0x14) )
	{
	  rv = true;
	}
      }
    }
    return rv;
  }


  public static CopyQMDisk readFile(
				Frame owner,
				File  file ) throws IOException
  {
    CopyQMDisk  rv    = null;
    InputStream in    = null;
    Exception   errEx = null;
    try {
      in = new FileInputStream( file );
      if( EmuUtil.isGZipFile( file ) ) {
	in = new GZIPInputStream( in );
      }

      // Kopfblock lesen
      byte[] header = new byte[ 133 ];
      if( EmuUtil.read( in, header ) != header.length ) {
	throwNoCopyQMFile();
      }
      if( (header[ 0 ] != 'C')
	  || (header[ 1 ] != 'Q')
	  || (header[ 2 ] != 0x14) )
      {
	throwNoCopyQMFile();
      }
      int sectorSize     = EmuUtil.getWord( header, 3 );
      int sectorSizeCode = SectorData.getSizeCode( sectorSize );
      if( (sectorSize < 1) || (sectorSizeCode < 0) ) {
	throwUnsupportedCopyQMFmt(
		String.format(
			"%d Byte Sektorgr\u00F6\u00DFe"
				+ " nicht unterst\u00FCtzt",
			sectorSize ) );
      }
      int sectorsPerCyl = EmuUtil.getWord( header, 0x10 );
      if( sectorsPerCyl < 1 ) {
	throwUnsupportedCopyQMFmt(
		String.format( "Sektoren pro Spur", sectorsPerCyl ) );
      }
      int sides = (int) header[ 0x12 ] & 0xFF;
      if( (sides < 1) || (sides > 2) ) {
	throwUnsupportedCopyQMFmt(
		String.format( "%d Seiten nicht unterst\u00FCtzt", sides ) );
      }
      int usedCyls = (int) header[ 0x5A ] & 0xFF;
      int cyls     = (int) header[ 0x5B ] & 0xFF;
      if( cyls < usedCyls ) {
	cyls = usedCyls;
      }
      if( cyls < 1 ) {
	throwUnsupportedCopyQMFmt(
		String.format( "%d Spuren nicht unterst\u00FCtzt", sides ) );
      }
      int sectorOffset = (int) header[ 0x71 ] & 0xFF;

      // Datum lesen
      java.util.Date diskDate = null;
      int            time     = EmuUtil.getWord( header, 0x6B );
      int            date     = EmuUtil.getWord( header, 0x6d );
      int            year     = 1980 + ((date >> 9) & 0x7F);
      int            month    = (date >> 5) & 0x0F;
      int            day      = date & 0x1F;
      int            hour     = (time >> 11) & 0x1F;
      int            minute   = (time >> 5) & 0x3F;
      int            second   = (time & 0x1F) * 2;
      if( (month >= 1) && (month <= 12)
	  && (day >= 1) && (day <= 31)
	  && (hour < 24) && (minute < 60) && (second < 60) )
      {
	diskDate = (new GregorianCalendar(
				year, month - 1, day,
				hour, minute, second )).getTime();
      }

      // Kommentar lesen
      String remark    = null;
      int    remarkLen = EmuUtil.getWord( header, 0x6F );
      if( remarkLen < 0 ) {
	throwUnsupportedCopyQMFmt(
		String.format(
			"Mysteri\u00F6se Kommentarl\u00E4nge: %d",
			remarkLen ) );
      }
      if( remarkLen > 0 ) {
	CharConverter cc  = new CharConverter( CharConverter.Encoding.CP850 );
	StringBuilder buf = new StringBuilder( remarkLen );
	while( remarkLen > 0 ) {
	  int b = in.read();
	  if( b < 0 ) {
	    break;
	  }
	  b = cc.toUnicode( (char) b );
	  if( b > 0 ) {
	    buf.append( (char) b );
	  }
	  --remarkLen;
	}
	remark = buf.toString().trim();
      }

      // Daten lesen
      int    diskSize  = sides * cyls * sectorsPerCyl * sectorSize;
      byte[] diskBytes = new byte[ diskSize ];
      Arrays.fill( diskBytes, (byte) 0 );
      int dstPos = 0;
      while( dstPos < diskSize ) {
	int lLen = in.read();
	int hLen = in.read();
	if( (lLen < 0) || (hLen < 0) ) {
	  break;
	}
	int len = (hLen << 8) | lLen;
	if( (len & 0x8000) != 0 ) {
	  int n = (int) -((short) len);
	  int b = in.read();
	  if( b < 0 ) {
	    break;
	  }
	  while( (dstPos < diskSize) && (n > 0) ) {
	    diskBytes[ dstPos++ ] = (byte) b;
	    --n;
	  }
	} else {
	  while( (len > 0) && (dstPos < diskSize) ) {
	    int b = in.read();
	    if( b < 0 ) {
	      break;
	    }
	    diskBytes[ dstPos++ ] = (byte) b;
	    --len;
	  }
	}
      }

      // Disk-Object anlegen
      rv = new CopyQMDisk(
			owner,
			sides,
			cyls,
			sectorsPerCyl,
			sectorSize,
			file.getPath(),
			remark,
			diskDate,
			diskBytes,
			sectorSizeCode,
			sectorOffset,
			(int) header[ 0x74 ] & 0xFF,	// Interleave
			(int) header[ 0x75 ] & 0xFF );	// Skew

      // CRC ueber die entpackten Daten berechnen
      long orgCRC = 0L;
      long newCRC = 0L;
      for( int i = 0x5F; i >= 0x5C; --i ) {
	orgCRC = (orgCRC << 8) | ((int) header[ i ] & 0xFF);
      }
      if( orgCRC != 0 ) {
	newCRC = computeCRC( diskBytes, dstPos );
      }
      if( orgCRC != newCRC ) {
	rv.setWarningText( 
		"Die CopyQM-Datei scheint defekt zu sein (CRC-Fehler)!" );
      }
    }
    finally {
      EmuUtil.closeSilent( in );
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public java.util.Date getDiskDate()
  {
    return this.diskDate;
  }


  @Override
  public String getFileFormatText()
  {
    return "CopyQM-Datei";
  }


  @Override
  public String getRemark()
  {
    return this.remark;
  }


  @Override
  public SectorData getSectorByIndex(
				int physCyl,
				int physHead,
				int sectorIdx )
  {
    // Sektorindex entsprechend Interleave umrechnen
    sectorIdx = sectorIndexToInterleave( sectorIdx );

    // Sektorindex entsprechend Skew umrechnen
    if( this.skew > 0 ) {
      int sectorsPerCyl = getSectorsPerCylinder();
      if( (sectorsPerCyl > 0) && (this.skew < sectorsPerCyl) ) {
	sectorIdx += (sectorsPerCyl
			- (this.skew * (physCyl % sectorsPerCyl)));
	if( sectorIdx < 0 ) {
	  sectorIdx += ((-sectorIdx / sectorsPerCyl) * sectorsPerCyl);
	  if( sectorIdx < 0 ) {
	    sectorIdx += sectorsPerCyl;
	  }
	} else if( sectorIdx >= sectorsPerCyl ) {
	  sectorIdx -= ((sectorIdx / sectorsPerCyl) * sectorsPerCyl);
	}
      }
    }

    return getSectorByIndexInternal( physCyl, physHead, sectorIdx );
  }


  @Override
  public SectorData getSectorByID(
                                int physCyl,
                                int physHead,
                                int cyl,
                                int head,
                                int sectorNum,
                                int sizeCode )
  {
    SectorData rv = getSectorByIndexInternal(
				physCyl,
				physHead,
				sectorNum - 1 - this.sectorOffset );
    if( rv != null ) {
      if( (rv.getCylinder() != cyl)
	  || (rv.getHead() != head)
	  || (rv.getSectorNum() != sectorNum)
	  || ((sizeCode >= 0) && (rv.getSizeCode() != sizeCode)) )
      {
	rv = null;
      }
    }
    return rv;
  }


  @Override
  public int getSectorOffset()
  {
    return this.sectorOffset;
  }


  @Override
  public void putSettingsTo( Properties props, String prefix )
  {
    if( (props != null) && (this.fileName != null) ) {
      props.setProperty( prefix + PROP_FILE, this.fileName );
      props.setProperty( prefix + PROP_READONLY, EmuUtil.VALUE_TRUE );
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private CopyQMDisk(
		Frame          owner,
		int            sides,
		int            cyls,
		int            sectorsPerCyl,
		int            sectorSize,
		String         fileName,
		String         remark,
		java.util.Date diskDate,
		byte[]         diskBytes,
		int            sectorSizeCode,
		int            sectorOffset,
		int            interleave,
		int            skew )
  {
    super( owner, sides, cyls, sectorsPerCyl, sectorSize, interleave );
    this.fileName       = fileName;
    this.remark         = remark;
    this.diskDate       = diskDate;
    this.diskBytes      = diskBytes;
    this.sectorSizeCode = sectorSizeCode;
    this.sectorOffset   = sectorOffset;
    this.skew           = skew;
  }


  private static long computeCRC( byte[] dataBytes, int len )
  {
    long crc = 0;
    if( dataBytes != null ) {
      for( int i = 0; i < len; i++ ) {
	int b = (int) dataBytes[ i ] & 0x7F;
	crc   = crcTable[ ((int) ((long) b ^ crc)) & 0x3F ] ^ (crc >> 8);
      }
    }
    return crc;
  }


  /*
   * Die Methode liefert den Sektor an der angegebenen Position
   * im internen Datenbereich zurueck.
   * Interleave wird dabei nicht beruecksichtigt.
   */
  private SectorData getSectorByIndexInternal(
					int physCyl,
					int physHead,
					int sectorIdx )
  {
    SectorData rv = null;
    if( (physCyl >= 0) && (physHead >= 0) && (sectorIdx >= 0) ) {
      int sides         = getSides();
      int cyls          = getCylinders();
      int sectorsPerCyl = getSectorsPerCylinder();
      int sectorSize = getSectorSize();
      if( (physHead < sides)
	  && (physCyl < cyls)
          && (sectorIdx < sectorsPerCyl)
          && (sectorSize > 0) )
      {
        int nSkipSectors = sides * sectorsPerCyl * physCyl;
        if( physHead > 0 ) {
          nSkipSectors += sectorsPerCyl;
        }
        nSkipSectors += sectorIdx;
	rv = new SectorData(
			sectorIdx,
			physCyl,
			physHead,
			sectorIdx + 1 + this.sectorOffset,
			this.sectorSizeCode,
			this.diskBytes,
                        nSkipSectors * sectorSize,
			sectorSize );
      }
    }
    return rv;
  }


  private static void throwUnsupportedCopyQMFmt( String msg )
							throws IOException
  {
    throw new IOException( "Die Datei enth\u00E4lt ein nicht"
			+ " unterst\u00FCtztes CopyQM-Format:\n"
			+ msg );
  }


  private static void throwNoCopyQMFile() throws IOException
  {
    throw new IOException( "Datei ist keine CopyQM-Datei." );
  }
}
