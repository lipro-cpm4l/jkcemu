/*
 * (c) 2011-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Debug-Punkte auf eine Programmadresse, die auch eine Marke hat
 */

package jkcemu.tools.debugger;

import java.lang.*;


public class LabelBreakpoint extends PCBreakpoint
{
  private String labelName;


  public LabelBreakpoint(
		DebugFrm debugFrm,
		String   labelName,
		int      addr,
		String   reg,
		int      mask,
		String   cond,
		int      value,
		boolean  imported )
  {
    super( debugFrm, addr, reg, mask, cond, value, imported );
    this.labelName = labelName;
    updText();
  }


  public LabelBreakpoint(
		DebugFrm     debugFrm,
		String       labelName,
		int          addr,
		PCBreakpoint srcBP,
		boolean      imported )
  {
    this( debugFrm, labelName, addr, null, 0xFF, null, 0, imported );
    if( srcBP != null ) {
      setConditionValues(
		srcBP.getRegister(),
		srcBP.getMask(),
		srcBP.getCondition(),
		srcBP.getValue() );
      updText();
      setLogEnabled( srcBP.isLogEnabled() );
      setStopEnabled( srcBP.isStopEnabled() );
    }
  }


  public String getLabelName()
  {
    return this.labelName;
  }


	/* --- private Methoden --- */

  private void updText()
  {
    if( this.labelName != null ) {
      if( !this.labelName.isEmpty() ) {
	setText( this.labelName + ": " + getText() );
      }
    }
  }
}
