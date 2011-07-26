/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Knoten im Baum eines Datei-Browsers
 */

package jkcemu.base;

import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.tree.TreeNode;


public class FileTreeNode implements TreeNode
{
  protected File                 file;
  protected boolean              fileSystemRoot;
  protected boolean              childrenLoaded;
  protected Vector<FileTreeNode> vChildren;

  private TreeNode parent;
  private boolean  leaf;
  private String   nodeName;


  public FileTreeNode( TreeNode parent, File file, boolean fileSystemRoot )
  {
    this.parent           = parent;
    this.file             = file;
    this.fileSystemRoot   = fileSystemRoot;
    this.leaf             = !fileSystemRoot;
    this.childrenLoaded   = false;
    this.vChildren        = null;
    if( this.leaf && (this.file != null) ) {
      if( this.file.isDirectory() ) {
	this.leaf = false;
      }
    }
    updNodeText();
  }


  public void add( FileTreeNode fileNode )
  {
    if( this.vChildren == null ) {
      this.vChildren = new Vector<FileTreeNode>();
    }
    this.vChildren.add( fileNode );
    this.leaf = false;
  }


  public File getFile()
  {
    return this.file;
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
    this.vChildren.clear();
  }


  public void removeFromParent()
  {
    if( this.parent != null ) {
      if( this.parent instanceof FileTreeNode ) {
	((FileTreeNode) this.parent).vChildren.remove( this );
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
    updNodeText();
  }


  protected void updNodeText()
  {
    this.nodeName = null;
    if( this.file != null ) {
      this.nodeName = (this.fileSystemRoot ?
				this.file.getPath()
				: this.file.getName());
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
      this.vChildren = new Vector<FileTreeNode>();
    }
    return this.vChildren.elements();
  }


  @Override
  public boolean getAllowsChildren()
  {
    return !this.leaf;
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

