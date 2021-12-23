/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse zur Bedienung des Audioeingangs
 * fuer die Emulation des Anschlusses des Magnettonbandgeraetes
 */

package jkcemu.audio;

import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import jkcemu.file.FileUtil;
import z80emu.Z80CPU;


public class AudioInLine extends AudioIn
{
  private static final int[] frameRates = { 44100, 48000, 32000, 22050 };

  private Mixer.Info              mixerInfo;
  private volatile TargetDataLine dataLine;
  private byte[]                  frameBuf;
  private byte[]                  audioDataBuf;
  private int                     audioDataLen;
  private int                     audioDataPos;
  private int                     maxPauseTStates;
  private volatile boolean        lineRequested;


  public AudioInLine(
		AudioIOObserver observer,
		Z80CPU          z80cpu,
		int             speedKHz,
		int             frameRate,
		Mixer.Info      mixerInfo )
  {
    super( observer, z80cpu );
    this.speedHz         = speedKHz * 1000;
    this.frameRate       = frameRate;
    this.mixerInfo       = mixerInfo;
    this.dataLine        = null;
    this.frameBuf        = null;
    this.audioDataBuf    = null;
    this.audioDataLen    = 0;
    this.audioDataPos    = 0;
    this.maxPauseTStates = 0;
    this.lineRequested   = true;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void checkCloseAndFinished()
  {
    if( this.stopRequested ) {
      this.stopRequested = false;
      closeLine();
    }
    checkFinished();
  }


  @Override
  protected synchronized void checkOpen()
  {
    if( this.lineRequested ) {
      this.lineRequested = false;
      if( (this.dataLine == null) && !this.stopRequested ) {
	try {
	  this.dataLine = openTargetDataLine();
	}
	catch( IOException ex ) {
	  setErrorText( ex.getMessage() );
	}
      }
    }
  }


  @Override
  public synchronized void closeLine()
  {
    if( this.dataLine != null ) {
      closeDataLine( this.dataLine );
      this.dataLine = null;
      checkFinished();
    }
  }


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
    TargetDataLine line         = this.dataLine;
    byte[]         audioDataBuf = this.audioDataBuf;
    byte[]         frameBuf     = this.frameBuf;
    if( (line != null) && (audioDataBuf != null) && (frameBuf != null) ) {
      try {
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
      catch( Exception ex ) {
	line = null;
	fireStop();
      }
    }
    if( (line == null) && (frameBuf != null) ) {
      Arrays.fill( frameBuf, (byte) 0 );
    }
    return frameBuf;
  }


	/* --- private Methoden --- */

  private void checkFinished()
  {
    if( this.dataLine == null ) {
      this.frameBuf     = null;
      this.audioDataBuf = null;
      this.audioDataPos = 0;
      finished();
    }
  }


  private TargetDataLine openTargetDataLine() throws IOException
  {
    TargetDataLine line = null;
    if( this.frameRate > 0 ) {
      line = openTargetDataLine( this.frameRate );
    } else {
      for( int i = 0; i < frameRates.length; i++ ) {
	line = openTargetDataLine( frameRates[ i ] );
	if( line != null ) {
	  break;
	}
      }
    }
    if( line != null ) {
      AudioFormat fmt = line.getFormat();
      this.dataLine        = line;
      this.maxPauseTStates = this.speedHz;	// 1 Sekunde
      setFormat(
	null,
	Math.round( fmt.getSampleRate() ),
	fmt.getSampleSizeInBits(),
	fmt.getChannels(),
	fmt.isBigEndian(),
	fmt.getEncoding().equals( AudioFormat.Encoding.PCM_SIGNED ) );

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
    } else {
      setErrorText( ERROR_NO_LINE );
    }
    return line;
  }


  private TargetDataLine openTargetDataLine(
				int frameRate ) throws IOException
  {
    TargetDataLine line = openTargetDataLine( frameRate, 2 );
    if( line == null ) {
      line = openTargetDataLine( frameRate, 1 );
    }
    return line;
  }


  private TargetDataLine openTargetDataLine(
				int   frameRate,
				int   channels ) throws IOException
  {
    TargetDataLine line = openTargetDataLine( frameRate, channels, false );
    if( line == null ) {
      line = openTargetDataLine( frameRate, channels, true );
    }
    return line;
  }


  private TargetDataLine openTargetDataLine(
				int     frameRate,
				int     channels,
				boolean dataSigned ) throws IOException
  {
    TargetDataLine line = openTargetDataLine(
					frameRate,
					channels,
					dataSigned,
					false );
    if( line == null ) {
      line = openTargetDataLine( frameRate, channels, dataSigned, true );
    }
    return line;
  }


  private TargetDataLine openTargetDataLine(
				int     frameRate,
				int     channels,
				boolean dataSigned,
				boolean bigEndian ) throws IOException
  {
    AudioFormat fmt = new AudioFormat(
				frameRate,
				8,
				channels,
				dataSigned,
				bigEndian );

    TargetDataLine line = null;
    try {
      if( this.mixerInfo != null ) {
	line = AudioSystem.getTargetDataLine( fmt, this.mixerInfo );
      } else {
	line = AudioSystem.getTargetDataLine( fmt );
      }
      if( line != null ) {
	registerCPUSynchronLine( line );
	line.open( fmt );
	line.start();
      }
    }
    catch( Exception ex ) {
      closeDataLine( line );
      line = null;
      if( ex instanceof LineUnavailableException ) {
	throw new IOException( ERROR_LINE_UNAVAILABLE );
      }
    }
    return line;
  }
}
