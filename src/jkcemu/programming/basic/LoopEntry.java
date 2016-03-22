/*
 * (c) 2012-2014 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Struktureintrag einer Schleife
 */

package jkcemu.programming.basic;

import java.lang.*;
import jkcemu.programming.PrgSource;


public abstract class LoopEntry extends BasicSourcePos
{
  private String loopLabel;
  private String exitLabel;


  protected LoopEntry(
		PrgSource source,
		long      basicLineNum,
		String    loopLabel,
		String    exitLabel )
  {
    super( source, basicLineNum );
    this.loopLabel = loopLabel;
    this.exitLabel = exitLabel;
  }


  public String getExitLabel()
  {
    return this.exitLabel;
  }


  public abstract String getLoopBegKeyword();


  public String getLoopLabel()
  {
    return this.loopLabel;
  }
}
