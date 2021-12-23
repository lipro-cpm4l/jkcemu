/*
 * (c) 2020-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Scannen eines Ausgabestroms und,
 * sofern es sich um ein JPEG-Bild handelt,
 * einfuegen bzw. ersetzen der EXIF-Daten
 */

package jkcemu.image;

import java.io.OutputStream;
import java.io.IOException;
import jkcemu.base.EmuUtil;


public class JPEGExifOutputStream extends OutputStream
{
  private enum ScanStatus {
			CHECK_JPEG_BEG,
			CHECK_JPEG_TAG,
			PASS_JPEG_TAG,
			SKIP_JPEG_TAG,
			PASS_THRU };

  private OutputStream out;
  private ExifData     exifData;
  private byte[]       buf;
  private int          len;
  private int          pos;
  private long         remainTagLen;
  private ScanStatus   scanStatus;


  public JPEGExifOutputStream( OutputStream out, ExifData exifData )
  {
    this.out          = out;
    this.exifData     = exifData;
    this.buf          = new byte[ 4 ];
    this.len          = 0;
    this.remainTagLen = 0;
    this.scanStatus   = (exifData != null ?
				ScanStatus.CHECK_JPEG_BEG
				: ScanStatus.PASS_THRU);
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void close() throws IOException
  {
    this.len        = 0;
    this.scanStatus = ScanStatus.PASS_THRU;
    this.out.close();
  }


  @Override
  public void flush() throws IOException
  {
    this.out.flush();
  }


  @Override
  public void write( int b ) throws IOException
  {
    switch( this.scanStatus ) {
      case CHECK_JPEG_BEG:
	this.buf[ this.len++ ] = (byte) b;
	if( this.len == 2 ) {
	  if( (this.buf[ 0 ] == (byte) 0xFF)
	      && (this.buf[ 1 ] == (byte) 0xD8) )
	  {
	    this.scanStatus = ScanStatus.CHECK_JPEG_TAG;
	  } else {
	    this.scanStatus = ScanStatus.PASS_THRU;
	  }
	  this.out.write( this.buf, 0, this.len );
	  this.len = 0;
	}
	break;
      case CHECK_JPEG_TAG:
	this.buf[ this.len++ ] = (byte) b;
	if( this.len == 4 ) {
	  if( this.buf[ 0 ] == (byte) 0xFF ) {
	    int tagLen = EmuUtil.getInt2BE( this.buf, 2 ) - 2;
	    if( tagLen >= 2 ) {
	      if( this.buf[ 1 ] == (byte) 0xE0 ) {
		this.remainTagLen = tagLen - 1;
		if( this.remainTagLen > 0 ) {
		  this.scanStatus = ScanStatus.PASS_JPEG_TAG;
		}
	      } else if( this.buf[ 1 ] == (byte) 0xE1 ) {
		this.len          = 0;		// Pufferinhalt verwerfen
		this.remainTagLen = tagLen - 1;
		if( this.remainTagLen > 0 ) {
		  this.scanStatus = ScanStatus.SKIP_JPEG_TAG;
		}
	      } else {
		writeExif();
		this.scanStatus = ScanStatus.PASS_THRU;
	      }
	    } else {
	      this.scanStatus = ScanStatus.PASS_THRU;
	    }
	  } else {
	    this.scanStatus = ScanStatus.PASS_THRU;
	  }
	  if( this.len > 0 ) {
	    this.out.write( this.buf, 0, this.len );
	    this.len = 0;
	  }
	}
	break;
      case PASS_JPEG_TAG:
	this.out.write( b );
	if( this.remainTagLen > 0 ) {
	  --this.remainTagLen;
	} else {
	  this.scanStatus = ScanStatus.CHECK_JPEG_TAG;
	}
	break;
      case SKIP_JPEG_TAG:
	if( this.remainTagLen > 0 ) {
	  --this.remainTagLen;
	} else {
	  this.scanStatus = ScanStatus.CHECK_JPEG_TAG;
	}
	break;
      default:
	this.out.write( b );
    }
  }


  @Override
  public void write( byte[] buf ) throws IOException
  {
    write( buf, 0, buf.length );
  }


  @Override
  public void write( byte[] buf, int pos, int len ) throws IOException
  {
    while( (len > 0) && (this.scanStatus != ScanStatus.PASS_THRU) ) {
      write( buf[ pos++ ] );
      --len;
    }
    if( len > 0 ) {
      this.out.write( buf, pos, len );
    }
  }


	/* --- private Methoden --- */

  private void writeExif() throws IOException
  {
    if( this.exifData != null ) {
      byte[] exifBuf = new byte[ 0x10000 ];
      exifBuf[ 0 ]   = (byte) 0xFF;
      exifBuf[ 1 ]   = (byte) 0xE1;
      exifBuf[ 4 ]   = 'E';
      exifBuf[ 5 ]   = 'x';
      exifBuf[ 6 ]   = 'i';
      exifBuf[ 7 ]   = 'f';
      exifBuf[ 8 ]   = 0x00;
      exifBuf[ 9 ]   = 0x00;
      int endPos     = this.exifData.writeTiffTagTo( exifBuf, 10 );
      if( (endPos > 10) && (endPos < 0x10000) ) {
	int len = endPos - 2;
	exifBuf[ 2 ] = (byte) (len >> 8);
	exifBuf[ 3 ] = (byte) len;
	this.out.write( exifBuf, 0, endPos );
      }
      this.exifData = null;
    }
  }
}
