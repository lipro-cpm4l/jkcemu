/*
 * (c) 2009-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Entpacker fuer Abbilddateien
 */

package jkcemu.disk;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.text.TextUtil;


public class DiskUtil
{
  public static final String[] anaDiskFileExt   = { ".dump" };
  public static final String[] copyQMFileExt    = { ".cqm", ".qm" };
  public static final String[] dskFileExt       = { ".dsk" };
  public static final String[] imageDiskFileExt = { ".imd" };
  public static final String[] teleDiskFileExt  = { ".td0" };
  public static final String[] plainDiskFileExt = { ".img", ".image", ".raw" };

  public static final String[] gzAnaDiskFileExt   = { ".dump.gz" };
  public static final String[] gzCopyQMFileExt    = { ".cqm.gz", ".qm.gz" };
  public static final String[] gzDskFileExt       = { ".dsk.gz" };
  public static final String[] gzImageDiskFileExt = { ".imd.gz" };
  public static final String[] gzTeleDiskFileExt  = { ".td0.gz" };
  public static final String[] gzPlainDiskFileExt = {
						".img.gz",
						".image.gz",
						".raw.gz" };

  private static final int DEFAULT_BLOCK_SIZE = 2048;


  public static boolean checkAndConfirmWarning(
				Component          owner,
				AbstractFloppyDisk disk )
  {
    boolean rv = true;
    if( disk != null ) {
      String msg = disk.getWarningText();
      if( msg != null ) {
	if( JOptionPane.showConfirmDialog(
		owner,
		msg,
		"Warnung",
		JOptionPane.OK_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE ) != JOptionPane.OK_OPTION )
	{
	  rv = false;
	}
      }
    }
    return rv;
  }


  public static boolean checkFileExt(
				Component   owner,
				File        file,
				String[]... extensions )
  {
    boolean rv = false;
    String  s  = file.getName();
    if( s != null ) {
      s = s.toLowerCase();
      for( int i = 0; i < extensions.length; i++ ) {
	if( TextUtil.endsWith( s, extensions[ i ] ) ) {
	  rv = true;
	  break;
	}
      }
    }
    if( !rv ) {
      rv = BasicDlg.showYesNoWarningDlg(
		owner,
		"Die Dateiendung entspricht nicht der f\u00FCr"
			+ " diesen Dateityp \u00FCblichen Endung.\n"
			+ "Wenn Sie die Datei sp\u00E4ter einmal"
			+ " \u00F6ffnen m\u00F6chten,\n"
			+ "wird JKCEMU den Dateityp nicht richtig"
			+ " erkennen k\u00F6nnen.\n\n"
			+ "M\u00F6chten Sie trotzdem fortsetzen?",
		"Achtung" );
    }
    return rv;
  }


  public static boolean equalsDiskSize(
				DeviceIO.RandomAccessDevice rad,
				int                         diskSize )
  {
    boolean rv = false;
    try {
      /*
       * Bei Zugriff auf ein Laufwerk unter Windows
       * muss der Puffer mit der Sektorgroesse uebereinstimmen.
       * Da die Diskettenformate, die hier getestet werden,
       * entweder 512 oder 1024 Bytes grosse Sektoren haben,
       * wird ein 1024 Byte grosser Puffer verwendet.
       */
      byte[] buf = new byte[ 1024 ];
      if( diskSize >= buf.length ) {
	rad.seek( diskSize - buf.length );
	if( rad.read( buf, 0, buf.length ) == buf.length ) {
	  try {
	    if( rad.read( buf, 0, buf.length ) <= 0 ) {
	      rv = true;
	    }
	  }
	  catch( IOException ex ) {
	    rv = true;
	  }
	}
      }
    }
    catch( IOException ex ) {}
    return rv;
  }


  public static boolean isFilledDirBlock( byte[] blockBuf )
  {
    return getBlockDirStatus( blockBuf ) == 2;
  }


  public static boolean isValidCPMFileNameChar( char ch )
  {
    boolean rv = false;
    if( (ch > '\u0020') && (ch <= '\u007E') ) {
      if( "<>.,;:=?*[]".indexOf( ch ) < 0 ) {
	rv = true;
      }
    }
    return rv;
  }


