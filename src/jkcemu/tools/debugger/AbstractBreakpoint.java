/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Halte-/Log-Punkt
 */

package jkcemu.tools.debugger;

import java.lang.*;
import z80emu.*;


public abstract class AbstractBreakpoint
			implements
				Comparable<AbstractBreakpoint>,
				Z80Breakpoint
{
  private DebugFrm debugFrm;
  private String   text;
  private boolean  stopEnabled;
  private boolean  logEnabled;


  protected AbstractBreakpoint( DebugFrm debugFrm )
  {
    this.debugFrm    = debugFrm;
    this.text        = "";
    this.stopEnabled = false;
    this.logEnabled  = false;
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


  public boolean isLogEnabled()
  {
    return this.logEnabled;
  }


  public boolean isStopEnabled()
  {
    return this.stopEnabled;
  }


  public boolean matches( Z80CPU cpu, Z80InterruptSource iSource )
  {
    boolean rv = false;
    if( this.logEnabled || this.stopEnabled ) {
      if( matchesImpl( cpu, iSource ) ) {
	if( this.logEnabled ) {
	  this.debugFrm.appendLogEntry( iSource );
	}
	if( this.stopEnabled ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


  protected abstract boolean matchesImpl(
				Z80CPU             cpu,
				Z80InterruptSource iSource );


  public void setLogEnabled( boolean state )
  {
    this.logEnabled = state;
  }


  public void setStopEnabled( boolean state )
  {
    this.stopEnabled = state;
  }


  protected void setText( String text )
  {
    this.text = (text != null ? text : "");
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
    return this.text;
  }
}
