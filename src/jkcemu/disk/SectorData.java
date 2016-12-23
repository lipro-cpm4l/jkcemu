/*
 * (c) 2009-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten eines Sektors
 */

package jkcemu.disk;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.*;
import java.util.Arrays;


public class SectorData extends SectorID
{
  public class Reader
  {
    private byte[]  buf;
    private int     pos;
    private int     len;
    private boolean deleted;

    private Reader(
		byte[]  buf,
		int     pos,
		int     len,
		boolean deleted )
    {
      this.buf     = buf;
      this.pos     = pos;
      this.len     = len;
      this.deleted = deleted;
    }


    public boolean isByteAvailable()
    {
      return this.len > 0;
    }


    public boolean isSectorDeleted()
    {
      return this.deleted;
    }


    public int read()
    {
      int rv = -1;
      if( (this.buf != null) && (this.len > 0) ) {
	if( (this.pos >= 0) && (this.pos < this.buf.length) ) {
	  rv = (int) this.buf[ this.pos++ ] & 0xFF;
	  --this.len;
	}
      }
      if( (rv == -1) && (len > 0) ) {
	rv = 0;
	--len;
      }
      return rv;
    }
  };


  private int                dataOffs;
  private int                dataLen;
  private byte[]             dataBuf;
  private boolean            shared;
  private boolean            bogusID;
  private boolean            err;
  private boolean            deleted;
  private long               filePos;
  private int                filePortionLen;
  private int                idxOnCyl;
  private AbstractFloppyDisk disk;


  public SectorData(
		int     idxOnCyl,
		int     cyl,
		int     head,
		int     sectorNum,
		int     sizeCode,
		byte[]  dataBuf,
		int     dataOffs,
		int     dataLen )
  {
    super( cyl, head, sectorNum, sizeCode );
    this.idxOnCyl       = idxOnCyl;
    this.dataBuf        = dataBuf;
    this.dataOffs       = dataOffs;
    this.dataLen        = dataLen;
    this.shared         = true;
    this.bogusID        = false;
    this.err            = false;
    this.deleted        = false;
    this.filePos        = -1;
    this.filePortionLen = 0;
    this.disk           = null;
    if( (sizeCode < 0) && (this.dataLen > 0) ) {
      setSizeCode( getSizeCode( this.dataLen ) );
    }
  }


  public boolean checkError()
  {
    return this.err;
  }


  public boolean equalsData( byte[] dataBuf, int pos, int dataLen )
  {
    boolean rv = false;
    if( dataLen == this.dataLen ) {
      rv = true;
      for( int i = 0; i < dataLen; i++ ) {
	int b = 0;
	if( dataBuf != null ) {
	  int idx = pos + i;
	  if( (idx >= 0) && (idx < dataBuf.length) ) {
	    b = (int) dataBuf[ idx ] & 0xFF;
	  }
	}
	if( b != getDataByte( i ) ) {
	  rv = false;
	  break;
	}
      }
    }
    return rv;
  }


  public synchronized int getDataByte( int idx )
  {
    int rv = -1;
    if( (idx >= 0) && (idx < this.dataLen) ) {
      rv = 0;
      if( this.dataBuf != null ) {
	idx += this.dataOffs;
	if( idx < this.dataBuf.length ) {
	  rv = (int) this.dataBuf[ idx ] & 0xFF;
	}
      }
    }
    return rv;
  }


  public int getDataLength()
  {
    return this.dataLen;
  }


  public AbstractFloppyDisk getDisk()
  {
    return this.disk;
  }


  public int getFilePortionLen()
  {
    return this.filePortionLen;
  }


  public long getFilePos()
  {
    return this.filePos;
  }


  public int getIndexOnCylinder()
  {
    return this.idxOnCyl;
  }


  public static int getSizeCode( int sectorSize )
  {
    int rv = -1;
    if( sectorSize >= 0 ) {
      if( sectorSize > 128 ) {
	int m = 128;
	int v = 0;
	while( (m > 0) && (m < sectorSize) ) {
	  v++;
	  m <<= 1;
	}
	if( m == sectorSize ) {
	  rv = v;
	}
      } else {
	rv = 0;
      }
    }
    return rv;
  }


  public boolean hasBogusID()
  {
    return this.bogusID;
  }


