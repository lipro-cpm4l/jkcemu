/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * EXIF-Datenelement
 */

package jkcemu.image;

import java.io.UnsupportedEncodingException;
import jkcemu.base.EmuUtil;


public class ExifEntry implements Comparable<ExifEntry>
{
  private int     entryType;
  private int     dataType;
  private int     dataCnt;
  private byte[]  dataBuf;
  private int     dataPos;
  private int     dataLen;
  private boolean bigEndian;


  public ExifEntry(
		int     entryType,
		int     dataType,
		int     dataCnt,
		byte[]  dataBuf,
		int     dataPos,
		int     dataLen,
		boolean bigEndian )
  {
    this.entryType = entryType;
    this.dataType  = dataType;
    this.dataCnt   = dataCnt;
    this.dataBuf   = dataBuf;
    this.dataPos   = dataPos;
    this.dataLen   = dataLen;
    this.bigEndian = bigEndian;
  }


  public byte[] getByteArrayValue()
  {
    byte[] rv = null;
    if( (this.dataType == ExifParser.DATA_TYPE_UBYTE)
	|| (this.dataType == ExifParser.DATA_TYPE_BYTE_ARRAY) )
    {
      rv = new byte[ Math.min(
			this.dataCnt,
			this.dataBuf.length - this.dataPos ) ];
      System.arraycopy( this.dataBuf, this.dataPos, rv, 0, rv.length );
    }
    return rv;
  }


  public int getInt4Value() throws IllegalStateException
  {
    if( (this.dataType != ExifParser.DATA_TYPE_UINT4)
	&& (this.dataType != ExifParser.DATA_TYPE_SINT4) )
    {
      throw new IllegalStateException();
    }
    return (int) (this.bigEndian ?
		EmuUtil.getInt4BE( this.dataBuf, this.dataPos )
		: EmuUtil.getInt4LE( this.dataBuf, this.dataPos ));
  }


  public Number getNumberValue()
  {
    Number rv = null;
    switch( this.dataType ) {
      case ExifParser.DATA_TYPE_UBYTE:
	if( this.dataLen == 1 ) {
	  rv = Integer.valueOf( (int) this.dataBuf[ this.dataPos ] & 0xFF );
	}
	break;
      case ExifParser.DATA_TYPE_UINT2:
	if( this.dataLen == 2 ) {
	  rv = Integer.valueOf( getInt2( 0 ) );
	}
	break;
      case ExifParser.DATA_TYPE_UINT4:
	if( this.dataLen == 4 ) {
	  rv = Long.valueOf( getInt4( 0 ) );
	}
	break;
      case ExifParser.DATA_TYPE_URATIONAL:
	if( this.dataLen == 8 ) {
	  long numerator   = getInt4( 0 );
	  long denominator = getInt4( 4 );
	  if( denominator == 1 ) {
	    rv = Long.valueOf( numerator );
	  } else {
	    rv = Double.valueOf( (double) numerator / (double) denominator );
	  }
	}
	break;
      case ExifParser.DATA_TYPE_SBYTE:
	if( this.dataLen == 1 ) {
	  rv = Integer.valueOf( this.dataBuf[ this.dataPos ] );
	}
	break;
      case ExifParser.DATA_TYPE_SINT2:
	if( this.dataLen == 2 ) {
	  rv = Integer.valueOf( (short) getInt2( 0 ) );
	}
	break;
      case ExifParser.DATA_TYPE_SINT4:
	if( this.dataLen == 4 ) {
	  rv = Integer.valueOf( (int) getInt4( 0 ) );
	}
	break;
      case ExifParser.DATA_TYPE_SRATIONAL:
	if( this.dataLen == 8 ) {
	  int numerator   = (int) getInt4( 0 );
	  int denominator = (int) getInt4( 4 );
	  if( denominator == 1 ) {
	    rv = Integer.valueOf( numerator );
	  } else {
	    rv = Double.valueOf( (double) numerator / (double) denominator );
	  }
	}
	break;
      case ExifParser.DATA_TYPE_FLOAT4:
	if( this.dataLen == 4 ) {
	  rv = Float.valueOf( Float.intBitsToFloat( (int) getInt4( 0 ) ) );
	}
	break;
      case ExifParser.DATA_TYPE_FLOAT8:
	if( this.dataLen == 8 ) {
	  long v = 0;
	  if( this.bigEndian ) {
	    for( int i = 0; i < 4; i++ ) {
	      v = ((v << 8) & 0xFFFFFFFFFFFFFF00L)
				| (this.dataBuf[ this.dataPos + i ] & 0xFFL);
	    }
	  } else {
	     for( int i = 3; i >= 0; --i ) {
	      v = ((v << 8) & 0xFFFFFFFFFFFFFF00L)
				| (this.dataBuf[ this.dataPos + i ] & 0xFFL);
	    }
	  }
	  rv = Double.valueOf( Double.longBitsToDouble( v ) );
	}
	break;
    }
    return rv;
  }


  public long[] getRational( int idx )
  {
    long[] rv   = null;
    int    offs = idx * 8;
    if( (idx >= 0) && (idx < this.dataCnt)
	&& (this.dataLen >= (offs + 8))
	&& (this.dataBuf != null) )
    {
      if( (this.dataPos + offs) <= this.dataBuf.length ) {
	switch( this.dataType ) {
	  case ExifParser.DATA_TYPE_URATIONAL:
	    rv = new long[] {
			getInt4( offs ) & 0x00000000FFFFFFFF,
			getInt4( offs + 4 ) & 0x00000000FFFFFFFF };
	    break;
	  case ExifParser.DATA_TYPE_SRATIONAL:
	    rv = new long[] {
			(long) (int) getInt4( offs ),
			(long) (int) getInt4( offs + 4 ) };
	    break;
	}
      }
    }
    return rv;
  }


