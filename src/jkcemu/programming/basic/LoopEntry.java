/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Struktureintrag einer Schleife
 */

package jkcemu.programming.basic;

import java.lang.*;


public abstract class LoopEntry extends StructureEntry
{
  private String loopLabel;
  private String exitLabel;


  protected LoopEntry(
		int    sourceLineNum,
		long   basicLineNum,
		String loopLabel,
		String exitLabel )
  {
    super( sourceLineNum, basicLineNum );
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
