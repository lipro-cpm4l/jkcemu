/*
 * (c) 2008 Jens Mueller
 *
 * Z80-Emulator
 *
 * Daten einer reassemblierten Instruction
 */

package z80emu;

import java.lang.*;


public class Z80ReassInstr extends Exception
{
  private int    len;
  private String name;
  private String arg1;
  private String arg2;


  public Z80ReassInstr( int len, String name )
  {
    this.len  = len;
    this.name = name;
    this.arg1 = null;
    this.arg2 = null;
  }


  public Z80ReassInstr( int len, String name, String arg1 )
  {
    this( len, name );
    this.arg1 = arg1;
  }


  public Z80ReassInstr(
		int    len,
		String name,
		String arg1,
		String arg2 )
  {
    this( len, name );
    this.arg1 = arg1;
    this.arg2 = arg2;
  }


  public int getLength()
  {
    return this.len;
  }


  public String getName()
  {
    return this.name;
  }


  public String getArg1()
  {
    return this.arg1;
  }


  public String getArg2()
  {
    return this.arg2;
  }
}

