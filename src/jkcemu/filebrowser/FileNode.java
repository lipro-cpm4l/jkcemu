/*
 * (c) 2008-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erweiterter Knoten im Baum des Datei-Browsers
 */

package jkcemu.filebrowser;

import java.io.*;
import java.lang.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.tree.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.text.TextUtil;


public class FileNode extends FileTreeNode
{
  private static String[] imageFileExtensions = null;

  private int                hsFileType;
  private boolean            fileChecked;
  private boolean            audioFile;
  private boolean            archiveFile;
  private boolean            binFile;
  private boolean            compressedFile;
  private boolean            headersaveFile;
  private boolean            imageFile;
  private boolean            kcBasicHeadFile;
  private boolean            kcBasicFile;
  private boolean            kcSysFile;
  private boolean            nonPlainDiskFile;
  private boolean            plainDiskFile;
  private boolean            textFile;
  private boolean            kc85TapFile;
  private boolean            z9001TapFile;
  private boolean            startableFile;
  private FileInfo           fileInfo;
  private Map<File,FileNode> fileToChild;


  public FileNode( TreeNode parent, File file, boolean fileSystemRoot )
  {
    super( parent, file, fileSystemRoot );
    this.hsFileType       = -1;
    this.fileChecked      = false;
    this.audioFile        = false;
    this.archiveFile      = false;
    this.binFile          = false;
    this.compressedFile   = false;
    this.headersaveFile   = false;
    this.imageFile        = false;
    this.kcBasicHeadFile  = false;
    this.kcBasicFile      = false;
    this.kcSysFile        = false;
    this.nonPlainDiskFile = false;
    this.plainDiskFile    = false;
    this.textFile         = false;
    this.kc85TapFile      = false;
    this.z9001TapFile     = false;
    this.startableFile    = false;
    this.fileInfo         = null;
    this.fileToChild      = null;
  }


  protected boolean fileNameEndsWith( String suffix )
  {
    boolean rv = false;
    if( (this.file != null) && (suffix != null) ) {
      String fName = this.file.getName();
      if( fName != null ) {
	rv = fName.toLowerCase().endsWith( suffix );
      }
    }
    return rv;
  }


  public FileInfo getFileInfo()
  {
    ensureFileChecked();
    return this.fileInfo;
  }


  public int getHeadersaveFileType()
  {
    ensureFileChecked();
    return this.hsFileType;
  }


  public boolean isAudioFile()
  {
    ensureFileChecked();
    return this.audioFile;
  }


  public boolean isArchiveFile()
  {
    ensureFileChecked();
    return this.archiveFile;
  }


  public boolean isBinFile()
  {
    ensureFileChecked();
    return this.binFile;
  }


  public boolean isCompressedFile()
  {
    ensureFileChecked();
    return this.compressedFile;
  }


  public boolean isHeadersaveFile()
  {
    ensureFileChecked();
    return this.headersaveFile;
  }


  public boolean isImageFile()
  {
    ensureFileChecked();
    return this.imageFile;
  }


  public boolean isKC85TapFile()
  {
    ensureFileChecked();
    return this.kc85TapFile;
  }


  public boolean isKCBasicHeadFile()
  {
    ensureFileChecked();
    return this.kcBasicHeadFile;
  }


  public boolean isKCBasicFile()
  {
    ensureFileChecked();
    return this.kcBasicFile;
  }


  public boolean isKCSysFile()
  {
    ensureFileChecked();
    return this.kcSysFile;
  }


  public boolean isNonPlainDiskFile()
  {
    ensureFileChecked();
    return this.nonPlainDiskFile;
  }


  public boolean isPlainDiskFile()
  {
    ensureFileChecked();
    return this.plainDiskFile;
  }


  public boolean isStartableFile()
  {
    ensureFileChecked();
    return this.startableFile;
  }


  public boolean isTextFile()
  {
    ensureFileChecked();
    return this.textFile;
  }


  public boolean isZ9001TapFile()
  {
    ensureFileChecked();
    return this.z9001TapFile;
  }


  public void setFile( File file )
  {
    super.setFile( file );
    this.fileChecked = false;
  }


