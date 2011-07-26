/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
* Lesen einer KC-TAP-Datei als InputStream von Audio-Daten
 *
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

package jkcemu.audio;

import java.io.*;
import java.lang.*;
import javax.sound.sampled.AudioFormat;


public class KCTapAudioInputStream extends InputStream
{
  private static final String KCTAP_HEADER = "\u00C3KC-TAPE by AF.\u0020";

  private byte[]      buf;
  private int         bufSize;
  private int         pos;
  private int         stage;
  private int         nPre;
  private int         nH;
  private int         nL;
  private int         posInBlk;
  private int         cks;
  private int         curByte;
  private int         nRemainBits;
  private long        frameLen;
  private AudioFormat audioFmt;


  public KCTapAudioInputStream(
			byte[] buf,
			int    offs,
			int    len ) throws IOException
  {
    this.buf         = buf;
    this.bufSize     = offs + len;
    this.pos         = offs;
    this.stage       = 0;
    this.posInBlk    = 0;
    this.nPre        = 0;
    this.nH          = 0;
    this.nL          = 0;
    this.cks         = 0;
    this.curByte     = 0;
    this.nRemainBits = 0;
    this.frameLen    = 0;
    if( !isKCTapHeader() ) {
      throw new IOException(
			"Unbekanntes Dateiformat: KC-TAP-Header erwartet" );
    }
    this.audioFmt = new AudioFormat( 8000F, 8, 1, false, false );

    // Stream-Laenge ermitteln
    int b = read();
    while( b >= 0 ) {
      this.frameLen++;
      b = read();
    }
    this.pos         = offs;
    this.stage       = 0;
    this.posInBlk    = 0;
    this.nPre        = 0;
    this.nH          = 0;
    this.nL          = 0;
    this.cks         = 0;
    this.curByte     = 0;
    this.nRemainBits = 0;
  }


  public KCTapAudioInputStream( byte[] buf ) throws IOException
  {
    this( buf, 0, buf.length );
  }


  public AudioFormat getAudioFormat()
  {
    return this.audioFmt;
  }


  public long getFrameLength()
  {
    return this.frameLen;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int available()
  {
    return 1;		// read-Methode blockt nie
  }


  @Override
  public void close()
  {
    // leer
  }


  @Override
  public boolean markSupported()
  {
    return false;
  }


  @Override
  public int read()
  {
    int rv = -1;
    boolean loop = false;
    do {
      if( this.stage == 0 ) {	// neuer Block?
	this.posInBlk    = 0;
	this.nPre        = 0;
	this.nH          = 0;
	this.nL          = 0;
	this.cks         = 0;
	this.curByte     = 0;
	this.nRemainBits = 0;
	if( isKCTapHeader() ) {
	  this.nPre = 8000;
	  this.pos += 16;
	} else {
	  this.nPre = 160;
	}
	if( this.pos < this.bufSize ) {
	  this.stage++;
	} else {
	  this.stage = -1;	// Dateiende
	}
      }
      if( this.stage == 1 ) {	// Vorschwingung
	if( (this.nH == 0) && (this.nL == 0) ) {
	  if( this.nPre > 0 ) {
	    this.nH = 4;	// naechste Vorschwingung
	    this.nL = 4;
	    --this.nPre;
	  } else {
	    this.stage++;
	    this.nH = 8;	// als naechstes Trennschwingung
	    this.nL = 8;
	  }
	}
      }
      if( this.stage == 2 ) {	// Trennschwingung
	if( (this.nH == 0) && (this.nL == 0) ) {
	  this.stage++;
	}
      }
      if( this.stage == 3 ) {	// Blocknummer, Datenbytes und Pruefsumme
	if( (this.nH == 0) && (this.nL == 0) ) {
	  if( this.nRemainBits == 0 ) {
	    if( this.posInBlk < 129 ) {
	      this.curByte = 0;
	      if( this.pos < this.bufSize ) {
		this.curByte = (int) this.buf[ this.pos++ ] & 0xFF;
		if( this.posInBlk > 0 ) {
		  this.cks = (this.cks + this.curByte) & 0xFF;
		}
	      }
	      this.nRemainBits = 9;
	      this.posInBlk++;
	    } else if( this.posInBlk == 129 ) {
	      this.curByte     = this.cks;
	      this.nRemainBits = 9;
	      this.posInBlk++;
	    } else {
	      this.stage = 0;	// Blockende -> neuer Block
	    }
	  }
	  if( this.nRemainBits == 1 ) {
	    this.nH = 8;	// als naechstes Trennschwingung
	    this.nL = 8;
	    --this.nRemainBits;
	  }
	  else if( this.nRemainBits > 1 ) {
	    if( (this.curByte & 0x01) != 0 ) {
	      this.nH = 4;	// 1-Bit
	      this.nL = 4;
	    } else {
	      this.nH = 2;	// 0-Bit
	      this.nL = 2;
	    }
	    this.curByte >>= 1;
	    --this.nRemainBits;
	  }
	}
      }
    } while( this.stage == 0 );
    if( this.nH > 0 ) {
      --this.nH;
      rv = 255;
    } else if( this.nL > 0 ) {
      --this.nL;
      rv = 0;
    }
    return rv;
  }


  @Override
  public int read( byte[] buf )
  {
    return read( buf, 0, buf.length );
  }


  @Override
  public int read( byte[] buf, int offs, int len )
  {
    int rv = 0;
    while( len > 0 ) {
      int b = read();
      if( b >= 0 ) {
	buf[ offs++ ] = (byte) b;
	rv++;
      } else {
	break;
      }
      --len;
    }
    return rv;
  }


  @Override
  public void reset() throws IOException
  {
    throw new IOException(
		"KCTapAudioInputStream.reset() nicht unterst\u00FCtzt" );
  }


  @Override
  public long skip( long n )
  {
    long rv = 0;
    while( n > 0 ) {
      if( read() >= 0 ) {
	rv++;
      } else {
	break;
      }
      --n;
    }
    return rv;
  }


	/* --- private Methoden --- */

  private boolean isKCTapHeader()
  {
    boolean rv = false;
    int     n  = KCTAP_HEADER.length();
    if( (this.pos + n) < this.bufSize ) {
      rv = true;
      for( int i = 0; i < n; i++ ) {
	if( ((int) this.buf[ this.pos + i ] & 0xFF)
				!= ((int) KCTAP_HEADER.charAt( i )) )
	{
	  rv = false;
	  break;
	}
      }
    }
    return rv;
  }
}

