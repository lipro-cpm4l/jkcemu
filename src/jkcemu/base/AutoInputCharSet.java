/*
 * (c) 2019-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Definition der Zeichen, die bei AutoInput erlaubt sind
 */

package jkcemu.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class AutoInputCharSet
{
  public static final String VIEW_LEFT  = "\u2190";
  public static final String VIEW_RIGHT = "\u2192";
  public static final String VIEW_DOWN  = "\u2193";
  public static final String VIEW_UP    = "\u2191";
  public static final String VIEW_HOME  = "\u2196";
  public static final String VIEW_ENTER = "\u21B2";
  public static final String VIEW_BEG   = "\u21E4";
  public static final String VIEW_END   = "\u21E5";
  public static final String VIEW_TAB   = "\u21C6";

  public static final String TEXT_BACK_SPACE = "Back Space";
  public static final String TEXT_LEFT       = "Cursor links";
  public static final String TEXT_RIGHT      = "Cursor rechts";
  public static final String TEXT_DOWN       = "Cursor runter";
  public static final String TEXT_UP         = "Cursor hoch";
  public static final String TEXT_HOME       = "Cursor links oben";
  public static final String TEXT_ENTER      = "ENTER";


  private static class Range
  {
    private char fromChar;
    private char toChar;

    private Range( char fromChar, char toChar )
    {
      this.fromChar = fromChar;
      this.toChar   = toChar;
    }
  };


  private static AutoInputCharSet cpmCharSet = null;
  private static AutoInputCharSet stdCharSet = null;

  private Map<Integer,String>       ctrlCode2Desc;
  private java.util.List<Character> specialChars;
  private java.util.List<Range>     charRanges;
  private Set<Character>            charSet;
  private Map<Character,String>     char2Desc;
  private Map<Character,String>     char2View;
  private Map<Character,Character>  view2Char;


  public AutoInputCharSet()
  {
    this.ctrlCode2Desc = null;
    this.specialChars  = new ArrayList<>();
    this.charRanges    = new ArrayList<>();
    this.charSet       = new TreeSet<>();
    this.char2Desc     = new HashMap<>();
    this.char2View     = new HashMap<>();
    this.view2Char     = new HashMap<>();
  }


  public void addAsciiChars()
  {
    addCharRange( '\u0020', '\u007E' );
  }


  public void addBackSpaceChar()
  {
    addSpecialChar(
		8,
		VIEW_LEFT,
		TEXT_LEFT + " / " + TEXT_BACK_SPACE );
  }


  public void addChar( char ch )
  {
    this.charSet.add( ch );
  }


  public void addCharRange( char fromChar, char toChar )
  {
    this.charRanges.add( new Range( fromChar, toChar ) );
  }


  public void addCtrlCodes()
  {
    if( this.ctrlCode2Desc == null ) {
      this.ctrlCode2Desc = new HashMap<>();
      setCtrlCodeDesc( 13, TEXT_ENTER );
    }
  }


  public void addCursorChars()
  {
    addSpecialChar( 8,  VIEW_LEFT,  TEXT_LEFT );
    addSpecialChar( 9,  VIEW_RIGHT, TEXT_RIGHT );
    addSpecialChar( 10, VIEW_DOWN,  TEXT_DOWN );
    addSpecialChar( 11, VIEW_UP,    TEXT_UP );
  }


  public void addDelChar()
  {
    addKeyChar( 127, "DEL" );
  }


  public void addEnterChar()
  {
    addSpecialChar( 13, VIEW_ENTER, TEXT_ENTER );
  }


  public void addEscChar()
  {
    addKeyChar( 27, "ESC" );
  }


  public void addHexChars()
  {
    this.charRanges.add( new Range( '0', '9' ) );
    this.charRanges.add( new Range( 'A', 'F' ) );
    this.char2View.put( 'a', "A" );
    this.char2View.put( 'b', "B" );
    this.char2View.put( 'c', "C" );
    this.char2View.put( 'd', "D" );
    this.char2View.put( 'e', "E" );
    this.char2View.put( 'f', "F" );
  }


  public void addKeyChar( int c, String keyText )
  {
    addSpecialChar( c, "<" + keyText + ">", keyText );
  }


  public void addSpecialChar( int c, String view, String desc )
  {
    char ch = (char) c;
    this.char2Desc.put( ch, desc );
    this.char2View.put( ch, view );
    if( view.length() == 1 ) {
      this.view2Char.put( view.charAt( 0 ), ch );
    }
    this.specialChars.add( ch );
  }


  public void addTabChar()
  {
    addSpecialChar( 9, VIEW_TAB, "TAB" );
  }


  public boolean contains( char ch )
  {
    boolean state = this.charSet.contains( ch );
    if( !state ) {
      state = this.char2View.containsKey( ch );
    }
    if( !state ) {
      for( Range r : this.charRanges ) {
	if( (ch >= r.fromChar) && (ch <= r.toChar) ) {
	  state = true;
	  break;
	}
      }
    }
    return state;
  }


  public boolean containsCtrlCodes()
  {
    return this.ctrlCode2Desc != null;
  }


  public char getCharByView( char view )
  {
    Character ch = this.view2Char.get( view );
    return ch != null ? ch.charValue() : view;
  }


  public static AutoInputCharSet getCPMCharSet()
  {
    if( cpmCharSet == null ) {
      cpmCharSet = new AutoInputCharSet();
      cpmCharSet.addAsciiChars();
      cpmCharSet.addEnterChar();
      cpmCharSet.addSpecialChar(  8, VIEW_LEFT, TEXT_LEFT );
      cpmCharSet.addSpecialChar(  4, VIEW_RIGHT, TEXT_RIGHT );
      cpmCharSet.addSpecialChar( 24, VIEW_DOWN, TEXT_DOWN );
      cpmCharSet.addSpecialChar(  5, VIEW_UP, TEXT_UP );
      cpmCharSet.addDelChar();
      cpmCharSet.addEscChar();
      cpmCharSet.addCtrlCodes();
    }
    return cpmCharSet;
  }


  public String getCtrlCodeDesc( int code )
  {
    return this.ctrlCode2Desc != null ?
			this.ctrlCode2Desc.get( code )
			: null;
  }


  public String getDescByChar( Character ch )
  {
    return ch != null ? this.char2Desc.get( ch ) : null;
  }


  public java.util.List<Character> getSpecialChars()
  {
    return this.specialChars;
  }


  public static AutoInputCharSet getStdCharSet()
  {
    if( stdCharSet == null ) {
      stdCharSet = new AutoInputCharSet();
      stdCharSet.addAsciiChars();
      stdCharSet.addEnterChar();
      stdCharSet.addBackSpaceChar();
      stdCharSet.addCtrlCodes();
    }
    return stdCharSet;
  }


  public String getViewByChar( char ch )
  {
    return this.char2View.get( ch );
  }


  public void setCtrlCodeDesc( int code, String desc )
  {
    if( this.ctrlCode2Desc != null )
      this.ctrlCode2Desc.put( code, desc );
  }


  public String toViewText( String text )
  {
    StringBuilder buf = new StringBuilder();
    for( char ch : text.toCharArray() ) {
      String view = this.char2View.get( ch );
      if( view != null ) {
	buf.append( view );
      } else {
	buf.append( ch );
      }
    }
    return buf.toString();
  }
}
