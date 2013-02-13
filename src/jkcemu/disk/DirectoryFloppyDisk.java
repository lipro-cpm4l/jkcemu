/*
 * (c) 2009-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation einer Diskette,
 * die ein Verzeichnis im Dateisystem des Host-Systems abbildet
 */

package jkcemu.disk;

import java.awt.Frame;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import jkcemu.base.EmuUtil;


public class DirectoryFloppyDisk extends AbstractFloppyDisk
{
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
      boolean at1 = s1.startsWith( "\u0000@" );
      boolean at2 = s2.startsWith( "\u0000@" );
      if( at1 && !at2 ) {
	rv = -1;
      } else if( !at1 && at2 ) {
	rv = 1;
      } else {
	rv = s1.compareTo( s2 );
      }
      return rv;
    }

    public boolean equals( Object o )
    {
      return o == this;
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
  private int                   sysCyls;
  private int                   sysSectors;
  private int                   blockSize;
  private boolean               blockNum16Bit;
  private boolean               autoRefresh;
  private boolean               forceLowerCase;
  private boolean               readOnly;
  private String                remark;
  private volatile boolean      refreshFired;


  public DirectoryFloppyDisk(
			Frame   owner,
			int     sides,
			int     cyls,
			int     sectorsPerCyl,
			int     sectorSize,
			int     sysCyls,
			int     blockSize,
			boolean blockNum16Bit,
			int     dirBlocks,
			File    dirFile,
			boolean autoRefresh,
			boolean readOnly,
			boolean forceLowerCase )
  {
    super( owner, sides, cyls, sectorsPerCyl, sectorSize );
    this.dirFile         = dirFile;
    this.sysFile         = new File( this.dirFile, SYS_FILE_NAME );
    this.sysCyls         = sysCyls;
    this.sysSectors      = sysCyls * sides * sectorsPerCyl;
    this.blockSize       = blockSize;
    this.blockNum16Bit   = blockNum16Bit;
    this.sectorsPerBlock = blockSize / sectorSize;
    this.dirBlocks       = dirBlocks;
    this.dirSectors      = dirBlocks * this.sectorsPerBlock;
    this.sectorSizeCode  = SectorData.getSizeCode( sectorSize );
    this.autoRefresh     = readOnly ? autoRefresh : false;
    this.readOnly        = readOnly;
    this.forceLowerCase  = forceLowerCase;
    this.lastBuildMillis = -1L;
    this.fileMap         = new HashMap<String,File>();
    this.errorMap        = null;;
    this.dirBytes        = null;
    this.maxDirEntries   = 0;
    this.sectors         = new SectorData[ sides * cyls * sectorsPerCyl ];
    this.refreshFired    = false;
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


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getFileFormatText()
  {
    return null;
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
       * Wenn eine Aktualisierung angefordert wurde
       * oder AutoRefresh aktiviert, wird beim naechsten lesenden Zugriff
       * auf den ersten Sektor der Diskette oder des Directories
       * die virtuelle Diskette neu erzeugt, bei AutoRefresh aber nur,
       * wenn die letzte Aktualisierung mehr als 10 Sekunden zurueckliegt.
       */
      if( ((physCyl == 0) || (physCyl == this.sysCyls))
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
		  || (this.lastBuildMillis < (curMillis - 10000))) )
	  {
	    rebuildDisk();
	  }
	}
      }
    }
    int sectPerCyl = getSectorsPerCylinder();
    int sectorSize = getSectorSize();
    if( (physCyl >= 0)
	&& (sectPerCyl > 0)
	&& (sectorIdx >= 0) && (sectorIdx < sectPerCyl) )
    {
      int absSectIdx = computeAbsSectorIdx( physCyl, physHead, sectorIdx );
      if( (absSectIdx >= 0) && (absSectIdx < this.sectors.length) ) {
	rv = this.sectors[ absSectIdx ];
	if( rv == null ) {
	  if( physCyl < this.sysCyls ) {
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

	      // betreffene Datei ermitteln, lesen und den Sektoren zuordnen
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
  protected int getSystemCylinders()
  {
    return this.sysCyls;
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
      props.setProperty( prefix + "directory", this.dirFile.getPath() );
      props.setProperty(
		prefix + "system_tracks",
		Integer.toString( this.sysCyls ) );
      props.setProperty(
		prefix + "block_size",
		Integer.toString( this.blockSize ) );
      props.setProperty(
		prefix + "block_number_size",
		this.blockNum16Bit ? "16" : "8" );
      props.setProperty(
		prefix + "dir_blocks",
		Integer.toString( this.dirBlocks ) );
      props.setProperty(
		prefix + "auto_refresh",
		Boolean.toString( this.autoRefresh ) );
      props.setProperty(
		prefix + "readonly",
		Boolean.toString( isReadOnly() ) );
      props.setProperty(
		prefix + "force_lowercase",
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
	  else if( absSectorIdx >= (this.sysSectors + this.dirSectors) ) {
	    // Datenbereich
	    int relSectorIdx = absSectorIdx - this.sysSectors;
	    if( relSectorIdx >= 0 ) {
	      String entryName = findEntryNameByDataBlock(
					relSectorIdx / this.sectorsPerBlock );
	      if( entryName != null ) {
		File file = this.fileMap.get( entryName );
		if( file != null ) {
		  raf = new RandomAccessFile( file, "rw" );
		  raf.seek( absSectorIdx * getSectorSize() );
		  raf.write( dataBuf, 0, Math.min( dataLen, sectorSize ) );
		  raf.close();
		  raf = null;
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
	  EmuUtil.doClose( raf );
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
    int absSectIdx = -1;
    int sectPerCyl = getSectorsPerCylinder();
    if( (physCyl >= 0)
	&& (sectPerCyl > 0)
	&& (sectorIdx >= 0) && (sectorIdx < sectPerCyl) )
    {
      absSectIdx = physCyl * getSides() * sectPerCyl;
      if( (physHead & 0x01) != 0 ) {
	absSectIdx += sectPerCyl;
      }
      absSectIdx += sectorIdx;
    }
    return absSectIdx;
  }


  private void ensureFileLoaded(String entryName )
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
      buf[ 0 ]   = (char) b0;
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
	  if( (b0 != 0xE5) && ((b0 & 0xF0) == 0)
	      && (((int) this.dirBytes[ pos + 12 ] & 0xFF)
						== (extentNum & 0x1F))
	      && (((int) this.dirBytes[ pos + 14 ] & 0xFF)
						== ((extentNum >> 5) & 0x3F)) )
	  {
	    boolean found = false;
	    if( (b0 & 0x0F) == entryName.charAt( 0 ) ) {
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
      int extentNum = 0;
      int entryPos  = findEntryBegPosByNameAndExtent( entryName, extentNum );
      while( (entryPos >= 0) && (entryPos < this.dirBytes.length) ) {
	int len = ((int) this.dirBytes[ entryPos + 15 ] & 0xFF) * 128;
	int pos = entryPos + 16;
	if( this.blockNum16Bit ) {
	  for( int i = 0; i < 8; i++ ) {
	    int blockNum = EmuUtil.getWord( this.dirBytes, pos );
	    if( blockNum > 0 ) {
	      if( rv == null ) {
		rv = new ArrayList<Integer>( 32 );
	      }
	      rv.add( new Integer( blockNum ) );
	    }
	    pos += 2;
	  }
	} else {
	  for( int i = 0; i < 16; i++ ) {
	    int blockNum = (int) this.dirBytes[ pos ] & 0xFF;
	    if( blockNum > 0 ) {
	      if( rv == null ) {
		rv = new ArrayList<Integer>( 32 );
	      }
	      rv.add( new Integer( blockNum ) );
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
	extentNum++;
	entryPos = findEntryBegPosByNameAndExtent( entryName, extentNum );
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
      fileBytes = EmuUtil.readFile( file, getDiskSize() );
    }
    catch( IOException ex ) {
      String fileName = file.getPath();
      if( fileName != null ) {
	if( !fileName.isEmpty() ) {
	  if( this.errorMap == null ) {
	    this.errorMap = new HashMap<String,Exception>();
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
    this.sysSectors = getSides() * this.sysCyls * getSectorsPerCylinder();
    this.fileMap.clear();
    if( this.errorMap != null ) {
      this.errorMap.clear();
    }
    this.lastBuildMillis = System.currentTimeMillis();
    try {
      Map<String,File> fileMap = null;
      boolean          dirFull = false;
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
		    if( (len > 0) && (len < 13) && (len == fName.length()) ) {
		      StringBuilder buf = new StringBuilder( 12 );
		      buf.append( (char) userNum );
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
			      buf.append( (char) '\u0020' );
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
			  buf.append( (char) '\u0020' );
			  nChars++;
			}
			String entryName = buf.toString();
			if( fileMap == null ) {
			  fileMap = new HashMap<String,File>();
			}
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
	int dirIdx         = 0;
	int blkIdx         = this.dirBlocks;
	int nRemainBlocks  = (getDiskSize() / this.blockSize) - blkIdx;
	int maxFileEntries = (this.blockNum16Bit ? 32 : 1);
	int maxEntrySize   = (this.blockNum16Bit ? 8 : 16) * this.blockSize;
	if( maxEntrySize > (0xFF * 128) ) {
	  maxEntrySize   = 0xFF * 128;
	  maxFileEntries = 1;
	}
	for( int i = 0; i < entryNames.length; i++ ) {
	  String entryName = entryNames[ i ];
	  if( entryName != null ) {
	    File file = fileMap.get( entryName );
	    if( file != null ) {
	      long fSize = file.length();
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
		    for( int k = 0; k < nEntries; k++ ) {
		      int roAttrPos = dirIdx + 9;
		      int len       = entryName.length();
		      if( len > 0 ) {
			this.dirBytes[ dirIdx++ ] =
					(byte) entryName.charAt( 0 );
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
		      if( !file.canWrite() ) {
			this.dirBytes[ roAttrPos ] |= 0x80;
		      }
		      this.dirBytes[ dirIdx++ ] = (byte) k;
		      this.dirBytes[ dirIdx++ ] = (byte) 0;
		      this.dirBytes[ dirIdx++ ] = (byte) 0;
		      long nSegs = 0;
		      if( fSize > 0 ) {
			nSegs = (Math.min( fSize, maxEntrySize ) + 127) / 128;
		      }
		      this.dirBytes[ dirIdx++ ] = (byte) nSegs;
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
		    }
		    this.fileMap.put( entryName, file );
		  }
		}
	      }
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {}

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
      int sides      = getSides();
      int sectPerCyl = getSectorsPerCylinder();
      if( (sides > 0) && (sectPerCyl > 0) && (sectorSize > 0) ) {
	int cyl = absSectIdx / (sides * sectPerCyl);
	if( (cyl >= 0) && (cyl < getCylinders()) ) {
	  int head = 0;
	  int idx  = absSectIdx - (cyl * sides * sectPerCyl);
	  if( idx >= sectPerCyl ) {
	    head++;
	    idx -= sectPerCyl;
	  }
	  if( (head < sides) && (idx >= 0) && (idx < sectPerCyl) ) {
	    this.sectors[ absSectIdx ] = new SectorData(
						idx,
						cyl,
						head,
						idx + 1,
						this.sectorSizeCode,
						err,
						false,
						dataBuf,
						dataOffs,
						sectorSize );
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
		newEntryNames = new TreeSet<String>();
	      }
	      newEntryNames.add( entryName );
	    }
	    /*
	     * den alten Namen in dem Directory-Eintrag merken,
	     * um beim Verschwinden von diesem die Datei zu loeschen,
	     * Eine Datei kann nur dann geloscht werden,
	     * wenn es den Eintrag fuer Extent 0 nicht mehr gibt.
	     */
	    if( (this.dirBytes[ 12 ] == 0)
		&& (this.dirBytes[ 14 ] == 0) )
	    {
	      String oldEntryName = extractEntryName( this.dirBytes, bufPos );
	      if( oldEntryName != null ) {
		if( oldEntryNames == null ) {
		  oldEntryNames = new TreeSet<String>();
		}
		oldEntryNames.add( oldEntryName );
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
		  EmuUtil.setFileWritable( file, ((int) b & 0x80) == 0 );
		}
	      }
	    }
	  }
	  bufPos += 32;
	  sectorPos += 32;
	}

	/*
	 * sicherstellen, dass die betreffenden Dateien vollstaendig
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

	// betreffende Dateien schreiben
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
		   * Sicherstellen, dass die Datei vollstaendig geladen
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
	     * Eine Pruefung auf einen Wert groesser 7Fh ist noetig,
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
	  int userNum = entryName.charAt( 0 );
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
	  EmuUtil.doClose( raf );
	}
      }
    }
  }
}