  public static java.util.List<FileEntry> readDirectory(
						AbstractFloppyDisk disk )
  {
    java.util.List<byte[]> dirBlocks = new ArrayList<byte[]>();
    readDirBlocks( dirBlocks, null, null, null, disk );
    return extractDirectory( dirBlocks );
  }


  public static java.util.List<FileEntry> readDirFromPlainDisk( File file )
  {
    java.util.List<FileEntry> rv = null;
    if( file != null ) {
      InputStream in = null;
      try {
	in = new FileInputStream( file );
	if( EmuUtil.isGZipFile( file ) ) {
	  in = new GZIPInputStream( in );
	}
	java.util.List<byte[]> dirBlocks = new ArrayList<byte[]>();
	readDirBlocks( dirBlocks, in, null, null, null );
	rv = extractDirectory( dirBlocks );
      }
      catch( IOException ex ) {}
      finally {
	EmuUtil.doClose( in );
      }
    }
    return rv;
  }


  /*
   * Diese Methode testet die Kapazitaet der Diskette,
   * die in dem mit raf geoeffneten Laufwerk liegt.
   * Kann dies nicht ermittelt werden, wird -1 zurueckgegeben.
   */
  public static int readDiskSize(
			DeviceIO.RandomAccessDevice rad ) throws IOException
  {
    int   diskSize  = -1;
    int[] diskSizes = {
		720 * 1024,
		800 * 1024,
		1200 * 1024,
		1440 * 1024,
		2880 * 1024 };
    for( int i = 0; i < diskSizes.length; i++ ) {
      if( equalsDiskSize( rad, diskSizes[ i ] ) ) {
	diskSize = diskSizes[ i ];
	break;
      }
    }
    return diskSize;
  }


  public static AbstractFloppyDisk readNonPlainDiskFile(
					Frame owner,
					File  file ) throws IOException
  {
    AbstractFloppyDisk disk = null;
    if( file != null ) {
      String fName = file.getName();
      if( fName != null ) {
	fName = fName.toLowerCase();
	if( TextUtil.endsWith( fName, DiskUtil.anaDiskFileExt )
	    || TextUtil.endsWith( fName, DiskUtil.gzAnaDiskFileExt ) )
	{
	  disk = AnaDisk.readFile( owner, file );
	}
      }
      if( disk == null ) {
	byte[] header = EmuUtil.readFile( file, 0x100 );
	if( header != null ) {
	  if( CopyQMDisk.isCopyQMFileHeader( header ) ) {
	    disk = CopyQMDisk.readFile( owner, file );
	  }
	  else if( CPCDisk.isCPCDiskFileHeader( header ) ) {
	    disk = CPCDisk.readFile( owner, file );
	  }
	  else if( ImageDisk.isImageDiskFileHeader( header ) ) {
	    disk = ImageDisk.readFile( owner, file );
	  }
	  else if( TeleDisk.isTeleDiskFileHeader( header ) ) {
	    disk = TeleDisk.readFile( owner, file );
	  }
	}
      }
    }
    return disk;
  }


