/*
 * (c) 2020-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Scannen eines Eingabestroms und,
 * sofern es sich um ein JPEG-Bild handelt,
 * parsen der darin enthaltenen EXIF-Daten
 */

package jkcemu.image;

import java.io.InputStream;
import java.io.IOException;
import jkcemu.base.EmuUtil;


public class JPEGExifInputStream extends InputStream
{
  private enum ScanStatus {
			CHECK_JPEG_BEG,
			CHECK_JPEG_TAG,
			SKIP_JPEG_TAG,
			NONE };

  private InputStream in;
  private ExifData    exifData;
  private byte[]      exifBuf;
  private int         exifPos;
  private byte[]      scanBuf;
  private int         scanPos;
  private ScanStatus  scanStatus;
  private int         nSkipToTag;


  public JPEGExifInputStream( InputStream in )
  {
    this.in         = in;
    this.exifData   = null;
    this.exifBuf    = null;
    this.exifPos    = 0;
    this.scanBuf    = new byte[ 4 ];
    this.scanPos    = 0;
    this.scanStatus = ScanStatus.CHECK_JPEG_BEG;
    this.nSkipToTag = 0;
  }


  public ExifData getExifData()
  {
    parseExifBuf();
    return this.exifData;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int available() throws IOException
  {
    return this.in.available();
  }


  @Override
  public void close() throws IOException
  {
    this.scanStatus = ScanStatus.NONE;
    this.in.close();
  }


  @Override
  public boolean markSupported()
  {
    return false;
  }


  @Override
  public int read() throws IOException
  {
    int b = this.in.read();
    if( b >= 0 ) {
      switch( this.scanStatus ) {
	case NONE:
	  if( this.exifBuf != null ) {
	    if( this.exifPos < this.exifBuf.length ) {
	      this.exifBuf[ this.exifPos++ ] = (byte) b;
	      if( this.exifPos == this.exifBuf.length ) {
		parseExifBuf();
	      }
	    }
	  }
	  break;
	case SKIP_JPEG_TAG:
	  if( this.nSkipToTag > 0 ) {
	    --this.nSkipToTag;
	    if( this.nSkipToTag == 0 ) {
	      this.scanStatus = ScanStatus.CHECK_JPEG_TAG;
	      this.scanPos    = 0;
	    }
	  } else {
	    this.scanStatus = ScanStatus.NONE;
	  }
	  break;
	case CHECK_JPEG_TAG:
	  if( this.scanPos < 4 ) {
	    this.scanBuf[ this.scanPos++ ] = (byte) b;
	    if( this.scanPos == 4 ) {
	      if( this.scanBuf[ 0 ] == (byte) 0xFF ) {
		int tagLen = EmuUtil.getInt2BE( this.scanBuf, 2 ) - 2;
		if( this.scanBuf[ 1 ] == (byte) 0xE1 ) {
		  if( tagLen > 0 ) {
		    this.exifBuf = new byte[ tagLen ];
		    this.exifPos = 0;
		  }
		  this.scanStatus = ScanStatus.NONE;
		} else if( (this.scanBuf[ 1 ] & 0xF0) == 0xE0 ) {
		  if( tagLen > 0 ) {
		    this.nSkipToTag = tagLen;
		    this.scanStatus = ScanStatus.SKIP_JPEG_TAG;
		  }
		} else {
		  this.scanStatus = ScanStatus.NONE;
		}
	      } else {
		this.scanStatus = ScanStatus.NONE;
	      }
	    }
	  }
	  break;
	case CHECK_JPEG_BEG:
	  if( this.scanPos < 2 ) {
	    this.scanBuf[ this.scanPos++ ] = (byte) b;
	    if( this.scanPos == 2 ) {
	      if( (this.scanBuf[ 0 ] == (byte) 0xFF)
		  && (this.scanBuf[ 1 ] == (byte) 0xD8) )
	      {
		this.scanStatus = ScanStatus.CHECK_JPEG_TAG;
		this.scanPos    = 0;
	      } else {
		this.scanStatus = ScanStatus.NONE;
	      }
	    }
	  }
	  break;
      }
    }
    return b;
  }


  @Override
  public int read( byte[] buf ) throws IOException
  {
    return read( buf, 0, buf.length );
  }


  @Override
  public int read( byte[] buf, int offs, int len ) throws IOException
  {
    int rv = 0;
    int b  = 0;
    while( (len > 0)
	   && ((this.scanStatus != ScanStatus.NONE)
	       || (this.exifBuf != null)) )
    {
      b = read();
      if( b < 0 ) {
	break;
      }
      buf[ offs++ ] = (byte) b;
      --len;
      rv++;
    }
    if( (b >= 0) && (len > 0) ) {
      int n = this.in.read( buf, offs, len );
      if( n >= 0 ) {
	rv += n;
      }
    }
    return rv > 0 ? rv : -1;
  }


  @Override
  public long skip( long n ) throws IOException
  {
    long rv = 0;
    int  b  = 0;
    while( (n > 0)
	   && ((this.scanStatus != ScanStatus.NONE)
	       || (this.exifBuf != null)) )
    {
      b = read();
      if( b < 0 ) {
	break;
      }
      --n;
      rv++;
    }
    if( (b >= 0) && (n > 0) ) {
      rv += this.in.skip( n );
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void parseExifBuf()
  {
    if( (this.exifBuf != null) && (this.exifPos > 0) ) {
      this.exifData = ExifParser.parseExif( this.exifBuf, 0, this.exifPos );
      this.exifBuf  = null;
    }
  }
}
