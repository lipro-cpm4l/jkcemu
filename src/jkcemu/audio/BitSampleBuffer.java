/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Speicheroptimierter Puffer fuer 1-Bit-Audiodaten
 *
 * In jedem Element des Attributs "data" werden die Anzahl Samples
 * mit gleicher Phasenlage gespeichert.
 * Dabei bedeutet ein positiver Wert eine positive Phase (true)
 * und ein negativer Wert eine negative Phase (false).
 * Wenn ein Element ueberlaueft, wird das nachste Element
 * mit der gleichen Phasenlage beschrieben.
 */

package jkcemu.audio;

import java.io.IOException;
import jkcemu.base.EmuUtil;


public class BitSampleBuffer
{
  public static class Reader implements PCMDataSource
  {
    private byte[] data;
    private byte   cur;
    private int    size;
    private int    pos;
    private int    frameRate;
    private long   frameCount;
    private long   framePos;

    public Reader(
		byte[] data,
		int    size,
		int    frameRate,
		long   frameCount )
    {
      this.data       = data;
      this.size       = size;
      this.frameRate  = frameRate;
      this.frameCount = frameCount;
      this.framePos   = 0;
      this.cur        = 0;
      this.pos        = 0;
    }

	/* --- ueberschriebene Methoden --- */

    @Override
    public void close()
    {
      // leer;
    }


    @Override
    public int getChannels()
    {
      return 1;
    }


    @Override
    public synchronized long getFramePos()
    {
      return this.framePos;
    }


    @Override
    public long getFrameCount()
    {
      return this.frameCount;
    }


    @Override
    public int getFrameRate()
    {
      return this.frameRate;
    }


    @Override
    public int getSampleSizeInBits()
    {
      return 1;
    }


    @Override
    public boolean isBigEndian()
    {
      return false;
    }


    @Override
    public boolean isSigned()
    {
      return false;
    }


    @Override
    public synchronized int read( byte[] buf, int offs, int len )
    {
      int rv = 0;
      while( len > 0 ) {
	while( (this.cur == 0) && (this.pos < this.size) ) {
	  this.cur = this.data[ this.pos++ ];
	}
	if( this.cur < 0 ) {
	  this.cur++;
	  buf[ offs++ ] = (byte) 0;
	} else if( this.cur > 0 ) {
	  --this.cur;
	  buf[ offs++ ] = (byte) 0x80;
	} else {
	  break;
	}
	rv++;
	--len;
      }
      this.framePos += rv;
      return rv;
    }


    @Override
    public synchronized void setFramePos( long pos ) throws IOException
    {
      if( pos < 0 ) {
	this.cur      = 0;
	this.pos      = 0;
        this.framePos = 0;
      } else {
	for( int i = 0; i < this.size; i++ ) {
	  int v = this.data[ i ];
	  int n = Math.abs( v );
	  if( n > pos ) {
	    n -= pos;
	    this.framePos += pos;
	    this.cur = (byte) (v < 0 ? -n : n);
	    this.pos = i;
	    break;
	  }
	  pos           -= n;
	  this.framePos += n;
	}
      }
    }


    @Override
    public boolean supportsSetFramePos()
    {
      return true;
    }
  };


  private byte[] data;
  private int    size;
  private int    frameRate;
  private long   frameCount;


  public BitSampleBuffer( int frameRate, int initSize )
  {
    this.frameRate  = frameRate;
    this.frameCount = 0;
    this.size       = 0;
    this.data       = new byte[ initSize ];
  }


  public synchronized void addSample( boolean phase ) throws IOException
  {
    if( this.size > 0 ) {
      if( phase == (this.data[ this.size - 1 ] > 0) ) {
	putLastPhaseAgain();
      } else {
	if( ensureSize() ) {
	  this.data[ this.size ] = (byte) (phase ? 1 : -1);
	  this.size++;
	}
      }
    } else {
      this.data[ 0 ] = (byte) (phase ? 1 : -1);
      this.size      = 1;
    }
    this.frameCount++;
  }


  public synchronized void addSamples(
				int     count,
				boolean phase ) throws IOException
  {
    while( count > 0 ) {
      if( this.size > 0 ) {
	int lastSamples = this.data[ this.size - 1 ];
	if( phase && (lastSamples > 0) ) {
	  int n = Math.min( count, (int) Byte.MAX_VALUE - lastSamples );
	  this.data[ this.size - 1 ] += (byte) n;
	  count -= n;
	  this.frameCount += n;
	} else if( !phase && (lastSamples < 0) ) {
	  int n = Math.min( count, -((int) Byte.MIN_VALUE) + lastSamples );
	  this.data[ this.size - 1 ] -= (byte) n;
	  count -= n;
	  this.frameCount += n;
	}
      }
      if( count > 0 ) {
	addSample( phase );
	--count;
      }
    }
  }


  public long getFrameCount()
  {
    return this.frameCount;
  }


  public PCMDataSource newReader()
  {
    return new BitSampleBuffer.Reader(
				this.data,
				this.size,
				this.frameRate,
				this.frameCount );
  }


	/* --- private Methoden --- */

  private boolean ensureSize() throws IOException
  {
    boolean status = true;
    if( this.size >= this.data.length ) {
      try {
	int stepSize = this.data.length / 2;
	if( stepSize < 0x100 ) {
	  stepSize = 0x100;
	}
	else if( stepSize > 0x100000 ) {
	  stepSize = 0x100000;
	}
	byte[] buf = new byte[ this.data.length + stepSize ];
	System.arraycopy( this.data, 0, buf, 0, this.data.length );
	this.data = buf;
      }
      catch( OutOfMemoryError e ) {
	status    = false;
	this.data = null;
	System.gc();
	throw new IOException(
		"Kein Speicher mehr f\u00FCr die Aufzeichnung\n"
				+ "der Audiodaten verf\u00FCgbar." );
      }
    }
    return status;
  }


  private void putLastPhaseAgain() throws IOException
  {
    final int  i = this.size - 1;
    final byte v = this.data[ i ];
    if( v > 0 ) {
      if( v == Byte.MAX_VALUE ) {
	if( ensureSize() ) {
	  this.data[ this.size ] = 1;
	  this.size++;
	}
      } else {
	this.data[ i ]++;
      }
    }
    else if( v < 0 ) {
      if( v == Byte.MIN_VALUE ) {
	if( ensureSize() ) {
	  this.data[ this.size ] = -1;
	  this.size++;
	}
      } else {
	--this.data[ i ];
      }
    }
  }
}
