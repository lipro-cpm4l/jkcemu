/*
 * (c) 2014-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Informationen ueber eine Datei
 */

package jkcemu.filebrowser;

import java.io.*;
import java.lang.*;
import jkcemu.audio.AudioUtil;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.image.ImgLoader;
import jkcemu.text.TextUtil;


public class FileCheckResult
{
  private int      hsFileType;
  private boolean  audioFile;
  private boolean  archiveFile;
  private boolean  binFile;
  private boolean  compressedFile;
  private boolean  headersaveFile;
  private boolean  imageFile;
  private boolean  kcBasicHeadFile;
  private boolean  kcBasicFile;
  private boolean  kcSysFile;
  private boolean  nonPlainDiskFile;
  private boolean  plainDiskFile;
  private boolean  tapeFile;
  private boolean  textFile;
  private boolean  kc85TapFile;
  private boolean  z9001TapFile;
  private boolean  startableFile;
  private FileInfo fileInfo;


  public static FileCheckResult checkFile( File file )
  {
    FileCheckResult rv = null;
    if( file != null ) {
      int      hsFileType       = -1;
      boolean  audioFile        = false;
      boolean  archiveFile      = false;
      boolean  binFile          = false;
      boolean  compressedFile   = false;
      boolean  headersaveFile   = false;
      boolean  imageFile        = false;
      boolean  kcBasicHeadFile  = false;
      boolean  kcBasicFile      = false;
      boolean  kcSysFile        = false;
      boolean  kc85TapFile      = false;
      boolean  z9001TapFile     = false;
      boolean  nonPlainDiskFile = false;
      boolean  plainDiskFile    = false;
      boolean  tapeFile         = false;
      boolean  textFile         = false;
      boolean  startableFile    = false;
      FileInfo fileInfo         = null;

      if( file.isFile() && file.canRead() ) {
	boolean done = false;

	// Sound-Datei pruefen
	if( AudioUtil.isAudioFile( file ) ) {
	  audioFile = true;
	  done      = true;
	}

	// Bilddatei pruefen
	if( ImgLoader.accepts( file ) ) {
	  imageFile = true;
	  done      = true;
	}

	/*
	 * Dateiextension pruefen,
	 * auch bei Audio und Bilddatei (z.B. wegen *.img und *.gz)
	 */
	String fName = file.getName();
	if( fName != null ) {
	  fName = fName.toLowerCase();
	  if( TextUtil.endsWith(
			    fName,
			    EmuUtil.archiveFileExtensions ) )
	  {
	    archiveFile = true;
	    done        = true;
	  }
	  else if( TextUtil.endsWith( fName, DiskUtil.anaDiskFileExt )
		   || TextUtil.endsWith( fName, DiskUtil.copyQMFileExt )
		   || TextUtil.endsWith( fName, DiskUtil.dskFileExt )
		   || TextUtil.endsWith( fName, DiskUtil.imageDiskFileExt )
		   || TextUtil.endsWith( fName, DiskUtil.teleDiskFileExt ) )
	  {
	    nonPlainDiskFile = true;
	    done             = true;
	  }
	  else if( TextUtil.endsWith( fName, DiskUtil.plainDiskFileExt ) ) {
	    plainDiskFile = true;
	    done          = true;
	  }
	  else if( TextUtil.endsWith(
				fName,
				DiskUtil.gzAnaDiskFileExt )
		   || TextUtil.endsWith(
				fName,
				DiskUtil.gzCopyQMFileExt )
		   || TextUtil.endsWith(
				fName,
				DiskUtil.gzDskFileExt )
		   || TextUtil.endsWith(
				fName,
				DiskUtil.gzImageDiskFileExt )
		   || TextUtil.endsWith(
				fName,
				DiskUtil.gzTeleDiskFileExt ) )
	  {
	    nonPlainDiskFile = true;
	    compressedFile   = true;
	    done             = true;
	  }
	  else if( TextUtil.endsWith(
				fName,
				DiskUtil.gzPlainDiskFileExt ) )
	  {
	    plainDiskFile  = true;
	    compressedFile = true;
	    done           = true;
	  }
	  else if( TextUtil.endsWith(
				fName,
				AudioUtil.tapeFileExtensions ) )
	  {
	    tapeFile = true;
	    done     = true;
	  }
	  else if( TextUtil.endsWith(
				fName,
				EmuUtil.textFileExtensions ) )
	  {
	    textFile = true;
	    done     = true;
	  }
	  else if( fName.endsWith( ".bin" ) ) {
	    binFile = true;
	    done    = true;
	  }
	  else if( fName.endsWith( ".gz" ) ) {
	    compressedFile = true;
	    done           = true;
	  }
	}

	// Kopfdaten ermitteln
	if( !done ) {
	  InputStream in = null;
	  try {
	    in = new FileInputStream( file );

	    byte[] header    = new byte[ 40 ];
	    int    headerLen = EmuUtil.read( in, header );
	    if( headerLen >= 3 ) {
	      if( AbstractFloppyDisk.isDiskFileHeader( header ) ) {
		nonPlainDiskFile = true;
	      } else {
		fileInfo = FileInfo.analyzeFile( header, headerLen, file );
		if( fileInfo != null ) {
		  if( fileInfo.equalsFileFormat(
					FileFormat.KCBASIC_HEAD_PRG )
		      || fileInfo.equalsFileFormat(
					FileFormat.KCBASIC_HEAD_DATA )
		      || fileInfo.equalsFileFormat(
					FileFormat.KCBASIC_HEAD_ASC ) )
		  {
		    kcBasicHeadFile = true;
		  }
		  else if( fileInfo.equalsFileFormat(
					FileFormat.KCBASIC_PRG ) )
		  {
		    kcBasicFile = true;
		  }
		  else if( fileInfo.equalsFileFormat( FileFormat.KCB )
			   || fileInfo.equalsFileFormat( FileFormat.KCC ) )
		  {
		    kcSysFile = true;
		  }
		  else if( fileInfo.equalsFileFormat(
					FileFormat.KCTAP_KC85 )
			   || fileInfo.equalsFileFormat(
					FileFormat.KCTAP_BASIC_PRG )
			   || fileInfo.equalsFileFormat(
					FileFormat.KCTAP_BASIC_DATA )
			   || fileInfo.equalsFileFormat(
					FileFormat.KCTAP_BASIC_ASC ) )
		  {
		    kc85TapFile = true;
		  }
		  else if( fileInfo.equalsFileFormat(
					FileFormat.KCTAP_Z9001 ) )
		  {
		    z9001TapFile = true;
		  }
		  int begAddr   = fileInfo.getBegAddr();
		  int endAddr   = fileInfo.getEndAddr();
		  int startAddr = fileInfo.getStartAddr();
		  if( (startAddr >= 0)
		      && (startAddr >= begAddr)
		      && (startAddr <= endAddr) )
		  {
		    startableFile = true;
		  }
		  FileFormat fileFmt = fileInfo.getFileFormat();
		  if( fileFmt != null ) {
		    if( fileFmt.equals( FileFormat.HEADERSAVE ) ) {
		      headersaveFile = true;
		      hsFileType     = fileInfo.getFileType();
		    }
		  }
		}
	      }
	    }
	  }
	  catch( Exception ex ) {}
	  finally {
	    EmuUtil.doClose( in );
	  }
	}
      }
      rv = new FileCheckResult(
			hsFileType,
			audioFile,
			archiveFile,
			binFile,
			compressedFile,
			headersaveFile,
			imageFile,
			kcBasicHeadFile,
			kcBasicFile,
			kcSysFile,
			nonPlainDiskFile,
			plainDiskFile,
			textFile,
			kc85TapFile,
			z9001TapFile,
			startableFile,
			fileInfo );
    }
    return rv;
  }


