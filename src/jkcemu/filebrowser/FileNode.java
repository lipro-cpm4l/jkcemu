/*
 * (c) 2008-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erweiterter Knoten im Baum des Datei-Browsers
 */

package jkcemu.filebrowser;

import java.io.*;
import java.lang.*;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.tree.*;
import jkcemu.Main;
import jkcemu.audio.AudioUtil;
import jkcemu.base.*;
import jkcemu.disk.*;
import jkcemu.image.ImgLoader;
import jkcemu.text.TextUtil;


public class FileNode extends FileTreeNode
			implements FileActionMngr.FileObject
{
  private FileCheckResult fileCheckResult;


  public FileNode( TreeNode parent, Path path, boolean fileSystemRoot )
  {
    super( parent, path, fileSystemRoot );
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
	this.fileCheckResult = FileCheckResult.checkFile(
						this.path.toFile() );
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
    Iterable<Path> entries = null;
    try {
      removeAllChildren();

      boolean fileSystemRoots = false;
      if( this.file != null ) {
	try {
	  Path path = this.file.toPath();
	  if( Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS )
	      && !Files.isSymbolicLink( path ) )
	  {
	    entries = Files.newDirectoryStream( path );
	  }
	}
	catch( InvalidPathException ex ) {}
	catch( IOException ex ) {}
      } else {
	fileSystemRoots = true;
	entries         = FileSystems.getDefault().getRootDirectories();
      }
      if( entries != null ) {
	for( Path entry : entries ) {
	  try {
	    boolean ignore    = false;
	    String  entryName = null;
	    if( fileSystemRoots ) {
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
		FileNode fn = new FileNode( this, entry, fileSystemRoots );
		fn.updNode();
		add( fn );
	      }
	    }
	  }
	  catch( IOException ex ) {}
	}

	// Sortieren
	comparator.setForFileSystemRoots( fileSystemRoots );
	sort( comparator );
      }

      // Aenderungen melden
      if( model != null ) {
	model.nodeStructureChanged( this );
	model.reload( this );
      }
      this.childrenLoaded = true;
    }
    finally {
      if( entries != null ) {
	if( entries instanceof Closeable ) {
	  EmuUtil.doClose( (Closeable) entries );
	}
      }
      this.fileCheckResult = null;
    }
  }


  public FileNode refreshNodeFor(
			Path                   path,
			DefaultTreeModel       model,
			boolean                forceLoadChildren,
			boolean                hiddenFiles,
			FileTreeNodeComparator comparator )
  {
    FileNode node = null;
    if( path != null ) {
      try {
	node = refreshNodeFor(
			path.toAbsolutePath().normalize(),
			model,
			hiddenFiles,
			comparator );
      }
      catch( Exception ex ) {}
    }
    return node;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void setPath( Path path )
  {
    this.path = path;
    this.file = null;
  }


	/* --- private Mathoden --- */

  private FileNode refreshNodeFor(
			Path                   absolutePath,
			DefaultTreeModel       model,
			boolean                hiddenFiles,
			FileTreeNodeComparator comparator )
  {
    FileNode rv = null;
    if( absolutePath != null ) {
      try {
	boolean recursive = true;
	if( this.path != null ) {
	  Path myAbsPath = this.path.toAbsolutePath();
	  if( myAbsPath.equals( absolutePath ) ) {
	    refresh( model, hiddenFiles, comparator );
	    recursive = false;
	    rv      = this;
	  } else if( myAbsPath.startsWith( absolutePath ) ) {
	    rv = this;
	  }
	}
	if( recursive && (this.vChildren != null) ) {
	  for( FileTreeNode child : this.vChildren ) {
	    if( child instanceof FileNode ) {
	      FileNode tmpNode = ((FileNode) child).refreshNodeFor(
							absolutePath,
							model,
							hiddenFiles,
							comparator );
	      if( tmpNode != null ) {
		rv = tmpNode;
	      }
	    }
	  }
	}
      }
      catch( Exception ex ) {}
    }
    return rv;
  }
}
