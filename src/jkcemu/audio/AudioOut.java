/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Ausgang)
 *
 * Die Ausgabe erfolgt als Rechteckkurve
 */

package jkcemu.audio;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import jkcemu.base.EmuUtil;
import jkcemu.base.MyByteArrayOutputStream;
import z80emu.Z80CPU;


public class AudioOut extends AudioIO
{
  public static final int MAX_LINE_PAUSE_MILLIS       = 100;
  public static final int MAX_RECORDING_PAUSE_SECONDS = 5;
  public static final int MAX_UNSIGNED_VALUE          = 255;
  public static final int MAX_UNSIGNED_USED_VALUE     = 220;
  public static final int UNSIGNED_VALUE_1            = 200;

  private static int[] frameRates = {
			44100, 48000, 32000, 22050, 16000, 8000 };

  private int                     speedKHz;
  private int                     maxWaveTStatesLine;
  private int                     maxWaveTStatesRec;
  private int                     lineChannels;
  private int                     audioPos;
  private byte[]                  audioBuf;
  private volatile SourceDataLine dataLine;
  private volatile Thread         writingThread;
  private MyByteArrayOutputStream recBufGZip;
  private GZIPOutputStream        recBufOut;
  private int                     recBufFrames;
  private int                     recPauseFrames;
  private int                     recStatus;
  private int                     lastRecMonoValue;
  private int                     lastRecLeftValue;
  private int                     lastRecRightValue;
  private int                     maxRecBufFrames;
  private int                     maxRecPauseFrames;
  private boolean                 stereo;


  public AudioOut(
		AudioIOObserver observer,
		Z80CPU          z80cpu,
		int             speedKHz,
		int             frameRate,
		boolean         openLine,
		Mixer           mixer,
		boolean         singleBit,
		boolean         stereo ) throws IOException
  {
    super( observer, z80cpu );
    this.speedKHz           = speedKHz;
    this.frameRate          = 0;		// wird spaeter gesetzt
    this.maxWaveTStatesLine = speedKHz * MAX_LINE_PAUSE_MILLIS;
    this.maxWaveTStatesRec  = speedKHz * MAX_RECORDING_PAUSE_SECONDS * 1000;
    this.lineChannels       = 0;
    this.audioPos           = 0;
    this.audioBuf           = null;
    this.dataLine           = null;
    this.writingThread      = null;
    this.recBufGZip         = null;
    this.recBufOut          = null;
    this.recBufFrames       = 0;
    this.recPauseFrames     = 0;
    this.recStatus          = 0;
    this.lastRecMonoValue   = 0;
    this.lastRecLeftValue   = 0;
    this.lastRecRightValue  = 0;
    this.maxRecBufFrames    = 0;
    this.stereo             = stereo;
    if( openLine ) {
      SourceDataLine line = null;
      if( frameRate > 0 ) {
	line = openSourceDataLine( mixer, frameRate, stereo );
      } else {
	for( int i = 0; i < this.frameRates.length; i++ ) {
	  line = openSourceDataLine(
			mixer,
			this.frameRates[ i ],
			stereo );
	  if( line != null ) {
	    break;
	  }
	}
      }
      if( line != null ) {
	AudioFormat fmt = line.getFormat();
	setFormat(
		Math.round( fmt.getSampleRate() ),
		singleBit ? 1 : 8,
		stereo ? 2 : 1,
		false,
		false );
	this.lineChannels = fmt.getChannels();
	this.dataLine     = line;

	// Audiopuffer anlegen
	int r = this.frameRate;
	int n = line.getBufferSize() / 32;
	if( n > r / 2 ) {		// max. 1/2 Sekunde puffern
	  n = r / 2;
	}
	if( n < 1 ) {
	  n = 1;
	}
	this.audioBuf = new byte[ n * this.channels ];
	this.audioPos = 0;

	// Fuer die Pegelanzeige gilt der Wertebereich 0...MAX_UNSIGEND_VALUE.
        this.observer.setVolumeLimits( 0, MAX_UNSIGNED_VALUE );
      }
    }
    if( this.frameRate <= 0 ) {
      if( frameRate <= 0 ) {
	frameRate = this.frameRates[ 0 ];
      }
      setFormat(
		frameRate,
		singleBit ? 1 : 8,
		1,
		false,
		false );
    }
    // nach 120 Minuten Aufnahme beenden
    this.maxRecBufFrames = this.frameRate * 60
				* AudioUtil.RECORDING_MINUTES_MAX;
    // nach entsprechender Pause Aufnahme stoppen
    this.maxRecPauseFrames = this.frameRate * MAX_RECORDING_PAUSE_SECONDS;
    this.tStatesPerFrame = (int) (((float) speedKHz) * 1000.0F
						/ (float) this.frameRate );
  }


