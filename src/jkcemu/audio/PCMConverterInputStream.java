/*
 * (c) 2016-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Konverter von PCMDataSource zu InputStream
 *
 * Im Konstruktor wird angegeben, ob die Audiodaten
 * vorzeichenlos oder vorzeichenbehaftet sowie
 * Little-Endian oder Big-Endian ausgegeben werden sollen.
 * Eine evtl. notwendige Umwandlung erfolgt automatisch.
 *
 * Des Weiteren werden 1-Bit-Audiodaten auf als 8-Bit-Wert erweitert.
 */

package jkcemu.audio;

import java.io.IOException;
import java.io.InputStream;


public class PCMConverterInputStream extends InputStream
{
  private PCMDataSource source;
  private boolean       toSigned;
  private boolean       toBigEndian;
  private boolean       ignoreClose;
  private byte[]        inBuf;
  private int           outBuf;
  private int           bytesInOutBuf;
  private int           valueOffs;


  public PCMConverterInputStream(
			PCMDataSource source,
			boolean       toSigned,
			boolean       toBigEndian )
  {
    this( source, toSigned, toBigEndian, false );
  }


  public PCMConverterInputStream(
			PCMDataSource source,
			boolean       toSigned,
			boolean       toBigEndian,
			boolean       ignoreClose )
  {
    this.source        = source;
    this.toSigned      = toSigned;
    this.toBigEndian   = toBigEndian;
    this.ignoreClose   = ignoreClose;
    this.inBuf         = new byte[ (source.getSampleSizeInBits() + 7) / 8 ];
    this.outBuf        = 0;
    this.bytesInOutBuf = 0;
    this.valueOffs     = 0;
    if( toSigned != source.isSigned() ) {
      switch( this.inBuf.length ) {
	case 1:
	  this.valueOffs = 0x80;
	  break;
	case 2:
	  this.valueOffs = 0x8000;
	  break;
	case 3:
	  this.valueOffs = 0x800000;
	  break;
	case 4:
	  this.valueOffs = 0x80000000;
	  break;
      }
      if( toSigned ) {
	this.valueOffs = -this.valueOffs;
      }
    }
  }


  public int getSampleSizeInBits()
  {
    return Math.max( 8, this.source.getSampleSizeInBits() );
  }


  public boolean isBigEndian()
  {
    return this.toBigEndian;
  }


  public boolean isSigned()
  {
    return this.toSigned;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void close() throws IOException
  {
    if( !ignoreClose )
      this.source.close();
  }


  @Override
  public synchronized int read() throws IOException
  {
    int rv = -1;
    if( this.bytesInOutBuf <= 0 ) {
      if( this.source.read(
			this.inBuf,
			0,
			this.inBuf.length ) == this.inBuf.length )
      {
	int value = 0;
	if( this.source.getSampleSizeInBits() == 1 ) {
	  if( this.inBuf[ 0 ] == 0 ) {
	    value = (this.toSigned ? AudioOut.SIGNED_VALUE_0 : 0);
	  } else {
	    value = (this.toSigned ?
				AudioOut.SIGNED_VALUE_1
				: AudioOut.MAX_USED_UNSIGNED_VALUE);
	  }
	} else {
	  if( this.source.isBigEndian() ) {
	    for( int i = 0; i < this.inBuf.length; i++ ) {
	      value <<= 8;
	      value |= ((int) this.inBuf[ i ]) & 0xFF;
	    }
	  } else {
	    int pos = this.inBuf.length - 1;
	    while( pos >= 0 ) {
	      value <<= 8;
	      value |= ((int) this.inBuf[ pos ]) & 0xFF;
	      --pos;
	    }
	  }
	  value += this.valueOffs;
	  if( this.toBigEndian ) {
	    switch( this.inBuf.length ) {
	      case 2:
		value = ((value << 8) & 0x0000FF00)
			| ((value >> 8) & 0x000000FF);
		break;
	      case 3:
		value = ((value << 16) & 0x00FF0000)
			| (value & 0x0000FF00)
			| ((value >> 16) & 0x000000FF);
		break;
	      case 4:
		value = ((value << 24) & 0xFF000000)
			| ((value << 8) & 0x00FF0000)
			| ((value >> 8) & 0x0000FF00)
			| ((value >> 24) & 0x000000FF);
		break;
	    }
	  }
	}
	this.outBuf        = value;
	this.bytesInOutBuf = this.inBuf.length;
      }
    }
    if( this.bytesInOutBuf > 0 ) {
      rv = this.outBuf & 0xFF;
      this.outBuf >>= 8;
      --this.bytesInOutBuf;
    }
    return rv;
  }
}
