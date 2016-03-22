/*
 * (c) 2012-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine IF-Anweisung
 */

package jkcemu.programming.basic;

import java.lang.*;
import jkcemu.programming.PrgSource;


public class IfEntry extends BasicSourcePos
{
  private boolean multiLine;
  private String  elseLabel;
  private String  endifLabel;
  private int     codeCreationDisabledLevel;
  private boolean ifCodeCreationDisabled;
  private boolean elseCodeCreationDisabled;


  public IfEntry(
		PrgSource source,
		long      basicLineNum,
		boolean   multiLine,
		String    elseLabel,
		String    endifLabel,
		int       codeCreationDisabledLevel,
		boolean   ifCodeCreationDisabled,
		boolean   elseCodeCreationDisabled )
  {
    super( source, basicLineNum );
    this.multiLine                 = multiLine;
    this.elseLabel                 = elseLabel;
    this.endifLabel                = endifLabel;
    this.codeCreationDisabledLevel = codeCreationDisabledLevel;
    this.ifCodeCreationDisabled    = ifCodeCreationDisabled;
    this.elseCodeCreationDisabled  = elseCodeCreationDisabled;
  }


  public int getCodeCreationDisabledLevel()
  {
    return this.codeCreationDisabledLevel;
  }


  public String getElseLabel()
  {
    return this.elseLabel;
  }


  public String getEndifLabel()
  {
    return this.endifLabel;
  }


  public boolean isElseCodeCreationDisabled()
  {
    return this.elseCodeCreationDisabled;
  }


  public boolean isIfCodeCreationDisabled()
  {
    return this.ifCodeCreationDisabled;
  }


  public boolean isMultiLine()
  {
    return this.multiLine;
  }


  public void setElseLabel( String elseLabel )
  {
    this.elseLabel = elseLabel;
  }


  public void setElseCodeCreationDisabled( boolean state )
  {
    this.elseCodeCreationDisabled = state;
  }


  public void setIfCodeCreationDisabled( boolean state )
  {
    this.ifCodeCreationDisabled = state;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return "IF-Anweisung";
  }
}
