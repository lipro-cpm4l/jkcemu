/*
 * (c) 2008-2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten eines externen ROM-Images
 */

package jkcemu.base;

import java.io.*;
import java.lang.*;


public class ExtROM extends ExtFile implements Comparable<ExtROM>
{
  private int    begAddr;
  private int    endAddr;
  private String text;


  public ExtROM( File file ) throws IOException
  {
    super( file );
    this.begAddr = 0;
    this.endAddr = 0;
    this.text    = "%0000  " + file.getPath();
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
    int    rv        = 0;
    byte[] fileBytes = getBytes();
    if( fileBytes != null ) {
      int idx = addr - this.begAddr;
      if( (idx >= 0) && (idx < fileBytes.length) )
	rv = (int) fileBytes[ idx ] & 0xFF;
    }
    return rv;
  }


  public synchronized void setBegAddress( int addr )
  {
    this.begAddr     = addr;
    this.endAddr     = 0;
    byte[] fileBytes = getBytes();
    if( fileBytes != null ) {
      this.endAddr = this.begAddr + fileBytes.length - 1;
    }
    this.text = String.format(
			"%%%04X  %s",
			this.begAddr,
			getFile().getPath() );
  }


  public void reload() throws IOException
  {
    super.reload();
    byte[] fileBytes = getBytes();
    if( fileBytes != null ) {
      this.endAddr = this.begAddr + fileBytes.length - 1;
    }
  }


	/* --- Comparable --- */

  public int compareTo( ExtROM data )
  {
    return data != null ? (this.begAddr - data.begAddr) : -1;
  }


	/* --- ueberschriebene Methoden --- */

  public boolean equals( Object o )
  {
    boolean rv = false;
    if( o != null ) {
      if( o == this ) {
	rv = true;
      } else {
	if( o instanceof ExtROM ) {
	  rv          = true;
	  ExtROM data = (ExtROM) o;
	  if( this.begAddr != data.getBegAddress() ) {
	    rv = false;
	  } else {
	    byte[] bytes1 = getBytes();
	    byte[] bytes2 = data.getBytes();
	    if( (bytes1 != null) && (bytes2 != null) ) {
	      if( bytes1.length == bytes2.length ) {
		for( int i = 0; i < bytes1.length; i++ ) {
		  if( bytes1[ i ] != bytes2[ i ] ) {
		    rv = false;
		    break;
		  }
		}
	      } else {
		rv = false;
	      }
	    } else {
	      int n1 = 0;
	      if( bytes1 != null ) {
		n1 = bytes1.length;
	      }
	      int n2 = 0;
	      if( bytes2 != null ) {
		n2 = bytes2.length;
	      }
	      if( n1 != n2 ) {
		rv = false;
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  public String toString()
  {
    return this.text;
  }
}

