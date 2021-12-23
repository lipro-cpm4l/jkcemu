/*
 * (c) 2014-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Lesen einer ZX-TAP- oder TZX-Datei als Audiodaten
 */

package jkcemu.emusys.zxspectrum;

import java.io.IOException;
import java.util.NoSuchElementException;
import jkcemu.audio.BitSampleBuffer;
import jkcemu.base.ByteIterator;
import jkcemu.base.EmuUtil;
import jkcemu.file.FileInfo;


public class ZXSpectrumAudioCreator extends BitSampleBuffer
{
  private final static int   SAMPLE_RATE         = 44100;
  private final static float T_STATES_PER_SAMPLE =
				3500000F / (float) SAMPLE_RATE;

  private ByteIterator iter;
  private boolean      phase;


  public ZXSpectrumAudioCreator(
			byte[] dataBytes,
			int    offs,
			int    len ) throws IOException
  {
    super( SAMPLE_RATE, 0x8000 );
    this.phase = false;
    if( EmuUtil.isTextAt( FileInfo.TZX_MAGIC, dataBytes, offs ) ) {
      int magicLen = FileInfo.TZX_MAGIC.length();
      this.iter    = new ByteIterator(
				dataBytes,
				offs + magicLen,
				len - magicLen );
      processTzxFile();
    } else {
      this.iter = new ByteIterator( dataBytes, offs, len );
      processTapFile();
    }
  }


	/* --- private Methoden --- */

  private void addSamplesByMillis( int millis ) throws IOException
  {
    if( millis > 0 ) {
      addSamples(
	Math.round( (float) millis * (float) SAMPLE_RATE / 1000F ),
	this.phase );
    }
  }


  private void addSamplesByTStates( int tStates ) throws IOException
  {
    addSamples(
	Math.round( (float) tStates / T_STATES_PER_SAMPLE ),
	this.phase );
  }


  private void changePhase()
  {
    this.phase = !this.phase;
  }


