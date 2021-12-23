/*
 * (c) 2015-2019 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * DateStamper-Unterstuetzung
 */

package jkcemu.disk;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import jkcemu.file.FileTimesData;


public class DateStamper
{
  public static final String ENTRYNAME = "!!!TIME&DAT";
  public static final String FILENAME  = "!!!TIME&.DAT";

  private static Calendar calendar = Calendar.getInstance();

  private byte[] dateTimeBytes;
  private int    dateTimeLen;
  private int    pos;


  public DateStamper( int dirEntries, byte[] dateTimeBytes )
  {
    this.dateTimeLen = dirEntries * 16;
    if( dateTimeBytes != null ) {
      this.dateTimeBytes = dateTimeBytes;
    } else {
      this.dateTimeBytes = new byte[ dirEntries * 16 ];
    }
    if( this.dateTimeLen < this.dateTimeBytes.length ) {
      Arrays.fill( this.dateTimeBytes, 0, this.dateTimeLen, (byte) 0x00 );
    } else {
      Arrays.fill( this.dateTimeBytes, (byte) 0x00 );
      this.dateTimeLen = this.dateTimeBytes.length;
    }
    String text    = "!!!TIME\u0092";
    int    textLen = text.length();
    int    srcPos  = 0;
    int    dstPos  = 0x0F;
    while( dstPos < this.dateTimeLen ) {
      if( srcPos >= textLen ) {
	srcPos = 0;
      }
      this.dateTimeBytes[ dstPos ] = (byte) text.charAt( srcPos++ );
      dstPos += 0x10;
    }
    this.pos = 0;
  }


  public void addFileTimes( File file )
  {
    FileTimesData ftd = FileTimesData.createOf( file );
    writeDateTimeEntry( ftd.getCreationMillis() );
    writeDateTimeEntry( ftd.getLastAccessMillis() );
    writeDateTimeEntry( ftd.getLastModifiedMillis() );
    this.pos++;
  }


  public byte[] getDateTimeByteBuffer()
  {
    // DateStamper Pruefsummen berechnen
    int pos = 0;
    while( pos < this.dateTimeLen ) {
      int cks = 0;
      for( int i = 0; (i < 0x7F) && (pos < this.dateTimeLen); i++ ) {
	cks += ((int) this.dateTimeBytes[ pos++ ] & 0xFF);
      }
      if( pos < this.dateTimeLen ) {
	this.dateTimeBytes[ pos++ ] = (byte) cks;
      }
    }
    return this.dateTimeBytes;
  }


  public static Long getMillis( byte[] buf, int pos )
  {
    Long millis = null;
    if( buf != null ) {
      if( (pos >= 0) && ((pos + 4) < buf.length) ) {
	int year   = fromBcdByte( buf[ pos ] );
	int month  = fromBcdByte( buf[ pos + 1 ] );
	int day    = fromBcdByte( buf[ pos + 2 ] );
	int hour   = fromBcdByte( buf[ pos + 3 ] );
	int minute = fromBcdByte( buf[ pos + 4 ] );
	if( year >= 78 ) {
	  year += 1900;
	} else {
	  year += 2000;
	}
	if( (month >= 1) && (month <= 12)
	    && (day >= 1) && (day <= 31)
	    && (hour < 24) && (minute < 60) )
	{
	  boolean valid = true;
	  if( month == 2 ) {
	    if( ((year % 4) == 0) && ((year % 100) != 0) ) {
	      if( day > 29 ) {
		valid = false;
	      }
	    } else {
	      if( day > 28 ) {
		valid = false;
	      }
	    }
	  }
	  else if( (month == 4) || (month == 6)
		   || (month == 9) || (month == 11) )
	  {
	    if( day > 30 ) {
	      valid = false;
	    }
	  }
	  if( valid ) {
	    synchronized( calendar ) {
	      calendar.clear();
	      calendar.set( year, month - 1, day, hour, minute );
	      millis = calendar.getTimeInMillis();
	    }
	  }
	}
      }
    }
    return millis;
  }


	/* --- private Methoden --- */

  private static int fromBcdByte( byte b )
  {
    return ((((int) b >> 4) & 0x0F) * 10) + ((int) b & 0x0F);
  }


  private static byte toBcdByte( int value )
  {
    return (byte) (((((value / 10) % 10) << 4) & 0xF0)
				| ((value % 10) & 0x0F));
  }


  private void writeDateTimeEntry( Long millis )
  {
    int endPos = Math.min( this.dateTimeLen, this.dateTimeBytes.length );
    if( (this.pos + 4) < endPos ) {
      boolean done = false;
      if( millis != null ) {
	synchronized( calendar ) {
	  calendar.clear();
	  calendar.setTimeInMillis( millis.longValue() );
	  int year = calendar.get( Calendar.YEAR );
	  if( (year >= 1978) && (year < 2078) ) {
	    this.dateTimeBytes[ this.pos++ ] = toBcdByte( year );
	    this.dateTimeBytes[ this.pos++ ] =
			toBcdByte( calendar.get( Calendar.MONTH ) + 1 );
	    this.dateTimeBytes[ this.pos++ ] =
			toBcdByte( calendar.get( Calendar.DAY_OF_MONTH ) );
	    this.dateTimeBytes[ this.pos++ ] =
			toBcdByte( calendar.get( Calendar.HOUR_OF_DAY ) );
	    this.dateTimeBytes[ this.pos++ ] =
			toBcdByte( calendar.get( Calendar.MINUTE ) );
	    done = true;
	  }
	}
      }
      if( !done ) {
	for( int i = 0; i < 5; i++ ) {
	  this.dateTimeBytes[ this.pos++ ] = (byte) 0;
	}
      }
    }
  }
}
