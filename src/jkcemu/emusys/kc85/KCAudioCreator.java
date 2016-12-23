/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Lesen einer KCB-, KCC- oder KC-TAP-Datei als Audiodaten
 *
 * Die Klasse wandelt ein Speicherabbild in Audiodaten
 * entsprechend dem KC85- und Z9001-Kassettenaufzeichnungsformat um.
 * Laut KC85/3- bzw. KC85/4-Systemhandbuch arbeitet die Kassettenaufzeichnung
 * mit den Frequenzen 600, 1200 und 2400 Hz.
 * In der Realitaet laesst sich dass dann aber nicht in den KC85 einlesen.
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
import jkcemu.audio.BitSampleBuffer;
import jkcemu.base.ByteIterator;
import jkcemu.base.FileInfo;


public class KCAudioCreator extends BitSampleBuffer
{
  private boolean phase;


  public KCAudioCreator(
		boolean tapFmt,
		int     blkNum,		// nur bei tapFmt == false relevant
		byte[]  dataBytes,
		int     offs,
		int     len ) throws IOException
  {
    super( 8000, 0x8000 );
    this.phase = false;

    ByteIterator iter = new ByteIterator( dataBytes, offs, len );
    if( tapFmt && !skipString( iter, FileInfo.KCTAP_MAGIC ) ) {
      throw new IOException( "KC-TAP-Kopf erwartet" );
    }

    boolean      firstBlk = true;
    while( iter.hasNext() ) {

      // Vorton
      int nHalf = 320;		// Anzahl Halbschwingungen
      if( tapFmt ) {
	/*
	 * Beginn einer neuen Teildatei innerhalb einer Multi-TAP-Datei?
	 * Wenn ja, dann Header uerberspringen und langer Vorton
	 */
	if( skipString( iter, FileInfo.KCTAP_MAGIC ) ) {
	  nHalf = 16000;
	}
      }
      if( firstBlk ) {
	nHalf = 16000;		// beim 1. Block immer langer Vortan
      }
      for( int i = 0; i < nHalf; i++ ) {
	addPhaseChangeSamples( 4 );
      }
      firstBlk = false;

      // Trennschwingung
      addPhaseChangeSamples( 8 );
      addPhaseChangeSamples( 8 );

      // Blocknummer
      int b = 0;
      if( tapFmt ) {
	b = iter.readByte();
      } else {
	b = (iter.available() > 128 ? blkNum++ : 0xFF);
      }
      addByteSamples( b );

      // Datenbytes
      int cks = 0;
      for( int i = 0; i < 128; i++ ) {
	b = iter.readByte();
	addByteSamples( b );
	cks = (cks + b) & 0xFF;
      }

      // Pruefsumme
      addByteSamples( cks );
    }

    // abschliessender Phasenwechsel
    if( getFrameCount() > 0 ) {
      addPhaseChangeSamples( 40 );
    }
  }


  public KCAudioCreator(
		boolean tapFmt,
		int     blkNum,		// nur bei tapFmt == false relevant
		byte[]  buf ) throws IOException
  {
    this( tapFmt, blkNum, buf, 0, buf.length );
  }


	/* --- private Methoden --- */

  private void addPhaseChangeSamples( int value ) throws IOException
  {
    this.phase = !this.phase;
    addSamples( value, this.phase );
  }


  private void addByteSamples( int value ) throws IOException
  {
    for( int i = 0; i < 8; i++ ) {
      if( (value & 0x01) != 0 ) {
	addPhaseChangeSamples( 4 );
	addPhaseChangeSamples( 4 );
      } else {
	addPhaseChangeSamples( 2 );
	addPhaseChangeSamples( 2 );
      }
      value >>= 1;
    }
    addPhaseChangeSamples( 8 );
    addPhaseChangeSamples( 8 );
  }


  private static boolean skipString( ByteIterator iter, String text )
  {
    boolean rv     = true;
    int     begPos = iter.getIndex();
    int     len    = text.length();
    for( int i = 0; i < len; i++ ) {
      if( iter.readByte() != text.charAt( i ) ) {
	iter.setIndex( begPos );
	rv = false;
	break;
      }
    }
    return rv;
  }
}
