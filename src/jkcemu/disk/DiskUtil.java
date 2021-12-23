/*
 * (c) 2009-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Entpacker fuer Abbilddateien
 */

package jkcemu.disk;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import javax.swing.JOptionPane;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileEntry;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;


public class DiskUtil
{
  public static final String[] anaDiskFileExt   = { ".dump" };
  public static final String[] copyQMFileExt    = { ".cqm", ".qm" };
  public static final String[] dskFileExt       = { ".dsk" };
  public static final String[] imageDiskFileExt = { ".imd" };
  public static final String[] isoFileExt       = { ".iso" };
  public static final String[] teleDiskFileExt  = { ".td0" };
  public static final String[] plainDiskFileExt = {
					".dd", ".img", ".image", ".raw" };

  public static final String[] gzAnaDiskFileExt   = { ".dump.gz" };
  public static final String[] gzCopyQMFileExt    = { ".cqm.gz", ".qm.gz" };
  public static final String[] gzDskFileExt       = { ".dsk.gz" };
  public static final String[] gzImageDiskFileExt = { ".imd.gz" };
  public static final String[] gzISOFileExt       = { ".iso.gz" };
  public static final String[] gzTeleDiskFileExt  = { ".td0.gz" };
  public static final String[] gzPlainDiskFileExt = {
						".dd.gz",
						".img.gz",
						".image.gz",
						".raw.gz" };

  public static final int DEFAULT_BLOCK_SIZE = 2048;

  private enum DirStatus { NO_DIR, EMPTY_DIR, FILLED_DIR };

  private static FloppyDiskFormat[] stdFloppyFormats35 = {
			new FloppyDiskFormat( 80, 2,  9, 512 ),
			new FloppyDiskFormat( 80, 2, 18, 512 ),
			new FloppyDiskFormat( 80, 2, 36, 512 ) };


