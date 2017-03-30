/*
 * (c) 2009-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette, deren Daten in einer TeleDisk-Datei vorliegen
 *
 * Der hier implementierte Algorithmus basiert auf den Informationen von:
 *   http://www.willsworks.net/wteledsk.htm
 *   http://www.classiccmp.org/dunfield/img54306/td0notes.txt
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;
import jkcemu.etc.CRC16;
import jkcemu.text.CharConverter;


public class TeleDisk extends AbstractFloppyDisk
{
  private static final int CRC_POLYNOM = 0xA097;
  private static final int CRC_INIT    = 0;

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
    // Kommentar aufbereiten
    byte[] remarkBytes = null;
    if( remark == null ) {
      remark = disk.getRemark();
    }
    if( remark != null ) {
      int len = remark.length();
      if( len > 0 ) {
	ByteArrayOutputStream  buf
			= new ByteArrayOutputStream( remark.length() + 8 );
	Calendar       calendar = new GregorianCalendar();
	java.util.Date diskDate = disk.getDiskDate();
	if( diskDate != null ) {
	  calendar.clear();
	  calendar.setTime( diskDate );
	}
	buf.write( calendar.get( Calendar.YEAR ) - 1900 );
	buf.write( calendar.get( Calendar.MONTH ) );
	buf.write( calendar.get( Calendar.DAY_OF_MONTH ) );
	buf.write( calendar.get( Calendar.HOUR_OF_DAY ) );
	buf.write( calendar.get( Calendar.MINUTE ) );
	buf.write( calendar.get( Calendar.SECOND ) );
	CharConverter cc = new CharConverter( CharConverter.Encoding.CP850 );
	for( int i = 0; i < len; i++ ) {
	  char ch = remark.charAt( i );
	  if( (ch == '\u0000') || (ch == '\u001A') ) {
	    break;
	  }
	  ch = (char) cc.toCharsetByte( ch );
	  if( (ch > '\u0000') && (buf.size() < 0x7E) ) {
	    buf.write( ch );
	  }
	}
	if( buf.size() > 6 ) {
	  buf.write( 0 );
	  remarkBytes = buf.toByteArray();
	}
      }
    }

    // Datei oeffnen
    OutputStream out = null;
    try {
      int nCyls  = disk.getCylinders();
      int nSides = disk.getSides();
      if( (nCyls < 1) || (nSides < 1) ) {
	throw new IOException( "Kein Inhalt vorhanden" );
      }

      // Datei oeffnen
      out       = EmuUtil.createOptionalGZipOutputStream( file );
      CRC16 crc = new CRC16( CRC_POLYNOM, CRC_INIT );

      // Kennung
      writeByte( out, 'T', crc );
      writeByte( out, 'D', crc );
      writeByte( out, 0, crc );

      /*
       * Pruefsequenz:
       *   Alle Dateien eines Multi-File-Archivs muessen
       *   hier den gleichen Wert haben.
       *   Zwischen den Archiven sollte sich dieser Wert moeglichst
       *   unterscheiden.
       *   Deshalb wird hier eine Zufallszahl geschrieben.
       */
      writeByte(
		out,
		(new Random( System.currentTimeMillis() )).nextInt( 0x100 ),
		crc );

      // TeleDisk-Format
      writeByte( out, 0x15, crc );

      // Datenrate
      int dataRate = 0;					// 250 kbps
      if( disk.getDiskSize() >= (1024 * 1024) ) {
	dataRate = 2;					// 500 kbps
      }
      writeByte( out, dataRate, crc );

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
      writeByte( out, driveType, crc );

      // Stepping und Remark Flag
      writeByte(
		out,
		remarkBytes != null ? 0x80 : 0,	// Flag + Einzelschritt
		crc );

      // DOS Allocation Flag
      writeByte( out, 0, crc );

      // Anzahl Seiten
      writeByte( out, disk.getSides(), crc );

      // CRC des Dateikopfs
      long v = crc.getValue();
      out.write( (int) v );
      out.write( (int) (v >> 8) );

      // Kommentar
      if( remarkBytes != null ) {
	int len = remarkBytes.length - 6;	// Laenge ohne Zeit
	crc.reset();
	crc.update( len );
	crc.update( len >> 8 );
	crc.update( remarkBytes, 0, remarkBytes.length );
	int crcValue = (int) crc.getValue();
	out.write( crcValue );
	out.write( crcValue >> 8 );
	out.write( len );
	out.write( len >> 8 );
	out.write( remarkBytes );
      }

      // Spuren
      int lastCyl      = -1;
      int lastHead     = -1;
      int lastTrackCRC = -1;
      for( int physCyl = 0; physCyl < nCyls; physCyl++ ) {
	lastCyl = physCyl;
	for( int physHead = 0; physHead < nSides; physHead++ ) {
	  lastHead = physHead;

	  // Sektoren der Spur
	  int n = disk.getSectorsOfCylinder( physCyl, physHead );
	  if( n > 0 ) {
	    java.util.List<SectorData> sectors = new ArrayList<>( n );
	    for( int i = 0; i < n; i++ ) {
	      SectorData sector = disk.getSectorByIndex(
						physCyl,
						physHead,
						i );
	      if( sector != null ) {
		sectors.add( sector );
	      }
	    }
	    n = sectors.size();
	    if( n > 0 ) {
	      crc.reset();
	      writeByte( out, n, crc );
	      writeByte( out, physCyl, crc );
	      writeByte( out, physHead, crc );
	      lastTrackCRC = (int) crc.getValue();
	      out.write( lastTrackCRC );
	      for( SectorData sector : sectors ) {
		int dataLen = sector.getDataLength();

		// Sektor-ID
		crc.reset();
		writeByte( out, sector.getCylinder(), crc );
		writeByte( out, sector.getHead(), crc );
		writeByte( out, sector.getSectorNum(), crc );
		writeByte( out, sector.getSizeCode(), crc );

		// Control-Byte
		int secCtrl = 0;
		if( sector.checkError() ) {
		  secCtrl |= 0x02;
		}
		if( sector.isDeleted() ) {
		  secCtrl |= 0x04;
		}
		if( sector.hasBogusID() ) {
		  secCtrl |= 0x40;
		}
		if( dataLen <= 0 ) {
		  secCtrl |= 0x20;
		}

		// Pruefsumme des Sektorkopfs
		writeByte( out, secCtrl, crc );

		// Sektordaten aufbereiten
		if( dataLen > 0 ) {
		  crc.reset();
		  boolean allEquals = true;
		  int     firstByte = sector.getDataByte( 0 );
		  crc.update( firstByte );
		  for( int i = 1; i < dataLen; i++ ) {
		    int b = sector.getDataByte( i );
		    crc.update( b );
		    if( b != firstByte ) {
		      allEquals = false;
		    }
		  }
		  out.write( (int) crc.getValue() );
		  if( allEquals ) {
		    ByteArrayOutputStream buf
				= new ByteArrayOutputStream( 128 );
		    int nRemain = dataLen;
		    while( nRemain > 5 ) {
		      int nSeg = nRemain / 2;
		      if( nSeg > 255 ) {
			nSeg = 255;
		      }
		      buf.write( 1 );		// RLE
		      buf.write( nSeg );
		      buf.write( firstByte );
		      buf.write( firstByte );
		      nRemain -= (nSeg * 2);
		    }
		    buf.write( 0 );		// COPY
		    buf.write( nRemain );
		    while( nRemain > 0 ) {
		      buf.write( firstByte );
		      --nRemain;
		    }
		    int size = buf.size() + 1;
		    out.write( size );
		    out.write( size >> 8 );
		    out.write( 2 );		// Kodierung: segmentiert
		    buf.writeTo( out );
		  } else {
		    int len = dataLen + 1;	// + 1 Byte Kodierung
		    out.write( len );
		    out.write( len >> 8 );
		    out.write( 0 );		// Kodierung: unkomprimiert
		    sector.writeTo( out, dataLen );
		  }
		} else {
		  out.write( (int) crc.getValue() );
		}
	      }
	    }
	  }
	}
      }

      // Dateiende
      out.write( 0xFF );
      out.write( lastCyl );
      out.write( lastHead );
      out.write( lastTrackCRC );
      out.close();
      out = null;
    }
    finally {
      EmuUtil.closeSilent( out );
    }
    return null;
  }


  public static boolean isTeleDiskFileHeader( byte[] header )
  {
    boolean rv = false;
    if( header != null ) {
      if( header.length >= 3 ) {
	if( (((header[ 0 ] == 'T') && (header[ 1 ] == 'D'))
		|| ((header[ 0 ] == 't') && (header[ 1 ] == 'd')))
            && (header[ 2 ] == 0) )

	{
	  rv = true;
	}
      }
    }
    return rv;
  }


  public static TeleDisk readFile(
				Frame   owner,
				File    file,
				boolean enableAutoRepair ) throws IOException
  {
    TeleDisk    rv    = null;
    InputStream in    = null;
    Exception   errEx = null;
    try {
      in = new FileInputStream( file );
      if( EmuUtil.isGZipFile( file ) ) {
	in = new GZIPInputStream( in );
      }
      boolean autoRepaired = false;

      // Kopfblock lesen
      int head0 = in.read();
      int head1 = in.read();
      int head2 = in.read();
      if( (head0 == 't') && (head1 == 'd') ) {
	throwUnsupportedTeleDiskFmt(
		"Advanced Compression nicht unterst\u00FCtzt" );
      }
      if( (head0 != 'T') || (head1 != 'D') || (head2 != 0) ) {
	throw new IOException( "Datei ist keine TeleDisk-Datei." );
      }
      readMandatoryByte( in );				// Check Sequence
      int fmtVersion = readMandatoryByte( in );		// Version
      if( fmtVersion != 0x15 ) {
	throwUnsupportedTeleDiskFmt(
		String.format(
			"Formatversion %02X nicht unterst\u00FCtzt",
			fmtVersion ) );
      }
      readMandatoryByte( in );			// Data Rate
      readMandatoryByte( in );			// Drive Type
						// Stepping und Remark Flag
      boolean hasRemark = ((readMandatoryByte( in ) & 0x80) != 0);
      readMandatoryByte( in );			// DOS Allocation Flag
      int sides = readMandatoryByte( in );	// Anzahl Seiten
      if( sides != 1 ) {
	sides = 2;
      }
      readMandatoryWord( in );			// CRC Kopfbereich

      // Kommantarblock mit Zeitstempel lesen
      String         remark   = null;
      String         dateText = null;
      java.util.Date diskDate = null;
      if( hasRemark ) {
	int crcValue = readMandatoryWord( in );
	int len      = readMandatoryWord( in );
	int year     = readMandatoryByte( in ) + 1900;
	int month    = readMandatoryByte( in );
	int day      = readMandatoryByte( in );
	int hour     = readMandatoryByte( in );
	int minute   = readMandatoryByte( in );
	int second   = readMandatoryByte( in );
	if( (month < 12) && (day >= 1) && (day <= 31)
	    && (hour < 24) && (minute < 60) && (second < 60) )
	{
	  diskDate = (new GregorianCalendar(
				year, month, day,
				hour, minute, second )).getTime();
	}
	if( len < 0 ) {
	  len = 0;
	}
	StringBuilder buf = new StringBuilder( len + 32 );
	CharConverter cc  = new CharConverter( CharConverter.Encoding.CP850 );
	if( len > 0 ) {
	  int trimLen = -1;
	  while( len > 0 ) {
	    int ch = readMandatoryByte( in );
	    if( ch == '\u0000' ) {
	      ch = '\n';
	      if( trimLen < 0 ) {
		trimLen = buf.length();
	      }
	    } else {
	      trimLen = -1;
	    }
	    ch = cc.toUnicode( ch );
	    if( ch > '\u0000' ) {
	      buf.append( (char) ch );
	    }
	    --len;
	  }
	  if( trimLen >= 0 ) {
	    buf.setLength( trimLen );
	  }
	}
	remark = buf.toString().trim();
      }

      // Spuren einlesen
      Map<Integer,java.util.List<SectorData>> side0 = null;
      Map<Integer,java.util.List<SectorData>> side1 = null;

      int cyls           = 0;
      int sectorsPerCyl  = 0;
      int diskSectorSize = 0;
      for(;;) {
	int nSec  = in.read();
	int track = in.read();
	int head  = in.read();
	in.read();			// CRC Kopfbereich der Spur

	if( (nSec == 0xFF)
	    || (nSec < 0)
	    || (track < 0)
	    || (head < 0) )
	{
	  break;
	}

	if( nSec > 0 ) {
	  head &= 0x01;
	  if( track >= cyls ) {
	    cyls = track + 1;
	  }
	  if( sectorsPerCyl == 0 ) {
	    sectorsPerCyl = nSec;
	  }

	  // Sektoren lesen
	  boolean                    abnormalFmt        = false;
	  java.util.List<SectorData> bogusHeaderSectors = null;
	  for( int i = 0; i < nSec; i++ ) {
	    int secTrack    = readMandatoryByte( in );
	    int secHead     = readMandatoryByte( in );
	    int secNum      = readMandatoryByte( in );
	    int secSizeCode = readMandatoryByte( in );
	    int secCtrl     = readMandatoryByte( in );
	    int secCrcValue = readMandatoryByte( in );

	    // Datenpuffer anlegen
	    byte[] secBuf = null;
	    if( (secSizeCode >= 0) && (secSizeCode <= 6) ) {
	      int secSize = 128;
	      if( secSizeCode > 0 ) {
		secSize <<= secSizeCode;
	      }
	      secBuf = new byte[ secSize ];
	      Arrays.fill( secBuf, (byte) 0 );
	      if( secSize > diskSectorSize ) {
		diskSectorSize = secSize;
	      }
	    }
	    if( secBuf == null ) {
	      throwUnsupportedTeleDiskFmt(
		String.format(
			"Code=%02X f\u00FCr Sektorgr\u00F6\u00DFe"
				+ " nicht unterst\u00FCtzt",
			secSizeCode ) );
	    }

	    /*
	     * Bits in secCtrl:
	     *   0x01: Sektor mehrfach auf der Spur enthalten
	     *   0x02: Sektor mit CRC-Fehler gelesen
	     *   0x04: Sektor hat Deleted Data Address Mark
	     *   0x10: Datenbereich wurde uebersprungen
	     *         (keine Daten enthalten)
	     *   0x20: Sektor hat ID-Feld, aber keine Daten
	     *   0x40: Sektor hat Daten, aber keinen Kopf
	     *         (Kopfdaten generiert)
	     */
	    boolean secCrcError = ((secCtrl & 0x02) != 0);
	    boolean secDeleted  = ((secCtrl & 0x04) != 0);
	    boolean bogusHeader = ((secCtrl & 0x40) != 0);
	    if( !bogusHeader
		&& ((secTrack != track) || (secHead != head)) )
	    {
	      abnormalFmt = true;
	    }

	    if( (secCtrl & 0x30) == 0 ) {
	      int len = readMandatoryWord( in );
	      if( len > 0 ) {
		int secEncoding = readMandatoryByte( in );
		--len;

		int pos = 0;
		switch( secEncoding ) {
		  case 0:
		    while( len > 0 ) {
		      int b = readMandatoryByte( in );
		      if( pos < secBuf.length ) {
			secBuf[ pos++ ] = (byte) b;
		      }
		      --len;
		    }
		    break;

		  case 1:
		    if( len >= 4 ) {
		      int n  = readMandatoryWord( in );
		      int b0 = readMandatoryByte( in );
		      int b1 = readMandatoryByte( in );
		      len -= 4;
		      while( n > 0 ) {
			if( pos < secBuf.length ) {
			  secBuf[ pos++ ] = (byte) b0;
			}
			if( pos < secBuf.length ) {
			  secBuf[ pos++ ] = (byte) b1;
			}
			--n;
		      }
		    }
		    break;

		  case 2:
		    while( len >= 2 ) {
		      int t = readMandatoryByte( in );
		      int n = readMandatoryByte( in );
		      len -= 2;
		      switch( t ) {
			case 0:
			  while( (len > 0) && (n > 0) ) {
			    int b = readMandatoryByte( in );
			    if( pos < secBuf.length ) {
			      secBuf[ pos++ ] = (byte) b;
			    }
			    --n;
			    --len;
			  }
			  if( n > 0 ) {
			    throwLengthMismatch();
			  }
			  break;

			case 1:
			  if( len >= 2 ) {
			    int b0 = readMandatoryByte( in );
			    int b1 = readMandatoryByte( in );
			    len -= 2;
			    while( n > 0 ) {
			      if( pos < secBuf.length ) {
				secBuf[ pos++ ] = (byte) b0;
			      }
			      if( pos < secBuf.length ) {
				secBuf[ pos++ ] = (byte) b1;
			      }
			      --n;
			    }
			  }
			  break;

			default:
			  throwUnsupportedTeleDiskFmt(
				String.format(
					"Sektorunterkodierung %02X"
						+ " nicht unterst\u00FCtzt",
					t ) );
		      }
		    }
		    while( len > 0 ) {
		      readMandatoryByte( in );
		      --len;
		    }
		    break;

		  default:
		    throwUnsupportedTeleDiskFmt(
			String.format(
				"Sektorkodierung %02Xh"
					+ " nicht unterst\u00FCtzt",
				secEncoding ) );
		}
		if( len > 0 ) {
		  throwLengthMismatch();
		}
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
	      java.util.List<SectorData> sectors = map.get( track );
	      if( sectors == null ) {
		sectors = new ArrayList<>( nSec > 0 ? nSec : 1 );
		map.put( track, sectors );
	      }
	      // doppelte Sektoren herausfiltern
	      boolean found  = false;
	      int     secLen = (secBuf != null ? secBuf.length : 0);
	      if( enableAutoRepair ) {
		for( SectorData sector : sectors ) {
		  if( sector.equalsSectorID(
					secTrack,
					secHead,
					secNum,
					secSizeCode ) )
		  {
		    found = true;
		    if( sector.equalsData( secBuf, 0, secLen ) ) {
		      /*
		       * Daten sind gleich:
		       *   sofern moeglich, den Sektor als fehlerfrei
		       *   und nicht geloescht behalten
		       */
		      if( !secCrcError ) {
			sector.setError( false );
		      }
		      if( !secDeleted ) {
			sector.setDeleted( false );
		      }
		    } else {
		      /*
		       * Daten sind unterschiedlich
		       *   sofern moeglich, den fehlerhaften durch den
		       *   fehlerfreien Sektor ersetzen
		       */
		      if( sector.checkError() && !secCrcError ) {
			if( secDeleted && !sector.isDeleted() ) {
			  secDeleted   = false;
			  autoRepaired = true;
			}
			sector.setData( secDeleted, secBuf, secLen );
			sector.setError( false );
		      }
		    }
		  }
		}
	      }
	      if( !found ) {
		SectorData sector = new SectorData(
						sectors.size(),
						secTrack,
						secHead,
						secNum,
						secSizeCode,
						secBuf,
						0,
						secLen );
		sector.setBogusID( bogusHeader );
		sector.setError( secCrcError );
		sector.setDeleted( secDeleted );
		sectors.add( sector );
		if( bogusHeader && !abnormalFmt ) {
		  if( bogusHeaderSectors == null ) {
		    bogusHeaderSectors = new ArrayList<>();
		  }
		  bogusHeaderSectors.add( sector );
		}
	      }
	    }
	  }

	  /*
	   * ggf. automatische Reparatur von Sektoren,
	   * deren Kopf nicht gelesen werden konnte
	   *
	   * Das ist jedoch nicht moeglich,
	   * wenn mehr als ein Sektor pro Spur betroffen sind.
	   */
	  if( enableAutoRepair
	      && (bogusHeaderSectors != null)
	      && !abnormalFmt )
	  {
	    if( bogusHeaderSectors.size() == 1 ) {
	      SectorData bogusHeaderSector = bogusHeaderSectors.get( 0 );

	      // Sektoren der Spur holen
	      Map<Integer,java.util.List<SectorData>> sideData     = null;
	      java.util.List<SectorData>              trackSectors = null;
	      if( head == 0 ) {
		sideData = side0;
	      } else if( head == 1 ) {
		sideData = side1;
	      }
	      if( sideData != null ) {
		trackSectors = sideData.get( track );
	      }
	      if( trackSectors != null ) {
		if( (trackSectors.size() == nSec)
		    && trackSectors.contains( bogusHeaderSector ) )
		{
		  // Sektornummern ermitteln
		  SortedSet<Integer> secNums = new TreeSet<>();
		  for( SectorData sector : trackSectors ) {
		    if( sector != bogusHeaderSector ) {
		      secNums.add( sector.getSectorNum() );
		    }
		  }
		  if( !secNums.isEmpty() && (secNums.size() + 1) == nSec ) {
		    int secNumRange = secNums.last() - secNums.first() + 1;
		    if( secNumRange == (nSec - 1) ) {
		      // Sektornummer fehlt am Anfang oder Ende
		      switch( secNums.first().intValue() ) {
			case 1:
			  // Sektornummer fehlt am Ende
			  bogusHeaderSector.setSectorID(
						track,
						head,
						secNums.last() + 1 );
			  bogusHeaderSector.setBogusID( false );
			  autoRepaired = true;
			  break;
			case 2:
			  // Sektornummer fehlt am Anfang
			  bogusHeaderSector.setSectorID( track, head, 1 );
			  bogusHeaderSector.setBogusID( false );
			  autoRepaired = true;
		      }
		    } else if( secNumRange == nSec ) {
		      // Sektornummer fehlt in der Mitte
		      int tmpSecNum  = secNums.first().intValue() + 1;
		      int lastSecNum = secNums.last().intValue();
		      while( tmpSecNum <= lastSecNum ) {
			if( !secNums.contains( tmpSecNum ) ) {
			  bogusHeaderSector.setSectorID(
						track,
						head,
						tmpSecNum );
			  bogusHeaderSector.setBogusID( false );
			  autoRepaired = true;
			}
			tmpSecNum++;
		      }
		    }
		  }
		}
	      }
	    }
	  }
	}
      }

      // Diskattenobjekt anlegen
      rv = new TeleDisk(
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

      // ggf. Warnung
      if( autoRepaired ) {
	rv.setWarningText( "JKCEMU hat Sektoren repariert,"
			+ " die beim Erzeugen der\n"
			+ "Teledisk-Datei nicht korrekt gelesen"
			+ " werden konnten." );
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
    return "TeleDisk-Datei";
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
      props.setProperty( prefix + PROP_FILE, this.fileName );
      props.setProperty( prefix + PROP_READONLY, EmuUtil.VALUE_TRUE );
    }
  }


  @Override
  public boolean supportsDeletedSectors()
  {
    return true;
  }


	/* --- private Konstruktoren und Methoden --- */

  private TeleDisk(
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


  private static int readMandatoryWord( InputStream in ) throws IOException
  {
    int b0 = readMandatoryByte( in );
    int b1 = readMandatoryByte( in );
    return (b1 << 8) | b0;
  }


  private static void throwLengthMismatch() throws IOException
  {
    throw new IOException( "In der Datei passen einzelne L\u00E4ngenangaben"
				+ " nicht zusammen." );
  }


  private static void throwUnsupportedTeleDiskFmt( String msg )
							throws IOException
  {
    
    throw new IOException( "Die Datei enth\u00E4lt ein nicht"
			+ " unterst\u00FCtztes TeleDisk-Format:\n"
			+ msg );
  }


  private static void writeByte(
			OutputStream out,
			int          b,
			CRC16        crc ) throws IOException
  {
    crc.update( b );
    out.write( b );
  }
}
