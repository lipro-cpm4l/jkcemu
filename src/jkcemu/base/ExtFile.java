/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer extern eingebundenen Datei
 */

package jkcemu.base;

import java.io.*;
import java.lang.*;


public class ExtFile
{
  private File   file;
  private byte[] bytes;


  public ExtFile( File file ) throws IOException
  {
    this.file  = file;
    this.bytes = null;
    reload();
  }


  public byte[] getBytes()
  {
    return this.bytes;
  }


  public File getFile()
  {
    return this.file;
  }


  public void reload() throws IOException
  {
    InputStream in = null;
    try {
      int  bufSize = 0x1000;
      long fileLen = file.length();
      if( fileLen > 0 ) {
	if( fileLen > 0x10000 ) {
	  bufSize = 0x10000;
	} else {
	  bufSize = (int) fileLen;
	}
      }
      in = new FileInputStream( this.file );

      ByteArrayOutputStream buf = new ByteArrayOutputStream( bufSize );

      int n = 0;
      int b = in.read();
      while( b != -1 ) {
	if( n >= 0x10000 ) {
	  throw new IOException( "Datei zu gro\u00DF" );
	}
	buf.write( b );
	b = in.read();
	n++;
      }
      if( buf.size() > 0 ) {
	byte[] fileBytes = buf.toByteArray();
	if( fileBytes != null )
	  this.bytes = fileBytes;
      }
    }
    finally {
      if( in != null ) {
	try {
	  in.close();
	}
	catch( IOException ex ) {}
      }
    }
  }
}

