/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Lesen einer Z1013-Datei als InputStream von Audio-Daten
 *
 * Die Klasse wandelt ein Speicherabbild in Audio-Daten
 * entsprechend dem Z1013-Kassettenaufzeichnungsformat um.
 * Im Original werden die Frequenzen 640, 1280 und 2560 Hz verwendet
 * (leicht gerundet). Diese lassen sich mit etwa 4% Abweichung
 * direkt auf eine Sample-Rate von 16000 Hz abbilden,
 * indem pro Halbwelle 12, 6, bzw. 3 Samples verwendet werden.
 * Die sich daraus ergebenden Frequenzen von 667, 1333 und 2667 Hz
 * werden problemlos von den Z1013-Laderoutinen eingelesen.
 */

package jkcemu.emusys.z1013;

import java.io.IOException;
import java.lang.*;
import jkcemu.base.EmuSysAudioDataStream;


public class Z1013AudioDataStream extends EmuSysAudioDataStream
{
  public Z1013AudioDataStream(
			boolean headersave,
			byte[]  buf,
			int     offs,
			int     len )
  {
    super( 16000F, buf, offs, len );

    // Anfangsadresse lesen und Headersave-Kennung testen
    int begAddr = 0;
    if( headersave ) {
      boolean hs = false;
      if( (this.srcPos + 32) <= this.srcEOF ) {
	if( (this.srcBuf[ this.srcPos + 13 ] == (byte) 0xD3)
	    && (this.srcBuf[ this.srcPos + 14 ] == (byte) 0xD3)
	    && (this.srcBuf[ this.srcPos + 15 ] == (byte) 0xD3) )
	{
	  hs      = true;
	  begAddr = ((this.srcBuf[ this.srcPos + 1 ]<< 8) & 0xFF00)
				| (this.srcBuf[ this.srcPos ] & 0x00FF);
	}
      }
      if( !hs ) {
	headersave = false;
      }
    }

    // neuer Block
    int     blkIdx  = 0;
    int     blkAddr = 0;
    while( sourceByteAvailable() ) {

      // Vorton, 1. Halbschwingung, ab dem 2. Block vergroessert (kurze Pause)
      addSamples( blkIdx > 0 ? 40 : 12 );

      // Vorton, restliche Halbschwingungen
      int nHalf = 27;				// (14 * 2) - 1
      if( (blkIdx == 0) || ((blkIdx == 1) && headersave) ) {
	nHalf= 3999;				// (2000 * 2) - 1
      }
      for( int i = 0; i < nHalf; i++ ) {	// weitere Halbschwingungen
	addSamples( 12 );
      }

      // Trennschwingung
      addSamples( 6 );
      addSamples( 6 );

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
	w = readSourceByte();
	w |= ((readSourceByte() << 8) & 0xFF00);
	addWordSamples( w );
	cks += w;
      }

      // Pruefsumme
      addWordSamples( cks );
    }

    // abschliessender Phasenwechsel
    if( getFrameLength() > 0 ) {
      addSamples( 40 );
    }
  }


  public Z1013AudioDataStream( boolean headersave, byte[] buf )
  {
    this( headersave, buf, 0, buf.length );
  }


	/* --- private Methoden --- */

  private void addWordSamples( int value )
  {
    for( int i = 0; i < 16; i++ ) {
      if( (value & 0x01) != 0 ) {
	addSamples( 6 );
      } else {
	addSamples( 3 );
	addSamples( 3 );
      }
      value >>= 1;
    }
  }
}

