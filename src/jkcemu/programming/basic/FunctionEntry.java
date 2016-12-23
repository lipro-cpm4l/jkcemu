/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine Funktion
 */

package jkcemu.programming.basic;

import java.lang.*;
import jkcemu.programming.PrgSource;


public class FunctionEntry extends CallableEntry
{
  private int           retVarIYOffs;
  private SimpleVarInfo retVarInfo;


  public FunctionEntry(
		PrgSource source,
		long      basicLineNum,
		String    name )
  {
    super(
	source,
	basicLineNum,
	name,
	name.endsWith( "$" ) ?
		("UFS_" + name.substring( 0, name.length() - 1 ))
		: ("UFI_" + name) );

    // Pseudovariable fuer Rueckgabewert
    addVar( source, basicLineNum, name );
    setVarUsed( name );
    this.retVarIYOffs = getVarIYOffs( getVarCount() - 1);
    this.retVarInfo   = new SimpleVarInfo(
				getReturnType(),
				null,
				this.retVarIYOffs );
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

