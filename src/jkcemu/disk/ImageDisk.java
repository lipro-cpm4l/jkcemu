/*
 * (c) 2012-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette,
 * deren Daten in einer ImageDisk-Datei vorliegen
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileUtil;
import jkcemu.text.CharConverter;


public class ImageDisk extends AbstractFloppyDisk
{
  private String                                  fileName;
  private String                                  remark;
  private java.util.Date                          diskDate;
  private Map<Integer,java.util.List<SectorData>> side0;
  private Map<Integer,java.util.List<SectorData>> side1;


  public static String export(
			AbstractFloppyDisk disk,
			File               file,
			String             remark ) throws IOException
  {
    StringBuilder msgBuf = null;

    /*
     * Pruefen, ob Sektoren mit geloschten Daten oder CRC-Fehlern
     * vorhanden sind
     */
    boolean needsVersion117 = false;
    int     cyls            = disk.getCylinders();
    int     sides           = disk.getSides();
    int     sectorBufSize   = 1024;
    for( int cyl = 0; !needsVersion117 && (cyl < cyls); cyl++ ) {
      for( int head = 0; !needsVersion117 && (head < sides); head++ ) {
	int n = disk.getSectorsOfTrack( cyl, head );
	for( int i = 0; i < n; i++ ) {
	  SectorData sector = disk.getSectorByIndex( cyl, head, i );
	  if( sector != null ) {
	    if( sector.getDataLength() > sectorBufSize ) {
	      sectorBufSize = sector.getDataLength();
	    }
	    if( sector.checkError() || sector.getDataDeleted() ) {
	      needsVersion117 = true;
	      break;
	    }
	  }
	}
      }
    }

    // Zeitstempel aufbereiten
    Calendar       calendar = new GregorianCalendar();
    java.util.Date diskDate = disk.getDiskDate();
    if( diskDate != null ) {
      calendar.clear();
      calendar.setTime( diskDate );
    }

    // Transfer Mode
    int transferMode = 5;	// 250 kbps MFM
    if( disk.getDiskSize() >= (1024 * 1024) ) {
      transferMode = 3;		// 500 kbps MFM
    }

    // Datei oeffnen
    OutputStream out = null;
    try {
      out = FileUtil.createOptionalGZipOutputStream( file );

      // Dateikopf
      EmuUtil.writeASCII(
		out,
		String.format(
			"IMD 1.1%c: %02d/%02d/%04d %02d:%02d:%02d",
			needsVersion117 ? '7' : '6',
			calendar.get( Calendar.DAY_OF_MONTH ),
			calendar.get( Calendar.MONTH ) + 1,
			calendar.get( Calendar.YEAR ),
			calendar.get( Calendar.HOUR_OF_DAY ),
			calendar.get( Calendar.MINUTE ),
			calendar.get( Calendar.SECOND ) ) );

      if( remark == null ) {
	remark = disk.getRemark();
      }
      if( remark != null ) {
	CharConverter cc  = new CharConverter( CharConverter.Encoding.CP850 );
	int           len = remark.length();
	for( int i = 0; i < len; i++ ) {
	  char ch = remark.charAt( i );
	  if( (ch == '\u0000') || (ch == '\u001A') ) {
	    break;
	  }
	  ch = (char) cc.toCharsetByte( ch );
	  if( ch > '\u0000' ) {
	    out.write( ch );
	  }
	}
      }
      out.write( 0x1A );

      // Spuren
      for( int cyl = 0; cyl < cyls; cyl++ ) {
	for( int head = 0; head < sides; head++ ) {
	  int nSec = disk.getSectorsOfTrack( cyl, head );
	  if( nSec > 0 ) {
	    SectorData[] sectors  = new SectorData[ nSec ];
	    boolean      cylMap   = false;
	    boolean      headMap  = false;
	    int          sizeCode = 0;
	    for( int i = 0; i < nSec; i++ ) {
	      SectorData sector = disk.getSectorByIndex( cyl, head, i );
	      if( sector == null ) {
		throw new IOException(
			String.format(
				"Seite %d, Spur %d: Sektor %d nicht gefunden",
				head + 1,
				cyl,
				i + 1  ) );
	      }
	      if( sector.getCylinder() != cyl ) {
		cylMap = true;
	      }
	      if( sector.getHead() != head ) {
		headMap = true;
	      }
	      if( sector.getSizeCode() > sizeCode ) {
		sizeCode = sector.getSizeCode();
	      }
	      sectors[ i ] = sector;
	    }
	    int headByte = head;
	    if( cylMap ) {
	      headByte |= 0x80;
	    }
	    if( headMap ) {
	      headByte |= 0x40;
	    }
	    out.write( transferMode );
	    out.write( cyl );
	    out.write( headByte );
	    out.write( nSec );
	    out.write( sizeCode );
	    for( SectorData sector : sectors ) {
	      out.write( sector.getSectorNum() );
	    }
	    if( cylMap ) {
	      for( SectorData sector : sectors ) {
		out.write( sector.getCylinder() );
	      }
	    }
	    if( headMap ) {
	      for( SectorData sector : sectors ) {
		out.write( sector.getHead() );
	      }
	    }
	    int sectorSize = 128;
	    if( sizeCode > 0 ) {
	      sectorSize <<= sizeCode;
	    }
	    for( SectorData sector : sectors ) {
	      if( sector.hasBogusID() ) {
		if( msgBuf == null ) {
		  msgBuf = new StringBuilder( 1024 );
		}
		msgBuf.append(
			String.format(
				"Seite %d, Spur %d, Sektor %d:"
					+ " Sektor-ID generiert\n",
				head + 1,
				cyl,
				sector.getSectorNum() ) );
	      }
	      int     fillByte    = -1;
	      boolean err         = sector.checkError();
	      boolean dataDeleted = sector.getDataDeleted();
	      int     dataLen     = sector.getDataLength();
	      if( dataLen > 0 ) {
		fillByte = sector.getDataByte( 0 );
		if( fillByte >= 0 ) {
		  for( int i = 1; i < dataLen; i++ ) {
		    if( sector.getDataByte( i ) != fillByte ) {
		      fillByte = -1;
		      break;
		    }
		  }
		}
		int code = 1;		// normale Daten
		if( err ) {
		  if( dataDeleted ) {
		    code = 7;		// geloeschte und fehlerhafte Daten
		  } else {
		    code = 5;		// fehlerhafte Daten
		  }
		} else {
		  if( dataDeleted ) {
		    code = 3;		// geloeschte Daten
		  }
		}
		if( fillByte >= 0 ) {
		  out.write( code + 1 );
		  out.write( fillByte );
		} else {
		  out.write( code );
		  int nBytes = sector.writeTo( out, sectorSize );
		  while( nBytes < sectorSize ) {
		    out.write( 0 );
		    nBytes++;
		  }
		}
	      } else {
		out.write( 0 );		// Daten nicht lesbar
	      }
	    }
	  }
	}
      }
      out.close();
      out = null;

      if( msgBuf != null ) {
	msgBuf.append( "\nDie angezeigten Informationen k\u00F6nnen"
		+ " in einer ImageDisk-Datei nicht gespeichert werden\n"
		+ "und sind deshalb in der erzeugten Datei"
		+ " nicht mehr enthalten.\n" );
      }
    }
    finally {
      EmuUtil.closeSilently( out );
    }
    return msgBuf != null ? msgBuf.toString() : null;
  }


  public static boolean isImageDiskFileHeader( byte[] header )
  {
    boolean rv = false;
    if( header != null ) {
      if( header.length >= 4 ) {
	if( (header[ 0 ] == 'I')
	    && (header[ 1 ] == 'M')
	    && (header[ 2 ] == 'D')
	    && (header[ 3 ] == '\u0020') )
	{
	  rv = true;
	}
      }
    }
    return rv;
  }


  public static ImageDisk readFile(
				Frame owner,
				File  file ) throws IOException
  {
    ImageDisk   rv    = null;
    InputStream in    = null;
    Exception   errEx = null;
    try {
      int cyls            = 0;
      int sides           = 0;
      int sectorsPerTrack = 0;
      int diskSectorSize  = 0;

      // Datei oeffnen
      in = new FileInputStream( file );
      if( FileUtil.isGZipFile( file ) ) {
	in = new GZIPInputStream( in );
      }

      // Kopfblock
      int b0 = in.read();
      int b1 = in.read();
      int b2 = in.read();
      int b3 = in.read();
      if( (b0 != 'I') || (b1 != 'M') || (b2 != 'D') || (b3 != '\u0020') ) {
	throwNoImageDiskFile();
      }
      for( int i = 0; i < 6; i++ ) {
	readMandatoryByte( in );
      }

      // Zeitstempel lesen
      java.util.Date diskDate = null;
      char[]         cBuf     = new char[ 19 ];
      int            pos      = 0;
      while( pos < cBuf.length ) {
	cBuf[ pos++ ] = (char) readMandatoryByte( in );
      }
      try {
	diskDate = (new SimpleDateFormat(
				"dd/MM/yyyy HH:mm:ss",
				Locale.US )).parse( new String( cBuf ) );
      }
      catch( ParseException ex ) {}

      // Kommentar
      String remark = null;
      int b = readMandatoryByte( in );
      if( b != 0x1A ) {
	CharConverter cc  = new CharConverter( CharConverter.Encoding.CP850 );
	StringBuilder buf = new StringBuilder( 1024 );
	while( b != 0x1A ) {
	  b = cc.toUnicode( (char) b );
	  if( b > 0 ) {
	    buf.append( (char) b );
	  }
	  b = readMandatoryByte( in );
	}
	remark = buf.toString();
      }
      if( b != 0x1A ) {
	throwNoImageDiskFile();
      }

      // Spuren einlesen
      Map<Integer,java.util.List<SectorData>> side0 = null;
      Map<Integer,java.util.List<SectorData>> side1 = null;

      int transferRate = in.read();
      while( transferRate >= 0 ) {
	int cyl      = readMandatoryByte( in );
	int head     = readMandatoryByte( in );
	int nSec     = readMandatoryByte( in );
	int sizeCode = readMandatoryByte( in );
	if( sizeCode > 6 ) {
	  throw new IOException(
		String.format(
			"Sektorgr\u00F6\u00DFe Nr. %d nicht unterst\u00FCtzt",
			sizeCode ) );
	}
	int sectorSize = 128;
	if( sizeCode > 0 ) {
	  sectorSize <<= sizeCode;
	}

	// Sektornummerntabelle
	int[] sectorNums = new int[ nSec ];
	for( int i = 0; i < nSec; i++ ) {
	  sectorNums[ i ] = readMandatoryByte( in );
	}

	// Sektorzylindertabelle
	int[] sectorCyls = null;
	if( (head & 0x80) != 0 ) {
	  sectorCyls = new int[ nSec ];
	  for( int i = 0; i < nSec; i++ ) {
	    sectorCyls[ i ] = readMandatoryByte( in );
	  }
	}

	// Sektorkopftabelle
	int[] sectorHeads = null;
	if( (head & 0x40) != 0 ) {
	  sectorHeads = new int[ nSec ];
	  for( int i = 0; i < nSec; i++ ) {
	    sectorHeads[ i ] = readMandatoryByte( in );
	  }
	}

	// Sektordaten
	head &= 0x01;
	for( int i = 0; i < nSec; i++ ) {
	  boolean crcError    = false;
	  boolean dataDeleted = false;
	  byte[]  secBuf      = null;
	  int     fillByte    = 0;
	  int     secNum      = sectorNums[ i ];
	  int     secType     = readMandatoryByte( in );
	  switch( secType ) {
	    case 0:	// keine Daten
	      break;

	    case 1:	// normale Daten
	    case 3:	// normale und geloeschte Daten
	    case 5:	// normale Daten mit Fehler
	    case 7:	// normale und geloeschte Daten Daten mit Fehler
	      secBuf = new byte[ sectorSize ];
	      if( EmuUtil.read( in, secBuf ) != secBuf.length ) {
		throwUnexpectedEOF();
	      }
	      break;

	    case 2:	// komprimierte Daten
	    case 4:	// komprimierte und geloeschte Daten
	    case 6:	// komprimierte Daten mit Fehler
	    case 8:	// komprimierte und geloeschte Daten mit Fehler
	      secBuf  = new byte[ sectorSize ];
	      fillByte = readMandatoryByte( in );
	      Arrays.fill( secBuf, (byte) fillByte );
	      break;

	    default:
	      throw new IOException(
			String.format(
				"Sektor C=%d, H=%d R=%d: Typ %02h"
					+ " nicht unterst\u00FCtzt",
				cyl,
				head,
				secNum,
				secType ) );
	  }
	  if( secBuf != null ) {
	    if( (secType == 3) || (secType == 4)
		|| (secType == 7) || (secType == 8) )
	    {
	      dataDeleted = true;
	    }
	    if( (secType >= 5) && (secType <= 8) ) {
	      crcError = true;
	    }
	  }
	  Map<Integer,java.util.List<SectorData>> map = null;
	  if( head == 0 ) {
	    if( side0 == null ) {
	      side0 = new HashMap<>();
	    }
	    map = side0;
	  } else if( head == 1 ) {
	    if( side1 == null ) {
	      side1 = new HashMap<>();
	    }
	    map = side1;
	  }
	  if( map != null ) {
	    java.util.List<SectorData> sectors = map.get( cyl );
	    if( sectors == null ) {
	      sectors = new ArrayList<>( nSec > 0 ? nSec : 1 );
	      map.put( cyl, sectors );
	    }
	    int secCyl = cyl;
	    if( sectorCyls != null ) {
	      if( i < sectorCyls.length ) {
		secCyl = sectorCyls[ i ];
	      }
	    }
	    int secHead = head;
	    if( sectorHeads != null ) {
	      if( i < sectorHeads.length ) {
		secHead = sectorHeads[ i ];
	      }
	    }
	    SectorData sector = new SectorData(
					i,
					secCyl,
					secHead,
					secNum,
					sizeCode,
					secBuf,
					0,
					secBuf != null ? secBuf.length : 0 );
	    sector.setError( crcError );
	    sector.setDataDeleted( dataDeleted );
	    sectors.add( sector );

	    if( cyl >= cyls ) {
	      cyls = cyl + 1;
	    }
	    if( head >= sides ) {
	      sides = head + 1;
	    }
	    if( i >= sectorsPerTrack ) {
	      sectorsPerTrack = i + 1;
	    }
	    if( sectorSize > diskSectorSize ) {
	      diskSectorSize = sectorSize;
	    }
	  }
	}
	transferRate = in.read();
      }
      rv = new ImageDisk(
		owner,
		cyls,
		sides,
		sectorsPerTrack,
		diskSectorSize,
		file.getPath(),
		remark,
		diskDate,
		side0,
		side1 );
    }
    finally {
      EmuUtil.closeSilently( in );
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
    return "ImageDisk-Datei";
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
    SectorData                 rv      = null;
    java.util.List<SectorData> sectors = getSectorList( physCyl, physHead );
    if( sectors != null ) {
      if( (sectorIdx >= 0) && (sectorIdx < sectors.size()) ) {
	rv = sectors.get( sectorIdx );
      }
    }
    return rv;
  }


  @Override
  public int getSectorsOfTrack( int physCyl, int physHead )
  {
    java.util.List<SectorData> sectors = getSectorList( physCyl, physHead );
    return sectors != null ? sectors.size() : 0;
  }


  @Override
  public void putSettingsTo( Properties props, String prefix )
  {
    if( (props != null) && (this.fileName != null) ) {
      props.setProperty( prefix + PROP_FILE, this.fileName );
      props.setProperty( prefix + PROP_READONLY, EmuUtil.VALUE_TRUE );
    }
  }


  @Override
  public boolean supportsDeletedDataSectors()
  {
    return true;
  }


	/* --- private Konstruktoren und Methoden --- */

  private ImageDisk(
		Frame                                   owner,
		int                                     cyls,
		int                                     sides,
		int                                     sectorsPerTrack,
		int                                     sectorSize,
		String                                  fileName,
		String                                  remark,
		java.util.Date                          diskDate,
		Map<Integer,java.util.List<SectorData>> side0,
		Map<Integer,java.util.List<SectorData>> side1 )
  {
    super( owner, cyls, sides, sectorsPerTrack, sectorSize );
    this.fileName = fileName;
    this.remark   = remark;
    this.diskDate = diskDate;
    this.side0    = side0;
    this.side1    = side1;
  }


  private java.util.List<SectorData> getSectorList(
						int physCyl,
						int physHead )
  {
    java.util.List<SectorData>              rv  = null;
    Map<Integer,java.util.List<SectorData>> map = ((physHead & 0x01) != 0 ?
							side1 : side0);
    if( map != null ) {
      rv = map.get( physCyl );
    }
    return rv;
  }


  private static int readMandatoryByte( InputStream in ) throws IOException
  {
    int b = in.read();
    if( b < 0 ) {
      throwUnexpectedEOF();
    }
    return b;
  }


  private static void throwNoImageDiskFile() throws IOException
  {
    throw new IOException( "Datei ist keine ImageDisk-Datei." );
  }
}
