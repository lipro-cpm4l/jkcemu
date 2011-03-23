/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette, deren Daten in einer Teledisk-Datei vorliegen
 *
 * Der hier implementierte Algorithmus basiert auf den Informationen von:
 * http://www.fpns.net/willy/wteledsk.htm
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;


public class TelediskFloppyDisk extends AbstractFloppyDisk
{
  private String                                  fileName;
  private String                                  remark;
  private Map<Integer,java.util.List<SectorData>> side0Sectors;
  private Map<Integer,java.util.List<SectorData>> side1Sectors;


  public static TelediskFloppyDisk readFile(
					Frame owner,
					File  file ) throws IOException
  {
    TelediskFloppyDisk rv    = null;
    InputStream        in    = null;
    Exception          errEx = null;
    try {
      in = new FileInputStream( file );
      if( EmuUtil.isGZipFile( file ) ) {
	in = new GZIPInputStream( in );
      }

      // Kopfblock lesen
      int head0 = in.read();
      int head1 = in.read();
      int head2 = in.read();
      if( (head0 == 't') && (head1 == 'd') ) {
	throwUnsupportedTelediskFmt(
		"Advanced Compression nicht unterst\u00FCtzt" );
      }
      if( (head0 != 'T') || (head1 != 'D') || (head2 != 0) ) {
	throw new IOException( "Datei ist keine Teledisk-Datei." );
      }
      in.read();
      int fmtVersion = readByte( in );
      if( fmtVersion != 0x15 ) {			// Version
	throwUnsupportedTelediskFmt(
		String.format(
			"Formatversion %02X nicht unterst\u00FCtzt",
			fmtVersion ) );
      }
      in.read();
      in.read();
      boolean hasRemark = ((readByte( in ) & 0x80) != 0);
      in.read();
      int sides = in.read();
      if( (sides < 1) || (sides > 2) ) {
	throwUnsupportedTelediskFmt(
		String.format( "%d Seiten nicht unterst\u00FCtzt", sides ) );
      }
      in.read();				// CRC
      in.read();				// CRC

      // Kommantarblock lesen
      String remark   = null;
      String dateText = null;
      if( hasRemark ) {
	in.read();				// CRC
	in.read();				// CRC
	int    len      = readWord( in );
	int    year     = readByte( in ) + 1900;
	int    month    = readByte( in );
	int    day      = readByte( in );
	int    hour     = readByte( in );
	int    minute   = readByte( in );
	int    second   = readByte( in );
	if( len < 0 ) {
	  len = 0;
	}
	StringBuilder buf = new StringBuilder( len + 32 );
	if( len > 0 ) {
	  while( len > 0 ) {
	    int ch = readByte( in );
	    if( (ch <= 0x20) || (ch >= 0x7F) ) {
	      ch = 0x20;
	    }
	    buf.append( (char) ch );
	    --len;
	  }
	  buf.append( (char) '\n' );
	}
	buf.append( String.format(
			"Erstellt am %02d.%02d.%04d %02d:%02d:%02d",
			day, month + 1, year, hour, minute, second ) );
	remark = buf.toString();
      }

      // Spuren einlesen
      Map<Integer,java.util.List<SectorData>> side0Sectors = null;
      Map<Integer,java.util.List<SectorData>> side1Sectors = null;

      int cyls          = 0;
      int sectorsPerCyl = 0;
      int sectorSize    = 0;
      for(;;) {
	int nSec  = in.read();
	int track = in.read();
	int head  = in.read();
	in.read();

	if( (nSec == 0xFF) || (nSec == -1) || (track == -1) || (head == -1) ) {
	  break;
	}
	if( (head < 0) || (head > 1) ) {
	  throwUnsupportedTelediskFmt(
		  String.format( "Kopf %d nicht unterst\u00FCtzt", head ) );
	}
	if( track >= cyls ) {
	  cyls = track + 1;
	}
	if( sectorsPerCyl == 0 ) {
	  sectorsPerCyl = nSec;
	}

	// Sektoren lesen
	for( int i = 0; i < nSec; i++ ) {
	  int    secTrack    = readByte( in );
	  int    secHead     = readByte( in );
	  int    secNum      = readByte( in );
	  int    secSizeCode = readByte( in );
	  int    secCtrl     = readByte( in );
	  in.read();				// CRC

	  byte[] secBuf = null;
	  if( (secSizeCode >= 0) && (secSizeCode <= 5) ) {
	    int secSize = 128;
	    if( secSizeCode > 0 ) {
	      secSize = 128 << secSizeCode;
	    }
	    secBuf = new byte[ secSize ];
	    Arrays.fill( secBuf, (byte) 0 );
	    if( sectorSize == 0 ) {
	      sectorSize = secSize;
	    }
	  } else {
	    throwUnsupportedTelediskFmt(
		String.format(
			"Code=%02X f\u00Fcr Sektorgr\u00F6\u00DFe"
				+ " nicht unterst\u00FCtzt",
			secSizeCode ) );
	  }

	  boolean crcError = ((secCtrl & 0x02) != 0);
	  boolean deleted  = ((secCtrl & 0x04) != 0);
	  if( ((secCtrl & 0x30) == 0) && (secBuf != null) ) {
	    int len = readWord( in );
	    if( len > 0 ) {
	      int secEncoding = readByte( in );
	      --len;

	      int pos = 0;
	      switch( secEncoding ) {
		case 0:
		  while( (len > 0) && (pos < secBuf.length) ) {
		    secBuf[ pos++ ] = (byte) readByte( in );
		    --len;
		  }
		  break;

		case 1:
		  if( len >= 4 ) {
		    int n  = readWord( in );
		    int b0 = readByte( in );
		    int b1 = readByte( in );
		    len -= 4;
		    while( (n > 0) && (pos < secBuf.length) ) {
		      secBuf[ pos++ ] = (byte) b0;
		      secBuf[ pos++ ] = (byte) b1;
		      --n;
		    }
		  }
		  break;

		case 2:
		  while( len >= 2 ) {
		    int t = readByte( in );
		    int n = readByte( in );
		    len -= 2;
		    switch( t ) {
		      case 0:
			while( (len > 0) && (n > 0)
			       && (pos < secBuf.length) )
			{
			  secBuf[ pos++ ] = (byte) readByte( in );
			  --n;
			  --len;
			}
			if( n > 0 ) {
			  throwLengthMismatch();
			}
			break;

		      case 1:
			if( len >= 2 ) {
			  int b0 = readByte( in );
			  int b1 = readByte( in );
			  len -= 2;
			  while( (n > 0) && (pos < secBuf.length) ) {
			    secBuf[ pos++ ] = (byte) b0;
			    secBuf[ pos++ ] = (byte) b1;
			    --n;
			  }
			}
			break;

		      default:
			throwUnsupportedTelediskFmt(
				String.format(
					"Sektorunterkodierung %02X"
						+ " nicht unterst\u00FCtzt",
				t ) );
		    }
		  }
		  break;

		default:
		  throwUnsupportedTelediskFmt(
			String.format(
				"Sektorkodierung %02Xh nicht unterst\u00FCtzt",
				secEncoding ) );
	      }
	      if( len > 0 ) {
		throwLengthMismatch();
	      }
	    }
	  }
	  Map<Integer,java.util.List<SectorData>> map = null;
	  if( head == 0 ) {
	    if( side0Sectors == null ) {
	      side0Sectors = new HashMap<Integer,java.util.List<SectorData>>();
	    }
	    map = side0Sectors;
	  } else if( head == 1 ) {
	    if( side1Sectors == null ) {
	      side1Sectors = new HashMap<Integer,java.util.List<SectorData>>();
	    }
	    map = side1Sectors;
	  }
	  if( map != null ) {
	    Integer keyObj                     = new Integer( track );
	    java.util.List<SectorData> sectors = map.get( keyObj );
	    if( sectors == null ) {
	      sectors = new ArrayList<SectorData>( nSec > 0 ? nSec : 1 );
	      map.put( keyObj, sectors );
	    }
	    sectors.add(
		new SectorData(
			sectors.size(),
			secTrack,
			secHead,
			secNum,
			secSizeCode,
			crcError,
			deleted,
			secBuf,
			0,
			secBuf != null ? secBuf.length : 0 ) );
	  }
	}
      }
      rv = new TelediskFloppyDisk(
				owner,
				sides,
				cyls,
				sectorsPerCyl,
				sectorSize,
				file.getPath(),
				remark,
				side0Sectors,
				side1Sectors );
    }
    finally {
      EmuUtil.doClose( in );
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

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


	/* --- private Konstruktoren und Methoden --- */

  private TelediskFloppyDisk(
		Frame                                   owner,
		int                                     sides,
		int                                     cyls,
		int                                     sectorsPerCyl,
		int                                     sectorSize,
		String                                  fileName,
		String                                  remark,
		Map<Integer,java.util.List<SectorData>> side0Sectors,
		Map<Integer,java.util.List<SectorData>> side1Sectors )
  {
    super( owner, sides, cyls, sectorsPerCyl, sectorSize );
    this.fileName     = fileName;
    this.remark       = remark;
    this.side0Sectors = side0Sectors;
    this.side1Sectors = side1Sectors;
  }


  private java.util.List<SectorData> getSectorList(
						int physCyl,
						int physHead )
  {
    java.util.List<SectorData>              rv  = null;
    Map<Integer,java.util.List<SectorData>> map = ((physHead & 0x01) != 0 ?
							side1Sectors
							: side0Sectors);
    if( map != null ) {
      rv = map.get( new Integer( physCyl ) );
    }
    return rv;
  }


  private static int readByte( InputStream in ) throws IOException
  {
    int b = in.read();
    if( b == -1 ) {
      throw new IOException( "Unerwartetes Dateiende" );
    }
    return b;
  }


  private static int readWord( InputStream in ) throws IOException
  {
    int b0 = readByte( in );
    int b1 = readByte( in );
    return (b1 << 8) | b0;
  }


  private static void throwLengthMismatch() throws IOException
  {
    throw new IOException( "In der Datei passen einzelne L\u00E4ngenangaben"
				+ " nicht zusammen." );
  }


  private static void throwUnsupportedTelediskFmt( String msg )
							throws IOException
  {
    
    throw new IOException( "Die Datei enth\u00E4lt ein nicht"
			+ " unterst\u00FCtztes Teledisk-Format:\n"
			+ msg );
  }
}
