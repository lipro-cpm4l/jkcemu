/*
 * (c) 2008-2021 Jens Mueller
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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import jkcemu.etc.ReadableByteArrayOutputStream;
import z80emu.Z80CPU;


public class AudioOut extends AudioIO
{
  public static final int MAX_LINE_PAUSE_MILLIS       = 100;
  public static final int MAX_RECORDING_PAUSE_SECONDS = 5;
  public static final int MAX_UNSIGNED_VALUE          = 255;
  public static final int MAX_USED_UNSIGNED_VALUE     = 200;

  public static final int SIGNED_VALUE_1 = MAX_USED_UNSIGNED_VALUE / 2;
  public static final int SIGNED_VALUE_0 = -SIGNED_VALUE_1;


  private static final String ERROR_LINE_CLOSED_BECAUSE_NOT_WORKING =
	"Der Audiokanal funktioniert nicht und wurde deshalb geschlossen.";

  private static final int[] frameRates = {
			44100, 48000, 32000, 22050, 16000, 8000 };

  private enum RecStatus {
			DISABLED,
			INIT,
			IDLE,
			RUNNING,
			PAUSE };

  private Z80CPU                        z80cpu;
  private long                          speedHz;
  private long                          totalFrameCnt;
  private long                          begTStates;
  private long                          lastTStates;
  private long                          maxTStates;
  private int                           maxWaveTStatesLine;
  private int                           maxWaveTStatesRec;
  private int                           audioPos;
  private byte[]                        audioBuf;
  private volatile SourceDataLine       dataLine;
  private Mixer.Info                    mixerInfo;
  private RecStatus                     recStatus;
  private ReadableByteArrayOutputStream recBufGZip;
  private GZIPOutputStream              recBufOut;
  private int                           recBufFrames;
  private int                           recPauseFrames;
  private int                           lastRecPauseFrames;
  private int                           lastRecMonoValue;
  private int                           lastRecLeftValue;
  private int                           lastRecRightValue;
  private int                           maxRecBufFrames;
  private int                           maxRecPauseFrames;
  private int                           lineChannels;
  private volatile boolean              lineRequested;
  private boolean                       formatMissing;
  private boolean                       firstCall;
  private boolean                       lastPhase;
  private boolean                       singleBit;
  private boolean                       stereo;
  private boolean                       forPrgSave;


  public AudioOut(
		AudioIOObserver observer,
		Z80CPU          z80cpu,
		int             speedKHz,
		int             frameRate,
		boolean         lineRequested,
		boolean         singleBit,
		boolean         stereo,
		Mixer.Info      mixerInfo,
		boolean         forPrgSave )
  {
    super( observer );
    this.z80cpu             = z80cpu;
    this.speedHz            = speedKHz * 1000;
    this.frameRate          = frameRate;
    this.lineRequested      = lineRequested;
    this.singleBit          = singleBit;
    this.stereo             = stereo;
    this.mixerInfo          = mixerInfo;
    this.forPrgSave         = forPrgSave;
    this.maxWaveTStatesLine = speedKHz * MAX_LINE_PAUSE_MILLIS;
    this.maxWaveTStatesRec  = speedKHz * MAX_RECORDING_PAUSE_SECONDS * 1000;
    this.formatMissing      = true;
    this.firstCall          = true;
    this.lastPhase          = false;
    this.lineChannels       = 0;
    this.audioPos           = 0;
    this.audioBuf           = null;
    this.dataLine           = null;
    this.recBufGZip         = null;
    this.recBufOut          = null;
    this.recBufFrames       = 0;
    this.recPauseFrames     = 0;
    this.lastRecPauseFrames = 0;
    this.recStatus          = RecStatus.DISABLED;
    this.lastRecMonoValue   = 0;
    this.lastRecLeftValue   = 0;
    this.lastRecRightValue  = 0;
    this.lastTStates        = 0;
    this.begTStates         = 0;
    this.maxTStates         = 0;
    this.maxRecBufFrames    = 0;
    this.maxRecPauseFrames  = 0;
    this.totalFrameCnt      = 0;
  }


  public synchronized PCMDataSource createPCMDataSourceOfRecordedData()
							throws IOException
  {
    PCMDataSource    rv     = null;
    GZIPOutputStream recBuf = this.recBufOut;
    if( hasRecordedData() ) {
      if( (this.frameRate < 1)
	  || (this.sampleSizeInBits < 1)
	  || (this.channels < 1)
	  || (this.recBufFrames < 1) )
      {
	recBuf.finish();
	this.recBufOut = null;
	throw new IOException( "Keine Aufnahme vorhanden" );
      }

      /*
       * Die Routinen fuer das Einlesen von Kassettenaufzeichnungen
       * bleiben haeufig haengen,
       * da sie noch auf eine abschliessende Schwingung warten.
       * Aus diesem Grund werden hier noch drei Halbwellen der letzten
       * Schwingung angehaengt, aber nur, wenn es sich auch
       * um eine Kassettenaufzeichnung handeln koennte (1 Bit Mono).
       */
      if( this.forPrgSave
	  && (this.channels == 1)
	  && (this.lastRecPauseFrames > 0)
	  && ((this.lastRecMonoValue == 0)
	      || (this.lastRecMonoValue == MAX_USED_UNSIGNED_VALUE)) )
      {
	int v = this.lastRecMonoValue;
	for( int i = 0; i < 3; i++ ) {
	  if( v == 0 ) {
	    v = MAX_USED_UNSIGNED_VALUE;
	  } else {
	    v = 0;
	  }
	  for( int k = 0; k < this.lastRecPauseFrames; k++ ) {
	    recBuf.write( v );
	    this.recBufFrames++;
	  }
	}
      }

      // aufgenommene Daten schliessen und PCMDataSource erzeugen
      if( recBuf != null ) {
	recBuf.finish();
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


  public static int getDefaultFrameRate()
  {
    return frameRates[ 0 ];
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
    return this.recStatus.equals( RecStatus.INIT )
		|| this.recStatus.equals( RecStatus.IDLE )
		|| this.recStatus.equals( RecStatus.RUNNING );
  }


  public boolean isSingleBit()
  {
    return this.singleBit;
  }


  public synchronized void setRecording( boolean state )
  {
    try {
      if( state ) {
	if( !isRecording() ) {
	  if( (this.recBufGZip == null) || (this.recBufOut == null) ) {
	    int initSize = getRealFrameRate() * 60;
	    if( this.stereo ) {
	      initSize *= 2;
	    }
	    if( (this.maxRecBufFrames < 1)
		|| (this.maxRecPauseFrames < 1) )
	    {
	      calcMaxRecFrames();
	    }

	    // Aufnahmepuffer mit Datenkomprimierung
	    this.recBufGZip = new ReadableByteArrayOutputStream( initSize );
	    this.recBufOut  = new GZIPOutputStream( this.recBufGZip );
	  }
	  this.recStatus = RecStatus.INIT;
	  this.observer.fireRecordingStatusChanged( this );
	}
      } else {
	if( isRecording() ) {
	  this.recStatus = RecStatus.PAUSE;
	  this.observer.fireRecordingStatusChanged( this );
	}
      }
    }
    catch( IOException ex ) {
      // sollte nie vorkommen
      this.recBufGZip = null;
      this.recBufOut  = null;
      this.recStatus  = RecStatus.DISABLED;
      checkFinished();
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
    try {
      checkOpen();

      // Daten in Audiokanal schreiben
      SourceDataLine line     = this.dataLine;
      byte[]         audioBuf = this.audioBuf;
      if( (line != null) && (audioBuf != null) && (nFrames > 0) ) {
	try {
	  for( int i = 0; i < nFrames; i++ ) {
	    if( (this.audioPos + this.lineChannels - 1) >= audioBuf.length ) {
	      if( line.available() < audioBuf.length ) {
		int n = 0;
		do {
		  Thread.sleep( 10 );
		  n++;
		  if( n > 100 ) {
		    throw new IOException(
				ERROR_LINE_CLOSED_BECAUSE_NOT_WORKING );
		  }
		} while( line.available() < audioBuf.length );
	      }
	      if( line != null ) {
		line.write( audioBuf, 0, audioBuf.length );
	      }
	      this.audioPos = 0;
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
	catch( InterruptedException ex ) {
	  // z.B. Programmbeendigung
	  fireStop();
	}
	catch( Exception ex ) {
	  // z.B. Abziehen eines aktiven USB-Audiogeraetes
	  setErrorText( ERROR_LINE_CLOSED_BECAUSE_NOT_WORKING );
	  fireStop();
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
	switch( this.recStatus ) {
	  case RUNNING:
	    try {
	      if( stereo ) {
		if( (leftValue == this.lastRecLeftValue)
		    && (rightValue == this.lastRecRightValue) )
		{
		  this.recPauseFrames += nFrames;
		} else {
		  this.lastRecPauseFrames = this.recPauseFrames;
		  while( this.recPauseFrames > 0 ) {
		    out.write( this.lastRecLeftValue );
		    out.write( this.lastRecRightValue );
		    this.recBufFrames++;
		    --this.recPauseFrames;
		  }
		  this.lastRecLeftValue  = leftValue;
		  this.lastRecRightValue = rightValue;
		  this.recPauseFrames    = nFrames;
		}
	      } else {
		if( monoValue == this.lastRecMonoValue ) {
		  this.recPauseFrames += nFrames;
		} else {
		  this.lastRecPauseFrames = this.recPauseFrames;
		  while( this.recPauseFrames > 0 ) {
		    out.write( this.lastRecMonoValue );
		    this.recBufFrames++;
		    --this.recPauseFrames;
		  }
		  this.lastRecMonoValue = monoValue;
		  this.recPauseFrames   = nFrames;
		}
	      }
	      if( this.recBufFrames >= this.maxRecBufFrames ) {
		fireStop();
		setErrorText( "Audiofunktion beendet, da die maximal\n"
			+ "zul\u00E4ssige Aufnahmedauer erreicht wurde" );
	      }
	      if( this.recPauseFrames > this.maxRecPauseFrames ) {
		this.recStatus      = RecStatus.DISABLED;
		this.recPauseFrames = 0;
		this.observer.fireRecordingStatusChanged( this );
	      }
	    }
	    catch( IOException ex ) {
	      fireStop();
	      setErrorText( ERROR_LINE_CLOSED_BECAUSE_NOT_WORKING );
	    }
	    catch( OutOfMemoryError e ) {
	      this.recStatus = RecStatus.DISABLED;
	      this.recBufOut = null;
	      System.gc();
	      setErrorText( ERROR_RECORDING_OUT_OF_MEMORY );
	      fireStop();
	    }
	    break;

	  case IDLE:
	    if( (!stereo && (monoValue != this.lastRecMonoValue))
		|| (stereo && ((leftValue != this.lastRecLeftValue)
			       || (rightValue != this.lastRecRightValue))) )
	    {
	      this.recPauseFrames = 0;
	      this.recStatus      = RecStatus.RUNNING;
	    }
	    break;

	  case INIT:
	    this.lastRecMonoValue  = monoValue;
	    this.lastRecLeftValue  = leftValue;
	    this.lastRecRightValue = rightValue;
	    this.recStatus         = RecStatus.IDLE;
	    break;
	}
      }

      // Pegelanzeige aktualisieren
      this.observer.updVolume( this.channels == 2 ?
					(leftValue + rightValue) / 2
					: monoValue );
    }
    catch( Exception ex ) {
      fireStop();
    }
    finally {
      checkCloseAndFinished();
    }
  }


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen und besagt,
   * dass auf der entsprechenden Ausgabeleitung ein Wert geschrieben wurde.
   */
  public void writePhase( boolean phase )
  {
    int value = (phase ? MAX_USED_UNSIGNED_VALUE : 0);
    writeValues( value, value, value );
  }


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und schreibt synchron zur verstrichenen CPU-Taktzyklenzahl
   * einen Byte-Wert in den Audiokanal.
   * Wertebereich: 0...MAX_UNSIGNED_VALUE
   */
  public void writeValues( int monoValue, int leftValue, int rightValue )
  {
    try {
      checkOpen();
      if( this.firstCall ) {
	this.firstCall = false;
	calcMaxRecFrames();

	// max. T-States, damit kein numerischer Ueberlauf auftritt
	this.maxTStates = 0x7FFFFFFF00000000L / (this.frameRate + 1);

	// Anfangswerte
	this.begTStates  = this.z80cpu.getProcessedTStates();
	this.lastTStates = this.begTStates;
	this.lastPhase   = false;

      } else {

	long curTStates  = this.z80cpu.getProcessedTStates();
	long allTStates  = curTStates - this.begTStates;
	if( (allTStates < 0) || (allTStates > this.maxTStates) ) {
	  fireStop();
	} else if( allTStates > 0 ) {
	  int nFrames = (int) ((allTStates
				* this.frameRate
				/ this.speedHz) - this.totalFrameCnt);
	  if( nFrames > 0 ) {
	    long diffTStates = curTStates - this.lastTStates;
	    if( diffTStates > 0 ) {
	      if( currentDiffTStates( diffTStates ) ) {
		writeFrames( nFrames, monoValue, leftValue, rightValue );
	      }
	      this.totalFrameCnt += nFrames;
	      this.lastTStates = curTStates;
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {
      // z.B. Abziehen eines aktiven USB-Audiogeraetes
      setErrorText( ERROR_LINE_CLOSED_BECAUSE_NOT_WORKING );
      fireStop();
    }
    finally {
      checkCloseAndFinished();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public synchronized void closeLine()
  {
    if( this.dataLine != null ) {
      closeDataLine( this.dataLine );
      this.dataLine = null;
      checkFinished();
    }
  }


  @Override
  protected boolean currentDiffTStates( long diffTStates )
  {
    boolean rv = true;

    // Audiokanal
    if( diffTStates > this.maxWaveTStatesLine ) {
      DataLine line = this.dataLine;
      if( line != null ) {
	/*
	 * Sollte nicht vorkommen, aber falls doch,
	 * dann Puffer leeren und fuer die verstrichene Zeit
	 * keine Audiodaten ausgeben
	 */
        line.flush();
	this.audioPos = 0;
	rv            = false;
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
    }

    // Recorder
    if( this.recStatus.equals( RecStatus.RUNNING )
	&& (diffTStates > this.maxWaveTStatesRec) )
    {
      this.recStatus = RecStatus.DISABLED;
      if( this.dataLine != null ) {
	this.observer.fireRecordingStatusChanged( this );
      } else {
	finished();
      }
    }
    return rv;
  }


  @Override
  public void fireStop()
  {
    this.recStatus = RecStatus.DISABLED;
    super.fireStop();
  }


	/* --- private Methoden --- */

  private void calcMaxRecFrames()
  {
    // nach entsprechender Zeit Aufnahme beenden
    this.maxRecBufFrames = this.frameRate
				* 60
				* AudioUtil.RECORDING_MINUTES_MAX;

    // nach entsprechender Pause Aufnahme beenden
    this.maxRecPauseFrames = this.frameRate * MAX_RECORDING_PAUSE_SECONDS;
  }


  private void checkCloseAndFinished()
  {
    if( this.stopRequested ) {
      this.stopRequested = false;
      closeLine();
    }
    checkFinished();
  }


  private void checkFinished()
  {
    if( (this.dataLine == null)
	&& this.recStatus.equals( RecStatus.DISABLED ) )
    {
      finished();
    }
  }


  private synchronized void checkOpen()
  {
    // ggf. Line oeffnen
    if( this.lineRequested ) {
      this.lineRequested = false;
      if( !this.stopRequested ) {
	try {
	  if( this.dataLine == null ) {
	    this.dataLine = openSourceDataLine();
	  }
	}
	catch( IOException ex ) {
	  setErrorText( ex.getMessage() );
	  if( this.recStatus.equals( RecStatus.DISABLED ) ) {
	    fireStop();
	  }
	}
      }
    }

    // Format
    if( this.formatMissing ) {
      setFormat(
		null,
		getRealFrameRate(),
		singleBit ? 1 : 8,
		stereo ? 2 : 1,
		false,
		false );
      this.formatMissing = false;
    }
  }


  private int getRealFrameRate()
  {
    return this.frameRate > 0 ? this.frameRate : getDefaultFrameRate();
  }


  private SourceDataLine openSourceDataLine() throws IOException
  {
    SourceDataLine line = null;
    if( this.frameRate > 0 ) {
      line = openSourceDataLine( this.frameRate );
    } else {
      for( int i = 0; i < frameRates.length; i++ ) {
	line = openSourceDataLine( frameRates[ i ] );
	if( line != null ) {
	  break;
	}
      }
    }
    if( line != null ) {
      AudioFormat fmt = line.getFormat();
      setFormat(
		null,
		Math.round( fmt.getSampleRate() ),
		singleBit ? 1 : 8,
		stereo ? 2 : 1,
		false,
		false );
      this.formatMissing = false;
      this.lineChannels  = fmt.getChannels();
      this.dataLine      = line;

      /*
       * externen Audiopuffer anlegen
       *
       * Damit die Implementierung des Blockens ausserhalb
       * der SourceDataLine.write-Methode funktioniert,
       * muss der externe Puffer kleiner als der interne sein.
       */
      this.audioBuf = new byte[ Math.min( line.getBufferSize() / 4, 512 ) ];
      this.audioPos = 0;

      // Fuer die Pegelanzeige gilt der Wertebereich 0...MAX_UNSIGEND_VALUE.
      this.observer.setVolumeLimits( 0, MAX_UNSIGNED_VALUE );
    } else {
      setErrorText( ERROR_NO_LINE );
    }
    return line;
  }


  private SourceDataLine openSourceDataLine(
				int frameRate ) throws IOException
  {
    AudioFormat fmt = new AudioFormat(
				(float) frameRate,
				8,
				this.stereo ? 2 : 1,
				false,
				false );

    SourceDataLine line = null;
    try {
      if( this.mixerInfo != null ) {
	line = AudioSystem.getSourceDataLine( fmt, this.mixerInfo );
      } else {
	line = AudioSystem.getSourceDataLine( fmt );
      }
      if( line != null ) {
	if( isEmuThread() ) {
	  registerCPUSynchronLine( line );
	}
	// interner Puffer bei Stereo fuer 125 ms
	int bufSize = frameRate / 8;
	if( this.stereo ) {
	  bufSize *= 2;
	}
	line.open( fmt, Math.max( bufSize, 256 ) );
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
