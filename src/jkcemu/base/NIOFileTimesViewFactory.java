/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Factory-Klasse zum Erzeugen von FileTimesView-Objekten
 *
 * Die Zeitstempel werden mit Hilfe der java.nio.file-Klassen gelesen.
 * Dadurch koennen auch der die Zeitstempel der Erzeugung
 * und des letzten Zugriffs ermittelt werden.
 */

package jkcemu.base;

import java.io.*;
import java.lang.*;
import java.nio.file.*;
import java.nio.file.attribute.*;


public class NIOFileTimesViewFactory extends FileTimesViewFactory
{
  public NIOFileTimesViewFactory()
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public FileTimesView getFileTimesView( final File file )
  {
    FileTimesView rv = null;
    try {
      final BasicFileAttributes attrs = Files.readAttributes(
						file.toPath(),
						BasicFileAttributes.class );
      if( attrs != null ) {
	rv = new FileTimesView()
		{
		  @Override
		  public Long getCreationMillis()
		  {
		    return toMillis( attrs.creationTime() );
		  }

		  @Override
		  public Long getLastAccessMillis()
		  {
		    return toMillis( attrs.lastAccessTime() );
		  }

		  @Override
		  public Long getLastModifiedMillis()
		  {
		    return toMillis( attrs.lastModifiedTime() );
		  }

		  @Override
		  public void setTimesInMillis(
					Long creationMillis,
					Long lastAccessMillis,
					Long lastModifiedMillis )
		  {
		    boolean done = false;
		    try {
		      FileAttributeView v = Files.getFileAttributeView(
					file.toPath(),
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
			file.setLastModified(
				lastModifiedMillis.longValue() );
		      }
		      catch( Exception ex ) {}
		    }
		  }
		};
	}	  
    }
    catch( InvalidPathException ex ) {}
    catch( IOException ex ) {}
    return rv != null ? rv : super.getFileTimesView( file );
  }


	/* --- private Methoden --- */

  private static FileTime toFileTime( Long millis )
  {
    return millis != null ? FileTime.fromMillis( millis.longValue() ) : null;
  }


  private static Long toMillis( FileTime fileTime )
  {
    return fileTime != null ? new Long( fileTime.toMillis() ) : null;
  }
}
