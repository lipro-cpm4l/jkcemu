/*
 * (c) 2008-2011 Jens Mueller
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
import jkcemu.disk.DiskUtil;


public class FileNode extends FileTreeNode
{
  private static final String[] archiveFileExtensions  = {
					".jar", ".tar.gz", ".tar", ".tgz",
					".zip" };

  private static final String[] textFileExtensions  = {
					".asc", ".asm", ".bas", ".bat",
					".c", ".cc", ".cmd", ".cpp", ".csh",
					".h", ".java", ".log", ".sh", ".txt" };

  private static String[] imageFileExtensions = null;

  private int                hsFileType;
  private boolean            fileChecked;
  private boolean            audioFile;
  private boolean            archiveFile;
  private boolean            compressedFile;
  private boolean            headersaveFile;
  private boolean            imageFile;
  private boolean            plainDiskFile;
  private boolean            anadiskFile;
  private boolean            copyQMFile;
  private boolean            telediskFile;
  private boolean            textFile;
  private boolean            tapFile;
  private boolean            startableFile;
  private EmuSys             emuSys;
  private FileInfo           fileInfo;
  private Map<File,FileNode> fileToChild;


  public FileNode( TreeNode parent, File file, boolean fileSystemRoot )
  {
    super( parent, file, fileSystemRoot );
    this.hsFileType     = -1;
    this.fileChecked    = false;
    this.audioFile      = false;
    this.archiveFile    = false;
    this.compressedFile = false;
    this.headersaveFile = false;
    this.imageFile      = false;
    this.plainDiskFile  = false;
    this.anadiskFile    = false;
    this.copyQMFile     = false;
    this.telediskFile   = false;
    this.textFile       = false;
    this.tapFile        = false;
    this.startableFile  = false;
    this.emuSys         = null;
    this.fileInfo       = null;
    this.fileToChild    = null;
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


  public boolean isAnadiskFile()
  {
    ensureFileChecked();
    return this.anadiskFile;
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


  public boolean isCompressedFile()
  {
    ensureFileChecked();
    return this.compressedFile;
  }


  public boolean isCopyQMFile()
  {
    ensureFileChecked();
    return this.copyQMFile;
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


  public boolean isTAPFile()
  {
    ensureFileChecked();
    return this.tapFile;
  }


  public boolean isTelediskFile()
  {
    ensureFileChecked();
    return this.telediskFile;
  }


  public boolean isTextFile()
  {
    ensureFileChecked();
    return this.textFile;
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

  private static boolean endsWith( String text, String[] extensions )
  {
    if( (text != null) && (extensions != null) ) {
      for( int i = 0; i < extensions.length; i++ ) {
	if( text.endsWith( extensions[ i ] ) )
	  return true;
      }
    }
    return false;
  }


  private synchronized void ensureFileChecked()
  {
    if( !this.fileSystemRoot && (this.file != null) ) {
      EmuSys emuSys = Main.getScreenFrm().getEmuSys();
      if( !this.fileChecked || (emuSys != this.emuSys) ) {
	if( this.file.isFile() && this.file.canRead() ) {
	  this.hsFileType     = -1;
	  this.audioFile      = false;
	  this.archiveFile    = false;
	  this.compressedFile = false;
	  this.headersaveFile = false;
	  this.imageFile      = false;
	  this.plainDiskFile  = false;
	  this.anadiskFile    = false;
	  this.copyQMFile     = false;
	  this.telediskFile   = false;
	  this.textFile       = false;
	  this.tapFile        = false;
	  this.startableFile  = false;
	  this.fileInfo       = null;

	  boolean done = false;

	  // Audio-Datei pruefen
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
	      if( endsWith( fName, archiveFileExtensions ) ) {
		this.archiveFile = true;
		done             = true;
	      } else if( endsWith( fName, DiskUtil.anadiskFileExt ) ) {
		this.anadiskFile = true;
		done             = true;
	      } else if( endsWith( fName, DiskUtil.plainDiskFileExt ) ) {
		this.plainDiskFile = true;
		done               = true;
	      } else if( endsWith( fName, DiskUtil.gzAnadiskFileExt ) ) {
		this.anadiskFile    = true;
		this.compressedFile = true;
		done                = true;
	      } else if( endsWith( fName, DiskUtil.gzCopyQMFileExt ) ) {
		this.copyQMFile     = true;
		this.compressedFile = true;
		done                = true;
	      } else if( endsWith( fName, DiskUtil.gzTelediskFileExt ) ) {
		this.telediskFile   = true;
		this.compressedFile = true;
		done                = true;
	      } else if( endsWith( fName, DiskUtil.gzPlainDiskFileExt ) ) {
		this.plainDiskFile  = true;
		this.compressedFile = true;
		done                = true;
	      } else if( endsWith( fName, textFileExtensions ) ) {
		this.textFile = true;
		done          = true;
	      } else if( endsWith( fName, getImageFileExtensions() ) ) {
		this.imageFile = true;
		done           = true;
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

	      byte[] head    = new byte[ 40 ];
	      int    headLen = EmuUtil.read( in, head );
	      if( headLen >= 3 ) {
		if( (head[ 0 ] == 'C')
		    && (head[ 1 ] == 'Q')
		    && (head[ 2 ] == 0x14) )
		{
		  this.copyQMFile = true;
		} else if( (((head[ 0 ] == 'T') && (head[ 1 ] == 'D'))
				|| ((head[ 0 ] == 't') && (head[ 1 ] == 'd')))
			   && (head[ 2 ] == 0) )
		{
		  this.telediskFile = true;
		} else {
		  this.fileInfo = FileInfo.analyzeFile(
						head,
						headLen,
						this.file );
		  if( this.fileInfo != null ) {
		    if( this.fileInfo.equalsFileFormat( FileInfo.KCTAP_SYS )
			|| this.fileInfo.equalsFileFormat(
						FileInfo.KCTAP_BASIC_PRG )
			|| this.fileInfo.equalsFileFormat(
						FileInfo.KCTAP_BASIC_DATA )
			|| this.fileInfo.equalsFileFormat(
						FileInfo.KCTAP_BASIC_ASC ) )
		    {
		      this.tapFile = true;
		    }
		    int    begAddr   = this.fileInfo.getBegAddr();
		    int    endAddr   = this.fileInfo.getEndAddr();
		    int    startAddr = this.fileInfo.getStartAddr();
		    if( (startAddr >= begAddr) && (startAddr <= endAddr) ) {
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
	this.emuSys = emuSys;
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

