/*
 * (c) 2008-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang und Ausgang)
 */

package jkcemu.audio;

import java.lang.*;
import javax.sound.sampled.*;
import z80emu.*;


public abstract class AudioIO
{
  protected Z80CPU      z80cpu;
  protected AudioFrm    audioFrm;
  protected AudioFormat audioFmt;
  protected boolean     firstCall;
  protected boolean     progressEnabled;
  protected boolean     lastPhase;
  protected long        lastTStates;
  protected int         tStatesPerFrame;
  protected String      errorText;

  private volatile SourceDataLine monitorLine;
  private volatile byte[]         monitorBuf;
  private volatile int            monitorPos;


  protected AudioIO( AudioFrm audioFrm, Z80CPU z80cpu )
  {
    this.audioFrm        = audioFrm;
    this.z80cpu          = z80cpu;
    this.firstCall       = true;
    this.progressEnabled = false;
    this.lastPhase       = false;
    this.lastTStates     = 0L;
    this.tStatesPerFrame = 0;
    this.errorText       = null;
    this.monitorLine     = null;
    this.monitorBuf      = null;
    this.monitorPos      = 0;
  }


  /*
   * Mit dieser Methode erfaehrt die Klasse die Anzahl
   * der seit dem letzten Aufruf vergangenen Taktzyklen.
   *
   * Rueckgabewert:
   *   true:  Audio-Daten verwenden
   *   false: Audio-Daten verwerfen
   */
  protected boolean currentDiffTStates( long diffTStates )
  {
    return true;
  }


  public AudioFormat getAudioFormat()
  {
    return this.audioFmt;
  }


  public String getAndClearErrorText()
  {
    String text    = this.errorText;
    this.errorText = null;
    return text;
  }


  public boolean isMonitorActive()
  {
    return this.monitorLine != null;
  }


  public boolean isProgressUpdateEnabled()
  {
    return this.progressEnabled;
  }


  protected void openMonitorLine()
  {
    AudioFormat fmt = this.audioFmt;
    if( supportsMonitor()
	&& (this.monitorLine == null)
	&& (fmt != null) )
    {
      DataLine.Info  info = new DataLine.Info( SourceDataLine.class, fmt );
      SourceDataLine line = null;
      try {
	if( AudioSystem.isLineSupported( info ) ) {
	  line = (SourceDataLine) AudioSystem.getLine( info );
	  if( line != null ) {
	    line.open( fmt );
	    line.start();

	    // Buffer anlegen
	    int r = Math.round( fmt.getSampleRate() );
	    int n = line.getBufferSize() / 32;
	    if( n < r / 8 ) {
	      n = r / 8;		// min. 1/8 Sekunde
	    }
	    else if( n > r / 2 ) {
	      n = r / 2;		// max. 1/2 Sekunde
	    }
	    if( n < 1 ) {
	      n = 1;
	    }
	    this.monitorBuf = new byte[ n ];
	  }
	}
      }
      catch( Exception ex ) {
	DataLineCloser.closeDataLine( line );
	line = null;
      }
      this.monitorLine = line;
    }
  }


  protected void closeMonitorLine()
  {
    DataLine line    = this.monitorLine;
    this.monitorLine = null;
    if( line != null )
      DataLineCloser.closeDataLine( line );
  }


  public void reset()
  {
    // leer
  }


  public abstract void stopAudio();


  protected boolean supportsMonitor()
  {
    return false;
  }


  protected void writeMonitorLine( byte[] buf )
  {
    SourceDataLine line = this.monitorLine;
    if( (line != null) && (buf != null) )
      line.write( buf, 0, buf.length );
  }


  protected void writeMonitorLine( int frameValue )
  {
    SourceDataLine line = this.monitorLine;
    byte[]         buf  = this.monitorBuf;
    if( (line != null) && (buf != null) ) {
      if( this.monitorPos >= buf.length ) {
	line.write( buf, 0, buf.length );
	this.monitorPos = 0;
      }
      buf[ this.monitorPos ] = (byte) frameValue;
      this.monitorPos++;
    }
  }
}

