/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse zur Bedienung des Audioeingangs
 * fuer die Emulation des Anschlusses des Magnettonbandgeraetes
 */

package jkcemu.audio;

import java.lang.*;
import javax.sound.sampled.*;
import z80emu.Z80CPU;


public class AudioInLine extends AudioIn
{
  private static int[] sampleRates = { 44100, 32000, 22050 };

  private volatile TargetDataLine dataLine;
  private byte[]                  frameBuf;
  private byte[]                  audioDataBuf;
  private int                     audioDataLen;
  private int                     audioDataPos;
  private int                     maxPauseTStates;


  public AudioInLine( AudioFrm audioFrm, Z80CPU z80cpu )
  {
    super( audioFrm, z80cpu );
    this.dataLine        = null;
    this.frameBuf        = null;
    this.audioDataBuf    = null;
    this.audioDataLen    = 0;
    this.audioDataPos    = 0;
    this.maxPauseTStates = 0;
  }


  public AudioFormat startAudio( Mixer mixer, int speedKHz, int sampleRate )
  {
    if( (this.dataLine == null) && (speedKHz > 0) ) {

      // Audioeingabekanal oeffnen
      TargetDataLine line = null;
      if( sampleRate > 0 ) {
	line = openTargetDataLine( mixer, sampleRate );
      } else {
	for( int i = 0;
	     (line == null) && (i < this.sampleRates.length);
	     i++ )
	{
	  line = openTargetDataLine( mixer, this.sampleRates[ i ] );
	}
      }

      if( line != null ) {
	this.errorText = null;

	AudioFormat fmt      = line.getFormat();
	this.dataLine        = line;
	this.maxPauseTStates = speedKHz * 1000;	// 1 Sekunde
	this.tStatesPerFrame = (int) (((float) speedKHz) * 1000.0F
						/ fmt.getFrameRate());

	// Buffer fuer ein Frame anlegen
	this.frameBuf = new byte[ fmt.getFrameSize() ];

	// Buffer fuer Leseoperationen anlegen
	int r = Math.round( fmt.getFrameRate() );
	int n = this.dataLine.getBufferSize() / 32;
	if( n > r / 2 ) {		// max. 1/2 Sekunde puffern
	  n = r / 2;
	}
	if( n < 1 ) {
	  n = 1;
	}
	this.audioDataBuf = new byte[ n * this.frameBuf.length ];
	this.audioDataLen = 0;
	this.audioDataPos = this.audioDataLen;
	setAudioFormat( fmt );
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
   *   true:  Audiodaten verwenden
   *   false: Audiodaten verwerfen
   */
  @Override
  protected boolean currentDiffTStates( long diffTStates )
  {
    boolean rv = false;
    if( diffTStates > this.maxPauseTStates ) {
      /*
       * Sollte die Zeit zu gross sein, werden die im Puffer stehenden
       * Audiodaten ignoriert und die Mittelwertregelung initialisiert.
       */
      this.minValue = 0;
      this.maxValue = 0;
    } else {
      /*
       * Wenn Daten gelesen werden, darf das Soundsystem
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
  protected byte[] readFrame()
  {
    int            value        = -1;
    TargetDataLine line         = this.dataLine;
    byte[]         audioDataBuf = this.audioDataBuf;
    byte[]         frameBuf     = this.frameBuf;
    if( (line != null) && (audioDataBuf != null) && (frameBuf != null) ) {
      
      if( this.audioDataPos >= this.audioDataLen ) {
	this.audioDataLen = line.read(
				this.audioDataBuf,
				0,
				this.audioDataBuf.length );
	this.audioDataPos = 0;
      }
      if( this.audioDataPos + frameBuf.length <= this.audioDataLen ) {
	System.arraycopy(
		audioDataBuf,
		this.audioDataPos,
		frameBuf, 0,
		frameBuf.length );
	this.audioDataPos += frameBuf.length;
      }
    }
    return frameBuf;
  }


  @Override
  public void stopAudio()
  {
    this.frameBuf     = null;
    this.audioDataBuf = null;
    this.audioDataPos = 0;

    DataLine line = dataLine;
    this.dataLine = null;
    DataLineCloser.closeDataLine( line );
  }


	/* --- private Methoden --- */

  private TargetDataLine openTargetDataLine( Mixer mixer, int sampleRate )
  {
    TargetDataLine line = openTargetDataLine( mixer, sampleRate, 2 );
    if( line == null ) {
      line = openTargetDataLine( mixer, sampleRate, 1 );
    }
    return line;
  }


  private TargetDataLine openTargetDataLine(
					Mixer mixer,
					int   sampleRate,
					int   channels )
  {
    TargetDataLine line = openTargetDataLine(
					mixer,
					sampleRate,
					channels,
					false );
    if( line == null ) {
      line = openTargetDataLine( mixer, sampleRate, channels, true );
    }
    return line;
  }


  private TargetDataLine openTargetDataLine(
					Mixer   mixer,
					int     sampleRate,
					int     channels,
					boolean bigEndian )
  {
    AudioFormat fmt = new AudioFormat(
				sampleRate,
				8,
				channels,
				true,
				bigEndian );

    DataLine.Info  info = new DataLine.Info( TargetDataLine.class, fmt );
    TargetDataLine line = null;
    try {
      if( mixer != null ) {
	if( mixer.isLineSupported( info ) ) {
	  line = (TargetDataLine) mixer.getLine( info );
	}
      } else {
	if( AudioSystem.isLineSupported( info ) ) {
	  line = (TargetDataLine) AudioSystem.getLine( info );
	}
      }
      if( line != null ) {
	line.open( fmt );
	line.flush();
	line.start();
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

