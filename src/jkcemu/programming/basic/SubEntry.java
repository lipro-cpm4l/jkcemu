/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 *  * Struktureintrag fuer eine Prozedur
 */

package jkcemu.programming.basic;

import java.lang.*;


public class SubEntry extends CallableEntry
{
  public SubEntry(
		int    sourceLineNum,
		long   basicLineNum,
		String name )
  {
    super( sourceLineNum, basicLineNum, name, "UP_" + name );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return "Prozedur " + getName();
  }
}