  public void refresh(
		DefaultTreeModel model,
		boolean          forceLoadChildren,
		boolean          hiddenFiles,
		FileComparator   fileComparator )
  {
    this.fileChecked = false;
    if( forceLoadChildren || this.childrenLoaded ) {
      java.util.List<FileTreeNode> oldChildren = this.vChildren;
      this.vChildren                           = null;

      boolean fileSystemRoots = false;
      File[]  entries         = null;
      if( this.file != null ) {
	if( this.file.isDirectory() ) {
	  entries = this.file.listFiles();
	}
      } else {
	fileSystemRoots = true;
	entries         = File.listRoots();
      }

      boolean changed      = false;
      int     nChildren    = 0;
      int     nOldChildren = 0;
      if( oldChildren != null ) {
	nOldChildren = oldChildren.size();
      }
      if( entries != null ) {
	if( entries.length > 0 ) {
	  fileComparator.setForFileSystemRoots( fileSystemRoots );
	  try {
	    Arrays.sort( entries, fileComparator );
	  }
	  catch( ClassCastException ex ) {}

	  // Eintraege lesen und mit alten Eintraegen vergleichen
	  Map<File,FileNode> fileToChild = new HashMap<File,FileNode>();
	  this.vChildren = new Vector<FileTreeNode>( entries.length );
	  for( int i = 0; i < entries.length; i++ ) {
	    boolean ignore    = false;
	    File    entry     = entries[ i ];
	    String  entryName = entry.getName();
	    if( entryName != null ) {
	      if( entryName.equals( "." ) || entryName.equals( ".." ) ) {
		ignore = true;
	      }
	    }
	    if( !ignore
		&& (this.fileSystemRoot || hiddenFiles || !entry.isHidden()) )
	    {
	      boolean  equals = false;
	      FileNode child  = null;
	      if( this.fileToChild != null ) {
		child = this.fileToChild.get( entry );
	      }
	      if( child == null ) {
		child = new FileNode( this, entry, fileSystemRoots );
	      }
	      this.vChildren.add( child );
	      fileToChild.put( entry, child );
	      if( (oldChildren != null) && (nChildren < nOldChildren) ) {
		if( oldChildren.get( nChildren ) == child ) {
		  equals = true;
		}
	      }
	      if( !equals ) {
		changed = true;
	      }
	      nChildren++;
	    }
	  }
	  this.fileToChild = fileToChild;
	  this.fileChecked = false;

	  // untergeordnete Verzeichnisse aktualisieren
	  if( this.vChildren != null ) {
	    DefaultTreeModel tmpModel = null;
	    if( !changed ) {
	      tmpModel = model;
	    }
	    for( FileTreeNode child : this.vChildren ) {
	      if( child instanceof FileNode ) {
		((FileNode) child).refresh(
					tmpModel,
					false,
					hiddenFiles,
					fileComparator );
	      }
	    }
	  }
	}
      }

      // Aenderungen melden
      if( nChildren != nOldChildren ) {
	changed = true;
      }
      if( changed && (model != null) ) {
	model.nodeStructureChanged( this );
	model.reload( this );
      }
      this.childrenLoaded = true;
    }
  }


  public void refreshNodeFor(
		File             file,
		DefaultTreeModel model,
		boolean          forceLoadChildren,
		boolean          hiddenFiles,
		FileComparator   fileComparator )
  {
    if( (file != null) && this.childrenLoaded && (this.vChildren != null) ) {
      try {
	boolean done     = false;
	boolean isParent = false;
	String  path     = file.getCanonicalPath();
	if( this.file != null ) {
	  String tmpPath = this.file.getCanonicalPath();
	  if( path.equals( tmpPath ) ) {
	    refresh( model, forceLoadChildren, hiddenFiles, fileComparator );
	    done = true;
	  }
	  if( !done ) {
	    if( !tmpPath.endsWith( File.separator ) ) {
	      tmpPath += File.separator;
	    }
	    if( path.startsWith( tmpPath ) ) {
	      isParent = true;
	    }
	  }
	} else {
	  isParent = true;
	}
	if( isParent ) {
	  for( FileTreeNode child : this.vChildren ) {
	    if( child instanceof FileNode ) {
	      ((FileNode) child).refreshNodeFor(
					file,
					model,
					forceLoadChildren,
					hiddenFiles,
					fileComparator );
	    }
	  }
	}
      }
      catch( IOException ex ) {}
    }
  }


	/* --- private Methoden --- */

