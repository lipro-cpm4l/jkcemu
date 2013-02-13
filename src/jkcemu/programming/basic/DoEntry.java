/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine DO-Schleife
 */

package jkcemu.programming.basic;

import java.lang.*;


public class DoEntry extends LoopEntry
{
  private String loopLabel;


  public DoEntry(
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
    return "DO";
  }


  @Override
  public String toString()
  {
    return "DO-Schleife";
  }
}

