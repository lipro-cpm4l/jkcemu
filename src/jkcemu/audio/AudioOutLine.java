/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse zur Bedienung des Audio-Ausgangs
 * fuer die Emulation des Anschlusses des Magnettonbandgeraetes
 */

package jkcemu.audio;

import java.lang.*;
import javax.sound.sampled.*;
import z80emu.*;


public class AudioOutLine extends AudioOut
{
  private static int[] sampleRatesSound = { 22050, 16000, 8000 };
  private static int[] sampleRatesData  = { 44100, 32000, 22050 };

  private boolean                 isSound;
  private volatile SourceDataLine dataLine;
  private byte[]                  audioDataBuf;
  private int                     audioDataPos;


  public AudioOutLine(
		Z80CPU    z80cpu,
		boolean   isSound )
  {
    super( z80cpu );
    this.isSound      = isSound;
    this.dataLine     = null;
    this.audioDataBuf = null;
    this.audioDataPos = 0;
  }


  public Control[] getDataControls()
  {
    Line line = this.dataLine;
    return line != null ? line.getControls() : null;
  }


	/* --- ueberschriebene Methoden --- */

  /*
   * Mit dieser Methode erfaehrt die Klasse den aktuellen
   * Taktzyklenzahlerstand und die Anzahl der seit dem letzten
   * Aufruf vergangenen Taktzyklen.
   *
   * Sollte die Zeit zu gross sein, werden die im Puffer stehenden
   * Audio-Daten ignoriert.
   */
  @Override
  protected void currentTStates( int tStates, int diffTStates )
  {
    if( diffTStates > this.maxPauseTStates ) {
      this.lastTStates  = tStates;
      this.audioDataPos = 0;
      DataLine line = this.dataLine;
      if( line != null ) {
        line.flush();
      }

    } else {

      /*
       * Wenn Daten geschrieben werden, darf das Soundsystem
       * auf keinen Fall auf die CPU-Emulation warten.
       * In diesem Fall wird die Geschwindigkeitsbremse
       * der CPU-Emulation temporaer, d.h.,
       * bis mindestens zum naechsten Soundsystemaufruf, abgeschaltet.
       */
      if( !this.isSound ) {
	this.z80cpu.setSpeedUnlimitedFor( diffTStates * 8 );
      }
    }
  }


  @Override
  public boolean isSoundOutEnabled()
  {
    return this.isSound;
  }


  @Override
  public AudioFormat startAudio( Mixer mixer, int speedKHz, int sampleRate )
  {
    if( (this.dataLine == null) && (speedKHz > 0) ) {

      // Audio-Ausgabekanal oeffnen, Mono
      SourceDataLine line = null;
      if( sampleRate > 0 ) {
	line = openSourceDataLine( mixer, sampleRate );
      } else {
	int[] sampleRates = this.isSound ?
				this.sampleRatesSound : this.sampleRatesData;
	for( int i = 0; (line == null) && (i < sampleRates.length); i++ ) {
	  line = openSourceDataLine( mixer, sampleRates[ i ] );
	}
      }
      if( line != null ) {
	this.maxPauseTStates = speedKHz * 1000;		// 1 Sekunde
	this.enabled         = true;
	this.dataLine        = line;
	this.audioFmt        = this.dataLine.getFormat();
	this.tStatesPerFrame = (int) (((float) speedKHz) * 1000.0F
					/ this.audioFmt.getFrameRate() );

	// Audio-Buffer anlegen
	int r = Math.round( this.audioFmt.getFrameRate() );
	int n = line.getBufferSize() / 32;
	if( n > r / 2 ) {		// max. 1/2 Sekunde puffern
	  n = r / 2;
	}
	if( n < 1 ) {
	  n = 1;
	}
	this.audioDataBuf = new byte[ n * this.audioFmt.getFrameSize() ];
	this.audioDataPos = 0;
      }
    }
    return this.audioFmt;
  }


  @Override
  public void stopAudio()
  {
    this.enabled        = false;
    SourceDataLine line = this.dataLine;
    if( line != null ) {

      // Puffer schreiben
      byte[] audioDataBuf = this.audioDataBuf;
      if( audioDataBuf != null ) {
	if( this.audioDataPos >= audioDataBuf.length ) {
	  line.write( audioDataBuf, 0, audioDataBuf.length );
	}
      }
    }
    this.dataLine = null;
    this.audioFmt = null;
    DataLineCloser.closeDataLine( line );
  }


  @Override
  protected void writeSamples( int nSamples, boolean phase )
  {
    writeSamples( nSamples, (byte) (phase ? PHASE1_VALUE : PHASE0_VALUE) );
  }


  @Override
  protected void writeSamples( int nSamples, byte value )
  {
    SourceDataLine line         = this.dataLine;
    byte[]         audioDataBuf = this.audioDataBuf;
    if( (line != null) && (audioDataBuf != null) ) {
      for( int i = 0; i < nSamples; i++ ) {
	if( this.audioDataPos >= audioDataBuf.length ) {
	  line.write( audioDataBuf, 0, audioDataBuf.length );
	  this.audioDataPos = 0;
	}
	audioDataBuf[ this.audioDataPos++ ] = value;
      }
    }
  }


	/* --- private Methoden --- */

  private SourceDataLine openSourceDataLine( Mixer mixer, int sampleRate )
  {
    SourceDataLine line = openSourceDataLine2( mixer, sampleRate, false );
    if( line == null ) {
      line = openSourceDataLine2( mixer, sampleRate, true );
    }
    return line;
  }


  private SourceDataLine openSourceDataLine2(
				Mixer   mixer,
				int     sampleRate,
				boolean bigEndian )
  {
    AudioFormat fmt = new AudioFormat(
				(float) sampleRate,
				8,
				1,
				true,
				bigEndian );

    DataLine.Info  info = new DataLine.Info( SourceDataLine.class, fmt );
    SourceDataLine line = null;
    try {
      if( mixer != null ) {
	if( mixer.isLineSupported( info ) ) {
	  line = (SourceDataLine) mixer.getLine( info );
	}
      } else {
	if( AudioSystem.isLineSupported( info ) ) {
	  line = (SourceDataLine) AudioSystem.getLine( info );
	}
      }
      if( line != null ) {
	line.open( fmt );
	line.start();
      }
    }
    catch( Exception ex ) {
      DataLineCloser.closeDataLine( line );
      line = null;
    }
    return line;
  }
}