  public synchronized PCMDataSource createPCMDataSourceOfRecordedData()
							throws IOException
  {
    PCMDataSource rv = null;
    if( hasRecordedData() ) {
      if( this.recBufOut != null ) {
	this.recBufOut.finish();
	this.recBufOut = null;
      }
      rv = new PCMDataStream(
		this.frameRate,
		this.sampleSizeInBits,
		this.channels,
		this.dataSigned,
		this.bigEndian,
		new GZIPInputStream( this.recBufGZip.newInputStream() ),
		this.recBufFrames * this.channels );
    }
    return rv;
  }


  public String getDurationText()
  {
    return AudioUtil.getDurationText( this.frameRate, this.recBufFrames );
  }


  public int getRecordedFrameCount()
  {
    return this.recBufFrames;
  }


  public boolean hasRecordedData()
  {
    return ((this.recBufGZip != null) && (this.recBufFrames > 0));
  }


  public boolean isRecording()
  {
    return this.recStatus > 0;
  }


  public synchronized void setRecording( boolean state )
  {
    try {
      if( state && ((this.recBufGZip == null) || (this.recBufOut == null)) ) {
	int initialSize = this.frameRate * 60;
	if( this.stereo ) {
	  initialSize *= 2;
	}
	this.recBufGZip = new MyByteArrayOutputStream( initialSize );
	this.recBufOut  = new GZIPOutputStream( this.recBufGZip );
      }
      this.recStatus = 1;
      this.observer.fireRecordingStatusChanged( this );
    }
    catch( IOException ex ) {
      // sollte nie vorkommen
      this.recBufGZip = null;
      this.recBufOut  = null;
      this.recStatus  = 0;
    }
  }


  public void stopAudio()
  {
    this.recStatus = 0;
    EmuUtil.closeSilent( this.recBufOut );
    SourceDataLine line = this.dataLine;
    if( line != null ) {
      this.dataLine = null;

      Thread t = this.writingThread;
      if( (t != null) && (t != Thread.currentThread()) ) {
	t.interrupt();
      }
      DataLineCloser.closeDataLine( line );
    }
  }


