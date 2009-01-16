/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Exception eines Compilers/Assemblers
 */

package jkcemu.programming;

import java.lang.*;


public class PrgException extends Exception
{
  public PrgException( String msg )
  {
    super( msg );
  }
}

