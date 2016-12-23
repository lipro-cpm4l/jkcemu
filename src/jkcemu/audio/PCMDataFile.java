/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Schnittstelle zu PCM-Audiodaten,
 * die in einem RandomAccessFile stehen.
 * Das Setzen der Frame-Position wird unterstuetzt.
 */

package jkcemu.audio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.*;


public class PCMDataFile extends AbstractPCMDataReader
{
  private RandomAccessFile raf;
  private boolean          eof;
  private byte[]           buf;
  private int              bufLen;	// gelesene Daten im Puffer
  private int              bufPos;	// Leseposition im Puffer
  private long             bufOffs;	// Pufferoffset vom Dateianfang


  public PCMDataFile(
		int              frameRate,
		int              sampleSizeInBits,
		int              channels,
		boolean          dataSigned,
		boolean          bigEndian,
		RandomAccessFile raf,
		long             pcmDataOffs,
		long             pcmDataLen ) throws IOException
  {
    super(
	frameRate,
	sampleSizeInBits,
	channels,
	dataSigned,
	bigEndian,
	pcmDataOffs,
	pcmDataLen );
    this.raf     = raf;
    this.eof     = false;
    this.buf     = new byte[ 0x8000 ];
    this.bufLen  = 0;
    this.bufPos  = 0;
    this.bufOffs = pcmDataOffs;
    this.raf.seek( pcmDataOffs );

    long fileLen = raf.length();
    if( fileLen < (pcmDataOffs + pcmDataLen) ) {
      pcmDataLen = fileLen - pcmDataOffs;
    }
    this.frameCount = pcmDataLen / this.bytesPerFrame;
    if( this.frameCount < 1 ) {
      throwNoAudioData();
    }
  }


	/* --- PCMDataSource --- */

  @Override
  public void close() throws IOException
  {
    this.raf.close();
  }


  @Override
  public synchronized int read(
			byte[] buf,
			int    offs,
			int    len ) throws IOException
  {
    int rv = 0;
    if( !this.eof ) {
      while( len > 0 ) {
	if( this.bufPos >= this.bufLen ) {
	  this.bufLen = this.raf.read( this.buf );
	  this.bufPos = 0;
	  this.bufOffs += this.bufLen;
	}
	if( this.bufPos >= this.bufLen ) {
	  this.eof = true;
	  break;
	}
	buf[ offs++ ] = this.buf[ this.bufPos++ ];
	rv++;
	--len;
      }
    }
    return rv;
  }


  @Override
  public synchronized void setFramePos( long framePos ) throws IOException
  {
    long filePos = this.pcmDataOffs + (framePos * this.bytesPerFrame);
    if( (this.bufLen > 0)
	&& (filePos >= this.bufOffs)
	&& (filePos < (this.bufOffs + this.bufLen)) )
    {
      // neue Position liegt innerhalb der gelesenen Bytes
      this.bufPos = (int) (filePos - this.bufOffs);
    } else {
      this.bufOffs = filePos;
      this.bufLen  = 0;
      this.bufPos  = 0;
    }
    this.eof = false;
  }


  @Override
  public boolean supportsSetFramePos()
  {
    return true;
  }
}