  private synchronized void ensureFileChecked()
  {
    if( !this.fileSystemRoot && (this.file != null) ) {
      if( !this.fileChecked ) {
	if( this.file.isFile() && this.file.canRead() ) {
	  this.hsFileType       = -1;
	  this.audioFile        = false;
	  this.archiveFile      = false;
	  this.binFile          = false;
	  this.compressedFile   = false;
	  this.headersaveFile   = false;
	  this.imageFile        = false;
	  this.kcBasicHeadFile  = false;
	  this.kcBasicFile      = false;
	  this.kcSysFile        = false;
	  this.kc85TapFile      = false;
	  this.z9001TapFile     = false;
	  this.nonPlainDiskFile = false;
	  this.plainDiskFile    = false;
	  this.textFile         = false;
	  this.startableFile    = false;
	  this.fileInfo         = null;

	  boolean done = false;

	  // Sound-Datei pruefen
	  try {
	    if( AudioSystem.getAudioFileFormat( this.file ) != null ) {
	      this.audioFile = true;
	      done           = true;
	    }
	  }
	  catch( UnsupportedAudioFileException ex1 ) {}
	  catch( IOException ex2 ) {}

	  // Dateiextension pruefen
	  if( !this.audioFile ) {
	    String fName = this.file.getName();
	    if( fName != null ) {
	      fName = fName.toLowerCase();
	      if( TextUtil.endsWith(
				fName,
				EmuUtil.archiveFileExtensions ) )
	      {
		this.archiveFile = true;
		done             = true;
	      }
	      else if( TextUtil.endsWith( fName, DiskUtil.anaDiskFileExt )
		       || TextUtil.endsWith( fName, DiskUtil.copyQMFileExt )
		       || TextUtil.endsWith( fName, DiskUtil.dskFileExt )
		       || TextUtil.endsWith( fName, DiskUtil.imageDiskFileExt )
		       || TextUtil.endsWith( fName, DiskUtil.teleDiskFileExt ) )
	      {
		this.nonPlainDiskFile = true;
		done                  = true;
	      }
	      else if( TextUtil.endsWith( fName, DiskUtil.plainDiskFileExt ) ) {
		this.plainDiskFile = true;
		done               = true;
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
		this.nonPlainDiskFile = true;
		this.compressedFile   = true;
		done                  = true;
	      }
	      else if( TextUtil.endsWith(
				fName,
				DiskUtil.gzPlainDiskFileExt ) )
	      {
		this.plainDiskFile  = true;
		this.compressedFile = true;
		done                = true;
	      }
	      else if( TextUtil.endsWith(
				fName,
				EmuUtil.textFileExtensions ) )
	      {
		this.textFile = true;
		done          = true;
	      }
	      else if( TextUtil.endsWith(
				fName,
				getImageFileExtensions() ) )
	      {
		this.imageFile = true;
		done           = true;
	      }
	      else if( fName.endsWith( ".bin" ) ) {
		this.binFile = true;
		done         = true;
	      }
	      else if( fName.endsWith( ".gz" ) ) {
		this.compressedFile = true;
		done                = true;
	      }
	    }
	  }

	  // Kopfdaten ermitteln
	  if( !done ) {
	    InputStream in = null;
	    try {
	      in = new FileInputStream( this.file );

	      byte[] header    = new byte[ 40 ];
	      int    headerLen = EmuUtil.read( in, header );
	      if( headerLen >= 3 ) {
		if( AbstractFloppyDisk.isDiskFileHeader( header ) ) {
		  this.nonPlainDiskFile = true;
		} else {
		  this.fileInfo = FileInfo.analyzeFile(
						header,
						headerLen,
						this.file );
		  if( this.fileInfo != null ) {
		    if( this.fileInfo.equalsFileFormat(
					FileInfo.KCBASIC_HEAD_PRG )
			|| this.fileInfo.equalsFileFormat(
					FileInfo.KCBASIC_HEAD_DATA )
			|| this.fileInfo.equalsFileFormat(
					FileInfo.KCBASIC_HEAD_ASC ) )
		    {
		      this.kcBasicHeadFile = true;
		    }
		    else if( this.fileInfo.equalsFileFormat(
					FileInfo.KCBASIC_PRG ) )
		    {
		      this.kcBasicFile = true;
		    }
		    else if( this.fileInfo.equalsFileFormat( FileInfo.KCB )
			     || this.fileInfo.equalsFileFormat(
							FileInfo.KCC ) )
		    {
		      this.kcSysFile = true;
		    }
		    else if( this.fileInfo.equalsFileFormat(
						FileInfo.KCTAP_KC85 )
			     || this.fileInfo.equalsFileFormat(
						FileInfo.KCTAP_BASIC_PRG )
			     || this.fileInfo.equalsFileFormat(
						FileInfo.KCTAP_BASIC_DATA )
			     || this.fileInfo.equalsFileFormat(
						FileInfo.KCTAP_BASIC_ASC ) )
		    {
		      this.kc85TapFile = true;
		    }
		    else if( this.fileInfo.equalsFileFormat(
						FileInfo.KCTAP_Z9001 ) )
		    {
		      this.z9001TapFile = true;
		    }
		    int    begAddr   = this.fileInfo.getBegAddr();
		    int    endAddr   = this.fileInfo.getEndAddr();
		    int    startAddr = this.fileInfo.getStartAddr();
		    if( (startAddr >= 0)
			&& (startAddr >= begAddr)
			&& (startAddr <= endAddr) )
		    {
		      this.startableFile = true;
		    }
		    String fileFmt = this.fileInfo.getFileFormat();
		    if( fileFmt != null ) {
		      if( fileFmt.equals( FileInfo.HEADERSAVE ) ) {
			this.headersaveFile = true;
			this.hsFileType     = this.fileInfo.getFileType();
		      }
		    }
		  }
		}
	      }
	    }
	    catch( Exception ex ) {}
	  }
	}
      }
      this.fileChecked = true;
    }
  }


  private static String[] getImageFileExtensions()
  {
    if( imageFileExtensions == null ) {
      imageFileExtensions = ImageIO.getReaderFormatNames();
      if( imageFileExtensions != null ) {
	for( int i = 0; i < imageFileExtensions.length; i++ ) {
	  String ext = imageFileExtensions[ i ];
	  if( ext.startsWith( "." ) ) {
	    imageFileExtensions[ i ] = ext.toLowerCase();
	  } else {
	    imageFileExtensions[ i ] = "." + ext.toLowerCase();
	  }
	}
      }
    }
    return imageFileExtensions;
  }
}

