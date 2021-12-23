/*
 * (c) 2009-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Knoten im Baum einer Anzeige der Dateisystemstruktur
 */

package jkcemu.file;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.Icon;
import javax.swing.tree.TreeNode;
import jkcemu.Main;
import jkcemu.base.DeviceIO;


public class FileNode implements TreeNode
{
  protected File             file;
  protected boolean          fileSystemRoot;
  protected boolean          childrenLoaded;
  protected Vector<FileNode> vChildren;

  private TreeNode parent;
  private boolean  allowsChildren;
  private boolean  leaf;
  private Icon     nodeIcon;
  private String   nodeName;


  public FileNode(
		TreeNode parent,
		File     file,
		boolean  fileSystemRoot )
  {
    this.parent         = parent;
    this.fileSystemRoot = fileSystemRoot;
    this.allowsChildren = false;
    this.leaf           = false;
    this.childrenLoaded = false;
    this.vChildren      = null;
    this.nodeIcon       = null;
    this.nodeName       = null;
    setFile( file );
  }


  public void add( FileNode fileNode )
  {
    if( this.vChildren == null ) {
      this.vChildren = new Vector<>();
    }
    this.vChildren.add( fileNode );
    this.allowsChildren = true;
    this.leaf           = false;
  }


  public FileNode getChildByFile( File file )
  {
    FileNode rv = null;
    if( file != null ) {
      for( FileNode child : this.vChildren ) {
	File f = child.getFile();
	if( f != null ) {
	  if( f.equals( file ) ) {
	    rv = child;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  public File getFile()
  {
    return this.file;
  }


  public Icon getNodeIcon()
  {
    return this.nodeIcon;
  }


  public Path getPath()
  {
    Path path = null;
    if( FileUtil.isUsable( this.file ) ) {
      try {
	path = this.file.toPath();
      }
      catch( InvalidPathException ex ) {}
    }
    return path;
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
      if( this.parent instanceof FileNode ) {
	FileNode node = (FileNode) this.parent;
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
    this.file = file;
    updNode();
  }


  public void sort( FileNodeComparator comparator )
  {
    if( (this.vChildren != null) && (comparator != null) ) {
      try {
	Collections.sort( this.vChildren, comparator );
      }
      catch( ClassCastException ex ) {}
    }
  }


  /*
   * Aktualisierung des Icons und des Textes des Knotens
   *
   * Rueckgabe:
   *   true: Knoten ist nutzbar (erreichbar)
   */
  public boolean updNode()
  {
    boolean allowsChildren = false;
    boolean leaf           = false;
    Icon    nodeIcon       = null;
    String  nodeName       = null;
    File    file           = this.file;
    boolean usable         = FileUtil.isUsable( file );
    if( file != null ) {
      if( this.fileSystemRoot ) {
	nodeName = file.getPath();
	if( usable ) {
	  nodeIcon       = FileUtil.getSystemIcon( file );
	  allowsChildren = true;
	}
      } else {
	if( usable ) {
	  nodeIcon = FileUtil.getSystemIcon( file );
	  nodeName = FileUtil.getSystemDisplayName( file );
	  if( file.isDirectory() ) {
	    allowsChildren = true;
	  } else {
	    leaf = true;
	  }
	}
	if( nodeName == null ) {
	  nodeName = file.getName();
	}
      }
    }
    if( usable ) {
      Path path = getPath();
      if( path != null ) {
	try {
	  if( Files.isSymbolicLink( path ) ) {
	    StringBuilder buf = new StringBuilder( 256 );
	    if( nodeName != null ) {
	      buf.append( nodeName );
	      buf.append( '\u0020' );
	    }
	    buf.append( '\u2192' );
	    Path targetPath = Files.readSymbolicLink( path );
	    if( targetPath != null ) {
	      String s = targetPath.toString();
	      if( s != null ) {
		if( !s.isEmpty() ) {
		  buf.append( '\u0020' );
		  buf.append( s );
		}
	      }
	    }
	    allowsChildren = false;
	    nodeName       = buf.toString();
	  }
	}
	catch( Exception ex ) {
	  allowsChildren = false;
	}
      }
    } else {
      nodeIcon = FileUtil.getUnreachablePathIcon();
    }
    synchronized( this ) {
      this.allowsChildren = allowsChildren;
      this.leaf           = leaf;
      this.nodeName       = nodeName;
      this.nodeIcon       = nodeIcon;
    }
    return usable;
  }


	/* --- TreeNode --- */

  @Override
  public Enumeration<FileNode> children()
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
    return this.nodeName != null ? this.nodeName : "";
  }
}
