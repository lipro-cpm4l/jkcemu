/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Exception fuer fehlerhafte Benutzereingaben
 */

package jkcemu.base;

import java.lang.*;


public class UserInputException extends Exception
{
  private String fieldName;


  public UserInputException( String msg )
  {
    super( msg );
    this.fieldName = null;
  }


  public UserInputException( String msg, String fieldName )
  {
    super( msg );
    this.fieldName = fieldName;
  }


  public String getFieldName()
  {
    return this.fieldName;
  }
}

