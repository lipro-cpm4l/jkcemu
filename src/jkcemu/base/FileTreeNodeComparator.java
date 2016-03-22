/*
 * (c) 2008-2014 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Vergleicher fuer Dateien, die in einem FileTreeNode-Objekt gekapselt sind
 *
 * Beim Sortieren der Dateisystemwurzeln wird nach dem
 * abstrakten Pfad sortiert.
 * Ansonsten vergleicht dieser Comparator nur die Dateinamen
 * und stellt sicher, dass Verzeichnisse vor den Dateien gestellt werden.
 */

package jkcemu.base;

import java.lang.*;
import java.io.File;
import jkcemu.base.FileTreeNode;


public class FileTreeNodeComparator
			implements java.util.Comparator<FileTreeNode>
{
  private static FileTreeNodeComparator caseSensitiveInstance = null;
  private static FileTreeNodeComparator ignoreCaseInstance    = null;

  private boolean caseSensitive;
  private boolean forFileSystemRoots;


  public static FileTreeNodeComparator getCaseSensitiveInstance()
  {
    if( caseSensitiveInstance == null ) {
      caseSensitiveInstance = new FileTreeNodeComparator( true );
    }
    return caseSensitiveInstance;
  }


  public static FileTreeNodeComparator getIgnoreCaseInstance()
  {
    if( ignoreCaseInstance == null ) {
      ignoreCaseInstance = new FileTreeNodeComparator( false );
    }
    return ignoreCaseInstance;
  }


  public void setForFileSystemRoots( boolean state )
  {
    this.forFileSystemRoots = state;
  }


	/* --- Comparator --- */

  @Override
  public int compare( FileTreeNode o1, FileTreeNode o2 )
  {
    int rv = -1;
    if( (o1 != null) && (o2 != null) ) {
      File f1 = o1.getFile();
      File f2 = o2.getFile();
      if( (f1 != null) && (f2 != null) ) {
	if( this.forFileSystemRoots ) {
	  String s1 = f1.getPath();
	  String s2 = f2.getPath();
	  if( s1 == null ) {
	    s1 = "";
	  }
	  if( s2 == null ) {
	    s2 = "";
	  }
	  if( this.caseSensitive ) {
	    rv = s1.compareTo( s2 );
	  } else {
	    rv = s1.compareToIgnoreCase( s2 );
	  }
	} else {
	  if( f1.isDirectory() && !f2.isDirectory() ) {
	    rv = -1;
	  } else if( !f1.isDirectory() && f2.isDirectory() ) {
	    rv = 1;
	  } else {
	    String s1 = f1.getName();
	    String s2 = f2.getName();
	    if( s1 == null ) {
	      s1 = "";
	    }
	    if( s2 == null ) {
	      s2 = "";
	    }
	    if( this.caseSensitive ) {
	      rv = s1.compareTo( s2 );
	    } else {
	      rv = s1.compareToIgnoreCase( s2 );
	    }
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public boolean equals( Object o )
  {
    return o == this;
  }


	/* --- private Konstruktoren --- */

  private FileTreeNodeComparator( boolean caseSensitive )
  {
    this.caseSensitive      = caseSensitive;
    this.forFileSystemRoots = false;
  }
}

