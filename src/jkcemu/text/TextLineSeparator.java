/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Daten einer Zeilenendekennung
 */

package jkcemu.text;


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

  @Override
  public boolean equals( Object o )
  {
    boolean rv = false;
    if( o != null ) {
      if( o instanceof TextLineSeparator ) {
	String lineEnd = ((TextLineSeparator) o).getLineEnd();
	if( lineEnd == null ) {
	  lineEnd = "";
	}
	rv = lineEnd.equals( this.lineEnd != null ? this.lineEnd : "" );
      }
    }
    return rv;
  }


  @Override
  public int hashCode()
  {
    String s = this.lineEnd;
    if( s == null ) {
      s = "";
    }
    return s.hashCode() ^ 0x46265139;
  }


  @Override
  public String toString()
  {
    return getDisplayText( this.lineEnd );
  }
}
