/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * LZW-Encoder
 *
 * Fuer diese Klasse wurde der im Internet frei verfuegbare Programmcode
 * gifcompress.c, der wiederum auf dem Programmcode des Unix-Kommandos
 * compress beruht, nach Java portiert.
 *
 * Autoren von compress mit dem LZW-Algorithmus:
 *   Spencer W. Thomas
 *   Jim McKie
 *   Steve Davies
 *   Ken Turkowski
 *   James A. Woods
 *   Joe Orost
 *
 * Autor der GIF-Erweiterungen:
 *   David Rowley (mgardi@watdcsu.waterloo.edu)
 *
 * Kommentar im urspruenglichen C-Programmcode zum Algorithmus:
 *   Algorithm:  use open addressing double hashing (no chaining) on the
 *   prefix code / next character combination.  We do a variant of Knuth's
 *   algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
 *   secondary probe.  Here, the modular division first probe is gives way
 *   to a faster exclusive-or manipulation.  Also do block compression with
 *   an adaptive reset, whereby the code table is cleared when the compression
 *   ratio decreases, but after the table fills.  The variable-length output
 *   codes are re-sized at this point, and a special CLEAR code is generated
 *   for the decompressor.  Late addition:  construct the table according to
 *   file size for noticeable speed improvement on small files.  Please direct
 *   questions about this implementation to ames!jaw.
 */

package jkcemu.image;

import java.io.*;
import java.lang.*;
import java.util.Arrays;


public class LZWEncoder
{
  private static final int   BITS         = 12;
  private static final int   MAX_MAX_CODE = (1 << BITS);
  private static final int   HASH_SIZE    = 5003;	// 80% Belegung
  private static final int[] masks = {
	0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F,
	0x00FF, 0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF,
	0xFFFF };

  private byte[]       dataBuf;
  private int          initBits;
  private int          initCodeSize;
  private int          curAcc       = 0;
  private int          curBits      = 0;
  private int          clearCode    = 0;
  private int          eofCode      = 0;
  private int          freeEntry    = 0;
  private int          nBits        = 0;
  private int          dataPos      = 0;
  private int          maxCode      = 0;
  private int          outPos       = 0;
  private boolean      clearFlag    = false;
  private byte[]       outBuf       = new byte[ 256 ];
  private int[]        codeTab      = new int[ HASH_SIZE ];
  private int[]        hashTab      = new int[ HASH_SIZE ];
  private OutputStream out;


  public LZWEncoder( byte[] dataBuf, int initCodeSize )
  {
    this.dataBuf      = dataBuf;
    this.initCodeSize = initCodeSize;
    this.initBits     = this.initCodeSize + 1;
  }


  public void encode( OutputStream out ) throws IOException
  {
    this.out        = out;
    this.outPos     = 0;
    this.dataPos    = 0;
    this.freeEntry  = 0;
    this.clearFlag  = false;
    this.nBits      = this.initBits;
    this.maxCode    = getMaxCode( this.nBits );
    this.clearCode  = (1 << (this.initBits - 1));
    this.eofCode    = this.clearCode + 1;
    this.freeEntry  = this.clearCode + 2;
    this.out.write( this.initCodeSize );

    int hShift = 0;
    for( int fCode = HASH_SIZE; fCode < 65536; fCode *= 2 ) {
      ++hShift;
    }
    hShift = 8 - hShift;			// Hashcode-Grenze

    int hSizeReg = HASH_SIZE;
    clearHash( hSizeReg );
    writeCode( this.clearCode );

    int e = nextByte();
    int b = nextByte();
    while( b >= 0 ) {
      boolean skip  = false;
      int     fCode = (b << BITS) + e;
      int     i     = (b << hShift) ^ e;	// XOR hashing
      if( this.hashTab[ i ] == fCode ) {
	e = this.codeTab[ i ];
	skip = true;
      } else if (this.hashTab[ i ] >= 0 ) {	// Position belegt
	// Sekundar-Hash nach G. Knott
	int disp = hSizeReg - i;
	if( i == 0 ) {
	  disp = 1;
	}
	do {
	  if( (i -= disp) < 0 ) {
	    i += hSizeReg;
	  }
	  if( this.hashTab[ i ] == fCode ) {
	    e    = this.codeTab[ i ];
	    skip = true;
	    break;
	  }
	} while( this.hashTab[ i ] >= 0 );
      }
      if( !skip ) {
	writeCode( e );
	e = b;
	if( this.freeEntry < MAX_MAX_CODE ) {
	  this.codeTab[ i ] = this.freeEntry++;
	  this.hashTab[ i ] = fCode;
	} else {
	  clearBlock();
	}
      }
      b = nextByte();
    }
    writeCode( e );
    writeCode( this.eofCode );
    this.out.write( 0 );		// Blockende
  }


	/* --- private Methoden --- */

  private void clearBlock() throws IOException
  {
    clearHash( HASH_SIZE );
    this.freeEntry = this.clearCode + 2;
    this.clearFlag = true;
    writeCode( this.clearCode );
  }


  private void clearHash( int size )
  {
    if( size < this.hashTab.length ) {
      Arrays.fill( this.hashTab, 0, size, (byte) -1 );
    } else {
      Arrays.fill( this.hashTab, (byte) -1 );
    }
  }


  private void flushBuf() throws IOException
  {
    if( this.outPos > 0 ) {
      this.out.write( this.outPos );
      this.out.write( this.outBuf, 0, this.outPos );
      this.outPos = 0;
    }
  }


  private static int getMaxCode( int nBits )
  {
    return (1 << nBits) - 1;
  }


  private int nextByte()
  {
    int rv = -1;
    if( this.dataPos < this.dataBuf.length ) {
      rv = (int) this.dataBuf[ this.dataPos++ ] & 0xFF;
    }
    return rv;
  }


  private void putByte( int b ) throws IOException
  {
    this.outBuf[ this.outPos++ ] = (byte) b;
    if( this.outPos >= 254 ) {
      flushBuf();
    }
  }


  private void writeCode( int code ) throws IOException
  {
    this.curAcc &= masks[ this.curBits ];
    if( this.curBits > 0 ) {
      this.curAcc |= (code << this.curBits);
    } else {
      this.curAcc = code;
    }
    this.curBits += this.nBits;

    while( this.curBits >= 8 ) {
      putByte( this.curAcc & 0xFF );
      this.curAcc >>= 8;
      this.curBits -= 8;
    }

    if( this.freeEntry > this.maxCode || this.clearFlag ) {
      if( this.clearFlag ) {
	this.nBits     = this.initBits;
	this.maxCode   = getMaxCode( this.nBits );
	this.clearFlag = false;
      } else {
	++this.nBits;
	if( this.nBits == BITS ) {
	  this.maxCode = MAX_MAX_CODE;
	} else {
	  this.maxCode = getMaxCode( this.nBits );
	}
      }
    }

    if( code == this.eofCode ) {
      // Dateiende -> Puffer leeren
      while( this.curBits > 0 ) {
	putByte( this.curAcc & 0xFF );
	this.curAcc >>= 8;
	this.curBits -= 8;
      }
      flushBuf();
    }
  }
}
