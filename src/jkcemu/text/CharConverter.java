/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Umwandler fuer Zeichensaetze
 */

package jkcemu.text;

import java.lang.*;


public class CharConverter
{
  public static enum Encoding {
			ASCII_7BIT,
			ISO646DE,
			CP437,
			CP850,
			LATIN1 };

  public static final char REPLACEMENT_CHAR = '\uFFFD';


  private static final String cp437ToUnicode =
	"\u0000\u263A\u263B\u2665\u2666\u2663\u2660\u2022"
		+ "\u25D8\u25CB\u25D9\u2642\u2640\u266A\u266B\u263C"
		+ "\u25BA\u25C4\u2195\u203C\u00B6\u00A7\u25AC\u21A8"
		+ "\u2191\u2193\u2192\u2190\u221F\u2194\u25B2\u25BC"
		+ "\u0020!\"#$%&\'()*+,-./0123456789:;<=>?"
		+ "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_"
		+ "\u0060abcdefghijklmnopqrstuvwxyz{|}~\u2302"
		+ "\u00C7\u00FC\u00E9\u00E2\u00E4\u00E0\u00E5\u00E7"
		+ "\u00EA\u00EB\u00E8\u00EF\u00EE\u00EC\u00C4\u00C5"
		+ "\u00C9\u00E6\u00C6\u00F4\u00F6\u00F2\u00FB\u00F9"
		+ "\u00FF\u00D6\u00DC\u00A2\u00A3\u00A5\u20A7\u0192"
		+ "\u00E1\u00ED\u00F3\u00FA\u00F1\u00D1\u00AA\u00BA"
		+ "\u00BF\u2310\u00AC\u00BD\u00BC\u00A1\u00AB\u00BB"
		+ "\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556"
		+ "\u2555\u2563\u2551\u2557\u255D\u255C\u255B\u2510"
		+ "\u2514\u2534\u252C\u251C\u2500\u253C\u255E\u255F"
		+ "\u255A\u2554\u2569\u2566\u2560\u2550\u256C\u2567"
		+ "\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256B"
		+ "\u256A\u2518\u250C\u2588\u2584\u258C\u2590\u2580"
		+ "\u03B1\u00DF\u0393\u03C0\u03A3\u03C3\u00B5\u03C4"
		+ "\u03A6\u0398\u03A9\u03B4\u221E\u03C6\u03B5\u2229"
		+ "\u2261\u00B1\u2265\u2264\u2320\u2321\u00F7\u2248"
		+ "\u00B0\u2219\u00B7\u221A\u207F\u00B2\u25A0\u00A0";

  private static final String cp850ToUnicode =
	"\u0000\u263A\u263B\u2665\u2666\u2663\u2660\u2022"
		+ "\u25D8\u25CB\u25D9\u2642\u2640\u266A\u266B\u263C"
		+ "\u25BA\u25C4\u2195\u203C\u00B6\u00A7\u25AC\u21A8"
		+ "\u2191\u2193\u2192\u2190\u221F\u2194\u25B2\u25BC"
		+ "\u0020!\"#$%&\'()*+,-./0123456789:;<=>?"
		+ "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_"
		+ "\u0060abcdefghijklmnopqrstuvwxyz{|}~\u2302"
		+ "\u00C7\u00FC\u00E9\u00E2\u00E4\u00E0\u00E5\u00E7"
		+ "\u00EA\u00EB\u00E8\u00EF\u00EE\u00EC\u00C4\u00C5"
		+ "\u00C9\u00E6\u00C6\u00F4\u00F6\u00F2\u00FB\u00F9"
		+ "\u00FF\u00D6\u00DC\u00F8\u00A3\u00D8\u00D7\u0192"
		+ "\u00E1\u00ED\u00F3\u00FA\u00F1\u00D1\u00AA\u00BA"
		+ "\u00BF\u00AE\u00AC\u00BD\u00BC\u00A1\u00AB\u00BB"
		+ "\u2591\u2592\u2593\u2502\u2524\u00C1\u00C2\u00C0"
		+ "\u00A9\u2563\u2551\u2557\u255D\u00A2\u00A5\u2510"
		+ "\u2514\u2534\u252C\u251C\u2500\u253C\u00E3\u00C3"
		+ "\u255A\u2554\u2569\u2566\u2560\u2550\u256C\u00A4"
		+ "\u00F0\u00D0\u00CA\u00CB\u00C8\u0131\u00CD\u00CE"
		+ "\u00CF\u2518\u250C\u2588\u2584\u00A6\u00CC\u2580"
		+ "\u00D3\u00DF\u00D4\u00D2\u00F5\u00D5\u00B5\u00FE"
		+ "\u00DE\u00DA\u00DB\u00D9\u00FD\u00DD\u00AF\u00B4"
		+ "\u00AD\u00B1\u2017\u00BE\u00B6\u00A7\u00F7\u00B8"
		+ "\u00B0\u00A8\u00B7\u00B9\u00B3\u00B2\u25A0\u00A0";


