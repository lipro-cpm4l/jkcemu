/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine WHILE-Schleife
 */

package jkcemu.programming.basic;

import java.lang.*;


public class WhileEntry extends LoopEntry
{
  private String loopLabel;
  private String endLabel;


  public WhileEntry(
		int    sourceLineNum,
		long   basicLineNum,
		String loopLabel,
		String exitLabel )
  {
    super( sourceLineNum, basicLineNum, loopLabel, exitLabel );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getLoopBegKeyword()
  {
    return "WHILE";
  }


  @Override
  public String toString()
  {
    return "WHILE-Schleife";
  }
}

