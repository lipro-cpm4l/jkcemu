/*
 * (c) 2017-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Interface fuer einen Parser fuer eine Operation
 */

package jkcemu.programming.basic;

import java.text.CharacterIterator;
import jkcemu.programming.PrgException;


public interface OpParser
{
  public BasicCompiler.DataType parseOp(
			BasicCompiler          compiler,
			CharacterIterator      iter,
			ParseContext           context,
			BasicCompiler.DataType prefRetType )
						throws PrgException;
}
