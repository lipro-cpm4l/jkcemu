/*
 * (c) 2012-2014 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine DO-Schleife
 */

package jkcemu.programming.basic;

import java.lang.*;
import jkcemu.programming.PrgSource;


public class DoEntry extends LoopEntry
{
  private String loopLabel;


  public DoEntry(
		PrgSource source,
		long      basicLineNum,
		String    loopLabel,
		String    exitLabel )
  {
    super( source, basicLineNum, loopLabel, exitLabel );
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

