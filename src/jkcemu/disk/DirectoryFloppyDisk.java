/*
 * (c) 2009-2010 Jens Mueller
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
import jkcemu.base.EmuUtil;


public class DirectoryFloppyDisk extends AbstractFloppyDisk
{
  private static final String SYS_FILE_NAME = "@boot.sys";

  private File             dirFile;
  private File             sysFile;
  private long             lastBuildMillis;
  private Map<String,File> fileMap;
  private byte[]           dirBytes;
  private SectorData[]     sectors;
  private int              sectorSizeCode;
  private int              dirBlocks;
  private int              sysCyls;
  private int              sysSectors;
  private int              blockSize;
  private boolean          blockNum16Bit;
  private boolean          autoRefresh;
  private String           remark;


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
			boolean autoRefresh )
  {
    super( owner, sides, cyls, sectorsPerCyl, sectorSize );
    this.dirFile         = dirFile;
    this.sysFile         = new File( this.dirFile, "@boot.sys" );
    this.sysCyls         = sysCyls;
    this.sysSectors      = sysCyls * sides * sectorsPerCyl;
    this.blockSize       = blockSize;
    this.blockNum16Bit   = blockNum16Bit;
    this.dirBlocks       = dirBlocks;
    this.sectorSizeCode  = SectorData.getSizeCode( sectorSize );
    this.autoRefresh     = autoRefresh;
    this.lastBuildMillis = -1L;
    this.fileMap         = new HashMap<String,File>();
    this.dirBytes        = null;
    this.sectors         = new SectorData[ sides * cyls * sectorsPerCyl ];
    if( this.autoRefresh ) {
      this.remark = "Automatische Aktualisierung: ein";
    } else {
      this.remark = "Automatische Aktualisierung: aus";
    }
    Arrays.fill( this.sectors, null );
  }


	/* --- ueberschriebene Methoden --- */

  public String getRemark()
  {
    return this.remark;
  }


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
       * Ist AutoRefresh aktiviert, wird bei Zugriff auf den ersten Sektor
       * der Diskette oder des Directories die virtuelle Diskette neu erzeugt,
       * wenn die letzte Generierung mehr als 10 Sekunden zurueckliegt.
       */
      if( this.autoRefresh
	  && ((physCyl == 0) || (physCyl == this.sysCyls))
	  && (physHead == 0)
	  && (sectorIdx == 0) )
      {
	long curMillis = System.currentTimeMillis();
	if( (curMillis != -1L)
	    && ((this.lastBuildMillis == -1L)
		|| (this.lastBuildMillis < (curMillis - 10000))) )
	{
	  rebuildDisk();
	}
      }
    }
    int sectPerCyl = getSectorsPerCylinder();
    int sectorSize = getSectorSize();
    if( (physCyl >= 0)
	&& (sectPerCyl > 0)
	&& (sectorIdx >= 0) && (sectorIdx < sectPerCyl) )
    {
      int absSectIdx = physCyl * getSides() * sectPerCyl;
      if( (physHead & 0x01) != 0 ) {
	absSectIdx += sectPerCyl;
      }
      absSectIdx += sectorIdx;
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
		  setSectorData( idx++, fileBytes, pos, false );
		  pos += sectorSize;
		}
	      }
	    }

	  } else {

	    // Block berechnen
	    int relSectorIdx = absSectIdx;
	    if( this.sysCyls > 0 ) {
	      relSectorIdx -= (this.sysCyls * getSides() * sectPerCyl);
	    }
	    int block = (relSectorIdx * sectorSize) / this.blockSize;
	    if( block > 0 ) {

	      // betreffene Datei ermitteln
	      String keyName = null;
	      int    dirPos  = 0;
	      while( (keyName == null)
		     && ((dirPos + 32) <= this.dirBytes.length) )
	      {
		int b = (int) this.dirBytes[ dirPos ] & 0xFF;
		if( (b >= 0) && (b <= 0x15) ) {
		  int pos = dirPos + 16;
		  if( this.blockNum16Bit ) {
		    for( int i = 0; i < 8; i++ ) {
		      if( EmuUtil.getWord( this.dirBytes, pos ) == block ) {
			StringBuilder buf = new StringBuilder( 11 );
			for( int k = 1; k < 12; k++ ) {
			  buf.append( (char)
				((int) this.dirBytes[ dirPos + k ] & 0x7F ) );
			}
			keyName = buf.toString();
			break;
		      }
		      pos += 2;
		    }
		  } else {
		    for( int i = 0; i < 16; i++ ) {
		      if( ((int) this.dirBytes[ pos ] & 0xFF) == block ) {
			StringBuilder buf = new StringBuilder( 11 );
			for( int k = 1; k < 12; k++ ) {
			  buf.append( (char)
				((int) this.dirBytes[ dirPos + k ] & 0x7F ) );
			}
			keyName = buf.toString();
			break;
		      }
		      pos++;
		    }
		  }
		}
		dirPos += 32;
	      }

	      // betreffene Datei lesen und den Sektoren zuordnen
	      if( keyName != null ) {
		File file = this.fileMap.get( keyName );
		if( file != null ) {
		  byte[] fileBytes = readFile( file );
		  dirPos           = 0;
		  int     firstBlk = -1;
		  int     keyLen   = keyName.length();
		  while( (dirPos + 32) <= this.dirBytes.length ) {
		    if( ((int) this.dirBytes[ dirPos ] & 0xFF) != 0xE5 ) {
		      boolean matches = true;
		      for( int i = 0; i < 11; i++ ) {
			int c = (int) this.dirBytes[ dirPos + 1 + i ] & 0x7F;
			if( i < keyLen ) {
			  if( c != keyName.charAt( i ) ) {
			    matches = false;
			    break;
			  }
			} else {
			  if( c != 0x20 ) {
			    matches = false;
			    break;
			  }
			}
		      }
		      if( matches ) {
			int pos = dirPos + 16;
			if( this.blockNum16Bit ) {
			  for( int i = 0; i < 8; i++ ) {
			    int blk = EmuUtil.getWord( this.dirBytes, pos );
			    if( blk >= this.dirBlocks ) {
			      if( firstBlk < 0 ) {
				firstBlk = blk;
			      }
			      setBlockData(
					blk,
					fileBytes,
					(blk - firstBlk) * this.blockSize );
			    }
			    pos += 2;
			  }
			} else {
			  for( int i = 0; i < 16; i++ ) {
			    int blk = (int) this.dirBytes[ pos ] & 0xFF;
			    if( blk >= this.dirBlocks ) {
			      if( firstBlk < 0 ) {
				firstBlk = blk;
			      }
			      setBlockData(
					blk,
					fileBytes,
					(blk - firstBlk) * this.blockSize );
			    }
			    pos++;
			  }
			}
		      }
		    }
		    dirPos += 32;
		  }
		}
	      }
	    }
	  }
	  rv = this.sectors[ absSectIdx ];
	}
	if( rv == null ) {
	  /*
	   * Sektor gehoert nicht zu Systemspuren, Directory
	   * oder zu einer Datei -> Sektor mit Null-Bytes
	   */
	  setSectorData( absSectIdx, null, 0, false );
	  rv = this.sectors[ absSectIdx ];
	}
      }
    }
    return rv;
  }


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


  public Integer getSystemTracks()
  {
    return new Integer( this.sysCyls );
  }


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
    }
  }


	/* --- private Methoden --- */

  private byte[] readFile( File file )
  {
    byte[] fileBytes = null;
    try {
      fileBytes = EmuUtil.readFile( file, getDiskSize() );
    }
    catch( IOException ex ) {
      String msg = ex.getMessage();
      if( msg == null ) {
	msg = file.getPath() + ": Fehler beim Lesen";
      }
      showError( msg, ex );
    }
    return fileBytes;
  }


  private void rebuildDisk()
  {
    if( this.dirBytes == null ) {
      this.dirBytes = new byte[ this.dirBlocks * this.blockSize ];
    }
    Arrays.fill( this.dirBytes, (byte) 0xE5 );
    Arrays.fill( this.sectors, null );
    this.sysSectors = getSides() * this.sysCyls * getSectorsPerCylinder();
    this.fileMap.clear();
    this.lastBuildMillis = System.currentTimeMillis();
    try {
      File[] files = this.dirFile.listFiles();
      if( files != null ) {
	Map<String,File> fileMap = new HashMap<String,File>();
	for( int i = 0; i < files.length; i++ ) {
	  File file = files[ i ];
	  if( file != null ) {
	    if( file.isFile() && !file.equals( this.sysFile ) ) {
	      String fName = files[ i ].getName();
	      if( fName != null ) {
		String fName2 = fName.trim().toUpperCase();
		if( fName2 != null ) {
		  int len = fName2.length();
		  if( (len <= 12) && (len == fName.length()) ) {
		    StringBuilder buf    = new StringBuilder( 11 );
		    int           nChars = 0;
		    boolean       ignore = false;
		    boolean       point  = false;
		    for( int k = 0; k < len; k++ ) {
		      char ch = fName2.charAt( k );
		      if( DiskUtil.isValidCPMFileNameChar( ch ) ) {
			int n = buf.length();
			if( (!point && (nChars < 8))
			    || (point && (nChars < 11)) )
			{
			  buf.append( ch );
			  nChars++;
			} else {
			  ignore = true;
			  break;
			}
		      } else if( ch == '.' ) {
			if( nChars > 1 ) {
			  while( nChars < 8 ) {
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
		    if( !ignore && (nChars > 0) ) {
		      while( nChars < 11 ) {
			buf.append( (char) '\u0020' );
			nChars++;
		      }
		      String keyName = buf.toString();
		      if( fileMap.containsKey( keyName ) ) {
			/*
			 * Falls mehrere Dateien existieren und deren
			 * grossgeschriebene Namen sich nicht unterscheiden,
			 * werden diese Dateien ignoriert.
			 */
			fileMap.put( keyName, null );
		      } else {
			fileMap.put( keyName, files[ i ] );
		      }
		    }
		  }
		}
	      }
	    }
	  }
	}
	String[]           keyNames = null;
	Collection<String> tmpNames = fileMap.keySet();
	if( tmpNames != null ) {
	  int n = tmpNames.size();
	  if( n > 0 ) {
	    keyNames = tmpNames.toArray( new String[ n ] );
	  }
	}
	if( keyNames != null ) {
	  Arrays.sort( keyNames );
	  int dirIdx         = 0;
	  int blkIdx         = this.dirBlocks;
	  int nRemainBlocks  = (getDiskSize() / this.blockSize) - blkIdx;
	  int maxFileEntries = (this.blockNum16Bit ? 32 : 1);
	  int maxEntrySize   = (this.blockNum16Bit ? 8 : 16) * this.blockSize;
	  if( maxEntrySize > (0xFF * 128) ) {
	    maxEntrySize   = 0xFF * 128;
	    maxFileEntries = 1;
	  }
	  for( int i = 0; i < keyNames.length; i++ ) {
	    String keyName = keyNames[ i ];
	    if( keyName != null ) {
	      File file = fileMap.get( keyName );
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
		    long nBlocks = (fSize + this.blockSize - 1)
							/ this.blockSize;
		    if( nBlocks <= nRemainBlocks ) {
		      for( int k = 0; k < nEntries; k++ ) {
			this.dirBytes[ dirIdx++ ] = (byte) 0;
			int len = keyName.length();
			for( int p = 0; p < 11; p++ ) {
			  char ch = '\u0020';
			  if( p < len ) {
			    ch = keyName.charAt( p );
			  }
			  this.dirBytes[ dirIdx++ ] = (byte) (ch & 0x7F);
			}
			this.dirBytes[ dirIdx++ ] = (byte) k;
			this.dirBytes[ dirIdx++ ] = (byte) 0;
			this.dirBytes[ dirIdx++ ] = (byte) 0;
			long nSegs = 0;
			if( fSize > 0 ) {
			  nSegs = (Math.min( fSize, maxEntrySize ) + 127)
								/ 128;
			}
			this.dirBytes[ dirIdx++ ] = (byte) nSegs;
			if( this.blockNum16Bit ) {
			  for( int m = 0; m < 8; m++ ) {
			    if( nBlocks > 0 ) {
			      this.dirBytes[ dirIdx++ ] = (byte) blkIdx++;
			      this.dirBytes[ dirIdx++ ] = (byte) (blkIdx >> 8);
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
		      this.fileMap.put( keyName, file );
		    }
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
	setSectorData( absSectIdx++, this.dirBytes, srcPos, false );
	srcPos += sectorSize;
      }
    }
  }


  private void setBlockData(
			int    block,
			byte[] dataBuf,
			int    dataOffs )
  {
    int sectorSize = getSectorSize();
    if( sectorSize > 0 ) {
      int sectorsPerBlock = this.blockSize / sectorSize;
      for( int i = 0; i < sectorsPerBlock; i++ ) {
	setSectorData(
		this.sysSectors + (block * sectorsPerBlock) + i,
		dataBuf,
		dataOffs,
		dataBuf == null );
	dataOffs += sectorSize;
      }
    }
  }


  private void setSectorData(
			int     absSectIdx,
			byte[]  dataBuf,
			int     dataOffs,
			boolean err )
  {
    if( (absSectIdx >= 0) && (absSectIdx < this.sectors.length) ) {
      int sides      = getSides();
      int sectPerCyl = getSectorsPerCylinder();
      int sectorSize = getSectorSize();
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
}

