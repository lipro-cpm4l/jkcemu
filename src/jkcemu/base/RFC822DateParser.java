/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Toleranter Parser fuer Datumswerte nach RFC-822
 */

package jkcemu.base;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Calendar;


public class RFC822DateParser
{
  private static String[] monthAbbrs = {
				"JAN", "FEB", "MAR", "APR",
				"MAY", "JUN", "JUL", "AUG",
				"SEP", "OCT", "NOV", "DEC" };

  public static java.util.Date parse(
				String        text,
				StringBuilder tzOut )
  {
    java.util.Date date = null;
    if( text != null ) {
      Integer           year  = null;
      Integer           month = null;
      Integer           day   = null;
      CharacterIterator iter  = new StringCharacterIterator( text );

      /*
       * Wochentag und/oder Monat am Anfang?
       * max. zwei Durchlaeufe
       */
      for( int i = 0; i < 2; i++ ) {
	String s = readLetters( iter );
	if( s == null ) {
	  break;
	}
	month = getMonthByName( s );
	if( month != null ) {
	  day  = readInteger( iter );
	  year = readInteger( iter );
	  break;
	}
      }
      if( (year == null) && (month == null) && (day == null) ) {
	day   = readInteger( iter );
	month = readInteger( iter );
	if( month == null ) {
	  month = getMonthByName( readLetters( iter ) );
	}
	year = readInteger( iter );
      }
      if( (day != null) && (year != null) ) {
	if( (day.intValue() > 31) && (year.intValue() <= 31) ) {
	  Integer m = day;
	  day       = year;
	  year      = m;
	}
      }

      // Datum gueltig?
      if( (year != null) && (month != null) && (day != null) ) {
	int y = year.intValue();
	int m = month.intValue();
	int d = day.intValue();
	if( (y >= 1582) && (y <= 9999)
	    && (m >= 1) && (m <= 12)
	    && (d >= 1) && (d <= 31) )
	{
	  boolean valid = true;
	  switch( m ) {
	    case 2:
	      if( (d == 29)
		  && (((y % 4) != 0)
		      || (((y % 100) == 0) && ((y % 400) != 0))) )
	      {
		valid = false;
	      } else if( d > 29 ) {
		valid = false;
	      }
	      break;
	    case 4:
	    case 6:
	    case 9:
	    case 11:
	      if( d > 31 ) {
		valid = false;
	      }
	      break;
	  }
	  if( valid ) {
	    Calendar cal = Calendar.getInstance();
	    cal.clear();
	    cal.set( Calendar.YEAR, y );
	    cal.set( Calendar.MONTH, m - 1 );
	    cal.set( Calendar.DAY_OF_MONTH, d );

	    // Uhrzeit lesen
	    Integer ss = null;
	    Integer mm = null;
	    Integer hh = readInteger( iter );
	    if( skipSpaces( iter ) == ':' ) {
	      mm = readInteger( iter );
	      if( skipSpaces( iter ) == ':' ) {
		ss = readInteger( iter );
	      }
	    }
	    if( (hh != null) && (mm != null) ) {
	      if( (hh.intValue() >= 0) && (hh.intValue() <= 59)
		  && (mm.intValue() >= 0) && (mm.intValue() <= 59) )
	      {
		/*
		 * AM/PM und Zeitzone lesen
		 *
		 * Die Zeitzone wird nicht im Calendar-Objekt gesetzt,
		 * da die Zeit nicht auf die lokale Zeitzone umgerechnet
		 * werden soll, sondern die Werte so bleiben sollen.
		 */
		boolean hourDone = false;
		if( !readNumericalTimezoneTo( tzOut, iter ) ) {

		  // AM/PM oder textuelle Zeitzone
		  String s = readLetters( iter );
		  if( s != null ) {
		    boolean amFlag = s.equals( "AM" );
		    if( amFlag || s.equals( "PM" ) ) {
		      cal.set( Calendar.HOUR, hh );
		      cal.set( Calendar.AM_PM,
			       amFlag ? Calendar.AM : Calendar.PM );
		      hourDone = true;

		      // nach AM/PM kann Zeitzone folgen
		      if( tzOut != null ) {
			if( !readNumericalTimezoneTo( tzOut, iter ) ) {
			  s = readLetters( iter );
			  if( s != null ) {
			    tzOut.append( s );
			  }
			}
		      }
		    }
		  }
		}

		// Uhrzeit setzen
		if( !hourDone ) {
		  cal.set( Calendar.HOUR_OF_DAY, hh.intValue() );
		}
		cal.set( Calendar.MINUTE, mm.intValue() );
		if( ss != null ) {
		  // 60: Leap-Sekunde
		  if( (ss.intValue() >= 0) && (ss.intValue() <= 60) ) {
		    cal.set( Calendar.SECOND, ss.intValue() );
		  }
		}
	      }
	    }

	    // Rueckgabewert
	    date = cal.getTime();
	  }
	}
      }
    }
    return date;
  }


