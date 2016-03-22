/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette basierend auf einer strukturlosen Abbilddatei
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.*;
import java.lang.*;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.zip.GZIPInputStream;
import jkcemu.base.*;


public class PlainDisk extends AbstractFloppyDisk
{
  private String                      fileName;
  private FileLock                    fileLock;
  private DeviceIO.RandomAccessDevice rad;
  private RandomAccessFile            raf;
  private byte[]                      diskBytes;
  private boolean                     readOnly;
  private boolean                     appendable;
  private int                         sectorSizeCode;


  public static PlainDisk createForDrive(
				Frame                       owner,
				String                      driveFileName,
				DeviceIO.RandomAccessDevice rad,
				boolean                     readOnly,
				FloppyDiskFormat            fmt )
  {
    return new PlainDisk(
			owner,
			fmt.getSides(),
			fmt.getCylinders(),
			fmt.getSectorsPerCylinder(),
			fmt.getSectorSize(),
			0,			// kein Interleave
			driveFileName,
			rad,
			null,
			null,
			null,
			readOnly,
			false );
  }


  public static PlainDisk createForByteArray(
			Frame            owner,
			String           fileName,
			byte[]           fileBytes,
			FloppyDiskFormat fmt,
			int              interleave ) throws IOException
  {
    PlainDisk rv = null;
    if( fileBytes != null ) {
      rv = new PlainDisk(
			owner,
			fmt.getSides(),
			fmt.getCylinders(),
			fmt.getSectorsPerCylinder(),
			fmt.getSectorSize(),
			interleave,
			fileName,
			null,
			null,
			null,
			fileBytes,
			true,
			false );
    }
    return rv;
  }


  public static PlainDisk createForByteArray(
			Frame            owner,
			String           fileName,
			byte[]           fileBytes,
			FloppyDiskFormat fmt ) throws IOException
  {
    return createForByteArray( owner, fileName, fileBytes, fmt, 0 );
  }


  public static PlainDisk createForFile(
				Frame            owner,
				String           driveFileName,
				RandomAccessFile raf,
				boolean          readOnly,
				FloppyDiskFormat fmt )
  {
    return new PlainDisk(
			owner,
			fmt.getSides(),
			fmt.getCylinders(),
			fmt.getSectorsPerCylinder(),
			fmt.getSectorSize(),
			0,			// kein Interleave
			driveFileName,
			null,
			raf,
			null,
			null,
			readOnly,
			false );
  }


