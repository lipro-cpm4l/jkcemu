/*
 * (c) 2016-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abstrakte Implementierung der Schnittstelle PCMDataSource
 * ohne Implementierung der eigentlichen Datenhaltung
 */

package jkcemu.audio;

import java.io.IOException;


public abstract class AbstractPCMDataReader implements PCMDataSource
{
  protected int     channels;
  protected int     frameRate;
  protected int     sampleSizeInBits;
  protected int     bytesPerSample;
  protected int     bytesPerFrame;
  protected boolean dataSigned;
  protected boolean bigEndian;
  protected long    pcmDataOffs;  // Beginn PCM-Daten in der Datenquelle
  protected long    frameCount;


  public AbstractPCMDataReader(
			int     frameRate,
			int     sampleSizeInBits,
			int     channels,
			boolean dataSigned,
			boolean bigEndian,
			long    pcmDataOffs,
			long    pcmDataLen ) throws IOException
  {
    if( frameRate < 1 ) {
      throw new IllegalArgumentException( "frameRate=" + frameRate );
    }
    if( sampleSizeInBits < 1 ) {
      throw new IllegalArgumentException(
				"sampleSizeInBits=" + sampleSizeInBits );
    }
    if( channels < 1 ) {
      throw new IllegalArgumentException( "channels=" + channels );
    }
    this.frameRate        = frameRate;
    this.sampleSizeInBits = sampleSizeInBits;
    this.bytesPerSample   = (sampleSizeInBits + 7) / 8;
    this.bytesPerFrame    = this.bytesPerSample * channels;
    this.channels         = channels;
    this.dataSigned       = dataSigned;
    this.bigEndian        = bigEndian;
    this.pcmDataOffs      = pcmDataOffs;
    this.frameCount       = pcmDataLen / this.bytesPerFrame;
    if( this.frameCount < 1 ) {
      throwNoAudioData();
    }
  }


  protected static void throwNoAudioData() throws IOException
  {
    throw new IOException( "Keine Audiodaten vorhanden" );
  }


	/* --- PCMDataSource --- */

  @Override
  public void close() throws IOException
  {
    // leer
  }


  @Override
  public int getChannels()
  {
    return this.channels;
  }


  @Override
  public int getFrameRate()
  {
    return this.frameRate;
  }


  @Override
  public long getFrameCount()
  {
    return this.frameCount;
  }


  @Override
  public int getSampleSizeInBits()
  {
    return this.sampleSizeInBits;
  }


  @Override
  public boolean isBigEndian()
  {
    return this.bigEndian;
  }


  @Override
  public boolean isSigned()
  {
    return this.dataSigned;
  }


  @Override
  public synchronized void setFramePos( long framePos ) throws IOException
  {
    throw new IOException(
		"Setzen der Abspielposition nicht m\u00F6glich" );
  }


  @Override
  public boolean supportsSetFramePos()
  {
    return false;
  }
}
