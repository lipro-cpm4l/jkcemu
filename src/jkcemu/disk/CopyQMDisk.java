/*
 * (c) 2010-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette, deren Daten in einer CopyQM-Datei vorliegen
 *
 * Die Informationen ueber das CopyQM-Format sowie die hier verwendete
 * CRC-Tabelle entstammen aus dem Projekt LIBDSK, (c) 2001-2005 John Elliott
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import jkcemu.base.*;


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

  private String fileName;
  private String remark;
  private byte[] diskBytes;
  private int    sectorSizeCode;


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
      byte[] head = new byte[ 133 ];
      if( EmuUtil.read( in, head ) != head.length ) {
	throwNoCopyQMFile();
      }
      if( (head[ 0 ] != 'C') || (head[ 1 ] != 'Q') || (head[ 2 ] != 0x14) ) {
	throwNoCopyQMFile();
      }
      int sectorSize     = (head[ 4 ] << 8) | head[ 3 ];
      int sectorSizeCode = SectorData.getSizeCode( sectorSize );
      if( (sectorSize < 1) || (sectorSizeCode < 0) ) {
	throwUnsupportedCopyQMFmt(
		String.format(
			"%d Byte Sektorgr\u00F6\u00DFe nicht unterst\u00FCtzt",
			sectorSize ) );
      }
      int sectorsPerCyl = (head[ 0x11 ] << 8) | head[ 0x10 ];
      if( sectorsPerCyl < 1 ) {
	throwUnsupportedCopyQMFmt(
		String.format( "Sektoren pro Spur", sectorsPerCyl ) );
      }
      int sides = (int) head[ 0x12 ] & 0xFF;
      if( (sides < 1) || (sides > 2) ) {
	throwUnsupportedCopyQMFmt(
		String.format( "%d Seiten nicht unterst\u00FCtzt", sides ) );
      }
      int usedCyls = (int) head[ 0x5A ] & 0xFF;
      int cyls     = (int) head[ 0x5B ] & 0xFF;
      if( cyls < usedCyls ) {
	cyls = usedCyls;
      }
      if( cyls < 1 ) {
	throwUnsupportedCopyQMFmt(
		String.format( "%d Spuren nicht unterst\u00FCtzt", sides ) );
      }
      if( head[ 0x71 ] != 0 ) {
	throwUnsupportedCopyQMFmt( "Mysteri\u00F6se Sektornummerierung" );
      }

      // Kommentar lesen
      String remark    = null;
      int    remarkLen = (head[ 0x70 ] << 8) | head[ 0x6F];
      if( remarkLen < 0 ) {
	throwUnsupportedCopyQMFmt(
		String.format(
			"Mysteri\u00F6se Kommentarl\u00E4nge: %d",
			remarkLen ) );
      }
      if( remarkLen > 0 ) {
	StringBuilder buf = new StringBuilder( remarkLen );
	while( remarkLen > 0 ) {
	  int b = in.read();
	  if( b < 0 ) {
	    break;
	  }
	  if( b < 0x20 ) {
	    b = 0x20;
	  } else if( b > 0x7E ) {
	    b = '?';
	  }
	  buf.append( (char) b );
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
			diskBytes,
			sectorSizeCode );

      // CRC ueber die entpackten Daten berechnen
      long orgCRC = 0L;
      long newCRC = 0L;
      for( int i = 0x5F; i >= 0x5C; --i ) {
	orgCRC = (orgCRC << 8) | ((int) head[ i ] & 0xFF);
      }
      if( orgCRC != 0 ) {
	for( int i = 0; i < dstPos; i++ ) {
	  int b = (int) diskBytes[ i ] & 0x7F;
	  newCRC = crcTable[ ((int) ((long) b ^ newCRC)) & 0x3F ]
							^ (newCRC >> 8);
	}
      }
      if( orgCRC != newCRC ) {
	rv.setWarningText( 
		"Die CopyQM-Datei scheint defekt zu sein (CRC-Fehler)!" );
      }
    }
    finally {
      EmuUtil.doClose( in );
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

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
    SectorData rv         = null;
    int        sectorSize = getSectorSize();
    if( (physCyl >= 0) && (sectorIdx >= 0) ) {
      int sides         = getSides();
      int cyls          = getCylinders();
      int sectorsPerCyl = getSectorsPerCylinder();
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
			sectorIdx + 1,
			this.sectorSizeCode,
			false,
			false,
			this.diskBytes,
                        nSkipSectors * sectorSize,
			sectorSize );
      }
    }
    return rv;
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
    SectorData rv = getSectorByIndex( physCyl, physHead, sectorNum - 1 );
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
  public void putSettingsTo( Properties props, String prefix )
  {
    if( (props != null) && (this.fileName != null) ) {
      props.setProperty( prefix + "file", this.fileName );
      props.setProperty( prefix + "readonly", "true" );
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private CopyQMDisk(
		Frame  owner,
		int    sides,
		int    cyls,
		int    sectorsPerCyl,
		int    sectorSize,
		String fileName,
		String remark,
		byte[] diskBytes,
		int    sectorSizeCode )
  {
    super( owner, sides, cyls, sectorsPerCyl, sectorSize );
    this.fileName       = fileName;
    this.remark         = remark;
    this.diskBytes      = diskBytes;
    this.sectorSizeCode = sectorSizeCode;
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