  /*
   * Die Methode schreibt direkt die Audiodaten.
   * Der Wertebereich ist 0...MAX_UNSIGNED_VALUE.
   */
  public void writeFrames(
			int nFrames,
			int monoValue,
			int leftValue,
			int rightValue )
  {
    // Daten in Audiokanal schreiben
    SourceDataLine line     = this.dataLine;
    byte[]         audioBuf = this.audioBuf;
    if( (line != null) && (audioBuf != null) && (nFrames > 0) ) {
      for( int i = 0; i < nFrames; i++ ) {
	if( (this.audioPos + this.channels - 1) >= audioBuf.length ) {
	  this.writingThread = Thread.currentThread();
	  if( !line.isActive() ) {
	    line.start();
	  }
	  line.write( audioBuf, 0, audioBuf.length );
	  this.audioPos      = 0;
	  this.writingThread = null;
	}
	if( this.lineChannels == 2 ) {
	  if( this.channels == 1 ) {
	    leftValue  = monoValue;
	    rightValue = monoValue;
	  }
	  audioBuf[ this.audioPos++ ] = (byte) leftValue;
	  audioBuf[ this.audioPos++ ] = (byte) rightValue;
	} else {
	  audioBuf[ this.audioPos++ ] = (byte) monoValue;
	}
      }
    }

    /*
     * Daten aufnehmen
     *
     * Die eigentliche Aufnahme erst beginnen,
     * wenn sich die Sample-Daten erstmalig aendern
     */
    OutputStream out = this.recBufOut;
    if( out != null ) {
      if( this.recStatus == 1 ) {
	this.lastRecMonoValue  = monoValue;
	this.lastRecLeftValue  = leftValue;
	this.lastRecRightValue = rightValue;
	this.recStatus         = 2;
      } else if( this.recStatus == 2 ) {
	if( (!stereo && (monoValue != this.lastRecMonoValue))
	    || (stereo && ((leftValue != this.lastRecLeftValue)
			   || (rightValue != this.lastRecRightValue))) )
	{
	  this.recPauseFrames = 0;
	  this.recStatus      = 3;
	}
      }
      if( this.recStatus == 3 ) {
	try {
	  if( stereo ) {
	    if( (leftValue == this.lastRecLeftValue)
		&& (rightValue == this.lastRecRightValue) )
	    {
	      this.recPauseFrames += nFrames;
	    } else {
	      while( this.recPauseFrames > 0 ) {
		out.write( this.lastRecLeftValue );
		out.write( this.lastRecRightValue );
		this.recBufFrames++;
		--this.recPauseFrames;
	      }
	      this.lastRecLeftValue  = leftValue;
	      this.lastRecRightValue = rightValue;
	      for( int i = 0; i < nFrames; i++ ) {
		out.write( leftValue );
		out.write( rightValue );
	      }
	      this.recBufFrames += nFrames;
	    }
	  } else {
	    if( monoValue == this.lastRecMonoValue ) {
	      this.recPauseFrames += nFrames;
	    } else {
	      while( this.recPauseFrames > 0 ) {
		out.write( this.lastRecMonoValue );
		this.recBufFrames++;
		--this.recPauseFrames;
	      }
	      this.lastRecMonoValue = monoValue;
	      for( int i = 0; i < nFrames; i++ ) {
		out.write( monoValue );
	      }
	      this.recBufFrames += nFrames;
	    }
	  }
	  if( this.recBufFrames >= this.maxRecBufFrames ) {
	    throw new IOException( "Audiofunktion beendet, da die maximal\n"
			+ " zul\u00E4ssige Aufnahmedauer erreicht wurde" );
	  }
	  if( this.recPauseFrames > this.maxRecPauseFrames ) {
	    this.recStatus      = 0;
	    this.recPauseFrames = 0;
	    this.observer.fireRecordingStatusChanged( this );
	  }
	}
	catch( IOException ex ) {
	  this.recStatus = 0;
	  stopAudio();
	  this.observer.fireFinished( this, ex.getMessage() );
	}
	catch( OutOfMemoryError e ) {
	  this.recStatus = 0;
	  this.recBufOut = null;
	  System.gc();
	  stopAudio();
	  this.observer.fireFinished(
				this,
				AudioUtil.ERROR_RECORDING_OUT_OF_MEMORY );
	}
      }
    }

    // Pegelanzeige aktualisieren
    this.observer.updVolume( this.channels == 2 ?
					(leftValue + rightValue) / 2
					: monoValue );
  }


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen und besagt,
   * dass auf der entsprechenden Ausgabeleitung ein Wert geschrieben wurde.
   */
  public void writePhase( boolean phase )
  {
    int value = (phase ? UNSIGNED_VALUE_1 : 0);
    writeValue( value, value, value );
  }


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und schreibt synchron zur verstrichenen CPU-Taktzyklenzahl
   * einen Byte-Wert in den Audiokanal.
   * Wertebereich: 0...MAX_UNSIGNED_VALUE
   */
  public void writeValue( int monoValue, int leftValue, int rightValue )
  {
    if( this.tStatesPerFrame > 0 ) {
      if( this.firstCall ) {
	this.firstCall   = false;
	this.lastTStates = this.z80cpu.getProcessedTStates();
	this.lastPhase   = false;

      } else {

	long tStates     = this.z80cpu.getProcessedTStates();
	long diffTStates = this.z80cpu.calcTStatesDiff(
					      this.lastTStates,
					      tStates );
	if( diffTStates > 0 ) {

	  // Anzahl der zu erzeugenden Samples
	  int nFrames = (int) (diffTStates / this.tStatesPerFrame);
	  if( currentDiffTStates( diffTStates ) ) {
	    writeFrames( nFrames, monoValue, leftValue, rightValue );
	  }

	  /*
	   * Anzahl der verstrichenen Taktzyklen auf den Wert
	   * des letzten ausgegebenen Samples korrigieren
	   */
	  this.lastTStates += (nFrames * this.tStatesPerFrame);
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean currentDiffTStates( long diffTStates )
  {
    boolean rv = false;

    // Audiokanal
    if( diffTStates > this.maxWaveTStatesLine ) {
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
      if( this.dataLine != null ) {
	this.z80cpu.setSpeedUnlimitedFor( diffTStates * 8 );
      }
      rv = true;
    }

    // Recorder
    if( (this.recStatus == 3) && (diffTStates > this.maxWaveTStatesRec) ) {
      this.recStatus = 0;
      if( this.dataLine != null ) {
	this.observer.fireRecordingStatusChanged( this );
      } else {
	this.observer.fireFinished( this, null );
      }
    } else {
      rv = true;
    }
    return rv;
  }


  @Override
  public boolean isLineOpen()
  {
    return super.isLineOpen() || (this.dataLine != null);
  }


	/* --- private Methoden --- */

  private SourceDataLine openSourceDataLine(
				Mixer   mixer,
				int     frameRate,
				boolean stereo ) throws IOException
  {
    SourceDataLine line = openSourceDataLine(
				mixer,
				frameRate,
				stereo,
				false );
    if( line == null ) {
      line = openSourceDataLine( mixer, frameRate, stereo, true );
    }
    return line;
  }


  private SourceDataLine openSourceDataLine(
				Mixer   mixer,
				int     sampleRate,
				boolean stereo,
				boolean bigEndian ) throws IOException
  {
    AudioFormat fmt = new AudioFormat(
				(float) sampleRate,
				8,
				stereo ? 2 : 1,
				false,
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
	line.open( fmt, sampleRate / 4 );
      }
    }
    catch( Exception ex ) {
      DataLineCloser.closeDataLine( line );
      line = null;
      if( ex instanceof LineUnavailableException ) {
	throw new IOException( AudioUtil.ERROR_TEXT_LINE_UNAVAILABLE );
      }
    }
    return line;
  }
}
