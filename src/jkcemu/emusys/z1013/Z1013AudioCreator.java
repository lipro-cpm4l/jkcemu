/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Lesen einer Z1013-Datei als Audiodaten
 *
 * Die Klasse wandelt ein Speicherabbild in Audiodaten
 * entsprechend dem Z1013-Kassettenaufzeichnungsformat um.
 * Im Original werden die Frequenzen 640, 1280 und 2560 Hz verwendet
 * (leicht gerundet). Diese lassen sich mit etwa 4% Abweichung
 * direkt auf eine Abtastrate von 16000 Hz abbilden,
 * indem pro Halbwelle 12, 6, bzw. 3 Samples verwendet werden.
 * Die sich daraus ergebenden Frequenzen von 667, 1333 und 2667 Hz
 * werden problemlos von den Z1013-Laderoutinen eingelesen.
 */

package jkcemu.emusys.z1013;

import java.io.IOException;
import java.lang.*;
import java.util.NoSuchElementException;
import jkcemu.audio.BitSampleBuffer;
import jkcemu.base.ByteIterator;


public class Z1013AudioCreator extends BitSampleBuffer
{
  private boolean phase;


  public Z1013AudioCreator(
			boolean headersave,
			byte[]  dataBytes,
			int     offs,
			int     len ) throws IOException
  {
    super( 16000, 0x8000 );
    this.phase = false;

    ByteIterator iter = new ByteIterator( dataBytes, offs, len );

    // Headersave-Kennung testen und Anfangsadresse lesen
    int begAddr = 0;
    if( headersave ) {
      iter.setIndex( offs + 13 );
      for( int i = 0; i < 3; i++ ) {
	if( iter.readByte() != 0xD3 ) {
	  headersave = false;
	  break;
	}
      }
      if( headersave ) {
	iter.setIndex( offs );
	begAddr = iter.readWord();
	if( begAddr < 0 ) {
	  begAddr = 0;
	}
      }
    }

    // Bloecke erzeugen
    int blkIdx  = 0;
    int blkAddr = 0;
    iter.setIndex( offs );
    while( iter.hasNext() ) {

      // Vorton, 1. Halbschwingung, ab dem 2. Block vergroessert (kurze Pause)
      addPhaseChangeSamples( blkIdx > 0 ? 40 : 12 );

      // Vorton, restliche Halbschwingungen
      int nHalf = 27;				// (14 * 2) - 1
      if( (blkIdx == 0) || ((blkIdx == 1) && headersave) ) {
	nHalf= 3999;				// (2000 * 2) - 1
      }
      for( int i = 0; i < nHalf; i++ ) {	// weitere Halbschwingungen
	addPhaseChangeSamples( 12 );
      }

      // Trennschwingung
      addPhaseChangeSamples( 6 );
      addPhaseChangeSamples( 6 );

      // Blocknummer
      int w = 0;
      if( headersave ) {
	if( blkIdx == 0 ) {
	  blkAddr = 0x00E0;
	} else if( blkIdx == 1 ) {
	  blkAddr = begAddr;
	} else {
	  blkAddr += 0x0020;
	}
	w = blkAddr;
      }
      addWordSamples( w );
      int cks = w;
      blkIdx++;

      // Datenwoerter
      for( int i = 0; i < 16; i++ ) {
	int b0 = iter.readByte();
	w      = ((iter.readByte() << 8) & 0xFF00) | (b0 & 0x00FF);
	addWordSamples( w );
	cks += w;
      }
      // Pruefsumme
      addWordSamples( cks );
    }
  }


  public Z1013AudioCreator(
			boolean headersave,
			byte[]  dataBytes ) throws IOException
  {
    this( headersave, dataBytes, 0, dataBytes.length );
  }


	/* --- private Methoden --- */

  private void addPhaseChangeSamples( int value ) throws IOException
  {
    this.phase = !this.phase;
    addSamples( value, this.phase );
  }


  private void addWordSamples( int value ) throws IOException
  {
    for( int i = 0; i < 16; i++ ) {
      if( (value & 0x01) != 0 ) {
	addPhaseChangeSamples( 6 );
      } else {
	addPhaseChangeSamples( 3 );
	addPhaseChangeSamples( 3 );
      }
      value >>= 1;
    }
  }
}
