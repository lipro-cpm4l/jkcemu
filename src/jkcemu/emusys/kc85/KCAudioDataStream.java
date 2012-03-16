/*
 * (c) 2011-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Lesen einer KCB-, KCC- oder KC-TAP-Datei als InputStream von Audio-Daten
 *
 * Die Klasse wandelt ein Speicherabbild in Audio-Daten
 * entsprechend dem KC85- und Z9001-Kassettenaufzeichnungsformat um.
 * Laut KC85/3- bzw. KC85/4-Systemhandbuch arbeitet die Kassettenaufzeichnung
 * mit den Frequenzen 600, 1200 und 2400 Hz.
 * In der Realitaet lasst sich dass dann aber nicht in den KC85 einlesen.
 * Vielmehr muessen die drei Frquenzen etwa 10% bis 20% niedriger sein.
 * Bei den verschiedenen KC-BASIC-Interpretern fuer den Z1013
 * muessen die Frequenzen sogar etwa 15% bis 20% niedriger sein.
 *
 * Es wird deshalb hier mit einer Samplerate von 8000 Hz gearbeitet
 * und daraus durch ganzahlige Teilung die Frequenzen 500, 1000 und 2000 Hz
 * ermittelt, die somit etwa 17% unter der Spezifikation liegen.
 */

package jkcemu.emusys.kc85;

import java.io.IOException;
import java.lang.*;
import jkcemu.base.*;


public class KCAudioDataStream extends EmuSysAudioDataStream
{
  public KCAudioDataStream(
			boolean tapFmt,
			int     blkNum,
			byte[]  buf,
			int     offs,
			int     len ) throws IOException
  {
    super( 8000F, buf, offs, len );
    if( tapFmt && !skipString( FileInfo.KCTAP_HEADER ) ) {
      throw new IOException( "KC-TAP-Header erwartet" );
    }

    boolean firstBlk = true;
    while( sourceByteAvailable() ) {

      // Vorton
      int nHalf = 320;		// Anzahl Halbschwingungen
      if( tapFmt ) {
	/*
	 * Beginn einer neuen Teildatei innerhalb einer Multi-TAP-Datei?
	 * Wenn ja, dann Header uerberspringen und langer Vorton
	 */
	if( skipString( FileInfo.KCTAP_HEADER ) ) {
	  nHalf = 16000;
	}
      }
      if( firstBlk ) {
	nHalf = 16000;		// beim 1. Block immer langer Vortan
      }
      for( int i = 0; i < nHalf; i++ ) {
	addSamples( 4 );
      }
      firstBlk = false;

      // Trennschwingung
      addSamples( 8 );
      addSamples( 8 );

      // Blocknummer und Datenbytes
      int cks = 0;
      int nBytes = 128;
      if( tapFmt ) {
	nBytes = 129;
      } else {
	addByteSamples( getAvailableSourceBytes() > 128 ? blkNum++ : 0xFF );
      }
      for( int i = 0; i < 129; i++ ) {
	int b = readSourceByte();
	addByteSamples( b );
	if( i > 0 ) {
	  cks = (cks + b) & 0xFF;
	}
      }

      // Pruefsumme
      addByteSamples( cks );
    }

    // abschliessender Phasenwechsel
    if( getFrameLength() > 0 ) {
      addSamples( 40 );
    }
  }


  public KCAudioDataStream(
			boolean tapFmt,
			int     blkNum,
			byte[]  buf ) throws IOException
  {
    this( tapFmt, blkNum, buf, 0, buf.length );
  }


	/* --- private Methoden --- */

  private void addByteSamples( int value )
  {
    for( int i = 0; i < 8; i++ ) {
      if( (value & 0x01) != 0 ) {
	addSamples( 4 );
	addSamples( 4 );
      } else {
	addSamples( 2 );
	addSamples( 2 );
      }
      value >>= 1;
    }
    addSamples( 8 );
    addSamples( 8 );
  }
}
