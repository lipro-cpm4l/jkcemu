/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Knoten im Baum des Datei-Browsers
 */

package jkcemu.filebrowser;

import java.io.*;
import java.lang.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.tree.*;
import jkcemu.base.*;


public class FileNode implements TreeNode
{
  private static final String[] archiveFileExtensions  = {
					".JAR", ".TAR.GZ", ".TAR", ".TGZ",
					".ZIP" };

  private static final String[] textFileExtensions  = {
					".ASC", ".ASM", ".BAS", ".BAT",
					".C", ".CC", ".CMD", ".CPP", ".CSH",
					".H", ".JAVA", ".LOG", ".SH", ".TXT" };

  private static String[] imageFileExtensions = null;

  private TreeNode           parent;
  private File               file;
  private int                hsFileType;
  private boolean            fileSystemRoot;
  private boolean            fileChecked;
  private boolean            audioFile;
  private boolean            archiveFile;
  private boolean            compressedFile;
  private boolean            headersaveFile;
  private boolean            imageFile;
  private boolean            textFile;
  private boolean            startableFile;
  private boolean            childrenLoaded;
  private boolean            leaf;
  private FileInfo           fileInfo;
  private Vector<FileNode>   vChildren;
  private Map<File,FileNode> fileToChild;
  private String             nodeName;


  public FileNode( TreeNode parent, File file, boolean fileSystemRoot )
  {
    this.parent         = parent;
    this.file           = file;
    this.hsFileType     = -1;
    this.fileSystemRoot = fileSystemRoot;
    this.fileChecked    = false;
    this.audioFile      = false;
    this.archiveFile    = false;
    this.compressedFile = false;
    this.headersaveFile = false;
    this.imageFile      = false;
    this.textFile       = false;
    this.startableFile  = false;
    this.childrenLoaded = false;
    this.leaf           = !fileSystemRoot;
    this.fileInfo       = null;
    this.vChildren      = null;
    this.fileToChild    = null;
    if( this.leaf && (this.file != null) ) {
      if( this.file.isDirectory() )
	this.leaf = false;
    }
    updNodeText();
  }


  public void add( FileNode fileNode )
  {
    if( this.vChildren == null ) {
      this.vChildren = new Vector<FileNode>();
    }
    this.vChildren.add( fileNode );
    this.leaf = false;
  }


  public boolean fileNameEndsWith( String suffix )
  {
    boolean rv = false;
    if( (this.file != null) && (suffix != null) ) {
      String fName = this.file.getName();
      if( fName != null ) {
	rv = fName.toUpperCase( Locale.ENGLISH ).endsWith(
			suffix.toUpperCase( Locale.ENGLISH ) );
      }
    }
    return rv;
  }


