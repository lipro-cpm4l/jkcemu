/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Umwandlung eines Speicherabbilds in Audiodaten
 */

package jkcemu.base;

import jkcemu.audio.AudioOut;
import java.io.*;
import java.lang.*;
import javax.sound.sampled.AudioFormat;


public class EmuSysAudioDataStream extends InputStream
{
  /*
   * Die Audiodaten werden temporaer in eine von ByteArrayOutputStream
   * abgeleitete Klasse geschrieben.
   * Die abgeleitete Klasse bietet Zugriff auf das interne Byte-Array,
   * um daraus die Audiodaten wieder lesen zu koennen.
   * Dadurch wird eine Duplizierung der Audiodaten vermieden.
   */
  public static class ByteOutBuf extends ByteArrayOutputStream
  {
    public ByteOutBuf( int size )
    {
      super( size );
    }

    public byte[] getBuf()
    {
      return this.buf;
    }
  };


  protected byte[] srcBuf;
  protected int    srcPos;
  protected int    srcEOF;

  private ByteOutBuf  outBuf;
  private AudioFormat audioFmt;
  private byte[]      audioBuf;
  private int         audioLen;
  private int         audioPos;
  private float       sampleRate;
  private long        frameLen;
  private int         lastCnt;
  private boolean     lastPhase;


  protected EmuSysAudioDataStream(
			float  sampleRate,
			byte[] buf,
			int    offs,
			int    len )
  {
    this.sampleRate = sampleRate;
    this.srcBuf     = buf;
    this.srcPos     = offs;
    this.srcEOF     = Math.min( offs + len, buf.length );
    this.outBuf     = new ByteOutBuf( 0x10000 );
    this.audioFmt   = null;
    this.audioBuf   = null;
    this.audioLen   = 0;
    this.audioPos   = 0;
    this.frameLen   = 0;
    this.lastCnt    = 0;
    this.lastPhase  = true;
  }


  protected void addPhaseChangeSamples( int samples )
  {
    if( samples > 0 ) {
      this.lastPhase = !this.lastPhase;
      addSamples( this.lastPhase ? samples : -samples );
    }
  }


  protected void addSamples( int samples )
  {
    this.lastPhase = (samples > 0);
    while( samples <= -128 ) {
      this.outBuf.write( -128 );
      this.frameLen += 128;
      samples += 128;
    }
    while( samples >= 127 ) {
      this.outBuf.write( 127 );
      this.frameLen += 127;
      samples -= 127;
    }
    if( samples != 0 ) {
      this.outBuf.write( samples );
      this.frameLen += Math.abs( samples );
    }
  }


  public AudioFormat getAudioFormat()
  {
    if( this.audioFmt == null ) {
      this.audioFmt = new AudioFormat( this.sampleRate, 8, 1, false, false );
    }
    return this.audioFmt;
  }


  protected int getAvailableSourceBytes()
  {
    return Math.max( this.srcEOF - this.srcPos, 0 );
  }


  public long getFrameLength()
  {
    return this.frameLen;
  }


  protected int readSourceByte()
  {
    int rv = 0;
    if( this.srcPos < this.srcEOF ) {
      rv = (int) this.srcBuf[ this.srcPos++ ] & 0xFF;
    }
    return rv;
  }


  protected int readSourceBytes( byte[] buf )
  {
    int rv     = 0;
    int dstPos = 0;
    while( (this.srcPos < this.srcEOF) && (dstPos < buf.length) ) {
      buf[ dstPos++ ] = this.srcBuf[ this.srcPos++ ];
      rv++;
    }
    return rv;
  }


  protected void setSampleRate( int sampleRate )
  {
    this.sampleRate = sampleRate;
  }


  protected boolean sourceByteAvailable()
  {
    return (this.srcPos < this.srcEOF);
  }


  protected void skipSourceBytes( int len )
  {
    int pos = this.srcPos + len;
    if( pos < 0 ) {
      this.srcPos = 0;
    } else if( pos > this.srcEOF ) {
      this.srcPos = this.srcEOF;
    } else {
      this.srcPos = pos;
    }
  }


  protected boolean skipSourceString( String text )
  {
    boolean rv  = false;
    int     len = text.length();
    if( (this.srcPos + len) <= this.srcEOF ) {
      rv = true;
      for( int i = 0; i < len; i++ ) {
	if( ((int) this.srcBuf[ this.srcPos + i ] & 0xFF)
						!= text.charAt( i ) )
	{
	  rv = false;
	  break;
	}
      }
      if( rv ) {
	this.srcPos += len;
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int available()
  {
    return 1;			// read-Methode blockt nie
  }


  @Override
  public void close()
  {
    // leer
  }


  @Override
  public boolean markSupported()
  {
    return false;
  }


  @Override
  public int read()
  {
    int rv = -1;
    if( this.lastCnt == 0 ) {
      if( this.audioBuf == null ) {
	this.audioBuf = this.outBuf.getBuf();
	this.audioLen = this.outBuf.size();
	this.audioPos = 0;
      }
      if( this.audioPos < this.audioLen ) {
	byte value = this.audioBuf[ this.audioPos++ ];
	if( value < 0 ) {
	  this.lastCnt   = (int) -value;
	  this.lastPhase = false;
	} else {
	  this.lastCnt   = (int) value;
	  this.lastPhase = true;
	}
      }
    }
    if( this.lastCnt > 0 ) {
      --this.lastCnt;
      rv = (this.lastPhase ? AudioOut.MAX_USED_VALUE : 0);
    }
    return rv;
  }


  @Override
  public int read( byte[] buf )
  {
    return read( buf, 0, buf.length );
  }


  @Override
  public int read( byte[] buf, int offs, int len )
  {
    int rv = 0;
    while( len > 0 ) {
      int b = read();
      if( b >= 0 ) {
        buf[ offs++ ] = (byte) b;
        rv++;
      } else {
        break;
      }
      --len;
    }
    return rv;
  }


  @Override
  public void reset()
  {
    this.audioPos = 0;
  }


  @Override
  public long skip( long n )
  {
    long rv = 0;
    while( n > 0 ) {
      if( read() >= 0 ) {
        rv++;
      } else {
        break;
      }
      --n;
    }
    return rv;
  }
}

