/*
 * (c) 2012-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Hilfsfunktionen fuer einen Compiler/Assembler
 */

package jkcemu.programming;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;


public class PrgUtil
{
  public static boolean isWhitespace( char ch )
  {
    return Character.isWhitespace( ch )
		|| (ch == '\u00A0')
		|| (ch == '\uC2A0');
  }


  public static char skipSpaces( CharacterIterator iter )
  {
    char ch = iter.current();
    while( (ch != CharacterIterator.DONE) && isWhitespace( ch ) ) {
      ch = iter.next();
    }
    return ch;
  }
}
