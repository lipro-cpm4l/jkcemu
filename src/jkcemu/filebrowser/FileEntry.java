/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eintrag in einer Dateiliste
 */

package jkcemu.filebrowser;

import java.io.File;
import java.lang.*;
import java.util.zip.ZipEntry;


public class FileEntry
{
  private File     file;
  private FileNode fileNode;
  private TarEntry tarEntry;
  private ZipEntry zipEntry;
  private Object   value;
  private boolean  marked;


  FileEntry( File file )
  {
    this.file     = file;
    this.fileNode = fileNode;
    this.tarEntry = null;
    this.zipEntry = null;
    this.value    = null;
    this.marked   = false;
  }


  FileEntry( FileNode fileNode )
  {
    this.file     = (fileNode != null ? fileNode.getFile() : null);
    this.fileNode = fileNode;
    this.tarEntry = null;
    this.zipEntry = null;
    this.value    = null;
    this.marked   = false;
  }


  FileEntry( TarEntry tarEntry )
  {
    this.file     = null;
    this.fileNode = null;
    this.tarEntry = tarEntry;
    this.zipEntry = null;
    this.value    = null;
    this.marked   = false;
  }


  FileEntry( ZipEntry zipEntry )
  {
    this.file     = null;
    this.fileNode = null;
    this.tarEntry = null;
    this.zipEntry = zipEntry;
    this.value    = null;
    this.marked   = false;
  }


  public File getFile()
  {
    return this.file;
  }


  public FileNode getFileNode()
  {
    return this.fileNode;
  }


  public Object getInfo()
  {
    boolean isDir = false;
    long    size  = -1;
    Object  rv    = null;
    if( this.tarEntry != null ) {
      isDir = this.tarEntry.isDirectory();
      size  = this.tarEntry.getSize();
      if( !this.tarEntry.isFile() ) {
	rv = this.tarEntry.getTypeText();
      }
    } else if( this.zipEntry != null ) {
      isDir = this.zipEntry.isDirectory();
      if( !isDir ) {
	String name = this.zipEntry.getName();
	if( name != null ) {
	  if( name.endsWith( "/" ) || name.endsWith( "\\" ) )
	    isDir = true;
	}
      }
      if( !isDir ) {
	size = this.zipEntry.getSize();
      }
    } else if( this.file != null ) {
      isDir = this.file.isDirectory();
      if( this.file.isFile() ) {
	size = this.file.length();
      }
    }
    if( isDir ) {
      rv = "Verzeichnis";
    } else {
      if( (rv == null) && (size >= 0) ) {
	rv = new Long( size );
      }
    }
    return rv;
  }


  public String getName()
  {
    String rv = null;
    if( this.tarEntry != null ) {
      rv = this.tarEntry.getName();
    } else if( this.zipEntry != null ) {
      rv = this.zipEntry.getName();
    } else if( this.fileNode != null ) {
      rv = this.fileNode.toString();
    } else if( this.file != null ) {
      rv = this.file.getName();
    }
    return rv;
  }


  public java.util.Date getTime()
  {
    long millis = -1;
    if( this.tarEntry != null ) {
      millis = this.tarEntry.getTime();
    } else if( this.zipEntry != null ) {
      millis = this.zipEntry.getTime();
    } else if( this.file != null ) {
      millis = this.file.lastModified();
    }
    return millis > 0 ? new java.util.Date( millis ) : null;
  }


  public Object getValue()
  {
    return this.value;
  }


  public boolean isMarked()
  {
    return this.marked;
  }


  public void setMarked( boolean state )
  {
    this.marked = state;
  }


  public void setValue( Object value )
  {
    this.value = value;
  }
}

