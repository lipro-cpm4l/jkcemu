/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Variablendeklaration
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.util.Map;


public class VarDecl
{
  private int     sourceLineNum;
  private long    basicLineNum;
  private String  varName;
  private int     dim1;
  private int     dim2;
  private int     nDims;
  private int     size;
  private boolean used;
  private String  label;
  private String  infoText;


  public VarDecl(
		int    sourceLineNum,
		long   basicLineNum,
		String varName,
		int    dim1,
		int    dim2 )
  {
    this.sourceLineNum = sourceLineNum;
    this.basicLineNum  = basicLineNum;
    this.varName       = varName;
    this.dim1          = dim1;
    this.dim2          = dim2;
    this.nDims         = 0;
    this.size          = 2;
    this.used          = false;
    if( dim1 > 0 ) {
      this.nDims++;
      this.size *= (dim1 + 1);
      if( dim2 > 0 ) {
	this.nDims++;
	this.size *= (dim2 + 1);
      }
    }
    if( varName.endsWith( "$" ) ) {
      this.label = "VS_" + varName.substring( 0, varName.length() - 1 );
    } else {
      this.label = "VI_" + varName;
    }
    if( this.nDims > 0 ) {
      this.infoText = "Feldvariable ";
    } else {
      this.infoText = "Variable ";
    }
    this.infoText += varName;
  }


  public VarDecl(
		int    sourceLineNum,
		long   basicLineNum,
		String varName,
		int    dim1 )
  {
    this( sourceLineNum, basicLineNum, varName, dim1, 0 );
  }


  public VarDecl(
		int    sourceLineNum,
		long   basicLineNum,
		String varName )
  {
    this( sourceLineNum, basicLineNum, varName, 0, 0 );
  }


  public long getBasicLineNum()
  {
    return this.basicLineNum;
  }


  public int getDim1()
  {
    return this.dim1;
  }


  public int getDim2()
  {
    return this.dim2;
  }


  public int getDimCount()
  {
    return this.nDims;
  }


  public String getLabel()
  {
    return this.label;
  }


  public int getSize()
  {
    return this.size;
  }


  public int getSourceLineNum()
  {
    return this.sourceLineNum;
  }


  public boolean isUsed()
  {
    return this.used;
  }


  public void setUsed()
  {
    this.used = true;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.infoText;
  }
}