  public String getStringValue()
  {
    String rv = null;
    if( (this.dataType == ExifParser.DATA_TYPE_ASCII)
	&& (this.dataLen > 0) )
    {
      int pos = this.dataPos;
      for( int i = 0; i < this.dataLen; i++ ) {
	if( this.dataBuf[ pos ] == 0 ) {
	  break;
	}
	pos++;
      }
      if( pos > this.dataPos ) {
	/*
	 * Obwohl der Datentyp ASCII heisst,
	 * werden auch 8-Bit-Zeichen akzeptiert,
	 * und zwar als Latin 1.
	 */
	try {
	  rv = new String(
			this.dataBuf,
			this.dataPos,
			pos - this.dataPos,
			"ISO-8859-1" );
	}
	catch( UnsupportedEncodingException ex ) {}
      }
    }
    return rv;
  }


  public void setInt4Value( int value ) throws IllegalStateException
  {
    if( (this.dataType != ExifParser.DATA_TYPE_UINT4)
	&& (this.dataType != ExifParser.DATA_TYPE_SINT4) )
    {
      throw new IllegalStateException();
    }
    byte[] dataBuf = new byte[ 4 ];
    if( this.bigEndian ) {
      writeInt4beTo( dataBuf, 0, value );
    } else {
      writeInt4leTo( dataBuf, 0, value );
    }
    this.dataCnt = 1;
    this.dataBuf = dataBuf;
    this.dataPos = 0;
    this.dataLen = dataBuf.length;
  }


  public void writeDirEntryTo(
			byte[] outBuf,
			int    outOffs,
			int    dirPos,
			int    extDataPos )
  {
    int pos = outOffs + dirPos;
    if( this.bigEndian ) {
      pos = writeInt2beTo( outBuf, pos, this.entryType );
      pos = writeInt2beTo( outBuf, pos, this.dataType );
      pos = writeInt4beTo( outBuf, pos, this.dataCnt );
      if( (this.dataType < 0) || (this.dataLen > 4) ) {
	pos = writeInt4beTo( outBuf, pos, extDataPos );
      }
    } else {
      pos = writeInt2leTo( outBuf, pos, this.entryType );
      pos = writeInt2leTo( outBuf, pos, this.dataType );
      pos = writeInt4leTo( outBuf, pos, this.dataCnt );
      if( (this.dataType < 0) || (this.dataLen > 4) ) {
	pos = writeInt4leTo( outBuf, pos, extDataPos );
      }
    }
    if( (this.dataType >= 0) && (this.dataLen <= 4) ) {
      int n = writeDataTo( outBuf, pos );
      pos += n;
      while( n < 4 ) {
	outBuf[ pos++ ] = 0x00;
	n++;
      }
    }
  }


  public int writeExtDataTo(
			byte[] outBuf,
			int    outOffs,
			int    dataPos )
  {
    int rv = 0;
    if( this.dataLen > 4 ) {
      rv = writeDataTo( outBuf, outOffs + dataPos );
      if( (rv & 0x0001) != 0 ) {
	outBuf[ outOffs + dataPos + rv ] = 0;
	rv++;
      }
    }
    return rv;
  }


	/* --- Comparable --- */

  @Override
  public int compareTo( ExifEntry entry )
  {
    return this.entryType - entry.entryType;
  }


	/* --- private Methoden --- */

  private int getInt2( int pos )
  {
    return this.bigEndian ?
		EmuUtil.getInt2BE( this.dataBuf, this.dataPos + pos )
		: EmuUtil.getInt2LE( this.dataBuf, dataPos + pos );
  }


  private long getInt4( int pos )
  {
    return this.bigEndian ?
		EmuUtil.getInt4BE( this.dataBuf, this.dataPos + pos )
		: EmuUtil.getInt4LE( this.dataBuf, dataPos + pos );
  }


  private int writeDataTo( byte[] outBuf, int outPos )
  {
    int rv  = 0;
    int len = this.dataLen;
    int pos = this.dataPos;
    while( len > 0 ) {
      outBuf[ outPos++ ] = this.dataBuf[ pos++ ];
      --len;
      rv++;
    }
    return rv;
  }


  private static int writeInt2beTo(
				byte[] outBuf,
				int    outPos,
				int    value )
  {
    outBuf[ outPos++ ] = (byte) (value >> 8);
    outBuf[ outPos++ ] = (byte) value;
    return outPos;
  }


  private static int writeInt4beTo(
				byte[] outBuf,
				int    outPos,
				int    value )
  {
    outPos = writeInt2beTo( outBuf, outPos, value >> 16 );
    outPos = writeInt2beTo( outBuf, outPos, value );
    return outPos;
  }


  private static int writeInt2leTo(
				byte[] outBuf,
				int    outPos,
				int    value )
  {
    outBuf[ outPos++ ] = (byte) value;
    outBuf[ outPos++ ] = (byte) (value >> 8);
    return outPos;
  }


  private static int writeInt4leTo(
				byte[] outBuf,
				int    outPos,
				int    value )
  {
    outPos = writeInt2leTo( outBuf, outPos, value );
    outPos = writeInt2leTo( outBuf, outPos, value >> 16 );
    return outPos;
  }
}
