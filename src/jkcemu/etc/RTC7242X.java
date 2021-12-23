/*
 * (c) 2011-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation der Real Time Clock RTC-72421/72423
 *
 * Die emulierte RTC verwendet die Systemzeit.
 * Aus diesem Grund kann die Zeit in der RTC nicht gesetzt werden.
 */

package jkcemu.etc;

import java.util.Calendar;


public class RTC7242X
{
  private boolean mode12h;


  public RTC7242X()
  {
    this.mode12h = false;
  }


  public int read( int reg )
  {
    int      rv  = 0;
    Calendar cal = Calendar.getInstance();
    switch( reg & 0x0F ) {
      case 0:
	rv = (cal.get( Calendar.SECOND ) % 10) & 0x0F;
	break;
      case 1:
	rv = (cal.get( Calendar.SECOND ) / 10) & 0x07;
	break;
      case 2:
	rv = (cal.get( Calendar.MINUTE ) % 10) & 0x0F;
	break;
      case 3:
	rv = (cal.get( Calendar.MINUTE ) / 10) & 0x07;
	break;
      case 4:
	rv = (cal.get( Calendar.HOUR_OF_DAY ) % 10) & 0x0F;
	break;
      case 5:
	if( this.mode12h ) {
	  rv = (cal.get( Calendar.HOUR ) / 10) & 0x01;
	  if( cal.get( Calendar.PM ) != 0 ) {
	    rv |= 0x04;
	  }
	} else {
	  rv = (cal.get( Calendar.HOUR_OF_DAY ) / 10) & 0x03;
	}
	break;
      case 6:
	rv = (cal.get( Calendar.DAY_OF_MONTH ) % 10) & 0x0F;
	break;
      case 7:
	rv = (cal.get( Calendar.DAY_OF_MONTH ) / 10) & 0x03;
	break;
      case 8:
	rv = ((cal.get( Calendar.MONTH ) + 1) % 10) & 0x0F;
	break;
      case 9:
	rv = ((cal.get( Calendar.MONTH ) + 1) / 10) & 0x01;
	break;
      case 10:
	rv = (cal.get( Calendar.YEAR ) % 10) & 0x0F;
	break;
      case 11:
	rv = ((cal.get( Calendar.YEAR ) / 10) % 10) & 0x0F;
	break;
      case 12:
	switch( cal.get( Calendar.DAY_OF_WEEK ) ) {
	  case Calendar.SUNDAY:    rv = 0; break;
	  case Calendar.MONDAY:    rv = 1; break;
	  case Calendar.TUESDAY:   rv = 2; break;
	  case Calendar.WEDNESDAY: rv = 3; break;
	  case Calendar.THURSDAY:  rv = 4; break;
	  case Calendar.FRIDAY:    rv = 5; break;
	  case Calendar.SATURDAY:  rv = 6; break;
	}
	break;
      case 15:
	rv = (this.mode12h ? 0 : 0x04);
	break;
    }
    return rv;
  }


  public void write( int reg, int value )
  {
    if( (reg & 0x0F) == 0x0F ) {
      this.mode12h = ((value & 0x04) == 0);
    }
  }
}

