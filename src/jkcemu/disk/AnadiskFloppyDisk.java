/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette, deren Daten in einer Anadisk-Datei vorliegen
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.*;
import java.lang.*;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;


public class AnadiskFloppyDisk extends AbstractFloppyDisk
{
  private String                                  fileName;
  private FileLock                                fileLock;
  private RandomAccessFile                        raf;
  private long                                    fileLen;
  private boolean                                 resource;
  private Map<Integer,java.util.List<SectorData>> side0;
  private Map<Integer,java.util.List<SectorData>> side1;


  public static void export(
			AbstractFloppyDisk disk,
			File               file ) throws IOException
  {
    OutputStream out = null;
    try {
      out = new FileOutputStream( file );

      int sides = disk.getSides();
      int cyls  = disk.getCylinders();
      for( int cyl = 0; cyl < cyls; cyl++ ) {
	for( int head = 0; head < sides; head++ ) {
	  int cylSectors = disk.getSectorsOfCylinder( cyl, head );
	  for( int i = 0; i < cylSectors; i++ ) {
	    SectorData sector = disk.getSectorByIndex( cyl, head, i );
	    if( sector != null ) {
	      if( sector.isDeleted() ) {
	        throw new IOException(
		    String.format(
			"Seite %d, Spur %d: Sektor %d ist als gel\u00F6scht"
				+ " markiert\n"
				+ "Gel\u00F6schte Sektoren werden"
				+ " in Anadisk-Dateien nicht"
				+ " unterst\u00FCtzt.",
			head + 1,
			cyl,
			sector.getSectorNum() ) );
	      }
	      out.write( cyl );
	      out.write( head );
	      out.write( sector.getCylinder() );
	      out.write( sector.getHead() );
	      out.write( sector.getSectorNum() );
	      out.write( sector.getSizeCode() );
	      int len = sector.getDataLength();
	      if( len < 0 ) {
		len = 0;
	      }
	      out.write( len & 0xFF );
	      out.write( len >> 8 );
	      if( len > 0 ) {
		sector.writeTo( out, cyl, head, len );
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


  public static AnadiskFloppyDisk newFile(
					Frame owner,
					File  file ) throws IOException
  {
    AnadiskFloppyDisk rv  = null;
    FileLock          fl  = null;
    RandomAccessFile  raf = null;
    try {
      raf = new RandomAccessFile( file, "rw" );
      fl  = EmuUtil.lockFile( file, raf );
      raf.setLength( 0 );
      rv = new AnadiskFloppyDisk(
				owner,
				0,
				0,
				0,
				0,
				file.getPath(),
				false,
				raf,
				fl,
				0,
				null,
				null );
    }
    finally {
      if( rv == null ) {
	EmuUtil.doRelease( fl );
	EmuUtil.doClose( raf );
      }
    }
    return rv;
  }


  public static AnadiskFloppyDisk openFile(
					Frame owner,
					File  file ) throws IOException
  {
    AnadiskFloppyDisk rv  = null;
    FileLock          fl  = null;
    RandomAccessFile  raf = null;
    try {
      raf = new RandomAccessFile( file, "rw" );
      fl  = EmuUtil.lockFile( file, raf );
      rv  = createInstance( owner, null, raf, fl, file.getPath(), false );
    }
    finally {
      if( rv == null ) {
	EmuUtil.doRelease( fl );
	EmuUtil.doClose( raf );
      }
    }
    return rv;
  }


  public static AnadiskFloppyDisk readFile(
					Frame owner,
					File  file ) throws IOException
  {
    AnadiskFloppyDisk rv = null;
    InputStream       in = null;
    try {
      in = new FileInputStream( file );
      if( EmuUtil.isGZipFile( file ) ) {
	in = new GZIPInputStream( in );
      }
      rv = createInstance( owner, in, null, null, file.getPath(), false );
    }
    finally {
      if( rv == null ) {
	EmuUtil.doClose( in );
      }
    }
    return rv;
  }


  public static AnadiskFloppyDisk readResourceStream(
				Frame       owner,
				InputStream in,
				String      resource ) throws IOException
  {
    return createInstance( owner, in, null, null, resource, true );
  }


	/* --- ueberschriebene Methoden --- */

  public synchronized void doClose()
  {
    EmuUtil.doRelease( this.fileLock );
    EmuUtil.doClose( this.raf );
  }


  public boolean formatTrack(
			int        physCyl,
			int        physHead,
			SectorID[] sectorIDs,
			byte[]     dataBuf )
  {
    boolean rv = false;
    if((this.raf != null) && (sectorIDs != null) && (dataBuf != null) ) {
      if( sectorIDs.length > 0 ) {
	/*
	 * Wenn auf der Spur und Seite noch keine Sektoren sind,
	 * werden sie an die Datei angehaengt.
	 * Anderenfalls werden die entsprechenden Sektoren
	 * einfach ueberschrieben.
	 */
	Map<Integer,java.util.List<SectorData>> map = null;
	if( (physHead & 0x01) != 0 ) {
	  if( this.side1 == null ) {
	    this.side1 = new HashMap<Integer,java.util.List<SectorData>>();
	  }
	  map = this.side1;
	} else {
	  if( this.side0 == null ) {
	    this.side0 = new HashMap<Integer,java.util.List<SectorData>>();
	  }
	  map = this.side0;
	}
	Integer                    cylObj   = new Integer( physCyl );
	java.util.List<SectorData> cylSects = map.get( cylObj );
	if( cylSects == null ) {
	  cylSects = new ArrayList<SectorData>( sectorIDs.length );
	  map.put( cylObj, cylSects );
	}
	if( cylSects.isEmpty() ) {
	  try {
	    this.raf.seek( this.fileLen );
	    for( int i = 0; i < sectorIDs.length; i++ ) {
	      SectorID sectorID = sectorIDs[ i ];
	      this.raf.write( physCyl );
	      this.raf.write( physHead );
	      this.raf.write( sectorID.getCylinder() );
	      this.raf.write( sectorID.getHead() );
	      this.raf.write( sectorID.getSectorNum() );
	      this.raf.write( sectorID.getSizeCode() );
	      this.raf.write( dataBuf.length & 0xFF );
	      this.raf.write( dataBuf.length >> 8 );
	      this.raf.write( dataBuf );

	      SectorData sector = new SectorData(
					i,
					sectorID.getCylinder(),
					sectorID.getHead(),
					sectorID.getSectorNum(),
					sectorID.getSizeCode(),
					false,
					false,
					dataBuf,
					0,
					dataBuf.length );
	      sector.setDisk( this );
	      sector.setFilePortionLen( dataBuf.length + 8 );
	      sector.setFilePos( this.fileLen );
	      cylSects.add( sector );

	      int sides = ((physHead & 0x01) != 0 ? 2 : 1);
	      if( sides > getSides() ) {
		setSides( sides );
	      }
	      if( physCyl >= getCylinders() ) {
		setCylinders( physCyl + 1 );
	      }
	      if( sectorIDs.length > getSectorsPerCylinder() ) {
		setSectorsPerCylinder( sectorIDs.length );
	      }
	      if( getSectorSize() == 0 ) {
		setSectorSize( dataBuf.length );
	      }
	      this.fileLen += 8;
	      this.fileLen += dataBuf.length;
	    }
	    rv = true;
	  }
	  catch( IOException ex ) {
	    rv = false;
	    showError( "Anh\u00E4ngen von Sektoren fehlgeschlagen", ex );
	  }
	} else {
	  rv = super.formatTrack( physCyl, physHead, sectorIDs, dataBuf );
	}
      }
    }
    return rv;
  }


  public synchronized SectorData getSectorByIndex(
					int physCyl,
					int physHead,
					int sectorIdx )
  {
    SectorData                 rv      = null;
    java.util.List<SectorData> sectors = getSectorsOfCyl( physCyl, physHead );
    if( sectors != null ) {
      if( (sectorIdx >= 0) && (sectorIdx < sectors.size()) ) {
	rv = sectors.get( sectorIdx );
	if( rv != null ) {
	  rv.setDisk( this );		// da es beim Einlesen nicht getan wird
	}
      }
    }
    return rv;
  }


  public int getSectorsOfCylinder( int physCyl, int physHead )
  {
    java.util.List<SectorData> sectors = getSectorsOfCyl( physCyl, physHead );
    return sectors != null ? sectors.size() : 0;
  }


  public boolean isReadOnly()
  {
    return this.raf == null;
  }


  public void putSettingsTo( Properties props, String prefix )
  {
    if( (props != null) && (this.fileName != null) ) {
      if( this.resource ) {
	props.setProperty( prefix + "resource", this.fileName );
      } else {
	props.setProperty( prefix + "file", this.fileName );
      }
      props.setProperty(
		prefix + "readonly",
		Boolean.toString( isReadOnly() ) );
    }
  }


  public boolean writeSector(
			int        physCyl,
			int        physHead,
			SectorData sector,
			byte[]     dataBuf,
			int        dataLen,
			boolean    deleted )
  {
    boolean rv = false;
    if( (this.raf != null)
	&& (sector != null)
	&& (dataBuf != null) && !deleted )
    {
      if( sector.getDisk() == this ) {
	int  sectorNum = sector.getSectorNum();
	long filePos   = sector.getFilePos();
	if( filePos >= 0 ) {
	  try {
	    if( (dataLen + 8) < sector.getFilePortionLen() ) {
	      throwSectorSpaceTooSmall(
				physCyl,
				physHead,
				sector.getSectorNum() );
	    }
	    this.raf.seek( filePos + 8 );	// hinter Kopf positionieren
	    this.raf.write( dataBuf, 0, dataLen );
	    sector.setData( deleted, dataBuf, dataLen );
	    rv = true;
	  }
	  catch( IOException ex ) {
	    showWriteError( physCyl, physHead, sectorNum, ex );
	    sector.setError( true );
	  }
	}
      }
    }
    return rv;
  }


	/* --- private Konstruktoren und Methoden --- */

  private AnadiskFloppyDisk(
		Frame                                   owner,
		int                                     sides,
		int                                     cyls,
		int                                     sectorsPerCyl,
		int                                     sectorSize,
		String                                  fileName,
		boolean                                 resource,
		RandomAccessFile                        raf,
		FileLock                                fileLock,
		long                                    fileLen,
		Map<Integer,java.util.List<SectorData>> side0,
		Map<Integer,java.util.List<SectorData>> side1 )
  {
    super( owner, sides, cyls, sectorsPerCyl, sectorSize );
    this.fileName = fileName;
    this.resource = resource;
    this.raf      = raf;
    this.fileLock = fileLock;
    this.fileLen  = fileLen;
    this.side0    = side0;
    this.side1    = side1;
  }


  private static AnadiskFloppyDisk createInstance(
				Frame            owner,
				InputStream      in,
				RandomAccessFile raf,
				FileLock         fl,
				String           fileName,
				boolean          resource ) throws IOException
  {
    AnadiskFloppyDisk rv = null;

    Map<Integer,java.util.List<SectorData>> side0 = null;
    Map<Integer,java.util.List<SectorData>> side1 = null;

    long filePos       = 0;
    int  cyls          = 0;
    int  sectorsPerCyl = 0;
    int  sectorSize    = 0;
    for(;;) {
      long sectorFilePos = filePos;

      int physCyl = read( in, raf );
      if( physCyl < 0 ) {
	break;
      }
      int physHead     = read( in, raf );
      int sectCyl      = read( in, raf );
      int sectHead     = read( in, raf );
      int sectNum      = read( in, raf );
      int sectSizeCode = read( in, raf );
      int l1           = read( in, raf );
      int l2           = read( in, raf );
      filePos += 8;

      int nRemain = ((l2 << 8) & 0xFF00) | (l1 & 0xFF);
      if( (physHead < 0) || (physHead > 1)
	  || (sectCyl < 0) || (sectHead < 0) || (sectHead > 1)
	  || (sectNum < 1) || (sectSizeCode < 0) || (sectSizeCode > 3)
	  || (l1 < 0) || (l2 < 0) || (nRemain < 0) )
      {
	break;
      }
      if( physCyl >= cyls ) {
	cyls = physCyl + 1;
      }
      if( sectNum > sectorsPerCyl ) {
	sectorsPerCyl = sectNum;
      }
      int len = 128;
      if( sectSizeCode > 0 ) {
	len <<= sectSizeCode;
      }
      if( sectorSize == 0 ) {
	sectorSize = len;
      }
      byte[] sectBuf = null;
      if( nRemain > 0 ) {
	sectBuf = new byte[ len ];
	int pos = 0;
	while( nRemain > 0 ) {
	  int b = read( in, raf );
	  filePos++;
	  if( (b != -1) && (pos < sectBuf.length) ) {
	    sectBuf[ pos++ ] = (byte) b;
	  }
	  --nRemain;
	}
	while( pos < sectBuf.length ) {
	  sectBuf[ pos++ ] = (byte) 0;
	}
      }
      Map<Integer,java.util.List<SectorData>> map = null;
      if( physHead == 0 ) {
	if( side0 == null ) {
	  side0 = new HashMap<Integer,java.util.List<SectorData>>();
	}
	map = side0;
      } else if( physHead == 1 ) {
	if( side1 == null ) {
	  side1 = new HashMap<Integer,java.util.List<SectorData>>();
	}
	map = side1;
      }
      if( map != null ) {
	Integer keyObj                     = new Integer( physCyl );
	java.util.List<SectorData> sectors = map.get( keyObj );
	if( sectors == null ) {
	  if( (physCyl > 0) && (sectorsPerCyl > 0) ) {
	    sectors = new ArrayList<SectorData>( sectorsPerCyl );
	  } else {
	    sectors = new ArrayList<SectorData>();
	  }
	  map.put( keyObj, sectors );
	}
	SectorData sector = new SectorData(
					sectors.size(),
					sectCyl,
					sectHead,
					sectNum,
					sectSizeCode,
					false,
					false,
					sectBuf,
					0,
					sectBuf != null ? sectBuf.length : 0 );
	sector.setFilePos( sectorFilePos );
	sector.setFilePortionLen( (int) (filePos - sectorFilePos) );
	sectors.add( sector );
      }
    }
    if( (side0 != null) || (side1 != null) ) {
      int sides = 1;
      if( side1 != null ) {
	sides++;
      }
      rv = new AnadiskFloppyDisk(
				owner,
				sides,
				cyls,
				sectorsPerCyl,
				sectorSize,
				fileName,
				resource,
				raf,
				fl,
				filePos,
				side0,
				side1 );
    }
    if( rv == null ) {
      throw new IOException( "Datei ist keine Anadisk-Datei" );
    }
    return rv;
  }


  private java.util.List<SectorData> getSectorsOfCyl(
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


  private static int read(
			InputStream      in,
			RandomAccessFile raf ) throws IOException
  {
    int rv = -1;
    if( in != null ) {
      rv = in.read();
    } else if( raf != null ) {
      rv = raf.read();
    }
    return rv;
  }


  private static void throwSectorSpaceTooSmall(
				int cyl,
				int head,
				int sectorNum ) throws IOException
  {
    throw new IOException(
	String.format(
		"Seite %d, Spur %d, Sektor %d: Datenfeld zu klein,\n"
			+ "Der Sektor kann nicht geschrieben werden,\n"
			+ "weil das Datenfeld des Sektors kleiner ist,\n"
			+ "als es laut Diskettenformat sein m\u00FCsste.\n"
			+ "Sie k\u00F6nnen im JKCEMU-Datei-Browser"
			+ " die Anadisk-Datei\n"
			+ "in eine einfache Abbilddatei exportieren\n"
			+ "und bei Bedarf diese dann  wieder in eine"
			+ " Anadisk-Datei.\n"
			+ "Dabei werden zu kleine Datenfelder entsprechend"
			+ " vergr\u00F6\u00DFert.",
		head - 1,
		cyl,
		sectorNum ) );
  }
}

