/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erweiterter Knoten im Baum des Datei-Browsers
 */

package jkcemu.filebrowser;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileTreeNode;
import jkcemu.base.FileTreeNodeComparator;


public class FileNode extends FileTreeNode
			implements FileActionMngr.FileObject
{
  private FileCheckResult fileCheckResult;


  public FileNode(
	TreeNode       parent,
	Path           path,
	File           file,
	boolean        fileSystemRoot,
	FileSystemView fsv )
  {
    super( parent, path, file, fileSystemRoot, fsv );
    this.fileCheckResult = null;
  }


  public boolean fileNameEndsWith( String suffix )
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


  @Override
  public FileCheckResult getCheckResult()
  {
    if( this.fileCheckResult == null ) {
      try {
	this.fileCheckResult = FileCheckResult.checkFile( getFile() );
      }
      catch( UnsupportedOperationException ex ) {}
    }
    return this.fileCheckResult;
  }


  public void refresh(
		final DefaultTreeModel model,
		boolean                hiddenFiles,
		FileTreeNodeComparator comparator )
  {
    Iterable<Path> pathEntries = null;
    removeAllChildren();
    try {
      boolean fileSystemRoot = false;
      File[]  fileEntries    = null;
      File    file           = getFile();
      if( file != null ) {

	// Verzeichnis lesen
	boolean ignore = false;
	Path    path   = getPath();
	if( path != null ) {
	  if( !Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS )
	      || Files.isSymbolicLink( path ) )
	  {
	    ignore = true;
	  }
	}
	if( !ignore ) {
	  if( this.fsv != null ) {
	    if( file.isDirectory() ) {
	      fileEntries = fsv.getFiles( file, !hiddenFiles );
	    }
	  } else {
	    try {
	      pathEntries = Files.newDirectoryStream( path );
	    }
	    catch( IOException ex ) {}
	  }
	}

      } else {

	// Dateisystemwurzeln ermitteln
	fileSystemRoot = true;
	if( this.fsv != null ) {
	  fileEntries = this.fsv.getRoots();
	} else {
	  pathEntries = FileSystems.getDefault().getRootDirectories();
	}
      }

      // Kindobjekte anlegen
      if( fileEntries != null ) {
	for( File entry : fileEntries ) {
	  boolean ignore    = false;
	  boolean hidden    = false;
	  String  entryName = null;
	  if( this.fsv != null ) {
	    entryName = this.fsv.getSystemDisplayName( entry );
	    hidden    = this.fsv.isHiddenFile( entry );
	  } else {
	    hidden = entry.isHidden();
	  }
	  if( entryName == null ) {
	    if( fileSystemRoot ) {
	      entryName = entry.getPath();
	    } else {
	      entryName = entry.getName();
	    }
	  }
	  if( entryName != null ) {
	    if( entryName.equals( "." ) || entryName.equals( ".." ) ) {
	      ignore = true;
	    }
	    if( !ignore && (hiddenFiles || !hidden) ) {
	      FileNode fn = new FileNode(
					this,
					null,
					entry,
					fileSystemRoot,
					this.fsv );
	      fn.updNode();
	      add( fn );
	    }
	  }
	}
      }
      if( pathEntries != null ) {
	for( Path entry : pathEntries ) {
	  try {
	    boolean ignore    = false;
	    String  entryName = null;
	    if( fileSystemRoot ) {
	      entryName = entry.toString();
	    } else {
	      Path namePath = entry.getFileName();
	      if( namePath != null ) {
		entryName = namePath.toString();
	      }
	    }
	    if( entryName != null ) {
	      if( entryName.equals( "." ) || entryName.equals( ".." ) ) {
		ignore = true;
	      }
	      if( !ignore && (hiddenFiles || !Files.isHidden( entry )) ) {
		FileNode fn = new FileNode(
					this,
					entry,
					null,
					fileSystemRoot,
					fileSystemRoot ? null : this.fsv );
		fn.updNode();
		add( fn );
	      }
	    }
	  }
	  catch( IOException ex ) {}
	}
      }

      // Sortieren
      comparator.setForFileSystemRoots( fileSystemRoot );
      sort( comparator );
    }
    finally {
      if( pathEntries != null ) {
	if( pathEntries instanceof Closeable ) {
	  EmuUtil.closeSilent( (Closeable) pathEntries );
	}
      }
      if( model != null ) {
	model.nodeStructureChanged( this );
      }
    }
  }


  public java.util.List<FileNode> refreshNodeFor(
			Path                   path,
			DefaultTreeModel       model,
			boolean                hiddenFiles,
			FileTreeNodeComparator comparator )
  {
    java.util.List<FileNode> nodes = null;
    if( path != null ) {
      try {
	nodes = refreshNodeForInternal(
			path.toAbsolutePath().normalize(),
			model,
			hiddenFiles,
			comparator );
      }
      catch( Exception ex ) {}
    }
    return nodes;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void setPath( Path path )
  {
    this.path = path;
    this.file = null;
  }


	/* --- private Mathoden --- */

  private java.util.List<FileNode> refreshNodeForInternal(
			Path                   absolutePath,
			DefaultTreeModel       model,
			boolean                hiddenFiles,
			FileTreeNodeComparator comparator )
  {
    java.util.List<FileNode> nodes = null;
    if( absolutePath != null ) {
      try {
	boolean recursive = true;
	if( this.path != null ) {
	  Path myAbsPath = this.path.toAbsolutePath();
	  if( myAbsPath.equals( absolutePath ) ) {
	    refresh( model, hiddenFiles, comparator );
	    recursive = false;
	    nodes     = new ArrayList<>();
	    nodes.add( this );
	  } else if( myAbsPath.startsWith( absolutePath ) ) {
	    nodes = new ArrayList<>();
	    nodes.add( this );
	  }
	}
	if( recursive && (this.vChildren != null) ) {
	  for( FileTreeNode child : this.vChildren ) {
	    if( child instanceof FileNode ) {
	      java.util.List<FileNode> tmpNodes
			= ((FileNode) child).refreshNodeForInternal(
							absolutePath,
							model,
							hiddenFiles,
							comparator );
	      if( tmpNodes != null ) {
		if( nodes != null ) {
		  nodes.addAll( tmpNodes );
		} else {
		  nodes = tmpNodes;
		}
	      }
	    }
	  }
	}
      }
      catch( Exception ex ) {}
    }
    return nodes;
  }
}
