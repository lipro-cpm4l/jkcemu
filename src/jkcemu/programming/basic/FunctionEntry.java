/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine Funktion
 */

package jkcemu.programming.basic;

import java.lang.*;


public class FunctionEntry extends CallableEntry
{
  private int           retVarIYOffs;
  private SimpleVarInfo retVarInfo;


  public FunctionEntry(
		int    sourceLineNum,
		long   basicLineNum,
		String name )
  {
    super(
	sourceLineNum,
	basicLineNum,
	name,
	name.endsWith( "$" ) ?
		("UFS_" + name.substring( 0, name.length() - 1 ))
		: ("UFI_" + name) );

    // Pseudovariable fuer Rueckgabewert
    addVar( sourceLineNum, basicLineNum, name );
    setVarUsed( name );
    this.retVarIYOffs = getVarIYOffs( getVarCount() - 1);
    this.retVarInfo   = new SimpleVarInfo(
				getReturnType(),
				null,
				new Integer( this.retVarIYOffs ) );
  }


  public BasicCompiler.DataType getReturnType()
  {
    return getName().endsWith( "$" ) ?
			BasicCompiler.DataType.STRING
			: BasicCompiler.DataType.INTEGER;
  }


  public SimpleVarInfo getReturnVarInfo()
  {
    return this.retVarInfo;
  }


  public int getReturnVarIYOffs()
  {
    return this.retVarIYOffs;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return "Funktion " + getName();
  }
}

