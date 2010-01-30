/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eintrag in einer Dateiliste
 */

package jkcemu.base;

import java.io.File;
import java.lang.*;


public class FileEntry
{
  private String  name;
  private Long    size;
  private Long    lastModified;
  private File    file;
  private Object  value;
  private boolean readOnly;
  private boolean sysFile;
  private boolean archived;
  private boolean dir;
  private boolean marked;


  public FileEntry()
  {
    this.name         = null;
    this.size         = null;
    this.lastModified = null;
    this.file         = null;
    this.value        = null;
    this.readOnly     = false;
    this.sysFile      = false;
    this.archived     = false;
    this.dir          = false;
    this.marked       = false;
  }


  public FileEntry( String name )
  {
    this();
    this.name = name;
  }


  public File getFile()
  {
    return this.file;
  }


  public Object getInfo()
  {
    return this.dir ? "Verzeichnis" : this.size;
  }


  public Long getLastModified()
  {
    return this.lastModified;
  }


  public String getName()
  {
    return this.name;
  }


  public Long getSize()
  {
    return this.size;
  }


  public Object getValue()
  {
    return this.value;
  }


  public boolean isDirectory()
  {
    return this.dir;
  }


  public boolean isArchived()
  {
    return this.archived;
  }


  public boolean isMarked()
  {
    return this.marked;
  }


  public boolean isReadOnly()
  {
    return this.readOnly;
  }


  public boolean isSystemFile()
  {
    return this.sysFile;
  }


  public void setArchived( boolean state )
  {
    this.archived = state;
  }


  public void setDirectory( boolean state )
  {
    this.dir = state;
  }


  public void setFile( File file )
  {
    this.file = file;
    if( file != null ) {
      this.dir          = file.isDirectory();
      this.lastModified = file.lastModified();
      this.size         = file.length();
    }
  }


  public void setLastModified( Long lastModified )
  {
    this.lastModified = lastModified;
  }


  public void setLastModified( long lastModified )
  {
    if( lastModified > 0 ) {
      this.lastModified = new Long( lastModified );
    } else {
      this.lastModified = null;
    }
  }


  public void setMarked( boolean state )
  {
    this.marked = state;
  }


  public void setName( String name )
  {
    this.name = name;
  }


  public void setReadOnly( boolean state )
  {
    this.readOnly = state;
  }


  public void setSize( Long size )
  {
    this.size = size;
  }


  public void setSystemFile( boolean state )
  {
    this.sysFile = state;
  }


  public void setValue( Object value )
  {
    this.value = value;
  }
}
