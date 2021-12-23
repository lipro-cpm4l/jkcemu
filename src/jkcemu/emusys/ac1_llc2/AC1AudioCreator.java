/*
 * (c) 2011-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Lesen einer AC1-Datei als Audiodaten
 * im klassischen Kassettenaufzeichnungsformat
 */

package jkcemu.emusys.ac1_llc2;

import java.io.IOException;
import jkcemu.audio.BitSampleBuffer;
import jkcemu.base.ByteIterator;


public class AC1AudioCreator extends BitSampleBuffer
{
  public AC1AudioCreator(
			boolean basic,
			byte[]  dataBytes,
			int     offs,
			int     len,
			String  fileName,
			int     begAddr,
			int     startAddr ) throws IOException
  {
    super( 44100, 0x8000 );
    ByteIterator iter = new ByteIterator( dataBytes, offs, len );
    if( iter.hasNext() ) {

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
	  addByteSamples( iter.readByte() );	// Datenbyte
	}
      } else {

	// 256 Nullbytes
	for( int i = 0; i < 256; i++ ) {
	  addByteSamples( 0 );
	}

	// Datenbloecke
	int addr   = begAddr;
	int blkLen = Math.min( iter.available(), 256 );
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
	    b = iter.readByte();
	    addByteSamples( b );		// Datenbyte
	    cks = (cks + b) & 0xFF;
	  }
	  addByteSamples( cks );		// Pruefsumme
	  blkLen = Math.min( iter.available(), 256 );
	}

	// Endeblock
	addByteSamples( 0x78 );
	addByteSamples( startAddr );
	addByteSamples( startAddr >> 8 );
      }
    }
  }


  public AC1AudioCreator(
			boolean basic,
			byte[]  dataBytes,
			String  fileName,
			int     begAddr,
			int     startAddr ) throws IOException
  {
    this(
	basic,
	dataBytes,
	0,
	dataBytes.length,
	fileName,
	begAddr,
	startAddr );
  }


	/* --- private Methoden --- */

  private void addByteSamples( int value ) throws IOException
  {
    for( int i = 0; i < 8; i++ ) {
      boolean state = ((value & 0x80) != 0);
      addSamples( 15, state );
      addSamples( 15, !state );
      value <<= 1;
    }
  }
}
