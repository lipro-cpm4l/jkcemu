/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erweiterter Knoten im Baum des Datei-Browsers
 */

package jkcemu.tools.filebrowser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileActionMngr;
import jkcemu.file.FileCheckResult;
import jkcemu.file.FileNode;
import jkcemu.file.FileNodeComparator;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;


public class ExtendedFileNode extends FileNode
			implements FileActionMngr.FileObject
{
  private FileCheckResult fileCheckResult;


  public ExtendedFileNode(
			TreeNode parent,
			File     file,
			boolean  fileSystemRoot )
  {
    super( parent, file, fileSystemRoot );
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


  public void refresh(
		final DefaultTreeModel model,
		boolean                hiddenFiles,
		FileNodeComparator     comparator )
  {
    removeAllChildren();
    try {
      boolean fileSystemRoot = false;
      File[]  fileEntries    = null;
      File    file           = getFile();
      if( file != null ) {
	// Verzeichnis lesen
	if( FileUtil.isUsable( file ) ) {
	  if( file.isDirectory() ) {
	    fileEntries = file.listFiles();
	  }
	}
      } else {
	// Dateisystemwurzeln ermitteln
	fileEntries    = DeviceIO.listRoots();
	fileSystemRoot = true;
      }

      // Kindobjekte anlegen
      if( fileEntries != null ) {
	for( File entry : fileEntries ) {
	  boolean hidden    = false;
	  String  entryName = null;
	  if( fileSystemRoot ) {
	    entryName = entry.getPath();
	  } else {
	    if( FileUtil.isUsable( file ) ) {
	      hidden    = entry.isHidden();
	      entryName = FileUtil.getSystemDisplayName( entry );
	    } else {
	      entryName = TextUtil.emptyToNull( file.getName() );
	    }
	    if( entryName == null ) {
	      entryName = file.getPath();
	    }
	  }
	  if( entryName != null ) {
	    if( fileSystemRoot
		|| ((hiddenFiles || !hidden)
			&& !entryName.equals( "." )
			&& !entryName.equals( ".." )) )
	    {
	      add( new ExtendedFileNode( this, entry, fileSystemRoot ) );
	    }
	  }
	}
      }

      // Sortieren
      sort( comparator );
    }
    finally {
      if( model != null ) {
	model.nodeStructureChanged( this );
      }
    }
  }


  public java.util.List<ExtendedFileNode> refreshNodeFor(
				Path               path,
				DefaultTreeModel   model,
				boolean            hiddenFiles,
				FileNodeComparator comparator )
  {
    java.util.List<ExtendedFileNode> nodes = null;
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

  @Override
  public void setPath( Path path )
  {
    if( path != null ) {
      try {
	this.file = path.toFile();
      }
      catch( UnsupportedOperationException ex ) {}
    }
  }


	/* --- private Mathoden --- */

  private java.util.List<ExtendedFileNode> refreshNodeForInternal(
				Path               absolutePath,
				DefaultTreeModel   model,
				boolean            hiddenFiles,
				FileNodeComparator comparator )
  {
    java.util.List<ExtendedFileNode> nodes = null;
    if( absolutePath != null ) {
      try {
	boolean recursive = true;
	Path    path      = getPath();
	if( path != null ) {
	  Path myAbsPath = path.toAbsolutePath();
	  if( myAbsPath.equals( absolutePath ) ) {
	    refresh(
		model,
		hiddenFiles,
		comparator );
	    recursive = false;
	    nodes     = new ArrayList<>();
	    nodes.add( this );
	  } else if( myAbsPath.startsWith( absolutePath ) ) {
	    nodes = new ArrayList<>();
	    nodes.add( this );
	  }
	}
	if( recursive && (this.vChildren != null) ) {
	  for( FileNode child : this.vChildren ) {
	    if( child instanceof ExtendedFileNode ) {
	      java.util.List<ExtendedFileNode> tmpNodes
			= ((ExtendedFileNode) child).refreshNodeForInternal(
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
