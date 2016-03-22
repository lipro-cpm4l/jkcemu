/*
 * (c) 2008-2016 Jens Mueller
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
  private static int[] sampleRates = { 44100, 32000, 22050, 16000, 8000 };

  private boolean                 isSound;
  private volatile SourceDataLine dataLine;
  private byte[]                  audioBuf;
  private int                     audioPos;
  private int                     channels;
  private int                     maxWaveTStates;


  public AudioOutLine(
		AudioFrm audioFrm,
		Z80CPU   z80cpu,
		boolean  isSound )
  {
    super( audioFrm, z80cpu );
    this.isSound        = isSound;
    this.dataLine       = null;
    this.audioBuf       = null;
    this.audioPos       = 0;
    this.channels       = 0;
    this.maxWaveTStates = 0;
  }


  public AudioFormat startAudio(
			Mixer   mixer,
			int     speedKHz,
			int     sampleRate,
			boolean stereo )
  {
    if( (this.dataLine == null) && (speedKHz > 0) ) {

      // Audio-Ausgabekanal oeffnen, Mono
      SourceDataLine line = null;
      if( sampleRate > 0 ) {
	line = openSourceDataLine( mixer, sampleRate, stereo );
      } else {
	for( int i = 0; (line == null) && (i < sampleRates.length); i++ ) {
	  line = openSourceDataLine( mixer, sampleRates[ i ], stereo );
	}
      }
      if( line != null ) {
	this.errorText       = null;
	this.maxWaveTStates  = speedKHz * 100;		// 0.1 Sekunde
	this.enabled         = true;
	this.dataLine        = line;
	this.audioFmt        = this.dataLine.getFormat();
	this.channels        = this.audioFmt.getChannels();
	this.tStatesPerFrame = (int) (((float) speedKHz) * 1000.0F
					/ this.audioFmt.getSampleRate() );

	// Audio-Buffer anlegen
	int r = Math.round( this.audioFmt.getSampleRate() );
	int n = line.getBufferSize() / 32;
	if( n > r / 2 ) {		// max. 1/2 Sekunde puffern
	  n = r / 2;
	}
	if( n < 1 ) {
	  n = 1;
	}
	this.audioBuf = new byte[ n * this.channels ];
	this.audioPos = 0;

	// Fuer die Pegelanzeige gilt der Wertebereich 0...MAX_OUT_VALUE.
        this.audioFrm.setVolumeLimits( 0, MAX_OUT_VALUE );
      }
    }
    return this.audioFmt;
  }


	/* --- ueberschriebene Methoden --- */

  /*
   * Mit dieser Methode erfaehrt die Klasse die Anzahl
   * der seit dem letzten Aufruf vergangenen Taktzyklen.
   *
   * Rueckgabewert:
   *   true:  Audio-Daten verwenden
   *   false: Audio-Daten verwerfen
   */
  @Override
  protected boolean currentDiffTStates( long diffTStates )
  {
    boolean rv = false;
    if( diffTStates > this.maxWaveTStates ) {
      this.audioPos = 0;
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
      this.z80cpu.setSpeedUnlimitedFor( diffTStates * 8 );
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean isSoundOutEnabled()
  {
    return this.isSound;
  }


  @Override
  public void stopAudio()
  {
    this.enabled        = false;
    SourceDataLine line = this.dataLine;
    if( line != null ) {

      // Puffer schreiben, wenn gerade Audio-Daten ausgegeben werden
      if( line.isActive() ) {
	byte[] audioBuf = this.audioBuf;
	if( audioBuf != null ) {
	  if( this.audioPos >= audioBuf.length ) {
	    line.write( audioBuf, 0, audioBuf.length );
	  }
	}
      }
    }
    this.dataLine = null;
    this.audioFmt = null;
    DataLineCloser.closeDataLine( line );
  }


  @Override
  public void writeSamples(
			int nSamples,
			int monoValue,
			int leftValue,
			int rightValue )
  {
    SourceDataLine line     = this.dataLine;
    byte[]         audioBuf = this.audioBuf;
    if( (line != null) && (audioBuf != null) && (nSamples > 0) ) {
      for( int i = 0; i < nSamples; i++ ) {
	if( (this.audioPos + this.channels - 1) >= audioBuf.length ) {
	  if( !line.isActive() ) {
	    line.start();
	  }
	  line.write( audioBuf, 0, audioBuf.length );
	  this.audioPos = 0;
	}
	if( this.channels == 2 ) {
	  audioBuf[ this.audioPos++ ] = (byte) (leftValue + SIGNED_VALUE_0);
	  audioBuf[ this.audioPos++ ] = (byte) (rightValue + SIGNED_VALUE_0);
	} else {
	  audioBuf[ this.audioPos++ ] = (byte) (monoValue + SIGNED_VALUE_0);
	}
      }
      this.audioFrm.updVolume( this.channels == 2 ?
					(leftValue + rightValue) / 2
					: monoValue );
    }
  }


	/* --- private Methoden --- */

  private SourceDataLine openSourceDataLine(
					Mixer   mixer,
					int     sampleRate,
					boolean stereo )
  {
    SourceDataLine line = openSourceDataLine2(
					mixer,
					sampleRate,
					stereo,
					false );
    if( line == null ) {
      line = openSourceDataLine2( mixer, sampleRate, stereo, true );
    }
    return line;
  }


  private SourceDataLine openSourceDataLine2(
				Mixer   mixer,
				int     sampleRate,
				boolean stereo,
				boolean bigEndian )
  {
    AudioFormat fmt = new AudioFormat(
				(float) sampleRate,
				8,
				stereo ? 2 : 1,
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
	line.open( fmt, sampleRate / (this.isSound ? 8 : 4) );
      }
    }
    catch( Exception ex ) {
      DataLineCloser.closeDataLine( line );
      line = null;
      if( ex instanceof LineUnavailableException ) {
	this.errorText = AudioUtil.ERROR_TEXT_LINE_UNAVAILABLE;
      }
    }
    return line;
  }
}
