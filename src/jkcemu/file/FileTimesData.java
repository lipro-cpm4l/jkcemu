/*
 * (c) 2017-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse fuer den Zugriff auf die Zeitstempel einer Datei
 */

package jkcemu.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;


public class FileTimesData
{
  private BasicFileAttributes attrs;
  private File                file;
  private boolean             errOccured;



  /*
   * Achtung! "file == null" ist moeglich!
   */
  public static FileTimesData createOf( File file )
  {
    return new FileTimesData( file );
  }


	/* --- private Methoden --- */

  public Long getCreationMillis()
  {
    Long                rv    = null;
    BasicFileAttributes attrs = getAttrs();
    if( attrs != null ) {
      rv = toMillis( attrs.creationTime() );
    }
    return rv;
  }


  public Long getLastAccessMillis()
  {
    Long                rv    = null;
    BasicFileAttributes attrs = getAttrs();
    if( attrs != null ) {
      rv = toMillis( attrs.lastAccessTime() );
    }
    return rv;
  }


  public Long getLastModifiedMillis()
  {
    Long                rv    = null;
    BasicFileAttributes attrs = getAttrs();
    if( attrs != null ) {
      rv = toMillis( attrs.lastModifiedTime() );
    }
    return rv;
  }


  public void setTimesInMillis(
			Long creationMillis,
			Long lastAccessMillis,
			Long lastModifiedMillis )
  {
    if( this.file != null ) {
      boolean done = false;
      try {
	FileAttributeView v = Files.getFileAttributeView(
					this.file.toPath(),
					BasicFileAttributeView.class );
	if( v != null ) {
	  if( v instanceof BasicFileAttributeView ) {
	    ((BasicFileAttributeView) v).setTimes(
					toFileTime( lastModifiedMillis ),
					toFileTime( lastAccessMillis ),
					toFileTime( creationMillis ) );
	    done = true;
	  }
	}
      }
      catch( Exception ex ) {}
      if( !done && (lastModifiedMillis != null) ) {
	try {
	  this.file.setLastModified( lastModifiedMillis.longValue() );
	}
	catch( Exception ex ) {}
      }
    }
  }


	/* --- private Methoden --- */

  private synchronized BasicFileAttributes getAttrs()
  {
    if( (this.attrs == null) && (this.file != null) && !this.errOccured ) {
      try {
	this.attrs = Files.readAttributes(
				this.file.toPath(),
				BasicFileAttributes.class );
      }
      catch( InvalidPathException | IOException ex ) {
	this.errOccured = true;
      }
    }
    return this.attrs;
  }


  private static FileTime toFileTime( Long millis )
  {
    return millis != null ? FileTime.fromMillis( millis.longValue() ) : null;
  }


  private static Long toMillis( FileTime fileTime )
  {
    return fileTime != null ? fileTime.toMillis() : null;
  }


	/* --- Konstruktor --- */

  private FileTimesData( File file )
  {
    this.file       = file;
    this.attrs      = null;
    this.errOccured = false;
  }
}
