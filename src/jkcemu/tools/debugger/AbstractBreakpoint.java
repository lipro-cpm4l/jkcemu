/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Halte-/Log-Punkt
 */

package jkcemu.tools.debugger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import z80emu.Z80Breakpoint;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;


public abstract class AbstractBreakpoint
			implements
				Comparable<AbstractBreakpoint>,
				Z80Breakpoint
{
  public static final String ELEM_BREAKPOINT   = "breakpoint";
  public static final String ATTR_TYPE         = "type";
  public static final String ATTR_LOG_ENABLED  = "log_enabled";
  public static final String ATTR_STOP_ENABLED = "stop_enabled";

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


  protected void appendAttributesTo( Element elem )
  {
    elem.setAttribute(
		ATTR_LOG_ENABLED,
		Boolean.toString( this.logEnabled ) );
    elem.setAttribute(
		ATTR_STOP_ENABLED,
		Boolean.toString( this.stopEnabled ) );
  }


  protected static String checkCondition( String cond )
					throws InvalidParamException
  {
    if( cond != null ) {
      if( cond.isEmpty() ) {
	cond = null;
      } else {
	if( !cond.equals( "<" )
		&& !cond.equals( "<=" )
		&& !cond.equals( "<>" )
		&& !cond.equals( "=" )
		&& !cond.equals( ">=" )
		&& !cond.equals( ">" ) )
	{
	  throw new InvalidParamException(
				cond + ": Ung\u00FCltige Bedingung" );
	}
      }
    }
    return cond;
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


  public static Element createBreakpointElement(
					Document doc,
					String   type )
  {
    Element elem = doc.createElement( ELEM_BREAKPOINT );
    elem.setAttribute( ATTR_TYPE, type );
    return elem;
  }


  protected static int getHex2Value( Attributes attrs, String attrName )
  {
    return BreakpointVarLoader.getIntValue( attrs, attrName ) & 0xFF;
  }


  protected static int getHex4Value( Attributes attrs, String attrName )
  {
    return BreakpointVarLoader.getIntValue( attrs, attrName ) & 0xFFFF;
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


  protected static String toHex2( int value )
  {
    return String.format( "%02XH", value & 0xFF );
  }


  protected static String toHex4( int value )
  {
    return String.format( "%04XH", value & 0xFFFF );
  }


  public void writeTo( Document doc, Node parent )
  {
    // zu ueberschreiben
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
