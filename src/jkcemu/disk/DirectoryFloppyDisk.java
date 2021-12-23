/*
 * (c) 2009-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette,
 * die auf ein Verzeichnis im Dateisystem des Host-Systems abbildet
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileTimesData;
import jkcemu.file.FileUtil;


public class DirectoryFloppyDisk extends AbstractFloppyDisk
{
  public static final String PROP_AUTO_REFRESH      = "auto_refresh";
  public static final String PROP_BLOCK_NUMBER_SIZE = "block_number_size";
  public static final String PROP_BLOCK_SIZE        = "block_size";
  public static final String PROP_DATESTAMPER       = "datestamper";
  public static final String PROP_DIR_BLOCKS        = "dir_blocks";
  public static final String PROP_DIRECTORY         = "directory";
  public static final String PROP_FORCE_LOWERCASE   = "force_lowercase";
  public static final String PROP_SYSTEM_TRACKS     = "system_tracks";

  public static final String SYS_FILE_NAME = "@boot.sys";


  private static class EntryNameComparator implements Comparator<String>
  {
    /*
     * Eintrage, die mit einem @ beginnen, sollen ganz am Anfang stehen.
     * Es muss beachtet werden, dass bei den Eintragsnamen
     * das erste Zeichen die User-Ebene ist.
     */
    public int compare( String s1, String s2 )
    {
      int rv = 0;
      if( s1 == null ) {
	s1 = "";
      }
      if( s2 == null ) {
	s2 = "";
      }
      boolean at1 = s1.startsWith( "0@" );
      boolean at2 = s2.startsWith( "0@" );
      if( at1 && !at2 ) {
	rv = -1;
      } else if( !at1 && at2 ) {
	rv = 1;
      } else {
	rv = s1.compareTo( s2 );
      }
      return rv;
    }
  };

  private static EntryNameComparator entryNameComparator = null;

  private File                  dirFile;
  private File                  sysFile;
  private long                  lastBuildMillis;
  private Map<String,File>      fileMap;
  private Map<String,Exception> errorMap;
  private byte[]                dirBytes;
  private SectorData[]          sectors;
  private int                   sectorSizeCode;
  private int                   sectorsPerBlock;
  private int                   dirBlocks;
  private int                   dirSectors;
  private int                   maxDirEntries;
  private int                   extentsPerDirEntry;
  private int                   sysTracks;
  private int                   sysSectors;
  private int                   blockSize;
  private boolean               blockNum16Bit;
  private boolean               autoRefresh;
  private boolean               forceLowerCase;
  private boolean               readOnly;
  private boolean               dsEnabled;
  private byte[]                dsBytes;
  private int                   dsFirstSector;
  private int                   dsSectors;
  private String                remark;
  private volatile boolean      refreshFired;


  public DirectoryFloppyDisk(
			Frame   owner,
			int     cyls,
			int     sides,
			int     sectorsPerTrack,
			int     sectorSize,
			int     sysTracks,
			int     dirBlocks,
			int     blockSize,
			boolean blockNum16Bit,
			boolean dateStamperEnabled,
			File    dirFile,
			boolean autoRefresh,
			boolean readOnly,
			boolean forceLowerCase )
  {
    super( owner, cyls, sides, sectorsPerTrack, sectorSize );
    this.dirFile         = dirFile;
    this.sysFile         = new File( this.dirFile, SYS_FILE_NAME );
    this.sysTracks       = sysTracks;
    this.sysSectors      = sysTracks * sides * sectorsPerTrack;
    this.blockSize       = blockSize;
    this.blockNum16Bit   = blockNum16Bit;
    this.sectorsPerBlock = blockSize / sectorSize;
    this.dirBlocks       = dirBlocks;
    this.dirSectors      = dirBlocks * this.sectorsPerBlock;
    this.sectorSizeCode  = SectorData.getSizeCodeBySize( sectorSize );
    this.autoRefresh     = autoRefresh;
    this.readOnly        = readOnly;
    this.forceLowerCase  = forceLowerCase;
    this.lastBuildMillis = -1L;
    this.fileMap         = new HashMap<>();
    this.errorMap        = null;;
    this.dirBytes        = null;
    this.dsEnabled       = dateStamperEnabled;
    this.dsBytes         = null;
    this.dsFirstSector   = -1;
    this.dsSectors       = 0;
    this.maxDirEntries   = 0;
    this.sectors         = new SectorData[ cyls * sides * sectorsPerTrack ];
    this.refreshFired    = false;
    this.extentsPerDirEntry = DiskUtil.getExtentsPerDirEntry(
							blockSize,
							blockNum16Bit );
    if( this.autoRefresh ) {
      this.remark = "Automatische Aktualisierung aktiv";
    } else {
      this.remark = "Keine automatische Aktualisierung";
    }
    Arrays.fill( this.sectors, null );
  }


  public void fireRefresh()
  {
    this.refreshFired = true;
  }


  public File getDirFile()
  {
    return this.dirFile;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getFileFormatText()
  {
    return null;
  }


  @Override
  public String getFormatText()
  {
    String text = super.getFormatText();
    if( (text != null) && this.dsEnabled ) {
      text += ", DateStamper";
    }
    return text;
  }


  @Override
  public String getRemark()
  {
    return this.remark;
  }


  @Override
  public synchronized SectorData getSectorByIndex(
						int physCyl,
						int physHead,
						int sectorIdx )
  {
    SectorData rv = null;
    if( this.dirBytes == null ) {
      rebuildDisk();
    } else {
      /*
       * Wenn eine Aktualisierung angefordert wurde oder AutoRefresh
       * aktiviert ist, wird beim naechsten lesenden Zugriff
       * auf den ersten Sektor der Diskette oder des Directories
       * die virtuelle Diskette neu erzeugt, bei AutoRefresh aber nur,
       * wenn die letzte Aktualisierung mehr als 5 Sekunden zurueckliegt.
       */
      if( ((physCyl == 0) || (physCyl == this.sysTracks))
	  && (physHead == 0)
	  && (sectorIdx == 0) )
      {
	if( this.refreshFired ) {
	  this.refreshFired = false;
	  rebuildDisk();
	} else if( this.autoRefresh ) {
	  long curMillis = System.currentTimeMillis();
	  if( (curMillis != -1L)
	      && ((this.lastBuildMillis == -1L)
		  || (this.lastBuildMillis < (curMillis - 5000))) )
	  {
	    rebuildDisk();
	  }
	}
      }
    }
    int sectPerTrack = getSectorsPerTrack();
    int sectorSize = getSectorSize();
    if( (physCyl >= 0)
	&& (sectPerTrack > 0)
	&& (sectorIdx >= 0) && (sectorIdx < sectPerTrack) )
    {
      int absSectIdx = computeAbsSectorIdx( physCyl, physHead, sectorIdx );
      if( (absSectIdx >= 0) && (absSectIdx < this.sectors.length) ) {
	rv = this.sectors[ absSectIdx ];
	if( rv == null ) {
	  if( physCyl < this.sysTracks ) {
	    if( this.sysFile.exists() ) {

	      // Systemspuren
	      int    idx       = 0;
	      byte[] fileBytes = readFile( this.sysFile );
	      if( fileBytes != null ) {
		int pos = 0;
		while( (idx < this.sysSectors) && (pos < fileBytes.length) ) {
		  setSectorData(
			idx++,
			fileBytes,
			pos,
			getSectorSize(),
			false );
		  pos += sectorSize;
		}
	      }
	    }

	  } else {

	    // Block berechnen
	    int relSectorIdx = absSectIdx - this.sysSectors;
	    int blockNum     = (relSectorIdx * sectorSize) / this.blockSize;
	    if( blockNum > 0 ) {

	      // betroffene Datei ermitteln, lesen und den Sektoren zuordnen
	      String entryName = findEntryNameByDataBlock( blockNum );
	      if( entryName != null ) {
		File file = this.fileMap.get( entryName );
		if( file != null ) {
		  loadFileIntoSectors(
				file,
				getBlockNumsByEntryName( entryName, null ) );
		}
	      }
	    }
	  }
	  rv = this.sectors[ absSectIdx ];
	}
	if( rv == null ) {
	  setSectorData( absSectIdx, null, 0, getSectorSize(), false );
	  rv = this.sectors[ absSectIdx ];
	}
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
  public int getSectorOffset()
  {
    return 0;
  }


  @Override
  protected int getSysTracks()
  {
    return this.sysTracks;
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
    if( props != null ) {
      props.setProperty(
		prefix + PROP_DIRECTORY,
		this.dirFile.getPath() );
      props.setProperty(
		prefix + PROP_SYSTEM_TRACKS,
		Integer.toString( this.sysTracks ) );
      props.setProperty(
		prefix + PROP_BLOCK_SIZE,
		Integer.toString( this.blockSize ) );
      props.setProperty(
		prefix + PROP_BLOCK_NUMBER_SIZE,
		this.blockNum16Bit ? "16" : "8" );
      props.setProperty(
		prefix + PROP_DIR_BLOCKS,
		Integer.toString( this.dirBlocks ) );
      props.setProperty(
		prefix + PROP_DATESTAMPER,
		Boolean.toString( this.dsEnabled ) );
      props.setProperty(
		prefix + PROP_AUTO_REFRESH,
		Boolean.toString( this.autoRefresh ) );
      props.setProperty(
		prefix + PROP_READONLY,
		Boolean.toString( isReadOnly() ) );
      props.setProperty(
		prefix + PROP_FORCE_LOWERCASE,
		Boolean.toString( this.forceLowerCase ) );
    }
  }


  @Override
  public synchronized boolean writeSector(
				int        physCyl,
				int        physHead,
				SectorData sector,
				byte[]     dataBuf,
				int        dataLen,
				boolean    deleted )
  {
    boolean rv = false;
    if( !this.readOnly
	&& (dataBuf != null)
	&& (dataLen > 0)
	&& !deleted )
    {
      if( dataLen > dataBuf.length ) {
	dataLen = dataBuf.length;
      }
      int absSectorIdx = computeAbsSectorIdx(
					physCyl,
					physHead,
					sector.getSectorNum() - 1 );
      if( (absSectorIdx >= 0) && (absSectorIdx < this.sectors.length) ) {
	RandomAccessFile raf = null;
	try {
	  int sectorSize = getSectorSize();

	  // Sektor in Lesepuffer schreiben
	  int    n = Math.min( dataLen, sectorSize );
	  byte[] a = new byte[ n ];
	  System.arraycopy( dataBuf, 0, a, 0, n );
	  setSectorData( absSectorIdx, a, 0, n, false );

	  // Synchronisation mit Dateisystem
	  if( absSectorIdx < this.sysSectors ) {
	    // Systemspuren
	    raf = new RandomAccessFile( this.sysFile, "rw" );
	    raf.seek( absSectorIdx * getSectorSize() );
	    raf.write( dataBuf, 0, Math.min( dataLen, sectorSize ) );
	    raf.close();
	    raf = null;
	  }
	  else if( (absSectorIdx >= this.sysSectors)
		   && (absSectorIdx < (this.sysSectors + this.dirSectors)) )
	  {
	    // Directory
	    writeDirSector( absSectorIdx, dataBuf, dataLen );
	  }
	  else if( this.dsEnabled
		   && (absSectorIdx >= this.dsFirstSector)
		   && (absSectorIdx < (this.dsFirstSector + this.dsSectors)) )
	  {
	    // DateStamper
	    writeDsSector( absSectorIdx, dataBuf, dataLen );
	  }
	  else if( absSectorIdx >= (this.sysSectors + this.dirSectors) ) {
	    // Datenbereich
	    int relSectorIdx = absSectorIdx - this.sysSectors;
	    if( relSectorIdx >= 0 ) {
	      int    blockNum  = relSectorIdx / this.sectorsPerBlock;
	      String entryName = findEntryNameByDataBlock( blockNum );
	      if( entryName != null ) {
		java.util.List<Integer> blockNums = getBlockNumsByEntryName(
								entryName,
								null );
		if( blockNums != null ) {
		  File file      = null;
		  int  blockOffs = 0;
		  for( Integer tmpBlockNum : blockNums ) {
		    if( tmpBlockNum.equals( blockNum ) ) {
		      file = this.fileMap.get( entryName );
		      break;
		    }
		    blockOffs++;
		  }
		  if( file != null ) {
		    int sectOffs = relSectorIdx
				- (blockNum * this.sectorsPerBlock);
		    raf = new RandomAccessFile( file, "rw" );
		    raf.seek( ((blockOffs * this.sectorsPerBlock)
					+ sectOffs) * getSectorSize() );
		    raf.write( dataBuf, 0, Math.min( dataLen, sectorSize ) );
		    raf.close();
		    raf = null;
		  }
		}
	      }
	    }
	  }
	}
	catch( IOException ex ) {
	  this.sectors[ absSectorIdx ] = null;
	  fireShowError( "E/A-Fehler", ex );
	  rv = false;
	}
	finally {
	  EmuUtil.closeSilently( raf );
	}
	rv = true;
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private int computeAbsSectorIdx(
				int physCyl,
				int physHead,
				int sectorIdx )
  {
    int absSectIdx   = -1;
    int sectPerTrack = getSectorsPerTrack();
    if( (physCyl >= 0)
	&& (sectPerTrack > 0)
	&& (sectorIdx >= 0) && (sectorIdx < sectPerTrack) )
    {
      absSectIdx = physCyl * getSides() * sectPerTrack;
      if( (physHead & 0x01) != 0 ) {
	absSectIdx += sectPerTrack;
      }
      absSectIdx += sectorIdx;
    }
    return absSectIdx;
  }


  private void ensureFileLoaded( String entryName )
  {
    if( (entryName != null)
	&& (this.dirBytes != null)
	&& (this.sectors != null) )
    {
      File file = this.fileMap.get( entryName );
      if( file != null ) {
	java.util.List<Integer> blockNums = getBlockNumsByEntryName(
								entryName,
								null );
	if( blockNums != null ) {
	  for( Integer blockNum : blockNums ) {
	    int tmpIdx = this.sysSectors + (blockNum * this.sectorsPerBlock);
	    for( int i = 0; i < this.sectorsPerBlock; i++ ) {
	      if( (tmpIdx >= 0) && (tmpIdx < this.sectors.length) ) {
		if( this.sectors[ tmpIdx ] == null ) {
		  loadFileIntoSectors( file, blockNums );
		  break;
		}
		tmpIdx++;
	      }
	    }
	  }
	}
      }
    }
  }


  private String extractEntryName( byte[] dirBytes, int entryBegPos )
  {
    String rv = null;
    int    b0 = (int) dirBytes[ entryBegPos ] & 0xFF;
    if( (b0 != 0xE5) && ((b0 & 0xF0) == 0) ) {
      char[] buf = new char[ 12 ];
      buf[ 0 ]   = (char) (b0 + '0');
      for( int i = 1; i < 12; i++ ) {
	buf[ i ] = (char) ((int) dirBytes[ entryBegPos + i ] & 0x7F);
      }
      rv = new String( buf );
    }
    return rv;
  }


  private int findEntryBegPosByNameAndExtent(
					String entryName,
					int    extentNum )
  {
    int rv = -1;
    if( (entryName != null) && (this.dirBytes != null) ) {
      if( entryName.length() == 12 ) {
	int pos = 0;
	while( (pos + 31) < this.dirBytes.length ) {
	  int b0 = (int) this.dirBytes[ pos ] & 0xFF;
	  if( (b0 != 0xE5) && ((b0 & 0xF0) == 0) ) {
	    int xNum = DiskUtil.getExtentNumByEntryPos( this.dirBytes, pos );
	    if( (xNum >= extentNum)
		&& (xNum < (extentNum + this.extentsPerDirEntry)) )
	    {
	      boolean found = false;
	      if( (b0 & 0x0F) == (entryName.charAt( 0 ) - '0') ) {
		found = true;
		for( int i = 1; i < 12; i++ ) {
		  if( ((int) this.dirBytes[ pos + i ] & 0x7F)
						!= entryName.charAt( i ) )
		  {
		    found = false;
		    break;
		  }
		}
	      }
	      if( found ) {
		rv = pos;
		break;
	      }
	    }
	  }
	  pos += 32;
	}
      }
    }
    return rv;
  }


  private String findEntryNameByDataBlock( int blockNum )
  {
    String entryName = null;
    if( this.dirBytes != null ) {
      int entryPos  = 0;
      while( (entryName == null)
	     && ((entryPos + 31) < this.dirBytes.length) )
      {
	int b0 = (int) this.dirBytes[ entryPos ] & 0xFF;
	if( (b0 != 0xE5) && ((b0 & 0xF0) == 0) ) {
	  int pos = entryPos + 16;
	  if( this.blockNum16Bit ) {
	    for( int i = 0; i < 8; i++ ) {
	      if( EmuUtil.getWord( this.dirBytes, pos ) == blockNum ) {
		entryName = extractEntryName( this.dirBytes, entryPos );
		break;
	      }
	      pos += 2;
	    }
	  } else {
	    for( int i = 0; i < 16; i++ ) {
	      if( ((int) this.dirBytes[ pos ] & 0xFF) == blockNum ) {
		entryName = extractEntryName( this.dirBytes, entryPos );
		break;
	      }
	      pos++;
	    }
	  }
	}
	entryPos += 32;
      }
    }
    return entryName;
  }


  private java.util.List<Integer> getBlockNumsByEntryName(
						String        entryName,
						AtomicInteger fileLen )
  {
    java.util.List<Integer> rv = null;
    if( (entryName != null) && (this.dirBytes != null) ) {
      if( fileLen != null ) {
	fileLen.set( -1 );
      }
      int baseExtentNum = 0;
      int entryPos      = findEntryBegPosByNameAndExtent(
						entryName,
						baseExtentNum );
      while( (entryPos >= 0) && (entryPos < this.dirBytes.length) ) {
	int len       = ((int) this.dirBytes[ entryPos + 15 ] & 0xFF) * 128;
	int extentNum = DiskUtil.getExtentNumByEntryPos(
						this.dirBytes,
						entryPos );
	if( (extentNum > baseExtentNum) && (this.extentsPerDirEntry > 1) ) {
	  int blocksPerExtent = (this.blockNum16Bit ? 8 : 16)
						/ this.extentsPerDirEntry;
	  len += (blocksPerExtent * this.blockSize);
	}
	int pos = entryPos + 16;
	if( this.blockNum16Bit ) {
	  for( int i = 0; i < 8; i++ ) {
	    int blockNum = EmuUtil.getWord( this.dirBytes, pos );
	    if( blockNum > 0 ) {
	      if( rv == null ) {
		rv = new ArrayList<>( 32 );
	      }
	      rv.add( blockNum );
	    }
	    pos += 2;
	  }
	} else {
	  for( int i = 0; i < 16; i++ ) {
	    int blockNum = (int) this.dirBytes[ pos ] & 0xFF;
	    if( blockNum > 0 ) {
	      if( rv == null ) {
		rv = new ArrayList<>( 32 );
	      }
	      rv.add( blockNum );
	      if( len == 0 ) {
		len = 0x100;
	      }
	    }
	    pos++;
	  }
	}
	if( fileLen != null ) {
	  if( fileLen.get() < 0 ) {
	    fileLen.set( len );
	  } else {
	    fileLen.addAndGet( len );
	  }
	}
	baseExtentNum += this.extentsPerDirEntry;
	entryPos = findEntryBegPosByNameAndExtent( entryName, baseExtentNum );
      }
    }
    return rv;
  }


  private static boolean isInSameMinute( Long minuteMillis, Long exactMillis )
  {
    boolean rv = false;
    if( (minuteMillis != null) && (exactMillis != null) ) {
      if( (exactMillis >= minuteMillis)
	  && (exactMillis < (minuteMillis + 60000L)) )
      {
	rv = true;
      }
    }
    return rv;
  }


  private void loadFileIntoSectors(
			File                    file,
			java.util.List<Integer> blockNums )
  {
    byte[] fileBytes = readFile( file );
    if( blockNums != null ) {
      int sectorSize = getSectorSize();
      if( sectorSize > 0 ) {
	int filePos = 0;
	for( Integer blockNum : blockNums ) {
	  if( blockNum >= this.dirBlocks ) {
	    for( int i = 0; i < this.sectorsPerBlock; i++ ) {
	      setSectorData(
			this.sysSectors
				+ (blockNum * this.sectorsPerBlock)
				+ i,
			fileBytes,
			filePos,
			sectorSize,
			fileBytes == null );
	      filePos += sectorSize;
	    }
	  }
	}
      }
    }
  }


  private byte[] readFile( File file )
  {
    byte[] fileBytes = null;
    try {
      fileBytes = FileUtil.readFile( file, false, getDiskSize() );
    }
    catch( IOException ex ) {
      String fileName = file.getPath();
      if( fileName != null ) {
	if( !fileName.isEmpty() ) {
	  if( this.errorMap == null ) {
	    this.errorMap = new HashMap<>();
	  }
	  this.errorMap.put( fileName, ex );
	}
      }
      fireShowError(
		fileName + ": Lesen der zugrunde liegenden Datei"
			+ " fehlgeschlagen",
		ex );
    }
    return fileBytes;
  }


  private void rebuildDisk()
  {
    if( this.dirBytes == null ) {
      this.dirBytes      = new byte[ this.dirBlocks * this.blockSize ];
      this.maxDirEntries = this.dirBytes.length / 32;
    }
    Arrays.fill( this.dirBytes, (byte) 0xE5 );
    Arrays.fill( this.sectors, null );
    this.sysSectors = getSides() * this.sysTracks * getSectorsPerTrack();
    this.fileMap.clear();
    if( this.errorMap != null ) {
      this.errorMap.clear();
    }
    this.lastBuildMillis = System.currentTimeMillis();

    int extentsPerDirEntry = DiskUtil.getExtentsPerDirEntry(
							blockSize,
							blockNum16Bit );

    // Dateien ermitteln, die in der emulierten Disketten enthalten sind
    Map<String,File> fileMap     = new HashMap<>();
    DateStamper      dateStamper = null;
    boolean          dirFull     = false;
    if( this.dsEnabled ) {
      fileMap.put( "0" + DateStamper.ENTRYNAME, null );
      dateStamper = new DateStamper(
				this.dirBlocks * this.blockSize / 32,
				this.dsBytes );
    }
    for( int userNum = 0; !dirFull && (userNum < 16); userNum++ ) {
      File dirFile = this.dirFile;
      if( userNum > 0 ) {
	dirFile = new File( dirFile, Integer.toString( userNum ) );
      }
      File[] files = dirFile.listFiles();
      if( files != null ) {
	for( int i = 0; i < files.length; i++ ) {
	  File file = files[ i ];
	  if( file != null ) {
	    if( file.isFile() && file.canRead()
		&& !file.equals( this.sysFile ) )
	    {
	      String fName = files[ i ].getName();
	      if( fName != null ) {
		String fName2 = fName.trim().toUpperCase();
		if( fName2 != null ) {
		  int len = fName2.length();
		  if( (!this.dsEnabled
				|| !fName.equals( DateStamper.FILENAME ))
		      &&(len > 0) && (len < 13)
		      && (len == fName.length()) )
		  {
		    StringBuilder buf = new StringBuilder( 12 );
		    buf.append( (char) (userNum + '0') );
		    int     nChars = 1;
		    boolean ignore = false;
		    boolean point  = false;
		    for( int k = 0; k < len; k++ ) {
		      char ch = fName2.charAt( k );
		      if( DiskUtil.isValidCPMFileNameChar( ch ) ) {
			int n = buf.length();
			if( (!point && (nChars < 9))
			    || (point && (nChars < 12)) )
			{
			  buf.append( ch );
			  nChars++;
			} else {
			  ignore = true;
			  break;
			}
		      } else if( ch == '.' ) {
			if( nChars > 0 ) {
			  while( nChars < 9 ) {
			    buf.append( '\u0020' );
			    nChars++;
			  }
			  point = true;
			} else {
			  ignore = true;
			  break;
			}
		      } else {
			ignore = true;
			break;
		      }
		    }
		    if( !ignore && (nChars > 1) ) {
		      while( nChars < 12 ) {
			buf.append( '\u0020' );
			nChars++;
		      }
		      String entryName = buf.toString();
		      if( fileMap.containsKey( entryName ) ) {
			/*
			 * Falls mehrere Dateien existieren und deren
			 * grossgeschriebene Namen sich nicht unterscheiden,
			 * werden diese Dateien ignoriert.
			 */
			fileMap.put( entryName, null );
		      } else {
			fileMap.put( entryName, file );
			if( fileMap.size() >= this.maxDirEntries ) {
			  dirFull = true;
			}
		      }
		    }
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    String[]           entryNames = null;
    Collection<String> tmpNames   = fileMap.keySet();
    if( tmpNames != null ) {
      int n = tmpNames.size();
      if( n > 0 ) {
	entryNames = tmpNames.toArray( new String[ n ] );
      }
    }
    if( entryNames != null ) {
      if( entryNameComparator == null ) {
	entryNameComparator = new EntryNameComparator();
      }
      try {
	Arrays.sort( entryNames, entryNameComparator );
      }
      catch( ClassCastException ex ) {}
      int firstDsBlkIdx  = -1;
      int dirIdx         = 0;
      int blkIdx         = this.dirBlocks;
      int nRemainBlocks  = (getDiskSize() / this.blockSize) - blkIdx;
      int maxFileEntries = Math.max( 32 / extentsPerDirEntry, 1 );
      int maxEntrySize   = (this.blockNum16Bit ? 8 : 16) * this.blockSize;
      int maxEntrySegs   = maxEntrySize / 128;
      for( int i = 0; i < entryNames.length; i++ ) {
	String entryName = entryNames[ i ];
	if( entryName != null ) {
	  File    file     = null;
	  long    fSize    = -1;
	  boolean writable = false;
	  if( this.dsEnabled
	      && entryName.equals( "0" + DateStamper.ENTRYNAME ) )
	  {
	    fSize         = maxDirEntries * 16;
	    writable      = true;
	    firstDsBlkIdx = blkIdx;
	  } else {
	    file = fileMap.get( entryName );
	    if( file != null ) {
	      fSize    = file.length();
	      writable = file.canWrite();
	    }
	  }
	  if( fSize >= 0 ) {
	    long nEntries = (fSize + maxEntrySize - 1) / maxEntrySize;
	    if( nEntries < 1 ) {
	      nEntries = 1;
	    }
	    if( ((dirIdx + (nEntries * 32)) <= this.dirBytes.length)
		&& (nEntries <= maxFileEntries) )
	    {
	      long nBlocks = (fSize + this.blockSize - 1) / this.blockSize;
	      if( nBlocks <= nRemainBlocks ) {
		int baseExtentNum = 0;
		for( int k = 0; k < nEntries; k++ ) {
		  int roAttrPos = dirIdx + 9;
		  int len       = entryName.length();
		  if( len > 0 ) {
		    this.dirBytes[ dirIdx++ ] =
			      (byte) (entryName.charAt( 0 ) - '0');
		  } else {
		    this.dirBytes[ dirIdx++ ] = (byte) 0;
		  }
		  for( int p = 1; p < 12; p++ ) {
		    char ch = '\u0020';
		    if( p < len ) {
		      ch = entryName.charAt( p );
		    }
		    this.dirBytes[ dirIdx++ ] = (byte) (ch & 0x7F);
		  }
		  if( !writable ) {
		    this.dirBytes[ roAttrPos ] |= 0x80;
		  }
		  int nEntrySegs = 0;
		  if( fSize > 0 ) {
		    nEntrySegs = Math.min(
					(int) ((fSize + 127) / 128),
					maxEntrySegs );
		  }
		  int nExtentSegs = nEntrySegs % 128;
		  if( (nEntrySegs > 0) && (nExtentSegs == 0) ) {
		    nExtentSegs = 128;
		  }
		  int nEntryExtents = (nEntrySegs + 127) / 128;
		  int tmpExtentNum  = baseExtentNum;
		  if( (nEntryExtents > 1)
		      && (nEntryExtents <= extentsPerDirEntry) )
		  {
		    tmpExtentNum = baseExtentNum + nEntryExtents - 1;
		  }
		  this.dirBytes[ dirIdx++ ] = (byte) tmpExtentNum;
		  this.dirBytes[ dirIdx++ ] = (byte) 0;
		  this.dirBytes[ dirIdx++ ] = (byte) 0;
		  nEntrySegs %= 128;
		  if( (fSize > 0) && (nEntrySegs == 0) ) {
		    nEntrySegs = 128;
		  }
		  this.dirBytes[ dirIdx++ ] = (byte) nEntrySegs;
		  if( this.blockNum16Bit ) {
		    for( int m = 0; m < 8; m++ ) {
		      if( nBlocks > 0 ) {
			this.dirBytes[ dirIdx++ ] = (byte) blkIdx;
			this.dirBytes[ dirIdx++ ] = (byte) (blkIdx >> 8);
			blkIdx++;
			--nBlocks;
			--nRemainBlocks;
			fSize -= this.blockSize;
		      } else {
			this.dirBytes[ dirIdx++ ] = (byte) 0;
			this.dirBytes[ dirIdx++ ] = (byte) 0;
		      }
		    }
		  } else {
		    for( int m = 0; m < 16; m++ ) {
		      if( nBlocks > 0 ) {
			this.dirBytes[ dirIdx++ ] = (byte) blkIdx++;
			--nBlocks;
			--nRemainBlocks;
			fSize -= this.blockSize;
		      } else {
			this.dirBytes[ dirIdx++ ] = (byte) 0;
		      }
		    }
		  }
		  if( dateStamper != null ) {
		    dateStamper.addFileTimes( file );
		  }
		  baseExtentNum += extentsPerDirEntry;
		}
	      }
	    }
	    this.fileMap.put( entryName, file );
	  }
	}
      }

      // DateStamper-Sektoren eintragen
      if( (dateStamper != null) && (firstDsBlkIdx >= 0) ) {
	this.dsBytes   = dateStamper.getDateTimeByteBuffer();
	int sectorSize = getSectorSize();
	if( (this.dsBytes != null) && (sectorSize > 0) ) {
	  int srcPos     = 0;
	  int absSectIdx = this.sysSectors
			      + (firstDsBlkIdx * this.sectorsPerBlock);
	  this.dsFirstSector = absSectIdx;
	  this.dsSectors     = dsBytes.length / sectorSize;
	  while( srcPos < this.dsBytes.length ) {
	    setSectorData(
			absSectIdx++,
			this.dsBytes,
			srcPos,
			sectorSize,
			false );
	    srcPos += sectorSize;
	  }
	}
      }
    }

    // Directory-Sektoren eintragen
    int sectorSize = getSectorSize();
    if( sectorSize > 0 ) {
      int absSectIdx = this.sysSectors;
      int srcPos     = 0;
      while( srcPos < this.dirBytes.length ) {
	setSectorData(
		absSectIdx++,
		this.dirBytes,
		srcPos,
		sectorSize,
		false );
	srcPos += sectorSize;
      }
    }
  }


  private void setSectorData(
			int     absSectIdx,
			byte[]  dataBuf,
			int     dataOffs,
			int     sectorSize,
			boolean err )
  {
    if( (absSectIdx >= 0) && (absSectIdx < this.sectors.length) ) {
      int sides        = getSides();
      int sectPerTrack = getSectorsPerTrack();
      if( (sides > 0) && (sectPerTrack > 0) && (sectorSize > 0) ) {
	int cyl = absSectIdx / (sides * sectPerTrack);
	if( (cyl >= 0) && (cyl < getCylinders()) ) {
	  int head = 0;
	  int idx  = absSectIdx - (cyl * sides * sectPerTrack);
	  if( idx >= sectPerTrack ) {
	    head++;
	    idx -= sectPerTrack;
	  }
	  if( (head < sides) && (idx >= 0) && (idx < sectPerTrack) ) {
	    this.sectors[ absSectIdx ] = new SectorData(
						idx,
						cyl,
						head,
						idx + 1,
						this.sectorSizeCode,
						dataBuf,
						dataOffs,
						sectorSize );
	    this.sectors[ absSectIdx ].setError( err );
	  }
	}
      }
    }
  }


  private void writeDirSector(
			int    absSectorIdx,
			byte[] dataBuf,
			int    dataLen ) throws IOException
  {
    if( this.dirBytes != null ) {
      int dirPos = (absSectorIdx - this.sysSectors) * getSectorSize();
      if( dirPos >= 0 ) {

	// zwischen alt und neu unterschiedliche Eintraege ermitteln
	Set<String> oldEntryNames = null;
	Set<String> newEntryNames = null;
	int         sectorPos     = 0;
	int         bufPos        = dirPos;
	while( ((bufPos + 31) < this.dirBytes.length)
	       && ((sectorPos + 31) < dataLen) )
	{
	  boolean entryDiffers = false;
	  for( int i = 0; i < 32; i++ ) {
	    byte b1 = dataBuf[ sectorPos + i ];
	    byte b2 = this.dirBytes[ bufPos + i ];
	    if( (i >= 1) && (i <= 11) ) {
	      b1 &= 0x7F;
	      b2 &= 0x7F;
	    }
	    if( b1 != b2 ) {
	      entryDiffers = true;
	      break;
	    }
	  }
	  String entryName = extractEntryName( dataBuf, sectorPos );
	  if( entryDiffers ) {
	    if( entryName != null ) {
	      if( newEntryNames == null ) {
		newEntryNames = new TreeSet<>();
	      }
	      newEntryNames.add( entryName );
	    }
	    /*
	     * den alten Namen in dem Directory-Eintrag merken,
	     * um beim Verschwinden von diesem die Datei zu loeschen,
	     * Eine Datei kann nur dann geloscht werden,
	     * wenn es den Eintrag fuer Extent 0 nicht mehr gibt.
	     * Des Weiteren auch die alte Datei laden,
	     * damit ein evtl. Umbenennen funktioniert.
	     */
	    if( (this.dirBytes[ 12 ] == 0)
		&& (this.dirBytes[ 14 ] == 0) )
	    {
	      String oldEntryName = extractEntryName( this.dirBytes, bufPos );
	      if( oldEntryName != null ) {
		if( oldEntryNames == null ) {
		  oldEntryNames = new TreeSet<>();
		}
		oldEntryNames.add( oldEntryName );
		ensureFileLoaded( oldEntryName );
	      }
	    }
	  } else {
	    if( (dataBuf[ sectorPos + 12 ] == 0)
		&& (dataBuf[ sectorPos + 14 ] == 0) )
	    {
	      byte b = dataBuf[ sectorPos + 9 ];
	      if( b != this.dirBytes[ bufPos + 9 ] ) {
		// nur das ReadOnly-Bit ist unterschiedlich
		File file = this.fileMap.get( entryName );
		if( file != null ) {
		  FileUtil.setFileWritable( file, ((int) b & 0x80) == 0 );
		}
	      }
	    }
	  }
	  bufPos += 32;
	  sectorPos += 32;
	}

	/*
	 * sicherstellen, dass die betroffenen Dateien vollstaendig
	 * geladen wurden
	 */
	if( newEntryNames != null ) {
	  for( String entryName : newEntryNames ) {
	    ensureFileLoaded( entryName );
	    if( oldEntryNames != null ) {
	      oldEntryNames.remove( entryName );
	    }
	  }
	}

	// Sektor in Directory schreiben
	System.arraycopy(
		dataBuf,
		0,
		this.dirBytes,
		dirPos,
		Math.min( this.dirBytes.length - dirPos, dataLen ) );

	// betroffene Dateien schreiben
	if( newEntryNames != null ) {
	  for( String entryName : newEntryNames ) {
	    writeFileByEntryName(
			entryName,
			this.fileMap.get( entryName ) );
	  }
	}

	// ggf. Dateien loeschen
	if( oldEntryNames != null ) {
	  for( String entryName : oldEntryNames ) {
	    File file = this.fileMap.get( entryName );
	    if( file != null ) {
	      if( findEntryBegPosByNameAndExtent( entryName, 0 ) < 0 ) {
		if( file.exists() ) {
		  /*
		   * aicherstellen, dass die Datei vollstaendig geladen
		   * wurde, damit ggf. ein anschliessendes "undelete"
		   * funktionieren kann
		   */
		  ensureFileLoaded( entryName );
		  if( !file.delete() ) {
		    throw new IOException( file.getPath()
			+ ":\nDatei konnte nicht gel\u00F6scht werden" );
		  }
		}
	      }
	    }
	  }
	}
      }
    }
  }


  private void writeDsSector(
			int    absSectorIdx,
			byte[] dataBuf,
			int    dataLen ) throws IOException
  {
    byte[] oldDsBytes     = this.dsBytes;
    int    dsFirstSectopr = this.dsFirstSector;
    if( this.dsEnabled && (oldDsBytes != null) && (dsFirstSector >= 0) ) {
      int sectorSize = getSectorSize();
      int oldIdx     = (absSectorIdx - dsFirstSector) * sectorSize;
      if( (sectorSize > 0 ) && (oldIdx >= 0) ) {
	int newIdx = 0;
	int dirIdx = oldIdx / 16 * 32;
	while( ((newIdx + 15) < dataLen)
	       && ((oldIdx + 15)< oldDsBytes.length) )
	{
	  // Zeitstempeleintraege nur bei Extent=0 auswerten
	  if( (dirIdx + 31) < this.dirBytes.length ) {
	    if( (((int) this.dirBytes[ dirIdx + 12 ] & 0x1F) == 0)
		&& (((int) this.dirBytes[ dirIdx + 14 ] & 0x3F) == 0) )
	    {
	      Long creationMillis     = null;
	      Long lastAccessMillis   = null;
	      Long lastModifiedMillis = null;
	      for( int i = 0; i < 5; i++ ) {
		if( dataBuf[ newIdx + i ] != oldDsBytes[ oldIdx + i ] ) {
		  creationMillis = DateStamper.getMillis( dataBuf, newIdx );
		  break;
		}
	      }
	      for( int i = 5; i < 10; i++ ) {
		if( dataBuf[ newIdx + i ] != oldDsBytes[ oldIdx + i ] ) {
		  lastAccessMillis = DateStamper.getMillis(
							dataBuf,
							newIdx + 5 );
		  break;
		}
	      }
	      for( int i = 10; i < 15; i++ ) {
		if( dataBuf[ newIdx + i ] != oldDsBytes[ oldIdx + i ] ) {
		  lastModifiedMillis = DateStamper.getMillis(
							dataBuf,
							newIdx + 10 );
		  break;
		}
	      }
	      if( (creationMillis != null)
		  || (lastAccessMillis != null)
		  || (lastModifiedMillis != null) )
	      {
		String entryName = extractEntryName( this.dirBytes, dirIdx );
		if( entryName != null ) {
		  File file = this.fileMap.get( entryName );
		  if( file != null ) {
		    /*
		     * Wenn ein zu setzender Zeitstempel
		     * der ja nur minutengenau ist,
		     * in der gleichen Minute liegt wie der bereits
		     * vorhandene Dateizeitstempel,
		     * dann soll der Zeitstempel nicht gesetzt werden,
		     * um die Sekunden nicht zu loeschen.
		     */
		    FileTimesData ftd = FileTimesData.createOf( file );
		    if( isInSameMinute(
				creationMillis,
				ftd.getCreationMillis() ) )
		    {
		      creationMillis = null;
		    }
		    if( isInSameMinute(
				lastAccessMillis,
				ftd.getLastAccessMillis() ) )
		    {
		      lastAccessMillis = null;
		    }
		    if( isInSameMinute(
				lastModifiedMillis,
				ftd.getLastModifiedMillis() ) )
		    {
		      lastModifiedMillis = null;
		    }
		    if( (creationMillis != null)
			|| (lastAccessMillis != null)
			|| (lastModifiedMillis != null) )
		    {
		      ftd.setTimesInMillis(
					creationMillis,
					lastAccessMillis,
					lastModifiedMillis );
		    }
		  }
		}
	      }
	    }
	  }
	  System.arraycopy( dataBuf, newIdx, this.dsBytes, oldIdx, 16 );
	  newIdx += 16;
	  oldIdx += 16;
	  dirIdx += 32;
	}
      }
    }
  }


  private void writeFileByEntryName(
			String entryName,
			File   file ) throws IOException
  {
    if( (entryName != null)
	&& (this.dirBytes != null)
	&& (this.sectors != null) )
    {
      int entryNameLen = entryName.length();
      if( entryNameLen == 12 ) {
	if( file == null ) {
	  /*
	   * neue Datei anlegen
	   *
	   * Es sollen keine neuen Dateien angelegt werden,
	   * wenn der Dateiname Nicht-ASCII-Zeichen enthaelt.
	   * Dies ist insbesondere auch ein begrenzter Schutz fuer den Fall,
	   * dass die eingestellte Verzeichnisgroesse groesser ist als die,
	   * mit der das im Emulator laufende Betriebssystem arbeitet.
	   */
	  for( int i = 1; i < entryNameLen; i++ ) {
	    /*
	     * Eine Pruefung auf einen Wert groesser 7Fh ist nicht noetig,
	     * da die Eintraege sowieso nur aus 7-Bit-Zeichen bestehen
	     * und deshalb das 8. Bit bereits ausgeblendet wurde.
	     */
	    if( entryName.charAt( i ) < '\u0020' ) {
	      throw new IOException(
		"Anlegen der Datei nicht m\u00F6glich, da der Dateiname"
			+ " nicht konforme Zeichen enthalten w\u00FCrde.\n\n"
			+ "M\u00F6glicherweise stimmt aber auch die"
			+ " beim Diskettenformat eingestellte"
			+ " Directory-Gr\u00F6\u00DFe"
			+ " (Anzahl der Bl\u00F6cke)\n"
			+ "nicht mit der \u00FCberein,"
			+ " mit der das im Emulator laufende Programm"
			+ " bzw. Betriebssystem arbeitet." );
	    }
	  }
	  String fileName = null;
	  String tmpName  = entryName.substring( 1 ).trim();
	  if( tmpName.isEmpty() ) {
	    fileName = "@noname";
	  } else if( tmpName.equals( "." ) ) {
	    fileName = "@dot";
	  } else if( tmpName.equals( ".." ) ) {
	    fileName = "@dotdot";
	  } else {
	    fileName = String.format(
			"%s.%s",
			entryName.substring( 1, 9 ).trim(),
			entryName.substring( 9 ).trim() );
	  }
	  if( this.forceLowerCase ) {
	    fileName = fileName.toLowerCase();
	  }
	  int userNum = (entryName.charAt( 0 ) - '0');
	  if( (userNum == 0) && fileName.equals( SYS_FILE_NAME ) ) {
	    throw new IOException(
		"Eine Datei mit dem Namen " + SYS_FILE_NAME
			+ " kann vom emulierten System aus nicht angelegt"
			+ "  werden,\n"
			+ "da JKCEMU diese Datei f\u00FCr die Systemspuren"
			+ " verwendet." );
	  }
	  File dirFile = this.dirFile;
	  if( userNum > 0 ) {
	    dirFile = new File( dirFile, Integer.toString( userNum ) );
	    dirFile.mkdir();
	  }
	  file = new File( dirFile, fileName );
	  this.fileMap.put( entryName, file );
	}
	if( this.errorMap != null ) {
	  String fileName = file.getPath();
	  if( fileName != null ) {
	    Exception ex = this.errorMap.get( fileName );
	    if( ex != null ) {
	      throw new IOException( ex.getMessage() );
	    }
	  }
	}
	RandomAccessFile raf = null;
	try {
	  raf = new RandomAccessFile( file, "rw" );

	  AtomicInteger           fileLen   = new AtomicInteger( -1 );
	  java.util.List<Integer> blockNums = getBlockNumsByEntryName(
								entryName,
								fileLen );
	  if( blockNums != null ) {
	    int sectorSize = getSectorSize();
	    int filePos    = 0;
	    for( Integer blockNum : blockNums ) {
	      int absSectorIdx = this.sysSectors
					  + (blockNum * this.sectorsPerBlock);
	      for( int i = 0; i < this.sectorsPerBlock; i++ ) {
		if( (absSectorIdx >= 0)
		    && (absSectorIdx < this.sectors.length) )
		{
		  if( this.sectors[ absSectorIdx ] != null ) {
		    raf.seek( filePos );
		    this.sectors[ absSectorIdx ].writeTo( raf, sectorSize );
		  }
		}
		filePos += sectorSize;
		absSectorIdx++;
	      }
	    }
	  }
	  if( fileLen.get() >= 0 ) {
	    raf.setLength( fileLen.get() );
	  }
	  raf.close();
	  raf = null;
	}
	finally {
	  EmuUtil.closeSilently( raf );
	}
      }
    }
  }
}
