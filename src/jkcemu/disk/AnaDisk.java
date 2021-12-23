/*
 * (c) 2009-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette, deren Daten in einer AnaDisk-Datei vorliegen
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileUtil;


public class AnaDisk extends AbstractFloppyDisk
{
  private String                                  fileName;
  private FileLock                                fileLock;
  private RandomAccessFile                        raf;
  private long                                    fileLen;
  private boolean                                 resource;
  private Map<Integer,java.util.List<SectorData>> side0;
  private Map<Integer,java.util.List<SectorData>> side1;


  public static String export(
			AbstractFloppyDisk disk,
			File               file ) throws IOException
  {
    StringBuilder msgBuf = null;
    OutputStream  out    = null;
    try {
      out = FileUtil.createOptionalGZipOutputStream( file );

      boolean dataDeleted = false;
      int     sides       = disk.getSides();
      int     cyls        = disk.getCylinders();
      for( int cyl = 0; cyl < cyls; cyl++ ) {
	for( int head = 0; head < sides; head++ ) {
	  int cylSectors = disk.getSectorsOfTrack( cyl, head );
	  for( int i = 0; i < cylSectors; i++ ) {
	    SectorData sector = disk.getSectorByIndex( cyl, head, i );
	    if( sector != null ) {
	      if( sector.checkError()
		  || sector.getDataDeleted()
		  || sector.hasBogusID() )
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
		    msgBuf.append( ',' );
		  }
		  msgBuf.append( " CRC-Fehler" );
		  appended = true;
		}
		if( sector.getDataDeleted() ) {
		  if( appended ) {
		    msgBuf.append( ',' );
		  }
		  msgBuf.append( " Daten als gel\u00F6scht markiert" );
		  appended    = true;
		  dataDeleted = true;
		}
		msgBuf.append( '\n' );
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
		sector.writeTo( out, len );
	      }
	    }
	  }
	}
      }
      out.close();
      out = null;

      if( msgBuf != null ) {
	msgBuf.append( "\nDie angezeigten Informationen k\u00F6nnen"
		+ " in einer AnaDisk-Datei nicht gespeichert werden\n"
		+ "und sind deshalb in der erzeugten Datei"
		+ " nicht mehr enthalten.\n" );
	if( dataDeleted ) {
	  msgBuf.append( "\nSektoren mit gel\u00F6schten Daten werden"
		+ " in AnaDisk-Dateien nicht unterst\u00FCtzt\n"
		+ "und sind deshalb als normale Sektoren enthalten.\n" );
	}
      }
    }
    finally {
      EmuUtil.closeSilently( out );
    }
    return msgBuf != null ? msgBuf.toString() : null;
  }


  public static AnaDisk newFile( Frame owner, File file ) throws IOException
  {
    AnaDisk           rv  = null;
    FileLock          fl  = null;
    RandomAccessFile  raf = null;
    try {
      raf = new RandomAccessFile( file, "rw" );
      fl  = FileUtil.lockFile( file, raf );
      raf.setLength( 0 );
      raf.seek( 0 );
      rv = new AnaDisk(
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
	FileUtil.releaseSilent( fl );
	EmuUtil.closeSilently( raf );
      }
    }
    return rv;
  }


  public static AnaDisk openFile( Frame owner, File file ) throws IOException
  {
    AnaDisk          rv  = null;
    FileLock         fl  = null;
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile( file, "rw" );
      fl  = FileUtil.lockFile( file, raf );
      rv  = createInstance( owner, null, raf, fl, file.getPath(), false );
    }
    finally {
      if( rv == null ) {
	FileUtil.releaseSilent( fl );
	EmuUtil.closeSilently( raf );
      }
    }
    return rv;
  }


  public static AnaDisk readFile( Frame owner, File file ) throws IOException
  {
    AnaDisk     rv = null;
    InputStream in = null;
    try {
      in = new FileInputStream( file );
      if( FileUtil.isGZipFile( file ) ) {
	in = new GZIPInputStream( in );
      }
      rv = createInstance( owner, in, null, null, file.getPath(), false );
    }
    finally {
      if( rv == null ) {
	EmuUtil.closeSilently( in );
      }
    }
    return rv;
  }


  public static AnaDisk readResourceStream(
				Frame       owner,
				InputStream in,
				String      resource ) throws IOException
  {
    return createInstance( owner, in, null, null, resource, true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public synchronized void closeSilently()
  {
    FileUtil.releaseSilent( this.fileLock );
    EmuUtil.closeSilently( this.raf );
  }


  @Override
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
	    this.side1 = new HashMap<>();
	  }
	  map = this.side1;
	} else {
	  if( this.side0 == null ) {
	    this.side0 = new HashMap<>();
	  }
	  map = this.side0;
	}
	java.util.List<SectorData> cylSects = map.get( physCyl );
	if( cylSects == null ) {
	  cylSects = new ArrayList<>( sectorIDs.length );
	  map.put( physCyl, cylSects );
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
	      if( sectorIDs.length > getSectorsPerTrack() ) {
		setSectorsPerTrack( sectorIDs.length );
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
	    fireShowError( "Anh\u00E4ngen von Sektoren fehlgeschlagen", ex );
	  }
	} else {
	  rv = super.formatTrack( physCyl, physHead, sectorIDs, dataBuf );
	}
      }
    }
    return rv;
  }


  @Override
  public String getFileFormatText()
  {
    return "AnaDisk-Datei";
  }


  @Override
  public synchronized SectorData getSectorByIndex(
					int physCyl,
					int physHead,
					int sectorIdx )
  {
    SectorData                 rv      = null;
    java.util.List<SectorData> sectors = getSectorsOfTrackInternal(
								physCyl,
								physHead );
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


  @Override
  public int getSectorsOfTrack( int physCyl, int physHead )
  {
    java.util.List<SectorData> sectors = getSectorsOfTrackInternal(
								physCyl,
								physHead );
    return sectors != null ? sectors.size() : 0;
  }


  @Override
  public boolean isReadOnly()
  {
    return this.raf == null;
  }


  @Override
  public void putSettingsTo( Properties props, String prefix )
  {
    if( (props != null) && (this.fileName != null) ) {
      if( this.resource ) {
	props.setProperty( prefix + PROP_RESOURCE, this.fileName );
      } else {
	props.setProperty( prefix + PROP_FILE, this.fileName );
      }
      props.setProperty(
		prefix + PROP_READONLY,
		Boolean.toString( isReadOnly() ) );
    }
  }


  @Override
  public boolean writeSector(
			int        physCyl,
			int        physHead,
			SectorData sector,
			byte[]     dataBuf,
			int        dataLen,
			boolean    dataDeleted )
  {
    boolean rv = false;
    if( (this.raf != null)
	&& (sector != null)
	&& (dataBuf != null) && !dataDeleted )
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
	    sector.setData( dataDeleted, dataBuf, dataLen );
	    rv = true;
	  }
	  catch( IOException ex ) {
	    fireShowWriteError( physCyl, physHead, sectorNum, ex );
	    sector.setError( true );
	  }
	}
      }
    }
    return rv;
  }


	/* --- private Konstruktoren und Methoden --- */

  private AnaDisk(
		Frame                                   owner,
		int                                     cyls,
		int                                     sides,
		int                                     sectorsPerTrack,
		int                                     sectorSize,
		String                                  fileName,
		boolean                                 resource,
		RandomAccessFile                        raf,
		FileLock                                fileLock,
		long                                    fileLen,
		Map<Integer,java.util.List<SectorData>> side0,
		Map<Integer,java.util.List<SectorData>> side1 )
  {
    super( owner, cyls, sides, sectorsPerTrack, sectorSize );
    this.fileName = fileName;
    this.resource = resource;
    this.raf      = raf;
    this.fileLock = fileLock;
    this.fileLen  = fileLen;
    this.side0    = side0;
    this.side1    = side1;
  }


  private static AnaDisk createInstance(
				Frame            owner,
				InputStream      in,
				RandomAccessFile raf,
				FileLock         fl,
				String           fileName,
				boolean          resource ) throws IOException
  {
    AnaDisk rv = null;
    if( in == null ) {
      in = FileUtil.createInputStream( raf );
    }

    Map<Integer,java.util.List<SectorData>> side0 = null;
    Map<Integer,java.util.List<SectorData>> side1 = null;

    long filePos         = 0;
    int  cyls            = 0;
    int  sectorsPerTrack = 0;
    int  sectorSize      = 0;
    for(;;) {
      long sectorFilePos = filePos;

      int physCyl = in.read();
      if( physCyl < 0 ) {
	break;
      }
      int physHead     = in.read();
      int sectCyl      = in.read();
      int sectHead     = in.read();
      int sectNum      = in.read();
      int sectSizeCode = in.read();
      int l1           = in.read();
      int l2           = in.read();
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
      int sectLen = 128;
      if( sectSizeCode > 0 ) {
	sectLen <<= sectSizeCode;
      }
      if( sectorSize == 0 ) {
	sectorSize = sectLen;
      }
      byte[] sectBuf = null;
      if( nRemain > 0 ) {
	sectBuf = new byte[ sectLen ];
	int pos = 0;
	while( nRemain > 0 ) {
	  int b = in.read();
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
	  side0 = new HashMap<>();
	}
	map = side0;
      } else if( physHead == 1 ) {
	if( side1 == null ) {
	  side1 = new HashMap<>();
	}
	map = side1;
      }
      if( map != null ) {
	java.util.List<SectorData> sectors = map.get( physCyl );
	if( sectors == null ) {
	  if( (physCyl > 0) && (sectorsPerTrack > 0) ) {
	    sectors = new ArrayList<>( sectorsPerTrack );
	  } else {
	    sectors = new ArrayList<>();
	  }
	  map.put( physCyl, sectors );
	}
	// doppelte Sektoren herausfiltern
	boolean found = false;
	for( SectorData tmpSector : sectors ) {
	  if( tmpSector.equalsSectorID(
				sectCyl,
				sectHead,
				sectNum,
				sectSizeCode ) )
	  {
	    if( tmpSector.equalsData( sectBuf, 0, sectLen ) ) {
	      found = true;
	      break;
	    }
	  }
	}
	if( !found ) {
	  SectorData sector = new SectorData(
				sectors.size(),
				sectCyl,
				sectHead,
				sectNum,
				sectSizeCode,
				sectBuf,
				0,
				sectBuf != null ? sectBuf.length : 0 );
	  sector.setFilePos( sectorFilePos );
	  sector.setFilePortionLen( (int) (filePos - sectorFilePos) );
	  sectors.add( sector );
	  sectorsPerTrack = Math.max( sectorsPerTrack, sectors.size() );
	}
      }
    }
    if( (side0 != null) || (side1 != null) ) {
      int sides = 1;
      if( side1 != null ) {
	sides++;
      }
      rv = new AnaDisk(
		owner,
		cyls,
		sides,
		sectorsPerTrack,
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
      throw new IOException( "Datei ist keine AnaDisk-Datei" );
    }
    return rv;
  }


  private java.util.List<SectorData> getSectorsOfTrackInternal(
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
}
