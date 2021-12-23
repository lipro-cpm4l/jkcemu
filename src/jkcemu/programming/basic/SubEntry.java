/*
 * (c) 2012-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine Prozedur
 */

package jkcemu.programming.basic;

import jkcemu.programming.PrgSource;


public class SubEntry extends CallableEntry
{
  public SubEntry(
		PrgSource source,
		long      basicLineNum,
		String    name )
  {
    super( source, basicLineNum, name, "UP_" + name );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return "Prozedur " + getName();
  }
}

