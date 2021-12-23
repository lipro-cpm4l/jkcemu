/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eintrag in einer Dateiliste mit erweiterten Funktionen
 */

package jkcemu.tools.filebrowser;

import java.io.File;
import java.util.zip.ZipEntry;
import jkcemu.file.FileEntry;
import jkcemu.file.TarEntry;


public class ExtendedFileEntry extends FileEntry
{
  private ExtendedFileNode fileNode;
  private TarEntry         tarEntry;
  private ZipEntry         zipEntry;


  public ExtendedFileEntry( File file )
  {
    this.fileNode = null;
    this.tarEntry = null;
    this.zipEntry = null;
    setFile( file );
  }


  public ExtendedFileEntry( ExtendedFileNode fileNode )
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
      setLastModified( tarEntry.getTimeMillis() );
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


  public ExtendedFileNode getExtendedFileNode()
  {
    return this.fileNode;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Object getInfo()
  {
    Object rv = null;
    if( this.tarEntry != null ) {
      if( !this.tarEntry.isRegularFile() ) {
	rv = this.tarEntry.getTypeText();
      }
    }
    return rv != null ? rv : super.getInfo();
  }
}