  public static void unpackDisk(
			Window             owner,
			File               diskFile,
			AbstractFloppyDisk disk )
  {
    // sinnvolle Vorbelegung ermitteln
    java.util.List<byte[]> dirBlocks = new ArrayList<byte[]>();
    int preSysTracks = 0;
    int preSysBlocks = readDirBlocks( dirBlocks, null, null, null, disk );
    if( preSysBlocks > 0 ) {
      int blocksPerTrack = (disk.getSides()
				* disk.getSectorsPerCylinder()
				* disk.getSectorSize()) / DEFAULT_BLOCK_SIZE;
      if( blocksPerTrack > 0 ) {
	preSysTracks = preSysBlocks / blocksPerTrack;
      }
    }
    boolean blockNum16Bit = check16BitBlockNums(
					dirBlocks,
					disk.getDiskSize() );
    FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			owner,
			null,
			FloppyDiskFormatDlg.Flag.SYSTEM_TRACKS,
			FloppyDiskFormatDlg.Flag.BLOCK_SIZE,
			FloppyDiskFormatDlg.Flag.BLOCK_NUM_SIZE,
			FloppyDiskFormatDlg.Flag.APPLY_READONLY,
			FloppyDiskFormatDlg.Flag.FORCE_LOWERCASE );
    dlg.setSystemTracks( preSysTracks );
    dlg.setBlockSize( DEFAULT_BLOCK_SIZE );
    dlg.setBlockNum16Bit( blockNum16Bit );
    dlg.setVisible( true );
    if( dlg.wasApproved() ) {
      int sysTracks = dlg.getSystemTracks();
      int blockSize = dlg.getBlockSize();
      if( (sysTracks >= 0) && (blockSize > 0) ) {
	String fileFmtText = disk.getFileFormatText();
	if( fileFmtText == null ) {
	  fileFmtText = "Diskettenabbilddatei";
	}
	File outDir = EmuUtil.askForOutputDir(
					owner,
					diskFile,
					"Entpacken nach:",
					fileFmtText + " entpacken" );
	if( outDir != null ) {
	  DiskUnpacker.unpackDisk(
			owner,
			disk,
			fileFmtText,
			outDir,
			sysTracks,
			blockSize,
			dlg.getBlockNum16Bit(),
			dlg.getApplyReadOnly(),
			dlg.getForceLowerCase() );
	}
      }
    }
  }


  public static void unpackPlainDisk(
			Frame  owner,
			String driveFileName ) throws IOException
  {
    DeviceIO.RandomAccessDevice rad = null;
    try {
      rad = DeviceIO.openDeviceForRandomAccess( driveFileName, true );
      Main.setLastDriveFileName( driveFileName );
      if( unpackPlainDisk(
			owner,
			driveFileName,
			rad,
			null,
			"Diskette",
			readDiskSize( rad ),
			getHomeDirFile(),
			"diskette" ) )
      {
	rad = null;		// Schliessen verhindern
      }
    }
    finally {
      EmuUtil.doClose( rad );
    }
  }


  public static void unpackPlainDiskFile(
				Frame owner,
				File  diskFile ) throws IOException
  {
    RandomAccessFile raf = null;
    try {
      long   fileLen    = diskFile.length();
      String presetName = diskFile.getName();
      if( presetName != null ) {
	int pos = presetName.lastIndexOf( "." );
	if( pos > 0 ) {
	  presetName = presetName.substring( 0, pos );
	} else {
	  presetName = presetName + ".d";
	}
      }
      raf = new RandomAccessFile( diskFile, "r" );
      if( unpackPlainDisk(
			owner,
			diskFile.getPath(),
			null,
			raf,
			"Einfache Abbilddatei",
			fileLen,
			diskFile.getParentFile(),
			presetName ) )
      {
	raf = null;		// Schliessen verhindern
      }
    }
    finally {
      EmuUtil.doClose( raf );
    }
  }


	/* --- private Methoden --- */

  private static boolean check16BitBlockNums(
				java.util.List<byte[]> dirBlocks,
				long                   diskSize )
  {
    boolean rv   = (diskSize > (512 * 1024));		// Default-Wert
    boolean done = false;
    if( dirBlocks != null ) {
      java.util.List<Integer> tmpList = new ArrayList<Integer>( 8 );
      int maxHValue = (int) ((diskSize / DEFAULT_BLOCK_SIZE) >> 8);
      for( byte[] blockBuf : dirBlocks ) {
	if( blockBuf != null ) {
	  int pos = 0;
	  while( !done && ((pos + 31) < blockBuf.length) ) {
	    int b0  = (int) blockBuf[ pos ] & 0xFF;
	    int b15 = (int) blockBuf[ pos + 15 ] & 0xFF;
	    if( (b0 >= 0) && (b0 <= 0x1F) && (b15 > 8) ) {
	      /*
	       * gueltiger Dateieintrag mit mehr als 2 KByte Groesse,
	       * d.h. mit mindestens zwei Bloecken
	       */
	      int nBlocks = ((b15 * 128) + 2047) / 2048;
	      if( nBlocks > 8 ) {
		/*
		 * Bei 16-Bit-Blocknummern koennen nur max. 8 Bloecke
		 * pro Directory-Eintrag referenziert werden.
		 */
		rv   = false;
		done = true;
	      } else {
		/*
		 * Wenn ein Byte auf der Position eines hoeherwertigen Bytes
		 * groesser als das fuer die Diskettengroesse maximal
		 * moegliche hoeherwertige Byte ist,
		 * muessen es 8-Bit-Nummern sein.
		 * Wenn dagegen so ein Byte groesser Null ist
		 * und innerhalb des Eintrags mehrfach vorkommt,
		 * muessen es 16-Bit-Nummern sein.
		 */
		tmpList.clear();
		int hPos = pos + 17;
		for( int i = 0; i < nBlocks; i++ ) {
		  int b = (int) blockBuf[ hPos ] & 0xFF;
		  if( b > maxHValue ) {
		    rv   = false;
		    done = true;
		    break;
		  }
		  Integer tmpValue = new Integer( b );
		  if( tmpList.contains( tmpValue ) ) {
		    rv   = true;
		    done = true;
		    break;
		  }
		  tmpList.add( tmpValue );
		  hPos += 2;
		}
	      }
	    }
	    pos += 32;
	  }
	}
	if( done ) {
	  break;
	}
      }
    }
    return rv;
  }


  private static java.util.List<FileEntry> extractDirectory(
					java.util.List<byte[]> dirBlocks )
  {
    java.util.List<FileEntry> rv = null;
    if( dirBlocks != null ) {
      if( !dirBlocks.isEmpty() ) {
	Map<String,Long> entrySizes = new HashMap<String,Long>();
	for( byte[] blockBuf : dirBlocks ) {
	  int entryPos = 0;
	  while( (entryPos + 15) < blockBuf.length ) {
	    int b = (int) blockBuf[ entryPos ] & 0xFF;
	    if( (b >= 0) && (b <= 15) ) {
	      int           pos = entryPos + 1;
	      StringBuilder buf = new StringBuilder( 12 );
	      for( int i = 0; i < 8; i++ ) {
		int ch = (int) blockBuf[ pos++ ] & 0x7F;
		if( (ch > 0x20) && (ch < 0x7F) ) {
		  buf.append( (char) ch );
		}
	      }
	      if( buf.length() > 0 ) {
		boolean point = true;
		for( int i = 0; i < 3; i++ ) {
		  int ch = (int) blockBuf[ pos++ ] & 0x7F;
		  if( (ch > 0x20) && (ch < 0x7F) ) {
		    if( point ) {
		      buf.append( (char) '.' );
		      point = false;
		    }
		    buf.append( (char) ch );
		  }
		}
		int    eLen  = ((int) blockBuf[ entryPos + 15 ] & 0xFF) * 128;
		String eName = buf.toString();
		Long   value = entrySizes.get( eName );
		if( value != null ) {
		  value = new Long( value.longValue() + eLen );
		} else {
		  value = new Long( eLen );
		}
		entrySizes.put( eName, value );
	      }
	    }
	    entryPos += 32;
	  }
	}
	Set<String> entryNames = entrySizes.keySet();
	if( entryNames != null ) {
	  int n = entryNames.size();
	  if( n > 0 ) {
	    String[] a = entryNames.toArray( new String[ n ] );
	    if( a != null ) {
	      Arrays.sort( a );
	      rv = new ArrayList<FileEntry>( n );
	      for( int i = 0; i < a.length; i++ ) {
		String s = a[ i ];
		if( s != null ) {
		  FileEntry entry = new FileEntry( s );
		  entry.setSize( entrySizes.get( s ) );
		  rv.add( entry );
		}
	      }
	    }
	  }
	}
	if( rv == null ) {
	  // leere aber gueltige Abbilddatei
	  rv = new ArrayList<FileEntry>();
	}
      }
    }
    return rv;
  }


  /*
   * Die Methoden prueft einen Block auf CP/M-Directory-Eintraege
   *
   * Rueckgabewert:
   *  0: kein Directory-Block
   *  1: leerer Directory-Block
   *  2. gefuellter Directory-Block
   */
  private static int getBlockDirStatus( byte[] blockBuf )
  {
    boolean isDir   = false;
    boolean isEmpty = true;
    if( blockBuf != null ) {
      if( blockBuf.length > 31 ) {
	isDir        = true;
	int entryPos = 0;
	while( (entryPos + 31) < blockBuf.length ) {
	  int b = (int) blockBuf[ entryPos ] & 0xFF;
	  if( b != 0xE5 ) {
	    isEmpty = false;
	  }
	  if( ((b & 0xE0) != 0)		// Bit 0-3: User, Bit 4: Passwort
	      && (b != 0x20)		// Disk-Label-Eintrag
	      && (b != 0x21)		// Zeitstempeleintrag
	      && (b != 0xE5) )		// leerer Eintrag
	  {
	    isDir = false;
	    break;
	  }
	  if( b <= 15 ) {
	    int pos = entryPos + 1;
	    for( int i = 0; isDir && (i < 11); i++ ) {
	      int ch = (int) blockBuf[ pos++ ] & 0x7F;
	      if( (ch < 0x20) || (ch >= 0x7F) ) {
		isDir = false;
		break;
	      }
	    }
	  }
	  entryPos += 32;
	}
      }
    }
    return isDir ? (isEmpty ? 1 : 2) : 0;
  }


  private static File getHomeDirFile()
  {
    File           rv  = null;
    FileSystemView fsv = FileSystemView.getFileSystemView();
    if( fsv != null ) {
      rv = fsv.getHomeDirectory();
    }
    if( rv == null ) {
      String homeDir = System.getProperty( "user.home" );
      if( homeDir != null ) {
	rv = new File( homeDir );
      }
    }
    return rv;
  }


  private static boolean readBlock(
				byte[]                      buf,
				int                         blockIdx,
				DeviceIO.RandomAccessDevice rad,
				RandomAccessFile            raf,
				AbstractFloppyDisk          disk,
				int                         blockSize )
  {
    boolean rv = false;
    if( (blockSize > 0) && (blockSize <= buf.length) ) {
      try {
	if( raf != null ) {
	  raf.seek( blockIdx * blockSize );
	  if( raf.read( buf, 0, blockSize ) == blockSize ) {
	    rv = true;
	  }
	}
	else if( disk != null ) {
	  int nRemain    = blockSize;
	  int sides      = disk.getSides();
	  int sectPerCyl = disk.getSectorsPerCylinder();
	  int sectorSize = disk.getSectorSize();
	  if( (sides > 0) && (sectPerCyl > 0) && (sectorSize > 0) ) {
	    int sectPerBlock = blockSize / sectorSize;
	    if( sectPerBlock > 0 ) {
	      for( int i = 0; i < sectPerBlock; i++ ) {
		int nRead      = 0;
		int absSectIdx = (sectPerBlock * blockIdx) + i;
		int cyl        = absSectIdx / sectPerCyl / sides;
		int head       = 0;
		int sectIdx    = absSectIdx - (cyl * sectPerCyl * sides);
		if( sectIdx >= sectPerCyl ) {
		  head++;
		  sectIdx -= sectPerCyl;
		}
		SectorData sector = disk.getSectorByID(
						cyl,
						head,
						cyl,
						head,
						sectIdx + 1,
						-1 );
		if( sector != null ) {
		  nRead = sector.read( buf, i * sectorSize, sectorSize );
		  nRemain -= nRead;
		}
		if( nRead <= 0 ) {
		  break;
		}
	      }
	    }
	  }
	  if( nRemain == 0 ) {
	    rv = true;
	  }
	}
      }
      catch( IOException ex ) {
	rv = false;
      }
    }
    return rv;
  }


  /*
   * Die Methode liest die Directory-Bloecke
   * und haengt sie an die uebergebene Liste an (sofern not null).
   * Es wird die Standardblockgroesse angenommen.
   *
   * Rueckgabewert: Anzahl der Bloecke der Systemspuren
   */
  private static int readDirBlocks(
				java.util.List<byte[]>      list,
				InputStream                 in,
				DeviceIO.RandomAccessDevice rad,
				RandomAccessFile            raf,
				AbstractFloppyDisk          disk )
  {
    byte[]  blockBuf      = null;
    int     firstDirBlock = -1;
    int     blockIdx      = 0;
    boolean readStatus    = true;
    while( readStatus ) {
      if( blockBuf == null ) {
	blockBuf = new byte[ DEFAULT_BLOCK_SIZE ];
      }
      Arrays.fill( blockBuf, (byte) 0 );
      try {
	if( in != null ) {
	  if( EmuUtil.read( in, blockBuf ) <= 0 ) {
	    readStatus = false;
	  }
	} else {
	  readStatus = readBlock(
				blockBuf,
				blockIdx,
				rad,
				raf,
				disk,
				DEFAULT_BLOCK_SIZE );
	}
      }
      catch( IOException ex ) {
	readStatus = false;
      }
      if( readStatus ) {
	int dirStatus = getBlockDirStatus( blockBuf );
	if( dirStatus > 0 ) {
	  if( firstDirBlock < 0 ) {
	    firstDirBlock = blockIdx;
	  }
	  if( list != null ) {
	    list.add( blockBuf );
	  }
	  blockBuf = null;
	  if( dirStatus == 1 ) {
	    // nach leeren Directory-Block abbrechen
	    readStatus = false;
	  }
	} else {
	  if( firstDirBlock >= 0 ) {
	    // beim erstem Block hinter Directory abbrechen
	    readStatus = false;
	  }
	}
      }
      blockIdx++;
    }
    return firstDirBlock > 0 ? firstDirBlock : 0;
  }


  private static boolean unpackPlainDisk(
		Frame                       owner,
		String                      diskFileName,
		DeviceIO.RandomAccessDevice rad,
		RandomAccessFile            raf,
		String                      diskDesc,
		long                        diskSize,
		File                        presetDir,
		String                      presetName ) throws IOException
  {
    boolean                rv           = false;
    int                    preSysTracks = 0;
    java.util.List<byte[]> dirBlocks    = null;

    // sinnvolle Vorbelegung ermitteln
    FloppyDiskFormat fmt = FloppyDiskFormat.getFormatByDiskSize( diskSize );
    if( (diskSize > 0) && (diskSize < Integer.MAX_VALUE) ) {
      if( fmt != null ) {
	dirBlocks        = new ArrayList<byte[]>();
	int preSysBlocks = readDirBlocks( dirBlocks, null, rad, raf, null );
	if( preSysBlocks > 0 ) {
	  int blocksPerTrack = (fmt.getSides()
				* fmt.getSectorsPerCylinder()
				* fmt.getSectorSize()) / DEFAULT_BLOCK_SIZE;
	  if( blocksPerTrack > 0 ) {
	    preSysTracks = preSysBlocks / blocksPerTrack;
	  }
	}
      }
    }
    boolean blockNum16Bit = check16BitBlockNums( dirBlocks, diskSize );
    FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			owner,
			fmt,
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT,
			FloppyDiskFormatDlg.Flag.SYSTEM_TRACKS,
			FloppyDiskFormatDlg.Flag.BLOCK_SIZE,
			FloppyDiskFormatDlg.Flag.BLOCK_NUM_SIZE,
			FloppyDiskFormatDlg.Flag.APPLY_READONLY,
			FloppyDiskFormatDlg.Flag.FORCE_LOWERCASE );
    dlg.setSystemTracks( preSysTracks );
    dlg.setBlockSize( DEFAULT_BLOCK_SIZE );
    dlg.setBlockNum16Bit( blockNum16Bit );
    dlg.setVisible( true );
    fmt = dlg.getFormat();
    if( fmt != null ) {
      int sysTracks = dlg.getSystemTracks();
      int blockSize = dlg.getBlockSize();
      if( (sysTracks >= 0) && (blockSize > 0) ) {
	File outDir = EmuUtil.askForOutputDir(
					owner,
					presetDir,
					presetName,
					"Entpacken nach:",
					diskDesc + " entpacken" );
	if( outDir != null ) {
	  AbstractFloppyDisk disk = null;
	  if( rad != null ) {
	    disk = PlainDisk.createForDrive(
					owner,
					diskFileName,
					rad,
					true,
					fmt );
	  } else if( raf != null ) {
	    disk = PlainDisk.createForFile(
					owner,
					diskFileName,
					raf,
					true,
					fmt );
	  }
	  if( disk != null ) {
	    DiskUnpacker.unpackDisk(
			owner,
			disk,
			diskDesc,
			outDir,
			sysTracks,
			blockSize,
			dlg.getBlockNum16Bit(),
			dlg.getApplyReadOnly(),
			dlg.getForceLowerCase() );
	    rv = true;
	  }
	}
      }
    }
    return rv;
  }
}