  public static boolean checkAndConfirmWarning(
				Component          owner,
				AbstractFloppyDisk disk )
  {
    boolean rv = true;
    if( disk != null ) {
      String msg = disk.getWarningText();
      if( msg != null ) {
	if( JOptionPane.showConfirmDialog(
		EmuUtil.getWindow( owner ),
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
      rv = BaseDlg.showYesNoWarningDlg(
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


  /*
   * Die Methode sucht und liest das Directory.
   * Es wird dazu die halbe Standardblockgroesse verwendet.
   * Optional kann ueber einen Parameter die Anzahl der uebersprungenen
   * Bytes (Systemspuren) zurueckgegeben werden.
   *
   * Es werden maximal soviel Bytes gelesen und ausgewertet wie
   * 3 Spuren vom groessten Format (2.88 MByte).
   */
  public static byte[] findAndReadDirBytes(
				InputStream                 in,
				DeviceIO.RandomAccessDevice rad,
				RandomAccessFile            raf,
				AbstractFloppyDisk          disk,
				AtomicInteger               rvSysBytes )
  {
    ByteArrayOutputStream outBuf = new ByteArrayOutputStream(
						3 * DEFAULT_BLOCK_SIZE );
    byte[]  blockBuf         = null;
    byte[]  emptyDirBlockBuf = null;
    int     nEmptyDirBlocks  = 0;
    int     emptyDirBlockIdx = -1;
    int     firstDirBlockIdx = -1;
    int     blockIdx         = 0;
    int     blockSize        = DEFAULT_BLOCK_SIZE / 2;
    int     maxBlocks        = 2880 * 1024 / 80 * 3 / blockSize;
    boolean readStatus       = true;
    while( readStatus && (blockIdx < maxBlocks) ) {
      if( blockBuf == null ) {
	blockBuf = new byte[ blockSize ];
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
				disk );
	}
      }
      catch( IOException ex ) {
	readStatus = false;
      }
      if( readStatus ) {
	switch( getDirStatus( blockBuf ) ) {
	  case EMPTY_DIR:
	    /*
	     * Es koennte sein, dass die Systemspuren mit E5h gefuellt sind,
	     * was als leerer Directory-Block erkannt wird.
	     * Aus diesem Grund werden weitere Directory-Bloecke abgewartet,
	     * um zu sehen, ob noch ein gefuellter Directory-Block folgt.
	     */
	    if( emptyDirBlockBuf == null ) {
	      emptyDirBlockBuf = blockBuf;
	      emptyDirBlockIdx = blockIdx;
	    }
	    nEmptyDirBlocks++;
	    if( outBuf.size() > 0 ) {
	      outBuf.write( blockBuf, 0, blockBuf.length );
	    }
	    blockBuf = null;
	    break;

	  case FILLED_DIR:
	    if( firstDirBlockIdx < 0 ) {
	      firstDirBlockIdx = blockIdx;
	    }
	    outBuf.write( blockBuf, 0, blockBuf.length );
	    blockBuf = null;
	    break;

	  default:
	    // beim ersten Block hinter Directory abbrechen
	    if( firstDirBlockIdx >= 0 ) {
	      readStatus = false;
	    }
	    break;
	}
      }
      blockIdx++;
    }

    /*
     * Wenn kein Directory gefunden wurde,
     * dann den ersten leeren Directory-Block nehmen,
     * falls vorhanden
     */
    if( (outBuf.size() == 0)
	&& (emptyDirBlockBuf != null)
	&& (emptyDirBlockIdx >= 0) )
    {
      outBuf.write( emptyDirBlockBuf, 0, emptyDirBlockBuf.length );
      firstDirBlockIdx = emptyDirBlockIdx;
    }

    if( (rvSysBytes != null)
	&& (outBuf.size() > 0)
	&& (firstDirBlockIdx >= 0) )
    {
      rvSysBytes.set( firstDirBlockIdx * blockSize );
    }
    return outBuf.toByteArray();
  }


  public static int getEntryBlock128Count(
				byte[] dirBytes,
				int    entryPos )
  {
    int rv = -1;
    if( dirBytes != null ) {
      if( (entryPos >= 0) && ((entryPos + 15) < dirBytes.length) ) {
	rv = (getExtentNumByEntryPos( dirBytes, entryPos ) * 0x80)
			+ ((int) dirBytes[ entryPos + 15 ] & 0xFF);
      }
    }
    return rv;
  }


  public static int getExtentNumByEntryPos( byte[] dirBytes, int entryPos )
  {
    int rv = 0;
    if( dirBytes != null ) {
      if( (entryPos + 14) < dirBytes.length ) {
	rv = ((int) dirBytes[ entryPos + 12 ] & 0x1F)
		| (((int) dirBytes[ entryPos + 14 ] << 5) & 0x07E0);
      }
    }
    return rv;
  }


  /*
   * Extents pro Directory-Eintrag ermitteln
   *
   * Da pro Extent die Groesse in 80h-Bloecken max. 4000h sein kann,
   * muessen u.U. pro Directory-Eintrag mehrere Extents verwendet werden.
   */
  public static int getExtentsPerDirEntry(
				int     blockSize,
				boolean blockNum16Bit )
  {
    int sizePerDirEntry = blockSize * (blockNum16Bit ? 8 : 16);
    return sizePerDirEntry > 0x4000 ?
		((sizePerDirEntry + 0x4000 - 1) / 0x4000) : 1;
  }


  public static FloppyDiskFormat getFloppyDiskFormat(
				DeviceIO.RandomAccessDevice rad )
						throws IOException
  {
    FloppyDiskFormat fmt = null;
    if( rad != null ) {
      long              diskSize = 0;
      DeviceIO.DiskInfo diskInfo = rad.getDiskInfo();
      if( diskInfo != null ) {
	if( diskInfo.hasGeometry() ) {
	  int cyls            = diskInfo.getCylinders();
	  int heads           = diskInfo.getHeads();
	  int sectorsPerTrack = diskInfo.getSectorsPerTrack();
	  int sectorSize      = diskInfo.getSectorSize();
	  if( (heads > 2)
	      || (cyls > 0x7F)
	      || (sectorsPerTrack > 0x7F)
	      || (sectorSize > 0x2000) )
	  {
	    throw new IOException( "Datentr\u00E4ger ist keine Diskette." );
	  }
	  fmt = new FloppyDiskFormat(
				cyls,
				heads,
				sectorsPerTrack,
				sectorSize );
	}
	diskSize = diskInfo.getDiskSize();
      }
      if( fmt == null ) {
	for( FloppyDiskFormat tmpFmt : stdFloppyFormats35 ) {
	  if( diskSize == tmpFmt.getDiskSize() ) {
	    fmt = tmpFmt;
	    break;
	  }
	}
      }
      if( fmt == null ) {
	for( FloppyDiskFormat tmpFmt : stdFloppyFormats35 ) {
	  if( DiskUtil.equalsDiskSize( rad, tmpFmt.getDiskSize() ) ) {
	    fmt = tmpFmt;
	    break;
	  }
	}
      }
    }
    if( fmt == null ) {
      throw new IOException( "Diskettenformat unbekannt"
				+ " oder nicht unterst\u00FCtzt" );
    }
    return fmt;
  }


  public static boolean isFilledDir( byte[] dirBytes )
  {
    return getDirStatus( dirBytes ) == DirStatus.FILLED_DIR;
  }


  public static boolean isHD( int sectorsPerTrack, int sectorSize )
  {
    return ((sectorsPerTrack * sectorSize) > 7000);
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
    return extractDirectory(
		findAndReadDirBytes( null, null, null, disk, null ) );
  }


  public static java.util.List<FileEntry> readDirFromPlainDisk( File file )
  {
    java.util.List<FileEntry> rv = null;
    if( file != null ) {
      InputStream in = null;
      try {
	in = new FileInputStream( file );
	if( FileUtil.isGZipFile( file ) ) {
	  in = new GZIPInputStream( in );
	}
	rv = extractDirectory(
		findAndReadDirBytes( in, null, null, null, null ) );
      }
      catch( IOException ex ) {}
      finally {
	EmuUtil.closeSilently( in );
      }
    }
    return rv;
  }


  /*
   * Lesen einer Diskettenabbilddatei
   * Rueckgabe:
   *   null: Bei Auswahl des Formats Abbrechen gedrueckt
   */
  public static AbstractFloppyDisk readDiskFile(
				Frame   owner,
				File    file,
				boolean enableAutoRepair ) throws IOException
  {
    AbstractFloppyDisk disk = readNonPlainDiskFile(
						owner,
						file,
						enableAutoRepair );
    if( (disk == null) && (file != null) ) {
      String fName = file.getName();
      if( fName != null ) {
	fName = fName.toLowerCase();
	if( TextUtil.endsWith( fName, DiskUtil.plainDiskFileExt )
	    || TextUtil.endsWith( fName, DiskUtil.gzPlainDiskFileExt ) )
	{
	  FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			owner,
			true,
			FloppyDiskFormat.getFormatByDiskSize( file.length() ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT );
	  dlg.setVisible( true );
	  FloppyDiskFormat fmt = dlg.getFormat();
	  if( fmt != null ) {
	    disk = PlainDisk.createForByteArray(
			owner,
			file.getPath(),
			FileUtil.readFile(
					file,
					true,
					FloppyDiskFormat.getMaxDiskSize() ),
			fmt );
	  }
	} else {
	  throw new IOException(
			"Unbekanntes Format einer Diskettenabbilddatei" );
	}
      }
    }
    return disk;
  }


  public static AbstractFloppyDisk readNonPlainDiskFile(
				Frame   owner,
				File    file,
				boolean enableAutoRepair ) throws IOException
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
	byte[] header = FileUtil.readFile( file, true, 0x100 );
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
	    disk = TeleDisk.readFile( owner, file, enableAutoRepair );
	  }
	}
      }
    }
    return disk;
  }


  public static boolean recognizeBlockNumFmt(
				byte[]        dirBytes,
				long          diskSize,
				AtomicInteger blockNumSizeOut,
				AtomicInteger blockSizeOut )
  {
    boolean rv = false;
    if( dirBytes != null ) {
      boolean block8possible = true;
      int     block8Size     = 0;
      int     block16Size    = 0;
      int     pos            = 0;
      while( (block8possible || (block16Size == 0))
	     && (pos + 31) < dirBytes.length )
      {
	int b0       = (int) dirBytes[ pos ] & 0xFF;
	int nBlks128 = getEntryBlock128Count( dirBytes, pos );
	if( (b0 >= 0) && (b0 <= 0x1F) && (nBlks128 > 0) ) {
	  int entryFileSize = nBlks128 * 128;
	  if( entryFileSize > 0 ) {

	    // 8-Bit-Blocknummern pruefen
	    if( block8possible ) {
	      boolean      hasNull   = false;
	      int          tmpPos    = pos + 16;
	      int          nBlocks   = 0;
	      Set<Integer> blockNums = new TreeSet<>();
	      for( int i = 0; i < 16; i++ ) {
		int blockNum = (int) dirBytes[ tmpPos++ ] & 0xFF;
		if( blockNum == 0 ) {
		  hasNull = true;
		} else {
		  if( hasNull ) {
		    /*
		     * Bei einem Nicht-Nullbyte nach einen Nullbyte
		     * koennen es keine 8-Bit-Blocknummer sein.
		     */
		    block8possible = false;
		    break;
		  } else {
		    if( blockNums.add( blockNum ) ) {
		      nBlocks++;
		    } else {
		      /*
		       * Bei zwei gleichen Nicht-Nullbytes
		       * koennen es keine 8-Bit-Blocknummer sein.
		       */
		      block8possible = false;
		      break;
		    }
		  }
		}
	      }
	      if( block8possible && (nBlocks > 1) ) {
		int blockSize = getBlockSize( entryFileSize, nBlocks );
		if( blockSize > 0 ) {
		  if( (block8Size > 0) && (block8Size != blockSize) ) {
		    block8possible = false;
		  } else {
		    block8Size = blockSize;
		  }
		}
	      }
	    }

	    // 16-Bit-Blocknummern pruefen
	    if( block16Size == 0 ) {
	      int          tmpPos    = pos + 16;
	      int          nBlocks   = 0;
	      Set<Integer> blockNums = new TreeSet<>();
	      for( int i = 0; i < 8; i++ ) {
		int blockNum = EmuUtil.getWord( dirBytes, tmpPos );
		tmpPos += 2;
		if( blockNum == 0 ) {
		  break;
		} else {
		  if( blockNums.add( blockNum ) ) {
		    nBlocks++;
		  } else {
		    break;
		  }
		}
	      }
	      if( nBlocks > 1 ) {
		block16Size = getBlockSize( entryFileSize, nBlocks );
	      }
	    }
	  }
	}
	pos += 32;
      }
      if( !block8possible ) {
	block8Size = 0;
      }
      if( (block8Size > 0) && (block16Size > 0) ) {
	if( (block8Size * 255) < diskSize ) {
	  /*
	   * Bei dieser Blockgroesse laesst sich mit 8-Bit-Blocknummern
	   * nicht die ganze Disk adressieren.
	   */
	  block8Size = 0;
	}
      }
      if( block8Size > 0 ) {
	blockNumSizeOut.set( 8 );
	blockSizeOut.set( block8Size );
	rv = true;
      } else if( block16Size > 0 ) {
	blockNumSizeOut.set( 16 );
	blockSizeOut.set( block16Size );
	rv = true;
      }
    }
    return rv;
  }


  public static void unpackDisk(
			Window             owner,
			File               diskFile,
			AbstractFloppyDisk disk,
			boolean            forceAskLogicalFmt )
							throws IOException
  {
    int           sysTracks      = 0;
    int           blockSize      = DEFAULT_BLOCK_SIZE;
    Boolean       blockNum16Bit  = null;
    AtomicInteger rvSysBytes     = new AtomicInteger( -1 );
    AtomicInteger rvBlockNumSize = new AtomicInteger( -1 );
    AtomicInteger rvBlockSize    = new AtomicInteger( -1 );
    byte[]        dirBytes       = findAndReadDirBytes(
						null,
						null,
						null,
						disk,
						rvSysBytes );
    if( rvSysBytes.get() > 0 ) {
      int bytesPerTrack = disk.getSides()
				* disk.getSectorsPerTrack()
				* disk.getSectorSize();
      if( bytesPerTrack > 0 ) {
	sysTracks = rvSysBytes.get() / bytesPerTrack;
      }
    }
    if( recognizeBlockNumFmt(
			dirBytes,
			disk.getDiskSize(),
			rvBlockNumSize,
			rvBlockSize ) )
    {
      blockNum16Bit = Boolean.valueOf( rvBlockNumSize.get() == 16 );
      blockSize     = rvBlockSize.get();
    }
    FloppyDiskFormatDlg dlg = null;
    if( forceAskLogicalFmt ) {
      dlg = new FloppyDiskFormatDlg(
			owner,
			true,
			null,
			FloppyDiskFormatDlg.Flag.SYSTEM_TRACKS,
			FloppyDiskFormatDlg.Flag.BLOCK_SIZE,
			FloppyDiskFormatDlg.Flag.BLOCK_NUM_SIZE,
			FloppyDiskFormatDlg.Flag.APPLY_READONLY,
			FloppyDiskFormatDlg.Flag.FORCE_LOWERCASE );
      if( (dirBytes.length > 0) && (rvSysBytes.get() >= 0) ) {
	dlg.setRecognizedSysTracks( sysTracks );
      }
      if( blockNum16Bit != null ) {
	dlg.setRecognizedBlockNum16Bit( blockNum16Bit );
	if( rvBlockSize.get() > 0 ) {
	  dlg.setRecognizedBlockSize( rvBlockSize.get() );
	}
      }
    } else {
      dlg = new FloppyDiskFormatDlg(
			owner,
			true,
			null,
			FloppyDiskFormatDlg.Flag.APPLY_READONLY,
			FloppyDiskFormatDlg.Flag.FORCE_LOWERCASE );
    }
    dlg.setVisible( true );
    if( dlg.wasApproved() ) {
      if( forceAskLogicalFmt ) {
	sysTracks     = dlg.getSysTracks();
	blockSize     = dlg.getBlockSize();
	blockNum16Bit = dlg.getBlockNum16Bit();
      }
      if( (sysTracks >= 0) && (blockSize > 0) ) {
	String fileFmtText = disk.getFileFormatText();
	if( fileFmtText == null ) {
	  fileFmtText = "Diskettenabbilddatei";
	}
	File outDir = FileUtil.askForOutputDir(
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
			blockNum16Bit,
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
			-1,
			FileUtil.getHomeDirFile(),
			"diskette" ) )
      {
	rad = null;		// Schliessen verhindern
      }
    }
    finally {
      EmuUtil.closeSilently( rad );
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
      EmuUtil.closeSilently( raf );
    }
  }


	/* --- private Methoden --- */

  private static java.util.List<FileEntry> extractDirectory( byte[] dirBytes )
  {
    java.util.List<FileEntry> rv = null;
    if( dirBytes != null ) {
      if( dirBytes.length > 0 ) {
	Map<String,Long> entrySizes = new HashMap<>();
	int              entryPos   = 0;
	while( (entryPos + 15) < dirBytes.length ) {
	  int b = (int) dirBytes[ entryPos ] & 0xFF;
	  if( (b >= 0) && (b <= 15) ) {
	    int           pos = entryPos + 1;
	    StringBuilder buf = new StringBuilder( 12 );
	    for( int i = 0; i < 8; i++ ) {
	      int ch = (int) dirBytes[ pos++ ] & 0x7F;
	      if( (ch > 0x20) && (ch < 0x7F) ) {
		buf.append( (char) ch );
	      }
	    }
	    if( buf.length() > 0 ) {
	      boolean point = true;
	      for( int i = 0; i < 3; i++ ) {
		int ch = (int) dirBytes[ pos++ ] & 0x7F;
		if( (ch > 0x20) && (ch < 0x7F) ) {
		  if( point ) {
		    buf.append( '.' );
		    point = false;
		  }
		  buf.append( (char) ch );
		}
	      }
	      String eName = buf.toString();
	      long   eLen  = getEntryBlock128Count(
						dirBytes,
						entryPos ) * 128L;
	      if( eLen >= 0 ) {
		Long value = entrySizes.get( eName );
		if( value != null ) {
		  if( eLen > value.intValue() ) {
		    entrySizes.put( eName, value );
		  }
		} else {
		  entrySizes.put( eName, value );
		}
	      }
	    }
	  }
	  entryPos += 32;
	}
	Set<String> entryNames = entrySizes.keySet();
	if( entryNames != null ) {
	  int n = entryNames.size();
	  if( n > 0 ) {
	    String[] a = entryNames.toArray( new String[ n ] );
	    if( a != null ) {
	      Arrays.sort( a );
	      rv = new ArrayList<>( n );
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
	  rv = new ArrayList<>();
	}
      }
    }
    return rv;
  }


  // Die Methoden prueft Bytes auf CP/M-Directory-Eintraege
  private static DirStatus getDirStatus( byte[] dirBytes )
  {
    boolean isDir   = false;
    boolean isEmpty = false;
    if( dirBytes != null ) {
      if( dirBytes.length > 31 ) {
	isDir        = true;
	isEmpty      = true;
	int entryPos = 0;
	while( (entryPos + 31) < dirBytes.length ) {
	  int b = (int) dirBytes[ entryPos ] & 0xFF;
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
	      int ch = (int) dirBytes[ pos++ ] & 0x7F;
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
    DirStatus rv = DirStatus.NO_DIR;
    if( isDir ) {
      rv = (isEmpty ? DirStatus.EMPTY_DIR : DirStatus.FILLED_DIR );
    }
    return rv;
  }


  private static int getBlockSize( int entryFileSize, int nBlocks )
  {
    int blockSize = 0;
    if( (entryFileSize > 0) && (nBlocks > 1) ) {
      blockSize = 0x80;
      while( (blockSize * nBlocks) < entryFileSize ) {
	blockSize <<= 1;
      }
    }
    return blockSize;
  }


  private static boolean readBlock(
				byte[]                      outBuf,
				int                         blockIdx,
				DeviceIO.RandomAccessDevice rad,
				RandomAccessFile            raf,
				AbstractFloppyDisk          disk )
  {
    boolean rv = false;
    try {
      if( rad != null ) {
	rad.seek( blockIdx * outBuf.length );
	if( rad.read( outBuf, 0, outBuf.length ) == outBuf.length ) {
	  rv = true;
	}
      }
      if( raf != null ) {
	raf.seek( blockIdx * outBuf.length );
	if( raf.read( outBuf ) == outBuf.length ) {
	  rv = true;
	}
      }
      else if( disk != null ) {
	int nRemain      = outBuf.length;
	int sides        = disk.getSides();
	int sectPerTrack = disk.getSectorsPerTrack();
	int sectorSize = disk.getSectorSize();
	if( (sides > 0) && (sectPerTrack > 0) && (sectorSize > 0) ) {
	  int sectPerBlock = outBuf.length / sectorSize;
	  if( sectPerBlock > 0 ) {
	    for( int i = 0; i < sectPerBlock; i++ ) {
	      int nRead      = 0;
	      int absSectIdx = (sectPerBlock * blockIdx) + i;
	      int cyl        = absSectIdx / sectPerTrack / sides;
	      int head       = 0;
	      int sectIdx    = absSectIdx - (cyl * sectPerTrack * sides);
	      if( sectIdx >= sectPerTrack ) {
		head++;
		sectIdx -= sectPerTrack;
	      }
	      SectorData sector = disk.getSectorByID(
						cyl,
						head,
						cyl,
						head,
						sectIdx + 1,
						-1 );
	      if( sector != null ) {
		nRead = sector.read( outBuf, i * sectorSize, sectorSize );
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
    return rv;
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
    boolean       rv             = false;
    int           sysTracks      = 0;
    int           blockSize      = DEFAULT_BLOCK_SIZE;
    Boolean       blkNum16Bit    = null;
    AtomicInteger rvSysBytes     = new AtomicInteger( -1 );
    AtomicInteger rvBlockNumSize = new AtomicInteger( -1 );
    AtomicInteger rvBlockSize    = new AtomicInteger( -1 );
    byte[]        dirBytes       = findAndReadDirBytes(
						null,
						rad,
						raf,
						null,
						rvSysBytes );
    FloppyDiskFormat fmt = null;
    if( rad != null ) {
      fmt = getFloppyDiskFormat( rad );
    } else {
      fmt = FloppyDiskFormat.getFormatByDiskSize( diskSize );
    }
    if( (fmt != null) && (rvSysBytes.get() > 0) ) {
      int bytesPerTrack = fmt.getSides()
				* fmt.getSectorsPerTrack()
				* fmt.getSectorSize();
      if( bytesPerTrack > 0 ) {
	sysTracks = rvSysBytes.get() / bytesPerTrack;
      }
    }
    if( recognizeBlockNumFmt(
			dirBytes,
			diskSize,
			rvBlockNumSize,
			rvBlockSize ) )
    {
      blkNum16Bit = Boolean.valueOf( rvBlockNumSize.get() == 16 );
      blockSize   = rvBlockSize.get();
    }
    FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			owner,
			true,
			fmt,
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT,
			FloppyDiskFormatDlg.Flag.SYSTEM_TRACKS,
			FloppyDiskFormatDlg.Flag.BLOCK_SIZE,
			FloppyDiskFormatDlg.Flag.BLOCK_NUM_SIZE,
			FloppyDiskFormatDlg.Flag.APPLY_READONLY,
			FloppyDiskFormatDlg.Flag.FORCE_LOWERCASE );
    if( (dirBytes.length > 0) && (rvSysBytes.get() >= 0) && (fmt != null) ) {
      dlg.setRecognizedSysTracks( sysTracks );
    }
    if( blkNum16Bit != null ) {
      dlg.setRecognizedBlockNum16Bit( blkNum16Bit );
      if( rvBlockSize.get() > 0 ) {
	dlg.setRecognizedBlockSize( rvBlockSize.get() );
      }
    }
    dlg.setVisible( true );
    fmt = dlg.getFormat();
    if( fmt != null ) {
      sysTracks = dlg.getSysTracks();
      blockSize = dlg.getBlockSize();
      if( (sysTracks >= 0) && (blockSize > 0) ) {
	File outDir = FileUtil.askForOutputDir(
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
