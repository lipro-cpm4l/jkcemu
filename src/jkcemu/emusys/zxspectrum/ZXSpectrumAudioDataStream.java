/*
 * (c) 2014-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Lesen einer ZX-TAP- oder TZX-Datei als InputStream von Audiodaten
 */

package jkcemu.emusys.zxspectrum;

import java.io.IOException;
import java.lang.*;
import jkcemu.base.*;


public class ZXSpectrumAudioDataStream extends EmuSysAudioDataStream
{
  private final static int   SAMPLE_RATE         = 44100;
  private final static float T_STATES_PER_SAMPLE =
				3500000F / (float) SAMPLE_RATE;

  private boolean phase;


  public ZXSpectrumAudioDataStream(
			byte[] buf,
			int    offs,
			int    len ) throws IOException
  {
    super( SAMPLE_RATE, buf, offs, len );
    this.phase = false;
    if( skipSourceString( FileInfo.TZX_MAGIC ) ) {
      processTzxFile();
    } else {
      processTapFile();
    }
  }


	/* --- private Methoden --- */

  private void addSamplesByMillis( int millis )
  {
    if( millis > 0 ) {
      int n = Math.round( (float) millis * (float) SAMPLE_RATE / 1000F );
      addSamples( this.phase ? n : -n );
    }
  }


  private void addSamplesByTStates( int tStates )
  {
    int n = Math.round( (float) tStates / T_STATES_PER_SAMPLE );
    addSamples( this.phase ? n : -n );
  }


  private void changePhase()
  {
    this.phase = !this.phase;
  }


  private void processStdTapBlock(
			int pauseMillis,
			int blockLen )
  {
    processTapBlock(
		2168,
		667,
		735,
		855,
		1710,
		8063,
		3223,
		8,
		pauseMillis,
		blockLen );
  }


  private void processTapBlock(
			int pilotPulseLen,
			int sync1PulseLen,
			int sync2PulseLen,
			int bit0PulseLen,
			int bit1PulseLen,
			int pilotPulseCnt0,
			int pilotPulseCnt1,
			int nBitsOfLastByte,
			int pauseMillis,
			int nBytes )
  {
    if( nBytes > 0 ) {
      int b        = readSourceByte();			// Flag Byte
      int pulseCnt = ((b & 0x80) != 0 ? pilotPulseCnt1 : pilotPulseCnt0);
      for( int i = 0; i < pulseCnt; i++ ) {
	addSamplesByTStates( pilotPulseLen );
	changePhase();
      }
      if( sync1PulseLen > 0 ) {
	addSamplesByTStates( sync1PulseLen );
	changePhase();
      }
      if( sync2PulseLen > 0 ) {
	addSamplesByTStates( sync2PulseLen );
	changePhase();
      }
      for(;;) {
	--nBytes;
	int nBits = (nBytes > 0 ? 8 : nBitsOfLastByte);
	for( int i = 0; i < nBits; i++ ) {
	  int pulseLen = ((b & 0x80) != 0 ? bit1PulseLen : bit0PulseLen);
	  addSamplesByTStates( pulseLen );
	  changePhase();
	  addSamplesByTStates( pulseLen );
	  changePhase();
	  b <<= 1;
	}
	if( nBytes == 0 ) {
	  break;
	}
	b = readSourceByte();
      }
      if( pauseMillis > 0 ) {
	addSamplesByMillis( 1 );
	this.phase = false;
	--pauseMillis;
	if( pauseMillis > 0 ) {
	  addSamplesByMillis( pauseMillis );
	}
      }
    }
  }


  private void processTapFile()
  {
    int blockLen = readWord();
    while( sourceByteAvailable() ) {
      processStdTapBlock( 1000, blockLen );
      blockLen = readWord();
    }
  }


  // Standard Speed Data Block
  private void processTzxBlock10()
  {
    int pauseMillis = readWord();
    int blockLen    = readWord();
    processStdTapBlock( pauseMillis, blockLen );
  }


