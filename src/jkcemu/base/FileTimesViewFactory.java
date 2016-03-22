/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Factory-Klasse zum Erzeugen von FileTimesView-Objekten
 *
 * Der Zugriff auf die Datei erfolgt ausschliesslich ueber die Klasse File.
 * Da man damit jedoch den Zeitpunkt der Erzeugung und
 * den des letzten Zugriffs nicht ermitteln kann,
 * liefern die FileTimesView-Objekte nur den Zeitpunkt
 * der letzten Aenderung.
 */

package jkcemu.base;

import java.io.File;
import java.lang.*;


public class FileTimesViewFactory
{
  public FileTimesViewFactory()
  {
    // leer
  }


  public FileTimesView getFileTimesView( final File file )
  {
    return new FileTimesView()
		{
		  @Override
		  public Long getCreationMillis()
		  {
		    return null;
		  }

		  @Override
		  public Long getLastAccessMillis()
		  {
		    return null;
		  }

		  @Override
		  public Long getLastModifiedMillis()
		  {
		    long millis = file.lastModified();
		    return millis != 0L ? new Long( millis ) : null;
		  }

		  @Override
		  public void setTimesInMillis(
					Long creationMillis,
					Long lastAccessMillis,
					Long lastModifiedMillis )
		  {
		    if( lastModifiedMillis != null ) {
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
