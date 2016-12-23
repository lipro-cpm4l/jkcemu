/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Knoten im Baum eines Datei-Browsers
 */

package jkcemu.base;

import java.io.File;
import java.lang.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.tree.TreeNode;


public class FileTreeNode implements TreeNode
{
  protected Path                 path;
  protected File                 file;
  protected boolean              fileSystemRoot;
  protected boolean              childrenLoaded;
  protected Vector<FileTreeNode> vChildren;

  private TreeNode parent;
  private boolean  allowsChildren;
  private boolean  leaf;
  private String   nodeName;


  public FileTreeNode( TreeNode parent, Path path, boolean fileSystemRoot )
  {
    this.parent         = parent;
    this.path           = path;
    this.file           = null;
    this.fileSystemRoot = fileSystemRoot;
    this.allowsChildren = fileSystemRoot;
    this.leaf           = !fileSystemRoot;
    this.childrenLoaded = false;
    this.vChildren      = null;
    updNode();
  }


  public void add( FileTreeNode fileNode )
  {
    if( this.vChildren == null ) {
      this.vChildren = new Vector<>();
    }
    this.vChildren.add( fileNode );
    this.allowsChildren = true;
    this.leaf           = false;
  }


  public synchronized File getFile()
  {
    if( (this.file == null) && (this.path != null) ) {
      try {
	this.file = this.path.toFile();
      }
      catch( UnsupportedOperationException ex ) {}
    }
    return this.file;
  }


  public Path getPath()
  {
    return this.path;
  }


  public boolean hasChildrenLoaded()
  {
    return this.childrenLoaded;
  }


  public boolean isFileSystemRoot()
  {
    return this.fileSystemRoot;
  }


  public void removeAllChildren()
  {
    if( this.vChildren != null )
      this.vChildren.clear();
  }


  public void removeFromParent()
  {
    if( this.parent != null ) {
      if( this.parent instanceof FileTreeNode ) {
	FileTreeNode node = (FileTreeNode) this.parent;
	if( node.vChildren != null ) {
	  node.vChildren.remove( this );
	}
      }
    }
  }


  public void setChildrenLoaded( boolean state )
  {
    this.childrenLoaded = state;
  }


  public void setFile( File file )
  {
    this.path = null;
    this.file = file;
    if( file != null ) {
      try {
	this.path = file.toPath();
      }
      catch( InvalidPathException ex ) {}
    }
    updNode();
  }


  public void sort( FileTreeNodeComparator comparator )
  {
    if( (this.vChildren != null) && (comparator != null) ) {
      try {
	Collections.sort( this.vChildren, comparator );
      }
      catch( ClassCastException ex ) {}
    }
  }


  protected void updNode()
  {
    if( this.path != null ) {
      if( this.fileSystemRoot ) {
	this.nodeName = this.path.toString();
      } else {
	Path p = null;
	int  n = this.path.getNameCount();
	if(  n > 0 ) {
	  p = this.path.getName( n - 1 );
	}
	if( p == null ) {
	  p = this.path;
	}
	this.nodeName = p.toString();
	if( Files.isDirectory( this.path ) ) {
	  this.allowsChildren = true;
	  this.leaf           = false;
	} else {
	  this.allowsChildren = false;
	  this.leaf           = true;
	}
	try {
	  if( Files.isSymbolicLink( path ) ) {
	    StringBuilder buf = new StringBuilder( 256 );
	    if( this.nodeName != null ) {
	      buf.append( this.nodeName );
	    }
	    buf.append( " \u2192" );
	    Path targetPath = Files.readSymbolicLink( path );
	    if( targetPath != null ) {
	      String s = targetPath.toString();
	      if( s != null ) {
		if( !s.isEmpty() ) {
		  buf.append( (char) '\u0020' );
		  buf.append( s );
		}
	      }
	    }
	    this.allowsChildren = false;
	    this.nodeName       = buf.toString();
	  }
	}
	catch( Exception ex ) {
	  this.allowsChildren = false;
	}
      }
    }
    if( this.nodeName == null ) {
      this.nodeName = "?";
    }
  }


	/* --- TreeNode --- */

  @Override
  public Enumeration children()
  {
    if( this.vChildren == null ) {
      this.vChildren = new Vector<>();
    }
    return this.vChildren.elements();
  }


  @Override
  public boolean getAllowsChildren()
  {
    return this.allowsChildren;
  }


  @Override
  public TreeNode getChildAt( int pos )
  {
    TreeNode rv = null;
    if( this.vChildren != null ) {
      if( (pos >= 0) && (pos < this.vChildren.size()) ) {
	rv = this.vChildren.get( pos );
      }
    }
    return rv;
  }


  @Override
  public int getChildCount()
  {
    return this.vChildren != null ? this.vChildren.size() : 0;
  }


  @Override
  public int getIndex( TreeNode item )
  {
    return this.vChildren != null ? this.vChildren.indexOf( item ) : -1;
  }


  @Override
  public TreeNode getParent()
  {
    return this.parent;
  }


  @Override
  public boolean isLeaf()
  {
    return this.leaf;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.nodeName;
  }
}

