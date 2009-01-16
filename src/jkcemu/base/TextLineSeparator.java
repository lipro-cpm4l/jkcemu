/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer Zeilenendekennung
 */

package jkcemu.base;

import java.lang.*;


public class TextLineSeparator
{
  private String lineEnd;


  public TextLineSeparator( String lineEnd )
  {
    this.lineEnd = lineEnd;
  }


  public static String getDisplayText( String lineEnd )
  {
    String rv = "System";
    if( lineEnd != null ) {
      if( lineEnd.equals( "\r\n" ) ) {
	rv = "DOS/Windows (0Dh 0Ah)";
      }
      else if( lineEnd.equals( "\n" ) ) {
	rv = "Unix/Linux (0Ah)";
      }
      else if( lineEnd.equals( "\r" ) ) {
	rv = "Classic Mac (0Dh)";
      }
      else if( lineEnd.equals( "\u001E" ) ) {
	rv = "Z1013 (1Eh)";
      }
    }
    return rv;
  }


  public String getLineEnd()
  {
    return this.lineEnd;
  }


	/* --- ueberschriebene Methoden --- */

  public boolean equals( Object o )
  {
    if( o != null ) {
      if( o instanceof TextLineSeparator ) {
	String lineEnd = ((TextLineSeparator) o).lineEnd;
	if( (lineEnd == null) && (this.lineEnd == null) ) {
	  return true;
	}
	else if( (lineEnd != null) && (this.lineEnd != null) ) {
	  return lineEnd.equals( this.lineEnd );
	}
      }
    }
    return super.equals( o );
  }


  public String toString()
  {
    return getDisplayText( this.lineEnd );
  }
}

