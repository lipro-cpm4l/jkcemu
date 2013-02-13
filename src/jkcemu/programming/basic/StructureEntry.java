/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abstrakte Klasse fuer den Stack-Eintrag einer Programmcodestruktur
 */

package jkcemu.programming.basic;

import java.lang.*;


public abstract class StructureEntry
{
  private long basicLineNum;
  private int  sourceLineNum;


  protected StructureEntry( int sourceLineNum, long basicLineNum )
  {
    this.sourceLineNum = sourceLineNum;
    this.basicLineNum  = basicLineNum;
  }


  public long getBasicLineNum()
  {
    return this.basicLineNum;
  }


  public int getSourceLineNum()
  {
    return this.sourceLineNum;
  }
}

