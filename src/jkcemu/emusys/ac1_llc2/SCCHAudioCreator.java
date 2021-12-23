/*
 * (c) 2011-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Lesen einer AC1- oder LLC2-Datei als Audiodaten im TurboSave-Format
 */

package jkcemu.emusys.ac1_llc2;

import java.io.IOException;
import jkcemu.audio.BitSampleBuffer;
import jkcemu.base.ByteIterator;


public class SCCHAudioCreator extends BitSampleBuffer
{
  private boolean phase;


  public SCCHAudioCreator(
			byte[] dataBytes,
			int    offs,
			int    len,
			String fileName,
			char   fileType,
			int    begAddr,
			int    endAddr ) throws IOException
  {
    super( 8000, 0x8000 );
    this.phase = false;

    ByteIterator iter = new ByteIterator( dataBytes, offs, len );
    if( iter.hasNext() ) {

      // 1000 Schwingungen Vorton
      for( int i = 0; i < 2000; i++ ) {
	addPhaseChangeSamples( 4 );
      }

      // Trennschwingung: 2x 1-Bit
      for( int i = 0; i < 2; i++ ) {
	addPhaseChangeSamples( 2 );
      }

      // Adresse Kopfpuffer
      int cks = 0x1896;		// 0x1896: Kopfpuffer in Monitor 8.0
      addWordSamples( cks );

      // 8x N
      byte[] m = new byte[ 32 ];
      int    p = 0;
      int    n = 8;
      while( n > 0 ) {
	m[ p++ ] = (byte) 'N';
	--n;
      }

      // 16 Zeichen Name
      n = 16;
      if( fileName != null ) {
	int l = fileName.length();
	int i = 0;
	while( (n > 0) && (i < l) ) {
	  char ch = fileName.charAt( i++ );
	  if( (ch >= 0x20) && (ch < 0x7F) ) {
	    m[ p++ ] = (byte) ch;
	  } else {
	    m[ p++ ] = (byte) '_';
	  }
	  --n;
	}
      }
      while( n > 0 ) {
	m[ p++ ] = (byte) 0x20;
	--n;
      }

      // Doppelpunkt, Leerzeichen, Dateityp
      m[ p++ ] = (byte) ':';
      m[ p++ ] = (byte) 0x20;
      m[ p++ ] = (byte) fileType;

      // Endadresse
      m[ p++ ] = (byte) endAddr;
      m[ p++ ] = (byte) (endAddr >> 8);

      // Anfangsadresse
      m[ p++ ] = (byte) begAddr;
      m[ p++ ] = (byte) (begAddr >> 8);

      // Leerzeichen
      m[ p++ ] = (byte) 0x20;

      // Vorblock in Audiodaten wandeln
      p = 0;
      for( int i = 0; i < 16; i++ ) {
	int w = 0;
	if( p < m.length ) {
	  w = (int) m[ p++ ] & 0xFF;
	  if( p < m.length ) {
	    w |= (((int) m[ p++ ] << 8) & 0xFF00);
	  }
	}
	addWordSamples( w );
	cks += w;
      }

      // Pruefsumme
      addWordSamples( cks );

      // Datenbloecke
      boolean pre  = true;
      int     addr = begAddr & 0xFFFF;
      while( iter.hasNext() ) {

	// Vorton
	n = 14;				// 7 Schwingungen
	if( pre ) {
	  n   = 2000;			// 1000 Schwingungen
	  pre = false;
	}
	for( int i = 0; i < n; i++ ) {
	  addPhaseChangeSamples( 4 );
	}

	// Trennschwingung: 2x 1-Bit
	for( int i = 0; i < 2; i++ ) {
	  addPhaseChangeSamples( 2 );
	}

	// Blockadresse
	addWordSamples( addr );
	cks  = addr;
	addr = (addr + 32) & 0xFFFF;

	// 32 Datenbytes = 16 Datenwoerter
	for( int i = 0; i < 16; i++ ) {
	  int b0 = iter.readByte();
	  int w  = ((iter.readByte() << 8) & 0xFF00) & (b0 & 0x00FF);
	  addWordSamples( w );
	  cks += w;
	}

	// Pruefsumme
	addWordSamples( cks );
      }
    }

    // abschliessender Phasenwechsel
    if( getFrameCount() > 0 ) {
      addPhaseChangeSamples( 16 );
    }
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
	addPhaseChangeSamples( 2 );
      } else {
	addPhaseChangeSamples( 1 );
	addPhaseChangeSamples( 1 );
      }
      value >>= 1;
    }
  }
}