  private Encoding encoding;
  private String   encodingDisplayText;


  public CharConverter( Encoding encoding )
  {
    this.encoding            = encoding;
    this.encodingDisplayText = "ASCII (keine Umlaute)";
    switch( this.encoding ) {
      case ISO646DE:
	this.encodingDisplayText =
		"Deutsche Variante von ISO-646"
			+ " (Umlaute anstelle von [\\]{|}~)";
	break;

      case CP437:
	this.encodingDisplayText = "Codepage 437 (alter DOS-Zeichensatz)";
	break;

      case CP850:
	this.encodingDisplayText = "Codepage 850 (DOS-Zeichensatz)";
	break;

      case LATIN1:
	this.encodingDisplayText = "ISO-8859-1 (Latin 1)";
	break;
    }
  }


  public String getEncodingName()
  {
    String rv = "ASCII";
    switch( this.encoding ) {
      case ISO646DE:
	rv = "ISO646DE";
	break;

      case CP437:
	rv = "CP437";
	break;

      case CP850:
	rv = "CP850";
	break;

      case LATIN1:
	rv = "LATIN1";
	break;
    }
    return rv;
  }


  public char toUnicode( int ch )
  {
    char rv = REPLACEMENT_CHAR;
    if( this.encoding == Encoding.ASCII_7BIT ) {
      if( (ch > 0) && (ch < 0x7F) ) {
	rv = (char) ch;
      }
    }
    else if( encoding == Encoding.ISO646DE ) {
      switch( ch ) {
	case '[':		// Ae
	  rv = '\u00C4';
	  break;

	case '\\':		// Oe
	  rv = '\u00D6';
	  break;

	case ']':		// Ue
	  rv = '\u00DC';
	  break;

	case '{':		// ae
	  rv = '\u00E4';
	  break;

	case '|':		// oe
	  rv = '\u00F6';
	  break;

	case '}':		// ue
	  rv = '\u00FC';
	  break;

	case '~':		// ss
	  rv = '\u00DF';
	  break;

	default:
	  rv = REPLACEMENT_CHAR;
	  if( (ch > 0) && (ch < 0x7F) ) {
	    rv = (char) ch;
	  }
      }
    }
    else if( encoding == Encoding.CP437 ) {
      if( (ch >= 0) && (ch < cp437ToUnicode.length()) ) {
	rv = cp437ToUnicode.charAt( ch );
      }
    }
    else if( encoding == Encoding.CP850 ) {
      if( (ch >= 0) && (ch < cp850ToUnicode.length()) ) {
	rv = cp850ToUnicode.charAt( ch );
      }
    } else {
      if( (ch > 0) && (ch <= 0xFF) ) {
	rv = (char) ch;
      }
    }
    return rv;
  }


  public int toCharsetByte( char ch )
  {
    int rv = 0;
    if( this.encoding == Encoding.ASCII_7BIT ) {
      if( (ch > 0) && (ch < 0x7F) ) {
	rv = ch;
      }
    }
    else if( encoding == Encoding.ISO646DE ) {
      if( (ch != '[') && (ch != '\\') && (ch != ']')
	  && (ch != '{') && (ch != '|') && (ch != '}') && (ch != '~') )
      {
	switch( ch ) {
	  case '\u00C4':	// Ae
	    rv = '[';
	    break;

	  case '\u00D6':	// Oe
	    rv = '\\';
	    break;

	  case '\u00DC':	// Ue
	    rv = ']';
	    break;

	  case '\u00E4':	// ae
	    rv = '{';
	    break;

	  case '\u00F6':	// oe
	    rv = '|';
	    break;

	  case '\u00FC':	// ue
	    rv = '}';
	    break;

	  case '\u00DF':	// ss
	    rv = '~';
	    break;

	  default:
	    if( (ch > 0) && (ch < 0x7F) ) {
	      rv = ch;
	    }
	}
      }
    }
    else if( encoding == Encoding.CP437 ) {
      rv = cp437ToUnicode.indexOf( ch );
    }
    else if( encoding == Encoding.CP850 ) {
      rv = cp850ToUnicode.indexOf( ch );
    } else {
      if( (ch > 0) && (ch < 0xFF) ) {
	rv = ch;
      }
    }
    return rv > 0 ? rv : 0;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean equals( Object o )
  {
    if( o != null ) {
      if( o instanceof CharConverter ) {
	return this.encoding == ((CharConverter) o).encoding;
      }
    }
    return super.equals( o );
  }


  @Override
  public String toString()
  {
    return this.encodingDisplayText;
  }
}
