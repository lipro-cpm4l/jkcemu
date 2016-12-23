/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang und Ausgang)
 */

package jkcemu.audio;

import java.lang.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import z80emu.Z80CPU;


public abstract class AudioIO
{
  protected AudioIOObserver observer;
  protected Z80CPU          z80cpu;
  protected int             frameRate;
  protected int             sampleSizeInBits;
  protected int             channels;
  protected boolean         dataSigned;
  protected boolean         bigEndian;
  protected boolean         firstCall;
  protected boolean         lastPhase;
  protected long            lastTStates;
  protected int             tStatesPerFrame;
  protected int             bytesPerSample;
  protected String          formatText;

  private volatile SourceDataLine monitorLine;
  private volatile byte[]         monitorBuf;
  private volatile int            monitorPos;


  protected AudioIO( AudioIOObserver observer, Z80CPU z80cpu )
  {
    this.observer         = observer;
    this.z80cpu           = z80cpu;
    this.frameRate        = 0;
    this.sampleSizeInBits = 0;
    this.channels         = 0;
    this.dataSigned       = false;
    this.bigEndian        = false;
    this.firstCall        = true;
    this.lastPhase        = false;
    this.lastTStates      = 0L;
    this.tStatesPerFrame  = 0;
    this.bytesPerSample   = 0;
    this.formatText       = null;
    this.monitorLine      = null;
    this.monitorBuf       = null;
    this.monitorPos       = 0;
  }


  /*
   * Mit dieser Methode erfaehrt die Klasse die Anzahl
   * der seit dem letzten Aufruf vergangenen Taktzyklen.
   *
   * Rueckgabewert:
   *   true:  Audiodaten verwenden
   *   false: Audiodaten verwerfen
   */
  protected boolean currentDiffTStates( long diffTStates )
  {
    return true;
  }


  public int getChannels()
  {
    return this.channels;
  }


  public String getFormatText()
  {
    return this.formatText;
  }


  public int getFrameRate()
  {
    return this.frameRate;
  }


  public int getSampleSizeInBits()
  {
    return this.sampleSizeInBits;
  }


  public boolean isLineOpen()
  {
    return this.monitorLine != null;
  }


  public boolean isMonitorActive()
  {
    return this.monitorLine != null;
  }


  protected void openMonitorLine()
  {
    AudioFormat fmt = new AudioFormat(
				(float) this.frameRate,
				this.sampleSizeInBits,
				this.channels,
				this.dataSigned,
				this.bigEndian );
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
    if( line != null ) {
      DataLineCloser.closeDataLine( line );
    }
  }


  protected void setFormat(
			int     frameRate,
			int     sampleSizeInBits,
			int     channels,
			boolean dataSigned,
			boolean bigEndian )
  {
    this.frameRate        = frameRate;
    this.sampleSizeInBits = sampleSizeInBits;
    this.bytesPerSample   = (sampleSizeInBits + 7) / 8;
    this.channels         = channels;
    this.dataSigned       = dataSigned;
    this.bigEndian        = bigEndian;

    // Formattext erzeugen
    StringBuilder buf = new StringBuilder( 128 );
    buf.append( frameRate );
    buf.append( " Hz, " );
    buf.append( sampleSizeInBits );
    buf.append( " Bit" );
    switch( this.channels ) {
      case 1:
	buf.append( " Mono" );
	break;
      case 2:
	buf.append( " Stereo" );
	break;
      default:
	buf.append( ", " );
	buf.append( this.channels );
	buf.append( " Kan\u00E4le" );
	break;
    }
    this.formatText = buf.toString();

    // Wertebereich der Pegelanzeige festlegen
    if( this.bytesPerSample == 1 ) {
      if( this.dataSigned ) {
	this.observer.setVolumeLimits( -128, 127 );
      } else {
	this.observer.setVolumeLimits( 0, 255 );
      }
    } else {
      if( this.dataSigned ) {
	this.observer.setVolumeLimits( -32768, 32767 );
      } else {
	this.observer.setVolumeLimits( 0, 65535 );
      }
    }
  }


  protected boolean supportsMonitor()
  {
    return false;
  }


  protected void writeMonitorLine( byte[] buf )
  {
    SourceDataLine line = this.monitorLine;
    if( (line != null) && (buf != null) ) {
      line.write( buf, 0, buf.length );
    }
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