  public FileInfo getFileInfo()
  {
    return this.fileInfo;
  }


  public int getHeadersaveFileType()
  {
    return this.hsFileType;
  }


  public boolean isAudioFile()
  {
    return this.audioFile;
  }


  public boolean isArchiveFile()
  {
    return this.archiveFile;
  }


  public boolean isBinFile()
  {
    return this.binFile;
  }


  public boolean isCompressedFile()
  {
    return this.compressedFile;
  }


  public boolean isHeadersaveFile()
  {
    return this.headersaveFile;
  }


  public boolean isImageFile()
  {
    return this.imageFile;
  }


  public boolean isKC85TapFile()
  {
    return this.kc85TapFile;
  }


  public boolean isKCBasicHeadFile()
  {
    return this.kcBasicHeadFile;
  }


  public boolean isKCBasicFile()
  {
    return this.kcBasicFile;
  }


  public boolean isKCSysFile()
  {
    return this.kcSysFile;
  }


  public boolean isNonPlainDiskFile()
  {
    return this.nonPlainDiskFile;
  }


  public boolean isPlainDiskFile()
  {
    return this.plainDiskFile;
  }


  public boolean isStartableFile()
  {
    return this.startableFile;
  }


  public boolean isTapeFile()
  {
    return this.tapeFile;
  }


  public boolean isTextFile()
  {
    return this.textFile;
  }


  public boolean isZ9001TapFile()
  {
    return this.z9001TapFile;
  }


	/* --- Konstruktor --- */

  private FileCheckResult(
		int      hsFileType,
		boolean  audioFile,
		boolean  archiveFile,
		boolean  binFile,
		boolean  compressedFile,
		boolean  headersaveFile,
		boolean  imageFile,
		boolean  kcBasicHeadFile,
		boolean  kcBasicFile,
		boolean  kcSysFile,
		boolean  nonPlainDiskFile,
		boolean  plainDiskFile,
		boolean  textFile,
		boolean  kc85TapFile,
		boolean  z9001TapFile,
		boolean  startableFile,
		FileInfo fileInfo )
  {
    this.hsFileType       = hsFileType;
    this.audioFile        = audioFile;
    this.archiveFile      = archiveFile;
    this.binFile          = binFile;
    this.compressedFile   = compressedFile;
    this.headersaveFile   = headersaveFile;
    this.imageFile        = imageFile;
    this.kcBasicHeadFile  = kcBasicHeadFile;
    this.kcBasicFile      = kcBasicFile;
    this.kcSysFile        = kcSysFile;
    this.nonPlainDiskFile = nonPlainDiskFile;
    this.plainDiskFile    = plainDiskFile;
    this.textFile         = textFile;
    this.kc85TapFile      = kc85TapFile;
    this.z9001TapFile     = z9001TapFile;
    this.startableFile    = startableFile;
    this.fileInfo         = fileInfo;
  }
}
