/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer einen Compiler/Assembler
 */

package jkcemu.programming;

import java.lang.*;


public class PrgUtil
{
  public static boolean isWhitespace( char ch )
  {
    return Character.isWhitespace( ch )
		|| (ch == '\u00A0')
		|| (ch == '\uC2A0');
  }
}

