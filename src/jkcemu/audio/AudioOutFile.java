/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Ausgang),
 * indem die Audiodaten in eine Datei geschrieben werden.
 */

package jkcemu.audio;

import java.io.*;
import java.lang.*;
import javax.sound.sampled.*;
import jkcemu.base.*;
import z80emu.*;


public class AudioOutFile extends AudioOut
{
  private static final int MAX_PAUSE_SECONDS = 3;

  private File                 file;
  private AudioFileFormat.Type fileType;
  private AudioDataQueue       queue;
  private int                  sampleRate;
  private int                  maxPauseTStates;


  public AudioOutFile(
		AudioFrm             audioFrm,
		Z80CPU               z80cpu,
		File                 file,
		AudioFileFormat.Type fileType )
  {
    super( audioFrm, z80cpu );
    this.file            = file;
    this.fileType        = fileType;
    this.audioFmt        = null;
    this.queue           = null;
    this.sampleRate      = 0;
    this.maxPauseTStates = 0;
  }


  public AudioFormat startAudio( int speedKHz, int sampleRate )
  {
    if( this.queue == null ) {
      if( speedKHz > 0 ) {
	if( sampleRate <= 0 ) {
	  sampleRate = 44100;
	}
	if( this.file.exists() ) {
	  if( !this.file.delete() ) {
	    this.errorText = "Bereits vorhandene Ausgabedatei"
			+ " kann nicht \u00FCberschrieben werden.";
	  }
	}
	if( this.errorText == null ) {
	  this.sampleRate      = sampleRate;
	  this.queue           = new AudioDataQueue( sampleRate * 60 );
	  this.maxPauseTStates = speedKHz * MAX_PAUSE_SECONDS * 1000;
	  this.enabled         = true;
	  this.audioFmt        = new AudioFormat(
					sampleRate,
					8,
					1,
					true,
					false );
	  this.tStatesPerFrame = (int) (((float) speedKHz) * 1000.0F
					/ this.audioFmt.getSampleRate() );

	  // Fuer die Pegelanzeige gilt der Wertebereich 0...MAX_OUT_VALUE.
	  this.audioFrm.setVolumeLimits( 0, MAX_OUT_VALUE );
	}
      }
    }
    return this.audioFmt;
  }


	/* --- ueberschrieben Methoden --- */

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
    boolean rv = true;

    /*
     * Wenn die Pause zu groess ist,
     * wird das Schreiben der Sound-Datei abgebrochen.
     */
    if( diffTStates > this.maxPauseTStates ) {
      this.enabled = false;
      this.audioFrm.fireFinished();
      rv = false;
    }
    return rv;
  }


  @Override
  public void reset()
  {
    closeMonitorLine();
    this.enabled = false;
    this.queue   = null;
    this.audioFrm.fireFinished();
  }


  @Override
  public void stopAudio()
  {
    closeMonitorLine();

    this.enabled         = false;
    AudioDataQueue queue = this.queue;
    if( queue != null ) {
      this.errorText = queue.getErrorText();
      int len        = queue.length();
      if( (this.errorText == null)
	  && (len > 0)
	  && (this.audioFmt != null) )
      {
	// Puffer leeren
	try {
	  if( this.fileType != null ) {

	    // Audiodatei erzeugen
	    queue.appendPauseSamples( this.sampleRate / 10 );
	    AudioUtil.write(
			new AudioInputStream( queue, this.audioFmt, len ),
			this.fileType,
			this.file );

	  } else {

	    boolean done  = false;
	    String  fName = this.file.getName();
	    if( fName != null ) {
	      if( fName.toLowerCase().endsWith( ".csw" ) ) {

		// CSW-Datei erzeugen
		queue.appendPauseSamples( this.sampleRate / 10 );
		CSWFile.write(
			sampleRate,
			queue,
			len,
			this.file );
		done = true;
	      }
	    }
	    if( !done ) {

	      // TZX-Datei erzeugen
	      int nBytes = (len + 7) / 8;
	      if( nBytes > 0x7FFFFF ) {
		nBytes = 0x7FFFFF;
		this.errorText = "Es wurden nicht alle Daten in die Datei"
			+ " geschrieben,\n"
			+ "da die mit dem Dateiformat maximal"
			+ " speicherbare Datenmenge erreicht wurde.";
	      }
	      OutputStream out = null;
	      try {
		out = new BufferedOutputStream(
				new FileOutputStream( this.file ) );

		// TZX-Signatur mit Versionsnummer
		EmuUtil.writeASCII( out, FileInfo.TZX_MAGIC );
		out.write( 1 );
		out.write( 20 );

		// Text Description
		String text = "File created by JKCEMU";
		out.write( 0x30 );		// Block-ID
		out.write( text.length() );	// Textlaenge
		EmuUtil.writeASCII( out, text );

		// Direct Recording Block
		out.write( 0x15 );		// Block-ID

		/*
		 * Spectrum-T-States pro Sample,
		 * Fuer die Abtastfrequenz 22050 Hz wuerde der
		 * gerundete Wert 159 betragen,
		 * in der TXZ-Spezifikation steht aber 158.
		 * Aus diesem Grund wird hier fuer 22050 Hz der Wert 158
		 * gesetzt und fuer alle anderen Abtastfrequenzen
		 * der Wert berechnet und anschliessend gerundet.
		 */
		int zxTStatesPerSample = 158;	// Wert fuer 22050 Hz
		if( this.sampleRate != 22050 ) {
		  zxTStatesPerSample = Math.round(
				3500000F / (float) this.sampleRate );
		}
		out.write( zxTStatesPerSample & 0xFF );
		out.write( (zxTStatesPerSample >> 8) & 0xFF );

		// anschliessende Pause in ms
		out.write( 100 );
		out.write( 0 );

		// benutzte Bits im letzten Byte
		int lastBits = (nBytes * 8) - len;
		out.write( (lastBits > 0) && (lastBits < 8) ? lastBits : 8 );

		// Anzahl Datenbytes
		out.write( nBytes & 0xFF );
		out.write( (nBytes >> 8) & 0xFF );
		out.write( nBytes >> 16 );

		// Datenbytes
		while( nBytes > 0 ) {
		  int b = 0;
		  int m = 0x80;
		  for( int i = 0; i < 8; i++ ) {
		    if( queue.read() > 0 ) {
		      b |= m;
		    }
		    m >>= 1;
		  }
		  out.write( b );
		  --nBytes;
		}
	      }
	      finally {
		if( out != null ) {
		  out.close();
		}
	      }
	    }
	  }
	}
	catch( Exception ex ) {
	  this.file.delete();
	  this.errorText = "Die Datei kann nicht gespeichert werden.\n\n"
							  + ex.getMessage();
	}
      } else {
	this.errorText = "Die Datei wurde nicht gespeichert,\n"
			+ "da keine Audiodaten erzeugt wurden.";
      }
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
      AudioDataQueue queue = this.queue;
      if( queue != null ) {
	for( int i = 0; i < nSamples; i++ ) {
	  queue.putPhase( phase );
	}
	this.audioFrm.updVolume( phase ? MAX_USED_VALUE : 0 );
	if( isMonitorActive() ) {
	  int value = (phase ? SIGNED_VALUE_1 : SIGNED_VALUE_0);
	  for( int i = 0; i < nSamples; i++ ) {
	    writeMonitorLine( value );
	  }
	}
      }
    }
  }
}