  // Turbo Speed Data Block
  private void processTzxBlock11()
  {
    int pilotPulseLen   = readWord();
    int sync1PulseLen   = readWord();
    int sync2PulseLen   = readWord();
    int bit0PulseLen    = readWord();
    int bit1PulseLen    = readWord();
    int pilotPulseCnt   = readWord();
    int nBitsOfLastByte = readSourceByte();
    int pauseMillis     = readWord();
    int blockLen        = readInt3();
    processTapBlock(
		pilotPulseLen,
		sync1PulseLen,
		sync2PulseLen,
		bit0PulseLen,
		bit1PulseLen,
		pilotPulseCnt,
		pilotPulseCnt,
		nBitsOfLastByte,
		pauseMillis,
		blockLen );
  }


  // Pure Tone
  private void processTzxBlock12()
  {
    int pulseLen = readWord();
    int nPulses  = readWord();
    for( int i = 0; i < nPulses; i++ ) {
      addSamplesByTStates( pulseLen );
      changePhase();
    }
  }


  // Pure Sequence
  private void processTzxBlock13()
  {
    int nPulses = readSourceByte();
    for( int i = 0; i < nPulses; i++ ) {
      addSamplesByTStates( readWord() );
      changePhase();
    }
  }


  // Pure Data Block
  private void processTzxBlock14()
  {
    int bit0PulseLen    = readWord();
    int bit1PulseLen    = readWord();
    int nBitsOfLastByte = readSourceByte();
    int pauseMillis     = readWord();
    int blockLen        = readInt3();
    processTapBlock(
		0,
		0,
		0,
		bit0PulseLen,
		bit1PulseLen,
		0,
		0,
		nBitsOfLastByte,
		pauseMillis,
		blockLen );
  }


  // Direct Recording
  private void processTzxBlock15()
  {
    int tStatesPerSample = readWord();
    int pauseMillis      = readWord();
    int nBitsOfLastByte  = readSourceByte();
    int nBytes           = readInt3();
    if( nBytes > 0 ) {
      int   sourceSamples = 0;
      float tStatesFactor = (float) tStatesPerSample
					/ (float) T_STATES_PER_SAMPLE;
      do {
	int nBits = (nBytes > 1 ? 8 : Math.max( nBitsOfLastByte, 8 ));
	int b     = readSourceByte();
	for( int i = 0; i < nBits; i++ ) {
	  boolean p = ((b & 0x80) != 0);
	  if( (p != this.phase) && (sourceSamples > 0) ) {
	    int v = Math.round( (float) sourceSamples * tStatesFactor );
	    addSamples( p ? v : -v );
	    this.phase    = p;
	    sourceSamples = 0;
	  }
	  sourceSamples++;
	  b <<= 1;
	}
	--nBytes;
      } while( nBytes > 0 );
      if( sourceSamples > 0 ) {
	int v = Math.round( (float) sourceSamples * tStatesFactor );
	addSamples( this.phase ? v : -v );
      }
      changePhase();
      addSamplesByMillis( pauseMillis );
    }
  }


  // CSW Recording
  private void processTzxBlock18() throws IOException
  {
    int blockLen    = readInt4();
    int pauseMillis = readWord();
    int sampleRate  = readInt3();
    if( sampleRate != SAMPLE_RATE ) {
      throw new IOException( "Block-ID 18: Abtastrate "
			+ String.valueOf( sampleRate )
			+ " Hz nicht unterst\u00FCtzt" );
    }
    int compression = readSourceByte();
    if( compression != 1 ) {
      StringBuilder buf = new StringBuilder( 128 );
      buf.append(  "CSW-Kompressionsmethode " );
      if( compression == 2 ) {
	buf.append( "Z-RLE" );
      } else {
	buf.append( compression );
      }
      buf.append( " nicht unterst\u00FCtzt" );
      throw new IOException( buf.toString() );
    }
    skipSourceBytes( 4 );			// Gesamtanzahl Pulse
    boolean firstSample = true;
    int     dataLen     = blockLen - 10;
    while( (dataLen != 0) && sourceByteAvailable() ) {
      int v = readSourceByte();
      if( dataLen > 0 ) {
	--dataLen;
      }
      if( v == 0 ) {
	v = readInt4();
	if( dataLen > 0 ) {
	  dataLen -= 4;
	  if( dataLen < 0 ) {
	    dataLen = 0;
	  }
	}
      }
      if( firstSample ) {
	addSamples( v );
	firstSample = false;
      } else {
	addPhaseChangeSamples( v );
      }
    }
  }


