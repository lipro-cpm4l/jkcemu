/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eintrag im Assembler-Stack fuer bedingte Assemblierung
 */

package jkcemu.programming.assembler;

import java.lang.*;
import jkcemu.programming.PrgException;


public class AsmStackEntry
{
  private int     lineNum;
  private boolean asmEnabled;
  private boolean elseProcessed;


  public AsmStackEntry( int lineNum, boolean asmEnabled )
  {
    this.lineNum       = lineNum;
    this.asmEnabled    = asmEnabled;
    this.elseProcessed = false;
  }


  public int getLineNum()
  {
    return this.lineNum;
  }


  public boolean isAssemblingEnabled()
  {
    return this.asmEnabled;
  }


  public void processELSE() throws PrgException
  {
    if( this.elseProcessed ) {
      throw new PrgException( "ELSE ohne zugeh\u00F6riges IF..." );
    }
    this.asmEnabled    = !this.asmEnabled;
    this.elseProcessed = true;
  }
}