	/* --- private Methoden --- */

  public static Integer getMonthByName( String text )
  {
    Integer rv = null;
    if( text != null ) {
      for( int i = 0; i < monthAbbrs.length; i++ ) {
	if( text.startsWith( monthAbbrs[ i ] ) ) {
	  rv = Integer.valueOf( i + 1 );
	  break;
	}
      }
    }
    return rv;
  }


  public static Integer readInteger( CharacterIterator iter )
  {
    skipDelimiters( iter );
    Integer rv = null;
    char ch = iter.current();
    if( (ch >= '0') && (ch <= '9') ) {
      int v = ch - '0';
      ch = iter.next();
      while( (ch >= '0') && (ch <= '9') ) {
	v  = (v * 10) + (ch - '0');
	ch = iter.next();
      }
      rv = Integer.valueOf( v );
    }
    return rv;
  }


  public static String readLetters( CharacterIterator iter )
  {
    StringBuilder buf = new StringBuilder();
    readLettersAndAppendTo( buf, iter );
    return buf.toString();
  }


  public static void readLettersAndAppendTo(
				StringBuilder     buf,
				CharacterIterator iter )
  {
    skipDelimiters( iter );
    char ch = iter.current();
    while( ((ch >= 'A') && (ch <= 'Z'))
	   || ((ch >= 'a') && (ch <= 'z')) )
    {
      buf.append( Character.toUpperCase( ch ) );
      ch = iter.next();
    }
  }


  public static boolean readNumericalTimezoneTo(
				StringBuilder     tzOut,
				CharacterIterator iter )
  {
    boolean rv = false;
    char    ch = skipSpaces( iter );
    if( (ch == '+') || (ch == '-') || ((ch >= '0') && (ch <= '9')) ) {
      int oldLen = 0;
      if( tzOut != null ) {
	oldLen = tzOut.length();
	tzOut.append( ch );
      }
      ch = iter.next();
      while( (ch >= '0') && (ch <= '9') ) {
	tzOut.append( Character.toUpperCase( ch ) );
	ch = iter.next();
      }
      if( tzOut != null ) {
	ch = tzOut.charAt( tzOut.length() - 1 );
	if( (ch < '0') || (ch > '9') ) {
	  tzOut.setLength( oldLen );
	}
      }

      /*
       * Auch wenn die Zeitzone ungueltig sein sollte,
       * wurde eine gelesen.
       */
      rv = true;
    }
    return rv;
  }


  public static char skipDelimiters( CharacterIterator iter )
  {
    char ch = iter.current();
    while( (ch != CharacterIterator.DONE)
	   && ((ch < '0') || (ch > '9'))
	   && ((ch < 'A') || (ch > 'Z'))
	   && ((ch < 'a') || (ch > 'z')) )
    {
      ch = iter.next();
    }
    return ch;
  }


  public static char skipSpaces( CharacterIterator iter )
  {
    char ch = iter.current();
    while( (ch == '\u0020') || (ch == '\t') ) {
      ch = iter.next();
    }
    return ch;
  }


	/* --- Konstruktor --- */

  private RFC822DateParser()
  {
    // nicht instanziierbar
  }
}