  private void processStdTapBlock(
			int pauseMillis,
			int blockLen )
		throws IOException, NoSuchElementException
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
		throws IOException, NoSuchElementException
  {
    if( nBytes > 0 ) {
      int b        = this.iter.nextByte();			// Flag Byte
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
	b = this.iter.nextByte();
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


  private void processTapFile() throws IOException, NoSuchElementException
  {
    try {
      int blockLen = this.iter.nextWord();
      while( this.iter.hasNext() ) {
	processStdTapBlock( 1000, blockLen );
	blockLen = this.iter.nextWord();
      }
    }
    catch( NoSuchElementException ex ) {}
  }


  // Standard Speed Data Block
  private void processTzxBlock10() throws IOException, NoSuchElementException
  {
    int pauseMillis = this.iter.nextWord();
    int blockLen    = this.iter.nextWord();
    processStdTapBlock( pauseMillis, blockLen );
  }


  // Turbo Speed Data Block
  private void processTzxBlock11() throws IOException, NoSuchElementException
  {
    int pilotPulseLen   = this.iter.nextWord();
    int sync1PulseLen   = this.iter.nextWord();
    int sync2PulseLen   = this.iter.nextWord();
    int bit0PulseLen    = this.iter.nextWord();
    int bit1PulseLen    = this.iter.nextWord();
    int pilotPulseCnt   = this.iter.nextWord();
    int nBitsOfLastByte = this.iter.nextByte();
    int pauseMillis     = this.iter.nextWord();
    int blockLen        = this.iter.nextInt3LE();
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
  private void processTzxBlock12() throws IOException, NoSuchElementException
  {
    int pulseLen = this.iter.nextWord();
    int nPulses  = this.iter.nextWord();
    for( int i = 0; i < nPulses; i++ ) {
      addSamplesByTStates( pulseLen );
      changePhase();
    }
  }


  // Pure Sequence
  private void processTzxBlock13() throws IOException, NoSuchElementException
  {
    int nPulses = this.iter.nextByte();
    for( int i = 0; i < nPulses; i++ ) {
      addSamplesByTStates( this.iter.nextWord() );
      changePhase();
    }
  }


  // Pure Data Block
  private void processTzxBlock14() throws IOException, NoSuchElementException
  {
    int bit0PulseLen    = this.iter.nextWord();
    int bit1PulseLen    = this.iter.nextWord();
    int nBitsOfLastByte = this.iter.nextByte();
    int pauseMillis     = this.iter.nextWord();
    int blockLen        = this.iter.nextInt3LE();
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
  private void processTzxBlock15() throws IOException, NoSuchElementException
  {
    int tStatesPerSample = this.iter.nextWord();
    int pauseMillis      = this.iter.nextWord();
    int nBitsOfLastByte  = this.iter.nextByte();
    int nBytes           = this.iter.nextInt3LE();
    if( nBytes > 0 ) {
      int samples = Math.round( (float) tStatesPerSample
					/ T_STATES_PER_SAMPLE );
      do {
	int nBits = (nBytes > 1 ? 8 : Math.max( nBitsOfLastByte, 8 ));
	int b     = this.iter.nextByte();
	for( int i = 0; i < nBits; i++ ) {
	  this.phase = ((b & 0x80) != 0);
	  addSamples( samples, this.phase );
	  b <<= 1;
	}
	--nBytes;
      } while( nBytes > 0 );
      changePhase();
      addSamplesByMillis( pauseMillis );
    }
  }


  // CSW Recording
  private void processTzxBlock18() throws IOException, NoSuchElementException
  {
    int blockLen    = this.iter.nextInt4LE();
    int pauseMillis = this.iter.nextWord();
    int sampleRate  = this.iter.nextInt3LE();
    if( sampleRate != SAMPLE_RATE ) {
      throw new IOException( "Block-ID 18: Abtastrate "
			+ String.valueOf( sampleRate )
			+ " Hz nicht unterst\u00FCtzt" );
    }
    int compression = this.iter.nextByte();
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
    this.iter.skip( 4 );			// Gesamtanzahl Pulse
    boolean firstSample = true;
    int     dataLen     = blockLen - 10;
    while( (dataLen != 0) && this.iter.hasNext() ) {
      int v = this.iter.nextByte();
      if( dataLen > 0 ) {
	--dataLen;
      }
      if( v == 0 ) {
	v = this.iter.nextInt4LE();
	if( dataLen > 0 ) {
	  dataLen -= 4;
	  if( dataLen < 0 ) {
	    dataLen = 0;
	  }
	}
      }
      if( firstSample ) {
	firstSample = false;
      } else {
	changePhase();
      }
      addSamples( v, this.phase );
    }
  }


  // Pause
  private void processTzxBlock20() throws IOException, NoSuchElementException
  {
    int millis = this.iter.nextWord();
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
  private void processTzxBlock2B() throws NoSuchElementException
  {
    int len    = this.iter.nextInt4LE();
    this.phase = (this.iter.nextByte() != 0);
    --len;
    if( len > 0 ) {
      this.iter.skip( len );
    }
  }


  private void processTzxFile() throws IOException
  {
    try {
      this.iter.skip( 2 );		// Versionsnummer ueberspringen

      // Bloecke
      while( this.iter.hasNext() ) {
	int blockID = this.iter.nextByte();
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
	    this.iter.skip( this.iter.nextByte() );
	    break;

	  /*
	   * zu ueberspringende Bloecke,
	   * deren Blocklaenge mit zwei Bytes angegeben ist
	   */
	  case 0x32:			// Archive Info Block
	    this.iter.skip( this.iter.nextWord() );
	    break;

	  case 0x35:			// Custom Info Block
	    this.iter.skip( 16 );	// Identification String
	    this.iter.skip( this.iter.nextInt4LE() );
	    break;

	  default:
	    throw new IOException(
		String.format(
			"Die TZX-Datei enth\u00E4lt mit ID %02X ein nicht"
				+ " unterst\u00FCtztes Blockformat.",
			blockID ) );
	}
      }
    }
    catch( NoSuchElementException ex ) {}
  }
}
