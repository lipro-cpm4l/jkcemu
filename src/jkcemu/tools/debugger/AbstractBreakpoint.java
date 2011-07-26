/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer Haltepunkte
 */

package jkcemu.tools.debugger;

import java.lang.*;
import z80emu.Z80Breakpoint;


public abstract class AbstractBreakpoint
			implements
				Comparable<AbstractBreakpoint>,
				Z80Breakpoint
{
  public static enum AccessMode { READ, WRITE, READ_WRITE };

  private static final String DEFAULT_TEXT = "<unbekannt>";

  private AccessMode accessMode;
  private String     text;
  private boolean    enabled;


  protected AbstractBreakpoint( AccessMode accessMode )
  {
    this.accessMode = accessMode;
    this.text       = DEFAULT_TEXT;
    this.enabled    = true;
  }


  protected AbstractBreakpoint()
  {
    this( null );
  }


  public void completeAccessMode( AccessMode accessMode )
  {
    if( this.accessMode != null ) {
      if( accessMode != null ) {
	if( (accessMode == AccessMode.READ_WRITE)
	    || ((accessMode == AccessMode.READ)
			&& (this.accessMode == AccessMode.WRITE))
	    || ((accessMode == AccessMode.WRITE)
			&& (this.accessMode == AccessMode.READ)) )
	{
	  this.accessMode = AccessMode.READ_WRITE;
	}
      }
    }
  }


  protected void createAndSetText(
				int     begAddr,
				int     endAddr,
				boolean is8Bit,
				int     value,
				int     mask )
  {
    StringBuilder buf = new StringBuilder( 20 );
    buf.append( String.format( is8Bit ? "%02X" : "%04X", begAddr ) );
    if( endAddr >= 0 ) {
      buf.append( String.format( is8Bit ? "-%02X" : "-%04X", endAddr ) );
    }
    if( value >= 0 ) {
      buf.append( String.format( ":%02X/%02X", value, mask ) );
    }
    if( this.accessMode != null ) {
      switch( this.accessMode ) {
	case READ:
	  buf.append( " R" );
	  break;
	case WRITE:
	  buf.append( " W" );
	  break;
	case READ_WRITE:
	  buf.append( " RW" );
	  break;
      }
    }
    setText( buf.toString() );
  }


  public AccessMode getAccessMode()
  {
    return this.accessMode;
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