  public boolean isDeleted()
  {
    return this.deleted;
  }


  public boolean isEmpty()
  {
    return this.dataLen == 0;
  }


  public synchronized int read( byte[] dstBuf, int dstPos, int dstLen )
  {
    int rv = 0;
    if( dstBuf != null ) {
      if( this.dataBuf != null ) {
	int srcIdx = this.dataOffs;
	int srcLen = this.dataLen;
	while( (srcIdx < this.dataBuf.length) && (srcLen > 0)
	       && (dstPos < dstBuf.length) && (dstLen > 0) )
	{
	  dstBuf[ dstPos++ ] = this.dataBuf[ srcIdx++ ];
	  --srcLen;
	  --dstLen;
	  rv++;
	}
      }
      while( (dstPos < dstBuf.length) && (dstLen > 0) ) {
	dstBuf[ dstPos++ ] = (byte) 0;
	--dstLen;
	rv++;
      }
    }
    return rv;
  }


  public synchronized Reader reader()
  {
    return new Reader(
		this.dataBuf,
		this.dataOffs,
		this.dataLen,
		this.deleted );
  }


  public void setBogusID( boolean state )
  {
    this.bogusID = state;
  }


  public synchronized void setData(
				boolean deleted,
				byte[]  dataBuf,
				int     dataLen )
  {
    this.deleted  = deleted;
    this.dataOffs = 0;
    if( dataBuf != null ) {
      int newLen = Math.min( dataBuf.length, dataLen );
      int oldLen = 0;
      if( this.dataBuf != null ) {
	oldLen = Math.min( this.dataBuf.length, this.dataLen );
      }
      if( this.shared
	  || (this.dataBuf == null)
	  || (oldLen != newLen) )
      {
	if( newLen > 0 ) {
	  this.dataBuf = new byte[ newLen ];
	} else {
	  this.dataBuf = null;
	}
	this.shared = false;
      }
      if( this.dataBuf != null ) {
	int pos = Math.min( newLen, oldLen );
	if( pos > 0 ) {
	  System.arraycopy( dataBuf, 0, this.dataBuf, 0, pos );
	}
	if( pos < this.dataBuf.length ) {
	  Arrays.fill( this.dataBuf, pos, this.dataBuf.length, (byte) 0 );
	}
      }
      this.dataLen = dataLen;
    } else {
      this.dataBuf = null;
      this.dataLen = 0;
      this.shared  = false;
    }
  }


  public void setDeleted( boolean state )
  {
    this.deleted = state;
  }


  public void setDisk( AbstractFloppyDisk disk )
  {
    this.disk = disk;
  }


  public void setError( boolean state )
  {
    this.err = state;
  }


  public void setFilePortionLen( int len )
  {
    this.filePortionLen = len;
  }


  public void setFilePos( long filePos )
  {
    this.filePos = filePos;
  }


  public void setSectorID( int cyl, int head, int sectorNum )
  {
    super.setSectorID( cyl, head, sectorNum );
  }


  public int writeTo(
		OutputStream out,
		int          maxDataLen ) throws IOException
  {
    int n = 0;
    if( this.dataBuf != null ) {
      n = Math.min( this.dataLen, this.dataBuf.length - this.dataOffs );
      if( n > 0 ) {
	if( (maxDataLen >= 0) && (maxDataLen < n) ) {
	  n = maxDataLen;
	}
	out.write( this.dataBuf, this.dataOffs, n );
      }
    }
    while( ((maxDataLen < 0) || (n < maxDataLen)) && (n < this.dataLen) ) {
      out.write( 0 );
      n++;
    }
    return n;
  }


  public int writeTo(
		RandomAccessFile raf,
		int              maxDataLen ) throws IOException
  {
    int n = 0;
    if( this.dataBuf != null ) {
      n = Math.min( this.dataLen, this.dataBuf.length - this.dataOffs );
      if( n > 0 ) {
	if( (maxDataLen >= 0) && (maxDataLen < n) ) {
	  n = maxDataLen;
	}
	raf.write( this.dataBuf, this.dataOffs, n );
      }
    }
    while( ((maxDataLen < 0) || (n < maxDataLen)) && (n < this.dataLen) ) {
      raf.write( 0 );
      n++;
    }
    return n;
  }
}
