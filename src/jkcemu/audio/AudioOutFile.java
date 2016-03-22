/*
 * (c) 2008-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Ausgang),
 * indem die Audio-Daten in eine Datei geschrieben werden.
 */

package jkcemu.audio;

import java.io.*;
import java.lang.*;
import javax.sound.sampled.*;
import jkcemu.Main;
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
   *   true:  Audio-Daten verwenden
   *   false: Audio-Daten verwerfen
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

	    // Audio-Datei erzeugen
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

		boolean      lastPhase = (queue.read() > 0);
		OutputStream out       = null;
		try {
		  out = new BufferedOutputStream(
				new FileOutputStream( this.file ) );

		  // CSW-Signatur mit Versionsnummer
		  EmuUtil.writeASCII( out, FileInfo.CSW_HEADER );
		  out.write( 2 );
		  out.write( 0 );

		  // 4 Bytes Abtastrate
		  out.write( this.sampleRate & 0xFF );
		  out.write( (this.sampleRate >> 8) & 0xFF );
		  out.write( (this.sampleRate >> 16) & 0xFF );
		  out.write( this.sampleRate >> 24 );

		  // 4 Bytes Gesamtanzahl Pulse
		  out.write( len & 0xFF );
		  out.write( (len >> 8) & 0xFF );
		  out.write( (len >> 16) & 0xFF );
		  out.write( len >> 24 );

		  // Kompression
		  out.write( 1 );			// RLE

		  // Flags
		  out.write( lastPhase ? 0x01 : 0 );	// B0: Initial-Phase

		  // Header Extension
		  out.write( 0 );			// keine Erweiterung

		  // Encoding Application
		  EmuUtil.writeFixLengthASCII( out, Main.APPNAME, 16, 0 );

		  // CSW-Daten
		  --len;
		  int n = 1;
		  while( len > 0 ) {
		    boolean phase = (queue.read() > 0);
		    if( phase == lastPhase ) {
		      n++;
		    } else {
		      writeCswSampleCount( out, n );
		      n = 1;
		      lastPhase = phase;
		    }
		    --len;
		  }
		  writeCswSampleCount( out, n );
		}
		finally {
		  if( out != null ) {
		    out.close();
		  }
		}
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
		EmuUtil.writeASCII( out, FileInfo.TZX_HEADER );
		out.write( 1 );
		out.write( 20 );

		// Text Description
		StringBuilder textBuf = new StringBuilder( 64 );
		textBuf.append( "File created by JKCEMU" );
		EmuThread emuThread = this.audioFrm.getEmuThread();
		if( emuThread != null ) {
		  EmuSys emuSys = emuThread.getEmuSys();
		  if( emuSys != null ) {
		    String s = emuSys.getTitle();
		    if( s != null ) {
		      if( !s.isEmpty() ) {
			textBuf.append( " (" );
			textBuf.append( s );
			textBuf.append( " emulation)" );
		      }
		    }
		  }
		}
		out.write( 0x32 );			// Block-ID
		out.write( textBuf.length() + 3 );	// L-Byte Blocklaenge
		out.write( 0 );			// H-Byte Blocklaenge
		out.write( 1 );			// Anzahl Textabschnitte
		out.write( 8 );			// Typ: Herkunft
		out.write( textBuf.length() );	// Laenge Textabschnitt
		EmuUtil.writeASCII( out, textBuf );

		// Direct Recording Block
		out.write( 0x15 );			// Block-ID

		// Spectrum-T-States pro Sample
		out.write( this.sampleRate == 22050 ? 158 : 79 );
		out.write( 0 );

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
			+ "da keine Audio-Daten erzeugt wurden.";
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


	/* --- private Methoden --- */

  private static void writeCswSampleCount(
				OutputStream out,
				int          n ) throws IOException
  {
    if( (n & 0xFF) == n ) {
      out.write( n );
    } else {
      out.write( 0 );
      out.write( n & 0xFF );
      out.write( (n >> 8) & 0xFF );
      out.write( (n >> 16) & 0xFF );
      out.write( n >> 24 );
    }
  }
}

