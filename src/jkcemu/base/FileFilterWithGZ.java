/*
 * (c) 2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dateifilter auf Basis der Dateinamenserweiterung,
 * wobei zusaetzlich auch alle zugehoerigen *.gz-Endungen aktzeptiert.
 */

package jkcemu.base;

import java.io.File;
import java.lang.*;


public class FileFilterWithGZ extends javax.swing.filechooser.FileFilter
{
  private String   desc;
  private String[] ext;


  public FileFilterWithGZ( String desc, String... ext )
  {
    this.desc = desc;
    this.ext  = null;
    if( ext != null ) {
      if( ext.length > 0 ) {
	this.ext = new String[ ext.length * 2 ];
	int idx  = 0;
	for( int i = 0; i < ext.length; i++ ) {
	  String s = ext[ i ].toLowerCase();
	  if( !s.startsWith( "." ) ) {
	    s = "." + s;
	  }
	  this.ext[ idx++ ] = s;
	  this.ext[ idx++ ] = s + ".gz";
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean accept( File file )
  {
    boolean rv = false;
    if( (file != null) && (this.ext != null) ) {
      String filename = file.getName();
      if( filename != null ) {
	filename = filename.toLowerCase();
	for( int i = 0; i < this.ext.length; i++ ) {
	  if( filename.endsWith( this.ext[ i ] ) ) {
	    rv = true;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public String getDescription()
  {
    return this.desc;
  }
}

