/*
 * (c) 2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Exception fuer fehlerhafte Parameter
 */

package jkcemu.tools.debugger;


public class InvalidParamException extends Exception
{
  public InvalidParamException( String msg )
  {
    super( msg );
  }
}
