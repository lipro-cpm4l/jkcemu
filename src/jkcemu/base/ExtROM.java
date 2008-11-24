/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten eines externen ROM-Images
 */

package jkcemu.base;

import java.io.*;
import java.lang.*;


public class ExtROM implements Comparable<ExtROM>
{
  private int    begAddr;
  private int    endAddr;
  private File   file;
  private byte[] fileBytes;
  private String text;


  public ExtROM( File file ) throws IOException
  {
    this.begAddr   = 0;
    this.endAddr   = 0;
    this.file      = file;
    this.fileBytes = null;
    this.text      = "%0000  " + this.file.getPath();
    reload();
  }


  public synchronized int getBegAddress()
  {
    return this.begAddr;
  }


  public synchronized int getEndAddress()
  {
    return this.endAddr;
  }


  public synchronized int getByte( int addr )
  {
    int rv = 0;
    if( this.fileBytes != null ) {
      int idx = addr - this.begAddr;
      if( (idx >= 0) && (idx < this.fileBytes.length) )
	rv = (int) this.fileBytes[ idx ] & 0xFF;
    }
    return rv;
  }


  public File getFile()
  {
    return this.file;
  }


  public synchronized void setBegAddress( int addr )
  {
    this.begAddr = addr;
    this.endAddr = 0;
    if( this.fileBytes != null ) {
      this.endAddr = this.begAddr + this.fileBytes.length - 1;
    }
    this.text = String.format(
			"%%%04X  %s",
			this.begAddr,
			this.file.getPath() );
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
	if( fileBytes != null ) {
	  this.fileBytes = fileBytes;
	  this.endAddr   = this.begAddr + this.fileBytes.length - 1;
	}
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


	/* --- Comparable --- */

  public int compareTo( ExtROM data )
  {
    return data != null ? (this.begAddr - data.begAddr) : -1;
  }


	/* --- ueberschriebene Methoden --- */

  public String toString()
  {
    return this.text;
  }
}