  public File getFile()
  {
    return this.file;
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


  public boolean isCompressedFile()
  {
    ensureFileChecked();
    return this.compressedFile;
  }


  public boolean isFileSystemRoot()
  {
    return this.fileSystemRoot;
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


  public void setFile( File file )
  {
    this.file        = file;
    this.fileChecked = false;
    updNodeText();
  }


  public void refresh(
		DefaultTreeModel model,
		boolean          forceLoadChildren,
		boolean          hiddenFiles,
		FileComparator   fileComparator )
  {
    if( forceLoadChildren || this.childrenLoaded ) {
      java.util.List<FileNode> oldChildren = this.vChildren;
      this.vChildren                       = null;

      boolean fileSystemRoots = false;
      File[]  entries         = null;
      if( this.file != null ) {
	if( this.file.isDirectory() )
	  entries = this.file.listFiles();
      } else {
	fileSystemRoots = true;
	entries         = File.listRoots();
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
	  this.vChildren = new Vector<FileNode>( entries.length );

	  boolean changed      = false;
	  int     nChildren    = 0;
	  int     nOldChildren = 0;
	  if( oldChildren != null ) {
	    nOldChildren = oldChildren.size();
	  }
	  for( int i = 0; i < entries.length; i++ ) {
	    boolean ignore    = false;
	    File    entry     = entries[ i ];
	    String  entryName = entry.getName();
	    if( entryName != null ) {
	      if( entryName.equals( "." ) || entryName.equals( ".." ) )
		ignore = true;
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
		if( oldChildren.get( nChildren ) == child )
		  equals = true;
	      }
	      if( !equals ) {
		changed = true;
	      }
	      nChildren++;
	    }
	  }
	  this.fileToChild = fileToChild;
	  if( !changed ) {
	    int nOld = 0;
	    if( oldChildren != null ) {
	      nOld = oldChildren.size();
	    }
	    if( nOld != this.vChildren.size() ) {
	      changed = true;
	    }
	  }

	  // untergeordnete Verzeichnisse aktualisieren
	  if( this.vChildren != null ) {
	    DefaultTreeModel tmpModel = null;
	    if( !changed ) {
	      tmpModel = model;
	    }
	    for( FileNode child : this.vChildren ) {
	      child.refresh( tmpModel, false, hiddenFiles, fileComparator );
	    }
	  }

	  // Aenderungen melden
	  if( changed && (model != null) ) {
	    model.nodeStructureChanged( this );
	  }
	}
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
	  for( FileNode child : this.vChildren ) {
	    child.refreshNodeFor(
			file,
			model,
			forceLoadChildren,
			hiddenFiles,
			fileComparator );
	  }
	}
      }
      catch( IOException ex ) {}
    }
  }


  public void removeAllChildren()
  {
    this.vChildren.clear();
  }


  public void removeFromParent()
  {
    if( this.parent != null ) {
      if( this.parent instanceof FileNode )
	((FileNode) this.parent).vChildren.remove( this );
    }
  }


  public String toString()
  {
    return this.nodeName;
  }


	/* --- TreeNode --- */

  public Enumeration children()
  {
    if( this.vChildren == null ) {
      this.vChildren = new Vector<FileNode>();
    }
    return this.vChildren.elements();
  }


  public boolean getAllowsChildren()
  {
    return !this.leaf;
  }


  public TreeNode getChildAt( int pos )
  {
    if( this.vChildren != null ) {
      if( (pos >= 0) && (pos < this.vChildren.size()) )
	return this.vChildren.get( pos );
    }
    return null;
  }


  public int getChildCount()
  {
    return this.vChildren != null ? this.vChildren.size() : 0;
  }


  public int getIndex( TreeNode item )
  {
    return this.vChildren != null ? this.vChildren.indexOf( item ) : -1;
  }


  public TreeNode getParent()
  {
    return this.parent;
  }


  public boolean isLeaf()
  {
    return this.leaf;
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


  private void ensureFileChecked()
  {
    if( !this.fileSystemRoot && !this.fileChecked ) {
      if( this.file != null ) {
	if( this.file.isFile() && this.file.canRead() ) {
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
	      fName = fName.toUpperCase();
	      if( fName.endsWith( ".GZ" ) ) {
		this.compressedFile = true;
		done                = true;
	      }
	      if( endsWith( fName, archiveFileExtensions ) ) {
		this.archiveFile = true;
		done             = true;
	      } else if( endsWith( fName, textFileExtensions ) ) {
		this.textFile = true;
		done          = true;
	      } else if( endsWith( fName, getImageFileExtensions() ) ) {
		this.imageFile = true;
		done           = true;
	      }
	    }
	  }

	  // Kopfdaten ermitteln
	  if( !done ) {
	    this.fileInfo = FileInfo.analyzeFile( this.file );
	    if( this.fileInfo != null ) {
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
	    imageFileExtensions[ i ] = ext.toUpperCase();
	  } else {
	    imageFileExtensions[ i ] = "." + ext.toUpperCase();
	  }
	}
      }
    }
    return imageFileExtensions;
  }


  private void updNodeText()
  {
    this.nodeName = null;
    if( this.file != null ) {
      this.nodeName = (isFileSystemRoot() ?
				this.file.getPath()
				: this.file.getName());
    }
    if( this.nodeName == null ) {
      this.nodeName = "?";
    }
  }
}

