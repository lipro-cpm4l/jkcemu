/*
 * (c) 2008-2010 Jens Mueller
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
  protected AudioFormat audioFmt;
  protected boolean     firstCall;
  protected boolean     progressEnabled;
  protected boolean     lastPhase;
  protected int         lastTStates;
  protected int         tStatesPerFrame;
  protected String      errorText;

  private volatile SourceDataLine monitorLine;
  private volatile byte[]         monitorBuf;
  private volatile int            monitorPos;


  protected AudioIO( Z80CPU z80cpu )
  {
    this.z80cpu          = z80cpu;
    this.firstCall       = true;
    this.progressEnabled = false;
    this.lastPhase       = false;
    this.lastTStates     = 0;
    this.tStatesPerFrame = 0;
    this.errorText       = null;
    this.monitorLine     = null;
    this.monitorBuf      = null;
    this.monitorPos      = 0;
  }


  /*
   * Die Methode wird aufgerufen um den aktuellen Zaehlerstand
   * der Taktzyklen sowie die Anzahl der Taktzyklen seit dem
   * letzten Aufruf mitzuteilen.
   * Abgeleitete Klassen koennen diese Methode ueberschreiben,
   * um z.B. auf eine zu lange Pause zu reagieren.
   */
  protected void currentTStates( int tStates, int diffTStates )
  {
    // empty
  }


  public Control[] getDataControls()
  {
    return null;
  }


  public String getErrorText()
  {
    return this.errorText;
  }


  public Control[] getMonitorControls()
  {
    Line line = this.monitorLine;
    return line != null ? line.getControls() : null;
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


  public abstract AudioFormat startAudio(
					Mixer mixer,
					int   speedKHz,
					int   sampleRate );

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

