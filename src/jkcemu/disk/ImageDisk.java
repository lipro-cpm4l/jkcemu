/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette,
 * deren Daten in einer ImageDisk-Datei vorliegen
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;


public class ImageDisk extends AbstractFloppyDisk
{
  private String                                  fileName;
  private String                                  remark;
  private java.util.Date                          diskDate;
  private Map<Integer,java.util.List<SectorData>> side0;
  private Map<Integer,java.util.List<SectorData>> side1;


  public static void export(
			AbstractFloppyDisk disk,
			File               file,
			String             remark ) throws IOException
  {
    /*
     * Pruefen, ob geloschte Sektoren oder Sektoren mit CRC-Fehler
     * vorhanden sind
     */
    boolean errorOrDeleted = false;
    int     cyls           = disk.getCylinders();
    int     sides          = disk.getSides();
    int     sectorBufSize  = 1024;
    for( int cyl = 0; !errorOrDeleted && (cyl < cyls); cyl++ ) {
      for( int head = 0; !errorOrDeleted && (head < sides); head++ ) {
	int n = disk.getSectorsPerCylinder();
	for( int i = 0; i < n; i++ ) {
	  SectorData sector = disk.getSectorByIndex( cyl, head, i );
	  if( sector != null ) {
	    if( sector.getDataLength() > sectorBufSize ) {
	      sectorBufSize = sector.getDataLength();
	    }
	    if( sector.checkError() || sector.isDeleted() ) {
	      errorOrDeleted = true;
	      break;
	    }
	  }
	}
      }
    }

    // Zeitstempel aufbereiten
    java.util.Date diskDate = disk.getDiskDate();
    Calendar       calendar = Calendar.getInstance();
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
      out = new FileOutputStream( file );

      // Dateikopf
      EmuUtil.writeASCII(
		out,
		String.format(
			"IMD 1.1%c: %02d/%02d/%04d %02d:%02d:%02d",
			(char) (errorOrDeleted ? '7' : '6'),
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
	int len = remark.length();
	for( int i = 0; i < len; i++ ) {
	  int ch = remark.charAt( i );
	  if( (ch == 0x00) || (ch == 0x1A) || (ch >= 0x80) ) {
	    break;
	  }
	  out.write( ch );
	}
      }
      out.write( 0x1A );

      // Spuren
      for( int cyl = 0; cyl < cyls; cyl++ ) {
	for( int head = 0; !errorOrDeleted && (head < sides); head++ ) {
	  int nSec = disk.getSectorsPerCylinder();
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
	      int     fillByte = -1;
	      boolean deleted  = sector.isDeleted();
	      boolean err      = sector.checkError();
	      int     dataLen  = sector.getDataLength();
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
		  if( deleted ) {
		    code = 7;		// geloeschte und fehlerhafte Daten
		  } else {
		    code = 5;		// fehlerhafte Daten
		  }
		} else {
		  if( deleted ) {
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
    }
    finally {
      EmuUtil.doClose( out );
    }
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
      int sides          = 0;
      int cyls           = 0;
      int sectorsPerCyl  = 0;
      int diskSectorSize = 0;

      // Datei oeffnen
      in = new FileInputStream( file );
      if( EmuUtil.isGZipFile( file ) ) {
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
	readByte( in );
      }

      // Zeitstempel lesen
      java.util.Date diskDate = null;
      char[]         cBuf     = new char[ 19 ];
      int            pos      = 0;
      while( pos < cBuf.length ) {
	cBuf[ pos++ ] = (char) readByte( in );
      }
      try {
	diskDate = (new SimpleDateFormat(
				"dd/MM/yyyy HH:mm:ss",
				Locale.US )).parse( new String( cBuf ) );
      }
      catch( ParseException ex ) {}

      // Kommentar
      String remark = null;
      int b = readByte( in );
      if( b != 0x1A ) {
	StringBuilder buf = new StringBuilder( 1024 );
	while( b != 0x1A ) {
	  buf.append( (char) (b & 0x7F) );
	  b = readByte( in );
	}
	remark = buf.toString().trim();
      }
      if( b != 0x1A ) {
	throwNoImageDiskFile();
      }

      // Spuren einlesen
      Map<Integer,java.util.List<SectorData>> side0 = null;
      Map<Integer,java.util.List<SectorData>> side1 = null;

      int transferRate = in.read();
      while( transferRate >= 0 ) {
	int cyl      = readByte( in );
	int head     = readByte( in );
	int nSec     = readByte( in );
	int sizeCode = readByte( in );
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
	  sectorNums[ i ] = readByte( in );
	}

	// Sektorzylindertabelle
	int[] sectorCyls = null;
	if( (head & 0x80) != 0 ) {
	  sectorCyls = new int[ nSec ];
	  for( int i = 0; i < nSec; i++ ) {
	    sectorCyls[ i ] = readByte( in );
	  }
	}

	// Sektorkopftabelle
	int[] sectorHeads = null;
	if( (head & 0x40) != 0 ) {
	  sectorHeads = new int[ nSec ];
	  for( int i = 0; i < nSec; i++ ) {
	    sectorHeads[ i ] = readByte( in );
	  }
	}

	// Sektordaten
	head &= 0x01;
	for( int i = 0; i < nSec; i++ ) {
	  boolean crcError = false;
	  boolean deleted  = false;
	  byte[]  secBuf   = null;
	  int     fillByte = 0;
	  int     secNum   = sectorNums[ i ];
	  int     secType  = readByte( in );
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
	      fillByte = readByte( in );
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
	      deleted = true;
	    }
	    if( (secType >= 5) && (secType <= 8) ) {
	      crcError = true;
	    }
	  }
	  Map<Integer,java.util.List<SectorData>> map = null;
	  if( head == 0 ) {
	    if( side0 == null ) {
	      side0 = new HashMap<Integer,java.util.List<SectorData>>();
	    }
	    map = side0;
	  } else if( head == 1 ) {
	    if( side1 == null ) {
	      side1 = new HashMap<Integer,java.util.List<SectorData>>();
	    }
	    map = side1;
	  }
	  if( map != null ) {
	    Integer keyObj                     = new Integer( cyl );
	    java.util.List<SectorData> sectors = map.get( keyObj );
	    if( sectors == null ) {
	      sectors = new ArrayList<SectorData>( nSec > 0 ? nSec : 1 );
	      map.put( keyObj, sectors );
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
	    sectors.add(
		new SectorData(
			i,
			secCyl,
			secHead,
			secNum,
			sizeCode,
			crcError,
			deleted,
			secBuf,
			0,
			secBuf != null ? secBuf.length : 0 ) );

	    if( cyl >= cyls ) {
	      cyls = cyl + 1;
	    }
	    if( head >= sides ) {
	      sides = head + 1;
	    }
	    if( i >= sectorsPerCyl ) {
	      sectorsPerCyl = i + 1;
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
		sides,
		cyls,
		sectorsPerCyl,
		diskSectorSize,
		file.getPath(),
		remark,
		diskDate,
		side0,
		side1 );
    }
    finally {
      EmuUtil.doClose( in );
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
  public int getSectorsOfCylinder( int physCyl, int physHead )
  {
    java.util.List<SectorData> sectors = getSectorList( physCyl, physHead );
    return sectors != null ? sectors.size() : 0;
  }


  @Override
  public void putSettingsTo( Properties props, String prefix )
  {
    if( (props != null) && (this.fileName != null) ) {
      props.setProperty( prefix + "file", this.fileName );
      props.setProperty( prefix + "readonly", "true" );
    }
  }


  @Override
  public boolean supportsDeletedSectors()
  {
    return true;
  }


	/* --- private Konstruktoren und Methoden --- */

  private ImageDisk(
		Frame                                   owner,
		int                                     sides,
		int                                     cyls,
		int                                     sectorsPerCyl,
		int                                     sectorSize,
		String                                  fileName,
		String                                  remark,
		java.util.Date                          diskDate,
		Map<Integer,java.util.List<SectorData>> side0,
		Map<Integer,java.util.List<SectorData>> side1 )
  {
    super( owner, sides, cyls, sectorsPerCyl, sectorSize );
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
      rv = map.get( new Integer( physCyl ) );
    }
    return rv;
  }


  private static void throwNoImageDiskFile() throws IOException
  {
    throw new IOException( "Datei ist keine ImageDisk-Datei." );
  }
}

