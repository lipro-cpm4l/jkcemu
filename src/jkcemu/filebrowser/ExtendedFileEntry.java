/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eintrag in einer Dateiliste mit erweiterten Funktionen
 */

package jkcemu.filebrowser;

import java.io.File;
import java.lang.*;
import java.util.zip.ZipEntry;


public class ExtendedFileEntry extends jkcemu.base.FileEntry
{
  private FileNode fileNode;
  private TarEntry tarEntry;
  private ZipEntry zipEntry;


  public ExtendedFileEntry( File file )
  {
    this.fileNode = null;
    this.tarEntry = null;
    this.zipEntry = null;
    setFile( file );
  }


  public ExtendedFileEntry( FileNode fileNode )
  {
    this.fileNode = fileNode;
    this.tarEntry = null;
    this.zipEntry = null;
    if( fileNode != null ) {
      setName( fileNode.toString() );
      setFile( fileNode.getFile() );
    }
  }


  public ExtendedFileEntry( TarEntry tarEntry )
  {
    this.fileNode = null;
    this.tarEntry = tarEntry;
    this.zipEntry = null;
    if( tarEntry != null ) {
      setName( tarEntry.getName() );
      setDirectory( this.tarEntry.isDirectory() );
      setLastModified( tarEntry.getTime() );
      setSize( this.tarEntry.getSize() );
    }
  }


  public ExtendedFileEntry( ZipEntry zipEntry )
  {
    this.fileNode = null;
    this.tarEntry = null;
    this.zipEntry = zipEntry;
    if( zipEntry != null ) {
      setName( zipEntry.getName() );
      setDirectory( this.zipEntry.isDirectory() );
      if( !isDirectory() ) {
	String name = this.zipEntry.getName();
	if( name != null ) {
	  if( name.endsWith( "/" ) || name.endsWith( "\\" ) ) {
	    setDirectory( true );
	  }
	}
      }
      setLastModified( zipEntry.getTime() );
      if( !isDirectory() ) {
	setSize( this.zipEntry.getSize() );
      }
    }
  }


  public FileNode getFileNode()
  {
    return this.fileNode;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Object getInfo()
  {
    Object rv = null;
    if( this.tarEntry != null ) {
      if( !this.tarEntry.isFile() ) {
	rv = this.tarEntry.getTypeText();
      }
    }
    return rv != null ? rv : super.getInfo();
  }
}

