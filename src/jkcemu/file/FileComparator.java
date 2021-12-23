/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Vergleicher fuer Dateien
 *
 * Der Comparator stellt Verzeichnisse vor Dateien.
 */

package jkcemu.file;

import java.io.File;


public class FileComparator implements java.util.Comparator<File>
{
  private static volatile FileComparator instance = null;

  private boolean caseSensitive;


  public static int compare( File f1, File f2, boolean caseSensitive )
  {
    int rv = -1;
    if( (f1 != null) && (f2 != null) ) {
      boolean d1 = f1.isDirectory();
      boolean d2 = f2.isDirectory();
      if( d1 && !d2 ) {
	rv = -1;
      } else if( !d1 && d2 ) {
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


	/* --- Konstruktor --- */

  private FileComparator( boolean caseSensitive )
  {
    this.caseSensitive = caseSensitive;
  }
}
