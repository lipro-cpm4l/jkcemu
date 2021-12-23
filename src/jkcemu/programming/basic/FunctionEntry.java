/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Struktureintrag fuer eine Funktion
 */

package jkcemu.programming.basic;

import jkcemu.programming.PrgException;
import jkcemu.programming.PrgSource;


public class FunctionEntry extends CallableEntry
{
  private BasicCompiler.DataType retType;
  private SimpleVarInfo          retVarInfo;


  public FunctionEntry(
		PrgSource              source,
		long                   basicLineNum,
		String                 name,
		BasicCompiler.DataType retType ) throws PrgException
  {
    super( source, basicLineNum, name, getLabel( name, retType ) );
    this.retType = retType;

    // Pseudovariable fuer Rueckgabewert
    addVar( source, basicLineNum, name, retType );
    updVarUsage( name, BasicCompiler.AccessMode.READ_WRITE );
    this.retVarInfo = null;
  }


  public BasicCompiler.DataType getReturnType()
  {
    return this.retType;
  }


  public synchronized SimpleVarInfo getReturnVarInfo()
  {
    if( this.retVarInfo == null ) {
      this.retVarInfo = new SimpleVarInfo(
				getReturnType(),
				null,
				getReturnVarIYOffs() );
    }
    return this.retVarInfo;
  }


  public int getReturnVarIYOffs()
  {
    return getIYOffs( getName() );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return "Funktion " + getName();
  }


	/* --- private Methoden --- */

  private static String getLabel(
				String                 name,
				BasicCompiler.DataType retType )
  {
    String rv = "UF_I2_" + name;
    switch( retType ) {
      case INT4:
	rv = "UF_I4_" + name;
	break;
      case DEC6:
	rv = "UF_D6_" + name;
	break;
      case STRING:
	rv = "UF_S_" + name.substring( 0, name.length() - 1 );
	break;
    }
    return rv;
  }
}
