/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Haltepunkt des Debuggers
 */

package z80emu;

import java.lang.*;


public class Z80Breakpoint implements Comparable<Z80Breakpoint>
{
  private int     addr;
  private boolean enabled;
  private String  text;


  public Z80Breakpoint( int addr )
  {
    this.addr    = addr;
    this.enabled = true;
    this.text    = null;
  }


  public int getAddress()
  {
    return this.addr;
  }


  public boolean isEnabled()
  {
    return this.enabled;
  }


  public void setEnabled( boolean state )
  {
    this.enabled = state;
    this.text    = null;
  }


        /* --- Comparable --- */

  public int compareTo( Z80Breakpoint bp )
  {
    return bp != null ? (this.addr - bp.addr) : -1;
  }


	/* --- ueberschriebene Methoden --- */

  public String toString()
  {
    if( this.text == null ) {
      if( this.addr < 0 ) {
	this.text = this.enabled ? "INT" : "( INT )";
      } else {
	if( this.enabled ) {
	  this.text = String.format( "%04X", this.addr );
	} else {
	  this.text = String.format( "( %04X )", this.addr );
	}
      }
    }
    return text;
  }
}

