/*
 * (c) 2009-2016 Jens Mueller
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
import java.lang.*;
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
import jkcemu.base.FileEntry;
import jkcemu.text.TextUtil;


public class DiskUtil
{
  public static final String[] anaDiskFileExt   = { ".dump" };
  public static final String[] copyQMFileExt    = { ".cqm", ".qm" };
  public static final String[] dskFileExt       = { ".dsk" };
  public static final String[] imageDiskFileExt = { ".imd" };
  public static final String[] isoFileExt       = { ".iso" };
  public static final String[] teleDiskFileExt  = { ".td0" };
  public static final String[] plainDiskFileExt = { ".img", ".image", ".raw" };

  public static final String[] gzAnaDiskFileExt   = { ".dump.gz" };
  public static final String[] gzCopyQMFileExt    = { ".cqm.gz", ".qm.gz" };
  public static final String[] gzDskFileExt       = { ".dsk.gz" };
  public static final String[] gzImageDiskFileExt = { ".imd.gz" };
  public static final String[] gzISOFileExt       = { ".iso.gz" };
  public static final String[] gzTeleDiskFileExt  = { ".td0.gz" };
  public static final String[] gzPlainDiskFileExt = {
						".img.gz",
						".image.gz",
						".raw.gz" };

  public static final int DEFAULT_BLOCK_SIZE = 2048;

  private enum DirStatus { NO_DIR, EMPTY_DIR, FILLED_DIR };


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


  public static Boolean checkBlockNum16Bit(
				byte[]        dirBytes,
				long          diskSize,
				AtomicInteger rvBlockSize )
  {
    Boolean rv        = null;
    Integer blockSize = null;
    if( dirBytes != null ) {
      int maxBlkNum8  = -1;
      int maxBlkNum16 = -1;
      int halfBlkSize = DEFAULT_BLOCK_SIZE / 2;
      if( diskSize <= 0 ) {
	diskSize = FloppyDiskFormat.getMaxDiskSize();
      }
      int pos = 0;
      while( ((rv == null) || (blockSize == null))
	     && ((pos + 31) < dirBytes.length) )
      {
	int b0  = (int) dirBytes[ pos ] & 0xFF;
	int b15 = (int) dirBytes[ pos + 15 ] & 0xFF;
	if( (b0 >= 0) && (b0 <= 0x1F) && (b15 > 0) ) {

	  /*
	   * Anzahl der benoetigten Bloecke bei Standard-
	   * und bei halber Standardblockgroesse ermitteln
	   */
	  int nBlocksHalf = ((b15 * 128) + halfBlkSize - 1)
					      / halfBlkSize;
	  int nBlocksStd  = ((b15 * 128) + DEFAULT_BLOCK_SIZE - 1)
					      / DEFAULT_BLOCK_SIZE;
	  if( nBlocksStd > 8 ) {
	    /*
	     * Bei 16-Bit-Blocknummern koennen nur max. 8 Bloecke
	     * pro Directory-Eintrag referenziert werden.
	     */
	    rv = Boolean.FALSE;
	  }

	  /*
	   * Anzahl der eingetragenen Blocknummern bei 8 Bit ermitteln,
	   * Gleichzeitig wird geprueft, ob bei den mindestens
	   * benoetigten Blocknummern (siehe nBlocksStd)
	   * Dopplungen auftreten.
	   * Wenn das der Fall ist,
	   * koennen es keine 8-Bit-Blocknummern sein.
	   */
	  int          tmpPos     = pos + 16;
	  int          nBlocks8   = 0;
	  Set<Integer> blockNums8 = new TreeSet<>();
	  for( int i = 0; i < 16; i++ ) {
	    int blockNum = (int) dirBytes[ tmpPos++ ] & 0xFF;
	    if( blockNum == 0 ) {
	      break;
	    }
	    nBlocks8++;
	    if( i < nBlocksStd ) {
	      if( blockNum > maxBlkNum8 ) {
		maxBlkNum8 = blockNum;
	      }
	      if( !blockNums8.add( blockNum ) ) {
		rv       = Boolean.TRUE;
		nBlocks8 = 0;
		break;
	      }
	    }
	  }

	  /*
	   * Anzahl der eingetragenen Blocknummern bei 16 Bit ermitteln,
	   * Gleichzeitig wird geprueft bei den mindestens
	   * benoetigten Blocknummern (siehe nBlocksStd) geprueft,
	   * ob so eine Blocknummer ausserhalb der Diskette liegt.
	   */
	  tmpPos        = pos + 16;
	  int nBlocks16 = 0;
	  for( int i = 0; i < 8; i++ ) {
	    int blockNum = EmuUtil.getWord( dirBytes, tmpPos );
	    tmpPos += 2;
	    if( blockNum == 0 ) {
	      break;
	    }
	    nBlocks16++;
	    if( i < nBlocksStd ) {
	      if( blockNum > maxBlkNum16 ) {
		maxBlkNum16 = blockNum;
	      }
	      if( (blockNum * halfBlkSize) >= diskSize ) {
		rv = Boolean.FALSE;
	      }
	    }
	  }

	  // Benoetigte und vorhandene Blockanzahlen vergleichen
	  if( nBlocks8 != nBlocks16 ) {
	    boolean only16Bit  = false;
	    boolean only8Bit   = false;
	    boolean tmp16Bit   = false;
	    int     tmpBlkSize = 0;
	    int     nMatches   = 0;
	    if( rv != null ) {
	      if( rv.booleanValue() ) {
		only16Bit = true;
	      } else {
		only8Bit = true;
	      }
	    }
	    if( nBlocksHalf > 0 ) {
	      if( !only16Bit && (nBlocksHalf == nBlocks8) ) {
		// halbe Standardblockgroesse + 8 Bit Blocknummern passt
		tmp16Bit   = false;
		tmpBlkSize = halfBlkSize;
		nMatches++;
	      }
	      if( !only8Bit && (nBlocksHalf == nBlocks16) ) {
		// halbe Standardblockgroesse + 16 Bit Blocknummern passt
		tmp16Bit   = true;
		tmpBlkSize = halfBlkSize;
		nMatches++;
	      }
	    }
	    if( nBlocksStd > 0 ) {
	      if( !only16Bit && (nBlocksStd == nBlocks8) ) {
		// Standardblockgroesse + 8 Bit Blocknummern passt
		tmp16Bit   = false;
		tmpBlkSize = DEFAULT_BLOCK_SIZE;
		nMatches++;
	      }
	      if( !only8Bit && (nBlocksStd == nBlocks16) ) {
		// Standardblockgroesse + 16 Bit Blocknummern passt
		tmp16Bit   = true;
		tmpBlkSize = DEFAULT_BLOCK_SIZE;
		nMatches++;
	      }
	    }
	    if( nMatches == 1 ) {
	      // eindeutige Kombination gefunden
	      if( rv != null ) {
		if( rv.booleanValue() == tmp16Bit ) {
		  blockSize = tmpBlkSize;
		}
	      } else {
		rv        = tmp16Bit;
		blockSize = tmpBlkSize;
	      }
	    }
	  }
	}
	pos += 32;
      }
      if( rv != null ) {
	if( blockSize == null ) {
	  if( rv.booleanValue() ) {
	    // 16-Bit-Blocknummern
	    if( (maxBlkNum16 > 0)
		&& ((maxBlkNum16 * DEFAULT_BLOCK_SIZE) >= diskSize)
		&& ((maxBlkNum16 * halfBlkSize) < diskSize) )
	    {
	      blockSize = halfBlkSize;
	    }
	  } else {
	    // 8-Bit-Blocknummern
	    if( (maxBlkNum8 > 0)
		&& ((maxBlkNum8 * DEFAULT_BLOCK_SIZE) >= diskSize)
		&& ((maxBlkNum8 * halfBlkSize) < diskSize) )
	    {
	      blockSize = halfBlkSize;
	    }
	  }
	}
      } else {
	if( (maxBlkNum16 > 0)
	    && ((maxBlkNum16 * halfBlkSize) > diskSize) )
	{
	  rv = Boolean.FALSE;
	}
      }
    }
    if( (rvBlockSize != null) && (blockSize != null) ) {
      rvBlockSize.set( blockSize.intValue() );
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


  public static boolean isFilledDir( byte[] dirBytes )
  {
    return getDirStatus( dirBytes ) == DirStatus.FILLED_DIR;
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
	if( EmuUtil.isGZipFile( file ) ) {
	  in = new GZIPInputStream( in );
	}
	rv = extractDirectory(
		findAndReadDirBytes( in, null, null, null, null ) );
      }
      catch( IOException ex ) {}
      finally {
	EmuUtil.closeSilent( in );
      }
    }
    return rv;
  }


  /*
   * Lesen einer Diskettenabbilddatei
   * Rueckgabe:
   *   null: Abbrechen gedrueckt
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
			FloppyDiskFormat.getFormatByDiskSize( file.length() ),
			FloppyDiskFormatDlg.Flag.PHYS_FORMAT );
	  dlg.setVisible( true );
	  FloppyDiskFormat fmt = dlg.getFormat();
	  if( fmt != null ) {
	    disk = PlainDisk.createForByteArray(
			owner,
			file.getPath(),
			EmuUtil.readFile(
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
	byte[] header = EmuUtil.readFile( file, true, 0x100 );
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


  public static void unpackDisk(
			Window             owner,
			File               diskFile,
			AbstractFloppyDisk disk,
			boolean            forceAskLogicalFmt )
  {
    // Vorbelegung ermitteln
    int           sysTracks     = 0;
    int           blockSize     = DEFAULT_BLOCK_SIZE;
    boolean       blockNum16Bit = true;
    AtomicInteger rvSysBytes    = new AtomicInteger( -1 );
    AtomicInteger rvBlockSize   = new AtomicInteger( -1 );
    byte[]        dirBytes      = findAndReadDirBytes(
						null,
						null,
						null,
						disk,
						rvSysBytes );
    if( rvSysBytes.get() > 0 ) {
      int bytesPerTrack = disk.getSides()
				* disk.getSectorsPerCylinder()
				* disk.getSectorSize();
      if( bytesPerTrack > 0 ) {
	sysTracks = rvSysBytes.get() / bytesPerTrack;
      }
    }
    Boolean tmp16Bit = checkBlockNum16Bit(
					dirBytes,
					disk.getDiskSize(),
					rvBlockSize );
    if( tmp16Bit != null ) {
      blockNum16Bit = tmp16Bit.booleanValue();
    }
    if( rvBlockSize.get() > 0 ) {
      blockSize = rvBlockSize.get();
    }
    FloppyDiskFormatDlg dlg = null;
    if( forceAskLogicalFmt ) {
      dlg = new FloppyDiskFormatDlg(
			owner,
			null,
			FloppyDiskFormatDlg.Flag.SYSTEM_TRACKS,
			FloppyDiskFormatDlg.Flag.BLOCK_SIZE,
			FloppyDiskFormatDlg.Flag.BLOCK_NUM_SIZE,
			FloppyDiskFormatDlg.Flag.APPLY_READONLY,
			FloppyDiskFormatDlg.Flag.FORCE_LOWERCASE );
      if( (dirBytes.length > 0) && (rvSysBytes.get() >= 0) ) {
	dlg.setRecognizedSysTracks( sysTracks );
      }
      if( tmp16Bit != null ) {
	dlg.setRecognizedBlockNum16Bit( tmp16Bit );
	if( rvBlockSize.get() > 0 ) {
	  dlg.setRecognizedBlockSize( rvBlockSize.get() );
	}
      }
    } else {
      dlg = new FloppyDiskFormatDlg(
			owner,
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
			readDiskSize( rad ),
			EmuUtil.getHomeDirFile(),
			"diskette" ) )
      {
	rad = null;		// Schliessen verhindern
      }
    }
    finally {
      EmuUtil.closeSilent( rad );
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
      EmuUtil.closeSilent( raf );
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
		    buf.append( (char) '.' );
		    point = false;
		  }
		  buf.append( (char) ch );
		}
	      }
	      int    eLen  = ((int) dirBytes[ entryPos + 15 ] & 0xFF) * 128;
	      String eName = buf.toString();
	      Long   value = entrySizes.get( eName );
	      if( value != null ) {
		value = Long.valueOf( value.longValue() + eLen );
	      } else {
		value = Long.valueOf( eLen );
	      }
	      entrySizes.put( eName, value );
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
	int nRemain    = outBuf.length;
	int sides      = disk.getSides();
	int sectPerCyl = disk.getSectorsPerCylinder();
	int sectorSize = disk.getSectorSize();
	if( (sides > 0) && (sectPerCyl > 0) && (sectorSize > 0) ) {
	  int sectPerBlock = outBuf.length / sectorSize;
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
    boolean       rv          = false;
    int           blockSize   = DEFAULT_BLOCK_SIZE;
    int           sysTracks   = 0;
    AtomicInteger rvSysBytes  = new AtomicInteger( -1 );
    AtomicInteger rvBlockSize = new AtomicInteger( -1 );
    byte[]        dirBytes    = findAndReadDirBytes(
						null,
						rad,
						raf,
						null,
						rvSysBytes );
    FloppyDiskFormat fmt = FloppyDiskFormat.getFormatByDiskSize( diskSize );
    if( (fmt != null) && (rvSysBytes.get() > 0) ) {
      int bytesPerTrack = fmt.getSides()
				* fmt.getSectorsPerCylinder()
				* fmt.getSectorSize();
      if( bytesPerTrack > 0 ) {
	sysTracks = rvSysBytes.get() / bytesPerTrack;
      }
    }
    Boolean blkNum16Bit = checkBlockNum16Bit(
					dirBytes,
					diskSize,
					rvBlockSize );
    if( rvBlockSize.get() > 0 ) {
      blockSize = rvBlockSize.get();
    }
    FloppyDiskFormatDlg dlg = new FloppyDiskFormatDlg(
			owner,
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
