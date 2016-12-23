/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Schnittstelle zu PCM-Audiodaten,
 * die in einem Byte-Array stehen.
 * Das Setzen der Frame-Position wird unterstuetzt.
 */

package jkcemu.audio;

import java.io.IOException;
import java.lang.*;


public class PCMDataBuffer extends AbstractPCMDataReader
{
  private byte[] dataBytes;
  private int    dataPos;


  public PCMDataBuffer(
		int     frameRate,
		int     sampleSizeInBits,
		int     channels,
		boolean dataSigned,
		boolean bigEndian,
		byte[]  dataBytes,
		long    pcmDataOffs,
		long    pcmDataLen ) throws IOException
  {
    super(
	frameRate,
	sampleSizeInBits,
	channels,
	dataSigned,
	bigEndian,
	pcmDataOffs,
	pcmDataLen );
    this.dataBytes = dataBytes;
    this.dataPos   = 0;

    if( dataBytes.length < (pcmDataOffs + pcmDataLen) ) {
      pcmDataLen = dataBytes.length - pcmDataOffs;
    }
    this.frameCount = pcmDataLen / this.bytesPerFrame;
    if( this.frameCount < 1 ) {
      throwNoAudioData();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public synchronized int read( byte[] buf, int offs, int len )
  {
    int rv = 0;
    while( (len > 0) && (this.dataPos < this.dataBytes.length) ) {
      buf[ offs++ ] = this.dataBytes[ this.dataPos++ ];
      rv++;
      --len;
    }
    return rv;
  }


  @Override
  public synchronized void setFramePos( long framePos ) throws IOException
  {
    long dataPos = this.pcmDataOffs + (framePos * this.bytesPerFrame);
    if( dataPos > this.dataBytes.length ) {
      this.dataPos = this.dataBytes.length;
    } else {
      this.dataPos = (int) dataPos;
    }
  }


  @Override
  public boolean supportsSetFramePos()
  {
    return true;
  }
}