  // Pause
  private void processTzxBlock20() throws IOException
  {
    int millis = readWord();
    if( millis == 0 ) {
      /*
       * In dem Fall soll das Abspielen gestoppt werden,
       * bis der Emulator es wieder startet.
       * Da das aber bei einer Umwandlung in einen Audiodatenstrom
       * nicht moeglich ist, wird eine Pause von 5 Sekunden eingelegt.
       */
      millis = 5000;
    }
    this.phase = false;
    addSamplesByMillis( millis );
  }


  // Set Signal Level
  private void processTzxBlock2B() throws IOException
  {
    skipSourceBytes( 4 );		// Versionsnummer ueberspringen
    this.phase = (readSourceByte() != 0);
  }


  private void processTzxFile() throws IOException
  {
    skipSourceBytes( 2 );		// Versionsnummer ueberspringen

    // Bloecke
    while( sourceByteAvailable() ) {
      int blockID = readSourceByte();
      switch( blockID ) {
	case 0x10:			// Standard Speed Data Block
	  processTzxBlock10();
	  break;

	case 0x11:			// Turbo Speed Data Block
	  processTzxBlock11();
	  break;

	case 0x12:			// Pure Tone
	  processTzxBlock12();
	  break;

	case 0x13:			// Pure Sequence
	  processTzxBlock13();
	  break;

	case 0x14:			// Pure Data Block
	  processTzxBlock14();
	  break;

	case 0x15:			// Direct Recording
	  processTzxBlock15();
	  break;

	case 0x18:			// CSW Recording
	  processTzxBlock18();
	  break;

	case 0x20:			// Pause
	  processTzxBlock20();
	  break;

	case 0x22:			// Group End
	  // kein Blockinhalt
	  break;

	case 0x2B:			// Set Signal Level
	  processTzxBlock2B();
	  break;

	/*
	 * zu ueberspringende Bloecke,
	 * deren Blocklaenge mit einem Byte angegeben ist
	 */
	case 0x21:			// Group Start
	case 0x30:			// Text Description
	  skipSourceBytes( readSourceByte() );
	  break;

	/*
	 * zu ueberspringende Bloecke,
	 * deren Blocklaenge mit zwei Bytes angegeben ist
	 */
	case 0x32:			// Archive Info Block
	  skipSourceBytes( readWord() );
	  break;

	case 0x35:			// Custom Info Block
	  skipSourceBytes( 16 );	// Identification String
	  skipSourceBytes( readInt4() );
	  break;

	default:
	  throw new IOException(
		String.format(
			"Die TZX-Datei enth\u00E4lt mit ID %02X ein nicht"
				+ " unterst\u00FCtztes Blockformat.",
			blockID ) );
      }
    }

    // abschliessende Pause
    addSamplesByMillis( 200 );
  }


  private int readInt3()
  {
    int b0 = readSourceByte();
    int b1 = readSourceByte();
    int b2 = readSourceByte();
    return ((b2 << 16) & 0xFF0000) | ((b1 << 8) & 0xFF00) | (b0 & 0xFF);
  }


  private int readInt4()
  {
    int b0 = readSourceByte();
    int b1 = readSourceByte();
    int b2 = readSourceByte();
    int b3 = readSourceByte();
    return ((b3 << 24) & 0xFF000000)
		| ((b2 << 16) & 0x00FF0000)
		| ((b1 << 8) & 0x0000FF00)
		| (b0 & 0x000000FF);
  }


  private int readWord()
  {
    int b0 = readSourceByte();
    int b1 = readSourceByte();
    return ((b1 << 8) & 0xFF00) | (b0 & 0x00FF);
  }
}

