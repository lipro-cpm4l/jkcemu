/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Vergleicher fuer Dateien
 *
 * Beim Sortieren der Dateisystemwurzeln wird nach dem abstrakten Pfad sortiert.
 * Ansonsten vergleicht dieser Comparator nur die Dateinamen
 * und stellt sicher, dass Verzeichnisse vor den Dateien gestellt werden.
 */

package jkcemu.filebrowser;

import java.lang.*;
import java.io.File;


public class FileComparator implements java.util.Comparator<File>
{
  private static FileComparator caseSensitiveInstance = null;
  private static FileComparator ignoreCaseInstance    = null;

  private boolean caseSensitive;
  private boolean forFileSystemRoots;


  public static FileComparator getCaseSensitiveInstance()
  {
    if( caseSensitiveInstance == null ) {
      caseSensitiveInstance = new FileComparator( true );
    }
    return caseSensitiveInstance;
  }


  public static FileComparator getIgnoreCaseInstance()
  {
    if( ignoreCaseInstance == null ) {
      ignoreCaseInstance = new FileComparator( false );
    }
    return ignoreCaseInstance;
  }


  public void setForFileSystemRoots( boolean state )
  {
    this.forFileSystemRoots = state;
  }


	/* --- Comparator --- */

  public int compare( File f1, File f2 )
  {
    int rv = -1;
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
    return rv;
  }


  public boolean equals( Object o )
  {
    return o == this;
  }


	/* --- private Konstruktoren --- */

  private FileComparator( boolean caseSensitive )
  {
    this.caseSensitive      = caseSensitive;
    this.forFileSystemRoots = false;
  }
}

