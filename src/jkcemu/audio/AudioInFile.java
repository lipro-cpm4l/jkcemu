/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang),
 * indem die Audio-Daten von einer Datei gelesen werden.
 */

package jkcemu.audio;

import java.io.*;
import java.lang.*;
import javax.sound.sampled.*;
import jkcemu.base.EmuUtil;
import z80emu.Z80CPU;


public class AudioInFile extends AudioIn
{
  private AudioFrm         audioFrm;
  private File             file;
  private byte[]           fileBytes;
  private int              offs;
  private boolean          tapFile;
  private AudioInputStream in;
  private byte[]           frameBuf;
  private long             frameCount;
  private long             framePos;
  private int              progressStepSize;
  private int              progressStepCnt;
  private volatile boolean pause;


  public AudioInFile(
		Z80CPU   z80cpu,
		AudioFrm audioFrm,
		File     file,
		byte[]   fileBytes,
		int      offs,
		boolean  tapFile )
  {
    super( z80cpu );
    this.audioFrm         = audioFrm;
    this.file             = file;
    this.fileBytes        = fileBytes;
    this.offs             = offs;
    this.tapFile          = tapFile;
    this.in               = null;
    this.frameBuf         = null;
    this.frameCount       = 0L;
    this.framePos         = 0L;
    this.progressStepSize = 0;
    this.progressStepCnt  = 0;
    this.pause            = false;
  }


  public boolean isPause()
  {
    return this.pause;
  }


  public void setPause( boolean state )
  {
    this.pause = state;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public AudioFormat startAudio( Mixer mixer, int speedKHz, int sampleRate )
  {
    AudioFormat fmt = null;
    if( (this.in == null) && (speedKHz > 0) ) {
      this.pause = true;
      try {
	if( this.tapFile ) {
	  if( this.fileBytes == null ) {
	    this.fileBytes = EmuUtil.readFile( this.file, 0x100000 );
	  }
	  if( this.fileBytes == null ) {
	    throw new IOException();
	  }
	  /*
	   * Wird in der Mitte einer Multi-TAP-Datei begonnen,
	   * soll auch die Fortschrittsanzeige in der Mitte beginnen.
	   * Aus diesem Grund wird in dem Fall sowohl die Gesamtlaenge
	   * als auch die Restlaenge der Multi-TAP-Datei ermittelt.
	   */
	  KCTapAudioInputStream tapIn = new KCTapAudioInputStream(
						this.fileBytes,
						0,
						this.fileBytes.length - offs );
	  this.frameCount = tapIn.getFrameLength();
	  if( this.offs > 0 ) {
	    tapIn = new KCTapAudioInputStream(
					this.fileBytes,
					this.offs,
					this.fileBytes.length - offs );
	    this.framePos = this.frameCount - tapIn.getFrameLength();
	    if( this.framePos < 0 ) {
	      this.framePos = 0;
	    }
	  }
	  this.in = new AudioInputStream(
				tapIn, 
				tapIn.getAudioFormat(),
				tapIn.getFrameLength() );
	} else {
	  this.in         = AudioSystem.getAudioInputStream( this.file );
	  this.frameCount = this.in.getFrameLength();
	}
	fmt = this.in.getFormat();
	if( this.frameCount > 0 ) {
	  this.progressStepSize = (int) this.frameCount / 100;
	  this.progressStepCnt  = this.progressStepSize;
	  this.progressEnabled  = true;
	  this.audioFrm.fireProgressUpdate(
			(float) this.framePos / (float) this.frameCount );
	}
      }
      catch( UnsupportedAudioFileException ex ) {
	this.errorText = "Das Dateiformat wird nicht unterst\u00FCtzt.";
      }
      catch( Exception ex ) {
	this.errorText = "Die Datei kann nicht ge\u00F6ffnet werden.";
	String msg     = ex.getMessage();
	if( msg != null ) {
	  if( !msg.isEmpty() ) {
	    this.errorText = msg;
	  }
	}
      }
      if( (this.in != null) || (fmt != null) ) {
	this.frameBuf        = new byte[ fmt.getFrameSize() ];
	this.tStatesPerFrame = (int) (((float) speedKHz) * 1000.0F
						/ fmt.getFrameRate());
      } else {
	stopAudio();
      }
    }
    setAudioFormat( fmt );
    return this.audioFmt;
  }


  @Override
  public void stopAudio()
  {
    closeMonitorLine();

    InputStream in       = this.in;
    this.in              = null;
    this.frameBuf        = null;
    this.progressEnabled = false;
    if( in != null ) {
      try {
	in.close();
      }
      catch( Exception ex ) {}
    }
  }


  @Override
  protected boolean supportsMonitor()
  {
    return true;
  }


  @Override
  protected byte[] readFrame()
  {
    AudioInputStream in  = this.in;
    byte[]           buf = this.frameBuf;
    if( (in != null) && (buf != null) ) {
      try {
	if( in.read( buf ) == buf.length ) {
	  if( isMonitorActive() ) {
            writeMonitorLine( buf );
          }
	  this.framePos++;
	  if( this.progressStepCnt > 0 ) {
	    --this.progressStepCnt;
	  } else {
	    this.progressStepCnt = this.progressStepSize;
	    this.audioFrm.fireProgressUpdate(
			(float) this.framePos / (float) this.frameCount );
	  }
	} else {
	  buf = null;
	  this.audioFrm.fireDisable();
	}
      }
      catch( IOException ex ) {
	this.errorText = ex.getMessage();
      }
    }
    return buf;
  }
}
