/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Vergleicher fuer Dateien, die in einem FileNode-Objekt gekapselt sind
 *
 * Beim Sortieren der Dateisystemwurzeln wird nach dem
 * abstrakten Pfad sortiert.
 * Ansonsten vergleicht dieser Comparator nach dem angezeigten Text
 * und stellt sicher, dass Verzeichnisse vor den Dateien gestellt werden.
 */

package jkcemu.file;

import java.io.File;
import java.util.Collection;


public class FileNodeComparator implements java.util.Comparator<FileNode>
{
  private static volatile FileNodeComparator caseSensitiveInstance = null;
  private static volatile FileNodeComparator ignoreCaseInstance    = null;

  private boolean caseSensitive;


  public static FileNodeComparator getCaseSensitiveInstance()
  {
    if( caseSensitiveInstance == null ) {
      caseSensitiveInstance = new FileNodeComparator( true );
    }
    return caseSensitiveInstance;
  }


  public static FileNodeComparator getIgnoreCaseInstance()
  {
    if( ignoreCaseInstance == null ) {
      ignoreCaseInstance = new FileNodeComparator( false );
    }
    return ignoreCaseInstance;
  }


	/* --- Comparator --- */

  @Override
  public int compare( FileNode o1, FileNode o2 )
  {
    int rv = 0;
    if( (o1 != null) && (o2 == null) ) {
      rv = -1;
    } else if( (o1 == null) && (o2 != null) ) {
      rv = 1;
    } else if( (o1 != null) && (o2 != null) ) {
      String  s1 = o1.toString();
      String  s2 = o2.toString();
      boolean d1 = false;
      boolean d2 = false;
      File    f1 = o1.getFile();
      if( f1 != null ) {
	if( FileUtil.isUsable( f1 ) ) {
	  d1 = f1.isDirectory();
	}
	if( o1.isFileSystemRoot() ) {
	  s1 = f1.getPath();
	}
      }
      File f2 = o2.getFile();
      if( f2 != null ) {
	if( FileUtil.isUsable( f2 ) ) {
	  d2 = f2.isDirectory();
	}
	if( o2.isFileSystemRoot() ) {
	  s2 = f2.getPath();
	}
      }
      if( d1 && !d2 ) {
	rv = -1;
      } else if( !d1 && d2 ) {
	rv = 1;
      } else {
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
    return rv;
  }


	/* --- Konstruktor --- */

  private FileNodeComparator( boolean caseSensitive )
  {
    this.caseSensitive = caseSensitive;
  }
}
