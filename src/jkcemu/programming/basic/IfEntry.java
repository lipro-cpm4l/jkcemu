/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine IF-Anweisung
 */

package jkcemu.programming.basic;

import java.lang.*;


public class IfEntry extends StructureEntry
{
  private boolean multiLine;
  private String  elseLabel;
  private String  endifLabel;

  public IfEntry(
		int     sourceLineNum,
		long    basicLineNum,
		boolean multiLine,
		String  elseLabel,
		String  endifLabel )
  {
    super( sourceLineNum, basicLineNum );
    this.multiLine     = multiLine;
    this.elseLabel     = elseLabel;
    this.endifLabel    = endifLabel;
  }


  public String getElseLabel()
  {
    return this.elseLabel;
  }


  public String getEndifLabel()
  {
    return this.endifLabel;
  }


  public boolean isMultiLine()
  {
    return this.multiLine;
  }


  public void setElseLabel( String elseLabel )
  {
    this.elseLabel = elseLabel;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return "IF-Anweisung";
  }
}

