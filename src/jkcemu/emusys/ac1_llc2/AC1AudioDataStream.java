/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Lesen einer AC1-Datei als InputStream von Audio-Daten
 * im klassischen Kassettenaufzeichnungsformat
 */

package jkcemu.emusys.ac1_llc2;

import java.lang.*;
import jkcemu.base.EmuSysAudioDataStream;


public class AC1AudioDataStream extends EmuSysAudioDataStream
{
  public AC1AudioDataStream(
			boolean basic,
			byte[]  buf,
			int     offs,
			int     len,
			String  fileName,
			int     begAddr,
			int     startAddr )
  {
    super( 44100F, buf, offs, len );
    if( sourceByteAvailable() ) {

      // 512 Nullbytes
      for( int i = 0; i < 512; i++ ) {
	addByteSamples( 0 );
      }

      // Polarisationsbyte
      addByteSamples( 0xE6 );

      // Dateiname
      int n = 0;
      if( basic ) {
	n = 6;
      } else {
        n = 16;
        addByteSamples( 0x55 );
      }
      if( fileName != null ) {
	int l = fileName.length();
	int p = 0;
	while( (n > 0) && (p < l) ) {
	  char ch = fileName.charAt( p++ );
	  addByteSamples( (ch >= 0x32) && (ch < 0x7F) ? ch : '_' );
	  --n;
	}
      }
      while( n > 0 ) {
	addByteSamples( 0x20 );
	--n;
      }
      if( basic ) {
	addByteSamples( 0xD3 );			// Kennzeichen BASIC-Programm
	addByteSamples( len >> 8 );		// Laenge H-Byte
	addByteSamples( len );			// Laenge L-Byte
	for( int i = 0; i < len; i++ ) {
	  addByteSamples( readSourceByte() );	// Datenbyte
	}
      } else {

	// 256 Nullbytes
	for( int i = 0; i < 256; i++ ) {
	  addByteSamples( 0 );
	}

	// Datenbloecke
	int addr   = begAddr;
	int blkLen = Math.min( this.srcEOF - this.srcPos, 256 );
	while( blkLen > 0 ) {
	  addByteSamples( 0x3C );		// Kennzeichen Datenblock
	  addByteSamples( blkLen );		// Blocklaenge
	  addByteSamples( addr );		// Blockadresse (L-Byte)
	  int cks = addr & 0xFF;
	  int b   = (addr >> 8) & 0xFF;
	  addByteSamples( b );			// Blockadresse (H-Byte)
	  cks = (cks + b) & 0xFF;
	  addr += blkLen;
	  for( int i = 0; i < blkLen; i++ ) {
	    b = readSourceByte();
	    addByteSamples( b );		// Datenbyte
	    cks = (cks + b) & 0xFF;
	  }
	  addByteSamples( cks );		// Pruefsumme
	  blkLen = Math.min( this.srcEOF - this.srcPos, 256 );
	}

	// Endeblock
	addByteSamples( 0x78 );
	addByteSamples( startAddr );
	addByteSamples( startAddr >> 8 );
      }
    }
  }


  public AC1AudioDataStream(
			boolean basic,
			byte[]  buf,
			String  fileName,
			int     begAddr,
			int     startAddr )
  {
    this( basic, buf, 0, buf.length, fileName, begAddr, startAddr );
  }


	/* --- private Methoden --- */

  private void addByteSamples( int value )
  {
    for( int i = 0; i < 8; i++ ) {
      int v = ((value & 0x80) != 0 ? -15 : 15);
      addSamples( v );
      addSamples( -v );
      value <<= 1;
    }
  }
}

