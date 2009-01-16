/*
 * (c) 2008 Jens Mueller
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
import z80emu.Z80CPU;


public class AudioInFile extends AudioIn
{
  private AudioFrm         audioFrm;
  private File             file;
  private boolean          monitorPlay;
  private AudioInputStream in;
  private byte[]           frameBuf;
  private long             frameCount;
  private long             framePos;
  private int              progressStepSize;
  private int              progressStepCnt;


  public AudioInFile(
		Z80CPU   z80cpu,
		AudioFrm audioFrm,
		File     file,
		boolean  monitorPlay )
  {
    super( z80cpu );
    this.audioFrm         = audioFrm;
    this.file             = file;
    this.monitorPlay      = monitorPlay;
    this.in               = null;
    this.frameBuf         = null;
    this.frameCount       = 0L;
    this.framePos         = 0L;
    this.progressStepSize = 0;
    this.progressStepCnt  = 0;
  }


  public AudioFormat startAudio( int speedKHz, int sampleRate )
  {
    AudioFormat fmt = null;
    if( (this.in == null) && (speedKHz > 0) ) {
      try {
	this.in         = AudioSystem.getAudioInputStream( this.file );
	fmt             = this.in.getFormat();
	this.frameCount = this.in.getFrameLength();
	if( this.frameCount > 0 ) {
	  this.progressStepSize = (int) this.frameCount / 100;
	  this.progressStepCnt  = this.progressStepSize;
	  this.progressEnabled  = true;
	}
      }
      catch( UnsupportedAudioFileException ex ) {
	this.errorText = "Das Dateiformat wird nicht unterst\u00FCtzt.";
      }
      catch( Exception ex ) {
	this.errorText = "Die Datei kann nicht ge\u00F6ffnet werden.\n\n"
				      + ex.getMessage();
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
    if( this.monitorPlay ) {
      openMonitorLine( fmt );
    }
    return fmt;
  }


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


  protected byte[] readFrame()
  {
    AudioInputStream in  = this.in;
    byte[]           buf = this.frameBuf;
    if( (in != null) && (buf != null) ) {
      try {
	if( in.read( buf ) == buf.length ) {
	  if( isMonitorPlayActive() ) {
            writeMonitorLine( buf );
          }
	  this.framePos++;
	  if( this.progressStepCnt > 0 ) {
	    --this.progressStepCnt;
	  } else {
	    this.progressStepCnt = this.progressStepSize;
	    this.audioFrm.fireProgressUpdate(
			(double) this.framePos / (double) this.frameCount );
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

