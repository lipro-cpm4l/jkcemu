/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Halte-/Log-Punkte auf eine Interruptquelle
 */

package jkcemu.tools.debugger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import z80emu.Z80CPU;
import z80emu.Z80InterruptSource;


public class InterruptBreakpoint extends AbstractBreakpoint
{
  public static final String BP_TYPE     = "interrupt";
  public static final String ATTR_SOURCE = "source";

  private Z80InterruptSource iSource;


  public InterruptBreakpoint( DebugFrm debugFrm, Z80InterruptSource iSource )
  {
    super( debugFrm );
    this.iSource = iSource;
    setText( iSource.toString() );
  }


  public Z80InterruptSource getInterruptSource()
  {
    return this.iSource;
  }


  public void setInterruptSource( Z80InterruptSource iSource )
  {
    this.iSource = iSource;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean matchesImpl( Z80CPU cpu, Z80InterruptSource iSource )
  {
    return iSource == this.iSource;
  }


  @Override
  public void writeTo( Document doc, Node parent )
  {
    Element elem = createBreakpointElement( doc, BP_TYPE );
    elem.setAttribute( ATTR_SOURCE, this.iSource.toString() );
    appendAttributesTo( elem );
    parent.appendChild( elem );
  }
}
