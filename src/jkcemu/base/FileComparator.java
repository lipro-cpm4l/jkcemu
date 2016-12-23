/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Vergleicher fuer Dateien
 *
 * Beim Sortieren der Dateisystemwurzeln wird nach dem
 * abstrakten Pfad sortiert.
 * Ansonsten vergleicht dieser Comparator nur die Dateinamen
 * und stellt sicher, dass Verzeichnisse vor den Dateien gestellt werden.
 */

package jkcemu.base;

import java.lang.*;
import java.io.File;


public class FileComparator implements java.util.Comparator<File>
{
  private static FileComparator instance = null;

  private boolean caseSensitive;


  public static int compare( File f1, File f2, boolean caseSensitive )
  {
    int rv = -1;
    if( (f1 != null) && (f2 != null) ) {
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
	if( caseSensitive ) {
	  rv = s1.compareTo( s2 );
	} else {
	  rv = s1.compareToIgnoreCase( s2 );
	}
      }
    }
    return rv;
  }


  public static FileComparator getInstance()
  {
    if( instance == null ) {
      instance = new FileComparator( File.separatorChar == '/' );
    }
    return instance;
  }


	/* --- Comparator --- */

  @Override
  public int compare( File f1, File f2 )
  {
    return compare( f1, f2, this.caseSensitive );
  }


  @Override
  public boolean equals( Object o )
  {
    return o == this;
  }


	/* --- private Konstruktoren --- */

  private FileComparator( boolean caseSensitive )
  {
    this.caseSensitive = caseSensitive;
  }
}
