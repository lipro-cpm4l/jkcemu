/*
 * (c) 2008-2009 Jens Mueller
 *
 * Klaincomputer-Emulator
 *
 * Emulation der 256 KByte RAM-Floppy nach MP 03/1988
 */

package jkcemu.base;

import java.io.*;
import java.lang.*;
import java.util.Arrays;


public class RAMFloppy
{
  private File    file;
  private boolean dataChanged;
  private int     addr0to7;
  private int     addr8to15;
  private int     endOfData;
  private byte[]  data;


  public RAMFloppy()
  {
    this.file        = null;
    this.dataChanged = false;
    this.addr0to7    = 0;
    this.addr8to15   = 0;
    this.endOfData   = 0;
    this.data        = new byte[ 0x40000 ];
    Arrays.fill( this.data, (byte) 0 );
  }


  public void writeByte( int offset, int value )
  {
    value &= 0xFF;
    if( (offset >= 0) && (offset <= 3) ) {
      int addr = computeAddr( offset );
      if( (addr >= 0) && (addr < this.data.length) ) {
	this.data[ addr ] = (byte) value;
	this.dataChanged  = true;
	if( addr >= this.endOfData )
	  this.endOfData = addr + 1;
      }
    }
    else if( offset == 6 ) {
      this.addr8to15 = value;
    }
    else if( offset == 7 ) {
      this.addr0to7 = value;
    }
  }


  public int readByte( int offset )
  {
    int value = 0xFF;
    if( (offset >= 0) && (offset <= 3) ) {
      int addr = computeAddr( offset );
      if( (addr >= 0) && (addr < this.data.length) )
	value = this.data[ addr ];
    }
    else if( offset == 6 ) {
      value = this.addr8to15;
    }
    else if( offset == 7 ) {
      value = this.addr0to7;
    }
    return value & 0xFF;
  }


  public File getFile()
  {
    return this.file;
  }


  public boolean hasDataChanged()
  {
    return this.dataChanged;
  }


  public void load( File file ) throws IOException
  {
    InputStream in = null;
    try {
      in = new FileInputStream( file );

      this.endOfData   = EmuUtil.read( in, this.data, 0 );
      this.file        = file;
      this.dataChanged = false;

      for( int i = this.endOfData; i < this.data.length; i++ )
	this.data[ i ] = (byte) 0;
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


  public void save( File file ) throws IOException
  {
    OutputStream out = null;
    try {
      out = new FileOutputStream( file );
      out.write( this.data, 0, Math.min( this.endOfData, this.data.length ) );
      out.close();
      out = null;

      this.file        = file;
      this.dataChanged = false;
    }
    finally {
      if( out != null ) {
	try {
	  out.close();
	}
	catch( IOException ex ) {}
      }
    }
  }


	/* --- private Methoden --- */

  private int computeAddr( int offset )
  {
    int addr = ((offset << 16) & 0x30000)
	       | ((this.addr8to15 << 8) & 0xFF00)
	       | (this.addr0to7 & 0xFF);

    // unteres Byte weiterzaehlen
    this.addr0to7 = (this.addr0to7 + 1) & 0xFF;
    return addr;
  }
}

