/*
 * (c) 2011-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Haltepunkt
 */

package jkcemu.tools.debugger;

import java.lang.*;
import z80emu.Z80Breakpoint;


public abstract class AbstractBreakpoint
			implements
				Comparable<AbstractBreakpoint>,
				Z80Breakpoint
{
  private static final String DEFAULT_TEXT = "<unbekannt>";

  private String  text;
  private boolean enabled;


  protected AbstractBreakpoint()
  {
    this.text    = DEFAULT_TEXT;
    this.enabled = true;
  }


  protected static boolean checkValues( int v1, String cond, int v2 )
  {
    boolean rv = false;
    if( cond != null ) {
      if( cond.equals( "<" ) ) {
	rv = (v1 < v2);
      } else if( cond.equals( "<=" ) ) {
	rv = (v1 <= v2);
      } else if( cond.equals( "<>" ) ) {
	rv = (v1 != v2);
      } else if( cond.equals( ">=" ) ) {
	rv = (v1 >= v2);
      } else if( cond.equals( ">" ) ) {
	rv = (v1 > v2);
      } else {
	rv = (v1 == v2);
      }
    }
    return rv;
  }


  public String getText()
  {
    return this.text;
  }


  public boolean isEnabled()
  {
    return this.enabled;
  }


  protected void setText( String text )
  {
    this.text = (text != null ? text : DEFAULT_TEXT);
  }


  public void setEnabled( boolean state )
  {
    this.enabled = state;
  }


	/* --- Comparable --- */

  @Override
  public int compareTo( AbstractBreakpoint bp )
  {
    return this.text.compareTo( bp.text );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean equals( Object o )
  {
    boolean rv = false;
    if( o != null ) {
      if( o instanceof AbstractBreakpoint ) {
	rv = this.text.equals( ((AbstractBreakpoint) o).text );
      }
    }
    return rv;
  }


  @Override
  public String toString()
  {
    return this.enabled ? this.text : String.format( "( %s )", this.text );
  }
}

