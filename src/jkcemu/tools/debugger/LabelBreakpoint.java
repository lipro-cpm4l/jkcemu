/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Haltepunkte auf eine Programmadresse, die auch eine Marke hat
 */

package jkcemu.tools.debugger;

import java.lang.*;


public class LabelBreakpoint extends PCBreakpoint
{
  private String labelText;


  public LabelBreakpoint( String labelText, int addr )
  {
    super( addr );
    this.labelText = labelText;
    setText( String.format( "%04X", addr ) );
    if( labelText != null ) {
      if( !labelText.isEmpty() ) {
	setText( String.format( "%s=%04X", labelText, addr ) );
      }
    }
  }


  public String getLabelText()
  {
    return this.labelText;
  }
}

