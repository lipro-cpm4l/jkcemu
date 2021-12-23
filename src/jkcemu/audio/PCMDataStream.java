/*
 * (c) 2016-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Schnittstelle zu PCM-Audiodaten,
 * die aus einem InputStream gelesen werden
 * Das Setzen der Frame-Position wird nicht unterstuetzt.
 */

package jkcemu.audio;

import java.io.InputStream;
import java.io.IOException;


public class PCMDataStream extends AbstractPCMDataReader
{
  protected InputStream in;
  protected boolean     eof;
  protected byte[]      buf;
  protected int         bufLen;	// gelesene Daten im Puffer
  protected int         bufPos;	// Leseposition im Puffer
  protected long        totalRead;


  public PCMDataStream(
		int         frameRate,
		int         sampleSizeInBits,
		int         channels,
		boolean     dataSigned,
		boolean     bigEndian,
		InputStream in,
		long        pcmDataLen ) throws IOException
  {
    super(
	frameRate,
	sampleSizeInBits,
	channels,
	dataSigned,
	bigEndian,
	0,
	pcmDataLen );
    this.in        = in;
    this.eof       = false;
    this.buf       = new byte[ 0x8000 ];
    this.bufLen    = 0;
    this.bufPos    = 0;
    this.totalRead = 0;
  }


	/* --- PCMDataSource --- */

  @Override
  public void close() throws IOException
  {
    this.in.close();
  }


  @Override
  public long getFramePos()
  {
    return this.totalRead / this.bytesPerFrame;
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
	  this.bufLen = this.in.read( this.buf );
	  this.bufPos = 0;
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
    this.totalRead += rv;
    return rv;
  }
}
