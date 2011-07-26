/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Ausgang),
 * indem die Audio-Daten in eine Datei geschrieben werden.
 */

package jkcemu.audio;

import java.io.*;
import java.lang.*;
import javax.sound.sampled.*;
import jkcemu.base.EmuThread;
import z80emu.*;


public class AudioOutFile extends AudioOut
{
  private AudioFrm             audioFrm;
  private File                 file;
  private AudioFileFormat.Type fileType;
  private AudioDataQueue       queue;
  private int                  sampleRate;


  public AudioOutFile(
		Z80CPU               z80cpu,
		AudioFrm             audioFrm,
		File                 file,
		AudioFileFormat.Type fileType )
  {
    super( z80cpu );
    this.audioFrm   = audioFrm;
    this.file       = file;
    this.fileType   = fileType;
    this.audioFmt   = null;
    this.queue      = null;
    this.sampleRate = 0;
  }


	/* --- ueberschrieben Methoden --- */

  /*
   * Wenn die Pause zu groess ist,
   * wird das Schreiben der Sound-Datei abgebrochen.
   */
  @Override
  protected void currentTStates( int tStates, int diffTStates )
  {
    if( diffTStates > this.maxPauseTStates ) {
      this.lastTStates = tStates;
      this.enabled     = false;
      this.audioFrm.fireDisable();
    }
  }


  @Override
  public AudioFormat startAudio( Mixer mixer, int speedKHz, int sampleRate )
  {
    if( this.queue == null ) {
      if( speedKHz > 0 ) {
	if( sampleRate <= 0 ) {
	  sampleRate = 44100;
	}
	this.sampleRate      = sampleRate;
	this.queue           = new AudioDataQueue( sampleRate * 60 );
	this.maxPauseTStates = speedKHz * 1000;		// 1 Sekunde
	this.enabled         = true;
	this.audioFmt        = new AudioFormat(
					sampleRate,
					8,
					1,
					true,
					false );
	this.tStatesPerFrame = (int) (((float) speedKHz) * 1000.0F
					/ this.audioFmt.getFrameRate() );
      }
    }
    return this.audioFmt;
  }


  @Override
  public void stopAudio()
  {
    closeMonitorLine();

    AudioDataQueue queue = this.queue;
    this.enabled         = false;
    this.errorText       = queue.getErrorText();
    if( (this.errorText == null)
	&& (queue.length() > 0)
	&& (this.audioFmt != null) )
    {
      // Puffer leeren
      try {
	queue.appendPauseFrames( this.sampleRate / 10 );
	AudioSystem.write(
		new AudioInputStream(
			queue,
			this.audioFmt,
			queue.length() ),
		this.fileType,
		this.file );
      }
      catch( Exception ex ) {
	this.errorText = "Die Sound-Datei kann nicht gespeichert werden.\n\n"
							  + ex.getMessage();
      }
    } else {
      this.errorText = "Die Sound-Datei wurde nicht gespeichert,\n"
			+ "da keine Audio-Daten erzeugt wurden.";
    }
    this.audioFmt = null;
  }


  @Override
  protected boolean supportsMonitor()
  {
    return true;
  }


  @Override
  protected void writeSamples( int nSamples, boolean phase )
  {
    if( nSamples > 0 ) {
      for( int i = 0; i < nSamples; i++ ) {
	this.queue.putPhase( phase );
      }
      if( isMonitorActive() ) {
	int value = (phase ? PHASE1_VALUE : PHASE0_VALUE);
	for( int i = 0; i < nSamples; i++ ) {
	  writeMonitorLine( value );
	}
      }
    }
  }
}
