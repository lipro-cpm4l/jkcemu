/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine FOR-NEXT-Schleife
 */

package jkcemu.programming.basic;

import java.lang.*;


public class ForEntry extends LoopEntry
{
  private SimpleVarInfo varInfo;
  private Integer       toValue;
  private Integer       stepValue;


  public ForEntry(
		int           sourceLineNum,
		long          basicLineNum,
		String        loopLabel,
		String        exitLabel,
		SimpleVarInfo varInfo,
		Integer       toValue,
		Integer       stepValue )
  {
    super( sourceLineNum, basicLineNum, loopLabel, exitLabel );
    this.varInfo   = varInfo;
    this.toValue   = toValue;
    this.stepValue = stepValue;
  }


  public Integer getStepValue()
  {
    return this.stepValue;
  }


  public Integer getToValue()
  {
    return this.toValue;
  }


  public SimpleVarInfo getSimpleVarInfo()
  {
    return this.varInfo;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getLoopBegKeyword()
  {
    return "FOR";
  }


  @Override
  public String toString()
  {
    return "FOR-Schleife";
  }
}
