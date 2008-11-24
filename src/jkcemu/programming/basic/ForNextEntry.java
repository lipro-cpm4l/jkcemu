/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eintrag fuer eine FOR-NEXT-Schleife
 */

package jkcemu.programming.basic;

import java.lang.*;


public class ForNextEntry
{
  private long    basicLineNum;
  private int     sourceLineNum;
  private int     stepValue;
  private Integer endValue;
  private String  counterVarAddrExpr;
  private String  loopLabel;


  public ForNextEntry( int sourceLineNum, long basicLineNum )
  {
    this.sourceLineNum      = sourceLineNum;
    this.basicLineNum       = basicLineNum;
    this.stepValue          = -1;
    this.endValue           = null;
    this.counterVarAddrExpr = null;
    this.loopLabel          = null;
  }


  public long getBasicLineNum()
  {
    return this.basicLineNum;
  }


  public int getSourceLineNum()
  {
    return this.sourceLineNum;
  }


  public String getCounterVariableAddrExpr()
  {
    return this.counterVarAddrExpr;
  }


  public Integer getEndValue()
  {
    return this.endValue;
  }


  public String getLoopLabel()
  {
    return this.loopLabel;
  }


  public int getStepValue()
  {
    return this.stepValue;
  }


  public void setCounterVariableAddrExpr( String expr )
  {
    this.counterVarAddrExpr = expr;
  }


  public void setEndValue( Integer value )
  {
    this.endValue = value;
  }


  public void setLoopLabel( String label )
  {
    this.loopLabel = label;
  }


  public void setStepValue( int value )
  {
    this.stepValue = value;
  }
}

