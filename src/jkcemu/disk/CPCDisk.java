/*
 * (c) 2012-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette,
 * deren Daten in einer CPC-Disk-Image-Datei vorliegen
 *
 * Die Klasse unterstuetzt sowohl das Standard-
 * als auch das Extended-Format.
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.*;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;


public class CPCDisk extends AbstractFloppyDisk
{
  private static final String FILE_HEADER_EXT =
				"EXTENDED CPC DSK File\r\nDisk-Info\r\n";

  private static final String FILE_HEADER_STD =
				"MV - CPCEMU Disk-File\r\nDisk-Info\r\n";

  private static final int MAX_SECTORS_PER_TRACK = 29;


  public static class TrackData
  {
    private long                       trackPos;
    private int                        trackSize;
    private java.util.List<SectorData> sectors;

    public TrackData( long trackPos, int trackSize )
    {
      this.trackPos  = trackPos;
      this.trackSize = trackSize;
      sectors        = new ArrayList<>( MAX_SECTORS_PER_TRACK );
    }

    public void add( SectorData sector )
    {
      this.sectors.add( sector );
    }

    public void clear()
    {
      this.sectors.clear();
    }

    public java.util.List<SectorData> getSectorList()
    {
      return this.sectors;
    }

    public long getTrackPos()
    {
      return this.trackPos;
    }

    public int getTrackSize()
    {
      return this.trackSize;
    }
  };


  private static final String TRACK_HEADER = "Track-Info\r\n";

  private String                 fileName;
  private boolean                resource;
  private boolean                extendedFmt;
  private int                    trackSize;
  private FileLock               fileLock;
  private RandomAccessFile       raf;
  private Map<Integer,TrackData> side0;
  private Map<Integer,TrackData> side1;


  public static String export(
			AbstractFloppyDisk disk,
			File               file ) throws IOException
  {
    StringBuilder msgBuf = null;

    // Format pruefen
    int cyls = disk.getCylinders();
    if( (cyls < 0) || (cyls > 255) ) {
      throw new IOException(
		String.format(
			"%d Zylinder nicht unterst\u00FCtzt",
			cyls ) );
    }
    int sides = disk.getSides();
    if( (sides < 1) || (sides > 2) ) {
      throw new IOException(
		String.format(
			"%d Seiten nicht unterst\u00FCtzt",
			sides ) );
    }
    int sectorsPerCyl = disk.getSectorsPerCylinder();
    if( (sectorsPerCyl < 0) || (sectorsPerCyl > 255) ) {
      throw new IOException(
		String.format(
			"%d Sektoren pro Zylinder nicht unterst\u00FCtzt",
			sectorsPerCyl ) );
    }
    int     sectorSize     = disk.getSectorSize();
    int     sectorSizeCode = getSectorSizeCode( sectorSize );
    int     initBufSize    = (disk.getDiskSize() * 5 / 4) + 0x4000;
    boolean needsExtFmt    = false;
    boolean hasDeleted     = false;

    // Sektoren lesen
    SectorData[][] tracks = new SectorData[ cyls * sides ][];
    Arrays.fill( tracks, null );
    int trackIdx = 0;
    for( int cyl = 0; cyl < cyls; cyl++ ) {
      for( int head = 0; head < sides; head++ ) {
	int nTrackSectors = disk.getSectorsOfCylinder( cyl, head );
	if( (nTrackSectors < 0) || (nTrackSectors > MAX_SECTORS_PER_TRACK) ) {
	    throw new IOException(
		String.format(
			"Seite %d, Spur %d: %d Sektoren"
				+ " nicht unterst\u00FCtzt",
			head + 1,
			cyl,
			nTrackSectors ) );
	}
	if( nTrackSectors != sectorsPerCyl ) {
	  needsExtFmt = true;
	}
	SectorData[] trackSectors = new SectorData[ nTrackSectors ];
	for( int i = 0; i < trackSectors.length; i++ ) {
	  SectorData sector = disk.getSectorByIndex( cyl, head, i );
	  if( sector == null ) {
	    throw new IOException(
		String.format(
			"Seite %d, Spur %d: Sektor %d nicht gefunden",
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
	  if( (sector.getSizeCode() != sectorSizeCode)
	      || (sector.getDataLength() != sectorSize) )
	  {
	    needsExtFmt = true;
	  }
	  trackSectors[ i ] = sector;
	}
	tracks[ trackIdx++ ] = trackSectors;
      }
    }

    /*
     * Datei im Standardformat in temporaeren Puffer schreiben,
     * wenn bisher noch kein Grund bekannt ist,
     * dass das erweiterte Format verwendet werden muss
     */
    if( !needsExtFmt ) {
      ByteArrayOutputStream outBuf = new ByteArrayOutputStream( initBufSize );
      try {

	// Disk Information Block
	int trackSize = 0x0100 + (sectorsPerCyl * sectorSize);
	EmuUtil.writeASCII( outBuf, FILE_HEADER_STD );
	EmuUtil.writeFixLengthASCII( outBuf, "JKCEMU", 14, 0 );
	outBuf.write( cyls );
	outBuf.write( sides );
	outBuf.write( trackSize & 0xFF );
	outBuf.write( trackSize >> 8 );
	for( int i = outBuf.size(); i < 0x100; i++ ) {
	  outBuf.write( 0 );
	}
	for( int i = 0; i < tracks.length; i++ ) {
	  int          cyl          = i / sides;
	  int          head         = i % sides;
	  SectorData[] trackSectors = tracks[ i ];
	  if( trackSectors.length != sectorsPerCyl ) {
	    needsExtFmt = true;
	    break;
	  }

	  // Track Information Block
	  int trackBegPos = outBuf.size();
	  EmuUtil.writeFixLengthASCII( outBuf, TRACK_HEADER, 16, 0 );
	  outBuf.write( cyl );
	  outBuf.write( head );
	  outBuf.write( 0 );
	  outBuf.write( 0 );
	  outBuf.write( sectorSizeCode );
	  outBuf.write( trackSectors.length );
	  outBuf.write( 0x4E );			// GAP 3 Laenge
	  outBuf.write( 0xE5 );			// Fuellbyte
	  for( SectorData sector : trackSectors ) {
	    if( sector.getDataLength() != sectorSize ) {
	      needsExtFmt = true;
	      break;
	    }
	    outBuf.write( sector.getCylinder() );
	    outBuf.write( sector.getHead() );
	    outBuf.write( sector.getSectorNum() );
	    outBuf.write( sectorSizeCode );
	    outBuf.write( 0 );			// FDC Statusregister 1
	    outBuf.write( 0 );			// FDC Statusregister 2
	    outBuf.write( 0 );
	    outBuf.write( 0 );
	  }
	  for( int k = outBuf.size() - trackBegPos; k < 0x100; k++ ) {
	    outBuf.write( 0 );
	  }
	  for( SectorData sector : trackSectors ) {
	    int nBytes = 0;
	    if( sector != null ) {
	      nBytes = sector.writeTo( outBuf, sectorSize );
	    }
	    for( int k = nBytes; k < sectorSize; k++ ) {
	      outBuf.write( 0 );
	    }
	  }
	}
      }
      finally {
	EmuUtil.closeSilent( outBuf );
      }
      if( !needsExtFmt ) {
	OutputStream out = null;
	try {
	  out = EmuUtil.createOptionalGZipOutputStream( file );
	  outBuf.writeTo( out );
	  out.close();
	  out = null;
	}
	finally {
	  EmuUtil.closeSilent( out );
	}
      }
    }

    /*
     * Wenn das Standardformat nicht ausreicht,
     * dann das erweiterte Format erzeugen
     */
    if( needsExtFmt ) {
      OutputStream out = null;
      try {
	out = EmuUtil.createOptionalGZipOutputStream( file );

	// Disk Information Block
	EmuUtil.writeASCII( out, FILE_HEADER_EXT );
	EmuUtil.writeFixLengthASCII( out, "JKCEMU", 14, 0 );
	out.write( cyls );
	out.write( sides );
	out.write( 0 );
	out.write( 0 );
	for( SectorData[] trackSectors : tracks ) {
	  int trackSize = 0;
	  for( SectorData sector : trackSectors ) {
	    trackSize += sector.getDataLength();
	  }
	  out.write( trackSize > 0 ? ((trackSize + 0x1FF) >> 8) : 0 );
	}
	for( int i = 0x34 + tracks.length; i < 0x0100; i++ ) {
	  out.write( 0 );
	}

	// Tracks
	for( int i = 0; i < tracks.length; i++ ) {
	  int          cyl          = i / sides;
	  int          head         = i % sides;
	  SectorData[] trackSectors = tracks[ i ];

	  // Track Information Block
	  EmuUtil.writeFixLengthASCII( out, TRACK_HEADER, 16, 0 );
	  out.write( cyl );
	  out.write( head );
	  out.write( 0 );
	  out.write( 0 );
	  out.write( sectorSizeCode );
	  out.write( trackSectors.length );
	  out.write( 0x4E );			// GAP 3 Laenge
	  out.write( 0xE5 );			// Fuellbyte
	  for( SectorData sector : trackSectors ) {
	    out.write( sector.getCylinder() );
	    out.write( sector.getHead() );
	    out.write( sector.getSectorNum() );
	    out.write( sectorSizeCode );
	    out.write( 0 );			// FDC Statusregister 1
	    out.write( 0 );			// FDC Statusregister 2
	    int dataLen = sector.getDataLength();
	    out.write( dataLen & 0xFF );
	    out.write( dataLen >> 8 );
	  }
	  for( int k = trackSectors.length; k < MAX_SECTORS_PER_TRACK; k++ ) {
	    for( int l = 0; l < 8; l++ ) {
	      out.write( 0 );
	    }
	  }
	  for( SectorData sector : trackSectors ) {
	    sector.writeTo( out, sector.getDataLength() );
	  }
	}
	out.close();
	out = null;
      }
      finally {
	EmuUtil.closeSilent( out );
      }
    }
    if( msgBuf != null ) {
      msgBuf.append( "\nDie angezeigten Informationen k\u00F6nnen"
		+ " in einer CPC-Disk-Datei nicht gespeichert werden\n"
		+ "und sind deshalb in der erzeugten Datei"
		+ " nicht mehr enthalten.\n" );
      if( hasDeleted ) {
	msgBuf.append( "\nGel\u00F6schte Sektoren werden"
		+ " in CPC-Disk-Dateien nicht unterst\u00FCtzt\n"
		+ "und sind deshalb als normale Sektoren enthalten.\n" );
      }
    }
    return msgBuf != null ? msgBuf.toString() : null;
  }


  public static boolean isCPCDiskFileHeader( byte[] header )
  {
    return isDiskFileHeader( header, FILE_HEADER_STD )
		|| isDiskFileHeader( header, FILE_HEADER_EXT );
  }


  public static CPCDisk newFile( Frame owner, File file ) throws IOException
  {
    CPCDisk          rv  = null;
    FileLock         fl  = null;
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile( file, "rw" );
      fl  = EmuUtil.lockFile( file, raf );
      raf.setLength( 0 );
      raf.seek( 0 );
      rv = new CPCDisk(
		owner,
		0,
		0,
		0,
		0,
		file.getPath(),
		false,
		false,
		0,
		raf,
		fl,
		null,
		null );
    }
    finally {
      if( rv == null ) {
	EmuUtil.releaseSilent( fl );
	EmuUtil.closeSilent( raf );
      }
    }
    return rv;
  }


  public static CPCDisk openFile( Frame owner, File file ) throws IOException
  {
    CPCDisk          rv  = null;
    FileLock         fl  = null;
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile( file, "rw" );
      fl  = EmuUtil.lockFile( file, raf );
      rv  = createInstance( owner, null, raf, fl, file.getPath(), false );
    }
    finally {
      if( rv == null ) {
	EmuUtil.releaseSilent( fl );
	EmuUtil.closeSilent( raf );
      }
    }
    return rv;
  }


  public static CPCDisk readFile( Frame owner, File file ) throws IOException
  {
    CPCDisk     rv = null;
    InputStream in = null;
    try {
      in = new FileInputStream( file );
      if( EmuUtil.isGZipFile( file ) ) {
	in = new GZIPInputStream( in );
      }
      rv = createInstance( owner, in, null, null, file.getPath(), false );
    }
    finally {
      if( rv == null ) {
	EmuUtil.closeSilent( in );
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public synchronized void closeSilent()
  {
    EmuUtil.releaseSilent( this.fileLock );
    EmuUtil.closeSilent( this.raf );
  }


  @Override
  public boolean formatTrack(
			int        physCyl,
			int        physHead,
			SectorID[] sectorIDs,
			byte[]     dataBuf )
  {
    boolean rv = false;
    physHead &= 0x01;
    if((this.raf != null) && (sectorIDs != null) && (dataBuf != null) ) {
      if( sectorIDs.length > 0 ) {
	try {
	  if( (physHead == 1)
	      && (getSides() < 2)
	      && (getCylinders() > 1) )
	  {
	    throw new IOException(
		"Nachtr\u00E4gliches Formatieren der zweiten Seite"
			+ " nicht unterstuetzt" );
	  }

	  // neue Groesse der Spur
	  int trackSize = (0x1FF + (sectorIDs.length * dataBuf.length))
								& 0xFF00;

	  // Dateikopf schreiben, wenn Diskette leer ist
	  if( getCylinders() == 0 ) {
	    setCylinders( 0 );
	    setSides( 0 );
	    setSectorSize( 0 );
	    setSectorsPerCylinder( 0 );
	    this.extendedFmt = false;
	    this.trackSize   = trackSize;
	    if( this.side0 != null ) {
	      this.side0.clear();
	    }
	    if( this.side1 != null ) {
	      this.side1.clear();
	    }

	    // Standard-Dateikopf schreiben
	    this.raf.setLength( 0 );
	    this.raf.seek( 0 );
	    this.raf.writeBytes( FILE_HEADER_STD );
	    this.raf.writeBytes( "JKCEMU" );
	    for( int i = 0; i < 8; i++ ) {
	      this.raf.write( 0 );
	    }
	    this.raf.write( getCylinders() );
	    this.raf.write( getSides() );
	    this.raf.write( this.trackSize );
	    this.raf.write( this.trackSize >> 8 );
	    for( int i = 0; i < 204; i++ ) {
	      this.raf.write( 0 );
	    }
	  }

	  // alte Spurgroesse und -Position ermitteln
	  long      oldTrackPos  = 0;
	  int       oldTrackSize = 0;
	  TrackData trackData    = getTrackData( physCyl, physHead );
	  if( trackData != null ) {
	    oldTrackPos  = trackData.getTrackPos();
	    oldTrackSize = trackData.getTrackSize();
	  }

	  /*
	   * Wenn die neue Spur nicht auf die alte geschrieben werden kann,
	   * muss der Rest der Abbilddatei geloescht und
	   * evtl. auch das Format auf Extended umgestellt werden.
	   */
	  long trackPos = oldTrackPos;
	  if( (trackPos < 0x100) || (trackSize != oldTrackSize) ) {
	    if( physHead == 0 ) {
	      clearSideFromCyl( this.side0, physCyl );
	    } else {
	      clearSideFromCyl( this.side0, physCyl + 1 );
	    }
	    clearSideFromCyl( this.side1, physCyl );
	    trackData = null;

	    if( this.extendedFmt ) {
	      /*
	       * Position der Spur anhand der Groesse
	       * der vorherigen Spuren ermitteln
	       */
	      trackPos = 0x100;
	      if( this.side0 != null ) {
		int endCyl = physCyl;
		if( physHead == 1 ) {
		  endCyl++;
		}
		for( int i = 0; i < endCyl; i++ ) {
		  TrackData td = this.side0.get( i );
		  if( td != null ) {
		    trackPos += td.getTrackSize();
		  }
		}
	      }
	      if( (this.side1 != null) && (getSides() > 1) ) {
		for( int i = 0; i < physCyl; i++ ) {
		  TrackData td = this.side1.get( i );
		  if( td != null ) {
		    trackPos += td.getTrackSize();
		  }
		}
	      }
	    } else {
	      trackPos = 0x100
			+ (physCyl * getSides() * this.trackSize)
			+ (physHead * this.trackSize);
	      if( (trackSize != this.trackSize)
		  || (dataBuf.length > 6144) )
	      {
		// Umstellung auf das Extended-Format
		this.raf.seek( 0L );
		this.raf.writeBytes( FILE_HEADER_EXT );
		this.raf.writeBytes( "JKCEMU" );
		for( int i = 0; i < 8; i++ ) {
		  this.raf.write( 0 );
		}
		this.raf.write( getCylinders() );
		this.raf.write( getSides() );
		this.raf.write( 0 );
		this.raf.write( 0 );
		int nTracks    = getCylinders() * getSides();
		int hTrackSize = (trackSize >> 8);
		for( int i = 0; i < nTracks; i++ ) {
		  this.raf.write( hTrackSize );
		}
		for( int i = nTracks; i < 204; i++ ) {
		  this.raf.write( 0 );
		}
		this.extendedFmt = true;
		this.trackSize   = 0;
	      }
	    }
	  }

	  // Spur-Objekt anlegen
	  if( trackData == null ) {
	    Map<Integer,TrackData> map = null;
	    if( (physHead & 0x01) != 0 ) {
	      if( this.side1 == null ) {
		this.side1 = new HashMap<Integer,TrackData>();
	      }
	      map = this.side1;
	    } else {
	      if( this.side0 == null ) {
		this.side0 = new HashMap<Integer,TrackData>();
	      }
	      map = this.side0;
	    }
	    trackData = new TrackData( trackPos, trackSize );
	    map.put( physCyl, trackData );
	  }

	  // Sektorgroesse
	  int sectorSizeCode = getSectorSizeCode( dataBuf.length );

	  // Track-Header schreiben
	  this.raf.seek( trackPos );
	  this.raf.writeBytes( TRACK_HEADER );
	  for( int i = 0; i < 4; i++ ) {
	    this.raf.write( 0 );
	  }
	  this.raf.write( physCyl );
	  this.raf.write( physHead );
	  this.raf.write( 0 );
	  this.raf.write( 0 );
	  this.raf.write( sectorSizeCode );
	  this.raf.write( sectorIDs.length );
	  this.raf.write( 0x4E );		// GAP 3 Laenge
	  this.raf.write( 0xE5 );		// Fuellbyte
	  for( int i = 0; i < sectorIDs.length; i++ ) {
	    this.raf.write( sectorIDs[ i ].getCylinder() );
	    this.raf.write( sectorIDs[ i ].getHead() );
	    this.raf.write( sectorIDs[ i ].getSectorNum() );
	    this.raf.write( sectorIDs[ i ].getSizeCode() );
	    for( int k = 0; k < 4; k++ ) {
	      this.raf.write( 0 );
	    }
	  }
	  for( int i = sectorIDs.length; i < MAX_SECTORS_PER_TRACK; i++ ) {
	    for( int k = 0; k < 8; k++ ) {
	      this.raf.write( 0 );
	    }
	  }

	  // Sektoren schreiben
	  int nRemain = this.trackSize - 0x100;
	  for( int i = 0; i < sectorIDs.length; i++ ) {
	    SectorID   sectorID = sectorIDs[ i ];
	    SectorData sector   = new SectorData(
					i,
					sectorID.getCylinder(),
					sectorID.getHead(),
					sectorID.getSectorNum(),
					sectorID.getSizeCode(),
					dataBuf,
					0,
					dataBuf.length );
	    sector.setDisk( this );
	    sector.setFilePortionLen( dataBuf.length );
	    sector.setFilePos( this.raf.getFilePointer() );
	    this.raf.write( dataBuf );
	    nRemain -= dataBuf.length;
	    trackData.add( sector );
	  }
	  while( nRemain > 0 ) {
	    this.raf.write( 0 );
	    --nRemain;
	  }

	  // ggf. Format anpassen
	  if( physCyl >= getCylinders() ) {
	    setCylinders( physCyl + 1 );
	    this.raf.seek( 0x30 );
	    this.raf.write( getCylinders() );
	  }
	  int sides = ((physHead & 0x01) != 0 ? 2 : 1);
	  if( sides > getSides() ) {
	    setSides( sides );
	    this.raf.seek( 0x31 );
	    this.raf.write( getSides() );
	  }
	  if( sectorIDs.length > getSectorsPerCylinder() ) {
	    setSectorsPerCylinder( sectorIDs.length );
	  }
	  if( getSectorSize() == 0 ) {
	    setSectorSize( dataBuf.length );
	  }

	  // Bei Extended-Format die Spurgroesse im Kopf eintragen
	  if( this.extendedFmt ) {
	    this.raf.seek( 0x34 + (physCyl * getSides()) + physHead );
	    this.raf.write( trackSize >> 8 );
	  }
	  rv = true;
	}
	catch( IOException ex ) {
	  rv = false;
	  fireShowError( "Formatieren fehlgeschlagen", ex );
	}
      }
    }
    return rv;
  }


  @Override
  public String getFileFormatText()
  {
    return "CPC-Disk-Datei";
  }


  @Override
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
	  rv.setDisk( this );	// da es beim Einlesen nicht getan wird
	}
      }
    }
    return rv;
  }


  @Override
  public int getSectorsOfCylinder( int physCyl, int physHead )
  {
    java.util.List<SectorData> sectors = getSectorsOfCyl( physCyl, physHead );
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
	    if( dataLen < sector.getFilePortionLen() ) {
	      throwSectorSpaceTooSmall(
				physCyl,
				physHead,
				sector.getSectorNum() );
	    }
	    this.raf.seek( filePos );
	    this.raf.write( dataBuf, 0, dataLen );
	    sector.setData( deleted, dataBuf, dataLen );
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

  private CPCDisk(
		Frame                  owner,
		int                    sides,
		int                    cyls,
		int                    sectorsPerCyl,
		int                    sectorSize,
		String                 fileName,
		boolean                resource,
		boolean                extendedFmt,
		int                    trackSize,
		RandomAccessFile       raf,
		FileLock               fileLock,
		Map<Integer,TrackData> side0,
		Map<Integer,TrackData> side1 )
  {
    super( owner, sides, cyls, sectorsPerCyl, sectorSize );
    this.fileName  = fileName;
    this.resource  = resource;
    this.raf       = raf;
    this.fileLock  = fileLock;
    this.trackSize = trackSize;
    this.side0     = side0;
    this.side1     = side1;
  }


  private static void clearSideFromCyl(
				Map<Integer,TrackData> side,
				int                    fromCyl )
  {
    if( side != null ) {
      Set<Integer> cyls = side.keySet();
      if( cyls != null ) {
	int size = cyls.size();
	if( size > 0 ) {
	  Integer[] cylArray = cyls.toArray( new Integer[ size ] );
	  if( cylArray != null ) {
	    for( Integer cyl : cylArray ) {
	      if( cyl.intValue() >= fromCyl ) {
		side.remove( cyl );
	      }
	    }
	  }
	}
      }
    }
  }


  private static CPCDisk createInstance(
				Frame            owner,
				InputStream      in,
				RandomAccessFile raf,
				FileLock         fl,
				String           fileName,
				boolean          resource ) throws IOException
  {
    CPCDisk rv = null;
    if( in == null ) {
      in = EmuUtil.createInputStream( raf );
    }

    // Disk Header pruefen
    byte[] fileHeader = new byte[ 0x100 ];
    long   filePos    = EmuUtil.read( in, fileHeader );
    if( filePos == fileHeader.length ) {
      boolean extendedFmt = isDiskFileHeader( fileHeader, FILE_HEADER_EXT );
      if( extendedFmt || isDiskFileHeader( fileHeader, FILE_HEADER_STD ) ) {
	int sectorsPerCyl  = 0;
	int diskSectorSize = 0;
	int nCyls          = (int) fileHeader[ 0x30 ] & 0xFF;
	int nSides         = (int) fileHeader[ 0x31 ] & 0xFF;
	int diskTrackSize  = 0;
	if( !extendedFmt ) {
	  diskTrackSize = EmuUtil.getWord( fileHeader, 0x32 );
	}

	if( (nCyls > 0) && (nSides > 0) ) {
	  int nTracks = nCyls * nSides;

	  Map<Integer,TrackData> side0 = null;
	  Map<Integer,TrackData> side1 = null;

	  // Spuren lesen
	  byte[] trackHeader = new byte[ 0x100 ];
	  for( int i = 0; i < nTracks; i++ ) {
	    long trackPos = filePos;
	    if( EmuUtil.read( in, trackHeader ) != trackHeader.length ) {
	      break;
	    }
	    if( !TRACK_HEADER.equals(
				new String(
					trackHeader,
					0,
					TRACK_HEADER.length(),
					"US-ASCII" ) ) )
	    {
	      break;
	    }
	    filePos += trackHeader.length;

	    int trackSize = diskTrackSize;
	    if( extendedFmt ) {
	      trackSize = ((int) fileHeader[ 0x34 + i ] << 8) & 0xFF00;
	    }
	    int cyl            = (int) trackHeader[ 0x10 ] & 0xFF;
	    int side           = (int) trackHeader[ 0x11 ] & 0xFF;
	    int sectorSizeCode = (int) trackHeader[ 0x14 ] & 0xFF;
	    if( sectorSizeCode > 6 ) {
	      side0 = null;
	      side1 = null;
	      break;
	    }
	    int sectorSize = 128;
	    if( !extendedFmt && (sectorSizeCode == 6) ) {
	      sectorSize = 0x1800;
	    } else if( sectorSizeCode > 0 ) {
	      sectorSize <<= sectorSizeCode;
	    }
	    if( sectorSize > diskSectorSize ) {
	      diskSectorSize = sectorSize;
	    }
	    int nSectors = (int) trackHeader[ 0x15 ] & 0xFF;
	    if( nSectors > MAX_SECTORS_PER_TRACK ) {
	      side0 = null;
	      side1 = null;
	      break;
	    }

	    long   sectorPos = filePos;
	    byte[] trackBuf  = null;
	    if( trackSize > 0x100 ) {
	      trackBuf = new byte[ trackSize - 0x100 ];
	      EmuUtil.read( in, trackBuf );
	      filePos += trackBuf.length;
	    }

	    // Sektorliste
	    if( nSectors > 0 ) {
	      if( sectorsPerCyl < nSectors ) {
		sectorsPerCyl = nSectors;
	      }
	      TrackData              trackData = null;
	      Map<Integer,TrackData> trackMap  = null;
	      if( side == 0 ) {
		if( side0 == null ) {
		  side0 = new HashMap<Integer,TrackData>();
		}
		trackMap = side0;
	      } else if( side == 1 ) {
		if( side1 == null ) {
		  side1 = new HashMap<Integer,TrackData>();
		}
		trackMap = side1;
	      }
	      if( (trackMap != null) && (trackBuf != null) ) {
		trackData = trackMap.get( cyl );
	        if( trackData == null ) {
		  trackData = new TrackData( trackPos, trackBuf.length );
		  trackMap.put( cyl, trackData );
		}
	      }
	      int infoPos = 0x18;
	      int dataPos = 0;
	      for( int k = 0; k < nSectors; k++ ) {
		int idCyl      = (int) trackHeader[ infoPos++ ] & 0xFF;
		int idHead     = (int) trackHeader[ infoPos++ ] & 0xFF;
		int idRecord   = (int) trackHeader[ infoPos++ ] & 0xFF;
		int idSizeCode = (int) trackHeader[ infoPos++ ] & 0xFF;
		if( extendedFmt ) {
		  sectorSize = EmuUtil.getWord( trackHeader, infoPos + 2 );
		}
		infoPos += 4;

		if( trackBuf != null ) {
		  SectorData sector = new SectorData(
			i,
			idCyl,
			idHead,
			idRecord,
			idSizeCode,
			trackBuf,
			dataPos,
			Math.min( sectorSize, trackBuf.length - dataPos ) );
		  sector.setFilePortionLen( sectorSize );
		  sector.setFilePos( sectorPos );
		  trackData.add( sector );

		  dataPos += sectorSize;
		  sectorPos += sectorSize;
		}
	      }
	    }
	  }
	  if( (side0 != null) || (side1 != null) ) {
	    int sides = 1;
	    if( side1 != null ) {
	      sides++;
	    }
	    rv = new CPCDisk(
			owner,
			nSides,
			nCyls,
			sectorsPerCyl,
			diskSectorSize,
			fileName,
			resource,
			extendedFmt,
			diskTrackSize,
			raf,
			fl,
			side0,
			side1 );
	  }
	}
      }
    }
    if( rv == null ) {
      throw new IOException( "Datei ist keine CPC-Disk-Datei" );
    }
    return rv;
  }


  private java.util.List<SectorData> getSectorsOfCyl(
						int physCyl,
						int physHead )
  {
    java.util.List<SectorData> rv = null;
    Map<Integer,TrackData> map = ((physHead & 0x01) != 0 ? side1 : side0);
    if( map != null ) {
      TrackData trackData = map.get( physCyl );
      if( trackData != null ) {
	rv = trackData.getSectorList();
      }
    }
    return rv;
  }


  private static int getSectorSizeCode( int sectorSize )
  {
    int sectorSizeCode = 0;
    while( sectorSize > 0x80 ) {
      sectorSize = (sectorSize >> 1) | (sectorSize & 0x01);
      sectorSizeCode++;
    }
    return sectorSizeCode;
  }


  private static boolean isDiskFileHeader( byte[] header, String pattern )
  {
    boolean rv = false;
    if( header != null ) {
      int len = pattern.length();
      if( header.length >= len ) {
	try {
	  rv = pattern.equals( new String( header, 0, len, "US-ASCII" ) );
	}
	catch( UnsupportedEncodingException ex ) {}
      }
    }
    return rv;
  }


  private TrackData getTrackData( int cyl, int side )
  {
    TrackData trackData = null;
    if( (side == 0) && (this.side0 != null) ) {
      trackData = this.side0.get( cyl );
    } else if( (side == 0) && (this.side0 != null) ) {
      trackData = this.side1.get( cyl );
    }
    return trackData;
  }
}