  public static String export(
			AbstractFloppyDisk disk,
			File               file ) throws IOException
  {
    StringBuilder msgBuf = null;
    OutputStream  out    = null;
    try {
      out = new FileOutputStream( file );

      boolean hasDeleted    = false;
      int     sides         = disk.getSides();
      int     cyls          = disk.getCylinders();
      int     sectorsPerCyl = disk.getSectorsPerCylinder();
      int     sectorSize    = disk.getSectorSize();
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
						i + 1,
						-1 );
	    if( sector == null ) {
	      throw new IOException(
		String.format(
			"Seite %d, Spur %d: Sektor %d nicht gefunden",
			head + 1,
			cyl,
			i + 1  ) );
	    }
	    if( sector.isDeleted() ) {
	      hasDeleted = true;
	      if( msgBuf == null ) {
		msgBuf = new StringBuilder( 1024 );
	      }
	      msgBuf.append(
			String.format(
				"Seite %d, Spur %d: Sektor %d ist als"
					+ " gel\u00F6scht markiert\n",
				head + 1,
				cyl,
				sector.getSectorNum() ) );
	    }
	    if( sector.getDataLength() > sectorSize ) {
	      throw new IOException(
		String.format(
			"Seite %d, Spur %d: Sektor %d ist zu gro\u00DF.",
			head + 1,
			cyl,
			sector.getSectorNum() ) );
	    }
	    int n = sector.writeTo( out, sectorSize );
	    while( n < sectorSize ) {
	      out.write( 0 );
	      n++;
	    }
	  }
	}
      }
      out.close();
      out = null;

      if( hasDeleted && (msgBuf != null) ) {
	msgBuf.append( "\nGel\u00F6schte Sektoren werden"
		+ " in einfachen Abbilddateien nicht unterst\u00FCtzt\n"
		+ "und sind deshalb als normale Sektoren enthalten.\n" );
      }
    }
    finally {
      EmuUtil.doClose( out );
    }
    return msgBuf != null ? msgBuf.toString() : null;
  }


  public static PlainDisk newFile( Frame owner, File file ) throws IOException
  {
    PlainDisk        rv  = null;
    FileLock         fl  = null;
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile( file, "rw" );
      fl  = EmuUtil.lockFile( file, raf );
      raf.setLength( 0 );
      rv = new PlainDisk(
			owner,
			0,
			0,
			0,
			0,
			0,			// kein Interleave
			file.getPath(),
			null,
			raf,
			fl,
			null,
			false,
			true );
    }
    finally {
      if( rv == null ) {
        EmuUtil.doRelease( fl );
        EmuUtil.doClose( raf );
      }
    }
    return rv;
  }


  public static PlainDisk openFile(
				Frame            owner,
				File             file,
				boolean          readOnly,
				FloppyDiskFormat fmt ) throws IOException
  {
    PlainDisk        rv  = null;
    FileLock         fl  = null;
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile( file, readOnly ? "r" : "rw" );
      if( !readOnly ) {
	fl = EmuUtil.lockFile( file, raf );
      }
      rv = new PlainDisk(
			owner,
			fmt.getSides(),
			fmt.getCylinders(),
			fmt.getSectorsPerCylinder(),
			fmt.getSectorSize(),
			0,			// kein Interleave
			file.getPath(),
			null,
			raf,
			fl,
			null,
			readOnly,
			!readOnly );
    }
    finally {
      if( rv == null ) {
        EmuUtil.doRelease( fl );
        EmuUtil.doClose( raf );
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public synchronized void doClose()
  {
    EmuUtil.doRelease( this.fileLock );
    EmuUtil.doClose( this.raf );
    EmuUtil.doClose( this.rad );
  }


  @Override
  public boolean formatTrack(
			int        physCyl,
			int        physHead,
			SectorID[] sectorIDs,
			byte[]     dataBuf )
  {
    boolean rv = false;
    if( !this.readOnly
	&& ((this.rad != null) || (this.raf != null))
	&& (sectorIDs != null)
	&& (dataBuf != null) )
    {
      int oldSectorSize = getSectorSize();
      if( (sectorIDs.length > 0)
	  && ((oldSectorSize == 0) || (oldSectorSize == dataBuf.length))
	  && this.appendable )
      {
	rv = true;
	try {
	  for( int i = 0; i < sectorIDs.length; i++ ) {
	    int  sectorIdx = sectorIDs[ i ].getSectorNum() - 1;
	    long filePos   = calcFilePos( physCyl, physHead, sectorIdx );
	    if( filePos >= 0 ) {
	      if( this.rad != null ) {
		this.rad.seek( filePos );
		this.rad.write( dataBuf, 0, dataBuf.length );
	      } else {
		this.raf.seek( filePos );
		this.raf.write( dataBuf );
	      }
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
		this.sectorSizeCode = sectorIDs[ 0 ].getSizeCode();
	      }
	    } else {
	      rv = false;
	      break;
	    }
	  }
	}
	catch( IOException ex ) {
	  rv = false;
	  fireShowError( "Anh\u00E4ngen von Sektoren fehlgeschlagen", ex );
	}
      } else {
	rv = super.formatTrack( physCyl, physHead, sectorIDs, dataBuf );
      }
    }
    return rv;
  }


  @Override
  public String getFileFormatText()
  {
    return "Einfache Abbilddatei";
  }


  @Override
  public synchronized SectorData getSectorByIndex(
					int physCyl,
					int physHead,
					int sectorIdx )
  {
    return getSectorByIndexInternal(
				physCyl,
				physHead,
				sectorIndexToInterleave( sectorIdx ) );
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
					sectorNum - 1 );
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
  public boolean isReadOnly()
  {
    return this.readOnly;
  }


  @Override
  public void putSettingsTo( Properties props, String prefix )
  {
    super.putSettingsTo( props, prefix );
    if( (props != null) && (fileName != null) ) {
      if( this.rad != null ) {
	props.setProperty( prefix + "drive", this.fileName );
      } else {
	props.setProperty( prefix + "file", this.fileName );
      }
    }
  }


  @Override
  public boolean writeSector(
			int        physCyl,
			int        physHead,
			SectorData sector,
			byte[]     dataBuf,
			int        dataLen,
			boolean    deleted )
  {
    boolean rv = false;
    if( !this.readOnly
	&& ((this.rad != null) || (this.raf != null))
	&& (sector != null)
	&& (dataBuf != null)
	&& !deleted )
    {
      if( (sector.getDisk() == this) && (dataLen == getSectorSize()) ) {
	int  sectorIdx = sector.getIndexOnCylinder();
	long filePos   = calcFilePos( physCyl, physHead, sectorIdx );
	if( filePos == sector.getFilePos() ) {
	  try {
	    if( this.rad != null ) {
	      this.rad.seek( filePos );
	      this.rad.write( dataBuf, 0, dataLen );
	    } else {
	      this.raf.seek( filePos );
	      this.raf.write( dataBuf, 0, dataLen );
	    }
	    rv = true;
	  }
	  catch( IOException ex ) {
	    fireShowWriteError(
			physCyl,
			physHead,
			sector.getSectorNum(),
			ex );
	    sector.setError( true );
	  }
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private PlainDisk(
		Frame                       owner,
		int                         sides,
		int                         cyls,
		int                         sectorsPerCyl,
		int                         sectorSize,
		int                         interleave,
		String                      fileName,
		DeviceIO.RandomAccessDevice rad,
		RandomAccessFile            raf,
		FileLock                    fileLock,
		byte[]                      diskBytes,
		boolean                     readOnly,
		boolean                     appendable )
  {
    super( owner, sides, cyls, sectorsPerCyl, sectorSize, interleave );
    this.fileName       = fileName;
    this.rad            = rad;
    this.raf            = raf;
    this.fileLock       = fileLock;
    this.diskBytes      = diskBytes;
    this.readOnly       = readOnly;
    this.appendable     = appendable;
    this.sectorSizeCode = SectorData.getSizeCode( sectorSize );
  }


  private long calcFilePos( int cyl, int head, int sectorIdx )
  {
    head &= 0x01;

    long rv         = -1;
    int  sectorSize = getSectorSize();
    if( (cyl >= 0) && (sectorIdx >= 0) ) {
      int sides         = getSides();
      int cyls          = getCylinders();
      int sectorsPerCyl = getSectorsPerCylinder();
      if( (head < sides)
	  //&& (cyl < cyls)
	  && (sectorIdx < sectorsPerCyl)
	  && (sectorSize > 0) )
      {
	int nSkipSectors = sides * sectorsPerCyl * cyl;
	if( head > 0 ) {
	  nSkipSectors += sectorsPerCyl;
	}
	nSkipSectors += sectorIdx;
	rv = (long) nSkipSectors * (long) sectorSize;
      } else {
	/*
	 * Waehrend des Formatierens sind die Formatinformationen
	 * noch unvollstaendig.
	 * Deshalb wird hier sichergestellt,
	 * dass die Positionsberechnung funktioniert,
	 * wenn aufsteigend formatiert wird.
	 */
	if( (cyl == 0) && (cyls <= 1) ) {
	  if( (head == 0) && (sectorIdx == 0) ) {
	    rv = 0L;
	  } else {
	    if( sectorSize > 0 ) {
	      if( head == 0 ) {
		rv = sectorIdx * sectorSize;
	      }
	      else if( (head == 1) && (sectorsPerCyl > 0) ) {
		rv = (sectorsPerCyl + sectorIdx) * sectorSize;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  private synchronized SectorData getSectorByIndexInternal(
							int physCyl,
							int physHead,
							int sectorIdx )
  {
    SectorData rv         = null;
    int        sectorSize = getSectorSize();
    long       filePos    = calcFilePos( physCyl, physHead, sectorIdx );
    if( (sectorSize > 0) && (filePos >= 0) ) {
      if( this.diskBytes != null ) {
	if( filePos <= Integer.MAX_VALUE ) {
	  rv = new SectorData(
			sectorIdx,
			physCyl,
			physHead,
			sectorIdx + 1,
			this.sectorSizeCode,
			this.diskBytes,
			(int) filePos,
			sectorSize );
	}
      }
      else if( (this.rad != null) || (this.raf != null) ) {
	try {
	  byte[] buf = new byte[ sectorSize ];
	  int    len = -1;
	  if( this.rad != null ) {
	    this.rad.seek( filePos );
	    len = this.rad.read( buf, 0, buf.length );
	  } else {
	    this.raf.seek( filePos );
	    len = this.raf.read( buf );
	  }
	  if( len > 0 ) {
	    // falls nicht vollstaendig gelesen wurde
	    while( len < buf.length ) {
	      int n = -1;
	      if( this.rad != null ) {
		n = this.rad.read( buf, len, buf.length - len );
	      } else {
		n = this.raf.read( buf, len, buf.length - len );
	      }
	      if( n > 0 ) {
		len += n;
	      } else {
		break;
	      }
	    }
	  }
	  if( len > 0 ) {
	    rv = new SectorData(
				sectorIdx,
				physCyl,
				physHead,
				sectorIdx + 1,
				this.sectorSizeCode,
				buf,
				0,
				len );
	  }
	}
	catch( IOException ex ) {
	  fireShowReadError( physCyl, physHead, sectorIdx + 1, ex );
	  rv = new SectorData(
			sectorIdx,
			physCyl,
			physHead,
			sectorIdx + 1,
			this.sectorSizeCode,
			null,
			0,
			0 );
	  rv.setError( true );
	}
      }
      if( rv != null ) {
	rv.setDisk( this );
	rv.setFilePos( filePos );
	rv.setFilePortionLen( sectorSize );
      }
    }
    return rv;
  }
}
